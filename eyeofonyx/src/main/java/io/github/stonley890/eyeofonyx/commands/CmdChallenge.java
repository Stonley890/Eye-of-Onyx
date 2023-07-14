package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.Challenge;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.Banned;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.NotificationType;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.github.stonley890.eyeofonyx.files.RoyaltyBoard.*;

public class CmdChallenge implements CommandExecutor {

    int attackingTribe;
    int attackingPosition;

    String[] tribes = RoyaltyBoard.getTribes();
    String[] teams = RoyaltyBoard.getTeamNames();
    String[] positions = RoyaltyBoard.getValidPositions();

    Mojang mojang = new Mojang().connect();

    FileConfiguration board = RoyaltyBoard.get();
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {

            sender.sendMessage(EyeOfOnyx.EOO + "Please wait...");

            int playerTribe;
            try {
                playerTribe = RoyaltyBoard.getTribeIndexOfUsername(player.getName());
            } catch (RuntimeException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not part of a team!");
                return true;
            }

            int playerPosition = RoyaltyBoard.getPositionIndexOfUsername(player.getName());

            if (Banned.isPlayerBanned(player.getUniqueId().toString())) {

                // Player is banned from royalty board
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not allowed to initiate a challenge!");

                return true;

            } else if (playerPosition != 5 && RoyaltyBoard.isOnCooldown(playerTribe, playerPosition)) {

                // Player is on cooldown. They cannot challenge
                StringBuilder builder = new StringBuilder();
                LocalDateTime challengeDate = LocalDateTime.parse(RoyaltyBoard.getLastChallengeDate(playerTribe, playerPosition));

                builder.append(EyeOfOnyx.EOO)
                        .append(ChatColor.RED)
                        .append("You are on movement cooldown until ")
                        .append(challengeDate.format(DateTimeFormatter.ISO_DATE));

                return true;

            }

        }


        if (args.length == 0) {
            if (sender instanceof Player player) {

                int playerTribe = RoyaltyBoard.getTribeIndexOfUsername(player.getName());
                int playerPosition = RoyaltyBoard.getPositionIndexOfUsername(player.getName());

                StringBuilder builder = new StringBuilder();

                /*

                CHALLENGE MENU
                You are currently $position$ of the $playerTeam$s.
                Select a position:

                 */

                builder.append(EyeOfOnyx.EOO + "CHALLENGE MENU")
                        .append("\nYou are currently ")
                        .append(ChatColor.YELLOW)
                        .append(positions[playerPosition].replace('_', ' '))
                        .append(ChatColor.WHITE)
                        .append(" of the ")
                        .append(ChatColor.YELLOW)
                        .append(teams[playerTribe])
                        .append("s")
                        .append(ChatColor.WHITE)
                        .append(".\nSelect a position:\n\n");

                if /* Player is a civilian */ (playerPosition == CIVILIAN) {

                    // Check for any empty positions
                    int nextEmptyPosition = CIVILIAN;

                    // Iterate through positions (start at ruler)
                    for (int i = 0; i < positions.length; i++) {
                        nextEmptyPosition = i;
                        // If position is empty, break
                        if (RoyaltyBoard.getUuid(playerTribe, i).equals("none")) {
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
                                .append("\nPosition Available\n\n");

                        TextComponent button = new TextComponent("[ Assume Position ]");
                        button.setUnderlined(true);
                        button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                        builder.append(button.toLegacyText());

                    } else {

                        /*

                        $POSITION$
                        $Username$

                        [ Initiate Challenge ]

                         */

                        builder.append(positions[NOBLE_PRESUMPTIVE].toUpperCase().replace('_', ' '))
                                .append("\n")
                                .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, NOBLE_PRESUMPTIVE)).getUsername());

                        TextComponent button = new TextComponent("[ Initiate Challenge ]");
                        button.setUnderlined(true);
                        button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                        builder.append(button.toLegacyText())
                                .append("\n\n")
                                .append(positions[NOBLE_APPARENT].toUpperCase().replace('_', ' '))
                                .append("\n")
                                .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, NOBLE_APPARENT)).getUsername());

                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position2"));

                        builder.append(button.toLegacyText());

                    }
                } else if /* Player is a noble */ (playerPosition == NOBLE_PRESUMPTIVE || playerPosition == NOBLE_APPARENT) {

                    builder.append(positions[HEIR_PRESUMPTIVE].toUpperCase().replace('_', ' '))
                            .append("\n")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, HEIR_PRESUMPTIVE)).getUsername());

                    TextComponent button = new TextComponent("[ Initiate Challenge ]");
                    button.setUnderlined(true);
                    button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                    builder.append(button.toLegacyText())
                            .append("\n\n")
                            .append(positions[HEIR_APPARENT].toUpperCase().replace('_', ' '))
                            .append("\n")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, HEIR_APPARENT)).getUsername());

                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position2"));

                    builder.append(button.toLegacyText());

                } else if /* Player is an heir */ (playerPosition == HEIR_PRESUMPTIVE || playerPosition == HEIR_APPARENT) {

                    builder.append(positions[RULER].toUpperCase().replace('_', ' '))
                            .append("\n")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, RULER)).getUsername());

                    TextComponent button = new TextComponent("[ Initiate Challenge ]");
                    button.setUnderlined(true);
                    button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                    builder.append(button.toLegacyText());

                } else if /* Player is ruler */ (playerPosition == RULER) {

                    builder.append(ChatColor.RED).append("You are ruler! No one to challenge.");

                }

                sender.sendMessage(builder.toString());

            } else {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments!");
                return false;
            }
        } else if (args.length == 1) {
            if (sender instanceof Player player) {

                int playerTribe = RoyaltyBoard.getTribeIndexOfUsername(player.getName());
                int playerPosition = RoyaltyBoard.getPositionIndexOfUUID(player.getUniqueId().toString());

                StringBuilder builder = new StringBuilder();
                builder.append(EyeOfOnyx.EOO);

                // Make sure challenge is valid
                if (playerPosition == CIVILIAN) {
                    int targetPosition;

                    // Determine which position is being targeted
                    if (args[0].equals("position1")) {
                        targetPosition = NOBLE_PRESUMPTIVE;
                    } else if (args[0].equals("positionn2")) {
                        targetPosition = NOBLE_APPARENT;
                    } else {
                        builder.append(ChatColor.RED).append("Invalid arguments!");
                        sender.sendMessage(builder.toString());
                        return true;
                    }

                    // Check that target is not being challenged
                    if (!RoyaltyBoard.getAttacker(playerTribe, targetPosition).equals("none") || !RoyaltyBoard.getAttacking(playerTribe, targetPosition).equals("none")) {

                        builder.append("That player is already in a challenge!");
                        sender.sendMessage(builder.toString());
                        return true;

                    } else {

                        // Check for cooldown
                        LocalDateTime targetChallenge = LocalDateTime.parse(RoyaltyBoard.getLastChallengeDate(playerTribe, targetPosition));
                        if (targetChallenge.isBefore(LocalDateTime.now().minusDays(14))) {
                            builder.append("That player is on movement cooldown until ")
                                    .append(targetChallenge.format(DateTimeFormatter.ISO_DATE));
                            sender.sendMessage(builder.toString());
                            return true;
                        } else {
                            String targetUuid = RoyaltyBoard.getUuid(playerTribe, targetPosition);

                            // set values in board.yml
                            RoyaltyBoard.setAttacker(playerTribe, targetPosition, player.getUniqueId().toString());
                            RoyaltyBoard.setAttacking(playerTribe, playerPosition, targetUuid);

                            // create notification for target
                            String title = "You've been challenged!";
                            String content = player.getName() + " has challenged your position for " + positions[targetPosition].replace('_', ' ') + ".";
                            Notification.saveNotification(new Notification(targetUuid, title, content, NotificationType.CHALLENGE_REQUESTED));

                            builder.append("Challenge initiated!");

                        }
                    }

                } else if (playerPosition == NOBLE_PRESUMPTIVE || playerPosition == NOBLE_APPARENT) {

                } else if (playerPosition == HEIR_PRESUMPTIVE || playerPosition == HEIR_APPARENT) {

                } else if (playerPosition == RULER) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are ruler! No one to challenge.");
                } else {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Unaccounted situation! playerPosition " + playerPosition);
                }

            }
        }

        return true;
    }
}