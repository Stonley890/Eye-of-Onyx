package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static io.github.stonley890.eyeofonyx.files.RoyaltyBoard.*;

public class CmdChallenge implements CommandExecutor {

    String[] teams = RoyaltyBoard.getTeamNames();
    String[] positions = RoyaltyBoard.getValidPositions();

    Mojang mojang = new Mojang().connect();

    public static List<Player> playersOnForm = new ArrayList<>();
    public static List<String> codesOnForm = new ArrayList<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {

            sender.sendMessage(EyeOfOnyx.EOO + "Please wait...");

            // Check if board is frozen
            if (RoyaltyBoard.isFrozen()) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "The royalty board is currently frozen.");
                return true;
            }

            // Ensure player is part of a team
            int playerTribe;
            try {
                playerTribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
            } catch (NotFoundException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not part of a team!");
                return true;
            }

            // Ensure player has a linked Discord account
            try {
                AccountLink.getDiscordId(player.getUniqueId());
            } catch (NullPointerException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have a linked Discord account! Contact a staff member for help.");
                return true;
            }

            int playerPosition = RoyaltyBoard.getPositionIndexOfUUID(playerTribe, player.getUniqueId());

            if (Banned.isPlayerBanned(player.getUniqueId())) {

                // Player is banned from royalty board
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not allowed to initiate a challenge!");

                return true;

            } else if (playerPosition != 5 && RoyaltyBoard.isOnCoolDown(playerTribe, playerPosition)) {

                // Player is on cooldown. They cannot challenge
                ComponentBuilder builder = new ComponentBuilder();
                LocalDateTime challengeDate = RoyaltyBoard.getLastChallengeDate(playerTribe, playerPosition);
                challengeDate = challengeDate.plusDays(EyeOfOnyx.getPlugin().getConfig().getInt("challenge-cool-down"));

                builder.append(EyeOfOnyx.EOO)
                        .color(net.md_5.bungee.api.ChatColor.RED)
                        .append("You are on movement cooldown until ")
                        .append(challengeDate.format(DateTimeFormatter.ISO_DATE));

                sender.spigot().sendMessage(builder.create());

                return true;

            } else if (playerPosition != CIVILIAN && playerPosition != RULER && RoyaltyBoard.getAttacking(playerTribe, playerPosition) != null) {

                if (args.length == 0 || (!args[0].equals("date") && !args[0].equals("start"))) {
                    // Player has already initiated a challenge
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You have already initiated a challenge!");
                    return true;
                }
            }
            if (args.length == 0) {

                ComponentBuilder builder = new ComponentBuilder();

                /*

                CHALLENGE MENU
                You are currently $position$ of the $playerTeam$s.
                Select a position:

                 */

                builder.append(EyeOfOnyx.EOO + "CHALLENGE MENU")
                        .append("\nYou are currently ");
                String position = "CIVILIAN";
                if (playerPosition != 5) position = positions[playerPosition];
                builder.append(position.replace('_', ' ')).color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append(" of the ").color(net.md_5.bungee.api.ChatColor.WHITE)
                        .append(teams[playerTribe]).color(net.md_5.bungee.api.ChatColor.YELLOW).append("s").color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append(".\nSelect a position:\n \n").color(net.md_5.bungee.api.ChatColor.WHITE);

                if /* Player is a civilian */ (playerPosition == CIVILIAN) {

                    // Check for any empty positions
                    int nextEmptyPosition = CIVILIAN;

                    // Iterate through positions (start at ruler)
                    for (int i = 0; i < positions.length; i++) {
                        nextEmptyPosition = i;
                        // If position is empty, break
                        if (RoyaltyBoard.getUuid(playerTribe, i) == null) {
                            break;
                        }
                    }

                    // If a position is available, offer
                    if (nextEmptyPosition < CIVILIAN) {

                        /*

                        NEXTEMPTYPOSITION
                        Position Available

                        [ Assume Position ]

                         */

                        builder.append(positions[nextEmptyPosition].toUpperCase().replace('_', ' '))
                                .append("\nPosition Available\n \n");

                        TextComponent button = new TextComponent("[ Assume Position ]");
                        button.setUnderlined(true);
                        button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                        builder.append(button);

                    } else {

                        /*

                        $POSITION$ $Username$
                        [ Initiate Challenge ]

                         */

                        builder.append(positions[NOBLE_PRESUMPTIVE].toUpperCase().replace('_', ' '))
                                .append(" ")
                                .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, NOBLE_PRESUMPTIVE).toString()).getUsername()).color(net.md_5.bungee.api.ChatColor.YELLOW)
                                .append("\n");

                        TextComponent button = new TextComponent("[ Initiate Challenge ]");
                        button.setUnderlined(true);
                        button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                        builder.append(button)
                                .color(net.md_5.bungee.api.ChatColor.RESET)
                                .append("\n \n")
                                .append(positions[NOBLE_APPARENT].toUpperCase().replace('_', ' '))
                                .append("\n")
                                .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, NOBLE_APPARENT).toString()).getUsername())
                                .append(" ");

                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position2"));

                        builder.append(button);

                    }
                } else if /* Player is a noble */ (playerPosition == NOBLE_PRESUMPTIVE || playerPosition == NOBLE_APPARENT) {

                    builder.append(positions[HEIR_PRESUMPTIVE].toUpperCase().replace('_', ' '))
                            .append(" ")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, HEIR_PRESUMPTIVE).toString()).getUsername()).color(net.md_5.bungee.api.ChatColor.YELLOW)
                            .append("\n");

                    TextComponent button = new TextComponent("[ Initiate Challenge ]");
                    button.setUnderlined(true);
                    button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                    builder.append(button)
                            .append("\n \n")
                            .append(positions[HEIR_APPARENT].toUpperCase().replace('_', ' '))
                            .append("\n")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, HEIR_APPARENT).toString()).getUsername())
                            .append(" ");

                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position2"));

                    builder.append(button)
                            .color(net.md_5.bungee.api.ChatColor.RESET);

                } else if /* Player is an heir */ (playerPosition == HEIR_PRESUMPTIVE || playerPosition == HEIR_APPARENT) {

                    builder.append(positions[RULER].toUpperCase().replace('_', ' '))
                            .append(" ")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, RULER).toString()).getUsername()).color(net.md_5.bungee.api.ChatColor.YELLOW)
                            .append("\n");

                    TextComponent button = new TextComponent("[Initiate Challenge]");
                    button.setUnderlined(true);
                    button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                    builder.append(button);

                } else if /* Player is ruler */ (playerPosition == RULER) {

                    builder.color(net.md_5.bungee.api.ChatColor.RED).append("You are ruler! No one to challenge.");

                }

                sender.spigot().sendMessage(builder.create());


            } else {

                switch (args[0]) {
                    case "position1", "position2" -> {

                        Dreamvisitor.debug("Initiating challenge");

                        ComponentBuilder builder = new ComponentBuilder();
                        builder.append(EyeOfOnyx.EOO);

                        int targetPosition;

                        Dreamvisitor.debug("Checking for validity");

                        // Check for active challenge
                        if (Competition.activeChallenge != null) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You cannot initiate a challenge while one is in progress!");
                            return true;
                        }

                        // Make sure challenge is valid
                        if (playerPosition == CIVILIAN) {

                            Dreamvisitor.debug("Player is civilian");

                            // Check for any empty positions
                            int nextEmptyPosition = CIVILIAN;

                            // Iterate through positions (start at ruler)
                            for (int i = 0; i < positions.length; i++) {
                                nextEmptyPosition = i;
                                Dreamvisitor.debug("Next empty position: " + i);
                                // If position is empty, break
                                if (getUuid(playerTribe, i) == null) {
                                    Dreamvisitor.debug("Position " + i + "is empty");
                                    break;
                                }
                            }

                            // If a position is available, do that
                            if (nextEmptyPosition < CIVILIAN) {
                                Dreamvisitor.debug("Position available. Skipping challenge process.");

                                BoardState oldBoard = getBoardOf(playerTribe).clone();
                                RoyaltyBoard.set(playerTribe, nextEmptyPosition, new BoardPosition(player.getUniqueId(), null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null));
                                BoardState newBoard = getBoardOf(playerTribe).clone();
                                reportChange(new RoyaltyAction(player.getName(), playerTribe, oldBoard, newBoard));
                                updateBoard(playerTribe, false);
                                Dreamvisitor.debug("Board updated.");

                                sender.sendMessage(EyeOfOnyx.EOO + "You are now " + getValidPositions()[nextEmptyPosition].toUpperCase().replace("_", " ") + " of the " + getTeamNames()[playerTribe].toUpperCase() + "s!");
                                try {
                                    RoyaltyBoard.updateDiscordBoard(playerTribe);
                                } catch (IOException e) {
                                    Bukkit.getLogger().warning("Unable to update Discord board.");
                                }
                                return true;
                            }

                            Dreamvisitor.debug("No extra positions available.");

                            // Determine which position is being targeted
                            if (args[0].equals("position1")) {
                                targetPosition = NOBLE_PRESUMPTIVE;
                            } else if (args[0].equals("positionn2")) {
                                targetPosition = NOBLE_APPARENT;
                            } else {
                                builder.color(net.md_5.bungee.api.ChatColor.RED).append("Invalid arguments!");
                                sender.spigot().sendMessage(builder.create());
                                return true;
                            }

                        } else if (playerPosition == NOBLE_PRESUMPTIVE || playerPosition == NOBLE_APPARENT) {

                            // Determine which position is being targeted
                            if (args[0].equals("position1")) {
                                targetPosition = HEIR_PRESUMPTIVE;
                            } else if (args[0].equals("positionn2")) {
                                targetPosition = HEIR_APPARENT;
                            } else {
                                builder.color(net.md_5.bungee.api.ChatColor.RED).append("Invalid arguments!");
                                sender.spigot().sendMessage(builder.create());
                                return true;
                            }


                        } else if (playerPosition == HEIR_PRESUMPTIVE || playerPosition == HEIR_APPARENT) {

                            // Determine which position is being targeted
                            targetPosition = RULER;

                        } else if (playerPosition == RULER) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are ruler! No one to challenge.");
                            return true;
                        } else {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Unaccounted situation! playerPosition " + playerPosition);
                            return true;
                        }

                        // Check that target is not being challenged
                        if (!(RoyaltyBoard.getAttacker(playerTribe, targetPosition) == null || (targetPosition != 0 && RoyaltyBoard.getAttacking(playerTribe, targetPosition) != null))) {

                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                            builder.append("That player is already in a challenge!");
                            sender.spigot().sendMessage(builder.create());
                            return true;

                        } else {

                            // Check for cooldown
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                            // Unless position is empty...
                            if (!RoyaltyBoard.isPositionEmpty(playerTribe, targetPosition)) {

                                // Get last challenge
                                LocalDateTime targetChallenge = RoyaltyBoard.getLastChallengeDate(playerTribe, targetPosition);
                                // Compare with cooldown time
                                targetChallenge = targetChallenge.plusDays(EyeOfOnyx.getPlugin().getConfig().getInt("challenge-cool-down"));
                                if (RoyaltyBoard.isOnCoolDown(playerTribe, targetPosition)) {
                                    builder.append("That player is on movement cooldown until ")
                                            .append(targetChallenge.format(DateTimeFormatter.ISO_DATE));

                                    sender.spigot().sendMessage(builder.create());

                                    return true;
                                }
                            }

                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                            java.util.UUID targetUuid = RoyaltyBoard.getUuid(playerTribe, targetPosition);

                            BoardState oldBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();

                            // set values in board.yml
                            RoyaltyBoard.setAttacker(playerTribe, targetPosition, player.getUniqueId());
                            if (playerPosition != 5) RoyaltyBoard.setAttacking(playerTribe, playerPosition, targetUuid);

                            BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
                            RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), playerTribe, oldBoard, newBoard));

                            // create notification for target
                            String title = "You've been challenged!";
                            String content = player.getName() + " has challenged your position for " + positions[targetPosition].replace('_', ' ') + ".";
                            new Notification(targetUuid, title, content, NotificationType.CHALLENGE_REQUESTED).create();

                            builder.append("Challenge initiated!");
                            sender.spigot().sendMessage(builder.create());
                        }


                    }
                    case "accept" -> {

                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                        ComponentBuilder builder = new ComponentBuilder();
                        builder.append(EyeOfOnyx.EOO).append("One more thing! Use the link below to select times that you are available to attend the challenge. You'll need the code below too.").append("\n");

                        TextComponent link = new TextComponent("[Submit Availability]");
                        link.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        link.setUnderlined(true);
                        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://" + EyeOfOnyx.getPlugin().getConfig().getString("address") + ":8000/availability"));
                        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open the web form.")));

                        String id = String.format("%04d", new Random().nextInt(10000));

                        TextComponent code = new TextComponent("[" + id + "]");
                        code.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        code.setUnderlined(true);
                        code.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id));
                        code.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy the code.")));

                        builder.append(link).append(" ").reset().append(code).append("\n").reset()
                                .append("The code will expire in five minutes.").color(net.md_5.bungee.api.ChatColor.RED).append("\n");

                        playersOnForm.add(player);
                        codesOnForm.add(id);

                        sender.spigot().sendMessage(builder.create());

                        return true;

                    }
                    case "deny" -> {

                        /*
                        [EoO] Are you sure you want to forfeit your role and remove yourself from the royalty board? This action cannot be undone.
                        [Accept Challenge] [Forfeit]

                         */

                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                        ComponentBuilder builder = new ComponentBuilder();
                        builder.append(EyeOfOnyx.EOO).append("Are you sure you want to forfeit your role and remove yourself from the royalty board? ")
                                .append("This action cannot be undone.").color(net.md_5.bungee.api.ChatColor.RED).append("\n");

                        // Add accept and forfeit buttons
                        TextComponent accept = new TextComponent();
                        accept.setText("[Accept Challenge]");
                        accept.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                        accept.setUnderlined(true);
                        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge accept"));

                        TextComponent deny = new TextComponent();
                        deny.setText("[Forfeit]");
                        deny.setColor(net.md_5.bungee.api.ChatColor.RED);
                        deny.setUnderlined(true);
                        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge denyconfirm"));

                        builder.append(accept).append(" ").reset().append(deny).append("\n");

                        sender.spigot().sendMessage(builder.create());

                    }
                    case "denyconfirm" -> {

                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);

                        // Remove notification
                        try {
                            List<Notification> notificationList = Notification.getNotificationsOfPlayer(player.getUniqueId());
                            for (Notification notification : notificationList) {
                                if (notification.type == NotificationType.CHALLENGE_REQUESTED)
                                    Notification.removeNotification(notification);
                            }
                        } catch (IOException | InvalidConfigurationException e) {
                            Bukkit.getLogger().warning("Eye of Onyx could not read from disk!");
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Error: Eye of Onyx could not read from disk!");
                            return true;
                        }

                        // Remove from royalty board
                        BoardState oldBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
                        RoyaltyBoard.removePlayer(playerTribe, playerPosition, true);
                        BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
                        reportChange(new RoyaltyAction(sender.getName(), playerTribe, oldBoard, newBoard));
                        RoyaltyBoard.updateBoard(playerTribe, false);
                        sender.sendMessage(EyeOfOnyx.EOO + "You have been removed from the royalty board.");

                        try {
                            RoyaltyBoard.updateDiscordBoard(playerTribe);
                        } catch (IOException e) {
                            Bukkit.getLogger().warning("Unable to update Discord board.");
                        }
                    }
                    case "start" -> {

                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                        if ((Competition.activeChallenge.defender.equals(player.getUniqueId()) || Competition.activeChallenge.attacker.equals(player.getUniqueId())) && !Competition.activeChallenge.started) {

                            List<Location> waitingRooms = (List<Location>) EyeOfOnyx.getPlugin().getConfig().getList("waiting-rooms");
                            if (waitingRooms != null) {
                                if (waitingRooms.get(Competition.activeChallenge.tribe) != null) {
                                    player.teleport(waitingRooms.get(Competition.activeChallenge.tribe));
                                } else
                                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "No waiting room set! Contact an admin.");
                            } else
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "No waiting room set! Contact an admin.");
                        }
                    }
                    case "date" -> {

                        Challenge playerChallenge = getChallenge(player);

                        if (playerChallenge == null) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not in any scheduled challenges!");
                            return true;
                        }

                        if (playerChallenge.finalized) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This challenge is already finalized!");
                            return true;
                        }

                        if (args.length < 2) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments!");
                            return true;
                        }

                        int selectedTimeIndex;

                        try {
                            selectedTimeIndex = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments!");
                            return true;
                        }

                        try {
                            Challenge.remove(playerChallenge);
                        } catch (IOException | InvalidConfigurationException e) {
                            throw new RuntimeException(e);
                        }

                        LocalDateTime selectedTime = playerChallenge.time.get(selectedTimeIndex);
                        playerChallenge.time.clear();
                        playerChallenge.time.add(selectedTime);
                        playerChallenge.finalized = true;

                        playerChallenge.passiveSave();

                        sender.sendMessage(EyeOfOnyx.EOO + "Time confirmed! Your challenge will take place " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + "!");
                        new Notification(playerChallenge.defender, "Challenge date confirmed!", "The time of your challenge with " + mojang.getPlayerProfile(playerChallenge.attacker.toString()).getUsername() + " will be " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + ".", NotificationType.GENERIC).create();

                        // Remove CHALLENGE_ACCEPTED notification
                        try {
                            for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId())) {
                                if (notification.type == NotificationType.CHALLENGE_ACCEPTED)
                                    Notification.removeNotification(notification);
                            }
                        } catch (IOException | InvalidConfigurationException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    default ->
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! Use the text GUI with /challenge");
                }
            }
        } else {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That command is only available for players!");
        }

        return true;
    }

    @Nullable
    private static Challenge getChallenge(Player player) {
        Challenge playerChallenge = null;

        try {
            for (Challenge challenge : Challenge.getChallenges()) {
                if (Objects.equals(challenge.attacker, player.getUniqueId())) {
                    playerChallenge = challenge;
                    break;
                }
            }
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
        return playerChallenge;
    }

}