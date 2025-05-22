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
package mekhq.gui.dialog.reportDialogs.FactionStanding;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import megamek.client.ui.swing.util.UIUtil;

public class FactionStandingSlider extends JSlider {
    private static final Color SLIDER_GRAY = new Color(100, 100, 100);
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int HANDLE_HEIGHT = UIUtil.scaleForGUI(30);
    private static final int HANDLE_WIDTH = UIUtil.scaleForGUI(3);
    private static final int BAR_HEIGHT = UIUtil.scaleForGUI(3);

    private final int politicsValue;

    public FactionStandingSlider(int minimum, int maximum, int fameValue, int politicsValue) {
        super(minimum, maximum);
        setValue(fameValue);
        this.politicsValue = politicsValue;
        setUI(new InvisibleThumbSliderUI(this));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int y = getHeight() / 2;
        int trackStart = PADDING;
        int trackEnd = getWidth() - PADDING;
        int barWidth = trackEnd - trackStart;
        int arc = BAR_HEIGHT;

        g2.setColor(SLIDER_GRAY);
        g2.fill(new RoundRectangle2D.Double(trackStart, y - BAR_HEIGHT / 2, barWidth, BAR_HEIGHT, arc, arc));
        g2.dispose();

        // Draw custom handles
        paintThumb(g);
    }

    protected void paintThumb(Graphics graphics) {
        int fameX = valueToX(getValue());
        int politicsX = valueToX(politicsValue);

        paintHandle(graphics, fameX, SLIDER_GRAY, "<html><b>Fame</b></html>", true);
        paintHandle(graphics, politicsX, SLIDER_GRAY, "<html><b>Politics</b></html>", false);
    }

    private void paintHandle(Graphics graphics, int x, Color color, String label, boolean isLabelAbove) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int y = getHeight() / 2;
        int handleWidth = HANDLE_WIDTH;
        int handleTop = y - HANDLE_HEIGHT / 2;
        int arc = handleWidth;

        g2.setColor(color);
        g2.fillRoundRect(x, handleTop, handleWidth, HANDLE_HEIGHT, arc, arc);

        JLabel htmlLabel = new JLabel(label);
        htmlLabel.setForeground(SLIDER_GRAY);
        htmlLabel.setSize(handleWidth, HANDLE_HEIGHT);
        Dimension preferred = htmlLabel.getPreferredSize();

        int labelX = x + (handleWidth - preferred.width) / 2;
        int labelY = isLabelAbove ? handleTop - preferred.height - 2 : handleTop + HANDLE_HEIGHT + 2;

        htmlLabel.setBounds(0, 0, preferred.width, preferred.height);

        g2.translate(labelX, labelY);
        htmlLabel.paint(g2);
        g2.translate(-labelX, -labelY);

        g2.dispose();
    }

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
