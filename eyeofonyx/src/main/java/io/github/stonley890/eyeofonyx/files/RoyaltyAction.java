package io.github.stonley890.eyeofonyx.files;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class RoyaltyAction {

    public String executor; // null if Eye of Onyx automated system
    public ActionType type;
    public UUID affectedPlayer;
    public int affectedTribe;
    public int affectedPosition;

    public RoyaltyAction(@Nullable String executor, @NotNull ActionType type, UUID affectedPlayer, int affectedTribe, int affectedPosition) {
        this.executor = executor;
        this.type = type;
        this.affectedPlayer = affectedPlayer;
        this.affectedTribe = affectedTribe;
        this.affectedPosition = affectedPosition;
    }

}

