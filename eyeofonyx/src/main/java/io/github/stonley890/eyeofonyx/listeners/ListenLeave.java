package io.github.stonley890.eyeofonyx.listeners;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import io.github.stonley890.eyeofonyx.files.PlayerTribe;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;

public class ListenLeave implements Listener {

    private final FileConfiguration board = RoyaltyBoard.get();
    private final Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();


    private final Mojang mojang = new Mojang().connect();

    private final String[] teamNames = RoyaltyBoard.getTeamNames();
    private final String[] tribes = RoyaltyBoard.getTribes();
    private final String[] validPositions = RoyaltyBoard.getValidPositions();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // Clear stored IP address data
        IpUtils.clearCache(player.getAddress().getAddress().getHostAddress());

        try {
            int playerTribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId().toString());
            String playerUUID = mojang.getUUIDOfUsername(player.getName()).replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5");

            // Check each position for player
            for (String validPosition : validPositions) {

                // If player is found on board, update last_online
                if (board.contains(tribes[playerTribe] + "." + validPosition + "." + playerUUID)) {
                    board.set(tribes[playerTribe] + "." + validPosition + ".last_online",
                            LocalDateTime.now().toString());

                    RoyaltyBoard.save(board);
                }
            }
        } catch (NullPointerException | NotFoundException e) {
            // Player is not part of a team
        }

    }
}
