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
import java.util.concurrent.ConcurrentLinkedDeque;

public final class PacketHandler extends PacketAdapter {

    private static final List<PacketType> MISPLACED_PACKETS = Arrays.asList(
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.ENTITY_LOOK
    );

    private final KnockbackManager plugin;

    private final Map<UUID, Deque<QueuedPacket>> packetQueues = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> expectedPackets = new ConcurrentHashMap<>();

    public PacketHandler(KnockbackManager plugin) {
        super(plugin, ListenerPriority.LOWEST, MISPLACED_PACKETS);
        this.plugin = plugin;
    }

    public void enable() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    public void disable() {
        packetQueues.clear();
        expectedPackets.clear();

        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }

    public void onQuit(Player player) {
        UUID uuid = player.getUniqueId();

        packetQueues.remove(uuid);
        expectedPackets.remove(uuid);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (!MISPLACED_PACKETS.contains(event.getPacketType())) return;

        Player victim = event.getPlayer();
        UUID uuid = victim.getUniqueId();

        PlayerData victimData = plugin.getDataManager().getData(uuid);
        if (victimData == null) return;

        int entityId = event.getPacket().getIntegers().read(0);

        // 防止死循环
        Set<String> set = expectedPackets.get(uuid);
        if (set != null) {
            String key = buildKey(event.getPacketType(), entityId, plugin.getTicks());
            if (set.remove(key)) return;
        }

        FileConfiguration config = plugin.getKbFile().getKbMap().get(victimData.getKbFilename()).getValue();

        if (!config.getBoolean("misplace.enabled")) return;

        // 不是攻击者的实体移动包
        if (entityId != victimData.getAttackerEntityId()) return;

        int currentTick = plugin.getTicks();
        int delay = Math.max(1, config.getInt("misplace.delay"));

        PacketContainer cloned = event.getPacket().deepClone();
        event.setCancelled(true);

        victimData.setLastMisplacedTicks(currentTick);

        packetQueues
                .computeIfAbsent(uuid, k -> new ConcurrentLinkedDeque<>())
                .addLast(new QueuedPacket(victim, cloned, currentTick + delay));
    }

    public void tick(int currentTick) {
        for (Iterator<Map.Entry<UUID, Deque<QueuedPacket>>> it = packetQueues.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Deque<QueuedPacket>> entry = it.next();
            Deque<QueuedPacket> queue = entry.getValue();

            while (true) {
                QueuedPacket queued = queue.peekFirst();
                if (queued == null || currentTick < queued.sendTick) break;

                String key = buildKey(
                        queued.packet.getType(),
                        queued.packet.getIntegers().read(0),
                        queued.sendTick
                );

                expectedPackets.computeIfAbsent(entry.getKey(), k -> ConcurrentHashMap.newKeySet()).add(key);

                ProtocolLibrary.getProtocolManager().sendServerPacket(queued.player, queued.packet);

                queue.pollFirst();
            }

            // 队列空了就清掉, 避免 Map 无限增长
            if (queue.isEmpty()) it.remove();
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
