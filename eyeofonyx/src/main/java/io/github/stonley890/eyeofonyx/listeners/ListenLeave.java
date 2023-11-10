package io.github.stonley890.eyeofonyx.listeners;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import io.github.stonley890.eyeofonyx.files.BoardPosition;
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


    private final String[] validPositions = RoyaltyBoard.getValidPositions();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // Clear stored IP address data
        IpUtils.clearCache(player.getAddress().getAddress().getHostAddress());

        try {
            int tribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());

            UUID uuid = player.getUniqueId();

            // Check each position for player
            for (int pos = 0; pos < validPositions.length; pos++) {

                // If player is found on board, update last_online
                if (RoyaltyBoard.getUuid(tribe, pos).equals(uuid)){
                    BoardPosition updatedPos = RoyaltyBoard.getBoardOf(tribe).getPos(pos);
                    updatedPos.lastOnline = LocalDateTime.now();
                    RoyaltyBoard.set(tribe, pos, updatedPos);
                }
            }
        } catch (NullPointerException | NotFoundException e) {
            // Player is not part of a team
        }

    }
}
