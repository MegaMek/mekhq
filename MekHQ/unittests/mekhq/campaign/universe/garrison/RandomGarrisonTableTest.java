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
package mekhq.campaign.universe.garrison;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Verifies the Random Garrisons Table rows, roll clamping, and era modifiers.
 */
class RandomGarrisonTableTest {

    @Test
    void everyTableRowMatchesTheSourceTable() {
        assertEquals(new GarrisonComposition(2, 1, 0), RandomGarrisonTable.forModifiedRoll(2));
        assertEquals(new GarrisonComposition(2, 2, 0), RandomGarrisonTable.forModifiedRoll(3));
        assertEquals(new GarrisonComposition(3, 2, 0), RandomGarrisonTable.forModifiedRoll(4));
        assertEquals(new GarrisonComposition(3, 3, 0), RandomGarrisonTable.forModifiedRoll(5));
        assertEquals(new GarrisonComposition(4, 3, 1), RandomGarrisonTable.forModifiedRoll(6));
        assertEquals(new GarrisonComposition(4, 3, 1), RandomGarrisonTable.forModifiedRoll(7));
        assertEquals(new GarrisonComposition(5, 4, 2), RandomGarrisonTable.forModifiedRoll(8));
        assertEquals(new GarrisonComposition(6, 5, 2), RandomGarrisonTable.forModifiedRoll(9));
        assertEquals(new GarrisonComposition(7, 6, 3), RandomGarrisonTable.forModifiedRoll(10));
    }

    @Test
    void modifiedRollIsClampedToTheTableBounds() {
        // "2 or less" and "10 or more" rows absorb everything past the table edges.
        assertEquals(RandomGarrisonTable.forModifiedRoll(2), RandomGarrisonTable.forModifiedRoll(-5));
        assertEquals(RandomGarrisonTable.forModifiedRoll(2), RandomGarrisonTable.forModifiedRoll(1));
        assertEquals(RandomGarrisonTable.forModifiedRoll(10), RandomGarrisonTable.forModifiedRoll(11));
        assertEquals(RandomGarrisonTable.forModifiedRoll(10), RandomGarrisonTable.forModifiedRoll(99));
    }

    @Test
    void totalFormationsSumsTheComponents() {
        assertEquals(16, RandomGarrisonTable.forModifiedRoll(10).totalFormations()); // 7 + 6 + 3
        assertEquals(3, RandomGarrisonTable.forModifiedRoll(2).totalFormations());    // 2 + 1 + 0
    }

    @Test
    void eraModifierFollowsTheEraBands() {
        assertEquals(2, RandomGarrisonTable.eraModifier(2500));  // Age of War
        assertEquals(-2, RandomGarrisonTable.eraModifier(2650)); // Star League
        assertEquals(2, RandomGarrisonTable.eraModifier(2820));  // First/Second Succession Wars
        assertEquals(0, RandomGarrisonTable.eraModifier(3025));  // Third/Fourth Succession Wars
        assertEquals(1, RandomGarrisonTable.eraModifier(3067));  // Post-Fourth Succession War
    }

    @Test
    void eraModifierBoundariesAreInclusiveOfTheEarlierEra() {
        assertEquals(2, RandomGarrisonTable.eraModifier(2570));   // last Age of War year
        assertEquals(-2, RandomGarrisonTable.eraModifier(2571));  // first Star League year
        assertEquals(-2, RandomGarrisonTable.eraModifier(2780));  // last Star League year
        assertEquals(2, RandomGarrisonTable.eraModifier(2781));   // first Succession Wars year
        assertEquals(0, RandomGarrisonTable.eraModifier(3030));   // last Fourth Succession War year
        assertEquals(1, RandomGarrisonTable.eraModifier(3031));   // first post-war year
    }

    @Test
    void rollWithExtremeModifiersLandsOnTheClampedRows() {
        // A 2D6 roll is 2..12, so a +100 modifier always exceeds row 10 and a -100 always falls below row 2,
        // making the outcome deterministic without stubbing the dice.
        assertEquals(new GarrisonComposition(7, 6, 3), RandomGarrisonTable.roll(100));
        assertEquals(new GarrisonComposition(2, 1, 0), RandomGarrisonTable.roll(-100));
    }
}
