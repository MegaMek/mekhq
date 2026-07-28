package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.clamp;
import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.enums.AtBContractType;
import mekhq.campaign.mission.newContract.ContractObjectiveData;
import org.jspecify.annotations.NonNull;

public class ChaosContractObjectiveDetermination {
    public static ContractObjectiveData determineContractObjectiveType(int contractGenerationModifier) {
        int roll = d6(2);
        int result = clamp(roll + contractGenerationModifier, 1, 13);

        // Hot Spots Draconis Reach, pg 143, first printing
        return switch (result) {
            case 1, 2, 3, 4 -> {
                AtBContractType playerObjective = getExpeditionPlayerObjective();
                AtBContractType opposingObjective = getExpeditionOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 5, 6 -> {
                AtBContractType playerObjective = getGarrisonPlayerObjective();
                AtBContractType opposingObjective = getGarrisonOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 7, 8, 9 -> {
                AtBContractType playerObjective = getRaidPlayerObjective();
                AtBContractType opposingObjective = getRaidOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 10, 11, 12, 13 -> {
                AtBContractType playerObjective = getInvasionPlayerObjective();
                AtBContractType opposingObjective = getInvasionOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static @NonNull AtBContractType getInvasionOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4 -> AtBContractType.EXPEDITION;
            case 5, 6, 7, 8 -> AtBContractType.GARRISON;
            case 9 -> AtBContractType.RAID;
            case 10, 11, 12 -> AtBContractType.INVASION;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static AtBContractType getInvasionPlayerObjective() {
        return AtBContractType.INVASION;
    }

    private static @NonNull AtBContractType getRaidPlayerObjective() {
        return AtBContractType.RAID;
    }

    private static AtBContractType getRaidOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4, 5, 6, 7 -> AtBContractType.EXPEDITION;
            case 8, 9, 10 -> AtBContractType.GARRISON;
            case 11 -> AtBContractType.RAID;
            case 12 -> AtBContractType.INVASION;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static @NonNull AtBContractType getGarrisonOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4 -> AtBContractType.EXPEDITION;
            case 5, 6, 7, 8 -> AtBContractType.RAID;
            case 9, 10, 11, 12 -> AtBContractType.INVASION;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static AtBContractType getGarrisonPlayerObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4, 5 -> AtBContractType.CADRE_DUTY;
            case 6, 7, 8, 9, 10, 11, 12 -> AtBContractType.GARRISON;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static AtBContractType getExpeditionOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 8, 7, 3, 4, 5, 6 -> AtBContractType.GARRISON;
            case 9, 10, 11, 12 -> AtBContractType.RAID;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static AtBContractType getExpeditionPlayerObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 1, 8, 7, 6, 5, 4, 3, 2 -> AtBContractType.EXPEDITION;
            case 9, 10, 11 -> AtBContractType.PIRATE_HUNT;
            case 12 -> AtBContractType.GUERILLA_OPERATION;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }
}
