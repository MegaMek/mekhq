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

import megamek.common.EntityWeightClass;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
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
import java.util.stream.Collectors;

public class AbstractCompanyGenerator {
    //region Variable Declarations
    private CompanyGenerationType type;
    private CompanyGenerationOptions options;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractCompanyGenerator(final CompanyGenerationType type) {
        this(type, new CompanyGenerationOptions(type));
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
    private int determineNumberLances() {
        return (getOptions().getCompanyCount() * getOptions().getLancesPerCompany())
                + getOptions().getIndividualLanceCount()
                + (getOptions().isGenerateMercenaryCompanyCommandLance() ? 1 : 0);
    }

    private int determineNumberCompanyCommanders() {
        return getOptions().isGenerateCompanyCommanders()
                ? getOptions().getCompanyCount() - (getOptions().isGenerateMercenaryCompanyCommandLance() ? 0 : 1)
                : 0;
    }

    private int determineFirstNonOfficer() {
        return determineNumberLances() + (getOptions().isCompanyCommanderLanceOfficer() ? 0 : 1);
    }
    //endregion Determination Methods

    //region Personnel
    //region Combat Personnel
    private List<Person> generateCombatPersonnel(final Campaign campaign) {
        List<Person> combatPersonnel = new ArrayList<>();
        final int numMechWarriors = determineNumberLances() * getOptions().getLanceSize();
        for (int i = 0; i < numMechWarriors; i++) {
            combatPersonnel.add(campaign.newPerson(Person.T_MECHWARRIOR));
        }

        if (getOptions().isAssignBestOfficers()) {
            combatPersonnel = combatPersonnel.stream()
                    .sorted(Comparator.comparingInt(o -> o.getExperienceLevel(false)))
                    .sorted(Comparator.comparingInt(o -> o.getSkillLevel(SkillType.S_LEADER)
                            + o.getSkillLevel(SkillType.S_STRATEGY) + o.getSkillLevel(SkillType.S_TACTICS)))
                    .collect(Collectors.toList());
        }

        generateCommandingOfficer(combatPersonnel.get(0), numMechWarriors);

        generateOfficers(combatPersonnel);

        generateStandardMechWarriors(combatPersonnel);

        return combatPersonnel;
    }

    /**
     * Turns a person into the commanding officer of the force being generated
     * 1) Assigns the Commander flag
     * 2) Improves Gunnery and Piloting by one level
     * 3) Gets two random officer skill increases
     * 4) Gets the highest rank possible assigned to them
     *
     * @param commandingOfficer the commanding officer
     * @param numMechWarriors the number of MechWarriors in their force, used to determine their rank
     */
    protected void generateCommandingOfficer(final Person commandingOfficer, final int numMechWarriors) {
        commandingOfficer.setCommander(true);
        commandingOfficer.improveSkill(SkillType.S_GUN_MECH);
        commandingOfficer.improveSkill(SkillType.S_PILOT_MECH);
        assignRandomOfficerSkillIncrease(commandingOfficer, 2);

        if (getOptions().isAutomaticallyAssignRanks()) {
            if (numMechWarriors >= 36) {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + (getOptions().getFaction().isComStarOrWoB() ? 7 : 8));
            } else if (numMechWarriors >= 12) {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + (getOptions().getFaction().isComStarOrWoB() ? 7 : 5));
            } else if (numMechWarriors >= 4) {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + 4);
            } else {
                commandingOfficer.setRankLevel(Ranks.RWO_MAX + 3);
            }
        }
    }

    /**
     * This generates officers based on the provided options.
     *
     * Custom addition for larger generation:
     * For every company (with a mercenary company command lance) or for every company
     * after the first (as the mercenary company commander is the leader of that company) you
     * generate a O4 - Captain, provided that company commander generation is enabled. These get
     * two officer skill boosts instead of 1, and the rank of O4 - Captain instead of O3 - Lieutenant.
     *
     * An Officer gets:
     * 1) An increase of one to either the highest or lowest skill of gunnery or piloting, depending
     * on the set options
     * 2) Two random officer skill increases if they are a company commander, otherwise they get one
     * 3) A rank of O4 - Captain for Company Commanders, otherwise O3 - Lieutenant
     *
     * @param personnel the list of all generated personnel
     */
    private void generateOfficers(final List<Person> personnel) {
        int companyCommanders = determineNumberCompanyCommanders();
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

            if (companyCommanders > 0) {
                // Assign Random Officer Skill Increase
                assignRandomOfficerSkillIncrease(officer, 2);

                if (getOptions().isAutomaticallyAssignRanks()) {
                    // Assign Rank of O4 - Captain
                    officer.setRankLevel(Ranks.RWO_MAX + 4);
                }

                // Decrement the number of company commanders
                companyCommanders--;
            } else {
                // Assign Random Officer Skill Increase
                assignRandomOfficerSkillIncrease(officer, 1);

                if (getOptions().isAutomaticallyAssignRanks()) {
                    // Assign Rank of O3 - Lieutenant
                    officer.setRankLevel(Ranks.RWO_MAX + 3);
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
    protected void assignRandomOfficerSkillIncrease(final Person person, final int boosts) {
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
                personnel.get(i).setRankLevel(isClanComStarOrWoB ? 4 : 12);
            }
        }
    }
    //endregion Combat Personnel

    //region Support Personnel
    /**
     * @param campaign the campaign to generate from
     * @return a list of all support personnel
     */
    private List<Person> generateSupportPersonnel(final Campaign campaign) {
        List<Person> supportPersonnel = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : getOptions().getSupportPersonnel().entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                final Person person = campaign.newPerson(entry.getKey());
                // All support personnel get assigned is their rank
                if (getOptions().isAutomaticallyAssignRanks()) {
                    switch (campaign.getRanks().getRankSystem()) {
                        case Ranks.RS_CCWH:
                        case Ranks.RS_CL:
                            break;
                        case Ranks.RS_COM:
                        case Ranks.RS_WOB:
                        case Ranks.RS_MOC:
                            person.setRankLevel(4);
                            break;
                        default:
                            person.setRankLevel(8);
                            break;
                    }
                }
                supportPersonnel.add(person);
            }
        }
        return supportPersonnel;
    }
    //endregion Support Personnel
    //endregion Personnel

    //region Units
    //endregion Units

    //region Unit
    private List<Person> sortPersonnelIntoLances(final List<Person> personnel) {
        final Person commander = personnel.get(0);
        List<Person> officers = new ArrayList<>(personnel.subList(1, determineFirstNonOfficer()));
        List<Person> standardMechWarriors = new ArrayList<>(personnel.subList(determineFirstNonOfficer(), personnel.size()));

        return new ArrayList<>(personnel);
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
            if (MHQStaticDirectoryManager.getForceIcons().getItems().keySet().stream()
                    .anyMatch(s -> s.equalsIgnoreCase(getOptions().getFaction().getFullName(campaign.getGameYear())))) {
                background = getOptions().getFaction().getFullName(campaign.getGameYear());
            }

            if (background.isEmpty() && (MHQStaticDirectoryManager.getForceIcons().getItems().keySet()
                    .stream().anyMatch(s -> s.equalsIgnoreCase(getOptions().getFaction().getShortName())))) {
                background = getOptions().getFaction().getShortName();
            }
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
            iconMap.put(LayeredForceIcon.BACKGROUND.getLayerPath(), new Vector<>());
            iconMap.get(LayeredForceIcon.BACKGROUND.getLayerPath()).add(background);

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
            createLance(campaign, originForce, personnel, alphabet[i], background);
        }
    }

    private void createLance(final Campaign campaign, final Force head, final List<Person> personnel,
                             final Alphabet alphabet, final String background) {
        createLance(campaign, head, personnel,
                getOptions().getForceNamingType().getValue(alphabet)
                        + resources.getString("AbstractCompanyGenerator.lance.text"),
                background);
    }

    private Force createLance(final Campaign campaign, final Force head,final List<Person> personnel,
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

    private void createLayeredForceIcon(final Campaign campaign, final Force force,
                                        final boolean isLance, final String background) {
        if (MHQStaticDirectoryManager.getForceIcons() == null) {
            return;
        }

        final LinkedHashMap<String, Vector<String>> iconMap = new LinkedHashMap<>();

        // Type
        final int weightClass = determineLanceWeightClass(campaign, force, isLance);
        final String weightClassName = (weightClass == EntityWeightClass.WEIGHT_SUPER_HEAVY)
                ? "Superheavy" : EntityWeightClass.getClassName(weightClass);
        String filename = String.format("BattleMech %s.png", weightClassName);
        if (!MHQStaticDirectoryManager.getForceIcons().getItems().containsKey(filename)) {
            filename = "BattleMech.png";
        }
        iconMap.put(LayeredForceIcon.TYPE.getLayerPath(), new Vector<>());
        iconMap.get(LayeredForceIcon.TYPE.getLayerPath()).add(filename);

        // Formation
        iconMap.put(LayeredForceIcon.FORMATION.getLayerPath(), new Vector<>());
        iconMap.get(LayeredForceIcon.FORMATION.getLayerPath()).add(isLance ? "04 Lance.png" : "06 Company.png");

        // Background
        if (!background.isEmpty()) {
            iconMap.put(LayeredForceIcon.BACKGROUND.getLayerPath(), new Vector<>());
            iconMap.get(LayeredForceIcon.BACKGROUND.getLayerPath()).add(background);
        }

        // Frame
        iconMap.put(LayeredForceIcon.FRAME.getLayerPath(), new Vector<>());
        iconMap.get(LayeredForceIcon.FRAME.getLayerPath()).add("Frame.png");
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
        for (UUID unitId : force.getAllUnits(true)) {
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

    //region Finances
    private void processFinances(final Campaign campaign, final List<Person> personnel) {
        if (getOptions().isPayForSetup()) {
            Money unitCosts = calculateUnitCosts();
            Money hiringCosts = calculateHiringCosts(personnel);
            Money partCosts = calculatePartCosts();
            Money ammunitionCosts = calculateAmmunitionCosts();
        }
    }
    private Money calculateUnitCosts() {
        if (!getOptions().isPayForUnits()) {
            return Money.zero();
        }

        return Money.zero();
    }

    private Money calculateHiringCosts(List<Person> personnel) {
        if (!getOptions().isPayForPersonnel()) {
            return Money.zero();
        }

        Money hiringCosts = Money.zero();
        for (Person person : personnel) {
            hiringCosts = hiringCosts.plus(person.getSalary().multipliedBy(2));
        }
        return hiringCosts;
    }

    private Money calculatePartCosts() {
        if (!getOptions().isPayForParts()) {
            return Money.zero();
        }

        return Money.zero();
    }

    private Money calculateAmmunitionCosts() {
        if (!getOptions().isPayForAmmunition()) {
            return Money.zero();
        }

        return Money.zero();
    }

    //endregion Finances

    public void applyToCampaign(final Campaign campaign, final List<Person> combatPersonnel,
                                final List<Person> supportPersonnel) {
        generateUnit(campaign, sortPersonnelIntoLances(combatPersonnel));

        List<Person> personnel = new ArrayList<>(supportPersonnel);
        personnel.addAll(combatPersonnel);

        if (getOptions().isPoolAssistants()) {
            campaign.fillAstechPool();
            campaign.fillMedicPool();
        } else {
            for (int i = 0; i < campaign.getAstechNeed(); i++) {
                personnel.add(campaign.newPerson(Person.T_ASTECH));
            }
            for (int i = 0; i < campaign.getMedicsNeed(); i++) {
                personnel.add(campaign.newPerson(Person.T_MEDIC));
            }
        }

        processFinances(campaign, personnel);
    }
}
