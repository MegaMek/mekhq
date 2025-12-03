/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.SkillType.S_SMALL_ARMS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.log.PerformanceLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;

/**
 * Utility class for performing Quick Training on personnel in a campaign.
 *
 * <p>Provides methods to batch-improve skills for one or more {@link Person} objects up to a desired level, using
 * campaign options and available experience points. Handles skill selection logic based on personnel roles, campaign
 * configuration, and current skill levels.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class QuickTrain {
    private static final MMLogger LOGGER = MMLogger.create(QuickTrain.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.QuickTrainDialog";

    /**
     * Processes quick training for a list of personnel, improving their applicable skills up to the specified target
     * level, according to campaign settings and XP constraints.
     *
     * @param targetPersonnel      the list of personnel (characters) to be trained
     * @param targetLevel          the minimum skill level to reach in each skill
     * @param campaign             the campaign context providing configuration and reporting support
     * @param isContinuousTraining if {@code true}, training will be repeated for each person as long as improvements
     *                             are possible and XP is available
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void processQuickTraining(List<Person> targetPersonnel, int targetLevel,
          Campaign campaign, boolean isContinuousTraining) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isAdminsHaveNegotiation = campaignOptions.isAdminsHaveNegotiation();
        boolean isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();
        boolean isTechsUseAdministration = campaignOptions.isTechsUseAdministration();
        boolean isUseArtillery = campaignOptions.isUseArtillery();
        boolean isUseSmallArmsOnly = campaignOptions.isUseSmallArmsOnly();
        boolean isUseReasoningMultiplier = campaignOptions.isUseReasoningXpMultiplier();
        double xpCostMultiplier = campaignOptions.getXpCostMultiplier();
        boolean isLogSkillGain = campaignOptions.isPersonnelLogSkillGain();
        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        boolean isClanCampaign = campaign.isClanCampaign();

        LocalDate today = campaign.getLocalDate();

        for (Person person : targetPersonnel) {
            if (person.isQuickTrainIgnore()) {
                continue;
            }

            PersonnelStatus status = person.getStatus();
            if (status.isDepartedUnit() || status.isStudent()) {
                continue;
            }

            List<String> targetSkills = new ArrayList<>();

            SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                  today, true);
            processSkills(person, isAdminsHaveNegotiation, isDoctorsUseAdministration, isTechsUseAdministration,
                  isUseArtillery, isUseSmallArmsOnly, targetSkills, skillModifierData);

            if (targetSkills.isEmpty()) {
                continue;
            }

            handleSkillTraining(targetLevel,
                  campaign,
                  isContinuousTraining,
                  person,
                  targetSkills,
                  isUseReasoningMultiplier,
                  xpCostMultiplier,
                  isLogSkillGain,
                  today);

            campaign.personUpdated(person); // Do this last so we're not spamming person update events
        }
    }

    /**
     * Trains a person's skills toward the specified target level, spending XP as needed, and processes skill logging
     * and reporting.
     *
     * <p>Iterates through the provided list of target skill names, attempting to improve each as long as the skill is
     * not at or above the target level, improvement is legal, and the person has sufficient XP. If continuous training
     * is enabled, continues training as long as at least one skill is improved in the previous iteration. Generates
     * training reports and optional logs. Trained skills are removed from the {@code targetSkills} list as they are
     * completed or found ineligible.</p>
     *
     * @param targetLevel              the minimum skill level to reach for each skill
     * @param campaign                 the campaign context for reporting
     * @param isContinuousTraining     if {@code true}, training continues as long as improvements are possible and XP
     *                                 is available
     * @param person                   the person undergoing training
     * @param targetSkills             a list of skill names to train; skills are removed from this list as they are
     *                                 completed or ineligible
     * @param isUseReasoningMultiplier whether the reasoning XP cost multiplier applies
     * @param xpCostMultiplier         an absolute multiplier applied to all skill XP costs
     * @param isLogSkillGain           whether skill gains should be logged via the performance logger
     * @param today                    the current campaign date (for logging)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void handleSkillTraining(int targetLevel, Campaign campaign, boolean isContinuousTraining,
          Person person, List<String> targetSkills, boolean isUseReasoningMultiplier, double xpCostMultiplier,
          boolean isLogSkillGain, LocalDate today) {
        do {
            boolean skillImproved = false;
            Iterator<String> targetSkillIterator = targetSkills.iterator();
            while (targetSkillIterator.hasNext()) {
                String skillName = targetSkillIterator.next();
                Skill skill = person.getSkill(skillName);

                if (skill == null || skill.getLevel() >= targetLevel) {
                    targetSkillIterator.remove();
                    continue;
                }
                if (!skill.isImprovementLegal()) {
                    targetSkillIterator.remove();
                    continue;
                }

                int improvementCost = person.getCostToImprove(skillName, isUseReasoningMultiplier);
                improvementCost = (int) Math.round(improvementCost * xpCostMultiplier);

                if (person.getXP() < improvementCost) {
                    targetSkillIterator.remove();
                    continue;
                }

                person.improveSkill(skillName);

                // Refresh skill info after improvement
                skill = person.getSkill(skillName);
                if (skill == null) {
                    // Something went wrong; the skill should have been improved
                    LOGGER.error("Failed to improve skill {} for person {}: skill was null after improvement",
                          skillName, person.getFullTitle());
                    continue;
                }

                person.spendXPOnSkills(campaign, improvementCost);

                PerformanceLogger.improvedSkill(
                      isLogSkillGain, person, today, skillName, skill.getLevel());

                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "improved.format",
                      person.getHyperlinkedName(), skillName));

                skillImproved = true;
            }

            if (!isContinuousTraining || !skillImproved) {
                break;
            }
        } while (!targetSkills.isEmpty());
    }

    /**
     * Determines the set of skills that can be trained for a given person, based on their profession(s), role-specific
     * logic, and current skill ownership/status, and sorts them by their current level.
     *
     * @param person                     the person whose skills are being considered
     * @param isAdminsHaveNegotiation    campaign option: admins substitute negotiation
     * @param isDoctorsUseAdministration campaign option: doctors substitute administration
     * @param isTechsUseAdministration   campaign option: techs substitute administration
     * @param isUseArtillery             campaign option: include artillery skills
     * @param isUseSmallArmsOnly         campaign options: infantry uses Small Arms only
     * @param targetSkills               (output) list of skill names eligible for training will be filled and sorted
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void processSkills(Person person, boolean isAdminsHaveNegotiation,
          boolean isDoctorsUseAdministration, boolean isTechsUseAdministration, boolean isUseArtillery,
          boolean isUseSmallArmsOnly, List<String> targetSkills, SkillModifierData skillModifierData) {
        fetchSkillsForProfession(isAdminsHaveNegotiation, isDoctorsUseAdministration,
              isTechsUseAdministration, isUseArtillery, isUseSmallArmsOnly, person, targetSkills,
              person.getPrimaryRole(), skillModifierData);
        fetchSkillsForProfession(isAdminsHaveNegotiation, isDoctorsUseAdministration,
              isTechsUseAdministration, isUseArtillery, isUseSmallArmsOnly, person, targetSkills,
              person.getSecondaryRole(), skillModifierData);

        if (!person.hasSkill(SkillType.S_ARTILLERY)) {
            targetSkills.remove(SkillType.S_ARTILLERY);
        }

        for (SkillType skillType : SkillType.getUtilitySkills()) {
            String skillName = skillType.getName();
            if (!targetSkills.contains(skillName) && person.hasSkill(skillName)) {
                targetSkills.add(skillName);
            }
        }

        // Sort the skills by their total skill level (lowest -> highest)
        targetSkills.sort(
              Comparator.comparingInt(skillName -> {
                  Skill skill = person.getSkill(skillName);
                  return (skill == null) // The character doesn't have this skill
                               ? Integer.MIN_VALUE // Unknown skills should always be trained first
                               : skill.getTotalSkillLevel(skillModifierData);
              })
        );
    }

    /**
     * Identifies and adds to the target skills list all relevant trainable skills for a given profession of a person,
     * observing special rules for vehicle crews and soldiers.
     *
     * @param isAdminsHaveNegotiation    campaign option: admins substitute negotiation
     * @param isDoctorsUseAdministration campaign option: doctors substitute administration
     * @param isTechsUseAdministration   campaign option: techs substitute administration
     * @param isUseArtillery             campaign option: include artillery skills
     * @param isUseSmallArmsOnly         campaign option: infantry uses Small Arms only
     * @param person                     the person whose skills are being evaluated
     * @param targetSkills               (output) list to add eligible skill names to
     * @param profession                 the personnel role/profession to check
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void fetchSkillsForProfession(boolean isAdminsHaveNegotiation, boolean isDoctorsUseAdministration,
          boolean isTechsUseAdministration, boolean isUseArtillery, boolean isUseSmallArmsOnly, Person person,
          List<String> targetSkills, PersonnelRole profession, SkillModifierData skillModifierData) {
        if (profession.isNone() || profession.isDependent()) {
            return;
        }

        switch (profession) {
            case SOLDIER -> {
                String highestSkillName = isUseSmallArmsOnly ?
                                                S_SMALL_ARMS :
                                                getHighestSkill(InfantryGunnerySkills.INFANTRY_GUNNERY_SKILLS,
                                                      person, skillModifierData);
                if (person.hasSkill(SkillType.S_ANTI_MEK)) {
                    targetSkills.add(SkillType.S_ANTI_MEK);
                }

                if (highestSkillName == null) {
                    targetSkills.addAll(PersonnelRole.SOLDIER.getSkillsForProfession(isAdminsHaveNegotiation,
                          isDoctorsUseAdministration, isTechsUseAdministration, isUseArtillery, false));
                } else {
                    targetSkills.add(highestSkillName);
                }
            }
            default -> targetSkills.addAll(profession.getSkillsForProfession(isAdminsHaveNegotiation,
                  isDoctorsUseAdministration, isTechsUseAdministration, isUseArtillery, false));
        }
    }

    /**
     * Finds the skill with the highest level for the given list of skill names on the specified person.
     *
     * @param skillNames a list of skill names to search
     * @param person     the person whose skill levels are being inspected
     *
     * @return the name of the skill with the highest level found in the person, or {@code null} if none are found
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static String getHighestSkill(List<String> skillNames, Person person, SkillModifierData skillModifierData) {
        String highestSkillName = null;
        int highestSkillLevel = -1;
        for (String skillName : skillNames) {
            Skill skill = person.getSkill(skillName);
            if (skill != null && skill.getTotalSkillLevel(skillModifierData) > highestSkillLevel) {
                highestSkillLevel = skill.getTotalSkillLevel(skillModifierData);
                highestSkillName = skillName;
            }
        }
        return highestSkillName;
    }
}
