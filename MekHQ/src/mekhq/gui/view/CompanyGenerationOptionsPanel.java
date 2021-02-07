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
package mekhq.gui.view;

import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.CompanyGenerationType;
import mekhq.campaign.universe.enums.ForceNamingType;
import mekhq.campaign.universe.generators.companyGeneration.CompanyGenerationOptions;
import mekhq.gui.FileDialogs;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;

public class CompanyGenerationOptionsPanel extends JPanel {
    //region Variable Declarations
    private JFrame frame;
    private Campaign campaign;

    // GUI Variables
    // Base Information
    private JComboBox<CompanyGenerationType> comboCompanyGenerationType;
    private JComboBox<FactionChoice> comboFaction;
    private JComboBox<PlanetarySystem> comboStartingSystem;
    private JComboBox<Planet> comboStartingPlanet;
    private JSpinner spnCompanyCount;
    private JSpinner spnIndividualLanceCount;
    private JCheckBox chkGenerateMercenaryCompanyCommandLance;
    private JSpinner spnLancesPerCompany;
    private JSpinner spnLanceSize;

    // Personnel
    private JLabel lblTotalSupportPersonnel;
    private RoleToSpinner[] spnSupportPersonnelNumbers;
    private JCheckBox chkPoolAssistants;
    private JCheckBox chkGenerateCaptains;
    private JCheckBox chkAssignCompanyCommanderFlag;
    private JCheckBox chkCompanyCommanderLanceOfficer;
    private JCheckBox chkApplyOfficerStatBonusToWorstSkill;
    private JCheckBox chkAssignBestOfficers;
    private JCheckBox chkAutomaticallyAssignRanks;

    // Personnel Randomization
    private JCheckBox chkRandomizeOrigin;
    private JComboBox<PlanetarySystem> comboCentralSystem;
    private JComboBox<Planet> comboCentralPlanet;
    private JSpinner spnOriginSearchRadius;
    private JCheckBox chkExtraRandomOrigin;
    private JSpinner spnOriginDistanceScale;

    // Units
    private JCheckBox chkGenerateUnitsAsAttached;
    private JCheckBox chkAssignBestRollToUnitCommander;
    private JCheckBox chkGroupByWeight;
    private JCheckBox chkKeepOfficerRollsSeparate;
    private JSpinner spnStarLeagueYear;
    private JCheckBox chkAssignTechsToUnits;

    // Unit
    private JComboBox<ForceNamingType> comboForceNamingType;
    private JCheckBox chkGenerateForceIcons;

    // Spares
    private JCheckBox ckhGenerateMothballedSpareUnits;
    private JSpinner spnSparesPercentOfActiveUnits;
    private JCheckBox chkGenerateSpareParts;
    private JSpinner spnStartingArmourWeight;
    private JCheckBox chkGenerateSpareAmmunition;
    private JSpinner spnNumberReloadsPerWeapon;
    private JCheckBox chkGenerateFractionalMachineGunAmmunition;

    // Contracts
    private JCheckBox chkSelectStartingContract;
    private JCheckBox chkStartCourseToContractPlanet;

    // Finances
    private JSpinner spnStartingCash;
    private JCheckBox chkRandomizeStartingCash;
    private JSpinner spnRandomStartingCashDiceCount;
    private JSpinner spnMinimumStartingFloat;
    private JCheckBox chkPayForSetup;
    private JCheckBox chkStartingLoan;
    private JCheckBox chkPayForPersonnel;
    private JCheckBox chkPayForUnits;
    private JCheckBox chkPayForParts;
    private JCheckBox chkPayForAmmunition;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationOptionsPanel(final JFrame frame, final Campaign campaign) {
        setFrame(frame);
        setCampaign(campaign);

        initialize();

        if (campaign.getCampaignOptions().getCompanyGenerationOptions() == null) {
            setOptions(MekHQ.getMekHQOptions().getDefaultCompanyGenerationType());
        } else {
            setOptions(campaign.getCampaignOptions().getCompanyGenerationOptions());
        }
    }
    //endregion Constructors

    //region Getters/Setters
    public JFrame getFrame() {
        return frame;
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    //region GUI Variables
    //region Base Information
    public JComboBox<CompanyGenerationType> getComboCompanyGenerationType() {
        return comboCompanyGenerationType;
    }

    public CompanyGenerationType getCompanyGenerationType() {
        return (CompanyGenerationType) Objects.requireNonNull(getComboCompanyGenerationType().getSelectedItem());
    }

    public void setComboCompanyGenerationType(JComboBox<CompanyGenerationType> comboCompanyGenerationType) {
        this.comboCompanyGenerationType = comboCompanyGenerationType;
    }

    public JComboBox<FactionChoice> getComboFaction() {
        return comboFaction;
    }

    public Faction getFaction() {
        return ((FactionChoice) Objects.requireNonNull(getComboFaction().getSelectedItem())).getFaction();
    }

    public void setComboFaction(JComboBox<FactionChoice> comboFaction) {
        this.comboFaction = comboFaction;
    }

    public JSpinner getSpnCompanyCount() {
        return spnCompanyCount;
    }

    public void setSpnCompanyCount(JSpinner spnCompanyCount) {
        this.spnCompanyCount = spnCompanyCount;
    }

    public JSpinner getSpnIndividualLanceCount() {
        return spnIndividualLanceCount;
    }

    public void setSpnIndividualLanceCount(JSpinner spnIndividualLanceCount) {
        this.spnIndividualLanceCount = spnIndividualLanceCount;
    }

    public JCheckBox getChkGenerateMercenaryCompanyCommandLance() {
        return chkGenerateMercenaryCompanyCommandLance;
    }

    public void setChkGenerateMercenaryCompanyCommandLance(JCheckBox chkGenerateMercenaryCompanyCommandLance) {
        this.chkGenerateMercenaryCompanyCommandLance = chkGenerateMercenaryCompanyCommandLance;
    }

    public JSpinner getSpnLancesPerCompany() {
        return spnLancesPerCompany;
    }

    public void setSpnLancesPerCompany(JSpinner spnLancesPerCompany) {
        this.spnLancesPerCompany = spnLancesPerCompany;
    }

    public JSpinner getSpnLanceSize() {
        return spnLanceSize;
    }

    public void setSpnLanceSize(JSpinner spnLanceSize) {
        this.spnLanceSize = spnLanceSize;
    }
    //endregion Base Information

    //region Personnel
    public JLabel getLblTotalSupportPersonnel() {
        return lblTotalSupportPersonnel;
    }

    public void updateLblTotalSupportPersonnel() {
        updateLblTotalSupportPersonnel(((getChkGenerateMercenaryCompanyCommandLance().isSelected() ? 1 : 0)
                + ((int) getSpnCompanyCount().getValue() * (int) getSpnLancesPerCompany().getValue())
                + (int) getSpnIndividualLanceCount().getValue()) * (int) getSpnLanceSize().getValue());
    }

    public void updateLblTotalSupportPersonnel(int numSupportPersonnel) {
        getLblTotalSupportPersonnel().setText(String.format(
                resources.getString("lblTotalSupportPersonnel.text"), numSupportPersonnel));
    }

    public void setLblTotalSupportPersonnel(JLabel lblTotalSupportPersonnel) {
        this.lblTotalSupportPersonnel = lblTotalSupportPersonnel;
    }

    public RoleToSpinner[] getSpnSupportPersonnelNumbers() {
        return spnSupportPersonnelNumbers;
    }

    public Map<Integer, Integer> getSupportPersonnelNumbers() {
        Map<Integer, Integer> supportPersonnelNumbers = new HashMap<>();
        for (RoleToSpinner rts : getSpnSupportPersonnelNumbers()) {
            if (rts.getValue() > 0) {
                supportPersonnelNumbers.put(rts.getRole(), rts.getValue());
            }
        }
        return supportPersonnelNumbers;
    }

    public void setSpnSupportPersonnelNumbers(RoleToSpinner... spnSupportPersonnelNumbers) {
        this.spnSupportPersonnelNumbers = spnSupportPersonnelNumbers;
    }

    public void setSupportPersonnelNumbers(Map<Integer, Integer> supportPersonnelNumbers) {
        for (RoleToSpinner rts : getSpnSupportPersonnelNumbers()) {
            rts.getSpinner().setValue(supportPersonnelNumbers.getOrDefault(rts.getRole(), 0));
        }
    }

    public JCheckBox getChkPoolAssistants() {
        return chkPoolAssistants;
    }

    public void setChkPoolAssistants(JCheckBox chkPoolAssistants) {
        this.chkPoolAssistants = chkPoolAssistants;
    }

    public JCheckBox getChkGenerateCaptains() {
        return chkGenerateCaptains;
    }

    public void setChkGenerateCaptains(JCheckBox chkGenerateCaptains) {
        this.chkGenerateCaptains = chkGenerateCaptains;
    }

    public JCheckBox getChkCompanyCommanderLanceOfficer() {
        return chkCompanyCommanderLanceOfficer;
    }

    public void setChkCompanyCommanderLanceOfficer(JCheckBox chkCompanyCommanderLanceOfficer) {
        this.chkCompanyCommanderLanceOfficer = chkCompanyCommanderLanceOfficer;
    }

    public JCheckBox getChkApplyOfficerStatBonusToWorstSkill() {
        return chkApplyOfficerStatBonusToWorstSkill;
    }

    public void setChkApplyOfficerStatBonusToWorstSkill(JCheckBox chkApplyOfficerStatBonusToWorstSkill) {
        this.chkApplyOfficerStatBonusToWorstSkill = chkApplyOfficerStatBonusToWorstSkill;
    }

    public JCheckBox getChkAssignBestOfficers() {
        return chkAssignBestOfficers;
    }

    public void setChkAssignBestOfficers(JCheckBox chkAssignBestOfficers) {
        this.chkAssignBestOfficers = chkAssignBestOfficers;
    }

    public JCheckBox getChkAutomaticallyAssignRanks() {
        return chkAutomaticallyAssignRanks;
    }

    public void setChkAutomaticallyAssignRanks(JCheckBox chkAutomaticallyAssignRanks) {
        this.chkAutomaticallyAssignRanks = chkAutomaticallyAssignRanks;
    }
    //endregion Personnel

    //region Units
    public JCheckBox getChkGenerateUnitsAsAttached() {
        return chkGenerateUnitsAsAttached;
    }

    public void setChkGenerateUnitsAsAttached(JCheckBox chkGenerateUnitsAsAttached) {
        this.chkGenerateUnitsAsAttached = chkGenerateUnitsAsAttached;
    }

    public JCheckBox getChkAssignBestRollToUnitCommander() {
        return chkAssignBestRollToUnitCommander;
    }

    public void setChkAssignBestRollToUnitCommander(JCheckBox chkAssignBestRollToUnitCommander) {
        this.chkAssignBestRollToUnitCommander = chkAssignBestRollToUnitCommander;
    }

    public JCheckBox getChkGroupByWeight() {
        return chkGroupByWeight;
    }

    public void setChkGroupByWeight(JCheckBox chkGroupByWeight) {
        this.chkGroupByWeight = chkGroupByWeight;
    }
    //endregion Units

    //region Unit
    public JComboBox<ForceNamingType> getComboForceNamingType() {
        return comboForceNamingType;
    }

    public ForceNamingType getForceNamingType() {
        return (ForceNamingType) Objects.requireNonNull(getComboForceNamingType().getSelectedItem());
    }

    public void setComboForceNamingType(JComboBox<ForceNamingType> comboForceNamingType) {
        this.comboForceNamingType = comboForceNamingType;
    }

    public JCheckBox getChkGenerateForceIcons() {
        return chkGenerateForceIcons;
    }

    public void setChkGenerateForceIcons(JCheckBox chkGenerateForceIcons) {
        this.chkGenerateForceIcons = chkGenerateForceIcons;
    }
    //endregion Unit

    //region Finances
    public JSpinner getSpnStartingCash() {
        return spnStartingCash;
    }

    public void setSpnStartingCash(JSpinner spnStartingCash) {
        this.spnStartingCash = spnStartingCash;
    }

    public JCheckBox getChkRandomizeStartingCash() {
        return chkRandomizeStartingCash;
    }

    public void setChkRandomizeStartingCash(JCheckBox chkRandomizeStartingCash) {
        this.chkRandomizeStartingCash = chkRandomizeStartingCash;
    }

    public JSpinner getSpnMinimumStartingFloat() {
        return spnMinimumStartingFloat;
    }

    public void setSpnMinimumStartingFloat(JSpinner spnMinimumStartingFloat) {
        this.spnMinimumStartingFloat = spnMinimumStartingFloat;
    }

    public JCheckBox getChkPayForSetup() {
        return chkPayForSetup;
    }

    public void setChkPayForSetup(JCheckBox chkPayForSetup) {
        this.chkPayForSetup = chkPayForSetup;
    }

    public JCheckBox getChkStartingLoan() {
        return chkStartingLoan;
    }

    public void setChkStartingLoan(JCheckBox chkStartingLoan) {
        this.chkStartingLoan = chkStartingLoan;
    }

    public JCheckBox getChkPayForPersonnel() {
        return chkPayForPersonnel;
    }

    public void setChkPayForPersonnel(JCheckBox chkPayForPersonnel) {
        this.chkPayForPersonnel = chkPayForPersonnel;
    }

    public JCheckBox getChkPayForUnits() {
        return chkPayForUnits;
    }

    public void setChkPayForUnits(JCheckBox chkPayForUnits) {
        this.chkPayForUnits = chkPayForUnits;
    }

    public JCheckBox getChkPayForParts() {
        return chkPayForParts;
    }

    public void setChkPayForParts(JCheckBox chkPayForParts) {
        this.chkPayForParts = chkPayForParts;
    }

    public JCheckBox getChkPayForAmmunition() {
        return chkPayForAmmunition;
    }

    public void setChkPayForAmmunition(JCheckBox chkPayForAmmunition) {
        this.chkPayForAmmunition = chkPayForAmmunition;
    }
    //endregion Finances
    //endregion GUI Variables
    //endregion Getters/Setters

    //region Initialization
    private void initialize() {
        setName("companyGenerationOptionsPanel");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(createBaseInformationPanel(), gbc);

        gbc.gridx++;
        add(createPersonnelPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(createUnitsPanel(), gbc);

        gbc.gridx++;
        add(createUnitPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        add(createFinancesPanel(), gbc);
    }

    private JPanel createBaseInformationPanel() {
        JLabel lblCompanyGenerationType = new JLabel(resources.getString("lblCompanyGenerationType.text"));
        lblCompanyGenerationType.setName("lblCompanyGenerationType");

        setComboCompanyGenerationType(new JComboBox<>(CompanyGenerationType.values()));
        getComboCompanyGenerationType().setName("comboCompanyGenerationType");

        JLabel lblFaction = new JLabel(resources.getString("lblFaction.text"));
        lblFaction.setName("lblFaction");

        setComboFaction(new JComboBox<>(getFactionChoices().toArray(new FactionChoice[]{})));
        getComboFaction().setName("comboFaction");

        setChkGenerateMercenaryCompanyCommandLance(new JCheckBox(resources.getString("chkGenerateMercenaryCompanyCommandLance.text")));
        getChkGenerateMercenaryCompanyCommandLance().setName("chkGenerateMercenaryCompanyCommandLance");

        JLabel lblCompanyCount = new JLabel(resources.getString("lblCompanyCount.text"));
        lblCompanyCount.setName("lblCompanyCount");

        setSpnCompanyCount(new JSpinner(new SpinnerNumberModel(0, 0, 3, 1)));
        getSpnCompanyCount().setName("spnCompanyCount");

        JLabel lblIndividualLanceCount = new JLabel(resources.getString("lblIndividualLanceCount.text"));
        lblIndividualLanceCount.setName("lblIndividualLanceCount");

        setSpnIndividualLanceCount(new JSpinner(new SpinnerNumberModel(0, 0, 2, 1)));
        getSpnIndividualLanceCount().setName("spnIndividualLanceCount");

        JLabel lblLancesPerCompany = new JLabel(resources.getString("lblLancesPerCompany.text"));
        lblLancesPerCompany.setName("lblLancesPerCompany");

        setSpnLancesPerCompany(new JSpinner(new SpinnerNumberModel(3, 2, 5, 1)));
        getSpnLancesPerCompany().setName("spnLancesPerCompany");

        JLabel lblLanceSize = new JLabel(resources.getString("lblLanceSize.text"));
        lblLanceSize.setName("lblLanceSize");

        setSpnLanceSize(new JSpinner(new SpinnerNumberModel(4, 3, 6, 1)));
        getSpnLanceSize().setName("spnLanceSize");

        // Layout the UI
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("baseInformationPanel.title")));
        panel.setName("baseInformationPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCompanyGenerationType)
                                .addComponent(getComboCompanyGenerationType(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblFaction)
                                .addComponent(getComboFaction())
                                .addComponent(getChkGenerateMercenaryCompanyCommandLance(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblCompanyCount)
                                .addComponent(getSpnCompanyCount())
                                .addComponent(lblIndividualLanceCount)
                                .addComponent(getSpnIndividualLanceCount(), GroupLayout.Alignment.LEADING))
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblLancesPerCompany)
                                .addComponent(getSpnLancesPerCompany())
                                .addComponent(lblLanceSize)
                                .addComponent(getSpnLanceSize(), GroupLayout.Alignment.LEADING))
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblCompanyGenerationType)
                                .addComponent(getComboCompanyGenerationType()))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblFaction)
                                .addComponent(getComboFaction())
                                .addComponent(getChkGenerateMercenaryCompanyCommandLance()))
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
        );
        return panel;
    }

    private JPanel createPersonnelPanel() {
        setLblTotalSupportPersonnel(new JLabel());
        updateLblTotalSupportPersonnel(0);
        getLblTotalSupportPersonnel().setName("lblTotalSupportPersonnel");

        JPanel supportPersonnelNumbersPanel = createSupportPersonnelNumbersPanel();

        setChkPoolAssistants(new JCheckBox(resources.getString("chkPoolAssistants.text")));
        getChkPoolAssistants().setToolTipText(resources.getString("chkPoolAssistants.toolTipText"));
        getChkPoolAssistants().setName("chkPoolAssistants");

        setChkGenerateCaptains(new JCheckBox(resources.getString("chkGenerateCaptains.text")));
        getChkGenerateCaptains().setToolTipText(resources.getString("chkGenerateCaptains.toolTipText"));
        getChkGenerateCaptains().setName("chkGenerateCaptains");

        setChkCompanyCommanderLanceOfficer(new JCheckBox(resources.getString("chkCompanyCommanderLanceOfficer.text")));
        getChkCompanyCommanderLanceOfficer().setToolTipText(resources.getString("chkCompanyCommanderLanceOfficer.toolTipText"));
        getChkCompanyCommanderLanceOfficer().setName("chkCompanyCommanderLanceOfficer");

        setChkApplyOfficerStatBonusToWorstSkill(new JCheckBox(resources.getString("chkApplyOfficerStatBonusToWorstSkill.text")));
        getChkApplyOfficerStatBonusToWorstSkill().setName("chkApplyOfficerStatBonusToWorstSkill");

        setChkAssignBestOfficers(new JCheckBox(resources.getString("chkAssignBestOfficers.text")));
        getChkAssignBestOfficers().setName("chkAssignBestOfficers");

        setChkAutomaticallyAssignRanks(new JCheckBox(resources.getString("chkAutomaticallyAssignRanks.text")));
        getChkAutomaticallyAssignRanks().setName("chkAutomaticallyAssignRanks");

        // Layout the UI
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("personnelPanel.title")));
        panel.setName("personnelPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getLblTotalSupportPersonnel())
                        .addComponent(supportPersonnelNumbersPanel)
                        .addComponent(getChkPoolAssistants())
                        .addComponent(getChkGenerateCaptains())
                        .addComponent(getChkCompanyCommanderLanceOfficer())
                        .addComponent(getChkApplyOfficerStatBonusToWorstSkill())
                        .addComponent(getChkAssignBestOfficers())
                        .addComponent(getChkAutomaticallyAssignRanks())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(getLblTotalSupportPersonnel())
                        .addComponent(supportPersonnelNumbersPanel)
                        .addComponent(getChkPoolAssistants())
                        .addComponent(getChkGenerateCaptains())
                        .addComponent(getChkCompanyCommanderLanceOfficer())
                        .addComponent(getChkApplyOfficerStatBonusToWorstSkill())
                        .addComponent(getChkAssignBestOfficers())
                        .addComponent(getChkAutomaticallyAssignRanks())
        );
        return panel;
    }

    private JPanel createSupportPersonnelNumbersPanel() {
        // This nasty array and panel must currently be created manually, for now at least
        final RoleToSpinner[] rtsArray = new RoleToSpinner[9];
        final JLabel[] rtsLabelArray = new JLabel[9];

        String roleName = Person.getRoleDesc(Person.T_MECH_TECH, false);
        rtsLabelArray[0] = new JLabel(roleName);
        rtsLabelArray[0].setName("lbl" + roleName);
        rtsArray[0] = new RoleToSpinner(Person.T_MECH_TECH, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[0].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_MECHANIC, false);
        rtsLabelArray[1] = new JLabel(roleName);
        rtsLabelArray[1].setName("lbl" + roleName);
        rtsArray[1] = new RoleToSpinner(Person.T_MECHANIC, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[1].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_AERO_TECH, false);
        rtsLabelArray[2] = new JLabel(roleName);
        rtsLabelArray[2].setName("lbl" + roleName);
        rtsArray[2] = new RoleToSpinner(Person.T_AERO_TECH, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[2].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_BA_TECH, false);
        rtsLabelArray[3] = new JLabel(roleName);
        rtsLabelArray[3].setName("lbl" + roleName);
        rtsArray[3] = new RoleToSpinner(Person.T_BA_TECH, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[3].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_DOCTOR, false);
        rtsLabelArray[4] = new JLabel(roleName);
        rtsLabelArray[4].setName("lbl" + roleName);
        rtsArray[4] = new RoleToSpinner(Person.T_DOCTOR, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[4].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_ADMIN_COM, false);
        rtsLabelArray[5] = new JLabel(roleName);
        rtsLabelArray[5].setName("lbl" + roleName);
        rtsArray[5] = new RoleToSpinner(Person.T_ADMIN_COM, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[5].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_ADMIN_LOG, false);
        rtsLabelArray[6] = new JLabel(roleName);
        rtsLabelArray[6].setName("lbl" + roleName);
        rtsArray[6] = new RoleToSpinner(Person.T_ADMIN_LOG, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[6].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_ADMIN_TRA, false);
        rtsLabelArray[7] = new JLabel(roleName);
        rtsLabelArray[7].setName("lbl" + roleName);
        rtsArray[7] = new RoleToSpinner(Person.T_ADMIN_TRA, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[7].getSpinner().setName("spn" + roleName);

        roleName = Person.getRoleDesc(Person.T_ADMIN_HR, false);
        rtsLabelArray[8] = new JLabel(roleName);
        rtsLabelArray[8].setName("lbl" + roleName);
        rtsArray[8] = new RoleToSpinner(Person.T_ADMIN_HR, new JSpinner(new SpinnerNumberModel(0, 0, 100, 1)));
        rtsArray[8].getSpinner().setName("spn" + roleName);
        setSpnSupportPersonnelNumbers(rtsArray);

        // Layout the UI
        JPanel panel = new JPanel(new GridLayout(6, 3));
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("supportPersonnelNumbersPanel.title")));
        panel.setName("supportPersonnelNumbersPanel");

        for (int i = 0; i < 3; i++) {
            panel.add(rtsLabelArray[i]);
        }
        for (int i = 0; i < 3; i++) {
            panel.add(rtsArray[i].getSpinner());
        }
        for (int i = 3; i < 6; i++) {
            panel.add(rtsLabelArray[i]);
        }
        for (int i = 3; i < 6; i++) {
            panel.add(rtsArray[i].getSpinner());
        }
        for (int i = 6; i < rtsLabelArray.length; i++) {
            panel.add(rtsLabelArray[i]);
        }
        for (int i = 6; i < rtsArray.length; i++) {
            panel.add(rtsArray[i].getSpinner());
        }
        return panel;
    }

    private JPanel createUnitsPanel() {
        setChkGenerateUnitsAsAttached(new JCheckBox(resources.getString("chkGenerateUnitsAsAttached.text")));
        getChkGenerateUnitsAsAttached().setToolTipText(resources.getString("chkGenerateUnitsAsAttached.toolTipText"));
        getChkGenerateUnitsAsAttached().setName("chkGenerateUnitsAsAttached");

        setChkAssignBestRollToUnitCommander(new JCheckBox(resources.getString("chkAssignBestRollToUnitCommander.text")));
        getChkAssignBestRollToUnitCommander().setToolTipText(resources.getString("chkAssignBestRollToUnitCommander.toolTipText"));
        getChkAssignBestRollToUnitCommander().setName("chkAssignBestRollToUnitCommander");

        setChkGroupByWeight(new JCheckBox(resources.getString("chkGroupByWeight.text")));
        getChkGroupByWeight().setToolTipText(resources.getString("chkGroupByWeight.toolTipText"));
        getChkGroupByWeight().setName("chkGroupByWeight");

        // Layout the UI
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("unitsPanel.title")));
        panel.setName("unitsPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(getChkGenerateUnitsAsAttached())
                        .addComponent(getChkAssignBestRollToUnitCommander())
                        .addComponent(getChkGroupByWeight())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(getChkGenerateUnitsAsAttached())
                        .addComponent(getChkAssignBestRollToUnitCommander())
                        .addComponent(getChkGroupByWeight())
        );
        return panel;
    }

    private JPanel createUnitPanel() {
        JLabel lblForceNamingType = new JLabel(resources.getString("lblForceNamingType.text"));
        lblForceNamingType.setName("lblForceNamingType");

        setComboForceNamingType(new JComboBox<>(ForceNamingType.values()));
        getComboForceNamingType().setName("comboForceNamingType");

        setChkGenerateForceIcons(new JCheckBox(resources.getString("chkGenerateForceIcons.text")));
        getChkGenerateForceIcons().setName("chkGenerateForceIcons");

        // Layout the UI
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("unitPanel.title")));
        panel.setName("unitPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblForceNamingType)
                                .addComponent(getComboForceNamingType(), GroupLayout.Alignment.LEADING))
                        .addComponent(getChkGenerateForceIcons())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblForceNamingType)
                                .addComponent(getComboForceNamingType()))
                        .addComponent(getChkGenerateForceIcons())
        );
        return panel;
    }

    private JPanel createFinancesPanel() {
        JLabel lblStartingCash = new JLabel(resources.getString("lblStartingCash.text"));
        lblStartingCash.setName("lblStartingCash");

        setSpnStartingCash(new JSpinner(new SpinnerNumberModel(0, 0, 200000000, 100000)));
        getSpnStartingCash().setName("spnStartingCash");

        setChkRandomizeStartingCash(new JCheckBox(resources.getString("chkRandomizeStartingCash.text")));
        getChkRandomizeStartingCash().setToolTipText(resources.getString("chkRandomizeStartingCash.toolTipText"));
        getChkRandomizeStartingCash().setName("chkRandomizeStartingCash");

        JLabel lblMinimumStartingFloat = new JLabel(resources.getString("lblMinimumStartingFloat.text"));
        lblMinimumStartingFloat.setToolTipText(resources.getString("lblMinimumStartingFloat.toolTipText"));
        lblMinimumStartingFloat.setName("lblMinimumStartingFloat");

        setSpnMinimumStartingFloat(new JSpinner(new SpinnerNumberModel(0, 0, 10000000, 100000)));
        getSpnMinimumStartingFloat().setToolTipText("lblMinimumStartingFloat.toolTipText");
        getSpnMinimumStartingFloat().setName("spnMinimumStartingFloat");

        setChkPayForSetup(new JCheckBox(resources.getString("chkPayForSetup.text")));
        getChkPayForSetup().setToolTipText(resources.getString("chkPayForSetup.toolTipText"));
        getChkPayForSetup().setName("chkPayForSetup");

        setChkStartingLoan(new JCheckBox(resources.getString("chkStartingLoan.text")));
        getChkStartingLoan().setToolTipText(resources.getString("chkStartingLoan.toolTipText"));
        getChkStartingLoan().setName("chkStartingLoan");

        setChkPayForPersonnel(new JCheckBox(resources.getString("chkPayForPersonnel.text")));
        getChkPayForPersonnel().setToolTipText(resources.getString("chkPayForPersonnel.toolTipText"));
        getChkPayForPersonnel().setName("chkPayForPersonnel");

        setChkPayForUnits(new JCheckBox(resources.getString("chkPayForUnits.text")));
        getChkPayForUnits().setToolTipText(resources.getString("chkPayForUnits.toolTipText"));
        getChkPayForUnits().setName("chkPayForUnits");

        setChkPayForParts(new JCheckBox(resources.getString("chkPayForParts.text")));
        getChkPayForParts().setToolTipText(resources.getString("chkPayForParts.toolTipText"));
        getChkPayForParts().setName("chkPayForParts");

        setChkPayForAmmunition(new JCheckBox(resources.getString("chkPayForAmmunition.text")));
        getChkPayForAmmunition().setToolTipText(resources.getString("chkPayForAmmunition.toolTipText"));
        getChkPayForAmmunition().setName("chkPayForAmmunition");

        // Layout the UI
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("financesPanel.title")));
        panel.setName("financesPanel");
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblStartingCash)
                                .addComponent(getSpnStartingCash(), GroupLayout.Alignment.LEADING))
                        .addComponent(getChkRandomizeStartingCash())
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(lblMinimumStartingFloat)
                                .addComponent(getSpnMinimumStartingFloat(), GroupLayout.Alignment.LEADING))
                        .addComponent(getChkPayForSetup())
                        .addComponent(getChkStartingLoan())
                        .addComponent(getChkPayForPersonnel())
                        .addComponent(getChkPayForUnits())
                        .addComponent(getChkPayForParts())
                        .addComponent(getChkPayForAmmunition())
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblStartingCash)
                                .addComponent(getSpnStartingCash()))
                        .addComponent(getChkRandomizeStartingCash())
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(lblMinimumStartingFloat)
                                .addComponent(getSpnMinimumStartingFloat()))
                        .addComponent(getChkPayForSetup())
                        .addComponent(getChkStartingLoan())
                        .addComponent(getChkPayForPersonnel())
                        .addComponent(getChkPayForUnits())
                        .addComponent(getChkPayForParts())
                        .addComponent(getChkPayForAmmunition())
        );
        return panel;
    }

    private List<FactionChoice> getFactionChoices() {
        final List<FactionChoice> factionChoices = new ArrayList<>();

        for (Faction faction : Factions.getInstance().getFactions()) {
            factionChoices.add(new FactionChoice(faction, getCampaign().getGameYear()));
        }

        final NaturalOrderComparator noc = new NaturalOrderComparator();
        factionChoices.sort((a, b) -> noc.compare(a.toString(), b.toString()));

        return factionChoices;
    }
    //endregion Initialization

    //region Options
    public void setOptions() {
        setOptions(getCompanyGenerationType());
    }

    public void setOptions(final CompanyGenerationType type) {
        getComboCompanyGenerationType().setSelectedItem(type);
        setOptions(new CompanyGenerationOptions(type, getCampaign()));
    }

    public void setOptions(final CompanyGenerationOptions options) {
        // Base Information
        getComboCompanyGenerationType().setSelectedItem(options.getType());

        getComboFaction().setSelectedIndex(0); // default to 0
        final List<FactionChoice> factionChoices = getFactionChoices();
        for (int i = 0; i < factionChoices.size(); i++) {
            if (options.getFaction().equals(factionChoices.get(i).getFaction())) {
                getComboFaction().setSelectedIndex(i);
                break;
            }
        }

        getSpnCompanyCount().setValue(options.getCompanyCount());
        getSpnIndividualLanceCount().setValue(options.getIndividualLanceCount());
        getChkGenerateMercenaryCompanyCommandLance().setSelected(options.isGenerateMercenaryCompanyCommandLance());
        getSpnLancesPerCompany().setValue(options.getLancesPerCompany());
        getSpnLanceSize().setValue(options.getLanceSize());

        // Personnel
        updateLblTotalSupportPersonnel();
        setSupportPersonnelNumbers(options.getSupportPersonnel());
        getChkPoolAssistants().setSelected(options.isPoolAssistants());
        getChkGenerateCaptains().setSelected(options.isGenerateCaptains());
        getChkCompanyCommanderLanceOfficer().setSelected(options.isCompanyCommanderLanceOfficer());
        getChkApplyOfficerStatBonusToWorstSkill().setSelected(options.isApplyOfficerStatBonusToWorstSkill());
        getChkAssignBestOfficers().setSelected(options.isAssignBestOfficers());
        getChkAutomaticallyAssignRanks().setSelected(options.isAutomaticallyAssignRanks());

        // Units
        getChkGenerateUnitsAsAttached().setSelected(options.isGenerateUnitsAsAttached());
        getChkAssignBestRollToUnitCommander().setSelected(options.isAssignBestRollToUnitCommander());
        getChkGroupByWeight().setSelected(options.isGroupByWeight());

        // Unit
        getComboForceNamingType().setSelectedItem(options.getForceNamingType());
        getChkGenerateForceIcons().setSelected(options.isGenerateForceIcons());

        // Finances
        getSpnStartingCash().setValue(options.getStartingCash());
        getChkRandomizeStartingCash().setSelected(options.isRandomizeStartingCash());
        getSpnMinimumStartingFloat().setValue(options.getMinimumStartingFloat());
        getChkPayForSetup().setSelected(options.isPayForSetup());
        getChkStartingLoan().setSelected(options.isStartingLoan());
        getChkPayForPersonnel().setSelected(options.isPayForPersonnel());
        getChkPayForUnits().setSelected(options.isPayForUnits());
        getChkPayForParts().setSelected(options.isPayForParts());
        getChkPayForAmmunition().setSelected(options.isPayForAmmunition());
    }

    /**
     * @return the CompanyGenerationOptions created from the current panel
     */
    public CompanyGenerationOptions createOptionsFromPanel() {
        final CompanyGenerationOptions options = new CompanyGenerationOptions();
        // Base Information
        options.setType(getCompanyGenerationType());
        options.setFaction(getFaction());
        options.setCompanyCount((Integer) getSpnCompanyCount().getValue());
        options.setIndividualLanceCount((Integer) getSpnIndividualLanceCount().getValue());
        options.setGenerateMercenaryCompanyCommandLance(getChkGenerateMercenaryCompanyCommandLance().isSelected());
        options.setLancesPerCompany((Integer) getSpnLancesPerCompany().getValue());
        options.setLanceSize((Integer) getSpnLanceSize().getValue());

        // Personnel
        options.setSupportPersonnel(getSupportPersonnelNumbers());
        options.setPoolAssistants(getChkPoolAssistants().isSelected());
        options.setGenerateCaptains(getChkGenerateCaptains().isSelected());
        options.setCompanyCommanderLanceOfficer(getChkCompanyCommanderLanceOfficer().isSelected());
        options.setApplyOfficerStatBonusToWorstSkill(getChkApplyOfficerStatBonusToWorstSkill().isSelected());
        options.setAssignBestOfficers(getChkAssignBestOfficers().isSelected());
        options.setAutomaticallyAssignRanks(getChkAutomaticallyAssignRanks().isSelected());

        // Units
        options.setGenerateUnitsAsAttached(getChkGenerateUnitsAsAttached().isSelected());
        options.setAssignBestRollToUnitCommander(getChkAssignBestRollToUnitCommander().isSelected());
        options.setGroupByWeight(getChkGroupByWeight().isSelected());

        // Unit
        options.setForceNamingType(getForceNamingType());
        options.setGenerateForceIcons(getChkGenerateForceIcons().isSelected());

        // Finances
        options.setStartingCash((Integer) getSpnStartingCash().getValue());
        options.setRandomizeStartingCash(getChkRandomizeStartingCash().isSelected());
        options.setMinimumStartingFloat((Integer) getSpnMinimumStartingFloat().getValue());
        options.setPayForSetup(getChkPayForSetup().isSelected());
        options.setStartingLoan(getChkStartingLoan().isSelected());
        options.setPayForPersonnel(getChkPayForPersonnel().isSelected());
        options.setPayForUnits(getChkPayForUnits().isSelected());
        options.setPayForParts(getChkPayForParts().isSelected());
        options.setPayForAmmunition(getChkPayForAmmunition().isSelected());

        return options;
    }

    public boolean validateOptions() {
        return ((int) getSpnCompanyCount().getValue() + (int) getSpnIndividualLanceCount().getValue()) > 0;
    }
    //endregion Options

    //region File I/O
    public void importOptionsFromXML() {
        File file = FileDialogs.openCompanyGenerationOptions(getFrame()).orElse(null);
        setOptions(CompanyGenerationOptions.parseFromXML(file, getCampaign()));
    }

    public void exportOptionsToXML() {
        File file = FileDialogs.saveCompanyGenerationOptions(getFrame()).orElse(null);
        createOptionsFromPanel().writeToFile(file);
    }
    //endregion File I/O

    //region Static Classes
    private static class FactionChoice {
        //region Variable Declarations
        private Faction faction;
        private int year;
        private String displayName;
        //endregion Variable Declarations

        //region Constructors
        public FactionChoice(Faction faction, int year) {
            setFaction(faction);
            setYear(year);
            setDisplayName();
        }
        //endregion Constructors

        //region Getters/Setters
        public Faction getFaction() {
            return faction;
        }

        public void setFaction(Faction faction) {
            this.faction = faction;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        private void setDisplayName() {
            this.displayName = String.format("%s [%s]", getFaction().getFullName(getYear()),
                    getFaction().getShortName());
        }
        //endregion Getters/Setters

        @Override
        public String toString() {
            return displayName;
        }
    }

    private static class RoleToSpinner {
        //region Variable Declarations
        private int role;
        private JSpinner spinner;
        //endregion Variable Declarations

        //region Constructors
        public RoleToSpinner(final int role, final JSpinner spinner) {
            setRole(role);
            setSpinner(spinner);
        }
        //endregion Constructors

        //region Getters/Setters
        public int getRole() {
            return role;
        }

        public void setRole(int role) {
            this.role = role;
        }

        public JSpinner getSpinner() {
            return spinner;
        }

        public int getValue() {
            return (Integer) getSpinner().getValue();
        }

        public void setSpinner(JSpinner spinner) {
            this.spinner = spinner;
        }
        //endregion Getters/Setters
    }
    //endregion Static Classes
}
