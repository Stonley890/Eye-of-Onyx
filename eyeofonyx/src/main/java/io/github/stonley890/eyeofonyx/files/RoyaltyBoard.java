package io.github.stonley890.eyeofonyx.files;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.shanerx.mojang.Mojang;

public class RoyaltyBoard {

    private static File file;
    private static FileConfiguration boardFile;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();
    private static final Mojang mojang = new org.shanerx.mojang.Mojang().connect();
    private static final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    // Team names
    private static final String[] teamNames = {
        "HiveWing", "IceWing", "LeafWing", "MudWing", "NightWing", "RainWing", "SandWing", "SeaWing", "SilkWing",
        "SkyWing"
    };

    public static final int HIVE = 0;
    public static final int ICE = 1;
    public static final int LEAF = 2;
    public static final int MUD = 3;
    public static final int NIGHT = 4;
    public static final int RAIN = 5;
    public static final int SAND = 6;
    public static final int SEA = 7;
    public static final int SILK = 8;
    public static final int SKY = 9;

    // Tribe IDs
    private static final String[] tribes = {
            "hive", "ice", "leaf", "mud", "night", "rain", "sand", "sea", "silk", "sky"
    };

    // Valid positions
    private static final String[] validPositions = {
            "ruler", "heir_apparent", "heir_presumptive", "noble_apparent", "noble_presumptive"
    };

    // Stored values
    public static final String UUID = "uuid";
    public static final String TITLE = "title";
    public static final String NAME = "name";
    public static final String JOINED_TIME = "joined_time";
    public static final String LAST_ONLINE = "last_online";
    public static final String LAST_CHALLENGE_TIME = "last_challenge_time";
    public static final String CHALLENGER = "challenger";
    public static final String CHALLENGING = "challenging";


    public static final int RULER = 0;
    public static final int HEIR_APPARENT = 1;
    public static final int HEIR_PRESUMPTIVE = 2;
    public static final int NOBLE_APPARENT = 3;
    public static final int NOBLE_PRESUMPTIVE = 4;
    public static final int CIVILIAN = 5;


    public static String[] getTeamNames() {
        return teamNames;
    }

    public static String[] getTribes() {
        return tribes;
    }

    public static String[] getValidPositions() {
        return validPositions;
    }

    /**
     * Initializes the royalty board.
     */
    public static void setup() {

        file = new File(plugin.getDataFolder(), "board.yml");

        if (!file.exists()) {
            Dreamvisitor.debug("board.yml does not exist. Creating one...");
            file.getParentFile().mkdirs();
            plugin.saveResource("board.yml", false);
        }

        boardFile = YamlConfiguration.loadConfiguration(file);
        save(boardFile);
    }

    /**
     * Get the YAML FileConfiguration for board.yml. You usually shouldn't need to use this unless there isn't a method available to manipulate the data.
     * @return A {@link FileConfiguration} of board.yml.
     */
    public static FileConfiguration get() {
        return boardFile;
    }

    public static void save(FileConfiguration board) {
        boardFile = board;
        try {
            boardFile.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Eye of Onyx could not save board.yml! If this persists after a restart, report this error!");
            e.printStackTrace();
        }
    }

    /**
     * Reload the file from disk.
     */
    public static void reload() {
        boardFile = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * The first thing this method does is reload the file from disk.
     * After that, it will automatically search for and remove inactive
     * players as well as move players up into empty ranks if they exist.
     * It will also set the name on board.yml based on the character
     * name stored from OpenRP (if it exists).
     */
    public static void updateBoard() {

        reload();
        // joined_time access
        String last_online;
        // Track current position
        String currentPath;
        // Count the number of empty positions
        int positionsEmpty = 0;

        // For each tribe
        for (int tribe = 0; tribe < tribes.length; tribe++) {

            positionsEmpty = 0;

            // For each position
            for (int pos = 0; pos < 5; pos++) {

                // Set current path
                currentPath = tribes[tribe] + "." + validPositions[pos];

                // Set name from OpenRP character
                if (EyeOfOnyx.openrp != null) {
                    String uuid = getUuid(tribe, pos);
                    String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                    if (ocName != null && !ocName.equals("No name set")) {
                        setValue(tribe, pos, plugin.getConfig().getString("character-name-field"), ocName);
                    }
                }

                // If last_online is before 30 days ago, set to empty
                last_online = getLastOnline(tribe, pos);
                if (last_online != null && !last_online.equals("none")) {
                    if (LocalDateTime.parse(last_online).isBefore(LocalDateTime.now().minusDays(30))) {
                        boardFile.set(currentPath + ".last_online", "none");
                    }
                }

                // If last_online is empty, clear position
                if (last_online == null || last_online.equals("none")) {

                    // This position is empty, so count up positionsEmpty
                    positionsEmpty += 1;

                    boardFile.set(currentPath + ".uuid", "none");
                    boardFile.set(currentPath + ".name", "none");
                    boardFile.set(currentPath + ".last_challenge_time", "none");
                    boardFile.set(currentPath + ".challenger", "none");
                    boardFile.set(currentPath + ".last_online", "none");

                    // If position is ruler, change 'title.' Else, change 'challenging'
                    if (validPositions[pos].equals(validPositions[RULER])) {
                        boardFile.set(currentPath + ".title", "none");
                    } else {
                        boardFile.set(currentPath + ".challenging", "none");
                    }

                } // If position is held by an active player
                else {

                    // If any previous position was empty, move this user up that many positions
                    // positionsEmpty is initialized as 0 so this cannot run as ruler
                    if (positionsEmpty > 0) {

                        // copy uuid & name to first empty position (thisPosition - emptyPositions)
                        boardFile.set(tribe + "." + validPositions[pos - positionsEmpty] + ".uuid",
                                boardFile.get(currentPath + ".uuid"));
                        boardFile.set(tribe + "." + validPositions[pos - positionsEmpty] + ".name",
                                boardFile.get(currentPath + ".name"));

                        // Update last_challenge and joined_time
                        // This will give the user movement cooldown
                        boardFile.set(tribe + "." + validPositions[pos - positionsEmpty] + ".last_challenge_time",
                                LocalDateTime.now().toString());
                        boardFile.set(tribe + "." + validPositions[pos - positionsEmpty] + ".joined_time",
                                LocalDateTime.now().toString());
                        boardFile.set(tribe + "." + validPositions[pos - positionsEmpty] + ".last_online",
                                LocalDateTime.now().toString());

                        // If previous position is ruler, also set title
                        if (validPositions[pos - 1].equals(validPositions[RULER])) {
                            boardFile.set(tribe + "." + validPositions[pos - positionsEmpty] + ".title", "Ruler");
                        }

                        // TO DO: notify challenger of challenge cancellation

                        // Clear data
                        boardFile.set(currentPath + ".uuid", "none");
                        boardFile.set(currentPath + ".name", "none");
                        boardFile.set(currentPath + ".joined_time", "none");
                        boardFile.set(currentPath + ".last_challenge_time", "none");
                        boardFile.set(currentPath + ".challenger", "none");
                        boardFile.set(currentPath + ".challenging", "none");
                        boardFile.set(currentPath + ".last_online", "none");

                        // Notify the user who has moved
                        new Notification(getUuid(tribe, (pos - positionsEmpty)), "You've been promoted!", "A player was removed from the royalty board and you moved into a higher position.", NotificationType.GENERIC).create();

                        // This position is now empty and another user will move up on the next
                        // iteration
                        // if there is an active user below this position
                    }
                }

            }
        }

        save(boardFile);

    }

    /**
     * Get the tribe index of a player UUID.
     * @param playerUuid The player UUID to search from.
     * @return The index of the tribe that the player is part of.
     */
    public static int getTribeIndexOfUUID(String playerUuid) {
        // Get player name from Mojang
         String playerUsername = mojang.getPlayerProfile(playerUuid).getUsername();
        
        // Search for a valid team from player scoreboard team
        return Arrays.binarySearch(teamNames, Objects.requireNonNull(Objects.requireNonNull(scoreboard.getEntryTeam(playerUsername)).getName()));
    }

    /**
     * Get the tribe index of a player username.
     * @param playerUsername The player username to search from.
     * @return The index of the tribe that the player is part of.
     */
    public static int getTribeIndexOfUsername(String playerUsername) {

        // Search for valid team from player scoreboard team
        Team team = scoreboard.getEntryTeam(playerUsername);
        if (team != null) {
            return Arrays.binarySearch(teamNames, team.getName());
        } else {
            throw new RuntimeException("Player is not part of a team!");
        }
    }

    /**
     * Get the position index of a player UUID.
     * @param playerUuid The player UUID to search from.
     * @return The index of the position that the player holds.
     */
    public static int getPositionIndexOfUUID(String playerUuid) {

        // Get player tribe
        int playerTribe = getTribeIndexOfUUID(playerUuid);
        
        // Position is 5 by default (citizen)
        int playerPosition = CIVILIAN;

        // Iterate though positions to search for target player
        for (int i = 0; i < validPositions.length; i++) {
            if (RoyaltyBoard.getUuid(playerTribe, i).equals(playerUuid)) {
                // Change position if found on the royalty board
                playerPosition = i;
                break;
            }
        }
        
        return playerPosition;
    }

    /**
     * Get the position index of a player username.
     * @param playerUsername The player username to search from.
     * @return The index of the position that the player holds.
     */
    public static int getPositionIndexOfUsername(String playerUsername) {

        // Get player uuid from Mojang
        String playerUuid = mojang.getUUIDOfUsername(playerUsername);

        // Get player tribe
        int playerTribe = getTribeIndexOfUUID(playerUuid);

        // Position is 5 by default (citizen)
        int playerPosition = CIVILIAN;

        // Iterate though positions to search for target player
        for (int i = 0; i < validPositions.length; i++) {

            if (getUuid(playerTribe, i).replaceAll("-","").equals(playerUuid)) {
                // Change position if found on the royalty board
                playerPosition = i;
                break;
            }
        }

        return playerPosition;
    }

    /**
     * Get a specific value from board.yml.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @param value The value to retrieve.
     * @return The value requested, if it exists. {@code null} if it does not.
     */
    public static String getValue(int tribeIndex, int positionIndex, String value) {
        return boardFile.getString(tribes[tribeIndex] + "." + validPositions[positionIndex] + "." + value);
    }

    /**
     * Set a specific value on board.yml.
     * @param tribeIndex The tribe to set.
     * @param positionIndex The position to set.
     * @param key The value to set.
     * @param value The variable to set to.
     */
    public static void setValue(int tribeIndex, int positionIndex, String key, String value) {
        boardFile.set(tribes[tribeIndex] + "." + validPositions[positionIndex] + "." + key, value);
        save(boardFile);
    }

    /**
     * Get the UUID of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The String UUID located at the given location.
     */
    public static String getUuid(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, UUID);
    }

    /**
     * Get the character name of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The character name located at the given location.
     */
    public static String getOcName(int tribeIndex, int positionIndex) {

        return getValue(tribeIndex, positionIndex, NAME);
    }

    /**
     * Get the date joined of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The date joined located at the given location.
     */
    public static String getJoinedDate(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, JOINED_TIME);
    }

    /**
     * Get the date last online of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The date joined last online at the given location.
     */
    public static String getLastOnline(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, LAST_ONLINE);
    }

    /**
     * Get the date last challenged of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The date last challenged at the given location.
     */
    public static String getLastChallengeDate(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, LAST_CHALLENGE_TIME);
    }

    /**
     * Get the attacker of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The attacker at the given location.
     */
    public static String getAttacker(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, CHALLENGER);
    }

    /**
     * Set the attacker of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @param uuid The UUID to set as the attacker.
     */
    public static void setAttacker(int tribeIndex, int positionIndex, String uuid) {
        setValue(tribeIndex, positionIndex, "challenger", uuid);
    }

    /**
     * Get the target of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     */
    public static String getAttacking(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, CHALLENGING);
    }

    /**
     * Set the target of a spot on the board.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @param uuid The UUID to set.
     */
    public static void setAttacking(int tribeIndex, int positionIndex, String uuid) {
        setValue(tribeIndex, positionIndex, "challenging", uuid);

    }

    /**
     * Get whether a position is on cool down or not.
     * @param tribeIndex The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     */
    public static boolean isOnCoolDown(int tribeIndex, int positionIndex) {
        LocalDateTime lastChallenge = LocalDateTime.parse(getLastChallengeDate(tribeIndex, positionIndex));
        return lastChallenge.isAfter(LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down")));
    }

    /**
     * Get whether a position is on cool down or not.
     * @param uuid The player UUID to fetch from.
     */
    public static boolean isOnCoolDown(String uuid) {
        int tribe = getTribeIndexOfUUID(uuid);
        int pos = getPositionIndexOfUUID(uuid);
        LocalDateTime lastChallenge = LocalDateTime.parse(getLastChallengeDate(tribe, pos));
        return lastChallenge.isBefore(LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down")));
    }

    private RoyaltyBoard() {
        throw new IllegalStateException("Utility class");
    }
}
