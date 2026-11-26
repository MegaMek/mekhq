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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
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
package mekhq.campaign.personnel.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import megamek.common.compute.Compute;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.ScoutingSkills;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.TechnicianSkills;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

class DefaultSkillGeneratorTest {
    @BeforeAll
    static void initializeSkillTypes() {
        SkillType.initializeTypes();
    }

    private static Person createPerson(PersonnelRole primaryRole, PersonnelRole secondaryRole) {
        Person person = new Person("GivenName", "Surname", null, "MERC");
        person.setPrimaryRoleDirect(primaryRole);
        person.setSecondaryRoleDirect(secondaryRole);
        return person;
    }

    private static void generateMedicalSupportSkills(Person person, int cyberneticsRoll) {
        RandomSkillPreferences preferences = new RandomSkillPreferences();
        preferences.setRandomizeSkill(false);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.d6(1)).thenReturn(cyberneticsRoll);

            new DefaultSkillGenerator(preferences).generateMedicalSupportSkills(person, SkillType.EXP_REGULAR, 0);
        }
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "DOCTOR", "MEDIC" })
    void medicalStaffReceiveCyberneticsOnSuccessfulRoll(PersonnelRole role) {
        Person person = createPerson(role, PersonnelRole.NONE);

        generateMedicalSupportSkills(person, 1);

        assertTrue(person.hasSkill(SkillType.S_TECH_CYBERNETICS));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "DOCTOR", "MEDIC" })
    void medicalStaffDoNotReceiveCyberneticsOnFailedRoll(PersonnelRole role) {
        Person person = createPerson(role, PersonnelRole.NONE);

        generateMedicalSupportSkills(person, 2);

        assertFalse(person.hasSkill(SkillType.S_TECH_CYBERNETICS));
    }

    @Test
    void doctorsAlwaysReceiveMedTech() {
        Person person = createPerson(PersonnelRole.DOCTOR, PersonnelRole.NONE);

        generateMedicalSupportSkills(person, 2);

        assertTrue(person.hasSkill(SkillType.S_MEDTECH));
    }

    @Test
    void secondaryDoctorsReceiveMedTech() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.DOCTOR);

        generateMedicalSupportSkills(person, 2);

        assertTrue(person.hasSkill(SkillType.S_MEDTECH));
    }

    private static void generateVesselSupportSkills(Person person, int zeroGravityRoll) {
        RandomSkillPreferences preferences = new RandomSkillPreferences();
        preferences.setRandomizeSkill(false);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.d6(1)).thenReturn(zeroGravityRoll);

            new DefaultSkillGenerator(preferences).generateVesselSupportSkills(person, SkillType.EXP_REGULAR, 0);
        }
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class,
          names = { "VESSEL_PILOT", "VESSEL_GUNNER", "VESSEL_CREW", "VESSEL_NAVIGATOR" })
    void vesselPersonnelReceiveZeroGravityOperationsOnSuccessfulRoll(PersonnelRole role) {
        Person person = createPerson(role, PersonnelRole.NONE);

        generateVesselSupportSkills(person, 1);

        assertTrue(person.hasSkill(SkillType.S_ZERO_G_OPERATIONS));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class,
          names = { "VESSEL_PILOT", "VESSEL_GUNNER", "VESSEL_CREW", "VESSEL_NAVIGATOR" })
    void vesselPersonnelDoNotReceiveZeroGravityOperationsOnFailedRoll(PersonnelRole role) {
        Person person = createPerson(role, PersonnelRole.NONE);

        generateVesselSupportSkills(person, 2);

        assertFalse(person.hasSkill(SkillType.S_ZERO_G_OPERATIONS));
    }

    @Test
    void secondaryVesselPersonnelReceiveZeroGravityOperations() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.VESSEL_NAVIGATOR);

        generateVesselSupportSkills(person, 1);

        assertTrue(person.hasSkill(SkillType.S_ZERO_G_OPERATIONS));
    }

    @Test
    void nonVesselPersonnelDoNotReceiveZeroGravityOperations() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);

        generateVesselSupportSkills(person, 1);

        assertFalse(person.hasSkill(SkillType.S_ZERO_G_OPERATIONS));
    }

    private static void generateScoutingSkills(Person person, int scoutingRoll) {
        RandomSkillPreferences preferences = new RandomSkillPreferences();
        preferences.setRandomizeSkill(false);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.d6(1)).thenReturn(scoutingRoll);

            new DefaultSkillGenerator(preferences).generateScoutingSkills(person, SkillType.EXP_REGULAR, 0);
        }
    }

    @Test
    void combatPersonnelReceiveScoutingSkillsOnSuccessfulRoll() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);

        generateScoutingSkills(person, 1);

        for (String skillName : ScoutingSkills.SCOUTING_SKILLS) {
            assertTrue(person.hasSkill(skillName), skillName);
        }
    }

    @Test
    void combatPersonnelDoNotReceiveScoutingSkillsOnFailedRoll() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);

        generateScoutingSkills(person, 2);

        for (String skillName : ScoutingSkills.SCOUTING_SKILLS) {
            assertFalse(person.hasSkill(skillName), skillName);
        }
    }

    @Test
    void secondaryCombatPersonnelReceiveScoutingSkills() {
        Person person = createPerson(PersonnelRole.ADMINISTRATOR, PersonnelRole.SOLDIER);

        generateScoutingSkills(person, 1);

        for (String skillName : ScoutingSkills.SCOUTING_SKILLS) {
            assertTrue(person.hasSkill(skillName), skillName);
        }
    }

    @Test
    void supportPersonnelDoNotReceiveScoutingSkills() {
        Person person = createPerson(PersonnelRole.ADMINISTRATOR, PersonnelRole.NONE);

        generateScoutingSkills(person, 1);

        for (String skillName : ScoutingSkills.SCOUTING_SKILLS) {
            assertFalse(person.hasSkill(skillName), skillName);
        }
    }

    @Test
    void existingScoutingSkillIsNotReplaced() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);
        person.addSkill(SkillType.S_PERCEPTION, 5, 0);

        generateScoutingSkills(person, 1);

        assertEquals(5, person.getSkill(SkillType.S_PERCEPTION).getLevel());
    }

    private static void generateTechSpecializationSkills(Person person, int specializationRoll) {
        RandomSkillPreferences preferences = new RandomSkillPreferences();
        preferences.setRandomizeSkill(false);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.d6(1)).thenReturn(specializationRoll);

            new DefaultSkillGenerator(preferences).generateTechSpecializationSkills(person, SkillType.EXP_REGULAR, 0);
        }
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "MEK_TECH", "MECHANIC", "AERO_TEK", "BA_TECH", "VESSEL_CREW" })
    void techsReceiveSpecializationSkillsOnSuccessfulRoll(PersonnelRole role) {
        Person person = createPerson(role, PersonnelRole.NONE);

        generateTechSpecializationSkills(person, 3);

        for (String skillName : TechnicianSkills.getTechSupplementalSkills(person, false)) {
            assertTrue(person.hasSkill(skillName), skillName);
        }
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelRole.class, names = { "MEK_TECH", "MECHANIC", "AERO_TEK", "BA_TECH", "VESSEL_CREW" })
    void techsDoNotReceiveSpecializationSkillsOnFailedRoll(PersonnelRole role) {
        Person person = createPerson(role, PersonnelRole.NONE);

        generateTechSpecializationSkills(person, 4);

        for (String skillName : TechnicianSkills.getTechSupplementalSkills(person, false)) {
            assertFalse(person.hasSkill(skillName), skillName);
        }
    }

    @Test
    void secondaryTechsReceiveSpecializationSkills() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.MEK_TECH);

        generateTechSpecializationSkills(person, 1);

        assertTrue(person.hasSkill(SkillType.S_TECH_MYOMER));
    }

    @Test
    void nonTechsDoNotReceiveSpecializationSkills() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);

        generateTechSpecializationSkills(person, 1);

        assertFalse(person.hasSkill(SkillType.S_TECH_ELECTRONIC));
        assertFalse(person.hasSkill(SkillType.S_TECH_MECHANICAL));
    }

    @Test
    void existingSpecializationSkillIsNotReplaced() {
        Person person = createPerson(PersonnelRole.MEK_TECH, PersonnelRole.NONE);
        person.addSkill(SkillType.S_TECH_MYOMER, 5, 0);

        generateTechSpecializationSkills(person, 1);

        assertEquals(5, person.getSkill(SkillType.S_TECH_MYOMER).getLevel());
    }

    @Test
    void nonMedicalStaffReceiveNoMedicalSupportSkills() {
        Person person = createPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);

        generateMedicalSupportSkills(person, 1);

        assertFalse(person.hasSkill(SkillType.S_MEDTECH));
        assertFalse(person.hasSkill(SkillType.S_TECH_CYBERNETICS));
    }


    @Test
    void generatedEdgeDoesNotExceedCampaignMaximum() {
        RandomSkillPreferences preferences = new RandomSkillPreferences();
        preferences.setUseAttributes(true);
        preferences.setRandomizeAttributes(true);
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_EDGE, true);
        options.set(CampaignOption.MAXIMUM_EDGE, 0);
        Person person = new Person("GivenName", "Surname", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.MEKWARRIOR);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.d6(2)).thenReturn(12);

            new DefaultSkillGenerator(preferences).generateAttributes(person, options);
        }

        assertEquals(0, person.getEdge());
    }

    @Test
    void generatedEdgeRemainsZeroWhenEdgeIsDisabled() {
        RandomSkillPreferences preferences = new RandomSkillPreferences();
        preferences.setUseAttributes(true);
        preferences.setRandomizeAttributes(true);
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.USE_EDGE, false);
        Person person = new Person("GivenName", "Surname", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.MEKWARRIOR);
        person.setEdge(5);

        try (MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            compute.when(() -> Compute.d6(2)).thenReturn(12);

            new DefaultSkillGenerator(preferences).generateAttributes(person, options);
        }

        assertEquals(0, person.getEdge());
    }
}
