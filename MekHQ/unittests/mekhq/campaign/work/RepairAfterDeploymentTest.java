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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.location.LocationNewDayUtil;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.FixedDieRolls;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A repair a tech has started, and cannot finish today, carries on once its Mek is back from a mission. Before, the
 * job could stay on the unit with time left that no tech ever worked (issue #7040).
 */
class RepairAfterDeploymentTest {
    private static final int SCENARIO_ID = 5;
    private static final int SUCCESSFUL_DIE_FACE = 6;
    private static final int MINUTES_WORKED_ON_THE_FIRST_DAY = 15;
    private static final int STRUCTURE_LOST = 2;
    private static final int FULL_ASTECH_TEAM = 6;

    private PartsScenario scenario;
    private Campaign campaign;
    private Unit wolverine;
    private Person tech;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        tech = scenario.withTech(SkillType.EXP_REGULAR);
        scenario.withAsTechs(FULL_ASTECH_TEAM);
        damageTheLeftArm(wolverine.getEntity());
        wolverine.runDiagnostic(false);
        FixedDieRolls.everyDieShows(SUCCESSFUL_DIE_FACE);
    }

    @AfterEach
    void restoreDice() {
        FixedDieRolls.restore();
    }

    private static void damageTheLeftArm(Entity entity) {
        entity.setInternal(entity.getOInternal(Mek.LOC_LEFT_ARM) - STRUCTURE_LOST, Mek.LOC_LEFT_ARM);
    }

    private MekLocation leftArm() {
        return PartsScenario.unitParts(wolverine, MekLocation.class)
                     .stream()
                     .filter(location -> location.getLoc() == Mek.LOC_LEFT_ARM)
                     .findFirst()
                     .orElseThrow();
    }

    private void startTheRepairWithoutFinishingIt() {
        MekLocation arm = leftArm();
        assertTrue(arm.needsFixing(), "The left arm is damaged");
        assertTrue(arm.getActualTime() > MINUTES_WORKED_ON_THE_FIRST_DAY, "The repair takes longer than the time left");
        tech.setMinutesLeft(MINUTES_WORKED_ON_THE_FIRST_DAY);
        tech.setOvertimeLeft(0);

        campaign.fixPart(arm, tech);

        assertSame(tech, arm.getTech(), "The tech carries the repair over to the next day");
        assertEquals(MINUTES_WORKED_ON_THE_FIRST_DAY, arm.getTimeSpent());
    }

    /** What the scenario resolver does to a unit that comes back: it takes the entity from the battle. */
    private void returnFromTheMission() {
        wolverine.setScenarioId(Scenario.S_DEFAULT_ID);
        Entity entityFromTheBattle = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        damageTheLeftArm(entityFromTheBattle);
        campaign.clearGameData(entityFromTheBattle);
        wolverine.setEntity(entityFromTheBattle);
        wolverine.runDiagnostic(true);
        wolverine.resetPilotAndEntity();
    }

    private void startANewDay() {
        tech.resetMinutesLeft(false);
        LocationNewDayUtil.processAllLocationUnits(campaign);
    }

    @Test
    void theRepairWaitsWhileTheMekIsDeployed() {
        startTheRepairWithoutFinishingIt();
        wolverine.setScenarioId(SCENARIO_ID);

        startANewDay();

        assertSame(tech, leftArm().getTech(), "The tech is still on the repair");
        assertEquals(MINUTES_WORKED_ON_THE_FIRST_DAY, leftArm().getTimeSpent(), "Nothing is done while it is away");
    }

    @Test
    void theRepairIsFinishedOnceTheMekIsBack() {
        startTheRepairWithoutFinishingIt();
        wolverine.setScenarioId(SCENARIO_ID);
        startANewDay();

        returnFromTheMission();
        startANewDay();

        assertFalse(leftArm().needsFixing(), "The tech finished the left arm");
    }
}
