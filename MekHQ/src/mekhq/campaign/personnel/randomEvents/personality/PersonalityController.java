package mekhq.campaign.personnel.randomEvents.personality;

import megamek.common.Compute;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Greed;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Social;

import static mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Greed.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Social.*;

public class PersonalityController {
    public void generatePersonality(Person person) {
        // first we wipe any pre-existing personality traits
        person.setAggression(Aggression.NONE);
        person.setAmbition(Ambition.NONE);
        person.setGreed(Greed.NONE);
        person.setSocial(Social.NONE);

        // next we roll to determine which tables we're rolling on
        // then we roll to determine what we get on that table
        int firstTableRoll = Compute.randomInt(4);
        int firstTraitRoll = Compute.randomInt(13);

        int secondTableRoll = Compute.randomInt(4);
        int secondTraitRoll = Compute.randomInt(13);

        // characters cannot have two major personality traits, so we re-roll until we get a general trait
        while ((firstTraitRoll > 8) && (secondTraitRoll > 8)) {
            secondTraitRoll = Compute.randomInt(13);
        }

        // finally set the new traits
        setPersonalityTraits(person, firstTableRoll, firstTraitRoll);
        setPersonalityTraits(person, secondTableRoll, secondTraitRoll);
    }

    /**
     * Sets the personality traits of a person based on the given table roll and trait roll.
     *
     * @param person the person whose personality traits will be set
     * @param tableRoll the table roll used to determine which personality trait to set
     * @param traitRoll the roll used to generate the value of the personality trait
     * @throws IllegalStateException if an unexpected value is rolled for tableRoll parameter
     */
    private void setPersonalityTraits(Person person, int tableRoll, int traitRoll) {
        switch (tableRoll) {
            case 0 -> person.setAggression(generateAggression(traitRoll));
            case 1 -> person.setAmbition(generateAmbition(traitRoll));
            case 2 -> person.setGreed(generateGreed(traitRoll));
            case 3 -> person.setSocial(generateSocial(traitRoll));
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/setPersonalityTraits: "
                    + tableRoll);
        }
    }


    /**
     * Generates an Aggression enum value based on the given roll.
     *
     * @param roll the random roll used to determine the Aggression enum value
     * @return the generated Aggression enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private Aggression generateAggression(int roll) {
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
     * Generates an Ambition enum value based on the given roll.
     *
     * @param roll the random roll used to determine the Ambition enum value
     * @return the generated Ambition enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private Ambition generateAmbition(int roll) {
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
     * Generates a Greed enum value based on a random roll.
     *
     * @param roll the random roll used to determine the Greed enum value
     * @return the generated Greed enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private Greed generateGreed(int roll) {
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
     * Generates a Social enum value based on a random roll.
     *
     * @param roll the random roll used to determine the Social enum value
     * @return the generated Social enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private Social generateSocial(int roll) {
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
