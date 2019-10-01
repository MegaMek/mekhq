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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import megamek.common.OffBoardDirection;
import mekhq.campaign.mission.ObjectiveEffect;
import mekhq.campaign.mission.ObjectiveEffect.*;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.ScenarioTemplate;

/**
 * UI for creating or editing a single scenario objective
 *
 */
public class ObjectiveEditPanel extends JDialog {
    private JLabel lblShortDescription;
    private JTextArea txtShortDescription;
    private JLabel lblObjectiveType;
    private JComboBox<ObjectiveCriterion> cboObjectiveType;
    private JComboBox<String> cboDirection;
    private JTextField txtPercentage;
    private JComboBox<String> cboCountType;
    private JTextField txtForceName;
    
    private JLabel lblMagnitude;
    private JTextField txtAmount;
    private JComboBox<EffectScalingType> cboScalingType;
    private JComboBox<ObjectiveEffectType> cboEffectType;
    private JComboBox<ObjectiveEffectConditionType> cboEffectCondition;
    
    private JList<ObjectiveEffect> successEffects;
    private JList<ObjectiveEffect> failureEffects;
    private JButton btnRemoveSuccess;
    private JButton btnRemoveFailure;
    
    private JList<String> forceNames;
    JButton btnRemove;
        
    private ScenarioTemplate currentScenarioTemplate;
    private ScenarioObjective objective;
    private ScenarioTemplateEditorDialog parent;
    
    public ObjectiveEditPanel(ScenarioTemplate template, ScenarioTemplateEditorDialog parent) {
        currentScenarioTemplate = template;
        objective = new ScenarioObjective();
        this.parent = parent;
        
        initGUI();
        validate();
        pack();
        setLocationRelativeTo(parent);
    }
    
    public ObjectiveEditPanel(ScenarioTemplate template, ScenarioObjective objective, ScenarioTemplateEditorDialog parent) {
        currentScenarioTemplate = template;
        this.objective = objective;
        this.parent = parent;
        
        initGUI();
        updateForceList();
        
        txtShortDescription.setText(objective.getDescription());
        cboObjectiveType.setSelectedItem(objective.getObjectiveCriterion());
        cboCountType.setSelectedItem(objective.getAmountType());
        txtPercentage.setText(Integer.toString(objective.getAmount()));
        setDirectionDropdownVisibility();
        
        cboDirection.setSelectedIndex(objective.getDestinationEdge().ordinal());
        
        updateEffectList(successEffects, objective.getSuccessEffects());
        updateEffectList(failureEffects, objective.getFailureEffects());
        
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
        gbc.anchor = GridBagConstraints.WEST;
        
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
        
        addObjectiveEffectUI(gbc);
        
        gbc.gridx = 0;
        gbc.gridy++;
        
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> this.setVisible(false));
        JButton btnSaveAndClose = new JButton("Save and Close");
        btnSaveAndClose.addActionListener(e -> this.saveObjectiveAndClose());
        
        getContentPane().add(btnCancel);
        getContentPane().add(btnSaveAndClose);
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
        cboObjectiveType.addActionListener(e -> this.setDirectionDropdownVisibility());
        
        txtPercentage = new JTextField();
        txtPercentage.setColumns(4);
        
        cboCountType = new JComboBox<>();
        cboCountType.addItem("Percent");
        cboCountType.addItem("Fixed Amount");
        
        
        cboDirection = new JComboBox<>();
        cboDirection.addItem("Force Destination Edge");
        for(int x = 1; x < OffBoardDirection.values().length; x++) {
            cboDirection.addItem(OffBoardDirection.values()[x].toString());
        }
        cboDirection.setVisible(false);
        
        objectivePanel.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);
        
        
        objectivePanel.add(lblObjectiveType, localGbc);
        localGbc.gridx++;
        objectivePanel.add(cboObjectiveType, localGbc);
        localGbc.gridx++;
        objectivePanel.add(cboDirection, localGbc);
        localGbc.gridx++;
        objectivePanel.add(txtPercentage, localGbc);
        localGbc.gridx++;
        objectivePanel.add(cboCountType, localGbc);
        
        getContentPane().add(objectivePanel, gbc);
    }
    
    /**
     * Handles the UI for adding objective effects
     */
    private void addObjectiveEffectUI(GridBagConstraints gbc) {
        JPanel effectPanel = new JPanel();
        
        
        JLabel lblSuccessEffects = new JLabel("Effects on completion:");
        JLabel lblFailureEffects = new JLabel("Effects on failure:");
        
        successEffects = new JList<>();
        successEffects.addListSelectionListener(e -> btnRemoveSuccess.setEnabled(successEffects.getSelectedValuesList().size() > 0));
        failureEffects = new JList<>();
        failureEffects.addListSelectionListener(e -> btnRemoveFailure.setEnabled(failureEffects.getSelectedValuesList().size() > 0));
        
        btnRemoveSuccess = new JButton("Remove");
        btnRemoveSuccess.addActionListener(e -> this.removeEffect(ObjectiveEffectConditionType.ObjectiveSuccess));
        btnRemoveSuccess.setEnabled(false);
        
        btnRemoveFailure = new JButton("Remove");
        btnRemoveFailure.addActionListener(e -> this.removeEffect(ObjectiveEffectConditionType.ObjectiveFailure));
        btnRemoveFailure.setEnabled(false);
        
        GridBagConstraints localGbc = new GridBagConstraints();
        effectPanel.setLayout(new GridBagLayout());
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);
        
        effectPanel.add(lblSuccessEffects, localGbc);
        localGbc.gridx++;
        effectPanel.add(successEffects, localGbc);
        localGbc.gridx++;
        effectPanel.add(btnRemoveSuccess, localGbc);
        localGbc.gridx++;
        effectPanel.add(lblFailureEffects, localGbc);
        localGbc.gridx++;
        effectPanel.add(failureEffects, localGbc);
        localGbc.gridx++;
        effectPanel.add(btnRemoveFailure, localGbc);
        
        getContentPane().add(effectPanel, gbc);
    }
    
    /**
     * Handles the UI for adding/removing forces relevant to this objective
     */
    private void addSubjectForce(GridBagConstraints gbc) {
        JPanel forcePanel = new JPanel();
        
        JLabel forcesLabel = new JLabel("Force Names:");
        
        txtForceName = new JTextField();
        txtForceName.setColumns(40);
        
        forceNames = new JList<String>();
        forceNames.setVisibleRowCount(5);
        forceNames.addListSelectionListener(e -> btnRemove.setEnabled(forceNames.getSelectedValuesList().size() > 0));
        
        JButton btnAdd = new JButton("Add");
        btnAdd.addActionListener(e -> this.addForce());
        
        btnRemove = new JButton("Remove");
        btnRemove.addActionListener(e -> this.removeForce());
        btnRemove.setEnabled(false);
        
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);
        
        forcePanel.add(forcesLabel, localGbc);
        localGbc.gridx++;
        forcePanel.add(txtForceName, localGbc);
        localGbc.gridx++;
        forcePanel.add(btnAdd, localGbc);
        localGbc.gridx--;
        localGbc.gridy++;
        forcePanel.add(forceNames, localGbc);
        localGbc.gridx++;
        forcePanel.add(btnRemove, localGbc);
        
        
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
        cboEffectCondition.addItem(ObjectiveEffectConditionType.ObjectiveSuccess);
        cboEffectCondition.addItem(ObjectiveEffectConditionType.ObjectiveFailure);
        
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

        if(cboEffectCondition.getSelectedItem() == ObjectiveEffectConditionType.ObjectiveSuccess) {
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
    
    private void removeEffect(ObjectiveEffectConditionType conditionType) {
        JList<ObjectiveEffect> listToUpdate;
        List<ObjectiveEffect> objectiveEffects;
        
        if(conditionType == ObjectiveEffectConditionType.ObjectiveSuccess) {
            listToUpdate = successEffects;
            objectiveEffects = objective.getSuccessEffects();
            btnRemoveSuccess.setEnabled(false);
        } else {
            listToUpdate = failureEffects;
            objectiveEffects = objective.getFailureEffects();
            btnRemoveFailure.setEnabled(false);
        }
        
        for(ObjectiveEffect effectToRemove : listToUpdate.getSelectedValuesList()) {
            objectiveEffects.remove(effectToRemove);
        }
        
        updateEffectList(listToUpdate, objectiveEffects);
    }
    
    private void addForce() {
        objective.addForce(txtForceName.getText());
        
        updateForceList();        
        pack();
    }
    
    private void removeForce() {
        for(String forceName : forceNames.getSelectedValuesList()) {
            objective.removeForce(forceName);
        }
        
        updateForceList();
        btnRemove.setEnabled(false);
        pack();
    }
    
    private void updateForceList() {
        DefaultListModel<String> forceModel = new DefaultListModel<>();
        for(String forceName : objective.getAssociatedForceNames()) {
            forceModel.addElement(forceName);
        }
        
        forceNames.setModel(forceModel);
    }
    
    private void setDirectionDropdownVisibility() {
        switch((ObjectiveCriterion) cboObjectiveType.getSelectedItem()) {
        case PreventReachMapEdge:
        case ReachMapEdge:
            cboDirection.setVisible(true);
            break;            
        default:
            cboDirection.setVisible(false);
            break;
        }
    }
    
    private void saveObjectiveAndClose() {
        int number = 0;
        
        try {
            number = Integer.parseInt(txtPercentage.getText());
            txtPercentage.setBorder(null);
        } catch(Exception e) {
            txtPercentage.setBorder(new LineBorder(Color.red));
            return;
        }
        
        objective.setObjectiveCriterion((ObjectiveCriterion) cboObjectiveType.getSelectedItem());
        objective.setDescription(txtShortDescription.getText());
        if(this.cboCountType.getSelectedIndex() == 0) {
            objective.setPercentage(number);
        } else {
            objective.setFixedAmount(number);
        }
        
        if(cboDirection.isVisible() && cboDirection.getSelectedIndex() > 0) {
            objective.setDestinationEdge(OffBoardDirection.getDirection(cboDirection.getSelectedIndex() - 1));
        } else {
            objective.setDestinationEdge(OffBoardDirection.NONE);
        }
             
            
        if(!currentScenarioTemplate.scenarioObjectives.contains(objective)) {
            currentScenarioTemplate.scenarioObjectives.add(objective);
        }
        
        parent.updateObjectiveList();
        setVisible(false);
    }
}
