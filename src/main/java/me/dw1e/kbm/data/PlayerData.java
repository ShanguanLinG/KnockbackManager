package me.dw1e.kbm.data;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PlayerData {

    private final Player player;

    private String kbFilename = "default";
    private Vector velocity = null;

    private boolean sprinting, filter;
    private double lastGroundY;

    private int attackerEntityId = -1;

    private int lastMisplacedTicks;

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

    public void setLastGroundY(double lastGroundY) {
        this.lastGroundY = lastGroundY;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    public int getAttackerEntityId() {
        return attackerEntityId;
    }

    public void setAttackerEntityId(int id) {
        attackerEntityId = id;
    }

    public int getLastMisplacedTicks() {
        return lastMisplacedTicks;
    }

    public void setLastMisplacedTicks(int ticks) {
        lastMisplacedTicks = ticks;
    }
}
