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

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.StringJoiner;
import java.util.function.Predicate;

import javax.swing.*;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.Messages;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Clan;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.gui.CampaignGUI;
import megamek.client.ui.preferences.JComboBoxPreference;
import megamek.client.ui.preferences.JIntNumberSpinnerPreference;
import megamek.client.ui.preferences.JTextFieldPreference;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;

public class GMToolsDialog extends JDialog {
    //region Variable Declarations
    private static final long serialVersionUID = 7724064095803583812L;

    //region GUI Variables
    private JSpinner numDice;
    private JSpinner sizeDice;
    private JLabel totalDiceResult;
    private JTextPane individualDiceResults;

    private JComboBox<FactionChoice> factionPicker;
    private JTextField yearPicker;
    private JComboBox<String> qualityPicker;
    private JComboBox<String> unitTypePicker;
    private JComboBox<String> unitWeightPicker;
    private JLabel unitPicked;

    private JComboBox<String> ethnicCodePicker;
    private JComboBox<Gender> genderPicker;
    private JComboBox<FactionChoice> nameGeneratorFactionPicker;
    private JCheckBox clannerPicker;
    private JSpinner numNames;
    private JLabel currentName;
    private JTextArea nameGenerated;

    private JSpinner numCallsigns;
    private JLabel currentCallsign;
    private JTextArea callsignGenerated;

    private JComboBox<String> originClanPicker;
    private JComboBox<Integer> bloodnameEraPicker;
    private JComboBox<Phenotype> phenotypePicker;
    private JLabel currentBloodname;
    private JLabel bloodnameGenerated;
    private JLabel originClanGenerated;
    private JLabel phenotypeGenerated;
    private JLabel bloodnameWarningLabel;
    //endregion GUI Variables

    //region Previously Generated Information
    /**
     * The last unit rolled, used when clicking 'Add Random Unit' after Roll for RAT is clicked
     */
    private MechSummary lastRolledUnit;

    /**
     * The last name rolled, used when clicking 'Set Generated Name' after Generate Name is clicked
     */
    private String[] lastGeneratedName;

    /**
     * The last callsign rolled, used when clicking 'Set Generated Callsign' after Generate Callsign is clicked
     */
    private String lastGeneratedCallsign;

    /**
     * The last bloodname rolled, used when clicking 'Set Generated Bloodname' after Generate Bloodname is clicked
     */
    private String lastGeneratedBloodname;
    //endregion Previously Generated Information

    //region Data Sources
    private CampaignGUI gui;
    private Person person;
    private List<Clan> clans = new ArrayList<>();
    //endregion Data Sources

    //region Bloodname Information
    private int originClan;
    private Phenotype selectedPhenotype;
    private int bloodnameYear;
    //endregion Bloodname Information

    //region Constants
    private static final String[] QUALITY_NAMES = {"F", "D", "C", "B", "A", "A*"};
    private static final String[] WEIGHT_NAMES = {"Light", "Medium", "Heavy", "Assault"};
    private static final Integer[] ERAS = {2807, 2825, 2850, 2900, 2950, 3000, 3050, 3060, 3075, 3085, 3100};
    //endregion Constants

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GMToolsDialog", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    /**
     * Creates a generic GM Tools dialog, with a dice roller
     * and a RAT roller that can add units to the campaign.
     */
    public GMToolsDialog(JFrame parent, CampaignGUI gui) {
        this(parent, gui, null);
    }

    /**
     * Creates a GM Tools dialog specific to a given person. The
     * dice roller and RAT roller are both available, however, the
     * RAT roller adds the unit directly to the person.
     */
    public GMToolsDialog(JFrame parent, CampaignGUI gui, @Nullable Person p) {
        super(parent, false);
        setGUI(gui);
        setPerson(p);
        setName("formGMTools");
        setTitle("GM Tools" + ((p != null) ? (" - " + p.getFullName()) : ""));
        getContentPane().setLayout(new GridBagLayout());
        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setUserPreferences();
        setValuesFromPerson();
        validateBloodnameInput();
    }
    //endregion Constructors

    //region Initialization
    private void initComponents() {
        getContentPane().add(getDiceRoller(), newGridBagConstraints(0,0));
        getContentPane().add(getRATRoller(), newGridBagConstraints(0,1));
        getContentPane().add(getNameGenerator(), newGridBagConstraints(0, 2));
        getContentPane().add(getCallsignGenerator(), newGridBagConstraints(0, 3));
        getContentPane().add(getBloodnameGenerator(), newGridBagConstraints(0, 4));
    }

    //region Dice Panel Initialization
    private JPanel getDiceRoller() {
        int gridx = 0, gridy = 0;

        JPanel dicePanel = new JPanel(new GridBagLayout());
        dicePanel.setName("dicePanel");
        dicePanel.setBorder(BorderFactory.createTitledBorder(resources.getString("dicePanel.text")));

        numDice = new JSpinner(new SpinnerNumberModel(2, 1, 100, 1));
        numDice.setName("numDice");
        ((JSpinner.DefaultEditor) numDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(numDice, newGridBagConstraints(gridx++, gridy));

        JLabel sides = new JLabel(resources.getString("sides.text"));
        sides.setName("sides");
        dicePanel.add(sides, newGridBagConstraints(gridx++, gridy));

        sizeDice = new JSpinner(new SpinnerNumberModel(6, 1, 200, 1));
        sizeDice.setName("sizeDice");
        ((JSpinner.DefaultEditor) sizeDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(sizeDice, newGridBagConstraints(gridx++, gridy++));

        gridx = 0;

        JLabel totalDiceResultLabel = new JLabel(resources.getString("totalDiceResultsLabel.text"));
        totalDiceResultLabel.setName("totalDiceResultsLabel");
        dicePanel.add(totalDiceResultLabel, newGridBagConstraints(gridx++, gridy));

        totalDiceResult = new JLabel("-");
        totalDiceResult.setName("totalDiceResult");
        dicePanel.add(totalDiceResult, newGridBagConstraints(gridx++, gridy));

        JButton diceRoll = new JButton(resources.getString("diceRoll.text"));
        diceRoll.setName("diceRoll");
        diceRoll.addActionListener(evt -> performDiceRoll());
        dicePanel.add(diceRoll, newGridBagConstraints(gridx++, gridy++, 2, 1));

        gridx = 0;

        JLabel individualDiceResultsLabel = new JLabel(resources.getString("individualDiceResultsLabel.text"));
        individualDiceResultsLabel.setName("individualDiceResultsLabel");
        dicePanel.add(individualDiceResultsLabel, newGridBagConstraints(gridx++, gridy));

        individualDiceResults = new JTextPane();
        individualDiceResults.setText("-");
        individualDiceResults.setName("individualDiceResults");
        individualDiceResults.setEditable(false);
        dicePanel.add(individualDiceResults, newGridBagConstraints(gridx++, gridy, 3, 1));

        return dicePanel;
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

        yearPicker = new JTextField(5);
        yearPicker.setText(String.valueOf(getGUI().getCampaign().getGameYear()));
        yearPicker.setName("yearPicker");
        ratPanel.add(yearPicker, newGridBagConstraints(0, 1));

        JLabel factionLabel = new JLabel(resources.getString("factionLabel.text"));
        factionLabel.setName("factionLabel");
        ratPanel.add(factionLabel, newGridBagConstraints(1, 0, 2, 1));

        List<FactionChoice> factionChoices = getFactionChoices((getPerson() == null)
                ? getGUI().getCampaign().getGameYear() : getPerson().getBirthday().getYear());
        DefaultComboBoxModel<FactionChoice> factionModel = new DefaultComboBoxModel<>(factionChoices.toArray(new FactionChoice[]{}));
        factionPicker = new JComboBox<>(factionModel);
        factionPicker.setName("factionPicker");
        factionPicker.setSelectedIndex(0);
        ratPanel.add(factionPicker, newGridBagConstraints(1, 1, 2, 1));

        JLabel qualityLabel = new JLabel(resources.getString("qualityLabel.text"));
        qualityLabel.setName("qualityLabel");
        ratPanel.add(qualityLabel, newGridBagConstraints(3, 0));

        qualityPicker = new JComboBox<>(QUALITY_NAMES);
        qualityPicker.setName("qualityPicker");
        ratPanel.add(qualityPicker, newGridBagConstraints(3, 1));

        JLabel unitTypeLabel = new JLabel(resources.getString("unitTypeLabel.text"));
        unitTypeLabel.setName("unitTypeLabel");
        ratPanel.add(unitTypeLabel, newGridBagConstraints(4, 0));

        DefaultComboBoxModel<String> unitTypeModel = new DefaultComboBoxModel<>();
        for (int ut = 0; ut < UnitType.SIZE; ut++) {
            if (getGUI().getCampaign().getUnitGenerator().isSupportedUnitType(ut)) {
                unitTypeModel.addElement(UnitType.getTypeName(ut));
            }
        }
        unitTypePicker = new JComboBox<>(unitTypeModel);
        unitTypePicker.setName("unitTypePicker");
        unitTypePicker.addItemListener(ev -> {
            final String unitType = (String) Objects.requireNonNull(unitTypePicker.getSelectedItem());
            unitWeightPicker.setEnabled(unitType.equals("Mek") || unitType.equals("Tank")
                    || unitType.equals("Aero"));
        });
        ratPanel.add(unitTypePicker, newGridBagConstraints(4, 1));

        JLabel unitWeightLabel = new JLabel(resources.getString("unitWeightLabel.text"));
        unitWeightLabel.setName("unitWeightLabel");
        ratPanel.add(unitWeightLabel, newGridBagConstraints(5, 0));

        unitWeightPicker = new JComboBox<>(WEIGHT_NAMES);
        unitWeightPicker.setName("unitWeightPicker");
        ratPanel.add(unitWeightPicker, newGridBagConstraints(5, 1));

        unitPicked = new JLabel("-");
        unitPicked.setName("unitPicked");
        ratPanel.add(unitPicked, newGridBagConstraints(0, 2, 4, 1));

        JButton ratRoll = new JButton(resources.getString("ratRoll.text"));
        ratRoll.setName("ratRoll");
        ratRoll.addActionListener(evt -> setLastRolledUnit(performRollRat()));
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
        genderPicker = new JComboBox<>(genderModel);
        genderPicker.setName("genderPicker");
        genderPicker.setSelectedIndex(0);
        namePanel.add(genderPicker, newGridBagConstraints(gridx++, 1));

        JLabel originFactionLabel = new JLabel(resources.getString("originFactionLabel.text"));
        originFactionLabel.setName("originFactionLabel");
        namePanel.add(originFactionLabel, newGridBagConstraints(gridx, 0));

        List<FactionChoice> factionChoices = getFactionChoices((getPerson() == null)
                ? getGUI().getCampaign().getGameYear() : getPerson().getBirthday().getYear());
        DefaultComboBoxModel<FactionChoice> factionModel = new DefaultComboBoxModel<>(factionChoices.toArray(new FactionChoice[]{}));
        nameGeneratorFactionPicker = new JComboBox<>(factionModel);
        nameGeneratorFactionPicker.setName("nameGeneratorFactionPicker");
        nameGeneratorFactionPicker.setSelectedIndex(0);
        namePanel.add(nameGeneratorFactionPicker, newGridBagConstraints(gridx++, 1));

        JLabel historicalEthnicityLabel = new JLabel(resources.getString("historicalEthnicityLabel.text"));
        historicalEthnicityLabel.setName("historicalEthnicityLabel");
        namePanel.add(historicalEthnicityLabel, newGridBagConstraints(gridx, 0));

        DefaultComboBoxModel<String> historicalEthnicityModel = new DefaultComboBoxModel<>();
        historicalEthnicityModel.addElement(resources.getString("factionWeighted.text"));
        for (String historicalEthnicity : RandomNameGenerator.getInstance().getHistoricalEthnicity().values()) {
            historicalEthnicityModel.addElement(historicalEthnicity);
        }
        ethnicCodePicker = new JComboBox<>(historicalEthnicityModel);
        ethnicCodePicker.setName("ethnicCodePicker");
        ethnicCodePicker.setSelectedIndex(0);
        ethnicCodePicker.addActionListener(evt -> nameGeneratorFactionPicker.setEnabled(ethnicCodePicker.getSelectedIndex() == 0));
        namePanel.add(ethnicCodePicker, newGridBagConstraints(gridx++, 1));

        JLabel clannerLabel = new JLabel(resources.getString("clannerLabel.text"));
        clannerLabel.setName("clannerLabel");
        namePanel.add(clannerLabel, newGridBagConstraints(gridx, 0));

        clannerPicker = new JCheckBox();
        clannerPicker.setName("clannerPicker");
        clannerPicker.getAccessibleContext().setAccessibleName(resources.getString("clannerLabel.text"));
        namePanel.add(clannerPicker, newGridBagConstraints(gridx++, 1));

        gridxMax = gridx - 1;
        gridx = 0;

        if (getPerson() != null) {
            JLabel currentNameLabel = new JLabel(resources.getString("currentNameLabel.text"));
            currentNameLabel.setName("currentNameLabel");
            namePanel.add(currentNameLabel, newGridBagConstraints(gridx++, 3));

            currentName = new JLabel("-");
            currentName.setName("currentName");
            namePanel.add(currentName, newGridBagConstraints(gridx++, 3));
        }

        JLabel nameGeneratedLabel = new JLabel(resources.getString((getPerson() == null)
                ? "namesGeneratedLabel.text" : "nameGeneratedLabel.text"));
        nameGeneratedLabel.setName("nameGeneratedLabel");
        namePanel.add(nameGeneratedLabel, newGridBagConstraints(gridx++, 3));

        nameGenerated = new JTextArea("-");
        nameGenerated.setName("nameGenerated");
        namePanel.add(nameGenerated, newGridBagConstraints(gridx++, 3));

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
            numNames = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
            numNames.setName("numNames");
            ((JSpinner.DefaultEditor) numNames.getEditor()).getTextField().setEditable(true);
            namePanel.add(numNames, newGridBagConstraints(gridxMax--, 4));
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

            currentCallsign = new JLabel("-");
            currentCallsign.setName("currentCallsign");
            callsignPanel.add(currentCallsign, newGridBagConstraints(gridx++, gridy));
        }

        JLabel callsignGeneratedLabel = new JLabel(resources.getString((getPerson() == null)
                ? "callsignsGeneratedLabel.text" : "callsignGeneratedLabel.text"));
        callsignGeneratedLabel.setName("callsignGeneratedLabel");
        callsignPanel.add(callsignGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        callsignGenerated = new JTextArea("-");
        callsignGenerated.setName("callsignGenerated");
        callsignPanel.add(callsignGenerated, newGridBagConstraints(gridx++, gridy));

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
            numCallsigns = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
            numCallsigns.setName("numCallsigns");
            ((JSpinner.DefaultEditor) numCallsigns.getEditor()).getTextField().setEditable(true);
            callsignPanel.add(numCallsigns, newGridBagConstraints(gridx--, gridy));
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
        originClanPicker = new JComboBox<>(originClanModel);
        originClanPicker.setName("originClanPicker");
        originClanPicker.setSelectedIndex(0);
        originClanPicker.addActionListener(evt -> validateBloodnameInput());
        namePanel.add(originClanPicker, newGridBagConstraints(gridx++, gridy + 1));

        JLabel bloodnameEraLabel = new JLabel(resources.getString("bloodnameEraLabel.text"));
        bloodnameEraLabel.setName("bloodnameEraLabel");
        namePanel.add(bloodnameEraLabel, newGridBagConstraints(gridx, gridy));

        bloodnameEraPicker = new JComboBox<>(ERAS);
        bloodnameEraPicker.setName("bloodnameEraPicker");
        bloodnameEraPicker.setSelectedIndex(0);
        bloodnameEraPicker.addActionListener(evt -> validateBloodnameInput());
        namePanel.add(bloodnameEraPicker, newGridBagConstraints(gridx++, gridy + 1));

        JLabel phenotypeLabel = new JLabel(resources.getString("phenotypeLabel.text"));
        phenotypeLabel.setName("phenotypeLabel");
        namePanel.add(phenotypeLabel, newGridBagConstraints(gridx, gridy));

        DefaultComboBoxModel<Phenotype> phenotypeModel = new DefaultComboBoxModel<>();
        phenotypeModel.addElement(Phenotype.GENERAL);
        for (Phenotype phenotype : Phenotype.getExternalPhenotypes()) {
            phenotypeModel.addElement(phenotype);
        }
        phenotypePicker = new JComboBox<>(phenotypeModel);
        phenotypePicker.setName("phenotypePicker");
        phenotypePicker.setSelectedItem(Phenotype.GENERAL);
        phenotypePicker.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setText((value == null) ? ""
                        : (value instanceof Phenotype ? ((Phenotype) value).getGroupingName() : "ERROR"));

                return this;
            }
        });
        phenotypePicker.addActionListener(evt -> validateBloodnameInput());
        namePanel.add(phenotypePicker, newGridBagConstraints(gridx++, gridy + 1));

        gridx = 0;
        gridy = 2;

        if (getPerson() != null) {
            JLabel currentBloodnameLabel = new JLabel(resources.getString("currentBloodnameLabel.text"));
            currentBloodnameLabel.setName("currentBloodnameLabel");
            namePanel.add(currentBloodnameLabel, newGridBagConstraints(gridx++, gridy));

            currentBloodname = new JLabel("-");
            currentBloodname.setName("currentBloodname");
            namePanel.add(currentBloodname, newGridBagConstraints(gridx++, gridy));
        }

        JLabel bloodnameGeneratedLabel = new JLabel(resources.getString("bloodnameGeneratedLabel.text"));
        bloodnameGeneratedLabel.setName("nameGeneratedLabel");
        namePanel.add(bloodnameGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        bloodnameGenerated = new JLabel("-");
        bloodnameGenerated.setName("bloodnameGenerated");
        namePanel.add(bloodnameGenerated, newGridBagConstraints(gridx++, gridy++));

        gridx = 0;

        JLabel originClanGeneratedLabel = new JLabel(resources.getString("originClanGeneratedLabel.text"));
        originClanGeneratedLabel.setName("originClanGeneratedLabel");
        namePanel.add(originClanGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        originClanGenerated = new JLabel("-");
        originClanGenerated.setName("originClanGenerated");
        namePanel.add(originClanGenerated, newGridBagConstraints(gridx++, gridy));

        JLabel phenotypeGeneratedLabel = new JLabel(resources.getString("phenotypeGeneratedLabel.text"));
        phenotypeGeneratedLabel.setName("phenotypeGeneratedLabel");
        namePanel.add(phenotypeGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        phenotypeGenerated = new JLabel("-");
        phenotypeGenerated.setName("phenotypeGenerated");
        namePanel.add(phenotypeGenerated, newGridBagConstraints(gridx++, gridy++));

        gridxMax = gridx - 1;
        gridx = 0;

        bloodnameWarningLabel = new JLabel("");
        GridBagConstraints gridBagConstraints = newGridBagConstraints(gridx++, gridy, 1, 1);
        gridBagConstraints.gridwidth = 2;
        namePanel.add(bloodnameWarningLabel, gridBagConstraints);

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

    //region GridBagConstraint Simplification Methods
    private GridBagConstraints newGridBagConstraints(int x, int y) {
        return newGridBagConstraints(x, y, 1, 1);
    }

    private GridBagConstraints newGridBagConstraints(int x, int y, int width, int height) {
        return new GridBagConstraints(x, y, width, height, 1.0, 1.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 3, 0, 3), 0, 0);
    }
    //endregion GridBagConstraint Simplification Methods

    /**
     * Gets a list of factions sorted by name.
     */
    private List<FactionChoice> getFactionChoices(int year) {
        List<FactionChoice> factionChoices = new ArrayList<>();

        for (Faction faction : Factions.getInstance().getFactions()) {
            factionChoices.add(new FactionChoice(faction, year));
        }

        factionChoices.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

        return factionChoices;
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(GMToolsDialog.class);

        preferences.manage(new JIntNumberSpinnerPreference(numDice));

        preferences.manage(new JIntNumberSpinnerPreference(sizeDice));

        preferences.manage(new JTextFieldPreference(yearPicker));

        preferences.manage(new JComboBoxPreference(factionPicker));

        preferences.manage(new JComboBoxPreference(qualityPicker));

        preferences.manage(new JComboBoxPreference(unitTypePicker));

        preferences.manage(new JComboBoxPreference(unitWeightPicker));

        if (numNames != null) {
            preferences.manage(new JIntNumberSpinnerPreference(numNames));
        }

        if (numCallsigns != null) {
            preferences.manage(new JIntNumberSpinnerPreference(numCallsigns));
        }

        setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void setValuesFromPerson() {
        if (getPerson() == null) {
            return;
        }

        // Current Name is the Person's full name
        currentName.setText(getPerson().getFullName());

        // Gender is set based on the person's gender
        genderPicker.setSelectedItem(person.getGender().isExternal() ? person.getGender()
                : getPerson().getGender().getExternalVariant());

        // Current Callsign is set if applicable
        if (!StringUtil.isNullOrEmpty(getPerson().getCallsign())) {
            currentCallsign.setText(getPerson().getCallsign());
        }

        // We set the clanner value based on whether or not the person is a clanner
        clannerPicker.setSelected(getPerson().isClanner());

        // Now we figure out the person's origin faction
        int factionIndex = findInitialSelectedFaction(getFactionChoices(getPerson().getBirthday().getYear()));
        if (factionIndex != 0) {
            factionPicker.setSelectedIndex(factionIndex);
            nameGeneratorFactionPicker.setSelectedIndex(factionIndex);
        }

        // Finally, we determine the default unit type
        for (int i = 0; i < unitTypePicker.getModel().getSize(); i++) {
            if (doesPersonPrimarilyDriveUnitType(UnitType.determineUnitTypeCode(unitTypePicker.getItemAt(i)))) {
                unitTypePicker.setSelectedIndex(i);
                break;
            }
        }

        if (!StringUtil.isNullOrEmpty(getPerson().getBloodname())) {
            currentBloodname.setText(getPerson().getBloodname());
        }

        int year = gui.getCampaign().getGameYear();
        for (int i = ERAS.length - 1; i >= 0; i--) {
            if (ERAS[i] <= year) {
                bloodnameEraPicker.setSelectedIndex(i);
                year = ERAS[i];
                break;
            }
        }

        originClanPicker.setSelectedItem((gui.getCampaign().getFaction().isClan()
                ? gui.getCampaign().getFaction() : getPerson().getOriginFaction()).getFullName(year));

        phenotypePicker.setSelectedItem(getPerson().getPhenotype());
    }

    /**
     * Finds the initial faction, which is either the person's faction (if
     * the dialog was launched for a specific person) or the campaign's
     * faction.
     */
    private int findInitialSelectedFaction(List<FactionChoice> factionChoices) {
        String factionId = (getPerson() != null)
                ? getPerson().getOriginFaction().getShortName()
                : getGUI().getCampaign().getFactionCode();
        if ((factionId == null) || factionId.isEmpty()) {
            return 0;
        }

        int index = 0;
        for (FactionChoice factionChoice : factionChoices) {
            if (factionChoice.id.equalsIgnoreCase(factionId)) {
                return index;
            }

            index++;
        }

        return 0;
    }

    /**
     * Determine if a person's primary role supports operating a
     * given unit type.
     */
    private boolean doesPersonPrimarilyDriveUnitType(int unitType) {
        int primaryRole = getPerson().getPrimaryRole();
        switch (unitType) {
            case UnitType.AERO:
                return primaryRole == Person.T_AERO_PILOT;
            case UnitType.BATTLE_ARMOR:
                return primaryRole == Person.T_BA;
            case UnitType.CONV_FIGHTER:
                return (primaryRole == Person.T_CONV_PILOT) || (primaryRole == Person.T_AERO_PILOT);
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.SMALL_CRAFT:
            case UnitType.WARSHIP:
                return primaryRole == Person.T_SPACE_PILOT;
            case UnitType.INFANTRY:
                return primaryRole == Person.T_INFANTRY;
            case UnitType.MEK:
                return primaryRole == Person.T_MECHWARRIOR;
            case UnitType.NAVAL:
                return primaryRole == Person.T_NVEE_DRIVER;
            case UnitType.PROTOMEK:
                return primaryRole == Person.T_PROTO_PILOT;
            case UnitType.TANK:
                return primaryRole == Person.T_GVEE_DRIVER;
            case UnitType.VTOL:
                return primaryRole == Person.T_VTOL_PILOT;
            default:
                return false;
        }
    }
    //endregion Initialization

    //region Getters and Setters
    public CampaignGUI getGUI() {
        return gui;
    }

    public void setGUI(CampaignGUI gui) {
        this.gui = gui;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public MechSummary getLastRolledUnit() {
        return lastRolledUnit;
    }

    public void setLastRolledUnit(MechSummary lastRolledUnit) {
        this.lastRolledUnit = lastRolledUnit;
    }

    public String[] getLastGeneratedName() {
        return lastGeneratedName;
    }

    public void setLastGeneratedName(String... lastGeneratedName) {
        this.lastGeneratedName = lastGeneratedName;
    }

    public String getLastGeneratedCallsign() {
        return lastGeneratedCallsign;
    }

    public void setLastGeneratedCallsign(String lastGeneratedCallsign) {
        this.lastGeneratedCallsign = lastGeneratedCallsign;
    }

    public String getLastGeneratedBloodname() {
        return lastGeneratedBloodname;
    }

    public void setLastGeneratedBloodname(String lastGeneratedBloodname) {
        this.lastGeneratedBloodname = lastGeneratedBloodname;
    }
    //endregion Getters and Setters

    //region ActionEvent Handlers
    public void performDiceRoll() {
        List<Integer> individualDice = Utilities.individualDice((Integer) numDice.getValue(),
                (Integer) sizeDice.getValue());
        totalDiceResult.setText(String.format(resources.getString("totalDiceResult.text"),
                individualDice.get(0)));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < individualDice.size() - 1; i++) {
            sb.append(individualDice.get(i)).append(", ");
        }
        sb.append(individualDice.get(individualDice.size() - 1));

        if (sb.length() > 0) {
            individualDiceResults.setText(sb.toString());
        } else {
            individualDiceResults.setText("-");
        }
    }

    private MechSummary performRollRat() {
        try {
            IUnitGenerator ug = getGUI().getCampaign().getUnitGenerator();
            int unitType = UnitType.determineUnitTypeCode((String) unitTypePicker.getSelectedItem());
            int unitWeight = unitWeightPicker.getSelectedIndex() + EntityWeightClass.WEIGHT_LIGHT;
            if (!unitWeightPicker.isEnabled()) {
                unitWeight = AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED;
            }
            int targetYear = Integer.parseInt(yearPicker.getText());
            int unitQuality = qualityPicker.getSelectedIndex();

            Campaign campaign = getGUI().getCampaign();
            Predicate<MechSummary> test = ms ->
                    (!campaign.getCampaignOptions().limitByYear() || (targetYear > ms.getYear()))
                            && (!ms.isClan() || campaign.getCampaignOptions().allowClanPurchases())
                            && (ms.isClan() || campaign.getCampaignOptions().allowISPurchases());
            MechSummary ms = ug.generate(((FactionChoice) Objects.requireNonNull(factionPicker.getSelectedItem())).id,
                    unitType, unitWeight, targetYear, unitQuality, test);
            if (ms != null) {
                unitPicked.setText(ms.getName());
                return ms;
            }
        } catch (Exception e) {
            unitPicked.setText(Messages.getString("invalidYear.error"));
            return null;
        }
        unitPicked.setText(Messages.getString("noValidUnit.error"));
        return null;
    }

    private void addRATRolledUnit() {
        if (getLastRolledUnit() == null) {
            setLastRolledUnit(performRollRat());
        }

        if (getLastRolledUnit() != null) {
            Entity e;
            try {
                e = new MechFileParser(getLastRolledUnit().getSourceFile(),
                        getLastRolledUnit().getEntryName()).getEntity();
                Unit u = getGUI().getCampaign().addNewUnit(e, false, 0);
                if ((getPerson() != null) && (getPerson().getUnit() == null)) {
                    u.addPilotOrSoldier(getPerson());
                    getPerson().setOriginalUnit(u);
                    setVisible(false);
                }
                setLastRolledUnit(null);
            } catch (Exception ex) {
                MekHQ.getLogger().error("Failed to load entity "
                        + getLastRolledUnit().getName() + " from " + getLastRolledUnit().getSourceFile().toString(), ex);
                unitPicked.setText(String.format(Messages.getString("entityLoadFailure.error"), getLastRolledUnit().getName()));
            }
        }
    }

    private void generateName() {
        String[] name = generateIndividualName();
        nameGenerated.setText((name[0] + " " + name[1]).trim());
        setLastGeneratedName(name);
    }

    private void generateNames() {
        StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < (Integer) numNames.getValue(); i++) {
            final String[] name = generateIndividualName();
            sj.add((name[0] + " " + name[1]).trim());
        }
        nameGenerated.setText(sj.toString());
    }

    private String[] generateIndividualName() {
        final int ethnicCode = ethnicCodePicker.getSelectedIndex();
        String[] name;

        if (ethnicCode == 0) {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                    (Gender) genderPicker.getSelectedItem(), clannerPicker.isSelected(),
                    ((FactionChoice) Objects.requireNonNull(nameGeneratorFactionPicker.getSelectedItem())).id);
        } else {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplitWithEthnicCode(
                    (Gender) genderPicker.getSelectedItem(), clannerPicker.isSelected(), ethnicCode);
        }
        return name;
    }

    private void assignName() {
        if (getLastGeneratedName() == null) {
            generateName();
        }

        currentName.setText((getLastGeneratedName()[0] + " " + getLastGeneratedName()[1]).trim());
        getPerson().setGivenName(getLastGeneratedName()[0]);
        getPerson().setSurname(getLastGeneratedName()[1]);

        MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
    }

    private void generateCallsign() {
        String callsign = RandomCallsignGenerator.getInstance().generate();
        callsignGenerated.setText(callsign);
        setLastGeneratedCallsign(callsign);
    }

    private void generateCallsigns() {
        StringJoiner sj = new StringJoiner("\n");
        for (int i = 0; i < (Integer) numCallsigns.getValue(); i++) {
            sj.add(RandomCallsignGenerator.getInstance().generate());
        }
        callsignGenerated.setText(sj.toString());
    }

    private void assignCallsign() {
        if (getLastGeneratedCallsign() == null) {
            generateCallsign();
        }

        currentCallsign.setText(getLastGeneratedCallsign());
        getPerson().setCallsign(getLastGeneratedCallsign());

        MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
    }

    private void generateBloodname() {
        Bloodname bloodname = Bloodname.randomBloodname(clans.get(originClan),
                selectedPhenotype, bloodnameYear);
        if (bloodname != null) {
            bloodnameGenerated.setText(bloodname.getName() + " (" + bloodname.getFounder() + ")");
            originClanGenerated.setText(Clan.getClan(bloodname.getOrigClan()).getFullName(bloodnameYear));
            phenotypeGenerated.setText(bloodname.getPhenotype().getGroupingName());
            setLastGeneratedBloodname(bloodname.getName());
        }
    }

    private void assignBloodname() {
        if (getLastGeneratedBloodname() == null) {
            generateBloodname();
        }

        currentBloodname.setText(getLastGeneratedBloodname());
        getPerson().setBloodname(getLastGeneratedBloodname());

        MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
    }


    private void validateBloodnameInput() {
        originClan = originClanPicker.getSelectedIndex();
        bloodnameYear = ERAS[bloodnameEraPicker.getSelectedIndex()];
        selectedPhenotype = (Phenotype) phenotypePicker.getSelectedItem();

        if ((originClan < 0) || (selectedPhenotype == Phenotype.NONE) || (selectedPhenotype == null)) {
            return;
        }

        Clan selectedClan = clans.get(originClan);
        String txt = "<html>";

        if (bloodnameYear < selectedClan.getStartDate()) {
            for (int era : ERAS) {
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
            for (int i = ERAS.length - 1; i >= 0; i--) {
                if (ERAS[i] <= selectedClan.getEndDate()) {
                    bloodnameYear = ERAS[i];
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
        bloodnameWarningLabel.setText(txt);
    }
    //endregion ActionEvent Handlers

    private static class FactionChoice {
        public final String name;
        public final String id;

        private final String displayName;

        public FactionChoice(Faction faction, int year) {
            id = faction.getShortName();
            name = faction.getFullName(year);
            displayName = String.format("%s [%s]", name, id);
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
