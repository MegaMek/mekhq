/*
 * Copyright (c) 2017-2020 - The MegaMek Team. All rights reserved.
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
package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;

import javax.swing.*;
import javax.swing.table.TableRowSorter;

import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import megameklab.com.util.UnitPrintManager;
import mekhq.MekHQ;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.event.GMModeEvent;
import mekhq.campaign.event.MissionChangedEvent;
import mekhq.campaign.event.MissionCompletedEvent;
import mekhq.campaign.event.MissionNewEvent;
import mekhq.campaign.event.MissionRemovedEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.ScenarioChangedEvent;
import mekhq.campaign.event.ScenarioNewEvent;
import mekhq.campaign.event.ScenarioRemovedEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.ScenarioTableMouseAdapter;
import mekhq.gui.dialog.ChooseMulFilesDialog;
import mekhq.gui.dialog.CompleteMissionDialog;
import mekhq.gui.dialog.CustomizeAtBContractDialog;
import mekhq.gui.dialog.CustomizeMissionDialog;
import mekhq.gui.dialog.CustomizeScenarioDialog;
import mekhq.gui.dialog.MissionTypeDialog;
import mekhq.gui.dialog.NewAtBContractDialog;
import mekhq.gui.dialog.NewContractDialog;
import mekhq.gui.dialog.ResolveScenarioWizardDialog;
import mekhq.gui.dialog.RetirementDefectionDialog;
import mekhq.gui.model.ScenarioTableModel;
import mekhq.gui.sorter.DateStringComparator;
import mekhq.gui.sorter.ScenarioStatusComparator;
import mekhq.gui.view.AtBScenarioViewPanel;
import mekhq.gui.view.LanceAssignmentView;
import mekhq.gui.view.MissionViewPanel;
import mekhq.gui.view.ScenarioViewPanel;

/**
 * Displays Mission/Contract and Scenario details.
 */
public final class BriefingTab extends CampaignGuiTab {

    private static final long serialVersionUID = 5927572086088284329L;

    private JPanel panMission;
    private JPanel panScenario;
    private LanceAssignmentView panLanceAssignment;
    private JSplitPane splitScenario;
    private JSplitPane splitBrief;
    private JTable scenarioTable;
    private JComboBox<String> choiceMission;
    private JScrollPane scrollMissionView;
    private JScrollPane scrollScenarioView;
    private JPanel panMissionButtons;
    private JPanel panScenarioButtons;
    private JButton btnAddScenario;
    private JButton btnAddMission;
    private JButton btnEditMission;
    private JButton btnCompleteMission;
    private JButton btnDeleteMission;
    private JButton btnGMGenerateScenarios;
    private JButton btnStartGame;
    private JButton btnJoinGame;
    private JButton btnLoadGame;
    private JButton btnPrintRS;
    private JButton btnGetMul;
    private JButton btnClearAssignedUnits;
    private JButton btnResolveScenario;

    private ScenarioTableModel scenarioModel;
    private TableRowSorter<ScenarioTableModel> scenarioSorter;

    public int selectedMission;
    public int selectedScenario;

    BriefingTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
        selectedMission = -1;
        selectedScenario = -1;
        MekHQ.registerHandler(this);
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.BRIEFING;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", new EncodeControl());
        GridBagConstraints gridBagConstraints;

        panMission = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panMission.add(new JLabel(resourceMap.getString("lblMission.text")), gridBagConstraints);

        choiceMission = new JComboBox<>();
        choiceMission.addActionListener(ev -> changeMission());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panMission.add(choiceMission, gridBagConstraints);

        panMissionButtons = new JPanel(new GridLayout(2, 3));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panMission.add(panMissionButtons, gridBagConstraints);

        btnAddMission = new JButton(resourceMap.getString("btnAddMission.text")); // NOI18N
        btnAddMission.setToolTipText(resourceMap.getString("btnAddMission.toolTipText")); // NOI18N
        btnAddMission.addActionListener(ev -> addMission());
        panMissionButtons.add(btnAddMission);

        btnAddScenario = new JButton(resourceMap.getString("btnAddScenario.text")); // NOI18N
        btnAddScenario.setToolTipText(resourceMap.getString("btnAddScenario.toolTipText")); // NOI18N
        btnAddScenario.addActionListener(ev -> addScenario());
        panMissionButtons.add(btnAddScenario);

        btnEditMission = new JButton(resourceMap.getString("btnEditMission.text")); // NOI18N
        btnEditMission.setToolTipText(resourceMap.getString("btnEditMission.toolTipText")); // NOI18N
        btnEditMission.addActionListener(ev -> editMission());
        panMissionButtons.add(btnEditMission);

        btnCompleteMission = new JButton(resourceMap.getString("btnCompleteMission.text")); // NOI18N
        btnCompleteMission.setToolTipText(resourceMap.getString("btnCompleteMission.toolTipText")); // NOI18N
        btnCompleteMission.addActionListener(ev -> completeMission());
        panMissionButtons.add(btnCompleteMission);

        btnDeleteMission = new JButton(resourceMap.getString("btnDeleteMission.text")); // NOI18N
        btnDeleteMission.setToolTipText(resourceMap.getString("btnDeleteMission.toolTipText")); // NOI18N
        btnDeleteMission.setName("btnDeleteMission"); // NOI18N
        btnDeleteMission.addActionListener(ev -> deleteMission());
        panMissionButtons.add(btnDeleteMission);

        btnGMGenerateScenarios = new JButton(resourceMap.getString("btnGMGenerateScenarios.text")); // NOI18N
        btnGMGenerateScenarios.setToolTipText(resourceMap.getString("btnGMGenerateScenarios.toolTipText")); // NOI18N
        btnGMGenerateScenarios.setName("btnGMGenerateScenarios"); // NOI18N
        btnGMGenerateScenarios.addActionListener(ev -> gmGenerateScenarios());
        panMissionButtons.add(btnGMGenerateScenarios);

        scrollMissionView = new JScrollPane();
        scrollMissionView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMissionView.setViewportView(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panMission.add(scrollMissionView, gridBagConstraints);

        scenarioModel = new ScenarioTableModel(getCampaign());
        scenarioTable = new JTable(scenarioModel);
        scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        scenarioSorter = new TableRowSorter<>(scenarioModel);
        scenarioSorter.setComparator(ScenarioTableModel.COL_NAME, new NaturalOrderComparator());
        scenarioSorter.setComparator(ScenarioTableModel.COL_STATUS, new ScenarioStatusComparator());
        scenarioSorter.setComparator(ScenarioTableModel.COL_DATE, new DateStringComparator());
        scenarioTable.setRowSorter(scenarioSorter);
        scenarioTable.setShowGrid(false);
        scenarioTable.addMouseListener(new ScenarioTableMouseAdapter(getCampaignGui(), scenarioTable, scenarioModel));
        scenarioTable.setIntercellSpacing(new Dimension(0, 0));
        scenarioTable.getSelectionModel().addListSelectionListener(ev -> refreshScenarioView());

        panScenario = new JPanel(new GridBagLayout());

        panScenarioButtons = new JPanel(new GridLayout(3, 3));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panScenario.add(panScenarioButtons, gridBagConstraints);

        btnStartGame = new JButton(resourceMap.getString("btnStartGame.text"));
        btnStartGame.setToolTipText(resourceMap.getString("btnStartGame.toolTipText"));
        btnStartGame.addActionListener(ev -> startScenario());
        btnStartGame.setEnabled(false);
        panScenarioButtons.add(btnStartGame);

        btnJoinGame = new JButton(resourceMap.getString("btnJoinGame.text"));
        btnJoinGame.setToolTipText(resourceMap.getString("btnJoinGame.toolTipText"));
        btnJoinGame.addActionListener(ev -> joinScenario());
        btnJoinGame.setEnabled(false);
        panScenarioButtons.add(btnJoinGame);

        btnLoadGame = new JButton(resourceMap.getString("btnLoadGame.text"));
        btnLoadGame.setToolTipText(resourceMap.getString("btnLoadGame.toolTipText"));
        btnLoadGame.addActionListener(ev -> loadScenario());
        btnLoadGame.setEnabled(false);
        panScenarioButtons.add(btnLoadGame);

        btnPrintRS = new JButton(resourceMap.getString("btnPrintRS.text"));
        btnPrintRS.setToolTipText(resourceMap.getString("btnPrintRS.toolTipText"));
        btnPrintRS.addActionListener(ev -> printRecordSheets());
        btnPrintRS.setEnabled(false);
        panScenarioButtons.add(btnPrintRS);

        btnGetMul = new JButton(resourceMap.getString("btnGetMul.text")); // NOI18N
        btnGetMul.setToolTipText(resourceMap.getString("btnGetMul.toolTipText")); // NOI18N
        btnGetMul.setName("btnGetMul"); // NOI18N
        btnGetMul.addActionListener(ev -> deployListFile());
        btnGetMul.setEnabled(false);
        panScenarioButtons.add(btnGetMul);

        btnResolveScenario = new JButton(resourceMap.getString("btnResolveScenario.text")); // NOI18N
        btnResolveScenario.setToolTipText(resourceMap.getString("btnResolveScenario.toolTipText")); // NOI18N
        btnResolveScenario.addActionListener(ev -> resolveScenario());
        btnResolveScenario.setEnabled(false);
        panScenarioButtons.add(btnResolveScenario);

        btnClearAssignedUnits = new JButton(resourceMap.getString("btnClearAssignedUnits.text")); // NOI18N
        btnClearAssignedUnits.setToolTipText(resourceMap.getString("btnClearAssignedUnits.toolTipText")); // NOI18N
        btnClearAssignedUnits.addActionListener(ev -> clearAssignedUnits());
        btnClearAssignedUnits.setEnabled(false);
        panScenarioButtons.add(btnClearAssignedUnits);

        scrollScenarioView = new JScrollPane();
        scrollScenarioView.setViewportView(null);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panScenario.add(scrollScenarioView, gridBagConstraints);

        /* ATB */
        panLanceAssignment = new LanceAssignmentView(getCampaign());
        JScrollPane paneLanceDeployment = new JScrollPane(panLanceAssignment);
        paneLanceDeployment.setMinimumSize(new java.awt.Dimension(200, 300));
        paneLanceDeployment.setPreferredSize(new java.awt.Dimension(200, 300));
        paneLanceDeployment.setVisible(getCampaign().getCampaignOptions().getUseAtB());
        splitScenario = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, panScenario,
                paneLanceDeployment);
        splitScenario.setOneTouchExpandable(true);
        splitScenario.setResizeWeight(1.0);

        splitBrief = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT, panMission, splitScenario);
        splitBrief.setOneTouchExpandable(true);
        splitBrief.setResizeWeight(0.5);
        splitBrief.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> refreshScenarioView());

        setLayout(new BorderLayout());
        add(splitBrief, BorderLayout.CENTER);
    }

    private void addMission() {
        MissionTypeDialog mtd = new MissionTypeDialog(getFrame(), true);
        mtd.setVisible(true);
        if (mtd.isContract()) {
            NewContractDialog ncd = getCampaignOptions().getUseAtB()
                    ? new NewAtBContractDialog(getFrame(), true, getCampaign())
                    : new NewContractDialog(getFrame(), true, getCampaign());
            ncd.setVisible(true);
            this.setVisible(false);
            if (ncd.getContractId() != -1) {
                selectedMission = ncd.getContractId();
            }
        } else {
            CustomizeMissionDialog cmd = new CustomizeMissionDialog(getFrame(), true, null, getCampaign());
            cmd.setVisible(true);
            this.setVisible(false);
            if (cmd.getMissionId() != -1) {
                selectedMission = cmd.getMissionId();
            }
        }
    }

    private void editMission() {
        Mission mission = getCampaign().getMission(selectedMission);
        if (null != mission) {
            if (getCampaign().getCampaignOptions().getUseAtB() && (mission instanceof AtBContract)) {
                CustomizeAtBContractDialog cmd = new CustomizeAtBContractDialog(getFrame(), true,
                        (AtBContract) mission, getCampaign());
                cmd.setVisible(true);
                if (cmd.getMissionId() != -1) {
                    selectedMission = cmd.getMissionId();
                }
            } else {
                CustomizeMissionDialog cmd = new CustomizeMissionDialog(getFrame(), true, mission, getCampaign());
                cmd.setVisible(true);
                if (cmd.getMissionId() != -1) {
                    selectedMission = cmd.getMissionId();
                }
            }
            MekHQ.triggerEvent(new MissionChangedEvent(mission));
        }

    }

    private void completeMission() {
        Mission mission = getCampaign().getMission(selectedMission);
        if (mission == null) {
            return;
        } else if (mission.hasPendingScenarios()) {
            JOptionPane.showMessageDialog(getFrame(), "You cannot complete a mission that has pending scenarios",
                    "Pending Scenarios", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CompleteMissionDialog cmd = new CompleteMissionDialog(getFrame(), true, mission);
        cmd.setVisible(true);
        if (cmd.getStatus() <= Mission.S_ACTIVE) {
            return;
        }

        if (getCampaign().getCampaignOptions().getUseAtB() && (mission instanceof AtBContract)) {
            if (((AtBContract) mission).contractExtended(getCampaign())) {
                return;
            }

            if (getCampaign().getCampaignOptions().doRetirementRolls()) {
                RetirementDefectionDialog rdd = new RetirementDefectionDialog(getCampaignGui(),
                        (AtBContract) mission, true);
                rdd.setVisible(true);
                if (rdd.wasAborted()) {
                    /*
                     * Once the retirement rolls have been made, the outstanding payouts can be resolved
                     * without reference to the contract and the dialog can be accessed through the menu
                     * provided they aren't still assigned to the mission in question.
                     */
                    if (!getCampaign().getRetirementDefectionTracker().isOutstanding(mission.getId())) {
                        return;
                    }
                } else {
                    if ((getCampaign().getRetirementDefectionTracker().getRetirees((AtBContract) mission) != null)
                            && getCampaign().getFinances().getBalance().isGreaterOrEqualThan(rdd.totalPayout())) {
                        final int[] admins = {Person.T_ADMIN_COM, Person.T_ADMIN_HR,
                                Person.T_ADMIN_LOG, Person.T_ADMIN_TRA};
                        for (int role : admins) {
                            Person admin = getCampaign().findBestInRole(role, SkillType.S_ADMIN);
                            if (admin != null) {
                                admin.awardXP(1);
                                getCampaign().addReport(admin.getHyperlinkedName() + " has gained 1 XP.");
                            }
                        }
                    }

                    if (!getCampaign().applyRetirement(rdd.totalPayout(), rdd.getUnitAssignments())) {
                        return;
                    }
                }
            }
        }

        getCampaign().completeMission(mission.getId(), cmd.getStatus());
        MekHQ.triggerEvent(new MissionCompletedEvent(mission));

        if (getCampaign().getCampaignOptions().getUseAtB() && (mission instanceof AtBContract)) {
            ((AtBContract) mission).checkForFollowup(getCampaign());
        }

        if (getCampaign().getSortedMissions().size() > 0) {
            selectedMission = getCampaign().getSortedMissions().get(0).getId();
        } else {
            selectedMission = -1;
        }
    }

    private void deleteMission() {
        Mission mission = getCampaign().getMission(selectedMission);
        MekHQ.getLogger().debug("Attempting to Delete Mission, Mission ID: " + mission.getId());
        if (0 != JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this mission?", "Delete mission?",
                JOptionPane.YES_NO_OPTION)) {
            return;
        }
        getCampaign().removeMission(mission.getId());
        if (getCampaign().getSortedMissions().size() > 0) {
            selectedMission = getCampaign().getSortedMissions().get(0).getId();
        } else {
            selectedMission = -1;
        }
        MekHQ.triggerEvent(new MissionRemovedEvent(mission));
    }

    private void gmGenerateScenarios() {
        if (!getCampaign().isGM()) {
            JOptionPane.showMessageDialog(this,
                    "Only allowed for GM players",
                    "Not GM", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (0 != JOptionPane.showConfirmDialog(null, "Are you sure you want to generate a new set of scenarios?", "Generate scenarios?",
                JOptionPane.YES_NO_OPTION)) {
            return;
        }

        AtBScenarioFactory.createScenariosForNewWeek(getCampaign());
    }

    private void addScenario() {
        Mission m = getCampaign().getMission(selectedMission);
        if (null != m) {
            CustomizeScenarioDialog csd = new CustomizeScenarioDialog(getFrame(), true, null, m, getCampaign());
            csd.setVisible(true);
        }
        //need to update the scenario table and refresh the scroll view
        refreshScenarioTableData();
        scrollMissionView.revalidate();
        scrollMissionView.repaint();
    }

    protected void clearAssignedUnits() {
        if (0 == JOptionPane.showConfirmDialog(null, "Do you really want to remove all units from this scenario?",
                "Clear Units?", JOptionPane.YES_NO_OPTION)) {
            int row = scenarioTable.getSelectedRow();
            Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
            if (null == scenario) {
                return;
            }
            scenario.clearAllForcesAndPersonnel(getCampaign());
        }
    }

    protected void resolveScenario() {
        int row = scenarioTable.getSelectedRow();
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (null == scenario) {
            return;
        }
        boolean control = JOptionPane.showConfirmDialog(getFrame(),
                "Did your side control the battlefield at the end of the scenario?", "Control of Battlefield?",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
        ResolveScenarioTracker tracker = new ResolveScenarioTracker(scenario, getCampaign(), control);
        ChooseMulFilesDialog chooseFilesDialog = new ChooseMulFilesDialog(getFrame(), true, tracker);
        chooseFilesDialog.setVisible(true);
        if (chooseFilesDialog.wasCancelled()) {
            return;
        }
        // tracker.postProcessEntities(control);
        ResolveScenarioWizardDialog resolveDialog = new ResolveScenarioWizardDialog(getFrame(), true, tracker);
        resolveDialog.setVisible(true);
        if (getCampaign().getCampaignOptions().getUseAtB()
                && getCampaign().getMission(scenario.getMissionId()) instanceof AtBContract
                && getCampaign().getRetirementDefectionTracker().getRetirees().size() > 0) {
            RetirementDefectionDialog rdd = new RetirementDefectionDialog(getCampaignGui(),
                    (AtBContract) getCampaign().getMission(scenario.getMissionId()), false);
            rdd.setVisible(true);
            if (!rdd.wasAborted()) {
                getCampaign().applyRetirement(rdd.totalPayout(), rdd.getUnitAssignments());
            }
        }

        MekHQ.triggerEvent(new ScenarioResolvedEvent(scenario));
    }

    protected void printRecordSheets() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(true);
        if (uids.size() == 0) {
            return;
        }

        Vector<Entity> chosen = new Vector<>();
        // ArrayList<Unit> toDeploy = new ArrayList<>();
        StringBuilder undeployed = new StringBuilder();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    chosen.add(u.getEntity());
                } else {
                    undeployed.append("\n").append(u.getName()).append(" (").append(u.checkDeployment()).append(")");
                }
            }
        }

        if (undeployed.length() > 0) {
            Object[] options = { "Continue", "Cancel" };
            int n = JOptionPane.showOptionDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed.toString(), "Could not deploy some units",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (n == 1) {
                return;
            }
        }

        if (chosen.size() > 0) {
            UnitPrintManager.printAllUnits(chosen, true);
        }
    }

    protected void loadScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (null != scenario) {
            getCampaignGui().getApplication().startHost(scenario, true, null);
        }
    }

    protected void startScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(true);
        if (uids.size() == 0) {
            return;
        }

        List<Unit> chosen = new ArrayList<>();
        StringBuilder undeployed = new StringBuilder();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if ((null != u) && (null != u.getEntity())) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to
                    // date!
                    u.resetPilotAndEntity();

                    // Add and run
                    chosen.add(u);

                } else {
                    undeployed.append("\n").append(u.getName()).append(" (").append(u.checkDeployment()).append(")");
                }
            }
        }

        if (scenario instanceof AtBDynamicScenario) {
            AtBDynamicScenarioFactory.setPlayerDeploymentTurns((AtBDynamicScenario) scenario, getCampaign());
            AtBDynamicScenarioFactory.finalizeStaggeredDeploymentTurns((AtBDynamicScenario) scenario, getCampaign());
            AtBDynamicScenarioFactory.setPlayerDeploymentZones((AtBDynamicScenario) scenario, getCampaign());
        }

        if (undeployed.length() > 0) {
            Object[] options = { "Continue", "Cancel" };
            int n = JOptionPane.showOptionDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed.toString(), "Could not deploy some units",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);

            if (n == 1) {
                return;
            }
        }

        // code to support deployment of reinforcements for legacy ATB scenarios.
        if ((scenario instanceof AtBScenario) && !(scenario instanceof AtBDynamicScenario)) {
            Lance assignedLance = ((AtBScenario) scenario).getLance(getCampaign());
            if (assignedLance != null) {
                int assignedForceId = assignedLance.getForceId();
                int cmdrStrategy = 0;
                Person commander = getCampaign().getPerson(Lance.findCommander(assignedForceId, getCampaign()));
                if ((null != commander) && (null != commander.getSkill(SkillType.S_STRATEGY))) {
                    cmdrStrategy = commander.getSkill(SkillType.S_STRATEGY).getLevel();
                }
                List<Entity> reinforcementEntities = new ArrayList<>();

                for (Unit unit : chosen) {
                    if (unit.getForceId() != assignedForceId) {
                        reinforcementEntities.add(unit.getEntity());
                    }
                }

                AtBDynamicScenarioFactory.setDeploymentTurnsForReinforcements(reinforcementEntities, cmdrStrategy);
            }
        }

        if (getCampaign().getCampaignOptions().getUseAtB() && (scenario instanceof AtBScenario)) {
            ((AtBScenario) scenario).refresh(getCampaign());
        }

        if (chosen.size() > 0) {
            // Ensure that the MegaMek year GameOption matches the campaign year
            getCampaign().getGameOptions().getOption("year").setValue(getCampaign().getGameYear());
            getCampaignGui().getApplication().startHost(scenario, false, chosen);
        }
    }

    protected void joinScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(true);
        if (uids.size() == 0) {
            return;
        }

        ArrayList<Unit> chosen = new ArrayList<>();
        // ArrayList<Unit> toDeploy = new ArrayList<>();
        StringBuilder undeployed = new StringBuilder();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to
                    // date!
                    u.resetPilotAndEntity();

                    // Add and run
                    chosen.add(u);

                } else {
                    undeployed.append("\n").append(u.getName()).append(" (").append(u.checkDeployment()).append(")");
                }
            }
        }

        if (undeployed.length() > 0) {
            Object[] options = { "Continue", "Cancel" };
            int n = JOptionPane.showOptionDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed.toString(), "Could not deploy some units",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (n == 1) {
                return;
            }
        }

        if (chosen.size() > 0) {
            getCampaignGui().getApplication().joinGame(scenario, chosen);
        }
    }

    protected void deployListFile() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(true);
        if (uids.size() == 0) {
            return;
        }

        ArrayList<Entity> chosen = new ArrayList<>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuilder undeployed = new StringBuilder();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to
                    // date!
                    u.resetPilotAndEntity();

                    // Add the entity
                    chosen.add(u.getEntity());
                } else {
                    undeployed.append("\n").append(u.getName()).append(" (").append(u.checkDeployment()).append(")");
                }
            }
        }

        if (undeployed.length() > 0) {
            Object[] options = { "Continue", "Cancel" };
            int n = JOptionPane.showOptionDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed.toString(), "Could not deploy some units",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (n == 1) {
                return;
            }
        }

        Optional<File> maybeUnitFile = FileDialogs.saveDeployUnits(getFrame(), scenario);

        if (!maybeUnitFile.isPresent()) {
            return;
        }

        File unitFile = maybeUnitFile.get();

        if (!(unitFile.getName().toLowerCase().endsWith(".mul") //$NON-NLS-1$
                || unitFile.getName().toLowerCase().endsWith(".xml"))) { //$NON-NLS-1$
            try {
                unitFile = new File(unitFile.getCanonicalPath() + ".mul"); //$NON-NLS-1$
            } catch (IOException ie) {
                // nothing needs to be done here
                return;
            }
        }

        try {
            // Save the player's entities to the file.
            // FIXME: this is not working
            EntityListFile.saveTo(unitFile, chosen);
        } catch (IOException e) {
            MekHQ.getLogger().error(this, e);
        }

        if (undeployed.length() > 0) {
            JOptionPane.showMessageDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed.toString(),
                    "Could not deploy some units", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refreshMissions() {
        choiceMission.removeAllItems();
        for (Mission m : getCampaign().getSortedMissions()) {
            String desc = m.getName();
            if (!m.isActive()) {
                desc += " (Complete)";
            }
            choiceMission.addItem(desc);
            if (m.getId() == selectedMission) {
                choiceMission.setSelectedItem(m.getName());
            }
        }
        if (choiceMission.getSelectedIndex() == -1 && getCampaign().getSortedMissions().size() > 0) {
            selectedMission = getCampaign().getSortedMissions().get(0).getId();
            choiceMission.setSelectedIndex(0);
        }
        changeMission();
        if (getCampaign().getCampaignOptions().getUseAtB()) {
            refreshLanceAssignments();
        }
    }

    public void refreshScenarioView() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            scrollScenarioView.setViewportView(null);
            btnStartGame.setEnabled(false);
            btnJoinGame.setEnabled(false);
            btnLoadGame.setEnabled(false);
            btnGetMul.setEnabled(false);
            btnClearAssignedUnits.setEnabled(false);
            btnResolveScenario.setEnabled(false);
            btnPrintRS.setEnabled(false);
            selectedScenario = -1;
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }
        selectedScenario = scenario.getId();
        if (getCampaign().getCampaignOptions().getUseAtB() && (scenario instanceof AtBScenario)) {
            scrollScenarioView.setViewportView(
                    new AtBScenarioViewPanel((AtBScenario) scenario, getCampaign(), getFrame()));
        } else {
            scrollScenarioView.setViewportView(new ScenarioViewPanel(scenario, getCampaign()));
        }
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(() -> scrollScenarioView.getVerticalScrollBar().setValue(0));
        boolean unitsAssigned = scenario.getForces(getCampaign()).getAllUnits(true).size() > 0;
        boolean canStartGame = scenario.isCurrent() && unitsAssigned;
        if (getCampaign().getCampaignOptions().getUseAtB() && scenario instanceof AtBScenario) {
            canStartGame = canStartGame && getCampaign().getLocalDate().equals(scenario.getDate());
        }
        btnStartGame.setEnabled(canStartGame);
        btnJoinGame.setEnabled(canStartGame);
        btnLoadGame.setEnabled(canStartGame);
        btnGetMul.setEnabled(scenario.isCurrent() && unitsAssigned);
        btnClearAssignedUnits.setEnabled(scenario.isCurrent() && unitsAssigned);
        btnResolveScenario.setEnabled(canStartGame);
        btnPrintRS.setEnabled(scenario.isCurrent() && unitsAssigned);
    }

    public void refreshLanceAssignments() {
        panLanceAssignment.refresh();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshMissions();
        refreshScenarioTableData();
    }

    public void changeMission() {
        int idx = choiceMission.getSelectedIndex();
        btnEditMission.setEnabled(false);
        btnCompleteMission.setEnabled(false);
        btnDeleteMission.setEnabled(false);
        btnAddScenario.setEnabled(false);
        btnGMGenerateScenarios.setEnabled(false);
        if (idx >= 0 && idx < getCampaign().getSortedMissions().size()) {
            Mission m = getCampaign().getSortedMissions().get(idx);
            if (null != m) {
                selectedMission = m.getId();
                scrollMissionView.setViewportView(new MissionViewPanel(m, scenarioTable, getCampaignGui()));
                // This odd code is to make sure that the scrollbar stays at the
                // top
                // I can't just call it here, because it ends up getting reset
                // somewhere later
                javax.swing.SwingUtilities.invokeLater(() -> scrollMissionView.getVerticalScrollBar().setValue(0));
                btnEditMission.setEnabled(true);
                btnCompleteMission.setEnabled(m.isActive());
                btnDeleteMission.setEnabled(true);
                btnAddScenario.setEnabled(m.isActive());
                btnGMGenerateScenarios.setEnabled(m.isActive() && getCampaign().isGM());
            }

        } else {
            selectedMission = -1;
            scrollMissionView.setViewportView(null);
        }
        refreshScenarioTableData();
    }

    public void refreshScenarioTableData() {
        Mission m = getCampaign().getMission(selectedMission);
        if (null != m) {
            scenarioModel.setData(m.getScenarios());
        } else {
            scenarioModel.setData(new ArrayList<Scenario>());
        }
        selectedScenario = -1;
        scenarioTable.setPreferredScrollableViewportSize(scenarioTable.getPreferredSize());
        scenarioTable.setFillsViewportHeight(true);
    }

    private ActionScheduler scenarioDataScheduler = new ActionScheduler(this::refreshScenarioTableData);
    private ActionScheduler scenarioViewScheduler = new ActionScheduler(this::refreshScenarioView);
    private ActionScheduler missionsScheduler = new ActionScheduler(this::refreshMissions);
    private ActionScheduler lanceAssignmentScheduler = new ActionScheduler(this::refreshLanceAssignments);

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        splitScenario.getBottomComponent().setVisible(getCampaignOptions().getUseAtB());
        splitScenario.resetToPreferredSizes();
    }

    @Subscribe
    public void handle(ScenarioChangedEvent ev) {
        if (ev.getScenario() != null && ev.getScenario().getMissionId() == selectedMission) {
            scenarioTable.repaint();
            if (ev.getScenario().getId() == selectedScenario) {
                scenarioViewScheduler.schedule();
            }
        }
    }

    @Subscribe
    public void handle(ScenarioResolvedEvent ev) {
        missionsScheduler.schedule();
    }

    @Subscribe
    public void handle(OrganizationChangedEvent ev) {
        scenarioDataScheduler.schedule();
        if (getCampaignOptions().getUseAtB()) {
            lanceAssignmentScheduler.schedule();
        }
    }

    @Subscribe
    public void handle(ScenarioNewEvent ev) {
        scenarioDataScheduler.schedule();
    }

    @Subscribe
    public void handle(ScenarioRemovedEvent ev) {
        scenarioDataScheduler.schedule();
    }

    @Subscribe
    public void handle(MissionNewEvent ev) {
        missionsScheduler.schedule();
    }

    @Subscribe
    public void handle(MissionRemovedEvent ev) {
        missionsScheduler.schedule();
    }

    @Subscribe
    public void handle(MissionCompletedEvent ev) {
        missionsScheduler.schedule();
    }

    @Subscribe
    public void handle(MissionChangedEvent ev) {
        if (ev.getMission().getId() == selectedMission) {
            changeMission();
        }
    }

    @Subscribe
    public void handle(GMModeEvent ev) {
        btnGMGenerateScenarios.setEnabled(ev.isGMMode());
    }
}
