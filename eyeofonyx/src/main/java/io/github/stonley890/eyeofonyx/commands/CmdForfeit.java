package io.github.stonley890.eyeofonyx.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.ExecutableCommand;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CmdForfeit {

    final List<Player> partingPlayers = new ArrayList<>();

    @NotNull
    public ExecutableCommand<?, ?> getCommand() {
        return new CommandAPICommand("forfeit")
                .withHelp("Leave the royalty board.", "Remove yourself from the royalty board.")
                .executesNative((sender, args) -> {
                    if (sender.getCallee() instanceof Player player) {

                        Tribe tribe;
                        int posIndex;


                        tribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
                        if (tribe == null) throw CommandAPI.failWithString("You do not have an associated tribe! Contact a staff member.");
                        posIndex = RoyaltyBoard.getPositionIndexOfUUID(tribe, player.getUniqueId());

                        if (posIndex == RoyaltyBoard.CIVILIAN) {
                            throw CommandAPI.failWithString("You are not in a royalty position!");
                        }

                        if (partingPlayers.contains(player)) {
                            BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();
                            partingPlayers.remove(player);
                            RoyaltyBoard.removePlayer(tribe, posIndex, true);

                            // Remove any challenges
                            Challenge.removeChallengesOfPlayer(player.getUniqueId(), "The player who was challenging you was removed from the royalty board, so your challenge was canceled.");

                            RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribe, oldBoard, RoyaltyBoard.getBoardOf(tribe)));
                            RoyaltyBoard.updateBoard(tribe, false);
                            sender.sendMessage(EyeOfOnyx.EOO + "You have been removed from the royalty board.");
                            RoyaltyBoard.updateDiscordBoard(tribe);
                        } else {
                            partingPlayers.add(player);
                            sender.sendMessage(EyeOfOnyx.EOO + "Are you sure you want to leave the position of " + tribe.getTeamName() + " " + RoyaltyBoard.getValidPositions()[posIndex].replace('_', ' ') + "? " + ChatColor.RED + "This action cannot be undone. Run /forfeit again to confirm.");
                        }
                    } else {
                        throw CommandAPI.failWithString("This command be executed as player!");
                    }
                });
    }
}
