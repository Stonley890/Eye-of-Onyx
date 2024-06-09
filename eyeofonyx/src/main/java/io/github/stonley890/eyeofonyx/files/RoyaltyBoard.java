package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.dreamvisitor.data.TribeUtil;
import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
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
import net.dv8tion.jda.api.utils.TimeFormat;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;

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

    public static BoardState getBoardOf(Tribe tribe) {
        return getBoard().get(TribeUtil.indexOf(tribe));
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

    @Nullable
    private static TextChannel getBoardChannel() {
        long channelID = plugin.getConfig().getLong("royalty-board-channel");
        JDA jda = Bot.getJda();
        return jda.getTextChannelById(channelID);
    }

    private static void saveFile(@NotNull FileConfiguration board) {
        try {
            board.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Eye of Onyx could not save board.yml! If this persists after a restart, report this error!");
        }
    }

    private static void saveToDisk(@NotNull Map<Integer, BoardState> board) {
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
    public static void reportChange(@NotNull RoyaltyAction action) {

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
            if (emblems.size() == TribeUtil.tribes.length) emblem = emblems.get(TribeUtil.indexOf(action.affectedTribe)) + " ";

            builder.setAuthor("Board Data Change", null, emblem);
            builder.setTitle("Changes have been made to the " + action.affectedTribe.getTeamName() + " board.");

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

    public static void report(@Nullable String executor, @Nullable String content) {

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

        assert username != null;
        changes.append("\nPlayer: ").append(Bot.escapeMarkdownFormatting(username))
                .append("\nUUID: ").append(uuid)
                .append("\nUser: ").append(discordUser)
                .append("\nOC Name: ").append(Bot.escapeMarkdownFormatting(ocName))
                .append("\nLast Online: ").append(lastOnline)
                .append("\nLast Challenge: ").append(lastChallenge)
                .append("\nDate Joined Board: ").append(joinedBoard)
                .append("\nDate Joined Position: ").append(joinedPos);

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
    public static void updateBoard(@NotNull Tribe tribe, boolean updateDiscord) {

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
                RoyaltyBoard.set(tribe, pos, royaltyBoard.get(TribeUtil.indexOf(tribe)).getPos(positionsEmpty).setLastOnline(null));
                royaltyBoard = getBoard();
            }

            // If last_online is empty, clear position
            if (last_online == null) {
                // This position is empty, so count up positionsEmpty
                Dreamvisitor.debug("[updateBoard] Tribe " + tribe + " pos " + pos + " (" + uuid + ") is empty");
                positionsEmpty += 1;

                if (uuid != null) {
                    // Update LuckPerms
                    RoyaltyBoard.updatePermissions(uuid);

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
                    java.util.UUID movedUser = royaltyBoard.get(TribeUtil.indexOf(tribe)).getPos(emptyPosition).player;
                    if (movedUser != null) {
                        Dreamvisitor.debug("[updateBoard] Moved user " + movedUser);
                        new Notification(movedUser, "You've been promoted!", "A player was removed from the royalty board and you moved into a higher position. Because of this, any challenges have been canceled.", Notification.Type.GENERIC).create();

                        // Remove challenges
                        Challenge.removeChallengesOfPlayer(movedUser, null);

                        // Remove notifications
                        Notification.removeNotificationsOfPlayer(movedUser, Notification.Type.CHALLENGE_REQUESTED);

                        RoyaltyBoard.updatePermissions(movedUser, tribe, emptyPosition);
                    }

                    // This position is now empty
                    // and another user will move up on the next iteration
                    // if there is an active user below this position
                }
            }
        }

        if (updateDiscord && !oldPos.equals(royaltyBoard.get(TribeUtil.indexOf(tribe)))) {
            updateDiscordBoard(tribe);
            reportChange(new RoyaltyAction(null, tribe, oldPos, royaltyBoard.get(TribeUtil.indexOf(tribe))));
        }

        saveToDisk(royaltyBoard);
    }

    /**
     * Update the OC name of the given tribe and position by their OpenRP character.
     * @param tribe tribe index.
     * @param pos position index.
     */
    public static void updateOCName(@NotNull Tribe tribe, int pos) {
        UUID uuid = getUuid(tribe, pos);
        Map<Integer, BoardState> royaltyBoard = getBoard();

        if (uuid != null) {
            // Set name from OpenRP character
            if (EyeOfOnyx.openrp != null) {
                String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                if (ocName != null && !ocName.equals("No name set")) {
                    RoyaltyBoard.set(tribe, pos, royaltyBoard.get(TribeUtil.indexOf(tribe)).getPos(pos).setName(ocName));
                }
            }
        }

        saveToDisk(royaltyBoard);
    }

    /**
     * Updates the Discord royalty boards. This runs async.
     * @param tribe the tribe whose board to update
     */
    public static void updateDiscordBoard(@NotNull Tribe tribe) {
        Dreamvisitor.debug("[updateDiscordBoard] Updating Discord board for " + tribe);
        plugin.reloadConfig();

        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {

            final Color[] tribeColors = {
                    new Color(209, 91, 46), // Hive
                    new Color(189, 220, 238), // Ice
                    new Color(0, 107, 14), // Leaf
                    new Color(151, 81, 9), // Mud
                    new Color(107, 14, 239), // Night
                    new Color(14, 240, 37), // Rain
                    new Color(240, 217, 14), // Sand
                    new Color(14, 57, 240), // Sea
                    new Color(232, 75, 202), // Silk
                    new Color(166, 41, 13) // Sky
            };
            final String rulerEmblem = "\uD83D\uDC51";
            final String heirEmblem = "\uD83D\uDC8D";
            final String nobleEmblem = "\uD83E\uDE99";

            // Get channel and message
            List<Long> messageIDs = plugin.getConfig().getLongList("royalty-board-message");

            TextChannel boardChannel = getBoardChannel();

            if (boardChannel == null) {
                Bukkit.getLogger().warning("Could not get royalty board channel!");
                return;
            }

            // Build message
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle(tribe.getTeamName() + " Kingdom");
            int tribeIndex = TribeUtil.indexOf(tribe);
            embed.setColor(tribeColors[tribeIndex]);
            embed.setTimestamp(Instant.now());

            if (messageIDs.size() != 10) messageIDs = new ArrayList<>(10);
            // Create message list

            // Replace $EMBLEM with appropriate tribe emoji
            if (!plugin.getConfig().getStringList("tribe-emblems").isEmpty()) {
                String url = plugin.getConfig().getStringList("tribe-emblems").get(tribeIndex);
                Dreamvisitor.debug("Emblem URL:" + url);
                try {
                    embed.setThumbnail(url);
                } catch (IllegalArgumentException ignored) {}
            }

            // for each position...
            String[] positions = RoyaltyBoard.getValidPositions();
            for (int j = 0; j < positions.length; j++) {
                final String position = positions[j];

                String name;
                String username;
                String joined;

                Dreamvisitor.debug("[updateDiscordBoard] Getting info for tribe " + tribe + " position " + j);

                String emblem;
                if (j == RULER) emblem = rulerEmblem;
                else if (j >= HEIR2) emblem = heirEmblem;
                else emblem = nobleEmblem;

                String value = "*None*";

                // Get info if not empty
                java.util.UUID uuid = RoyaltyBoard.getUuid(tribe, j);
                if (uuid != null) {
                    Dreamvisitor.debug("[updateDiscordBoard] Player " + uuid + " found.");

                    username = PlayerUtility.getUsernameOfUuid(uuid);

                    name = ChatColor.stripColor(RoyaltyBoard.getOcName(tribe, j));
                    if (name == null || name.equals("&c<No name set>")) name = username;

                    LocalDateTime joinedPosDate = RoyaltyBoard.getJoinedPosDate(tribe, j);
                    if (joinedPosDate == null) joined = "unknown";
                    else joined = Bot.createTimestamp(joinedPosDate, TimeFormat.DATE_SHORT).toString();

                    Member member = Bot.getGameLogChannel().getGuild().retrieveMemberById(AccountLink.getDiscordId(uuid)).complete();

                    value = "**" + name + "** since " + joined + "\n" + member.getAsMention() + " | `" + username + "`";
                }

                embed.addField(emblem + " " + position.replace("_", " ").toUpperCase(), value, false);
            }

            if (messageIDs.isEmpty() || messageIDs.size() < 10) {
                Bukkit.getLogger().warning("Some royalty board messages are not recorded. Use /eyeofonyx senddiscord to resend the Discord board");
            } else {

                // Try to edit message
                final Long targetMessageId = messageIDs.get(tribeIndex);
                Dreamvisitor.debug("Message ID to edit: " + targetMessageId);
                try {
                    boardChannel.retrieveMessageById(targetMessageId).queue((targetMessage) -> targetMessage.editMessageEmbeds(embed.build()).queue(), new ErrorHandler().handle(ErrorResponse.UNKNOWN_MESSAGE, (error) -> {
                        Bukkit.getLogger().warning("Unknown Message! Couldn't get message " + targetMessageId);
                        boardChannel.sendMessageEmbeds(embed.build()).queue(message -> {
                            plugin.reloadConfig();
                            List<Long> messageIds = plugin.getConfig().getLongList("royalty-board-message");
                            messageIds.set(tribeIndex, message.getIdLong());
                            plugin.getConfig().set("royalty-board-message", messageIds);
                            plugin.saveConfig();
                        });
                    }));
                    Dreamvisitor.debug("[updateDiscordBoard] Message edit queued.");
                } catch (InsufficientPermissionException e) {
                    Bukkit.getLogger().warning("Main Bot does not have permission to get the royalty board message! " + e.getMessage());
                }
            }

            // Update roles
            removeRoles(tribe);
            applyRoles(tribe);
        });
    }

    /**
     * Updates roles of this member if required.
     * This will check which roles the user should have and remove all others.
     * @param user the {@link net.dv8tion.jda.api.entities.User} to update.
     */
    public static void updateRoles(@NotNull net.dv8tion.jda.api.entities.User user) {

        java.util.UUID uuid = AccountLink.getUuid(user.getIdLong());
        Tribe tribe = null;
        int pos = CIVILIAN;

        // Get tribe and position if possible.
        // If not, that is ok.
        // All roles will just be removed.
        if (uuid != null) {

            tribe = PlayerTribe.getTribeOfPlayer(uuid);
            if (tribe != null) pos = getPositionIndexOfUUID(tribe, uuid);

        }

        Dreamvisitor.debug("[updateRoles] Updating roles of UUID " + uuid + ". Tribe " + tribe + ", pos " + pos);

        FileConfiguration config = EyeOfOnyx.getPlugin().getConfig();

        Dreamvisitor.debug("[updateRoles] Getting guilds and roles...");

        // Get guilds
        Guild mainGuild = Bot.getGameLogChannel().getGuild();

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

    }

    /**
     * Removes all royalty-associated Discord roles from tribe royalty of main and sister servers.
     * applyRoles() should be used after this to reapply the roles.
     * This does not apply to the sister guild due to how the roles are set up.
     * @param tribe the tribe whose roles to remove
     */
    public static void removeRoles(@NotNull Tribe tribe) {

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
    public static void applyRoles(@NotNull Tribe tribe) {

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

    /**
     * This will set information regardless of current royalty board.
     * @param uuid the UUID of player
     * @param tribe the tribe to set
     * @param pos the pos to set
     */
    public static void updatePermissions(@NotNull UUID uuid, @Nullable Tribe tribe, int pos) {
        if (EyeOfOnyx.luckperms != null) {

            Dreamvisitor.debug("[updatePermissions] Updating permissions for user " + uuid);

            // Get user manager
            UserManager userManager = EyeOfOnyx.luckperms.getUserManager();
            // Get user at tribe t and position p

            Dreamvisitor.debug("[updatePermissions] Updating permissions of UUID " + uuid + " of tribe " + tribe + " pos " + pos);

            // Run async
            userManager.modifyUser(uuid, user -> {

                String[] groupPositions = {"ruler", "heir", "noble", "citizen"};

                // For each tribe and position...
                for (String tribeName : Arrays.stream(TribeUtil.tribes).map(tribeCheck -> tribeCheck.getName().toLowerCase()).toList()) {
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
                if (tribe != null) {
                    String group = "citizen";
                    if (pos == RULER) {
                        group = "ruler";
                    } else if (pos <= HEIR3) {
                        group = "heir";
                    } else if (pos <= NOBLE5) {
                        group = "noble";
                    }

                    String groupName = plugin.getConfig().getString(group + "." + tribe.getName().toLowerCase());

                    if (groupName != null) {
                        Dreamvisitor.debug("[updatePermissions] Adding group " + groupName + " to " + uuid + " (pos " + pos + ")");
                        // Get the group from lp and add it to the user.
                        user.data().add(Node.builder("group." + groupName).build());
                    }
                }

            });
        } else
            Bukkit.getLogger().warning("Eye of Onyx could not hook into LuckPerms on startup. Permission update failed.");
    }

    /**
     * This will use the saved royalty board to fill information.
     * @param uuid the UUID of player
     */
    public static void updatePermissions(@NotNull UUID uuid) {
        if (EyeOfOnyx.luckperms != null) {

            Dreamvisitor.debug("[updatePermissions] Updating permissions for user " + uuid + " using info finding");

            Tribe playerTribe;
            int playerPos = CIVILIAN;

            playerTribe = PlayerTribe.getTribeOfPlayer(uuid);
            if (playerTribe != null) playerPos = getPositionIndexOfUUID(playerTribe, uuid);

            // Run async
            updatePermissions(uuid, playerTribe, playerPos);
        } else
            Bukkit.getLogger().warning("Eye of Onyx could not hook into LuckPerms on startup. Permission update failed.");
    }

    public static boolean isPositionEmpty(@NotNull Tribe tribe, int pos) {
        return getBoard().get(TribeUtil.indexOf(tribe)).getPos(pos).player == null;
    }

    /**
     * Get the position index of a player UUID.
     *
     * @param player The player UUID to search from.
     * @return The index of the position that the player holds.
     */
    public static int getPositionIndexOfUUID(@NotNull UUID player) {

        // Get player tribe
        Tribe playerTribe = PlayerTribe.getTribeOfPlayer(player);

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
    public static int getPositionIndexOfUUID(@NotNull Tribe tribe, @NotNull UUID player) {

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
    public static @Nullable java.util.UUID getUuid(@NotNull Tribe tribe, int pos) {
        return getBoard().get(TribeUtil.indexOf(tribe)).getPos(pos).player;
    }

    /**
     * Get the character name of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the character name located at the given location.
     */
    public static @Nullable String getOcName(@NotNull Tribe tribe, int pos) {
        return getBoard().get(TribeUtil.indexOf(tribe)).getPos(pos).name;
    }

    public static void setOcName(@NotNull Tribe tribe, int pos, @NotNull String name) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(TribeUtil.indexOf(tribe), royaltyBoard.get(TribeUtil.indexOf(tribe)).setName(pos, name));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get the date joined position of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the date joined located at the given location.
     */
    public static @Nullable LocalDateTime getJoinedPosDate(@NotNull Tribe tribe, int pos) {
        return getBoard().get(TribeUtil.indexOf(tribe)).getPos(pos).joinedPosition;
    }

    /**
     * Get the date joined board of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return the date joined located at the given location.
     */
    public static @Nullable LocalDateTime getJoinedBoardDate(@NotNull Tribe tribe, int pos) {
        return getBoard().get(TribeUtil.indexOf(tribe)).getPos(pos).joinedBoard;
    }

    /**
     * Get the date last online of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @return The date joined last online at the given location.
     */
    public static @Nullable LocalDateTime getLastOnline(@NotNull Tribe tribe, int pos) {
        return getBoard().get(TribeUtil.indexOf(tribe)).getPos(pos).lastOnline;
    }

    /**
     * Set the date last online of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @param time the date & time to set.
     */
    public static void setLastOnline(@NotNull Tribe tribe, int pos, @NotNull LocalDateTime time) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        int tribeIndex = TribeUtil.indexOf(tribe);
        royaltyBoard.put(tribeIndex, royaltyBoard.get(tribeIndex).setLastOnline(pos, time));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get the date last challenged of a spot on the board.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     * @return The date last challenged at the given location.
     */
    public static @Nullable LocalDateTime getLastChallengeDate(@NotNull Tribe tribe, int pos) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        return royaltyBoard.get(TribeUtil.indexOf(tribe)).getPos(pos).lastChallenge;
    }

    /**
     * Set the last challenge date of a spot on the board.
     *
     * @param tribe the tribe to fetch from.
     * @param pos the position to fetch from.
     * @param time the date & time to set.
     */
    public static void setLastChallengeDate(@NotNull Tribe tribe, int pos, @NotNull LocalDateTime time) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        int tribeIndex = TribeUtil.indexOf(tribe);
        royaltyBoard.put(tribeIndex, royaltyBoard.get(tribeIndex).setLastChallenge(pos, time));
        saveToDisk(royaltyBoard);
    }

    /**
     * Get whether a position is on cool down or not.
     *
     * @param tribe    The tribe to fetch from.
     * @param pos The position to fetch from.
     */
    public static boolean isOnCoolDown(@NotNull Tribe tribe, int pos) {
        LocalDateTime lastChallenge = getLastChallengeDate(tribe, pos);
        if (lastChallenge != null) {
            return lastChallenge.isAfter(LocalDateTime.now().minusDays(plugin.getConfig().getInt("challenge-cool-down")));
        } else return false;
    }

    public static @Nullable LocalDateTime getCooldownEnd(@NotNull Tribe tribe, int pos) {
        LocalDateTime lastChallenge = getLastChallengeDate(tribe, pos);
        if (lastChallenge != null) {
            return lastChallenge.plusDays(plugin.getConfig().getInt("challenge-cool-down"));
        } else return null;
    }

    /**
     * Moves a player from a given position to another given position.
     * If a player is in the position to be moved to, they will be removed.
     * @param tribe the tribe to target.
     * @param fromPos the first position to swap.
     * @param toPos the second position to swap.
     */
    public static void replace(@NotNull Tribe tribe, int fromPos, int toPos) {
        Dreamvisitor.debug("[replace] Replacing tribe " + tribe + " from pos " + fromPos + "(" + getUuid(tribe, fromPos) + ") to pos " + toPos + " (" + getUuid(tribe, toPos) + ")");
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(TribeUtil.indexOf(tribe), royaltyBoard.get(TribeUtil.indexOf(tribe)).replace(fromPos, toPos));
        saveToDisk(royaltyBoard);
    }

    /**
     * Remove a player from the royalty board.
     * @param tribe the tribe to target.
     * @param pos the position to target.
     * @param complete if {@code true}, also updates this user's LuckPerms permissions and Discord roles.
     */
    public static void removePlayer(@NotNull Tribe tribe, int pos, boolean complete) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        Dreamvisitor.debug("[removePlayer] Removing player at tribe " + tribe + " pos " + pos + ". Complete? " + complete);
        java.util.UUID uuid = null;
        if (complete) uuid = royaltyBoard.get(TribeUtil.indexOf(tribe)).getPos(pos).player;
        Dreamvisitor.debug("[removePlayer] UUID: " + uuid);
        royaltyBoard.put(TribeUtil.indexOf(tribe), royaltyBoard.get(TribeUtil.indexOf(tribe)).clear(pos));
        saveToDisk(royaltyBoard);

        if (uuid != null) {
            Dreamvisitor.debug("[removePlayer] Updating permissions...");
            updatePermissions(uuid, tribe, CIVILIAN);
            try {
                long discordId = AccountLink.getDiscordId(uuid);
                Dreamvisitor.debug("[removePlayer] Player has a linked Discord account by ID " + discordId + ". Updating roles...");
                Bot.getJda().retrieveUserById(discordId).queue(RoyaltyBoard::updateRoles);
            } catch (NullPointerException ignored) {}
        }
    }

    public static void set(@NotNull Tribe tribe, int pos, @NotNull BoardPosition newPosition) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(TribeUtil.indexOf(tribe), royaltyBoard.get(TribeUtil.indexOf(tribe)).updatePosition(pos, newPosition));
        saveToDisk(royaltyBoard);

    }

    public static void set(@NotNull Tribe tribe, @NotNull BoardState newState) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        royaltyBoard.put(TribeUtil.indexOf(tribe), newState);
        saveToDisk(royaltyBoard);
    }

    public static void setJoinedBoard(@NotNull Tribe tribe, int pos, @NotNull LocalDateTime dateTime) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        int tribeIndex = TribeUtil.indexOf(tribe);
        royaltyBoard.put(tribeIndex, royaltyBoard.get(tribeIndex).setJoinedBoard(pos, dateTime));
        saveToDisk(royaltyBoard);
    }

    public static void setJoinedPosition(@NotNull Tribe tribe, int pos, @NotNull LocalDateTime dateTime) {
        Map<Integer, BoardState> royaltyBoard = getBoard();
        int tribeIndex = TribeUtil.indexOf(tribe);
        royaltyBoard.put(tribeIndex, royaltyBoard.get(tribeIndex).setJoinedPosition(pos, dateTime));
        saveToDisk(royaltyBoard);
    }

}
