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

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jakarta.annotation.Nullable;

/**
 * A segmented, mutually exclusive choice. Each segment has a tracked title and an optional sub-line that explains its
 * effect (for example, how much money the choice makes or costs). The selected segment is raised with the map's
 * accent underline; disabled segments are dimmed and ignore clicks.
 *
 * <p>Setting the segments or the selection never fires the listener; only the player's choices do.</p>
 *
 * @param <T> the type of value each segment stands for
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudSegmentedControl<T> extends JPanel {
    private final transient Consumer<T> onChoose;
    private final List<Cell> cells = new ArrayList<>();
    private transient @Nullable T selected;

    /**
     * One segment of the control.
     *
     * @param value   the value the segment stands for
     * @param title   the segment's title
     * @param sub     the segment's sub-line, or {@code null} for none
     * @param enabled {@code true} if the player may choose the segment
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Segment<T>(T value, String title, @Nullable String sub, boolean enabled) {}

    /**
     * @param onChoose called with the segment's value when the player chooses a segment
     */
    public HudSegmentedControl(Consumer<T> onChoose) {
        this.onChoose = onChoose;
        setOpaque(true);
        setBackground(SURFACE_DEEP);
        setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
    }

    /**
     * Replaces the control's segments, keeping the selection if its value is still offered.
     *
     * @param segments the segments, left to right
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setSegments(List<Segment<T>> segments) {
        removeAll();
        cells.clear();
        setLayout(new GridLayout(1, Math.max(1, segments.size()), 0, 0));
        for (int index = 0; index < segments.size(); index++) {
            Cell cell = new Cell(segments.get(index), index < segments.size() - 1);
            cells.add(cell);
            add(cell);
        }
        setSelected(selected);
        revalidate();
        repaint();
    }

    /**
     * Selects the segment for a value, without notifying the listener.
     *
     * @param value the value to select, or {@code null} to select nothing
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setSelected(@Nullable T value) {
        selected = value;
        for (Cell cell : cells) {
            cell.refresh();
        }
    }

    /**
     * @return the selected value, or {@code null} if nothing is selected
     */
    public @Nullable T getSelected() {
        return selected;
    }

    private final class Cell extends JPanel {
        private final Segment<T> segment;
        private final boolean rightBorder;
        private final JLabel title;
        private final @Nullable JLabel sub;
        private boolean hovered;

        private Cell(Segment<T> segment, boolean rightBorder) {
            this.segment = segment;
            this.rightBorder = rightBorder;
            setOpaque(false);
            setFocusable(segment.enabled());
            setLayout(new GridBagLayout());
            int vertical = scaleForGUI(6);
            setBorder(BorderFactory.createEmptyBorder(vertical, scaleForGUI(6), vertical, scaleForGUI(6)));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            title = new JLabel(segment.title().toUpperCase(Locale.ROOT), SwingConstants.CENTER);
            title.setFont(hudFont(Font.BOLD, 0.78f, 0.1f));
            add(title, constraints);

            if (segment.sub() != null) {
                constraints.gridy = 1;
                sub = new JLabel(segment.sub(), SwingConstants.CENTER);
                sub.setFont(hudFont(Font.PLAIN, 0.74f, 0.0f));
                add(sub, constraints);
            } else {
                sub = null;
            }

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (contains(event.getPoint())) {
                        choose();
                    }
                }

                @Override
                public void mouseEntered(MouseEvent event) {
                    hovered = segment.enabled();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
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
            refresh();
        }

        private boolean isSelectedCell() {
            return Objects.equals(selected, segment.value());
        }

        private void choose() {
            if (!segment.enabled()) {
                return;
            }
            requestFocusInWindow();
            selected = segment.value();
            for (Cell cell : cells) {
                cell.refresh();
            }
            onChoose.accept(segment.value());
        }

        private void refresh() {
            if (!segment.enabled()) {
                title.setForeground(TEXT_FAINT);
            } else {
                title.setForeground(isSelectedCell() ? ACCENT_BRIGHT : (hovered ? TEXT : TEXT_MUTED));
            }
            if (sub != null) {
                sub.setForeground(segment.enabled() ? TEXT_MUTED : TEXT_FAINT);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                boolean isSelectedCell = isSelectedCell();
                g2.setColor(isSelectedCell ? SURFACE_HIGHLIGHT : (hovered ? SURFACE : SURFACE_DEEP));
                g2.fillRect(0, 0, width, height);
                if (rightBorder) {
                    g2.setColor(BORDER);
                    g2.fillRect(width - scaleForGUI(1), 0, scaleForGUI(1), height);
                }
                if (isSelectedCell) {
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
