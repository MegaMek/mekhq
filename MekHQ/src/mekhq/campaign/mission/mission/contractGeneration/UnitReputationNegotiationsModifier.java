package mekhq.campaign.mission.mission.contractGeneration;

import static java.lang.Math.clamp;

import megamek.logging.MMLogger;

public enum UnitReputationNegotiationsModifier {
    UNIT_REPUTATION_RATING_0(-2, -1, -1, -3),
    UNIT_REPUTATION_RATING_1(-1, -1, -1, -2),
    UNIT_REPUTATION_RATING_2(-1, 0, 0, -2),
    UNIT_REPUTATION_RATING_3(-1, 0, 0, -1),
    UNIT_REPUTATION_RATING_4(0, 0, 0, -1),
    UNIT_REPUTATION_RATING_5(0, 0, 0, 0),
    UNIT_REPUTATION_RATING_6(1, 1, 0, 0),
    UNIT_REPUTATION_RATING_7(1, 1, 0, 0),
    UNIT_REPUTATION_RATING_8(1, 1, 1, 0),
    UNIT_REPUTATION_RATING_9(2, 2, 1, 1),
    UNIT_REPUTATION_RATING_10_PLUS(3, 2, 2, 2);

    private final int commandModifier;
    private final int salvageModifier;
    private final int supportModifier;
    private final int transportModifier;

    private final static MMLogger LOGGER = MMLogger.create(UnitReputationNegotiationsModifier.class);

    UnitReputationNegotiationsModifier(final int commandModifier, final int salvageModifier, final int supportModifier,
          final int transportModifier) {
        this.commandModifier = commandModifier;
        this.salvageModifier = salvageModifier;
        this.supportModifier = supportModifier;
        this.transportModifier = transportModifier;
    }

    public int getCommandModifier() {
        return commandModifier;
    }

    public int getSalvageModifier() {
        return salvageModifier;
    }

    public int getSupportModifier() {
        return supportModifier;
    }

    public int getTransportModifier() {
        return transportModifier;
    }

    public static void getNegotiationsModifier(int reputationRating, NegotiationsModifierData modifierData) {
        int clampedRating = clamp(reputationRating, 0, 10);

        UnitReputationNegotiationsModifier modifierEntry = switch (clampedRating) {
            case 0 -> UNIT_REPUTATION_RATING_0;
            case 1 -> UNIT_REPUTATION_RATING_1;
            case 2 -> UNIT_REPUTATION_RATING_2;
            case 3 -> UNIT_REPUTATION_RATING_3;
            case 4 -> UNIT_REPUTATION_RATING_4;
            case 5 -> UNIT_REPUTATION_RATING_5;
            case 6 -> UNIT_REPUTATION_RATING_6;
            case 7 -> UNIT_REPUTATION_RATING_7;
            case 8 -> UNIT_REPUTATION_RATING_8;
            case 9 -> UNIT_REPUTATION_RATING_9;
            case 10 -> UNIT_REPUTATION_RATING_10_PLUS;
            default -> {
                LOGGER.error("Unexpected reputation rating: {}", clampedRating);
                yield null;
            }
        };

        if (modifierEntry != null) {
            applyNegotiationModifiers(modifierEntry, modifierData);
        }
    }

    private static void applyNegotiationModifiers(UnitReputationNegotiationsModifier modifierEntry,
          NegotiationsModifierData modifierData) {
        modifierData.modifyCommandModifier(modifierEntry.getCommandModifier());
        modifierData.modifySalvageModifier(modifierEntry.getSalvageModifier());
        modifierData.modifySupportModifier(modifierEntry.getSupportModifier());
        modifierData.modifyTransportModifier(modifierEntry.getTransportModifier());
    }
}
