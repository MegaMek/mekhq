/*
 * Copyright (c) 2017-2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.generator.ReconfigurationParameters;
import megamek.client.generator.TeamLoadOutGenerator;
import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.Game;
import megamek.common.annotations.Nullable;
import megamek.common.containers.MunitionTree;
import megamek.common.event.Subscribe;
import megamek.common.options.OptionsConstants;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import megameklab.util.UnitPrintManager;
import mekhq.MekHQ;
import mekhq.campaign.Kill;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.ResolveScenarioTracker.PersonStatus;
import mekhq.campaign.event.*;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Lance;
import mekhq.campaign.mission.*;
import mekhq.campaign.mission.atb.AtBScenarioFactory;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.adapter.ScenarioTableMouseAdapter;
import mekhq.gui.dialog.*;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.ScenarioTableModel;
import mekhq.gui.sorter.DateStringComparator;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.view.AtBScenarioViewPanel;
import mekhq.gui.view.LanceAssignmentView;
import mekhq.gui.view.MissionViewPanel;
import mekhq.gui.view.ScenarioViewPanel;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static megamek.client.ratgenerator.ForceDescriptor.RATING_5;

/**
 * Displays Mission/Contract and Scenario details.
 */
public final class BriefingTab extends CampaignGuiTab {
    private LanceAssignmentView panLanceAssignment;
    private JSplitPane splitScenario;
    private JTable scenarioTable;
    private MMComboBox<Mission> comboMission;
    private JScrollPane scrollMissionView;
    private JScrollPane scrollScenarioView;
    private JButton btnAddScenario;
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
    private JButton btnAutoResolveScenario;

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
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
                MekHQ.getMHQOptions().getLocale());

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

        JButton btnAddMission = new JButton(resourceMap.getString("btnAddMission.text"));
        btnAddMission.setToolTipText(resourceMap.getString("btnAddMission.toolTipText"));
        btnAddMission.addActionListener(ev -> addMission());
        panMissionButtons.add(btnAddMission);

        btnAddScenario = new JButton(resourceMap.getString("btnAddScenario.text"));
        btnAddScenario.setToolTipText(resourceMap.getString("btnAddScenario.toolTipText"));
        btnAddScenario.addActionListener(ev -> addScenario());
        panMissionButtons.add(btnAddScenario);

        btnEditMission = new JButton(resourceMap.getString("btnEditMission.text"));
        btnEditMission.setToolTipText(resourceMap.getString("btnEditMission.toolTipText"));
        btnEditMission.addActionListener(ev -> editMission());
        panMissionButtons.add(btnEditMission);

        btnCompleteMission = new JButton(resourceMap.getString("btnCompleteMission.text"));
        btnCompleteMission.setToolTipText(resourceMap.getString("btnCompleteMission.toolTipText"));
        btnCompleteMission.addActionListener(ev -> completeMission());
        panMissionButtons.add(btnCompleteMission);

        btnDeleteMission = new JButton(resourceMap.getString("btnDeleteMission.text"));
        btnDeleteMission.setToolTipText(resourceMap.getString("btnDeleteMission.toolTipText"));
        btnDeleteMission.setName("btnDeleteMission");
        btnDeleteMission.addActionListener(ev -> deleteMission());
        panMissionButtons.add(btnDeleteMission);

        btnGMGenerateScenarios = new JButton(resourceMap.getString("btnGMGenerateScenarios.text"));
        btnGMGenerateScenarios.setToolTipText(resourceMap.getString("btnGMGenerateScenarios.toolTipText"));
        btnGMGenerateScenarios.setName("btnGMGenerateScenarios");
        btnGMGenerateScenarios.addActionListener(ev -> gmGenerateScenarios());
        panMissionButtons.add(btnGMGenerateScenarios);

        scrollMissionView = new JScrollPaneWithSpeed();
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

        btnGetMul = new JButton(resourceMap.getString("btnGetMul.text"));
        btnGetMul.setToolTipText(resourceMap.getString("btnGetMul.toolTipText"));
        btnGetMul.setName("btnGetMul");
        btnGetMul.addActionListener(ev -> deployListFile());
        btnGetMul.setEnabled(false);
        panScenarioButtons.add(btnGetMul);

        btnResolveScenario = new JButton(resourceMap.getString("btnResolveScenario.text"));
        btnResolveScenario.setToolTipText(resourceMap.getString("btnResolveScenario.toolTipText"));
        btnResolveScenario.addActionListener(ev -> resolveScenario());
        btnResolveScenario.setEnabled(false);
        panScenarioButtons.add(btnResolveScenario);

        btnAutoResolveScenario = new JButton(resourceMap.getString("btnAutoResolveScenario.text"));
        btnAutoResolveScenario.setToolTipText(resourceMap.getString("btnAutoResolveScenario.toolTipText"));
        btnAutoResolveScenario.addActionListener(ev -> autoResolveScenario());
        btnAutoResolveScenario.setEnabled(false);
        panScenarioButtons.add(btnAutoResolveScenario);

        btnClearAssignedUnits = new JButton(resourceMap.getString("btnClearAssignedUnits.text"));
        btnClearAssignedUnits.setToolTipText(resourceMap.getString("btnClearAssignedUnits.toolTipText"));
        btnClearAssignedUnits.addActionListener(ev -> clearAssignedUnits());
        btnClearAssignedUnits.setEnabled(false);
        panScenarioButtons.add(btnClearAssignedUnits);

        scrollScenarioView = new JScrollPaneWithSpeed();
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
        JScrollPane paneLanceDeployment = new JScrollPaneWithSpeed(panLanceAssignment);
        paneLanceDeployment.setMinimumSize(new Dimension(200, 300));
        paneLanceDeployment.setPreferredSize(new Dimension(200, 300));
        paneLanceDeployment.setVisible(getCampaign().getCampaignOptions().isUseAtB());
        splitScenario = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panScenario,
                paneLanceDeployment);
        splitScenario.setOneTouchExpandable(true);
        splitScenario.setResizeWeight(1.0);

        JSplitPane splitBrief = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panMission, splitScenario);
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
            NewContractDialog ncd = getCampaignOptions().isUseAtB()
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

        if (getCampaign().getCampaignOptions().isUseAtB() && (mission instanceof AtBContract)) {
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
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
                MekHQ.getMHQOptions().getLocale());

        final Mission mission = comboMission.getSelectedItem();

        if (mission == null) {
            return;
        } else if (mission.hasPendingScenarios()) {
            JOptionPane.showMessageDialog(getFrame(), "You cannot complete a mission that has pending scenarios",
                    "Pending Scenarios", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final CompleteMissionDialog cmd = new CompleteMissionDialog(getFrame());
        if (!cmd.showDialog().isConfirmed()) {
            return;
        }

        final MissionStatus status = cmd.getStatus();
        if (status.isActive()) {
            return;
        }

        if (getCampaign().getCampaignOptions().isUseAtB() && (mission instanceof AtBContract)) {
            if (((AtBContract) mission).contractExtended(getCampaign())) {
                return;
            }
        }

        getCampaign().completeMission(mission, status);
        MekHQ.triggerEvent(new MissionCompletedEvent(mission));

        // apply mission xp
        int xpAward = getMissionXpAward(cmd.getStatus(), mission);

        if (xpAward > 0) {
            for (Person person : getCampaign().getActivePersonnel()) {
                person.awardXP(getCampaign(), xpAward);
            }
        }

        // resolve friendly PoW ransoming
        // this needs to be before turnover and autoAwards so friendly PoWs can be
        // factored into those events
        if (getCampaign().getCampaignOptions().isUseAtBPrisonerRansom()) {
            List<Person> alliedPoWs = getCampaign().getFriendlyPrisoners();

            if (!alliedPoWs.isEmpty()) {
                Money total = alliedPoWs.stream()
                        .map(person -> person.getRansomValue(getCampaign()))
                        .reduce(Money.zero(), Money::plus);

                String message;
                int dialogOption;

                if (getCampaign().getFunds().isLessThan(total)) {
                    message = String.format(resources.getString("unableToRansom.format"), alliedPoWs.size(),
                            total.toAmountAndSymbolString());
                    dialogOption = JOptionPane.OK_CANCEL_OPTION;
                } else {
                    message = String.format(resources.getString("ransomFriendlyQ.format"), alliedPoWs.size(),
                            total.toAmountAndSymbolString());
                    dialogOption = JOptionPane.YES_NO_CANCEL_OPTION;
                }

                int optionSelected = JOptionPane.showConfirmDialog(
                        null,
                        message,
                        resources.getString("ransom.text"),
                        dialogOption);

                if (optionSelected != JOptionPane.OK_OPTION && getCampaign().getFunds().isLessThan(total)) {
                    return;
                }

                switch (optionSelected) {
                    case JOptionPane.YES_OPTION -> {
                        getCampaign().addReport(String.format(resources.getString("ransomReport.format"),
                                alliedPoWs.size(), total.toAmountAndSymbolString()));
                        getCampaign().removeFunds(TransactionType.RANSOM, total, resources.getString("ransom.text"));
                        alliedPoWs.forEach(ally -> ally.changeStatus(getCampaign(), getCampaign().getLocalDate(),
                                PersonnelStatus.ACTIVE));
                    }

                    case JOptionPane.NO_OPTION, JOptionPane.CANCEL_OPTION -> {
                    }

                    default -> {
                        return;
                    }
                }
            }
        }

        // resolve turnover
        if ((getCampaign().getCampaignOptions().isUseRandomRetirement())
                && (getCampaign().getCampaignOptions().isUseContractCompletionRandomRetirement())) {
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

        // resolve bonus parts exchange
        if (getCampaign().getCampaignOptions().isUseAtB() && (mission instanceof AtBContract)) {
            getCampaign().getContractMarket().checkForFollowup(getCampaign(), (AtBContract) mission);
            bonusPartExchange((AtBContract) mission);
        }

        // prompt autoAwards ceremony
        if (getCampaign().getCampaignOptions().isEnableAutoAwards()) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();

            // for the purposes of Mission Accomplished awards, we do not count partial
            // Successes as Success
            autoAwardsController.PostMissionController(getCampaign(), mission,
                    Objects.equals(String.valueOf(cmd.getStatus()), "Success"));
        }

        // prompt enemy prisoner ransom & freeing
        // this should always be placed after autoAwards, so that prisoners are not
        // factored into autoAwards
        if (getCampaign().getCampaignOptions().isUseAtBPrisonerRansom()) {
            List<Person> defectors = new ArrayList<>();
            List<Person> prisoners = new ArrayList<>();

            for (Person prisoner : getCampaign().getActivePersonnel()) {
                if (prisoner.getPrisonerStatus().isPrisoner()) {
                    prisoners.add(prisoner);
                } else if (prisoner.getPrisonerStatus().isPrisonerDefector()) {
                    defectors.add(prisoner);
                }
            }

            if (!defectors.isEmpty()) {
                // will return true if the prompt is canceled
                if (prisonerPrompt(defectors, "ransomDefectorsQ.format", resources)) {
                    return;
                }
            }

            if (!prisoners.isEmpty()) {
                if (prisonerPrompt(prisoners, "ransomQ.format", resources)) {
                    return;
                }
            }
        }

        // we have to rebuild the list, so we can factor in any prisoners that have been
        // ransomed.
        List<Person> prisoners = getCampaign().getActivePersonnel().stream()
                .filter(prisoner -> prisoner.getPrisonerStatus().isPrisoner())
                .toList();

        if (!prisoners.isEmpty()) {
            String title = (prisoners.size() == 1) ? prisoners.get(0).getFullTitle()
                    : String.format(resources.getString("numPrisoners.text"), prisoners.size());
            int option = JOptionPane.showConfirmDialog(null,
                    String.format(resources.getString("confirmFree.format"), title),
                    resources.getString("freeQ.text"),
                    JOptionPane.YES_NO_CANCEL_OPTION);

            switch (option) {
                case JOptionPane.YES_OPTION -> {
                    for (Person prisoner : prisoners) {
                        getCampaign().removePerson(prisoner);
                    }
                }
                case JOptionPane.NO_OPTION -> {
                }
                default -> {
                    return;
                }
            }
        }

        final List<Mission> missions = getCampaign().getSortedMissions();
        comboMission.setSelectedItem(missions.isEmpty() ? null : missions.get(0));
    }

    /**
     * Displays a prompt asking the user if they want to ransom their prisoners or
     * defectors.
     *
     * @param prisoners    The list of prisoners to be ransomed.
     * @param resourceName The name of the resource bundle key for the prompt
     *                     message.
     * @param resources    The resource bundle containing the string resources.
     * @return true if the user selects the "Cancel" option, false otherwise.
     */
    private boolean prisonerPrompt(List<Person> prisoners, String resourceName, ResourceBundle resources) {
        Money total = Money.zero();
        total = total.plus(prisoners.stream()
                .map(person -> person.getRansomValue(getCampaign()))
                .collect(Collectors.toList()));

        int optionSelected = JOptionPane.showConfirmDialog(
                null,
                String.format(resources.getString(resourceName),
                        prisoners.size(),
                        total.toAmountAndSymbolString()),
                resources.getString("ransom.text"),
                JOptionPane.YES_NO_CANCEL_OPTION);

        switch (optionSelected) {
            case JOptionPane.YES_OPTION -> {
                getCampaign().addReport(String.format(resources.getString("ransomReport.format"),
                        prisoners.size(),
                        total.toAmountAndSymbolString()));
                getCampaign().addFunds(TransactionType.RANSOM,
                        total,
                        resources.getString("ransom.text"));
                prisoners.forEach(prisoner -> getCampaign().removePerson(prisoner, false));
            }
            case JOptionPane.NO_OPTION -> {
            }
            default -> {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the XP award for completing a mission.
     *
     * @param missionStatus The status of the mission as a MissionStatus enum.
     * @param mission       The Mission object representing the completed mission.
     * @return The XP award for completing the mission.
     */
    private int getMissionXpAward(MissionStatus missionStatus, Mission mission) {
        return switch (missionStatus) {
            case FAILED, BREACH -> getCampaign().getCampaignOptions().getMissionXpFail();
            case SUCCESS, PARTIAL -> {
                if ((getCampaign().getCampaignOptions().isUseStratCon())
                        && (mission instanceof AtBContract)
                        && (((AtBContract) mission).getStratconCampaignState().getVictoryPoints() >= 3)) {
                    yield getCampaign().getCampaignOptions().getMissionXpOutstandingSuccess();
                } else {
                    yield getCampaign().getCampaignOptions().getMissionXpSuccess();
                }
            }
            case ACTIVE -> 0;
        };
    }

    /**
     * Credits the campaign finances with additional funds based on campaign
     * settings and remaining Bonus Parts.
     *
     * @param mission the mission just concluded
     */
    private void bonusPartExchange(AtBContract mission) {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
                MekHQ.getMHQOptions().getLocale());

        double bonusPartExchangeValue = getCampaign().getCampaignOptions().getBonusPartExchangeValue();

        if (bonusPartExchangeValue != 0.0) {
            int bonusPartMaxExchangeCount = getCampaign().getCampaignOptions().getBonusPartMaxExchangeCount();

            int spareBonusParts = mission.getNumBonusParts();

            if (bonusPartMaxExchangeCount != 0) {
                spareBonusParts = Math.min(bonusPartMaxExchangeCount, spareBonusParts);
            }

            bonusPartExchangeValue *= spareBonusParts;

            getCampaign().getFinances().credit(
                    TransactionType.BONUS_EXCHANGE,
                    getCampaign().getLocalDate(),
                    Money.of(bonusPartExchangeValue),
                    resourceMap.getString("spareBonusPartExchange.text"));
        }
    }

    private void deleteMission() {
        final Mission mission = comboMission.getSelectedItem();
        if (mission == null) {
            logger.error("Cannot remove null mission");
            return;
        }
        logger.debug("Attempting to Delete Mission, Mission ID: {}", mission.getId());
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

        if (0 != JOptionPane.showConfirmDialog(null, "Are you sure you want to generate a new set of scenarios?",
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

        if (!getCampaign().getRetirementDefectionTracker().getRetirees().isEmpty()) {
            RetirementDefectionDialog dialog = new RetirementDefectionDialog(getCampaignGui(),
                    getCampaign().getMission(scenario.getMissionId()), false);

            if (!dialog.wasAborted()) {
                getCampaign().applyRetirement(dialog.totalPayout(), dialog.getUnitAssignments());
            }
        }

        if (getCampaign().getCampaignOptions().isEnableAutoAwards()) {
            HashMap<UUID, Integer> personnel = new HashMap<>();
            HashMap<UUID, List<Kill>> scenarioKills = new HashMap<>();

            for (UUID personId : tracker.getPeopleStatus().keySet()) {
                Person person = getCampaign().getPerson(personId);
                PersonStatus status = tracker.getPeopleStatus().get(personId);
                int injuryCount = 0;

                if (!person.getStatus().isDead() || getCampaign().getCampaignOptions().isIssuePosthumousAwards()) {
                    if (status.getHits() > person.getHitsPrior()) {
                        injuryCount = status.getHits() - person.getHitsPrior();
                    }
                }

                personnel.put(personId, injuryCount);
                scenarioKills.put(personId, tracker.getPeopleStatus().get(personId).getKills());
            }

            boolean isCivilianHelp = false;

            if (tracker.getScenario() instanceof AtBScenario) {
                isCivilianHelp = ((AtBScenario) tracker.getScenario()).getScenarioType() == AtBScenario.CIVILIANHELP;
            }

            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.PostScenarioController(getCampaign(), personnel, scenarioKills, isCivilianHelp);
        }

        for (UUID personId : tracker.getPeopleStatus().keySet()) {
            Person person = getCampaign().getPerson(personId);

            if (person.getStatus() == PersonnelStatus.MIA && !control) {
                person.changeStatus(getCampaign(), getCampaign().getLocalDate(), PersonnelStatus.POW);
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

        // Then, we need to convert the ids to units, and filter out any units that are
        // null and
        // any units with null entities
        final List<Unit> units = unitIds.stream()
                .map(unitId -> getCampaign().getUnit(unitId))
                .filter(unit -> (unit != null) && (unit.getEntity() != null))
                .toList();

        final List<Entity> chosen = new ArrayList<>();
        final StringBuilder undeployed = new StringBuilder();

        for (final Unit unit : units) {
            if (unit.checkDeployment() == null) {
                unit.resetPilotAndEntity();
                chosen.add(unit.getEntity());
            } else {
                undeployed.append('\n').append(unit.getName()).append(" (").append(unit.checkDeployment()).append(')');
            }
        }

        if (!undeployed.isEmpty()) {
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
                .toList());

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
        startScenario(null);
    }

    private void autoResolveScenario() {
        startScenario(getCampaign().getAutoResolveBehaviorSettings());
    }

    private void startScenario(BehaviorSettings autoResolveBehaviorSettings) {
        int row = scenarioTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        Scenario scenario = scenarioModel.getScenario(scenarioTable.convertRowIndexToModel(row));
        if (scenario == null) {
            return;
        }
        Vector<UUID> uids = scenario.getForces(getCampaign()).getAllUnits(true);
        if (uids.isEmpty()) {
            return;
        }

        List<Unit> chosen = new ArrayList<>();
        StringBuilder undeployed = new StringBuilder();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if ((null != u) && (null != u.getEntity())) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to date!
                    u.resetPilotAndEntity();

                    // Add and run
                    chosen.add(u);

                } else {
                    undeployed.append('\n').append(u.getName()).append(" (").append(u.checkDeployment()).append(')');
                }
            }
        }

        if (scenario instanceof AtBDynamicScenario) {
            AtBDynamicScenarioFactory.setPlayerDeploymentTurns((AtBDynamicScenario) scenario, getCampaign());
            AtBDynamicScenarioFactory.finalizeStaggeredDeploymentTurns((AtBDynamicScenario) scenario, getCampaign());
            AtBDynamicScenarioFactory.setPlayerDeploymentZones((AtBDynamicScenario) scenario, getCampaign());
        }

        if (!undeployed.isEmpty()) {
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

        if (getCampaign().getCampaignOptions().isUseAtB() && (scenario instanceof AtBScenario)) {
            ((AtBScenario) scenario).refresh(getCampaign());

            // Autoconfigure munitions for all non-player forces once more, using finalized
            // forces
            if (getCampaign().getCampaignOptions().isAutoConfigMunitions()) {
                autoconfigureBotMunitions(((AtBScenario) scenario), chosen);
            }
        }

        if (!chosen.isEmpty()) {
            // Ensure that the MegaMek year GameOption matches the campaign year
            getCampaign().getGameOptions().getOption(OptionsConstants.ALLOWED_YEAR)
                    .setValue(getCampaign().getGameYear());
            getCampaignGui().getApplication()
                .startHost(scenario, false, chosen, autoResolveBehaviorSettings);

        }
    }

    /**
     * Designed to fully kit out all non-player-controlled forces prior to battle.
     * Does not do any checks for supplies, only for availability to each faction
     * during the current timeframe.
     *
     * @param scenario
     * @param chosen
     */
    private void autoconfigureBotMunitions(AtBScenario scenario, List<Unit> chosen) {
        Game cGame = getCampaign().getGame();
        boolean groundMap = scenario.getBoardType() == AtBScenario.T_GROUND;
        boolean spaceMap = scenario.getBoardType() == AtBScenario.T_SPACE;
        ArrayList<Entity> alliedEntities = new ArrayList<>();

        ArrayList<String> allyFactionCodes = new ArrayList<>();
        ArrayList<String> opforFactionCodes = new ArrayList<>();
        String opforFactionCode = "IS";
        String allyFaction = "IS";
        int opforQuality = RATING_5;
        HashMap<Integer, ArrayList<Entity>> botTeamMappings = new HashMap<>();
        int allowedYear = cGame.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);

        // This had better be an AtB contract...
        final Mission mission = comboMission.getSelectedItem();
        if (mission instanceof AtBContract atbc) {
            opforFactionCode = (atbc.getEnemyCode().isBlank()) ? opforFactionCode : atbc.getEnemyCode();
            opforQuality = atbc.getEnemyQuality();
            allyFactionCodes.add(atbc.getEmployerCode());
            allyFaction = atbc.getEmployerName(allowedYear);
        } else {
            allyFactionCodes.add(allyFaction);
        }
        Faction opforFaction = Factions.getInstance().getFaction(opforFactionCode);
        opforFactionCodes.add(opforFactionCode);
        boolean isPirate = opforFaction.isRebelOrPirate();

        // Collect player units to use as configuration fodder
        ArrayList<Entity> playerEntities = new ArrayList<>();
        for (final Unit unit : chosen) {
            playerEntities.add(unit.getEntity());
        }
        allyFactionCodes.add(getCampaign().getFaction().getShortName());

        // Split up bot forces into teams for separate handling
        for (final BotForce botForce : scenario.getBotForces()) {
            if (botForce.getName().contains(allyFaction)) {
                // Stuff with our employer's name should be with us.
                playerEntities.addAll(botForce.getFixedEntityList());
                alliedEntities.addAll(botForce.getFixedEntityList());
            } else {
                int botTeam = botForce.getTeam();
                if (!botTeamMappings.containsKey(botTeam)) {
                    botTeamMappings.put(botTeam, new ArrayList<>());
                }
                botTeamMappings.get(botTeam).addAll(botForce.getFixedEntityList());
            }
        }

        // Configure generated units with appropriate munitions (for BV calcs)
        TeamLoadOutGenerator tlg = new TeamLoadOutGenerator(cGame);

        // Reconfigure each group separately so they only consider their own
        // capabilities
        for (ArrayList<Entity> entityList : botTeamMappings.values()) {
            // bin fill ratio will be adjusted by the loadout generator based on piracy and
            // quality
            ReconfigurationParameters rp = TeamLoadOutGenerator.generateParameters(
                    cGame,
                    cGame.getOptions(),
                    entityList,
                    opforFactionCode,
                    playerEntities,
                    allyFactionCodes,
                    opforQuality,
                    ((isPirate) ? TeamLoadOutGenerator.UNSET_FILL_RATIO : 1.0f));
            rp.isPirate = isPirate;
            rp.groundMap = groundMap;
            rp.spaceEnvironment = spaceMap;
            MunitionTree mt = TeamLoadOutGenerator.generateMunitionTree(rp, entityList, "");
            tlg.reconfigureEntities(entityList, opforFactionCode, mt, rp);
        }

        // Finally, reconfigure all allies (but not player entities) as one organization
        ArrayList<Entity> allEnemyEntities = new ArrayList<>();
        botTeamMappings.values().stream().forEach(x -> allEnemyEntities.addAll(x));
        ReconfigurationParameters rp = TeamLoadOutGenerator.generateParameters(
                cGame,
                cGame.getOptions(),
                alliedEntities,
                allyFactionCodes.get(0),
                allEnemyEntities,
                opforFactionCodes,
                opforQuality,
                (getCampaign().getFaction().isPirate()) ? TeamLoadOutGenerator.UNSET_FILL_RATIO : 1.0f);
        rp.isPirate = getCampaign().getFaction().isPirate();
        rp.groundMap = groundMap;
        rp.spaceEnvironment = spaceMap;
        MunitionTree mt = TeamLoadOutGenerator.generateMunitionTree(rp, alliedEntities, "");
        tlg.reconfigureEntities(alliedEntities, allyFactionCodes.get(0), mt, rp);

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
        if (uids.isEmpty()) {
            return;
        }

        List<Unit> chosen = new ArrayList<>();
        StringBuilder undeployed = new StringBuilder();

        for (UUID uid : uids) {
            Unit u = getCampaign().getUnit(uid);
            if (null != u.getEntity()) {
                if (null == u.checkDeployment()) {
                    // Make sure the unit's entity and pilot are fully up to date!
                    u.resetPilotAndEntity();

                    // Add and run
                    chosen.add(u);

                } else {
                    undeployed.append('\n').append(u.getName()).append(" (").append(u.checkDeployment()).append(')');
                }
            }
        }

        if (!undeployed.isEmpty()) {
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

        // Then, we need to convert the ids to units, and filter out any units that are
        // null and
        // any units with null entities
        final List<Unit> units = unitIds.stream()
                .map(unitId -> getCampaign().getUnit(unitId))
                .filter(unit -> (unit != null) && (unit.getEntity() != null))
                .toList();

        final ArrayList<Entity> chosen = new ArrayList<>();
        final StringBuilder undeployed = new StringBuilder();

        for (final Unit unit : units) {
            if (unit.checkDeployment() == null) {
                unit.resetPilotAndEntity();
                chosen.add(unit.getEntity());
            } else {
                undeployed.append('\n').append(unit.getName()).append(" (").append(unit.checkDeployment()).append(')');
            }
        }

        if (!undeployed.isEmpty()) {
            final Object[] options = { "Continue", "Cancel" };
            if (JOptionPane.showOptionDialog(getFrame(),
                    "The following units could not be deployed:" + undeployed,
                    "Could not deploy some units", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, options, options[1]) == JOptionPane.NO_OPTION) {
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
        if ((mission instanceof AtBContract) && (scenario instanceof AtBScenario)
                && !((AtBScenario) scenario).getAlliesPlayer().isEmpty()) {
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
     * Calculates the total generic battle value of the entities chosen.
     * If the use of generic battle value option is enabled in the campaign options, the generic battle
     * value of each entity in the list is summed up and returned as the total generic battle value.
     * If the said option is disabled, the method returns 0.
     *
     * @param chosen the list of entities for which the generic battle value is to be calculated.
     * @return the total generic battle value or 0 if the generic battle value usage is turned off in
     * campaign options.
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
        scenarioModel.setData((mission == null) ? new ArrayList<Scenario>() : mission.getVisibleScenarios());
        selectedScenario = -1;
        scenarioTable.setPreferredScrollableViewportSize(scenarioTable.getPreferredSize());
        scenarioTable.setFillsViewportHeight(true);
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
        if ((evt.getScenario() != null)
                && (evt.getScenario().getMissionId() == (mission == null ? -1 : mission.getId()))) {
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
