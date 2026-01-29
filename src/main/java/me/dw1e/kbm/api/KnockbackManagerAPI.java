package me.dw1e.kbm.api;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public interface KnockbackManagerAPI {

    boolean isKBFileExist(String filename);

    String getKBFile(Player player);

    boolean setKBFile(Player player, String filename);

    boolean isFilter(Player player);

    void setFilter(Player player, boolean toggle);

    FileConfiguration getKBConfig(String filename);
}
