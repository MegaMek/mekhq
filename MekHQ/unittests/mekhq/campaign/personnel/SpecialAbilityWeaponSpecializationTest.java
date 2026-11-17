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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that {@link SpecialAbility#chooseWeaponSpecialization(Person, int, int, boolean)} compares a weapon's rules
 * level against the campaign's Tech Level limit on the same scale (Intro through Unofficial), so Advanced and Clan
 * weapons on the person's unit are picked when the limit allows them.
 */
class SpecialAbilityWeaponSpecializationTest {
    private static final String ROTARY_AC_5 = "ISRotaryAC5";
    private static final String CLAN_ER_PPC = "CLERPPC";

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /**
     * @return a MekWarrior from the given faction whose unit mounts only the given equipment
     */
    private static Person mekWarriorWith(String equipmentName, String factionCode, boolean clan) {
        EquipmentType equipmentType = EquipmentType.get(equipmentName);
        assertNotNull(equipmentType, "Missing equipment " + equipmentName);

        Mounted<?> mounted = mock(Mounted.class);
        doReturn(equipmentType).when(mounted).getType();

        Entity entity = mock(Entity.class);
        doReturn(List.of(mounted)).when(entity).getEquipment();

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);

        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(factionCode);
        when(faction.isClan()).thenReturn(clan);

        Person person = mock(Person.class);
        when(person.getUnit()).thenReturn(unit);
        when(person.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(person.isClanPersonnel()).thenReturn(clan);
        when(person.getOriginFaction()).thenReturn(faction);
        return person;
    }

    @Test
    void advancedWeaponOnUnitIsPickedUnderExperimentalLimit() {
        // Legionnaire LGN-2D: in 3065 the Rotary AC/5 is Advanced (production 3062, common 3071)
        Person person = mekWarriorWith(ROTARY_AC_5, "FS", false);

        String result = SpecialAbility.chooseWeaponSpecialization(person, CampaignOptions.TECH_EXPERIMENTAL, 3065,
              false);

        assertEquals("Rotary AC/5", result);
    }

    @Test
    void clanStandardWeaponOnUnitIsPickedUnderStandardLimit() {
        // Timber Wolf Prime: in 3055 the Clan ER PPC is Standard (common since 2828)
        Person person = mekWarriorWith(CLAN_ER_PPC, "CSR", true);

        String result = SpecialAbility.chooseWeaponSpecialization(person, CampaignOptions.TECH_STANDARD, 3055,
              false);

        assertEquals("ER PPC", result);
    }

    @Test
    void advancedWeaponOnUnitIsRejectedUnderStandardLimit() {
        Person person = mekWarriorWith(ROTARY_AC_5, "FS", false);

        String result = SpecialAbility.chooseWeaponSpecialization(person, CampaignOptions.TECH_STANDARD, 3065,
              false);

        assertNotNull(result);
        assertNotEquals("Rotary AC/5", result);
    }
}
