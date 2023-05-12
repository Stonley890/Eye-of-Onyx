package io.github.stonley890;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.stonley890.commands.CmdEyeOfOnyx;
import io.github.stonley890.files.RoyaltyBoard;
import io.github.stonley890.listeners.ListenJoin;

/*
 * The main ticking thread.
*/

public class Main extends JavaPlugin {

    public final String version = getDescription().getVersion();

    static Main plugin;

    @Override
    public void onEnable() {

        // Initialize variables
        plugin = this;

        // Create config if needed
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        // Set up royalty board file
        RoyaltyBoard.setup();
        RoyaltyBoard.get().options().copyDefaults();
        RoyaltyBoard.save();

        // Initialize command executors
        getCommand("eyeofonyx").setExecutor(new CmdEyeOfOnyx());

        // Initialize listeners
        getServer().getPluginManager().registerEvents(new ListenJoin(), this);

        // Start message
        Bukkit.getLogger().log(Level.INFO, "Eye of Onyx {0}: A plugin that manages the royalty board on Wings of Fire: The New World", version);
    }

    // Allow other classes to access plugin instance
    public static Main getPlugin() {
        return plugin;
    }

    // Write config shortcut
    public boolean writeConfig(String path, Object value) {
        try {
            getConfig().set(path, value);
            saveConfig();
            return true;
        } catch (Exception e) {
            getLogger().severe("Error writing to config.yml:\n" + e);
            return false;
        }

    }
}
