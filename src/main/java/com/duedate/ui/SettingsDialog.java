package com.duedate.ui;

import com.duedate.config.AppConfig;
import com.duedate.service.WindowsStartupService;
import com.duedate.ui.components.ModernButton;
import com.duedate.ui.components.ModernCard;
import com.duedate.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern settings dialog for configuring notifications, Windows startup,
 * storage directory, visible columns, and themes.
 */
public class SettingsDialog extends JDialog {

    private final AppConfig config;
    private final com.duedate.io.DueDateStorage storage;
    private final File jarFile;
    private final Runnable onSettingsUpdated;

    private JCheckBox startupCheckbox;
    private JCheckBox minimizeTrayCheckbox;
    private JCheckBox soundChimeCheckbox;
    private JCheckBox startupNotifyCheckbox;
    private JCheckBox notifyTodayCheckbox;
    private JCheckBox notifyWeekCheckbox;
    private JCheckBox notifyOverdueCheckbox;
    private JSpinner reminderIntervalSpinner;
    private JTextField defaultPersonField;
    private JTextField dataFolderField;
    private JComboBox<String> themeCombo;
    private JLabel trackingSummaryLabel;

    private final List<JCheckBox> columnCheckboxes = new ArrayList<>();

    public SettingsDialog(Frame owner, AppConfig config, com.duedate.io.DueDateStorage storage, File jarFile, Runnable onSettingsUpdated) {
        super(owner, "DueDate Settings", true);
        this.config = config;
        this.storage = storage;
        this.jarFile = jarFile;
        this.onSettingsUpdated = onSettingsUpdated;

        setSize(650, 720);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(Theme.bg());

        buildUI();
        loadValues();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.bg());
        root.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel title = new JLabel("Settings & Preferences");
        title.setFont(Theme.FONT_HEADER);
        title.setForeground(Theme.textPrimary());
        root.add(title, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // 1. Windows Startup & Background
        content.add(createSectionHeader("System & Startup"));
        ModernCard sysCard = new ModernCard(8);
        sysCard.setLayout(new BoxLayout(sysCard, BoxLayout.Y_AXIS));
        sysCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        startupCheckbox = createCheckBox("Start with Windows (Run in background on boot)");
        minimizeTrayCheckbox = createCheckBox("Minimize to System Tray when closing window");
        sysCard.add(startupCheckbox);
        sysCard.add(Box.createVerticalStrut(6));
        sysCard.add(minimizeTrayCheckbox);
        content.add(sysCard);
        content.add(Box.createVerticalStrut(12));

        // 2. Notification Preferences
        content.add(createSectionHeader("Notifications & Chimes"));
        ModernCard notifyCard = new ModernCard(8);
        notifyCard.setLayout(new BoxLayout(notifyCard, BoxLayout.Y_AXIS));
        notifyCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        soundChimeCheckbox = createCheckBox("Play chime bell sound on reminder alerts");
        startupNotifyCheckbox = createCheckBox("Show deadline summary upon app / system startup");
        notifyTodayCheckbox = createCheckBox("Alert for deadlines due Today");
        notifyWeekCheckbox = createCheckBox("Alert for deadlines due This Week");
        notifyOverdueCheckbox = createCheckBox("Alert for Overdue deadlines");

        notifyCard.add(soundChimeCheckbox);
        notifyCard.add(Box.createVerticalStrut(6));
        notifyCard.add(startupNotifyCheckbox);
        notifyCard.add(Box.createVerticalStrut(6));
        notifyCard.add(notifyTodayCheckbox);
        notifyCard.add(Box.createVerticalStrut(6));
        notifyCard.add(notifyWeekCheckbox);
        notifyCard.add(Box.createVerticalStrut(6));
        notifyCard.add(notifyOverdueCheckbox);
        notifyCard.add(Box.createVerticalStrut(8));

        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        intervalPanel.setOpaque(false);
        JLabel intervalLbl = new JLabel("Persistent Reminder Frequency:");
        intervalLbl.setFont(Theme.FONT_REGULAR);
        intervalLbl.setForeground(Theme.textPrimary());
        reminderIntervalSpinner = new JSpinner(new SpinnerNumberModel(60, 5, 1440, 15));
        reminderIntervalSpinner.setPreferredSize(new Dimension(65, 28));
        JLabel minLbl = new JLabel("minutes (if deadline is today/overdue)");
        minLbl.setFont(Theme.FONT_SMALL);
        minLbl.setForeground(Theme.textSecondary());

        intervalPanel.add(intervalLbl);
        intervalPanel.add(reminderIntervalSpinner);
        intervalPanel.add(minLbl);
        notifyCard.add(intervalPanel);

        content.add(notifyCard);
        content.add(Box.createVerticalStrut(12));

        // 3. Storage Directory & User Tag
        content.add(createSectionHeader("Storage & Default Profile"));
        ModernCard storeCard = new ModernCard(8);
        storeCard.setLayout(new BoxLayout(storeCard, BoxLayout.Y_AXIS));
        storeCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel dirRow = new JPanel(new BorderLayout(8, 0));
        dirRow.setOpaque(false);
        dataFolderField = new JTextField();
        dataFolderField.setFont(Theme.FONT_REGULAR);
        dataFolderField.setEditable(false);
        dirRow.add(dataFolderField, BorderLayout.CENTER);

        JPanel dirBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        dirBtns.setOpaque(false);
        ModernButton openDirBtn = new ModernButton("Open", ModernButton.ButtonStyle.SECONDARY);
        openDirBtn.addActionListener(e -> openDirectoryInExplorer());
        ModernButton changeDirBtn = new ModernButton("Browse", ModernButton.ButtonStyle.SECONDARY);
        changeDirBtn.addActionListener(e -> chooseDirectory());
        dirBtns.add(openDirBtn);
        dirBtns.add(changeDirBtn);
        dirRow.add(dirBtns, BorderLayout.EAST);
        storeCard.add(dirRow);

        storeCard.add(Box.createVerticalStrut(8));

        JPanel personRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        personRow.setOpaque(false);
        JLabel personLbl = new JLabel("Default Person / Tag:");
        personLbl.setFont(Theme.FONT_REGULAR);
        personLbl.setForeground(Theme.textPrimary());
        defaultPersonField = new JTextField(15);
        defaultPersonField.setFont(Theme.FONT_REGULAR);
        personRow.add(personLbl);
        personRow.add(defaultPersonField);
        storeCard.add(personRow);

        content.add(storeCard);
        content.add(Box.createVerticalStrut(12));

        // 4. Visible Columns
        content.add(createSectionHeader("Display Columns"));
        ModernCard colsCard = new ModernCard(8);
        colsCard.setLayout(new GridLayout(2, 4, 8, 6));
        colsCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        for (String col : ModernTable.ALL_COLUMNS) {
            JCheckBox cb = createCheckBox(col);
            columnCheckboxes.add(cb);
            colsCard.add(cb);
        }
        content.add(colsCard);
        content.add(Box.createVerticalStrut(12));

        // 5. Tracking & Muted Files (tracked.conf)
        content.add(createSectionHeader("Tracking & Overdues (tracked.conf)"));
        ModernCard trackCard = new ModernCard(8);
        trackCard.setLayout(new BorderLayout(8, 6));
        trackCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        trackingSummaryLabel = new JLabel();
        trackingSummaryLabel.setFont(Theme.FONT_REGULAR);
        trackingSummaryLabel.setForeground(Theme.textPrimary());
        trackCard.add(trackingSummaryLabel, BorderLayout.CENTER);

        JPanel trackBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        trackBtns.setOpaque(false);

        ModernButton unmuteAllBtn = new ModernButton("Unmute All", ModernButton.ButtonStyle.SECONDARY);
        unmuteAllBtn.addActionListener(e -> {
            if (storage != null && storage.getTrackingConfig() != null) {
                for (String f : new ArrayList<>(storage.getTrackingConfig().getMutedFiles())) {
                    storage.getTrackingConfig().setMuted(f, false);
                }
                updateTrackingLabel();
            }
        });
        trackBtns.add(unmuteAllBtn);

        ModernButton retrackAllBtn = new ModernButton("Re-track All", ModernButton.ButtonStyle.SECONDARY);
        retrackAllBtn.addActionListener(e -> {
            if (storage != null && storage.getTrackingConfig() != null) {
                for (String f : new ArrayList<>(storage.getTrackingConfig().getUntrackedFiles())) {
                    storage.getTrackingConfig().setUntracked(f, false);
                }
                updateTrackingLabel();
            }
        });
        trackBtns.add(retrackAllBtn);

        trackCard.add(trackBtns, BorderLayout.SOUTH);
        content.add(trackCard);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        root.add(scroll, BorderLayout.CENTER);

        // Buttons
        JPanel btm = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btm.setOpaque(false);
        ModernButton cancelBtn = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        cancelBtn.addActionListener(e -> dispose());
        ModernButton saveBtn = new ModernButton("Save Preferences", ModernButton.ButtonStyle.PRIMARY);
        saveBtn.addActionListener(e -> saveSettings());
        btm.add(cancelBtn);
        btm.add(saveBtn);
        root.add(btm, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel createSectionHeader(String title) {
        JLabel l = new JLabel(title);
        l.setFont(Theme.FONT_MEDIUM);
        l.setForeground(Theme.accentHover());
        l.setBorder(new EmptyBorder(4, 0, 4, 0));
        return l;
    }

    private JCheckBox createCheckBox(String label) {
        JCheckBox cb = new JCheckBox(label);
        cb.setFont(Theme.FONT_REGULAR);
        cb.setForeground(Theme.textPrimary());
        cb.setOpaque(false);
        return cb;
    }

    private void loadValues() {
        startupCheckbox.setSelected(config.isStartWithWindows() || WindowsStartupService.isStartupEnabled());
        minimizeTrayCheckbox.setSelected(config.isMinimizeToTray());
        soundChimeCheckbox.setSelected(config.isSoundChime());
        startupNotifyCheckbox.setSelected(config.isStartupNotification());
        notifyTodayCheckbox.setSelected(config.isNotifyToday());
        notifyWeekCheckbox.setSelected(config.isNotifyThisWeek());
        notifyOverdueCheckbox.setSelected(config.isNotifyOverdue());
        reminderIntervalSpinner.setValue(config.getPersistentReminderMinutes());
        defaultPersonField.setText(config.getDefaultPerson());
        dataFolderField.setText(config.getDataFolder().toAbsolutePath().toString());

        List<String> visible = config.getVisibleColumns();
        for (JCheckBox cb : columnCheckboxes) {
            cb.setSelected(visible.contains(cb.getText()));
        }
        updateTrackingLabel();
    }

    private void updateTrackingLabel() {
        if (storage != null && storage.getTrackingConfig() != null) {
            int muted = storage.getTrackingConfig().getMutedFiles().size();
            int untracked = storage.getTrackingConfig().getUntrackedFiles().size();
            trackingSummaryLabel.setText(String.format("Muted (reminders off): %d file(s)  |  Untracked (ignored): %d file(s)", muted, untracked));
        } else {
            trackingSummaryLabel.setText("Tracking manager active.");
        }
    }

    private void openDirectoryInExplorer() {
        try {
            Desktop.getDesktop().open(config.getDataFolder().toFile());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not open directory: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void chooseDirectory() {
        JFileChooser chooser = new JFileChooser(config.getDataFolder().toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            dataFolderField.setText(selected.getAbsolutePath());
        }
    }

    private void saveSettings() {
        boolean startWin = startupCheckbox.isSelected();
        config.setStartWithWindows(startWin);
        if (jarFile != null) {
            WindowsStartupService.setStartupEnabled(startWin, jarFile);
        }

        config.setMinimizeToTray(minimizeTrayCheckbox.isSelected());
        config.setSoundChime(soundChimeCheckbox.isSelected());
        config.setStartupNotification(startupNotifyCheckbox.isSelected());
        config.setNotifyToday(notifyTodayCheckbox.isSelected());
        config.setNotifyThisWeek(notifyWeekCheckbox.isSelected());
        config.setNotifyOverdue(notifyOverdueCheckbox.isSelected());
        config.setPersistentReminderMinutes((Integer) reminderIntervalSpinner.getValue());

        String person = defaultPersonField.getText().trim();
        config.setDefaultPerson(person.isEmpty() ? "Guest" : person);

        String folder = dataFolderField.getText().trim();
        if (!folder.isEmpty()) {
            config.setDataFolder(Paths.get(folder));
        }

        List<String> selectedCols = new ArrayList<>();
        for (JCheckBox cb : columnCheckboxes) {
            if (cb.isSelected()) {
                selectedCols.add(cb.getText());
            }
        }
        if (selectedCols.isEmpty()) {
            selectedCols.add(ModernTable.COL_TITLE);
        }
        config.setVisibleColumns(selectedCols);

        config.save();

        if (onSettingsUpdated != null) {
            onSettingsUpdated.run();
        }
        dispose();
    }
}
