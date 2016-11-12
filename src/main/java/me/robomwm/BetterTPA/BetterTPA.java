package me.robomwm.BetterTPA;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

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
    Map<Player, Player> requesters = new HashMap<>();
    Set<Player> recentRequesters = new HashSet<>();
    Set<Player> tpToggled = new HashSet<>();
    Map<Player, Integer> pendingTeleports = new HashMap<>();

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
    public Boolean isAllowed(String playerUUID, String targetUUID, boolean returnNullIfNotSpecified)
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
                player.sendMessage(ChatColor.AQUA + "0k, but 1st, " + target.getDisplayName() + ChatColor.AQUA + " n33ds 2 accept ur teleport proposal.\nWe'll let u know if they say yes.");
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
                }.runTaskLater(this, 200L);
                return true;
            }

            //Blocked
            if (!allowed)
            {
                player.sendMessage(target.getDisplayName() + tpToggled);
                return true;
            }

            //Allowed
            pendingTeleports.remove(player);
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

    public String canTeleport(Player player, Player target)
    {
        PreTPATeleportEvent event = new PreTPATeleportEvent(player);
        getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return "";

        if (target.getGameMode() == GameMode.CREATIVE)
            return target.getName() + " is currently in a non-public (developing) world.";
        return null;
    }

    public void preTeleportPlayer(Player player, Player target)
    {
        String allowed = canTeleport(player, target);
        if (allowed != null)
        {
            player.sendMessage(ChatColor.RED + allowed);
            return;
        }
        if (allowed.isEmpty())
            return;

        boolean applyWarmup = true;
        if (player.hasPermission(teleportWarmupPermission) && target.hasPermission(teleportWarmupPermission))
            applyWarmup = false;
        teleportPlayer(player, target, target.getLocation(), applyWarmup);
    }

    public void teleportPlayer(Player player, Player target, final Location targetLocation, boolean warmup)
    {
        if (!warmup)
        {
            player.teleport(target);
            postTeleportPlayer(player, target);
            return;
        }

        player.sendMessage(ChatColor.GOLD + "0k pls standby while we beem u 2 " + target.getDisplayName());
        int anIDThing = ThreadLocalRandom.current().nextInt();
        pendingTeleports.put(player, anIDThing);

        new BukkitRunnable()
        {
            public void run()
            {
                if (pendingTeleports.containsKey(player) && pendingTeleports.get(player).equals(anIDThing))
                {
                    player.teleport(targetLocation);
                    postTeleportPlayer(player, target);
                }
            }
        }.runTaskLater(this, 140L);
    }

    public void postTeleportPlayer(Player player, Player target)
    {
        player.sendMessage(requestTeleportSuccessMessage + target.getDisplayName());
        PostTPATeleportEvent event = new PostTPATeleportEvent(player, target, false);
    }

    //TODO: things to cancel warmup
}
