package me.robomwm.BetterTPA;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by RoboMWM on 11/12/2016.
 */
public class PreTPATeleportEvent extends Event {
    // Custom Event Requirements
    private static final HandlerList handlers = new HandlerList();
    public HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList()
    {
        return handlers;
    }


    private Player player;
    private boolean cancelled = false;
    String whyCancelled = null;

    PreTPATeleportEvent(Player player) //Will implement more if requested to do so
    {
        this.player = player;
    }

    public void setCancelled(boolean cancel)
    {
        this.cancelled = cancel;
    }

    public boolean isCancelled()
    {
        return this.cancelled;
    }

    public Player getPlayer()
    {
        return this.player;
    }
}
