package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.BoardState;
import io.github.stonley890.eyeofonyx.files.PlayerTribe;
import io.github.stonley890.eyeofonyx.files.RoyaltyAction;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import javassist.NotFoundException;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CmdForfeit implements CommandExecutor {

    List<Player> partingPlayers = new ArrayList<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (sender instanceof Player player) {

            int tribe;
            int posIndex;

            try {
                tribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
                posIndex = RoyaltyBoard.getPositionIndexOfUUID(tribe, player.getUniqueId());
            } catch (NotFoundException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have an associated tribe! Contact a staff member.");
                return true;
            }

            if (posIndex == RoyaltyBoard.CIVILIAN) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not in a royalty position!");
                return true;
            }

            if (partingPlayers.contains(player)) {
                BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe).clone();
                partingPlayers.remove(player);
                RoyaltyBoard.removePlayer(tribe, posIndex, true);
                RoyaltyBoard.reportChange(new RoyaltyAction(sender.getName(), tribe, oldBoard, RoyaltyBoard.getBoardOf(tribe)));
                RoyaltyBoard.updateBoard(tribe, false);
                sender.sendMessage(EyeOfOnyx.EOO + "You have been removed from the royalty board.");
                try {
                    RoyaltyBoard.updateDiscordBoard(tribe);
                } catch (IOException e) {
                    sender.sendMessage(EyeOfOnyx.EOO + org.bukkit.ChatColor.RED + "An I/O error occurred while attempting to update Discord board.");
                }
            } else {
                partingPlayers.add(player);
                sender.sendMessage(EyeOfOnyx.EOO + "Are you sure you want to leave the position of " + RoyaltyBoard.getTeamNames()[tribe] + " " + RoyaltyBoard.getValidPositions()[posIndex].replace('_', ' ') + "? " + ChatColor.RED + "This action cannot be undone. Run /forfeit again to confirm.");
            }
            return true;

        } else {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This command be must by a player!");
        }

        return true;
    }
}
