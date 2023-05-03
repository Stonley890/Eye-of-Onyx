package io.github.stonley890.files;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.stonley890.Main;

public class RoyaltyBoard {

    private static File file;
    private static FileConfiguration royaltyBoard;
    private static Main plugin = Main.getPlugin();

    public static void setup() {

        file = new File(plugin.getDataFolder(), "board.yml");

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().severe("Error creating board.yml file:");
                e.printStackTrace();
            }
        }
        royaltyBoard = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get() {
        return royaltyBoard;
    }

    public static void save() {
        try {
            royaltyBoard.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving board.yml file:");
            e.printStackTrace();
        }
    }

    public static void reload() {
        royaltyBoard = YamlConfiguration.loadConfiguration(file);
    }
}
