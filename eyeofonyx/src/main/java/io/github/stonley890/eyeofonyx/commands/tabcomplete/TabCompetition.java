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

    List<String> suggestions = new ArrayList<>();

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        // Clear previous suggestions or create a new ArrayList
        if (suggestions != null)  suggestions.clear();
        else suggestions = new ArrayList<>();

        if (args.length == 1) {

            // competition <create|modify>
            suggestions.add("create");
            suggestions.add("modify");

        } else if (args.length == 2) {

            // competition create <attacker>
            suggestions.add("@p");
            for (Player player : Bukkit.getOnlinePlayers())
                suggestions.add(player.getName());

            // competition modify <player>
            for (Player player : Bukkit.getOnlinePlayers())
                suggestions.add(player.getName());
        } else if (args.length == 3) {

            // competition create <attacker> <defender>
            for (Player player : Bukkit.getOnlinePlayers())
                suggestions.add(player.getName());

            // competition modify <player> <action>
            suggestions.add("attacker");
            suggestions.add("defender");
            suggestions.add("type");
            suggestions.add("start");
            suggestions.add("cancel");

        }

        return suggestions;
    }
}
