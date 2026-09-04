/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.commandGeneration;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import mekhq.campaign.universe.enums.TechAssignmentSortFactor;

/**
 * Options for the Command Generator (the ratgen-driven force-generation pipeline behind the Command
 * Designer dialog).
 *
 * <p>This is the standalone successor to the deprecated AtB-era {@code CompanyGenerationOptions}: it
 * carries only the members the Command Generator actually consumes, with defaults matching the
 * legacy {@code RULESET_BASED} configuration. The rolled force itself is described by the embedded
 * {@link ForceDescriptorSnapshot}; everything else here controls how {@link CommandGenerator}
 * materializes the roll into the campaign (support staff, tech assignment, ranks, flags, formation
 * icons, and the OtherTab simulation / contract / finance toggles).</p>
 *
 * <p>The options are dialog-lifetime state: they are populated from the Command Designer tabs on
 * each run and are not persisted.</p>
 */
public class CommandGenerationOptions {

    /**
     * Support roles seeded with default coverage percentages and skill levels: the four tech roles,
     * doctor, and administrators - matches the SetupTab spinner/dropdown layout.
     */
    private static final PersonnelRole[] SUPPORT_ROLES_FOR_COVERAGE = {
          PersonnelRole.MEK_TECH,
          PersonnelRole.MECHANIC,
          PersonnelRole.AERO_TEK,
          PersonnelRole.BA_TECH,
          PersonnelRole.DOCTOR,
          PersonnelRole.ADMINISTRATOR
    };

    // region Variable Declarations
    // Base information
    private Faction specifiedFaction;
    private boolean generateMercenaryCompanyCommandLance;

    /**
     * Inputs for the ratgen roll (faction, year, echelon, unit type, rating, experience, and weight).
     * Lazy-initialized on first access so the options object is always usable.
     */
    private ForceDescriptorSnapshot forceDescriptorSnapshot;

    // Support personnel - per-role coverage percentages and skill levels.
    // Coverage: 100 = full canonical coverage, 0 = generate none, >100 = redundancy.
    // Skill level: a fixed SkillLevel, or SkillLevel.NONE for "Random" (each person rolls their own level).
    private Map<PersonnelRole, Integer> supportPersonnelCoveragePercents;
    private Map<PersonnelRole, SkillLevel> supportPersonnelSkillLevels;
    // Astech / Medic generation - each auxiliary type can independently be skipped, pooled, or
    // generated as named Persons. Skill level is a fixed SkillLevel, or SkillLevel.NONE for "Random".
    private boolean generateAstechs;
    private boolean astechsAsPersonnel;
    private SkillLevel astechSkillLevel;
    private boolean generateMedics;
    private boolean medicsAsPersonnel;
    private SkillLevel medicSkillLevel;
    // Medical reserve (generation-only): optionally create spare unassigned MekWarriors as injury
    // replacements, sized as a percentage of the generated combatants.
    private boolean generateMedicalReserve;
    private int medicalReservePercent;

    // Tech-to-unit assignment sort grid. Three sort slots each holding a
    // TechAssignmentSortFactor; the assigner chains them in slot order (primary -> secondary ->
    // tertiary). Each slot has its own ascending / descending direction.
    private boolean assignTechsToUnits;
    private TechAssignmentSortFactor techAssignmentPrimarySort;
    private boolean techAssignmentPrimaryDescending;
    private TechAssignmentSortFactor techAssignmentSecondarySort;
    private boolean techAssignmentSecondaryDescending;
    private TechAssignmentSortFactor techAssignmentTertiarySort;
    private boolean techAssignmentTertiaryDescending;

    // Officer assignment and personnel flags
    private boolean generateCaptains;
    private boolean assignCompanyCommanderFlag;
    private boolean applyOfficerStatBonusToWorstSkill;
    private boolean assignBestCompanyCommander;
    private boolean prioritizeCompanyCommanderCombatSkills;
    private boolean assignBestOfficers;
    private boolean prioritizeOfficerCombatSkills;
    private boolean assignMostSkilledToPrimaryLances;
    private boolean automaticallyAssignRanks;
    private boolean useSpecifiedFactionToAssignRanks;
    private boolean assignMekWarriorsCallSigns;
    private boolean assignFounderFlag;

    // Augmentation rules. These mirror settings that live on the campaign and on MegaMek's game
    // options rather than belonging to a generation run, and are carried here so the Command
    // Generator can both read them and write the player's choice back - a new campaign has them all
    // off, and a player who has never opened either options dialog would otherwise have no way to
    // generate an augmented command.
    private boolean useImplants;
    /**
     * The crew roles whose seats temporary crew fill instead of named people. These live on the campaign: the
     * dialog seeds them from the campaign's current settings and the generator writes them back before any unit is
     * crewed, because the crew assembler reads the campaign options as it builds each unit.
     */
    private Set<TemporaryCrewRole> temporaryCrewRoles;
    private boolean useManeiDomini;
    private NeuralInterfaceMode neuralInterfaceMode;

    // Force naming and formation icons
    private ForceNamingMethod forceNamingMethod;
    private boolean alwaysNumberRegiments;
    private boolean generateFormationIcons;
    private boolean useSpecifiedFactionToGenerateFormationIcons;
    private boolean generateOriginNodeFormationIcon;
    private boolean useOriginNodeFormationIconLogo;

    // Starting simulation
    private boolean runStartingSimulation;
    private int simulationDuration = 10;
    private boolean simulateRandomMarriages;
    private boolean simulateRandomProcreation;

    // Contracts
    private boolean selectStartingContract;
    private boolean startCourseToContractPlanet;

    // Finances. Starting cash is working capital sized as a percentage of the generated units'
    // total purchase cost (or a dice roll when randomized); the pay-for toggles then debit the
    // command's real generation costs, flooring at the minimum float with an optional loan for the
    // shortfall.
    private boolean processFinances;
    private int startingCashPercent;
    private boolean randomizeStartingCash;
    private int randomStartingCashDiceCount;
    private int minimumStartingFloat;
    private boolean startingLoan;
    private boolean payForSetup;
    private boolean payForPersonnel;
    private boolean payForUnits;
    private boolean payForParts;
    private boolean payForArmour;
    private boolean payForAmmunition;
    // endregion Variable Declarations

    // region Constructors
    /**
     * Creates the options seeded with the Command Generator defaults (the legacy
     * {@code RULESET_BASED} configuration).
     */
    public CommandGenerationOptions() {
        // Base information
        setSpecifiedFaction(Factions.getInstance().getDefaultFaction());
        setGenerateMercenaryCompanyCommandLance(false);

        // Support personnel: full canonical coverage for every support role, with skill level left at
        // the "Random" default (SkillLevel.NONE) so each generated person rolls their own level. The user opts
        // into full coverage changes or a fixed skill level via the SetupTab.
        final Map<PersonnelRole, Integer> coveragePercents = new HashMap<>();
        final Map<PersonnelRole, SkillLevel> skillLevels = new HashMap<>();
        for (final PersonnelRole role : SUPPORT_ROLES_FOR_COVERAGE) {
            coveragePercents.put(role, 100);
            skillLevels.put(role, SkillLevel.NONE);
        }
        setSupportPersonnelCoveragePercents(coveragePercents);
        setSupportPersonnelSkillLevels(skillLevels);

        // Astech / Medic generation defaults: generated as pools out-of-box; opting into
        // named-Persons mode is explicit. Skill level defaults to Random (SkillLevel.NONE).
        setGenerateAstechs(true);
        setAstechsAsPersonnel(false);
        setAstechSkillLevel(SkillLevel.NONE);
        setGenerateMedics(true);
        setMedicsAsPersonnel(false);
        setMedicSkillLevel(SkillLevel.NONE);
        setGenerateMedicalReserve(false);
        setMedicalReservePercent(10);

        // Tech-to-unit assignment grid defaults: officers first, then heaviest, then best pilot.
        setAssignTechsToUnits(true);
        setTechAssignmentPrimarySort(TechAssignmentSortFactor.PILOT_RANK);
        setTechAssignmentPrimaryDescending(true);
        setTechAssignmentSecondarySort(TechAssignmentSortFactor.UNIT_WEIGHT);
        setTechAssignmentSecondaryDescending(true);
        setTechAssignmentTertiarySort(TechAssignmentSortFactor.PILOT_SKILL);
        setTechAssignmentTertiaryDescending(true);

        // Officer assignment and personnel flags
        setGenerateCaptains(false);
        setAssignCompanyCommanderFlag(true);
        setApplyOfficerStatBonusToWorstSkill(false);
        setAssignBestCompanyCommander(false);
        setPrioritizeCompanyCommanderCombatSkills(false);
        setAssignBestOfficers(false);
        setPrioritizeOfficerCombatSkills(false);
        setAssignMostSkilledToPrimaryLances(false);
        setAutomaticallyAssignRanks(true);
        // Default ON so the target faction's rank system (Clan ranks for a Clan target, ComStar
        // ranks for a CS target, etc.) drives the generated commanders' rank names.
        setUseSpecifiedFactionToAssignRanks(true);
        setAssignMekWarriorsCallSigns(true);
        setAssignFounderFlag(true);

        // Left off, matching a fresh campaign. The dialog replaces these with whatever the campaign
        // currently has, so the defaults here only matter to a caller that never opens it.
        setUseImplants(false);
        setUseManeiDomini(false);
        setNeuralInterfaceMode(NeuralInterfaceMode.OFF);
        // None by default, matching a fresh campaign; the dialog replaces this with the campaign's own settings.
        setTemporaryCrewRoles(EnumSet.noneOf(TemporaryCrewRole.class));

        // Force naming and formation icons
        setForceNamingMethod(ForceNamingMethod.CCB_1943);
        // On by default: numbered regiments ("1st Mek Regiment") are the form players expect,
        // and the naming alphabet still drives every echelon below regiment level.
        setAlwaysNumberRegiments(true);
        setGenerateFormationIcons(true);
        setUseSpecifiedFactionToGenerateFormationIcons(false);
        setGenerateOriginNodeFormationIcon(true);
        setUseOriginNodeFormationIconLogo(false);

        // Starting simulation
        setRunStartingSimulation(false);
        setSimulationDuration(10);
        setSimulateRandomMarriages(false);
        setSimulateRandomProcreation(false);

        // Contracts
        setSelectStartingContract(true);
        setStartCourseToContractPlanet(true);

        // Finances. Pay for Initial Setup defaults OFF: with the percentage-of-unit-value cash base,
        // paying for the units would always dwarf the base (10% cash vs 100% cost) and floor every
        // default build into a maximum loan. Out of the box the command is granted free with its
        // working capital; players opt into the pay-for accounting.
        setProcessFinances(true);
        setStartingCashPercent(10);
        setRandomizeStartingCash(false);
        setRandomStartingCashDiceCount(18);
        setMinimumStartingFloat(0);
        setStartingLoan(true);
        setPayForSetup(false);
        setPayForPersonnel(true);
        setPayForUnits(true);
        setPayForParts(true);
        setPayForArmour(true);
        setPayForAmmunition(true);
    }
    // endregion Constructors

    // region Getters/Setters
    public Faction getSpecifiedFaction() {
        return specifiedFaction;
    }

    public void setSpecifiedFaction(final Faction specifiedFaction) {
        this.specifiedFaction = specifiedFaction;
    }

    public boolean isGenerateMercenaryCompanyCommandLance() {
        return generateMercenaryCompanyCommandLance;
    }

    public void setGenerateMercenaryCompanyCommandLance(final boolean generateMercenaryCompanyCommandLance) {
        this.generateMercenaryCompanyCommandLance = generateMercenaryCompanyCommandLance;
    }

    /**
     * The ratgen roll inputs. Lazy-initialized so this never returns {@code null}.
     *
     * @return the force-descriptor snapshot backing the roll
     */
    public ForceDescriptorSnapshot getForceDescriptorSnapshot() {
        if (forceDescriptorSnapshot == null) {
            forceDescriptorSnapshot = new ForceDescriptorSnapshot();
        }
        return forceDescriptorSnapshot;
    }

    public void setForceDescriptorSnapshot(final ForceDescriptorSnapshot forceDescriptorSnapshot) {
        this.forceDescriptorSnapshot = forceDescriptorSnapshot;
    }

    public Map<PersonnelRole, Integer> getSupportPersonnelCoveragePercents() {
        return supportPersonnelCoveragePercents;
    }

    public void setSupportPersonnelCoveragePercents(final Map<PersonnelRole, Integer> supportPersonnelCoveragePercents) {
        this.supportPersonnelCoveragePercents = supportPersonnelCoveragePercents;
    }

    public Map<PersonnelRole, SkillLevel> getSupportPersonnelSkillLevels() {
        return supportPersonnelSkillLevels;
    }

    public void setSupportPersonnelSkillLevels(final Map<PersonnelRole, SkillLevel> supportPersonnelSkillLevels) {
        this.supportPersonnelSkillLevels = supportPersonnelSkillLevels;
    }

    public boolean isGenerateAstechs() {
        return generateAstechs;
    }

    public void setGenerateAstechs(final boolean generateAstechs) {
        this.generateAstechs = generateAstechs;
    }

    public boolean isAstechsAsPersonnel() {
        return astechsAsPersonnel;
    }

    public void setAstechsAsPersonnel(final boolean astechsAsPersonnel) {
        this.astechsAsPersonnel = astechsAsPersonnel;
    }

    public SkillLevel getAstechSkillLevel() {
        return astechSkillLevel;
    }

    public void setAstechSkillLevel(final SkillLevel astechSkillLevel) {
        this.astechSkillLevel = astechSkillLevel;
    }

    public boolean isGenerateMedics() {
        return generateMedics;
    }

    public void setGenerateMedics(final boolean generateMedics) {
        this.generateMedics = generateMedics;
    }

    public boolean isMedicsAsPersonnel() {
        return medicsAsPersonnel;
    }

    public void setMedicsAsPersonnel(final boolean medicsAsPersonnel) {
        this.medicsAsPersonnel = medicsAsPersonnel;
    }

    public SkillLevel getMedicSkillLevel() {
        return medicSkillLevel;
    }

    public void setMedicSkillLevel(final SkillLevel medicSkillLevel) {
        this.medicSkillLevel = medicSkillLevel;
    }

    public boolean isGenerateMedicalReserve() {
        return generateMedicalReserve;
    }

    public void setGenerateMedicalReserve(final boolean generateMedicalReserve) {
        this.generateMedicalReserve = generateMedicalReserve;
    }

    public int getMedicalReservePercent() {
        return medicalReservePercent;
    }

    public void setMedicalReservePercent(final int medicalReservePercent) {
        this.medicalReservePercent = medicalReservePercent;
    }

    public boolean isAssignTechsToUnits() {
        return assignTechsToUnits;
    }

    public void setAssignTechsToUnits(final boolean assignTechsToUnits) {
        this.assignTechsToUnits = assignTechsToUnits;
    }

    public TechAssignmentSortFactor getTechAssignmentPrimarySort() {
        return techAssignmentPrimarySort;
    }

    public void setTechAssignmentPrimarySort(final TechAssignmentSortFactor techAssignmentPrimarySort) {
        this.techAssignmentPrimarySort = techAssignmentPrimarySort;
    }

    public boolean isTechAssignmentPrimaryDescending() {
        return techAssignmentPrimaryDescending;
    }

    public void setTechAssignmentPrimaryDescending(final boolean techAssignmentPrimaryDescending) {
        this.techAssignmentPrimaryDescending = techAssignmentPrimaryDescending;
    }

    public TechAssignmentSortFactor getTechAssignmentSecondarySort() {
        return techAssignmentSecondarySort;
    }

    public void setTechAssignmentSecondarySort(final TechAssignmentSortFactor techAssignmentSecondarySort) {
        this.techAssignmentSecondarySort = techAssignmentSecondarySort;
    }

    public boolean isTechAssignmentSecondaryDescending() {
        return techAssignmentSecondaryDescending;
    }

    public void setTechAssignmentSecondaryDescending(final boolean techAssignmentSecondaryDescending) {
        this.techAssignmentSecondaryDescending = techAssignmentSecondaryDescending;
    }

    public TechAssignmentSortFactor getTechAssignmentTertiarySort() {
        return techAssignmentTertiarySort;
    }

    public void setTechAssignmentTertiarySort(final TechAssignmentSortFactor techAssignmentTertiarySort) {
        this.techAssignmentTertiarySort = techAssignmentTertiarySort;
    }

    public boolean isTechAssignmentTertiaryDescending() {
        return techAssignmentTertiaryDescending;
    }

    public void setTechAssignmentTertiaryDescending(final boolean techAssignmentTertiaryDescending) {
        this.techAssignmentTertiaryDescending = techAssignmentTertiaryDescending;
    }

    public boolean isGenerateCaptains() {
        return generateCaptains;
    }

    public void setGenerateCaptains(final boolean generateCaptains) {
        this.generateCaptains = generateCaptains;
    }

    public boolean isAssignCompanyCommanderFlag() {
        return assignCompanyCommanderFlag;
    }

    public void setAssignCompanyCommanderFlag(final boolean assignCompanyCommanderFlag) {
        this.assignCompanyCommanderFlag = assignCompanyCommanderFlag;
    }

    public boolean isApplyOfficerStatBonusToWorstSkill() {
        return applyOfficerStatBonusToWorstSkill;
    }

    public void setApplyOfficerStatBonusToWorstSkill(final boolean applyOfficerStatBonusToWorstSkill) {
        this.applyOfficerStatBonusToWorstSkill = applyOfficerStatBonusToWorstSkill;
    }

    public boolean isAssignBestCompanyCommander() {
        return assignBestCompanyCommander;
    }

    public void setAssignBestCompanyCommander(final boolean assignBestCompanyCommander) {
        this.assignBestCompanyCommander = assignBestCompanyCommander;
    }

    public boolean isPrioritizeCompanyCommanderCombatSkills() {
        return prioritizeCompanyCommanderCombatSkills;
    }

    public void setPrioritizeCompanyCommanderCombatSkills(final boolean prioritizeCompanyCommanderCombatSkills) {
        this.prioritizeCompanyCommanderCombatSkills = prioritizeCompanyCommanderCombatSkills;
    }

    public boolean isAssignBestOfficers() {
        return assignBestOfficers;
    }

    public void setAssignBestOfficers(final boolean assignBestOfficers) {
        this.assignBestOfficers = assignBestOfficers;
    }

    public boolean isPrioritizeOfficerCombatSkills() {
        return prioritizeOfficerCombatSkills;
    }

    public void setPrioritizeOfficerCombatSkills(final boolean prioritizeOfficerCombatSkills) {
        this.prioritizeOfficerCombatSkills = prioritizeOfficerCombatSkills;
    }

    public boolean isAssignMostSkilledToPrimaryLances() {
        return assignMostSkilledToPrimaryLances;
    }

    public void setAssignMostSkilledToPrimaryLances(final boolean assignMostSkilledToPrimaryLances) {
        this.assignMostSkilledToPrimaryLances = assignMostSkilledToPrimaryLances;
    }

    public boolean isAutomaticallyAssignRanks() {
        return automaticallyAssignRanks;
    }

    public void setAutomaticallyAssignRanks(final boolean automaticallyAssignRanks) {
        this.automaticallyAssignRanks = automaticallyAssignRanks;
    }

    public boolean isUseSpecifiedFactionToAssignRanks() {
        return useSpecifiedFactionToAssignRanks;
    }

    public void setUseSpecifiedFactionToAssignRanks(final boolean useSpecifiedFactionToAssignRanks) {
        this.useSpecifiedFactionToAssignRanks = useSpecifiedFactionToAssignRanks;
    }

    public boolean isAssignMekWarriorsCallSigns() {
        return assignMekWarriorsCallSigns;
    }

    public void setAssignMekWarriorsCallSigns(final boolean assignMekWarriorsCallSigns) {
        this.assignMekWarriorsCallSigns = assignMekWarriorsCallSigns;
    }

    public boolean isAssignFounderFlag() {
        return assignFounderFlag;
    }

    public void setAssignFounderFlag(final boolean assignFounderFlag) {
        this.assignFounderFlag = assignFounderFlag;
    }

    /**
     * @return whether the campaign should track cybernetic implants, MekHQ's gate on every kind of
     *       augmentation
     */
    public boolean isUseImplants() {
        return useImplants;
    }

    public void setUseImplants(final boolean useImplants) {
        this.useImplants = useImplants;
    }

    /**
     * @return the crew roles whose seats temporary crew fill instead of named people; never {@code null}
     */
    public Set<TemporaryCrewRole> getTemporaryCrewRoles() {
        return temporaryCrewRoles;
    }

    /**
     * @param temporaryCrewRoles the crew roles whose seats temporary crew fill; {@code null} means none
     */
    public void setTemporaryCrewRoles(@Nullable final Set<TemporaryCrewRole> temporaryCrewRoles) {
        this.temporaryCrewRoles = (temporaryCrewRoles == null)
                                        ? EnumSet.noneOf(TemporaryCrewRole.class)
                                        : EnumSet.copyOf(temporaryCrewRoles);
    }

    /**
     * @return whether MegaMek's Manei Domini rule is in play, without which a Shadow Division's
     *       implants do nothing
     */
    public boolean isUseManeiDomini() {
        return useManeiDomini;
    }

    public void setUseManeiDomini(final boolean useManeiDomini) {
        this.useManeiDomini = useManeiDomini;
    }

    /**
     * @return which of MegaMek's neural interface rules is in play, which decides whether an enhanced
     *       imaging or direct neural implant does anything
     */
    public NeuralInterfaceMode getNeuralInterfaceMode() {
        return neuralInterfaceMode;
    }

    public void setNeuralInterfaceMode(final NeuralInterfaceMode neuralInterfaceMode) {
        this.neuralInterfaceMode = neuralInterfaceMode;
    }

    public ForceNamingMethod getForceNamingMethod() {
        return forceNamingMethod;
    }

    public void setForceNamingMethod(final ForceNamingMethod forceNamingMethod) {
        this.forceNamingMethod = forceNamingMethod;
    }

    /**
     * @return {@code true} when regiment-level formations take a numeric ordinal ("1st Mek Regiment")
     *       instead of the selected naming alphabet, with each type of regiment counted separately
     */
    public boolean isAlwaysNumberRegiments() {
        return alwaysNumberRegiments;
    }

    public void setAlwaysNumberRegiments(final boolean alwaysNumberRegiments) {
        this.alwaysNumberRegiments = alwaysNumberRegiments;
    }

    public boolean isGenerateFormationIcons() {
        return generateFormationIcons;
    }

    public void setGenerateFormationIcons(final boolean generateFormationIcons) {
        this.generateFormationIcons = generateFormationIcons;
    }

    public boolean isUseSpecifiedFactionToGenerateFormationIcons() {
        return useSpecifiedFactionToGenerateFormationIcons;
    }

    public void setUseSpecifiedFactionToGenerateFormationIcons(final boolean useSpecifiedFactionToGenerateFormationIcons) {
        this.useSpecifiedFactionToGenerateFormationIcons = useSpecifiedFactionToGenerateFormationIcons;
    }

    public boolean isGenerateOriginNodeFormationIcon() {
        return generateOriginNodeFormationIcon;
    }

    public void setGenerateOriginNodeFormationIcon(final boolean generateOriginNodeFormationIcon) {
        this.generateOriginNodeFormationIcon = generateOriginNodeFormationIcon;
    }

    public boolean isUseOriginNodeFormationIconLogo() {
        return useOriginNodeFormationIconLogo;
    }

    public void setUseOriginNodeFormationIconLogo(final boolean useOriginNodeFormationIconLogo) {
        this.useOriginNodeFormationIconLogo = useOriginNodeFormationIconLogo;
    }

    public boolean isRunStartingSimulation() {
        return runStartingSimulation;
    }

    public void setRunStartingSimulation(final boolean runStartingSimulation) {
        this.runStartingSimulation = runStartingSimulation;
    }

    public int getSimulationDuration() {
        return simulationDuration;
    }

    public void setSimulationDuration(final int simulationDuration) {
        this.simulationDuration = simulationDuration;
    }

    public boolean isSimulateRandomMarriages() {
        return simulateRandomMarriages;
    }

    public void setSimulateRandomMarriages(final boolean simulateRandomMarriages) {
        this.simulateRandomMarriages = simulateRandomMarriages;
    }

    public boolean isSimulateRandomProcreation() {
        return simulateRandomProcreation;
    }

    public void setSimulateRandomProcreation(final boolean simulateRandomProcreation) {
        this.simulateRandomProcreation = simulateRandomProcreation;
    }

    public boolean isSelectStartingContract() {
        return selectStartingContract;
    }

    public void setSelectStartingContract(final boolean selectStartingContract) {
        this.selectStartingContract = selectStartingContract;
    }

    public boolean isStartCourseToContractPlanet() {
        return startCourseToContractPlanet;
    }

    public void setStartCourseToContractPlanet(final boolean startCourseToContractPlanet) {
        this.startCourseToContractPlanet = startCourseToContractPlanet;
    }

    public boolean isProcessFinances() {
        return processFinances;
    }

    public void setProcessFinances(final boolean processFinances) {
        this.processFinances = processFinances;
    }

    /**
     * Starting cash as a percentage of the generated units' total purchase cost. {@code 10} grants
     * working capital worth a tenth of the command's units.
     *
     * @return the starting-cash percentage
     */
    public int getStartingCashPercent() {
        return startingCashPercent;
    }

    public void setStartingCashPercent(final int startingCashPercent) {
        this.startingCashPercent = startingCashPercent;
    }

    public boolean isRandomizeStartingCash() {
        return randomizeStartingCash;
    }

    public void setRandomizeStartingCash(final boolean randomizeStartingCash) {
        this.randomizeStartingCash = randomizeStartingCash;
    }

    public int getRandomStartingCashDiceCount() {
        return randomStartingCashDiceCount;
    }

    public void setRandomStartingCashDiceCount(final int randomStartingCashDiceCount) {
        this.randomStartingCashDiceCount = randomStartingCashDiceCount;
    }

    public int getMinimumStartingFloat() {
        return minimumStartingFloat;
    }

    public void setMinimumStartingFloat(final int minimumStartingFloat) {
        this.minimumStartingFloat = minimumStartingFloat;
    }

    public boolean isStartingLoan() {
        return startingLoan;
    }

    public void setStartingLoan(final boolean startingLoan) {
        this.startingLoan = startingLoan;
    }

    public boolean isPayForSetup() {
        return payForSetup;
    }

    public void setPayForSetup(final boolean payForSetup) {
        this.payForSetup = payForSetup;
    }

    public boolean isPayForPersonnel() {
        return payForPersonnel;
    }

    public void setPayForPersonnel(final boolean payForPersonnel) {
        this.payForPersonnel = payForPersonnel;
    }

    public boolean isPayForUnits() {
        return payForUnits;
    }

    public void setPayForUnits(final boolean payForUnits) {
        this.payForUnits = payForUnits;
    }

    public boolean isPayForParts() {
        return payForParts;
    }

    public void setPayForParts(final boolean payForParts) {
        this.payForParts = payForParts;
    }

    public boolean isPayForArmour() {
        return payForArmour;
    }

    public void setPayForArmour(final boolean payForArmour) {
        this.payForArmour = payForArmour;
    }

    public boolean isPayForAmmunition() {
        return payForAmmunition;
    }

    public void setPayForAmmunition(final boolean payForAmmunition) {
        this.payForAmmunition = payForAmmunition;
    }
    // endregion Getters/Setters

    /**
     * Two CommandGenerationOptionss are equal when every setting matches, which is how the dialog tells whether the
     * settings have moved since the model was last generated.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CommandGenerationOptions other)) {
            return false;
        }
        return Objects.equals(specifiedFaction, other.specifiedFaction)
              && (generateMercenaryCompanyCommandLance == other.generateMercenaryCompanyCommandLance)
              && Objects.equals(forceDescriptorSnapshot, other.forceDescriptorSnapshot)
              && Objects.equals(supportPersonnelCoveragePercents, other.supportPersonnelCoveragePercents)
              && Objects.equals(supportPersonnelSkillLevels, other.supportPersonnelSkillLevels)
              && (generateAstechs == other.generateAstechs)
              && (astechsAsPersonnel == other.astechsAsPersonnel)
              && Objects.equals(astechSkillLevel, other.astechSkillLevel)
              && (generateMedics == other.generateMedics)
              && (medicsAsPersonnel == other.medicsAsPersonnel)
              && Objects.equals(medicSkillLevel, other.medicSkillLevel)
              && (generateMedicalReserve == other.generateMedicalReserve)
              && (medicalReservePercent == other.medicalReservePercent)
              && (assignTechsToUnits == other.assignTechsToUnits)
              && Objects.equals(techAssignmentPrimarySort, other.techAssignmentPrimarySort)
              && (techAssignmentPrimaryDescending == other.techAssignmentPrimaryDescending)
              && Objects.equals(techAssignmentSecondarySort, other.techAssignmentSecondarySort)
              && (techAssignmentSecondaryDescending == other.techAssignmentSecondaryDescending)
              && Objects.equals(techAssignmentTertiarySort, other.techAssignmentTertiarySort)
              && (techAssignmentTertiaryDescending == other.techAssignmentTertiaryDescending)
              && (generateCaptains == other.generateCaptains)
              && (assignCompanyCommanderFlag == other.assignCompanyCommanderFlag)
              && (applyOfficerStatBonusToWorstSkill == other.applyOfficerStatBonusToWorstSkill)
              && (assignBestCompanyCommander == other.assignBestCompanyCommander)
              && (prioritizeCompanyCommanderCombatSkills == other.prioritizeCompanyCommanderCombatSkills)
              && (assignBestOfficers == other.assignBestOfficers)
              && (prioritizeOfficerCombatSkills == other.prioritizeOfficerCombatSkills)
              && (assignMostSkilledToPrimaryLances == other.assignMostSkilledToPrimaryLances)
              && (automaticallyAssignRanks == other.automaticallyAssignRanks)
              && (useSpecifiedFactionToAssignRanks == other.useSpecifiedFactionToAssignRanks)
              && (assignMekWarriorsCallSigns == other.assignMekWarriorsCallSigns)
              && (assignFounderFlag == other.assignFounderFlag)
              && (useImplants == other.useImplants)
              && Objects.equals(temporaryCrewRoles, other.temporaryCrewRoles)
              && (useManeiDomini == other.useManeiDomini)
              && Objects.equals(neuralInterfaceMode, other.neuralInterfaceMode)
              && Objects.equals(forceNamingMethod, other.forceNamingMethod)
              && (alwaysNumberRegiments == other.alwaysNumberRegiments)
              && (generateFormationIcons == other.generateFormationIcons)
              && (useSpecifiedFactionToGenerateFormationIcons == other.useSpecifiedFactionToGenerateFormationIcons)
              && (generateOriginNodeFormationIcon == other.generateOriginNodeFormationIcon)
              && (useOriginNodeFormationIconLogo == other.useOriginNodeFormationIconLogo)
              && (runStartingSimulation == other.runStartingSimulation)
              && (simulationDuration == other.simulationDuration)
              && (simulateRandomMarriages == other.simulateRandomMarriages)
              && (simulateRandomProcreation == other.simulateRandomProcreation)
              && (selectStartingContract == other.selectStartingContract)
              && (startCourseToContractPlanet == other.startCourseToContractPlanet)
              && (processFinances == other.processFinances)
              && (startingCashPercent == other.startingCashPercent)
              && (randomizeStartingCash == other.randomizeStartingCash)
              && (randomStartingCashDiceCount == other.randomStartingCashDiceCount)
              && (minimumStartingFloat == other.minimumStartingFloat)
              && (startingLoan == other.startingLoan)
              && (payForSetup == other.payForSetup)
              && (payForPersonnel == other.payForPersonnel)
              && (payForUnits == other.payForUnits)
              && (payForParts == other.payForParts)
              && (payForArmour == other.payForArmour)
              && (payForAmmunition == other.payForAmmunition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(specifiedFaction, 
              generateMercenaryCompanyCommandLance, 
              forceDescriptorSnapshot, 
              supportPersonnelCoveragePercents, 
              supportPersonnelSkillLevels, 
              generateAstechs, 
              astechsAsPersonnel, 
              astechSkillLevel, 
              generateMedics, 
              medicsAsPersonnel, 
              medicSkillLevel, 
              generateMedicalReserve, 
              medicalReservePercent, 
              assignTechsToUnits, 
              techAssignmentPrimarySort, 
              techAssignmentPrimaryDescending, 
              techAssignmentSecondarySort, 
              techAssignmentSecondaryDescending, 
              techAssignmentTertiarySort, 
              techAssignmentTertiaryDescending, 
              generateCaptains, 
              assignCompanyCommanderFlag, 
              applyOfficerStatBonusToWorstSkill, 
              assignBestCompanyCommander, 
              prioritizeCompanyCommanderCombatSkills, 
              assignBestOfficers, 
              prioritizeOfficerCombatSkills, 
              assignMostSkilledToPrimaryLances, 
              automaticallyAssignRanks, 
              useSpecifiedFactionToAssignRanks, 
              assignMekWarriorsCallSigns, 
              assignFounderFlag, 
              useImplants, 
              temporaryCrewRoles, 
              useManeiDomini, 
              neuralInterfaceMode, 
              forceNamingMethod, 
              alwaysNumberRegiments, 
              generateFormationIcons, 
              useSpecifiedFactionToGenerateFormationIcons, 
              generateOriginNodeFormationIcon, 
              useOriginNodeFormationIconLogo, 
              runStartingSimulation, 
              simulationDuration, 
              simulateRandomMarriages, 
              simulateRandomProcreation, 
              selectStartingContract, 
              startCourseToContractPlanet, 
              processFinances, 
              startingCashPercent, 
              randomizeStartingCash, 
              randomStartingCashDiceCount, 
              minimumStartingFloat, 
              startingLoan, 
              payForSetup, 
              payForPersonnel, 
              payForUnits, 
              payForParts, 
              payForArmour, 
              payForAmmunition);
    }
}
