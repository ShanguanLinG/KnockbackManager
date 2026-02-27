package me.dw1e.kbm.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.util.Vector;

public final class KBMPlayerVelocityEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private Vector velocity;
    private boolean cancel;

    public KBMPlayerVelocityEvent(Player player, Vector velocity) {
        super(player);
        this.velocity = velocity;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector velocity) {
        this.velocity = velocity;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
