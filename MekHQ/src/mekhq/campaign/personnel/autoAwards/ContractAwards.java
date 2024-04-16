package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ContractAwards {
    /**
     * This function loops through Contract Awards, checking whether the person is eligible to receive each type of award
     * @param campaign the campaign to be processed
     * @param mission the mission that just concluded
     * @param awards the awards to be processed (should only include awards where item == Kill)
     * @param person the person to check award eligibility for
     */
    public ContractAwards(Campaign campaign, Mission mission, List<Award> awards, Person person) {
        final ResourceBundle resource = ResourceBundle.getBundle("mekhq.resources.AutoAwards",
                MekHQ.getMHQOptions().getLocale());

        long contractDuration = ChronoUnit.MONTHS.between(((Contract) mission).getStartDate(),
                ((Contract) mission).getEndingDate());

        // these entries should always be in lower case
        List<String> validTypes = Arrays.asList("months", "duty", "garrison duty", "cadre duty", "security duty",
                "riot duty", "planetary assault", "relief duty", "guerrilla warfare", "pirate hunting", "raid",
                "diversionary raid", "objective raid", "recon raid", "extraction raid");

        boolean isEligible;

        for (Award award : awards) {
            isEligible = false;

            if (award.canBeAwarded(person)) {
                if (award.getRange().equalsIgnoreCase("months")) {
                    try {
                        int requiredDuration = award.getQty();

                        if (contractDuration >= requiredDuration) {
                            isEligible = true;
                        }
                    } catch (Exception e) {
                        LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                                award.getName(), award.getSet(), award.getQty());
                        continue;
                    }
                } else if (validTypes.contains(award.getRange().toLowerCase())) {
                    switch (award.getRange().toLowerCase()) {
                        case "duty":
                            if (mission.getType().toLowerCase().contains("duty")) {
                                isEligible = true;
                            }
                            break;
                        case "raid":
                            if (mission.getType().toLowerCase().contains("raid")) {
                                isEligible = true;
                            }
                            break;
                        default:
                            if (mission.getType().equalsIgnoreCase(award.getRange())) {
                                isEligible = true;
                            }
                    }
                } else {
                    LogManager.getLogger().warn("Award {} from the {} set has an invalid range value {}",
                            award.getName(), award.getSet(), award.getRange());
                    continue;
                }
            }

            if (isEligible) {
                // we have to include ' ' as hyperlinked names lose their hyperlink if used within resource.getString()
                campaign.addReport(person.getHyperlinkedName() + ' ' +
                        MessageFormat.format(resource.getString("EligibleForAwardReport.format"),
                                award.getName(), award.getSet()));
            }
        }
    }
}
