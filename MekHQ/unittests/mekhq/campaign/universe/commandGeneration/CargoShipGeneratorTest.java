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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import mekhq.campaign.universe.commandGeneration.CargoShipGenerator.Candidate;
import org.junit.jupiter.api.Test;

/**
 * Verifies how {@link CargoShipGenerator} chooses hulls for a cargo lift.
 *
 * <p>The two failure modes being guarded against are a large lift being met by a swarm of hulls that
 * each carry very little, and a small lift buying a hull far bigger than it needs. Both were possible
 * when selection was a plain weighted draw over every design with any cargo space at all.</p>
 *
 * <p>Hull summaries are {@code null} here: {@code pickHull} decides on capacity and availability weight
 * alone, so nothing has to be loaded from the unit files.</p>
 */
class CargoShipGeneratorTest {

    /** A stand-in fleet spanning the range of real DropShip holds, smallest first. */
    private static List<Candidate> fleet() {
        List<Candidate> candidates = new ArrayList<>(List.of(
              new Candidate(null, 75, 0, 10),      // small freighter
              new Candidate(null, 200, 0, 10),
              new Candidate(null, 800, 100, 5),
              new Candidate(null, 3000, 0, 3),     // dedicated freighter
              new Candidate(null, 8000, 0, 1)));   // bulk hauler
        candidates.sort(Comparator.comparingDouble(Candidate::solidTons));
        return candidates;
    }

    @Test
    void aLargeLiftIsNotMetByTheSmallestHulls() {
        // 4000 tons outstanding: the band spans 1000 to 6000, so only the 3000-ton freighter fits.
        // The 75- and 200-ton hulls must not be reachable, or the lift becomes a swarm.
        Candidate chosen = CargoShipGenerator.pickHull(fleet(), 4000);
        assertEquals(3000, chosen.solidTons(),
              "a large lift must draw from hulls sized for it, not from incidental holds");
    }

    @Test
    void aSmallLiftDoesNotBuyALeviathan() {
        // 150 tons outstanding: the band spans 37.5 to 225, admitting the 75- and 200-ton hulls only.
        for (int attempt = 0; attempt < 25; attempt++) {
            Candidate chosen = CargoShipGenerator.pickHull(fleet(), 150);
            assertTrue(chosen.solidTons() <= 225,
                  "a 150 ton lift must not buy a " + chosen.solidTons() + " ton hull");
        }
    }

    @Test
    void theLastShipOfALiftIsTheSmallestThatFinishesIt() {
        // 30 tons outstanding is below every hull, so nothing falls in the band. The smallest hull that
        // covers it wins - buying the 8000 ton hauler to move 30 tons would be absurd.
        Candidate chosen = CargoShipGenerator.pickHull(fleet(), 30);
        assertEquals(75, chosen.solidTons(),
              "the trailing remainder should take the least oversized hull that covers it");
    }

    @Test
    void aRequirementBiggerThanAnyHullTakesTheLargest() {
        // 50000 tons: no hull covers it and none reaches the band's lower bound, so the run should chip
        // away with the biggest available rather than stalling or picking arbitrarily.
        Candidate chosen = CargoShipGenerator.pickHull(fleet(), 50000);
        assertEquals(8000, chosen.solidTons(),
              "an oversized requirement should be reduced by the largest hull available");
    }

    @Test
    void availabilityWeightDecidesBetweenSimilarlySizedHulls() {
        // Two hulls of the same size, one far more common. Selection inside the band is a weighted draw,
        // so the ruleset's availability data still decides which freighter turns up.
        List<Candidate> pair = List.of(
              new Candidate(null, 1000, 0, 1),
              new Candidate(null, 1000, 0, 99));
        int commonPicks = 0;
        for (int attempt = 0; attempt < 200; attempt++) {
            if (CargoShipGenerator.pickHull(pair, 1000).availabilityWeight() == 99) {
                commonPicks++;
            }
        }
        assertTrue(commonPicks > 150,
              "the common hull should dominate the draw; got " + commonPicks + "/200");
    }

    @Test
    void aSingleCandidateIsAlwaysChosen() {
        List<Candidate> only = List.of(new Candidate(null, 500, 0, 1));
        assertEquals(500, CargoShipGenerator.pickHull(only, 12345).solidTons());
        assertEquals(500, CargoShipGenerator.pickHull(only, 1).solidTons());
    }
}
