package me.dw1e.kbm.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.data.PlayerData;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class KBMPlaceholder extends PlaceholderExpansion {

    private final KnockbackManager plugin;

    public KBMPlaceholder(KnockbackManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getAuthor() {
        return "dw1e";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "kbm";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null) return "玩家数据未加载";

        if (identifier.equals("current_profile")) {
            return data.getProfile();
        }

        if (identifier.equals("excluded")) {
            return String.valueOf(data.isExcluded());
        }

        return null;
    }

}
