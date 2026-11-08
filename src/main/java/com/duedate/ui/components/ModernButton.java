package com.duedate.ui.components;

import com.duedate.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Modern rounded button with hover and press effects.
 */
public class ModernButton extends JButton {

    public enum ButtonStyle {
        PRIMARY, SECONDARY, DANGER, SUCCESS
    }

    private ButtonStyle style = ButtonStyle.SECONDARY;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private int cornerRadius = 8;

    public ModernButton(String text) {
        this(text, ButtonStyle.SECONDARY);
    }

    public ModernButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;
        setFont(Theme.FONT_MEDIUM);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 16, 8, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                isPressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setButtonStyle(ButtonStyle style) {
        this.style = style;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);

        Color bg;
        Color fg;

        switch (style) {
            case PRIMARY -> {
                bg = isPressed ? Theme.accent().darker() : (isHovered ? Theme.accentHover() : Theme.accent());
                fg = Theme.accentText();
            }
            case DANGER -> {
                Color d = new Color(220, 38, 38);
                bg = isPressed ? d.darker() : (isHovered ? new Color(239, 68, 68) : d);
                fg = Color.WHITE;
            }
            case SUCCESS -> {
                Color s = new Color(16, 185, 129);
                bg = isPressed ? s.darker() : (isHovered ? new Color(52, 211, 153) : s);
                fg = Color.WHITE;
            }
            default -> { // SECONDARY
                bg = isPressed ? Theme.surfaceActive() : (isHovered ? Theme.surfaceHover() : Theme.surface());
                fg = Theme.textPrimary();
            }
        }

        if (!isEnabled()) {
            bg = Theme.surface();
            fg = Theme.textSecondary();
        }

        // Draw background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Draw border for secondary
        if (style == ButtonStyle.SECONDARY && isEnabled()) {
            g2.setColor(Theme.border());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }

        // Draw text
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;

        g2.setColor(fg);
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
