/*
 * GMToolsDialog.java
 *
 * Copyright (c) 2013-2020 - The MegaMek Team. All Rights Reserved.
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import javax.swing.*;

import megamek.client.generator.RandomNameGenerator;
import megamek.client.generators.RandomCallsignGenerator;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
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
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.gui.CampaignGUI;
import mekhq.gui.preferences.JComboBoxPreference;
import mekhq.gui.preferences.JIntNumberSpinnerPreference;
import mekhq.gui.preferences.JTextFieldPreference;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

public class GMToolsDialog extends JDialog {
    private static final long serialVersionUID = 7724064095803583812L;

    private JLabel diceResults;
    private JSpinner numDice;
    private JSpinner sizeDice;
    private JLabel d = new JLabel("d");

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
    private JLabel currentName;
    private JLabel nameGenerated;

    private JLabel currentCallsign;
    private JLabel callsignGenerated;

    private CampaignGUI gui;
    private Person person;

    /**
     * The last unit rolled, used when clicking 'Add Random Unit' after Roll for RAT is clicked
     */
    private MechSummary lastRolledUnit;

    /**
     * The last name rolled, used when clocking 'Set Generated Name' after Generate Name is clicked
     */
    private String[] lastGeneratedName;

    /**
     * The last callsign rolled, used when clocking 'Set Generated Callsign' after Generate Callsign is clicked
     */
    private String lastGeneratedCallsign;

    private static final String[] QUALITY_NAMES = {"F", "D", "C", "B", "A", "A*"};
    private static final String[] WEIGHT_NAMES = {"Light", "Medium", "Heavy", "Assault"};

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GMToolsDialog", new EncodeControl());

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
    }
    //endregion Constructors

    //region Initialization
    private void initComponents() {
        getContentPane().add(getDiceRoller(), newGridBagConstraints(0,0));
        getContentPane().add(getRATRoller(), newGridBagConstraints(0,1));
        getContentPane().add(getNameGenerator(), newGridBagConstraints(0, 2));
        getContentPane().add(getCallsignGenerator(), newGridBagConstraints(0, 3));
    }

    //region Dice Panel Initialization
    private JPanel getDiceRoller() {
        JPanel dicePanel = new JPanel(new GridBagLayout());
        dicePanel.setBorder(BorderFactory.createTitledBorder("Dice Roller"));

        numDice = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
        ((JSpinner.DefaultEditor) numDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(numDice, newGridBagConstraints(0,0));

        dicePanel.add(d, newGridBagConstraints(1,0));

        sizeDice = new JSpinner(new SpinnerNumberModel(6, 1, 200, 1));
        ((JSpinner.DefaultEditor) sizeDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(sizeDice, newGridBagConstraints(2, 0));

        JButton dice = new JButton("Roll");
        dice.addActionListener(evt -> performDiceRoll());
        dicePanel.add(dice, newGridBagConstraints(0, 1, 3, 1));

        diceResults = new JLabel(String.format("Result: %5d", 0));
        dicePanel.add(diceResults, newGridBagConstraints(0, 2, 3, 1));
        return dicePanel;
    }
    //endregion Dice Panel Initialization

    //region RAT Roller Initialization
    private JPanel getRATRoller() {
        JPanel ratPanel = new JPanel(new GridBagLayout());
        ratPanel.setBorder(BorderFactory.createTitledBorder("RAT Roller"));

        List<FactionChoice> factionChoices = getFactionChoices((person == null)
                ? getGUI().getCampaign().getGameYear() : person.getBirthday().getYear());
        DefaultComboBoxModel<FactionChoice> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(factionChoices);
        factionPicker = new JComboBox<>(factionModel);
        factionPicker.setSelectedIndex(0);

        yearPicker = new JTextField(5);
        yearPicker.setText(String.valueOf(getGUI().getCampaign().getGameYear()));
        qualityPicker = new JComboBox<>(QUALITY_NAMES);
        unitWeightPicker = new JComboBox<>(WEIGHT_NAMES);
        unitTypePicker = new JComboBox<>();
        for (int ut = 0; ut < UnitType.SIZE; ut++) {
            if (getGUI().getCampaign().getUnitGenerator().isSupportedUnitType(ut)) {
                unitTypePicker.addItem(UnitType.getTypeName(ut));
            }
        }
        unitTypePicker.addItemListener(ev ->
            unitWeightPicker.setEnabled(unitTypePicker.getSelectedItem().equals("Mek")
                    || unitTypePicker.getSelectedItem().equals("Tank")
                    || unitTypePicker.getSelectedItem().equals("Aero"))
        );
        unitPicked = new JLabel("-");

        ratPanel.add(new JLabel("Year"), newGridBagConstraints(0, 0));
        ratPanel.add(yearPicker, newGridBagConstraints(0, 1));

        ratPanel.add(new JLabel("Faction"), newGridBagConstraints(1, 0, 2, 1));
        ratPanel.add(factionPicker, newGridBagConstraints(1, 1, 2, 1));

        ratPanel.add(new JLabel("Quality"), newGridBagConstraints(3, 0));
        ratPanel.add(qualityPicker, newGridBagConstraints(3, 1));

        ratPanel.add(new JLabel("Unit Type"), newGridBagConstraints(4, 0));
        ratPanel.add(unitTypePicker, newGridBagConstraints(4, 1));

        ratPanel.add(new JLabel("Weight"), newGridBagConstraints(5, 0));
        ratPanel.add(unitWeightPicker, newGridBagConstraints(5, 1));

        ratPanel.add(unitPicked, newGridBagConstraints(0, 2, 4, 1));

        JButton roll = new JButton("Roll For RAT");
        roll.addActionListener(evt -> setLastRolledUnit(performRollRat()));
        ratPanel.add(roll, newGridBagConstraints(5, 3));

        if (getGUI().getCampaign().isGM()) {
            JButton roll2 = new JButton("Add Random Unit");
            roll2.addActionListener(evt -> addRATRolledUnit());
            ratPanel.add(roll2, newGridBagConstraints(6, 3));
        }
        return ratPanel;
    }
    //endregion RAT Roller Initialization

    //region Name Generator Panel Initialization
    private JPanel getNameGenerator() {
        JPanel namePanel = new JPanel(new GridBagLayout());
        namePanel.setBorder(BorderFactory.createTitledBorder(resources.getString("namePanel.text")));

        int gridx = 0, gridxMax;

        JLabel genderLabel = new JLabel(resources.getString("genderLabel.text"));
        namePanel.add(genderLabel, newGridBagConstraints(gridx, 0));

        DefaultComboBoxModel<Gender> genderModel = new DefaultComboBoxModel<>();
        genderModel.addAll(Gender.getExternalOptions());
        genderPicker = new JComboBox<>(genderModel);
        genderPicker.setSelectedIndex(0);
        namePanel.add(genderPicker, newGridBagConstraints(gridx++, 1));

        JLabel originFactionLabel = new JLabel(resources.getString("originFactionLabel.text"));
        namePanel.add(originFactionLabel, newGridBagConstraints(gridx, 0));

        List<FactionChoice> factionChoices = getFactionChoices((person == null)
                ? getGUI().getCampaign().getGameYear() : person.getBirthday().getYear());
        DefaultComboBoxModel<FactionChoice> factionModel = new DefaultComboBoxModel<>();
        factionModel.addAll(factionChoices);
        nameGeneratorFactionPicker = new JComboBox<>(factionModel);
        nameGeneratorFactionPicker.setSelectedIndex(0);
        namePanel.add(nameGeneratorFactionPicker, newGridBagConstraints(gridx++, 1));

        JLabel historicalEthnicityLabel = new JLabel(resources.getString("historicalEthnicityLabel.text"));
        namePanel.add(historicalEthnicityLabel, newGridBagConstraints(gridx, 0));

        DefaultComboBoxModel<String> historicalEthnicityModel = new DefaultComboBoxModel<>();
        historicalEthnicityModel.addElement(resources.getString("factionWeighted.text"));
        historicalEthnicityModel.addAll(RandomNameGenerator.getInstance().getHistoricalEthnicity().values());
        ethnicCodePicker = new JComboBox<>(historicalEthnicityModel);
        ethnicCodePicker.setSelectedIndex(0);
        ethnicCodePicker.addActionListener(evt -> nameGeneratorFactionPicker.setEnabled(ethnicCodePicker.getSelectedIndex() == 0));
        namePanel.add(ethnicCodePicker, newGridBagConstraints(gridx++, 1));

        JLabel clannerLabel = new JLabel(resources.getString("clannerLabel.text"));
        namePanel.add(clannerLabel, newGridBagConstraints(gridx, 0));

        clannerPicker = new JCheckBox();
        clannerPicker.setName("clannerPicker");
        clannerPicker.getAccessibleContext().setAccessibleName(resources.getString("clannerLabel.text"));
        namePanel.add(clannerPicker, newGridBagConstraints(gridx++, 1));

        gridxMax = gridx - 1;
        gridx = 0;

        if (person != null) {
            JLabel currentNameLabel = new JLabel(resources.getString("currentNameLabel.text"));
            namePanel.add(currentNameLabel, newGridBagConstraints(gridx++, 3));

            currentName = new JLabel("-");
            namePanel.add(currentName, newGridBagConstraints(gridx++, 3));
        }

        JLabel nameGeneratedLabel = new JLabel(resources.getString("nameGeneratedLabel.text"));
        namePanel.add(nameGeneratedLabel, newGridBagConstraints(gridx++, 3));

        nameGenerated = new JLabel("-");
        namePanel.add(nameGenerated, newGridBagConstraints(gridx++, 3));

        if (person != null) {
            JButton assignNameButton = new JButton(resources.getString("assignNameButton.text"));
            assignNameButton.addActionListener(evt -> assignName());
            namePanel.add(assignNameButton, newGridBagConstraints(gridxMax--, 4));
        }

        JButton generateNameButton = new JButton(resources.getString("generateNameButton.text"));
        generateNameButton.addActionListener(evt -> generateName());
        namePanel.add(generateNameButton, newGridBagConstraints(gridxMax--, 4));

        return namePanel;
    }
    //endregion Name Generator Panel Initialization

    //region Callsign Generator Panel Initialization
    private JPanel getCallsignGenerator() {
        JPanel callsignPanel = new JPanel(new GridBagLayout());
        callsignPanel.setBorder(BorderFactory.createTitledBorder(resources.getString("callsignPanel.text")));

        int gridx = 0, gridy = 0;

        if (person != null) {
            JLabel currentCallsignLabel = new JLabel(resources.getString("currentCallsignLabel.text"));
            callsignPanel.add(currentCallsignLabel, newGridBagConstraints(gridx++, gridy));

            currentCallsign = new JLabel("-");
            callsignPanel.add(currentCallsign, newGridBagConstraints(gridx++, gridy));
        }

        JLabel callsignGeneratedLabel = new JLabel(resources.getString("callsignGeneratedLabel.text"));
        callsignPanel.add(callsignGeneratedLabel, newGridBagConstraints(gridx++, gridy));

        callsignGenerated = new JLabel("-");
        callsignPanel.add(callsignGenerated, newGridBagConstraints(gridx++, gridy));

        gridy++;

        if (person != null) {
            JButton assignCallsignButton = new JButton(resources.getString("assignCallsignButton.text"));
            assignCallsignButton.addActionListener(evt -> assignCallsign());
            callsignPanel.add(assignCallsignButton, newGridBagConstraints(gridx--, gridy));
        }

        JButton generateCallsignButton = new JButton(resources.getString("generateCallsignButton.text"));
        generateCallsignButton.addActionListener(evt -> generateCallsign());
        callsignPanel.add(generateCallsignButton, newGridBagConstraints(gridx--, gridy));

        return callsignPanel;
    }
    //endregion Callsign Generator Panel Initialization

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

        for (Faction faction : Faction.getFactions()) {
            factionChoices.add(new FactionChoice(faction, year));
        }

        factionChoices.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

        return factionChoices;
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(GMToolsDialog.class);

        numDice.setName("numDice");
        preferences.manage(new JIntNumberSpinnerPreference(numDice));

        sizeDice.setName("sizeDice");
        preferences.manage(new JIntNumberSpinnerPreference(sizeDice));

        yearPicker.setName("year");
        preferences.manage(new JTextFieldPreference(yearPicker));

        factionPicker.setName("faction");
        preferences.manage(new JComboBoxPreference(factionPicker));

        qualityPicker.setName("quality");
        preferences.manage(new JComboBoxPreference(qualityPicker));

        unitTypePicker.setName("unitType");
        preferences.manage(new JComboBoxPreference(unitTypePicker));

        unitWeightPicker.setName("unitWeight");
        preferences.manage(new JComboBoxPreference(unitWeightPicker));

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    private void setValuesFromPerson() {
        if (getPerson() == null) {
            return;
        }

        // Current Name is the Person's full name
        currentName.setText(getPerson().getFullName());

        // Gender is set based on the person's gender
        genderPicker.setSelectedItem(getPerson().getGender().getExternalVariant());

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
        //endregion RAT Roller
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

    public void setLastGeneratedName(String[] lastGeneratedName) {
        this.lastGeneratedName = lastGeneratedName;
    }

    public String getLastGeneratedCallsign() {
        return lastGeneratedCallsign;
    }

    public void setLastGeneratedCallsign(String lastGeneratedCallsign) {
        this.lastGeneratedCallsign = lastGeneratedCallsign;
    }
    //endregion Getters and Setters

    //region ActionEvent Handlers
    public void performDiceRoll() {
        diceResults.setText(String.format("Result: %5d", Utilities.dice((Integer) numDice.getValue(),
                (Integer) sizeDice.getValue())));
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
            unitPicked.setText("Please enter a valid year");
            return null;
        }
        unitPicked.setText("No unit matching criteria and purchase restrictions.");
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
                Unit u = getGUI().getCampaign().addUnit(e, false, 0);
                if (getPerson() != null) {
                    u.addPilotOrSoldier(getPerson());
                    getPerson().setOriginalUnit(u);
                    setVisible(false);
                }
                setLastRolledUnit(null);
            } catch (Exception ex) {
                MekHQ.getLogger().error(this, "Failed to load entity "
                        + getLastRolledUnit().getName() + " from " + getLastRolledUnit().getSourceFile().toString(), ex);
                unitPicked.setText("Failed to load entity " + getLastRolledUnit().getName());
            }
        }
    }

    private void generateName() {
        int ethnicCode = ethnicCodePicker.getSelectedIndex();
        String[] name;

        if (ethnicCode == 0) {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(
                    (Gender) genderPicker.getSelectedItem(), clannerPicker.isSelected(),
                    ((FactionChoice) Objects.requireNonNull(nameGeneratorFactionPicker.getSelectedItem())).id);
        } else {
            name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplitWithEthnicCode(
                    (Gender) genderPicker.getSelectedItem(), clannerPicker.isSelected(), ethnicCode);
        }

        nameGenerated.setText((name[0] + " " + name[1]).trim());
        setLastGeneratedName(name);
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

    private void assignCallsign() {
        if (getLastGeneratedCallsign() == null) {
            generateCallsign();
        }

        currentCallsign.setText(getLastGeneratedCallsign());
        getPerson().setCallsign(getLastGeneratedCallsign());

        MekHQ.triggerEvent(new PersonChangedEvent(getPerson()));
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
