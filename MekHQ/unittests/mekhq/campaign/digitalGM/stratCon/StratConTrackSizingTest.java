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

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.LatitudeBand;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.SectorSpec;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorCountMethod;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for the sizing and temperature behaviour of {@link StratConContractInitializer#initializeTrackState}.
 */
class StratConTrackSizingTest {

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    private static final int NEUTRAL_TEMPERATURE = 25;

    /**
     * Real options rather than a mock: every option then falls back to its declared default, so generation reading an
     * option this test does not care about gets a usable value instead of a mock's null.
     */
    private static CampaignOptions options(StratConSectorCountMethod countMethod, double sizeMultiplier) {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.STRAT_CON_SECTOR_COUNT_METHOD, countMethod);
        options.set(CampaignOption.STRAT_CON_SECTOR_SIZE_MULTIPLIER, sizeMultiplier);
        return options;
    }

    /**
     * @return the land hexes a sector fronting the given number of combat teams is budgeted, on a Terra-sized world.
     *       The recon share is deliberately not rounded - four teams buy 1.33 recon teams' worth of ground.
     */
    private static int playableFor(int combatTeams) {
        double reconTeams = Math.max(1.0,
              combatTeams / (double) StratConContractInitializer.COMBAT_TEAMS_PER_RECON_FORCE);
        return (int) Math.round(reconTeams * StratConContractInitializer.RECON_HEXES_PER_QUARTER);
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
        CampaignOptions options = options(StratConSectorCountMethod.ALTERNATE, 1.0);
        options.set(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN, true);

        StratConTrackState track = track(new SectorSpec(9, LatitudeBand.EQUATORIAL), options);

        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                assertFalse(track.getTerrainTile(new StratConCoords(x, y)).isEmpty(),
                      "improved terrain left hex " + x + "," + y + " empty");
            }
        }
    }

    @Test
    void improvedSizing_matchesWhatTheSectorsReconCanCoverInAQuarter() {
        // The sizing rule: one recon force per three combat teams fronting the sector, each covering
        // RECON_HEXES_PER_QUARTER dry hexes in three months. A neutral planet is 50% water, so the sector is grown to
        // twice its dry budget.
        StratConTrackState track = track(new SectorSpec(9, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.ALTERNATE, 1.0));

        assertAreaNear(playableFor(9) * 2, track);
    }

    @Test
    void improvedSizing_aLegacySizedSectorIsBuiltForItsThreeTeams() {
        // Guards an option mismatch: with only "condense sectors" on, the COUNT is still legacy (a sector per three
        // teams) while the SIZING is improved. Sizing per sector-unit rather than per team handed each of those small
        // sectors a full nine-team sector's worth of ground.
        StratConTrackState track = track(new SectorSpec(3, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.ALTERNATE, 1.0));

        assertAreaNear(playableFor(3) * 2, track);
    }

    @Test
    void improvedSizing_sizeMultiplierAndUnitCountScaleEquivalently() {
        // A 2.0 multiplier on nine teams and a 1.0 multiplier on eighteen both double the playable target.
        StratConTrackState doubledByMultiplier = track(new SectorSpec(9, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.ALTERNATE, 2.0));
        StratConTrackState doubledByTeams = track(new SectorSpec(18, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.CONDENSED, 1.0));

        // Compared by area, not by dimensions: each track rolls its own shape, so their proportions legitimately differ.
        assertAreaNear(playableFor(18) * 2, doubledByMultiplier);
        assertAreaNear(playableFor(18) * 2, doubledByTeams);
    }

    @Test
    void improvedSizing_multiplierAlwaysBitesOnCondensedSectors() {
        // Eighteen teams at a 2.0 multiplier: twice the six recon forces' quarterly budget.
        StratConTrackState track = track(new SectorSpec(18, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.CONDENSED, 2.0));

        assertAreaNear(playableFor(18) * 2 * 2, track);
    }

    @Test
    void improvedSizing_variesTheShapeOfSectors() {
        // Sectors used to always be square. They are now laid out by a weighted shape profile, so across many rolls
        // more than one set of proportions should appear - that variety is the whole point.
        Set<String> shapes = new HashSet<>();
        for (int roll = 0; roll < 60; roll++) {
            StratConTrackState track = track(new SectorSpec(9, LatitudeBand.EQUATORIAL),
                  options(StratConSectorCountMethod.ALTERNATE, 1.0));
            shapes.add(track.getWidth() + "x" + track.getHeight());
        }

        assertTrue(shapes.size() > 1, "every sector came out the same shape: " + shapes);
    }

    @Test
    void improvedSizing_keepsEveryDimensionPlayable() {
        // No shape may produce a sliver, and no combination of planet and options may produce a runaway map.
        for (int roll = 0; roll < 60; roll++) {
            StratConTrackState track = track(new SectorSpec(27, LatitudeBand.EQUATORIAL),
                  options(StratConSectorCountMethod.CONDENSED, 2.0));

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

        StratConTrackState track = StratConContractInitializer.initializeTrackState(new SectorSpec(27,
              LatitudeBand.EQUATORIAL), bigOceanWorld, options(StratConSectorCountMethod.CONDENSED, 2.0), true, 0, 0);

        assertTrue(track.getSize() <= 1024, "runaway sector was not capped: " + track.getSize() + " hexes");
    }

    @Test
    void legacySizing_bothFlagsOff_matchesHistoricalRectangle() {
        // numHexes = 3 * 28 = 84; height = floor(sqrt(84)) = 9; width = 84 / 9 = 9
        StratConTrackState track = track(new SectorSpec(3, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.LEGACY, 1.0));

        assertEquals(9, track.getWidth());
        assertEquals(9, track.getHeight());
    }

    @Test
    void legacySizing_isCappedAtTheSameAreaCeilingAsTheImprovedPath() {
        // Unreachable through the legacy count, which gives a sector three formations at most - this guards the sizing
        // rule itself, so a count method built on legacy sizing later cannot produce an unbounded map.
        // 100 * 28 = 2800 hexes uncapped; capped to 1024, height = floor(sqrt(1024)) = 32, width = 1024 / 32 = 32.
        StratConTrackState track = track(new SectorSpec(100, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.LEGACY, 1.0));

        assertTrue((track.getWidth() * track.getHeight()) <= 1024,
              "legacy sizing produced " + track.getWidth() + "x" + track.getHeight() + ", past the area ceiling");
        assertEquals(32, track.getWidth());
        assertEquals(32, track.getHeight());
    }

    @Test
    void improvedTemperature_equatorialSectorStaysNearEquatorialBaseline() {
        StratConTrackState track = track(new SectorSpec(9, LatitudeBand.EQUATORIAL),
              options(StratConSectorCountMethod.ALTERNATE, 1.0));

        // 25 + 0 offset + (-5..+5)
        assertTrue(track.getTemperature() >= NEUTRAL_TEMPERATURE - 5);
        assertTrue(track.getTemperature() <= NEUTRAL_TEMPERATURE + 5);
    }

    @Test
    void improvedTemperature_polarSectorIsSharplyColder() {
        StratConTrackState track = track(new SectorSpec(9, LatitudeBand.SOUTH_POLAR),
              options(StratConSectorCountMethod.ALTERNATE, 1.0));

        // 25 + (-40) offset + (-5..+5) => -20..-10
        assertTrue(track.getTemperature() >= NEUTRAL_TEMPERATURE - 40 - 5);
        assertTrue(track.getTemperature() <= NEUTRAL_TEMPERATURE - 40 + 5);
    }

    @Test
    void trackAlwaysCarriesRequiredFormationCount() {
        StratConTrackState track = track(new SectorSpec(18, LatitudeBand.NORTH_POLAR),
              options(StratConSectorCountMethod.CONDENSED, 1.0));

        assertEquals(18, track.getRequiredLanceCount());
    }
}
