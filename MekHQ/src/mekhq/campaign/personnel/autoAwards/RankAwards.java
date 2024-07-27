package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RankAwards {
    //region Enum Declarations
    enum RankAwardsEnums {
        IMPLICIT("Implicit"),
        INCLUSIVE("Inclusive"),
        EXCLUSIVE("Exclusive");

        private final String name;

        RankAwardsEnums(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    //endRegion Enum Declarations

    /**
     * This function loops through Rank Awards, checking whether the person is eligible to receive each type of award.
     *
     * @param campaign the current campaign
     * @param personId the person to check award eligibility for
     * @param awards the awards to be processed (should only include awards where item == Kill)
     */
    public static Map<Integer, List<Object>> RankAwardsProcessor(Campaign campaign, UUID personId, List<Award> awards) {
        int requiredRankNumeric;
        boolean isEligible;

        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            try {
                requiredRankNumeric = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(),
                        award.getSet(),
                        award.getQty());
                continue;
            }

            boolean matchFound = false;

            // as there is only a max iteration of 3, there is no reason to use a stream here
            for (RankAwardsEnums value : RankAwardsEnums.values()) {
                if (value.getName().equalsIgnoreCase(award.getRange())) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                LogManager.getLogger().warn("Award {} from the {} set has the invalid range {}",
                        award.getName(),
                        award.getSet(),
                        award.getRange());
            }

            Person person = campaign.getPerson(personId);

            isEligible = switch (award.getRange()) {
                case "Implicit" -> person.getRankNumeric() == requiredRankNumeric;
                case "Inclusive" -> person.getRankNumeric() >= requiredRankNumeric;
                case "Exclusive" -> {
                    if ((requiredRankNumeric <= 20 && person.getRankNumeric() <= 20)
                            || (requiredRankNumeric <= 30 && person.getRankNumeric() <= 30)
                            || (requiredRankNumeric >= 31 && person.getRankNumeric() >= 31)) {
                        yield person.getRankNumeric() >= requiredRankNumeric;
                    } else {
                        yield false;
                    }
                }
                default -> false;
            };

            if (isEligible) {
                eligibleAwards.add(award);
            }
        }

        return AutoAwardsController.prepareAwardData(personId, eligibleAwards);
    }
}
