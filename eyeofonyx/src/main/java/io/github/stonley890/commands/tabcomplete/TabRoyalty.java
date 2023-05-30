package io.github.stonley890.commands.tabcomplete;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import io.github.stonley890.files.RoyaltyBoard;
import kotlin.collections.builders.ListBuilder;

public class TabRoyalty implements TabCompleter {

    String[] tribes = RoyaltyBoard.getTribes();
    String[] positions = RoyaltyBoard.getValidPositions();

    List<String> suggestions = new ListBuilder<>();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        if (suggestions != null) {
            suggestions.clear();
        } else {
            suggestions = new ListBuilder<>();
        }

        if (args.length == 1) {
            
            suggestions.add("clear");
            suggestions.add("list");
            suggestions.add("set");
            suggestions.add("update");

        } else if (args.length == 2) {

            if (args[0].equals("clear") || args[0].equals("list")) {
                suggestions.addAll(List.of(tribes));

            } else if (args[0].equals("set")) {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
                
            }
        } else if (args.length == 3 && (args[0].equals("clear") || args[0].equals("set"))) {

            suggestions.addAll(List.of(positions));

        } else {
            suggestions = null;
        }

        return suggestions;
    }
    
}
