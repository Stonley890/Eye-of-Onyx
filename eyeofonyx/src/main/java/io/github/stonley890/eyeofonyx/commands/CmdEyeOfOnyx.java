package io.github.stonley890.eyeofonyx.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.ExecutableCommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.wrappers.Rotation;
import io.github.stonley890.dreamvisitor.commands.CommandUtils;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.dreamvisitor.data.TribeUtil;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.Utils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CmdEyeOfOnyx {

    final EyeOfOnyx main = EyeOfOnyx.getPlugin();

    @NotNull
    public CommandAPICommand getCommand() {
        return new CommandAPICommand("eyeofonxy")
                .executes((sender, args) -> {
                    sender.sendMessage(ChatColor.YELLOW + "Eye of Onyx " + main.version + "\nStonley890 / iHeron\nOpen source at https://github.com/Stonley890/Eye-Of-Onyx");
                })
                .withSubcommand(new CommandAPICommand("reload")
                        .withPermission(CommandPermission.OP)
                        .executes((sender, args) -> {
                            EyeOfOnyx.getPlugin().reloadConfig();
                            sender.sendMessage(EyeOfOnyx.EOO + "Configuration refreshed from file.");
                        })
                )
                .withSubcommand(new CommandAPICommand("manage")
                        .withPermission(CommandPermission.OP)
                        .withSubcommands(
                                new CommandAPICommand("address")
                                        .withHelp("Set address.", "The address to use for the challenge availability web interface.\n" +
                                                "Default: \"https://eyeofonyx.woftnw.org\"")
                                        .withOptionalArguments(new TextArgument("address"))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "address";
                                            configString(sender, args, key);
                                        }),
                                new CommandAPICommand("port")
                                        .withHelp("Set port.", """
                                                The port to use for the challenge availability web interface.
                                                You'll need to forward this on your network.
                                                If not set, it will fall back to the default 8000.
                                                Default: 8000""")
                                        .withOptionalArguments(new IntegerArgument("port"))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "port";
                                            configInt(sender, args, key);
                                        }),
                                new CommandAPICommand("character-name-field")
                                        .withHelp("Set name.", """
                                                The name of the field where a character's name is stored in OpenRP.
                                                OpenRP uses "name" by default. It can be changed in OpenRP/descriptions/config.yml.
                                                Don't change this if you don't know what this is.
                                                Default: "name\"""")
                                        .withOptionalArguments(new StringArgument("character-name-field"))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "character-name-field";
                                            configString(sender, args, key);
                                        }),
                                new CommandAPICommand("royalty-log-channel")
                                        .withHelp("Set name.", "The channel ID for the Discord channel where updates to the royalty board should be recorded.\n" +
                                                "# Default: 660597606233276436")
                                        .withOptionalArguments(new LongArgument("royalty-log-channel"))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "royalty-log-channel";
                                            configLong(sender, args, key);
                                        }),
                                new CommandAPICommand("main-royalty-roles")
                                        .withHelp("Set main-royalty-roles.", "The royalty role IDs for the main server")
                                        .withArguments(new StringArgument("position").replaceSuggestions(ArgumentSuggestions.strings(
                                                "ruler", "heir", "noble"
                                        )))
                                        .withOptionalArguments(new LongArgument("channelId"))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "main-royalty-roles";
                                            @NotNull String position = (String) Objects.requireNonNull(args.get("position"));
                                            Long value = (Long) args.get("channelId");

                                            int pos = Utils.posIndexFromString(position);
                                            if (pos == -1) throw CommandAPI.failWithString("Invalid position!");

                                            String fullKey = key + "." + position;

                                            if (value == null) sender.sendMessage(EyeOfOnyx.EOO + fullKey + " is currently set to\n" + main.getConfig().getLong(fullKey));
                                            else {
                                                main.getConfig().set(fullKey, value);
                                                main.saveConfig();
                                                sender.sendMessage(EyeOfOnyx.EOO + "Set " + fullKey + " to\n" + main.getConfig().getLong(fullKey));
                                            }
                                        }),
                                new CommandAPICommand("challenge-cool-down")
                                        .withHelp("Set challenge-cool-down.", """
                                                Cool down period:
                                                The number of DAYS that a user is unable to participate in a challenge after they have moved.
                                                Default: 7""")
                                        .withOptionalArguments(new IntegerArgument("challenge-cool-down", 0))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "challenge-cool-down";
                                            configInt(sender, args, key);
                                        }),
                                new CommandAPICommand("challenge-acknowledgement-time")
                                        .withHelp("Set challenge-acknowledgement-time.", """
                                                Challenge acknowledgement period:
                                                The number of DAYS that a user has to acknowledge a challenge that has been issued to them.
                                                Default: 7""")
                                        .withOptionalArguments(new IntegerArgument("challenge-acknowledgement-time", 0))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "challenge-acknowledgement-time";
                                            configInt(sender, args, key);
                                        }),
                                new CommandAPICommand("challenge-time-period")
                                        .withHelp("Set challenge-acknowledgement-time.", """
                                                Challenge time period:
                                                The maximum number of DAYS from challenge acknowledgement that a challenge can be scheduled.
                                                Default: 7""")
                                        .withOptionalArguments(new IntegerArgument("challenge-time-period", 0))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "challenge-time-period";
                                            configInt(sender, args, key);
                                        }),
                                new CommandAPICommand("time-selection-period")
                                        .withHelp("Set time-selection-period.", """
                                                Time selection period:
                                                The number of DAYS a challenger is allotted to select one of the provided times.
                                                Default: 3""")
                                        .withOptionalArguments(new IntegerArgument("time-selection-period", 0))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "time-selection-period";
                                            configInt(sender, args, key);
                                        }),
                                new CommandAPICommand("inactivity-timer")
                                        .withHelp("Set inactivity-timer.", """
                                                Inactivity timer:
                                                The number of DAYS that a user can be offline before they are removed from the royalty board.
                                                Default: 30""")
                                        .withOptionalArguments(new IntegerArgument("inactivity-timer", 0))
                                        .executes((sender, args) -> {
                                            @Nullable String key = "inactivity-timer";
                                            configInt(sender, args, key);
                                        }),
                                new CommandAPICommand("waiting-rooms")
                                        .withHelp("Set inactivity-timer.", """
                                                Challenge waiting room location:
                                                The locations of the challenge waiting rooms.
                                                This should be set with /eyeofonyx manage waiting-rooms <tribe> <location>
                                                Do not change this unless you know what you're doing.""")
                                        .withArguments(CommandUtils.customTribeArgument("tribe"))
                                        .withOptionalArguments(
                                                new LocationArgument("location"),
                                                new RotationArgument("rotation"),
                                                new WorldArgument("world")
                                        )
                                        .executes((sender, args) -> {
                                            String key = "waiting-rooms";
                                            @Nullable Location location = (Location) args.get("location");
                                            @Nullable Rotation rotation = (Rotation) args.get("rotation");
                                            @Nullable World world = (World) args.get("world");
                                            @NotNull Tribe tribe = (Tribe) Objects.requireNonNull(args.get("tribe"));
                                            int tribeIndex = TribeUtil.indexOf(tribe);

                                            List<Location> locations = (List<Location>) main.getConfig().getList(key);
                                            assert locations != null;

                                            if (location == null) {
                                                Location currentLoc = locations.get(tribeIndex);
                                                sender.sendMessage(EyeOfOnyx.EOO + key + " of " + tribe.getName() + " is currently set to\n" + currentLoc.getBlockX() + " " + currentLoc.getBlockY() + " " + currentLoc.getBlockZ() + " in " + Objects.requireNonNull(currentLoc.getWorld()).getName());
                                            } else {

                                                if (rotation != null) {
                                                    location.setPitch(rotation.getPitch());
                                                    location.setYaw(rotation.getYaw());
                                                }
                                                if (world == null) {
                                                    if (sender instanceof Player player) location.setWorld(player.getWorld());
                                                    else throw CommandAPI.failWithString("World must be specified if it cannot be inferred!");
                                                } else location.setWorld(world);

                                                locations.set(tribeIndex, location);

                                                main.getConfig().set(key, locations);
                                                main.saveConfig();
                                                sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " of " + tribe.getName() + " to\n" + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + " in " + Objects.requireNonNull(location.getWorld()).getName());
                                            }
                                        }),
                                new CommandAPICommand("tribe-emblems")
                                        .withHelp("Set tribe-emblems.", "Emojis for each of the tribes.\n" +
                                                "Optionally available for Discord royalty board.")
                                        .withArguments(CommandUtils.customTribeArgument("tribe"))
                                        .withOptionalArguments(new TextArgument("emblem-url"))
                                        .executes((sender, args) -> {
                                            String key = "tribe-emblems";
                                            @NotNull Tribe tribe = (Tribe) Objects.requireNonNull(args.get("tribe"));
                                            int tribeIndex = TribeUtil.indexOf(tribe);
                                            @Nullable String url = (String) args.get("emblem-url");

                                            if (url == null) sender.sendMessage(EyeOfOnyx.EOO + key + " of " + tribe.getName() + " is currently set to\n" + main.getConfig().getStringList(key).get(tribeIndex));
                                            else {
                                                List<String> emblemList = main.getConfig().getStringList(key);
                                                emblemList.set(tribeIndex, url);
                                                main.getConfig().set(key, emblemList);
                                                main.saveConfig();
                                                sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " of " + tribe.getName() + " to\n" + url);
                                            }
                                        })
                        )
                );
    }

    private void configInt(CommandSender sender, @NotNull CommandArguments args, String key) {
        Integer value = (Integer) args.get(key);
        if (value == null) sender.sendMessage(EyeOfOnyx.EOO + key + " is currently set to\n" + main.getConfig().getInt(key));
        else {
            main.getConfig().set(key, value);
            main.saveConfig();
            sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to\n" + main.getConfig().getInt(key));
        }
    }

    private void configLong(CommandSender sender, @NotNull CommandArguments args, String key) {
        Long value = (Long) args.get(key);
        if (value == null) sender.sendMessage(EyeOfOnyx.EOO + key + " is currently set to\n" + main.getConfig().getLong(key));
        else {
            main.getConfig().set(key, value);
            main.saveConfig();
            sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to\n" + main.getConfig().getLong(key));
        }
    }

    private void configString(CommandSender sender, @NotNull CommandArguments args, String key) {
        String value = (String) args.get(key);
        if (value == null) sender.sendMessage(EyeOfOnyx.EOO + key + " is currently set to\n" + main.getConfig().getString(key));
        else {
            main.getConfig().set(key, value);
            main.saveConfig();
            sender.sendMessage(EyeOfOnyx.EOO + "Set " + key + " to\n" + main.getConfig().getString(key));
        }
    }
}
