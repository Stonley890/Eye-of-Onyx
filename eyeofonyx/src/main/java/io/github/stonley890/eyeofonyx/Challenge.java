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

    static Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

    static Mojang mojang = new Mojang().connect();

    static FileConfiguration board = RoyaltyBoard.get();

    static String[] teams = RoyaltyBoard.getTeamNames();
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
                    // Ensure attacker is not challenging
                    RoyaltyBoard.getValueOfPosition(attackingTribe, attackingPosition, "challenging").equals("none") &&
                            // Ensure attacker is not being challenged
                            RoyaltyBoard.getValueOfPosition(attackingTribe, attackingPosition, "challenger").equals("none") &&
                            // Ensure attacker is not being challenged
                            RoyaltyBoard.getValueOfPosition(defendingTribe, defendingPosition, "challenger").equals("none") &&
                            // Ensure attacker is not challenging OR they are civilian (in which they have no data)
                            (defendingPosition == 0 || RoyaltyBoard.getValueOfPosition(attackingTribe, defendingPosition, "challenging").equals("none")) &&

                            LocalDateTime.parse(RoyaltyBoard.getValueOfPosition(attackingTribe, attackingPosition, "last_challenge_time")).isBefore(LocalDateTime.now().minusDays(14))
            );
        }
        return false;
    }

}
