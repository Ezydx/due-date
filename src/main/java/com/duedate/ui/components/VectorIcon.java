package com.duedate.ui.components;

import com.duedate.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * High-DPI vector-drawn icons using Java 2D Graphics.
 * Guaranteed to render 100% crisp and clean on all Windows configurations
 * with zero font/Unicode dependency.
 */
public class VectorIcon implements Icon {

    public enum IconType {
        REFRESH,
        SETTINGS,
        CALENDAR,
        IMPORT,
        PLUS,
        MUTE_BELL,
        HOURGLASS
    }

    private final IconType type;
    private final int size;
    private final Color color;

    public VectorIcon(IconType type, int size, Color color) {
        this.type = type;
        this.size = size;
        this.color = color;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.applyAntiAliasing(g2);
        g2.translate(x, y);

        Color drawColor = (color != null) ? color : (c != null ? c.getForeground() : Theme.textPrimary());
        g2.setColor(drawColor);

        double s = size;

        switch (type) {
            case REFRESH -> {
                g2.setStroke(new BasicStroke((float) (s * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Draw 270 degree arc
                int pad = (int) (s * 0.18);
                int arcSize = (int) (s - pad * 2);
                g2.drawArc(pad, pad, arcSize, arcSize, 45, 270);

                // Draw arrow head at top
                int ax = pad + arcSize / 2;
                int ay = pad;
                int aw = (int) (s * 0.18);
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(ax - aw * 0.4, ay - aw * 0.8);
                arrow.lineTo(ax + aw * 0.8, ay);
                arrow.lineTo(ax - aw * 0.4, ay + aw * 0.8);
                arrow.closePath();
                g2.fill(arrow);
            }
            case SETTINGS -> {
                // Outer gear
                g2.setStroke(new BasicStroke((float) (s * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double rOuter = s * 0.38;
                double rInner = s * 0.28;
                double cx = s / 2.0;
                double cy = s / 2.0;

                Path2D gear = new Path2D.Double();
                int teeth = 6;
                for (int i = 0; i < teeth * 2; i++) {
                    double angle = i * Math.PI / teeth;
                    double r = (i % 2 == 0) ? rOuter : rInner;
                    double px = cx + r * Math.cos(angle);
                    double py = cy + r * Math.sin(angle);
                    if (i == 0) gear.moveTo(px, py);
                    else gear.lineTo(px, py);
                }
                gear.closePath();
                g2.fill(gear);

                // Center hole
                g2.setColor(c != null ? c.getBackground() : Theme.surface());
                double holeR = s * 0.15;
                g2.fill(new Ellipse2D.Double(cx - holeR, cy - holeR, holeR * 2, holeR * 2));
            }
            case CALENDAR -> {
                int pad = (int) (s * 0.15);
                int w = (int) (s - pad * 2);
                int h = (int) (s - pad * 2);
                g2.setStroke(new BasicStroke((float) (s * 0.08), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                // Outer border
                g2.drawRoundRect(pad, pad, w, h, (int) (s * 0.15), (int) (s * 0.15));

                // Header bar
                int headH = (int) (h * 0.3);
                g2.fillRoundRect(pad, pad, w, headH, (int) (s * 0.15), (int) (s * 0.15));
                g2.fillRect(pad, pad + headH / 2, w, headH / 2);

                // Ring hooks
                g2.setStroke(new BasicStroke((float) (s * 0.09)));
                g2.drawLine(pad + (int)(w * 0.25), pad - (int)(s * 0.06), pad + (int)(w * 0.25), pad + (int)(s * 0.06));
                g2.drawLine(pad + (int)(w * 0.75), pad - (int)(s * 0.06), pad + (int)(w * 0.75), pad + (int)(s * 0.06));

                // Dots grid
                g2.setColor(drawColor);
                int dotPad = (int) (s * 0.04);
                int dotSize = (int) (s * 0.09);
                int startY = pad + headH + dotPad * 2;
                g2.fillRect(pad + (int)(w * 0.25), startY, dotSize, dotSize);
                g2.fillRect(pad + (int)(w * 0.50), startY, dotSize, dotSize);
                g2.fillRect(pad + (int)(w * 0.75), startY, dotSize, dotSize);
            }
            case IMPORT -> {
                g2.setStroke(new BasicStroke((float) (s * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = (int) (s * 0.15);
                int w = (int) (s - pad * 2);
                int h = (int) (s - pad * 2);

                // Folder outline
                Path2D folder = new Path2D.Double();
                folder.moveTo(pad, pad + (int)(h * 0.25));
                folder.lineTo(pad + (int)(w * 0.35), pad + (int)(h * 0.25));
                folder.lineTo(pad + (int)(w * 0.45), pad + (int)(h * 0.4));
                folder.lineTo(pad + w, pad + (int)(h * 0.4));
                folder.lineTo(pad + w, pad + h);
                folder.lineTo(pad, pad + h);
                folder.closePath();
                g2.draw(folder);

                // Arrow inside
                int cx = pad + w / 2;
                int cy = pad + (int)(h * 0.7);
                g2.drawLine(cx, cy - (int)(h * 0.2), cx, cy + (int)(h * 0.15));
                g2.drawLine(cx - (int)(w * 0.15), cy + (int)(h * 0.02), cx, cy + (int)(h * 0.15));
                g2.drawLine(cx + (int)(w * 0.15), cy + (int)(h * 0.02), cx, cy + (int)(h * 0.15));
            }
            case PLUS -> {
                g2.setStroke(new BasicStroke((float) (s * 0.15), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = (int) (s * 0.2);
                g2.drawLine(pad, (int)(s / 2), size - pad, (int)(s / 2));
                g2.drawLine((int)(s / 2), pad, (int)(s / 2), size - pad);
            }
            case MUTE_BELL -> {
                g2.setStroke(new BasicStroke((float) (s * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = size / 2;
                int cy = size / 2;
                // Bell body
                Path2D bell = new Path2D.Double();
                bell.moveTo(cx - s * 0.25, cy + s * 0.2);
                bell.curveTo(cx - s * 0.25, cy - s * 0.1, cx - s * 0.15, cy - s * 0.3, cx, cy - s * 0.3);
                bell.curveTo(cx + s * 0.15, cy - s * 0.3, cx + s * 0.25, cy - s * 0.1, cx + s * 0.25, cy + s * 0.2);
                bell.closePath();
                g2.draw(bell);

                // Slash across
                g2.setStroke(new BasicStroke((float) (s * 0.12), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(239, 68, 68));
                g2.drawLine((int)(s * 0.18), (int)(s * 0.18), (int)(s * 0.82), (int)(s * 0.82));
            }
            case HOURGLASS -> {
                g2.setStroke(new BasicStroke((float) (s * 0.09), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = (int) (s * 0.16);
                int w = size - pad * 2;
                int h = size - pad * 2;

                g2.drawLine(pad, pad, pad + w, pad);
                g2.drawLine(pad, pad + h, pad + w, pad + h);

                Path2D glass = new Path2D.Double();
                glass.moveTo(pad + (int)(w * 0.1), pad);
                glass.lineTo(pad + (int)(w * 0.9), pad);
                glass.lineTo(pad + (int)(w * 0.5), pad + h / 2);
                glass.lineTo(pad + (int)(w * 0.9), pad + h);
                glass.lineTo(pad + (int)(w * 0.1), pad + h);
                glass.lineTo(pad + (int)(w * 0.5), pad + h / 2);
                glass.closePath();
                g2.fill(glass);
            }
        }

        g2.dispose();
    }
}
