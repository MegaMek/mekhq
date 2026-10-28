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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;

/**
 * A column of filter rows for a HUD side rail: each row has a label and a count, and exactly one row is selected,
 * raised to the surface colour with an accent bar on its left. Rows fire on mouse release and on space/enter; the up
 * and down arrows move between rows.
 *
 * <p>Choosing a row only notifies the owner, which applies the filter and selects the row with
 * {@link #setSelected(Object)}. Setting the items or the selection never notifies.</p>
 *
 * @param <T> the type of value each row stands for; {@code null} is a valid value, often meaning "all"
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudFilterList<T> extends JPanel {
    /**
     * One row.
     *
     * @param value the value the row stands for
     * @param label the row's text
     * @param count the number shown on the right, or a negative number to show none
     */
    public record Item<T>(@Nullable T value, String label, int count) {}

    private final transient Consumer<T> onChoose;
    private final List<Row> rows = new ArrayList<>();
    private transient List<Item<T>> items = List.of();
    private transient T selected;

    /**
     * @param onChoose called with the row's value when the player chooses a row
     */
    public HudFilterList(Consumer<T> onChoose) {
        this.onChoose = onChoose;
        setOpaque(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    /**
     * Replaces the rows, keeping the selection if one of the new rows has the selected value.
     *
     * @param items the rows, top to bottom
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setItems(List<Item<T>> items) {
        this.items = List.copyOf(items);
        removeAll();
        rows.clear();
        for (int index = 0; index < this.items.size(); index++) {
            Row row = new Row(index, this.items.get(index));
            rows.add(row);
            add(row);
        }
        refresh();
        revalidate();
        repaint();
    }

    /**
     * Selects the row with this value, without notifying the owner.
     *
     * @param value the value
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setSelected(@Nullable T value) {
        selected = value;
        refresh();
    }

    /**
     * @return the selected value
     */
    public @Nullable T getSelected() {
        return selected;
    }

    private void refresh() {
        int selectedIndex = indexOf(items, selected);
        for (Row row : rows) {
            row.setSelected(row.index == selectedIndex);
        }
    }

    private void choose(int index) {
        rows.get(index).requestFocusInWindow();
        onChoose.accept(items.get(index).value());
    }

    /**
     * @param items the rows
     * @param value a value
     *
     * @return the index of the first row with that value, or {@code -1} if none has it
     *
     * @author Illiani
     * @since 0.51.01
     */
    static <T> int indexOf(List<Item<T>> items, @Nullable T value) {
        for (int index = 0; index < items.size(); index++) {
            if (Objects.equals(items.get(index).value(), value)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * @param index the current row
     * @param step  how far to move, negative for up
     * @param size  how many rows there are
     *
     * @return the row to move to, kept within the list
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int step(int index, int step, int size) {
        if (size == 0) {
            return -1;
        }
        return Math.max(0, Math.min(size - 1, index + step));
    }

    /**
     * @param count a count
     *
     * @return the count as shown on a row, grouped by thousands, or an empty string for a negative count
     */
    static String formatCount(int count) {
        return (count < 0) ? "" : NumberFormat.getIntegerInstance().format(count);
    }

    private final class Row extends JPanel {
        private final int index;
        private final JLabel label;
        private final JLabel count;
        private boolean isSelected;
        private boolean hovered;

        private Row(int index, Item<T> item) {
            super(new BorderLayout(scaleForGUI(8), 0));
            this.index = index;
            setOpaque(false);
            setFocusable(true);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(8), scaleForGUI(4), scaleForGUI(6)));

            label = new JLabel(item.label());
            label.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            count = new JLabel(formatCount(item.count()));
            count.setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
            count.setForeground(TEXT_FAINT);
            add(label, BorderLayout.CENTER);
            add(count, BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (contains(event.getPoint())) {
                        choose(Row.this.index);
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = true;
                    refreshRow();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    refreshRow();
                }
            });
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> choose(Row.this.index);
                        case KeyEvent.VK_UP -> focusRow(step(Row.this.index, -1, rows.size()));
                        case KeyEvent.VK_DOWN -> focusRow(step(Row.this.index, 1, rows.size()));
                        default -> {
                            // other keys are left to the window
                        }
                    }
                }
            });
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent event) {
                    refreshRow();
                }

                @Override
                public void focusLost(FocusEvent event) {
                    refreshRow();
                }
            });
            refreshRow();
        }

        private void focusRow(int target) {
            if (target >= 0) {
                rows.get(target).requestFocusInWindow();
            }
        }

        private void setSelected(boolean isSelected) {
            this.isSelected = isSelected;
            refreshRow();
        }

        private void refreshRow() {
            label.setForeground((isSelected || hovered || isFocusOwner()) ? TEXT : TEXT_MUTED);
            repaint();
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                if (isSelected || hovered) {
                    g2.setColor(SURFACE);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                if (isSelected) {
                    g2.setColor(ACCENT);
                    g2.fillRect(0, 0, scaleForGUI(2), getHeight());
                }
                if (isFocusOwner()) {
                    g2.setColor(ACCENT);
                    g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
