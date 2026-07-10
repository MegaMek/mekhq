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
package mekhq.campaign.mission.newContract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UnitReputationNegotiationsModifierTest {
    private static void assertModifiers(EmployerModifierData data, int command, int salvage, int support,
          int transport) {
        assertEquals(command, data.getCommandModifier(), "commandModifier");
        assertEquals(salvage, data.getSalvageModifier(), "salvageModifier");
        assertEquals(support, data.getSupportModifier(), "supportModifier");
        assertEquals(transport, data.getTransportModifier(), "transportModifier");
    }

    // ---- Accessors --------------------------------------------------------------------------

    @Test
    public void testAccessors_Rating0() {
        UnitReputationNegotiationsModifier entry = UnitReputationNegotiationsModifier.UNIT_REPUTATION_RATING_0;

        assertEquals(-2, entry.getCommandModifier());
        assertEquals(-1, entry.getSalvageModifier());
        assertEquals(-1, entry.getSupportModifier());
        assertEquals(-3, entry.getTransportModifier());
    }

    @Test
    public void testAccessors_Rating10Plus() {
        UnitReputationNegotiationsModifier entry = UnitReputationNegotiationsModifier.UNIT_REPUTATION_RATING_10_PLUS;

        assertEquals(3, entry.getCommandModifier());
        assertEquals(2, entry.getSalvageModifier());
        assertEquals(2, entry.getSupportModifier());
        assertEquals(2, entry.getTransportModifier());
    }

    // ---- getNegotiationsModifier: every rating ----------------------------------------------

    @ParameterizedTest
    @CsvSource({
          // rating, command, salvage, support, transport
          "0, -2, -1, -1, -3",
          "1, -1, -1, -1, -2",
          "2, -1,  0,  0, -2",
          "3, -1,  0,  0, -1",
          "4,  0,  0,  0, -1",
          "5,  0,  0,  0,  0",
          "6,  1,  1,  0,  0",
          "7,  1,  1,  0,  0",
          "8,  1,  1,  1,  0",
          "9,  2,  2,  1,  1",
          "10, 3,  2,  2,  2"
    })
    public void testGetNegotiationsModifier(int rating, int command, int salvage, int support, int transport) {
        EmployerModifierData data = new EmployerModifierData();

        UnitReputationNegotiationsModifier.getNegotiationsModifier(rating, data);

        assertModifiers(data, command, salvage, support, transport);
    }

    // ---- getNegotiationsModifier: clamping --------------------------------------------------

    @Test
    public void testGetNegotiationsModifier_NegativeRating_ClampsToZero() {
        EmployerModifierData data = new EmployerModifierData();

        UnitReputationNegotiationsModifier.getNegotiationsModifier(-5, data);

        // Same as rating 0.
        assertModifiers(data, -2, -1, -1, -3);
    }

    @Test
    public void testGetNegotiationsModifier_RatingAboveTen_ClampsToTen() {
        EmployerModifierData data = new EmployerModifierData();

        UnitReputationNegotiationsModifier.getNegotiationsModifier(100, data);

        // Same as rating 10 (UNIT_REPUTATION_RATING_10_PLUS).
        assertModifiers(data, 3, 2, 2, 2);
    }

    // ---- getNegotiationsModifier: unrelated fields ------------------------------------------

    @Test
    public void testGetNegotiationsModifier_LeavesTempoAndEmploymentUntouched() {
        // This enum only writes the four rights modifiers; it never touches the multipliers.
        EmployerModifierData data = new EmployerModifierData();

        UnitReputationNegotiationsModifier.getNegotiationsModifier(9, data);

        assertEquals(0.0, data.getTempoMultiplier());
        assertEquals(0.0, data.getEmploymentMultiplier());
    }
}
