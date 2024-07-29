package io.github.stonley890.eyeofonyx.discord;

import io.github.stonley890.dreamvisitor.Bot;
import io.github.stonley890.dreamvisitor.data.AccountLink;
import io.github.stonley890.dreamvisitor.data.PlayerUtility;
import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.dreamvisitor.discord.DiscCommandsManager;
import io.github.stonley890.dreamvisitor.discord.commands.DiscordCommand;
import io.github.stonley890.eyeofonyx.discord.commands.DCmdEyeOfOnyx;
import io.github.stonley890.eyeofonyx.discord.commands.DCmdRoyalinfo;
import io.github.stonley890.eyeofonyx.discord.commands.DCmdRoyalty;
import io.github.stonley890.eyeofonyx.files.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

public class Discord extends ListenerAdapter {

    public static final OptionData tribeOption = new OptionData(OptionType.STRING, "tribe", "The tribe to write to.")
            .setAutoComplete(false)
            .addChoice("Hive", "hive")
            .addChoice("Ice", "ice")
            .addChoice("Leaf", "leaf")
            .addChoice("Mud", "mud")
            .addChoice("Night", "night")
            .addChoice("Rain", "rain")
            .addChoice("Sand", "sand")
            .addChoice("Sea", "sea")
            .addChoice("Silk", "silk")
            .addChoice("Sky", "sky");

    public static final OptionData posOption = new OptionData(OptionType.STRING, "position", "The position to target.", true)
            .setAutoComplete(false)
            .addChoice("Ruler", "ruler")
            .addChoice("Crown Heir", "crown_heir")
            .addChoice("Apparent Heir", "apparent_heir")
            .addChoice("Presumptive Heir", "presumptive_heir")
            .addChoice("Crown Noble", "crown_noble")
            .addChoice("Grand Noble", "grand_noble")
            .addChoice("High Noble", "high_noble")
            .addChoice("Apparent Noble", "apparent_noble")
            .addChoice("Presumptive Noble", "presumptive_noble");

    @SuppressWarnings({"null"})
    public static void initCommands() {
        List<DiscordCommand> commands = new ArrayList<>();

        commands.add(new DCmdEyeOfOnyx());
        commands.add(new DCmdRoyalinfo());
        commands.add(new DCmdRoyalty());

        DiscCommandsManager.addCommands(commands);

        commands.clear();

        Bot.getJda().addEventListener(new Discord());

    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

        Button button = event.getButton();

        String id = button.getId();
        if (id == null) return;

        if (id.startsWith("revertaction-")) {

            String targetId = id.substring("revertaction-".length());

            for (RoyaltyAction royaltyAction : RoyaltyAction.actionHistory) {
                if (targetId.equals(String.valueOf(royaltyAction.id))) {
                    EmbedBuilder embed = new EmbedBuilder();
                    embed.setTitle("Are you sure?")
                            .setDescription("Reverting this action will revert all values of this tribe to the values they were set to before this change.")
                            .setFooter("This action cannot be undone.");

                    for (int pos = 0; pos < RoyaltyBoard.CIVILIAN; pos++) {


                        BoardPosition position = royaltyAction.oldState.getPos(pos);

                        StringBuilder values = new StringBuilder();

                        String uuid = "N/A";
                        String username = "N/A";
                        String ocName = "N/A";
                        String lastOnline = "N/A";
                        String lastChallenge = "N/A";
                        String joinedBoard = "N/A";
                        String joinedPos = "N/A";

                        // OLD
                        if (position.player != null) uuid = position.player.toString();
                        if (position.player != null) username = PlayerUtility.getUsernameOfUuid(uuid);
                        if (position.name != null) ocName = position.name;
                        if (position.lastOnline != null) lastOnline = position.lastOnline.toString();
                        if (position.lastChallenge != null) lastChallenge = position.lastChallenge.toString();
                        if (position.joinedBoard != null) joinedBoard = position.joinedBoard.toString();
                        if (position.joinedPosition != null) joinedPos = position.joinedPosition.toString();

                        String discordUser = "N/A";

                        try {
                            long discordId = AccountLink.getDiscordId(Objects.requireNonNull(position.player));
                            net.dv8tion.jda.api.entities.User user = Bot.getJda().retrieveUserById(discordId).complete();
                            if (user != null) discordUser = user.getAsMention();
                        } catch (NullPointerException IllegalArgumentException) {
                            // No user
                        }

                        assert username != null;
                        values.append("\nPlayer: ").append(Bot.escapeMarkdownFormatting(username))
                                .append("\nUUID: ").append(uuid)
                                .append("\nUser: ").append(discordUser)
                                .append("\nOC Name: ").append(Bot.escapeMarkdownFormatting(ocName))
                                .append("\nLast Online: ").append(lastOnline)
                                .append("\nLast Challenge: ").append(lastChallenge)
                                .append("\nDate Joined Board: ").append(joinedBoard)
                                .append("\nDate Joined Position: ").append(joinedPos);

                        embed.addField(RoyaltyBoard.getValidPositions()[pos].replace("_", " ").toUpperCase(), values.toString(), false);

                    }

                    Button danger = Button.danger("revertconfirm-" + royaltyAction.id, "Yes, revert to this state.");
                    event.replyEmbeds(embed.build()).addActionRow(danger).queue();
                    break;
                }
            }

        } else if (id.startsWith("revertconfirm-")) {
            String targetId = id.substring("revertconfirm-".length());

            List<RoyaltyAction> actionHistory = RoyaltyAction.actionHistory;
            for (RoyaltyAction royaltyAction : actionHistory) {
                if (targetId.equals(String.valueOf(royaltyAction.id))) {

                    RoyaltyBoard.set(royaltyAction.affectedTribe, royaltyAction.oldState);
                    RoyaltyBoard.updateBoard(royaltyAction.affectedTribe, false);
                    RoyaltyBoard.updateDiscordBoard(royaltyAction.affectedTribe);
                    event.reply("✅ Values reverted.").queue();
                    event.getInteraction().editButton(button.asDisabled()).queue();
                    break;

                }
            }
        } else if (id.startsWith("challenge-delete-")) {
            String uuidString = id.substring("challenge-delete-".length());

            if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.MANAGE_ROLES)) {
                event.reply(event.getMember().getAsMention() + ", you do not have permission to do that! You need " + Permission.MANAGE_ROLES.getName()).queue();
                return;
            }

            UUID uuid = UUID.fromString(uuidString);

            Challenge challenge = Challenge.getChallenge(uuid);
            if (challenge == null) {
                event.reply("That challenge does not exist!").queue();
                return;
            }

            Challenge.remove(challenge);
            // Remove any challenge notifications
            Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_REQUESTED);
            Notification.removeNotificationsOfPlayer(uuid, Notification.Type.CHALLENGE_ACCEPTED);

            Tribe tribe = PlayerTribe.getTribeOfPlayer(challenge.attacker);
            assert tribe != null;

            event.reply("Challenged deleted by " + event.getMember().getAsMention()).queue();
            event.getInteraction().editButton(button.asDisabled()).queue();
        }

    }

}
