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

import static java.lang.Math.max;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HE_SHE_THEY;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIM_HER_THEM;
import static mekhq.campaign.personnel.enums.GenderDescriptors.HIS_HER_THEIR;
import static mekhq.campaign.randomEvents.personalities.enums.Reasoning.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    public final static int PERSONALITY_QUIRK_CHANCE = 10;

    /**
     * @deprecated use {@link #generatePersonality(Person, boolean)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static void generatePersonality(Person person) {
        generatePersonality(person, false);
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
    public static void generatePersonality(Person person, boolean isBigPersonality) {
        // As this method can be applied over an existing personality profile, we first reset the character's
        // personality to default.
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

        if (isBigPersonality || (randomInt(4) == 0)) {
            PersonalityTraitType secondTrait = possibleTraits.get(0);
            possibleTraits.remove(secondTrait);
            chosenTraits.add(secondTrait);
        }

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

        // REASONING
        int firstReasoning = randomInt(8346);
        int secondReasoning = isBigPersonality ? randomInt(8346) : 0;
        person.setReasoning(generateReasoning(max(firstReasoning, secondReasoning)));

        // finally, write the description
        writePersonalityDescription(person);
        writeInterviewersNotes(person);
    }

    /**
     * @deprecated use {@link #generatePersonality(Person, boolean)} instead
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public static void generateBigPersonality(Person person) {
        // As this method is likely going to be be applied over an existing personality profile, we
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

        // REASONING
        int firstReasoning = randomInt(8346);
        int secondReasoning = randomInt(8346);
        person.setReasoning(generateReasoning(max(firstReasoning, secondReasoning)));

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
    private static String getTraitIndex(final int majorTraitsStartIndex) {
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

        // Reasoning and personality quirk are handled differently to general personality traits.
        // REASONING
        Reasoning reasoning = person.getReasoning();
        String reasoningDescription = "";
        if (!reasoning.isAverageType()) {
            reasoningDescription = reasoning.getDescription(person.getReasoningDescriptionIndex(), gender, givenName);
        }

        // PERSONALITY QUIRK
        PersonalityQuirk personalityQuirk = person.getPersonalityQuirk();
        String quirkDescription = "";
        if (!personalityQuirk.isNone()) {
            quirkDescription = personalityQuirk.getDescription(person.getPrimaryRole(),
                  person.getPersonalityQuirkDescriptionIndex(),
                  gender,
                  person.getOriginFaction(),
                  givenName);
        }

        // Build the description proper
        StringBuilder personalityDescription = new StringBuilder();

        // Append the first trait description, if exists, without wrapping in <p>
        // We do this so that we don't end up with weird spacing
        if (!traitDescriptions.isEmpty()) {
            personalityDescription.append(traitDescriptions.get(0));
            personalityDescription.append(' ');
        }

        for (int i = 1; i < traitDescriptions.size(); i++) {
            if (i % 2 == 0) {
                personalityDescription.append("<p>");
            }

            personalityDescription.append(traitDescriptions.get(i));

            if (i % 2 == 1 || i == traitDescriptions.size() - 1) {
                personalityDescription.append("</p>");
            } else {
                personalityDescription.append(' ');
            }
        }

        if (!reasoningDescription.isBlank()) {
            if (!personalityDescription.toString().isBlank()) {
                personalityDescription.append("<p>").append(reasoningDescription).append("</p>");
            } else {
                personalityDescription.append(reasoningDescription);
            }
        }

        if (!quirkDescription.isBlank()) {
            if (!personalityDescription.toString().isBlank()) {
                personalityDescription.append("<p>").append(quirkDescription).append("</p>");
            } else {
                personalityDescription.append(quirkDescription);
            }
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
        // REASONING
        Reasoning reasoning = person.getReasoning();
        String examResults = reasoning.getExamResults();

        // Build the description proper
        StringBuilder interviewersNotes = new StringBuilder("<html>");

        interviewersNotes.append(examResults);

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
     * Generates an {@link Reasoning} enum value for a person based on a randomly rolled value. Each reasoning
     * level is mapped to a specific range of values, with lower rolls producing less intelligent results and higher
     * rolls producing more intelligent results.
     *
     * @param roll the random roll value used to determine the {@link Reasoning} enum value
     *
     * @return the {@link Reasoning} enum value corresponding to the rolled range
     *
     * @throws IllegalStateException if the roll exceeds the expected value range
     */ private static Reasoning generateReasoning(int roll) {
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

    /**
     * Represents a set of grammatical pronouns associated with a subject, object, and possessive form, along with their
     * lowercased variations, and additional data for pluralization handling.
     *
     * <p>This record is intended to encapsulate information related to personal pronouns for use in
     * linguistic or grammatical processing. It includes pronoun forms commonly required for sentence construction and
     * supports pluralization when applicable.</p>
     *
     * <b>Fields</b>
     * <ul>
     *   <li><b>subjectPronoun</b>: The pronoun used as the subject of a sentence (e.g., "He", "She", "They").</li>
     *   <li><b>subjectPronounLowerCase</b>: The lowercased version of the subject pronoun (e.g., "he", "she", "they").</li>
     *   <li><b>objectPronoun</b>: The pronoun used as the object of a sentence (e.g., "Him", "Her", "Them").</li>
     *   <li><b>objectPronounLowerCase</b>: The lowercased version of the object pronoun (e.g., "him", "her", "them").</li>
     *   <li><b>possessivePronoun</b>: The pronoun used to indicate possession (e.g., "His", "Hers", "Theirs").</li>
     *   <li><b>possessivePronounLowerCase</b>: The lowercased version of the possessive pronoun (e.g., "his", "hers", "theirs").</li>
     *   <li><b>pluralizer</b>: An integer value used to determine pluralization behavior. The exact logic depends
     *       on the context in which the pronoun data is used (e.g., 1 for singular, >1 for plural).</li>
     * </ul>
     *
     * @param subjectPronoun             The subject pronoun (e.g., "He", "She", "They").
     * @param subjectPronounLowerCase    The lowercased version of the subject pronoun.
     * @param objectPronoun              The object pronoun (e.g., "Him", "Her", "Them").
     * @param objectPronounLowerCase     The lowercased version of the object pronoun.
     * @param possessivePronoun          The possessive pronoun (e.g., "His", "Hers", "Theirs").
     * @param possessivePronounLowerCase The lowercased version of the possessive pronoun.
     * @param pluralizer                 An integer value to represent singular (1) or plural (>1).
     */
    public record PronounData(String subjectPronoun, String subjectPronounLowerCase, String objectPronoun,
          String objectPronounLowerCase, String possessivePronoun, String possessivePronounLowerCase, int pluralizer) {
        /**
         * Constructs a new {@code PronounData} record based on the specified gender.
         *
         * @param gender The gender used to determine the pronouns and pluralizer.
         */
        public PronounData(Gender gender) {
            this(HE_SHE_THEY.getDescriptorCapitalized(gender),
                  HE_SHE_THEY.getDescriptorCapitalized(gender).toLowerCase(),
                  HIM_HER_THEM.getDescriptorCapitalized(gender),
                  HIM_HER_THEM.getDescriptorCapitalized(gender).toLowerCase(),
                  HIS_HER_THEIR.getDescriptorCapitalized(gender),
                  HIS_HER_THEIR.getDescriptorCapitalized(gender).toLowerCase(),
                  gender.isGenderNeutral() ? 0 : 1
                  // Used to determine whether to use a plural case
            );
        }
    }
}
