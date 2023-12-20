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
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ListenJoin implements Listener {

    private static final Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

    private static final String[] teamNames = RoyaltyBoard.getTeamNames();
    private static final String[] tribes = RoyaltyBoard.getTribes();
    private static final String[] validPositions = RoyaltyBoard.getValidPositions();

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {

        Player player = event.getPlayer();

        // Update tribe
        try {
            PlayerTribe.updateTribeOfPlayer(player.getUniqueId());
        } catch (InvalidObjectException | NotFoundException e) {
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

                String groupName = EyeOfOnyx.getPlugin().getConfig().getString("citizen." + tribes[tribeIndex]);

                user.data().add(Node.builder("group." + groupName).build());
                userManager.saveUser(user);

            }

        });

        // Get and cache timezone
        Bukkit.getScheduler().runTaskAsynchronously(EyeOfOnyx.getPlugin(), new Runnable() {
            @Override
            public void run() {
                // Cache player IP data
                IpUtils.ipToTime(player.getAddress().getAddress().getHostAddress());
            }
        });

        // Call to join challenge if active
        if (Competition.activeChallenge != null) {
            Competition.activeChallenge.callToJoin();
        }

        try {
            for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId())) {
                notification.sendMessage();
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        try {
            String playerTeam = scoreboard.getEntryTeam(player.getName()).getName();
            int tribe = Arrays.binarySearch(teamNames, playerTeam);

            if (tribe > 6 || tribe < 1) {
                return;
            }

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
        } catch (NullPointerException e) {
            // Player is not part of a team
        }

    }
}
