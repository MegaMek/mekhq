package mekhq.campaign.personnel.education;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

public class EducationController {
    public EducationController(Campaign campaign, Person person, String origin) {
        switch (origin) {
            case "Basic Training":
                break;
            case "Local Academy":
                LocalAcademy.localAcademy(campaign, person);
                break;
            case "MechWarrior Academy":
                break;
            case "Prestigious Academy":
                break;
            case "NCO Academy":
                break;
            case "Officer Academy":
                break;
            case "Tech Academy":
                break;
            case "Warrant Officer Academy":
                break;
            default:
                throw new IllegalStateException("Education module found unexpected value: "
                        + origin.replaceAll("\\s", "").toLowerCase());
        }
    }

    public static int getBalance(Campaign campaign) {
        String balance = String.valueOf(campaign.getFinances().getBalance()).replaceAll("CSB ", "");

        if (balance.contains(".")) {
            balance = balance.substring(0, balance.indexOf('.'));
        }

        return Integer.parseInt(balance);
    }
}
