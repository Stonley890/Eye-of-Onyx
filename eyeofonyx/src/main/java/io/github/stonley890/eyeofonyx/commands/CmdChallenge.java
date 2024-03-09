package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.dreamvisitor.Main;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.md_5.bungee.api.chat.*;
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

    public static List<Player> playersOnForm = new ArrayList<>();
    public static List<String> codesOnForm = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
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

            } else if (playerPosition != CIVILIAN && RoyaltyBoard.isOnCoolDown(playerTribe, playerPosition)) {

                // Player is on cooldown. They cannot challenge
                ComponentBuilder builder = new ComponentBuilder();
                LocalDateTime challengeDate = RoyaltyBoard.getLastChallengeDate(playerTribe, playerPosition);
                assert challengeDate != null;
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
                if (playerPosition != CIVILIAN) position = positions[playerPosition];
                builder.append(position.replace('_', ' ')).color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append(" of the ").color(net.md_5.bungee.api.ChatColor.WHITE)
                        .append(teams[playerTribe]).color(net.md_5.bungee.api.ChatColor.YELLOW).append("s").color(net.md_5.bungee.api.ChatColor.YELLOW)
                        .append(".\nSelect a position:\n").color(net.md_5.bungee.api.ChatColor.WHITE);

                if /* Player is a civilian */ (playerPosition == CIVILIAN) {

                    // Check for any empty positions
                    int nextEmptyPosition = CIVILIAN;

                    // Iterate through positions (start at ruler)
                    for (int i = 0; i < positions.length; i++) {
                        nextEmptyPosition = i;
                        // If position is empty, break
                        if (RoyaltyBoard.isPositionEmpty(playerTribe, i)) {
                            break;
                        }
                        nextEmptyPosition = CIVILIAN;
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

                        builder.append(challengeEntry(playerTribe, NOBLE1, "position1"))
                                .append(challengeEntry(playerTribe, NOBLE2, "position2"))
                                .append(challengeEntry(playerTribe, NOBLE3, "position3"))
                                .append(challengeEntry(playerTribe, NOBLE4, "position4"))
                                .append(challengeEntry(playerTribe, NOBLE5, "position5"));

                    }
                } else if /* Player is a noble */ (playerPosition == NOBLE2 || playerPosition == NOBLE1) {

                    builder.append(challengeEntry(playerTribe, HEIR1, "position1"))
                            .append(challengeEntry(playerTribe, HEIR2, "position2"))
                            .append(challengeEntry(playerTribe, HEIR3, "position3"));

                } else if /* Player is an heir */ (playerPosition == HEIR2 || playerPosition == HEIR1) {

                    builder.append(challengeEntry(playerTribe, RULER, "position1"));

                } else if /* Player is ruler */ (playerPosition == RULER) {

                    builder.color(net.md_5.bungee.api.ChatColor.RED).append("You are ruler! No one to challenge.");

                }

                sender.spigot().sendMessage(builder.create());


            } else {

                switch (args[0]) {
                    case "position1", "position2", "position3", "position4", "position5" -> {

                        Main.debug("Initiating challenge");

                        ComponentBuilder builder = new ComponentBuilder();
                        builder.append(EyeOfOnyx.EOO);

                        int targetPosition;

                         Main.debug("Checking for validity");

                        // Check for active challenge
                        if (Competition.activeChallenge != null) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You cannot initiate a challenge while one is in progress!");
                            return true;
                        }

                        // Make sure challenge is valid
                        if (playerPosition == CIVILIAN) {

                            Main.debug("Player is civilian");

                            // Check for any empty positions
                            int nextEmptyPosition = CIVILIAN;

                            // Iterate through positions (start at ruler)
                            for (int posToCheck = 0; posToCheck < positions.length; posToCheck++) {
                                nextEmptyPosition = posToCheck;
                                Main.debug("Next empty position: " + posToCheck);
                                // If position is empty, break
                                if (getUuid(playerTribe, posToCheck) == null) {
                                    Main.debug("Position " + posToCheck + "is empty");
                                    break;
                                }
                                nextEmptyPosition = CIVILIAN;
                            }

                            // If a position is available, do that
                            if (nextEmptyPosition < CIVILIAN) {
                                Main.debug("Position available. Skipping challenge process.");

                                BoardState oldBoard = getBoardOf(playerTribe).clone();
                                RoyaltyBoard.set(playerTribe, nextEmptyPosition, new BoardPosition(player.getUniqueId(), null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null));
                                BoardState newBoard = getBoardOf(playerTribe).clone();
                                reportChange(new RoyaltyAction(player.getName(), playerTribe, oldBoard, newBoard));
                                updateBoard(playerTribe, false);
                                Main.debug("Board updated.");

                                sender.sendMessage(EyeOfOnyx.EOO + "You are now " + getValidPositions()[nextEmptyPosition].toUpperCase().replace("_", " ") + " of the " + getTeamNames()[playerTribe].toUpperCase() + "s!");
                                try {
                                    RoyaltyBoard.updateDiscordBoard(playerTribe);
                                } catch (IOException e) {
                                    Bukkit.getLogger().warning("Unable to update Discord board.");
                                }
                                return true;
                            }

                            Main.debug("No extra positions available.");

                            // Determine which position is being targeted
                            switch (args[0]) {
                                case "position1" -> targetPosition = NOBLE1;
                                case "position2" -> targetPosition = NOBLE2;
                                case "position3" -> targetPosition = NOBLE3;
                                case "position4" -> targetPosition = NOBLE4;
                                case "position5" -> targetPosition = NOBLE5;
                                default -> {
                                    builder.color(net.md_5.bungee.api.ChatColor.RED).append("Invalid arguments!");
                                    sender.spigot().sendMessage(builder.create());
                                    return true;
                                }
                            }

                        } else if (playerPosition >= NOBLE1 && playerPosition <= NOBLE5) {

                            // Determine which position is being targeted
                            switch (args[0]) {
                                case "position1" -> targetPosition = HEIR1;
                                case "position2" -> targetPosition = HEIR2;
                                case "position3" -> targetPosition = HEIR3;
                                default -> {
                                    builder.color(net.md_5.bungee.api.ChatColor.RED).append("Invalid arguments!");
                                    sender.spigot().sendMessage(builder.create());
                                    return true;
                                }
                            }


                        } else if (playerPosition >= HEIR1 && playerPosition <= HEIR3) {

                            targetPosition = RULER;

                        } else if (playerPosition == RULER) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are ruler! No one to challenge.");
                            return true;
                        } else {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Unaccounted situation! playerPosition " + playerPosition);
                            return true;
                        }

                        // Check that target is not being challenged
                        if (!(RoyaltyBoard.getAttacker(playerTribe, targetPosition) == null || (targetPosition != RULER && RoyaltyBoard.getAttacking(playerTribe, targetPosition) != null))) {

                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                            builder.append("That player is already in a challenge!");
                            sender.spigot().sendMessage(builder.create());
                            return true;

                        } else {

                            // Check for cooldown
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
                            // Unless position is empty...
                            if (!RoyaltyBoard.isPositionEmpty(playerTribe, targetPosition)) {

                                if (RoyaltyBoard.isOnCoolDown(playerTribe, targetPosition)) {
                                    builder.append("That player is on movement cooldown until ")
                                            .append(Objects.requireNonNull(getCooldownEnd(playerTribe, targetPosition)).format(DateTimeFormatter.ISO_DATE));
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
                            if (!oldBoard.equals(newBoard)) RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), playerTribe, oldBoard, newBoard));

                            // create notification for target
                            String title = "You've been challenged!";
                            String content = player.getName() + " has challenged your position for " + positions[targetPosition].replace('_', ' ') + ".";
                            new Notification(targetUuid, title, content, NotificationType.CHALLENGE_REQUESTED).create();

                            // create challenge
                            new Challenge(player.getUniqueId(), targetUuid, null, Challenge.State.PROPOSED).passiveSave();

                            assert targetUuid != null;
                            report(player.getName(), player.getName() + " initiated a challenge against " + PlayerUtility.getUsernameOfUuid(targetUuid) + ".");

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
                        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://" + EyeOfOnyx.getPlugin().getConfig().getString("address") + ":" + EyeOfOnyx.getPlugin().getConfig().getInt("port") + "/availability"));
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

                        Challenge playerChallenge = Challenge.getChallenge(player);

                        if (playerChallenge == null) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not in any scheduled challenges!");
                            return true;
                        }

                        if (playerChallenge.state == Challenge.State.SCHEDULED) {
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
                        playerChallenge.state = Challenge.State.SCHEDULED;

                        playerChallenge.passiveSave();

                        report(player.getName(), player.getName() + "'s challenge with " + PlayerUtility.getUsernameOfUuid(playerChallenge.defender) + " has been scheduled for " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")));

                        sender.sendMessage(EyeOfOnyx.EOO + "Time confirmed! Your challenge will take place " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + "!");
                        new Notification(playerChallenge.defender, "Challenge date confirmed!", "The time of your challenge with " + PlayerUtility.getUsernameOfUuid(playerChallenge.attacker.toString()) + " will be " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + ".", NotificationType.GENERIC).create();

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

    private static BaseComponent[] challengeEntry(int tribe, int pos, String command) {
        ComponentBuilder builder = new ComponentBuilder();

        TextComponent challengeButton = new TextComponent("[ Initiate Challenge ]");
        challengeButton.setUnderlined(true);
        challengeButton.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        challengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Initiate a challenge with this player for their royalty position.")));
        challengeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge " + command));

        TextComponent quickChallengeButton = new TextComponent("[ Quick Challenge ]");
        quickChallengeButton.setUnderlined(true);
        quickChallengeButton.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        quickChallengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Execute an immediate challenge with this player. They can deny this if they choose.")));
        quickChallengeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge " + command + " quick"));

        TextComponent disabledQuickChallengeButton = new TextComponent("[ Quick Challenge ]");
        disabledQuickChallengeButton.setUnderlined(false);
        disabledQuickChallengeButton.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        disabledQuickChallengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("This player is not online.")));

        builder.append("\n").reset().append(getValidPositions()[pos].toUpperCase().replace('_', ' '))
                .append(" ")
                .append(PlayerUtility.getUsernameOfUuid(Objects.requireNonNull(getUuid(tribe, pos)))).color(net.md_5.bungee.api.ChatColor.YELLOW)
                .append("\n");

        builder.append(challengeButton)
                .color(net.md_5.bungee.api.ChatColor.RESET)
                .append(" ").reset();

        if (Bukkit.getPlayer(Objects.requireNonNull(getUuid(tribe, pos))) == null) builder.append(disabledQuickChallengeButton);
        else builder.append(quickChallengeButton);

        return builder.append("\n").reset().create();
    }

}