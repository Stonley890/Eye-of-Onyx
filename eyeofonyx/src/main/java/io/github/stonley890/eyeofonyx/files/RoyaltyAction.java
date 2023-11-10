package io.github.stonley890.eyeofonyx.files;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class RoyaltyAction {

    @Nullable
    public String executor; // null if Eye of Onyx automated system
    public int affectedTribe;
    @NotNull
    public BoardState oldState;
    @NotNull
    public BoardState newState;

    public RoyaltyAction(@Nullable String executor, int affectedTribe, @NotNull BoardState oldState, @NotNull BoardState newState) {
        this.executor = executor;
        this.affectedTribe = affectedTribe;
        this.oldState = oldState;
        this.newState = newState;
    }

}

