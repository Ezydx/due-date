package com.duedate.ui.components;

import com.duedate.io.DueDateParser;
import com.duedate.io.DueDateStorage;
import com.duedate.model.DeadlineStatus;
import com.duedate.model.DueDateItem;
import com.duedate.ui.Theme;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

/**
 * Modern JTable tailored for displaying DueDate items with
 * custom badge renderers, column sorting, index numbers, and styling.
 */
public class ModernTable extends JTable {

    public static final String COL_INDEX = "#";
    public static final String COL_TITLE = "Title";
    public static final String COL_SINCE = "Since (Start)";
    public static final String COL_DEADLINE = "Deadline";
    public static final String COL_STATUS = "Status / Remaining";
    public static final String COL_PERSON = "Person";
    public static final String COL_DESCRIPTION = "Description";
    public static final String COL_FILENAME = "File Name";

    public static final List<String> ALL_COLUMNS = List.of(
            COL_INDEX, COL_TITLE, COL_SINCE, COL_DEADLINE, COL_STATUS, COL_PERSON, COL_DESCRIPTION, COL_FILENAME
    );

    private final DueDateTableModel model;
    private final DueDateStorage storage;
    private final ZoneId zone;

    private int sortColumnIndex = 3; // Default: Deadline
    private boolean sortAscending = true; // Default: Ascending

    public ModernTable(DueDateStorage storage, ZoneId zone) {
        this.storage = storage;
        this.zone = (zone != null) ? zone : ZoneId.systemDefault();
        this.model = new DueDateTableModel();

        setModel(model);
        setFont(Theme.FONT_REGULAR);
        setRowHeight(38);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        setupHeader();
        setupRenderers();
    }

    @Override
    public String getToolTipText(java.awt.event.MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        if (row >= 0 && row < model.getRowCount()) {
            DueDateItem item = model.getItemAt(row);
            if (item != null) {
                boolean muted = storage.getTrackingConfig() != null && storage.getTrackingConfig().isMuted(item.getFileName());
                Instant now = Instant.now();
                DeadlineStatus status = item.getStatus(now, zone);
                String title = storage.getDisplayTitle(item);
                String sinceStr = DueDateParser.formatDate(item.getSince(), zone);
                String deadlineStr = DueDateParser.formatDate(item.getDeadline(), zone);
                String remaining = item.getRemainingFormatted(now);
                String desc = item.getDescription();
                if (desc == null || desc.isBlank()) desc = "(No description)";

                return "<html><div style='padding:6px; font-family: Segoe UI, sans-serif; font-size:12px;'>" +
                        "<b style='font-size:13px; color:#4F46E5;'>" + escapeHtml(title) + "</b> " + (muted ? "<span style='color:#EF4444;'>[Muted 🔕]</span>" : "") + "<br/>" +
                        "<span style='color:#6B7280;'>File: " + escapeHtml(item.getFileName()) + " | Person: " + escapeHtml(item.getPerson()) + " | #" + item.getIntrinsicIndex() + "</span><hr style='border:0; border-top:1px solid #D1D5DB; margin:4px 0;'/>" +
                        "<b>Start (Since):</b> " + sinceStr + "<br/>" +
                        "<b>Deadline:</b> " + deadlineStr + " <span style='color:" + toHex(status.getBadgeColor()) + ";'>(" + remaining + ")</span><br/>" +
                        "<b>Status:</b> " + status.getLabel() + "<br/>" +
                        "<b>Description:</b><br/>" +
                        "<div style='max-width:340px; margin-top:2px; white-space:pre-wrap; color:#374151;'>" + escapeHtml(desc) + "</div>" +
                        "</div></html>";
            }
        }
        return super.getToolTipText(e);
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void setupHeader() {
        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(true);
        header.setFont(Theme.FONT_MEDIUM);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));

        header.setDefaultRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JLabel label = new JLabel();
            label.setFont(Theme.FONT_MEDIUM);
            label.setForeground(Theme.textPrimary());
            label.setBackground(Theme.surface());
            label.setOpaque(true);
            label.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.border()),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)
            ));

            String text = (value != null) ? value.toString() : "";
            int modelCol = table.convertColumnIndexToModel(column);
            if (modelCol == sortColumnIndex) {
                text += sortAscending ? "  ▲" : "  ▼";
            }
            label.setText(text);
            return label;
        });

        header.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int viewCol = header.columnAtPoint(e.getPoint());
                if (viewCol >= 0) {
                    int modelCol = convertColumnIndexToModel(viewCol);
                    if (sortColumnIndex == modelCol) {
                        sortAscending = !sortAscending;
                    } else {
                        sortColumnIndex = modelCol;
                        sortAscending = true;
                    }
                    model.sortItems();
                    header.repaint();
                }
            }
        });
    }

    private void setupRenderers() {
        setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                DueDateItem item = model.getItemAt(row);
                boolean muted = item != null && storage.getTrackingConfig() != null && storage.getTrackingConfig().isMuted(item.getFileName());

                if (isSelected) {
                    c.setBackground(Theme.accent().darker());
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.surface() : Theme.bg());
                    if (muted) {
                        c.setForeground(new Color(156, 163, 175)); // Muted dimmed text
                    } else {
                        c.setForeground(Theme.textPrimary());
                    }
                }

                String colName = table.getColumnName(column);
                if (!isSelected) {
                    if (COL_INDEX.equals(colName) || COL_DESCRIPTION.equals(colName)) {
                        c.setForeground(muted ? new Color(120, 125, 135) : Theme.textSecondary());
                    }
                }

                if (COL_TITLE.equals(colName) && muted) {
                    setText(getText() + "  [Muted 🔕]");
                }

                return c;
            }
        });

        // Custom renderer for Status badge
        setDefaultRenderer(DeadlineStatus.class, (table, value, isSelected, hasFocus, row, column) -> {
            DueDateItem item = model.getItemAt(row);
            Instant now = Instant.now();
            DeadlineStatus status = (value instanceof DeadlineStatus ds) ? ds : (item != null ? item.getStatus(now, zone) : DeadlineStatus.FUTURE);
            String remaining = (item != null) ? item.getRemainingFormatted(now) : "";
            boolean muted = item != null && storage.getTrackingConfig() != null && storage.getTrackingConfig().isMuted(item.getFileName());

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 8)) {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (isSelected) {
                        g.setColor(Theme.accent().darker());
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                }
            };
            panel.setOpaque(!isSelected);
            panel.setBackground(row % 2 == 0 ? Theme.surface() : Theme.bg());

            Color badgeBg = muted ? new Color(107, 114, 128) : status.getBadgeColor();
            String labelText = muted ? " [Muted] " + status.getLabel() : " " + status.getLabel() + " (" + remaining + ") ";

            JLabel badge = new JLabel(labelText) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.applyAntiAliasing(g2);
                    g2.setColor(badgeBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setOpaque(false);
            badge.setFont(Theme.FONT_SMALL);
            badge.setForeground(Color.WHITE);
            badge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));

            panel.add(badge);
            return panel;
        });
    }

    public void updateVisibleColumns(List<String> visibleCols) {
        model.setVisibleColumns(visibleCols);
        model.fireTableStructureChanged();
        setupHeader();

        // Adjust column minimum and preferred widths
        for (int i = 0; i < getColumnCount(); i++) {
            String name = getColumnName(i);
            TableColumn col = getColumnModel().getColumn(i);
            col.setResizable(true);
            switch (name) {
                case COL_INDEX -> {
                    col.setMinWidth(45);
                    col.setMaxWidth(65);
                    col.setPreferredWidth(50);
                }
                case COL_TITLE -> {
                    col.setMinWidth(120);
                    col.setPreferredWidth(180);
                }
                case COL_SINCE, COL_DEADLINE -> {
                    col.setMinWidth(135);
                    col.setPreferredWidth(160);
                }
                case COL_STATUS -> {
                    col.setMinWidth(140);
                    col.setPreferredWidth(180);
                }
                case COL_PERSON -> {
                    col.setMinWidth(70);
                    col.setPreferredWidth(90);
                }
                case COL_DESCRIPTION -> {
                    col.setMinWidth(120);
                    col.setPreferredWidth(200);
                }
                case COL_FILENAME -> {
                    col.setMinWidth(100);
                    col.setPreferredWidth(130);
                }
            }
        }
    }

    public void setData(List<DueDateItem> items) {
        model.setItems(items);
    }

    public DueDateItem getSelectedItem() {
        int row = getSelectedRow();
        if (row >= 0 && row < model.getRowCount()) {
            return model.getItemAt(row);
        }
        return null;
    }

    public class DueDateTableModel extends AbstractTableModel {

        private List<String> activeColumns = new ArrayList<>(ALL_COLUMNS);
        private final List<DueDateItem> displayedItems = new ArrayList<>();

        public void setVisibleColumns(List<String> cols) {
            this.activeColumns = new ArrayList<>();
            for (String c : ALL_COLUMNS) {
                if (cols.contains(c)) {
                    this.activeColumns.add(c);
                }
            }
            if (activeColumns.isEmpty()) {
                activeColumns.add(COL_TITLE);
            }
        }

        public void setItems(List<DueDateItem> items) {
            this.displayedItems.clear();
            if (items != null) {
                this.displayedItems.addAll(items);
            }
            sortItems();
            fireTableDataChanged();
        }

        public DueDateItem getItemAt(int row) {
            if (row >= 0 && row < displayedItems.size()) {
                return displayedItems.get(row);
            }
            return null;
        }

        public void sortItems() {
            if (sortColumnIndex < 0 || sortColumnIndex >= activeColumns.size()) return;
            String colName = activeColumns.get(sortColumnIndex);
            Instant now = Instant.now();

            Comparator<DueDateItem> comparator = switch (colName) {
                case COL_INDEX -> Comparator.comparingInt(DueDateItem::getIntrinsicIndex);
                case COL_TITLE -> Comparator.comparing(it -> storage.getDisplayTitle(it).toLowerCase(Locale.ROOT));
                case COL_SINCE -> Comparator.comparing(DueDateItem::getSince, Comparator.nullsLast(Comparator.naturalOrder()));
                case COL_DEADLINE -> Comparator.comparing(DueDateItem::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));
                case COL_STATUS -> Comparator.comparing(it -> it.getDeadline(), Comparator.nullsLast(Comparator.naturalOrder()));
                case COL_PERSON -> Comparator.comparing(it -> it.getPerson().toLowerCase(Locale.ROOT));
                case COL_DESCRIPTION -> Comparator.comparing(it -> it.getDescription().toLowerCase(Locale.ROOT));
                case COL_FILENAME -> Comparator.comparing(it -> it.getFileName().toLowerCase(Locale.ROOT));
                default -> Comparator.comparingInt(DueDateItem::getIntrinsicIndex);
            };

            if (!sortAscending) {
                comparator = comparator.reversed();
            }

            displayedItems.sort(comparator);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return displayedItems.size();
        }

        @Override
        public int getColumnCount() {
            return activeColumns.size();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return activeColumns.get(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            String col = activeColumns.get(columnIndex);
            if (COL_STATUS.equals(col)) {
                return DeadlineStatus.class;
            }
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            DueDateItem item = displayedItems.get(rowIndex);
            String colName = activeColumns.get(columnIndex);

            switch (colName) {
                case COL_INDEX -> {
                    return String.valueOf(item.getIntrinsicIndex());
                }
                case COL_TITLE -> {
                    // Includes disambiguating filename if collision exists
                    return storage.getDisplayTitle(item);
                }
                case COL_SINCE -> {
                    return DueDateParser.formatDate(item.getSince(), zone);
                }
                case COL_DEADLINE -> {
                    return DueDateParser.formatDate(item.getDeadline(), zone);
                }
                case COL_STATUS -> {
                    return item.getStatus(Instant.now(), zone);
                }
                case COL_PERSON -> {
                    return item.getPerson();
                }
                case COL_DESCRIPTION -> {
                    return item.getSummaryDescription(50);
                }
                case COL_FILENAME -> {
                    return item.getFileName();
                }
                default -> {
                    return "";
                }
            }
        }
    }
}
