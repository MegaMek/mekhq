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
package mekhq.service.mrms;

import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTimeEstimate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * When Mass Repair optimizes to finish jobs today and no tech can, the job goes to the tech who will finish it
 * soonest, counting the minutes each has every day after maintenance (issue #5343). A tech whose day goes almost
 * entirely to maintaining their units would carry a long job for many days, even if assigned to the unit.
 */
class MRMSSoonestFinishTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int MINUTES_LEFT_TODAY = 30;
    private static final int WOLVERINES_MAINTAINED = 7;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private EquipmentPart damagedLaser;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_RUSH_JOB, false);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_ALLOW_CARRYOVER, true);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_OPTIMIZE_TO_COMPLETE_TODAY, true);
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_ASSIGNED_TECHS_FIRST, true);
        // Every unit's check is due every day, so a tech maintaining many units has little of each day left
        campaign.getCampaignOptions().set(CampaignOption.MAINTENANCE_CYCLE_DAYS, 1);
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                damagedLaser = equipmentPart;
            }
        }
        assertNotNull(damagedLaser, "The Locust LCT-1V fixture has a Medium Laser");
        damagedLaser.setHits(1);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    /** A tech with only a little of today left, so no one can finish the repair today. */
    private Person techWithLittleOfTodayLeft(int experienceLevel) {
        Person tech = scenario.withTech(experienceLevel);
        tech.setMinutesLeft(MINUTES_LEFT_TODAY);
        tech.setOvertimeLeft(0);
        return tech;
    }

    /** The Locust's own tech, who also maintains seven Wolverines, leaving almost none of each day free. */
    private Person busyAssignedVeteran() {
        Person veteran = techWithLittleOfTodayLeft(EXP_VETERAN);
        veteran.addTechUnit(locust);
        for (int count = 0; count < WOLVERINES_MAINTAINED; count++) {
            veteran.addTechUnit(scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R));
        }
        return veteran;
    }

    @Test
    void theRepairGoesToTheTechWhoFinishesSoonest() {
        Person busyVeteran = busyAssignedVeteran();
        Person idleRegular = techWithLittleOfTodayLeft(EXP_REGULAR);
        assertTrue(damagedLaser.getBaseTime() > MINUTES_LEFT_TODAY, "Nobody can finish the repair today");

        FixedDieRolls.everyDieShows(6);
        MRMSService.mrmsUnits(campaign, List.of(locust), new MRMSConfiguredOptions(campaign));

        assertEquals(idleRegular, damagedLaser.getTech(),
              "The idle Regular finishes long before the Veteran, whose days go to maintenance");
        assertEquals(MINUTES_LEFT_TODAY, busyVeteran.getMinutesLeft(), "The busy Veteran is left alone");
    }

    @Test
    void maintenanceIsSpreadOverItsCycle() {
        Person tech = scenario.withTech(EXP_REGULAR);
        tech.addTechUnit(scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R));
        int dailyCheckFree = WorkTimeEstimate.getMinutesFreeForJobsPerDay(campaign, tech);

        campaign.getCampaignOptions().set(CampaignOption.MAINTENANCE_CYCLE_DAYS, 7);
        int weeklyCheckFree = WorkTimeEstimate.getMinutesFreeForJobsPerDay(campaign, tech);

        assertTrue(weeklyCheckFree > dailyCheckFree,
              "A check every 7 days takes far less of each day: " + dailyCheckFree + " / " + weeklyCheckFree);
    }

    @Test
    void theDaysToFinishCountTheMinutesFreeEachDay() {
        Person busyVeteran = busyAssignedVeteran();
        Person idleRegular = techWithLittleOfTodayLeft(EXP_REGULAR);
        int jobMinutes = MINUTES_LEFT_TODAY + 480;

        int idleDays = MRMSService.estimateDaysToFinish(campaign, idleRegular, jobMinutes);
        int busyDays = MRMSService.estimateDaysToFinish(campaign, busyVeteran, jobMinutes);

        assertEquals(1, idleDays, "An idle tech finishes the rest tomorrow");
        assertTrue(busyDays > idleDays, "The busy tech needs many more days: " + busyDays);
    }
}
