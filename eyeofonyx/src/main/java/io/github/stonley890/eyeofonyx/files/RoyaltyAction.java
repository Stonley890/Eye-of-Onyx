package io.github.stonley890.eyeofonyx.files;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RoyaltyAction {

    public static List<RoyaltyAction> actionHistory = new ArrayList<>();
    public static int lastId = -1;

    @Nullable
    public String executor; // null if Eye of Onyx automated system
    public int affectedTribe;
    @NotNull
    public BoardState oldState;
    @NotNull
    public BoardState newState;

    public int id;

    public RoyaltyAction(@Nullable String executor, int affectedTribe, @NotNull BoardState oldState, @NotNull BoardState newState) {
        this.executor = executor;
        this.affectedTribe = affectedTribe;
        this.oldState = oldState;
        this.newState = newState;
        this.id = lastId + 1;
        lastId++;
        if (actionHistory.size() >= 20) actionHistory.remove(0);
        actionHistory.add(this);
    }

}

