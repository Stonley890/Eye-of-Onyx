package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.Banned;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.NotificationType;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static io.github.stonley890.eyeofonyx.files.RoyaltyBoard.*;

public class CmdChallenge implements CommandExecutor {

    int attackingTribe;
    int attackingPosition;

    String[] tribes = RoyaltyBoard.getTribes();
    String[] teams = RoyaltyBoard.getTeamNames();
    String[] positions = RoyaltyBoard.getValidPositions();

    Mojang mojang = new Mojang().connect();

    FileConfiguration board = RoyaltyBoard.get();

    public static List<Player> playersOnForm = new ArrayList<>();
    public static List<String> codesOnForm = new ArrayList<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {

            sender.sendMessage(EyeOfOnyx.EOO + "Please wait...");

            int playerTribe;
            try {
                playerTribe = RoyaltyBoard.getTribeIndexOfUsername(player.getName());
                if (playerTribe > 6 || playerTribe < 1) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not part of a team!");
                    return true;
                }
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
                ComponentBuilder builder = new ComponentBuilder();
                LocalDateTime challengeDate = LocalDateTime.parse(RoyaltyBoard.getLastChallengeDate(playerTribe, playerPosition));

                builder.append(EyeOfOnyx.EOO)
                        .color(net.md_5.bungee.api.ChatColor.RED)
                        .append("You are on movement cooldown until ")
                        .append(challengeDate.format(DateTimeFormatter.ISO_DATE));

                sender.spigot().sendMessage(builder.create());

                return true;

            } else if (playerPosition != RULER && !RoyaltyBoard.getAttacking(playerTribe, playerPosition).equals("none")) {

                // Player has already initiated a challenge
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You have already initiated a challenge!");

                return true;

            }
            if (args.length == 0) {

                ComponentBuilder builder = new ComponentBuilder();

                /*

                CHALLENGE MENU
                You are currently $position$ of the $playerTeam$s.
                Select a position:

                 */

                builder.append(EyeOfOnyx.EOO + "CHALLENGE MENU")
                        .append("\nYou are currently ")
                        .append(positions[playerPosition].replace('_', ' ')).color(net.md_5.bungee.api.ChatColor.YELLOW)
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
                                .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, NOBLE_PRESUMPTIVE)).getUsername()).color(net.md_5.bungee.api.ChatColor.YELLOW)
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
                                .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, NOBLE_APPARENT)).getUsername())
                                .append(" ");

                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position2"));

                        builder.append(button);

                    }
                } else if /* Player is a noble */ (playerPosition == NOBLE_PRESUMPTIVE || playerPosition == NOBLE_APPARENT) {

                    builder.append(positions[HEIR_PRESUMPTIVE].toUpperCase().replace('_', ' '))
                            .append(" ")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, HEIR_PRESUMPTIVE)).getUsername()).color(net.md_5.bungee.api.ChatColor.YELLOW)
                            .append("\n");

                    TextComponent button = new TextComponent("[ Initiate Challenge ]");
                    button.setUnderlined(true);
                    button.setColor(net.md_5.bungee.api.ChatColor.GREEN);
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                    builder.append(button)
                            .append("\n \n")
                            .append(positions[HEIR_APPARENT].toUpperCase().replace('_', ' '))
                            .append("\n")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, HEIR_APPARENT)).getUsername())
                            .append(" ");

                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position2"));

                    builder.append(button)
                            .color(net.md_5.bungee.api.ChatColor.RESET);

                } else if /* Player is an heir */ (playerPosition == HEIR_PRESUMPTIVE || playerPosition == HEIR_APPARENT) {

                    builder.append(positions[RULER].toUpperCase().replace('_', ' '))
                            .append(" ")
                            .append(mojang.getPlayerProfile(RoyaltyBoard.getUuid(playerTribe, RULER)).getUsername()).color(net.md_5.bungee.api.ChatColor.YELLOW)
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


            } else if (args.length == 1) {

                switch (args[0]) {
                    case "position1", "position2" -> {

                        ComponentBuilder builder = new ComponentBuilder();
                        builder.append(EyeOfOnyx.EOO);

                        int targetPosition;

                        // Make sure challenge is valid
                        if (playerPosition == CIVILIAN) {

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
                        if (!(RoyaltyBoard.getAttacker(playerTribe, targetPosition).equals("none") || (RoyaltyBoard.getAttacking(playerTribe, targetPosition).equals("none") && targetPosition != 0))) {

                            builder.append("That player is already in a challenge!");
                            sender.spigot().sendMessage(builder.create());
                            return true;

                        } else {

                            // Check for cooldown
                            LocalDateTime targetChallenge = LocalDateTime.parse(RoyaltyBoard.getLastChallengeDate(playerTribe, targetPosition));
                            if (targetChallenge.isBefore(LocalDateTime.now().minusDays(14))) {
                                builder.append("That player is on movement cooldown until ")
                                        .append(targetChallenge.format(DateTimeFormatter.ISO_DATE));

                                return true;
                            } else {
                                String targetUuid = RoyaltyBoard.getUuid(playerTribe, targetPosition);


                                // set values in board.yml
                                // RoyaltyBoard.setAttacker(playerTribe, targetPosition, player.getUniqueId().toString());
                                RoyaltyBoard.setAttacking(playerTribe, playerPosition, targetUuid);

                                // create notification for target
                                String title = "You've been challenged!";
                                String content = player.getName() + " has challenged your position for " + positions[targetPosition].replace('_', ' ') + ".";
                                new Notification(targetUuid, title, content, NotificationType.CHALLENGE_REQUESTED).create();

                                builder.append("Challenge initiated!");
                                sender.spigot().sendMessage(builder.create());
                            }
                        }


                    }
                    case "accept" -> {

                        ComponentBuilder builder = new ComponentBuilder();
                        builder.append(EyeOfOnyx.EOO).append("One more thing! Use the link below to select times that you are available to attend the challenge. You'll need the code below too.").append("\n");

                        TextComponent link = new TextComponent("[Submit Availability]");
                        link.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        link.setUnderlined(true);
                        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://" + EyeOfOnyx.getPlugin().getConfig().getString("address") + ":8000/availability"));
                        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to open the web form.").create()));

                        String id = String.format("%04d", new Random().nextInt(10000));

                        TextComponent code = new TextComponent("[" + id + "]");
                        code.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
                        code.setUnderlined(true);
                        code.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id));
                        code.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Click to copy the code.").create()));

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

                    }
                    case "denyconfirm" -> {

                        // Remove notification
                        try {
                            Notification.getNotificationsOfPlayer(player.getUniqueId().toString()).removeIf(notification -> notification.type == NotificationType.CHALLENGE_REQUESTED);
                        } catch (IOException | InvalidConfigurationException e) {
                            e.printStackTrace();
                        }

                        // Remove from royalty board
                        RoyaltyBoard.setValue(playerTribe, playerPosition, LAST_ONLINE, "none");
                        RoyaltyBoard.updateBoard();
                        sender.sendMessage(EyeOfOnyx.EOO + "You have been removed from the royalty board.");
                    }
                    default ->
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! Use the text GUI with /challenge");
                }
            } else {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Too many arguments! Use the text GUI with /challenge");
            }
        } else {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That command is only available for players!");
        }

        return true;
    }

}