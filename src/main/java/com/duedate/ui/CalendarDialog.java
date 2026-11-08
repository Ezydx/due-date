package com.duedate.ui;

import com.duedate.io.DueDateParser;
import com.duedate.io.DueDateStorage;
import com.duedate.model.DeadlineStatus;
import com.duedate.model.DueDateItem;
import com.duedate.ui.components.ModernButton;
import com.duedate.ui.components.ModernCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;

/**
 * Modern calendar view displaying colored proximity dots on each day:
 * - Purple dot: Start / Since date
 * - Black dot: Ended / Overdue deadline
 * - Red dot: Due today
 * - Orange dot: Due this week
 * - Green dot: Due within 1 month (> 1 week)
 * - Blue dot: Due > 1 month away
 * Clicking any day displays full deadline details in the side panel.
 */
public class CalendarDialog extends JDialog {

    private final DueDateStorage storage;
    private final ZoneId zone;

    private YearMonth currentYearMonth;
    private LocalDate selectedDate;

    private JLabel monthYearLabel;
    private JPanel calendarGrid;
    private JPanel dayDetailsPanel;
    private JLabel selectedDateTitle;
    private JPanel eventsListContainer;

    public CalendarDialog(Frame owner, DueDateStorage storage, ZoneId zone) {
        super(owner, "DueDate Calendar View", false);
        this.storage = storage;
        this.zone = (zone != null) ? zone : ZoneId.systemDefault();

        LocalDate today = LocalDate.now(zone);
        this.currentYearMonth = YearMonth.from(today);
        this.selectedDate = today;

        setSize(1000, 680);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(owner);
        getContentPane().setBackground(Theme.bg());

        buildUI();
        refreshCalendar();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(16, 12));
        root.setBackground(Theme.bg());
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Top Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout(12, 0));
        topBar.setOpaque(false);

        monthYearLabel = new JLabel();
        monthYearLabel.setFont(Theme.FONT_HEADER);
        monthYearLabel.setForeground(Theme.textPrimary());
        topBar.add(monthYearLabel, BorderLayout.WEST);

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navButtons.setOpaque(false);

        ModernButton prevBtn = new ModernButton("◀ Prev", ModernButton.ButtonStyle.SECONDARY);
        prevBtn.addActionListener(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            refreshCalendar();
        });
        navButtons.add(prevBtn);

        ModernButton todayBtn = new ModernButton("Today", ModernButton.ButtonStyle.SECONDARY);
        todayBtn.addActionListener(e -> {
            LocalDate today = LocalDate.now(zone);
            currentYearMonth = YearMonth.from(today);
            selectedDate = today;
            refreshCalendar();
        });
        navButtons.add(todayBtn);

        ModernButton nextBtn = new ModernButton("Next ▶", ModernButton.ButtonStyle.SECONDARY);
        nextBtn.addActionListener(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            refreshCalendar();
        });
        navButtons.add(nextBtn);

        topBar.add(navButtons, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // Center Split: Left = Calendar Grid, Right = Day Details
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setDividerSize(8);
        splitPane.setResizeWeight(0.68);

        // Left Panel: Calendar Grid + Legend
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setOpaque(false);

        // Day of Week Header
        JPanel weekHeader = new JPanel(new GridLayout(1, 7, 4, 0));
        weekHeader.setOpaque(false);
        String[] daysOfWeek = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : daysOfWeek) {
            JLabel lbl = new JLabel(day, SwingConstants.CENTER);
            lbl.setFont(Theme.FONT_MEDIUM);
            lbl.setForeground(Theme.textSecondary());
            weekHeader.add(lbl);
        }
        leftPanel.add(weekHeader, BorderLayout.NORTH);

        // Calendar Grid
        calendarGrid = new JPanel(new GridLayout(6, 7, 4, 4));
        calendarGrid.setOpaque(false);
        leftPanel.add(calendarGrid, BorderLayout.CENTER);

        // Legend at bottom
        JPanel legendPanel = buildLegendPanel();
        leftPanel.add(legendPanel, BorderLayout.SOUTH);

        splitPane.setLeftComponent(leftPanel);

        // Right Panel: Day Details Card
        ModernCard rightCard = new ModernCard(12);
        rightCard.setLayout(new BorderLayout(0, 10));
        rightCard.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel detailHeader = new JPanel(new BorderLayout());
        detailHeader.setOpaque(false);

        selectedDateTitle = new JLabel("Events for Selected Day");
        selectedDateTitle.setFont(Theme.FONT_LARGE);
        selectedDateTitle.setForeground(Theme.textPrimary());
        detailHeader.add(selectedDateTitle, BorderLayout.CENTER);

        rightCard.add(detailHeader, BorderLayout.NORTH);

        eventsListContainer = new JPanel();
        eventsListContainer.setLayout(new BoxLayout(eventsListContainer, BoxLayout.Y_AXIS));
        eventsListContainer.setOpaque(false);

        JScrollPane scrollEvents = new JScrollPane(eventsListContainer);
        scrollEvents.setBorder(null);
        scrollEvents.setOpaque(false);
        scrollEvents.getViewport().setOpaque(false);
        rightCard.add(scrollEvents, BorderLayout.CENTER);

        splitPane.setRightComponent(rightCard);

        root.add(splitPane, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildLegendPanel() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 6));
        legend.setOpaque(false);

        legend.add(createLegendItem(Theme.PURPLE_SINCE, "Start (Since)"));
        legend.add(createLegendItem(Theme.STATUS_OVERDUE, "Ended"));
        legend.add(createLegendItem(Theme.STATUS_TODAY, "Today"));
        legend.add(createLegendItem(Theme.STATUS_THIS_WEEK, "This Week"));
        legend.add(createLegendItem(Theme.STATUS_THIS_MONTH, "1W - 1M"));
        legend.add(createLegendItem(Theme.STATUS_FUTURE, "> 1 Month"));

        return legend;
    }

    private JPanel createLegendItem(Color dotColor, String labelText) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        item.setOpaque(false);

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyAntiAliasing(g2);
                g2.setColor(dotColor);
                g2.fillOval(1, 1, 9, 9);
                g2.setColor(Theme.border());
                g2.drawOval(1, 1, 9, 9);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));
        dot.setOpaque(false);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.textSecondary());

        item.add(dot);
        item.add(lbl);
        return item;
    }

    public void refreshCalendar() {
        monthYearLabel.setText(currentYearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentYearMonth.getYear());

        calendarGrid.removeAll();

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeekOffset = firstOfMonth.getDayOfWeek().getValue() % 7; // Sunday = 0
        int daysInMonth = currentYearMonth.lengthOfMonth();

        List<DueDateItem> allItems = storage.loadAll();
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zone);

        // Previous month filler days
        YearMonth prevYm = currentYearMonth.minusMonths(1);
        int daysInPrevMonth = prevYm.lengthOfMonth();
        for (int i = dayOfWeekOffset - 1; i >= 0; i--) {
            LocalDate d = prevYm.atDay(daysInPrevMonth - i);
            calendarGrid.add(createDayCell(d, false, allItems, now, today));
        }

        // Current month days
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate d = currentYearMonth.atDay(day);
            calendarGrid.add(createDayCell(d, true, allItems, now, today));
        }

        // Next month filler days to complete 42 cells (6 rows x 7 cols)
        int totalCellsSoFar = dayOfWeekOffset + daysInMonth;
        int nextDays = 42 - totalCellsSoFar;
        YearMonth nextYm = currentYearMonth.plusMonths(1);
        for (int day = 1; day <= nextDays; day++) {
            LocalDate d = nextYm.atDay(day);
            calendarGrid.add(createDayCell(d, false, allItems, now, today));
        }

        calendarGrid.revalidate();
        calendarGrid.repaint();

        updateDayDetails(selectedDate, allItems, now);
    }

    private JPanel createDayCell(LocalDate date, boolean isCurrentMonth, List<DueDateItem> allItems, Instant now, LocalDate today) {
        // Collect events on this day
        List<DueDateItem> sinceEvents = new ArrayList<>();
        List<DueDateItem> deadlineEvents = new ArrayList<>();

        for (DueDateItem item : allItems) {
            if (item.getSince() != null && LocalDate.ofInstant(item.getSince(), zone).equals(date)) {
                sinceEvents.add(item);
            }
            if (item.getDeadline() != null && LocalDate.ofInstant(item.getDeadline(), zone).equals(date)) {
                deadlineEvents.add(item);
            }
        }

        boolean isSelected = date.equals(selectedDate);
        boolean isToday = date.equals(today);

        JPanel cell = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyAntiAliasing(g2);

                Color bg;
                if (isSelected) {
                    bg = Theme.accent().darker();
                } else if (isToday) {
                    bg = Theme.surfaceHover();
                } else {
                    bg = isCurrentMonth ? Theme.surface() : Theme.bg();
                }

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // Border
                if (isSelected) {
                    g2.setColor(Theme.accent());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                } else if (isToday) {
                    g2.setColor(Theme.STATUS_TODAY);
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                } else {
                    g2.setColor(Theme.border());
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }

                g2.dispose();
                super.paintComponent(g);
            }
        };

        cell.setLayout(new BorderLayout(0, 2));
        cell.setOpaque(false);
        cell.setBorder(new EmptyBorder(4, 6, 4, 6));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top number
        JLabel dayNum = new JLabel(String.valueOf(date.getDayOfMonth()));
        dayNum.setFont(isToday ? Theme.FONT_MEDIUM : Theme.FONT_REGULAR);
        dayNum.setForeground(isSelected ? Color.WHITE : (isCurrentMonth ? Theme.textPrimary() : Theme.textSecondary()));
        cell.add(dayNum, BorderLayout.NORTH);

        // Dots container
        JPanel dotsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 2));
        dotsPanel.setOpaque(false);

        // Purple dots for Since events
        for (int i = 0; i < Math.min(sinceEvents.size(), 3); i++) {
            dotsPanel.add(new CircleDot(Theme.PURPLE_SINCE, 7));
        }

        // Deadline proximity colored dots
        for (int i = 0; i < Math.min(deadlineEvents.size(), 4); i++) {
            DueDateItem item = deadlineEvents.get(i);
            DeadlineStatus status = item.getStatus(now, zone);
            Color dotColor = switch (status) {
                case OVERDUE -> Theme.STATUS_OVERDUE;
                case TODAY -> Theme.STATUS_TODAY;
                case THIS_WEEK -> Theme.STATUS_THIS_WEEK;
                case THIS_MONTH -> Theme.STATUS_THIS_MONTH;
                case FUTURE -> Theme.STATUS_FUTURE;
            };
            dotsPanel.add(new CircleDot(dotColor, 7));
        }

        if (sinceEvents.size() + deadlineEvents.size() > 7) {
            JLabel more = new JLabel("+");
            more.setFont(new Font("Segoe UI", Font.BOLD, 9));
            more.setForeground(Theme.textSecondary());
            dotsPanel.add(more);
        }

        cell.add(dotsPanel, BorderLayout.CENTER);

        cell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedDate = date;
                refreshCalendar();
            }
        });

        return cell;
    }

    private void updateDayDetails(LocalDate date, List<DueDateItem> allItems, Instant now) {
        selectedDateTitle.setText("Events: " + date.toString());
        eventsListContainer.removeAll();

        List<DueDateItem> sinceEvents = new ArrayList<>();
        List<DueDateItem> deadlineEvents = new ArrayList<>();

        for (DueDateItem item : allItems) {
            if (item.getSince() != null && LocalDate.ofInstant(item.getSince(), zone).equals(date)) {
                sinceEvents.add(item);
            }
            if (item.getDeadline() != null && LocalDate.ofInstant(item.getDeadline(), zone).equals(date)) {
                deadlineEvents.add(item);
            }
        }

        if (sinceEvents.isEmpty() && deadlineEvents.isEmpty()) {
            JLabel emptyLbl = new JLabel("No deadlines or starting events on this date.");
            emptyLbl.setFont(Theme.FONT_REGULAR);
            emptyLbl.setForeground(Theme.textSecondary());
            emptyLbl.setBorder(new EmptyBorder(20, 10, 0, 10));
            eventsListContainer.add(emptyLbl);
        } else {
            // Deadline Due Events
            if (!deadlineEvents.isEmpty()) {
                JLabel sec = new JLabel("Deadlines Ending Today (" + deadlineEvents.size() + "):");
                sec.setFont(Theme.FONT_MEDIUM);
                sec.setForeground(Theme.textPrimary());
                sec.setBorder(new EmptyBorder(8, 0, 6, 0));
                eventsListContainer.add(sec);

                for (DueDateItem item : deadlineEvents) {
                    eventsListContainer.add(createEventCard(item, false, now));
                    eventsListContainer.add(Box.createVerticalStrut(6));
                }
            }

            // Starting / Since Events
            if (!sinceEvents.isEmpty()) {
                JLabel sec = new JLabel("Starting Today (" + sinceEvents.size() + "):");
                sec.setFont(Theme.FONT_MEDIUM);
                sec.setForeground(Theme.PURPLE_SINCE);
                sec.setBorder(new EmptyBorder(12, 0, 6, 0));
                eventsListContainer.add(sec);

                for (DueDateItem item : sinceEvents) {
                    eventsListContainer.add(createEventCard(item, true, now));
                    eventsListContainer.add(Box.createVerticalStrut(6));
                }
            }
        }

        eventsListContainer.revalidate();
        eventsListContainer.repaint();
    }

    private JPanel createEventCard(DueDateItem item, boolean isStart, Instant now) {
        ModernCard card = new ModernCard(8);
        card.setLayout(new BorderLayout(8, 4));
        card.setBorder(new EmptyBorder(8, 10, 8, 10));

        // Header: title and collision name if any
        boolean muted = storage.getTrackingConfig() != null && storage.getTrackingConfig().isMuted(item.getFileName());
        String displayTitle = storage.getDisplayTitle(item) + (muted ? " [Muted 🔕]" : "");
        JLabel titleLbl = new JLabel(displayTitle);
        titleLbl.setFont(Theme.FONT_MEDIUM);
        titleLbl.setForeground(Theme.textPrimary());

        DeadlineStatus status = item.getStatus(now, zone);
        JLabel badge = new JLabel(isStart ? " Since " : " " + status.getLabel() + " ");
        badge.setFont(Theme.FONT_SMALL);
        badge.setForeground(Color.WHITE);
        badge.setOpaque(true);
        badge.setBackground(isStart ? Theme.PURPLE_SINCE : status.getBadgeColor());

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleLbl, BorderLayout.WEST);
        top.add(badge, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        // Body: Person, time, and description
        String timeStr = isStart ? DueDateParser.formatDate(item.getSince(), zone) : DueDateParser.formatDate(item.getDeadline(), zone);
        JLabel subLbl = new JLabel("Time: " + timeStr + "  |  Person: " + item.getPerson());
        subLbl.setFont(Theme.FONT_SMALL);
        subLbl.setForeground(Theme.textSecondary());
        card.add(subLbl, BorderLayout.CENTER);

        if (!item.getDescription().isEmpty()) {
            JLabel descLbl = new JLabel(item.getSummaryDescription(60));
            descLbl.setFont(Theme.FONT_SMALL);
            descLbl.setForeground(Theme.textSecondary());
            card.add(descLbl, BorderLayout.SOUTH);
        }

        return card;
    }

    private static class CircleDot extends JComponent {
        private final Color color;
        private final int diameter;

        public CircleDot(Color color, int diameter) {
            this.color = color;
            this.diameter = diameter;
            setPreferredSize(new Dimension(diameter + 2, diameter + 2));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            Theme.applyAntiAliasing(g2);
            g2.setColor(color);
            g2.fillOval(1, 1, diameter, diameter);
            g2.dispose();
        }
    }
}
