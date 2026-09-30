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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit and its tech stay tied together: the tech is busy with nothing else until the refit is completed or
 * cancelled, a refit whose tech leaves waits for a new one instead of being cancelled, and a refitting unit that
 * leaves the campaign gives back what its refit had set aside (issue #10199).
 */
class RefitTechLifecycleTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private Unit locust;
    private Person tech;
    private Refit refit;

    @BeforeEach
    void setUp() throws Exception {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        tech = scenario.withTech(SkillType.EXP_REGULAR);
        refit = new Refit(locust, UnitFixture.LOCUST_LCT_1E.loadEntity(), false, false, false);
        refit.setTech(tech);
        refit.begin();
    }

    private void deliverKit() {
        refit.find(0, 1.0);
        refit.changeQuantity(-1);
        campaign.getPlayerForce().getShoppingList().removeZeroQuantityFromList();
    }

    /** A repair job elsewhere in the campaign that the refit tech might be asked to do. */
    private Part otherRepairJob() {
        Unit otherUnit = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        return otherUnit.getParts().getFirst();
    }

    private static boolean isBusyWithRefit(TargetRoll target) {
        return (target.getValue() == TargetRoll.IMPOSSIBLE) && target.getDesc().contains("refit of");
    }

    @Test
    void theRefitTechTakesNoOtherWorkWhileWaitingForTheKit() {
        assertFalse(refit.acquireParts(), "The kit has not been found yet");

        TargetRoll target = campaign.getTargetFor(otherRepairJob(), tech);

        assertTrue(isBusyWithRefit(target), target.getDesc());
    }

    @Test
    void theRefitTechTakesNoOtherWorkWhileWorking() {
        deliverKit();
        campaign.refit(refit);

        TargetRoll target = campaign.getTargetFor(otherRepairJob(), tech);

        assertTrue(isBusyWithRefit(target), target.getDesc());
    }

    @Test
    void theRefitTechIsFreeAgainOnceTheRefitIsCancelled() {
        Part otherJob = otherRepairJob();
        refit.cancel();

        TargetRoll target = campaign.getTargetFor(otherJob, tech);

        assertFalse(isBusyWithRefit(target), target.getDesc());
    }

    @Test
    void aRefitWhoseTechLeavesIsPausedAndANewTechCarriesOn() {
        deliverKit();
        campaign.refit(refit);
        int minutesDone = refit.getTimeSpent();
        assertTrue(minutesDone > 0);

        tech.removeAllTechJobs(campaign);
        campaign.refit(refit);

        assertSame(refit, locust.getRefit(), "The refit is paused, not cancelled");
        assertNull(refit.getTech());
        assertEquals(minutesDone, refit.getTimeSpent(), "No progress is lost");

        Person newTech = scenario.withTech(SkillType.EXP_REGULAR);
        refit.setTech(newTech);
        campaign.refit(refit);

        assertTrue(refit.getTimeSpent() > minutesDone, "The new tech carries on from where the old one stopped");
    }

    @Test
    void sellingARefittingUnitGivesBackItsPartsAndOrders() {
        deliverKit();
        assertEquals(Map.of("Medium Laser [reserved]", 1, "Small Laser [reserved]", 2),
              PartsCensus.ofWarehouseStock(scenario.getWarehouse()));

        campaign.getQuartermaster().sellUnit(locust);

        assertEquals(Map.of("Medium Laser", 1, "Small Laser", 2), PartsCensus.ofWarehouseStock(scenario.getWarehouse()),
              "The kit's parts are ordinary spares again");
    }

    @Test
    void removingARefittingUnitDropsItsKitOrder() {
        assertFalse(campaign.getPlayerForce().getShoppingList().getShoppingList().isEmpty(), "The kit is on order");

        campaign.removeUnit(locust.getId());

        assertTrue(campaign.getPlayerForce().getShoppingList().getShoppingList().isEmpty());
    }
}
