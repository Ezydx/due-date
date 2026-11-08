package com.duedate.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Represents a single deadline entry (.dd object).
 */
public class DueDateItem {
    private String fileName;        // e.g. "meeting.dd"
    private String title;           // limited or flexible text
    private Instant since;          // start date/time
    private Instant deadline;       // due date/time
    private String person;          // person tag (default "Guest")
    private String description;     // notes / description
    private Instant createdAt;      // creation timestamp metadata
    private int intrinsicIndex = 0; // permanent creation order index

    public DueDateItem() {
        this.title = "";
        this.since = Instant.now();
        this.deadline = Instant.now().plus(Duration.ofDays(1));
        this.person = "Guest";
        this.description = "";
        this.fileName = "";
        this.createdAt = Instant.now();
    }

    public DueDateItem(String title, Instant since, Instant deadline, String person, String description, String fileName) {
        this(title, since, deadline, person, description, fileName, Instant.now());
    }

    public DueDateItem(String title, Instant since, Instant deadline, String person, String description, String fileName, Instant createdAt) {
        this.title = title != null ? title.trim() : "";
        this.since = since != null ? since : Instant.now();
        this.deadline = deadline != null ? deadline : Instant.now();
        this.person = (person != null && !person.trim().isEmpty()) ? person.trim() : "Guest";
        this.description = description != null ? description : "";
        this.fileName = fileName != null ? fileName.trim() : "";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public DueDateItem copy() {
        DueDateItem cp = new DueDateItem(title, since, deadline, person, description, fileName, createdAt);
        cp.setIntrinsicIndex(this.intrinsicIndex);
        return cp;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getSince() {
        return since;
    }

    public void setSince(Instant since) {
        this.since = since;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public String getPerson() {
        return (person == null || person.trim().isEmpty()) ? "Guest" : person;
    }

    public void setPerson(String person) {
        this.person = (person == null || person.trim().isEmpty()) ? "Guest" : person.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public int getIntrinsicIndex() {
        return intrinsicIndex;
    }

    public void setIntrinsicIndex(int intrinsicIndex) {
        this.intrinsicIndex = intrinsicIndex;
    }

    public DeadlineStatus getStatus(Instant now, ZoneId zone) {
        return DeadlineStatus.from(deadline, now, zone);
    }

    public String getSummaryDescription(int maxLength) {
        if (description == null || description.isEmpty()) return "";
        String firstLine = description.lines().findFirst().orElse("");
        if (firstLine.length() > maxLength) {
            return firstLine.substring(0, maxLength) + "...";
        }
        return firstLine;
    }

    public String getRemainingFormatted(Instant now) {
        if (deadline == null) return "No deadline";
        Duration duration = Duration.between(now, deadline);
        boolean isPast = duration.isNegative();
        duration = duration.abs();

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        StringBuilder sb = new StringBuilder();
        if (isPast) {
            sb.append("Overdue by ");
        } else {
            sb.append("Due in ");
        }

        if (days > 0) {
            sb.append(days).append("d ");
            if (hours > 0) sb.append(hours).append("h");
        } else if (hours > 0) {
            sb.append(hours).append("h ").append(minutes).append("m");
        } else {
            sb.append(minutes).append("m");
        }

        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DueDateItem that = (DueDateItem) o;
        return Objects.equals(fileName, that.fileName) &&
               Objects.equals(title, that.title) &&
               Objects.equals(since, that.since) &&
               Objects.equals(deadline, that.deadline);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, title, since, deadline);
    }

    @Override
    public String toString() {
        return "DueDateItem{" +
                "title='" + title + '\'' +
                ", fileName='" + fileName + '\'' +
                ", person='" + person + '\'' +
                ", since=" + since +
                ", deadline=" + deadline +
                '}';
    }
}
