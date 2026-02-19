package me.dw1e.kbm.api;

import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class DefaultKnockbackManagerAPI implements KnockbackManagerAPI {

    private final KnockbackManager plugin;

    public DefaultKnockbackManagerAPI(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    private PlayerData getData(Player player) {
        return plugin.getDataManager().getData(player.getUniqueId());
    }

    @Override
    public boolean isProfileExists(String profileName) {
        return plugin.getKbFile().getKbMap().containsKey(profileName);
    }

    @Override
    public String getProfile(Player player) {
        return getData(player).getProfile();
    }

    @Override
    public boolean setProfile(Player player, String profileName) {
        if (!isProfileExists(profileName)) return false;

        getData(player).setProfile(profileName);
        return true;
    }

    @Override
    public boolean isModificationExcluded(Player player) {
        return getData(player).isExcluded();
    }

    @Override
    public void setModificationExcluded(Player player, boolean managed) {
        getData(player).setExcluded(managed);
    }

    @Override
    public FileConfiguration getProfileConfig(String profileName) {
        return isProfileExists(profileName) ? plugin.getKbFile().getConfig(profileName) : null;
    }
}
