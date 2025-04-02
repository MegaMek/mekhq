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
 */
package mekhq.campaign.rating.CamOpsReputation;

import static mekhq.campaign.randomEvents.personalities.PersonalityController.getPersonalityValue;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;

public class CommandRating {
    private static final MMLogger logger = MMLogger.create(CommandRating.class);

    /**
     * Calculates the rating of a commander based on their skills and personality.
     *
     * @param campaign  the campaign the commander belongs to
     * @param commander the commander to calculate the rating for
     * @return a map containing the commander's rating in different areas:
     *         - "leadership": the commander's leadership skill value
     *         - "tactics": the commander's tactics skill value
     *         - "strategy": the commander's strategy skill value
     *         - "negotiation": the commander's negotiation skill value
     *         - "traits": the commander's traits (not currently tracked, always 0)
     *         - "personality": the value of the commander's personality
     *         characteristics (or 0, if disabled)
     */
    protected static Map<String, Integer> calculateCommanderRating(Campaign campaign, Person commander) {
        Map<String, Integer> commandRating = new HashMap<>();

        commandRating.put("leadership", getSkillValue(commander, SkillType.S_LEADER));
        commandRating.put("tactics", getSkillValue(commander, SkillType.S_TACTICS));
        commandRating.put("strategy", getSkillValue(commander, SkillType.S_STRATEGY));
        commandRating.put("negotiation", getSkillValue(commander, SkillType.S_NEG));

        // ATOW traits are not currently tracked by mhq, but when they are, this is
        // where we'd add that data
        commandRating.put("traits", 0);

        int personalityValue = 0;
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        if (campaignOptions.isUseRandomPersonalityReputation() && commander != null) {
            personalityValue = getPersonalityValue(campaignOptions.isUseRandomPersonalities(),
                commander.getAggression(), commander.getAmbition(), commander.getGreed(),
                commander.getSocial());
        }
        commandRating.put("personality", personalityValue);

        commandRating.put("total", commandRating.values().stream().mapToInt(rating -> rating).sum());

        logger.debug("Command Rating = {}",
                commandRating.keySet().stream()
                        .map(key -> key + ": " + commandRating.get(key) + '\n')
                        .collect(Collectors.joining()));

        return commandRating;
    }

    /**
     * @return the final skill value for the given skill,
     *         or 0 if the person does not have the skill
     *
     * @param person the person
     * @param skill  the skill
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
