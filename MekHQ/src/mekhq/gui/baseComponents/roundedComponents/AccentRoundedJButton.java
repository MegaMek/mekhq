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

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.border.Border;

/**
 * A {@link RoundedJButton} painted in a fixed accent colour scheme instead of the theme's button colour, so that
 * one button stands out from the ordinary ones around it: a coloured face inside a contrasting rounded frame with a
 * bold label.
 *
 * <p>The {@link Accent#HAZARD} scheme is the MekHQ counterpart of MegaMek's {@code HazardButton}. MegaMek tints its
 * skinned button images; MekHQ buttons are drawn from plain colours, so this class sets the accent colours directly
 * and keeps the same rounded shape as every other {@link RoundedJButton}. Hover and pressed shades come from the
 * parent's painting, which brightens or darkens the face colour.</p>
 *
 * @author Illiani
 * @since 0.50.12
 */
public class AccentRoundedJButton extends RoundedJButton {

    /**
     * The colour schemes an {@link AccentRoundedJButton} can wear.
     */
    public enum Accent {
        /** Emergency-stop red and yellow, the same values as MegaMek's hazard widgets. Used for Report a Bug. */
        HAZARD(new Color(204, 34, 34), new Color(255, 204, 0),
              new Color(255, 221, 0), new Color(255, 245, 150), new Color(160, 110, 40)),
        /** Calm green, for reference material such as the Glossary. */
        REFERENCE(new Color(34, 120, 60), new Color(150, 220, 120),
              new Color(225, 255, 215), new Color(255, 255, 255), new Color(120, 160, 120));

        private final Color face;
        private final Color frame;
        private final Color label;
        private final Color labelHover;
        private final Color labelDisabled;

        Accent(Color face, Color frame, Color label, Color labelHover, Color labelDisabled) {
            this.face = face;
            this.frame = frame;
            this.label = label;
            this.labelHover = labelHover;
            this.labelDisabled = labelDisabled;
        }

        /** @return the colour the face is filled with when the button is idle */
        public Color getFace() {
            return face;
        }

        /** @return the colour of the rounded frame */
        public Color getFrame() {
            return frame;
        }

        /** @return the label colour when the button is idle */
        public Color getLabel() {
            return label;
        }

        /** @return the label colour when the button is hovered or focused */
        public Color getLabelHover() {
            return labelHover;
        }

        /** @return the label colour when the button is disabled */
        public Color getLabelDisabled() {
            return labelDisabled;
        }
    }

    private final Accent accent;

    /**
     * Creates an accent-coloured button with the given label.
     *
     * @param text   the button label
     * @param accent the colour scheme to paint it in
     */
    public AccentRoundedJButton(final String text, final Accent accent) {
        super(text);
        this.accent = accent;
        setFont(getFont().deriveFont(Font.BOLD));
        setBackground(accent.getFace());

        Border frame = new RoundedLineBorder(accent.getFrame(), THICKNESS, ARC);
        Border padding = BorderFactory.createEmptyBorder(VERTICAL_PADDING, HORIZONTAL_PADDING, VERTICAL_PADDING,
              HORIZONTAL_PADDING);
        setBorder(BorderFactory.createCompoundBorder(frame, padding));
    }

    /** @return the colour scheme this button wears */
    public Accent getAccent() {
        return accent;
    }

    /**
     * The label colour for the button's current state. The look and feel reads this when it draws the text, so the
     * label follows hover, focus and enabled state without any extra painting here.
     *
     * @return the accent's idle label colour, its hover colour on hover or focus, and its disabled colour when
     *       disabled
     */
    @Override
    public Color getForeground() {
        if (accent == null) {
            // Called from the superclass constructor, before this button's fields are set.
            return super.getForeground();
        }
        if (!isEnabled()) {
            return accent.getLabelDisabled();
        }
        if ((getModel() != null) && (getModel().isRollover() || hasFocus())) {
            return accent.getLabelHover();
        }
        return accent.getLabel();
    }
}
