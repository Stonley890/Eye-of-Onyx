package io.github.stonley890.eyeofonyx.discord.commands;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.*;
import io.github.stonley890.dreamvisitor.discord.commands.DiscordCommand;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.Utils;
import io.github.stonley890.eyeofonyx.discord.Discord;
import io.github.stonley890.eyeofonyx.files.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.TimeFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDateTime;
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
                                        Discord.posOption.setRequired(true)
                                ),
                        new SubcommandData("swap", "Swap two players on the royalty board.")
                                .addOptions(
                                        Discord.tribeOption.setRequired(true),
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
                                        Discord.tribeOption, Discord.posOption
                                ),
                        new SubcommandData("update", "Reload and force update the royalty board."),
                        new SubcommandData("challenge", "Manage challenges.")
                                .addOptions(
                                        new OptionData(OptionType.USER, "user", "The user whose challenge to fetch", false)
                                )
                )
                .addSubcommandGroups(
                        new SubcommandGroupData("read", "Read data directly from the the royalty board.")
                                .addSubcommands(
                                        new SubcommandData("name", "Display name.")
                                                .addOptions(Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true)),
                                        new SubcommandData("joined-board", "Date joined royalty board.")
                                                .addOptions(Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true)),
                                        new SubcommandData("joined-position", "Date joined current position.")
                                                .addOptions(Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true)),
                                        new SubcommandData("last-online", "Date last joined the server.")
                                                .addOptions(Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true)),
                                        new SubcommandData("last-challenge", "Date last completed a challenge.")
                                                .addOptions(Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true)),
                                        new SubcommandData("challenger", "The player attacking this player.")
                                                .addOptions(Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true)),
                                        new SubcommandData("challenging", "The player the this player is attacking.")
                                                .addOptions(Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true))
                                ),
                        new SubcommandGroupData("write", "Write data directly to the the royalty board.")
                                .addSubcommands(
                                        new SubcommandData("name", "Display name.")
                                                .addOptions(
                                                        Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true),
                                                        new OptionData(OptionType.STRING, "name", "The new name to set.", true)
                                                ),
                                        new SubcommandData("joined-board", "Date joined royalty board.")
                                                .addOptions(
                                                        Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true),
                                                        new OptionData(OptionType.INTEGER, "year", "The year to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "month", "The month to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "day", "The day to set as joined.", true)
                                                ),
                                        new SubcommandData("joined-position", "Date joined current position.")
                                                .addOptions(
                                                        Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true),
                                                        new OptionData(OptionType.INTEGER, "year", "The year to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "month", "The month to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "day", "The day to set as joined.", true)
                                                ),
                                        new SubcommandData("last-online", "Date last joined the server.")
                                                .addOptions(
                                                        Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true),
                                                        new OptionData(OptionType.INTEGER, "year", "The year to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "month", "The month to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "day", "The day to set as joined.", true)
                                                ),
                                        new SubcommandData("last-challenge", "Date of last challenge.")
                                                .addOptions(
                                                        Discord.tribeOption.setRequired(true), Discord.posOption.setRequired(true),
                                                        new OptionData(OptionType.INTEGER, "year", "The year to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "month", "The month to set as joined.", true),
                                                        new OptionData(OptionType.INTEGER, "day", "The day to set as joined.", true)
                                                ),
                                        new SubcommandData("challenger", "Clear the challenger value."),
                                        new SubcommandData("challenging", "Clear the challenging value.")
                                )
                )
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_ROLES));
    }

    @Override
    public void onCommand(@NotNull SlashCommandInteractionEvent event) {

        String subcommandGroup = event.getSubcommandGroup();
        String subCommand = event.getSubcommandName();

        if (subCommand == null) {
            event.reply("You must specify a subcommand!").setEphemeral(true).queue();
            return;
        }

        event.deferReply().setEphemeral(false).queue();

        if (subcommandGroup == null) {
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
                        Tribe playerTribe = PlayerTribe.getTribeOfPlayer(targetPlayerUUID);

                        if (playerTribe == null) {
                            event.getHook().setEphemeral(false).editOriginal("That player is not associated with a tribe.").queue();
                            return;
                        }

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

                            BoardPosition newPos = new BoardPosition(targetPlayerUUID, null, LocalDateTime.now(), LocalDateTime.now(),LocalDateTime.now(), LocalDateTime.now());

                            // Remove any player that might be there
                            RoyaltyBoard.removePlayer(playerTribe, targetPos, true);

                            // Set value in board.yml
                            RoyaltyBoard.set(playerTribe, targetPos, newPos);

                            // Log update
                            BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
                            RoyaltyBoard.reportChange(new RoyaltyAction(Objects.requireNonNull(event.getMember()).getId(), playerTribe, oldBoard, newBoard));

                            event.getHook().setEphemeral(false).editOriginal("✅ " + targetUser.getAsMention() + " is now " + position.toUpperCase().replace('_', ' ')).queue();

                            RoyaltyBoard.updateBoard(playerTribe, false);

                            RoyaltyBoard.updateDiscordBoard(playerTribe);

                        } else
                            event.getHook().setEphemeral(true).editOriginal("Invalid position. Valid positions: " + Arrays.toString(RoyaltyBoard.getValidPositions())).queue();

                    } catch (IllegalArgumentException e) {
                        // getTeam() throws IllegalArgumentException if teams do not exist
                        event.getHook().setEphemeral(false).editOriginal("Required teams do not exist!").queue();
                    }
                }
                case "clear" -> {

                    String tribeString;
                    String position;

                    tribeString = Objects.requireNonNull(event.getOption("tribe")).getAsString();
                    position = Objects.requireNonNull(event.getOption("position")).getAsString();

                    Tribe tribe = TribeUtil.parse(tribeString);

                    if (tribe == null) {
                        event.getHook().setEphemeral(true).editOriginal("Not a valid tribe!").queue();
                        return;
                    }

                    int posIndex = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position);

                    if (posIndex == -1) {
                        event.getHook().setEphemeral(true).editOriginal("Not a valid position!").queue();
                        return;
                    }

                    UUID uuid = RoyaltyBoard.getUuid(tribe, posIndex);
                    if (uuid == null) {
                        event.getHook().setEphemeral(false).editOriginal("That position is already empty!").queue();
                        return;
                    }

                    BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();

                    Challenge.removeChallengesOfPlayer(uuid, "The other player in your challenge was removed from the royalty board.");
                    Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_REQUESTED);
                    Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_ACCEPTED);

                    new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. All pending challenges have been canceled.", Notification.Type.GENERIC).create();

                    RoyaltyBoard.removePlayer(tribe, posIndex, true);

                    BoardState newBoard = RoyaltyBoard.getBoardOf(tribe).clone();
                    RoyaltyBoard.reportChange(new RoyaltyAction(Objects.requireNonNull(event.getMember()).getId(), tribe, oldBoard, newBoard));
                    RoyaltyBoard.updateBoard(tribe, false);
                    RoyaltyBoard.updateDiscordBoard(tribe);
                    event.getHook().setEphemeral(false).editOriginal("✅ " + tribe.getName() + " " + position.toUpperCase() + " position cleared.").queue();

                }
                case "swap" -> {

                    Dreamvisitor.debug("Swap command");

                    String tribeArg;
                    String position1;
                    String position2;

                    tribeArg = event.getOption("tribe", OptionMapping::getAsString);
                    position1 = event.getOption("position-1", OptionMapping::getAsString);
                    position2 = event.getOption("position-2", OptionMapping::getAsString);

                    if (Objects.equals(position1, position2)) {
                        event.getHook().setEphemeral(false).editOriginal("You cannot swap a position with itself!").queue();
                        return;
                    }

                    Tribe tribe = TribeUtil.parse(tribeArg);
                    int posIndex1 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position1);
                    int posIndex2 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position2);

                    if (tribe == null) {
                        event.getHook().setEphemeral(true).editOriginal("Not a valid tribe!").queue();
                        return;
                    }
                    if (posIndex1 == -1 || posIndex2 == -1) {
                        event.getHook().setEphemeral(true).editOriginal("Not a valid position!").queue();
                        return;
                    }

                    BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();

                    BoardPosition pos1 = RoyaltyBoard.getBoardOf(tribe).getPos(posIndex1);
                    BoardPosition pos2 = RoyaltyBoard.getBoardOf(tribe).getPos(posIndex2);

                    if (pos1.player == null || pos2.player == null) {
                        event.getHook().setEphemeral(false).editOriginal("You cannot swap with an empty position!").queue();
                        return;
                    }

                    // Remove challenges
                    Dreamvisitor.debug("Removing challenges");
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
                    RoyaltyBoard.set(tribe, RoyaltyBoard.getBoardOf(tribe).swap(posIndex1, posIndex2));

                    // Notify users
                    new Notification(pos1.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", Notification.Type.GENERIC).create();
                    new Notification(pos2.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", Notification.Type.GENERIC).create();

                    // Send update
                    BoardState newBoard = RoyaltyBoard.getBoardOf(tribe).clone();
                    RoyaltyBoard.reportChange(new RoyaltyAction(event.getUser().getId(), tribe, oldBoard, newBoard));

                    assert position1 != null;
                    assert position2 != null;
                    event.getHook().setEphemeral(false).editOriginal("Swapped " + position1.toUpperCase() + " and " + position2.toUpperCase() + " of " + tribe.getName()).queue();

                    RoyaltyBoard.updateBoard(tribe, false);
                    RoyaltyBoard.updateDiscordBoard(tribe);

                }
                case "update" -> {

                    for (int i = 0; i < TribeUtil.tribes.length; i++) {
                        Tribe tribe = TribeUtil.tribes[i];
                        RoyaltyBoard.updateBoard(tribe, false);
                        RoyaltyBoard.updateDiscordBoard(tribe);
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
                                timesDescription.append(Bot.createTimestamp(time, TimeFormat.DATE_TIME_SHORT)).append(" ");
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
                        event.getHook().setEphemeral(true).editOriginal("Invalid arguments!").queue();
            }
        } else if (subcommandGroup.equals("read")) {

            Tribe tribe = TribeUtil.parse(event.getOption("tribe", OptionMapping::getAsString));
            if (tribe == null) {
                event.getHook().setEphemeral(true).editOriginal("Tribe could not be parsed!").queue();
                return;
            }
            int pos = Utils.posIndexFromString(event.getOption("position", OptionMapping::getAsString));

            EmbedBuilder embed = new EmbedBuilder();

            String value;

            switch (subCommand) {
                case "name" -> value = RoyaltyBoard.getOcName(tribe, pos);
                case "joined-board" -> {
                    LocalDateTime joinedBoardDate = RoyaltyBoard.getJoinedBoardDate(tribe, pos);
                    if (joinedBoardDate == null) value = "None";
                    else value = Bot.createTimestamp(joinedBoardDate, TimeFormat.DATE_TIME_SHORT).toString();
                }
                case "joined-position" -> {
                    LocalDateTime joinedPosDate = RoyaltyBoard.getJoinedPosDate(tribe, pos);
                    if (joinedPosDate == null) value = "None";
                    else value = Bot.createTimestamp(joinedPosDate, TimeFormat.DATE_TIME_SHORT).toString();
                }
                case "last-online" -> {
                    LocalDateTime lastOnline = RoyaltyBoard.getLastOnline(tribe, pos);
                    if (lastOnline == null) value = "None";
                    else value = Bot.createTimestamp(lastOnline, TimeFormat.DATE_TIME_SHORT).toString();
                }
                case "last-challenge" -> {
                    LocalDateTime lastChallenge = RoyaltyBoard.getLastChallengeDate(tribe, pos);
                    if (lastChallenge == null) value = "None";
                    else value = Bot.createTimestamp(lastChallenge, TimeFormat.DATE_TIME_SHORT).toString();
                }
                default -> {
                    event.reply("Invalid request: " + subCommand).setEphemeral(true).queue();
                    return;
                }
            }
            embed.setDescription(subCommand + " for " + tribe.getName() + " " + RoyaltyBoard.getValidPositions()[pos] + " is " + value);
            event.getHook().editOriginalEmbeds(embed.build()).queue();
        } else if (subcommandGroup.equals("write")) {

            Tribe tribe = TribeUtil.parse(event.getOption("tribe", OptionMapping::getAsString));
            if (tribe == null) {
                event.getHook().setEphemeral(true).editOriginal("Tribe could not be parsed!").queue();
                return;
            }
            int pos = Utils.posIndexFromString(event.getOption("position", OptionMapping::getAsString));

            BoardState oldBoardState = RoyaltyBoard.getBoardOf(tribe).clone();

            switch (subCommand) {
                case "name" -> {
                    String name = event.getOption("name", OptionMapping::getAsString);
                    if (name == null) {
                        event.getHook().setEphemeral(true).editOriginal("Name cannot be null.").queue();
                        return;
                    }

                    RoyaltyBoard.setOcName(tribe, pos, name);
                    event.getHook().editOriginal("Set " + subCommand + " of " + tribe.getName() + " " + RoyaltyBoard.getValidPositions()[pos] + " to " + name).queue();
                }
                case "joined-board" -> {
                    LocalDateTime dateTime = getLocalDateTime(event);
                    if (dateTime == null) return;

                    RoyaltyBoard.setJoinedBoard(tribe, pos, dateTime);
                    event.getHook().editOriginal("Set " + subCommand + " of " + tribe.getName() + " " + RoyaltyBoard.getValidPositions()[pos] + " to " + dateTime).queue();
                }
                case "joined-position" -> {
                    LocalDateTime dateTime = getLocalDateTime(event);
                    if (dateTime == null) return;

                    RoyaltyBoard.setJoinedPosition(tribe, pos, dateTime);
                    event.getHook().editOriginal("Set " + subCommand + " of " + tribe.getName() + " " + RoyaltyBoard.getValidPositions()[pos] + " to " + dateTime).queue();
                }
                case "last-online" -> {
                    LocalDateTime dateTime = getLocalDateTime(event);
                    if (dateTime == null) return;

                    RoyaltyBoard.setLastOnline(tribe, pos, dateTime);
                    event.getHook().editOriginal("Set " + subCommand + " of " + tribe.getName() + " " + RoyaltyBoard.getValidPositions()[pos] + " to " + dateTime).queue();
                }
                case "last-challenge" -> {
                    LocalDateTime dateTime = getLocalDateTime(event);
                    if (dateTime == null) return;

                    RoyaltyBoard.setLastChallengeDate(tribe, pos, dateTime);
                    event.getHook().editOriginal("Set " + subCommand + " of " + tribe.getName() + " " + RoyaltyBoard.getValidPositions()[pos] + " to " + dateTime).queue();
                }
                default -> {
                    event.getHook().setEphemeral(true).editOriginal("Invalid request: " + subCommand).queue();
                    return;
                }
            }

            new RoyaltyAction(event.getUser().getAsMention(), tribe, oldBoardState, RoyaltyBoard.getBoardOf(tribe));
            RoyaltyBoard.updateBoard(tribe, true);
        }


    }

    private static @Nullable LocalDateTime getLocalDateTime(@NotNull SlashCommandInteractionEvent event) {
        Integer year = event.getOption("year", OptionMapping::getAsInt);
        if (year == null) {
            event.reply("Year cannot be null.").setEphemeral(true).queue();
            return null;
        } else if (year < 1970) {
            event.reply("Year must be greater than 1970!").setEphemeral(true).queue();
            return null;
        } else if (year > LocalDateTime.now().getYear()) {
            event.reply("Year must be less than " + LocalDateTime.now().getYear() + "!").setEphemeral(true).queue();
            return null;
        }

        Integer month = event.getOption("month", OptionMapping::getAsInt);
        if (month == null) {
            event.reply("Month cannot be null.").setEphemeral(true).queue();
            return null;
        } else if (month < 1) {
            event.reply("Month must be greater than 0!").setEphemeral(true).queue();
            return null;
        } else if (month > 12) {
            event.reply("Month must be less than 12!").setEphemeral(true).queue();
            return null;
        }

        Integer day = event.getOption("day", OptionMapping::getAsInt);
        if (day == null) {
            event.reply("Day cannot be null.").setEphemeral(true).queue();
            return null;
        } else if (day < 1) {
            event.reply("Day must be greater than 0!").setEphemeral(true).queue();
            return null;
        } else if (day > 31) {
            event.reply("Day must be less than 31!").setEphemeral(true).queue();
            return null;
        }

        LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.of(year, month, day, 12, 0);
        } catch (DateTimeException e) {
            event.reply("That date is invalid: " + e.getMessage()).setEphemeral(true).queue();
            return null;
        }
        return dateTime;
    }
}
