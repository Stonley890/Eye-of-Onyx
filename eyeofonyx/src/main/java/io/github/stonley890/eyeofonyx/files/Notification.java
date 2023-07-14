package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Notification {

    private static File file;
    private static FileConfiguration fileConfig;
    private static final Dreamvisitor plugin = Dreamvisitor.getPlugin();

    private String player;
    private String title;
    private String content;
    private NotificationType type;
    private LocalDateTime time;

    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "notifications.yml");

        if (!file.exists()) {
            if (file.getParentFile().mkdirs()) {
                file.createNewFile();
            }
        }
        fileConfig = YamlConfiguration.loadConfiguration(file);
        save(fileConfig);
    }

    private static void save(FileConfiguration board) {
        fileConfig = board;
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving notifications.yml file:");
            e.printStackTrace();
        }
    }

    public static void saveNotification(Notification notification) {
        // Get list of notifications of given player
        List<Notification> notifications = (List<Notification>) fileConfig.getList(notification.player);

        // Init if null or empty
        if (notifications == null || notifications.isEmpty()) {
            notifications = new ArrayList<>();
        }

        // Add given notifications
        notifications.add(notification);

        fileConfig.set(notification.player, notifications);
    }

    public static List<Notification> getNotificationsOfPlayer(String uuid) {
        // Get list of notifications of given player
        return (List<Notification>) fileConfig.getList(uuid);
    }

    public Notification(String playerUuid, String notificationTitle, String notificationContent, NotificationType notificationType) {
        player = playerUuid;
        title = notificationTitle;
        content = notificationContent;
        type = notificationType;
        time = LocalDateTime.now();


    }

    // Attempt to send message
    private boolean sendMessage() {
        if (Bukkit.getPlayer(UUID.fromString(player)) != null) {
            StringBuilder builder = new StringBuilder();

            /*

            [EoO] YYYY-MM-DD HH:MM $Title$
            $Content$

             */

            builder.append(EyeOfOnyx.EOO)
                    .append(time.format(DateTimeFormatter.ISO_DATE)).append(" ").append(time.format(DateTimeFormatter.ISO_TIME))
                    .append(ChatColor.YELLOW).append(ChatColor.BOLD).append(title)
                    .append(ChatColor.RESET).append("\n").append(content);

            Bukkit.getPlayer(UUID.fromString(player)).sendMessage(builder.toString());

            return true;
        } else {
            return false;
        }
    }



}
