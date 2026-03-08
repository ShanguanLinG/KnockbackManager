package me.dw1e.kbm.gui;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class ItemBuilder {

    private final Material type;
    private final String name;
    private List<String> lore;
    private short damage = 0;

    public ItemBuilder(Material type, String name) {
        this.type = type;
        this.name = name;
    }

    public void setDamage(int damage) {
        this.damage = (short) damage;
    }

    public ItemBuilder setLore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    public ItemStack build() {
        ItemStack itemStack = new ItemStack(type);

        itemStack.setDurability(damage);

        ItemMeta itemMeta = itemStack.getItemMeta();

        if (itemMeta != null) {
            itemMeta.setDisplayName(name);
            itemMeta.setLore(lore);
        }

        itemStack.setItemMeta(itemMeta);

        return itemStack;
    }

}