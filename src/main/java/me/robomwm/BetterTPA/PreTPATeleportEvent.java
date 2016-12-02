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
    private Location targetLocation;
    String reason = null;

    PreTPATeleportEvent(Player player, Location targetLocation) //Will implement more if requested to do so
    {
        this.player = player;
        this.targetLocation = targetLocation;
    }

    public Location getTargetLocation()
    {
        return this.getTargetLocation();
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

    /**
     * If you canceled this event, use this to tell the player why it was canceled
     * Idk if this follows the standard, but w/e makes it easier for me
     * @param reason
     */
    public void setReason(String reason)
    {
        this.reason = reason;
    }

    public String getReason()
    {
        return this.reason;
    }
}
