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
package mekhq.campaign.mission.contract.contractData;

import static java.lang.Math.clamp;

import jakarta.annotation.Nullable;

public enum ChaosContractStepsTable {
    // Hot Spots Draconis Reach pg 145 first printing
    STEP_ONE(1, 0.5, ContractCommandRights.INTEGRATED, 0.0, false, 0.0, 0.0, 0.0),
    STEP_TWO(2, 0.55, ContractCommandRights.INTEGRATED, 0.25, true, 0.2, 0.0, 0.0),
    STEP_THREE(3, 0.6, ContractCommandRights.INTEGRATED, 0.25, true, 0.4, 0.0, 0.0),
    STEP_FOUR(4, 0.7, ContractCommandRights.HOUSE, 0.1, false, 0.6, 0.0, 0.0),
    STEP_FIVE(5, 0.8, ContractCommandRights.HOUSE, 0.2, false, 0.7, 0.0, 0.0),
    STEP_SIX(6, 0.9, ContractCommandRights.HOUSE, 0.3, false, 0.8, 0.0, .25),
    STEP_SEVEN(7, 1.0, ContractCommandRights.HOUSE, 0.4, false, 0.9, 0.0, 0.5),
    STEP_EIGHT(8, 1.1, ContractCommandRights.LIAISON, 0.5, false, 1.0, 0.0, 0.75),
    STEP_NINE(9, 1.2, ContractCommandRights.LIAISON, 0.6, false, 1.0, 0.1, 1.0),
    STEP_TEN(10, 1.3, ContractCommandRights.LIAISON, 0.7, false, 1.0, 0.2, 1.0),
    STEP_ELEVEN(11, 1.4, ContractCommandRights.INDEPENDENT, 0.8, false, 1.0, 0.3, 1.0),
    STEP_TWELVE(12, 1.5, ContractCommandRights.INDEPENDENT, 0.9, false, 1.0, 0.4, 1.0),
    STEP_THIRTEEN(13, 1.6, ContractCommandRights.INDEPENDENT, 1.0, false, 1.0, 0.5, 1.0),
    STEP_FOURTEEN(14, 1.7, ContractCommandRights.INDEPENDENT, 1.0, false, 1.0, 0.75, 1.0),
    STEP_FIFTEEN(15, 1.8, ContractCommandRights.INDEPENDENT, 1.0, false, 1.0, 1.0, 1.0),
    STEP_SIXTEEN(16, 1.9, ContractCommandRights.INDEPENDENT, 1.0, false, 1.0, 1.0, 1.0),
    STEP_SEVENTEEN(17, 2.0, ContractCommandRights.INDEPENDENT, 1.0, false, 1.0, 1.0, 1.0);

    public final static int CHAOS_CONTRACT_MINIMUM_STEP_VALUE = 1;
    public final static int CHAOS_CONTRACT_MAXIMUM_STEP_VALUE = 17;

    private final int stepValue;
    private final double basePayMultiplier;
    private final ContractCommandRights contractCommandRights;
    private final double salvageMultiplier;
    private final boolean isExchangeSalvage;
    private final double straightSupportMultiplier;
    private final double battlefieldLossMultiplier;
    private final double transportMultiplier;

    ChaosContractStepsTable(final int stepValue, final double basePayMultiplier,
          final ContractCommandRights contractCommandRights, final double salvageMultiplier,
          final boolean isExchangeSalvage, final double straightSupportMultiplier,
          final double battlefieldLossMultiplier, final double transportMultiplier) {
        this.stepValue = stepValue;
        this.basePayMultiplier = basePayMultiplier;
        this.contractCommandRights = contractCommandRights;
        this.salvageMultiplier = salvageMultiplier;
        this.isExchangeSalvage = isExchangeSalvage;
        this.straightSupportMultiplier = straightSupportMultiplier;
        this.battlefieldLossMultiplier = battlefieldLossMultiplier;
        this.transportMultiplier = transportMultiplier;
    }

    public int stepValue() {
        return stepValue;
    }

    public @Nullable ChaosContractStepsTable influenceStep(final int delta) {
        int newStepValue = this.stepValue + delta;
        newStepValue = clamp(newStepValue, CHAOS_CONTRACT_MINIMUM_STEP_VALUE, CHAOS_CONTRACT_MAXIMUM_STEP_VALUE);

        for (ChaosContractStepsTable step : ChaosContractStepsTable.values()) {
            if (step.stepValue == newStepValue) {
                return step;
            }
        }

        return null;
    }

    public static ChaosContractStepsTable fromStepValue(final int stepValue) {
        for (ChaosContractStepsTable step : ChaosContractStepsTable.values()) {
            if (step.stepValue == stepValue) {
                return step;
            }
        }

        throw new IllegalArgumentException("No step value " + stepValue);
    }

    public double getBasePayMultiplier() {
        return basePayMultiplier;
    }

    public ContractCommandRights getContractCommandRights() {
        return contractCommandRights;
    }

    public double getSalvageMultiplier() {
        return salvageMultiplier;
    }

    public boolean isExchangeSalvage() {
        return isExchangeSalvage;
    }

    public double getStraightSupportMultiplier() {
        return straightSupportMultiplier;
    }

    public double getBattlefieldLossMultiplier() {
        return battlefieldLossMultiplier;
    }

    public double getTransportMultiplier() {
        return transportMultiplier;
    }
}
