package io.github.stonley890.eyeofonyx;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Main;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class Discord extends ListenerAdapter {

    @SuppressWarnings({"null"})
    public static void initCommands() {
        List<CommandData> commandData = new ArrayList<>();

        commandData.add(Commands.slash("royalty", "Access or manage the royalty board.")
                .addSubcommands(
                        new SubcommandData("set", "Set the specified user to the specified position. This will overwrite the position of their tribe.")
                                .addOption(OptionType.USER, "user", "The user to set.", true, false)
                                .addOptions(new OptionData(OptionType.STRING, "position", "The position to set the user to.", true)
                                        .setAutoComplete(false)
                                        .addChoice("Ruler", "ruler")
                                        .addChoice("Crown Heir", "crown_heir")
                                        .addChoice("Apparent Heir", "apparent_heir")
                                        .addChoice("Presumptive Heir", "presumptive_heir")
                                        .addChoice("Crown Noble", "crown_noble")
                                        .addChoice("Grand Noble", "grand_noble")
                                        .addChoice("High Noble", "high_noble")
                                        .addChoice("Apparent Noble", "apparent_noble")
                                        .addChoice("Presumptive Noble", "presumptive_noble")
                                ),
                        new SubcommandData("swap", "Swap two players on the royalty board.")
                                .addOptions(new OptionData(OptionType.STRING, "tribe", "The tribe to target.", true)
                                        .setAutoComplete(false)
                                        .addChoice("Hive", "hive")
                                        .addChoice("Ice", "ice")
                                        .addChoice("Leaf", "leaf")
                                        .addChoice("Mud", "mud")
                                        .addChoice("Night", "night")
                                        .addChoice("Rain", "rain")
                                        .addChoice("Sand", "sand")
                                        .addChoice("Sea", "sea")
                                        .addChoice("Silk", "silk")
                                        .addChoice("Sky", "sky")
                                )
                                .addOptions(new OptionData(OptionType.STRING, "position-1", "The first position to swap.", true)
                                        .setAutoComplete(false)
                                        .addChoice("Ruler", "ruler")
                                        .addChoice("Crown Heir", "crown_heir")
                                        .addChoice("Apparent Heir", "apparent_heir")
                                        .addChoice("Presumptive Heir", "presumptive_heir")
                                        .addChoice("Crown Noble", "crown_noble")
                                        .addChoice("Grand Noble", "grand_noble")
                                        .addChoice("High Noble", "high_noble")
                                        .addChoice("Apparent Noble", "apparent_noble")
                                        .addChoice("Presumptive Noble", "presumptive_noble")
                                )
                                .addOptions(new OptionData(OptionType.STRING, "position-2", "The second position to swap.", true)
                                        .setAutoComplete(false)
                                        .addChoice("Ruler", "ruler")
                                        .addChoice("Crown Heir", "crown_heir")
                                        .addChoice("Apparent Heir", "apparent_heir")
                                        .addChoice("Presumptive Heir", "presumptive_heir")
                                        .addChoice("Crown Noble", "crown_noble")
                                        .addChoice("Grand Noble", "grand_noble")
                                        .addChoice("High Noble", "high_noble")
                                        .addChoice("Apparent Noble", "apparent_noble")
                                        .addChoice("Presumptive Noble", "presumptive_noble")
                                ),
                        new SubcommandData("clear", "Remove the player at the specified position.")
                                .addOptions(new OptionData(OptionType.STRING, "tribe", "The tribe to target.", true)
                                        .setAutoComplete(false)
                                        .addChoice("Hive", "hive")
                                        .addChoice("Ice", "ice")
                                        .addChoice("Leaf", "leaf")
                                        .addChoice("Mud", "mud")
                                        .addChoice("Night", "night")
                                        .addChoice("Rain", "rain")
                                        .addChoice("Sand", "sand")
                                        .addChoice("Sea", "sea")
                                        .addChoice("Silk", "silk")
                                        .addChoice("Sky", "sky")
                                )
                                .addOptions(new OptionData(OptionType.STRING, "position", "The position to target.", true)
                                        .setAutoComplete(false)
                                        .addChoice("Ruler", "ruler")
                                        .addChoice("Crown Heir", "crown_heir")
                                        .addChoice("Apparent Heir", "apparent_heir")
                                        .addChoice("Presumptive Heir", "presumptive_heir")
                                        .addChoice("Crown Noble", "crown_noble")
                                        .addChoice("Grand Noble", "grand_noble")
                                        .addChoice("High Noble", "high_noble")
                                        .addChoice("Apparent Noble", "apparent_noble")
                                        .addChoice("Presumptive Noble", "presumptive_noble")
                                ),
                        new SubcommandData("update", "Reload and force update the royalty board.")
                ).setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES)));

        commandData.add(Commands.slash("eyeofonyx", "Manage Eye of Onyx.")
                .addSubcommands(
                        new SubcommandData("ban", "Disallow a player from participating in royalty.")
                                .addOption(OptionType.USER, "user", "The user to set.", true, false),
                        new SubcommandData("unban", "Allow a previously disallowed player to participate in royalty.")
                                .addOption(OptionType.USER, "user", "The user to set.", true, false),
                        new SubcommandData("banlist", "List all banned players."),
                        new SubcommandData("freeze", "Toggle the freezing functionality.")
                ).setDefaultPermissions(DefaultMemberPermissions.DISABLED));

        // register commands
        for (CommandData commandDatum : commandData) {
            Bot.gameLogChannel.getGuild().upsertCommand(commandDatum).queue();
        }

        commandData.clear();

        Bot.getJda().addEventListener(new Discord());

    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        String command = event.getName();
        String subCommand = event.getSubcommandName();
        User user = event.getUser();

        if (command.equals("royalty")) {

            if (subCommand == null) {
                event.reply("You must specify a subcommand!").setEphemeral(true).queue();
                return;
            }

            switch (subCommand) {
                case "set" -> {

                    User targetUser;
                    String position;

                    try {
                        targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
                        position = Objects.requireNonNull(event.getOption("position")).getAsString();
                    } catch (NullPointerException e) {
                        event.reply("Missing arguments!").setEphemeral(true).queue();
                        return;
                    }

                    // Get UUID
                    UUID targetPlayerUUID = AccountLink.getUuid(targetUser.getIdLong());

                    if (targetPlayerUUID == null) {
                        event.reply("That user is not associated with a Minecraft account.").queue();
                        return;
                    }

                    // Check for ban
                    if (Banned.isPlayerBanned(targetPlayerUUID)) {
                        event.reply("This player must be unbanned first.").queue();
                        return;
                    }

                    // Get tribe
                    try {

                        // Get team of player by iterating through list
                        int playerTribe = PlayerTribe.getTribeOfPlayer(targetPlayerUUID);

                        // Make sure player is not already on the board
                        if (RoyaltyBoard.getPositionIndexOfUUID(playerTribe, targetPlayerUUID) != 5) {
                            event.reply("That player is already on the royalty board. Use the swap command instead.").queue();
                            return;
                        }

                        // Check if third argument contains a valid position
                        if (Arrays.stream(RoyaltyBoard.getValidPositions()).anyMatch(position::contains)) {

                            int targetPos = -1;

                            for (int i = 0; i < RoyaltyBoard.getValidPositions().length; i++) {
                                if (position.equals(RoyaltyBoard.getValidPositions()[i])) targetPos = i;
                            }

                            BoardState oldBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();

                            BoardPosition newPos = new BoardPosition(targetPlayerUUID, null, LocalDateTime.now(), LocalDateTime.now(),LocalDateTime.now(), LocalDateTime.now(), null, null);

                            // Remove any player that might be there
                            RoyaltyBoard.removePlayer(playerTribe, targetPos, true);

                            // Set value in board.yml
                            RoyaltyBoard.set(playerTribe, targetPos, newPos);

                            // Log update
                            BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
                            RoyaltyBoard.reportChange(new RoyaltyAction(Objects.requireNonNull(event.getMember()).getId(), playerTribe, oldBoard, newBoard));

                            event.reply("✅ " + targetUser.getAsMention() + " is now " + position.toUpperCase().replace('_', ' ')).queue();

                            RoyaltyBoard.updateBoard(playerTribe, false);

                            try {
                                RoyaltyBoard.updateDiscordBoard(playerTribe);
                            } catch (IOException e) {
                                Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                            }

                        } else
                            event.reply("Invalid position. Valid positions: " + Arrays.toString(RoyaltyBoard.getValidPositions())).queue();

                    } catch (IllegalArgumentException e) {
                        // getTeam() throws IllegalArgumentException if teams do not exist
                        event.reply("Required teams do not exist!").queue();
                    } catch (NotFoundException e) {
                        event.reply("Player is not associated with a tribe!").queue();
                        if (Main.debugMode) e.printStackTrace();
                    }
                }
                case "clear" -> {

                    String tribe;
                    String position;

                    try {
                        tribe = Objects.requireNonNull(event.getOption("tribe")).getAsString();
                        position = Objects.requireNonNull(event.getOption("position")).getAsString();
                    } catch (NullPointerException e) {
                        event.reply("Missing arguments!").setEphemeral(true).queue();
                        return;
                    }

                    int tribeIndex = io.github.stonley890.eyeofonyx.Utils.tribeIndexFromString(tribe);
                    int posIndex = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position);

                    if (tribeIndex == -1) {
                        event.reply("Not a valid tribe!").queue();
                        return;
                    }
                    if (posIndex == -1) {
                        event.reply("Not a valid position!").queue();
                        return;
                    }

                    UUID uuid = RoyaltyBoard.getUuid(tribeIndex, posIndex);
                    if (uuid == null) {
                        event.reply("That position is already empty!").queue();
                        return;
                    }

                    BoardState oldBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();

                    Challenge.removeChallengesOfPlayer(uuid, "The other player in your challenge was removed from the royalty board.");
                    Notification.removeNotificationsOfPlayer(uuid, NotificationType.CHALLENGE_REQUESTED);
                    Notification.removeNotificationsOfPlayer(uuid, NotificationType.CHALLENGE_ACCEPTED);

                    new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. All pending challenges have been canceled.", NotificationType.GENERIC).create();

                    RoyaltyBoard.removePlayer(tribeIndex, posIndex, true);

                    BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
                    RoyaltyBoard.reportChange(new RoyaltyAction(Objects.requireNonNull(event.getMember()).getId(), tribeIndex, oldBoard, newBoard));
                    RoyaltyBoard.updateBoard(tribeIndex, false);
                    try {
                        RoyaltyBoard.updateDiscordBoard(tribeIndex);
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                    }
                    event.reply("✅ " + tribe.toUpperCase() + " " + position.toUpperCase() + " position cleared.").queue();

                }
                case "swap" -> {

                    String tribe;
                    String position1;
                    String position2;

                    tribe = event.getOption("tribe", OptionMapping::getAsString);
                    position1 = event.getOption("position-1", OptionMapping::getAsString);
                    position2 = event.getOption("position-2", OptionMapping::getAsString);

                    if (Objects.equals(position1, position2)) {
                        event.reply("You cannot swap a position with itself!").queue();
                        return;
                    }

                    int tribeIndex = io.github.stonley890.eyeofonyx.Utils.tribeIndexFromString(tribe);
                    int posIndex1 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position1);
                    int posIndex2 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position2);

                    if (tribeIndex == -1) {
                        event.reply("Not a valid tribe!").queue();
                        return;
                    }
                    if (posIndex1 == -1 || posIndex2 == -1) {
                        event.reply("Not a valid position!").queue();
                        return;
                    }

                    BoardState oldBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();

                    BoardPosition pos1 = RoyaltyBoard.getBoardOf(tribeIndex).getPos(posIndex1);
                    BoardPosition pos2 = RoyaltyBoard.getBoardOf(tribeIndex).getPos(posIndex2);

                    // Remove challenges
                    Challenge.removeChallengesOfPlayer(pos1.player, "The player who was in your challenge was moved to a different position.");
                    Challenge.removeChallengesOfPlayer(pos2.player, "The player who was in your challenge was moved to a different position.");

                    if (pos1.player != null) {
                        Notification.removeNotificationsOfPlayer(pos1.player, NotificationType.CHALLENGE_ACCEPTED);
                        Notification.removeNotificationsOfPlayer(pos1.player, NotificationType.CHALLENGE_REQUESTED);
                    }
                    if (pos2.player != null) {
                        Notification.removeNotificationsOfPlayer(pos2.player, NotificationType.CHALLENGE_ACCEPTED);
                        Notification.removeNotificationsOfPlayer(pos2.player, NotificationType.CHALLENGE_REQUESTED);
                    }


                    // Apply change
                    RoyaltyBoard.set(tribeIndex, RoyaltyBoard.getBoardOf(tribeIndex).swap(posIndex1, posIndex2));

                    // Notify users
                    new Notification(pos1.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", NotificationType.GENERIC).create();
                    new Notification(pos2.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", NotificationType.GENERIC).create();

                    // Send update
                    BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
                    RoyaltyBoard.reportChange(new RoyaltyAction(event.getUser().getId(), tribeIndex, oldBoard, newBoard));

                    assert position1 != null;
                    assert position2 != null;
                    assert tribe != null;
                    event.reply("Swapped " + position1.toUpperCase() + " and " + position2.toUpperCase() + " of " + tribe.toUpperCase()).queue();

                    RoyaltyBoard.updateBoard(tribeIndex, false);
                    try {
                        RoyaltyBoard.updateDiscordBoard(tribeIndex);
                    } catch (IOException e) {
                        Bukkit.getLogger().severe("Unable to update Discord board!");
                    }

                }
                case "update" -> {
                    InteractionHook hook = event.getHook();
                    event.deferReply().queue();

                    try {
                        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                            RoyaltyBoard.updateBoard(i, false);
                            RoyaltyBoard.updateDiscordBoard(i);
                        }
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                    }
                    hook.editOriginal("✅ Board updated. It may take some time for changes to apply everywhere.").queue();
                }
                default ->
                        event.reply("Invalid arguments! /royalty <set|clear|update>").queue();
            }

        } else if (command.equals("eyeofonyx")) {

            if (subCommand == null) {
                event.reply("You must specify a subcommand!").setEphemeral(true).queue();
                return;
            }

            switch (subCommand) {
                case "ban" -> {

                    User targetUser;

                    try {
                        targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
                    } catch (NullPointerException e) {
                        event.reply("Missing arguments!").queue();
                        return;
                    }

                    UUID uuid = AccountLink.getUuid(targetUser.getIdLong());

                    if /* the username is invalid */ (uuid == null) {
                        event.reply("That user could not be found.").queue();
                    } else /* the username is valid */ {
                        if /* player is already banned */ (Banned.isPlayerBanned(uuid)) {
                            event.reply("That player is already banned.").queue();
                        } else /* player is not yet banned */ {

                            // Add player to ban list, remove them from the royalty board, and send them a notification.
                            Banned.addPlayer(uuid);
                            int tribe = -1;
                            try {
                                tribe = PlayerTribe.getTribeOfPlayer(uuid);
                            } catch (NotFoundException e) {
                                // Player does not have a tribe.
                            }
                            int pos = 0;
                            try {
                                pos = RoyaltyBoard.getPositionIndexOfUUID(uuid);
                            } catch (NotFoundException e) {
                                // Player does not have a tribe.
                            }

                            if (pos != -1 && pos != RoyaltyBoard.CIVILIAN) {

                                // Remove any challenges
                                Challenge.removeChallengesOfPlayer(uuid, "The player who was challenging you was removed from the royalty board, so your challenge was canceled.");

                                // Remove any challenge notifications
                                Notification.removeNotificationsOfPlayer(uuid, NotificationType.CHALLENGE_REQUESTED);
                                Notification.removeNotificationsOfPlayer(uuid, NotificationType.CHALLENGE_ACCEPTED);

                                RoyaltyBoard.removePlayer(tribe, pos, true);
                                RoyaltyBoard.updateBoard(tribe, false);
                                try {
                                    RoyaltyBoard.updateDiscordBoard(tribe);
                                } catch (IOException e) {
                                    Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                                }
                            }
                            new Notification(uuid, "Royalty Ban", "You are no longer allowed to participate in royalty. Contact staff if you think this is a mistake.", NotificationType.GENERIC).create();
                            event.reply(user.getAsMention() + " has been banned.").queue();

                        }
                    }

                }
                case "unban" -> {

                    User targetUser;

                    try {
                        targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
                    } catch (NullPointerException e) {
                        event.reply("Missing arguments!").queue();
                        return;
                    }

                    UUID uuid = AccountLink.getUuid(targetUser.getIdLong());

                    if /* the username is invalid */ (uuid == null) {
                        event.reply("That user could not be found.").queue();
                    } else /* the username is valid */ {
                        if /* player is banned */ (Banned.isPlayerBanned(uuid)) {
                            Banned.removePlayer(uuid);
                            event.reply(user.getAsMention() + " has been unbanned.").queue();
                        } else /* player is not banned */ {
                            event.reply("That player is not banned.").queue();
                        }
                    }

                }
                case "banlist" -> {

                    event.deferReply().queue();

                    Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {

                        Mojang mojang = new Mojang().connect();

                        EmbedBuilder message = new EmbedBuilder();
                        message.setTitle("Banned players:\n");

                        StringBuilder description = new StringBuilder();

                        for (String bannedPlayer : Banned.getBannedPlayers()) {

                            String username = mojang.getPlayerProfile(bannedPlayer).getUsername();
                            long discordId = AccountLink.getDiscordId(UUID.fromString(bannedPlayer));

                            description.append("`").append(username).append("`: ").append("<@").append(discordId).append(">\n");
                        }
                        message.setDescription(description);
                        event.getHook().editOriginalEmbeds(message.build()).queue();
                    });

                }
                case "freeze" -> {

                    if (RoyaltyBoard.isFrozen()) {
                        RoyaltyBoard.setFrozen(false);
                        event.reply("The royalty board is now unfrozen.").queue();
                    } else {
                        RoyaltyBoard.setFrozen(true);
                        event.reply("The royalty board is now frozen.").queue();
                    }

                }
            }

        }

    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        Button button = event.getButton();

        String id = button.getId();
        if (id == null) return;

        if (id.startsWith("revertaction-")) {

            String targetId = id.substring("revertaction-".length());

            for (RoyaltyAction royaltyAction : RoyaltyAction.actionHistory) {
                if (targetId.equals(String.valueOf(royaltyAction.id))) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Are you sure?")
                            .setDescription("Reverting this action will revert all values of this tribe to the values they were set to before this change.")
                            .setFooter("This action cannot be undone.");

                    for (int pos = 0; pos < RoyaltyBoard.CIVILIAN; pos++) {


                        BoardPosition position = royaltyAction.oldState.getPos(pos);

                        StringBuilder values = new StringBuilder();

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
                        if (position.player != null) uuid = position.player.toString();
                        if (position.player != null) username = PlayerUtility.getUsernameOfUuid(uuid);
                        if (position.name != null) ocName = position.name;
                        if (position.lastOnline != null) lastOnline = position.lastOnline.toString();
                        if (position.lastChallenge != null) lastChallenge = position.lastChallenge.toString();
                        if (position.joinedBoard != null) joinedBoard = position.joinedBoard.toString();
                        if (position.joinedPosition != null) joinedPos = position.joinedPosition.toString();
                        if (position.challenger != null) challenger = PlayerUtility.getUsernameOfUuid(position.challenger.toString());
                        if (position.challenging != null) challenging = PlayerUtility.getUsernameOfUuid(position.challenging.toString());

                        String discordUser = "N/A";

                        try {
                            long discordId = AccountLink.getDiscordId(position.player);
                            net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordId).complete();
                            if (user != null) discordUser = user.getAsMention();
                        } catch (NullPointerException IllegalArgumentException) {
                            // No user
                        }

                        values.append("\nPlayer: ").append(Bot.escapeMarkdownFormatting(username))
                                .append("\nUUID: ").append(uuid)
                                .append("\nUser: ").append(discordUser)
                                .append("\nOC Name: ").append(Bot.escapeMarkdownFormatting(ocName))
                                .append("\nLast Online: ").append(lastOnline)
                                .append("\nLast Challenge: ").append(lastChallenge)
                                .append("\nDate Joined Board: ").append(joinedBoard)
                                .append("\nDate Joined Position: ").append(joinedPos)
                                .append("\nChallenger: ").append(Bot.escapeMarkdownFormatting(challenger))
                                .append("\nChallenging: ").append(Bot.escapeMarkdownFormatting(challenging));

                        embed.addField(RoyaltyBoard.getValidPositions()[pos].replace("_", " ").toUpperCase(), values.toString(), false);

                    }

                    ActionRow actionRow = ActionRow.of(Button.danger("revertconfirm-" + royaltyAction.id, "Yes, revert to this state."));
                    event.replyEmbeds(embed.build()).addActionRows(actionRow).queue();
                    break;
                }
            }

        } else if (id.startsWith("revertconfirm-")) {
            String targetId = id.substring("revertconfirm-".length());

            List<RoyaltyAction> actionHistory = RoyaltyAction.actionHistory;
            for (RoyaltyAction royaltyAction : actionHistory) {
                if (targetId.equals(String.valueOf(royaltyAction.id))) {

                    RoyaltyBoard.getBoard().put(royaltyAction.affectedTribe, royaltyAction.oldState);
                    RoyaltyBoard.saveToDisk();
                    RoyaltyBoard.updateBoard(royaltyAction.affectedTribe, false);
                    try {
                        RoyaltyBoard.updateDiscordBoard(royaltyAction.affectedTribe);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    event.reply("✅ Values reverted.").queue();
                    event.getInteraction().editButton(button.asDisabled()).queue();
                    break;

                }
            }
        }

    }

}
