package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;

public class MiscAwards {
    /**
     * This function loops through Misc Awards, checking whether the person is eligible to receive each type of award.
     * All Misc awards need to be coded as individual functions
     * @param campaign the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == Kill)
     * @param person the person to check award eligibility for
     * @param missionWasSuccessful true if the completed mission was successful
     */
    public MiscAwards(Campaign campaign, List<Award> awards, Person person, Boolean missionWasSuccessful) {
        for (Award award : awards) {
            switch (award.getRange().toLowerCase()) {
                case "mission accomplished":
                    if (missionWasSuccessful) {
                        MissionAccomplishedAward(campaign, award, person);
                    }
                default:
            }
        }
    }

    /**
     * This function checks whether the Mission Accomplished award can be awarded to Person
     * @param campaign the current campaign
     * @param award the award to be processed
     * @param person the person to check award eligibility for
     */
    private void MissionAccomplishedAward(Campaign campaign, Award award, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        if (award.canBeAwarded(person)) {
            campaign.addReport(MessageFormat.format(resource.getString("EligibleForAwardReportAll.format"),
                    award.getName(), award.getSet()));
        }
    }
}
