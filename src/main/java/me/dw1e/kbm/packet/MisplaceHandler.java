package me.dw1e.kbm.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.KBProfile;
import me.dw1e.kbm.data.PlayerData;
import me.dw1e.kbm.util.MathUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class MisplaceHandler extends PacketAdapter {

    private static final Set<PacketType> PACKETS = new HashSet<>(Arrays.asList(
            PacketType.Play.Server.ENTITY_TELEPORT,
            PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
            PacketType.Play.Server.REL_ENTITY_MOVE,
            PacketType.Play.Server.ENTITY_LOOK
    ));

    private final KnockbackManager plugin;

    private final Map<UUID, Deque<QueuedPacket>> packetQueues = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> expectedPackets = new ConcurrentHashMap<>();

    public MisplaceHandler(KnockbackManager plugin) {
        super(plugin, ListenerPriority.LOWEST, PACKETS);
        this.plugin = plugin;
    }

    public void enable() {
        plugin.getProtocolManager().addPacketListener(this);
    }

    public void disable() {
        packetQueues.clear();
        expectedPackets.clear();

        plugin.getProtocolManager().removePacketListener(this);
    }

    public void onQuit(Player player) {
        UUID uuid = player.getUniqueId();

        packetQueues.remove(uuid);
        expectedPackets.remove(uuid);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (!PACKETS.contains(event.getPacketType())) return;

        Player viewer = event.getPlayer();
        UUID viewerUUID = viewer.getUniqueId();

        PlayerData viewerData = plugin.getDataManager().getData(viewerUUID);
        if (viewerData == null || viewerData.isExcluded()) return;

        PacketContainer packet = event.getPacket();
        StructureModifier<Integer> integers = packet.getIntegers();

        int entityId = integers.read(0);
        if (entityId == viewer.getEntityId()) return;

        KBProfile profile = plugin.getKbFile().getProfile(viewerData.getProfile());
        if (profile == null) return;

        int tick = plugin.getTick();
        int lastAttackTick = viewerData.getLastAttackTick();
        int lastAttackedByOtherTick = viewerData.getLastAttackedByOtherTick();
        int noDamageTicks = viewer.getMaximumNoDamageTicks();

        // 处理错位
        process_misplace:
        {
            if (!profile.PACKET_MISPLACE_ENABLED
                    || !event.getPacketType().equals(PacketType.Play.Server.ENTITY_TELEPORT)
            ) break process_misplace;

            Player target = viewerData.getTarget();

            if (target == null || entityId != target.getEntityId()
                    || tick - lastAttackedByOtherTick <= (noDamageTicks / 2) + 3
                    || tick - lastAttackTick > (noDamageTicks / 2) + 3
            ) break process_misplace;

            Location viewerLoc = viewer.getLocation();

            double viewerX = viewerLoc.getX(), viewerZ = viewerLoc.getZ();
            double entityX, entityZ;

            StructureModifier<Double> doubles = packet.getDoubles();
            boolean modern = doubles.size() >= 3; // 1.17+开始数据包结构变化

            if (modern) {
                entityX = doubles.read(0);
                entityZ = doubles.read(2);
            } else {
                if (integers.size() < 4) break process_misplace;

                entityX = integers.read(1) / 32.0D;
                entityZ = integers.read(3) / 32.0D;
            }

            double dx = viewerX - entityX;
            double dz = viewerZ - entityZ;

            double len = dx * dx + dz * dz;
            if (len <= 0.0D) break process_misplace;

            len = Math.sqrt(len);
            dx /= len;
            dz /= len;

            double newX = entityX + dx * profile.PACKET_MISPLACE_DISTANCE;
            double newZ = entityZ + dz * profile.PACKET_MISPLACE_DISTANCE;

            if (modern) {
                doubles.write(0, newX);
                doubles.write(2, newZ);
            } else {
                integers.write(1, MathUtil.floor(newX * 32.0D));
                integers.write(3, MathUtil.floor(newZ * 32.0D));
            }
        }

        // 处理延迟更新位置包
        process_delay:
        {
            // 防止死循环
            Set<String> set = expectedPackets.get(viewerUUID);
            if (set != null) {
                String key = buildKey(event.getPacketType(), entityId, tick);
                if (set.remove(key)) break process_delay;
            }

            if (!profile.PACKET_DELAY_ENABLED) break process_delay;

            Player attacker = viewerData.getAttacker();

            if (attacker == null || entityId != attacker.getEntityId()
                    || tick - lastAttackedByOtherTick > noDamageTicks
                    || tick - lastAttackTick <= noDamageTicks
            ) break process_delay;

            int delay = Math.max(1, profile.PACKET_DELAY_TICKS);

            PacketContainer cloned = packet.deepClone();
            event.setCancelled(true);

            packetQueues
                    .computeIfAbsent(viewerUUID, k -> new ConcurrentLinkedDeque<>())
                    .addLast(new QueuedPacket(viewer, cloned, tick + delay));
        }
    }

    public void tick(int currentTick) {
        for (Iterator<Map.Entry<UUID, Deque<QueuedPacket>>> it = packetQueues.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Deque<QueuedPacket>> entry = it.next();

            UUID uuid = entry.getKey();

            Set<String> set = expectedPackets.get(uuid);
            if (set != null && set.size() > 128) set.clear();

            Deque<QueuedPacket> queue = entry.getValue();

            while (true) {
                QueuedPacket queued = queue.peekFirst();
                if (queued == null || currentTick < queued.sendTick) break;

                String key = buildKey(
                        queued.packet.getType(),
                        queued.packet.getIntegers().read(0),
                        queued.sendTick
                );

                expectedPackets.computeIfAbsent(uuid, k -> ConcurrentHashMap.newKeySet()).add(key);

                // 不要直接在最后面设置 filters: true, 不然其它使用 ProtocolLib 的插件不会收到你新发的包!
                plugin.getProtocolManager().sendServerPacket(queued.player, queued.packet);

                queue.pollFirst();
            }

            // 队列空了就清掉, 避免 Map 无限增长
            if (queue.isEmpty()) it.remove();

            Set<String> after = expectedPackets.get(uuid);
            if (after != null && after.isEmpty()) expectedPackets.remove(uuid);
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
