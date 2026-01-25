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

    public boolean isKBFileExist(String filename) {
        return plugin.getKbFile().getKbMap().containsKey(filename);
    }

    public String getKBFile(Player player) {
        return getData(player).getKbFilename();
    }

    public boolean setKBFile(Player player, String filename) {
        if (!isKBFileExist(filename)) return false;

        getData(player).setKbFilename(filename);
        return true;
    }

    public boolean isFilter(Player player) {
        return getData(player).isFilter();
    }

    public void setFilter(Player player, boolean toggle) {
        getData(player).setFilter(toggle);
    }

    public boolean isMisplacing(Player player) {
        PlayerData data = getData(player);

        FileConfiguration kbConfig = getKBConfig(data.getKbFilename());

        return kbConfig != null && plugin.getTicks() - data.getLastMisplacedTicks() < kbConfig.getInt("misplace.delay");
    }

    public FileConfiguration getKBConfig(String filename) {
        return isKBFileExist(filename) ? plugin.getKbFile().getKbMap().get(filename).getValue() : null;
    }
}
