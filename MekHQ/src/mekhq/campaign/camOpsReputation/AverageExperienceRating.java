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
package mekhq.campaign.camOpsReputation;

import java.util.ArrayList;

import megamek.codeUtilities.MathUtility;
import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;

public class AverageExperienceRating {
    private static final MMLogger LOGGER = MMLogger.create(AverageExperienceRating.class);

    private static final int NO_CAMPAIGN_EXPERIENCE = 7;

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
        int experienceScore = MathUtility.clamp(calculateAverageExperienceRating(campaign, log),
              0,
              NO_CAMPAIGN_EXPERIENCE);

        return switch (experienceScore) {
            case NO_CAMPAIGN_EXPERIENCE -> SkillLevel.NONE;
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
        int unitCount = 0;
        double totalExperience = 0;

        Hangar hangar = campaign.getHangar();
        ArrayList<CombatTeam> combatTeams = campaign.getCombatTeamsAsList();

        if (combatTeams.isEmpty()) {
            return NO_CAMPAIGN_EXPERIENCE;
        }

        boolean hasAtLeastOneCrew = false;
        for (CombatTeam combatTeam : combatTeams) {
            Force force = combatTeam.getForce(campaign);
            if (force == null) {
                LOGGER.warn("Force returned null for forceId {}", combatTeam.getForceId());
                continue;
            }

            // CamOps is explicit in that we should only be counting combat forces. A decision was made during 50.10
            // to not consider Training forces combat forces. We're extending that logic here, too.
            if (force.getCombatRoleInMemory().isTraining()) {
                continue;
            }

            for (Unit unit : force.getAllUnitsAsUnits(hangar, true)) {
                Entity entity = unit.getEntity();
                if (entity == null || entity instanceof Jumpship) {
                    continue;
                }

                // CamOps treats all units as single entities. Tracking down to the individual crew level is a MekHQ
                // invention. To keep as close to CamOps as possible, we only consider the unit commander when
                // calculating experience rating.
                Person commander = unit.getCommander();
                if (commander == null) { // Unit is uncrewed
                    continue;
                } else {
                    hasAtLeastOneCrew = true;
                }

                SkillModifierData skillModifierData = commander.getSkillModifierData(true);
                int pilotingTargetNumber = getSkillTargetNumber(commander, entity, skillModifierData, true);
                int gunneryTargetNumber = getSkillTargetNumber(commander, entity, skillModifierData, false);
                totalExperience += pilotingTargetNumber + gunneryTargetNumber;
                unitCount++;
            }
        }

        if (unitCount == 0 || !hasAtLeastOneCrew) { // this can equal false no matter what IDEA says
            return NO_CAMPAIGN_EXPERIENCE;
        }

        // CamOps states that we need to divide the skill target numbers by twice the unit count.
        unitCount *= 2;

        // Calculate the average experience rating across all personnel.
        double rawAverage = totalExperience / unitCount;

        // CamOps wants us to round down from 0.5 and up from >0.5, so we need to do an extra step here
        double fractionalPart = rawAverage - Math.floor(rawAverage);
        int averageExperienceRating = (int) (fractionalPart > 0.5 ? Math.ceil(rawAverage) : Math.floor(rawAverage));

        // Log the details of the calculation to aid debugging, and so the user can easily see if there is a mistake
        if (log) {
            LOGGER.info("Average Experience Rating: {} / {} = {}",
                  totalExperience,
                  unitCount,
                  averageExperienceRating);
        }

        // Return the average experience rating
        return averageExperienceRating;
    }

    /**
     * Returns the target number associated with the driving or gunnery skill for the given person when operating the
     * specified entity.
     *
     * <p>The appropriate skill type is determined based on whether the caller requests a driving skill or a gunnery
     * skill. The method then attempts to retrieve the corresponding {@link Skill} from the person.</p>
     *
     * <p>If the skill is missing, a warning is logged and the method returns the base target number for that skill
     * type plus one. This effectively penalizes entities missing an expected skill, ensuring they do not benefit from
     * an uninitialized value.</p>
     *
     * <p>If the skill exists, the method returns its final skill value after applying any relevant modifiers, but
     * never below zero.</p>
     *
     * @param person            the person whose skill value is being evaluated
     * @param entity            the entity for which the driving or gunnery skill is required
     * @param skillModifierData modifier data used to compute the final effective skill value
     * @param isDriving         {@code true} to fetch the driving skill target number; {@code false} to fetch the
     *                          gunnery skill target number
     *
     * @return the effective target number for the selected skill, adjusted for modifiers; or the base target number +1
     *       if the skill cannot be retrieved
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int getSkillTargetNumber(Person person, Entity entity,
          SkillModifierData skillModifierData, boolean isDriving) {

        String skillType = isDriving
                                 ? SkillType.getDrivingSkillFor(entity)
                                 : SkillType.getGunnerySkillFor(entity);

        Skill skill = person.getSkill(skillType);
        if (skill == null) {
            LOGGER.warn("(calculateRegularExperience) unable to fetch skill {} for {}. Skipping",
                  skillType, entity);
            return SkillType.getType(skillType).getTarget() + 1; // Returning the base target number +1
        } else {
            return Math.max(0, skill.getFinalSkillValue(skillModifierData));
        }
    }
}
