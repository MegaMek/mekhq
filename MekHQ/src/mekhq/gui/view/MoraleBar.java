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
package mekhq.gui.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.mission.enums.AtBMoraleLevel;

/**
 * A compact, segmented gauge that visualizes enemy {@link AtBMoraleLevel}.
 *
 * <p>
 * The bar has one segment per possible morale level, ordered from the morale
 * scale's minimum to its maximum. Each
 * segment is colored on a fixed green-to-red gradient and the colors are
 * presented from the <em>player's</em>
 * perspective: low enemy morale (e.g. {@code ROUTED}) is green because it is
 * favourable for the player, while high
 * enemy morale (e.g. {@code OVERWHELMING}) is red because it is dangerous.
 * Segments up to and including the current
 * morale level are drawn at full strength; the remaining segments are faded, so
 * the lit length communicates how high
 * the enemy's morale currently is while the lit "tip" colour communicates its
 * severity.
 * </p>
 *
 * @author The MegaMek Team
 */
public class MoraleBar extends JComponent {
    private static final int SEGMENT_COUNT = AtBMoraleLevel.MAXIMUM_MORALE_LEVEL - AtBMoraleLevel.MINIMUM_MORALE_LEVEL
            + 1;
    private static final int FADED_ALPHA = 55;
    private static final int BORDER_ALPHA = 60;

    private AtBMoraleLevel moraleLevel;

    /**
     * Creates a morale bar for the given morale level.
     *
     * @param moraleLevel the enemy morale level to display; may be {@code null}, in
     *                    which case nothing is painted
     */
    public MoraleBar(final @Nullable AtBMoraleLevel moraleLevel) {
        this.moraleLevel = moraleLevel;
        setOpaque(false);
    }

    /**
     * Updates the displayed morale level and repaints the bar.
     *
     * @param moraleLevel the new enemy morale level to display
     */
    public void setMoraleLevel(final @Nullable AtBMoraleLevel moraleLevel) {
        this.moraleLevel = moraleLevel;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return UIUtil.scaleForGUI(140, 14);
    }

    @Override
    public Dimension getMinimumSize() {
        return UIUtil.scaleForGUI(84, 10);
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (moraleLevel == null) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int width = getWidth();
            final int height = getHeight();
            final int gap = Math.max(UIUtil.scaleForGUI(2), 1);
            final int arc = Math.max(UIUtil.scaleForGUI(4), 2);
            final int totalGap = gap * (SEGMENT_COUNT - 1);
            final int segmentWidth = Math.max((width - totalGap) / SEGMENT_COUNT, 1);
            final int usedWidth = segmentWidth * SEGMENT_COUNT + totalGap;
            final int xStart = Math.max((width - usedWidth) / 2, 0);

            final int filledIndex = moraleLevel.getLevel() - AtBMoraleLevel.MINIMUM_MORALE_LEVEL;

            for (int i = 0; i < SEGMENT_COUNT; i++) {
                final int x = xStart + i * (segmentWidth + gap);
                final Color base = segmentColor(i);

                if (i <= filledIndex) {
                    g2.setColor(base);
                } else {
                    g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), FADED_ALPHA));
                }
                g2.fillRoundRect(x, 0, segmentWidth, height, arc, arc);

                g2.setColor(new Color(0, 0, 0, BORDER_ALPHA));
                g2.drawRoundRect(x, 0, segmentWidth - 1, height - 1, arc, arc);
            }

            // Outline the current level so the exact morale value is unambiguous.
            final int markerX = xStart + filledIndex * (segmentWidth + gap);
            Color markerColor = UIManager.getColor("Label.foreground");
            if (markerColor == null) {
                markerColor = Color.WHITE;
            }
            g2.setColor(markerColor);
            g2.setStroke(new BasicStroke(Math.max(UIUtil.scaleForGUI(2), 1)));
            g2.drawRoundRect(markerX, 0, segmentWidth - 1, height - 1, arc, arc);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Computes the fixed gradient color for the segment at the given index,
     * interpolating the hue from green (favorable
     * for the player) at the lowest morale to red (dangerous for the player) at the
     * highest.
     *
     * @param index the zero-based segment index, from lowest to highest morale
     *
     * @return the color for the segment
     */
    private static Color segmentColor(final int index) {
        final float fraction = (SEGMENT_COUNT <= 1) ? 0f : (float) index / (SEGMENT_COUNT - 1);
        final float hue = (120f * (1f - fraction)) / 360f; // 120 degrees (green) down to 0 degrees (red)
        return Color.getHSBColor(hue, 0.85f, 0.80f);
    }
}
