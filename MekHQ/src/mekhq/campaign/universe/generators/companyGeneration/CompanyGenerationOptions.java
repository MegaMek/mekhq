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

import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.enums.CompanyGenerationType;
import mekhq.campaign.universe.enums.ForceNamingType;

import java.util.HashMap;
import java.util.Map;

public class CompanyGenerationOptions {
    //region Variable Declarations
    // Base Information
    private Faction faction; // Done
    private int companyCount;
    private int individualLanceCount;
    private boolean generateMercenaryCompanyCommandLance;
    private int lancesPerCompany;
    private int lanceSize;

    // Personnel
    private Map<Integer, Integer> supportPersonnel; // Done
    private boolean poolAssistants; // Done
    private boolean generateCompanyCommanders; // Done
    /**
     * If the company commander counts as one of the lance officers (reduces officer generation by 1 if true)
     */
    private boolean companyCommanderLanceOfficer; // Done
    private boolean applyOfficerStatBonusToWorstSkill; // Done
    private boolean assignBestOfficers; // Done
    private boolean automaticallyAssignRanks; // Done

    // Units
    private boolean generateUnitsAsAttached;
    private boolean assignBestRollToUnitCommander;
    private boolean groupByWeight;

    // Unit
    private ForceNamingType forceNamingType;
    private boolean generateForceIcons;

    // Finances
    private int startingCash;
    private boolean randomizeStartingCash;
    private boolean startingLoan;
    private boolean payForSetup;
    private boolean payForUnits;
    private boolean payForPersonnel;
    private boolean payForParts;
    private boolean payForAmmunition;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationOptions(CompanyGenerationType type) {
        // Base Information
        setFaction(Factions.getInstance().getFaction("MERC"));
        setCompanyCount(1);
        setIndividualLanceCount(0);
        setGenerateMercenaryCompanyCommandLance(false);
        setLancesPerCompany(3);
        setLanceSize(4);

        // Personnel
        Map<Integer, Integer> supportPersonnel = new HashMap<>();
        if (type.isWindchild()) {
            supportPersonnel.put(Person.T_MECH_TECH, 5);
            supportPersonnel.put(Person.T_MECHANIC, 1);
            supportPersonnel.put(Person.T_AERO_TECH, 1);
            supportPersonnel.put(Person.T_DOCTOR, 1);
            supportPersonnel.put(Person.T_ADMIN_COM, 1);
            supportPersonnel.put(Person.T_ADMIN_LOG, 1);
            supportPersonnel.put(Person.T_ADMIN_TRA, 1);
            supportPersonnel.put(Person.T_ADMIN_HR, 1);
        } else { // Defaults to AtB
            supportPersonnel.put(Person.T_MECH_TECH, 10);
            supportPersonnel.put(Person.T_DOCTOR, 1);
            supportPersonnel.put(Person.T_ADMIN_LOG, 1);
        }
        setSupportPersonnel(supportPersonnel);
        setPoolAssistants(true);
        setGenerateCompanyCommanders(type.isWindchild());
        setCompanyCommanderLanceOfficer(type.isWindchild());
        setApplyOfficerStatBonusToWorstSkill(type.isWindchild());
        setAssignBestOfficers(type.isWindchild());
        setAutomaticallyAssignRanks(true);

        // Units
        setGenerateUnitsAsAttached(type.isAtB());
        setAssignBestRollToUnitCommander(type.isWindchild());
        setGroupByWeight(true);

        // Unit
        setForceNamingType(ForceNamingType.CCB_1943);
        setGenerateForceIcons(true);

        // Finances
        setStartingCash(0);
        setRandomizeStartingCash(type.isWindchild());
        setStartingLoan(type.isWindchild());
        setPayForSetup(type.isWindchild());
        setPayForUnits(type.isWindchild());
        setPayForPersonnel(type.isWindchild());
        setPayForParts(type.isWindchild());
        setPayForAmmunition(type.isWindchild());
    }
    //endregion Constructors

    //region Getters/Setters
    //region Base Information
    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public int getCompanyCount() {
        return companyCount;
    }

    public void setCompanyCount(int companyCount) {
        this.companyCount = companyCount;
    }

    public int getIndividualLanceCount() {
        return individualLanceCount;
    }

    public void setIndividualLanceCount(int individualLanceCount) {
        this.individualLanceCount = individualLanceCount;
    }

    public boolean isGenerateMercenaryCompanyCommandLance() {
        return generateMercenaryCompanyCommandLance;
    }

    public void setGenerateMercenaryCompanyCommandLance(boolean generateMercenaryCompanyCommandLance) {
        this.generateMercenaryCompanyCommandLance = generateMercenaryCompanyCommandLance;
    }

    public int getLanceSize() {
        return lanceSize;
    }

    public void setLanceSize(int lanceSize) {
        this.lanceSize = lanceSize;
    }

    public int getLancesPerCompany() {
        return lancesPerCompany;
    }

    public void setLancesPerCompany(int lancesPerCompany) {
        this.lancesPerCompany = lancesPerCompany;
    }
    //endregion Base Information

    //region Personnel
    public Map<Integer, Integer> getSupportPersonnel() {
        return supportPersonnel;
    }

    public void setSupportPersonnel(Map<Integer, Integer> supportPersonnel) {
        this.supportPersonnel = supportPersonnel;
    }

    public boolean isPoolAssistants() {
        return poolAssistants;
    }

    public void setPoolAssistants(boolean poolAssistants) {
        this.poolAssistants = poolAssistants;
    }

    public boolean isGenerateCompanyCommanders() {
        return generateCompanyCommanders;
    }

    public void setGenerateCompanyCommanders(boolean generateCompanyCommanders) {
        this.generateCompanyCommanders = generateCompanyCommanders;
    }

    public boolean isCompanyCommanderLanceOfficer() {
        return companyCommanderLanceOfficer;
    }

    public void setCompanyCommanderLanceOfficer(boolean companyCommanderLanceOfficer) {
        this.companyCommanderLanceOfficer = companyCommanderLanceOfficer;
    }

    public boolean isApplyOfficerStatBonusToWorstSkill() {
        return applyOfficerStatBonusToWorstSkill;
    }

    public void setApplyOfficerStatBonusToWorstSkill(boolean applyOfficerStatBonusToWorstSkill) {
        this.applyOfficerStatBonusToWorstSkill = applyOfficerStatBonusToWorstSkill;
    }

    public boolean isAssignBestOfficers() {
        return assignBestOfficers;
    }

    public void setAssignBestOfficers(boolean assignBestOfficers) {
        this.assignBestOfficers = assignBestOfficers;
    }

    public boolean isAutomaticallyAssignRanks() {
        return automaticallyAssignRanks;
    }

    public void setAutomaticallyAssignRanks(boolean automaticallyAssignRanks) {
        this.automaticallyAssignRanks = automaticallyAssignRanks;
    }
    //endregion Personnel

    //region Units
    public boolean isGenerateUnitsAsAttached() {
        return generateUnitsAsAttached;
    }

    public void setGenerateUnitsAsAttached(boolean generateUnitsAsAttached) {
        this.generateUnitsAsAttached = generateUnitsAsAttached;
    }

    public boolean isAssignBestRollToUnitCommander() {
        return assignBestRollToUnitCommander;
    }

    public void setAssignBestRollToUnitCommander(boolean assignBestRollToUnitCommander) {
        this.assignBestRollToUnitCommander = assignBestRollToUnitCommander;
    }

    public boolean isGroupByWeight() {
        return groupByWeight;
    }

    public void setGroupByWeight(boolean groupByWeight) {
        this.groupByWeight = groupByWeight;
    }
    //endregion Units

    //region Unit
    public ForceNamingType getForceNamingType() {
        return forceNamingType;
    }

    public void setForceNamingType(ForceNamingType forceNamingType) {
        this.forceNamingType = forceNamingType;
    }

    public boolean isGenerateForceIcons() {
        return generateForceIcons;
    }

    public void setGenerateForceIcons(boolean generateForceIcons) {
        this.generateForceIcons = generateForceIcons;
    }
    //endregion Unit

    //region Finances
    public int getStartingCash() {
        return startingCash;
    }

    public void setStartingCash(int startingCash) {
        this.startingCash = startingCash;
    }

    public boolean isRandomizeStartingCash() {
        return randomizeStartingCash;
    }

    public void setRandomizeStartingCash(boolean randomizeStartingCash) {
        this.randomizeStartingCash = randomizeStartingCash;
    }

    public boolean isStartingLoan() {
        return startingLoan;
    }

    public void setStartingLoan(boolean startingLoan) {
        this.startingLoan = startingLoan;
    }

    public boolean isPayForSetup() {
        return payForSetup;
    }

    public void setPayForSetup(boolean payForSetup) {
        this.payForSetup = payForSetup;
    }

    public boolean isPayForUnits() {
        return payForUnits;
    }

    public void setPayForUnits(boolean payForUnits) {
        this.payForUnits = payForUnits;
    }

    public boolean isPayForPersonnel() {
        return payForPersonnel;
    }

    public void setPayForPersonnel(boolean payForPersonnel) {
        this.payForPersonnel = payForPersonnel;
    }

    public boolean isPayForParts() {
        return payForParts;
    }

    public void setPayForParts(boolean payForParts) {
        this.payForParts = payForParts;
    }

    public boolean isPayForAmmunition() {
        return payForAmmunition;
    }

    public void setPayForAmmunition(boolean payForAmmunition) {
        this.payForAmmunition = payForAmmunition;
    }
    //endregion Finances
    //endregion Getters/Setters
}
