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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import mekhq.gui.baseComponents.roundedComponents.AccentRoundedJButton.Accent;

class AccentRoundedJButtonTest {

    @Test
    void faceIsHazardRedAndLabelIsBold() {
        AccentRoundedJButton button = new AccentRoundedJButton("Report a Bug", Accent.HAZARD);

        assertEquals(Accent.HAZARD.getFace(), button.getBackground());
        assertTrue(button.getFont().isBold());
    }

    @Test
    void labelColourFollowsState() {
        AccentRoundedJButton button = new AccentRoundedJButton("Report a Bug", Accent.HAZARD);

        assertEquals(Accent.HAZARD.getLabel(), button.getForeground());

        button.getModel().setRollover(true);
        assertEquals(Accent.HAZARD.getLabelHover(), button.getForeground());

        button.getModel().setRollover(false);
        button.setEnabled(false);
        assertEquals(Accent.HAZARD.getLabelDisabled(), button.getForeground());
    }

    @Test
    void referenceAccentUsesItsOwnColours() {
        AccentRoundedJButton button = new AccentRoundedJButton("Glossary", Accent.REFERENCE);

        assertEquals(Accent.REFERENCE, button.getAccent());
        assertEquals(Accent.REFERENCE.getFace(), button.getBackground());
        assertEquals(Accent.REFERENCE.getLabel(), button.getForeground());
    }

    @Test
    void paintsRedFaceInsideYellowFrame() {
        AccentRoundedJButton button = new AccentRoundedJButton("Report a Bug", Accent.HAZARD);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setSize(120, 40);

        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }

        // A point well inside the face, away from the text, is the hazard red.
        assertEquals(Accent.HAZARD.getFace().getRGB(), image.getRGB(12, 20));
        // The middle of the top edge is the yellow frame.
        assertEquals(Accent.HAZARD.getFrame().getRGB(), image.getRGB(60, 1));
    }
}
