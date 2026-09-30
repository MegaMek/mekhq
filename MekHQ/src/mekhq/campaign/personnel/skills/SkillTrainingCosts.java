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
     * @param campaign  the campaign, for the profession skill and skill modifier options
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
        SkillType skillType = SkillType.getType(skillName);
        if (skillType == null || !campaign.getCampaignOptions().get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS)) {
            return Money.zero();
        }

        boolean isRoleSkill = isRoleSkill(campaign, person.getPrimaryRole(), skillName)
                                    || isRoleSkill(campaign, person.getSecondaryRole(), skillName);
        return getSkillImprovementCost(campaign, person, skillName, isRoleSkill,
              getSkillModifierData(campaign, person));
    }

    /**
     * As {@link #getSkillImprovementCost(Campaign, Person, String)}, for callers that have already worked out the
     * person's role skills and skill modifiers, such as a menu listing every skill.
     *
     * @param campaign          the campaign
     * @param person            the person improving the skill
     * @param skillName         the name of the skill being improved (or gained)
     * @param isRoleSkill       whether the skill belongs to the person's primary or secondary role
     * @param skillModifierData the person's skill modifiers, which decide the skill's experience level
     *
     * @return the cost, or zero if the option is disabled or the improvement doesn't reach a new priced experience
     *       level
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getSkillImprovementCost(Campaign campaign, Person person, String skillName,
          boolean isRoleSkill, SkillModifierData skillModifierData) {
        if (!campaign.getCampaignOptions().get(CampaignOption.SKILL_IMPROVEMENTS_COST_C_BILLS)) {
            return Money.zero();
        }

        SkillType skillType = SkillType.getType(skillName);
        if (skillType == null) {
            return Money.zero();
        }

        // Use the level including modifiers, as shown to the player; improving the skill raises it by one
        Skill skill = person.getSkill(skillName);
        int currentLevel = skill == null ? -1 : skill.getTotalSkillLevel(skillModifierData);
        int currentExperienceLevel = skill == null ? -1 : skillType.getExperienceLevel(currentLevel);
        int nextExperienceLevel = skillType.getExperienceLevel(currentLevel + 1);

        if (nextExperienceLevel <= currentExperienceLevel || nextExperienceLevel < EXP_REGULAR) {
            return Money.zero();
        }

        boolean isProfessionSkill = isRoleSkill || skillType.isUtilitySkill() || skillType.isRoleplaySkill();
        int[] costTable = isProfessionSkill
                                ? PROFESSION_COSTS_IN_SUPPORT_POINTS
                                : NON_PROFESSION_COSTS_IN_SUPPORT_POINTS;
        int cappedExperienceLevel = Math.min(nextExperienceLevel, EXP_LEGENDARY);

        return ChaosCampaignUtilities.getMoneyFromChaosSupportPoints(costTable[cappedExperienceLevel]);
    }

    /**
     * Gets the combined C-bill cost of every priced experience level a person's skills have reached, as though each
     * skill had been trained up from scratch. Used to price recruits. Roleplay skills are left out; profession and
     * utility skills use the profession table. Unlike {@link #getSkillImprovementCost(Campaign, Person, String)},
     * this doesn't check {@link CampaignOption#SKILL_IMPROVEMENTS_COST_C_BILLS}; the caller decides whether it applies.
     *
     * @param campaign the campaign, for the profession skill and skill modifier options
     * @param person   the person whose skills are priced
     *
     * @return the combined cost of the person's skill levels
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Money getAccumulatedSkillTrainingCost(Campaign campaign, Person person) {
        SkillModifierData skillModifierData = getSkillModifierData(campaign, person);
        List<String> roleSkills = getRoleSkills(campaign, person.getPrimaryRole());
        roleSkills.addAll(getRoleSkills(campaign, person.getSecondaryRole()));

        long totalSupportPoints = 0;
        for (String skillName : person.getSkills().getSkillNames()) {
            SkillType skillType = SkillType.getType(skillName);
            Skill skill = person.getSkill(skillName);
            if (skillType == null || skill == null || skillType.isRoleplaySkill()) {
                continue;
            }

            boolean isProfessionSkill = roleSkills.contains(skillName) || skillType.isUtilitySkill();
            int[] costTable = isProfessionSkill
                                    ? PROFESSION_COSTS_IN_SUPPORT_POINTS
                                    : NON_PROFESSION_COSTS_IN_SUPPORT_POINTS;
            int experienceLevel = Math.min(
                  skillType.getExperienceLevel(skill.getTotalSkillLevel(skillModifierData)), EXP_LEGENDARY);
            for (int level = EXP_REGULAR; level <= experienceLevel; level++) {
                totalSupportPoints += costTable[level];
            }
        }

        // Converted here, as a long, rather than through ChaosCampaignUtilities, whose int maths could overflow
        return Money.of((double) totalSupportPoints * ChaosCampaignUtilities.SUPPORT_POINTS_TO_MONEY_CONVERSION);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static SkillModifierData getSkillModifierData(Campaign campaign, Person person) {
        return person.getSkillModifierData(campaign.getCampaignOptions().get(CampaignOption.USE_AGE_EFFECTS),
              campaign.getPlayerForce().isClanForce(),
              campaign.getLocalDate());
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isRoleSkill(Campaign campaign, PersonnelRole role, String skillName) {
        return getRoleSkills(campaign, role).contains(skillName);
    }

    /**
     * @return a modifiable list of the skills belonging to the role
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static List<String> getRoleSkills(Campaign campaign, PersonnelRole role) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        return role.getSkillsForProfession(
              campaignOptions.get(CampaignOption.ADMINS_HAVE_NEGOTIATION),
              campaignOptions.get(CampaignOption.DOCTORS_USE_ADMINISTRATION),
              campaignOptions.get(CampaignOption.TECHS_USE_ADMINISTRATION),
              campaignOptions.get(CampaignOption.USE_ARTILLERY),
              true);
    }
}
