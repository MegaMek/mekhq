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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.education;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;
import static mekhq.campaign.personnel.skills.SkillType.S_TRAINING;
import static mekhq.campaign.personnel.skills.SkillUtilities.EXP_GREEN;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.BARELY_MADE_IT;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessColor;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessObject;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginOfSuccessString;
import static mekhq.campaign.personnel.skills.enums.MarginOfSuccess.getMarginValue;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.*;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.log.PerformanceLogger;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillCheckUtility;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.ReportingUtilities;

/**
 * Handles the training of combat teams within the campaign.
 *
 * <p>This class is responsible for managing the process of skill improvement and education for
 * training combat teams and their associated personnel. It identifies eligible combat teams, validates their contracts
 * and current conditions, and processes training for each team member.</p>
 *
 * <p>Key functionality includes tracking education time for trainees, determining skills eligible
 * for training, generating skill improvement reports, and handling both individual and group training scenarios.</p>
 *
 * <p>Main methods:
 * <ul>
 *     <li>{@link #processTrainingCombatTeams(Campaign)}: Entry point for processing all combat
 *     team training in the campaign.</li>
 *     <li>{@link #processTraining(Campaign, Force)}: Applies training logic to a specific
 *     combat team.</li>
 *     <li>{@link #performTraining(Campaign, Force, Person, Map, int)}: Handles training for individual
 *     trainees in a force.</li>
 *     <li>{@link #processEducationTime(Person, Person, List, int, double, boolean, LocalDate)}  Updates a trainee's education progression
 *     and improves skills.</li>
 *     <li>{@link #createSkillsList(Campaign, Set)}: Collects the skill levels of educators to
 *     determine skills eligible for training.</li>
 * </ul>
 */
public class TrainingCombatTeams {
    private static final MMLogger LOGGER = MMLogger.create(TrainingCombatTeams.class);

    private static final String BUNDLE_NAME = "mekhq.resources.Education";
    private static final ResourceBundle resources = ResourceBundle.getBundle(BUNDLE_NAME,
          MekHQ.getMHQOptions().getLocale());

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
        final List<CombatTeam> combatTeams = campaign.getAllCombatTeams();

        for (CombatTeam combatTeam : combatTeams) {
            if (!combatTeam.getRole().isTraining()) {
                continue;
            }

            AtBContract contract = combatTeam.getContract(campaign);
            if (contract == null || !contract.isActiveOn(today, true)) {
                continue;
            }

            if (campaign.getCampaignOptions().isUseStratCon() &&
                      !contract.getStratconCampaignState().isForceDeployedHere(combatTeam.getForceId())) {
                continue;
            }

            Force force = combatTeam.getForce(campaign);
            List<Force> allForces = new ArrayList<>();
            allForces.add(force); // We want to include the force itself in the training process
            allForces.addAll(force.getAllSubForces());

            for (Force trainingForce : allForces) {
                processTraining(campaign, trainingForce);
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
     * @param campaign the {@link Campaign} managing the combat team and its associated personnel
     * @param force    the {@link Force} undergoing training during this session
     */
    private static void processTraining(final Campaign campaign, final Force force) {
        // If the force is empty, we skip it
        Vector<UUID> units = force.getUnits(); // We only want units in the direct force, not child forces
        if (units.isEmpty()) {
            LOGGER.info("No units in force '{}' for campaign '{}'", force.getName(), campaign.getName());
            return;
        }

        // Identify the Combat Team's commander (i.e. the Trainer)
        UUID commanderID = force.getForceCommanderID();
        Person commander = campaign.getPerson(commanderID);

        if (commander == null) {
            campaign.addReport(String.format(resources.getString("noCommander.text"), force.getName(),
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG));
            LOGGER.info("Failed to fetch commander for Force: {}", force.getName());
            return;
        }

        // Second, fetch all active crew in the commander's unit. If the commander's unit is not multi-crewed, only
        // the commander will be returned.
        Set<Person> educators = new HashSet<>(commander.getUnit().getActiveCrew());

        // Then build a set of their skills
        Map<String, Integer> educatorSkills = createSkillsList(campaign, educators);

        int marginOfSuccess = performTrainingSkillCheck(campaign, commander);

        performTraining(campaign, force, commander, educatorSkills, marginOfSuccess);
    }

    /**
     * Processes training for all eligible trainees within a force.
     *
     * <p>This method iterates through every unit assigned to the specified {@link Force} and, for each unit,
     * processes training for all of its active crew members (trainees). For each trainee, it determines which skills
     * are eligible for improvement by comparing the educator's skills against the trainee's skill and experience
     * levels. Only skills where the trainee's experience is less than one level below the educator's are eligible.</p>
     *
     * <p>The method also handles simulation of fatigue changes for trainees based on campaign settings and personnel
     * options, and skips training for the commander (educator) themself if present in the active crew.</p>
     *
     * <p>If there are no eligible skills to train, the trainee's education state is reset and a report is generated.
     * For each valid skill, a learning report is generated and progress is recorded via the
     * {@code processEducationTime} helper method, using the result of the training check's {@code marginOfSuccess} to
     * determine training time awarded and progress.</p>
     *
     * @param campaign        the current {@link Campaign} in which training is occurring
     * @param force           the {@link Force} containing the units and trainees to train
     * @param commander       the {@link Person} acting as the educator/commander providing the training
     * @param educatorSkills  a map containing all skills and their experience levels available for teaching by the
     *                        educator(s)
     * @param marginOfSuccess the margin of success for the training check, as an integer (affects training
     *                        speed/progress)
     */
    private static void performTraining(Campaign campaign, Force force, Person commander,
          Map<String, Integer> educatorSkills, int marginOfSuccess) {
        double xpCostMultiplier = campaign.getCampaignOptions().getXpCostMultiplier();

        for (UUID unitId : force.getUnits()) {
            Unit unit = campaign.getUnit(unitId);

            if (unit == null) {
                continue;
            }

            for (Person trainee : unit.getActiveCrew()) {
                if (campaign.getCampaignOptions().isUseFatigue()) {
                    int fatigueChangeRate = campaign.getCampaignOptions().getFatigueRate();

                    boolean hasGlassJaw = trainee.getOptions().booleanOption(FLAW_GLASS_JAW);
                    boolean hasToughness = trainee.getOptions().booleanOption(ATOW_TOUGHNESS);
                    boolean hasGlassJawAndToughness = hasGlassJaw && hasToughness;

                    if (hasGlassJaw && !hasGlassJawAndToughness) {
                        fatigueChangeRate = fatigueChangeRate * 2;
                    } else if (hasToughness && !hasGlassJawAndToughness) {
                        fatigueChangeRate = (int) round(fatigueChangeRate * 0.5);
                    }

                    trainee.changeFatigue(fatigueChangeRate);
                }

                if (commander.getUnit().getActiveCrew().contains(trainee)) {
                    continue;
                }

                List<Skill> skillsBeingTrained = new ArrayList<>();
                for (String commanderSkill : educatorSkills.keySet()) {
                    Skill traineeSkill = trainee.getSkill(commanderSkill);

                    if (traineeSkill != null) {
                        int skillLevel = traineeSkill.getLevel();
                        int traineeExperienceLevel = traineeSkill.getType().getExperienceLevel(skillLevel);

                        if (traineeExperienceLevel > EXP_GREEN) {
                            continue;
                        }

                        // The commander is required to be one step above the experience level they
                        // are teaching.
                        if (traineeExperienceLevel < (educatorSkills.get(commanderSkill) - 1)) {
                            skillsBeingTrained.add(traineeSkill);
                        }
                    }
                }

                if (educatorSkills.isEmpty() || skillsBeingTrained.isEmpty()) {
                    campaign.addReport(String.format(resources.getString("notLearningAnything.text"),
                          trainee.getHyperlinkedFullTitle(),
                          commander.getFullTitle(),
                          spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                          CLOSING_SPAN_TAG));
                    trainee.setEduAcademyName("");
                    trainee.setEduEducationTime(0);
                    continue;
                }

                // We piggyback on the education module here. If the character ever enters actual education, this
                // will be overwritten.
                String report = processEducationTime(commander, trainee, skillsBeingTrained, marginOfSuccess,
                      xpCostMultiplier, campaign.getCampaignOptions().isPersonnelLogSkillGain(),
                      campaign.getLocalDate());

                if (!StringUtility.isNullOrBlank(report)) {
                    campaign.addReport(report);
                }
            }
        }
    }

    /**
     * Progresses a trainee's education time and increases the skill level of the trainee if enough training time has
     * accrued.
     *
     * <p>This method simulates on-the-job or field training for a {@link Person} (the trainee) by the given educator
     * (trainer). It does so by maintaining an education history and counting accumulated days. When the accumulated
     * training time reaches the required threshold for the next skill improvement, the lowest eligible skill from
     * {@code skillsBeingTrained} will be increased by one level, the trainee's education-time counter will be reduced
     * appropriately, and a report is generated.</p>
     *
     * <p>Functional details:</p>
     * <ul>
     *     <li>If the trainee is not currently set as being in the {@code TRAINING_COMBAT_TEAM} 'academy', their
     *     education time is reset and this value is set.</li>
     *     <li>If the trainee is already in team training, increment their education time. If they have no trainable
     *     skills for this session, no further action is taken.</li>
     *     <li>The skill improved is always the lowest-level skill in {@code skillsBeingTrained} (ties are broken
     *     arbitrarily).</li>
     *     <li>The experience required for the next level is based on a campaign setting-multiplied constant, with
     *     the experience tier for the <b>next</b> level (not current) used as a multiplier.</li>
     *     <li>If after incrementing, the education time counter is still above the required threshold, another point
     *     of improvement may happen at a future call.</li>
     * </ul>
     *
     * @param educator           the {@link Person} acting as the educator (commander or trainer) for the trainee
     * @param trainee            the {@link Person} receiving the training and accumulating skill improvement
     * @param skillsBeingTrained a list of {@link Skill} objects that the trainee is eligible to improve during this
     *                           session. The lowest-level skill in this list is chosen for improvement if the time
     *                           threshold is met.
     */
    private static String processEducationTime(Person educator, Person trainee, List<Skill> skillsBeingTrained,
          int marginOfSuccess, double trainingMultiplier, boolean isLogSkillChange, LocalDate today) {
        final String EDUCATION_STRING = "TRAINING_COMBAT_TEAM"; // Never change this
        final int WEEK_DURATION = 7; // days
        final int EDUCATION_TIME_MULTIPLIER = 35; // days

        if (!Objects.equals(trainee.getEduAcademyName(), EDUCATION_STRING)) {
            trainee.setEduAcademyName(EDUCATION_STRING);
            trainee.setEduEducationTime(0);
            return String.format(resources.getString("learningStarted.text"), trainee.getHyperlinkedFullTitle());
        } else {
            if (skillsBeingTrained.isEmpty()) {
                return "";
            }

            double successMultiplier = 1.0;
            if (marginOfSuccess >= getMarginValue(BARELY_MADE_IT)) {
                successMultiplier += (marginOfSuccess * 0.25);
            } else {
                successMultiplier -= (abs(marginOfSuccess) * 0.25);
            }

            int trainingTime = (int) round(WEEK_DURATION * successMultiplier);
            int newEducationTime = trainee.getEduEducationTime() + trainingTime;
            trainee.setEduEducationTime(newEducationTime);

            // The lowest skill is improved first
            skillsBeingTrained.sort(Comparator.comparingInt(Skill::getLevel));
            Skill targetSkill = skillsBeingTrained.get(0);
            // The +1 is to account for the next experience level to be gained

            int currentSkillLevel = targetSkill.getLevel();
            int currentExperienceLevel = targetSkill.getType().getExperienceLevel(currentSkillLevel + 1);

            int perExperienceLevelMultiplier = EDUCATION_TIME_MULTIPLIER;

            // Reasoning cost changes should always take place before global changes
            perExperienceLevelMultiplier = (int) round(perExperienceLevelMultiplier * trainingMultiplier);

            int educationTimeReduction = currentExperienceLevel * perExperienceLevelMultiplier;
            if (newEducationTime >= educationTimeReduction) {
                trainee.setEduEducationTime(newEducationTime - educationTimeReduction);
                int newSkillLevel = currentSkillLevel + 1;
                targetSkill.setLevel(newSkillLevel);
                String skillName = targetSkill.getType().getName();

                PerformanceLogger.improvedSkill(isLogSkillChange, trainee, today, skillName, newSkillLevel);

                return String.format(resources.getString("learnedNewSkill.text"),
                      educator.getFullTitle(),
                      trainee.getHyperlinkedFullTitle(),
                      spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
                      CLOSING_SPAN_TAG,
                      skillName,
                      targetSkill.getFinalSkillValue(trainee.getOptions(), trainee.getATOWAttributes()));
            }
        }

        return "";
    }

    private static int performTrainingSkillCheck(Campaign campaign, Person educator) {
        final LocalDate today = campaign.getLocalDate();
        final boolean isClanCampaign = campaign.isClanCampaign();
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean useAgingEffects = campaignOptions.isUseAgeEffects();

        SkillCheckUtility skillCheck = new SkillCheckUtility(educator, S_TRAINING, new ArrayList<>(), 0, true,
              false, useAgingEffects, isClanCampaign, today);
        int raw = skillCheck.getMarginOfSuccess();
        MarginOfSuccess marginOfSuccess = getMarginOfSuccessObject(raw);

        String report = String.format(resources.getString("learnedProgress.text"),
              educator.getHyperlinkedFullTitle(), spanOpeningWithCustomColor(getMarginOfSuccessColor(marginOfSuccess)),
              getMarginOfSuccessString(marginOfSuccess), CLOSING_SPAN_TAG);
        campaign.addReport(report);

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
        Map<String, Integer> educatorSkills = new HashMap<>();
        for (Person educator : educators) {
            // First, collect all the skills. We use a Set, as we don't want duplicates
            Set<String> professionSkills = new HashSet<>();

            if (educator.getPrimaryRole().isCombat()) {
                professionSkills.addAll(educator.getProfessionSkills(campaign, false));
            }

            if (educator.getSecondaryRole().isCombat()) {
                professionSkills.addAll(educator.getProfessionSkills(campaign, true));
            }

            // Then, find the best experience level available among educators in the commander's unit
            for (String professionSkill : professionSkills) {
                Skill skill = educator.getSkill(professionSkill);

                if (skill != null) {
                    if (!educatorSkills.containsKey(professionSkill)) {
                        educatorSkills.put(professionSkill, skill.getLevel());
                    } else {
                        int educatorSkillLevel = educatorSkills.get(professionSkill);

                        if (educatorSkillLevel < skill.getLevel()) {
                            educatorSkills.put(professionSkill, skill.getLevel());
                        }
                    }
                }
            }
        }

        return educatorSkills;
    }
}
