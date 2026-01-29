package me.dw1e.kbm.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.data.PlayerData;
import me.dw1e.kbm.util.MathUtil;
import org.bukkit.Location;
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

        Player viewer = event.getPlayer();
        UUID viewerUUID = viewer.getUniqueId();

        PlayerData viewerData = plugin.getDataManager().getData(viewerUUID);
        if (viewerData == null || viewerData.isFilter()) return;

        PacketContainer packet = event.getPacket();
        StructureModifier<Integer> integers = packet.getIntegers();

        int entityId = integers.read(0);
        if (entityId == viewer.getEntityId()) return;

        FileConfiguration config = plugin.getKbFile().getKbMap().get(viewerData.getKbFilename()).getValue();

        // 处理错位
        process_misplace:
        {
            if (!event.getPacketType().equals(PacketType.Play.Server.ENTITY_TELEPORT)
                    || !config.getBoolean("packet.misplace.enabled")) break process_misplace;

            double entityX = integers.read(1) / 32.0D;
            double entityZ = integers.read(3) / 32.0D;

            Location viewerLoc = viewer.getLocation();

            float angle = MathUtil.getAngle(entityX, entityZ, viewerLoc.getX(), viewerLoc.getZ());

            double misplace = config.getDouble("packet.misplace.distance");

            double offsetX = Math.cos(Math.toRadians(angle)) * misplace;
            double offsetZ = Math.sin(Math.toRadians(angle)) * misplace;

            integers.write(1, MathUtil.floor((entityX + offsetX) * 32.0D));
            integers.write(3, MathUtil.floor((entityZ + offsetZ) * 32.0D));
        }

        int currentTick = plugin.getTicks();

        // 处理延迟更新位置包
        process_delay:
        {
            // 防止死循环
            Set<String> set = expectedPackets.get(viewerUUID);
            if (set != null) {
                String key = buildKey(event.getPacketType(), entityId, currentTick);
                if (set.remove(key)) break process_delay;
            }

            if (!config.getBoolean("packet.delay.enabled")) break process_delay;

            // 不是攻击者的实体移动包
            if (entityId != viewerData.getAttackerEntityId()) break process_delay;

            int delay = Math.max(1, config.getInt("packet.delay.ticks"));

            PacketContainer cloned = packet.deepClone();
            event.setCancelled(true);

            packetQueues
                    .computeIfAbsent(viewerUUID, k -> new ConcurrentLinkedDeque<>())
                    .addLast(new QueuedPacket(viewer, cloned, currentTick + delay));
        }
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
