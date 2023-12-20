package io.github.stonley890.eyeofonyx.listeners;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

public class ListenCmdPreprocess implements Listener {

    @EventHandler
    public void onCmdPreprocess(@NotNull PlayerCommandPreprocessEvent event) {

        if (event.getMessage().startsWith("/tempban")) {

            // Suggest /eyeofonyx ban when using /tempban

            ComponentBuilder message = new ComponentBuilder(EyeOfOnyx.EOO);
            message.append("If this is a temp-ban, use ");

            TextComponent suggestion = new TextComponent("/eyeofonyx ban");
            suggestion.setUnderlined(true);
            suggestion.setColor(ChatColor.YELLOW);
            String targetUser = event.getMessage().substring("/tempban".length());
            targetUser = targetUser.substring(0, targetUser.indexOf(" ") - 1);
            suggestion.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/eyeofonyx ban " + targetUser));

            message.append(suggestion).append(" to restrict access to the royalty board.").color(ChatColor.WHITE);

            event.getPlayer().spigot().sendMessage(message.create());
        }

    }
}
