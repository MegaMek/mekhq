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

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

import java.util.Map;

public class CompanyGenerationOptions {
    //region Variable Declarations
    // Unit Setup
    private Faction faction;
    private int companyCount;
    private int individualLanceCount;

    // Personnel
    private Map<Integer, Integer> supportPersonnel;
    private boolean poolAssistants;
    private boolean generateCompanyCommander;
    private boolean generateOfficers;
    /**
     * If the company commander counts as one of the lance officers (reduces officer generation by 1 if true)
     */
    private boolean companyCommanderLanceOfficer;
    private boolean applyOfficerStatBonusToWorstSkill;
    private boolean assignBestOfficers;
    private boolean automaticallyAssignRanks;

    // Finances
    private int startingCash;
    private boolean randomizeStartingCash;
    private boolean startingLoan;
    private boolean payForSetup;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationOptions() {
        // Unit Setup
        setFaction(Factions.getInstance().getFaction("MERC"));

        // Personnel
        setPoolAssistants(true);
        setGenerateCompanyCommander(true);
        setGenerateOfficers(true);
        setCompanyCommanderLanceOfficer(false);
        setAutomaticallyAssignRanks(false);

        // Finances
        setStartingLoan(false);
    }
    //endregion Constructors

    //region Getters/Setters
    //region Unit Setup
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
    //endregion Unit Setup

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

    public boolean isGenerateCompanyCommander() {
        return generateCompanyCommander;
    }

    public void setGenerateCompanyCommander(boolean generateCompanyCommander) {
        this.generateCompanyCommander = generateCompanyCommander;
    }

    public boolean isGenerateOfficers() {
        return generateOfficers;
    }

    public void setGenerateOfficers(boolean generateOfficers) {
        this.generateOfficers = generateOfficers;
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
    //endregion Finances
    //endregion Getters/Setters
}
