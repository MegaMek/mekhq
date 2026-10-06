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
package mekhq.gui.baseComponents.immersiveDialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class TransmissionRevealPanelTest {
    private static final int WIDTH = 80;
    private static final int HEIGHT = 60;

    @Test
    void initialFrameIsCenterLineAndCompletedFrameShowsContent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionRevealPanel revealPanel = createRevealPanel();

            BufferedImage initialFrame = render(revealPanel);
            assertNotEquals(Color.RED.getRGB(), initialFrame.getRGB(10, 10));
            assertTrue(new Color(initialFrame.getRGB(WIDTH / 2, HEIGHT / 2), true).getAlpha() > 0);
            assertFalse(revealPanel.contains(WIDTH / 2, HEIGHT / 2));
            assertTrue(revealPanel.isPaintingOrigin());

            revealPanel.completeReveal();
            BufferedImage completedFrame = render(revealPanel);
            assertEquals(Color.RED.getRGB(), completedFrame.getRGB(10, 10));
            assertTrue(revealPanel.contains(WIDTH / 2, HEIGHT / 2));
            assertFalse(revealPanel.isPaintingOrigin());
        });
    }

    @Test
    void wrapperPreservesContentPreferredSize() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionRevealPanel revealPanel = createRevealPanel();
            assertEquals(new Dimension(WIDTH, HEIGHT), revealPanel.getPreferredSize());
        });
    }

    private static TransmissionRevealPanel createRevealPanel() {
        JPanel content = new JPanel();
        content.setBackground(Color.RED);
        content.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        TransmissionRevealPanel revealPanel = new TransmissionRevealPanel(content);
        revealPanel.setSize(WIDTH, HEIGHT);
        revealPanel.doLayout();
        return revealPanel;
    }

    private static BufferedImage render(TransmissionRevealPanel revealPanel) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        revealPanel.paint(graphics2D);
        graphics2D.dispose();
        return image;
    }
}
