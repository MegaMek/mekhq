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
package mekhq.gui.utilities;

import mekhq.MHQConstants;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.*;

/**
 * {@code RoundedLineBorder} is a custom border implementation for Swing components that draws a rectangular border with
 * rounded corners, a configurable color, thickness, and arc radius.
 *
 * <p>This border is suitable for modern UI designs that require rounded visual elements. It also provides a
 * convenience method to create a compound border with built-in padding.</p>
 *
 * @author Illiani
 * @since 0.50.7
 */
public class RoundedLineBorder extends AbstractBorder {
    private static final int PADDING = 10;

    private final Color color;
    private final int thickness;
    private final int arc;

    /**
     * Creates a {@link CompoundBorder} consisting of a {@code RoundedLineBorder} with default color, thickness, and
     * arc, combined with internal padding.
     *
     * @return a compound border with a rounded line border and padding.
     *
     * @author Illiani
     * @since 0.50.7
     */
    public static CompoundBorder createRoundedLineBorder() {
        Border rounded = new RoundedLineBorder(MHQConstants.BORDER_COLOR_GRAY, 2, 16);
        Border padding = BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING);

        return BorderFactory.createCompoundBorder(rounded, padding);
    }

    /**
     * Constructs a new {@code RoundedLineBorder} with the specified color, thickness, and arc radius.
     *
     * @param color     the color of the border's lines
     * @param thickness the thickness of the border's lines in pixels
     * @param arc       the arc (corner radius) of the border in pixels
     *
     * @author Illiani
     * @since 0.50.7
     */
    public RoundedLineBorder(final Color color, final int thickness, final int arc) {
        this.color = color;
        this.thickness = thickness;
        this.arc = arc;
    }

    /**
     * Paints the border for the specified component with rounded corners.
     *
     * @param component the component for which this border is being painted
     * @param graphics  the graphics context for painting
     * @param xPosition the x position of the painted border
     * @param yPosition the y position of the painted border
     * @param width     the width of the painted border
     * @param height    the height of the painted border
     *
     * @author Illiani
     * @since 0.50.7
     */
    @Override
    public void paintBorder(Component component, Graphics graphics, int xPosition, int yPosition, int width,
          int height) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();

        graphics2D.setColor(color);
        graphics2D.setStroke(new BasicStroke(thickness));
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int drawX = xPosition + thickness / 2;
        int drawY = yPosition + thickness / 2;
        int drawWidth = width - thickness;
        int drawHeight = height - thickness;
        graphics2D.drawRoundRect(drawX, drawY, drawWidth, drawHeight, arc, arc);

        graphics2D.dispose();
    }
}
