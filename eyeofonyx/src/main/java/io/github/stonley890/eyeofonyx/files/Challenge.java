package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Main;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public final UUID attacker;
    public final UUID defender;
    public final ChallengeType type;
    public final List<LocalDateTime> time;
    public boolean finalized = false;

    /**
     * Create a new challenge.
     * @param attackerUUID The UUID of the attacking player.
     * @param defenderUUID The UUID of the defending player.
     * @param challengeTimes The possible dates/times of the challenge.
     * @param challengeType The competition to be played.
     */
    public Challenge(UUID attackerUUID, UUID defenderUUID, ChallengeType challengeType, List<LocalDateTime> challengeTimes) {
        attacker = attackerUUID;
        defender = defenderUUID;
        type = challengeType;
        time = challengeTimes;
    }

    public static void remove(Challenge challenge) throws IOException, InvalidConfigurationException {
        List<Challenge> challenges = getChallenges();

        challenges.removeIf(challenge1 -> challenge1.defender.equals(challenge.defender));

        List<List<Object>> yamlChallenges = new ArrayList<>();

        for (Challenge challenge1 : challenges) {
            // Challenge -> List
            List<Object> yamlChallenge = new ArrayList<>();

            Main.debug("Converting to YAML...");
            yamlChallenge.add(challenge1.attacker);
            yamlChallenge.add(challenge1.defender);
            yamlChallenge.add(challenge1.type.toString());

            List<String> dateTimes = new ArrayList<>();

            for (LocalDateTime dateTime : challenge1.time) {
                dateTimes.add(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            }
            yamlChallenge.add(dateTimes);
            yamlChallenge.add(challenge1.finalized);

            // Add the given challenge
            yamlChallenges.add(yamlChallenge);
        }

        Main.debug("Added to List");

        fileConfig.set("challenges", yamlChallenges);

        Main.debug("Saving to file");
        saveFile(fileConfig);
    }

    /**
     * Deletes all challenges that contain a given player.
     * @param uuid the players whose challenges to delete.
     * @param reason the reason for the challenge being removed.
     *               This will be sent in a notification to the other party.
     *               If this is {@code null}, no notification will be sent.
     */
    public static void removeChallengesOfPlayer(UUID uuid, @Nullable String reason) {
        try {
            for (Challenge challenge : getChallenges()) {
                if (challenge.defender == uuid || challenge.attacker == uuid) {
                    if (reason != null) {
                        if (challenge.defender == uuid) new Notification(challenge.attacker, "Your challenge was canceled.", reason, NotificationType.GENERIC).create();
                        if (challenge.attacker == uuid) new Notification(challenge.defender, "Your challenge was canceled.", reason, NotificationType.GENERIC).create();
                    }
                    Challenge.remove(challenge);
                }
            }
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().severe("Unable to access challenges.yml file!");
        }
        saveFile(fileConfig);
    }

    /**
     * Deletes all challenges that contain the given players.
     * @param attacker the first player whose challenges to delete.
     * @param defender the second player whose challenges to delete.
     */
    public static void removeChallengesOfPlayers(UUID attacker, UUID defender) {
        try {
            for (Challenge challenge : getChallenges()) {
                if (challenge.attacker == attacker && challenge.defender == defender) {
                    Challenge.remove(challenge);
                }
            }
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().severe("Unable to access challenges.yml file!");
        }
    }

    /**
     * Saves the challenge to challenge.yml on disk
      */
    public void save() {

        Main.debug("Creating new challenge...");

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
        Main.debug("Got list of challenges.");

        // Init if null or empty
        if (challenges == null || challenges.isEmpty()) {
            Main.debug("No challenges; creating a new list");
            challenges = new ArrayList<>();
        }

        // Challenge -> List
        List<Object> yamlChallenge = new ArrayList<>();

        Main.debug("Converting to YAML...");
        yamlChallenge.add(this.attacker);
        yamlChallenge.add(this.defender);
        yamlChallenge.add(this.type.toString());

        List<String> dateTimes = new ArrayList<>();

        for (LocalDateTime dateTime : this.time) {
            dateTimes.add(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        yamlChallenge.add(dateTimes);
        yamlChallenge.add(finalized);

        // Add the given challenge
        challenges.add(yamlChallenge);
        Main.debug("Added to List");

        fileConfig.set("challenges", challenges);

        Main.debug("Creating notification...");
        Mojang mojang = new Mojang().connect();

        String defenderUsername = mojang.getPlayerProfile(defender.toString()).getUsername();
        String content = defenderUsername + " accepted your challenge! Please select from one of the following times:";

        Main.debug("Saving to file");
        saveFile(fileConfig);

        // Create notification
        new Notification(attacker, "Your challenge was accepted!", content, NotificationType.CHALLENGE_ACCEPTED).create();
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
                UUID attacker = UUID.fromString((String) yamlChallenge.get(0));
                UUID defender = UUID.fromString((String) yamlChallenge.get(1));
                ChallengeType type = ChallengeType.valueOf((String) yamlChallenge.get(2));

                List<String> dateTimes = (List<String>) yamlChallenge.get(3);
                List<LocalDateTime> parsedTimes = new ArrayList<>();

                for (String dateTime : dateTimes) {
                    parsedTimes.add(LocalDateTime.parse(dateTime));
                }

                boolean finalized = (boolean) yamlChallenge.get(4);

                // Add to a Challenge object
                Challenge challenge = new Challenge(attacker, defender, type, parsedTimes);
                challenge.finalized = finalized;

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
            Bukkit.getLogger().info("challenges.yml does not exist. Creating one...");
            file.createNewFile();
        }

        fileConfig = YamlConfiguration.loadConfiguration(file);
        saveFile(fileConfig);
    }

    /**
     * Saves the current file configuration to disk.
     * @param fileConfig The file configuration to save.
     */
    private static void saveFile(FileConfiguration fileConfig) {
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving challenges.yml file.");
        }
    }

    public void passiveSave() {
        // Get the list of challenges
        List<List<Object>> challenges = (List<List<Object>>) fileConfig.getList("challenges");
        Main.debug("Got list of challenges.");

        // Init if null or empty
        if (challenges == null || challenges.isEmpty()) {
            Main.debug("No challenges; creating a new list");
            challenges = new ArrayList<>();
        }

        // Challenge -> List
        List<Object> yamlChallenge = new ArrayList<>();

        Main.debug("Converting to YAML...");
        yamlChallenge.add(this.attacker);
        yamlChallenge.add(this.defender);
        yamlChallenge.add(this.type.toString());

        List<String> dateTimes = new ArrayList<>();

        for (LocalDateTime dateTime : this.time) {
            dateTimes.add(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        yamlChallenge.add(dateTimes);
        yamlChallenge.add(finalized);

        // Add the given challenge
        challenges.add(yamlChallenge);
        Main.debug("Added to List");

        fileConfig.set("challenges", challenges);
        Main.debug("Saving to file");
        saveFile(fileConfig);
    }
}