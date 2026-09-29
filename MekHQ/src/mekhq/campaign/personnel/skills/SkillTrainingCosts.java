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

import static mekhq.campaign.personnel.skills.SkillType.EXP_LEGENDARY;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.chaosCampaign.ChaosCampaignUtilities;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * C-bill costs of skill improvements, covering training materials and sim pod time. Unofficial, following the table in
 * Hot Spots: Draconis Reach first printing pg 34.
 *
 * <p>Hot Spots skill levels are mapped onto experience levels: Regular = 4, Veteran = 3, Elite = 2, Heroic = 1,
 * Legendary = 0. An improvement is only charged when it raises the skill into a new, priced experience level; Green and
 * Ultra-Green cost nothing.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class SkillTrainingCosts {
    private SkillTrainingCosts() {
    }

    /**
     * Costs for profession, utility, and roleplay skills, indexed by experience level.
     */
    private static final int[] PROFESSION_COSTS_IN_SUPPORT_POINTS = { 0, 0, 100, 200, 700, 1200, 1200 };

    /**
     * Costs for all other skills, indexed by experience level.
     */
    private static final int[] NON_PROFESSION_COSTS_IN_SUPPORT_POINTS = { 0, 0, 100, 300, 700, 1200, 2200 };

    /**
     * Gets the C-bill cost of improving a person's skill by one level.
     *
     * @param campaign  the campaign, for the profession skill options
     * @param person    the person improving the skill
     * @param skillName the name of the skill being improved (or gained)
     *
     * @return the cost, or zero if the option is disabled or the improvement doesn't reach a new priced experience
     *       level
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getSkillImprovementCost(Campaign campaign, Person person, String skillName) {
        if (!campaign.getCampaignOptions().get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS)) {
            return Money.zero();
        }

        SkillType skillType = SkillType.getType(skillName);
        if (skillType == null) {
            return Money.zero();
        }

        Skill skill = person.getSkill(skillName);
        int currentExperienceLevel = skill == null ? -1 : skillType.getExperienceLevel(skill.getLevel());
        int nextLevel = skill == null ? 0 : skill.getLevel() + 1;
        int nextExperienceLevel = skillType.getExperienceLevel(nextLevel);

        if (nextExperienceLevel <= currentExperienceLevel || nextExperienceLevel < EXP_REGULAR) {
            return Money.zero();
        }

        int[] costTable = isProfessionSkill(campaign, person, skillType)
                                ? PROFESSION_COSTS_IN_SUPPORT_POINTS
                                : NON_PROFESSION_COSTS_IN_SUPPORT_POINTS;
        int cappedExperienceLevel = Math.min(nextExperienceLevel, EXP_LEGENDARY);

        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(costTable[cappedExperienceLevel]);
    }

    /**
     * A skill is a profession skill if it belongs to the person's primary or secondary role, or is a utility or
     * roleplay skill.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isProfessionSkill(Campaign campaign, Person person, SkillType skillType) {
        if (skillType.isUtilitySkill() || skillType.isRoleplaySkill()) {
            return true;
        }

        String skillName = skillType.getName();
        return isRoleSkill(campaign, person.getPrimaryRole(), skillName)
                     || isRoleSkill(campaign, person.getSecondaryRole(), skillName);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isRoleSkill(Campaign campaign, PersonnelRole role, String skillName) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        List<String> roleSkills = role.getSkillsForProfession(campaignOptions.get(CampaignOption.ADMINS_HAVE_NEGOTIATION),
              campaignOptions.get(CampaignOption.DOCTORS_USE_ADMINISTRATION),
              campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION),
              campaignOptions.get(CampaignOption.USE_ARTILLERY),
              true);
        return roleSkills.contains(skillName);
    }
}
