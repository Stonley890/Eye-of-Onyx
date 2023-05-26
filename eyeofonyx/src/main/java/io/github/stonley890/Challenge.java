package io.github.stonley890;

import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.files.RoyaltyBoard;

public class Challenge {

    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    Mojang mojang = new Mojang().connect();

    FileConfiguration board = RoyaltyBoard.get();

    String[] teams = RoyaltyBoard.getTeamNames();
    String[] tribes = RoyaltyBoard.getTribes();
    String[] positions = RoyaltyBoard.getValidPositions();

    public boolean isChallengeValid(String attackingPlayerUUID, String defendingPlayerUUID) {

        // Get player information
        String attackingPlayer = mojang.getPlayerProfile(attackingPlayerUUID).getUsername();
        int attackingTribe = Arrays.binarySearch(teams, scoreboard.getEntryTeam(attackingPlayer).getName());
        // Position is set to 5 (civilian) by default
        int attackingPosition = 5;

        // Check each position in board.yml
        for (int i = 0; i < positions.length; i++) {
            if (board.contains(tribes[attackingTribe] + "." + positions[i] + "." + attackingPlayerUUID)) {
                // Change position if found on the royalty board
                attackingPosition = i;
            }
        }

        // Get player information
        String defendingPlayer = mojang.getPlayerProfile(defendingPlayerUUID).getUsername();
        int defendingTribe = Arrays.binarySearch(teams, scoreboard.getEntryTeam(defendingPlayer).getName());
        // Position is set to 5 (civilian) by default
        int defendingPosition = 5;

        // Check each position in board.yml
        for (int i = 0; i < positions.length; i++) {
            if (board.contains(tribes[defendingTribe] + "." + positions[i] + "." + defendingPlayerUUID)) {
                // Change position if found on the royalty board
                defendingPosition = i;
            }
        }

        // Make sure the defending player is not a civilian
        // Make sure both players are of same tribe
        // Make sure defendingPosition is one index below attackingPosition
        return (defendingPosition != 5 && attackingTribe == defendingTribe && (attackingPosition - 1) == defendingPosition);
    }
}
