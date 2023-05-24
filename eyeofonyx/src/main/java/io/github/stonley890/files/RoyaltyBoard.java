package io.github.stonley890.files;

import java.io.File;
import java.time.LocalDateTime;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import io.github.stonley890.Main;

public class RoyaltyBoard {

    private static File file;
    private static FileConfiguration boardFile;
    private static Main plugin = Main.getPlugin();

    // Tribe IDs
    static String[] tribes = {
            "hive", "ice", "leaf", "mud", "night", "rain", "sand", "sea", "silk", "sky"
    };

    // Valid positions
    static String[] validPositions = {
            "ruler", "heir_apparent", "heir_presumptive", "noble_apparent", "noble_presumptive"
    };

    public static void setup() {

        file = new File(plugin.getDataFolder(), "board.yml");

        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource("board.yml", false);
        }
        boardFile = YamlConfiguration.loadConfiguration(file);
        save(boardFile);
    }

    public static FileConfiguration get() {
        return boardFile;
    }

    public static void save(FileConfiguration board) {
        boardFile = board;
        try {
            boardFile.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error saving board.yml file:");
            e.printStackTrace();
        }
    }

    public static void reload() {
        boardFile = YamlConfiguration.loadConfiguration(file);
    }

    public static void updateBoard() {

        reload();
        // joined_time access
        String joinedTime;
        // Track current position
        String currentPath;
        // Count number of empty positions
        int positionsEmpty = 0;

        // For each tribe
        for (int i = 0; i < tribes.length; i++) {

            positionsEmpty = 0;
            
            // For each position
            for (int j = 0; j < validPositions.length; j++) {

                // Set current path
                currentPath = tribes[i] + "." + validPositions[j];

                // If joinedTime is before 30 days ago, set to empty
                joinedTime = (String) boardFile.get(currentPath + ".joined_time");
                if (!joinedTime.equals("none")) {
                    if (LocalDateTime.parse(joinedTime).isBefore(LocalDateTime.now().minusDays(30))) {
                        boardFile.set(currentPath + ".joined_time", "none");
                    }
                }

                // If joinedTime is empty, clear position
                if (joinedTime.equals("none")) {

                    // This position is empty, so count up positionsEmpty
                    positionsEmpty += 1;

                    boardFile.set(currentPath + ".uuid", "none");
                    boardFile.set(currentPath + ".name", "none");
                    boardFile.set(currentPath + ".last_challenge_time", "none");
                    boardFile.set(currentPath + ".challenger", "none");

                    // If position is ruler, change 'title'. Else, change 'challenging'
                    if (validPositions[j].equals(validPositions[0])) {
                        boardFile.set(currentPath + ".title", "none");
                    } else {
                        boardFile.set(currentPath + ".challenging", "none");
                    }

                } // If position is held by an active player
                else {

                    // If any previous position was empty, move this user up that many positions
                    // positionsEmpty is initialized as 0 so this cannot run as ruler
                    if (positionsEmpty > 0) {

                        // copy uuid & name to first empty position (thisPosition - emptyPositions)
                        boardFile.set(tribes[i] + "." + validPositions[j - positionsEmpty] + ".uuid",
                                boardFile.get(currentPath + ".uuid"));
                        boardFile.set(tribes[i] + "." + validPositions[j - positionsEmpty] + ".name",
                                boardFile.get(currentPath + ".name"));

                        // Update last_challenge and joined_time
                        // This will give the user movement cooldown
                        boardFile.set(tribes[i] + "." + validPositions[j - positionsEmpty] + ".last_challenge_time",
                                LocalDateTime.now().toString());
                        boardFile.set(tribes[i] + "." + validPositions[j - positionsEmpty] + ".joined_time",
                                LocalDateTime.now().toString());

                        // If previous position is ruler, also set title
                        if (validPositions[j - 1].equals(validPositions[0])) {
                            boardFile.set(tribes[i] + "." + validPositions[j - positionsEmpty] + ".title", "Ruler");
                        }

                        // TO DO: notify challenger of challenge cancellation

                        // Clear data
                        boardFile.set(currentPath + ".uuid", "none");
                        boardFile.set(currentPath + ".name", "none");
                        boardFile.set(currentPath + ".joined_time", "none");
                        boardFile.set(currentPath + ".last_challenge_time", "none");
                        boardFile.set(currentPath + ".challenger", "none");
                        boardFile.set(currentPath + ".challenging", "none");

                        // This position is now empty and another user will move up on the next iteration
                        // if there is an active user below this position
                    }
                }

            }
        }

        save(boardFile);

    }

    private RoyaltyBoard() {
        throw new IllegalStateException("Utility class");
    }
}
