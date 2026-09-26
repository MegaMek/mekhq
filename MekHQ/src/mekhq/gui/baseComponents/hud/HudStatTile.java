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
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.annotations.Nullable;

/**
 * A deep-surface stat tile - a small key, a large coloured value, and a faint sub-line - matching the debrief
 * console's tiles. The value and sub-line can be updated live, and the tile can show a meter: a fill bar with an
 * optional threshold mark, for budgets such as tech time or a salvage cap.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudStatTile extends JPanel {
    private final JLabel valueLabel = new JLabel();
    private final JLabel subLabel = new JLabel();
    private final Meter meter = new Meter();

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
        add(leftAligned(valueLabel));

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
        meter.fill = Math.clamp(fill, 0.0, 1.0);
        meter.fillColor = fillColor;
        meter.threshold = (threshold == null) ? null : Math.clamp(threshold, 0.0, 1.0);
        meter.setVisible(true);
        meter.repaint();
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

    /** A thin fill bar with an optional amber threshold mark. */
    private static final class Meter extends JComponent {
        private double fill;
        private Color fillColor = ACCENT;
        /** Where the threshold mark sits (0-1), or {@code null} for no mark. */
        private Double threshold;

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
                if (threshold != null) {
                    int markWidth = scaleForGUI(2);
                    int markX = Math.min(width - markWidth, (int) Math.round(width * threshold));
                    g2.setColor(AMBER);
                    g2.fillRect(markX, 0, markWidth, getHeight());
                }
            } finally {
                g2.dispose();
            }
        }
    }
}
