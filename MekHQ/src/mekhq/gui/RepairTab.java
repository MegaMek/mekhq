/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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

package mekhq.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.common.MechView;
import megamek.common.TargetRoll;
import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.event.AcquisitionEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.OvertimeModeEvent;
import mekhq.campaign.event.PartEvent;
import mekhq.campaign.event.PartWorkEvent;
import mekhq.campaign.event.PersonEvent;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.event.RepairStatusChangedEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.event.UnitEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.adapter.AcquisitionTableMouseAdapter;
import mekhq.gui.adapter.ServicedUnitsTableMouseAdapter;
import mekhq.gui.adapter.TaskTableMouseAdapter;
import mekhq.gui.dialog.MassRepairSalvageDialog;
import mekhq.gui.model.AcquisitionTableModel;
import mekhq.gui.model.TaskTableModel;
import mekhq.gui.model.TechTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.TaskSorter;
import mekhq.gui.sorter.TechSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;

/**
 * Shows damaged units and controls for repair.
 *
 */
public final class RepairTab extends CampaignGuiTab implements ITechWorkPanel {

    private static final long serialVersionUID = 6757065427956450309L;

    private JPanel panDoTask;
    private JTabbedPane tabTasks;
    private JSplitPane splitServicedUnits;
    private JTable servicedUnitTable;
    private JTable taskTable;
    private JTable acquisitionTable;
    private JTable techTable;
    private JButton btnDoTask;
    private JButton btnUseBonusPart;
    private JToggleButton btnShowAllTechs;
    private JScrollPane scrTextTarget;
    private JLabel lblTargetNum;
    private JTextPane txtServicedUnitView;
    private JTextArea textTarget;
    private JLabel astechPoolLabel;
    private JComboBox<String> choiceLocation;

    private UnitTableModel servicedUnitModel;
    private TaskTableModel taskModel;
    private AcquisitionTableModel acquireModel;
    private TechTableModel techsModel;

    private TableRowSorter<UnitTableModel> servicedUnitSorter;
    private TableRowSorter<TaskTableModel> taskSorter;
    private TableRowSorter<TechTableModel> techSorter;

    RepairTab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        setLayout(new GridBagLayout());

        JPanel panServicedUnits = new JPanel(new GridBagLayout());

        // Add panel for MRMS buttons
        JPanel massRepairButtons = new JPanel(new GridBagLayout());

        JButton btnMRMSDialog = new JButton();
        btnMRMSDialog.setText("Mass Repair/Salvage"); // NOI18N
        btnMRMSDialog.setToolTipText("Start Mass Repair/Salvage from dialog");
        btnMRMSDialog.setName("btnMRMSDialog"); // NOI18N
        btnMRMSDialog.addActionListener(ev -> {
            MassRepairSalvageDialog dlg = new MassRepairSalvageDialog(getFrame(), true, getCampaignGui(), null,
                    MassRepairSalvageDialog.MODE.UNITS);
            dlg.setVisible(true);
        });

        JButton btnMRMSInstantAll = new JButton();
        btnMRMSInstantAll.setText("Instant Mass Repair/Salvage All"); // NOI18N
        btnMRMSInstantAll
                .setToolTipText("Perform Mass Repair/Salvage immediately on all units using active configuration");
        btnMRMSInstantAll.setName("btnMRMSInstantAll"); // NOI18N
        btnMRMSInstantAll.addActionListener(ev -> {
            MassRepairSalvageDialog.massRepairSalvageAllUnits(getCampaignGui());
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        massRepairButtons.add(btnMRMSDialog, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        massRepairButtons.add(btnMRMSInstantAll, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 0, 5, 0);
        panServicedUnits.add(massRepairButtons, gridBagConstraints);

        servicedUnitModel = new UnitTableModel(getCampaign());
        servicedUnitTable = new JTable(servicedUnitModel);
        servicedUnitTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        servicedUnitTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        servicedUnitTable.setColumnModel(new XTableColumnModel());
        servicedUnitTable.createDefaultColumnsFromModel();
        servicedUnitSorter = new TableRowSorter<UnitTableModel>(servicedUnitModel);
        servicedUnitSorter.setComparator(UnitTableModel.COL_STATUS, new UnitStatusSorter());
        servicedUnitSorter.setComparator(UnitTableModel.COL_TYPE, new UnitTypeSorter());
        servicedUnitTable.setRowSorter(servicedUnitSorter);
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(UnitTableModel.COL_TYPE, SortOrder.DESCENDING));
        servicedUnitSorter.setSortKeys(sortKeys);
        TableColumn column = null;
        for (int i = 0; i < UnitTableModel.N_COL; i++) {
            column = ((XTableColumnModel) servicedUnitTable.getColumnModel()).getColumnByModelIndex(i);
            column.setPreferredWidth(servicedUnitModel.getColumnWidth(i));
            column.setCellRenderer(servicedUnitModel.getRenderer(false, getIconPackage()));
            if (i != UnitTableModel.COL_NAME && i != UnitTableModel.COL_STATUS && i != UnitTableModel.COL_REPAIR
                    && i != UnitTableModel.COL_PARTS && i != UnitTableModel.COL_SITE && i != UnitTableModel.COL_TYPE) {
                ((XTableColumnModel) servicedUnitTable.getColumnModel()).setColumnVisible(column, false);
            }
        }
        servicedUnitTable.setIntercellSpacing(new Dimension(0, 0));
        servicedUnitTable.setShowGrid(false);
        servicedUnitTable.getSelectionModel().addListSelectionListener(ev -> servicedUnitTableValueChanged(ev));
        servicedUnitTable.addMouseListener(new ServicedUnitsTableMouseAdapter(getCampaignGui(),
                servicedUnitTable, servicedUnitModel));
        JScrollPane scrollServicedUnitTable = new JScrollPane(servicedUnitTable);
        scrollServicedUnitTable.setMinimumSize(new java.awt.Dimension(350, 200));
        scrollServicedUnitTable.setPreferredSize(new java.awt.Dimension(350, 200));

        txtServicedUnitView = new JTextPane();
        txtServicedUnitView.setEditable(false);
        txtServicedUnitView.setContentType("text/html");
        JScrollPane scrollServicedUnitView = new JScrollPane(txtServicedUnitView);
        scrollServicedUnitView.setMinimumSize(new java.awt.Dimension(350, 400));
        scrollServicedUnitView.setPreferredSize(new java.awt.Dimension(350, 400));

        splitServicedUnits = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollServicedUnitTable, scrollServicedUnitView);
        splitServicedUnits.setOneTouchExpandable(true);
        splitServicedUnits.setResizeWeight(0.0);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panServicedUnits.add(splitServicedUnits, gridBagConstraints);

        JPanel panTasks = new JPanel(new GridBagLayout());

        techsModel = new TechTableModel(getCampaignGui(), this);
        techTable = new JTable(techsModel);
        techTable.setRowHeight(60);
        techTable.getColumnModel().getColumn(0).setCellRenderer(techsModel.getRenderer(getIconPackage()));
        techTable.getSelectionModel().addListSelectionListener(ev -> techTableValueChanged(ev));
        techSorter = new TableRowSorter<TechTableModel>(techsModel);
        techSorter.setComparator(0, new TechSorter());
        techTable.setRowSorter(techSorter);
        sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        techSorter.setSortKeys(sortKeys);
        JScrollPane scrollTechTable = new JScrollPane(techTable);
        scrollTechTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollTechTable.setPreferredSize(new java.awt.Dimension(300, 300));

        tabTasks = new JTabbedPane();
        tabTasks.setMinimumSize(new java.awt.Dimension(300, 200));
        tabTasks.setName("tabTasks"); // NOI18N
        tabTasks.setPreferredSize(new java.awt.Dimension(300, 300));

        panDoTask = new JPanel(new GridBagLayout());
        panDoTask.setMinimumSize(new java.awt.Dimension(300, 100));
        panDoTask.setName("panelDoTask"); // NOI18N
        panDoTask.setPreferredSize(new java.awt.Dimension(300, 100));

        btnDoTask = new JButton(resourceMap.getString("btnDoTask.text")); // NOI18N
        btnDoTask.setToolTipText(resourceMap.getString("btnDoTask.toolTipText")); // NOI18N
        btnDoTask.setEnabled(false);
        btnDoTask.addActionListener(ev -> doTask());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        panDoTask.add(btnDoTask, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        panDoTask.add(new JLabel(resourceMap.getString("lblTarget.text")), gridBagConstraints);

        lblTargetNum = new JLabel(resourceMap.getString("lblTargetNum.text")); // NOI18N
        lblTargetNum.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTargetNum.setName("lblTargetNum"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        panDoTask.add(lblTargetNum, gridBagConstraints);

        textTarget = new JTextArea();
        textTarget.setColumns(20);
        textTarget.setEditable(false);
        textTarget.setLineWrap(true);
        textTarget.setRows(5);
        textTarget.setText(resourceMap.getString("textTarget.text")); // NOI18N
        textTarget.setWrapStyleWord(true);
        textTarget.setBorder(null);
        textTarget.setName("textTarget"); // NOI18N
        scrTextTarget = new JScrollPane(textTarget);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        panDoTask.add(scrTextTarget, gridBagConstraints);

        choiceLocation = new JComboBox<String>();
        choiceLocation.removeAllItems();
        choiceLocation.addItem("All");
        choiceLocation.setEnabled(false);
        choiceLocation.addActionListener(ev -> filterTasks());
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridheight = 1;
        panDoTask.add(choiceLocation, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        panTasks.add(panDoTask, gridBagConstraints);

        taskModel = new TaskTableModel(getCampaignGui(), this);
        taskTable = new JTable(taskModel);
        taskTable.setRowHeight(70);
        taskTable.getColumnModel().getColumn(0).setCellRenderer(taskModel.getRenderer(getIconPackage()));
        taskTable.getSelectionModel().addListSelectionListener(ev -> taskTableValueChanged());
        taskSorter = new TableRowSorter<TaskTableModel>(taskModel);
        taskSorter.setComparator(0, new TaskSorter());
        taskTable.setRowSorter(taskSorter);
        sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        taskSorter.setSortKeys(sortKeys);
        taskTable.addMouseListener(new TaskTableMouseAdapter(getCampaignGui(),
                taskTable, taskModel));
        JScrollPane scrollTaskTable = new JScrollPane(taskTable);
        scrollTaskTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollTaskTable.setPreferredSize(new java.awt.Dimension(300, 300));

        btnUseBonusPart = new JButton();
        btnUseBonusPart.setVisible(false);
        btnUseBonusPart.addActionListener(ev -> useBonusPart());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        panTasks.add(btnUseBonusPart, gridBagConstraints);

        acquireModel = new AcquisitionTableModel();
        acquisitionTable = new JTable(acquireModel);
        acquisitionTable.setName("AcquisitionTable"); // NOI18N
        acquisitionTable.setRowHeight(70);
        acquisitionTable.getColumnModel().getColumn(0).setCellRenderer(acquireModel.getRenderer(getIconPackage()));
        acquisitionTable.getSelectionModel().addListSelectionListener(ev -> acquisitionTableValueChanged());
        acquisitionTable.addMouseListener(new AcquisitionTableMouseAdapter(getCampaignGui(),
                acquisitionTable, acquireModel));
        JScrollPane scrollAcquisitionTable = new JScrollPane(acquisitionTable);
        scrollAcquisitionTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollAcquisitionTable.setPreferredSize(new java.awt.Dimension(300, 300));

        tabTasks.addTab(resourceMap.getString("scrollTaskTable.TabConstraints.tabTasks"), scrollTaskTable); // NOI18N
        tabTasks.addTab(resourceMap.getString("scrollAcquisitionTable.TabConstraints.tabTasks"),
                scrollAcquisitionTable); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panTasks.add(tabTasks, gridBagConstraints);

        tabTasks.addChangeListener(ev -> taskTabChanged());

        JPanel panTechs = new JPanel(new GridBagLayout());

        btnShowAllTechs = new JToggleButton(resourceMap.getString("btnShowAllTechs.text")); // NOI18N
        btnShowAllTechs.setToolTipText(resourceMap.getString("btnShowAllTechs.toolTipText")); // NOI18N
        btnShowAllTechs.setName("btnShowAllTechs"); // NOI18N
        btnShowAllTechs.addActionListener(ev -> filterTechs());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTechs.add(btnShowAllTechs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panTechs.add(scrollTechTable, gridBagConstraints);

        astechPoolLabel = new JLabel("<html><b>Astech Pool Minutes:</> " + getCampaign().getAstechPoolMinutes() + " ("
                + getCampaign().getNumberAstechs() + " Astechs)</html>"); // NOI18N
        astechPoolLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        astechPoolLabel.setName("astechPoolLabel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panTechs.add(astechPoolLabel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(panServicedUnits, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(panTasks, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(panTechs, gridBagConstraints);

        filterTechs();
    }

    protected boolean repairsSelected() {
        return tabTasks.getSelectedIndex() == 0;
    }

    protected boolean acquireSelected() {
        return tabTasks.getSelectedIndex() == 1;
    }

    private void taskTabChanged() {
        filterTechs();
        updateTechTarget();
    }

    protected void updateTechTarget() {
        TargetRoll target = null;

        if (acquireSelected()) {
            IAcquisitionWork acquire = getSelectedAcquisition();
            if (null != acquire) {
                Person admin = getCampaign().getLogisticsPerson();
                target = getCampaign().getTargetForAcquisition(acquire, admin);
            }
        } else {
            Part part = getSelectedTask();
            if (null != part) {
                Unit u = part.getUnit();
                Person tech = getSelectedTech();
                if (null != u && u.isSelfCrewed()) {
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
                    if (part.getTeamId() == null) {
                        part.setTeamId(tech.getId());
                        wasNull = true;
                    }
                    target = getCampaign().getTargetFor(part, tech);
                    if (wasNull) { // If it was null, make it null again
                        part.setTeamId(null);
                    }
                }
            }
            ((TechSorter) techSorter.getComparator(0)).clearPart();
        }
        if (null != target) {
            btnDoTask.setEnabled(target.getValue() != TargetRoll.IMPOSSIBLE);
            textTarget.setText(target.getDesc());
            lblTargetNum.setText(target.getValueAsString());
        } else {
            btnDoTask.setEnabled(false);
            textTarget.setText("");
            lblTargetNum.setText("-");
        }
        if (getCampaign().getCampaignOptions().getUseAtB()) {
            int numBonusParts = 0;
            if (acquireSelected() && null != getSelectedAcquisition()) {
                AtBContract contract = getCampaign().getAttachedAtBContract(getSelectedAcquisition().getUnit());
                if (null == contract) {
                    numBonusParts = getCampaign().totalBonusParts();
                } else {
                    numBonusParts = contract.getNumBonusParts();
                }
            }
            if (numBonusParts > 0) {
                btnUseBonusPart.setText("Use Bonus Part (" + numBonusParts + ")");
                btnUseBonusPart.setVisible(true);
            } else {
                btnUseBonusPart.setVisible(false);
            }
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
        refreshAcquireList();
        refreshTechsList();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#tabType()
     */
    @Override
    public GuiTabType tabType() {
        return GuiTabType.REPAIR;
    }

    @Override
    public Person getSelectedTech() {
        int row = techTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return techsModel.getTechAt(techTable.convertRowIndexToModel(row));
    }

    @Override
    public Part getSelectedTask() {
        int row = taskTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return taskModel.getTaskAt(taskTable.convertRowIndexToModel(row));
    }

    private IAcquisitionWork getSelectedAcquisition() {
        int row = acquisitionTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return acquireModel.getAcquisitionAt(acquisitionTable.convertRowIndexToModel(row));
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

    private void acquisitionTableValueChanged() {
        filterTechs();
        updateTechTarget();
    }

    private void servicedUnitTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        refreshTaskList();
        refreshAcquireList();
        int selected = servicedUnitTable.getSelectedRow();
        txtServicedUnitView.setText("");
        if (selected > -1) {
            Unit unit = servicedUnitModel.getUnit(servicedUnitTable.convertRowIndexToModel(selected));
            if (null != unit) {
                MechView mv = new MechView(unit.getEntity(), true, true);
                txtServicedUnitView.setText("<div style='font: 12pt monospaced'>" + mv.getMechReadoutBasic() + "<br>"
                        + mv.getMechReadoutLoadout() + "</div>");
            }
        }
    }

    private void techTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        updateTechTarget();

        taskTable.repaint();
    }

    private void doTask() {
        int selectedRow = -1;
        int selectedLocation = -1;
        Unit selectedUnit = null;
        // int selectedTechRow = -1;
        Person tech = getSelectedTech();
        if (repairsSelected()) {
            selectedRow = taskTable.getSelectedRow();
            // selectedTechRow = TechTable.getSelectedRow();
            selectedLocation = choiceLocation.getSelectedIndex();
            selectedUnit = getSelectedServicedUnit();
            Part part = getSelectedTask();
            if (null == part) {
                return;
            }
            Unit u = part.getUnit();
            if (null != u && u.isSelfCrewed()) {
                tech = u.getEngineer();
            }
            if (null == tech) {
                return;
            }
            if (part.onBadHipOrShoulder() && !part.isSalvaging()) {
                if (part instanceof MekLocation && ((MekLocation) part).isBreached()
                        && 0 != JOptionPane.showConfirmDialog(getFrame(),
                                "You are sealing a limb with a bad shoulder or hip.\n"
                                        + "You may continue, but this limb cannot be repaired and you will have to\n"
                                        + "scrap it in order to repair the internal structure and fix the shoulder/hip.\n"
                                        + "Do you wish to continue?",
                                "Busted Hip/Shoulder", JOptionPane.YES_NO_OPTION)) {
                    return;
                } else if (part instanceof MekLocation && ((MekLocation) part).isBlownOff()
                        && 0 != JOptionPane.showConfirmDialog(getFrame(),
                                "You are re-attaching a limb with a bad shoulder or hip.\n"
                                        + "You may continue, but this limb cannot be repaired and you will have to\n"
                                        + "scrap it in order to repair the internal structure and fix the shoulder/hip.\n"
                                        + "Do you wish to continue?",
                                "Busted Hip/Shoulder", JOptionPane.YES_NO_OPTION)) {
                    return;
                } else if (0 != JOptionPane.showConfirmDialog(getFrame(),
                        "You are repairing/replacing a part on a limb with a bad shoulder or hip.\n"
                                + "You may continue, but this limb cannot be repaired and you will have to\n"
                                + "remove this equipment if you wish to scrap and then replace the limb.\n"
                                + "Do you wish to continue?",
                        "Busted Hip/Shoulder", JOptionPane.YES_NO_OPTION)) {
                    return;
                }
            }
            getCampaign().fixPart(part, tech);
            if (null != u && !u.isRepairable() && u.getSalvageableParts().size() == 0) {
                selectedRow = -1;
                getCampaign().removeUnit(u.getId());
            }
            if (null != u && !getCampaign().getServiceableUnits().contains(u)) {
                selectedRow = -1;
            }
            MekHQ.triggerEvent(new PartWorkEvent(tech, part));
        } else if (acquireSelected()) {
            selectedRow = acquisitionTable.getSelectedRow();
            IAcquisitionWork acquisition = getSelectedAcquisition();
            if (null == acquisition) {
                return;
            }
            getCampaign().getShoppingList().addShoppingItem(acquisition, 1, getCampaign());
        }

        getCampaignGui().refreshReport();
        getCampaignGui().refreshFunds();

        // get the selected row back for tasks
        if (selectedRow != -1) {
            if (acquireSelected()) {
                if (acquisitionTable.getRowCount() > 0) {
                    if (acquisitionTable.getRowCount() == selectedRow) {
                        acquisitionTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    } else {
                        acquisitionTable.setRowSelectionInterval(selectedRow, selectedRow);
                    }
                }
            } else if (repairsSelected()) {
                if (taskTable.getRowCount() > 0) {
                    if (taskTable.getRowCount() == selectedRow) {
                        taskTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    } else {
                        taskTable.setRowSelectionInterval(selectedRow, selectedRow);
                    }
                }
            }

            // If requested, switch to top entry
            if (getCampaign().getCampaignOptions().useResetToFirstTech() && techTable.getRowCount() > 0) {
                techTable.setRowSelectionInterval(0, 0);
            } else {
                // Or get the selected tech back
                for (int i = 0; i < techTable.getRowCount(); i++) {
                    Person p = techsModel.getTechAt(techTable.convertRowIndexToModel(i));
                    if (tech.getId().equals(p.getId())) {
                        techTable.setRowSelectionInterval(i, i);
                        break;
                    }
                }
            }
        }
        if (selectedLocation != -1) {
            if (selectedUnit == null || getSelectedServicedUnit() == null
                    || !selectedUnit.equals(getSelectedServicedUnit())
                    || selectedLocation >= choiceLocation.getItemCount()) {
                selectedLocation = 0;
            }
            choiceLocation.setSelectedIndex(selectedLocation);
        }

    }

    private void useBonusPart() {
        int selectedRow = -1;
        if (acquireSelected()) {
            selectedRow = acquisitionTable.getSelectedRow();
            IAcquisitionWork acquisition = getSelectedAcquisition();
            if (null == acquisition) {
                return;
            }
            if (acquisition instanceof AmmoBin) {
                acquisition = ((AmmoBin) acquisition).getAcquisitionWork();
            }
            String report = acquisition.find(0);
            if (report.endsWith("0 days.")) {
                AtBContract contract = getCampaign().getAttachedAtBContract(getSelectedAcquisition().getUnit());
                if (null == contract) {
                    for (Mission m : getCampaign().getMissions()) {
                        if (m.isActive() && m instanceof AtBContract && ((AtBContract) m).getNumBonusParts() > 0) {
                            contract = (AtBContract) m;
                            break;
                        }
                    }
                }
                if (null == contract) {
                    MekHQ.logError("AtB: used bonus part but no contract has bonus parts available.");
                } else {
                    contract.useBonusPart();
                }
            }
        }

        getCampaignGui().refreshReport();
        getCampaignGui().refreshFunds();

        if (selectedRow != -1) {
            if (acquireSelected()) {
                if (acquisitionTable.getRowCount() > 0) {
                    if (acquisitionTable.getRowCount() == selectedRow) {
                        acquisitionTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
                    } else {
                        acquisitionTable.setRowSelectionInterval(selectedRow, selectedRow);
                    }
                }
            }
        }
    }

    public void filterTasks() {
        RowFilter<TaskTableModel, Integer> taskLocationFilter = null;
        final String loc = (String) choiceLocation.getSelectedItem();
        taskLocationFilter = new RowFilter<TaskTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TaskTableModel, ? extends Integer> entry) {
                TaskTableModel taskModel = entry.getModel();
                Part part = taskModel.getTaskAt(entry.getIdentifier());
                if (part == null) {
                    return false;
                }
                if (loc != null && !loc.isEmpty()) {
                    if (loc.equals("All")) {
                        return true;
                    }
                    return part.isInLocation(loc);
                }
                return false;

            }
        };
        taskSorter.setRowFilter(taskLocationFilter);
    }

    public void filterTechs() {
        RowFilter<TechTableModel, Integer> techTypeFilter = null;
        final Part part = getSelectedTask();
        final Unit unit = getSelectedServicedUnit();
        techTypeFilter = new RowFilter<TechTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TechTableModel, ? extends Integer> entry) {
                if (acquireSelected()) {
                    return false;
                }
                if (null == part) {
                    return false;
                }
                if (!part.needsFixing() && !part.isSalvaging()) {
                    return false;
                }
                TechTableModel techModel = entry.getModel();
                Person tech = techModel.getTechAt(entry.getIdentifier());
                if (null != unit && unit.isSelfCrewed()) {
                    if (tech.getPrimaryRole() != Person.T_SPACE_CREW) {
                        return false;
                    }
                    // check whether the engineer is assigned to the correct
                    // unit
                    return unit.getId().equals(tech.getUnitId());
                }
                if (tech.getPrimaryRole() == Person.T_SPACE_CREW && (null != unit) && !unit.isSelfCrewed()) {
                    return false;
                }
                if (!tech.isRightTechTypeFor(part) && !btnShowAllTechs.isSelected()) {
                    return false;
                }
                Skill skill = tech.getSkillForWorkingOn(part);
                int modePenalty = part.getMode().expReduction;
                if (skill == null) {
                    return false;
                }
                if (part.getSkillMin() > SkillType.EXP_ELITE) {
                    return false;
                }
                if (tech.getMinutesLeft() <= 0) {
                    return false;
                }
                return (getCampaign().getCampaignOptions().isDestroyByMargin()
                        || part.getSkillMin() <= (skill.getExperienceLevel() - modePenalty));
            }
        };
        if (getCampaign().getCampaignOptions().useAssignedTechFirst()) {
            ((TechSorter) techSorter.getComparator(0)).setPart(part);
        }
        techSorter.setRowFilter(techTypeFilter);
    }

    public void focusOnUnit(UUID id) {
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
        }
    }

    public void refreshServicedUnitList() {
        int selected = servicedUnitTable.getSelectedRow();
        servicedUnitModel.setData(getCampaign().getServiceableUnits());
        if (selected == servicedUnitTable.getRowCount()) {
            selected--;
        }
        if ((selected > -1) && (selected < servicedUnitTable.getRowCount())) {
            servicedUnitTable.setRowSelectionInterval(selected, selected);
        }
        getCampaignGui().refreshRating();
    }

    public void refreshTaskList() {
        UUID uuid = null;
        if (null != getSelectedServicedUnit()) {
            uuid = getSelectedServicedUnit().getId();
        }
        taskModel.setData(getCampaign().getPartsNeedingServiceFor(uuid));

        if (getSelectedServicedUnit() != null && getSelectedServicedUnit().getEntity() != null) {
            int index = choiceLocation.getSelectedIndex();
            int numLocations = choiceLocation.getModel().getSize();
            choiceLocation.removeAllItems();
            choiceLocation.addItem("All");
            for (String s : getSelectedServicedUnit().getEntity().getLocationAbbrs()) {
                choiceLocation.addItem(s);
            }
            choiceLocation.setSelectedIndex(0);
            if (index > -1 && choiceLocation.getModel().getSize() == numLocations) {
                choiceLocation.setSelectedIndex(index);
            }
            choiceLocation.setEnabled(true);
        } else {
            choiceLocation.removeAllItems();
            choiceLocation.setEnabled(false);
        }
        filterTasks();
    }

    public void refreshTechsList() {
        int selected = techTable.getSelectedRow();
        ArrayList<Person> techs = getCampaign().getTechs(true, null);
        techsModel.setData(techs);
        if ((selected > -1) && (selected < techs.size())) {
            techTable.setRowSelectionInterval(selected, selected);
        }
        String astechString = "<html><b>Astech Pool Minutes:</> " + getCampaign().getAstechPoolMinutes();
        if (getCampaign().isOvertimeAllowed()) {
            astechString += " [" + getCampaign().getAstechPoolOvertime() + " overtime]";
        }
        astechString += " (" + getCampaign().getNumberAstechs() + " Astechs)</html>";
        astechPoolLabel.setText(astechString); // NOI18N
    }

    public void refreshAcquireList() {
        UUID uuid = null;
        if (null != getSelectedServicedUnit()) {
            uuid = getSelectedServicedUnit().getId();
        }
        acquireModel.setData(getCampaign().getAcquisitionsForUnit(uuid));
    }
    
    private ActionScheduler servicedUnitListScheduler = new ActionScheduler(this::refreshServicedUnitList);
    private ActionScheduler techsScheduler = new ActionScheduler(this::refreshTechsList);
    private ActionScheduler taskScheduler = new ActionScheduler(this::refreshTaskList);
    private ActionScheduler acquireScheduler = new ActionScheduler(this::refreshAcquireList);

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
}
