/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * (GPL), version 3 or (at your option) any later version, as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project; if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers creating free software for the BattleTech
 * community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or affiliated with Microsoft.
 */
package mekhq.campaign.finances;

import jakarta.annotation.Nullable;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.unit.Unit;

/**
 * Multipliers applied to the cost of repairing a part.
 *
 * <ul>
 *     <li>Off-contract planetary cost reductions, see {@link PlanetaryCostReductions}.</li>
 *     <li>Clan and mixed-tech units cost ×{@link #CLAN_REPAIR_COST_MULTIPLIER} to repair when
 *     {@link CampaignOption#INCREASE_CLAN_REPAIR_COSTS} is set (Hot Spots: Draconis Reach first printing pg 31).</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class RepairCosts {
    static final double CLAN_REPAIR_COST_MULTIPLIER = 1.5;

    private RepairCosts() {
    }

    /**
     * @param campaign the campaign
     * @param unit     the unit the part is being repaired on, or {@code null} for a spare part
     *
     * @return the multiplier to apply to the part's repair cost
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getRepairCostMultiplier(Campaign campaign, @Nullable Unit unit) {
        double multiplier = PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign);
        if (campaign.getCampaignOptions().get(CampaignOption.INCREASE_CLAN_REPAIR_COSTS) && isClanOrMixedTech(unit)) {
            multiplier *= CLAN_REPAIR_COST_MULTIPLIER;
        }
        return multiplier;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isClanOrMixedTech(@Nullable Unit unit) {
        if (unit == null) {
            return false;
        }

        Entity entity = unit.getEntity();
        return entity != null && (entity.isClan() || entity.isMixedTech());
    }
}
