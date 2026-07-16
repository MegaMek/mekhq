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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.campaignOptions.CampaignOptions;
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

    private static StratConTrackState track(SectorSpec spec, CampaignOptions options) {
        // A neutral profile has sizeFactor 1.0 and 50% water, so improved sizing is easy to reason about.
        return StratConContractInitializer.initializeTrackState(spec,
              PlanetProfile.neutral(NEUTRAL_TEMPERATURE),
              options,
              0,
              0);
    }

    @Test
    void improvedSizing_neutralPlanet_singleUnit_isTwelveByTwelve() {
        // perUnitPlayable = round(78 * 1.0) = 78; playable = 78; total = round(78 / 0.5) = 156; dim = round(sqrt) = 12
        StratConTrackState track = track(new SectorSpec(1, 9, LatitudeBand.EQUATORIAL), options(true, false, 1.0));

        assertEquals(12, track.getWidth());
        assertEquals(12, track.getHeight());
    }

    @Test
    void improvedSizing_sizeMultiplierAndUnitCountScaleEquivalently() {
        // A 2.0 multiplier on one unit and a 1.0 multiplier on two units both double the playable target.
        StratConTrackState doubledByMultiplier = track(new SectorSpec(1, 9, LatitudeBand.EQUATORIAL),
              options(true, false, 2.0));
        StratConTrackState doubledByUnits = track(new SectorSpec(2, 18, LatitudeBand.EQUATORIAL),
              options(true, true, 1.0));

        assertEquals(doubledByUnits.getWidth(), doubledByMultiplier.getWidth());
        assertEquals(doubledByUnits.getHeight(), doubledByMultiplier.getHeight());
    }

    @Test
    void improvedSizing_multiplierAlwaysBitesOnCondensedSectors() {
        // unit count 2 with a 2.0 multiplier: playable = round(2 * 78 * 2) = 312; total = 624; dim = round(sqrt) = 25
        StratConTrackState track = track(new SectorSpec(2, 18, LatitudeBand.EQUATORIAL), options(true, true, 2.0));

        assertEquals(25, track.getWidth());
        assertEquals(25, track.getHeight());
    }

    @Test
    void improvedSizing_producesSquareTracks() {
        StratConTrackState track = track(new SectorSpec(3, 27, LatitudeBand.NORTH_TEMPERATE), options(true, true, 1.0));

        assertEquals(track.getWidth(), track.getHeight());
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
