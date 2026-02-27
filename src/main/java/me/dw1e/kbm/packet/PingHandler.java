package me.dw1e.kbm.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.data.PlayerData;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public final class PingHandler extends PacketAdapter {

    private final KnockbackManager plugin;

    public PingHandler(KnockbackManager plugin) {
        super(plugin, plugin.isAtLeast1_17() ? PacketType.Play.Client.PONG : PacketType.Play.Client.TRANSACTION);
        this.plugin = plugin;
    }

    public void enable() {
        plugin.getProtocolManager().addPacketListener(this);
    }

    public void disable() {
        plugin.getProtocolManager().removePacketListener(this);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();

        PlayerData data = plugin.getDataManager().getData(player.getUniqueId());
        if (data == null) return;

        PacketContainer container = event.getPacket();
        Integer id = null;

        PacketType packetType = event.getPacketType();

        if (packetType == PacketType.Play.Client.PONG) {
            id = container.getIntegers().read(0);
        } else if (packetType == PacketType.Play.Client.TRANSACTION) {
            id = (int) container.getShorts().read(0);
        }

        if (id == null) return;

        Long sendTime = data.getTimeline().remove(id);
        if (sendTime == null || !ConfigValue.KB_SYNC) return;

        long now = System.currentTimeMillis();
        int ping = (int) (now - sendTime);

        if (data.getPing() == -1) {
            data.setLastPing(ping);
        } else {
            data.setLastPing(data.getPing());
        }

        data.setPing(ping);
    }

    public void tick() {
        plugin.getDataManager().getAllData().forEach(PlayerData::sendPing);
    }
}