/*
 * Copyright (c) 2017-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.event.Subscribe;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import megamek.common.util.sorter.NaturalOrderComparator;
import megameklab.util.UnitPrintManager;
import mekhq.MekHQ;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.event.*;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.ScenarioTableMouseAdapter;
import mekhq.gui.dialog.*;
import mekhq.gui.model.ScenarioTableModel;
import mekhq.gui.sorter.DateStringComparator;
import mekhq.gui.view.AtBScenarioViewPanel;
import mekhq.gui.view.LanceAssignmentView;
import mekhq.gui.view.MissionViewPanel;
import mekhq.gui.view.ScenarioViewPanel;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Displays Mission/Contract and Scenario details.
 */
public final class BriefingTab extends CampaignGuiTab {
    private JPanel panMission;
    private JPanel panScenario;
    private LanceAssignmentView panLanceAssignment;
    private JSplitPane splitScenario;
    private JSplitPane splitBrief;
    private JTable scenarioTable;
    private MMComboBox<Mission> comboMission;
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

    public int selectedScenario;

    BriefingTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
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
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
                MekHQ.getMHQOptions().getLocale(), new EncodeControl());
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

        comboMission = new MMComboBox<>("comboMission");
        comboMission.addActionListener(ev -> changeMission());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panMission.add(comboMission, gridBagConstraints);

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
        scenarioSorter.setComparator(ScenarioTableModel.COL_DATE, new DateStringComparator());
        scenarioTable.setRowSorter(scenarioSorter);
        scenarioTable.setShowGrid(false);
        ScenarioTableMouseAdapter.connect(getCampaignGui(), scenarioTable, scenarioModel);
        for (int i = 0; i < ScenarioTableModel.N_COL; i++) {
            final TableColumn column = scenarioTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(scenarioModel.getColumnWidth(i));
            column.setCellRenderer(scenarioModel.getRenderer());
        }
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
            comboMission.setSelectedItem(ncd.getContract());
        } else {
            CustomizeMissionDialog cmd = new CustomizeMissionDialog(getFrame(), true, null, getCampaign());
            cmd.setVisible(true);
            this.setVisible(false);
            comboMission.setSelectedItem(cmd.getMission());
        }
    }

    private void editMission() {
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            return;
        }

        if (getCampaign().getCampaignOptions().getUseAtB() && (mission instanceof AtBContract)) {
            CustomizeAtBContractDialog cmd = new CustomizeAtBContractDialog(getFrame(), true,
                    (AtBContract) mission, getCampaign());
            cmd.setVisible(true);
            comboMission.setSelectedItem(cmd.getAtBContract());
        } else {
            CustomizeMissionDialog cmd = new CustomizeMissionDialog(getFrame(), true, mission, getCampaign());
            cmd.setVisible(true);
            comboMission.setSelectedItem(cmd.getMission());
        }
        MekHQ.triggerEvent(new MissionChangedEvent(mission));
    }

    private void completeMission() {
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            return;
        } else if (mission.hasPendingScenarios()) {
            JOptionPane.showMessageDialog(getFrame(), "You cannot complete a mission that has pending scenarios",
                    "Pending Scenarios", JOptionPane.WARNING_MESSAGE);
            return;
        }

        CompleteMissionDialog cmd = new CompleteMissionDialog(getFrame(), true, mission);
        cmd.setVisible(true);
        if (cmd.getStatus().isActive()) {
            return;
        }

        if (getCampaign().getCampaignOptions().getUseAtB() && (mission instanceof AtBContract)) {
            if (((AtBContract) mission).contractExtended(getCampaign())) {
                return;
            }

            if (getCampaign().getCampaignOptions().getRandomRetirementMethod().isAtB()
                    && getCampaign().getCampaignOptions().isUseContractCompletionRandomRetirement()) {
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
                    if ((getCampaign().getRetirementDefectionTracker().getRetirees(mission) != null)
                            && getCampaign().getFinances().getBalance().isGreaterOrEqualThan(rdd.totalPayout())) {
                        for (PersonnelRole role : PersonnelRole.getAdministratorRoles()) {
                            Person admin = getCampaign().findBestInRole(role, SkillType.S_ADMIN);
                            if (admin != null) {
                                admin.awardXP(getCampaign(), 1);
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

        getCampaign().completeMission(mission, cmd.getStatus());
        MekHQ.triggerEvent(new MissionCompletedEvent(mission));

        if (getCampaign().getCampaignOptions().getUseAtB() && (mission instanceof AtBContract)) {
            ((AtBContract) mission).checkForFollowup(getCampaign());
        }

        final List<Mission> missions = getCampaign().getSortedMissions();
        comboMission.setSelectedItem(missions.isEmpty() ? null : missions.get(0));
    }

    private void deleteMission() {
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            LogManager.getLogger().error("Cannot remove null mission");
            return;
        }
        LogManager.getLogger().debug("Attempting to Delete Mission, Mission ID: " + mission.getId());
        if (0 != JOptionPane.showConfirmDialog(null, "Are you sure you want to delete this mission?", "Delete mission?",
                JOptionPane.YES_NO_OPTION)) {
            return;
        }
        getCampaign().removeMission(mission);
        final List<Mission> missions = getCampaign().getSortedMissions();
        comboMission.setSelectedItem(missions.isEmpty() ? null : missions.get(0));
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
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            return;
        }

        CustomizeScenarioDialog csd = new CustomizeScenarioDialog(getFrame(), true, null, mission, getCampaign());
        csd.setVisible(true);
        //need to update the scenario table and refresh the scroll view
        refreshScenarioTableData();
        scrollMissionView.revalidate();
        scrollMissionView.repaint();
    }

    private void clearAssignedUnits() {
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

    private void resolveScenario() {
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

    private void printRecordSheets() {
        final int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        final Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }

        // First, we need to get all units assigned to the current scenario
        final List<UUID> unitIds = scenario.getForces(getCampaign()).getAllUnits(true);

        // Then, we need to convert the ids to units, and filter out any units that are null and
        // any units with null entities
        final List<Unit> units = unitIds.stream().map(unitId -> getCampaign().getUnit(unitId))
                .filter(unit -> (unit != null) && (unit.getEntity() != null))
                .collect(Collectors.toList());

        final List<Entity> chosen = new ArrayList<>();
        final StringBuilder undeployed = new StringBuilder();

        for (final Unit unit : units) {
            if (unit.checkDeployment() == null) {
                chosen.add(unit.getEntity());
            } else {
                undeployed.append("\n").append(unit.getName()).append(" (").append(unit.checkDeployment()).append(")");
            }
        }

        if (undeployed.length() > 0) {
            final Object[] options = { "Continue", "Cancel" };
            if (JOptionPane.showOptionDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed,
                    "Could not deploy some units", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[1]) == JOptionPane.NO_OPTION) {
                return;
            }
        }

        if (scenario instanceof AtBScenario) {
            // Also print off allied sheets
            chosen.addAll(((AtBScenario) scenario).getAlliesPlayer());
        }

        // add bot forces
        chosen.addAll(scenario.getBotForces().stream()
                .flatMap(botForce -> botForce.getFullEntityList(getCampaign()).stream())
                .collect(Collectors.toList()));

        if (!chosen.isEmpty()) {
            UnitPrintManager.printAllUnits(chosen, true);
        }
    }

    private void loadScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (null != scenario) {
            getCampaignGui().getApplication().startHost(scenario, true, new ArrayList<>());
        }
    }

    private void startScenario() {
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
                    "The following units could not be deployed:" + undeployed, "Could not deploy some units",
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

        if (!chosen.isEmpty()) {
            // Ensure that the MegaMek year GameOption matches the campaign year
            getCampaign().getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(getCampaign().getGameYear());
            getCampaignGui().getApplication().startHost(scenario, false, chosen);
        }
    }

    private void joinScenario() {
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
                    "The following units could not be deployed:" + undeployed, "Could not deploy some units",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (n == 1) {
                return;
            }
        }

        if (!chosen.isEmpty()) {
            getCampaignGui().getApplication().joinGame(scenario, chosen);
        }
    }

    private void deployListFile() {
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
                    "The following units could not be deployed:" + undeployed, "Could not deploy some units",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            if (n == 1) {
                return;
            }
        }

        Optional<File> maybeUnitFile = FileDialogs.saveDeployUnits(getFrame(), scenario);

        if (maybeUnitFile.isEmpty()) {
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
            LogManager.getLogger().error("", e);
        }

        if (undeployed.length() > 0) {
            JOptionPane.showMessageDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed,
                    "Could not deploy some units", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refreshMissions() {
        comboMission.removeAllItems();
        final List<Mission> missions = getCampaign().getSortedMissions();
        for (final Mission mission : missions) {
            comboMission.addItem(mission);
        }

        if ((comboMission.getSelectedIndex() == -1) && !missions.isEmpty()) {
            comboMission.setSelectedIndex(0);
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
            scrollScenarioView.setViewportView(new ScenarioViewPanel(getFrame(), getCampaign(), scenario));
        }
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        SwingUtilities.invokeLater(() -> scrollScenarioView.getVerticalScrollBar().setValue(0));

        final boolean canStartGame = scenario.canStartScenario(getCampaign());
        btnStartGame.setEnabled(canStartGame);
        btnJoinGame.setEnabled(canStartGame);
        btnLoadGame.setEnabled(canStartGame);
        btnGetMul.setEnabled(canStartGame);
        btnClearAssignedUnits.setEnabled(canStartGame);
        btnResolveScenario.setEnabled(canStartGame);
        btnPrintRS.setEnabled(canStartGame);
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
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            scrollMissionView.setViewportView(null);
            btnEditMission.setEnabled(false);
            btnCompleteMission.setEnabled(false);
            btnDeleteMission.setEnabled(false);
            btnAddScenario.setEnabled(false);
            btnGMGenerateScenarios.setEnabled(false);
        } else {
            scrollMissionView.setViewportView(new MissionViewPanel(mission, scenarioTable, getCampaignGui()));
            // This odd code is to make sure that the scrollbar stays at the top
            // I can't just call it here, because it ends up getting reset somewhere later
            SwingUtilities.invokeLater(() -> scrollMissionView.getVerticalScrollBar().setValue(0));
            btnEditMission.setEnabled(true);
            btnCompleteMission.setEnabled(mission.getStatus().isActive());
            btnDeleteMission.setEnabled(true);
            btnAddScenario.setEnabled(mission.isActiveOn(getCampaign().getLocalDate()));
            btnGMGenerateScenarios.setEnabled(mission.isActiveOn(getCampaign().getLocalDate()) && getCampaign().isGM());
        }
        refreshScenarioTableData();
    }

    public void refreshScenarioTableData() {
        final Mission mission = comboMission.getSelectedItem();
        scenarioModel.setData((mission == null) ? new ArrayList<Scenario>() : mission.getVisibleScenarios());
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
    public void handle(ScenarioChangedEvent evt) {
        final Mission mission = comboMission.getSelectedItem();
        if ((evt.getScenario() != null)
                && (((mission == null) && (evt.getScenario().getMissionId() == -1))
                || (mission != null) && (evt.getScenario().getMissionId() == mission.getId()))) {
            scenarioTable.repaint();
            if (evt.getScenario().getId() == selectedScenario) {
                scenarioViewScheduler.schedule();
            }
            scenarioDataScheduler.schedule();
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
    public void handle(MissionChangedEvent evt) {
        final Mission mission = comboMission.getSelectedItem();
        if ((mission != null) && (evt.getMission().getId() == mission.getId())) {
            changeMission();
        }
    }

    @Subscribe
    public void handle(GMModeEvent ev) {
        btnGMGenerateScenarios.setEnabled(ev.isGMMode());
    }
}
