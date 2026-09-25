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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.Locale;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import mekhq.gui.baseComponents.ImmersiveScrollBarStyle;

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
