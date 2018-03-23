package me.robomwm.BetterTPA;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Robo on 4/24/2016.
 */
public class BetterTPA extends JavaPlugin implements Listener
{
    YamlConfiguration storage;
    Config config;

    Map<Player, Player> requesters = new HashMap<>();
    Set<Player> recentRequesters = new HashSet<>();
    Map<Player, PendingTeleportee> pendingTeleports = new HashMap<>();
    Map<String, LinkedHashMap<String, Boolean>> allowedPlayers = new LinkedHashMap<>();

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        config = new Config(this);
        //I use .data extension cuz reasons that have to do with how I automatically manage .yml files on my server so yea...
        //Not like they're supposed to touch this file anyways.
        File storageFile = new File(getDataFolder(), "storage.data");
        if (!storageFile.exists())
        {
            try
            {
                storageFile.createNewFile();
                storage = YamlConfiguration.loadConfiguration(storageFile);
            }
            catch (IOException e)
            {
                this.getLogger().severe("Could not create storage.data! Since I'm lazy, there currently is no \"in memory\" option. Will now disable along with a nice stack trace for you to bother me with:");
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        }
        else
            storage = YamlConfiguration.loadConfiguration(storageFile);

        ConfigurationSection allowedPlayersSection = storage.getConfigurationSection("allowedPlayers");

        //Set variables/shortcuts
        if (allowedPlayersSection == null)
            return;
        for (String uuid : allowedPlayersSection.getKeys(false))
        {
            LinkedHashMap<String, Boolean> allowedPlayerThingy = new LinkedHashMap<>();
            for (String allowedUUIDs : allowedPlayersSection.getConfigurationSection(uuid).getKeys(false))
                allowedPlayerThingy.put(allowedUUIDs, (Boolean)allowedPlayersSection.getConfigurationSection(uuid).get(allowedUUIDs));
            allowedPlayers.put(uuid, allowedPlayerThingy);
        }
    }

    public void onDisable()
    {
        File storageFile = new File(getDataFolder(), "storage.data");
        if (storage != null)
        {
            try
            {
                storage.set("allowedPlayers", allowedPlayers);
                storage.save(storageFile);
            }
            catch (IOException e) //really
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check a player's /tpacceptance status or w/e
     */
    private Boolean isAllowed(String playerUUID, String targetUUID)
    {
        Boolean result;
        //Check if target has allowed anyone
        if (!allowedPlayers.containsKey(targetUUID))
            result = null;
        else
            result = allowedPlayers.get(targetUUID).get(playerUUID);
        return result;
    }

    private PendingTeleportee getPendingTeleport(Player player)
    {
        return pendingTeleports.get(player);
    }

    /**
     * Set a player as allowed, blocked, or unspecified
     * @param playerUUID
     * @param targetUUID
     * @param allow
     */
    public void setAllowed(String playerUUID, String targetUUID, Boolean allow)
    {
        //If player already has an "allowlist," just add to this list.
        if (allowedPlayers.containsKey(playerUUID))
            allowedPlayers.get(playerUUID).put(targetUUID, allow);

        //Otherwise, if never allowed anyone before, create the "allowlist"
        else
        {
            LinkedHashMap<String, Boolean> playerToAddMaybe = new LinkedHashMap<>();
            playerToAddMaybe.put(targetUUID, allow);
            allowedPlayers.put(playerUUID, playerToAddMaybe);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
            return false;

        final Player player = (Player)sender;
        String playerUUID = player.getUniqueId().toString();

        if (cmd.getName().equalsIgnoreCase("tplist"))
        {
            StringBuilder allowed = new StringBuilder(config.getTplistMessage(true) + "  ");
            StringBuilder blocked = new StringBuilder(config.getTplistMessage(false) + "  ");
            //Only build list if player allowed/blocked anyone
            if (allowedPlayers.containsKey(playerUUID))
            {
                LinkedHashMap<String, Boolean> list = allowedPlayers.get(playerUUID);
                for (String targetUUID : list.keySet())
                {
                    OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(UUID.fromString(targetUUID));
                    if (offlinePlayer.getName() == null || offlinePlayer.getName().isEmpty())
                        continue;
                    if (list.get(targetUUID) == null)
                        continue;
                    if (list.get(targetUUID))
                        allowed.append(offlinePlayer.getName() + ", ");
                    else
                        blocked.append(offlinePlayer.getName() + ", ");
                }
            }
            player.sendMessage(allowed.substring(0, allowed.length() - 2));
            player.sendMessage(blocked.substring(0, blocked.length() - 2));
            return true;
        }

        /*Commands requiring 1 argument*/

        if (args.length < 1)
        {
            if (cmd.getName().equalsIgnoreCase("tpa"))
                config.send(player, config.getWhatever("tphelp"));
            return false;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null)
            target = Bukkit.getPlayer(args[0]);

        //Check if target is invalid or invisible player
        if (target == null || !player.canSee(target))
        {
            config.send(player, config.getInvalidPlayerMessage(args[0]));
            return true;
        }

        String targetUUID = target.getUniqueId().toString();

        //Requesting to tp/accept urself? pls
        if (target == player)
        {
            config.send(player, config.getTpToSelf());
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpblock"))
        {
            setAllowed(playerUUID, targetUUID, false);
            config.send(player, config.getBlockingMessage(target.getName()));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpremove"))
        {
            setAllowed(playerUUID, targetUUID, null);
            config.send(player, config.getRemovingMessage(target.getName()));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpa"))
        {
            Boolean allowed = isAllowed(playerUUID, targetUUID);

            //Check if target has not yet allowed/blocked player
            if (allowed == null)
            {
                if (recentRequesters.contains(player))
                {
                    config.send(player, config.getSpam());
                    return true;
                }
                config.send(player, config.getRequestingMessage(true, target.getDisplayName()));
                config.send(target, config.getRequestingMessage(false, player.getName()));
                requesters.put(player, target);
                //command cooldown
                recentRequesters.add(player);
                new BukkitRunnable()
                {
                    public void run()
                    {
                        recentRequesters.remove(player);
                    }
                }.runTaskLater(this, 300L);
                return true;
            }

            //Blocked
            if (!allowed)
            {
                config.send(player, config.getTpNotAllowed(target.getDisplayName()));
                return true;
            }

            //Allowed
            preTeleportPlayer(player, target);
            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("tpallow"))
        {
            setAllowed(playerUUID, targetUUID, true);
            config.send(player, config.getAllowingMessage(target.getName()));
            if (requesters.containsKey(target) && requesters.remove(target) == player)
                config.send(target, config.getRequestAcceptedMessage(player.getName()));
            return true;
        }

        //Not enough arguments
        return false;
    }

    private long getWarmup(Player player, Location targetLocation, @Nullable Player target, long warmupTime)
    {
        PreTPATeleportEvent event = new PreTPATeleportEvent(player, targetLocation, target, warmupTime);
        //Permission check
        if (target != null)
        {
            if (!target.hasPermission("bettertpa.receiveteleports"))
            {
                config.send(player, config.getTpNotAllowed(target.getDisplayName()));
                return -1L;
            }
        }

        getServer().getPluginManager().callEvent(event);

        if (event.isCancelled())
        {
            if (event.getReason() != null && !event.getReason().isEmpty())
                player.sendMessage(ChatColor.RED + event.getReason());
            return -1L;
        }

        return event.getWarmup();
    }

    //Used internally, primarily for warmups, permission checks, etc.
    private void preTeleportPlayer(Player player, Player target)
    {
        boolean canBypassWarmup = (config.hasNoWarmupOverride(player) || config.haveNoWarmup(player, target));
        teleportPlayer(player, target.getDisplayName(), target.getLocation(), !canBypassWarmup, target);
    }

    /**
     * @Deprecated
     * @param player
     * @param targetName
     * @param targetLocation
     * @param warmup
     * @param target target player - set to null if not teleporting to a player
     */
    public void teleportPlayer(Player player, @Nonnull String targetName, @Nonnull final Location targetLocation, boolean warmup, @Nullable Player target)
    {
        if (warmup)
            teleportPlayer(player, targetName, targetLocation, 140L, target); //TODO: use config value
        else
            teleportPlayer(player, targetName, targetLocation, 0L, target);
    }

    public void teleportPlayer(Player player, @Nonnull String targetName, @Nonnull final Location targetLocation, long warmup, @Nullable Player target)
    {
        //Silently cancel any existing teleports
        cancelPendingTeleport(player, false);

        long warmupTime = getWarmup(player, targetLocation, target, warmup);
        if (warmupTime < 0L)
            return;

        //No warmup, no problem
        if (warmupTime == 0L)
        {
            postTeleportPlayer(player, target, targetName, targetLocation);
            return;
        }

        player.sendMessage(ChatColor.GOLD + "0k pls standby while we beem u 2 " + targetName);
        int anIDThing = ThreadLocalRandom.current().nextInt();
        new BukkitRunnable()
        {
            public void run()
            {
                pendingTeleports.put(player, new PendingTeleportee(player, anIDThing));
            }
        }.runTask(this); //1 tick delay in case of right clicking a sign for example

        new BukkitRunnable()
        {
            public void run()
            {
                if (pendingTeleports.containsKey(player) && pendingTeleports.get(player).getId() == anIDThing)
                {
                    postTeleportPlayer(player, target, targetName, targetLocation);
                }
            }
        }.runTaskLater(this, warmupTime);
    }

    private void postTeleportPlayer(Player player, @Nullable Player target, @Nonnull String destinationName, Location targetLocation)
    {
        pendingTeleports.remove(player);
        player.teleport(targetLocation);
        config.send(player, config.getTeleportSuccess(destinationName));
        PostTPATeleportEvent event = new PostTPATeleportEvent(player, target, false);
        getServer().getPluginManager().callEvent(event);
        //TODO: send message???
    }

    /**
     * Cancels an existing teleport task
     * Used for "warmup cancelers" in event handlers or when making a new request during a warmup stage
     * Private for now, as I don't see a use to make public
     * @param player
     */
    private void cancelPendingTeleport(@Nonnull Player player, boolean sendMessage)
    {
        if (pendingTeleports.remove(player) != null)
        {
            getServer().getPluginManager().callEvent(new PostTPATeleportEvent(player, null, true));
            if (sendMessage)
                config.send(player, config.getTeleportReject());
        }
    }

    /*
     * Events to handle canceling pending teleport
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerMove(PlayerMoveEvent event)
    {
        Player player = event.getPlayer();
        PendingTeleportee teleportee = getPendingTeleport(event.getPlayer());
        if (teleportee == null)
            return;
        if (teleportee.getStartLocation().getWorld() != player.getWorld() || teleportee.getStartLocation().distanceSquared(event.getTo()) > 0.3D)
            cancelPendingTeleport(player, true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerTeleport(PlayerTeleportEvent event)
    {
        //Ignore teleports due to Minecraft's wonderful move correction. https://gist.github.com/RoboMWM/dd38528ca995674538a43405c035892f
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.UNKNOWN && event.getFrom().getWorld() == event.getTo().getWorld())
            return;
        cancelPendingTeleport(event.getPlayer(), false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onPlayerTakesDamage(EntityDamageEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        cancelPendingTeleport((Player)event.getEntity(), true);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    void onPlayerInteract(PlayerInteractEvent event)
    {
        cancelPendingTeleport(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onPlayerQuit(PlayerQuitEvent event)
    {
        cancelPendingTeleport(event.getPlayer(), false);
    }
}

class PendingTeleportee
{
    private Location startLocation;
    private int id;

    public PendingTeleportee(Player player, int id)
    {
        this.startLocation = player.getLocation();
        this.id = id;
    }

    Location getStartLocation()
    {
        return this.startLocation;
    }

    int getId()
    {
        return this.id;
    }
}
