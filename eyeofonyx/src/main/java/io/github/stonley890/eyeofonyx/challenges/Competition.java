package io.github.stonley890.eyeofonyx.challenges;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.Challenge;
import io.github.stonley890.eyeofonyx.files.PlayerTribe;
import javassist.NotFoundException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class Competition {

    public static Competition activeChallenge;

    public UUID attacker;
    public UUID defender;
    public int tribe;
    public boolean started = false;

    public static void call(@NotNull Challenge challenge) {

        Dreamvisitor.debug("Challenge " + challenge.attacker + " vs " + challenge.defender + " is getting ready to start.");
        // Add to activeChallenges list
        try {
            activeChallenge = new Competition(challenge);
            activeChallenge.callToJoin();
        } catch (NotFoundException e) {
            // No associate tribe (should not happen)
            Bukkit.getLogger().warning("Tried to create a challenge, but could not find the tribe associated with the attacker!");
        }

        // Remove from saved challenges
        Challenge.remove(challenge);

    }

    private Competition(@NotNull Challenge challenge) throws NotFoundException {
        attacker = challenge.attacker;
        defender = challenge.defender;
        tribe = PlayerTribe.getTribeOfPlayer(attacker);
    }

    public void callToJoin() {

        Dreamvisitor.debug("Calling to join...");

        ComponentBuilder message = new ComponentBuilder(EyeOfOnyx.EOO);
        message.append("Your challenge is starting!\n");
        TextComponent button = new TextComponent("[Teleport]");
        button.setColor(ChatColor.GREEN);
        button.setUnderlined(true);
        button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge start"));
        message.append(button);
        message.append("\n");

        Dreamvisitor.debug("Message built. Sending to " + this.attacker + " and " + this.defender + ".");

        if (Bukkit.getPlayer(this.attacker) != null) {
            Objects.requireNonNull(Bukkit.getPlayer(this.attacker)).spigot().sendMessage(message.create());
            Dreamvisitor.debug("Send message to attacker.");
        }
        if (Bukkit.getPlayer(this.defender) != null) {
            Objects.requireNonNull(Bukkit.getPlayer(this.defender)).spigot().sendMessage(message.create());
            Dreamvisitor.debug("Send message to defender.");
        }
    }

}
