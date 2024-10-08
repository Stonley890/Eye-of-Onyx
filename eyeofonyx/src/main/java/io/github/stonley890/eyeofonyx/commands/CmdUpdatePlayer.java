package io.github.stonley890.eyeofonyx.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.ExecutableCommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.dreamvisitor.data.TribeUtil;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class CmdUpdatePlayer {

    private static void doUpdate(UUID uuid) throws IOException {

        Dreamvisitor.debug("Doing player update.");
        // Success
        for (int t = 0; t < TribeUtil.tribes.length; t++) {
            Tribe tribe = TribeUtil.tribes[t];
            Dreamvisitor.debug("Checking tribe " + tribe);
            for (int p = 0; p < RoyaltyBoard.getValidPositions().length; p++) {
                Dreamvisitor.debug("Checking pos " + p);

                if (Objects.equals(RoyaltyBoard.getUuid(tribe,p), uuid)) {
                    Dreamvisitor.debug("Found board position " + p);
                    if (tribe != PlayerTribe.getTribeOfPlayer(uuid)) {

                        Dreamvisitor.debug("Tribe does not match.");
                        Challenge challenge = Challenge.getChallenge(uuid);

                        // Notify attacker if exists
                        if (challenge != null) {
                            UUID attacker = challenge.attacker;
                            new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", Notification.Type.GENERIC).create();
                            Dreamvisitor.debug("Notified attacker.");
                        }

                        // Notify defender if exists
                        if (p != RoyaltyBoard.RULER) {
                            if (challenge != null) {
                                UUID defender = challenge.defender;
                                new Notification(defender, "Your challenge was canceled.", "The player who was challenging you was removed from the royalty board, so your challenge was canceled.", Notification.Type.GENERIC).create();
                            }
                            Dreamvisitor.debug("Notified defender.");
                        }

                        // Remove any challenges
                        if (challenge != null) Challenge.remove(challenge);
                        Dreamvisitor.debug("Removed challenges.");

                        // Remove any challenge notifications
                        for (Notification notification : Notification.getNotificationsOfPlayer(uuid)) {
                            if (notification.type == Notification.Type.CHALLENGE_ACCEPTED || notification.type == Notification.Type.CHALLENGE_REQUESTED) Notification.removeNotification(notification);
                            Dreamvisitor.debug("Removed challenge notifications.");
                        }

                        BoardState oldBoard = RoyaltyBoard.getBoardOf(tribe);
                        RoyaltyBoard.removePlayer(tribe, p, true);
                        RoyaltyBoard.updateBoard(tribe, false, false);
                        RoyaltyBoard.updateDiscordBoard(tribe);
                        RoyaltyBoard.reportChange(new RoyaltyAction("Command Executor", null, tribe, oldBoard, RoyaltyBoard.getBoardOf(tribe)));
                        Dreamvisitor.debug("Board updated.");
                        new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. Any pending challenges have been canceled.", Notification.Type.GENERIC).create();
                    }

                }

            }
        }

        Dreamvisitor.debug("Updating permissions...");
        RoyaltyBoard.updatePermissions(uuid);
    }

    @NotNull
    public ExecutableCommand<?, ?> getCommand() {
        return new CommandAPICommand("updateplayer")
                .withHelp("Update a player's royalty status.", "Auto-update a player's royalty roles and groups.")
                .withPermission(CommandPermission.OP)
                .withArguments(new EntitySelectorArgument.ManyPlayers("players"))
                .executesNative((sender, args) -> {

                    Collection<Player> targets = (Collection<Player>) args.get("players");

                    assert targets != null;
                    for (Player player : targets) {
                        try {
                            doUpdate(player.getUniqueId());
                        } catch (IOException e) {

                            sender.sendMessage(EyeOfOnyx.EOO + "There was a problem accessing one or more files. Check logs for stacktrace.");
                            throw new RuntimeException(e);

                        }
                    }

                    sender.sendMessage(EyeOfOnyx.EOO + "Updated " + targets.size() + " players.");
                });
    }
}
