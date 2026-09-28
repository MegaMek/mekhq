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
package mekhq.campaign;

import static mekhq.campaign.personnel.skills.SkillType.EXP_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.location.LocationNewDayUtil;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Characterization tests that pin down what {@link Campaign#fixPart} and {@link Campaign#fixWarehousePart} do today
 * when a Regular tech works on a real Locust LCT-1V: the part's state, the tech's minutes and overtime, the AsTech
 * pool, XP and money, and how a job longer than one day is carried over to the next.
 *
 * <p>The repair skill check rolls through MegaMek's {@code Compute}, so each case fixes the roll with
 * {@link FixedDieRolls}: every die shows the same face, so a face of 3 is a roll of 6. Known bugs are asserted as
 * they behave today and name their audit finding; such a case changes when that finding is fixed.</p>
 *
 * <p>Unless a case says otherwise the campaign runs on default options (overtime off, destroy-by-margin off), the
 * tech starts the day with 480 minutes and 240 minutes of overtime, and a full team of six AsTechs is available,
 * giving an AsTech pool of 2,880 minutes. A damaged medium laser has a target of 3, a damaged actuator 6.</p>
 */
class RepairEngineCharacterizationTest {
    private static final int FULL_ASTECH_TEAM = 6;
    private static final int FULL_ASTECH_POOL_MINUTES = 2880;
    private static final int FULL_SHIFT_MINUTES = 480;
    private static final int FULL_OVERTIME_MINUTES = 240;
    private static final int ACTUATOR_REPAIR_MINUTES = 120;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        tech = scenario.withTech(EXP_REGULAR);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private String fixWithEveryDieShowing(Part part, int face) {
        FixedDieRolls.everyDieShows(face);
        return campaign.fixPart(part, tech);
    }

    private int asTechPoolMinutes() {
        return campaign.getPlayerForce().getHumanResources().getAsTechPoolMinutes();
    }

    private EquipmentPart mediumLaser() {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(locust, EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Locust LCT-1V fixture has no Medium Laser");
    }

    private EquipmentPart damagedMediumLaser() {
        EquipmentPart laser = mediumLaser();
        laser.setHits(1);
        return laser;
    }

    private MekActuator damagedActuator() {
        MekActuator actuator = PartsScenario.unitParts(locust, MekActuator.class).getFirst();
        actuator.setHits(1);
        return actuator;
    }

    /**
     * Destroys the Locust's medium laser and stocks the given number of spare lasers in the warehouse.
     */
    private MissingPart missingMediumLaserWithSpares(int spareCount) {
        EquipmentPart laser = mediumLaser();
        Part spareLaser = laser.clone();
        laser.remove(false);
        scenario.withSpare(spareLaser, spareCount);
        return PartsScenario.unitParts(locust, MissingPart.class).getFirst();
    }

    /**
     * Starts the actuator repair with only 30 minutes left in the tech's day, so the job carries over.
     */
    private MekActuator actuatorCarriedOverAfterThirtyMinutes() {
        MekActuator actuator = damagedActuator();
        tech.setMinutesLeft(30);
        fixWithEveryDieShowing(actuator, 3);
        return actuator;
    }

    @Test
    void successfulRepairFixesThePartAndSpendsTechAndAsTechTime() {
        EquipmentPart laser = damagedMediumLaser();

        fixWithEveryDieShowing(laser, 3);

        assertEquals(0, laser.getHits());
        assertFalse(laser.needsFixing());
        assertNull(laser.getTech());
        assertEquals(FULL_SHIFT_MINUTES - 100, tech.getMinutesLeft());
        assertEquals(FULL_OVERTIME_MINUTES, tech.getOvertimeLeft());
        assertEquals(FULL_ASTECH_POOL_MINUTES - (100 * FULL_ASTECH_TEAM), asTechPoolMinutes());
        assertEquals(1, tech.getNTasks());
        assertEquals(0, tech.getXP());
    }

    @Test
    void failedRepairRaisesTheSkillNeededAndStillSpendsTheTime() {
        EquipmentPart laser = damagedMediumLaser();

        fixWithEveryDieShowing(laser, 1);

        assertEquals(1, laser.getHits());
        assertEquals(EXP_REGULAR + 1, laser.getSkillMin());
        assertEquals(0, laser.getTimeSpent());
        assertNull(laser.getTech());
        assertEquals(FULL_SHIFT_MINUTES - 100, tech.getMinutesLeft());
        assertEquals(FULL_ASTECH_POOL_MINUTES - (100 * FULL_ASTECH_TEAM), asTechPoolMinutes());
        assertEquals(0, tech.getNTasks());
        assertEquals("Task is beyond this tech's skill level", campaign.getTargetFor(laser, tech).getDesc());
    }

    @Test
    void rollOfTwelveAwardsTheSuccessXp() {
        campaign.getCampaignOptions().set(CampaignOption.SUCCESS_XP, 2);
        EquipmentPart laser = damagedMediumLaser();

        fixWithEveryDieShowing(laser, 6);

        assertEquals(2, tech.getXP());
        assertEquals(1, tech.getNTasks());
    }

    @Test
    void theTwentyFifthTaskAwardsTheTaskXpAndRestartsTheCount() {
        tech.setNTasks(24);
        EquipmentPart laser = damagedMediumLaser();

        fixWithEveryDieShowing(laser, 3);

        assertEquals(1, tech.getXP());
        assertEquals(0, tech.getNTasks());
    }

    @Test
    void successfulReplacementInstallsASpareAndUsesItUp() {
        MissingPart placeholder = missingMediumLaserWithSpares(2);
        int replacementMinutes = placeholder.getTimeLeft();

        fixWithEveryDieShowing(placeholder, 3);

        assertTrue(PartsScenario.unitParts(locust, MissingPart.class).isEmpty());
        assertEquals(0, mediumLaser().getHits());
        assertEquals(1, scenario.countSpareParts(EquipmentPart.class));
        assertEquals(FULL_SHIFT_MINUTES - replacementMinutes, tech.getMinutesLeft());
    }

    @Test
    void failedReplacementKeepsThePlaceholderAndTheSpares() {
        MissingPart placeholder = missingMediumLaserWithSpares(2);

        fixWithEveryDieShowing(placeholder, 1);

        assertSame(placeholder, PartsScenario.unitParts(locust, MissingPart.class).getFirst());
        assertEquals(EXP_REGULAR + 1, placeholder.getSkillMin());
        assertEquals(2, scenario.countSpareParts(EquipmentPart.class));
    }

    @Test
    void armorRepairWithNoArmorInStockIsSuspendedWithoutSpendingTime() {
        Armor armor = PartsScenario.unitParts(locust, Armor.class).getFirst();
        int fullAmount = armor.getAmount();
        armor.setAmount(fullAmount - 4);
        armor.setAmountNeeded(4);

        String report = fixWithEveryDieShowing(armor, 6);

        assertTrue(report.contains("Task suspended"), report);
        assertEquals(fullAmount - 4, armor.getAmount());
        assertEquals(FULL_SHIFT_MINUTES, tech.getMinutesLeft());
        assertEquals(FULL_ASTECH_POOL_MINUTES, asTechPoolMinutes());
    }

    @Test
    void payForRepairsDebitsAFifthOfThePartValue() {
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_REPAIRS, true);
        campaign.getPlayerForce().getFinances().credit(TransactionType.MISCELLANEOUS, campaign.getLocalDate(),
              Money.of(100_000), "test funds");
        EquipmentPart laser = damagedMediumLaser();

        fixWithEveryDieShowing(laser, 3);

        // a fifth of the laser's undamaged value, which comes to 20,000 C-bills here
        assertEquals(Money.of(96_000), campaign.getPlayerForce().getFinances().getBalance());
    }

    @Test
    void payForRepairsWithAnEmptyAccountRepairsForFree() {
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_REPAIRS, true);
        EquipmentPart laser = damagedMediumLaser();

        String report = fixWithEveryDieShowing(laser, 3);

        // Current behaviour: the refused debit is ignored, so the part is fixed and the report still quotes a cost
        assertEquals(0, laser.getHits());
        assertTrue(report.contains("Repairs cost"), report);
        assertEquals(Money.of(0), campaign.getPlayerForce().getFinances().getBalance());
    }

    @Test
    void destroyByMarginFailureInsideTheMarginLeavesThePartWorkable() {
        campaign.getCampaignOptions().set(CampaignOption.DESTROY_BY_MARGIN, true);
        MekActuator actuator = damagedActuator();

        // A roll of 4 against 6 misses by 2, less than the default margin of 4
        fixWithEveryDieShowing(actuator, 2);

        assertEquals(EXP_GREEN, actuator.getSkillMin());
        assertEquals(6, campaign.getTargetFor(actuator, tech).getValue());
    }

    @Test
    void destroyByMarginFailureAtTheMarginMakesThePartImpossible() {
        campaign.getCampaignOptions().set(CampaignOption.DESTROY_BY_MARGIN, true);
        MekActuator actuator = damagedActuator();

        // A roll of 2 against 6 misses by exactly the default margin of 4
        fixWithEveryDieShowing(actuator, 1);

        assertEquals(EXP_LEGENDARY + 1, actuator.getSkillMin());
        assertEquals("Task is impossible.", campaign.getTargetFor(actuator, tech).getDesc());
    }

    @Test
    void jobLongerThanTheDayCarriesOverWithTheTechAssigned() {
        MekActuator actuator = actuatorCarriedOverAfterThirtyMinutes();

        assertEquals(1, actuator.getHits());
        assertEquals(30, actuator.getTimeSpent());
        assertEquals(ACTUATOR_REPAIR_MINUTES - 30, actuator.getTimeLeft());
        assertSame(tech, actuator.getTech());
        assertEquals(0, tech.getMinutesLeft());
        assertFalse(actuator.hasWorkedOvertime());
        // Current behaviour, see REP-05: the 30 minutes worked today cost no AsTech time; changes when that is fixed
        assertEquals(FULL_ASTECH_POOL_MINUTES, asTechPoolMinutes());
    }

    @Test
    void carriedOverJobIsFinishedByTheNewDayPass() {
        MekActuator actuator = actuatorCarriedOverAfterThirtyMinutes();
        tech.resetMinutesLeft(false);
        campaign.getPlayerForce().getHumanResources().resetAsTechMinutes(campaign.getCampaignOptions());

        FixedDieRolls.everyDieShows(3);
        LocationNewDayUtil.processAllLocationUnits(campaign);

        assertEquals(0, actuator.getHits());
        assertNull(actuator.getTech());
        assertEquals(FULL_SHIFT_MINUTES - (ACTUATOR_REPAIR_MINUTES - 30), tech.getMinutesLeft());
        assertEquals(FULL_ASTECH_POOL_MINUTES - ((ACTUATOR_REPAIR_MINUTES - 30) * FULL_ASTECH_TEAM),
              asTechPoolMinutes());
    }

    @Test
    void jobThatFitsInOvertimeIsFinishedTheSameDay() {
        campaign.setOvertime(true);
        MekActuator actuator = damagedActuator();
        tech.setMinutesLeft(30);

        // A roll of 10 beats the target of 9, which is 6 plus 3 for overtime
        fixWithEveryDieShowing(actuator, 5);

        assertEquals(0, actuator.getHits());
        assertEquals(0, tech.getMinutesLeft());
        assertEquals(FULL_OVERTIME_MINUTES - (ACTUATOR_REPAIR_MINUTES - 30), tech.getOvertimeLeft());
        assertEquals(FULL_ASTECH_POOL_MINUTES - (ACTUATOR_REPAIR_MINUTES * FULL_ASTECH_TEAM), asTechPoolMinutes());
    }

    @Test
    void carriedOverJobIsMarkedOvertimeEvenWhenNoOvertimeWasWorked() {
        campaign.setOvertime(true);
        MekActuator actuator = damagedActuator();
        tech.setMinutesLeft(30);
        tech.setOvertimeLeft(0);

        fixWithEveryDieShowing(actuator, 3);
        tech.resetMinutesLeft(false);

        // Current behaviour, see REP-06: no overtime was worked, yet tomorrow's roll carries +3; changes when fixed
        assertTrue(actuator.hasWorkedOvertime());
        assertEquals(9, campaign.getTargetFor(actuator, tech).getValue());
    }

    @Test
    void carriedOverJobLosesItsProgressWhenTheTechHasNoTimeLeft() {
        MekActuator actuator = actuatorCarriedOverAfterThirtyMinutes();
        // maintenance used the tech's whole day before the overnight pass reached the job
        tech.setMinutesLeft(0);

        String report = fixWithEveryDieShowing(actuator, 3);

        // Current behaviour, see REP-04: the 30 minutes already worked are thrown away; changes when that is fixed
        assertTrue(report.contains("no time left after maintenance"), report);
        assertEquals(0, actuator.getTimeSpent());
        assertNull(actuator.getTech());
        assertEquals(1, actuator.getHits());
    }

    @Test
    void carriedOverReplacementReservesOneSpareFromTheStack() {
        MissingPart placeholder = missingMediumLaserWithSpares(2);
        tech.setMinutesLeft(30);

        fixWithEveryDieShowing(placeholder, 3);

        assertSame(tech, placeholder.getTech());
        assertTrue(placeholder.hasReplacementPart());
        assertTrue(placeholder.getReplacementPart().isReservedForReplacement());
        assertEquals(1, placeholder.getReplacementPart().getQuantity());
        // the reserved laser no longer counts as a spare; only the other one does
        assertEquals(1, scenario.countSpareParts(EquipmentPart.class));
    }

    @Test
    void carriedOverJobThatBecameImpossibleStillRollsAndFails() {
        MissingPart placeholder = missingMediumLaserWithSpares(2);
        tech.setMinutesLeft(30);
        fixWithEveryDieShowing(placeholder, 3);
        tech.resetMinutesLeft(false);
        campaign.getCampaignOptions().set(CampaignOption.TECHS_NEED_TOOL_KIT, true);

        String report = fixWithEveryDieShowing(placeholder, 6);

        // Current behaviour, see REP-03: an impossible task is rolled anyway and fails even on a 12; changes when
        // that is fixed
        assertTrue(report.contains("Impossible"), report);
        assertEquals(EXP_REGULAR + 1, placeholder.getSkillMin());
        assertSame(placeholder, PartsScenario.unitParts(locust, MissingPart.class).getFirst());
        assertEquals(2, scenario.countSpareParts(EquipmentPart.class));
    }

    @Test
    void warehouseRepairSplitsOneSpareOffTheStackAndRepairsIt() {
        EquipmentPart spareLaser = (EquipmentPart) mediumLaser().clone();
        spareLaser.setHits(1);
        scenario.withSpare(spareLaser, 3);
        Part damagedStack = scenario.getSpareParts().getFirst();

        FixedDieRolls.everyDieShows(3);
        Part repairedLaser = campaign.fixWarehousePart(damagedStack, tech);

        assertEquals(2, damagedStack.getQuantity());
        assertEquals(1, damagedStack.getHits());
        assertEquals(0, repairedLaser.getHits());
        assertEquals(3, scenario.countSpareParts(EquipmentPart.class));
        assertEquals(2, scenario.getSpareParts().size());
    }
}
