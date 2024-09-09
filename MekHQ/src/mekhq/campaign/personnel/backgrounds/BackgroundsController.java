package mekhq.campaign.personnel.backgrounds;

import java.util.ResourceBundle;
import java.util.function.Supplier;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.EndWordCorporate;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.EndWordMercenary;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.MiddleWordCorporate;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.MiddleWordMercenary;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.PreFabHumorous;

public class BackgroundsController {
    static final ResourceBundle resources = ResourceBundle
            .getBundle("mekhq.resources.RandomMercenaryCompanyNameGenerator");

    public static void generateBackground(Campaign campaign, Person person) {
        if (campaign.getCampaignOptions().isUseToughness()) {
            Toughness.generateToughness(person);
        }
    }

    /**
     * Generates a random mercenary company name.
     *
     * @return A string containing the generated name.
     * @throws IllegalStateException if an unexpected value is encountered during
     *                               the generation process.
     */
    public static String randomMercenaryCompanyNameGenerator(@Nullable Person commander) {
        return getPrefix(commander) + ' ' + getNameBody();
    }

    /**
     * Returns the body of the generated name.
     *
     * @return the name body as a String.
     * @throws IllegalStateException if an unexpected value is encountered in the
     *                               switch statement.
     */
    private static String getNameBody() {
        String name = "";
        int roll = Compute.randomInt(4);

        return switch (roll) {
            // Corporate
            case 0 -> {
                name = MiddleWordCorporate.getRandomWord();
                String newWordSuggestion = getNewWord(name, EndWordCorporate::getRandomWord);

                yield name + ' ' + newWordSuggestion;
            }
            // Mercenary
            case 1 -> getNewWord(name, EndWordMercenary::getRandomWord);
            case 2 -> {
                name = getNewWord(name, MiddleWordMercenary::getRandomWord) + ' ';
                String newWordSuggestion = getNewWord(name, EndWordMercenary::getRandomWord);

                yield name + newWordSuggestion;
            }
            // Pre-Fab
            case 3 -> PreFabHumorous.getRandomWord();
            default -> throw new IllegalStateException(
                    "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/randomMercenaryCompanyNameGenerator 1 of 2: "
                            + roll);
        };
    }

    /**
     * Retrieves the prefix for generating a random mercenary company name.
     *
     * @param commander The person object representing the commander. Can be null.
     * @return The prefix for generating a random mercenary company name.
     * @throws IllegalStateException if an unexpected value is encountered during
     *                               the generation process.
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
                    "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/randomMercenaryCompanyNameGenerator 2 of 2: "
                            + roll);
        };
    }

    /**
     * Retrieves the name of the commander.
     *
     * @param commander The person object representing the commander. Can be null.
     * @return The name of the commander. If the commander is null, a random
     *         callsign from a weighted list will be returned.
     */
    private static String getCommanderName(@Nullable Person commander) {
        try { // this allows us to use getCampaign() in tests without needing to also mock
              // RandomCallsignGenerator
            if (commander == null) {
                return RandomCallsignGenerator.getWeightedCallsigns().randomItem();
            } else {
                String name = commander.getCallsign().isBlank() ? commander.getSurname() : commander.getCallsign();
                return name.isBlank() ? commander.getFirstName() : name;
            }
        } catch (NullPointerException ignored) {
        }

        return "";
    }

    /**
     * Retrieves a new word based on the given name and end word supplier.
     *
     * @param name            The name to check if it contains the generated word.
     * @param endWordSupplier The supplier to provide a new word.
     * @return The new word that unique to the given name.
     */
    private static String getNewWord(String name, Supplier<String> endWordSupplier) {
        String newWord;

        do {
            newWord = endWordSupplier.get();
        } while (checkIfNameContains(name, newWord));

        return newWord;
    }

    /**
     * Checks if the start of the suggested addition is present in the current name.
     *
     * @param currentName       the current name to check against
     * @param suggestedAddition the suggested addition to the name
     * @return true if the start of the suggested addition is not present in the
     *         current name, otherwise false
     */
    private static boolean checkIfNameContains(String currentName, String suggestedAddition) {
        int checkLength = suggestedAddition.length() - 2;

        String startOfSecondString = suggestedAddition.substring(0, checkLength);

        return currentName.contains(startOfSecondString);
    }

    /**
     * Generates a numerical name using a random number and a suffix based on the
     * number's modulo.
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
