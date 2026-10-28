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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConPathfinder.Pathing;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConPathfinder.StepCost;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConPathfinder}: the shared Dijkstra sweep behind roads and supply routes.
 */
class StratConPathfinderTest {
    private static final StepCost UNIT_COST = (from, to) -> 1;

    private static StratConTrackState track(int width, int height) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        return track;
    }

    @Test
    void aSourceCostsNothingAndIsItsOwnPath() {
        StratConTrackState track = track(5, 5);
        StratConCoords source = new StratConCoords(2, 2);

        Pathing pathing = StratConPathfinder.pathfind(track, List.of(source), UNIT_COST);

        assertEquals(0, pathing.distance().get(source));
        assertEquals(List.of(source), pathing.pathTo(source));
    }

    @Test
    void aPathRunsFromTheTargetBackToTheSourceOneStepAtATime() {
        StratConTrackState track = track(8, 8);
        StratConCoords source = new StratConCoords(0, 0);
        StratConCoords target = new StratConCoords(7, 7);

        Pathing pathing = StratConPathfinder.pathfind(track, List.of(source), UNIT_COST);
        List<StratConCoords> path = pathing.pathTo(target);

        assertEquals(target, path.get(0));
        assertEquals(source, path.get(path.size() - 1));
        for (int step = 1; step < path.size(); step++) {
            assertTrue(StratConHexGeometry.neighbors(track, path.get(step - 1)).contains(path.get(step)),
                  path.get(step - 1) + " and " + path.get(step) + " are not adjacent");
        }
        // At one per step, the cost is the number of steps.
        assertEquals(path.size() - 1, pathing.distance().get(target));
    }

    @Test
    void anImpassableStepIsNeverTaken() {
        StratConTrackState track = track(5, 5);
        StratConCoords source = new StratConCoords(2, 2);
        StratConCoords other = new StratConCoords(0, 0);

        Pathing pathing = StratConPathfinder.pathfind(track, List.of(source),
              (from, to) -> StratConPathfinder.IMPASSABLE);

        assertTrue(pathing.reaches(source));
        assertFalse(pathing.reaches(other));
        assertTrue(pathing.pathTo(other).isEmpty());
        assertNull(pathing.nearest(List.of(other)));
    }

    @Test
    void theCheapestStepsWinOverTheFewest() {
        StratConTrackState track = track(6, 6);
        StratConCoords source = new StratConCoords(0, 0);
        StratConCoords target = new StratConCoords(5, 5);
        StratConCoords dear = StratConHexGeometry.neighbors(track, source).get(0);

        Pathing pathing = StratConPathfinder.pathfind(track, List.of(source),
              (from, to) -> to.equals(dear) ? 100 : 1);

        assertFalse(pathing.pathTo(target).contains(dear), "the route should go around the dear hex");
    }

    @Test
    void everySourceStartsAtNoCost() {
        StratConTrackState track = track(8, 8);
        StratConCoords first = new StratConCoords(0, 0);
        StratConCoords second = new StratConCoords(7, 7);
        StratConCoords besideSecond = StratConHexGeometry.neighbors(track, second).get(0);

        Pathing pathing = StratConPathfinder.pathfind(track, List.of(first, second), UNIT_COST);

        assertEquals(0, pathing.distance().get(first));
        assertEquals(0, pathing.distance().get(second));
        assertEquals(1, pathing.distance().get(besideSecond));
        assertEquals(List.of(besideSecond, second), pathing.pathTo(besideSecond));
    }

    @Test
    void theNearestIsTheCheapestAndTheFirstMetOnATie() {
        StratConTrackState track = track(8, 8);
        StratConCoords source = new StratConCoords(3, 3);
        List<StratConCoords> beside = StratConHexGeometry.neighbors(track, source);
        StratConCoords far = new StratConCoords(7, 7);

        Pathing pathing = StratConPathfinder.pathfind(track, List.of(source), UNIT_COST);

        assertEquals(beside.get(0), pathing.nearest(List.of(far, beside.get(0))));
        assertEquals(beside.get(1), pathing.nearest(List.of(beside.get(1), beside.get(0))));
        assertEquals(beside.get(0), pathing.nearest(List.of(beside.get(0), beside.get(1))));
    }
}
