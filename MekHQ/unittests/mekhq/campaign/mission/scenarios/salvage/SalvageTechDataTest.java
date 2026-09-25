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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class SalvageTechDataTest {
    private final Campaign campaign = mock(Campaign.class);

    private Person tech(PersonnelRole primaryRole, PersonnelRole secondaryRole) {
        Person tech = mock(Person.class);
        when(tech.getId()).thenReturn(UUID.randomUUID());
        when(tech.getRankName()).thenReturn("Sergeant");
        when(tech.getRankNumeric()).thenReturn(7);
        when(tech.getPrimaryRole()).thenReturn(primaryRole);
        when(tech.getSecondaryRole()).thenReturn(secondaryRole);
        when(tech.getFirstName()).thenReturn("Jane");
        when(tech.getLastName()).thenReturn("Doe");
        when(tech.getCurrentEdge()).thenReturn(2);
        when(tech.getMinutesLeft()).thenReturn(480);
        when(tech.getSkillLevel(campaign, false, true)).thenReturn(SkillLevel.REGULAR);
        when(tech.getSkillLevel(campaign, true, true)).thenReturn(SkillLevel.ELITE);
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn("Atlas");
        when(tech.getTechUnits()).thenReturn(List.of(unit));
        return tech;
    }

    @Test
    void techDetailsAreCaptured() {
        Person tech = tech(PersonnelRole.MEK_TECH, PersonnelRole.NONE);

        SalvageTechData data = SalvageTechData.buildData(campaign, tech);

        assertSame(tech, data.tech());
        assertEquals(tech.getId(), data.techId());
        assertEquals("Sergeant", data.rank());
        assertEquals(7, data.rankNumeric());
        assertEquals(PersonnelRole.MEK_TECH, data.primaryRole());
        assertEquals(PersonnelRole.NONE, data.secondaryRole());
        assertEquals(List.of("Atlas"), data.techUnits());
        assertEquals("Jane", data.firstName());
        assertEquals("Doe", data.lastName());
        assertEquals(2, data.edge());
        assertEquals(480, data.minutesAvailable());
        assertEquals(SkillLevel.REGULAR.toString(), data.skillLevelName());
    }

    @Test
    void secondaryTechRoleSetsTheSkillLevel() {
        Person tech = tech(PersonnelRole.MEKWARRIOR, PersonnelRole.MEK_TECH);

        assertEquals(SkillLevel.ELITE.toString(), SalvageTechData.buildData(campaign, tech).skillLevelName());
    }

    @Test
    void techUnitsCantBeChanged() {
        SalvageTechData data = SalvageTechData.buildData(campaign, tech(PersonnelRole.MEK_TECH, PersonnelRole.NONE));

        assertThrows(UnsupportedOperationException.class, () -> data.techUnits().add("Locust"));
    }
}
