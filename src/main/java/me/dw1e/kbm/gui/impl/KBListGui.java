package me.dw1e.kbm.gui.impl;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.KB;
import me.dw1e.kbm.gui.Gui;
import me.dw1e.kbm.gui.GuiManager;
import me.dw1e.kbm.gui.ItemBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class KBListGui extends Gui {

    private final Map<Integer, KB> kbMap = new HashMap<>();

    public KBListGui() {
        super(45, "已加载的 KB 配置");

        for (int i = 0; i < 9; ++i) getInventory().setItem(i, getBackButton());

        int id = 9;

        for (KB kb : KnockbackManager.getInstance().getKBLoader().getKbMap().values()) {

            getInventory().setItem(id, new ItemBuilder(Material.BOOK, "§f" + kb.getName()).build());

            kbMap.put(id++, kb);
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        int value = event.getSlot();

        GuiManager guiManager = KnockbackManager.getInstance().getGuiManager();

        if (value >= 0 && value < 9) {
            guiManager.getMainGui().openGui(event.getWhoClicked());
            return;
        }

        ItemStack currentItem = event.getCurrentItem();

        if (currentItem != null && currentItem.getItemMeta() != null) {
            KB kb = kbMap.get(value);
            String displayName = ChatColor.stripColor(currentItem.getItemMeta().getDisplayName());

            if (kb != null && kb.getName().equals(displayName)) {
                guiManager.getTypeGui(kb).openGui(event.getWhoClicked());
            }
        }
    }

}