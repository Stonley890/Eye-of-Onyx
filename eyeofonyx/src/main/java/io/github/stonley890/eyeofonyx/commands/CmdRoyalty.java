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
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
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

    // Get server scoreboard service (for teams)
    private static final Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

    // Easy access royalty board

    // Team names
    private static final String[] teamNames = RoyaltyBoard.getTeamNames();

    // Tribe IDs
    private static final String[] tribes = RoyaltyBoard.getTribes();

    // Valid positions
    private static final String[] validPositions = RoyaltyBoard.getValidPositions();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

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

        } else if (args[0].equalsIgnoreCase("update")) {

            if (sender.hasPermission("eyeofonyx.manageboard")) {
                sender.sendMessage(EyeOfOnyx.EOO + "Reloading and updating the board...");
                RoyaltyBoard.reload();
                RoyaltyBoard.updateBoard();
                for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                    try {
                        RoyaltyBoard.updateDiscordBoard(i);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "Board updated.");
            } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else if (args[0].equals("manage")) {

            if (sender.hasPermission("eyeofonyx.manageboard")) manage(sender, args);
            else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid arguments! /royalty <set|list|clear|update|manage>");

        RoyaltyBoard.save(RoyaltyBoard.get());
        return true;
    }

    // royalty set <player> <position> [name]
    static void set(CommandSender sender, String[] args) {

        UUID targetPlayerUUID;

        // Get @p if specified
        if (args[1].equals("@p")) {
            Entity nearest = Bukkit.selectEntities(sender, args[1]).get(0);
            if (nearest instanceof Player player) {
                targetPlayerUUID = player.getUniqueId();
            } else {
                return;
            }
        } else {
            try {
                // Try to get online Player, otherwise lookup OfflinePlayer
                targetPlayerUUID = UUID.fromString(mojang.getUUIDOfUsername(args[1]).replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"));
            } catch (NullPointerException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Player not found.");
                return;
            }
        }

        // Check for ban
        if (Banned.isPlayerBanned(targetPlayerUUID.toString())) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This player must be unbanned first.");
            return;
        }

        // Get tribe from scoreboard team
        try {

            // Get team of player by iterating through list
            int playerTribe = PlayerTribe.getTribeOfPlayer(targetPlayerUUID.toString());

            // Check if third argument contains a valid position
            if (Arrays.stream(validPositions).anyMatch(args[2]::contains)) {

                int targetPos = -1;

                for (int i = 0; i < validPositions.length; i++) {
                    if (args[2].equals(validPositions[i])) targetPos = i;
                }

                // Set value in board.yml
                RoyaltyBoard.setValue(playerTribe, targetPos, "uuid", targetPlayerUUID.toString());
                RoyaltyBoard.setValue(playerTribe, targetPos, "last_online", LocalDateTime.now().toString());
                RoyaltyBoard.setValue(playerTribe, targetPos, "last_challenge_time", LocalDateTime.now().toString());
                RoyaltyBoard.setValue(playerTribe, targetPos, "joined_time", LocalDateTime.now().toString());
                RoyaltyBoard.setValue(playerTribe, targetPos, "challenger", "none");
                if (targetPos == RoyaltyBoard.RULER) RoyaltyBoard.setValue(playerTribe, targetPos, "challenging", "none");

                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + args[1] + " is now " + args[2].toUpperCase().replace('_', ' '));

                // If no name was provided, use username
                if (args.length == 3) {
                    RoyaltyBoard.setValue(playerTribe, targetPos, "name", args[1]);
                }

                // Canon name
                // if ruler do title, then name
                if (args[2].equals(validPositions[0]) && args.length > 3) {
                    RoyaltyBoard.setValue(playerTribe, targetPos, "title", args[3]);
                    RoyaltyBoard.setValue(playerTribe, targetPos, "name", args[4]);
                } else if (args.length > 3) RoyaltyBoard.setValue(playerTribe, targetPos, "name", args[3]);


                RoyaltyBoard.setValue(playerTribe, targetPos, "joined_time", LocalDateTime.now().toString());
                RoyaltyBoard.setValue(playerTribe, targetPos, "last_challenge_time", LocalDateTime.now().toString());
                RoyaltyBoard.setValue(playerTribe, targetPos, "last_online", LocalDateTime.now().toString());

                RoyaltyBoard.save(RoyaltyBoard.get());
                RoyaltyBoard.updateBoard();
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

    // royalty list [tribe]
    static void list(CommandSender sender, String[] args) {

        // If no other arguments, build and send full board
        if (args.length < 2) {
            // Init a StringBuilder to store message for building
            StringBuilder boardMessage = new StringBuilder();

            // Build for each tribe
            for (int i = 0; i < teamNames.length; i++) {
                boardMessage.append(buildBoard(i));
            }

            // Send built message
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage);

        } // If next argument is a tribe, send just that board
        else if (Arrays.stream(tribes).anyMatch(args[1]::contains)) {

            // Init a StringBuilder to store message for building
            StringBuilder boardMessage;

            // Find index of tribe and build
            boardMessage = buildBoard(Arrays.binarySearch(tribes, args[1]));

            // Send built message
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage.toString());

        } else {
            // Invalid argument
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid tribe name!");
        }

    }

    // royalty clear <tribe> <position>
    static void clear(CommandSender sender, String[] args) {

        String tribe = args[1];
        String pos = args[2];

        int tribeIndex = -1;
        int posIndex = -1;

        for (int i = 0; i < tribes.length; i++) {
            String vTribe = tribes[i];
            if (vTribe.equals(tribe)) {
                tribeIndex = i;
                break;
            }
        }

        if (tribeIndex == -1) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid tribe!");
            return;
        }

        for (int i = 0; i < validPositions.length; i++) {
            String vTribe = validPositions[i];
            if (vTribe.equals(pos)) {
                posIndex = i;
                break;
            }
        }

        if (posIndex == -1) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid position!");
            return;
        }

        String uuid = RoyaltyBoard.getUuid(tribeIndex, posIndex);
        if (uuid.equals("null")) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That position is already empty!");
            return;
        }

        try {

        // Notify attacker if exists
        String attacker = RoyaltyBoard.getAttacker(tribeIndex,posIndex);
        if (!attacker.equals("none")) {
            int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
            RoyaltyBoard.setAttacking(tribeIndex, attackerPos, "none");
            new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
        }

        // Notify defender if exists
        if (posIndex != RoyaltyBoard.RULER) {
            String attacking = RoyaltyBoard.getAttacking(tribeIndex,posIndex);
            if (!attacking.equals("none")) {
                int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                RoyaltyBoard.setAttacker(tribeIndex, defenderPos, "none");
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
            e.printStackTrace();
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }

        new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. And pending challenges have been canceled.", NotificationType.GENERIC).create();

        RoyaltyBoard.removePlayer(tribeIndex, posIndex);

        // If not ruler clear challenging
        if (!args[2].equals(validPositions[0])) {
            RoyaltyBoard.setValue(tribeIndex, posIndex, "challenging", "none");
        }

        RoyaltyBoard.save(RoyaltyBoard.get());
        RoyaltyBoard.updateBoard();
        try {
            RoyaltyBoard.updateDiscordBoard(tribeIndex);
        } catch (IOException e) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
        }
        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + args[1].toUpperCase() + " " + args[2].toUpperCase() + " position cleared.");

    }

    // royalty manage <tribe> <position> [key] [value]
    void manage(CommandSender sender, String[] args) {

        if (args.length < 3)
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /royalty manage <tribe> <position> [key] [value]");
        else {
            // Get tribe and position
            int tribeIndex = -1;
            int posIndex = -1;

            String[] tribes = RoyaltyBoard.getTribes();
            for (int i = 0; i < tribes.length; i++) if (args[1].equals(tribes[i])) tribeIndex = i;

            if (tribeIndex == -1) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid tribe!");
                return;
            }

            String[] positions = RoyaltyBoard.getValidPositions();
            for (int i = 0; i < positions.length; i++) if (args[2].equals(positions[i])) posIndex = i;

            if (posIndex == -1) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid position!");
                return;
            }

            if (args.length == 3) {
                // Print info about specified position
                // Get data
                String uuid = RoyaltyBoard.getUuid(tribeIndex, posIndex);
                String username = "N/A";
                String joinedTime = "N/A";
                String lastOnline = "N/A";
                String lastChallenge = "N/A";
                String challenger = "N/A";
                String challenging = "N/A";

                if (uuid != null && !uuid.equals("none")) {
                    username = mojang.getPlayerProfile(uuid).getUsername();
                    joinedTime = RoyaltyBoard.getJoinedDate(tribeIndex, posIndex);
                    lastOnline = RoyaltyBoard.getLastOnline(tribeIndex, posIndex);
                    lastChallenge = RoyaltyBoard.getLastChallengeDate(tribeIndex, posIndex);
                    challenger = RoyaltyBoard.getAttacker(tribeIndex, posIndex);
                    if (challenger == null || challenger.equals("none")) challenger = "N/A";
                    if (posIndex != RoyaltyBoard.RULER) {
                        challenging = RoyaltyBoard.getAttacking(tribeIndex, posIndex);
                        if (challenging.equals("none")) challenging = "N/A";
                    }
                }

                ZoneId offset = ZoneOffset.systemDefault();

                if (sender instanceof Player player) {
                    ZonedDateTime playerTime = IpUtils.ipToTime(player.getAddress().getAddress().getHostAddress());
                    if (playerTime != null) offset = playerTime.getZone();
                }

                ComponentBuilder builder = new ComponentBuilder(EyeOfOnyx.EOO);
                TextComponent button = new TextComponent();
                ZonedDateTime dateTime;
                ZonedDateTime offsetDateTime;
                builder.append("Data for ").append(tribes[tribeIndex].toUpperCase()).append(" ").append(validPositions[posIndex].toUpperCase())
                        .append("\n[ NAME: ").append(username).color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("\n[ UUID: ").append(uuid).color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append("\n[ DATE JOINED: ");

                if (!joinedTime.equals("N/A")) {
                    dateTime = ZonedDateTime.of(LocalDateTime.parse(joinedTime), ZoneOffset.systemDefault());
                    offsetDateTime = dateTime.withZoneSameLocal(offset);
                    button.setText("[" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")) + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " joined_time " + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                } else builder.append(joinedTime).color(net.md_5.bungee.api.ChatColor.YELLOW);

                builder.append("\n[ LAST ONLINE: ");

                if (!lastOnline.equals("N/A")) {
                    dateTime = ZonedDateTime.of(LocalDateTime.parse(lastOnline), ZoneOffset.systemDefault());
                    offsetDateTime = dateTime.withZoneSameLocal(offset);
                    button.setText("[" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")) + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " last_online " + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                } else builder.append(lastOnline).color(net.md_5.bungee.api.ChatColor.YELLOW);

                builder.append("\n[ LAST CHALLENGE: ");

                if (!lastChallenge.equals("N/A")) {
                    dateTime = ZonedDateTime.of(LocalDateTime.parse(lastChallenge), ZoneOffset.systemDefault());
                    offsetDateTime = dateTime.withZoneSameLocal(offset);
                    button.setText("[" + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a z")) + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " last_challenge " + offsetDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                } else builder.append(lastChallenge).color(net.md_5.bungee.api.ChatColor.YELLOW);

                builder.append("\n[ CHALLENGER: ");
                if (challenger.equals("N/A")) builder.append("N/A").color(net.md_5.bungee.api.ChatColor.YELLOW);
                else {
                    button.setText("[" + challenger + "]");
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " challenger "));
                    button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                    builder.append(button);
                }

                if (posIndex != RoyaltyBoard.RULER) {
                    builder.append("\n[ CHALLENGING: ");
                    if (challenging.equals("N/A")) builder.append("N/A").color(net.md_5.bungee.api.ChatColor.YELLOW);
                    else {
                        button.setText("[" + challenging + "]");
                        button.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/royalty manage " + args[1] + " " + args[2] + " challenging "));
                        button.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        builder.append(button);
                    }
                }
            } else if (args.length == 4) {
                // Print info for specified key
                // Get data
                String key = args[3];

                String[] keys = {"name", "last_online", "last_challenge_time", "challenger", "challenging"};
                if (Arrays.stream(keys).anyMatch(Predicate.isEqual(key))) {

                    String value = null;

                    switch (key) {
                        case "name" -> value = EyeOfOnyx.getPlugin().getConfig().getString("name");
                        case "last_online" -> {
                            String dateString = EyeOfOnyx.getPlugin().getConfig().getString("last_online");
                            if (dateString == null || dateString.equals("none")) value = "N/A";
                            else if (sender instanceof Player player) {
                                try {
                                    value = Utils.localTimeToPlayerTime(LocalDateTime.parse(dateString), player).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a z"));
                                } catch (NotFoundException e) {
                                    value = LocalDateTime.parse(dateString).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();;
                                }
                            }
                            else value = LocalDateTime.parse(dateString).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                        }
                        case "last_challenge_time" -> {
                            String dateString = EyeOfOnyx.getPlugin().getConfig().getString("last_challenge_time");
                            if (dateString == null || dateString.equals("none")) value = "N/A";
                            else if (sender instanceof Player player) {
                                try {
                                    value = Utils.localTimeToPlayerTime(LocalDateTime.parse(dateString), player).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a z"));
                                } catch (NotFoundException e) {
                                    value = LocalDateTime.parse(dateString).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();;
                                }
                            }
                            else value = LocalDateTime.parse(dateString).format(DateTimeFormatter.ofPattern("uuuu-MM-dd hh:mm a")) + " " + ZoneOffset.systemDefault().getId();
                        }
                        case "challenger" -> value = mojang.getPlayerProfile(EyeOfOnyx.getPlugin().getConfig().getString("challenger")).getUsername();
                        case "challenging" -> value = mojang.getPlayerProfile(EyeOfOnyx.getPlugin().getConfig().getString("challenging")).getUsername();
                    }


                    sender.sendMessage(EyeOfOnyx.EOO + "Value for " + teamNames[tribeIndex] + " " + validPositions[posIndex].replaceAll("_", " ") + " " + key.toUpperCase() + ": " + ChatColor.YELLOW + value);
                } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid key! /royalty manage <tribe> <position> [name|joined_time|last_online|last_challenge_time|challenger|challenging]");
            } else if (args.length == 5) {
                // Set info for specified key
                // Get data
                String key = args[3];
                String value = args[4];

                String[] keys = {"name", "joined_time", "last_challenge_time", "challenger", "challenging"};
                if (Arrays.stream(keys).anyMatch(Predicate.isEqual(key))) {

                    switch (key) {
                        case  "name" -> EyeOfOnyx.getPlugin().getConfig().set("name", value);
                        case "last_online" -> {
                            if (value.equals("now")) EyeOfOnyx.getPlugin().getConfig().set("last_online", LocalDateTime.now().toString());
                            else if (value.equals("never")) EyeOfOnyx.getPlugin().getConfig().set("last_online", "none");
                            else {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid arguments! /royalty manage <tribe> <position> last_online [now|never]");
                                return;
                            }
                        }
                        case "last_challenge_time" -> {
                            if (value.equals("now")) EyeOfOnyx.getPlugin().getConfig().set("last_challenge_time", LocalDateTime.now().toString());
                            else if (value.equals("never")) EyeOfOnyx.getPlugin().getConfig().set("last_challenge_time", "none");
                            else {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid arguments! /royalty manage <tribe> <position> last_online [now|never]");
                                return;
                            }
                        }
                        case  "challenger" -> {

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
                                EyeOfOnyx.getPlugin().getConfig().set("challenger", target.getUniqueId().toString());
                                value = target.getName();
                            }
                        }
                        case  "challenging" -> {

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
                                EyeOfOnyx.getPlugin().getConfig().set("challenging", target.getUniqueId().toString());
                                value = target.getName();
                            }
                        }
                    }

                    try {
                        RoyaltyBoard.updateDiscordBoard(tribeIndex);
                    } catch (IOException e) {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                        if (Dreamvisitor.debug) e.printStackTrace();
                    }
                    sender.sendMessage(EyeOfOnyx.EOO + "Set " + key.toUpperCase() + " for " + teamNames[tribeIndex] + " " + validPositions[posIndex].replaceAll("_", " ") + " to " + ChatColor.YELLOW + value);

                } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid key! /royalty manage <tribe> <position> [name|joined_time|last_online|last_challenge_time|challenger|challenging]");
            }
        }
    }

    /*
     * Used to build a message for `royalty list [tribe]`
     * Requires index of tribe
     * Returns a StringBuilder with the built message
     * This method uses Mojang API lookup to get player names and gets data from
     * royalty.yml
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
    static StringBuilder buildBoard(int index) {

        // Create string builder
        StringBuilder strBuild = new StringBuilder();
        // Add tribe name (line 1)
        strBuild.append("\n").append(ChatColor.GOLD).append(teamNames[index]).append(" ---").append(ChatColor.RESET);

        // For each position...
        for (int position = 0; position < validPositions.length; position++) {

            // Add the name of the position, change to uppercase, remove underscores
            strBuild.append(ChatColor.WHITE).append("\n[ ").append(validPositions[position].toUpperCase().replace('_', ' ')).append(": ");
            // If no one filling position, report as "none"
            if (Objects.equals(RoyaltyBoard.get().get(tribes[index] + "." + validPositions[position] + ".uuid"), "none")) {
                strBuild.append(ChatColor.GRAY).append("none");
            } // Otherwise...
            else {
                strBuild.append(ChatColor.YELLOW);
                // Add canon name w/ username in parentheses
                // Set name from OpenRP character
                if (EyeOfOnyx.openrp != null) {
                    String uuid = RoyaltyBoard.getUuid(index, position);
                    String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                    if (ocName != null && !ocName.equals("&c<No name set>")) {
                        strBuild.append(EyeOfOnyx.openrp.getDesc().getUserdata().getString(uuid + "." + EyeOfOnyx.getPlugin().getConfig().getString("character-name-field")));
                    } else {
                        strBuild.append(RoyaltyBoard.get().get(tribes[index] + "." + validPositions[position] + ".name"));
                    }
                } else {
                    strBuild.append(RoyaltyBoard.get().get(tribes[index] + "." + validPositions[position] + ".name"));
                }

                strBuild.append(ChatColor.WHITE).append(" (").append(mojang.getPlayerProfile(RoyaltyBoard.get().getString(tribes[index] + "." + validPositions[position] + ".uuid"))
                        .getUsername()).append(")");
            }
        }
        return strBuild;
    }

}
