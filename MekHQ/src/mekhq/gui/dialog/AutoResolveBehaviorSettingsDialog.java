package mekhq.gui.dialog;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.bot.princess.PrincessException;
import megamek.client.ui.Messages;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.dialogs.helpDialogs.PrincessHelpDialog;
import megamek.client.ui.swing.MMToggleButton;
import megamek.client.ui.swing.util.ScalingPopup;
import megamek.client.ui.swing.util.UIUtil;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQDialog;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AutoResolveBehaviorSettingsDialog
    extends AbstractMHQDialog
    implements ActionListener, ListSelectionListener, ChangeListener
{
    private final static MMLogger logger = MMLogger.create(AutoResolveBehaviorSettingsDialog.class);

    private static final String OK_ACTION = "Ok_Action";

    private final transient BehaviorSettingsFactory behaviorSettingsFactory = BehaviorSettingsFactory.getInstance();
    private BehaviorSettings autoResolveBehavior;

    private final JLabel nameLabel = new JLabel(Messages.getString("BotConfigDialog.nameLabel"));
    private final UIUtil.TipTextField nameField = new UIUtil.TipTextField("", 16);

    private final MMToggleButton forcedWithdrawalCheck = new UIUtil.TipMMToggleButton(
        Messages.getString("BotConfigDialog.forcedWithdrawalCheck"));
    private final JLabel withdrawEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.retreatEdgeLabel"));
    private final MMComboBox<CardinalEdge> withdrawEdgeCombo = new UIUtil.TipCombo<>("EdgeToWithdraw", CardinalEdge.values());
    private final MMToggleButton autoFleeCheck = new UIUtil.TipMMToggleButton(Messages.getString("BotConfigDialog.autoFleeCheck"));
    private final JLabel fleeEdgeLabel = new JLabel(Messages.getString("BotConfigDialog.homeEdgeLabel"));
    private final MMComboBox<CardinalEdge> fleeEdgeCombo = new UIUtil.TipCombo<>("EdgeToFlee", CardinalEdge.values());

    private final UIUtil.TipSlider aggressionSlidebar = new UIUtil.TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private final UIUtil.TipSlider fallShameSlidebar = new UIUtil.TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private final UIUtil.TipSlider herdingSlidebar = new UIUtil.TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private final UIUtil.TipSlider selfPreservationSlidebar = new UIUtil.TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
    private final UIUtil.TipSlider braverySlidebar = new UIUtil.TipSlider(SwingConstants.HORIZONTAL, 0, 10, 5);
//    private final UIUtil.TipButton savePreset = new UIUtil.TipButton(Messages.getString("BotConfigDialog.save"));
    private final UIUtil.TipButton saveNewPreset = new UIUtil.TipButton(Messages.getString("BotConfigDialog.saveNew"));

    private final JButton princessHelpButton = new JButton(Messages.getString("BotConfigDialog.help"));

    private JPanel presetsPanel;
    private final JLabel chooseLabel = new JLabel(Messages.getString("BotConfigDialog.behaviorNameLabel"));
    /**
     * A copy of the current presets. Modifications will only be saved when
     * accepted.
     */
    private List<String> presets;
    private final AutoResolveBehaviorSettingsDialog.PresetsModel presetsModel = new AutoResolveBehaviorSettingsDialog.PresetsModel();
    private final JList<String> presetsList = new JList<>(presetsModel);

    private final JButton butOK = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));

    /**
     * Stores the currently chosen preset. Used to detect if the player has changed
     * the sliders.
     */
    private BehaviorSettings chosenPreset;
    private Campaign campaign;

    //region Constructors
    public AutoResolveBehaviorSettingsDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "AutoResolveBehaviorSettingsDialog", "AutoResolveBehaviorSettingsDialog.title");
        setAlwaysOnTop(true);
        setCampaign(campaign);
        autoResolveBehavior = (
            campaign.getAutoResolveBehaviorSettings() != null ?
                campaign.getAutoResolveBehaviorSettings() : new BehaviorSettings());
        updatePresets();
        initialize();
        updateDialogFields();
    }

    private String getAutoResolveBehaviorSettingName() {
        return campaign.getName() + ":AI";
    }

    public void setCampaign(final Campaign campaign) {
        this.campaign = campaign;
    }

    @Override
    protected void initialize() {
        // Make Enter confirm and close the dialog
        final KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enter, OK_ACTION);
        getRootPane().getInputMap(JComponent.WHEN_FOCUSED).put(enter, OK_ACTION);
        getRootPane().getActionMap().put(OK_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                okAction();
            }
        });
        super.initialize();
    }

    @Override
    protected Container createCenterPane() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.add(nameSection());
        result.add(settingSection());
        return result;
    }

    /**
     * The setting section contains the presets list on the left side and the
     * princess settings on the right.
     */
    private JPanel settingSection() {
//        var princessScroll = new JScrollPane(princessPanel());
//        princessScroll.getVerticalScrollBar().setUnitIncrement(16);
//        princessScroll.setBorder(null);
//        presetsPanel = presetsPanel();

        var result = new JPanel(new BorderLayout(0, 0));
        result.setAlignmentX(LEFT_ALIGNMENT);
        result.add(princessPanel(), BorderLayout.CENTER);
//        result.add(presetsPanel, BorderLayout.LINE_START);
        return result;
    }

    /** The princess panel contains the individual princess settings. */
    private JPanel princessPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.add(behaviorSection());
//        result.add(retreatSection());
        result.add(createButtonPanel());
        return result;
    }

    private JPanel nameSection() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        UIUtil.Content panContent = new UIUtil.Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        var namePanel = new JPanel();
        nameField.setToolTipText(Messages.getString("BotConfigDialog.namefield.tooltip"));
        // When the dialog configures an existing player, the name must not be changed
        nameField.setText(getAutoResolveBehaviorSettingName());
        nameField.setEnabled(false);
        nameLabel.setLabelFor(nameField);
        nameLabel.setDisplayedMnemonic(KeyEvent.VK_N);
        namePanel.add(nameLabel);
        namePanel.add(nameField);

        panContent.add(namePanel);
        return result;
    }

    /** The presets panel has a list of behavior presets for Princess. */
    private JPanel presetsPanel() {
        var result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setBorder(new EmptyBorder(0, 10, 0, 20));

        chooseLabel.setAlignmentX(CENTER_ALIGNMENT);
        chooseLabel.setDisplayedMnemonic(KeyEvent.VK_P);
        chooseLabel.setLabelFor(presetsList);
        var headerPanel = new UIUtil.FixedYPanel();
        headerPanel.add(chooseLabel);

        presetsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        presetsList.addListSelectionListener(this);
        presetsList.setCellRenderer(new PresetsRenderer());
        presetsList.addMouseListener(presetsMouseListener);

        result.add(headerPanel);
        result.add(Box.createVerticalStrut(10));
        result.add(presetsList);

        return result;
    }

    private JPanel behaviorSection() {
        JPanel result = new UIUtil.OptionPanel("BotConfigDialog.behaviorSection");
        UIUtil.Content panContent = new UIUtil.Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        panContent.add(buildSlider(braverySlidebar, Messages.getString("BotConfigDialog.braverySliderMin"),
            Messages.getString("BotConfigDialog.braverySliderMax"),
            Messages.getString("BotConfigDialog.braveryTooltip"),
            Messages.getString("BotConfigDialog.braverySliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(
            buildSlider(selfPreservationSlidebar, Messages.getString("BotConfigDialog.selfPreservationSliderMin"),
                Messages.getString("BotConfigDialog.selfPreservationSliderMax"),
                Messages.getString("BotConfigDialog.selfPreservationTooltip"),
                Messages.getString("BotConfigDialog.selfPreservationSliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(buildSlider(aggressionSlidebar, Messages.getString("BotConfigDialog.aggressionSliderMin"),
            Messages.getString("BotConfigDialog.aggressionSliderMax"),
            Messages.getString("BotConfigDialog.aggressionTooltip"),
            Messages.getString("BotConfigDialog.aggressionSliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(buildSlider(herdingSlidebar, Messages.getString("BotConfigDialog.herdingSliderMin"),
            Messages.getString("BotConfigDialog.herdingSliderMax"),
            Messages.getString("BotConfigDialog.herdingToolTip"),
            Messages.getString("BotConfigDialog.herdingSliderTitle")));
        panContent.add(Box.createVerticalStrut(7));

        panContent.add(buildSlider(fallShameSlidebar, Messages.getString("BotConfigDialog.fallShameSliderMin"),
            Messages.getString("BotConfigDialog.fallShameSliderMax"),
            Messages.getString("BotConfigDialog.fallShameToolTip"),
            Messages.getString("BotConfigDialog.fallShameSliderTitle")));

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setAlignmentX(SwingConstants.CENTER);
        result.add(buttonPanel);

//        savePreset.addActionListener(this);
//        savePreset.setMnemonic(KeyEvent.VK_S);
//        savePreset.setToolTipText(Messages.getString("BotConfigDialog.saveTip"));
//        buttonPanel.add(savePreset);
//        saveNewPreset.addActionListener(this);
//        saveNewPreset.setMnemonic(KeyEvent.VK_A);
//        saveNewPreset.setToolTipText(Messages.getString("BotConfigDialog.saveNewTip"));
//        buttonPanel.add(saveNewPreset);

        return result;
    }


    private JPanel retreatSection() {
        JPanel result = new UIUtil.OptionPanel("BotConfigDialog.retreatSection");
        UIUtil.Content panContent = new UIUtil.Content();
        panContent.setLayout(new BoxLayout(panContent, BoxLayout.PAGE_AXIS));
        result.add(panContent);

        autoFleeCheck.setToolTipText(Messages.getString("BotConfigDialog.autoFleeTooltip"));
        autoFleeCheck.addActionListener(this);
        autoFleeCheck.setMnemonic(KeyEvent.VK_F);

        fleeEdgeCombo.removeItem(CardinalEdge.NONE);
        fleeEdgeCombo.setToolTipText(Messages.getString("BotConfigDialog.homeEdgeTooltip"));
        fleeEdgeCombo.setSelectedIndex(0);
        fleeEdgeCombo.addActionListener(this);

        forcedWithdrawalCheck.setToolTipText(Messages.getString("BotConfigDialog.forcedWithdrawalTooltip"));
        forcedWithdrawalCheck.addActionListener(this);
        forcedWithdrawalCheck.setMnemonic(KeyEvent.VK_W);

        withdrawEdgeCombo.removeItem(CardinalEdge.NONE);
        withdrawEdgeCombo.setToolTipText(Messages.getString("BotConfigDialog.retreatEdgeTooltip"));
        withdrawEdgeCombo.setSelectedIndex(0);

        var firstLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        var secondLine = new JPanel(new FlowLayout(FlowLayout.LEFT));
        firstLine.add(forcedWithdrawalCheck);
        firstLine.add(Box.createHorizontalStrut(20));
        firstLine.add(withdrawEdgeLabel);
        firstLine.add(withdrawEdgeCombo);
        secondLine.add(autoFleeCheck);
        secondLine.add(Box.createHorizontalStrut(20));
        secondLine.add(fleeEdgeLabel);
        secondLine.add(fleeEdgeCombo);
        panContent.add(firstLine);
        panContent.add(Box.createVerticalStrut(5));
        panContent.add(secondLine);

        return result;
    }

    protected void updatePresetFields() {
        selfPreservationSlidebar.setValue(autoResolveBehavior.getSelfPreservationIndex());
        aggressionSlidebar.setValue(autoResolveBehavior.getHyperAggressionIndex());
        fallShameSlidebar.setValue(autoResolveBehavior.getFallShameIndex());
        herdingSlidebar.setValue(autoResolveBehavior.getHerdMentalityIndex());
        braverySlidebar.setValue(autoResolveBehavior.getBraveryIndex());
    }

    private void updateDialogFields() {
        updatePresetFields();

        forcedWithdrawalCheck.setSelected(autoResolveBehavior.isForcedWithdrawal());
        withdrawEdgeCombo.setSelectedItem(autoResolveBehavior.getRetreatEdge());

        autoFleeCheck.setSelected(autoResolveBehavior.shouldAutoFlee());
        fleeEdgeCombo.setSelectedItem(autoResolveBehavior.getDestinationEdge());

        updateEnabledStates();
    }

    /** Updates all necessary enabled states of buttons/dropdowns. */
    private void updateEnabledStates() {
        fleeEdgeLabel.setEnabled(autoFleeCheck.isSelected());
        fleeEdgeCombo.setEnabled(autoFleeCheck.isSelected());
        withdrawEdgeLabel.setEnabled(forcedWithdrawalCheck.isSelected());
        withdrawEdgeCombo.setEnabled(forcedWithdrawalCheck.isSelected());
//        savePreset.setEnabled(isChangedPreset());
    }

    /**
     * Returns true if a preset is selected and is different from the current slider
     * settings.
     */
    private boolean isChangedPreset() {
        return (chosenPreset != null)
            && (chosenPreset.getSelfPreservationIndex() != selfPreservationSlidebar.getValue()
            || chosenPreset.getHyperAggressionIndex() != aggressionSlidebar.getValue()
            || chosenPreset.getFallShameIndex() != fallShameSlidebar.getValue()
            || chosenPreset.getHerdMentalityIndex() != herdingSlidebar.getValue()
            || chosenPreset.getBraveryIndex() != braverySlidebar.getValue());
    }

    private JPanel buildSlider(JSlider thisSlider, String minMsgProperty,
                               String maxMsgProperty, String toolTip, String title) {
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitlePosition(TitledBorder.TOP);
        border.setTitleJustification(TitledBorder.CENTER);
        var result = new UIUtil.TipPanel();
        result.setBorder(border);
        result.setLayout(new BoxLayout(result, BoxLayout.PAGE_AXIS));
        result.setToolTipText(toolTip);
        thisSlider.setToolTipText(toolTip);
        thisSlider.setPaintLabels(false);
        thisSlider.setSnapToTicks(true);
        thisSlider.addChangeListener(this);

        var panLabels = new JPanel();
        panLabels.setLayout(new BoxLayout(panLabels, BoxLayout.LINE_AXIS));
        panLabels.add(new JLabel(minMsgProperty, SwingConstants.LEFT));
        panLabels.add(Box.createHorizontalGlue());
        panLabels.add(new JLabel(maxMsgProperty, SwingConstants.RIGHT));

        result.add(panLabels);
        result.add(thisSlider);
        result.revalidate();
        return result;
    }

    protected JPanel createButtonPanel() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        butOK.addActionListener((l) -> {
            okAction();
            setVisible(false);
        });
        butOK.setMnemonic(KeyEvent.VK_K);
        result.add(butOK);

        butCancel.addActionListener(this::cancelActionPerformed);
        butCancel.setMnemonic(KeyEvent.VK_C);
        result.add(butCancel);

        princessHelpButton.addActionListener(this);
        princessHelpButton.setMnemonic(KeyEvent.VK_H);
        result.add(princessHelpButton);

        return result;
    }

    private void showPrincessHelp() {
        new PrincessHelpDialog(getFrame()).setVisible(true);
    }

    private void okAction() {
        try {
            savePrincessProperties();
        } catch (PrincessException e) {
            logger.error("Error saving AutoResolveBehaviorSettings properties", e);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == princessHelpButton) {
            showPrincessHelp();
        }
    }

    /** Saves the current Behavior to the currently selected Behavior Preset. */
    private void savePreset() {
        writePreset(getAutoResolveBehaviorSettingName());
    }

    /** Removes the given Behavior Preset. */
    private void removePreset(String name) {
        behaviorSettingsFactory.removeBehavior(name);
        behaviorSettingsFactory.saveBehaviorSettings(false);
        updatePresets();
    }

    private void savePrincessProperties() throws PrincessException {
        BehaviorSettings tempBehavior = new BehaviorSettings();
        tempBehavior.setFallShameIndex(fallShameSlidebar.getValue());
        tempBehavior.setForcedWithdrawal(forcedWithdrawalCheck.isSelected());
        tempBehavior.setAutoFlee(autoFleeCheck.isSelected());
        tempBehavior.setDestinationEdge(fleeEdgeCombo.getSelectedItem());
        tempBehavior.setRetreatEdge(withdrawEdgeCombo.getSelectedItem());
        tempBehavior.setHyperAggressionIndex(aggressionSlidebar.getValue());
        tempBehavior.setSelfPreservationIndex(selfPreservationSlidebar.getValue());
        tempBehavior.setHerdMentalityIndex(herdingSlidebar.getValue());
        tempBehavior.setBraveryIndex(braverySlidebar.getValue());
        tempBehavior.setDescription(getAutoResolveBehaviorSettingName());
        autoResolveBehavior = tempBehavior;
        campaign.setAutoResolveBehaviorSettings(tempBehavior);
        savePreset();
    }

    private void writePreset(String name) {
        BehaviorSettings newBehavior = new BehaviorSettings();
        try {
            newBehavior.setDescription(name);
        } catch (PrincessException e1) {
            return;
        }
        newBehavior.setFallShameIndex(fallShameSlidebar.getValue());
        newBehavior.setHyperAggressionIndex(aggressionSlidebar.getValue());
        newBehavior.setSelfPreservationIndex(selfPreservationSlidebar.getValue());
        newBehavior.setHerdMentalityIndex(herdingSlidebar.getValue());
        newBehavior.setBraveryIndex(braverySlidebar.getValue());
        behaviorSettingsFactory.addBehavior(newBehavior);
        behaviorSettingsFactory.saveBehaviorSettings(false);
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        if (event.getSource() == presetsList) {
            presetSelected();
        }
    }

    /** Shows a popup menu for a behavior preset, allowing to delete it. */
    private transient MouseListener presetsMouseListener = new MouseAdapter() {

        @Override
        public void mouseReleased(MouseEvent e) {
            int row = presetsList.locationToIndex(e.getPoint());
            if (e.isPopupTrigger() && (row != -1)) {
                ScalingPopup popup = new ScalingPopup();
                String behavior = presetsList.getModel().getElementAt(row);
                var deleteItem = new JMenuItem("Delete " + behavior);
                deleteItem.addActionListener(event -> removePreset(behavior));
                popup.add(deleteItem);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        @Override
        public void mouseClicked(MouseEvent evt) {
            if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 1) {
                presetSelected();
            }
        }
    };

    /**
     * Called when a Preset is selected. This will often be called twice when
     * clicking with the mouse (by the listselectionlistener and the mouselistener).
     * In this way the list will react when copying a Preset from another bot and
     * then clicking the already selected Preset again. And it will also react to
     * keyboard navigation.
     */
    private void presetSelected() {
        if (presetsList.isSelectionEmpty()) {
            chosenPreset = null;
        } else {
            autoResolveBehavior = behaviorSettingsFactory.getBehavior(presetsList.getSelectedValue());
            chosenPreset = behaviorSettingsFactory.getBehavior(presetsList.getSelectedValue());

            if (autoResolveBehavior == null) {
                autoResolveBehavior = new BehaviorSettings();
            }
            updatePresetFields();
        }
        updateEnabledStates();
    }

    /**
     * Sets up/Updates the displayed preset list (e.g. after adding or deleting a
     * preset)
     */
    private void updatePresets() {
        presets = new ArrayList<>(Arrays.asList(behaviorSettingsFactory.getBehaviorNames()));
        ((AutoResolveBehaviorSettingsDialog.PresetsModel) presetsList.getModel()).fireUpdate();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        updateEnabledStates();
    }

    private class PresetsModel extends DefaultListModel<String> {

        @Override
        public int getSize() {
            return presets.size();
        }

        @Override
        public String getElementAt(int index) {
            return presets.get(index);
        }

        /** Call when elements of the list change. */
        private void fireUpdate() {
            fireContentsChanged(this, 0, getSize() - 1);
        }
    }

    /**
     * A renderer for the Behavior Presets list. Adapts the font size to the gui
     * scaling and
     * colors the special list elements (other bot Configurations and original
     * Config).
     */
    private class PresetsRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            comp.setFont(UIUtil.getScaledFont());
            String preset = (String) value;
            if (preset.startsWith(UIUtil.BOT_MARKER)) {
                comp.setForeground(UIUtil.uiLightBlue());
            }

            if (preset.equals(Messages.getString("BotConfigDialog.previousConfig"))) {
                comp.setForeground(UIUtil.uiGreen());
            }

            return comp;
        }
    }
}
