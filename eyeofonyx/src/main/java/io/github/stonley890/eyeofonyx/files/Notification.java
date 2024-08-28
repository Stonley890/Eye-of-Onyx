package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import io.github.stonley890.eyeofonyx.web.IpUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Notification {

    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();
    private static File file;
    public final Type type;
    private final UUID player;
    private final String title;
    private final String content;
    public LocalDateTime time;
    public boolean seen = false;

    /**
     * Creates a notification. Append .create() to automatically try to send and save to disk.
     *
     * @param playerUuid          The UUID of the player to deliver this message to.
     * @param notificationTitle   The title of the message.
     * @param notificationContent The content of the message.
     * @param notificationType    The type of Notification. This will determine the buttons added and actions taken upon delivery.
     */
    public Notification(UUID playerUuid, String notificationTitle, String notificationContent, Type notificationType) {
        player = playerUuid;
        title = notificationTitle;
        content = notificationContent;
        type = notificationType;
        time = LocalDateTime.now();
    }

    /**
     * Initializes the notification storage.
     *
     * @throws IOException If the file could not be created.
     */
    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "notifications.yml");

        if (!file.exists()) {
            Bukkit.getLogger().info("notifications.yml does not exist. Creating one...");
            if (!file.createNewFile()) {
                Bukkit.getLogger().warning("notifications.yml could not be created!");
            }
        }
    }

    @Contract(" -> new")
    private static @NotNull YamlConfiguration getConfig() {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(file);
            return configuration;
        } catch (IOException | InvalidConfigurationException e) {
            Bukkit.getLogger().severe("Unable to load " + file.getName() + "!");
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new RuntimeException();
        }
    }

    /**
     * Saves the current file configuration to disk.
     *
     * @param board The file configuration to save.
     */
    private static void save(FileConfiguration board) {
        try {
            board.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving notifications.yml file:");
        }
    }

    /**
     * Saves the notification to notifications.yml on disk
     *
     * @param notification The notification to save.
     */
    @SuppressWarnings("unchecked")
    public static void saveNotification(@NotNull Notification notification) {

        /* Notifications in notification.yml are saved as a list of string lists for each player

        eedf3c55-2e73-4e73-99a7-81d953745f0a:
            -   - Title
                - Content
                - TYPE
                - 'uuuu-MM-dd'
                - false

            -   - Title
                - Content
                - TYPE
                - 'uuuu-MM-dd'
                - true

        ...and so on

         */

        YamlConfiguration fileConfig = getConfig();

        // Get list of notifications of given player
        List<List<String>> notifications = (List<List<String>>) fileConfig.getList(notification.player.toString());

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
        yamlNotification.add(String.valueOf(notification.seen));

        // Add given notifications
        notifications.add(yamlNotification);

        fileConfig.set(notification.player.toString(), notifications);
        save(fileConfig);
    }

    /**
     * Retrieves all notifications of a given player UUID.
     *
     * @param uuid The player UUID to get notifications of.
     * @return A list of {@link Notification}s. If none exist, the list will return empty.
     */
    @SuppressWarnings("unchecked")
    public static @NotNull List<Notification> getNotificationsOfPlayer(@NotNull UUID uuid) {

        YamlConfiguration fileConfig = getConfig();

        List<Notification> notifications = new ArrayList<>();

        // Get list of notifications of given player
        List<List<String>> yamlNotifications;
        try {
            yamlNotifications = (List<List<String>>) fileConfig.getList(uuid.toString());
        } catch (NullPointerException e) {
            return notifications;
        }

        if (yamlNotifications == null || yamlNotifications.isEmpty()) {
            notifications = new ArrayList<>();
        } else {
            for (List<String> yamlNotification : yamlNotifications) {

                // Get saved values
                String title = yamlNotification.get(0);
                String content = yamlNotification.get(1);
                Type type = Type.valueOf(yamlNotification.get(2));
                LocalDateTime time = LocalDateTime.parse(yamlNotification.get(3));
                boolean seen = Boolean.parseBoolean(yamlNotification.get(4));

                // Add to a Notification object
                Notification notification = new Notification(uuid, title, content, type);
                notification.time = time;
                notification.seen = seen;

                // Add to list
                notifications.add(notification);
            }
        }

        // Return List<Notification> that was built
        return notifications;
    }

    public static void removeNotification(@NotNull Notification notification) {
        YamlConfiguration fileConfig = getConfig();

        // Get list of notifications of given player
        List<Notification> notifications = getNotificationsOfPlayer(notification.player);

        if (notifications.isEmpty()) {
            return;
        }

        // Remove given notification
        notifications.removeIf(next -> next.equals(notification));

        // Notification -> List
        List<List<String>> yamlNotifications = new ArrayList<>();

        for (Notification notification1 : notifications) {
            List<String> yamlNotification = new ArrayList<>();

            yamlNotification.add(notification1.title);
            yamlNotification.add(notification1.content);
            yamlNotification.add(notification1.type.toString());
            yamlNotification.add(notification1.time.toString());
            yamlNotification.add(String.valueOf(notification1.seen));

            // Add given notifications
            yamlNotifications = new ArrayList<>();
            yamlNotifications.add(yamlNotification);
        }

        fileConfig.set(notification.player.toString(), yamlNotifications);
        save(fileConfig);
    }

    public static void removeNotificationsOfPlayer(@NotNull UUID uuid, Type type) {
        for (Notification notification : Notification.getNotificationsOfPlayer(uuid)) {
            if (notification.type == type)
                Notification.removeNotification(notification);
        }
    }

    /**
     * Saves the Notification to disk and attempts to send it.
     */
    public void create() {
        saveNotification(this);
        if (Bukkit.getPlayer(this.player) != null) {
            sendMessage();
        }
    }

    /**
     * Attempts to send the message to the player.
     */
    public void sendMessage() {
        Player onlinePlayer = Bukkit.getPlayer(player);
        if (onlinePlayer != null) {

            boolean remove = false;

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

            if (type == Type.CHALLENGE_REQUESTED) {

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

                RoyaltyBoard.report(onlinePlayer.getName(), onlinePlayer.getName() + " has been shown the notification for their challenge request.");

            } else if (type == Type.CHALLENGE_ACCEPTED) {

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
                    removeNotification(this);
                    return;
                }

                List<LocalDateTime> dates = challenge.time;
                List<ZonedDateTime> offsetDates = new ArrayList<>();

                ZonedDateTime playerTime = IpUtils.ipToTime(Objects.requireNonNull(onlinePlayer.getAddress()).getAddress().getHostAddress());
                if (playerTime != null) {
                    ZoneId playerOffset = playerTime.getZone();

                    for (LocalDateTime localDate : dates) {
                        offsetDates.add(ZonedDateTime.of(localDate, playerOffset));
                    }
                }


                // Create button for each date
                for (int i = 0; i < dates.size(); i++) {
                    TextComponent button = new TextComponent();

                    if (playerTime == null)
                        button.setText("[" + dates.get(i).format(DateTimeFormatter.ofPattern("MM dd uuuu hh:mm a ")) + ZoneOffset.systemDefault().getId() + "]\n");
                    else
                        button.setText("[" + offsetDates.get(i).format(DateTimeFormatter.ofPattern("MM dd uuuu hh:mm a z")) + "]\n");

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

                buttons.add(deny);

                RoyaltyBoard.report(onlinePlayer.getName(), onlinePlayer.getName() + " has been shown the notification for their challenge scheduling.");

            } else if (type == Type.GENERIC) {

                // Do not show notification again
                remove = true;

            } else if (type == Type.QUICK_CHALLENGE) {

                TextComponent accept = new TextComponent();
                accept.setText("[Accept]");
                accept.setColor(ChatColor.YELLOW);
                accept.setUnderlined(true);
                accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Start the quick challenge now.")));
                accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/challenge quickaccept"));

                buttons.add(accept);

                remove = true;

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

            // Mark notification as seen
            if (remove) removeNotification(this);
            else {
                this.seen = true;
                Notification.saveNotification(this);
            }

        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof Notification that) {
            return (
                    this.player == that.player &&
                            this.type == that.type &&
                            Objects.equals(this.content, that.content) &&
                            Objects.equals(this.time, that.time) &&
                            Objects.equals(this.title, that.title) &&
                            this.seen == that.seen
            );
        }
        return false;
    }

    public enum Type {

        /**
         * Generic notification.
         * Deletes when read.
         */
        GENERIC,

        /**
         * Challenge sent notification.
         * Includes buttons to accept or deny.
         * Deletes when acknowledged.
         */
        CHALLENGE_REQUESTED,

        /**
         * Challenge accepted notification.
         * Includes buttons with available times.
         * Deletes when acknowledged.
         */
        CHALLENGE_ACCEPTED,

        /**
         * Quick challenge request notification.
         * Includes accept and deny buttons.
         * Deletes when read.
         */
        QUICK_CHALLENGE

    }
}
