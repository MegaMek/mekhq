package mekhq.campaign.mission.newContract.contractGeneration;

import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.newContract.contractData.ChaosContractStepsTable;

public class ChaosContractDetermineTerms {
    public static ContractTermsData determineInitialTerms(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType) {
        ChaosContractStepsTable payRate = determinePayRate(objectiveType, employerType);
        ChaosContractStepsTable support = determineSupport(objectiveType, employerType);
        ChaosContractStepsTable transport = determineTransport(objectiveType, employerType);
        ChaosContractStepsTable salvageRights = determineSalvageRights(objectiveType, employerType);
        ChaosContractStepsTable commandRights = determineCommandRights(objectiveType, employerType);

        return new ContractTermsData(payRate, support, transport, salvageRights, commandRights);
    }

    private static ChaosContractStepsTable determinePayRate(ChaosObjectiveType objectiveType,
          ChaosEmployerType employerType) {
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
        int totalDelta = contractDelta + employerDelta;

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
          ChaosEmployerType employerType) {
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
        int totalDelta = contractDelta + employerDelta;

        return step.influenceStep(totalDelta);
    }
}
