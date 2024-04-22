package mekhq.campaign.personnel.autoAwards;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class ContractAwards {
    final Campaign campaign;
    final Person person;

    /**
     * This function loops through Contract Awards, checking whether the person is eligible to receive each type of award
     * @param c the campaign to be processed
     * @param mission the mission that just concluded
     * @param awards the awards to be processed (should only include awards where item == Kill)
     * @param p the person to check award eligibility for
     */
    public ContractAwards(Campaign c, Mission mission, List<Award> awards, Person p) {
        campaign = c;
        person = p;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        long contractDuration = ChronoUnit.MONTHS.between(
                ((Contract) mission).getStartDate(),
                campaign.getLocalDate());

        // these entries should always be in lower case
        List<String> validTypes = Arrays.asList("months", "duty", "garrison duty", "cadre duty", "security duty",
                "riot duty", "planetary assault", "relief duty", "guerrilla warfare", "pirate hunting", "raid",
                "diversionary raid", "objective raid", "recon raid", "extraction raid");

        for (Award award : awards) {
            if (award.canBeAwarded(person)) {
                if (award.getRange().equalsIgnoreCase("months")) {
                    try {
                        int requiredDuration = award.getQty();

                        if (contractDuration >= requiredDuration) {
                            eligibleAwardsBestable.add(award);
                        }
                    } catch (Exception e) {
                        LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                                award.getName(), award.getSet(), award.getQty());
                    }
                } else if (validTypes.contains(award.getRange().toLowerCase())) {
                    switch (award.getRange().toLowerCase()) {
                        case "duty":
                            if (mission.getType().toLowerCase().contains("duty")) {
                                eligibleAwards.add(award);
                            }
                            break;
                        case "raid":
                            if (mission.getType().toLowerCase().contains("raid")) {
                                eligibleAwards.add(award);
                            }
                            break;
                        default:
                            if (mission.getType().equalsIgnoreCase(award.getRange())) {
                                eligibleAwards.add(award);
                            }
                    }
                } else {
                    LogManager.getLogger().warn("Award {} from the {} set has an invalid range value {}",
                            award.getName(), award.getSet(), award.getRange());
                }
            }
        }

        if (!eligibleAwards.isEmpty()) {
            for (Award award : eligibleAwards) {
                announceEligibility(award);
            }
        }

        if (!eligibleAwardsBestable.isEmpty()) {
            int rollingQty = 0;

            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                for (Award award : eligibleAwardsBestable) {
                    if (award.getQty() > rollingQty) {
                        rollingQty = award.getQty();
                        bestAward = award;
                    }
                }
                announceEligibility(bestAward);
            } else {
                for (Award award : eligibleAwardsBestable) {
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
