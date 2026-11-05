package com.libraflow.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Centralized design system providing colors, typography, custom rounded cards,
 * styled tables, and modern buttons for LibraFlow Pro.
 */
public final class Theme {

    // Palette
    public static final Color SIDEBAR_BG = new Color(15, 23, 42);         // Slate 900 (#0F172A)
    public static final Color SIDEBAR_HOVER = new Color(30, 41, 59);      // Slate 800 (#1E293B)
    public static final Color SIDEBAR_ACTIVE = new Color(37, 99, 235);    // Royal Blue (#2563EB)
    public static final Color SIDEBAR_TEXT = new Color(226, 232, 240);    // Slate 200
    public static final Color SIDEBAR_MUTED = new Color(148, 163, 184);   // Slate 400

    public static final Color APP_BG = new Color(241, 245, 249);          // Slate 100 (#F1F5F9)
    public static final Color CARD_BG = Color.WHITE;
    public static final Color BORDER_COLOR = new Color(226, 232, 240);    // Slate 200

    public static final Color TEXT_PRIMARY = new Color(15, 23, 42);       // Slate 900
    public static final Color TEXT_SECONDARY = new Color(71, 85, 105);    // Slate 600
    public static final Color TEXT_MUTED = new Color(100, 116, 139);      // Slate 500

    public static final Color PRIMARY = new Color(37, 99, 235);           // Blue 600
    public static final Color PRIMARY_DARK = new Color(29, 78, 216);      // Blue 700
    public static final Color SUCCESS = new Color(16, 185, 129);          // Emerald 500
    public static final Color SUCCESS_BG = new Color(209, 250, 229);      // Emerald 100
    public static final Color WARNING = new Color(245, 158, 11);          // Amber 500
    public static final Color WARNING_BG = new Color(254, 243, 199);      // Amber 100
    public static final Color DANGER = new Color(239, 68, 68);            // Red 500
    public static final Color DANGER_BG = new Color(254, 226, 226);       // Red 100
    public static final Color PURPLE = new Color(124, 58, 237);           // Violet 600

    // Fonts
    public static final Font FONT_TITLE = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FONT_BODY = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_KPI = new Font("SansSerif", Font.BOLD, 26);
    public static final Font FONT_MONO = new Font("Monospaced", Font.BOLD, 12);

    private Theme() {}

    /**
     * Rounded card panel with clean border and white surface.
     */
    public static class CardPanel extends JPanel {
        private final int radius;

        public CardPanel(LayoutManager layout, int radius) {
            super(layout);
            this.radius = radius;
            setOpaque(false);
        }

        public CardPanel(LayoutManager layout) {
            this(layout, 14);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton createPrimaryButton(String text) {
        return createStyledButton(text, PRIMARY, Color.WHITE);
    }

    public static JButton createSuccessButton(String text) {
        return createStyledButton(text, SUCCESS, Color.WHITE);
    }

    public static JButton createDangerButton(String text) {
        return createStyledButton(text, DANGER, Color.WHITE);
    }

    public static JButton createSecondaryButton(String text) {
        return createStyledButton(text, new Color(226, 232, 240), TEXT_PRIMARY);
    }

    public static JButton createStyledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed()) {
                    g2.setColor(bg.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(brighten(bg, 0.08f));
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BODY_BOLD);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }

    private static Color brighten(Color c, float factor) {
        int r = Math.min(255, (int) (c.getRed() + (255 - c.getRed()) * factor));
        int g = Math.min(255, (int) (c.getGreen() + (255 - c.getGreen()) * factor));
        int b = Math.min(255, (int) (c.getBlue() + (255 - c.getBlue()) * factor));
        return new Color(r, g, b);
    }

    public static JTextField createStyledTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(FONT_BODY);
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(Color.WHITE);
        Border line = BorderFactory.createLineBorder(new Color(203, 213, 225), 1, true);
        Border pad = BorderFactory.createEmptyBorder(7, 10, 7, 10);
        tf.setBorder(BorderFactory.createCompoundBorder(line, pad));
        return tf;
    }

    public static <T> void styleComboBox(JComboBox<T> combo) {
        combo.setFont(FONT_BODY);
        combo.setBackground(Color.WHITE);
        combo.setForeground(TEXT_PRIMARY);
    }

    public static void styleTable(JTable table) {
        table.setFont(FONT_BODY);
        table.setRowHeight(36);
        table.setGridColor(new Color(241, 245, 249));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BODY_BOLD);
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(TEXT_PRIMARY);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }
    }

    public static JLabel createBadge(String text, Color bg, Color fg) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL.deriveFont(Font.BOLD));
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setForeground(fg);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        return lbl;
    }
}
