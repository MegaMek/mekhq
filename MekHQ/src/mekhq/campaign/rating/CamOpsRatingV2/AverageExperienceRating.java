package mekhq.campaign.rating.CamOpsRatingV2;

import megamek.MegaMek;
import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

public class AverageExperienceRating {
    private static final MMLogger logger = MMLogger.create(MegaMek.class);

    /**
     * Calculates the skill level based on the average experience rating of a campaign.
     *
     * @param campaign the campaign to calculate the average experience rating from
     * @return the skill level based on the average experience rating
     * @throws IllegalStateException if the experience score is not within the expected range
     */
    protected static SkillLevel getSkillLevel(Campaign campaign) {
        // values below 0 are treated as 'Legendary',
        // values above 7 are treated as 'wet behind the ears' which we call 'None'
        int experienceScore = MathUtility.clamp(
                calculateAverageExperienceRating(campaign),
                0,
                7
        );

        return switch (experienceScore) {
            case 7 -> SkillLevel.NONE;
            case 6 -> SkillLevel.ULTRA_GREEN;
            case 5 -> SkillLevel.GREEN;
            case 4 -> SkillLevel.REGULAR;
            case 3 -> SkillLevel.VETERAN;
            case 2 -> SkillLevel.ELITE;
            case 1 -> SkillLevel.HEROIC;
            case 0 -> SkillLevel.LEGENDARY;
            default -> throw new IllegalStateException(
                    "Unexpected value in mekhq/campaign/rating/CamOpsRatingV2/AverageExperienceRating.java/getSkillLevel: "
                            + experienceScore
            );
        };
    }

    /**
     * Retrieves the reputation modifier.
     *
     * @param averageSkillLevel the average skill level to calculate the reputation modifier for
     * @return the reputation modifier for the camera operator
     */
    protected static int getReputationModifier(SkillLevel averageSkillLevel) {
        return switch(averageSkillLevel) {
            case NONE, ULTRA_GREEN, GREEN -> 5;
            case REGULAR -> 10;
            case VETERAN -> 20;
            case ELITE, HEROIC, LEGENDARY -> 40;
        };
    }

    /**
     * Calculates a modifier for Against the Bot's various systems, based on the average skill level.
     *
     * @param campaign the campaign from which to calculate the ATB modifier
     * @return the ATB modifier as an integer value
     */
    public static int getAtBModifier(Campaign campaign) {
        SkillLevel averageSkillLevel = getSkillLevel(campaign);

        return switch (averageSkillLevel) {
            case NONE, ULTRA_GREEN -> 0;
            case GREEN -> 1;
            case REGULAR -> 2;
            case VETERAN -> 3;
            case ELITE -> 4;
            case HEROIC, LEGENDARY -> 5;
        };
    }

    /**
     * Calculates the average experience rating of combat personnel in the given campaign.
     *
     * @param campaign the campaign to calculate the average experience rating for
     * @return the average experience rating of personnel in the campaign
     */
    private static int calculateAverageExperienceRating(Campaign campaign) {
        int personnelCount = 0;
        double totalExperience = 0.0;

        for (Person person : campaign.getActivePersonnel()) {
            Unit unit = person.getUnit();

            // if the person does not belong to a unit or is not the commander, then skip this person
            if (unit == null || !unit.isCommander(person)) {
                continue;
            }

            Entity entity = unit.getEntity();
            // if the unit's entity is a JumpShip, then it is not considered combatant.
            if (entity instanceof Jumpship) {
                continue;
            }

            // if both primary and secondary roles are support roles, skip this person
            // as they are also not considered combat personnel
            if (person.getPrimaryRole().isSupport() && person.getSecondaryRole().isSupport()) {
                continue;
            }

            Crew crew = entity.getCrew();
            double averageExperience;

            // Experience calculation varies depending on the type of entity
            if (entity instanceof Infantry) {
                // For Infantry, average experience is calculated a different method.
                averageExperience = calculateInfantryExperience((Infantry) entity, crew);
                personnelCount++;
            } else if (entity instanceof Protomech) {
                // ProtoMech entities only use gunnery for calculation
                averageExperience = crew.getGunnery();
                personnelCount += unit.getActiveCrew().size();
            } else {
                // For regular entities, another method calculates the average experience
                averageExperience = calculateRegularExperience(crew);
                personnelCount += unit.getActiveCrew().size();
            }

            totalExperience += averageExperience; // add the average experience to the total
        }

        // Calculate the average experience rating across all personnel. If there are no personnel, return 0
        double rawAverage = personnelCount > 0 ? (totalExperience / personnelCount) : 0;

        // CamOps wants us to round down from 0.5 and up from >0.5, so we need to do an extra step here
        double fractionalPart = rawAverage - Math.floor(rawAverage);

        int averageExperienceRating = (int) (fractionalPart > 0.5 ? Math.ceil(rawAverage) : Math.floor(rawAverage));

        // Log the details of the calculation to aid debugging,
        // and so the user can easily see if there is a mistake
        logger.info("Average Experience Rating: {} / {} = {}",
                totalExperience,
                personnelCount,
                averageExperienceRating);

        // Return the average experience rating
        return averageExperienceRating;
    }

    /**
     * Calculates the average experience of an Infantry entity's crew.
     *
     * @param infantry The Infantry entity, which also includes some crew details.
     * @param crew The unit crew.
     * @return The average experience of the Infantry crew.
     */
    private static double calculateInfantryExperience(Infantry infantry, Crew crew) {
        // Average of gunnery and antiMek skill
        return (double) (crew.getGunnery() + infantry.getAntiMekSkill()) / 2;
    }

    /**
     * Calculates the average experience of a (non-Infantry, non-ProtoMech) crew.
     *
     * @param crew The unit's crew.
     * @return The average experience of the crew.
     */
    private static double calculateRegularExperience(Crew crew) {
        // Average of gunnery and piloting skills
        return (double) (crew.getGunnery() + crew.getPiloting()) / 2;
    }
}
