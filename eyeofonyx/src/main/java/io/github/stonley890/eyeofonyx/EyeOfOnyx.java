package io.github.stonley890.eyeofonyx;

import com.sun.net.httpserver.HttpServer;
import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.commands.*;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabCompetition;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabEyeOfOnyx;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabRoyalty;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabUpdatePlayer;
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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
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

        Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin("Dreamvisitor"));
        Bukkit.getPluginManager().enablePlugin(Bukkit.getPluginManager().getPlugin("LuckPerms"));

        // Create config if needed
        saveDefaultConfig();

        Dreamvisitor.debug("Setting up files.");
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
                            if (challenge.time.size() == 1 && challenge.time.get(0).isBefore(LocalDateTime.now()) && challenge.finalized) {
                                Dreamvisitor.debug("Challenge ready to be called.");
                                Competition.call(challenge);
                            }
                        }
                    }

                } catch (IOException | InvalidConfigurationException e) {
                    Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
                }

                // Check for unnoticed challenges
                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 5; j++) {
                        UUID uuid = RoyaltyBoard.getUuid(i, j);
                        try {
                            List<Notification> notifications = Notification.getNotificationsOfPlayer(uuid);

                            for (Notification notification : notifications) {
                                // If notification is CHALLENGE_REQUESTED and time is beyond challenge-acknowledgement-time
                                if (notification.type == NotificationType.CHALLENGE_REQUESTED && notification.time.isBefore(LocalDateTime.now().minusDays(getConfig().getInt("challenge-acknowledgement-time")))) {

                                    // Check if it was seen or not
                                    if (!notification.seen) {
                                        // If seen, cancel the challenge.

                                        // Send expired notification to defender
                                        Notification.removeNotification(notification);
                                        new Notification(uuid, "You missed a challenge notification.", "You did not acknowledge a challenge request within the allowed time, but you will remain on the royalty board because you were unable to receive it.", NotificationType.GENERIC).create();

                                        // Send notification to attacker
                                        UUID attackerUuid = RoyaltyBoard.getAttacker(PlayerTribe.getTribeOfPlayer(uuid), RoyaltyBoard.getPositionIndexOfUUID(uuid));
                                        if (attackerUuid != null) {
                                            String defenderUsername = new Mojang().connect().getPlayerProfile(uuid.toString()).getUsername();
                                            new Notification(attackerUuid, "Your challenge to " + defenderUsername + " was not seen.", "Your challenge was nullified because the user you challenged was unable to receive the notification.", NotificationType.GENERIC).create();
                                        }

                                    } else {
                                        // Kick from board if seen and ignored.

                                        // Remove all notifications that are CHALLENGE_REQUESTED
                                        List<Notification> notificationList = Notification.getNotificationsOfPlayer(uuid);

                                        for (Notification notification1 : notificationList) {
                                            if (notification1.type.equals(NotificationType.CHALLENGE_REQUESTED))
                                                Notification.removeNotification(notification1);
                                        }
                                        new Notification(uuid, "You were removed from the royalty board!", "You did not acknowledge a challenge request within the allowed time.", NotificationType.GENERIC).create();

                                        RoyaltyBoard.removePlayer(i, j);
                                        RoyaltyBoard.updateBoard();

                                        break;
                                    }

                                }
                            }

                        } catch (IOException | InvalidConfigurationException | NotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }


            }
        };

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, tick1200Run, 20, 1200);

        // Start message
        Bukkit.getLogger().log(Level.INFO, "Eye of Onyx {0}: A plugin that manages the royalty board on Wings of Fire: The New World", version);
        Bot.sendMessage(Bot.gameLogChannel, "*Eye of Onyx " + version + " enabled.*");
    }

    @Override
    public void onDisable() {

        // Finish up
        RoyaltyBoard.updateBoard();
        server.stop(1);
    }

    // Allow other classes to access plugin instance
    public static EyeOfOnyx getPlugin() {
        return plugin;
    }

    public static void createTeams() {
        // Check for and create missing teams
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (String teamName : RoyaltyBoard.getTeamNames()) {
            if (scoreboard.getTeam(teamName) == null) {

                scoreboard.registerNewTeam(teamName);
                Bukkit.getLogger().info(EyeOfOnyx.EOO + "Created missing " + teamName + " team.");
            }
        }
        if (scoreboard.getTeam("eoo.attacker") == null) {
            scoreboard.registerNewTeam("eoo.attacker");
            Bukkit.getLogger().info(EyeOfOnyx.EOO + "Created missing " + "Attacker" + " team.");
        }
        if (scoreboard.getTeam("eoo.defender") == null) {
            scoreboard.registerNewTeam("eoo.defender");
            Bukkit.getLogger().info(EyeOfOnyx.EOO + "Created missing " + "Defender" + " team.");
        }
    }
}
