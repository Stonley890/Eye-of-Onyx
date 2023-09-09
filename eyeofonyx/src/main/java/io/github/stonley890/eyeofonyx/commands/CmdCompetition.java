package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.ChallengeType;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.util.Objects;


public class CmdCompetition implements CommandExecutor {

    private final Mojang mojang = new Mojang().connect();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /competition <create> <attacker> <defender> | modify <player> <action> [value]>");
        } else if (args.length == 1) {
            switch (args[0]) {
                case "create" -> sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /competition create <attacker> <defender>");
                case "modify" -> sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /competition modify <player> <action> [value]");
                default -> sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /competition <create <attacker> <defender> | modify <player> <action> [value]>");
            }
        } else {

            if (args[0].equals("create") ||  args[0].equals("modify")) {

                // /competition create needs at least 3 args
                if (args[0].equals("create") && args.length < 3) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /competition create <attacker> <defender>");
                    return true;
                }

                sender.sendMessage(EyeOfOnyx.EOO + "Please wait...");

                // Find player

                String playerUuid;

                if (args[1].equals("@p")) {
                    Entity nearest = Bukkit.selectEntities(sender, "@p").get(0);

                    if (nearest instanceof Player player) {
                        playerUuid = player.getUniqueId().toString();
                    } else {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Unable to locate a player");
                        return true;
                    }
                } else if (Bukkit.getPlayer(args[1]) != null) {
                    playerUuid = Objects.requireNonNull(Bukkit.getPlayer(args[1])).getUniqueId().toString();
                } else {
                    if (mojang.getUUIDOfUsername(args[1]) != null) {
                        playerUuid = mojang.getUUIDOfUsername(args[1]);
                    } else {
                        if (args[0].equals("create") || args[0].equals("modify")) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Could not find player!");
                        } else {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /competition <create <attacker> <defender> | modify <player> <action> [value]>");
                        }
                        return true;
                    }
                }

                if (args[0].equals("modify")) {
                    Competition competition = null;

                        if (Competition.activeChallenge.attacker.equals(playerUuid)) {
                            competition = Competition.activeChallenge;
                        } else if (Competition.activeChallenge.defender.equals(playerUuid)) {
                            competition = Competition.activeChallenge;
                        }


                    if (competition == null) {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is not in an active competition.");
                        return true;
                    }


                    if (args.length == 2) {
                        // competition modify <player>
                        // Print into about competition
                        ComponentBuilder builder = new ComponentBuilder(EyeOfOnyx.EOO);

                        int attackerPosIndex = RoyaltyBoard.getPositionIndexOfUUID(competition.attacker);
                        String attackerPos;
                        if (attackerPosIndex == 5) {
                            attackerPos = "CIVILIAN";
                        } else {
                            attackerPos = RoyaltyBoard.getValidPositions()[attackerPosIndex].toUpperCase().replace('_',' ');
                        }

                        builder.append("Competition Information\n")
                                .append("Tribe: ").append(RoyaltyBoard.getTribes()[competition.tribe])
                                .append("Attacker: ").append(attackerPos).append(" ").append(mojang.getPlayerProfile(competition.attacker).getUsername())
                                .append("Defender: ").append(RoyaltyBoard.getValidPositions()[RoyaltyBoard.getPositionIndexOfUUID(competition.defender)].toUpperCase().replace('_',' ')).append(" ").append(mojang.getPlayerProfile(competition.attacker).getUsername())
                                .append("Type: ").append(competition.type.toString());

                        sender.spigot().sendMessage(builder.create());
                    } else if (args.length == 3) {
                        // competition modify <player> <action>
                        switch (args[2]) {
                            case "attacker" -> sender.sendMessage(EyeOfOnyx.EOO + "Attacker:" + mojang.getPlayerProfile(competition.attacker).getUsername());
                            case "defender" -> sender.sendMessage(EyeOfOnyx.EOO + "Defender:" + mojang.getPlayerProfile(competition.defender).getUsername());
                            case "type" -> sender.sendMessage(EyeOfOnyx.EOO + "Type:" + competition.type.toString());
                            case "start" -> {
                                if (competition.type == ChallengeType.UNKNOWN) {
                                    sender.sendMessage(EyeOfOnyx.EOO + "Challenge invalid! Must have a valid challenge type. Current: " + competition.type);
                                    return true;
                                }

                                Competition.activeChallenge.started = true;
                                sender.sendMessage(EyeOfOnyx.EOO + "Competition is now started.");

                            }
                            case "cancel" -> {

                                // Clear values in board.yml and delete competition

                                int tribe = competition.tribe;

                                int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(competition.attacker);
                                int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(competition.defender);

                                RoyaltyBoard.setAttacker(tribe, defenderPos, "none");
                                RoyaltyBoard.setAttacking(tribe, attackerPos, "none");

                                Competition.activeChallenge = null;
                            }
                            case ""
                            case "end" -> {

                            }
                            default -> sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /competition modify <player> <action> [value]");
                        }
                    }

                }
            } else {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /competition <create <attacker> <defender> | modify <player> <action>>");
            }



        }

        return true;
    }
}
