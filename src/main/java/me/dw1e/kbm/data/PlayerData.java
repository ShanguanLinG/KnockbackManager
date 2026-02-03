package me.dw1e.kbm.data;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PlayerData {

    private final Player player;

    private String kbFilename = "default";
    private Vector velocity = null;

    private boolean sprinting, filter;
    private double lastGroundY;

    private int lastAttackTick, lastAttackedByOtherTick;

    private Player target;

    public PlayerData(Player player) {
        this.player = player;
        lastGroundY = player.getLocation().getY();
    }

    public String getKbFilename() {
        return kbFilename;
    }

    public void setKbFilename(String kbFilename) {
        this.kbFilename = kbFilename;
    }

    @SuppressWarnings("deprecation")
    public boolean isOnGround() {
        return player.isOnGround();
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    public boolean isFilter() {
        return filter;
    }

    public void setFilter(boolean filter) {
        this.filter = filter;
        player.setMaximumNoDamageTicks(20);
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

    public Player getTarget() {
        return target;
    }

    public void setTarget(Player target) {
        this.target = target;
    }
}
