package me.dw1e.kbm.gui.impl;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.KB;
import me.dw1e.kbm.config.KBLoader;
import me.dw1e.kbm.gui.Gui;
import me.dw1e.kbm.gui.GuiManager;
import me.dw1e.kbm.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class TypeGui extends Gui {

    private final KB kb;
    private final Map<Integer, String> valueMap = new HashMap<>();

    private int index = 9;

    public TypeGui(KB kb) {
        super(45, kb.getName());
        this.kb = kb;
        build();
    }

    private void build() {
        valueMap.clear();
        index = 9;

        getInventory().clear();

        for (int i = 0; i < 9; ++i) getInventory().setItem(i, getBackButton());

        addDouble("horizontal.ground", "地面水平方向击退距离");
        addDouble("horizontal.air", "空中水平方向击退距离");
        addDouble("horizontal.sprint_extra", "疾跑时增加的水平方向击退距离");

        addDouble("vertical.ground", "地面垂直方向击退距离");
        addDouble("vertical.air", "空中垂直方向击退距离");
        addDouble("vertical.sprint_extra", "疾跑时增加的垂直方向击退距离");

        addBoolean("projectile.enabled", "是否启用投掷物击退修改");
        addDouble("projectile.horizontal_multiplier", "投掷物造成的水平方向击退距离的乘以倍数");
        addDouble("projectile.vertical_multiplier", "投掷物造成的垂直方向击退距离的乘以倍数");
        addBoolean("projectile.direction_override", "是否使用投掷物发射者的位置来计算击退方向");

        addBoolean("packet.misplace.enabled", "是否将位置更新包错位");
        addDouble("packet.misplace.distance", "错位距离 (单位: 格)");
        addBoolean("packet.delay.enabled", "是否延迟发送位置更新包");
        addInt("packet.delay.ticks", "延迟发送的时间 (单位: 游戏刻)");

        addBoolean("stop_sprint", "重置疾跑");

        addDouble("y_limit", "击退造成的 Y 轴高度限制 (单位: 格)");

        addInt("hit_delay", "攻击间隔 (单位: 游戏刻)");

        addBoolean("potion.enabled", "是否启用投掷型药水运动修改");
        addDouble("potion.horizontal_multiplier", "药水的水平方向运动的乘以倍数");
        addDouble("potion.vertical_multiplier", "药水的垂直方向运动的乘以倍数");
        addDouble("potion.compensation_multiplier", "药水命中自身时的强度补偿倍数");

        addBoolean("modern.cooldown_affects_kb", "是否让攻击冷却影响击退");
        addBoolean("modern.netherite_kb_resistance", "是否应用下界合金击退抗性");
    }

    private void addDouble(String path, String display) {
        FileConfiguration config = kb.getConfig();
        double value = config.getDouble(path);

        getInventory().setItem(index, new ItemBuilder(Material.PAPER, "§f" + display)
                .setLore(Arrays.asList(
                        "§7当前值: §e" + value,
                        "",
                        "§7左键 §a+0.005",
                        "§7右键 §c-0.005",
                        "§7Shift 左键 §a+0.02",
                        "§7Shift 右键 §c-0.02"
                )).build());

        valueMap.put(index, path);
        index++;
    }

    private void addInt(String path, String display) {
        FileConfiguration config = kb.getConfig();
        int value = config.getInt(path);

        getInventory().setItem(index, new ItemBuilder(Material.PAPER, "§f" + display)
                .setLore(Arrays.asList(
                        "§7当前值: §e" + value,
                        "",
                        "§7左键 §a+1",
                        "§7右键 §c-1"
                )).build());

        valueMap.put(index, path);
        index++;
    }

    private void addBoolean(String path, String display) {
        FileConfiguration config = kb.getConfig();
        boolean value = config.getBoolean(path);

        getInventory().setItem(index, new ItemBuilder(Material.LEVER, "§f" + display)
                .setLore(Arrays.asList(
                        "§7当前状态: " + (value ? "§a启用" : "§c关闭"),
                        "",
                        "§7点击切换"
                )).build());

        valueMap.put(index, path);
        index++;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        KnockbackManager kbm = KnockbackManager.getInstance();
        GuiManager guiManager = kbm.getGuiManager();

        if (slot >= 0 && slot < 9) {
            guiManager.getKbListGui().openGui(event.getWhoClicked());
            return;
        }

        String path = valueMap.get(slot);
        if (path == null) return;

        FileConfiguration config = kb.getConfig();

        if (config.isBoolean(path)) {
            boolean value = config.getBoolean(path);
            config.set(path, !value);

        } else if (config.isDouble(path)) {
            double value = config.getDouble(path);
            double change = event.isShiftClick() ? 0.02 : 0.005;

            if (event.isLeftClick()) value += change;
            if (event.isRightClick()) value -= change;

            value = Math.round(value * 1000.0) / 1000.0;
            config.set(path, value);

        } else if (config.isInt(path)) {
            int value = config.getInt(path);

            if (event.isLeftClick()) value += 1;
            if (event.isRightClick()) value -= 1;

            if (value < 0) value = 0;
            config.set(path, value);
        }

        KBLoader kbLoader = kbm.getKBLoader();

        kbLoader.save(kb.getName(), null);
        kbLoader.reload(kb.getName(), null);

        build();
    }
}