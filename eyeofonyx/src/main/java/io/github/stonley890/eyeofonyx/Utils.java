package io.github.stonley890.eyeofonyx;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static io.github.stonley890.dreamvisitor.Dreamvisitor.plugin;

public class Utils {

    /**
     * Adds the hyphens back into a String UUID.
     * @param uuid the UUID as a string without hyphens.
     * @return a UUID as a string with hyphens.
     */
    public static String formatUuid(String uuid) {
        return uuid.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                "$1-$2-$3-$4-$5");
    }

    /**
     * Convert a local time to a player's time.
     * @param time the {@link LocalDateTime} to convert.
     * @param player the {@link Player} who's timezone is to be converted to.
     * @return A converted {@link ZonedDateTime} with the player timezone.
     * @throws NotFoundException if the player time could not be determined by {@link IpUtils}.
     */
    public static ZonedDateTime localTimeToPlayerTime(LocalDateTime time, Player player) throws NotFoundException {
        ZonedDateTime playerTime = IpUtils.ipToTime(player.getAddress().getAddress().getHostAddress());
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
    public static ZoneId getZoneIdOfPlayer(Player player) throws NotFoundException {
        ZonedDateTime playerTime = IpUtils.ipToTime(player.getAddress().getAddress().getHostAddress());
        if (playerTime == null) throw new NotFoundException("Player time could not be determined.");
        return playerTime.getZone();
    }

    /**
     * Removes all tribe-based permissions from the user and adds the one specified. This will run asynchronously, so
     * keep in mind that results may not be immediately up-to-date.
     * @param tribeIndex the tribe to set.
     * @param posIndex the position to set.
     */
    public static void setPlayerPerms(String playerUuid, int tribeIndex, int posIndex) {
        if (EyeOfOnyx.luckperms != null) {
            // Get user manager
            UserManager userManager = EyeOfOnyx.luckperms.getUserManager();

            // Get user
            CompletableFuture<User> userFuture = userManager.loadUser(UUID.fromString(playerUuid));

            userFuture.thenAcceptAsync(user -> {
                String[] tribes = RoyaltyBoard.getTribes();
                String[] positions = RoyaltyBoard.getValidPositions();

                // Remove all other groups
                for (int t = 0; t < tribes.length; t++) {
                    for (int p = 0; p < positions.length; p++) {
                        // Get the lp group name from config
                        String groupName = plugin.getConfig().getString(positions[p] + "." + tribes[t]);

                        if (groupName != null) {
                            // Get the group from lp and remove it from the user.
                            Group group = EyeOfOnyx.luckperms.getGroupManager().getGroup(groupName);
                            user.getInheritedGroups(user.getQueryOptions()).remove(group);

                        } else
                            Bukkit.getLogger().warning("Group " + positions[p] + "." + tribes[t] + " is null in the config!");
                    }
                }

                // Add the group
                String groupName = plugin.getConfig().getString(positions[posIndex] + "." + tribes[tribeIndex]);

                if (groupName != null) {
                    // Get the group from lp and add it to the user.
                    Group group = EyeOfOnyx.luckperms.getGroupManager().getGroup(groupName);
                    user.getInheritedGroups(user.getQueryOptions()).add(group);
                }

            });
        }
    }
}
