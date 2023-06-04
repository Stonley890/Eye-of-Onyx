package io.github.stonley890.eyeofonyx.listeners;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;

public class ListenJoin implements Listener {

    FileConfiguration board = RoyaltyBoard.get();
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    Mojang mojang = new Mojang().connect();

    String[] teamNames = RoyaltyBoard.getTeamNames();
    String[] tribes = RoyaltyBoard.getTribes();
    String[] validPositions = RoyaltyBoard.getValidPositions();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        try {
            String playerTeam = scoreboard.getEntryTeam(player.getName()).getName();
            int playerTribe = Arrays.binarySearch(teamNames, playerTeam);
            String playerUUID = mojang.getUUIDOfUsername(player.getName()).replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5");

            // Check each position for player
            for (int i = 0; i < validPositions.length; i++) {

                // If player is found on board, update last_online
                if (board.contains(tribes[playerTribe] + "." + validPositions[i] + "." + playerUUID)) {
                    board.set(tribes[playerTribe] + "." + validPositions[i] + ".last_online",
                            LocalDateTime.now().toString());
                    
                    RoyaltyBoard.save(board);
                }
            }
        } catch (IllegalArgumentException e) {
            // Player is not part of a team
        }

    }
}