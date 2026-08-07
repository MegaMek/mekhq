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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.scenarioTemplateEditor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.units.UnitType;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MissionRoleChoices}: role-set parse/format and unit-type filtering.
 */
class MissionRoleChoicesTest {

    @Test
    void selectableRolesForAUnitTypeAllFitAndRoundTrip() {
        List<MissionRole> roles = MissionRoleChoices.selectableRoles(UnitType.MEK);

        assertFalse(roles.isEmpty(), "Expected at least one selectable role for MEK");
        for (MissionRole role : roles) {
            assertTrue(role.fitsUnitType(UnitType.MEK), () -> role + " should fit MEK");
            assertEquals(role, MissionRole.parseRole(role.name()), () -> role + " should round-trip through parseRole");
        }
    }

    @Test
    void specialUnitTypesAreNotRestrictedByUnitType() {
        // Negative values represent the AtB mixed types; every round-trippable role should be offered.
        List<MissionRole> mixRoles = MissionRoleChoices.selectableRoles(-2);
        List<MissionRole> mekRoles = MissionRoleChoices.selectableRoles(UnitType.MEK);

        assertTrue(mixRoles.size() >= mekRoles.size(),
              "Special mix should offer at least as many roles as a concrete unit type");
    }

    @Test
    void toEntryJoinsNamesInOrder() {
        assertEquals("FIRE_SUPPORT,CARGO",
              MissionRoleChoices.toEntry(List.of(MissionRole.FIRE_SUPPORT, MissionRole.CARGO)));
    }

    @Test
    void fromEntryParsesCommaSeparatedRoles() {
        assertEquals(List.of(MissionRole.FIRE_SUPPORT, MissionRole.CARGO),
              MissionRoleChoices.fromEntry("FIRE_SUPPORT,CARGO"));
    }

    @Test
    void fromEntryTrimsAndSkipsUnknownOrBlankTokens() {
        assertEquals(List.of(MissionRole.RAIDER),
              MissionRoleChoices.fromEntry(" NOT_A_ROLE , RAIDER , "));
    }

    @Test
    void toEntryAndFromEntryRoundTrip() {
        List<MissionRole> roles = List.of(MissionRole.RECON, MissionRole.CAVALRY);
        assertEquals(roles, MissionRoleChoices.fromEntry(MissionRoleChoices.toEntry(roles)));
    }

    @Test
    void describeRendersReadableRoleNames() {
        assertEquals("fire support, cargo", MissionRoleChoices.describe("FIRE_SUPPORT,CARGO"));
    }

    @Test
    void describeFallsBackToRawEntryWhenNothingParses() {
        assertEquals("NONSENSE", MissionRoleChoices.describe("NONSENSE"));
    }
}
