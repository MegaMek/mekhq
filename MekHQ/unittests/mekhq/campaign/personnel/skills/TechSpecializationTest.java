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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.PersonnelOptions.TECH_ENGINEER;
import static mekhq.campaign.personnel.PersonnelOptions.TECH_MAINTAINER;
import static mekhq.campaign.personnel.PersonnelOptions.TECH_SPECIALIST_JETS;
import static mekhq.campaign.personnel.PersonnelOptions.TECH_SPECIALIST_WEAPONS;
import static mekhq.campaign.personnel.PersonnelOptions.TECH_WEAPON_SPECIALIST;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_ELECTRONIC;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_JETS;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANICAL;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MYOMER;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.work.IPartWork;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A tech's specialization: the discipline from a Tech Specialist SPA or a clear lead in one granular tech skill, then
 * the focus abilities, and whether it fits a selected job.
 */
class TechSpecializationTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    private static Person mekTech() {
        Person person = new Person("Test", "Tech", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.MEK_TECH);
        person.addSkill(S_TECH_MEK, 3, 0);
        return person;
    }

    private static void giveAbility(Person person, String ability) {
        person.getOptions().getOption(ability).setValue(true);
    }

    private static String label(String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    @Test
    void aTechSpecialistAbilityNamesTheDisciplineEvenWithFlatSkills() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_JETS, 3, 0);
        tech.addSkill(S_TECH_MYOMER, 3, 0);
        giveAbility(tech, TECH_SPECIALIST_JETS);

        assertEquals(S_TECH_JETS, TechSpecialization.getDisciplineSkill(tech, false));
    }

    @Test
    void aTechSpecialistAbilityOutranksALeadInAnotherSkill() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 7, 0);
        tech.addSkill(S_TECH_JETS, 1, 0);
        giveAbility(tech, TECH_SPECIALIST_JETS);

        assertEquals(S_TECH_JETS, TechSpecialization.getDisciplineSkill(tech, false),
              "the player's chosen specialization wins over one read from skill levels");
    }

    @Test
    void aSkillTwoLevelsAboveEveryOtherIsPickedAsTheDiscipline() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 5, 0);
        tech.addSkill(S_TECH_MECHANICAL, 3, 0);
        tech.addSkill(S_TECH_ELECTRONIC, 2, 0);

        assertEquals(S_TECH_MYOMER, TechSpecialization.getDisciplineSkill(tech, false));
    }

    @Test
    void aLeadOfOneLevelIsNotEnough() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 5, 0);
        tech.addSkill(S_TECH_MECHANICAL, 4, 0);
        tech.addSkill(S_TECH_ELECTRONIC, 1, 0);

        assertNull(TechSpecialization.getDisciplineSkill(tech, false),
              "the lead is measured against the next highest skill, not the lowest");
    }

    @Test
    void aTieIsNeverGuessed() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 5, 0);
        tech.addSkill(S_TECH_MECHANICAL, 5, 0);
        tech.addSkill(S_TECH_ELECTRONIC, 1, 0);

        assertNull(TechSpecialization.getDisciplineSkill(tech, false));
    }

    @Test
    void aSingleGranularSkillHasNothingToStandOutFrom() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 5, 0);

        assertNull(TechSpecialization.getDisciplineSkill(tech, false));
    }

    @Test
    void skillBonusesDoNotCountTowardsTheLead() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 3, 2);
        tech.addSkill(S_TECH_MECHANICAL, 3, 0);

        assertNull(TechSpecialization.getDisciplineSkill(tech, false),
              "only trained levels count, so a bonus does not make a specialization");
    }

    @Test
    void theGlobalSkillIsNotADiscipline() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MEK, 9, 0);
        tech.addSkill(S_TECH_MYOMER, 3, 0);
        tech.addSkill(S_TECH_MECHANICAL, 3, 0);

        assertNull(TechSpecialization.getDisciplineSkill(tech, false),
              "a high whole-unit skill is the profession, not a specialization");
    }

    @Test
    void withGlobalTechSkillsOnlyThereIsNoDisciplineButFocusAbilitiesRemain() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 5, 0);
        tech.addSkill(S_TECH_MECHANICAL, 1, 0);
        giveAbility(tech, TECH_ENGINEER);

        assertNull(TechSpecialization.getDisciplineSkill(tech, true));
        assertEquals(List.of(label("TechSpecialization.focus.engineer")), TechSpecialization.getLabels(tech, true));
    }

    @Test
    void someoneWithoutATechProfessionHasNoSpecialization() {
        Person person = new Person("Test", "Admin", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.ADMINISTRATOR_HR);
        person.addSkill(S_TECH_MYOMER, 5, 0);
        person.addSkill(S_TECH_MECHANICAL, 1, 0);
        giveAbility(person, TECH_ENGINEER);

        assertTrue(TechSpecialization.getLabels(person, false).isEmpty());
        assertEquals("", TechSpecialization.describe(person, false));
    }

    @Test
    void aSecondaryTechProfessionIsEnough() {
        Person person = new Person("Test", "Admin", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.ADMINISTRATOR_HR);
        person.setSecondaryRoleDirect(PersonnelRole.MECHANIC);
        person.addSkill(S_TECH_MECHANICAL, 5, 0);
        person.addSkill(S_TECH_ELECTRONIC, 2, 0);

        assertEquals(S_TECH_MECHANICAL, TechSpecialization.getDisciplineSkill(person, false));
    }

    @Test
    void theDisciplineIsListedFirstThenTheFocusAbilities() {
        Person tech = mekTech();
        tech.addSkill(S_TECH_MYOMER, 5, 0);
        tech.addSkill(S_TECH_MECHANICAL, 2, 0);
        giveAbility(tech, TECH_MAINTAINER);
        giveAbility(tech, TECH_ENGINEER);

        assertEquals(List.of(label("TechSpecialization.discipline.myomer"),
              label("TechSpecialization.focus.engineer"),
              label("TechSpecialization.focus.maintainer")), TechSpecialization.getLabels(tech, false));
    }

    @Test
    void weaponsIsOnlyListedOnceWhenItIsBothTheDisciplineAndAFocus() {
        Person tech = mekTech();
        giveAbility(tech, TECH_SPECIALIST_WEAPONS);
        giveAbility(tech, TECH_WEAPON_SPECIALIST);

        assertEquals(List.of(label("TechSpecialization.discipline.weapons")),
              TechSpecialization.getLabels(tech, false));
    }

    @Test
    void theDisciplineFitsAJobRolledWithItsSkill() {
        Person tech = mekTech();
        giveAbility(tech, TECH_SPECIALIST_JETS);
        IPartWork jetJob = mock(IPartWork.class);
        when(jetJob.isRightTechType(S_TECH_JETS)).thenReturn(true);
        IPartWork myomerJob = mock(IPartWork.class);
        when(myomerJob.isRightTechType(S_TECH_MYOMER)).thenReturn(true);

        assertTrue(TechSpecialization.isSpecialistFor(tech, false, jetJob));
        assertFalse(TechSpecialization.isSpecialistFor(tech, false, myomerJob));
        assertFalse(TechSpecialization.isSpecialistFor(tech, true, jetJob),
              "with global tech skills only, the discipline is not rolled and so fits nothing");
        assertFalse(TechSpecialization.isSpecialistFor(tech, false, null));
    }

    @Test
    void aJobThatAcceptsAnyTechSkillFitsNoDiscipline() {
        Person tech = mekTech();
        giveAbility(tech, TECH_SPECIALIST_JETS);
        IPartWork ammoReload = mock(IPartWork.class);
        when(ammoReload.isRightTechType(anyString())).thenReturn(true);

        assertFalse(TechSpecialization.isSpecialistFor(tech, false, ammoReload),
              "a job any tech skill can do is not special to any discipline");
    }

    @Test
    void anEngineerFitsARefit() {
        Person engineer = mekTech();
        giveAbility(engineer, TECH_ENGINEER);
        Person jetSpecialist = mekTech();
        giveAbility(jetSpecialist, TECH_SPECIALIST_JETS);
        Refit refit = mock(Refit.class);
        when(refit.isRightTechType(S_TECH_JETS)).thenReturn(true);

        assertTrue(TechSpecialization.isSpecialistFor(engineer, false, refit));
        assertFalse(TechSpecialization.isSpecialistFor(jetSpecialist, false, refit),
              "a refit is rolled with the global skill, so only Engineer fits it");
    }

    @Test
    void theWeaponSpecialistFitsWeaponParts() {
        Person tech = mekTech();
        giveAbility(tech, TECH_WEAPON_SPECIALIST);
        Part weapon = mock(Part.class);
        when(weapon.isWeaponSpecialistWork()).thenReturn(true);
        Part armor = mock(Part.class);
        when(armor.isArmorSpecialistWork()).thenReturn(true);

        assertTrue(TechSpecialization.isSpecialistFor(tech, false, weapon));
        assertFalse(TechSpecialization.isSpecialistFor(tech, false, armor));
    }
}
