package mekhq.campaign.personnel.backgrounds;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.*;

import java.util.ResourceBundle;
import java.util.function.Supplier;

public class BackgroundsController {
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
        int roll = Compute.d6(1);

        return switch (roll) {
            case 1 -> { // Corporate
                String name = MiddleWordCorporate.getRandomWord();
                String newWordSuggestion = getNewWord(name, EndWordCorporate::getRandomWord);

                yield name + ' ' + newWordSuggestion;
            }
            case 2 -> { // Mercenary - Vanity 1
                String name = getCommanderName(commander) + "'s ";

                String newWordSuggestion = getNewWord(name, EndWordMercenary::getRandomWord);

                yield name + newWordSuggestion;
            }
            case 3 -> { // Mercenary - Vanity 2
                String name = getCommanderName(commander) + "'s ";

                name += getNewWord(name, MiddleWordMercenary::getRandomWord) + ' ';
                String newWordSuggestion = getNewWord(name, EndWordMercenary::getRandomWord);

                yield name + newWordSuggestion;
            }
            case 4 -> { // Mercenary - Generic
                final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RandomMercenaryCompanyNameGenerator");

                String name = resources.getString("definiteArticle.text");
                name += ' ' + MiddleWordMercenary.getRandomWord();
                String newWordSuggestion = getNewWord(name, EndWordMercenary::getRandomWord);

                yield name + ' ' + newWordSuggestion;
            }
            case 5 -> { // Pre-Fab
                final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.RandomMercenaryCompanyNameGenerator");

                yield resources.getString("definiteArticle.text") + ' ' + PreFabHumorous.getRandomWord();
            }
            case 6 -> { // Pre-Fab - Vanity
                String name = getCommanderName(commander) + "'s ";
                yield name + PreFabHumorous.getRandomWord();
            }
            default -> throw new IllegalStateException(
                    "Unexpected value in mekhq/campaign/personnel/backgrounds/BackgroundsController.java/randomMercenaryCompanyNameGenerator: "
                    + roll
            );
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
            return RandomCallsignGenerator.getWeightedCallsigns().randomItem();
        } else {
            String name = commander.getCallsign().isBlank() ? commander.getSurname() : commander.getCallsign();
            return name.isBlank() ? commander.getFirstName() : name;
        }
    }

    /**
     * Retrieves a new word based on the given name and end word supplier.
     *
     * @param name The name to check if it contains the generated word.
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
     * @param currentName         the current name to check against
     * @param suggestedAddition   the suggested addition to the name
     * @return true if the start of the suggested addition is not present in the current name, otherwise false
     */
    public static boolean checkIfNameContains(String currentName, String suggestedAddition) {
        int checkLength = suggestedAddition.length() - 2;

        String startOfSecondString = suggestedAddition.substring(0, checkLength);

        return currentName.contains(startOfSecondString);
    }
}
