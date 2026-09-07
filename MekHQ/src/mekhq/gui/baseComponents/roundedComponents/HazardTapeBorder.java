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
package mekhq.gui.baseComponents.roundedComponents;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.TexturePaint;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.border.AbstractBorder;

import megamek.client.ui.util.UIUtil;

/**
 * A rounded border striped like warning tape: diagonal bars alternating between two colours, run around the
 * component instead of a plain line.
 *
 * <p>Used for the one button a player must not walk past. A solid frame reads as decoration at a glance;
 * hazard tape reads as "this is the thing", which is the whole point of the button it frames.</p>
 *
 * <p>The stripes are painted as a repeating tile, so they stay the same width whatever size the component is,
 * and they run in one continuous direction around all four sides rather than mirroring at the corners.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HazardTapeBorder extends AbstractBorder {

    /** Width of one light-plus-dark pair, before GUI scaling. Wide enough for the diagonal to read as tape. */
    private static final int STRIPE_PERIOD = 10;

    private final Color lightStripe;
    private final Color darkStripe;
    private final int thickness;
    private final int arc;

    /**
     * Creates a hazard-tape border.
     *
     * @param lightStripe the lighter of the two bars, typically the same yellow as the face it frames
     * @param darkStripe  the darker bar
     * @param thickness   how wide the tape is, before GUI scaling
     * @param arc         corner radius, matching the component's own rounding
     */
    public HazardTapeBorder(Color lightStripe, Color darkStripe, int thickness, int arc) {
        this.lightStripe = lightStripe;
        this.darkStripe = darkStripe;
        this.thickness = thickness;
        this.arc = arc;
    }

    /**
     * Builds the repeating tile the tape is painted with: one light bar and one dark bar, running at 45 degrees.
     *
     * <p>The dark bar is drawn three times, one period to each side of the tile as well as across it, so the
     * diagonal continues across tile edges instead of stopping at them.</p>
     *
     * @param period the tile's size in device pixels, one full light-plus-dark pair
     *
     * @return the paint to stroke the outline with
     */
    private TexturePaint stripeTile(int period) {
        BufferedImage tile = new BufferedImage(period, period, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tileGraphics = tile.createGraphics();
        try {
            tileGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            tileGraphics.setColor(lightStripe);
            tileGraphics.fillRect(0, 0, period, period);

            tileGraphics.setColor(darkStripe);
            tileGraphics.setStroke(new BasicStroke(period / 2.0f));
            for (int offset = -period; offset <= period; offset += period) {
                tileGraphics.drawLine(offset, 0, offset + period, period);
            }
        } finally {
            tileGraphics.dispose();
        }
        return new TexturePaint(tile, new Rectangle(0, 0, period, period));
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int xPosition, int yPosition, int width,
          int height) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        try {
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int tapeWidth = UIUtil.scaleForGUI(thickness);
            int scaledArc = UIUtil.scaleForGUI(arc);
            graphics2D.setPaint(stripeTile(Math.max(2, UIUtil.scaleForGUI(STRIPE_PERIOD))));
            graphics2D.setStroke(new BasicStroke(tapeWidth));

            // Inset by half the tape so the whole width lands inside the component rather than half outside it.
            double half = tapeWidth / 2.0;
            graphics2D.draw(new RoundRectangle2D.Double(xPosition + half, yPosition + half,
                  width - tapeWidth, height - tapeWidth, scaledArc, scaledArc));
        } finally {
            graphics2D.dispose();
        }
    }

    @Override
    public Insets getBorderInsets(Component component) {
        int tapeWidth = UIUtil.scaleForGUI(thickness);
        return new Insets(tapeWidth, tapeWidth, tapeWidth, tapeWidth);
    }

    @Override
    public Insets getBorderInsets(Component component, Insets insets) {
        int tapeWidth = UIUtil.scaleForGUI(thickness);
        insets.set(tapeWidth, tapeWidth, tapeWidth, tapeWidth);
        return insets;
    }

    @Override
    public boolean isBorderOpaque() {
        return false;
    }
}
