/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import megamek.common.OffBoardDirection;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.gui.utilities.Java2sAutoTextField;

public class ObjectiveEditPanel extends JDialog {
    private JLabel lblShortDescription;
    private JTextArea txtShortDescription;
    private JLabel lblObjectiveType;
    private JComboBox<ObjectiveCriterion> cboObjectiveType;
    private JLabel lblDirection;
    private JComboBox<OffBoardDirection> cboDirection;
    private JTextField txtPercentage;
    private JComboBox<String> cboCountType;
    private JLabel forceName;
    private Java2sAutoTextField txtForceName;
    
    private ScenarioTemplate currentScenarioTemplate;
    
    public ObjectiveEditPanel(ScenarioTemplate template, Component parent) {
        currentScenarioTemplate = template;
        
        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initGUI() {
        lblShortDescription = new JLabel("Short Description:");
        lblObjectiveType = new JLabel("Objective Type:");
                
        txtShortDescription = new JTextArea();
        txtShortDescription.setColumns(40);
        txtShortDescription.setRows(5);
        
        cboObjectiveType = new JComboBox<>();
        for(ObjectiveCriterion objectiveType : ObjectiveCriterion.values()) {
            cboObjectiveType.addItem(objectiveType);
        }
        
        txtPercentage = new JTextField();
        txtPercentage.setColumns(4);
        
        cboCountType = new JComboBox<>();
        cboCountType.addItem("Percent");
        cboCountType.addItem("Fixed Amount");
        
        
        cboDirection = new JComboBox<>();
        for(OffBoardDirection direction : OffBoardDirection.values()) {
            cboDirection.addItem(direction);
        }
        
        txtForceName = new Java2sAutoTextField(getAvailableForceNames());
        txtForceName.setColumns(40);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        getContentPane().setLayout(new GridBagLayout());
        
        getContentPane().add(lblShortDescription, gbc);
        gbc.gridx++;
        getContentPane().add(txtShortDescription, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        getContentPane().add(lblObjectiveType, gbc);
        gbc.gridx++;
        getContentPane().add(cboObjectiveType, gbc);
        gbc.gridx++;
        getContentPane().add(txtPercentage, gbc);
        gbc.gridx++;
        getContentPane().add(cboCountType, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        
    }
    
    private List<String> getAvailableForceNames() {
        List<String> retVal = new ArrayList<>();
        
        for(ScenarioForceTemplate forceTemplate : currentScenarioTemplate.getAllScenarioForces()) {
            retVal.add(forceTemplate.getForceName());
        }
        
        return retVal;
    }
}
