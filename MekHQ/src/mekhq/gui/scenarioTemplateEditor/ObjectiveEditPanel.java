/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.scenarioTemplateEditor;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.*;
import javax.swing.border.LineBorder;

import megamek.common.OffBoardDirection;
import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.mission.scenarios.ObjectiveEffect;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.EffectScalingType;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.ObjectiveEffectConditionType;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.scenarios.ScenarioObjective;
import mekhq.campaign.mission.scenarios.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.scenarios.ScenarioObjective.TimeLimitType;
import mekhq.campaign.mission.scenarios.ScenarioTemplate;

/**
 * UI for creating or editing a single scenario objective
 */
public class ObjectiveEditPanel extends JDialog {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioTemplateEditorDialog";
    private JTextArea txtShortDescription;
    private JComboBox<ObjectiveCriterion> cboObjectiveType;
    private JComboBox<String> cboDirection;
    private JTextField txtPercentage;
    private JComboBox<String> cboCountType;
    private JComboBox<String> cboForceName;

    private JLabel lblMagnitude;
    private JTextField txtAmount;
    private JComboBox<EffectScalingType> cboScalingType;
    private JComboBox<ObjectiveEffectType> cboEffectType;
    private JComboBox<ObjectiveEffectConditionType> cboEffectCondition;

    private JList<ObjectiveEffect> successEffects;
    private JList<ObjectiveEffect> failureEffects;
    private JButton btnRemoveSuccess;
    private JButton btnRemoveFailure;

    private JComboBox<String> cboTimeLimitDirection;
    private JComboBox<TimeLimitType> cboTimeScaling;
    private JTextField txtTimeLimit;

    private JList<String> forceNames;
    JButton btnRemove;

    private JList<String> lstDetails;

    private final ScenarioTemplate currentScenarioTemplate;
    private final ScenarioObjective objective;
    private final Runnable onSaved;

    public ObjectiveEditPanel(ScenarioTemplate template, Component owner, Runnable onSaved) {
        currentScenarioTemplate = template;
        objective = new ScenarioObjective();
        this.onSaved = onSaved;

        initGUI();
        updateTimeLimitUI();
        validate();
        pack();
        setLocationRelativeTo(owner);
    }

    public ObjectiveEditPanel(ScenarioTemplate template, ScenarioObjective objective, Component owner,
          Runnable onSaved) {
        currentScenarioTemplate = template;
        this.objective = objective;
        this.onSaved = onSaved;

        initGUI();
        updateForceList();

        txtShortDescription.setText(objective.getOverrideDescription());
        cboObjectiveType.setSelectedItem(objective.getObjectiveCriterion());
        cboCountType.setSelectedItem(objective.getAmountType());
        txtPercentage.setText(Integer.toString(objective.getAmount()));
        setDirectionDropdownVisibility();

        cboDirection.setSelectedIndex(objective.getDestinationEdge().ordinal());

        cboTimeScaling.setSelectedItem(objective.getTimeLimitType());
        updateTimeLimitUI();
        cboTimeLimitDirection.setSelectedIndex(objective.isTimeLimitAtMost() ? 0 : 1);
        if (objective.getTimeLimitType() == TimeLimitType.ScaledToPrimaryUnitCount) {
            txtTimeLimit.setText(objective.getTimeLimitScaleFactor().toString());
        } else {
            if (objective.getTimeLimit() != null) {
                txtTimeLimit.setText(objective.getTimeLimit().toString());
            }
        }

        updateEffectList(successEffects, objective.getSuccessEffects());
        updateEffectList(failureEffects, objective.getFailureEffects());
        updateDetailList();

        validate();
        pack();
        setLocationRelativeTo(owner);
    }

    private void initGUI() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        getContentPane().setLayout(new GridBagLayout());

        addDescriptionUI(gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        addObjectiveTypeUI(gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        addSubjectForce(gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        addTimeLimitUI(gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        addEffectUI(gbc);
        gbc.gridx = 0;
        gbc.gridy++;

        addObjectiveEffectUI(gbc);

        gbc.gridx = 0;
        gbc.gridy++;

        addSaveCloseButtons(gbc);
    }

    /**
     * Handles the save/close buttons row.
     */
    private void addSaveCloseButtons(GridBagConstraints gbc) {
        JPanel saveClosePanel = new JPanel();
        saveClosePanel.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);

        JButton btnCancel = new JButton(getTextAt(RESOURCE_BUNDLE, "button.cancel"));
        btnCancel.addActionListener(e -> this.setVisible(false));
        JButton btnSaveAndClose = new JButton(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.saveAndClose"));
        btnSaveAndClose.addActionListener(e -> this.saveObjectiveAndClose());

        saveClosePanel.add(btnCancel);
        saveClosePanel.add(btnSaveAndClose);

        getContentPane().add(saveClosePanel, gbc);
    }

    /**
     * Handles the "description" row.
     */
    private void addDescriptionUI(GridBagConstraints gbc) {
        JLabel lblShortDescription = new JLabel(getTextAt(RESOURCE_BUNDLE,
              "ObjectiveEditPanel.shortDescription.label"));

        JScrollPane txtScroll = new FastJScrollPane();
        txtShortDescription = new JTextArea();
        txtShortDescription.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
              "ObjectiveEditPanel.shortDescription.tooltip")));
        txtShortDescription.setColumns(40);
        txtShortDescription.setRows(5);
        txtShortDescription.setLineWrap(true);
        txtShortDescription.setWrapStyleWord(true);
        txtScroll.setViewportView(txtShortDescription);

        JTextField txtDetail = new JTextField();
        txtDetail.setColumns(40);
        JLabel lblDetail = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.details.label"));
        lstDetails = new JList<>();
        JButton btnAddDetail = new JButton(getTextAt(RESOURCE_BUNDLE, "button.add"));
        JButton btnRemoveDetail = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));

        lstDetails.addListSelectionListener(e -> btnRemoveDetail.setEnabled(!lstDetails.getSelectedValuesList()
                                                                                   .isEmpty()));
        lstDetails.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        btnRemoveDetail.addActionListener(e -> this.removeDetails());
        btnAddDetail.addActionListener(e -> this.addDetail(txtDetail));

        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setLayout(new GridBagLayout());
        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(5, 0, 5, 5);

        descriptionPanel.add(lblShortDescription, localGbc);
        localGbc.gridx++;
        descriptionPanel.add(txtScroll, localGbc);
        localGbc.gridx = 0;
        localGbc.gridy++;
        descriptionPanel.add(lblDetail, localGbc);
        localGbc.gridx++;
        descriptionPanel.add(txtDetail, localGbc);
        localGbc.gridx++;
        descriptionPanel.add(btnAddDetail, localGbc);
        localGbc.gridx++;
        descriptionPanel.add(lstDetails, localGbc);
        localGbc.gridx++;
        descriptionPanel.add(btnRemoveDetail, localGbc);

        getContentPane().add(descriptionPanel, gbc);
    }

    /**
     * Handles the "objective type" row
     */
    private void addObjectiveTypeUI(GridBagConstraints gbc) {
        JPanel objectivePanel = new JPanel();

        JLabel lblObjectiveType = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.objectiveType.label"));
        cboObjectiveType = new JComboBox<>();
        for (ObjectiveCriterion objectiveType : ObjectiveCriterion.values()) {
            cboObjectiveType.addItem(objectiveType);
        }
        cboObjectiveType.addActionListener(e -> this.setDirectionDropdownVisibility());

        txtPercentage = new JTextField();
        txtPercentage.setColumns(4);

        cboCountType = new JComboBox<>();
        cboCountType.addItem(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.amountType.percent"));
        cboCountType.addItem(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.amountType.fixed"));


        cboDirection = new JComboBox<>();
        cboDirection.addItem(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.forceDestinationEdge.label"));
        for (int x = 1; x < OffBoardDirection.values().length; x++) {
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


        JLabel lblSuccessEffects = new JLabel(getTextAt(RESOURCE_BUNDLE,
              "ObjectiveEditPanel.effectsOnCompletion.label"));
        JLabel lblFailureEffects = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.effectsOnFailure.label"));

        successEffects = new JList<>();
        successEffects.addListSelectionListener(e -> btnRemoveSuccess.setEnabled(!successEffects.getSelectedValuesList()
                                                                                        .isEmpty()));
        failureEffects = new JList<>();
        failureEffects.addListSelectionListener(e -> btnRemoveFailure.setEnabled(!failureEffects.getSelectedValuesList()
                                                                                        .isEmpty()));

        btnRemoveSuccess = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));
        btnRemoveSuccess.addActionListener(e -> this.removeEffect(ObjectiveEffectConditionType.ObjectiveSuccess));
        btnRemoveSuccess.setEnabled(false);

        btnRemoveFailure = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));
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

        JLabel forcesLabel = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.forceNames.label"));

        cboForceName = new JComboBox<>();
        for (ScenarioForceTemplate forceTemplate : currentScenarioTemplate.getAllScenarioForces()) {
            cboForceName.addItem(forceTemplate.getForceName());
        }

        forceNames = new JList<>();
        forceNames.setVisibleRowCount(5);
        forceNames.addListSelectionListener(e -> btnRemove.setEnabled(!forceNames.getSelectedValuesList().isEmpty()));

        JButton btnAdd = new JButton(getTextAt(RESOURCE_BUNDLE, "button.add"));
        btnAdd.addActionListener(e -> this.addForce());

        btnRemove = new JButton(getTextAt(RESOURCE_BUNDLE, "button.remove"));
        btnRemove.addActionListener(e -> this.removeForce());
        btnRemove.setEnabled(false);

        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);

        forcePanel.add(forcesLabel, localGbc);
        localGbc.gridx++;
        forcePanel.add(cboForceName, localGbc);
        localGbc.gridx++;
        forcePanel.add(btnAdd, localGbc);
        localGbc.gridx--;
        localGbc.gridy++;
        forcePanel.add(forceNames, localGbc);
        localGbc.gridx++;
        forcePanel.add(btnRemove, localGbc);


        getContentPane().add(forcePanel, gbc);
    }

    private void addTimeLimitUI(GridBagConstraints gbc) {
        JPanel timeLimitPanel = new JPanel();

        cboTimeLimitDirection = new JComboBox<>();
        cboTimeLimitDirection.addItem(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.timeLimit.atMost"));
        cboTimeLimitDirection.addItem(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.timeLimit.atLeast"));

        cboTimeScaling = new JComboBox<>();
        for (TimeLimitType timeLimitType : TimeLimitType.values()) {
            cboTimeScaling.addItem(timeLimitType);
        }
        cboTimeScaling.addActionListener(e -> this.updateTimeLimitUI());

        txtTimeLimit = new JTextField();
        txtTimeLimit.setColumns(5);

        GridBagConstraints localGbc = new GridBagConstraints();
        localGbc.gridx = 0;
        localGbc.gridy = 0;
        localGbc.insets = new Insets(0, 0, 0, 5);

        timeLimitPanel.add(cboTimeLimitDirection, localGbc);
        localGbc.gridx++;
        timeLimitPanel.add(cboTimeScaling, localGbc);
        localGbc.gridx++;
        timeLimitPanel.add(txtTimeLimit, localGbc);


        getContentPane().add(timeLimitPanel, gbc);
    }

    /**
     * Handles the "add objective effect" row
     */
    private void addEffectUI(GridBagConstraints gbc) {
        JPanel effectPanel = new JPanel();

        lblMagnitude = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.amount.label"));
        txtAmount = new JTextField();
        txtAmount.setColumns(5);

        JLabel lblScaling = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.effectScaling.label"));
        cboScalingType = new JComboBox<>();
        for (EffectScalingType scalingType : EffectScalingType.values()) {
            cboScalingType.addItem(scalingType);
        }

        JLabel lblEffectType = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.effectType.label"));
        cboEffectType = new JComboBox<>();
        for (ObjectiveEffectType scalingType : ObjectiveEffectType.values()) {
            cboEffectType.addItem(scalingType);
        }

        JLabel lblEffectCondition = new JLabel(getTextAt(RESOURCE_BUNDLE, "ObjectiveEditPanel.effectCondition.label"));
        cboEffectCondition = new JComboBox<>();
        cboEffectCondition.addItem(ObjectiveEffectConditionType.ObjectiveSuccess);
        cboEffectCondition.addItem(ObjectiveEffectConditionType.ObjectiveFailure);

        JButton btnAdd = new JButton(getTextAt(RESOURCE_BUNDLE, "button.add"));
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
        int amount;
        try {
            amount = Integer.parseInt(txtAmount.getText());
            lblMagnitude.setForeground(UIManager.getColor("text"));
        } catch (Exception e) {
            lblMagnitude.setForeground(Color.red);
            return;
        }

        ObjectiveEffect effect = new ObjectiveEffect();
        effect.howMuch = amount;
        effect.effectScaling = (EffectScalingType) cboScalingType.getSelectedItem();
        effect.effectType = (ObjectiveEffectType) cboEffectType.getSelectedItem();

        if (cboEffectCondition.getSelectedItem() == ObjectiveEffectConditionType.ObjectiveSuccess) {
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
        for (ObjectiveEffect currentEffect : objectiveEffects) {
            effectModel.addElement(currentEffect);
        }

        listToUpdate.setModel(effectModel);
    }

    private void removeEffect(ObjectiveEffectConditionType conditionType) {
        JList<ObjectiveEffect> listToUpdate;
        List<ObjectiveEffect> objectiveEffects;

        if (conditionType == ObjectiveEffectConditionType.ObjectiveSuccess) {
            listToUpdate = successEffects;
            objectiveEffects = objective.getSuccessEffects();
            btnRemoveSuccess.setEnabled(false);
        } else {
            listToUpdate = failureEffects;
            objectiveEffects = objective.getFailureEffects();
            btnRemoveFailure.setEnabled(false);
        }

        for (ObjectiveEffect effectToRemove : listToUpdate.getSelectedValuesList()) {
            objectiveEffects.remove(effectToRemove);
        }

        updateEffectList(listToUpdate, objectiveEffects);
    }

    private void addForce() {
        Object object = cboForceName.getSelectedItem();

        if (object instanceof String string) {
            objective.addForce(string);
        }

        updateForceList();
        pack();
    }

    private void removeForce() {
        for (String forceName : forceNames.getSelectedValuesList()) {
            objective.removeForce(forceName);
        }

        updateForceList();
        btnRemove.setEnabled(false);
        pack();
    }

    private void addDetail(JTextField field) {
        objective.addDetail(field.getText());
        updateDetailList();
    }

    private void removeDetails() {
        for (int index : lstDetails.getSelectedIndices()) {
            objective.getDetails().remove(index);
        }
        updateDetailList();
    }

    private void updateDetailList() {
        DefaultListModel<String> detailModel = new DefaultListModel<>();
        for (String detail : objective.getDetails()) {
            detailModel.addElement(detail);
        }

        lstDetails.setModel(detailModel);
    }

    private void updateForceList() {
        DefaultListModel<String> forceModel = new DefaultListModel<>();
        for (String forceName : objective.getAssociatedForceNames()) {
            forceModel.addElement(forceName);
        }

        forceNames.setModel(forceModel);
    }

    private void setDirectionDropdownVisibility() {
        Object object = cboObjectiveType.getSelectedItem();

        if (object instanceof ObjectiveCriterion criterion) {
            switch (criterion) {
                case PreventReachMapEdge:
                case ReachMapEdge:
                    cboDirection.setVisible(true);
                    break;
                default:
                    cboDirection.setVisible(false);
                    break;
            }
        }
    }

    private void updateTimeLimitUI() {
        Object object = cboTimeScaling.getSelectedItem();

        if (object instanceof TimeLimitType timeLimit) {
            boolean enable = !timeLimit.equals(TimeLimitType.None);

            txtTimeLimit.setEnabled(enable);
            cboTimeLimitDirection.setEnabled(enable);
        }
    }

    private void saveObjectiveAndClose() {
        int number;
        int timeLimit = 0;

        try {
            number = Integer.parseInt(txtPercentage.getText());
            txtPercentage.setBorder(null);
        } catch (Exception e) {
            txtPercentage.setBorder(new LineBorder(Color.red));
            return;
        }

        try {
            if (txtTimeLimit.isEnabled()) {
                timeLimit = Integer.parseInt(txtTimeLimit.getText());
                txtTimeLimit.setBorder(null);
            }
        } catch (Exception e) {
            txtTimeLimit.setBorder(new LineBorder(Color.red));
            return;
        }

        objective.setObjectiveCriterion((ObjectiveCriterion) cboObjectiveType.getSelectedItem());
        objective.setDescription(txtShortDescription.getText());
        if (this.cboCountType.getSelectedIndex() == 0) {
            objective.setPercentage(number);
        } else {
            objective.setFixedAmount(number);
        }

        if (cboDirection.isVisible() && cboDirection.getSelectedIndex() > 0) {
            objective.setDestinationEdge(OffBoardDirection.getDirection(cboDirection.getSelectedIndex() - 1));
        } else {
            objective.setDestinationEdge(OffBoardDirection.NONE);
        }

        objective.setTimeLimitType((TimeLimitType) cboTimeScaling.getSelectedItem());
        if (txtTimeLimit.isEnabled()) {
            if (objective.getTimeLimitType() == TimeLimitType.ScaledToPrimaryUnitCount) {
                objective.setTimeLimitScaleFactor(timeLimit);
            } else {
                objective.setTimeLimit(timeLimit);
            }
        }

        if (cboTimeLimitDirection.isEnabled()) {
            objective.setTimeLimitAtMost(cboTimeLimitDirection.getSelectedIndex() == 0);
        }

        if (!currentScenarioTemplate.scenarioObjectives.contains(objective)) {
            currentScenarioTemplate.scenarioObjectives.add(objective);
        }

        onSaved.run();
        setVisible(false);
    }
}
