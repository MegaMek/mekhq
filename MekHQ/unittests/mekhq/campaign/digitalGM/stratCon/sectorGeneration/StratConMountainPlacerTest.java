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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Predicate;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConMountainPlacer}: orogeny-driven mountain shapes, gravity-driven counts, volcanic terrain,
 * and ocean avoidance.
 */
class StratConMountainPlacerTest {
    private static final String MOUNTAIN = "Mountain";
    private static final String VOLCANO = "Volcano";
    private static final String OCEAN = "Sea";
    private static final int SIZE = 20;

    private static StratConTrackState track() {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(SIZE);
        track.setHeight(SIZE);
        return track;
    }

    private static OrogenyProfile profile(OrogenyProfileType type, double rangeCountModifier, int volcanism) {
        return new OrogenyProfile(type, null, null, null, null, null, null, rangeCountModifier, volcanism);
    }

    private static long count(StratConTrackState track, Predicate<String> predicate) {
        long total = 0;
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                if (predicate.test(track.getTerrainTile(new StratConCoords(x, y)))) {
                    total++;
                }
            }
        }
        return total;
    }

    private static long mountainOrVolcano(StratConTrackState track) {
        return count(track, terrain -> terrain.equals(MOUNTAIN) || terrain.equals(VOLCANO));
    }

    private static void fillOcean(StratConTrackState track) {
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                track.setTerrainTile(new StratConCoords(x, y), OCEAN);
            }
        }
    }

    @Test
    void zeroGravity_placesNoMountains() {
        StratConTrackState track = track();
        StratConMountainPlacer.placeMountains(track,
              MOUNTAIN,
              profile(OrogenyProfileType.CORDILLERA, 1.0, 10),
              0.0,
              0.0);
        assertEquals(0, mountainOrVolcano(track));
    }

    @Test
    void noMountainTerrain_placesNothing() {
        StratConTrackState track = track();
        StratConMountainPlacer.placeMountains(track, null, profile(OrogenyProfileType.CORDILLERA, 1.0, 10), 2.0, 0.0);
        assertEquals(0, count(track, terrain -> !terrain.isEmpty()));
    }

    @Test
    void nullOrogeny_placesNothing() {
        StratConTrackState track = track();
        StratConMountainPlacer.placeMountains(track, MOUNTAIN, null, 2.0, 0.0);
        assertEquals(0, count(track, terrain -> !terrain.isEmpty()));
    }

    @Test
    void everyShape_onlyPlacesMountainOrVolcanicTerrain() {
        for (OrogenyProfileType type : OrogenyProfileType.values()) {
            StratConTrackState track = track();
            StratConMountainPlacer.placeMountains(track, MOUNTAIN, profile(type, 1.5, 30), 2.0, 0.0);

            long stray = count(track,
                  terrain -> !terrain.isEmpty() && !terrain.equals(MOUNTAIN) && !terrain.equals(VOLCANO));
            assertEquals(0, stray, type + " placed non-mountain terrain");
        }
    }

    @Test
    void everyShape_neverOverwritesOcean() {
        for (OrogenyProfileType type : OrogenyProfileType.values()) {
            StratConTrackState track = track();
            fillOcean(track);

            for (int run = 0; run < 5; run++) {
                StratConMountainPlacer.placeMountains(track, MOUNTAIN, profile(type, 1.5, 30), 2.0, 0.0);
            }

            assertEquals(SIZE * SIZE, count(track, OCEAN::equals), type + " overwrote ocean");
            assertEquals(0, mountainOrVolcano(track), type + " placed mountains on an all-ocean sector");
        }
    }

    @Test
    void highGravity_placesMountainsAcrossRuns() {
        long total = 0;
        for (int run = 0; run < 40; run++) {
            StratConTrackState track = track();
            StratConMountainPlacer.placeMountains(track,
                  MOUNTAIN,
                  profile(OrogenyProfileType.CORDILLERA, 1.0, 10),
                  2.0, 0.0);
            total += mountainOrVolcano(track);
        }
        assertTrue(total > 0, "high gravity should produce mountains across many runs");
    }

    @Test
    void higherRangeCountModifier_placesMoreMountainsOnAverage() {
        long low = 0;
        long high = 0;
        for (int run = 0; run < 80; run++) {
            StratConTrackState lowTrack = track();
            StratConMountainPlacer.placeMountains(lowTrack, MOUNTAIN, profile(OrogenyProfileType.CORDILLERA, 0.3, 0),
                  2.0, 0.0);
            low += mountainOrVolcano(lowTrack);

            StratConTrackState highTrack = track();
            StratConMountainPlacer.placeMountains(highTrack, MOUNTAIN, profile(OrogenyProfileType.CORDILLERA, 2.0, 0),
                  2.0, 0.0);
            high += mountainOrVolcano(highTrack);
        }
        assertTrue(high > low, "a higher range-count modifier should place more mountains on average");
    }

    @Test
    void volcanicArc_isMostlyVolcanic() {
        long volcano = 0;
        long mountain = 0;
        for (int run = 0; run < 40; run++) {
            StratConTrackState track = track();
            StratConMountainPlacer.placeMountains(track, MOUNTAIN, profile(OrogenyProfileType.VOLCANIC_ARC, 1.0, 70),
                  2.0, 0.0);
            volcano += count(track, VOLCANO::equals);
            mountain += count(track, MOUNTAIN::equals);
        }
        assertTrue(volcano > mountain, "a 70% volcanism arc should be mostly volcanic");
    }

    @Test
    void volcanism_isUnchangedOnAnUninhabitableWorld() {
        // An airless or lethal world scores 0 habitability and keeps its authored volcanism in full.
        OrogenyProfile arc = profile(OrogenyProfileType.VOLCANIC_ARC, 1.0, 70);

        assertEquals(70, StratConMountainPlacer.effectiveVolcanism(arc, 0.0));
    }

    @Test
    void volcanism_isDampedOnAComfortableWorld() {
        // The complaint this addresses: orogeny is picked on gravity, temperature and water, none of which stop a
        // settled temperate world drawing a volcanic profile - so the volcanism itself is damped by habitability.
        OrogenyProfile arc = profile(OrogenyProfileType.VOLCANIC_ARC, 1.0, 70);

        int comfortable = StratConMountainPlacer.effectiveVolcanism(arc, 1.0);

        assertTrue(comfortable < 70, "a habitable world should shed most of its volcanism, got " + comfortable);
        assertTrue(comfortable > 0, "a habitable world may still sit on a volcanic arc, got " + comfortable);
    }

    @Test
    void volcanism_fallsAsAWorldBecomesMoreHabitable() {
        OrogenyProfile arc = profile(OrogenyProfileType.VOLCANIC_ARC, 1.0, 70);

        int previous = Integer.MAX_VALUE;
        for (double habitability : new double[] { 0.0, 0.25, 0.5, 0.75, 1.0 }) {
            int volcanism = StratConMountainPlacer.effectiveVolcanism(arc, habitability);
            assertTrue(volcanism <= previous,
                  "volcanism should not rise as habitability rises (at " + habitability + ')');
            previous = volcanism;
        }
    }

    @Test
    void volcanism_handlesAMissingProfile() {
        assertEquals(0, StratConMountainPlacer.effectiveVolcanism(null, 0.5));
    }
}
