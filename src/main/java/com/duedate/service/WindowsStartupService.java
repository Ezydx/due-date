package com.duedate.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages Windows Startup registry entry (HKCU\Software\Microsoft\Windows\CurrentVersion\Run)
 * to allow the app to launch on system boot in the background.
 */
public class WindowsStartupService {

    private static final String REG_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final String APP_VALUE_NAME = "DueDateApp";

    /**
     * Checks if startup is currently enabled in Windows registry.
     */
    public static boolean isStartupEnabled() {
        if (!isWindows()) return false;
        try {
            Process process = new ProcessBuilder("reg", "query", REG_KEY, "/v", APP_VALUE_NAME)
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Configures the Windows registry entry to launch the JAR on startup with --background argument.
     */
    public static boolean setStartupEnabled(boolean enable, File jarFile) {
        if (!isWindows()) return false;

        if (!enable) {
            try {
                Process process = new ProcessBuilder("reg", "delete", REG_KEY, "/v", APP_VALUE_NAME, "/f")
                        .redirectErrorStream(true)
                        .start();
                process.waitFor();
                return true;
            } catch (Exception e) {
                System.err.println("Failed to remove startup registry key: " + e.getMessage());
                return false;
            }
        }

        if (jarFile == null || !jarFile.exists()) {
            System.err.println("Cannot enable startup: JAR file does not exist.");
            return false;
        }

        String javaHome = System.getProperty("java.home");
        Path javawPath = Paths.get(javaHome, "bin", "javaw.exe");
        if (!Files.exists(javawPath)) {
            javawPath = Paths.get(javaHome, "bin", "java.exe");
        }

        // Command: "C:\path\to\javaw.exe" -jar "C:\path\to\DueDate.jar" --background
        String command = "\"" + javawPath.toAbsolutePath() + "\" -jar \"" + jarFile.getAbsolutePath() + "\" --background";

        try {
            Process process = new ProcessBuilder("reg", "add", REG_KEY, "/v", APP_VALUE_NAME, "/t", "REG_SZ", "/d", command, "/f")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Failed to add startup registry key: " + e.getMessage());
            return false;
        }
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }
}
