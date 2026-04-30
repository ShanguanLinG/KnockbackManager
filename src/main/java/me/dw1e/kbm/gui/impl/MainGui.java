package me.dw1e.kbm.gui.impl;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.gui.Gui;
import me.dw1e.kbm.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Collections;

public final class MainGui extends Gui {

    public MainGui() {
        super(27, "Knockback Manager");

        getInventory().setItem(10, new ItemBuilder(Material.PAPER, "§f重载主配置")
                .setLore(Collections.singletonList("§7点击重新加载主配置文件")).build());

        getInventory().setItem(13, new ItemBuilder(Material.BOOK, "§f编辑 KB 数值")
                .setLore(Collections.singletonList("§7点击打开编辑 KB 数值页")).build());

        getInventory().setItem(16, new ItemBuilder(Material.WATER_BUCKET, "§f重载所有 KB 配置")
                .setLore(Collections.singletonList("§7点击重新加载所有 KB 配置文件")).build());
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int value = event.getSlot();

        HumanEntity clicker = event.getWhoClicked();

        KnockbackManager kbm = KnockbackManager.getInstance();

        if (value == 10) {
            kbm.reload();

            clicker.sendMessage(ConfigValue.PREFIX + " §a已重载主配置文件");

            clicker.closeInventory();
        } else if (value == 13) {
            kbm.getGuiManager().getKbListGui().openGui(clicker);

        } else if (value == 16) {
            kbm.getKBLoader().reload("*", clicker);
            kbm.getGuiManager().disable();
            kbm.getGuiManager().enable();

            clicker.closeInventory();
        }
    }

}