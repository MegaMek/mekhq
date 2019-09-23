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

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;

import megamek.common.OffBoardDirection;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.*;
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
    private JTextField txtForceName;
    
    private JLabel lblMagnitude;
    private JTextField txtAmount;
    private JComboBox<EffectScalingType> cboScalingType;
    private JComboBox<ObjectiveEffectType> cboEffectType;
    private JComboBox<String> cboEffectCondition;
    
    private JList<ObjectiveEffect> successEffects;
    private JList<ObjectiveEffect> failureEffects;
    private JList<String> forceNames;
        
    private ScenarioTemplate currentScenarioTemplate;
    private ScenarioObjective objective;
    
    public ObjectiveEditPanel(ScenarioTemplate template, Component parent) {
        currentScenarioTemplate = template;
        objective = new ScenarioObjective();
        
        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
    }
    
    private void initGUI() {
        lblShortDescription = new JLabel("Short Description:");
                
        txtShortDescription = new JTextArea();
        txtShortDescription.setColumns(40);
        txtShortDescription.setRows(5);
        
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

        addObjectiveTypeUI(gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        
        addSubjectForce(gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        
        addEffectUI(gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        
        JLabel lblSuccessEffects = new JLabel("Effects on completion:");
        JLabel lblFailureEffects = new JLabel("Effects on failure:");
        
        successEffects = new JList<>();
        failureEffects = new JList<>();
        
        getContentPane().add(lblSuccessEffects, gbc);
        gbc.gridx++;
        getContentPane().add(successEffects, gbc);
        gbc.gridx++;
        getContentPane().add(lblFailureEffects, gbc);
        gbc.gridx++;
        getContentPane().add(failureEffects, gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
    }
    
    /**
     * Handles the "objective type" row
     */
    private void addObjectiveTypeUI(GridBagConstraints gbc) {
        JPanel objectivePanel = new JPanel();
        
        lblObjectiveType = new JLabel("Objective Type:");        
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
        
        objectivePanel.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);
        
        
        objectivePanel.add(lblObjectiveType, localGbc);
        localGbc.gridx++;
        objectivePanel.add(cboObjectiveType, localGbc);
        localGbc.gridx++;
        objectivePanel.add(txtPercentage, localGbc);
        localGbc.gridx++;
        objectivePanel.add(cboCountType, localGbc);
        
        getContentPane().add(objectivePanel, gbc);
    }
    
    private void addSubjectForce(GridBagConstraints gbc) {
        JPanel forcePanel = new JPanel();
        
        JLabel forcesLabel = new JLabel("Force Names:");
        
        txtForceName = new JTextField();
        txtForceName.setColumns(40);
        
        forceNames = new JList<String>();
        
        JButton btnAdd = new JButton("Add");
        btnAdd.addActionListener(e -> this.addForce());
        
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);
        
        forcePanel.add(forcesLabel, localGbc);
        localGbc.gridx++;
        forcePanel.add(txtForceName, localGbc);
        localGbc.gridx++;
        forcePanel.add(forceNames, localGbc);
        localGbc.gridx++;
        forcePanel.add(btnAdd, localGbc);
        
        getContentPane().add(forcePanel, gbc);
    }
    
    /**
     * Handles the "add objective effect" row
     */
    private void addEffectUI(GridBagConstraints gbc) {
        JPanel effectPanel = new JPanel();
                
        lblMagnitude = new JLabel("Amount:");
        txtAmount = new JTextField();
        txtAmount.setColumns(5);
        
        JLabel lblScaling = new JLabel("Effect Scaling:");
        cboScalingType = new JComboBox<>();
        for(EffectScalingType scalingType : EffectScalingType.values()) {
            cboScalingType.addItem(scalingType);
        }
        
        JLabel lblEffectType = new JLabel("Effect Type:");
        cboEffectType = new JComboBox<>();
        for(ObjectiveEffectType scalingType : ObjectiveEffectType.values()) {
            cboEffectType.addItem(scalingType);
        }
        
        JLabel lblEffectCondition = new JLabel("Effect Condition:");
        cboEffectCondition = new JComboBox<>();
        cboEffectCondition.addItem("Victory");
        cboEffectCondition.addItem("Defeat");
        
        JButton btnAdd = new JButton("Add");
        btnAdd.addActionListener(e -> this.addEffect());
        
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);
        effectPanel.setLayout(new GridBagLayout());
        
        effectPanel.add(lblMagnitude, localGbc);
        localGbc.gridx++;
        effectPanel.add(txtAmount, localGbc);
        localGbc.gridx++;
        effectPanel.add(lblScaling, localGbc);
        localGbc.gridx++;
        effectPanel.add(cboScalingType, localGbc);
        localGbc.gridx++;
        effectPanel.add(lblEffectType, localGbc);
        localGbc.gridx++;
        effectPanel.add(cboEffectType, localGbc);
        localGbc.gridx++;
        effectPanel.add(lblEffectCondition, localGbc);
        localGbc.gridx++;
        effectPanel.add(cboEffectCondition, localGbc);
        localGbc.gridx++;
        effectPanel.add(btnAdd, localGbc);
        
        getContentPane().add(effectPanel, gbc);
    }
    
    
    
    private List<String> getAvailableForceNames() {
        List<String> retVal = new ArrayList<>();
        
        for(ScenarioForceTemplate forceTemplate : currentScenarioTemplate.getAllScenarioForces()) {
            retVal.add(forceTemplate.getForceName());
        }
        
        return retVal;
    }
    
    /**
     * Event handler for the 'add' button for scenario effects
     */
    private void addEffect() {
        int amount = 0; 
        try {
            amount = Integer.parseInt(txtAmount.getText());
            lblMagnitude.setForeground(Color.black);
        } catch(Exception e) {
            lblMagnitude.setForeground(Color.red);
            return;
        }
        
        ObjectiveEffect effect = new ObjectiveEffect();
        effect.howMuch = amount;
        effect.effectScaling = (EffectScalingType) cboScalingType.getSelectedItem();
        effect.effectType = (ObjectiveEffectType) cboEffectType.getSelectedItem();

        if(cboEffectCondition.getSelectedIndex() == 0) {
            objective.addSuccessEffect(effect);
            
            updateEffectList(successEffects, objective.getSuccessEffects());
        } else {
            objective.addFailureEffect(effect);
            
            updateEffectList(failureEffects, objective.getFailureEffects());
        }
        
        pack();
    }
    
    /**
     * Worker function that updates an objective effects list with the given objective effects
     */
    private void updateEffectList(JList<ObjectiveEffect> listToUpdate, List<ObjectiveEffect> objectiveEffects) {
        DefaultListModel<ObjectiveEffect> effectModel = new DefaultListModel<>();
        for(ObjectiveEffect currentEffect : objectiveEffects) {
            effectModel.addElement(currentEffect);
        }
        
        listToUpdate.setModel(effectModel);
    }
    
    private void removeEffect() {
        
    }
    
    private void addForce() {
        objective.addForce(txtForceName.getText());
        
        DefaultListModel<String> forceModel = new DefaultListModel<>();
        for(String forceName : objective.getAssociatedForceNames()) {
            forceModel.addElement(forceName);
        }
        
        forceNames.setModel(forceModel);
        pack();
    }
}
