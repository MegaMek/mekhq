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
package mekhq.campaign.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;

import megamek.common.equipment.AmmoType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Ammunition a refit has set aside counts towards "in use" by the ton, like the bins it will fill, not by the shot
 * (issue #9572: one campaign showed 388 tons of machine gun ammunition in use for 7 tons of bins).
 *
 * <p>Refitting a Wolverine WVR-6M into the WVR-6R adds an AC/5 and its one-ton bin, loaded from 20 rounds in
 * stock.</p>
 */
class PartsInUseRefitAmmoTest {
    private static final int AUTOCANNON_ROUNDS_IN_STOCK = 20;

    @Test
    void ammunitionSetAsideForARefitCountsByTheTon() throws Exception {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        campaign.getCampaignOptions().set(CampaignOption.PAY_FOR_PARTS, true);
        campaign.getPlayerForce()
              .getFinances()
              .credit(TransactionType.STARTING_CAPITAL, campaign.getLocalDate(), Money.of(10000000), "Test funds");

        Unit donor = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        AmmoType autocannonAmmo = null;
        for (Part part : new ArrayList<>(donor.getParts())) {
            if ("AC/5".equals(part.getName())) {
                scenario.withSpare(part.clone(), 1);
            }
            if ((part instanceof AmmoBin ammoBin) && ammoBin.getName().startsWith("AC/5 Ammo")) {
                autocannonAmmo = ammoBin.getType();
            }
        }
        assertNotNull(autocannonAmmo, "The WVR-6R carries AC/5 ammunition");
        campaign.getQuartermaster().addAmmo(autocannonAmmo, AUTOCANNON_ROUNDS_IN_STOCK);
        campaign.getPlayerForce().getHangar().removeUnit(donor.getId());

        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6M);
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6R.loadEntity(), true, false, false);
        refit.begin();

        PartInUse autocannonAmmoInUse = null;
        for (PartInUse partInUse : new PartsInUseManager(campaign).getPartsInUse(false, false,
              PartQuality.QUALITY_A)) {
            if (partInUse.getDescription().startsWith("AC/5 Ammo")) {
                autocannonAmmoInUse = partInUse;
            }
        }
        assertNotNull(autocannonAmmoInUse, "AC/5 ammunition is listed as in use");
        // Measured: 2 for the bins, plus 1 for the 20 rounds set aside; counting the rounds one by one gave 22
        assertEquals(3, autocannonAmmoInUse.getUseCount(), "20 AC/5 rounds set aside count as one ton");
        assertEquals(0, autocannonAmmoInUse.getStoreCount(), "The rounds are set aside, not free stock");
    }
}
