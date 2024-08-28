package io.github.stonley890.eyeofonyx.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.*;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.challenges.Competition;
import io.github.stonley890.eyeofonyx.files.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.github.stonley890.eyeofonyx.files.RoyaltyBoard.*;

public class CmdChallenge {

    final String[] positions = RoyaltyBoard.getValidPositions();

    public static final List<Player> playersOnForm = new ArrayList<>();
    public static final List<String> codesOnForm = new ArrayList<>();

    public static final Map<Player, Player> quickChallenges = new HashMap<>();

    private static void date(@NotNull CommandSender sender, @NotNull CommandArguments args, Player player, Challenge challenge) {
        if (challenge == null) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not in any scheduled challenges!");
            return;
        }

        if (challenge.state == Challenge.State.SCHEDULED) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "This challenge is already finalized!");
            return;
        }

        int selectedTimeIndex;
        String arg = (String) args.get("arg2");
        assert arg != null;
        try {
            selectedTimeIndex = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments!");
            return;
        }

        Challenge.remove(challenge);

        LocalDateTime selectedTime = challenge.time.get(selectedTimeIndex);
        challenge.time.clear();
        challenge.time.add(selectedTime);
        challenge.state = Challenge.State.SCHEDULED;

        challenge.save();

        report(player.getName(), player.getName() + "'s challenge with " + PlayerUtility.getUsernameOfUuid(challenge.defender) + " has been scheduled for " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")));

        sender.sendMessage(EyeOfOnyx.EOO + "Time confirmed! Your challenge will take place " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + "!");
        new Notification(challenge.defender, "Challenge date confirmed!", "The time of your challenge with " + PlayerUtility.getUsernameOfUuid(challenge.attacker.toString()) + " will be " + selectedTime.format(DateTimeFormatter.ofPattern("MM/dd hh:mm a")) + ".", Notification.Type.GENERIC).create();

        // Remove CHALLENGE_ACCEPTED notification
        for (Notification notification : Notification.getNotificationsOfPlayer(player.getUniqueId())) {
            if (notification.type == Notification.Type.CHALLENGE_ACCEPTED)
                Notification.removeNotification(notification);
        }
    }

    @SuppressWarnings("unchecked")
    private static void start(@NotNull CommandSender sender, @NotNull Player player) {
        if ((Competition.activeChallenge.defender.equals(player.getUniqueId()) || Competition.activeChallenge.attacker.equals(player.getUniqueId())) && !Competition.activeChallenge.started) {

            List<Location> waitingRooms = (List<Location>) EyeOfOnyx.getPlugin().getConfig().getList("waiting-rooms");
            if (waitingRooms != null) {
                if (waitingRooms.get(TribeUtil.indexOf(Competition.activeChallenge.tribe)) != null) {
                    player.teleport(waitingRooms.get(TribeUtil.indexOf(Competition.activeChallenge.tribe)));
                } else
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "No waiting room set! Contact an admin.");
            } else
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "No waiting room set! Contact an admin.");
        }
    }

    private static void denyconfirm(@NotNull CommandSender sender, @NotNull Player player, Tribe playerTribe, int playerPosition) {

        Challenge challenge = Challenge.getChallenge(player.getUniqueId());
        if (challenge == null) {
            sender.sendMessage(EyeOfOnyx.EOO + "Your challenge could not be found! Contact a staff member.");
            return;
        }
        int attackerPosition = getPositionIndexOfUUID(challenge.attacker);

        // Remove notification
        List<Notification> notificationList = Notification.getNotificationsOfPlayer(player.getUniqueId());
        for (Notification notification : notificationList) {
            if (notification.type == Notification.Type.CHALLENGE_REQUESTED)
                Notification.removeNotification(notification);
        }

        // Remove from royalty board
        BoardState oldBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
        RoyaltyBoard.removePlayer(playerTribe, playerPosition, true);
        RoyaltyBoard.replace(playerTribe, playerPosition, attackerPosition);
        RoyaltyBoard.updateBoard(playerTribe, false, false);
        BoardState newBoard = RoyaltyBoard.getBoardOf(playerTribe).clone();
        reportChange(new RoyaltyAction(sender.getName(), "The player denied a challenge.", playerTribe, oldBoard, newBoard));

        sender.sendMessage(EyeOfOnyx.EOO + "You have been removed from the royalty board.");

        RoyaltyBoard.updateDiscordBoard(playerTribe);
    }

    private static void deny(@NotNull CommandSender sender) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO).append("Are you sure you want to forfeit your role to your challenger and remove yourself from the royalty board? ")
                .append("This action cannot be undone.").color(ChatColor.RED).append("\n");

        // Add accept and forfeit buttons
        TextComponent accept = new TextComponent();
        accept.setText("[Accept Challenge]");
        accept.setColor(ChatColor.GREEN);
        accept.setUnderlined(true);
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge accept"));

        TextComponent deny = new TextComponent();
        deny.setText("[Forfeit]");
        deny.setColor(ChatColor.RED);
        deny.setUnderlined(true);
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge denyconfirm"));

        builder.append(accept).append(" ").reset().append(deny).append("\n");

        sender.spigot().sendMessage(builder.create());
    }

    private static void accept(@NotNull CommandSender sender, Player player) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO).append("There's one more step before you can accept! Use the link below to select times that you are available to attend the challenge. You'll need the code below for validation. If you need help, contact a staff member.").append("\n");

        TextComponent link = new TextComponent("[Submit Availability]");
        link.setColor(ChatColor.YELLOW);
        link.setUnderlined(true);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, EyeOfOnyx.getPlugin().getConfig().getString("address") + "/availability"));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to open the web form.")));

        String id = String.format("%04d", new Random().nextInt(10000));

        TextComponent code = new TextComponent("[" + id + "]");
        code.setColor(ChatColor.YELLOW);
        code.setUnderlined(true);
        code.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id));
        code.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to copy the code.")));

        builder.append(link).append(" ").reset().append(code).append("\n").reset()
                .append("The code will expire in five minutes.").color(ChatColor.RED).append("\n");

        playersOnForm.add(player);
        codesOnForm.add(id);

        sender.spigot().sendMessage(builder.create());
    }

    private void initiate(@NotNull CommandSender sender, @NotNull String action, boolean quick, @NotNull Player player, int playerPosition, Tribe playerTribe) {
        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO);

        int targetPosition;

        // Check for active challenge
        if (Competition.activeChallenge != null) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You cannot initiate a challenge while one is in progress!");
            return;
        }

        // Make sure challenge is valid
        if (playerPosition == CIVILIAN) {

            // Check for any empty positions
            int nextEmptyPosition = CIVILIAN;

            // Iterate through positions (start at ruler)
            for (int posToCheck = 0; posToCheck < positions.length; posToCheck++) {
                nextEmptyPosition = posToCheck;
                // If position is empty, break
                if (getUuid(playerTribe, posToCheck) == null) break;
                nextEmptyPosition = CIVILIAN;
            }

            // If a position is available, do that
            if (nextEmptyPosition < CIVILIAN) {
                BoardState oldBoard = getBoardOf(playerTribe).clone();
                RoyaltyBoard.set(playerTribe, nextEmptyPosition, new BoardPosition(player.getUniqueId(), null, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()));
                RoyaltyBoard.updatePermissions(player.getUniqueId(), playerTribe, nextEmptyPosition);
                updateBoard(playerTribe, true, false);
                BoardState newBoard = getBoardOf(playerTribe).clone();
                reportChange(new RoyaltyAction(player.getName(), "Player assumed an empty position.", playerTribe, oldBoard, newBoard));

                sender.sendMessage(EyeOfOnyx.EOO + "You are now " + getValidPositions()[nextEmptyPosition].toUpperCase().replace("_", " ") + " of the " + playerTribe.getTeamName() + "s!");
                RoyaltyBoard.updateDiscordBoard(playerTribe);
                return;
            }

            Dreamvisitor.debug("No extra positions available.");

            // Determine which position is being targeted
            switch (action) {
                case "position1" -> targetPosition = NOBLE1;
                case "position2" -> targetPosition = NOBLE2;
                case "position3" -> targetPosition = NOBLE3;
                case "position4" -> targetPosition = NOBLE4;
                case "position5" -> targetPosition = NOBLE5;
                default -> {
                    builder.color(ChatColor.RED).append("Invalid arguments!");
                    sender.spigot().sendMessage(builder.create());
                    return;
                }
            }

        } else if (playerPosition >= NOBLE1 && playerPosition <= NOBLE5) {

            // Determine which position is being targeted
            switch (action) {
                case "position1" -> targetPosition = HEIR1;
                case "position2" -> targetPosition = HEIR2;
                case "position3" -> targetPosition = HEIR3;
                default -> {
                    builder.color(ChatColor.RED).append("Invalid arguments!");
                    sender.spigot().sendMessage(builder.create());
                    return;
                }
            }

        } else if (playerPosition >= HEIR1 && playerPosition <= HEIR3) {
            targetPosition = RULER;
        } else if (playerPosition == RULER) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are ruler! No one to challenge.");
            return;
        } else {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Unaccounted situation! playerPosition " + playerPosition);
            return;
        }

        // Check that target is not being challenged
        if (!(Challenge.getChallenge(player.getUniqueId()) == null || (targetPosition != RULER && Challenge.getChallenge(player.getUniqueId()) != null))) {
            builder.append("That player is already in a challenge!");
            sender.spigot().sendMessage(builder.create());
        } else {
            if (RoyaltyBoard.isPosFrozen(playerTribe, targetPosition)) {
                builder.append("That position is frozen and cannot be challenged.");
                sender.spigot().sendMessage(builder.create());
                return;
            }

            // Unless position is empty...
            if (!RoyaltyBoard.isPositionEmpty(playerTribe, targetPosition)) {

                if (RoyaltyBoard.isOnCoolDown(playerTribe, targetPosition)) {
                    builder.append("That player is on movement cooldown until ")
                            .append(Objects.requireNonNull(getCooldownEnd(playerTribe, targetPosition)).format(DateTimeFormatter.ISO_DATE));
                    sender.spigot().sendMessage(builder.create());
                    return;
                }
            }

            UUID targetUuid = RoyaltyBoard.getUuid(playerTribe, targetPosition);
            assert targetUuid != null;

            if (quick) {
                Player targetPlayer = Bukkit.getPlayer(targetUuid);
                if (targetPlayer == null) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is not currently online.");
                    return;
                }
                if (Competition.activeChallenge != null) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You cannot start a challenge while one is currently occurring.");
                    return;
                }

                quickChallenges.put(player, targetPlayer);
                new Notification(
                        targetUuid,
                        "You have been challenged!",
                        player.getName() + " wants to quick-challenge you. This will start an immediate challenge if you accept. You can ignore this type of challenge with no consequence.",
                        Notification.Type.QUICK_CHALLENGE
                ).create();
                sender.sendMessage(EyeOfOnyx.EOO + "Quick challenge sent!");


            } else {
                // create notification for target
                String title = "You've been challenged!";
                String content = player.getName() + " has challenged your position for " + positions[targetPosition].replace('_', ' ') + ". If you ignore this message, you will be automatically removed from the royalty board on " + LocalDate.now().plusDays(EyeOfOnyx.getPlugin().getConfig().getInt("challenge-acknowledgement-time")).format(DateTimeFormatter.ISO_DATE) + ".";
                new Notification(targetUuid, title, content, Notification.Type.CHALLENGE_REQUESTED).create();

                // create challenge
                new Challenge(player.getUniqueId(), targetUuid, new ArrayList<>(), Challenge.State.PROPOSED).save();

                report(player.getName(), player.getName() + " initiated a challenge against " + PlayerUtility.getUsernameOfUuid(targetUuid) + ".");

                builder.append("Challenge initiated!");
                sender.spigot().sendMessage(builder.create());
            }


        }
    }

    private static void sendChallengeDetails(@NotNull Player player, @NotNull Challenge challenge) {
        boolean attacker = false;
        java.util.UUID opponentUuid = challenge.attacker;
        if (opponentUuid.equals(player.getUniqueId())) {
            opponentUuid = challenge.defender;
            attacker = true;
        }
        String opponentName = PlayerUtility.getUsernameOfUuid(opponentUuid);

        ComponentBuilder builder = new ComponentBuilder();
        builder.append(EyeOfOnyx.EOO).append("You are currently challenging ").append(opponentName).append(".\n");

        builder.append("[").color(ChatColor.WHITE).append("✓").color(ChatColor.GREEN).append("] Proposed\n[").color(ChatColor.WHITE);
        if (challenge.isAccepted()) builder.append("✓").color(ChatColor.GREEN);
        else builder.append("-").color(ChatColor.RED);
        builder.append("] Accepted\n[").color(ChatColor.WHITE);
        if (challenge.isScheduled()) builder.append("✓").color(ChatColor.GREEN);
        else builder.append("-").color(ChatColor.RED);
        builder.append("] Scheduled\n[").color(ChatColor.WHITE)
                .append("Cancel");
        if ((attacker && !challenge.isAttackerCanceled()) || (!attacker && !challenge.isDefenderCanceled())) builder.underlined(true).color(ChatColor.RED).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("If both you and your opponent agree, the challenge will be canceled."))).event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge cancel"));
        else builder.color(ChatColor.GRAY).event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("You already pressed cancel.")));
        builder.append("]").color(ChatColor.WHITE).underlined(false);

        player.spigot().sendMessage(builder.create());
    }

    private static BaseComponent[] challengeEntry(Tribe tribe, int pos, String command) {
        ComponentBuilder builder = new ComponentBuilder();

        TextComponent challengeButton = new TextComponent("[ Initiate Challenge ]");
        challengeButton.setUnderlined(true);
        challengeButton.setColor(ChatColor.GREEN);
        challengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Initiate a challenge with this player for their royalty position.")));
        challengeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge " + command));

        TextComponent quickChallengeButton = new TextComponent("[ Quick Challenge ]");
        quickChallengeButton.setUnderlined(true);
        quickChallengeButton.setColor(ChatColor.YELLOW);
        quickChallengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Execute an immediate challenge with this player. They can deny this if they choose.")));
        quickChallengeButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge " + command + " quick"));

        TextComponent disabledQuickChallengeButton = new TextComponent("[ Quick Challenge ]");
        disabledQuickChallengeButton.setUnderlined(false);
        disabledQuickChallengeButton.setColor(ChatColor.GRAY);
        disabledQuickChallengeButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("This player is not online.")));

        builder.append("\n").reset().append(getValidPositions()[pos].toUpperCase().replace('_', ' '))
                .append(" ")
                .append(PlayerUtility.getUsernameOfUuid(Objects.requireNonNull(getUuid(tribe, pos)))).color(ChatColor.YELLOW)
                .append("\n");

        builder.append(challengeButton).append("")
                .append(" ").reset().retain(ComponentBuilder.FormatRetention.NONE);

        if (Bukkit.getPlayer(Objects.requireNonNull(getUuid(tribe, pos))) == null) builder.append(disabledQuickChallengeButton, ComponentBuilder.FormatRetention.NONE);
        else builder.append(quickChallengeButton, ComponentBuilder.FormatRetention.NONE);

        return builder.append("\n").reset().create();
    }

    @NotNull
    public CommandAPICommand getCommand() {
        return new CommandAPICommand("challenge")
                .withPermission("eyeofonyx.challenge")
                .withHelp("Initiate a challenge.", "Initiate or manage a challenge.")
                .withOptionalArguments(new StringArgument("arg"))
                .withOptionalArguments(new StringArgument("arg2"))
                .withOptionalArguments(new StringArgument("arg3"))
                .executesNative((sender, args) -> {

                    String arg1 = (String) args.get("arg");
                    Dreamvisitor.debug("arg1: " + arg1);
                    String arg2 = (String) args.get("arg2");
                    Dreamvisitor.debug("arg2: " + arg2);

                    for (Player player : quickChallenges.keySet()) {
                        if (!player.isOnline()) {
                            quickChallenges.remove(player);
                        }
                    }

                    if (sender.getCallee() instanceof Player player) {

                        sender.sendMessage(EyeOfOnyx.EOO + "Please wait...");

                        // Check if board is frozen
                        if (RoyaltyBoard.isBoardFrozen()) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "The royalty board is currently frozen.");
                            return;
                        }

                        // Check if at least 12 hours play time
                        if (player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60 / 60 < 12) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You must have at least twelve hours of play time before you can participate in royalty.");
                            return;
                        }

                        // Ensure player is part of a team
                        Tribe playerTribe;
                        playerTribe = PlayerTribe.getTribeOfPlayer(player.getUniqueId());
                        if (playerTribe == null) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not part of a team!");
                            return;
                        }

                        // Ensure player has a linked Discord account
                        long discordId;
                        try {
                            discordId = AccountLink.getDiscordId(player.getUniqueId());

                        } catch (NullPointerException e) {
                            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have a linked Discord account! Contact a staff member for help.");
                            return;
                        }

                        Bot.getGameLogChannel().getGuild().retrieveMemberById(discordId).queue(member -> Bukkit.getScheduler().runTask(EyeOfOnyx.getPlugin(), () -> {
                            // Check if alt account
                            if (AltFamily.getParent(member.getIdLong()) != member.getIdLong()) {
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Your account is marked as an alt. Please contact staff if this is incorrect.");
                                return;
                            }

                            int playerPosition = RoyaltyBoard.getPositionIndexOfUUID(playerTribe, player.getUniqueId());

                            if (Banned.isPlayerBanned(player.getUniqueId())) {
                                // Player is banned from royalty board
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not allowed to initiate a challenge!");
                                return;
                            }

                            Challenge challenge = Challenge.getChallenge(player.getUniqueId());
                            if (arg1 == null && challenge != null) {
                                sendChallengeDetails(player, challenge);
                                return;
                            }

                            if (playerPosition != CIVILIAN && RoyaltyBoard.isOnCoolDown(playerTribe, playerPosition)) {

                                // Player is on cooldown. They cannot challenge
                                ComponentBuilder builder = new ComponentBuilder();
                                LocalDateTime challengeDate = RoyaltyBoard.getLastChallengeDate(playerTribe, playerPosition);
                                assert challengeDate != null;
                                challengeDate = challengeDate.plusDays(EyeOfOnyx.getPlugin().getConfig().getInt("challenge-cool-down"));

                                builder.append(EyeOfOnyx.EOO)
                                        .color(ChatColor.RED)
                                        .append("You are on movement cooldown until ")
                                        .append(challengeDate.format(DateTimeFormatter.ISO_DATE));

                                sender.spigot().sendMessage(builder.create());

                                return;

                            } else if (challenge != null && arg1.startsWith("position")) {

                                // Player has already initiated a challenge
                                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You have already initiated a challenge!");
                                return;

                            }
                            if (arg1 == null) {

                                ComponentBuilder builder = new ComponentBuilder();

                                /*

                                CHALLENGE MENU
                                You are currently $position$ of the $playerTeam$s.
                                Select a position:

                                 */

                                builder.append(EyeOfOnyx.EOO + "CHALLENGE MENU")
                                        .append("\nYou are currently ");
                                String position = "CIVILIAN";
                                if (playerPosition != CIVILIAN) position = positions[playerPosition];
                                builder.append(position.replace('_', ' ')).color(ChatColor.YELLOW)
                                        .append(" of the ").color(ChatColor.WHITE)
                                        .append(playerTribe.getTeamName()).color(ChatColor.YELLOW).append("s").color(ChatColor.YELLOW)
                                        .append(".\nSelect a position:\n").color(ChatColor.WHITE);

                                if /* Player is a civilian */ (playerPosition == CIVILIAN) {

                                    // Check for any empty positions
                                    int nextEmptyPosition = CIVILIAN;

                                    // Iterate through positions (start at ruler)
                                    for (int i = 0; i < positions.length; i++) {
                                        nextEmptyPosition = i;
                                        // If position is empty, break
                                        if (RoyaltyBoard.isPositionEmpty(playerTribe, i)) {
                                            break;
                                        }
                                        nextEmptyPosition = CIVILIAN;
                                    }

                                    // If a position is available, offer
                                    if (nextEmptyPosition < CIVILIAN) {

                                        builder.append(positions[nextEmptyPosition].toUpperCase().replace('_', ' '))
                                                .append("\nPosition Available\n \n");

                                        TextComponent button = new TextComponent("[ Assume Position ]");
                                        button.setUnderlined(true);
                                        button.setColor(ChatColor.GREEN);
                                        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge position1"));

                                        builder.append(button);

                                    } else {
                                        builder.append(challengeEntry(playerTribe, NOBLE1, "position1"), ComponentBuilder.FormatRetention.NONE).reset()
                                                .append(challengeEntry(playerTribe, NOBLE2, "position2"), ComponentBuilder.FormatRetention.NONE).reset()
                                                .append(challengeEntry(playerTribe, NOBLE3, "position3"), ComponentBuilder.FormatRetention.NONE).reset()
                                                .append(challengeEntry(playerTribe, NOBLE4, "position4"), ComponentBuilder.FormatRetention.NONE).reset()
                                                .append(challengeEntry(playerTribe, NOBLE5, "position5"), ComponentBuilder.FormatRetention.NONE).reset();

                                    }
                                } else if /* Player is a noble */ (playerPosition == NOBLE1 || playerPosition == NOBLE2 || playerPosition == NOBLE3 || playerPosition == NOBLE4 || playerPosition == NOBLE5) {

                                    builder.append(challengeEntry(playerTribe, HEIR1, "position1"), ComponentBuilder.FormatRetention.NONE).reset()
                                            .append(challengeEntry(playerTribe, HEIR2, "position2"), ComponentBuilder.FormatRetention.NONE).reset()
                                            .append(challengeEntry(playerTribe, HEIR3, "position3"), ComponentBuilder.FormatRetention.NONE).reset();

                                } else if /* Player is an heir */ (playerPosition == HEIR1 || playerPosition == HEIR2 || playerPosition == HEIR3) {

                                    builder.append(challengeEntry(playerTribe, RULER, "position1"), ComponentBuilder.FormatRetention.NONE).reset();

                                } else if /* Player is ruler */ (playerPosition == RULER) {

                                    builder.color(ChatColor.RED).append("You are ruler! No one to challenge.");

                                }

                                sender.spigot().sendMessage(builder.create());

                            } else {

                                switch (arg1) {
                                    case "position1", "position2", "position3", "position4", "position5" -> {
                                        boolean quick = arg2 != null && arg2.equals("quick");
                                        initiate(sender, arg1, quick, player, playerPosition, playerTribe);
                                    }
                                    case "accept" -> accept(sender, player);
                                    case "deny" -> deny(sender);
                                    case "denyconfirm" -> denyconfirm(sender, player, playerTribe, playerPosition);
                                    case "start" -> start(sender, player);
                                    case "date" -> date(sender, args, player, challenge);
                                    case "cancel" -> {
                                        if (challenge != null) {
                                            if (challenge.attacker.equals(player.getUniqueId())) challenge.cancelAttacker();
                                            if (challenge.defender.equals(player.getUniqueId())) challenge.cancelDefender();
                                            sender.sendMessage(EyeOfOnyx.EOO + "Cancel request sent.");
                                        } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You do not have a challenge to cancel!");
                                    }
                                    case "quickaccept" -> {
                                        for (Player quickChallengePlayer : quickChallenges.keySet()) {
                                            if (Objects.equals(quickChallenges.get(quickChallengePlayer), player)) {
                                                Competition.call(new Challenge(quickChallengePlayer.getUniqueId(), player.getUniqueId(), new ArrayList<>(), Challenge.State.SCHEDULED));
                                                quickChallenges.remove(quickChallengePlayer);
                                                return;
                                            }
                                        }
                                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That quick challenge proposal either expired or does not exist.");
                                    }
                                    default -> sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Incorrect arguments! Use the text GUI with /challenge");
                                }
                            }
                        }));
                    } else {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That command is only available for players!");
                    }
                });
    }
}