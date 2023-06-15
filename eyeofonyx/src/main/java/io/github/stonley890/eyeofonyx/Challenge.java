package io.github.stonley890.eyeofonyx;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;

public class Challenge {

    Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

    Mojang mojang = new Mojang().connect();

    static FileConfiguration board = RoyaltyBoard.get();

    String[] teams = RoyaltyBoard.getTeamNames();
    static String[] tribes = RoyaltyBoard.getTribes();
    static String[] positions = RoyaltyBoard.getValidPositions();

    public static boolean isChallengeValid(String attackingPlayerUUID, String defendingPlayerUUID) {

        // Get attacking tribe
        int attackingTribe = RoyaltyBoard.getTribeIndexOfUUID(attackingPlayerUUID);
        // Get attacking position
        int attackingPosition = RoyaltyBoard.getPositionIndexOfUUID(attackingPlayerUUID);

        // Get defending tribe
        int defendingTribe = RoyaltyBoard.getTribeIndexOfUUID(defendingPlayerUUID);
        // Get defending position
        int defendingPosition = RoyaltyBoard.getPositionIndexOfUUID(defendingPlayerUUID);

        // Make sure the defending player is not a civilian
        // Make sure both players are of same tribe
        // Make sure defendingPosition is one index below attackingPosition
        if (defendingPosition != 5 && attackingTribe == defendingTribe && (attackingPosition - 1) == defendingPosition) {
            return (
                    Objects.equals(board.getString(tribes[attackingTribe] + "." + positions[attackingPosition] + ".challenging"), "none") &&
                            Objects.equals(board.getString(tribes[attackingTribe] + "." + positions[attackingPosition] + ".challenger"), "none") &&
                            Objects.equals(board.getString(tribes[defendingTribe] + "." + positions[defendingPosition] + ".challenger"), "none") &&
                            (defendingPosition == 0 || Objects.equals(board.getString(tribes[defendingTribe] + "." + positions[defendingPosition] + ".challenging"), "none"))
            );
        }
        return false;
    }


}
