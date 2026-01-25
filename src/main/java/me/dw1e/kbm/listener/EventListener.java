package me.dw1e.kbm.listener;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.api.event.KBMPlayerVelocityEvent;
import me.dw1e.kbm.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class EventListener implements Listener {

    private final KnockbackManager plugin;

    public EventListener(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Entity damager = event.getDamager();

        Player victim = (Player) event.getEntity(), attacker;
        PlayerData victimData = plugin.getDataManager().getData(victim.getUniqueId());

        if (victimData == null || victimData.isFilter()) return;

        boolean isProjectileHit = false, isSelfShootHit = false;

        int arrowStrength = 0;

        if (damager instanceof Player) attacker = (Player) damager;
        else if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();

            if (!(shooter instanceof Player)) return;

            isProjectileHit = true;

            attacker = (Player) shooter;

            isSelfShootHit = attacker.equals(victim);

            if (damager.getType() == EntityType.ARROW) arrowStrength = ((Arrow) damager).getKnockbackStrength();
        } else return;

        PlayerData attackerData = plugin.getDataManager().getData(attacker.getUniqueId());
        if (attackerData == null) return;

        victimData.setAttackerEntityId(attacker.getEntityId());

        FileConfiguration config = plugin.getKbFile().getKbMap().get(victimData.getKbFilename()).getValue();

        double hor_ground = config.getDouble("horizontal.ground");
        double hor_air = config.getDouble("horizontal.air");
        double hor_sprint_extra = config.getDouble("horizontal.sprint_extra");
        double hor_proj_mult = config.getDouble("horizontal.projectile_multiplier");
        double ver_ground = config.getDouble("vertical.ground");
        double ver_air = config.getDouble("vertical.air");
        double ver_sprint_extra = config.getDouble("vertical.sprint_extra");
        double ver_proj_mult = config.getDouble("vertical.projectile_multiplier");
        double y_limit = config.getDouble("y_limit");

        boolean stop_sprint = config.getBoolean("stop_sprint");

        int hit_delay = config.getInt("hit_delay");

        double deltaX = victim.getLocation().getX() - attacker.getLocation().getX();
        double deltaZ = victim.getLocation().getZ() - attacker.getLocation().getZ();

        while (Math.hypot(deltaX, deltaZ) < 0.0001) deltaX = deltaZ = (Math.random() - Math.random()) * 0.01;

        float radianYaw = attacker.getLocation().getYaw() * 0.017453292F;

        Vector velocity = isSelfShootHit
                ? new Vector(-Math.sin(radianYaw), 1.0, Math.cos(radianYaw))
                : new Vector(deltaX, 0.0, deltaZ).normalize().setY(1.0);

        boolean onGround = victimData.isOnGround();

        double hor = onGround ? hor_ground : hor_air, ver = onGround ? ver_ground : ver_air;

        velocity.multiply(new Vector(hor, ver, hor));

        if (isProjectileHit) {
            velocity.multiply(new Vector(hor_proj_mult, ver_proj_mult, hor_proj_mult));

            if (arrowStrength > 0) {
                double adder = arrowStrength * 0.6F, dist = Math.hypot(velocity.getX(), velocity.getZ());

                velocity.add(new Vector(velocity.getX() * adder / dist, ver_sprint_extra, velocity.getZ() * adder / dist));
            }
        } else {
            int kbLevel = attacker.getItemInHand().getEnchantmentLevel(Enchantment.KNOCKBACK);

            if (stop_sprint ? attacker.isSprinting() : attackerData.isSprinting()) ++kbLevel;

            if (kbLevel > 0) {
                velocity.add(new Vector(
                        -Math.sin(radianYaw) * kbLevel * hor_sprint_extra,
                        ver_sprint_extra,
                        Math.cos(radianYaw) * kbLevel * hor_sprint_extra
                ));
            }
        }

        if (!isProjectileHit && victim.getLocation().getY() - victimData.getLastGroundY() > y_limit) velocity.setY(0.0);

        KBMPlayerVelocityEvent velocityEvent = new KBMPlayerVelocityEvent(victim, velocity);

        Bukkit.getPluginManager().callEvent(velocityEvent);

        if (velocityEvent.isCancelled()) return;

        if (victim.getMaximumNoDamageTicks() != hit_delay) victim.setMaximumNoDamageTicks(hit_delay);

        victimData.setVelocity(velocityEvent.getVelocity());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onVelocity(PlayerVelocityEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());
        if (data == null || data.getVelocity() == null) return;

        event.setVelocity(data.getVelocity());
        data.setVelocity(null);
    }

    @EventHandler
    private void onToggleSprint(PlayerToggleSprintEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());

        if (data != null) data.setSprinting(event.isSprinting());
    }

    @EventHandler
    private void onMove(PlayerMoveEvent event) {
        Location to = event.getTo(), from = event.getFrom();

        if (to.getX() == from.getX() && to.getY() == from.getY() && to.getZ() == from.getZ()) return;

        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());
        if (data == null) return;

        if (data.isOnGround()) data.setLastGroundY(to.getY());
    }

    @EventHandler
    private void onTeleport(PlayerTeleportEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());
        if (data == null) return;

        double y = event.getTo().getY();

        if (y % 1.0 == 0.0) data.setLastGroundY(y);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();

        switch (command) {
            case "/kb":
            case "/kbm":
            case "/knockback":
            case "/knockbackmanager:kb":
            case "/knockbackmanager:kbm":
            case "/knockbackmanager:knockback":
                event.getPlayer().sendMessage(KnockbackManager.PREFIX + "§7此服务器正在使用 §fKnockback Manager§7(v"
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
