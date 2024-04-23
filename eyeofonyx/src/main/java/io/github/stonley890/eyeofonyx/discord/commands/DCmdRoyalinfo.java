package io.github.stonley890.eyeofonyx.discord.commands;

import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.dreamvisitor.discord.commands.DiscordCommand;
import io.github.stonley890.eyeofonyx.Utils;
import io.github.stonley890.eyeofonyx.files.Challenge;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public class DCmdRoyalinfo implements DiscordCommand {
    @Override
    public @NotNull SlashCommandData getCommandData() {
        return Commands.slash("royalinfo", "Get information about a member on the royalty board.")
                .addOptions(
                        new OptionData(OptionType.STRING, "tribe", "The tribe of the position to fetch.", true, false)
                                .addChoice("Hive", "hive")
                                .addChoice("Ice", "ice")
                                .addChoice("Leaf", "leaf")
                                .addChoice("Mud", "mud")
                                .addChoice("Night", "night")
                                .addChoice("Rain", "rain")
                                .addChoice("Sand", "sand")
                                .addChoice("Sea", "sea")
                                .addChoice("Silk", "silk")
                                .addChoice("Sky", "sky"),
                        new OptionData(OptionType.STRING, "position", "The position to fetch.", true, false)
                                .addChoice("Ruler", "ruler")
                                .addChoice("Crown Heir", "crown_heir")
                                .addChoice("Apparent Heir", "apparent_heir")
                                .addChoice("Presumptive Heir", "presumptive_heir")
                                .addChoice("Crown Noble", "crown_noble")
                                .addChoice("Grand Noble", "grand_noble")
                                .addChoice("High Noble", "high_noble")
                                .addChoice("Apparent Noble", "apparent_noble")
                                .addChoice("Presumptive Noble", "presumptive_noble")
                ).setDefaultPermissions(DefaultMemberPermissions.ENABLED);
    }

    @Override
    public void onCommand(@NotNull SlashCommandInteractionEvent event) {

        String tribeOption = event.getOption("tribe", OptionMapping::getAsString);
        if (tribeOption == null) {
            event.reply("Tribe is null!").queue();
            return;
        }
        String posOption = event.getOption("position", OptionMapping::getAsString);
        if (posOption == null) {
            event.reply("Position is null!").queue();
            return;
        }

        int tribe = Utils.tribeIndexFromString(tribeOption);
        if (tribe == -1) {
            event.reply("Tribe is invalid!").queue();
            return;
        }
        int pos = Utils.posIndexFromString(posOption);
        if (pos == -1) {
            event.reply("Position is invalid!").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();

        UUID uuid = RoyaltyBoard.getUuid(tribe, pos);

        String displayName = RoyaltyBoard.getOcName(tribe, pos);

        LocalDateTime joinedBoardDate = RoyaltyBoard.getJoinedBoardDate(tribe, pos);
        String joinedBoardDateString;

        LocalDateTime joinedPosDate = RoyaltyBoard.getJoinedPosDate(tribe, pos);
        String joinedPosDateString;

        LocalDateTime cooldownEnd = RoyaltyBoard.getCooldownEnd(tribe, pos);
        String cooldownEndString;

        embed.setTitle(RoyaltyBoard.getTeamNames()[tribe] + " " + RoyaltyBoard.getValidPositions()[pos].toUpperCase().replace('_', ' ') + " Info");

        if (uuid == null) {
            embed.setDescription("This position is currently not occupied.");
            event.replyEmbeds(embed.build()).queue();
        } else {
            String username = PlayerUtility.getUsernameOfUuid(uuid);
            if (username == null) username = "NOTFOUND";

            if (displayName == null) displayName = "Not set";

            if (joinedBoardDate == null) joinedBoardDateString = "Unknown";
            else joinedBoardDateString = TimeFormat.DATE_SHORT.format(joinedBoardDate.toEpochSecond(ZoneOffset.UTC));

            if (joinedPosDate == null) joinedPosDateString = "Unknown";
            else joinedPosDateString = TimeFormat.DATE_SHORT.format(joinedPosDate.toEpochSecond(ZoneOffset.UTC));

            if (cooldownEnd == null) cooldownEndString = "Unknown";
            else cooldownEndString = TimeFormat.DATE_SHORT.format(cooldownEnd.toEpochSecond(ZoneOffset.UTC));

            embed.addField("Username", username, true)
                    .addField("Character Name", ChatColor.stripColor(displayName), true)
                    .addField("Joined Board", joinedBoardDateString, true)
                    .addField("Joined Position", joinedPosDateString, true);

            Challenge challenge = Challenge.getChallenge(uuid);

            if (challenge != null) {
                UUID opponentUuid;
                String opponentUsername;

                if (uuid.equals(challenge.attacker)) {
                    opponentUuid = challenge.defender;
                } else {
                    opponentUuid = challenge.attacker;
                }

                opponentUsername = PlayerUtility.getUsernameOfUuid(opponentUuid);
                if (opponentUsername == null) opponentUsername = "NOTFOUND";

                embed.addField(
                        "Challenge Status",
                        "This player is currently organizing a challenge with " + opponentUsername,
                        false
                );
            } else {
                if (RoyaltyBoard.isOnCoolDown(tribe, pos)) {
                    embed.addField(
                            "Challenge Status",
                            "This player is on challenge cool-down until " + cooldownEndString,
                            false
                    );
                } else {
                    embed.addField(
                            "Challenge Status",
                            "This player is not currently organizing a challenge.",
                            false
                    );
                }
            }

            event.replyEmbeds(embed.build()).queue();

        }
    }
}
