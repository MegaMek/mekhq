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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.annotation.Nullable;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.commandGeneration.ratgen.CommandGenerator;
import mekhq.campaign.universe.commandGeneration.ratgen.ForceDescriptorSnapshot;
import mekhq.campaign.universe.enums.ForceNamingMethod;

/**
 * Options for the Command Generator (the ratgen-driven force-generation pipeline behind the Command
 * Designer dialog).
 *
 * <p>This is the standalone successor to the deprecated AtB-era {@code CompanyGenerationOptions}: it
 * carries only the members the Command Generator actually consumes, with defaults matching the
 * legacy {@code RULESET_BASED} configuration. The rolled force itself is described by the embedded
 * {@link ForceDescriptorSnapshot}; everything else here controls how {@link CommandGenerator}
 * materializes the roll into the campaign (support staff, random origin, ranks, flags, formation
 * icons, and the OtherTab simulation / contract / finance toggles).</p>
 *
 * <p>The options are dialog-lifetime state: they are populated from the Command Designer tabs on
 * each run and are not persisted.</p>
 */
public class CommandGenerationOptions {

    /** A year of running costs: enough to reach and see out a first contract before its pay arrives. */
    public static final int DEFAULT_STARTING_CASH_MONTHS = 12;

    /**
     * The least starting cash a command is founded with by default, in C-bills, so one whose campaign charges little or
     * nothing can still pay its first month's wages.
     */
    public static final long DEFAULT_MINIMUM_STARTING_CASH = 10_000_000L;

    /**
     * Support roles seeded with default coverage percentages and skill levels: the four tech roles,
     * doctor, and administrators - matches the People page's support staff table.
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
    // Medical reserve (generation-only): optionally create spare unassigned combat personnel as injury
    // replacements, sized as a percentage of the people generated in each combat role.
    private boolean generateMedicalReserve;
    private int medicalReservePercent;

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
    private boolean assignMekWarriorsCallSigns;
    private boolean assignFounderFlag;

    // Augmentation rules. These mirror settings that live on the campaign and on MegaMek's game
    // options rather than belonging to a generation run, and are carried here so the Command
    // Generator can both read them and write the player's choice back - a new campaign has them all
    // off, and a player who has never opened either options dialog would otherwise have no way to
    // generate an augmented command.
    private boolean useImplants;
    private NeuralInterfaceMode neuralInterfaceMode;
    private boolean applyAugmentationRules = true;

    // Random origin for the people this run generates. Applied to the campaign only for the length of the build;
    // the campaign's own Random Origin options are restored afterwards.
    private RandomOriginOptions randomOriginOptions;

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

    // Finances. Starting cash is working capital sized as a number of months of the command's running costs, never
    // less than the minimum. The optional starting loan is a loan equal to the starting cash, doubling it.
    private int startingCashMonths;
    private long minimumStartingCash;
    private boolean startingLoan;
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
        // into full coverage changes or a fixed skill level on the People page.
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
        setGenerateMedicalReserve(true);
        setMedicalReservePercent(10);

        // Officer assignment and personnel flags
        setGenerateCaptains(true);
        setAssignCompanyCommanderFlag(true);
        setApplyOfficerStatBonusToWorstSkill(false);
        setAssignBestCompanyCommander(true);
        setPrioritizeCompanyCommanderCombatSkills(false);
        setAssignBestOfficers(true);
        setPrioritizeOfficerCombatSkills(false);
        setAssignMostSkilledToPrimaryLances(true);
        setAutomaticallyAssignRanks(true);
        setAssignMekWarriorsCallSigns(true);
        setAssignFounderFlag(true);

        // Left off, matching a fresh campaign. The dialog replaces these with whatever the campaign
        // currently has, so the defaults here only matter to a caller that never opens it.
        setUseImplants(false);
        setNeuralInterfaceMode(NeuralInterfaceMode.OFF);

        // Random origin: everyone generated, dependents included, gets a randomized origin with the extra
        // random planetary origin.
        RandomOriginOptions origin = new RandomOriginOptions(true);
        origin.setRandomizeOrigin(true);
        origin.setRandomizeDependentOrigin(true);
        origin.setRandomizeAroundSpecifiedPlanet(false);
        origin.setOriginSearchRadius(45);
        origin.setOriginDistanceScale(0.6);
        origin.setAllowClanOrigins(false);
        origin.setExtraRandomOrigin(true);
        setRandomOriginOptions(origin);

        // Force naming and formation icons
        setForceNamingMethod(ForceNamingMethod.ICAO_1956);
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
        setSimulateRandomMarriages(true);
        setSimulateRandomProcreation(true);

        // Contracts
        setSelectStartingContract(true);
        setStartCourseToContractPlanet(true);

        // Finances. The command is granted free, with its working capital and no debt.
        setStartingCashMonths(DEFAULT_STARTING_CASH_MONTHS);
        setMinimumStartingCash(DEFAULT_MINIMUM_STARTING_CASH);
        setStartingLoan(false);
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
     * @return which of MegaMek's neural interface rules is in play, which decides whether an enhanced
     *       imaging or direct neural implant does anything
     */
    public NeuralInterfaceMode getNeuralInterfaceMode() {
        return neuralInterfaceMode;
    }

    public void setNeuralInterfaceMode(final NeuralInterfaceMode neuralInterfaceMode) {
        this.neuralInterfaceMode = neuralInterfaceMode;
    }

    public boolean shouldApplyAugmentationRules() {
        return applyAugmentationRules;
    }

    public void setApplyAugmentationRules(final boolean applyAugmentationRules) {
        this.applyAugmentationRules = applyAugmentationRules;
    }

    /**
     * @return the Random Origin options the people generated by this run are given, in place of the campaign's own
     *       for the length of the build
     */
    public RandomOriginOptions getRandomOriginOptions() {
        return randomOriginOptions;
    }

    public void setRandomOriginOptions(final RandomOriginOptions randomOriginOptions) {
        this.randomOriginOptions = randomOriginOptions;
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

    /**
     * Starting cash as a number of months of the built command's running costs: salaries, maintenance,
     * overhead, food and housing, and Hot Spots upkeep, each counted only while the campaign charges it.
     * {@code 12} grants working capital to run the command for a year without income.
     *
     * @return the number of months of running costs granted as starting cash
     */
    public int getStartingCashMonths() {
        return startingCashMonths;
    }

    public void setStartingCashMonths(final int startingCashMonths) {
        this.startingCashMonths = startingCashMonths;
    }

    /**
     * @return the least starting cash the command is founded with, in C-bills, whatever its running costs come to
     *
     * @author Illiani
     * @since 0.51.01
     */
    public long getMinimumStartingCash() {
        return minimumStartingCash;
    }

    /**
     * @param minimumStartingCash the least starting cash the command is founded with, in C-bills
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setMinimumStartingCash(final long minimumStartingCash) {
        this.minimumStartingCash = minimumStartingCash;
    }

    /**
     * @return {@code true} if the command is founded owing a loan equal to the starting cash, doubling it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isStartingLoan() {
        return startingLoan;
    }

    /**
     * @param startingLoan {@code true} to found the command owing a loan equal to the starting cash, doubling it
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setStartingLoan(final boolean startingLoan) {
        this.startingLoan = startingLoan;
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
              && (generateCaptains == other.generateCaptains)
              && (assignCompanyCommanderFlag == other.assignCompanyCommanderFlag)
              && (applyOfficerStatBonusToWorstSkill == other.applyOfficerStatBonusToWorstSkill)
              && (assignBestCompanyCommander == other.assignBestCompanyCommander)
              && (prioritizeCompanyCommanderCombatSkills == other.prioritizeCompanyCommanderCombatSkills)
              && (assignBestOfficers == other.assignBestOfficers)
              && (prioritizeOfficerCombatSkills == other.prioritizeOfficerCombatSkills)
              && (assignMostSkilledToPrimaryLances == other.assignMostSkilledToPrimaryLances)
              && (automaticallyAssignRanks == other.automaticallyAssignRanks)
              && (assignMekWarriorsCallSigns == other.assignMekWarriorsCallSigns)
              && (assignFounderFlag == other.assignFounderFlag)
              && (useImplants == other.useImplants)
              && Objects.equals(neuralInterfaceMode, other.neuralInterfaceMode)
              && sameRandomOrigin(randomOriginOptions, other.randomOriginOptions)
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
              && (startingCashMonths == other.startingCashMonths)
              && (minimumStartingCash == other.minimumStartingCash)
              && (startingLoan == other.startingLoan);
    }

    /**
     * Compares two sets of Random Origin options by value; {@link RandomOriginOptions} has no {@code equals} of its
     * own, and the specified planet is compared by ID.
     */
    private static boolean sameRandomOrigin(@Nullable RandomOriginOptions first,
          @Nullable RandomOriginOptions second) {
        if ((first == null) || (second == null)) {
            return first == second;
        }
        return (first.isRandomizeOrigin() == second.isRandomizeOrigin())
              && (first.isRandomizeDependentOrigin() == second.isRandomizeDependentOrigin())
              && (first.isRandomizeAroundSpecifiedPlanet() == second.isRandomizeAroundSpecifiedPlanet())
              && Objects.equals(planetId(first), planetId(second))
              && (first.getOriginSearchRadius() == second.getOriginSearchRadius())
              && (Double.compare(first.getOriginDistanceScale(), second.getOriginDistanceScale()) == 0)
              && (first.isAllowClanOrigins() == second.isAllowClanOrigins())
              && (first.isExtraRandomOrigin() == second.isExtraRandomOrigin());
    }

    private static int randomOriginHash(@Nullable RandomOriginOptions origin) {
        if (origin == null) {
            return 0;
        }
        return Objects.hash(origin.isRandomizeOrigin(), origin.isRandomizeDependentOrigin(),
              origin.isRandomizeAroundSpecifiedPlanet(), planetId(origin), origin.getOriginSearchRadius(),
              origin.getOriginDistanceScale(), origin.isAllowClanOrigins(), origin.isExtraRandomOrigin());
    }

    private static @Nullable String planetId(RandomOriginOptions origin) {
        return (origin.getSpecifiedPlanet() == null) ? null : origin.getSpecifiedPlanet().getId();
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
              generateCaptains,
              assignCompanyCommanderFlag,
              applyOfficerStatBonusToWorstSkill,
              assignBestCompanyCommander,
              prioritizeCompanyCommanderCombatSkills,
              assignBestOfficers,
              prioritizeOfficerCombatSkills,
              assignMostSkilledToPrimaryLances,
              automaticallyAssignRanks,
              assignMekWarriorsCallSigns,
              assignFounderFlag,
              useImplants,
              neuralInterfaceMode,
              randomOriginHash(randomOriginOptions),
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
              startingCashMonths,
              minimumStartingCash,
              startingLoan);
    }
}
