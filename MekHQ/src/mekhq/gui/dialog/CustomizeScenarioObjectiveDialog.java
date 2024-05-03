package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.OffBoardDirection;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CustomizeScenarioObjectiveDialog extends JDialog {

    private ScenarioObjective objective;
    private List<BotForce> botForces;
    private JFrame frame;

    private JPanel panObjectiveType;
    private JPanel panForce;
    private JPanel panTimeLimits;
    private JPanel panEffect;
    private JPanel panObjectiveEffect;
    private JTextField txtShortDescription;
    private JComboBox<ScenarioObjective.ObjectiveCriterion> cboObjectiveType;
    private JComboBox<String> cboDirection;
    private JTextField txtPercentage;
    private MMComboBox<ScenarioObjective.ObjectiveAmountType> cboCountType;
    private JComboBox<String> cboForceName;

    private JLabel lblMagnitude;
    private JTextField txtAmount;
    private JComboBox<ObjectiveEffect.EffectScalingType> cboScalingType;
    private JComboBox<ObjectiveEffect.ObjectiveEffectType> cboEffectType;
    private JComboBox<ObjectiveEffect.ObjectiveEffectConditionType> cboEffectCondition;

    private JList<ObjectiveEffect> successEffects;
    private JList<ObjectiveEffect> failureEffects;
    private JButton btnRemoveSuccess;
    private JButton btnRemoveFailure;

    private JComboBox<String> cboTimeLimitDirection;
    private JComboBox<ScenarioObjective.TimeLimitType> cboTimeScaling;
    private JTextField txtTimeLimit;

    private JList<String> forceNames;
    JButton btnRemove;

    private JList<String> lstDetails;

    DefaultListModel<String> forceModel = new DefaultListModel<>();
    DefaultListModel<ObjectiveEffect> successEffectsModel = new DefaultListModel<>();
    DefaultListModel<ObjectiveEffect> failureEffectsModel = new DefaultListModel<>();

    DefaultListModel<String> detailModel = new DefaultListModel<>();


    public CustomizeScenarioObjectiveDialog(JFrame parent, boolean modal, ScenarioObjective objective, List<BotForce> botForces) {
        super(parent, modal);
        this.frame = parent;
        this.objective = objective;
        this.botForces = botForces;

        initialize();

        for(String forceName : objective.getAssociatedForceNames()) {
            forceModel.addElement(forceName);
        }

        txtShortDescription.setText(objective.getDescription());
        cboObjectiveType.setSelectedItem(objective.getObjectiveCriterion());
        cboCountType.setSelectedItem(objective.getAmountType());
        txtPercentage.setText(Integer.toString(objective.getAmount()));
        setDirectionDropdownVisibility();

        cboDirection.setSelectedIndex(objective.getDestinationEdge().ordinal());

        cboTimeScaling.setSelectedItem(objective.getTimeLimitType());
        updateTimeLimitUI();
        cboTimeLimitDirection.setSelectedIndex(objective.isTimeLimitAtMost() ? 0 : 1);
        if (objective.getTimeLimitType() == ScenarioObjective.TimeLimitType.ScaledToPrimaryUnitCount) {
            txtTimeLimit.setText(objective.getTimeLimitScaleFactor().toString());
        } else {
            if (objective.getTimeLimit() != null) {
                txtTimeLimit.setText(objective.getTimeLimit().toString());
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
        setTitle("Customize Scenario Objective");
        getContentPane().setLayout(new BorderLayout());
        JPanel panMain = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);
        panMain.add(new JLabel("Description:"), gbc);

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
        panMain.add(new JLabel("Details:"), gbc);

        JTextField txtDetail = new JTextField();
        txtDetail.setColumns(40);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        panMain.add(txtDetail, gbc);

        JButton btnAddDetail = new JButton("Add");
        btnAddDetail.addActionListener(e -> this.addDetail(txtDetail));
        gbc.gridx++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        panMain.add(btnAddDetail, gbc);


        lstDetails = new JList<>(detailModel);
        JButton btnRemoveDetail = new JButton("Remove");
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
        panMain.add(new JLabel("Objective Type:"), gbc);
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
        panMain.add(new JLabel("Force Names:"), gbc);

        initForcePanel();
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
        panMain.add(new JLabel("Time Limit:"), gbc);

        initTimeLimitPanel();
        gbc.gridx++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panMain.add(panTimeLimits, gbc);

        initObjectiveEffectPanel();
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        panMain.add(panObjectiveEffect, gbc);

        getContentPane().add(panMain, BorderLayout.CENTER);

        JPanel panButtons = new JPanel(new GridLayout(0, 2));
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> this.setVisible(false));
        JButton btnOK = new JButton("OK");
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
        for (ScenarioObjective.ObjectiveCriterion objectiveType : ScenarioObjective.ObjectiveCriterion.values()) {
            cboObjectiveType.addItem(objectiveType);
        }
        cboObjectiveType.addActionListener(e -> this.setDirectionDropdownVisibility());

        txtPercentage = new JTextField();
        txtPercentage.setColumns(4);

        cboCountType = new MMComboBox("cboCountType", ScenarioObjective.ObjectiveAmountType.values());


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
        panObjectiveType.add(txtPercentage, localGbc);
        localGbc.gridx++;
        localGbc.weightx = 1.0;
        panObjectiveType.add(cboCountType, localGbc);

    }

    /**
     * Handles the UI for adding/removing forces relevant to this objective
     */
    private void initForcePanel() {
        panForce = new JPanel(new GridBagLayout());

        cboForceName = new JComboBox<>();
        cboForceName.addItem(MHQConstants.EGO_OBJECTIVE_NAME);
        for(BotForce force : botForces) {
            cboForceName.addItem(force.getName());
        }

        forceNames = new JList<>(forceModel);
        forceNames.setVisibleRowCount(5);
        forceNames.addListSelectionListener(e -> btnRemove.setEnabled(!forceNames.getSelectedValuesList().isEmpty()));

        JButton btnAdd = new JButton("Add");
        btnAdd.addActionListener(e -> this.addForce());

        btnRemove = new JButton("Remove");
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
    private void initObjectiveEffectPanel() {
        panObjectiveEffect = new JPanel(new GridBagLayout());
        panObjectiveEffect.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Objective Effects"),
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

        lblMagnitude = new JLabel("Amount:");
        panObjectiveEffect.add(lblMagnitude, gbcLeft);
        txtAmount = new JTextField();
        txtAmount.setColumns(5);
        panObjectiveEffect.add(txtAmount, gbcRight);

        gbcLeft.gridy++;
        gbcRight.gridy++;
        panObjectiveEffect.add(new JLabel("Effect Scaling:"), gbcLeft);
        cboScalingType = new JComboBox<>();
        for (ObjectiveEffect.EffectScalingType scalingType : ObjectiveEffect.EffectScalingType.values()) {
            cboScalingType.addItem(scalingType);
        }
        panObjectiveEffect.add(cboScalingType, gbcRight);

        gbcLeft.gridy++;
        gbcRight.gridy++;
        panObjectiveEffect.add(new JLabel("Effect Type:"), gbcLeft);
        cboEffectType = new JComboBox<>();
        for (ObjectiveEffect.ObjectiveEffectType scalingType : ObjectiveEffect.ObjectiveEffectType.values()) {
            cboEffectType.addItem(scalingType);
        }
        panObjectiveEffect.add(cboEffectType, gbcRight);

        gbcLeft.gridy++;
        gbcRight.gridy++;
        panObjectiveEffect.add(new JLabel("Effect Condition:"), gbcLeft);
        cboEffectCondition = new JComboBox<>();
        cboEffectCondition.addItem(ObjectiveEffect.ObjectiveEffectConditionType.ObjectiveSuccess);
        cboEffectCondition.addItem(ObjectiveEffect.ObjectiveEffectConditionType.ObjectiveFailure);
        panObjectiveEffect.add(cboEffectCondition, gbcRight);

        JButton btnAdd = new JButton("Add");
        btnAdd.addActionListener(e -> this.addEffect());
        gbcLeft.gridy++;
        panObjectiveEffect.add(btnAdd, gbcLeft);

        JLabel lblSuccessEffects = new JLabel("Effects on Success");
        JLabel lblFailureEffects = new JLabel("Effects on Failure");

        successEffects = new JList<>(successEffectsModel);
        successEffects.addListSelectionListener(e -> btnRemoveSuccess.setEnabled(!successEffects.getSelectedValuesList().isEmpty()));
        failureEffects = new JList<>(failureEffectsModel);
        failureEffects.addListSelectionListener(e -> btnRemoveFailure.setEnabled(!failureEffects.getSelectedValuesList().isEmpty()));

        btnRemoveSuccess = new JButton("Remove");
        btnRemoveSuccess.addActionListener(e -> this.removeEffect(ObjectiveEffect.ObjectiveEffectConditionType.ObjectiveSuccess));
        btnRemoveSuccess.setEnabled(false);

        btnRemoveFailure = new JButton("Remove");
        btnRemoveFailure.addActionListener(e -> this.removeEffect(ObjectiveEffect.ObjectiveEffectConditionType.ObjectiveFailure));
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
        for (ScenarioObjective.TimeLimitType timeLimitType : ScenarioObjective.TimeLimitType.values()) {
            cboTimeScaling.addItem(timeLimitType);
        }
        cboTimeScaling.addActionListener(e -> this.updateTimeLimitUI());

        txtTimeLimit = new JTextField();
        txtTimeLimit.setColumns(5);

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
        panTimeLimits.add(txtTimeLimit, localGbc);
    }

    /**
     * Handles the "add objective effect" row
     */
    private void initEffectPanel() {


    }

    /**
     * Event handler for the 'add' button for scenario effects
     */
    private void addEffect() {
        int amount = 0;
        try {
            amount = Integer.parseInt(txtAmount.getText());
            lblMagnitude.setForeground(UIManager.getColor("text"));
        } catch (Exception e) {
            lblMagnitude.setForeground(Color.red);
            return;
        }

        ObjectiveEffect effect = new ObjectiveEffect();
        effect.howMuch = amount;
        effect.effectScaling = (ObjectiveEffect.EffectScalingType) cboScalingType.getSelectedItem();
        effect.effectType = (ObjectiveEffect.ObjectiveEffectType) cboEffectType.getSelectedItem();

        if (cboEffectCondition.getSelectedItem() == ObjectiveEffect.ObjectiveEffectConditionType.ObjectiveSuccess) {
            successEffectsModel.addElement(effect);
            successEffects.repaint();
        } else {
            failureEffectsModel.addElement(effect);
        }

        pack();
    }

    private void removeEffect(ObjectiveEffect.ObjectiveEffectConditionType conditionType) {
        JList<ObjectiveEffect> targetList;
        DefaultListModel<ObjectiveEffect> modelToUpdate;

        if (conditionType == ObjectiveEffect.ObjectiveEffectConditionType.ObjectiveSuccess) {
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
        switch ((ScenarioObjective.ObjectiveCriterion) cboObjectiveType.getSelectedItem()) {
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
        boolean enable = !cboTimeScaling.getSelectedItem().equals(ScenarioObjective.TimeLimitType.None);

        txtTimeLimit.setEnabled(enable);
        cboTimeLimitDirection.setEnabled(enable);
    }

    public ScenarioObjective getObjective() {
        return objective;
    }

    private void saveObjectiveAndClose() {
        int number = 0;
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

        objective.setObjectiveCriterion((ScenarioObjective.ObjectiveCriterion) cboObjectiveType.getSelectedItem());
        objective.setDescription(txtShortDescription.getText());
        if (cboCountType.getSelectedItem().equals(ScenarioObjective.ObjectiveAmountType.Percentage)) {
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

        objective.setTimeLimitType((ScenarioObjective.TimeLimitType) cboTimeScaling.getSelectedItem());
        if (txtTimeLimit.isEnabled()) {
            if (objective.getTimeLimitType() == ScenarioObjective.TimeLimitType.ScaledToPrimaryUnitCount) {
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
