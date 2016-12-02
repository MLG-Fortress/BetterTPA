package me.robomwm.BetterTPA;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by RoboMWM on 11/12/2016.
 */
public class PostTPATeleportEvent extends Event {
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
    private Player target;
    private boolean cancelled;

    PostTPATeleportEvent(Player player, Player target, boolean cancel) //Will implement more if requested to do so
    {
        this.player = player;
        this.target = target;
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

    /**
     * Nullcheck!
     * @return null if no intended target player was specified
     */
    public Player getTarget()
    {
        return this.target;
    }
}
