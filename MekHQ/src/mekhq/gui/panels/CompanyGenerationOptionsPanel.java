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
package mekhq.gui.panels;

import megamek.client.ui.baseComponents.JDisableablePanel;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.enums.ValidationState;
import megamek.common.EntityWeightClass;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.enums.*;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JSpinner.NumberEditor;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author Justin "Windchild" Bowen
 */
public class CompanyGenerationOptionsPanel extends AbstractMHQScrollablePanel {
    //region Variable Declarations
    private final Campaign campaign;

    // Base Information
    private MMComboBox<CompanyGenerationMethod> comboCompanyGenerationMethod;
    private MMComboBox<FactionDisplay> comboSpecifiedFaction;
    private JCheckBox chkGenerateMercenaryCompanyCommandLance;
    private JSpinner spnCompanyCount;
    private JSpinner spnIndividualLanceCount;
    private JSpinner spnLancesPerCompany;
    private JSpinner spnLanceSize;
    private JSpinner spnStarLeagueYear;

    // Personnel
    private JLabel lblTotalSupportPersonnel;
    private Map<PersonnelRole, JSpinner> spnSupportPersonnelNumbers;
    private JCheckBox chkPoolAssistants;
    private JCheckBox chkGenerateCaptains;
    private JCheckBox chkAssignCompanyCommanderFlag;
    private JCheckBox chkApplyOfficerStatBonusToWorstSkill;
    private JCheckBox chkAssignBestCompanyCommander;
    private JCheckBox chkPrioritizeCompanyCommanderCombatSkills;
    private JCheckBox chkAssignBestOfficers;
    private JCheckBox chkPrioritizeOfficerCombatSkills;
    private JCheckBox chkAssignMostSkilledToPrimaryLances;
    private JCheckBox chkAutomaticallyAssignRanks;
    private JCheckBox chkUseSpecifiedFactionToAssignRanks;
    private JCheckBox chkAssignMekWarriorsCallsigns;
    private JCheckBox chkAssignFounderFlag;

    // Personnel Randomization
    private RandomOriginOptionsPanel randomOriginOptionsPanel;

    // Starting Simulation
    private JCheckBox chkRunStartingSimulation;
    private JSpinner spnSimulationDuration;
    private JCheckBox chkSimulateRandomMarriages;
    private JCheckBox chkSimulateRandomProcreation;

    // Units
    private MMComboBox<BattleMekFactionGenerationMethod> comboBattleMekFactionGenerationMethod;
    private MMComboBox<BattleMekWeightClassGenerationMethod> comboBattleMekWeightClassGenerationMethod;
    private MMComboBox<BattleMekQualityGenerationMethod> comboBattleMekQualityGenerationMethod;
    private JCheckBox chkNeverGenerateStarLeagueMeks;
    private JCheckBox chkOnlyGenerateStarLeagueMeks;
    private JCheckBox chkOnlyGenerateOmniMeks;
    private JCheckBox chkGenerateUnitsAsAttached;
    private JCheckBox chkAssignBestRollToCompanyCommander;
    private JCheckBox chkSortStarLeagueUnitsFirst;
    private JCheckBox chkGroupByWeight;
    private JCheckBox chkGroupByQuality;
    private JCheckBox chkKeepOfficerRollsSeparate;
    private JCheckBox chkAssignTechsToUnits;

    // Unit
    private MMComboBox<ForceNamingMethod> comboForceNamingMethod;
    private JCheckBox chkGenerateForceIcons;
    private JCheckBox chkUseSpecifiedFactionToGenerateForceIcons;
    private JCheckBox chkGenerateOriginNodeForceIcon;
    private JCheckBox chkUseOriginNodeForceIconLogo;
    private Map<Integer, JSpinner> spnForceWeightLimits;

    // Spares
    private JCheckBox chkGenerateMothballedSpareUnits;
    private JSpinner spnSparesPercentOfActiveUnits;
    private MMComboBox<PartGenerationMethod> comboPartGenerationMethod;
    private JSpinner spnStartingArmourWeight;
    private JCheckBox chkGenerateSpareAmmunition;
    private JSpinner spnNumberReloadsPerWeapon;
    private JCheckBox chkGenerateFractionalMachineGunAmmunition;

    // Contracts
    private JCheckBox chkSelectStartingContract;
    private JCheckBox chkStartCourseToContractPlanet;

    // Finances
    private JCheckBox chkProcessFinances;
    private JSpinner spnStartingCash;
    private JCheckBox chkRandomizeStartingCash;
    private JSpinner spnRandomStartingCashDiceCount;
    private JSpinner spnMinimumStartingFloat;
    private JCheckBox chkIncludeInitialContractPayment;
    private JCheckBox chkStartingLoan;
    private JCheckBox chkPayForSetup;
    private JCheckBox chkPayForPersonnel;
    private JCheckBox chkPayForUnits;
    private JCheckBox chkPayForParts;
    private JCheckBox chkPayForArmour;
    private JCheckBox chkPayForAmmunition;

    // Surprises
    private JCheckBox chkGenerateSurprises;
    private JCheckBox chkGenerateMysteryBoxes;
    private Map<MysteryBoxType, JCheckBox> chkGenerateMysteryBoxTypes;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationOptionsPanel(final JFrame frame, final Campaign campaign,
                                         final @Nullable CompanyGenerationOptions companyGenerationOptions) {
        super(frame, "CompanyGenerationOptionsPanel", new GridBagLayout());
        this.campaign = campaign;
        setTracksViewportWidth(false);

        initialize();

        if (companyGenerationOptions == null) {
            setOptions(MekHQ.getMHQOptions().getDefaultCompanyGenerationMethod());
        } else {
            setOptions(companyGenerationOptions);
        }
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    //region Base Information
    public MMComboBox<CompanyGenerationMethod> getComboCompanyGenerationMethod() {
        return comboCompanyGenerationMethod;
    }

    public void setComboCompanyGenerationMethod(final MMComboBox<CompanyGenerationMethod> comboCompanyGenerationMethod) {
        this.comboCompanyGenerationMethod = comboCompanyGenerationMethod;
    }

    public MMComboBox<FactionDisplay> getComboSpecifiedFaction() {
        return comboSpecifiedFaction;
    }

    public void setComboSpecifiedFaction(final MMComboBox<FactionDisplay> comboSpecifiedFaction) {
        this.comboSpecifiedFaction = comboSpecifiedFaction;
    }

    public JCheckBox getChkGenerateMercenaryCompanyCommandLance() {
        return chkGenerateMercenaryCompanyCommandLance;
    }

    public void setChkGenerateMercenaryCompanyCommandLance(final JCheckBox chkGenerateMercenaryCompanyCommandLance) {
        this.chkGenerateMercenaryCompanyCommandLance = chkGenerateMercenaryCompanyCommandLance;
    }

    public JSpinner getSpnCompanyCount() {
        return spnCompanyCount;
    }

    public void setSpnCompanyCount(final JSpinner spnCompanyCount) {
        this.spnCompanyCount = spnCompanyCount;
    }

    public JSpinner getSpnIndividualLanceCount() {
        return spnIndividualLanceCount;
    }

    public void setSpnIndividualLanceCount(final JSpinner spnIndividualLanceCount) {
        this.spnIndividualLanceCount = spnIndividualLanceCount;
    }

    public JSpinner getSpnLancesPerCompany() {
        return spnLancesPerCompany;
    }

    public void setSpnLancesPerCompany(final JSpinner spnLancesPerCompany) {
        this.spnLancesPerCompany = spnLancesPerCompany;
    }

    public JSpinner getSpnLanceSize() {
        return spnLanceSize;
    }

    public void setSpnLanceSize(final JSpinner spnLanceSize) {
        this.spnLanceSize = spnLanceSize;
    }

    public JSpinner getSpnStarLeagueYear() {
        return spnStarLeagueYear;
    }

    public void setSpnStarLeagueYear(final JSpinner spnStarLeagueYear) {
        this.spnStarLeagueYear = spnStarLeagueYear;
    }
    //endregion Base Information

    //region Personnel
    public JLabel getLblTotalSupportPersonnel() {
        return lblTotalSupportPersonnel;
    }

    public void updateLblTotalSupportPersonnel(final int numSupportPersonnel) {
        getLblTotalSupportPersonnel().setText(String.format(
                resources.getString("lblTotalSupportPersonnel.text"), numSupportPersonnel));
    }

    public void setLblTotalSupportPersonnel(final JLabel lblTotalSupportPersonnel) {
        this.lblTotalSupportPersonnel = lblTotalSupportPersonnel;
    }

    public Map<PersonnelRole, JSpinner> getSpnSupportPersonnelNumbers() {
        return spnSupportPersonnelNumbers;
    }

    public void setSpnSupportPersonnelNumbers(final Map<PersonnelRole, JSpinner> spnSupportPersonnelNumbers) {
        this.spnSupportPersonnelNumbers = spnSupportPersonnelNumbers;
    }

    public JCheckBox getChkPoolAssistants() {
        return chkPoolAssistants;
    }

    public void setChkPoolAssistants(final JCheckBox chkPoolAssistants) {
        this.chkPoolAssistants = chkPoolAssistants;
    }

    public JCheckBox getChkGenerateCaptains() {
        return chkGenerateCaptains;
    }

    public void setChkGenerateCaptains(final JCheckBox chkGenerateCaptains) {
        this.chkGenerateCaptains = chkGenerateCaptains;
    }

    public JCheckBox getChkAssignCompanyCommanderFlag() {
        return chkAssignCompanyCommanderFlag;
    }

    public void setChkAssignCompanyCommanderFlag(final JCheckBox chkAssignCompanyCommanderFlag) {
        this.chkAssignCompanyCommanderFlag = chkAssignCompanyCommanderFlag;
    }

    public JCheckBox getChkApplyOfficerStatBonusToWorstSkill() {
        return chkApplyOfficerStatBonusToWorstSkill;
    }

    public void setChkApplyOfficerStatBonusToWorstSkill(final JCheckBox chkApplyOfficerStatBonusToWorstSkill) {
        this.chkApplyOfficerStatBonusToWorstSkill = chkApplyOfficerStatBonusToWorstSkill;
    }

    public JCheckBox getChkAssignBestCompanyCommander() {
        return chkAssignBestCompanyCommander;
    }

    public void setChkAssignBestCompanyCommander(final JCheckBox chkAssignBestCompanyCommander) {
        this.chkAssignBestCompanyCommander = chkAssignBestCompanyCommander;
    }

    public JCheckBox getChkPrioritizeCompanyCommanderCombatSkills() {
        return chkPrioritizeCompanyCommanderCombatSkills;
    }

    public void setChkPrioritizeCompanyCommanderCombatSkills(final JCheckBox chkPrioritizeCompanyCommanderCombatSkills) {
        this.chkPrioritizeCompanyCommanderCombatSkills = chkPrioritizeCompanyCommanderCombatSkills;
    }

    public JCheckBox getChkAssignBestOfficers() {
        return chkAssignBestOfficers;
    }

    public void setChkAssignBestOfficers(final JCheckBox chkAssignBestOfficers) {
        this.chkAssignBestOfficers = chkAssignBestOfficers;
    }

    public JCheckBox getChkPrioritizeOfficerCombatSkills() {
        return chkPrioritizeOfficerCombatSkills;
    }

    public void setChkPrioritizeOfficerCombatSkills(final JCheckBox chkPrioritizeOfficerCombatSkills) {
        this.chkPrioritizeOfficerCombatSkills = chkPrioritizeOfficerCombatSkills;
    }

    public JCheckBox getChkAssignMostSkilledToPrimaryLances() {
        return chkAssignMostSkilledToPrimaryLances;
    }

    public void setChkAssignMostSkilledToPrimaryLances(final JCheckBox chkAssignMostSkilledToPrimaryLances) {
        this.chkAssignMostSkilledToPrimaryLances = chkAssignMostSkilledToPrimaryLances;
    }

    public JCheckBox getChkAutomaticallyAssignRanks() {
        return chkAutomaticallyAssignRanks;
    }

    public void setChkAutomaticallyAssignRanks(final JCheckBox chkAutomaticallyAssignRanks) {
        this.chkAutomaticallyAssignRanks = chkAutomaticallyAssignRanks;
    }

    public JCheckBox getChkUseSpecifiedFactionToAssignRanks() {
        return chkUseSpecifiedFactionToAssignRanks;
    }

    public void setChkUseSpecifiedFactionToAssignRanks(final JCheckBox chkUseSpecifiedFactionToAssignRanks) {
        this.chkUseSpecifiedFactionToAssignRanks = chkUseSpecifiedFactionToAssignRanks;
    }

    public JCheckBox getChkAssignMekWarriorsCallsigns() {
        return chkAssignMekWarriorsCallsigns;
    }

    public void setChkAssignMekWarriorsCallsigns(final JCheckBox chkAssignMekWarriorsCallsigns) {
        this.chkAssignMekWarriorsCallsigns = chkAssignMekWarriorsCallsigns;
    }

    public JCheckBox getChkAssignFounderFlag() {
        return chkAssignFounderFlag;
    }

    public void setChkAssignFounderFlag(final JCheckBox chkAssignFounderFlag) {
        this.chkAssignFounderFlag = chkAssignFounderFlag;
    }
    //endregion Personnel

    //region Personnel Randomization
    public RandomOriginOptionsPanel getRandomOriginOptionsPanel() {
        return randomOriginOptionsPanel;
    }

    public void setRandomOriginOptionsPanel(final RandomOriginOptionsPanel randomOriginOptionsPanel) {
        this.randomOriginOptionsPanel = randomOriginOptionsPanel;
    }
    //endregion Personnel Randomization

    //region Starting Simulation
    public JCheckBox getChkRunStartingSimulation() {
        return chkRunStartingSimulation;
    }

    public void setChkRunStartingSimulation(final JCheckBox chkRunStartingSimulation) {
        this.chkRunStartingSimulation = chkRunStartingSimulation;
    }

    public JSpinner getSpnSimulationDuration() {
        return spnSimulationDuration;
    }

    public void setSpnSimulationDuration(final JSpinner spnSimulationDuration) {
        this.spnSimulationDuration = spnSimulationDuration;
    }

    public JCheckBox getChkSimulateRandomMarriages() {
        return chkSimulateRandomMarriages;
    }

    public void setChkSimulateRandomMarriages(final JCheckBox chkSimulateRandomMarriages) {
        this.chkSimulateRandomMarriages = chkSimulateRandomMarriages;
    }

    public JCheckBox getChkSimulateRandomProcreation() {
        return chkSimulateRandomProcreation;
    }

    public void setChkSimulateRandomProcreation(final JCheckBox chkSimulateRandomProcreation) {
        this.chkSimulateRandomProcreation = chkSimulateRandomProcreation;
    }
    //endregion Starting Simulation

    //region Units
    public MMComboBox<BattleMekFactionGenerationMethod> getComboBattleMekFactionGenerationMethod() {
        return comboBattleMekFactionGenerationMethod;
    }

    public void setComboBattleMekFactionGenerationMethod(
            final MMComboBox<BattleMekFactionGenerationMethod> comboBattleMekFactionGenerationMethod) {
        this.comboBattleMekFactionGenerationMethod = comboBattleMekFactionGenerationMethod;
    }

    public MMComboBox<BattleMekWeightClassGenerationMethod> getComboBattleMekWeightClassGenerationMethod() {
        return comboBattleMekWeightClassGenerationMethod;
    }

    public void setComboBattleMekWeightClassGenerationMethod(
            final MMComboBox<BattleMekWeightClassGenerationMethod> comboBattleMekWeightClassGenerationMethod) {
        this.comboBattleMekWeightClassGenerationMethod = comboBattleMekWeightClassGenerationMethod;
    }

    public MMComboBox<BattleMekQualityGenerationMethod> getComboBattleMekQualityGenerationMethod() {
        return comboBattleMekQualityGenerationMethod;
    }

    public void setComboBattleMekQualityGenerationMethod(
            final MMComboBox<BattleMekQualityGenerationMethod> comboBattleMekQualityGenerationMethod) {
        this.comboBattleMekQualityGenerationMethod = comboBattleMekQualityGenerationMethod;
    }

    public JCheckBox getChkNeverGenerateStarLeagueMeks() {
        return chkNeverGenerateStarLeagueMeks;
    }

    public void setChkNeverGenerateStarLeagueMeks(final JCheckBox chkNeverGenerateStarLeagueMeks) {
        this.chkNeverGenerateStarLeagueMeks = chkNeverGenerateStarLeagueMeks;
    }

    public JCheckBox getChkOnlyGenerateStarLeagueMeks() {
        return chkOnlyGenerateStarLeagueMeks;
    }

    public void setChkOnlyGenerateStarLeagueMeks(JCheckBox chkOnlyGenerateStarLeagueMeks) {
        this.chkOnlyGenerateStarLeagueMeks = chkOnlyGenerateStarLeagueMeks;
    }

    public JCheckBox getChkOnlyGenerateOmniMeks() {
        return chkOnlyGenerateOmniMeks;
    }

    public void setChkOnlyGenerateOmniMeks(JCheckBox chkOnlyGenerateOmniMeks) {
        this.chkOnlyGenerateOmniMeks = chkOnlyGenerateOmniMeks;
    }

    public JCheckBox getChkGenerateUnitsAsAttached() {
        return chkGenerateUnitsAsAttached;
    }

    public void setChkGenerateUnitsAsAttached(final JCheckBox chkGenerateUnitsAsAttached) {
        this.chkGenerateUnitsAsAttached = chkGenerateUnitsAsAttached;
    }

    public JCheckBox getChkAssignBestRollToCompanyCommander() {
        return chkAssignBestRollToCompanyCommander;
    }

    public void setChkAssignBestRollToCompanyCommander(final JCheckBox chkAssignBestRollToCompanyCommander) {
        this.chkAssignBestRollToCompanyCommander = chkAssignBestRollToCompanyCommander;
    }

    public JCheckBox getChkSortStarLeagueUnitsFirst() {
        return chkSortStarLeagueUnitsFirst;
    }

    public void setChkSortStarLeagueUnitsFirst(final JCheckBox chkSortStarLeagueUnitsFirst) {
        this.chkSortStarLeagueUnitsFirst = chkSortStarLeagueUnitsFirst;
    }

    public JCheckBox getChkGroupByWeight() {
        return chkGroupByWeight;
    }

    public void setChkGroupByWeight(final JCheckBox chkGroupByWeight) {
        this.chkGroupByWeight = chkGroupByWeight;
    }

    public JCheckBox getChkGroupByQuality() {
        return chkGroupByQuality;
    }

    public void setChkGroupByQuality(final JCheckBox chkGroupByQuality) {
        this.chkGroupByQuality = chkGroupByQuality;
    }

    public JCheckBox getChkKeepOfficerRollsSeparate() {
        return chkKeepOfficerRollsSeparate;
    }

    public void setChkKeepOfficerRollsSeparate(final JCheckBox chkKeepOfficerRollsSeparate) {
        this.chkKeepOfficerRollsSeparate = chkKeepOfficerRollsSeparate;
    }

    public JCheckBox getChkAssignTechsToUnits() {
        return chkAssignTechsToUnits;
    }

    public void setChkAssignTechsToUnits(final JCheckBox chkAssignTechsToUnits) {
        this.chkAssignTechsToUnits = chkAssignTechsToUnits;
    }
    //endregion Units

    //region Unit
    public MMComboBox<ForceNamingMethod> getComboForceNamingMethod() {
        return comboForceNamingMethod;
    }

    public void setComboForceNamingMethod(final MMComboBox<ForceNamingMethod> comboForceNamingMethod) {
        this.comboForceNamingMethod = comboForceNamingMethod;
    }

    public JCheckBox getChkGenerateForceIcons() {
        return chkGenerateForceIcons;
    }

    public void setChkGenerateForceIcons(final JCheckBox chkGenerateForceIcons) {
        this.chkGenerateForceIcons = chkGenerateForceIcons;
    }

    public JCheckBox getChkUseSpecifiedFactionToGenerateForceIcons() {
        return chkUseSpecifiedFactionToGenerateForceIcons;
    }

    public void setChkUseSpecifiedFactionToGenerateForceIcons(
            final JCheckBox chkUseSpecifiedFactionToGenerateForceIcons) {
        this.chkUseSpecifiedFactionToGenerateForceIcons = chkUseSpecifiedFactionToGenerateForceIcons;
    }

    public JCheckBox getChkGenerateOriginNodeForceIcon() {
        return chkGenerateOriginNodeForceIcon;
    }

    public void setChkGenerateOriginNodeForceIcon(final JCheckBox chkGenerateOriginNodeForceIcon) {
        this.chkGenerateOriginNodeForceIcon = chkGenerateOriginNodeForceIcon;
    }

    public JCheckBox getChkUseOriginNodeForceIconLogo() {
        return chkUseOriginNodeForceIconLogo;
    }

    public void setChkUseOriginNodeForceIconLogo(final JCheckBox chkUseOriginNodeForceIconLogo) {
        this.chkUseOriginNodeForceIconLogo = chkUseOriginNodeForceIconLogo;
    }

    public Map<Integer, JSpinner> getSpnForceWeightLimits() {
        return spnForceWeightLimits;
    }

    public void setSpnForceWeightLimits(final Map<Integer, JSpinner> spnForceWeightLimits) {
        this.spnForceWeightLimits = spnForceWeightLimits;
    }
    //endregion Unit

    //region Spares
    public JCheckBox getChkGenerateMothballedSpareUnits() {
        return chkGenerateMothballedSpareUnits;
    }

    public void setChkGenerateMothballedSpareUnits(final JCheckBox chkGenerateMothballedSpareUnits) {
        this.chkGenerateMothballedSpareUnits = chkGenerateMothballedSpareUnits;
    }

    public JSpinner getSpnSparesPercentOfActiveUnits() {
        return spnSparesPercentOfActiveUnits;
    }

    public void setSpnSparesPercentOfActiveUnits(final JSpinner spnSparesPercentOfActiveUnits) {
        this.spnSparesPercentOfActiveUnits = spnSparesPercentOfActiveUnits;
    }

    public MMComboBox<PartGenerationMethod> getComboPartGenerationMethod() {
        return comboPartGenerationMethod;
    }

    public void setComboPartGenerationMethod(final MMComboBox<PartGenerationMethod> comboPartGenerationMethod) {
        this.comboPartGenerationMethod = comboPartGenerationMethod;
    }

    public JSpinner getSpnStartingArmourWeight() {
        return spnStartingArmourWeight;
    }

    public void setSpnStartingArmourWeight(final JSpinner spnStartingArmourWeight) {
        this.spnStartingArmourWeight = spnStartingArmourWeight;
    }

    public JCheckBox getChkGenerateSpareAmmunition() {
        return chkGenerateSpareAmmunition;
    }

    public void setChkGenerateSpareAmmunition(final JCheckBox chkGenerateSpareAmmunition) {
        this.chkGenerateSpareAmmunition = chkGenerateSpareAmmunition;
    }

    public JSpinner getSpnNumberReloadsPerWeapon() {
        return spnNumberReloadsPerWeapon;
    }

    public void setSpnNumberReloadsPerWeapon(final JSpinner spnNumberReloadsPerWeapon) {
        this.spnNumberReloadsPerWeapon = spnNumberReloadsPerWeapon;
    }

    public JCheckBox getChkGenerateFractionalMachineGunAmmunition() {
        return chkGenerateFractionalMachineGunAmmunition;
    }

    public void setChkGenerateFractionalMachineGunAmmunition(final JCheckBox chkGenerateFractionalMachineGunAmmunition) {
        this.chkGenerateFractionalMachineGunAmmunition = chkGenerateFractionalMachineGunAmmunition;
    }
    //endregion Spares

    //region Contracts
    public JCheckBox getChkSelectStartingContract() {
        return chkSelectStartingContract;
    }

    public void setChkSelectStartingContract(final JCheckBox chkSelectStartingContract) {
        this.chkSelectStartingContract = chkSelectStartingContract;
    }

    public JCheckBox getChkStartCourseToContractPlanet() {
        return chkStartCourseToContractPlanet;
    }

    public void setChkStartCourseToContractPlanet(final JCheckBox chkStartCourseToContractPlanet) {
        this.chkStartCourseToContractPlanet = chkStartCourseToContractPlanet;
    }
    //endregion Contracts

    //region Finances
    public JCheckBox getChkProcessFinances() {
        return chkProcessFinances;
    }

    public void setChkProcessFinances(final JCheckBox chkProcessFinances) {
        this.chkProcessFinances = chkProcessFinances;
    }

    public JSpinner getSpnStartingCash() {
        return spnStartingCash;
    }

    public void setSpnStartingCash(final JSpinner spnStartingCash) {
        this.spnStartingCash = spnStartingCash;
    }

    public JCheckBox getChkRandomizeStartingCash() {
        return chkRandomizeStartingCash;
    }

    public void setChkRandomizeStartingCash(final JCheckBox chkRandomizeStartingCash) {
        this.chkRandomizeStartingCash = chkRandomizeStartingCash;
    }

    public JSpinner getSpnRandomStartingCashDiceCount() {
        return spnRandomStartingCashDiceCount;
    }

    public void setSpnRandomStartingCashDiceCount(final JSpinner spnRandomStartingCashDiceCount) {
        this.spnRandomStartingCashDiceCount = spnRandomStartingCashDiceCount;
    }

    public JSpinner getSpnMinimumStartingFloat() {
        return spnMinimumStartingFloat;
    }

    public void setSpnMinimumStartingFloat(final JSpinner spnMinimumStartingFloat) {
        this.spnMinimumStartingFloat = spnMinimumStartingFloat;
    }

    public JCheckBox getChkIncludeInitialContractPayment() {
        return chkIncludeInitialContractPayment;
    }

    public void setChkIncludeInitialContractPayment(final JCheckBox chkIncludeInitialContractPayment) {
        this.chkIncludeInitialContractPayment = chkIncludeInitialContractPayment;
    }

    public JCheckBox getChkStartingLoan() {
        return chkStartingLoan;
    }

    public void setChkStartingLoan(final JCheckBox chkStartingLoan) {
        this.chkStartingLoan = chkStartingLoan;
    }

    public JCheckBox getChkPayForSetup() {
        return chkPayForSetup;
    }

    public void setChkPayForSetup(final JCheckBox chkPayForSetup) {
        this.chkPayForSetup = chkPayForSetup;
    }

    public JCheckBox getChkPayForPersonnel() {
        return chkPayForPersonnel;
    }

    public void setChkPayForPersonnel(final JCheckBox chkPayForPersonnel) {
        this.chkPayForPersonnel = chkPayForPersonnel;
    }

    public JCheckBox getChkPayForUnits() {
        return chkPayForUnits;
    }

    public void setChkPayForUnits(final JCheckBox chkPayForUnits) {
        this.chkPayForUnits = chkPayForUnits;
    }

    public JCheckBox getChkPayForParts() {
        return chkPayForParts;
    }

    public void setChkPayForParts(final JCheckBox chkPayForParts) {
        this.chkPayForParts = chkPayForParts;
    }

    public JCheckBox getChkPayForArmour() {
        return chkPayForArmour;
    }

    public void setChkPayForArmour(final JCheckBox chkPayForArmour) {
        this.chkPayForArmour = chkPayForArmour;
    }

    public JCheckBox getChkPayForAmmunition() {
        return chkPayForAmmunition;
    }

    public void setChkPayForAmmunition(final JCheckBox chkPayForAmmunition) {
        this.chkPayForAmmunition = chkPayForAmmunition;
    }
    //endregion Finances

    //region Surprises
    public JCheckBox getChkGenerateSurprises() {
        return chkGenerateSurprises;
    }

    public void setChkGenerateSurprises(final JCheckBox chkGenerateSurprises) {
        this.chkGenerateSurprises = chkGenerateSurprises;
    }

    public JCheckBox getChkGenerateMysteryBoxes() {
        return chkGenerateMysteryBoxes;
    }

    public void setChkGenerateMysteryBoxes(final JCheckBox chkGenerateMysteryBoxes) {
        this.chkGenerateMysteryBoxes = chkGenerateMysteryBoxes;
    }

    public Map<MysteryBoxType, JCheckBox> getChkGenerateMysteryBoxTypes() {
        return chkGenerateMysteryBoxTypes;
    }

    public void setChkGenerateMysteryBoxTypes(final Map<MysteryBoxType, JCheckBox> chkGenerateMysteryBoxTypes) {
        this.chkGenerateMysteryBoxTypes = chkGenerateMysteryBoxTypes;
    }
    //endregion Surprises
    //endregion Getters/Setters

    //region Determination Methods
    public int determineMaximumSupportPersonnel() {
        return ((getChkGenerateMercenaryCompanyCommandLance().isSelected() ? 1 : 0)
                + ((int) getSpnCompanyCount().getValue() * (int) getSpnLancesPerCompany().getValue())
                + (int) getSpnIndividualLanceCount().getValue()) * (int) getSpnLanceSize().getValue();
    }
    //endregion Determination Methods

    //region Initialization
    @Override
    protected void initialize() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(createBaseInformationPanel(), gbc);

        gbc.gridx++;
        add(createPersonnelPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(createPersonnelRandomizationPanel(), gbc);

        gbc.gridx++;
        add(createStartingSimulationPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(createUnitsPanel(), gbc);

        gbc.gridx++;
        add(createUnitPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(createSparesPanel(), gbc);

        gbc.gridx++;
        add(createContractsPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(createFinancesPanel(), gbc);

        gbc.gridx++;
        add(createSurprisesPanel(), gbc);
    }

    private JPanel createBaseInformationPanel() {
        // Create Panel Components
        final JLabel lblCompanyGenerationMethod = new JLabel(resources.getString("lblCompanyGenerationMethod.text"));
        lblCompanyGenerationMethod.setToolTipText(resources.getString("lblCompanyGenerationMethod.toolTipText"));
        lblCompanyGenerationMethod.setName("lblCompanyGenerationMethod");

        setComboCompanyGenerationMethod(new MMComboBox<>("comboCompanyGenerationMethod", CompanyGenerationMethod.values()));
        getComboCompanyGenerationMethod().setToolTipText(resources.getString("lblCompanyGenerationMethod.toolTipText"));
        getComboCompanyGenerationMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof CompanyGenerationMethod) {
                    list.setToolTipText(((CompanyGenerationMethod) value).getToolTipText());
                }
                return this;
            }
        });

        final JLabel lblSpecifiedFaction = new JLabel(resources.getString("lblSpecifiedFaction.text"));
        lblSpecifiedFaction.setToolTipText(resources.getString("lblSpecifiedFaction.toolTipText"));
        lblSpecifiedFaction.setName("lblSpecifiedFaction");

        final DefaultComboBoxModel<FactionDisplay> specifiedFactionModel = new DefaultComboBoxModel<>();
        specifiedFactionModel.addAll(FactionDisplay
                .getSortedValidFactionDisplays(Factions.getInstance().getChoosableFactions(), getCampaign().getLocalDate()));
        setComboSpecifiedFaction(new MMComboBox<>("comboFaction", specifiedFactionModel));
        getComboSpecifiedFaction().setToolTipText(resources.getString("lblSpecifiedFaction.toolTipText"));

        setChkGenerateMercenaryCompanyCommandLance(new JCheckBox(resources.getString("chkGenerateMercenaryCompanyCommandLance.text")));
        getChkGenerateMercenaryCompanyCommandLance().setToolTipText(resources.getString("chkGenerateMercenaryCompanyCommandLance.toolTipText"));
        getChkGenerateMercenaryCompanyCommandLance().setName("chkGenerateMercenaryCompanyCommandLance");

        final JLabel lblCompanyCount = new JLabel(resources.getString("lblCompanyCount.text"));
        lblCompanyCount.setToolTipText(resources.getString("lblCompanyCount.toolTipText"));
        lblCompanyCount.setName("lblCompanyCount");

        setSpnCompanyCount(new JSpinner(new SpinnerNumberModel(0, 0, 5, 1)));
        getSpnCompanyCount().setToolTipText(resources.getString("lblCompanyCount.toolTipText"));
        getSpnCompanyCount().setName("spnCompanyCount");

        final JLabel lblIndividualLanceCount = new JLabel(resources.getString("lblIndividualLanceCount.text"));
        lblIndividualLanceCount.setToolTipText(resources.getString("lblIndividualLanceCount.toolTipText"));
        lblIndividualLanceCount.setName("lblIndividualLanceCount");

        setSpnIndividualLanceCount(new JSpinner(new SpinnerNumberModel(0, 0, 2, 1)));
        getSpnIndividualLanceCount().setToolTipText(resources.getString("lblIndividualLanceCount.toolTipText"));
        getSpnIndividualLanceCount().setName("spnIndividualLanceCount");

        final JLabel lblLancesPerCompany = new JLabel(resources.getString("lblLancesPerCompany.text"));
        lblLancesPerCompany.setToolTipText(resources.getString("lblLancesPerCompany.toolTipText"));
        lblLancesPerCompany.setName("lblLancesPerCompany");

        setSpnLancesPerCompany(new JSpinner(new SpinnerNumberModel(3, 2, 6, 1)));
        getSpnLancesPerCompany().setToolTipText(resources.getString("lblLancesPerCompany.toolTipText"));
        getSpnLancesPerCompany().setName("spnLancesPerCompany");

        final JLabel lblLanceSize = new JLabel(resources.getString("lblLanceSize.text"));
        lblLanceSize.setToolTipText(resources.getString("lblLanceSize.toolTipText"));
        lblLanceSize.setName("lblLanceSize");

        setSpnLanceSize(new JSpinner(new SpinnerNumberModel(4, 3, 6, 1)));
        getSpnLanceSize().setToolTipText(resources.getString("lblLanceSize.toolTipText"));
        getSpnLanceSize().setName("spnLanceSize");

        final JLabel lblStarLeagueYear = new JLabel(resources.getString("lblStarLeagueYear.text"));
        lblStarLeagueYear.setToolTipText(resources.getString("lblStarLeagueYear.toolTipText"));
        lblStarLeagueYear.setName("lblStarLeagueYear");

        setSpnStarLeagueYear(new JSpinner(new SpinnerNumberModel(2765, 2571, 2780, 1)));
        getSpnStarLeagueYear().setToolTipText(resources.getString("lblStarLeagueYear.toolTipText"));
        getSpnStarLeagueYear().setName("spnStarLeagueYear");
        getSpnStarLeagueYear().setEditor(new NumberEditor(getSpnStarLeagueYear(), "#"));

        // Programmatically Assign Accessibility Labels
        lblCompanyGenerationMethod.setLabelFor(getComboCompanyGenerationMethod());
        lblSpecifiedFaction.setLabelFor(getComboSpecifiedFaction());
        lblCompanyCount.setLabelFor(getSpnCompanyCount());
        lblIndividualLanceCount.setLabelFor(getSpnIndividualLanceCount());
        lblLancesPerCompany.setLabelFor(getSpnLancesPerCompany());
        lblLanceSize.setLabelFor(getSpnLanceSize());
        lblStarLeagueYear.setLabelFor(getSpnStarLeagueYear());

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("baseInformationPanel.title")));
        panel.setName("baseInformationPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblCompanyGenerationMethod)
                                .addComponent(getComboCompanyGenerationMethod(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblSpecifiedFaction)
                                .addComponent(getComboSpecifiedFaction(), Alignment.LEADING))
                        .addComponent(getChkGenerateMercenaryCompanyCommandLance())
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblCompanyCount)
                                .addComponent(getSpnCompanyCount())
                                .addComponent(lblIndividualLanceCount)
                                .addComponent(getSpnIndividualLanceCount(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblLancesPerCompany)
                                .addComponent(getSpnLancesPerCompany())
                                .addComponent(lblLanceSize)
                                .addComponent(getSpnLanceSize(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStarLeagueYear)
                                .addComponent(getSpnStarLeagueYear(), Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCompanyGenerationMethod)
                                .addComponent(getComboCompanyGenerationMethod()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblSpecifiedFaction)
                                .addComponent(getComboSpecifiedFaction()))
                        .addComponent(getChkGenerateMercenaryCompanyCommandLance())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCompanyCount)
                                .addComponent(getSpnCompanyCount())
                                .addComponent(lblIndividualLanceCount)
                                .addComponent(getSpnIndividualLanceCount()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblLancesPerCompany)
                                .addComponent(getSpnLancesPerCompany())
                                .addComponent(lblLanceSize)
                                .addComponent(getSpnLanceSize()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStarLeagueYear)
                                .addComponent(getSpnStarLeagueYear()))
        );

        return panel;
    }

    private JPanel createPersonnelPanel() {
        // Create Panel Components
        setLblTotalSupportPersonnel(new JLabel());
        updateLblTotalSupportPersonnel(0);
        getLblTotalSupportPersonnel().setToolTipText(resources.getString("lblTotalSupportPersonnel.toolTipText"));
        getLblTotalSupportPersonnel().setName("lblTotalSupportPersonnel");

        final JPanel supportPersonnelNumbersPanel = createSupportPersonnelNumbersPanel();

        setChkPoolAssistants(new JCheckBox(resources.getString("chkPoolAssistants.text")));
        getChkPoolAssistants().setToolTipText(resources.getString("chkPoolAssistants.toolTipText"));
        getChkPoolAssistants().setName("chkPoolAssistants");

        setChkGenerateCaptains(new JCheckBox(resources.getString("chkGenerateCaptains.text")));
        getChkGenerateCaptains().setToolTipText(resources.getString("chkGenerateCaptains.toolTipText"));
        getChkGenerateCaptains().setName("chkGenerateCaptains");

        setChkAssignCompanyCommanderFlag(new JCheckBox(resources.getString("chkAssignCompanyCommanderFlag.text")));
        getChkAssignCompanyCommanderFlag().setToolTipText(resources.getString("chkAssignCompanyCommanderFlag.toolTipText"));
        getChkAssignCompanyCommanderFlag().setName("chkAssignCompanyCommanderFlag");

        setChkApplyOfficerStatBonusToWorstSkill(new JCheckBox(resources.getString("chkApplyOfficerStatBonusToWorstSkill.text")));
        getChkApplyOfficerStatBonusToWorstSkill().setToolTipText(resources.getString("chkApplyOfficerStatBonusToWorstSkill.toolTipText"));
        getChkApplyOfficerStatBonusToWorstSkill().setName("chkApplyOfficerStatBonusToWorstSkill");

        setChkAssignBestCompanyCommander(new JCheckBox(resources.getString("chkAssignBestCompanyCommander.text")));
        getChkAssignBestCompanyCommander().setToolTipText(resources.getString("chkAssignBestCompanyCommander.toolTipText"));
        getChkAssignBestCompanyCommander().setName("chkAssignBestCompanyCommander");
        getChkAssignBestCompanyCommander().addActionListener(evt ->
                getChkPrioritizeCompanyCommanderCombatSkills().setEnabled(getChkAssignBestCompanyCommander().isSelected()));

        setChkPrioritizeCompanyCommanderCombatSkills(new JCheckBox(resources.getString("chkPrioritizeCompanyCommanderCombatSkills.text")));
        getChkPrioritizeCompanyCommanderCombatSkills().setToolTipText(resources.getString("chkPrioritizeCompanyCommanderCombatSkills.toolTipText"));
        getChkPrioritizeCompanyCommanderCombatSkills().setName("chkPrioritizeCompanyCommanderCombatSkills");

        setChkAssignBestOfficers(new JCheckBox(resources.getString("chkAssignBestOfficers.text")));
        getChkAssignBestOfficers().setToolTipText(resources.getString("chkAssignBestOfficers.toolTipText"));
        getChkAssignBestOfficers().setName("chkAssignBestOfficers");
        getChkAssignBestOfficers().addActionListener(evt ->
                getChkPrioritizeOfficerCombatSkills().setEnabled(getChkAssignBestOfficers().isSelected()));

        setChkPrioritizeOfficerCombatSkills(new JCheckBox(resources.getString("chkPrioritizeOfficerCombatSkills.text")));
        getChkPrioritizeOfficerCombatSkills().setToolTipText(resources.getString("chkPrioritizeOfficerCombatSkills.toolTipText"));
        getChkPrioritizeOfficerCombatSkills().setName("chkPrioritizeOfficerCombatSkills");

        setChkAssignMostSkilledToPrimaryLances(new JCheckBox(resources.getString("chkAssignMostSkilledToPrimaryLances.text")));
        getChkAssignMostSkilledToPrimaryLances().setToolTipText(resources.getString("chkAssignMostSkilledToPrimaryLances.toolTipText"));
        getChkAssignMostSkilledToPrimaryLances().setName("chkAssignMostSkilledToPrimaryLances");

        setChkAutomaticallyAssignRanks(new JCheckBox(resources.getString("chkAutomaticallyAssignRanks.text")));
        getChkAutomaticallyAssignRanks().setToolTipText(resources.getString("chkAutomaticallyAssignRanks.toolTipText"));
        getChkAutomaticallyAssignRanks().setName("chkAutomaticallyAssignRanks");

        setChkUseSpecifiedFactionToAssignRanks(new JCheckBox(resources.getString("chkUseSpecifiedFactionToAssignRanks.text")));
        getChkUseSpecifiedFactionToAssignRanks().setToolTipText(resources.getString("chkUseSpecifiedFactionToAssignRanks.toolTipText"));
        getChkUseSpecifiedFactionToAssignRanks().setName("chkUseSpecifiedFactionToAssignRanks");

        setChkAssignMekWarriorsCallsigns(new JCheckBox(resources.getString("chkAssignMekWarriorsCallsigns.text")));
        getChkAssignMekWarriorsCallsigns().setToolTipText(resources.getString("chkAssignMekWarriorsCallsigns.toolTipText"));
        getChkAssignMekWarriorsCallsigns().setName("chkAssignMekWarriorsCallsigns");

        setChkAssignFounderFlag(new JCheckBox(resources.getString("chkAssignFounderFlag.text")));
        getChkAssignFounderFlag().setToolTipText(resources.getString("chkAssignFounderFlag.toolTipText"));
        getChkAssignFounderFlag().setName("chkAssignFounderFlag");

        // Disable Panel Portions by Default
        getChkAssignBestCompanyCommander().setSelected(true);
        getChkAssignBestCompanyCommander().doClick();
        getChkAssignBestOfficers().setSelected(true);
        getChkAssignBestOfficers().doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelPanel.title")));
        panel.setName("personnelPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getLblTotalSupportPersonnel())
                        .addComponent(supportPersonnelNumbersPanel)
                        .addComponent(getChkPoolAssistants())
                        .addComponent(getChkGenerateCaptains())
                        .addComponent(getChkAssignCompanyCommanderFlag())
                        .addComponent(getChkApplyOfficerStatBonusToWorstSkill())
                        .addComponent(getChkAssignBestCompanyCommander())
                        .addComponent(getChkPrioritizeCompanyCommanderCombatSkills())
                        .addComponent(getChkAssignBestOfficers())
                        .addComponent(getChkPrioritizeOfficerCombatSkills())
                        .addComponent(getChkAssignMostSkilledToPrimaryLances())
                        .addComponent(getChkAutomaticallyAssignRanks())
                        .addComponent(getChkUseSpecifiedFactionToAssignRanks())
                        .addComponent(getChkAssignMekWarriorsCallsigns())
                        .addComponent(getChkAssignFounderFlag())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getLblTotalSupportPersonnel())
                        .addComponent(supportPersonnelNumbersPanel)
                        .addComponent(getChkPoolAssistants())
                        .addComponent(getChkGenerateCaptains())
                        .addComponent(getChkAssignCompanyCommanderFlag())
                        .addComponent(getChkApplyOfficerStatBonusToWorstSkill())
                        .addComponent(getChkAssignBestCompanyCommander())
                        .addComponent(getChkPrioritizeCompanyCommanderCombatSkills())
                        .addComponent(getChkAssignBestOfficers())
                        .addComponent(getChkPrioritizeOfficerCombatSkills())
                        .addComponent(getChkAssignMostSkilledToPrimaryLances())
                        .addComponent(getChkAutomaticallyAssignRanks())
                        .addComponent(getChkUseSpecifiedFactionToAssignRanks())
                        .addComponent(getChkAssignMekWarriorsCallsigns())
                        .addComponent(getChkAssignFounderFlag())
        );

        return panel;
    }

    private JPanel createSupportPersonnelNumbersPanel() {
        final PersonnelRole[] personnelRoles = new PersonnelRole[] {
                PersonnelRole.MEK_TECH, PersonnelRole.MECHANIC, PersonnelRole.AERO_TECH,
                PersonnelRole.BA_TECH, PersonnelRole.DOCTOR, PersonnelRole.ADMINISTRATOR_COMMAND,
                PersonnelRole.ADMINISTRATOR_LOGISTICS, PersonnelRole.ADMINISTRATOR_TRANSPORT, PersonnelRole.ADMINISTRATOR_HR
        };

        // Create Panel Components
        setSpnSupportPersonnelNumbers(new HashMap<>());
        final Map<PersonnelRole, JLabel> labels = new HashMap<>();
        for (final PersonnelRole role : personnelRoles) {
            final String name = role.getName(getCampaign().getFaction().isClan());
            final String toolTipText = String.format(resources.getString("supportPersonnelNumber.toolTipText"), name);

            labels.put(role, new JLabel(name));
            labels.get(role).setToolTipText(toolTipText);
            labels.get(role).setName("lbl" + role.name());

            getSpnSupportPersonnelNumbers().put(role, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
            getSpnSupportPersonnelNumbers().get(role).setToolTipText(toolTipText);
            getSpnSupportPersonnelNumbers().get(role).setName("spn" + role.name());

            // Programmatically Assign Accessibility Labels
            labels.get(role).setLabelFor(getSpnSupportPersonnelNumbers().get(role));
        }

        // Layout the UI
        final JPanel panel = new JPanel(new GridLayout(0, 3));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("supportPersonnelNumbersPanel.title")));
        panel.setName("supportPersonnelNumbersPanel");

        // This puts the label above the spinner, separated into three columns. From the
        // personnelRoles array declaration, the i tracks the line and the j tracks the
        for (int i = 0; i < (personnelRoles.length / 3); i++) {
            for (int j = 0; j < 3; j++) {
                panel.add(labels.get(personnelRoles[j + (3 * i)]));
            }

            for (int j = 0; j < 3; j++) {
                panel.add(getSpnSupportPersonnelNumbers().get(personnelRoles[j + (3 * i)]));
            }
        }

        return panel;
    }

    private JPanel createPersonnelRandomizationPanel() {
        setRandomOriginOptionsPanel(new RandomOriginOptionsPanel(getFrame(), getCampaign(),
                getCampaign().getFaction()));
        return getRandomOriginOptionsPanel();
    }

    private JPanel createStartingSimulationPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblSimulationDuration = new JLabel();

        // Create Panel Components
        setChkRunStartingSimulation(new JCheckBox(resources.getString("chkRunStartingSimulation.text")));
        getChkRunStartingSimulation().setToolTipText(resources.getString("chkRunStartingSimulation.toolTipText"));
        getChkRunStartingSimulation().setName("chkRunStartingSimulation");
        getChkRunStartingSimulation().addActionListener(evt -> {
            final boolean selected = getChkRunStartingSimulation().isSelected();
            lblSimulationDuration.setEnabled(selected);
            getSpnSimulationDuration().setEnabled(selected);
            getChkSimulateRandomMarriages().setEnabled(selected);
            getChkSimulateRandomProcreation().setEnabled(selected);
        });

        lblSimulationDuration.setText(resources.getString("lblSimulationDuration.text"));
        lblSimulationDuration.setToolTipText(resources.getString("lblSimulationDuration.toolTipText"));
        lblSimulationDuration.setName("lblSimulationDuration");

        setSpnSimulationDuration(new JSpinner(new SpinnerNumberModel(0, 0, 25, 1)));
        getSpnSimulationDuration().setToolTipText(resources.getString("lblSimulationDuration.toolTipText"));
        getSpnSimulationDuration().setName("spnSimulationDuration");

        setChkSimulateRandomMarriages(new JCheckBox(resources.getString("chkSimulateRandomMarriages.text")));
        getChkSimulateRandomMarriages().setToolTipText(resources.getString("chkSimulateRandomMarriages.toolTipText"));
        getChkSimulateRandomMarriages().setName("chkSimulateRandomMarriages");

        setChkSimulateRandomProcreation(new JCheckBox(resources.getString("chkSimulateRandomProcreation.text")));
        getChkSimulateRandomProcreation().setToolTipText(resources.getString("chkSimulateRandomProcreation.toolTipText"));
        getChkSimulateRandomProcreation().setName("chkSimulateRandomProcreation");

        // Programmatically Assign Accessibility Labels
        lblSimulationDuration.setLabelFor(getSpnSimulationDuration());

        // Disable Panel Portions by Default
        getChkRunStartingSimulation().setSelected(true);
        getChkRunStartingSimulation().doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("startingSimulationPanel.title")));
        panel.setName("startingSimulationPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkRunStartingSimulation())
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblSimulationDuration)
                                .addComponent(getSpnSimulationDuration(), Alignment.LEADING))
                        .addComponent(getChkSimulateRandomMarriages())
                        .addComponent(getChkSimulateRandomProcreation())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getChkRunStartingSimulation())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblSimulationDuration)
                                .addComponent(getSpnSimulationDuration()))
                        .addComponent(getChkSimulateRandomMarriages())
                        .addComponent(getChkSimulateRandomProcreation())
        );

        return panel;
    }

    private JPanel createUnitsPanel() {
        // Create Panel Components
        final JLabel lblBattleMekFactionGenerationMethod = new JLabel(resources.getString("lblBattleMekFactionGenerationMethod.text"));
        lblBattleMekFactionGenerationMethod.setToolTipText(resources.getString("lblBattleMekFactionGenerationMethod.toolTipText"));
        lblBattleMekFactionGenerationMethod.setName("lblBattleMekFactionGenerationMethod");

        setComboBattleMekFactionGenerationMethod(new MMComboBox<>("comboBattleMekFactionGenerationMethod", BattleMekFactionGenerationMethod.values()));
        getComboBattleMekFactionGenerationMethod().setToolTipText(resources.getString("lblBattleMekFactionGenerationMethod.toolTipText"));
        getComboBattleMekFactionGenerationMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BattleMekFactionGenerationMethod) {
                    list.setToolTipText(((BattleMekFactionGenerationMethod) value).getToolTipText());
                }
                return this;
            }
        });

        final JLabel lblBattleMekWeightClassGenerationMethod = new JLabel(resources.getString("lblBattleMekWeightClassGenerationMethod.text"));
        lblBattleMekWeightClassGenerationMethod.setToolTipText(resources.getString("lblBattleMekWeightClassGenerationMethod.toolTipText"));
        lblBattleMekWeightClassGenerationMethod.setName("lblBattleMekWeightClassGenerationMethod");

        setComboBattleMekWeightClassGenerationMethod(new MMComboBox<>("comboBattleMekWeightClassGenerationMethod", BattleMekWeightClassGenerationMethod.values()));
        getComboBattleMekWeightClassGenerationMethod().setToolTipText(resources.getString("lblBattleMekWeightClassGenerationMethod.toolTipText"));
        getComboBattleMekWeightClassGenerationMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BattleMekWeightClassGenerationMethod) {
                    list.setToolTipText(((BattleMekWeightClassGenerationMethod) value).getToolTipText());
                }
                return this;
            }
        });

        final JLabel lblBattleMekQualityGenerationMethod = new JLabel(resources.getString("lblBattleMekQualityGenerationMethod.text"));
        lblBattleMekQualityGenerationMethod.setToolTipText(resources.getString("lblBattleMekQualityGenerationMethod.toolTipText"));
        lblBattleMekQualityGenerationMethod.setName("lblBattleMekQualityGenerationMethod");

        setComboBattleMekQualityGenerationMethod(new MMComboBox<>("comboBattleMekQualityGenerationMethod", BattleMekQualityGenerationMethod.values()));
        getComboBattleMekQualityGenerationMethod().setToolTipText(resources.getString("lblBattleMekQualityGenerationMethod.toolTipText"));
        getComboBattleMekQualityGenerationMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof BattleMekQualityGenerationMethod) {
                    list.setToolTipText(((BattleMekQualityGenerationMethod) value).getToolTipText());
                }
                return this;
            }
        });

        setChkNeverGenerateStarLeagueMeks(new JCheckBox(resources.getString("chkNeverGenerateStarLeagueMeks.text")));
        getChkNeverGenerateStarLeagueMeks().setToolTipText(resources.getString("chkNeverGenerateStarLeagueMeks.toolTipText"));
        getChkNeverGenerateStarLeagueMeks().setName("chkNeverGenerateStarLeagueMeks");
        getChkNeverGenerateStarLeagueMeks().addActionListener(evt ->
                getChkOnlyGenerateStarLeagueMeks().setEnabled(!getChkNeverGenerateStarLeagueMeks().isSelected()));

        setChkOnlyGenerateStarLeagueMeks(new JCheckBox(resources.getString("chkOnlyGenerateStarLeagueMeks.text")));
        getChkOnlyGenerateStarLeagueMeks().setToolTipText(resources.getString("chkOnlyGenerateStarLeagueMeks.toolTipText"));
        getChkOnlyGenerateStarLeagueMeks().setName("chkOnlyGenerateStarLeagueMeks");
        getChkOnlyGenerateStarLeagueMeks().addActionListener(evt ->
                getChkNeverGenerateStarLeagueMeks().setEnabled(!getChkOnlyGenerateStarLeagueMeks().isSelected()));

        setChkOnlyGenerateOmniMeks(new JCheckBox(resources.getString("chkOnlyGenerateOmniMeks.text")));
        getChkOnlyGenerateOmniMeks().setToolTipText(resources.getString("chkOnlyGenerateOmniMeks.toolTipText"));
        getChkOnlyGenerateOmniMeks().setName("chkOnlyGenerateOmniMeks");

        setChkGenerateUnitsAsAttached(new JCheckBox(resources.getString("chkGenerateUnitsAsAttached.text")));
        getChkGenerateUnitsAsAttached().setToolTipText(resources.getString("chkGenerateUnitsAsAttached.toolTipText"));
        getChkGenerateUnitsAsAttached().setName("chkGenerateUnitsAsAttached");

        setChkAssignBestRollToCompanyCommander(new JCheckBox(resources.getString("chkAssignBestRollToCompanyCommander.text")));
        getChkAssignBestRollToCompanyCommander().setToolTipText(resources.getString("chkAssignBestRollToCompanyCommander.toolTipText"));
        getChkAssignBestRollToCompanyCommander().setName("chkAssignBestRollToCompanyCommander");

        setChkSortStarLeagueUnitsFirst(new JCheckBox(resources.getString("chkSortStarLeagueUnitsFirst.text")));
        getChkSortStarLeagueUnitsFirst().setToolTipText(resources.getString("chkSortStarLeagueUnitsFirst.toolTipText"));
        getChkSortStarLeagueUnitsFirst().setName("chkSortStarLeagueUnitsFirst");

        setChkGroupByWeight(new JCheckBox(resources.getString("chkGroupByWeight.text")));
        getChkGroupByWeight().setToolTipText(resources.getString("chkGroupByWeight.toolTipText"));
        getChkGroupByWeight().setName("chkGroupByWeight");

        setChkGroupByQuality(new JCheckBox(resources.getString("chkGroupByQuality.text")));
        getChkGroupByQuality().setToolTipText(resources.getString("chkGroupByQuality.toolTipText"));
        getChkGroupByQuality().setName("chkGroupByQuality");

        setChkKeepOfficerRollsSeparate(new JCheckBox(resources.getString("chkKeepOfficerRollsSeparate.text")));
        getChkKeepOfficerRollsSeparate().setToolTipText(resources.getString("chkKeepOfficerRollsSeparate.toolTipText"));
        getChkKeepOfficerRollsSeparate().setName("chkKeepOfficerRollsSeparate");

        setChkAssignTechsToUnits(new JCheckBox(resources.getString("chkAssignTechsToUnits.text")));
        getChkAssignTechsToUnits().setToolTipText(resources.getString("chkAssignTechsToUnits.toolTipText"));
        getChkAssignTechsToUnits().setName("chkAssignTechsToUnits");

        // Programmatically Assign Accessibility Labels
        lblBattleMekFactionGenerationMethod.setLabelFor(getComboBattleMekFactionGenerationMethod());
        lblBattleMekWeightClassGenerationMethod.setLabelFor(getComboBattleMekWeightClassGenerationMethod());
        lblBattleMekQualityGenerationMethod.setLabelFor(getComboBattleMekQualityGenerationMethod());

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("unitsPanel.title")));
        panel.setName("unitsPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblBattleMekFactionGenerationMethod)
                                .addComponent(getComboBattleMekFactionGenerationMethod(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblBattleMekWeightClassGenerationMethod)
                                .addComponent(getComboBattleMekWeightClassGenerationMethod(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblBattleMekQualityGenerationMethod)
                                .addComponent(getComboBattleMekQualityGenerationMethod(), Alignment.LEADING))
                        .addComponent(getChkNeverGenerateStarLeagueMeks())
                        .addComponent(getChkOnlyGenerateStarLeagueMeks())
                        .addComponent(getChkOnlyGenerateOmniMeks())
                        .addComponent(getChkGenerateUnitsAsAttached())
                        .addComponent(getChkAssignBestRollToCompanyCommander())
                        .addComponent(getChkSortStarLeagueUnitsFirst())
                        .addComponent(getChkGroupByWeight())
                        .addComponent(getChkGroupByQuality())
                        .addComponent(getChkKeepOfficerRollsSeparate())
                        .addComponent(getChkAssignTechsToUnits())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblBattleMekFactionGenerationMethod)
                                .addComponent(getComboBattleMekFactionGenerationMethod()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblBattleMekWeightClassGenerationMethod)
                                .addComponent(getComboBattleMekWeightClassGenerationMethod()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblBattleMekQualityGenerationMethod)
                                .addComponent(getComboBattleMekQualityGenerationMethod()))
                        .addComponent(getChkNeverGenerateStarLeagueMeks())
                        .addComponent(getChkOnlyGenerateStarLeagueMeks())
                        .addComponent(getChkOnlyGenerateOmniMeks())
                        .addComponent(getChkGenerateUnitsAsAttached())
                        .addComponent(getChkAssignBestRollToCompanyCommander())
                        .addComponent(getChkSortStarLeagueUnitsFirst())
                        .addComponent(getChkGroupByWeight())
                        .addComponent(getChkGroupByQuality())
                        .addComponent(getChkKeepOfficerRollsSeparate())
                        .addComponent(getChkAssignTechsToUnits())
        );

        return panel;
    }

    private JPanel createUnitPanel() {
        // Initialize Components Used in ActionListeners
        final JPanel forceWeightLimitsPanel = new JDisableablePanel("forceWeightLimitsPanel");

        // Create Panel Components
        final JLabel lblForceNamingMethod = new JLabel(resources.getString("lblForceNamingMethod.text"));
        lblForceNamingMethod.setToolTipText(resources.getString("lblForceNamingMethod.toolTipText"));
        lblForceNamingMethod.setName("lblForceNamingMethod");

        setComboForceNamingMethod(new MMComboBox<>("comboForceNamingMethod", ForceNamingMethod.values()));
        getComboForceNamingMethod().setToolTipText(resources.getString("lblForceNamingMethod.toolTipText"));
        getComboForceNamingMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ForceNamingMethod) {
                    list.setToolTipText(((ForceNamingMethod) value).getToolTipText());
                }
                return this;
            }
        });

        setChkGenerateForceIcons(new JCheckBox(resources.getString("chkGenerateForceIcons.text")));
        getChkGenerateForceIcons().setToolTipText(resources.getString("chkGenerateForceIcons.toolTipText"));
        getChkGenerateForceIcons().setName("chkGenerateForceIcons");
        getChkGenerateForceIcons().addActionListener(evt -> {
            final boolean selected = getChkGenerateForceIcons().isSelected();
            getChkUseSpecifiedFactionToGenerateForceIcons().setEnabled(selected);
            getChkGenerateOriginNodeForceIcon().setEnabled(selected);
            getChkUseOriginNodeForceIconLogo().setEnabled(selected
                    && getChkGenerateOriginNodeForceIcon().isSelected());
            forceWeightLimitsPanel.setEnabled(selected);
        });

        setChkUseSpecifiedFactionToGenerateForceIcons(new JCheckBox(resources.getString("chkUseSpecifiedFactionToGenerateForceIcons.text")));
        getChkUseSpecifiedFactionToGenerateForceIcons().setToolTipText(resources.getString("chkUseSpecifiedFactionToGenerateForceIcons.toolTipText"));
        getChkUseSpecifiedFactionToGenerateForceIcons().setName("chkUseSpecifiedFactionToGenerateForceIcons");

        setChkGenerateOriginNodeForceIcon(new JCheckBox(resources.getString("chkGenerateOriginNodeForceIcon.text")));
        getChkGenerateOriginNodeForceIcon().setToolTipText(resources.getString("chkGenerateOriginNodeForceIcon.toolTipText"));
        getChkGenerateOriginNodeForceIcon().setName("chkGenerateOriginNodeForceIcon");
        getChkGenerateOriginNodeForceIcon().addActionListener(evt -> getChkUseOriginNodeForceIconLogo()
                .setEnabled(getChkGenerateOriginNodeForceIcon().isEnabled()
                        && getChkGenerateOriginNodeForceIcon().isSelected()));

        setChkUseOriginNodeForceIconLogo(new JCheckBox(resources.getString("chkUseOriginNodeForceIconLogo.text")));
        getChkUseOriginNodeForceIconLogo().setToolTipText(resources.getString("chkUseOriginNodeForceIconLogo.toolTipText"));
        getChkUseOriginNodeForceIconLogo().setName("chkUseOriginNodeForceIconLogo");

        createForceWeightLimitsPanel(forceWeightLimitsPanel);

        // Programmatically Assign Accessibility Labels
        lblForceNamingMethod.setLabelFor(getComboForceNamingMethod());

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("unitPanel.title")));
        panel.setName("unitPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblForceNamingMethod)
                                .addComponent(getComboForceNamingMethod(), Alignment.LEADING))
                        .addComponent(getChkGenerateForceIcons())
                        .addComponent(getChkUseSpecifiedFactionToGenerateForceIcons())
                        .addComponent(getChkGenerateOriginNodeForceIcon())
                        .addComponent(getChkUseOriginNodeForceIconLogo())
                        .addComponent(forceWeightLimitsPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblForceNamingMethod)
                                .addComponent(getComboForceNamingMethod()))
                        .addComponent(getChkGenerateForceIcons())
                        .addComponent(getChkUseSpecifiedFactionToGenerateForceIcons())
                        .addComponent(getChkGenerateOriginNodeForceIcon())
                        .addComponent(getChkUseOriginNodeForceIconLogo())
                        .addComponent(forceWeightLimitsPanel)
        );
        return panel;
    }

    private void createForceWeightLimitsPanel(final JPanel panel) {
        // Create Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("forceWeightLimitsPanel.title")));
        panel.setToolTipText(resources.getString("forceWeightLimitsPanel.toolTipText"));
        panel.setLayout(new GridLayout(0, 2));

        // Create Panel Components
        setSpnForceWeightLimits(new HashMap<>());
        for (int i = EntityWeightClass.WEIGHT_ULTRA_LIGHT; i <= EntityWeightClass.WEIGHT_ASSAULT; i++) {
            final String weightClass = EntityWeightClass.getClassName(i);

            final JLabel label = new JLabel(weightClass);
            label.setToolTipText(resources.getString("forceWeightLimitsPanel.toolTipText"));
            label.setName("lbl" + weightClass);
            panel.add(label);

            getSpnForceWeightLimits().put(i, new JSpinner(new SpinnerNumberModel(0, 0, 10000, 10)));
            getSpnForceWeightLimits().get(i).setToolTipText(resources.getString("forceWeightLimitsPanel.toolTipText"));
            getSpnForceWeightLimits().get(i).setName("spn" + weightClass);
            panel.add(getSpnForceWeightLimits().get(i));
        }
    }

    private JPanel createSparesPanel() {
        // Initialize Labels Used in ActionListeners
        final JLabel lblSparesPercentOfActiveUnits = new JLabel();
        final JLabel lblNumberReloadsPerWeapon = new JLabel();

        // Create Panel Components
        setChkGenerateMothballedSpareUnits(new JCheckBox(resources.getString("chkGenerateMothballedSpareUnits.text")));
        getChkGenerateMothballedSpareUnits().setToolTipText(resources.getString("chkGenerateMothballedSpareUnits.toolTipText"));
        getChkGenerateMothballedSpareUnits().setName("chkGenerateMothballedSpareUnits");
        getChkGenerateMothballedSpareUnits().addActionListener(evt -> {
            final boolean selected = getChkGenerateMothballedSpareUnits().isSelected();
            lblSparesPercentOfActiveUnits.setEnabled(selected);
            getSpnSparesPercentOfActiveUnits().setEnabled(selected);
        });

        lblSparesPercentOfActiveUnits.setText(resources.getString("lblSparesPercentOfActiveUnits.text"));
        lblSparesPercentOfActiveUnits.setToolTipText(resources.getString("lblSparesPercentOfActiveUnits.toolTipText"));
        lblSparesPercentOfActiveUnits.setName("lblSparesPercentOfActiveUnits");

        setSpnSparesPercentOfActiveUnits(new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        getSpnSparesPercentOfActiveUnits().setToolTipText(resources.getString("chkGenerateMothballedSpareUnits.toolTipText"));
        getSpnSparesPercentOfActiveUnits().setName("spnGenerateMothballedSpareUnits");

        final JLabel lblPartGenerationMethod = new JLabel(resources.getString("lblPartGenerationMethod.text"));
        lblPartGenerationMethod.setToolTipText(resources.getString("lblPartGenerationMethod.toolTipText"));
        lblPartGenerationMethod.setName("lblPartGenerationMethod");

        setComboPartGenerationMethod(new MMComboBox<>("comboPartGenerationMethod", PartGenerationMethod.values()));
        getComboPartGenerationMethod().setToolTipText(resources.getString("lblPartGenerationMethod.toolTipText"));
        getComboPartGenerationMethod().setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PartGenerationMethod) {
                    list.setToolTipText(((PartGenerationMethod) value).getToolTipText());
                }
                return this;
            }
        });

        final JLabel lblStartingArmourWeight = new JLabel(resources.getString("lblStartingArmourWeight.text"));
        lblStartingArmourWeight.setToolTipText(resources.getString("lblStartingArmourWeight.toolTipText"));
        lblStartingArmourWeight.setName("lblStartingArmourWeight");

        setSpnStartingArmourWeight(new JSpinner(new SpinnerNumberModel(0, 0, 500, 1)));
        getSpnStartingArmourWeight().setToolTipText(resources.getString("lblStartingArmourWeight.toolTipText"));
        getSpnStartingArmourWeight().setName("spnStartingArmourWeight");

        setChkGenerateSpareAmmunition(new JCheckBox(resources.getString("chkGenerateSpareAmmunition.text")));
        getChkGenerateSpareAmmunition().setToolTipText(resources.getString("chkGenerateSpareAmmunition.toolTipText"));
        getChkGenerateSpareAmmunition().setName("chkGenerateSpareAmmunition");
        getChkGenerateSpareAmmunition().addActionListener(evt -> {
            final boolean selected = getChkGenerateSpareAmmunition().isSelected();
            lblNumberReloadsPerWeapon.setEnabled(selected);
            getSpnNumberReloadsPerWeapon().setEnabled(selected);
            getChkGenerateFractionalMachineGunAmmunition().setEnabled(selected);
        });

        lblNumberReloadsPerWeapon.setText(resources.getString("lblNumberReloadsPerWeapon.text"));
        lblNumberReloadsPerWeapon.setToolTipText(resources.getString("lblNumberReloadsPerWeapon.toolTipText"));
        lblNumberReloadsPerWeapon.setName("lblNumberReloadsPerWeapon");

        setSpnNumberReloadsPerWeapon(new JSpinner(new SpinnerNumberModel(0, 0, 25, 1)));
        getSpnNumberReloadsPerWeapon().setToolTipText(resources.getString("lblNumberReloadsPerWeapon.toolTipText"));
        getSpnNumberReloadsPerWeapon().setName("spnNumberReloadsPerWeapon");

        setChkGenerateFractionalMachineGunAmmunition(new JCheckBox(resources.getString("chkGenerateFractionalMachineGunAmmunition.text")));
        getChkGenerateFractionalMachineGunAmmunition().setToolTipText(resources.getString("chkGenerateFractionalMachineGunAmmunition.toolTipText"));
        getChkGenerateFractionalMachineGunAmmunition().setName("chkGenerateFractionalMachineGunAmmunition");

        // Programmatically Assign Accessibility Labels
        lblSparesPercentOfActiveUnits.setLabelFor(getSpnSparesPercentOfActiveUnits());
        lblPartGenerationMethod.setLabelFor(getComboPartGenerationMethod());
        lblStartingArmourWeight.setLabelFor(getSpnStartingArmourWeight());
        lblNumberReloadsPerWeapon.setLabelFor(getSpnNumberReloadsPerWeapon());

        // Disable Panel Portions by Default
        getChkGenerateMothballedSpareUnits().setSelected(true);
        getChkGenerateMothballedSpareUnits().doClick();
        getChkGenerateSpareAmmunition().setSelected(true);
        getChkGenerateSpareAmmunition().doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("sparesPanel.title")));
        panel.setName("sparesPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkGenerateMothballedSpareUnits())
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblSparesPercentOfActiveUnits)
                                .addComponent(getSpnSparesPercentOfActiveUnits(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblPartGenerationMethod)
                                .addComponent(getComboPartGenerationMethod(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStartingArmourWeight)
                                .addComponent(getSpnStartingArmourWeight(), Alignment.LEADING))
                        .addComponent(getChkGenerateSpareAmmunition())
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblNumberReloadsPerWeapon)
                                .addComponent(getSpnNumberReloadsPerWeapon(), Alignment.LEADING))
                        .addComponent(getChkGenerateFractionalMachineGunAmmunition())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getChkGenerateMothballedSpareUnits())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblSparesPercentOfActiveUnits)
                                .addComponent(getSpnSparesPercentOfActiveUnits()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblPartGenerationMethod)
                                .addComponent(getComboPartGenerationMethod()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartingArmourWeight)
                                .addComponent(getSpnStartingArmourWeight()))
                        .addComponent(getChkGenerateSpareAmmunition())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblNumberReloadsPerWeapon)
                                .addComponent(getSpnNumberReloadsPerWeapon()))
                        .addComponent(getChkGenerateFractionalMachineGunAmmunition())
        );

        return panel;
    }

    private JPanel createContractsPanel() {
        // Create Panel Components
        setChkSelectStartingContract(new JCheckBox(resources.getString("chkSelectStartingContract.text")));
        getChkSelectStartingContract().setToolTipText(resources.getString("chkSelectStartingContract.toolTipText"));
        getChkSelectStartingContract().setName("chkSelectStartingContract");
        getChkSelectStartingContract().addActionListener(evt -> {
            final boolean selected = getChkSelectStartingContract().isSelected();
            getChkStartCourseToContractPlanet().setEnabled(selected);
            if (getChkIncludeInitialContractPayment() != null) {
                getChkIncludeInitialContractPayment().setEnabled(selected);
            }
        });

        setChkStartCourseToContractPlanet(new JCheckBox(resources.getString("chkStartCourseToContractPlanet.text")));
        getChkStartCourseToContractPlanet().setToolTipText(resources.getString("chkStartCourseToContractPlanet.toolTipText"));
        getChkStartCourseToContractPlanet().setName("chkStartCourseToContractPlanet");

        // Disable Panel by Default
        getChkSelectStartingContract().setSelected(true);
        getChkSelectStartingContract().doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("contractsPanel.title")));
        panel.setName("contractsPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkSelectStartingContract())
                        .addComponent(getChkStartCourseToContractPlanet())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getChkSelectStartingContract())
                        .addComponent(getChkStartCourseToContractPlanet())
        );

        // TODO : Wave 5 : Company Generation GUI
        panel.setEnabled(false);
        getChkSelectStartingContract().setEnabled(false);
        getChkStartCourseToContractPlanet().setEnabled(false);

        return panel;
    }

    private JPanel createFinancesPanel() {
        // Initialize Components Used in ActionListeners
        final JPanel financialCreditsPanel = new JDisableablePanel("financialCreditsPanel");
        final JPanel financialDebitsPanel = new JDisableablePanel("financialDebitsPanel");

        // Create Panel Components
        setChkProcessFinances(new JCheckBox(resources.getString("chkProcessFinances.text")));
        getChkProcessFinances().setToolTipText(resources.getString("chkProcessFinances.toolTipText"));
        getChkProcessFinances().setName("chkProcessFinances");
        getChkProcessFinances().addActionListener(evt -> {
            final boolean selected = getChkProcessFinances().isSelected();
            financialCreditsPanel.setEnabled(selected);
            financialDebitsPanel.setEnabled(selected);

            if (selected) {
                getChkRandomizeStartingCash().setSelected(!getChkRandomizeStartingCash().isSelected());
                getChkRandomizeStartingCash().doClick();

                getChkIncludeInitialContractPayment().setEnabled(getChkSelectStartingContract().isSelected());

                getChkPayForSetup().setSelected(!getChkPayForSetup().isSelected());
                getChkPayForSetup().doClick();
            }
        });

        createFinancialCreditsPanel(financialCreditsPanel);

        createFinancialDebitsPanel(financialDebitsPanel);

        // Disable Panel Portions by Default
        getChkProcessFinances().setSelected(true);
        getChkProcessFinances().doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("financesPanel.title")));
        panel.setName("financesPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkProcessFinances())
                        .addComponent(financialCreditsPanel)
                        .addComponent(financialDebitsPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getChkProcessFinances())
                        .addComponent(financialCreditsPanel)
                        .addComponent(financialDebitsPanel)
        );

        return panel;
    }

    private void createFinancialCreditsPanel(final JPanel panel) {
        // Initialize Components Used in ActionListeners
        final JLabel lblRandomStartingCashDiceCount = new JLabel();

        // Create Panel Components
        final JLabel lblStartingCash = new JLabel(resources.getString("lblStartingCash.text"));
        lblStartingCash.setToolTipText(resources.getString("lblStartingCash.toolTipText"));
        lblStartingCash.setName("lblStartingCash");

        setSpnStartingCash(new JSpinner(new SpinnerNumberModel(0, 0, 200000000, 100000)));
        getSpnStartingCash().setToolTipText(resources.getString("lblStartingCash.toolTipText"));
        getSpnStartingCash().setName("spnStartingCash");

        setChkRandomizeStartingCash(new JCheckBox(resources.getString("chkRandomizeStartingCash.text")));
        getChkRandomizeStartingCash().setToolTipText(resources.getString("chkRandomizeStartingCash.toolTipText"));
        getChkRandomizeStartingCash().setName("chkRandomizeStartingCash");
        getChkRandomizeStartingCash().addActionListener(evt -> {
            final boolean selected = getChkRandomizeStartingCash().isSelected();
            lblStartingCash.setEnabled(!selected);
            getSpnStartingCash().setEnabled(!selected);
            lblRandomStartingCashDiceCount.setEnabled(selected);
            getSpnRandomStartingCashDiceCount().setEnabled(selected);
        });

        lblRandomStartingCashDiceCount.setText(resources.getString("lblRandomStartingCashDiceCount.text"));
        lblRandomStartingCashDiceCount.setToolTipText(resources.getString("lblRandomStartingCashDiceCount.toolTipText"));
        lblRandomStartingCashDiceCount.setName("lblRandomStartingCashDiceCount");

        setSpnRandomStartingCashDiceCount(new JSpinner(new SpinnerNumberModel(8, 1, 100, 1)));
        getSpnRandomStartingCashDiceCount().setToolTipText(resources.getString("lblRandomStartingCashDiceCount.toolTipText"));
        getSpnRandomStartingCashDiceCount().setName("spnRandomStartingCashDiceCount");

        final JLabel lblMinimumStartingFloat = new JLabel(resources.getString("lblMinimumStartingFloat.text"));
        lblMinimumStartingFloat.setToolTipText(resources.getString("lblMinimumStartingFloat.toolTipText"));
        lblMinimumStartingFloat.setName("lblMinimumStartingFloat");

        setSpnMinimumStartingFloat(new JSpinner(new SpinnerNumberModel(0, 0, 10000000, 100000)));
        getSpnMinimumStartingFloat().setToolTipText(resources.getString("lblMinimumStartingFloat.toolTipText"));
        getSpnMinimumStartingFloat().setName("spnMinimumStartingFloat");

        setChkIncludeInitialContractPayment(new JCheckBox(resources.getString("chkIncludeInitialContractPayment.text")));
        getChkIncludeInitialContractPayment().setToolTipText(resources.getString("chkIncludeInitialContractPayment.toolTipText"));
        getChkIncludeInitialContractPayment().setName("chkIncludeInitialContractPayment");

        setChkStartingLoan(new JCheckBox(resources.getString("chkStartingLoan.text")));
        getChkStartingLoan().setToolTipText(resources.getString("chkStartingLoan.toolTipText"));
        getChkStartingLoan().setName("chkStartingLoan");

        // Programmatically Assign Accessibility Labels
        lblStartingCash.setLabelFor(getSpnStartingCash());
        lblRandomStartingCashDiceCount.setLabelFor(getSpnRandomStartingCashDiceCount());
        lblMinimumStartingFloat.setLabelFor(getSpnMinimumStartingFloat());

        // Disable Panel Portions by Default
        // This is handled by createFinancesPanel

        // Layout the UI
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("financialCreditsPanel.title")));
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblStartingCash)
                                .addComponent(getSpnStartingCash(), Alignment.LEADING))
                        .addComponent(getChkRandomizeStartingCash())
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblRandomStartingCashDiceCount)
                                .addComponent(getSpnRandomStartingCashDiceCount(), Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                                .addComponent(lblMinimumStartingFloat)
                                .addComponent(getSpnMinimumStartingFloat(), Alignment.LEADING))
                        .addComponent(getChkIncludeInitialContractPayment())
                        .addComponent(getChkStartingLoan())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartingCash)
                                .addComponent(getSpnStartingCash()))
                        .addComponent(getChkRandomizeStartingCash())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblRandomStartingCashDiceCount)
                                .addComponent(getSpnRandomStartingCashDiceCount()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMinimumStartingFloat)
                                .addComponent(getSpnMinimumStartingFloat()))
                        .addComponent(getChkIncludeInitialContractPayment())
                        .addComponent(getChkStartingLoan())
        );
    }

    private void createFinancialDebitsPanel(final JPanel panel) {
        // Create Panel Components
        setChkPayForSetup(new JCheckBox(resources.getString("chkPayForSetup.text")));
        getChkPayForSetup().setToolTipText(resources.getString("chkPayForSetup.toolTipText"));
        getChkPayForSetup().setName("chkPayForSetup");
        getChkPayForSetup().addActionListener(evt -> {
            final boolean enabled = getChkPayForSetup().isEnabled() && getChkPayForSetup().isSelected();
            getChkPayForPersonnel().setEnabled(enabled);
            getChkPayForUnits().setEnabled(enabled);
            getChkPayForParts().setEnabled(enabled);
            getChkPayForArmour().setEnabled(enabled);
            getChkPayForAmmunition().setEnabled(enabled);
        });

        setChkPayForPersonnel(new JCheckBox(resources.getString("chkPayForPersonnel.text")));
        getChkPayForPersonnel().setToolTipText(resources.getString("chkPayForPersonnel.toolTipText"));
        getChkPayForPersonnel().setName("chkPayForPersonnel");

        setChkPayForUnits(new JCheckBox(resources.getString("chkPayForUnits.text")));
        getChkPayForUnits().setToolTipText(resources.getString("chkPayForUnits.toolTipText"));
        getChkPayForUnits().setName("chkPayForUnits");

        setChkPayForParts(new JCheckBox(resources.getString("chkPayForParts.text")));
        getChkPayForParts().setToolTipText(resources.getString("chkPayForParts.toolTipText"));
        getChkPayForParts().setName("chkPayForParts");

        setChkPayForArmour(new JCheckBox(resources.getString("chkPayForArmour.text")));
        getChkPayForArmour().setToolTipText(resources.getString("chkPayForArmour.toolTipText"));
        getChkPayForArmour().setName("chkPayForArmour");

        setChkPayForAmmunition(new JCheckBox(resources.getString("chkPayForAmmunition.text")));
        getChkPayForAmmunition().setToolTipText(resources.getString("chkPayForAmmunition.toolTipText"));
        getChkPayForAmmunition().setName("chkPayForAmmunition");

        // Disable Panel Portions by Default
        // This is handled by createFinancesPanel

        // Layout the UI
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("financialDebitsPanel.title")));
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkPayForSetup())
                        .addComponent(getChkPayForPersonnel())
                        .addComponent(getChkPayForUnits())
                        .addComponent(getChkPayForParts())
                        .addComponent(getChkPayForArmour())
                        .addComponent(getChkPayForAmmunition())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getChkPayForSetup())
                        .addComponent(getChkPayForPersonnel())
                        .addComponent(getChkPayForUnits())
                        .addComponent(getChkPayForParts())
                        .addComponent(getChkPayForArmour())
                        .addComponent(getChkPayForAmmunition())
        );
    }

    private JPanel createSurprisesPanel() {
        // Initialize Components Used in ActionListeners
        final JPanel mysteryBoxPanel = new JDisableablePanel("mysteryBoxPanel");

        // Create Panel Components
        setChkGenerateSurprises(new JCheckBox(resources.getString("chkGenerateSurprises.text")));
        getChkGenerateSurprises().setToolTipText(resources.getString("chkGenerateSurprises.toolTipText"));
        getChkGenerateSurprises().setName("chkGenerateSurprises");
        getChkGenerateSurprises().addActionListener(evt -> {
            final boolean selected = getChkGenerateSurprises().isSelected();
            getChkGenerateMysteryBoxes().setEnabled(selected);
            mysteryBoxPanel.setEnabled(selected && getChkGenerateMysteryBoxes().isSelected());
        });

        setChkGenerateMysteryBoxes(new JCheckBox(resources.getString("chkGenerateMysteryBoxes.text")));
        getChkGenerateMysteryBoxes().setToolTipText(resources.getString("chkGenerateMysteryBoxes.toolTipText"));
        getChkGenerateMysteryBoxes().setName("chkGenerateMysteryBoxes");
        getChkGenerateMysteryBoxes().addActionListener(evt -> mysteryBoxPanel.setEnabled(
                getChkGenerateMysteryBoxes().isSelected()));

        createMysteryBoxPanel(mysteryBoxPanel);

        // Disable Panel by Default
        getChkGenerateSurprises().setSelected(true);
        getChkGenerateSurprises().doClick();

        // Layout the UI
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("surprisesPanel.title")));
        panel.setToolTipText(resources.getString("surprisesPanel.toolTipText"));
        panel.setName("surprisesPanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkGenerateSurprises())
                        .addComponent(getChkGenerateMysteryBoxes())
                        .addComponent(mysteryBoxPanel)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(Alignment.LEADING)
                        .addComponent(getChkGenerateSurprises())
                        .addComponent(getChkGenerateMysteryBoxes())
                        .addComponent(mysteryBoxPanel)
        );

        // TODO : Wave 7 : Surprises
        panel.setEnabled(false);
        getChkGenerateSurprises().setEnabled(false);
        getChkGenerateMysteryBoxes().setEnabled(false);
        mysteryBoxPanel.setEnabled(false);

        return panel;
    }

    private void createMysteryBoxPanel(final JPanel panel) {
        // Create Panel
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("mysteryBoxPanel.title")));
        panel.setToolTipText(resources.getString("mysteryBoxPanel.toolTipText"));
        panel.setLayout(new GridLayout(0, 1));

        // Create Panel Components
        setChkGenerateMysteryBoxTypes(new HashMap<>());
        for (final MysteryBoxType type : MysteryBoxType.values()) {
            getChkGenerateMysteryBoxTypes().put(type, new JCheckBox(type.toString()));
            getChkGenerateMysteryBoxTypes().get(type).setToolTipText(type.getToolTipText());
            getChkGenerateMysteryBoxTypes().get(type).setName("chk" + type.name());
            panel.add(getChkGenerateMysteryBoxTypes().get(type));
        }
    }
    //endregion Initialization

    //region Options
    /**
     * Sets the options for this panel to the default for the selected CompanyGenerationMethod
     */
    public void setOptions() {
        setOptions(getComboCompanyGenerationMethod().getSelectedItem());
    }

    /**
     * Sets the options for this panel to the default for the provided CompanyGenerationMethod
     * @param method the CompanyGenerationOptions to create the CompanyGenerationOptions from
     */
    public void setOptions(final CompanyGenerationMethod method) {
        setOptions(new CompanyGenerationOptions(method));
    }

    /**
     * Sets the options for this panel based on the provided CompanyGenerationOptions
     * @param options the CompanyGenerationOptions to use
     */
    public void setOptions(final CompanyGenerationOptions options) {
        // Base Information
        getComboCompanyGenerationMethod().setSelectedItem(options.getMethod());
        getComboSpecifiedFaction().setSelectedItem(new FactionDisplay(options.getSpecifiedFaction(), getCampaign().getLocalDate()));
        getChkGenerateMercenaryCompanyCommandLance().setSelected(options.isGenerateMercenaryCompanyCommandLance());
        getSpnCompanyCount().setValue(options.getCompanyCount());
        getSpnIndividualLanceCount().setValue(options.getIndividualLanceCount());
        getSpnLancesPerCompany().setValue(options.getLancesPerCompany());
        getSpnLanceSize().setValue(options.getLanceSize());
        getSpnStarLeagueYear().setValue(options.getStarLeagueYear());

        // Personnel
        updateLblTotalSupportPersonnel(determineMaximumSupportPersonnel());
        for (final Entry<PersonnelRole, JSpinner> entry : getSpnSupportPersonnelNumbers().entrySet()) {
            entry.getValue().setValue(options.getSupportPersonnel().getOrDefault(entry.getKey(), 0));
        }
        getChkPoolAssistants().setSelected(options.isPoolAssistants());
        getChkGenerateCaptains().setSelected(options.isGenerateCaptains());
        getChkAssignCompanyCommanderFlag().setSelected(options.isAssignCompanyCommanderFlag());
        getChkApplyOfficerStatBonusToWorstSkill().setSelected(options.isApplyOfficerStatBonusToWorstSkill());
        if (getChkAssignBestCompanyCommander().isSelected() != options.isAssignBestCompanyCommander()) {
            getChkAssignBestCompanyCommander().doClick();
        }
        getChkPrioritizeCompanyCommanderCombatSkills().setSelected(options.isPrioritizeCompanyCommanderCombatSkills());
        if (getChkAssignBestOfficers().isSelected() != options.isAssignBestOfficers()) {
            getChkAssignBestOfficers().doClick();
        }
        getChkPrioritizeOfficerCombatSkills().setSelected(options.isPrioritizeOfficerCombatSkills());
        getChkAssignMostSkilledToPrimaryLances().setSelected(options.isAssignMostSkilledToPrimaryLances());
        getChkAutomaticallyAssignRanks().setSelected(options.isAutomaticallyAssignRanks());
        getChkUseSpecifiedFactionToAssignRanks().setSelected(options.isUseSpecifiedFactionToAssignRanks());
        getChkAssignMekWarriorsCallsigns().setSelected(options.isAssignMekWarriorsCallsigns());
        getChkAssignFounderFlag().setSelected(options.isAssignFounderFlag());

        // Personnel Randomization
        getRandomOriginOptionsPanel().setOptions(options.getRandomOriginOptions());

        // Starting Simulation
        if (getChkRunStartingSimulation().isSelected() != options.isRunStartingSimulation()) {
            getChkRunStartingSimulation().doClick();
        }
        getSpnSimulationDuration().setValue(options.getSimulationDuration());
        getChkSimulateRandomMarriages().setSelected(options.isSimulateRandomMarriages());
        getChkSimulateRandomProcreation().setSelected(options.isSimulateRandomProcreation());

        // Units
        getComboBattleMekFactionGenerationMethod().setSelectedItem(options.getBattleMekFactionGenerationMethod());
        getComboBattleMekWeightClassGenerationMethod().setSelectedItem(options.getBattleMekWeightClassGenerationMethod());
        getComboBattleMekQualityGenerationMethod().setSelectedItem(options.getBattleMekQualityGenerationMethod());
        getChkNeverGenerateStarLeagueMeks().setSelected(options.isNeverGenerateStarLeagueMeks());
        getChkOnlyGenerateStarLeagueMeks().setSelected(options.isOnlyGenerateStarLeagueMeks());
        getChkOnlyGenerateOmniMeks().setSelected(options.isOnlyGenerateOmniMeks());
        getChkGenerateUnitsAsAttached().setSelected(options.isGenerateUnitsAsAttached());
        getChkAssignBestRollToCompanyCommander().setSelected(options.isAssignBestRollToCompanyCommander());
        getChkSortStarLeagueUnitsFirst().setSelected(options.isSortStarLeagueUnitsFirst());
        getChkGroupByWeight().setSelected(options.isGroupByWeight());
        getChkGroupByQuality().setSelected(options.isGroupByQuality());
        getChkKeepOfficerRollsSeparate().setSelected(options.isKeepOfficerRollsSeparate());
        getChkAssignTechsToUnits().setSelected(options.isAssignTechsToUnits());

        // Unit
        getComboForceNamingMethod().setSelectedItem(options.getForceNamingMethod());
        if (getChkGenerateForceIcons().isSelected() != options.isGenerateForceIcons()) {
            getChkGenerateForceIcons().doClick();
        }
        getChkUseSpecifiedFactionToGenerateForceIcons().setSelected(options.isUseSpecifiedFactionToGenerateForceIcons());
        if (getChkGenerateOriginNodeForceIcon().isSelected() != options.isGenerateOriginNodeForceIcon()) {
            getChkGenerateOriginNodeForceIcon().doClick();
        }
        getChkUseOriginNodeForceIconLogo().setSelected(options.isUseOriginNodeForceIconLogo());
        for (final Entry<Integer, Integer> entry : options.getForceWeightLimits().entrySet()) {
            getSpnForceWeightLimits().get(entry.getValue()).setValue(entry.getKey());
        }

        // Spares
        if (getChkGenerateMothballedSpareUnits().isSelected() != options.isGenerateMothballedSpareUnits()) {
            getChkGenerateMothballedSpareUnits().doClick();
        }
        getSpnSparesPercentOfActiveUnits().setValue(options.getSparesPercentOfActiveUnits());
        getComboPartGenerationMethod().setSelectedItem(options.getPartGenerationMethod());
        getSpnStartingArmourWeight().setValue(options.getStartingArmourWeight());
        if (getChkGenerateSpareAmmunition().isSelected() != options.isGenerateSpareAmmunition()) {
            getChkGenerateSpareAmmunition().doClick();
        }
        getSpnNumberReloadsPerWeapon().setValue(options.getNumberReloadsPerWeapon());
        getChkGenerateFractionalMachineGunAmmunition().setSelected(options.isGenerateFractionalMachineGunAmmunition());

        // Contracts
        if (getChkSelectStartingContract().isSelected() != options.isSelectStartingContract()) {
            getChkSelectStartingContract().doClick();
        }
        getChkStartCourseToContractPlanet().setSelected(options.isStartCourseToContractPlanet());

        // Finances
        if (getChkProcessFinances().isSelected() != options.isProcessFinances()) {
            getChkProcessFinances().doClick();
        }
        getSpnStartingCash().setValue(options.getStartingCash());
        if (getChkRandomizeStartingCash().isSelected() != options.isRandomizeStartingCash()) {
            getChkRandomizeStartingCash().doClick();
        }
        getSpnRandomStartingCashDiceCount().setValue(options.getRandomStartingCashDiceCount());
        getSpnMinimumStartingFloat().setValue(options.getMinimumStartingFloat());
        getChkIncludeInitialContractPayment().setSelected(options.isIncludeInitialContractPayment());
        getChkStartingLoan().setSelected(options.isStartingLoan());
        if (getChkPayForSetup().isSelected() != options.isPayForSetup()) {
            getChkPayForSetup().doClick();
        }
        getChkPayForPersonnel().setSelected(options.isPayForPersonnel());
        getChkPayForUnits().setSelected(options.isPayForUnits());
        getChkPayForParts().setSelected(options.isPayForParts());
        getChkPayForArmour().setSelected(options.isPayForArmour());
        getChkPayForAmmunition().setSelected(options.isPayForAmmunition());

        // Surprises
        if (getChkGenerateSurprises().isSelected() != options.isGenerateSurprises()) {
            getChkGenerateSurprises().doClick();
        }

        if (getChkGenerateMysteryBoxes().isSelected() != options.isGenerateMysteryBoxes()) {
            getChkGenerateMysteryBoxes().doClick();
        }

        for (final Entry<MysteryBoxType, JCheckBox> entry : getChkGenerateMysteryBoxTypes().entrySet()) {
            entry.getValue().setSelected(options.getGenerateMysteryBoxTypes().getOrDefault(entry.getKey(), false));
        }
    }

    /**
     * @return the CompanyGenerationOptions created from the current panel
     */
    public CompanyGenerationOptions createOptionsFromPanel() {
        final CompanyGenerationOptions options = new CompanyGenerationOptions(
                getComboCompanyGenerationMethod().getSelectedItem());

        // Base Information
        options.setSpecifiedFaction(Objects.requireNonNull(getComboSpecifiedFaction().getSelectedItem()).getFaction());
        options.setGenerateMercenaryCompanyCommandLance(getChkGenerateMercenaryCompanyCommandLance().isSelected());
        options.setCompanyCount((Integer) getSpnCompanyCount().getValue());
        options.setIndividualLanceCount((Integer) getSpnIndividualLanceCount().getValue());
        options.setLancesPerCompany((Integer) getSpnLancesPerCompany().getValue());
        options.setLanceSize((Integer) getSpnLanceSize().getValue());
        options.setStarLeagueYear((Integer) getSpnStarLeagueYear().getValue());

        // Personnel
        options.setSupportPersonnel(new HashMap<>());
        for (final Entry<PersonnelRole, JSpinner> entry : getSpnSupportPersonnelNumbers().entrySet()) {
            final int value = (int) entry.getValue().getValue();
            if (value <= 0) {
                continue;
            }
            options.getSupportPersonnel().put(entry.getKey(), value);
        }
        options.setPoolAssistants(getChkPoolAssistants().isSelected());
        options.setGenerateCaptains(getChkGenerateCaptains().isSelected());
        options.setAssignCompanyCommanderFlag(getChkAssignCompanyCommanderFlag().isSelected());
        options.setApplyOfficerStatBonusToWorstSkill(getChkApplyOfficerStatBonusToWorstSkill().isSelected());
        options.setAssignBestCompanyCommander(getChkAssignBestCompanyCommander().isSelected());
        options.setPrioritizeCompanyCommanderCombatSkills(getChkPrioritizeCompanyCommanderCombatSkills().isSelected());
        options.setAssignBestOfficers(getChkAssignBestOfficers().isSelected());
        options.setPrioritizeOfficerCombatSkills(getChkPrioritizeOfficerCombatSkills().isSelected());
        options.setAssignMostSkilledToPrimaryLances(getChkAssignMostSkilledToPrimaryLances().isSelected());
        options.setAutomaticallyAssignRanks(getChkAutomaticallyAssignRanks().isSelected());
        options.setUseSpecifiedFactionToAssignRanks(getChkUseSpecifiedFactionToAssignRanks().isSelected());
        options.setAssignMekWarriorsCallsigns(getChkAssignMekWarriorsCallsigns().isSelected());
        options.setAssignFounderFlag(getChkAssignFounderFlag().isSelected());

        // Personnel Randomization
        options.setRandomOriginOptions(getRandomOriginOptionsPanel().createOptionsFromPanel());

        // Starting Simulation
        options.setRunStartingSimulation(getChkRunStartingSimulation().isSelected());
        options.setSimulationDuration((Integer) getSpnSimulationDuration().getValue());
        options.setSimulateRandomMarriages(getChkSimulateRandomMarriages().isSelected());
        options.setSimulateRandomProcreation(getChkSimulateRandomProcreation().isSelected());

        // Units
        options.setBattleMekFactionGenerationMethod(getComboBattleMekFactionGenerationMethod().getSelectedItem());
        options.setBattleMekWeightClassGenerationMethod(getComboBattleMekWeightClassGenerationMethod().getSelectedItem());
        options.setBattleMekQualityGenerationMethod(getComboBattleMekQualityGenerationMethod().getSelectedItem());
        options.setNeverGenerateStarLeagueMeks(getChkNeverGenerateStarLeagueMeks().isSelected());
        options.setOnlyGenerateStarLeagueMeks(getChkOnlyGenerateStarLeagueMeks().isSelected());
        options.setOnlyGenerateOmniMeks(getChkOnlyGenerateOmniMeks().isSelected());
        options.setGenerateUnitsAsAttached(getChkGenerateUnitsAsAttached().isSelected());
        options.setAssignBestRollToCompanyCommander(getChkAssignBestRollToCompanyCommander().isSelected());
        options.setSortStarLeagueUnitsFirst(getChkSortStarLeagueUnitsFirst().isSelected());
        options.setGroupByWeight(getChkGroupByWeight().isSelected());
        options.setGroupByQuality(getChkGroupByQuality().isSelected());
        options.setKeepOfficerRollsSeparate(getChkKeepOfficerRollsSeparate().isSelected());
        options.setAssignTechsToUnits(getChkAssignTechsToUnits().isSelected());

        // Unit
        options.setForceNamingMethod(getComboForceNamingMethod().getSelectedItem());
        options.setGenerateForceIcons(getChkGenerateForceIcons().isSelected());
        options.setUseSpecifiedFactionToGenerateForceIcons(getChkUseSpecifiedFactionToGenerateForceIcons().isSelected());
        options.setGenerateOriginNodeForceIcon(getChkGenerateOriginNodeForceIcon().isSelected());
        options.setUseOriginNodeForceIconLogo(getChkUseOriginNodeForceIconLogo().isSelected());
        options.setForceWeightLimits(new TreeMap<>());
        for (final Entry<Integer, JSpinner> entry : getSpnForceWeightLimits().entrySet()) {
            options.getForceWeightLimits().put((int) entry.getValue().getValue(), entry.getKey());
        }

        // Spares
        options.setGenerateMothballedSpareUnits(getChkGenerateMothballedSpareUnits().isSelected());
        options.setSparesPercentOfActiveUnits((Integer) getSpnSparesPercentOfActiveUnits().getValue());
        options.setPartGenerationMethod(getComboPartGenerationMethod().getSelectedItem());
        options.setStartingArmourWeight((Integer) getSpnStartingArmourWeight().getValue());
        options.setGenerateSpareAmmunition(getChkGenerateSpareAmmunition().isSelected());
        options.setNumberReloadsPerWeapon((Integer) getSpnNumberReloadsPerWeapon().getValue());
        options.setGenerateFractionalMachineGunAmmunition(getChkGenerateFractionalMachineGunAmmunition().isSelected());

        // Contracts
        options.setSelectStartingContract(getChkSelectStartingContract().isSelected());
        options.setStartCourseToContractPlanet(getChkStartCourseToContractPlanet().isSelected());

        // Finances
        options.setProcessFinances(getChkProcessFinances().isSelected());
        options.setStartingCash((Integer) getSpnStartingCash().getValue());
        options.setRandomizeStartingCash(getChkRandomizeStartingCash().isSelected());
        options.setRandomStartingCashDiceCount((Integer) getSpnRandomStartingCashDiceCount().getValue());
        options.setMinimumStartingFloat((Integer) getSpnMinimumStartingFloat().getValue());
        options.setIncludeInitialContractPayment(getChkIncludeInitialContractPayment().isSelected());
        options.setStartingLoan(getChkStartingLoan().isSelected());
        options.setPayForSetup(getChkPayForSetup().isSelected());
        options.setPayForPersonnel(getChkPayForPersonnel().isSelected());
        options.setPayForUnits(getChkPayForUnits().isSelected());
        options.setPayForParts(getChkPayForParts().isSelected());
        options.setPayForArmour(getChkPayForArmour().isSelected());
        options.setPayForAmmunition(getChkPayForAmmunition().isSelected());

        // Surprises
        options.setGenerateSurprises(getChkGenerateSurprises().isSelected());
        options.setGenerateMysteryBoxes(getChkGenerateMysteryBoxes().isSelected());
        options.setGenerateMysteryBoxTypes(new HashMap<>());
        for (final Entry<MysteryBoxType, JCheckBox> entry : getChkGenerateMysteryBoxTypes().entrySet()) {
            options.getGenerateMysteryBoxTypes().put(entry.getKey(), entry.getValue().isSelected());
        }

        return options;
    }

    /**
     * Validates the data contained in this panel, returning the current state of validation.
     * @param display to display dialogs containing the messages or not
     * @return true if the data validates successfully, otherwise false
     */
    public ValidationState validateOptions(final boolean display) {
        //region Errors
        // Minimum Generation Size Validation
        // Minimum Generation Parameter of 1 Company or Lance, the Company Command Lance Doesn't Count
        if (((int) getSpnCompanyCount().getValue() <= 0)
                && ((int) getSpnIndividualLanceCount().getValue() <= 0)) {
            if (display) {
                JOptionPane.showMessageDialog(getFrame(),
                        resources.getString("CompanyGenerationOptionsPanel.InvalidGenerationSize.text"),
                        resources.getString("InvalidOptions.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return ValidationState.FAILURE;
        }

        // Random Origin Options Validation
        if (getRandomOriginOptionsPanel().validateOptions(display).isFailure()) {
            return ValidationState.FAILURE;
        }
        //endregion Errors

        //region Warnings
        // Only need to check these if they are to be displayed
//        if (display) {
//            // Support Personnel Count:
//            // 1) Above Recommended Maximum Support Personnel Count
//            // 2) Below Half of Recommended Maximum Support Personnel Count
//            final int maximumSupportPersonnelCount = determineMaximumSupportPersonnel();
//            final int currentSupportPersonnelCount = getSpnSupportPersonnelNumbers().values().stream()
//                    .mapToInt(spinner -> (int) spinner.getValue()).sum();
//            if ((maximumSupportPersonnelCount < currentSupportPersonnelCount)
//                    && (JOptionPane.showConfirmDialog(getFrame(),
//                    resources.getString("CompanyGenerationOptionsPanel.OverMaximumSupportPersonnel.text"),
//                    resources.getString("CompanyGenerationOptionsPanel.OverMaximumSupportPersonnel.title"),
//                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)) {
//                return ValidationState.WARNING;
//            } else if ((currentSupportPersonnelCount < (maximumSupportPersonnelCount / 2.0))
//                    && (JOptionPane.showConfirmDialog(getFrame(),
//                    resources.getString("CompanyGenerationOptionsPanel.UnderHalfMaximumSupportPersonnel.text"),
//                    resources.getString("CompanyGenerationOptionsPanel.UnderHalfMaximumSupportPersonnel.title"),
//                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)) {
//                return ValidationState.WARNING;
//            }
//        }
        //endregion Warnings

        // The options specified are correct, and thus can be saved
        return ValidationState.SUCCESS;
    }
    //endregion Options

    //region File I/O
    /**
     * Imports CompanyGenerationOptions from an XML file
     */
    public void importOptionsFromXML() {
        FileDialogs.openCompanyGenerationOptions(getFrame())
                .ifPresent(file -> setOptions(CompanyGenerationOptions.parseFromXML(file)));
    }

    /**
     * Exports the CompanyGenerationOptions displayed on this panel to an XML file.
     */
    public void exportOptionsToXML() {
        FileDialogs.saveCompanyGenerationOptions(getFrame())
                .ifPresent(file -> createOptionsFromPanel().writeToFile(file));
    }
    //endregion File I/O
}
