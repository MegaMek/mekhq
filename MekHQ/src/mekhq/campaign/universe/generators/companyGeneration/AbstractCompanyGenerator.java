/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.generators.companyGeneration;

import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.generator.AbstractPersonnelGenerator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.AbstractFactionSelector;
import mekhq.campaign.universe.AbstractPlanetSelector;
import mekhq.campaign.universe.DefaultFactionSelector;
import mekhq.campaign.universe.DefaultPlanetSelector;
import mekhq.campaign.universe.RangedFactionSelector;
import mekhq.campaign.universe.RangedPlanetSelector;
import mekhq.campaign.universe.enums.Alphabet;
import mekhq.campaign.universe.enums.CompanyGenerationType;
import mekhq.gui.enums.LayeredForceIcon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Ideas:
 * Finances: Randomize is a base 8d6 million C-Bill option, creates a nice curve averaging around 28m c-bills,
 * which is a company plus likely a slight float. Recommended 10d6 for 3067 in the notes
 *
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
 * TODO :
 *      Mercenaries may customize their mechs, with clantech if enabled only post-3055
 *      Unit weight sorting needs more options and variants... like an isolate SL units, keep SL units to highest ranked officers, that kind of thing
 *      Finish the personnel randomization overrides
 *      Cleanup the dialog, have it disable and enable based on variable values
 *      Implement:
 *          assignBestRollToUnitCommander
 *          startingPlanet
 *          centerPlanet
 *          assignTechsToUnits
 *          generateMothballedSpareUnits
 *          sparesPercentOfActiveUnits
 *          generateSpareParts // 1 for every 3 (round normally) parts in inventory?
 *          startingArmourWeight
 *          generateSpareAmmunition
 *          numberReloadsPerWeapon
 *          generateFractionalMachineGunAmmunition // 50 rounds per machine gun in force?
 *          selectStartingContract
 *          startCourseToContractPlanet
 *
 * FIXME :
 *      Backgrounds don't work
 *      Weighted Type for force icons don't work
 *      Dialog x doesn't work
 *      Dialog Buttons look odd and need fixing
 *      Dialog Modify the buttons, and have them appear or disappear based on the current panel
 *      Panel has odd whitespace usage
 *
 * Class Notes:
 * {{@link AbstractCompanyGenerator#applyToCampaign}} takes the campaign and applies all changes to
 * it. No method not directly called from there may alter the campaign.
 */
public abstract class AbstractCompanyGenerator {
    //region Variable Declarations
    private CompanyGenerationType type;
    private CompanyGenerationOptions options;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractCompanyGenerator(final CompanyGenerationType type, final Campaign campaign) {
        this(type, new CompanyGenerationOptions(type, campaign));
    }

    protected AbstractCompanyGenerator(final CompanyGenerationType type, final CompanyGenerationOptions options) {
        setType(type);
        setOptions(options);
    }
    //endregion Constructors

    //region Getters/Setters
    public CompanyGenerationType getType() {
        return type;
    }

    public void setType(final CompanyGenerationType type) {
        this.type = type;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }

    public void setOptions(final CompanyGenerationOptions options) {
        this.options = options;
    }
    //endregion Getters/Setters

    //region Determination Methods
    /**
     * @return the number of lances to generate
     */
    private int determineNumberLances() {
        return (getOptions().getCompanyCount() * getOptions().getLancesPerCompany())
                + getOptions().getIndividualLanceCount()
                + (getOptions().isGenerateMercenaryCompanyCommandLance() ? 1 : 0);
    }

    /**
     * @return the number of Captains
     */
    private int determineNumberCaptains() {
        return getOptions().isGenerateCaptains()
                ? Math.max((getOptions().getCompanyCount()
                        - (getOptions().isGenerateMercenaryCompanyCommandLance() ? 0 : 1)), 0)
                : 0;
    }

    /**
     * @return the index of the first non-officer
     */
    private int determineFirstNonOfficer() {
        return determineNumberLances() + (getOptions().isCompanyCommanderLanceOfficer() ? 0 : 1);
    }
    //endregion Determination Methods

    //region Personnel
    //region Combat Personnel
    /**
     * @param campaign the campaign to use to generate the combat personnel
     * @return the list of generated combat personnel
     */
    public List<Person> generateCombatPersonnel(final Campaign campaign) {
        List<Person> combatPersonnel = new ArrayList<>();
        final int numMechWarriors = determineNumberLances() * getOptions().getLanceSize();
        final AbstractPersonnelGenerator personnelGenerator = createPersonnelGenerator(campaign);

        for (int i = 0; i < numMechWarriors; i++) {
            combatPersonnel.add(campaign.newPerson(Person.T_MECHWARRIOR, personnelGenerator));
        }

        if (getOptions().isAssignBestOfficers()) {
            combatPersonnel = combatPersonnel.stream()
                    .sorted(Comparator.comparingInt((Person p) -> p.getExperienceLevel(false))
                            .reversed()
                            .thenComparingInt(p -> p.getSkillLevel(SkillType.S_LEADER)
                                    + p.getSkillLevel(SkillType.S_STRATEGY) + p.getSkillLevel(SkillType.S_TACTICS)))
                    .collect(Collectors.toList());
        }

        generateCommandingOfficer(combatPersonnel.get(0), numMechWarriors);

        generateOfficers(combatPersonnel);

        generateStandardMechWarriors(combatPersonnel);

        return combatPersonnel;
    }

    /**
     * Turns a person into the commanding officer of the force being generated
     * 1) Assigns the Commander flag (if that option is true)
     * 2) Improves Gunnery and Piloting by one level
     * 3) Gets two random officer skill increases
     * 4) Gets the highest rank possible assigned to them
     *
     * @param commandingOfficer the commanding officer
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    private void generateCommandingOfficer(final Person commandingOfficer, final int numMechWarriors) {
        commandingOfficer.setCommander(getOptions().isAssignCompanyCommanderFlag());
        commandingOfficer.improveSkill(SkillType.S_GUN_MECH);
        commandingOfficer.improveSkill(SkillType.S_PILOT_MECH);
        assignRandomOfficerSkillIncrease(commandingOfficer, 2);

        if (getOptions().isAutomaticallyAssignRanks()) {
            generateCommandingOfficerRank(commandingOfficer, numMechWarriors);
        }
    }

    /**
     * @param commandingOfficer the commanding officer
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    protected abstract void generateCommandingOfficerRank(final Person commandingOfficer, final int numMechWarriors);

    /**
     * This generates officers based on the provided options.
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
     * @param personnel the list of all generated personnel
     */
    private void generateOfficers(final List<Person> personnel) {
        int captains = determineNumberCaptains();
        // Starting at 1, as 0 is the mercenary company commander
        for (int i = 1; i < determineFirstNonOfficer(); i++) {
            final Person officer = personnel.get(i);

            // Improve Skills
            final Skill gunnery = officer.getSkill(SkillType.S_GUN_MECH);
            final Skill piloting = officer.getSkill(SkillType.S_PILOT_MECH);
            if ((gunnery == null) && (piloting != null)) {
                officer.improveSkill(SkillType.S_GUN_MECH);
            } else if ((gunnery != null) && (piloting == null)) {
                officer.improveSkill(SkillType.S_PILOT_MECH);
            } else if (gunnery == null) {
                // Both are null... this shouldn't occur. In this case, boost both
                officer.improveSkill(SkillType.S_GUN_MECH);
                officer.improveSkill(SkillType.S_PILOT_MECH);
            } else {
                officer.improveSkill((gunnery.getLevel() > piloting.getLevel()
                        && getOptions().isApplyOfficerStatBonusToWorstSkill() ? piloting : gunnery)
                        .getType().getName());
            }

            if (captains > 0) {
                // Assign Random Officer Skill Increase
                assignRandomOfficerSkillIncrease(officer, 2);

                if (getOptions().isAutomaticallyAssignRanks()) {
                    // Assign Rank of O4 - Captain
                    officer.setRankNumeric(Ranks.RWO_MAX + 4);
                }

                // Decrement the number of captains left to generate
                captains--;
            } else {
                // Assign Random Officer Skill Increase
                assignRandomOfficerSkillIncrease(officer, 1);

                if (getOptions().isAutomaticallyAssignRanks()) {
                    // Assign Rank of O3 - Lieutenant
                    officer.setRankNumeric(Ranks.RWO_MAX + 3);
                }
            }
        }
    }

    /**
     * This randomly assigns officer skill increases during officer creation.
     * The skill level is improved by one level per roll, but if the skill is newly acquired
     * it applies a second boost so that the value is set to 1.
     *
     * @param person the person to assign the skill increases to
     * @param boosts the number of boosts to apply
     */
    private void assignRandomOfficerSkillIncrease(final Person person, final int boosts) {
        for (int i = 0; i < boosts; i++) {
            switch (Utilities.dice(1, 3)) {
                case 0:
                    person.improveSkill(SkillType.S_LEADER);
                    if (person.getSkillLevel(SkillType.S_LEADER) == 0) {
                        person.improveSkill(SkillType.S_LEADER);
                    }
                    break;
                case 1:
                    person.improveSkill(SkillType.S_STRATEGY);
                    if (person.getSkillLevel(SkillType.S_STRATEGY) == 0) {
                        person.improveSkill(SkillType.S_STRATEGY);
                    }
                    break;
                case 2:
                    person.improveSkill(SkillType.S_TACTICS);
                    if (person.getSkillLevel(SkillType.S_TACTICS) == 0) {
                        person.improveSkill(SkillType.S_TACTICS);
                    }
                    break;
            }
        }
    }
    /**
     * Sets up a standard MechWarrior
     * 1) Assigns rank of E12 - Sergeant, or E4 for Clan, WoB, and ComStar
     *
     * @param personnel the list of all generated personnel
     */
    private void generateStandardMechWarriors(final List<Person> personnel) {
        final boolean isClanComStarOrWoB = getOptions().getFaction().isComStarOrWoB()
                || getOptions().getFaction().isClan();
        for (int i = determineFirstNonOfficer(); i < personnel.size(); i++) {
            if (getOptions().isAutomaticallyAssignRanks()) {
                personnel.get(i).setRankNumeric(isClanComStarOrWoB ? 4 : 12);
            }
        }
    }
    //endregion Combat Personnel

    //region Support Personnel
    /**
     * @param campaign the campaign to generate from
     * @return a list of all support personnel
     */
    public List<Person> generateSupportPersonnel(final Campaign campaign) {
        final List<Person> supportPersonnel = new ArrayList<>();
        final AbstractPersonnelGenerator personnelGenerator = createPersonnelGenerator(campaign);

        for (final Map.Entry<Integer, Integer> entry : getOptions().getSupportPersonnel().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                final Person person = campaign.newPerson(entry.getKey(), personnelGenerator);
                // All support personnel get assigned is their rank
                if (getOptions().isAutomaticallyAssignRanks()) {
                    switch (campaign.getRanks().getRankSystem()) {
                        case Ranks.RS_CCWH:
                        case Ranks.RS_CL:
                            break;
                        case Ranks.RS_COM:
                        case Ranks.RS_WOB:
                        case Ranks.RS_MOC:
                            person.setRankNumeric(4);
                            break;
                        default:
                            person.setRankNumeric(8);
                            break;
                    }
                }
                supportPersonnel.add(person);
            }
        }
        return supportPersonnel;
    }

    /**
     * @param campaign the campaign to use in creating the assistants
     * @param personnel the list of personnel to add the newly created assistants to
     */
    private void generateAssistants(final Campaign campaign, final List<Person> personnel) {
        // If you don't want to use pooled assistants, then this generates them as personnel instead
        if (!getOptions().isPoolAssistants()) {
            final AbstractPersonnelGenerator personnelGenerator = createPersonnelGenerator(campaign);
            for (int i = 0; i < campaign.getAstechNeed(); i++) {
                personnel.add(campaign.newPerson(Person.T_ASTECH, personnelGenerator));
            }
            for (int i = 0; i < campaign.getMedicsNeed(); i++) {
                personnel.add(campaign.newPerson(Person.T_MEDIC, personnelGenerator));
            }
        }
    }
    //endregion Support Personnel

    /**
     * @param campaign the Campaign to generate using
     * @return the Personnel Generator to use in generation
     */
    private AbstractPersonnelGenerator createPersonnelGenerator(final Campaign campaign) {
        final AbstractFactionSelector factionSelector;
        final AbstractPlanetSelector planetSelector;

        if (getOptions().isRandomizeOrigin()) {
            factionSelector = new RangedFactionSelector(getOptions().getOriginSearchRadius());
            ((RangedFactionSelector) factionSelector).setDistanceScale(getOptions().getOriginDistanceScale());

            planetSelector = new RangedPlanetSelector(getOptions().getOriginSearchRadius(),
                    getOptions().isExtraRandomOrigin());
            ((RangedPlanetSelector) planetSelector).setDistanceScale(getOptions().getOriginDistanceScale());
        } else {
            factionSelector = new DefaultFactionSelector(getOptions().getFaction());
            planetSelector = new DefaultPlanetSelector();
        }
        return campaign.getPersonnelGenerator(factionSelector, planetSelector);
    }
    //endregion Personnel

    //region Units
    /**
     * @param campaign the campaign to generate for
     * @param combatPersonnel the list of all combat personnel
     * @return the list of all generated entities, with null holding spaces without 'Mechs
     */
    public List<Entity> generateUnits(final Campaign campaign, List<Person> combatPersonnel) {
        final int firstNonOfficer = determineFirstNonOfficer();
        final List<RandomMechParameters> parameters = createUnitGenerationParameters(combatPersonnel, firstNonOfficer);

        // This parses through all combat personnel and checks if the roll is SL.
        // If it is, reroll the weight with a max value of 12
        for (int i = 0; i < combatPersonnel.size(); i++) {
            if (parameters.get(i).isStarLeague()) {
                parameters.get(i).setWeight(determineBattleMechWeight(Math.max(Utilities.dice(2, 6)
                        + ((i == 0) ? 2 : ((i < firstNonOfficer) ? 1 : 0)), 12)));
            }
        }

        // TODO : Add a sort SL units first option
        if (getOptions().isGroupByWeight()) {
            if (getOptions().isKeepOfficerRollsSeparate()) {
                parameters.subList(0, firstNonOfficer)
                        .sort(Comparator.comparingInt(RandomMechParameters::getWeight).reversed());
                parameters.subList(firstNonOfficer, parameters.size())
                        .sort(Comparator.comparingInt(RandomMechParameters::getWeight).reversed());
            } else {
                parameters.sort(Comparator.comparingInt(RandomMechParameters::getWeight).reversed());
                combatPersonnel = sortPersonnelIntoLances(combatPersonnel);
            }
        }

        if (getOptions().isAssignBestRollToUnitCommander()) {
            // TODO : Implement
        }

        return generateEntities(campaign, parameters, combatPersonnel);
    }

    /**
     * @param combatPersonnel the list of all combat personnel
     * @param firstNonOfficer the index of the first non-officer
     * @return a list of RandomMechParameters
     */
    protected List<RandomMechParameters> createUnitGenerationParameters(
            final List<Person> combatPersonnel, final int firstNonOfficer) {
        List<RandomMechParameters> parameters = new ArrayList<>();
        for (int i = 0; i < combatPersonnel.size(); i++) {
            final int modifier = (i == 0) ? 2 : ((i < firstNonOfficer) ? 1 : 0);
            parameters.add(new RandomMechParameters(
                    determineBattleMechWeight(Utilities.dice(2, 6) + modifier),
                    determineBattleMechQuality(Utilities.dice(2, 6) + modifier)
            ));
        }
        return parameters;
    }

    /**
     * @param roll the modified roll to use
     * @return the generated EntityWeightClass magic int
     * EntityWeightClass.WEIGHT_ULTRA_LIGHT for none,
     * EntityWeightClass.WEIGHT_SUPER_HEAVY for SL tables
     */
    protected abstract int determineBattleMechWeight(int roll);

    /**
     * @param roll the modified roll to use
     * @return the generated IUnitRating magic int for Dragoon Quality
     */
    protected abstract int determineBattleMechQuality(int roll);

    /**
     * @param campaign the campaign to generate for
     * @param parameters the list of all parameters to use in generation
     * @param combatPersonnel the list of all combat personnel
     * @return the list of all generated entities, with null holding spaces without 'Mechs
     */
    private List<Entity> generateEntities(final Campaign campaign,
                                          final List<RandomMechParameters> parameters,
                                          final List<Person> combatPersonnel) {
        List<Entity> entities = new ArrayList<>();
        for (int i = 0; i < parameters.size(); i++) {
            entities.add(generateEntity(campaign, parameters.get(i), combatPersonnel.get(i)));
        }
        return entities;
    }

    /**
     * This generates a single entity, thus allowing for individual rerolls
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param pilot the pilot to generate the Entity for
     * @return the entity generated, or null otherwise
     */
    public @Nullable Entity generateEntity(final Campaign campaign,
                                            final RandomMechParameters parameters,
                                            final Person pilot) {
        // Ultra-Light means no mech generated
        if (parameters.getWeight() == EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
            return null;
        }

        final MechSummary mechSummary = generateMechSummary(campaign, parameters, pilot);

        try {
            return new MechFileParser(mechSummary.getSourceFile(), mechSummary.getEntryName()).getEntity();
        } catch (Exception e) {
            MekHQ.getLogger().error("Failed to generate entity", e);
        }

        return null;
    }

    /**
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param person the person to generate the mech for
     * @return the MechSummary generated from the provided parameters
     */
    protected abstract MechSummary generateMechSummary(final Campaign campaign,
                                                       final RandomMechParameters parameters,
                                                       final Person person);

    /**
     * @param campaign the campaign to generate for
     * @param parameters the parameters to use in generation
     * @param faction the faction code to use in generation
     * @param year the year to use in generation
     * @return the MechSummary generated from the provided parameters
     */
    protected MechSummary generateMechSummary(final Campaign campaign,
                                              final RandomMechParameters parameters,
                                              final String faction, int year) {
        Predicate<MechSummary> filter = ms ->
                (!campaign.getCampaignOptions().limitByYear() || (year > ms.getYear()))
                        && (!ms.isClan() || campaign.getCampaignOptions().allowClanPurchases())
                        && (ms.isClan() || campaign.getCampaignOptions().allowISPurchases());
        return campaign.getUnitGenerator().generate(faction, UnitType.MEK,
                parameters.getWeight(), year, parameters.getQuality(), filter);
    }

    /**
     * @param campaign the campaign to add the units to
     * @param combatPersonnel the list of combat personnel to assign to units
     * @param entities the list of generated entities, with null holding spaces without 'Mechs
     * @return the list of created units
     */
    private List<Unit> createUnits(final Campaign campaign, final List<Person> combatPersonnel,
                                   final List<Entity> entities) {
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            if (entities.get(i) != null) {
                final Unit unit = campaign.addNewUnit(entities.get(i), false, 0);
                if (i < combatPersonnel.size()) {
                    unit.addPilotOrSoldier(combatPersonnel.get(i));
                    if (getOptions().isGenerateUnitsAsAttached()) {
                        combatPersonnel.get(i).setOriginalUnit(unit);
                    }
                }
                units.add(unit);
            }
        }
        return units;
    }

    /**
     * @param supportPersonnel the list of support personnel including the techs to assign to units
     * @param units the list of units to have techs assigned to
     */
    private void assignTechsToUnits(final List<Person> supportPersonnel, final List<Unit> units) {
        if (getOptions().isAssignTechsToUnits()) {
            // TODO : Implement me
        }
    }
    //endregion Units

    //region Unit
    /**
     * @param personnel the combat personnel to sort into their lances
     * @return a new List containing the sorted personnel
     */
    private List<Person> sortPersonnelIntoLances(final List<Person> personnel) {
        final Person commander = personnel.get(0);
        List<Person> officers = new ArrayList<>(personnel.subList(1, determineFirstNonOfficer()));
        final List<Person> standardMechWarriors = new ArrayList<>(personnel.subList(determineFirstNonOfficer(), personnel.size()));
        final List<Person> sortedPersonnel = new ArrayList<>();

        // Sort Command Lance
        sortedPersonnel.add(commander);
        if (!getOptions().isCompanyCommanderLanceOfficer()) {
            // This removes the first non-Captain officer, as Captains each get their own companies
            sortedPersonnel.add(officers.remove(determineNumberCaptains()));
        }
        for (int i = sortedPersonnel.size() - 1; i < getOptions().getLanceSize(); i++) {
            sortedPersonnel.add(standardMechWarriors.remove(0));
        }

        // If the command lance is part of a company, we sort the rest of that company immediately
        if (!getOptions().isGenerateMercenaryCompanyCommandLance() && (getOptions().getCompanyCount() > 0)) {
            for (int i = 1; i < getOptions().getLancesPerCompany(); i++) {
                // This removes the first non-Captain officer, as Captains each get their own companies
                sortedPersonnel.add(officers.remove(determineNumberCaptains()));
                for (int y = 1; (y < getOptions().getLanceSize()) && !standardMechWarriors.isEmpty(); y++) {
                    sortedPersonnel.add(standardMechWarriors.remove(0));
                }
            }
        }

        // Sort into Companies
        int numberCaptains = determineNumberCaptains();
        for (int i = 0; i < determineNumberCaptains(); i++) {
            // Assign the Captain's Lance
            sortedPersonnel.add(officers.remove(0));
            numberCaptains--;
            for (int y = 1; (y < getOptions().getLanceSize()) && !standardMechWarriors.isEmpty(); y++) {
                sortedPersonnel.add(standardMechWarriors.remove(0));
            }
            // Then assign the other lances
            for (int y = 1; y < getOptions().getLancesPerCompany(); y++) {
                // This removes the first non-Captain officer, as Captains each get their own companies
                sortedPersonnel.add(officers.remove(numberCaptains));
                for (int z = 1; (z < getOptions().getLanceSize()) && !standardMechWarriors.isEmpty(); z++) {
                    sortedPersonnel.add(standardMechWarriors.remove(0));
                }
            }
        }

        // Sort any individual lances
        final int originalOfficersSize = officers.size();
        for (int i = 0; i < originalOfficersSize; i++) {
            sortedPersonnel.add(officers.remove(0));
            for (int y = 1; (y < getOptions().getLanceSize()) && !standardMechWarriors.isEmpty(); y++) {
                sortedPersonnel.add(standardMechWarriors.remove(0));
            }
        }

        return sortedPersonnel;
    }

    /**
     * This generates the TO&E structure, and assigns personnel to their individual lances.
     * This is called after all dialog modifications to personnel.
     * @param campaign the campaign to generate the unit within
     * @param personnel a CLONED list of personnel properly organized into lances
     */
    private void generateUnit(final Campaign campaign, final List<Person> personnel) {
        final Force originForce = campaign.getForce(0);
        final Alphabet[] alphabet = Alphabet.values();
        String background = "";

        if (getOptions().isGenerateForceIcons() && (MHQStaticDirectoryManager.getForceIcons() != null)) {
            // FIXME: We need a new way to handle this form of search... just default to CSR for now
            background = "CSR.png";
            /*
            if (MHQStaticDirectoryManager.getForceIcons().getItems().keySet().stream()
                    .anyMatch(s -> s.equalsIgnoreCase(getOptions().getFaction().getFullName(campaign.getGameYear())))) {
                background = getOptions().getFaction().getFullName(campaign.getGameYear());
            }

            if (background.isBlank() && (MHQStaticDirectoryManager.getForceIcons().getItems().keySet()
                    .stream().anyMatch(s -> s.equalsIgnoreCase(getOptions().getFaction().getShortName())))) {
                background = getOptions().getFaction().getShortName();
            }
            */
        }

        // Create the Origin Force Icon, if we are generating force icons and the origin icon has
        // not been set
        if (getOptions().isGenerateForceIcons()
                && Force.ROOT_LAYERED.equals(originForce.getIconCategory())
                && (originForce.getIconMap().entrySet().size() == 1)
                && (originForce.getIconMap().containsKey(LayeredForceIcon.FRAME.getLayerPath()))) {
            final LinkedHashMap<String, Vector<String>> iconMap = new LinkedHashMap<>();

            // Type
            iconMap.put(LayeredForceIcon.TYPE.getLayerPath(), new Vector<>());
            iconMap.get(LayeredForceIcon.TYPE.getLayerPath()).add("BattleMech.png");

            // Background
            if (!background.isBlank()) {
                iconMap.put(LayeredForceIcon.BACKGROUND.getLayerPath(), new Vector<>());
                iconMap.get(LayeredForceIcon.BACKGROUND.getLayerPath()).add(background);
            }

            // Frame
            iconMap.put(LayeredForceIcon.FRAME.getLayerPath(), new Vector<>());
            iconMap.get(LayeredForceIcon.FRAME.getLayerPath()).add("Frame.png");

            originForce.setIconMap(iconMap);
        }

        // Generate the Mercenary Company Command Lance
        if (getOptions().isGenerateMercenaryCompanyCommandLance()) {
            Force commandLance = createLance(campaign, originForce, personnel, campaign.getName()
                    + resources.getString("AbstractCompanyGenerator.commandLance.text"), background);
            commandLance.getIconMap().put(LayeredForceIcon.SPECIAL_MODIFIER.getLayerPath(), new Vector<>());
            commandLance.getIconMap().get(LayeredForceIcon.SPECIAL_MODIFIER.getLayerPath()).add("HQ indicator.png");
        }

        // Create Companies
        for (int i = 0; i < getOptions().getCompanyCount(); i++) {
            final Force company = new Force(getOptions().getForceNamingType().getValue(alphabet[i])
                    + resources.getString("AbstractCompanyGenerator.company.text"));
            campaign.addForce(company, originForce);
            for (int y = 0; y < getOptions().getLancesPerCompany(); y++) {
                createLance(campaign, company, personnel, alphabet[y], background);
            }

            if (getOptions().isGenerateForceIcons()) {
                createLayeredForceIcon(campaign, company, false, background);
            }
        }

        // Create Individual Lances
        for (int i = 0 ; i < getOptions().getIndividualLanceCount(); i++) {
            createLance(campaign, originForce, personnel, alphabet[i + getOptions().getCompanyCount()], background);
        }
    }

    /**
     * This creates a lance with a standard name
     * @param campaign the campaign to generate the unit within
     * @param head the force to append the new lance to
     * @param personnel the list of personnel, properly ordered to be assigned to the lance
     * @param alphabet the alphabet value to determine the lance name from
     * @param background the background filename
     */
    private void createLance(final Campaign campaign, final Force head, final List<Person> personnel,
                             final Alphabet alphabet, final String background) {
        createLance(campaign, head, personnel,
                getOptions().getForceNamingType().getValue(alphabet)
                        + resources.getString("AbstractCompanyGenerator.lance.text"),
                background);
    }

    /**
     * @param campaign the campaign to generate the unit within
     * @param head the force to append the new lance to
     * @param personnel the list of personnel, properly ordered to be assigned to the lance
     * @param name the lance's name
     * @param background the background filename
     * @return the newly created lance
     */
    private Force createLance(final Campaign campaign, final Force head, final List<Person> personnel,
                              final String name, final String background) {
        Force lance = new Force(name);
        campaign.addForce(lance, head);
        for (int i = 0; (i < getOptions().getLanceSize()) && !personnel.isEmpty(); i++) {
            campaign.addUnitToForce(personnel.remove(0).getUnit(), lance);
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
     * @param background the background filename
     */
    private void createLayeredForceIcon(final Campaign campaign, final Force force,
                                        final boolean isLance, final String background) {
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return;
        }

        final LinkedHashMap<String, Vector<String>> iconMap = new LinkedHashMap<>();

        // Type
        // FIXME : I'm not working properly to determine the filename
        final int weightClass = determineLanceWeightClass(campaign, force, isLance);
        final String weightClassName = EntityWeightClass.getClassName(weightClass);
        String filename = String.format("BattleMech %s.png", weightClassName);
        MekHQ.getLogger().warning(filename);
        if (!MHQStaticDirectoryManager.getForceIcons().getItems().containsKey(filename)) {
            filename = "BattleMech.png";
        }
        iconMap.put(LayeredForceIcon.TYPE.getLayerPath(), new Vector<>());
        iconMap.get(LayeredForceIcon.TYPE.getLayerPath()).add(filename);

        // Formation
        iconMap.put(LayeredForceIcon.FORMATION.getLayerPath(), new Vector<>());
        iconMap.get(LayeredForceIcon.FORMATION.getLayerPath()).add(isLance ? "04 Lance.png" : "06 Company.png");

        // Background
        if (!background.isBlank()) {
            iconMap.put(LayeredForceIcon.BACKGROUND.getLayerPath(), new Vector<>());
            iconMap.get(LayeredForceIcon.BACKGROUND.getLayerPath()).add(background);
        }

        // Frame
        iconMap.put(LayeredForceIcon.FRAME.getLayerPath(), new Vector<>());
        iconMap.get(LayeredForceIcon.FRAME.getLayerPath()).add("Frame.png");

        force.setIconMap(iconMap);
    }

    /**
     * This determines the weight class of a force (lance or company) based on the
     * @param campaign the campaign to determine based on
     * @param force the force to determine the weight class for
     * @param isLance whether the force is a lance or a company
     * @return the weight class of the force
     */
    private int determineLanceWeightClass(final Campaign campaign, final Force force,
                                          final boolean isLance) {
        double weight = 0.0;
        for (final UUID unitId : force.getAllUnits(true)) {
            final Unit unit = campaign.getUnit(unitId);
            if ((unit != null) && (unit.getEntity() != null)) {
                weight += unit.getEntity().getWeight();
            }
        }

        weight = weight * 4.0 / (getOptions().getLanceSize() * (isLance ? 1 : getOptions().getLancesPerCompany()));
        if (weight < 40) {
            return EntityWeightClass.WEIGHT_ULTRA_LIGHT;
        } else if (weight > 130) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        } else if (weight > 200) {
            return EntityWeightClass.WEIGHT_HEAVY;
        } else if (weight > 280) {
            return EntityWeightClass.WEIGHT_ASSAULT;
        } else if (weight > 390) {
            return EntityWeightClass.WEIGHT_SUPER_HEAVY;
        } else { // 40 <= weight <= 130
            return EntityWeightClass.WEIGHT_LIGHT;
        }
    }
    //endregion Unit

    //region Spares
    public List<Part> generateSpareParts() {
        // TODO : Implement me
        return new ArrayList<>();
    }

    public List<AmmoStorage> generateAmmunition() {
        // TODO : Implement me
        return new ArrayList<>();
    }
    //endregion Spares

    //region Contract
    private void processContract(final Campaign campaign, final @Nullable Contract contract) {
        if (getOptions().isStartCourseToContractPlanet()) {
            // TODO : implement me
        }
    }
    //endregion Contract

    //region Finances
    private void processFinances(final Campaign campaign, final List<Person> personnel,
                                 final List<Unit> units, final List<Part> parts,
                                 final List<AmmoStorage> ammunition,
                                 final @Nullable Contract contract) {
        Money startingCash = getOptions().isRandomizeStartingCash() ? rollRandomStartingCash()
                : Money.of(getOptions().getStartingCash());
        final Money minimumStartingFloat = Money.of(getOptions().getMinimumStartingFloat());
        if (getOptions().isPayForSetup()) {
            final Money hiringCosts = calculateHiringCosts(personnel);
            final Money unitCosts = calculateUnitCosts(units);
            final Money partCosts = calculatePartCosts(parts);
            final Money ammunitionCosts = calculateAmmunitionCosts(ammunition);
            // TODO : Finish implementation
        } else {
            campaign.addReport("");
            startingCash = startingCash.isGreaterOrEqualThan(minimumStartingFloat) ? startingCash
                    : minimumStartingFloat;
            if (!startingCash.isZero()) {
                campaign.getFinances().credit(startingCash, Transaction.C_START,
                        resources.getString(""), campaign.getLocalDate());
            }
        }
    }

    /**
     * @return the option dice count d6 million c-bills, or zero if randomize starting cash is disabled
     */
    private Money rollRandomStartingCash() {
        return getOptions().isRandomizeStartingCash()
                ? Money.of(10^6).multipliedBy(Utilities.dice(getOptions().getRandomStartingCashDiceCount(), 6))
                : Money.zero();
    }

    /**
     * @param personnel the list of personnel to get the hiring cost for
     * @return the cost of hiring the personnel, or zero if you aren't paying for hiring costs
     */
    private Money calculateHiringCosts(final List<Person> personnel) {
        if (!getOptions().isPayForPersonnel()) {
            return Money.zero();
        }

        Money hiringCosts = Money.zero();
        for (final Person person : personnel) {
            hiringCosts = hiringCosts.plus(person.getSalary().multipliedBy(2));
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

    /**
     * This method takes the campaign and applies all changes to it. No method not directly
     * called from here may alter the campaign.
     *
     * @param campaign the campaign to apply the generation to
     * @param combatPersonnel the list of generated combat personnel
     * @param supportPersonnel the list of generated support personnel
     * @param entities the list of generated entities, with null holding spaces without 'Mechs
     * @param parts the list of generated parts
     * @param ammunition the list of generated ammunition
     * @param contract the selected contract, or null if one has not been selected
     */
    public void applyToCampaign(final Campaign campaign, final List<Person> combatPersonnel,
                                final List<Person> supportPersonnel, final List<Entity> entities,
                                final List<Part> parts, final List<AmmoStorage> ammunition,
                                final @Nullable Contract contract) {
        // Process Personnel
        List<Person> personnel = new ArrayList<>(supportPersonnel);
        personnel.addAll(combatPersonnel);

        // If we aren't using the pool, generate all of the Astechs and Medics required
        generateAssistants(campaign, personnel);

        // Recruit all of the personnel, GM-style so that the initial hiring cost is calculated as
        // part of the financial model
        personnel.forEach(p -> campaign.recruitPerson(p, true));

        // We can only fill the pool after recruiting our support personnel
        if (getOptions().isPoolAssistants()) {
            campaign.fillAstechPool();
            campaign.fillMedicPool();
        }

        // Process Units
        final List<Unit> units = createUnits(campaign, combatPersonnel, entities);

        // Assign Techs to Units
        assignTechsToUnits(supportPersonnel, units);

        // Generate the Forces and Assign Units to them
        generateUnit(campaign, sortPersonnelIntoLances(combatPersonnel));

        // Process Contract
        processContract(campaign, contract);

        // Process Finances
        processFinances(campaign, personnel, units, parts, ammunition, contract);

        // Process Parts - After finances because we want to merge parts together
        parts.forEach(p -> campaign.getWarehouse().addPart(p, true));

        // Process Ammunition - After finances because we want to merge the ammunition together when
        // possible
        ammunition.forEach(a -> campaign.getWarehouse().addPart(a, true));
    }

    //region Local Classes
    /**
     * This class contains the parameters used to generate a random mech, and allows sorting and
     * swapping the order of rolled parameters while keeping them connected.
     */
    protected static class RandomMechParameters {
        //region Variable Declarations
        private int weight;
        private int quality;
        private boolean starLeague;
        //endregion Variable Declarations

        //region Constructors
        public RandomMechParameters(final int weight, final int quality) {
            setWeight(weight);
            setQuality(quality);
            setStarLeague(weight == EntityWeightClass.WEIGHT_SUPER_HEAVY);
        }
        //endregion Constructors

        //region Getters/Setters
        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }

        public int getQuality() {
            return quality;
        }

        public void setQuality(int quality) {
            this.quality = quality;
        }

        public boolean isStarLeague() {
            return starLeague;
        }

        public void setStarLeague(boolean starLeague) {
            this.starLeague = starLeague;
        }
        //endregion Getters/Setters
    }
    //endregion Local Classes
}
