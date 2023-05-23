package io.github.stonley890.commands;

import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.Main;
import io.github.stonley890.files.RoyaltyBoard;

public class CmdRoyalty implements CommandExecutor {

    // Get plugin instance (main thread)
    Plugin plugin = Main.getPlugin();

    // Get Mojang services
    Mojang mojang = new Mojang().connect();

    // Get server scoreboard service (for teams)
    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

    // Easy access royalty board
    FileConfiguration board = RoyaltyBoard.get();

    // Team names
    String[] teamNames = {
            "HiveWing", "IceWing", "LeafWing", "MudWing", "NightWing", "RainWing", "SandWing", "SeaWing", "SilkWing",
            "SkyWing"
    };

    // Tribe IDs
    String[] tribes = {
            "hive", "ice", "leaf", "mud", "night", "rain", "sand", "sea", "silk", "sky"
    };

    // Valid positions
    String[] validPositions = {
            "ruler", "heir_apparent", "heir_presumptive", "noble_apparent", "noble_presumptive"
    };

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Check for and create missing teams
        for (int i = 0; i < teamNames.length; i++) {
            if (scoreboard.getTeam(teamNames[i]) == null) {

                scoreboard.registerNewTeam(teamNames[i]);
                sender.sendMessage("Created missing " + teamNames[i] + " team.");
            }
        }
        
        // Fail if not enough arguements
        if (args.length < 1) {
            return false;
        } // If first arguement is "set" and there is another arguement
        else if (args[0].equalsIgnoreCase("set") && args.length > 2) {

            // Try to get online Player, otherwise lookup OfflinePlayer
            UUID targetPlayerUUID = UUID.fromString(mojang.getUUIDOfUsername(args[1]).replaceFirst(
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                    "$1-$2-$3-$4-$5"));

            // Get tribe from scoreboard team
            String playerTribe = null;
            try {

                // Get team of player by iterating through list
                for (int i = 0; i < teamNames.length; i++) {
                    if (scoreboard.getTeam(teamNames[i]).hasEntry(args[1]))
                        playerTribe = tribes[i];
                }

                // If target has no associated tribe team, fail
                if (playerTribe == null) {
                    sender.sendMessage(ChatColor.RED + "Target does not have a tribe tag!");
                    return false;
                }

                // Check if third arguement contains a valid position
                if (Arrays.stream(validPositions).anyMatch(args[2]::contains)) {

                    // Set value in board.yml
                    setBoard(playerTribe, args[2], "uuid", targetPlayerUUID.toString());
                    RoyaltyBoard.save();
                    sender.sendMessage(ChatColor.YELLOW + "" + args[1] + " is now " + args[2]);

                    // If no name was provided, use username
                    if (args.length == 3) {
                        setBoard(playerTribe, args[2], "name", args[1]);

                        // If ruler, set title to Ruler
                        if (args[2].equals(validPositions[0])) {
                            setBoard(playerTribe, args[2], "title", "Ruler");
                        }
                    }

                    // Canon name
                    // if ruler do title, then name
                    if (args[2].equals(validPositions[0]) && args.length > 4) {
                        setBoard(playerTribe, args[2], "title", args[3]);
                        setBoard(playerTribe, args[2], "name", args[4]);
                    } else {
                        setBoard(playerTribe, args[2], "name", args[3]);
                    }

                    setBoard(playerTribe, args[2], "joined_time", Calendar.getInstance().getTime().getTime());

                } else {
                    sender.sendMessage(
                            ChatColor.RED + "Invalid position. Valid positions: " + Arrays.toString(validPositions));
                }
            } catch (IllegalArgumentException e) {
                // getTeam() throws IllegalArgumentException if teams do not exist
                sender.sendMessage(ChatColor.RED + "Required teams do not exist!");
            }

        } else if (args[0].equalsIgnoreCase("list")) {
            // If no other arguements, build and send full board
            if (args.length == 1) {
                // Init a StringBuilder to store message for building
                StringBuilder boardMessage = new StringBuilder();

                // Build for each tribe
                for (int i = 0; i < teamNames.length; i++) {
                    boardMessage.append(buildBoard(i));
                }

                // Send built message
                sender.sendMessage(ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage.toString());

            } // If next argument is a tribe, send just that board
            else if (Arrays.stream(tribes).anyMatch(args[1]::contains)) {

                // Init a StringBuilder to store message for building
                StringBuilder boardMessage;

                // Find index of tribe and build
                boardMessage = buildBoard(Arrays.binarySearch(tribes, args[1]));

                // Send built message
                sender.sendMessage(ChatColor.YELLOW + "ROYALTY BOARD" + boardMessage.toString());

            } else {
                // Invalid arguement
                sender.sendMessage(ChatColor.RED + "Invalid tribe name!");
                return false;
            }
        } else if (args[0].equalsIgnoreCase("clear") && args.length > 2) {
            
            if (Arrays.stream(tribes).noneMatch(args[1]::contains)) {
                sender.sendMessage(ChatColor.RED + "Not a valid tribe.");
                return false;
            }
            if (Arrays.stream(validPositions).noneMatch(args[2]::contains)) {
                sender.sendMessage(ChatColor.RED + "Not a valid position.");
                return false;
            }

            setBoard(args[1], args[2], "uuid", "none");
            setBoard(args[1], args[2], "name", "none");
            setBoard(args[1], args[2], "joined_time", "none");
            setBoard(args[1], args[2], "last_challenge_time", "none");
            setBoard(args[1], args[2], "challenger", "none");
            if (args[2].equals(validPositions[0])) { setBoard(args[1], args[2], "title", "none"); }

            sender.sendMessage(args[1].toUpperCase() + " " + args[2].toUpperCase() + " position cleared.");

        } else {
            return false;
        }

        RoyaltyBoard.save();
        return true;
    }

    /*
     * Used to build a message for `royalty list [tribe]`
     * Requires index of tribe
     * Returns a StringBuilder with the built message
     * This method uses Mojang API lookup to get player names and gets data from
     * royalty.yml
     * Here is what the result looks like:
     * 
     *1 TeamName ---
     *2 [ RULER: Title Name (Username)
     *3 [ HEIR APPARENT: Name (Username)
     *4 [ HEIR PRESUMPTIVE: Name (Username)
     *5 [ NOBLE APPARENT: Name (Username)
     *6 [ NOBLE PRESUMPTIVE: Name (Username)
     * 
     */
    StringBuilder buildBoard(int index) {

        // Create stringbuilder
        StringBuilder strBuild = new StringBuilder();
        // Add tribe name (line 1)
        strBuild.append("\n" + ChatColor.GOLD + teamNames[index] + " ---" + ChatColor.RESET);

        // For each position...
        for (int j = 0; j < validPositions.length; j++) {

            // Add the name of the position, change to uppercase, remove underscores
            strBuild.append("\n[ " + validPositions[j].toUpperCase().replace('_', ' ') + ": ");
            // If no one filling position, report as "none"
            if (board.get(tribes[index] + "." + validPositions[j] + ".uuid").equals("none")) {
                strBuild.append("none");
            } // Otherwise...
            else {
                // If position is ruler, prepend the title
                if (j == 0) {
                    strBuild.append(board.get(tribes[index] + "." + validPositions[j] + ".title") + " ");
                }
                // Add canon name w/ username in parenthesis
                strBuild.append(board.get(tribes[index] + "." + validPositions[j] + ".name"));
                strBuild.append(" (" + mojang.getPlayerProfile((String) board.get(tribes[index] + "." + validPositions[j] + ".uuid")).getUsername() + ")");
            }
        }
        return strBuild;
    }

    void setBoard(String tribe, String position, String data, Object value) {
        board.set(tribe + "." + position + "." + data, value);
    }
}
