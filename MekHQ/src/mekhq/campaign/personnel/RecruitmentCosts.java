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

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillTrainingCosts;
import mekhq.utilities.spaUtilities.SpaUtilities;

/**
 * The C-bill cost of recruiting a person.
 *
 * <p>Normally a recruit costs {@link #SALARY_MONTHS} months' salary. When recruitment is paid for and skill or SPA
 * training costs C-bills, a recruit instead costs what their training would have cost: every priced experience level
 * their skills have reached (Roleplay skills excluded) if skill training costs C-bills, plus every SPA they hold if SPA
 * training costs C-bills, all multiplied by {@link CampaignOption#RECRUITMENT_TRAINING_COST_MULTIPLIER}.</p>
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
     * Gets what a recruit's training would have cost, multiplied by
     * {@link CampaignOption#RECRUITMENT_TRAINING_COST_MULTIPLIER}. Skill levels are only counted if skill training
     * costs C-bills, and SPAs only if SPA training does.
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
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Money trainingCost = Money.zero();
        if (campaignOptions.get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS)) {
            trainingCost = trainingCost.plus(SkillTrainingCosts.getAccumulatedSkillTrainingCost(campaign, person));
        }

        if (campaignOptions.get(CampaignOption.USE_SPA_TRAINING_COSTS)) {
            trainingCost = trainingCost.plus(SpaUtilities.getAccumulatedSpaTrainingCost(person));
        }

        return trainingCost.multipliedBy(campaignOptions.get(CampaignOption.RECRUITMENT_TRAINING_COST_MULTIPLIER));
    }
}
