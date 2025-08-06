/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog.factionStanding;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import megamek.client.ui.util.UIUtil;

/**
 * A custom {@link JSlider} used to visually represent faction standing levels with labeled "Regard" and "Climate"
 * slider handles. The slider offers a modern, minimal style and is designed to avoid the default thumb UI for a sleeker
 * look in faction standing reports.
 *
 * <p>The slider supports two distinct handles, one for "Regard" (the current value) and one for "Climate" (a
 * comparison/reference value). Both are rendered as custom vertical bars with HTML labels placed above or below the
 * handle.</p>
 *
 * <p>This class is intended for use in {@link FactionStandingReport} but the base code can probably be adapted for use
 * elsewhere.</p>}
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandingSlider extends JSlider {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    private static final Color SLIDER_GRAY = new Color(100, 100, 100);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int HANDLE_HEIGHT = UIUtil.scaleForGUI(30);
    private static final int HANDLE_WIDTH = UIUtil.scaleForGUI(3);
    private static final int BAR_HEIGHT = UIUtil.scaleForGUI(3);

    private final int climateValue;


    /**
     * Constructs a FactionStandingSlider with the specified range and values.
     *
     * @param minimum      the minimum value of the slider (inclusive)
     * @param maximum      the maximum value of the slider (inclusive)
     * @param regardValue  the current "Regard" standing to show (slider value)
     * @param climateValue the reference "Climate" standing to display
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandingSlider(int minimum, int maximum, int regardValue, int climateValue) {
        super(minimum, maximum);
        setValue(regardValue);
        this.climateValue = climateValue;
        setUI(new InvisibleThumbSliderUI(this));
    }

    /**
     * Paints the custom slider component, including the bar and handles.
     *
     * <p>This method overrides default painting to implement a custom, anti-aliased rounded slider bar and invokes
     * handle painting.</p>
     *
     * @param graphics the {@link Graphics} context for painting
     *
     * @author Illiani
     * @since 0.50.07
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int y = getHeight() / 2;
        int trackStart = PADDING;
        int trackEnd = getWidth() - PADDING;
        int barWidth = trackEnd - trackStart;
        int arc = BAR_HEIGHT;

        graphics2D.setColor(SLIDER_GRAY);
        graphics2D.fill(new RoundRectangle2D.Double(trackStart,
              y - (double) BAR_HEIGHT / 2,
              barWidth,
              BAR_HEIGHT,
              arc,
              arc));
        graphics2D.dispose();

        // Draw custom handles
        paintThumb(graphics);
    }

    /**
     * Paints both the "Regard" and "Climate" handles with their labels.
     *
     * @param graphics the {@link Graphics} context for painting
     *
     * @author Illiani
     * @since 0.50.07
     */
    protected void paintThumb(Graphics graphics) {
        int regardX = valueToX(getValue());
        int climateX = valueToX(climateValue);

        paintHandle(graphics, regardX, getTextAt(RESOURCE_BUNDLE, "factionStandingSlider.label.regard"), true);
        paintHandle(graphics, climateX, getTextAt(RESOURCE_BUNDLE, "factionStandingSlider.label.climate"), false);
    }

    /**
     * Paints an individual handle as a vertical rounded bar and labels it with the specified HTML string, placing the
     * label above or below.
     *
     * @param graphics     the {@link Graphics} context
     * @param x            the x-coordinate for the handle's center
     * @param label        the HTML label for the handle
     * @param isLabelAbove true if the label should appear above the handle, false for below
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void paintHandle(Graphics graphics, int x, String label, boolean isLabelAbove) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int y = getHeight() / 2;
        int handleWidth = HANDLE_WIDTH;
        int handleTop = y - HANDLE_HEIGHT / 2;
        int arc = handleWidth;

        graphics2D.setColor(SLIDER_GRAY);
        graphics2D.fillRoundRect(x, handleTop, handleWidth, HANDLE_HEIGHT, arc, arc);

        JLabel htmlLabel = new JLabel(label);
        htmlLabel.setForeground(SLIDER_GRAY);
        htmlLabel.setSize(handleWidth, HANDLE_HEIGHT);
        Dimension preferred = htmlLabel.getPreferredSize();

        int labelX = x + (handleWidth - preferred.width) / 2;
        int labelY = isLabelAbove ? handleTop - preferred.height - 2 : handleTop + HANDLE_HEIGHT + 2;

        htmlLabel.setBounds(0, 0, preferred.width, preferred.height);

        graphics2D.translate(labelX, labelY);
        htmlLabel.paint(graphics2D);
        graphics2D.translate(-labelX, -labelY);

        graphics2D.dispose();
    }

    /**
     * Converts a standing value into its respective x-coordinate on the slider track, accounting for padding and slider
     * range.
     *
     * @param value the value to convert
     *
     * @return the x-coordinate on the slider
     *
     * @author Illiani
     * @since 0.50.07
     */
    private int valueToX(int value) {
        int min = getMinimum();
        int max = getMaximum();
        if (max == min) {
            return PADDING; // Avoids division by zero
        }
        double percent = (double) (value - min) / (max - min);
        int trackLength = getWidth() - 2 * PADDING;
        return (int) (PADDING + percent * trackLength);
    }

    /**
     * Custom UI that prevents the default JSlider thumb from being painted, used to allow for entirely custom rendering
     * of the slider.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static class InvisibleThumbSliderUI extends BasicSliderUI {
        public InvisibleThumbSliderUI(JSlider slider) {
            super(slider);
        }

        @Override
        public void paintThumb(Graphics g) {
            // Suppress default thumb
        }
    }
}
