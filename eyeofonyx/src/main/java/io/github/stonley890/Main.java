package io.github.stonley890;

import org.bukkit.Bukkit;

public class Main {

    public static Main plugin;

    public void onEnable() {
        // Register variables
        plugin = this;

        String version = getDescription().getVersion();

        // Create config if needed
        getDataFolder().mkdir();
        saveDefaultConfig();

        // Start message
        Bukkit.getLogger().info("Eye of Onyx " + version + ": A plugin that manages the royalty board on Wings of Fire: The New World");
    }
}
