/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

public class CommandRating {
    private static final MMLogger logger = MMLogger.create(CommandRating.class);

    /**
     * Calculates the rating of a commander based on their skills, traits, and (optionally) personality.
     * Follows the rules outlined in CamOps (revised 2021) pages 33-34.
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
     *           <li>"traits": the commander's trait score</li>
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
              getATOWTraitScore(commander,
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
     * Calculates the ATOW (A Time of War) trait score for the provided commander.
     *
     * <p>The score one point for each positive trait and loses one point for each negative trait.</p>
     *
     * <p>Positive traits:
     *     <ul>
     *         <li>"Combat Sense"</li>
     *         <li>Connections > 0</li>
     *         <li>Reputation > 0</li>
     *         <li>Wealth >= 7</li>
     *         <li>Charisma >= 7</li>
     *     </ul>
     * </p>
     *
     * <p>Negative traits:
     *     <ul>
     *         <li>"Combat Paralysis"</li>
     *         <li>Reputation < 0</li>
     *         <li>Unlucky > 0</li>
     *         <li>Charisma <= 3</li>
     *     </ul>
     * </p>
     *
     * @param commander The {@link Person} representing the commander whose trait values need to be calculated. Can be
     *                  {@code null}, in which case the output is 0.
     *
     * @return The calculated trait score for the commander, with a minimum value of 1.
     */
    private static int getATOWTraitScore(Person commander, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        if (commander == null) {
            return 0;
        }

        int traitScore = 0;
        PersonnelOptions options = commander.getOptions();

        // Connections
        int connections = commander.getAdjustedConnections(false);
        if (connections > 0) {
            traitScore += 1;
        }

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

        // Charisma
        int charisma = commander.getAttributeScore(SkillAttribute.CHARISMA);
        if (charisma <= 3) {
            traitScore -= 1;
        } else if (charisma >= 7) {
            traitScore += 1;
        }

        // Unlucky
        if (commander.getUnlucky() > 0) {
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
