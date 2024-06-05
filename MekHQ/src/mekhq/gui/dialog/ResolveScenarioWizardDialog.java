/*
 * ResolveScenarioWizardDialog.java
 *
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.GunEmplacement;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.OppositionPersonnelStatus;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.ResolveScenarioTracker.UnitStatus;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.campaign.mission.ScenarioObjectiveProcessor;
import mekhq.campaign.mission.enums.ScenarioStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratcon.StratconRulesManager;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.utilities.MarkdownEditorPanel;
import mekhq.gui.view.PersonViewPanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;

/**
 * @author Taharqa
 */
public class ResolveScenarioWizardDialog extends JDialog {
    //region Variable Declarations
    final static String UNITSPANEL   = "Unit Status";
    final static String PILOTPANEL   = "Pilot Status";
    final static String SALVAGEPANEL = "Claim Salvage";
    final static String PRISONERPANEL= "Captured Personnel Status";
    final static String KILLPANEL    = "Assign Kills";
    final static String REWARDPANEL  = "Scenario Costs & Payouts";
    final static String PREVIEWPANEL = "Preview";
    /* Used by AtB to determine minor contract breaches and bonus rolls */
    final static String OBJECTIVEPANEL    = "Objective Status";

    final static String[] panelOrder = {
            UNITSPANEL, PILOTPANEL, SALVAGEPANEL, PRISONERPANEL, KILLPANEL, REWARDPANEL,
            OBJECTIVEPANEL, PREVIEWPANEL
    };

    private JFrame frame;

    private ResolveScenarioTracker tracker;
    private ScenarioObjectiveProcessor objectiveProcessor;

    private JButton btnNext;
    private JButton btnFinish;
    private JButton btnBack;
    private JTextArea txtInstructions;

    private CardLayout cardLayout;
    private String currentPanel;

    private JScrollPane scrMain;
    private JPanel pnlMain;
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
    private List<JCheckBox> miaBtns = new ArrayList<>();
    private List<JCheckBox> kiaBtns = new ArrayList<>();
    private List<JSlider> hitSliders = new ArrayList<>();
    private List<PersonStatus> pstatuses = new ArrayList<>();

    /*
     * Prisoner status panel components
     */
    private List<JCheckBox> prisonerCapturedBtns = new ArrayList<>();
    private List<JCheckBox> prisonerRansomedBtns = new ArrayList<>();
    private List<JCheckBox> prisonerKiaBtns = new ArrayList<>();
    private List<JSlider> pr_hitSliders = new ArrayList<>();
    private List<OppositionPersonnelStatus> prstatuses = new ArrayList<>();

    //region Salvage Panel Components
    private List<JLabel> salvageUnitLabel;
    private List<JCheckBox> salvageBoxes;
    private List<JCheckBox> soldUnitBoxes;
    private List<JCheckBox> escapeBoxes;
    private List<JButton> btnsSalvageEditUnit;
    private List<Unit> salvageables;

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
    private List<Loot> loots;

    //region Preview Panel components
    private JComboBox<ScenarioStatus> choiceStatus;
    private JScrollPane scrRecoveredUnits;
    private JScrollPane scrRecoveredPilots;
    private JScrollPane scrMissingUnits;
    private JScrollPane scrMissingPilots;
    private JScrollPane scrDeadPilots;
    private JScrollPane scrSalvage;
    private MarkdownEditorPanel txtReport;
    private JTextArea txtRecoveredUnits;
    private JTextArea txtRecoveredPilots;
    private JTextArea txtMissingUnits;
    private JTextArea txtMissingPilots;
    private JTextArea txtDeadPilots;
    private JTextArea txtSalvage;
    private JTextArea txtRewards;
    private JLabel lblStatus;
    //endregion Preview Panel components

    private final transient ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ResolveScenarioWizardDialog",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    public ResolveScenarioWizardDialog(JFrame parent, boolean modal, ResolveScenarioTracker t) {
        super(parent, modal);
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
        currentPanel = UNITSPANEL;
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
        // Initialize Local Variables
        GridBagConstraints gridBagConstraints;
        int gridy = 0;
        int gridx = 0;
        int i = 2;
        int j = 0;

        cardLayout = new CardLayout();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // Adding Escape Mnemonic
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setName("Form");

        getContentPane().setLayout(new BorderLayout());

        setTitle(resourceMap.getString("title"));

        /*
         * Instructions at the top
         */
        txtInstructions = new JTextArea();
        txtInstructions.setText("");
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        txtInstructions.setMinimumSize(new Dimension(590,120));
        txtInstructions.setPreferredSize(new Dimension(590,120));
        getContentPane().add(txtInstructions, BorderLayout.PAGE_START);

        /*
         * Set up main panel with cards
         */
        pnlMain = new JPanel(cardLayout);

        //region Unit Status Panel
        pnlUnitStatus = new JPanel();

        pnlUnitStatus.setLayout(new GridBagLayout());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
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

        for (Unit unit : tracker.getUnits()) {
            UnitStatus status = tracker.getUnitsStatus().get(unit.getId());
            ustatuses.add(status);
            nameLbl = new JLabel(status.getDesc());
            lblsUnitName.add(nameLbl);

            chkTotaled = new JCheckBox("");
            chkTotaled.setName("chkTotaled");
            chkTotaled.getAccessibleContext().setAccessibleName(resourceMap.getString("totaled"));
            chkTotaled.setSelected(status.isTotalLoss());
            chkTotaled.setName(Integer.toString(j));
            chkTotaled.setActionCommand(unit.getId().toString());
            chkTotaled.addItemListener(new CheckTotalListener());
            chksTotaled.add(chkTotaled);

            btnViewUnit = new JButton("View Unit");
            btnViewUnit.setActionCommand(unit.getId().toString());
            btnViewUnit.addActionListener(new ViewUnitListener(false));

            btnEditUnit = new JButton("Edit Unit");
            btnEditUnit.setEnabled(!status.isTotalLoss());
            btnEditUnit.setActionCommand(unit.getId().toString());
            btnEditUnit.setName(Integer.toString(j));
            btnEditUnit.addActionListener(new EditUnitListener());
            btnsEditUnit.add(btnEditUnit);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (++j == tracker.getUnits().size()) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlUnitStatus.add(nameLbl, gridBagConstraints);
            gridBagConstraints.gridx = 1;
            pnlUnitStatus.add(chkTotaled, gridBagConstraints);
            gridBagConstraints.gridx = 2;
            pnlUnitStatus.add(btnViewUnit, gridBagConstraints);
            gridBagConstraints.gridx = 3;
            gridBagConstraints.weightx = 1.0;
            pnlUnitStatus.add(btnEditUnit, gridBagConstraints);
            i++;
        }
        pnlMain.add(pnlUnitStatus, UNITSPANEL);
        //endregion Unit Status Panel

        //region Objective Status Panel
        generateObjectiveStatusPanel();
        pnlMain.add(pnlObjectiveStatus, OBJECTIVEPANEL);
        //endregion Objective Status Panel

        //region Pilot Status Panel
        pnlPilotStatus = new JPanel();
        pnlPilotStatus.setLayout(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
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

        j = 0;
        gridy = 2;

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0") );
        labelTable.put(1, new JLabel("1") );
        labelTable.put(2, new JLabel("2") );
        labelTable.put(3, new JLabel("3") );
        labelTable.put(4, new JLabel("4") );
        labelTable.put(5, new JLabel("5") );

        for (PersonStatus status : tracker.getSortedPeople()) {
            pstatuses.add(status);

            gridx = 0;

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (++j == tracker.getPeopleStatus().keySet().size()) {
                gridBagConstraints.weighty = 1.0;
            }

            nameLbl = new JLabel("<html>" + status.getName() + "<br><i> " + status.getUnitName() + "</i></html>");
            pnlPilotStatus.add(nameLbl, gridBagConstraints);

            JSlider hitSlider = new JSlider(JSlider.HORIZONTAL, 0, 5, Math.min(status.getHits(), 5));
            hitSlider.setName(Integer.toString(j - 1));
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
            gridBagConstraints.weightx = 1.0;
            pnlPilotStatus.add(btnViewPilot, gridBagConstraints);
        }
        pnlMain.add(pnlPilotStatus, PILOTPANEL);
        //endregion Pilot Status Panel

        //region Prisoner Status Panel
        pnlPrisonerStatus = new JPanel();
        pnlPrisonerStatus.setLayout(new GridBagLayout());

        gridx = 1;

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

        j = 0;
        gridy = 2;

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
            if (++j == tracker.getOppositionPersonnel().keySet().size()) {
                gridBagConstraints.weighty = 1.0;
            }

            nameLbl = new JLabel("<html>" + status.getName() + "<br><i> " + status.getUnitName() + "</i></html>");
            pnlPrisonerStatus.add(nameLbl, gridBagConstraints);

            JSlider hitSlider = new JSlider(JSlider.HORIZONTAL, 0, 5, Math.min(status.getHits(), 5));
            hitSlider.setName(Integer.toString(j - 1));
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
            gridBagConstraints.weightx = 1.0;
            pnlPrisonerStatus.add(btnViewPrisoner, gridBagConstraints);

            // if the person is dead, set the checkbox and skip all this captured stuff
            if ((status.getHits() > 5) || status.isDead()) {
                kiaCheck.setSelected(true);
            } else if (status.isCaptured()
                    && tracker.getCampaign().getCampaignOptions().getPrisonerCaptureStyle().isAtB()) {
                boolean wasCaptured = false;
                if (status.wasPickedUp()) {
                    wasCaptured = true;
                } else {
                    for (int n = 0; n < status.getHits() + 1; n++) {
                        if (Utilities.dice(1, 6) == 1) {
                            wasCaptured = true;
                            break;
                        }
                    }
                }
                prisonerCapturedCheck.setSelected(wasCaptured);
            }
        }
        pnlMain.add(pnlPrisonerStatus, PRISONERPANEL);
        //endregion Prisoner Status Panel

        //region Salvage Panel
        // Create the panel
        pnlSalvage = new JPanel();
        pnlSalvage.setLayout(new GridBagLayout());
        JPanel pnlSalvageValue = new JPanel(new GridBagLayout());

        gridx = 0;
        gridy = 0;
        if ((tracker.getMission() instanceof Contract) && !tracker.usesSalvageExchange()) {
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);

            JLabel lblSalvageValueUnit1 = new JLabel(resourceMap.getString("lblSalvageValueUnit1.text"));
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            gridBagConstraints.weightx = 0.0;
            pnlSalvageValue.add(lblSalvageValueUnit1, gridBagConstraints);

            lblSalvageValueUnit2 = new JLabel(salvageUnit.toAmountAndSymbolString());
            gridBagConstraints.gridx = gridx--;
            gridBagConstraints.weightx = 1.0;
            pnlSalvageValue.add(lblSalvageValueUnit2, gridBagConstraints);

            JLabel lblSalvageValueEmployer1 = new JLabel(resourceMap.getString("lblSalvageValueEmployer1.text"));
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            gridBagConstraints.weightx = 0.0;
            pnlSalvageValue.add(lblSalvageValueEmployer1, gridBagConstraints);

            lblSalvageValueEmployer2 = new JLabel(salvageEmployer.toAmountAndSymbolString());
            gridBagConstraints.gridx = gridx--;
            gridBagConstraints.weightx = 1.0;
            pnlSalvageValue.add(lblSalvageValueEmployer2, gridBagConstraints);

            JLabel lblSalvagePct1 = new JLabel(resourceMap.getString("lblSalvagePct1.text"));
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.gridy = gridy++;
            gridBagConstraints.weightx = 0.0;
            pnlSalvageValue.add(lblSalvagePct1, gridBagConstraints);

            String lead = "<html><font" + ((currentSalvagePct > maxSalvagePct) ? " color='red'" : "") + ">";
            lblSalvagePct2 = new JLabel(lead + currentSalvagePct + "%</font> <span>(max " + maxSalvagePct + "%)</span></html>");
            gridBagConstraints.gridx = gridx--;
            gridBagConstraints.weightx = 1.0;
            pnlSalvageValue.add(lblSalvagePct2, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.gridwidth = 4;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(0, 0, 20, 0);
            pnlSalvage.add(pnlSalvageValue, gridBagConstraints);
        }

        // Update any indexing variables
        j = 0;
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

        for (TestUnit u : tracker.getPotentialSalvage()) {
            // Initial variable work
            gridx = 0;
            salvageables.add(u);
            UnitStatus status = tracker.getSalvageStatus().get(u.getId());

            // Initial update to the GridBagConstraints
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.gridy = gridy++;
            gridBagConstraints.weightx = 0.0;
            if ((j + 1) == tracker.getPotentialSalvage().size()) { // we only want the weight change on the last one
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
            escaped.setEnabled(!(u.getEntity().isDestroyed() || u.getEntity().isDoomed()));
            escaped.setSelected(!status.isLikelyCaptured());
            escaped.addItemListener(evt -> checkSalvageRights());
            escaped.setActionCommand(u.getEntity().getExternalIdAsString());
            escapeBoxes.add(escaped);
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(escaped, gridBagConstraints);

            JButton btnSalvageViewUnit = new JButton("View Unit");
            btnSalvageViewUnit.setActionCommand(u.getId().toString());
            btnSalvageViewUnit.addActionListener(new ViewUnitListener(true));
            gridBagConstraints.gridx = gridx++;
            pnlSalvage.add(btnSalvageViewUnit, gridBagConstraints);

            JButton btnSalvageEditUnit = new JButton("Edit Unit");
            btnSalvageEditUnit.setName(Integer.toString(j++));
            btnSalvageEditUnit.setActionCommand(u.getId().toString());
            btnSalvageEditUnit.addActionListener(new EditUnitListener(true));
            btnsSalvageEditUnit.add(btnSalvageEditUnit);
            gridBagConstraints.gridx = gridx++;
            gridBagConstraints.weightx = 1.0;
            pnlSalvage.add(btnSalvageEditUnit, gridBagConstraints);
        }

        checkSalvageRights();
        pnlMain.add(pnlSalvage, SALVAGEPANEL);
        //endregion Salvage Panel

        /*
         * Assign Kills panel
         */
        pnlKills = new JPanel();
        killChoices = new Hashtable<>();
        pnlKills.setLayout(new GridBagLayout());

        gridBagConstraints = new GridBagConstraints();
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

        i = 2;
        JComboBox<String> comboAssign;
        DefaultComboBoxModel<String> assignModel;
        j = 0;
        for (String killName : tracker.getKillCredits().keySet()) {
            j++;
            nameLbl = new JLabel(killName);
            assignModel = new DefaultComboBoxModel<>();
            assignModel.addElement(resourceMap.getString("none"));
            int idx = 0;
            int selected = 0;
            if (null == tracker.getKillCredits().get(killName)) {
                continue;
            }
            for (Unit u : tracker.getUnits()) {
                idx++;
                if (u.getEntity() instanceof GunEmplacement) {
                    assignModel.addElement("AutoTurret, " + u.getName());
                } else {
                    assignModel.addElement(u.getCommander().getFullTitle() + ", " + u.getName());
                }

                if (u.getId().toString().equals(tracker.getKillCredits().get(killName))) {
                    selected = idx;
                }
            }
            comboAssign = new JComboBox<>(assignModel);
            comboAssign.setSelectedIndex(selected);
            killChoices.put(killName, comboAssign);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            gridBagConstraints.weightx = 0.0;
            if (j == tracker.getKillCredits().keySet().size()) {
                gridBagConstraints.weighty = 1.0;
            }
            pnlKills.add(nameLbl, gridBagConstraints);
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.gridx = 1;
            pnlKills.add(comboAssign, gridBagConstraints);
            i++;
        }
        pnlMain.add(pnlKills, KILLPANEL);

        /*
         * Collect Rewards Panel
         */
        pnlRewards = new JPanel();
        pnlRewards.setLayout(new GridBagLayout());
        lootBoxes = new ArrayList<>();
        i = 0;
        j = 0;
        for (Loot loot : loots) {
            j++;
            JCheckBox box = new JCheckBox(loot.getShortDescription());
            box.setSelected(false);
            lootBoxes.add(box);
            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = i;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
            gridBagConstraints.weightx = 1.0;
            if (j == (loots.size())) {
                gridBagConstraints.weighty = 1.0;
            }
            gridBagConstraints.insets = new Insets(5, 5, 0, 0);
            pnlRewards.add(box, gridBagConstraints);
            i++;
        }
        pnlMain.add(pnlRewards, REWARDPANEL);

        /*
         * Preview Panel
         */
        pnlPreview = new JPanel();
        choiceStatus = new JComboBox<>();
        scrRecoveredUnits = new JScrollPane();
        scrRecoveredPilots = new JScrollPane();
        scrMissingUnits = new JScrollPane();
        scrMissingPilots = new JScrollPane();
        scrDeadPilots = new JScrollPane();
        scrSalvage = new JScrollPane();
        txtInstructions = new JTextArea();
        txtReport = new MarkdownEditorPanel("After-Action Report");
        txtRecoveredUnits = new JTextArea();
        txtRecoveredPilots = new JTextArea();
        txtMissingUnits = new JTextArea();
        txtMissingPilots = new JTextArea();
        txtDeadPilots = new JTextArea();
        txtSalvage = new JTextArea();
        txtRewards = new JTextArea();
        lblStatus = new JLabel();

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
        gridBagConstraints = new GridBagConstraints();
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
        pnlPreview.add(new JScrollPane(txtRewards), gridBagConstraints);

        txtReport.setText("");
        txtReport.setPreferredSize(new Dimension(500, 300));
        txtReport.setMinimumSize(new Dimension(500, 300));
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
        scrRecoveredUnits.setViewportView(txtRecoveredUnits);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(scrRecoveredUnits, gridBagConstraints);

        txtRecoveredPilots.setName("txtRecoveredPilots");
        txtRecoveredPilots.setText(resourceMap.getString("none"));
        txtRecoveredPilots.setEditable(false);
        txtRecoveredPilots.setLineWrap(true);
        txtRecoveredPilots.setWrapStyleWord(true);
        txtRecoveredPilots.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtRecoveredPilots.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrRecoveredPilots.setViewportView(txtRecoveredPilots);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(scrRecoveredPilots, gridBagConstraints);

        txtMissingUnits.setName("txtMissingUnits");
        txtMissingUnits.setText(resourceMap.getString("none"));
        txtMissingUnits.setEditable(false);
        txtMissingUnits.setLineWrap(true);
        txtMissingUnits.setWrapStyleWord(true);
        txtMissingUnits.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtMissingUnits.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrMissingUnits.setViewportView(txtMissingUnits);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(scrMissingUnits, gridBagConstraints);

        txtMissingPilots.setName("txtMissingPilots");
        txtMissingPilots.setText(resourceMap.getString("none"));
        txtMissingPilots.setEditable(false);
        txtMissingPilots.setLineWrap(true);
        txtMissingPilots.setWrapStyleWord(true);
        txtMissingPilots.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtMissingPilots.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrMissingPilots.setViewportView(txtMissingPilots);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(scrMissingPilots, gridBagConstraints);

        txtSalvage.setName("txtSalvage");
        txtSalvage.setText("None");
        txtSalvage.setEditable(false);
        txtSalvage.setLineWrap(true);
        txtSalvage.setWrapStyleWord(true);
        txtSalvage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtSalvagedUnits.title")),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        scrSalvage.setViewportView(txtSalvage);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(scrSalvage, gridBagConstraints);

        txtDeadPilots.setName("txtDeadPilots");
        txtDeadPilots.setText(resourceMap.getString("none"));
        txtDeadPilots.setEditable(false);
        txtDeadPilots.setLineWrap(true);
        txtDeadPilots.setWrapStyleWord(true);
        txtDeadPilots.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtDeadPilots.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        scrDeadPilots.setViewportView(txtDeadPilots);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
        pnlPreview.add(scrDeadPilots, gridBagConstraints);
        pnlMain.add(pnlPreview, PREVIEWPANEL);


        scrMain = new JScrollPane(pnlMain);
        scrMain.setMinimumSize(new Dimension(600, 500));
        scrMain.setPreferredSize(new Dimension(600, 500));
        getContentPane().add(scrMain, BorderLayout.CENTER);


        /*
         * Set up button panel
         */
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
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        gridBagConstraints.insets = new Insets(5, 5, 0, 0);
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

        getContentPane().add(panButtons, BorderLayout.PAGE_END);

        switchPanel(currentPanel);
        if (!usePanel(currentPanel)) {
            next();
        }

        btnNext.setEnabled(true);
        btnBack.setEnabled(false);
        btnFinish.setEnabled(false);

        pack();
    }

    private void generateObjectiveStatusPanel() {
        pnlObjectiveStatus = new JPanel();
        if (usePanel(OBJECTIVEPANEL)) {
            pnlObjectiveStatus.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
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

                gbc.gridy++;
                updateObjectiveDisplay(objective, lblObjective);
            }
        }
    }

    @Deprecated // These need to be migrated to the Suite Constants / Suite Options Setup
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(ResolveScenarioWizardDialog.class);
            this.setName("dialog");
            preferences.manage(new JWindowPreference(this));
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to set user preferences", ex);
        }
    }

    private void switchPanel(String name) {
        if (name.equals(PREVIEWPANEL)) {
            updatePreviewPanel();
        }
        currentPanel = name;
        setTitle(currentPanel);
        cardLayout.show(pnlMain, currentPanel);
        switchInstructions();

        switch (name) {
            case PILOTPANEL:
                pnlMain.setPreferredSize(pnlPilotStatus.getLayout().preferredLayoutSize(pnlMain));
                break;
            case UNITSPANEL:
                pnlMain.setPreferredSize(pnlUnitStatus.getLayout().preferredLayoutSize(pnlMain));
                break;
            case SALVAGEPANEL:
                pnlMain.setPreferredSize(pnlSalvage.getLayout().preferredLayoutSize(pnlMain));
                break;
            case KILLPANEL:
                pnlMain.setPreferredSize(pnlKills.getLayout().preferredLayoutSize(pnlMain));
                break;
            case REWARDPANEL:
                pnlMain.setPreferredSize(pnlRewards.getLayout().preferredLayoutSize(pnlMain));
                break;
            case PREVIEWPANEL:
                pnlMain.setPreferredSize(pnlPreview.getLayout().preferredLayoutSize(pnlMain));
                break;
            case OBJECTIVEPANEL:
                pnlMain.setPreferredSize(pnlObjectiveStatus.getLayout().preferredLayoutSize(pnlMain));
                break;
            case PRISONERPANEL:
                pnlMain.setPreferredSize(pnlPrisonerStatus.getLayout().preferredLayoutSize(pnlMain));
                break;
        }


        SwingUtilities.invokeLater(() -> scrMain.getVerticalScrollBar().setValue(0));
        pack();
    }

    private void switchInstructions() {
        switch (currentPanel) {
            case UNITSPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.missingunits"));
                break;
            case PILOTPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.personnel"));
                break;
            case SALVAGEPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.salvage"));
                break;
            case KILLPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.kills"));
                break;
            case PRISONERPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.prisoners"));
                break;
            case PREVIEWPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.preview"));
                break;
            case REWARDPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.reward"));
                break;
            case OBJECTIVEPANEL:
                txtInstructions.setText(resourceMap.getString("txtInstructions.text.objectives"));
                break;
            default:
                txtInstructions.setText("");
                break;
        }
        txtInstructions.setEditable(false);
        txtInstructions.setLineWrap(true);
        txtInstructions.setWrapStyleWord(true);
        txtInstructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(resourceMap.getString("txtInstructions.title")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        txtInstructions.setMinimumSize(new Dimension(590, 150));
        txtInstructions.setPreferredSize(new Dimension(590, 150));
        getContentPane().add(txtInstructions, BorderLayout.PAGE_START);
    }

    private void next() {
        btnNext.setEnabled(false);
        btnBack.setEnabled(true);
        btnFinish.setEnabled(false);
        boolean passedCurrent = false;
        boolean switchMade = false;

        // if we're done with the units panel, update the objectives state based on selection status
        // and damaged editing results
        if (currentPanel.equals(ResolveScenarioWizardDialog.UNITSPANEL)) {
            for (JCheckBox box : chksTotaled) {
                UUID id = UUID.fromString(box.getActionCommand());
                objectiveProcessor.updateObjectiveEntityState(tracker.getAllInvolvedUnits().get(id),
                        false, box.isSelected(), !tracker.playerHasBattlefieldControl());
            }
        }

        // if we're done with the salvage panel, update the objectives state based on selection status
        // and damaged editing results
        if (currentPanel.equals(ResolveScenarioWizardDialog.SALVAGEPANEL)) {
            for (JCheckBox box : escapeBoxes) {
                UUID id = UUID.fromString(box.getActionCommand());
                objectiveProcessor.updateObjectiveEntityState(tracker.getAllInvolvedUnits().get(id),
                        box.isSelected(), false, tracker.playerHasBattlefieldControl());
            }
        }

        // now update the objective panel if we updated objective state
        if (usePanel(ResolveScenarioWizardDialog.OBJECTIVEPANEL)) {
            if (currentPanel.equals(ResolveScenarioWizardDialog.UNITSPANEL) ||
                    currentPanel.equals(ResolveScenarioWizardDialog.SALVAGEPANEL)) {
                for (ScenarioObjective objective : objectiveCheckboxes.keySet()) {
                    for (JCheckBox checkBox : objectiveCheckboxes.get(objective)) {
                        checkBox.setSelected(objectiveProcessor.getQualifyingObjectiveUnits()
                                .get(objective).contains(checkBox.getActionCommand()));
                    }
                }
            }
        }

        for (String name : panelOrder) {
            if (passedCurrent) {
                if (usePanel(name)) {
                    if (!switchMade) {
                        switchPanel(name);
                        switchMade = true;
                        if (name.equals(PREVIEWPANEL)) {
                            btnFinish.setEnabled(true);
                        }
                    } else {
                        btnNext.setEnabled(true);
                        break;
                    }
                }
            } else if (name.equals(currentPanel)) {
                passedCurrent = true;
            }
        }
    }

    private void back() {
        btnNext.setEnabled(true);
        btnBack.setEnabled(false);
        btnFinish.setEnabled(false);
        boolean passedCurrent = false;
        boolean switchMade = false;
        for (int i = (panelOrder.length - 1); i >= 0; i--) {
            String name = panelOrder[i];
            if (passedCurrent) {
                if (usePanel(name)) {
                    if (!switchMade) {
                        switchPanel(name);
                        switchMade = true;
                    } else {
                        btnBack.setEnabled(true);
                        break;
                    }
                }
            } else if (name.equals(currentPanel)) {
                passedCurrent = true;
            }
        }
    }

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
        for (int i = 0; i < lootBoxes.size(); i++) {
            JCheckBox box = lootBoxes.get(i);
            if (box.isSelected()) {
                tracker.addLoot(loots.get(i));
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

                objectiveProcessor.processObjective(objective, qualifyingUnitCount, override, tracker, false);
            }
        }

        StratconRulesManager.processScenarioCompletion(tracker);

        this.setVisible(false);
    }

    private void cancel() {
        setVisible(false);
    }

    private boolean usePanel(String panelName) {
        switch (panelName) {
            case UNITSPANEL:
                return !tracker.getUnitsStatus().keySet().isEmpty();
            case OBJECTIVEPANEL:
                return tracker.getScenario().hasObjectives();
            case PILOTPANEL:
                return !tracker.getPeopleStatus().keySet().isEmpty();
            case PRISONERPANEL:
                return !tracker.getOppositionPersonnel().keySet().isEmpty();
            case SALVAGEPANEL:
                return !tracker.getPotentialSalvage().isEmpty()
                        && (!(tracker.getMission() instanceof Contract) || ((Contract) tracker.getMission()).canSalvage());
            case KILLPANEL:
                return !tracker.getKillCredits().isEmpty();
            case REWARDPANEL:
                return !loots.isEmpty();
            case PREVIEWPANEL:
                return true;
            default:
                return false;
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

        String lead = (currentSalvagePct <= maxSalvagePct) ? "<html><font>" : "<html><font color='red'>";

        lblSalvagePct2.setText(lead + currentSalvagePct + "%</font> <span>(max " + maxSalvagePct + "%)</span></html>");
    }

    private void updatePreviewPanel() {
        // set victory/defeat status based on scenario objectives
        ScenarioStatus scenarioStatus = objectiveProcessor.determineScenarioStatus(tracker.getScenario(),
                getObjectiveOverridesStatus(), getObjectiveUnitCounts());
        choiceStatus.setSelectedItem(scenarioStatus);

        // do a "dry run" of the scenario objectives to output a report
        StringBuilder sb = new StringBuilder();

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

                sb.append(objectiveProcessor.processObjective(objective, qualifyingUnitCount, override, tracker, true));
                sb.append("\n");
            }

            txtReport.setText(sb.toString());
        }

        //pilots first
        StringBuilder missingNames = new StringBuilder();
        StringBuilder kiaNames = new StringBuilder();
        StringBuilder recoverNames = new StringBuilder();
        for (int i = 0; i < pstatuses.size(); i++) {
            PersonStatus status = pstatuses.get(i);
            if (hitSliders.get(i).getValue() >= 6 || kiaBtns.get(i).isSelected()) {
                kiaNames.append(status.getName()).append("\n");
            } else if (miaBtns.get(i).isSelected()) {
                missingNames.append(status.getName()).append("\n");
            } else {
                recoverNames.append(status.getName()).append("\n");
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
                missUnits.append(name).append("\n");
            } else {
                recoverUnits.append(name).append("\n");
            }
        }
        txtRecoveredUnits.setText(recoverUnits.toString());
        txtMissingUnits.setText(missUnits.toString());

        //now salvage
        StringBuilder salvageUnits = new StringBuilder();
        for (int i = 0; i < salvageBoxes.size(); i++) {
            JCheckBox box = salvageBoxes.get(i);
            if (box.isSelected()) {
                salvageUnits.append(salvageables.get(i).getName()).append("\n");
            }
        }
        txtSalvage.setText(salvageUnits.toString());

        //now rewards
        StringBuilder claimed = new StringBuilder();
        for (int i = 0; i < lootBoxes.size(); i++) {
            JCheckBox box = lootBoxes.get(i);
            if (box.isSelected()) {
                claimed.append(loots.get(i).getShortDescription()).append("\n");
            }
        }
        txtRewards.setText(claimed.toString());
    }

    private void showUnit(UUID id, boolean salvage) {
        // TODO : I am not sure I like the pop up dialog, might just make this a view on this
        // TODO : dialog
        UnitStatus ustatus;
        if (salvage) {
            ustatus = tracker.getSalvageStatus().get(id);
        } else {
            ustatus = tracker.getUnitsStatus().get(id);
        }

        if ((ustatus == null) || (ustatus.getEntity() == null)) {
            return;
        }
        new EntityReadoutDialog(frame, true, ustatus.getEntity()).setVisible(true);
    }

    private void editUnit(UUID id, int idx, boolean salvage) {
        UnitStatus ustatus = (salvage ? tracker.getSalvageStatus() : tracker.getUnitsStatus()).get(id);
        if ((ustatus == null) || (ustatus.getEntity() == null)) {
            return;
        }

        UnitEditorDialog med = new UnitEditorDialog(frame, ustatus.getEntity());
        med.setVisible(true);
        ustatus.getUnit().runDiagnostic(false);

        if (salvage) {
            salvageUnitLabel.get(idx).setText(ustatus.getDesc(true));
            checkSalvageRights();
        } else {
            lblsUnitName.get(idx).setText(ustatus.getDesc());
        }
    }

    private void showPerson(PersonStatus status, boolean isPrisoner) {
        if (status == null) {
            return;
        }

        Person person = isPrisoner ? ((OppositionPersonnelStatus) status).getPerson()
                : tracker.getCampaign().getPerson(status.getId());
        if (person == null) {
            LogManager.getLogger().error("Failed to show person after selecting view personnel for a "
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
        JScrollPane scrollPersonnelView = new JScrollPane();
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

        dialog.setMinimumSize(new Dimension(600, 300));
        dialog.setPreferredSize(new Dimension(600, 300));
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
        private JSlider slider;
        private JCheckBox checkbox;

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
