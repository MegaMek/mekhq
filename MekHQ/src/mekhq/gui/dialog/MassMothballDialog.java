/*
 * Person.java
 *
 * Copyright (c) 2018 Megamek Team. All rights reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.UnitTableMouseAdapter;
import mekhq.gui.preferences.JWindowPreference;
import mekhq.preferences.PreferencesNode;

/**
 * This class handles the display of the Mass Mothball/Reactivate dialog
 * @author NickAragua
 *
 */
public class MassMothballDialog extends JDialog implements ActionListener, ListSelectionListener {
    /**
     *
     */
    private static final long serialVersionUID = -7435381378836891774L;

    Map<Integer, List<Unit>> unitsByType = new HashMap<>();
    Map<Integer, JList<Person>> techListsByUnitType = new HashMap<>();
    Map<Integer, JLabel> timeLabelsByUnitType = new HashMap<>();
    Campaign campaign;
    boolean activating;

    JScrollPane scrollPane = new JScrollPane();
    JPanel contentPanel = new JPanel();

    /**
     * Constructor
     * @param parent MekHQ frame
     * @param units An array of unit IDs to mothball/activate
     * @param campaign Campaign with which we're working
     * @param activate Whether to activate or mothball
     */
    public MassMothballDialog(Frame parent, Unit[] units, Campaign campaign, boolean activate) {
        super(parent, "Mass Mothball/Activate");
        setLocationRelativeTo(parent);

        sortUnitsByType(units);
        this.campaign = campaign;

        contentPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 3;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.ipadx = 4;
        gbc.ipady = 4;
        gbc.insets = new Insets(2, 2, 2, 2);

        gbc.weightx = 3;
        JLabel instructionLabel = new JLabel();
        instructionLabel.setBorder(new LineBorder(Color.BLUE));
        instructionLabel.setText("<html>Choose the techs to carry out mothball/reactivation operations on the displayed units. <br/>A * indicates that the tech is currently maintaining units.</html>");
        contentPanel.add(instructionLabel, gbc);

        gbc.weightx = 1;
        gbc.gridy++;
        addTableHeaders(gbc);

        for(int unitType : unitsByType.keySet()) {
            gbc.gridy++;
            addUnitTypePanel(unitType, gbc);
        }

        gbc.gridy++;
        addExecuteButton(activate, gbc);

        scrollPane.setViewportView(contentPanel);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setMaximumSize(new Dimension(600, 600));
        scrollPane.setPreferredSize(new Dimension(600, 600));
        getContentPane().add(scrollPane);

        this.setResizable(true);
        this.pack();
        this.validate();
        setUserPreferences();
    }

    /**
     * Adds the table headers to the content pane
     * @param gbc
     */
    private void addTableHeaders(GridBagConstraints gbc) {
        gbc.gridwidth = 1;
        gbc.weightx = .3;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridx = 0;
        JLabel labelUnitNames = new JLabel();
        labelUnitNames.setText("Units to Process");
        contentPanel.add(labelUnitNames, gbc);

        gbc.gridx = 1;
        JLabel labelTechs = new JLabel();
        labelTechs.setText("Available Techs");
        contentPanel.add(labelTechs, gbc);

        gbc.gridx = 2;
        JLabel labelPlaceHolder = new JLabel();
        labelPlaceHolder.setText(" ");
        contentPanel.add(labelPlaceHolder, gbc);
    }

    /**
     * Adds a row of units, techs and time summary to the content pane
     * @param unitType
     * @param gbc
     */
    private void addUnitTypePanel(int unitType, GridBagConstraints gbc) {
        gbc.gridwidth = 1;
        gbc.weightx = .3;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        JPanel unitPanel = new JPanel();
        unitPanel.setLayout(new GridLayout(unitsByType.get(unitType).size(), 1, 1, 0));

        for(Unit unit : unitsByType.get(unitType)) {
            JLabel unitLabel = new JLabel();
            unitLabel.setText(unit.getName());
            unitPanel.add(unitLabel);
        }

        unitPanel.setBorder(new LineBorder(Color.GRAY, 1));
        contentPanel.add(unitPanel, gbc);

        gbc.gridx = 1;

        JList<Person> techList = new JList<Person>();
        DefaultListModel<Person> listModel = new DefaultListModel<Person>();

        for(Person tech : campaign.getTechs()) {
            if(tech.canTech(unitsByType.get(unitType).get(0).getEntity())) {
                listModel.addElement(tech);
            }
        }

        techList.setModel(listModel);
        techList.addListSelectionListener(this);
        techList.setBorder(new LineBorder(Color.GRAY, 1));
        techList.setCellRenderer(new TechListCellRenderer());

        JScrollPane techListPane = new JScrollPane();
        techListPane.setViewportView(techList);
        techListPane.setMaximumSize(new Dimension(200, 100));
        techListPane.setPreferredSize(new Dimension(200, 50));

        contentPanel.add(techListPane, gbc);
        techListsByUnitType.put(unitType, techList);

        gbc.gridx = 2;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel labelTotalTime = new JLabel();
        labelTotalTime.setBorder(new LineBorder(Color.BLUE));
        labelTotalTime.setText(getCompletionTimeText(0));
        timeLabelsByUnitType.put(unitType, labelTotalTime);
        contentPanel.add(labelTotalTime, gbc);
    }

    /**
     * Renders the mothball/activate button on the content pane
     * @param activate Whether the button is "mothball" or "activate"
     * @param gbc
     */
    private void addExecuteButton(boolean activate, GridBagConstraints gbc) {
        gbc.gridx = 1;
        gbc.weightx = .8;
        gbc.weighty = .8;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton buttonExecute = new JButton();
        buttonExecute.setText(activate ? "Activate" : "Mothball");
        buttonExecute.setActionCommand(activate ? UnitTableMouseAdapter.COMMAND_ACTIVATE : UnitTableMouseAdapter.COMMAND_MOTHBALL);
        buttonExecute.addActionListener(this);
        contentPanel.add(buttonExecute, gbc);
    }

    private void setUserPreferences() {
        PreferencesNode preferences = MekHQ.getPreferences().forClass(MassMothballDialog.class);

        this.setName("dialog");
        preferences.manage(new JWindowPreference(this));
    }

    /**
     * Worker function that sorts out the passed-in units by unit type and stores them in the local dictionary.
     * @param units Units to sort
     */
    private void sortUnitsByType(Unit[] units) {
        for(int x = 0; x < units.length; x++) {
            int unitType = units[x].getEntity().getUnitType();

            if(!unitsByType.containsKey(unitType)) {
                unitsByType.put(unitType, new ArrayList<>());
            }

            unitsByType.get(unitType).add(units[x]);
        }
    }

    /**
     * Event handler for the mothball/activate button.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getActionCommand() != UnitTableMouseAdapter.COMMAND_MOTHBALL &&
                e.getActionCommand() != UnitTableMouseAdapter.COMMAND_ACTIVATE) {
            return;
        }

        boolean isMothballing = e.getActionCommand() == UnitTableMouseAdapter.COMMAND_MOTHBALL;

        for(int unitType : unitsByType.keySet()) {
            List<Person> selectedTechs = techListsByUnitType.get(unitType).getSelectedValuesList();
            int techIndex = 0;

            // this is a "naive" approach, where we assign each of the selected techs
            // to approximately # units / # techs in mothball/reactivation tasks
            for(Unit unit : unitsByType.get(unitType)) {
                UUID id = selectedTechs.get(techIndex).getId();
                if (isMothballing) {
                    unit.startMothballing(id);
                } else {
                    unit.startActivating(id);
                }
                MekHQ.triggerEvent(new UnitChangedEvent(unit));

                if(techIndex == selectedTechs.size() - 1) {
                    techIndex = 0;
                } else {
                    techIndex++;
                }
            }
        }

        this.setVisible(false);
    }

    /**
     * Event handler for when an item is clicked on a tech list.
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        @SuppressWarnings("unchecked")
        JList<Person> techList = (JList<Person>) e.getSource();

        int unitType = -1;
        // this is mildly kludgy:
        // we scan the 'tech lists by unit type' dictionary to determine the unit type
        // since the number of tech lists is limited by the number of unit types, it shouldn't be too problematic for performance
        for(int key : techListsByUnitType.keySet()) {
            if(techListsByUnitType.get(key).equals(techList)) {
                unitType = key;
                break;
            }
        }

        // time to do the work is # units * 2 if mothballing * work day in minutes;
        int workTime = unitsByType.get(unitType).size() * (activating ? 1 : 2) * Unit.TECH_WORK_DAY;
        // a unit can only be mothballed by one tech, so it's pointless to assign more techs to the task
        int numTechs = Math.min(techList.getSelectedValuesList().size(), unitsByType.get(unitType).size());

        timeLabelsByUnitType.get(unitType).setText(getCompletionTimeText(workTime / numTechs));
        pack();
    }

    /**
     * Worker function that determines the "completion time" text based on the passed-in number.
     * @param completionTime How many minutes to complete the work.
     * @return Displayable text.
     */
    private String getCompletionTimeText(int completionTime) {
        if(completionTime > 0) {
            return String.format("Completion Time: %d minutes", completionTime);
        } else {
            return "<html>Completion Time: <span color='red'>Never</span></html>";
        }
    }

    /**
     * Custom list cell renderer that displays a * next to the name of a person who's maintaining units.
     * @author NickAragua
     *
     */
    class TechListCellRenderer extends DefaultListCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -1552997620131149101L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            Person person = (Person) value;

            boolean maintainsUnits = person.getTechUnitIDs().size() > 0;
            setText(person.getFullName() + (maintainsUnits ? " (*)" : ""));

            return this;
        }

    }
}
