package me.dw1e.kbm.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.dw1e.kbm.KnockbackManager;
import me.dw1e.kbm.config.ConfigValue;
import me.dw1e.kbm.util.MathUtil;
import me.dw1e.kbm.util.VersionUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class PlayerData {

    private final Player player;
    private final Map<Integer, Long> timeline = new HashMap<>();

    private String profile = "default";
    private Vector velocity = null;

    private boolean excluded;
    private boolean debugging;

    private boolean sprinting;
    private double lastGroundY;

    private float cacheCooldown;

    private int lastAttackTick;
    private int lastAttackedByOtherTick;
    private int lastDamageTick;

    private Player target, attacker;

    private int transId;
    private int lastPing = -1, ping = -1;
    private long lastSendPing;

    public PlayerData(Player player) {
        this.player = player;
        lastGroundY = player.getLocation().getY();
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    @SuppressWarnings("deprecation")
    public boolean isOnGround() {
        boolean playerGround = player.isOnGround();

        if (!ConfigValue.KB_SYNC_ENABLED) return playerGround;

        playerGround |= computeGround(player.getVelocity().getY());

        return playerGround;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public float getCacheCooldown() {
        return cacheCooldown;
    }

    public void setCacheCooldown(float cacheCooldown) {
        this.cacheCooldown = cacheCooldown;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
        player.setMaximumNoDamageTicks(20);
    }

    public boolean isDebugging() {
        return debugging;
    }

    public void setDebugging(boolean debugging) {
        this.debugging = debugging;
    }

    public double getLastGroundY() {
        return lastGroundY;
    }

    public void setLastGroundY(double y) {
        this.lastGroundY = y;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public int getLastAttackedByOtherTick() {
        return lastAttackedByOtherTick;
    }

    public void setLastAttackedByOtherTick(int tick) {
        this.lastAttackedByOtherTick = tick;
    }

    public int getLastAttackTick() {
        return lastAttackTick;
    }

    public void setLastAttackTick(int tick) {
        this.lastAttackTick = tick;
    }

    public int getLastDamageTick() {
        return lastDamageTick;
    }

    public void setLastDamageTick(int tick) {
        this.lastDamageTick = tick;
    }

    public Player getTarget() {
        return target;
    }

    public void setTarget(Player target) {
        this.target = target;
    }

    public Player getAttacker() {
        return attacker;
    }

    public void setAttacker(Player attacker) {
        this.attacker = attacker;
    }

    public void sendDebugMessage(String message) {
        if (debugging) player.sendMessage(ConfigValue.PREFIX + " §8[§3Debug§8] §7" + message);
    }

    public Map<Integer, Long> getTimeline() {
        return timeline;
    }

    public void setLastPing(int lastPing) {
        this.lastPing = lastPing;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    @SuppressWarnings("deprecation")
    public void sendPing() {
        KnockbackManager kbm = KnockbackManager.getInstance();

        int tick = kbm.getTick();

        long now = System.currentTimeMillis();

        if (tick - lastAttackTick > 40 && tick - lastAttackedByOtherTick > 40) {
            if (now - lastSendPing <= 1000L) return; // 脱战, 1秒发1次
        } else {
            if (now - lastSendPing <= 200L) return; // 战斗中, 1秒发5次
        }

        lastSendPing = now;

        PacketContainer packet;

        if (kbm.isAtLeast1_17()) {
            packet = new PacketContainer(PacketType.Play.Server.PING);

            packet.getIntegers().write(0, transId);
        } else {
            packet = new PacketContainer(PacketType.Play.Server.TRANSACTION);

            packet.getIntegers().write(0, 0);
            packet.getShorts().write(0, (short) transId);
            packet.getBooleans().write(0, false);
        }

        kbm.getProtocolManager().sendServerPacket(player, packet);

        timeline.put(transId, System.currentTimeMillis());

        int newId;

        do {
            newId = -ThreadLocalRandom.current().nextInt(Short.MAX_VALUE);
        } while (newId == transId);

        transId = newId;
    }

    private double getDistToGround() {
        World world = player.getWorld();
        Location location = player.getLocation();

        double minY = location.getY();
        double lowest = 5;

        double x = location.getX();
        double z = location.getZ();

        double[][] corners = {{x, z}, {x, z + 0.3}, {x + 0.3, z}, {x + 0.3, z + 0.3}}; // 近似玩家 bounding box

        for (double[] corner : corners) {
            int cx = (int) Math.floor(corner[0]);
            int cz = (int) Math.floor(corner[1]);

            for (int i = 0; i <= 5; i++) {
                Block block = world.getBlockAt(cx, (int) Math.floor(minY) - i, cz);

                if (block.getType().isSolid()) {
                    double dist = minY - (block.getY() + 1.62);
                    lowest = Math.min(lowest, dist);
                    break;
                }
            }
        }

        return Math.min(lowest, 5);
    }

    private long getEstimatedPing() {
        int nmsPing = VersionUtil.getPing(player);

        long currentPing = ping != -1 ? ping : nmsPing;
        long previousPing = lastPing != -1 ? lastPing : nmsPing;

        long ping = (currentPing - previousPing > 20) ? previousPing : currentPing;

        return Math.max(1, ping - 25);
    }

    private boolean computeGround(double verticalMotion) {
        Block under = player.getLocation().getBlock();
        Material material = under.getType();

        if (ping < 25 || under.isLiquid()
                || material.name().contains("WEB")
                || material.name().equals("SCAFFOLDING")
        ) return false;

        if (KnockbackManager.getInstance().isAtLeast1_16()) {
            if (player.isGliding()) return false;
        }

        double gDist = getDistToGround();
        if (gDist <= 0) return false;

        int tMax = verticalMotion > 0 ? MathUtil.calculateTimeToMaxVelocity(verticalMotion) : 0;
        double mH = verticalMotion > 0 ? MathUtil.calculateDistanceTraveled(verticalMotion, tMax) : 0;
        int tFall = MathUtil.calculateFallTime(verticalMotion, mH + gDist);

        return getEstimatedPing() >= tMax + tFall / 20.0 * 1000 && gDist <= 1.3;
    }
}
