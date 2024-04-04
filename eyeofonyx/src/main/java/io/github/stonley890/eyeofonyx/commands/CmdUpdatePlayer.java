package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.*;
import javassist.NotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CmdUpdatePlayer implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

        List<Player> targets = new ArrayList<>();

        if (args.length == 0) {
            // If no arguments, do self (if player)
            if (sender instanceof Player player) {
                targets.add(player);
            } else {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments! /updateplayer <targets>");
                return true;
            }
        } else if (args.length == 1) {

            // Use vanilla target selector args
            List<Entity> entities;
            try {
                entities = Bukkit.selectEntities(sender, args[0]);
            } catch (IllegalArgumentException e) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! /updateplayer <targets>");
                return true;
            }

            // Check if empty
            if (entities.isEmpty()) {

                // try offline search
                UUID offlineUuid = PlayerUtility.getUUIDOfUsername(args[0]);
                if (offlineUuid == null) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Could not find a player by that name.");
                    return true;
                }

                String username = args[0];

                try {

                    doUpdate(offlineUuid);

                    sender.sendMessage(EyeOfOnyx.EOO + "Updated " + username + ".");
                    return true;

                } catch (NotFoundException e) {

                    sender.sendMessage(EyeOfOnyx.EOO + username + " does not have a tribe-associated team and is not online.");
                    return true;

                } catch (IOException | InvalidConfigurationException e) {

                    sender.sendMessage(EyeOfOnyx.EOO + "There was a problem accessing one or more files. Check logs for stacktrace.");
                    throw new RuntimeException(e);

                }

            }

            // Check for non-players
            for (Entity entity : entities) {
                if (entity instanceof Player player) {
                    targets.add(player);
                } else {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This command is only applicable to players.");
                    return true;
                }
            }

        } else {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Too many arguments! /updateplayer <targets>");
            return true;
        }

        for (Player player : targets) {
            try {

                doUpdate(player.getUniqueId());

            } catch (IOException | InvalidConfigurationException e) {

                sender.sendMessage(EyeOfOnyx.EOO + "There was a problem accessing one or more files. Check logs for stacktrace.");
                throw new RuntimeException(e);

            } catch (NotFoundException e) {
                sender.sendMessage(EyeOfOnyx.EOO + player.getName() + " does not have a tribe-associated team or tag.");
                return true;
            }
        }

        sender.sendMessage(EyeOfOnyx.EOO + "Updated " + targets.size() + " players.");

        return true;
    }

    private static void doUpdate(UUID uuid) throws NotFoundException, IOException, InvalidConfigurationException {

        // Success
        PlayerTribe.updateTribeOfPlayer(uuid);

        for (int t = 0; t < RoyaltyBoard.getTribes().length; t++) {
            for (int p = 0; p < RoyaltyBoard.getValidPositions().length; p++) {

                if (Objects.equals(RoyaltyBoard.getUuid(t,p), uuid) && t != PlayerTribe.getTribeOfPlayer(uuid)) {

                    // Notify attacker if exists
                    UUID attacker = RoyaltyBoard.getAttacker(t,p);
                    if (attacker != null) {
                        int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                        RoyaltyBoard.setAttacking(t, attackerPos, null);
                        new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", Notification.Type.GENERIC).create();
                    }

                    // Notify defender if exists
                    if (p != RoyaltyBoard.RULER) {
                        UUID attacking = RoyaltyBoard.getAttacking(t,p);
                        if (attacking != null) {
                            int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                            RoyaltyBoard.setAttacker(t, defenderPos, null);
                            new Notification(attacker, "Your challenge was canceled.", "The player who was challenging you was removed from the royalty board, so your challenge was canceled.", Notification.Type.GENERIC).create();
                        }
                    }

                    // Remove any challenges
                    for (Challenge challenge : Challenge.getChallenges()) {
                        if (challenge.defender.equals(uuid) || challenge.attacker.equals(uuid)) Challenge.remove(challenge);
                    }

                    // Remove any challenge notifications
                    for (Notification notification : Notification.getNotificationsOfPlayer(uuid)) {
                        if (notification.type == Notification.Type.CHALLENGE_ACCEPTED || notification.type == Notification.Type.CHALLENGE_REQUESTED) Notification.removeNotification(notification);
                    }

                    RoyaltyBoard.removePlayer(t, p, true);
                    RoyaltyBoard.updateBoard(t, false);
                    RoyaltyBoard.updateDiscordBoard(t);
                    new Notification(uuid, "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. Any pending challenges have been canceled.", Notification.Type.GENERIC).create();
                }

            }
        }
    }

}
