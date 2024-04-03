package io.github.stonley890.eyeofonyx.files;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
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

    public BoardPosition(@Nullable UUID uuid, @Nullable String name, @Nullable LocalDateTime joinedBoard, @Nullable LocalDateTime joinedPosition, @Nullable LocalDateTime lastOnline, @Nullable LocalDateTime lastChallenge, @Nullable UUID challenger, @Nullable UUID challenging) {

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

    public boolean equals(@NotNull BoardPosition that) {
        if (this.player != that.player) return false;
        if (!Objects.equals(this.name, that.name)) return false;
        if (!Objects.equals(this.joinedBoard, that.joinedBoard)) return false;
        if (!Objects.equals(this.joinedPosition, that.joinedPosition)) return false;
        if (!Objects.equals(this.lastOnline, that.lastOnline)) return false;
        if (!Objects.equals(this.lastChallenge, that.lastChallenge)) return false;
        if (this.challenger != that.challenger) return false;
        return this.challenging == that.challenging;
    }

}
