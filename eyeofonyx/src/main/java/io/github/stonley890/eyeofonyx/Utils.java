package io.github.stonley890.eyeofonyx;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import javassist.NotFoundException;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

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
