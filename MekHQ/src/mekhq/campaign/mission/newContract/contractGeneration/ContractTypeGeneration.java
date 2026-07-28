package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.clamp;
import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.enums.AtBContractType;

public class ContractTypeGeneration {
    private AtBContractType playerContractType;
    private AtBContractType opposingContractType;

    public ContractTypeGeneration(int contractGenerationModifier) {
        generateContractType(contractGenerationModifier);
    }

    public AtBContractType getPlayerContractType() {
        return playerContractType;
    }

    public AtBContractType getOpposingContractType() {
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
        playerContractType = AtBContractType.INVASION;

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4 -> AtBContractType.EXPEDITION;
            case 5, 6, 8, 7 -> AtBContractType.GARRISON;
            case 9 -> AtBContractType.RAID;
            case 10, 11, 12 -> AtBContractType.INVASION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }

    private void generateRaidContractTypes(int roll) {
        playerContractType = AtBContractType.RAID;

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4, 5, 6, 7 -> AtBContractType.EXPEDITION;
            case 8, 9, 10 -> AtBContractType.GARRISON;
            case 11 -> AtBContractType.RAID;
            case 12 -> AtBContractType.INVASION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }

    private void generateGarrisonContractTypes(int roll) {
        int playerFollowUpRoll = d6(2);
        playerContractType = switch (playerFollowUpRoll) {
            case 2, 3, 4, 5, 6 -> AtBContractType.CADRE_DUTY;
            case 7, 8, 9, 10, 11, 12 -> AtBContractType.GARRISON;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4 -> AtBContractType.EXPEDITION;
            case 5, 6, 7, 8 -> AtBContractType.RAID;
            case 9, 10, 11, 12 -> AtBContractType.INVASION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }

    private void generateExpeditionContractTypes(int roll) {
        int playerFollowUpRoll = d6(2);
        playerContractType = switch (playerFollowUpRoll) {
            case 2, 3, 4, 5, 6, 7 -> AtBContractType.STANDARD_EXPEDITION;
            case 8, 9, 10, 11 -> AtBContractType.PIRATE_HUNT;
            case 12 -> AtBContractType.GUERILLA_OPERATION;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };

        int opposingFollowUpRoll = d6(2);
        opposingContractType = switch (opposingFollowUpRoll) {
            case 2, 3, 4, 5, 6, 7 -> AtBContractType.GARRISON;
            case 8, 9, 10, 11, 12 -> AtBContractType.RAID;
            default -> throw new IllegalArgumentException("Invalid roll value " + roll);
        };
    }
}
