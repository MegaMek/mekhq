package mekhq.campaign.personnel.backgrounds;

import megamek.common.Compute;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public class Toughness {
    /**
     * Generates the toughness attribute for a character.
     *
     * @param person The person for whom the toughness attribute is being generated.
     * @param resources The resource bundle for retrieving localized strings.
     */
    public static void generateToughness(Person person, ResourceBundle resources) {
        int roll = Compute.d6(2);

        if (roll == 2) {
            person.setToughness(-1);

            person.setBiography(String.format(resources.getString("toughnessWeakJaw.biography"), person.getFirstName()));
        } else if (roll == 12) {
            person.setToughness(1);

            person.setBiography(String.format(resources.getString("toughnessStrongJaw.biography"), person.getFirstName()));
        }
    }
}
