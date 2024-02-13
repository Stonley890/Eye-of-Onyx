package io.github.stonley890.eyeofonyx.commands.tabcomplete;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TabCompetition implements TabCompleter {



    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {

            // competition modify <player> <action>
            suggestions.add("start");
            suggestions.add("cancel");
            suggestions.add("end");

        } else if (args.length == 2) {

            if (args[0].equals("end")) {
                suggestions.add("attacker");
                suggestions.add("defender");
            }

            if (args[0].equals("create")) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }
            }
        }

        return suggestions;
    }
}
