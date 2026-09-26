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

import megamek.common.annotations.Nullable;

/**
 * A segmented, mutually exclusive choice. Each segment has a tracked title and an optional sub-line that explains its
 * effect (for example, how much money the choice makes or costs). The selected segment is raised with the map's
 * accent underline; disabled segments are dimmed and ignore clicks.
 *
 * <p>Setting the segments or the selection never fires the listener; only the player's choices do.</p>
 *
 * <p>This is also the one implementation behind {@link HudTabStrip} and {@link HudModeSelector}, which differ only in
 * their {@link SegmentStyle}, so a change to how segments are drawn or handled is made here once.</p>
 *
 * @param <T> the type of value each segment stands for
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudSegmentedControl<T> extends JPanel {
    /** The look of a stand-alone choice, such as the salvage console's claim and recovery method controls. */
    static final SegmentStyle SEGMENTED = new SegmentStyle(6, 0.78f, 0.1f, true, SURFACE_HIGHLIGHT, SURFACE,
          ACCENT_BRIGHT, true, false);
    /** The look of a page tab strip. */
    static final SegmentStyle TABS = new SegmentStyle(10, 0.85f, 0.06f, true, SURFACE, SURFACE_HIGHLIGHT, TEXT,
          false, true);
    /** The smaller, lighter look of a second level of tabs. */
    static final SegmentStyle COMPACT_TABS = new SegmentStyle(7, 0.76f, 0.1f, true, SURFACE, SURFACE_HIGHLIGHT, TEXT,
          false, true);
    /** The look of a page selector whose pages are the values of an enum; its labels keep their own case. */
    static final SegmentStyle MODES = new SegmentStyle(10, 0.85f, 0.06f, false, SURFACE, SURFACE_HIGHLIGHT, TEXT,
          false, true);

    private final transient Consumer<T> onChoose;
    private final transient SegmentStyle style;
    private final List<Cell> cells = new ArrayList<>();
    /** The chosen value, or {@code null} while nothing is chosen. */
    private transient T selected;

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
     * How a family of segmented controls looks and behaves.
     *
     * @param verticalPadding         the padding above and below each segment's text, before GUI scaling
     * @param titleFontScale          the title font size, relative to the label font
     * @param titleTracking           the title letter spacing
     * @param isTitleUppercase        {@code true} to show titles in upper case
     * @param selectedBackground      the background of the selected segment
     * @param hoveredBackground       the background of a segment under the pointer
     * @param selectedTitleColor      the title colour of the selected segment
     * @param isSelectedOnChoose      {@code true} if choosing a segment selects it; {@code false} if the owner selects
     *                                it from the listener
     * @param isDisabledCellFocusable {@code true} if a disabled segment can still take the keyboard focus
     */
    record SegmentStyle(int verticalPadding, float titleFontScale, float titleTracking, boolean isTitleUppercase,
          Color selectedBackground, Color hoveredBackground, Color selectedTitleColor, boolean isSelectedOnChoose,
          boolean isDisabledCellFocusable) {}

    /**
     * @param onChoose called with the segment's value when the player chooses a segment
     */
    public HudSegmentedControl(Consumer<T> onChoose) {
        this(onChoose, SEGMENTED);
    }

    /**
     * @param onChoose called with the segment's value when the player chooses a segment
     * @param style    how the segments look and behave
     */
    HudSegmentedControl(Consumer<T> onChoose, SegmentStyle style) {
        this.onChoose = onChoose;
        this.style = style;
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
        refreshCells();
    }

    /**
     * @return the selected value, or {@code null} if nothing is selected
     */
    public @Nullable T getSelected() {
        return selected;
    }

    /**
     * Locks or unlocks the segment for a value. A locked segment is dimmed and ignores clicks and keys.
     *
     * @param value   the segment's value; does nothing if no segment has it
     * @param enabled {@code true} to let the player choose the segment
     */
    public void setSegmentEnabled(T value, boolean enabled) {
        for (Cell cell : cells) {
            if (Objects.equals(cell.value, value)) {
                cell.setCellEnabled(enabled);
            }
        }
    }

    /**
     * Replaces the title of the segment for a value, for example to show a count.
     *
     * @param value the segment's value; does nothing if no segment has it
     * @param title the new title
     */
    public void setSegmentTitle(T value, String title) {
        for (Cell cell : cells) {
            if (Objects.equals(cell.value, value)) {
                cell.title.setText(titleText(title));
            }
        }
    }

    private String titleText(String title) {
        return style.isTitleUppercase() ? title.toUpperCase(Locale.ROOT) : title;
    }

    private void refreshCells() {
        for (Cell cell : cells) {
            cell.refresh();
        }
    }

    private final class Cell extends JPanel {
        private final transient T value;
        private final boolean hasRightBorder;
        private final JLabel title;
        /** The sub-line under the title, or {@code null} for a segment without one. */
        private final JLabel sub;
        private boolean isCellEnabled;
        private boolean isHovered;

        private Cell(Segment<T> segment, boolean hasRightBorder) {
            this.value = segment.value();
            this.hasRightBorder = hasRightBorder;
            this.isCellEnabled = segment.enabled();
            setOpaque(false);
            setFocusable(style.isDisabledCellFocusable() || isCellEnabled);
            setLayout(new GridBagLayout());
            int vertical = scaleForGUI(style.verticalPadding());
            setBorder(BorderFactory.createEmptyBorder(vertical, scaleForGUI(6), vertical, scaleForGUI(6)));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            title = new JLabel(titleText(segment.title()), SwingConstants.CENTER);
            title.setFont(hudFont(Font.BOLD, style.titleFontScale(), style.titleTracking()));
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
                    isHovered = isCellEnabled;
                    refresh();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    isHovered = false;
                    refresh();
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
            return Objects.equals(selected, value);
        }

        private void choose() {
            if (!isCellEnabled) {
                return;
            }
            requestFocusInWindow();
            if (style.isSelectedOnChoose()) {
                selected = value;
                refreshCells();
            }
            onChoose.accept(value);
        }

        private void setCellEnabled(boolean isCellEnabled) {
            this.isCellEnabled = isCellEnabled;
            if (!isCellEnabled) {
                isHovered = false;
            }
            if (!style.isDisabledCellFocusable()) {
                setFocusable(isCellEnabled);
            }
            refresh();
        }

        private void refresh() {
            if (!isCellEnabled) {
                title.setForeground(TEXT_FAINT);
            } else if (isSelectedCell()) {
                title.setForeground(style.selectedTitleColor());
            } else {
                title.setForeground(isHovered ? TEXT : TEXT_MUTED);
            }
            if (sub != null) {
                sub.setForeground(isCellEnabled ? TEXT_MUTED : TEXT_FAINT);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                boolean isSelectedCell = isSelectedCell();
                Color background = SURFACE_DEEP;
                if (isSelectedCell) {
                    background = style.selectedBackground();
                } else if (isHovered) {
                    background = style.hoveredBackground();
                }
                graphics2D.setColor(background);
                graphics2D.fillRect(0, 0, width, height);
                if (hasRightBorder) {
                    graphics2D.setColor(BORDER);
                    graphics2D.fillRect(width - scaleForGUI(1), 0, scaleForGUI(1), height);
                }
                if (isSelectedCell) {
                    int underline = scaleForGUI(2);
                    graphics2D.setColor(ACCENT);
                    graphics2D.fillRect(0, height - underline, width, underline);
                }
            } finally {
                graphics2D.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
