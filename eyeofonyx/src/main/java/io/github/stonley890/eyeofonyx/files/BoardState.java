package io.github.stonley890.eyeofonyx.files;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.LocalDateTime;
import java.util.*;

public class BoardState {

    private final Map<Integer, BoardPosition> positions = new HashMap<>(5);

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

    public static YamlConfiguration createYamlConfiguration(Map<Integer, BoardState> boardStates) {

        YamlConfiguration config = new YamlConfiguration();

        for (int i = 0; i < RoyaltyBoard.getTribes().length; i++) {
            for (int p = 0; p < RoyaltyBoard.getValidPositions().length; p++) {
                String uuid = "none";
                if (boardStates.get(i).positions.get(p).player != null) uuid = boardStates.get(i).positions.get(p).player.toString();

                String joinedBoard = "none";
                if (boardStates.get(i).positions.get(p).joinedBoard != null) joinedBoard = boardStates.get(i).positions.get(p).joinedBoard.toString();

                String joinedTime = "none";
                if (boardStates.get(i).positions.get(p).joinedPosition != null) joinedTime = boardStates.get(i).positions.get(p).joinedPosition.toString();

                String lastOnline = "none";
                if (boardStates.get(i).positions.get(p).lastOnline != null) lastOnline = boardStates.get(i).positions.get(p).lastOnline.toString();

                String lastChallenge = "none";
                if (boardStates.get(i).positions.get(p).lastChallenge != null) lastChallenge = boardStates.get(i).positions.get(p).lastChallenge.toString();

                String challenger = "none";
                if (boardStates.get(i).positions.get(p).challenger != null) challenger = boardStates.get(i).positions.get(p).challenger.toString();

                String challenging = "none";
                if (boardStates.get(i).positions.get(p).challenging != null) challenging = boardStates.get(i).positions.get(p).challenging.toString();

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

    public static Map<Integer, BoardState> fromYamlConfig(FileConfiguration config) {

        Map<Integer, BoardState> yamlBoard = new HashMap<>(10);

        String[] tribes = RoyaltyBoard.getTribes();
        String[] positions = RoyaltyBoard.getValidPositions();

        for (int i = 0; i < tribes.length; i++) {
            String tribe = tribes[i];

            BoardState boardState = new BoardState();

            for (int p = 0; p < positions.length; p++) {

                UUID uuid;
                String uuidString = config.getString(tribe + "." + positions[p] + ".uuid");
                if (uuidString == null || uuidString.equals("none")) uuid = null;
                else uuid = UUID.fromString(uuidString);

                LocalDateTime joinedBoard;
                String joinedBoardString = config.getString(tribe + "." + positions[p] + ".joined_board");
                if (joinedBoardString == null || joinedBoardString.equals("none") || joinedBoardString.equals("null")) joinedBoard = null;
                else joinedBoard = LocalDateTime.parse(joinedBoardString);

                LocalDateTime joinedTime;
                String joinedTimeString = config.getString(tribe + "." + positions[p] + ".joined_time");
                if (joinedTimeString == null || joinedTimeString.equals("none") || joinedTimeString.equals("null")) joinedTime = null;
                else joinedTime = LocalDateTime.parse(joinedTimeString);

                LocalDateTime lastOnline;
                String lastOnlineString = config.getString(tribe + "." + positions[p] + ".last_online");
                if (lastOnlineString == null || lastOnlineString.equals("none") || lastOnlineString.equals("null")) lastOnline = null;
                else lastOnline = LocalDateTime.parse(lastOnlineString);

                LocalDateTime lastChallenge;
                String lastChallengeString = config.getString(tribe + "." + positions[p] + ".last_online");
                if (lastChallengeString == null || lastChallengeString.equals("none") || lastChallengeString.equals("null")) lastChallenge = null;
                else lastChallenge = LocalDateTime.parse(lastChallengeString);

                UUID challenger;
                String challengerString = config.getString(tribe + "." + positions[p] + ".uuid");
                if (challengerString == null || challengerString.equals("none") || challengerString.equals("null")) challenger = null;
                else challenger = UUID.fromString(challengerString);

                UUID challenging;
                String challengingString = config.getString(tribe + "." + positions[p] + ".uuid");
                if (challengingString == null || challengingString.equals("none") || challengingString.equals("null")) challenging = null;
                else challenging = UUID.fromString(challengingString);

                boardState.positions.put(p, new BoardPosition(
                        uuid,
                        config.getString(tribe + "." + positions[p] + ".name"),
                        joinedBoard,
                        joinedTime,
                        lastOnline,
                        lastChallenge,
                        challenger,
                        challenging
                ));
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
        clear(fromPos);
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

    public boolean equals(BoardState state) {
        for (int pos = 0; pos < this.positions.size(); pos++) if (!this.positions.get(pos).equals(state.positions.get(pos))) return false;
        return true;
    }
}
