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

import static megamek.client.ratgenerator.ForceDescriptor.RATING_5;
import static mekhq.campaign.force.Force.NO_ASSIGNED_SCENARIO;
import static mekhq.campaign.mission.enums.MissionStatus.PARTIAL;
import static mekhq.campaign.mission.enums.MissionStatus.SUCCESS;
import static mekhq.campaign.mission.enums.ScenarioStatus.REFUSED_ENGAGEMENT;
import static mekhq.campaign.randomEvents.prisoners.PrisonerEventManager.DEFAULT_TEMPORARY_CAPACITY;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.gui.dialog.factionStanding.manualMissionDialogs.SimulateMissionDialog.handleFactionRegardUpdates;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.File;
import java.time.LocalDate;
import java.util.*;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.PrincessException;
import megamek.client.generator.ReconfigurationParameters;
import megamek.client.generator.TeamLoadOutGenerator;
import megamek.client.ui.comboBoxes.MMComboBox;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.containers.MunitionTree;
import megamek.common.enums.Gender;
import megamek.common.equipment.GunEmplacement;
import megamek.common.event.Subscribe;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import megameklab.util.UnitPrintManager;
import mekhq.MekHQ;
import mekhq.campaign.Hangar;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.GMModeEvent;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.events.missions.MissionCompletedEvent;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.events.missions.MissionRemovedEvent;
import mekhq.campaign.events.scenarios.ScenarioChangedEvent;
import mekhq.campaign.events.scenarios.ScenarioNewEvent;
import mekhq.campaign.events.scenarios.ScenarioRemovedEvent;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForce;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.PrisonerMissionEndEvent;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.adapter.ScenarioTableMouseAdapter;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.*;
import mekhq.gui.dialog.factionStanding.manualMissionDialogs.ManualMissionDialog;
import mekhq.gui.dialog.factionStanding.manualMissionDialogs.SimulateMissionDialog;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.ScenarioTableModel;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.sorter.DateStringComparator;
import mekhq.gui.view.AtBScenarioViewPanel;
import mekhq.gui.view.LanceAssignmentView;
import mekhq.gui.view.MissionViewPanel;
import mekhq.gui.view.ScenarioViewPanel;

/**
 * Displays Mission/Contract and Scenario details.
 */
public final class BriefingTab extends CampaignGuiTab {
    private static final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
          MekHQ.getMHQOptions().getLocale());

    private LanceAssignmentView panLanceAssignment;
    private JSplitPane splitScenario;
    private JTable scenarioTable;
    private MMComboBox<Mission> comboMission;
    private JScrollPane scrollMissionView;
    private JScrollPane scrollScenarioView;
    private RoundedJButton btnAddScenario;
    private RoundedJButton btnEditMission;
    private RoundedJButton btnCompleteMission;
    private RoundedJButton btnDeleteMission;
    private RoundedJButton btnGMGenerateScenarios;
    private RoundedJButton btnStartGame;
    private RoundedJButton btnJoinGame;
    private RoundedJButton btnLoadGame;
    private RoundedJButton btnPrintRS;
    private RoundedJButton btnGetMul;
    private RoundedJButton btnClearAssignedUnits;
    private RoundedJButton btnResolveScenario;
    private RoundedJButton btnAutoResolveScenario;

    private ScenarioTableModel scenarioModel;

    public int selectedScenario;

    private static final MMLogger logger = MMLogger.create(BriefingTab.class);

    // region Constructors
    public BriefingTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
        selectedScenario = -1;
        MekHQ.registerHandler(this);
    }
    // endregion Constructors

    @Override
    public MHQTabType tabType() {
        return MHQTabType.BRIEFING_ROOM;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        JPanel panMission = new JPanel(new GridBagLayout());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
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

        JPanel panMissionButtons = new JPanel(new GridLayout(2, 3));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panMission.add(panMissionButtons, gridBagConstraints);

        RoundedJButton btnAddMission = new RoundedJButton(resourceMap.getString("btnAddMission.text"));
        btnAddMission.setToolTipText(resourceMap.getString("btnAddMission.toolTipText"));
        btnAddMission.addActionListener(ev -> addMission());
        panMissionButtons.add(btnAddMission);

        btnAddScenario = new RoundedJButton(resourceMap.getString("btnAddScenario.text"));
        btnAddScenario.setToolTipText(resourceMap.getString("btnAddScenario.toolTipText"));
        btnAddScenario.addActionListener(ev -> addScenario());
        panMissionButtons.add(btnAddScenario);

        btnEditMission = new RoundedJButton(resourceMap.getString("btnEditMission.text"));
        btnEditMission.setToolTipText(resourceMap.getString("btnEditMission.toolTipText"));
        btnEditMission.addActionListener(ev -> editMission());
        panMissionButtons.add(btnEditMission);

        btnCompleteMission = new RoundedJButton(resourceMap.getString("btnCompleteMission.text"));
        btnCompleteMission.setToolTipText(resourceMap.getString("btnCompleteMission.toolTipText"));
        btnCompleteMission.addActionListener(ev -> completeMission());
        panMissionButtons.add(btnCompleteMission);

        btnDeleteMission = new RoundedJButton(resourceMap.getString("btnDeleteMission.text"));
        btnDeleteMission.setToolTipText(resourceMap.getString("btnDeleteMission.toolTipText"));
        btnDeleteMission.setName("btnDeleteMission");
        btnDeleteMission.addActionListener(ev -> deleteMission());
        panMissionButtons.add(btnDeleteMission);

        btnGMGenerateScenarios = new RoundedJButton(resourceMap.getString("btnGMGenerateScenarios.text"));
        btnGMGenerateScenarios.setToolTipText(resourceMap.getString("btnGMGenerateScenarios.toolTipText"));
        btnGMGenerateScenarios.setName("btnGMGenerateScenarios");
        btnGMGenerateScenarios.addActionListener(ev -> gmGenerateScenarios());
        panMissionButtons.add(btnGMGenerateScenarios);

        scrollMissionView = new FastJScrollPane();
        scrollMissionView.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollMissionView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollMissionView.setViewportView(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panMission.add(scrollMissionView, gridBagConstraints);

        scenarioModel = new ScenarioTableModel(getCampaign());
        scenarioTable = new JTable(scenarioModel);
        scenarioTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scenarioTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        TableRowSorter<ScenarioTableModel> scenarioSorter = new TableRowSorter<>(scenarioModel);
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

        JPanel panScenario = new JPanel(new GridBagLayout());

        JPanel panScenarioButtons = new JPanel(new GridLayout(3, 3));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panScenario.add(panScenarioButtons, gridBagConstraints);

        btnStartGame = new RoundedJButton(resourceMap.getString("btnStartGame.text"));
        btnStartGame.setToolTipText(resourceMap.getString("btnStartGame.toolTipText"));
        btnStartGame.addActionListener(ev -> startScenario());
        btnStartGame.setEnabled(false);
        panScenarioButtons.add(btnStartGame);

        btnJoinGame = new RoundedJButton(resourceMap.getString("btnJoinGame.text"));
        btnJoinGame.setToolTipText(resourceMap.getString("btnJoinGame.toolTipText"));
        btnJoinGame.addActionListener(ev -> joinScenario());
        btnJoinGame.setEnabled(false);
        panScenarioButtons.add(btnJoinGame);

        btnLoadGame = new RoundedJButton(resourceMap.getString("btnLoadGame.text"));
        btnLoadGame.setToolTipText(resourceMap.getString("btnLoadGame.toolTipText"));
        btnLoadGame.addActionListener(ev -> loadScenario());
        btnLoadGame.setEnabled(false);
        panScenarioButtons.add(btnLoadGame);

        btnPrintRS = new RoundedJButton(resourceMap.getString("btnPrintRS.text"));
        btnPrintRS.setToolTipText(resourceMap.getString("btnPrintRS.toolTipText"));
        btnPrintRS.addActionListener(ev -> printRecordSheets());
        btnPrintRS.setEnabled(false);
        panScenarioButtons.add(btnPrintRS);

        btnGetMul = new RoundedJButton(resourceMap.getString("btnGetMul.text"));
        btnGetMul.setToolTipText(resourceMap.getString("btnGetMul.toolTipText"));
        btnGetMul.setName("btnGetMul");
        btnGetMul.addActionListener(ev -> deployListFile());
        btnGetMul.setEnabled(false);
        panScenarioButtons.add(btnGetMul);

        btnResolveScenario = new RoundedJButton(resourceMap.getString("btnResolveScenario.text"));
        btnResolveScenario.setToolTipText(resourceMap.getString("btnResolveScenario.toolTipText"));
        btnResolveScenario.addActionListener(ev -> resolveScenario());
        btnResolveScenario.setEnabled(false);
        panScenarioButtons.add(btnResolveScenario);

        btnAutoResolveScenario = new RoundedJButton(resourceMap.getString("btnAutoResolveScenario.text"));
        btnAutoResolveScenario.setToolTipText(resourceMap.getString("btnAutoResolveScenario.toolTipText"));
        btnAutoResolveScenario.addActionListener(ev -> autoResolveScenario());
        btnAutoResolveScenario.setEnabled(false);
        panScenarioButtons.add(btnAutoResolveScenario);

        btnClearAssignedUnits = new RoundedJButton(resourceMap.getString("btnClearAssignedUnits.text"));
        btnClearAssignedUnits.setToolTipText(resourceMap.getString("btnClearAssignedUnits.toolTipText"));
        btnClearAssignedUnits.addActionListener(ev -> clearAssignedUnits());
        btnClearAssignedUnits.setEnabled(false);
        panScenarioButtons.add(btnClearAssignedUnits);

        scrollScenarioView = new FastJScrollPane();
        scrollScenarioView.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollScenarioView.setViewportView(null);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panScenario.add(scrollScenarioView, gridBagConstraints);

        /* ATB */
        panLanceAssignment = new LanceAssignmentView(getCampaign());
        JScrollPane paneLanceDeployment = new FastJScrollPane(panLanceAssignment);
        paneLanceDeployment.setBorder(null);
        paneLanceDeployment.setMinimumSize(new Dimension(200, 300));
        paneLanceDeployment.setPreferredSize(new Dimension(200, 300));
        paneLanceDeployment.setVisible(getCampaign().getCampaignOptions().isUseAtB());
        splitScenario = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panScenario, paneLanceDeployment);
        splitScenario.setOneTouchExpandable(true);
        splitScenario.setResizeWeight(1.0);

        JSplitPane splitBrief = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panMission, splitScenario);
        splitBrief.setOneTouchExpandable(true);
        splitBrief.setResizeWeight(0.5);
        splitBrief.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> refreshScenarioView());

        JPanel pnlTutorial = new TutorialHyperlinkPanel("missionTab");

        setLayout(new BorderLayout());
        add(splitBrief, BorderLayout.CENTER);
        add(pnlTutorial, BorderLayout.SOUTH);
    }

    private void addMission() {
        MissionTypeDialog mtd = new MissionTypeDialog(getFrame(), true);
        mtd.setVisible(true);
        if (mtd.isContract()) {
            NewContractDialog ncd = getCampaignOptions().isUseAtB() ?
                                          new NewAtBContractDialog(getFrame(), true, getCampaign()) :
                                          new NewContractDialog(getFrame(), true, getCampaign());
            ncd.setVisible(true);
            comboMission.setSelectedItem(ncd.getContract());
        }
        if (mtd.isMission()) {
            CustomizeMissionDialog cmd = new CustomizeMissionDialog(getFrame(), true, null, getCampaign());
            cmd.setVisible(true);
            comboMission.setSelectedItem(cmd.getMission());
        }
    }

    private void editMission() {
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            return;
        }

        if (getCampaign().getCampaignOptions().isUseAtB() && (mission instanceof AtBContract)) {
            CustomizeAtBContractDialog cmd = new CustomizeAtBContractDialog(getFrame(),
                  true,
                  (AtBContract) mission,
                  getCampaign());
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
        }

        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();

        getCampaign().getApp().getAutosaveService().requestBeforeMissionEndAutosave(getCampaign());

        final CompleteMissionDialog cmd = new CompleteMissionDialog(getFrame());
        if (!cmd.showDialog().isConfirmed()) {
            return;
        }

        final MissionStatus status = cmd.getStatus();
        if (status.isActive()) {
            return;
        }

        PrisonerMissionEndEvent prisoners = new PrisonerMissionEndEvent(getCampaign(), mission);

        if (!getCampaign().getPrisonerDefectors().isEmpty() &&
                  prisoners.handlePrisonerDefectors() == 0) { // This is the cancel choice index
            return;
        }

        if (campaignOptions.isUseAtB() && (mission instanceof AtBContract)) {
            if (((AtBContract) mission).contractExtended(getCampaign())) {
                return;
            }
        }

        getCampaign().completeMission(mission, status);
        MekHQ.triggerEvent(new MissionCompletedEvent(mission));

        // apply mission xp
        int xpAward = getMissionXpAward(cmd.getStatus(), mission);

        LocalDate today = getCampaign().getLocalDate();
        if (xpAward > 0) {
            for (Person person : getCampaign().getActivePersonnel(false, false)) {
                if (person.isChild(today)) {
                    continue;
                }

                if (person.isDependent()) {
                    continue;
                }

                person.awardXP(getCampaign(), xpAward);
            }
        }

        // Prisoners
        boolean wasOverallSuccess = cmd.getStatus() == SUCCESS || cmd.getStatus() == PARTIAL;

        // We only resolve prisoners if there are no active Missions
        if (getCampaign().getActiveMissions(false).isEmpty()) {
            if (!getCampaign().getFriendlyPrisoners().isEmpty()) {
                prisoners.handlePrisoners(wasOverallSuccess, true);
            }

            if (!getCampaign().getCurrentPrisoners().isEmpty()) {
                prisoners.handlePrisoners(wasOverallSuccess, false);
            }

            getCampaign().setTemporaryPrisonerCapacity(DEFAULT_TEMPORARY_CAPACITY);
        }

        // resolve turnover
        if ((campaignOptions.isUseRandomRetirement()) && (campaignOptions.isUseContractCompletionRandomRetirement())) {
            RetirementDefectionDialog rdd = new RetirementDefectionDialog(getCampaignGui(), mission, true);

            if (rdd.wasAborted()) {
                /*
                 * Once the retirement rolls have been made, the outstanding payouts can be
                 * resolved
                 * without a reference to the contract and the dialog can be accessed through
                 * the menu
                 * provided they aren't still assigned to the mission in question.
                 */
                if (!getCampaign().getRetirementDefectionTracker().isOutstanding(mission.getId())) {
                    return;
                }
            } else {
                if ((getCampaign().getRetirementDefectionTracker().getRetirees(mission) != null) &&
                          getCampaign().getFinances().getBalance().isGreaterOrEqualThan(rdd.totalPayout())) {
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

        if (campaignOptions.isUseAtB() && (mission instanceof AtBContract)) {
            getCampaign().getContractMarket().checkForFollowup(getCampaign(), (AtBContract) mission);
        }

        // prompt autoAwards ceremony
        if (campaignOptions.isEnableAutoAwards()) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();

            // for the purposes of Mission Accomplished awards, we do not count partial
            // Successes as Success
            autoAwardsController.PostMissionController(getCampaign(),
                  mission,
                  Objects.equals(String.valueOf(cmd.getStatus()), "Success"));
        }

        // Update Faction Standings
        if (campaignOptions.isTrackFactionStanding()) {
            FactionStandings factionStandings = getCampaign().getFactionStandings();
            List<String> reports = new ArrayList<>();

            double regardMultiplier = campaignOptions.getRegardMultiplier();

            if (mission instanceof AtBContract contract) {
                Faction employer = contract.getEmployerFaction();
                reports = factionStandings.processContractCompletion(getCampaign().getFaction(), employer, today,
                      status, regardMultiplier, contract.getLength());
            } else {
                SimulateMissionDialog dialog = getSimulateMissionDialog(mission, status);

                Faction employerChoice = dialog.getEmployerChoice();
                Faction enemyChoice = dialog.getEnemyChoice();
                MissionStatus statusChoice = dialog.getStatusChoice();
                int durationChoice = dialog.getDurationChoice();

                reports.addAll(handleFactionRegardUpdates(getCampaign().getFaction(), employerChoice, enemyChoice,
                      statusChoice, today, factionStandings, regardMultiplier, durationChoice));
            }

            for (String report : reports) {
                if (report != null && !report.isBlank()) {
                    getCampaign().addReport(report);
                }
            }
        }

        // Undeploy forces
        boolean isCadreDuty = mission instanceof AtBContract && ((AtBContract) mission).getContractType().isCadreDuty();
        boolean hadCadreForces = false;
        for (Force force : getCampaign().getAllForces()) {
            if (isCadreDuty && force.getCombatRoleInMemory().isCadre()) {
                force.setCombatRoleInMemory(CombatRole.FRONTLINE);
                hadCadreForces = true;
            }

            int scenarioAssignment = force.getScenarioId();
            if (scenarioAssignment != NO_ASSIGNED_SCENARIO) {
                Scenario scenario = getCampaign().getScenario(force.getScenarioId());

                // This shouldn't be necessary, but now is as good a time as any to check for null scenarios
                if (scenario == null || scenario.getMissionId() == mission.getId()) {
                    force.setScenarioId(NO_ASSIGNED_SCENARIO, getCampaign());
                }
            }
        }

        if (hadCadreForces) {
            new ImmersiveDialogNotification(getCampaign(), resourceMap.getString("cadreReassignment.text"),
                  true);
        }

        // Resolve any outstanding scenarios
        for (Scenario scenario : mission.getCurrentScenarios()) {
            scenario.setStatus(REFUSED_ENGAGEMENT);
        }

        if (getCampaign().getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
            if (mission instanceof AtBContract contract) {
                if (contract.getEmployerCode().equals(PIRATE_FACTION_CODE)) {
                    // CamOps 'other crimes' value
                    getCampaign().changeCrimePirateModifier(10);
                }
            }
        }

        final List<Mission> missions = getCampaign().getSortedMissions();
        comboMission.setSelectedItem(missions.isEmpty() ? null : missions.get(0));
    }

    /**
     * Creates and returns a {@link SimulateMissionDialog} for the given mission and status.
     *
     * <p>Determines the start date for the dialog as follows:</p>
     *
     * <ul>
     *   <li>If the mission is a contract, the contract's start date is used; otherwise, {@code null} is assigned.</li>
     *   <li>If the start date equals the campaign's current local date, the method iterates through the mission's
     *   scenarios and uses the earliest scenario date found.</li>
     * </ul>
     *
     * <p>The returned dialog is initialized with campaign and mission details.</p>
     *
     * @param mission the mission for which the simulation dialog is created
     * @param status  the status to preselect in the dialog
     *
     * @return a configured {@code ManualMissionDialog} for the mission
     *
     * @author Illiani
     * @since 0.50.07
     */
    private SimulateMissionDialog getSimulateMissionDialog(Mission mission, MissionStatus status) {
        LocalDate startDate = mission instanceof Contract
                                    ? ((Contract) mission).getStartDate()
                                    : null;
        LocalDate today = getCampaign().getLocalDate();
        if (startDate == null) {
            startDate = today;
        }

        if (startDate.equals(today)) {
            for (Scenario scenario : mission.getScenarios()) {
                LocalDate scenarioDate = scenario.getDate();
                if (scenarioDate.isBefore(startDate)) {
                    startDate = scenarioDate;
                }
            }
        }

        return new ManualMissionDialog(getFrame(),
              getCampaign().getCampaignFactionIcon(),
              getCampaign().getFaction(),
              startDate,
              status,
              mission.getName(),
              mission.getLength());
    }

    /**
     * Calculates the XP award for completing a mission.
     *
     * @param missionStatus The status of the mission as a MissionStatus enum.
     * @param mission       The Mission object representing the completed mission.
     *
     * @return The XP award for completing the mission.
     */
    private int getMissionXpAward(MissionStatus missionStatus, Mission mission) {
        return switch (missionStatus) {
            case FAILED, BREACH -> getCampaign().getCampaignOptions().getMissionXpFail();
            case SUCCESS, PARTIAL -> {
                if ((getCampaign().getCampaignOptions().isUseStratCon()) &&
                          (mission instanceof AtBContract)) {
                    StratConCampaignState stratConCampaignState = ((AtBContract) mission).getStratconCampaignState();

                    if (stratConCampaignState == null || stratConCampaignState.getVictoryPoints() < 3) {
                        yield getCampaign().getCampaignOptions().getMissionXpSuccess();
                    } else {
                        yield getCampaign().getCampaignOptions().getMissionXpOutstandingSuccess();
                    }
                } else {
                    yield getCampaign().getCampaignOptions().getMissionXpSuccess();
                }
            }
            case ACTIVE -> 0;
        };
    }

    private void deleteMission() {
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            logger.error("Cannot remove null mission");
            return;
        }
        logger.debug("Attempting to Delete Mission, Mission ID: {}", mission.getId());
        if (0 !=
                  JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to delete this mission?",
                        "Delete mission?",
                        JOptionPane.YES_NO_OPTION)) {
            return;
        }

        // Undeploy forces
        for (Scenario scenario : mission.getScenarios()) {
            for (Force force : getCampaign().getAllForces()) {
                if (force.getScenarioId() == scenario.getId()) {
                    force.setScenarioId(NO_ASSIGNED_SCENARIO, getCampaign());
                }
            }
        }

        getCampaign().removeMission(mission);
        final List<Mission> missions = getCampaign().getSortedMissions();
        comboMission.setSelectedItem(missions.isEmpty() ? null : missions.get(0));
        MekHQ.triggerEvent(new MissionRemovedEvent(mission));
    }

    private void gmGenerateScenarios() {
        if (!getCampaign().isGM()) {
            JOptionPane.showMessageDialog(this, "Only allowed for GM players", "Not GM", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (0 !=
                  JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to generate a new set of scenarios?",
                        "Generate scenarios?",
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
        // need to update the scenario table and refresh the scroll view
        refreshScenarioTableData();
        scrollMissionView.revalidate();
        scrollMissionView.repaint();
    }

    private void clearAssignedUnits() {
        if (0 ==
                  JOptionPane.showConfirmDialog(null,
                        "Do you really want to remove all units from this scenario?",
                        "Clear Units?",
                        JOptionPane.YES_NO_OPTION)) {
            Scenario scenario = getScenario();
            if (scenario == null) {
                return;
            }

            // This handles StratCon undeployment
            if (scenario instanceof AtBScenario) {
                AtBContract contract = ((AtBScenario) scenario).getContract(getCampaign());
                StratConScenario stratConScenario = ((AtBScenario) scenario).getStratconScenario(contract,
                      (AtBScenario) scenario);

                if (stratConScenario != null) {
                    stratConScenario.resetScenario(getCampaign());
                    return;
                }
            }

            // This handles Legacy AtB undeployment
            scenario.clearAllForcesAndPersonnel(getCampaign());
        }
    }

    private void printRecordSheets() {
        final Scenario scenario = getScenario();
        if (scenario == null) {
            return;
        }

        // First, we need to get all units assigned to the current scenario
        final List<UUID> unitIds = scenario.getForces(getCampaign()).getAllUnits(false);

        // Then, we need to convert the ids to units, and filter out any units that are
        // null and
        // any units with null entities
        final List<Unit> units = unitIds.stream()
                                       .map(unitId -> getCampaign().getUnit(unitId))
                                       .filter(unit -> (unit != null) && (unit.getEntity() != null))
                                       .toList();

        final List<Entity> chosen = new ArrayList<>();
        final StringBuilder unDeployed = new StringBuilder();

        for (final Unit unit : units) {
            if (unit.checkDeployment() == null) {
                unit.resetPilotAndEntity();
                chosen.add(unit.getEntity());
            } else {
                unDeployed.append('\n').append(unit.getName()).append(" (").append(unit.checkDeployment()).append(')');
            }
        }

        if (!unDeployed.isEmpty()) {
            final Object[] options = { "Continue", "Cancel" };
            if (JOptionPane.showOptionDialog(getFrame(),
                  "The following units could not be deployed:" + unDeployed,
                  "Could not deploy some units",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.WARNING_MESSAGE,
                  null,
                  options,
                  options[1]) == JOptionPane.NO_OPTION) {
                return;
            }
        }

        if (scenario instanceof AtBScenario atBScenario) {
            // Also print off allied sheets
            chosen.addAll(atBScenario.getAlliesPlayer());
        }

        // add bot forces
        chosen.addAll(scenario.getBotForces()
                            .stream()
                            .flatMap(botForce -> botForce.getFullEntityList(getCampaign()).stream())
                            .toList());

        if (!chosen.isEmpty()) {
            UnitPrintManager.printAllUnits(chosen, true);
        }
    }

    private void loadScenario() {
        Scenario scenario = getScenario();
        if (null != scenario) {
            getCampaignGui().getApplication().startHost(scenario, true, new ArrayList<>());
        }
    }

    /**
     * Initiates the start of the selected scenario after confirming briefing acceptance.
     *
     * <p>This method first presents the scenario briefing dialog to the user via {@link #createBriefingDialog()}. If
     * the user cancels or does not accept the briefing, the method returns and does not proceed further. If accepted,
     * it calls {@link #startScenario(Scenario, BehaviorSettings)} to begin the scenario.</p>
     */
    private void startScenario() {
        if (!createBriefingDialog()) {
            return;
        }

        Scenario scenario = getScenario();
        if (scenario == null) {
            return;
        }

        if (!displaySalvageForcePicker(scenario)) {
            return;
        }

        if (!displaySalvageTechPicker(scenario)) {
            return;
        }

        startScenario(scenario, null);
    }

    /**
     * Displays a dialog allowing the player to select forces for salvage operations.
     *
     * <p>This method gathers all available salvage-capable forces from the campaign and presents
     * them to the player via a {@link SalvageForcePicker} dialog. Forces are filtered based on their salvage
     * capabilities and whether they are deployed.</p>
     *
     * @param scenario the scenario for which salvage forces are being selected
     *
     * @return {@code true} if the player confirmed their force selection, {@code false} if they canceled
     *
     * @author Illiani
     * @since 0.50.10
     */
    private boolean displaySalvageForcePicker(Scenario scenario) {
        scenario.getSalvageForces().clear(); // reset, in case we've previously canceled out of the dialog
        List<Force> salvageForceOptions = getSalvageForces(getCampaign().getHangar(),
              scenario.getBoardType() == AtBScenario.T_SPACE);

        SalvageForcePicker forcePicker = new SalvageForcePicker(getCampaign(), scenario, salvageForceOptions);
        boolean wasConfirmed = forcePicker.wasConfirmed();
        if (wasConfirmed) {
            List<Force> selectedForces = forcePicker.getSelectedForces();
            for (Force force : selectedForces) {
                scenario.addSalvageForce(force.getId());
            }
        }

        return forcePicker.wasConfirmed();
    }

    private boolean displaySalvageTechPicker(Scenario scenario) {
        scenario.getSalvageTechs().clear(); // reset, in case we've previously canceled out of the dialog
        List<Person> availableTechs = getAvailableTechs();

        SalvageTechPicker techPicker = new SalvageTechPicker(getCampaign(), availableTechs);
        boolean wasConfirmed = techPicker.wasConfirmed();
        if (wasConfirmed) {
            List<Person> selectedTechs = techPicker.getSelectedTechs();
            for (Person tech : selectedTechs) {
                scenario.addSalvageTech(tech.getId());
            }
        }

        return techPicker.wasConfirmed();
    }

    private List<Person> getAvailableTechs() {
        List<Person> availableTechs = new ArrayList<>();
        for (Person tech : getCampaign().getTechs()) {
            if (!tech.isDeployed() && tech.getMinutesLeft() > 0) {
                availableTechs.add(tech);
            }
        }

        // experienceLevel lowest -> highest, minutes highest -> lowest, rank lowest -> highest, full name a -> x
        availableTechs.sort(Comparator.comparing((Person p) -> p.getExperienceLevel(getCampaign(),
                    p.getPrimaryRole().isTech()))
                                  .thenComparing(Comparator.comparing(Person::getMinutesLeft).reversed())
                                  .thenComparing(Person::getRankNumeric)
                                  .thenComparing(Person::getFullName));
        return availableTechs;
    }

    /**
     * Retrieves all available forces capable of performing salvage operations.
     *
     * <p>This method collects forces in two passes:</p>
     * <ol>
     *   <li>First, it examines all combat teams and their parent forces, adding any undeployed forces
     *       with salvage-capable units. It tracks visited force IDs to avoid duplication.</li>
     *   <li>Second, it searches for dedicated salvage forces (non-combat team forces with salvage type)
     *       that weren't already visited in the first pass.</li>
     * </ol>
     *
     * <p>Forces are filtered to include only those that:</p>
     * <ul>
     *   <li>Are not currently deployed</li>
     *   <li>Have at least one unit capable of salvage operations</li>
     *   <li>Meet the scenario environment requirements (ground or space)</li>
     * </ul>
     *
     * <p>The returned list is sorted alphabetically by force name.</p>
     *
     * @param hangar          the campaign hangar containing all units
     * @param isSpaceScenario {@code true} if checking for space salvage capabilities, {@code false} for ground
     *
     * @return a sorted list of forces capable of salvage operations
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<Force> getSalvageForces(Hangar hangar, boolean isSpaceScenario) {
        List<Integer> visitedForceIds = new ArrayList<>();
        List<Force> salvageForceOptions = new ArrayList<>();

        // Collect Combat Teams
        for (CombatTeam combatTeam : getCampaign().getCombatTeamsAsList()) {
            Force force = getCampaign().getForce(combatTeam.getForceId());
            if (force == null) {
                continue;
            }

            visitedForceIds.add(force.getId());
            force.getSubForces().forEach(subForce -> visitedForceIds.add(subForce.getId()));

            if (!force.isDeployed() && force.getSalvageUnitCount(hangar, isSpaceScenario) > 0) {
                salvageForceOptions.add(force);
            }
        }

        // Collect non-Combat Team salvage forces
        for (Force force : getCampaign().getAllForces()) {
            if (visitedForceIds.contains(force.getId())) {
                continue;
            }

            Force parentForce = force.getParentForce();
            if (parentForce != null && parentForce.getForceType().isSalvage()) {
                continue;
            }

            if (force.getForceType().isSalvage() && force.getSalvageUnitCount(hangar, isSpaceScenario) > 0) {
                salvageForceOptions.add(force);
                visitedForceIds.add(force.getId());
            }
        }

        salvageForceOptions.sort(Comparator.comparing(Force::getFullName));
        return salvageForceOptions;
    }

    /**
     * Displays a dialog presenting the scenario briefing and allows the user to accept or cancel the scenario.
     *
     * <p>This method retrieves the selected scenario from the scenario table, obtains relevant mission and commander
     * information, constructs the description with preserved line breaks, and then shows an
     * {@code ImmersiveDialogSimple} with accept and cancel buttons.</p>
     *
     * @return {@code true} if the user accepts the scenario or no scenario is selected; {@code false} if the scenario
     *       is canceled.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private boolean createBriefingDialog() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return true;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return true;
        }

        String description = scenario.getDescription().replaceAll("(\r\n|\n)", "<br>");

        // If there isn't a description, we have nothing to display, so just act as if the player confirmed the dialog
        if (description.isBlank()) {
            return true;
        }

        Mission mission = null;
        if (scenario.getMissionId() != -1) {
            mission = getCampaign().getMission(scenario.getMissionId());
        }
        if (mission == null) {
            mission = comboMission.getSelectedItem();
        }

        Person speaker;
        if (mission instanceof AtBContract contract) {
            speaker = contract.getEmployerLiaison();
        } else {
            // If we're not working with an AtBContract we have to generate the liaison each time
            speaker = getCampaign().newPerson(PersonnelRole.ADMINISTRATOR_COMMAND, "MERC", Gender.RANDOMIZE);
        }

        List<Person> forceCommanders = new ArrayList<>();
        for (Force force : getCampaign().getAllForces()) {
            Person commander = getCampaign().getPerson(force.getForceCommanderID());
            if (commander != null) {
                forceCommanders.add(commander);
            }
        }

        Person overallCommander = null;
        for (Person commander : forceCommanders) {
            if (overallCommander == null) {
                overallCommander = commander;
                continue;
            }

            if (commander.outRanksUsingSkillTiebreaker(getCampaign(), overallCommander)) {
                overallCommander = commander;
            }
        }

        List<String> buttons = List.of(resourceMap.getString("dialogScenarioAcceptance.button.accept"),
              resourceMap.getString("dialogScenarioAcceptance.button.cancel"));

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(getCampaign(),
              speaker,
              overallCommander,
              description,
              buttons,
              resourceMap.getString("dialogScenarioAcceptance.outOfCharacter"),
              null,
              false);

        return dialog.getDialogChoice() == 0;
    }


    /**
     * Resolve the selected scenario by proving a MUL file
     */
    private void resolveScenario() {
        Scenario scenario = getSelectedScenario();
        if (null == scenario) {
            return;
        }
        getCampaign().getApp().resolveScenario(scenario);
    }

    /**
     * Auto-resolve the selected scenario. Can run both the auto resolve using princess or using the ACS engine
     */
    private void autoResolveScenario() {
        Scenario scenario = getSelectedScenario();
        if (null == scenario) {
            return;
        }
        promptAutoResolve(scenario);
    }

    private void runAbstractCombatAutoResolve(Scenario scenario) {
        if (!displaySalvageForcePicker(scenario)) {
            return;
        }

        if (!displaySalvageTechPicker(scenario)) {
            return;
        }

        List<Unit> chosen = playerUnits(scenario, new StringBuilder());
        if (chosen.isEmpty()) {
            return;
        }
        getCampaign().getApp().startAutoResolve(scenario, chosen);
    }

    private void runPrincessAutoResolve() {
        Scenario scenario = getScenario();
        if (scenario == null) {
            return;
        }

        if (!displaySalvageForcePicker(scenario)) {
            return;
        }

        if (!displaySalvageTechPicker(scenario)) {
            return;
        }

        startScenario(scenario, getCampaign().getAutoResolveBehaviorSettings());
    }

    private @Nullable Scenario getScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
    }

    private void promptAutoResolve(Scenario scenario) {
        // the options for the auto resolve method follow a predefined order, which is the same as the order in the enum,
        // and it uses that to preselect the option that is currently set in the campaign options
        Object[] options = new Object[] { getText("AutoResolveMethod.PRINCESS.text"),
                                          getText("AutoResolveMethod.ABSTRACT_COMBAT.text"), };

        var preSelectedOptionIndex = getCampaignOptions().getAutoResolveMethod().ordinal();

        var selectedOption = JOptionPane.showOptionDialog(getFrame(),
              getText("AutoResolveMethod.promptForAutoResolveMethod.text"),
              getText("AutoResolveMethod.promptForAutoResolveMethod.title"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              options,
              options[preSelectedOptionIndex]);

        if (selectedOption == JOptionPane.CLOSED_OPTION) {
            return;
        }

        var autoResolveMethod = AutoResolveMethod.values()[selectedOption];

        if (autoResolveMethod == AutoResolveMethod.PRINCESS) {
            runPrincessAutoResolve();
        } else if (autoResolveMethod == AutoResolveMethod.ABSTRACT_COMBAT) {
            runAbstractCombatAutoResolve(scenario);
        }
    }


    private List<Unit> playerUnits(Scenario scenario, StringBuilder unDeployed) {
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(false);
        if (uids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Unit> chosen = new ArrayList<>();
        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);

            if ((null != u) && (null != u.getEntity())) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to date!
                    u.resetPilotAndEntity();
                    chosen.add(u);
                } else {
                    unDeployed.append('\n').append(u.getName()).append(" (").append(u.checkDeployment()).append(')');
                }
            }
        }
        return chosen;
    }

    private Scenario getSelectedScenario() {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
    }

    private void startScenario(Scenario scenario, BehaviorSettings autoResolveBehaviorSettings) {
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(false);
        if (uids.isEmpty()) {
            return;
        }

        List<Unit> chosen = new ArrayList<>();
        List<Unit> unDeployedUnits = new ArrayList<>();
        StringBuilder unDeployed = new StringBuilder();

        Map<Unit, Entity> unitEntityMap = new HashMap<>();
        for (UUID uid : uids) {
            Unit unit = getCampaign().getUnit(uid);

            if (unit == null) {
                logger.error("Skipping unit {} because it is null", uid);
                continue;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                logger.error("Skipping unit {} because it's entity is null", uid);
                continue;
            }

            String deploymentStatus = unit.checkDeployment();
            if (deploymentStatus != null) {
                unDeployed.append('\n').append(unit.getName()).append(" (").append(unit.checkDeployment()).append(')');
                unDeployedUnits.add(unit);
                continue;
            }

            unitEntityMap.put(unit, entity);
        }

        List<Unit> prospectiveCommanders = new ArrayList<>();

        for (Unit unit : unitEntityMap.keySet()) {
            int forceId = unit.getForceId();
            Force force = getCampaign().getForce(forceId);

            // This will occur if the unit doesn't have an associated force
            if (force == null) {
                logger.error("Skipping unit {} because it's force is null", unit.getName());
                continue;
            }

            UUID forceCommanderId = force.getForceCommanderID();
            Person unitCommander = unit.getCommander();

            if (unitCommander == null) {
                logger.error("Skipping unit {} because it's commander is null", unit.getName());
                continue;
            }

            UUID unitCommanderId = unitCommander.getId();

            if (Objects.equals(forceCommanderId, unitCommanderId)) {
                prospectiveCommanders.add(unit);
            }
        }

        Unit commandUnit = null;
        if (prospectiveCommanders.isEmpty()) {
            // If we don't have an empty map we can just grab someone at random
            if (!unitEntityMap.isEmpty()) {
                Entity entity = ObjectUtility.getRandomItem(unitEntityMap.values());
                entity.setCommander(true);
            }
        } else {
            Person overallCommander = null;
            for (Unit unit : prospectiveCommanders) {
                Person commander = unit.getCommander();

                if (commander.outRanksUsingSkillTiebreaker(getCampaign(), overallCommander)) {
                    overallCommander = commander;
                    commandUnit = unit;
                }
            }

            if (commandUnit != null) {
                Entity entity = unitEntityMap.get(commandUnit);
                entity.setCommander(true);
            } else {
                if (!unitEntityMap.isEmpty()) {
                    commandUnit = ObjectUtility.getRandomItem(unitEntityMap.keySet());
                }
            }
        }

        for (Unit unit : unitEntityMap.keySet()) {
            if (!unDeployedUnits.contains(unit)) {
                // Make sure the unit's entity and pilot are fully up to date!
                unit.resetPilotAndEntity();

                // Assign commander - we need to do this here because otherwise the above step will wipe it
                Entity commandEntity = unitEntityMap.get(commandUnit);

                if (commandEntity != null) {
                    commandEntity.setCommander(true);
                }

                // Add and run
                chosen.add(unit);
            }
        }

        if (scenario instanceof AtBDynamicScenario atBDynamicScenario) {
            AtBDynamicScenarioFactory.setPlayerDeploymentTurns(atBDynamicScenario, getCampaign());
            AtBDynamicScenarioFactory.finalizeStaggeredDeploymentTurns(atBDynamicScenario, getCampaign());
            AtBDynamicScenarioFactory.setPlayerDeploymentZones(atBDynamicScenario, getCampaign());
        }

        if (!unDeployed.isEmpty()) {
            Object[] options = { "Continue", "Cancel" };
            int n = JOptionPane.showOptionDialog(getFrame(),
                  "The following units could not be deployed:" + unDeployed,
                  "Could not deploy some units",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.WARNING_MESSAGE,
                  null,
                  options,
                  options[1]);

            if (n == 1) {
                return;
            }
        }

        // Ensure that the MegaMek year GameOption matches the campaign year
        // this is being set early on so that when setting up the autoconfig munitions
        // the correct year is used
        getCampaign().getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(getCampaign().getGameYear());

        // code to support deployment of reinforcements for legacy ATB scenarios.
        if ((scenario instanceof AtBScenario atBScenario) && !(scenario instanceof AtBDynamicScenario)) {

            CombatTeam combatTeam = atBScenario.getCombatTeamById(getCampaign());
            if (combatTeam != null) {
                int assignedForceId = combatTeam.getForceId();
                int cmdrStrategy = 0;
                Person commander = getCampaign().getPerson(CombatTeam.findCommander(assignedForceId, getCampaign()));
                if ((null != commander) && (null != commander.getSkill(SkillType.S_STRATEGY))) {
                    cmdrStrategy = commander.getSkill(SkillType.S_STRATEGY).getLevel();
                }
                List<Entity> reinforcementEntities = new ArrayList<>();

                for (Unit unit : chosen) {
                    if (unit.getForceId() != assignedForceId) {
                        reinforcementEntities.add(unit.getEntity());
                    }
                }

                AtBDynamicScenarioFactory.setDeploymentTurnsForReinforcements(getCampaign().getHangar(),
                      scenario,
                      reinforcementEntities,
                      cmdrStrategy);
            }
        }

        if (getCampaign().getCampaignOptions().isUseAtB() && (scenario instanceof AtBScenario atBScenario)) {
            atBScenario.refresh(getCampaign());

            // Autoconfigure munitions for all non-player forces once more, using finalized
            // forces
            if (getCampaign().getCampaignOptions().isAutoConfigMunitions()) {
                autoconfigureBotMunitions(atBScenario, chosen);
            }
            configureBotAi(atBScenario);
        }

        if (scenario.getStratConScenarioType().isConvoy() && (autoResolveBehaviorSettings != null)) {
            try {
                autoResolveBehaviorSettings = autoResolveBehaviorSettings.getCopy();
                autoResolveBehaviorSettings.setIAmAPirate(true);
            } catch (PrincessException e) {
                logger.error("Failed to copy autoResolveBehaviorSettings", e);
            }
        }

        if (!chosen.isEmpty()) {
            getCampaignGui().getApplication().startHost(scenario, false, chosen, autoResolveBehaviorSettings);
        }
    }

    private void configureBotAi(AtBScenario scenario) {
        Faction opFor = getEnemyFactionFromScenario(scenario);
        boolean isPirate = opFor.isRebelOrPirate();
        for (var bf : scenario.getBotForces()) {
            bf.getBehaviorSettings().setIAmAPirate(isPirate);
        }
    }

    /**
     * Get the enemy faction from the Mission from the scenario
     *
     * @param scenario the scenario to get the enemy faction from
     *
     * @return the enemy faction
     */
    private Faction getEnemyFactionFromScenario(Scenario scenario) {
        Mission mission = null;
        if (scenario.getMissionId() != -1) {
            mission = getCampaign().getMission(scenario.getMissionId());
        }
        if (mission == null) {
            mission = comboMission.getSelectedItem();
        }
        String opForFactionCode = "IS";
        Faction enemy;
        if (mission instanceof AtBContract atBContract) {
            enemy = atBContract.getEnemy();
            if (enemy != null) {
                return atBContract.getEnemy();
            }
            opForFactionCode = atBContract.getEnemyCode().isBlank() ? opForFactionCode : atBContract.getEnemyCode();
        }
        enemy = Factions.getInstance().getFaction(opForFactionCode);
        return enemy;
    }

    /**
     * Designed to fully kit out all non-player-controlled forces prior to battle. Does not do any checks for supplies,
     * only for availability to each faction during the current timeframe.
     *
     */
    private void autoconfigureBotMunitions(AtBScenario scenario, List<Unit> chosen) {
        Game cGame = getCampaign().getGame();
        boolean groundMap = scenario.getBoardType() == AtBScenario.T_GROUND;
        boolean spaceMap = scenario.getBoardType() == AtBScenario.T_SPACE;
        ArrayList<Entity> alliedEntities = new ArrayList<>();

        ArrayList<String> allyFactionCodes = new ArrayList<>();
        ArrayList<String> opForFactionCodes = new ArrayList<>();
        String opForFactionCode = "IS";
        String allyFaction = "IS";
        int opForQuality = RATING_5;
        HashMap<Integer, ArrayList<Entity>> botTeamMappings = new HashMap<>();
        int allowedYear = cGame.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);

        // This had better be an AtB contract...
        final Mission mission = comboMission.getSelectedItem();
        if (mission instanceof AtBContract atbContract) {
            opForFactionCode = (atbContract.getEnemyCode().isBlank()) ? opForFactionCode : atbContract.getEnemyCode();
            opForQuality = atbContract.getEnemyQuality();
            allyFactionCodes.add(atbContract.getEmployerCode());
            allyFaction = atbContract.getEmployerName(allowedYear);
        } else {
            allyFactionCodes.add(allyFaction);
        }
        Faction opforFaction = Factions.getInstance().getFaction(opForFactionCode);
        opForFactionCodes.add(opForFactionCode);
        boolean isPirate = opforFaction.isRebelOrPirate();

        // Collect player units to use as configuration fodder
        ArrayList<Entity> playerEntities = new ArrayList<>();
        for (final Unit unit : chosen) {
            playerEntities.add(unit.getEntity());
        }
        allyFactionCodes.add(getCampaign().getFaction().getShortName());

        // Split up bot forces into teams for separate handling
        for (final BotForce botForce : scenario.getBotForces()) {
            // Do not include Turrets
            List<Entity> filteredEntityList =
                  botForce.getFixedEntityList().stream().filter(
                        e -> !(e instanceof GunEmplacement)
                  ).toList();
            if (botForce.getName().contains(allyFaction)) {
                // Stuff with our employer's name should be with us.
                playerEntities.addAll(filteredEntityList);
                alliedEntities.addAll(filteredEntityList);
            } else {
                int botTeam = botForce.getTeam();
                if (!botTeamMappings.containsKey(botTeam)) {
                    botTeamMappings.put(botTeam, new ArrayList<>());
                }
                botTeamMappings.get(botTeam).addAll(filteredEntityList);
            }
        }

        // Configure generated units with appropriate munitions (for BV calculations)
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(cGame);

        // Reconfigure each group separately so they only consider their own
        // capabilities
        for (ArrayList<Entity> entityList : botTeamMappings.values()) {
            // bin fill ratio will be adjusted by the loadout generator based on piracy and
            // quality
            ReconfigurationParameters rp = TeamLoadOutGenerator.generateParameters(cGame,
                  cGame.getOptions(),
                  entityList,
                  opForFactionCode,
                  playerEntities,
                  allyFactionCodes,
                  opForQuality,
                  ((isPirate) ? TeamLoadOutGenerator.UNSET_FILL_RATIO : 1.0f));
            rp.isPirate = isPirate;
            rp.groundMap = groundMap;
            rp.spaceEnvironment = spaceMap;
            MunitionTree mt = TeamLoadOutGenerator.generateMunitionTree(rp, entityList, "");
            tlg.reconfigureEntities(entityList, opForFactionCode, mt, rp);
        }

        // Finally, reconfigure all allies (but not player entities) as one organization
        ArrayList<Entity> allEnemyEntities = new ArrayList<>();
        botTeamMappings.values().forEach(allEnemyEntities::addAll);
        ReconfigurationParameters rp = TeamLoadOutGenerator.generateParameters(cGame,
              cGame.getOptions(),
              alliedEntities,
              allyFactionCodes.get(0),
              allEnemyEntities,
              opForFactionCodes,
              opForQuality,
              (getCampaign().getFaction().isPirate()) ? TeamLoadOutGenerator.UNSET_FILL_RATIO : 1.0f);
        rp.isPirate = getCampaign().getFaction().isPirate();
        rp.groundMap = groundMap;
        rp.spaceEnvironment = spaceMap;
        MunitionTree mt = TeamLoadOutGenerator.generateMunitionTree(rp, alliedEntities, "");
        tlg.reconfigureEntities(alliedEntities, allyFactionCodes.get(0), mt, rp);

    }

    private void joinScenario() {
        Scenario scenario = getScenario();
        if (scenario == null) {
            return;
        }
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(false);
        if (uids.isEmpty()) {
            return;
        }

        List<Unit> chosen = new ArrayList<>();
        StringBuilder unDeployed = new StringBuilder();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to date!
                    u.resetPilotAndEntity();

                    // Add and run
                    chosen.add(u);

                } else {
                    unDeployed.append('\n').append(u.getName()).append(" (").append(u.checkDeployment()).append(')');
                }
            }
        }

        if (!unDeployed.isEmpty()) {
            Object[] options = { "Continue", "Cancel" };
            int n = JOptionPane.showOptionDialog(getFrame(),
                  "The following units could not be deployed:" + unDeployed,
                  "Could not deploy some units",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.WARNING_MESSAGE,
                  null,
                  options,
                  options[1]);
            if (n == 1) {
                return;
            }
        }

        if (!chosen.isEmpty()) {
            getCampaignGui().getApplication().joinGame(scenario, chosen);
        }
    }

    private void deployListFile() {
        final Scenario scenario = getScenario();
        if (scenario == null) {
            return;
        }

        // First, we need to get all units assigned to the current scenario
        final List<UUID> unitIds = scenario.getForces(getCampaign()).getAllUnits(false);

        // Then, we need to convert the ids to units, and filter out any units that are
        // null and
        // any units with null entities
        final List<Unit> units = unitIds.stream()
                                       .map(unitId -> getCampaign().getUnit(unitId))
                                       .filter(unit -> (unit != null) && (unit.getEntity() != null))
                                       .toList();

        final ArrayList<Entity> chosen = new ArrayList<>();
        final StringBuilder unDeployed = new StringBuilder();

        for (final Unit unit : units) {
            if (unit.checkDeployment() == null) {
                unit.resetPilotAndEntity();
                chosen.add(unit.getEntity());
            } else {
                unDeployed.append('\n').append(unit.getName()).append(" (").append(unit.checkDeployment()).append(')');
            }
        }

        if (!unDeployed.isEmpty()) {
            final Object[] options = { "Continue", "Cancel" };
            if (JOptionPane.showOptionDialog(getFrame(),
                  "The following units could not be deployed:" + unDeployed,
                  "Could not deploy some units",
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.WARNING_MESSAGE,
                  null,
                  options,
                  options[1]) == JOptionPane.NO_OPTION) {
                return;
            }
        }

        File file = determineMULFilePath(scenario, getCampaign().getName());
        if (file == null) {
            return;
        }

        try {
            // Save the player's entities to the file.
            EntityListFile.saveTo(file, chosen);
        } catch (Exception ex) {
            logger.error("", ex);
        }

        final Mission mission = comboMission.getSelectedItem();
        if ((mission instanceof AtBContract) &&
                  (scenario instanceof AtBScenario) &&
                  !((AtBScenario) scenario).getAlliesPlayer().isEmpty()) {
            // Export allies
            chosen.clear();
            chosen.addAll(((AtBScenario) scenario).getAlliesPlayer());
            file = determineMULFilePath(scenario, ((AtBContract) mission).getEmployer());

            int genericBattleValue = calculateGenericBattleValue(chosen);

            if (file != null) {
                try {
                    // Save the player's allied entities to the file.
                    EntityListFile.saveTo(file, chosen, genericBattleValue);
                } catch (Exception ex) {
                    logger.error("", ex);
                }
            }
        }

        // Export Bot forces
        for (final BotForce botForce : scenario.getBotForces()) {
            chosen.clear();
            chosen.addAll(botForce.getFullEntityList(getCampaign()));
            if (chosen.isEmpty()) {
                continue;
            }
            file = determineMULFilePath(scenario, botForce.getName());

            int genericBattleValue = calculateGenericBattleValue(chosen);

            if (file != null) {
                try {
                    // Save the bot force's entities to the file.
                    EntityListFile.saveTo(file, chosen, genericBattleValue);
                } catch (Exception ex) {
                    logger.error("", ex);
                }
            }
        }
    }

    /**
     * Calculates the total generic battle value of the entities chosen. If the use of generic battle value option is
     * enabled in the campaign options, the generic battle value of each entity in the list is summed up and returned as
     * the total generic battle value. If the said option is disabled, the method returns 0.
     *
     * @param chosen the list of entities for which the generic battle value is to be calculated.
     *
     * @return the total generic battle value or 0 if the generic battle value usage is turned off in campaign options.
     */
    private int calculateGenericBattleValue(ArrayList<Entity> chosen) {
        int genericBattleValue = 0;
        if (getCampaign().getCampaignOptions().isUseGenericBattleValue()) {
            genericBattleValue = chosen.stream().mapToInt(Entity::getGenericBattleValue).sum();
        }
        return genericBattleValue;
    }

    private @Nullable File determineMULFilePath(final Scenario scenario, final String name) {
        final Optional<File> maybeUnitFile = FileDialogs.saveDeployUnits(getFrame(), scenario, name);
        if (maybeUnitFile.isEmpty()) {
            return null;
        }

        final File unitFile = maybeUnitFile.get();
        if (unitFile.getName().toLowerCase().endsWith(".mul")) {
            return unitFile;
        } else {
            try {
                return new File(unitFile.getCanonicalPath() + ".mul");
            } catch (Exception ignored) {
                // nothing needs to be done here
                return null;
            }
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
        if (getCampaign().getCampaignOptions().isUseAtB()) {
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
            btnAutoResolveScenario.setEnabled(false);
            btnPrintRS.setEnabled(false);
            selectedScenario = -1;
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }
        selectedScenario = scenario.getId();
        if (getCampaign().getCampaignOptions().isUseAtB() && (scenario instanceof AtBScenario)) {
            scrollScenarioView.setViewportView(new AtBScenarioViewPanel((AtBScenario) scenario,
                  getCampaign(),
                  getFrame()));
        } else {
            scrollScenarioView.setViewportView(new ScenarioViewPanel(getFrame(), getCampaign(), scenario));
        }
        // This odd code is to make sure that the scrollbar stays at the top
        // I can't just call it here, because it ends up getting reset somewhere
        // later
        SwingUtilities.invokeLater(() -> scrollScenarioView.getVerticalScrollBar().setValue(0));

        final boolean canStartGame = ((!getCampaign().checkLinkedScenario(scenario.getId())) &&
                                            (scenario.canStartScenario(getCampaign())));

        btnStartGame.setEnabled(canStartGame);
        btnJoinGame.setEnabled(canStartGame);
        btnLoadGame.setEnabled(canStartGame);
        btnGetMul.setEnabled(canStartGame);

        final boolean hasTrack = scenario.getHasTrack();
        if (hasTrack) {
            btnClearAssignedUnits.setEnabled(canStartGame && getCampaign().isGM());
        } else {
            btnClearAssignedUnits.setEnabled(canStartGame);
        }

        btnResolveScenario.setEnabled(canStartGame);
        btnAutoResolveScenario.setEnabled(canStartGame);
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
        scenarioModel.setData((mission == null) ? new ArrayList<>() : mission.getVisibleScenarios());
        selectedScenario = -1;
        scenarioTable.setPreferredScrollableViewportSize(scenarioTable.getPreferredSize());
        scenarioTable.setFillsViewportHeight(true);
    }

    /**
     * Focuses the UI on a specific scenario by its ID.
     *
     * <p>This method searches through all missions in the campaign to find the scenario with the matching ID. If
     * found, it will:</p>
     *
     * <ol>
     *   <li>Select the parent mission in the mission combo box (if available)</li>
     *   <li>Select the scenario in the scenario table (if the table contains rows)</li>
     *   <li>Scroll the scenario table to make the selected scenario visible</li>
     *   <li>Update the selectedScenario tracking variable</li>
     * </ol>
     *
     * <p>If the parent mission is not available in the mission combo box, or if the scenario table is empty, those
     * selection steps will be skipped.</p>
     *
     * @param targetId The unique identifier of the scenario to focus on
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void focusOnScenario(int targetId) {
        Mission targetMission = null;

        // First find the mission and scenario
        for (Mission mission : getCampaign().getMissions()) {
            for (Scenario scenario : mission.getScenarios()) {
                if (scenario.getId() == targetId) {
                    targetMission = mission;
                    break;
                }
            }

            if (targetMission != null) {
                break;
            }
        }

        // If we found the mission, select it
        if (targetMission != null) {
            // Check if the targetMission is actually in the comboMission items
            boolean missionInCombo = false;
            for (int i = 0; i < comboMission.getItemCount(); i++) {
                if (comboMission.getItemAt(i).equals(targetMission)) {
                    missionInCombo = true;
                    break;
                }
            }

            if (missionInCombo) {
                comboMission.setSelectedItem(targetMission);

                // Only try to select in the table if the table has rows
                if (scenarioTable.getRowCount() > 0) {
                    for (int row = 0; row < scenarioTable.getRowCount(); row++) {
                        Scenario currentScenario = scenarioModel.getScenario(row);
                        if (currentScenario.getId() == targetId) {
                            // Select the row in the table
                            scenarioTable.setRowSelectionInterval(row, row);
                            // Ensure the selected row is visible
                            scenarioTable.scrollRectToVisible(scenarioTable.getCellRect(row, 0, true));
                            selectedScenario = row;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Focuses the UI on a specific mission by its ID.
     *
     * <p>This method retrieves the mission with the matching ID from the campaign. If the mission is found and is
     * available in the mission combo box, it will be selected, making it the currently displayed mission in the
     * UI.</p>
     *
     * <p>If the mission cannot be found in the campaign, or if it is not available in the mission combo box items,
     * no selection will occur.</p>
     *
     * @param targetId The unique identifier of the mission to focus on
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void focusOnMission(int targetId) {
        Mission mission = getCampaign().getMission(targetId);

        if (mission == null) {
            return;
        }

        // Check if the targetMission is actually in the comboMission items
        for (int i = 0; i < comboMission.getItemCount(); i++) {
            if (comboMission.getItemAt(i).equals(mission)) {
                comboMission.setSelectedItem(mission);
                break;
            }
        }
    }

    private final ActionScheduler scenarioDataScheduler = new ActionScheduler(this::refreshScenarioTableData);
    private final ActionScheduler scenarioViewScheduler = new ActionScheduler(this::refreshScenarioView);
    private final ActionScheduler missionsScheduler = new ActionScheduler(this::refreshMissions);
    private final ActionScheduler lanceAssignmentScheduler = new ActionScheduler(this::refreshLanceAssignments);

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        splitScenario.getBottomComponent().setVisible(getCampaignOptions().isUseAtB());
        splitScenario.resetToPreferredSizes();
    }

    @Subscribe
    public void handle(ScenarioChangedEvent evt) {
        final Mission mission = comboMission.getSelectedItem();
        if ((evt.getScenario() != null) &&
                  (evt.getScenario().getMissionId() == (mission == null ? -1 : mission.getId()))) {
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
        if (getCampaignOptions().isUseAtB()) {
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
