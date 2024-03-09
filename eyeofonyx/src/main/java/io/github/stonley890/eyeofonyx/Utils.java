package io.github.stonley890.eyeofonyx;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class Utils {

    /**
     * Convert a local time to a player's time.
     * @param time the {@link LocalDateTime} to convert.
     * @param player the {@link Player} who's timezone is to be converted to.
     * @return A converted {@link ZonedDateTime} with the player timezone.
     * @throws NotFoundException if the player time could not be determined by {@link IpUtils}.
     */
    public static @NotNull ZonedDateTime localTimeToPlayerTime(LocalDateTime time, @NotNull Player player) throws NotFoundException {
        ZonedDateTime playerTime = IpUtils.ipToTime(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress());
        if (playerTime == null) throw new NotFoundException("Player time could not be determined.");
        ZoneId playerOffset = playerTime.getOffset();
        return ZonedDateTime.of(time, ZoneId.systemDefault()).withZoneSameLocal(playerOffset);
    }

    /**
     * Get the ZoneId of a given player.
     * @param player the player to check.
     * @return a {@link ZoneId} of the given player.
     * @throws NotFoundException if the player's time could not be retrieved.
     */
    public static ZoneId getZoneIdOfPlayer(@NotNull Player player) throws NotFoundException {
        ZonedDateTime playerTime = IpUtils.ipToTime(Objects.requireNonNull(player.getAddress()).getAddress().getHostAddress());
        if (playerTime == null) throw new NotFoundException("Player time could not be determined.");
        return playerTime.getZone();
    }

    /**
     * Removes all tribe-based permissions from the user and adds the one specified. This will run asynchronously, so
     * keep in mind that results may not be immediately up-to-date.
     * @param tribeIndex the tribe to set.
     * @param posIndex the position to set.
     */
    public static void setPlayerPerms(UUID playerUuid, int tribeIndex, int posIndex) {
        if (EyeOfOnyx.luckperms != null) {
            // Get user manager
            UserManager userManager = EyeOfOnyx.luckperms.getUserManager();

            // Get user
            CompletableFuture<User> userFuture = userManager.loadUser(playerUuid);

            userFuture.thenAcceptAsync(user -> {
                String[] tribes = RoyaltyBoard.getTribes();
                String[] positions = RoyaltyBoard.getValidPositions();

                // Remove all other groups
                for (String tribe : tribes) {
                    for (String position : positions) {
                        // Get the lp group name from config
                        String groupName = EyeOfOnyx.getPlugin().getConfig().getString(position + "." + tribe);

                        if (groupName != null) {
                            // Get the group from lp and remove it from the user.
                            Group group = EyeOfOnyx.luckperms.getGroupManager().getGroup(groupName);
                            user.getInheritedGroups(user.getQueryOptions()).remove(group);

                        } else
                            Bukkit.getLogger().warning("Group " + position + "." + tribe + " is null in the config!");
                    }
                }

                // Add the group
                String groupName = EyeOfOnyx.getPlugin().getConfig().getString(positions[posIndex] + "." + tribes[tribeIndex]);

                if (groupName != null) {
                    // Get the group from lp and add it to the user.
                    Group group = EyeOfOnyx.luckperms.getGroupManager().getGroup(groupName);
                    user.getInheritedGroups(user.getQueryOptions()).add(group);
                }

            });
        }
    }

    /**
     * Get the integer index of a tribe's name.
     * @param tribe a {@link String}.
     * @return the index of the tribe identified or -1 if no match.
     */
    public static int tribeIndexFromString(String tribe) {
        int tribeIndex = -1;

        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
            String vTribe = RoyaltyBoard.getTribes()[i];
            if (vTribe.equals(tribe)) {
                tribeIndex = i;
                break;
            }
        }

        return tribeIndex;
    }

    /**
     * Get the integer index of a position's name.
     * @param pos a {@link String}.
     * @return the index of the position identified or -1 if no match.
     */
    public static int posIndexFromString(String pos) {
        int posIndex = -1;

        for (int i = 0; i < RoyaltyBoard.getValidPositions().length; i++) {
            String vTribe = RoyaltyBoard.getValidPositions()[i];
            if (vTribe.equals(pos)) {
                posIndex = i;
                break;
            }
        }

        return posIndex;
    }
}
