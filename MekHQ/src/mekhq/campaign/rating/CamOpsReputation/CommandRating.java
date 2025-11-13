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
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_PARALYSIS;
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_SENSE;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.getPersonalityValue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.skills.SkillType;

public class CommandRating {
    private static final MMLogger logger = MMLogger.create(CommandRating.class);

    /**
     * Calculates the rating of a commander based on their skills and personality.
     *
     * @param campaign  the campaign the commander belongs to
     * @param commander the commander to calculate the rating for
     *
     * @return A map containing the commander's rating in different areas:
     *       <ul>
     *           <li>"leadership": the commander's leadership skill value</li>
     *           <li>"tactics": the commander's tactics skill value</li>
     *           <li>"strategy": the commander's strategy skill value</li>
     *           <li>"negotiation": the commander's negotiation skill value</li>
     *           <li>"traits": the commander's trait score (see CamOps pg34)</li>
     *           <li>"personality": the value of the commander's personality characteristics (or 0, if disabled)</li>
     *       </ul>
     */
    protected static Map<String, Integer> calculateCommanderRating(Campaign campaign, Person commander) {
        Map<String, Integer> commandRating = new HashMap<>();

        commandRating.put("leadership", getSkillValue(commander, SkillType.S_LEADER));
        commandRating.put("tactics", getSkillValue(commander, SkillType.S_TACTICS));
        commandRating.put("strategy", getSkillValue(commander, SkillType.S_STRATEGY));
        commandRating.put("negotiation", getSkillValue(commander, SkillType.S_NEGOTIATION));

        commandRating.put("traits",
              getATOWTraitValues(commander,
                    campaign.getCampaignOptions().isUseAgeEffects(),
                    campaign.isClanCampaign(),
                    campaign.getLocalDate()));

        int personalityValue = 0;
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        if (campaignOptions.isUseRandomPersonalityReputation() && commander != null) {
            personalityValue = getPersonalityValue(campaignOptions.isUseRandomPersonalities(),
                  commander.getAggression(),
                  commander.getAmbition(),
                  commander.getGreed(),
                  commander.getSocial());
        }
        commandRating.put("personality", personalityValue);

        // CamOps pg34 states any score less than 1 is treated as 1
        commandRating.put("total", max(1, commandRating.values().stream().mapToInt(rating -> rating).sum()));

        logger.debug("Command Rating = {}",
              commandRating.keySet()
                    .stream()
                    .map(key -> key + ": " + commandRating.get(key) + '\n')
                    .collect(Collectors.joining()));

        return commandRating;
    }

    /**
     * Calculates the ATOW (A Time of War) trait values for the provided commander.
     *
     * <p>The trait score is determined by summing various attribute scores, including connection levels,
     * combat-related traits, wealth, and reputation. The score has a minimum value of 1 regardless of calculated
     * modifiers.</p>
     *
     * <p>Trait scoring includes:
     * <ul>
     *   <li>Connections: Adds a score based on connection levels ranging from 1 to 10.</li>
     *   <li>Combat Traits: Adds or subtracts points based on "Combat Sense" (+1) and "Combat Paralysis" (-1).</li>
     *   <li>Wealth: Adds 1 point if the commander has sufficient wealth.</li>
     *   <li>Reputation: Adds or subtracts scores based on positive and negative reputation levels.</li>
     * </ul>
     *
     * @param commander The {@link Person} representing the commander whose trait values need to be calculated. Can be
     *                  {@code null}, in which case the output is 0.
     *
     * @return The calculated trait score for the commander, with a minimum value of 1.
     */
    private static int getATOWTraitValues(Person commander, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        if (commander == null) {
            return 0;
        }

        int traitScore = 0;
        PersonnelOptions options = commander.getOptions();

        // Connections
        traitScore += commander.getAdjustedConnections(false);

        // Wealth
        traitScore += commander.getWealth() >= 7 ? 1 : 0;

        // Reputation
        int reputation = commander.getAdjustedReputation(isUseAgingEffects,
              isClanCampaign,
              today,
              commander.getRankNumeric());
        if (reputation < 0) {
            traitScore -= 1;
        } else if (reputation > 0) {
            traitScore += 1;
        }

        // Combat-related traits
        if (options.booleanOption(ATOW_COMBAT_SENSE)) {
            traitScore += 1;
        }

        if (options.booleanOption(ATOW_COMBAT_PARALYSIS)) {
            traitScore -= 1;
        }

        return traitScore;
    }

    /**
     * @param person the person
     * @param skill  the skill
     *
     * @return the final skill value for the given skill, or 0 if the person does not have the skill
     */
    private static int getSkillValue(Person person, String skill) {
        int skillValue = 0;

        if (person == null) {
            return 0;
        }

        if (person.hasSkill(skill)) {
            skillValue += person.getSkill(skill).getLevel();
            skillValue += person.getSkill(skill).getBonus();
        } else {
            return 0;
        }

        return skillValue;
    }
}
