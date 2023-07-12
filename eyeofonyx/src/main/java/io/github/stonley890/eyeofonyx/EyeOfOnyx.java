package io.github.stonley890.eyeofonyx;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.logging.Level;

import io.github.stonley890.eyeofonyx.commands.CmdChallenge;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.commands.discord.DiscCommandsManager;
import io.github.stonley890.eyeofonyx.commands.CmdEyeOfOnyx;
import io.github.stonley890.eyeofonyx.commands.CmdRoyalty;
import io.github.stonley890.eyeofonyx.commands.tabcomplete.TabRoyalty;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.listeners.ListenJoin;
import io.github.stonley890.eyeofonyx.listeners.ListenLeave;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

/*
 * The main ticking thread.
*/

public class EyeOfOnyx extends JavaPlugin {

    public final String version = getDescription().getVersion();
    public static final String EOO = ChatColor.GRAY + "[" + ChatColor.GREEN + "EoO" + ChatColor.GRAY + "] " + ChatColor.RESET;

    static EyeOfOnyx plugin;

    @Override
    public void onEnable() {

        // Initialize variables
        plugin = this;

        // Create config if needed
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        // Set up royalty board file
        RoyaltyBoard.setup();

        // Initialize command executors
        Objects.requireNonNull(getCommand("eyeofonyx")).setExecutor(new CmdEyeOfOnyx());
        Objects.requireNonNull(getCommand("royalty")).setExecutor(new CmdRoyalty());
        Objects.requireNonNull(getCommand("challenge")).setExecutor(new CmdChallenge());

        // Initialize tab completer
        Objects.requireNonNull(getCommand("royalty")).setTabCompleter(new TabRoyalty());

        // Initialize listeners
        getServer().getPluginManager().registerEvents(new ListenJoin(), this);
        getServer().getPluginManager().registerEvents(new ListenLeave(), this);

        // Start message
        Bukkit.getLogger().log(Level.INFO, "Eye of Onyx {0}: A plugin that manages the royalty board on Wings of Fire: The New World", version);
        Bot.sendMessage(DiscCommandsManager.gameLogChannel, "*Eye of Onyx " + version + " enabled.*");

        // 20-tick operations
        Runnable tick1200Run = new BukkitRunnable() {
            // Run every minute
            @Override
            public void run() {
                FileConfiguration board = RoyaltyBoard.get();

                // Update board
                RoyaltyBoard.updateBoard();

            }
        };

        Bukkit.getScheduler().runTaskTimer(this, tick1200Run, 20, 1200);
    }

    // Allow other classes to access plugin instance
    public static EyeOfOnyx getPlugin() {
        return plugin;
    }
}
