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

    @Test
    void cautionAccentUsesItsOwnColours() {
        AccentRoundedJButton button = new AccentRoundedJButton("Generate Starting Command", Accent.CAUTION);

        assertEquals(Accent.CAUTION, button.getAccent());
        assertEquals(Accent.CAUTION.getFace(), button.getBackground());
        assertEquals(Accent.CAUTION.getLabel(), button.getForeground());
    }

    @Test
    void cautionIsTheHazardYellowSoTheTwoReadAsOnePalette() {
        assertEquals(Accent.HAZARD.getFrame(), Accent.CAUTION.getFace(),
              "the caution face is the same yellow the hazard button is framed in");
    }

    @Test
    void paintsYellowFaceInsideAFrameOfHazardTape() {
        // A short label, so the sampled face pixel cannot land on an antialiased glyph. The caution label is
        // the same near-black as its dark stripe, which would otherwise read as a stripe hit.
        AccentRoundedJButton button = new AccentRoundedJButton("Go", Accent.CAUTION);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setSize(120, 40);

        BufferedImage image = new BufferedImage(120, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }

        // A point well inside the face, away from the text, is the hazard yellow.
        assertEquals(Accent.CAUTION.getFace().getRGB(), image.getRGB(12, 20));

        // The frame is tape rather than a line, so the top edge alternates: both stripe colours appear along it.
        // Asserting a single pixel would only be testing where the diagonal happens to fall.
        boolean sawLight = false;
        boolean sawDark = false;
        for (int x = 8; x < 112; x++) {
            int pixel = image.getRGB(x, 1);
            sawLight |= (pixel == Accent.CAUTION.getFace().getRGB());
            sawDark |= (pixel == Accent.CAUTION.getFrame().getRGB());
        }
        assertTrue(sawLight, "the tape's light bars must show along the top edge");
        assertTrue(sawDark, "the tape's dark bars must show along the top edge");
    }
}
