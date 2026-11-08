package com.duedate;

import com.duedate.config.AppConfig;
import com.duedate.io.DueDateStorage;
import com.duedate.service.NotificationService;
import com.duedate.ui.MainFrame;
import com.duedate.ui.Theme;

import javax.swing.*;
import java.io.File;
import java.net.URISyntaxException;
import java.time.ZoneId;

/**
 * DueDate Application Entry Point.
 */
public class Main {

    public static final String APP_VERSION = "1.0.0";

    public static void main(String[] args) {
        boolean startInBackground = false;

        for (String arg : args) {
            if ("--background".equalsIgnoreCase(arg) || "-b".equalsIgnoreCase(arg)) {
                startInBackground = true;
            }
        }

        // Configure High DPI scaling and system rendering properties
        System.setProperty("sun.java2d.uiScale.enabled", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        try {
            // Apply native System Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        final boolean backgroundMode = startInBackground;

        SwingUtilities.invokeLater(() -> {
            try {
                ZoneId zone = ZoneId.systemDefault();
                AppConfig config = new AppConfig();
                DueDateStorage storage = new DueDateStorage(config.getDataFolder(), zone);
                NotificationService notificationService = new NotificationService(storage, config, zone);

                File jarFile = resolveRunningJar();

                MainFrame frame = new MainFrame(config, storage, notificationService, jarFile, zone);

                // Start background notifications
                notificationService.start();

                if (!backgroundMode) {
                    frame.setVisible(true);
                } else {
                    System.out.println("DueDate started in background mode (System Tray active).");
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error starting DueDate: " + e.getMessage(), "Fatal Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static File resolveRunningJar() {
        try {
            File file = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (file.getName().endsWith(".jar")) {
                return file;
            }
        } catch (URISyntaxException ignored) {}
        // Fallback to local DueDate.jar if running in directory
        File local = new File("DueDate.jar");
        return local.exists() ? local : null;
    }
}
