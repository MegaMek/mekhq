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
package mekhq.gui.dialog.quartermaster;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.stratCon.deployment.HudStyle.*;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

import mekhq.gui.baseComponents.ImmersiveScrollBarStyle;

/**
 * The heads-up-display building blocks for the kit-issue dialog, giving it the look of the interstellar-map tab, the
 * StratCon deployment wizard, and the contract debrief console: the shared {@code HudStyle} palette and fonts, a
 * segmented tab strip with the map's glowing accent underline, section headings with a divider rule, stat tiles whose
 * value can be updated live, and dark, hairline-bordered scroll panes and tables.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class KitHud {
    /** The scrollbar thumb colour the debrief console uses. */
    private static final Color SCROLLBAR_THUMB = new Color(54, 101, 113);
    private static final int SCROLLBAR_WIDTH = 10;

    private KitHud() {
    }

    // region Text
    /**
     * A section heading: a tracked, upper-case accent label followed by a hairline divider rule, as the debrief
     * console heads its sections.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static JComponent sectionHeading(String text) {
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
     * A muted instruction line.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static JLabel hint(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_MUTED);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        return label;
    }

    /**
     * A centred, faint notice for an empty page.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static JLabel notice(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.PLAIN, 0.95f, 0.0f));
        return label;
    }
    // endregion Text

    // region Scrolling and tables
    /**
     * Styles a scroll pane for the HUD: no border (or a hairline when {@code framed}), a dark viewport, and the
     * debrief console's slim scrollbar.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void styleScroll(JScrollPane scroll, Color viewportColor, boolean framed) {
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
     * Styles a read-only roster table for the HUD: deep-surface rows with hairline dividers, and a tracked, upper-case
     * faint header on the surface colour. The last column is drawn in the accent colour, since every roster ends with
     * the "after issue" column the player is previewing.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void styleTable(JTable table) {
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
    // endregion Scrolling and tables

    // region Stat tile
    /**
     * A deep-surface stat tile - a small key, a large coloured value, and a faint sub-line - matching the debrief
     * console's tiles, with a value that can be updated as the player's selection changes.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static final class StatTile extends JPanel {
        private final JLabel valueLabel = new JLabel();
        private final JLabel subLabel = new JLabel();

        StatTile(String key, String sub) {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(true);
            setBackground(SURFACE_DEEP);
            int inset = scaleForGUI(11);
            setBorder(BorderFactory.createEmptyBorder(inset, inset + scaleForGUI(3), inset, inset + scaleForGUI(3)));

            JLabel keyLabel = new JLabel(key.toUpperCase(Locale.ROOT));
            keyLabel.setForeground(TEXT_FAINT);
            keyLabel.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
            add(leftAligned(keyLabel));
            add(Box.createVerticalStrut(scaleForGUI(4)));

            valueLabel.setFont(hudFont(Font.BOLD, 1.45f, 0.0f));
            valueLabel.setForeground(TEXT);
            add(leftAligned(valueLabel));

            add(Box.createVerticalStrut(scaleForGUI(3)));
            subLabel.setForeground(TEXT_FAINT);
            subLabel.setFont(hudFont(Font.PLAIN, 0.78f, 0.0f));
            subLabel.setText(sub);
            add(leftAligned(subLabel));
        }

        void setValue(String value, Color color) {
            valueLabel.setText(value);
            valueLabel.setForeground(color);
        }

        /** A floor on the width, so the command bar does not shift as the tallies grow and shrink. */
        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            return new Dimension(Math.max(preferred.width, scaleForGUI(150)), preferred.height);
        }
    }

    /**
     * A row of tiles whose 1px gaps show the divider colour behind, matching the map's inset-line panels.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static JPanel tileRow(JComponent... tiles) {
        JPanel grid = new JPanel(new GridLayout(1, tiles.length, scaleForGUI(1), 0));
        grid.setOpaque(true);
        grid.setBackground(DIVIDER);
        grid.setBorder(BorderFactory.createLineBorder(DIVIDER, scaleForGUI(1)));
        for (JComponent tile : tiles) {
            grid.add(tile);
        }
        return grid;
    }
    // endregion Stat tile

    // region Tab strip
    /**
     * A segmented tab strip in the style of the deployment wizard's mode selector: one cell per page, the selected
     * cell raised to the surface colour with the map's accent underline. Fires on mouse release and on space/enter.
     * {@code compact} draws a lighter, smaller strip for a second level of tabs.
     *
     * @author Illiani
     * @since 0.51.01
     */
    static final class TabStrip extends JPanel {
        private final List<Cell> cells = new ArrayList<>();
        private final transient IntConsumer onSelect;

        TabStrip(List<String> labels, boolean compact, IntConsumer onSelect) {
            this.onSelect = onSelect;
            setLayout(new GridLayout(1, labels.size(), 0, 0));
            setOpaque(true);
            setBackground(SURFACE_DEEP);
            setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
            for (int index = 0; index < labels.size(); index++) {
                Cell cell = new Cell(index, labels.get(index), compact, index < labels.size() - 1);
                cells.add(cell);
                add(cell);
            }
        }

        /** Highlights a cell without firing the selection callback. */
        void setSelected(int selectedIndex) {
            for (int index = 0; index < cells.size(); index++) {
                cells.get(index).setSelected(index == selectedIndex);
            }
        }

        /** Replaces a cell's label (for example to update a count). */
        void setLabel(int index, String label) {
            cells.get(index).title.setText(label.toUpperCase(Locale.ROOT));
        }

        private final class Cell extends JPanel {
            private final int index;
            private final boolean rightBorder;
            private final JLabel title;
            private boolean selected;
            private boolean hovered;

            private Cell(int index, String label, boolean compact, boolean rightBorder) {
                this.index = index;
                this.rightBorder = rightBorder;
                setOpaque(false);
                setFocusable(true);
                int vertical = scaleForGUI(compact ? 7 : 10);
                setBorder(BorderFactory.createEmptyBorder(vertical, scaleForGUI(6), vertical, scaleForGUI(6)));
                setLayout(new GridBagLayout());

                title = new JLabel(label.toUpperCase(Locale.ROOT), SwingConstants.CENTER);
                title.setForeground(TEXT_MUTED);
                title.setFont(hudFont(Font.BOLD, compact ? 0.76f : 0.85f, compact ? 0.1f : 0.06f));
                add(title);

                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseReleased(MouseEvent event) {
                        if (contains(event.getPoint())) {
                            choose();
                        }
                    }

                    @Override
                    public void mouseEntered(MouseEvent event) {
                        hovered = true;
                        refreshTitleColor();
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent event) {
                        hovered = false;
                        refreshTitleColor();
                        repaint();
                    }
                });
                addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent event) {
                        if ((event.getKeyCode() == KeyEvent.VK_SPACE) || (event.getKeyCode() == KeyEvent.VK_ENTER)) {
                            choose();
                        }
                    }
                });
            }

            private void choose() {
                requestFocusInWindow();
                onSelect.accept(index);
            }

            private void setSelected(boolean selected) {
                this.selected = selected;
                refreshTitleColor();
                repaint();
            }

            private void refreshTitleColor() {
                title.setForeground((selected || hovered) ? TEXT : TEXT_MUTED);
            }

            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g2 = (Graphics2D) graphics.create();
                try {
                    int width = getWidth();
                    int height = getHeight();
                    g2.setColor(selected ? SURFACE : (hovered ? SURFACE_HIGHLIGHT : SURFACE_DEEP));
                    g2.fillRect(0, 0, width, height);
                    if (rightBorder) {
                        g2.setColor(BORDER);
                        g2.fillRect(width - scaleForGUI(1), 0, scaleForGUI(1), height);
                    }
                    if (selected) {
                        int underline = scaleForGUI(2);
                        g2.setColor(ACCENT);
                        g2.fillRect(0, height - underline, width, underline);
                    }
                } finally {
                    g2.dispose();
                }
                super.paintComponent(graphics);
            }
        }
    }
    // endregion Tab strip

    static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }
}
