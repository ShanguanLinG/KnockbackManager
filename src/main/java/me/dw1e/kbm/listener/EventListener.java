package me.dw1e.kbm.listener;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.api.event.KBMPlayerVelocityEvent;
import me.dw1e.kbm.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class EventListener implements Listener {

    private final KnockbackManager plugin;

    public EventListener(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        PlayerData victimData = plugin.getDataManager().getData(victim.getUniqueId());

        if (victimData == null || victimData.isFilter()) return;

        int tick = plugin.getTick();

        victimData.setLastAttackedByOtherTick(tick);

        Entity source = event.getDamager();
        LivingEntity attacker;

        boolean isProjectileHit = false, isSelfShootHit = false;
        int arrowStrength = 0; // 弓箭上的'冲击'附魔等级

        if (source instanceof LivingEntity) { // 玩家造成的击退
            attacker = (LivingEntity) source;

        } else if (source instanceof Projectile) { // 投掷物造成的击退
            ProjectileSource shooter = ((Projectile) source).getShooter();

            if (!(shooter instanceof LivingEntity)) return;

            // 本次击退为投掷物造成的
            isProjectileHit = true;

            attacker = (LivingEntity) shooter;

            // 本次击退的攻击者是自己, 例如: Bow Boost
            isSelfShootHit = attacker.getEntityId() == victim.getEntityId();

            // 获取弓箭的冲击附魔等级
            if (source.getType() == EntityType.ARROW) {
                arrowStrength = ((Arrow) source).getKnockbackStrength();
            }

        } else return;

        double deltaX = victim.getLocation().getX() - source.getLocation().getX();
        double deltaZ = victim.getLocation().getZ() - source.getLocation().getZ();

        // 防止坐标一致时, 水平击退为0的问题 (原版处理逻辑)
        while (Math.hypot(deltaX, deltaZ) < 0.0001) {
            double random = (Math.random() - Math.random()) * 0.01;

            deltaX = random;
            deltaZ = random;
        }

        float radianYaw = attacker.getLocation().getYaw() * 0.017453292F;

        Vector velocity = isSelfShootHit
                // Bow Boost 的击退, 按照玩家视角朝向造成击退
                ? new Vector(-Math.sin(radianYaw), 1.0, Math.cos(radianYaw))
                // 其他玩家攻击造成的击退
                : new Vector(deltaX, 0.0, deltaZ).normalize().setY(1.0);

        // 玩家发送的数据包地面状态, 可被外挂欺骗
        boolean onGround = victimData.isOnGround();

        FileConfiguration config = plugin.getKbFile().getKbMap().get(victimData.getKbFilename()).getValue();

        // 地面击退
        double hor_ground = config.getDouble("horizontal.ground");
        double ver_ground = config.getDouble("vertical.ground");

        // 空中击退
        double hor_air = config.getDouble("horizontal.air");
        double ver_air = config.getDouble("vertical.air");

        // 根据判断玩家是否在地面上来决定该应用哪个击退
        double hor = onGround ? hor_ground : hor_air;
        double ver = onGround ? ver_ground : ver_air;

        // 应用基础击退
        velocity.multiply(new Vector(hor, ver, hor));

        double hor_sprint_extra = config.getDouble("horizontal.sprint_extra");
        double ver_sprint_extra = config.getDouble("vertical.sprint_extra");

        // Bukkit 内部状态, 在一次攻击后会被服务器强制置为 false (需要疾跑重置)
        boolean bukkitSprinting = false;

        if (attacker instanceof Player) {
            bukkitSprinting = ((Player) attacker).isSprinting();
        }

        // 客户端数据包的疾跑状态, 只要玩家未松开疾跑键就保持为 true (按住前进与疾跑即可)
        boolean packetSprinting = false;

        PlayerData attackerData = plugin.getDataManager().getData(attacker.getUniqueId());
        if (attackerData != null) {
            attackerData.setLastAttackTick(tick);
            attackerData.setTarget(victim);

            packetSprinting = attackerData.isSprinting();
        }

        // 投掷物造成的击退
        if (isProjectileHit) {
            double hor_proj_mult = config.getDouble("horizontal.projectile_multiplier");
            double ver_proj_mult = config.getDouble("vertical.projectile_multiplier");

            velocity.multiply(new Vector(hor_proj_mult, ver_proj_mult, hor_proj_mult));

            if (arrowStrength > 0) {
                double adder = arrowStrength * 0.6F;
                double dist = Math.hypot(velocity.getX(), velocity.getZ());

                // 弓箭带有'击退'附魔时, 造成的垂直击退就是固定的 疾跑垂直击退
                velocity.add(new Vector(velocity.getX() * adder / dist, ver_sprint_extra, velocity.getZ() * adder / dist));
            }
        }
        // 其它生物攻击造成的击退
        else {
            // 击退附魔的等级
            int kbLevel = 0;
            ItemStack hand = attacker.getEquipment().getItemInHand();

            if (hand != null && hand.getType() != Material.AIR) {
                kbLevel = hand.getEnchantmentLevel(Enchantment.KNOCKBACK);
            }

            boolean stop_sprint = config.getBoolean("stop_sprint");
            if (stop_sprint ? bukkitSprinting : packetSprinting) ++kbLevel;

            // 原版击退附魔的处理就是: 疾跑击退 * 击退附魔等级, 例如击退2就是两倍的疾跑击退, 此时如果你再疾跑, 最终就是三倍
            if (kbLevel > 0) {
                velocity.add(new Vector(
                        -Math.sin(radianYaw) * kbLevel * hor_sprint_extra,
                        ver_sprint_extra,
                        Math.cos(radianYaw) * kbLevel * hor_sprint_extra
                ));
            }
        }

        // Y 轴击退限制
        double y_limit = config.getDouble("y_limit");
        if (!isProjectileHit && victim.getLocation().getY() - victimData.getLastGroundY() > y_limit) velocity.setY(0.0);

        KBMPlayerVelocityEvent velocityEvent = new KBMPlayerVelocityEvent(victim, velocity);

        // 触发 本插件修改玩家击退 事件
        Bukkit.getPluginManager().callEvent(velocityEvent);
        if (velocityEvent.isCancelled()) return; // 事件被取消, 返回

        // 攻击速度
        int hit_delay = config.getInt("hit_delay");
        if (victim.getMaximumNoDamageTicks() != hit_delay) victim.setMaximumNoDamageTicks(hit_delay);

        // 缓存玩家被修改过的击退, 至击退事件中修改
        victimData.setVelocity(velocityEvent.getVelocity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onVelocity(PlayerVelocityEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());
        if (data == null || data.getVelocity() == null) return;

        // 替换击退为本插件修改过后的击退
        event.setVelocity(data.getVelocity());

        // 清空缓存的击退
        data.setVelocity(null);
    }

    @EventHandler
    private void onToggleSprint(PlayerToggleSprintEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());

        // 玩家数据包切换疾跑状态
        if (data != null) data.setSprinting(event.isSprinting());
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        Location to = event.getTo(), from = event.getFrom();

        // 只移动视角, 位置没动过, 不需要处理, 跳过
        if (to.getX() == from.getX() && to.getY() == from.getY() && to.getZ() == from.getZ()) return;

        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());
        if (data == null) return;

        // 记录玩家最后一次触碰到地面的Y轴坐标
        if (data.isOnGround()) data.setLastGroundY(to.getY());
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());

        // 将最后地面Y轴设置为传送位置, 修复: 在传送后从未移动过时, 第一次被攻击, 会误触 y_limit 的问题
        if (data != null) data.setLastGroundY(event.getTo().getY());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();

        switch (command) {
            case "/kb":
            case "/kbm":
            case "/knockback":
            case "/knockbackmanager:kb":
            case "/knockbackmanager:kbm":
            case "/knockbackmanager:knockback":
                event.getPlayer().sendMessage(KnockbackManager.PREFIX + " §7此服务器正在使用 §fKnockback Manager§7(v"
                        + plugin.getDescription().getVersion() + ") 击退修改插件");
                break;
        }
    }

    @EventHandler
    private void onJoin(PlayerJoinEvent event) {
        plugin.getDataManager().create(event.getPlayer());
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getDataManager().delete(player.getUniqueId());
        plugin.getPacketHandler().onQuit(player);
    }
}
