package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Main;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
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
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class PlayerTribe {

    private static final HashMap<String, String> tribeStorage = new HashMap<>();
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
            Bukkit.getLogger().info("player-tribes.yml does not exist. Creating one...");
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
     * Gets the stored tribe of a given player.
     *
     * @param playerUuid The player UUID to search for.
     * @return The index of their tribe.
     * @throws NotFoundException The given player does not have a recorded tribe. Use {@code updateTribeOfPlayer(Player player)} to get and record it from an online player.
     */
    public static int getTribeOfPlayer(@NotNull UUID playerUuid) throws NotFoundException {

        String tribeName = tribeStorage.get(playerUuid.toString());

        // If not cached, get from file.
        if (tribeName == null) {
            tribeName = fileConfig.getString(playerUuid.toString());

            // If not in file, try to get from online player
            if (tribeName == null) {
                Player player = Bukkit.getPlayer(playerUuid);

                // If player not online, throw exception
                if (player == null) throw new NotFoundException("The given player does not have a recorded tribe.");
                else {
                    // if online, try to get from team or tag
                    try {
                        updateTribeOfPlayer(player.getUniqueId());
                        tribeName = tribeStorage.get(playerUuid.toString());
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

    /**
     * Attempts to update a player's recorded tribe by team.
     * If not found on a team, tags will be checked if player is online.
     * @param uuid the UUID of the player to update, online or offline.
     * @throws NotFoundException given player does not have a valid team or tag associated with a tribe.
     */
    public static void updateTribeOfPlayer(@NotNull UUID uuid) throws NotFoundException {

        boolean online;
        Player player = Bukkit.getPlayer(uuid);

        online = player != null;

        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

        int playerTribe;

        String username;
        if (online) username = player.getName();
        else username = PlayerUtility.getUsernameOfUuid(uuid);

        // Check by team
        Main.debug("Checking by team...");
        String[] teamNames = RoyaltyBoard.getTeamNames();
        for (int i = 0; i < teamNames.length; i++) {
            String teamName = teamNames[i];
            Team team = scoreboard.getTeam(teamName);
            if (team != null && team.hasEntry(username)) {
                Main.debug("Found tribe " + i);
                playerTribe = i;
                saveTribe(uuid, playerTribe);
                return;
            }
        }

        if (online) {
            // If no matching team, check by tags
            Main.debug("Checking by tag...");
            for (int i = 0; i < teamNames.length; i++) {
                String teamName = teamNames[i];
                if (player.getScoreboardTags().contains(teamName)) {
                    Main.debug("Found tag " + i);
                    playerTribe = i;
                    saveTribe(uuid, playerTribe);
                    return;
                }
            }
        }

        throw new NotFoundException("Given player does not have a valid team or tag associated with a tribe!");
    }

    private static void saveTribe(@NotNull UUID player, int tribeIndex) {
        tribeStorage.put(player.toString(), RoyaltyBoard.getTeamNames()[tribeIndex]);
        fileConfig.set(player.toString(), RoyaltyBoard.getTeamNames()[tribeIndex]);
        save();
    }

}
