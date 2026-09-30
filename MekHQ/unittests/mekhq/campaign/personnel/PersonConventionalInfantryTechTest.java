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

import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_VEHICLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.ConvInfantry;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests which technicians may work on conventional infantry, and with which skill, both when the infantry maintain
 * themselves (the default) and when the "Mechanics Maintain Conventional Infantry" campaign option hands them to
 * Mechanics.
 *
 * @author Illiani
 * @since 0.51.01
 */
class PersonConventionalInfantryTechTest {
    /** Deliberately not the Regular level, so a Mechanic's own skill can be told apart from the stock skill. */
    private static final int MECHANIC_SKILL_LEVEL = 7;

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    // region canTech
    @Test
    void mechanicCanTechConventionalInfantry() {
        assertTrue(mechanic().canTech(new ConvInfantry()));
    }

    @Test
    void nonMechanicTechCannotTechConventionalInfantry() {
        assertFalse(mekTech().canTech(new ConvInfantry()));
    }

    @Test
    void personWithoutMechanicRoleCannotTechConventionalInfantry() {
        Person person = new Person("Test", "Soldier", null, "MERC");
        person.addSkill(S_TECH_VEHICLE, MECHANIC_SKILL_LEVEL, 0);
        person.setPrimaryRoleDirect(PersonnelRole.SOLDIER);

        assertFalse(person.canTech(new ConvInfantry()));
    }
    // endregion canTech

    // region getSkillForWorkingOn(IPartWork)
    @Test
    void selfMaintainedInfantryPartWorkUsesStockVehicleSkillForAnyone() {
        IPartWork part = vehiclePartOn(infantryUnit(true));
        int regularLevel = SkillType.getType(S_TECH_VEHICLE).getRegularLevel();

        for (Person person : new Person[] { mechanic(), mekTech(), new Person("Test", "Soldier", null, "MERC") }) {
            Skill skill = person.getSkillForWorkingOn(part);
            assertNotNull(skill);
            assertEquals(S_TECH_VEHICLE, skill.getType().getName());
            assertEquals(regularLevel, skill.getLevel(),
                  "self-maintained infantry should always use the stock Regular vehicle skill");
        }
    }

    @Test
    void techMaintainedInfantryPartWorkUsesTheMechanicsOwnSkill() {
        IPartWork part = vehiclePartOn(infantryUnit(false));

        Skill skill = mechanic().getSkillForWorkingOn(part);
        assertNotNull(skill);
        assertEquals(S_TECH_VEHICLE, skill.getType().getName());
        assertEquals(MECHANIC_SKILL_LEVEL, skill.getLevel(),
              "tech-maintained infantry should use the Mechanic's real skill, not the stock skill");
    }

    @Test
    void techMaintainedInfantryPartWorkGivesNoSkillToSomeoneWithoutTechSkills() {
        IPartWork part = vehiclePartOn(infantryUnit(false));

        assertNull(new Person("Test", "Soldier", null, "MERC").getSkillForWorkingOn(part));
    }

    @Test
    void selfMaintainedInfantryPartWorkIsAlwaysTheRightTechType() {
        IPartWork part = vehiclePartOn(infantryUnit(true));

        assertTrue(mekTech().isRightTechTypeFor(part));
    }

    @Test
    void techMaintainedInfantryPartWorkIsOnlyTheRightTechTypeForMechanics() {
        IPartWork part = vehiclePartOn(infantryUnit(false));

        assertTrue(mechanic().isRightTechTypeFor(part));
        assertFalse(mekTech().isRightTechTypeFor(part));
    }
    // endregion getSkillForWorkingOn(IPartWork)

    // region getSkillForWorkingOn(Unit) and maintenance/refit
    @Test
    void mechanicUsesVehicleSkillForInfantryUnit() {
        Skill skill = mechanic().getSkillForWorkingOn(infantryUnit(false));

        assertNotNull(skill);
        assertEquals(S_TECH_VEHICLE, skill.getType().getName());
    }

    @Test
    void mekTechHasNoSkillForInfantryUnit() {
        assertNull(mekTech().getSkillForWorkingOn(infantryUnit(false)));
    }

    @Test
    void anyoneIsTheRightProfessionForSelfMaintainedInfantry() {
        Unit unit = infantryUnit(true);

        assertTrue(mekTech().isRightTechProfessionFor(unit));
        assertNotNull(mekTech().getMaintenanceOrRefitSkill(unit));
    }

    @Test
    void onlyMechanicsAreTheRightProfessionForTechMaintainedInfantry() {
        Unit unit = infantryUnit(false);

        assertTrue(mechanic().isRightTechProfessionFor(unit));
        assertFalse(mekTech().isRightTechProfessionFor(unit));

        Skill mechanicSkill = mechanic().getMaintenanceOrRefitSkill(unit);
        assertNotNull(mechanicSkill);
        assertEquals(MECHANIC_SKILL_LEVEL, mechanicSkill.getLevel());
        assertNull(mekTech().getMaintenanceOrRefitSkill(unit));
    }
    // endregion getSkillForWorkingOn(Unit) and maintenance/refit

    private static Person mechanic() {
        Person person = new Person("Test", "Mechanic", null, "MERC");
        person.addSkill(S_TECH_VEHICLE, MECHANIC_SKILL_LEVEL, 0);
        person.setPrimaryRoleDirect(PersonnelRole.MECHANIC);
        return person;
    }

    private static Person mekTech() {
        Person person = new Person("Test", "MekTech", null, "MERC");
        person.addSkill(S_TECH_MEK, 5, 0);
        person.setPrimaryRoleDirect(PersonnelRole.MEK_TECH);
        return person;
    }

    /**
     * A conventional infantry unit that either maintains itself or is maintained by Mechanics, in a campaign using
     * granular (not global-only) tech skills.
     */
    private static Unit infantryUnit(boolean isSelfMaintained) {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_GLOBAL_TECH_SKILLS_ONLY, false);
        campaignOptions.set(CampaignOption.TECHS_MAINTAIN_CONVENTIONAL_INFANTRY, !isSelfMaintained);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(new ConvInfantry());
        when(unit.getCampaign()).thenReturn(campaign);
        when(unit.isConventionalInfantry()).thenReturn(true);
        when(unit.isSelfMaintainedInfantry()).thenReturn(isSelfMaintained);
        return unit;
    }

    /** A part on the given unit that accepts the Technician/Vehicle skill. */
    private static IPartWork vehiclePartOn(Unit unit) {
        IPartWork part = mock(IPartWork.class);
        when(part.getUnit()).thenReturn(unit);
        when(part.isRightTechType(S_TECH_VEHICLE)).thenReturn(true);
        return part;
    }
}
