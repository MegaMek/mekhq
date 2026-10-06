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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LatitudeBand}: the per-band temperature offset improved sector generation applies to a planet's
 * equatorial baseline, and the uniform random band pick.
 */
class LatitudeBandTest {

    @Test
    void temperatureOffset_equatorialBandIsTheBaseline() {
        assertEquals(0,
              LatitudeBand.EQUATORIAL.getTemperatureOffset(),
              "the equatorial band is the planet's recorded temperature, so its offset must be zero");
    }

    @Test
    void temperatureOffset_getsColderTowardThePoles() {
        // Each band away from the equator must be strictly colder than the one inside it, otherwise polar sectors
        // would generate temperate terrain.
        List<LatitudeBand> northToPole = List.of(LatitudeBand.EQUATORIAL,
              LatitudeBand.NORTH_TROPICAL,
              LatitudeBand.NORTH_TEMPERATE,
              LatitudeBand.NORTH_POLAR);

        for (int index = 1; index < northToPole.size(); index++) {
            LatitudeBand inner = northToPole.get(index - 1);
            LatitudeBand outer = northToPole.get(index);

            assertTrue(outer.getTemperatureOffset() < inner.getTemperatureOffset(),
                  outer + " should be colder than " + inner);
        }
    }

    @Test
    void temperatureOffset_isNeverPositive() {
        for (LatitudeBand band : LatitudeBand.values()) {
            assertTrue(band.getTemperatureOffset() <= 0,
                  band + " warmed the sector; latitude may only chill it relative to the equator");
        }
    }

    @Test
    void temperatureOffset_northernAndSouthernHalvesOfABandMatch() {
        // The hemispheres are mirror images, so a sector's hemisphere must never change its temperature.
        assertEquals(LatitudeBand.NORTH_TROPICAL.getTemperatureOffset(),
              LatitudeBand.SOUTH_TROPICAL.getTemperatureOffset(),
              "the tropical bands should share one offset across both hemispheres");
        assertEquals(LatitudeBand.NORTH_TEMPERATE.getTemperatureOffset(),
              LatitudeBand.SOUTH_TEMPERATE.getTemperatureOffset(),
              "the temperate bands should share one offset across both hemispheres");
        assertEquals(LatitudeBand.NORTH_POLAR.getTemperatureOffset(),
              LatitudeBand.SOUTH_POLAR.getTemperatureOffset(),
              "the polar bands should share one offset across both hemispheres");
    }

    @Test
    void temperatureOffset_matchesTheAuthoredValues() {
        assertEquals(0, LatitudeBand.EQUATORIAL.getTemperatureOffset(), "equatorial offset changed");
        assertEquals(-8, LatitudeBand.NORTH_TROPICAL.getTemperatureOffset(), "tropical offset changed");
        assertEquals(-20, LatitudeBand.NORTH_TEMPERATE.getTemperatureOffset(), "temperate offset changed");
        assertEquals(-40, LatitudeBand.NORTH_POLAR.getTemperatureOffset(), "polar offset changed");
    }

    @Test
    void random_onlyEverReturnsADeclaredBand() {
        Set<LatitudeBand> declared = EnumSet.allOf(LatitudeBand.class);

        for (int roll = 0; roll < 500; roll++) {
            LatitudeBand band = LatitudeBand.random();

            assertNotNull(band, "random() must never return null");
            assertTrue(declared.contains(band), "random() returned a band outside the declared set: " + band);
        }
    }
}
