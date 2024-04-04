package io.github.stonley890.eyeofonyx.listeners;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.BoardPosition;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.PlayerTribe;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ListenJoin implements Listener {
    private static final String[] tribes = RoyaltyBoard.getTribes();

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {

        Player player = event.getPlayer();

        // Update tribe
        try {
            PlayerTribe.updateTribeOfPlayer(player.getUniqueId());
        } catch (NotFoundException e) {
            // Player does not have a tribe.
        }

        // Update LuckPerms group (just in case)
        UserManager userManager = EyeOfOnyx.luckperms.getUserManager();

        CompletableFuture<User> userFuture = userManager.loadUser(player.getUniqueId());

        userFuture.thenAcceptAsync(user -> {

            int tribeIndex;
            int posIndex;

            try {
                tribeIndex = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
                posIndex = RoyaltyBoard.getPositionIndexOfUUID(player.getUniqueId());
            } catch (NotFoundException e) {
                return;
            }

            if (posIndex == RoyaltyBoard.CIVILIAN) {

                // remove other permissions
                for (int tribe = 0; tribe < tribes.length; tribe++) {
                    if (tribe != tribeIndex) {
                        String groupName = EyeOfOnyx.getPlugin().getConfig().getString("citizen." + tribes[tribe]);
                        user.data().remove(Node.builder("group." + groupName).build());
                    }
                }

                // add appropriate permission
                String groupName = EyeOfOnyx.getPlugin().getConfig().getString("citizen." + tribes[tribeIndex]);

                user.data().add(Node.builder("group." + groupName).build());
                userManager.saveUser(user);

            }

        });

        // Get and cache timezone
        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), () -> {
            // Cache player IP data
            IpUtils.ipToTime(player.getAddress().getAddress().getHostAddress());
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
