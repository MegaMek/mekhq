package mekhq.campaign.personnel.turnoverAndRetention;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;

import java.util.ResourceBundle;

public class Morale {

    /**
     * This method returns the Morale level as a string based on the value of the 'Morale' variable.
     *
     * @return The Morale level as a string.
     * @throws IllegalStateException if the value of 'Morale' is unexpected.
     */
    public static String getMoraleLevel(Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        switch(campaign.getMorale()) {
            case 1:
                return resources.getString("moraleLevelUnbreakable.text");
            case 2:
                return resources.getString("moraleLevelVeryHigh.text");
            case 3:
                return resources.getString("moraleLevelHigh.text");
            case 4:
                return resources.getString("moraleLevelNormal.text");
            case 5:
                return resources.getString("moraleLevelLow.text");
            case 6:
                return resources.getString("moraleLevelVeryLow.text");
            case 7:
                return resources.getString("moraleLevelBroken.text");
            default:
                throw new IllegalStateException("Unexpected value in getMoraleLevel(): " + campaign.getMorale());
        }
    }

    /**
     * Calculates the base target number for desertion based on the morale of the campaign.
     *
     * @param campaign the campaign for which to calculate the base target number for desertion
     * @return the base target number for desertion
     * @throws IllegalStateException if the morale value of the campaign is unexpected
     */
    public static Integer getDesertionBaseTargetNumber(Campaign campaign) {
        switch(campaign.getMorale()) {
            case 1:
            case 2:
            case 3:
                return 0;
            case 4:
                return 2;
            case 5:
            case 6:
                return 5;
            case 7:
                return 8;
            default:
                throw new IllegalStateException("Unexpected value in getDesertionTargetNumber: " + campaign.getMorale());
        }
    }

    /**
     * Calculates the base target number for mutiny based on the morale of the campaign.
     *
     * @param campaign the campaign for which to calculate the base target number for mutiny
     * @return the base target number for mutiny
     * @throws IllegalStateException if the morale value of the campaign is unexpected
     */
    public static Integer getMutinyBaseTargetNumber(Campaign campaign) {
        switch(campaign.getMorale()) {
            case 1:
            case 2:
            case 3:
            case 4:
                return 0;
            case 5:
            case 6:
                return 4;
            case 7:
                return 7;
            default:
                throw new IllegalStateException("Unexpected value in getDesertionTargetNumber: " + campaign.getMorale());
        }
    }

    /**
     * Returns a morale report for the given Campaign.
     *
     * @param campaign the Campaign for which to generate the report
     * @return the morale report as a String
     */
    public static String getMoraleReport(Campaign campaign) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Morale",
                MekHQ.getMHQOptions().getLocale());

        StringBuilder moraleReport = new StringBuilder();

        if ((getDesertionBaseTargetNumber(campaign) - campaign.getCampaignOptions().getCustomMoraleModifier()) >= 2) {
            moraleReport.append(String.format(resources.getString("moraleReportLow.text="), getMoraleReport(campaign)));
        } else {
            moraleReport.append(String.format(resources.getString("moraleReport.text="), getMoraleReport(campaign)));
        }

        if ((getMutinyBaseTargetNumber(campaign) - campaign.getCampaignOptions().getCustomMoraleModifier()) >= 2) {
            moraleReport.append(' ').append(resources.getString("moraleReportMutiny.text="));
        }

        return moraleReport.toString();
    }
}
