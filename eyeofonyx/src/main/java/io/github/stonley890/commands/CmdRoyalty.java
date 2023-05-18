package io.github.stonley890.commands;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.shanerx.mojang.Mojang;

import io.github.stonley890.Main;
import io.github.stonley890.files.RoyaltyBoard;
import net.md_5.bungee.api.ChatColor;

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

        // Fail if not enough arguements
        if (args.length < 1) {
            return false;
        } // If first arguement is "set" and there is another arguement
        else if (args[0].equalsIgnoreCase("set") && args.length > 2) {

            // Try to get online Player, otherwise lookup OfflinePlayer
            Player targetPlayer;
            if (Bukkit.getPlayerExact(args[1]) == null) {
                targetPlayer = (Player) Bukkit.getOfflinePlayer(UUID.fromString(mojang.getUUIDOfUsername(args[1])));
            } else {
                targetPlayer = Bukkit.getPlayerExact(args[1]);
            }

            // Get tribe from scoreboard team
            String playerTribe = null;
            try {

                // Get team of player by iterating through list
                for (int i = 0; i < teamNames.length; i++) {
                    if (scoreboard.getTeam(teamNames[i]).hasEntry(targetPlayer.getName()))
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
                    board.set(playerTribe + "." + args[2] + ".uuid", targetPlayer.getUniqueId());
                    RoyaltyBoard.save();
                    sender.sendMessage(ChatColor.YELLOW + "" + targetPlayer.getName() + " is now " + args[2]);

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
            if (args[1] == null) {
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
        }

        RoyaltyBoard.save();
        return true;
    }

    /*
    * Used to build a message for `royalty list [tribe]`
    * Requires index of tribe
    * Returns a StringBuilder with the built message
    * This method uses Mojang API lookup to get player names and gets data from royalty.yml
    * Here is what the result looks like:
    
    TeamName ---
    [ RULER: Username
    [ HEIR APPARENT: Username
    [ HEIR PRESUMPTIVE: Username
    [ NOBLE APPARENT: Username
    [ NOBLE PRESUMPTIVE: Username

    */
    StringBuilder buildBoard(int index) {
        StringBuilder strBuild = new StringBuilder();
        strBuild.append("\n" + ChatColor.GOLD + teamNames[index] + " ---" + ChatColor.RESET + "\n[ RULER: "
                + mojang.getPlayerProfile((String) board.get(tribes[index] + ".ruler.uuid")).getUsername()
                + "\n[ HEIR APPARENT: "
                + mojang.getPlayerProfile((String) board.get(tribes[index] + ".heir_apparent.uuid")).getUsername()
                + "\n[ HEIR PRESUMPTIVE: "
                + mojang.getPlayerProfile((String) board.get(tribes[index] + ".heir_presumptive.uuid")).getUsername()
                + "\n[ NOBLE APPARENT: "
                + mojang.getPlayerProfile((String) board.get(tribes[index] + ".noble_apparent.uuid")).getUsername()
                + "\n[ NOBLE PRESUMPTIVE: "
                + mojang.getPlayerProfile((String) board.get(tribes[index] + ".noble_presumptive.uuid")).getUsername());
        return strBuild;
    }
}
