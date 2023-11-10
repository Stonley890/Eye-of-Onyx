package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.util.*;

public class CmdEyeOfOnyx implements CommandExecutor {

    EyeOfOnyx main = EyeOfOnyx.getPlugin();
    Mojang mojang = new Mojang().connect();

    boolean disabling = false;

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Eye of Onyx " + main.version + "\nStonley890 / iHeron");
        } else {
            switch (args[0]) {
                case "ban" -> {
                    // Player must have permission eyeofonxy.ban
                    if (!sender.hasPermission("eyeofonxy.ban"))
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have permission to run that command.");
                    else {
                        // There must be at least 2 arguments: /eyeofonxy ban <username>
                        if /* there is another argument */ (args.length > 1) {

                            UUID uuid;

                            try {
                                // Get UUID
                                uuid = UUID.fromString(mojang.getUUIDOfUsername(args[1]));

                                if /* player is already banned */ (Banned.isPlayerBanned(uuid)) {
                                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is already banned.");
                                } else /* player is not yet banned */ {

                                    // Add player to ban list, remove them from the royalty board, and send them a notification.
                                    Banned.addPlayer(uuid);

                                    int tribe = 0;
                                    try {
                                        tribe = PlayerTribe.getTribeOfPlayer(uuid);
                                        int pos = 0;
                                        try {
                                            pos = RoyaltyBoard.getPositionIndexOfUUID(uuid);
                                        } catch (NotFoundException e) {
                                            // Player does not have a tribe.
                                            sender.sendMessage(EyeOfOnyx.EOO + "This player does not have an associated tribe.");
                                        }

                                        try {

                                            // Notify attacker if exists
                                            UUID attacker = RoyaltyBoard.getAttacker(tribe,pos);
                                            if (attacker != null) {
                                                int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                                                RoyaltyBoard.setAttacking(tribe, attackerPos, null);
                                                new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                                            }

                                            // Notify defender if exists
                                            if (pos != RoyaltyBoard.RULER) {
                                                UUID attacking = RoyaltyBoard.getAttacking(tribe,pos);
                                                if (attacking != null) {
                                                    int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                                                    RoyaltyBoard.setAttacker(tribe, defenderPos, null);
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

                                        if (pos != -1 && pos != RoyaltyBoard.CIVILIAN) {
                                            RoyaltyBoard.removePlayer(tribe, pos);
                                            RoyaltyBoard.updateBoard();
                                            try {
                                                RoyaltyBoard.updateDiscordBoard(tribe);
                                            } catch (IOException e) {
                                                sender.sendMessage(EyeOfOnyx.EOO + org.bukkit.ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                                            }
                                        }

                                    } catch (NotFoundException e) {
                                        // Player does not have a tribe. Not necessary in this case.
                                        // sender.sendMessage(EyeOfOnyx.EOO + "This player does not have an associated tribe.");
                                    }

                                    new Notification(uuid, "Royalty Ban", "You are no longer allowed to participate in royalty. Contact staff if you think this is a mistake.", NotificationType.GENERIC).create();
                                    sender.sendMessage(EyeOfOnyx.EOO + args[1] + " has been banned.");

                                }

                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That user could not be found.");
                            }

                        } else /* there are no other arguments */ {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments. /eyeofonyx ban <player>");
                        }
                    }

                }
                case "unban" -> {
                    // Player must have permission eyeofonxy.ban
                    if (!sender.hasPermission("eyeofonxy.ban"))
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have permission to run that command.");
                    else {
                        if /* there is another argument */ (args.length > 1) {
                            try {
                                UUID uuid = UUID.fromString(mojang.getUUIDOfUsername(args[1]));
                                if /* player is banned */ (Banned.isPlayerBanned(uuid)) {
                                    Banned.removePlayer(uuid);
                                    sender.sendMessage(EyeOfOnyx.EOO + args[1] + " has been unbanned.");
                                } else /* player is not banned */ {
                                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is not banned.");
                                }
                            } catch (IllegalArgumentException e) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That user could not be found.");
                            }

                        } else {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments. /eyeofonyx ban <player>");
                        }
                    }

                }
                case "banlist" -> {
                    if (!sender.hasPermission("eyeofonxy.ban"))
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have permission to run that command.");
                    else {
                        sender.sendMessage(EyeOfOnyx.EOO + "Please wait.");

                        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {

                            Mojang mojang = new Mojang().connect();

                            ComponentBuilder message = new ComponentBuilder(EyeOfOnyx.EOO);
                            message.append("Banned players:\n");

                            for (String bannedPlayer : Banned.getBannedPlayers()) {

                                String username = mojang.getPlayerProfile(bannedPlayer).getUsername();

                                TextComponent button = new TextComponent();
                                button.setText("Unban");
                                button.setItalic(true);
                                button.setColor(ChatColor.RED);
                                button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to unban this player from royalty.")));
                                button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/eyeofonyx unban " + username));

                                message.append("[").color(ChatColor.DARK_GRAY)
                                        .append(button).append("]").color(ChatColor.DARK_GRAY)
                                        .append(" ").reset().append(username).append("\n");

                            }

                            sender.spigot().sendMessage(message.create());

                        });
                    }

                }
                case "disable" -> {
                    if (sender.isOp()) {
                        Runnable disable = new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (disabling) {
                                    Bukkit.getLogger().info(sender.getName() + " disabled Eye of Onyx.");
                                    Bukkit.getPluginManager().disablePlugin(main);
                                }

                            }
                        };
                        if (disabling) {
                            disabling = false;
                            sender.sendMessage(EyeOfOnyx.EOO + "Disable canceled.");

                        } else {
                            sender.sendMessage(EyeOfOnyx.EOO + "Eye of Onyx will disable in five seconds. Run /disable again to cancel.");
                            disabling = true;
                            Bukkit.getScheduler().runTaskLater(EyeOfOnyx.getPlugin(), disable, 100L);
                        }
                    }

                }
                case "config" -> {

                    if (args.length < 2) {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /eyeofonyx config <key> <value>");
                    } else {

                        String key = args[1];
                        String value = null;
                        if (args.length > 2) value = args[2];

                        switch (key) {

                            case "challenge-cool-down" -> {
                                if (args.length == 2) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "The number of DAYS that a user is unable to participate in a challenge after they have completed one. Default: 7. Current: " + main.getConfig().get(key));
                                } else {
                                    try {
                                        main.getConfig().set(key, Integer.valueOf(value));
                                        sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to " + value + ".");
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(EyeOfOnyx.EOO + "That is not a valid int.");
                                    }
                                }
                            }
                            case "challenge-acknowledgement-time" -> {
                                if (args.length == 2) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "The number of DAYS that a user has to acknowledge a challenge that has been issued to them. Default: 7. Current: " + main.getConfig().get(key));
                                } else {
                                    try {
                                        main.getConfig().set(key, Integer.valueOf(value));
                                        sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to " + value + ".");
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(EyeOfOnyx.EOO + "That is not a valid int.");
                                    }
                                }
                            }
                            case "challenge-time-period" -> {
                                if (args.length == 2) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "The maximum number of DAYS from challenge acknowledgement that a challenge can be scheduled. Default: 7. Current: " + main.getConfig().get(key));
                                } else {
                                    try {
                                        main.getConfig().set(key, Integer.valueOf(value));
                                        sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to " + value + ".");
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(EyeOfOnyx.EOO + "That is not a valid int.");
                                    }
                                }
                            }
                            case "time-selection-period" -> {
                                if (args.length == 2) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "The number of DAYS a challenger is allotted to select one of the provided times. Default: 3. Current: " + main.getConfig().get(key));
                                } else {
                                    try {
                                        main.getConfig().set(key, Integer.valueOf(value));
                                        sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to " + value + ".");
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(EyeOfOnyx.EOO + "That is not a valid int.");
                                    }
                                }
                            }
                            case "inactivity-timer" -> {
                                if (args.length == 2) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "The number of DAYS that a user can be offline before they are removed from the royalty board. Default: 30. Current: " + main.getConfig().get(key));
                                } else {
                                    try {
                                        main.getConfig().set(key, Integer.valueOf(value));
                                        sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to " + value + ".");
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(EyeOfOnyx.EOO + "That is not a valid int.");
                                    }

                                }
                            }
                            case "waiting-rooms" -> {
                                if (args.length == 2) {

                                    List<Location> waitingRooms = (List<Location>) main.getConfig().getList(key);
                                    ComponentBuilder message = new ComponentBuilder(EyeOfOnyx.EOO);
                                    message.append("The locations of the challenge waiting rooms. Current: ");

                                    if (waitingRooms == null || waitingRooms.isEmpty()) {
                                        waitingRooms = new ArrayList<>();
                                    }

                                    for (int i = 0; i < waitingRooms.size(); i++) {
                                        message.append("\n");
                                        message.append(RoyaltyBoard.getTribes()[i].toUpperCase() + ": ");
                                        TextComponent teleport = getTeleport(waitingRooms.get(i));
                                        message.append(teleport);
                                    }

                                    sender.sendMessage(EyeOfOnyx.EOO + "The locations of the challenge waiting rooms. Current: " + main.getConfig().get(key));
                                } else {
                                    if (args.length < 5) {
                                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /eyeofonyx config waiting-rooms <tribe> <x> <y> <z> [world]");
                                    } else {

                                        int tribeIndex = -1;

                                        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                                            if (args[2].equals(RoyaltyBoard.getTribes()[i])) {
                                                tribeIndex = i;
                                                break;
                                            }
                                        }

                                        try {
                                            double x = Double.parseDouble(args[3]);
                                            double y = Double.parseDouble(args[4]);
                                            double z = Double.parseDouble(args[5]);

                                            World world = Bukkit.getWorlds().get(0);

                                            if (args.length > 6) {
                                                world = Bukkit.getWorld(args[6]);
                                                if (world == null) {
                                                    world = Bukkit.getWorlds().get(0);
                                                }
                                            }


                                            if (tribeIndex > -1) {

                                                List<Location> waitingRooms = (List<Location>) main.getConfig().getList(key);

                                                if (waitingRooms == null) {
                                                    waitingRooms = new ArrayList<>();
                                                }

                                                waitingRooms.set(tribeIndex, new Location(world, x, y, z));

                                                main.getConfig().set("waiting-rooms", waitingRooms);

                                                sender.sendMessage(EyeOfOnyx.EOO + RoyaltyBoard.getTribes()[tribeIndex].toUpperCase() + " waiting room set to " + x + ", " + y + ", " + z + " in " + world.getName());

                                            } else {
                                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid tribe! Acceptable values: " + Arrays.toString(RoyaltyBoard.getTribes()));
                                            }

                                        } catch (NumberFormatException e) {
                                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Could not parse double! /eyeofonyx config waiting-rooms <tribe> <x> <y> <z> [world]");
                                        }



                                    }
                                }
                            }
                            case "royalty-board-channel" -> {
                                if (args.length == 2) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "The channel ID for the Discord channel where the royalty board should be stored. Default: 913509632348139591. Current: " + main.getConfig().get(key));
                                } else {
                                    try {
                                        main.getConfig().set(key, Long.valueOf(value));
                                        sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to " + value + ".");
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(EyeOfOnyx.EOO + "That is not a valid long.");
                                    }

                                }
                            }
                            case "royalty-log-channel" -> {
                                if (args.length == 2) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "The channel ID for the Discord channel where updates to the royalty board should be recorded. Default: 660597606233276436. Current: " + main.getConfig().get(key));
                                } else {
                                    try {
                                        main.getConfig().set(key, Long.valueOf(value));
                                        sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to " + value + ".");
                                    } catch (NumberFormatException e) {
                                        sender.sendMessage(EyeOfOnyx.EOO + "That is not a valid long.");
                                    }

                                }
                            }
                        }
                    }

                    EyeOfOnyx.getPlugin().saveConfig();

                } case "freeze" -> {

                    if (RoyaltyBoard.isFrozen()) {
                        RoyaltyBoard.setFrozen(false);
                        sender.sendMessage(EyeOfOnyx.EOO + "The royalty board is now unfrozen.");
                    } else {
                        RoyaltyBoard.setFrozen(true);
                        sender.sendMessage(EyeOfOnyx.EOO + "The royalty board is now frozen.");
                    }

                } case "senddiscord" -> {

                    // Clear list of recorded messages
                    List<Long> messages = Dreamvisitor.getPlugin().getConfig().getLongList("royalty-board-message");
                    messages.clear();
                    EyeOfOnyx.getPlugin().getConfig().set("royalty-board-message", messages);
                    EyeOfOnyx.getPlugin().saveConfig();

                    // Send
                    try {
                        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
                            RoyaltyBoard.updateDiscordBoard(i);
                        }
                        sender.sendMessage(EyeOfOnyx.EOO + "Success!");
                    } catch (IOException e) {
                        Bukkit.getLogger().severe("Could not get message file!");
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Could not get message file! Check logs for more information.");
                        throw new RuntimeException(e);
                    }

                }
            }
        }


        return true;
    }

    @NotNull
    private static TextComponent getTeleport(Location waitingRoom) {
        TextComponent teleport = new TextComponent();
        if (waitingRoom != null) {
            teleport.setText("[" + Objects.requireNonNull(waitingRoom.getWorld()).getName() + waitingRoom.getX() + ", " + waitingRoom.getY() + ", " + waitingRoom.getZ() + "]");
            teleport.setUnderlined(true);
            teleport.setColor(ChatColor.GREEN);
            teleport.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + waitingRoom.getX() + " " + waitingRoom.getY() + " " + waitingRoom.getZ()));
        } else {
            teleport.setText("Unset");
            teleport.setColor(ChatColor.YELLOW);
        }

        return teleport;
    }
}
