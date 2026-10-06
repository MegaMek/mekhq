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

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.PlanetarySystem;

/**
 * Hot Spots: Draconis Reach off-contract planetary repair and refit cost reductions (Draconis Reach first printing pg
 * 69). While on a planet, repair and refit costs are scaled by the system's Hiring Hall Rating: A-B ×0.5, C ×0.75,
 * D-F ×1.0.
 *
 * <p>By default the reduction only applies while the force has no active contract. Campaign options can extend it to
 * cover time on contract (repairs and refits only) and maintenance while off contract. Maintenance is never reduced
 * while on contract.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class PlanetaryCostReductions {
    private static final double NO_REDUCTION = 1.0;

    private PlanetaryCostReductions() {
    }

    /**
     * @param campaign the campaign
     *
     * @return the multiplier to apply to repair and refit costs
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getRepairAndRefitMultiplier(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.get(CampaignOption.USE_PLANETARY_COST_REDUCTIONS)) {
            return NO_REDUCTION;
        }

        if (isOnContract(campaign)
                  && !campaignOptions.get(CampaignOption.PLANETARY_COST_REDUCTIONS_ON_CONTRACT)) {
            return NO_REDUCTION;
        }

        return getPlanetaryMultiplier(campaign);
    }

    /**
     * @param campaign the campaign
     *
     * @return the multiplier to apply to maintenance costs
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static double getMaintenanceMultiplier(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.get(CampaignOption.USE_PLANETARY_COST_REDUCTIONS)
                  || !campaignOptions.get(CampaignOption.PLANETARY_COST_REDUCTIONS_FOR_MAINTENANCE)) {
            return NO_REDUCTION;
        }

        // Maintenance is never reduced while on contract, even when reductions otherwise apply on contract
        if (isOnContract(campaign)) {
            return NO_REDUCTION;
        }

        return getPlanetaryMultiplier(campaign);
    }

    /**
     * Checks the contracts directly rather than {@link Campaign#hasActiveContract()}, which is a cached flag that is
     * only refreshed daily while StratCon is enabled.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isOnContract(Campaign campaign) {
        return !campaign.getActiveContracts().isEmpty();
    }

    /**
     * @return the current system's Hiring Hall cost multiplier, or no reduction if the force isn't on a planet
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static double getPlanetaryMultiplier(Campaign campaign) {
        AbstractLocation location = campaign.getPlayerForce().getForceDetachment().getCurrentLocation();
        if (location == null || !location.isOnPlanet()) {
            return NO_REDUCTION;
        }

        PlanetarySystem currentSystem = location.getCurrentSystem();
        if (currentSystem == null) {
            return NO_REDUCTION;
        }

        return currentSystem.getHiringHallLevel(campaign.getLocalDate()).getPlanetaryCostMultiplier();
    }
}
