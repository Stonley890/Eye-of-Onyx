package io.github.stonley890.eyeofonyx.commands.tabcomplete;

import java.util.List;


import kotlin.collections.builders.ListBuilder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import org.jetbrains.annotations.NotNull;

public class TabRoyalty implements TabCompleter {

    String[] tribes = RoyaltyBoard.getTribes();
    String[] positions = RoyaltyBoard.getValidPositions();

    List<String> suggestions = new ListBuilder<>();

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // Clear previous suggestions or create a new ListBuilder
        if (suggestions != null) {
            suggestions.clear();
        } else {
            suggestions = new ListBuilder<>();
        }

        if (args.length == 1) {
            
            // royalty <clear|list|set|update>
            suggestions.add("clear");
            suggestions.add("list");
            suggestions.add("set");
            suggestions.add("update");

        } else if (args.length == 2) {

            // royalty <clear|set> <tribe>
            if (args[0].equals("clear") || args[0].equals("list")) {
                suggestions.addAll(List.of(tribes));

            } // royalty <set> <player>
            else if (args[0].equals("set")) {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
                
            }
        } else if (args.length == 3 && (args[0].equals("clear") || args[0].equals("set"))) {

            // royalty set <clear> <arg1> <position>
            suggestions.addAll(List.of(positions));

        }

        return suggestions;
    }
    
}
