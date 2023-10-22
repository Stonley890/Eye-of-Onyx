package io.github.stonley890.eyeofonyx;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
                ).setDefaultPermissions(DefaultMemberPermissions.DISABLED));

        commandData.add(Commands.slash("eyeofonyx", "Manage Eye of Onyx.")
                .addSubcommands(
                        new SubcommandData("ban", "Disallow a player from participating in royalty.")
                                .addOption(OptionType.USER, "user", "The user to set.", true, false),
                        new SubcommandData("unban", "Allow a previously disallowed player to participate in royalty.")
                                .addOption(OptionType.USER, "user", "The user to set.", true, false),
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
                    String targetPlayerUUID = Utils.formatUuid(AccountLink.getUuid(targetUser.getId()));

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

                            // Set value in board.yml
                            RoyaltyBoard.setValue(playerTribe, targetPos, "uuid", targetPlayerUUID.toString());
                            RoyaltyBoard.setValue(playerTribe, targetPos, "last_online", LocalDateTime.now().toString());
                            RoyaltyBoard.setValue(playerTribe, targetPos, "last_challenge_time", LocalDateTime.now().toString());
                            RoyaltyBoard.setValue(playerTribe, targetPos, "joined_time", LocalDateTime.now().toString());
                            RoyaltyBoard.setValue(playerTribe, targetPos, "challenger", "none");
                            if (targetPos == RoyaltyBoard.RULER)
                                RoyaltyBoard.setValue(playerTribe, targetPos, "challenging", "none");

                            event.reply("✅ " + user.getAsMention() + " is now " + position.toUpperCase().replace('_', ' ')).queue();

                            RoyaltyBoard.setValue(playerTribe, targetPos, "joined_time", LocalDateTime.now().toString());
                            RoyaltyBoard.setValue(playerTribe, targetPos, "last_challenge_time", LocalDateTime.now().toString());
                            RoyaltyBoard.setValue(playerTribe, targetPos, "last_online", LocalDateTime.now().toString());

                            RoyaltyBoard.save(RoyaltyBoard.get());
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

                    String uuid = RoyaltyBoard.getUuid(tribeIndex, posIndex);
                    if (uuid.equals("null")) {
                        event.reply("That position is already empty!").queue();
                        return;
                    }

                    try {

                        // Notify attacker if exists
                        String attacker = RoyaltyBoard.getAttacker(tribeIndex, posIndex);
                        if (!attacker.equals("none")) {
                            int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                            RoyaltyBoard.setAttacking(tribeIndex, attackerPos, "none");
                            new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                        }

                        // Notify defender if exists
                        if (posIndex != RoyaltyBoard.RULER) {
                            String attacking = RoyaltyBoard.getAttacking(tribeIndex, posIndex);
                            if (!attacking.equals("none")) {
                                int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                                RoyaltyBoard.setAttacker(tribeIndex, defenderPos, "none");
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

                    new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. And pending challenges have been canceled.", NotificationType.GENERIC).create();

                    RoyaltyBoard.removePlayer(tribeIndex, posIndex);

                    // If not ruler clear challenging
                    if (posIndex != 0) {
                        RoyaltyBoard.setValue(tribeIndex, posIndex, "challenging", "none");
                    }

                    RoyaltyBoard.save(RoyaltyBoard.get());
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

            RoyaltyBoard.save(RoyaltyBoard.get());
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

                    String uuid = AccountLink.getUuid(targetUser.getId());

                    if /* the username is invalid */ (uuid == null) {
                        event.reply("That user could not be found.").queue();
                    } else /* the username is valid */ {
                        if /* player is already banned */ (Banned.isPlayerBanned(uuid)) {
                            event.reply("That player is already banned.").queue();
                        } else /* player is not yet banned */ {

                            // Add player to ban list, remove them from the royalty board, and send them a notification.
                            Banned.addPlayer(uuid);
                            int tribe = 0;
                            try {
                                tribe = PlayerTribe.getTribeOfPlayer(uuid);
                            } catch (NotFoundException e) {
                                // Player does not have a tribe.
                                event.reply("This player does not have an associated tribe.").queue();
                                return;
                            }
                            int pos = 0;
                            try {
                                pos = RoyaltyBoard.getPositionIndexOfUUID(uuid);
                            } catch (NotFoundException e) {
                                // Player does not have a tribe.
                                event.reply("This player does not have an associated tribe.").queue();
                                return;
                            }

                            try {

                                // Notify attacker if exists
                                String attacker = RoyaltyBoard.getAttacker(tribe,pos);
                                if (!attacker.equals("none")) {
                                    int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                                    RoyaltyBoard.setAttacking(tribe, attackerPos, "none");
                                    new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                                }

                                // Notify defender if exists
                                if (pos != RoyaltyBoard.RULER) {
                                    String attacking = RoyaltyBoard.getAttacking(tribe,pos);
                                    if (!attacking.equals("none")) {
                                        int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                                        RoyaltyBoard.setAttacker(tribe, defenderPos, "none");
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

                            if (pos != -1 && pos != RoyaltyBoard.CIVILIAN) {
                                RoyaltyBoard.removePlayer(tribe, pos);
                                RoyaltyBoard.updateBoard();
                                try {
                                    RoyaltyBoard.updateDiscordBoard(tribe);
                                } catch (IOException e) {
                                    Bukkit.getLogger().warning(EyeOfOnyx.EOO + org.bukkit.ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
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

                    String uuid = AccountLink.getUuid(targetUser.getId());

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

                } case "freeze" -> {

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

}
