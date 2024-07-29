package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Challenge {

    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();
    private static File file;
    @NotNull
    public final UUID attacker;
    @NotNull
    public final UUID defender;
    @NotNull
    public List<LocalDateTime> time;
    @NotNull
    public State state;
    private boolean attackerCanceled = false;
    private boolean defenderCanceled = false;

    /**
     * Create a new challenge.
     *
     * @param attackerUUID   The UUID of the attacking player.
     * @param defenderUUID   The UUID of the defending player.
     * @param challengeTimes The possible dates/times of the challenge.
     */
    public Challenge(@NotNull UUID attackerUUID, @NotNull UUID defenderUUID, @NotNull List<LocalDateTime> challengeTimes, @NotNull State challengeState) {
        attacker = attackerUUID;
        defender = defenderUUID;
        time = challengeTimes;
        state = challengeState;
    }

    @Contract(" -> new")
    private static @NotNull YamlConfiguration getConfig() {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(file);
            return configuration;
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().severe("Unable to load " + file.getName() + "!\n" + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new RuntimeException();
        }
    }

    @Nullable
    public static Challenge getChallenge(@NotNull UUID uuid) {
        Challenge playerChallenge = null;

        for (Challenge challenge : getChallenges()) {
            if (Objects.equals(challenge.attacker, uuid) || Objects.equals(challenge.defender, uuid)) {
                playerChallenge = challenge;
                break;
            }
        }
        return playerChallenge;
    }

    private static @NotNull YamlConfiguration toYaml(@NotNull List<Challenge> challenges) {

        YamlConfiguration fileConfig = new YamlConfiguration();
        List<List<Object>> yamlChallenges = new ArrayList<>();

        for (Challenge challenge : challenges) {
            // Challenge -> List
            List<Object> yamlChallenge = new ArrayList<>();

            Dreamvisitor.debug("Converting to YAML...");
            yamlChallenge.add(challenge.attacker.toString());
            yamlChallenge.add(challenge.defender.toString());

            List<String> dateTimes = new ArrayList<>();

            for (LocalDateTime dateTime : challenge.time) {
                dateTimes.add(dateTime.format(DateTimeFormatter.ISO_DATE_TIME));
            }
            yamlChallenge.add(dateTimes);
            yamlChallenge.add(challenge.state.toString());
            yamlChallenge.add(challenge.attackerCanceled);
            yamlChallenge.add(challenge.defenderCanceled);

            // Add the given challenge
            yamlChallenges.add(yamlChallenge);
        }

        fileConfig.set("challenges", yamlChallenges);

        return fileConfig;
    }

    public static void remove(@NotNull Challenge challenge) {
        Dreamvisitor.debug("Removing challenge of " + challenge.attacker + " and  " + challenge.defender);
        List<Challenge> challenges = getChallenges();

        Dreamvisitor.debug("Challenge found? " + challenges.removeIf(challenge1 -> challenge1.defender.equals(challenge.defender)));

        saveFile(toYaml(challenges));
    }

    /**
     * Deletes all challenges that contain a given player.
     *
     * @param uuid   the players whose challenges to delete.
     * @param reason the reason for the challenge being removed.
     *               This will be sent in a notification to the other party.
     *               If this is {@code null}, no notification will be sent.
     */
    public static void removeChallengesOfPlayer(@NotNull UUID uuid, @Nullable String reason) {
        Dreamvisitor.debug("Removing challenges of " + uuid);
        List<Challenge> challenges = getChallenges();
        for (Challenge challenge : challenges) {
            Dreamvisitor.debug("Checking challenge of " + challenge.defender + " and " + challenge.attacker);
            if (challenge.defender.equals(uuid) || challenge.attacker.equals(uuid)) {
                Dreamvisitor.debug("Found match.");
                Notification.removeNotificationsOfPlayer(challenge.attacker, Notification.Type.CHALLENGE_REQUESTED);
                Notification.removeNotificationsOfPlayer(challenge.defender, Notification.Type.CHALLENGE_ACCEPTED);
                if (reason != null) {
                    if (challenge.defender == uuid)
                        new Notification(challenge.attacker, "Your challenge was canceled.", reason, Notification.Type.GENERIC).create();
                    if (challenge.attacker == uuid)
                        new Notification(challenge.defender, "Your challenge was canceled.", reason, Notification.Type.GENERIC).create();
                }
                Challenge.remove(challenge);
            }
        }
        saveFile(toYaml(challenges));
    }

    /**
     * Deletes all challenges that contain the given players.
     *
     * @param attacker the first player whose challenges to delete.
     * @param defender the second player whose challenges to delete.
     */
    public static void removeChallengesOfPlayers(@NotNull UUID attacker, @NotNull UUID defender) {
        List<Challenge> challenges = getChallenges();
        for (Challenge challenge : challenges) {
            if (challenge.attacker == attacker && challenge.defender == defender) {
                Challenge.remove(challenge);
            }
        }
        saveFile(toYaml(challenges));
    }

    /**
     * Get all challenges that contain both the given players.
     *
     * @param attacker the first player whose challenges to get.
     * @param defender the second player whose challenges to get.
     */
    public static @Nullable Challenge getChallengeOfPlayers(UUID attacker, UUID defender) {
        Dreamvisitor.debug("Getting challenge of players " + attacker + " and " + defender);
        for (Challenge challenge : getChallenges()) {
            Dreamvisitor.debug("Checking " + challenge.attacker + " and " + challenge.defender);
            if (challenge.attacker.equals(attacker) && challenge.defender.equals(defender)) {
                Dreamvisitor.debug("Match.");
                return challenge;
            }
        }

        Dreamvisitor.debug("No match.");
        return null;
    }

    /**
     * Retrieves all challenges from challenges.yml.
     *
     * @return A list of {@link Challenge}s. If none exist, the list will return empty.
     */
    @SuppressWarnings("unchecked")
    public static @NotNull List<Challenge> getChallenges() {

        YamlConfiguration fileConfig = getConfig();

        // Get the list of challenges
        List<List<Object>> yamlChallenges = (List<List<Object>>) fileConfig.getList("challenges");

        List<Challenge> challenges = new ArrayList<>();

        if (yamlChallenges != null && !yamlChallenges.isEmpty()) {
            for (List<Object> yamlChallenge : yamlChallenges) {

                // Get saved values
                UUID attacker = UUID.fromString((String) yamlChallenge.get(0));
                UUID defender = UUID.fromString((String) yamlChallenge.get(1));

                List<String> dateTimes = (List<String>) yamlChallenge.get(2);
                List<LocalDateTime> parsedTimes = new ArrayList<>();

                for (String dateTime : dateTimes) {
                    parsedTimes.add(LocalDateTime.parse(dateTime));
                }

                State state = State.valueOf((String) yamlChallenge.get(3));
                boolean attackerCanceled = (boolean) yamlChallenge.get(4);
                boolean defenderCanceled = (boolean) yamlChallenge.get(5);

                // Add to a Challenge object
                Challenge challenge = new Challenge(attacker, defender, parsedTimes, state);
                challenge.attackerCanceled = attackerCanceled;
                challenge.defenderCanceled = defenderCanceled;

                // Add to list
                challenges.add(challenge);
            }
        }

        // Return List<Challenge> that was built
        return challenges;
    }

    /**
     * Initializes the challenge storage.
     *
     * @throws IOException If the file could not be created.
     */
    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "challenges.yml");

        if (!file.exists()) {
            Bukkit.getLogger().info("challenges.yml does not exist. Creating one...");
            file.createNewFile();
        }
    }

    /**
     * Saves the current file configuration to disk.
     *
     * @param fileConfig The file configuration to save.
     */
    private static void saveFile(@NotNull FileConfiguration fileConfig) {
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving challenges.yml file.");
        }
    }

    public boolean isAccepted() {
        return (state == State.ACCEPTED || state == State.SCHEDULED);
    }

    public boolean isScheduled() {
        return (state == State.SCHEDULED);
    }

    public void cancelAttacker() {
        attackerCanceled = true;
        if (defenderCanceled) cancel();
        else {
            new Notification(defender, "Your opponent wants to cancel their challenge", "Your opponent, " + PlayerUtility.getUsernameOfUuid(attacker) + ", wants to cancel their challenge. Run /challenge to open the details of your challenge and complete the cancellation.", Notification.Type.GENERIC).create();
            String attackerUsername = PlayerUtility.getUsernameOfUuid(attacker);
            String defenderUsername = PlayerUtility.getUsernameOfUuid(defender);
            RoyaltyBoard.report(attackerUsername, attackerUsername + " requested to cancel their challenge with " + defenderUsername);
            save();
        }
    }

    public void cancelDefender() {
        defenderCanceled = true;
        if (attackerCanceled) cancel();
        else {
            new Notification(attacker, "Your opponent wants to cancel their challenge", "Your opponent, " + PlayerUtility.getUsernameOfUuid(defender) + ", wants to cancel their challenge. Run /challenge to open the details of your challenge and complete the cancellation.", Notification.Type.GENERIC).create();
            String attackerUsername = PlayerUtility.getUsernameOfUuid(attacker);
            String defenderUsername = PlayerUtility.getUsernameOfUuid(defender);
            RoyaltyBoard.report(defenderUsername, defenderUsername + " requested to cancel their challenge with " + attackerUsername);
            save();
        }
    }

    private void cancel() {
        String attackerUsername = PlayerUtility.getUsernameOfUuid(attacker);
        String defenderUsername = PlayerUtility.getUsernameOfUuid(defender);
        RoyaltyBoard.report(null, "The challenge between " + attackerUsername + " and " + defenderUsername + " was canceled upon agreement.");
        Challenge.remove(this);
        new Notification(attacker, "Your challenge was canceled", "You and your opponent, " + defenderUsername + ", both agreed to cancel the challenge.", Notification.Type.GENERIC).create();
        new Notification(defender, "Your challenge was canceled", "You and your opponent, " + attackerUsername + ", both agreed to cancel the challenge.", Notification.Type.GENERIC).create();
    }

    public boolean isAttackerCanceled() {
        return attackerCanceled;
    }

    public boolean isDefenderCanceled() {
        return defenderCanceled;
    }

    /**
     * Saves the challenge to challenge.yml on disk.
     * If the challenge given matches an attacker/defender pair, it will be updated.
     */
    public void save() {

        List<Challenge> challenges = getChallenges();

        Challenge challenge = getChallengeOfPlayers(attacker, defender);
        if (challenge != null) {
            challenges.remove(challenge);
            challenges.add(this);
        } else challenges.add(this);

        // Challenge -> List
        saveFile(toYaml(challenges));

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Challenge challenge = (Challenge) o;
        return attackerCanceled == challenge.attackerCanceled && defenderCanceled == challenge.defenderCanceled && Objects.equals(attacker, challenge.attacker) && Objects.equals(defender, challenge.defender) && Objects.equals(time, challenge.time) && state == challenge.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(attacker, defender, time, state, attackerCanceled, defenderCanceled);
    }

    public enum State {
        PROPOSED,
        ACCEPTED,
        SCHEDULED
    }

}