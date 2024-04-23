package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import javassist.NotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class PlayerTribe {

    private static File file;
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
    }

    private static String getPlayer(@NotNull UUID uuid) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(file);
            return configuration.getString(uuid.toString());
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().severe("Unable to load " + file.getName() + "!");
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new RuntimeException();
        }
    }

    /**
     * Saves the current file configuration to disk.
     */
    private static void savePlayer(@NotNull UUID uuid, @NotNull String teamName) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(file);
            configuration.set(uuid.toString(), teamName);
            configuration.save(file);
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().severe("Unable to load " + file.getName() + "!");
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new RuntimeException();
        }
    }

    /**
     * Gets the stored tribe of a given player. If it does not exist, this method will try to update it.
     *
     * @param playerUuid The player UUID to search for.
     * @return The index of their tribe.
     * @throws NotFoundException The given player does not have a recorded tribe.
     */
    public static int getTribeOfPlayer(@NotNull UUID playerUuid) throws NotFoundException {

        String tribeName = getPlayer(playerUuid);

        Dreamvisitor.debug(tribeName);

        // If not in file, try to update
        if (tribeName == null) {
            try {
                updateTribeOfPlayer(playerUuid);
                tribeName = getPlayer(playerUuid);
            } catch (Exception e) {
                throw new NotFoundException("The given player does not have a recorded tribe.");
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

        if (username == null) throw new NotFoundException("Player is null");

        // Check by team
        Dreamvisitor.debug("Checking by team...");
        String[] teamNames = RoyaltyBoard.getTeamNames();
        for (int i = 0; i < teamNames.length; i++) {
            String teamName = teamNames[i];
            Team team = scoreboard.getTeam(teamName);
            if (team != null && team.hasEntry(username)) {
                Dreamvisitor.debug("Found tribe " + i);
                playerTribe = i;
                savePlayer(uuid, teamNames[playerTribe]);
                return;
            }
        }

        if (online) {
            // If no matching team, check by tags
            Dreamvisitor.debug("Checking by tag...");
            for (int i = 0; i < teamNames.length; i++) {
                String teamName = teamNames[i];
                if (player.getScoreboardTags().contains(teamName)) {
                    Dreamvisitor.debug("Found tag " + i);
                    playerTribe = i;
                    savePlayer(uuid, teamNames[playerTribe]);
                    return;
                }
            }
        }

        throw new NotFoundException("Given player does nozt have a valid team or tag associated with a tribe!");
    }
}
