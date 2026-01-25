package me.dw1e.kbm.data;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DataManager {

    private final Map<UUID, PlayerData> dataMap = new ConcurrentHashMap<>();

    public void enable() {
        Bukkit.getOnlinePlayers().forEach(this::create);
    }

    public void disable() {
        dataMap.clear();
    }

    public void create(Player player) {
        dataMap.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerData(player));
    }

    public void delete(UUID uuid) {
        dataMap.remove(uuid);
    }

    public PlayerData getData(UUID uuid) {
        return dataMap.get(uuid);
    }

    public Collection<PlayerData> getAllData() {
        return dataMap.values();
    }
}
