package io.github.stonley890.eyeofonyx.commands.tabcomplete;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import io.github.stonley890.eyeofonyx.web.IpUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import org.jetbrains.annotations.NotNull;

public class TabRoyalty implements TabCompleter {

    static String[] tribes = RoyaltyBoard.getTribes();
    static String[] positions = RoyaltyBoard.getValidPositions();

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

        List<String> suggestions = new ArrayList<>();

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
                    suggestions.add("@p");
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
                suggestions.add("last_challenge_time");
                suggestions.add("challenger");
                if (!args[2].equals("ruler")) {
                    suggestions.add("challenging");
                }
            }
        } else if (args.length == 5) {
            if (args[0].equals("manage")) {
                if (args[3].equals("joined_time") || args[3].equals("last_challenge_time")) {
                    suggestions.add("now");
                    suggestions.add("never");
                } else if (args[3].equals("challenger") || args[3].equals("challenging")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        suggestions.add("@a");
                        suggestions.add("@p");
                        suggestions.add("@r");
                        suggestions.add("@s");
                        suggestions.add(player.getName());
                    }
                }
            }
        }

        return suggestions;
    }

}
