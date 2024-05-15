/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.OffBoardDirection;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectConditionType;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveAmountType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.ScenarioObjective.TimeLimitType;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.ResourceBundle;

public class CustomizeScenarioObjectiveDialog extends JDialog {

    private ScenarioObjective objective;
    private List<String> botForceNames;
    private JPanel panObjectiveType;
    private JPanel panForce;
    private JPanel panTimeLimits;
    private JPanel panObjectiveEffect;
    private JTextField txtShortDescription;
    private JComboBox<ObjectiveCriterion> cboObjectiveType;
    private JComboBox<String> cboDirection;
    private JSpinner spnAmount;
    private SpinnerNumberModel modelPercent;
    private SpinnerNumberModel modelFixed;
    private MMComboBox<ObjectiveAmountType> cboCountType;
    private JComboBox<String> cboForceName;

    private JSpinner spnScore;
    private JComboBox<EffectScalingType> cboScalingType;
    private JComboBox<ObjectiveEffectType> cboEffectType;
    private JComboBox<ObjectiveEffectConditionType> cboEffectCondition;

    private JList<ObjectiveEffect> successEffects;
    private JList<ObjectiveEffect> failureEffects;
    private JButton btnRemoveSuccess;
    private JButton btnRemoveFailure;

    private JComboBox<String> cboTimeLimitDirection;
    private JComboBox<TimeLimitType> cboTimeScaling;
    private JSpinner spnTimeLimit;

    private JList<String> forceNames;
    JButton btnRemove;

    private JList<String> lstDetails;

    DefaultListModel<String> forceModel = new DefaultListModel<>();
    DefaultListModel<ObjectiveEffect> successEffectsModel = new DefaultListModel<>();
    DefaultListModel<ObjectiveEffect> failureEffectsModel = new DefaultListModel<>();

    DefaultListModel<String> detailModel = new DefaultListModel<>();


    public CustomizeScenarioObjectiveDialog(JFrame parent, boolean modal, ScenarioObjective objective, List<String> botForceNames) {
        super(parent, modal);
        this.objective = objective;
        this.botForceNames = botForceNames;

        initialize();

        for (String forceName : objective.getAssociatedForceNames()) {
            forceModel.addElement(forceName);
        }

        txtShortDescription.setText(objective.getDescription());
        cboObjectiveType.setSelectedItem(objective.getObjectiveCriterion());
        cboCountType.setSelectedItem(objective.getAmountType());
        spnAmount.setValue(objective.getAmount());
        setDirectionDropdownVisibility();

        cboDirection.setSelectedIndex(objective.getDestinationEdge().ordinal());

        cboTimeScaling.setSelectedItem(objective.getTimeLimitType());
        updateTimeLimitUI();
        cboTimeLimitDirection.setSelectedIndex(objective.isTimeLimitAtMost() ? 0 : 1);
        if (objective.getTimeLimitType() == TimeLimitType.ScaledToPrimaryUnitCount) {
            spnTimeLimit.setValue(objective.getTimeLimitScaleFactor());
        } else {
            if (objective.getTimeLimit() != null) {
                spnTimeLimit.setValue(objective.getTimeLimit());
            }
        }

        for (ObjectiveEffect currentEffect : objective.getSuccessEffects()) {
            successEffectsModel.addElement(currentEffect);
        }
        for (ObjectiveEffect currentEffect : objective.getFailureEffects()) {
            failureEffectsModel.addElement(currentEffect);
        }

        for (String detail : objective.getDetails()) {
            detailModel.addElement(detail);
        }

        validate();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initialize() {

        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CustomizeScenarioObjectiveDialog",
                MekHQ.getMHQOptions().getLocale());

        setTitle(resourceMap.getString("dialog.title"));
        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel(resourceMap.getString("lblDescription.text")), gbc);

        txtShortDescription = new JTextField();
        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panMain.add(txtShortDescription, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panMain.add(new JLabel(resourceMap.getString("lblDetails.text")), gbc);

        JTextField txtDetail = new JTextField();
        txtDetail.setColumns(40);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        panMain.add(txtDetail, gbc);

        JButton btnAddDetail = new JButton(resourceMap.getString("btnAdd.text"));
        btnAddDetail.addActionListener(e -> this.addDetail(txtDetail));
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panMain.add(btnAddDetail, gbc);


        lstDetails = new JList<>(detailModel);
        JButton btnRemoveDetail = new JButton(resourceMap.getString("btnRemove.text"));
        btnRemoveDetail.addActionListener(e -> this.removeDetails());
        lstDetails.addListSelectionListener(e -> btnRemoveDetail.setEnabled(!lstDetails.getSelectedValuesList().isEmpty()));
        lstDetails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrDetails = new JScrollPane(lstDetails);
        scrDetails.setMinimumSize(new Dimension(200, 100));
        scrDetails.setPreferredSize(new Dimension(200, 100));
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        panMain.add(scrDetails, gbc);

        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panMain.add(btnRemoveDetail, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        panMain.add(new JLabel(resourceMap.getString("lblObjectiveType.text")), gbc);
        initObjectiveTypePanel();
        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panMain.add(panObjectiveType, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panMain.add(new JLabel(resourceMap.getString("lblForceNames.text")), gbc);

        initForcePanel(resourceMap);
        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panMain.add(panForce, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        panMain.add(new JLabel(resourceMap.getString("lblTimeLimit.text")), gbc);

        initTimeLimitPanel();
        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panMain.add(panTimeLimits, gbc);

        initObjectiveEffectPanel(resourceMap);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panMain.add(panObjectiveEffect, gbc);

        getContentPane().add(panMain, BorderLayout.CENTER);

        JPanel panButtons = new JPanel(new FlowLayout());
        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.addActionListener(e -> this.setVisible(false));
        JButton btnOK = new JButton(resourceMap.getString("btnOK.text"));
        btnOK.addActionListener(e -> this.saveObjectiveAndClose());
        panButtons.add(btnOK);
        panButtons.add(btnCancel);
        getContentPane().add(panButtons, BorderLayout.PAGE_END);

    }

    /**
     * Handles the "objective type" row
     */
    private void initObjectiveTypePanel() {
        panObjectiveType = new JPanel(new GridBagLayout());
        cboObjectiveType = new JComboBox<>();
        for (ObjectiveCriterion objectiveType : ObjectiveCriterion.values()) {
            cboObjectiveType.addItem(objectiveType);
        }
        cboObjectiveType.addActionListener(e -> this.setDirectionDropdownVisibility());

        modelPercent = new SpinnerNumberModel(0, 0, 100, 5);
        modelFixed = new SpinnerNumberModel(0, 0, null, 1);
        spnAmount = new JSpinner(modelPercent);

        cboCountType = new MMComboBox<>("cboCountType", ObjectiveAmountType.values());
        cboCountType.addActionListener(etv -> switchNumberModel());


        cboDirection = new JComboBox<>();
        cboDirection.addItem("Force Destination Edge");
        for (int x = 1; x < OffBoardDirection.values().length; x++) {
            cboDirection.addItem(OffBoardDirection.values()[x].toString());
        }
        cboDirection.setVisible(false);

        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.WEST;
        localGbc.insets = new Insets(0, 0, 0, 5);

        panObjectiveType.add(cboObjectiveType, localGbc);
        localGbc.gridx++;
        panObjectiveType.add(cboDirection, localGbc);
        localGbc.gridx++;
        panObjectiveType.add(spnAmount, localGbc);
        localGbc.gridx++;
        localGbc.weightx = 1.0;
        panObjectiveType.add(cboCountType, localGbc);

    }

    /**
     * Handles the UI for adding/removing forces relevant to this objective
     */
    private void initForcePanel(ResourceBundle resourceMap) {
        panForce = new JPanel(new GridBagLayout());

        cboForceName = new JComboBox<>();
        cboForceName.addItem(MHQConstants.EGO_OBJECTIVE_NAME);
        for (String name : botForceNames) {
            cboForceName.addItem(name);
        }

        forceNames = new JList<>(forceModel);
        forceNames.setVisibleRowCount(5);
        forceNames.addListSelectionListener(e -> btnRemove.setEnabled(!forceNames.getSelectedValuesList().isEmpty()));

        JButton btnAdd = new JButton(resourceMap.getString("btnAdd.text"));
        btnAdd.addActionListener(e -> this.addForce());

        btnRemove = new JButton(resourceMap.getString("btnRemove.text"));
        btnRemove.addActionListener(e -> this.removeForce());
        btnRemove.setEnabled(false);

        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.NORTHWEST;
        localGbc.insets = new Insets(0, 0, 0, 5);

        panForce.add(cboForceName, localGbc);
        localGbc.gridx++;
        panForce.add(btnAdd, localGbc);
        localGbc.gridx++;
        JScrollPane scrForceNames = new JScrollPane(forceNames);
        scrForceNames.setMinimumSize(new Dimension(250, 100));
        scrForceNames.setPreferredSize(new Dimension(250, 100));
        panForce.add(scrForceNames, localGbc);
        localGbc.gridx++;
        localGbc.weightx = 1.0;
        panForce.add(btnRemove, localGbc);
    }

    /**
     * Handles the UI for adding objective effects
     */
    private void initObjectiveEffectPanel(ResourceBundle resourceMap) {
        panObjectiveEffect = new JPanel(new GridBagLayout());
        panObjectiveEffect.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("panObjectiveEffect.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));

        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 0;
        gbcLeft.anchor = GridBagConstraints.WEST;
        gbcLeft.fill = GridBagConstraints.NONE;
        gbcLeft.weightx = 0.0;

        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.gridx = 1;
        gbcRight.gridy = 0;
        gbcRight.anchor = GridBagConstraints.WEST;
        gbcRight.fill = GridBagConstraints.NONE;
        gbcRight.weightx = 1.0;

        JLabel lblMagnitude = new JLabel(resourceMap.getString("lblMagnitude.text"));
        panObjectiveEffect.add(lblMagnitude, gbcLeft);
        spnScore = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));
        panObjectiveEffect.add(spnScore, gbcRight);

        gbcLeft.gridy++;
        gbcRight.gridy++;
        panObjectiveEffect.add(new JLabel(resourceMap.getString("lblEffectScaling.text")), gbcLeft);
        cboScalingType = new JComboBox<>();
        for (EffectScalingType scalingType : EffectScalingType.values()) {
            cboScalingType.addItem(scalingType);
        }
        panObjectiveEffect.add(cboScalingType, gbcRight);

        gbcLeft.gridy++;
        gbcRight.gridy++;
        panObjectiveEffect.add(new JLabel(resourceMap.getString("lblEffectType.text")), gbcLeft);
        cboEffectType = new JComboBox<>();
        for (ObjectiveEffectType scalingType : ObjectiveEffectType.values()) {
            cboEffectType.addItem(scalingType);
        }
        panObjectiveEffect.add(cboEffectType, gbcRight);

        gbcLeft.gridy++;
        gbcRight.gridy++;
        panObjectiveEffect.add(new JLabel(resourceMap.getString("lblEffectCondition.text")), gbcLeft);
        cboEffectCondition = new JComboBox<>();
        cboEffectCondition.addItem(ObjectiveEffectConditionType.ObjectiveSuccess);
        cboEffectCondition.addItem(ObjectiveEffectConditionType.ObjectiveFailure);
        panObjectiveEffect.add(cboEffectCondition, gbcRight);

        JButton btnAdd = new JButton(resourceMap.getString("btnAdd.text"));
        btnAdd.addActionListener(e -> this.addEffect());
        gbcLeft.gridy++;
        panObjectiveEffect.add(btnAdd, gbcLeft);

        JLabel lblSuccessEffects = new JLabel(resourceMap.getString("lblSuccessEffects.text"));
        JLabel lblFailureEffects = new JLabel(resourceMap.getString("lblSuccessEffects.text"));

        successEffects = new JList<>(successEffectsModel);
        successEffects.addListSelectionListener(e -> btnRemoveSuccess.setEnabled(!successEffects.getSelectedValuesList().isEmpty()));
        failureEffects = new JList<>(failureEffectsModel);
        failureEffects.addListSelectionListener(e -> btnRemoveFailure.setEnabled(!failureEffects.getSelectedValuesList().isEmpty()));

        btnRemoveSuccess = new JButton(resourceMap.getString("btnRemove.text"));
        btnRemoveSuccess.addActionListener(e -> this.removeEffect(ObjectiveEffectConditionType.ObjectiveSuccess));
        btnRemoveSuccess.setEnabled(false);

        btnRemoveFailure = new JButton(resourceMap.getString("btnRemove.text"));
        btnRemoveFailure.addActionListener(e -> this.removeEffect(ObjectiveEffectConditionType.ObjectiveFailure));
        btnRemoveFailure.setEnabled(false);

        JPanel panBottom = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5,5,0,5);
        panBottom.add(lblSuccessEffects, gbc);
        gbc.gridx++;
        gbc.gridx++;
        panBottom.add(lblFailureEffects, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JScrollPane scrSuccessEffects = new JScrollPane(successEffects);
        scrSuccessEffects.setMinimumSize(new Dimension(300, 100));
        scrSuccessEffects.setPreferredSize(new Dimension(300, 100));
        panBottom.add(scrSuccessEffects, gbc);
        gbc.gridx++;
        panBottom.add(btnRemoveSuccess, gbc);
        gbc.gridx++;
        JScrollPane scrFailureEffects = new JScrollPane(failureEffects);
        scrFailureEffects.setMinimumSize(new Dimension(300, 100));
        scrFailureEffects.setPreferredSize(new Dimension(300, 100));
        panBottom.add(scrFailureEffects, gbc);
        gbc.gridx++;
        panBottom.add(btnRemoveFailure, gbc);

        gbcLeft.gridy++;
        gbcLeft.gridwidth = 3;
        gbcLeft.anchor = GridBagConstraints.WEST;
        gbcLeft.fill = GridBagConstraints.BOTH;
        gbcLeft.weightx = 1.0;
        gbcLeft.weighty = 1.0;
        panObjectiveEffect.add(panBottom, gbcLeft);

    }

    private void initTimeLimitPanel() {
        panTimeLimits = new JPanel(new GridBagLayout());

        cboTimeLimitDirection = new JComboBox<>();
        cboTimeLimitDirection.addItem("at most");
        cboTimeLimitDirection.addItem("at least");

        cboTimeScaling = new JComboBox<>();
        for (TimeLimitType timeLimitType : TimeLimitType.values()) {
            cboTimeScaling.addItem(timeLimitType);
        }
        cboTimeScaling.addActionListener(e -> this.updateTimeLimitUI());

        spnTimeLimit = new JSpinner(new SpinnerNumberModel(1, 1, null, 1));

        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.anchor = GridBagConstraints.NORTHWEST;
        localGbc.insets = new Insets(0, 0, 0, 5);

        panTimeLimits.add(cboTimeLimitDirection, localGbc);
        localGbc.gridx++;
        panTimeLimits.add(cboTimeScaling, localGbc);
        localGbc.gridx++;
        localGbc.weightx = 1.0;
        panTimeLimits.add(spnTimeLimit, localGbc);
    }

    private void switchNumberModel() {
        int value = (int) spnAmount.getValue();
        if(cboCountType.getSelectedItem() == ObjectiveAmountType.Percentage) {
            if(value > 100) {
                value = 100;
            }
            modelPercent.setValue(value);
            spnAmount.setModel(modelPercent);
        } else {
            modelFixed.setValue(value);
            spnAmount.setModel(modelFixed);
        }
    }

    /**
     * Event handler for the 'add' button for scenario effects
     */
    private void addEffect() {

        ObjectiveEffect effect = new ObjectiveEffect();
        effect.howMuch = (int) spnScore.getValue();
        effect.effectScaling = (EffectScalingType) cboScalingType.getSelectedItem();
        effect.effectType = (ObjectiveEffectType) cboEffectType.getSelectedItem();

        if (cboEffectCondition.getSelectedItem() == ObjectiveEffectConditionType.ObjectiveSuccess) {
            successEffectsModel.addElement(effect);
            successEffects.repaint();
        } else {
            failureEffectsModel.addElement(effect);
        }

        pack();
    }

    private void removeEffect(ObjectiveEffectConditionType conditionType) {
        JList<ObjectiveEffect> targetList;
        DefaultListModel<ObjectiveEffect> modelToUpdate;

        if (conditionType == ObjectiveEffectConditionType.ObjectiveSuccess) {
            targetList = successEffects;
            modelToUpdate = (DefaultListModel<ObjectiveEffect>) successEffects.getModel();
            btnRemoveSuccess.setEnabled(false);
        } else {
            targetList = failureEffects;
            modelToUpdate = (DefaultListModel<ObjectiveEffect>) failureEffects.getModel();
            btnRemoveFailure.setEnabled(false);
        }

        for (ObjectiveEffect effectToRemove : targetList.getSelectedValuesList()) {
            modelToUpdate.removeElement(effectToRemove);
        }
    }

    private void addForce() {
        forceModel.addElement(cboForceName.getSelectedItem().toString());
        pack();
    }

    private void removeForce() {
        for (String forceName : forceNames.getSelectedValuesList()) {
           forceModel.removeElement(forceName);
        }
        btnRemove.setEnabled(false);
        pack();
    }

    private void addDetail(JTextField field) {
        detailModel.addElement(field.getText());
    }

    private void removeDetails() {
        for (String detail : lstDetails.getSelectedValuesList()) {
            detailModel.removeElement(detail);
        }
    }

    private void setDirectionDropdownVisibility() {
        switch ((ObjectiveCriterion) cboObjectiveType.getSelectedItem()) {
            case PreventReachMapEdge:
            case ReachMapEdge:
                cboDirection.setVisible(true);
                break;
            default:
                cboDirection.setVisible(false);
                break;
        }
    }

    private void updateTimeLimitUI() {
        boolean enable = !cboTimeScaling.getSelectedItem().equals(TimeLimitType.None);
        spnTimeLimit.setEnabled(enable);
        cboTimeLimitDirection.setEnabled(enable);
    }

    public ScenarioObjective getObjective() {
        return objective;
    }

    private void saveObjectiveAndClose() {
        objective.setObjectiveCriterion((ObjectiveCriterion) cboObjectiveType.getSelectedItem());
        objective.setDescription(txtShortDescription.getText());
        int number = (int) spnAmount.getValue();
        if (cboCountType.getSelectedItem().equals(ObjectiveAmountType.Percentage)) {
            objective.setPercentage(number);
            objective.setFixedAmount(null);
        } else {
            objective.setFixedAmount(number);
        }

        objective.clearForces();
        for (int i = 0; i< forceModel.getSize(); i++) {
            objective.addForce(forceModel.getElementAt(i));
        }


        objective.clearSuccessEffects();
        for (int i = 0; i< successEffectsModel.getSize(); i++) {
            objective.addSuccessEffect(successEffectsModel.getElementAt(i));
        }

        objective.clearFailureEffects();
        for (int i = 0; i< failureEffectsModel.getSize(); i++) {
            objective.addFailureEffect(failureEffectsModel.getElementAt(i));
        }

        objective.clearDetails();
        for (int i = 0; i< detailModel.getSize(); i++) {
            objective.addDetail(detailModel.getElementAt(i));
        }

        if (cboDirection.isVisible() && cboDirection.getSelectedIndex() > 0) {
            objective.setDestinationEdge(OffBoardDirection.getDirection(cboDirection.getSelectedIndex() - 1));
        } else {
            objective.setDestinationEdge(OffBoardDirection.NONE);
        }

        int timeLimit = (int) spnTimeLimit.getValue();
        objective.setTimeLimitType((TimeLimitType) cboTimeScaling.getSelectedItem());
        if (spnTimeLimit.isEnabled()) {
            if (objective.getTimeLimitType() == TimeLimitType.ScaledToPrimaryUnitCount) {
                objective.setTimeLimitScaleFactor(timeLimit);
            } else {
                objective.setTimeLimit(timeLimit);
            }
        }
        if (cboTimeLimitDirection.isEnabled()) {
            objective.setTimeLimitAtMost(cboTimeLimitDirection.getSelectedIndex() == 0);
        }

        setVisible(false);
    }
}
