/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.backgrounds;

import static mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator.*;

import java.util.ResourceBundle;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;

public class BackgroundsController {
    static final ResourceBundle resources = ResourceBundle
                                                  .getBundle("mekhq.resources.RandomMercenaryCompanyNameGenerator");

    public static void generateBackground(Campaign campaign, Person person) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.get(CampaignOption.USE_TOUGHNESS) && campaignOptions.get(CampaignOption.USE_RANDOM_TOUGHNESS)) {
            Toughness.generateToughness(person);
        }
    }

    /**
     * Generates a random mercenary company name.
     *
     * @return A string containing the generated name.
     *
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    public static String randomMercenaryCompanyNameGenerator(@Nullable Person commander) {
        try { // this allows us to use getCampaign() in tests without needing to also mock RandomCallsignGenerator
            String prefix = getPrefix(commander);
            return getNameBody(prefix + ' ');
        } catch (NullPointerException e) {
            return resources.getString("fallbackValue");
        }
    }

    /**
     * Generates a random pirate company name.
     *
     * @return A string containing the generated name.
     *
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    public static String randomPirateCompanyNameGenerator() {
        return getWeightedPreFab().randomItem();
    }

    /**
     * Generates a random corporate company name.
     *
     * @return A string containing the generated name.
     *
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    public static String randomCorporationCompanyNameGenerator() {
        try { // this allows us to use getCampaign() in tests without needing to also mock RandomCallsignGenerator
            return getCorporateNameBody();
        } catch (NullPointerException e) {
            return resources.getString("fallbackValue");
        }
    }

    /**
     * Returns the body of the generated mercenary name.
     *
     * @return the name body as a String.
     *
     * @throws IllegalStateException if an unexpected value is encountered in the switch statement.
     */
    private static String getNameBody(String name) {
        int roll = Compute.randomInt(3);

        return switch (roll) {
            // Mercenary
            case 0 -> name + getNewWord(name, getWeightedEndWordMercenary());
            case 1 -> {
                name += getNewWord(name, getWeightedMiddleWordMercenary()) + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordMercenary());

                yield name + newWordSuggestion;
            }
            // Pre-Fab
            case 2 -> getWeightedPreFab().randomItem();
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/getNameBody: "
                        + roll
            );
        };
    }

    /**
     * Returns the body of the generated corporate name.
     *
     * @return the name body as a String.
     *
     * @throws IllegalStateException if an unexpected value is encountered in the switch statement.
     */
    private static String getCorporateNameBody() {
        String name = getWeightedMiddleWordCorporate().randomItem() + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordCorporate());

        return name + newWordSuggestion;
    }

    /**
     * Generates a random name for a civilian coalition; that is, a group of business concerns banding together to hire
     * the player.
     *
     * @return A string containing the generated name.
     *
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    public static String randomCivilianCompanyNameGenerator() {
        try { // this allows us to use getCampaign() in tests without needing to also mock RandomCallsignGenerator
            return getCivilianNameBody();
        } catch (NullPointerException e) {
            return resources.getString("fallbackValue");
        }
    }

    /**
     * Returns the body of the generated civilian coalition name.
     *
     * @return the name body as a String.
     *
     * @throws IllegalStateException if an unexpected value is encountered in the switch statement.
     */
    private static String getCivilianNameBody() {
        int roll = Compute.randomInt(2);

        return switch (roll) {
            // Descriptor + Collective
            case 0 -> {
                String name = getWeightedMiddleWordCivilian().randomItem() + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordCivilian());

                yield name + newWordSuggestion;
            }
            // Descriptor + Descriptor + Collective
            case 1 -> {
                String name = getWeightedMiddleWordCivilian().randomItem() + ' ';
                name += getNewWord(name, getWeightedMiddleWordCivilian()) + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordCivilian());

                yield name + newWordSuggestion;
            }
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/getCivilianNameBody: "
                        + roll
            );
        };
    }

    /**
     * Generates a random rebel force name.
     *
     * @param commander The person object representing the commander. Can be null.
     *
     * @return A string containing the generated name.
     *
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    public static String randomRebelCompanyNameGenerator(@Nullable Person commander) {
        try { // this allows us to use getCampaign() in tests without needing to also mock RandomCallsignGenerator
            String prefix = getPrefix(commander);
            return getRebelNameBody(prefix + ' ');
        } catch (NullPointerException e) {
            return resources.getString("fallbackValue");
        }
    }

    /**
     * Returns the body of the generated rebel name.
     *
     * @return the name body as a String.
     *
     * @throws IllegalStateException if an unexpected value is encountered in the switch statement.
     */
    private static String getRebelNameBody(String name) {
        int roll = Compute.randomInt(2);

        return switch (roll) {
            // Single word
            case 0 -> name + getNewWord(name, getWeightedEndWordRebel());
            // Middle + End
            case 1 -> {
                name += getNewWord(name, getWeightedMiddleWordRebel()) + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordRebel());

                yield name + newWordSuggestion;
            }
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/getRebelNameBody: "
                        + roll
            );
        };
    }

    /**
     * Generates a random militia force name.
     *
     * @param commander The person object representing the commander. Can be null.
     *
     * @return A string containing the generated name.
     *
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    public static String randomMilitiaCompanyNameGenerator(@Nullable Person commander) {
        try { // this allows us to use getCampaign() in tests without needing to also mock RandomCallsignGenerator
            String prefix = getPrefix(commander);
            return getMilitiaNameBody(prefix + ' ');
        } catch (NullPointerException e) {
            return resources.getString("fallbackValue");
        }
    }

    /**
     * Returns the body of the generated militia name.
     *
     * @return the name body as a String.
     *
     * @throws IllegalStateException if an unexpected value is encountered in the switch statement.
     */
    private static String getMilitiaNameBody(String name) {
        int roll = Compute.randomInt(2);

        return switch (roll) {
            // Single word
            case 0 -> name + getNewWord(name, getWeightedEndWordMilitia());
            // Middle + End
            case 1 -> {
                name += getNewWord(name, getWeightedMiddleWordMilitia()) + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordMilitia());

                yield name + newWordSuggestion;
            }
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/getMilitiaNameBody: "
                        + roll
            );
        };
    }

    /**
     * Retrieves the prefix for generating a random mercenary company name.
     *
     * @param commander The person object representing the commander. Can be null.
     *
     * @return The prefix for generating a random mercenary company name.
     *
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    private static String getPrefix(Person commander) {
        int roll = Compute.randomInt(4);

        return switch (roll) {
            // Numerical
            case 0 -> resources.getString("definiteArticle.text") + ' ' + getNumericalNameStart();
            // Vanity
            case 1 -> getCommanderName(commander) + "'s";
            // 'The'
            case 2, 3 -> resources.getString("definiteArticle.text");
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/getPrefix: "
                        + roll);
        };
    }

    /**
     * Retrieves the name of the commander.
     *
     * @param commander The person object representing the commander. Can be null.
     *
     * @return The name of the commander. If the commander is null, a random callsign from a weighted list will be
     *       returned.
     */
    private static String getCommanderName(@Nullable Person commander) {
        if (commander == null) {
            return RandomCallsignGenerator.getInstance().generate();
        } else {
            String name = commander.getCallsign().isBlank() ? commander.getSurname() : commander.getCallsign();
            return name.isBlank() ? commander.getFirstName() : name;
        }
    }

    /**
     * Returns a random word from the given `wordMap` that is unique to the currently generated name.
     *
     * @param name    the name string to check against the generated word
     * @param wordMap the weighted map containing available words to choose from
     *
     * @return a new word that is unique within 'name'
     */
    private static String getNewWord(String name, WeightedIntMap<String> wordMap) {
        String newWord;

        do {
            newWord = wordMap.randomItem();
        } while (checkIfNameContains(name, newWord));

        return newWord;
    }

    /**
     * Checks if the start of the suggested addition is present in the current name.
     *
     * @param currentName       the current name to check against
     * @param suggestedAddition the suggested addition to the name
     *
     * @return true if the start of the suggested addition is not present in the current name, otherwise false
     */
    private static boolean checkIfNameContains(String currentName, String suggestedAddition) {
        int checkLength = suggestedAddition.length() - 2;

        String startOfSecondString = suggestedAddition.substring(0, checkLength);

        return currentName.contains(startOfSecondString);
    }

    /**
     * Generates a numerical name using a random number and a suffix based on the number's modulo.
     */
    private static String getNumericalNameStart() {
        int number = Compute.randomInt(30) + 1;

        int modulo100 = number % 100;
        int modulo10 = number % 10;

        if (modulo100 >= 11 && modulo100 <= 13) {
            return number + resources.getString("suffixTh.text");
        } else if (modulo10 == 1) {
            return number + resources.getString("suffixSt.text");
        } else if (modulo10 == 2) {
            return number + resources.getString("suffixNd.text");
        } else if (modulo10 == 3) {
            return number + resources.getString("suffixRd.text");
        } else {
            return number + resources.getString("suffixTh.text");
        }
    }
}
