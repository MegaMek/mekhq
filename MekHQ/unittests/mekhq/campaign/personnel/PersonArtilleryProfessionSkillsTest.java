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

import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_ARTILLERY;
import static mekhq.campaign.personnel.skills.SkillType.S_GUN_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_GUN_VEE;
import static mekhq.campaign.personnel.skills.SkillType.S_PILOT_GVEE;
import static mekhq.campaign.personnel.skills.SkillType.S_PILOT_MEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that, when artillery is enabled, the better of a combat profession's gunnery skill and Artillery is used in
 * place of the gunnery skill, so personnel without Artillery are not shown as unqualified (skill level "None").
 */
class PersonArtilleryProfessionSkillsTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 1);

    @BeforeAll
    static void beforeAll() {
        SkillType.initializeTypes();
    }

    private static CampaignOptions optionsWithArtillery(boolean isUseArtillery) {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_ARTILLERY, isUseArtillery);
        campaignOptions.set(CampaignOption.USE_AGE_EFFECTS, false);
        return campaignOptions;
    }

    private static Person groundVehicleCrew() {
        Person person = new Person("Test", "Crew", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.VEHICLE_CREW_GROUND);
        person.addSkill(S_PILOT_GVEE, 3, 0);
        person.addSkill(S_GUN_VEE, 3, 0);
        return person;
    }

    @Test
    void vehicleCrewWithoutArtilleryOnlyUsesDrivingAndGunnery() {
        Person person = groundVehicleCrew();

        assertEquals(List.of(S_PILOT_GVEE, S_GUN_VEE),
              person.getProfessionSkills(optionsWithArtillery(true), false));
    }

    @Test
    void vehicleCrewWithBetterArtilleryUsesArtilleryInsteadOfGunnery() {
        Person person = groundVehicleCrew();
        person.addSkill(S_ARTILLERY, 5, 0);

        assertEquals(List.of(S_PILOT_GVEE, S_ARTILLERY),
              person.getProfessionSkills(optionsWithArtillery(true), false));
    }

    @Test
    void vehicleCrewWithWorseArtilleryUsesGunnery() {
        Person person = groundVehicleCrew();
        person.addSkill(S_ARTILLERY, 1, 0);

        assertEquals(List.of(S_PILOT_GVEE, S_GUN_VEE),
              person.getProfessionSkills(optionsWithArtillery(true), false));
    }

    @Test
    void vehicleCrewWithEqualArtilleryUsesGunnery() {
        Person person = groundVehicleCrew();
        person.addSkill(S_ARTILLERY, 3, 0);

        assertEquals(List.of(S_PILOT_GVEE, S_GUN_VEE),
              person.getProfessionSkills(optionsWithArtillery(true), false));
    }

    @Test
    void vehicleCrewWithArtilleryButNoGunneryUsesArtillery() {
        Person person = new Person("Test", "Crew", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.VEHICLE_CREW_GROUND);
        person.addSkill(S_PILOT_GVEE, 3, 0);
        person.addSkill(S_ARTILLERY, 3, 0);

        assertEquals(List.of(S_PILOT_GVEE, S_ARTILLERY),
              person.getProfessionSkills(optionsWithArtillery(true), false));
    }

    @Test
    void mekWarriorWithBetterArtilleryUsesArtilleryInsteadOfGunnery() {
        Person person = new Person("Test", "Pilot", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.MEKWARRIOR);
        person.addSkill(S_GUN_MEK, 3, 0);
        person.addSkill(S_PILOT_MEK, 3, 0);
        person.addSkill(S_ARTILLERY, 5, 0);

        assertEquals(List.of(S_ARTILLERY, S_PILOT_MEK),
              person.getProfessionSkills(optionsWithArtillery(true), false));
    }

    @Test
    void artilleryIsIgnoredWhenArtilleryIsDisabled() {
        Person person = groundVehicleCrew();
        person.addSkill(S_ARTILLERY, 5, 0);

        assertEquals(List.of(S_PILOT_GVEE, S_GUN_VEE),
              person.getProfessionSkills(optionsWithArtillery(false), false));
    }

    @Test
    void secondaryVehicleCrewWithoutArtilleryOnlyUsesDrivingAndGunnery() {
        Person person = new Person("Test", "Crew", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.NONE);
        person.setSecondaryRoleDirect(PersonnelRole.VEHICLE_CREW_GROUND);
        person.addSkill(S_PILOT_GVEE, 3, 0);
        person.addSkill(S_GUN_VEE, 3, 0);

        assertEquals(List.of(S_PILOT_GVEE, S_GUN_VEE),
              person.getProfessionSkills(optionsWithArtillery(true), true));
    }

    @Test
    void mekWarriorWithoutArtilleryOnlyUsesPilotingAndGunnery() {
        Person person = new Person("Test", "Pilot", null, "MERC");
        person.setPrimaryRoleDirect(PersonnelRole.MEKWARRIOR);
        person.addSkill(S_GUN_MEK, 3, 0);
        person.addSkill(S_PILOT_MEK, 3, 0);

        assertEquals(List.of(S_GUN_MEK, S_PILOT_MEK),
              person.getProfessionSkills(optionsWithArtillery(true), false));
    }

    @Test
    void vehicleCrewWithoutArtilleryHasTheSameExperienceLevelAsWithArtilleryDisabled() {
        Person person = groundVehicleCrew();

        int levelWithArtilleryEnabled = person.getExperienceLevel(optionsWithArtillery(true), false, TODAY, false,
              true);
        int levelWithArtilleryDisabled = person.getExperienceLevel(optionsWithArtillery(false), false, TODAY, false,
              true);

        assertNotEquals(EXP_NONE, levelWithArtilleryEnabled,
              "a vehicle crew member without Artillery should not be treated as unqualified");
        assertEquals(levelWithArtilleryDisabled, levelWithArtilleryEnabled);
    }
}
