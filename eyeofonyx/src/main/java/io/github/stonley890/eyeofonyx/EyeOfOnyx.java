package io.github.stonley890.eyeofonyx;

import com.sun.net.httpserver.HttpServer;
import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.commands.*;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.*;
import io.github.stonley890.eyeofonyx.files.*;
import io.github.stonley890.eyeofonyx.listeners.ListenJoin;
import io.github.stonley890.eyeofonyx.listeners.ListenLeave;
import io.github.stonley890.eyeofonyx.web.AvailabilityHandler;
import io.github.stonley890.eyeofonyx.web.SubmitHandler;
import javassist.NotFoundException;
import net.luckperms.api.LuckPerms;
import net.md_5.bungee.api.ChatColor;
import openrp.OpenRP;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
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
            PlayerTribe.setup();
        } catch (IOException e) {
            Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
        }

        Dreamvisitor.debug("Restoring settings.");
        // Restore frozen state
        RoyaltyBoard.setFrozen(getConfig().getBoolean("frozen"));

        Dreamvisitor.debug("Setting up commands.");
        // Initialize command executors
        Objects.requireNonNull(getCommand("eyeofonyx")).setExecutor(new CmdEyeOfOnyx());
        Objects.requireNonNull(getCommand("royalty")).setExecutor(new CmdRoyalty());
        Objects.requireNonNull(getCommand("challenge")).setExecutor(new CmdChallenge());
        Objects.requireNonNull(getCommand("competition")).setExecutor(new CmdCompetition());
        Objects.requireNonNull(getCommand("updateplayer")).setExecutor(new CmdUpdatePlayer());
        Objects.requireNonNull(getCommand("forfeit")).setExecutor(new CmdForfeit());

        // Initialize tab completer
        Objects.requireNonNull(getCommand("royalty")).setTabCompleter(new TabRoyalty());
        Objects.requireNonNull(getCommand("eyeofonyx")).setTabCompleter(new TabEyeOfOnyx());
        Objects.requireNonNull(getCommand("competition")).setTabCompleter(new TabCompetition());
        Objects.requireNonNull(getCommand("updateplayer")).setTabCompleter(new TabUpdatePlayer());
        Objects.requireNonNull(getCommand("challenge")).setTabCompleter(new TabChallenge());

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

                try {
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

                } catch (IOException | InvalidConfigurationException e) {
                    Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
                }

                // Check for unnoticed challenges
                for (int tribe = 0; tribe < RoyaltyBoard.getTribes().length; tribe++) {
                    for (int pos = 0; pos < RoyaltyBoard.getValidPositions().length; pos++) {
                        UUID uuid = RoyaltyBoard.getUuid(tribe, pos);
                        if (uuid != null)
                            try {
                                List<Notification> notifications = Notification.getNotificationsOfPlayer(uuid);

                                for (Notification notification : notifications) {
                                    // If notification is CHALLENGE_REQUESTED and time is beyond challenge-acknowledgement-time
                                    if (notification.type == NotificationType.CHALLENGE_REQUESTED && notification.time.isBefore(LocalDateTime.now().minusDays(getConfig().getInt("challenge-acknowledgement-time")))) {

                                        // Check if it was seen or not
                                        if (!notification.seen) {

                                            // If seen, cancel the challenge.
                                            Challenge.removeChallengesOfPlayers(uuid, RoyaltyBoard.getAttacker(tribe, pos));

                                            // Send expired notification to defender
                                            Notification.removeNotification(notification);
                                            new Notification(uuid, "You missed a challenge notification.", "You did not acknowledge a challenge request within the allowed time, but you will remain on the royalty board because you were unable to receive it.", NotificationType.GENERIC).create();

                                            // Send notification to attacker
                                            UUID attackerUuid = RoyaltyBoard.getAttacker(PlayerTribe.getTribeOfPlayer(uuid), RoyaltyBoard.getPositionIndexOfUUID(uuid));
                                            if (attackerUuid != null) {
                                                String defenderUsername = new Mojang().connect().getPlayerProfile(uuid.toString()).getUsername();
                                                new Notification(attackerUuid, "Your challenge to " + defenderUsername + " was not seen.", "Your challenge request was nullified because the user you challenged was unable to receive the notification.", NotificationType.GENERIC).create();
                                            }

                                            // Set data
                                            RoyaltyBoard.setAttacker(tribe, pos, null);

                                        } else {
                                            // Kick from board if seen and ignored.

                                            // Remove all notifications that are CHALLENGE_REQUESTED
                                            Notification.removeNotificationsOfPlayer(uuid, NotificationType.CHALLENGE_REQUESTED);

                                            new Notification(uuid, "You were removed from the royalty board!", "You did not acknowledge a challenge request within the allowed time.", NotificationType.GENERIC).create();

                                            RoyaltyBoard.removePlayer(tribe, pos, true);
                                            RoyaltyBoard.updateBoard(tribe, false);
                                            RoyaltyBoard.updateDiscordBoard(tribe);

                                            break;
                                        }

                                    }
                                }

                            } catch (IOException | InvalidConfigurationException | NotFoundException e) {
                                throw new RuntimeException(e);
                            }
                    }
                }

                // Check for OC name changes
                if (EyeOfOnyx.openrp != null) {
                    for (Player player : Bukkit.getOnlinePlayers()) {

                        try {
                            int tribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
                            int pos = RoyaltyBoard.getPositionIndexOfUUID(tribe, player.getUniqueId());

                            if (pos != RoyaltyBoard.CIVILIAN) {
                                String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(player.getUniqueId() + ".name");
                                // only update if oc name is not equal to currently stored OC name
                                if (ocName != null && !ocName.equals("No name set") && !ocName.equals(RoyaltyBoard.getOcName(tribe, pos))) {
                                    RoyaltyBoard.updateDiscordBoard(tribe);
                                }
                            }


                        } catch (NotFoundException ignored) {

                        } catch (IOException e) {
                            Bukkit.getLogger().warning("Eye of Onyx was unable to edit the Discord royalty board!");
                            if (Dreamvisitor.debugMode) e.printStackTrace();
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
        RoyaltyBoard.saveToDisk();
        server.stop(1);
    }

    // Allow other classes to access plugin instance
    public static EyeOfOnyx getPlugin() {
        return plugin;
    }

}
