package com.duedate.io;

import com.duedate.model.DueDateItem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and serializes .dd files with [tag] value |?| format.
 * Format is human-readable and order-independent.
 *
 * Example:
 * [title] "Project Alpha" |?|
 * [since] 2026/11/08 - 09:00:am |?|
 * [deadline] 2026/11/15 - 05:00:pm |?|
 * [person] "Guest" |?|
 * [description] "Complete milestones 1 and 2" |?|
 */
public class DueDateParser {

    public static final String DELIMITER = "|?|";
    private static final String ESCAPED_DELIMITER_TOKEN = "__DD_DELIMITER_ESC__";

    // Formatter used for writing: yyyy/MM/dd - hh:mm:a
    private static final DateTimeFormatter WRITE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy/MM/dd - hh:mm:a")
            .toFormatter(Locale.ENGLISH);

    // Primary parser: accepts 1 or 2 digit hour, case-insensitive
    private static final DateTimeFormatter PRIMARY_PARSER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy/MM/dd - ")
            .appendValue(java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
            .appendPattern(":mm:a")
            .toFormatter(Locale.ENGLISH);

    // Fallback formatters
    private static final DateTimeFormatter[] FALLBACK_FORMATTERS = new DateTimeFormatter[] {
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd - ").appendValue(java.time.temporal.ChronoField.CLOCK_HOUR_OF_AMPM, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE).appendPattern(":mm a").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd - ").appendValue(java.time.temporal.ChronoField.HOUR_OF_DAY, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE).appendPattern(":mm").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd ").appendValue(java.time.temporal.ChronoField.HOUR_OF_DAY, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE).appendPattern(":mm").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy/MM/dd").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("yyyy-MM-dd").toFormatter(Locale.ENGLISH)
    };

    private static final Pattern TAG_PATTERN = Pattern.compile("^\\[([a-zA-Z0-9_-]+)\\]\\s*(.*)$", Pattern.DOTALL);

    /**
     * Formats an Instant into human-readable "yyyy/MM/dd - hh:mm:am/pm" string.
     */
    public static String formatDate(Instant instant, ZoneId zone) {
        if (instant == null) return "";
        ZonedDateTime zdt = instant.atZone(zone != null ? zone : ZoneId.systemDefault());
        return WRITE_FORMATTER.format(zdt).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parses a date string into an Instant.
     */
    public static Instant parseDate(String text, ZoneId zone) {
        if (text == null || text.trim().isEmpty()) {
            return Instant.now();
        }
        // Normalize whitespace, hyphens, and AM/PM format (e.g. " -  05:01:am" or "05:01 am")
        text = text.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*-\\s*", " - ")
                .replaceAll("(?i)(\\d{1,2}:\\d{2}):?\\s*(am|pm)", "$1:$2");

        ZoneId activeZone = zone != null ? zone : ZoneId.systemDefault();

        // 1. Try primary format
        try {
            LocalDateTime ldt = LocalDateTime.parse(text, PRIMARY_PARSER);
            return ldt.atZone(activeZone).toInstant();
        } catch (DateTimeParseException ignored) {}

        // 2. Try fallbacks
        for (DateTimeFormatter dtf : FALLBACK_FORMATTERS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(text, dtf);
                return ldt.atZone(activeZone).toInstant();
            } catch (DateTimeParseException ignored) {}
            try {
                java.time.LocalDate ld = java.time.LocalDate.parse(text, dtf);
                return ld.atStartOfDay(activeZone).toInstant();
            } catch (DateTimeParseException ignored) {}
        }

        // 3. Try standard ISO instant
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {}

        System.err.println("Warning: Unable to parse date: '" + text + "', defaulting to now.");
        return Instant.now();
    }

    /**
     * Serializes a DueDateItem into .dd file format string.
     */
    public static String serialize(DueDateItem item, ZoneId zone) {
        ZoneId activeZone = zone != null ? zone : ZoneId.systemDefault();
        StringBuilder sb = new StringBuilder();

        // Title
        sb.append("[title] \"").append(escapeContent(item.getTitle())).append("\" ").append(DELIMITER).append("\n");

        // Since
        String sinceStr = formatDate(item.getSince(), activeZone);
        sb.append("[since] ").append(sinceStr).append(" ").append(DELIMITER).append("\n");

        // Deadline
        String deadlineStr = formatDate(item.getDeadline(), activeZone);
        sb.append("[deadline] ").append(deadlineStr).append(" ").append(DELIMITER).append("\n");

        // Person
        String person = item.getPerson();
        if (person == null || person.trim().isEmpty()) {
            person = "Guest";
        }
        sb.append("[person] \"").append(escapeContent(person)).append("\" ").append(DELIMITER).append("\n");

        // Description
        String desc = item.getDescription() != null ? item.getDescription() : "";
        sb.append("[description] \"").append(escapeContent(desc)).append("\" ").append(DELIMITER).append("\n");

        return sb.toString();
    }

    /**
     * Parses a .dd file content string into a DueDateItem.
     */
    public static DueDateItem parse(String content, String fileName, ZoneId zone) {
        DueDateItem item = new DueDateItem();
        item.setFileName(fileName);

        if (content == null || content.trim().isEmpty()) {
            return item;
        }

        // Handle escape before splitting
        String safeContent = content.replace("\\|?\\|", ESCAPED_DELIMITER_TOKEN);
        String[] parts = safeContent.split(Pattern.quote(DELIMITER));

        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) continue;

            // Restore escaped delimiter
            token = token.replace(ESCAPED_DELIMITER_TOKEN, DELIMITER);

            Matcher matcher = TAG_PATTERN.matcher(token);
            if (matcher.find()) {
                String tag = matcher.group(1).toLowerCase(Locale.ROOT);
                String rawValue = matcher.group(2).trim();
                String value = unquote(rawValue);

                switch (tag) {
                    case "title" -> item.setTitle(value);
                    case "since" -> item.setSince(parseDate(value, zone));
                    case "deadline" -> item.setDeadline(parseDate(value, zone));
                    case "person", "user" -> item.setPerson(value.isEmpty() ? "Guest" : value);
                    case "description" -> item.setDescription(value);
                    default -> {
                        // Forward compatibility: ignore unknown tags
                    }
                }
            }
        }

        // Fallback for title if empty
        if (item.getTitle() == null || item.getTitle().isEmpty()) {
            if (fileName != null && fileName.endsWith(".dd")) {
                item.setTitle(fileName.substring(0, fileName.length() - 3));
            } else if (fileName != null) {
                item.setTitle(fileName);
            }
        }

        return item;
    }

    private static String escapeContent(String str) {
        if (str == null) return "";
        return str.replace(DELIMITER, "\\|?\\|");
    }

    private static String unquote(String str) {
        if (str == null) return "";
        str = str.trim();
        if (str.length() >= 2 && str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1).trim();
        }
        return str;
    }
}
