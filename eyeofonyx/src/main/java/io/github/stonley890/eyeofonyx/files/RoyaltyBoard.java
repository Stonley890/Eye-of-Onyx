package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.Utils;
import javassist.NotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RoyaltyBoard {

    // Stored values
    public static final int RULER = 0;
    public static final int HEIR1 = 1;
    public static final int HEIR2 = 2;
    public static final int HEIR3 = 3;
    public static final int NOBLE1 = 4;
    public static final int NOBLE2 = 5;

    // private static List<RoyaltyAction> recordedActions;
    public static final int NOBLE3 = 6;
    public static final int NOBLE4 = 7;
    public static final int NOBLE5 = 8;
    public static final int CIVILIAN = 9;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();
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
            "ruler", "crown_heir", "apparent_heir", "presumptive_heir", "crown_noble", "grand_noble", "high_noble", "apparent_noble", "presumptive_noble"
    };
    private static File file;
    /**
     * Whether the royalty board is frozen
     */
    private static boolean frozen;
    private RoyaltyBoard() {
        throw new IllegalStateException("Utility class");
    }

    public static BoardState getBoardOf(int tribe) {
        return getBoard().get(tribe);
    }

    public static String[] getTeamNames() {
        return teamNames;
    }

    public static String[] getTribes() {
        return tribes;
    }

    public static String[] getValidPositions() {
        return validPositions;
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static void setFrozen(boolean value) {
        frozen = value;
        plugin.getConfig().set("frozen", value);
        plugin.saveConfig();
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

        getBoard();

    }

    private static TextChannel getBoardChannel() {
        long channelID = plugin.getConfig().getLong("royalty-board-channel");
        JDA jda = Bot.getJda();
        return jda.getTextChannelById(channelID);
    }

    private static void saveFile(FileConfiguration board) {
        try {
            board.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Eye of Onyx could not save board.yml! If this persists after a restart, report this error!");
        }
    }

    private static void saveToDisk(Map<Integer, BoardState> board) {
        saveFile(BoardState.createYamlConfiguration(board));
        Dreamvisitor.debug("[saveToDisk] Saved royalty board to disk.");
    }

    /**
     * Reload the file from disk.
     */
    public static @NotNull Map<Integer, BoardState> getBoard() {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        frozen = plugin.getConfig().getBoolean("frozen");
        return BoardState.fromYamlConfig(configuration);
    }

    /**
     * Send a board change report to the royalty log in Discord.
     * @param action the {@link RoyaltyAction} to report.
     */
    public static void reportChange(RoyaltyAction action) {

        // Run async
        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {

            // Build embed
            EmbedBuilder builder = new EmbedBuilder();

            long channelID = plugin.getConfig().getLong("royalty-log-channel");

            // Null executor means it was automatic
            if (action.executor == null) action.executor = "Eye of Onyx";

            // Emblem in message title
            String emblem = "";
            List<String> emblems = EyeOfOnyx.getPlugin().getConfig().getStringList("tribe-emblems");
            if (emblems.size() == tribes.length) emblem = emblems.get(action.affectedTribe) + " ";

            builder.setTitle(emblem + "Changes have been made to the " + teamNames[action.affectedTribe] + " board.");

            try {
                long discordID = Long.parseLong(action.executor);

                net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordID).complete();
                builder.setFooter("This action was performed by " + user.getName(), user.getAvatarUrl());

            } catch (NumberFormatException e) {
                builder.setFooter("This action was performed by " + action.executor);
            }

            for (int i = 0; i < validPositions.length; i++) {

                BoardPosition oldPos = action.oldState.getPos(i);
                BoardPosition newPos = action.newState.getPos(i);

                if (!oldPos.equals(newPos)) {
                    builder.addField(
                            validPositions[i].replace("_"," ").toUpperCase(),
                            "**Before**" + writeChanges(oldPos) + "\n\n**After**" + writeChanges(newPos),
                            false
                    );
                }
            }

            // If no fields have been written, something has gone wrong because there were no changes by this action
            if (builder.getFields().isEmpty()) builder.addField("Something went wrong!","That's odd... It looks like someone filed a report without any changes.", false);

            TextChannel logChannel = Bot.getJda().getTextChannelById(channelID);
            Button danger = Button.danger("revertaction-" + action.id, "Revert");

            if (logChannel != null) logChannel.sendMessageEmbeds(builder.build()).setActionRow(danger).queue();
        });
    }

    public static void report(@Nullable String executor, String content) {

        // Run async
        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {

            // Build embed
            EmbedBuilder builder = new EmbedBuilder();

            long channelID = plugin.getConfig().getLong("royalty-log-channel");

            // Null executor means it was automatic
            String recordedExecutor = "Eye of Onyx";
            if (executor != null) recordedExecutor = executor;

            try {
                long discordID = Long.parseLong(recordedExecutor);

                net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordID).complete();
                builder.setFooter("This action was performed by " + user.getName(), user.getAvatarUrl());

            } catch (NumberFormatException e) {
                builder.setFooter("This action was performed by " + recordedExecutor);
            }

            builder.setDescription(content);

            TextChannel logChannel = Bot.getJda().getTextChannelById(channelID);

            if (logChannel != null) logChannel.sendMessageEmbeds(builder.build()).queue();

        });

    }

    private static @NotNull StringBuilder writeChanges(@NotNull BoardPosition position) {
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

        String discordUser = "N/A";

        if (position.player != null) {
            uuid = position.player.toString();
            try {
                long discordId = AccountLink.getDiscordId(position.player);
                net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordId).complete();
                if (user != null) discordUser = user.getAsMention();
            } catch (NullPointerException IllegalArgumentException) {
                // No user
            }
        }
        if (position.player != null) username = PlayerUtility.getUsernameOfUuid(uuid);
        if (position.name != null) ocName = position.name;
        if (position.lastOnline != null) lastOnline = position.lastOnline.toString();
        if (position.lastChallenge != null) lastChallenge = position.lastChallenge.toString();
        if (position.joinedBoard != null) joinedBoard = position.joinedBoard.toString();
        if (position.joinedPosition != null) joinedPos = position.joinedPosition.toString();
        if (position.challenger != null) challenger = PlayerUtility.getUsernameOfUuid(position.challenger);
        if (position.challenging != null) challenging = PlayerUtility.getUsernameOfUuid(position.challenging);

        assert username != null;
        assert challenger != null;
        assert challenging != null;
        changes.append("\nPlayer: ").append(Bot.escapeMarkdownFormatting(username))
                .append("\nUUID: ").append(uuid)
                .append("\nUser: ").append(discordUser)
                .append("\nOC Name: ").append(Bot.escapeMarkdownFormatting(ocName))
                .append("\nLast Online: ").append(lastOnline)
                .append("\nLast Challenge: ").append(lastChallenge)
                .append("\nDate Joined Board: ").append(joinedBoard)
                .append("\nDate Joined Position: ").append(joinedPos)
                .append("\nChallenger: ").append(Bot.escapeMarkdownFormatting(challenger))
                .append("\nChallenging: ").append(Bot.escapeMarkdownFormatting(challenging));

        return changes;
    }

    /**
     * Search for and remove inactive players as well as move players up into empty ranks if they exist.
     * This will also set the name on board.yml based on the character name stored from OpenRP (if it exists).
     * The board will be saved to disk after updating.
     * @param tribe the tribe whose board to update.
     * @param updateDiscord whether to update the Discord board after changes have been made.
     *                      This should be disabled
     *                      if an action has occurred before this
     *                      to reduce the chance that Discord will be updated twice or not at all.
     */
    public static void updateBoard(int tribe, boolean updateDiscord) {

        Dreamvisitor.debug("[updateBoard] Updating royalty board.");

        Map<Integer, BoardState> royaltyBoard = getBoard();

        // joined_time access
        LocalDateTime last_online;
        // Count the number of empty positions
        int positionsEmpty;

        BoardState oldPos = getBoardOf(tribe).clone();

        positionsEmpty = 0;

        // For each position
        for (int pos = 0; pos < validPositions.length; pos++) {

            UUID uuid = getUuid(tribe, pos);

            updateOCName(tribe, pos);

            // If last_online is before inactivity period, clear position
            last_online = getLastOnline(tribe, pos);
            if (last_online != null && last_online.isBefore(LocalDateTime.now().minusDays(plugin.getConfig().getInt("inactivity-timer")))) {
                RoyaltyBoard.set(tribe, pos, royaltyBoard.get(tribe).getPos(positionsEmpty).setLastOnline(null));
                royaltyBoard = getBoard();
            }

            // If last_online is empty, clear position
            if (last_online == null) {
                // This position is empty, so count up positionsEmpty
                Dreamvisitor.debug("[updateBoard] Tribe " + tribe + " pos " + pos + " (" + uuid + ") is empty");
                positionsEmpty += 1;

                if (uuid != null) {
                    // Update LuckPerms
                    try {
                        int playerTribe = PlayerTribe.getTribeOfPlayer(uuid);
                        Utils.setPlayerPerms(uuid, playerTribe, pos);
                    } catch (NotFoundException e) {
                        // no tribe
                    }

                    // Remove challenges
                    Challenge.removeChallengesOfPlayer(uuid, "The player you were challenging was removed from the royalty board for inactivity.");

                    // Remove notifications
                    Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_REQUESTED);
                }

                // Clear data
                RoyaltyBoard.removePlayer(tribe, pos, true);
                royaltyBoard = getBoard();

            } // If position is held by an active player
            else {
                // If any previous position was empty, move this user up that many positions
                // positionsEmpty is initialized as 0 so this cannot run as ruler
                if (positionsEmpty > 0) {

                    Dreamvisitor.debug("[updateBoard] Positions empty: " + positionsEmpty);
                    Dreamvisitor.debug("[updateBoard] Current position: " + pos);

                    int emptyPosition = pos - positionsEmpty;

                    RoyaltyBoard.replace(tribe, pos, emptyPosition);
                    royaltyBoard = getBoard();

                    // Notify the user who has moved
                    java.util.UUID movedUser = royaltyBoard.get(tribe).getPos(emptyPosition).player;
                    if (movedUser != null) {
                        Dreamvisitor.debug("[updateBoard] Moved user " + movedUser);
                        new Notification(movedUser, "You've been promoted!", "A player was removed from the royalty board and you moved into a higher position. Because of this, any challenges have been canceled.", Notification.Type.GENERIC).create();

                        // Remove challenges
                        Challenge.removeChallengesOfPlayer(movedUser, null);

                        // Remove notifications
                        Notification.removeNotificationsOfPlayer(movedUser, Notification.Type.CHALLENGE_REQUESTED);
                    }

                    // This position is now empty
                    // and another user will move up on the next iteration
                    // if there is an active user below this position
                }
            }
        }

        if (updateDiscord && !oldPos.equals(royaltyBoard.get(tribe))) {
            try {
                updateDiscordBoard(tribe);
            } catch (IOException e) {
                Bukkit.getLogger().warning("Eye of Onyx was unable to update the Discord board!");
            }
            reportChange(new RoyaltyAction(null, tribe, oldPos, royaltyBoard.get(tribe)));
        }

        saveToDisk(royaltyBoard);
        updatePermissions(tribe);
    }

    /**
     * Update the OC name of the given tribe and position by their OpenRP character.
     * @param tribe tribe index.
     * @param pos position index.
     */
    public static void updateOCName(int tribe, int pos) {
        UUID uuid = getUuid(tribe, pos);
        Map<Integer, BoardState> royaltyBoard = getBoard();

        if (uuid != null) {
            // Set name from OpenRP character
            if (EyeOfOnyx.openrp != null) {
                String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                if (ocName != null && !ocName.equals("No name set")) {
                    RoyaltyBoard.set(tribe, pos, royaltyBoard.get(tribe).getPos(pos).setName(ocName));
                }
            }
        }

        saveToDisk(royaltyBoard);
    }

    /**
     * Updates the Discord royalty boards
     * @param tribeIndex the tribe whose board to update
     * @throws IOException if messageformat.txt cannot be retrieved
     */
    public static void updateDiscordBoard(int tribeIndex) throws IOException {

        Dreamvisitor.debug("[updateDiscordBoard] Updating Discord board for " + tribeIndex);

        // Get channel and message
        List<Long> messageIDs = plugin.getConfig().getLongList("royalty-board-message");

        TextChannel boardChannel = getBoardChannel();

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

                Dreamvisitor.debug("[updateDiscordBoard] Getting info for tribe " + tribeIndex + " position " + j);

                // Get info if not empty
                java.util.UUID uuid = RoyaltyBoard.getUuid(tribeIndex, j);
                if (uuid != null) {
                    Dreamvisitor.debug("[updateDiscordBoard] Player " + uuid + " found.");

                    username = PlayerUtility.getUsernameOfUuid(uuid);

                    name = ChatColor.stripColor(RoyaltyBoard.getOcName(tribeIndex, j));
                    if (name == null || name.equals("&c<No name set>")) name = username;

                    LocalDateTime joinedPosDate = RoyaltyBoard.getJoinedPosDate(tribeIndex, j);
                    if (joinedPosDate == null) joined = "(No join time)";
                    else joined = "<t:" + joinedPosDate.toEpochSecond(ZoneOffset.UTC) + ":d>";
                }

                // Replace in message
                assert name != null;
                assert username != null;
                message = message.replace("$" + position.toUpperCase() + "-NAME", name)
                        .replace("$" + position.toUpperCase() + "-USERNAME", username)
                        .replace("$" + position.toUpperCase() + "-JOINED", joined);
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
                    Dreamvisitor.debug("[updateDiscordBoard] Message edit queued.");
                } catch (InsufficientPermissionException e) {
                    Bukkit.getLogger().warning("Main Bot does not have permission to get the royalty board message! " + e.getMessage());
                }
            }

            // Update Discord roles
            Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {
                removeRoles(tribeIndex);
                applyRoles(tribeIndex);
            });

        }

    }

    /**
     * Updates roles of this member if required.
     * This will check which roles the user should have and remove all others.
     * @param user the {@link net.dv8tion.jda.api.entities.User} to update.
     */
    public static void updateRoles(@NotNull net.dv8tion.jda.api.entities.User user) {

        java.util.UUID uuid = AccountLink.getUuid(user.getIdLong());
        int tribe = -1;
        int pos = CIVILIAN;

        // Get tribe and position if possible.
        // If not, that is ok.
        // All roles will just be removed.
        if (uuid != null) {
            try {
                tribe = PlayerTribe.getTribeOfPlayer(uuid);
                pos = getPositionIndexOfUUID(tribe, uuid);
            } catch (NotFoundException ignored) {}
        }

        Dreamvisitor.debug("[updateRoles] Updating roles of UUID " + uuid + ". Tribe " + tribe + ", pos " + pos);

        FileConfiguration config = EyeOfOnyx.getPlugin().getConfig();

        Dreamvisitor.debug("[updateRoles] Getting guilds and roles...");

        // Get guilds
        Guild mainGuild = Bot.getGameLogChannel().getGuild();
        Guild sisterGuild = Bot.getJda().getGuildById(config.getLong("tribeGuildID"));

        // Get roles
        Role rulerRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.ruler"));
        Role heirRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.heir"));
        Role nobleRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.noble"));

        Dreamvisitor.debug("[updateRoles] Getting member...");
        int finalPos = pos;
        mainGuild.retrieveMember(user).queue(member -> {
            Dreamvisitor.debug("[updateRoles] Got member. Null? " + (member == null));

            if (member != null) {
                // Remove roles if applicable
                Dreamvisitor.debug("[updateRoles] Removing main guild roles if needed.");
                Dreamvisitor.debug("[updateRoles] Is pos RULER? " + (finalPos != RULER));
                Dreamvisitor.debug("[updateRoles] Is ruler role null? " + (rulerRole != null));
                Dreamvisitor.debug("[updateRoles] Does roles contain ruler? " + (member.getRoles().contains(rulerRole)));
                if (finalPos != RULER && rulerRole != null && member.getRoles().contains(rulerRole)) mainGuild.removeRoleFromMember(member, rulerRole).queue();
                if (finalPos != HEIR1 && finalPos != HEIR2 && finalPos != HEIR3 && heirRole != null && member.getRoles().contains(heirRole)) mainGuild.removeRoleFromMember(member, heirRole).queue();
                if (finalPos != NOBLE1 && finalPos != NOBLE2 && finalPos != NOBLE3 && finalPos != NOBLE4 && finalPos != NOBLE5 && nobleRole != null && member.getRoles().contains(nobleRole)) mainGuild.removeRoleFromMember(member, nobleRole).queue();

                // Add roles if applicable
                Dreamvisitor.debug("[updateRoles] Adding main guild roles if needed.");
                if (finalPos == RULER && rulerRole != null && !member.getRoles().contains(rulerRole)) mainGuild.addRoleToMember(member, rulerRole).queue();
                if ((finalPos == HEIR1 || finalPos == HEIR2 || finalPos == HEIR3) && heirRole != null && !member.getRoles().contains(heirRole)) mainGuild.addRoleToMember(member, heirRole).queue();
                if ((finalPos == NOBLE1 || finalPos == NOBLE2 || finalPos == NOBLE3 || finalPos == NOBLE4 || finalPos == NOBLE5) && nobleRole != null && !member.getRoles().contains(nobleRole)) mainGuild.addRoleToMember(member, nobleRole).queue();
            }

        });

        if (sisterGuild == null) {
            Bukkit.getLogger().warning("Sister guild could not be found! Make sure the ID in the Eye of Onyx config is correct and restart!");
            return;
        }

        int finalPos1 = pos;
        int finalTribe = tribe;
        sisterGuild.retrieveMember(user).queue(member -> {
            if (member != null) {
                // get sister guild roles
                List<Role> sisterRoyaltyRoles = new ArrayList<>();
                List<Long> sisterRoyaltyRoleIDs = config.getLongList("sister-royalty-roles");
                if (sisterRoyaltyRoleIDs.size() < tribes.length) {
                    Bukkit.getLogger().warning("Missing sister guild royalty role IDs! Check Eye of Onyx configuration and restart.");
                    return;
                }
                for (Long roleID : sisterRoyaltyRoleIDs) {
                    sisterRoyaltyRoles.add(Bot.getJda().getRoleById(roleID));
                }

                // For each position, remove royalty role if they are citizen or are not part of that tribe.
                // Add role if applicable
                for (int tribeRole = 0; tribeRole < tribes.length; tribeRole++) {
                    Role role = sisterRoyaltyRoles.get(tribeRole);
                    if (finalPos1 == CIVILIAN || finalTribe != tribeRole && member.getRoles().contains(role)) sisterGuild.removeRoleFromMember(member, role).queue();
                    if (finalPos1 != CIVILIAN && finalTribe == tribeRole && !member.getRoles().contains(role)) sisterGuild.addRoleToMember(member, role).queue();
                }
            }
        });

    }

    /**
     * Removes all royalty-associated Discord roles from tribe royalty of main and sister servers.
     * applyRoles() should be used after this to reapply the roles.
     * This does not apply to the sister guild due to how the roles are set up.
     * @param tribe the tribe whose roles to remove
     */
    public static void removeRoles(int tribe) {

        Dreamvisitor.debug("[removeRoles] Removing roles for tribe " + tribe);
        FileConfiguration config = EyeOfOnyx.getPlugin().getConfig();

        // Get guilds
        Guild mainGuild = Bot.getGameLogChannel().getGuild();

        // Get roles
        Role rulerRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.ruler"));
        Role heirRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.heir"));
        Role nobleRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.noble"));

        Dreamvisitor.debug("[removeRoles] Got roles.");

        // Remove roles
        for (int pos = 0; pos < validPositions.length; pos++) {
            Dreamvisitor.debug("[removeRoles] Checking pos " + pos);
            java.util.UUID uuid = getBoardOf(tribe).getPos(pos).player;

            Dreamvisitor.debug("[removeRoles] UUID is " + uuid);

            if (uuid == null) continue;

            long discordId;

            try {
                discordId = AccountLink.getDiscordId(uuid);
            } catch (NullPointerException e) {
                Bukkit.getLogger().warning("Player with UUID " + uuid + " is on the royalty board but does not have a linked Discord account!");
                continue;
            }

            Member member = mainGuild.retrieveMemberById(discordId).complete();

            if (member == null) {
                Bukkit.getLogger().warning("Player with UUID " + uuid + " and Discord ID " + discordId + " is on the royalty board and has a linked Discord account, but is not in the guild!");
            } else {
                // Remove roles if applicable
                if (pos != RULER && rulerRole != null && member.getRoles().contains(rulerRole)) mainGuild.removeRoleFromMember(member, rulerRole).complete();
                if (pos != HEIR1 && pos != HEIR2 && pos != HEIR3 && heirRole != null && member.getRoles().contains(heirRole)) mainGuild.removeRoleFromMember(member, heirRole).complete();
                if (pos != NOBLE1 && pos != NOBLE2 && pos != NOBLE3 && pos != NOBLE4 && pos != NOBLE5 && nobleRole != null && member.getRoles().contains(nobleRole)) mainGuild.removeRoleFromMember(member, nobleRole).complete();
                Dreamvisitor.debug("[removeRoles] Roles removed.");
            }

        }

        Dreamvisitor.debug("[removeRoles] Finished removing roles.");
    }

    /**
     * Adds appropriate royalty roles to members of the royalty board for a specified tribe.
     * This does not remove roles from non-royalty members. removeRoles() should be used to do that.
     * @param tribe the tribe to apply roles to.
     */
    public static void applyRoles(int tribe) {

        Dreamvisitor.debug("[applyRoles] Applying roles for tribe " + tribe);
        FileConfiguration config = EyeOfOnyx.getPlugin().getConfig();

        // Get guild
        Guild mainGuild = Bot.getGameLogChannel().getGuild();

        // Get roles
        Role rulerRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.ruler"));
        Role heirRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.heir"));
        Role nobleRole = Bot.getJda().getRoleById(config.getLong("main-royalty-roles.noble"));

        Dreamvisitor.debug("[applyRoles] Got roles.");

        // Check each position
        for (int pos = 0; pos < validPositions.length; pos++) {

            Dreamvisitor.debug("[applyRoles] Checking tribe " + tribe + " position " + pos);

            // Skip if empty.
            // Not breaking from loop because up-to-date positions cannot be guaranteed,
            // and it's not that much extra computation.
            if (isPositionEmpty(tribe, pos)) continue;

            Dreamvisitor.debug("[applyRoles] Not empty");

            // Get recorded user ID
            try {
                Dreamvisitor.debug("[applyRoles] Getting ID");
                long userId = AccountLink.getDiscordId(Objects.requireNonNull(getUuid(tribe, pos)));
                Member member;

                // Get appropriate role
                Role royaltyRole = null;
                if (pos == RULER) royaltyRole = rulerRole;
                if (pos >= HEIR1) royaltyRole = heirRole;
                if (pos >= NOBLE1) royaltyRole = nobleRole;
                // Add to user
                member = mainGuild.retrieveMemberById(userId).complete();
                if (royaltyRole != null) mainGuild.addRoleToMember(member, royaltyRole).complete();

                Dreamvisitor.debug("[applyRoles] Added main role");

            } catch (NullPointerException e) {

                String username = PlayerUtility.getUsernameOfUuid(Objects.requireNonNull(getUuid(tribe, pos)));

                Bukkit.getLogger().warning(username + " is on the royalty board but does not have an associated Discord ID!");
            }
        }
    }

    public static void updatePermissions(@NotNull UUID uuid) {
        if (EyeOfOnyx.luckperms != null) {

            Dreamvisitor.debug("[updatePermissions] Updating permissions for user " + uuid);

            // Get user manager
            UserManager userManager = EyeOfOnyx.luckperms.getUserManager();
            // Get user at tribe t and position p
            CompletableFuture<User> userFuture = userManager.loadUser(uuid);

            int playerTribe = -1;
            int playerPos = CIVILIAN;

            try {
                playerTribe = PlayerTribe.getTribeOfPlayer(uuid);
                playerPos = getPositionIndexOfUUID(playerTribe, uuid);
            } catch (NotFoundException ignored) {}

            Dreamvisitor.debug("[updatePermissions] Updating permissions of UUID " + uuid + " of tribe " + playerTribe + " pos " + playerPos);

            // Run async
            int finalPlayerPos = playerPos;
            int finalPlayerTribe = playerTribe;
            userFuture.thenAcceptAsync(user -> {

                String[] groupPositions = {"ruler", "heir", "noble", "citizen"};

                // For each tribe and position...
                for (String tribeName : tribes) {
                    for (String position : groupPositions) {
                        // ...get the lp group name from config
                        String groupName = plugin.getConfig().getString(position + "." + tribeName);

                        if (groupName != null) {
                            Dreamvisitor.debug("[updatePermissions] Removing group " + groupName + " from " + uuid);
                            // Get the group from lp and remove it from the user.
                            user.data().remove(Node.builder("group." + groupName).build());

                        } else
                            Bukkit.getLogger().warning("Group " + position + "." + tribeName + " is null in the config!");
                    }
                }

                // Now that all have been removed, add the correct one

                String group = null;
                if (finalPlayerPos == RULER) {
                    group = "ruler";
                } else if (finalPlayerPos < HEIR3) {
                    group = "heir";
                } else if (finalPlayerPos < NOBLE5) {
                    group = "noble";
                }

                if (group != null) {
                    String groupName = plugin.getConfig().getString(group + "." + tribes[finalPlayerTribe]);

                    if (groupName != null) {
                        Dreamvisitor.debug("[updatePermissions] Adding group " + groupName + " to " + uuid + " (pos " + finalPlayerPos + ")");
                        // Get the group from lp and add it to the user.
                        user.data().add(Node.builder("group." + groupName).build());
                    }
                }

                userManager.saveUser(user);
            });
        } else
            Bukkit.getLogger().warning("Eye of Onyx could not hook into LuckPerms on startup. Permission update failed.");
    }

    /**
     * Updates LuckPerms groups for all users on the specified royalty board. This will not work if LuckPerms was not initialized on enable.
     * This does not remove roles from non-royalty members. {@code removePermissions()} should be used to do that.
     */
    public static void updatePermissions(int tribe) {

        Dreamvisitor.debug("[updatePermissions] Updating LuckPerms permissions for tribe " + tribe);
        if (EyeOfOnyx.luckperms != null) {

            // Get user manager
            UserManager userManager = EyeOfOnyx.luckperms.getUserManager();

            // Go through each entry in the royalty board
            for (int p = 0; p < validPositions.length; p++) {

                // Only update if there is a player in the position
                java.util.UUID uuid = getUuid(tribe, p);
                if (uuid != null) {

                    Dreamvisitor.debug("[updatePermissions] Checking permissions of tribe " + tribe + " pos " + p + ", which is occupied by UUID " + uuid);

                    // Get user at tribe t and position p
                    CompletableFuture<User> userFuture = userManager.loadUser(uuid);

                    // Run async
                    int finalP = p;
                    userFuture.thenAcceptAsync(user -> {

                        String[] groupPositions = {"ruler", "heir", "noble", "citizen"};

                        // For each tribe and position...
                        for (String tribeName : tribes) {
                            for (String position : groupPositions) {

                                // ...get the lp group name from config
                                String groupName = plugin.getConfig().getString(position + "." + tribeName);

                                if (groupName != null) {
                                    // Get the group from lp and remove it from the user.
                                    user.data().remove(Node.builder("group." + groupName).build());

                                } else
                                    Bukkit.getLogger().warning("Group " + position + "." + tribeName + " is null in the config!");
                            }
                        }

                        // Now that all have been removed, add the correct one

                        String group;
                        if (finalP == RULER) {
                            group = "ruler";
                        } else if (finalP <= HEIR3) {
                            group = "heir";
                        } else {
                            group = "noble";
                        }

                        String groupName = plugin.getConfig().getString(group + "." + tribes[tribe]);

                        if (groupName != null) {
                            // Get the group from lp and add it to the user.
                            user.data().add(Node.builder("group." + groupName).build());
                        }

                        userManager.saveUser(user);
                    });
                }
            }
        } else
            Bukkit.getLogger().warning("Eye of Onyx could not hook into LuckPerms on startup. Permission update failed.");
    }

    public static boolean isPositionEmpty(int tribe, int pos) {
        return getBoard().get(tribe).getPos(pos).player == null;
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

        // Position is citizen by default
        int playerPosition = CIVILIAN;

        // Iterate though positions to search for target player
        for (int i = 0; i < validPositions.length; i++) {
            if (Objects.equals(RoyaltyBoard.getUuid(playerTribe, i), (player))) {
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

        // Position is citizen by default
        int playerPosition = CIVILIAN;

        // Iterate though positions to search for target player
        for (int i = 0; i < validPositions.length; i++) {
            if (Objects.equals(RoyaltyBoard.getUuid(tribe, i), player)) {
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
    public static @Nullable java.util.UUID getUuid(int tribe, int pos) {
        return getBoard().get(tribe).getPos(pos).player;
    }

    /**
     * Get the character name of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the character name located at the given location.
     */
    public static @Nullable String getOcName(int tribe, int pos) {
        return getBoard().get(tribe).getPos(pos).name;
    }

    public static void setOcName(int tribe, int pos, String name) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).setName(pos, name));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get the date joined position of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the date joined located at the given location.
     */
    public static @Nullable LocalDateTime getJoinedPosDate(int tribe, int pos) {
        return getBoard().get(tribe).getPos(pos).joinedPosition;
    }

    /**
     * Get the date joined board of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the date joined located at the given location.
     */
    public static @Nullable LocalDateTime getJoinedBoardDate(int tribe, int pos) {
        return getBoard().get(tribe).getPos(pos).joinedBoard;
    }

    /**
     * Get the date last online of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return The date joined last online at the given location.
     */
    public static @Nullable LocalDateTime getLastOnline(int tribe, int pos) {
        return getBoard().get(tribe).getPos(pos).lastOnline;
    }

    /**
     * Set the date last online of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @param time the date & time to set.
     */
    public static void setLastOnline(int tribe, int pos, LocalDateTime time) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).setLastOnline(pos, time));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get the date last challenged of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @return The date last challenged at the given location.
     */
    public static @Nullable LocalDateTime getLastChallengeDate(int tribe, int pos) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        return royaltyBoard.get(tribe).getPos(pos).lastChallenge;
    }

    /**
     * Set the last challenge date of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @param time the date & time to set.
     */
    public static void setLastChallengeDate(int tribe, int pos, LocalDateTime time) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).setLastChallenge(pos, time));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get the attacker of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @return The attacker at the given location.
     */
    public static @Nullable UUID getAttacker(int tribe, int pos) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
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
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).setChallenger(pos, uuid));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get the target of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     */
    public static @Nullable UUID getAttacking(int tribe, int pos) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
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
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).setChallenging(pos, uuid));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get whether a position is on cool down or not.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     */
    public static boolean isOnCoolDown(int tribe, int pos) {
        LocalDateTime lastChallenge = getLastChallengeDate(tribe, pos);
        if (lastChallenge != null) {
            return lastChallenge.isAfter(LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down")));
        } else return false;
    }

    public static @org.jetbrains.annotations.Nullable LocalDateTime getCooldownEnd(int tribe, int pos) {
        LocalDateTime lastChallenge = getLastChallengeDate(tribe, pos);
        if (lastChallenge != null) {
            return LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down"));
        } else return null;
    }

    /**
     * Moves a player from a given position to another given position.
     * If a player is in the position to be moved to, they will be removed.
     * @param tribe the tribe to target.
     * @param fromPos the first position to swap.
     * @param toPos the second position to swap.
     */
    public static void replace(int tribe, int fromPos, int toPos) {
        Dreamvisitor.debug("[replace] Replacing tribe " + tribe + " from pos " + fromPos + "(" + getUuid(tribe, fromPos) + ") to pos " + toPos + " (" + getUuid(tribe, toPos) + ")");
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).replace(fromPos, toPos));
        saveToDisk(royaltyBoard);
    }

    /**
     * Remove a player from the royalty board.
     * @param tribe the tribe to target.
     * @param pos the position to target.
     * @param complete if {@code true}, also updates this user's LuckPerms permissions and Discord roles.
     */
    public static void removePlayer(int tribe, int pos, boolean complete) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        Dreamvisitor.debug("[removePlayer] Removing player at tribe " + tribe + " pos " + pos + ". Complete? " + complete);
        java.util.UUID uuid = null;
        if (complete) uuid = royaltyBoard.get(tribe).getPos(pos).player;
        Dreamvisitor.debug("[removePlayer] UUID: " + uuid);
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).clear(pos));
        if (uuid != null) {
            Dreamvisitor.debug("[removePlayer] Updating permissions...");
            updatePermissions(uuid);
            try {
                long discordId = AccountLink.getDiscordId(uuid);
                Dreamvisitor.debug("[removePlayer] Player has a linked Discord account by ID " + discordId + ". Updating roles...");
                Bot.getJda().retrieveUserById(discordId).queue(RoyaltyBoard::updateRoles);
            } catch (NullPointerException ignored) {}
        }
        saveToDisk(royaltyBoard);
    }

    public static void set(int tribe, int pos, BoardPosition newPosition) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, royaltyBoard.get(tribe).updatePosition(pos, newPosition));
        saveToDisk(royaltyBoard);

    }

    public static void set(int tribe, BoardState newState) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(tribe, newState);
        saveToDisk(royaltyBoard);
    }
}
