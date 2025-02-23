/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.randomEvents.personalities;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.personalities.enums.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static mekhq.campaign.randomEvents.personalities.enums.Intelligence.*;

public class PersonalityController {
    public static int PERSONALITY_QUIRK_CHANCE = 10;

    /**
     * Generates a personality for the given person. The method assigns various personality traits,
     * intelligence, and potential quirks to the person.
     *
     * @param person   the person for whom the personality will be generated and updated
     */
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
            if (d6() == 1) {
                setPersonalityTrait(person, table, randomInt(26));
            }
        }

        // we only want 1 in 10 persons to have a quirk,
        // as these helps reduce repetitiveness and keeps them unique
        if (randomInt(PERSONALITY_QUIRK_CHANCE) == 0) {
            person.setPersonalityQuirk(generatePersonalityQuirk());
        }

        person.setIntelligence(generateIntelligence(randomInt(8346)));

        // finally, write the description
        writeDescription(person);

        // check at least one characteristic has been generated, if not, then repeat the
        // process
        // while this might create a couple of loops,
        // probability says we can only expect 1 additional loop, 2 in exceptional
        // circumstances
        if (Objects.equals(person.getPersonalityDescription(), "")) {
            generatePersonality(person);
        }
    }

    /**
     * Sets the personality traits of a person based on the given table roll and
     * trait roll.
     *
     * @param person    the person whose personality traits will be set
     * @param tableRoll the table roll used to determine which personality trait to
     *                  set
     * @param traitRoll the roll used to generate the value of the personality trait
     * @throws IllegalStateException if an unexpected value is rolled for tableRoll
     *                               parameter
     */
    private static void setPersonalityTrait(Person person, int tableRoll, int traitRoll) {
        // We want major traits to have a low chance of occurring.
        // This ensures each trait only has a 1 in 25 chance of spawning
        if (traitRoll == 25) {
            traitRoll += randomInt(6);
        }

        String rollString = String.valueOf(traitRoll);

        switch (tableRoll) {
            case 0 -> person.setAggression(Aggression.fromString(rollString));
            case 1 -> person.setAmbition(Ambition.fromString(rollString));
            case 2 -> person.setGreed(Greed.fromString(rollString));
            case 3 -> person.setSocial(Social.fromString(rollString));
            default -> throw new IllegalStateException(
                    "Unexpected value in mekhq/campaign/personnel/randomEvents/personality/PersonalityController.java/setPersonalityTrait: "
                            + tableRoll);
        }
    }

    /**
     * Sets the personality description of a person based on their personality
     * traits.
     *
     * @param person the person whose personality description will be set
     */
    public static void writeDescription(Person person) {
        List<String> traitDescriptions = getTraitDescriptions(person);

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

        person.setPersonalityDescription(personalityDescription.toString());
    }

    /**
     * Retrieves descriptions for various personality traits of the given person. Each non-default
     * personality trait is processed to generate a corresponding description, which is then added
     * to the resulting list.
     *
     * @param person   the person whose personality trait descriptions are to be retrieved
     * @return a list of strings containing descriptions of the person's personality traits
     */
    private static List<String> getTraitDescriptions(Person person) {
        List<String> traitDescriptions = new ArrayList<>();

        if (!person.getAggression().isNone()) {
            traitDescriptions.add(person.getAggression().getDescription(person));
        }

        if (!person.getAmbition().isNone()) {
            traitDescriptions.add(person.getAmbition().getDescription(person));
        }

        if (!person.getGreed().isNone()) {
            traitDescriptions.add(person.getGreed().getDescription(person));
        }

        if (!person.getSocial().isNone()) {
            traitDescriptions.add(person.getSocial().getDescription(person));
        }

        if (!person.getIntelligence().isAverageType()) {
            traitDescriptions.add(person.getIntelligence().getDescription(person));
        }

        if (!person.getPersonalityQuirk().isNone()) {
            traitDescriptions.add(person.getPersonalityQuirk().getDescription(person));
        }

        return traitDescriptions;
    }

    /**
     * @return a random personality quirk for a person.
     */
    public static PersonalityQuirk generatePersonalityQuirk() {
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
                    "Unexpected value in mekhq/campaign/personnel/randomEvents/PersonalityController.java/generateIntelligence: "
                            + roll);
        }
    }

    /**
     * Calculates the total value of a person's personality characteristics.
     *
     * @param campaign the current campaign
     * @param person   the person to calculate the personality value for
     * @return the total personality value of the person in the campaign
     */
    public static int getPersonalityValue(Campaign campaign, Person person) {
        if (person == null) {
            return 0;
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        if (campaignOptions.isUseRandomPersonalities() && campaignOptions.isUseRandomPersonalityReputation()) {
            int personalityValue = 0;
            int modifier;

            Aggression aggression = person.getAggression();
            if (!person.getAggression().isNone()) {
                modifier = aggression.isTraitPositive() ? 1 : -1;
                personalityValue += aggression.isTraitMajor() ? modifier * 2 : modifier;
            }

            Ambition ambition = person.getAmbition();
            if (!person.getAmbition().isNone()) {
                modifier = ambition.isTraitPositive() ? 1 : -1;
                personalityValue += ambition.isTraitMajor() ? modifier * 2 : modifier;
            }

            Greed greed = person.getGreed();
            if (!person.getGreed().isNone()) {
                modifier = greed.isTraitPositive() ? 1 : -1;
                personalityValue += greed.isTraitMajor() ? modifier * 2 : modifier;
            }

            Social social = person.getSocial();
            if (!person.getSocial().isNone()) {
                modifier = social.isTraitPositive() ? 1 : -1;
                personalityValue += social.isTraitMajor() ? modifier * 2 : modifier;
            }

            return personalityValue;
        } else {
            return 0;
        }
    }
}
