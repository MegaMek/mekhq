package mekhq.campaign.personnel.randomEvents;

import megamek.common.Compute;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.randomEvents.personalities.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static mekhq.campaign.personnel.enums.randomEvents.personalities.Aggression.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Ambition.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Greed.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Intelligence.*;
import static mekhq.campaign.personnel.enums.randomEvents.personalities.Social.*;

public class PersonalityController {
    public static void generatePersonality(Person person) {
        // first we wipe any pre-existing personality traits
        person.setAggression(Aggression.NONE);
        person.setAmbition(Ambition.NONE);
        person.setGreed(Greed.NONE);
        person.setSocial(Social.NONE);
        person.setPersonalityQuirk(PersonalityQuirk.NONE);

        // next we roll to determine which tables we're rolling on
        // then we roll to determine what traits we get on those tables
        int firstTableRoll = Compute.randomInt(4);
        int firstTraitRoll = Compute.randomInt(25);

        int secondTableRoll = Compute.randomInt(4);
        int secondTraitRoll = Compute.randomInt(25);

        int majorTraitRoll = Compute.d6(1);

        // characters cannot have two personality traits from the same table,
        // so we re-roll until we get different tables
        while (firstTableRoll == secondTableRoll) {
            secondTableRoll = Compute.randomInt(4);
        }

        // characters cannot have two major personality traits, so we re-roll
        if ((firstTraitRoll == 24) && (secondTraitRoll == 24)) {
            secondTraitRoll = Compute.randomInt(24);
        }

        // next set the new traits
        if (firstTraitRoll == 24) {
            setMajorPersonalityTrait(person, firstTableRoll, majorTraitRoll);
        } else {
            setPersonalityTrait(person, firstTableRoll, firstTraitRoll);
        }

        if (secondTraitRoll == 24) {
            setMajorPersonalityTrait(person, secondTableRoll, majorTraitRoll);
        } else {
            setPersonalityTrait(person, secondTableRoll, secondTraitRoll);
        }

        // we only want 1in20 persons to have a quirk,
        // as these helps reduce repetitiveness and keeps them unique
        if (Compute.randomInt(20) == 0) {
            person.setPersonalityQuirk(generatePersonalityQuirk());
        }

        person.setIntelligence(generateIntelligence(Compute.randomInt(1000)));

        // finally, write the description
        writeDescription(person);
    }

    /**
     * Sets the personality traits of a person based on the given table roll and trait roll.
     *
     * @param person the person whose personality traits will be set
     * @param tableRoll the table roll used to determine which personality trait to set
     * @param traitRoll the roll used to generate the value of the personality trait
     * @throws IllegalStateException if an unexpected value is rolled for tableRoll parameter
     */
    private static void setPersonalityTrait(Person person, int tableRoll, int traitRoll) {
        switch (tableRoll) {
            case 0 -> person.setAggression(generateAggression(traitRoll));
            case 1 -> person.setAmbition(generateAmbition(traitRoll));
            case 2 -> person.setGreed(generateGreed(traitRoll));
            case 3 -> person.setSocial(generateSocial(traitRoll));
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/setPersonalityTrait: "
                    + tableRoll);
        }
    }

    /**
     * Sets the major personality traits of a person based on the given table roll and trait roll.
     *
     * @param person the person whose personality traits will be set
     * @param tableRoll the table roll used to determine which personality trait to set
     * @param traitRoll the roll used to generate the value of the personality trait
     * @throws IllegalStateException if an unexpected value is rolled for tableRoll parameter
     */
    private static void setMajorPersonalityTrait(Person person, int tableRoll, int traitRoll) {
        switch (tableRoll) {
            case 0 -> person.setAggression(generateAggressionMajorTrait(traitRoll));
            case 1 -> person.setAmbition(generateAmbitionMajorTrait(traitRoll));
            case 2 -> person.setGreed(generateGreedMajorTrait(traitRoll));
            case 3 -> person.setSocial(generateSocialMajorTrait(traitRoll));
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/setMajorPersonalityTrait: "
                    + tableRoll);
        }
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
        // these CSS tags are needed to avoid overspilling on the AtB Personnel Market
        StringBuilder personalityDescription = new StringBuilder("<html><span style='display: inline-block; width: 200px;'>");

        String firstName = person.getFirstName();
        String pronoun = GenderDescriptors.HE_SHE_THEY.getDescriptorCapitalized(person.getGender());
        String forward = pronoun;

        for (int index = 0; index < traitDescriptions.size(); index++) {
            forward = pronoun;

            if (index == 0) {
                forward = firstName;
            } else {
                personalityDescription.append(' ');
            }

            personalityDescription.append(String.format(traitDescriptions.get(index), forward));
        }

        forward = firstName;

        // if the individual has a personality quirk, we add that next
        if (person.getPersonalityQuirk() != PersonalityQuirk.NONE) {
            personalityDescription.append(' ');
            personalityDescription.append(String.format(person.getPersonalityQuirk().getDescription(), firstName));

            forward = pronoun;
        }

        // if the individual has intelligence other than average, add that last
        if (person.getIntelligence() != AVERAGE) {
            personalityDescription.append(' ');
            personalityDescription.append(String.format(person.getIntelligence().getDescription(), forward));
        }

        // close off the CSS tags
        personalityDescription.append("</span></html>");

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
     * Generates an Aggression enum value based on the given roll.
     *
     * @param roll the random roll used to determine the Aggression enum value
     * @return the generated Aggression enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private static Aggression generateAggression(int roll) {
        return switch (roll) {
            case 0 -> BOLD;
            case 1 -> AGGRESSIVE;
            case 2 -> ASSERTIVE;
            case 3 -> BELLIGERENT;
            case 4 -> BRASH;
            case 5 -> CONFIDENT;
            case 6 -> COURAGEOUS;
            case 7 -> DARING;
            case 8 -> DECISIVE;
            case 9 -> DETERMINED;
            case 10 -> DOMINEERING;
            case 11 -> FEARLESS;
            case 12 -> HOSTILE;
            case 13 -> HOT_HEADED;
            case 14 -> IMPETUOUS;
            case 15 -> IMPULSIVE;
            case 16 -> INFLEXIBLE;
            case 17 -> INTREPID;
            case 18 -> OVERBEARING;
            case 19 -> RECKLESS;
            case 20 -> RESOLUTE;
            case 21 -> STUBBORN;
            case 22 -> TENACIOUS;
            case 23 -> VIGILANT;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateAggression: "
                            + roll);
        };
    }

    /**
     * Generates an Aggression major trait enum based on the given roll.
     *
     * @param roll the random roll used to determine the Aggression enum value
     * @return the generated Aggression enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private static Aggression generateAggressionMajorTrait(int roll) {
        return switch (roll) {
            case 1 -> BLOODTHIRSTY;
            case 2 -> DIPLOMATIC;
            case 3 -> MURDEROUS;
            case 4 -> PACIFISTIC;
            case 5 -> SADISTIC;
            case 6 -> SAVAGE;
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateAggressionMajorTrait: "
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
            case 0 -> AMBITIOUS;
            case 1 -> ARROGANT;
            case 2 -> ASPIRING;
            case 3 -> CALCULATING;
            case 4 -> CONNIVING;
            case 5 -> CONTROLLING;
            case 6 -> CUTTHROAT;
            case 7 -> DILIGENT;
            case 8 -> DRIVEN;
            case 9 -> ENERGETIC;
            case 10 -> EXCESSIVE;
            case 11 -> FOCUSED;
            case 12 -> MANIPULATIVE;
            case 13 -> MOTIVATED;
            case 14 -> OPPORTUNISTIC;
            case 15 -> OVERCONFIDENT;
            case 16 -> PERSISTENT;
            case 17 -> PROACTIVE;
            case 18 -> RESILIENT;
            case 19 -> RUTHLESS;
            case 20 -> SELFISH;
            case 21 -> STRATEGIC;
            case 22 -> UNAMBITIOUS;
            case 23 -> UNSCRUPULOUS;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateAmbition: "
                            + roll);
        };
    }

    /**
     * Generates an Ambition major trait enum based on the given roll.
     *
     * @param roll the random roll used to determine the Ambition enum value
     * @return the generated Ambition enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private static Ambition generateAmbitionMajorTrait(int roll) {
        return switch (roll) {
            case 1 -> DISHONEST;
            case 2 -> INNOVATIVE;
            case 3 -> MANIPULATIVE;
            case 4 -> RESOURCEFUL;
            case 5 -> TYRANNICAL;
            case 6 -> VISIONARY;
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateAmbitionMajorTrait: "
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
            case 0 -> ASTUTE;
            case 1 -> ADEPT;
            case 2 -> AVARICIOUS;
            case 3 -> DYNAMIC;
            case 4 -> EAGER;
            case 5 -> EXPLOITATIVE;
            case 6 -> FRAUDULENT;
            case 7 -> GENEROUS;
            case 8 -> GREEDY;
            case 9 -> HOARDING;
            case 10 -> INSATIABLE;
            case 11 -> INSIGHTFUL;
            case 12 -> JUDICIOUS;
            case 13 -> LUSTFUL;
            case 14 -> MERCENARY;
            case 15 -> OVERREACHING;
            case 16 -> PROFITABLE;
            case 17 -> SAVVY;
            case 18 -> SELF_SERVING;
            case 19 -> SHAMELESS;
            case 20 -> SHREWD;
            case 21 -> TACTICAL;
            case 22 -> UNPRINCIPLED;
            case 23 -> VORACIOUS;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateGreed: "
                            + roll);
        };
    }

    /**
     * Generates an Greed major trait enum based on the given roll.
     *
     * @param roll the random roll used to determine the Greed enum value
     * @return the generated Greed enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private static Greed generateGreedMajorTrait(int roll) {
        return switch (roll) {
            case 1 -> CORRUPT;
            case 2 -> ENTERPRISING;
            case 3 -> INTUITIVE;
            case 4 -> METICULOUS;
            case 5 -> NEFARIOUS;
            case 6 -> THIEF;
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateGreedMajorTrait: "
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
            case 0 -> APATHETIC;
            case 1 -> AUTHENTIC;
            case 2 -> BLUNT;
            case 3 -> CALLOUS;
            case 4 -> CONDESCENDING;
            case 5 -> CONSIDERATE;
            case 6 -> DISINGENUOUS;
            case 7 -> DISMISSIVE;
            case 8 -> ENCOURAGING;
            case 9 -> ERRATIC;
            case 10 -> EMPATHETIC;
            case 11 -> FRIENDLY;
            case 12 -> INSPIRING;
            case 13 -> INDIFFERENT;
            case 14 -> INTROVERTED;
            case 15 -> IRRITABLE;
            case 16 -> NEGLECTFUL;
            case 17 -> PETTY;
            case 18 -> PERSUASIVE;
            case 19 -> RECEPTIVE;
            case 20 -> SINCERE;
            case 21 -> SUPPORTIVE;
            case 22 -> TACTFUL;
            case 23 -> UNTRUSTWORTHY;
            default ->
                    throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateSocial: "
                            + roll);
        };
    }

    /**
     * Generates an Social major trait enum based on the given roll.
     *
     * @param roll the random roll used to determine the Social enum value
     * @return the generated Social enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private static Social generateSocialMajorTrait(int roll) {
        return switch (roll) {
            case 1 -> ALTRUISTIC;
            case 2 -> COMPASSIONATE;
            case 3 -> GREGARIOUS;
            case 4 -> NARCISSISTIC;
            case 5 -> POMPOUS;
            case 6 -> SCHEMING;
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/generateSocialMajorTrait: "
                    + roll);
        };
    }

    /**
     * @return a random personality quirk for a person.
     */
    private static PersonalityQuirk generatePersonalityQuirk() {
        Random random = new Random();
        PersonalityQuirk[] values = PersonalityQuirk.values();

        PersonalityQuirk randomQuirk = PersonalityQuirk.NONE;

        // we want to keep re-rolling until we hit a quirk that isn't 'NONE'
        while (randomQuirk == PersonalityQuirk.NONE) {
            randomQuirk = values[random.nextInt(values.length)];
        }

        return randomQuirk;
    }

    /**
     * Generates an Intelligence enum value based on a random roll.
     *
     * @param roll the random roll used to determine the Intelligence enum value
     * @return the generated Intelligence enum value
     * @throws IllegalStateException if an unexpected value is rolled
     */
    private static Intelligence generateIntelligence(int roll) {
        if (roll < 5) {
            return BRAIN_DEAD;
        } else if (roll < 14) {
            return UNINTELLIGENT;
        } else if (roll < 29) {
            return FEEBLE_MINDED;
        } else if (roll < 49) {
            return SIMPLE;
        } else if (roll < 79) {
            return SLOW;
        } else if (roll < 119) {
            return UNINSPIRED;
        } else if (roll < 159) {
            return DULL;
        } else if (roll < 199) {
            return DIMWITTED;
        } else if (roll < 239) {
            return OBTUSE;
        } else if (roll < 269) {
            return BELOW_AVERAGE;
        } else if (roll < 309) {
            return UNDER_PERFORMING;
        } else if (roll < 375) {
            return LIMITED_INSIGHT;
        } else if (roll < 625) {
            return AVERAGE;
        } else if (roll < 691) {
            return ABOVE_AVERAGE;
        } else if (roll < 731) {
            return STUDIOUS;
        } else if (roll < 771) {
            return DISCERNING;
        } else if (roll < 811) {
            return SHARP;
        } else if (roll < 851) {
            return QUICK_WITTED;
        } else if (roll < 891) {
            return PERCEPTIVE;
        } else if (roll < 921) {
            return BRIGHT;
        } else if (roll < 951) {
            return CLEVER;
        } else if (roll < 971) {
            return INTELLECTUAL;
        } else if (roll < 986) {
            return BRILLIANT;
        } else if (roll < 995) {
            return EXCEPTIONAL;
        } else {
            return GENIUS;
        }
    }
}
