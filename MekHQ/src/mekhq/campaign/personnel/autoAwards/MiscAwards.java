package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MiscAwards {

    /**
     * This function loops through Misc Awards, checking whether the person is eligible to receive each type of award.
     * All Misc awards need to be coded as individual functions
     * @param person the person to check award eligibility for
     * @param awards the awards to be processed (should only include awards where item == Kill)
     * @param missionWasSuccessful true if the completed mission was successful
     */
    public static Map<Integer, List<Object>> MiscAwardsProcessor(Person person, List<Award> awards, Boolean missionWasSuccessful) {
        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            switch (award.getRange().replaceAll("\\s","").toLowerCase()) {
                case "missionaccomplished":
                    if (missionWasSuccessful) {
                        try {
                            eligibleAwards.add(MissionAccomplishedAward(award, person));
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    break;
                default:
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }

    /**
     * This function checks whether the Mission Accomplished award can be awarded to Person
     * @param award the award to be processed
     * @param person the person to check award eligibility for
     */
    private static Award MissionAccomplishedAward(Award award, Person person) {
        if (award.canBeAwarded(person)) {
            return award;
        } else {
            return null;
        }
    }
}
