package io.github.stonley890.eyeofonyx.files;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BoardState {

    private final Map<Integer, BoardPosition> positions = new HashMap<>(9);

    public BoardState() {}

    /**
     * Get the position at the specified position index.
     * @param positionIndex the index of the position. 0 is ruler, 4 is noble presumptive.
     * @return the {@link BoardState} at that position.
     */
    public BoardPosition getPos(int positionIndex) {
        return positions.get(positionIndex);
    }

    public BoardState updatePosition(int positionIndex, BoardPosition position) {
        positions.put(positionIndex, position);
        return this;
    }

    /**
     * Convert a {@code Map<Integer, BoardState>} to a {@link YamlConfiguration}.
     * @param boardStates the {@code Map<Integer, BoardState>} to convert.
     * @return a {@link FileConfiguration} with the data.
     */
    public static @NotNull YamlConfiguration createYamlConfiguration(Map<Integer, BoardState> boardStates) {

        YamlConfiguration config = new YamlConfiguration();

        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
            for (int p = 0; p < RoyaltyBoard.getValidPositions().length; p++) {
                String uuid = "none";
                UUID player = boardStates.get(i).positions.get(p).player;
                if (player != null)
                    uuid = player.toString();

                String joinedBoard = "none";
                LocalDateTime joinedBoardTime = boardStates.get(i).positions.get(p).joinedBoard;
                if (joinedBoardTime != null)
                    joinedBoard = joinedBoardTime.toString();

                String joinedTime = "none";
                LocalDateTime joinedPosition = boardStates.get(i).positions.get(p).joinedPosition;
                if (joinedPosition != null)
                    joinedTime = joinedPosition.toString();

                String lastOnline = "none";
                LocalDateTime lastOnlineTime = boardStates.get(i).positions.get(p).lastOnline;
                if (lastOnlineTime != null)
                    lastOnline = lastOnlineTime.toString();

                String lastChallenge = "none";
                LocalDateTime lastChallengeTime = boardStates.get(i).positions.get(p).lastChallenge;
                if (lastChallengeTime != null)
                    lastChallenge = lastChallengeTime.toString();

                String challenger = "none";
                UUID challengerUuid = boardStates.get(i).positions.get(p).challenger;
                if (challengerUuid != null)
                    challenger = challengerUuid.toString();

                String challenging = "none";
                UUID challengingUuid = boardStates.get(i).positions.get(p).challenging;
                if (challengingUuid != null)
                    challenging = challengingUuid.toString();

                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".uuid", uuid);
                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".name", boardStates.get(i).positions.get(p).name);
                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".joined_board", joinedBoard);
                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".joined_time", joinedTime);
                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".last_online", lastOnline);
                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".last_challenge_time", lastChallenge);
                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".challenger", challenger);
                config.set(RoyaltyBoard.getTribes()[i] + "." + RoyaltyBoard.getValidPositions()[p] + ".challenging", challenging);
            }
        }

        return config;
    }

    /**
     * Convert a {@link FileConfiguration} to a {@code Map<Integer, BoardState>}.
     * @param config a {@link FileConfiguration} that contains royalty board information.
     * @return a {@code Map<Integer, BoardState>}.
     */
    public static @NotNull Map<Integer, BoardState> fromYamlConfig(FileConfiguration config) {

        Map<Integer, BoardState> yamlBoard = new HashMap<>(10);

        String[] tribes = RoyaltyBoard.getTribes();
        String[] positions = RoyaltyBoard.getValidPositions();

        for /* each tribe */ (int i = 0; i < tribes.length; i++) {
            String tribe = tribes[i];

            // Create a board state for this tribe
            BoardState boardState = new BoardState();

            for /* each position */ (int pos = 0; pos < positions.length; pos++) {
                String position = positions[pos];

                // Get UUID
                UUID uuid;
                String uuidString = config.getString(tribe + "." + position + ".uuid");
                // Attempt to parse
                try {
                    if (uuidString == null || uuidString.equals("none")) uuid = null;
                    else uuid = UUID.fromString(uuidString);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("UUID of tribe " + tribe.toUpperCase() + " position " + position.toUpperCase() + " could not be parsed. It will be overwritten as empty.");
                    uuid = null;
                }

                LocalDateTime joinedBoard;
                String joinedBoardString = config.getString(tribe + "." + position + ".joined_board");
                try {
                    if (joinedBoardString == null || joinedBoardString.equals("none") || joinedBoardString.equals("null")) joinedBoard = null;
                    else joinedBoard = LocalDateTime.parse(joinedBoardString);
                } catch (DateTimeParseException e) {
                    Bukkit.getLogger().warning("joined_board of tribe " + tribe.toUpperCase() + " position " + position.toUpperCase() + " could not be parsed. It will be overwritten as empty.");
                    joinedBoard = null;
                }

                LocalDateTime joinedTime;
                String joinedTimeString = config.getString(tribe + "." + position + ".joined_time");
                try {
                    if (joinedTimeString == null || joinedTimeString.equals("none") || joinedTimeString.equals("null")) joinedTime = null;
                    else joinedTime = LocalDateTime.parse(joinedTimeString);
                } catch (DateTimeParseException e) {
                    Bukkit.getLogger().warning("joined_time of tribe " + tribe.toUpperCase() + " position " + position.toUpperCase() + " could not be parsed. It will be overwritten as empty.");
                    joinedTime = null;
                }

                LocalDateTime lastOnline;
                String lastOnlineString = config.getString(tribe + "." + position + ".last_online");
                try {
                    if (lastOnlineString == null || lastOnlineString.equals("none") || lastOnlineString.equals("null")) lastOnline = null;
                    else lastOnline = LocalDateTime.parse(lastOnlineString);
                } catch (DateTimeParseException e) {
                    Bukkit.getLogger().warning("last_online of tribe " + tribe.toUpperCase() + " position " + position.toUpperCase() + " could not be parsed. It will be overwritten as empty.");
                    lastOnline = null;
                }

                LocalDateTime lastChallenge;
                String lastChallengeString = config.getString(tribe + "." + position + ".last_challenge_time");
                try {
                    if (lastChallengeString == null || lastChallengeString.equals("none") || lastChallengeString.equals("null")) lastChallenge = null;
                    else lastChallenge = LocalDateTime.parse(lastChallengeString);
                } catch (DateTimeParseException e) {
                    Bukkit.getLogger().warning("last_challenge_time of tribe " + tribe.toUpperCase() + " position " + position.toUpperCase() + " could not be parsed. It will be overwritten as empty.");
                    lastChallenge = null;
                }

                UUID challenger;
                String challengerString = config.getString(tribe + "." + position + ".challenger");
                try {
                    if (challengerString == null || challengerString.equals("none") || challengerString.equals("null")) challenger = null;
                    else challenger = UUID.fromString(challengerString);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("challenger of tribe " + tribe.toUpperCase() + " position " + position.toUpperCase() + " could not be parsed. It will be overwritten as empty.");
                    challenger = null;
                }

                UUID challenging;
                String challengingString = config.getString(tribe + "." + position + ".challenging");
                try {
                    if (challengingString == null || challengingString.equals("none") || challengingString.equals("null")) challenging = null;
                    else challenging = UUID.fromString(challengingString);
                } catch (IllegalArgumentException e) {
                    Bukkit.getLogger().warning("challenging of tribe " + tribe.toUpperCase() + " position " + position.toUpperCase() + " could not be parsed. It will be overwritten as empty.");
                    challenging = null;
                }

                // Set new position of BoardState we created earlier
                boardState.positions.put(pos, new BoardPosition(
                        uuid,
                        config.getString(tribe + "." + position + ".name"),
                        joinedBoard,
                        joinedTime,
                        lastOnline,
                        lastChallenge,
                        challenger,
                        challenging
                ));
            }

            // Add the completed BoardState to the map
            yamlBoard.put(i, boardState);
        }

        // Return the completed map
        return yamlBoard;

    }

    public BoardState setPlayer(int pos, UUID uuid) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.player = uuid;
        this.positions.put(pos, affectedPosition);
        return this;
    }
    public BoardState setName(int pos, String name) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.name = name;
        this.positions.put(pos, affectedPosition);
        return this;
    }
    public BoardState setJoinedBoard(int pos, LocalDateTime joinedBoard) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.joinedBoard = joinedBoard;
        this.positions.put(pos, affectedPosition);
        return this;
    }
    public BoardState setJoinedPosition(int pos, LocalDateTime joinedPosition) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.joinedPosition = joinedPosition;
        this.positions.put(pos, affectedPosition);
        return this;
    }
    public BoardState setLastOnline(int pos, LocalDateTime lastOnline) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.lastOnline = lastOnline;
        this.positions.put(pos, affectedPosition);
        return this;
    }
    public BoardState setLastChallenge(int pos, LocalDateTime lastChallenge) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.lastChallenge = lastChallenge;
        this.positions.put(pos, affectedPosition);
        return this;
    }
    public BoardState setChallenger(int pos, UUID challenger) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.challenger = challenger;
        this.positions.put(pos, affectedPosition);
        return this;
    }
    public BoardState setChallenging(int pos, UUID challenging) {
        BoardPosition affectedPosition = this.positions.get(pos);
        affectedPosition.challenging = challenging;
        this.positions.put(pos, affectedPosition);
        return this;
    }

    /**
     * Check if a given position of this tribe is empty.
     * @param pos the position to check.
     * @return whether the position is empty.
     */
    public boolean isEmpty(int pos) {
        return this.positions.get(pos).player == null;
    }

    /**
     * Clears all data from the specified position.
     * @param pos the position to clear
     * @return the modified {@link BoardState}.
     */
    public BoardState clear(int pos) {
        this.updatePosition(pos, new BoardPosition(null, null, null, null, null, null, null, null));
        return this;
    }

    /**
     * Moves a position to the location of another position, replacing it.
     * This will clear any attackers or defenders and update the {@code lastChallenge} value.
     * @param fromPos the position to move.
     * @param toPos the position to replace.
     * @return the modified {@link BoardState}.
     */
    public BoardState replace(int fromPos, int toPos) {

        BoardPosition movingPos = positions.get(fromPos);
        movingPos.challenging = null;
        movingPos.challenger = null;
        movingPos.lastChallenge = LocalDateTime.now();
        clear(toPos);
        positions.put(toPos, movingPos);
        clear(fromPos);
        return this;
    }

    /**
     * Swap two positions.
     * @param fromPos the first position to swap.
     * @param toPos the second position to swap.
     * @return the modified {@link BoardState}.
     */
    public BoardState swap(int fromPos, int toPos) {

        // Get positions
        BoardPosition pos1 = getPos(fromPos);
        BoardPosition pos2 = getPos(toPos);

        // Update data
        pos1.challenging = null;
        pos1.challenger = null;
        pos1.lastChallenge = LocalDateTime.now();

        pos2.challenging = null;
        pos2.challenger = null;
        pos2.lastChallenge = LocalDateTime.now();

        // Swap positions
        updatePosition(fromPos, pos2);
        updatePosition(toPos, pos1);
        return this;
    }

    /**
     * Create a non-referencing clone of this {@link BoardState}.
     * @return a clone of this {@link BoardState} that is not a reference variable.
     */
    @Override
    public BoardState clone() {
        BoardState clone;
        try {
            clone = (BoardState) super.clone();
        } catch (CloneNotSupportedException e) {
            clone = new BoardState();
        }
        clone.positions.putAll(this.positions);
        return clone;
    }

    /**
     * Check if this {@link BoardState} contains the same information as another.
     * @param obj the {@link BoardState} to compare this with.
     * @return whether the data is the same.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof BoardState that) {
            for (int pos = 0; pos < this.positions.size(); pos++) if (!this.positions.get(pos).equals(that.positions.get(pos))) return false;
            return true;
        }
        return false;
    }
}
