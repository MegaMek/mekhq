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
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.commanderLanceHasDefensiveAssignment;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.getEligibleFrontlineUnits;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.getEligibleLeadershipUnits;
import static mekhq.campaign.personnel.skills.SkillType.SKILL_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.personnel.skills.SkillType.S_STRATEGY;
import static mekhq.campaign.personnel.skills.SkillType.S_TACTICS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentEvaluator;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentMode;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementAdvisor;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementCost;
import mekhq.campaign.digitalGM.stratCon.deployment.ReinforcementRoll;
import mekhq.campaign.digitalGM.stratCon.deployment.StratConDeploymentService;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillModifierData;
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
    private static final MMLogger LOGGER = MMLogger.create(StratConDeploymentWizard.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private final transient StratConPanel owner;
    private final transient Campaign campaign;

    private transient StratConCampaignState campaignState;
    private transient StratConScenario scenario;

    private transient StratConTrackState currentTrack;
    private transient StratConCoords selectedCoords;

    private boolean assignToScenario;
    private boolean restrictToSingleForce;

    private DeploymentMode mode = DeploymentMode.PRIMARY;

    // The full eligible set for the current page (formations on Primary/Reinforce, units on Auxiliaries/Utility), and
    // the filtered view the board actually shows.
    private final transient List<Object> allItems = new ArrayList<>();
    private final DefaultListModel<Object> boardModel = new DefaultListModel<>();
    private final JList<Object> boardList = new JList<>(boardModel);
    private final JTextField searchField = new JTextField(20);
    private final JComboBox<Object> roleFilter = new JComboBox<>();
    private JPanel roleFilterPanel;
    private final JLabel instructionsLabel = new JLabel();

    // One shared staged set, kept typed by the page each item was staged from, so the single Commit can batch them.
    private final transient List<Formation> stagedPrimaryForces = new ArrayList<>();
    private final transient List<Formation> stagedReinforcementForces = new ArrayList<>();
    private final transient List<Unit> stagedAuxiliaryUnits = new ArrayList<>();
    private final transient List<Unit> stagedUtilityUnits = new ArrayList<>();

    // Forces already committed to the scenario when the wizard opened. They are shown (locked) in the staged tray so the
    // player can see the current deployment, but cannot be removed here - the scenario must be reset to change them.
    private final transient List<Formation> deployedPrimaryForces = new ArrayList<>();
    private final transient List<Formation> deployedReinforcementForces = new ArrayList<>();

    // Loose units (leadership/auxiliary, utility, or otherwise) already committed to the scenario as individual units
    // rather than whole formations. Shown (locked) in the staged tray; the scenario must be reset to change them. Their
    // original page (Auxiliaries vs Utility) is not recorded on the scenario, so they share one "deployed" grouping.
    private final transient List<Unit> deployedLooseUnits = new ArrayList<>();

    // On the Reinforce page, the reinforcement template slot each eligible force fills (needed to commit it).
    private final transient Map<Integer, String> reinforcementTemplateByForceId = new LinkedHashMap<>();

    // The player's explicit off-board choice per formation ID (artillery only). A formation absent from the map has not
    // been touched and falls back to the client-option default. Applied to the scenario on commit.
    private final transient Map<Integer, Boolean> offBoardChoiceByForceId = new HashMap<>();

    // The force or unit whose dossier is showing, and whether it is focused from the staged tray (so Stage acts as
    // Unstage) rather than from the board. A staged item never appears on the board, so these two focus sources never
    // point at the same item.
    private transient Object focusedItem;
    private boolean focusedFromStagedTray;

    // Budget inputs for the unit pages, gathered from the scenario when its page is loaded.
    private int leadershipSkill;
    private int leadershipPointsUsed;
    private int defensivePoints;

    private final HudModeSelector modeSelector = new HudModeSelector(this::switchMode);
    private transient ReinforcementAdvisor reinforcementAdvisor;

    private final transient DeploymentInspectorPanel inspector;

    public StratConDeploymentWizard(StratConPanel owner, Campaign campaign) {
        super((java.awt.Frame) null, false);
        this.owner = owner;
        this.campaign = campaign;
        this.inspector = new DeploymentInspectorPanel(campaign);

        setTitle(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.title"));
        setLayout(new BorderLayout());
        getContentPane().setBackground(HudStyle.GROUND);
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
        // Snapshot the deployment target now: the non-modal wizard must not follow later hex clicks on the map.
        this.currentTrack = owner.getCurrentTrack();
        this.selectedCoords = owner.getSelectedCoords();
        this.assignToScenario = assignToScenario;
        this.restrictToSingleForce = restrictToSingleForce;
        this.reinforcementAdvisor = (scenario == null)
                                          ? null
                                          : new ReinforcementAdvisor(campaign, campaignState, currentTrack);

        inspector.setEnvironment((scenario == null) ? null : scenario.getBackingScenario(),
              currentTrack, selectedCoords);

        clearStaged();
        loadExistingAssignments();
        mode = initialMode;
        modeSelector.setSelected(initialMode);
        enterMode();

        setPreferences(); // Must be before setVisible
        setVisible(true);
    }

    /**
     * Loads the forces already committed to the scenario into the locked "deployed" lists, so the staged tray reflects
     * the current deployment when the wizard opens. Primary forces come from the scenario's committed primary IDs; every
     * other assigned force is shown as an already-committed reinforcement. These are display-only and cannot be unstaged
     * here (the scenario must be reset to change them); only forces newly staged this session are committed.
     */
    private void loadExistingAssignments() {
        deployedPrimaryForces.clear();
        deployedReinforcementForces.clear();
        deployedLooseUnits.clear();
        // Only a confirmed deployment has locked forces. When assigning a fresh primary (an unresolved scenario), any
        // force IDs the scenario carries are auto-assigned suggestions from generation, not a committed deployment, so
        // they are left out - the player is choosing the primary now.
        if ((scenario == null) || assignToScenario) {
            return;
        }

        List<Integer> primaryForceIds = scenario.getPrimaryForceIDs();
        for (int forceId : primaryForceIds) {
            Formation formation = campaign.getPlayerForce().getFormation(forceId);
            if (formation != null) {
                deployedPrimaryForces.add(formation);
            }
        }

        for (int forceId : scenario.getAssignedForces()) {
            if (primaryForceIds.contains(forceId)) {
                continue;
            }
            Formation formation = campaign.getPlayerForce().getFormation(forceId);
            if (formation != null) {
                deployedReinforcementForces.add(formation);
            }
        }

        // Loose units deployed to the scenario as individuals (leadership/auxiliary, utility, or otherwise), not as part
        // of a formation.
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        if (backingScenario != null) {
            for (UUID unitId : backingScenario.getIndividualUnitIDs()) {
                Unit unit = campaign.getUnit(unitId);
                if (unit != null) {
                    deployedLooseUnits.add(unit);
                }
            }
        }
    }

    private void switchMode(DeploymentMode newMode) {
        if (mode == newMode) {
            return;
        }
        mode = newMode;
        modeSelector.setSelected(newMode);
        enterMode();
    }

    /**
     * Loads the current page's items and refreshes the board, inspector, and budget. Staged selections persist across a
     * mode switch (they are only cleared when the wizard is reopened), so the batch Commit can gather them all.
     */
    private void enterMode() {
        instructionsLabel.setText(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.instructions." + mode.name()));

        // The role filter only applies to formation pages; hide it (and drop any stale selection) on the unit pages.
        boolean showRoleFilter = !mode.isUnitTier();
        if (!showRoleFilter && (roleFilter.getSelectedIndex() != 0)) {
            roleFilter.setSelectedIndex(0);
        }
        roleFilterPanel.setVisible(showRoleFilter);

        loadEligibleItems();
        applySearchFilter();

        clearFocus();
        refreshStagedTray();
        updateBudget();
        refreshCommitEnabled();
        updateTabLocks();
    }

    private void clearStaged() {
        stagedPrimaryForces.clear();
        stagedReinforcementForces.clear();
        stagedAuxiliaryUnits.clear();
        stagedUtilityUnits.clear();
        deployedPrimaryForces.clear();
        deployedReinforcementForces.clear();
        deployedLooseUnits.clear();
        offBoardChoiceByForceId.clear();
    }

    private JPanel buildCommandBar() {
        JPanel commandBar = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(8)));
        commandBar.setOpaque(true);
        commandBar.setBackground(HudStyle.GROUND);
        int pad = UIUtil.scaleForGUI(12);
        commandBar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(1), 0, HudStyle.BORDER),
              BorderFactory.createEmptyBorder(pad, UIUtil.scaleForGUI(14), pad, UIUtil.scaleForGUI(14))));

        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.title").toUpperCase(Locale.ROOT));
        title.setForeground(HudStyle.ACCENT_BRIGHT);
        title.setFont(HudStyle.hudFont(Font.BOLD, 1.25f, 0.16f));

        instructionsLabel.setForeground(HudStyle.TEXT_MUTED);
        instructionsLabel.setFont(HudStyle.hudFont(Font.PLAIN, 0.9f, 0.0f));

        commandBar.add(title, BorderLayout.NORTH);
        commandBar.add(modeSelector, BorderLayout.CENTER);
        commandBar.add(instructionsLabel, BorderLayout.SOUTH);
        return commandBar;
    }

    private JSplitPane buildBody() {
        JPanel boardPanel = new JPanel(new BorderLayout(0, UIUtil.scaleForGUI(6)));
        boardPanel.setOpaque(true);
        boardPanel.setBackground(HudStyle.GROUND);
        int pad = UIUtil.scaleForGUI(12);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(pad, pad, pad, pad));

        JPanel searchRow = new JPanel(new BorderLayout(UIUtil.scaleForGUI(8), 0));
        searchRow.setOpaque(false);
        searchRow.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.search.label")
              .toUpperCase(Locale.ROOT)), BorderLayout.WEST);
        styleField(searchField);
        searchRow.add(searchField, BorderLayout.CENTER);
        searchRow.add(buildRoleFilter(), BorderLayout.EAST);

        boardList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        boardList.setCellRenderer(new DeploymentItemRenderer(campaign));
        boardList.setBackground(HudStyle.SURFACE_DEEP);
        boardList.setBorder(null);

        JScrollPane boardScroll = new JScrollPane(boardList);
        boardScroll.setBorder(BorderFactory.createLineBorder(HudStyle.BORDER, UIUtil.scaleForGUI(1)));
        boardScroll.getViewport().setBackground(HudStyle.SURFACE_DEEP);

        boardPanel.add(searchRow, BorderLayout.NORTH);
        boardPanel.add(boardScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, boardPanel, inspector);
        splitPane.setResizeWeight(0.58);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setBackground(HudStyle.GROUND);
        return splitPane;
    }

    /**
     * Builds the combat-role filter: an "All Roles" entry followed by every {@link CombatRole}, letting the player show
     * only formations of a chosen role (for example, only Patrols). Hidden on the unit-tier pages, which have no roles.
     */
    private JPanel buildRoleFilter() {
        roleFilter.addItem(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.filter.allRoles"));
        for (CombatRole role : CombatRole.values()) {
            // Auxiliary and Reserve are not deployable combat roles, so they are not offered as filters.
            if ((role == CombatRole.AUXILIARY) || (role == CombatRole.RESERVE)) {
                continue;
            }
            roleFilter.addItem(role);
        }
        roleFilter.setBackground(HudStyle.SURFACE_DEEP);
        roleFilter.setForeground(HudStyle.TEXT);
        roleFilter.addActionListener(event -> applySearchFilter());

        roleFilterPanel = new JPanel(new BorderLayout(UIUtil.scaleForGUI(8), 0));
        roleFilterPanel.setOpaque(false);
        roleFilterPanel.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, "deploymentWizard.filter.role.label")
              .toUpperCase(Locale.ROOT)), BorderLayout.WEST);
        roleFilterPanel.add(roleFilter, BorderLayout.CENTER);
        return roleFilterPanel;
    }

    private static void styleField(JTextField field) {
        field.setBackground(HudStyle.SURFACE_DEEP);
        field.setForeground(HudStyle.TEXT);
        field.setCaretColor(HudStyle.ACCENT);
        field.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(HudStyle.BORDER, UIUtil.scaleForGUI(1)),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(8),
                    UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(8))));
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
                focusFromBoard();
            }
        });
    }

    private void wireInspector() {
        inspector.getStageButton().addActionListener(event -> toggleStageSelected());
        inspector.getCancelButton().addActionListener(event -> dispose());
        inspector.getCommitButton().addActionListener(event -> commit());
        inspector.addStagedSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                focusFromStagedTray();
            }
        });
        inspector.getOffBoardCheckBox().addActionListener(event -> toggleOffBoardForFocused());
    }

    // region board loading

    private void loadEligibleItems() {
        allItems.clear();

        // Refresh the leadership/defensive budget inputs before loading, so the auxiliary eligibility query and both
        // unit-page budgets reflect the primary force staged this session rather than the scenario's generated force.
        refreshBudgetInputs();

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
              currentTrack,
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
                  currentTrack,
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

        // A staged formation deploys with all of its units, so those units must not be offered again here.
        List<Unit> eligibleUnits = getEligibleLeadershipUnits(campaign, scenario, leadershipSkill);
        allItems.addAll(StratConDeploymentService.excludeUnitsOfFormations(eligibleUnits, stagedFormations()));
    }

    private void loadEligibleUtilityUnits() {
        if (scenario == null) {
            return;
        }

        // A staged formation deploys with all of its units, so those units must not be offered again here.
        List<Unit> eligibleUnits = getEligibleFrontlineUnits(campaign, scenario);
        allItems.addAll(StratConDeploymentService.excludeUnitsOfFormations(eligibleUnits, stagedFormations()));
    }

    /**
     * Recomputes the leadership and defensive-point budget inputs from the primary force staged this session. The staged
     * primary is not assigned to the backing scenario until commit, so the scenario-derived budgets would otherwise stay
     * pinned to whatever force the scenario was generated around and never track the player's choice. When no primary is
     * staged - reinforcing an already-committed scenario - the scenario's committed values are used instead.
     */
    private void refreshBudgetInputs() {
        if (scenario == null) {
            leadershipSkill = 0;
            leadershipPointsUsed = 0;
            defensivePoints = 0;
            return;
        }

        leadershipPointsUsed = scenario.getLeadershipPointsUsed();

        Formation stagedPrimary = stagedPrimaryForces.isEmpty() ? null : stagedPrimaryForces.getFirst();
        if (stagedPrimary == null) {
            // Reinforcing a committed scenario: fall back to the scenario's assigned commander lance.
            leadershipSkill = resolveLeadershipSkill();
            defensivePoints = scenario.getNumDefensivePoints();
            return;
        }

        // The staged primary becomes the commander lance on commit. Leadership units and defensive minefields are only
        // available when that lance is on a frontline (defensive) assignment, and never during official challenges.
        CombatTeam combatTeam = campaign.getPlayerForce().getCombatTeamsAsMap(campaign).get(stagedPrimary.getId());
        boolean defensiveAssignment = (combatTeam != null) && combatTeam.getRole().isFrontline();
        if (!defensiveAssignment || scenario.getBackingScenario().getStratConScenarioType().isOfficialChallenge()) {
            leadershipSkill = 0;
            defensivePoints = 0;
            return;
        }

        leadershipSkill = commanderSkill(combatTeam, S_LEADER);
        defensivePoints = commanderSkill(combatTeam, S_TACTICS) * 2;
    }

    /** The staged primary's commander skill level, mirroring {@code AtBDynamicScenario.getLanceCommanderSkill}. */
    private int commanderSkill(CombatTeam combatTeam, String skillType) {
        combatTeam.refreshCommander(campaign);
        Person commander = combatTeam.getCommander(campaign);
        if ((commander == null) || !commander.hasSkill(skillType)) {
            return SKILL_NONE;
        }
        SkillModifierData skillModifierData = commander.getSkillModifierData(
              campaign.getCampaignOptions().get(CampaignOption.USE_AGE_EFFECTS),
              campaign.getPlayerForce().isClanForce(), campaign.getLocalDate());
        return commander.getSkill(skillType).getTotalSkillLevel(skillModifierData);
    }

    /**
     * @return the committed scenario commander's leadership skill: zero unless that lance is on a defensive assignment,
     *       and always zero for official challenges (leadership units would be cheating)
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
        Object roleSelection = roleFilter.getSelectedItem();
        CombatRole roleQuery = (roleSelection instanceof CombatRole role) ? role : null;
        Object focused = boardList.getSelectedValue();

        boardModel.clear();
        for (Object item : allItems) {
            // A force or unit that is already staged (as primary, reinforcement, auxiliary, or utility) is pulled from
            // the picker until it is unstaged, so the same force cannot be staged twice - for example as both the
            // primary force and a reinforcement.
            if (isStagedAnywhere(item)) {
                continue;
            }
            if (!matchesRole(item, roleQuery)) {
                continue;
            }
            if (matchesQuery(item, query)) {
                boardModel.addElement(item);
            }
        }

        // Keep the focused item selected if it survived the filter, so the dossier does not flicker away mid-search.
        if ((focused != null) && boardModel.contains(focused)) {
            boardList.setSelectedValue(focused, true);
        }
    }

    private static boolean matchesRole(Object item, @Nullable CombatRole roleQuery) {
        if (roleQuery == null) {
            return true;
        }
        // Only formations carry a combat role; the filter is hidden on the unit pages, so roleQuery is null there.
        return (item instanceof Formation formation) && (formation.getCombatRoleInMemory() == roleQuery);
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

    /** Focuses the board selection (a candidate to stage), clearing any staged-tray selection. */
    private void focusFromBoard() {
        Object focused = boardList.getSelectedValue();
        if (focused == null) {
            return;
        }
        inspector.clearStagedSelection();

        focusedItem = focused;
        focusedFromStagedTray = false;
        showDossier(focused, mode == DeploymentMode.REINFORCE);
        updateOffBoardOption(focused);
        inspector.setStageButtonEnabled(true);
        inspector.setStageButtonStaged(false);
    }

    /** Focuses a staged-tray selection (a candidate to unstage), clearing any board selection. Dividers are ignored. */
    private void focusFromStagedTray() {
        Object focused = inspector.getSelectedStagedItem();
        if (focused == null) {
            return;
        }
        if (focused instanceof DeploymentItemRenderer.SectionHeader) {
            inspector.clearStagedSelection();
            return;
        }
        boardList.clearSelection();

        focusedItem = focused;
        focusedFromStagedTray = true;
        // Show the dossier for whatever the item was staged as, regardless of which page is open. Only a newly-staged
        // reinforcement gets the prospective roll/odds/arrival preview; an already-committed force shows its plain
        // formation dossier, since its reinforcement roll is already spent.
        boolean asReinforcement = (focused instanceof Formation formation)
                                        && stagedReinforcementForces.contains(formation);
        showDossier(focused, asReinforcement);
        updateOffBoardOption(focused);
        // Forces and units already committed to the scenario are locked: they can only be changed by resetting the
        // deployment, so the unstage control is disabled for them.
        inspector.setStageButtonEnabled(isUnlocked(focused));
        inspector.setStageButtonStaged(true);
    }

    private void showDossier(Object item, boolean asReinforcement) {
        if (item instanceof Unit unit) {
            inspector.showUnit(unit);
        } else if (item instanceof Formation formation) {
            if (asReinforcement && (reinforcementAdvisor != null)) {
                ReinforcementEligibilityType eligibility = reinforcementAdvisor.getEligibility(formation.getId());
                // Preview the roll at no support-point spent; the actual spend is chosen in the commit dialog.
                boolean isManeuver = formation.getCombatRoleInMemory().isManeuver();
                ReinforcementRoll roll = reinforcementAdvisor.getRoll(0, false, isManeuver);
                int perForceCost = DeploymentEvaluator.reinforcementCost(0, false, 1).perForceSupportPoints();
                inspector.showReinforcementFormation(formation, eligibility, roll, perForceCost,
                      estimateReinforcementArrival(formation));
            } else {
                inspector.showFormation(formation);
            }
        }
    }

    /**
     * @return the estimated turn this reinforcement force would arrive on, using the same slowest-speed-over-arrival-
     *       scale formula the scenario applies at finalization, reduced by the scenario commander's Strategy skill and
     *       the scenario's reinforcement delay reduction
     */
    private int estimateReinforcementArrival(Formation formation) {
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        int turnModifier = backingScenario.getLanceCommanderSkill(S_STRATEGY, campaign)
                                 + backingScenario.getFriendlyReinforcementDelayReduction();

        List<Entity> entities = new ArrayList<>();
        for (UUID unitId : formation.getAllUnits(true)) {
            Unit unit = campaign.getUnit(unitId);
            if ((unit != null) && (unit.getEntity() != null)) {
                entities.add(unit.getEntity());
            }
        }

        return AtBDynamicScenarioFactory.estimateReinforcementArrivalTurn(entities, turnModifier);
    }

    /**
     * Shows the "deploy off-board" checkbox whenever the focused item is an artillery-bearing formation on a formation
     * page (Primary or Reinforce) of a real scenario, and reflects its current state. The checkbox is always offered
     * for such a force; the client option only sets its default state (see {@link #effectiveOffBoard(int)}).
     */
    private void updateOffBoardOption(Object item) {
        boolean applicable = offBoardApplicable(item);
        inspector.setOffBoardOptionVisible(applicable);
        inspector.setOffBoardOptionSelected(applicable && effectiveOffBoard(((Formation) item).getId()));
    }

    private boolean offBoardApplicable(Object item) {
        return (scenario != null)
                     && (item instanceof Formation formation)
                     && ((mode == DeploymentMode.PRIMARY) || (mode == DeploymentMode.REINFORCE))
                     && isUnlocked(item)
                     && formationHasArtillery(formation);
    }

    private void toggleOffBoardForFocused() {
        if (!(focusedItem instanceof Formation formation)) {
            return;
        }
        offBoardChoiceByForceId.put(formation.getId(), inspector.isOffBoardOptionSelected());
        // Refresh the staged-tray indicator in case the toggled force is already staged.
        inspector.setStagedOffBoardForceIds(stagedOffBoardForceIds());
    }

    /**
     * @return whether the given force should deploy off-board: the player's explicit choice if they ticked the box, or
     *       the client-option default otherwise
     */
    private boolean effectiveOffBoard(int forceId) {
        return offBoardChoiceByForceId.getOrDefault(forceId,
              MekHQ.getMHQOptions().getDefaultPlayerForcesOffBoard());
    }

    private boolean formationHasArtillery(Formation formation) {
        for (UUID unitId : formation.getAllUnits(true)) {
            Unit unit = campaign.getUnit(unitId);
            if ((unit != null) && (unit.getEntity() != null)
                      && AtBDynamicScenarioFactory.entityHasArtillery(unit.getEntity())) {
                return true;
            }
        }
        return false;
    }

    private void toggleStageSelected() {
        if (focusedItem == null) {
            return;
        }

        Integer primaryBefore = stagedPrimaryId();

        if (focusedFromStagedTray) {
            unstage(focusedItem);
        } else {
            // Exactly one force may be the primary; any others must be deployed as reinforcements. Staging a new
            // primary force therefore replaces whatever was staged before.
            if (mode == DeploymentMode.PRIMARY) {
                stagedPrimaryForces.clear();
            }

            boolean staged = stage(focusedItem);
            if (!staged) {
                return;
            }
        }

        // If the primary force was assigned, swapped, or removed, the auxiliaries and utility choices no longer apply to
        // it, so reset them.
        if (!Objects.equals(primaryBefore, stagedPrimaryId())) {
            stagedAuxiliaryUnits.clear();
            stagedUtilityUnits.clear();
        }

        // A formation staged after some of its units were picked as auxiliary or utility units takes them with it.
        dropStagedUnitsCoveredByStagedFormations();

        // The item just moved between the board and the staged tray, so drop the focus and rebuild both views.
        clearFocus();
        applySearchFilter();
        refreshStagedTray();
        updateBudget();
        refreshCommitEnabled();
        updateTabLocks();
    }

    /** Clears the focused item and both selections, and resets the inspector to its "nothing focused" state. */
    private void clearFocus() {
        focusedItem = null;
        focusedFromStagedTray = false;
        boardList.clearSelection();
        inspector.clearStagedSelection();
        inspector.showEmpty();
        inspector.setOffBoardOptionVisible(false);
        inspector.setStageButtonEnabled(false);
    }

    /** @return every formation staged this session, primary and reinforcement alike */
    private List<Formation> stagedFormations() {
        List<Formation> formations = new ArrayList<>(stagedPrimaryForces);
        formations.addAll(stagedReinforcementForces);
        return formations;
    }

    /**
     * Removes any staged auxiliary or utility unit that sits inside a staged formation. The formation deploys with all
     * of its units, so keeping the unit staged on its own would commit it twice.
     */
    private void dropStagedUnitsCoveredByStagedFormations() {
        List<Formation> stagedFormations = stagedFormations();
        List<Unit> remainingAuxiliaryUnits = StratConDeploymentService.excludeUnitsOfFormations(stagedAuxiliaryUnits,
              stagedFormations);
        List<Unit> remainingUtilityUnits = StratConDeploymentService.excludeUnitsOfFormations(stagedUtilityUnits,
              stagedFormations);
        if (remainingAuxiliaryUnits.size() != stagedAuxiliaryUnits.size()) {
            LOGGER.debug("[Deployment] {} staged auxiliary unit(s) dropped: their formation is staged too",
                  stagedAuxiliaryUnits.size() - remainingAuxiliaryUnits.size());
            stagedAuxiliaryUnits.clear();
            stagedAuxiliaryUnits.addAll(remainingAuxiliaryUnits);
        }
        if (remainingUtilityUnits.size() != stagedUtilityUnits.size()) {
            LOGGER.debug("[Deployment] {} staged utility unit(s) dropped: their formation is staged too",
                  stagedUtilityUnits.size() - remainingUtilityUnits.size());
            stagedUtilityUnits.clear();
            stagedUtilityUnits.addAll(remainingUtilityUnits);
        }
    }

    private boolean isStagedAnywhere(Object item) {
        if (item instanceof Formation formation) {
            return stagedPrimaryForces.contains(formation) || stagedReinforcementForces.contains(formation)
                         || deployedPrimaryForces.contains(formation)
                         || deployedReinforcementForces.contains(formation);
        }
        if (item instanceof Unit unit) {
            return stagedAuxiliaryUnits.contains(unit) || stagedUtilityUnits.contains(unit)
                         || deployedLooseUnits.contains(unit);
        }
        return false;
    }

    private boolean stage(Object item) {
        if (!canStage(item)) {
            return false;
        }

        if (item instanceof Formation formation) {
            (mode == DeploymentMode.PRIMARY ? stagedPrimaryForces : stagedReinforcementForces).add(formation);
        } else if (item instanceof Unit unit) {
            (mode == DeploymentMode.AUXILIARIES ? stagedAuxiliaryUnits : stagedUtilityUnits).add(unit);
        }

        return true;
    }
    private boolean canStage(Object item) {
        if (item instanceof Unit unit) {
            if (mode == DeploymentMode.AUXILIARIES) {
                return canStageAuxiliary(unit);
            }
            if (mode == DeploymentMode.UTILITY) {
                return canStageUtility();
            }
        }

        return true;
    }

    private boolean canStageAuxiliary(Unit unit) {
        if (unit.getEntity() == null) {
            return true;
        }

        int remaining = leadershipBattleValueRemaining();
        int battleValue = unit.getEntity().calculateBattleValue(true, true);
        return (battleValue >= 0) && (battleValue <= remaining);
    }

    private boolean canStageUtility() {
        return minefieldsRemaining() > 0;
    }

    private boolean stagedWithinBudget() {
        return (leadershipBattleValueRemaining() >= 0) && (minefieldsRemaining() >= 0);
    }

    private void unstage(Object item) {
        if (item instanceof Formation formation) {
            stagedPrimaryForces.remove(formation);
            stagedReinforcementForces.remove(formation);
            offBoardChoiceByForceId.remove(formation.getId());
        } else if (item instanceof Unit unit) {
            stagedAuxiliaryUnits.remove(unit);
            stagedUtilityUnits.remove(unit);
        }
    }

    private void refreshStagedTray() {
        // Group the staged items under captioned dividers so a staged primary force reads apart from staged
        // reinforcements (and from auxiliary/utility units). Forces already committed to the scenario lead their section,
        // ahead of anything newly staged this session.
        List<Object> staged = new ArrayList<>();
        addStagedSection(staged, DeploymentMode.PRIMARY.getLabel(), merged(deployedPrimaryForces, stagedPrimaryForces));
        addStagedSection(staged, DeploymentMode.REINFORCE.getLabel(),
              merged(deployedReinforcementForces, stagedReinforcementForces));
        addStagedSection(staged, getTextAt(RESOURCE_BUNDLE, "deploymentWizard.deployedUnits.title"), deployedLooseUnits);
        addStagedSection(staged, DeploymentMode.AUXILIARIES.getLabel(), stagedAuxiliaryUnits);
        addStagedSection(staged, DeploymentMode.UTILITY.getLabel(), stagedUtilityUnits);
        inspector.setStaged(staged);
        inspector.setStagedOffBoardForceIds(stagedOffBoardForceIds());
        inspector.setStagedLockedForceIds(lockedForceIds());
        inspector.setStagedLockedUnitIds(lockedUnitIds());
    }

    /** Concatenates the already-deployed forces (first) with the newly-staged ones for a tray section. */
    private static List<Formation> merged(List<Formation> deployed, List<Formation> staged) {
        List<Formation> combined = new ArrayList<>(deployed);
        combined.addAll(staged);
        return combined;
    }

    /** @return the IDs of forces already committed to the scenario (locked, non-removable in this session) */
    private Set<Integer> lockedForceIds() {
        Set<Integer> ids = new HashSet<>();
        for (Formation formation : deployedPrimaryForces) {
            ids.add(formation.getId());
        }
        for (Formation formation : deployedReinforcementForces) {
            ids.add(formation.getId());
        }
        return ids;
    }

    /** @return the IDs of loose units already committed to the scenario (locked, non-removable in this session) */
    private Set<UUID> lockedUnitIds() {
        Set<UUID> ids = new HashSet<>();
        for (Unit unit : deployedLooseUnits) {
            ids.add(unit.getId());
        }
        return ids;
    }

    /**
     * @return whether the given item (a formation or a loose unit) can still be staged or unstaged here, i.e. it is
     *       not already committed to the scenario - committed items are locked and require a scenario reset to change
     */
    private boolean isUnlocked(Object item) {
        if (item instanceof Formation formation) {
            return !(deployedPrimaryForces.contains(formation) || deployedReinforcementForces.contains(formation));
        }
        if (item instanceof Unit unit) {
            return !deployedLooseUnits.contains(unit);
        }
        return false;
    }

    /**
     * @return the IDs of staged formations that will actually deploy off-board (those with artillery whose effective
     *       off-board choice is on), for the staged-tray indicator
     */
    private Set<Integer> stagedOffBoardForceIds() {
        Set<Integer> ids = new HashSet<>();
        collectOffBoardForceIds(ids, stagedPrimaryForces);
        collectOffBoardForceIds(ids, stagedReinforcementForces);
        // Already-deployed forces show their committed off-board state, read straight from the scenario.
        AtBDynamicScenario backingScenario = (scenario == null) ? null : scenario.getBackingScenario();
        if (backingScenario != null) {
            collectCommittedOffBoardForceIds(ids, backingScenario, deployedPrimaryForces);
            collectCommittedOffBoardForceIds(ids, backingScenario, deployedReinforcementForces);
        }
        return ids;
    }

    private void collectCommittedOffBoardForceIds(Set<Integer> ids, AtBDynamicScenario backingScenario,
          List<Formation> deployedFormations) {
        for (Formation formation : deployedFormations) {
            if (backingScenario.isForceDeployingOffBoard(formation.getId())) {
                ids.add(formation.getId());
            }
        }
    }

    private void collectOffBoardForceIds(Set<Integer> ids, List<Formation> stagedFormations) {
        for (Formation formation : stagedFormations) {
            if (formationHasArtillery(formation) && effectiveOffBoard(formation.getId())) {
                ids.add(formation.getId());
            }
        }
    }

    private static void addStagedSection(List<Object> staged, String label, List<?> items) {
        if (items.isEmpty()) {
            return;
        }
        staged.add(new DeploymentItemRenderer.SectionHeader(label));
        staged.addAll(items);
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
        // A deployment is meaningless without a primary force, so commit stays disabled until one is present - either
        // staged this session, or already committed on the scenario being reinforced.

        // The unit-tier budgets are also enforced here as a second line of defense. This keeps Commit disabled if staged
        // selections somehow become over budget through a future code path that bypasses stage().
        inspector.getCommitButton().setArmed(hasPrimary() && !nothingStaged() && stagedWithinBudget());
    }

    /**
     * Reinforce, Auxiliaries, and Utility only make sense once a primary force is present, so their tabs are locked
     * until a primary is assigned - either already committed on the scenario, or staged in this session.
     */
    private void updateTabLocks() {
        boolean hasPrimary = hasPrimary();
        modeSelector.setModeEnabled(DeploymentMode.REINFORCE, hasPrimary);
        modeSelector.setModeEnabled(DeploymentMode.AUXILIARIES, hasPrimary);
        modeSelector.setModeEnabled(DeploymentMode.UTILITY, hasPrimary);
    }

    private boolean hasPrimary() {
         // When reinforcing an already-committed scenario, its committed primary counts. When assigning a fresh primary
        // (the Primary page), an unresolved scenario may already carry an auto-assigned primary from generation, so
        // ignore that and key the lock purely off what the player has staged in the Primary tab this session.
        boolean committedPrimary = !assignToScenario && (scenario != null) && !scenario.getPrimaryForceIDs().isEmpty();
        return committedPrimary || !stagedPrimaryForces.isEmpty();
    }

    private Integer stagedPrimaryId() {
        return stagedPrimaryForces.isEmpty() ? null : stagedPrimaryForces.getFirst().getId();
    }

    // endregion

    // region commit

    /**
     * Batches every staged page into one commit: primary forces are deployed first (which sizes the OpFor and finalizes
     * the scenario against the real primary), then auxiliary/utility units are added, then any reinforcement forces are
     * rolled, then the scenario is finalized once and a single staying-home summary is shown.
     *
     * <p>The order matters: {@code assignForceToScenario} rebuilds the scenario's primary force list from every templated
     * force (see {@link mekhq.campaign.digitalGM.stratCon.StratConScenario#commitPrimaryForces()}), so a reinforcement
     * committed before the primary would be swept into the primary list and skew OpFor generation. Deploying the primary
     * first keeps the reinforcements out of that list; they only land in the delayed/instant arrival lists.</p>
     */
    private void commit() {
        // The primary/auxiliary/utility pages share the deploy nag; reinforcements carry their own confirmation dialog.
        boolean hasNonReinforcement = !stagedPrimaryForces.isEmpty() ||
                                            !stagedAuxiliaryUnits.isEmpty() ||
                                            !stagedUtilityUnits.isEmpty();
        if (hasNonReinforcement && !confirmDeployment()) {
            return;
        }

        if (!stagedPrimaryForces.isEmpty()) {
            deployPrimaryForces();
        }
        // Guard: a unit that also sits inside a staged formation deploys with that formation, and adding it again as a
        // loose unit would put it in the scenario twice and charge its battle value to the leadership budget.
        List<Formation> stagedFormations = stagedFormations();
        List<Unit> auxiliaryUnits = StratConDeploymentService.excludeUnitsOfFormations(stagedAuxiliaryUnits,
              stagedFormations);
        List<Unit> utilityUnits = StratConDeploymentService.excludeUnitsOfFormations(stagedUtilityUnits,
              stagedFormations);
        if (!auxiliaryUnits.isEmpty()) {
            StratConDeploymentService.addAuxiliaryUnits(scenario, auxiliaryUnits);
        }
        if (!utilityUnits.isEmpty()) {
            StratConDeploymentService.addUtilityUnits(scenario, utilityUnits);
            StratConDeploymentService.setMinefieldCount(scenario,
                  DeploymentEvaluator.minefieldsRemaining(defensivePoints, utilityUnits.size()));
        }

        // Roll reinforcements only after the primary is committed.
        List<Formation> committedReinforcements = new ArrayList<>();
        if (!stagedReinforcementForces.isEmpty()) {
            processReinforcementBatch(committedReinforcements);
        }

        boolean managedScenario = !committedReinforcements.isEmpty() ||
                                        !auxiliaryUnits.isEmpty() ||
                                        !utilityUnits.isEmpty();
        if (managedScenario && (scenario != null)) {
            StratConDeploymentService.finalizeForceDeployment(campaign, currentTrack, scenario);
        }

        applyOffBoardSelections();

        if (!committedReinforcements.isEmpty()) {
            SupportCarrierDeploymentDialogs.showStayingHome(campaign,
                  committedReinforcements,
                  scenario.getBackingScenario());
        }

        finishCommit();
    }

    /**
     * Records the player's off-board choices on the scenario for every staged formation, so the units deploy off-board
     * when the scenario is played (or auto-resolved). Only artillery units of a marked force are actually placed
     * off-board; that check happens at play time.
     */
    private void applyOffBoardSelections() {
        if ((scenario == null) || (scenario.getBackingScenario() == null)) {
            return;
        }
        AtBDynamicScenario backingScenario = scenario.getBackingScenario();
        applyOffBoardSelections(backingScenario, stagedPrimaryForces);
        applyOffBoardSelections(backingScenario, stagedReinforcementForces);
    }

    private void applyOffBoardSelections(AtBDynamicScenario backingScenario, List<Formation> stagedFormations) {
        for (Formation formation : stagedFormations) {
            // A force is only ever marked off-board when it actually has artillery, so the default-on option never
            // pushes an infantry or mek force off the map.
            boolean offBoard = formationHasArtillery(formation) && effectiveOffBoard(formation.getId());
            backingScenario.setForceDeployingOffBoard(formation.getId(), offBoard);
        }
    }

    private void deployPrimaryForces() {
        List<Integer> stagedForceIds = new ArrayList<>();
        for (Formation formation : stagedPrimaryForces) {
            stagedForceIds.add(formation.getId());
        }

        StratConDeploymentService.deployPrimaryForces(campaign,
              campaignState,
              currentTrack,
              selectedCoords,
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

        // The advisor was built in display() with these same inputs (command liaison, contract, base target number), and
        // scenario is non-null past the guard above, so it is non-null here. Reuse its target roll rather than recompute.
        TargetRoll targetNumber = reinforcementAdvisor.getBaseTargetRoll();
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
              currentTrack,
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

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void setPreferences() {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(getClass());
            this.setName(getClass().getSimpleName());
            preferences.manage(new JWindowPreference(this));
        } catch (Exception exception) {
            LOGGER.error("Failed to set user preferences", exception);
        }
    }
}
