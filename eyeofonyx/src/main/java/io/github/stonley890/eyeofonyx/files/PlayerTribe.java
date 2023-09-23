package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import javassist.NotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.HashMap;

public class PlayerTribe {

    private static HashMap<String, String> tribeStorage = new HashMap<>();
    /*
    These are stored as <String player-uuid, String TeamName> but returned as tribe indexes. This way, the file is human-readable, and the system can be adapted if new tribes are introduced.
     */

    private static File file;
    private static FileConfiguration fileConfig;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();

    /**
     * Initializes the player tribe storage.
     *
     * @throws IOException If the file could not be created.
     */
    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "player-tribes.yml");

        if (!file.exists()) {
            Dreamvisitor.debug("player-tribes.yml does not exist. Creating one...");
            file.createNewFile();
        }

        fileConfig = YamlConfiguration.loadConfiguration(file);
        save();
    }

    /**
     * Saves the current file configuration to disk.
     */
    private static void save() {
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving player-tribes.yml file:");
        }
    }

    /**
     * Reloads data from file.
     *
     * @throws IOException
     * @throws InvalidConfigurationException
     */
    public static void reload() throws IOException, InvalidConfigurationException {
        fileConfig.load(file);
    }

    /**
     * Gets the stored tribe of a given player.
     *
     * @param playerUuid The player UUID to search for.
     * @return The index of their tribe.
     * @throws NotFoundException The given player does not have a recorded tribe. Use {@code updateTribeOfPlayer(Player player)} to get and record it from an online player.
     */
    public static int getTribeOfPlayer(String playerUuid) throws NotFoundException {

        String tribeName = tribeStorage.get(playerUuid);

        // If not cached, get from file.
        if (tribeName == null) {
            tribeName = fileConfig.getString(playerUuid);

            // If not in file, try to get from online player
            if (tribeName == null) {
                Player player = Bukkit.getPlayer(playerUuid);

                // If player not online, throw exception
                if (player == null) throw new NotFoundException("The given player does not have a recorded tribe.");
                else {
                    // if online, try to get from team or tag
                    try {
                        updateTribeOfPlayer(player);
                        tribeName = tribeStorage.get(playerUuid);
                    } catch (Exception e) {
                        throw new NotFoundException("The given player does not have a recorded tribe.");
                    }
                }
            }
        }

        // Covert to index
        String[] teamNames = RoyaltyBoard.getTeamNames();
        for (int i = 0; i < teamNames.length; i++) {
            String teamName = teamNames[i];
            if (teamName.equals(tribeName)) return i;
        }

        throw new NotFoundException("The given player does not have a recorded tribe.");

    }

    public static void updateTribeOfPlayer(@NotNull Player player) throws InvalidObjectException, NotFoundException {

        if (!player.isOnline()) {
            throw new InvalidObjectException("The given player is not online.");
        }

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        int playerTribe = -1;

        // Check by team
        String[] teamNames = RoyaltyBoard.getTeamNames();
        for (int i = 0; i < teamNames.length; i++) {
            String teamName = teamNames[i];
            Team team = scoreboard.getTeam(teamName);
            if (team != null && team.hasEntry(player.getName())) {
                playerTribe = i;
                saveTribe(player, playerTribe);
                return;
            }
        }

        // If no matching team, check by tags
        for (int i = 0; i < teamNames.length; i++) {
            String teamName = teamNames[i];
            if (player.getScoreboardTags().contains(teamName)) {
                playerTribe = i;
                saveTribe(player, playerTribe);
                return;
            }
        }

        throw new NotFoundException("Given player does not have a valid team or tag associated with a tribe!");
    }

    private static void saveTribe(Player player, int tribeIndex) {
        tribeStorage.put(player.getUniqueId().toString(), RoyaltyBoard.getTeamNames()[tribeIndex]);
        fileConfig.set(player.getUniqueId().toString(), RoyaltyBoard.getTeamNames()[tribeIndex]);
        save();
    }

}
