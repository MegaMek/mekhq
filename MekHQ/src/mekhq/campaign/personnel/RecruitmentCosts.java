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
package mekhq.campaign.personnel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillTrainingCategory;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.utilities.spaUtilities.SpaUtilities;

/**
 * The C-bill cost of recruiting a person.
 *
 * <p>Normally a recruit costs {@link #SALARY_MONTHS} months' salary. When recruitment is paid for and skill or SPA
 * training costs C-bills, a recruit instead costs what their training would have cost: every priced experience level
 * their skills have reached if skill training costs C-bills, plus every SPA they hold if SPA training costs C-bills.
 * Profession, Utility, and Roleplay skills each have their own multiplier (Roleplay defaults to zero, leaving those
 * skills out); SPAs are multiplied by {@link CampaignOption#RECRUITMENT_TRAINING_COST_MULTIPLIER}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class RecruitmentCosts {
    /** Months of salary a recruit normally costs */
    public static final int SALARY_MONTHS = 2;

    private RecruitmentCosts() {
    }

    /**
     * @param campaignOptions the campaign options
     *
     * @return {@code true} if recruits are priced by their training rather than their salary
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isUsingTrainingBasedCost(CampaignOptions campaignOptions) {
        return campaignOptions.get(CampaignOption.PAY_FOR_RECRUITMENT)
                     && (campaignOptions.get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS)
                               || campaignOptions.get(CampaignOption.USE_SPA_TRAINING_COSTS));
    }

    /**
     * Gets the cost of recruiting a person: their training-based cost if {@link #isUsingTrainingBasedCost} applies,
     * otherwise {@link #SALARY_MONTHS} months' salary.
     *
     * @param campaign the campaign
     * @param person   the recruit
     *
     * @return the recruitment cost
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getRecruitmentCost(Campaign campaign, Person person) {
        if (isUsingTrainingBasedCost(campaign.getCampaignOptions())) {
            return getTrainingBasedCost(campaign, person);
        }

        return person.getSalary(campaign.getCampaignOptions(),
                    campaign.getPlayerForce().isClanForce(),
                    campaign.getLocalDate())
                     .multipliedBy(SALARY_MONTHS);
    }

    /**
     * Gets what a recruit's training would have cost. Each {@link SkillTrainingCategory} of skills is multiplied by its
     * own recruitment multiplier; SPAs are multiplied by {@link CampaignOption#RECRUITMENT_TRAINING_COST_MULTIPLIER}.
     * Skill levels are only counted if skill training costs C-bills, and SPAs only if SPA training does.
     *
     * @param campaign the campaign
     * @param person   the recruit
     *
     * @return the training-based recruitment cost
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getTrainingBasedCost(Campaign campaign, Person person) {
        return getTrainingBasedCostBreakdown(campaign, person).total();
    }

    /**
     * As {@link #getTrainingBasedCost(Campaign, Person)}, split into the cost of each {@link SkillTrainingCategory} of
     * skills and of SPAs, with their recruitment multipliers already applied.
     *
     * @param campaign the campaign
     * @param person   the recruit
     *
     * @return the training-based recruitment cost, by part
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static TrainingCostBreakdown getTrainingBasedCostBreakdown(Campaign campaign, Person person) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Map<SkillTrainingCategory, Money> skillCosts = new EnumMap<>(SkillTrainingCategory.class);
        for (SkillTrainingCategory category : SkillTrainingCategory.values()) {
            skillCosts.put(category, Money.zero());
        }

        if (campaignOptions.get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS)) {
            Map<SkillTrainingCategory, Money> skillTrainingCosts =
                  SkillTrainingCosts.getAccumulatedSkillTrainingCosts(campaign, person);
            for (Map.Entry<SkillTrainingCategory, Money> entry : skillTrainingCosts.entrySet()) {
                double multiplier = entry.getKey().getRecruitmentCostMultiplier(campaignOptions);
                skillCosts.put(entry.getKey(), entry.getValue().multipliedBy(multiplier));
            }
        }

        Money spaCost = Money.zero();
        if (campaignOptions.get(CampaignOption.USE_SPA_TRAINING_COSTS)) {
            double multiplier = campaignOptions.get(CampaignOption.RECRUITMENT_TRAINING_COST_MULTIPLIER);
            spaCost = SpaUtilities.getAccumulatedSpaTrainingCost(person).multipliedBy(multiplier);
        }

        return new TrainingCostBreakdown(skillCosts, spaCost);
    }

    /**
     * A training-based recruitment cost, split by part. Recruitment multipliers have already been applied.
     *
     * @param skillCosts the cost of the recruit's skills in each {@link SkillTrainingCategory}; every category is
     *                   present
     * @param spaCost    the cost of the recruit's SPAs
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record TrainingCostBreakdown(Map<SkillTrainingCategory, Money> skillCosts, Money spaCost) {
        /**
         * @return the combined cost of every part
         */
        public Money total() {
            return spaCost.plus(List.copyOf(skillCosts.values()));
        }
    }
}
