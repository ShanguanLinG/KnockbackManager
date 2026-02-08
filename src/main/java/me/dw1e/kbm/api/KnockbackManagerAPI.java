package me.dw1e.kbm.api;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public interface KnockbackManagerAPI {

    @Deprecated
    FileConfiguration getKBConfig(String filename);

    boolean isProfileExists(String profileName);

    String getProfile(Player player);

    boolean setProfile(Player player, String profileName);

    boolean isModificationExcluded(Player player);

    void setModificationExcluded(Player player, boolean ignore);

    FileConfiguration getProfileConfig(String profileName);
}
