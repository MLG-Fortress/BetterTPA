package me.robomwm.BetterTPA;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
    private Player target;
    private boolean cancelled = false;
    private Location targetLocation;
    private long warmup;
    private String reason = null;

    PreTPATeleportEvent(Player player, Location targetLocation, @Nullable Player target, @Nonnull long warmup) //Will implement more if requested to do so
    {
        this.player = player;
        this.targetLocation = targetLocation;
        this.target = target;
        this.warmup = warmup;
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

    public Player getTarget()
    {
        return target;
    }

    public long getWarmup()
    {
        return warmup;
    }

    public void setWarmup(long warmup)
    {
        if (warmup < 0L)
            setCancelled(true);
        this.warmup = warmup;
    }
}
