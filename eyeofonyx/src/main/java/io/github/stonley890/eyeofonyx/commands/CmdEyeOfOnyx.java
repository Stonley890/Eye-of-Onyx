package io.github.stonley890.eyeofonyx.commands;

import io.github.stonley890.eyeofonyx.files.Banned;
import io.github.stonley890.eyeofonyx.files.Notification;
import io.github.stonley890.eyeofonyx.files.NotificationType;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.shanerx.mojang.Mojang;

import java.util.List;

public class CmdEyeOfOnyx implements CommandExecutor {

    EyeOfOnyx main = EyeOfOnyx.getPlugin();
    Mojang mojang = new Mojang().connect();

    boolean disabling = false;

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        sender.sendMessage(ChatColor.YELLOW + "Eye of Onyx " + main.version + "\nStonley890 / iHeron");

        if (args[0].equals("ban")) {
            if /* there is another argument */ (args.length > 1) {
                String uuid = mojang.getUUIDOfUsername(args[1]);
                if /* the username is invalid */ (uuid == null) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That user could not be found.");
                } else /* the username is valid */ {
                    if /* player is already banned */ (Banned.isPlayerBanned(uuid)) {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is already banned.");
                    } else /* player is not already banned */ {

                        Banned.addPlayer(uuid);
                        int tribe = RoyaltyBoard.getTribeIndexOfUUID(uuid);
                        int pos = RoyaltyBoard.getPositionIndexOfUUID(uuid);
                        new Notification(uuid, "Royalty Ban", "You are no longer allowed to participate in royalty. Contact staff if you think this is a mistake.", NotificationType.GENERIC).create();
                        sender.sendMessage(EyeOfOnyx.EOO + args[1] + " has been banned.");

                    }
                }
            } else /* there are no other arguments */ {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments. /eyeofonyx ban <player>");
            }
        } else if (args[0].equals("unban")) {
            if /* there is another argument */ (args.length > 1) {
                String uuid = mojang.getUUIDOfUsername(args[1]);
                if /* the username is invalid */ (uuid == null) {
                    sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That user could not be found.");
                } else /* the username is valid */ {
                    if /* player is banned */ (Banned.isPlayerBanned(uuid)) {
                        Banned.removePlayer(uuid);
                        sender.sendMessage(EyeOfOnyx.EOO + args[1] + " has been unbanned.");
                    } else /* player is not banned */ {
                        sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "That player is not banned.");
                    }
                }
            } else {
                sender.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "Missing arguments. /eyeofonyx ban <player>");
            }
        } else if (args[0].equals("disable")) {
            Runnable disable = new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.getLogger().info(sender.getName() + " disabled Eye of Onyx.");
                    Bukkit.getPluginManager().disablePlugin(main);
                }
            };

            if (disabling) {

                List<BukkitTask> tasks = Bukkit.getScheduler().getPendingTasks();
                tasks.get(tasks.size() - 1).cancel();
                sender.sendMessage(EyeOfOnyx.EOO + "Disable canceled.");

            } else {
                sender.sendMessage(EyeOfOnyx.EOO + "Eye of Onyx will disable in five seconds. Run /disable again to cancel.");
                disabling = true;
                Bukkit.getScheduler().runTaskLater(EyeOfOnyx.getPlugin(), disable, 100L);
            }

        }

        return true;
    }
}
