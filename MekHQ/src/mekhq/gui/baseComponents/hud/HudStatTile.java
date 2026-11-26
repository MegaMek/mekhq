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
import static mekhq.gui.baseComponents.hud.HudStyle.AMBER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_HIGHLIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;

/**
 * A deep-surface stat tile - a small key, a large coloured value, and a faint sub-line - matching the debrief
 * console's tiles. The value and sub-line can be updated live, and the tile can show a meter: a fill bar with
 * optional threshold marks, for budgets such as tech time or a salvage cap.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudStatTile extends JPanel {
    private final JLabel valueLabel = new JLabel();
    private final JLabel subLabel = new JLabel();
    private final Meter meter = new Meter();
    private final JPanel valueRow = new JPanel();

    /**
     * @param key the tile's caption
     * @param sub the tile's sub-line
     */
    public HudStatTile(String key, String sub) {
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
        valueRow.setOpaque(false);
        valueRow.setLayout(new BoxLayout(valueRow, BoxLayout.X_AXIS));
        valueRow.add(valueLabel);
        valueRow.add(Box.createHorizontalGlue());
        add(leftAligned(valueRow));

        // Hidden until a meter is set, so tiles without one keep the debrief console's spacing
        meter.setVisible(false);
        add(leftAligned(meter));

        add(Box.createVerticalStrut(scaleForGUI(3)));
        subLabel.setForeground(TEXT_FAINT);
        subLabel.setFont(hudFont(Font.PLAIN, 0.78f, 0.0f));
        subLabel.setText(sub);
        add(leftAligned(subLabel));
    }

    /**
     * Sets the tile's value.
     *
     * @param value the value text
     * @param color the value's colour
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setValue(String value, Color color) {
        valueLabel.setText(value);
        valueLabel.setForeground(color);
    }

    /**
     * Sets the tile's sub-line.
     *
     * @param sub the sub-line text
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setSub(String sub) {
        subLabel.setText(sub);
    }

    /**
     * Shows the tile's meter.
     *
     * @param fill      how full the meter is, from 0 to 1 (values outside that range are clamped)
     * @param fillColor the fill's colour
     * @param threshold where to draw a threshold mark, from 0 to 1, or {@code null} for none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setMeter(double fill, Color fillColor, @Nullable Double threshold) {
        setMeterWithMarks(fill, fillColor, (threshold == null) ? List.of() : List.of(new MeterMark(threshold, AMBER)));
    }

    /**
     * Shows the tile's meter with any number of threshold marks, each in its own colour.
     *
     * @param fill      how full the meter is, from 0 to 1 (values outside that range are clamped)
     * @param fillColor the fill's colour
     * @param marks     the threshold marks to draw, in drawing order; empty for none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setMeterWithMarks(double fill, Color fillColor, List<MeterMark> marks) {
        meter.fill = Math.clamp(fill, 0.0, 1.0);
        meter.fillColor = fillColor;
        List<MeterMark> clampedMarks = new ArrayList<>();
        for (MeterMark mark : marks) {
            clampedMarks.add(new MeterMark(Math.clamp(mark.position(), 0.0, 1.0), mark.color()));
        }
        meter.marks = clampedMarks;
        meter.setVisible(true);
        meter.repaint();
    }

    /**
     * A threshold mark on a tile's meter.
     *
     * @param position where the mark sits, from 0 to 1
     * @param color    the mark's colour
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record MeterMark(double position, Color color) {}

    /**
     * Places a control to the right of the value, such as a stepper for adjusting it.
     *
     * @param accessory the control
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setValueAccessory(JComponent accessory) {
        valueRow.add(accessory);
        revalidate();
    }

    /**
     * Adds a component beneath the sub-line, such as a link to a related action.
     *
     * @param footer the component
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void addFooter(JComponent footer) {
        add(Box.createVerticalStrut(scaleForGUI(3)));
        add(leftAligned(footer));
        revalidate();
    }

    /**
     * Hides the tile's meter.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void hideMeter() {
        meter.setVisible(false);
    }

    /** A floor on the width, so a row of tiles does not shift as the values grow and shrink. */
    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        return new Dimension(Math.max(preferred.width, scaleForGUI(150)), preferred.height);
    }

    /** A thin fill bar with optional threshold marks. */
    private static final class Meter extends JComponent {
        private double fill;
        private Color fillColor = ACCENT;
        /** The threshold marks, with positions already clamped to 0-1. */
        private List<MeterMark> marks = List.of();

        private Meter() {
            Dimension size = new Dimension(Short.MAX_VALUE, scaleForGUI(10));
            setPreferredSize(new Dimension(scaleForGUI(120), scaleForGUI(10)));
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int barHeight = scaleForGUI(4);
                int barTop = (getHeight() - barHeight) / 2;
                g2.setColor(SURFACE_HIGHLIGHT);
                g2.fillRect(0, barTop, width, barHeight);
                g2.setColor(fillColor);
                g2.fillRect(0, barTop, (int) Math.round(width * fill), barHeight);
                int markWidth = scaleForGUI(2);
                for (MeterMark mark : marks) {
                    int markX = Math.min(width - markWidth, (int) Math.round(width * mark.position()));
                    g2.setColor(mark.color());
                    g2.fillRect(markX, 0, markWidth, getHeight());
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
