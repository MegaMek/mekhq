/*
 * ResolveScenarioWizardDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.EntityReadoutDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.swing.UnitEditorDialog;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.GunEmplacement;
import megamek.common.options.OptionsConstants;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.OppositionPersonnelStatus;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.ResolveScenarioTracker.UnitStatus;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjectiveProcessor;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.DefaultMHQScrollablePanel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.MarkdownEditorPanel;
import mekhq.gui.view.PersonViewPanel;
import mekhq.utilities.ReportingUtilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

import static mekhq.campaign.personnel.randomEvents.PersonalityController.writeDescription;

/**
 * @author Taharqa
 */
public class ResolveScenarioWizardDialog extends JDialog {
    //region Variable Declarations
    final static String UNITSPANEL = "Your Units";
    final static String PILOTPANEL = "Your Personnel";
    final static String SALVAGEPANEL = "Salvage";
    final static String PRISONERPANEL = "Captured Personnel";
    final static String KILLSPANEL = "Assign Kills";
    final static String REWARDPANEL = "Costs & Payouts";
    final static String OBJECTIVEPANEL = "Objective Status";
    final static String PREVIEWPANEL = "Preview";

    final static String[] panelOrder = {
            UNITSPANEL, PILOTPANEL, SALVAGEPANEL, PRISONERPANEL, KILLSPANEL, REWARDPANEL,
            OBJECTIVEPANEL, PREVIEWPANEL
    };

    private Campaign campaign;
    private final JFrame frame;

    private final ResolveScenarioTracker tracker;
    private final ScenarioObjectiveProcessor objectiveProcessor;

    private JButton btnNext;
    private JButton btnFinish;
    private JButton btnBack;

    private JTabbedPane tabMain;

    private JPanel pnlUnitStatus;
    private JPanel pnlObjectiveStatus;
    private JPanel pnlPilotStatus;
    private JPanel pnlSalvage;
    private JPanel pnlPrisonerStatus;
    private JPanel pnlKills;
    private JPanel pnlRewards;
    private JPanel pnlPreview;

    /*
     * Unit status panel components
     */
    private List<JCheckBox> chksTotaled;
    private List<JButton> btnsEditUnit;
    private List<UnitStatus> ustatuses;
    private List<JLabel> lblsUnitName;

    // maps objectives to list of associated entity checkboxes
    private Map<ScenarioObjective, List<JCheckBox>> objectiveCheckboxes;
    private Map<ScenarioObjective, JCheckBox> objectiveOverrideCheckboxes;

    /*
     * Pilot status panel components
     */
    private final List<JCheckBox> miaBtns = new ArrayList<>();
    private final List<JCheckBox> kiaBtns = new ArrayList<>();
    private final List<JSlider> hitSliders = new ArrayList<>();
    private final List<PersonStatus> pstatuses = new ArrayList<>();

    /*
     * Prisoner status panel components
     */
    private JButton prisonerRansomAllButton;
    private final List<JCheckBox> prisonerCapturedBtns = new ArrayList<>();
    private final List<JCheckBox> prisonerRansomedBtns = new ArrayList<>();
    private final List<JCheckBox> prisonerKiaBtns = new ArrayList<>();
    private final List<JSlider> pr_hitSliders = new ArrayList<>();
    private final List<OppositionPersonnelStatus> prstatuses = new ArrayList<>();

    //region Salvage Panel Components
    private List<JLabel> salvageUnitLabel;
    private List<JCheckBox> salvageBoxes;
    private List<JCheckBox> soldUnitBoxes;
    private List<JCheckBox> escapeBoxes;
    private List<JButton> btnsSalvageEditUnit;
    private final List<Unit> salvageables;

    private JLabel lblSalvageValueUnit2;
    private JLabel lblSalvageValueEmployer2;
    private JLabel lblSalvagePct2;

    private Money salvageEmployer = Money.zero();
    private Money salvageUnit = Money.zero();
    private int currentSalvagePct;
    private int maxSalvagePct;
    //endregion Salvage Panel Components

    /*
     * Assign Kills components
     */
    private Hashtable<String, JComboBox<String>> killChoices;

    /*
     * Collect Rewards components
     */
    private List<JCheckBox> lootBoxes;
    private final List<Loot> loots;

    //region Preview Panel components
    private JScrollPane scrPreviewPanel;
    private JComboBox<ScenarioStatus> choiceStatus;
    private MarkdownEditorPanel txtReport;
    private JTextArea txtRecoveredUnits;
    private JTextArea txtRecoveredPilots;
    private JTextArea txtMissingUnits;
    private JTextArea txtMissingPilots;
    private JTextArea txtDeadPilots;
    private JTextArea txtSalvage;
    private JTextArea txtRewards;
    //endregion Preview Panel components

    private static final MMLogger logger = MMLogger.create(ResolveScenarioWizardDialog.class);

    private final transient ResourceBundle resourceMap =
            ResourceBundle.getBundle("mekhq.resources.ResolveScenarioWizardDialog", MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    public ResolveScenarioWizardDialog(Campaign campaign, JFrame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
        this.campaign = campaign;
        this.frame = parent;
        this.tracker = t;
        objectiveProcessor = new ScenarioObjectiveProcessor();
        loots = tracker.getPotentialLoot();
        salvageables = new ArrayList<>();
        if (tracker.getMission() instanceof Contract) {
            salvageEmployer = ((Contract) tracker.getMission()).getSalvagedByEmployer();
            salvageUnit = ((Contract) tracker.getMission()).getSalvagedByUnit();
            maxSalvagePct = ((Contract) tracker.getMission()).getSalvagePct();

            currentSalvagePct = 0;
            if (salvageUnit.plus(salvageEmployer).isPositive()) {
                currentSalvagePct = salvageUnit.multipliedBy(100)
                        .dividedBy(salvageUnit.plus(salvageEmployer)).getAmount().intValue();
            }
        }
        initComponents();
        setLocationRelativeTo(parent);
        setUserPreferences();
        pack();
    }

    /**
     * This initializes the dialog's components
     * It currently uses the following Mnemonics:
     * B, C, F, N, ESCAPE
     */
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Adding Escape Mnemonic
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setName("Form");

        getContentPane().setLayout(new GridBagLayout());

        setTitle(resourceMap.getString("title"));

        tabMain = new JTabbedPane();

        //region Make Tab Panels
        pnlUnitStatus = makeUnitStatusPanel();
        tabMain.add(wrapWithInstructions(pnlUnitStatus, null,
                resourceMap.getString("txtInstructions.text.missingunits")), UNITSPANEL);


        pnlPilotStatus = makePilotStatusPanel();
        tabMain.add(wrapWithInstructions(pnlPilotStatus, null,
                resourceMap.getString("txtInstructions.text.personnel")), PILOTPANEL);


        pnlSalvage = makeSalvagePanel();
        tabMain.add(wrapWithInstructions(pnlSalvage, null,
                resourceMap.getString("txtInstructions.text.salvage")), SALVAGEPANEL);

        pnlPrisonerStatus = makePrisonerStatusPanel();
        tabMain.add(wrapWithInstructions(pnlPrisonerStatus, null,
                resourceMap.getString("txtInstructions.text.prisoners")), PRISONERPANEL);

        pnlKills = makeKillsPanel();
        tabMain.add(wrapWithInstructions(pnlKills, null,
                resourceMap.getString("txtInstructions.text.kills")), KILLSPANEL);

        pnlRewards = makeRewardsPanel();
        tabMain.add(wrapWithInstructions(pnlRewards, null,
                resourceMap.getString("txtInstructions.text.reward")), REWARDPANEL);

        pnlObjectiveStatus = makeObjectiveStatusPanel();
        tabMain.add(wrapWithInstructions(pnlObjectiveStatus, null,
                resourceMap.getString("txtInstructions.text.objectives")), OBJECTIVEPANEL);

        pnlPreview = makePreviewPanel();
        scrPreviewPanel = new JScrollPaneWithSpeed();
        tabMain.add(wrapWithInstructions(pnlPreview, scrPreviewPanel,
                resourceMap.getString("txtInstructions.text.preview")), PREVIEWPANEL);


        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=0;
        gridBagConstraints.weightx=1.0;
        gridBagConstraints.weighty=1.0;
        gridBagConstraints.fill=GridBagConstraints.BOTH;
        gridBagConstraints.anchor=GridBagConstraints.NORTHWEST;
        getContentPane().add(tabMain, gridBagConstraints);


        // region Button Panel
        JPanel panButtons = new JPanel();
        panButtons.setName("panButtons");
        panButtons.setLayout(new GridBagLayout());

        JButton btnCancel = new JButton(resourceMap.getString("btnCancel.text"));
        btnCancel.setName("btnClose");
        btnCancel.setMnemonic(KeyEvent.VK_C);
        btnCancel.addActionListener(evt -> cancel());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 0, 5, 5);
        panButtons.add(btnCancel, gridBagConstraints);

        btnBack = new JButton(resourceMap.getString("btnBack.text"));
        btnBack.setName("btnBack");
        btnBack.setMnemonic(KeyEvent.VK_B);
        btnBack.addActionListener(evt -> back());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.weightx = 0.0;
        panButtons.add(btnBack, gridBagConstraints);

        btnNext = new JButton(resourceMap.getString("btnNext.text"));
        btnNext.setName("btnNext");
        btnNext.setMnemonic(KeyEvent.VK_N);
        btnNext.addActionListener(evt -> next());
        gridBagConstraints.gridx = 2;
        panButtons.add(btnNext, gridBagConstraints);

        btnFinish = new JButton(resourceMap.getString("btnFinish.text"));
        btnFinish.setName("btnFinish");
        btnFinish.setMnemonic(KeyEvent.VK_F);
        btnFinish.addActionListener(evt -> finish());
        gridBagConstraints.gridx = 3;
        panButtons.add(btnFinish, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx=0;
        gridBagConstraints.gridy=1;
        gridBagConstraints.fill=GridBagConstraints.HORIZONTAL;

        getContentPane().add(panButtons, gridBagConstraints);

        tabMain.addChangeListener(evt -> tabChanged());
        setEnabledTabs();

        // Go to first enabled tab.
        for (int i = tabMain.getSelectedIndex(); i < tabMain.getTabCount(); i++ ) {
            if (tabMain.isEnabledAt(i)) {
                tabMain.setSelectedIndex(i);
                break;
            }
        }

        tabChanged(); // Make sure the right buttons are active.

        setMinimumSize(UIUtil.scaleForGUI(850,600));
        setPreferredSize(UIUtil.scaleForGUI(850,1000));
    }

    //region Make Unit Status
    /**
     * Sub-function of initComponents. Makes the Unit Status Panel.
     * @return the Unit Status Panel.
     */
    private JPanel makeUnitStatusPanel() {
        GridBagConstraints gridBagConstraints;

        JPanel pnlUnitStatus = new JPanel(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlUnitStatus.add(new JLabel(resourceMap.getString("totaled")), gridBagConstraints);

        chksTotaled = new ArrayList<>();
        ustatuses = new ArrayList<>();
        btnsEditUnit = new ArrayList<>();
        lblsUnitName = new ArrayList<>();

        JLabel nameLbl;
        JCheckBox chkTotaled;
        JButton btnViewUnit;
        JButton btnEditUnit;

        int gridy = 2;
        int unitIndex = 0;
        for (Unit unit : tracker.getUnits()) {
            UnitStatus status = tracker.getUnitsStatus().get(unit.getId());
            ustatuses.add(status);
            nameLbl = new JLabel(status.getDesc());
            lblsUnitName.add(nameLbl);

            chkTotaled = new JCheckBox("");
            chkTotaled.setName("chkTotaled");
            chkTotaled.getAccessibleContext().setAccessibleName(resourceMap.getString("totaled"));
            chkTotaled.setSelected(status.isTotalLoss());
            chkTotaled.setName(Integer.toString(unitIndex));
            chkTotaled.setActionCommand(unit.getId().toString());
            chkTotaled.addItemListener(new CheckTotalListener());
            chksTotaled.add(chkTotaled);

            btnViewUnit = new JButton("View Unit");
            btnViewUnit.setActionCommand(unit.getId().toString());
            btnViewUnit.addActionListener(new ViewUnitListener(false));

            btnEditUnit = new JButton("Edit Unit");
            btnEditUnit.setEnabled(!status.isTotalLoss());
            btnEditUnit.setActionCommand(unit.getId().toString());
            btnEditUnit.setName(Integer.toString(unitIndex));
            btnEditUnit.addActionListener(new EditUnitListener());
            btnsEditUnit.add(btnEditUnit);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (unitIndex == tracker.getUnits().size() - 1) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlUnitStatus.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            pnlUnitStatus.add(chkTotaled, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            pnlUnitStatus.add(btnViewUnit, gridBagConstraints);
            gridBagConstraints.gridx = 3;
            pnlUnitStatus.add(btnEditUnit, gridBagConstraints);
            gridy++;
            unitIndex++;
        }
        return pnlUnitStatus;
    }

    // region Make Pilot Status
    /**
     * Sub-function of initComponents. Makes the Pilot Panel.
     * @return the Pilot Panel
     */
    private JPanel makePilotStatusPanel() {
        JPanel pnlPilotStatus = new JPanel();
        pnlPilotStatus.setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPilotStatus.add(new JLabel(resourceMap.getString("hits")), gridBagConstraints);

        gridBagConstraints.gridx = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        pnlPilotStatus.add(new JLabel(resourceMap.getString("mia")), gridBagConstraints);

        gridBagConstraints.gridx = 3;
        pnlPilotStatus.add(new JLabel(resourceMap.getString("kia")), gridBagConstraints);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0") );
        labelTable.put(1, new JLabel("1") );
        labelTable.put(2, new JLabel("2") );
        labelTable.put(3, new JLabel("3") );
        labelTable.put(4, new JLabel("4") );
        labelTable.put(5, new JLabel("5") );

        int sortedPeopleIndex = 0;
        int gridy = 2;

        for (PersonStatus status : tracker.getSortedPeople()) {
            pstatuses.add(status);

            int gridx = 0;

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (sortedPeopleIndex == tracker.getPeopleStatus().keySet().size() - 1) {
                gridBagConstraints.weighty = 1.0;
            }

            JLabel nameLbl = new JLabel("<html>" + status.getName() + "<br><i> " + status.getUnitName() + "</i></html>");
            pnlPilotStatus.add(nameLbl, gridBagConstraints);

            JSlider hitSlider = new JSlider(JSlider.HORIZONTAL, 0, 5, Math.min(status.getHits(), 5));
            hitSlider.setName(Integer.toString(sortedPeopleIndex));
            hitSlider.setMajorTickSpacing(1);
            hitSlider.setPaintTicks(true);
            hitSlider.setLabelTable(labelTable);
            hitSlider.setPaintLabels(true);
            hitSlider.setSnapToTicks(true);
            hitSliders.add(hitSlider);
            gridBagConstraints.gridx = gridx++;
            pnlPilotStatus.add(hitSlider, gridBagConstraints);

            JCheckBox miaCheck = new JCheckBox("");
            miaCheck.setName("miaCheck");
            miaCheck.getAccessibleContext().setAccessibleName(resourceMap.getString("mia"));
            miaCheck.setSelected(status.isMissing());
            miaBtns.add(miaCheck);
            gridBagConstraints.gridx = gridx++;
            pnlPilotStatus.add(miaCheck, gridBagConstraints);

            JCheckBox kiaCheck = new JCheckBox("");
            kiaCheck.setName("kiaCheck");
            kiaCheck.getAccessibleContext().setAccessibleName(resourceMap.getString("kia"));
            kiaCheck.addItemListener(new CheckBoxKIAListener(hitSlider, miaCheck));
            kiaCheck.setSelected(status.isDead());
            kiaBtns.add(kiaCheck);
            gridBagConstraints.gridx = gridx++;
            pnlPilotStatus.add(kiaCheck, gridBagConstraints);

            JButton btnViewPilot = new JButton("View Personnel");
            btnViewPilot.addActionListener(evt -> showPerson(status, false));
            gridBagConstraints.gridx = gridx++;
            pnlPilotStatus.add(btnViewPilot, gridBagConstraints);
            sortedPeopleIndex++;
        }

        return pnlPilotStatus;
    }

    // region Make Salvage
    /**
     * Sub-function of initComponents. Makes the Salvage Panel.
     * @return the Salvage Panel
     */
    private JPanel makeSalvagePanel() {
        // Create the panel
        JPanel pnlSalvage = new JPanel();
        pnlSalvage.setLayout(new GridBagLayout());
        JPanel pnlSalvageValue = new JPanel(new GridBagLayout());

        int gridx = 0;
        int gridy = 0;
        GridBagConstraints gridBagConstraints;
        if ((tracker.getMission() instanceof Contract) && !tracker.usesSalvageExchange()) {
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);

            JLabel lblSalvageValueUnit1 = new JLabel(resourceMap.getString("lblSalvageValueUnit1.text"));
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            pnlSalvageValue.add(lblSalvageValueUnit1, gridBagConstraints);

            lblSalvageValueUnit2 = new JLabel(salvageUnit.toAmountAndSymbolString());
            gridBagConstraints.gridx = gridx--;
            pnlSalvageValue.add(lblSalvageValueUnit2, gridBagConstraints);

            JLabel lblSalvageValueEmployer1 = new JLabel(resourceMap.getString("lblSalvageValueEmployer1.text"));
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            pnlSalvageValue.add(lblSalvageValueEmployer1, gridBagConstraints);

            lblSalvageValueEmployer2 = new JLabel(salvageEmployer.toAmountAndSymbolString());
            gridBagConstraints.gridx = gridx--;
            pnlSalvageValue.add(lblSalvageValueEmployer2, gridBagConstraints);

            JLabel lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvagePct1.text"));
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            pnlSalvageValue.add(lblSalvagePct1, gridBagConstraints);

            String salvageUsed = "<html>" + ((currentSalvagePct <= maxSalvagePct) ? "" : ReportingUtilities.spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())) + currentSalvagePct + '%' + ((currentSalvagePct <= maxSalvagePct) ? "" : ReportingUtilities.CLOSING_SPAN_TAG) + "<span>(max " + maxSalvagePct + "%)</span></html>";
            lblSalvagePct2 = new JLabel(salvageUsed);
            gridBagConstraints.gridx = gridx--;
            pnlSalvageValue.add(lblSalvagePct2, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 4;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(0, 0, 20, 0);
            pnlSalvage.add(pnlSalvageValue, gridBagConstraints);
        }

        // Update any indexing variables
        int salvageIndex = 0;
        gridx = 1;

        // Create the Title Line
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridy = gridy++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);

        gridBagConstraints.gridx = gridx++;
        pnlSalvage.add(new JLabel(resourceMap.getString("lblSalvage.text")), gridBagConstraints);

        gridBagConstraints.gridx = gridx++;
        pnlSalvage.add(new JLabel(resourceMap.getString("lblSell.text")), gridBagConstraints);

        gridBagConstraints.gridx = gridx++;
        pnlSalvage.add(new JLabel(resourceMap.getString("lblEscaped.text")), gridBagConstraints);

        // Initialize the tracking ArrayLists
        salvageUnitLabel = new ArrayList<>();
        salvageBoxes = new ArrayList<>();
        soldUnitBoxes = new ArrayList<>();
        escapeBoxes = new ArrayList<>();
        btnsSalvageEditUnit = new ArrayList<>();

        // Create the GridBagConstraint to use for the buttons
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);

        for (TestUnit unit : tracker.getPotentialSalvage()) {
            // Initial variable work
            gridx = 0;
            salvageables.add(unit);
            UnitStatus status = tracker.getSalvageStatus().get(unit.getId());

            // Initial update to the GridBagConstraints
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.gridy = gridy++;
            if (salvageIndex == tracker.getPotentialSalvage().size() - 1) {
                gridBagConstraints.weighty = 1.0;
            }

            // Now, we start creating the boxes
            JLabel salvageUnit = new JLabel(status.getDesc(true));
            salvageUnitLabel.add(salvageUnit);
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(salvageUnit, gridBagConstraints);

            JCheckBox salvaged = new JCheckBox("");
            salvaged.setName("salvaged");
            salvaged.getAccessibleContext().setAccessibleName(resourceMap.getString("lblSalvage.text"));
            salvaged.setEnabled(!tracker.usesSalvageExchange());
            salvaged.setSelected(!tracker.usesSalvageExchange() && (maxSalvagePct >= 100));
            salvaged.addItemListener(evt -> checkSalvageRights());
            salvageBoxes.add(salvaged);
            gridBagConstraints.anchor = GridBagConstraints.NORTH;
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(salvaged, gridBagConstraints);

            JCheckBox sold = new JCheckBox("");
            sold.setName("sold");
            sold.getAccessibleContext().setAccessibleName(resourceMap.getString("lblSell.text"));
            sold.setEnabled(!tracker.usesSalvageExchange()  && tracker.getCampaign().getCampaignOptions().isSellUnits());
            sold.addItemListener(evt -> checkSalvageRights());
            soldUnitBoxes.add(sold);
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(sold, gridBagConstraints);

            JCheckBox escaped = new JCheckBox("");
            escaped.setName("escaped");
            escaped.getAccessibleContext().setAccessibleName(resourceMap.getString("lblEscaped.text"));
            escaped.setEnabled(!(unit.getEntity().isDestroyed() || unit.getEntity().isDoomed()));
            escaped.setSelected(!status.isLikelyCaptured());
            escaped.addItemListener(evt -> checkSalvageRights());
            escaped.setActionCommand(unit.getEntity().getExternalIdAsString());
            escapeBoxes.add(escaped);
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(escaped, gridBagConstraints);

            JButton btnSalvageViewUnit = new JButton("View Unit");
            btnSalvageViewUnit.setActionCommand(unit.getId().toString());
            btnSalvageViewUnit.addActionListener(new ViewUnitListener(true));
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(btnSalvageViewUnit, gridBagConstraints);

            JButton btnSalvageEditUnit = new JButton("Edit Unit");
            btnSalvageEditUnit.setName(Integer.toString(salvageIndex++));
            btnSalvageEditUnit.setActionCommand(unit.getId().toString());
            btnSalvageEditUnit.addActionListener(new EditUnitListener(true));
            btnsSalvageEditUnit.add(btnSalvageEditUnit);
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(btnSalvageEditUnit, gridBagConstraints);
        }

        checkSalvageRights();
        return pnlSalvage;
    }


    // region Make Prisoner
    /**
     * Sub-function of initComponents. Makes the Prisoner Panel.
     * @return the Prisoner Panel
     */
    private JPanel makePrisonerStatusPanel() {
        JPanel pnlPrisonerStatus = new JPanel();
        pnlPrisonerStatus.setLayout(new GridBagLayout());

        int gridx = 1;

        prisonerRansomAllButton = new JButton(resourceMap.getString("prisonerRansomAllButton.text"));

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx=2;
        gridBagConstraints.gridy=0;
        gridBagConstraints.gridwidth=3;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPrisonerStatus.add(prisonerRansomAllButton, gridBagConstraints);

        prisonerRansomAllButton.addActionListener(evt -> prisonerRansomAll());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = gridx++;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPrisonerStatus.add(new JLabel(resourceMap.getString("hits")), gridBagConstraints);

        gridBagConstraints.gridx = gridx++;
        pnlPrisonerStatus.add(new JLabel(resourceMap.getString("prisoner")), gridBagConstraints);

        gridBagConstraints.gridx = gridx++;
        pnlPrisonerStatus.add(new JLabel(resourceMap.getString("prisonerRansomed.text")), gridBagConstraints);

        gridBagConstraints.gridx = gridx++;
        pnlPrisonerStatus.add(new JLabel(resourceMap.getString("kia")), gridBagConstraints);

        int prisonerIndex = 0;
        int gridy = 2;

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0") );
        labelTable.put(1, new JLabel("1") );
        labelTable.put(2, new JLabel("2") );
        labelTable.put(3, new JLabel("3") );
        labelTable.put(4, new JLabel("4") );
        labelTable.put(5, new JLabel("5") );

        for (OppositionPersonnelStatus status : tracker.getSortedPrisoners()) {
            prstatuses.add(status);

            gridx = 0;

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (prisonerIndex == tracker.getOppositionPersonnel().keySet().size() - 1) {
                gridBagConstraints.weighty = 1.0;
            }

            JLabel nameLbl = new JLabel("<html>" + status.getName() + "<br><i> " + status.getUnitName() + "</i></html>");
            pnlPrisonerStatus.add(nameLbl, gridBagConstraints);

            JSlider hitSlider = new JSlider(JSlider.HORIZONTAL, 0, 5, Math.min(status.getHits(), 5));
            hitSlider.setName(Integer.toString(prisonerIndex));
            hitSlider.setMajorTickSpacing(1);
            hitSlider.setPaintTicks(true);
            hitSlider.setLabelTable(labelTable);
            hitSlider.setPaintLabels(true);
            hitSlider.setSnapToTicks(true);
            pr_hitSliders.add(hitSlider);
            gridBagConstraints.anchor = GridBagConstraints.NORTH;
            gridBagConstraints.gridx = gridx++;
            pnlPrisonerStatus.add(hitSlider, gridBagConstraints);

            JCheckBox prisonerCapturedCheck = new JCheckBox("");
            prisonerCapturedCheck.setName("prisonerCapturedCheck");
            prisonerCapturedCheck.getAccessibleContext().setAccessibleName(resourceMap.getString("prisoner"));
            prisonerCapturedCheck.setSelected(status.isCaptured());
            prisonerCapturedCheck.addItemListener(evt -> checkPrisonerStatus());
            prisonerCapturedBtns.add(prisonerCapturedCheck);
            gridBagConstraints.gridx = gridx++;
            pnlPrisonerStatus.add(prisonerCapturedCheck, gridBagConstraints);

            JCheckBox prisonerRansomedCheck = new JCheckBox("");
            prisonerRansomedCheck.setName("prisonerRansomedCheck");
            prisonerRansomedCheck.getAccessibleContext().setAccessibleName(resourceMap.getString("prisonerRansomed.text"));
            prisonerRansomedCheck.setEnabled(!status.isCaptured());
            prisonerRansomedCheck.addItemListener(evt -> checkPrisonerStatus());
            prisonerRansomedBtns.add(prisonerRansomedCheck);
            gridBagConstraints.gridx = gridx++;
            pnlPrisonerStatus.add(prisonerRansomedCheck, gridBagConstraints);

            JCheckBox kiaCheck = new JCheckBox("");
            kiaCheck.setName("kiaCheck");
            kiaCheck.getAccessibleContext().setAccessibleName(resourceMap.getString("kia"));
            kiaCheck.addItemListener(evt -> checkPrisonerStatus());
            prisonerKiaBtns.add(kiaCheck);
            gridBagConstraints.gridx = gridx++;
            pnlPrisonerStatus.add(kiaCheck, gridBagConstraints);

            JButton btnViewPrisoner = new JButton("View Personnel");
            btnViewPrisoner.addActionListener(evt -> showPerson(status, true));
            gridBagConstraints.gridx = gridx++;
            pnlPrisonerStatus.add(btnViewPrisoner, gridBagConstraints);

            // if the person is dead, set the checkbox and skip all this captured stuff
            if ((status.getHits() > 5) || status.isDead()) {
                kiaCheck.setSelected(true);
            } else if (status.isCaptured()
                    && tracker.getCampaign().getCampaignOptions().getPrisonerCaptureStyle().isAtB()) {
                boolean wasCaptured;
                if (status.wasPickedUp()) {
                    wasCaptured = true;
                } else {
                    wasCaptured = rollForCapture(tracker.getCampaign(), status);
                }
                prisonerCapturedCheck.setSelected(wasCaptured);
            }

            // When generating NPC personnel, we use placeholder characters and then later re-assign
            // their details to match expected values.
            // This causes a disconnect between their name, at point of creation, and the name
            // presented to the user.
            // We therefore need to re-generate the personality description at this point,
            // as this is the earliest point in which that description is visible to the user
            writeDescription(status.getPerson());
            prisonerIndex++;
        }

        return pnlPrisonerStatus;
    }


    /**
     * Check Ransom box for every prisoner that has Captured checked.
     */
    private void prisonerRansomAll() {
        Iterator<JCheckBox> capturedBoxes = prisonerCapturedBtns.iterator();
        Iterator<JCheckBox> ransomedBoxes = prisonerRansomedBtns.iterator();
        while (capturedBoxes.hasNext() && ransomedBoxes.hasNext()) {
            JCheckBox capturedBox = capturedBoxes.next();
            JCheckBox ransomedBox = ransomedBoxes.next();
            if (capturedBox.isSelected()) {
                ransomedBox.setSelected(true);
            }
        }
    }


    /**
     * Rolls the dice to determine if the opposition personnel is captured.
     *
     * @param status The opposition personnel status object.
     * @return True if the opposition personnel was captured, false otherwise.
     */
    private static boolean rollForCapture(Campaign campaign, OppositionPersonnelStatus status) {
        int target = 6;

        if (campaign.getGameOptions().booleanOption(OptionsConstants.ADVGRNDMOV_EJECTED_PILOTS_FLEE)) {
            target = 5;
        }

        for (int n = 0; n < status.getHits() + 1; n++) {
            if (Utilities.dice(1, 6) >= target) {
                return true;
            }
        }

        return false;
    }


    // region Make Kills
    /**
     * Sub-function of initComponents. Makes the Kills Panel.
     * @return the Kills Panel
     */
    private JPanel makeKillsPanel() {
        JPanel pnlKills = new JPanel();
        killChoices = new Hashtable<>();
        pnlKills.setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlKills.add(new JLabel(resourceMap.getString("kill")), gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        gridBagConstraints.weightx = 1.0;
        pnlKills.add(new JLabel(resourceMap.getString("claim")), gridBagConstraints);

        JComboBox<String> comboAssign;
        DefaultComboBoxModel<String> assignModel;
        int gridy = 2;
        int killIndex = 0;
        for (String killName : tracker.getKillCredits().keySet()) {
            JLabel nameLbl = new JLabel(killName);
            assignModel = new DefaultComboBoxModel<>();
            assignModel.addElement(resourceMap.getString("none"));
            int index = 0;
            int selected = 0;
            if (null == tracker.getKillCredits().get(killName)) {
                continue;
            }
            for (Unit unit : tracker.getUnits()) {
                index++;
                if (unit.getEntity() instanceof GunEmplacement) {
                    assignModel.addElement("AutoTurret, " + unit.getName());
                } else {
                    assignModel.addElement(unit.getCommander().getFullTitle() + ", " + unit.getName());
                }

                if (unit.getId().toString().equals(tracker.getKillCredits().get(killName))) {
                    selected = index;
                }
            }
            comboAssign = new JComboBox<>(assignModel);
            comboAssign.setSelectedIndex(selected);
            killChoices.put(killName, comboAssign);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (killIndex == tracker.getKillCredits().keySet().size() - 1) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlKills.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            pnlKills.add(comboAssign, gridBagConstraints);
            gridy++;
            killIndex++;
        }
        return pnlKills;
    }


    // region Make Rewards
    /**
     * Sub-function of initComponents. Makes the Rewards Panel.
     * @return the Rewards Panel
     */
    private JPanel makeRewardsPanel() {
        JPanel pnlRewards = new JPanel();
        pnlRewards.setLayout(new GridBagLayout());
        lootBoxes = new ArrayList<>();
        int gridy = 0;
        int lootIndex = 0;
        GridBagConstraints gridBagConstraints;
        for (Loot loot : loots) {
            JCheckBox box = new JCheckBox(loot.getShortDescription());
            box.setSelected(true);
            lootBoxes.add(box);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = gridy;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if (lootIndex == loots.size() - 1) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlRewards.add(box, gridBagConstraints);
            gridy++;
            lootIndex++;
        }
        return pnlRewards;
    }


    // region Make Objective
    /**
     * Sub-function of initComponents. Makes the Objective Status panel.
     * @return the Objective Status panel
     */
    private JPanel makeObjectiveStatusPanel() {
        JPanel pnlObjectiveStatus = new JPanel();
        pnlObjectiveStatus.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(5, 5, 0, 0);
        pnlObjectiveStatus.add(new JLabel("Objectives:"), gbc);
        objectiveCheckboxes = new HashMap<>();
        objectiveOverrideCheckboxes = new HashMap<>();

        objectiveProcessor.evaluateScenarioObjectives(tracker);

        Map<ScenarioObjective, Set<String>> potentialObjectiveUnits = objectiveProcessor.getPotentialObjectiveUnits();
        Map<ScenarioObjective, Set<String>> qualifyingObjectiveUnits = objectiveProcessor.getQualifyingObjectiveUnits();

        for (ScenarioObjective objective : tracker.getScenario().getScenarioObjectives()) {
            // first, we determine the list of units that are potentially associated with the current objective
            List<String> currentObjectiveUnits = new ArrayList<>();
            for (String unitID : potentialObjectiveUnits.get(objective)) {
                UUID uuid = UUID.fromString(unitID);
                if (tracker.getAllInvolvedUnits().containsKey(uuid)) {
                    currentObjectiveUnits.add(unitID);
                }
            }

            // if it's a custom objective or there are no associated units, we display
            // a single line and an override
            if (currentObjectiveUnits.isEmpty()) {
                JCheckBox chkObjective = new JCheckBox();
                chkObjective.setText(objective.getDescription());
                chkObjective.setForeground(Color.RED);
                gbc.gridy++;
                pnlObjectiveStatus.add(chkObjective, gbc);
                objectiveOverrideCheckboxes.put(objective, chkObjective);

                chkObjective.addItemListener(e -> {
                    JCheckBox source = (JCheckBox) e.getSource();
                    source.setForeground(source.isSelected() ? Color.green.darker() : Color.RED);
                });

                continue;
            }

            // each "standard" objective has a list of units that determine whether it's completed
            // the objective matrix contains a set of unit IDs that meet the objective
            JLabel lblObjective = new JLabel(objective.toShortString());
            gbc.gridy++;
            pnlObjectiveStatus.add(lblObjective, gbc);

            objectiveCheckboxes.put(objective, new ArrayList<>());

            for (String unitID : currentObjectiveUnits) {
                UUID uuid = UUID.fromString(unitID);

                JCheckBox chkItemState = new JCheckBox(tracker.getAllInvolvedUnits().get(uuid).getShortName());
                chkItemState.setSelected(qualifyingObjectiveUnits.get(objective).contains(unitID));
                chkItemState.setActionCommand(unitID);
                chkItemState.addItemListener(e -> updateObjectiveDisplay(objective, lblObjective));
                gbc.gridy++;
                pnlObjectiveStatus.add(chkItemState, gbc);
                objectiveCheckboxes.get(objective).add(chkItemState);
            }

            updateObjectiveDisplay(objective, lblObjective);
        }
        // To push the objective list up to the top of the panel
        gbc.gridy++;
        gbc.weighty = 1.0;
        JLabel lblPlaceholder = new JLabel(" ");
        pnlObjectiveStatus.add(lblPlaceholder,gbc);
        return pnlObjectiveStatus;
    }


    // region Make Preview
    /**
     * Sub-function of initComponents. Makes the final panel.
     * @return the preview panel
     */
    private JPanel makePreviewPanel() {
        JPanel pnlPreview = new DefaultMHQScrollablePanel(frame, "Test");
        choiceStatus = new JComboBox<>();
        txtReport = new MarkdownEditorPanel("After-Action Report");
        txtRecoveredUnits = new JTextArea();
        txtRecoveredPilots = new JTextArea();
        txtMissingUnits = new JTextArea();
        txtMissingPilots = new JTextArea();
        txtDeadPilots = new JTextArea();
        txtSalvage = new JTextArea();
        txtRewards = new JTextArea();
        JLabel lblStatus = new JLabel();

        pnlPreview.setLayout(new GridBagLayout());

        JPanel pnlStatus = new JPanel();

        lblStatus.setText(resourceMap.getString("lblStatus.text"));
        DefaultComboBoxModel<ScenarioStatus> scenarioStatusModel = new DefaultComboBoxModel<>(ScenarioStatus.values());
        scenarioStatusModel.removeElement(ScenarioStatus.CURRENT);
        choiceStatus.setModel(scenarioStatusModel);
        choiceStatus.setName("choiceStatus");

        // dynamically update victory/defeat dropdown based on objective checkboxes
        ScenarioStatus scenarioStatus = objectiveProcessor.determineScenarioStatus(tracker.getScenario(),
                getObjectiveOverridesStatus(), getObjectiveUnitCounts());
        choiceStatus.setSelectedItem(scenarioStatus);

        pnlStatus.setLayout(new FlowLayout(FlowLayout.LEADING, 5, 5));
        pnlStatus.add(lblStatus);
        pnlStatus.add(choiceStatus);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(pnlStatus, gridBagConstraints);

        txtRewards.setText(resourceMap.getString("none"));
        txtRewards.setEditable(false);
        txtRewards.setLineWrap(true);
        txtRewards.setWrapStyleWord(true);
        txtRewards.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtRewards.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(txtRewards, gridBagConstraints);

        txtReport.setText("");
        txtReport.setPreferredSize(UIUtil.scaleForGUI(500, 300));
        txtReport.setMinimumSize(UIUtil.scaleForGUI(500, 300));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        pnlPreview.add(txtReport, gridBagConstraints);

        txtRecoveredUnits.setName("txtRecoveredUnits");
        txtRecoveredUnits.setText(resourceMap.getString("none"));
        txtRecoveredUnits.setEditable(false);
        txtRecoveredUnits.setLineWrap(true);
        txtRecoveredUnits.setWrapStyleWord(true);
        txtRecoveredUnits.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtRecoveredUnits.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(txtRecoveredUnits, gridBagConstraints);

        txtRecoveredPilots.setName("txtRecoveredPilots");
        txtRecoveredPilots.setText(resourceMap.getString("none"));
        txtRecoveredPilots.setEditable(false);
        txtRecoveredPilots.setLineWrap(true);
        txtRecoveredPilots.setWrapStyleWord(true);
        txtRecoveredPilots.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtRecoveredPilots.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(txtRecoveredPilots, gridBagConstraints);

        txtMissingUnits.setName("txtMissingUnits");
        txtMissingUnits.setText(resourceMap.getString("none"));
        txtMissingUnits.setEditable(false);
        txtMissingUnits.setLineWrap(true);
        txtMissingUnits.setWrapStyleWord(true);
        txtMissingUnits.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtMissingUnits.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(txtMissingUnits, gridBagConstraints);

        txtMissingPilots.setName("txtMissingPilots");
        txtMissingPilots.setText(resourceMap.getString("none"));
        txtMissingPilots.setEditable(false);
        txtMissingPilots.setLineWrap(true);
        txtMissingPilots.setWrapStyleWord(true);
        txtMissingPilots.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtMissingPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(txtMissingPilots, gridBagConstraints);

        txtSalvage.setName("txtSalvage");
        txtSalvage.setText("None");
        txtSalvage.setEditable(false);
        txtSalvage.setLineWrap(true);
        txtSalvage.setWrapStyleWord(true);
        txtSalvage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtSalvagedUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(txtSalvage, gridBagConstraints);

        txtDeadPilots.setName("txtDeadPilots");
        txtDeadPilots.setText(resourceMap.getString("none"));
        txtDeadPilots.setEditable(false);
        txtDeadPilots.setLineWrap(true);
        txtDeadPilots.setWrapStyleWord(true);
        txtDeadPilots.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtDeadPilots.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(txtDeadPilots, gridBagConstraints);

        return pnlPreview;
    }


    // region Misc
    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ResolveScenarioWizardDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    /**
     * @param toWrap - A JPanel to work with
     * @param scrPane - JScrollPane to use, or null and we'll make a new one
     * @param instructionText - The text for the instruction box
     * @return A new JPanel containing the instruction text box above toWrap
     */
    private JPanel wrapWithInstructions(JPanel toWrap, JScrollPane scrPane, String instructionText) {
        JTextArea instructions = new JTextArea();
        instructions.setText(instructionText);
        instructions.setEditable(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));


        JPanel container = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        container.add(instructions, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;

        scrPane = null != scrPane ? scrPane : new JScrollPaneWithSpeed();
        scrPane.setViewportView(toWrap);
        container.add(scrPane, gridBagConstraints);

        return container;
    }


    // region Inter-tab updates

    /**
     * Something changed on the Units panel, update objective panel based on current unit status.
     */
    private void updateFromUnitsTab () {
        for (JCheckBox box : chksTotaled) {
            UUID id = UUID.fromString(box.getActionCommand());
            objectiveProcessor.updateObjectiveEntityState(tracker.getAllInvolvedUnits().get(id),
                    false, box.isSelected(), !tracker.playerHasBattlefieldControl());
        }
    }


    /**
     * Something changed on the Salvage panel, update objective panel based on current salvage status.
     */
    private void updateFromSalvageTab() {
        for (JCheckBox box : escapeBoxes) {
            UUID id = UUID.fromString(box.getActionCommand());
            objectiveProcessor.updateObjectiveEntityState(tracker.getAllInvolvedUnits().get(id),
                    box.isSelected(), false, tracker.playerHasBattlefieldControl());
        }
    }


    /**
     * Something chnaged about units OR salvage, we need to update the objective panel.
     */
    private void recheckObjectives() {
        for (ScenarioObjective objective : objectiveCheckboxes.keySet()) {
            for (JCheckBox checkBox : objectiveCheckboxes.get(objective)) {
                checkBox.setSelected(objectiveProcessor.getQualifyingObjectiveUnits()
                        .get(objective).contains(checkBox.getActionCommand()));
            }
        }
    }

    /**
     * Updates the final panel with information taken from the other ones.
     */
    private void updatePreviewPanel() {
        // set victory/defeat status based on scenario objectives
        ScenarioStatus scenarioStatus = objectiveProcessor.determineScenarioStatus(tracker.getScenario(),
                getObjectiveOverridesStatus(), getObjectiveUnitCounts());
        choiceStatus.setSelectedItem(scenarioStatus);

        // do a "dry run" of the scenario objectives to output a report
        StringBuilder reportText = new StringBuilder();

        if (tracker.getScenario().hasObjectives()) {
            for (ScenarioObjective objective : tracker.getScenario().getScenarioObjectives()) {
                int qualifyingUnitCount = 0;

                if (objectiveCheckboxes.containsKey(objective)) {
                    for (JCheckBox box : objectiveCheckboxes.get(objective)) {
                        if (box.isSelected()) {
                            qualifyingUnitCount++;
                        }
                    }
                }

                Boolean override = null;
                if (objectiveOverrideCheckboxes.containsKey(objective)) {
                    override = objectiveOverrideCheckboxes.get(objective).isSelected();
                }

                reportText.append(objectiveProcessor.processObjective(campaign, objective,
                    qualifyingUnitCount, override, tracker, true));
                reportText.append('\n');
            }

            txtReport.setText(reportText.toString());
        }

        //pilots first
        StringBuilder missingNames = new StringBuilder();
        StringBuilder kiaNames = new StringBuilder();
        StringBuilder recoverNames = new StringBuilder();
        for (int i = 0; i < pstatuses.size(); i++) {
            PersonStatus status = pstatuses.get(i);
            if (hitSliders.get(i).getValue() >= 6 || kiaBtns.get(i).isSelected()) {
                kiaNames.append(status.getName()).append('\n');
            } else if (miaBtns.get(i).isSelected()) {
                missingNames.append(status.getName()).append('\n');
            } else {
                recoverNames.append(status.getName()).append('\n');
            }
        }
        txtRecoveredPilots.setText(recoverNames.toString());
        txtMissingPilots.setText(missingNames.toString());
        txtDeadPilots.setText(kiaNames.toString());

        //now units
        StringBuilder recoverUnits = new StringBuilder();
        StringBuilder missUnits = new StringBuilder();
        for (int i = 0; i < chksTotaled.size(); i++) {
            String name = ustatuses.get(i).getName();
            if (chksTotaled.get(i).isSelected()) {
                missUnits.append(name).append('\n');
            } else {
                recoverUnits.append(name).append('\n');
            }
        }
        txtRecoveredUnits.setText(recoverUnits.toString());
        txtMissingUnits.setText(missUnits.toString());

        //now salvage
        StringBuilder salvageUnits = new StringBuilder();
        for (int i = 0; i < salvageBoxes.size(); i++) {
            JCheckBox box = salvageBoxes.get(i);
            if (box.isSelected()) {
                salvageUnits.append(salvageables.get(i).getName()).append('\n');
            }
        }
        txtSalvage.setText(salvageUnits.toString());

        //now rewards
        StringBuilder claimed = new StringBuilder();
        for (int i = 0; i < lootBoxes.size(); i++) {
            JCheckBox box = lootBoxes.get(i);
            if (box.isSelected()) {
                claimed.append(loots.get(i).getShortDescription()).append('\n');
            }
        }
        txtRewards.setText(claimed.toString());
        SwingUtilities.invokeLater(() -> scrPreviewPanel.getVerticalScrollBar().setValue(0));
    }


    // region Tab Movement

    /**
     * Go to next tab, if there is one. Button listener.
     */
    private void next() {
        for (int i = tabMain.getSelectedIndex() + 1; i < tabMain.getTabCount(); i++) {
            if (tabMain.isEnabledAt(i)) {
                tabMain.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Go to previous tab, if there is one. Button listener.
     */
    private void back() {
        for (int i = tabMain.getSelectedIndex() - 1; i >= 0; i--) {
            if (tabMain.isEnabledAt(i)) {
                tabMain.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Update buttons and status because the tab changed. Event Listener.
     */
    private void tabChanged() {
        int current = tabMain.getSelectedIndex();
        boolean nextEnable = false;
        boolean prevEnable = false;

        for (int i = current + 1; i < tabMain.getTabCount(); i++) {
            if (tabMain.isEnabledAt(i)) {
                nextEnable = true;
                break;
            }
        }

        for (int i = current - 1; i >= 0; i--) {
            if (tabMain.isEnabledAt(i)) {
                prevEnable = true;
                break;
            }
        }

        btnBack.setEnabled(prevEnable);
        btnNext.setEnabled(nextEnable);

        btnFinish.setEnabled(current == tabMain.getTabCount() - 1);
        // Let's just call these all on tab change for safety for now
        updateFromUnitsTab();
        updateFromSalvageTab();
        // TODO: WeaverThree - This wipes out user selecitons on the objective panel, so we can't use it right now.
        // recheckObjectives();
        updatePreviewPanel();
    }


    // region Finish

    /**
     * End the wizard and update everything in the game that results from it.
     */
    private void finish() {
        //unit status
        for (JCheckBox box : chksTotaled) {
            UUID id = UUID.fromString(box.getActionCommand());
            if (null == tracker.getUnitsStatus().get(id)) {
                continue;
            }
            tracker.getUnitsStatus().get(id).setTotalLoss(box.isSelected());
        }

        //now personnel
        for (int i = 0; i < pstatuses.size(); i++) {
            PersonStatus status = pstatuses.get(i);

            if (hitSliders.get(i).isEnabled()) {
                status.setHits(hitSliders.get(i).getValue());
            }
            status.setMissing(miaBtns.get(i).isSelected());
            status.setDead(kiaBtns.get(i).isSelected());
        }

        //now prisoners
        for (int i = 0; i < prstatuses.size(); i++) {
            OppositionPersonnelStatus status = prstatuses.get(i);

            if (pr_hitSliders.get(i).isEnabled()) {
                status.setHits(pr_hitSliders.get(i).getValue());
            }
            status.setCaptured(prisonerCapturedBtns.get(i).isSelected());
            status.setRansomed(prisonerRansomedBtns.get(i).isSelected());
            status.setDead(prisonerKiaBtns.get(i).isSelected());
        }

        //now salvage
        for (int i = 0; i < salvageBoxes.size(); i++) {
            JCheckBox salvaged = salvageBoxes.get(i);
            JCheckBox sold = soldUnitBoxes.get(i);
            JCheckBox escaped = escapeBoxes.get(i);
            if (salvaged.isSelected()) {
                tracker.salvageUnit(i);
            } else if (sold.isSelected()) {
                tracker.sellUnit(i);
            } else if (!escaped.isSelected()) { // Only salvage if they don't escape
                tracker.doNotSalvageUnit(i);
            }
        }

        // now DropShip bonuses (if any)
        if (tracker.getDropShipBonus().isPositive()) {
            tracker.getCampaign().getFinances().credit(
                    TransactionType.MISCELLANEOUS,
                    tracker.getCampaign().getLocalDate(),
                    tracker.getDropShipBonus(),
                    resourceMap.getString("dropShipBonus.text")
            );
        }

        //now assign kills
        for (String killName : tracker.getKillCredits().keySet()) {
            if (killChoices.get(killName).getSelectedIndex() == 0) {
                tracker.getKillCredits().put(killName, "None");
            } else {
                Unit u = tracker.getUnits().get(killChoices.get(killName).getSelectedIndex() - 1);
                if (null != u) {
                    tracker.getKillCredits().put(killName, u.getId().toString());
                }
            }
        }

        tracker.assignKills();

        //now get loot
        if (((ScenarioStatus) Objects.requireNonNull(choiceStatus.getSelectedItem())).isOverallVictory()) {
            for (int i = 0; i < lootBoxes.size(); i++) {
                JCheckBox box = lootBoxes.get(i);
                if (box.isSelected()) {
                    tracker.addLoot(loots.get(i));
                }
            }
        }

        //now process
        tracker.resolveScenario((ScenarioStatus) choiceStatus.getSelectedItem(), txtReport.getText());

        if (tracker.getScenario().hasObjectives()) {
            // process objectives here
            for (ScenarioObjective objective : tracker.getScenario().getScenarioObjectives()) {
                int qualifyingUnitCount = 0;

                if (objectiveCheckboxes.containsKey(objective)) {
                    for (JCheckBox box : objectiveCheckboxes.get(objective)) {
                        if (box.isSelected()) {
                            qualifyingUnitCount++;
                        }
                    }
                }

                Boolean override = null;
                if (objectiveOverrideCheckboxes.containsKey(objective)) {
                    override = objectiveOverrideCheckboxes.get(objective).isSelected();
                }

                objectiveProcessor.processObjective(campaign, objective, qualifyingUnitCount, override, tracker, false);
            }
        }

        StratconRulesManager.processScenarioCompletion(campaign, tracker);

        this.setVisible(false);
    }

    private void cancel() {
        setVisible(false);
    }


    // region Misc II

    /**
     * Figuires out which tabs should be enabled. Needs to be called after all tabs are generated.
     */
    private void setEnabledTabs() {
        for( int i = 0; i < tabMain.getTabCount(); i++ ) {
            boolean enable = switch (tabMain.getTitleAt(i)) {
                case UNITSPANEL -> !tracker.getUnitsStatus().keySet().isEmpty();
                case OBJECTIVEPANEL -> tracker.getScenario().hasObjectives();
                case PILOTPANEL -> !tracker.getPeopleStatus().keySet().isEmpty();
                case PRISONERPANEL -> !tracker.getOppositionPersonnel().keySet().isEmpty();
                case SALVAGEPANEL -> !tracker.getPotentialSalvage().isEmpty()
                        && (!(tracker.getMission() instanceof Contract)
                        || ((Contract) tracker.getMission()).canSalvage());
                case KILLSPANEL -> !tracker.getKillCredits().isEmpty();
                case REWARDPANEL -> !loots.isEmpty();
                case PREVIEWPANEL -> true;
                default -> false;
            };
            tabMain.setEnabledAt(i, enable);
        }
    }


    /**
     * Count up the selected objective checkboxes
     * @return the count of selected objective checkboxes
     */
    private Map<ScenarioObjective, Integer> getObjectiveUnitCounts() {
        Map<ScenarioObjective, Integer> objectiveUnitCounts = new HashMap<>();

        if (objectiveCheckboxes == null) {
            return objectiveUnitCounts;
        }

        for (ScenarioObjective objective : objectiveCheckboxes.keySet()) {
            int qualifyingUnitCount = 0;

            for (JCheckBox box : objectiveCheckboxes.get(objective)) {
                if (box.isSelected()) {
                    qualifyingUnitCount++;
                }
            }

            objectiveUnitCounts.put(objective, qualifyingUnitCount);
        }

        return objectiveUnitCounts;
    }

    /**
     * Determine the status of each custom objective checkbox
     * @return the true/false condition of custom objective checkboxes
     */
    private Map<ScenarioObjective, Boolean> getObjectiveOverridesStatus() {
        Map<ScenarioObjective, Boolean> objectiveOverrides = new HashMap<>();

        if (objectiveOverrideCheckboxes == null) {
            return objectiveOverrides;
        }

        for (ScenarioObjective objective : objectiveOverrideCheckboxes.keySet()) {
            objectiveOverrides.put(objective, objectiveOverrideCheckboxes.get(objective).isSelected());
        }

        return objectiveOverrides;
    }


    /**
     * Event listener for changes to prisoner status.
     */
    private void checkPrisonerStatus() {
        for (int i = 0; i < prisonerCapturedBtns.size(); i++) {
            JCheckBox captured = prisonerCapturedBtns.get(i);
            JCheckBox ransomed = prisonerRansomedBtns.get(i);
            JCheckBox kia = prisonerKiaBtns.get(i);
            JSlider wounds = pr_hitSliders.get(i);
            if (kia.isSelected()) {
                captured.setSelected(false);
                captured.setEnabled(false);
                ransomed.setSelected(false);
                ransomed.setEnabled(false);
                wounds.setEnabled(false);
            } else if (ransomed.isSelected()) {
                captured.setSelected(true);
                captured.setEnabled(false);
                kia.setSelected(false);
                wounds.setEnabled(true);
            } else if (captured.isSelected()) {
                captured.setEnabled(true);
                ransomed.setEnabled(true);
                kia.setSelected(false);
                wounds.setEnabled(true);
            } else {
                captured.setEnabled(true);
                ransomed.setEnabled(false);
                wounds.setEnabled(true);
            }
        }
    }


    /**
     * Updates the salvage percentages used and such. Event listener for slavage changes.
     */
    private void checkSalvageRights() {
        // Perform a little magic to make sure we aren't trying to do more than one of these things
        for (int i = 0; i < escapeBoxes.size(); i++) {
            JCheckBox salvaged = salvageBoxes.get(i);
            JCheckBox sold = soldUnitBoxes.get(i);
            JCheckBox escaped = escapeBoxes.get(i);
            if (sold.isSelected()) {
                salvaged.setSelected(false);
                salvaged.setEnabled(false);
                escaped.setSelected(false);
                escaped.setEnabled(false);
            } else if (escaped.isSelected()) {
                salvaged.setSelected(false);
                salvaged.setEnabled(false);
                sold.setSelected(false);
                sold.setEnabled(false);
                btnsSalvageEditUnit.get(i).setEnabled(false);
            } else if (salvaged.isSelected()) {
                sold.setSelected(false);
                sold.setEnabled(false);
                escaped.setSelected(false);
                escaped.setEnabled(false);
            } else {
                salvaged.setEnabled(!tracker.usesSalvageExchange());
                sold.setEnabled(!tracker.usesSalvageExchange() && tracker.getCampaign().getCampaignOptions().isSellUnits());
                escaped.setEnabled(true);
                btnsSalvageEditUnit.get(i).setEnabled(true);
            }
        }

        if (!(tracker.getMission() instanceof Contract) || tracker.usesSalvageExchange()) {
            return;
        }
        salvageEmployer = ((Contract) tracker.getMission()).getSalvagedByEmployer();
        salvageUnit = ((Contract) tracker.getMission()).getSalvagedByUnit();
        for (int i = 0; i < salvageBoxes.size(); i++) {
            // Skip the escaping units
            if (escapeBoxes.get(i).isSelected()) {
                continue;
            }

            // Set up the values
            if (salvageBoxes.get(i).isSelected() || soldUnitBoxes.get(i).isSelected()) {
                salvageUnit = salvageUnit.plus(salvageables.get(i).getSellValue());
            } else {
                salvageEmployer = salvageEmployer.plus(salvageables.get(i).getSellValue());
            }
        }

        currentSalvagePct = 0;
        if (salvageUnit.plus(salvageEmployer).isPositive()) {
            currentSalvagePct = salvageUnit.multipliedBy(100)
                    .dividedBy(salvageUnit.plus(salvageEmployer)).getAmount().intValue();
        }

        for (int i = 0; i < salvageBoxes.size(); i++) {
            // Skip the escaping units
            if (escapeBoxes.get(i).isSelected()) {
                continue;
            }

            // always eligible with 100% salvage rights even when current == max
            if ((currentSalvagePct > maxSalvagePct) && (maxSalvagePct < 100)) {
                if (!salvageBoxes.get(i).isSelected()) {
                    salvageBoxes.get(i).setEnabled(false);
                }

                if (!soldUnitBoxes.get(i).isSelected()) {
                    soldUnitBoxes.get(i).setEnabled(false);
                }
            }
        }
        lblSalvageValueUnit2.setText(salvageUnit.toAmountAndSymbolString());
        lblSalvageValueEmployer2.setText(salvageEmployer.toAmountAndSymbolString());

        String salvageUsed = "<html>" + ((currentSalvagePct <= maxSalvagePct) ? "" : ReportingUtilities.spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor())) + currentSalvagePct + '%' + ((currentSalvagePct <= maxSalvagePct) ? "" : ReportingUtilities.CLOSING_SPAN_TAG) + "<span>(max " + maxSalvagePct + "%)</span></html>";

        lblSalvagePct2.setText(salvageUsed);
    }


    /**
     * Shows the info for the given unit in a dialog.
     * @param id - UUID of the unit to show
     * @param isSalvage - is this from the salvage page?
     */
    private void showUnit(UUID id, boolean isSalvage) {
        UnitStatus ustatus;
        if (isSalvage) {
            ustatus = tracker.getSalvageStatus().get(id);
        } else {
            ustatus = tracker.getUnitsStatus().get(id);
        }

        if ((ustatus == null) || (ustatus.getEntity() == null)) {
            return;
        }
        new EntityReadoutDialog(frame, true, ustatus.getEntity()).setVisible(true);
    }


    /**
     * Opens the unit dmaage editor for a given unit from the units or salvage panel
     * @param id - UUID of the unit to show
     * @param unitIndex - index into the unit UI elements lists
     * @param isSalvage - is this from the salvage page?
     */
    private void editUnit(UUID id, int unitIndex, boolean isSalvage) {
        UnitStatus ustatus = (isSalvage ? tracker.getSalvageStatus() : tracker.getUnitsStatus()).get(id);
        if ((ustatus == null) || (ustatus.getEntity() == null)) {
            return;
        }

        UnitEditorDialog med = new UnitEditorDialog(frame, ustatus.getEntity());
        med.setVisible(true);
        ustatus.getUnit().runDiagnostic(false);

        if (isSalvage) {
            salvageUnitLabel.get(unitIndex).setText(ustatus.getDesc(true));
            checkSalvageRights();
        } else {
            lblsUnitName.get(unitIndex).setText(ustatus.getDesc());
        }
    }


    /**
     * Shows a person from the pilot or prisoner list in a dialog
     * @param status - the record to show
     * @param isPrisoner - are they a prisoner?
     */
    private void showPerson(PersonStatus status, boolean isPrisoner) {
        if (status == null) {
            return;
        }

        Person person = isPrisoner ? ((OppositionPersonnelStatus) status).getPerson()
                : tracker.getCampaign().getPerson(status.getId());
        if (person == null) {
            logger.error("Failed to show person after selecting view personnel for a "
                    + (isPrisoner ? "Prisoner" : "member of the force") +
                    " because the person could not be found.");
            return;
        }
        PersonViewPanel personViewPanel = new PersonViewPanel(person, tracker.getCampaign(),
                tracker.getCampaign().getApp().getCampaigngui());
        final JDialog dialog = new JDialog(frame, isPrisoner ? "Prisoner View" : "Personnel View", true);
        dialog.getContentPane().setLayout(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;

        //scroll panel
        JScrollPane scrollPersonnelView = new JScrollPaneWithSpeed();
        scrollPersonnelView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPersonnelView.setViewportView(personViewPanel);
        dialog.getContentPane().add(scrollPersonnelView, gridBagConstraints);

        //Okay button
        JButton btn = new JButton(Messages.getString("Okay"));
        btn.addActionListener(e -> dialog.setVisible(false));
        dialog.getRootPane().setDefaultButton(btn);
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        dialog.getContentPane().add(btn, gridBagConstraints);

        dialog.setMinimumSize(UIUtil.scaleForGUI(450, 700));
        dialog.setPreferredSize(UIUtil.scaleForGUI(450, 700));
        dialog.validate();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    /**
     * Event handler for when the user clicks on an objective unit checkbox
     * @param objective The objective to check
     * @param label label to update
     */
    private void updateObjectiveDisplay(ScenarioObjective objective, JLabel label) {
        int qualifyingUnitCount = 0;

        for (JCheckBox checkBox : objectiveCheckboxes.get(objective)) {
            if (checkBox.isSelected()) {
                qualifyingUnitCount++;
            }
        }

        label.setForeground(objectiveProcessor.objectiveMet(objective, qualifyingUnitCount)
                ? Color.green.darker() : Color.RED);
    }

    private class CheckTotalListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent evt) {
            int idx = Integer.parseInt(((JCheckBox) evt.getItem()).getName());
            btnsEditUnit.get(idx).setEnabled(!chksTotaled.get(idx).isSelected());
        }
    }

    /**
     * Event handler for a KIA checkbox
     * Manipulates other associated controls
     * @author NickAragua
     */
    private static class CheckBoxKIAListener implements ItemListener {
        private final JSlider slider;
        private final JCheckBox checkbox;

        public CheckBoxKIAListener(JSlider slider, JCheckBox checkBox) {
            this.slider = slider;
            this.checkbox = checkBox;
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            JCheckBox kiaChk = (JCheckBox) e.getSource();
            boolean enable = !kiaChk.isSelected();

            if (slider != null) {
                slider.setEnabled(enable);
            }

            if (checkbox != null) {
                checkbox.setEnabled(enable);
            }
        }
    }

    private class ViewUnitListener implements ActionListener {
        boolean salvage;

        public ViewUnitListener(boolean b) {
            salvage = b;
        }


        @Override
        public void actionPerformed(ActionEvent evt) {
            UUID id = UUID.fromString(evt.getActionCommand());
            showUnit(id, salvage);
        }
    }

    private class EditUnitListener implements ActionListener {
        boolean salvage = false;

        public EditUnitListener(boolean b) {
            salvage = b;
        }

        public EditUnitListener() {
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            UUID id = UUID.fromString(evt.getActionCommand());
            int idx = Integer.parseInt(((JButton) evt.getSource()).getName());
            editUnit(id, idx, salvage);
        }
    }
}
