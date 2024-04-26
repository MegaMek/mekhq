package mekhq.campaign.personnel.education;

import mekhq.campaign.Campaign;

public class EducationController {
    public EducationController(Campaign campaign, String origin) {
        switch (origin) {
            case "Basic Training":
                break;
            case "Local Academy":
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
}
