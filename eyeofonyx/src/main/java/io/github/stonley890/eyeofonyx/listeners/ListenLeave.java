package io.github.stonley890.eyeofonyx.listeners;

import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ListenLeave implements Listener {

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {

        Player player = event.getPlayer();

        // Clear stored IP address data
        IpUtils.clearCache(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress());

        // update last online
        UUID uuid = player.getUniqueId();
        Tribe tribe = PlayerTribe.getTribeOfPlayer(uuid);
        if (tribe == null) return;
        int pos = RoyaltyBoard.getPositionIndexOfUUID(tribe, uuid);

        // If the player is not a citizen
        if (pos != RoyaltyBoard.CIVILIAN) RoyaltyBoard.setLastOnline(tribe, pos, LocalDateTime.now());
    }
}
