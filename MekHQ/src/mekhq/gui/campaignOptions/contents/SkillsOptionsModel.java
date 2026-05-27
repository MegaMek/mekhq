/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import java.util.HashMap;
import java.util.Map;

import megamek.logging.MMLogger;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.skills.SkillType;

class SkillsOptionsModel {
    private static final MMLogger LOGGER = MMLogger.create(SkillsOptionsModel.class);

    private final Map<String, SkillConfiguration> skillConfigurations = new HashMap<>();
    int edgeCost;

    SkillsOptionsModel(CampaignOptions options, Map<String, SkillType> presetSkillValues) {
        loadFrom(options, presetSkillValues);
    }

    SkillConfiguration getSkillConfiguration(String skillName) {
        return skillConfigurations.get(skillName);
    }

    void loadFrom(CampaignOptions options, Map<String, SkillType> presetSkillValues) {
        Map<String, SkillType> skillValues = presetSkillValues == null ? Map.of() : presetSkillValues;
        String[] skills = SkillType.getSkillList();

        for (String skillName : skills) {
            SkillType skill = skillValues.getOrDefault(skillName, SkillType.getType(skillName));
            if (skill == null) {
                LOGGER.info("(loadValuesFromCampaignOptions) Skipping outdated or missing skill: {}", skillName);
                continue;
            }

            skillConfigurations.put(skillName, new SkillConfiguration(skill));
        }

        edgeCost = options.getEdgeCost();
    }

    void applyTo(CampaignOptions options, Map<String, SkillType> presetSkills) {
        for (final String skillName : SkillType.getSkillList()) {
            SkillType type = SkillType.getType(skillName);
            if (presetSkills != null) {
                type = presetSkills.get(skillName);
            }

            if (type == null) {
                LOGGER.info("(applyCampaignOptionsToCampaign) Skipping outdated or missing skill: {}", skillName);
                continue;
            }

            SkillType configuredType = type;
            SkillConfiguration skillConfiguration = skillConfigurations.computeIfAbsent(skillName,
                  ignored -> new SkillConfiguration(configuredType));
            skillConfiguration.applyTo(type);
        }

        options.setEdgeCost(edgeCost);
    }
}