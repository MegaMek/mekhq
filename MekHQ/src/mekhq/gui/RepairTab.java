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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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
import mekhq.campaign.event.AstechPoolChangedEvent;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.OvertimeModeEvent;
import mekhq.campaign.event.PartEvent;
import mekhq.campaign.event.PartWorkEvent;
import mekhq.campaign.event.PersonEvent;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.event.RepairStatusChangedEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.event.UnitEvent;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PodSpace;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;
import mekhq.gui.adapter.ServicedUnitsTableMouseAdapter;
import mekhq.gui.adapter.TaskTableMouseAdapter;
import mekhq.gui.dialog.AcquisitionsDialog;
import mekhq.gui.dialog.MassRepairSalvageDialog;
import mekhq.gui.model.TaskTableModel;
import mekhq.gui.model.TechTableModel;
import mekhq.gui.model.UnitTableModel;
import mekhq.gui.model.XTableColumnModel;
import mekhq.gui.sorter.TaskSorter;
import mekhq.gui.sorter.TechSorter;
import mekhq.gui.sorter.UnitStatusSorter;
import mekhq.gui.sorter.UnitTypeSorter;
import mekhq.service.MassRepairService;
import mekhq.service.PartsAcquisitionService;

/**
 * Shows damaged units and controls for repair.
 *
 */
public final class RepairTab extends CampaignGuiTab implements ITechWorkPanel {

    private static final long serialVersionUID = 6757065427956450309L;

    private JPanel panDoTask;
    private JSplitPane splitServicedUnits;
    private JTable servicedUnitTable;
    private JTable taskTable;
    private JTable techTable;
    private JButton btnDoTask;
    private JToggleButton btnShowAllTechs;
    private JScrollPane scrTextTarget;
    private JLabel lblTargetNum;
    private JTextPane txtServicedUnitView;
    private JTextArea textTarget;
    private JLabel astechPoolLabel;
    private JComboBox<String> choiceLocation;
    private JButton btnAcquisitions;
    
    private UnitTableModel servicedUnitModel;
    private TaskTableModel taskModel;
    private TechTableModel techsModel;

    private TableRowSorter<UnitTableModel> servicedUnitSorter;
    private TableRowSorter<TaskTableModel> taskSorter;
    private TableRowSorter<TechTableModel> techSorter;
    
    //Maintain selections after refresh
    private int selectedRow = -1;
    private int selectedLocation = -1;
    private Unit selectedUnit = null;
    private Person selectedTech = getSelectedTech();
    private boolean ignoreUnitTable = false; // Used to disable selection listener while data is updated.

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

        // Add panel for action buttons
        JPanel actionButtons = new JPanel(new GridBagLayout());

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
            MassRepairService.massRepairSalvageAllUnits(getCampaignGui());
        });

		btnAcquisitions = new JButton();
		btnAcquisitions.setText("Parts"); // NOI18N
		btnAcquisitions.setToolTipText("Show missing/in transit/on order parts");
		btnAcquisitions.setName("btnAcquisitions"); // NOI18N
		btnAcquisitions.addActionListener(ev -> {
			AcquisitionsDialog dlg = new AcquisitionsDialog(getFrame(), true, getCampaignGui());
			dlg.setVisible(true);
		});
		btnAcquisitions.addPropertyChangeListener("counts", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				String txt = "Parts Acquisition";
				
				if (PartsAcquisitionService.getMissingCount() > 0) {
					if (PartsAcquisitionService.getUnavailableCount() > 0) {
						txt += String.format(" (%s missing, %s unavailable)", PartsAcquisitionService.getMissingCount(), PartsAcquisitionService.getUnavailableCount());
					} else {
						txt += String.format(" (%s missing)", PartsAcquisitionService.getMissingCount());
					}
				}
				
				btnAcquisitions.setText(txt);
				
				btnAcquisitions.repaint();
			}
		});
        
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        actionButtons.add(btnMRMSDialog, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        actionButtons.add(btnMRMSInstantAll, gridBagConstraints);
        
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.weightx = 1;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		actionButtons.add(btnAcquisitions, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new Insets(5, 0, 5, 0);
        panServicedUnits.add(actionButtons, gridBagConstraints);

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
                    && i != UnitTableModel.COL_SITE && i != UnitTableModel.COL_TYPE) {
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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panTasks.add(scrollTaskTable, gridBagConstraints);

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

    protected void updateTechTarget() {
        TargetRoll target = null;

        IPartWork part = getSelectedTask();
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

    private void servicedUnitTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
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
                MechView mv = new MechView(unit.getEntity(), true, true);
                txtServicedUnitView.setText("<div style='font: 12pt monospaced'>" + mv.getMechReadoutBasic() + "<br>"
                        + mv.getMechReadoutLoadout() + "</div>");
            }
            if (!unit.equals(selectedUnit)) {
                choiceLocation.setSelectedIndex(0);
            }
            selectedUnit = unit;
        } else {
            selectedUnit = null;
            choiceLocation.setSelectedItem(null);
        }
    }

    private void techTableValueChanged(javax.swing.event.ListSelectionEvent evt) {
        updateTechTarget();

        taskTable.repaint();
    }

    private void doTask() {
        selectedRow = taskTable.getSelectedRow();
        selectedLocation = choiceLocation.getSelectedIndex();
        selectedUnit = getSelectedServicedUnit();
        selectedTech = getSelectedTech();
        IPartWork part = (IPartWork)getSelectedTask();
        if (null == part) {
            return;
        }
        Unit u = part.getUnit();
        if (null != u && u.isSelfCrewed()) {
            selectedTech = u.getEngineer();
        }
        if (null == selectedTech) {
            return;
        }
        if (part instanceof Part && ((Part)part).onBadHipOrShoulder() && !part.isSalvaging()) {
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
        getCampaign().fixPart(part, selectedTech);
        if (null != u) {
            if (!u.isRepairable() && u.getSalvageableParts().size() == 0) {
                selectedRow = -1;
                getCampaign().removeUnit(u.getId());
            }
            if (!getCampaign().getServiceableUnits().contains(u)) {
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
            if (getCampaign().getCampaignOptions().useResetToFirstTech() && techTable.getRowCount() > 0) {
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
            if (selectedUnit == null || getSelectedServicedUnit() == null
                    || !selectedUnit.equals(getSelectedServicedUnit())
                    || selectedLocation >= choiceLocation.getItemCount()) {
                selectedLocation = 0;
            }
            choiceLocation.setSelectedIndex(selectedLocation);
        }

    }

    public void filterTasks() {
        selectedLocation = choiceLocation.getSelectedIndex();
        RowFilter<TaskTableModel, Integer> taskLocationFilter = null;
        final String loc = (String) choiceLocation.getSelectedItem();
        taskLocationFilter = new RowFilter<TaskTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TaskTableModel, ? extends Integer> entry) {
                TaskTableModel taskModel = entry.getModel();
                IPartWork part = (IPartWork)taskModel.getTaskAt(entry.getIdentifier());
                if (part == null) {
                    return false;
                }
                if (loc != null && !loc.isEmpty()) {
                    if (loc.equals("All")) {
                        return true;
                    } else if (loc.equals("OmniPod")) {
                        return part instanceof PodSpace;
                    } else if (part instanceof PodSpace) {
                        return ((PodSpace)part).getLocation() == part.getUnit().getEntity().getLocationFromAbbr(loc);
                    } else {
                        return ((Part)part).isInLocation(loc);
                    }
                }
                return false;

            }
        };
        taskSorter.setRowFilter(taskLocationFilter);
    }

    public void filterTechs() {
        RowFilter<TechTableModel, Integer> techTypeFilter = null;
        final IPartWork part = getSelectedTask();
        final Unit unit = getSelectedServicedUnit();
        techTypeFilter = new RowFilter<TechTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TechTableModel, ? extends Integer> entry) {
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
        ignoreUnitTable = true;
        servicedUnitModel.setData(getCampaign().getServiceableUnits());
        ignoreUnitTable = false;
        if (selected == servicedUnitTable.getRowCount()) {
            selected--;
        }
        if ((selected > -1) && (selected < servicedUnitTable.getRowCount())) {
            servicedUnitTable.setRowSelectionInterval(selected, selected);
        } else {
            refreshTaskList();
        }

        refreshPartsAcquisition();
    }

    public void refreshTaskList() {
        UUID uuid = null;
        if (null != getSelectedServicedUnit()) {
            uuid = getSelectedServicedUnit().getId();
        }
        taskModel.setData(getCampaign().getPartsNeedingServiceFor(uuid));

        if (getSelectedServicedUnit() != null && getSelectedServicedUnit().getEntity() != null) {
            // Retain the selected index while the contents is refreshed.
            int selected = choiceLocation.getSelectedIndex();
            choiceLocation.removeAllItems();
            choiceLocation.addItem("All");
            for (String s : getSelectedServicedUnit().getEntity().getLocationAbbrs()) {
                if (s.equals("WNG")) {
                    choiceLocation.addItem("FSLG");
                } else {
                    choiceLocation.addItem(s);
                }
            }
            if (getSelectedServicedUnit().getEntity().isOmni()) {
                choiceLocation.addItem("OmniPod");
            }
            selectedLocation = selected;
            if (selectedLocation > -1 && choiceLocation.getModel().getSize() > selectedLocation) {
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
        
        if (selectedRow != -1 && taskTable.getRowCount() > 0) {
            if (taskTable.getRowCount() <= selectedRow) {
                selectedRow = taskTable.getRowCount() - 1;
            }
            taskTable.setRowSelectionInterval(selectedRow,
                    selectedRow);
        }
        if (selectedLocation != -1 && choiceLocation.getItemCount() > 0) {
//            if (selectedUnit == null || getSelectedServicedUnit() == null
//                    || !selectedUnit.equals(getSelectedServicedUnit())
//                    || selectedLocation >= choiceLocation.getItemCount()) {
//                selectedLocation = 0;
//            }
            choiceLocation.setSelectedIndex(selectedLocation);
        }
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

        // If requested, switch to top entry
        if(getCampaign().getCampaignOptions().useResetToFirstTech() && techTable.getRowCount() > 0) {
            techTable.setRowSelectionInterval(0, 0);
        } else {
            // Or get the selected tech back
            for (int i = 0; i < techTable.getRowCount(); i++) {
                Person p = techsModel
                        .getTechAt(techTable.convertRowIndexToModel(i));
                if (selectedTech.getId().equals(p.getId())) {
                    techTable.setRowSelectionInterval(i, i);
                    break;
                }
            }
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
    public void handle(AstechPoolChangedEvent ev) {
        filterTechs();
    }
}
