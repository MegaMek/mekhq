package mekhq.campaign.personnel.education;

import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.unit.Unit;

import java.time.LocalDate;
import java.util.*;

import static java.lang.Math.round;
import static mekhq.campaign.personnel.SkillType.EXP_GREEN;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * Handles the training of combat teams within the campaign.
 *
 * <p>This class is responsible for managing the process of skill improvement and education for
 * training combat teams and their associated personnel. It identifies eligible combat teams,
 * validates their contracts and current conditions, and processes training for each team member.</p>
 *
 * <p>Key functionality includes tracking education time for trainees, determining skills eligible
 * for training, generating skill improvement reports, and handling both individual and group
 * training scenarios.</p>
 *
 * <p>Main methods:
 * <ul>
 *     <li>{@link #processTrainingCombatTeams(Campaign)}: Entry point for processing all combat
 *     team training in the campaign.</li>
 *     <li>{@link #processTraining(Campaign, CombatTeam)}: Applies training logic to a specific
 *     combat team.</li>
 *     <li>{@link #performTraining(Campaign, Force, Person, Map)}: Handles training for individual
 *     trainees in a force.</li>
 *     <li>{@link #processEducationTime(Campaign, Person, Person, List)}: Updates a trainee's
 *     education progression and improves skills.</li>
 *     <li>{@link #createSkillsList(Campaign, Set)}: Collects the skill levels of educators to
 *     determine skills eligible for training.</li>
 * </ul>
 */
public class TrainingCombatTeams {
    private static final MMLogger logger = MMLogger.create(TrainingCombatTeams.class);

    private static final String BUNDLE_NAME = "mekhq.resources.Education";
    private static ResourceBundle resources = ResourceBundle.getBundle(BUNDLE_NAME, MekHQ.getMHQOptions().getLocale());

    /**
     * Processes all training combat teams in the campaign.
     *
     * <p>This method iterates through all combat teams in the campaign and processes training for
     * those whose role includes training. It ensures that combat teams are eligible for training
     * by checking that their contracts are active and valid on the current date. If StratCon is
     * used, it also verifies that the teams are deployed in their appropriate sectors.</p>
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

            if (campaign.getCampaignOptions().isUseStratCon()
                && !contract.getStratconCampaignState().isForceDeployedHere(combatTeam.getForceId())) {
                continue;
            }

            processTraining(campaign, combatTeam);
        }
    }

    /**
     * Handles the training progression for an individual combat team.
     *
     * <p>This method identifies the combat team's commander and educators within the unit,
     * collects their skills, and compares them to the skills of the trainees in the team. Eligible
     * trainees undergo training to improve their skills, which is simulated through education
     * time tracking and skill level progression.</p>
     *
     * <p>If educators or trainees lack eligible skills, appropriate reports are generated.
     * Training updates for skills and progression are logged within the campaign.</p>
     *
     * @param campaign   the {@link Campaign} managing the combat team and its associated personnel
     * @param combatTeam the {@link CombatTeam} undergoing training during this session
     */
    private static void processTraining(final Campaign campaign, final CombatTeam combatTeam) {
        // First, identify the Combat Team's commander
        Person commander = combatTeam.getCommander(campaign);

        if (commander == null) {
            logger.info(String.format("Failed to fetch commander for Combat Team: %s",
                    combatTeam.getForceId()));
            return;
        }

        // Second, fetch all active crew in the commander's unit.
        // If the commander's unit is not multi-crewed, only the commander will be returned.
        Set<Person> educators = new HashSet<>(commander.getUnit().getActiveCrew());

        // Then build a set of their skills
        Map<String, Integer> educatorSkills = createSkillsList(campaign, educators);

        // Next cycle through each character in the force
        Force force = campaign.getForceFor(commander);

        performTraining(campaign, force, commander, educatorSkills);
    }

    /**
     * Handles training for all trainees within a force.
     *
     * <p>This method iterates over each unit in the specified force and processes training
     * for all active personnel within each unit. Eligible skills are identified by comparing
     * the educator's abilities to those of the trainees, and skill improvement is simulated.</p>
     *
     * @param campaign       the current {@link Campaign}
     * @param force          the {@link Force} containing the units to train
     * @param commander      the {@link Person} commanding the combat team
     * @param educatorSkills a map of skills and their experience levels available for teaching
     */
    private static void performTraining(Campaign campaign, Force force, Person commander,
                                        Map<String, Integer> educatorSkills) {
        for (UUID unitId : force.getAllUnits(true)) {
            Unit unit = campaign.getUnit(unitId);

            if (unit == null) {
                continue;
            }

            for (Person trainee : unit.getActiveCrew()) {
                if (commander.getUnit().getActiveCrew().contains(trainee)) {
                    continue;
                }

                List<Skill> skillsBeingTrained = new ArrayList<>();
                for (String commanderSkill : educatorSkills.keySet()) {
                    Skill traineeSkill = trainee.getSkill(commanderSkill);

                    if (traineeSkill != null) {
                        int traineeExperienceLevel = traineeSkill.getExperienceLevel();

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
                            trainee.getHyperlinkedFullTitle(), commander.getFullTitle(),
                            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                            CLOSING_SPAN_TAG));
                    trainee.setEduAcademyName("");
                    trainee.setEduEducationTime(0);
                    continue;
                }

                // We piggyback on the education module here.
                // If the character ever enters actual education, this will be overwritten.
                processEducationTime(campaign, commander, trainee, skillsBeingTrained);
            }
        }
    }

    /**
     * Progresses a trainee's education time and improves skills if the required threshold is met.
     *
     * <p>This method calculates the education time required for the next experience level and compares
     * it with the trainee's accumulated education time. If the required time is met or exceeded, the
     * skill level is increased, and education time is reset or reduced as needed.</p>
     *
     * @param campaign          the current {@link Campaign}
     * @param commander         the {@link Person} acting as the educator for the trainee
     * @param trainee           the {@link Person} receiving training
     * @param skillsBeingTrained a list of eligible {@link Skill} objects for training
     */
    private static void processEducationTime(Campaign campaign, Person commander,
                                             Person trainee, List<Skill> skillsBeingTrained) {
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final String EDUCATION_STRING = "TRAINING_COMBAT_TEAM"; // Never change this
        final int WEEK_DURATION = 7; // days
        final int EDUCATION_TIME_MULTIPLIER = 28; // days

        if (!Objects.equals(trainee.getEduAcademyName(), EDUCATION_STRING)) {
            trainee.setEduAcademyName(EDUCATION_STRING);
            trainee.setEduEducationTime(0);
        } else {
            int newEducationTime = trainee.getEduEducationTime() + WEEK_DURATION;
            trainee.setEduEducationTime(newEducationTime);

            if (skillsBeingTrained.isEmpty()) {
                return;
            }

            // The lowest skill is improved first
            skillsBeingTrained.sort(Comparator.comparingInt(Skill::getExperienceLevel));
            Skill targetSkill = skillsBeingTrained.get(0);
            // The +1 is to account for the next experience level to be gained
            int currentExperienceLevel = targetSkill.getExperienceLevel() + 1;

            int perExperienceLevelMultiplier = EDUCATION_TIME_MULTIPLIER;
            double experienceMultiplier = campaignOptions.getXpCostMultiplier();
            double intelligenceCostMultiplier = trainee.getIntelligenceXpCostMultiplier(campaignOptions);

            perExperienceLevelMultiplier = (int) round(perExperienceLevelMultiplier * experienceMultiplier * intelligenceCostMultiplier);

            int educationTimeReduction = currentExperienceLevel * perExperienceLevelMultiplier;
            if (newEducationTime >= educationTimeReduction) {
                trainee.setEduEducationTime(newEducationTime - educationTimeReduction);
                targetSkill.setLevel(targetSkill.getLevel() + 1);


                campaign.addReport(String.format(resources.getString("learnedNewSkill.text"),
                        commander.getFullTitle(), trainee.getHyperlinkedFullTitle(),
                        spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                        CLOSING_SPAN_TAG, targetSkill.getType().getName(), targetSkill.getFinalSkillValue()));
            }
        }
    }

    /**
     * Creates a list of skills available for education based on the educators' abilities.
     *
     * <p>This method aggregates the skills of all given educators, taking the highest experience
     * level available for each skill among the provided educators.</p>
     *
     * @param campaign  the current {@link Campaign}
     * @param educators a {@link Set} of {@link Person} objects acting as educators
     * @return a {@link Map} of skill names to experience levels representing the available skills
     * for teaching
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
                        educatorSkills.put(professionSkill, skill.getExperienceLevel());
                    } else {
                        int educatorSkillLevel = educatorSkills.get(professionSkill);

                        if (educatorSkillLevel < skill.getExperienceLevel()) {
                            educatorSkills.put(professionSkill, skill.getExperienceLevel());
                        }
                    }
                }
            }
        }

        return educatorSkills;
    }
}
