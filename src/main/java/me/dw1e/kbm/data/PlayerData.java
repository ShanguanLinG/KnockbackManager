package me.dw1e.kbm.data;

import me.dw1e.kbm.KnockbackManager;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class PlayerData {

    private final Player player;

    private String profile = "default";
    private Vector velocity = null;

    private boolean excluded;
    private boolean debugging;

    private boolean sprinting;
    private double lastGroundY;

    private int lastAttackTick, lastAttackedByOtherTick;

    private Player target, attacker;

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
        return player.isOnGround();
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
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
        if (debugging) player.sendMessage(KnockbackManager.PREFIX + " §8[§3Debug§8] §7" + message);
    }
}
