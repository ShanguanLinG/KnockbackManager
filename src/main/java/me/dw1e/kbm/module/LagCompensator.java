package me.dw1e.kbm.module;

import me.dw1e.kbm.util.Pair;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.util.Vector;

import java.util.*;

public final class LagCompensator implements Listener {

    private static final int HISTORY_SIZE = 20;
    private static final int PING_OFFSET = 175;
    private static final int TIME_RESOLUTION = 40; // ms

    private final Map<UUID, List<Pair<Location, Long>>> locationTimes = new HashMap<>();

    public Location getHistoryLocation(int rewindMS, Player player) {
        List<Pair<Location, Long>> times = locationTimes.get(player.getUniqueId());
        if (times == null || times.isEmpty()) return player.getLocation();

        long currentTime = System.currentTimeMillis();
        int rewindTime = rewindMS + PING_OFFSET;
        for (int i = times.size() - 1; i >= 0; i--) {
            int elapsedTime = (int) (currentTime - times.get(i).getValue());

            if (elapsedTime >= rewindTime) {
                if (i == times.size() - 1) return times.get(i).getKey();

                double nextMoveWeight = (elapsedTime - rewindTime)
                        / (double) (elapsedTime - (currentTime - times.get(i + 1).getValue()));

                Location before = times.get(i).getKey().clone();
                Location after = times.get(i + 1).getKey();

                Vector interpolate = after.toVector().subtract(before.toVector());
                interpolate.multiply(nextMoveWeight);

                return before.add(interpolate);
            }
        }

        return times.get(0).getKey().clone();
    }

    private void processPosition(Location location, Player player) {
        List<Pair<Location, Long>> times = locationTimes.getOrDefault(player.getUniqueId(), new ArrayList<>());
        long currTime = System.currentTimeMillis();

        if (!times.isEmpty() && currTime - times.get(times.size() - 1).getValue() < TIME_RESOLUTION) return;
        times.add(new Pair<>(location, currTime));

        if (times.size() > HISTORY_SIZE) times.remove(0);
        locationTimes.put(player.getUniqueId(), times);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        processPosition(event.getTo(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRespawn(PlayerRespawnEvent event) {
        processPosition(event.getRespawnLocation(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        processPosition(event.getTo(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        processPosition(player.getLocation(), player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        locationTimes.remove(event.getPlayer().getUniqueId());
    }
}