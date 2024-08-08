package mekhq.campaign.personnel.backgrounds;

import megamek.common.Compute;
import mekhq.campaign.personnel.Person;

public class Toughness {
    /**
     * Generates the toughness attribute for a character.
     *
     * @param person The person for whom the toughness attribute is being generated.
     */
    public static void generateToughness(Person person) {
        int roll = Compute.d6(2);

        if (roll == 2) {
            person.setToughness(-1);
        } else if (roll == 12) {
            person.setToughness(1);
        }
    }
}
