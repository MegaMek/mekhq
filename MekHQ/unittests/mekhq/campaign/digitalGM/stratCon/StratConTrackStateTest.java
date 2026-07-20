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
import static org.junit.jupiter.api.Assertions.assertNull;

import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StratConTrackState#findClosestAlliedFacilityCoords}, which measures in hex steps across the map
 * rather than by the coordinate distance StratConCoords inherits.
 *
 * <p>The method under test is deprecated for removal, so exercising it is the whole point of this class; the warning
 * is suppressed here and nowhere else. Delete this class along with the method.</p>
 */
@SuppressWarnings("deprecation")
class StratConTrackStateTest {

    private static StratConTrackState track(int width, int height) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        return track;
    }

    private static void addFacility(StratConTrackState track, StratConCoords coords, ForceAlignment owner) {
        StratConFacility facility = new StratConFacility();
        facility.setOwner(owner);
        track.addFacility(coords, facility);
    }

    @Test
    void noFacilities_returnsNull() {
        assertNull(track(10, 10).findClosestAlliedFacilityCoords(new StratConCoords(5, 5)));
    }

    @Test
    void hostileFacilitiesAreIgnored() {
        StratConTrackState track = track(10, 10);
        addFacility(track, new StratConCoords(5, 5), ForceAlignment.Opposing);

        assertNull(track.findClosestAlliedFacilityCoords(new StratConCoords(5, 6)),
              "only allied facilities should be considered");
    }

    @Test
    void findsTheFacilityOnTheSearchedHex() {
        StratConTrackState track = track(10, 10);
        StratConCoords coords = new StratConCoords(4, 4);
        addFacility(track, coords, ForceAlignment.Allied);

        assertEquals(coords, track.findClosestAlliedFacilityCoords(coords));
    }

    @Test
    void picksTheNearerOfTwoAlliedFacilities() {
        StratConTrackState track = track(14, 14);
        StratConCoords origin = new StratConCoords(6, 6);
        StratConCoords near = origin.translate(0);
        StratConCoords far = new StratConCoords(13, 13);

        addFacility(track, far, ForceAlignment.Allied);
        addFacility(track, near, ForceAlignment.Allied);

        assertEquals(near, track.findClosestAlliedFacilityCoords(origin), "the adjacent facility is the closest one");
    }

    @Test
    void everyNeighborOfAHexCountsAsEquallyClose() {
        // Guards the parity bug: measured by coordinate distance rather than map adjacency, two of a hex's six
        // neighbors come out as distance 2 while two non-neighbors come out as distance 1.
        StratConCoords origin = new StratConCoords(6, 6);

        for (int direction = 0; direction < 6; direction++) {
            StratConTrackState oneFacility = track(14, 14);
            StratConCoords neighbor = origin.translate(direction);
            addFacility(oneFacility, neighbor, ForceAlignment.Allied);

            assertEquals(neighbor,
                  oneFacility.findClosestAlliedFacilityCoords(origin),
                  "the facility on the neighbor in direction " + direction + " should be found");
        }
    }
}
