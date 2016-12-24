package me.robomwm.BetterTPA;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Robo on 4/24/2016.
 */
public class BetterTPA extends JavaPlugin implements Listener
{
    YamlConfiguration storage;
    ConfigurationSection allowedPlayersSection;
    String blockedMessage = ChatColor.DARK_GREEN + " will no longer be able to send teleport requests 2 u. Use" +
            ChatColor.GOLD + " /tpremove" + ChatColor.DARK_GREEN + " to undo dis if dis wuz mistake.";
    String removeMessage = ChatColor.DARK_GREEN + " was removed from ur /tpallow. FYI, u can view ur /tplist";
    String tpToggledMessage = ChatColor.RED + " is currently n0t in da m00d 2 receive teleport pr0posals. Try l8r, mebee?";
    String requestTeleportSuccessMessage = ChatColor.GREEN + "U successfully teleported 2 ";
    String targetTeleportSuccessMessage = ChatColor.AQUA + " teleported 2 u";
    String teleportWarmupPermission = "bettertpa.nowarmup";
    String rejectMessage = ChatColor.DARK_RED + "culdnt lock on 2 ur location! U gotta stay still! :(";
    Map<Player, Player> requesters = new HashMap<>();
    Set<Player> recentRequesters = new HashSet<>();
    Set<Player> tpToggled = new HashSet<>();
    Map<Player, PendingTeleportee> pendingTeleports = new HashMap<>();

    Map<String, LinkedHashMap<String, Boolean>> allowedPlayers = new LinkedHashMap<>();

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
        saveConfig();
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
                this.getLogger().severe("Could not create storage.yml! Since I'm lazy, there currently is no \"in memory\" option. Will now disable along with a nice stack trace for you to bother me with:");
                e.printStackTrace();
                getServer().getPluginManager().disablePlugin(this);
            }
        }
        else
            storage = YamlConfiguration.loadConfiguration(storageFile);

        //Create appropriate configurationsections, if they don't exist
        if (storage.getConfigurationSection("allowedPlayers") == null)
            storage.set("allowedPlayers", new LinkedHashMap<String, String>());

        //Set variables/shortcuts
        allowedPlayersSection = storage.getConfigurationSection("allowedPlayers");
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
     * @param returnNullIfNotSpecified if true, will return null if target is not allowed nor blocked. Otherwise, will return false for this case
     */
    private Boolean isAllowed(String playerUUID, String targetUUID, boolean returnNullIfNotSpecified)
    {
        Boolean result;
        //Check if target has allowed anyone
        if (!allowedPlayers.containsKey(targetUUID))
            result = null;
        else
            result = allowedPlayers.get(targetUUID).get(playerUUID);

        //Return false instead of null if we should not return null
        if (result == null && !returnNullIfNotSpecified)
            return false;

        return result;
    }

    /**
     * Cancels an existing teleport task
     * Used for "warmup cancelers" in event handlers or when making a new request during a warmup stage
     * Private for now, as I don't see a use to make public
     * @param player
     */
    private void cancelPendingTeleport(Player player, boolean sendMessage)
    {
        if (pendingTeleports.remove(player) != null)
        {
            PostTPATeleportEvent event = new PostTPATeleportEvent(player, null, false);
            getServer().getPluginManager().callEvent(event);
            if (sendMessage)
                player.sendMessage(rejectMessage);
        }

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
            //TODO: implement tplist
            return false;
        }

        if (args.length < 1)
            return false;

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null)
            target = Bukkit.getPlayer(args[0]);

        //Check if target is invalid or invisible player
        if (target == null || !player.canSee(target))
        {
            player.sendMessage(ChatColor.RED + "Doesn't look like " + ChatColor.AQUA + args[0] + ChatColor.RED + " is online or a valid name.");
            return true;
        }

        String targetUUID = target.getUniqueId().toString();

        //Requesting to tp/accept urself? pls
        if (target == player)
        {
            player.sendMessage(ChatColor.RED + "kek");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpblock"))
        {
            setAllowed(playerUUID, targetUUID, false);
            player.sendMessage(target.getDisplayName() + blockedMessage);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpremove"))
        {
            setAllowed(playerUUID, targetUUID, null);
            player.sendMessage(target.getDisplayName() + removeMessage);
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("tpa"))
        {
            Boolean allowed = isAllowed(playerUUID, targetUUID, true);

            //Check if target has not yet allowed/blocked player
            if (allowed == null)
            {
                if (tpToggled.contains(target))
                {
                    player.sendMessage(target.getDisplayName() + tpToggled);
                    return true;
                }
                if (recentRequesters.contains(player))
                {
                    player.sendMessage(ChatColor.RED + "ayy m8 slow down with ur teleport pr0posals.");
                    return true;
                }
                player.sendMessage(ChatColor.AQUA + "0k, but 1st, " + target.getDisplayName() + ChatColor.AQUA + " n33ds 2 accept ur teleport proposal.");
                target.sendMessage(player.getDisplayName() + ChatColor.AQUA + " w0ts 2 tp 2 u." +
                        "\nU can: " + ChatColor.GOLD + "/tpallow " + player.getName() + ChatColor.AQUA + " or " + ChatColor.GOLD + "/tpblock " + player.getName());
                requesters.put(player, target);
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
                player.sendMessage(target.getDisplayName() + tpToggled);
                return true;
            }

            //Allowed
            cancelPendingTeleport(player, false);
            preTeleportPlayer(player, target);
            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("tpallow"))
        {
            setAllowed(playerUUID, targetUUID, true);
            player.sendMessage(ChatColor.GREEN + "U allowed " +  ChatColor.AQUA + target.getName() + ChatColor.GREEN + " 2 teleport 2 you.");
            player.sendMessage(ChatColor.GREEN + "If u regret ur decision, u can " + ChatColor.GOLD + "/tpremove " + target.getName());
            if (requesters.containsKey(target) && requesters.remove(target) == player)
                target.sendMessage(player.getDisplayName() + ChatColor.GREEN + " has accepted ur teleport pr0posal.\nU may now /tp " + player.getName());
            return true;
        }
        //Not enough arguments
        return false;
    }

    private boolean canTeleport(Player player, Location targetLocation, Player target)
    {
        PreTPATeleportEvent event = new PreTPATeleportEvent(player, targetLocation);

        //target checks
        if (target != null)
        {
            if (target.isDead() || target.hasMetadata("DEAD"))
            {
                event.setReason(target.getName() + " iz ded rite now :( Try again in a few seconds?");
                event.setCancelled(true);
            }
            else if (target.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.SPECTATOR)
            {
                event.setReason(target.getName() + " is not able to be teleported to at this time.");
                event.setCancelled(true);
            }
        }

        getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
        {
            if (event.getReason() != null && !event.getReason().isEmpty())
                player.sendMessage(ChatColor.RED + event.getReason());
            return false;
        }

        return true;
    }

    private void preTeleportPlayer(Player player, Player target)
    {
        boolean applyWarmup = true;
        if (player.hasPermission(teleportWarmupPermission) && target.hasPermission(teleportWarmupPermission))
            applyWarmup = false;
        teleportPlayer(player, target.getDisplayName(), target.getLocation(), applyWarmup, target);
    }

    /**
     *
     * @param player
     * @param targetName
     * @param targetLocation
     * @param warmup
     * @param target target player - set to null if not teleporting to a player
     */
    public void teleportPlayer(Player player, String targetName, @Nonnull final Location targetLocation, boolean warmup, @Nullable Player target)
    {
        if (!canTeleport(player, targetLocation, target))
            return;

        if (!warmup)
        {
            player.teleport(targetLocation);
            postTeleportPlayer(player, target, targetName);
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
                    cancelPendingTeleport(player, false);
                    player.teleport(targetLocation);
                    postTeleportPlayer(player, target, targetName);
                }
            }
        }.runTaskLater(this, 140L);
    }

    private void postTeleportPlayer(Player player, @Nullable Player target, String destinationName)
    {
        player.sendMessage(requestTeleportSuccessMessage + destinationName);
        PostTPATeleportEvent event = new PostTPATeleportEvent(player, target, false);
        getServer().getPluginManager().callEvent(event);
    }

    /**
     * Events to handle canceling pending teleport
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        Player player = event.getPlayer();
        PendingTeleportee teleportee = getPendingTeleport(event.getPlayer());
        if (teleportee == null)
            return;
        if (teleportee.getStartLocation().getWorld() != player.getWorld() || teleportee.getStartLocation().distanceSquared(event.getTo()) > 0.3D)
            cancelPendingTeleport(player, true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event)
    {
        cancelPendingTeleport(event.getPlayer(), false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTakesDamage(EntityDamageEvent event)
    {
        if (event.getEntityType() != EntityType.PLAYER)
            return;
        cancelPendingTeleport((Player)event.getEntity(), true);
    }
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        cancelPendingTeleport(event.getPlayer(), true);
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
