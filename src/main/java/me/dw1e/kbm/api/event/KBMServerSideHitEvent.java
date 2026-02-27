package me.dw1e.kbm.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class KBMServerSideHitEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final Player attacker, victim;
    private boolean cancelled;

    public KBMServerSideHitEvent(Player attacker, Player victim) {
        this.attacker = attacker;
        this.victim = victim;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    public Player getAttacker() {
        return attacker;
    }

    public Player getVictim() {
        return victim;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
