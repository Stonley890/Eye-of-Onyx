package io.github.stonley890.eyeofonyx.listeners;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.PlayerTribe;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;

public class ListenJoin implements Listener {

    private static final FileConfiguration board = RoyaltyBoard.get();
    private static final Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

    private static final Mojang mojang = new Mojang().connect();

    private static final String[] teamNames = RoyaltyBoard.getTeamNames();
    private static final String[] tribes = RoyaltyBoard.getTribes();
    private static final String[] validPositions = RoyaltyBoard.getValidPositions();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        // Update tribe
        try {
            PlayerTribe.updateTribeOfPlayer(player);
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
                tribeIndex = PlayerTribe.getTribeOfPlayer(player.getUniqueId().toString());
                posIndex = RoyaltyBoard.getPositionIndexOfUUID(player.getUniqueId().toString());
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
            for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId().toString())) {
                notification.sendMessage();
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }

        try {
            String playerTeam = scoreboard.getEntryTeam(player.getName()).getName();
            int playerTribe = Arrays.binarySearch(teamNames, playerTeam);

            if (playerTribe > 6 || playerTribe < 1) {
                return;
            }

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
        } catch (NullPointerException e) {
            // Player is not part of a team
        }

    }
}
