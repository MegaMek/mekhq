package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.clamp;
import static megamek.common.compute.Compute.d6;

public class ContractTypeGeneration {
    private ChaosContractType playerContractType;
    private ChaosContractType opposingContractType;

    public ContractTypeGeneration(int contractGenerationModifier) {
        generateContractType(contractGenerationModifier);
    }

    public ChaosContractType getPlayerContractType() {
        return playerContractType;
    }

    public ChaosContractType getOpposingContractType() {
        return opposingContractType;
    }

    private void generateContractType(int contractGenerationModifier) {
        int roll = d6(2);
        int result = roll + contractGenerationModifier;
        int clampedResult = clamp(result, 1, 13);

        switch (clampedResult) {
            case 1, 4, 3, 2 -> generateExpeditionContractTypes(roll);
            case 5, 6 -> generateGarrisonContractTypes(roll);
            case 7, 8, 9 -> generateRaidContractTypes(roll);
            case 10, 11, 12, 13 -> generateInvasionContractTypes(roll);
        }
    }

    private void generateInvasionContractTypes(int roll) {
        playerContractType = ChaosContractType.INVASION;

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4 -> ChaosContractType.EXPEDITION;
            case 5, 6, 8, 7 -> ChaosContractType.GARRISON;
            case 9 -> ChaosContractType.RAID;
            case 10, 11, 12 -> ChaosContractType.INVASION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }

    private void generateRaidContractTypes(int roll) {
        playerContractType = ChaosContractType.RAID;

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4, 5, 6, 7 -> ChaosContractType.EXPEDITION;
            case 8, 9, 10 -> ChaosContractType.GARRISON;
            case 11 -> ChaosContractType.RAID;
            case 12 -> ChaosContractType.INVASION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }

    private void generateGarrisonContractTypes(int roll) {
        int playerFollowUpRoll = d6(2);
        playerContractType = switch (playerFollowUpRoll) {
            case 2, 3, 4, 5, 6 -> ChaosContractType.CADRE_DUTY;
            case 7, 8, 9, 10, 11, 12 -> ChaosContractType.GARRISON;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4 -> ChaosContractType.EXPEDITION;
            case 5, 6, 7, 8 -> ChaosContractType.RAID;
            case 9, 10, 11, 12 -> ChaosContractType.INVASION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }

    private void generateExpeditionContractTypes(int roll) {
        int playerFollowUpRoll = d6(2);
        playerContractType = switch (playerFollowUpRoll) {
            case 2, 3, 4, 5, 6, 7 -> ChaosContractType.STANDARD_EXPEDITION;
            case 8, 9, 10, 11 -> ChaosContractType.PIRATE_HUNT;
            case 12 -> ChaosContractType.GUERILLA_OPERATION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4, 5, 6, 7 -> ChaosContractType.GARRISON;
            case 8, 9, 10, 11, 12 -> ChaosContractType.RAID;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }
}
