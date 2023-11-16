package io.github.stonley890.eyeofonyx;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.Utils;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
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
                                        .addChoice("Heir Apparent", "heir_apparent")
                                        .addChoice("Heir Presumptive", "heir_presumptive")
                                        .addChoice("Noble Apparent", "noble_apparent")
                                        .addChoice("Noble Presumptive", "noble_presumptive")
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
                                        .addChoice("Heir Apparent", "heir_apparent")
                                        .addChoice("Heir Presumptive", "heir_presumptive")
                                        .addChoice("Noble Apparent", "noble_apparent")
                                        .addChoice("Noble Presumptive", "noble_presumptive")
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

                    // Get tribe from scoreboard team
                    try {

                        // Get team of player by iterating through list
                        int playerTribe = PlayerTribe.getTribeOfPlayer(targetPlayerUUID);

                        // Check if third argument contains a valid position
                        if (Arrays.stream(RoyaltyBoard.getValidPositions()).anyMatch(position::contains)) {

                            int targetPos = -1;

                            for (int i = 0; i < RoyaltyBoard.getValidPositions().length; i++) {
                                if (position.equals(RoyaltyBoard.getValidPositions()[i])) targetPos = i;
                            }

                            BoardState oldBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();

                            BoardPosition newPos = new BoardPosition(targetPlayerUUID, null, LocalDateTime.now(), LocalDateTime.now(),LocalDateTime.now(), LocalDateTime.now(), null, null);

                            // Set value in board.yml
                            RoyaltyBoard.set(playerTribe, targetPos, newPos);

                            BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
                            RoyaltyBoard.sendUpdate(new RoyaltyAction(event.getMember().getId(), playerTribe, oldBoard, newBoard));

                            event.reply("✅ " + targetUser.getAsMention() + " is now " + position.toUpperCase().replace('_', ' ')).queue();

                            RoyaltyBoard.updateBoard();

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
                        if (Dreamvisitor.debug) e.printStackTrace();
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

                    int tribeIndex = -1;
                    int posIndex = -1;

                    for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                        String vTribe = RoyaltyBoard.getTribes()[i];
                        if (vTribe.equals(tribe)) {
                            tribeIndex = i;
                            break;
                        }
                    }

                    if (tribeIndex == -1) {
                        event.reply("Not a valid tribe!").queue();
                        return;
                    }

                    for (int i = 0; i < RoyaltyBoard.getValidPositions().length; i++) {
                        String vTribe = RoyaltyBoard.getValidPositions()[i];
                        if (vTribe.equals(position)) {
                            posIndex = i;
                            break;
                        }
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

                    try {

                        // Notify attacker if exists
                        UUID attacker = RoyaltyBoard.getAttacker(tribeIndex, posIndex);
                        if (attacker != null) {
                            int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                            RoyaltyBoard.setAttacking(tribeIndex, attackerPos, null);
                            new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                        }

                        // Notify defender if exists
                        if (posIndex != RoyaltyBoard.RULER) {
                            UUID attacking = RoyaltyBoard.getAttacking(tribeIndex, posIndex);
                            if (attacking != null) {
                                int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                                RoyaltyBoard.setAttacker(tribeIndex, defenderPos, null);
                                new Notification(attacker, "Your challenge was canceled.", "The player who was challenging you was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                            }
                        }


                        // Remove any challenges
                        for (Challenge challenge : Challenge.getChallenges()) {
                            if (challenge.defender.equals(uuid) || challenge.attacker.equals(uuid))
                                Challenge.remove(challenge);
                        }

                        // Remove any challenge notifications
                        for (Notification notification : Notification.getNotificationsOfPlayer(uuid)) {
                            if (notification.type == NotificationType.CHALLENGE_ACCEPTED || notification.type == NotificationType.CHALLENGE_REQUESTED)
                                Notification.removeNotification(notification);
                        }
                    } catch (IOException | InvalidConfigurationException e) {
                        e.printStackTrace();
                    } catch (NotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. All pending challenges have been canceled.", NotificationType.GENERIC).create();

                    Dreamvisitor.debug("OldPos is null: " +  (oldBoard.getPos(posIndex).player == null));
                    RoyaltyBoard.removePlayer(tribeIndex, posIndex);
                    Dreamvisitor.debug("OldPos is null: " +  (oldBoard.getPos(posIndex).player == null));
                    BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
                    Dreamvisitor.debug("OldPos is null: " +  (oldBoard.getPos(posIndex).player == null));
                    Dreamvisitor.debug("NewPos is null: " +  (newBoard.getPos(posIndex).player == null));
                    RoyaltyBoard.sendUpdate(new RoyaltyAction(event.getMember().getId(), tribeIndex, oldBoard, newBoard));
                    Dreamvisitor.debug("OldPos is null: " +  (oldBoard.getPos(posIndex).player == null));
                    Dreamvisitor.debug("NewPos is null: " +  (newBoard.getPos(posIndex).player == null));
                    Dreamvisitor.debug("Currentpos is null: " +  (RoyaltyBoard.getBoardOf(tribeIndex).getPos(posIndex).player == null));

                    RoyaltyBoard.updateBoard();
                    try {
                        RoyaltyBoard.updateDiscordBoard(tribeIndex);
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                    }
                    event.reply("✅ " + tribe.toUpperCase() + " " + position.toUpperCase() + " position cleared.").queue();

                }
                case "update" -> {
                    RoyaltyBoard.reload();
                    RoyaltyBoard.updateBoard();
                    try {
                        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                            RoyaltyBoard.updateDiscordBoard(i);
                        }
                    } catch (IOException e) {
                        Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                    }
                    event.reply("✅ " + EyeOfOnyx.EOO + ChatColor.YELLOW + "Board updated.").queue();
                }
                default ->
                        event.reply(EyeOfOnyx.EOO + ChatColor.RED + "Invalid arguments! /royalty <set|clear|update>").queue();
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

                                try {

                                    // Notify attacker if exists
                                    UUID attacker = RoyaltyBoard.getAttacker(tribe,pos);
                                    if (attacker != null) {
                                        int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                                        RoyaltyBoard.setAttacking(tribe, attackerPos, null);
                                        new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                                    }

                                    // Notify defender if exists
                                    if (pos != RoyaltyBoard.RULER) {
                                        UUID attacking = RoyaltyBoard.getAttacking(tribe,pos);
                                        if (attacking != null) {
                                            int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                                            RoyaltyBoard.setAttacker(tribe, defenderPos, null);
                                            new Notification(attacker, "Your challenge was canceled.", "The player who was challenging you was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                                        }
                                    }

                                    // Remove any challenges
                                    for (Challenge challenge : Challenge.getChallenges()) {
                                        if (challenge.defender.equals(uuid) || challenge.attacker.equals(uuid)) Challenge.remove(challenge);
                                    }

                                    // Remove any challenge notifications
                                    for (Notification notification : Notification.getNotificationsOfPlayer(uuid)) {
                                        if (notification.type == NotificationType.CHALLENGE_ACCEPTED || notification.type == NotificationType.CHALLENGE_REQUESTED) Notification.removeNotification(notification);
                                    }
                                } catch (IOException | InvalidConfigurationException e) {
                                    if (Dreamvisitor.debug) e.printStackTrace();
                                } catch (NotFoundException e) {
                                    throw new RuntimeException(e);
                                }

                                RoyaltyBoard.removePlayer(tribe, pos);
                                RoyaltyBoard.updateBoard();
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
    public void onButtonInteraction(ButtonInteractionEvent event) {

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

                    for (int pos = 0; pos < 5; pos++) {


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
                        if (position.player != null) username = Utils.getUsernameOfUuid(uuid);
                        if (position.name != null) ocName = position.name;
                        if (position.lastOnline != null) lastOnline = position.lastOnline.toString();
                        if (position.lastChallenge != null) lastChallenge = position.lastChallenge.toString();
                        if (position.joinedBoard != null) joinedBoard = position.joinedBoard.toString();
                        if (position.joinedPosition != null) joinedPos = position.joinedPosition.toString();
                        if (position.challenger != null) challenger = Utils.getUsernameOfUuid(position.challenger.toString());
                        if (position.challenging != null) challenging = Utils.getUsernameOfUuid(position.challenging.toString());

                        String discordUser = "N/A";

                        try {
                            long discordId = AccountLink.getDiscordId(position.player);
                            net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordId).complete();
                            if (user != null) discordUser = user.getAsMention();
                        } catch (NullPointerException IllegalArgumentException) {
                            // No user
                        }

                        values.append("\nPlayer: ").append(io.github.stonley890.dreamvisitor.Utils.escapeMarkdownFormatting(username))
                                .append("\nUUID: ").append(uuid)
                                .append("\nUser: ").append(discordUser)
                                .append("\nOC Name: ").append(io.github.stonley890.dreamvisitor.Utils.escapeMarkdownFormatting(ocName))
                                .append("\nLast Online: ").append(lastOnline)
                                .append("\nLast Challenge: ").append(lastChallenge)
                                .append("\nDate Joined Board: ").append(joinedBoard)
                                .append("\nDate Joined Position: ").append(joinedPos)
                                .append("\nChallenger: ").append(io.github.stonley890.dreamvisitor.Utils.escapeMarkdownFormatting(challenger))
                                .append("\nChallenging: ").append(io.github.stonley890.dreamvisitor.Utils.escapeMarkdownFormatting(challenging));

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
                    RoyaltyBoard.save();
                    RoyaltyBoard.updateBoard();
                    event.reply("✅ Values reverted.").queue();
                    event.getInteraction().editButton(button.asDisabled()).queue();
                    break;

                }
            }
        }

    }

}
