package mekhq.campaign.personnel.randomEvents;

import megamek.common.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.randomEvents.enums.personalities.*;

import java.util.*;

import static mekhq.campaign.personnel.randomEvents.enums.personalities.Intelligence.*;

public class PersonalityController {
    public static void generatePersonality(Person person) {
        // first, we wipe any pre-existing personality traits
        person.setAggression(Aggression.NONE);
        person.setAmbition(Ambition.NONE);
        person.setGreed(Greed.NONE);
        person.setSocial(Social.NONE);
        person.setPersonalityQuirk(PersonalityQuirk.NONE);

        // next, we roll to determine which tables we're rolling on,
        // then we roll to determine what traits we get on those tables
        for (int table = 0; table < 4; table++) {
            // we only want a 1 in 6 chance of getting a personality trait, per table
            // this prevents trait bloat and helps reduce repetitiveness
            if (Compute.randomInt(6) == 0) {
                setPersonalityTrait(person, table, Compute.randomInt(25) + 1);
            }
        }

        // we only want 1 in 10 persons to have a quirk,
        // as these helps reduce repetitiveness and keeps them unique
        if (Compute.randomInt(10) == 0) {
            person.setPersonalityQuirk(generatePersonalityQuirk());
        }

        person.setIntelligence(generateIntelligence(Compute.randomInt(1000)));

        // finally, write the description
        writeDescription(person);

        // check at least one characteristic has been generated, if not, then repeat the process
        // while this might create a couple of loops,
        // probability says we can only expect 1 additional loop, 2 in exceptional circumstances
        if (Objects.equals(person.getPersonalityDescription(), "")) {
            generatePersonality(person);
        }
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
        // We want major traits to have a low chance of occurring.
        // This ensures each trait only has a 1 in 25 chance of spawning
        if (traitRoll == 25) {
            traitRoll += Compute.d6(1) - 1;
        }

        switch (tableRoll) {
            case 0 -> person.setAggression(Aggression.parseFromInt(traitRoll));
            case 1 -> person.setAmbition(Ambition.parseFromInt(traitRoll));
            case 2 -> person.setGreed(Greed.parseFromInt(traitRoll));
            case 3 -> person.setSocial(Social.parseFromInt(traitRoll));
            default -> throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/setPersonalityTrait: "
                    + tableRoll);
        }
    }

    /**
     * Sets the personality description of a person based on their personality traits.
     *
     * @param person         the person whose personality description will be set
     */
    public static void writeDescription(Person person) {
        List<String> traitDescriptions = getTraitDescriptions(person);

        // It is beneficial to shuffle descriptions for variety.
        Collections.shuffle(traitDescriptions);

        StringBuilder personalityDescription = new StringBuilder();

        String firstName = person.getFirstName();

        // The use of capitalized gender-neutral 'they'.
        String pronoun = GenderDescriptors.HE_SHE_THEY.getDescriptorCapitalized(Gender.OTHER_FEMALE);

        for (int index = 0; index < traitDescriptions.size(); index++) {

            // Define "forward" and "plural" based on the index value.
            // If the index is an even number, use first name; otherwise use pronoun.
            // The alternation between first name and pronoun adds variety to the descriptions.
            // The "plural" string is only used when the first name is used.
            String forward = ((index % 2) == 0) ? firstName : pronoun;
            String plural = ((index % 2) == 0) ? "s" : "";

            // We only append a space between descriptions, not at the start.
            personalityDescription.append(' ');

            personalityDescription.append(String.format(traitDescriptions.get(index), forward, plural));
        }

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

        if (!person.getIntelligence().isAverage()) {
            traitDescriptions.add(person.getIntelligence().getDescription());
        }

        if (!person.getPersonalityQuirk().isNone()) {
            traitDescriptions.add(person.getPersonalityQuirk().getDescription());
        }

        return traitDescriptions;
    }

    /**
     * @return a random personality quirk for a person.
     */
    private static PersonalityQuirk generatePersonalityQuirk() {
        Random random = new Random();
        PersonalityQuirk[] values = PersonalityQuirk.values();

        PersonalityQuirk randomQuirk = PersonalityQuirk.NONE;

        // we want to keep re-rolling until we hit a quirk that isn't 'NONE.'
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
            return FOOLISH;
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
