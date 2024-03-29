package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.BoardState;
import io.github.stonley890.eyeofonyx.files.RoyaltyAction;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import javassist.NotFoundException;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.io.IOException;
import java.util.Objects;


public class CmdCompetition implements CommandExecutor {

    private final Mojang mojang = new Mojang().connect();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        if (args.length == 0) {
            // Send information

            if (Competition.activeChallenge == null) sender.sendMessage(EyeOfOnyx.EOO + "There is no currently active challenge. Create one with /challenge create <attacker> <defender>");
            else {
                // competition
                // Print into about competition
                ComponentBuilder builder = new ComponentBuilder(EyeOfOnyx.EOO);

                int attackerPosIndex = 0;
                try {
                    attackerPosIndex = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.attacker);
                } catch (NotFoundException e) {
                    // Attacker does not have an associate tribe (should not happen)
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Attacker does not have an associated tribe!");
                }
                String attackerPos;
                if (attackerPosIndex == RoyaltyBoard.CIVILIAN) {
                    attackerPos = "CIVILIAN";
                } else {
                    attackerPos = RoyaltyBoard.getValidPositions()[attackerPosIndex].toUpperCase().replace('_', ' ');
                }

                try {
                    builder.append("Competition Information\n")
                            .append("Tribe: ").append(RoyaltyBoard.getTribes()[Competition.activeChallenge.tribe])
                            .append("Attacker: ").append(attackerPos).append(" ").append(mojang.getPlayerProfile(Competition.activeChallenge.attacker.toString()).getUsername())
                            .append("Defender: ").append(RoyaltyBoard.getValidPositions()[RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.defender)].toUpperCase().replace('_', ' ')).append(" ").append(mojang.getPlayerProfile(Competition.activeChallenge.attacker.toString()).getUsername());
                } catch (NotFoundException e) {
                    // Attacker does not have an associate tribe (should not happen)
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Attacker does not have an associated tribe!");
                }

                sender.spigot().sendMessage(builder.create());
            }
        } else {
            // competition <action>
            switch (args[0]) {
                case "start" -> {
                    if (Competition.activeChallenge == null) sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "There is no currently active challenge.");
                    else if (!Competition.activeChallenge.started) {

                        // Get participants as players
                        Player attacker = Bukkit.getPlayer(Competition.activeChallenge.attacker);
                        Player defender = Bukkit.getPlayer(Competition.activeChallenge.defender);

                        // Ensure both players are online
                        if (attacker != null && defender != null) {

                            int tribe = Competition.activeChallenge.tribe;

                            BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();

                            // Get positions
                            int attackerPos;
                            int defenderPos;
                            try {
                                defenderPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.defender);
                                attackerPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.attacker);

                                RoyaltyBoard.setAttacker(tribe, defenderPos, null);
                                RoyaltyBoard.setAttacking(tribe, attackerPos, null);
                            } catch (NotFoundException e) {
                                // No associated tribe (should not happen)
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "One or more players do not have an associated tribe. Royalty board values will not be set.");
                            }

                            // Add scoreboard tags
                            Objects.requireNonNull(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard().getTeam("eoo.attacker")).addEntry(attacker.getName());
                            Objects.requireNonNull(Bukkit.getScoreboardManager().getMainScoreboard().getTeam("eoo.defender")).addEntry(defender.getName());
                            // Start challenge
                            Competition.activeChallenge.started = true;

                            BoardState newBoard = RoyaltyBoard.getBoardOf(tribe).clone();
                            RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribe, newBoard, oldBoard));

                            sender.sendMessage(EyeOfOnyx.EOO + "Competition is now started.");
                        } else {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Both players must be online!");
                        }
                    } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This challenge has already started!");


                }
                case "cancel" -> {

                    if (Competition.activeChallenge == null) sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "There is no currently active challenge.");
                    else {
                        // Clear values in board.yml and delete competition

                        int tribe = Competition.activeChallenge.tribe;

                        BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();

                        // Get positions
                        int attackerPos;
                        int defenderPos;
                        try {
                            defenderPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.defender);
                            attackerPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.attacker);

                            RoyaltyBoard.setAttacker(tribe, defenderPos, null);
                            RoyaltyBoard.setAttacking(tribe, attackerPos, null);
                        } catch (NotFoundException e) {
                            // No associated tribe (should not happen)
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "One or more players do not have an associated tribe. Royalty board values will not be set.");
                        }

                        BoardState newBoard = RoyaltyBoard.getBoardOf(tribe).clone();
                        RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribe, oldBoard, newBoard));

                        // Set teams back
                        Objects.requireNonNull(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard().getTeam(RoyaltyBoard.getTeamNames()[tribe])).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.attacker)).getName());
                        Objects.requireNonNull(Bukkit.getScoreboardManager().getMainScoreboard().getTeam(RoyaltyBoard.getTeamNames()[tribe])).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.defender)).getName());

                        Competition.activeChallenge = null;

                        sender.sendMessage(EyeOfOnyx.EOO + "Competition canceled.");
                    }
                }
                case "end" -> {

                    if (Competition.activeChallenge == null) sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "There is no currently active challenge.");
                    else if (Competition.activeChallenge.started) {

                        if (args.length < 2) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /competition end <attacker|defender>");
                        } else {

                            // Get positions
                            int attackerPos;
                            int defenderPos;
                            try {
                                attackerPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.attacker);
                                defenderPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.defender);
                            } catch (NotFoundException e) {
                                throw new RuntimeException(e);
                            }

                            int tribe = Competition.activeChallenge.tribe;
                            BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();

                            if (args[1].equals("attacker")) {

                                RoyaltyBoard.replace(tribe, attackerPos, defenderPos);
                                RoyaltyBoard.removePlayer(tribe, attackerPos, true);
                                RoyaltyBoard.saveToDisk();
                                RoyaltyBoard.updateBoard(tribe, false);
                                try {
                                    RoyaltyBoard.updateDiscordBoard(tribe);
                                } catch (IOException e) {
                                    Bukkit.getLogger().warning("Unable to update Discord board.");
                                }

                            } else if (args[1].equals("defender")) {

                                // Defender win; remove attacker
                                RoyaltyBoard.removePlayer(Competition.activeChallenge.tribe, attackerPos, true);
                                RoyaltyBoard.saveToDisk();
                                RoyaltyBoard.updateBoard(tribe, false);
                                try {
                                    RoyaltyBoard.updateDiscordBoard(tribe);
                                } catch (IOException e) {
                                    Bukkit.getLogger().warning("Unable to update Discord board.");
                                }

                            } else {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /competition end <attacker|defender>");
                                return true;
                            }

                            // Set teams back
                            Objects.requireNonNull(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard().getTeam(RoyaltyBoard.getTeamNames()[tribe])).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.attacker)).getName());
                            Objects.requireNonNull(Bukkit.getScoreboardManager().getMainScoreboard().getTeam(RoyaltyBoard.getTeamNames()[tribe])).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.defender)).getName());

                            Competition.activeChallenge = null;

                            BoardState newBoard = RoyaltyBoard.getBoardOf(tribe);
                            RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribe, oldBoard.clone(), newBoard));

                            try {
                                RoyaltyBoard.updateDiscordBoard(tribe);
                            } catch (IOException e) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                            }

                            sender.sendMessage(EyeOfOnyx.EOO + "Board updated.");

                        }

                    } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This challenge hasn't yet started!");

                }
                default ->
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /competition <action> [value]");
            }
        }

        return true;
    }
}
