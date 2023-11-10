package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.Utils;
import javassist.NotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
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
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RoyaltyBoard {

    private static Map<Integer, BoardState> royaltyBoard = new HashMap<>(10);
    public static Map<Integer, BoardState> getBoard() {return royaltyBoard;}
    public static BoardState getBoardOf(int tribe) {return royaltyBoard.get(tribe);}

    private static File file;
    private static FileConfiguration boardFile;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();
    private static final Mojang mojang = new org.shanerx.mojang.Mojang().connect();
    private static TextChannel boardChannel;
    /**
     * Whether the royalty board is frozen
     */
    private static boolean frozen;

    // private static List<RoyaltyAction> recordedActions;

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
        load();

        long channelID = plugin.getConfig().getLong("royalty-board-channel");
        boardChannel = Bot.getJda().getTextChannelById(channelID);

    }

    public static void saveFile(FileConfiguration board) {
        boardFile = board;
        try {
            boardFile.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Eye of Onyx could not save board.yml! If this persists after a restart, report this error!");
        }
    }

    public static void save() {
        saveFile(BoardState.createYamlConfiguration(royaltyBoard));
    }

    public static void save(Map<Integer, BoardState> boardStates) {
        saveFile(BoardState.createYamlConfiguration(boardStates));
    }

    public static void load() {
        royaltyBoard = BoardState.fromYamlConfig(boardFile);
    }

    /**
     * Reload the file from disk.
     */
    public static void reload() {
        Dreamvisitor.debug("Reloading.");
        boardFile = YamlConfiguration.loadConfiguration(file);
        frozen = plugin.getConfig().getBoolean("frozen");
        load();
    }

    public static void sendUpdate(RoyaltyAction action) {

        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {
            EmbedBuilder builder = new EmbedBuilder();

            long channelID = plugin.getConfig().getLong("royalty-log-channel");

            if (action.executor == null) action.executor = "**Eye of Onyx**";

            builder.setAuthor("Eye of Onyx")
                    .setTitle("Changes have been made to the " + teamNames[action.affectedTribe] + " board.");

            try {
                long discordID = Long.parseLong(action.executor);

                net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordID).complete();
                builder.setAuthor("This action was performed by " + user.getAsMention(), user.getAvatarUrl());

            } catch (NumberFormatException e) {
                builder.setFooter("This action was performed by " + action.executor);
            }

            Mojang mojang = new Mojang().connect();

            for (int i = 0; i < validPositions.length; i++) {

                BoardPosition oldPos = action.oldState.getPos(i);
                BoardPosition newPos = action.newState.getPos(i);

                if (!Objects.equals(oldPos, newPos)) {
                    StringBuilder changes = new StringBuilder();

                    String uuid = "N/A";
                    String username = "N/A";
                    String ocName = "N/A";
                    String lastOnline = "N/A";
                    String lastChallenge = "N/A";
                    String joinedBoard = "N/A";
                    String joinedPos = "N/A";
                    String challenger = "N/A";
                    String challenging = "N/A";

                    // OLD
                    if (oldPos.player != null) uuid = oldPos.player.toString();
                    if (oldPos.player != null) username = mojang.getPlayerProfile(uuid).getUsername();
                    if (oldPos.name != null) ocName = oldPos.name;
                    if (oldPos.lastOnline != null) lastOnline = oldPos.lastOnline.toString();
                    if (oldPos.lastChallenge != null) lastChallenge = oldPos.lastChallenge.toString();
                    if (oldPos.joinedBoard != null) joinedBoard = oldPos.joinedBoard.toString();
                    if (oldPos.joinedPosition != null) joinedPos = oldPos.joinedPosition.toString();
                    if (oldPos.challenger != null) challenger = mojang.getPlayerProfile(oldPos.challenger.toString()).getUsername();
                    if (oldPos.challenging != null) challenging = mojang.getPlayerProfile(oldPos.challenging.toString()).getUsername();

                    String discordUser = "N/A";

                    try {
                        long discordId = AccountLink.getDiscordId(oldPos.player);
                        net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordId).complete();
                        if (user != null) discordUser = user.getAsMention();
                    } catch (IllegalArgumentException e) {
                        // No user
                    }

                    changes.append("**BEFORE**")
                                    .append("Player: ").append(username)
                                    .append("\nUUID: ").append(uuid)
                                    .append("\nUser: ").append(discordUser)
                                    .append("\nOC Name: ").append(ocName)
                                    .append("\nLast Online: ").append(lastOnline)
                                    .append("\nLast Challenge: ").append(lastChallenge)
                                    .append("\nDate Joined Board: ").append(joinedBoard)
                                    .append("\nDate Joined Position: ").append(joinedPos)
                                    .append("\nChallenger: ").append(challenger)
                                    .append("\nChallenging: ").append(challenging);

                    // NEW
                    if (newPos.player != null) uuid = newPos.player.toString();
                    if (newPos.player != null) username = mojang.getPlayerProfile(uuid).getUsername();
                    if (newPos.name != null) ocName = newPos.name;
                    if (newPos.lastOnline != null) lastOnline = newPos.lastOnline.toString();
                    if (newPos.lastChallenge != null) lastChallenge = newPos.lastChallenge.toString();
                    if (newPos.joinedBoard != null) joinedBoard = newPos.joinedBoard.toString();
                    if (newPos.joinedPosition != null) joinedPos = newPos.joinedPosition.toString();
                    if (newPos.challenger != null) challenger = mojang.getPlayerProfile(newPos.challenger.toString()).getUsername();
                    if (newPos.challenging != null) challenging = mojang.getPlayerProfile(newPos.challenging.toString()).getUsername();

                    discordUser = "N/A";

                    try {
                        long discordId = AccountLink.getDiscordId(newPos.player);
                        net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordId).complete();
                        if (user != null) discordUser = user.getAsMention();
                    } catch (IllegalArgumentException e) {
                        // No user
                    }

                    changes.append("\n\n**AFTER**")
                            .append("Player: ").append(username)
                            .append("\nUUID: ").append(uuid)
                            .append("\nUser: ").append(discordUser)
                            .append("\nOC Name: ").append(ocName)
                            .append("\nLast Online: ").append(lastOnline)
                            .append("\nLast Challenge: ").append(lastChallenge)
                            .append("\nDate Joined Board: ").append(joinedBoard)
                            .append("\nDate Joined Position: ").append(joinedPos)
                            .append("\nChallenger: ").append(challenger)
                            .append("\nChallenging: ").append(challenging);

                    builder.addField(
                            validPositions[i].replace("_","").toUpperCase(),
                            changes.toString(),
                            false
                    );
                }

            }

            if (builder.getFields().isEmpty()) builder.addField("Something went wrong!","That's odd... It looks like there aren't any changes.", false);

            TextChannel logChannel = Bot.getJda().getTextChannelById(channelID);

            if (logChannel != null) logChannel.sendMessageEmbeds(builder.build()).queue();
        });

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

        Map<Integer, BoardState> newBoard = royaltyBoard;

        // joined_time access
        LocalDateTime last_online;
        // Count the number of empty positions
        int positionsEmpty;

        // For each tribe
        for (int tribe = 0; tribe < tribes.length; tribe++) {

            positionsEmpty = 0;

            // For each position
            for (int pos = 0; pos < validPositions.length; pos++) {

                // Set current path
                // currentPath = tribes[tribe] + "." + validPositions[pos];

                UUID uuid = getUuid(tribe, pos);

                if (uuid != null) {
                    // Set name from OpenRP character
                    if (EyeOfOnyx.openrp != null) {
                        String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                        if (ocName != null && !ocName.equals("No name set")) {
                            newBoard.get(tribe).updatePosition(pos, newBoard.get(tribe).getPos(pos).setName(ocName));

                        }
                    }
                }

                // If last_online is before inactivity period, clear position
                last_online = getLastOnline(tribe, pos);
                if (last_online != null && last_online.isBefore(LocalDateTime.now().minusDays(30))) {
                    newBoard.get(tribe).setLastOnline(tribe, null);
                }

                // If last_online is empty, clear position
                if (last_online == null) {

                    // This position is empty, so count up positionsEmpty
                    positionsEmpty += 1;

                    if (uuid != null) {

                        // Update LuckPerms
                        try {
                            int playerTribe = PlayerTribe.getTribeOfPlayer(uuid);
                            Utils.setPlayerPerms(uuid, playerTribe, pos);
                        } catch (NotFoundException e) {
                            // no tribe
                        }
                    }

                    // Clear data
                    newBoard.get(tribe).clear(pos);

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

                        newBoard.get(tribe).replace(pos, emptyPosition);

                        // Notify the user who has moved
                        if (newBoard.get(tribe).getPos(emptyPosition).player != null) {
                            new Notification(newBoard.get(tribe).getPos(emptyPosition).player, "You've been promoted!", "A player was removed from the royalty board and you moved into a higher position.", NotificationType.GENERIC).create();
                        }

                        // This position is now empty
                        // and another user will move up on the next iteration
                        // if there is an active user below this position
                    }
                }
            }

            if (newBoard.get(tribe) != royaltyBoard.get(tribe)) sendUpdate(new RoyaltyAction(null, tribe, royaltyBoard.get(tribe), newBoard.get(tribe)));

        }

        royaltyBoard = newBoard;
        save();
        updatePermissions();

    }

    public static void updateDiscordBoard(int tribeIndex) throws IOException {
        // Get channel and message
        List<Long> messageIDs = plugin.getConfig().getLongList("royalty-board-message");

        if (boardChannel == null) {
            Bukkit.getLogger().warning("Could not get royalty board channel!");
        } else {

            // Build message

            if (messageIDs.size() != 10) messageIDs = new ArrayList<>(10);
            // Create message list

            // Get base message from messageformat.txt
            String message = MessageFormat.get();

            // Replace $EMBLEM with appropriate tribe emoji
            if (!plugin.getConfig().getStringList("tribe-emblems").isEmpty()) {
                message = message.replace("$EMBLEM", plugin.getConfig().getStringList("tribe-emblems").get(tribeIndex));
            }

            // Replace $TRIBE with appropriate tribe name
            message = message.replace("$TRIBE", RoyaltyBoard.tribes[tribeIndex].substring(0, 1).toUpperCase() + RoyaltyBoard.tribes[tribeIndex].substring(1));

            // for each position...
            String[] positions = RoyaltyBoard.getValidPositions();
            for (int j = 0; j < positions.length; j++) {
                final String position = positions[j];

                String name = "N/A";
                String username = "N/A";
                String joined = "";

                Dreamvisitor.debug("Getting info for tribe " + tribeIndex + " position " + j);

                // Get info if not empty
                if (RoyaltyBoard.getUuid(tribeIndex, j) != null) {
                    Dreamvisitor.debug("Player here");

                    username = mojang.getPlayerProfile(RoyaltyBoard.getUuid(tribeIndex, j).toString()).getUsername();

                    name = ChatColor.stripColor(RoyaltyBoard.getOcName(tribeIndex, j));
                    if (name == null || name.equals("&c<No name set>")) name = username;

                    joined = "<t:" + LocalDateTime.parse(RoyaltyBoard.getJoinedDate(tribeIndex, j).toString()).toEpochSecond(ZoneOffset.UTC) + ":d>";
                }

                // Replace in message
                message = message.replace("$" + position.toUpperCase() + "-NAME", name)
                        .replace("$" + position.toUpperCase() + "-USERNAME", username)
                        .replace("$" + position.toUpperCase() + "-JOINED", joined);

                // Update Discord roles
                if (RoyaltyBoard.getUuid(tribeIndex, j) != null && !Dreamvisitor.botFailed) {

                    // Get recorded user ID
                    try {
                        long userId = AccountLink.getDiscordId(getUuid(tribeIndex, j));
                        // Get sister guild
                        Guild sisterGuild = Bot.getJda().getGuildById(Dreamvisitor.getPlugin().getConfig().getLong("tribeGuildID"));
                        if (sisterGuild != null) {
                            // Get role
                            Role tribeRole = sisterGuild.getRoleById(plugin.getConfig().getLongList("sister-royalty-roles").get(tribeIndex));
                            if (tribeRole != null) {
                                // Get user and add role
                                sisterGuild.retrieveMemberById(userId).queue(user -> sisterGuild.addRoleToMember(user, tribeRole).queue(), new ErrorHandler().handle(ErrorResponse.UNKNOWN_MEMBER, (error) -> Dreamvisitor.debug("Could not get member of ID " + userId + " in sister server.")));
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
                            mainGuild.retrieveMemberById(userId).queue(user -> mainGuild.addRoleToMember(user, finalRoyaltyRole).queue(), new ErrorHandler().handle(ErrorResponse.UNKNOWN_MEMBER, (error) -> Dreamvisitor.debug("Could not get member of ID " + userId + " in main server.")));
                        }
                    } catch (NullPointerException e) {
                        Bukkit.getLogger().warning(username + " does not have an associated Discord ID!");
                    }
                }
            }

            if (messageIDs.isEmpty() || messageIDs.size() < 10) {
                Bukkit.getLogger().warning("Some royalty board messages are not recorded. Use /eyeofonyx senddiscord to resend the Discord board");
            } else {

                // Try to edit message
                // Get message
                final String finalMessage = message;
                final Long targetMessageId = messageIDs.get(tribeIndex);
                try {
                    boardChannel.retrieveMessageById(targetMessageId).queue((targetMessage) -> targetMessage.editMessage(finalMessage).queue(), new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (error) -> Bukkit.getLogger().warning("Unknwon Message! Couldn't get message " + targetMessageId)));

                } catch (InsufficientPermissionException e) {
                    Bukkit.getLogger().warning("Dreamvisitor Bot does not have permission to get the royalty board message!");
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
                    if (getUuid(t, p) != null) {
                        // Get user at tribe t and position p
                        CompletableFuture<User> userFuture = userManager.loadUser(getUuid(t, p));

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

    public static boolean isPositionEmpty(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).player == null;
    }

    /**
     * Get the position index of a player UUID.
     *
     * @param player The player UUID to search from.
     * @return The index of the position that the player holds.
     */
    public static int getPositionIndexOfUUID(UUID player) throws NotFoundException {

        // Get player tribe
        int playerTribe = PlayerTribe.getTribeOfPlayer(player);

        // Position is 5 by default (citizen)
        int playerPosition = CIVILIAN;

        // Iterate though positions to search for target player
        for (int i = 0; i < validPositions.length; i++) {
            if (RoyaltyBoard.getUuid(playerTribe, i).equals(player)) {
                // Change position if found on the royalty board
                playerPosition = i;
                break;
            }
        }

        return playerPosition;
    }

    /**
     * Get the position index of a player UUID.
     *
     * @param tribe The tribe to search for. This will skip the call to {@link PlayerTribe}.
     * @param player The player UUID to search from.
     * @return The index of the position that the player holds.
     */
    public static int getPositionIndexOfUUID(int tribe, UUID player) {

        // Position is 5 by default (citizen)
        int playerPosition = CIVILIAN;

        // Iterate though positions to search for target player
        for (int i = 0; i < validPositions.length; i++) {
            if (RoyaltyBoard.getUuid(tribe, i).equals(player)) {
                // Change position if found on the royalty board
                playerPosition = i;
                break;
            }
        }

        return playerPosition;
    }

    /**
     * Get the UUID of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @return The String UUID located at the given location.
     */
    public static java.util.UUID getUuid(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).player;
    }

    /**
     * Get the character name of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the character name located at the given location.
     */
    public static String getOcName(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).name;
    }

    /**
     * Get the date joined position of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the date joined located at the given location.
     */
    public static LocalDateTime getJoinedDate(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).joinedPosition;
    }

    /**
     * Get the date last online of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return The date joined last online at the given location.
     */
    public static LocalDateTime getLastOnline(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).lastOnline;
    }

    /**
     * Get the date last challenged of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @return The date last challenged at the given location.
     */
    public static LocalDateTime getLastChallengeDate(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).lastChallenge;
    }

    /**
     * Get the attacker of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @return The attacker at the given location.
     */
    public static UUID getAttacker(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).challenger;
    }

    /**
     * Set the attacker of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @param uuid          The UUID to set as the attacker.
     */
    public static void setAttacker(int tribe, int pos, UUID uuid) {
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).setChallenger(pos, uuid));
    }

    /**
     * Get the target of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     */
    public static UUID getAttacking(int tribe, int pos) {
        return royaltyBoard.get(tribe).getPos(pos).challenging;
    }

    /**
     * Set the target of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @param uuid          The UUID to set.
     */
    public static void setAttacking(int tribe, int pos, UUID uuid) {
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).setChallenging(pos, uuid));
    }

    /**
     * Get whether a position is on cool down or not.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     */
    public static boolean isOnCoolDown(int tribe, int pos) {
        LocalDateTime lastChallenge = getLastChallengeDate(tribe, pos);
        return lastChallenge.isAfter(LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down")));
    }

    private RoyaltyBoard() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Moves a player from a given position to another given position.
     * If a player is in the position to be moved to, they will be removed.
     *
     * @param tribe
     * @param fromPos
     * @param toPos
     */
    public static void replace(int tribe, int fromPos, int toPos) {
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).replace(fromPos, toPos));
    }

    public static void removePlayer(int tribe, int pos) {
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).clear(pos));
    }

    public static void set(int tribe, int pos, BoardPosition newPosition) {
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).updatePosition(pos, newPosition));
    }
}
