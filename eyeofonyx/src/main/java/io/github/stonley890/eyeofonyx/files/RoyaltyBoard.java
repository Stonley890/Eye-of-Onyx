package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.Utils;
import javassist.NotFoundException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.shanerx.mojang.Mojang;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class RoyaltyBoard {

    private static File file;
    private static FileConfiguration boardFile;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();
    private static final Mojang mojang = new org.shanerx.mojang.Mojang().connect();
    private static TextChannel boardChannel;
    /**
     * Whether the royalty board is frozen
     */
    private static boolean frozen;

    // Team names
    private static final String[] teamNames = {
            "HiveWing", "IceWing", "LeafWing", "MudWing", "NightWing", "RainWing", "SandWing", "SeaWing", "SilkWing",
            "SkyWing"
    };

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

    public static void setFrozen(boolean value) {
        frozen = value;
        plugin.getConfig().set("frozen", value);
        plugin.saveConfig();
    }

    public static boolean isFrozen() {
        return frozen;
    }


    /**
     * Initializes the royalty board.
     */
    public static void setup() {

        file = new File(plugin.getDataFolder(), "board.yml");

        if (!file.exists()) {
            Bukkit.getLogger().info("board.yml does not exist. Creating one...");
            file.getParentFile().mkdirs();
            plugin.saveResource("board.yml", false);
        }

        boardFile = YamlConfiguration.loadConfiguration(file);
        save(boardFile);

        long channelID = plugin.getConfig().getLong("royalty-board-channel");
        boardChannel = Bot.getJda().getTextChannelById(channelID);

    }

    /**
     * Get the YAML FileConfiguration for board.yml. You usually shouldn't need to use this unless there isn't a method available to manipulate the data.
     *
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
        }
    }

    /**
     * Reload the file from disk.
     */
    public static void reload() {
        Dreamvisitor.debug("Reloading.");
        boardFile = YamlConfiguration.loadConfiguration(file);
        frozen = plugin.getConfig().getBoolean("frozen");
    }

    /**
     * The first thing this method does is reload the file from disk.
     * After that, it will automatically search for and remove inactive
     * players as well as move players up into empty ranks if they exist.
     * It will also set the name on board.yml based on the character
     * name stored from OpenRP (if it exists).
     */
    public static void updateBoard() {

        Dreamvisitor.debug("Updating royalty board.");

        // reload();
        // joined_time access
        String last_online;
        // Track current position
        String currentPath;
        // Count the number of empty positions
        int positionsEmpty;

        // For each tribe
        for (int tribe = 0; tribe < tribes.length; tribe++) {

            positionsEmpty = 0;

            // For each position
            for (int pos = 0; pos < 5; pos++) {

                // Set current path
                currentPath = tribes[tribe] + "." + validPositions[pos];

                String uuid = getUuid(tribe, pos);

                if (uuid != null) {
                    // Set name from OpenRP character
                    if (EyeOfOnyx.openrp != null) {
                        String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                        if (ocName != null && !ocName.equals("No name set")) {
                            boardFile.set(currentPath + ".name", ocName);
                        }
                    }
                }

                // If last_online is before inactivity period, clear position
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

                    // Update LuckPerms
                    if (uuid != null && !uuid.equals("none")) {
                        try {
                            int playerTribe = PlayerTribe.getTribeOfPlayer(uuid);
                            Utils.setPlayerPerms(uuid, playerTribe, pos);
                        } catch (NotFoundException e) {
                            // no tribe
                        }
                    }

                    // Clear data
                    boardFile.set(currentPath + ".uuid", "none");
                    boardFile.set(currentPath + ".name", "none");
                    boardFile.set(currentPath + ".last_challenge_time", "none");
                    boardFile.set(currentPath + ".challenger", "none");
                    boardFile.set(currentPath + ".last_online", "none");
                    boardFile.set(currentPath + ".joined_time", "none");

                    // If position is not ruler, change 'challenging'
                    if (!validPositions[pos].equals(validPositions[RULER])) {
                        boardFile.set(currentPath + ".challenging", "none");
                    }

                } // If position is held by an active player
                else {
                    // If any previous position was empty, move this user up that many positions
                    // positionsEmpty is initialized as 0 so this cannot run as ruler
                    if (positionsEmpty > 0) {

                        Dreamvisitor.debug("Positions empty: " + positionsEmpty);
                        Dreamvisitor.debug("Current position: " + pos);

                        int emptyPosition = pos - positionsEmpty;

                        String tribeName = tribes[tribe];
                        String posName = validPositions[pos];
                        String emptyPosName = validPositions[emptyPosition];

                        // copy uuid & name to first empty position (thisPosition - emptyPositions)
                        boardFile.set(tribeName + "." + emptyPosName + ".uuid",
                                boardFile.get(tribeName + "." + posName + ".uuid"));
                        boardFile.set(tribeName + "." + emptyPosName + ".name",
                                boardFile.get(tribeName + "." + posName + ".name"));

                        // Update last_challenge and joined_time
                        // This will give the user movement cooldown
                        boardFile.set(tribeName + "." + emptyPosName + ".last_challenge_time",
                                LocalDateTime.now().toString());
                        boardFile.set(tribeName + "." + emptyPosName + ".joined_time",
                                LocalDateTime.now().toString());
                        boardFile.set(tribeName + "." + emptyPosName + ".last_online",
                                LocalDateTime.now().toString());

                        // Clear data
                        boardFile.set(currentPath + ".uuid", "none");
                        boardFile.set(currentPath + ".name", "none");
                        boardFile.set(currentPath + ".joined_time", "none");
                        boardFile.set(currentPath + ".last_challenge_time", "none");
                        boardFile.set(currentPath + ".challenger", "none");
                        boardFile.set(currentPath + ".challenging", "none");
                        boardFile.set(currentPath + ".last_online", "none");

                        // Notify the user who has moved
                        if (boardFile.getString(tribeName + "." + validPositions[pos - positionsEmpty] + ".uuid") != null && !boardFile.getString(tribeName + "." + validPositions[pos - positionsEmpty] + ".uuid").equals("none")) {
                            new Notification(boardFile.getString(tribeName + "." + validPositions[pos - positionsEmpty] + ".uuid"), "You've been promoted!", "A player was removed from the royalty board and you moved into a higher position.", NotificationType.GENERIC).create();
                        }

                        // This position is now empty
                        // and another user will move up on the next iteration
                        // if there is an active user below this position
                    }
                }
            }
        }

        save(boardFile);
        updatePermissions();

    }

    public static void updateDiscordBoard() throws IOException {
        // Get channel and message
        List<Long> messageIDs = plugin.getConfig().getLongList("royalty-board-message");

        if (boardChannel == null) {
            Bukkit.getLogger().warning("Could not get royalty board channel!");
        } else {

            // Build message

            if (messageIDs.size() != 10) messageIDs = new ArrayList<>(10);
            // Create message list
            List<String> messages = new ArrayList<>();

            // for each tribe...
            for (int i = 0; i < RoyaltyBoard.tribes.length; i++) {

                // Get base message from messageformat.txt
                String message = MessageFormat.get();

                // Replace $EMBLEM with appropriate tribe emoji
                if (!plugin.getConfig().getStringList("tribe-emblems").isEmpty()) {
                    message = message.replace("$EMBLEM", plugin.getConfig().getStringList("tribe-emblems").get(i));
                }

                // Replace $TRIBE with appropriate tribe name
                message = message.replace("$TRIBE", RoyaltyBoard.tribes[i].substring(0, 1).toUpperCase() + RoyaltyBoard.tribes[i].substring(1));

                // for each position...
                String[] positions = RoyaltyBoard.getValidPositions();
                for (int j = 0; j < positions.length; j++) {
                    final String position = positions[j];

                    String name = "N/A";
                    String username = "N/A";
                    String joined = "";

                    Dreamvisitor.debug("Getting info for tribe " + i + " position " + j);

                    // Get info if not empty
                    if (!RoyaltyBoard.getUuid(i, j).equals("none")) {
                        Dreamvisitor.debug("Player here");

                        username = mojang.getPlayerProfile(RoyaltyBoard.getUuid(i, j)).getUsername();

                        name = ChatColor.stripColor(RoyaltyBoard.getOcName(i, j));
                        if (name == null || name.equals("&c<No name set>")) name = username;

                        joined = "<t:" + LocalDateTime.parse(RoyaltyBoard.getJoinedDate(i, j)).toEpochSecond(ZoneOffset.UTC) + ":d>";
                    }

                    // Replace in message
                    message = message.replace("$" + position.toUpperCase() + "-NAME", name)
                            .replace("$" + position.toUpperCase() + "-USERNAME", username)
                            .replace("$" + position.toUpperCase() + "-JOINED", joined);

                    // Update Discord roles
                    if (!RoyaltyBoard.getUuid(i, j).equals("none") && !Dreamvisitor.botFailed) {

                        // Get recorded user ID
                        String userId = AccountLink.getDiscordId(getUuid(i, j).replaceAll("-", ""));

                        if (userId != null) {
                            // Get sister guild
                            Guild sisterGuild = Bot.getJda().getGuildById(Dreamvisitor.getPlugin().getConfig().getLong("tribeGuildID"));
                            if (sisterGuild != null) {
                                // Get role
                                Role tribeRole = sisterGuild.getRoleById(plugin.getConfig().getLongList("sister-royalty-roles").get(i));
                                if (tribeRole != null && userId != null) {
                                    // Get user and add role
                                    sisterGuild.retrieveMemberById(userId).queue(user -> {
                                        sisterGuild.addRoleToMember(user, tribeRole).queue();
                                    }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MEMBER, (error) -> {
                                        Dreamvisitor.debug("Could not get member of ID " + userId + " in sister server.");
                                    }));
                                }
                            }

                            // Get main guild
                            Guild mainGuild = Bot.gameLogChannel.getGuild();
                            // Get appropriate role
                            Role royaltyRole = null;
                            if (j == 0)
                                royaltyRole = mainGuild.getRoleById(plugin.getConfig().getLong("main-royalty-roles.ruler"));
                            if (j == 1 || j == 2)
                                royaltyRole = mainGuild.getRoleById(plugin.getConfig().getLong("main-royalty-roles.heir"));
                            if (j == 3 || j == 4)
                                royaltyRole = mainGuild.getRoleById(plugin.getConfig().getLong("main-royalty-roles.noble"));
                            // Add to user
                            if (royaltyRole != null) {
                                Role finalRoyaltyRole = royaltyRole;
                                mainGuild.retrieveMemberById(userId).queue(user -> {
                                    mainGuild.addRoleToMember(user, finalRoyaltyRole).queue();
                                }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MEMBER, (error) -> {
                                    Dreamvisitor.debug("Could not get member of ID " + userId + " in main server.");
                                }));
                            }
                        } else {
                            Bukkit.getLogger().warning(username + " does not have an associated Discord ID!");
                        }
                    }
                }

                messages.add(message);

            } // end of for loop

            if (messageIDs.isEmpty() || messageIDs.size() < 10) {
                Bukkit.getLogger().warning("Some royalty board messages are not recorded. Creating new ones.");

                // send new messages
                List<Long> newMessageIDs = new ArrayList<>();
                for (String message : messages) {
                    newMessageIDs.add(boardChannel.sendMessage(message).complete().getIdLong());
                }
                // save sent message IDs for later editing
                plugin.getConfig().set("royalty-board-message", newMessageIDs);
                plugin.saveConfig();

            } else {
                // Try to edit messages
                for (int i = 0; i < messages.size(); i++) {
                    // Get message
                    final String finalMessage = messages.get(i);
                    final Long targetMessageId = messageIDs.get(i);
                    try {
                        boardChannel.retrieveMessageById(targetMessageId).queue((targetMessage) -> {
                            targetMessage.editMessage(finalMessage).queue();
                        }, new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (error) -> {
                            Bukkit.getLogger().warning("Unknwon Message! Couldn't get message " + targetMessageId);
                        }));

                    } catch (InsufficientPermissionException e) {
                        Bukkit.getLogger().warning("Dreamvisitor Bot does not have permission to get the royalty board message!");
                    }
                }

            }

        }

    }

    /**
     * Updates LuckPerms groups for all users on the royalty board. This will not work if LuckPerms was not initialized on enable.
     */
    public static void updatePermissions() {

        Dreamvisitor.debug("Updating LuckPerms permissions");
        if (EyeOfOnyx.luckperms != null) {

            // Get user manager
            UserManager userManager = EyeOfOnyx.luckperms.getUserManager();

            // Go through each entry in the royalty board
            for (int t = 0; t < tribes.length; t++) {
                for (int p = 0; p < validPositions.length; p++) {


                    // Only update if there is a player in the position
                    if (!getUuid(t, p).equals("none")) {
                        // Get user at tribe t and position p
                        CompletableFuture<User> userFuture = userManager.loadUser(java.util.UUID.fromString(getUuid(t, p)));

                        // Run async
                        int finalP = p;
                        int finalT = t;
                        userFuture.thenAcceptAsync(user -> {

                            String[] groupPositions = {"ruler", "heir", "noble", "citizen"};

                            // For each tribe and position...
                            for (String tribe : tribes) {
                                for (String position : groupPositions) {

                                    // ...get the lp group name from config
                                    String groupName = plugin.getConfig().getString(position + "." + tribe);

                                    if (groupName != null) {
                                        // Get the group from lp and remove it from the user.
                                        user.data().remove(net.luckperms.api.node.Node.builder("group." + groupName).build());

                                    } else
                                        Bukkit.getLogger().warning("Group " + position + "." + tribe + " is null in the config!");
                                }
                            }

                            // Now that all have been removed, add the correct one

                            String group;
                            if (finalP == 0) {
                                group = "ruler";
                            } else if (finalP < 3) {
                                group = "heir";
                            } else {
                                group = "noble";
                            }

                            String groupName = plugin.getConfig().getString(group + "." + tribes[finalT]);

                            if (groupName != null) {
                                // Get the group from lp and add it to the user.
                                user.data().add(Node.builder("group." + groupName).build());
                            }

                            userManager.saveUser(user);
                        });
                    }

                }
            }
        } else
            Bukkit.getLogger().warning("Eye of Onyx could not hook into LuckPerms on startup. Permission update failed.");
    }

    public static boolean positionEmpty(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, "uuid").equals("none");
    }

    /**
     * Get the position index of a player UUID.
     *
     * @param playerUuid The player UUID to search from.
     * @return The index of the position that the player holds.
     */
    public static int getPositionIndexOfUUID(String playerUuid) throws NotFoundException {

        // Get player tribe
        int playerTribe = PlayerTribe.getTribeOfPlayer(playerUuid);

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
     * Remove an entry from board.yml. This does not update the board. Use updateBoard() to update the board.
     *
     * @param tribeIndex    The tribe.
     * @param positionIndex The position.
     */
    public static void removePlayer(int tribeIndex, int positionIndex) {

        String tribe = getTribes()[tribeIndex];
        String pos = getValidPositions()[positionIndex];

        boardFile.set(tribe + "." + pos + ".uuid", "none");
        boardFile.set(tribe + "." + pos + ".name", "none");
        boardFile.set(tribe + "." + pos + ".joined_time", "none");
        boardFile.set(tribe + "." + pos + ".last_online", "none");
        boardFile.set(tribe + "." + pos + ".uuid", "none");
        boardFile.set(tribe + "." + pos + ".challenger", "none");

        if (tribeIndex != RULER) {
            boardFile.set(tribe + "." + pos + ".challenging", "none");
        }
        save(boardFile);

    }

    /**
     * Get a specific value from board.yml.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @param value         The value to retrieve.
     * @return The value requested, if it exists. {@code null} if it does not.
     */
    public static String getValue(int tribeIndex, int positionIndex, String value) {
        return boardFile.getString(tribes[tribeIndex] + "." + validPositions[positionIndex] + "." + value);
    }

    /**
     * Set a specific value on board.yml.
     *
     * @param tribeIndex    The tribe to set.
     * @param positionIndex The position to set.
     * @param key           The value to set.
     * @param value         The variable to set to.
     */
    public static void setValue(int tribeIndex, int positionIndex, String key, String value) {
        Dreamvisitor.debug("Value " + key + " of tribe " + tribeIndex + " position " + positionIndex + " set to " + value);
        boardFile.set(tribes[tribeIndex] + "." + validPositions[positionIndex] + "." + key, value);
        // save(boardFile);
    }

    /**
     * Get the UUID of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The String UUID located at the given location.
     */
    public static String getUuid(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, UUID);
    }

    /**
     * Get the character name of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The character name located at the given location.
     */
    public static String getOcName(int tribeIndex, int positionIndex) {

        return getValue(tribeIndex, positionIndex, NAME);
    }

    /**
     * Get the date joined of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The date joined located at the given location.
     */
    public static String getJoinedDate(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, JOINED_TIME);
    }

    /**
     * Get the date last online of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The date joined last online at the given location.
     */
    public static String getLastOnline(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, LAST_ONLINE);
    }

    /**
     * Get the date last challenged of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The date last challenged at the given location.
     */
    public static String getLastChallengeDate(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, LAST_CHALLENGE_TIME);
    }

    /**
     * Get the attacker of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @return The attacker at the given location.
     */
    public static String getAttacker(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, CHALLENGER);
    }

    /**
     * Set the attacker of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @param uuid          The UUID to set as the attacker.
     */
    public static void setAttacker(int tribeIndex, int positionIndex, String uuid) {
        setValue(tribeIndex, positionIndex, "challenger", uuid);
        save(boardFile);
    }

    /**
     * Get the target of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     */
    public static String getAttacking(int tribeIndex, int positionIndex) {
        return getValue(tribeIndex, positionIndex, CHALLENGING);
    }

    /**
     * Set the target of a spot on the board.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     * @param uuid          The UUID to set.
     */
    public static void setAttacking(int tribeIndex, int positionIndex, String uuid) {
        setValue(tribeIndex, positionIndex, "challenging", uuid);
        save(boardFile);
    }

    /**
     * Get whether a position is on cool down or not.
     *
     * @param tribeIndex    The tribe to fetch from.
     * @param positionIndex The position to fetch from.
     */
    public static boolean isOnCoolDown(int tribeIndex, int positionIndex) {
        LocalDateTime lastChallenge = LocalDateTime.parse(getLastChallengeDate(tribeIndex, positionIndex));
        return lastChallenge.isAfter(LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down")));
    }

    /**
     * Get whether a position is on cool down or not. Will return false is player is not part of a tribe.
     *
     * @param uuid The player UUID to fetch from.
     */
    public static boolean isOnCoolDown(String uuid) {
        int tribe;
        int pos;
        try {
            tribe = PlayerTribe.getTribeOfPlayer(uuid);
            pos = RoyaltyBoard.getPositionIndexOfUUID(uuid);
        } catch (NotFoundException e) {
            return false;
        }
        LocalDateTime lastChallenge = LocalDateTime.parse(getLastChallengeDate(tribe, pos));
        return lastChallenge.isBefore(LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down")));
    }

    private RoyaltyBoard() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Moves a player from a given position to another given position.
     * If a player is in the position to be moved to, they will be removed.
     *
     * @param tribeIndex
     * @param fromPositionIndex
     * @param toPositionIndex
     */
    public static void move(int tribeIndex, int fromPositionIndex, int toPositionIndex) {
        // removePlayer(tribeIndex, toPositionIndex);
        String uuid = getUuid(tribeIndex, fromPositionIndex);
        String name = getOcName(tribeIndex, fromPositionIndex);
        LocalDateTime now = LocalDateTime.now();
        String tribe = getTribes()[tribeIndex];
        String pos = getValidPositions()[toPositionIndex];
        boardFile.set(tribe + "." + pos + ".uuid", uuid);
        boardFile.set(tribe + "." + pos + ".name", name);
        boardFile.set(tribe + "." + pos + ".joined_time", now.toString());
        boardFile.set(tribe + "." + pos + ".last_online", now.toString());
        boardFile.set(tribe + "." + pos + ".last_challenge_time", now.toString());
        boardFile.set(tribe + "." + pos + ".challenger", "none");
        if (toPositionIndex != RULER) boardFile.set(tribe + "." + pos + ".challenging", "none");
        removePlayer(tribeIndex, fromPositionIndex);
    }
}
