package io.github.stonley890.eyeofonyx.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

public class CmdEyeOfOnyx implements CommandExecutor {

    EyeOfOnyx main = EyeOfOnyx.getPlugin();

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Eye of Onyx " + main.version + "\nStonley890 / iHeron");
        return true;
    }
}
