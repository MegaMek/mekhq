/*
 * GMToolsDialog.java
 *
 * Copyright (c) 2013-2018 MegaMek Team.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.dialog;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.UnitType;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
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

public class GMToolsDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 7724064095803583812L;
    
    private JButton dice;
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
    
    private CampaignGUI gui;
    private Person person;

    /** The last unit rolled, used when clicking 'Add Random Unit'
     * after Roll for RAT is clicked.
     */
    private MechSummary lastRolledUnit;

    private static final String[] QUALITY_NAMES = {"F", "D", "C", "B", "A", "A*"};
    private final String[] WEIGHT_NAMES = {"Light", "Medium", "Heavy", "Assault"};

    private static final String GM_TOOL_DICE = "gmToolDice";
    private static final String RAT_ROLLER_TOOL = "ratRollerTool";
    private static final String RAT_ADDER_TOOL = "ratAdderTool";

    /**
     * Creates a generic GM Tools dialog, with a dice roller
     * and a RAT roller that can add units to the campaign.
     */
    public GMToolsDialog(Frame parent, CampaignGUI gui) {
        super(parent, false);
        this.gui = gui;
        setName("formGMTools"); // NOI18N
        setTitle("GM Tools");
        getContentPane().setLayout(new java.awt.GridBagLayout());
        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    /**
     * Creates a GM Tools dialog specific to a given person. The
     * dice roller and RAT roller are both available, however, the
     * RAT roller adds the unit directly to the person.
     */
    public GMToolsDialog(Frame parent, CampaignGUI gui, Person p) {
        super(parent, false);
        this.gui = gui;
        this.person = p;
        setName("formGMTools"); // NOI18N
        setTitle("GM Tools - " + p.getFullName());
        getContentPane().setLayout(new java.awt.GridBagLayout());
        initComponents();
        pack();
        setLocationRelativeTo(parent);
        setUserPreferences();
    }

    private JPanel getDiceRoller() {
        JPanel dicePanel = new JPanel(new GridBagLayout());
        dicePanel.setBorder(BorderFactory.createTitledBorder("Dice Roller"));

        numDice = new JSpinner(new SpinnerNumberModel(2, 1, 50, 1));
        ((JSpinner.DefaultEditor)numDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(numDice, newGridBagConstraints(0,0));
        
        dicePanel.add(d, newGridBagConstraints(1,0));

        sizeDice = new JSpinner(new SpinnerNumberModel(6, 1, 200, 1));
        ((JSpinner.DefaultEditor)sizeDice.getEditor()).getTextField().setEditable(true);
        dicePanel.add(sizeDice, newGridBagConstraints(2, 0));

        dice = new JButton("Roll");
        dice.setActionCommand(GM_TOOL_DICE);
        dice.addActionListener(this);
        dicePanel.add(dice, newGridBagConstraints(0, 1, 3, 1));

        diceResults = new JLabel(String.format("Result: %5d", 0));
        dicePanel.add(diceResults, newGridBagConstraints(0, 2, 3, 1));
        return dicePanel;
    }
    
    /**
     * Gets a list of factions sorted by name.
     */
    private List<FactionChoice> getFactionChoices() {
        List<FactionChoice> factionChoices = new ArrayList<>();

        int year = gui.getCampaign().getGameYear();
        for(Faction faction : Faction.getFactions()) {
            factionChoices.add(
                new FactionChoice(faction, year));
        }

        Collections.sort(factionChoices, (a, b) -> a.name.compareToIgnoreCase(b.name));

        return factionChoices;
    }

    /**
     * Finds the initial faction, which is either the person's faction (if
     * the dialog was launched for a specific person) or the campaign's
     * faction.
     */
    private int findInitialSelectedFaction(Iterable<FactionChoice> factionChoices) {
        String factionId = person != null 
            ? person.getOriginFaction().getShortName() 
            : gui.getCampaign().getFactionCode();
        if (factionId == null || factionId.isEmpty()) {
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

    private JPanel getRATRoller() {
        JPanel ratPanel = new JPanel(new GridBagLayout());
        ratPanel.setBorder(BorderFactory.createTitledBorder("RAT Roller"));
        
        List<FactionChoice> factionChoices = getFactionChoices();
        factionPicker = new JComboBox<FactionChoice>(factionChoices.toArray(new FactionChoice[factionChoices.size()]));
        factionPicker.setSelectedIndex(findInitialSelectedFaction(factionChoices));

        yearPicker = new JTextField(5);
        yearPicker.setText(String.valueOf(gui.getCampaign().getGameYear()));
        qualityPicker = new JComboBox<String>(QUALITY_NAMES);
        unitTypePicker = new JComboBox<String>();
        int selectedUnitType = -1;
        for (int ut = 0; ut < UnitType.SIZE; ut++) {
            if (gui.getCampaign().getUnitGenerator().isSupportedUnitType(ut)) {
                unitTypePicker.addItem(UnitType.getTypeName(ut));
                if (null != person && selectedUnitType == -1 && doesPersonPrimarilyDriveUnitType(person, ut)) {
                    selectedUnitType = ut;
                }
            }
        }
        unitTypePicker.setSelectedIndex(Math.max(selectedUnitType, 0));
        unitWeightPicker = new JComboBox<String>(WEIGHT_NAMES);
        unitPicked = new JLabel("-");
        unitTypePicker.addItemListener(ev ->
            unitWeightPicker.setEnabled(unitTypePicker.getSelectedItem().equals("Mek")
                    || unitTypePicker.getSelectedItem().equals("Tank")
                    || unitTypePicker.getSelectedItem().equals("Aero"))
        );
        
        ratPanel.add(new JLabel("Year"), newGridBagConstraints(0, 0));
        ratPanel.add(yearPicker, newGridBagConstraints(0, 1));
        
        ratPanel.add(new JLabel("Faction"), newGridBagConstraints(1, 0, 2, 1));
        ratPanel.add(factionPicker, newGridBagConstraints(1, 1, 2, 1));
        
        ratPanel.add(new JLabel("Quality"), newGridBagConstraints(3, 0));
        ratPanel.add(qualityPicker,newGridBagConstraints(3, 1));
        
        ratPanel.add(new JLabel("Unit Type"), newGridBagConstraints(4, 0));
        ratPanel.add(unitTypePicker, newGridBagConstraints(4, 1));
        
        ratPanel.add(new JLabel("Weight"), newGridBagConstraints(5, 0));
        ratPanel.add(unitWeightPicker, newGridBagConstraints(5, 1));
        
        ratPanel.add(unitPicked, newGridBagConstraints(0, 2, 4, 1));

        JButton roll = new JButton("Roll For RAT");
        roll.setActionCommand(RAT_ROLLER_TOOL);
        roll.addActionListener(this);
        ratPanel.add(roll, newGridBagConstraints(5, 3));
        
        JButton roll2 = new JButton("Add Random Unit");
        if (!gui.getCampaign().isGM()) roll2.setEnabled(false);
        roll2.setActionCommand(RAT_ADDER_TOOL);
        roll2.addActionListener(this);
        ratPanel.add(roll2, newGridBagConstraints(6, 3));
        return ratPanel;
    }

    /**
     * Determine if a person's primary role supports operating a
     * given unit type.
     */
    private boolean doesPersonPrimarilyDriveUnitType(Person p, int unitType) {
        int primaryRole = p.getPrimaryRole();
        switch (unitType) {
            case UnitType.AERO:
                return primaryRole == Person.T_AERO_PILOT;
            case UnitType.BATTLE_ARMOR:
                return primaryRole == Person.T_BA;
            case UnitType.CONV_FIGHTER:
                return primaryRole == Person.T_CONV_PILOT || primaryRole == Person.T_AERO_PILOT;
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
    
    private GridBagConstraints newGridBagConstraints(int x, int y) {
        return newGridBagConstraints(x, y, 1, 1);
    }
    
    private GridBagConstraints newGridBagConstraints(int x, int y, int width, int height) {
        return new GridBagConstraints(x, y, width, height, 1.0, 1.0,
            GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 3, 0, 3), 0, 0);
    }
    
    private void initComponents() {
        getContentPane().add(getDiceRoller(), newGridBagConstraints(0,0));
        getContentPane().add(getRATRoller(), newGridBagConstraints(0,1));
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
    
    @Override
    public void actionPerformed(ActionEvent event) {
        final String METHOD_NAME = "actionPerformed(ActionEvent)"; //$NON-NLS-1$

        if(event.getActionCommand().equals(GM_TOOL_DICE)) {
            performDiceRoll();
        }
        
        if(event.getActionCommand().equals(RAT_ROLLER_TOOL)) {
            lastRolledUnit = performRollRat();
        }
        
        if(event.getActionCommand().equals(RAT_ADDER_TOOL)) {
            if (null == lastRolledUnit) {
                lastRolledUnit = performRollRat();
            }

            if (null != lastRolledUnit) {
                Entity e = null;
                try {
                    e = new MechFileParser(lastRolledUnit.getSourceFile(), lastRolledUnit.getEntryName()).getEntity();
                    Unit u = gui.getCampaign().addUnit(e, false, 0);
                    if (null != person) {
                        u.addPilotOrSoldier(person);
                        person.setOriginalUnit(u);
                        setVisible(false);
                    }
                    lastRolledUnit = null;
                } catch (EntityLoadingException e1) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            "Failed to load entity " + lastRolledUnit.getName() + " from " //$NON-NLS-1$
                                    + lastRolledUnit.getSourceFile().toString()); //$NON-NLS-1$
                    MekHQ.getLogger().error(getClass(), METHOD_NAME, e1);
                    unitPicked.setText("Failed to load entity " + lastRolledUnit.getName());
                }
            }
        }
    }
    
    private MechSummary performRollRat() {
        try{
            IUnitGenerator ug = gui.getCampaign().getUnitGenerator();
            int unitType = 0;
            for (int ut = 0; ut < UnitType.SIZE; ut++) {
                if (UnitType.getTypeName(ut).equals(unitTypePicker.getSelectedItem())) {
                    unitType = ut;
                    break;
                }
            }
            int unitQuality = qualityPicker.getSelectedIndex();
            int unitWeight = unitWeightPicker.getSelectedIndex() + EntityWeightClass.WEIGHT_LIGHT;
            if (!unitWeightPicker.isEnabled()) {
                unitWeight = AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED;
            }
            
            int targetYear = Integer.parseInt(yearPicker.getText());
            
            Campaign campaign = gui.getCampaign();
            Predicate<MechSummary> test = ms ->
                	(!campaign.getCampaignOptions().limitByYear() || targetYear > ms.getYear())
                		&& (!ms.isClan() || campaign.getCampaignOptions().allowClanPurchases())
                		&& (ms.isClan() || campaign.getCampaignOptions().allowISPurchases());
            MechSummary ms = ug
            		.generate(((FactionChoice) factionPicker.getSelectedItem()).id,
            				unitType, unitWeight, targetYear, unitQuality, test);
            if (ms != null) {
                unitPicked.setText(ms.getName());
                return ms;
            }
        } catch(NumberFormatException e) {
            unitPicked.setText("Please enter a valid year");
            return null;
        }
        unitPicked.setText("No unit matching criteria and purchase restrictions.");
        return null;
    }

    public void performDiceRoll() {
            diceResults.setText(String.format("Result: %5d", Utilities.dice((Integer)numDice.getValue(), (Integer)sizeDice.getValue())));
    }
    
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
