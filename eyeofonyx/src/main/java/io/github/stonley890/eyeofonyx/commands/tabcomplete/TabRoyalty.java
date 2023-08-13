package io.github.stonley890.eyeofonyx.commands.tabcomplete;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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

    List<String> suggestions = new ArrayList<>();

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        // Clear previous suggestions or create a new ArrayList
        if (suggestions != null) {
            suggestions.clear();
        } else {
            suggestions = new ArrayList<>();
        }

        if (args.length == 1) {
            
            // royalty <clear|list|set|update|manage>
            suggestions.add("clear");
            suggestions.add("list");
            suggestions.add("set");
            suggestions.add("update");
            suggestions.add("manage");

        } else if (args.length == 2) {

            switch (args[0]) {
                case "clear", "list" -> suggestions.addAll(List.of(tribes));
                case "set" -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        suggestions.add(player.getName());
                    }
                }
                case "manage" -> suggestions.addAll(Arrays.asList(RoyaltyBoard.getTribes()));
            }

        } else if (args.length == 3) {

            if (args[0].equals("clear") || args[0].equals("set") || args[0].equals("manage")) {
                suggestions.addAll(List.of(positions));
            }
        } else if (args.length == 4) {

            if (args[0].equals("manage")) {
                suggestions.add("name");
                suggestions.add("joined_time");
                suggestions.add("last_online");
                suggestions.add("last_challenge_time");
                suggestions.add("challenger");
                if (!args[2].equals("ruler")) {
                    suggestions.add("challenging");
                }
            }
        }

        return suggestions;
    }

}
