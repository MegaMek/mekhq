package mekhq.campaign.personnel.autoAwards;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class TrainingAwards {
    /**
     * This function loops through Training Awards, checking whether the person is eligible to receive each type of award.
     *
     * @param campaign the campaign to be processed
     * @param person the person to check award eligibility for
     * @param academyAttributes the attributes of the person's academy (education level, type, name)
     * @param awards the awards to be processed (should only include awards where item == Training)
     * @return a mapping of award IDs to lists of eligible awards for the given person
     */
    public static Map<Integer, List<Object>> TrainingAwardsProcessor(Campaign campaign, UUID person, List<Object> academyAttributes, List<Award> awards) {
        Person student = campaign.getPerson(person);
        List<Award> eligibleAwards = new ArrayList<>();

        int academyEducationLevel;
        int academyType;
        String academyName;

        // We start by prepping the data we're going to be comparing against and ensuring it's all valid
        try {
            academyEducationLevel = (int) academyAttributes.get(0);
        } catch (ClassCastException e) {
            LogManager.getLogger().warn("{} has invalid academyEducationLevel value '{}'. Aborting.",
                    student.getFullName(), academyAttributes.get(0).toString());

            return AutoAwardsController.prepareAwardData(person, eligibleAwards);
        }

        try {
            academyType = (int) academyAttributes.get(1);
        } catch (ClassCastException e) {
            LogManager.getLogger().warn("{} has invalid academyType value '{}'. Aborting.",
                    student.getFullName(), academyAttributes.get(1).toString());

            return AutoAwardsController.prepareAwardData(person, eligibleAwards);
        }

        try {
            academyName = academyAttributes.get(2).toString();
        } catch (ClassCastException e) {
            LogManager.getLogger().warn("{} has invalid academyName value '{}'. Aborting.",
                    student.getFullName(), academyAttributes.get(2).toString());

            return AutoAwardsController.prepareAwardData(person, eligibleAwards);
        }

        // Then we process the individual awards
        for (Award award : awards) {
            int requiredEducationLevel;
            int requiredType;
            String requiredAcademyName;

            try {
                requiredEducationLevel = award.getQty();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(), award.getSet(), award.getQty());
                continue;
            }

            try {
                requiredType = Integer.parseInt(award.getSize());
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid size value {}",
                        award.getName(), award.getSet(), award.getSize());
                continue;
            }

            try {
                requiredAcademyName = award.getRange();
            } catch (Exception e) {
                LogManager.getLogger().warn("Award {} from the {} set has an invalid range value {}",
                        award.getName(), award.getSet(), award.getRange());
                continue;
            }

            if (award.canBeAwarded(campaign.getPerson(person))) {
                if ((requiredEducationLevel != 0) && (requiredEducationLevel < academyEducationLevel)) {
                    eligibleAwards.add(award);
                    continue;
                }

                if ((requiredType != 0) && (requiredType == academyType)) {
                    eligibleAwards.add(award);
                    continue;
                }

                if (Objects.equals(requiredAcademyName, academyName)) {
                    eligibleAwards.add(award);
                }
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
