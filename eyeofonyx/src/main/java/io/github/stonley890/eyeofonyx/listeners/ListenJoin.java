package io.github.stonley890.eyeofonyx.listeners;

import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class ListenJoin implements Listener {

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {

        Player player = event.getPlayer();

        // Get and cache timezone
        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {
            // Cache player IP data
            try {
                IpUtils.ipToTime(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress());
            } catch (NullPointerException ignored) {}
        });

        // Call to join challenge if active
        if (Competition.activeChallenge != null) {
            Competition.activeChallenge.callToJoin();
        }

        for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId())) {
            notification.sendMessage();
        }

        // update last online
        UUID uuid = player.getUniqueId();
        Tribe tribe = PlayerTribe.getTribeOfPlayer(uuid);
        if (tribe == null) return;
        int pos = RoyaltyBoard.getPositionIndexOfUUID(tribe, uuid);

        // If the player is not a citizen
        if (pos != RoyaltyBoard.CIVILIAN) RoyaltyBoard.setLastOnline(tribe, pos, LocalDateTime.now());
    }
}
