package io.github.stonley890.eyeofonyx.challenges;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.Challenge;
import io.github.stonley890.eyeofonyx.files.ChallengeType;
import io.github.stonley890.eyeofonyx.files.RoyaltyBoard;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Competition {

    public static List<Competition> activeChallenges = new ArrayList<>();

    public String attacker;
    public String defender;
    public int tribe;
    public ChallengeType type;

    public static void call(Challenge challenge) throws IOException, InvalidConfigurationException {

        // Add to activeChallenges list
        activeChallenges.add(new Competition(challenge));

        // Remove from saved challenges
        Challenge.getChallenges().remove(challenge);

    }

    private Competition(Challenge challenge) {
        attacker = challenge.attacker;
        defender = challenge.defender;
        tribe = RoyaltyBoard.getTribeIndexOfUUID(attacker);
        type = challenge.type;
    }

    public void callToJoin() {

        ComponentBuilder message = new ComponentBuilder(EyeOfOnyx.EOO);
        message.append("Your challenge is starting!\n");
        TextComponent button = new TextComponent("[Teleport]");
        button.setColor(ChatColor.GREEN);
        button.setUnderlined(true);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge start"));
        message.append(button);
        message.append("\n");

        if (Bukkit.getPlayer(attacker) != null) {
            Bukkit.getPlayer(attacker).spigot().sendMessage(message.create());
        }
        if (Bukkit.getPlayer(defender) != null) {
            Bukkit.getPlayer(defender).spigot().sendMessage(message.create());
        }

    }

}
