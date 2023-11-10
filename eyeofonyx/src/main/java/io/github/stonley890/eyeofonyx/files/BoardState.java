package io.github.stonley890.eyeofonyx.files;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.LocalDateTime;
import java.util.*;

public class BoardState {

    private final Map<Integer, BoardPosition> positions = new HashMap<>(5);

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

    public static YamlConfiguration createYamlConfiguration(Map<Integer, BoardState> boardStates) {

        YamlConfiguration config = new YamlConfiguration();

        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
            for (int p = 0; p < RoyaltyBoard.getValidPositions().length; p++) {
                config.set(RoyaltyBoard.getTribes()[i] + ".uuid", boardStates.get(i).positions.get(p).player);
                config.set(RoyaltyBoard.getTribes()[i] + ".name", boardStates.get(i).positions.get(p).name);
                config.set(RoyaltyBoard.getTribes()[i] + ".joined_board", boardStates.get(i).positions.get(p).joinedBoard);
                config.set(RoyaltyBoard.getTribes()[i] + ".joined_time", boardStates.get(i).positions.get(p).joinedPosition);
                config.set(RoyaltyBoard.getTribes()[i] + ".last_online", boardStates.get(i).positions.get(p).lastOnline);
                config.set(RoyaltyBoard.getTribes()[i] + ".last_challenge_time", boardStates.get(i).positions.get(p).lastChallenge);
                config.set(RoyaltyBoard.getTribes()[i] + ".challenger", boardStates.get(i).positions.get(p).challenger);
                config.set(RoyaltyBoard.getTribes()[i] + ".challenging", boardStates.get(i).positions.get(p).challenging);
            }
        }

        return config;
    }

    public static Map<Integer, BoardState> fromYamlConfig(FileConfiguration config) {

        Map<Integer, BoardState> yamlBoard = new HashMap<>(10);

        String[] tribes = RoyaltyBoard.getTribes();
        String[] positions = RoyaltyBoard.getValidPositions();

        for (int i = 0; i < tribes.length; i++) {
            String tribe = tribes[i];

            BoardState boardState = new BoardState();

            for (int p = 0; p < positions.length; p++) {
                boardState.positions.put(p, new BoardPosition(
                        (UUID) config.get(tribe + "." + positions[p] + ".uuid"),
                        config.getString(tribe + "." + positions[p] + ".name"),
                        LocalDateTime.parse(config.getString(tribe + "." + positions[p] + ".joined_board")),
                        LocalDateTime.parse(config.getString(tribe + "." + positions[p] + ".joined_time")),
                        LocalDateTime.parse(config.getString(tribe + "." + positions[p] + ".last_online")),
                        LocalDateTime.parse(config.getString(tribe + "." + positions[p] + ".last_challenge_time")),
                        (UUID) config.get(tribe + "." + positions[p] + ".challenger"),
                        (UUID) config.get(tribe + "." + positions[p] + ".challenging")));
            }

            yamlBoard.put(i, boardState);
        }

        return yamlBoard;

    }

    public BoardState setPlayer(int tribe, UUID uuid) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.player = uuid;
        this.positions.put(tribe, affectedPosition);
        return this;
    }
    public BoardState setName(int tribe, String name) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.name = name;
        this.positions.put(tribe, affectedPosition);
        return this;
    }
    public BoardState setJoinedBoard(int tribe, LocalDateTime joinedBoard) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.joinedBoard = joinedBoard;
        this.positions.put(tribe, affectedPosition);
        return this;
    }
    public BoardState setJoinedPosition(int tribe, LocalDateTime joinedPosition) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.joinedPosition = joinedPosition;
        this.positions.put(tribe, affectedPosition);
        return this;
    }
    public BoardState setLastOnline(int tribe, LocalDateTime lastOnline) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.lastOnline = lastOnline;
        this.positions.put(tribe, affectedPosition);
        return this;
    }
    public BoardState setLastChallenge(int tribe, LocalDateTime lastChallenge) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.lastChallenge = lastChallenge;
        this.positions.put(tribe, affectedPosition);
        return this;
    }
    public BoardState setChallenger(int tribe, UUID challenger) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.challenger = challenger;
        this.positions.put(tribe, affectedPosition);
        return this;
    }
    public BoardState setChallenging(int tribe, UUID challenging) {
        BoardPosition affectedPosition = this.positions.get(tribe);
        affectedPosition.challenging = challenging;
        this.positions.put(tribe, affectedPosition);
        return this;
    }

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
     * This will clear any attackers or defenders and update the lastChallenge value.
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
        return this;
    }

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

}
