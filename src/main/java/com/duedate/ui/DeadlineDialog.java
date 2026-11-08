package com.duedate.ui;

import com.duedate.config.AppConfig;
import com.duedate.io.DueDateParser;
import com.duedate.io.DueDateStorage;
import com.duedate.model.DueDateItem;
import com.duedate.model.RelativeOffset;
import com.duedate.ui.components.DateTimePicker;
import com.duedate.ui.components.ModernButton;
import com.duedate.ui.components.ModernCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Modern modal dialog for creating and editing deadline items.
 * Includes "Now" checkbox for start date, direct vs relative deadline resolver,
 * person tag, and filename collision handling.
 */
public class DeadlineDialog extends JDialog {

    private final DueDateStorage storage;
    private final AppConfig config;
    private final ZoneId zone;
    private final DueDateItem existingItem;
    private final String originalFileName;

    private boolean saved = false;
    private DueDateItem resultItem = null;

    // Form fields
    private JTextField titleField;
    private JLabel charCountLabel;
    private JTextField fileNameField;
    private JTextField personField;
    private JCheckBox sinceNowCheckbox;
    private DateTimePicker sincePicker;

    // Deadline selection
    private JRadioButton directRadio;
    private JRadioButton relativeRadio;
    private JPanel deadlineCards;
    private CardLayout deadlineCardLayout;

    private DateTimePicker directDeadlinePicker;

    // Relative offset controls
    private JComboBox<RelativeOffset.BaseOption> relativeBaseCombo;
    private DateTimePicker customBasePicker;
    private JPanel customBaseWrapper;
    private JSpinner relYearsSpinner;
    private JSpinner relMonthsSpinner;
    private JSpinner relDaysSpinner;
    private JSpinner relHoursSpinner;
    private JSpinner relMinutesSpinner;
    private JLabel resolvedDeadlineLabel;

    private JTextArea descriptionArea;

    public DeadlineDialog(Frame owner, DueDateStorage storage, AppConfig config, DueDateItem itemToEdit, ZoneId zone) {
        super(owner, itemToEdit == null ? "New Deadline" : "Edit Deadline", true);
        this.storage = storage;
        this.config = config;
        this.zone = (zone != null) ? zone : ZoneId.systemDefault();
        this.existingItem = itemToEdit;
        this.originalFileName = itemToEdit != null ? itemToEdit.getFileName() : null;

        setSize(680, 720);
        setLocationRelativeTo(owner);
        setResizable(true);
        getContentPane().setBackground(Theme.bg());

        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Theme.bg());
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header Title
        JLabel headerLabel = new JLabel(existingItem == null ? "Create New Deadline" : "Edit Deadline Details");
        headerLabel.setFont(Theme.FONT_HEADER);
        headerLabel.setForeground(Theme.textPrimary());
        root.add(headerLabel, BorderLayout.NORTH);

        // Scrollable Form Content
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        // 1. Title & Character Count
        form.add(createFieldLabel("Title:"));
        JPanel titlePanel = new JPanel(new BorderLayout(8, 0));
        titlePanel.setOpaque(false);

        titleField = createStyledTextField();
        titlePanel.add(titleField, BorderLayout.CENTER);

        charCountLabel = new JLabel("0 / 64");
        charCountLabel.setFont(Theme.FONT_SMALL);
        charCountLabel.setForeground(Theme.textSecondary());
        titlePanel.add(charCountLabel, BorderLayout.EAST);
        form.add(titlePanel);

        titleField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { update(); }
            @Override
            public void removeUpdate(DocumentEvent e) { update(); }
            @Override
            public void changedUpdate(DocumentEvent e) { update(); }

            private void update() {
                int len = titleField.getText().length();
                charCountLabel.setText(len + " / 64");
                charCountLabel.setForeground(len > 64 ? Theme.STATUS_TODAY : Theme.textSecondary());

                // Auto suggest filename if new and user hasn't explicitly typed custom filename
                if (existingItem == null && (fileNameField.getText().isEmpty() || fileNameField.getText().endsWith(".dd"))) {
                    fileNameField.setText(DueDateStorage.sanitizeFileName(titleField.getText()) + ".dd");
                }
            }
        });

        form.add(Box.createVerticalStrut(12));

        // 2. Person & File Name (Two Columns)
        JPanel twoCol = new JPanel(new GridLayout(1, 2, 16, 0));
        twoCol.setOpaque(false);

        JPanel personCol = new JPanel(new BorderLayout(0, 4));
        personCol.setOpaque(false);
        personCol.add(createFieldLabel("Person / Tag:"), BorderLayout.NORTH);
        personField = createStyledTextField();
        personField.setText(config.getDefaultPerson());
        personCol.add(personField, BorderLayout.CENTER);
        twoCol.add(personCol);

        JPanel fileCol = new JPanel(new BorderLayout(0, 4));
        fileCol.setOpaque(false);
        fileCol.add(createFieldLabel("File Name (.dd):"), BorderLayout.NORTH);
        fileNameField = createStyledTextField();
        fileCol.add(fileNameField, BorderLayout.CENTER);
        twoCol.add(fileCol);

        form.add(twoCol);
        form.add(Box.createVerticalStrut(14));

        // 3. Start Date (Since) Card
        ModernCard sinceCard = new ModernCard(8);
        sinceCard.setLayout(new BorderLayout(8, 8));
        sinceCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel sinceHeader = new JPanel(new BorderLayout());
        sinceHeader.setOpaque(false);
        JLabel sinceTitle = new JLabel("Start Date (Since):");
        sinceTitle.setFont(Theme.FONT_MEDIUM);
        sinceTitle.setForeground(Theme.PURPLE_SINCE);
        sinceHeader.add(sinceTitle, BorderLayout.WEST);

        sinceNowCheckbox = new JCheckBox("Set to Now (Auto)");
        sinceNowCheckbox.setFont(Theme.FONT_REGULAR);
        sinceNowCheckbox.setForeground(Theme.textPrimary());
        sinceNowCheckbox.setOpaque(false);
        sinceNowCheckbox.setSelected(true);
        sinceNowCheckbox.addActionListener(e -> {
            boolean isNow = sinceNowCheckbox.isSelected();
            sincePicker.setEnabled(!isNow);
            if (isNow) {
                sincePicker.setDateTime(Instant.now(), zone);
            }
            updateResolvedRelativeDeadline();
        });
        sinceHeader.add(sinceNowCheckbox, BorderLayout.EAST);
        sinceCard.add(sinceHeader, BorderLayout.NORTH);

        sincePicker = new DateTimePicker(Instant.now(), zone);
        sincePicker.setEnabled(false); // Default: now is checked
        sinceCard.add(sincePicker, BorderLayout.CENTER);
        form.add(sinceCard);

        form.add(Box.createVerticalStrut(14));

        // 4. Deadline Card (Direct vs Relative)
        ModernCard deadlineCard = new ModernCard(8);
        deadlineCard.setLayout(new BorderLayout(8, 8));
        deadlineCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel deadlineHeader = new JPanel(new BorderLayout());
        deadlineHeader.setOpaque(false);
        JLabel deadlineTitle = new JLabel("Deadline (Target Due Date):");
        deadlineTitle.setFont(Theme.FONT_MEDIUM);
        deadlineTitle.setForeground(Theme.STATUS_TODAY);
        deadlineHeader.add(deadlineTitle, BorderLayout.WEST);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        modePanel.setOpaque(false);
        directRadio = new JRadioButton("Direct Date", true);
        relativeRadio = new JRadioButton("Relative Offset", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(directRadio);
        bg.add(relativeRadio);

        directRadio.setFont(Theme.FONT_REGULAR);
        directRadio.setForeground(Theme.textPrimary());
        directRadio.setOpaque(false);
        relativeRadio.setFont(Theme.FONT_REGULAR);
        relativeRadio.setForeground(Theme.textPrimary());
        relativeRadio.setOpaque(false);

        modePanel.add(directRadio);
        modePanel.add(relativeRadio);
        deadlineHeader.add(modePanel, BorderLayout.EAST);
        deadlineCard.add(deadlineHeader, BorderLayout.NORTH);

        // CardLayout for Direct vs Relative
        deadlineCardLayout = new CardLayout();
        deadlineCards = new JPanel(deadlineCardLayout);
        deadlineCards.setOpaque(false);

        // Panel A: Direct
        JPanel directPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        directPanel.setOpaque(false);
        directDeadlinePicker = new DateTimePicker(Instant.now().plusSeconds(86400), zone);
        directPanel.add(directDeadlinePicker);
        deadlineCards.add(directPanel, "DIRECT");

        // Panel B: Relative
        JPanel relativePanel = buildRelativePanel();
        deadlineCards.add(relativePanel, "RELATIVE");

        deadlineCard.add(deadlineCards, BorderLayout.CENTER);

        directRadio.addActionListener(e -> deadlineCardLayout.show(deadlineCards, "DIRECT"));
        relativeRadio.addActionListener(e -> {
            deadlineCardLayout.show(deadlineCards, "RELATIVE");
            updateResolvedRelativeDeadline();
        });

        form.add(deadlineCard);
        form.add(Box.createVerticalStrut(14));

        // 5. Description
        form.add(createFieldLabel("Description / Notes:"));
        descriptionArea = new JTextArea(4, 30);
        descriptionArea.setFont(Theme.FONT_REGULAR);
        descriptionArea.setForeground(Theme.textPrimary());
        descriptionArea.setBackground(Theme.surface());
        descriptionArea.setCaretColor(Theme.textPrimary());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scrollDesc = new JScrollPane(descriptionArea);
        scrollDesc.setBorder(BorderFactory.createLineBorder(Theme.border()));
        form.add(scrollDesc);

        JScrollPane mainScroll = new JScrollPane(form);
        mainScroll.setBorder(null);
        mainScroll.setOpaque(false);
        mainScroll.getViewport().setOpaque(false);
        root.add(mainScroll, BorderLayout.CENTER);

        // Bottom Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonPanel.setOpaque(false);

        ModernButton cancelButton = new ModernButton("Cancel", ModernButton.ButtonStyle.SECONDARY);
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        ModernButton saveButton = new ModernButton("Save Deadline", ModernButton.ButtonStyle.PRIMARY);
        saveButton.addActionListener(e -> onSave());
        buttonPanel.add(saveButton);

        root.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildRelativePanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        // Base Selector Row
        JPanel baseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        baseRow.setOpaque(false);
        JLabel baseLabel = new JLabel("Base:");
        baseLabel.setFont(Theme.FONT_SMALL);
        baseLabel.setForeground(Theme.textSecondary());
        baseRow.add(baseLabel);

        relativeBaseCombo = new JComboBox<>(RelativeOffset.BaseOption.values());
        relativeBaseCombo.setFont(Theme.FONT_REGULAR);
        relativeBaseCombo.addActionListener(e -> {
            boolean isCustom = relativeBaseCombo.getSelectedItem() == RelativeOffset.BaseOption.CUSTOMIZE;
            customBaseWrapper.setVisible(isCustom);
            updateResolvedRelativeDeadline();
        });
        baseRow.add(relativeBaseCombo);

        p.add(baseRow);

        // Custom base picker wrapper (hidden unless CUSTOMIZE is selected)
        customBaseWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        customBaseWrapper.setOpaque(false);
        customBaseWrapper.setVisible(false);
        customBasePicker = new DateTimePicker(Instant.now(), zone);
        customBaseWrapper.add(customBasePicker);
        p.add(customBaseWrapper);

        // Offset Spinners Row
        JPanel spinRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        spinRow.setOpaque(false);

        relYearsSpinner = createOffsetSpinner(0, 0, 50, "Years");
        relMonthsSpinner = createOffsetSpinner(0, 0, 120, "Months");
        relDaysSpinner = createOffsetSpinner(1, 0, 3650, "Days");
        relHoursSpinner = createOffsetSpinner(0, 0, 240, "Hours");
        relMinutesSpinner = createOffsetSpinner(0, 0, 1440, "Mins");

        spinRow.add(createLabeledSpinner("+ Years", relYearsSpinner));
        spinRow.add(createLabeledSpinner("+ Months", relMonthsSpinner));
        spinRow.add(createLabeledSpinner("+ Days", relDaysSpinner));
        spinRow.add(createLabeledSpinner("+ Hours", relHoursSpinner));
        spinRow.add(createLabeledSpinner("+ Mins", relMinutesSpinner));

        p.add(spinRow);

        // Resolved preview label
        resolvedDeadlineLabel = new JLabel("Resolved: --");
        resolvedDeadlineLabel.setFont(Theme.FONT_MEDIUM);
        resolvedDeadlineLabel.setForeground(Theme.STATUS_THIS_MONTH);
        resolvedDeadlineLabel.setBorder(new EmptyBorder(6, 4, 2, 4));
        p.add(resolvedDeadlineLabel);

        return p;
    }

    private JSpinner createOffsetSpinner(int initial, int min, int max, String tip) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, min, max, 1));
        spinner.setPreferredSize(new Dimension(54, 28));
        spinner.setFont(Theme.FONT_REGULAR);
        spinner.addChangeListener(e -> updateResolvedRelativeDeadline());
        return spinner;
    }

    private JPanel createLabeledSpinner(String text, JSpinner spinner) {
        JPanel wrap = new JPanel(new BorderLayout(0, 2));
        wrap.setOpaque(false);
        JLabel lbl = new JLabel(text);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.textSecondary());
        wrap.add(lbl, BorderLayout.NORTH);
        wrap.add(spinner, BorderLayout.CENTER);
        return wrap;
    }

    private void updateResolvedRelativeDeadline() {
        if (resolvedDeadlineLabel == null) return;

        RelativeOffset offset = new RelativeOffset(
                (RelativeOffset.BaseOption) relativeBaseCombo.getSelectedItem(),
                (Integer) relYearsSpinner.getValue(),
                (Integer) relMonthsSpinner.getValue(),
                (Integer) relDaysSpinner.getValue(),
                (Integer) relHoursSpinner.getValue(),
                (Integer) relMinutesSpinner.getValue()
        );

        if (offset.getBaseOption() == RelativeOffset.BaseOption.CUSTOMIZE) {
            offset.setCustomBase(customBasePicker.getDateTime(zone));
        }

        Instant sinceInstant = sinceNowCheckbox.isSelected() ? Instant.now() : sincePicker.getDateTime(zone);
        Instant resolved = offset.resolve(Instant.now(), sinceInstant, zone);

        String formatted = DueDateParser.formatDate(resolved, zone);
        resolvedDeadlineLabel.setText("Resolved Target: " + formatted);
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.FONT_MEDIUM);
        l.setForeground(Theme.textPrimary());
        l.setBorder(new EmptyBorder(4, 0, 4, 0));
        return l;
    }

    private JTextField createStyledTextField() {
        JTextField tf = new JTextField();
        tf.setFont(Theme.FONT_REGULAR);
        tf.setForeground(Theme.textPrimary());
        tf.setBackground(Theme.surface());
        tf.setCaretColor(Theme.textPrimary());
        tf.setPreferredSize(new Dimension(tf.getPreferredSize().width, 32));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.border()),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return tf;
    }

    private void loadData() {
        if (existingItem != null) {
            titleField.setText(existingItem.getTitle());
            fileNameField.setText(existingItem.getFileName());
            personField.setText(existingItem.getPerson());
            descriptionArea.setText(existingItem.getDescription());

            sinceNowCheckbox.setSelected(false);
            sincePicker.setEnabled(true);
            sincePicker.setDateTime(existingItem.getSince(), zone);

            directRadio.setSelected(true);
            directDeadlinePicker.setDateTime(existingItem.getDeadline(), zone);
            deadlineCardLayout.show(deadlineCards, "DIRECT");
        } else {
            sinceNowCheckbox.setSelected(true);
            sincePicker.setEnabled(false);
            sincePicker.setDateTime(Instant.now(), zone);
            updateResolvedRelativeDeadline();
        }
    }

    private void onSave() {
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title cannot be empty.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            titleField.requestFocusInWindow();
            return;
        }

        String fileName = fileNameField.getText().trim();
        if (fileName.isEmpty()) {
            fileName = DueDateStorage.sanitizeFileName(title) + ".dd";
        }
        if (!fileName.toLowerCase().endsWith(".dd")) {
            fileName = fileName + ".dd";
        }

        // If file exists and is not the one we are editing
        if (originalFileName == null || !originalFileName.equalsIgnoreCase(fileName)) {
            java.nio.file.Path target = storage.getStorageDirectory().resolve(fileName);
            if (java.nio.file.Files.exists(target)) {
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "A file named '" + fileName + "' already exists. Overwrite it?",
                        "File Conflict",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }

        Instant sinceInstant = sinceNowCheckbox.isSelected() ? Instant.now() : sincePicker.getDateTime(zone);
        Instant deadlineInstant;

        if (directRadio.isSelected()) {
            deadlineInstant = directDeadlinePicker.getDateTime(zone);
        } else {
            RelativeOffset offset = new RelativeOffset(
                    (RelativeOffset.BaseOption) relativeBaseCombo.getSelectedItem(),
                    (Integer) relYearsSpinner.getValue(),
                    (Integer) relMonthsSpinner.getValue(),
                    (Integer) relDaysSpinner.getValue(),
                    (Integer) relHoursSpinner.getValue(),
                    (Integer) relMinutesSpinner.getValue()
            );
            if (offset.getBaseOption() == RelativeOffset.BaseOption.CUSTOMIZE) {
                offset.setCustomBase(customBasePicker.getDateTime(zone));
            }
            deadlineInstant = offset.resolve(Instant.now(), sinceInstant, zone);
        }

        String person = personField.getText().trim();
        if (person.isEmpty()) person = "Guest";

        String desc = descriptionArea.getText();

        DueDateItem item = (existingItem != null) ? existingItem : new DueDateItem();
        item.setTitle(title);
        item.setFileName(fileName);
        item.setSince(sinceInstant);
        item.setDeadline(deadlineInstant);
        item.setPerson(person);
        item.setDescription(desc);

        try {
            storage.save(item, originalFileName);
            saved = true;
            resultItem = item;
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public DueDateItem getResultItem() {
        return resultItem;
    }
}
