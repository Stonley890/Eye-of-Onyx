package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.dreamvisitor.data.AltFamily;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.github.stonley890.eyeofonyx.files.RoyaltyBoard.*;

public class CmdChallenge implements CommandExecutor {

    String[] teams = RoyaltyBoard.getTeamNames();
    String[] positions = RoyaltyBoard.getValidPositions();

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

            // Check if at least 12 hours play time
            if (player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60 / 60 < 12) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You must have at least twelve hours of play time before you can participate in royalty.");
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
            long discordId;
            try {
                discordId = AccountLink.getDiscordId(player.getUniqueId());

            } catch (NullPointerException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have a linked Discord account! Contact a staff member for help.");
                return true;
            }

            Bot.getGameLogChannel().getGuild().retrieveMemberById(discordId).queue(member -> Bukkit.getScheduler().runTask(EyeOfOnyx.getPlugin(), () -> {
                // Check if alt account
                if (AltFamily.getParent(member.getIdLong()) != member.getIdLong()) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Your account is marked as an alt. Please contact staff if this is incorrect.");
                }

                int playerPosition = RoyaltyBoard.getPositionIndexOfUUID(playerTribe, player.getUniqueId());

                if (Banned.isPlayerBanned(player.getUniqueId())) {
                    // Player is banned from royalty board
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not allowed to initiate a challenge!");
                    return;
                }

                Challenge challenge = Challenge.getChallenge(player);
                if (challenge != null) {
                    sendChallengeDetails(player, challenge);
                }

                if (playerPosition != CIVILIAN && RoyaltyBoard.isOnCoolDown(playerTribe, playerPosition)) {

                    // Player is on cooldown. They cannot challenge
                    ComponentBuilder builder = new ComponentBuilder();
                    LocalDateTime challengeDate = RoyaltyBoard.getLastChallengeDate(playerTribe, playerPosition);
                    assert challengeDate != null;
                    challengeDate = challengeDate.plusDays(EyeOfOnyx.getPlugin().getConfig().getInt("challenge-cool-down"));

                    builder.append(EyeOfOnyx.EOO)
                            .color(ChatColor.RED)
                            .append("You are on movement cooldown until ")
                            .append(challengeDate.format(DateTimeFormatter.ISO_DATE));

                    sender.spigot().sendMessage(builder.create());

                    return;

                } else if (playerPosition != CIVILIAN && playerPosition != RULER && RoyaltyBoard.getAttacking(playerTribe, playerPosition) != null) {

                    if (args.length == 0 || (!args[0].equals("date") && !args[0].equals("start"))) {
                        // Player has already initiated a challenge
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You have already initiated a challenge!");
                        return;
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
                    builder.append(position.replace('_', ' ')).color(ChatColor.YELLOW)
                            .append(" of the ").color(ChatColor.WHITE)
                            .append(teams[playerTribe]).color(ChatColor.YELLOW).append("s").color(ChatColor.YELLOW)
                            .append(".\nSelect a position:\n").color(ChatColor.WHITE);

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

                            builder.append(positions[nextEmptyPosition].toUpperCase().replace('_', ' '))
                                    .append("\nPosition Available\n \n");

                            TextComponent button = new TextComponent("[ Assume Position ]");
                            button.setUnderlined(true);
                            button.setColor(ChatColor.GREEN);
                            button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                            builder.append(button);

                        } else {
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

                        builder.color(ChatColor.RED).append("You are ruler! No one to challenge.");

                    }

                    sender.spigot().sendMessage(builder.create());

                } else {

                    switch (args[0]) {
                        case "position1", "position2", "position3", "position4", "position5" -> initiate(sender, args, player, playerPosition, playerTribe);
                        case "accept" -> accept(sender, player);
                        case "deny" -> deny(sender);
                        case "denyconfirm" -> denyconfirm(sender, player, playerTribe, playerPosition);
                        case "start" -> start(sender, player);
                        case "date" -> date(sender, args, player, challenge);
                        case "cancel" -> {
                            if (challenge != null) {
                                if (challenge.attacker.equals(player.getUniqueId())) challenge.cancelAttacker();
                                if (challenge.defender.equals(player.getUniqueId())) challenge.cancelDefender();
                                sender.sendMessage(EyeOfOnyx.EOO + "Cancel request sent.");
                            }
                        }
                        default -> sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! Use the text GUI with /challenge");
                    }
                }
            }));
        } else {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That command is only available for players!");
        }

        return true;
    }

    private static void date(@NotNull CommandSender sender, @NotNull String @NotNull [] args, Player player, Challenge challenge) {
        if (challenge == null) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not in any scheduled challenges!");
            return;
        }

        if (challenge.state == Challenge.State.SCHEDULED) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This challenge is already finalized!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments!");
            return;
        }

        int selectedTimeIndex;

        try {
            selectedTimeIndex = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments!");
            return;
        }

        Challenge.remove(challenge);

        LocalDateTime selectedTime = challenge.time.get(selectedTimeIndex);
        challenge.time.clear();
        challenge.time.add(selectedTime);
        challenge.state = Challenge.State.SCHEDULED;

        challenge.save();

        report(player.getName(), player.getName() + "'s challenge with " + PlayerUtility.getUsernameOfUuid(challenge.defender) + " has been scheduled for " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")));

        sender.sendMessage(EyeOfOnyx.EOO + "Time confirmed! Your challenge will take place " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + "!");
        new Notification(challenge.defender, "Challenge date confirmed!", "The time of your challenge with " + PlayerUtility.getUsernameOfUuid(challenge.attacker.toString()) + " will be " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + ".", Notification.Type.GENERIC).create();

        // Remove CHALLENGE_ACCEPTED notification
        for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId())) {
            if (notification.type == Notification.Type.CHALLENGE_ACCEPTED)
                Notification.removeNotification(notification);
        }
    }

    private static void start(@NotNull CommandSender sender, @NotNull Player player) {
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

    private static void denyconfirm(@NotNull CommandSender sender, @NotNull Player player, int playerTribe, int playerPosition) {
        // Remove notification
        List<Notification> notificationList = Notification.getNotificationsOfPlayer(player.getUniqueId());
        for (Notification notification : notificationList) {
            if (notification.type == Notification.Type.CHALLENGE_REQUESTED)
                Notification.removeNotification(notification);
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

    private static void deny(@NotNull CommandSender sender) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO).append("Are you sure you want to forfeit your role and remove yourself from the royalty board? ")
                .append("This action cannot be undone.").color(ChatColor.RED).append("\n");

        // Add accept and forfeit buttons
        TextComponent accept = new TextComponent();
        accept.setText("[Accept Challenge]");
        accept.setColor(ChatColor.GREEN);
        accept.setUnderlined(true);
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge accept"));

        TextComponent deny = new TextComponent();
        deny.setText("[Forfeit]");
        deny.setColor(ChatColor.RED);
        deny.setUnderlined(true);
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge denyconfirm"));

        builder.append(accept).append(" ").reset().append(deny).append("\n");

        sender.spigot().sendMessage(builder.create());
    }

    private static void accept(@NotNull CommandSender sender, Player player) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO).append("One more thing! Use the link below to select times that you are available to attend the challenge. You'll need the code below too.").append("\n");

        TextComponent link = new TextComponent("[Submit Availability]");
        link.setColor(ChatColor.YELLOW);
        link.setUnderlined(true);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://" + EyeOfOnyx.getPlugin().getConfig().getString("address") + ":" + EyeOfOnyx.getPlugin().getConfig().getInt("port") + "/availability"));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open the web form.")));

        String id = String.format("%04d", new Random().nextInt(10000));

        TextComponent code = new TextComponent("[" + id + "]");
        code.setColor(ChatColor.YELLOW);
        code.setUnderlined(true);
        code.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id));
        code.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy the code.")));

        builder.append(link).append(" ").reset().append(code).append("\n").reset()
                .append("The code will expire in five minutes.").color(ChatColor.RED).append("\n");

        playersOnForm.add(player);
        codesOnForm.add(id);

        sender.spigot().sendMessage(builder.create());
    }

    private void initiate(@NotNull CommandSender sender, @NotNull String @NotNull [] args, Player player, int playerPosition, int playerTribe) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO);

        int targetPosition;

        // Check for active challenge
        if (Competition.activeChallenge != null) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You cannot initiate a challenge while one is in progress!");
            return;
        }

        // Make sure challenge is valid
        if (playerPosition == CIVILIAN) {

            // Check for any empty positions
            int nextEmptyPosition = CIVILIAN;

            // Iterate through positions (start at ruler)
            for (int posToCheck = 0; posToCheck < positions.length; posToCheck++) {
                nextEmptyPosition = posToCheck;
                // If position is empty, break
                if (getUuid(playerTribe, posToCheck) == null) break;
                nextEmptyPosition = CIVILIAN;
            }

            // If a position is available, do that
            if (nextEmptyPosition < CIVILIAN) {
                BoardState oldBoard = getBoardOf(playerTribe).clone();
                RoyaltyBoard.set(playerTribe, nextEmptyPosition, new BoardPosition(player.getUniqueId(), null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), null, null));
                BoardState newBoard = getBoardOf(playerTribe).clone();
                reportChange(new RoyaltyAction(player.getName(), playerTribe, oldBoard, newBoard));
                updateBoard(playerTribe, false);

                sender.sendMessage(EyeOfOnyx.EOO + "You are now " + getValidPositions()[nextEmptyPosition].toUpperCase().replace("_", " ") + " of the " + getTeamNames()[playerTribe].toUpperCase() + "s!");
                try {
                    RoyaltyBoard.updateDiscordBoard(playerTribe);
                } catch (IOException e) {
                    Bukkit.getLogger().warning("Unable to update Discord board. Message format file cannot be read.");
                }
                return;
            }

            Dreamvisitor.debug("No extra positions available.");

            // Determine which position is being targeted
            switch (args[0]) {
                case "position1" -> targetPosition = NOBLE1;
                case "position2" -> targetPosition = NOBLE2;
                case "position3" -> targetPosition = NOBLE3;
                case "position4" -> targetPosition = NOBLE4;
                case "position5" -> targetPosition = NOBLE5;
                default -> {
                    builder.color(ChatColor.RED).append("Invalid arguments!");
                    sender.spigot().sendMessage(builder.create());
                    return;
                }
            }

        } else if (playerPosition >= NOBLE1 && playerPosition <= NOBLE5) {

            // Determine which position is being targeted
            switch (args[0]) {
                case "position1" -> targetPosition = HEIR1;
                case "position2" -> targetPosition = HEIR2;
                case "position3" -> targetPosition = HEIR3;
                default -> {
                    builder.color(ChatColor.RED).append("Invalid arguments!");
                    sender.spigot().sendMessage(builder.create());
                    return;
                }
            }

        } else if (playerPosition >= HEIR1 && playerPosition <= HEIR3) {
            targetPosition = RULER;
        } else if (playerPosition == RULER) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are ruler! No one to challenge.");
            return;
        } else {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Unaccounted situation! playerPosition " + playerPosition);
            return;
        }

        // Check that target is not being challenged
        if (!(RoyaltyBoard.getAttacker(playerTribe, targetPosition) == null || (targetPosition != RULER && RoyaltyBoard.getAttacking(playerTribe, targetPosition) != null))) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            builder.append("That player is already in a challenge!");
            sender.spigot().sendMessage(builder.create());
        } else {

            // Check for cooldown
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 1);
            // Unless position is empty...
            if (!RoyaltyBoard.isPositionEmpty(playerTribe, targetPosition)) {

                if (RoyaltyBoard.isOnCoolDown(playerTribe, targetPosition)) {
                    builder.append("That player is on movement cooldown until ")
                            .append(Objects.requireNonNull(getCooldownEnd(playerTribe, targetPosition)).format(DateTimeFormatter.ISO_DATE));
                    sender.spigot().sendMessage(builder.create());
                    return;
                }
            }

            UUID targetUuid = RoyaltyBoard.getUuid(playerTribe, targetPosition);

            BoardState oldBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();

            // set values in board.yml
            RoyaltyBoard.setAttacker(playerTribe, targetPosition, player.getUniqueId());
            if (playerPosition != 5) RoyaltyBoard.setAttacking(playerTribe, playerPosition, targetUuid);

            BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
            if (!oldBoard.equals(newBoard)) RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), playerTribe, oldBoard, newBoard));

            // create notification for target
            String title = "You've been challenged!";
            String content = player.getName() + " has challenged your position for " + positions[targetPosition].replace('_', ' ') + ".";
            new Notification(targetUuid, title, content, Notification.Type.CHALLENGE_REQUESTED).create();

            // create challenge
            new Challenge(player.getUniqueId(), targetUuid, null, Challenge.State.PROPOSED).save();

            assert targetUuid != null;
            report(player.getName(), player.getName() + " initiated a challenge against " + PlayerUtility.getUsernameOfUuid(targetUuid) + ".");

            builder.append("Challenge initiated!");
            sender.spigot().sendMessage(builder.create());
        }
    }

    private static void sendChallengeDetails(@NotNull Player player, @NotNull Challenge challenge) {
        boolean attacker = true;
        java.util.UUID opponentUuid = challenge.attacker;
        if (opponentUuid.equals(player.getUniqueId())) {
            opponentUuid = challenge.defender;
            attacker = false;
        }
        String opponentName = PlayerUtility.getUsernameOfUuid(opponentUuid);

        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO).append("You are currently challenging ").append(opponentName).append(".\n");

        builder.append("[").color(ChatColor.WHITE).append("✓").color(ChatColor.GREEN).append("] Proposed\n[").color(ChatColor.WHITE);
        if (challenge.isAccepted()) builder.append("✓").color(ChatColor.GREEN);
        else builder.append("-").color(ChatColor.RED);
        builder.append("] Accepted\n[").color(ChatColor.WHITE);
        if (challenge.isScheduled()) builder.append("✓").color(ChatColor.GREEN);
        else builder.append("-").color(ChatColor.RED);
        builder.append("] Scheduled\n[").color(ChatColor.WHITE);
        if (challenge.isActive()) builder.append("✓").color(ChatColor.GREEN);
        else builder.append("-").color(ChatColor.RED);
        builder.append("] Active\n[").color(ChatColor.WHITE)
                .append("Cancel");
        if ((attacker && !challenge.isAttackerCanceled()) || (!attacker && !challenge.isDefenderCanceled())) builder.underlined(true).color(ChatColor.RED).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("If both you and your opponent agree, the challenge will be canceled."))).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge cancel"));
        else builder.color(ChatColor.GRAY).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("You already pressed cancel.")));
        builder.append("]").color(ChatColor.WHITE).underlined(false);
    }

    private static BaseComponent[] challengeEntry(int tribe, int pos, String command) {
        ComponentBuilder builder = new ComponentBuilder();

        TextComponent challengeButton = new TextComponent("[ Initiate Challenge ]");
        challengeButton.setUnderlined(true);
        challengeButton.setColor(ChatColor.GREEN);
        challengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Initiate a challenge with this player for their royalty position.")));
        challengeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge " + command));

        TextComponent quickChallengeButton = new TextComponent("[ Quick Challenge ]");
        quickChallengeButton.setUnderlined(true);
        quickChallengeButton.setColor(ChatColor.YELLOW);
        quickChallengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Execute an immediate challenge with this player. They can deny this if they choose.")));
        quickChallengeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge " + command + " quick"));

        TextComponent disabledQuickChallengeButton = new TextComponent("[ Quick Challenge ]");
        disabledQuickChallengeButton.setUnderlined(false);
        disabledQuickChallengeButton.setColor(ChatColor.GRAY);
        disabledQuickChallengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("This player is not online.")));

        builder.append("\n").reset().append(getValidPositions()[pos].toUpperCase().replace('_', ' '))
                .append(" ")
                .append(PlayerUtility.getUsernameOfUuid(Objects.requireNonNull(getUuid(tribe, pos)))).color(ChatColor.YELLOW)
                .append("\n");

        builder.append(challengeButton)
                .color(ChatColor.RESET)
                .append(" ").reset();

        if (Bukkit.getPlayer(Objects.requireNonNull(getUuid(tribe, pos))) == null) builder.append(disabledQuickChallengeButton);
        else builder.append(quickChallengeButton);

        return builder.append("\n").reset().create();
    }

}