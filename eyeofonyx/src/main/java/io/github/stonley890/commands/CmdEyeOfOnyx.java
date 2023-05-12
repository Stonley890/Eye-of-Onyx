package io.github.stonley890.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import io.github.stonley890.Main;
import net.md_5.bungee.api.ChatColor;

public class CmdEyeOfOnyx implements CommandExecutor {

    Main main = Main.getPlugin();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Eye of Onyx " + main.version + "\nStonley890 / iHeron");
        return true;
    }
}
