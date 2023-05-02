package io.github.stonley890;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.stonley890.commands.CmdEyeOfOnyx;

/*
 * The main ticking thread.
*/

public class Main extends JavaPlugin {

    public static Main plugin;

    public static String version;

    public void onEnable() {
        // Register variables
        plugin = this;

        version = getDescription().getVersion();
        
        // Create config if needed
        getDataFolder().mkdir();
        saveDefaultConfig();

        // Initialize command executors
        getCommand("eyeofonyx").setExecutor(new CmdEyeOfOnyx());

        // Start message
        Bukkit.getLogger().info("Eye of Onyx " + version + ": A plugin that manages the royalty board on Wings of Fire: The New World");
    }

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
