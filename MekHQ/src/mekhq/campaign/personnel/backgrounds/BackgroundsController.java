package mekhq.campaign.personnel.backgrounds;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.EndWordCorporate;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.EndWordMercenary;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.MiddleWordCorporate;
import mekhq.campaign.personnel.backgrounds.enums.mercenaryCompanyNameGenerator.MiddleWordMercenary;

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
        int roll = Compute.randomInt(7);

        return switch (roll) {
            case 0 -> { // Corporate
                String name = MiddleWordCorporate.getRandomWord();
                String newWordSuggestion = getNewWord(name, EndWordCorporate::getRandomWord);

                yield name + ' ' + newWordSuggestion;
            }
            case 1 -> { // Mercenary - Vanity 1
                String name = getCommanderName(commander);

                String newWordSuggestion = getNewWord(name, EndWordMercenary::getRandomWord);

                yield name + ' ' + newWordSuggestion;
            }
            case 2 -> { // Mercenary - Vanity 2
                String name = getCommanderName(commander);

                name += "'s " + getNewWord(name, MiddleWordMercenary::getRandomWord);
                String newWordSuggestion = getNewWord(name, EndWordMercenary::getRandomWord);

                yield name + ' ' + newWordSuggestion;
            }
            case 3, 4, 5, 6 -> { // Mercenary
                String name = MiddleWordMercenary.getRandomWord();
                String newWordSuggestion = getNewWord(name, EndWordMercenary::getRandomWord);

                yield name + ' ' + newWordSuggestion;
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
            return commander.getCallsign().isBlank() ? commander.getSurname() : commander.getCallsign();
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
