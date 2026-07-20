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
package mekhq.campaign.digitalGM.stratCon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.LatitudeBand;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.SectorSpec;
import org.junit.jupiter.api.Test;

/**
 * Tests for the sizing and temperature behaviour of {@link StratConContractInitializer#initializeTrackState}.
 */
class StratConTrackSizingTest {
    private static final int NEUTRAL_TEMPERATURE = 25;

    private static CampaignOptions options(boolean alternateCount, boolean condense, double sizeMultiplier) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseStratConAlternateSectorCount()).thenReturn(alternateCount);
        when(options.isUseStratConCondenseSectors()).thenReturn(condense);
        when(options.getStratConSectorSizeMultiplier()).thenReturn(sizeMultiplier);
        return options;
    }

    /**
     * Asserts a sector covers roughly the expected number of hexes. Exact equality is not available: the shape profile
     * rounds each dimension independently, so the laid-out area lands near the target rather than on it.
     */
    private static void assertAreaNear(int expectedHexes, StratConTrackState track) {
        int actual = track.getSize();
        assertTrue((actual >= (expectedHexes * 0.75)) && (actual <= (expectedHexes * 1.25)),
              "expected roughly " + expectedHexes + " hexes but got " + actual +
                    " (" + track.getWidth() + "x" + track.getHeight() + ')');
    }

    private static StratConTrackState track(SectorSpec spec, CampaignOptions options) {
        // A neutral profile has sizeFactor 1.0 and 50% water, so improved sizing is easy to reason about.
        return StratConContractInitializer.initializeTrackState(spec,
              PlanetProfile.neutral(NEUTRAL_TEMPERATURE),
              options,
              true,
              0,
              0);
    }

    @Test
    void alternateTerrain_generatesAFullyTerrainedSector() {
        CampaignOptions options = options(true, false, 1.0);
        when(options.isUseStratConAlternateSectorTerrain()).thenReturn(true);

        StratConTrackState track = track(new SectorSpec(1, 9, LatitudeBand.EQUATORIAL), options);

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                assertFalse(track.getTerrainTile(new StratConCoords(x, y)).isEmpty(),
                      "improved terrain left hex " + x + "," + y + " empty");
            }
        }
    }

    @Test
    void improvedSizing_neutralPlanet_singleUnit_coversRoughlyItsTargetArea() {
        // perUnitPlayable = round(78 * 1.0) = 78; playable = 78; total = round(78 / 0.5) = 156 hexes. The shape profile
        // then lays that area out, so the exact dimensions vary - the area is what the sizing math controls.
        StratConTrackState track = track(new SectorSpec(1, 9, LatitudeBand.EQUATORIAL), options(true, false, 1.0));

        assertAreaNear(156, track);
    }

    @Test
    void improvedSizing_sizeMultiplierAndUnitCountScaleEquivalently() {
        // A 2.0 multiplier on one unit and a 1.0 multiplier on two units both double the playable target to 312.
        StratConTrackState doubledByMultiplier = track(new SectorSpec(1, 9, LatitudeBand.EQUATORIAL),
              options(true, false, 2.0));
        StratConTrackState doubledByUnits = track(new SectorSpec(2, 18, LatitudeBand.EQUATORIAL),
              options(true, true, 1.0));

        // Compared by area, not by dimensions: each track rolls its own shape, so their proportions legitimately differ.
        assertAreaNear(312, doubledByMultiplier);
        assertAreaNear(312, doubledByUnits);
    }

    @Test
    void improvedSizing_multiplierAlwaysBitesOnCondensedSectors() {
        // unit count 2 with a 2.0 multiplier: playable = round(2 * 78 * 2) = 312; total = 624.
        StratConTrackState track = track(new SectorSpec(2, 18, LatitudeBand.EQUATORIAL), options(true, true, 2.0));

        assertAreaNear(624, track);
    }

    @Test
    void improvedSizing_variesTheShapeOfSectors() {
        // Sectors used to always be square. They are now laid out by a weighted shape profile, so across many rolls
        // more than one set of proportions should appear - that variety is the whole point.
        Set<String> shapes = new HashSet<>();
        for (int roll = 0; roll < 60; roll++) {
            StratConTrackState track = track(new SectorSpec(1, 9, LatitudeBand.EQUATORIAL), options(true, false, 1.0));
            shapes.add(track.getWidth() + "x" + track.getHeight());
        }

        assertTrue(shapes.size() > 1, "every sector came out the same shape: " + shapes);
    }

    @Test
    void improvedSizing_keepsEveryDimensionPlayable() {
        // No shape may produce a sliver, and no combination of planet and options may produce a runaway map.
        for (int roll = 0; roll < 60; roll++) {
            StratConTrackState track = track(new SectorSpec(3, 27, LatitudeBand.EQUATORIAL), options(true, true, 2.0));

            assertTrue(track.getWidth() >= 4, "sector too narrow to play: " + track.getWidth());
            assertTrue(track.getHeight() >= 4, "sector too shallow to play: " + track.getHeight());
            assertTrue(track.getWidth() <= 48, "sector wider than the ceiling: " + track.getWidth());
            assertTrue(track.getHeight() <= 48, "sector taller than the ceiling: " + track.getHeight());
        }
    }

    @Test
    void improvedSizing_capsRunawaySectors() {
        // A big, wet world with a condensed multi-unit sector and a doubled multiplier used to ask for thousands of
        // hexes (measured at 61x61). The area ceiling holds it to roughly 32x32 worth of map.
        PlanetProfile bigOceanWorld = new PlanetProfile(20, PlanetProfile.TERRA_DIAMETER_KM * 2, 95, false, null,
              "Rocky", 1, 1.0, 1_000_000L, mekhq.campaign.universe.enums.HPGRating.X);

        StratConTrackState track = StratConContractInitializer.initializeTrackState(new SectorSpec(3,
              27,
              LatitudeBand.EQUATORIAL), bigOceanWorld, options(true, true, 2.0), true, 0, 0);

        assertTrue(track.getSize() <= 1024, "runaway sector was not capped: " + track.getSize() + " hexes");
    }

    @Test
    void legacySizing_bothFlagsOff_matchesHistoricalRectangle() {
        // numHexes = 3 * 28 = 84; height = floor(sqrt(84)) = 9; width = 84 / 9 = 9
        StratConTrackState track = track(new SectorSpec(1, 3, LatitudeBand.EQUATORIAL), options(false, false, 1.0));

        assertEquals(9, track.getWidth());
        assertEquals(9, track.getHeight());
    }

    @Test
    void improvedTemperature_equatorialSectorStaysNearEquatorialBaseline() {
        StratConTrackState track = track(new SectorSpec(1, 9, LatitudeBand.EQUATORIAL), options(true, false, 1.0));

        // 25 + 0 offset + (-5..+5)
        assertTrue(track.getTemperature() >= NEUTRAL_TEMPERATURE - 5);
        assertTrue(track.getTemperature() <= NEUTRAL_TEMPERATURE + 5);
    }

    @Test
    void improvedTemperature_polarSectorIsSharplyColder() {
        StratConTrackState track = track(new SectorSpec(1, 9, LatitudeBand.SOUTH_POLAR), options(true, false, 1.0));

        // 25 + (-40) offset + (-5..+5) => -20..-10
        assertTrue(track.getTemperature() >= NEUTRAL_TEMPERATURE - 40 - 5);
        assertTrue(track.getTemperature() <= NEUTRAL_TEMPERATURE - 40 + 5);
    }

    @Test
    void trackAlwaysCarriesRequiredFormationCount() {
        StratConTrackState track = track(new SectorSpec(2, 18, LatitudeBand.NORTH_POLAR), options(true, true, 1.0));

        assertEquals(18, track.getRequiredLanceCount());
    }
}
