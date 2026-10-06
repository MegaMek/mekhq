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

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Mass Repair on default settings (preferred target number 4, maximum 6): Rush Job stops at the preferred target
 * number, a tie between equally skilled techs goes to the one with more time, and a task Mass Repair does not take on
 * keeps the work time the player chose (issue #10215).
 *
 * <p>With a full AsTech team, a damaged medium laser has these target numbers. Rush Job makes the roll harder and
 * also counts the tech one experience level lower per step:</p>
 * <ul>
 *     <li>Regular: 3 at normal time, 5 at half time</li>
 *     <li>Veteran: 2 at normal time, 3 at half time, 5 at quarter time</li>
 * </ul>
 */
class MRMSWorkTimeAndTechChoiceTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int FULL_SHIFT_MINUTES = 480;
    private static final int SHORT_DAY_MINUTES = 30;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private EquipmentPart damagedLaser;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_REPAIR, true);
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

    private void runMassRepair() {
        FixedDieRolls.everyDieShows(6);
        MRMSService.mrmsUnits(campaign, List.of(locust), new MRMSConfiguredOptions(campaign));
    }

    @Test
    void aVeteranRushesOnlyAsFarAsThePreferredTargetNumber() {
        Person veteran = scenario.withTech(EXP_VETERAN);
        int halfTimeMinutes = (int) Math.ceil(damagedLaser.getBaseTime() * WorkTime.RUSH_2.timeMultiplier);

        runMassRepair();

        assertEquals(0, damagedLaser.getHits());
        assertEquals(FULL_SHIFT_MINUTES - halfTimeMinutes, veteran.getMinutesLeft(),
              "Half time reaches 3, within the preferred 4; quarter time would reach 5");
    }

    @Test
    void aRegularTechDoesNotRushPastThePreferredTargetNumber() {
        Person regular = scenario.withTech(EXP_REGULAR);
        int normalMinutes = damagedLaser.getBaseTime();

        runMassRepair();

        assertEquals(0, damagedLaser.getHits());
        assertEquals(FULL_SHIFT_MINUTES - normalMinutes, regular.getMinutesLeft(),
              "Even half time would reach 5, past the preferred 4, so the job is done at normal time");
    }

    @Test
    void aTieGoesToTheTechWithMoreTimeLeft() {
        campaign.getCampaignOptions().set(CampaignOption.MRMS_USE_RUSH_JOB, false);
        Person techWithAShortDay = scenario.withTech(EXP_REGULAR);
        techWithAShortDay.setMinutesLeft(SHORT_DAY_MINUTES);
        Person techWithAFullDay = scenario.withTech(EXP_REGULAR);
        int normalMinutes = damagedLaser.getBaseTime();

        runMassRepair();

        assertEquals(SHORT_DAY_MINUTES, techWithAShortDay.getMinutesLeft(), "The tech with a short day is left alone");
        assertEquals(FULL_SHIFT_MINUTES - normalMinutes, techWithAFullDay.getMinutesLeft(),
              "The tech with the full day does the repair");
    }

    @Test
    void aTaskMassRepairLeavesAloneKeepsThePlayersWorkTime() {
        scenario.withTech(EXP_REGULAR);
        for (MRMSOption option : campaign.getCampaignOptions().get(CampaignOption.MRMS_OPTIONS)) {
            if (option.getType() == PartRepairType.WEAPON) {
                option.setActive(false);
            }
        }
        damagedLaser.setMode(WorkTime.EXTRA_2);

        runMassRepair();

        assertEquals(1, damagedLaser.getHits(), "Weapons are switched off in Mass Repair");
        assertEquals(WorkTime.EXTRA_2, damagedLaser.getMode(), "The player's choice of extra time is kept");
    }
}
