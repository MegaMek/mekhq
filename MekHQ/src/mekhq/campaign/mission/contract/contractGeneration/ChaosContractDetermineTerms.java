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

import static megamek.common.compute.Compute.d6;

import jakarta.annotation.Nullable;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractTermsData;
import mekhq.campaign.universe.Faction;

public class ChaosContractDetermineTerms {
    public static ContractTermsData determineInitialTerms(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType, @Nullable Faction employerFaction, boolean useFactionModifiers) {
        int factionPayDelta = useFactionModifiers ? getFactionPayRateModifier(employerFaction) : 0;
        int factionCommandDelta = useFactionModifiers ? getFactionCommandRightsModifier(employerFaction) : 0;

        ChaosContractStepsTable payRate = determinePayRate(objectiveType, employerType, factionPayDelta);
        ChaosContractStepsTable support = determineSupport(objectiveType, employerType);
        ChaosContractStepsTable transport = determineTransport(objectiveType, employerType);
        ChaosContractStepsTable salvageRights = determineSalvageRights(objectiveType, employerType);
        ChaosContractStepsTable commandRights = determineCommandRights(objectiveType,
              employerType,
              factionCommandDelta);

        return new ContractTermsData(payRate, support, transport, salvageRights, commandRights);
    }

    /**
     * The base-pay step modifier a faction's disposition contributes: a generous employer pays more (+1), a stingy one
     * pays less (-1). Both tags cancel; neither yields 0.
     */
    static int getFactionPayRateModifier(@Nullable Faction employerFaction) {
        if (employerFaction == null) {
            return 0;
        }
        int delta = 0;
        if (employerFaction.isGenerous()) {
            delta += 1;
        }
        if (employerFaction.isStingy()) {
            delta -= 1;
        }
        return delta;
    }

    /**
     * The command-rights step modifier a faction's disposition contributes: a lenient employer grants more independence
     * (+1, toward Independent command), a controlling one keeps a tighter grip (-1, toward Integrated). Both tags
     * cancel; neither yields 0.
     */
    static int getFactionCommandRightsModifier(@Nullable Faction employerFaction) {
        if (employerFaction == null) {
            return 0;
        }
        int delta = 0;
        if (employerFaction.isLenient()) {
            delta += 1;
        }
        if (employerFaction.isControlling()) {
            delta -= 1;
        }
        return delta;
    }

    private static ChaosContractStepsTable determinePayRate(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType, int factionDelta) {
        int roll = d6(2);
        ChaosContractStepsTable step = switch (roll) {
            case 2, 3 -> ChaosContractStepsTable.STEP_THREE;
            case 4, 5 -> ChaosContractStepsTable.STEP_FOUR;
            case 6, 7 -> ChaosContractStepsTable.STEP_FIVE;
            case 8, 9 -> ChaosContractStepsTable.STEP_SIX;
            case 10, 11 -> ChaosContractStepsTable.STEP_SEVEN;
            case 12 -> ChaosContractStepsTable.STEP_EIGHT;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };

        int contractDelta = objectiveType.getPayRateModifier();
        int employerDelta = employerType.getPayRateModifier();
        int totalDelta = contractDelta + employerDelta + factionDelta;

        return step.influenceStep(totalDelta);
    }

    private static ChaosContractStepsTable determineSupport(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType) {
        int roll = d6(2);
        ChaosContractStepsTable step = switch (roll) {
            case 2, 3, 5, 4 -> ChaosContractStepsTable.STEP_THREE;
            case 6, 7 -> ChaosContractStepsTable.STEP_FOUR;
            case 8, 9 -> ChaosContractStepsTable.STEP_FIVE;
            case 10, 11 -> ChaosContractStepsTable.STEP_SIX;
            case 12 -> ChaosContractStepsTable.STEP_SEVEN;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };

        int contractDelta = objectiveType.getSupportModifier();
        int employerDelta = employerType.getSupportModifier();
        int totalDelta = contractDelta + employerDelta;

        return step.influenceStep(totalDelta);
    }

    private static ChaosContractStepsTable determineTransport(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType) {
        int roll = d6(2);
        ChaosContractStepsTable step = switch (roll) {
            case 2, 3, 4, 5 -> ChaosContractStepsTable.STEP_FIVE;
            case 6, 7 -> ChaosContractStepsTable.STEP_SIX;
            case 8, 9 -> ChaosContractStepsTable.STEP_SEVEN;
            case 10, 11 -> ChaosContractStepsTable.STEP_EIGHT;
            case 12 -> ChaosContractStepsTable.STEP_NINE;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };

        int contractDelta = objectiveType.getTransportModifier();
        int employerDelta = employerType.getTransportModifier();
        int totalDelta = contractDelta + employerDelta;

        return step.influenceStep(totalDelta);
    }

    private static ChaosContractStepsTable determineSalvageRights(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType) {
        int roll = d6(2);
        ChaosContractStepsTable step = switch (roll) {
            case 2, 3, 4, 5 -> ChaosContractStepsTable.STEP_THREE;
            case 6, 7 -> ChaosContractStepsTable.STEP_FOUR;
            case 8, 9 -> ChaosContractStepsTable.STEP_FIVE;
            case 10, 11 -> ChaosContractStepsTable.STEP_SIX;
            case 12 -> ChaosContractStepsTable.STEP_SEVEN;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };

        int contractDelta = objectiveType.getSalvageRightsModifier();
        int employerDelta = employerType.getSalvageRightsModifier();
        int totalDelta = contractDelta + employerDelta;

        return step.influenceStep(totalDelta);
    }

    private static ChaosContractStepsTable determineCommandRights(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType, int factionDelta) {
        int roll = d6(2);
        ChaosContractStepsTable step = switch (roll) {
            case 2, 3, 4, 5 -> ChaosContractStepsTable.STEP_THREE;
            case 6, 7 -> ChaosContractStepsTable.STEP_SEVEN;
            case 8, 9 -> ChaosContractStepsTable.STEP_EIGHT;
            case 10, 11, 12 -> ChaosContractStepsTable.STEP_ELEVEN;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };

        int contractDelta = objectiveType.getCommandRightsModifier();
        int employerDelta = employerType.getCommandRightsModifier();
        int totalDelta = contractDelta + employerDelta + factionDelta;

        return step.influenceStep(totalDelta);
    }
}
