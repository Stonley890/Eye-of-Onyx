package io.github.stonley890.eyeofonyx.challenges;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.dreamvisitor.data.Tribe;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.files.Challenge;
import io.github.stonley890.dreamvisitor.data.PlayerTribe;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class Competition {

    public static Competition activeChallenge;

    public final UUID attacker;
    public final UUID defender;
    public final Tribe tribe;
    public boolean started = false;

    public static void call(@NotNull Challenge challenge) {

        Dreamvisitor.debug("Challenge " + challenge.attacker + " vs " + challenge.defender + " is getting ready to start.");
        // Add to activeChallenges list
        activeChallenge = new Competition(challenge);
        activeChallenge.callToJoin();

        // schedule expire
        Bukkit.getScheduler().runTaskLater(EyeOfOnyx.getPlugin(), () -> {
            if (!activeChallenge.started) Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "competition cancel");
        }, 20 * 60 * 5);

        // Remove from saved challenges
        Challenge.remove(challenge);

    }

    private Competition(@NotNull Challenge challenge) {
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
