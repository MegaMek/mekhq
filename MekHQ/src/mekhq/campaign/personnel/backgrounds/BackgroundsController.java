package mekhq.campaign.personnel.backgrounds;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

import static megamek.client.generator.RandomCallsignGenerator.getWeightedCallsigns;
import static mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator.*;

public class BackgroundsController {
    final static ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RandomMercenaryCompanyNameGenerator");

    public static void generateBackground(Campaign campaign, Person person) {
        if (campaign.getCampaignOptions().isUseToughness()) {
            Toughness.generateToughness(person);
        }
    }

    /**
     * Generates a random mercenary company name.
     *
     * @return A string containing the generated name.
     * @throws IllegalStateException if an unexpected value is encountered during the generation process.
     */
    public static String randomMercenaryCompanyNameGenerator(@Nullable Person commander) {
        try { // this allows us to use getCampaign() in tests without needing to also mock RandomCallsignGenerator
            String prefix = getPrefix(commander);
            return getNameBody(prefix + ' ');
        } catch (NullPointerException e) {
            return "";
        }
    }

    /**
     * Returns the body of the generated name.
     *
     * @return the name body as a String.
     * @throws IllegalStateException if an unexpected value is encountered in the switch statement.
     */
    private static String getNameBody(String name) {
        int roll = Compute.randomInt(4);

        return switch (roll) {
            // Corporate
            case 0 -> {
                name += getNewWord(name, getWeightedMiddleWordCorporate()) + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordCorporate());

                yield name + newWordSuggestion;
            }
            // Mercenary
            case 1 -> name + getNewWord(name, getWeightedEndWordMercenary());
            case 2 -> {
                name += getNewWord(name, getWeightedMiddleWordMercenary()) + ' ';
                String newWordSuggestion = getNewWord(name, getWeightedEndWordMercenary());

                yield name + newWordSuggestion;
            }
            // Pre-Fab
            case 3 -> name + getWeightedPreFab().randomItem();
            default -> throw new IllegalStateException(
                    "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/getNameBody: "
                    + roll
            );
        };
    }

    /**
     * Retrieves the prefix for generating a random mercenary company name.
     *
     * @param commander The person object representing the commander. Can be null.
     * @return The prefix for generating a random mercenary company name.
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
     * @return The name of the commander. If the commander is null, a random callsign from a weighted list will be returned.
     */
    private static String getCommanderName(@Nullable Person commander) {
        if (commander == null) {
            return getWeightedCallsigns().randomItem();
        } else {
            String name = commander.getCallsign().isBlank() ? commander.getSurname() : commander.getCallsign();
            return name.isBlank() ? commander.getFirstName() : name;
        }
    }

    /**
     * Returns a random word from the given `wordMap` that is unique to the currently generated name.
     *
     * @param name the name string to check against the generated word
     * @param wordMap the weighted map containing available words to choose from
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
     * @param currentName         the current name to check against
     * @param suggestedAddition   the suggested addition to the name
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
