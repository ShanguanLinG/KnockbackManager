package me.dw1e.kbm.listener;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.api.event.KBMPlayerVelocityEvent;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.config.KBProfile;
import me.dw1e.kbm.data.PlayerData;
import me.dw1e.kbm.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

public final class VelocityListener implements Listener {

    private final KnockbackManager plugin;

    public VelocityListener(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void computeVelocity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();

        PlayerData victimData = plugin.getDataManager().getData(victim.getUniqueId());
        if (victimData == null || victimData.isExcluded()) return;

        Entity source = event.getDamager();
        LivingEntity attackerEntity;

        boolean isProjectileHit = false, isSelfShootHit = false;
        int arrowStrength = 0; // 弓箭上的'冲击'附魔等级

        KBProfile profile = plugin.getKbFile().getProfile(victimData.getProfile());
        if (profile == null) return;

        if (source instanceof LivingEntity) { // 玩家造成的击退
            attackerEntity = (LivingEntity) source;

        } else if (source instanceof Projectile) { // 投掷物造成的击退
            if (source instanceof EnderPearl) return;

            if (!profile.PROJECTILE_ENABLED) return; // 未启用投掷物击退修改

            ProjectileSource shooter = ((Projectile) source).getShooter();

            if (!(shooter instanceof LivingEntity)) return;

            // 本次击退为投掷物造成的
            isProjectileHit = true;

            attackerEntity = (LivingEntity) shooter;

            // 本次击退的攻击者是自己, 例如: Bow Boost
            isSelfShootHit = attackerEntity.getEntityId() == victim.getEntityId();

            // 获取弓箭的冲击附魔等级
            if (source.getType() == EntityType.ARROW) {
                arrowStrength = ((Arrow) source).getKnockbackStrength();
            }

        } else return;

        Location victimLoc = victim.getLocation();
        Location attackerLoc = attackerEntity.getLocation();

        double deltaX = victimLoc.getX() - attackerLoc.getX();
        double deltaZ = victimLoc.getZ() - attackerLoc.getZ();

        // 防止坐标一致时, 水平击退为0的问题 (原版处理逻辑)
        while (MathUtil.hypot(deltaX, deltaZ) < 1E-4) {
            double random = (Math.random() - Math.random()) * 0.01;

            deltaX = random;
            deltaZ = random;

            victimData.sendDebugMessage("距离过近, 随机击退方向");
        }

        float radianYaw = attackerLoc.getYaw() * 0.017453292F;
        double sinYaw = Math.sin(radianYaw);
        double cosYaw = Math.cos(radianYaw);

        Vector velocity;

        if (isSelfShootHit) { // Bow Boost 的击退
            if (!profile.PROJECTILE_DIRECTION_OVERRIDE) {
                // 未开启覆盖: 使用投掷物自身的飞行方向
                velocity = source.getVelocity().clone().normalize().setY(1.0);
            } else {
                // 开启覆盖: 按玩家视角朝向造成击退
                velocity = new Vector(-sinYaw, 1.0, cosYaw);
            }

        } else {
            if (isProjectileHit && !profile.PROJECTILE_DIRECTION_OVERRIDE) {
                // 投掷物命中且未开启覆盖: 使用投掷物飞行方向
                velocity = source.getVelocity().clone().normalize().setY(1.0);
            } else {
                // 近战攻击, 或开启覆盖时: 使用 attacker → victim 的位置方向
                double dist = MathUtil.hypot(deltaX, deltaZ);

                velocity = new Vector(deltaX / dist, 1.0, deltaZ / dist);
            }
        }

        // 玩家发送的数据包地面状态, 可被外挂欺骗
        boolean onGround = victimData.isOnGround();

        // 根据判断玩家是否在地面上来决定该应用哪个击退
        double hor = onGround ? profile.HORIZONTAL_GROUND : profile.HORIZONTAL_AIR;
        double ver = onGround ? profile.VERTICAL_GROUND : profile.VERTICAL_AIR;

        // 应用基础击退
        velocity.setX(velocity.getX() * hor);
        velocity.setY(velocity.getY() * ver);
        velocity.setZ(velocity.getZ() * hor);

        // 投掷物造成的击退
        if (isProjectileHit) {
            velocity.setX(velocity.getX() * profile.PROJECTILE_HORIZONTAL_MULTIPLIER);
            velocity.setY(velocity.getY() * profile.PROJECTILE_VERTICAL_MULTIPLIER);
            velocity.setZ(velocity.getZ() * profile.PROJECTILE_HORIZONTAL_MULTIPLIER);

            if (arrowStrength > 0) {
                double vx = velocity.getX();
                double vz = velocity.getZ();

                double dist = MathUtil.hypot(vx, vz);
                if (dist > 1.0E-6) {

                    double multiplier = 1.0 + (arrowStrength * 0.6F) / dist;

                    velocity.setX(vx * multiplier);
                    velocity.setZ(vz * multiplier);
                }
            }
        }
        // 其它生物攻击造成的击退
        else {
            // 击退附魔的等级
            int kbLevel = 0;

            EntityEquipment equipment = attackerEntity.getEquipment();
            if (equipment != null) {
                ItemStack hand = equipment.getItemInHand();

                // 1.8和高版本有变化, 别改这块
                if (hand != null && hand.getType() != Material.AIR) {
                    kbLevel = hand.getEnchantmentLevel(Enchantment.KNOCKBACK);
                }
            }

            boolean sprinting = computeSprint(profile, attackerEntity);

            // 原版击退附魔的处理就是: 疾跑击退 * 击退附魔等级, 例如击退2就是两倍的疾跑击退, 此时如果你再疾跑, 最终就是三倍
            if (sprinting) ++kbLevel;

            if (kbLevel > 0) {
                double horizontal = kbLevel * profile.HORIZONTAL_SPRINT_EXTRA;

                velocity.setX(velocity.getX() - sinYaw * horizontal);
                velocity.setY(velocity.getY() + profile.VERTICAL_SPRINT_EXTRA);
                velocity.setZ(velocity.getZ() + cosYaw * horizontal);
            }
        }

        if (profile.MODERN_NETHERITE_KB_RESISTANCE && plugin.isAtLeast1_16()) {
            AttributeInstance attribute = victim.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);

            if (attribute != null) {
                double resistance = 1.0 - Math.max(0.0, Math.min(1.0, attribute.getValue()));

                velocity.multiply(resistance);
            }
        }

        // Y 轴击退限制
        if (!isProjectileHit && victimLoc.getY() - victimData.getLastGroundY() > profile.Y_LIMIT) {
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
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null || data.getVelocity() == null) return;

        // 替换击退为本插件修改过后的击退
        event.setVelocity(data.getVelocity());

        // 清空缓存的击退
        data.setVelocity(null);
    }

    private boolean computeSprint(KBProfile profile, LivingEntity attackerEntity) {
        // 只有玩家可以将疾跑, 其它生物造成的均为非疾跑击退
        if (!(attackerEntity instanceof Player)) return false;

        Player attackerPlayer = (Player) attackerEntity;
        PlayerData attackerData = plugin.getDataManager().getData(attackerEntity.getUniqueId());

        if (profile.MODERN_COOLDOWN_AFFECTS_KB && plugin.isAtLeast1_16()) {
            float cooldown = ConfigValue.HIT_DETECTION_ENABLED
                    ? attackerData.getCacheCooldown() : attackerPlayer.getAttackCooldown();

            // 高版本需要冷却达标才可触发疾跑击退
            if (cooldown <= 0.9F) return false;
        }

        if (profile.STOP_SPRINT) {
            // 服务器状态 (攻击后会被重置)
            return attackerPlayer.isSprinting();
        } else {
            // 客户端状态
            return attackerData.isSprinting();
        }
    }

}
