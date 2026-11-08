package com.duedate.service;

import com.duedate.config.AppConfig;
import com.duedate.io.DueDateStorage;
import com.duedate.model.DeadlineStatus;
import com.duedate.model.DueDateItem;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Background notification daemon that checks deadlines, rings bell chimes,
 * and displays system tray alerts / persistent reminders.
 */
public class NotificationService {

    private final DueDateStorage storage;
    private final AppConfig config;
    private final ZoneId zone;
    private TrayIcon trayIcon;
    private Consumer<String> toastCallback;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "DueDate-NotificationDaemon");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, Instant> lastRemindedMap = new ConcurrentHashMap<>();

    public NotificationService(DueDateStorage storage, AppConfig config, ZoneId zone) {
        this.storage = storage;
        this.config = config;
        this.zone = (zone != null) ? zone : ZoneId.systemDefault();
    }

    public void setTrayIcon(TrayIcon trayIcon) {
        this.trayIcon = trayIcon;
    }

    public void setToastCallback(Consumer<String> toastCallback) {
        this.toastCallback = toastCallback;
    }

    /**
     * Starts the periodic reminder checks.
     */
    public void start() {
        // Run initial startup check after a brief 2-second warm-up
        scheduler.schedule(this::checkStartupNotifications, 2, TimeUnit.SECONDS);

        // Run persistent loop every 1 minute
        scheduler.scheduleAtFixedRate(this::checkPeriodicReminders, 1, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /**
     * Checks notifications on application startup.
     */
    public void checkStartupNotifications() {
        if (!config.isStartupNotification()) return;

        Instant now = Instant.now();
        List<DueDateItem> items = storage.loadAll();

        List<DueDateItem> overdueItems = new ArrayList<>();
        List<DueDateItem> todayItems = new ArrayList<>();
        List<DueDateItem> thisWeekItems = new ArrayList<>();

        for (DueDateItem item : items) {
            if (storage.getTrackingConfig() != null && storage.getTrackingConfig().isMuted(item.getFileName())) {
                continue; // Muted items do not trigger alerts
            }
            DeadlineStatus status = item.getStatus(now, zone);
            if (status == DeadlineStatus.OVERDUE && config.isNotifyOverdue()) {
                overdueItems.add(item);
            } else if (status == DeadlineStatus.TODAY && config.isNotifyToday()) {
                todayItems.add(item);
            } else if (status == DeadlineStatus.THIS_WEEK && config.isNotifyThisWeek()) {
                thisWeekItems.add(item);
            }
        }

        if (!overdueItems.isEmpty() || !todayItems.isEmpty() || !thisWeekItems.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (!todayItems.isEmpty()) {
                sb.append(todayItems.size()).append(" due today! ");
            }
            if (!overdueItems.isEmpty()) {
                sb.append(overdueItems.size()).append(" overdue. ");
            }
            if (!thisWeekItems.isEmpty()) {
                sb.append(thisWeekItems.size()).append(" due this week.");
            }

            String summary = sb.toString().trim();
            notifyUser("DueDate Startup Summary", summary, TrayIcon.MessageType.INFO);

            // Mark today items as reminded so we don't double notify immediately
            for (DueDateItem item : todayItems) {
                lastRemindedMap.put(item.getFileName(), now);
            }
        }
    }

    /**
     * Checks persistent hourly (or configured interval) reminders for deadlines.
     */
    public void checkPeriodicReminders() {
        Instant now = Instant.now();
        int intervalMinutes = Math.max(5, config.getPersistentReminderMinutes());
        List<DueDateItem> items = storage.getItems();

        List<DueDateItem> urgentItems = new ArrayList<>();

        for (DueDateItem item : items) {
            if (storage.getTrackingConfig() != null && storage.getTrackingConfig().isMuted(item.getFileName())) {
                continue; // Muted items do not trigger periodic alerts
            }
            DeadlineStatus status = item.getStatus(now, zone);
            boolean isEligible = false;

            if (status == DeadlineStatus.TODAY && config.isNotifyToday()) {
                isEligible = true;
            } else if (status == DeadlineStatus.OVERDUE && config.isNotifyOverdue()) {
                isEligible = true;
            }

            if (isEligible) {
                Instant last = lastRemindedMap.get(item.getFileName());
                if (last == null || Duration.between(last, now).toMinutes() >= intervalMinutes) {
                    urgentItems.add(item);
                    lastRemindedMap.put(item.getFileName(), now);
                }
            }
        }

        if (!urgentItems.isEmpty()) {
            DueDateItem first = urgentItems.get(0);
            String title = storage.getDisplayTitle(first);
            String timing = formatClampedDuration(first.getDeadline(), now);
            String msg = (urgentItems.size() == 1)
                    ? title + " (" + timing + ")"
                    : title + " and " + (urgentItems.size() - 1) + " other deadline(s) require attention!";

            notifyUser("Deadline Reminder", msg, TrayIcon.MessageType.WARNING);
        }
    }

    /**
     * Clamps remaining duration into clean, natural user-facing text
     * (e.g. "due in ~1 hour" or "overdue by 15m" instead of odd numbers like "1 hour and 2 minutes").
     */
    private String formatClampedDuration(Instant target, Instant now) {
        if (target == null) return "no deadline";
        Duration d = Duration.between(now, target);
        boolean isPast = d.isNegative();
        d = d.abs();
        long totalMins = d.toMinutes();

        if (isPast) {
            if (totalMins < 60) {
                return "overdue by " + totalMins + "m";
            }
            long hours = (totalMins + 5) / 60;
            return "overdue by ~" + hours + (hours == 1 ? " hour" : " hours");
        } else {
            if (totalMins < 60) {
                return "due in " + totalMins + "m";
            }
            long hours = totalMins / 60;
            long remMins = totalMins % 60;
            if (remMins <= 5) {
                return "due in " + hours + (hours == 1 ? " hour" : " hours");
            } else if (remMins >= 55) {
                return "due in " + (hours + 1) + " hours";
            } else {
                long clampedMins = Math.round(remMins / 5.0) * 5;
                return "due in " + hours + "h " + clampedMins + "m";
            }
        }
    }

    public void notifyUser(String title, String message, TrayIcon.MessageType type) {
        if (config.isSoundChime()) {
            SoundService.ringBell();
        }

        if (trayIcon != null) {
            try {
                trayIcon.displayMessage(title, message, type);
            } catch (Exception e) {
                System.err.println("TrayIcon displayMessage failed: " + e.getMessage());
            }
        }

        if (toastCallback != null) {
            toastCallback.accept(title + ": " + message);
        }
    }
}
