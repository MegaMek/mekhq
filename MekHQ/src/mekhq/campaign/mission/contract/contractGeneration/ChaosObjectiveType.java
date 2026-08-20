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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveSpecialRules.DOUBLE_ALL_COSTS;
import static mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveSpecialRules.DOUBLE_SUPPORT_PAYOUTS;
import static mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveSpecialRules.END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS;
import static mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveSpecialRules.NO_IN_CONTRACT_SUPPORT;
import static mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveSpecialRules.SIMULATED_DAMAGE;
import static mekhq.campaign.mission.contract.contractGeneration.ChaosObjectiveSpecialRules.USE_PIRATE_LOOTING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;

public enum ChaosObjectiveType {
    EXPEDITION(3, 1, 0, 1, 0, 0, 2, -1, true,
          Collections.emptyList()),
    PIRATE_HUNT(3, 1, 0, 0, 0, -1, 2, -1, true,
          List.of(END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS)),
    GUERILLA_OPERATION(3, 3, 0, 0, 0, -1, 2, -1, true,
          List.of(DOUBLE_ALL_COSTS, NO_IN_CONTRACT_SUPPORT, DOUBLE_SUPPORT_PAYOUTS)),
    GARRISON(6, -1, 1, 0, 1, -2, 0, 1, false,
          Collections.emptyList()),
    CADRE_DUTY(6, 0, 1, 0, 1, -2, 0, -2, false,
          List.of(SIMULATED_DAMAGE)),
    RAID(3, 2, 0, 0, 0, -1, 0, 0, true,
          Collections.emptyList()),
    INVASION(6, 2, -1, 2, -1, 1, -2, 3, true,
          Collections.emptyList()),
    PIRATE_RAID(3, 1, 0, 0, 0, -1, 0, -2, true,
          List.of(END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS, USE_PIRATE_LOOTING));

    private static final MMLogger LOGGER = MMLogger.create(ChaosObjectiveType.class);

    private final int monthsLength; // Hot Spots Draconis Reach pg 144 first printing
    private final int procurementTargetNumberModifier;
    private final int payRateModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int supportModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int transportModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int salvageRightsModifier; // Hot Spots Draconis Reach pg 144 first printing
    private final int commandRightsModifier; // Hot Spots Draconis Reach pg 144 first printing
    // MekHQ force-commitment heuristic (not from Hot Spots): how much force quality an employer or enemy commits to an
    // objective of this strategic scope. Positive fields better-skilled, better-equipped forces; negative fields less.
    private final int forceCommitmentModifier;
    private final boolean isAttacker;
    private final List<ChaosObjectiveSpecialRules> specialRules;  // Hot Spots Draconis Reach pg 146 first printing

    ChaosObjectiveType(final int monthsLength, final int procurementTargetNumberModifier, final int payRateModifier,
          final int supportModifier, final int transportModifier, final int salvageRightsModifier,
          final int commandRightsModifier, final int forceCommitmentModifier, final boolean isAttacker,
          final List<ChaosObjectiveSpecialRules> specialRules) {
        this.monthsLength = monthsLength;
        this.procurementTargetNumberModifier = procurementTargetNumberModifier;
        this.payRateModifier = payRateModifier;
        this.supportModifier = supportModifier;
        this.transportModifier = transportModifier;
        this.salvageRightsModifier = salvageRightsModifier;
        this.commandRightsModifier = commandRightsModifier;
        this.forceCommitmentModifier = forceCommitmentModifier;
        this.isAttacker = isAttacker;
        this.specialRules = specialRules;
    }

    public int getMonthsLength() {
        return monthsLength;
    }


    /**
     * The returned value is a modifier added to the target number of procurement checks, so a higher value makes parts
     * harder to acquire, and a lower (negative) value makes them easier. In other words, higher is worse. The modifier
     * is only applied when StratCon and "restrict parts by mission" are both enabled.
     *
     * @return the procurement modifier for the current contract type, where higher values mean worse availability
     */
    public int getProcurementTargetNumberModifier() {
        return procurementTargetNumberModifier;
    }

    /**
     * Calculates the length of the contract in months.
     *
     * <p>If variable contract lengths are enabled, the length is calculated with randomization around the base
     * contract type's standard duration. Otherwise, the constant length defined by the contract type is used.</p>
     *
     * @param useVariableContractLengths whether to use variable length calculation
     *
     * @return the calculated contract length in months
     */
    public int calculateLength(final boolean useVariableContractLengths) {
        return useVariableContractLengths ? calculateVariableLength() : getMonthsLength();
    }

    /**
     * Calculates a variable contract length with randomization.
     *
     * <p>The length is calculated as 75% of the constant length plus a random variance of up to 50% of the constant
     * length. For example, a contract type with a constant length of 12 months would have a base of 9 months plus 0-6
     * months variance, resulting in a range of 9-15 months.</p>
     *
     * @return the calculated variable contract length in months
     */
    private int calculateVariableLength() {
        int baseLength = (int) round(monthsLength * 0.75);
        int variance = (int) round(monthsLength * 0.5);

        if (variance > 0) {
            return max(1, baseLength + randomInt(variance));
        } else {
            // If we can't determine variance return the constantLength
            return monthsLength;
        }
    }

    public ContractObjectiveType getCamOpsObjectiveType() {
        List<ContractObjectiveType> candidates = new ArrayList<>();
        for (ContractObjectiveType type : ContractObjectiveType.values()) {
            if (type.getChaosObjectiveType() == this) {
                candidates.add(type);
            }
        }

        if (candidates.isEmpty()) {
            LOGGER.warn("No candidate of type {} was found.", this);
        }

        return ObjectUtility.getRandomItem(candidates);
    }

    public int getPayRateModifier() {
        return payRateModifier;
    }

    public int getSupportModifier() {
        return supportModifier;
    }

    public int getTransportModifier() {
        return transportModifier;
    }

    public int getSalvageRightsModifier() {
        return salvageRightsModifier;
    }

    public int getCommandRightsModifier() {
        return commandRightsModifier;
    }

    public int getForceCommitmentModifier() {
        return forceCommitmentModifier;
    }

    public boolean isAttacker() {
        return isAttacker;
    }

    public List<ChaosObjectiveSpecialRules> getSpecialRules() {
        return specialRules;
    }

    public boolean usesSpecialRule(ChaosObjectiveSpecialRules specialRule) {
        return specialRules.contains(specialRule);
    }
}
