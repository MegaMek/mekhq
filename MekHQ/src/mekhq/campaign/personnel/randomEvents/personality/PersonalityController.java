package mekhq.campaign.personnel.randomEvents.personality;

import megamek.common.Compute;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Greed;
import mekhq.campaign.personnel.enums.randomEvents.personalities.Social;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Greed.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Social.*;

public class PersonalityController {
    public static void generatePersonality(Person person) {
        // first we wipe any pre-existing personality traits
        person.setAggression(Aggression.NONE);
        person.setAmbition(Ambition.NONE);
        person.setGreed(Greed.NONE);
        person.setSocial(Social.NONE);

        // next we roll to determine which tables we're rolling on
        // then we roll to determine what traits we get on those tables
        int firstTableRoll = Compute.randomInt(4);
        int firstTraitRoll = Compute.randomInt(13);

        int secondTableRoll = Compute.randomInt(4);
        int secondTraitRoll = Compute.randomInt(13);

        // characters cannot have two personality traits from the same table,
        // so we re-roll until we get different tables
        while (firstTableRoll == secondTableRoll) {
            secondTableRoll = Compute.randomInt(4);
        }

        // characters cannot have two major personality traits, so we re-roll until we get a general trait
        while ((firstTraitRoll > 8) && (secondTraitRoll > 8)) {
            secondTraitRoll = Compute.randomInt(13);
        }

        // next set the new traits
        setPersonalityTraits(person, firstTableRoll, firstTraitRoll);
        setPersonalityTraits(person, secondTableRoll, secondTraitRoll);

        // finally, write the description
        writeDescription(person);
    }

    /**
     * Sets the personality description of a person based on their personality traits.
     *
     * @param person         the person whose personality description will be set
     */
    public static void writeDescription(Person person) {
        // first we gather the personality trait descriptions and place them in a list
        List<String> traitDescriptions = getTraitDescriptions(person);

        // we shuffle the order so the elements are not always printed in the same order
        Collections.shuffle(traitDescriptions);

        // next we build a string that contains all the descriptions
        StringBuilder personalityDescription = new StringBuilder();

        String firstName = person.getFirstName();
        String pronoun = GenderDescriptors.HE_SHE_THEY.getDescriptorCapitalized(person.getGender());

        for (int index = 0; index < traitDescriptions.size(); index++) {
            String forward = pronoun;

            if (index == 0) {
                forward = firstName;
            } else {
                personalityDescription.append(' ');
            }

            personalityDescription.append(String.format(traitDescriptions.get(index), forward));
        }

        // finally we set the description in place
        person.setPersonalityDescription(personalityDescription.toString());
    }

    /**
     * Returns a list of trait descriptions for a given person.
     *
     * @param person the person for whom to retrieve the trait descriptions
     * @return a list of trait descriptions for the person
     */
    private static List<String> getTraitDescriptions(Person person) {
        List<String> traitDescriptions = new ArrayList<>();

        if (!person.getAggression().isNone()) {
            traitDescriptions.add(person.getAggression().getDescription());
        }

        if (!person.getAmbition().isNone()) {
            traitDescriptions.add(person.getAmbition().getDescription());
        }

        if (!person.getGreed().isNone()) {
            traitDescriptions.add(person.getGreed().getDescription());
        }

        if (!person.getSocial().isNone()) {
            traitDescriptions.add(person.getSocial().getDescription());
        }

        return traitDescriptions;
    }

    /**
     * Sets the personality traits of a person based on the given table roll and trait roll.
     *
     * @param person the person whose personality traits will be set
     * @param tableRoll the table roll used to determine which personality trait to set
     * @param traitRoll the roll used to generate the value of the personality trait
     * @throws IllegalStateException if an unexpected value is rolled for tableRoll parameter
     */
    private static void setPersonalityTraits(Person person, int tableRoll, int traitRoll) {
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
    private static Aggression generateAggression(int roll) {
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
    private static Ambition generateAmbition(int roll) {
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
    private static Greed generateGreed(int roll) {
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
    private static Social generateSocial(int roll) {
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
