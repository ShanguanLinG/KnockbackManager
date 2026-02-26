package me.dw1e.kbm.listener;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.KBProfile;
import me.dw1e.kbm.data.PlayerData;
import me.dw1e.kbm.util.MathUtil;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class PotionListener implements Listener {

    private final KnockbackManager plugin;

    public PotionListener(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    private void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntityType() != EntityType.SPLASH_POTION) return;

        Projectile projectile = event.getEntity();
        ProjectileSource shooter = projectile.getShooter();

        if (!(shooter instanceof Player)) return;
        Player player = (Player) shooter;

        PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || data.isExcluded()) return;

        KBProfile profile = plugin.getKbFile().getProfile(data.getProfile());
        if (profile == null) return;

        if (profile.POTION_ENABLED && data.isSprinting()) {
            Vector originalVel = projectile.getVelocity();

            Vector finalVel = new Vector(
                    originalVel.getX() * profile.POTION_HORIZONTAL_MULTIPLIER,
                    originalVel.getY() * profile.POTION_VERTICAL_MULTIPLIER,
                    originalVel.getZ() * profile.POTION_HORIZONTAL_MULTIPLIER
            );

            projectile.setVelocity(finalVel);

            double originalHor = MathUtil.hypot(originalVel.getX(), originalVel.getZ());
            double finalHor = MathUtil.hypot(finalVel.getX(), finalVel.getZ());

            data.sendDebugMessage(String.format(
                    "药水运动: 原始=[水平=%.3f, 垂直=%.3f], 最终=[水平=%.3f, 垂直=%.3f]",
                    originalHor, originalVel.getY(), finalHor, finalVel.getY())
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onPotionSplash(PotionSplashEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        Player player = (Player) event.getEntity().getShooter();

        PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || data.isExcluded()) return;

        KBProfile profile = plugin.getKbFile().getProfile(data.getProfile());
        if (profile == null) return;

        if (profile.POTION_ENABLED) {
            double intensity = event.getIntensity(player);
            double finalIntensity = Math.max(0.0, Math.min(1.0, intensity * profile.POTION_COMPENSATION_MULTIPLIER));

            event.setIntensity(player, finalIntensity);

            data.sendDebugMessage(String.format("药水命中强度: 原始=%.3f, 最终=%.3f", intensity, finalIntensity));
        }
    }

}
