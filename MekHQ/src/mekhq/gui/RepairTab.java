/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import megamek.client.ui.entityreadout.EntityReadout;
import megamek.client.ui.models.XTableColumnModel;
import megamek.client.ui.preferences.JTablePreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.ViewFormatting;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.events.AcquisitionEvent;
import mekhq.campaign.events.AsTechPoolChangedEvent;
import mekhq.campaign.events.DeploymentChangedEvent;
import mekhq.campaign.events.OvertimeModeEvent;
import mekhq.campaign.events.ProcurementEvent;
import mekhq.campaign.events.RepairStatusChangedEvent;
import mekhq.campaign.events.StratConDeploymentEvent;
import mekhq.campaign.events.parts.PartEvent;
import mekhq.campaign.events.parts.PartWorkEvent;
import mekhq.campaign.events.persons.PersonEvent;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.events.units.UnitEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.adapter.ServicedUnitsTableMouseAdapter;
import mekhq.gui.adapter.TaskTableMouseAdapter;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.baseComponents.roundedComponents.RoundedMMToggleButton;
import mekhq.gui.dialog.AcquisitionsDialog;
import mekhq.gui.dialog.MRMSDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.TaskTableModel;
import mekhq.gui.model.TechTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.sorter.TaskSorter;
import mekhq.gui.sorter.TechSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.service.PartsAcquisitionService;
import mekhq.service.enums.MRMSMode;
import mekhq.service.mrms.MRMSService;

/**
 * Shows damaged units and controls for repair.
 */
public final class RepairTab extends CampaignGuiTab implements ITechWorkPanel {
    private static final MMLogger logger = MMLogger.create(RepairTab.class);

    private JPanel panDoTask;
    private JPanel panDoTaskText;
    private JSplitPane splitServicedUnits;
    private JTable servicedUnitTable;
    private JTable taskTable;
    private JTable techTable;
    private RoundedJButton btnDoTask;
    private RoundedMMToggleButton btnShowAllTechs;
    private JScrollPane scrTextTarget;
    private JLabel lblTargetNum;
    private JTextPane txtServicedUnitView;
    private JTextArea textTarget;
    private JTextPane txtResult;
    private JLabel astechPoolLabel;
    private JComboBox<String> choiceLocation;
    private RoundedJButton btnAcquisitions;
    private JScrollPane scrollServicedUnitView;

    private UnitTableModel servicedUnitModel;
    private TaskTableModel taskModel;
    private TechTableModel techsModel;

    private TableRowSorter<UnitTableModel> servicedUnitSorter;
    private TableRowSorter<TaskTableModel> taskSorter;
    private TableRowSorter<TechTableModel> techSorter;

    // Maintain selections after refresh
    private int selectedRow = -1;
    private int selectedLocation = -1;
    private Unit selectedUnit = null;
    private Person selectedTech = getSelectedTech();
    private boolean ignoreUnitTable = false; // Used to disable selection listener while data is updated.

    // region Constructors
    public RepairTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
        setUserPreferences();
    }
    // endregion Constructors

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
              MekHQ.getMHQOptions().getLocale());
        GridBagConstraints gridBagConstraints;

        setLayout(new BorderLayout());

        JPanel panServicedUnits = new JPanel(new GridBagLayout());

        // Add panel for action buttons
        JPanel actionButtons = new JPanel(new GridBagLayout());

        RoundedJButton btnMRMSDialog = new RoundedJButton("Mass Repair/Salvage");
        btnMRMSDialog.setToolTipText("Start Mass Repair/Salvage from dialog");
        btnMRMSDialog.setName("btnMRMSDialog");
        btnMRMSDialog.addActionListener(evt -> {
            new MRMSDialog(getFrame(), true, getCampaignGui(), null, MRMSMode.UNITS).setVisible(true);
        });

        RoundedJButton btnMRMSInstantAll = new RoundedJButton("Instant Mass Repair/Salvage All");
        btnMRMSInstantAll.setToolTipText(
              "Perform Mass Repair/Salvage immediately on all units using active configuration");
        btnMRMSInstantAll.setName("btnMRMSInstantAll");
        btnMRMSInstantAll.addActionListener(evt -> {
            MRMSService.mrmsAllUnits(getCampaign());
            JOptionPane.showMessageDialog(getCampaignGui().getFrame(),
                  "Mass Repair/Salvage complete.",
                  "Complete",
                  JOptionPane.INFORMATION_MESSAGE);
        });

        btnAcquisitions = new RoundedJButton("Parts");
        btnAcquisitions.setToolTipText("Show missing/in transit/on order parts");
        btnAcquisitions.setName("btnAcquisitions");
        btnAcquisitions.addActionListener(evt -> {
            AcquisitionsDialog dlg = new AcquisitionsDialog(getFrame(), true, getCampaignGui());
            dlg.setVisible(true);
        });
        btnAcquisitions.addPropertyChangeListener("counts", evt -> {
            String txt = "Parts Acquisition";

            if (PartsAcquisitionService.getMissingCount() > 0) {
                if (PartsAcquisitionService.getUnavailableCount() > 0) {
                    txt += String.format(" (%s missing, %s unavailable)",
                          PartsAcquisitionService.getMissingCount(),
                          PartsAcquisitionService.getUnavailableCount());
                } else {
                    txt += String.format(" (%s missing)", PartsAcquisitionService.getMissingCount());
                }
            }
            btnAcquisitions.setText(txt);
            btnAcquisitions.repaint();
        });

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        actionButtons.add(btnMRMSDialog, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        actionButtons.add(btnMRMSInstantAll, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1;
        gridBagConstraints.weighty = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        actionButtons.add(btnAcquisitions, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 0, 5, 0);
        panServicedUnits.add(actionButtons, gridBagConstraints);

        servicedUnitModel = new UnitTableModel(getCampaign());
        servicedUnitTable = new JTable(servicedUnitModel);
        servicedUnitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        servicedUnitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        servicedUnitTable.setColumnModel(new XTableColumnModel());
        servicedUnitTable.createDefaultColumnsFromModel();
        servicedUnitSorter = new TableRowSorter<>(servicedUnitModel);
        servicedUnitSorter.setComparator(UnitTableModel.COL_STATUS, new UnitStatusSorter());
        servicedUnitSorter.setComparator(UnitTableModel.COL_TYPE, new UnitTypeSorter());
        servicedUnitTable.setRowSorter(servicedUnitSorter);
        ArrayList<SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new SortKey(UnitTableModel.COL_TYPE, SortOrder.DESCENDING));
        servicedUnitSorter.setSortKeys(sortKeys);
        TableColumn column;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = ((XTableColumnModel) servicedUnitTable.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(servicedUnitModel.getColumnWidth(i));
            column.setCellRenderer(servicedUnitModel.getRenderer(false));
            if ((i != UnitTableModel.COL_NAME) &&
                      (i != UnitTableModel.COL_TYPE) &&
                      (i != UnitTableModel.COL_STATUS) &&
                      (i != UnitTableModel.COL_REPAIR) &&
                      (i != UnitTableModel.COL_SITE) &&
                      (i != UnitTableModel.COL_RSTATUS)) {
                ((XTableColumnModel) servicedUnitTable.getColumnModel()).setColumnVisible(column, false);
            }
        }
        servicedUnitTable.setIntercellSpacing(new Dimension(0, 0));
        servicedUnitTable.setShowGrid(false);
        servicedUnitTable.getSelectionModel().addListSelectionListener(this::servicedUnitTableValueChanged);
        ServicedUnitsTableMouseAdapter.connect(getCampaignGui(), servicedUnitTable, servicedUnitModel);
        JScrollPane scrollServicedUnitTable = new JScrollPaneWithSpeed(servicedUnitTable);
        scrollServicedUnitTable.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollServicedUnitTable.setMinimumSize(new Dimension(350, 200));
        scrollServicedUnitTable.setPreferredSize(new Dimension(350, 200));

        txtServicedUnitView = new JTextPane();
        txtServicedUnitView.setEditable(false);
        txtServicedUnitView.setContentType("text/html");
        scrollServicedUnitView = new JScrollPaneWithSpeed(txtServicedUnitView);
        scrollServicedUnitView.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollServicedUnitView.setMinimumSize(new Dimension(350, 400));
        scrollServicedUnitView.setPreferredSize(new Dimension(350, 400));

        splitServicedUnits = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollServicedUnitTable, scrollServicedUnitView);
        splitServicedUnits.setOneTouchExpandable(true);
        splitServicedUnits.setResizeWeight(0.0);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panServicedUnits.add(splitServicedUnits, gridBagConstraints);

        JPanel panTasks = new JPanel(new GridBagLayout());
        panTasks.setFocusable(false);

        techsModel = new TechTableModel(getCampaignGui(), this);
        techTable = new JTable(techsModel);
        techTable.setRowHeight(UIUtil.scaleForGUI(60));
        techTable.getColumnModel().getColumn(0).setCellRenderer(techsModel.getRenderer());
        techTable.getSelectionModel().addListSelectionListener(this::techTableValueChanged);
        techSorter = new TableRowSorter<>(techsModel);
        techSorter.setComparator(0, new TechSorter());
        techTable.setRowSorter(techSorter);
        sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        techSorter.setSortKeys(sortKeys);
        JScrollPane scrollTechTable = new JScrollPaneWithSpeed(techTable);
        scrollTechTable.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollTechTable.setMinimumSize(new Dimension(200, 200));
        scrollTechTable.setPreferredSize(new Dimension(300, 300));

        panDoTask = new JPanel(new GridBagLayout());
        panDoTask.setMinimumSize(UIUtil.scaleForGUI(100, 100));
        panDoTask.setName("panelDoTask");
        panDoTask.setPreferredSize(UIUtil.scaleForGUI(100, 100));

        btnDoTask = new RoundedJButton(resourceMap.getString("btnDoTask.text"));
        btnDoTask.setToolTipText(resourceMap.getString("btnDoTask.toolTipText"));
        btnDoTask.setEnabled(false);
        btnDoTask.addActionListener(ev -> doTask());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panDoTask.add(btnDoTask, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.SOUTH;
        panDoTask.add(new JLabel(resourceMap.getString("lblTarget.text")), gridBagConstraints);

        lblTargetNum = new JLabel(resourceMap.getString("lblTargetNum.text"));
        lblTargetNum.setHorizontalAlignment(SwingConstants.CENTER);
        lblTargetNum.setName("lblTargetNum");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.NORTH;
        panDoTask.add(lblTargetNum, gridBagConstraints);

        choiceLocation = new JComboBox<>();
        choiceLocation.removeAllItems();
        choiceLocation.addItem("All");
        choiceLocation.setEnabled(false);
        choiceLocation.addActionListener(ev -> filterTasks());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        panDoTask.add(choiceLocation, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        panTasks.add(panDoTask, gridBagConstraints);

        panDoTaskText = new JPanel(new GridBagLayout());
        panDoTaskText.setMinimumSize(new Dimension(150, 100));
        panDoTaskText.setName("panelDoTask");
        panDoTaskText.setPreferredSize(new Dimension(150, 100));

        textTarget = new JTextArea();
        textTarget.setColumns(20);
        textTarget.setEditable(false);
        textTarget.setLineWrap(true);
        textTarget.setRows(5);
        textTarget.setText("");
        textTarget.setWrapStyleWord(true);
        textTarget.setBorder(null);
        textTarget.setName("textTarget");
        scrTextTarget = new JScrollPaneWithSpeed(textTarget);
        scrTextTarget.setBorder(RoundedLineBorder.createRoundedLineBorder());

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        panDoTaskText.add(scrTextTarget, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        panTasks.add(panDoTaskText, gridBagConstraints);

        txtResult = new JTextPane();
        txtResult.addHyperlinkListener(getCampaignGui().getReportHLL());
        txtResult.setContentType("text/html");
        txtResult.setEditable(false);
        DefaultCaret caret = (DefaultCaret) txtResult.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        txtResult.setBorder(new EmptyBorder(2, 5, 2, 2));
        JPanel panResult = new JPanel(new BorderLayout());
        panResult.add(txtResult, BorderLayout.CENTER);
        panResult.setBorder(RoundedLineBorder.createRoundedLineBorder("Last Repair Check"));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(2, 2, 2, 2);
        panTasks.add(panResult, gridBagConstraints);

        taskModel = new TaskTableModel(getCampaignGui(), this);
        taskTable = new JTable(taskModel);
        taskTable.setRowHeight(UIUtil.scaleForGUI(70));
        taskTable.getColumnModel().getColumn(0).setCellRenderer(taskModel.getRenderer(getIconPackage()));
        taskTable.getSelectionModel().addListSelectionListener(ev -> taskTableValueChanged());
        taskSorter = new TableRowSorter<>(taskModel);
        taskSorter.setComparator(0, new TaskSorter());
        taskTable.setRowSorter(taskSorter);
        sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        taskSorter.setSortKeys(sortKeys);
        TaskTableMouseAdapter.connect(getCampaignGui(), taskTable, taskModel);
        JScrollPane scrollTaskTable = new JScrollPaneWithSpeed(taskTable);
        scrollTaskTable.setMinimumSize(new Dimension(200, 200));
        scrollTaskTable.setPreferredSize(new Dimension(300, 300));

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.gridwidth = 2;
        panTasks.add(scrollTaskTable, gridBagConstraints);

        JPanel panTechs = new JPanel(new GridBagLayout());

        btnShowAllTechs = new RoundedMMToggleButton(resourceMap.getString("btnShowAllTechs.text"));
        btnShowAllTechs.setToolTipText(resourceMap.getString("btnShowAllTechs.toolTipText"));
        btnShowAllTechs.setName("btnShowAllTechs");
        btnShowAllTechs.addActionListener(ev -> filterTechs());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panTechs.add(btnShowAllTechs, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panTechs.add(scrollTechTable, gridBagConstraints);

        astechPoolLabel = new JLabel("<html><b>Astech Pool Minutes:</> " +
                                           getCampaign().getAstechPoolMinutes() +
                                           " (" +
                                           getCampaign().getNumberAstechs() +
                                           " Astechs)</html>");
        astechPoolLabel.setHorizontalAlignment(SwingConstants.CENTER);
        astechPoolLabel.setName("astechPoolLabel");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panTechs.add(astechPoolLabel, gridBagConstraints);

        add(panServicedUnits);
        add(panTasks);
        add(panTechs);

        JPanel centerPanel = new JPanel(new GridLayout(1, 3));
        centerPanel.add(panServicedUnits);
        centerPanel.add(panTasks);
        centerPanel.add(panTechs);
        add(centerPanel, BorderLayout.CENTER);

        JPanel pnlTutorial = new TutorialHyperlinkPanel("repairTab");
        add(pnlTutorial, BorderLayout.SOUTH);

        filterTechs();
    }

    /**
     * These need to be migrated to the Suite Constants / Suite Options Setup
     */
    private void setUserPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(RepairTab.class);

            servicedUnitTable.setName("serviceUnitsTable");
            preferences.manage(new JTablePreference(servicedUnitTable));
        } catch (Exception ex) {
            logger.error("Failed to set user preferences", ex);
        }
    }

    private void updateTechTarget() {
        TargetRoll target = null;

        IPartWork part = getSelectedTask();
        if (null != part) {
            Unit u = part.getUnit();
            Person tech = getSelectedTech();
            if ((u != null) && u.isSelfCrewed()) {
                tech = u.getEngineer();
                if (null == tech) {
                    target = new TargetRoll(TargetRoll.IMPOSSIBLE,
                          "You must have a crew assigned to large vessels to attempt repairs.");
                }
            }
            if (null != tech) {
                boolean wasNull = false;
                // Temporarily set the Team ID if it isn't already.
                // This is needed for the Clan Tech flag
                if (part.getTech() == null) {
                    part.setTech(tech);
                    wasNull = true;
                }
                target = getCampaign().getTargetFor(part, tech);
                if (wasNull) { // If it was null, make it null again
                    part.setTech(null);
                }
            }
        }
        ((TechSorter) techSorter.getComparator(0)).clearPart();

        if (null != target) {
            btnDoTask.setEnabled(target.getValue() != TargetRoll.IMPOSSIBLE);
            textTarget.setText(target.getDesc());
            lblTargetNum.setText(target.getValueAsString());
        } else {
            btnDoTask.setEnabled(false);
            textTarget.setText("");
            lblTargetNum.setText("-");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshServicedUnitList();
        refreshTaskList();
        refreshPartsAcquisition();
        refreshTechsList();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#tabType()
     */
    @Override
    public MHQTabType tabType() {
        return MHQTabType.REPAIR_BAY;
    }

    @Override
    public @Nullable Person getSelectedTech() {
        if (techTable == null) {
            logger.error(new IllegalStateException(), "Tech table is not initialized.");
            return null;
        }

        int row = techTable.getSelectedRow();
        if (row < 0) {
            return null; // No row is selected
        }

        if (techsModel == null) {
            logger.error(new IllegalStateException(), "Tech model is not initialized.");
            return null;
        }

        return techsModel.getTechAt(techTable.convertRowIndexToModel(row));
    }

    @Override
    public IPartWork getSelectedTask() {
        int row = taskTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return taskModel.getTaskAt(taskTable.convertRowIndexToModel(row));
    }

    private Unit getSelectedServicedUnit() {
        int row = servicedUnitTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return servicedUnitModel.getUnit(servicedUnitTable.convertRowIndexToModel(row));
    }

    private void taskTableValueChanged() {
        filterTechs();
        updateTechTarget();
    }

    private void servicedUnitTableValueChanged(ListSelectionEvent evt) {
        if (ignoreUnitTable) {
            return;
        }
        refreshTaskList();
        refreshPartsAcquisition();
        int selected = servicedUnitTable.getSelectedRow();
        txtServicedUnitView.setText("");
        if (selected > -1) {
            Unit unit = servicedUnitModel.getUnit(servicedUnitTable.convertRowIndexToModel(selected));
            if (null != unit) {
                EntityReadout mv = EntityReadout.createReadout(unit.getEntity(), true, true);
                txtServicedUnitView.setText("<div style='font: 12pt monospaced'>" +
                                                  mv.getBasicSection(ViewFormatting.HTML) +
                                                  "<br>" +
                                                  mv.getLoadoutSection(ViewFormatting.HTML) +
                                                  "</div>");
                SwingUtilities.invokeLater(() -> scrollServicedUnitView.getVerticalScrollBar().setValue(0));
                if (!unit.equals(selectedUnit)) {
                    choiceLocation.setSelectedIndex(0);
                }
            }
            selectedUnit = unit;
        } else {
            selectedUnit = null;
            choiceLocation.setSelectedItem(null);
        }
    }

    private void techTableValueChanged(ListSelectionEvent evt) {
        updateTechTarget();

        taskTable.repaint();
    }

    private void doTask() {
        selectedRow = taskTable.getSelectedRow();
        selectedLocation = choiceLocation.getSelectedIndex();
        selectedUnit = getSelectedServicedUnit();
        selectedTech = getSelectedTech();
        IPartWork part = getSelectedTask();
        if (null == part) {
            return;
        }
        Unit u = part.getUnit();
        if ((u != null) && u.isSelfCrewed()) {
            selectedTech = u.getEngineer();
        }
        if (null == selectedTech) {
            return;
        }
        if ((part instanceof Part) && ((Part) part).onBadHipOrShoulder() && !part.isSalvaging()) {
            if (0 != JOptionPane.showConfirmDialog(getFrame(), """
                  You are repairing/replacing a part on a limb with a bad shoulder or hip.
                  You may continue, but this limb cannot be repaired and you will have to
                  remove this equipment if you wish to scrap and then replace the limb.
                  Do you wish to continue?""", "Busted Hip/Shoulder", JOptionPane.YES_NO_OPTION)) {
                return;
            }
        }
        String r = getCampaign().fixPart(part, selectedTech);

        Reader stringReader = new StringReader(r);
        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument blank = (HTMLDocument) htmlKit.createDefaultDocument();
        try {
            htmlKit.read(stringReader, blank, 0);
        } catch (Exception ignored) {

        }
        txtResult.setDocument(blank);
        txtResult.setCaretPosition(blank.getLength());

        if (null != u) {
            if (!u.isRepairable() && !u.hasSalvageableParts()) {
                selectedRow = -1;
                getCampaign().removeUnit(u.getId());
            }
            if (!u.isServiceable()) {
                selectedRow = -1;
            }
            u.refreshPodSpace();
        }
        MekHQ.triggerEvent(new PartWorkEvent(selectedTech, part));

        // get the selected row back for tasks
        if (selectedRow != -1) {
            if (taskTable.getRowCount() > 0) {
                if (taskTable.getRowCount() <= selectedRow) {
                    selectedRow = taskTable.getRowCount() - 1;
                }
                taskTable.setRowSelectionInterval(selectedRow, selectedRow);
            }

            // If requested, switch to top entry
            if (getCampaignOptions().isResetToFirstTech() && (techTable.getRowCount() > 0)) {
                techTable.setRowSelectionInterval(0, 0);
            } else {
                // Or get the selected tech back
                for (int i = 0; i < techTable.getRowCount(); i++) {
                    Person p = techsModel.getTechAt(techTable.convertRowIndexToModel(i));
                    if (selectedTech.getId().equals(p.getId())) {
                        techTable.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
        }
        if (selectedLocation != -1) {
            if ((selectedUnit == null) ||
                      (getSelectedServicedUnit() == null) ||
                      !selectedUnit.equals(getSelectedServicedUnit()) ||
                      (selectedLocation >= choiceLocation.getItemCount())) {
                selectedLocation = 0;
            }
            choiceLocation.setSelectedIndex(selectedLocation);
        }

    }

    public void filterTasks() {
        selectedLocation = choiceLocation.getSelectedIndex();
        final String loc = (String) choiceLocation.getSelectedItem();
        RowFilter<TaskTableModel, Integer> taskLocationFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TaskTableModel, ? extends Integer> entry) {
                TaskTableModel taskModel = entry.getModel();
                IPartWork part = taskModel.getTaskAt(entry.getIdentifier());
                if ((part != null) && (loc != null) && !loc.isEmpty()) {
                    if (loc.equals("All")) {
                        return true;
                    } else if (loc.equals("OmniPod")) {
                        return part instanceof PodSpace;
                    } else if (part instanceof PodSpace) {
                        return part.getLocation() == part.getUnit().getEntity().getLocationFromAbbr(loc);
                    } else {
                        return ((Part) part).isInLocation(loc);
                    }
                } else {
                    return false;
                }

            }
        };
        taskSorter.setRowFilter(taskLocationFilter);
    }

    public void filterTechs() {
        final IPartWork part = getSelectedTask();
        final Unit unit = getSelectedServicedUnit();
        RowFilter<TechTableModel, Integer> techTypeFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TechTableModel, ? extends Integer> entry) {
                if (part == null) {
                    return false;
                } else if (!part.needsFixing() && !part.isSalvaging()) {
                    return false;
                }
                TechTableModel techModel = entry.getModel();
                Person tech = techModel.getTechAt(entry.getIdentifier());
                if ((unit != null) && unit.isSelfCrewed()) {
                    if (!tech.getPrimaryRole().isVesselCrew()) {
                        return false;
                    }
                    // check whether the engineer is assigned to the correct unit
                    return unit.equals(tech.getUnit());
                } else if (tech.getPrimaryRole().isVesselCrew() && (unit != null) && !unit.isSelfCrewed()) {
                    return false;
                } else if (!tech.isRightTechTypeFor(part) && !btnShowAllTechs.isSelected()) {
                    return false;
                }
                Skill skill = tech.getSkillForWorkingOn(part);
                int modePenalty = part.getMode().expReduction;
                if (skill == null) {
                    return false;
                } else if (part.getSkillMin() > SkillType.EXP_LEGENDARY) {
                    return false;
                } else if (tech.getMinutesLeft() <= 0) {
                    return false;
                } else {
                    return getCampaign().getCampaignOptions().isDestroyByMargin() ||
                                 (part.getSkillMin() <=
                                        (skill.getExperienceLevel(tech.getOptions(), tech.getATOWAttributes()) -
                                               modePenalty));
                }
            }
        };

        if (getCampaignOptions().isAssignedTechFirst()) {
            ((TechSorter) techSorter.getComparator(0)).setPart(part);
        }
        techSorter.setRowFilter(techTypeFilter);
    }

    /**
     * Focuses on the unit with the given ID if it exists.
     *
     * @param id The unique identifier of the unit.
     *
     * @return A value indicating whether the unit was focused.
     */
    public boolean focusOnUnit(UUID id) {
        int row = -1;
        for (int i = 0; i < servicedUnitTable.getRowCount(); i++) {
            if (servicedUnitModel.getUnit(servicedUnitTable.convertRowIndexToModel(i)).getId().equals(id)) {
                row = i;
                break;
            }
        }
        if (row != -1) {
            servicedUnitTable.setRowSelectionInterval(row, row);
            servicedUnitTable.scrollRectToVisible(servicedUnitTable.getCellRect(row, 0, true));
            return true;
        }
        return false;
    }

    public void refreshServicedUnitList() {
        UUID uuid = (getSelectedServicedUnit() != null) ? getSelectedServicedUnit().getId() : null;

        ignoreUnitTable = true;
        servicedUnitModel.setData(getCampaign().getServiceableUnits());
        ignoreUnitTable = false;

        if (!focusOnUnit(uuid)) {
            refreshTaskList();
            txtServicedUnitView.setText("");
        }

        refreshPartsAcquisition();
    }

    public void refreshTaskList() {
        selectedRow = taskTable.getSelectedRow();

        List<IPartWork> partsNeedingService = (getSelectedServicedUnit() != null) ?
                                                    getSelectedServicedUnit().getPartsNeedingService() :
                                                    Collections.emptyList();
        taskModel.setData(partsNeedingService);

        if ((getSelectedServicedUnit() != null) && (getSelectedServicedUnit().getEntity() != null)) {
            // Retain the selected index while the contents is refreshed.
            int selected = choiceLocation.getSelectedIndex();
            choiceLocation.removeAllItems();
            choiceLocation.addItem("All");
            for (String s : getSelectedServicedUnit().getEntity().getLocationAbbreviations()) {
                if (!s.equals("WNG")) {
                    choiceLocation.addItem(s);
                }
            }

            if (getSelectedServicedUnit().getEntity().isOmni()) {
                choiceLocation.addItem("OmniPod");
            }
            selectedLocation = selected;
            if ((selectedLocation > -1) && (choiceLocation.getModel().getSize() > selectedLocation)) {
                choiceLocation.setSelectedIndex(selectedLocation);
            } else {
                choiceLocation.setSelectedIndex(0);
            }
            choiceLocation.setEnabled(true);
        } else {
            choiceLocation.removeAllItems();
            choiceLocation.setEnabled(false);
        }
        filterTasks();

        if ((selectedRow != -1) && (taskTable.getRowCount() > 0)) {
            if (taskTable.getRowCount() <= selectedRow) {
                selectedRow = taskTable.getRowCount() - 1;
            }
            taskTable.setRowSelectionInterval(selectedRow, selectedRow);
        }

        if ((selectedLocation != -1) && (choiceLocation.getItemCount() > 0)) {
            choiceLocation.setSelectedIndex(selectedLocation);
        }
    }

    /**
     * Refreshes the list of technicians displayed in the techTable, updates their details, and ensures a valid row
     * selection is maintained after any data changes.
     *
     * <p>This method performs the following steps:</p>
     * <ul>
     *   <li>Retrieves the selected row from the table prior to any updates.</li>
     *   <li>Fetches the updated list of technicians who are available for work, sorted by skill
     *       (with elites appearing at the bottom).</li>
     *   <li>Updates the `techsModel` with the new list of technicians, and re-applies the row
     *       filter through the `filterTechs()` method to ensure only valid rows are displayed.</li>
     *   <li>Updates the AsTech pool statistics (minutes, overtime availability, and Astech count)
     *       in the UI label.</li>
     *   <li>Ensures the table selection is consistent after the updates:
     *       <ul>
     *         <li>If campaign options specify resetting to the first row, the first row is selected
     *             (if it exists).</li>
     *         <li>If a previously selected technician (`selectedTech`) is still valid, it is re-selected.</li>
     *         <li>If the previously selected row index is still valid after updates, it is re-selected.</li>
     *         <li>Otherwise, the row selection is cleared.</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * <p><b>Edge case handling:</b></p>
     * <ul>
     *   <li>If the table's row count changes (due to filtering or updating), the previously
     *       selected index and technician are checked for validity before re-selecting.</li>
     *   <li>If the table becomes empty after filtering or updating, the selection is safely cleared.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if an invalid row index is provided to the selection methods. This exception is
     *                                  prevented by validating indices against updated row counts.
     */
    public void refreshTechsList() {
        int selected = techTable.getSelectedRow();
        // Get all techs who have more than 0 minutes free, and sort by skill descending (elites at bottom)
        List<Person> techs = getCampaign().getTechs(true);
        techsModel.setData(techs);
        filterTechs();

        String astechString = "<html><b>Astech Pool Minutes:</> " + getCampaign().getAstechPoolMinutes();
        if (getCampaign().isOvertimeAllowed()) {
            astechString += " [" + getCampaign().getAstechPoolOvertime() + " overtime]";
        }
        astechString += " (" + getCampaign().getNumberAstechs() + " Astechs)</html>";
        astechPoolLabel.setText(astechString);

        // Ensuring valid row selection after refresh
        if (getCampaignOptions().isResetToFirstTech() && (techTable.getRowCount() > 0)) {
            // Double-check the row count and safely select the first row
            techTable.setRowSelectionInterval(0, 0);
        } else if (selectedTech != null) {
            // If a specific tech was selected, try to match it
            boolean techFound = false;
            for (int i = 0; i < techTable.getRowCount(); i++) {
                Person person = techsModel.getTechAt(techTable.convertRowIndexToModel(i));
                if (selectedTech.equals(person)) {
                    techTable.setRowSelectionInterval(i, i);
                    techFound = true;
                    break;
                }
            }
            if (!techFound) {
                techTable.clearSelection(); // Clear selection if tech is no longer in the table
            }
        } else if ((selected >= 0) && (selected < techTable.getRowCount())) {
            // Ensure selected index is valid after table updates
            techTable.setRowSelectionInterval(selected, selected);
        } else {
            techTable.clearSelection(); // Clear selection if there's no valid option
        }
    }

    public void refreshPartsAcquisition() {
        refreshPartsAcquisitionService(true);
    }

    public void refreshPartsAcquisitionService(boolean rebuildPartsList) {
        if (rebuildPartsList) {
            PartsAcquisitionService.buildPartsList(getCampaign());
        }

        btnAcquisitions.firePropertyChange("counts", -1, 0);
    }

    private ActionScheduler servicedUnitListScheduler = new ActionScheduler(this::refreshServicedUnitList);
    private ActionScheduler techsScheduler = new ActionScheduler(this::refreshTechsList);
    private ActionScheduler taskScheduler = new ActionScheduler(this::refreshTaskList);
    private ActionScheduler acquireScheduler = new ActionScheduler(this::refreshPartsAcquisition);

    @Subscribe
    public void handle(DeploymentChangedEvent ev) {
        servicedUnitListScheduler.schedule();
    }

    @Subscribe
    public void handle(ScenarioResolvedEvent ev) {
        servicedUnitListScheduler.schedule();
    }

    @Subscribe
    public void handle(UnitEvent ev) {
        servicedUnitListScheduler.schedule();
    }

    @Subscribe
    public void handle(RepairStatusChangedEvent ev) {
        servicedUnitListScheduler.schedule();
    }

    @Subscribe
    public void handle(StratConDeploymentEvent ev) {
        servicedUnitListScheduler.schedule();
    }

    @Subscribe
    public void handle(PersonEvent ev) {
        techsScheduler.schedule();
    }

    @Subscribe
    public void handle(PartEvent ev) {
        if (ev.getPart().getUnit() == null) {
            acquireScheduler.schedule();
            taskScheduler.schedule();
        } else {
            servicedUnitListScheduler.schedule();
        }
    }

    @Subscribe
    public void handle(AcquisitionEvent ev) {
        acquireScheduler.schedule();
        taskScheduler.schedule();
    }

    @Subscribe
    public void handle(ProcurementEvent ev) {
        filterTasks();
        acquireScheduler.schedule();
    }

    @Subscribe
    public void handle(PartWorkEvent ev) {
        if (ev.getPartWork().getUnit() == null) {
            acquireScheduler.schedule();
            taskScheduler.schedule();
        } else {
            servicedUnitListScheduler.schedule();
        }
        techsScheduler.schedule();
    }

    @Subscribe
    public void handle(OvertimeModeEvent ev) {
        filterTechs();
    }

    @Subscribe
    public void handle(AsTechPoolChangedEvent ev) {
        filterTechs();
    }
}
