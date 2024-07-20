package mekhq.campaign.personnel.randomEvents.personality;

import megamek.common.Compute;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Greed;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Social;

import static mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Greed.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Social.*;

public class PersonalityController {

    /**
     * Generates an Aggression enum value based on a random roll.
     *
     * @return The generated Aggression enum value.
     * @throws IllegalStateException if an unexpected value is rolled.
     */
    private Aggression generateAggression() {
        int roll = Compute.randomInt(12);

        return switch (roll) {
            case 0, 1, 2 -> PEACEFUL;
            case 3, 4, 5 -> PROFESSIONAL;
            case 6, 7, 8 -> STUBBORN;
            case 9 -> AGGRESSIVE;
            case 10 -> BRUTAL;
            case 11 -> BLOODTHIRSTY;
            case 12 -> MURDEROUS;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateAggression: "
                            + roll);
        };
    }

    /**
     * Generates an Ambition enum value based on a random roll.
     *
     * @return The generated Ambition enum value.
     * @throws IllegalStateException if an unexpected value is rolled.
     */
    private Ambition generateAmbition() {
        int roll = Compute.randomInt(12);

        return switch (roll) {
            case 0, 1, 2 -> UNAMBITIOUS;
            case 3, 4, 5 -> DRIVEN;
            case 6, 7, 8 -> ASSERTIVE;
            case 9 -> ARROGANT;
            case 10 -> CONTROLLING;
            case 11 -> RUTHLESS;
            case 12 -> DECEITFUL;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateAmbition: "
                            + roll);
        };
    }

    /**
     * Generates an Greed enum value based on a random roll.
     *
     * @return The generated Greed enum value.
     * @throws IllegalStateException if an unexpected value is rolled.
     */
    private Greed generateGreed() {
        int roll = Compute.randomInt(12);

        return switch (roll) {
            case 0, 1, 2 -> GENEROUS;
            case 3, 4, 5 -> FRUGAL;
            case 6, 7, 8 -> GREEDY;
            case 9 -> SELFISH;
            case 10 -> INSATIABLE;
            case 11 -> LUSTFUL;
            case 12 -> THIEF;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateGreed: "
                            + roll);
        };
    }

    /**
     * Generates an Social enum value based on a random roll.
     *
     * @return The generated Social enum value.
     * @throws IllegalStateException if an unexpected value is rolled.
     */
    private Social generateSocial() {
        int roll = Compute.randomInt(12);

        return switch (roll) {
            case 0, 1, 2 -> RECLUSIVE;
            case 3, 4, 5 -> RESILIENT;
            case 6, 7, 8 -> TEMPERATE;
            case 9 -> WISE;
            case 10 -> LOVING;
            case 11 -> IMPARTIAL;
            case 12 -> HONORABLE;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateSocial: "
                            + roll);
        };
    }

}
