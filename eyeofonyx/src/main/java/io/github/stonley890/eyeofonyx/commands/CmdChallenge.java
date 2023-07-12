package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.Challenge;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

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

        if (args.length == 0) {
            if (sender instanceof Player player) {

                int playerTribe = RoyaltyBoard.getTribeIndexOfUsername(player.getName());
                int playerPosition = RoyaltyBoard.getPositionIndexOfUsername(player.getName());

                if (playerPosition != 0) {

                    String targetName = RoyaltyBoard.getValueOfPosition(playerTribe, playerPosition - 1, "name");
                    String targetUuid = RoyaltyBoard.getValueOfPosition(playerTribe, playerPosition - 1, "uuid");

//                    sender.sendMessage("You are challenging " + targetName + "(" + mojang.getPlayerProfile(targetUuid).getUsername() + ")");

                    sender.sendMessage("Challenge valid: " + Challenge.isChallengeValid(player.getUniqueId().toString(), targetUuid));
                }
            }
        }

        return true;
    }
}
