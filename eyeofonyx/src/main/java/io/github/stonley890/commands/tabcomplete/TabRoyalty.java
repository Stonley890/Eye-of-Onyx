package io.github.stonley890.commands.tabcomplete;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import io.github.stonley890.files.RoyaltyBoard;
import kotlin.collections.builders.ListBuilder;

public class TabRoyalty implements TabCompleter {

    String[] tribes = RoyaltyBoard.getTribes();

    List<String> suggestions = new ListBuilder<>();

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            
            suggestions.add("clear");
            suggestions.add("list");
            suggestions.add("set");
        } else if (args.length == 2) {
            if (args[0].equals("clear")) {
                suggestions.addAll(Arrays.copyOf(tribes, ));
            }
        }
        return suggestions;
    }
    
}
