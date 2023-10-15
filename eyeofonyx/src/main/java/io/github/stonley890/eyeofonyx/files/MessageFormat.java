package io.github.stonley890.eyeofonyx.files;

import io.github.stonley890.dreamvisitor.Dreamvisitor;
import io.github.stonley890.eyeofonyx.EyeOfOnyx;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;

public class MessageFormat {

    private static File file;
    private static final EyeOfOnyx plugin = EyeOfOnyx.getPlugin();

    /**
     * Initializes the file.
     *
     * @throws IOException If the file could not be created.
     */
    public static void setup() throws IOException {

        file = new File(plugin.getDataFolder(), "messageformat.txt");

        if (!file.exists()) {
            Bukkit.getLogger().info("messageformat.txt does not exist. Creating one...");
            Files.copy(Objects.requireNonNull(EyeOfOnyx.getPlugin().getResource("messageformat.txt")), file.toPath());
        }

    }

    public static String get() throws IOException {
        String string;

        string = Files.readString(file.toPath(), Charset.defaultCharset());

        return string;
    }
}