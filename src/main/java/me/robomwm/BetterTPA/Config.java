package me.robomwm.BetterTPA;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.text.MessageFormat;

/**
 * Created on 2/22/2017.
 * A mistake
 * @author RoboMWM
 */
public class Config
{
    private BetterTPA instance;
    private FileConfiguration config;

    public Config(BetterTPA betterTPA)
    {
        instance = betterTPA;
        config = instance.getConfig();

        config.addDefault("blockingMessage", "{0} &2will no longer be able to send teleport requests 2 u." +
                                                        "\nUse &6 /tpremove &2to undo dis if dis wuz mistake.");
        config.addDefault("allowingMessage", "&aU allowed &f{0} 2 teleport 2 you." +
                                                        "\nIf u regret ur decision, u can &6/tpremove {0}");
        config.addDefault("requestingMessage", "&b0k, but 1st, {0} n33ds 2 accept ur teleport proposal.");
        config.addDefault("requestMessage", "{0} &bw0ts 2 tp 2 u." +
                "\nU can: &6/tpallow {0} &bor &6/tpblock {0}");
        config.addDefault("requestAcceptedMessage", "&a{0} has accepted ur teleport pr0posal.\nU may now &6/tpa {0}");
        config.addDefault("removingMessage", "{0} &2was removed from ur /tpallow. FYI, u can view ur /tplist");
        config.addDefault("teleportSuccessMessage", "&aU successfully teleported 2 {0}");
        config.addDefault("teleportRejectMessage", "&4culdnt lock on 2 ur location! U gotta stay still! :(");
        config.addDefault("tpToSelfMessage", "&ckek");
        config.addDefault("tplistAllowedMessage", "Players in /tpallow:");
        config.addDefault("tplistBlockedMessage", "Players in /tpblock:");
        config.addDefault("invalidPlayerMessage", "&cDoesn't look like &f{0} &cis online or a valid name.");
        config.addDefault("tpaSpam", "&cayy m8 slow down with ur teleport pr0posals.");
        config.addDefault("tpNotAllowed", "&c{0} is not able to be teleported to at this time.");

        config.options().copyDefaults(true);
        instance.saveConfig();
    }

    public void send(Player player, String message)
    {
        if (message == null || message.isEmpty())
            return;
        player.sendMessage(message);
    }

    //Language getters

    public String getTeleportReject()
    {
        return formatter(get("teleportRejectMessage"));
    }

    public String getTeleportSuccess(String name)
    {
        return formatter(get("teleportSuccessMessage"), name);
    }


    public String getSpam()
    {
        return formatter(get("tpaSpam"));
    }

    public String getBlockingMessage(String name)
    {
        return formatter(get("blockingMessage"), name);
    }

    public String getRemovingMessage(String name)
    {
        return formatter(get("removingMessage"), name);
    }

    public String getTpNotAllowed(String name)
    {
        return formatter(get("tpNotallowed"), name);
    }

    public String getTpToSelf()
    {
        return formatter(get("tpToSelfMessage"));
    }

    public String getTplistMessage(boolean allowed)
    {
        if (allowed)
            return formatter(get("tplistAllowedMessage"));
        else
            return formatter(get("tplistBlockedMessage"));
    }

    public String getInvalidPlayerMessage(String name)
    {
        return formatter(get("invalidPlayerMessage"), name);
    }

    public String getAllowingMessage(String name)
    {
        return formatter(get("allowingMessage"), name);
    }

    public String getRequestAcceptedMessage(String name)
    {
        return formatter(get("requestAcceptedMessage"), name);
    }

    public String getRequestingMessage(boolean requester, String name)
    {
        if (requester)
            return formatter(get("requestingMessage"), name);
        return formatter(get("requestMessage"), name);
    }

    //Config getters

    public boolean hasNoWarmupOverride(Player player)
    {
        return player.hasPermission("bettertpa.nowarmup.override");
    }

    //Check if both teleporter and teleportee have warmup bypass
    public boolean haveNoWarmup(Player player, Player target)
    {
        return hasNoWarmup(player) && hasNoWarmup(target);
    }

    //Private methods

    private String get(String string)
    {
        return config.getString(string);
    }

    private boolean hasNoWarmup(Player player)
    {
        return player.hasPermission("bettertpa.nowarmup");
    }

    private String formatter(String stringToFormat, Object... formatees)
    {
        if (stringToFormat == null)
            return null;
        return formatter(MessageFormat.format(stringToFormat, formatees));
    }

    private String formatter(String stringToFormat)
    {
        if (stringToFormat == null)
            return null;
        return ChatColor.translateAlternateColorCodes('&', stringToFormat);
    }
}
