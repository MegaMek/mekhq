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

import static java.lang.Math.clamp;
import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.contract.contractData.ContractObjectiveData;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.jspecify.annotations.NonNull;

public class ChaosContractObjectiveDetermination {
    public static ContractObjectiveData determineContractObjectiveType(int contractGenerationModifier) {
        int roll = d6(2);
        int result = clamp(roll + contractGenerationModifier, 1, 13);

        // Hot Spots Draconis Reach, pg 143, first printing
        return switch (result) {
            case 1, 2, 3, 4 -> {
                ContractObjectiveType playerObjective = getExpeditionPlayerObjective();
                ContractObjectiveType opposingObjective = getExpeditionOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 5, 6 -> {
                ContractObjectiveType playerObjective = getGarrisonPlayerObjective();
                ContractObjectiveType opposingObjective = getGarrisonOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 7, 8, 9 -> {
                ContractObjectiveType playerObjective = getRaidPlayerObjective();
                ContractObjectiveType opposingObjective = getRaidOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 10, 11, 12, 13 -> {
                ContractObjectiveType playerObjective = getInvasionPlayerObjective();
                ContractObjectiveType opposingObjective = getInvasionOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static @NonNull ContractObjectiveType getInvasionOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4 -> ChaosObjectiveType.EXPEDITION.getCamOpsObjectiveType();
            case 5, 6, 7, 8 -> ChaosObjectiveType.GARRISON.getCamOpsObjectiveType();
            case 9 -> ChaosObjectiveType.RAID.getCamOpsObjectiveType();
            case 10, 11, 12 -> ChaosObjectiveType.INVASION.getCamOpsObjectiveType();
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ContractObjectiveType getInvasionPlayerObjective() {
        return ChaosObjectiveType.INVASION.getCamOpsObjectiveType();
    }

    private static @NonNull ContractObjectiveType getRaidPlayerObjective() {
        return ChaosObjectiveType.RAID.getCamOpsObjectiveType();
    }

    private static ContractObjectiveType getRaidOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4, 5, 6, 7 -> ChaosObjectiveType.EXPEDITION.getCamOpsObjectiveType();
            case 8, 9, 10 -> ChaosObjectiveType.GARRISON.getCamOpsObjectiveType();
            case 11 -> ChaosObjectiveType.RAID.getCamOpsObjectiveType();
            case 12 -> ChaosObjectiveType.INVASION.getCamOpsObjectiveType();
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static @NonNull ContractObjectiveType getGarrisonOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4 -> ChaosObjectiveType.EXPEDITION.getCamOpsObjectiveType();
            case 5, 6, 7, 8 -> ChaosObjectiveType.RAID.getCamOpsObjectiveType();
            case 9, 10, 11, 12 -> ChaosObjectiveType.INVASION.getCamOpsObjectiveType();
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ContractObjectiveType getGarrisonPlayerObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4, 5 -> ChaosObjectiveType.CADRE_DUTY.getCamOpsObjectiveType();
            case 6, 7, 8, 9, 10, 11, 12 -> ChaosObjectiveType.GARRISON.getCamOpsObjectiveType();
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ContractObjectiveType getExpeditionOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 8, 7, 3, 4, 5, 6 -> ChaosObjectiveType.GARRISON.getCamOpsObjectiveType();
            case 9, 10, 11, 12 -> ChaosObjectiveType.RAID.getCamOpsObjectiveType();
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ContractObjectiveType getExpeditionPlayerObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 1, 8, 7, 6, 5, 4, 3, 2 -> ChaosObjectiveType.EXPEDITION.getCamOpsObjectiveType();
            case 9, 10, 11 -> ChaosObjectiveType.PIRATE_HUNT.getCamOpsObjectiveType();
            case 12 -> ChaosObjectiveType.GUERILLA_OPERATION.getCamOpsObjectiveType();
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }
}
