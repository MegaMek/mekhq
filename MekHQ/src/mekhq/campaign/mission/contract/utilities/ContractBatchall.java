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

import static mekhq.campaign.enums.DailyReportType.POLITICS;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.EnemyData;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.PerformBatchall;

/**
 * Offers the player a Batchall from a contract's enemy: when most contracts begin, and when the enemy first raids a
 * garrison-type contract (see {@link MHQMorale#checkMorale}).
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractBatchall {
    private ContractBatchall() {
    }

    /**
     * Offers the player a Batchall from the contract's enemy, if <i>Generic Battle Value</i> is enabled and the enemy
     * is a Clan that follows the custom. Clans always declare Batchalls against pirates and bandits; against anyone
     * else, the Batchall Restrictions option may withhold one while the player's Standing with the Clan is too low.
     *
     * <p>The player's answer is recorded on the contract's enemy. A refusal costs Standing with the Clan, if Faction
     * Standing is tracked.</p>
     *
     * @param campaign the current campaign
     * @param contract the contract whose enemy issues the Batchall
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void offerBatchall(Campaign campaign, AbstractContract contract) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.get(CampaignOption.USE_GENERIC_BATTLE_VALUE)) {
            return;
        }

        Faction enemyFaction = contract.getEnemyFaction();
        if (!enemyFaction.performsBatchalls()) {
            return;
        }

        Faction campaignFaction = campaign.getPlayerForce().getFaction();
        String enemyFactionCode = contract.getEnemyFactionCode();

        // Clans always declare Batchalls against pirates and bandits, so the Faction Standing restriction only applies
        // to everyone else
        if (!campaignFaction.isPirateOrBanditCaste()
                  && campaignOptions.isUseFactionStandingBatchallRestrictionsSafe()) {
            double regard = campaign.getPlayerForce().getFactionStandings().getRegardForFaction(enemyFactionCode, true);
            if (!FactionStandingUtilities.isBatchallAllowed(regard)) {
                return;
            }
        }

        PerformBatchall batchallDialog = new PerformBatchall(campaign,
              contract.getEnemyData().opposingCommander(),
              enemyFactionCode);

        boolean batchallAccepted = batchallDialog.isBatchallAccepted();
        contract.setEnemyData(new EnemyData(contract.getEnemyData(), batchallAccepted));

        if (!batchallAccepted && campaignOptions.get(CampaignOption.TRACK_FACTION_STANDING)) {
            List<String> reports = campaign.getPlayerForce()
                                         .getFactionStandings()
                                         .processRefusedBatchall(campaignFaction.getShortName(),
                                               enemyFactionCode,
                                               campaign.getLocalDate().getYear(),
                                               campaignOptions.get(CampaignOption.REGARD_MULTIPLIER));

            for (String report : reports) {
                campaign.addReport(POLITICS, report);
            }
        }
    }
}
