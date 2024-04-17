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
package mekhq.campaign.universe.companyGeneration;

import megamek.Version;
import megamek.common.EntityWeightClass;
import megamek.common.annotations.Nullable;
import mekhq.MHQConstants;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.enums.*;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Justin "Windchild" Bowen
 */
public class CompanyGenerationOptions {
    //region Variable Declarations
    // Base Information
    private CompanyGenerationMethod method;
    private Faction specifiedFaction;
    private boolean generateMercenaryCompanyCommandLance;
    private int companyCount;
    private int individualLanceCount;
    private int lancesPerCompany;
    private int lanceSize;
    private int starLeagueYear;

    // Personnel
    private Map<PersonnelRole, Integer> supportPersonnel;
    private boolean poolAssistants;
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
    private boolean assignMechWarriorsCallsigns;
    private boolean assignFounderFlag;

    // Personnel Randomization
    private RandomOriginOptions randomOriginOptions;

    // Starting Simulation
    private boolean runStartingSimulation;
    private int simulationDuration;
    private boolean simulateRandomMarriages;
    private boolean simulateRandomProcreation;

    // Units
    private BattleMechFactionGenerationMethod battleMechFactionGenerationMethod;
    private BattleMechWeightClassGenerationMethod battleMechWeightClassGenerationMethod;
    private BattleMechQualityGenerationMethod battleMechQualityGenerationMethod;
    private boolean neverGenerateStarLeagueMechs;
    private boolean onlyGenerateStarLeagueMechs;
    private boolean onlyGenerateOmniMechs;
    private boolean generateUnitsAsAttached;
    private boolean assignBestRollToCompanyCommander;
    private boolean sortStarLeagueUnitsFirst;
    private boolean groupByWeight;
    private boolean groupByQuality;
    private boolean keepOfficerRollsSeparate;
    private boolean assignTechsToUnits;

    // Unit
    private ForceNamingMethod forceNamingMethod;
    private boolean generateForceIcons;
    private boolean useSpecifiedFactionToGenerateForceIcons;
    private boolean generateOriginNodeForceIcon;
    private boolean useOriginNodeForceIconLogo;
    private TreeMap<Integer, Integer> forceWeightLimits;

    // Spares
    private boolean generateMothballedSpareUnits;
    private int sparesPercentOfActiveUnits;
    private PartGenerationMethod partGenerationMethod;
    private int startingArmourWeight;
    private boolean generateSpareAmmunition;
    private int numberReloadsPerWeapon;
    private boolean generateFractionalMachineGunAmmunition;

    // Contracts
    private boolean selectStartingContract;
    private boolean startCourseToContractPlanet;

    // Finances
    private boolean processFinances;
    private int startingCash;
    private boolean randomizeStartingCash;
    private int randomStartingCashDiceCount;
    private int minimumStartingFloat;
    private boolean includeInitialContractPayment;
    private boolean startingLoan;
    private boolean payForSetup;
    private boolean payForPersonnel;
    private boolean payForUnits;
    private boolean payForParts;
    private boolean payForArmour;
    private boolean payForAmmunition;

    // Surprises
    private boolean generateSurprises;
    private boolean generateMysteryBoxes;
    private Map<MysteryBoxType, Boolean> generateMysteryBoxTypes;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationOptions(final CompanyGenerationMethod method) {
        // Base Information
        setMethod(method);
        setSpecifiedFaction(Factions.getInstance().getDefaultFaction());
        setGenerateMercenaryCompanyCommandLance(false);
        setCompanyCount(1);
        setIndividualLanceCount(0);
        setLancesPerCompany(3);
        setLanceSize(4);
        setStarLeagueYear(2765);

        // Personnel
        final Map<PersonnelRole, Integer> supportPersonnel = new HashMap<>();
        if (method.isWindchild()) {
            supportPersonnel.put(PersonnelRole.MECH_TECH, 7);
            supportPersonnel.put(PersonnelRole.MECHANIC, 0);
            supportPersonnel.put(PersonnelRole.AERO_TECH, 0);
            supportPersonnel.put(PersonnelRole.DOCTOR, 1);
            supportPersonnel.put(PersonnelRole.ADMINISTRATOR_COMMAND, 1);
            supportPersonnel.put(PersonnelRole.ADMINISTRATOR_LOGISTICS, 1);
            supportPersonnel.put(PersonnelRole.ADMINISTRATOR_TRANSPORT, 1);
            supportPersonnel.put(PersonnelRole.ADMINISTRATOR_HR, 1);
        } else { // Defaults to AtB
            supportPersonnel.put(PersonnelRole.MECH_TECH, 10);
            supportPersonnel.put(PersonnelRole.DOCTOR, 1);
            supportPersonnel.put(PersonnelRole.ADMINISTRATOR_LOGISTICS, 1);
        }
        setSupportPersonnel(supportPersonnel);
        setPoolAssistants(true);
        setGenerateCaptains(method.isWindchild());
        setAssignCompanyCommanderFlag(true);
        setApplyOfficerStatBonusToWorstSkill(method.isWindchild());
        setAssignBestCompanyCommander(method.isWindchild());
        setPrioritizeCompanyCommanderCombatSkills(false);
        setAssignBestOfficers(method.isWindchild());
        setPrioritizeOfficerCombatSkills(false);
        setAssignMostSkilledToPrimaryLances(method.isWindchild());
        setAutomaticallyAssignRanks(true);
        setUseSpecifiedFactionToAssignRanks(false);
        setAssignMechWarriorsCallsigns(true);
        setAssignFounderFlag(true);

        // Personnel Randomization
        setRandomOriginOptions(new RandomOriginOptions(false));

        // Starting Simulation
        setRunStartingSimulation(method.isWindchild());
        setSimulationDuration(5);
        setSimulateRandomMarriages(method.isWindchild());
        setSimulateRandomProcreation(method.isWindchild());

        // Units
        setBattleMechFactionGenerationMethod(BattleMechFactionGenerationMethod.ORIGIN_FACTION);
        setBattleMechWeightClassGenerationMethod(method.isAgainstTheBot()
                ? BattleMechWeightClassGenerationMethod.AGAINST_THE_BOT
                : BattleMechWeightClassGenerationMethod.WINDCHILD);
        setBattleMechQualityGenerationMethod(method.isAgainstTheBot()
                ? BattleMechQualityGenerationMethod.AGAINST_THE_BOT
                : BattleMechQualityGenerationMethod.WINDCHILD);
        setNeverGenerateStarLeagueMechs(false);
        setOnlyGenerateStarLeagueMechs(false);
        setOnlyGenerateOmniMechs(false);
        setGenerateUnitsAsAttached(method.isAgainstTheBot());
        setAssignBestRollToCompanyCommander(method.isWindchild());
        setSortStarLeagueUnitsFirst(true);
        setGroupByWeight(true);
        setGroupByQuality(method.isWindchild());
        setKeepOfficerRollsSeparate(method.isAgainstTheBot());
        setAssignTechsToUnits(true);

        // Unit
        setForceNamingMethod(ForceNamingMethod.CCB_1943);
        setGenerateForceIcons(true);
        setUseSpecifiedFactionToGenerateForceIcons(false);
        setGenerateOriginNodeForceIcon(true);
        setUseOriginNodeForceIconLogo(false);
        setForceWeightLimits(new TreeMap<>());
        getForceWeightLimits().put(390, EntityWeightClass.WEIGHT_ASSAULT);
        getForceWeightLimits().put(280, EntityWeightClass.WEIGHT_HEAVY);
        getForceWeightLimits().put(200, EntityWeightClass.WEIGHT_MEDIUM);
        getForceWeightLimits().put(130, EntityWeightClass.WEIGHT_LIGHT);
        getForceWeightLimits().put(60, EntityWeightClass.WEIGHT_ULTRA_LIGHT);

        // Spares
        setGenerateMothballedSpareUnits(false);
        setSparesPercentOfActiveUnits(10);
        setPartGenerationMethod(PartGenerationMethod.WINDCHILD);
        setStartingArmourWeight(60);
        setGenerateSpareAmmunition(method.isWindchild());
        setNumberReloadsPerWeapon(4);
        setGenerateFractionalMachineGunAmmunition(true);

        // Contracts
        setSelectStartingContract(true);
        setStartCourseToContractPlanet(true);

        // Finances
        setProcessFinances(true);
        setStartingCash(60000000);
        setRandomizeStartingCash(method.isWindchild());
        setRandomStartingCashDiceCount(18);
        setMinimumStartingFloat(method.isWindchild() ? 3500000 : 0);
        setIncludeInitialContractPayment(method.isWindchild());
        setStartingLoan(!method.isWindchild());
        setPayForSetup(true);
        setPayForPersonnel(true);
        setPayForUnits(true);
        setPayForParts(true);
        setPayForArmour(true);
        setPayForAmmunition(true);

        // Surprises
        setGenerateSurprises(true);
        setGenerateMysteryBoxes(true);
        setGenerateMysteryBoxTypes(new HashMap<>());
        getGenerateMysteryBoxTypes().put(MysteryBoxType.STAR_LEAGUE_REGULAR, true);
    }
    //endregion Constructors

    //region Getters/Setters
    //region Base Information
    public CompanyGenerationMethod getMethod() {
        return method;
    }

    public void setMethod(final CompanyGenerationMethod method) {
        this.method = method;
    }

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

    public int getCompanyCount() {
        return companyCount;
    }

    public void setCompanyCount(final int companyCount) {
        this.companyCount = companyCount;
    }

    public int getIndividualLanceCount() {
        return individualLanceCount;
    }

    public void setIndividualLanceCount(final int individualLanceCount) {
        this.individualLanceCount = individualLanceCount;
    }

    public int getLancesPerCompany() {
        return lancesPerCompany;
    }

    public void setLancesPerCompany(final int lancesPerCompany) {
        this.lancesPerCompany = lancesPerCompany;
    }

    public int getLanceSize() {
        return lanceSize;
    }

    public void setLanceSize(final int lanceSize) {
        this.lanceSize = lanceSize;
    }

    public int getStarLeagueYear() {
        return starLeagueYear;
    }

    public void setStarLeagueYear(final int starLeagueYear) {
        this.starLeagueYear = starLeagueYear;
    }
    //endregion Base Information

    //region Personnel
    public Map<PersonnelRole, Integer> getSupportPersonnel() {
        return supportPersonnel;
    }

    public void setSupportPersonnel(final Map<PersonnelRole, Integer> supportPersonnel) {
        this.supportPersonnel = supportPersonnel;
    }

    public boolean isPoolAssistants() {
        return poolAssistants;
    }

    public void setPoolAssistants(final boolean poolAssistants) {
        this.poolAssistants = poolAssistants;
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

    public boolean isAssignMechWarriorsCallsigns() {
        return assignMechWarriorsCallsigns;
    }

    public void setAssignMechWarriorsCallsigns(final boolean assignMechWarriorsCallsigns) {
        this.assignMechWarriorsCallsigns = assignMechWarriorsCallsigns;
    }

    public boolean isAssignFounderFlag() {
        return assignFounderFlag;
    }

    public void setAssignFounderFlag(final boolean assignFounderFlag) {
        this.assignFounderFlag = assignFounderFlag;
    }
    //endregion Personnel

    //region Personnel Randomization
    public RandomOriginOptions getRandomOriginOptions() {
        return randomOriginOptions;
    }

    public void setRandomOriginOptions(final RandomOriginOptions randomOriginOptions) {
        this.randomOriginOptions = randomOriginOptions;
    }
    //endregion Personnel Randomization

    //region Starting Simulation
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
    //endregion Starting Simulation

    //region Units
    public BattleMechFactionGenerationMethod getBattleMechFactionGenerationMethod() {
        return battleMechFactionGenerationMethod;
    }

    public void setBattleMechFactionGenerationMethod(
            final BattleMechFactionGenerationMethod battleMechFactionGenerationMethod) {
        this.battleMechFactionGenerationMethod = battleMechFactionGenerationMethod;
    }

    public BattleMechWeightClassGenerationMethod getBattleMechWeightClassGenerationMethod() {
        return battleMechWeightClassGenerationMethod;
    }

    public void setBattleMechWeightClassGenerationMethod(
            final BattleMechWeightClassGenerationMethod battleMechWeightClassGenerationMethod) {
        this.battleMechWeightClassGenerationMethod = battleMechWeightClassGenerationMethod;
    }

    public BattleMechQualityGenerationMethod getBattleMechQualityGenerationMethod() {
        return battleMechQualityGenerationMethod;
    }

    public void setBattleMechQualityGenerationMethod(
            final BattleMechQualityGenerationMethod battleMechQualityGenerationMethod) {
        this.battleMechQualityGenerationMethod = battleMechQualityGenerationMethod;
    }

    public boolean isNeverGenerateStarLeagueMechs() {
        return neverGenerateStarLeagueMechs;
    }

    public void setNeverGenerateStarLeagueMechs(final boolean neverGenerateStarLeagueMechs) {
        this.neverGenerateStarLeagueMechs = neverGenerateStarLeagueMechs;
    }

    public boolean isOnlyGenerateStarLeagueMechs() {
        return onlyGenerateStarLeagueMechs;
    }

    public void setOnlyGenerateStarLeagueMechs(final boolean onlyGenerateStarLeagueMechs) {
        this.onlyGenerateStarLeagueMechs = onlyGenerateStarLeagueMechs;
    }

    public boolean isOnlyGenerateOmniMechs() {
        return onlyGenerateOmniMechs;
    }

    public void setOnlyGenerateOmniMechs(final boolean onlyGenerateOmniMechs) {
        this.onlyGenerateOmniMechs = onlyGenerateOmniMechs;
    }

    public boolean isGenerateUnitsAsAttached() {
        return generateUnitsAsAttached;
    }

    public void setGenerateUnitsAsAttached(final boolean generateUnitsAsAttached) {
        this.generateUnitsAsAttached = generateUnitsAsAttached;
    }

    public boolean isAssignBestRollToCompanyCommander() {
        return assignBestRollToCompanyCommander;
    }

    public void setAssignBestRollToCompanyCommander(final boolean assignBestRollToCompanyCommander) {
        this.assignBestRollToCompanyCommander = assignBestRollToCompanyCommander;
    }

    public boolean isSortStarLeagueUnitsFirst() {
        return sortStarLeagueUnitsFirst;
    }

    public void setSortStarLeagueUnitsFirst(final boolean sortStarLeagueUnitsFirst) {
        this.sortStarLeagueUnitsFirst = sortStarLeagueUnitsFirst;
    }

    public boolean isGroupByWeight() {
        return groupByWeight;
    }

    public void setGroupByWeight(final boolean groupByWeight) {
        this.groupByWeight = groupByWeight;
    }

    public boolean isGroupByQuality() {
        return groupByQuality;
    }

    public void setGroupByQuality(final boolean groupByQuality) {
        this.groupByQuality = groupByQuality;
    }

    public boolean isKeepOfficerRollsSeparate() {
        return keepOfficerRollsSeparate;
    }

    public void setKeepOfficerRollsSeparate(final boolean keepOfficerRollsSeparate) {
        this.keepOfficerRollsSeparate = keepOfficerRollsSeparate;
    }

    public boolean isAssignTechsToUnits() {
        return assignTechsToUnits;
    }

    public void setAssignTechsToUnits(final boolean assignTechsToUnits) {
        this.assignTechsToUnits = assignTechsToUnits;
    }
    //endregion Units

    //region Unit
    public ForceNamingMethod getForceNamingMethod() {
        return forceNamingMethod;
    }

    public void setForceNamingMethod(final ForceNamingMethod forceNamingMethod) {
        this.forceNamingMethod = forceNamingMethod;
    }

    public boolean isGenerateForceIcons() {
        return generateForceIcons;
    }

    public void setGenerateForceIcons(final boolean generateForceIcons) {
        this.generateForceIcons = generateForceIcons;
    }

    public boolean isUseSpecifiedFactionToGenerateForceIcons() {
        return useSpecifiedFactionToGenerateForceIcons;
    }

    public void setUseSpecifiedFactionToGenerateForceIcons(final boolean useSpecifiedFactionToGenerateForceIcons) {
        this.useSpecifiedFactionToGenerateForceIcons = useSpecifiedFactionToGenerateForceIcons;
    }

    public boolean isGenerateOriginNodeForceIcon() {
        return generateOriginNodeForceIcon;
    }

    public void setGenerateOriginNodeForceIcon(final boolean generateOriginNodeForceIcon) {
        this.generateOriginNodeForceIcon = generateOriginNodeForceIcon;
    }

    public boolean isUseOriginNodeForceIconLogo() {
        return useOriginNodeForceIconLogo;
    }

    public void setUseOriginNodeForceIconLogo(final boolean useOriginNodeForceIconLogo) {
        this.useOriginNodeForceIconLogo = useOriginNodeForceIconLogo;
    }

    public TreeMap<Integer, Integer> getForceWeightLimits() {
        return forceWeightLimits;
    }

    public void setForceWeightLimits(final TreeMap<Integer, Integer> forceWeightLimits) {
        this.forceWeightLimits = forceWeightLimits;
    }
    //endregion Unit

    //region Spares
    public boolean isGenerateMothballedSpareUnits() {
        return generateMothballedSpareUnits;
    }

    public void setGenerateMothballedSpareUnits(final boolean generateMothballedSpareUnits) {
        this.generateMothballedSpareUnits = generateMothballedSpareUnits;
    }

    public int getSparesPercentOfActiveUnits() {
        return sparesPercentOfActiveUnits;
    }

    public void setSparesPercentOfActiveUnits(final int sparesPercentOfActiveUnits) {
        this.sparesPercentOfActiveUnits = sparesPercentOfActiveUnits;
    }

    public PartGenerationMethod getPartGenerationMethod() {
        return partGenerationMethod;
    }

    public void setPartGenerationMethod(final PartGenerationMethod partGenerationMethod) {
        this.partGenerationMethod = partGenerationMethod;
    }

    public int getStartingArmourWeight() {
        return startingArmourWeight;
    }

    public void setStartingArmourWeight(final int startingArmourWeight) {
        this.startingArmourWeight = startingArmourWeight;
    }

    public boolean isGenerateSpareAmmunition() {
        return generateSpareAmmunition;
    }

    public void setGenerateSpareAmmunition(final boolean generateSpareAmmunition) {
        this.generateSpareAmmunition = generateSpareAmmunition;
    }

    public int getNumberReloadsPerWeapon() {
        return numberReloadsPerWeapon;
    }

    public void setNumberReloadsPerWeapon(final int numberReloadsPerWeapon) {
        this.numberReloadsPerWeapon = numberReloadsPerWeapon;
    }

    public boolean isGenerateFractionalMachineGunAmmunition() {
        return generateFractionalMachineGunAmmunition;
    }

    public void setGenerateFractionalMachineGunAmmunition(final boolean generateFractionalMachineGunAmmunition) {
        this.generateFractionalMachineGunAmmunition = generateFractionalMachineGunAmmunition;
    }
    //endregion Spares

    //region Contracts
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
    //endregion Contracts

    //region Finances
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

    public boolean isIncludeInitialContractPayment() {
        return includeInitialContractPayment;
    }

    public void setIncludeInitialContractPayment(final boolean includeInitialContractPayment) {
        this.includeInitialContractPayment = includeInitialContractPayment;
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
    //endregion Finances

    //region Surprises
    public boolean isGenerateSurprises() {
        return generateSurprises;
    }

    public void setGenerateSurprises(final boolean generateSurprises) {
        this.generateSurprises = generateSurprises;
    }

    public boolean isGenerateMysteryBoxes() {
        return generateMysteryBoxes;
    }

    public void setGenerateMysteryBoxes(final boolean generateMysteryBoxes) {
        this.generateMysteryBoxes = generateMysteryBoxes;
    }

    public Map<MysteryBoxType, Boolean> getGenerateMysteryBoxTypes() {
        return generateMysteryBoxTypes;
    }

    public void setGenerateMysteryBoxTypes(final Map<MysteryBoxType, Boolean> generateMysteryBoxTypes) {
        this.generateMysteryBoxTypes = generateMysteryBoxTypes;
    }
    //endregion Surprises
    //endregion Getters/Setters

    //region File IO
    /**
     * Writes these options to an XML file
     * @param file the file to write to, or null to not write to a file
     */
    public void writeToFile(@Nullable File file) {
        if (file == null) {
            return;
        }
        String path = file.getPath();
        if (!path.endsWith(".xml")) {
            path += ".xml";
            file = new File(path);
        }

        try (OutputStream fos = new FileOutputStream(file);
             OutputStream bos = new BufferedOutputStream(fos);
             OutputStreamWriter osw = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {
            // Then save it out to that file.
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writeToXML(pw, 0, MHQConstants.VERSION);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    /**
     * @param pw the print writer to write to
     * @param indent the indent level to write at
     * @param version the version these options were written to file in. This may be null, in which
     *                case they are being written to file as a part of a larger save than just these
     *                options (e.g. saved as part of Campaign or CampaignOptions)
     */
    public void writeToXML(final PrintWriter pw, int indent, final @Nullable Version version) {
        if (version == null) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "companyGenerationOptions");
        } else {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "companyGenerationOptions", "version", version);
        }

        // Base Information
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "method", getMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "specifiedFaction", getSpecifiedFaction().getShortName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateMercenaryCompanyCommandLance", isGenerateMercenaryCompanyCommandLance());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "companyCount", getCompanyCount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "individualLanceCount", getIndividualLanceCount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lancesPerCompany", getLancesPerCompany());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lanceSize", getLanceSize());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "starLeagueYear", getStarLeagueYear());

        // Personnel
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "supportPersonnel");
        for (final Entry<PersonnelRole, Integer> entry : getSupportPersonnel().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "supportPersonnel");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "poolAssistants", isPoolAssistants());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateCaptains", isGenerateCaptains());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignCompanyCommanderFlag", isAssignCompanyCommanderFlag());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "applyOfficerStatBonusToWorstSkill", isApplyOfficerStatBonusToWorstSkill());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignBestCompanyCommander", isAssignBestCompanyCommander());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prioritizeCompanyCommanderCombatSkills", isPrioritizeCompanyCommanderCombatSkills());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignBestOfficers", isAssignBestOfficers());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prioritizeOfficerCombatSkills", isPrioritizeOfficerCombatSkills());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignMostSkilledToPrimaryLances", isAssignMostSkilledToPrimaryLances());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "automaticallyAssignRanks", isAutomaticallyAssignRanks());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSpecifiedFactionToAssignRanks", isUseSpecifiedFactionToAssignRanks());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignMechWarriorsCallsigns", isAssignMechWarriorsCallsigns());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignFounderFlag", isAssignFounderFlag());

        // Personnel Randomization
        getRandomOriginOptions().writeToXML(pw, indent);

        // Starting Simulation
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "runStartingSimulation", isRunStartingSimulation());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "simulationDuration", getSimulationDuration());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "simulateRandomMarriages", isSimulateRandomMarriages());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "simulateRandomProcreation", isSimulateRandomProcreation());

        // Units
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "battleMechFactionGenerationMethod", getBattleMechFactionGenerationMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "battleMechWeightClassGenerationMethod", getBattleMechWeightClassGenerationMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "battleMechQualityGenerationMethod", getBattleMechQualityGenerationMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "neverGenerateStarLeagueMechs", isNeverGenerateStarLeagueMechs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "onlyGenerateStarLeagueMechs", isOnlyGenerateStarLeagueMechs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "onlyGenerateOmniMechs", isOnlyGenerateOmniMechs());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateUnitsAsAttached", isGenerateUnitsAsAttached());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignBestRollToCompanyCommander", isAssignBestRollToCompanyCommander());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sortStarLeagueUnitsFirst", isSortStarLeagueUnitsFirst());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "groupByWeight", isGroupByWeight());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "groupByQuality", isGroupByQuality());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "keepOfficerRollsSeparate", isKeepOfficerRollsSeparate());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "assignTechsToUnits", isAssignTechsToUnits());

        // Unit
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceNamingMethod", getForceNamingMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateForceIcons", isGenerateForceIcons());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useSpecifiedFactionToGenerateForceIcons", isUseSpecifiedFactionToGenerateForceIcons());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateOriginNodeForceIcon", isGenerateOriginNodeForceIcon());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "useOriginNodeForceIconLogo", isUseOriginNodeForceIconLogo());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "forceWeightLimits");
        for (final Entry<Integer, Integer> entry : getForceWeightLimits().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "WeightClass:" + entry.getValue(), entry.getKey().toString());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "forceWeightLimits");

        // Spares
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateMothballedSpareUnits", isGenerateMothballedSpareUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sparesPercentOfActiveUnits", getSparesPercentOfActiveUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "partGenerationMethod", getPartGenerationMethod().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingArmourWeight", getStartingArmourWeight());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateSpareAmmunition", isGenerateSpareAmmunition());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "numberReloadsPerWeapon", getNumberReloadsPerWeapon());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateFractionalMachineGunAmmunition", isGenerateFractionalMachineGunAmmunition());

        // Contracts
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "selectStartingContract", isSelectStartingContract());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startCourseToContractPlanet", isStartCourseToContractPlanet());

        // Finances
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "processFinances", isProcessFinances());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingCash", getStartingCash());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomizeStartingCash", isRandomizeStartingCash());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "randomStartingCashDiceCount", getRandomStartingCashDiceCount());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "minimumStartingFloat", getMinimumStartingFloat());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "includeInitialContractPayment", isIncludeInitialContractPayment());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startingLoan", isStartingLoan());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForSetup", isPayForSetup());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForPersonnel", isPayForPersonnel());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForUnits", isPayForUnits());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForParts", isPayForParts());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForArmour", isPayForArmour());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payForAmmunition", isPayForAmmunition());

        // Surprises
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateSurprises", isGenerateSurprises());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "generateMysteryBoxes", isGenerateMysteryBoxes());
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "generateMysteryBoxTypes");
        for (final Entry<MysteryBoxType, Boolean> entry : getGenerateMysteryBoxTypes().entrySet()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, entry.getKey().name(), entry.getValue());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "generateMysteryBoxTypes");
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "companyGenerationOptions");
    }

    /**
     * @param file the XML file to parse the company generation options from. This should not be null,
     *             but null values are handled nicely.
     * @return the parsed CompanyGenerationOptions, or the default Windchild options if there is an
     * issue parsing the file.
     */
    public static CompanyGenerationOptions parseFromXML(final @Nullable File file) {
        if (file == null) {
            LogManager.getLogger().error("Received a null file, returning the default Windchild options");
            return new CompanyGenerationOptions(CompanyGenerationMethod.WINDCHILD);
        }
        final Element element;

        // Open up the file.
        try (InputStream is = new FileInputStream(file)) {
            element = MHQXMLUtility.newSafeDocumentBuilder().parse(is).getDocumentElement();
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to open file, returning the default Windchild options", ex);
            return new CompanyGenerationOptions(CompanyGenerationMethod.WINDCHILD);
        }
        element.normalize();

        final Version version = new Version(element.getAttribute("version"));
        final CompanyGenerationOptions options = parseFromXML(element.getChildNodes(), version);
        if (options == null) {
            LogManager.getLogger().error("Failed to parse file, returning the default Windchild options");
            return new CompanyGenerationOptions(CompanyGenerationMethod.WINDCHILD);
        } else {
            return options;
        }
    }

    /**
     * @param nl the node list to parse the options from
     * @param version the Version of the XML to parse from
     * @return the parsed company generation options, or null if the parsing fails
     */
    public static @Nullable CompanyGenerationOptions parseFromXML(final NodeList nl,
                                                                  final Version version) {
        if (MHQConstants.VERSION.isLowerThan(version)) {
            LogManager.getLogger().error(String.format(
                    "Cannot parse Company Generation Options from %s in older version %s.",
                    version.toString(), MHQConstants.VERSION));
            return null;
        }

        final CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.WINDCHILD);
        try {
            for (int x = 0; x < nl.getLength(); x++) {
                final Node wn = nl.item(x);
                switch (wn.getNodeName()) {
                    //region Base Information
                    case "method":
                        options.setMethod(CompanyGenerationMethod.valueOf(wn.getTextContent().trim()));
                        break;
                    case "specifiedFaction":
                        final String factionCode = wn.getTextContent().trim();
                        final Faction faction = Factions.getInstance().getFaction(factionCode);
                        Objects.requireNonNull(faction, "Cannot parse unknown faction with code " + factionCode);
                        options.setSpecifiedFaction(faction);
                        break;
                    case "generateMercenaryCompanyCommandLance":
                        options.setGenerateMercenaryCompanyCommandLance(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "companyCount":
                        options.setCompanyCount(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "individualLanceCount":
                        options.setIndividualLanceCount(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "lancesPerCompany":
                        options.setLancesPerCompany(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "lanceSize":
                        options.setLanceSize(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "starLeagueYear":
                        options.setStarLeagueYear(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    //endregion Base Information

                    //region Personnel
                    case "supportPersonnel": {
                        options.setSupportPersonnel(new HashMap<>());
                        final NodeList nl2 = wn.getChildNodes();
                        for (int y = 0; y < nl2.getLength(); y++) {
                            final Node wn2 = nl2.item(y);
                            try {
                                options.getSupportPersonnel().put(
                                        PersonnelRole.valueOf(wn2.getNodeName().trim()),
                                        Integer.parseInt(wn2.getTextContent().trim()));
                            } catch (Exception ignored) {

                            }
                        }
                        break;
                    }
                    case "poolAssistants":
                        options.setPoolAssistants(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "generateCaptains":
                        options.setGenerateCaptains(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignCompanyCommanderFlag":
                        options.setAssignCompanyCommanderFlag(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "applyOfficerStatBonusToWorstSkill":
                        options.setApplyOfficerStatBonusToWorstSkill(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignBestCompanyCommander":
                        options.setAssignBestCompanyCommander(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "prioritizeCompanyCommanderCombatSkills":
                        options.setPrioritizeCompanyCommanderCombatSkills(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignBestOfficers":
                        options.setAssignBestOfficers(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "prioritizeOfficerCombatSkills":
                        options.setPrioritizeOfficerCombatSkills(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignMostSkilledToPrimaryLances":
                        options.setAssignMostSkilledToPrimaryLances(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "automaticallyAssignRanks":
                        options.setAutomaticallyAssignRanks(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "useSpecifiedFactionToAssignRanks":
                        options.setUseSpecifiedFactionToAssignRanks(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignMechWarriorsCallsigns":
                        options.setAssignMechWarriorsCallsigns(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignFounderFlag":
                        options.setAssignFounderFlag(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    //endregion Personnel

                    //region Personnel Randomization
                    case "randomOriginOptions":
                        if (!wn.hasChildNodes()) {
                            continue;
                        }
                        final RandomOriginOptions randomOriginOptions = RandomOriginOptions.parseFromXML(wn.getChildNodes(), false);
                        if (randomOriginOptions != null) {
                            options.setRandomOriginOptions(randomOriginOptions);
                        }
                        break;
                    //endregion Personnel Randomization

                    //region Starting Simulation
                    case "runStartingSimulation":
                        options.setRunStartingSimulation(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "simulationDuration":
                        options.setSimulationDuration(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "simulateRandomMarriages":
                        options.setSimulateRandomMarriages(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "simulateRandomProcreation":
                        options.setSimulateRandomProcreation(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    //endregion Starting Simulation

                    //region Units
                    case "battleMechFactionGenerationMethod":
                        options.setBattleMechFactionGenerationMethod(BattleMechFactionGenerationMethod.valueOf(wn.getTextContent().trim()));
                        break;
                    case "battleMechWeightClassGenerationMethod":
                        options.setBattleMechWeightClassGenerationMethod(BattleMechWeightClassGenerationMethod.valueOf(wn.getTextContent().trim()));
                        break;
                    case "battleMechQualityGenerationMethod":
                        options.setBattleMechQualityGenerationMethod(BattleMechQualityGenerationMethod.valueOf(wn.getTextContent().trim()));
                        break;
                    case "neverGenerateStarLeagueMechs":
                        options.setNeverGenerateStarLeagueMechs(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "onlyGenerateStarLeagueMechs":
                        options.setOnlyGenerateStarLeagueMechs(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "onlyGenerateOmniMechs":
                        options.setOnlyGenerateOmniMechs(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "generateUnitsAsAttached":
                        options.setGenerateUnitsAsAttached(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignBestRollToCompanyCommander":
                        options.setAssignBestRollToCompanyCommander(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "sortStarLeagueUnitsFirst":
                        options.setSortStarLeagueUnitsFirst(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "groupByWeight":
                        options.setGroupByWeight(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "groupByQuality":
                        options.setGroupByQuality(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "keepOfficerRollsSeparate":
                        options.setKeepOfficerRollsSeparate(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "assignTechsToUnits":
                        options.setAssignTechsToUnits(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    //endregion Units

                    //region Unit
                    case "forceNamingMethod":
                        options.setForceNamingMethod(ForceNamingMethod.valueOf(wn.getTextContent().trim()));
                        break;
                    case "generateForceIcons":
                        options.setGenerateForceIcons(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "useSpecifiedFactionToGenerateForceIcons":
                        options.setUseSpecifiedFactionToGenerateForceIcons(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "generateOriginNodeForceIcon":
                        options.setGenerateOriginNodeForceIcon(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "useOriginNodeForceIconLogo":
                        options.setUseOriginNodeForceIconLogo(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "forceWeightLimits": {
                        options.setForceWeightLimits(new TreeMap<>());
                        final NodeList nl2 = wn.getChildNodes();
                        for (int y = 0; y < nl2.getLength(); y++) {
                            final Node wn2 = nl2.item(y);
                            try {
                                options.getForceWeightLimits().put(
                                        Integer.parseInt(wn2.getTextContent().trim()),
                                        Integer.parseInt(wn2.getNodeName().trim().split(":")[1]));
                            } catch (Exception ignored) {

                            }
                        }
                        break;
                    }
                    //endregion Units

                    //region Spares
                    case "generateMothballedSpareUnits":
                        options.setGenerateMothballedSpareUnits(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "sparesPercentOfActiveUnits":
                        options.setSparesPercentOfActiveUnits(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "partGenerationMethod":
                        options.setPartGenerationMethod(PartGenerationMethod.valueOf(wn.getTextContent().trim()));
                        break;
                    case "startingArmourWeight":
                        options.setStartingArmourWeight(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "generateSpareAmmunition":
                        options.setGenerateSpareAmmunition(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "numberReloadsPerWeapon":
                        options.setNumberReloadsPerWeapon(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "generateFractionalMachineGunAmmunition":
                        options.setGenerateFractionalMachineGunAmmunition(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    //endregion Spares

                    //region Contracts
                    case "selectStartingContract":
                        options.setSelectStartingContract(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "startCourseToContractPlanet":
                        options.setStartCourseToContractPlanet(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    //endregion Contracts

                    //region Finances
                    case "processFinances":
                        options.setProcessFinances(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "startingCash":
                        options.setStartingCash(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "randomizeStartingCash":
                        options.setRandomizeStartingCash(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "randomStartingCashDiceCount":
                        options.setRandomStartingCashDiceCount(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "minimumStartingFloat":
                        options.setMinimumStartingFloat(Integer.parseInt(wn.getTextContent().trim()));
                        break;
                    case "includeInitialContractPayment":
                        options.setIncludeInitialContractPayment(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "startingLoan":
                        options.setStartingLoan(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "payForSetup":
                        options.setPayForSetup(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "payForPersonnel":
                        options.setPayForPersonnel(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "payForUnits":
                        options.setPayForUnits(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "payForParts":
                        options.setPayForParts(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "payForArmour":
                        options.setPayForArmour(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "payForAmmunition":
                        options.setPayForAmmunition(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    //endregion Finances

                    //region Surprises
                    case "generateSurprises":
                        options.setGenerateSurprises(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "generateMysteryBoxes":
                        options.setGenerateMysteryBoxes(Boolean.parseBoolean(wn.getTextContent().trim()));
                        break;
                    case "generateMysteryBoxTypes": {
                        options.setGenerateMysteryBoxTypes(new HashMap<>());
                        final NodeList nl2 = wn.getChildNodes();
                        for (int y = 0; y < nl2.getLength(); y++) {
                            final Node wn2 = nl2.item(y);
                            try {
                                options.getGenerateMysteryBoxTypes().put(
                                        MysteryBoxType.valueOf(wn2.getNodeName().trim()),
                                        Boolean.parseBoolean(wn2.getTextContent().trim()));
                            } catch (Exception ignored) {

                            }
                        }
                        break;
                    }
                    //endregion Surprises

                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return null;
        }

        return options;
    }
    //endregion File IO
}
