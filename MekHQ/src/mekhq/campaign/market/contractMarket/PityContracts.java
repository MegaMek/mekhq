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

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBContractType;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.enums.SkillLevel.GREEN;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.mission.Contract.OH_NONE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;

/**
 * Utility class for generating pity contracts when a campaign does not have enough successful completed contracts.
 *
 * <p>Pity contracts are intended to ensure that a campaign has access to a minimum number of easy contract
 * opportunities by creating additional contracts when the campaign has fewer than {@link #PITY_CONTRACT_COUNT}
 * successful completed contracts.</p>
 *
 * <p>This system is intended to smooth out early game progression.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public class PityContracts {
    /**
     * The target number of successful contracts used when determining how many pity contracts should be generated.
     *
     * <p>The number of successful contracts the campaign has completed is deducted from this value.</p>
     */
    static final int PITY_CONTRACT_COUNT = 4;

    /**
     * Generates pity contracts for the supplied campaign.
     *
     * <p>The number of generated contracts is based on the number of successful completed contracts already present
     * in the campaign. If the campaign already has at least {@link #PITY_CONTRACT_COUNT} successful completed
     * contracts, no pity contracts are generated.</p>
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
        int contractCount = max(0, getPityContractCount(successfulContractCount));

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
     * Calculates the number of pity contracts needed from the supplied successful contract count.
     *
     * @param successfulContractCount the number of successful completed contracts
     *
     * @return the raw number of pity contracts needed before lower-bound clamping
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static int getPityContractCount(int successfulContractCount) {
        return PITY_CONTRACT_COUNT - successfulContractCount;
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

        if (!campaign.isPirateCampaign()) { // Pirate campaigns have fixed contractual terms
            overrideContractTermsForPityContracts(contract);
        }

        contract.setName(AtbMonthlyContractMarket.generateDefaultName(contract.getEmployer(), contract));
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
        contract.setSalvagePct(salvageRoll);

        int supportRoll = d6(1) * 10;
        contract.setStraightSupport(supportRoll);
        contract.setOverheadComp(OH_NONE);

        int battleLossRoll = d6(1) * 10;
        contract.setBattleLossComp(battleLossRoll);

        int transportRoll = (4 + d6(1)) * 10;
        contract.setTransportComp(transportRoll);
    }
}
