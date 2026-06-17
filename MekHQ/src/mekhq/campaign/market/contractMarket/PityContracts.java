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
package mekhq.campaign.market.contractMarket;

import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.mission.Contract.OH_NONE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;

/**
 * Utility class for generating pity contracts when a campaign does not have enough successful completed contracts.
 *
 * <p>Pity contracts are intended to ensure that a campaign has access to a minimum number of easy contract
 * opportunities by creating additional contracts when the campaign has fewer than the requested number of pity
 * contracts.</p>
 *
 * <p>This system is intended to smooth out early game progression.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public class PityContracts {
    /**
     * Generates pity contracts for the supplied campaign.
     *
     * <p>The number of generated contracts is based on the number of successful completed contracts already present
     * in the campaign. If the campaign already has at least a number of successful completed contracts in excess of the
     * pity contract count, no pity contracts are generated.</p>
     *
     * @param campaign the campaign for which pity contracts are generated
     *
     * @return the number of pity contracts requested for generation
     *
     * @author Illiani
     * @since 0.51.0
     */
    public static int generatePityContracts(Campaign campaign) {
        AbstractContractMarket contractMarket = campaign.getContractMarket();

        int successfulContractCount = getSuccessfulContractCount(campaign);
        int targetPityContractCount = campaign.getCampaignOptions().getPityContracts();

        int contractCount = targetPityContractCount - successfulContractCount;
        contractCount = max(0, contractCount);

        for (int i = 0; i < contractCount; i++) {
            PityContracts.createPityContract(campaign, contractMarket);
        }

        return contractCount;
    }

    /**
     * Counts the number of completed contracts in the supplied campaign that have a successful mission status.
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
        for (AtBContract contract : campaign.getCompletedAtBContracts()) {
            if (contract.getStatus().isSuccess()) {
                successfulContractCount++;
            }
        }
        return successfulContractCount;
    }

    /**
     * Creates and initializes a single pity contract using the supplied contract market.
     *
     * @param campaign       the campaign receiving the pity contract
     * @param contractMarket the contract market used to create the contract
     *
     * @author Illiani
     * @since 0.51.0
     */
    static void createPityContract(Campaign campaign, AbstractContractMarket contractMarket) {
        AtBContract contract = contractMarket.addAtBContract(campaign);
        if (contract == null) {
            return;
        }

        contract.setAllySkill(VETERAN);
        contract.setEnemySkill(GREEN);

        updateEnemyFaction(campaign, contract);

        // We need to rebuild the difficulty estimate as otherwise it will still be reporting for the contract's
        // original enemy
        boolean isUseGenericBattleValue = campaign.getCampaignOptions().isUseGenericBattleValue();
        List<Entity> combatUnits = campaign.getAllCombatEntities();
        int difficulty = contract.calculateContractDifficulty(campaign.getGameYear(),
              isUseGenericBattleValue,
              combatUnits);
        contract.setDifficulty(difficulty);

        if (!campaign.isPirateCampaign()) { // Pirate campaigns have fixed contractual terms
            overrideContractTermsForPityContracts(contract);
        }

        contract.setName(AtbMonthlyContractMarket.generateDefaultName(contract.getEmployerName(), contract));
    }

    /**
     * Sets the enemy faction for a pity contract and refreshes the contract enemy data.
     *
     * <p>Clan campaigns use the bandit caste faction code, while non-Clan campaigns use the pirate faction code.</p>
     *
     * @param campaign the campaign used to determine the appropriate enemy faction
     * @param contract the contract whose enemy faction is updated
     *
     * @author Illiani
     * @since 0.51.0
     */
    static void updateEnemyFaction(Campaign campaign, AtBContract contract) {
        String banditCasteFactionCode = "BAN";
        String enemyCode = campaign.isClanCampaign() ? banditCasteFactionCode : PIRATE_FACTION_CODE;

        contract.setEnemyCode(enemyCode);
        contract.updateEnemy(campaign, campaign.getLocalDate(), enemyCode);
    }

    /**
     * Overrides generated contract terms with pity contract-specific values.
     *
     * <p>The resulting contract is configured as a pirate hunting contract with randomly generated salvage, support,
     * battle loss compensation, and transport compensation values.</p>
     *
     * @param contract the contract whose terms are overridden
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static void overrideContractTermsForPityContracts(AtBContract contract) {
        contract.setContractType(AtBContractType.PIRATE_HUNTING);

        int salvageRoll = d6(1) * 10;
        contract.setSalvagePercent(salvageRoll);

        int supportRoll = d6(1) * 10;
        contract.setStraightSupport(supportRoll);
        contract.setOverheadCompensation(OH_NONE);

        int battleLossRoll = d6(1) * 10;
        contract.setBattleLossCompensation(battleLossRoll);

        int transportRoll = (4 + d6(1)) * 10;
        contract.setTransportCompensation(transportRoll);
    }
}
