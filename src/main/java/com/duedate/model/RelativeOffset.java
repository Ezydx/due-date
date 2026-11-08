package com.duedate.model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Handles relative deadline calculation based on [Now, Since, Customize]
 * with additions of years, months, days, hours, and minutes.
 */
public class RelativeOffset {

    public enum BaseOption {
        NOW("Now"),
        SINCE("Since (Start Date)"),
        CUSTOMIZE("Custom Base Date");

        private final String displayName;

        BaseOption(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private BaseOption baseOption = BaseOption.NOW;
    private Instant customBase = null;
    private int years = 0;
    private int months = 0;
    private int days = 0;
    private int hours = 0;
    private int minutes = 0;

    public RelativeOffset() {}

    public RelativeOffset(BaseOption baseOption, int years, int months, int days, int hours, int minutes) {
        this.baseOption = baseOption;
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
    }

    public BaseOption getBaseOption() {
        return baseOption;
    }

    public void setBaseOption(BaseOption baseOption) {
        this.baseOption = baseOption;
    }

    public Instant getCustomBase() {
        return customBase;
    }

    public void setCustomBase(Instant customBase) {
        this.customBase = customBase;
    }

    public int getYears() {
        return years;
    }

    public void setYears(int years) {
        this.years = years;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    /**
     * Resolves the target deadline instant based on the configured base and offsets.
     */
    public Instant resolve(Instant now, Instant sinceDate, ZoneId zone) {
        Instant base;
        switch (baseOption) {
            case SINCE -> base = (sinceDate != null) ? sinceDate : now;
            case CUSTOMIZE -> base = (customBase != null) ? customBase : now;
            case NOW -> base = now;
            default -> base = now;
        }

        ZonedDateTime zdt = base.atZone(zone);
        if (years != 0) zdt = zdt.plusYears(years);
        if (months != 0) zdt = zdt.plusMonths(months);
        if (days != 0) zdt = zdt.plusDays(days);
        if (hours != 0) zdt = zdt.plusHours(hours);
        if (minutes != 0) zdt = zdt.plusMinutes(minutes);

        return zdt.toInstant();
    }
}
