package com.duedate.ui.components;

import com.duedate.ui.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Universal search bar with icon, live filter callback, and clear button.
 */
public class SearchBar extends JPanel {

    private final JTextField textField;
    private final JLabel clearButton;
    private Consumer<String> onSearchCallback;

    public SearchBar(String placeholder) {
        setOpaque(false);
        setLayout(new BorderLayout(8, 0));
        setBorder(new EmptyBorder(4, 12, 4, 12));
        setPreferredSize(new Dimension(320, 36));

        // Search Icon
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        searchIcon.setForeground(Theme.textSecondary());
        add(searchIcon, BorderLayout.WEST);

        // Text Field
        textField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !(isFocusOwner())) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    Theme.applyAntiAliasing(g2);
                    g2.setColor(Theme.textSecondary());
                    g2.setFont(Theme.FONT_REGULAR);
                    FontMetrics fm = g2.getFontMetrics();
                    int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(placeholder, 4, y);
                    g2.dispose();
                }
            }
        };

        textField.setFont(Theme.FONT_REGULAR);
        textField.setForeground(Theme.textPrimary());
        textField.setCaretColor(Theme.textPrimary());
        textField.setOpaque(false);
        textField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        add(textField, BorderLayout.CENTER);

        // Clear Button
        clearButton = new JLabel("✕");
        clearButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        clearButton.setForeground(Theme.textSecondary());
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.setVisible(false);
        clearButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                textField.setText("");
                textField.requestFocusInWindow();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                clearButton.setForeground(Theme.textPrimary());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearButton.setForeground(Theme.textSecondary());
            }
        });
        add(clearButton, BorderLayout.EAST);

        // Document Listener for instant search
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateState();
            }

            private void updateState() {
                String text = textField.getText().trim();
                clearButton.setVisible(!text.isEmpty());
                if (onSearchCallback != null) {
                    onSearchCallback.accept(text);
                }
            }
        });
    }

    public void setOnSearch(Consumer<String> callback) {
        this.onSearchCallback = callback;
    }

    public String getText() {
        return textField.getText().trim();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);

        g2.setColor(Theme.surface());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);

        g2.setColor(textField.isFocusOwner() ? Theme.accent() : Theme.border());
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);

        g2.dispose();
        super.paintComponent(g);
    }
}
