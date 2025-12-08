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

import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.personnel.skills.SkillType.S_APPRAISAL;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.personnel.skills.SkillType.S_SMALL_ARMS;
import static mekhq.campaign.personnel.skills.SkillType.S_STRATEGY;
import static mekhq.campaign.personnel.skills.SkillType.S_TACTICS;
import static mekhq.campaign.personnel.skills.SkillType.S_TRAINING;
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
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;

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
        // Should we train Negotiation for Admins?
        boolean isAdminsHaveNegotiation = campaignOptions.isAdminsHaveNegotiation();
        // Should we train Administration for Techs and Doctors?
        boolean isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();
        boolean isTechsUseAdministration = campaignOptions.isTechsUseAdministration();
        // Should we train Artillery on combat personnel characters who already have it?
        boolean isUseArtillery = campaignOptions.isUseArtillery();
        // Should soldiers only train Small Arms?
        boolean isUseSmallArmsOnly = campaignOptions.isUseSmallArmsOnly();
        // Should we train command utility & training skills?
        boolean isUseStratCon = campaignOptions.isUseStratCon();
        // Should we train scouting skills on combat personnel who already have them?
        boolean isUseAdvancedScouting = isUseStratCon && campaignOptions.isUseAdvancedScouting();
        // Should we train escape skills on personnel who already have them?
        boolean isUseEscapeSkills = campaignOptions.isUseFunctionalEscapeArtist();
        // Should we train appraisal on procurement personnel?
        boolean isUseAppraisal = campaignOptions.isUseFunctionalAppraisal();
        ProcurementPersonnelPick procurementPersonnel = campaignOptions.getAcquisitionPersonnelCategory();
        // Should we train Leadership?
        boolean isUseManagementSkill = campaignOptions.isUseRandomRetirement() &&
                                             campaignOptions.isUseManagementSkill();

        // Do XP costs need to be adjusted?
        boolean isUseReasoningMultiplier = campaignOptions.isUseReasoningXpMultiplier();
        double xpCostMultiplier = campaignOptions.getXpCostMultiplier();

        // Are we logging skill gain in the personnel logs?
        boolean isLogSkillGain = campaignOptions.isPersonnelLogSkillGain();

        // These are used to determining the current total skill level? Used when prioritizing skill training
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
                  isUseArtillery, isUseSmallArmsOnly, isUseStratCon, isUseAdvancedScouting, isUseEscapeSkills,
                  isUseAppraisal, procurementPersonnel, isUseManagementSkill, targetSkills, skillModifierData);

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

                campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "improved.format",
                      person.getHyperlinkedName(), skillName));

                skillImproved = true;
            }

            if (!isContinuousTraining || !skillImproved) {
                break;
            }
        } while (!targetSkills.isEmpty());
    }

    /**
     * Populates and sorts the list of target skills for the given {@link Person} based on their roles, existing skills,
     * and various campaign settings.
     *
     * <p>This method:
     * <ul>
     *     <li>Adds skills associated with the person's primary and secondary roles via
     *     {@code fetchSkillsForProfession}, honoring the administration/negotiation and artillery/small-arms
     *     flags.</li>
     *     <li>Removes artillery from the target list if the person does not have the {@code S_ARTILLERY} skill.</li>
     *     <li>Optionally adds StratCon-related skills (training, tactics, leadership, strategy) for combat personnel
     *     when StratCon rules are enabled.</li>
     *     <li>Optionally adds the best scouting skill for combat personnel when advanced scouting rules are
     *     enabled.</li>
     *     <li>Optionally adds the best escape skill for combat personnel when escape skills are enabled.</li>
     *     <li>Optionally adds the appraisal skill based on the procurement personnel setting and whether the person
     *     is support or logistics-focused.</li>
     *     <li>Sorts {@code targetSkills} in ascending order of total skill level, so that lower (worse) skills and
     *     unknown skills are prioritized first.</li>
     * </ul>
     *
     * <p>The {@code targetSkills} list is modified in place; skills may be added, removed, and finally reordered.
     *
     * @param person                     the person whose skills and roles are being evaluated
     * @param isAdminsHaveNegotiation    {@code true} if administrators should use negotiation instead of administration
     *                                   for their profession-based skill picks
     * @param isDoctorsUseAdministration {@code true} if doctors should use administration instead of medical-specific
     *                                   skills for their profession-based picks
     * @param isTechsUseAdministration   {@code true} if technicians should use administration instead of
     *                                   technical-specific skills for their profession-based picks
     * @param isUseArtillery             {@code true} if artillery skills should be considered when building the target
     *                                   skill list
     * @param isUseSmallArmsOnly         {@code true} if only small-arms combat skills should be considered for combat
     *                                   roles instead of heavier weapon skills
     * @param isUseStratCon              {@code true} to add StratCon-related skills (training, tactics, leadership,
     *                                   strategy) for combat personnel
     * @param isUseAdvancedScouting      {@code true} to consider and add the best scouting skill for combat personnel
     * @param isUseEscapeSkills          {@code true} to consider and add the best escape skill for combat personnel
     * @param isUseAppraisal             {@code true} to consider adding the appraisal skill based on the
     *                                   {@code procurementPersonnel} policy
     * @param procurementPersonnel       the policy that determines which personnel (none, all, support, or logistics)
     *                                   are eligible to receive appraisal training
     * @param isUseManagementSkill       {@code true} to consider and add the leadership skill
     * @param targetSkills               the mutable list of skill IDs to populate and then sort
     * @param skillModifierData          the modifier data used to compute each skill's total effective level when
     *                                   ordering {@code targetSkills}
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void processSkills(Person person, boolean isAdminsHaveNegotiation,
          boolean isDoctorsUseAdministration, boolean isTechsUseAdministration, boolean isUseArtillery,
          boolean isUseSmallArmsOnly, boolean isUseStratCon, boolean isUseAdvancedScouting, boolean isUseEscapeSkills,
          boolean isUseAppraisal, ProcurementPersonnelPick procurementPersonnel, boolean isUseManagementSkill,
          List<String> targetSkills, SkillModifierData skillModifierData) {
        Skills personSkills = person.getSkills();
        boolean isCombatPersonnel = person.isCombat();

        fetchSkillsForProfession(isAdminsHaveNegotiation, isDoctorsUseAdministration,
              isTechsUseAdministration, isUseArtillery, isUseSmallArmsOnly, person, targetSkills,
              person.getPrimaryRole(), skillModifierData);
        fetchSkillsForProfession(isAdminsHaveNegotiation, isDoctorsUseAdministration,
              isTechsUseAdministration, isUseArtillery, isUseSmallArmsOnly, person, targetSkills,
              person.getSecondaryRole(), skillModifierData);

        if (!personSkills.hasSkill(SkillType.S_ARTILLERY)) {
            targetSkills.remove(SkillType.S_ARTILLERY);
        }

        if (isUseStratCon) {
            if (isCombatPersonnel) {
                if (shouldAddSkill(personSkills, S_TRAINING, targetSkills)) {
                    targetSkills.add(S_TRAINING);
                }
                if (shouldAddSkill(personSkills, S_TACTICS, targetSkills)) {
                    targetSkills.add(S_TACTICS);
                }
                if (shouldAddSkill(personSkills, S_LEADER, targetSkills)) {
                    targetSkills.add(S_LEADER);
                }
                if (shouldAddSkill(personSkills, S_STRATEGY, targetSkills)) {
                    targetSkills.add(S_STRATEGY);
                }
            }
        }

        if (isCombatPersonnel && isUseAdvancedScouting) {
            String bestSkill = ScoutingSkills.getBestScoutingSkill(person);
            if (shouldAddSkill(personSkills, bestSkill, targetSkills)) {
                targetSkills.add(bestSkill);
            }
        }

        if (isCombatPersonnel && isUseEscapeSkills) {
            String bestSkill = EscapeSkills.getHighestEscapeSkill(person);
            if (shouldAddSkill(personSkills, bestSkill, targetSkills)) {
                targetSkills.add(bestSkill);
            }
        }

        if (isUseAppraisal) {
            switch (procurementPersonnel) {
                case NONE -> {}
                case ALL -> {
                    if (shouldAddSkill(personSkills, S_APPRAISAL, targetSkills)) {
                        targetSkills.add(S_APPRAISAL);
                    }
                }
                case SUPPORT -> {
                    if (person.isSupport() && shouldAddSkill(personSkills, S_APPRAISAL, targetSkills)) {
                        targetSkills.add(S_APPRAISAL);
                    }
                }
                case LOGISTICS -> {
                    boolean isLogisticsCharacter = person.getPrimaryRole().isAdministratorLogistics() ||
                                                         person.getSecondaryRole().isAdministratorLogistics();
                    if (isLogisticsCharacter && shouldAddSkill(personSkills, S_APPRAISAL, targetSkills)) {
                        targetSkills.add(S_APPRAISAL);
                    }
                }
            }
        }

        if (isUseManagementSkill) {
            if (shouldAddSkill(personSkills, S_LEADER, targetSkills)) {
                targetSkills.add(S_LEADER);
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
     * Determines whether a skill should be added to the target skill list.
     *
     * <p>A skill is eligible to be added only if:
     * <ul>
     *     <li>It is not present in {@code targetSkills}; and</li>
     *     <li>The person actually possesses the skill, as indicated by {@link Skills#hasSkill(String)}.</li>
     * </ul>
     *
     * @param personSkills the skill set of the person being evaluated
     * @param skillName    the internal skill identifier to check
     * @param targetSkills the list of target skills being populated
     *
     * @return {@code true} if the skill exists on the person and is not already in {@code targetSkills}
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static boolean shouldAddSkill(Skills personSkills, String skillName, List<String> targetSkills) {
        return !targetSkills.contains(skillName) && personSkills.hasSkill(skillName);
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
