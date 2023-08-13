package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

public class CmdRoyalty implements CommandExecutor {

    // Get Mojang services
    private static final Mojang mojang = new Mojang().connect();

    // Get server scoreboard service (for teams)
    private static final Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();

    // Easy access royalty board
    private static FileConfiguration board = RoyaltyBoard.get();

    // Team names
    private static final String[] teamNames = RoyaltyBoard.getTeamNames();

    // Tribe IDs
    private static final String[] tribes = RoyaltyBoard.getTribes();

    // Valid positions
    private static final String[] validPositions = RoyaltyBoard.getValidPositions();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // Check for and create missing teams
        for (String teamName : teamNames) {
            if (scoreboard.getTeam(teamName) == null) {

                scoreboard.registerNewTeam(teamName);
                sender.sendMessage(EyeOfOnyx.EOO + "Created missing " + teamName + " team.");
            }
        }

        sender.sendMessage(EyeOfOnyx.EOO + "Please wait...");

        RoyaltyBoard.reload();

        if (args.length < 1)  list(sender, args);
        else if (args[0].equalsIgnoreCase("set") && args.length > 2) {

            if (sender.hasPermission("eyeofonyx.manageboard")) set(sender, args);
            else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else if (args[0].equalsIgnoreCase("list")) {

            list(sender, args);

        } else if (args[0].equalsIgnoreCase("clear") && args.length > 2) {

            if (sender.hasPermission("eyeofonyx.manageboard")) clear(sender, args);
            else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");

        } else if (args[0].equalsIgnoreCase("update")) {

            if (sender.hasPermission("eyeofonyx.manageboard")) {
                sender.sendMessage(EyeOfOnyx.EOO + "Reloading and updating the board...");
                RoyaltyBoard.reload();
                RoyaltyBoard.updateBoard();
                board = RoyaltyBoard.get();
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "Board updated.");
            } else sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You are not permitted to use that command.");


        } else sender.sendMessage(EyeOfOnyx.EOO + "Invalid arguments! /royalty <set|list|clear|update>");

        RoyaltyBoard.save(board);
        return true;
    }

    // royalty set <player> <position> [name]
    void set(CommandSender sender, String[] args) {

        UUID targetPlayerUUID;

        try {
            // Try to get online Player, otherwise lookup OfflinePlayer
            targetPlayerUUID = UUID.fromString(mojang.getUUIDOfUsername(args[1]).replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"));
        } catch (NullPointerException e) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Player not found.");
            return;
        }

        // Get tribe from scoreboard team
        String playerTribe = null;
        try {

            // Get team of player by iterating through list
            for (int i = 0; i < teamNames.length; i++) {
                if (Objects.requireNonNull(scoreboard.getTeam(teamNames[i])).hasEntry(args[1]))
                    playerTribe = tribes[i];
            }

            // If target has no associated tribe team, fail
            if (playerTribe == null) {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Target does not have a tribe tag!");
                return;
            }

            // Check if third argument contains a valid position
            if (Arrays.stream(validPositions).anyMatch(args[2]::contains)) {

                // Set value in board.yml
                setBoard(playerTribe, args[2], "uuid", targetPlayerUUID.toString());
                RoyaltyBoard.save(board);
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + args[1] + " is now " + args[2].toUpperCase().replace('_', ' '));

                // If no name was provided, use username
                if (args.length == 3) {
                    setBoard(playerTribe, args[2], "name", args[1]);

                    // If ruler, set title to Ruler
                    if (args[2].equals(validPositions[0])) setBoard(playerTribe, args[2], "title", "Ruler");
                }

                // Canon name
                // if ruler do title, then name
                if (args[2].equals(validPositions[0]) && args.length > 3) {
                    setBoard(playerTribe, args[2], "title", args[3]);
                    setBoard(playerTribe, args[2], "name", args[4]);
                } else if (args.length > 3) setBoard(playerTribe, args[2], "name", args[3]);


                setBoard(playerTribe, args[2], "joined_time", LocalDateTime.now().toString());
                setBoard(playerTribe, args[2], "last_challenge_time", LocalDateTime.now().toString());
                setBoard(playerTribe, args[2], "last_online", LocalDateTime.now().toString());

            } else
                sender.sendMessage(EyeOfOnyx.EOO + 
                        ChatColor.RED + "Invalid position. Valid positions: " + Arrays.toString(validPositions));

        } catch (IllegalArgumentException e) {
            // getTeam() throws IllegalArgumentException if teams do not exist
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Required teams do not exist!");
        }

    }

    // royalty list [tribe]
    void list(CommandSender sender, String[] args) {

        // If no other arguments, build and send full board
        if (args.length == 1) {
            // Init a StringBuilder to store message for building
            StringBuilder boardMessage = new StringBuilder();

            // Build for each tribe
            for (int i = 0; i < teamNames.length; i++) {
                boardMessage.append(buildBoard(i));
            }

            // Send built message
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage);

        } // If next argument is a tribe, send just that board
        else if (Arrays.stream(tribes).anyMatch(args[1]::contains)) {

            // Init a StringBuilder to store message for building
            StringBuilder boardMessage;

            // Find index of tribe and build
            boardMessage = buildBoard(Arrays.binarySearch(tribes, args[1]));

            // Send built message
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage.toString());

        } else {
            // Invalid argument
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Invalid tribe name!");
        }

    }

    // royalty clear <tribe> <position>
    void clear(CommandSender sender, String[] args) {

        if (Arrays.stream(tribes).noneMatch(args[1]::contains)) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid tribe.");
            return;
        }
        if (Arrays.stream(validPositions).noneMatch(args[2]::contains)) {
            sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Not a valid position.");
            return;
        }

        // Clear data
        setBoard(args[1], args[2], "uuid", "none");
        setBoard(args[1], args[2], "name", "none");
        setBoard(args[1], args[2], "joined_time", "none");
        setBoard(args[1], args[2], "last_challenge_time", "none");
        setBoard(args[1], args[2], "challenger", "none");
        setBoard(args[1], args[2], "last_online", "none");

        // If ruler, clear title, else clear challenging
        if (args[2].equals(validPositions[0])) {
            setBoard(args[1], args[2], "title", "none");
        } else {
            setBoard(args[1], args[2], "challenging", "none");
        }

        RoyaltyBoard.updateBoard();
        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.YELLOW + args[1].toUpperCase() + " " + args[2].toUpperCase() + " position cleared.");

    }

    /*
     * Used to build a message for `royalty list [tribe]`
     * Requires index of tribe
     * Returns a StringBuilder with the built message
     * This method uses Mojang API lookup to get player names and gets data from
     * royalty.yml
     * Here is what the result looks like:
     * 
     * 1 TeamName ---
     * 2 [ RULER: Title Name (Username)
     * 3 [ HEIR APPARENT: Name (Username)
     * 4 [ HEIR PRESUMPTIVE: Name (Username)
     * 5 [ NOBLE APPARENT: Name (Username)
     * 6 [ NOBLE PRESUMPTIVE: Name (Username)
     * 
     */
    StringBuilder buildBoard(int index) {

        // Create string builder
        StringBuilder strBuild = new StringBuilder();
        // Add tribe name (line 1)
        strBuild.append("\n").append(ChatColor.GOLD).append(teamNames[index]).append(" ---").append(ChatColor.RESET);

        // For each position...
        for (int position = 0; position < validPositions.length; position++) {

            // Add the name of the position, change to uppercase, remove underscores
            strBuild.append(ChatColor.WHITE).append("\n[ ").append(validPositions[position].toUpperCase().replace('_', ' ')).append(": ");
            // If no one filling position, report as "none"
            if (Objects.equals(board.get(tribes[index] + "." + validPositions[position] + ".uuid"), "none")) {
                strBuild.append(ChatColor.GRAY).append("none");
            } // Otherwise...
            else {
                strBuild.append(ChatColor.YELLOW);
                // Add canon name w/ username in parentheses
                // Set name from OpenRP character
                if (EyeOfOnyx.openrp != null) {
                    String uuid = RoyaltyBoard.getUuid(index, position);
                    String ocName = (String) EyeOfOnyx.openrp.getDesc().getUserdata().get(uuid + ".name");
                    if (ocName != null && !ocName.equals("No name set")) {
                        strBuild.append(EyeOfOnyx.openrp.getDesc().getUserdata().getString(uuid + "." + EyeOfOnyx.getPlugin().getConfig().getString("character-name-field")));
                    } else {
                        strBuild.append(board.get(tribes[index] + "." + validPositions[position] + ".name"));
                    }
                } else {
                    strBuild.append(board.get(tribes[index] + "." + validPositions[position] + ".name"));
                }

                strBuild.append(ChatColor.WHITE).append(" (").append(mojang.getPlayerProfile(board.getString(tribes[index] + "." + validPositions[position] + ".uuid"))
                        .getUsername()).append(")");
            }
        }
        return strBuild;
    }

    void setBoard(String tribe, String position, String data, Object value) {
        board.set(tribe + "." + position + "." + data, value);
    }

}
