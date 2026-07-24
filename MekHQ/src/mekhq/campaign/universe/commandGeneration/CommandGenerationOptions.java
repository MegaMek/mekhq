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
     * doctor, and the four administrator roles - matches the SetupTab spinner/dropdown layout.
     */
    private static final PersonnelRole[] SUPPORT_ROLES_FOR_COVERAGE = {
          PersonnelRole.MEK_TECH,
          PersonnelRole.MECHANIC,
          PersonnelRole.AERO_TEK,
          PersonnelRole.BA_TECH,
          PersonnelRole.DOCTOR,
          PersonnelRole.ADMINISTRATOR_COMMAND,
          PersonnelRole.ADMINISTRATOR_LOGISTICS,
          PersonnelRole.ADMINISTRATOR_TRANSPORT,
          PersonnelRole.ADMINISTRATOR_HR
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
    // 100 = full canonical coverage, 0 = generate none, >100 = redundancy.
    private Map<PersonnelRole, Integer> supportPersonnelCoveragePercents;
    private Map<PersonnelRole, SkillLevel> supportPersonnelSkillLevels;
    // Astech / Medic generation - each auxiliary type can independently be skipped, pooled, or
    // generated as named Persons.
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

    // Force naming and formation icons
    private ForceNamingMethod forceNamingMethod;
    private boolean generateFormationIcons;
    private boolean useSpecifiedFactionToGenerateFormationIcons;
    private boolean generateOriginNodeFormationIcon;
    private boolean useOriginNodeFormationIconLogo;

    // Starting simulation
    private boolean runStartingSimulation;
    private int simulationDuration;
    private boolean simulateRandomMarriages;
    private boolean simulateRandomProcreation;

    // Contracts
    private boolean selectStartingContract;
    private boolean startCourseToContractPlanet;

    // Finances
    private boolean processFinances;
    private int startingCash;
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

        // Support personnel: full canonical coverage at Regular skill for every support role - the
        // user opts into changes via the SetupTab.
        final Map<PersonnelRole, Integer> coveragePercents = new HashMap<>();
        final Map<PersonnelRole, SkillLevel> skillLevels = new HashMap<>();
        for (final PersonnelRole role : SUPPORT_ROLES_FOR_COVERAGE) {
            coveragePercents.put(role, 100);
            skillLevels.put(role, SkillLevel.REGULAR);
        }
        setSupportPersonnelCoveragePercents(coveragePercents);
        setSupportPersonnelSkillLevels(skillLevels);

        // Astech / Medic generation defaults: generated as pools out-of-box; opting into
        // named-Persons mode is explicit.
        setGenerateAstechs(true);
        setAstechsAsPersonnel(false);
        setAstechSkillLevel(SkillLevel.REGULAR);
        setGenerateMedics(true);
        setMedicsAsPersonnel(false);
        setMedicSkillLevel(SkillLevel.REGULAR);
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

        // Force naming and formation icons
        setForceNamingMethod(ForceNamingMethod.CCB_1943);
        setGenerateFormationIcons(true);
        setUseSpecifiedFactionToGenerateFormationIcons(false);
        setGenerateOriginNodeFormationIcon(true);
        setUseOriginNodeFormationIconLogo(false);

        // Starting simulation
        setRunStartingSimulation(false);
        setSimulationDuration(5);
        setSimulateRandomMarriages(false);
        setSimulateRandomProcreation(false);

        // Contracts
        setSelectStartingContract(true);
        setStartCourseToContractPlanet(true);

        // Finances
        setProcessFinances(true);
        setStartingCash(60000000);
        setRandomizeStartingCash(false);
        setRandomStartingCashDiceCount(18);
        setMinimumStartingFloat(0);
        setStartingLoan(true);
        setPayForSetup(true);
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

    public ForceNamingMethod getForceNamingMethod() {
        return forceNamingMethod;
    }

    public void setForceNamingMethod(final ForceNamingMethod forceNamingMethod) {
        this.forceNamingMethod = forceNamingMethod;
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

    public int getStartingCash() {
        return startingCash;
    }

    public void setStartingCash(final int startingCash) {
        this.startingCash = startingCash;
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
}
