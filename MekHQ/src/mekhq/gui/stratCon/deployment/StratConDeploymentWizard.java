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

import static mekhq.MHQConstants.CONFIRMATION_STRATCON_DEPLOY;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentContext;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentEvaluator;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentMode;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementAdvisor;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementRoll;
import mekhq.campaign.digitalGM.stratCon.deployment.StratConDeploymentService;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.unit.Unit;
import mekhq.gui.StratConPanel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.stratCon.ScenarioWizardLanceModel;

/**
 * The redesigned StratCon deployment wizard: a non-modal, four-page window that browses the player's forces on a
 * searchable board and shows a dossier for the focused force before it is committed.
 *
 * <p>The {@link DeploymentMode#PRIMARY} page is fully wired, delegating its commit to the non-GUI
 * {@link StratConDeploymentService}. Reinforce, Auxiliaries, and Utility are browsable - their boards and inspectors
 * show the real decision data (reinforcement eligibility/roll/odds/cost; leadership battle-value budget; the
 * minefield tradeoff) - but their commit is wired in a later step. The legacy {@code StratConScenarioWizard} and
 * {@code TrackForceAssignmentUI} remain the live path until this replacement is validated in-game.</p>
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

    private final transient DeploymentContext context = new DeploymentContext(DeploymentMode.PRIMARY);

    // The full eligible set for the current page (formations on Primary/Reinforce, units on Auxiliaries/Utility), and
    // the filtered view the board actually shows.
    private final transient List<Object> allItems = new ArrayList<>();
    private final DefaultListModel<Object> boardModel = new DefaultListModel<>();
    private final JList<Object> boardList = new JList<>(boardModel);
    private final JTextField searchField = new JTextField(20);

    // Staged items are kept here as the display source of truth and mirrored into the context.
    private final transient List<Object> stagedItems = new ArrayList<>();

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
     * @param restrictToSingleForce {@code true} when only one force may be staged (official challenges)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void display(StratConCampaignState campaignState, @Nullable StratConScenario scenario,
          boolean assignToScenario, boolean restrictToSingleForce) {
        this.campaignState = campaignState;
        this.scenario = scenario;
        this.assignToScenario = assignToScenario;
        this.restrictToSingleForce = restrictToSingleForce;
        this.reinforcementAdvisor = (scenario == null)
                                          ? null
                                          : new ReinforcementAdvisor(campaign, campaignState, owner.getCurrentTrack());

        context.setMode(DeploymentMode.PRIMARY);
        modeButtons.get(DeploymentMode.PRIMARY).setSelected(true);
        enterMode();

        setVisible(true);
    }

    private void switchMode(DeploymentMode mode) {
        if (context.getMode() == mode) {
            return;
        }
        context.setMode(mode);
        enterMode();
    }

    /**
     * Loads the current page's items and resets the inspector, staged tray, and budget. Shared by the initial display
     * and every mode switch.
     */
    private void enterMode() {
        clearStaged();
        loadEligibleItems();
        applySearchFilter();

        inspector.showEmpty();
        inspector.setStageButtonEnabled(false);
        inspector.setStaged(stagedItems);
        updateBudget();
        refreshCommitEnabled();
    }

    private void clearStaged() {
        stagedItems.clear();
        for (Integer stagedId : new ArrayList<>(context.getStagedFormationIds())) {
            context.unstageFormation(stagedId);
        }
        for (Unit stagedUnit : new ArrayList<>(context.getStagedUnits())) {
            context.unstageUnit(stagedUnit);
        }
    }

    private JPanel buildCommandBar() {
        JPanel commandBar = new JPanel(new BorderLayout());

        JPanel modeStrip = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup modeGroup = new ButtonGroup();
        for (DeploymentMode mode : DeploymentMode.values()) {
            JToggleButton modeButton = new JToggleButton(mode.getLabel());
            modeGroup.add(modeButton);
            modeButtons.put(mode, modeButton);
            modeButton.addActionListener(event -> switchMode(mode));
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

    private void loadEligibleItems() {
        allItems.clear();

        switch (context.getMode()) {
            case PRIMARY, REINFORCE -> loadEligibleFormations();
            case AUXILIARIES -> loadEligibleAuxiliaryUnits();
            case UTILITY -> loadEligibleUtilityUnits();
        }
    }

    private void loadEligibleFormations() {
        boolean reinforcements = context.getMode() == DeploymentMode.REINFORCE;
        // Primary mirrors TrackForceAssignmentUI's query (whole eligible mix, no scenario); Reinforce asks for the
        // reinforcement-eligible mix for this scenario.
        StratConScenario eligibilityScenario = reinforcements ? scenario : null;
        boolean singleForceOnly = !reinforcements && restrictToSingleForce;

        List<Integer> eligibleIds = StratConRulesManager.getAvailableForceIDsForManualDeployment(
              ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX,
              campaign,
              owner.getCurrentTrack(),
              reinforcements,
              eligibilityScenario,
              campaignState,
              singleForceOnly);

        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign, eligibleIds);
        for (int index = 0; index < lanceModel.getSize(); index++) {
            allItems.add(lanceModel.getElementAt(index));
        }
    }

    private void loadEligibleAuxiliaryUnits() {
        if (scenario == null) {
            return;
        }

        int leadershipSkill = resolveLeadershipSkill();
        context.setLeadershipSkill(leadershipSkill);
        context.setLeadershipPointsUsed(scenario.getLeadershipPointsUsed());
        allItems.addAll(getEligibleLeadershipUnits(campaign, scenario, leadershipSkill));
    }

    private void loadEligibleUtilityUnits() {
        if (scenario == null) {
            return;
        }

        context.setDefensivePoints(scenario.getNumDefensivePoints());
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
            if ((context.getMode() == DeploymentMode.REINFORCE) && (reinforcementAdvisor != null)) {
                ReinforcementEligibilityType eligibility = reinforcementAdvisor.getEligibility(formation.getId());
                ReinforcementRoll roll = reinforcementAdvisor.getRoll(context.getChosenSupportPoints(),
                      context.isInstantArrival());
                int perForceCost = DeploymentEvaluator.reinforcementCost(context.getChosenSupportPoints(),
                      context.isInstantArrival(),
                      1).perForceSupportPoints();
                inspector.showReinforcementFormation(formation, eligibility, roll, perForceCost);
            } else {
                inspector.showFormation(formation);
            }
        }

        inspector.setStageButtonEnabled(true);
        inspector.setStageButtonStaged(stagedItems.contains(focused));
    }

    private void toggleStageSelected() {
        Object focused = boardList.getSelectedValue();
        if (focused == null) {
            return;
        }

        if (stagedItems.contains(focused)) {
            unstageItem(focused);
        } else {
            // Official challenges permit a single force: staging a new one replaces whatever was staged before.
            if (restrictToSingleForce && (context.getMode() == DeploymentMode.PRIMARY)) {
                for (Object staged : new ArrayList<>(stagedItems)) {
                    unstageItem(staged);
                }
            }
            stageItem(focused);
        }

        inspector.setStaged(stagedItems);
        inspector.setStageButtonStaged(stagedItems.contains(focused));
        updateBudget();
        refreshCommitEnabled();
    }

    private void stageItem(Object item) {
        stagedItems.add(item);
        if (item instanceof Formation formation) {
            context.stageFormation(formation.getId());
        } else if (item instanceof Unit unit) {
            context.stageUnit(unit);
        }
    }

    private void unstageItem(Object item) {
        stagedItems.remove(item);
        if (item instanceof Formation formation) {
            context.unstageFormation(formation.getId());
        } else if (item instanceof Unit unit) {
            context.unstageUnit(unit);
        }
    }

    /**
     * Updates the inspector's budget line for the current page: leadership battle value remaining (Auxiliaries),
     * minefields remaining (Utility), or nothing (Primary/Reinforce, until reinforcement commit is wired).
     */
    private void updateBudget() {
        switch (context.getMode()) {
            case AUXILIARIES -> inspector.setBudget(getFormattedTextAt(RESOURCE_BUNDLE,
                  "deploymentWizard.budget.leadership",
                  context.getLeadershipBattleValueRemaining()));
            case UTILITY -> inspector.setBudget(getFormattedTextAt(RESOURCE_BUNDLE,
                  "deploymentWizard.budget.minefields",
                  context.getMinefieldsRemaining()));
            default -> inspector.clearBudget();
        }
    }

    private void refreshCommitEnabled() {
        // Only Primary has a wired commit for now; the other pages are browse-only until their commit is wired.
        boolean primary = context.getMode() == DeploymentMode.PRIMARY;
        boolean reinforce = context.getMode() == DeploymentMode.REINFORCE;
        inspector.getCommitButton().setEnabled(primary && !stagedItems.isEmpty());
        inspector.getCommitButton().setToolTipText(primary ? null : getTextAt(RESOURCE_BUNDLE,
              reinforce ? "deploymentWizard.reinforce.commitPending" : "deploymentWizard.unitCommitPending"));
    }

    private void commit() {
        // Guard: only the Primary page commits in this slice.
        if (context.getMode() != DeploymentMode.PRIMARY) {
            return;
        }

        // This is a point of no return, so confirm unless the player has silenced the nag.
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_STRATCON_DEPLOY)) {
            ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign,
                  CONFIRMATION_STRATCON_DEPLOY);
            if (!confirmation.wasConfirmed()) {
                return;
            }
        }

        List<Integer> stagedForceIds = new ArrayList<>();
        for (Object staged : stagedItems) {
            if (staged instanceof Formation formation) {
                stagedForceIds.add(formation.getId());
            }
        }

        StratConDeploymentService.deployPrimaryForces(campaign,
              campaignState,
              owner.getCurrentTrack(),
              owner.getSelectedCoords(),
              assignToScenario,
              stagedForceIds);

        owner.repaint();
        dispose();
    }
}
