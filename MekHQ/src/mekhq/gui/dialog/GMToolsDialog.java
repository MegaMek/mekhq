/*
 * GMToolsDialog.java
 *
 * Copyright (c) 2013-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.JTabbedPanePreference;
import megamek.client.ui.preferences.JTextFieldPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.Messages;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Clan;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.gui.CampaignGUI;
import mekhq.gui.baseComponents.AbstractMHQDialog;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Predicate;

public class GMToolsDialog extends AbstractMHQDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 7724064095803583812L;

    private final CampaignGUI gui;
    private final Person person;

    //region GUI Variables
    private JTabbedPane tabbedPane;

    //region General Tab
    private JSpinner spnDiceCount;
    private JSpinner spnDiceNumber;
    private JSpinner spnDiceSides;
    private JLabel lblTotalDiceResult;
    private JTextPane txtIndividualDiceResults;

    private MMComboBox<FactionDisplay> comboRATFaction;
    private JTextField txtYear;
    private MMComboBox<String> comboQuality;
    private MMComboBox<String> comboUnitType;
    private MMComboBox<String> comboUnitWeight;
    private JLabel lblUnitPicked;
    private MechSummary lastRolledUnit;
    //endregion General Tab

    //region Name Tab
    private MMComboBox<String> comboEthnicCode;
    private MMComboBox<Gender> comboGender;
    private MMComboBox<FactionDisplay> comboNameGeneratorFaction;
    private JCheckBox chkClanner;
    private JSpinner spnNameNumber;
    private JLabel lblCurrentName;
    private JTextArea txtNamesGenerated;
    private String[] lastGeneratedName;

    private JSpinner spnCallsignNumber;
    private JLabel lblCurrentCallsign;
    private JTextArea lblCallsignsGenerated;
    private String lastGeneratedCallsign;

    private MMComboBox<Clan> comboOriginClan;
    private MMComboBox<Integer> comboBloodnameEra;
    private MMComboBox<Phenotype> comboPhenotype;
    private JLabel lblCurrentBloodname;
    private JLabel lblBloodnameGenerated;
    private JLabel lblOriginClanGenerated;
    private JLabel lblPhenotypeGenerated;
    private JLabel lblBloodnameWarning;
    private String lastGeneratedBloodname;
    //endregion Name Tab
    //endregion GUI Variables

    //region Constants
    private static final String[] QUALITY_NAMES = {"F", "D", "C", "B", "A", "A*"};
    private static final String[] WEIGHT_NAMES = {"Light", "Medium", "Heavy", "Assault"};
    private static final Integer[] BLOODNAME_ERAS = {2807, 2825, 2850, 2900, 2950, 3000, 3050, 3060, 3075, 3085, 3100};
    //endregion Constants
    //endregion Variable Declarations

    //region Constructors
    public GMToolsDialog(final JFrame frame, final CampaignGUI gui, final @Nullable Person person) {
        super(frame, "GMToolsDialog", "GMToolsDialog.title");
        this.gui = gui;
        this.person = person;
        initialize();
        setValuesFromPerson();
        validateBloodnameInput();
    }
    //endregion Constructors

    //region Getters and Setters
    public CampaignGUI getGUI() {
        return gui;
    }

    public Person getPerson() {
        return person;
    }

    //region GUI Variables
    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public void setTabbedPane(final JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    //region General Tab
    public JSpinner getSpnDiceCount() {
        return spnDiceCount;
    }

    public void setSpnDiceCount(final JSpinner spnDiceCount) {
        this.spnDiceCount = spnDiceCount;
    }

    public JSpinner getSpnDiceNumber() {
        return spnDiceNumber;
    }

    public void setSpnDiceNumber(final JSpinner spnDiceNumber) {
        this.spnDiceNumber = spnDiceNumber;
    }

    public JSpinner getSpnDiceSides() {
        return spnDiceSides;
    }

    public void setSpnDiceSides(final JSpinner spnDiceSides) {
        this.spnDiceSides = spnDiceSides;
    }

    public JLabel getLblTotalDiceResult() {
        return lblTotalDiceResult;
    }

    public void setLblTotalDiceResult(final JLabel lblTotalDiceResult) {
        this.lblTotalDiceResult = lblTotalDiceResult;
    }

    public JTextPane getTxtIndividualDiceResults() {
        return txtIndividualDiceResults;
    }

    public void setTxtIndividualDiceResults(final JTextPane txtIndividualDiceResults) {
        this.txtIndividualDiceResults = txtIndividualDiceResults;
    }

    public MMComboBox<FactionDisplay> getComboRATFaction() {
        return comboRATFaction;
    }

    public void setComboRATFaction(final MMComboBox<FactionDisplay> comboRATFaction) {
        this.comboRATFaction = comboRATFaction;
    }

    public JTextField getTxtYear() {
        return txtYear;
    }

    public void setTxtYear(final JTextField txtYear) {
        this.txtYear = txtYear;
    }

    public MMComboBox<String> getComboQuality() {
        return comboQuality;
    }

    public void setComboQuality(final MMComboBox<String> comboQuality) {
        this.comboQuality = comboQuality;
    }

    public MMComboBox<String> getComboUnitType() {
        return comboUnitType;
    }

    public void setComboUnitType(final MMComboBox<String> comboUnitType) {
        this.comboUnitType = comboUnitType;
    }

    public MMComboBox<String> getComboUnitWeight() {
        return comboUnitWeight;
    }

    public void setComboUnitWeight(final MMComboBox<String> comboUnitWeight) {
        this.comboUnitWeight = comboUnitWeight;
    }

    public JLabel getLblUnitPicked() {
        return lblUnitPicked;
    }

    public void setLblUnitPicked(final JLabel lblUnitPicked) {
        this.lblUnitPicked = lblUnitPicked;
    }

    public MechSummary getLastRolledUnit() {
        return lastRolledUnit;
    }

    public void setLastRolledUnit(final MechSummary lastRolledUnit) {
        this.lastRolledUnit = lastRolledUnit;
    }
    //endregion General Tab

    //region Name Tab

    public MMComboBox<String> getComboEthnicCode() {
        return comboEthnicCode;
    }

    public void setComboEthnicCode(final MMComboBox<String> comboEthnicCode) {
        this.comboEthnicCode = comboEthnicCode;
    }

    public MMComboBox<Gender> getComboGender() {
        return comboGender;
    }

    public void setComboGender(final MMComboBox<Gender> comboGender) {
        this.comboGender = comboGender;
    }

    public MMComboBox<FactionDisplay> getComboNameGeneratorFaction() {
        return comboNameGeneratorFaction;
    }

    public void setComboNameGeneratorFaction(final MMComboBox<FactionDisplay> comboNameGeneratorFaction) {
        this.comboNameGeneratorFaction = comboNameGeneratorFaction;
    }

    public JCheckBox getChkClanner() {
        return chkClanner;
    }

    public void setChkClanner(final JCheckBox chkClanner) {
        this.chkClanner = chkClanner;
    }

    public JSpinner getSpnNameNumber() {
        return spnNameNumber;
    }

    public void setSpnNameNumber(final JSpinner spnNameNumber) {
        this.spnNameNumber = spnNameNumber;
    }

    public JLabel getLblCurrentName() {
        return lblCurrentName;
    }

    public void setLblCurrentName(final JLabel lblCurrentName) {
        this.lblCurrentName = lblCurrentName;
    }

    public JTextArea getTxtNamesGenerated() {
        return txtNamesGenerated;
    }

    public void setTxtNamesGenerated(final JTextArea txtNamesGenerated) {
        this.txtNamesGenerated = txtNamesGenerated;
    }

    public String[] getLastGeneratedName() {
        return lastGeneratedName;
    }

    public void setLastGeneratedName(final String... lastGeneratedName) {
        this.lastGeneratedName = lastGeneratedName;
    }

    public JSpinner getSpnCallsignNumber() {
        return spnCallsignNumber;
    }

    public void setSpnCallsignNumber(final JSpinner spnCallsignNumber) {
        this.spnCallsignNumber = spnCallsignNumber;
    }

    public JLabel getLblCurrentCallsign() {
        return lblCurrentCallsign;
    }

    public void setLblCurrentCallsign(JLabel lblCurrentCallsign) {
        this.lblCurrentCallsign = lblCurrentCallsign;
    }

    public JTextArea getLblCallsignsGenerated() {
        return lblCallsignsGenerated;
    }

    public void setLblCallsignsGenerated(final JTextArea lblCallsignsGenerated) {
        this.lblCallsignsGenerated = lblCallsignsGenerated;
    }

    public String getLastGeneratedCallsign() {
        return lastGeneratedCallsign;
    }

    public void setLastGeneratedCallsign(final String lastGeneratedCallsign) {
        this.lastGeneratedCallsign = lastGeneratedCallsign;
    }

    public MMComboBox<Clan> getComboOriginClan() {
        return comboOriginClan;
    }

    public void setComboOriginClan(final MMComboBox<Clan> comboOriginClan) {
        this.comboOriginClan = comboOriginClan;
    }

    public MMComboBox<Integer> getComboBloodnameEra() {
        return comboBloodnameEra;
    }

    public void setComboBloodnameEra(final MMComboBox<Integer> comboBloodnameEra) {
        this.comboBloodnameEra = comboBloodnameEra;
    }

    public MMComboBox<Phenotype> getComboPhenotype() {
        return comboPhenotype;
    }

    public void setComboPhenotype(final MMComboBox<Phenotype> comboPhenotype) {
        this.comboPhenotype = comboPhenotype;
    }

    public JLabel getLblCurrentBloodname() {
        return lblCurrentBloodname;
    }

    public void setLblCurrentBloodname(final JLabel lblCurrentBloodname) {
        this.lblCurrentBloodname = lblCurrentBloodname;
    }

    public JLabel getLblBloodnameGenerated() {
        return lblBloodnameGenerated;
    }

    public void setLblBloodnameGenerated(final JLabel lblBloodnameGenerated) {
        this.lblBloodnameGenerated = lblBloodnameGenerated;
    }

    public JLabel getLblOriginClanGenerated() {
        return lblOriginClanGenerated;
    }

    public void setLblOriginClanGenerated(final JLabel lblOriginClanGenerated) {
        this.lblOriginClanGenerated = lblOriginClanGenerated;
    }

    public JLabel getLblPhenotypeGenerated() {
        return lblPhenotypeGenerated;
    }

    public void setLblPhenotypeGenerated(final JLabel lblPhenotypeGenerated) {
        this.lblPhenotypeGenerated = lblPhenotypeGenerated;
    }

    public JLabel getLblBloodnameWarning() {
        return lblBloodnameWarning;
    }

    public void setLblBloodnameWarning(final JLabel lblBloodnameWarning) {
        this.lblBloodnameWarning = lblBloodnameWarning;
    }

    public String getLastGeneratedBloodname() {
        return lastGeneratedBloodname;
    }

    public void setLastGeneratedBloodname(final String lastGeneratedBloodname) {
        this.lastGeneratedBloodname = lastGeneratedBloodname;
    }
    //endregion Name Tab
    //endregion GUI Variables
    //endregion Getters and Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setTabbedPane(new JTabbedPane());
        getTabbedPane().setName("GMToolsTabbedPane");
        getTabbedPane().addTab(resources.getString("generalTab.title"), createGeneralTab());
        getTabbedPane().addTab(resources.getString("namesTab.title"), createNamesTab());
        //getTabbedPane().addTab(resources.getString("personnelModuleTab.title"), createPersonnelModuleTab());
        return getTabbedPane();
    }

    //region General Tab
    private JScrollPane createGeneralTab() {

    }

    //region Dice Panel Initialization
    private JPanel getDiceRoller() {
        int gridx = 0, gridy = 0;

        JPanel dicePanel = new JPanel(new GridBagLayout());
        dicePanel.setName("dicePanel");
        dicePanel.setBorder(BorderFactory.createTitledBorder(resources.getString("dicePanel.text")));

        countDice = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        countDice.setName("countDice");
        dicePanel.add(countDice, newGridBagConstraints(gridx++, gridy));

        JLabel rolls = new JLabel(resources.getString("rolls.text"));
        dicePanel.add(rolls, newGridBagConstraints(gridx++, gridy));

        numberDice = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        numberDice.setName("numberDice");
        dicePanel.add(numberDice, newGridBagConstraints(gridx++, gridy));

        JLabel sides = new JLabel(resources.getString("sides.text"));
        sides.setName("sides");
        dicePanel.add(sides, newGridBagConstraints(gridx++, gridy));

        spnNumberOfSides = new JSpinner(new SpinnerNumberModel(6, 1, 200, 1));
        spnNumberOfSides.setName("sizeDice");
        ((JSpinner.DefaultEditor) spnNumberOfSides.getEditor()).getTextField().setEditable(true);
        dicePanel.add(spnNumberOfSides, newGridBagConstraints(gridx++, gridy++));

        gridx = 0;

        JLabel totalDiceResultLabel = new JLabel(resources.getString("totalDiceResultsLabel.text"));
        totalDiceResultLabel.setName("totalDiceResultsLabel");
        dicePanel.add(totalDiceResultLabel, newGridBagConstraints(gridx++, gridy));

        lblTotalDiceResult = new JLabel("-");
        lblTotalDiceResult.setName("totalDiceResult");
        dicePanel.add(lblTotalDiceResult, newGridBagConstraints(gridx++, gridy));

        JButton diceRoll = new JButton(resources.getString("diceRoll.text"));
        diceRoll.setName("diceRoll");
        diceRoll.addActionListener(evt -> performDiceRoll());
        dicePanel.add(diceRoll, newGridBagConstraints(gridx++, gridy++, 2, 1));

        gridx = 0;

        JLabel individualDiceResultsLabel = new JLabel(resources.getString("individualDiceResultsLabel.text"));
        individualDiceResultsLabel.setName("individualDiceResultsLabel");
        dicePanel.add(individualDiceResultsLabel, newGridBagConstraints(gridx++, gridy));

        txtIndividualDiceResults = new JTextPane();
        txtIndividualDiceResults.setText("-");
        txtIndividualDiceResults.setName("individualDiceResults");
        txtIndividualDiceResults.setEditable(false);
        dicePanel.add(txtIndividualDiceResults, newGridBagConstraints(gridx++, gridy, 3, 1));

        // Layout the Panel
        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("dicePanel.title")));
        panel.setName("dicePanel");
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setVerticalGroup(
                layout.createSequentialGroup()
                        .addComponent(chkUseTactics)
                        .addComponent(chkUseInitiativeBonus)
                        .addComponent(chkUseToughness)
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging)
                        .addComponent(chkUseTransfers)
        );

        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(chkUseTactics)
                        .addComponent(chkUseInitiativeBonus)
                        .addComponent(chkUseToughness)
                        .addComponent(chkUseArtillery)
                        .addComponent(chkUseAbilities)
                        .addComponent(chkUseEdge)
                        .addComponent(chkUseSupportEdge)
                        .addComponent(chkUseImplants)
                        .addComponent(chkUseAlternativeQualityAveraging)
                        .addComponent(chkUseTransfers)
        );

        return panel;
    }
    //endregion Dice Panel Initialization

    //region RAT Roller Initialization
    private JPanel getRATRoller() {
        JPanel ratPanel = new JPanel(new GridBagLayout());
        ratPanel.setName("ratPanel");
        ratPanel.setBorder(BorderFactory.createTitledBorder(resources.getString("ratPanel.text")));

        JLabel yearLabel = new JLabel(resources.getString("yearLabel.text"));
        yearLabel.setName("yearLabel");
        ratPanel.add(yearLabel, newGridBagConstraints(0, 0));

        txtYear = new JTextField(5);
        txtYear.setText(String.valueOf(getGUI().getCampaign().getGameYear()));
        txtYear.setName("yearPicker");
        ratPanel.add(txtYear, newGridBagConstraints(0, 1));

        JLabel factionLabel = new JLabel(resources.getString("factionLabel.text"));
        factionLabel.setName("factionLabel");
        ratPanel.add(factionLabel, newGridBagConstraints(1, 0, 2, 1));

        List<FactionChoice> factionChoices = getFactionChoices((getPerson() == null)
                ? getGUI().getCampaign().getGameYear() : getPerson().getBirthday().getYear());
        DefaultComboBoxModel<FactionChoice> factionModel = new DefaultComboBoxModel<>(factionChoices.toArray(new FactionChoice[]{}));
        comboRATFaction = new JComboBox<>(factionModel);
        comboRATFaction.setName("factionPicker");
        comboRATFaction.setSelectedIndex(0);
        ratPanel.add(comboRATFaction, newGridBagConstraints(1, 1, 2, 1));

        JLabel qualityLabel = new JLabel(resources.getString("qualityLabel.text"));
        qualityLabel.setName("qualityLabel");
        ratPanel.add(qualityLabel, newGridBagConstraints(3, 0));

        comboQuality = new JComboBox<>(QUALITY_NAMES);
        comboQuality.setName("qualityPicker");
        ratPanel.add(comboQuality, newGridBagConstraints(3, 1));

        JLabel unitTypeLabel = new JLabel(resources.getString("unitTypeLabel.text"));
        unitTypeLabel.setName("unitTypeLabel");
        ratPanel.add(unitTypeLabel, newGridBagConstraints(4, 0));

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        for (int ut = 0; ut < UnitType.SIZE; ut++) {
            if (getGUI().getCampaign().getUnitGenerator().isSupportedUnitType(ut)) {
                unitTypeModel.addElement(UnitType.getTypeName(ut));
            }
        }
        comboUnitType = new JComboBox<>(unitTypeModel);
        comboUnitType.setName("unitTypePicker");
        comboUnitType.addItemListener(ev -> {
            final String unitType = Objects.requireNonNull(comboUnitType.getSelectedItem());
            comboUnitWeight.setEnabled(unitType.equals("Mek") || unitType.equals("Tank")
                    || unitType.equals("Aero"));
        });
        ratPanel.add(comboUnitType, newGridBagConstraints(4, 1));

        JLabel unitWeightLabel = new JLabel(resources.getString("unitWeightLabel.text"));
        unitWeightLabel.setName("unitWeightLabel");
        ratPanel.add(unitWeightLabel, newGridBagConstraints(5, 0));

        comboUnitWeight = new JComboBox<>(WEIGHT_NAMES);
        comboUnitWeight.setName("unitWeightPicker");
        ratPanel.add(comboUnitWeight, newGridBagConstraints(5, 1));

        lblUnitPicked = new JLabel("-");
        lblUnitPicked.setName("unitPicked");
        ratPanel.add(lblUnitPicked, newGridBagConstraints(0, 2, 4, 1));

        JButton ratRoll = new JButton(resources.getString("ratRoll.text"));
        ratRoll.setName("ratRoll");
        ratRoll.addActionListener(evt -> setLastRolledUnit(performRATRoll()));
        ratPanel.add(ratRoll, newGridBagConstraints(5, 3));

        if (getGUI().getCampaign().isGM()) {
            JButton addRandomUnit = new JButton(resources.getString("addRandomUnit.text"));
            addRandomUnit.setName("addRandomUnit");
            addRandomUnit.addActionListener(evt -> addRATRolledUnit());
            ratPanel.add(addRandomUnit, newGridBagConstraints(6, 3));
        }
        return ratPanel;
    }
    //endregion RAT Roller Initialization
    //endregion General Tab

    //region Names Tab
    private JScrollPane createNamesTab() {

    }

    //region Name Generator Panel Initialization
    private JPanel getNameGenerator() {
        JPanel namePanel = new JPanel(new GridBagLayout());
        namePanel.setName("namePanel");
        namePanel.setBorder(BorderFactory.createTitledBorder(resources.getString("namePanel.text")));

        int gridx = 0, gridxMax;

        JLabel genderLabel = new JLabel(resources.getString("genderLabel.text"));
        genderLabel.setName("genderLabel");
        namePanel.add(genderLabel, newGridBagConstraints(gridx, 0));

        DefaultComboBoxModel<Gender> genderModel = new DefaultComboBoxModel<>(Gender.getExternalOptions().toArray(new Gender[]{}));
        comboGender = new JComboBox<>(genderModel);
        comboGender.setName("genderPicker");
        comboGender.setSelectedIndex(0);
        namePanel.add(comboGender, newGridBagConstraints(gridx++, 1));

        JLabel originFactionLabel = new JLabel(resources.getString("originFactionLabel.text"));
        originFactionLabel.setName("originFactionLabel");
        namePanel.add(originFactionLabel, newGridBagConstraints(gridx, 0));

        List<FactionChoice> factionChoices = getFactionChoices((getPerson() == null)
                ? getGUI().getCampaign().getGameYear() : getPerson().getBirthday().getYear());
        DefaultComboBoxModel<FactionChoice> factionModel = new DefaultComboBoxModel<>(factionChoices.toArray(new FactionChoice[]{}));
        comboNameGeneratorFaction = new JComboBox<>(factionModel);
        comboNameGeneratorFaction.setName("nameGeneratorFactionPicker");
        comboNameGeneratorFaction.setSelectedIndex(0);
        namePanel.add(comboNameGeneratorFaction, newGridBagConstraints(gridx++, 1));

        JLabel historicalEthnicityLabel = new JLabel(resources.getString("historicalEthnicityLabel.text"));
        historicalEthnicityLabel.setName("historicalEthnicityLabel");
        namePanel.add(historicalEthnicityLabel, newGridBagConstraints(gridx, 0));

        DefaultComboBoxModel<String> historicalEthnicityModel = new DefaultComboBoxModel<>();
        historicalEthnicityModel.addElement(resources.getString("factionWeighted.text"));
        for (String historicalEthnicity : RandomNameGenerator.getInstance().getHistoricalEthnicity().values()) {
            historicalEthnicityModel.addElement(historicalEthnicity);
        }
        comboEthnicCode = new JComboBox<>(historicalEthnicityModel);
        comboEthnicCode.setName("ethnicCodePicker");
        comboEthnicCode.setSelectedIndex(0);
        comboEthnicCode.addActionListener(evt -> comboNameGeneratorFaction.setEnabled(comboEthnicCode.getSelectedIndex() == 0));
        namePanel.add(comboEthnicCode, newGridBagConstraints(gridx++, 1));

        JLabel clannerLabel = new JLabel(resources.getString("clannerLabel.text"));
        clannerLabel.setName("clannerLabel");
        namePanel.add(clannerLabel, newGridBagConstraints(gridx, 0));

        chkClanner = new JCheckBox();
        chkClanner.setName("clannerPicker");
        chkClanner.getAccessibleContext().setAccessibleName(resources.getString("clannerLabel.text"));
        namePanel.add(chkClanner, newGridBagConstraints(gridx++, 1));

        gridxMax = gridx - 1;
        gridx = 0;

        if (getPerson() != null) {
            JLabel currentNameLabel = new JLabel(resources.getString("currentNameLabel.text"));
            currentNameLabel.setName("currentNameLabel");
            namePanel.add(currentNameLabel, newGridBagConstraints(gridx++, 3));

            lblCurrentName = new JLabel("-");
            lblCurrentName.setName("currentName");
            namePanel.add(lblCurrentName, newGridBagConstraints(gridx++, 3));
        }

        JLabel nameGeneratedLabel = new JLabel(resources.getString((getPerson() == null)
                ? "namesGeneratedLabel.text" : "nameGeneratedLabel.text"));
        nameGeneratedLabel.setName("nameGeneratedLabel");
        namePanel.add(nameGeneratedLabel, newGridBagConstraints(gridx++, 3));

        txtNamesGenerated = new JTextArea("-");
        txtNamesGenerated.setName("nameGenerated");
        namePanel.add(txtNamesGenerated, newGridBagConstraints(gridx++, 3));

        JButton generateNameButton = new JButton(resources.getString((getPerson() == null)
                ? "generateNamesButton.text" : "generateNameButton.text"));
        generateNameButton.setName("generateNameButton");
        generateNameButton.addActionListener(evt -> {
            if (getPerson() == null) {
                generateNames();
            } else {
                generateName();
            }
        });
        namePanel.add(generateNameButton, newGridBagConstraints(gridxMax--, 4));

        if (getPerson() == null) {
            spnNameNumber = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
            spnNameNumber.setName("numNames");
            ((JSpinner.DefaultEditor) spnNameNumber.getEditor()).getTextField().setEditable(true);
            namePanel.add(spnNameNumber, newGridBagConstraints(gridxMax--, 4));
        } else {
            JButton assignNameButton = new JButton(resources.getString("assignNameButton.text"));
            assignNameButton.setName("assignNameButton");
            assignNameButton.addActionListener(evt -> assignName());
            namePanel.add(assignNameButton, newGridBagConstraints(gridxMax--, 4));
        }

        return namePanel;
    }
    //endregion Name Generator Panel Initialization

    //region Callsign Generator Panel Initialization
    private JPanel getCallsignGenerator() {
        JPanel callsignPanel = new JPanel(new GridBagLayout());
        callsignPanel.setName("callsignPanel");
        callsignPanel.setBorder(BorderFactory.createTitledBorder(resources.getString("callsignPanel.text")));

        int gridx = 0, gridy = 0;

        if (getPerson() != null) {
            JLabel currentCallsignLabel = new JLabel(resources.getString("currentCallsignLabel.text"));
            currentCallsignLabel.setName("currentCallsignLabel");
            callsignPanel.add(currentCallsignLabel, newGridBagConstraints(gridx++, gridy));

            lblCurrentCallsign = new JLabel("-");
            lblCurrentCallsign.setName("currentCallsign");
            callsignPanel.add(lblCurrentCallsign, newGridBagConstraints(gridx++, gridy));
        }

        JLabel callsignGeneratedLabel = new JLabel(resources.getString((getPerson() == null)
                ? "callsignsGeneratedLabel.text" : "callsignGeneratedLabel.text"));
        callsignGeneratedLabel.setName("callsignGeneratedLabel");
        callsignPanel.add(callsignGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        lblCallsignsGenerated = new JTextArea("-");
        lblCallsignsGenerated.setName("callsignGenerated");
        callsignPanel.add(lblCallsignsGenerated, newGridBagConstraints(gridx++, gridy));

        gridy++;

        JButton generateCallsignButton = new JButton(resources.getString((getPerson() == null)
                ? "generateCallsignsButton.text" : "generateCallsignButton.text"));
        generateCallsignButton.setName("generateCallsignButton");
        generateCallsignButton.addActionListener(evt -> {
            if (getPerson() == null) {
                generateCallsigns();
            } else {
                generateCallsign();
            }
        });
        callsignPanel.add(generateCallsignButton, newGridBagConstraints(gridx--, gridy));

        if (getPerson() == null) {
            spnCallsignNumber = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
            spnCallsignNumber.setName("numCallsigns");
            ((JSpinner.DefaultEditor) spnCallsignNumber.getEditor()).getTextField().setEditable(true);
            callsignPanel.add(spnCallsignNumber, newGridBagConstraints(gridx--, gridy));
        } else {
            JButton assignCallsignButton = new JButton(resources.getString("assignCallsignButton.text"));
            assignCallsignButton.setName("assignCallsignButton");
            assignCallsignButton.addActionListener(evt -> assignCallsign());
            callsignPanel.add(assignCallsignButton, newGridBagConstraints(gridx--, gridy));
        }

        return callsignPanel;
    }
    //endregion Callsign Generator Panel Initialization

    //region Bloodname Generator Panel Initialization
    private JPanel getBloodnameGenerator() {
        JPanel namePanel = new JPanel(new GridBagLayout());
        namePanel.setName("bloodnamePanel");
        namePanel.setBorder(BorderFactory.createTitledBorder(resources.getString("bloodnamePanel.text")));

        int gridx = 0, gridy = 0, gridxMax;

        JLabel originClanLabel = new JLabel(resources.getString("originClanLabel.text"));
        originClanLabel.setName("originClanLabel");
        namePanel.add(originClanLabel, newGridBagConstraints(gridx, gridy));

        clans.addAll(Clan.getClans());
        clans.sort(Comparator.comparing(o -> o.getFullName(bloodnameYear)));
        DefaultComboBoxModel<String> originClanModel = new DefaultComboBoxModel<>();
        for (Clan clan : clans) {
            originClanModel.addElement(clan.getFullName(bloodnameYear));
        }
        comboOriginClan = new JComboBox<>(originClanModel);
        comboOriginClan.setName("originClanPicker");
        comboOriginClan.setSelectedIndex(0);
        comboOriginClan.addActionListener(evt -> validateBloodnameInput());
        namePanel.add(comboOriginClan, newGridBagConstraints(gridx++, gridy + 1));

        JLabel bloodnameEraLabel = new JLabel(resources.getString("bloodnameEraLabel.text"));
        bloodnameEraLabel.setName("bloodnameEraLabel");
        namePanel.add(bloodnameEraLabel, newGridBagConstraints(gridx, gridy));

        comboBloodnameEra = new JComboBox<>(BLOODNAME_ERAS);
        comboBloodnameEra.setName("bloodnameEraPicker");
        comboBloodnameEra.setSelectedIndex(0);
        comboBloodnameEra.addActionListener(evt -> validateBloodnameInput());
        namePanel.add(comboBloodnameEra, newGridBagConstraints(gridx++, gridy + 1));

        JLabel phenotypeLabel = new JLabel(resources.getString("phenotypeLabel.text"));
        phenotypeLabel.setName("phenotypeLabel");
        namePanel.add(phenotypeLabel, newGridBagConstraints(gridx, gridy));

        DefaultComboBoxModel<Phenotype> phenotypeModel = new DefaultComboBoxModel<>();
        phenotypeModel.addElement(Phenotype.GENERAL);
        for (Phenotype phenotype : Phenotype.getExternalPhenotypes()) {
            phenotypeModel.addElement(phenotype);
        }
        comboPhenotype = new JComboBox<>(phenotypeModel);
        comboPhenotype.setName("phenotypePicker");
        comboPhenotype.setSelectedItem(Phenotype.GENERAL);
        comboPhenotype.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText((value == null) ? ""
                        : (value instanceof Phenotype ? ((Phenotype) value).getGroupingName() : "ERROR"));

                return this;
            }
        });
        comboPhenotype.addActionListener(evt -> validateBloodnameInput());
        namePanel.add(comboPhenotype, newGridBagConstraints(gridx++, gridy + 1));

        gridx = 0;
        gridy = 2;

        if (getPerson() != null) {
            JLabel currentBloodnameLabel = new JLabel(resources.getString("currentBloodnameLabel.text"));
            currentBloodnameLabel.setName("currentBloodnameLabel");
            namePanel.add(currentBloodnameLabel, newGridBagConstraints(gridx++, gridy));

            lblCurrentBloodname = new JLabel("-");
            lblCurrentBloodname.setName("currentBloodname");
            namePanel.add(lblCurrentBloodname, newGridBagConstraints(gridx++, gridy));
        }

        JLabel bloodnameGeneratedLabel = new JLabel(resources.getString("bloodnameGeneratedLabel.text"));
        bloodnameGeneratedLabel.setName("nameGeneratedLabel");
        namePanel.add(bloodnameGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        lblBloodnameGenerated = new JLabel("-");
        lblBloodnameGenerated.setName("bloodnameGenerated");
        namePanel.add(lblBloodnameGenerated, newGridBagConstraints(gridx++, gridy++));

        gridx = 0;

        JLabel originClanGeneratedLabel = new JLabel(resources.getString("originClanGeneratedLabel.text"));
        originClanGeneratedLabel.setName("originClanGeneratedLabel");
        namePanel.add(originClanGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        lblOriginClanGenerated = new JLabel("-");
        lblOriginClanGenerated.setName("originClanGenerated");
        namePanel.add(lblOriginClanGenerated, newGridBagConstraints(gridx++, gridy));

        JLabel phenotypeGeneratedLabel = new JLabel(resources.getString("phenotypeGeneratedLabel.text"));
        phenotypeGeneratedLabel.setName("phenotypeGeneratedLabel");
        namePanel.add(phenotypeGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        lblPhenotypeGenerated = new JLabel("-");
        lblPhenotypeGenerated.setName("phenotypeGenerated");
        namePanel.add(lblPhenotypeGenerated, newGridBagConstraints(gridx++, gridy++));

        gridxMax = gridx - 1;
        gridx = 0;

        lblBloodnameWarning = new JLabel("");
        GridBagConstraints gridBagConstraints = newGridBagConstraints(gridx++, gridy, 1, 1);
        gridBagConstraints.gridwidth = 2;
        namePanel.add(lblBloodnameWarning, gridBagConstraints);

        if (getPerson() != null) {
            JButton assignBloodnameButton = new JButton(resources.getString("assignBloodnameButton.text"));
            assignBloodnameButton.setName("assignBloodnameButton");
            assignBloodnameButton.addActionListener(evt -> assignBloodname());
            namePanel.add(assignBloodnameButton, newGridBagConstraints(gridxMax--, gridy));
        }

        JButton generateBloodnameButton = new JButton(resources.getString("generateBloodnameButton.text"));
        generateBloodnameButton.setName("generateNameButton");
        generateBloodnameButton.addActionListener(evt -> generateBloodname());
        namePanel.add(generateBloodnameButton, newGridBagConstraints(gridxMax--, gridy));

        return namePanel;
    }
    //endregion Bloodname Generator Panel Initialization
    //endregion Names Tab

    @Override
    protected void setCustomPreferences(final PreferencesNode preferences) {
        super.setCustomPreferences(preferences);
        preferences.manage(new JTabbedPanePreference(getTabbedPane()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDiceCount()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDiceNumber()));
        preferences.manage(new JIntNumberSpinnerPreference(getSpnDiceSides()));

        preferences.manage(new JComboBoxPreference(getComboRATFaction()));
        preferences.manage(new JTextFieldPreference(getTxtYear()));
        preferences.manage(new JComboBoxPreference(getComboQuality()));
        preferences.manage(new JComboBoxPreference(getComboUnitType()));
        preferences.manage(new JComboBoxPreference(getComboUnitWeight()));

        if (getSpnNameNumber() != null) {
            preferences.manage(new JIntNumberSpinnerPreference(getSpnNameNumber()));
        }

        if (getSpnCallsignNumber() != null) {
            preferences.manage(new JIntNumberSpinnerPreference(getSpnCallsignNumber()));
        }
    }

    private void setValuesFromPerson() {
        if (getPerson() == null) {
            return;
        }

        // Current Name is the Person's full name
        getLblCurrentName().setText(getPerson().getFullName());

        // Gender is set based on the person's gender
        getComboGender().setSelectedItem(getPerson().getGender().isExternal() ? getPerson().getGender()
                : getPerson().getGender().getExternalVariant());

        // Current Callsign is set if applicable
        if (!StringUtil.isNullOrEmpty(getPerson().getCallsign())) {
            getLblCurrentCallsign().setText(getPerson().getCallsign());
        }

        // We set the clanner value based on whether or not the person is a clanner
        getChkClanner().setSelected(getPerson().isClanner());

        // Now we figure out the person's origin faction
        final Faction faction = (getPerson() == null)
                ? getGUI().getCampaign().getFaction() : getPerson().getOriginFaction();
        getComboRATFaction().setSelectedItem(faction);
        getComboNameGeneratorFaction().setSelectedItem(faction);

        // Finally, we determine the default unit type
        for (int i = 0; i < getComboUnitType().getModel().getSize(); i++) {
            if (doesPersonPrimarilyDriveUnitType(UnitType.determineUnitTypeCode(getComboUnitType().getItemAt(i)))) {
                getComboUnitType().setSelectedIndex(i);
                break;
            }
        }

        if (!StringUtil.isNullOrEmpty(getPerson().getBloodname())) {
            getLblCurrentBloodname().setText(getPerson().getBloodname());
        }

        int year = getGUI().getCampaign().getGameYear();
        for (int i = BLOODNAME_ERAS.length - 1; i >= 0; i--) {
            if (BLOODNAME_ERAS[i] <= year) {
                getComboBloodnameEra().setSelectedIndex(i);
                year = BLOODNAME_ERAS[i];
                break;
            }
        }

        getComboOriginClan().setSelectedItem((getGUI().getCampaign().getFaction().isClan()
                ? getGUI().getCampaign().getFaction() : getPerson().getOriginFaction()).getFullName(year));

        getComboPhenotype().setSelectedItem(getPerson().getPhenotype());
    }

    /**
     * Determine if a person's primary role supports operating a
     * given unit type.
     */
    private boolean doesPersonPrimarilyDriveUnitType(final int unitType) {
        switch (unitType) {
            case UnitType.AERO:
                return getPerson().getPrimaryRole().isAerospacePilot();
            case UnitType.BATTLE_ARMOR:
                return getPerson().getPrimaryRole().isBattleArmour();
            case UnitType.CONV_FIGHTER:
                return getPerson().getPrimaryRole().isConventionalAircraftPilot()
                        || getPerson().getPrimaryRole().isAerospacePilot();
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.SMALL_CRAFT:
            case UnitType.WARSHIP:
                return getPerson().getPrimaryRole().isVesselPilot();
            case UnitType.INFANTRY:
                return getPerson().getPrimaryRole().isSoldier();
            case UnitType.MEK:
                return getPerson().getPrimaryRole().isMechWarrior();
            case UnitType.NAVAL:
                return getPerson().getPrimaryRole().isNavalVehicleDriver();
            case UnitType.PROTOMEK:
                return getPerson().getPrimaryRole().isProtoMechPilot();
            case UnitType.TANK:
                return getPerson().getPrimaryRole().isGroundVehicleDriver();
            case UnitType.VTOL:
                return getPerson().getPrimaryRole().isVTOLPilot();
            default:
                return false;
        }
    }
    //endregion Initialization

    //region ActionEvent Handlers
    public void performDiceRoll() {
        final List<Integer> individualDice = Compute.individualRolls((Integer) getSpnDiceCount().getValue(),
                (Integer) getSpnDiceNumber().getValue(), (Integer) getSpnDiceSides().getValue());
        getLblTotalDiceResult().setText(String.format(resources.getString("totalDiceResult.text"), individualDice.get(0)));

        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i < individualDice.size() - 1; i++) {
            sb.append(individualDice.get(i)).append(", ");
        }
        sb.append(individualDice.get(individualDice.size() - 1));

        getTxtIndividualDiceResults().setText((sb.length() > 0) ? sb.toString() : "-");
    }

    private @Nullable MechSummary performRATRoll() {
        try {
            final int targetYear = Integer.parseInt(getTxtYear().getText());
            final Predicate<MechSummary> predicate = summary ->
                    (!getGUI().getCampaign().getCampaignOptions().limitByYear() || (targetYear > summary.getYear()))
                            && (!summary.isClan() || getGUI().getCampaign().getCampaignOptions().allowClanPurchases())
                            && (summary.isClan() || getGUI().getCampaign().getCampaignOptions().allowISPurchases());
            final int unitType = UnitType.determineUnitTypeCode(getComboUnitType().getSelectedItem());
            final int unitWeight = getComboUnitWeight().isEnabled()
                    ? getComboUnitWeight().getSelectedIndex() + EntityWeightClass.WEIGHT_LIGHT
                    : AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED;

            final MechSummary summary = getGUI().getCampaign().getUnitGenerator()
                    .generate(Objects.requireNonNull(getComboRATFaction().getSelectedItem()).getFaction().getShortName(),
                            unitType, unitWeight, targetYear, getComboQuality().getSelectedIndex(), predicate);
            if (summary != null) {
                getLblUnitPicked().setText(summary.getName());
                return summary;
            }
        } catch (Exception ignored) {
            getLblUnitPicked().setText(Messages.getString("invalidYear.error"));
            return null;
        }

        getLblUnitPicked().setText(Messages.getString("noValidUnit.error"));
        return null;
    }

    private void addRATRolledUnit() {
        if (getLastRolledUnit() == null) {
            setLastRolledUnit(performRATRoll());
        }

        if (getLastRolledUnit() != null) {
            try {
                final Entity entity = new MechFileParser(getLastRolledUnit().getSourceFile(),
                        getLastRolledUnit().getEntryName()).getEntity();
                final Unit unit = getGUI().getCampaign().addNewUnit(entity, false, 0);
                if ((getPerson() != null) && (getPerson().getUnit() == null)) {
                    unit.addPilotOrSoldier(getPerson());
                    getPerson().setOriginalUnit(unit);
                }
                setLastRolledUnit(null);
            } catch (Exception e) {
                final String message = String.format(Messages.getString("entityLoadFailure.error"), getLastRolledUnit().getName(), getLastRolledUnit().getSourceFile());
                MekHQ.getLogger().error(message, e);
                getLblUnitPicked().setText(message);
            }
        }
    }

    private void generateName() {
        final String[] name = generateIndividualName();
        getTxtNamesGenerated().setText((name[0] + " " + name[1]).trim());
        setLastGeneratedName(name);
    }

    private void generateNames() {
        final StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < (Integer) getSpnNameNumber().getValue(); i++) {
            final String[] name = generateIndividualName();
            sj.add((name[0] + " " + name[1]).trim());
        }
        getTxtNamesGenerated().setText(sj.toString());
    }

    private String[] generateIndividualName() {
        final int ethnicCode = getComboEthnicCode().getSelectedIndex();
        final String[] name;

        if (ethnicCode == 0) {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                    getComboGender().getSelectedItem(), getChkClanner().isSelected(),
                    (Objects.requireNonNull(getComboNameGeneratorFaction().getSelectedItem()))
                            .getFaction().getShortName());
        } else {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplitWithEthnicCode(
                    getComboGender().getSelectedItem(), getChkClanner().isSelected(), ethnicCode);
        }
        return name;
    }

    private void assignName() {
        if (getLastGeneratedName() == null) {
            generateName();
        }

        getLblCurrentName().setText((getLastGeneratedName()[0] + " " + getLastGeneratedName()[1]).trim());
        getPerson().setGivenName(getLastGeneratedName()[0]);
        getPerson().setSurname(getLastGeneratedName()[1]);

        MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
    }

    private void generateCallsign() {
        getLblCallsignsGenerated().setText(RandomCallsignGenerator.getInstance().generate());
        setLastGeneratedCallsign(getLblCallsignsGenerated().getText());
    }

    private void generateCallsigns() {
        final StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < (Integer) getSpnCallsignNumber().getValue(); i++) {
            sj.add(RandomCallsignGenerator.getInstance().generate());
        }
        getLblCallsignsGenerated().setText(sj.toString());
    }

    private void assignCallsign() {
        if (getLastGeneratedCallsign() == null) {
            generateCallsign();
        }

        getLblCurrentCallsign().setText(getLastGeneratedCallsign());
        getPerson().setCallsign(getLastGeneratedCallsign());

        MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
    }

    private void generateBloodname() {
        Bloodname bloodname = Bloodname.randomBloodname(clans.get(originClan),
                selectedPhenotype, bloodnameYear);
        if (bloodname != null) {
            getLblBloodnameGenerated().setText(bloodname.getName() + " (" + bloodname.getFounder() + ")");
            getLblOriginClanGenerated().setText(Clan.getClan(bloodname.getOrigClan()).getFullName(bloodnameYear));
            getLblPhenotypeGenerated().setText(bloodname.getPhenotype().getGroupingName());
            setLastGeneratedBloodname(bloodname.getName());
        }
    }

    private void assignBloodname() {
        if (getLastGeneratedBloodname() == null) {
            generateBloodname();
        }

        getLblCurrentBloodname().setText(getLastGeneratedBloodname());
        getPerson().setBloodname(getLastGeneratedBloodname());

        MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
    }


    private void validateBloodnameInput() {
        originClan = comboOriginClan.getSelectedItem();
        bloodnameYear = BLOODNAME_ERAS[comboBloodnameEra.getSelectedIndex()];
        selectedPhenotype = comboPhenotype.getSelectedItem();

        if ((originClan < 0) || (selectedPhenotype == Phenotype.NONE) || (selectedPhenotype == null)) {
            return;
        }

        Clan selectedClan = clans.get(originClan);
        String txt = "<html>";

        if (bloodnameYear < selectedClan.getStartDate()) {
            for (int era : BLOODNAME_ERAS) {
                if (era >= selectedClan.getStartDate()) {
                    bloodnameYear = era;
                    txt += "<div>" + selectedClan.getFullName(bloodnameYear) + " formed in "
                            + selectedClan.getStartDate() + ". Using " + bloodnameYear + ".</div>";
                    break;
                }
            }

            if (bloodnameYear < selectedClan.getStartDate()) {
                bloodnameYear = selectedClan.getStartDate();
            }
        } else if (bloodnameYear > selectedClan.getEndDate()) {
            for (int i = BLOODNAME_ERAS.length - 1; i >= 0; i--) {
                if (BLOODNAME_ERAS[i] <= selectedClan.getEndDate()) {
                    bloodnameYear = BLOODNAME_ERAS[i];
                    txt += "<div>" + selectedClan.getFullName(bloodnameYear) + " ceased to existed in "
                            + selectedClan.getEndDate() + ". Using " + bloodnameYear + ".</div>";
                    break;
                }
            }

            if (bloodnameYear > selectedClan.getEndDate()) {
                bloodnameYear = selectedClan.getEndDate();
            }
        }

        if ((selectedPhenotype == Phenotype.PROTOMECH) && (bloodnameYear < 3060)) {
            txt += "<div>ProtoMechs did not exist in " + bloodnameYear + ". Using Aerospace.</div>";
            selectedPhenotype = Phenotype.AEROSPACE;
        } else if ((selectedPhenotype == Phenotype.NAVAL) && (!"CSR".equals(selectedClan.getGenerationCode()))) {
            txt += "<div>The Naval phenotype is unique to Clan Snow Raven. Using General.</div>";
            selectedPhenotype = Phenotype.GENERAL;
        } else if ((selectedPhenotype == Phenotype.VEHICLE) && (!"CHH".equals(selectedClan.getGenerationCode()))) {
            txt += "<div>The vehicle phenotype is unique to Clan Hell's Horses. Using General.</div>";
            selectedPhenotype = Phenotype.GENERAL;
        } else if ((selectedPhenotype == Phenotype.VEHICLE) && (bloodnameYear < 3100)) {
            txt += "<div>The vehicle phenotype began development in the 32nd century. Using 3100.</div>";
            bloodnameYear = 3100;
        }
        txt += "</html>";
        lblBloodnameWarning.setText(txt);
    }
    //endregion ActionEvent Handlers
}
