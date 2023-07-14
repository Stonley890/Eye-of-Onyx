package io.github.stonley890.eyeofonyx;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;

import static io.github.stonley890.eyeofonyx.files.RoyaltyBoard.CIVILIAN;
import static io.github.stonley890.eyeofonyx.files.RoyaltyBoard.RULER;

public class Challenge {

    private static final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    private static final Mojang mojang = new Mojang().connect();

    private static final FileConfiguration board = RoyaltyBoard.get();

    private static final String[] teams = RoyaltyBoard.getTeamNames();
    private static final String[] tribes = RoyaltyBoard.getTribes();
    private static final String[] positions = RoyaltyBoard.getValidPositions();

    /*
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
        if (defendingPosition != CIVILIAN && attackingTribe == defendingTribe && (attackingPosition - 1) == defendingPosition) {
            return (
                    // Ensure attacker is not challenging
                    RoyaltyBoard.getValueOfPosition(attackingTribe, attackingPosition, "challenging").equals("none") &&
                            // Ensure attacker is not being challenged
                            RoyaltyBoard.getValueOfPosition(attackingTribe, attackingPosition, "challenger").equals("none") &&
                            // Ensure attacker is not being challenged
                            RoyaltyBoard.getValueOfPosition(defendingTribe, defendingPosition, "challenger").equals("none") &&
                            // Ensure attacker is not challenging OR they are civilian (in which they have no data)
                            (defendingPosition == RULER || RoyaltyBoard.getValueOfPosition(attackingTribe, defendingPosition, "challenging").equals("none")) &&

                            LocalDateTime.parse(RoyaltyBoard.getValueOfPosition(attackingTribe, attackingPosition, "last_challenge_time")).isBefore(LocalDateTime.now().minusDays(14))
            );
        }
        return false;
    }
    */
}
