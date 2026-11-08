package com.duedate.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages muted (turned off reminders) and untracked (ignored) .dd files.
 * Format:
 * [mute]
 * file1.dd, file4.dd
 * file9-11.dd
 * // comments starting with // or #
 * [untrack]
 * file2.dd
 * # note 1
 * file7elaven.dd , dodo.dd
 */
public class TrackingConfig {

    private final Path trackingFile;
    private final Set<String> mutedFiles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private final Set<String> untrackedFiles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public TrackingConfig(Path baseDir) {
        this.trackingFile = baseDir.resolve("tracked.conf");
        load();
    }

    public synchronized void load() {
        mutedFiles.clear();
        untrackedFiles.clear();

        if (!Files.exists(trackingFile)) {
            // Check fallback .tracked
            Path dotTracked = trackingFile.resolveSibling(".tracked");
            if (Files.exists(dotTracked)) {
                loadFile(dotTracked);
            }
            return;
        }

        loadFile(trackingFile);
    }

    private void loadFile(Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            String currentSection = "";

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("//") || trimmed.startsWith("#")) continue;

                if (trimmed.equalsIgnoreCase("[mute]")) {
                    currentSection = "MUTE";
                    continue;
                } else if (trimmed.equalsIgnoreCase("[untrack]")) {
                    currentSection = "UNTRACK";
                    continue;
                }

                // Split on commas
                String[] tokens = trimmed.split(",");
                for (String token : tokens) {
                    String fn = token.trim();
                    if (fn.isEmpty() || fn.startsWith("//") || fn.startsWith("#")) continue;
                    if (!fn.toLowerCase(Locale.ROOT).endsWith(".dd")) {
                        fn = fn + ".dd";
                    }

                    if ("MUTE".equals(currentSection)) {
                        mutedFiles.add(fn);
                    } else if ("UNTRACK".equals(currentSection)) {
                        untrackedFiles.add(fn);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read tracking file: " + e.getMessage());
        }
    }

    public synchronized void save() {
        StringBuilder sb = new StringBuilder();
        sb.append("// DueDate Tracking Configuration (.tracked / tracked.conf)\n");
        sb.append("// Lines starting with // or # are comments. Delimiters are commas and newlines.\n\n");

        sb.append("[mute]\n");
        for (String fn : mutedFiles) {
            sb.append(fn).append("\n");
        }

        sb.append("\n[untrack]\n");
        for (String fn : untrackedFiles) {
            sb.append(fn).append("\n");
        }

        try {
            if (trackingFile.getParent() != null && !Files.exists(trackingFile.getParent())) {
                Files.createDirectories(trackingFile.getParent());
            }
            Files.writeString(trackingFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save tracking file: " + e.getMessage());
        }
    }

    public synchronized boolean isMuted(String fileName) {
        if (fileName == null) return false;
        String fn = fileName.trim();
        return mutedFiles.contains(fn) || mutedFiles.contains(fn + ".dd");
    }

    public synchronized boolean isUntracked(String fileName) {
        if (fileName == null) return false;
        String fn = fileName.trim();
        return untrackedFiles.contains(fn) || untrackedFiles.contains(fn + ".dd");
    }

    public synchronized void setMuted(String fileName, boolean muted) {
        if (fileName == null || fileName.isBlank()) return;
        String fn = normalizeFileName(fileName);
        if (muted) {
            mutedFiles.add(fn);
        } else {
            mutedFiles.remove(fn);
        }
        save();
    }

    public synchronized void setUntracked(String fileName, boolean untracked) {
        if (fileName == null || fileName.isBlank()) return;
        String fn = normalizeFileName(fileName);
        if (untracked) {
            untrackedFiles.add(fn);
        } else {
            untrackedFiles.remove(fn);
        }
        save();
    }

    public synchronized Set<String> getMutedFiles() {
        return Collections.unmodifiableSet(new TreeSet<>(mutedFiles));
    }

    public synchronized Set<String> getUntrackedFiles() {
        return Collections.unmodifiableSet(new TreeSet<>(untrackedFiles));
    }

    private String normalizeFileName(String input) {
        String fn = input.trim();
        if (!fn.toLowerCase(Locale.ROOT).endsWith(".dd")) {
            fn = fn + ".dd";
        }
        return fn;
    }
}
