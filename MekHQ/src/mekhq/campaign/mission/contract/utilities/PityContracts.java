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
package mekhq.campaign.mission.contract.utilities;

import static java.lang.Math.max;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ContractMarket;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractMarketAvailability;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.universe.Faction;

/**
 * Utility class for generating pity contracts when a campaign does not have enough successful completed contracts.
 *
 * <p>Pity contracts guarantee a struggling force a minimum number of easy opportunities: whenever the campaign has
 * fewer successful completed contracts than the configured pity count, the shortfall is topped up with easy offers (a
 * veteran ally against a green enemy). These offers are flagged as {@link AbstractContract#isProvingGround() Proving
 * Grounds} and surfaced in the market, smoothing out early-game progression.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public class PityContracts {
    private PityContracts() {}

    /**
     * Tops up the contract market with pity ("Proving Ground") offers for the supplied campaign.
     *
     * <p>The shortfall is the configured pity count less the number of successful completed contracts already earned;
     * that many easy offers are generated and added to the market. When the force already has at least the pity count in
     * successful contracts, nothing is added.</p>
     *
     * @param campaign the campaign for which pity contracts are generated
     *
     * @return the shortfall that was attempted - how many offers generation was asked to place - regardless of how many
     *       actually generated (a best-effort generation may fail to place an offer, which is skipped rather than
     *       filed as a null)
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static int generatePityContracts(Campaign campaign) {
        int successfulContractCount = getSuccessfulContractCount(campaign);
        int targetPityContractCount = campaign.getCampaignOptions().get(CampaignOption.PITY_CONTRACTS);

        int contractCount = max(0, targetPityContractCount - successfulContractCount);

        ContractMarket contractMarket = campaign.getPlayerForce().getContractMarket();
        ContractSearchType bucket = pityBucket(campaign);
        for (int i = 0; i < contractCount; i++) {
            AbstractContract contract = ChaosContractMarketAvailability.generateProvingGroundOffer(campaign, bucket);
            // Generation is best-effort and may fail to place an offer; a null result is skipped rather than filed.
            if (contract != null) {
                contractMarket.addContract(bucket, contract);
            }
        }

        return contractCount;
    }

    /**
     * Counts the completed contracts in the supplied campaign that ended in success.
     *
     * @param campaign the campaign whose completed contracts are inspected
     *
     * @return the number of completed contracts with a successful status
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int getSuccessfulContractCount(Campaign campaign) {
        int successfulContractCount = 0;
        for (AbstractContract contract : campaign.getCompletedContracts()) {
            if (contract.getStatus().isSuccess()) {
                successfulContractCount++;
            }
        }
        return successfulContractCount;
    }

    /**
     * The market bucket pity contracts are placed in, matching where the campaign's faction actually looks for work:
     * pirate bands browse acts of piracy, mercenaries browse mercenary work, everyone else browses government orders.
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static ContractSearchType pityBucket(Campaign campaign) {
        Faction faction = campaign.getPlayerForce().getFaction();
        if (faction.isPirate()) {
            return ContractSearchType.PIRATE;
        }
        if (faction.isMercenary()) {
            return ContractSearchType.MERCENARY;
        }
        return ContractSearchType.GOVERNMENT;
    }
}
