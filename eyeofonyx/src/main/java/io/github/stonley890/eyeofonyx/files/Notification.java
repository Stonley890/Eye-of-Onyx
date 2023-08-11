package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Notification {

    private static File file;
    private static FileConfiguration fileConfig;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();

    private final String player;
    private final String title;
    private final String content;
    public final NotificationType type;
    private LocalDateTime time;

    /**
     * Initializes the notification storage.
     * @throws IOException If the file could not be created.
     */
    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "notifications.yml");

        if (!file.exists()) {
            Dreamvisitor.debug("notifications.yml does not exist. Creating one...");
            file.createNewFile();
        }

        fileConfig = YamlConfiguration.loadConfiguration(file);
        save(fileConfig);
    }

    /**
     * Saves the current file configuration to disk.
     * @param board The file configuration to save.
     */
    private static void save(FileConfiguration board) {
        fileConfig = board;
        try {
            fileConfig.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving notifications.yml file:");
            e.printStackTrace();
        }
    }

    /**
     * Saves the notification to notifications.yml on disk
     * @param notification The notification to save.
     * @throws IOException If file could not be accessed.
     */
    public static void saveNotification(Notification notification) throws IOException {

        /* Notifications in notification.yml are saved as a list of string lists for each player

        eedf3c55-2e73-4e73-99a7-81d953745f0a:
            -   - Title
                - Content
                - TYPE
                - 'uuuu-MM-dd'

            -   - Title
                - Content
                - TYPE
                - 'uuuu-MM-dd'

        ...and so on

         */

        // Get list of notifications of given player
        List<List<String>> notifications = (List<List<String>>) fileConfig.getList(notification.player);

        // Init if null or empty
        if (notifications == null || notifications.isEmpty()) {
            notifications = new ArrayList<>();
        }

        // Notification -> List
        List<String> yamlNotification = new ArrayList<>();

        yamlNotification.add(notification.title);
        yamlNotification.add(notification.content);
        yamlNotification.add(notification.type.toString());
        yamlNotification.add(notification.time.toString());

        // Add given notifications
        notifications.add(yamlNotification);

        fileConfig.set(notification.player, notifications);
        save(fileConfig);
    }

    /**
     * Retrieves all notifications of a given player UUID.
     * @param uuid The player UUID to get notifications of.
     * @return A list of {@link Notification}s. If none exist, the list will return empty.
     * @throws IOException If the file could not be accessed.
     * @throws InvalidConfigurationException If the configuration is invalid.
     */
    public static @NotNull List<Notification> getNotificationsOfPlayer(String uuid) throws IOException, InvalidConfigurationException {

        fileConfig.load(file);

        // Get list of notifications of given player
        List<List<String>> yamlNotifications = (List<List<String>>) fileConfig.getList(uuid);

        Dreamvisitor.debug("Number of notifications for " + uuid + ": " + (yamlNotifications != null ? yamlNotifications.size() : 0));

        List<Notification> notifications = new ArrayList<>();

        if (yamlNotifications == null || yamlNotifications.isEmpty()) {
            notifications = new ArrayList<>();
        } else {
            for (List<String> yamlNotification : yamlNotifications) {

                // Get saved values
                String title = yamlNotification.get(0);
                String content = yamlNotification.get(1);
                NotificationType type = NotificationType.valueOf(yamlNotification.get(2));
                LocalDateTime time = LocalDateTime.parse(yamlNotification.get(3));

                // Add to a Notification object
                Notification notification = new Notification(uuid, title, content, type);
                notification.time = time;

                // Add to list
                notifications.add(notification);
            }
        }

        // Return List<Notification> that was built
        return notifications;
    }

    /**
     * Creates a notification. Append .create() to automatically try to send and save to disk.
     * @param playerUuid The UUID of the player to deliver this message to.
     * @param notificationTitle The title of the message.
     * @param notificationContent The content of the message.
     * @param notificationType The type of Notification. This will determine the buttons added and actions taken upon delivery.
     */
    public Notification(String playerUuid, String notificationTitle, String notificationContent, NotificationType notificationType) {
        player = playerUuid;
        title = notificationTitle;
        content = notificationContent;
        type = notificationType;
        time = LocalDateTime.now();
    }

    /**
     * Saves the Notification to disk and attempts to send it.
     */
    public void create() {
        try {
            saveNotification(this);
            if (Bukkit.getPlayer(UUID.fromString(this.player)) != null) {
                sendMessage();
            }
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Attempts to send the message to the player.
     * @return Whether the message was sent.
     * @throws IOException If the file could not be accessed.
     * @throws InvalidConfigurationException If the configuration is invalid.
     */
    public boolean sendMessage() throws IOException, InvalidConfigurationException {
        Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(player));
        if (onlinePlayer != null) {

            /*

            [EoO] YYYY-MM-DD HH:MM $Title$
            $Content$

             */

            // Create message
            ComponentBuilder message = new ComponentBuilder();
            message.append(EyeOfOnyx.EOO).append(time.format(DateTimeFormatter.ISO_DATE)).append(" ").append(time.format(DateTimeFormatter.ofPattern("hh:mm a")))
                    .append("\n").append(title).color(net.md_5.bungee.api.ChatColor.YELLOW).bold(true)
                    .append("\n").append(content).reset().italic(true).color(net.md_5.bungee.api.ChatColor.GRAY)
                    .append("\n").reset();

            // Create buttons
            List<TextComponent> buttons = new ArrayList<>();

            // NotificationType determines buttons and post-notify actions

            if (type == NotificationType.CHALLENGE_REQUESTED) {

                // Add accept and forfeit buttons

                TextComponent accept = new TextComponent();
                accept.setText("[Accept Challenge]");
                accept.setColor(ChatColor.GREEN);
                accept.setUnderlined(true);
                accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge accept"));

                TextComponent deny = new TextComponent();
                deny.setText("[Forfeit]");
                deny.setColor(ChatColor.RED);
                deny.setUnderlined(true);
                deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge deny"));

                buttons.add(accept);
                buttons.add(deny);

            } else if (type == NotificationType.CHALLENGE_ACCEPTED) {

                // Add buttons for dates

                // Get challenge
                List<Challenge> challenges = Challenge.getChallenges();
                Challenge challenge = null;
                for (Challenge listChallenge : challenges) {
                    if (listChallenge.attacker.equals(player)) {
                        challenge = listChallenge;
                        break;
                    }
                }

                if (challenge == null) {
                    // The challenge was not found
                    // Should not happen
                    onlinePlayer.sendMessage(EyeOfOnyx.EOO + ChatColor.RED + "You had a challenge accepted, but it couldn't be found! Contact staff for help.");
                    return true;
                }

                List<LocalDateTime> dates = challenge.time;

                // Create button for each date
                for (int i = 0; i < dates.size(); i++) {
                    TextComponent button = new TextComponent();
                    button.setText("[" + dates.get(i).format(DateTimeFormatter.ofPattern("MM dd uuuu hh:mm a")) + "]");
                    button.setColor(ChatColor.YELLOW);
                    button.setUnderlined(true);
                    button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge date " + i));

                    buttons.add(button);
                }

                // Button for no matching availabilities

                TextComponent deny = new TextComponent();
                deny.setText("[I can't make these times]");
                deny.setColor(ChatColor.RED);
                deny.setUnderlined(true);
                deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge ignore"));

            }   else if (type == NotificationType.GENERIC) {

                // Do not show notification again
                Notification.getNotificationsOfPlayer(player).remove(this);

            }

            // Add buttons to message
            if (!buttons.isEmpty()) {
                for (TextComponent button : buttons) {
                    message.append(button).append(" ").reset();
                }
                message.append("\n");
            }

            // Send the message to player
            onlinePlayer.spigot().sendMessage(message.create());

            return true;
        } else {
            return false;
        }
    }
}
