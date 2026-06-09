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
package mekhq.campaign.personnel.education;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.PERSONNEL;
import static mekhq.campaign.enums.DailyReportType.SKILL_CHECKS;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;
import static mekhq.campaign.personnel.skills.SkillType.S_TRAINING;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessObject;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.log.PerformanceLogger;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.InfantryGunnerySkills;
import mekhq.campaign.personnel.skills.ScoutingSkills;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;
import org.jspecify.annotations.NonNull;

/**
 * Manages the daily training progression of combat teams and their personnel within a campaign.
 *
 * <p>Each week that a combat team is assigned a training role, this class drives the full
 * training pipeline: validating eligibility, resolving the educator's skill check, distributing XP progress to
 * trainees, and advancing skills when the improvement threshold is met. The result of each session is surfaced as
 * entries in the campaign's daily report and, optionally, each affected person's personnel log.</p>
 *
 * <h2>Training Resolution</h2>
 * <ol>
 *   <li>The combat team's commander makes a {@link SkillType#S_TRAINING}
 *       skill check. The raw margin of success drives the amount of XP awarded this session.</li>
 *   <li>A margin at or below {@link MarginOfSuccess#BARELY_MADE_IT}
 *       produces no XP for any trainee.</li>
 *   <li>Above that threshold, each trainee's target skill receives {@code max(1, marginOfSuccess)} XP.
 *       Accumulated XP persists across sessions until the improvement cost is met.</li>
 *   <li>When accumulated XP meets or exceeds the cost to improve (adjusted by
 *       {@link CampaignOptions#getXpCostMultiplier()} and optional
 *       reasoning-based adjustments), the skill advances one level and veterancy awards are evaluated.</li>
 * </ol>
 *
 * <h2>Entry Point</h2>
 * <p>Call {@link #processTrainingCombatTeams(Campaign)} once per day to run the full pipeline for
 * all eligible combat teams in the campaign. All other methods in this class are internal
 * implementation details.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class TrainingCombatTeams {
    private static final MMLogger LOGGER = MMLogger.create(TrainingCombatTeams.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Education";

    private static final int EXPERIENCE_LEVEL_REDUCTION = -1;
    static final int XP_RATE_BASE_LINE = 1;

    /**
     * Processes all training combat teams in the campaign.
     *
     * <p>This method iterates through all combat teams in the campaign and processes training for
     * those whose role includes training. It ensures that combat teams are eligible for training by checking that their
     * contracts are active and valid on the current date. If StratCon is used, it also verifies that the teams are
     * deployed in their appropriate sectors.</p>
     *
     * @param campaign the {@link Campaign} instance managing the combat teams and their operations
     */
    public static void processTrainingCombatTeams(final Campaign campaign) {
        final LocalDate today = campaign.getLocalDate();
        final List<CombatTeam> combatTeams = campaign.getCombatTeamsAsList();

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUsingStratCon = campaignOptions.isUseStratCon();
        boolean isUsingMaplessMode = campaignOptions.isUseStratConMaplessMode();

        for (CombatTeam combatTeam : combatTeams) {
            if (!combatTeam.getRole().isTraining()) {
                continue;
            }

            AtBContract contract = combatTeam.getContract(campaign);
            if (contract == null || !contract.isActiveOn(today, false)) {
                continue;
            }

            StratConCampaignState campaignState = contract.getStratconCampaignState();
            boolean isForceDeployed = campaignState != null &&
                                            campaignState.isForceDeployedHere(combatTeam.getFormationId());
            if (isUsingStratCon) {
                if (!isUsingMaplessMode && !isForceDeployed) {
                    continue;
                }
            }

            Formation formation = combatTeam.getFormation(campaign);
            List<Formation> allFormations = new ArrayList<>();
            allFormations.add(formation); // We want to include the force itself in the training process
            allFormations.addAll(formation.getAllSubFormations());

            for (Formation trainingFormation : allFormations) {
                processTraining(campaign, trainingFormation);
            }
        }
    }

    /**
     * Handles the training progression for an individual combat team.
     *
     * <p>This method identifies the combat team's commander and educators within the unit,
     * collects their skills, and compares them to the skills of the trainees in the team. Eligible trainees undergo
     * training to improve their skills, which is simulated through education time tracking and skill level
     * progression.</p>
     *
     * <p>If educators or trainees lack eligible skills, appropriate reports are generated.
     * Training updates for skills and progression are logged within the campaign.</p>
     *
     * @param campaign  the {@link Campaign} managing the combat team and its associated personnel
     * @param formation the {@link Formation} undergoing training during this session
     */
    private static void processTraining(final Campaign campaign, final Formation formation) {
        // If the force is empty, we skip it
        Vector<UUID> units = formation.getUnits(); // We only want units in the direct force, not child forces
        if (units.isEmpty()) {
            LOGGER.info("No units in force '{}' for campaign '{}'", formation.getName(), campaign.getName());
            return;
        }

        // Identify the Combat Team's commander (i.e., the Trainer)
        UUID commanderID = formation.getFormationCommanderID();
        Person commander = campaign.getPerson(commanderID);

        if (commander == null) {
            campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "noCommander.text", formation.getName(),
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG));
            LOGGER.info("Failed to fetch commander for Force: {}", formation.getName());
            return;
        }

        // Second, fetch all active crew in the commander's unit. If the commander's unit is not multi-crewed, only
        // the commander will be returned.
        Set<Person> educators = new HashSet<>(commander.getUnit().getActiveCrew());

        // Then build a set of their skills
        Map<String, Integer> educatorSkills = createSkillsList(campaign, educators);

        int marginOfSuccess = performTrainingSkillCheck(campaign, commander);

        performTraining(campaign, formation, commander, educatorSkills, marginOfSuccess);
    }

    /**
     * Processes training for all eligible trainees within a force.
     *
     * <p>This method iterates through every unit assigned to the specified {@link Formation} and, for each unit,
     * processes training for all of its active crew members (trainees). For each trainee, it determines which skills
     * are eligible for improvement by comparing the educator's skills against the trainee's skill and experience
     * levels. Only skills where the trainee's experience is less than one level below the educators are eligible.</p>
     *
     * <p>The method also handles simulation of fatigue changes for trainees based on campaign settings and personnel
     * options, and skips training for the commander (educator) themselves if present in the active crew.</p>
     *
     * <p>If there are no eligible skills to train, the trainee's education state is reset and a report is generated.
     * For each valid skill, a learning report is generated and progress is recorded via the
     * {@code processEducationTime} helper method, using the result of the training check's {@code marginOfSuccess} to
     * determine training time awarded and progress.</p>
     *
     * @param campaign        the current {@link Campaign} in which training is occurring
     * @param formation       the {@link Formation} containing the units and trainees to train
     * @param commander       the {@link Person} acting as the educator/commander providing the training
     * @param educatorSkills  a map containing all skills and their experience levels available for teaching by the
     *                        educator(s)
     * @param marginOfSuccess the margin of success for the training check, as an integer (affects training
     *                        speed/progress)
     */
    private static void performTraining(Campaign campaign, Formation formation, Person commander,
          Map<String, Integer> educatorSkills, int marginOfSuccess) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean useReasoningXPChanges = campaignOptions.isUseReasoningXpMultiplier();
        boolean isUseFatigue = campaignOptions.isUseFatigue();
        int fatigueRate = campaignOptions.getFatigueRate();
        double xpCostMultiplier = campaignOptions.getXpCostMultiplier();

        List<Person> educatorCrew = commander.getUnit().getActiveCrew();
        for (UUID unitId : formation.getUnits()) {
            Unit unit = campaign.getUnit(unitId);

            if (unit == null) {
                continue;
            }

            for (Person trainee : unit.getActiveCrew()) {
                if (isUseFatigue) {
                    LOGGER.info("adding fatigue to {}", trainee.getFullTitle());
                    int fatigueChangeRate = getFatigueChangeRate(trainee, fatigueRate);
                    trainee.changeFatigue(fatigueChangeRate);
                }

                if (educatorCrew.contains(trainee)) {
                    continue;
                }

                List<Skill> skillsBeingTrained = new ArrayList<>();
                for (String commanderSkill : educatorSkills.keySet()) {
                    Skill traineeSkill = trainee.getSkill(commanderSkill);

                    if (traineeSkill != null) {
                        SkillType skillType = traineeSkill.getType();
                        int targetLevel = skillType.getRegularLevel();

                        int traineeSkillLevel = traineeSkill.getLevel();
                        if (traineeSkillLevel >= targetLevel) {
                            continue;
                        }

                        if (traineeSkillLevel < educatorSkills.get(commanderSkill)) {
                            skillsBeingTrained.add(traineeSkill);
                        }
                    }
                }

                if (educatorSkills.isEmpty() || skillsBeingTrained.isEmpty()) {
                    campaign.addReport(PERSONNEL, getFormattedTextAt(RESOURCE_BUNDLE, "notLearningAnything.text",
                          trainee.getHyperlinkedFullTitle(),
                          commander.getFullTitle(),
                          spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                          CLOSING_SPAN_TAG));
                    trainee.setTrainingForceEducationTime(0);
                    continue;
                }

                String report = processTrainingTime(campaign, commander, trainee, skillsBeingTrained, marginOfSuccess,
                      xpCostMultiplier, useReasoningXPChanges, campaign.getCampaignOptions().isPersonnelLogSkillGain(),
                      campaign.getLocalDate());

                campaign.personUpdated(trainee);

                if (!StringUtility.isNullOrBlank(report)) {
                    campaign.addReport(PERSONNEL, report);
                }
            }
        }
    }

    private static int getFatigueChangeRate(Person trainee, int fatigueRate) {
        PersonnelOptions options = trainee.getOptions();
        boolean hasGlassJaw = options.booleanOption(FLAW_GLASS_JAW);
        boolean hasToughness = options.booleanOption(ATOW_TOUGHNESS);
        boolean hasGlassJawAndToughness = hasGlassJaw && hasToughness;

        int fatigueChangeRate = fatigueRate;
        if (hasGlassJaw && !hasGlassJawAndToughness) {
            fatigueChangeRate *= 2;
        } else if (hasToughness && !hasGlassJawAndToughness) {
            fatigueChangeRate = max(1, (int) ceil(fatigueChangeRate * 0.5));
        }

        return fatigueChangeRate;
    }

    /**
     * Processes a single training session between an educator and a trainee, determining whether the trainee's
     * lowest-level skill advances to the next level based on XP progress earned.
     *
     * <p>The method follows this sequence:</p>
     * <ol>
     *   <li>Returns early with an empty string if training is impossible (skill check not cleared above {@link
     *   MarginOfSuccess#BARELY_MADE_IT}, or no skills queued).</li>
     *   <li>Sorts {@code skillsBeingTrained} ascending by level and targets the lowest-level skill for improvement.</li>
     *   <li>Calculates the base XP cost to reach the next level of the target skill, applying
     *       {@code xpCostMultiplier} and optional reasoning-based adjustments.</li>
     *   <li>Applies XP progress to the target skill scaled by {@code marginOfSuccess} (minimum 1), then checks
     *   whether the accumulated progress meets or exceeds the cost to improve.</li>
     *   <li>If the cost is fully covered, delegates to
     *       {@link #processCompletedTraining(Campaign, Person, Person, boolean, LocalDate, Skill, int, String, int)}
     *       to apply the improvement and return a formatted report message.</li>
     *   <li>If the cost is not yet met, XP progress is retained on the skill for future sessions and an empty string
     *   is returned.</li>
     * </ol>
     *
     * <p><b>Note:</b> {@code skillsBeingTrained} is sorted in-place. XP progress is always applied to the target
     * skill regardless of whether the improvement threshold is reached, so partial progress persists across calls.</p>
     *
     * @param campaign              the current {@link Campaign} context, used for veterancy award processing and report
     *                              generation
     * @param educator              the {@link Person} acting as the instructor; referenced in the returned report
     *                              message
     * @param trainee               the {@link Person} receiving the training; their skill and XP state are mutated if
     *                              improvement occurs
     * @param skillsBeingTrained    the list of {@link Skill}s queued for training; sorted ascending by level in-place;
     *                              must not be {@code null}
     * @param marginOfSuccess       the margin by which the skill check was passed; values at or below
     *                              {@link MarginOfSuccess#BARELY_MADE_IT} immediately abort training with no XP
     *                              applied; higher values yield proportionally more XP progress
     * @param xpCostMultiplier      a multiplier applied to the raw XP improvement cost before comparing against
     *                              progress; values below {@code 1.0} reduce the cost, values above {@code 1.0}
     *                              increase it
     * @param useReasoningXPChanges whether to apply reasoning-based XP cost adjustments when calculating the
     *                              improvement cost via {@link Person#getCostToImprove}
     * @param isLogSkillChange      whether to write the skill improvement to the trainee's personnel log via
     *                              {@link PerformanceLogger#improvedSkill}
     * @param today                 the in-game date of the training session; recorded in the personnel log if
     *                              {@code isLogSkillChange} is {@code true}
     *
     * @return a formatted report message announcing the skill improvement if the target skill advanced this session; an
     *       empty string if training was impossible or XP progress was enough to complete the improvement
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String processTrainingTime(Campaign campaign, Person educator, Person trainee,
          List<Skill> skillsBeingTrained, int marginOfSuccess, double xpCostMultiplier, boolean useReasoningXPChanges,
          boolean isLogSkillChange, LocalDate today) {
        if (isTrainingImpossible(skillsBeingTrained, marginOfSuccess)) {
            return "";
        }

        sortSkillsLowestLevelToHighest(skillsBeingTrained);

        Skill targetSkill = skillsBeingTrained.getFirst();
        String skillName = targetSkill.getType().getName();
        int targetSkillLevel = targetSkill.getLevel() + 1; // The +1 is to account for the next skill level to be gained

        int baseCostToImprove = getBaseCostToImprove(trainee, xpCostMultiplier, useReasoningXPChanges,
              skillName, targetSkillLevel);

        int finalXPProgress = getFinalXPProgress(marginOfSuccess, targetSkill);

        boolean wasTrainingCompleted = isWasTrainingCompleted(baseCostToImprove, finalXPProgress);

        if (wasTrainingCompleted) {
            return processCompletedTraining(campaign, educator, trainee, isLogSkillChange, today, targetSkill,
                  baseCostToImprove, skillName, targetSkillLevel);
        } else {
            return "";
        }
    }

    /**
     * Finalizes a completed training session by applying the skill improvement, logging the change to the personnel
     * record if requested, and returning a formatted daily report message.
     *
     * @param campaign          the current {@link Campaign} context
     * @param educator          the {@link Person} acting as the instructor
     * @param trainee           the {@link Person} whose skill was improved
     * @param isLogSkillChange  whether to log the skill change to the personnel record
     * @param today             the in-game date on which training was completed
     * @param targetSkill       the {@link Skill} that was improved
     * @param baseCostToImprove the base XP cost (after multiplier) that was deducted from the skill's progress
     * @param skillName         the display name of the skill that was improved
     * @param targetSkillLevel  the new skill level reached after improvement
     *
     * @return a formatted report message announcing the skill improvement, including the educator, trainee, skill name,
     *       and new skill value
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @NonNull String processCompletedTraining(Campaign campaign, Person educator, Person trainee,
          boolean isLogSkillChange, LocalDate today, Skill targetSkill, int baseCostToImprove, String skillName,
          int targetSkillLevel) {
        improveSkill(campaign, trainee, targetSkill, baseCostToImprove);

        // Personnel Log
        PerformanceLogger.improvedSkill(isLogSkillChange, trainee, today, skillName, targetSkillLevel);

        // Daily Report Message
        SkillModifierData skillModifierData = trainee.getSkillModifierData();
        return getFormattedTextAt(RESOURCE_BUNDLE, "learnedNewSkill.text",
              educator.getFullTitle(),
              trainee.getHyperlinkedFullTitle(),
              spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
              CLOSING_SPAN_TAG,
              skillName,
              targetSkill.getFinalSkillValue(skillModifierData));
    }

    /**
     * Applies a skill improvement to the trainee by decrementing the skill's XP progress by the base cost, advancing
     * the skill level, and triggering any veterancy award processing.
     *
     * @param campaign          the current {@link Campaign} context, used for veterancy award evaluation
     * @param trainee           the {@link Person} receiving the skill improvement
     * @param targetSkill       the {@link Skill} to be improved
     * @param baseCostToImprove the XP amount to deduct from the skill's current progress before improving
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void improveSkill(Campaign campaign, Person trainee, Skill targetSkill, int baseCostToImprove) {
        targetSkill.changeXpProgress(-baseCostToImprove);
        targetSkill.improve();

        trainee.processVeterancyAwards(campaign);
    }

    /**
     * Determines whether a training session resulted in a completed skill improvement.
     *
     * <p>Training is considered complete when the remaining XP cost after applying progress is zero.</p>
     *
     * @param baseCostToImprove the total XP cost required to improve the skill
     * @param finalXPProgress   the trainee's accumulated XP progress toward the skill after this session
     *
     * @return {@code true} if the XP progress fully covers the improvement cost; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isWasTrainingCompleted(int baseCostToImprove, int finalXPProgress) {
        int actualCostToImprove = max(0, baseCostToImprove - finalXPProgress);

        return actualCostToImprove == 0;
    }

    /**
     * Calculates and applies XP progress to the target skill for this training session.
     *
     * <p>Progress is at minimum 1 XP, scaled by the margin of success. Returns the skill's total accumulated XP
     * progress after applying this session's gain.</p>
     *
     * @param marginOfSuccess the margin by which the skill check was passed, used to scale XP gain
     * @param targetSkill     the {@link Skill} receiving the XP progress; its progress is mutated in-place
     *
     * @return the skill's total accumulated XP progress after this session's contribution is applied
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int getFinalXPProgress(int marginOfSuccess, Skill targetSkill) {
        int actualXPProgress = max(XP_RATE_BASE_LINE, (XP_RATE_BASE_LINE * marginOfSuccess));
        targetSkill.changeXpProgress(actualXPProgress);

        return targetSkill.getXpProgress();
    }

    /**
     * Calculates the XP cost for the trainee to improve the specified skill to the target level, applying the given
     * cost multiplier and rounding to the nearest integer.
     *
     * @param trainee               the {@link Person} whose skill cost is being evaluated
     * @param xpCostMultiplier      a multiplier applied to the raw improvement cost
     * @param useReasoningXPChanges whether to apply reasoning-based XP cost adjustments
     * @param skillName             the name of the skill being evaluated
     * @param targetSkillLevel      the skill level the trainee is working toward
     *
     * @return the adjusted XP cost to improve the skill, rounded to the nearest integer
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static int getBaseCostToImprove(Person trainee, double xpCostMultiplier, boolean useReasoningXPChanges,
          String skillName, int targetSkillLevel) {
        int baseCostToImprove = trainee.getCostToImprove(skillName, useReasoningXPChanges, targetSkillLevel);
        baseCostToImprove = (int) round(baseCostToImprove * xpCostMultiplier);
        return baseCostToImprove;
    }

    /**
     * Sorts the provided list of skills in ascending order by skill level, so the lowest-level skill is processed first
     * during training.
     *
     * @param skillsBeingTrained the list of {@link Skill}s to sort; modified in-place
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void sortSkillsLowestLevelToHighest(List<Skill> skillsBeingTrained) {
        skillsBeingTrained.sort(Comparator.comparingInt(Skill::getLevel));
    }

    /**
     * Determines whether training is impossible for the current session.
     *
     * <p>Training cannot proceed if the skill check was not passed above the minimum threshold or if no skills are
     * queued for training.</p>
     *
     * @param skillsBeingTrained the list of {@link Skill}s queued for training
     * @param marginOfSuccess    the margin by which the skill check was passed; must equal or exceed
     *                           {@link MarginOfSuccess#BARELY_MADE_IT} for training to be possible
     *
     * @return {@code true} if training cannot proceed; {@code false} if it can
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isTrainingImpossible(List<Skill> skillsBeingTrained, int marginOfSuccess) {
        boolean isSkillCheckFailed = marginOfSuccess < BARELY_MADE_IT.getValue();
        boolean isNothingBeingTrained = skillsBeingTrained.isEmpty();

        return isSkillCheckFailed || isNothingBeingTrained;
    }

    private static int performTrainingSkillCheck(Campaign campaign, Person educator) {
        final LocalDate today = campaign.getLocalDate();
        final boolean isClanCampaign = campaign.isClanCampaign();
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean useAgingEffects = campaignOptions.isUseAgeEffects();

        SkillCheckUtility skillCheck = new SkillCheckUtility(
              getTextAt(RESOURCE_BUNDLE, "trainingCombatTeam.skillCheck"),
              educator,
              S_TRAINING,
              new ArrayList<>(),
              0,
              true,
              true,
              useAgingEffects,
              isClanCampaign,
              today);
        int raw = skillCheck.getMarginOfSuccess();
        MarginOfSuccess marginOfSuccess = getMarginOfSuccessObject(raw);

        String personnelReport = getFormattedTextAt(RESOURCE_BUNDLE, "learnedProgress.text",
              educator.getHyperlinkedFullTitle(), spanOpeningWithCustomColor(marginOfSuccess.getColor()),
              marginOfSuccess.getLabel(), CLOSING_SPAN_TAG);
        campaign.addReport(PERSONNEL, personnelReport);

        String skillRollReport = skillCheck.getResultsText();
        campaign.addReport(SKILL_CHECKS, skillRollReport);

        return raw;
    }

    /**
     * Creates a list of skills available for education based on the educators' abilities.
     *
     * <p>This method aggregates the skills of all given educators, taking the highest experience
     * level available for each skill among the provided educators.</p>
     *
     * @param campaign  the current {@link Campaign}
     * @param educators a {@link Set} of {@link Person} objects acting as educators
     *
     * @return a {@link Map} of skill names to experience levels representing the available skills for teaching
     */
    private static Map<String, Integer> createSkillsList(Campaign campaign, Set<Person> educators) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean isUseArtillery = campaignOptions.isUseArtillery();
        final boolean isUseAdvancedScouting = campaign.getCampaignOptions().isUseAdvancedScouting();

        Set<String> professionSkills = new HashSet<>();
        for (Person educator : educators) {
            // Collect all unique skills from all educators. We use a Set to avoid duplicates.

            PersonnelRole primaryRole = educator.getPrimaryRole();
            PersonnelRole secondaryRole = educator.getSecondaryRole();
            getSkillsForProfession(
                  primaryRole,
                  professionSkills,
                  isUseArtillery,
                  secondaryRole,
                  isUseAdvancedScouting);
        }

        // Then, find the best experience level available among educators in the commander's unit
        return getEducatorSkills(educators, professionSkills);
    }

    /**
     * Aggregates and filters skills from a set of educator {@link Person} objects, returning only the most proficient
     * skill within each competitive skill group (e.g., scouting, infantry gunnery).
     *
     * <p>For each educator, profession skills are first collected via {@code getProfessionSkillsForEducator}, then
     * competing skill groups are filtered so that only the highest-level skill in each group is retained.</p>
     *
     * @param educators        the set of {@link Person} objects acting as educators; must not be {@code null}
     * @param professionSkills the set of skill names eligible for collection from each educator; must not be
     *                         {@code null}
     *
     * @return a {@link Map} of skill name to experience level, containing at most one representative skill per
     *       competitive skill group
     *
     * @author Illiani
     * @since 50.10
     */
    private static Map<String, Integer> getEducatorSkills(Set<Person> educators, Set<String> professionSkills) {
        Map<String, Integer> educatorSkills = new HashMap<>();

        for (Person educator : educators) {
            getProfessionSkillsForEducator(professionSkills, educator, educatorSkills);
        }

        filterInfantryGunnerySkillsForEducator(educatorSkills);
        filterScoutingSkillsForEducator(educatorSkills);

        return educatorSkills;
    }

    /**
     * Filters the educator's current skill map so that only the single highest-level infantry gunnery skill is
     * retained, removing all others.
     *
     * <p>Iterates over {@link InfantryGunnerySkills#INFANTRY_GUNNERY_SKILLS}, tracking the skill with the greatest
     * experience level. All infantry gunnery skills are removed from {@code educatorSkills} during iteration; the best
     * one is then re-added via {@link #validateAndAddBackEducatorAggregateSkill} if it resolves to a valid
     * {@link SkillType}.</p>
     *
     * @param educatorSkills the mutable skill map to filter in place; must not be {@code null}
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void filterInfantryGunnerySkillsForEducator(Map<String, Integer> educatorSkills) {
        final int NO_SKILL_PICKED = -1;
        String bestInfantryGunnerySkillName = "";
        int bestInfantryGunnerySkillExperienceLevel = NO_SKILL_PICKED;
        for (String currentInfantryGunnerySkill : InfantryGunnerySkills.INFANTRY_GUNNERY_SKILLS) {
            if (!isSkillAmongEducatorSkills(educatorSkills, currentInfantryGunnerySkill)) {
                continue;
            }

            int currentInfantryGunneryLevel = educatorSkills.get(currentInfantryGunnerySkill);
            boolean isNoSkillSet = bestInfantryGunnerySkillExperienceLevel == NO_SKILL_PICKED;
            boolean isCurrentSkillBetter = bestInfantryGunnerySkillExperienceLevel < currentInfantryGunneryLevel;

            if (isNoSkillSet || isCurrentSkillBetter) {
                bestInfantryGunnerySkillName = currentInfantryGunnerySkill;
                bestInfantryGunnerySkillExperienceLevel = currentInfantryGunneryLevel;
            }

            educatorSkills.remove(currentInfantryGunnerySkill);
        }

        if (bestInfantryGunnerySkillExperienceLevel != NO_SKILL_PICKED) {
            validateAndAddBackEducatorAggregateSkill(educatorSkills,
                  bestInfantryGunnerySkillName,
                  bestInfantryGunnerySkillExperienceLevel);
        }
    }

    /**
     * Validates a candidate aggregate skill name and, if valid, inserts it back into the educator's skill map at the
     * given experience level.
     *
     * <p>Validation is performed by checking that {@link SkillType#getType(String)} returns a non-{@code null}
     * result for {@code bestSkillName}. If validation fails, an error is logged with a {@link NullPointerException} and
     * the skill is not added.</p>
     *
     * @param educatorSkills           the skill map to update; must not be {@code null}
     * @param bestSkillName            the name of the skill to validate and re-insert
     * @param bestSkillExperienceLevel the experience level to associate with the skill
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void validateAndAddBackEducatorAggregateSkill(Map<String, Integer> educatorSkills,
          String bestSkillName, int bestSkillExperienceLevel) {
        if (SkillType.getType(bestSkillName) != null) {
            educatorSkills.put(bestSkillName, bestSkillExperienceLevel);
        } else {
            LOGGER.error(new NullPointerException(), "No valid skill for {}", bestSkillName);
        }
    }

    /**
     * Returns {@code true} if the given skill name is present as a key in the educator's skill map.
     *
     * @param educatorSkills the skill map to query; must not be {@code null}
     * @param skillName      the skill name to look up
     *
     * @return {@code true} if {@code educatorSkills} contains the specified skill
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isSkillAmongEducatorSkills(Map<String, Integer> educatorSkills, String skillName) {
        return educatorSkills.containsKey(skillName);
    }

    /**
     * Filters the educator's current skill map so that only the single highest-level scouting skill is retained,
     * removing all others.
     *
     * <p>Iterates over {@link ScoutingSkills#SCOUTING_SKILLS}, tracking the skill with the greatest experience level.
     * All scouting skills are removed from {@code educatorSkills} during iteration; the best one is then re-added via
     * {@link #validateAndAddBackEducatorAggregateSkill} if it resolves to a valid {@link SkillType}.</p>
     *
     * @param educatorSkills the mutable skill map to filter in place
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void filterScoutingSkillsForEducator(Map<String, Integer> educatorSkills) {
        final int NO_SKILL_PICKED = -1;
        String bestScoutingSkillName = "";
        int bestScoutingSkillExperienceLevel = NO_SKILL_PICKED;
        for (String currentScoutingSkill : ScoutingSkills.SCOUTING_SKILLS) {
            if (!educatorSkills.containsKey(currentScoutingSkill)) {
                continue;
            }

            int currentScoutingLevel = educatorSkills.get(currentScoutingSkill);
            boolean isNoSkillSet = bestScoutingSkillExperienceLevel == NO_SKILL_PICKED;
            boolean isCurrentSkillBetter = bestScoutingSkillExperienceLevel < currentScoutingLevel;

            if (isNoSkillSet || isCurrentSkillBetter) {
                bestScoutingSkillName = currentScoutingSkill;
                bestScoutingSkillExperienceLevel = currentScoutingLevel;
            }

            educatorSkills.remove(currentScoutingSkill);
        }

        if (bestScoutingSkillExperienceLevel != NO_SKILL_PICKED) {
            validateAndAddBackEducatorAggregateSkill(educatorSkills,
                  bestScoutingSkillName,
                  bestScoutingSkillExperienceLevel);
        }
    }

    /**
     * Collects profession skills from a single educator and merges them into the shared skill map, retaining the
     * highest experience level seen for each skill.
     *
     * <p>For each skill name in {@code professionSkills}, the educator's corresponding {@link Skill} is retrieved.
     * If present, its experience level is computed via {@link Skill#getExperienceLevel(SkillModifierData)}, reduced by
     * {@link #EXPERIENCE_LEVEL_REDUCTION}, and merged into {@code educatorSkills} using {@link Math#max} so that a
     * skill already recorded at a higher level is never overwritten.</p>
     *
     * <p>Skills the educator does not possess (i.e. {@link Person#getSkill(String)} returns {@code null}) are
     * silently skipped.</p>
     *
     * @param professionSkills the set of skill names to evaluate for the educator; must not be {@code null}
     * @param educator         the {@link Person} whose skills are being assessed; must not be {@code null}
     * @param educatorSkills   the mutable skill map to merge results into; must not be {@code null}
     */
    private static void getProfessionSkillsForEducator(Set<String> professionSkills, Person educator,
          Map<String, Integer> educatorSkills) {
        SkillModifierData skillModifierData = educator.getSkillModifierData();
        for (String professionSkill : professionSkills) {
            Skill skill = educator.getSkill(professionSkill);

            if (skill != null) {
                int experienceLevel = skill.getExperienceLevel(skillModifierData) + EXPERIENCE_LEVEL_REDUCTION;
                educatorSkills.merge(professionSkill, experienceLevel, Math::max);
            }
        }
    }

    /**
     * Collects the relevant skills for a given profession based on an educator's roles.
     *
     * <p>For combat roles (primary or secondary), adds all combat-related profession skills. If advanced scouting is
     * enabled, it also adds any scouting skills the educator possesses. Non-combat roles are not processed and will
     * cause an early return.</p>
     *
     * @param primaryRole           the educator's primary role
     * @param professionSkills      the set to add relevant skill names to (additive)
     * @param isUseArtillery        whether artillery skills should be included
     * @param secondaryRole         the educator's secondary role
     * @param isUseAdvancedScouting whether to include advanced scouting skills
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void getSkillsForProfession(PersonnelRole primaryRole, Set<String> professionSkills,
          boolean isUseArtillery, PersonnelRole secondaryRole, boolean isUseAdvancedScouting) {
        if (primaryRole.isCombat()) {
            professionSkills.addAll(primaryRole.getSkillsForProfession(false, false, false,
                  isUseArtillery, true));
        } else if (secondaryRole.isCombat()) { // Primary overrides secondary, so no double-dipping
            professionSkills.addAll(secondaryRole.getSkillsForProfession(false, false, false,
                  isUseArtillery, true));
        } else {
            // support professions cannot teach skills through training forces
            return;
        }

        // Add all scouting skills if advanced scouting is enabled
        if (isUseAdvancedScouting) {
            professionSkills.addAll(ScoutingSkills.SCOUTING_SKILLS);
        }
    }
}
