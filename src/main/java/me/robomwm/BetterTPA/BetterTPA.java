package me.robomwm.BetterTPA;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

/**
 * Created by Robo on 4/24/2016.
 */
public class BetterTPA extends JavaPlugin implements Listener
{
    YamlConfiguration storage;

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
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
    }

    public void onDisable()
    {
        File storageFile = new File(getDataFolder(), "storage.data");
        if (storage != null)
        {
            try
            {
                storage.save(storageFile);
            }
            catch (IOException e) //really
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
            return false;

        if (args.length < 1)
            return false;

        final Player player = (Player)sender;
        String playerUUID = player.getUniqueId().toString();
        final Player target = Bukkit.getPlayerExact(args[0]);
        String targetUUID = target.getUniqueId().toString();

        if (cmd.getName().equalsIgnoreCase("tpdeny"))
        {
            //If player hasn't allowed anyone before, add to hashmap
            if (!allowedPlayers.containsKey(player))
                allowedPlayers.put(player, new HashSet<Player>());
        }

        if (cmd.getName().equalsIgnoreCase("tpa"))
        {
            //Check if target is invalid or invisible player
            if (target == null || !player.canSee(target))
            {
                player.sendMessage(ChatColor.RED + "Doesn't look like " + ChatColor.AQUA + args[0] + ChatColor.RED + " is online or a valid name.");
                return true;
            }

            //Don't allow tracking self
            if (target == player)
            {
                player.sendMessage(ChatColor.RED + "kek");
                return true;
            }

            //Check if target isn't allowing player
            if (!allowedPlayers.containsKey(target) || !allowedPlayers.get(target).contains(player))
            {
                player.sendMessage(ChatColor.AQUA + "0k, but 1st, " + target.getDisplayName() + ChatColor.AQUA + " n33ds 2 accept ur teleport proposal. We'll let u know if they say yes.");
                target.sendMessage(player.getDisplayName() + ChatColor.BLUE + " w0ts 2 tp 2 u. U can:" + ChatColor.GOLD + " /tpallow " + player.getName() + ChatColor.BLUE + "\nor u can" + ChatColor.GOLD + " /tpblock " + player.getName());
                return true;
            }

            trackingPlayers.put(player, target);
            player.sendMessage(ChatColor.GREEN + "Your compass is now tracking " + target.getName() + ".");

            new BukkitRunnable()
            {
                public void run()
                {
                    //Cancel task if player is offline or is no longer tracking target
                    if (!player.isOnline() || !trackingPlayers.containsKey(player) || !trackingPlayers.get(player).equals(target))
                        this.cancel();

                        //Cancel task if target is offline
                    else if (!target.isOnline())
                    {
                        player.sendMessage(ChatColor.RED + target.getName() + " is offline. Resetting compass to spawn.");
                        player.setCompassTarget(player.getWorld().getSpawnLocation());
                        this.cancel();
                    }

                    //Cancel task if target removed player from their allowedPlayers
                    else if (!trackingPlayers.containsKey(target) || !allowedPlayers.get(target).contains(player))
                    {
                        player.sendMessage(ChatColor.RED + target.getName() + " is no longer allowing you to track them. Resetting compass to spawn.");
                        player.setCompassTarget(player.getWorld().getSpawnLocation());
                        this.cancel();
                    }
                    else
                        player.setCompassTarget(target.getLocation());
                }
            }.runTaskTimer(this, 5L, 300L);
            return true;
        }

        else if (cmd.getName().equalsIgnoreCase("tpallow"))
        {
            //Check if target is online and visible
            if (target == null || !player.canSee(target))
            {
                player.sendMessage(ChatColor.RED + "Doesn't look like " + ChatColor.AQUA + args[0] + ChatColor.RED + " is online or a valid name.");
                return true;
            }
            //If player hasn't allowed anyone before, add to hashmap
            if (!allowedPlayers.containsKey(player))
                allowedPlayers.put(player, new HashSet<Player>());
                //otherwise first check if they already allowed the target
            else if (allowedPlayers.get(player).contains(target))
            {
                player.sendMessage(ChatColor.GREEN + "You already allowed " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " to teleport to you.");
                return true;
            }

            allowedPlayers.get(player).add(target);
            player.sendMessage(ChatColor.GREEN + "You allowed " +  ChatColor.AQUA + target.getName() + ChatColor.GREEN + " 2 teleport 2 you.");
            player.sendMessage(ChatColor.DARK_GREEN + "If u regret ur decision, u can always" + ChatColor.GOLD + " /tpdeny " + target.getName());
            return true;
        }
        else if (cmd.getName().equalsIgnoreCase("tpblock"))
        {
            if (allowedPlayers.containsKey(player))
                allowedPlayers.get(player).remove(target);

        }
        //Not enough arguments
        return false;
    }
}
