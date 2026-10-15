/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License
 * (GPL), version 3 or (at your option) any later version, as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project; if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers creating free software for the BattleTech
 * community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or affiliated with Microsoft.
 */
package mekhq.campaign.personnel.skills;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;

/**
 * How a skill is priced by {@link SkillTrainingCosts}. Profession, Utility, and Roleplay skills share the profession
 * cost table, but each has its own improvement and recruitment cost multipliers. Every other skill uses the
 * non-profession table, at full price when improved, and shares the recruitment multiplier used for SPAs.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum SkillTrainingCategory {
    PROFESSION(CampaignOption.PROFESSION_SKILL_TRAINING_COST_MULTIPLIER,
          CampaignOption.RECRUITMENT_PROFESSION_SKILL_COST_MULTIPLIER),
    UTILITY(CampaignOption.UTILITY_SKILL_TRAINING_COST_MULTIPLIER,
          CampaignOption.RECRUITMENT_UTILITY_SKILL_COST_MULTIPLIER),
    ROLEPLAY(CampaignOption.ROLEPLAY_SKILL_TRAINING_COST_MULTIPLIER,
          CampaignOption.RECRUITMENT_ROLEPLAY_SKILL_COST_MULTIPLIER),
    OTHER(null, CampaignOption.RECRUITMENT_TRAINING_COST_MULTIPLIER);

    private final CampaignOption<Double> trainingCostMultiplierOption;
    private final CampaignOption<Double> recruitmentCostMultiplierOption;

    SkillTrainingCategory(CampaignOption<Double> trainingCostMultiplierOption,
          CampaignOption<Double> recruitmentCostMultiplierOption) {
        this.trainingCostMultiplierOption = trainingCostMultiplierOption;
        this.recruitmentCostMultiplierOption = recruitmentCostMultiplierOption;
    }

    /**
     * Gets the category of a skill. A skill belonging to the person's primary or secondary role is always a profession
     * skill, even if it is also a Utility skill.
     *
     * @param isRoleSkill whether the skill belongs to the person's primary or secondary role
     * @param skillType   the skill's type
     *
     * @return the skill's category
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static SkillTrainingCategory getCategory(boolean isRoleSkill, SkillType skillType) {
        if (isRoleSkill) {
            return PROFESSION;
        }

        if (skillType.isUtilitySkill()) {
            return UTILITY;
        }

        if (skillType.isRoleplaySkill()) {
            return ROLEPLAY;
        }

        return OTHER;
    }

    /**
     * @return {@code true} if skills in this category are priced from the profession cost table
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isUsingProfessionCostTable() {
        return this != OTHER;
    }

    /**
     * @param campaignOptions the campaign options
     *
     * @return the multiplier applied to the cost of improving a skill in this category
     *
     * @author Illiani
     * @since 0.51.01
     */
    public double getTrainingCostMultiplier(CampaignOptions campaignOptions) {
        if (trainingCostMultiplierOption == null) {
            return 1.0;
        }

        return campaignOptions.get(trainingCostMultiplierOption);
    }

    /**
     * @param campaignOptions the campaign options
     *
     * @return the multiplier applied to the training-based recruitment cost of skills in this category
     *
     * @author Illiani
     * @since 0.51.01
     */
    public double getRecruitmentCostMultiplier(CampaignOptions campaignOptions) {
        return campaignOptions.get(recruitmentCostMultiplierOption);
    }
}
