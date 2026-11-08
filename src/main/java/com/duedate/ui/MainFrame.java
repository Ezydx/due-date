package com.duedate.ui;

import com.duedate.config.AppConfig;
import com.duedate.io.DueDateParser;
import com.duedate.io.DueDateStorage;
import com.duedate.model.DeadlineStatus;
import com.duedate.model.DueDateItem;
import com.duedate.service.NotificationService;
import com.duedate.service.SoundService;
import com.duedate.ui.components.ModernButton;
import com.duedate.ui.components.ModernCard;
import com.duedate.ui.components.ModernTable;
import com.duedate.ui.components.SearchBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main application window for DueDate.
 */
public class MainFrame extends JFrame {

    private final AppConfig config;
    private DueDateStorage storage;
    private final NotificationService notificationService;
    private final ZoneId zone;
    private final File jarFile;

    private ModernTable table;
    private SearchBar searchBar;
    private JLabel statusLabel;
    private TrayIcon trayIcon;

    // Filter Chips
    private String currentStatusFilter = "ALL";
    private JLabel chipAll;
    private JLabel chipToday;
    private JLabel chipThisWeek;
    private JLabel chipOverdue;
    private JLabel chipFuture;

    private List<DueDateItem> allItems = new ArrayList<>();

    public MainFrame(AppConfig config, DueDateStorage storage, NotificationService notificationService, File jarFile, ZoneId zone) {
        super("DueDate");
        this.config = config;
        this.storage = storage;
        this.notificationService = notificationService;
        this.jarFile = jarFile;
        this.zone = (zone != null) ? zone : ZoneId.systemDefault();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1100, 720);
        setMinimumSize(new Dimension(850, 500));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.bg());

        // Set App Icon
        Image appIcon = createAppIcon(32, Theme.accent());
        setIconImage(appIcon);

        setupSystemTray(appIcon);
        setupWindowEvents();
        buildUI();
        refreshData();
    }

    private void setupWindowEvents() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (config.isMinimizeToTray() && SystemTray.isSupported() && trayIcon != null) {
                    setVisible(false);
                    if (trayIcon != null) {
                        trayIcon.displayMessage("DueDate", "Running in background to alert you of deadlines.", TrayIcon.MessageType.INFO);
                    }
                } else {
                    exitApplication();
                }
            }
        });
    }

    private void setupSystemTray(Image iconImage) {
        if (!SystemTray.isSupported()) return;

        SystemTray tray = SystemTray.getSystemTray();
        PopupMenu popup = new PopupMenu();

        MenuItem openItem = new MenuItem("Open DueDate");
        openItem.addActionListener(e -> restoreFromTray());

        MenuItem newDeadlineItem = new MenuItem("New Deadline...");
        newDeadlineItem.addActionListener(e -> openNewDeadlineDialog());

        MenuItem checkItem = new MenuItem("Check Deadlines Now");
        checkItem.addActionListener(e -> {
            notificationService.checkStartupNotifications();
            refreshData();
        });

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(e -> exitApplication());

        popup.add(openItem);
        popup.add(newDeadlineItem);
        popup.add(checkItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(iconImage, "DueDate - Deadline Manager", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> restoreFromTray());

        try {
            tray.add(trayIcon);
            notificationService.setTrayIcon(trayIcon);
        } catch (AWTException e) {
            System.err.println("Could not add tray icon: " + e.getMessage());
        }
    }

    public void restoreFromTray() {
        setVisible(true);
        setExtendedState(JFrame.NORMAL);
        toFront();
        requestFocus();
    }

    private void exitApplication() {
        if (trayIcon != null && SystemTray.isSupported()) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        notificationService.stop();
        dispose();
        System.exit(0);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.bg());
        root.setBorder(new EmptyBorder(14, 18, 12, 18));

        // 1. Top Header & Toolbar
        JPanel topPanel = new JPanel(new BorderLayout(14, 8));
        topPanel.setOpaque(false);

        // Logo & Title
        JPanel titleLogoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleLogoPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("⌛");
        logoLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        logoLabel.setForeground(Theme.accent());

        JLabel appTitle = new JLabel("DueDate");
        appTitle.setFont(Theme.FONT_HEADER);
        appTitle.setForeground(Theme.textPrimary());

        titleLogoPanel.add(logoLabel);
        titleLogoPanel.add(appTitle);
        topPanel.add(titleLogoPanel, BorderLayout.WEST);

        // Search Bar in Center
        searchBar = new SearchBar("Search title, description, person, or date...");
        searchBar.setOnSearch(query -> applyFilter());
        topPanel.add(searchBar, BorderLayout.CENTER);

        // Action Buttons on Right
        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        ModernButton newBtn = new ModernButton("+ New Deadline", ModernButton.ButtonStyle.PRIMARY);
        newBtn.addActionListener(e -> openNewDeadlineDialog());
        actionBtns.add(newBtn);

        ModernButton calendarBtn = new ModernButton("📅 Calendar", ModernButton.ButtonStyle.SECONDARY);
        calendarBtn.addActionListener(e -> openCalendarDialog());
        actionBtns.add(calendarBtn);

        ModernButton importBtn = new ModernButton("📁 Import", ModernButton.ButtonStyle.SECONDARY);
        importBtn.setToolTipText("Import any external .dd file");
        importBtn.addActionListener(e -> openImportDialog());
        actionBtns.add(importBtn);

        ModernButton refreshBtn = new ModernButton("↻", ModernButton.ButtonStyle.SECONDARY);
        refreshBtn.setToolTipText("Reload files from disk");
        refreshBtn.addActionListener(e -> refreshData());
        actionBtns.add(refreshBtn);

        ModernButton settingsBtn = new ModernButton("⚙", ModernButton.ButtonStyle.SECONDARY);
        settingsBtn.setToolTipText("Settings & Preferences");
        settingsBtn.addActionListener(e -> openSettingsDialog());
        actionBtns.add(settingsBtn);

        topPanel.add(actionBtns, BorderLayout.EAST);
        root.add(topPanel, BorderLayout.NORTH);

        // 2. Center Content: Filter Chips + Table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);

        // Filter chips bar
        JPanel chipsBar = buildFilterChipsBar();
        centerPanel.add(chipsBar, BorderLayout.NORTH);

        // Table
        table = new ModernTable(storage, zone);
        table.updateVisibleColumns(config.getVisibleColumns());

        // Double-click to edit
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedItem();
                }
            }
        });

        // Context Menu
        setupTableContextMenu();

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(Theme.border()));
        scrollTable.getViewport().setBackground(Theme.bg());
        centerPanel.add(scrollTable, BorderLayout.CENTER);

        root.add(centerPanel, BorderLayout.CENTER);

        // 3. Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setOpaque(false);
        statusBar.setBorder(new EmptyBorder(4, 4, 0, 4));

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.textSecondary());
        statusBar.add(statusLabel, BorderLayout.WEST);

        JLabel storagePathLbl = new JLabel("Data: " + storage.getStorageDirectory().toString());
        storagePathLbl.setFont(Theme.FONT_SMALL);
        storagePathLbl.setForeground(Theme.textSecondary());
        statusBar.add(storagePathLbl, BorderLayout.EAST);

        root.add(statusBar, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildFilterChipsBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bar.setOpaque(false);

        chipAll = createChip("ALL", "All (0)", Theme.textPrimary());
        chipToday = createChip("TODAY", "Today (0)", Theme.STATUS_TODAY);
        chipThisWeek = createChip("THIS_WEEK", "This Week (0)", Theme.STATUS_THIS_WEEK);
        chipOverdue = createChip("OVERDUE", "Ended / Overdue (0)", Theme.textSecondary());
        chipFuture = createChip("FUTURE", "Future (0)", Theme.STATUS_FUTURE);

        bar.add(chipAll);
        bar.add(chipToday);
        bar.add(chipThisWeek);
        bar.add(chipOverdue);
        bar.add(chipFuture);

        return bar;
    }

    private JLabel createChip(String filterKey, String initialText, Color accentColor) {
        JLabel chip = new JLabel(initialText) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                Theme.applyAntiAliasing(g2);

                boolean selected = filterKey.equals(currentStatusFilter);
                Color bg = selected ? Theme.surfaceActive() : Theme.surface();
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                g2.setColor(selected ? accentColor : Theme.border());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        chip.setFont(Theme.FONT_REGULAR);
        chip.setForeground(Theme.textPrimary());
        chip.setOpaque(false);
        chip.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        chip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentStatusFilter = filterKey;
                chipAll.repaint();
                chipToday.repaint();
                chipThisWeek.repaint();
                chipOverdue.repaint();
                chipFuture.repaint();
                applyFilter();
            }
        });

        return chip;
    }

    private void setupTableContextMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem editItem = new JMenuItem("Edit Deadline");
        editItem.addActionListener(e -> editSelectedItem());
        menu.add(editItem);

        JMenuItem duplicateItem = new JMenuItem("Duplicate");
        duplicateItem.addActionListener(e -> duplicateSelectedItem());
        menu.add(duplicateItem);

        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> deleteSelectedItem());
        menu.add(deleteItem);

        menu.addSeparator();

        JMenuItem openFileItem = new MenuItemWrapper("Open in File Explorer", e -> {
            DueDateItem item = table.getSelectedItem();
            if (item != null && item.getFileName() != null) {
                File f = storage.getStorageDirectory().resolve(item.getFileName()).toFile();
                try {
                    Desktop.getDesktop().open(f.getParentFile());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Could not open folder: " + ex.getMessage());
                }
            }
        });
        menu.add(openFileItem);

        JMenuItem chimeItem = new JMenuItem("Test Bell Chime");
        chimeItem.addActionListener(e -> SoundService.ringBell());
        menu.add(chimeItem);

        table.setComponentPopupMenu(menu);
    }

    private static class MenuItemWrapper extends JMenuItem {
        public MenuItemWrapper(String text, ActionListener listener) {
            super(text);
            addActionListener(listener);
        }
    }

    public void refreshData() {
        allItems = storage.loadAll();
        updateChipCounts();
        applyFilter();
    }

    private void updateChipCounts() {
        Instant now = Instant.now();
        int countToday = 0;
        int countThisWeek = 0;
        int countOverdue = 0;
        int countFuture = 0;

        for (DueDateItem item : allItems) {
            DeadlineStatus status = item.getStatus(now, zone);
            switch (status) {
                case TODAY -> countToday++;
                case THIS_WEEK -> countThisWeek++;
                case OVERDUE -> countOverdue++;
                case THIS_MONTH, FUTURE -> countFuture++;
            }
        }

        chipAll.setText("All (" + allItems.size() + ")");
        chipToday.setText("Today (" + countToday + ")");
        chipThisWeek.setText("This Week (" + countThisWeek + ")");
        chipOverdue.setText("Ended (" + countOverdue + ")");
        chipFuture.setText("Future (" + countFuture + ")");

        statusLabel.setText("Loaded " + allItems.size() + " deadline(s) from " + storage.getStorageDirectory().getFileName());
    }

    private void applyFilter() {
        String query = searchBar.getText().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();

        List<DueDateItem> filtered = allItems.stream().filter(item -> {
            // Status Chip Filter
            if (!"ALL".equals(currentStatusFilter)) {
                DeadlineStatus status = item.getStatus(now, zone);
                switch (currentStatusFilter) {
                    case "TODAY" -> { if (status != DeadlineStatus.TODAY) return false; }
                    case "THIS_WEEK" -> { if (status != DeadlineStatus.THIS_WEEK) return false; }
                    case "OVERDUE" -> { if (status != DeadlineStatus.OVERDUE) return false; }
                    case "FUTURE" -> { if (status != DeadlineStatus.THIS_MONTH && status != DeadlineStatus.FUTURE) return false; }
                }
            }

            // Universal Search Query Filter
            if (!query.isEmpty()) {
                String title = storage.getDisplayTitle(item).toLowerCase(Locale.ROOT);
                String desc = item.getDescription().toLowerCase(Locale.ROOT);
                String person = item.getPerson().toLowerCase(Locale.ROOT);
                String file = item.getFileName().toLowerCase(Locale.ROOT);
                String sinceStr = DueDateParser.formatDate(item.getSince(), zone).toLowerCase(Locale.ROOT);
                String deadlineStr = DueDateParser.formatDate(item.getDeadline(), zone).toLowerCase(Locale.ROOT);

                return title.contains(query) ||
                       desc.contains(query) ||
                       person.contains(query) ||
                       file.contains(query) ||
                       sinceStr.contains(query) ||
                       deadlineStr.contains(query);
            }

            return true;
        }).collect(Collectors.toList());

        table.setData(filtered);
    }

    private void openImportDialog() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select a .dd Deadline File to Import");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("DueDate Files (*.dd)", "dd", "txt"));
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            try {
                String content = java.nio.file.Files.readString(selected.toPath(), java.nio.charset.StandardCharsets.UTF_8);
                String fn = selected.getName();
                if (fn.toLowerCase(Locale.ROOT).endsWith(".dd.txt")) {
                    fn = fn.substring(0, fn.length() - 4);
                } else if (!fn.toLowerCase(Locale.ROOT).endsWith(".dd")) {
                    fn = fn + ".dd";
                }
                DueDateItem imported = DueDateParser.parse(content, fn, zone);
                storage.save(imported, null);
                refreshData();
                JOptionPane.showMessageDialog(this, "Successfully imported '" + storage.getDisplayTitle(imported) + "'!", "Import Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to import file: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void openNewDeadlineDialog() {
        DeadlineDialog dialog = new DeadlineDialog(this, storage, config, null, zone);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    private void editSelectedItem() {
        DueDateItem selected = table.getSelectedItem();
        if (selected == null) return;

        DeadlineDialog dialog = new DeadlineDialog(this, storage, config, selected, zone);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refreshData();
        }
    }

    private void duplicateSelectedItem() {
        DueDateItem selected = table.getSelectedItem();
        if (selected == null) return;

        DueDateItem copy = selected.copy();
        copy.setTitle(selected.getTitle() + " (Copy)");
        copy.setFileName(DueDateStorage.sanitizeFileName(copy.getTitle()) + ".dd");

        try {
            storage.save(copy, null);
            refreshData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not duplicate: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedItem() {
        DueDateItem selected = table.getSelectedItem();
        if (selected == null) return;

        int res = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete '" + storage.getDisplayTitle(selected) + "' (" + selected.getFileName() + ")?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (res == JOptionPane.YES_OPTION) {
            storage.delete(selected);
            refreshData();
        }
    }

    private void openCalendarDialog() {
        CalendarDialog dialog = new CalendarDialog(this, storage, zone);
        dialog.setVisible(true);
    }

    private void openSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this, config, jarFile, () -> {
            // Callback when settings updated
            if (!storage.getStorageDirectory().equals(config.getDataFolder())) {
                storage = new DueDateStorage(config.getDataFolder(), zone);
            }
            table.updateVisibleColumns(config.getVisibleColumns());
            refreshData();
        });
        dialog.setVisible(true);
    }

    private Image createAppIcon(int size, Color accentColor) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        Theme.applyAntiAliasing(g2);

        g2.setColor(accentColor);
        g2.fillRoundRect(0, 0, size, size, 8, 8);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, (int) (size * 0.55)));
        FontMetrics fm = g2.getFontMetrics();
        String txt = "DD";
        int x = (size - fm.stringWidth(txt)) / 2;
        int y = (size + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(txt, x, y);

        g2.dispose();
        return img;
    }
}
