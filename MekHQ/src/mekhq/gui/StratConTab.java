/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;

import jakarta.annotation.Nonnull;
import megamek.client.ui.util.UIUtil;
import megamek.common.event.Subscribe;
import megamek.common.ui.FastJScrollPane;
import mekhq.MHQOptionsChangedEvent;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.events.GMModeEvent;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.StratConDeploymentEvent;
import mekhq.campaign.events.missions.MissionChangedEvent;
import mekhq.campaign.events.missions.MissionCompletedEvent;
import mekhq.campaign.events.missions.MissionRemovedEvent;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.utilities.ContractScore;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.view.ContractMeterBar;
import mekhq.utilities.ReportingUtilities;

/**
 * This class contains code relevant to rendering the StratCon ("AtB Campaign State") tab.
 *
 * @author NickAragua
 */
public class StratConTab extends CampaignGuiTab {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private static final String OBJECTIVE_FAILED = "x";
    private static final String OBJECTIVE_COMPLETED = "&#10003;";
    private static final String OBJECTIVE_IN_PROGRESS = "o";

    private StratConPanel stratconPanel;
    private JScrollPane mapScrollPane;
    private JComboBox<ContractItem> contractSelector;
    private JTabbedPane sectorTabs;
    private List<StratConTrackState> currentSectorTracks;
    private JPanel infoPanel;
    private JLabel infoPanelText;
    private RoundedJButton btnScoutSector;
    private RoundedJButton btnResetSectorFog;
    private RoundedJButton btnRegenerateSector;
    private RoundedJButton btnResizeSector;
    private RoundedJButton btnChangeTerrain;
    private RoundedJButton btnEditSupportPoints;
    private RoundedJButton btnEditVictoryPoints;
    private RoundedJButton btnToggleHiddenObjects;
    private JLabel objectiveStatusText;
    private JPanel threatLevelPanel;
    private JPanel deploymentTimePanel;
    private JPanel supportPointsPanel;
    private JPanel victoryPointsPanel;
    private JScrollPane expandedObjectivePanel;
    private boolean objectivesCollapsed = false;

    private AbstractContract currentContract;
    private StratConTrackState currentSectorTrack;

    private boolean adjustingSelectors = false;

    //region Constructors

    /**
     * Creates an instance of the StratConTab.
     */
    public StratConTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
        setEnabled(!gui.getCampaign().getCampaignOptions().isUseStratConMaplessMode());
    }
    //endregion Constructors

    public StratConPanel getStratconPanel() {
        return stratconPanel;
    }

    /**
     * Override of the base initTab method. Populates the tab.
     */
    @Override
    public void initTab() {
        removeAll();

        currentSectorTracks = new ArrayList<>();

        infoPanelText = new JLabel();
        infoPanelText.setHorizontalAlignment(SwingConstants.LEFT);
        infoPanelText.setVerticalAlignment(SwingConstants.TOP);

        objectiveStatusText = new JLabel();
        objectiveStatusText.setHorizontalAlignment(SwingConstants.LEFT);
        objectiveStatusText.setVerticalAlignment(SwingConstants.TOP);
        objectiveStatusText.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent me) {
                if ((currentContract == null) || (currentSectorTrack == null)) {
                    return;
                }
                objectivesCollapsed = !objectivesCollapsed;
                applyObjectiveText(getStrategicObjectiveText(currentContract.getStratConCampaignState(),
                      currentSectorTrack));
            }
        });

        setLayout(new BorderLayout());

        stratconPanel = new StratConPanel(getCampaignGui(), infoPanelText);
        mapScrollPane = new JScrollPane(stratconPanel);
        mapScrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        mapScrollPane.getHorizontalScrollBar().setUnitIncrement(StratConPanel.HEX_X_RADIUS);
        mapScrollPane.getVerticalScrollBar().setUnitIncrement(StratConPanel.HEX_Y_RADIUS);
        // Repaint the whole map on scroll rather than blit-copying old pixels; the default BLIT mode tears when panning
        // the large, scaled hex map.
        mapScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        contractSelector = new JComboBox<>();
        contractSelector.addActionListener(evt -> contractSelectionHandler());

        JPanel contractPanel = new JPanel(new BorderLayout(UIUtil.scaleForGUI(5), 0));
        contractPanel.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "stratConTab.contractSelector.label")),
              BorderLayout.WEST);
        contractPanel.add(contractSelector, BorderLayout.CENTER);

        sectorTabs = new JTabbedPane();
        sectorTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        sectorTabs.addChangeListener(evt -> sectorSelectionHandler());

        // TODO: lance role assignment UI here?

        JPanel pnlTutorial = new TutorialHyperlinkPanel("stratConTab.keyText");

        // The GM tools sit under the sector pane rather than across the whole tab, so they line up with the map they
        // act on instead of running beneath the info panel as well. The tutorial link stays below them, at the very
        // bottom of the column.
        JPanel centerFooter = new JPanel(new BorderLayout());
        centerFooter.add(initializeGmButtonPanel(), BorderLayout.NORTH);
        centerFooter.add(pnlTutorial, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(contractPanel, BorderLayout.NORTH);
        centerPanel.add(sectorTabs, BorderLayout.CENTER);
        centerPanel.add(centerFooter, BorderLayout.SOUTH);

        this.add(centerPanel, BorderLayout.CENTER);

        initializeInfoPanel();

        JScrollPane infoScrollPane = new FastJScrollPane(infoPanel);
        infoScrollPane.setBorder(null);
        infoScrollPane.setMaximumSize(new Dimension(UIUtil.scaleForGUI(UIUtil.scaleForGUI(600),
              infoScrollPane.getHeight())));
        this.add(infoScrollPane, BorderLayout.EAST);

        repopulateContractsAndSectors();
    }

    /**
     * Worker function that sets up the layout of the right-side info panel.
     */
    private void initializeInfoPanel() {
        int gridY = 0;
        infoPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // Default settings for left-aligned components
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.gridx = 0;

        // The old contract status-text block was removed: the contract name is already in the picker, and the figures
        // (support points, deployment period) now show as bars below. Support-point / victory-point editing lives on the
        // "Edit SP (GM)" / "Edit CVP (GM)" bottom-bar buttons; the sector environment and selected-hex stats are HUDs
        // drawn on the map (see StratConPanel).

        // Bars stack at the top: threat, deployment time, support points, victory points.
        threatLevelPanel = addBarPanel(constraints, gridY++);
        deploymentTimePanel = addBarPanel(constraints, gridY++);
        supportPointsPanel = addBarPanel(constraints, gridY++);
        victoryPointsPanel = addBarPanel(constraints, gridY++);

        // Add the objectives panel below the bars. Its height tracks the objective content (see applyObjectiveText) so
        // it is only as tall as it needs to be; the scroll pane remains as a safety net for unusually long lists.
        expandedObjectivePanel = new FastJScrollPane(objectiveStatusText);
        expandedObjectivePanel.setBorder(RoundedLineBorder.createRoundedLineBorder());
        constraints.gridy = gridY++;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        infoPanel.add(expandedObjectivePanel, constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;

        // The selected-hex stats (temperature, terrain, recon status, scenario details) are drawn as a HUD in the
        // bottom-right of the map itself; see StratConPanel.drawSelectedHexInfo(). infoPanelText is the label it paints.

        // Add a spacer to push all components upward (top alignment)
        constraints.gridx = 0;
        constraints.gridy = gridY;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.VERTICAL;
        infoPanel.add(new JPanel(), constraints); // Invisible filler component
    }

    /**
     * Creates a horizontally-filling, transparent host panel for a {@link ContractMeterBar}, adds it to the info panel
     * at the given grid row, and returns it. Restores the shared constraints afterward.
     */
    private JPanel addBarPanel(GridBagConstraints constraints, int gridY) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        constraints.gridy = gridY;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        infoPanel.add(panel, constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        return panel;
    }

    /**
     * Worker function that builds the GM sector-tool button bar shown beneath the sector pane. The buttons are enabled
     * only for a GM (kept in sync by {@link #updateCampaignState()}).
     *
     * <p><b>Call this exactly once.</b> Each call builds a fresh set of buttons and reassigns the fields that track
     * them, so a second call renders a second bar and leaves the first one's buttons orphaned - still visible, but no
     * longer reachable by the GM-mode enabling.</p>
     *
     * @return the button bar
     */
    private JPanel initializeGmButtonPanel() {
        boolean isGM = getCampaignGui().getCampaign().isGM();

        // "Scout Sector" - reveals every hex in the selected sector (GM only)
        btnScoutSector = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.scoutSector.text"));
        btnScoutSector.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.scoutSector.tooltip"));
        btnScoutSector.setEnabled(isGM);
        btnScoutSector.addActionListener(evt -> stratconPanel.scoutCurrentSector());

        // "Reset Sector Fog" - un-reveals every hex in the selected sector (GM only)
        btnResetSectorFog = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.resetSectorFog.text"));
        btnResetSectorFog.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.resetSectorFog.tooltip"));
        btnResetSectorFog.setEnabled(isGM);
        btnResetSectorFog.addActionListener(evt -> stratconPanel.resetSectorFog());

        // "Regenerate Sector" - clears and re-rolls the selected sector's terrain (GM only)
        btnRegenerateSector = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.regenerateSector.text"));
        btnRegenerateSector.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.regenerateSector.tooltip"));
        btnRegenerateSector.setEnabled(isGM);
        btnRegenerateSector.addActionListener(evt -> {
            stratconPanel.regenerateCurrentSector();
            // Regeneration re-rolls the sector's environment (latitude, profiles); refresh the info panel to match.
            updateCampaignState();
        });

        // "Change Terrain" - opens the terrain palette and puts the map into paint mode (GM only). It lives here
        // rather than on the hex right-click menu because the palette paints wherever you drag, not just one hex.
        btnChangeTerrain = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.changeTerrain.text"));
        btnChangeTerrain.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.changeTerrain.tooltip"));
        btnChangeTerrain.setEnabled(isGM);
        btnChangeTerrain.addActionListener(evt -> stratconPanel.openTerrainPaintDialog());

        // "Resize Sector" - grows or shrinks the sector at its right and bottom edges (GM only)
        btnResizeSector = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.resizeSector.text"));
        btnResizeSector.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.resizeSector.tooltip"));
        btnResizeSector.setEnabled(isGM);
        btnResizeSector.addActionListener(evt -> stratconPanel.resizeSector());

        // "Toggle Hidden Objects" - reveals/hides cloaked scenarios, invisible facilities, and fog (GM only)
        btnToggleHiddenObjects = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.toggleHiddenObjects.text"));
        btnToggleHiddenObjects.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.toggleHiddenObjects.tooltip"));
        btnToggleHiddenObjects.setEnabled(isGM);
        btnToggleHiddenObjects.addActionListener(evt -> stratconPanel.toggleHiddenObjects());

        // "Edit SP" - set the contract's support-point total (GM only)
        btnEditSupportPoints = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.editSupportPoints.text"));
        btnEditSupportPoints.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.editSupportPoints.tooltip"));
        btnEditSupportPoints.setEnabled(isGM);
        btnEditSupportPoints.addActionListener(this::editSupportPoints);

        // "Edit CVP" - set the contract's Campaign Victory Point total (GM only)
        btnEditVictoryPoints = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "stratConTab.editVictoryPoints.text"));
        btnEditVictoryPoints.setToolTipText(getTextAt(RESOURCE_BUNDLE, "stratConTab.editVictoryPoints.tooltip"));
        btnEditVictoryPoints.setEnabled(isGM);
        btnEditVictoryPoints.addActionListener(this::editVictoryPoints);

        int gap = UIUtil.scaleForGUI(5);
        JPanel gmButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, gap, gap));
        // The panel is titled rather than tagging every button "(GM)": one label says it once for the whole group.
        gmButtonPanel.setBorder(RoundedLineBorder.createRoundedLineBorder(getTextAt(RESOURCE_BUNDLE,
              "stratConTab.gmTools.title")));
        gmButtonPanel.add(btnScoutSector);
        gmButtonPanel.add(btnResetSectorFog);
        gmButtonPanel.add(btnRegenerateSector);
        gmButtonPanel.add(btnChangeTerrain);
        gmButtonPanel.add(btnResizeSector);
        gmButtonPanel.add(btnToggleHiddenObjects);
        gmButtonPanel.add(btnEditSupportPoints);
        gmButtonPanel.add(btnEditVictoryPoints);
        return gmButtonPanel;
    }

    /**
     * GM action: prompts for and sets the current contract's support-point total.
     */
    private void editSupportPoints(ActionEvent e) {
        if (currentContract == null) {
            return;
        }
        StratConCampaignState campaignState = currentContract.getStratConCampaignState();
        if (campaignState == null) {
            return;
        }

        Integer value = promptForInt("stratConTab.editSupportPoints.prompt", campaignState.getSupportPoints());
        if (value != null) {
            campaignState.setSupportPoints(value);
            updateCampaignState();
        }
    }

    /**
     * GM action: prompts for and sets the current contract's Campaign Victory Point total.
     */
    private void editVictoryPoints(ActionEvent e) {
        if (currentContract == null) {
            return;
        }
        StratConCampaignState campaignState = currentContract.getStratConCampaignState();
        if (campaignState == null) {
            return;
        }

        Integer value = promptForInt("stratConTab.editVictoryPoints.prompt", campaignState.getVictoryPoints());
        if (value != null) {
            campaignState.setVictoryPoints(value);
            updateCampaignState();
        }
    }

    /**
     * Shows a simple integer input dialog seeded with the current value.
     *
     * @param promptKey    the resource key for the prompt message
     * @param currentValue the value to seed the input with
     *
     * @return the entered integer, or {@code null} if cancelled or not a valid integer
     */
    /**
     * Asks the GM for a whole number, seeded with the current value.
     *
     * @return the number entered, or {@code null} if the GM cancelled. Unparseable input is reported rather than
     *       silently discarded - typing a stray letter used to close the dialog and change nothing, with no way to tell
     *       that from a deliberate cancel.
     */
    private Integer promptForInt(String promptKey, int currentValue) {
        String input = JOptionPane.showInputDialog(this, getTextAt(RESOURCE_BUNDLE, promptKey),
              String.valueOf(currentValue));
        if (input == null) {
            return null;
        }
        try {
            return Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.promptForInt.notANumber", input),
                  getTextAt(RESOURCE_BUNDLE, "stratConTab.promptForInt.notANumber.title"),
                  JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    /**
     * Handles selection of a contract from the dropdown: rebuilds the sector tabs for that contract's tracks.
     */
    private void contractSelectionHandler() {
        if (adjustingSelectors) {
            return;
        }

        ContractItem selected = (ContractItem) contractSelector.getSelectedItem();
        if (selected == null) {
            return;
        }

        currentContract = selected.contract;
        repopulateSectorTabs(currentContract);
    }

    /**
     * Handles selection of a sector tab: moves the shared AO map into the active tab and points the map and the
     * objective display at that sector.
     */
    private void sectorSelectionHandler() {
        if (adjustingSelectors) {
            return;
        }

        int index = sectorTabs.getSelectedIndex();
        if ((index < 0) || (index >= currentSectorTracks.size()) || (currentContract == null)) {
            return;
        }

        currentSectorTrack = currentSectorTracks.get(index);

        Component tabContent = sectorTabs.getComponentAt(index);
        if (tabContent instanceof JPanel holder) {
            holder.add(mapScrollPane, BorderLayout.CENTER);
            holder.revalidate();
            holder.repaint();
        }

        stratconPanel.selectTrack(currentContract.getStratConCampaignState(), currentSectorTrack);
        stratconPanel.setVisible(true);
        updateCampaignState();
    }

    @Override
    public void repaint() {
        updateCampaignState();
        super.repaint();
    }

    @Override
    public void refreshAll() {
        stratconPanel.repaint();
        updateCampaignState();
    }

    @Override
    public MHQTabType tabType() {
        return MHQTabType.STRAT_CON;
    }

    /**
     * Worker function that updates the campaign state section of the info panel with such info as current objective
     * status, VP/SP totals, etc.
     */
    public void updateCampaignState() {
        if ((contractSelector == null) || (victoryPointsPanel == null)) {
            return;
        }

        // The GM button bar tools are GM-only conveniences.
        boolean isGM = getCampaignGui().getCampaign().isGM();
        if (btnScoutSector != null) {
            btnScoutSector.setEnabled(isGM);
        }
        if (btnResetSectorFog != null) {
            btnResetSectorFog.setEnabled(isGM);
        }
        if (btnRegenerateSector != null) {
            btnRegenerateSector.setEnabled(isGM);
        }
        if (btnResizeSector != null) {
            btnResizeSector.setEnabled(isGM);
        }
        if (btnChangeTerrain != null) {
            btnChangeTerrain.setEnabled(isGM);
        }
        if (btnToggleHiddenObjects != null) {
            btnToggleHiddenObjects.setEnabled(isGM);
        }
        if (btnEditSupportPoints != null) {
            btnEditSupportPoints.setEnabled(isGM);
        }
        if (btnEditVictoryPoints != null) {
            btnEditVictoryPoints.setEnabled(isGM);
        }

        // No active/started contract selected: nothing to chart.
        if (currentContract == null) {
            hideInfoBars();
            applyObjectiveText("");
            return;
        }

        LocalDate currentDate = getCampaignGui().getCampaign().getLocalDate();
        LocalDate startDate = currentContract.getStartDate();
        if (startDate != null && startDate.isAfter(currentDate)) {
            hideInfoBars();
            applyObjectiveText("");
            return;
        }

        StratConCampaignState campaignState = currentContract.getStratConCampaignState();
        expandedObjectivePanel.setVisible(true);
        updateThreatLevelBar(currentSectorTrack);
        updateDeploymentTimeBar(currentSectorTrack);
        updateSupportPointsBar();
        updateVictoryPointsBar(campaignState);

        if (currentSectorTrack != null) {
            applyObjectiveText(getStrategicObjectiveText(campaignState, currentSectorTrack));
        } else {
            applyObjectiveText("");
        }

        // keep the sector tab colors in sync as objectives are completed/failed over the course of the contract
        applySectorTabColors();
    }

    /**
     * Hides every info-panel bar and the objectives list, for states with no active/started contract.
     */
    private void hideInfoBars() {
        expandedObjectivePanel.setVisible(false);
        threatLevelPanel.setVisible(false);
        deploymentTimePanel.setVisible(false);
        supportPointsPanel.setVisible(false);
        victoryPointsPanel.setVisible(false);
    }

    /**
     * Refreshes the victory-point progress bar shown above the objectives list. Mirrors the Briefing Room's contract
     * gauge: a {@link ContractMeterBar} charting current-versus-required Campaign Victory Points when a positive target
     * exists, falling back to a plain current/required label when it does not.
     *
     * @param campaignState the StratCon state of the currently selected contract
     */
    private void updateVictoryPointsBar(StratConCampaignState campaignState) {
        victoryPointsPanel.removeAll();
        victoryPointsPanel.setVisible(true);

        boolean maplessMode = getCampaignGui().getCampaign().getCampaignOptions().isUseStratConMaplessMode();
        int currentScore = ContractScore.getContractScore(maplessMode, currentContract);
        int requiredScore = currentContract.getRequiredVictoryPoints();

        if (requiredScore > 0) {
            victoryPointsPanel.add(ContractMeterBar.victoryPoints(currentScore, requiredScore, campaignState),
                  BorderLayout.CENTER);
        } else {
            // No positive target to chart (e.g., a contract with a zero requirement); show the figures as text instead,
            // matching the Briefing Room's fallback.
            victoryPointsPanel.add(new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "stratConTab.status.victoryPointsBarFallback", currentScore, requiredScore)), BorderLayout.CENTER);
        }

        victoryPointsPanel.revalidate();
        victoryPointsPanel.repaint();
    }

    /**
     * Refreshes the threat-level bar shown above the victory-point bar. Charts the selected sector's effective scenario
     * odds from 0% (calm) to 100% (hostile), running good-to-bad. Hidden when no sector is selected.
     *
     * <p>This is the number a deploying force actually rolls against, so it is taken from
     * {@link StratConRulesManager#calculateScenarioOdds} rather than read off the track: the track carries the base
     * odds and the facility adjustment, but the enemy's morale shifts the real figure by anywhere from -10 to +50, and
     * a routed enemy mounts no attacks at all. Showing the track's own numbers understated a dominant enemy badly and
     * promised a threat that a routed one could not deliver.</p>
     *
     * @param track the currently selected sector's track, or {@code null}
     */
    private void updateThreatLevelBar(StratConTrackState track) {
        threatLevelPanel.removeAll();

        if ((track == null) || (currentContract == null)) {
            threatLevelPanel.setVisible(false);
            return;
        }

        threatLevelPanel.setVisible(true);
        // Matches deployForceToCoords, which rolls against the reinforcement figure: this bar answers "if I deploy
        // here, how likely is a fight?"
        int odds = Math.clamp(StratConRulesManager.calculateScenarioOdds(track, currentContract, true), 0, 100);
        threatLevelPanel.add(ContractMeterBar.threatLevel(odds), BorderLayout.CENTER);

        threatLevelPanel.revalidate();
        threatLevelPanel.repaint();
    }

    /**
     * Refreshes the deployment-time bar for the selected sector: how long a deployment lasts, 0 to 10, where a longer
     * deployment is worse. Hidden when no sector is selected.
     *
     * @param track the currently selected sector's track, or {@code null}
     */
    private void updateDeploymentTimeBar(StratConTrackState track) {
        deploymentTimePanel.removeAll();

        if (track == null) {
            deploymentTimePanel.setVisible(false);
            return;
        }

        deploymentTimePanel.setVisible(true);
        deploymentTimePanel.add(ContractMeterBar.deploymentTime(track.getDeploymentTime()), BorderLayout.CENTER);

        deploymentTimePanel.revalidate();
        deploymentTimePanel.repaint();
    }

    /**
     * Refreshes the support-points bar: the contract's current support points against the reserve it can negotiate up
     * to. Hidden when there is no positive reserve.
     */
    private void updateSupportPointsBar() {
        supportPointsPanel.removeAll();

        int current = currentContract.getCurrentSupportPoints();
        int maximum = currentContract.getMaximumSupportPoints();
        if (maximum <= 0) {
            supportPointsPanel.setVisible(false);
            return;
        }

        supportPointsPanel.setVisible(true);
        supportPointsPanel.add(ContractMeterBar.supportPoints(current, maximum), BorderLayout.CENTER);

        supportPointsPanel.revalidate();
        supportPointsPanel.repaint();
    }

    /**
     * Sets the objective text and resizes the objectives panel so it is only as tall as the content needs, rather than
     * a fixed height. The scroll pane remains as a safety net for unusually long objective lists.
     *
     * @param text the objective HTML to display
     */
    private void applyObjectiveText(String text) {
        objectiveStatusText.setText(text);
        int width = UIUtil.scaleForGUI(550);
        int height = objectiveStatusText.getPreferredSize().height + UIUtil.scaleForGUI(12);
        expandedObjectivePanel.setPreferredSize(new Dimension(width, height));
        expandedObjectivePanel.revalidate();
    }

    /**
     * Builds strategic objective text, appropriately appending details if the objectives are not "collapsed".
     */
    private String getStrategicObjectiveText(StratConCampaignState campaignState, StratConTrackState track) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html>")
              .append(buildShortStrategicObjectiveText(track));

        if (objectivesCollapsed) {
            sb.append(" [+] ");
        } else {
            sb.append(" [-]<br/>")
                  .append(buildStrategicObjectiveText(campaignState, track));
        }

        sb.append("</html>");

        return sb.toString();
    }

    /**
     * Builds strategic objective one-liner summary for a single sector (track).
     */
    private String buildShortStrategicObjectiveText(StratConTrackState track) {
        int completedObjectives = 0, desiredObjectives = 0;

        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            desiredObjectives++;

            if (objective.isObjectiveCompleted(track)) {
                completedObjectives++;
            }
        }

        String summary = getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.summary",
              String.valueOf(completedObjectives), String.valueOf(desiredObjectives));

        // Colour the summary to match this sector's tab and detailed objective list: green when every objective is
        // complete, red as soon as any objective has failed, amber while objectives remain outstanding, and no emphasis
        // colour when the sector has no objectives at all (a 0/0 sector is not "complete").
        String color = switch (sectorObjectiveState(track)) {
            case NONE -> null;
            case ALL_COMPLETE -> ReportingUtilities.getPositiveColor();
            case ANY_FAILED -> ReportingUtilities.getNegativeColor();
            case IN_PROGRESS -> ReportingUtilities.getWarningColor();
        };

        if (color == null) {
            return summary;
        }
        return "<span color='" + color + "'>" + summary + "</span>";
    }

    /**
     * Fetches a localized objective phrase, capitalizing its first letter when the objective's location is revealed (it
     * starts the sentence) or lower-casing it when the phrase follows the "Locate and" prefix.
     *
     * @param key         the resource key of the phrase, stored capitalized
     * @param capitalized whether the phrase should keep its leading capital letter
     *
     * @return the phrase with its first letter cased appropriately
     */
    private String objectivePhrase(String key, boolean capitalized) {
        String phrase = getTextAt(RESOURCE_BUNDLE, key);
        if (capitalized || phrase.isEmpty()) {
            return phrase;
        }
        return Character.toLowerCase(phrase.charAt(0)) + phrase.substring(1);
    }

    /**
     * Builds the detailed strategic objective list for a single sector (track). Only objectives belonging to that
     * sector are shown; the contract-wide Turning Point / Victory Point reminder is intentionally not included here.
     */
    private String buildStrategicObjectiveText(StratConCampaignState campaignState, StratConTrackState track) {
        StringBuilder sb = new StringBuilder();

        // for each objective in this sector, grab the coordinates
        // if !revealed, "locate and"
        // if specific scenario "engage hostile forces"
        // if hostile facility "capture or destroy [facility name]"
        // if allied facility "maintain control of [facility name]"
        // if revealed, " on track [current track] at coordinates [coords]
        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            boolean coordsRevealed = track.getRevealedCoords().contains(objective.getObjectiveCoords());
            boolean displayCoordinateData = objective.getObjectiveCoords() != null;
            boolean objectiveCompleted = objective.isObjectiveCompleted(track);
            boolean objectiveFailed = objective.isObjectiveFailed(track);

            // special case: allied facilities can get lost at any point in time
            if ((objective.getObjectiveType() == StrategicObjectiveType.AlliedFacilityControl) &&
                      !campaignState.allowEarlyVictory()) {
                sb.append("<span color='")
                      .append(ReportingUtilities.getWarningColor())
                      .append("'>")
                      .append(OBJECTIVE_IN_PROGRESS);
            } else if (objectiveCompleted) {
                sb.append("<span color='")
                      .append(ReportingUtilities.getPositiveColor())
                      .append("'>")
                      .append(OBJECTIVE_COMPLETED);
            } else if (objectiveFailed) {
                sb.append("<span color='")
                      .append(ReportingUtilities.getNegativeColor())
                      .append("'>")
                      .append(OBJECTIVE_FAILED);
            } else {
                sb.append("<span color='")
                      .append(ReportingUtilities.getWarningColor())
                      .append("'>")
                      .append(OBJECTIVE_IN_PROGRESS);
            }

            sb.append(' ');

            if (!coordsRevealed && displayCoordinateData) {
                sb.append(getTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.locatePrefix")).append(' ');
            }

            switch (objective.getObjectiveType()) {
                case SpecificScenarioVictory:
                    sb.append(objectivePhrase("stratConTab.objectives.specificScenario", coordsRevealed));
                    break;
                case HostileFacilityControl:
                    sb.append(objectivePhrase("stratConTab.objectives.hostileFacility", coordsRevealed));
                    break;
                case AlliedFacilityControl:
                    sb.append(objectivePhrase("stratConTab.objectives.alliedFacility", coordsRevealed));

                    if (!campaignState.allowEarlyVictory()) {
                        sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.until",
                              campaignState.getContract().getEndingDate()));
                    }
                    break;
                case AnyScenarioVictory:
                    sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.anyScenario",
                          String.valueOf(objective.getCurrentObjectiveCount()),
                          String.valueOf(objective.getDesiredObjectiveCount()),
                          track.getDisplayableName()));
                    break;
                default:
                    break;
            }
            if (coordsRevealed && displayCoordinateData) {
                sb.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.objectives.location",
                      objective.getObjectiveCoords().toBTString(), track.getDisplayableName()));
            }

            sb.append("</span><br/>");
        }

        return sb.toString();
    }

    /**
     * Refreshes the contract dropdown (one entry per active contract in the current system) and the sector tabs for the
     * selected contract, preserving the current selection where possible.
     */
    private void repopulateContractsAndSectors() {
        ContractItem previouslySelected = (ContractItem) contractSelector.getSelectedItem();

        adjustingSelectors = true;
        contractSelector.removeAllItems();

        Campaign campaign = getCampaignGui().getCampaign();
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        for (AbstractContract contract : campaign.getActiveContracts(false)) {
            // The contract's location is a system, not a planet within one - comparing against getTargetPlanet()
            // can never match, since PlanetarySystem.equals requires the same class.
            if (!Objects.equals(currentSystem, contract.getTargetSystem())) {
                continue;
            }
            if (contract.getStratConCampaignState() == null) {
                continue;
            }
            contractSelector.addItem(new ContractItem(contract));
        }

        // restore the previously selected contract if it is still present
        if (previouslySelected != null) {
            for (int i = 0; i < contractSelector.getItemCount(); i++) {
                if (contractSelector.getItemAt(i).equals(previouslySelected)) {
                    contractSelector.setSelectedIndex(i);
                    break;
                }
            }
        }
        adjustingSelectors = false;

        ContractItem selected = (ContractItem) contractSelector.getSelectedItem();
        if (selected != null) {
            currentContract = selected.contract;
            repopulateSectorTabs(currentContract);
        } else {
            currentContract = null;
            currentSectorTrack = null;
            currentSectorTracks.clear();

            adjustingSelectors = true;
            sectorTabs.removeAll();
            adjustingSelectors = false;

            infoPanelText.setText("");
            stratconPanel.setVisible(false);
            updateCampaignState();
        }
    }

    /**
     * Rebuilds the sector tabs for the given contract, one tab per track. Preserves the currently displayed sector when
     * it still exists, otherwise selects the first sector.
     */
    private void repopulateSectorTabs(AbstractContract contract) {
        StratConTrackState previousTrack = currentSectorTrack;

        adjustingSelectors = true;
        sectorTabs.removeAll();
        currentSectorTracks.clear();

        int selectIndex = 0;
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState != null) {
            int index = 0;
            for (StratConTrackState track : campaignState.getTracks()) {
                currentSectorTracks.add(track);
                // each tab holds an empty panel; the shared map is dropped into the active one on selection
                sectorTabs.addTab(track.getDisplayableName(), new JPanel(new BorderLayout()));

                if (track.equals(previousTrack)) {
                    selectIndex = index;
                }
                index++;
            }

            if (!currentSectorTracks.isEmpty()) {
                sectorTabs.setSelectedIndex(selectIndex);
            }
        }
        adjustingSelectors = false;

        if (!currentSectorTracks.isEmpty()) {
            sectorSelectionHandler();
        } else {
            currentSectorTrack = null;
            stratconPanel.setVisible(false);
            updateCampaignState();
        }

        applySectorTabColors();
    }

    /**
     * Colors each sector tab's title by that sector's objective status: positive when every objective is complete,
     * negative when an objective has failed, and warning while objectives are still in progress. Sectors with no
     * objectives keep the default tab color.
     */
    private void applySectorTabColors() {
        if (sectorTabs.getTabCount() != currentSectorTracks.size()) {
            return;
        }

        for (int i = 0; i < currentSectorTracks.size(); i++) {
            // a null foreground resets the tab to the tabbed pane's default color
            sectorTabs.setForegroundAt(i, sectorTabColor(currentSectorTracks.get(i)));
        }
    }

    /**
     * The aggregate objective status of a single sector, shared by the sector tab colour and the objective summary
     * one-liner so the two can never disagree.
     */
    private enum SectorObjectiveState {NONE, ALL_COMPLETE, ANY_FAILED, IN_PROGRESS}

    /**
     * @return the aggregate objective state for the given sector: {@link SectorObjectiveState#NONE} when it has no
     *       objectives, {@code ANY_FAILED} as soon as <em>any</em> objective has failed, {@code ALL_COMPLETE} when
     *       every objective is complete, and {@code IN_PROGRESS} whenever objectives remain outstanding.
     */
    private SectorObjectiveState sectorObjectiveState(StratConTrackState track) {
        boolean hasObjectives = false;
        boolean allCompleted = true;
        boolean anyFailed = false;

        for (StratConStrategicObjective objective : track.getStrategicObjectives()) {
            hasObjectives = true;

            if (!objective.isObjectiveCompleted(track)) {
                allCompleted = false;
            }
            if (objective.isObjectiveFailed(track)) {
                anyFailed = true;
            }
        }

        if (!hasObjectives) {
            return SectorObjectiveState.NONE;
        }
        if (anyFailed) {
            return SectorObjectiveState.ANY_FAILED;
        }
        if (allCompleted) {
            return SectorObjectiveState.ALL_COMPLETE;
        }
        return SectorObjectiveState.IN_PROGRESS;
    }

    /**
     * @return the tab color for the given sector, or {@code null} to use the default color when the sector has no
     *       objectives.
     */
    private Color sectorTabColor(StratConTrackState track) {
        return switch (sectorObjectiveState(track)) {
            case NONE -> null;
            case ALL_COMPLETE -> MekHQ.getMHQOptions().getFontColorPositive();
            case ANY_FAILED -> MekHQ.getMHQOptions().getFontColorNegative();
            case IN_PROGRESS -> MekHQ.getMHQOptions().getFontColorWarning();
        };
    }

    @Subscribe
    public void handleNewDay(NewDayEvent ev) {
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handleGMMode(GMModeEvent ev) {
        // Toggling GM mode enables/disables the GM button bar, so re-evaluate the tab's state immediately.
        updateCampaignState();
    }

    @Subscribe
    public void handle(MHQOptionsChangedEvent ev) {
        // The map reads client options while painting (the fog-of-war display style), so a change in MekHQ Options
        // has to trigger a repaint or the old style lingers until something else dirties the panel.
        stratconPanel.repaint();
    }

    @Subscribe
    public void handle(MissionRemovedEvent ev) {
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handle(MissionCompletedEvent ev) {
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handle(MissionChangedEvent ev) {
        // Fired (among other times) once a newly accepted contract's StratCon state has been initialized, so a
        // concurrent contract shows up in the selector immediately instead of only after the next day.
        repopulateContractsAndSectors();
        updateCampaignState();
    }

    @Subscribe
    public void handle(StratConDeploymentEvent ev) {
        updateCampaignState();
    }

    /**
     * Data structure backing an entry in the contract selection dropdown.
     */
    private record ContractItem(AbstractContract contract) {
        @Override
        @Nonnull
        public String toString() {
            return contract.getName();
        }
    }
}
