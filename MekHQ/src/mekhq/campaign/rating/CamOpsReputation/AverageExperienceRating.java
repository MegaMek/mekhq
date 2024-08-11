package mekhq.campaign.rating.CamOpsReputation;

import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;

public class AverageExperienceRating {
    private static final MMLogger logger = MMLogger.create(AverageExperienceRating.class);

    /**
     * Calculates the skill level based on the average experience rating of a campaign.
     *
     * @param campaign the campaign to calculate the average experience rating from
     * @param log whether to log the calculation in mekhq.log
     * @return the skill level based on the average experience rating
     * @throws IllegalStateException if the experience score is not within the expected range
     */
    protected static SkillLevel getSkillLevel(Campaign campaign, boolean log) {
        // values below 0 are treated as 'Legendary',
        // values above 7 are treated as 'wet behind the ears' which we call 'None'
        int experienceScore = MathUtility.clamp(
                calculateAverageExperienceRating(campaign, log),
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
        int modifier = switch(averageSkillLevel) {
            case NONE, ULTRA_GREEN, GREEN -> 5;
            case REGULAR -> 10;
            case VETERAN -> 20;
            case ELITE, HEROIC, LEGENDARY -> 40;
        };

        logger.debug("Reputation Rating = {}, +{}",
                averageSkillLevel.toString(),
                modifier);

        return modifier;
    }

    /**
     * Calculates a modifier for Against the Bot's various systems, based on the average skill level.
     *
     * @param campaign the campaign from which to calculate the ATB modifier
     * @return the ATB modifier as an integer value
     */
    public static int getAtBModifier(Campaign campaign) {
        SkillLevel averageSkillLevel = getSkillLevel(campaign, false);

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
     * @param log whether to log the calculation to mekhq.log
     * @return the average experience rating of personnel in the campaign
     */
    private static int calculateAverageExperienceRating(Campaign campaign, boolean log) {
        int personnelCount = 0;
        double totalExperience = 0.0;

        for (Person person : campaign.getActivePersonnel()) {
            Unit unit = person.getUnit();

            // if the person does not belong to a unit, then skip this person
            if (unit == null) {
                continue;
            }

            Entity entity = unit.getEntity();
            // if the unit's entity is a JumpShip, then it is not considered a combatant.
            if (entity instanceof Jumpship) {
                continue;
            }

            // if both primary and secondary roles are support roles, skip this person
            // as they are also not considered combat personnel
            if (person.getPrimaryRole().isSupport() && person.getSecondaryRole().isSupport()) {
                continue;
            }

            Crew crew = entity.getCrew();

            // Experience calculation varies depending on the type of entity
            if (entity instanceof Infantry) {
                // we only want to parse infantry units once, as CamOps treats them as an individual entity
                if (!unit.isCommander(person)) {
                    continue;
                }

                // For Infantry, average experience is calculated using a different method.
                totalExperience += calculateInfantryExperience((Infantry) entity, crew); // add the average experience to the total
                personnelCount++;
            } else if (entity instanceof Protomech) {
                // ProtoMech entities only use gunnery for calculation
                if (person.hasSkill(SkillType.S_GUN_PROTO)) {
                    totalExperience += person.getSkill(SkillType.S_GUN_PROTO).getTotalSkillLevel();
                }

                personnelCount ++;
            } else {
                // For regular entities, another method calculates the average experience
                if (unit.isGunner(person) || unit.isDriver(person)) {
                    totalExperience += calculateRegularExperience(person, entity, unit);
                    personnelCount ++;
                }
            }
        }

        // Calculate the average experience rating across all personnel. If there are no personnel, return 0
        double rawAverage = personnelCount > 0 ? (totalExperience / personnelCount) : 0;

        // CamOps wants us to round down from 0.5 and up from >0.5, so we need to do an extra step here
        double fractionalPart = rawAverage - Math.floor(rawAverage);

        int averageExperienceRating = (int) (fractionalPart > 0.5 ? Math.ceil(rawAverage) : Math.floor(rawAverage));

        // Log the details of the calculation to aid debugging,
        // and so the user can easily see if there is a mistake
        if (log) {
        logger.debug("Average Experience Rating: {} / {} = {}",
                totalExperience,
                personnelCount,
                averageExperienceRating);
        }

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
        int gunnery = crew.getGunnery();
        int antiMek = infantry.getAntiMekSkill();

        return (double) (gunnery + antiMek) / 2;
    }

    /**
     * Calculates the average experience of a (non-Infantry, non-ProtoMech) crew.
     *
     * @param person The person in the crew.
     * @param entity The entity associated with the crew.
     * @param unit The unit the crew belongs to.
     * @return The average experience of the crew.
     */
    private static double calculateRegularExperience(Person person, Entity entity, Unit unit) {
        int sumOfSkillLevels = 0;
        int skillCount = 0;

        if (unit.isDriver(person)) {
            String drivingSkill = SkillType.getDrivingSkillFor(entity);

            if (person.hasSkill(drivingSkill)) {
                sumOfSkillLevels += person.getSkill(drivingSkill).getTotalSkillLevel();
                skillCount++;
            }
        }

        if (unit.isGunner(person)) {
            String gunnerySkill = SkillType.getGunnerySkillFor(entity);

            if (person.hasSkill(gunnerySkill)) {
                sumOfSkillLevels += person.getSkill(gunnerySkill).getTotalSkillLevel();
                skillCount++;
            }
        }

        if (sumOfSkillLevels > 0) {
            return (double) sumOfSkillLevels / skillCount;
        } else {
            return 0;
        }
    }
}
