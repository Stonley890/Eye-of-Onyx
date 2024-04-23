package io.github.stonley890.eyeofonyx.discord.commands;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.dreamvisitor.discord.commands.DiscordCommand;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DCmdRoyalty implements DiscordCommand {
    @Override
    public @NotNull SlashCommandData getCommandData() {
        return Commands.slash("royalty", "Access or manage the royalty board.")
                .addSubcommands(
                        new SubcommandData("set", "Set the specified user to the specified position. This will overwrite the position of their tribe.")
                                .addOption(OptionType.USER, "user", "The user to set.", true, false)
                                .addOptions(
                                        new OptionData(OptionType.STRING, "position", "The position to set the user to.", true)
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
                                .addOptions(
                                        new OptionData(OptionType.STRING, "tribe", "The tribe to target.", true)
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
                                                .addChoice("Sky", "sky"),
                                        new OptionData(OptionType.STRING, "position-1", "The first position to swap.", true)
                                                .setAutoComplete(false)
                                                .addChoice("Ruler", "ruler")
                                                .addChoice("Crown Heir", "crown_heir")
                                                .addChoice("Apparent Heir", "apparent_heir")
                                                .addChoice("Presumptive Heir", "presumptive_heir")
                                                .addChoice("Crown Noble", "crown_noble")
                                                .addChoice("Grand Noble", "grand_noble")
                                                .addChoice("High Noble", "high_noble")
                                                .addChoice("Apparent Noble", "apparent_noble")
                                                .addChoice("Presumptive Noble", "presumptive_noble"),
                                        new OptionData(OptionType.STRING, "position-2", "The second position to swap.", true)
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
                                .addOptions(
                                        new OptionData(OptionType.STRING, "tribe", "The tribe to target.", true)
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
                                                .addChoice("Sky", "sky"),
                                        new OptionData(OptionType.STRING, "position", "The position to target.", true)
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
                        new SubcommandData("update", "Reload and force update the royalty board."),
                        new SubcommandData("challenge", "Manage challenges.")
                                .addOptions(
                                        new OptionData(OptionType.USER, "user", "The user whose challenge to fetch", false)
                                )
                ).setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES));
    }

    @Override
    public void onCommand(@NotNull SlashCommandInteractionEvent event) {

        String subCommand = event.getSubcommandName();

        if (subCommand == null) {
            event.reply("You must specify a subcommand!").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(false).queue();

        switch (subCommand) {
            case "set" -> {

                User targetUser;
                String position;

                try {
                    targetUser = Objects.requireNonNull(event.getOption("user")).getAsUser();
                    position = Objects.requireNonNull(event.getOption("position")).getAsString();
                } catch (NullPointerException e) {
                    event.getHook().setEphemeral(true).editOriginal("Missing arguments!").queue();
                    return;
                }

                // Get UUID
                UUID targetPlayerUUID = AccountLink.getUuid(targetUser.getIdLong());

                if (targetPlayerUUID == null) {
                    event.getHook().setEphemeral(false).editOriginal("That user is not associated with a Minecraft account.").queue();
                    return;
                }

                // Check for ban
                if (Banned.isPlayerBanned(targetPlayerUUID)) {
                    event.getHook().setEphemeral(false).editOriginal("This player must be unbanned first.").queue();
                    return;
                }

                // Get tribe
                try {

                    // Get team of player by iterating through list
                    int playerTribe = PlayerTribe.getTribeOfPlayer(targetPlayerUUID);

                    // Make sure player is not already on the board
                    if (RoyaltyBoard.getPositionIndexOfUUID(playerTribe, targetPlayerUUID) != RoyaltyBoard.CIVILIAN) {
                        event.getHook().setEphemeral(false).editOriginal("That player is already on the royalty board. Use the swap command instead.").queue();
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

                        event.getHook().setEphemeral(false).editOriginal("✅ " + targetUser.getAsMention() + " is now " + position.toUpperCase().replace('_', ' ')).queue();

                        RoyaltyBoard.updateBoard(playerTribe, false);

                        try {
                            RoyaltyBoard.updateDiscordBoard(playerTribe);
                        } catch (IOException e) {
                            Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board. " + e.getMessage());
                        }

                    } else
                        event.getHook().setEphemeral(true).editOriginal("Invalid position. Valid positions: " + Arrays.toString(RoyaltyBoard.getValidPositions())).queue();

                } catch (IllegalArgumentException e) {
                    // getTeam() throws IllegalArgumentException if teams do not exist
                    event.getHook().setEphemeral(false).editOriginal("Required teams do not exist!").queue();
                } catch (NotFoundException e) {
                    event.getHook().setEphemeral(false).editOriginal("Player is not associated with a tribe!").queue();
                    if (Dreamvisitor.debugMode) e.printStackTrace();
                }
            }
            case "clear" -> {

                String tribe;
                String position;

                try {
                    tribe = Objects.requireNonNull(event.getOption("tribe")).getAsString();
                    position = Objects.requireNonNull(event.getOption("position")).getAsString();
                } catch (NullPointerException e) {
                    event.getHook().setEphemeral(true).editOriginal("Missing arguments!").queue();
                    return;
                }

                int tribeIndex = io.github.stonley890.eyeofonyx.Utils.tribeIndexFromString(tribe);
                int posIndex = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position);

                if (tribeIndex == -1) {
                    event.getHook().setEphemeral(true).editOriginal("Not a valid tribe!").queue();
                    return;
                }
                if (posIndex == -1) {
                    event.getHook().setEphemeral(true).editOriginal("Not a valid position!").queue();
                    return;
                }

                UUID uuid = RoyaltyBoard.getUuid(tribeIndex, posIndex);
                if (uuid == null) {
                    event.getHook().setEphemeral(false).editOriginal("That position is already empty!").queue();
                    return;
                }

                BoardState oldBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();

                Challenge.removeChallengesOfPlayer(uuid, "The other player in your challenge was removed from the royalty board.");
                Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_REQUESTED);
                Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_ACCEPTED);

                new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. All pending challenges have been canceled.", Notification.Type.GENERIC).create();

                RoyaltyBoard.removePlayer(tribeIndex, posIndex, true);

                BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
                RoyaltyBoard.reportChange(new RoyaltyAction(Objects.requireNonNull(event.getMember()).getId(), tribeIndex, oldBoard, newBoard));
                RoyaltyBoard.updateBoard(tribeIndex, false);
                try {
                    RoyaltyBoard.updateDiscordBoard(tribeIndex);
                } catch (IOException e) {
                    Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                }
                event.getHook().setEphemeral(false).editOriginal("✅ " + tribe.toUpperCase() + " " + position.toUpperCase() + " position cleared.").queue();

            }
            case "swap" -> {

                String tribe;
                String position1;
                String position2;

                tribe = event.getOption("tribe", OptionMapping::getAsString);
                position1 = event.getOption("position-1", OptionMapping::getAsString);
                position2 = event.getOption("position-2", OptionMapping::getAsString);

                if (Objects.equals(position1, position2)) {
                    event.getHook().setEphemeral(false).editOriginal("You cannot swap a position with itself!").queue();
                    return;
                }

                int tribeIndex = io.github.stonley890.eyeofonyx.Utils.tribeIndexFromString(tribe);
                int posIndex1 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position1);
                int posIndex2 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position2);

                if (tribeIndex == -1) {
                    event.getHook().setEphemeral(true).editOriginal("Not a valid tribe!").queue();
                    return;
                }
                if (posIndex1 == -1 || posIndex2 == -1) {
                    event.getHook().setEphemeral(true).editOriginal("Not a valid position!").queue();
                    return;
                }

                BoardState oldBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();

                BoardPosition pos1 = RoyaltyBoard.getBoardOf(tribeIndex).getPos(posIndex1);
                BoardPosition pos2 = RoyaltyBoard.getBoardOf(tribeIndex).getPos(posIndex2);

                if (pos1.player == null || pos2.player == null) {
                    event.getHook().setEphemeral(false).editOriginal("You cannot swap with an empty position!").queue();
                    return;
                }

                // Remove challenges
                Challenge.removeChallengesOfPlayer(pos1.player, "The player who was in your challenge was moved to a different position.");
                Challenge.removeChallengesOfPlayer(pos2.player, "The player who was in your challenge was moved to a different position.");

                if (pos1.player != null) {
                    Notification.removeNotificationsOfPlayer(pos1.player, Notification.Type.CHALLENGE_ACCEPTED);
                    Notification.removeNotificationsOfPlayer(pos1.player, Notification.Type.CHALLENGE_REQUESTED);
                }
                if (pos2.player != null) {
                    Notification.removeNotificationsOfPlayer(pos2.player, Notification.Type.CHALLENGE_ACCEPTED);
                    Notification.removeNotificationsOfPlayer(pos2.player, Notification.Type.CHALLENGE_REQUESTED);
                }


                // Apply change
                RoyaltyBoard.set(tribeIndex, RoyaltyBoard.getBoardOf(tribeIndex).swap(posIndex1, posIndex2));

                // Notify users
                new Notification(pos1.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", Notification.Type.GENERIC).create();
                new Notification(pos2.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", Notification.Type.GENERIC).create();

                // Send update
                BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
                RoyaltyBoard.reportChange(new RoyaltyAction(event.getUser().getId(), tribeIndex, oldBoard, newBoard));

                assert position1 != null;
                assert position2 != null;
                assert tribe != null;
                event.getHook().setEphemeral(false).editOriginal("Swapped " + position1.toUpperCase() + " and " + position2.toUpperCase() + " of " + tribe.toUpperCase()).queue();

                RoyaltyBoard.updateBoard(tribeIndex, false);
                try {
                    RoyaltyBoard.updateDiscordBoard(tribeIndex);
                } catch (IOException e) {
                    Bukkit.getLogger().severe("Unable to update Discord board!");
                }

            }
            case "update" -> {

                try {
                    for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                        RoyaltyBoard.updateBoard(i, false);
                        RoyaltyBoard.updateDiscordBoard(i);
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                }
                event.getHook().setEphemeral(false).editOriginal("✅ Board updated. It may take some time for changes to apply everywhere.").queue();
            } case "challenge" -> {
                Member member = event.getOption("user", OptionMapping::getAsMember);
                if (member == null) {

                    List<Challenge> challenges = Challenge.getChallenges();

                    if (challenges.isEmpty()) {
                        event.getHook().setEphemeral(false).editOriginal("There are no challenges currently ongoing.").queue();
                        return;
                    }

                    EmbedBuilder embed = new EmbedBuilder();

                    embed.setTitle("Challenge List");

                    for (Challenge challenge : challenges) {
                        String attackerName = PlayerUtility.getUsernameOfUuid(challenge.attacker);
                        if (attackerName == null) attackerName = challenge.attacker.toString();
                        String defenderName = PlayerUtility.getUsernameOfUuid(challenge.defender);
                        if (defenderName == null) defenderName = challenge.defender.toString();

                        embed.addField(attackerName + " VS " + defenderName, "Status: " + challenge.state, false);
                    }

                    event.getHook().setEphemeral(false).editOriginalEmbeds(embed.build()).queue();
                } else {
                    UUID uuid = AccountLink.getUuid(member.getIdLong());
                    if (uuid == null) {
                        event.getHook().setEphemeral(false).editOriginal("That member does not have a linked Minecraft account.").queue();
                        return;
                    }

                    Challenge challenge = Challenge.getChallenge(uuid);
                    if (challenge == null) {
                        event.getHook().setEphemeral(false).editOriginal("That member is not currently organizing a challenge.").queue();
                        return;
                    }

                    EmbedBuilder embed = new EmbedBuilder();

                    UUID attackerUuid = challenge.attacker;
                    String attackerUsername = PlayerUtility.getUsernameOfUuid(attackerUuid);
                    if (attackerUsername == null) attackerUsername = attackerUuid.toString();

                    UUID defenderUuid = challenge.defender;
                    String defenderUsername = PlayerUtility.getUsernameOfUuid(defenderUuid);
                    if (defenderUsername == null) defenderUsername = defenderUuid.toString();

                    embed.setTitle("Challenge Status").addField("Attacker", attackerUsername, true).addField("Defender", defenderUsername, true);

                    if (!challenge.time.isEmpty()) {

                        StringBuilder timesDescription = new StringBuilder();
                        for (LocalDateTime time : challenge.time) {
                            timesDescription.append(time.format(DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a"))).append(" ");
                        }

                        embed.addField("Times", timesDescription.toString(), false);
                    } else embed.addField("Times", "None", false);

                    if (challenge.state == Challenge.State.PROPOSED) embed.setDescription(Challenge.State.PROPOSED + ": This challenge has been initiated, but not accepted by the defender.");
                    if (challenge.state == Challenge.State.ACCEPTED) embed.setDescription(Challenge.State.ACCEPTED + ": This challenge has been accepted by the defender and the times below have been suggested.");
                    if (challenge.state == Challenge.State.SCHEDULED) embed.setDescription(Challenge.State.SCHEDULED + ": The attacker and defender have agreed upon the time listed.");

                    Button danger = Button.danger("challenge-delete-" + challenge.attacker, "Delete");

                    event.getHook().setEphemeral(true).editOriginalEmbeds(embed.build()).setActionRow(danger).queue();
                }


            }
            default ->
                    event.getHook().setEphemeral(true).editOriginal("Invalid arguments! /royalty <set|clear|update>").queue();
        }


    }
}
