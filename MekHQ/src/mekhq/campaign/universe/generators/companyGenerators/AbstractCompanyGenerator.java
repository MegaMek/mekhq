/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe.generators.companyGenerators;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import mekhq.MHQConstants;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialTerm;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Force;
import mekhq.campaign.icons.ForcePieceIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.AbstractPersonnelGenerator;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.*;
import mekhq.campaign.universe.companyGeneration.AtBRandomMechParameters;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationPersonTracker;
import mekhq.campaign.universe.enums.Alphabet;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.campaign.universe.enums.CompanyGenerationPersonType;
import mekhq.campaign.universe.generators.battleMechQualityGenerators.AbstractBattleMechQualityGenerator;
import mekhq.campaign.universe.generators.battleMechWeightClassGenerators.AbstractBattleMechWeightClassGenerator;
import mekhq.campaign.universe.selectors.factionSelectors.AbstractFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.RangedFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.AbstractPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.RangedPlanetSelector;
import mekhq.campaign.work.WorkTime;
import org.apache.logging.log4j.LogManager;

import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Startup:
 * Second Panel: Presets, Date, Starting Faction, Starting Planet, AtB
 * Third Panel: Campaign Options
 * Fifth Panel: Start of the Company Generator
 *
 * Ideas:
 * First panel is the options panel
 * Second panel is the generated personnel panel, where you can customize and reroll personnel
 * Third panel is the generated units panel, where you can customize applicable units
 * Fourth panel is the parts list, which is customizable
 * Fifth panel is a view generated pairings, and allows the reorder of the preset lances
 *
 * Second to Last panel of the dialog should be the contract market when coming from quickstart, to select starting contract
 * Final panel is the starting finances overview
 *
 * Button that lets you pop out the options panel with everything disabled
 *
 * TODO - Wave 3:
 *      Contract Market Pane
 *      Campaign Options Pane, Campaign Options Dialog Base Validation
 *      Date Pane
 *      Loan Selection Pane
 *      Implement Loan Options
 *      Probably some stuff from public requests
 * TODO - Wave 4:
 *      Startup GUI Rework
 * TODO - Wave 5:
 *      Company Generator GUI
 *      Implement Contracts
 *      Add dependent generation options, that apply pre-module simulation
 * TODO - Wave 6:
 *      Suite Options loading during startup, during the first load of a newer version (use a SuiteOption to track)
 *      Add MegaMek Options as a panel during the startup
 * TODO - Wave 7:
 *      Implement Era-based Part Generators
 *      Implement Surprises
 *      Implement Mystery Boxes
 *      Generate spare personnel (?)
 *      Optional: Mercenaries may customize their 'Mechs, with clantech if enabled only post-3055
 *
 * @author Justin "Windchild" Bowen
 */
public abstract class AbstractCompanyGenerator {
    //region Variable Declarations
    private final CompanyGenerationMethod method;
    private final CompanyGenerationOptions options;
    private final AbstractPersonnelGenerator personnelGenerator;
    private final AbstractBattleMechWeightClassGenerator battleMechWeightClassGenerator;
    private final AbstractBattleMechQualityGenerator battleMechQualityGenerator;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractCompanyGenerator(final CompanyGenerationMethod method, final Campaign campaign,
                                       final CompanyGenerationOptions options) {
        this.method = method;
        this.options = options;
        this.personnelGenerator = campaign.getPersonnelGenerator(createFactionSelector(), createPlanetSelector());
        this.battleMechWeightClassGenerator = getOptions().getBattleMechWeightClassGenerationMethod().getGenerator();
        this.battleMechQualityGenerator = getOptions().getBattleMechQualityGenerationMethod().getGenerator();
    }
    //endregion Constructors

    //region Getters/Setters
    public CompanyGenerationMethod getMethod() {
        return method;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }

    public AbstractPersonnelGenerator getPersonnelGenerator() {
        return personnelGenerator;
    }

    /**
     * @return a newly created Faction Selector
     */
    private AbstractFactionSelector createFactionSelector() {
        return getOptions().getRandomOriginOptions().isRandomizeOrigin()
                ? new RangedFactionSelector(getOptions().getRandomOriginOptions())
                : new DefaultFactionSelector(getOptions().getRandomOriginOptions());
    }

    /**
     * @return a newly created Planet Selector based on the provided options
     */
    private AbstractPlanetSelector createPlanetSelector() {
        return getOptions().getRandomOriginOptions().isRandomizeOrigin()
                ? new RangedPlanetSelector(getOptions().getRandomOriginOptions())
                : new DefaultPlanetSelector(getOptions().getRandomOriginOptions());
    }

    public AbstractBattleMechWeightClassGenerator getBattleMechWeightClassGenerator() {
        return battleMechWeightClassGenerator;
    }

    public AbstractBattleMechQualityGenerator getBattleMechQualityGenerator() {
        return battleMechQualityGenerator;
    }
    //endregion Getters/Setters

    //region Determination Methods
    /**
     * @return the number of lances to generate
     */
    private int determineNumberOfLances() {
        return (getOptions().getCompanyCount() * getOptions().getLancesPerCompany())
                + getOptions().getIndividualLanceCount()
                + (getOptions().isGenerateMercenaryCompanyCommandLance() ? 1 : 0);
    }

    /**
     * @return the number of Captains
     */
    private int determineNumberOfCaptains() {
        return getOptions().isGenerateCaptains()
                ? Math.max((getOptions().getCompanyCount()
                        - (getOptions().isGenerateMercenaryCompanyCommandLance() ? 0 : 1)), 0)
                : 0;
    }
    //endregion Determination Methods

    //region Personnel
    /**
     * @param campaign the campaign to use to generate the personnel
     * @return the list of generated personnel within their individual trackers
     */
    public List<CompanyGenerationPersonTracker> generatePersonnel(final Campaign campaign) {
        final List<CompanyGenerationPersonTracker> trackers = generateCombatPersonnel(campaign);
        trackers.addAll(generateSupportPersonnel(campaign));
        return trackers;
    }

    //region Combat Personnel
    /**
     * @param campaign the campaign to use to generate the combat personnel
     * @return the list of generated combat personnel within their individual trackers
     */
    private List<CompanyGenerationPersonTracker> generateCombatPersonnel(final Campaign campaign) {
        final int numMechWarriors = determineNumberOfLances() * getOptions().getLanceSize();

        final List<CompanyGenerationPersonTracker> initialTrackers = IntStream.range(0, numMechWarriors)
                .mapToObj(i -> new CompanyGenerationPersonTracker(
                        campaign.newPerson(PersonnelRole.MECHWARRIOR, getPersonnelGenerator())))
                .collect(Collectors.toList());

        final List<CompanyGenerationPersonTracker> sortedTrackers = new ArrayList<>();

        Comparator<CompanyGenerationPersonTracker> personnelSorter;

        // First, Assign the Company Commander
        if (getOptions().isAssignBestCompanyCommander()) {
            // Tactical Genius makes for the best commanders
            personnelSorter = Comparator.comparing(t -> t.getPerson().getOptions().booleanOption(OptionsConstants.MISC_TACTICAL_GENIUS));

            // Then prioritize either combat or command skills based on the selected option
            if (getOptions().isPrioritizeCompanyCommanderCombatSkills()) {
                personnelSorter = personnelSorter
                        .thenComparingInt(t -> t.getPerson().getExperienceLevel(campaign, false))
                        .thenComparingInt(t -> Stream.of(SkillType.S_LEADER, SkillType.S_STRATEGY, SkillType.S_TACTICS)
                                .mapToInt(s -> t.getPerson().getSkillLevel(s)).sum());
            } else {
                personnelSorter = personnelSorter
                        .thenComparingInt(t -> Stream.of(SkillType.S_LEADER, SkillType.S_STRATEGY, SkillType.S_TACTICS)
                                .mapToInt(s -> t.getPerson().getSkillLevel(s)).sum())
                        .thenComparingInt(t -> t.getPerson().getExperienceLevel(campaign, false));
            }
            // Always need to reverse it at the end
            personnelSorter = personnelSorter.reversed();

            // Find the best commander using the minimum value from the comparator, use a fallback
            // that can never occur. Then remove the commander from the initial trackers
            sortedTrackers.add(initialTrackers.stream()
                    .min(personnelSorter)
                    .orElse(new CompanyGenerationPersonTracker(
                            campaign.newPerson(PersonnelRole.MECHWARRIOR, getPersonnelGenerator()))));
            initialTrackers.remove(sortedTrackers.get(0));
        }

        // Second, Assign the Officers
        if (getOptions().isAssignBestOfficers()) {
            // Tactical Genius makes for the best officers
            personnelSorter = Comparator.comparing(t ->
                    t.getPerson().getOptions().booleanOption(OptionsConstants.MISC_TACTICAL_GENIUS));
            // Then prioritize either combat or command skills based on the selected option
            if (getOptions().isPrioritizeOfficerCombatSkills()) {
                personnelSorter = personnelSorter
                        .thenComparingInt(t -> t.getPerson().getExperienceLevel(campaign, false))
                        .thenComparingInt(t -> Stream.of(SkillType.S_LEADER, SkillType.S_STRATEGY, SkillType.S_TACTICS)
                                .mapToInt(s -> t.getPerson().getSkillLevel(s)).sum());
            } else {
                personnelSorter = personnelSorter
                        .thenComparingInt(t -> Stream.of(SkillType.S_LEADER, SkillType.S_STRATEGY, SkillType.S_TACTICS)
                                .mapToInt(s -> t.getPerson().getSkillLevel(s)).sum())
                        .thenComparingInt(t -> t.getPerson().getExperienceLevel(campaign, false));
            }
            // Always need to reverse it at the end
            personnelSorter = personnelSorter.reversed();

            // Sort the current trackers based on the provided sorter, then select one per lance
            // minus the one for taken by the company commander if they're already assigned
            sortedTrackers.addAll(initialTrackers.stream()
                    .sorted(personnelSorter)
                    .collect(Collectors.toList())
                    .subList(0, determineNumberOfLances() - (getOptions().isAssignBestCompanyCommander() ? 1 : 0)));

            // Finally, remove the officers from the initial trackers
            initialTrackers.removeAll(sortedTrackers);
        }

        // Default Sort is Tactical Genius First
        personnelSorter = Comparator.comparing(t ->
                t.getPerson().getOptions().booleanOption(OptionsConstants.MISC_TACTICAL_GENIUS));
        if (getOptions().isAssignMostSkilledToPrimaryLances()) {
            // Unless we are prioritizing the most skilled, then we also care about experience level
            personnelSorter = personnelSorter
                    .thenComparingInt(t -> t.getPerson().getExperienceLevel(campaign, false));
        }

        // Sort whatever is left of the initial trackers before adding them to the initial trackers
        initialTrackers.sort(personnelSorter.reversed());
        sortedTrackers.addAll(initialTrackers);

        // Then generate the individuals based on their sorted trackers
        generateCommandingOfficer(campaign, sortedTrackers.get(0), numMechWarriors);
        generateOfficers(sortedTrackers);
        generateStandardMechWarriors(campaign, sortedTrackers);

        return sortedTrackers;
    }

    /**
     * Turns a person into the commanding officer of the force being generated
     * 1) Assigns the Commander flag (if that option is true)
     * 2) Improves Gunnery and Piloting by one level
     * 3) Gets two random officer skill increases
     * 4) Gets the highest rank possible assigned to them
     *
     * @param campaign the campaign to use in generating the commanding officer
     * @param tracker the commanding officer's tracker
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    private void generateCommandingOfficer(final Campaign campaign,
                                           final CompanyGenerationPersonTracker tracker,
                                           final int numMechWarriors) {
        tracker.setPersonType(CompanyGenerationPersonType.MECHWARRIOR_COMPANY_COMMANDER);
        tracker.getPerson().setCommander(getOptions().isAssignCompanyCommanderFlag());
        tracker.getPerson().improveSkill(SkillType.S_GUN_MECH);
        tracker.getPerson().improveSkill(SkillType.S_PILOT_MECH);
        assignRandomOfficerSkillIncrease(tracker, 2);

        if (getOptions().isAutomaticallyAssignRanks()) {
            generateCommandingOfficerRank(campaign, tracker, numMechWarriors);
        }
    }

    /**
     * @param campaign the campaign to use in generating the commanding officer's rank
     * @param tracker the commanding officer's tracker
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    protected abstract void generateCommandingOfficerRank(final Campaign campaign,
                                                          final CompanyGenerationPersonTracker tracker,
                                                          final int numMechWarriors);

    /**
     * This generates the initial officer list and assigns the type
     *
     * @param trackers the list of all generated personnel in their trackers
     */
    private void generateOfficers(final List<CompanyGenerationPersonTracker> trackers) {
        // First, we need to determine the captain threshold
        final int captainThreshold = determineNumberOfCaptains() + 1;
        // Starting at 1, as 0 is the mercenary company commander
        for (int i = 1; i < determineNumberOfLances(); i++) {
            // Set the Person Type on the tracker, so we can properly reroll later
            trackers.get(i).setPersonType((i < captainThreshold)
                    ? CompanyGenerationPersonType.MECHWARRIOR_CAPTAIN
                    : CompanyGenerationPersonType.MECHWARRIOR_LIEUTENANT);
            // Generate the individual officer
            generateOfficer(trackers.get(i));
        }
    }

    /**
     * This generates an officer based on the provided options.
     *
     * Custom addition for larger generation:
     * For every company (with a mercenary company command lance) or for every company
     * after the first (as the mercenary company commander is the leader of that company) you
     * generate a O4 - Captain, provided that captain generation is enabled. These get
     * two officer skill boosts instead of 1, and the rank of O4 - Captain instead of O3 - Lieutenant.
     *
     * An Officer gets:
     * 1) An increase of one to either the highest or lowest skill of gunnery or piloting, depending
     * on the set options
     * 2) Two random officer skill increases if they are a Captain, otherwise they get one
     * 3) A rank of O4 - Captain for Captains, otherwise O3 - Lieutenant
     *
     * @param tracker the officer's tracker
     */
    private void generateOfficer(final CompanyGenerationPersonTracker tracker) {
        if (!tracker.getPersonType().isOfficer()) {
            LogManager.getLogger().error(tracker.getPerson().getFullTitle()
                    + " is not a valid officer for the officer generation, cannot generate them as an officer.");
            return;
        }

        // Improve Skills
        final Skill gunnery = tracker.getPerson().getSkill(SkillType.S_GUN_MECH);
        final Skill piloting = tracker.getPerson().getSkill(SkillType.S_PILOT_MECH);
        if ((gunnery == null) && (piloting != null)) {
            tracker.getPerson().improveSkill(SkillType.S_GUN_MECH);
        } else if ((gunnery != null) && (piloting == null)) {
            tracker.getPerson().improveSkill(SkillType.S_PILOT_MECH);
        } else if (gunnery == null) {
            // Both are null... this shouldn't occur. In this case, boost both
            tracker.getPerson().improveSkill(SkillType.S_GUN_MECH);
            tracker.getPerson().improveSkill(SkillType.S_PILOT_MECH);
        } else {
            tracker.getPerson().improveSkill((((gunnery.getLevel() > piloting.getLevel())
                    && getOptions().isApplyOfficerStatBonusToWorstSkill()) ? piloting : gunnery)
                    .getType().getName());
        }

        if (tracker.getPersonType().isMechWarriorCaptain()) {
            // Assign Random Officer Skill Increase
            assignRandomOfficerSkillIncrease(tracker, 2);

            if (getOptions().isAutomaticallyAssignRanks()) {
                // Assign Rank of O4 - Captain
                tracker.getPerson().setRank(Rank.RWO_MAX + 4);
            }
        } else {
            // Assign Random Officer Skill Increase
            assignRandomOfficerSkillIncrease(tracker, 1);

            if (getOptions().isAutomaticallyAssignRanks()) {
                // Assign Rank of O3 - Lieutenant
                tracker.getPerson().setRank(Rank.RWO_MAX + 3);
            }
        }
    }

    /**
     * This randomly assigns officer skill increases during officer creation.
     * The skill level is improved by one level per roll, but if the skill is newly acquired
     * it applies a second boost so that the value is set to 1.
     *
     * @param tracker the tracker to assign the skill increases to
     * @param boosts the number of boosts to apply
     */
    private void assignRandomOfficerSkillIncrease(final CompanyGenerationPersonTracker tracker,
                                                  final int boosts) {
        for (int i = 0; i < boosts; i++) {
            switch (Utilities.dice(1, 3)) {
                case 0:
                    tracker.getPerson().improveSkill(SkillType.S_LEADER);
                    if (tracker.getPerson().getSkillLevel(SkillType.S_LEADER) == 0) {
                        tracker.getPerson().improveSkill(SkillType.S_LEADER);
                    }
                    break;
                case 1:
                    tracker.getPerson().improveSkill(SkillType.S_STRATEGY);
                    if (tracker.getPerson().getSkillLevel(SkillType.S_STRATEGY) == 0) {
                        tracker.getPerson().improveSkill(SkillType.S_STRATEGY);
                    }
                    break;
                case 2:
                    tracker.getPerson().improveSkill(SkillType.S_TACTICS);
                    if (tracker.getPerson().getSkillLevel(SkillType.S_TACTICS) == 0) {
                        tracker.getPerson().improveSkill(SkillType.S_TACTICS);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Sets up standard MechWarriors from the provided trackers
     *
     * @param campaign the campaign to generate the MechWarriors based on
     * @param trackers the list of all generated trackers
     */
    private void generateStandardMechWarriors(final Campaign campaign,
                                              final List<CompanyGenerationPersonTracker> trackers) {
        for (final CompanyGenerationPersonTracker tracker : trackers) {
            if (!tracker.getPersonType().isMechWarrior()) {
                continue;
            }

            generateStandardMechWarrior(campaign, tracker);
        }
    }

    /**
     * This sets up a standard MechWarrior
     * 1) Assigns rank of E12 - Sergeant, or E4 for Clan, WoB, and ComStar
     *
     * @param campaign the campaign to generate the MechWarrior based on
     * @param tracker the MechWarrior tracker to set up
     */
    private void generateStandardMechWarrior(final Campaign campaign,
                                             final CompanyGenerationPersonTracker tracker) {
        if (getOptions().isAutomaticallyAssignRanks()) {
            tracker.getPerson().setRank((campaign.getFaction().isComStarOrWoB() || campaign.getFaction().isClan())
                    ? 4 : 12);
        }
    }
    //endregion Combat Personnel

    //region Support Personnel
    /**
     * @param campaign the campaign to generate from
     * @return a list of all support personnel
     */
    private List<CompanyGenerationPersonTracker> generateSupportPersonnel(final Campaign campaign) {
        final List<CompanyGenerationPersonTracker> trackers = new ArrayList<>();

        for (final Entry<PersonnelRole, Integer> entry : getOptions().getSupportPersonnel().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                trackers.add(new CompanyGenerationPersonTracker(CompanyGenerationPersonType.SUPPORT,
                        generateSupportPerson(campaign, entry.getKey())));
            }
        }
        return trackers;
    }

    /**
     * @param campaign the campaign to generate from
     * @param role the created person's primary role
     * @return the newly created support person with the provided role
     */
    private Person generateSupportPerson(final Campaign campaign, final PersonnelRole role) {
        final Person person = campaign.newPerson(role, getPersonnelGenerator());
        // All support personnel get assigned Corporal or equivalent as their rank
        if (getOptions().isAutomaticallyAssignRanks()) {
            switch (campaign.getRankSystem().getCode()) {
                case "CCWH":
                case "CLAN":
                    break;
                case "CG":
                case "WOBM":
                case "MAF":
                    person.setRank(4);
                    break;
                default:
                    person.setRank(8);
                    break;
            }
        }
        return person;
    }

    /**
     * @param campaign the campaign to use in creating the assistants
     * @param trackers the trackers to add the newly created assistants to
     */
    private void generateAssistants(final Campaign campaign,
                                    final List<CompanyGenerationPersonTracker> trackers) {
        // If you don't want to use pooled assistants, then this generates them as personnel instead
        if (getOptions().isPoolAssistants()) {
            return;
        }

        final int assistantRank;
        switch (campaign.getRankSystem().getCode()) {
            case "CCWH":
            case "CLAN":
                assistantRank = 0;
                break;
            case "CG":
            case "WOBM":
            case "MAF":
                assistantRank = 4;
                break;
            default:
                assistantRank = 2;
                break;
        }

        for (int i = 0; i < campaign.getAstechNeed(); i++) {
            final Person astech = campaign.newPerson(PersonnelRole.ASTECH, getPersonnelGenerator());
            if (getOptions().isAutomaticallyAssignRanks()) {
                astech.setRank(assistantRank);
            }
            trackers.add(new CompanyGenerationPersonTracker(CompanyGenerationPersonType.ASSISTANT, astech));
        }

        for (int i = 0; i < campaign.getMedicsNeed(); i++) {
            final Person medic = campaign.newPerson(PersonnelRole.MEDIC, getPersonnelGenerator());
            if (getOptions().isAutomaticallyAssignRanks()) {
                medic.setRank(assistantRank);
            }
            trackers.add(new CompanyGenerationPersonTracker(CompanyGenerationPersonType.ASSISTANT, medic));
        }
    }
    //endregion Support Personnel

    /**
     * This does the final personnel processing
     * @param campaign the campaign to use in processing and to add the personnel to
     * @param trackers ALL trackers for the campaign
     */
    private void finalizePersonnel(final Campaign campaign,
                                   final List<CompanyGenerationPersonTracker> trackers) {
        // Assign the founder flag if we need to
        if (getOptions().isAssignFounderFlag()) {
            trackers.forEach(tracker -> tracker.getPerson().setFounder(true));
        }

        // Recruit all the personnel, GM-style so that the initial hiring cost is calculated as part
        // of the financial model
        trackers.forEach(t -> campaign.recruitPerson(t.getPerson(), true));

        // Now that they are recruited, we can simulate backwards a few years and generate marriages
        // and children
        if (getOptions().isRunStartingSimulation()) {
            LocalDate date = campaign.getLocalDate().minusYears(getOptions().getSimulationDuration()).minusDays(1);
            while (date.isBefore(campaign.getLocalDate())) {
                date = date.plusDays(1);

                for (final CompanyGenerationPersonTracker tracker : trackers) {
                    if (getOptions().isSimulateRandomMarriages()) {
                        campaign.getMarriage().processNewDay(campaign, date, tracker.getPerson());
                    }

                    if (getOptions().isSimulateRandomProcreation()) {
                        campaign.getProcreation().processNewDay(campaign, date, tracker.getPerson());
                    }
                }
            }
        }
    }
    //endregion Personnel

    //region Units
    //region Unit Generation Parameters
    /**
     * This generates the unit generation parameters and assigns them to their trackers
     * @param trackers the list of all personnel trackers
     */
    public void generateUnitGenerationParameters(List<CompanyGenerationPersonTracker> trackers) {
        // First, we need to create the unit generation parameters
        final List<AtBRandomMechParameters> parameters = createUnitGenerationParameters(trackers);

        // Then, we need to separate out the best roll for the unit commander if that option is enabled
        if (getOptions().isAssignBestRollToCompanyCommander()) {
            int bestIndex = 0;
            AtBRandomMechParameters bestParameters = parameters.get(bestIndex);
            for (int i = 1; i < parameters.size(); i++) {
                final AtBRandomMechParameters checkParameters = parameters.get(i);
                if (bestParameters.isStarLeague() == checkParameters.isStarLeague()) {
                    if (bestParameters.getWeight() == checkParameters.getWeight()) {
                        if (bestParameters.getQuality() < checkParameters.getQuality()) {
                            bestParameters = checkParameters;
                            bestIndex = i;
                        }
                    } else if (bestParameters.getWeight() < checkParameters.getWeight()) {
                        bestParameters = checkParameters;
                        bestIndex = i;
                    }
                } else if (!bestParameters.isStarLeague() && checkParameters.isStarLeague()) {
                    bestParameters = checkParameters;
                    bestIndex = i;
                }
            }

            if (bestIndex != 0) {
                Collections.swap(parameters, 0, bestIndex);
            }
        }

        // Now, we need to apply the various sorts based on the provided options
        Comparator<AtBRandomMechParameters> parametersComparator = (p1, p2) -> 0;

        if (getOptions().isSortStarLeagueUnitsFirst()) {
            parametersComparator = parametersComparator.thenComparing(AtBRandomMechParameters::isStarLeague);
        }

        if (getOptions().isGroupByWeight()) {
            parametersComparator = parametersComparator.thenComparingInt(AtBRandomMechParameters::getWeight);
        }

        if (getOptions().isGroupByQuality()) {
            parametersComparator = parametersComparator.thenComparingInt(AtBRandomMechParameters::getQuality);
        }

        parametersComparator = parametersComparator.reversed();

        if (getOptions().isKeepOfficerRollsSeparate()) {
            final int firstNonOfficer = determineNumberOfLances();
            parameters.subList(getOptions().isAssignBestRollToCompanyCommander() ? 1 : 0, firstNonOfficer)
                    .sort(parametersComparator);
            parameters.subList(firstNonOfficer, parameters.size()).sort(parametersComparator);
        } else {
            // Officer Rolls are not separated. However, if the unit commander is assigned the best
            // roll we don't sort the unit commander, just the rest of the rolls
            if (getOptions().isAssignBestRollToCompanyCommander()) {
                parameters.subList(1, parameters.size()).sort(parametersComparator);
            } else {
                parameters.sort(parametersComparator);
            }

            trackers = sortPersonnelIntoLances(trackers);
        }

        // Now that everything is nicely sorted, we can set the parameters. Parameters will ALWAYS
        // be of a length equal to or less than that of trackers, as we don't generate parameters
        // for support personnel.
        for (int i = 0; i < parameters.size(); i++) {
            trackers.get(i).setParameters(parameters.get(i));
        }
    }

    /**
     * @param trackers the list of all personnel trackers
     * @return a list of the generated RandomMechParameters. These have NOT been assigned to the
     * individual trackers
     */
    private List<AtBRandomMechParameters> createUnitGenerationParameters(
            final List<CompanyGenerationPersonTracker> trackers) {
        return trackers.stream().filter(tracker -> tracker.getPersonType().isCombat())
                .map(this::createUnitGenerationParameter).collect(Collectors.toList());
    }

    /**
     * Creates an individual set of parameters, rerolling the weight if Star League
     * (EntityWeightClass.WEIGHT_SUPER_HEAVY) is rolled originally.
     *
     * @param tracker the tracker to generate the parameters based on
     * @return the created parameters
     */
    private AtBRandomMechParameters createUnitGenerationParameter(
            final CompanyGenerationPersonTracker tracker) {
        final AtBRandomMechParameters parameters = new AtBRandomMechParameters(
                getOptions().isOnlyGenerateStarLeagueMechs() ? EntityWeightClass.WEIGHT_SUPER_HEAVY
                        : rollBattleMechWeight(tracker, !getOptions().isNeverGenerateStarLeagueMechs()),
                rollBattleMechQuality(tracker));
        if (parameters.isStarLeague()) {
            parameters.setWeight(rollBattleMechWeight(tracker, false));
        }
        return parameters;
    }

    /**
     * @param tracker the tracker to roll based on
     * @param initialRoll if this isn't the initial roll, then we need to cap the Entity Weight
     *                    Class at EntityWeightClass.WEIGHT_ASSAULT
     * @return the weight to use in generating the BattleMech, which may be
     * EntityWeightClass.WEIGHT_NONE to not generate a BattleMech or
     * EntityWeightClass.WEIGHT_SUPER_HEAVY to generate a Star League BattleMech
     */
    private int rollBattleMechWeight(final CompanyGenerationPersonTracker tracker,
                                     final boolean initialRoll) {
        final int roll = Utilities.dice(2, 6) + getUnitGenerationParameterModifier(tracker);
        final int entityWeightClass = getBattleMechWeightClassGenerator().generate(roll);
        return initialRoll ? entityWeightClass : Math.min(entityWeightClass, EntityWeightClass.WEIGHT_ASSAULT);
    }

    /**
     * @param tracker the tracker to roll based on
     * @return the quality to use in generating the BattleMech
     */
    private int rollBattleMechQuality(final CompanyGenerationPersonTracker tracker) {
        return getBattleMechQualityGenerator().generate(
                Utilities.dice(2, 6) + getUnitGenerationParameterModifier(tracker));
    }

    /**
     * @param tracker the tracker to get the unit generation parameter modifier for
     * @return the modifier value
     */
    private int getUnitGenerationParameterModifier(final CompanyGenerationPersonTracker tracker) {
        switch (tracker.getPersonType()) {
            case MECHWARRIOR_COMPANY_COMMANDER:
                return 2;
            case MECHWARRIOR_CAPTAIN:
            case MECHWARRIOR_LIEUTENANT:
                return 1;
            case MECHWARRIOR:
                return 0;
            default:
                // Shouldn't be hit, but a safety for attempting non-combat generation
                LogManager.getLogger().error("Attempting to generate a unit for a " + tracker.getPersonType() + ", returning a -20 modifier");
                return -20;
        }
    }

    /**
     * @param trackers the trackers to sort into their lances
     * @return a new List containing the sorted personnel
     */
    private List<CompanyGenerationPersonTracker> sortPersonnelIntoLances(
            final List<CompanyGenerationPersonTracker> trackers) {
        // We start by creating the return list, the Captains list, the Lieutenants list
        // and the MechWarriors list
        final List<CompanyGenerationPersonTracker> sortedTrackers = new ArrayList<>();
        final List<CompanyGenerationPersonTracker> captains = trackers.stream().filter(tracker ->
                tracker.getPersonType().isMechWarriorCaptain()).collect(Collectors.toList());
        final List<CompanyGenerationPersonTracker> lieutenants = trackers.stream().filter(tracker ->
                tracker.getPersonType().isMechWarriorLieutenant()).collect(Collectors.toList());
        final List<CompanyGenerationPersonTracker> standardMechWarriors = trackers.stream().filter(tracker ->
                tracker.getPersonType().isMechWarrior()).collect(Collectors.toList());

        // Sort Command Lance
        organizeTrackersIntoLance(sortedTrackers, trackers.get(0), standardMechWarriors);

        // If the command lance is part of a company, we sort the rest of that company immediately
        if (!getOptions().isGenerateMercenaryCompanyCommandLance() && (getOptions().getCompanyCount() > 0)) {
            for (int i = 1; i < getOptions().getLancesPerCompany(); i++) {
                organizeTrackersIntoLance(sortedTrackers, lieutenants.remove(0), standardMechWarriors);
            }
        }

        // Sort into Companies
        while (!captains.isEmpty()) {
            // Assign the Captain's Lance
            organizeTrackersIntoLance(sortedTrackers, captains.remove(0), standardMechWarriors);
            // Then assign the other lances
            for (int y = 1; y < getOptions().getLancesPerCompany(); y++) {
                organizeTrackersIntoLance(sortedTrackers, lieutenants.remove(0), standardMechWarriors);
            }
        }

        // Sort any individual lances
        while (!lieutenants.isEmpty()) {
            organizeTrackersIntoLance(sortedTrackers, lieutenants.remove(0), standardMechWarriors);
        }

        return sortedTrackers;
    }

    /**
     * @param sortedTrackers the list to add the now sorted lance to
     * @param officer the officer to lead the lance
     * @param standardMechWarriors the list of normal MechWarriors who can be assigned to this lance.
     */
    private void organizeTrackersIntoLance(final List<CompanyGenerationPersonTracker> sortedTrackers,
                                           final CompanyGenerationPersonTracker officer,
                                           final List<CompanyGenerationPersonTracker> standardMechWarriors) {
        sortedTrackers.add(officer);
        if (standardMechWarriors.size() <= getOptions().getLanceSize() - 1) {
            sortedTrackers.addAll(standardMechWarriors);
            standardMechWarriors.clear();
        } else {
            for (int i = 1; (i < getOptions().getLanceSize()) && !standardMechWarriors.isEmpty(); i++) {
                sortedTrackers.add(standardMechWarriors.remove(0));
            }
        }
    }
    //endregion Unit Generation Parameters

    //region Entities
    /**
     * @param campaign the campaign to generate for
     * @param trackers the list of all personnel trackers
     */
    public void generateEntities(final Campaign campaign,
                                 final List<CompanyGenerationPersonTracker> trackers) {
        trackers.stream().filter(tracker -> tracker.getPersonType().isCombat())
                .forEach(tracker -> generateEntity(campaign, tracker));
    }

    /**
     * This generates a single entity and assigns it to the specified tracker.
     * @param campaign the campaign to generate for
     * @param tracker the tracker to generate based on the parameters and to assign the result to
     */
    private void generateEntity(final Campaign campaign,
                                final CompanyGenerationPersonTracker tracker) {
        tracker.setEntity((tracker.getParameters() == null) ? null
            : generateEntity(campaign, tracker.getParameters(), tracker.getPerson().getOriginFaction()));
    }

    /**
     * This generates a single entity, thus allowing for individual rerolls
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction the faction to generate the Entity from
     * @return the entity generated, or null otherwise
     */
    private @Nullable Entity generateEntity(final Campaign campaign,
                                            final AtBRandomMechParameters parameters,
                                            final Faction faction) {
        // Ultra-Light means no mech generated
        if (parameters.getWeight() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
            return null;
        }

        final MechSummary mechSummary = generateMechSummary(campaign, parameters, faction);

        if (mechSummary == null) {
            LogManager.getLogger().error("Failed to generate an entity due to a null mech summary for faction " + faction.getShortName());
            return null;
        }

        try {
            return new MechFileParser(mechSummary.getSourceFile(), mechSummary.getEntryName()).getEntity();
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to generate entity", ex);
            return null;
        }
    }

    /**
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction the faction to generate the mech from
     * @return the MechSummary generated from the provided parameters, or null if generation fails
     */
    protected abstract @Nullable MechSummary generateMechSummary(final Campaign campaign,
                                                                 final AtBRandomMechParameters parameters,
                                                                 final Faction faction);

    /**
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction the faction code to use in generation
     * @param year the year to use in generation
     * @return the MechSummary generated from the provided parameters, or null if generation fails
     */
    protected @Nullable MechSummary generateMechSummary(final Campaign campaign,
                                                        final AtBRandomMechParameters parameters,
                                                        final String faction, int year) {
        Predicate<MechSummary> filter = ms ->
                (!campaign.getCampaignOptions().limitByYear() || (year > ms.getYear()));
        if (getOptions().isOnlyGenerateOmniMechs()) {
            filter = filter.and(ms -> "Omni".equalsIgnoreCase(ms.getUnitSubType()));
        }

        return campaign.getUnitGenerator().generate(faction, UnitType.MEK,
                parameters.getWeight(), year, parameters.getQuality(), filter);
    }
    //endregion Entities

    /**
     * @param campaign the campaign to add the units to
     * @param trackers the list of trackers to assign to their units
     * @return the list of created units
     */
    private List<Unit> createUnits(final Campaign campaign,
                                   final List<CompanyGenerationPersonTracker> trackers) {
        final List<Unit> units = new ArrayList<>();
        for (final CompanyGenerationPersonTracker tracker : trackers) {
            if (tracker.getEntity() == null) {
                continue;
            }

            final Unit unit = campaign.addNewUnit(tracker.getEntity(), false, 0);
            unit.addPilotOrSoldier(tracker.getPerson());
            if (getOptions().isGenerateUnitsAsAttached()) {
                tracker.getPerson().setOriginalUnit(unit);
            }
            units.add(unit);
        }
        return units;
    }

    /**
     * @param trackers the list of trackers including the support 'Mech techs
     * @param units the list of units to have techs assigned to (order does not matter)
     */
    private void assignTechsToUnits(final List<CompanyGenerationPersonTracker> trackers,
                                    final List<Unit> units) {
        if (!getOptions().isAssignTechsToUnits()) {
            return;
        }

        final List<CompanyGenerationPersonTracker> mechTechs = trackers.parallelStream()
                .filter(tracker -> tracker.getPersonType().isSupport())
                .filter(tracker -> tracker.getPerson().getPrimaryRole().isMechTech())
                .collect(Collectors.toList());
        if (mechTechs.isEmpty()) {
            return;
        }

        units.sort(Comparator.comparingDouble(Unit::getMaintenanceTime));
        int numberMechTechs = mechTechs.size();
        for (int i = 0; (i < units.size()) && !mechTechs.isEmpty(); i++) {
            final Person mechTech = mechTechs.get(i % numberMechTechs).getPerson();
            if (mechTech.getMaintenanceTimeUsing() + units.get(i).getMaintenanceTime() <= Person.PRIMARY_ROLE_SUPPORT_TIME) {
                units.get(i).setTech(mechTech);
            } else {
                mechTechs.remove(i % numberMechTechs--);
            }
        }
    }
    //endregion Units

    //region Unit
    /**
     * This generates the TO&E structure, and assigns personnel to their individual lances.
     * @param campaign the campaign to generate the unit within
     * @param trackers a CLONED list of trackers properly organized into lances
     */
    private void generateUnit(final Campaign campaign,
                              final List<CompanyGenerationPersonTracker> trackers) {
        final Force originForce = campaign.getForce(0);
        final Alphabet[] alphabet = Alphabet.values();
        ForcePieceIcon background = null;

        if (getOptions().isGenerateForceIcons()) {
            if (campaign.getFaction().getLayeredForceIconBackgroundFilename() != null) {
                background = new ForcePieceIcon(LayeredForceIconLayer.BACKGROUND,
                        campaign.getFaction().getLayeredForceIconBackgroundCategory(),
                        campaign.getFaction().getLayeredForceIconBackgroundFilename());
            }

            // Create the Origin Force Icon
            if (getOptions().isGenerateOriginNodeForceIcon()) {
                final LayeredForceIcon layeredForceIcon = new LayeredForceIcon();

                // Logo / Type
                if (getOptions().isUseOriginNodeForceIconLogo()
                        && (campaign.getFaction().getLayeredForceIconLogoFilename() != null)) {
                    layeredForceIcon.getPieces().putIfAbsent(LayeredForceIconLayer.LOGO, new ArrayList<>());
                    layeredForceIcon.getPieces().get(LayeredForceIconLayer.LOGO)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.LOGO,
                                    campaign.getFaction().getLayeredForceIconLogoCategory(),
                                    campaign.getFaction().getLayeredForceIconLogoFilename()));
                } else {
                    layeredForceIcon.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
                    layeredForceIcon.getPieces().get(LayeredForceIconLayer.TYPE)
                            .add(new ForcePieceIcon(LayeredForceIconLayer.TYPE,
                                    MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                                    MHQConstants.LAYERED_FORCE_ICON_BATTLEMECH_CENTER_FILENAME));
                }

                // Background
                if (background != null) {
                    layeredForceIcon.getPieces().putIfAbsent(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
                    layeredForceIcon.getPieces().get(LayeredForceIconLayer.BACKGROUND).add(background.clone());
                }

                originForce.setForceIcon(layeredForceIcon);
            }
        }

        // Generate the Mercenary Company Command Lance
        if (getOptions().isGenerateMercenaryCompanyCommandLance()) {
            final Force commandLance = createLance(campaign, originForce, trackers, campaign.getName()
                    + resources.getString("AbstractCompanyGenerator.CommandLance.text"), background);
            if (getOptions().isGenerateForceIcons()
                    && (commandLance.getForceIcon() instanceof LayeredForceIcon)) {
                final LayeredForceIcon icon = (LayeredForceIcon) commandLance.getForceIcon();
                icon.getPieces().putIfAbsent(LayeredForceIconLayer.ALPHANUMERIC, new ArrayList<>());
                icon.getPieces().get(LayeredForceIconLayer.ALPHANUMERIC)
                        .add(new ForcePieceIcon(LayeredForceIconLayer.ALPHANUMERIC,
                                MHQConstants.LAYERED_FORCE_ICON_ALPHANUMERIC_BOTTOM_RIGHT_PATH,
                                MHQConstants.LAYERED_FORCE_ICON_ALPHANUMERIC_HQ_FILENAME));
            }
        }

        // Create Companies
        for (int i = 0; i < getOptions().getCompanyCount(); i++) {
            final Force company = new Force(getOptions().getForceNamingMethod().getValue(alphabet[i])
                    + resources.getString("AbstractCompanyGenerator.Company.text"));
            campaign.addForce(company, originForce);
            for (int y = 0; y < getOptions().getLancesPerCompany(); y++) {
                createLance(campaign, company, trackers, alphabet[y], background);
            }

            if (getOptions().isGenerateForceIcons()) {
                createLayeredForceIcon(campaign, company, false, background);
            }
        }

        // Create Individual Lances
        for (int i = 0 ; i < getOptions().getIndividualLanceCount(); i++) {
            createLance(campaign, originForce, trackers, alphabet[i + getOptions().getCompanyCount()], background);
        }
    }

    /**
     * This creates a lance with a standard name
     * @param campaign the campaign to generate the unit within
     * @param head the force to append the new lance to
     * @param trackers the list of trackers, properly ordered to be assigned to the lance
     * @param alphabet the alphabet value to determine the lance name from
     * @param background the background force piece icon, which is null when there's no valid background
     */
    private void createLance(final Campaign campaign, final Force head,
                             final List<CompanyGenerationPersonTracker> trackers,
                             final Alphabet alphabet, final @Nullable ForcePieceIcon background) {
        createLance(campaign, head, trackers,
                getOptions().getForceNamingMethod().getValue(alphabet)
                        + resources.getString("AbstractCompanyGenerator.Lance.text"),
                background);
    }

    /**
     * @param campaign the campaign to generate the unit within
     * @param head the force to append the new lance to
     * @param trackers the list of trackers, properly ordered to be assigned to the lance
     * @param name the lance's name
     * @param background the background force piece icon, which is null when there's no valid background
     * @return the newly created lance
     */
    private Force createLance(final Campaign campaign, final Force head,
                              final List<CompanyGenerationPersonTracker> trackers,
                              final String name, final @Nullable ForcePieceIcon background) {
        final Force lance = new Force(name);
        campaign.addForce(lance, head);
        for (int i = 0; (i < getOptions().getLanceSize()) && !trackers.isEmpty(); i++) {
            campaign.addUnitToForce(trackers.remove(0).getPerson().getUnit(), lance);
        }

        if (getOptions().isGenerateForceIcons()) {
            createLayeredForceIcon(campaign, lance, true, background);
        }

        return lance;
    }

    /**
     * This creates a layered force icon for a force
     * @param campaign the campaign the force is a part of
     * @param force the force to create a layered force icon for
     * @param isLance whether the force is a lance or a company
     * @param background the background force piece icon, which is null when there's no valid background
     */
    private void createLayeredForceIcon(final Campaign campaign, final Force force,
                                        final boolean isLance,
                                        final @Nullable ForcePieceIcon background) {
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return;
        }

        final LayeredForceIcon layeredForceIcon = new LayeredForceIcon();

        // Type
        final String filename = String.format("%s.png",
                EntityWeightClass.getClassName(determineForceWeightClass(campaign, force, isLance)));
        try {
            layeredForceIcon.getPieces().putIfAbsent(LayeredForceIconLayer.TYPE, new ArrayList<>());
            if (MHQStaticDirectoryManager.getForceIcons().getItem(
                    LayeredForceIconLayer.TYPE.getLayerPath() + MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                    filename) == null) {
                layeredForceIcon.getPieces().get(LayeredForceIconLayer.TYPE).add(
                        new ForcePieceIcon(LayeredForceIconLayer.TYPE,
                                MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                                MHQConstants.LAYERED_FORCE_ICON_BATTLEMECH_CENTER_FILENAME));
            } else {
                layeredForceIcon.getPieces().get(LayeredForceIconLayer.TYPE).add(
                        new ForcePieceIcon(LayeredForceIconLayer.TYPE,
                                MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH,
                                MHQConstants.LAYERED_FORCE_ICON_BATTLEMECH_LEFT_FILENAME));
                layeredForceIcon.getPieces().get(LayeredForceIconLayer.TYPE).add(
                        new ForcePieceIcon(LayeredForceIconLayer.TYPE,
                                MHQConstants.LAYERED_FORCE_ICON_TYPE_STRAT_OPS_PATH, filename));
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("Cannot create a layered force icon, setting " + force + " to the default", ex);
            force.setForceIcon(new LayeredForceIcon());
            return;
        }

        // Formation
        layeredForceIcon.getPieces().putIfAbsent(LayeredForceIconLayer.FORMATION, new ArrayList<>());
        if (campaign.getFaction().isClan()) {
            layeredForceIcon.getPieces().get(LayeredForceIconLayer.FORMATION)
                    .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION,
                            MHQConstants.LAYERED_FORCE_ICON_FORMATION_CLAN_PATH,
                            isLance ? MHQConstants.LAYERED_FORCE_ICON_FORMATION_STAR_FILENAME
                                    : MHQConstants.LAYERED_FORCE_ICON_FORMATION_TRINARY_FILENAME));
        } else if (campaign.getFaction().isComStarOrWoB()) {
            layeredForceIcon.getPieces().get(LayeredForceIconLayer.FORMATION)
                    .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION,
                            MHQConstants.LAYERED_FORCE_ICON_FORMATION_COMSTAR_PATH,
                            isLance ? MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_II_FILENAME
                                    : MHQConstants.LAYERED_FORCE_ICON_FORMATION_LEVEL_III_FILENAME));
        } else {
            layeredForceIcon.getPieces().get(LayeredForceIconLayer.FORMATION)
                    .add(new ForcePieceIcon(LayeredForceIconLayer.FORMATION,
                            MHQConstants.LAYERED_FORCE_ICON_FORMATION_INNER_SPHERE_PATH,
                            isLance ? MHQConstants.LAYERED_FORCE_ICON_FORMATION_LANCE_FILENAME
                                    : MHQConstants.LAYERED_FORCE_ICON_FORMATION_COMPANY_FILENAME));
        }

        // Background
        if (background != null) {
            layeredForceIcon.getPieces().putIfAbsent(LayeredForceIconLayer.BACKGROUND, new ArrayList<>());
            layeredForceIcon.getPieces().get(LayeredForceIconLayer.BACKGROUND).add(background.clone());
        }

        force.setForceIcon(layeredForceIcon);
    }

    /**
     * This determines the weight class of a force (lance or company) based on the units within
     * @param campaign the campaign to determine based on
     * @param force the force to determine the weight class for
     * @param isLance whether the force is a lance or a company
     * @return the weight class of the force
     */
    private int determineForceWeightClass(final Campaign campaign, final Force force,
                                          final boolean isLance) {
        double weight = force.getAllUnits(true).stream().map(campaign::getUnit)
                .filter(unit -> (unit != null) && (unit.getEntity() != null))
                .mapToDouble(unit -> unit.getEntity().getWeight()).sum();
        weight = weight * 4.0 / (getOptions().getLanceSize() * (isLance ? 1 : getOptions().getLancesPerCompany()));
        final Entry<Integer, Integer> entry = getOptions().getForceWeightLimits().ceilingEntry((int) Math.round(weight));
        return (entry == null) ? EntityWeightClass.WEIGHT_SUPER_HEAVY : entry.getValue();
    }
    //endregion Unit

    //region Spares
    /**
     * This generates any mothballed spare entities for the force
     * @param campaign the campaign to generate for
     * @param trackers the trackers containing the generated combat entities
     * @return the list of all generated entities to mothball as spares
     */
    public List<Entity> generateMothballedEntities(final Campaign campaign,
                                                   final List<CompanyGenerationPersonTracker> trackers) {
        // Determine how many entities to generate
        final int numberMothballedEntities;
        if (getOptions().isGenerateMothballedSpareUnits()
                && (getOptions().getSparesPercentOfActiveUnits() > 0)) {
            // No free units for null rolls!
            numberMothballedEntities = Math.toIntExact(Math.round(
                    trackers.stream().map(CompanyGenerationPersonTracker::getEntity)
                            .filter(Objects::nonNull).count()
                    * (getOptions().getSparesPercentOfActiveUnits() / 100.0)));
        } else {
            numberMothballedEntities = 0;
        }

        // Return if we aren't generating any mothballed entities
        if (numberMothballedEntities <= 0) {
            return new ArrayList<>();
        }

        // Create the return list
        final List<Entity> mothballedEntities = new ArrayList<>();

        // Create the Faction Selector
        final AbstractFactionSelector factionSelector = createFactionSelector();

        // Create the Mothballed Entities
        for (int i = 0; i < numberMothballedEntities; i++) {
            final Faction faction = factionSelector.selectFaction(campaign);
            if (faction == null) {
                LogManager.getLogger().error("Failed to generate a valid faction, and thus cannot generate a mothballed 'Mech");
                continue;
            }

            // Create the parameters to generate the 'Mech from
            final AtBRandomMechParameters parameters = new AtBRandomMechParameters(
                    getBattleMechWeightClassGenerator().generate(Utilities.dice(2, 6)),
                    getBattleMechQualityGenerator().generate(Utilities.dice(2, 6))
            );

            // We want to ensure we get a 'Mech generated
            while (parameters.getWeight() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                parameters.setWeight(getBattleMechWeightClassGenerator().generate(Utilities.dice(2, 6)));
            }

            // Generate the 'Mech, and add it to the mothballed entities list
            final Entity entity = generateEntity(campaign, parameters, faction);
            if (entity != null) {
                mothballedEntities.add(entity);
            }
        }
        return mothballedEntities;
    }

    /**
     * @param campaign the campaign to add the units to
     * @param mothballedEntities the list of generated spare 'Mech entities to add and mothball
     * @return the list of created units
     */
    private List<Unit> createMothballedSpareUnits(final Campaign campaign,
                                                  final List<Entity> mothballedEntities) {
        final List<Unit> mothballedUnits = mothballedEntities.stream()
                .map(entity -> campaign.addNewUnit(entity, false, 0))
                .collect(Collectors.toList());
        mothballedUnits.forEach(Unit::completeMothball);
        return mothballedUnits;
    }

    /**
     * @param units the list of units to generate spare parts based on
     * @return the list of randomly generated parts
     */
    public List<Part> generateSpareParts(final List<Unit> units) {
        return getOptions().getPartGenerationMethod().isDisabled() ? new ArrayList<>()
                : getOptions().getPartGenerationMethod().getGenerator().generate(units, false, false);
    }

    /**
     * @param units the list of units to generate spare armour based on
     * @return the generated armour
     */
    public List<Armor> generateArmour(final List<Unit> units) {
        if (getOptions().getStartingArmourWeight() <= 0) {
            return new ArrayList<>();
        }

        final List<Armor> unitAssignedArmour = units.stream()
                .flatMap(unit -> unit.getParts().stream())
                .filter(part -> part instanceof Armor)
                .map(part -> (Armor) part)
                .collect(Collectors.toList());
        final List<Armor> armour = mergeIdenticalArmour(unitAssignedArmour);
        final double armourTonnageMultiplier = getOptions().getStartingArmourWeight()
                / armour.stream().mapToDouble(Armor::getTonnage).sum();
        armour.forEach(a -> a.setAmount(Math.toIntExact(Math.round(a.getAmount() * armourTonnageMultiplier))));
        return armour;
    }

    /**
     * This clones and merges armour determined by the custom check below together
     * @param unmergedArmour the unmerged list of armour, which may be assigned to a unit
     * @return the merged list of armour
     */
    private List<Armor> mergeIdenticalArmour(final List<Armor> unmergedArmour) {
        final List<Armor> mergedArmour = new ArrayList<>();
        unmergedArmour.forEach(armour -> {
            boolean unmerged = true;
            for (final Armor a : mergedArmour) {
                if (areSameArmour(a, armour)) {
                    a.addAmount(armour.getAmount());
                    unmerged = false;
                    break;
                }
            }

            if (unmerged) {
                final Armor a = armour.clone();
                a.setMode(WorkTime.NORMAL);
                a.setOmniPodded(false);
                mergedArmour.add(a);
            }
        });
        return mergedArmour;
    }

    /**
     * This is a custom equals comparison utilized by this class to determine if two Armour Parts
     * are the same
     * @param a1 the first Armour part
     * @param a2 the second Armour part
     * @return whether this class considers both types of Armour to be the same. This DIFFERS
     * from Armor::equals
     */
    private boolean areSameArmour(final Armor a1, final Armor a2) {
        return (a1.getClass() == a2.getClass())
                && a1.isSameType(a2)
                && (a1.isClan() == a2.isClan())
                && (a1.getQuality() == a2.getQuality())
                && (a1.getHits() == a2.getHits())
                && (a1.getSkillMin() == a2.getSkillMin());
    }

    /**
     * @param campaign the campaign to generate ammunition for
     * @param units the list of units to generate ammunition for
     * @return the generated ammunition
     */
    public List<AmmoStorage> generateAmmunition(final Campaign campaign, final List<Unit> units) {
        if (!getOptions().isGenerateSpareAmmunition() || ((getOptions().getNumberReloadsPerWeapon() <= 0)
                && !getOptions().isGenerateFractionalMachineGunAmmunition())) {
            return new ArrayList<>();
        }

        final List<AmmoBin> ammoBins = units.stream()
                .flatMap(unit -> unit.getParts().stream())
                .filter(part -> part instanceof AmmoBin)
                .map(part -> (AmmoBin) part)
                .collect(Collectors.toList());

        final List<AmmoStorage> ammunition = new ArrayList<>();
        final boolean generateReloads = getOptions().getNumberReloadsPerWeapon() > 0;
        ammoBins.forEach(ammoBin -> {
            if (getOptions().isGenerateFractionalMachineGunAmmunition() && ammoBinIsMachineGun(ammoBin)) {
                ammunition.add(new AmmoStorage(0, ammoBin.getType(), 50, campaign));
            } else if (generateReloads) {
                ammunition.add(new AmmoStorage(0, ammoBin.getType(),
                        ammoBin.getFullShots() * getOptions().getNumberReloadsPerWeapon(), campaign));
            }
        });

        return ammunition;
    }

    /**
     * @param ammoBin the ammo bin to check
     * @return whether the ammo bin's ammo type is a machine gun type
     */
    private boolean ammoBinIsMachineGun(final AmmoBin ammoBin) {
        switch (ammoBin.getType().getAmmoType()) {
            case AmmoType.T_MG:
            case AmmoType.T_MG_HEAVY:
            case AmmoType.T_MG_LIGHT:
                return true;
            default:
                return false;
        }
    }
    //endregion Spares

    //region Contract
    /**
     * This processes the selected contract
     * @param campaign the campaign to apply changes to
     * @param contract the selected contract, if any
     */
    private void processContract(final Campaign campaign, final @Nullable Contract contract) {
        if (contract == null) {
            return;
        }

        if (getOptions().isStartCourseToContractPlanet()) {
            campaign.getLocation().setJumpPath(contract.getJumpPath(campaign));
        }
    }
    //endregion Contract

    //region Finances
    /**
     * This processes the full financial setup for a campaign based on the one's options
     *
     * @param campaign the campaign to process finances for
     * @param trackers the trackers containing the personnel to get the hiring cost for
     * @param units the list of units to get the cost for
     * @param parts the list of parts to get the cost for
     * @param armour the list of different armours to get the cost for
     * @param ammunition the list of ammunition to get the cost for
     * @param contract the contract to potentially process the initial contract payment, which may
     *                 be null.
     */
    private void processFinances(final Campaign campaign,
                                 final List<CompanyGenerationPersonTracker> trackers,
                                 final List<Unit> units, final List<Part> parts,
                                 final List<Armor> armour, final List<AmmoStorage> ammunition,
                                 final @Nullable Contract contract) {
        // Don't bother processing if it's disabled
        if (!getOptions().isProcessFinances()) {
            return;
        }

        // Create Base Parsing Variables
        Money startingCash = generateStartingCash();
        Money minimumStartingFloat = Money.of(getOptions().getMinimumStartingFloat());
        Money loan = Money.zero();

        // Process Initial Contract Payment
        if (getOptions().isIncludeInitialContractPayment() && (contract != null)) {
            startingCash = startingCash.plus(contract.getTotalAdvanceAmount());
        }

        if (getOptions().isPayForSetup()) {
            // Calculate the total costs of setup
            final Money costs = calculateHiringCosts(campaign, trackers)
                    .plus(calculateUnitCosts(units))
                    .plus(calculatePartCosts(parts))
                    .plus(calculateArmourCosts(armour))
                    .plus(calculateAmmunitionCosts(ammunition));

            // Determine the maximum costs before a loan needs to be taken, and determine the
            // starting cash based on it.
            final Money maximumPreLoanCosts = startingCash.minus(minimumStartingFloat);
            if (maximumPreLoanCosts.isGreaterOrEqualThan(costs)) {
                startingCash = startingCash.minus(costs);
            } else {
                // Otherwise, the starting cash is the minimum float, with a loan created with the
                // remaining costs if that option is selected
                startingCash = minimumStartingFloat;
                if (getOptions().isStartingLoan()) {
                    loan = costs.minus(maximumPreLoanCosts).round();
                }
            }

            // Round the starting cash so we don't have any weird trailing numbers
            startingCash = startingCash.round();

            // Credit the campaign with the starting cash if it is positive
            if (startingCash.isPositive()) {
                campaign.getFinances().credit(TransactionType.STARTING_CAPITAL,
                        campaign.getLocalDate(), startingCash,
                        resources.getString("AbstractCompanyGenerator.CompanyStartupFunding.text"));
            }

            // Add the loan if there's one to add
            if (!loan.isZero()) {
                campaign.getFinances().addLoan(new Loan(loan, 15, 2, FinancialTerm.MONTHLY,
                        100, campaign.getLocalDate()));
            }
        } else {
            // Credit the campaign with the starting cash if it is positive
            startingCash = startingCash.isGreaterOrEqualThan(minimumStartingFloat) ? startingCash
                    : minimumStartingFloat;

            // Credit the campaign with the starting cash if it is positive
            if (startingCash.isPositive()) {
                campaign.getFinances().credit(TransactionType.STARTING_CAPITAL,
                        campaign.getLocalDate(), startingCash,
                        resources.getString("AbstractCompanyGenerator.CompanyStartupFunding.text"));
            }
        }

        // Report the financial state in the daily report
        if (loan.isZero()) {
            campaign.addReport(String.format(
                    resources.getString("AbstractCompanyGenerator.CompanyStartupFundedWithoutLoan.report"),
                    startingCash));
        } else {
            campaign.addReport(String.format(
                    resources.getString("AbstractCompanyGenerator.CompanyStartupFundedWithLoan.report"),
                    startingCash, loan));
        }
    }

    /**
     * @return the amount of starting cash generated for the Mercenary Company
     */
    private Money generateStartingCash() {
        return getOptions().isRandomizeStartingCash() ? rollRandomStartingCash()
                : Money.of(getOptions().getStartingCash());
    }

    /**
     * @return the option dice count d6 million c-bills, or zero if randomize starting cash is disabled
     */
    private Money rollRandomStartingCash() {
        return getOptions().isRandomizeStartingCash()
                ? Money.of(Math.pow(10, 6)).multipliedBy(Utilities.dice(getOptions().getRandomStartingCashDiceCount(), 6))
                : Money.zero();
    }

    /**
     * @param campaign the campaign to use in determining the hiring costs
     * @param trackers the trackers containing the personnel to get the hiring cost for
     * @return the cost of hiring the personnel, or zero if you aren't paying for hiring costs
     */
    private Money calculateHiringCosts(final Campaign campaign,
                                       final List<CompanyGenerationPersonTracker> trackers) {
        if (!getOptions().isPayForPersonnel()) {
            return Money.zero();
        }

        Money hiringCosts = Money.zero();
        for (final CompanyGenerationPersonTracker tracker : trackers) {
            hiringCosts = hiringCosts.plus(tracker.getPerson().getSalary(campaign).multipliedBy(2));
        }
        return hiringCosts;
    }

    /**
     * @param units the list of units to get the cost for
     * @return the cost of the units, or zero if you aren't paying for units
     */
    private Money calculateUnitCosts(final List<Unit> units) {
        if (!getOptions().isPayForUnits()) {
            return Money.zero();
        }

        Money unitCosts = Money.zero();

        for (final Unit unit : units) {
            if (unit.hasCommander() && getOptions().isGenerateUnitsAsAttached()) {
                unitCosts = unitCosts.plus(unit.getBuyCost().dividedBy(2));
            } else {
                unitCosts = unitCosts.plus(unit.getBuyCost());
            }
        }

        return unitCosts;
    }

    /**
     * @param parts the list of parts to get the cost for
     * @return the cost of the parts, or zero if you aren't paying for parts
     */
    private Money calculatePartCosts(final List<Part> parts) {
        if (!getOptions().isPayForParts()) {
            return Money.zero();
        }

        Money partCosts = Money.zero();
        for (final Part part : parts) {
            partCosts = partCosts.plus(part.getStickerPrice());
        }
        return partCosts;
    }

    /**
     * @param armours the list of different armours to get the cost for
     * @return the cost of the armour, or zero if you aren't paying for armour
     */
    private Money calculateArmourCosts(final List<Armor> armours) {
        if (!getOptions().isPayForArmour()) {
            return Money.zero();
        }

        Money armourCosts = Money.zero();
        for (final Armor armour : armours) {
            armourCosts = armourCosts.plus(armour.getStickerPrice());
        }
        return armourCosts;
    }

    /**
     * @param ammunition the list of ammunition to get the cost for
     * @return the cost of the ammunition, or zero if you aren't paying for ammunition
     */
    private Money calculateAmmunitionCosts(final List<AmmoStorage> ammunition) {
        if (!getOptions().isPayForAmmunition()) {
            return Money.zero();
        }

        Money ammunitionCosts = Money.zero();
        for (final AmmoStorage ammoStorage : ammunition) {
            ammunitionCosts = ammunitionCosts.plus(ammoStorage.getStickerPrice());
        }

        return ammunitionCosts;
    }
    //endregion Finances

    //region Surprises
/*
    private void generateSurprises(final Campaign campaign) {
        if (!getOptions().isGenerateSurprises()) {
            return;
        }

        generateMysteryBoxes(campaign);
    }

    private void generateMysteryBoxes(final Campaign campaign) {
        if (!getOptions().isGenerateMysteryBoxes()) {
            return;
        }

        final MysteryBoxType[] mysteryBoxTypes = MysteryBoxType.values();
        final List<AbstractMysteryBox> mysteryBoxes = new ArrayList<>();
        for (int i = 0; i < getOptions().getGenerateMysteryBoxTypes().length; i++) {
            if (getOptions().getGenerateMysteryBoxTypes()[i]) {
                mysteryBoxes.add(mysteryBoxTypes[i].getMysteryBox());
            }
        }

        // TODO : Processing of mystery boxes
    }
 */
    //endregion Surprises

    //region Apply to Campaign
    /**
     * Phase One: Starting Planet and Finalizing Personnel, Unit, and Units
     *
     * @param campaign the campaign to apply the generation to
     * @param trackers the trackers containing all the data required for Phase One
     * @return a list of the newly created units to add to the campaign
     */
    public List<Unit> applyPhaseOneToCampaign(final Campaign campaign,
                                              final List<CompanyGenerationPersonTracker> trackers) {
        // Process Personnel
        // If we aren't using the pool, generate all the Astechs and Medics required
        generateAssistants(campaign, trackers);

        // This does all the final personnel processing, including recruitment and running random
        // marriages
        finalizePersonnel(campaign, trackers);

        // We can only fill the pool after finalizing and recruiting our support personnel
        if (getOptions().isPoolAssistants()) {
            campaign.fillAstechPool();
            campaign.fillMedicPool();
        }

        // Process Units
        final List<Unit> units = createUnits(campaign, trackers);

        // Assign Techs to Units
        assignTechsToUnits(trackers, units);

        // Generate the Forces and Assign Units to them
        generateUnit(campaign, sortPersonnelIntoLances(trackers));

        return units;
    }

    /**
     * Phase Two: Finalizing Spares
     *
     * @param campaign the campaign to apply the generation to
     * @param mothballedEntities the generated mothballed spare entities
     * @param parts the generated spare parts
     * @param armour the generated spare armour
     * @param ammunition the generated spare armour
     * @return a list of the generated mothballed spare units
     */
    public List<Unit> applyPhaseTwoToCampaign(final Campaign campaign,
                                              final List<Entity> mothballedEntities,
                                              final List<Part> parts, final List<Armor> armour,
                                              final List<AmmoStorage> ammunition) {
        final List<Unit> mothballedUnits = createMothballedSpareUnits(campaign, mothballedEntities);
        parts.forEach(p -> campaign.getWarehouse().addPart(p, true));
        armour.forEach(a -> campaign.getWarehouse().addPart(a, true));
        ammunition.forEach(a -> campaign.getWarehouse().addPart(a, true));
        return mothballedUnits;
    }


    /**
     * Phase Three: Finalizing Contract and Finances
     *
     * @param campaign the campaign to apply the generation to
     * @param trackers the trackers containing all the data required for Phase One, which
     *                 includes all Personnel
     * @param units the units added to the campaign, including any mothballed units
     * @param parts the spare parts generated
     * @param armour the spare armour generated
     * @param ammunition the spare ammunition generated
     * @param contract the contract selected, if any
     */
    public void applyPhaseThreeToCampaign(final Campaign campaign,
                                          final List<CompanyGenerationPersonTracker> trackers,
                                          final List<Unit> units, final List<Part> parts,
                                          final List<Armor> armour,
                                          final List<AmmoStorage> ammunition,
                                          final @Nullable Contract contract) {
        // Process Contract
        processContract(campaign, contract);

        // Process Finances
        processFinances(campaign, trackers, units, parts, armour, ammunition, contract);
    }
    //endregion Apply to Campaign
}
