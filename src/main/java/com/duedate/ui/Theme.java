package com.duedate.ui;

import java.awt.*;
import javax.swing.UIManager;

/**
 * Modern color palette, typography and styles for DueDate UI.
 */
public class Theme {

    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_MEDIUM = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_LARGE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

    // Status / Proximity Colors
    public static final Color PURPLE_SINCE = new Color(168, 85, 247); // Start date (always purple)
    public static final Color STATUS_OVERDUE = new Color(30, 30, 35);  // Black / Ended
    public static final Color STATUS_TODAY = new Color(239, 68, 68);    // Red
    public static final Color STATUS_THIS_WEEK = new Color(249, 115, 22); // Orange
    public static final Color STATUS_THIS_MONTH = new Color(16, 185, 129); // Green
    public static final Color STATUS_FUTURE = new Color(59, 130, 246);  // Blue (>1 month)

    public static boolean isDark = true;

    // Dynamic colors based on theme
    public static Color bg() {
        return isDark ? new Color(24, 24, 27) : new Color(248, 250, 252);
    }

    public static Color surface() {
        return isDark ? new Color(39, 39, 42) : new Color(255, 255, 255);
    }

    public static Color surfaceHover() {
        return isDark ? new Color(50, 50, 55) : new Color(241, 245, 249);
    }

    public static Color surfaceActive() {
        return isDark ? new Color(63, 63, 70) : new Color(226, 232, 240);
    }

    public static Color card() {
        return isDark ? new Color(32, 32, 36) : new Color(255, 255, 255);
    }

    public static Color border() {
        return isDark ? new Color(63, 63, 70) : new Color(203, 213, 225);
    }

    public static Color textPrimary() {
        return isDark ? new Color(244, 244, 245) : new Color(15, 23, 42);
    }

    public static Color textSecondary() {
        return isDark ? new Color(161, 161, 170) : new Color(100, 116, 139);
    }

    public static Color accent() {
        return new Color(79, 70, 229); // Indigo 600
    }

    public static Color accentHover() {
        return new Color(99, 102, 241); // Indigo 500
    }

    public static Color accentText() {
        return Color.WHITE;
    }

    public static void applyAntiAliasing(Graphics g) {
        if (g instanceof Graphics2D g2) {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
    }
}
