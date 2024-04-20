package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class TimeAwards {
    Campaign campaign;
    Person person;

    /**
     * This function loops through Time Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param awards the awards to be processed (should only include awards where item == Time)
     * @param person the person to check award eligibility for
     */
    public TimeAwards(Campaign c, Person p, List<Award> awards) {
        campaign = c;
        person = p;

        int requiredYearsOfService;
        boolean isCumulative;
        int yearsOfService;

        List<Award> eligibleAwards = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            try {
                requiredYearsOfService = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            try {
                isCumulative = award.isStackable();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid stackable value {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (award.canBeAwarded(person)) {
                try {
                    yearsOfService = Integer.parseInt(Pattern.compile("\\s+")
                            .splitAsStream(person.getTimeInService(campaign))
                            .findFirst()
                            .orElseThrow());
                } catch (Exception e) {
                    LogManager.getLogger().error("Unable to parse yearsOfService for {} while processing Award {} from the [{}] set." +
                                    " This can be ignored if {} has 0 years Time in Service.",
                            person.getFullName(), award.getName(), award.getSet(), person.getFullName());
                    continue;
                }

                if (isCumulative) {
                    requiredYearsOfService *= person.getAwardController().getNumberOfAwards(award) + 1;
                }

                if (yearsOfService >= requiredYearsOfService) {
                    eligibleAwards.add(award);
                }
            }
        }

        if (!eligibleAwards.isEmpty()) {
            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                int rollingQty = 0;

                for (Award award : eligibleAwards) {
                    if (award.getQty() > rollingQty) {
                        rollingQty = award.getQty();
                        bestAward = award;
                    }
                }

                announceEligibility(bestAward);
            } else {
                for (Award award : eligibleAwards) {
                    announceEligibility(award);
                }
            }
        }
    }

    /**
     * This function announced Award eligibility to the Daily Report pane
     * @param award the award to be announced
     */
    private void announceEligibility (Award award){
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
        campaign.addReport(person.getHyperlinkedName() + ' ' + MessageFormat.format(resource.getString(
                "EligibleForAwardReport.format"), award.getName(), award.getSet()));
    }
}
