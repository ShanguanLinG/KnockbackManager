package me.dw1e.kbm.module;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.api.event.KBMServerSideHitEvent;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.data.PlayerData;
import me.dw1e.kbm.util.AABB;
import me.dw1e.kbm.util.Ray;
import me.dw1e.kbm.util.VersionUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 基于 Islandscout 的 ServerSideHitDetection
public final class HitDetection extends PacketAdapter implements Listener {

    // 1.8没有攻击冷却, 但是我懒得把这些拆开了

    // 此功能可以说和反作弊有99%的相似, 而且也会因为misplace误判
    // 除非把FairFight那套根据Rel_Entity_Move数据包的延迟补偿搬来, 不然不可避免

    private final Map<UUID, State> states = new HashMap<>();

    private final KnockbackManager plugin;
    private final LagCompensator lagCompensator;

    private AABB aabb;

    public HitDetection(KnockbackManager plugin, LagCompensator lagCompensator) {
        super(plugin, PacketType.Play.Client.ARM_ANIMATION);

        this.plugin = plugin;

        this.lagCompensator = lagCompensator;
        loadConfig();
    }

    public void enable() {
        plugin.getProtocolManager().addPacketListener(this);
    }

    public void disable() {
        plugin.getProtocolManager().removePacketListener(this);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (!ConfigValue.SSHD_ENABLED) return;

        // 为什么要单独注册这个数据包? 因为Bukkit Event呼的太晚了, 这时候冷却已经开始了

        if (plugin.isAtLeast1_16() && event.getPacketType() == PacketType.Play.Client.ARM_ANIMATION) {
            Player player = event.getPlayer();
            PlayerData data = plugin.getDataManager().getData(player.getUniqueId());

            data.setCacheCooldown(player.getAttackCooldown()); // 缓存攻击冷却
        }
    }

    public void loadConfig() {
        double length = ConfigValue.SSHD_HITBOX_LENGTH;
        double height = ConfigValue.SSHD_HITBOX_HEIGHT;
        aabb = new AABB(new Vector(-length / 2, 0, -length / 2), new Vector(length / 2, height, length / 2));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void cancelDefaultHit(EntityDamageByEntityEvent event) {
        if (!ConfigValue.SSHD_ENABLED) return;

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            State state = getState(event.getDamager().getUniqueId());

            if (!state.serverSideHit) event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!ConfigValue.SSHD_ENABLED) return;

        Location from = event.getFrom(), to = event.getTo();
        if (to == null) return;

        State state = getState(event.getPlayer().getUniqueId());

        state.noMoveTicks = 0;

        if (state.event != null) {
            Location location = from.clone();

            // 视角会比位置先更新, 所以使用上次的位置与当前的视角
            location.setYaw(to.getYaw());
            location.setPitch(to.getPitch());

            detectHit(state.event, location);

            state.event = null;
        }
    }

    @EventHandler
    public void onArmAnimation(PlayerAnimationEvent event) {
        if (!ConfigValue.SSHD_ENABLED) return;

        Player attacker = event.getPlayer();
        State state = getState(attacker.getUniqueId());

        state.event = event;
        state.lastLoc = attacker.getLocation().clone();
    }

    private void detectHit(PlayerAnimationEvent event, Location attackerLoc) {
        Player attacker = event.getPlayer();

        PlayerData attackerData = plugin.getDataManager().getData(attacker.getUniqueId());
        if (attackerData == null) return;

        Vector attackerPos = attackerLoc.toVector().add(new Vector(0, attacker.getEyeHeight(), 0));

        double maxDist = attacker.getGameMode() == GameMode.CREATIVE
                ? ConfigValue.SSHD_MAX_DISTANCE_CREATIVE
                : ConfigValue.SSHD_MAX_DISTANCE_SURVIVAL;

        double adder = 2;
        List<Entity> nearbyEntities = attacker.getNearbyEntities(maxDist + adder, maxDist + adder, maxDist + adder);

        AABB victimBox = aabb.clone();
        Vector boxOffset = aabb.getMin();

        Ray ray = null;
        double hitDistance = Double.MAX_VALUE;

        Player victim = null;

        int ping = attackerData.getPing();

        for (Entity chkEntity : nearbyEntities) {
            if (!(chkEntity instanceof Player)) continue;
            Player chkVictim = (Player) chkEntity;

            ray = new Ray(attackerPos, attackerLoc.getDirection());

            victimBox.translateTo(lagCompensator.getHistoryLocation(ping, chkVictim).toVector());
            victimBox.translate(boxOffset);

            Vector intersection = victimBox.intersectsRay(ray, 0, (float) maxDist);
            if (intersection == null) continue;

            double chkHitDistance = intersection.distance(attackerPos);
            if (chkHitDistance < hitDistance) {
                hitDistance = chkHitDistance;
                victim = chkVictim;
            }
        }

        if (victim == null) return;

        int dist = (int) hitDistance;
        if (dist != 0) {
            BlockIterator iterator = new BlockIterator(attacker.getWorld(), attackerPos, ray.getDirection(), 0, dist);

            while (iterator.hasNext()) {
                Block chkBlock = iterator.next();

                if (chkBlock.getType().isSolid()) return;
            }
        }

        KBMServerSideHitEvent hitEvent = new KBMServerSideHitEvent(attacker, victim);
        Bukkit.getPluginManager().callEvent(hitEvent);
        if (hitEvent.isCancelled()) return;

        State state = getState(attacker.getUniqueId());

        state.serverSideHit = true;

        if (plugin.isAtLeast1_16()) {
            attacker.attack(victim);
        } else {
            VersionUtil.attack(attacker, victim);
        }

        state.serverSideHit = false;

        // 调用NMS的attack方法攻击后, 疾跑状态不会重置, 需要手动重置
        attacker.setSprinting(false);
    }

    public void onTick() {
        if (!ConfigValue.SSHD_ENABLED) return;

        for (State state : states.values()) {
            if (state.event != null) {
                if (++state.noMoveTicks >= 2) {
                    state.noMoveTicks = 0;

                    detectHit(state.event, state.lastLoc);

                    state.event = null;
                }
            } else {
                state.noMoveTicks = 0;
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    private State getState(UUID uuid) {
        return states.computeIfAbsent(uuid, k -> new State());
    }

    private static final class State {
        private PlayerAnimationEvent event;
        private int noMoveTicks;
        private boolean serverSideHit;
        private Location lastLoc;
    }
}
