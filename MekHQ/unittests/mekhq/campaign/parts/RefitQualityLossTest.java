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
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit costs the unit a quality level when its check fails, and another when work of Class C or higher is done
 * without a refit kit (Campaign Operations p.211). The Locust LCT-1V to LCT-1E refit is Class C.
 */
class RefitQualityLossTest {
    private static final boolean REFIT_KIT = false;
    private static final boolean CUSTOM_JOB = true;

    private PartsScenario scenario;
    private Unit locust;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_PARTS, true);
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.STARTING_CAPITAL, campaign.getLocalDate(), Money.of(1000000), "Test funds");
        locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
    }

    private Refit planRefit(Unit unit, UnitFixture target, boolean isCustomJob) {
        return new Refit(unit, target.loadEntity(), isCustomJob, false, false);
    }

    private Refit completeKitRefit(boolean isCheckFailed) throws Exception {
        Refit refit = planRefit(locust, UnitFixture.LOCUST_LCT_1E, REFIT_KIT);
        refit.begin();
        refit.find(0, 1.0);
        refit.changeQuantity(-1);
        scenario.getCampaign().getPlayerForce().getShoppingList().removeZeroQuantityFromList();
        assertTrue(refit.acquireParts());
        if (isCheckFailed) {
            refit.fail(0);
        }
        refit.succeed();
        return refit;
    }

    private void assertEveryPartIs(PartQuality expectedQuality) {
        for (Part part : locust.getParts()) {
            if (!(part instanceof MissingPart)) {
                assertEquals(expectedQuality, part.getQuality(), part.getName());
            }
        }
    }

    @Test
    void classCWithoutAKitLosesQuality() {
        assertEquals(Refit.CLASS_C, planRefit(locust, UnitFixture.LOCUST_LCT_1E, CUSTOM_JOB).getRefitClass());

        assertTrue(planRefit(locust, UnitFixture.LOCUST_LCT_1E, CUSTOM_JOB).isLosingQualityForNoKit());
        assertFalse(planRefit(locust, UnitFixture.LOCUST_LCT_1E, REFIT_KIT).isLosingQualityForNoKit(),
              "A refit kit avoids the loss");
    }

    @Test
    void fieldWorkWithoutAKitKeepsQuality() {
        // Swapping the Javelin's SRM 6s for an LRM 15 in the same torso is Class B
        Unit javelin = scenario.getCampaign()
                             .addNewUnit(UnitTestUtilities.getJavelinJVN10N(), false, 0,
                                   PartQuality.QUALITY_D);
        Refit refit = new Refit(javelin, UnitTestUtilities.getJavelinJVN10A(), CUSTOM_JOB, false,
              false);

        assertEquals(Refit.CLASS_B, refit.getRefitClass());
        assertFalse(refit.isLosingQualityForNoKit());
    }

    @Test
    void aKitRefitThatPassesKeepsQuality() throws Exception {
        completeKitRefit(false);

        assertEveryPartIs(PartQuality.QUALITY_D);
    }

    @Test
    void aFailedCheckCostsOneQualityLevel() throws Exception {
        Refit refit = completeKitRefit(true);

        assertTrue(refit.isLosingQualityForFailedCheck());
        assertEveryPartIs(PartQuality.QUALITY_C);
    }
}
