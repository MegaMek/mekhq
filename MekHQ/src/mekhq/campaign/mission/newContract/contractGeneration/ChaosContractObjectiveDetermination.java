package mekhq.campaign.mission.newContract.contractGeneration;

import static java.lang.Math.clamp;
import static megamek.common.compute.Compute.d6;

import mekhq.campaign.mission.newContract.ContractObjectiveData;
import org.jspecify.annotations.NonNull;

public class ChaosContractObjectiveDetermination {
    public static ContractObjectiveData determineContractObjectiveType(int contractGenerationModifier) {
        int roll = d6(2);
        int result = clamp(roll + contractGenerationModifier, 1, 13);

        // Hot Spots Draconis Reach, pg 143, first printing
        return switch (result) {
            case 1, 2, 3, 4 -> {
                ChaosObjectiveType playerObjective = getExpeditionPlayerObjective();
                ChaosObjectiveType opposingObjective = getExpeditionOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 5, 6 -> {
                ChaosObjectiveType playerObjective = getGarrisonPlayerObjective();
                ChaosObjectiveType opposingObjective = getGarrisonOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 7, 8, 9 -> {
                ChaosObjectiveType playerObjective = getRaidPlayerObjective();
                ChaosObjectiveType opposingObjective = getRaidOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            case 10, 11, 12, 13 -> {
                ChaosObjectiveType playerObjective = getInvasionPlayerObjective();
                ChaosObjectiveType opposingObjective = getInvasionOpposingObjective();
                yield new ContractObjectiveData(playerObjective, opposingObjective);
            }
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static @NonNull ChaosObjectiveType getInvasionOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4 -> ChaosObjectiveType.EXPEDITION;
            case 5, 6, 7, 8 -> ChaosObjectiveType.GARRISON;
            case 9 -> ChaosObjectiveType.RAID;
            case 10, 11, 12 -> ChaosObjectiveType.INVASION;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ChaosObjectiveType getInvasionPlayerObjective() {
        return ChaosObjectiveType.INVASION;
    }

    private static @NonNull ChaosObjectiveType getRaidPlayerObjective() {
        return ChaosObjectiveType.RAID;
    }

    private static ChaosObjectiveType getRaidOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4, 5, 6, 7 -> ChaosObjectiveType.EXPEDITION;
            case 8, 9, 10 -> ChaosObjectiveType.GARRISON;
            case 11 -> ChaosObjectiveType.RAID;
            case 12 -> ChaosObjectiveType.INVASION;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static @NonNull ChaosObjectiveType getGarrisonOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4 -> ChaosObjectiveType.EXPEDITION;
            case 5, 6, 7, 8 -> ChaosObjectiveType.RAID;
            case 9, 10, 11, 12 -> ChaosObjectiveType.INVASION;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ChaosObjectiveType getGarrisonPlayerObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 3, 4, 5 -> ChaosObjectiveType.CADRE_DUTY;
            case 6, 7, 8, 9, 10, 11, 12 -> ChaosObjectiveType.GARRISON;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ChaosObjectiveType getExpeditionOpposingObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 8, 7, 3, 4, 5, 6 -> ChaosObjectiveType.GARRISON;
            case 9, 10, 11, 12 -> ChaosObjectiveType.RAID;
            default -> throw new IllegalStateException("Illegal roll number: " + roll);
        };
    }

    private static ChaosObjectiveType getExpeditionPlayerObjective() {
        int roll = d6(2);
        return switch (roll) {
            case 1, 8, 7, 6, 5, 4, 3, 2 -> ChaosObjectiveType.EXPEDITION;
            case 9, 10, 11 -> ChaosObjectiveType.PIRATE_HUNT;
            case 12 -> ChaosObjectiveType.GUERILLA_OPERATION;
            default -> throw new IllegalStateException("Unexpected value: " + roll);
        };
    }
}
