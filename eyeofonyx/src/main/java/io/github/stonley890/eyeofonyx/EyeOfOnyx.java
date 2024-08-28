package io.github.stonley890.eyeofonyx;

import com.sun.net.httpserver.HttpServer;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.ExecutableCommand;
import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.dreamvisitor.data.TribeUtil;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.commands.*;
import io.github.stonley890.eyeofonyx.discord.Discord;
import io.github.stonley890.eyeofonyx.files.*;
import io.github.stonley890.eyeofonyx.listeners.ListenJoin;
import io.github.stonley890.eyeofonyx.listeners.ListenLeave;
import io.github.stonley890.eyeofonyx.web.AvailabilityHandler;
import io.github.stonley890.eyeofonyx.web.SubmitHandler;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ChatColor;
import openrp.OpenRP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/*
 * The main ticking thread.
 */

public class EyeOfOnyx extends JavaPlugin {

    public final String version = getDescription().getVersion();
    public static final String EOO = ChatColor.GRAY + "[" + ChatColor.GREEN + "EoO" + ChatColor.GRAY + "] " + ChatColor.RESET;
    public static OpenRP openrp;
    public static LuckPerms luckperms;
    private static EyeOfOnyx plugin;

    private static HttpServer server;

    @Override
    public void onEnable() {

        // Initialize variables
        plugin = this;

        Bukkit.getPluginManager().enablePlugin(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("Dreamvisitor")));
        Bukkit.getPluginManager().enablePlugin(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("LuckPerms")));

        // Create config if needed
        saveDefaultConfig();

        try {
            Bot.getJda().awaitReady();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Set up files
        try {
            RoyaltyBoard.setup();
            Banned.setup();
            Notification.setup();
            Challenge.setup();
            MessageFormat.setup();
        } catch (IOException e) {
            Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
        }

        Dreamvisitor.debug("Restoring settings.");
        // Restore frozen state
        RoyaltyBoard.setFrozen(getConfig().getBoolean("frozen"));

        Dreamvisitor.debug("Setting up commands.");

        // Initialize command executors
        List<ExecutableCommand<?, ?>> commands = new ArrayList<>();
        commands.add(new CmdEyeOfOnyx().getCommand());
        commands.add(new CmdChallenge().getCommand());
        commands.add(new CmdCompetition().getCommand());
        commands.add(new CmdUpdatePlayer().getCommand());
        commands.add(new CmdResign().getCommand());

        for (ExecutableCommand<?, ?> command : commands) {
            if (command instanceof CommandAPICommand apiCommand) {
                apiCommand.register();
            } else if (command instanceof CommandTree apiCommand) {
                apiCommand.register();
            }
        }

        // Initialize listeners
        getServer().getPluginManager().registerEvents(new ListenJoin(), this);
        getServer().getPluginManager().registerEvents(new ListenLeave(), this);

        Dreamvisitor.debug("Creating Discord commands.");
        // Add commands
        Discord.initCommands();

        Dreamvisitor.debug("Starting web server.");
        // Web server
        try {
            server = HttpServer.create(new InetSocketAddress(getConfig().getInt("port")), 0);
            server.createContext("/availability", new AvailabilityHandler());
            server.createContext("/availability-submitted", new SubmitHandler());
            server.setExecutor(null); // creates a default executor
            server.start();

        } catch (IOException e) {
            Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
            e.printStackTrace();
        }

        // OpenRP API
        openrp = (OpenRP) Bukkit.getPluginManager().getPlugin("OpenRP");

        // LuckPerms API
        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) luckperms = provider.getProvider();

        Runnable tick1200Run = new BukkitRunnable() {

            @Override
            public void run() {

                List<Challenge> challenges = Challenge.getChallenges();

                Dreamvisitor.debug("Checking for challenges.");

                if (!challenges.isEmpty()) {
                    Dreamvisitor.debug("There are/is " + challenges.size() + " challenge(s) pending.");
                    for (Challenge challenge : challenges) {
                        if (challenge.time.size() == 1 && challenge.time.get(0).isBefore(LocalDateTime.now()) && challenge.state == Challenge.State.SCHEDULED) {
                            Dreamvisitor.debug("Challenge ready to be called.");
                            Competition.call(challenge);
                        }
                    }
                }

                // Check for unnoticed challenges
                for (int t = 0; t < TribeUtil.tribes.length; t++) {
                    Tribe tribe = TribeUtil.tribes[t];
                    for (int pos = 0; pos < RoyaltyBoard.getValidPositions().length; pos++) {
                        UUID uuid = RoyaltyBoard.getUuid(tribe, pos);
                        if (uuid != null) {

                            LocalDateTime lastOnline = RoyaltyBoard.getLastOnline(tribe, pos);
                            if (lastOnline == null || lastOnline.isBefore(LocalDateTime.now().minusDays(getConfig().getInt("inactivity-timer")))) {
                                RoyaltyBoard.updateBoard(tribe, true, true);
                            }

                            List<Notification> notifications = Notification.getNotificationsOfPlayer(uuid);

                            for (Notification notification : notifications) {
                                // If notification is CHALLENGE_REQUESTED and time is beyond challenge-acknowledgement-time
                                if (notification.type == Notification.Type.CHALLENGE_REQUESTED && notification.time.isBefore(LocalDateTime.now().minusDays(getConfig().getInt("challenge-acknowledgement-time")))) {

                                    // Check if it was seen or not
                                    Challenge challenge = Challenge.getChallenge(uuid);
                                    if (!notification.seen) {

                                        // If not seen, cancel the challenge.

                                        // Send expired notification to defender
                                        Notification.removeNotification(notification);
                                        new Notification(uuid, "You missed a challenge notification.", "You did not acknowledge a challenge request within the allowed time, but you will remain on the royalty board because you were unable to receive it.", Notification.Type.GENERIC).create();

                                        if (challenge != null) {
                                            // Send notification to attacker
                                            UUID attackerUuid = challenge.attacker;
                                            String defenderUsername = new Mojang().connect().getPlayerProfile(uuid.toString()).getUsername();
                                            new Notification(attackerUuid, "Your challenge to " + defenderUsername + " was not seen.", "Your challenge request was nullified because the user you challenged was unable to receive the notification.", Notification.Type.GENERIC).create();

                                            // Set data
                                            Challenge.remove(challenge);
                                        }

                                    } else {
                                        // Kick from board if seen and ignored.
                                        if (challenge != null) {
                                            // Send notification to attacker
                                            UUID attackerUuid = challenge.attacker;
                                            String defenderUsername = new Mojang().connect().getPlayerProfile(uuid.toString()).getUsername();
                                            new Notification(attackerUuid, "Your challenge to " + defenderUsername + " was ignored.", "Your challenge request was nullified because the user you challenged did not respond to the notification.", Notification.Type.GENERIC).create();

                                            // Set data
                                            Challenge.remove(challenge);
                                        }

                                        String username = PlayerUtility.getUsernameOfUuid(uuid);
                                        RoyaltyBoard.report(username, username + " was kicked from " + tribe.getTeamName() + " " + RoyaltyBoard.getValidPositions()[pos].toUpperCase() + " because they saw but did not respond to their challenge request.");

                                        // Remove all notifications that are CHALLENGE_REQUESTED
                                        Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_REQUESTED);

                                        new Notification(uuid, "You were removed from the royalty board!", "You did not acknowledge a challenge request within the allowed time.", Notification.Type.GENERIC).create();

                                        RoyaltyBoard.removePlayer(tribe, pos, true);
                                        RoyaltyBoard.updateBoard(tribe, false, true);
                                        RoyaltyBoard.updateDiscordBoard(tribe);

                                        break;
                                    }

                                }
                            }

                        }
                    }
                }

                // Check for OC name changes
                if (EyeOfOnyx.openrp != null) {
                    for (Player player : Bukkit.getOnlinePlayers()) {

                        Tribe tribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
                        if (tribe != null) {
                            int pos = RoyaltyBoard.getPositionIndexOfUUID(tribe, player.getUniqueId());

                            if (pos != RoyaltyBoard.CIVILIAN) {
                                String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(player.getUniqueId() + ".name");
                                // only update if oc name is not equal to currently stored OC name
                                if (ocName != null && !ocName.equals("No name set") && !ocName.equals(RoyaltyBoard.getOcName(tribe, pos))) {
                                    RoyaltyBoard.setOcName(tribe, pos, ocName);
                                    RoyaltyBoard.updateDiscordBoard(tribe);
                                }
                            }
                        }

                    }
                }
            }
        };

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, tick1200Run, 20, 1200);

        // Start message
        Bukkit.getLogger().log(Level.INFO, "Eye of Onyx {0}: A plugin that manages the royalty board on Wings of Fire: The New World", version);
        Bot.sendLog("*Eye of Onyx " + version + " enabled.*");
    }

    @Override
    public void onDisable() {
        // Finish up
        server.stop(1);
    }

    // Allow other classes to access plugin instance
    public static EyeOfOnyx getPlugin() {
        return plugin;
    }

}
