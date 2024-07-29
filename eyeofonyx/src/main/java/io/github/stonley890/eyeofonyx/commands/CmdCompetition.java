package io.github.stonley890.eyeofonyx.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.ExecutableCommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.BoardState;
import io.github.stonley890.eyeofonyx.files.RoyaltyAction;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;


public class CmdCompetition {

    @NotNull
    public ExecutableCommand<?, ?> getCommand() {
        return new CommandAPICommand("competition")
                .withHelp("Control challenges.", "Control active challenges.")
                .withPermission("eyeofonyx.managechallenge")
                .withSubcommands(
                        new CommandAPICommand("start")
                                .executesNative((sender, args) -> {
                                    if (Competition.activeChallenge == null) throw CommandAPI.failWithString("There is no currently active challenge.");
                                    else if (!Competition.activeChallenge.started) {

                                        // Get participants as players
                                        Player attacker = Bukkit.getPlayer(Competition.activeChallenge.attacker);
                                        Player defender = Bukkit.getPlayer(Competition.activeChallenge.defender);

                                        // Ensure both players are online
                                        if (attacker != null && defender != null) {

                                            // Add scoreboard tags
                                            try {
                                                Objects.requireNonNull(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard().getTeam("eoo.attacker")).addEntry(attacker.getName());
                                                Objects.requireNonNull(Bukkit.getScoreboardManager().getMainScoreboard().getTeam("eoo.defender")).addEntry(defender.getName());
                                            } catch (NullPointerException e) {
                                                throw CommandAPI.failWithString("Teams eoo.attacker or eoo.defender do not exist!");
                                            }
                                            // Start challenge
                                            Competition.activeChallenge.started = true;

                                            sender.sendMessage(EyeOfOnyx.EOO + "Competition is now started.");
                                        } else {
                                            throw CommandAPI.failWithString("Both players must be online!");
                                        }
                                    } else throw CommandAPI.failWithString("This challenge has already started!");
                                }),
                        new CommandAPICommand("cancel")
                                .executesNative((sender, args) -> {
                                    if (Competition.activeChallenge == null) throw CommandAPI.failWithString("There is no currently active challenge.");
                                    else {
                                        // delete competition
                                        Tribe tribe = Competition.activeChallenge.tribe;

                                        // Set teams back
                                        Objects.requireNonNull(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard().getTeam(tribe.getTeamName())).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.attacker)).getName());
                                        Objects.requireNonNull(Bukkit.getScoreboardManager().getMainScoreboard().getTeam(tribe.getTeamName())).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.defender)).getName());

                                        Competition.activeChallenge = null;

                                        sender.sendMessage(EyeOfOnyx.EOO + "Competition canceled.");
                                    }
                                }),
                        new CommandAPICommand("end")
                                .withArguments(new StringArgument("winner").includeSuggestions(ArgumentSuggestions.strings(
                                        "attacker", "defender"
                                )))
                                .executesNative((sender, args) -> {
                                    if (Competition.activeChallenge == null)
                                        throw CommandAPI.failWithString("There is no currently active challenge.");
                                    else if (Competition.activeChallenge.started) {

                                        // Get positions
                                        int attackerPos;
                                        int defenderPos;
                                        attackerPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.attacker);
                                        defenderPos = RoyaltyBoard.getPositionIndexOfUUID(Competition.activeChallenge.defender);

                                        Tribe tribe = Competition.activeChallenge.tribe;
                                        BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();

                                        String winner = (String) args.get("winner");

                                        assert winner != null;
                                        if (winner.equals("attacker")) {

                                            RoyaltyBoard.replace(tribe, attackerPos, defenderPos);
                                            RoyaltyBoard.removePlayer(tribe, attackerPos, true);
                                            RoyaltyBoard.updatePermissions(Competition.activeChallenge.attacker);
                                            RoyaltyBoard.updatePermissions(Competition.activeChallenge.defender);
                                            RoyaltyBoard.updateBoard(tribe, false);
                                            RoyaltyBoard.updateDiscordBoard(tribe);

                                        } else if (winner.equals("defender")) {

                                            // Defender win; remove attacker
                                            RoyaltyBoard.removePlayer(Competition.activeChallenge.tribe, attackerPos, true);
                                            RoyaltyBoard.updateBoard(tribe, false);
                                            RoyaltyBoard.updateDiscordBoard(tribe);

                                        } else throw CommandAPI.failWithString("Incorrect arguments! /competition end <attacker|defender>");

                                        // Set teams back
                                        Objects.requireNonNull(Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard().getTeam(tribe.getTeamName())).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.attacker)).getName());
                                        Objects.requireNonNull(Bukkit.getScoreboardManager().getMainScoreboard().getTeam(tribe.getTeamName())).addEntry(Objects.requireNonNull(Bukkit.getPlayer(Competition.activeChallenge.defender)).getName());

                                        Competition.activeChallenge = null;

                                        BoardState newBoard = RoyaltyBoard.getBoardOf(tribe);
                                        RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribe, oldBoard.clone(), newBoard));

                                        RoyaltyBoard.updateDiscordBoard(tribe);

                                        sender.sendMessage(EyeOfOnyx.EOO + "Board updated.");

                                    } else throw CommandAPI.failWithString("This challenge hasn't yet started!");
                                })
                );
    }
}
