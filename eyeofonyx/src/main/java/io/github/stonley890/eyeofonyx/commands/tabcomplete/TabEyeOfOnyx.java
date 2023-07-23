package io.github.stonley890.eyeofonyx.commands.tabcomplete;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.Banned;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.shanerx.mojang.Mojang;

import java.util.ArrayList;
import java.util.List;

public class TabEyeOfOnyx implements TabCompleter {

    List<String> suggestions = new ArrayList<>();

    Mojang mojang = new Mojang().connect();
    EyeOfOnyx plugin = EyeOfOnyx.getPlugin();

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Clear previous suggestions or create a new ArrayList
        if (suggestions != null) {
            suggestions.clear();
        } else {
            suggestions = new ArrayList<>();
        }

        if (args.length == 1) {

            // royalty <clear|list|set|update>
            suggestions.add("ban");
            suggestions.add("unban");
            suggestions.add("freeze");
            suggestions.add("disable");

        } else if (args.length == 2) {

            // royalty <ban> <player>
            if (args[0].equals("ban")) {

                for (Player player : Bukkit.getOnlinePlayers()) {
                    suggestions.add(player.getName());
                }

            } // royalty <unban> <player>
            else if (args[0].equals("unban")) {

                for (String bannedPlayer : Banned.getBannedPlayers()) {
                    String name = mojang.getPlayerProfile(bannedPlayer).getUsername();
                    suggestions.add(name);
                }

            }
        }

        return suggestions;
    }
}
