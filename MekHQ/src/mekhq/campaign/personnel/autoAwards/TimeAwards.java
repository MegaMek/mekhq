package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class TimeAwards {
    /**
     * This function loops through Time Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == Time)
     * @param person the person to check award eligibility for
     */
    public TimeAwards(Campaign campaign, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        int requiredYearsOfService;
        boolean isCumulative;
        int yearsOfService;

        for (Award award : awards) {
            try {
                requiredYearsOfService = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(), award.getSet(), award.getQty());
                break;
            }

            if (award.getRange().equalsIgnoreCase("cumulative")) {
                isCumulative = Boolean.parseBoolean(award.getRange());
            } else {
                isCumulative = false;
            }

            if (award.canBeAwarded(person)) {
                yearsOfService = Integer.parseInt(Pattern.compile("\\s+")
                        .splitAsStream(person.getTimeInService(campaign))
                        .findFirst()
                        .orElseThrow());

                if (isCumulative) {
                    requiredYearsOfService *= person.getAwardController().getNumberOfAwards(award) + 1;
                }

                if (yearsOfService >= requiredYearsOfService) {
                    // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                    campaign.addReport(person.getHyperlinkedName() + ' ' +
                            MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                    award.getName(), award.getSet()));
                }
            }
        }
    }
}
