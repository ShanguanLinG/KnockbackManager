package me.dw1e.kbm.listener;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.api.event.KBMPlayerVelocityEvent;
import me.dw1e.kbm.config.KBProfile;
import me.dw1e.kbm.data.PlayerData;
import me.dw1e.kbm.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class VelocityListener implements Listener {

    private final KnockbackManager plugin;

    public VelocityListener(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void computeVelocity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();

        PlayerData victimData = plugin.getDataManager().getData(victim.getUniqueId());
        if (victimData == null || victimData.isExcluded()) return;

        Entity source = event.getDamager();
        LivingEntity attacker;

        boolean isProjectileHit = false, isSelfShootHit = false;
        int arrowStrength = 0; // 弓箭上的'冲击'附魔等级

        KBProfile profile = plugin.getKbFile().getProfile(victimData.getProfile());

        if (source instanceof LivingEntity) { // 玩家造成的击退
            attacker = (LivingEntity) source;

        } else if (source instanceof Projectile) { // 投掷物造成的击退
            if (source instanceof EnderPearl) return;

            if (!profile.PROJECTILE_ENABLED) return; // 未启用投掷物击退修改

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

        double deltaX = victim.getLocation().getX() - attacker.getLocation().getX();
        double deltaZ = victim.getLocation().getZ() - attacker.getLocation().getZ();

        // 防止坐标一致时, 水平击退为0的问题 (原版处理逻辑)
        while (MathUtil.hypot(deltaX, deltaZ) < 0.0001) {
            double random = (Math.random() - Math.random()) * 0.01;

            deltaX = random;
            deltaZ = random;

            victimData.sendDebugMessage("距离过近, 随机击退方向");
        }

        float radianYaw = attacker.getLocation().getYaw() * 0.017453292F;

        Vector velocity;

        if (isSelfShootHit) { // Bow Boost 的击退
            if (!profile.PROJECTILE_DIRECTION_OVERRIDE) {
                // 未开启覆盖: 使用投掷物自身的飞行方向
                velocity = source.getVelocity().clone().normalize().setY(1.0);
            } else {
                // 开启覆盖: 按玩家视角朝向造成击退
                velocity = new Vector(-Math.sin(radianYaw), 1.0, Math.cos(radianYaw));
            }

        } else {
            if (isProjectileHit && !profile.PROJECTILE_DIRECTION_OVERRIDE) {
                // 投掷物命中且未开启覆盖: 使用投掷物飞行方向
                velocity = source.getVelocity().clone().normalize().setY(1.0);
            } else {
                // 近战攻击, 或开启覆盖时: 使用 attacker → victim 的位置方向
                velocity = new Vector(deltaX, 0.0, deltaZ).normalize().setY(1.0);
            }
        }

        // 玩家发送的数据包地面状态, 可被外挂欺骗
        boolean onGround = victimData.isOnGround();

        // 根据判断玩家是否在地面上来决定该应用哪个击退
        double hor = onGround ? profile.HORIZONTAL_GROUND : profile.HORIZONTAL_AIR;
        double ver = onGround ? profile.VERTICAL_GROUND : profile.VERTICAL_AIR;

        // 应用基础击退
        velocity.multiply(new Vector(hor, ver, hor));

        PlayerData attackerData = plugin.getDataManager().getData(attacker.getUniqueId());

        // 投掷物造成的击退
        if (isProjectileHit) {
            velocity.multiply(new Vector(
                    profile.PROJECTILE_HORIZONTAL_MULTIPLIER,
                    profile.PROJECTILE_VERTICAL_MULTIPLIER,
                    profile.PROJECTILE_HORIZONTAL_MULTIPLIER)
            );

            if (arrowStrength > 0) {
                double adder = arrowStrength * 0.6F;
                double dist = MathUtil.hypot(velocity.getX(), velocity.getZ());

                // 弓箭带有'击退'附魔时, 造成的垂直击退就是固定的 疾跑垂直击退
                velocity.add(new Vector(
                        velocity.getX() * adder / dist,
                        profile.VERTICAL_SPRINT_EXTRA,
                        velocity.getZ() * adder / dist)
                );
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

            boolean sprinting = false;

            if (profile.STOP_SPRINT) {
                if (attacker instanceof Player) {
                    // Bukkit 内部状态, 在一次攻击后会被服务器强制置为 false (需要疾跑重置)
                    sprinting = ((Player) attacker).isSprinting();
                }
            } else {
                if (attackerData != null) {
                    // 客户端数据包的疾跑状态, 只要玩家未松开疾跑键就保持为 true (按住前进与疾跑即可)
                    sprinting = attackerData.isSprinting();
                }
            }

            // 原版击退附魔的处理就是: 疾跑击退 * 击退附魔等级, 例如击退2就是两倍的疾跑击退, 此时如果你再疾跑, 最终就是三倍
            if (sprinting) ++kbLevel;

            if (kbLevel > 0) {
                velocity.add(new Vector(
                        -Math.sin(radianYaw) * kbLevel * profile.HORIZONTAL_SPRINT_EXTRA,
                        profile.VERTICAL_SPRINT_EXTRA,
                        Math.cos(radianYaw) * kbLevel * profile.HORIZONTAL_SPRINT_EXTRA
                ));
            }
        }

        // Y 轴击退限制
        if (!isProjectileHit && victim.getLocation().getY() - victimData.getLastGroundY() > profile.Y_LIMIT) {
            velocity.setY(0.0);
        }

        KBMPlayerVelocityEvent velocityEvent = new KBMPlayerVelocityEvent(victim, velocity);

        // 触发 本插件修改玩家击退 事件
        Bukkit.getPluginManager().callEvent(velocityEvent);
        if (velocityEvent.isCancelled()) return; // 事件被取消, 返回

        // 攻击速度
        if (victim.getMaximumNoDamageTicks() != profile.HIT_DELAY) victim.setMaximumNoDamageTicks(profile.HIT_DELAY);

        // 缓存玩家被修改过的击退, 至击退事件中修改
        victimData.setVelocity(velocityEvent.getVelocity());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void applyVelocity(PlayerVelocityEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());
        if (data == null || data.getVelocity() == null) return;

        // 替换击退为本插件修改过后的击退
        event.setVelocity(data.getVelocity());

        // 清空缓存的击退
        data.setVelocity(null);
    }

}
