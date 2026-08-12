package me.dw1e.kbm.listener;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.data.PlayerData;
import me.dw1e.kbm.gui.Gui;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.projectiles.ProjectileSource;

public final class PlayerStateListener implements Listener {

    private final KnockbackManager plugin;

    public PlayerStateListener(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getDataManager().create(event.getPlayer());
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getDataManager().delete(player.getUniqueId());
        plugin.getPacketHandler().onQuit(player);
    }

    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());

        // 玩家数据包切换疾跑状态
        if (data != null) data.setSprinting(event.isSprinting());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Entity source = event.getDamager();
        Player attacker;

        if (source instanceof Player) {
            attacker = (Player) event.getDamager();

        } else if (source instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) source).getShooter();

            if (!(shooter instanceof Player)) return;

            attacker = (Player) ((Projectile) source).getShooter();

        } else return;

        PlayerData attackerData = plugin.getDataManager().getData(attacker.getUniqueId());
        if (attackerData == null) return;

        int tick = plugin.getTick();

        if (event.getEntity() instanceof Player) {
            Player victim = (Player) event.getEntity();

            attackerData.setTarget(victim);

            PlayerData victimData = plugin.getDataManager().getData(victim.getUniqueId());

            if (victimData != null) {
                victimData.setAttacker(attacker);
                victimData.setLastAttackedByOtherTick(tick);
            }
        }

        attackerData.setLastAttackTick(tick);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Location to = event.getTo(), from = event.getFrom();
        if (to == null) return;

        // 只移动视角, 位置没动过, 不需要处理, 跳过
        if (to.getX() == from.getX() && to.getY() == from.getY() && to.getZ() == from.getZ()) return;

        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());
        if (data == null) return;

        if (data.isOnGround()) {
            data.setLastGroundY(to.getY()); // 记录玩家最后一次触碰到地面的Y轴坐标
            data.setTouchedTop(false); // 碰地则结束触顶
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Location to = event.getTo();
        if (to == null) return;

        PlayerData data = plugin.getDataManager().getData(event.getPlayer().getUniqueId());

        // 将最后地面Y轴设置为传送位置, 修复: 在传送后从未移动过时, 第一次被攻击, 会误触 y_limit 的问题
        if (data != null) data.setLastGroundY(to.getY());
    }

    @EventHandler // GUI中的点击事件
    private void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (top.getHolder() instanceof Gui) {
            event.setCancelled(true);

            Inventory clicked = event.getClickedInventory();

            if (clicked != null && clicked.getHolder() != null && clicked.equals(top)) {
                ((Gui) clicked.getHolder()).onClick(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase();

        switch (command) {
            case "/kb":
            case "/kbm":
            case "/knockback":
            case "/knockbackmanager:kb":
            case "/knockbackmanager:kbm":
            case "/knockbackmanager:knockback":
                event.getPlayer().sendMessage(ConfigValue.PREFIX + " §7此服务器正在使用 §fKnockback Manager§7(v"
                        + plugin.getDescription().getVersion() + ") 击退修改插件");
                break;
        }
    }

}
