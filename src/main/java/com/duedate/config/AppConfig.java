package com.duedate.config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Handles application configuration stored in $HOME/AppData/DueDates/config.properties
 */
public class AppConfig {

    private static final String APP_DIR_NAME = "DueDates";
    private static final String CONFIG_FILE_NAME = "config.properties";

    private Path configDirectory;
    private Path configFile;
    private final Properties properties = new Properties();

    private Path dataFolder;
    private boolean startWithWindows = false;
    private boolean minimizeToTray = true;
    private boolean soundChime = true;
    private boolean startupNotification = true;
    private int persistentReminderMinutes = 60;
    private boolean notifyToday = true;
    private boolean notifyThisWeek = true;
    private boolean notifyOverdue = true;
    private String defaultPerson = "Guest";
    private String theme = "DARK";
    private List<String> visibleColumns = new ArrayList<>(List.of(
            "Index", "Title", "Since", "Deadline", "Remaining", "Person", "Description", "FileName"
    ));

    public AppConfig() {
        this.configDirectory = resolveAppDir();
        this.configFile = configDirectory.resolve(CONFIG_FILE_NAME);
        this.dataFolder = configDirectory.resolve("deadlines");
        ensureDirectories();
        load();
    }

    public static Path resolveAppDir() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            Path p = Paths.get(appData, APP_DIR_NAME);
            try {
                if (!Files.exists(p)) {
                    Files.createDirectories(p);
                }
                return p;
            } catch (Exception ignored) {
                // If APPDATA is restricted, fallback below
            }
        }
        String userHome = System.getProperty("user.home");
        Path winPath = Paths.get(userHome, "AppData", "Roaming", APP_DIR_NAME);
        try {
            if (Files.exists(winPath.getParent())) {
                if (!Files.exists(winPath)) Files.createDirectories(winPath);
                return winPath;
            }
        } catch (Exception ignored) {}

        // Portable local directory fallback
        Path localPath = Paths.get("data", APP_DIR_NAME);
        try {
            if (!Files.exists(localPath)) Files.createDirectories(localPath);
            return localPath;
        } catch (Exception ignored) {}

        return Paths.get(APP_DIR_NAME);
    }

    private void ensureDirectories() {
        try {
            if (!Files.exists(configDirectory)) {
                Files.createDirectories(configDirectory);
            }
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }
        } catch (Exception e) {
            System.err.println("Could not create directories: " + e.getMessage() + ". Switching to local workspace directory.");
            this.configDirectory = Paths.get("data");
            this.configFile = configDirectory.resolve(CONFIG_FILE_NAME);
            this.dataFolder = configDirectory.resolve("deadlines");
            try {
                Files.createDirectories(dataFolder);
            } catch (Exception ex) {
                System.err.println("Local directory fallback error: " + ex.getMessage());
            }
        }
    }

    public void load() {
        if (!Files.exists(configFile)) {
            save(); // save defaults
            return;
        }

        try (InputStream in = Files.newInputStream(configFile)) {
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));

            String folderProp = properties.getProperty("dataFolder");
            if (folderProp != null && !folderProp.isBlank()) {
                dataFolder = Paths.get(folderProp);
            }

            startWithWindows = Boolean.parseBoolean(properties.getProperty("startWithWindows", "false"));
            minimizeToTray = Boolean.parseBoolean(properties.getProperty("minimizeToTray", "true"));
            soundChime = Boolean.parseBoolean(properties.getProperty("soundChime", "true"));
            startupNotification = Boolean.parseBoolean(properties.getProperty("startupNotification", "true"));
            notifyToday = Boolean.parseBoolean(properties.getProperty("notifyToday", "true"));
            notifyThisWeek = Boolean.parseBoolean(properties.getProperty("notifyThisWeek", "true"));
            notifyOverdue = Boolean.parseBoolean(properties.getProperty("notifyOverdue", "true"));
            defaultPerson = properties.getProperty("defaultPerson", "Guest");
            theme = properties.getProperty("theme", "DARK");

            try {
                persistentReminderMinutes = Integer.parseInt(properties.getProperty("persistentReminderMinutes", "60"));
            } catch (NumberFormatException ignored) {}

            String cols = properties.getProperty("visibleColumns");
            if (cols != null && !cols.isBlank()) {
                visibleColumns = new ArrayList<>(Arrays.stream(cols.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList());
            }
        } catch (IOException e) {
            System.err.println("Failed to read config file: " + e.getMessage());
        }
    }

    public void save() {
        ensureDirectories();
        properties.setProperty("dataFolder", dataFolder.toAbsolutePath().toString());
        properties.setProperty("startWithWindows", String.valueOf(startWithWindows));
        properties.setProperty("minimizeToTray", String.valueOf(minimizeToTray));
        properties.setProperty("soundChime", String.valueOf(soundChime));
        properties.setProperty("startupNotification", String.valueOf(startupNotification));
        properties.setProperty("persistentReminderMinutes", String.valueOf(persistentReminderMinutes));
        properties.setProperty("notifyToday", String.valueOf(notifyToday));
        properties.setProperty("notifyThisWeek", String.valueOf(notifyThisWeek));
        properties.setProperty("notifyOverdue", String.valueOf(notifyOverdue));
        properties.setProperty("defaultPerson", defaultPerson);
        properties.setProperty("theme", theme);
        properties.setProperty("visibleColumns", String.join(",", visibleColumns));

        try (OutputStream out = Files.newOutputStream(configFile)) {
            properties.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "DueDate Application Configuration");
        } catch (IOException e) {
            System.err.println("Failed to write config file: " + e.getMessage());
        }
    }

    // Getters and Setters
    public Path getConfigDirectory() {
        return configDirectory;
    }

    public Path getDataFolder() {
        return dataFolder;
    }

    public void setDataFolder(Path dataFolder) {
        this.dataFolder = dataFolder;
    }

    public boolean isStartWithWindows() {
        return startWithWindows;
    }

    public void setStartWithWindows(boolean startWithWindows) {
        this.startWithWindows = startWithWindows;
    }

    public boolean isMinimizeToTray() {
        return minimizeToTray;
    }

    public void setMinimizeToTray(boolean minimizeToTray) {
        this.minimizeToTray = minimizeToTray;
    }

    public boolean isSoundChime() {
        return soundChime;
    }

    public void setSoundChime(boolean soundChime) {
        this.soundChime = soundChime;
    }

    public boolean isStartupNotification() {
        return startupNotification;
    }

    public void setStartupNotification(boolean startupNotification) {
        this.startupNotification = startupNotification;
    }

    public int getPersistentReminderMinutes() {
        return persistentReminderMinutes;
    }

    public void setPersistentReminderMinutes(int persistentReminderMinutes) {
        this.persistentReminderMinutes = persistentReminderMinutes;
    }

    public boolean isNotifyToday() {
        return notifyToday;
    }

    public void setNotifyToday(boolean notifyToday) {
        this.notifyToday = notifyToday;
    }

    public boolean isNotifyThisWeek() {
        return notifyThisWeek;
    }

    public void setNotifyThisWeek(boolean notifyThisWeek) {
        this.notifyThisWeek = notifyThisWeek;
    }

    public boolean isNotifyOverdue() {
        return notifyOverdue;
    }

    public void setNotifyOverdue(boolean notifyOverdue) {
        this.notifyOverdue = notifyOverdue;
    }

    public String getDefaultPerson() {
        return defaultPerson;
    }

    public void setDefaultPerson(String defaultPerson) {
        this.defaultPerson = defaultPerson;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public List<String> getVisibleColumns() {
        return visibleColumns;
    }

    public void setVisibleColumns(List<String> visibleColumns) {
        this.visibleColumns = visibleColumns;
    }
}
