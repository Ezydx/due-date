package com.duedate.test;

import com.duedate.io.DueDateParser;
import com.duedate.io.DueDateStorage;
import com.duedate.model.DeadlineStatus;
import com.duedate.model.DueDateItem;
import com.duedate.model.RelativeOffset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Automated test suite for DueDate functionality.
 */
public class DueDateTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("=== Starting DueDate Test Suite ===");

        testParserRoundTrip();
        testParserOutOfOrder();
        testUserSnippet();
        testCollisionDetection();
        testRelativeOffsetCalculation();
        testDeadlineStatusProximity();
        testStorageSaveAndLoad();

        System.out.println("===================================");
        System.out.println("Tests Run: " + testsRun + ", Passed: " + testsPassed);
        if (testsRun == testsPassed) {
            System.out.println("ALL TESTS PASSED SUCCESSFULLY! ✓");
        } else {
            System.err.println("SOME TESTS FAILED! ✗");
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName);
        }
    }

    private static void assertEquals(String testName, Object expected, Object actual) {
        testsRun++;
        if (expected == null && actual == null || (expected != null && expected.equals(actual))) {
            testsPassed++;
            System.out.println("  [PASS] " + testName);
        } else {
            System.err.println("  [FAIL] " + testName + " - Expected: <" + expected + ">, Got: <" + actual + ">");
        }
    }

    private static void testParserRoundTrip() {
        System.out.println("\nTesting DueDateParser Round Trip:");
        ZoneId zone = ZoneId.of("UTC");
        Instant since = Instant.parse("2026-11-01T09:30:00Z");
        Instant deadline = Instant.parse("2026-11-15T17:45:00Z");

        DueDateItem item = new DueDateItem("Team Sync", since, deadline, "larckh", "Discuss Q4 objectives |?| carefully", "team_sync.dd");

        String serialized = DueDateParser.serialize(item, zone);
        System.out.println("Serialized format:\n" + serialized);

        assertTrue("Serialized contains delimiter |?|", serialized.contains("|?|"));
        assertTrue("Serialized contains [title]", serialized.contains("[title]"));
        assertTrue("Serialized contains [since]", serialized.contains("[since]"));
        assertTrue("Serialized contains [deadline]", serialized.contains("[deadline]"));
        assertTrue("Serialized contains [person]", serialized.contains("[person]"));
        assertTrue("Serialized contains [description]", serialized.contains("[description]"));

        DueDateItem parsed = DueDateParser.parse(serialized, "team_sync.dd", zone);
        assertEquals("Parsed title matches", "Team Sync", parsed.getTitle());
        assertEquals("Parsed person matches", "larckh", parsed.getPerson());
        assertEquals("Parsed description matches with delimiter escape", "Discuss Q4 objectives |?| carefully", parsed.getDescription());
        assertEquals("Parsed since matches", since, parsed.getSince());
        assertEquals("Parsed deadline matches", deadline, parsed.getDeadline());
    }

    private static void testParserOutOfOrder() {
        System.out.println("\nTesting Parser With Out-of-Order Fields & Null Person:");
        ZoneId zone = ZoneId.of("UTC");

        String rawContent = """
                [description] "Check grocery items" |?|
                [deadline] 2026/11/10 - 06:00:pm |?|
                [title] "Groceries" |?|
                [since] 2026/11/08 - 10:00:am |?|
                """;

        DueDateItem parsed = DueDateParser.parse(rawContent, "groceries.dd", zone);
        assertEquals("Title parsed out of order", "Groceries", parsed.getTitle());
        assertEquals("Default person is Guest when missing", "Guest", parsed.getPerson());
        assertEquals("Description parsed out of order", "Check grocery items", parsed.getDescription());
        assertTrue("Deadline parsed correctly", parsed.getDeadline() != null);
        assertTrue("Since parsed correctly", parsed.getSince() != null);
    }

    private static void testUserSnippet() {
        System.out.println("\nTesting User Manual Snippet:");
        ZoneId zone = ZoneId.of("UTC");
        String input = """
[title] "test oh!" |?|
[since]   2026/11/08 - 03:00:am |?|
[deadline] 2026/11/08 -  05:01:am |?|
[person] "
zed-dead"
|?|

[description]"
 lol
>
 something is testing wahhaha ." |?|
""";
        DueDateItem parsed = DueDateParser.parse(input, "manual_test.dd", zone);
        assertEquals("Title parsed", "test oh!", parsed.getTitle());
        assertEquals("Person parsed with newline inside quotes", "zed-dead", parsed.getPerson());
        assertEquals("Since parsed", Instant.parse("2026-11-08T03:00:00Z"), parsed.getSince());
        assertEquals("Deadline parsed with double spaces", Instant.parse("2026-11-08T05:01:00Z"), parsed.getDeadline());
    }

    private static void testCollisionDetection() {
        System.out.println("\nTesting Title Collision Detection:");
        ZoneId zone = ZoneId.of("UTC");
        try {
            Path tempDir = Files.createTempDirectory("duedate_collision_test");
            DueDateStorage storage = new DueDateStorage(tempDir, zone);

            DueDateItem item1 = new DueDateItem("deadline44", Instant.now(), Instant.now().plus(Duration.ofDays(1)), "Guest", "Task 1", "meating1.dd");
            DueDateItem item2 = new DueDateItem("deadline44", Instant.now(), Instant.now().plus(Duration.ofDays(2)), "Guest", "Task 2", "subscription.dd");
            DueDateItem item3 = new DueDateItem("UniqueTask", Instant.now(), Instant.now().plus(Duration.ofDays(3)), "Guest", "Task 3", "unique.dd");

            storage.save(item1, null);
            storage.save(item2, null);
            storage.save(item3, null);

            storage.loadAll();

            assertTrue("Has collision for deadline44", storage.hasTitleCollision("deadline44"));
            assertTrue("No collision for UniqueTask", !storage.hasTitleCollision("UniqueTask"));

            assertEquals("Display title for item1 includes filename", "deadline44(meating1)", storage.getDisplayTitle(item1));
            assertEquals("Display title for item2 includes filename", "deadline44(subscription)", storage.getDisplayTitle(item2));
            assertEquals("Display title for item3 has no extra filename", "UniqueTask", storage.getDisplayTitle(item3));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void testRelativeOffsetCalculation() {
        System.out.println("\nTesting Relative Offset Calculations:");
        ZoneId zone = ZoneId.of("UTC");
        Instant now = Instant.parse("2026-11-08T12:00:00Z");
        Instant since = Instant.parse("2026-11-01T12:00:00Z");

        // Base Now + 2 years and 6 months
        RelativeOffset offset1 = new RelativeOffset(RelativeOffset.BaseOption.NOW, 2, 6, 0, 0, 0);
        Instant resolved1 = offset1.resolve(now, since, zone);
        assertEquals("Now + 2y 6m matches", Instant.parse("2029-05-08T12:00:00Z"), resolved1);

        // Base Since + 10 days
        RelativeOffset offset2 = new RelativeOffset(RelativeOffset.BaseOption.SINCE, 0, 0, 10, 0, 0);
        Instant resolved2 = offset2.resolve(now, since, zone);
        assertEquals("Since + 10 days matches", Instant.parse("2026-11-11T12:00:00Z"), resolved2);
    }

    private static void testDeadlineStatusProximity() {
        System.out.println("\nTesting Deadline Status Proximity Colors:");
        ZoneId zone = ZoneId.of("UTC");
        Instant now = Instant.parse("2026-11-08T12:00:00Z");

        // Past: Overdue (Black)
        Instant past = Instant.parse("2026-11-07T12:00:00Z");
        assertEquals("Past is OVERDUE", DeadlineStatus.OVERDUE, DeadlineStatus.from(past, now, zone));

        // Same day: Today (Red)
        Instant todayLater = Instant.parse("2026-11-08T20:00:00Z");
        assertEquals("Later today is TODAY", DeadlineStatus.TODAY, DeadlineStatus.from(todayLater, now, zone));

        // Within 7 days: This Week (Orange)
        Instant fourDays = Instant.parse("2026-11-12T12:00:00Z");
        assertEquals("4 days ahead is THIS_WEEK", DeadlineStatus.THIS_WEEK, DeadlineStatus.from(fourDays, now, zone));

        // 20 days ahead: This Month (Green)
        Instant twentyDays = Instant.parse("2026-11-28T12:00:00Z");
        assertEquals("20 days ahead is THIS_MONTH", DeadlineStatus.THIS_MONTH, DeadlineStatus.from(twentyDays, now, zone));

        // 60 days ahead: Future (Blue)
        Instant sixtyDays = Instant.parse("2027-01-10T12:00:00Z");
        assertEquals("60 days ahead is FUTURE", DeadlineStatus.FUTURE, DeadlineStatus.from(sixtyDays, now, zone));
    }

    private static void testStorageSaveAndLoad() {
        System.out.println("\nTesting Storage Save and Load:");
        ZoneId zone = ZoneId.of("UTC");
        try {
            Path tempDir = Files.createTempDirectory("duedate_storage_test");
            DueDateStorage storage = new DueDateStorage(tempDir, zone);

            DueDateItem item = new DueDateItem("Budget 2027", Instant.now(), Instant.now().plus(Duration.ofDays(30)), "Alice", "Review company budget", "budget.dd");
            storage.save(item, null);

            List<DueDateItem> loaded = storage.loadAll();
            assertEquals("One item loaded", 1, loaded.size());
            assertEquals("Title matches", "Budget 2027", loaded.get(0).getTitle());
            assertEquals("Filename matches", "budget.dd", loaded.get(0).getFileName());

            // Delete item
            storage.delete(loaded.get(0));
            List<DueDateItem> afterDelete = storage.loadAll();
            assertEquals("Zero items after delete", 0, afterDelete.size());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
