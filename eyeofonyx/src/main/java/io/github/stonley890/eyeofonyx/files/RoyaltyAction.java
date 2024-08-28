package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.data.Tribe;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class RoyaltyAction {

    public static final List<RoyaltyAction> actionHistory = new ArrayList<>();
    public static int lastId = -1;

    @Nullable
    public String executor; // null if Eye of Onyx automated system
    @NotNull
    public String reason;
    @NotNull
    public final Tribe affectedTribe;
    @NotNull
    public final BoardState oldState;
    @NotNull
    public final BoardState newState;

    public final int id;

    public RoyaltyAction(@Nullable String executor, @Nullable String reason, @NotNull Tribe affectedTribe, @NotNull BoardState oldState, @NotNull BoardState newState) {
        this.executor = executor;
        if (reason == null) this.reason = "Error: No reason specified.";
        else this.reason = reason;
        this.affectedTribe = affectedTribe;
        this.oldState = oldState;
        this.newState = newState;
        this.id = lastId + 1;
        lastId++;
        if (actionHistory.size() >= 20) actionHistory.remove(0);
        actionHistory.add(this);
    }

}

