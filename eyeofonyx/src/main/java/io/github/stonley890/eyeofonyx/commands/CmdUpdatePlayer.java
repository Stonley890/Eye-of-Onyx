package io.github.stonley890.eyeofonyx.commands;

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
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CmdUpdatePlayer implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

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
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "No players were selected.");
                return true;
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

                // Success
                PlayerTribe.updateTribeOfPlayer(player);

                for (int t = 0; t < RoyaltyBoard.getTribes().length; t++) {
                    for (int p = 0; p < RoyaltyBoard.getValidPositions().length; p++) {

                        if (RoyaltyBoard.getUuid(t,p).equals(player.getUniqueId()) && t != PlayerTribe.getTribeOfPlayer(player.getUniqueId())) {

                            // Notify attacker if exists
                            UUID attacker = RoyaltyBoard.getAttacker(t,p);
                            if (attacker != null) {
                                int attackerPos = RoyaltyBoard.getPositionIndexOfUUID(attacker);
                                RoyaltyBoard.setAttacking(t, attackerPos, null);
                                new Notification(attacker, "Your challenge was canceled.", "The player you were challenging was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                            }

                            // Notify defender if exists
                            if (p != RoyaltyBoard.RULER) {
                                UUID attacking = RoyaltyBoard.getAttacking(t,p);
                                if (attacking != null) {
                                    int defenderPos = RoyaltyBoard.getPositionIndexOfUUID(attacking);
                                    RoyaltyBoard.setAttacker(t, defenderPos, null);
                                    new Notification(attacker, "Your challenge was canceled.", "The player who was challenging you was removed from the royalty board, so your challenge was canceled.", NotificationType.GENERIC).create();
                                }
                            }

                            // Remove any challenges
                            for (Challenge challenge : Challenge.getChallenges()) {
                                if (challenge.defender.equals(player.getUniqueId()) || challenge.attacker.equals(player.getUniqueId())) Challenge.remove(challenge);
                            }

                            // Remove any challenge notifications
                            for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId())) {
                                if (notification.type == NotificationType.CHALLENGE_ACCEPTED || notification.type == NotificationType.CHALLENGE_REQUESTED) Notification.removeNotification(notification);
                            }

                            RoyaltyBoard.removePlayer(t, p);
                            RoyaltyBoard.updateBoard();
                            new Notification(player.getUniqueId(), "You have been removed from the royalty board.", "You were removed from the royalty board because you changed your tribe. Any pending challenges have been canceled.", NotificationType.GENERIC).create();
                        }

                    }
                }

            } catch (InvalidObjectException | NotFoundException e) {
                // Player is offline
                // Will not happen
            } catch (IOException | InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
        }

        sender.sendMessage(EyeOfOnyx.EOO + "Updated " + targets.size() + " players.");

        return true;
    }
}
