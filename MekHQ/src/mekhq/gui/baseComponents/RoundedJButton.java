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
package mekhq.gui.baseComponents;

import mekhq.MHQConstants;
import mekhq.gui.utilities.RoundedLineBorder;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * {@link RoundedJButton} is a custom {@link JButton} implementation that renders a button with rounded corners and a
 * customizable border. It sets up its look and feel to complement modern UI design standards and supports custom text,
 * background highlighting on hover and press, and smooth antialiased drawing.
 *
 * @author Illiani
 * @since 0.50.07
 */
public class RoundedJButton extends JButton {
    /**
     * The padding between the button border and its contents, in pixels.
     */
    private static final int VERTICAL_PADDING = 5;
    private static final int HORIZONTAL_PADDING = 5;

    /**
     * The thickness of the border, in pixels.
     */
    private static final int THICKNESS = 2;

    /**
     * The arc diameter for the button's rounded corners, in pixels.
     */
    private static final int ARC = 16;

    /**
     * Constructs a default {@link RoundedJButton} with no text.
     *
     * <p>Sets the content area to be non-filled, disables focus painting, and sets a compound border with a rounded
     * border and padding.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public RoundedJButton() {
        super();
        setContentAreaFilled(false);
        setFocusPainted(false);
        RoundedLineBorder roundedBorder = new RoundedButtonBorder();
        Border paddingBorder = BorderFactory.createEmptyBorder(VERTICAL_PADDING,
                HORIZONTAL_PADDING,
                VERTICAL_PADDING,
                HORIZONTAL_PADDING);
        setBorder(BorderFactory.createCompoundBorder(roundedBorder, paddingBorder));
    }

    /**
     * Constructs a {@link RoundedJButton} with the specified text label.
     *
     * <p>Sets the content area to be non-filled, disables focus painting, and sets a compound border with a rounded
     * border and padding.</p>
     *
     * @param text the text label for the button
     * @author Illiani
     * @since 0.50.07
     */
    public RoundedJButton(final String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        RoundedLineBorder roundedBorder = new RoundedButtonBorder();
        Border paddingBorder = BorderFactory.createEmptyBorder(VERTICAL_PADDING,
                HORIZONTAL_PADDING,
                VERTICAL_PADDING,
                HORIZONTAL_PADDING);
        setBorder(BorderFactory.createCompoundBorder(roundedBorder, paddingBorder));
    }

    /**
     * Paints the component with rounded corners and background color depending on the button state (normal, pressed,
     * rollover, or disabled).
     *
     * <p>Uses antialiasing for smooth rendering.</p>
     *
     * @param graphics the {@link Graphics} context in which to paint
     * @author Illiani
     * @since 0.50.07
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();

        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (getModel().isPressed()) {
            graphics2D.setColor(getBackground().darker());
        } else if (getModel().isRollover()) {
            graphics2D.setColor(getBackground().brighter());
        } else if (!getModel().isEnabled()) {
            graphics2D.setColor(getBackground().darker());
        } else {
            graphics2D.setColor(getBackground());
        }

        int width = getWidth();
        int height = getHeight();
        graphics2D.fillRoundRect(0, 0, width, height, ARC, ARC);

        graphics2D.dispose();
        super.paintComponent(graphics);
    }

    /**
     * {@link RoundedButtonBorder} is a subclass of {@link RoundedLineBorder} that defines a border with a specific
     * color, thickness, and corner arc.
     *
     * <p>Intended to be used as the border for {@link RoundedJButton}.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static class RoundedButtonBorder extends RoundedLineBorder {
        /**
         * Constructs a new {@link RoundedButtonBorder} with predefined color, thickness, and corner arc.
         *
         * @author Illiani
         * @since 0.50.07
         */
        public RoundedButtonBorder() {
            super(MHQConstants.BORDER_COLOR_GRAY, THICKNESS, ARC);
        }
    }
}
