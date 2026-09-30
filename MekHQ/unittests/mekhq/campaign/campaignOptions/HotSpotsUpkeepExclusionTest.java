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
package mekhq.campaign.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Hot Spots upkeep replaces maintenance, overhead, and peacetime operating costs. The options dialog stops them being
 * combined, but presets and hand-edited saves can still combine them, so {@link CampaignOptions} enforces the exclusion
 * at runtime. Repairs are deliberately left alone.
 */
class HotSpotsUpkeepExclusionTest {
    @ParameterizedTest
    @CsvSource({ "false, false, false", "true, false, true", "false, true, false", "true, true, false" })
    void maintenanceIsOnlyChargedWithoutUpkeep(boolean payForMaintenance, boolean payForUpkeep, boolean expected) {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, payForMaintenance);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, payForUpkeep);

        assertEquals(expected, campaignOptions.isChargingMaintenance());
    }

    @ParameterizedTest
    @CsvSource({ "false, false, false", "true, false, true", "false, true, false", "true, true, false" })
    void overheadIsOnlyChargedWithoutUpkeep(boolean payForOverhead, boolean payForUpkeep, boolean expected) {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, payForOverhead);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, payForUpkeep);

        assertEquals(expected, campaignOptions.isChargingOverhead());
    }

    @ParameterizedTest
    @CsvSource({ "false, false, false", "true, false, true", "false, true, false", "true, true, false" })
    void peacetimeCostIsOnlyChargedWithoutUpkeep(boolean usePeacetimeCost, boolean payForUpkeep, boolean expected) {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_PEACETIME_COST, usePeacetimeCost);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, payForUpkeep);

        assertEquals(expected, campaignOptions.isChargingPeacetimeCost());
    }

    /** The exclusion must not rewrite the stored options, so turning upkeep back off restores the player's choices. */
    @Test
    void upkeepDoesNotOverwriteTheUnderlyingOptions() {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, true);
        campaignOptions.set(CampaignOption.USE_PEACETIME_COST, true);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, true);

        assertTrue(campaignOptions.get(CampaignOption.PAY_FOR_MAINTAIN));
        assertTrue(campaignOptions.get(CampaignOption.PAY_FOR_OVERHEAD));
        assertTrue(campaignOptions.get(CampaignOption.USE_PEACETIME_COST));

        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);

        assertTrue(campaignOptions.isChargingMaintenance());
        assertTrue(campaignOptions.isChargingOverhead());
        assertTrue(campaignOptions.isChargingPeacetimeCost());
    }

    /** Repairs are not replaced by upkeep. */
    @Test
    void upkeepDoesNotAffectRepairs() {
        CampaignOptions campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_REPAIRS, true);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, true);

        assertTrue(campaignOptions.get(CampaignOption.PAY_FOR_REPAIRS));
    }

    /** Every new Hot Spots option is opt-in, so existing campaigns play exactly as before. */
    @Test
    void everyNewHotSpotsOptionDefaultsToOff() {
        CampaignOptions campaignOptions = new CampaignOptions();
        List<CampaignOption<Boolean>> newOptions = List.of(
              CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP,
              CampaignOption.ESCALATING_HOT_SPOTS_UPKEEP,
              CampaignOption.USE_PLANETARY_COST_REDUCTIONS,
              CampaignOption.PLANETARY_COST_REDUCTIONS_ON_CONTRACT,
              CampaignOption.PLANETARY_COST_REDUCTIONS_FOR_MAINTENANCE,
              CampaignOption.INCREASE_CLAN_REPAIR_COSTS,
              CampaignOption.USE_ALTERNATE_UNIT_COST,
              CampaignOption.USE_SPA_TRAINING_COSTS,
              CampaignOption.CAP_TOTAL_SPAS,
              CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS,
              CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL,
              CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE,
              CampaignOption.BASE_PAY_ONLY_CONSIDERS_SCALE);

        for (CampaignOption<Boolean> option : newOptions) {
            assertFalse(campaignOptions.get(option), option + " should default to off");
        }
    }

    /** A fresh campaign with default options must charge exactly what it charged before these changes. */
    @Test
    void defaultsLeaveExistingChargesUnchanged() {
        CampaignOptions campaignOptions = new CampaignOptions();

        assertEquals(campaignOptions.get(CampaignOption.PAY_FOR_MAINTAIN), campaignOptions.isChargingMaintenance());
        assertEquals(campaignOptions.get(CampaignOption.PAY_FOR_OVERHEAD), campaignOptions.isChargingOverhead());
        assertEquals(campaignOptions.get(CampaignOption.USE_PEACETIME_COST),
              campaignOptions.isChargingPeacetimeCost());
    }
}
