package com.duedate.model;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Represents the proximity status of a deadline.
 * Color coding rules:
 * - BLACK: Already ended
 * - RED: Due today
 * - ORANGE: Due this week (within 7 days)
 * - GREEN: Longer than a week and up to 1 month
 * - BLUE: Longer than one month
 */
public enum DeadlineStatus {
    OVERDUE("Ended", new Color(40, 40, 45), new Color(200, 200, 210)),
    TODAY("Today", new Color(229, 57, 53), Color.WHITE),
    THIS_WEEK("This Week", new Color(251, 140, 0), Color.WHITE),
    THIS_MONTH("This Month", new Color(67, 160, 71), Color.WHITE),
    FUTURE("Far Future (>1M)", new Color(30, 136, 229), Color.WHITE);

    private final String label;
    private final Color badgeColor;
    private final Color textColor;

    DeadlineStatus(String label, Color badgeColor, Color textColor) {
        this.label = label;
        this.badgeColor = badgeColor;
        this.textColor = textColor;
    }

    public String getLabel() {
        return label;
    }

    public Color getBadgeColor() {
        return badgeColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    /**
     * Determines status given a deadline and current instant.
     */
    public static DeadlineStatus from(Instant deadline, Instant now, ZoneId zone) {
        if (deadline == null) {
            return FUTURE;
        }
        if (deadline.isBefore(now)) {
            return OVERDUE;
        }

        LocalDate today = LocalDate.ofInstant(now, zone);
        LocalDate deadlineDate = LocalDate.ofInstant(deadline, zone);

        if (deadlineDate.equals(today)) {
            return TODAY;
        }

        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(today, deadlineDate);
        if (daysDiff <= 7) {
            return THIS_WEEK;
        } else if (daysDiff <= 31) {
            return THIS_MONTH;
        } else {
            return FUTURE;
        }
    }
}
