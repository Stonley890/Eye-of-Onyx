package io.github.stonley890.eyeofonyx.discord.commands;

import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.dreamvisitor.data.TribeUtil;
import io.github.stonley890.dreamvisitor.discord.commands.DiscordCommand;
import io.github.stonley890.eyeofonyx.discord.Discord;
import io.github.stonley890.eyeofonyx.files.Challenge;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

public class DCmdFreezePosition implements DiscordCommand {
    @NotNull
    @Override
    public SlashCommandData getCommandData() {
        return Commands.slash("freezeposition", "Toggle total immunity of a position on the royalty board.")
                .addOptions(Discord.tribeOption, Discord.posOption)
                .addOption(OptionType.BOOLEAN, "frozen", "Whether to make frozen or unfrozen.", false)
                .setDefaultPermissions(DefaultMemberPermissions.DISABLED);
    }

    @Override
    public void onCommand(@NotNull SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        Tribe tribe = TribeUtil.parse(event.getOption("tribe", OptionMapping::getAsString));
        if (tribe == null) {
            event.getHook().editOriginal("That tribe could not be parsed.").queue();
            return;
        }
        String pos = event.getOption("position", OptionMapping::getAsString);
        if (pos == null) {
            event.getHook().editOriginal("position cannot be null.").queue();
            return;
        }

        int targetPos = -1;

        // Check if third argument contains a valid position
        if (Arrays.stream(RoyaltyBoard.getValidPositions()).anyMatch(pos::contains)) {
            for (int i = 0; i < RoyaltyBoard.getValidPositions().length; i++) {
                if (pos.equals(RoyaltyBoard.getValidPositions()[i])) targetPos = i;
            }
        }
        if (targetPos == -1) {
            event.getHook().editOriginal("That position could not be parsed.").queue();
            return;
        }

        if (RoyaltyBoard.isPositionEmpty(tribe, targetPos)) {
            event.getHook().editOriginal("That position is empty and cannot be frozen or unfrozen.").queue();
            return;
        }

        Boolean setFrozen = event.getOption("frozen", OptionMapping::getAsBoolean);

        if (setFrozen == null) {
            setFrozen = !RoyaltyBoard.isPosFrozen(tribe, targetPos);
        }

        RoyaltyBoard.setFrozen(tribe, targetPos, setFrozen);
        if (!setFrozen) {
            // set last online to now to make sure they don't get auto-removed
            RoyaltyBoard.setLastOnline(tribe, targetPos, LocalDateTime.now());
        } else {
            Challenge.removeChallengesOfPlayer(Objects.requireNonNull(RoyaltyBoard.getUuid(tribe, targetPos)), "The position you were challenging has been frozen and challenges cannot be executed on them.");
        }
        event.getHook().editOriginal(tribe.getName() + " " + pos + " frozen set to " + setFrozen).queue();
    }
}
