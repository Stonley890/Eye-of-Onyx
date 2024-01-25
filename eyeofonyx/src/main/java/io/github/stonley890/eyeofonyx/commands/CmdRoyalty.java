package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.Utils;
import io.github.stonley890.eyeofonyx.files.*;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;

public class CmdRoyalty implements CommandExecutor {

    // Get Mojang services
    private static final Mojang mojang = new Mojang().connect();

    // Team names
    private static final String[] teamNames = RoyaltyBoard.getTeamNames();

    // Tribe IDs
    private static final String[] tribes = RoyaltyBoard.getTribes();

    // Valid positions
    private static final String[] validPositions = RoyaltyBoard.getValidPositions();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {

        sender.sendMessage(EyeOfOnyx.EOO + "Please wait...");

        if (args.length < 1) list(sender, args);
        else if (args[0].equalsIgnoreCase("set") && args.length > 2) {

            if (sender.hasPermission("eyeofonyx.manageboard")) set(sender, args);
            else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else if (args[0].equalsIgnoreCase("list")) {

            list(sender, args);

        } else if (args[0].equalsIgnoreCase("clear") && args.length > 2) {

            if (sender.hasPermission("eyeofonyx.manageboard")) clear(sender, args);
            else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else if (args[0].equalsIgnoreCase("swap") && args.length > 3) {

            if (sender.hasPermission("eyeofonyx.manageboard")) swap(sender, args);
            else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else if (args[0].equalsIgnoreCase("update")) {

            if (sender.hasPermission("eyeofonyx.manageboard")) {
                sender.sendMessage(EyeOfOnyx.EOO + "Reloading and updating the board...");
                RoyaltyBoard.loadFromDisk();

                for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                    try {
                        RoyaltyBoard.updateBoard(i, false);
                        RoyaltyBoard.updateDiscordBoard(i);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "Board updated. It may take some time for changes to apply completely.");
            } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else if (args[0].equals("manage")) {

            if (sender.hasPermission("eyeofonyx.manageboard")) manage(sender, args);
            else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid arguments! /royalty <set|list|clear|update|manage>");

        RoyaltyBoard.saveToDisk();
        return true;
    }

    // royalty set <player> <position> [name]
    public static void set(CommandSender sender, String @NotNull [] args) {

        UUID targetPlayerUUID;

        String target = args[1];
        String desiredPos = args[2];

        // Get @p if specified
        if (target.equals("@p")) {
            Entity nearest = Bukkit.selectEntities(sender, target).get(0);
            if (nearest instanceof Player player) {
                targetPlayerUUID = player.getUniqueId();
            } else {
                return;
            }
        } else {
            try {
                // Try to get online Player, otherwise lookup OfflinePlayer
                targetPlayerUUID = UUID.fromString(mojang.getUUIDOfUsername(target).replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"));
            } catch (NullPointerException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Player not found.");
                return;
            }
        }

        // Check for ban
        if (Banned.isPlayerBanned(targetPlayerUUID)) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This player must be unbanned first.");
            return;
        }

        // Get tribe
        try {

            // Get team of player by iterating through list
            int playerTribe = PlayerTribe.getTribeOfPlayer(targetPlayerUUID);

            // Make sure player is not already on the board
            if (RoyaltyBoard.getPositionIndexOfUUID(playerTribe, targetPlayerUUID) != 5) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is already on the royalty board!");
                return;
            }

            // Check if third argument contains a valid position
            if (Arrays.stream(validPositions).anyMatch(desiredPos::contains)) {

                BoardState oldBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();

                int targetPos = -1;

                for (int i = 0; i < validPositions.length; i++) {
                    if (desiredPos.equals(validPositions[i])) targetPos = i;
                }

                BoardPosition newPos = new BoardPosition(targetPlayerUUID, null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null);

                // Remove any player that might be there
                RoyaltyBoard.removePlayer(playerTribe, targetPos, true);

                // Set value in board.yml
                RoyaltyBoard.set(playerTribe, targetPos, newPos);

                // Log update
                BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
                RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), playerTribe, oldBoard, newBoard));

                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + target + " is now " + desiredPos.toUpperCase().replace('_', ' '));

                RoyaltyBoard.updateBoard(playerTribe, false);
                try {
                    RoyaltyBoard.updateDiscordBoard(playerTribe);
                } catch (IOException e) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                }

            } else
                sender.sendMessage(EyeOfOnyx.EOO +
                        ChatColor.RED + "Invalid position. Valid positions: " + Arrays.toString(validPositions));

        } catch (IllegalArgumentException e) {
            // getTeam() throws IllegalArgumentException if teams do not exist
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Required teams do not exist!");
        } catch (NotFoundException e) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Player is not associated with a tribe!");
        }


    }

    static void swap(CommandSender sender, String @NotNull [] args) {

        String tribe = args[1];
        String position1 = args[2];
        String position2 = args[3];

        if (position1.equals(position2)) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You cannot swap a position with itself!");
            return;
        }

        int tribeIndex = io.github.stonley890.eyeofonyx.Utils.tribeIndexFromString(tribe);
        int posIndex1 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position1);
        int posIndex2 = io.github.stonley890.eyeofonyx.Utils.posIndexFromString(position2);

        if (tribeIndex == -1) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid tribe!");
            return;
        }
        if (posIndex1 == -1 || posIndex2 == -1) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid position!");
            return;
        }

        BoardState oldBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();

        BoardPosition pos1 = RoyaltyBoard.getBoardOf(tribeIndex).getPos(posIndex1);
        BoardPosition pos2 = RoyaltyBoard.getBoardOf(tribeIndex).getPos(posIndex2);

        // Remove challenges
        Challenge.removeChallengesOfPlayer(pos1.player, "The player who was in your challenge was moved to a different position.");
        Challenge.removeChallengesOfPlayer(pos2.player, "The player who was in your challenge was moved to a different position.");

        Notification.removeNotificationsOfPlayer(pos1.player, NotificationType.CHALLENGE_ACCEPTED);
        Notification.removeNotificationsOfPlayer(pos1.player, NotificationType.CHALLENGE_REQUESTED);
        Notification.removeNotificationsOfPlayer(pos2.player, NotificationType.CHALLENGE_ACCEPTED);
        Notification.removeNotificationsOfPlayer(pos2.player, NotificationType.CHALLENGE_REQUESTED);

        // Apply change
        RoyaltyBoard.set(tribeIndex, RoyaltyBoard.getBoardOf(tribeIndex).swap(posIndex1, posIndex2));

        // Notify users
        new Notification(pos1.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", NotificationType.GENERIC).create();
        new Notification(pos2.player, "You've been moved!","You have been moved to a different spot on the royalty board. Any challenges you were in have been canceled.", NotificationType.GENERIC).create();

        // Send update
        BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
        RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribeIndex, oldBoard, newBoard));

        sender.sendMessage(EyeOfOnyx.EOO + "Swapped " + position1.toUpperCase() + " and " + position2.toUpperCase() + " of " + tribe.toUpperCase());

        RoyaltyBoard.updateBoard(tribeIndex, false);
        try {
            RoyaltyBoard.updateDiscordBoard(tribeIndex);
        } catch (IOException e) {
            Bukkit.getLogger().warning(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
        }
    }

    // royalty list [tribe]
    static void list(CommandSender sender, String @NotNull [] args) {

        // If no other arguments, build and send full board
        if (args.length < 2) {
            sender.sendMessage(EyeOfOnyx.EOO + "Please wait.");

            Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {
                // Init a StringBuilder to store message for building
                StringBuilder boardMessage = new StringBuilder();

                // Build for each tribe
                for (int i = 0; i < teamNames.length; i++) {
                    boardMessage.append(buildBoard(i));
                }

                // Send built message
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage);
            });

        } // If next argument is a tribe, send just that board
        else if (Arrays.stream(tribes).anyMatch(args[1]::contains)) {
            sender.sendMessage(EyeOfOnyx.EOO + "Please wait.");

            Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {
                // Init a StringBuilder to store message for building
                StringBuilder boardMessage;

                // Find index of tribe and build
                boardMessage = buildBoard(Arrays.binarySearch(tribes, args[1]));

                // Send built message
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage);
            });


        } else {
            // Invalid argument
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid tribe name!");
        }

    }

    // royalty clear <tribe> <position>
    static void clear(CommandSender sender, String @NotNull [] args) {

        Dreamvisitor.debug("Clearing...");

        String tribe = args[1];
        String pos = args[2];

        int tribeIndex = Utils.tribeIndexFromString(tribe);;
        int posIndex = Utils.posIndexFromString(pos);

        if (tribeIndex == -1) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid tribe!");
            return;
        }

        if (posIndex == -1) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid position!");
            return;
        }

        UUID uuid = RoyaltyBoard.getUuid(tribeIndex, posIndex);
        if (uuid == null) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That position is already empty!");
            return;
        }

        BoardState oldBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();

        Challenge.removeChallengesOfPlayer(uuid, "The other player in your challenge was removed from the royalty board.");
        Notification.removeNotificationsOfPlayer(uuid, NotificationType.CHALLENGE_REQUESTED);
        Notification.removeNotificationsOfPlayer(uuid, NotificationType.CHALLENGE_ACCEPTED);

        new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. All pending challenges have been canceled.", NotificationType.GENERIC).create();

        RoyaltyBoard.removePlayer(tribeIndex, posIndex, true);

        BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
        RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribeIndex, oldBoard, newBoard));
        RoyaltyBoard.updateBoard(tribeIndex, false);
        try {
            RoyaltyBoard.updateDiscordBoard(tribeIndex);
        } catch (IOException e) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
        }
        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + args[1].toUpperCase() + " " + args[2].toUpperCase() + " position cleared.");

    }

    // royalty manage <tribe> <position> [key] [value]
    void manage(CommandSender sender, String @NotNull [] args) {

        if (args.length < 3)
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /royalty manage <tribe> <position> [key] [value]");
        else {
            // Get tribe and position
            int tribeIndex = Utils.tribeIndexFromString(args[1]);
            int posIndex = Utils.posIndexFromString(args[2]);

            if (tribeIndex == -1) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid tribe!");
                return;
            }

            if (posIndex == -1) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid position!");
                return;
            }

            if (args.length == 3) {
                // Print info about specified position
                // Get data
                UUID uuid = RoyaltyBoard.getUuid(tribeIndex, posIndex);
                String username = "N/A";
                String name = "N/A";
                String joinedPos = "N/A";
                String joinedBoard = "N/A";
                String lastOnline = "N/A";
                String lastChallenge = "N/A";
                String challenger = "N/A";
                String challenging = "N/A";

                if (uuid != null) {
                    username = io.github.stonley890.dreamvisitor.Utils.getUsernameOfUuid(uuid);
                    name = RoyaltyBoard.getOcName(tribeIndex, posIndex);
                    joinedPos = RoyaltyBoard.getJoinedPosDate(tribeIndex, posIndex).toString();
                    joinedBoard = RoyaltyBoard.getJoinedBoardDate(tribeIndex, posIndex).toString();
                    lastOnline = RoyaltyBoard.getLastOnline(tribeIndex, posIndex).toString();
                    lastChallenge = RoyaltyBoard.getLastChallengeDate(tribeIndex, posIndex).toString();
                    challenger = String.valueOf(RoyaltyBoard.getAttacker(tribeIndex, posIndex));
                    if (challenger == null || challenger.equals("null")) challenger = "N/A";
                    if (posIndex != RoyaltyBoard.RULER) {
                        challenging = String.valueOf(RoyaltyBoard.getAttacking(tribeIndex, posIndex));
                        if (challenging == null || challenging.equals("null")) challenging = "N/A";
                    }
                }

                ZoneId offset = ZoneOffset.systemDefault();

                if (sender instanceof Player player) {
                    ZonedDateTime playerTime = IpUtils.ipToTime(player.getAddress().getAddress().getHostAddress());
                    if (playerTime != null) offset = playerTime.getZone();
                }

                String stringUuid;
                if (uuid == null) stringUuid = "N/A";
                else stringUuid = uuid.toString();

                ComponentBuilder builder = new ComponentBuilder(EyeOfOnyx.EOO);
                TextComponent button = new TextComponent();
                ZonedDateTime dateTime;
                ZonedDateTime offsetDateTime;
                builder.append("Data for ").append(tribes[tribeIndex].toUpperCase()).append(" ").append(validPositions[posIndex].toUpperCase())
                        .append("\n[ USERNAME: ").append(username).color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("\n[ NAME: ").color(net.md_5.bungee.api.ChatColor.WHITE).append(name).color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("\n[ UUID: ").color(net.md_5.bungee.api.ChatColor.WHITE).append(stringUuid).color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("\n[ DATE JOINED POSITION: ").color(net.md_5.bungee.api.ChatColor.WHITE);

                if (!joinedPos.equals("N/A")) {
                    dateTime = ZonedDateTime.of(LocalDateTime.parse(joinedPos), ZoneOffset.systemDefault());
                    offsetDateTime = dateTime.withZoneSameLocal(offset);
                    button.setText("[" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")) + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " joined_time " + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                } else builder.append(joinedPos).color(net.md_5.bungee.api.ChatColor.YELLOW);

                builder.append("\n[ DATE JOINED BOARD: ").color(net.md_5.bungee.api.ChatColor.WHITE);

                if (!joinedBoard.equals("N/A")) {
                    dateTime = ZonedDateTime.of(LocalDateTime.parse(joinedBoard), ZoneOffset.systemDefault());
                    offsetDateTime = dateTime.withZoneSameLocal(offset);
                    button.setText("[" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")) + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " joined_time " + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                } else builder.append(joinedBoard).color(net.md_5.bungee.api.ChatColor.YELLOW);

                builder.append("\n[ LAST ONLINE: ").color(net.md_5.bungee.api.ChatColor.WHITE);

                if (!lastOnline.equals("N/A")) {
                    dateTime = ZonedDateTime.of(LocalDateTime.parse(lastOnline), ZoneOffset.systemDefault());
                    offsetDateTime = dateTime.withZoneSameLocal(offset);
                    button.setText("[" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")) + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " last_online " + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                } else builder.append(lastOnline).color(net.md_5.bungee.api.ChatColor.YELLOW);

                builder.append("\n[ LAST CHALLENGE: ").color(net.md_5.bungee.api.ChatColor.WHITE);

                if (!lastChallenge.equals("N/A")) {
                    dateTime = ZonedDateTime.of(LocalDateTime.parse(lastChallenge), ZoneOffset.systemDefault());
                    offsetDateTime = dateTime.withZoneSameLocal(offset);
                    button.setText("[" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")) + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " last_challenge " + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                } else builder.append(lastChallenge).color(net.md_5.bungee.api.ChatColor.YELLOW);

                builder.append("\n[ CHALLENGER: ").color(net.md_5.bungee.api.ChatColor.WHITE);
                if (challenger.equals("N/A")) builder.append("N/A").color(net.md_5.bungee.api.ChatColor.YELLOW);
                else {
                    button.setText("[" + challenger + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " challenger "));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                }

                if (posIndex != RoyaltyBoard.RULER) {
                    builder.append("\n[ CHALLENGING: ").color(net.md_5.bungee.api.ChatColor.WHITE);
                    if (challenging.equals("N/A")) builder.append("N/A").color(net.md_5.bungee.api.ChatColor.YELLOW);
                    else {
                        button.setText("[" + challenging + "]");
                        button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " challenging "));
                        button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        builder.append(button);
                    }
                }

                sender.spigot().sendMessage(builder.create());

            } else if (args.length == 4) {
                // Print info for specified key
                // Get data
                String key = args[3];

                String[] keys = {"name", "joined_board", "joined_position", "last_online", "last_challenge_time", "challenger", "challenging"};
                if (Arrays.stream(keys).anyMatch(Predicate.isEqual(key))) {

                    String value = null;

                    switch (key) {
                        case "name" -> value = RoyaltyBoard.getOcName(tribeIndex, posIndex);
                        case "joined_position" -> {
                            LocalDateTime joinedPos = RoyaltyBoard.getJoinedPosDate(tribeIndex, posIndex);

                            if (joinedPos == null) value = "N/A";
                            else if (sender instanceof Player player) {
                                try {
                                    // try to use player timezone
                                    value = Utils.localTimeToPlayerTime(joinedPos, player).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a z"));
                                } catch (NotFoundException e) {
                                    // if not available, use system time zone
                                    value = joinedPos.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                                }
                            } else
                                // if not player use system time zone
                                value = joinedPos.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                        }
                        case "joined_board" -> {
                            LocalDateTime joinedBoard = RoyaltyBoard.getJoinedBoardDate(tribeIndex, posIndex);

                            if (joinedBoard == null) value = "N/A";
                            else if (sender instanceof Player player) {
                                try {
                                    // try to use player timezone
                                    value = Utils.localTimeToPlayerTime(joinedBoard, player).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a z"));
                                } catch (NotFoundException e) {
                                    // if not available, use system time zone
                                    value = joinedBoard.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                                }
                            } else
                                // if not player use system time zone
                                value = joinedBoard.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                        }
                        case "last_online" -> {
                            LocalDateTime lastOnline = RoyaltyBoard.getLastOnline(tribeIndex, posIndex);

                            if (lastOnline == null) value = "N/A";
                            else if (sender instanceof Player player) {
                                try {
                                    // try to use player timezone
                                    value = Utils.localTimeToPlayerTime(lastOnline, player).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a z"));
                                } catch (NotFoundException e) {
                                    // if not available, use system time zone
                                    value = lastOnline.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                                }
                            } else
                                // if not player use system time zone
                                value = lastOnline.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                        }
                        case "last_challenge_time" -> {
                            LocalDateTime lastChallenge = RoyaltyBoard.getLastChallengeDate(tribeIndex, posIndex);

                            if (lastChallenge == null) value = "N/A";
                            else if (sender instanceof Player player) {
                                try {
                                    // try to use player timezone
                                    value = Utils.localTimeToPlayerTime(lastChallenge, player).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a z"));
                                } catch (NotFoundException e) {
                                    // if not available, use system time zone
                                    value = lastChallenge.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                                }
                            } else
                                // if not player use system time zone
                                value = lastChallenge.format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                        }
                        case "challenger" -> {
                            value = "N/A";
                            UUID challengerUuid = RoyaltyBoard.getAttacker(tribeIndex, posIndex);
                            if (challengerUuid != null) {
                                value = io.github.stonley890.dreamvisitor.Utils.getUsernameOfUuid(challengerUuid);
                            }
                        }

                        case "challenging" -> {
                            value = "N/A";
                            UUID challengingUuid = RoyaltyBoard.getAttacking(tribeIndex, posIndex);
                            if (challengingUuid != null) {
                                value = io.github.stonley890.dreamvisitor.Utils.getUsernameOfUuid(challengingUuid);
                            }
                        }
                    }


                    sender.sendMessage(EyeOfOnyx.EOO + "Value for " + teamNames[tribeIndex] + " " + validPositions[posIndex].replaceAll("_", " ") + " " + key.toUpperCase() + ": " + ChatColor.YELLOW + value);
                } else
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid key! /royalty manage <tribe> <position> [name|joined_time|last_online|last_challenge_time|challenger|challenging]");
            } else if (args.length == 5) {

                BoardState oldBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();

                // Set info for specified key
                // Get data
                String key = args[3];
                String value = args[4];

                String[] keys = {"name", "joined_board", "joined_position", "last_challenge_time", "challenger", "challenging"};
                if (Arrays.stream(keys).anyMatch(Predicate.isEqual(key))) {

                    switch (key) {
                        case "name" -> EyeOfOnyx.getPlugin().getConfig().set("name", value);
                        case "last_online" -> {
                            if (value.equals("now"))
                                RoyaltyBoard.setLastOnline(tribeIndex, posIndex, LocalDateTime.now());
                            else if (value.equals("never"))
                                RoyaltyBoard.setLastOnline(tribeIndex, posIndex, null);
                            else {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid arguments! /royalty manage <tribe> <position> last_online [now|never]");
                                return;
                            }
                        }
                        case "last_challenge_time" -> {
                            if (value.equals("now"))
                                RoyaltyBoard.setLastChallengeDate(tribeIndex, posIndex, LocalDateTime.now());
                            else if (value.equals("never"))
                                RoyaltyBoard.setLastChallengeDate(tribeIndex, posIndex, null);
                            else {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid arguments! /royalty manage <tribe> <position> last_challenge_time [now|never]");
                                return;
                            }
                        }
                        case "challenger" -> {

                            List<Player> targets = new ArrayList<>();

                            // Use vanilla target selector args
                            List<Entity> entities;
                            try {
                                entities = Bukkit.selectEntities(sender, value);
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /updateplayer <targets>");
                                return;
                            }

                            // Check if empty
                            if (entities.isEmpty()) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "No players were selected.");
                                return;
                            }

                            // Check for non-players
                            for (Entity entity : entities) {
                                if (entity instanceof Player player) {
                                    targets.add(player);
                                } else {
                                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This command is only applicable to players.");
                                    return;
                                }
                            }

                            if (targets.size() > 1) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Too many targets! Only one player may be selected.");
                                return;
                            }

                            for (Player target : targets) {
                                // Success
                                RoyaltyBoard.setAttacker(tribeIndex, posIndex, target.getUniqueId());
                                value = target.getName();
                            }
                        }
                        case "challenging" -> {

                            if (posIndex == RoyaltyBoard.RULER) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You cannot set this value for a ruler!");
                                return;
                            }

                            List<Player> targets = new ArrayList<>();

                            // Use vanilla target selector args
                            List<Entity> entities;
                            try {
                                entities = Bukkit.selectEntities(sender, value);
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /updateplayer <targets>");
                                return;
                            }

                            // Check if empty
                            if (entities.isEmpty()) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "No players were selected.");
                                return;
                            }

                            // Check for non-players
                            for (Entity entity : entities) {
                                if (entity instanceof Player player) {
                                    targets.add(player);
                                } else {
                                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This command is only applicable to players.");
                                    return;
                                }
                            }

                            if (targets.size() > 1) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Too many targets! Only one player may be selected.");
                                return;
                            }

                            for (Player target : targets) {
                                // Success
                                RoyaltyBoard.setAttacking(tribeIndex, posIndex, target.getUniqueId());
                                value = target.getName();
                            }
                        }
                    }

                    BoardState newBoard = RoyaltyBoard.getBoardOf(tribeIndex).clone();
                    if (!Objects.equals(oldBoard, newBoard)) RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribeIndex, oldBoard, newBoard));

                    try {
                        RoyaltyBoard.updateDiscordBoard(tribeIndex);
                    } catch (IOException e) {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                        if (Dreamvisitor.debug) e.printStackTrace();
                    }
                    sender.sendMessage(EyeOfOnyx.EOO + "Set " + key.toUpperCase() + " for " + teamNames[tribeIndex] + " " + validPositions[posIndex].replaceAll("_", " ") + " to " + ChatColor.YELLOW + value);

                } else
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid key! /royalty manage <tribe> <position> [name|joined_time|last_online|last_challenge_time|challenger|challenging]");
            }
        }
    }

    /*
     * Used to build a message for `royalty list [tribe]`.
     * Requires tribe of tribe.
     * Returns a StringBuilder with the built message.
     * This method uses Mojang API lookup to get player names, and fetches data from
     * royalty.yml.
     * Here is what the result looks like:
     *
     * 1 TeamName ---
     * 2 [ RULER: Title Name (Username)
     * 3 [ HEIR APPARENT: Name (Username)
     * 4 [ HEIR PRESUMPTIVE: Name (Username)
     * 5 [ NOBLE APPARENT: Name (Username)
     * 6 [ NOBLE PRESUMPTIVE: Name (Username)
     *
     */
    static @NotNull StringBuilder buildBoard(int tribe) {

        // Create string builder
        StringBuilder strBuild = new StringBuilder();
        // Add tribe name (line 1)
        strBuild.append("\n").append(ChatColor.GOLD).append(teamNames[tribe]).append(" ---").append(ChatColor.RESET);

        // For each position...
        for (int position = 0; position < validPositions.length; position++) {

            // Add the name of the position, change to uppercase, remove underscores
            strBuild.append(ChatColor.WHITE).append("\n[ ").append(validPositions[position].toUpperCase().replace('_', ' ')).append(": ");
            // If no one filling position, report as "none"
            if (RoyaltyBoard.isPositionEmpty(tribe, position)) {
                strBuild.append(ChatColor.GRAY).append("none");
            } // Otherwise...
            else {
                strBuild.append(ChatColor.YELLOW);
                // Add canon name w/ username in parentheses
                // Set name from OpenRP character
                if (EyeOfOnyx.openrp != null) {
                    UUID uuid = RoyaltyBoard.getUuid(tribe, position);
                    String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                    if (ocName != null && !ocName.equals("&c<No name set>")) {
                        strBuild.append(EyeOfOnyx.openrp.getDesc().getUserdata().getString(uuid + "." + EyeOfOnyx.getPlugin().getConfig().getString("character-name-field")));
                    } else {
                        strBuild.append(RoyaltyBoard.getOcName(tribe, position));
                    }
                } else {
                    strBuild.append(RoyaltyBoard.getOcName(tribe, position));
                }

                strBuild.append(ChatColor.WHITE).append(" (").append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(tribe, position).toString())
                        .getUsername()).append(")");
            }
        }
        return strBuild;
    }

}
