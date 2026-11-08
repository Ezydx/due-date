package com.duedate.io;

import com.duedate.config.TrackingConfig;
import com.duedate.model.DueDateItem;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

/**
 * Manages loading, saving, and collision detection for .dd files.
 */
public class DueDateStorage {

    private final Path storageDirectory;
    private final ZoneId zone;
    private final TrackingConfig trackingConfig;
    private final List<DueDateItem> items = new ArrayList<>();
    private final Map<String, Integer> titleCounts = new HashMap<>();

    public DueDateStorage(Path storageDirectory, ZoneId zone) {
        this(storageDirectory, zone, null);
    }

    public DueDateStorage(Path storageDirectory, ZoneId zone, TrackingConfig trackingConfig) {
        this.storageDirectory = storageDirectory;
        this.zone = (zone != null) ? zone : ZoneId.systemDefault();
        Path baseDir = (storageDirectory.getParent() != null) ? storageDirectory.getParent() : storageDirectory;
        this.trackingConfig = (trackingConfig != null) ? trackingConfig : new TrackingConfig(baseDir);
        ensureDirectoryExists();
    }

    public TrackingConfig getTrackingConfig() {
        return trackingConfig;
    }

    public Path getStorageDirectory() {
        return storageDirectory;
    }

    private void ensureDirectoryExists() {
        try {
            if (!Files.exists(storageDirectory)) {
                Files.createDirectories(storageDirectory);
            }
        } catch (IOException e) {
            System.err.println("Failed to create storage directory: " + e.getMessage());
        }
    }

    /**
     * Loads all .dd (and .dd.txt) files from storageDirectory, subdirectories,
     * and parent folder if placed directly in DueDates/.
     * Untracked files are skipped.
     */
    public synchronized List<DueDateItem> loadAll() {
        items.clear();
        ensureDirectoryExists();

        // Refresh tracking configurations
        trackingConfig.load();

        Set<Path> scannedPaths = new HashSet<>();

        // 1. Scan storageDirectory and subdirectories (up to depth 2)
        scanDirectory(storageDirectory, scannedPaths);

        // 2. Also check parent directory if storageDirectory is a subfolder (e.g. "deadlines" inside "DueDates")
        Path parent = storageDirectory.getParent();
        if (parent != null && Files.isDirectory(parent)) {
            try (Stream<Path> stream = Files.list(parent)) {
                stream.filter(this::isCandidateFile)
                      .forEach(p -> loadCandidatePath(p, scannedPaths));
            } catch (IOException ignored) {}
        }

        // Sort items by creation time to assign permanent intrinsic index
        items.sort(Comparator.comparing(DueDateItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setIntrinsicIndex(i + 1);
        }

        recalculateTitleCounts();
        return new ArrayList<>(items);
    }

    private void scanDirectory(Path dir, Set<Path> scannedPaths) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> stream = Files.walk(dir, 2)) {
            stream.filter(this::isCandidateFile)
                  .forEach(p -> loadCandidatePath(p, scannedPaths));
        } catch (IOException e) {
            System.err.println("Error reading storage directory: " + e.getMessage());
        }
    }

    private boolean isCandidateFile(Path p) {
        if (!Files.isRegularFile(p)) return false;
        String rawName = p.getFileName().toString();
        if (trackingConfig.isUntracked(rawName)) {
            return false;
        }
        String name = rawName.toLowerCase(Locale.ROOT);
        return name.endsWith(".dd") || name.endsWith(".dd.txt");
    }

    private void loadCandidatePath(Path path, Set<Path> scannedPaths) {
        Path norm = path.toAbsolutePath().normalize();
        if (scannedPaths.contains(norm)) return;
        scannedPaths.add(norm);

        // If it was accidentally saved as .dd.txt (common Windows Notepad issue), rename it to .dd
        String fileName = norm.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".dd.txt")) {
            Path targetDd = norm.resolveSibling(fileName.substring(0, fileName.length() - 4));
            try {
                if (!Files.exists(targetDd)) {
                    Files.move(norm, targetDd);
                    norm = targetDd;
                    fileName = targetDd.getFileName().toString();
                }
            } catch (Exception ignored) {}
        }

        loadFile(norm);
    }

    private void loadFile(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String fileName = path.getFileName().toString();
            DueDateItem item = DueDateParser.parse(content, fileName, zone);

            // Fallback for creation time from file attributes if missing
            if (item.getCreatedAt() == null) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    Instant t = attrs.creationTime().toInstant();
                    if (t.toEpochMilli() <= 0) {
                        t = attrs.lastModifiedTime().toInstant();
                    }
                    item.setCreatedAt(t);
                } catch (Exception ignored) {
                    item.setCreatedAt(Instant.now());
                }
            }

            items.add(item);
        } catch (Exception e) {
            System.err.println("Error parsing file " + path + ": " + e.getMessage());
        }
    }

    /**
     * Recalculates frequency of titles to detect collisions.
     */
    public synchronized void recalculateTitleCounts() {
        titleCounts.clear();
        for (DueDateItem item : items) {
            String titleKey = normalizeTitle(item.getTitle());
            titleCounts.put(titleKey, titleCounts.getOrDefault(titleKey, 0) + 1);
        }
    }

    private String normalizeTitle(String title) {
        return (title == null ? "" : title.trim().toLowerCase());
    }

    /**
     * Returns whether a title has a collision with another item.
     */
    public synchronized boolean hasTitleCollision(String title) {
        if (title == null) return false;
        return titleCounts.getOrDefault(normalizeTitle(title), 0) > 1;
    }

    /**
     * Resolves the display title for an item:
     * If there's a collision on title, returns e.g. "deadline44(meating1)"
     * Otherwise returns the plain title.
     */
    public synchronized String getDisplayTitle(DueDateItem item) {
        if (item == null) return "";
        String title = item.getTitle();
        if (title == null || title.isEmpty()) {
            title = "Untitled";
        }

        if (hasTitleCollision(title)) {
            String fn = item.getFileName();
            if (fn != null && fn.toLowerCase().endsWith(".dd")) {
                fn = fn.substring(0, fn.length() - 3);
            }
            if (fn != null && !fn.isEmpty()) {
                return title + "(" + fn + ")";
            }
        }
        return title;
    }

    /**
     * Saves a DueDateItem to disk. If the item's filename changed, deletes old file.
     */
    public synchronized void save(DueDateItem item, String oldFileName) throws IOException {
        ensureDirectoryExists();

        String rawFileName = item.getFileName();
        if (rawFileName == null || rawFileName.trim().isEmpty()) {
            rawFileName = sanitizeFileName(item.getTitle()) + ".dd";
        }
        if (!rawFileName.toLowerCase().endsWith(".dd")) {
            rawFileName = rawFileName + ".dd";
        }
        item.setFileName(rawFileName);

        // If renamed, delete the old file
        if (oldFileName != null && !oldFileName.trim().isEmpty() && !oldFileName.equalsIgnoreCase(rawFileName)) {
            Path oldPath = storageDirectory.resolve(oldFileName);
            Files.deleteIfExists(oldPath);
            items.removeIf(it -> oldFileName.equalsIgnoreCase(it.getFileName()));
        }

        Path targetPath = storageDirectory.resolve(rawFileName);
        String serialized = DueDateParser.serialize(item, zone);
        Files.writeString(targetPath, serialized, StandardCharsets.UTF_8);

        // Update in-memory list
        final String finalFileName = rawFileName;
        items.removeIf(it -> finalFileName.equalsIgnoreCase(it.getFileName()));
        items.add(item);
        recalculateTitleCounts();
    }

    /**
     * Deletes a DueDateItem from disk and memory.
     */
    public synchronized boolean delete(DueDateItem item) {
        if (item == null || item.getFileName() == null) return false;
        Path targetPath = storageDirectory.resolve(item.getFileName());
        try {
            boolean deleted = Files.deleteIfExists(targetPath);
            items.remove(item);
            recalculateTitleCounts();
            return deleted;
        } catch (IOException e) {
            System.err.println("Failed to delete " + targetPath + ": " + e.getMessage());
            return false;
        }
    }

    public synchronized List<DueDateItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public static String sanitizeFileName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "deadline_" + System.currentTimeMillis();
        }
        String clean = input.trim().replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        if (clean.isEmpty() || clean.equals("_")) {
            return "deadline_" + System.currentTimeMillis();
        }
        return clean;
    }
}
