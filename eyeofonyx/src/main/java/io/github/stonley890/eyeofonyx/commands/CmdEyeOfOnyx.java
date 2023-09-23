package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

                            // Get UUID
                            String uuid = mojang.getUUIDOfUsername(args[1]);
                            if /* the username is invalid */ (uuid == null) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That user could not be found.");
                            } else /* the username is valid */ {
                                if /* player is already banned */ (Banned.isPlayerBanned(uuid)) {
                                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is already banned.");
                                } else /* player is not yet banned */ {

                                    // Add player to ban list, remove them from the royalty board, and send them a notification.
                                    Banned.addPlayer(uuid);
                                    int tribe = 0;
                                    try {
                                        tribe = PlayerTribe.getTribeOfPlayer(uuid);
                                    } catch (NotFoundException e) {
                                        // Player does not have a tribe.
                                        sender.sendMessage(EyeOfOnyx.EOO + "This player does not have an associated tribe.");
                                    }
                                    int pos = 0;
                                    try {
                                        pos = RoyaltyBoard.getPositionIndexOfUUID(uuid);
                                    } catch (NotFoundException e) {
                                        // Player does not have a tribe.
                                        sender.sendMessage(EyeOfOnyx.EOO + "This player does not have an associated tribe.");
                                    }

                                    try {

                                        // Notify attacker if exists
                                        String attacker = RoyaltyBoard.getAttacker(tribe,pos);
                                        if (!attacker.equals("none")) {
                                            int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                                            RoyaltyBoard.setAttacking(tribe, attackerPos, "none");
                                            new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                                        }

                                        // Notify defender if exists
                                        if (pos != RoyaltyBoard.RULER) {
                                            String attacking = RoyaltyBoard.getAttacking(tribe,pos);
                                            if (!attacking.equals("none")) {
                                                int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                                                RoyaltyBoard.setAttacker(tribe, defenderPos, "none");
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
                                    }
                                    new Notification(uuid, "Royalty Ban", "You are no longer allowed to participate in royalty. Contact staff if you think this is a mistake.", NotificationType.GENERIC).create();
                                    sender.sendMessage(EyeOfOnyx.EOO + args[1] + " has been banned.");

                                }
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
                            String uuid = mojang.getUUIDOfUsername(args[1]);
                            if /* the username is invalid */ (uuid == null) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That user could not be found.");
                            } else /* the username is valid */ {
                                if /* player is banned */ (Banned.isPlayerBanned(uuid)) {
                                    Banned.removePlayer(uuid);
                                    sender.sendMessage(EyeOfOnyx.EOO + args[1] + " has been unbanned.");
                                } else /* player is not banned */ {
                                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is not banned.");
                                }
                            }
                        } else {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments. /eyeofonyx ban <player>");
                        }
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
