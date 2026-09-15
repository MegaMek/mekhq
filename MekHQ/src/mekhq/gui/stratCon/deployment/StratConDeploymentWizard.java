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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentContext;
import mekhq.campaign.digitalGM.stratCon.deployment.DeploymentMode;
import mekhq.campaign.digitalGM.stratCon.gm.StratConGMs;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.gui.StratConPanel;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.stratCon.ScenarioWizardLanceModel;
import mekhq.gui.stratCon.ScenarioWizardLanceRenderer;

/**
 * The redesigned StratCon deployment wizard: a non-modal, four-page window that browses the player's forces on a
 * searchable board and shows a dossier for the focused force before it is committed.
 *
 * <p>This is the first vertical slice of the redesign - the {@link DeploymentMode#PRIMARY} page, fully wired to the
 * existing deployment pipeline ({@link StratConGMs}). The Reinforce, Auxiliaries, and Utility pages follow the same
 * board-plus-inspector shape and are stubbed here until later phases. The legacy {@code StratConScenarioWizard} and
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

    // The full eligible set for the current page, and the filtered view the board actually shows.
    private final transient List<Formation> allFormations = new ArrayList<>();
    private final DefaultListModel<Formation> boardModel = new DefaultListModel<>();
    private final JList<Formation> boardList = new JList<>(boardModel);
    private final JTextField searchField = new JTextField(20);

    // Staged forces are kept here as the display source of truth and mirrored into the context by id.
    private final transient List<Formation> stagedFormations = new ArrayList<>();

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

        context.setMode(DeploymentMode.PRIMARY);
        stagedFormations.clear();
        for (Integer stagedId : new ArrayList<>(context.getStagedFormationIds())) {
            context.unstageFormation(stagedId);
        }

        loadEligibleFormations();
        applySearchFilter();

        inspector.showEmpty();
        inspector.setStageButtonEnabled(false);
        inspector.setStaged(stagedFormations);
        refreshCommitEnabled();

        setVisible(true);
    }

    private JPanel buildCommandBar() {
        JPanel commandBar = new JPanel(new BorderLayout());

        JPanel modeStrip = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup modeGroup = new ButtonGroup();
        for (DeploymentMode mode : DeploymentMode.values()) {
            JToggleButton modeButton = new JToggleButton(mode.getLabel());
            modeGroup.add(modeButton);
            if (mode == DeploymentMode.PRIMARY) {
                modeButton.setSelected(true);
            } else {
                // The other pages arrive in later phases; keep them visible so the shape is clear, but inert.
                modeButton.setEnabled(false);
                modeButton.setToolTipText(getFormattedTextAt(RESOURCE_BUNDLE,
                      "deploymentWizard.page.placeholder",
                      mode.getLabel()));
            }
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
        boardList.setCellRenderer(new ScenarioWizardLanceRenderer(campaign));

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
                focusSelectedFormation();
            }
        });
    }

    private void wireInspector() {
        inspector.getStageButton().addActionListener(event -> toggleStageSelected());
        inspector.getCancelButton().addActionListener(event -> dispose());
        inspector.getCommitButton().addActionListener(event -> commit());
    }

    private void loadEligibleFormations() {
        allFormations.clear();

        // Matches TrackForceAssignmentUI's primary-force query: the whole eligible mix from the current track.
        List<Integer> eligibleIds = StratConRulesManager.getAvailableForceIDsForManualDeployment(
              ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX,
              campaign,
              owner.getCurrentTrack(),
              false,
              null,
              campaignState,
              restrictToSingleForce);

        ScenarioWizardLanceModel lanceModel = new ScenarioWizardLanceModel(campaign, eligibleIds);
        for (int index = 0; index < lanceModel.getSize(); index++) {
            allFormations.add(lanceModel.getElementAt(index));
        }
    }

    private void applySearchFilter() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        Formation focused = boardList.getSelectedValue();

        boardModel.clear();
        for (Formation formation : allFormations) {
            if (matchesQuery(formation, query)) {
                boardModel.addElement(formation);
            }
        }

        // Keep the focused force selected if it survived the filter, so the dossier does not flicker away mid-search.
        if ((focused != null) && boardModel.contains(focused)) {
            boardList.setSelectedValue(focused, true);
        }
    }

    private static boolean matchesQuery(Formation formation, String query) {
        if (query.isEmpty()) {
            return true;
        }
        return formation.getName().toLowerCase(Locale.ROOT).contains(query) ||
                     formation.getFullName().toLowerCase(Locale.ROOT).contains(query);
    }

    private void focusSelectedFormation() {
        Formation focused = boardList.getSelectedValue();
        if (focused == null) {
            inspector.showEmpty();
            inspector.setStageButtonEnabled(false);
            return;
        }

        inspector.showFormation(focused);
        inspector.setStageButtonEnabled(true);
        inspector.setStageButtonStaged(stagedFormations.contains(focused));
    }

    private void toggleStageSelected() {
        Formation focused = boardList.getSelectedValue();
        if (focused == null) {
            return;
        }

        if (stagedFormations.contains(focused)) {
            stagedFormations.remove(focused);
            context.unstageFormation(focused.getId());
        } else {
            // Official challenges permit a single force: staging a new one replaces whatever was staged before.
            if (restrictToSingleForce) {
                for (Formation staged : stagedFormations) {
                    context.unstageFormation(staged.getId());
                }
                stagedFormations.clear();
            }
            stagedFormations.add(focused);
            context.stageFormation(focused.getId());
        }

        inspector.setStaged(stagedFormations);
        inspector.setStageButtonStaged(stagedFormations.contains(focused));
        refreshCommitEnabled();
    }

    private void refreshCommitEnabled() {
        inspector.getCommitButton().setEnabled(!stagedFormations.isEmpty());
    }

    private void commit() {
        // This is a point of no return, so confirm unless the player has silenced the nag.
        if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_STRATCON_DEPLOY)) {
            ImmersiveDialogConfirmation confirmation = new ImmersiveDialogConfirmation(campaign,
                  CONFIRMATION_STRATCON_DEPLOY);
            if (!confirmation.wasConfirmed()) {
                return;
            }
        }

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        StratConCoords coords = owner.getSelectedCoords();
        StratConTrackState track = owner.getCurrentTrack();

        for (Formation formation : stagedFormations) {
            if (assignToScenario) {
                StratConGMs.forceDeployment(campaignOptions).assignForceToScenario(coords,
                      formation.getId(),
                      campaign,
                      campaignState.getContract(),
                      track,
                      false);
            } else {
                StratConGMs.forceDeployment(campaignOptions).deployForceToCoords(coords,
                      formation.getId(),
                      campaign,
                      campaignState.getContract(),
                      track,
                      false);
            }
        }

        owner.repaint();
        dispose();
    }
}
