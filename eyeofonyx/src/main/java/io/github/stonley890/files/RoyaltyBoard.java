package io.github.stonley890.files;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.stonley890.Main;

public class RoyaltyBoard {

    private static File file;
    private static FileConfiguration boardFile;
    private static Main plugin = Main.getPlugin();

    public static void setup() {

        file = new File(plugin.getDataFolder(), "board.yml");

        if (!file.exists()) {

            try {
                if (!file.createNewFile()) {
                    Bukkit.getLogger().severe("Error creating board.yml file.");
                    
                }
            } catch (IOException e) {
                Bukkit.getLogger().severe("Error creating board.yml file:");
                e.printStackTrace();
            }
        }
        boardFile = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration get() {
        return boardFile;
    }

    public static void save() {
        try {
            boardFile.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving board.yml file:");
            e.printStackTrace();
        }
    }

    public static void reload() {
        boardFile = YamlConfiguration.loadConfiguration(file);
    }

    private RoyaltyBoard() {
        throw new IllegalStateException("Utility class");
    }
}
