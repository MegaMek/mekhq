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
package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InterstellarMapPanelNavigationInstrumentTest {
    private static final int VIEWPORT_WIDTH = 1200;
    private static final int VIEWPORT_HEIGHT = 800;
    private static final double DELTA = 0.000_001;

    @Test
    void modelUsesBattleTechChartDirections() {
        InterstellarMapPanel.NavigationInstrumentLayout layout =
              InterstellarMapPanel.createNavigationInstrumentLayout(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, 1.0);

        assertTrue(layout.visible());
        assertEquals("COREWARD", layout.corewardLabel());
        assertEquals("RIMWARD", layout.rimwardLabel());
        assertEquals("ANTI-SPINWARD", layout.antiSpinwardLabel());
        assertEquals("SPINWARD", layout.spinwardLabel());
    }

    @ParameterizedTest
    @CsvSource({
            "0.5, 340, '340 LY'",
            "1.0, 170, '170 LY'",
            "2.0, 85, '85 LY'",
            "3.0, 56.666666666666664, '~56.7 LY'",
            "5.0, 34, '34 LY'"
    })
    void fixedScaleBarUsesExactLightYearDistancesAndReadableLabels(double mapScale,
        double expectedDistanceLy, String expectedLabel) {
      InterstellarMapPanel.NavigationInstrumentLayout referenceLayout =
          InterstellarMapPanel.createNavigationInstrumentLayout(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, 1.0);
        InterstellarMapPanel.NavigationInstrumentLayout layout =
              InterstellarMapPanel.createNavigationInstrumentLayout(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, mapScale);

        assertEquals(expectedDistanceLy, layout.distanceLy(), DELTA);
      assertEquals(referenceLayout.scaleBarPixelWidth(), layout.scaleBarPixelWidth(), DELTA);
      assertEquals(layout.scaleBarPixelWidth(), layout.distanceLy() * mapScale, DELTA);
      assertEquals(expectedLabel, layout.distanceLabel());
    }

    @Test
    void placementAndScaleBarEndpointsStayFixedAtLowerLeftAcrossZoom() {
        InterstellarMapPanel.NavigationInstrumentLayout scaleOne =
              InterstellarMapPanel.createNavigationInstrumentLayout(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, 1.0);
        InterstellarMapPanel.NavigationInstrumentLayout scaleThree =
              InterstellarMapPanel.createNavigationInstrumentLayout(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, 3.0);

        assertEquals(scaleOne.x(), scaleThree.x(), DELTA);
        assertEquals(scaleOne.y(), scaleThree.y(), DELTA);
        assertEquals(scaleOne.width(), scaleThree.width(), DELTA);
        assertEquals(scaleOne.height(), scaleThree.height(), DELTA);
        assertEquals(scaleOne.x(), VIEWPORT_HEIGHT - scaleOne.bounds().getMaxY(), DELTA,
              "left and bottom viewport margins");
        assertEquals(scaleOne.distanceLy() * 1.0, scaleOne.scaleBarPixelWidth(), DELTA);
        assertEquals(scaleThree.distanceLy() * 3.0, scaleThree.scaleBarPixelWidth(), DELTA);
          assertEquals(scaleOne.scaleBarStartX(), scaleThree.scaleBarStartX(), DELTA);
          assertEquals(scaleOne.scaleBarEndX(), scaleThree.scaleBarEndX(), DELTA);
          assertEquals(scaleOne.scaleBarPixelWidth(), scaleThree.scaleBarPixelWidth(), DELTA);
          assertNotEquals(scaleOne.distanceLy(), scaleThree.distanceLy());
          assertNotEquals(scaleOne.distanceLabel(), scaleThree.distanceLabel());

        Rectangle2D bounds = scaleThree.bounds();
        assertTrue(bounds.getMinX() >= 0.0);
        assertTrue(bounds.getMinY() >= 0.0);
        assertTrue(bounds.getMaxX() <= VIEWPORT_WIDTH);
        assertTrue(bounds.getMaxY() <= VIEWPORT_HEIGHT);
    }

    @Test
    void smallOrInvalidViewportsOmitInstrumentWithoutPainting() {
        InterstellarMapPanel.NavigationInstrumentLayout small =
              InterstellarMapPanel.createNavigationInstrumentLayout(120, 80, 1.0);
        InterstellarMapPanel.NavigationInstrumentLayout invalid =
              InterstellarMapPanel.createNavigationInstrumentLayout(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, Double.NaN);
        assertFalse(small.visible());
        assertFalse(invalid.visible());

        BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        assertDoesNotThrow(() -> InterstellarMapPanel.drawNavigationInstrument(graphics, small));
        graphics.dispose();
        assertEquals(0, countPaintedPixels(image));
    }

    @Test
    void instrumentRendersNonblankOffscreen() {
        InterstellarMapPanel.NavigationInstrumentLayout layout =
              InterstellarMapPanel.createNavigationInstrumentLayout(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, 3.0);
        BufferedImage image = new BufferedImage(VIEWPORT_WIDTH, VIEWPORT_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        InterstellarMapPanel.drawNavigationInstrument(graphics, layout);
        graphics.dispose();

        assertTrue(countPaintedPixels(image) > 100);
    }

    private static int countPaintedPixels(BufferedImage image) {
        int paintedPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    paintedPixels++;
                }
            }
        }
        return paintedPixels;
    }
}
