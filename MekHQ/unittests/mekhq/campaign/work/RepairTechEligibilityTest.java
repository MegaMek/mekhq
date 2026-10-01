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
package mekhq.campaign.work;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.location.LocationNode.LocationManager;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.work.RepairTechEligibility.Refusal;
import mekhq.campaign.work.RepairTechEligibility.TechListToggles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The rules that decide which techs the Repair tab and the warehouse bench offer a task to (issue #10220).
 */
class RepairTechEligibilityTest {
    private static final TechListToggles TECHNICIANS_ONLY = new TechListToggles(false, false);

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private EquipmentPart damagedPart;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        damagedPart = PartsScenario.unitParts(locust, EquipmentPart.class).getFirst();
        damagedPart.setHits(1);
        tech = scenario.withTech(EXP_REGULAR);
    }

    private Refusal refusalFor(Person candidate, TechListToggles toggles) {
        return RepairTechEligibility.findRefusal(campaign, damagedPart, locust, candidate, toggles);
    }

    @Test
    void aRegularMekTechAtTheUnitCanTakeTheTask() {
        assertNull(refusalFor(tech, TECHNICIANS_ONLY));
    }

    @Test
    void aTaskThatNeedsNoWorkIsOfferedToNobody() {
        damagedPart.setHits(0);

        assertEquals(Refusal.TASK_NOT_NEEDED, refusalFor(tech, TECHNICIANS_ONLY));
    }

    @Test
    void aTechAtAnotherBaseIsNotOffered() {
        LocationManager.setLocation(tech, new PlayerBase(new FixedLocation(mock(PlanetarySystem.class))));

        assertEquals(Refusal.AT_ANOTHER_LOCATION, refusalFor(tech, TECHNICIANS_ONLY));
    }

    @Test
    void aTechWithNoTimeLeftIsNotOffered() {
        tech.setMinutesLeft(0);

        assertEquals(Refusal.NO_TIME_LEFT, refusalFor(tech, TECHNICIANS_ONLY));
    }

    @Test
    void aTaskThatNeedsABetterTechIsNotOfferedToARegular() {
        damagedPart.setSkillMin(EXP_VETERAN);

        assertEquals(Refusal.SKILL_TOO_LOW, refusalFor(tech, TECHNICIANS_ONLY));
        assertNull(refusalFor(scenario.withTech(EXP_VETERAN), TECHNICIANS_ONLY));
    }

    @Test
    void aMechanicIsLeftOutOnlyWhenTheListShowsUnitTechsOnly() {
        tech.setPrimaryRoleDirect(PersonnelRole.MECHANIC);

        assertEquals(Refusal.WRONG_PROFESSION, refusalFor(tech, new TechListToggles(true, false)));
        assertNull(refusalFor(tech, TECHNICIANS_ONLY));
    }

    @Test
    void someoneOutsideATechnicianRoleIsOfferedOnlyWhenTheListShowsEveryone() {
        tech.setPrimaryRoleDirect(PersonnelRole.MEKWARRIOR);

        Refusal refusal = refusalFor(tech, TECHNICIANS_ONLY);
        assertNotNull(refusal);
        assertEquals(Refusal.NOT_A_TECHNICIAN, refusal);
        assertNull(refusalFor(tech, new TechListToggles(false, true)));
    }
}
