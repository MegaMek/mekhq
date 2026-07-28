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
package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.randomInt;

import java.util.ArrayList;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.enums.AtBContractType;

public enum ChaosObjectiveType {
    EXPEDITION(3, 0, 1, 0, 0, 2),
    PIRATE_HUNT(3, 0, 0, 0, -1, 2),
    GUERILLA_OPERATION(3, 0, 0, 0, -1, 2),
    GARRISON(6, 1, 0, 1, -2, 0),
    CADRE_DUTY(6, 1, 0, 1, -2, 0),
    RAID(3, 0, 0, 0, -1, 0),
    INVASION(6, -1, 2, -1, 1, -2),
    PIRATE_RAID(3, 0, 0, 0, -1, 0);

    private static final MMLogger LOGGER = MMLogger.create(ChaosObjectiveType.class);

    // Hot Spots Draconis Reach pg 144 first printing
    private final int monthsLength;
    // Hot Spots Draconis Reach pg 144 first printing
    private final int payRateModifier;
    // Hot Spots Draconis Reach pg 144 first printing
    private final int supportModifier;
    // Hot Spots Draconis Reach pg 144 first printing
    private final int transportModifier;
    // Hot Spots Draconis Reach pg 144 first printing
    private final int salvageRightsModifier;
    private final int commandRightsModifier;

    ChaosObjectiveType(final int monthsLength, final int payRateModifier, final int supportModifier,
          final int transportModifier, final int salvageRightsModifier, final int commandRightsModifier) {
        this.monthsLength = monthsLength;
        this.payRateModifier = payRateModifier;
        this.supportModifier = supportModifier;
        this.transportModifier = transportModifier;
        this.salvageRightsModifier = salvageRightsModifier;
        this.commandRightsModifier = commandRightsModifier;
    }

    public int getMonthsLength() {
        return monthsLength;
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

    public AtBContractType getCamOpsObjectiveType() {
        List<AtBContractType> candidates = new ArrayList<>();
        for (AtBContractType type : AtBContractType.values()) {
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
}
