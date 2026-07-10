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

class SpecialNegotiationsModifierTest {
    private static void assertModifiers(EmployerModifierData data, double tempo, int command, int salvage,
          int support, int transport) {
        assertEquals(tempo, data.getTempoMultiplier(), "tempoMultiplier");
        assertEquals(command, data.getCommandModifier(), "commandModifier");
        assertEquals(salvage, data.getSalvageModifier(), "salvageModifier");
        assertEquals(support, data.getSupportModifier(), "supportModifier");
        assertEquals(transport, data.getTransportModifier(), "transportModifier");
    }

    // ---- Accessors --------------------------------------------------------------------------

    @Test
    public void testAccessors_HighRisk() {
        SpecialNegotiationsModifier entry = SpecialNegotiationsModifier.OBJECTIVE_HIGH_RISK;

        assertEquals(0.5, entry.getTempoMultiplier());
        assertEquals(-1, entry.getCommandModifier());
        assertEquals(-2, entry.getSalvageModifier());
        assertEquals(1, entry.getSupportModifier());
        assertEquals(0, entry.getTransportModifier());
    }

    @Test
    public void testAccessors_Covert() {
        SpecialNegotiationsModifier entry = SpecialNegotiationsModifier.OBJECTIVE_COVERT;

        assertEquals(0.3, entry.getTempoMultiplier());
        assertEquals(1, entry.getCommandModifier());
        assertEquals(1, entry.getSalvageModifier());
        assertEquals(-1, entry.getSupportModifier());
        assertEquals(-1, entry.getTransportModifier());
    }

    // ---- applyHighRisk / applyCovert --------------------------------------------------------

    @Test
    public void testApplyHighRisk() {
        EmployerModifierData data = new EmployerModifierData();

        SpecialNegotiationsModifier.applyHighRisk(data);

        assertModifiers(data, 0.5, -1, -2, 1, 0);
    }

    @Test
    public void testApplyCovert() {
        EmployerModifierData data = new EmployerModifierData();

        SpecialNegotiationsModifier.applyCovert(data);

        assertModifiers(data, 0.3, 1, 1, -1, -1);
    }

    @Test
    public void testApply_StacksAdditively() {
        EmployerModifierData data = new EmployerModifierData();

        SpecialNegotiationsModifier.applyHighRisk(data);
        SpecialNegotiationsModifier.applyCovert(data);

        // HIGH_RISK (0.5, -1,-2,1,0) + COVERT (0.3, 1,1,-1,-1)
        assertModifiers(data, 0.8, 0, -1, 0, -1);
    }

    @Test
    public void testApplyHighRisk_Twice_Accumulates() {
        EmployerModifierData data = new EmployerModifierData();

        SpecialNegotiationsModifier.applyHighRisk(data);
        SpecialNegotiationsModifier.applyHighRisk(data);

        assertModifiers(data, 1.0, -2, -4, 2, 0);
    }

    @Test
    public void testApply_LeavesEmploymentMultiplierUntouched() {
        // The enum's tempoMultiplier feeds modifyTempoMultiplier, so employmentMultiplier is never written.
        EmployerModifierData data = new EmployerModifierData();

        SpecialNegotiationsModifier.applyHighRisk(data);

        assertEquals(0.0, data.getEmploymentMultiplier());
    }
}
