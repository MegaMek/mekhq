/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.baseComponents.hud;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import megamek.client.ui.WrapLayout;
import mekhq.gui.baseComponents.ImmersiveScrollBarStyle;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Static helpers for building heads-up-display dialogs in the style of the interstellar-map tab, the StratCon
 * deployment wizard, and the contract debrief console: headings, hints, and HUD-styled scroll panes, tables, text
 * fields, and combo boxes. Colors and fonts come from {@link HudStyle}.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class Hud {
    private static final int SCROLLBAR_WIDTH = 10;

    private Hud() {
    }

    // region Text

    /**
     * A section heading: a tracked, upper-case accent label followed by a hairline divider rule, as the debrief
     * console heads its sections.
     *
     * @param text the heading text
     *
     * @return the heading
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JComponent sectionHeading(String text) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(text.toUpperCase(Locale.ROOT));
        label.setForeground(ACCENT);
        label.setFont(hudFont(Font.BOLD, 0.82f, 0.16f));
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        row.add(label, labelConstraints);

        JPanel rule = new JPanel();
        rule.setOpaque(true);
        rule.setBackground(DIVIDER);
        rule.setPreferredSize(new Dimension(scaleForGUI(10), scaleForGUI(1)));
        GridBagConstraints ruleConstraints = new GridBagConstraints();
        ruleConstraints.gridx = 1;
        ruleConstraints.weightx = 1.0;
        ruleConstraints.fill = GridBagConstraints.HORIZONTAL;
        ruleConstraints.insets = new Insets(0, scaleForGUI(10), 0, 0);
        row.add(rule, ruleConstraints);
        return row;
    }

    /**
     * A small, tracked, upper-case caption, as the map's inset panels label their contents.
     *
     * @param text the caption
     *
     * @return the caption label
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JLabel eyebrow(String text) {
        return HudStyle.keyLabel(text.toUpperCase(Locale.ROOT));
    }

    /**
     * A muted instruction line.
     *
     * @param text the instruction
     *
     * @return the label
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_MUTED);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        return label;
    }

    /**
     * A centred, faint notice for an empty page.
     *
     * @param text the notice
     *
     * @return the label
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JLabel notice(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        return label;
    }

    /**
     * A clickable accent-coloured link, for jumping to related content.
     *
     * @param text     the link text
     * @param onChoose called when the link is clicked, or activated with Space or Enter
     *
     * @return the link
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JLabel link(String text, Runnable onChoose) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT_BRIGHT);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setFocusable(true);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (label.isEnabled() && label.contains(event.getPoint())) {
                    onChoose.run();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                if (label.isEnabled()) {
                    label.setForeground(TEXT);
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                if (label.isEnabled()) {
                    label.setForeground(ACCENT_BRIGHT);
                }
            }
        });
        label.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (label.isEnabled() && (event.getKeyCode() == KeyEvent.VK_SPACE
                                                || event.getKeyCode() == KeyEvent.VK_ENTER)) {
                    onChoose.run();
                }
            }
        });
        return label;
    }

    /**
     * A small circled question mark that explains a term on hover.
     *
     * @param tooltip the explanation; wrapped to a comfortable width
     *
     * @return the mark
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JComponent helpMark(String tooltip) {
        JComponent mark = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D canvas = (Graphics2D) graphics.create();
                try {
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
                    int size = Math.min(getWidth(), getHeight()) - 1;
                    canvas.setColor(TEXT_FAINT);
                    canvas.drawOval(0, 0, size, size);
                    canvas.setFont(hudFont(Font.BOLD, 0.62f, 0.0f));
                    FontMetrics metrics = canvas.getFontMetrics();
                    canvas.drawString("?", (size + 1 - metrics.stringWidth("?")) / 2,
                          (size + 1 + metrics.getAscent() - metrics.getDescent()) / 2);
                } finally {
                    canvas.dispose();
                }
            }
        };
        int size = scaleForGUI(14);
        mark.setPreferredSize(new Dimension(size, size));
        mark.setMaximumSize(new Dimension(size, size));
        mark.setToolTipText("<html><div style='width:" + scaleForGUI(260) + "px'>" + tooltip + "</div></html>");
        return mark;
    }

    // endregion Text

    // region Containers

    /**
     * A row of tiles whose 1px gaps show the divider colour behind, matching the map's inset-line panels.
     *
     * @param tiles the tiles, left to right
     *
     * @return the row
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel tileRow(JComponent... tiles) {
        JPanel grid = new JPanel(new GridLayout(1, tiles.length, scaleForGUI(1), 0));
        grid.setOpaque(true);
        grid.setBackground(DIVIDER);
        grid.setBorder(BorderFactory.createLineBorder(DIVIDER, scaleForGUI(1)));
        for (JComponent tile : tiles) {
            grid.add(tile);
        }
        return grid;
    }

    /**
     * A transparent panel, for grouping HUD components without painting over the surface behind them.
     *
     * @param layout the panel's layout
     *
     * @return the panel
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel transparentPanel(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Plain text that wraps to the width it is given, for lines such as a window's subtitle that must never be cut off
     * when the window is narrow or the font is large.
     *
     * @param text  the text; it is shown as written, never read as HTML
     * @param color the text colour
     * @param font  the font
     *
     * @return the text component
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JLabel wrappingText(String text, Color color, Font font) {
        return wrappingHtml(StringEscapeUtils.escapeHtml4(text), color, font);
    }

    /**
     * HTML text that wraps to the width it is given and grows tall enough for every line. See
     * {@link HudWrappingLabel}.
     *
     * @param html  the body of the text, without the surrounding {@code <html>} tags
     * @param color the text colour
     * @param font  the font
     *
     * @return the text component
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JLabel wrappingHtml(String html, Color color, Font font) {
        JLabel label = new HudWrappingLabel("<html>" + html + "</html>");
        label.setForeground(color);
        label.setFont(font);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * A row of small boxed labels, tinted with one colour, that wraps to the width of its container. Used for short
     * lists such as the option groups a campaign preset replaces.
     *
     * @param labels the labels
     * @param tone   the tint of the boxes and their outlines
     *
     * @return the row
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel chipRow(List<String> labels, Color tone) {
        JPanel row = transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(6), scaleForGUI(6)));
        // WrapLayout pads the outside of the row by its gaps; pull the row back so the first chip lines up
        row.setBorder(BorderFactory.createEmptyBorder(-scaleForGUI(6), -scaleForGUI(6), 0, 0));
        for (String label : labels) {
            JLabel chip = new JLabel(label);
            chip.setOpaque(true);
            chip.setBackground(translucent(tone, 18));
            chip.setForeground(TEXT);
            chip.setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
            chip.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createLineBorder(translucent(tone, 140), scaleForGUI(1)),
                  BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(8), scaleForGUI(3), scaleForGUI(8))));
            row.add(chip);
        }
        return row;
    }

    /**
     * The bar across the top of a HUD window: the ground colour, a divider beneath, and the standard padding.
     *
     * @param layout the bar's layout
     *
     * @return the bar; add the window's identity and controls to it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel commandBar(LayoutManager layout) {
        JPanel bar = new JPanel(layout);
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(12), scaleForGUI(16))));
        return bar;
    }

    /**
     * A window's identity for its command bar: a small tracked eyebrow, a large title, and a subtitle that wraps.
     *
     * @param eyebrow  the line above the title
     * @param title    the title
     * @param subtitle the line beneath the title, as plain text
     *
     * @return the identity block
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel identity(String eyebrow, String title, String subtitle) {
        JPanel identity = transparentPanel(null);
        identity.setLayout(new BoxLayout(identity, BoxLayout.Y_AXIS));
        JLabel eyebrowLabel = new JLabel(eyebrow.toUpperCase(Locale.ROOT));
        eyebrowLabel.setForeground(TEXT_FAINT);
        eyebrowLabel.setFont(hudFont(Font.BOLD, 0.72f, 0.18f));
        JLabel titleLabel = new JLabel(title.toUpperCase(Locale.ROOT));
        titleLabel.setForeground(ACCENT_BRIGHT);
        titleLabel.setFont(hudFont(Font.BOLD, 1.35f, 0.16f));
        identity.add(leftAligned(eyebrowLabel));
        identity.add(Box.createVerticalStrut(scaleForGUI(3)));
        identity.add(leftAligned(titleLabel));
        identity.add(Box.createVerticalStrut(scaleForGUI(4)));
        identity.add(wrappingText(subtitle, TEXT_MUTED, hudFont(Font.PLAIN, 0.92f, 0.0f)));
        return identity;
    }

    /**
     * The main area of a HUD window, between its command bar and footer: the ground colour with the standard padding.
     *
     * @param layout the body's layout
     *
     * @return the body
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel windowBody(LayoutManager layout) {
        JPanel body = new JPanel(layout);
        body.setOpaque(true);
        body.setBackground(GROUND);
        body.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(16), scaleForGUI(16),
              scaleForGUI(16)));
        return body;
    }

    /**
     * The bar along the bottom of a HUD window: a summary line on the left and the window's buttons on the right.
     *
     * @param summary the summary line, styled here
     * @param actions the buttons, left to right
     *
     * @return the footer
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel footer(JLabel summary, JComponent... actions) {
        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(16), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(18), scaleForGUI(10), scaleForGUI(18))));

        summary.setForeground(TEXT_MUTED);
        summary.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));

        JPanel actionRow = transparentPanel(null);
        actionRow.setLayout(new BoxLayout(actionRow, BoxLayout.X_AXIS));
        for (int index = 0; index < actions.length; index++) {
            if (index > 0) {
                actionRow.add(Box.createHorizontalStrut(scaleForGUI(8)));
            }
            actionRow.add(actions[index]);
        }

        footer.add(summary, BorderLayout.CENTER);
        footer.add(actionRow, BorderLayout.EAST);
        return footer;
    }

    /**
     * Holds a component at the top of the space it is given, at its own height, such as a button beside a taller
     * block.
     *
     * @param component the component
     *
     * @return the holder
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JPanel topAligned(JComponent component) {
        JPanel holder = transparentPanel(new BorderLayout());
        holder.add(component, BorderLayout.NORTH);
        return holder;
    }

    // endregion Containers

    // region Controls

    /**
     * Styles a scroll pane for the HUD: no border (or a hairline when {@code framed}), a dark viewport, and the
     * debrief console's slim scrollbar.
     *
     * @param scroll        the scroll pane
     * @param viewportColor the viewport's background
     * @param framed        {@code true} to draw a hairline border
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void styleScroll(JScrollPane scroll, Color viewportColor, boolean framed) {
        scroll.setBorder(framed ? BorderFactory.createLineBorder(BORDER, scaleForGUI(1)) : null);
        scroll.setOpaque(true);
        scroll.setBackground(viewportColor);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(viewportColor);
        scroll.getVerticalScrollBar().setUnitIncrement(scaleForGUI(16));
        ImmersiveScrollBarStyle.apply(scroll.getVerticalScrollBar(), viewportColor, DIVIDER, SCROLLBAR_THUMB,
              ACCENT_BRIGHT, scaleForGUI(SCROLLBAR_WIDTH));
    }

    /**
     * Styles a text field for the HUD: a deep surface, a hairline border, and an accent caret.
     *
     * @param field the field
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void styleField(JTextField field) {
        field.setBackground(SURFACE_DEEP);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        field.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(8), scaleForGUI(4), scaleForGUI(8))));
    }

    /**
     * Styles a combo box for the HUD. Its items are shown with their {@code toString()} unless a renderer is set
     * afterward.
     *
     * @param comboBox the combo box
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setBackground(SURFACE_DEEP);
        comboBox.setForeground(TEXT);
        comboBox.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        comboBox.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        comboBox.setRenderer(comboRenderer());
    }

    /**
     * @return a list renderer for HUD combo boxes: deep-surface rows, raised to the surface colour when selected
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ListCellRenderer<Object> comboRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setOpaque(true);
                setBackground(isSelected ? SURFACE : SURFACE_DEEP);
                setForeground(TEXT);
                setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(8), scaleForGUI(3),
                      scaleForGUI(8)));
                return this;
            }
        };
    }

    /**
     * Styles a read-only roster table for the HUD: deep-surface rows with hairline dividers, and a tracked, upper-case
     * faint header on the surface colour. The last column is drawn in the accent colour, since every roster ends with
     * the "after issue" column the player is previewing.
     *
     * @param table the table
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void styleTable(JTable table) {
        table.setOpaque(true);
        table.setBackground(SURFACE_DEEP);
        table.setForeground(TEXT);
        table.setGridColor(DIVIDER);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, scaleForGUI(1)));
        table.setRowHeight(scaleForGUI(24));
        table.setFillsViewportHeight(true);
        table.setFocusable(false);
        table.setRowSelectionAllowed(false);
        table.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));

        final int lastColumn = table.getColumnCount() - 1;
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value, boolean isSelected,
                  boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(source, value, false, false, row, column);
                setOpaque(true);
                setBackground(SURFACE_DEEP);
                setForeground(column == lastColumn ? ACCENT_BRIGHT : (column == 0 ? TEXT : TEXT_MUTED));
                setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(10), 0, scaleForGUI(10)));
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setOpaque(true);
        header.setBackground(SURFACE);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable source, Object value, boolean isSelected,
                  boolean hasFocus, int row, int column) {
                String text = (value == null) ? "" : value.toString().toUpperCase(Locale.ROOT);
                super.getTableCellRendererComponent(source, text, false, false, row, column);
                setOpaque(true);
                setBackground(SURFACE);
                setForeground(TEXT_FAINT);
                setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
                setBorder(BorderFactory.createCompoundBorder(
                      BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
                      BorderFactory.createEmptyBorder(scaleForGUI(7), scaleForGUI(10), scaleForGUI(7),
                            scaleForGUI(10))));
                return this;
            }
        };
        header.setDefaultRenderer(headerRenderer);
    }

    // endregion Controls
}
