package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Banned {

    private static File file;
    private static FileConfiguration fileConfig;
    private static final Dreamvisitor plugin = Dreamvisitor.getPlugin();

    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "banned.yml");

        if (!file.exists()) {
            if (file.getParentFile().mkdirs()) {
                file.createNewFile();
            }
        }
        fileConfig = YamlConfiguration.loadConfiguration(file);
        save(fileConfig);
    }

    public static void save(FileConfiguration board) {
        fileConfig = board;
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving banned.yml file:");
            e.printStackTrace();
        }
    }

    public static void addPlayer(String uuid) {
        List<String> banlist = fileConfig.getStringList("banned");
        if (banlist == null) {
            banlist = new ArrayList<>();
        }
        banlist.add(uuid);
        fileConfig.set("banned", banlist);
    }

    public static void removePlayer(String uuid) {
        List<String> banlist = fileConfig.getStringList("banned");
        if (banlist != null) {
            banlist.remove(uuid);
            fileConfig.set("banned", banlist);
        }
    }

    public static boolean isPlayerBanned(String uuid) {
        List<String> banlist = fileConfig.getStringList("banned");
        boolean isBanned = false;
        if (banlist != null) {
            isBanned = banlist.contains(uuid);
        }
        return isBanned;
    }
}
