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

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.personnel.skills.SkillType.EXP_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.campaign.work.WorkTime;
import mekhq.service.mrms.MRMSService.MRMSPartSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Characterization tests that run the real Mass Repair service ({@link MRMSService}) against a real campaign, real
 * techs, the real target numbers of {@link Campaign#getTargetFor} and the real {@link Campaign#fixPart}, on a damaged
 * Locust LCT-1V. Each case records which tasks Mass Repair took, which tech it gave them to, the target number and
 * work time used, and the outcome.
 *
 * <p>The repair rolls go through MegaMek's {@code Compute}, so every case fixes them with {@link FixedDieRolls}: a
 * face of 3 is a roll of 6. Every Mass Repair option is set explicitly by {@link #configureMassRepair}, so a change to
 * the option defaults does not change these cases. Known bugs are asserted as they behave today and name their audit
 * finding; such a case changes when that finding is fixed.</p>
 *
 * <p>Not covered: MRMS-01, the endless loop when the chosen tech is at another location. Running it would hang the
 * test suite, so it waits for the fix. Salvage mode, Quick Strip and OmniPods are also not covered here.</p>
 */
class MassRepairCharacterizationTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int FULL_SHIFT_MINUTES = 480;
    private static final int LASER_REPAIR_MINUTES = 100;
    private static final int ACTUATOR_REPAIR_MINUTES = 120;
    private static final int PREFERRED_TARGET_NUMBER = 4;
    private static final int MAXIMUM_TARGET_NUMBER = 8;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    /**
     * Sets every Mass Repair option, so the cases do not depend on the defaults. Every valid repair type is active for
     * every skill level, with a preferred target number of 4 and a maximum of 8.
     */
    private void configureMassRepair(boolean useExtraTime, boolean useRushJob) {
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.MRMS_USE_REPAIR, true);
        options.set(CampaignOption.MRMS_USE_SALVAGE, true);
        options.set(CampaignOption.MRMS_USE_EXTRA_TIME, useExtraTime);
        options.set(CampaignOption.MRMS_USE_RUSH_JOB, useRushJob);
        options.set(CampaignOption.MRMS_ALLOW_CARRYOVER, true);
        options.set(CampaignOption.MRMS_OPTIMIZE_TO_COMPLETE_TODAY, false);
        options.set(CampaignOption.MRMS_SCRAP_IMPOSSIBLE, false);
        options.set(CampaignOption.MRMS_USE_ASSIGNED_TECHS_FIRST, false);
        options.set(CampaignOption.MRMS_REPLACE_POD, true);
        List<MRMSOption> repairTypeOptions = new ArrayList<>();
        for (PartRepairType repairType : PartRepairType.getMRMSValidTypes()) {
            repairTypeOptions.add(new MRMSOption(repairType, true, EXP_ULTRA_GREEN, EXP_LEGENDARY,
                  PREFERRED_TARGET_NUMBER, MAXIMUM_TARGET_NUMBER, 0));
        }
        options.set(CampaignOption.MRMS_OPTIONS, repairTypeOptions);
    }

    /**
     * Adds a tech together with a full team of six AsTechs, because Mass Repair refuses to run at all while any tech
     * is short of AsTechs.
     */
    private Person withTechAndTeam(int experienceLevel) {
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        return scenario.withTech(experienceLevel);
    }

    private EquipmentPart damagedMediumLaser() {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                equipmentPart.setHits(1);
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Locust LCT-1V fixture has no Medium Laser");
    }

    private MekActuator damagedActuator() {
        MekActuator actuator = PartsScenario.unitParts(locust, MekActuator.class).getFirst();
        actuator.setHits(1);
        return actuator;
    }

    private String massRepairTheLocustWithEveryDieShowing(int face) {
        FixedDieRolls.everyDieShows(face);
        return MRMSService.performSingleUnitMRMS(campaign, locust);
    }

    @Test
    void unitRepairFixesEachDamagedPartWithTheOnlyTechAtNormalTime() {
        configureMassRepair(false, false);
        Person tech = withTechAndTeam(EXP_REGULAR);
        EquipmentPart laser = damagedMediumLaser();
        MekActuator actuator = damagedActuator();
        assertEquals(3, campaign.getTargetFor(laser, tech).getValue());
        assertEquals(6, campaign.getTargetFor(actuator, tech).getValue());

        String summary = massRepairTheLocustWithEveryDieShowing(3);

        assertEquals("Mass Repair complete on Locust LCT-1V.", summary);
        assertEquals(0, laser.getHits());
        assertEquals(0, actuator.getHits());
        assertEquals(FULL_SHIFT_MINUTES - LASER_REPAIR_MINUTES - ACTUATOR_REPAIR_MINUTES, tech.getMinutesLeft());
        assertEquals(2, tech.getNTasks());
    }

    @Test
    void unitRepairGivesTheWorkToTheLeastSkilledTechWithinTheMaximumTarget() {
        configureMassRepair(false, false);
        Person greenTech = withTechAndTeam(EXP_GREEN);
        Person veteranTech = withTechAndTeam(EXP_VETERAN);
        EquipmentPart laser = damagedMediumLaser();
        MekActuator actuator = damagedActuator();
        assertEquals(5, campaign.getTargetFor(laser, greenTech).getValue());
        assertEquals(8, campaign.getTargetFor(actuator, greenTech).getValue());
        assertEquals(5, campaign.getTargetFor(actuator, veteranTech).getValue());

        // A roll of 8 meets the Green tech's hardest target
        massRepairTheLocustWithEveryDieShowing(4);

        assertEquals(0, laser.getHits());
        assertEquals(0, actuator.getHits());
        assertEquals(FULL_SHIFT_MINUTES - LASER_REPAIR_MINUTES - ACTUATOR_REPAIR_MINUTES, greenTech.getMinutesLeft());
        assertEquals(FULL_SHIFT_MINUTES, veteranTech.getMinutesLeft());
    }

    @Test
    void failedRollLeavesThePartDamagedButIsCountedAsARepairAction() {
        configureMassRepair(false, false);
        Person tech = withTechAndTeam(EXP_REGULAR);
        EquipmentPart laser = damagedMediumLaser();

        FixedDieRolls.everyDieShows(1);
        MRMSConfiguredOptions configuredOptions = new MRMSConfiguredOptions(campaign);
        MRMSService.mrmsUnits(campaign, List.of(locust), configuredOptions);

        // The roll of 2 fails, the task now needs a better tech than the only one, and Mass Repair stops there
        assertEquals(1, laser.getHits());
        assertEquals(EXP_REGULAR + 1, laser.getSkillMin());
        assertEquals(FULL_SHIFT_MINUTES - LASER_REPAIR_MINUTES, tech.getMinutesLeft());
        // Current behaviour: repairPart reports REPAIRED whatever fixPart did, so the daily report counts the
        // failed attempt as a performed action
        assertTrue(technicalReportContains("1 repair/salvage action performed"));
    }

    @Test
    void rushJobDoesNotRushPastThePreferredTarget() {
        configureMassRepair(false, true);
        Person tech = withTechAndTeam(EXP_REGULAR);
        EquipmentPart laser = damagedMediumLaser();

        massRepairTheLocustWithEveryDieShowing(6);

        // MRMS-02: the laser's normal target of 3 already meets the preferred 4, and the half-time job would reach
        // 5, past it, so Mass Repair does the job at normal time
        int minutesUsed = FULL_SHIFT_MINUTES - tech.getMinutesLeft();
        assertEquals(LASER_REPAIR_MINUTES, minutesUsed);
        assertEquals(0, laser.getHits());
        laser.setHits(1);
        assertEquals(3, campaign.getTargetFor(laser, tech).getValue());
        laser.setMode(WorkTime.RUSH_2);
        int rushedTarget = campaign.getTargetFor(laser, tech).getValue();
        assertEquals(5, rushedTarget);
        assertTrue(rushedTarget > PREFERRED_TARGET_NUMBER);
    }

    @Test
    void equallySkilledTechsGiveTheJobToTheTechWithTheMostTimeLeft() {
        configureMassRepair(false, false);
        Person tiredTech = withTechAndTeam(EXP_REGULAR);
        tiredTech.setMinutesLeft(30);
        Person freshTech = withTechAndTeam(EXP_REGULAR);
        MekActuator actuator = damagedActuator();

        massRepairTheLocustWithEveryDieShowing(3);

        // MRMS-08: the two-hour job goes to the tech with a full day and is finished today
        assertEquals(0, actuator.getHits());
        assertEquals(30, tiredTech.getMinutesLeft());
        assertEquals(FULL_SHIFT_MINUTES - ACTUATOR_REPAIR_MINUTES, freshTech.getMinutesLeft());
    }

    @Test
    void warehouseRepairFixesEachSpareInADamagedStack() {
        configureMassRepair(false, false);
        Person tech = withTechAndTeam(EXP_REGULAR);
        EquipmentPart spareLaser = (EquipmentPart) damagedMediumLaser().clone();
        spareLaser.setHits(1);
        scenario.withSpare(spareLaser, 2);
        Part damagedStack = scenario.getSpareParts().getFirst();
        List<IPartWork> selectedParts = List.of(damagedStack);

        FixedDieRolls.everyDieShows(3);
        MRMSPartSet partSet = MRMSService.performWarehouseMRMS(selectedParts, new MRMSConfiguredOptions(campaign),
              campaign);

        assertEquals(2, partSet.countRepairs());
        assertEquals(2, scenario.countSpareParts(EquipmentPart.class));
        for (Part sparePart : scenario.getSpareParts()) {
            assertEquals(0, sparePart.getHits(), "spare " + sparePart.getId() + " is still damaged");
            assertNull(sparePart.getTech());
        }
        assertEquals(FULL_SHIFT_MINUTES - (2 * LASER_REPAIR_MINUTES), tech.getMinutesLeft());
    }

    private boolean technicalReportContains(String text) {
        for (String report : campaign.getDailyReportLog().getLines(TECHNICAL)) {
            if (report.contains(text)) {
                return true;
            }
        }
        return false;
    }
}
