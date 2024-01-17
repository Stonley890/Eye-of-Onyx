package io.github.stonley890.eyeofonyx.listeners;

import io.github.stonley890.eyeofonyx.files.BoardPosition;
import io.github.stonley890.eyeofonyx.files.PlayerTribe;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public class ListenLeave implements Listener {

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // Clear stored IP address data
        IpUtils.clearCache(player.getAddress().getAddress().getHostAddress());

        // update last online
        UUID uuid = player.getUniqueId();
        try {
            int tribe = PlayerTribe.getTribeOfPlayer(uuid);
            int pos = RoyaltyBoard.getPositionIndexOfUUID(tribe, uuid);

            // If the player is not a citizen
            if (pos != RoyaltyBoard.CIVILIAN) {
                BoardPosition updatedPos = RoyaltyBoard.getBoardOf(tribe).getPos(pos);
                updatedPos.lastOnline = LocalDateTime.now();
                RoyaltyBoard.set(tribe, pos, updatedPos);
            }
        } catch (NotFoundException ignored) {}

    }
}
