/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.rating.CamOpsReputation;

import static java.lang.Math.max;
import static megamek.common.force.Force.NO_FORCE;

import megamek.codeUtilities.MathUtility;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.ProtoMek;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;

public class AverageExperienceRating {
    private static final MMLogger LOGGER = MMLogger.create(AverageExperienceRating.class);

    /**
     * Calculates the skill level based on the average experience rating of a campaign.
     *
     * @param campaign the campaign to calculate the average experience rating from
     * @param log      whether to log the calculation in mekhq.log
     *
     * @return the skill level based on the average experience rating
     *
     * @throws IllegalStateException if the experience score is not within the expected range
     */
    protected static SkillLevel getSkillLevel(Campaign campaign, boolean log) {
        // values below 0 are treated as 'Legendary',
        // values above 7 are treated as 'wet behind the ears' which we call 'None'
        int experienceScore = MathUtility.clamp(calculateAverageExperienceRating(campaign, log), 0, 7);

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
                  "Unexpected value in mekhq/campaign/rating/CamOpsRatingV2/AverageExperienceRating.java/getSkillLevel: " +
                        experienceScore);
        };
    }

    /**
     * Retrieves the average experience level of the campaign. Useful for ATB Systems.
     *
     * @param averageSkillLevel the average skill level for which to calculate the reputation modifier
     *
     * @return the reputation modifier for the camera operator
     */
    protected static int getAverageExperienceModifier(SkillLevel averageSkillLevel) {
        int modifier = switch (averageSkillLevel) {
            case NONE, ULTRA_GREEN, GREEN -> 5;
            case REGULAR -> 10;
            case VETERAN -> 20;
            case ELITE, HEROIC, LEGENDARY -> 40;
        };

        LOGGER.debug("Reputation Rating = {}, +{}", averageSkillLevel.toString(), modifier);

        return modifier;
    }

    /**
     * Calculates the average experience rating of combat personnel in the given campaign.
     *
     * @param campaign the campaign to calculate the average experience rating for
     * @param log      whether to log the calculation to mekhq.log
     *
     * @return the average experience rating of personnel in the campaign
     */
    private static int calculateAverageExperienceRating(Campaign campaign, boolean log) {
        int personnelCount = 0;
        double totalExperience = 0.0;

        for (Person person : campaign.getActivePersonnel(false, false)) {
            Unit unit = person.getUnit();

            // if the person does not belong to a unit, then skip this person
            if (unit == null) {
                continue;
            }

            // If the unit does not belong to a force, skip it
            int forceId = unit.getForceId();
            if (forceId == NO_FORCE) {
                continue;
            }
            Force force = campaign.getForce(forceId);
            if (force == null) {
                LOGGER.warn("Force returned null for forceId {}", forceId);
                continue;
            }

            // If the unit does not belong to a standard force, skip it
            if (!force.isForceType(ForceType.STANDARD)) {
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
            SkillModifierData skillModifierData = person.getSkillModifierData();

            // Experience calculation varies depending on the type of entity
            if (entity instanceof Infantry) {
                // we only want to parse infantry units once, as CamOps treats them as an
                // individual entity
                if (!unit.isCommander(person)) {
                    continue;
                }

                // For Infantry, average experience is calculated using a different method.
                totalExperience += calculateInfantryExperience((Infantry) entity, crew); // add the average experience
                // to the total
                personnelCount++;
            } else if (entity instanceof ProtoMek) {
                // ProtoMek entities only use gunnery for calculation
                if (person.hasSkill(SkillType.S_GUN_PROTO)) {
                    totalExperience += max(0,
                          person.getSkill(SkillType.S_GUN_PROTO)
                                .getFinalSkillValue(skillModifierData));
                }

                personnelCount++;
            } else {
                // For regular entities, another method calculates the average experience
                if (unit.isGunner(person) || unit.isDriver(person)) {
                    totalExperience += calculateRegularExperience(person, entity, unit);

                    if (totalExperience > 0) {
                        personnelCount++;
                    }
                }
            }
        }

        if (personnelCount == 0) {
            return 7;
        }

        // Calculate the average experience rating across all personnel. If there are no
        // personnel, return 0
        double rawAverage = personnelCount > 0 ? (totalExperience / personnelCount) : 0;

        // CamOps wants us to round down from 0.5 and up from >0.5, so we need to do an
        // extra step here
        double fractionalPart = rawAverage - Math.floor(rawAverage);

        int averageExperienceRating = (int) (fractionalPart > 0.5 ? Math.ceil(rawAverage) : Math.floor(rawAverage));

        // Log the details of the calculation to aid debugging,
        // and so the user can easily see if there is a mistake
        if (log) {
            LOGGER.debug("Average Experience Rating: {} / {} = {}",
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
     * @param crew     The unit crew.
     *
     * @return The average experience of the Infantry crew.
     */
    private static double calculateInfantryExperience(Infantry infantry, Crew crew) {
        // Average of gunnery and antiMek skill
        int gunnery = max(0, crew.getGunnery());
        int antiMek = max(0, infantry.getAntiMekSkill());

        return (double) (gunnery + antiMek) / 2;
    }

    /**
     * Calculates the average experience of a (non-Infantry, non-ProtoMek) crew.
     *
     * @param person The person in the crew.
     * @param entity The entity associated with the crew.
     * @param unit   The unit the crew belongs to.
     *
     * @return The average experience of the crew.
     */
    private static double calculateRegularExperience(Person person, Entity entity, Unit unit) {
        String skillType;

        int skillValue = 0;
        int skillCount = 0;

        SkillModifierData skillModifierData = person.getSkillModifierData();
        if (unit.isDriver(person)) {
            skillType = SkillType.getDrivingSkillFor(entity);
            Skill skill = person.getSkill(skillType);

            if (skill != null) {
                skillValue += max(0, skill.getFinalSkillValue(skillModifierData));
                skillCount++;
            } else {
                LOGGER.warn("(calculateRegularExperience) unable to fetch diving skill {} for {}. Skipping",
                      skillType,
                      entity);
            }
        }

        if (unit.isGunner(person)) {
            skillType = SkillType.getGunnerySkillFor(entity);

            Skill skill = person.getSkill(skillType);
            if (skill != null) {
                skillValue += max(0, skill.getFinalSkillValue(skillModifierData));
                skillCount++;
            } else {
                LOGGER.warn("(calculateRegularExperience) unable to fetch gunnery skill {} for {}. Skipping",
                      skillType,
                      entity);
            }
        }

        if (skillCount == 0) {
            return 0;
        }

        return (double) skillValue / skillCount;
    }
}
