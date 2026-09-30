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
import static testUtilities.parts.RefitKitPricing.expectedKitPrice;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit kit costs what its components cost plus 10 percent (CO p.212), with every component priced the way the
 * campaign prices it when bought on its own (issue #10196).
 *
 * <p>The Locust LCT-1V and LCT-5V share a 160 engine; the 5V swaps single heat sinks for double ones, including the
 * six built into the engine, and standard armor for ferro-fibrous. A double heat sink costs the same inside the engine
 * as outside it (TM p.277).</p>
 */
class RefitKitPriceTest {
    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    @Test
    void aKitIsPricedAtItsComponentsPlusTenPercentIncludingEngineHeatSinks() throws Exception {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_5V.loadEntity(), false, false, false);

        assertEquals(expectedKitPrice(refit), refit.getCost().round());
    }

    @Test
    void theAmmoInAKitIsPricedWithTheCampaignPriceMultipliers() throws Exception {
        campaign.getCampaignOptions().set(CampaignOption.COMMON_PART_PRICE_MULTIPLIER, 2.0);
        campaign.getCampaignOptions().set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 2.0);
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_5V.loadEntity(), false, false, false);

        assertEquals(expectedKitPrice(refit), refit.getCost().round());
    }
}
