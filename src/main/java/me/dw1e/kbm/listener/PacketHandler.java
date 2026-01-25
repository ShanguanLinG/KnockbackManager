package me.dw1e.kbm.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.data.PlayerData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class PacketHandler extends PacketAdapter {

    private static final List<PacketType> MISPLACED_PACKETS = Arrays.asList(
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.ENTITY_LOOK
    );

    private final KnockbackManager plugin;

    private final Queue<QueuedPacket> queue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Set<String>> expectedPackets = new ConcurrentHashMap<>();

    public PacketHandler(KnockbackManager plugin) {
        super(plugin, ListenerPriority.LOW, MISPLACED_PACKETS);
        this.plugin = plugin;
    }

    public void enable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    public void disable() {
        queue.clear();
        expectedPackets.clear();
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }

    public void onQuit(Player player) {
        queue.removeIf(q -> q.player.equals(player));
        expectedPackets.remove(player.getUniqueId());
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (!MISPLACED_PACKETS.contains(event.getPacketType())) return;

        Player victim = event.getPlayer();
        PlayerData victimData = plugin.getDataManager().getData(victim.getUniqueId());
        if (victimData == null) return;

        Set<String> set = expectedPackets.get(victim.getUniqueId());
        if (set != null) {
            String key = buildKey(event.getPacketType(), event.getPacket().getIntegers().read(0), plugin.getTicks());

            if (set.remove(key)) return;
        }

        FileConfiguration config = plugin.getKbFile().getKbMap().get(victimData.getKbFilename()).getValue();
        if (!config.getBoolean("misplace.enabled")) return;

        int entityId = event.getPacket().getIntegers().read(0);
        if (entityId != victimData.getAttackerEntityId()) return;

        int ticks = plugin.getTicks();

        PacketContainer clonedPacket = event.getPacket().deepClone();

        event.setCancelled(true);
        victimData.setLastMisplacedTicks(ticks);

        int delay = Math.max(1, config.getInt("misplace.delay"));
        queue.add(new QueuedPacket(victim, clonedPacket, ticks + delay));
    }

    public void tick(int currentTick) {
        QueuedPacket queued;

        while ((queued = queue.peek()) != null) {
            if (currentTick < queued.sendTick) break;

            String key = buildKey(
                    queued.packet.getType(),
                    queued.packet.getIntegers().read(0),
                    queued.sendTick
            );

            expectedPackets.computeIfAbsent(queued.player.getUniqueId(), k -> new HashSet<>()).add(key);

            ProtocolLibrary.getProtocolManager().sendServerPacket(queued.player, queued.packet);

            queue.poll();
        }
    }

    private String buildKey(PacketType type, int entityId, int tick) {
        return type.name() + ":" + entityId + ":" + tick;
    }

    private static final class QueuedPacket {
        private final Player player;
        private final PacketContainer packet;
        private final int sendTick;

        private QueuedPacket(Player player, PacketContainer packet, int sendTick) {
            this.player = player;
            this.packet = packet;
            this.sendTick = sendTick;
        }
    }
}
