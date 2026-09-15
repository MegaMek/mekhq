/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.stratCon.deployment;

import static mekhq.MHQConstants.CONFIRMATION_STRATCON_BATCHALL_BREACH;
import static mekhq.MHQConstants.CONFIRMATION_STRATCON_DEPLOY;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.calculateReinforcementTargetNumber;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.commanderLanceHasDefensiveAssignment;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.getEligibleFrontlineUnits;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.getEligibleLeadershipUnits;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentEvaluator;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentMode;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementAdvisor;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementCost;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementRoll;
import mekhq.campaign.digitalGM.stratCon.deployment.StratConDeploymentService;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.gui.StratConPanel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNotification;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.dialog.StratConReinforcementsConfirmationDialog;
import mekhq.gui.dialog.StratConReinforcementsConfirmationDialog.ReinforcementDialogResponseType;
import mekhq.gui.dialog.StratConSinglesReinforcementsDialog;
import mekhq.gui.dialog.SupportCarrierDeploymentDialogs;
import mekhq.gui.stratCon.ScenarioWizardLanceModel;

/**
 * The redesigned StratCon deployment wizard: a non-modal, four-page window that browses the player's forces on a
 * searchable board and shows a dossier for the focused force before it is committed.
 *
 * <p>Staging is one shared set across the four pages, typed by the page each item was staged from (primary forces,
 * reinforcement forces, auxiliary units, utility units). A single Commit batches everything staged in one action -
 * deploy, reinforcement roll, auxiliaries, utility - with one finalize and one staying-home summary. The wizard keeps
 * only the dialogs; the state changes live in the non-GUI {@link StratConDeploymentService}, and the deployment
 * arithmetic in {@link DeploymentEvaluator}.</p>
 *
 * <p>The right-click deployment entry points ({@code StratConPanel}) and the GM mapless flow
 * ({@code MaplessStratCon}) open this wizard. It replaces the former {@code StratConScenarioWizard} and
 * {@code TrackForceAssignmentUI}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConDeploymentWizard extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private final transient StratConPanel owner;
    private final transient Campaign campaign;

    private transient StratConCampaignState campaignState;
    private transient StratConScenario scenario;
    private boolean assignToScenario;
    private boolean restrictToSingleForce;

    private DeploymentMode mode = DeploymentMode.PRIMARY;

    // The full eligible set for the current page (formations on Primary/Reinforce, units on Auxiliaries/Utility), and
    // the filtered view the board actually shows.
    private final transient List<Object> allItems = new ArrayList<>();
    private final DefaultListModel<Object> boardModel = new DefaultListModel<>();
    private final JList<Object> boardList = new JList<>(boardModel);
    private final JTextField searchField = new JTextField(20);

    // One shared staged set, kept typed by the page each item was staged from, so the single Commit can batch them.
    private final transient List<Formation> stagedPrimaryForces = new ArrayList<>();
    private final transient List<Formation> stagedReinforcementForces = new ArrayList<>();
    private final transient List<Unit> stagedAuxiliaryUnits = new ArrayList<>();
    private final transient List<Unit> stagedUtilityUnits = new ArrayList<>();

    // On the Reinforce page, the reinforcement template slot each eligible force fills (needed to commit it).
    private final transient Map<Integer, String> reinforcementTemplateByForceId = new LinkedHashMap<>();

    // Budget inputs for the unit pages, gathered from the scenario when its page is loaded.
    private int leadershipSkill;
    private int leadershipPointsUsed;
    private int defensivePoints;

    private final Map<DeploymentMode, JToggleButton> modeButtons = new EnumMap<>(DeploymentMode.class);
    private transient ReinforcementAdvisor reinforcementAdvisor;

    private final transient DeploymentInspectorPanel inspector;

    public StratConDeploymentWizard(StratConPanel owner, Campaign campaign) {
        super((java.awt.Frame) null, false);
        this.owner = owner;
        this.campaign = campaign;
        this.inspector = new DeploymentInspectorPanel(campaign);

        setTitle(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.title"));
        setLayout(new BorderLayout());
        add(buildCommandBar(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);

        wireBoard();
        wireInspector();

        setResizable(true);
        setSize(new Dimension(960, 640));
        setLocationRelativeTo(owner);
    }

    /**
     * Populates the wizard for a deployment and shows it. Mirrors the inputs the legacy {@code TrackForceAssignmentUI}
     * takes for the primary-force pick.
     *
     * @param campaignState        the current StratCon campaign state
     * @param scenario             the scenario being deployed to, or {@code null} for a bare-hex deployment
     * @param assignToScenario     {@code true} to assign to an unresolved scenario, {@code false} to deploy to the hex
     * @param restrictToSingleForce {@code true} when only one primary force may be staged (official challenges)
     * @param initialMode          the page to open on (Primary for an initial deploy, Reinforce for managing a
     *                             committed scenario)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void display(StratConCampaignState campaignState, @Nullable StratConScenario scenario,
          boolean assignToScenario, boolean restrictToSingleForce, DeploymentMode initialMode) {
        this.campaignState = campaignState;
        this.scenario = scenario;
        this.assignToScenario = assignToScenario;
        this.restrictToSingleForce = restrictToSingleForce;
        this.reinforcementAdvisor = (scenario == null)
                                          ? null
                                          : new ReinforcementAdvisor(campaign, campaignState, owner.getCurrentTrack());

        clearStaged();
        mode = initialMode;
        modeButtons.get(initialMode).setSelected(true);
        enterMode();

        setVisible(true);
    }

    private void switchMode(DeploymentMode newMode) {
        if (mode == newMode) {
            return;
        }
        mode = newMode;
        enterMode();
    }

    /**
     * Loads the current page's items and refreshes the board, inspector, and budget. Staged selections persist across a
     * mode switch (they are only cleared when the wizard is reopened), so the batch Commit can gather them all.
     */
    private void enterMode() {
        loadEligibleItems();
        applySearchFilter();

        inspector.showEmpty();
        inspector.setStageButtonEnabled(false);
        refreshStagedTray();
        updateBudget();
        refreshCommitEnabled();
    }

    private void clearStaged() {
        stagedPrimaryForces.clear();
        stagedReinforcementForces.clear();
        stagedAuxiliaryUnits.clear();
        stagedUtilityUnits.clear();
    }

    private JPanel buildCommandBar() {
        JPanel commandBar = new JPanel(new BorderLayout());

        JPanel modeStrip = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup modeGroup = new ButtonGroup();
        for (DeploymentMode deploymentMode : DeploymentMode.values()) {
            JToggleButton modeButton = new JToggleButton(deploymentMode.getLabel());
            modeGroup.add(modeButton);
            modeButtons.put(deploymentMode, modeButton);
            modeButton.addActionListener(event -> switchMode(deploymentMode));
            modeStrip.add(modeButton);
        }

        JLabel instructions = new JLabel(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.primary.instructions"));
        instructions.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));

        commandBar.add(modeStrip, BorderLayout.CENTER);
        commandBar.add(instructions, BorderLayout.SOUTH);
        return commandBar;
    }

    private JSplitPane buildBody() {
        JPanel boardPanel = new JPanel(new BorderLayout(0, 4));

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchRow.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.search.label")));
        searchRow.add(searchField);

        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardList.setCellRenderer(new DeploymentItemRenderer(campaign));

        boardPanel.add(searchRow, BorderLayout.NORTH);
        boardPanel.add(new JScrollPane(boardList), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardPanel, inspector);
        splitPane.setResizeWeight(0.58);
        return splitPane;
    }

    private void wireBoard() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySearchFilter();
            }
        });

        boardList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                focusSelectedItem();
            }
        });
    }

    private void wireInspector() {
        inspector.getStageButton().addActionListener(event -> toggleStageSelected());
        inspector.getCancelButton().addActionListener(event -> dispose());
        inspector.getCommitButton().addActionListener(event -> commit());
    }

    // region board loading

    private void loadEligibleItems() {
        allItems.clear();

        switch (mode) {
            case PRIMARY -> loadEligiblePrimaryFormations();
            case REINFORCE -> loadEligibleReinforcementForces();
            case AUXILIARIES -> loadEligibleAuxiliaryUnits();
            case UTILITY -> loadEligibleUtilityUnits();
        }
    }

    private void loadEligiblePrimaryFormations() {
        // Mirrors TrackForceAssignmentUI's primary-force query: the whole eligible mix from the current track.
        List<Integer> eligibleIds = StratConRulesManager.getAvailableForceIDsForManualDeployment(
              ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX,
              campaign,
              owner.getCurrentTrack(),
              false,
              null,
              campaignState,
              restrictToSingleForce);

        addFormationsToBoard(eligibleIds);
    }

    private void loadEligibleReinforcementForces() {
        reinforcementTemplateByForceId.clear();
        if (scenario == null) {
            return;
        }

        // Query each reinforcement template slot separately (as the legacy wizard did) so we know which template every
        // eligible force fills - the commit needs that mapping.
        List<Integer> orderedIds = new ArrayList<>();
        for (ScenarioForceTemplate forceTemplate : scenario.getScenarioTemplate().getAllPlayerReinforcementForces()) {
            boolean arrivesAsReinforcements = forceTemplate.getArrivalTurn() ==
                                                    ScenarioForceTemplate.ARRIVAL_TURN_AS_REINFORCEMENTS;
            List<Integer> templateIds = StratConRulesManager.getAvailableForceIDsForManualDeployment(
                  forceTemplate.getAllowedUnitType(),
                  campaign,
                  owner.getCurrentTrack(),
                  arrivesAsReinforcements,
                  scenario,
                  campaignState,
                  false);

            for (int forceId : templateIds) {
                if (reinforcementTemplateByForceId.putIfAbsent(forceId, forceTemplate.getForceName()) == null) {
                    orderedIds.add(forceId);
                }
            }
        }

        addFormationsToBoard(orderedIds);
    }

    private void loadEligibleAuxiliaryUnits() {
        if (scenario == null) {
            return;
        }

        leadershipSkill = resolveLeadershipSkill();
        leadershipPointsUsed = scenario.getLeadershipPointsUsed();
        allItems.addAll(getEligibleLeadershipUnits(campaign, scenario, leadershipSkill));
    }

    private void loadEligibleUtilityUnits() {
        if (scenario == null) {
            return;
        }

        defensivePoints = scenario.getNumDefensivePoints();
        allItems.addAll(getEligibleFrontlineUnits(campaign, scenario));
    }

    /**
     * @return the commander's leadership skill for auxiliary-unit budgeting: zero unless the commander lance is on a
     *       defensive assignment, and always zero for official challenges (leadership units would be cheating)
     */
    private int resolveLeadershipSkill() {
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        if (!commanderLanceHasDefensiveAssignment(backingScenario, campaign)) {
            return 0;
        }
        if (backingScenario.getStratConScenarioType().isOfficialChallenge()) {
            return 0;
        }
        return backingScenario.getLanceCommanderSkill(S_LEADER, campaign);
    }

    private void addFormationsToBoard(List<Integer> forceIds) {
        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign, forceIds);
        for (int index = 0; index < lanceModel.getSize(); index++) {
            allItems.add(lanceModel.getElementAt(index));
        }
    }

    private void applySearchFilter() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        Object focused = boardList.getSelectedValue();

        boardModel.clear();
        for (Object item : allItems) {
            if (matchesQuery(item, query)) {
                boardModel.addElement(item);
            }
        }

        // Keep the focused item selected if it survived the filter, so the dossier does not flicker away mid-search.
        if ((focused != null) && boardModel.contains(focused)) {
            boardList.setSelectedValue(focused, true);
        }
    }

    private static boolean matchesQuery(Object item, String query) {
        if (query.isEmpty()) {
            return true;
        }
        if (item instanceof Formation formation) {
            return formation.getName().toLowerCase(Locale.ROOT).contains(query) ||
                         formation.getFullName().toLowerCase(Locale.ROOT).contains(query);
        }
        if (item instanceof Unit unit) {
            return unit.getName().toLowerCase(Locale.ROOT).contains(query);
        }
        return false;
    }

    // endregion

    // region focus and staging

    private void focusSelectedItem() {
        Object focused = boardList.getSelectedValue();
        if (focused == null) {
            inspector.showEmpty();
            inspector.setStageButtonEnabled(false);
            return;
        }

        if (focused instanceof Unit unit) {
            inspector.showUnit(unit);
        } else if (focused instanceof Formation formation) {
            if ((mode == DeploymentMode.REINFORCE) && (reinforcementAdvisor != null)) {
                ReinforcementEligibilityType eligibility = reinforcementAdvisor.getEligibility(formation.getId());
                // Preview the roll at no support-point spend; the actual spend is chosen in the commit dialog.
                ReinforcementRoll roll = reinforcementAdvisor.getRoll(0, false);
                int perForceCost = DeploymentEvaluator.reinforcementCost(0, false, 1).perForceSupportPoints();
                inspector.showReinforcementFormation(formation, eligibility, roll, perForceCost);
            } else {
                inspector.showFormation(formation);
            }
        }

        inspector.setStageButtonEnabled(true);
        inspector.setStageButtonStaged(isStaged(focused));
    }

    private void toggleStageSelected() {
        Object focused = boardList.getSelectedValue();
        if (focused == null) {
            return;
        }

        if (isStaged(focused)) {
            unstage(focused);
        } else {
            // Official challenges permit a single primary force: staging a new one replaces whatever was staged before.
            if (restrictToSingleForce && (mode == DeploymentMode.PRIMARY)) {
                stagedPrimaryForces.clear();
            }
            stage(focused);
        }

        inspector.setStageButtonStaged(isStaged(focused));
        refreshStagedTray();
        updateBudget();
        refreshCommitEnabled();
    }

    private boolean isStaged(Object item) {
        if (item instanceof Formation formation) {
            return (mode == DeploymentMode.PRIMARY)
                         ? stagedPrimaryForces.contains(formation)
                         : stagedReinforcementForces.contains(formation);
        }
        if (item instanceof Unit unit) {
            return (mode == DeploymentMode.AUXILIARIES)
                         ? stagedAuxiliaryUnits.contains(unit)
                         : stagedUtilityUnits.contains(unit);
        }
        return false;
    }

    private void stage(Object item) {
        if (item instanceof Formation formation) {
            (mode == DeploymentMode.PRIMARY ? stagedPrimaryForces : stagedReinforcementForces).add(formation);
        } else if (item instanceof Unit unit) {
            (mode == DeploymentMode.AUXILIARIES ? stagedAuxiliaryUnits : stagedUtilityUnits).add(unit);
        }
    }

    private void unstage(Object item) {
        if (item instanceof Formation formation) {
            stagedPrimaryForces.remove(formation);
            stagedReinforcementForces.remove(formation);
        } else if (item instanceof Unit unit) {
            stagedAuxiliaryUnits.remove(unit);
            stagedUtilityUnits.remove(unit);
        }
    }

    private void refreshStagedTray() {
        List<Object> staged = new ArrayList<>();
        staged.addAll(stagedPrimaryForces);
        staged.addAll(stagedReinforcementForces);
        staged.addAll(stagedAuxiliaryUnits);
        staged.addAll(stagedUtilityUnits);
        inspector.setStaged(staged);
    }

    private boolean nothingStaged() {
        return stagedPrimaryForces.isEmpty() &&
                     stagedReinforcementForces.isEmpty() &&
                     stagedAuxiliaryUnits.isEmpty() &&
                     stagedUtilityUnits.isEmpty();
    }

    private void updateBudget() {
        switch (mode) {
            case AUXILIARIES -> inspector.setBudget(getFormattedTextAt(RESOURCE_BUNDLE,
                  "deploymentWizard.budget.leadership",
                  leadershipBattleValueRemaining()));
            case UTILITY -> inspector.setBudget(getFormattedTextAt(RESOURCE_BUNDLE,
                  "deploymentWizard.budget.minefields",
                  minefieldsRemaining()));
            default -> inspector.clearBudget();
        }
    }

    private int leadershipBattleValueRemaining() {
        int remaining = DeploymentEvaluator.leadershipPointsRemaining(leadershipSkill, leadershipPointsUsed);
        for (Unit unit : stagedAuxiliaryUnits) {
            if (unit.getEntity() != null) {
                remaining -= unit.getEntity().calculateBattleValue(true, true);
            }
        }
        return remaining;
    }

    private int minefieldsRemaining() {
        return DeploymentEvaluator.minefieldsRemaining(defensivePoints, stagedUtilityUnits.size());
    }

    private void refreshCommitEnabled() {
        inspector.getCommitButton().setEnabled(!nothingStaged());
    }

    // endregion

    // region commit

    /**
     * Batches every staged page into one commit: any reinforcement forces are confirmed and rolled first (their dialog
     * can abort the whole commit before anything is applied), then primary forces are deployed and auxiliary/utility
     * units added, then the scenario is finalized once and a single staying-home summary is shown.
     */
    private void commit() {
        // Reinforcements carry their own confirmation dialog; the rest use the deploy nag.
        boolean hasNonReinforcement = !stagedPrimaryForces.isEmpty() ||
                                            !stagedAuxiliaryUnits.isEmpty() ||
                                            !stagedUtilityUnits.isEmpty();
        if (hasNonReinforcement && !confirmDeployment()) {
            return;
        }

        List<Formation> committedReinforcements = new ArrayList<>();
        if (!stagedReinforcementForces.isEmpty() && !processReinforcementBatch(committedReinforcements)) {
            return;
        }

        if (!stagedPrimaryForces.isEmpty()) {
            deployPrimaryForces();
        }
        if (!stagedAuxiliaryUnits.isEmpty()) {
            StratConDeploymentService.addAuxiliaryUnits(scenario, stagedAuxiliaryUnits);
        }
        if (!stagedUtilityUnits.isEmpty()) {
            StratConDeploymentService.addUtilityUnits(scenario, stagedUtilityUnits);
            StratConDeploymentService.setMinefieldCount(scenario, minefieldsRemaining());
        }

        boolean managedScenario = !stagedReinforcementForces.isEmpty() ||
                                        !stagedAuxiliaryUnits.isEmpty() ||
                                        !stagedUtilityUnits.isEmpty();
        if (managedScenario && (scenario != null)) {
            StratConDeploymentService.finalizeForceDeployment(campaign, owner.getCurrentTrack(), scenario);
        }

        if (!committedReinforcements.isEmpty()) {
            SupportCarrierDeploymentDialogs.showStayingHome(campaign,
                  committedReinforcements,
                  scenario.getBackingScenario());
        }

        finishCommit();
    }

    private void deployPrimaryForces() {
        List<Integer> stagedForceIds = new ArrayList<>();
        for (Formation formation : stagedPrimaryForces) {
            stagedForceIds.add(formation.getId());
        }

        StratConDeploymentService.deployPrimaryForces(campaign,
              campaignState,
              owner.getCurrentTrack(),
              owner.getSelectedCoords(),
              assignToScenario,
              stagedForceIds);
    }

    /**
     * Reinforcement confirmation flow, ported from the legacy wizard. Applies the player's choice (spending support
     * points and committing forces) and collects the committed formations.
     *
     * @return {@code true} if reinforcements were committed (or there were none to commit); {@code false} if the player
     *       cancelled or backed out, which aborts the whole commit
     */
    private boolean processReinforcementBatch(List<Formation> committedOut) {
        if (scenario == null) {
            return false;
        }

        Map<String, List<Formation>> forcesByTemplate = groupStagedReinforcementsByTemplate();
        int selectedForceCount = stagedReinforcementForces.size();

        if (campaign.getCampaignOptions().isUseStratConSinglesMode()) {
            StratConSinglesReinforcementsDialog dialog = new StratConSinglesReinforcementsDialog(campaign);
            if (dialog.getResponseType() == ReinforcementDialogResponseType.REINFORCE_GM_INSTANTLY) {
                committedOut.addAll(applyReinforcements(forcesByTemplate, 0, true, true, false, null, null));
                return true;
            }
            return false;
        }

        if (scenario.getBackingScenario().getStratConScenarioType().isOfficialChallenge()) {
            new ImmersiveDialogNotification(campaign, getTextAt(RESOURCE_BUNDLE, "officialChallenge.notice"), true);
            return false;
        }

        Person commandLiaison = campaign.getPlayerForce()
                                      .getHumanResources()
                                      .getSeniorAdminPerson(campaign.getCampaignOptions(),
                                            campaign.getPlayerForce().isClanForce(),
                                            campaign.getLocalDate());
        int baseTargetNumber = campaign.getCampaignOptions().get(CampaignOption.REINFORCEMENT_BASE_TARGET_NUMBER);
        TargetRoll targetNumber = calculateReinforcementTargetNumber(commandLiaison,
              campaignState.getContract(),
              baseTargetNumber);
        int availableSupportPoints = campaignState.getSupportPoints();

        AbstractContract contract = scenario.getBackingContract(campaign);
        Faction enemy = contract.getEnemyFaction();

        boolean brokeBatchallTerms = false;
        if (enemy.isClan() && contract.isBatchallAccepted()) {
            if (backOutAtBatchallWarning()) {
                return false;
            }
            brokeBatchallTerms = true;
        }

        StratConReinforcementsConfirmationDialog dialog = new StratConReinforcementsConfirmationDialog(campaign,
              targetNumber,
              availableSupportPoints,
              selectedForceCount);
        switch (dialog.getResponseType()) {
            case CANCEL -> {
                return false;
            }
            case REINFORCE -> committedOut.addAll(applyPaidReinforcements(forcesByTemplate,
                  false,
                  dialog.getSupportPoints(),
                  selectedForceCount,
                  targetNumber,
                  brokeBatchallTerms,
                  contract,
                  enemy.getShortName()));
            case REINFORCE_INSTANTLY -> committedOut.addAll(applyPaidReinforcements(forcesByTemplate,
                  true,
                  dialog.getSupportPoints(),
                  selectedForceCount,
                  targetNumber,
                  brokeBatchallTerms,
                  contract,
                  enemy.getShortName()));
            case REINFORCE_GM -> committedOut.addAll(applyReinforcements(forcesByTemplate,
                  0,
                  true,
                  false,
                  brokeBatchallTerms,
                  contract,
                  enemy.getShortName()));
            case REINFORCE_GM_INSTANTLY -> committedOut.addAll(applyReinforcements(forcesByTemplate,
                  0,
                  true,
                  true,
                  brokeBatchallTerms,
                  contract,
                  enemy.getShortName()));
        }
        return true;
    }

    private List<Formation> applyPaidReinforcements(Map<String, List<Formation>> forcesByTemplate, boolean instant,
          int chosenSupportPoints, int selectedForceCount, TargetRoll targetNumber, boolean brokeBatchallTerms,
          AbstractContract contract, String enemyCode) {
        ReinforcementCost cost = DeploymentEvaluator.reinforcementCost(chosenSupportPoints, instant, selectedForceCount);
        campaignState.changeSupportPoints(-cost.totalSupportPoints());
        int finalTargetNumber = targetNumber.getValue() + cost.targetNumberModifier();
        return applyReinforcements(forcesByTemplate,
              finalTargetNumber,
              false,
              instant,
              brokeBatchallTerms,
              contract,
              enemyCode);
    }

    private List<Formation> applyReinforcements(Map<String, List<Formation>> forcesByTemplate, int targetNumber,
          boolean isGM, boolean isInstant, boolean brokeBatchallTerms, @Nullable AbstractContract contract,
          @Nullable String enemyCode) {
        List<Formation> committed = StratConDeploymentService.commitReinforcementForces(campaign,
              campaignState,
              owner.getCurrentTrack(),
              scenario,
              forcesByTemplate,
              targetNumber,
              isGM,
              isInstant);

        if (brokeBatchallTerms && (contract != null) && (enemyCode != null)) {
            StratConDeploymentService.processBatchallBreach(campaign, contract, enemyCode);
        }
        return committed;
    }

    /**
     * Ported batchall-breach warning. Loops until the player confirms a decision.
     *
     * @return {@code true} if the player backed out of reinforcing rather than breaching the batchall
     */
    private boolean backOutAtBatchallWarning() {
        final int continueOption = 1;
        Person speaker = campaign.getPlayerForce()
                               .getHumanResources()
                               .getSeniorAdminPerson(campaign.getCampaignOptions(),
                                     campaign.getPlayerForce().isClanForce(),
                                     campaign.getLocalDate());
        String inCharacter = String.format(getTextAt(RESOURCE_BUNDLE, "batchallBreach.ic"),
              campaign.getCommanderAddress());
        String outOfCharacter = getTextAt(RESOURCE_BUNDLE, "batchallBreach.ooc");
        String cancelButton = getTextAt(RESOURCE_BUNDLE, "batchallBreach.button.cancel");
        String continueButton = getTextAt(RESOURCE_BUNDLE, "batchallBreach.button.continue");

        boolean dialogAccepted = false;
        boolean backedOut = false;
        while (!dialogAccepted) {
            ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign, speaker, null, inCharacter,
                  List.of(cancelButton, continueButton), outOfCharacter, null, true);
            backedOut = dialog.getDialogChoice() != continueOption;

            if (MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_STRATCON_BATCHALL_BREACH)) {
                dialogAccepted = true;
            } else {
                dialogAccepted = new ImmersiveDialogConfirmation(campaign,
                      CONFIRMATION_STRATCON_BATCHALL_BREACH).wasConfirmed();
            }
        }
        return backedOut;
    }

    private boolean confirmDeployment() {
        // This is a point of no return, so confirm unless the player has silenced the nag.
        if (MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_STRATCON_DEPLOY)) {
            return true;
        }
        return new ImmersiveDialogConfirmation(campaign, CONFIRMATION_STRATCON_DEPLOY).wasConfirmed();
    }

    private Map<String, List<Formation>> groupStagedReinforcementsByTemplate() {
        Map<String, List<Formation>> forcesByTemplate = new LinkedHashMap<>();
        for (Formation formation : stagedReinforcementForces) {
            String templateId = reinforcementTemplateByForceId.get(formation.getId());
            if (templateId != null) {
                forcesByTemplate.computeIfAbsent(templateId, key -> new ArrayList<>()).add(formation);
            }
        }
        return forcesByTemplate;
    }

    private void finishCommit() {
        owner.repaint();
        dispose();
    }

    // endregion
}
