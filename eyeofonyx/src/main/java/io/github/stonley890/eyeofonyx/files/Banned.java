package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Banned {

    private static File file;
    private static FileConfiguration fileConfig;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();

    /**
     * Initializes the ban list.
     * @throws IOException If the file could not be created.
     */
    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "banned.yml");

        if (!file.exists()) {
            Bukkit.getLogger().info("banned.yml does not exist. Creating one...");
            file.createNewFile();
        }

        fileConfig = YamlConfiguration.loadConfiguration(file);
        save(fileConfig);
    }

    /**
     * Saves the current file configuration to disk.
     * @param board The file configuration to save.
     */
    public static void save(FileConfiguration board) {
        fileConfig = board;
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving banned.yml file.");
        }
    }

    /**
     * Adds a UUID to the ban list.
     * @param uuid The UUID to add.
     */
    public static void addPlayer(String uuid) {
        List<String> banlist = fileConfig.getStringList("banned");
        banlist.add(uuid);
        fileConfig.set("banned", banlist);
    }

    /**
     * Removes a UUID from the ban list.
     * @param uuid The UUID to remove.
     */
    public static void removePlayer(String uuid) {
        List<String> banlist = fileConfig.getStringList("banned");
        banlist.remove(uuid);
        fileConfig.set("banned", banlist);
    }

    /**
     * Checks whether the given UUID is banned or not.
     * @param uuid The player UUID to check.
     * @return Whether the UUID is on the ban list.
     */
    public static boolean isPlayerBanned(String uuid) {
        List<String> banlist = fileConfig.getStringList("banned");
        boolean isBanned;
        isBanned = banlist.contains(uuid);
        return isBanned;
    }

    /**
     * Get the list of banned UUIDs
     * @return A list of String UUIDs
     */
    public static List<String> getBannedPlayers() {
        return fileConfig.getStringList("banned");
    }
}
