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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;

import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.event.Subscribe;
import megamek.common.options.GameOptions;
import megamek.common.util.EncodeControl;
import megameklab.com.util.UnitPrintManager;
import mekhq.MekHQ;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.event.MissionChangedEvent;
import mekhq.campaign.event.MissionNewEvent;
import mekhq.campaign.event.MissionRemovedEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.ScenarioChangedEvent;
import mekhq.campaign.event.ScenarioNewEvent;
import mekhq.campaign.event.ScenarioRemovedEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
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
import mekhq.gui.view.AtBContractViewPanel;
import mekhq.gui.view.AtBScenarioViewPanel;
import mekhq.gui.view.ContractViewPanel;
import mekhq.gui.view.LanceAssignmentView;
import mekhq.gui.view.MissionViewPanel;
import mekhq.gui.view.ScenarioViewPanel;

/**
 * Displays Mission/Contract and Scenario details.
 *
 */
public final class BriefingTab extends CampaignGuiTab {

    private static final long serialVersionUID = 5927572086088284329L;

    private JPanel panBriefing;
    private JPanel panScenario;
    private LanceAssignmentView panLanceAssignment;
    private JSplitPane splitScenario;
    private JSplitPane splitBrief;
    private JSplitPane splitMission;
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
    private JButton btnStartGame;
    private JButton btnJoinGame;
    private JButton btnLoadGame;
    private JButton btnPrintRS;
    private JButton btnGetMul;
    private JButton btnClearAssignedUnits;
    private JButton btnResolveScenario;

    private ScenarioTableModel scenarioModel;

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
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        panBriefing = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.CENTER;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        panBriefing.add(new JLabel(resourceMap.getString("lblMission.text")), //$NON-NLS-1$ ;
                gridBagConstraints);

        choiceMission = new JComboBox<String>();
        choiceMission.addActionListener(ev -> changeMission());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panBriefing.add(choiceMission, gridBagConstraints);

        panMissionButtons = new JPanel(new GridLayout(2, 3));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panBriefing.add(panMissionButtons, gridBagConstraints);

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
        panBriefing.add(scrollMissionView, gridBagConstraints);

        scenarioModel = new ScenarioTableModel(getCampaign());
        scenarioTable = new JTable(scenarioModel);
        scenarioTable.setShowGrid(false);
        scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioTable.addMouseListener(new ScenarioTableMouseAdapter(getCampaignGui(),
                scenarioTable, scenarioModel));
        scenarioTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        scenarioTable.setIntercellSpacing(new Dimension(0, 0));
        scenarioTable.getSelectionModel().addListSelectionListener(ev -> refreshScenarioView());
        JScrollPane scrollScenarioTable = new JScrollPane(scenarioTable);
        scrollScenarioTable.setMinimumSize(new java.awt.Dimension(200, 200));
        scrollScenarioTable.setPreferredSize(new java.awt.Dimension(200, 200));

        splitMission = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panBriefing, scrollScenarioTable);
        splitMission.setOneTouchExpandable(true);
        splitMission.setResizeWeight(1.0);

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

        btnStartGame = new JButton(resourceMap.getString("btnStartGame.text")); // NOI18N
        btnStartGame.setToolTipText(resourceMap.getString("btnStartGame.toolTipText")); // NOI18N
        btnStartGame.addActionListener(ev -> startScenario());
        btnStartGame.setEnabled(false);
        panScenarioButtons.add(btnStartGame);

        btnJoinGame = new JButton(resourceMap.getString("btnJoinGame.text")); // NOI18N
        btnJoinGame.setToolTipText(resourceMap.getString("btnJoinGame.toolTipText")); // NOI18N
        btnJoinGame.addActionListener(ev -> joinScenario());
        btnJoinGame.setEnabled(false);
        panScenarioButtons.add(btnJoinGame);

        btnLoadGame = new JButton(resourceMap.getString("btnLoadGame.text")); // NOI18N
        btnLoadGame.setToolTipText(resourceMap.getString("btnLoadGame.toolTipText")); // NOI18N
        btnLoadGame.addActionListener(ev -> loadScenario());
        btnLoadGame.setEnabled(false);
        panScenarioButtons.add(btnLoadGame);

        btnPrintRS = new JButton(resourceMap.getString("btnPrintRS.text")); // NOI18N
        btnPrintRS.setToolTipText(resourceMap.getString("btnPrintRS.toolTipText")); // NOI18N
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
        scrollScenarioView.setMinimumSize(new java.awt.Dimension(450, 600));
        scrollScenarioView.setPreferredSize(new java.awt.Dimension(450, 600));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
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

        splitBrief = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT, splitMission, splitScenario);
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
            if (getCampaign().getCampaignOptions().getUseAtB() && mission instanceof AtBContract) {
                CustomizeAtBContractDialog cmd = new CustomizeAtBContractDialog(getFrame(), true, (AtBContract) mission,
                        getCampaign(), getIconPackage().getCamos());
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
        if (null != mission) {
            if (mission.hasPendingScenarios()) {
                JOptionPane.showMessageDialog(getFrame(), "You cannot complete a mission that has pending scenarios",
                        "Pending Scenarios", JOptionPane.WARNING_MESSAGE);
            } else {
                CompleteMissionDialog cmd = new CompleteMissionDialog(getFrame(), true, mission);
                cmd.setVisible(true);
                if (cmd.getStatus() > Mission.S_ACTIVE) {
                    getCampaign().completeMission(mission.getId(), cmd.getStatus());

                    if (getCampaign().getCampaignOptions().getUseAtB() && mission instanceof AtBContract) {
                        if (((AtBContract) mission).contractExtended(getCampaign())) {
                            mission.setStatus(Mission.S_ACTIVE);
                        } else {
                            if (getCampaign().getCampaignOptions().doRetirementRolls()) {
                                RetirementDefectionDialog rdd = new RetirementDefectionDialog(getCampaignGui(),
                                        (AtBContract) mission, true);
                                rdd.setVisible(true);
                                if (rdd.wasAborted()) {
                                    /*
                                     * Once the retirement rolls have been made,
                                     * the outstanding payouts can be resolved
                                     * without reference to the contract and the
                                     * dialog can be accessed through the menu.
                                     */
                                    if (!getCampaign().getRetirementDefectionTracker().isOutstanding(mission.getId())) {
                                        mission.setStatus(Mission.S_ACTIVE);
                                    }
                                } else {
                                    if (null != getCampaign().getRetirementDefectionTracker()
                                            .getRetirees((AtBContract) mission)
                                            && getCampaign().getFinances().getBalance() >= rdd.totalPayout()) {
                                        final int[] admins = { Person.T_ADMIN_COM, Person.T_ADMIN_HR,
                                                Person.T_ADMIN_LOG, Person.T_ADMIN_TRA };
                                        for (int role : admins) {
                                            Person admin = getCampaign().findBestInRole(role, SkillType.S_ADMIN);
                                            if (admin != null) {
                                                admin.setXp(admin.getXp() + 1);
                                                getCampaign()
                                                        .addReport(admin.getHyperlinkedName() + " has gained 1 XP.");
                                            }
                                        }
                                    }
                                    if (!getCampaign().applyRetirement(rdd.totalPayout(), rdd.getUnitAssignments())) {
                                        mission.setStatus(Mission.S_ACTIVE);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!mission.isActive()) {
                    if (getCampaign().getCampaignOptions().getUseAtB() && mission instanceof AtBContract) {
                        ((AtBContract) mission).checkForFollowup(getCampaign());
                    }
                    if (getCampaign().getSortedMissions().size() > 0) {
                        selectedMission = getCampaign().getSortedMissions().get(0).getId();
                    } else {
                        selectedMission = -1;
                    }
                }
            }
            refreshMissions();
            getCampaignGui().refreshRating();
        }
    }

    private void deleteMission() {
        Mission mission = getCampaign().getMission(selectedMission);
        MekHQ.logMessage("Attempting to Delete Mission, Mission ID: " + mission.getId());
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
        getCampaignGui().refreshRating();
    }

    private void addScenario() {
        Mission m = getCampaign().getMission(selectedMission);
        if (null != m) {
            CustomizeScenarioDialog csd = new CustomizeScenarioDialog(getFrame(), true, null, m, getCampaign());
            csd.setVisible(true);
            refreshScenarioTableData();
        }
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
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        Vector<Entity> chosen = new Vector<Entity>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

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
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        ArrayList<Unit> chosen = new ArrayList<Unit>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to
                    // date!
                    u.resetPilotAndEntity();

                    // Add and run
                    chosen.add(u);

                    // So MegaMek has correct crew sizes
                    u.getEntity().getCrew().setSize(u.getActiveCrew().size());
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

        if (getCampaign().getCampaignOptions().getUseAtB() && scenario instanceof AtBScenario) {
            ((AtBScenario) scenario).refresh(getCampaign());

            /*
             * For standard battles, any deployed unit not part of the lance
             * assigned to the battle is assumed to be reinforcements.
             */
            if (null != ((AtBScenario) scenario).getLance(getCampaign())) {
                int assignedForceId = ((AtBScenario) scenario).getLance(getCampaign()).getForceId();
                int cmdrStrategy = 0;
                Person commander = getCampaign().getPerson(Lance.findCommander(assignedForceId, getCampaign()));
                if (null != commander && null != commander.getSkill(SkillType.S_STRATEGY)) {
                    cmdrStrategy = commander.getSkill(SkillType.S_STRATEGY).getLevel();
                }
                for (Force f : scenario.getForces(getCampaign()).getSubForces()) {
                    if (f.getId() != assignedForceId) {
                        Vector<UUID> units = f.getAllUnits();
                        int slowest = 12;
                        for (UUID id : units) {
                            if (chosen.contains(getCampaign().getUnit(id))) {
                                int speed = getCampaign().getUnit(id).getEntity().getWalkMP();
                                if (getCampaign().getUnit(id).getEntity().getJumpMP() > 0) {
                                    if (getCampaign().getUnit(id).getEntity() instanceof megamek.common.Infantry) {
                                        speed = getCampaign().getUnit(id).getEntity().getJumpMP();
                                    } else {
                                        speed++;
                                    }
                                }
                                slowest = Math.min(slowest, speed);
                            }
                        }
                        int deployRound = Math.max(0, 12 - slowest - cmdrStrategy);

                        for (UUID id : units) {
                            if (chosen.contains(getCampaign().getUnit(id))) {
                                getCampaign().getUnit(id).getEntity().setDeployRound(deployRound);
                            }
                        }
                    }
                }
            }
        }

        if (chosen.size() > 0) {
            // Ensure that the MegaMek year GameOption matches the campaign year
            GameOptions gameOpts = getCampaign().getGameOptions();
            int campaignYear = getCampaign().getCalendar().get(Calendar.YEAR);
            if (gameOpts.intOption("year") != campaignYear) {
                gameOpts.getOption("year").setValue(campaignYear);
            }
            getCampaignGui().getApplication().startHost(scenario, false, chosen);
        }
    }

    protected void joinScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        ArrayList<Unit> chosen = new ArrayList<Unit>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to
                    // date!
                    u.resetPilotAndEntity();

                    // Add and run
                    chosen.add(u);

                    // So MegaMek has correct crew sizes
                    u.getEntity().getCrew().setSize(u.getActiveCrew().size());
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
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits();

        if (uids.size() == 0) {
            return;
        }

        ArrayList<Entity> chosen = new ArrayList<Entity>();
        // ArrayList<Unit> toDeploy = new ArrayList<Unit>();
        StringBuffer undeployed = new StringBuffer();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to
                    // date!
                    u.resetPilotAndEntity();

                    // So MegaMek has correct crew sizes
                    u.getEntity().getCrew().setSize(u.getActiveCrew().size());

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

        JFileChooser saveList = new JFileChooser(".");
        saveList.setDialogTitle("Deploy Units");

        saveList.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                if (dir.isDirectory()) {
                    return true;
                }
                return dir.getName().endsWith(".mul");
            }

            @Override
            public String getDescription() {
                return "MUL file";
            }
        });

        saveList.setSelectedFile(new File(scenario.getName() + ".mul")); //$NON-NLS-1$
        int returnVal = saveList.showSaveDialog(this);

        if ((returnVal != JFileChooser.APPROVE_OPTION) || (saveList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        File unitFile = saveList.getSelectedFile();

        if (unitFile != null) {
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

            } catch (IOException excep) {
                excep.printStackTrace(System.err);
            }
        }

        if (undeployed.length() > 0) {
            JOptionPane.showMessageDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed.toString(), "Could not deploy some units",
                    JOptionPane.WARNING_MESSAGE);
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
        getCampaignGui().refreshRating();
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
        selectedScenario = scenario.getId();
        if (getCampaign().getCampaignOptions().getUseAtB() && (scenario instanceof AtBScenario)) {
            scrollScenarioView.setViewportView(
                    new AtBScenarioViewPanel((AtBScenario) scenario, getCampaign(), getIconPackage(), getFrame()));
        } else {
            scrollScenarioView.setViewportView(new ScenarioViewPanel(scenario, getCampaign(), getIconPackage()));
        }
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        javax.swing.SwingUtilities.invokeLater(() -> scrollScenarioView.getVerticalScrollBar().setValue(0));
        boolean unitsAssigned = scenario.getForces(getCampaign()).getAllUnits().size() > 0;
        boolean canStartGame = scenario.isCurrent() && unitsAssigned;
        if (getCampaign().getCampaignOptions().getUseAtB() && scenario instanceof AtBScenario) {
            canStartGame = canStartGame && getCampaign().getDate().equals(scenario.getDate());
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
        if (idx >= 0 && idx < getCampaign().getSortedMissions().size()) {
            Mission m = getCampaign().getSortedMissions().get(idx);
            if (null != m) {
                selectedMission = m.getId();
                if (getCampaign().getCampaignOptions().getUseAtB() && m instanceof AtBContract) {
                    scrollMissionView.setViewportView(new AtBContractViewPanel((AtBContract) m, getCampaign()));
                } else if (m instanceof Contract) {
                    scrollMissionView.setViewportView(new ContractViewPanel((Contract) m));
                } else {
                    scrollMissionView.setViewportView(new MissionViewPanel(m));
                }
                // This odd code is to make sure that the scrollbar stays at the
                // top
                // I can't just call it here, because it ends up getting reset
                // somewhere later
                javax.swing.SwingUtilities.invokeLater(() -> scrollMissionView.getVerticalScrollBar().setValue(0));
                btnEditMission.setEnabled(true);
                btnCompleteMission.setEnabled(m.isActive());
                btnDeleteMission.setEnabled(true);
                btnAddScenario.setEnabled(m.isActive());
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
    public void handle(MissionChangedEvent ev) {
        if (ev.getMission().getId() == selectedMission) {
            changeMission();
        }
    }
}
