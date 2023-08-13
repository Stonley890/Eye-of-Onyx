package io.github.stonley890.eyeofonyx;

import com.sun.net.httpserver.HttpServer;
import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.commands.discord.DiscCommandsManager;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.commands.*;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabCompetition;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabEyeOfOnyx;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabRoyalty;
import io.github.stonley890.eyeofonyx.files.Banned;
import io.github.stonley890.eyeofonyx.files.Challenge;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.listeners.ListenJoin;
import io.github.stonley890.eyeofonyx.listeners.ListenLeave;
import io.github.stonley890.eyeofonyx.web.AvailabilityHandler;
import io.github.stonley890.eyeofonyx.web.SubmitHandler;
import net.md_5.bungee.api.ChatColor;
import openrp.OpenRP;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/*
 * The main ticking thread.
*/

public class EyeOfOnyx extends JavaPlugin {

    public final String version = getDescription().getVersion();
    public static final String EOO = ChatColor.GRAY + "[" + ChatColor.GREEN + "EoO" + ChatColor.GRAY + "] " + ChatColor.RESET;
    public static OpenRP openrp;
    private static EyeOfOnyx plugin;

    private static HttpServer server;

    @Override
    public void onEnable() {

        // Initialize variables
        plugin = this;

        // Create config if needed
        saveDefaultConfig();

        // Set up files
        try {
            RoyaltyBoard.setup();
            Banned.setup();
            Notification.setup();
            Challenge.setup();
        } catch (IOException e) {
            Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // Initialize command executors
        Objects.requireNonNull(getCommand("eyeofonyx")).setExecutor(new CmdEyeOfOnyx());
        Objects.requireNonNull(getCommand("royalty")).setExecutor(new CmdRoyalty());
        Objects.requireNonNull(getCommand("challenge")).setExecutor(new CmdChallenge());
        Objects.requireNonNull(getCommand("tribeupdate")).setExecutor(new CmdTribeUpdate());
        Objects.requireNonNull(getCommand("competition")).setExecutor(new CmdCompetition());

        // Initialize tab completer
        Objects.requireNonNull(getCommand("royalty")).setTabCompleter(new TabRoyalty());
        Objects.requireNonNull(getCommand("eyeofonyx")).setTabCompleter(new TabEyeOfOnyx());
        Objects.requireNonNull(getCommand("competition")).setTabCompleter(new TabCompetition());

        // Initialize listeners
        getServer().getPluginManager().registerEvents(new ListenJoin(), this);
        getServer().getPluginManager().registerEvents(new ListenLeave(), this);

        // Web server
        try {

            server = HttpServer.create(new InetSocketAddress(8000), 0);
            server.createContext("/availability", new AvailabilityHandler());
            server.createContext("/availability-submitted", new SubmitHandler());
            server.setExecutor(null); // creates a default executor
            server.start();

        } catch (IOException e) {
            Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
            Bukkit.getPluginManager().disablePlugin(this);
        }

        // OpenRP API
        openrp = (OpenRP) Bukkit.getPluginManager().getPlugin("OpenRP");

        // 60-second operations
        Runnable tick1200Run = new BukkitRunnable() {
            // Run every minute
            @Override
            public void run() {

                // Update board
                RoyaltyBoard.updateBoard();

                try {
                    List<Challenge> challenges = Challenge.getChallenges();

                    if (!challenges.isEmpty()) {
                        for (Challenge challenge : challenges) {
                            if (challenge.time.size() == 1 && challenge.time.get(0).isBefore(LocalDateTime.now())) {

                                Competition.call(challenge);

                            }
                        }
                    }

                } catch (IOException | InvalidConfigurationException e) {
                    Bukkit.getLogger().warning("An I/O exception of some sort has occurred. Eye of Onyx could not initialize files. Does the server have write access?");
                }

            }
        };

        Bukkit.getScheduler().runTaskTimer(this, tick1200Run, 20, 1200);

        // Start message
        Bukkit.getLogger().log(Level.INFO, "Eye of Onyx {0}: A plugin that manages the royalty board on Wings of Fire: The New World", version);
        Bot.sendMessage(DiscCommandsManager.gameLogChannel, "*Eye of Onyx " + version + " enabled.*");
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
}
