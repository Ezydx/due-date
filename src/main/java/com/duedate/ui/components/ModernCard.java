package com.duedate.ui.components;

import com.duedate.ui.Theme;

import javax.swing.*;
import java.awt.*;

/**
 * Rounded card container panel with modern styling.
 */
public class ModernCard extends JPanel {

    private int cornerRadius = 12;
    private Color customBackground = null;
    private boolean drawBorder = true;

    public ModernCard() {
        this(12);
    }

    public ModernCard(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        setOpaque(false);
    }

    public void setCustomBackground(Color color) {
        this.customBackground = color;
        repaint();
    }

    public void setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);

        Color bg = (customBackground != null) ? customBackground : Theme.card();
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        if (drawBorder) {
            g2.setColor(Theme.border());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
