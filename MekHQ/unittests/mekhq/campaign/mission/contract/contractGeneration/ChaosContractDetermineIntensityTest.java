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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import megamek.common.compute.Compute;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests the roll-to-track-count tables in {@link ChaosContractDetermineIntensity}. The 2d6 roll is stubbed so each
 * branch of every objective family is pinned, including the boundary rolls (2 and 12) where an off-by-one in the switch
 * would show up.
 */
class ChaosContractDetermineIntensityTest {

    private static int trackCountForRoll(final ChaosObjectiveType objectiveType, final int roll) {
        try (MockedStatic<Compute> compute = mockStatic(Compute.class)) {
            compute.when(() -> Compute.d6(2)).thenReturn(roll);
            return ChaosContractDetermineIntensity.determineTrackCount(objectiveType);
        }
    }

    @Test
    void invasionScalesFromTwoToSixTracks() {
        assertEquals(2, trackCountForRoll(ChaosObjectiveType.INVASION, 2));
        assertEquals(2, trackCountForRoll(ChaosObjectiveType.INVASION, 5));
        assertEquals(3, trackCountForRoll(ChaosObjectiveType.INVASION, 6));
        assertEquals(4, trackCountForRoll(ChaosObjectiveType.INVASION, 8));
        assertEquals(5, trackCountForRoll(ChaosObjectiveType.INVASION, 10));
        assertEquals(6, trackCountForRoll(ChaosObjectiveType.INVASION, 12));
    }

    @Test
    void garrisonAndCadreScaleFromZeroToFiveTracks() {
        assertEquals(0, trackCountForRoll(ChaosObjectiveType.GARRISON, 2));
        assertEquals(0, trackCountForRoll(ChaosObjectiveType.GARRISON, 4));
        assertEquals(1, trackCountForRoll(ChaosObjectiveType.GARRISON, 5));
        assertEquals(2, trackCountForRoll(ChaosObjectiveType.GARRISON, 7));
        assertEquals(3, trackCountForRoll(ChaosObjectiveType.GARRISON, 9));
        assertEquals(4, trackCountForRoll(ChaosObjectiveType.GARRISON, 10));
        assertEquals(5, trackCountForRoll(ChaosObjectiveType.GARRISON, 12));

        // CADRE_DUTY shares the garrison/retainer table.
        assertEquals(0, trackCountForRoll(ChaosObjectiveType.CADRE_DUTY, 3));
        assertEquals(5, trackCountForRoll(ChaosObjectiveType.CADRE_DUTY, 12));
    }

    @Test
    void raidAndExpeditionFamilyScalesFromOneToThreeTracks() {
        assertEquals(1, trackCountForRoll(ChaosObjectiveType.RAID, 2));
        assertEquals(1, trackCountForRoll(ChaosObjectiveType.RAID, 8));
        assertEquals(2, trackCountForRoll(ChaosObjectiveType.RAID, 9));
        assertEquals(2, trackCountForRoll(ChaosObjectiveType.RAID, 11));
        assertEquals(3, trackCountForRoll(ChaosObjectiveType.RAID, 12));

        // The whole light-forces family maps through the same table.
        assertEquals(1, trackCountForRoll(ChaosObjectiveType.EXPEDITION, 2));
        assertEquals(1, trackCountForRoll(ChaosObjectiveType.PIRATE_HUNT, 8));
        assertEquals(2, trackCountForRoll(ChaosObjectiveType.GUERILLA_OPERATION, 9));
        assertEquals(3, trackCountForRoll(ChaosObjectiveType.PIRATE_RAID, 12));
    }
}
