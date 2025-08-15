/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.randomEvents.personalities;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.randomEvents.personalities.enums.Reasoning.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.enums.Gender;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType;
import mekhq.campaign.randomEvents.personalities.enums.Reasoning;
import mekhq.campaign.randomEvents.personalities.enums.Social;

/**
 * This class is responsible for generating and managing personalities for characters. It assigns traits such as
 * aggression, ambition, greed, social behavior, reasoning, and personality quirks to a person, and generates a
 * descriptive personality summary.
 *
 * <p>Additionally, this class includes methods for handling fallback personality logic, generating
 * trait values, and calculating a personality's overall value for campaign mechanics.
 */
public class PersonalityController {
    @Deprecated(since = "0.50.07", forRemoval = true)
    public final static int PERSONALITY_QUIRK_CHANCE = 10;

    /**
     * @deprecated use {@link #generatePersonality(Person)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static void generatePersonality(Person person, boolean isBigPersonality) {
        generatePersonality(person);
    }

    /**
     * Generates a personality for the given person by assigning various personality characteristics such as aggression,
     * ambition, greed, social behavior, reasoning, and optional quirks.
     *
     * <p>The method ensures that each personality characteristic is generated with a specified
     * probability (1 in 4 by default). If no characteristic is assigned, a fallback mechanism generates at least one
     * characteristic to ensure a meaningful personality.
     *
     * @param person the person for whom the personality will be generated; this person's attributes will be updated
     *               based on generated traits
     */
    public static void generatePersonality(Person person) {
        // As this method can be applied over an existing personality profile, we first reset the character's
        // personality to default.
        resetPersonality(person);

        // Then we generate a new personality
        List<PersonalityTraitType> possibleTraits = new ArrayList<>();
        possibleTraits.add(PersonalityTraitType.AGGRESSION);
        possibleTraits.add(PersonalityTraitType.AMBITION);
        possibleTraits.add(PersonalityTraitType.GREED);
        possibleTraits.add(PersonalityTraitType.SOCIAL);
        possibleTraits.add(PersonalityTraitType.PERSONALITY_QUIRK);

        PersonalityTraitType pickedTrait = ObjectUtility.getRandomItem(possibleTraits);
        switch (pickedTrait) {
            case AGGRESSION -> {
                String traitIndex = getTraitIndex(Aggression.MAJOR_TRAITS_START_INDEX);
                person.setAggression(Aggression.fromString(traitIndex));
                person.setAggressionDescriptionIndex(randomInt(Aggression.MAXIMUM_VARIATIONS));
            }
            case AMBITION -> {
                String traitIndex = getTraitIndex(Ambition.MAJOR_TRAITS_START_INDEX);
                person.setAmbition(Ambition.fromString(traitIndex));
                person.setAmbitionDescriptionIndex(randomInt(Ambition.MAXIMUM_VARIATIONS));
            }
            case GREED -> {
                String traitIndex = getTraitIndex(Greed.MAJOR_TRAITS_START_INDEX);
                person.setGreed(Greed.fromString(traitIndex));
                person.setGreedDescriptionIndex(randomInt(Greed.MAXIMUM_VARIATIONS));
            }
            case SOCIAL -> {
                String traitIndex = getTraitIndex(Social.MAJOR_TRAITS_START_INDEX);
                person.setSocial(Social.fromString(traitIndex));
                person.setSocialDescriptionIndex(randomInt(Social.MAXIMUM_VARIATIONS));
            }
            case PERSONALITY_QUIRK -> {
                int traitRoll = randomInt(PersonalityQuirk.values().length) + 1;
                String traitIndex = String.valueOf(traitRoll);
                person.setPersonalityQuirk(PersonalityQuirk.fromString(traitIndex));
                person.setPersonalityQuirkDescriptionIndex(randomInt(PersonalityQuirk.MAXIMUM_VARIATIONS));
            }
            default -> {}
        }

        // finally, write the description
        writePersonalityDescription(person);
        writeInterviewersNotes(person);
    }

    /**
     * Resets the personality attributes of the given {@link Person} to their default (neutral) values.
     *
     * <p>This sets aggression, ambition, greed, social, and personality quirk to {@code NONE}, and reasoning to
     * {@code AVERAGE}.
     *
     * @param person the {@link Person} whose personality attributes will be reset
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void resetPersonality(Person person) {
        person.setAggression(Aggression.NONE);
        person.setAmbition(Ambition.NONE);
        person.setGreed(Greed.NONE);
        person.setSocial(Social.NONE);
        person.setPersonalityQuirk(PersonalityQuirk.NONE);
    }

    /**
     * @deprecated use {@link #generatePersonality(Person, boolean)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static void generateBigPersonality(Person person) {
        // As this method is likely going to be applied over an existing personality profile, we
        // wipe the old to ensure a clean slate.
        person.setAggression(Aggression.NONE);
        person.setAmbition(Ambition.NONE);
        person.setGreed(Greed.NONE);
        person.setSocial(Social.NONE);

        // Then we generate a new personality
        List<PersonalityTraitType> possibleTraits = new ArrayList<>(Arrays.asList(PersonalityTraitType.AGGRESSION,
              PersonalityTraitType.AMBITION,
              PersonalityTraitType.GREED,
              PersonalityTraitType.SOCIAL));

        Collections.shuffle(possibleTraits);

        List<PersonalityTraitType> chosenTraits = new ArrayList<>();

        PersonalityTraitType firstTrait = possibleTraits.get(0);
        possibleTraits.remove(firstTrait);
        chosenTraits.add(firstTrait);

        PersonalityTraitType secondTrait = possibleTraits.get(0);
        possibleTraits.remove(secondTrait);
        chosenTraits.add(secondTrait);

        if (randomInt(4) == 0) {
            PersonalityTraitType thirdTrait = possibleTraits.get(0);
            possibleTraits.remove(thirdTrait);
            chosenTraits.add(thirdTrait);
        }

        if (randomInt(4) == 0) {
            PersonalityTraitType forthTrait = possibleTraits.get(0);
            chosenTraits.add(forthTrait);
        }

        for (PersonalityTraitType traitType : chosenTraits) {
            switch (traitType) {
                case AGGRESSION -> {
                    String traitIndex = getTraitIndex(Aggression.MAJOR_TRAITS_START_INDEX);
                    person.setAggression(Aggression.fromString(traitIndex));
                }
                case AMBITION -> {
                    String traitIndex = getTraitIndex(Ambition.MAJOR_TRAITS_START_INDEX);
                    person.setAmbition(Ambition.fromString(traitIndex));
                }
                case GREED -> {
                    String traitIndex = getTraitIndex(Greed.MAJOR_TRAITS_START_INDEX);
                    person.setGreed(Greed.fromString(traitIndex));
                }
                case SOCIAL -> {
                    String traitIndex = getTraitIndex(Social.MAJOR_TRAITS_START_INDEX);
                    person.setSocial(Social.fromString(traitIndex));
                }
                default -> {
                }
            }
        }

        // PERSONALITY QUIRK
        generateAndApplyPersonalityQuirk(person);

        // finally, write the description
        writePersonalityDescription(person);
        writeInterviewersNotes(person);
    }

    /**
     * Generates and applies a personality quirk to the given person. This method ensures the quirk index is valid by
     * rolling a random value between 1 and the number of quirk constants in the {@link PersonalityQuirk} enum.
     *
     * @param person the person to whom the personality quirk will be applied; their quirk attribute is updated based on
     *               the rolled quirk
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static void generateAndApplyPersonalityQuirk(Person person) {
        // This ensures we're rolling a value between 1 and the maximum index in the enum
        int traitRoll = randomInt(PersonalityQuirk.values().length) + 1;
        String traitIndex = String.valueOf(traitRoll);

        person.setPersonalityQuirk(PersonalityQuirk.fromString(traitIndex));
    }

    /**
     * Generates a trait index based on the provided starting index for major traits.
     *
     * <p>The method rolls a random integer between 1 and the provided index, and if the
     * major traits index is rolled, it further rolls a value between 0 and 5 to cover the extended major traits range.
     *
     * @param majorTraitsStartIndex the starting index for major traits in the enum
     *
     * @return a {@link String} representation of a valid trait index within the trait range
     */
    public static String getTraitIndex(final int majorTraitsStartIndex) {
        // This gives us a random number between 1 and the start of the major traits
        int traitRoll = randomInt(majorTraitsStartIndex) + 1;

        if (traitRoll == majorTraitsStartIndex) {
            // We're deliberately not using d6() here as we want 0 to be a possibility
            traitRoll += randomInt(6);
        }

        return String.valueOf(traitRoll);
    }

    /**
     * Generates and sets a descriptive personality summary for the given person based on their assigned personality
     * traits and quirks. It combines descriptions for each non-default characteristic and formats them into
     * paragraphs.
     *
     * @param person the person whose personality description will be generated and updated
     */
    public static void writePersonalityDescription(Person person) {
        Gender gender = person.getGender();
        String givenName = person.getGivenName();

        List<String> traitDescriptions = getTraitDescriptions(gender,
              givenName,
              person.getAggression(),
              person.getAggressionDescriptionIndex(),
              person.getAmbition(),
              person.getAmbitionDescriptionIndex(),
              person.getGreed(),
              person.getGreedDescriptionIndex(),
              person.getSocial(),
              person.getSocialDescriptionIndex());

        // PERSONALITY QUIRK
        PersonalityQuirk personalityQuirk = person.getPersonalityQuirk();
        if (!personalityQuirk.isNone()) {
            String quirkDescription = personalityQuirk.getDescription(person.getPrimaryRole(),
                  person.getPersonalityQuirkDescriptionIndex(),
                  gender,
                  person.getOriginFaction(),
                  givenName);
            traitDescriptions.add(quirkDescription);
        }

        // Build the description proper
        StringBuilder personalityDescription = new StringBuilder();

        for (String description : traitDescriptions) {
            personalityDescription.append(description);
            personalityDescription.append(' ');
        }

        person.setPersonalityDescription(personalityDescription.toString());
    }

    public static void writeInterviewersNotes(Person person) {
        List<String> notes = getTraitInterviewerNotes(person.getAggression(),
              person.getAggressionDescriptionIndex(),
              person.getAmbition(),
              person.getAmbitionDescriptionIndex(),
              person.getGreed(),
              person.getGreedDescriptionIndex(),
              person.getSocial(),
              person.getSocialDescriptionIndex());

        // Reasoning and personality quirk are handled differently to general personality traits.
        // Build the description proper
        StringBuilder interviewersNotes = new StringBuilder("<html>");

        for (String note : notes) {
            interviewersNotes.append("<br>- ").append(note);
        }

        interviewersNotes.append("</html>");

        person.setPersonalityInterviewNotes(interviewersNotes.toString());
    }

    /**
     * Retrieves the descriptions of all personality traits (other than Reasoning and Quirks) for the given person. This
     * method processes various personality traits such as aggression, ambition, greed, and social behavior, generating
     * descriptions based on the specified indices, gender, and given name of the person.
     *
     * <p>Descriptions for traits that are not assigned or are empty will be excluded from the
     * returned list. This ensures only meaningful and applicable descriptions are included.
     *
     * @param gender                     the gender of the person, used for generating gender-specific pronouns in trait
     *                                   descriptions
     * @param givenName                  the given name of the person, used to personalize the descriptions
     * @param aggression                 the {@link Aggression} trait assigned to the person; omitted if the trait is
     *                                   set to "none"
     * @param aggressionDescriptionIndex the index used to determine the specific {@link Aggression} description
     * @param ambition                   the {@link Ambition} trait assigned to the person; omitted if the trait is set
     *                                   to "none"
     * @param ambitionDescriptionIndex   the index used to determine the specific {@link Ambition} description
     * @param greed                      the {@link Greed} trait assigned to the person; omitted if the trait is set to
     *                                   "none"
     * @param greedDescriptionIndex      the index used to determine the specific {@link Greed} description
     * @param social                     the {@link Social} behavior trait assigned to the person; omitted if the trait
     *                                   is set to "none"
     * @param socialDescriptionIndex     the index used to determine the specific {@link Social} description
     *
     * @return a list of strings, where each string represents a detailed description of a personality trait assigned to
     *       the given person; traits without meaningful descriptions are excluded
     */
    private static List<String> getTraitDescriptions(Gender gender, String givenName, Aggression aggression,
          int aggressionDescriptionIndex, Ambition ambition, int ambitionDescriptionIndex, Greed greed,
          int greedDescriptionIndex, Social social, int socialDescriptionIndex) {
        List<String> traitDescriptions = new ArrayList<>();

        // AGGRESSION
        if (!aggression.isNone()) {
            String traitDescription = aggression.getDescription(aggressionDescriptionIndex, gender, givenName);

            if (!traitDescription.isBlank()) {
                traitDescriptions.add(traitDescription);
            }
        }

        // AMBITION
        if (!ambition.isNone()) {
            String traitDescription = ambition.getDescription(ambitionDescriptionIndex, gender, givenName);

            if (!traitDescription.isBlank()) {
                traitDescriptions.add(traitDescription);
            }
        }

        // GREED
        if (!greed.isNone()) {
            String traitDescription = greed.getDescription(greedDescriptionIndex, gender, givenName);

            if (!traitDescription.isBlank()) {
                traitDescriptions.add(traitDescription);
            }
        }

        // SOCIAL
        if (!social.isNone()) {
            String traitDescription = social.getDescription(socialDescriptionIndex, gender, givenName);

            if (!traitDescription.isBlank()) {
                traitDescriptions.add(traitDescription);
            }
        }

        return traitDescriptions;
    }

    private static List<String> getTraitInterviewerNotes(Aggression aggression, int aggressionDescriptionIndex,
          Ambition ambition, int ambitionDescriptionIndex, Greed greed, int greedDescriptionIndex, Social social,
          int socialDescriptionIndex) {
        List<String> interviewersNotes = new ArrayList<>();

        // AGGRESSION
        if (!aggression.isNone()) {
            String traitDescription = aggression.getInterviewersNotes(aggressionDescriptionIndex);

            if (!traitDescription.isBlank()) {
                interviewersNotes.add(traitDescription);
            }
        }

        // AMBITION
        if (!ambition.isNone()) {
            String traitDescription = ambition.getInterviewersNotes(ambitionDescriptionIndex);

            if (!traitDescription.isBlank()) {
                interviewersNotes.add(traitDescription);
            }
        }

        // GREED
        if (!greed.isNone()) {
            String traitDescription = greed.getInterviewersNotes(greedDescriptionIndex);

            if (!traitDescription.isBlank()) {
                interviewersNotes.add(traitDescription);
            }
        }

        // SOCIAL
        if (!social.isNone()) {
            String traitDescription = social.getInterviewersNotes(socialDescriptionIndex);

            if (!traitDescription.isBlank()) {
                interviewersNotes.add(traitDescription);
            }
        }

        return interviewersNotes;
    }


    /**
     * Generates an {@link Reasoning} enum value for a person based on a randomly rolled value. Each reasoning level is
     * mapped to a specific range of values, with lower rolls producing less intelligent results and higher rolls
     * producing more intelligent results.
     *
     * @param roll the random roll value used to determine the {@link Reasoning} enum value
     *
     * @return the {@link Reasoning} enum value corresponding to the rolled range
     *
     * @throws IllegalStateException if the roll exceeds the expected value range
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static Reasoning generateReasoning(int roll) {
        if (roll < 1) {
            return BRAIN_DEAD;
        } else if (roll < 2) {
            return UNINTELLIGENT;
        } else if (roll < 4) {
            return FOOLISH;
        } else if (roll < 8) {
            return SIMPLE;
        } else if (roll < 16) {
            return SLOW;
        } else if (roll < 29) {
            return UNINSPIRED;
        } else if (roll < 52) {
            return DULL;
        } else if (roll < 92) {
            return DIMWITTED;
        } else if (roll < 162) {
            return OBTUSE;
        } else if (roll < 285) {
            return BELOW_AVERAGE;
        } else if (roll < 501) {
            return UNDER_PERFORMING;
        } else if (roll < 878) {
            return LIMITED_INSIGHT;
        } else if (roll < 7028) {
            return AVERAGE;
        } else if (roll < 7594) {
            return ABOVE_AVERAGE;
        } else if (roll < 7917) {
            return STUDIOUS;
        } else if (roll < 8102) {
            return DISCERNING;
        } else if (roll < 8208) {
            return SHARP;
        } else if (roll < 8268) {
            return QUICK_WITTED;
        } else if (roll < 8302) {
            return PERCEPTIVE;
        } else if (roll < 8322) {
            return BRIGHT;
        } else if (roll < 8333) {
            return CLEVER;
        } else if (roll < 8339) {
            return INTELLECTUAL;
        } else if (roll < 8343) {
            return BRILLIANT;
        } else if (roll < 8345) {
            return EXCEPTIONAL;
        } else if (roll < 8346) {
            return GENIUS;
        } else {
            throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/randomEvents/PersonalityController.java/generateReasoning: " +
                        roll);
        }
    }

    /**
     * Calculates the total personality value for a person based on their assigned personality traits (aggression,
     * ambition, greed, and social behavior) and whether personalities are enabled in the campaign options.
     *
     * <p>The calculation assigns positive or negative values to each trait depending on whether it
     * is considered positive or negative, with major traits contributing more heavily to the total value. If
     * personality mechanics are disabled, the method will return 0.</p>
     *
     * @param isUseRandomPersonalities a flag indicating whether random personalities are enabled in the campaign
     *                                 options; if false, the method will return 0
     * @param aggression               the {@link Aggression} trait assigned to the person
     * @param ambition                 the {@link Ambition} trait assigned to the person
     * @param greed                    the {@link Greed} trait assigned to the person
     * @param social                   the {@link Social} trait assigned to the person
     *
     * @return the total personality value, which is the sum of values contributed by each trait, or 0 if personalities
     *       are not enabled
     */
    public static int getPersonalityValue(final boolean isUseRandomPersonalities, final Aggression aggression,
          final Ambition ambition, final Greed greed, final Social social) {
        if (!isUseRandomPersonalities) {
            return 0;
        }

        int personalityValue = 0;
        int modifier;

        if (!aggression.isNone()) {
            modifier = aggression.isTraitPositive() ? 1 : -1;
            personalityValue += aggression.isTraitMajor() ? modifier * 2 : modifier;
        }

        if (!ambition.isNone()) {
            modifier = ambition.isTraitPositive() ? 1 : -1;
            personalityValue += ambition.isTraitMajor() ? modifier * 2 : modifier;
        }

        if (!greed.isNone()) {
            modifier = greed.isTraitPositive() ? 1 : -1;
            personalityValue += greed.isTraitMajor() ? modifier * 2 : modifier;
        }

        if (!social.isNone()) {
            modifier = social.isTraitPositive() ? 1 : -1;
            personalityValue += social.isTraitMajor() ? modifier * 2 : modifier;
        }

        return personalityValue;
    }
}
