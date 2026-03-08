package me.dw1e.kbm.gui;

import me.dw1e.kbm.KnockbackManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public abstract class Gui implements InventoryHolder {

    private final ItemStack backButton;
    private final Inventory inventory;

    public Gui(int size, String title) {
        inventory = Bukkit.createInventory(this, size, title);

        String back = "§c返回";
        List<String> lore = Collections.singletonList("§7点击返回上一页");

        Material material = getRedPaneMaterial();

        ItemBuilder builder = new ItemBuilder(material, back).setLore(lore);

        // 1.8 需要设置 damage 值
        if (!KnockbackManager.getInstance().isAtLeast1_13()) builder.setDamage(14);

        backButton = builder.build();
    }

    private Material getRedPaneMaterial() {
        Material material = Material.matchMaterial("RED_STAINED_GLASS_PANE");
        if (material != null) return material;

        return Material.matchMaterial("STAINED_GLASS_PANE"); // 1.12- fallback
    }

    public void openGui(HumanEntity player) {
        if (inventory != null) player.openInventory(inventory);
    }

    public abstract void onClick(InventoryClickEvent event);

    public Inventory getInventory() {
        return inventory;
    }

    protected ItemStack getBackButton() {
        return backButton;
    }
}
