package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Challenge {

    private static File file;
    private static FileConfiguration fileConfig;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();

    public final String attacker;
    public final String defender;
    public final ChallengeType type;
    public final List<LocalDateTime> time;

    /**
     * Create a new challenge.
     * @param attackerUUID The UUID of the attacking player.
     * @param defenderUUID The UUID of the defending player.
     * @param challengeTimes The possible dates/times of the challenge.
     * @param challengeType The competition to be played.
     */
    public Challenge(String attackerUUID, String defenderUUID, ChallengeType challengeType, List<LocalDateTime> challengeTimes) {
        attacker = attackerUUID;
        defender = defenderUUID;
        type = challengeType;
        time = challengeTimes;
    }

     /**
     * Saves the challenge to challenge.yml on disk
     * @throws IOException If file could not be accessed.
     */
    public void save() throws IOException {

        /* Challenges in challenges.yml are saved as a list of string lists

        challenges:
            -   - 'attacker-UUID-xxxx-xxxxxxxxxxxx'
                - 'defender-UUID-xxxx-xxxxxxxxxxxx'
                - TYPE
                -   - 'uuuu-MM-dd'
                    - 'uuuu-MM-dd'
                    - 'uuuu-MM-dd'

            -   - 'attacker-UUID-xxxx-xxxxxxxxxxxx'
                - 'defender-UUID-xxxx-xxxxxxxxxxxx'
                - TYPE
                -   - 'uuuu-MM-dd'

        ...and so on

         */

        // Get the list of challenges
        List<List<Object>> challenges = (List<List<Object>>) fileConfig.getList("challenges");

        // Init if null or empty
        if (challenges == null || challenges.isEmpty()) {
            challenges = new ArrayList<>();
        }

        // Challenge -> List
        List<Object> yamlChallenge = new ArrayList<>();

        yamlChallenge.add(this.attacker);
        yamlChallenge.add(this.defender);
        yamlChallenge.add(this.type.toString());

        List<String> dateTimes = new ArrayList<>();

        for (LocalDateTime dateTime : this.time) {
            dateTimes.add(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        yamlChallenge.add(dateTimes);

        // Add the given challenge
        challenges.add(yamlChallenge);

        fileConfig.set("challenges", challenges);

        Mojang mojang = new Mojang().connect();

        String defenderUsername = mojang.getPlayerProfile(defender).getUsername();
        String content = defenderUsername + " accepted your challenge! Please select from one of the following times:";

        // Create notification
        new Notification(attacker, "Your challenge was accepted!", content, NotificationType.CHALLENGE_ACCEPTED).create();

        saveFile(fileConfig);
    }

    /**
     * Retrieves all challenges from challenges.yml.
     * @return A list of {@link Challenge}s. If none exist, the list will return empty.
     * @throws IOException If the file could not be accessed.
     * @throws InvalidConfigurationException If the configuration is invalid.
     */
    public static @NotNull List<Challenge> getChallenges() throws IOException, InvalidConfigurationException {

        fileConfig.load(file);

        // Get the list of challenges
        List<List<Object>> yamlChallenges = (List<List<Object>>) fileConfig.getList("challenges");

        List<Challenge> challenges = new ArrayList<>();

        if (yamlChallenges != null && !yamlChallenges.isEmpty()) {
            for (List<Object> yamlChallenge : yamlChallenges) {

                // Get saved values
                String attacker = (String) yamlChallenge.get(0);
                String defender = (String) yamlChallenge.get(1);
                ChallengeType type = ChallengeType.valueOf((String) yamlChallenge.get(2));

                List<String> dateTimes = (List<String>) yamlChallenge.get(3);
                List<LocalDateTime> parsedTimes = new ArrayList<>();

                for (String dateTime : dateTimes) {
                    parsedTimes.add(LocalDateTime.parse(dateTime));
                }

                // Add to a Challenge object
                Challenge challenge = new Challenge(attacker, defender, type, parsedTimes);

                // Add to list
                challenges.add(challenge);
            }
        }

        // Return List<Challenge> that was built
        return challenges;
    }

    /**
     * Initializes the challenge storage.
     * @throws IOException If the file could not be created.
     */
    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "challenges.yml");

        if (!file.exists()) {
            Dreamvisitor.debug("challenges.yml does not exist. Creating one...");
            file.createNewFile();
        }

        fileConfig = YamlConfiguration.loadConfiguration(file);
        saveFile(fileConfig);
    }

    /**
     * Saves the current file configuration to disk.
     * @param board The file configuration to save.
     */
    private static void saveFile(FileConfiguration board) {
        fileConfig = board;
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving challenges.yml file:");
            e.printStackTrace();
        }
    }

}