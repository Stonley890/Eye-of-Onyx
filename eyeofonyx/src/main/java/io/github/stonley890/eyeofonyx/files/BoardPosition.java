package io.github.stonley890.eyeofonyx.files;

import org.bukkit.configuration.file.YamlConfiguration;

import javax.annotation.Nullable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BoardPosition {

    /**
     * The UUID of the player who holds this position. If {@code null}, this position must be empty.
     */
    @Nullable
    public UUID player;
    @Nullable
    public String name;
    @Nullable
    public LocalDateTime joinedBoard;
    @Nullable
    public LocalDateTime joinedPosition;
    @Nullable
    public LocalDateTime lastOnline;
    @Nullable
    public LocalDateTime lastChallenge;
    @Nullable
    public UUID challenger;
    @Nullable
    public UUID challenging;

    public BoardPosition(UUID uuid, String name, LocalDateTime joinedBoard, LocalDateTime joinedPosition, LocalDateTime lastOnline, LocalDateTime lastChallenge, UUID challenger, UUID challenging) {

        this.player = uuid;
        this.name = name;
        this.joinedBoard = joinedBoard;
        this.joinedPosition = joinedPosition;
        this.lastOnline = lastOnline;
        this.lastChallenge = lastChallenge;
        this.challenger = challenger;
        this.challenging = challenging;

    }

    public BoardPosition setPlayer(UUID uuid) {
        this.player = uuid;
        return this;
    }
    public BoardPosition setName(String name) {
        this.name = name;
        return this;
    }
    public BoardPosition setJoinedBoard(LocalDateTime joinedBoard) {
        this.joinedBoard = joinedBoard;
        return this;
    }
    public BoardPosition setJoinedPosition(LocalDateTime joinedPosition) {
        this.joinedPosition = joinedPosition;
        return this;
    }
    public BoardPosition setLastOnline(LocalDateTime lastOnline) {
        this.lastOnline = lastOnline;
        return this;
    }
    public BoardPosition setLastChallenge(LocalDateTime lastChallenge) {
        this.lastChallenge = lastChallenge;
        return this;
    }
    public BoardPosition setChallenger(UUID challenger) {
        this.challenger = challenger;
        return this;
    }
    public BoardPosition setChallenging(UUID challenging) {
        this.challenging = challenging;
        return this;
    }

    /**
     * Converts this {@link BoardPosition} to a {@link YamlConfiguration}-compatible list of objects. If an object is null, it will be transformed into the {@link String} "none"
     * @return a {@link List<Object>} containing never-null values.
     */
    public List<Object> toObjectList() {

        List<Object> list = new ArrayList<>();

        list.add(noneIfNull(player));
        list.add(noneIfNull(name));
        list.add(noneIfNull(joinedBoard));
        list.add(noneIfNull(joinedPosition));
        list.add(noneIfNull(lastOnline));
        list.add(noneIfNull(lastChallenge));
        list.add(noneIfNull(challenger));
        list.add(noneIfNull(challenging));

        return list;
    }

    private Object noneIfNull(Object object) {

        if (object == null) return "none";
        else return object;

    }

}
