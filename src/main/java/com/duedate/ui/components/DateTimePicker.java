package com.duedate.ui.components;

import com.duedate.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;

/**
 * Clean and intuitive date & time picker component.
 * Allows picking Year, Month, Day, Hour (1-12), Minute (00-59), and AM/PM.
 */
public class DateTimePicker extends JPanel {

    private final JSpinner yearSpinner;
    private final JComboBox<String> monthCombo;
    private final JSpinner daySpinner;
    private final JSpinner hourSpinner;
    private final JSpinner minuteSpinner;
    private final JComboBox<String> amPmCombo;

    private static final String[] MONTHS = {
            "01 - Jan", "02 - Feb", "03 - Mar", "04 - Apr",
            "05 - May", "06 - Jun", "07 - Jul", "08 - Aug",
            "09 - Sep", "10 - Oct", "11 - Nov", "12 - Dec"
    };

    public DateTimePicker() {
        this(Instant.now(), ZoneId.systemDefault());
    }

    public DateTimePicker(Instant initialInstant, ZoneId zone) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 4));

        ZonedDateTime zdt = (initialInstant != null ? initialInstant : Instant.now()).atZone(zone != null ? zone : ZoneId.systemDefault());

        // Year Spinner
        yearSpinner = new JSpinner(new SpinnerNumberModel(zdt.getYear(), 1970, 2100, 1));
        styleSpinner(yearSpinner, 65);
        add(createLabeledWrapper("Year", yearSpinner));

        // Month Combo
        monthCombo = new JComboBox<>(MONTHS);
        monthCombo.setSelectedIndex(zdt.getMonthValue() - 1);
        styleComboBox(monthCombo);
        add(createLabeledWrapper("Month", monthCombo));

        // Day Spinner
        int maxDay = YearMonth.of(zdt.getYear(), zdt.getMonthValue()).lengthOfMonth();
        daySpinner = new JSpinner(new SpinnerNumberModel(Math.min(zdt.getDayOfMonth(), maxDay), 1, 31, 1));
        styleSpinner(daySpinner, 50);
        add(createLabeledWrapper("Day", daySpinner));

        // Update day maximum on month or year change
        monthCombo.addActionListener(e -> updateDayModel());
        yearSpinner.addChangeListener(e -> updateDayModel());

        // Hour Spinner (1-12)
        int hour24 = zdt.getHour();
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        hourSpinner = new JSpinner(new SpinnerNumberModel(hour12, 1, 12, 1));
        styleSpinner(hourSpinner, 50);
        add(createLabeledWrapper("Hour", hourSpinner));

        // Minute Spinner (0-59)
        minuteSpinner = new JSpinner(new SpinnerNumberModel(zdt.getMinute(), 0, 59, 1));
        styleSpinner(minuteSpinner, 50);
        add(createLabeledWrapper("Min", minuteSpinner));

        // AM/PM Combo
        amPmCombo = new JComboBox<>(new String[]{"AM", "PM"});
        amPmCombo.setSelectedIndex(hour24 >= 12 ? 1 : 0);
        styleComboBox(amPmCombo);
        add(createLabeledWrapper("AM/PM", amPmCombo));
    }

    private JPanel createLabeledWrapper(String labelText, JComponent comp) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.textSecondary());
        panel.add(label, BorderLayout.NORTH);
        panel.add(comp, BorderLayout.CENTER);

        return panel;
    }

    private void styleSpinner(JSpinner spinner, int width) {
        spinner.setFont(Theme.FONT_REGULAR);
        spinner.setPreferredSize(new Dimension(width, 28));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setFont(Theme.FONT_REGULAR);
            de.getTextField().setHorizontalAlignment(JTextField.CENTER);
        }
    }

    private void styleComboBox(JComboBox<?> combo) {
        combo.setFont(Theme.FONT_REGULAR);
        combo.setPreferredSize(new Dimension(combo == monthCombo ? 95 : 65, 28));
    }

    private void updateDayModel() {
        int year = (Integer) yearSpinner.getValue();
        int month = monthCombo.getSelectedIndex() + 1;
        int maxDays = YearMonth.of(year, month).lengthOfMonth();
        int currentDay = (Integer) daySpinner.getValue();
        daySpinner.setModel(new SpinnerNumberModel(Math.min(currentDay, maxDays), 1, maxDays, 1));
    }

    public Instant getDateTime(ZoneId zone) {
        ZoneId activeZone = (zone != null) ? zone : ZoneId.systemDefault();
        int year = (Integer) yearSpinner.getValue();
        int month = monthCombo.getSelectedIndex() + 1;
        int day = (Integer) daySpinner.getValue();

        int hour12 = (Integer) hourSpinner.getValue();
        int minute = (Integer) minuteSpinner.getValue();
        boolean isPm = "PM".equalsIgnoreCase((String) amPmCombo.getSelectedItem());

        int hour24 = hour12 % 12;
        if (isPm) {
            hour24 += 12;
        }

        try {
            LocalDateTime ldt = LocalDateTime.of(year, month, day, hour24, minute, 0);
            return ldt.atZone(activeZone).toInstant();
        } catch (DateTimeException e) {
            return Instant.now();
        }
    }

    public void setDateTime(Instant instant, ZoneId zone) {
        if (instant == null) instant = Instant.now();
        ZoneId activeZone = (zone != null) ? zone : ZoneId.systemDefault();
        ZonedDateTime zdt = instant.atZone(activeZone);

        yearSpinner.setValue(zdt.getYear());
        monthCombo.setSelectedIndex(zdt.getMonthValue() - 1);
        updateDayModel();
        daySpinner.setValue(Math.min(zdt.getDayOfMonth(), YearMonth.of(zdt.getYear(), zdt.getMonthValue()).lengthOfMonth()));

        int hour24 = zdt.getHour();
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        hourSpinner.setValue(hour12);
        minuteSpinner.setValue(zdt.getMinute());
        amPmCombo.setSelectedIndex(hour24 >= 12 ? 1 : 0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        yearSpinner.setEnabled(enabled);
        monthCombo.setEnabled(enabled);
        daySpinner.setEnabled(enabled);
        hourSpinner.setEnabled(enabled);
        minuteSpinner.setEnabled(enabled);
        amPmCombo.setEnabled(enabled);
    }
}
