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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class TransmissionImagePanelTest {
    @Test
    void imageDimensionsArePreserved() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage sourceImage = createSourceImage(40, 55);
            TransmissionImagePanel panel = new TransmissionImagePanel(new ImageIcon(sourceImage));

            assertEquals(new Dimension(40, 55), panel.getMinimumSize());
            assertEquals(new Dimension(40, 55), panel.getPreferredSize());
            assertEquals(new Dimension(40, 55), panel.getMaximumSize());
        });
    }

    @Test
    void transmissionTreatmentIsPaintedOverSourceImage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage sourceImage = createSourceImage(40, 55);
            TransmissionImagePanel panel = new TransmissionImagePanel(new ImageIcon(sourceImage));
            panel.setSize(panel.getPreferredSize());

            BufferedImage renderedImage = new BufferedImage(40, 55, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics2D = renderedImage.createGraphics();
            panel.paint(graphics2D);
            graphics2D.dispose();

            assertNotEquals(sourceImage.getRGB(20, 20), renderedImage.getRGB(20, 20));
            assertNotEquals(sourceImage.getRGB(1, 1), renderedImage.getRGB(1, 1));
        });
    }

    @Test
    void absentImageHasNoLayoutFootprint() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionImagePanel panel = new TransmissionImagePanel(null);
            assertEquals(new Dimension(), panel.getPreferredSize());
        });
    }

    @Test
    void signalQualitiesProduceDifferentTreatments() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage sourceImage = createSourceImage(40, 55);
            BufferedImage clearImage = render(sourceImage, TransmissionSignalQuality.CLEAR);
            BufferedImage remoteImage = render(sourceImage, TransmissionSignalQuality.REMOTE);
            BufferedImage degradedImage = render(sourceImage, TransmissionSignalQuality.DEGRADED);

            assertFalse(Arrays.equals(getPixels(clearImage), getPixels(remoteImage)));
            assertFalse(Arrays.equals(getPixels(remoteImage), getPixels(degradedImage)));
        });
    }

    private static BufferedImage render(BufferedImage sourceImage, TransmissionSignalQuality signalQuality) {
        TransmissionImagePanel panel = new TransmissionImagePanel(new ImageIcon(sourceImage), signalQuality);
        panel.setSize(panel.getPreferredSize());

        BufferedImage renderedImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(),
              BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = renderedImage.createGraphics();
        panel.paint(graphics2D);
        graphics2D.dispose();
        return renderedImage;
    }

    private static int[] getPixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static BufferedImage createSourceImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        graphics2D.setColor(Color.RED);
        graphics2D.fillRect(0, 0, width, height);
        graphics2D.dispose();
        return image;
    }
}
