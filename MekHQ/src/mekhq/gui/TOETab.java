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

import static megamek.client.ui.util.UIUtil.scaleForGUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.events.DeploymentChangedEvent;
import mekhq.campaign.events.NetworkChangedEvent;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.persons.PersonRemovedEvent;
import mekhq.campaign.events.scenarios.ScenarioResolvedEvent;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.events.units.UnitRemovedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.stratCon.MaplessStratCon;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.TOEMouseAdapter;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.ForceTemplateAssignmentDialog;
import mekhq.gui.dialog.MaplessStratConForcePicker;
import mekhq.gui.dialog.MaplessStratConScenarioPicker;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.handler.TOETransferHandler;
import mekhq.gui.model.CrewListModel;
import mekhq.gui.model.OrgTreeModel;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.view.ForceViewPanel;
import mekhq.gui.view.PersonViewPanel;
import mekhq.gui.view.UnitViewPanel;

/**
 * Display organization tree (TO&amp;E) and force/unit summary
 */
public final class TOETab extends CampaignGuiTab {
    private JTree orgTree;
    private JPanel panForceView;
    private JTabbedPane tabUnit;

    private int tabUnitLastSelectedIndex;

    //region Constructors
    public TOETab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }
    //endregion Constructors

    @Override
    public MHQTabType tabType() {
        return MHQTabType.TOE;
    }

    @Override
    public void initTab() {
        setLayout(new GridBagLayout());

        OrgTreeModel orgModel = new OrgTreeModel(getCampaign());
        orgTree = new JTree(orgModel);
        orgTree.getAccessibleContext().setAccessibleName("Table of Organization and Equipment (TOE)");
        TOEMouseAdapter.connect(getCampaignGui(), orgTree);
        orgTree.setCellRenderer(new ForceRenderer());
        orgTree.setRowHeight(60);
        orgTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        orgTree.addTreeSelectionListener(evt -> refreshForceView());
        orgTree.setDragEnabled(true);
        orgTree.setDropMode(DropMode.ON);
        orgTree.setTransferHandler(new TOETransferHandler(getCampaignGui()));
        orgTree.setBorder(RoundedLineBorder.createRoundedLineBorder());
        orgTree.setFocusable(false);

        JPanel pnlTutorial = new TutorialHyperlinkPanel("toeTab");

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(null);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnDeploy = new RoundedJButton("Deploy Directly to Scenario");
        btnDeploy.addActionListener(evt -> deploymentButton());
        buttonPanel.add(btnDeploy);
        leftPanel.add(buttonPanel, BorderLayout.NORTH);

        JScrollPane orgScrollPane = new JScrollPane(orgTree);
        orgScrollPane.setBorder(null);
        leftPanel.add(orgScrollPane, BorderLayout.CENTER);
        leftPanel.add(pnlTutorial, BorderLayout.SOUTH);

        panForceView = new JPanel();
        panForceView.getAccessibleContext().setAccessibleName("Selected Force Viewer");

        Dimension dimension = scaleForGUI(700, 600);
        panForceView.setMinimumSize(dimension);
        panForceView.setPreferredSize(dimension);
        panForceView.setLayout(new BorderLayout());

        JSplitPane splitOrg = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, panForceView);
        splitOrg.setOneTouchExpandable(true);
        splitOrg.setResizeWeight(1.0);
        splitOrg.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> refreshForceView());

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(splitOrg, gridBagConstraints);

        tabUnitLastSelectedIndex = 0;
    }

    /**
     * Handles the deployment button action by allowing the player to select a scenario and deploy forces to it.
     *
     * <p>This method presents a dialog with all current scenarios from active missions, sorted by date (newest
     * first). After the player selects a scenario, the method determines whether it's a StratCon scenario or a regular
     * scenario and delegates to the appropriate deployment handler.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void deploymentButton() {
        // Build scenario list with mission mapping
        Map<Scenario, Mission> scenarioMissionMap = new HashMap<>();
        for (Mission mission : getCampaign().getActiveMissions(false)) {
            for (Scenario scenario : mission.getCurrentScenarios()) {
                scenarioMissionMap.put(scenario, mission);
            }
        }

        List<Scenario> sortedScenarios = new ArrayList<>(scenarioMissionMap.keySet());
        sortedScenarios.sort(Comparator.comparing(Scenario::getDate).reversed());

        // Show scenario picker
        MaplessStratConScenarioPicker scenarioPicker = new MaplessStratConScenarioPicker(getCampaign(),
              sortedScenarios);
        if (!scenarioPicker.wasConfirmed()) {
            return;
        }

        Scenario selectedScenario = sortedScenarios.get(scenarioPicker.getComboBoxChoiceIndex());
        Mission selectedMission = scenarioMissionMap.get(selectedScenario);

        // Check if this is a StratCon scenario
        boolean isStratConScenario = selectedScenario instanceof AtBDynamicScenario &&
                                           selectedMission instanceof AtBContract atbContract &&
                                           atbContract.getStratconCampaignState() != null;

        if (isStratConScenario) {
            deployToStratCon(selectedScenario);
        } else {
            deployToRegularScenario(selectedScenario);
        }
    }

    /**
     * Deploys forces to a StratCon scenario using the mapless StratCon deployment interface.
     *
     * <p>This method retrieves the StratCon tab and delegates to the mapless deployment system, which handles force
     * assignment through the StratCon scenario wizard.</p>
     *
     * @param selectedScenario the StratCon scenario to deploy forces to
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void deployToStratCon(Scenario selectedScenario) {
        if (getCampaignGui().getTab(MHQTabType.STRAT_CON) instanceof StratConTab stratConTab) {
            MaplessStratCon.deployWithoutMap(stratConTab.getStratconPanel(), getCampaign(), selectedScenario);
        }
    }

    /**
     * Deploys forces to a regular (non-StratCon) scenario.
     *
     * <p>This method presents a dialog allowing the player to select from available combat teams that are not
     * currently deployed. For AtB dynamic scenarios, it opens the force template assignment dialog. For standard
     * scenarios, it directly assigns the selected force to the scenario and triggers the appropriate deployment
     * events.</p>
     *
     * @param selectedScenario the scenario to deploy forces to
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void deployToRegularScenario(Scenario selectedScenario) {
        // Get available forces
        List<Force> forceOptions = getCampaign().getCombatTeamsAsList().stream()
                                         .map(combatTeam -> getCampaign().getForce(combatTeam.getForceId()))
                                         .filter(force -> force != null && !force.isDeployed())
                                         .sorted(Comparator.comparing(Force::getFullName))
                                         .toList();

        // Show force picker
        MaplessStratConForcePicker forcePicker = new MaplessStratConForcePicker(getCampaign(), forceOptions);
        if (!forcePicker.wasConfirmed()) {
            return;
        }

        Force selectedForce = forceOptions.get(forcePicker.getComboBoxChoiceIndex());

        // Deploy force to scenario
        if (selectedScenario instanceof AtBDynamicScenario dynamicScenario) {
            new ForceTemplateAssignmentDialog(getCampaignGui(),
                  new Vector<>(List.of(selectedForce)),
                  null,
                  dynamicScenario);
        } else {
            getCampaignGui().undeployForce(selectedForce);
            selectedForce.clearScenarioIds(getCampaign(), true);
            if (selectedScenario != null) {
                selectedScenario.addForces(selectedForce.getId());
                selectedForce.setScenarioId(selectedScenario.getId(), getCampaign());
            }
            MekHQ.triggerEvent(new DeploymentChangedEvent(selectedForce, selectedScenario));
        }
    }

    @Override
    public void refreshAll() {
        refreshOrganization();
    }

    public void refreshOrganization() {
        SwingUtilities.invokeLater(() -> {
            orgTree.updateUI();
            refreshForceView();
        });
    }


    public void forceViewTabChange() {
        tabUnitLastSelectedIndex = tabUnit.getSelectedIndex();
    }

    public void refreshForceView() {
        panForceView.removeAll();
        Object node = orgTree.getLastSelectedPathComponent();
        if (null == node || -1 == orgTree.getRowForPath(orgTree.getSelectionPath())) {
            return;
        }
        if (node instanceof Unit unit) {
            tabUnit = new JTabbedPane();
            int crewSize = unit.getCrew().size();
            if (crewSize > 0) {
                JPanel crewPanel = new JPanel(new BorderLayout());
                crewPanel.getAccessibleContext().setAccessibleName("Crew for " + unit.getName());
                final JScrollPane scrollPerson = new JScrollPaneWithSpeed();
                scrollPerson.setBorder(null);
                crewPanel.add(scrollPerson, BorderLayout.CENTER);
                CrewListModel model = new CrewListModel();
                model.setData(unit, getCampaign().getCampaignOptions().isUseSmallArmsOnly());
                /* For units with multiple crew members, present a horizontal list above the PersonViewPanel.
                 * This custom version of JList was the only way I could figure out how to limit the JList
                 * to a single row with a horizontal scrollbar.
                 */
                final JList<Person> crewList = getCrewList(model, scrollPerson);
                if (crewSize > 1) {
                    JScrollPaneWithSpeed crewScrollPane = new JScrollPaneWithSpeed(crewList);
                    crewScrollPane.setBorder(null);
                    crewPanel.add(crewScrollPane, BorderLayout.NORTH);
                }
                String name = "Crew";
                if (unit.usesSoloPilot()) {
                    name = "Pilot";
                }
                scrollPerson.setPreferredSize(crewList.getPreferredScrollableViewportSize());
                tabUnit.add(name, crewPanel);
                SwingUtilities.invokeLater(() -> scrollPerson.getVerticalScrollBar().setValue(0));
            }
            final JScrollPane scrollUnit = new JScrollPaneWithSpeed(new UnitViewPanel(unit, getCampaign()));
            scrollUnit.setBorder(null);
            tabUnit.add("Unit", scrollUnit);
            panForceView.add(tabUnit, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> scrollUnit.getVerticalScrollBar().setValue(0));
            try {
                tabUnit.setSelectedIndex(tabUnitLastSelectedIndex);
                tabUnit.addChangeListener(evt -> forceViewTabChange()); // added late so it won't overwrite
            } catch (IndexOutOfBoundsException ignored) {}
            // We can ignore here because if the selected index is out of bounds, we're just going
            // to not select the unit in the TO&E.
        } else if (node instanceof Force) {
            final JScrollPane scrollForce = new JScrollPaneWithSpeed(new ForceViewPanel((Force) node, getCampaign()));
            scrollForce.setBorder(null);
            panForceView.add(scrollForce, BorderLayout.CENTER);
            panForceView.setBorder(null);
            SwingUtilities.invokeLater(() -> scrollForce.getVerticalScrollBar().setValue(0));
        }
        panForceView.updateUI();
    }

    private JList<Person> getCrewList(CrewListModel model, JScrollPane scrollPerson) {
        final JList<Person> crewList = new JList<>(model) {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                Dimension d = super.getPreferredScrollableViewportSize();
                d.width = scrollPerson.getPreferredSize().width;
                return d;
            }
        };
        crewList.setCellRenderer(model.getRenderer());
        crewList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        crewList.setVisibleRowCount(1);
        crewList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        crewList.addListSelectionListener(e -> {
            if (null != model.getElementAt(crewList.getSelectedIndex())) {
                scrollPerson.setViewportView(new PersonViewPanel(model.getElementAt(crewList.getSelectedIndex()),
                      getCampaign(), getCampaignGui()));
            }
        });
        crewList.setSelectedIndex(0);
        return crewList;
    }

    private final ActionScheduler orgRefreshScheduler = new ActionScheduler(this::refreshOrganization);

    @Subscribe
    public void deploymentChanged(DeploymentChangedEvent ev) {
        orgTree.repaint();
    }

    @Subscribe
    public void organizationChanged(OrganizationChangedEvent ev) {
        orgRefreshScheduler.schedule();
    }

    @Subscribe
    public void networkChanged(NetworkChangedEvent ev) {
        orgTree.repaint();
    }

    @Subscribe
    public void scenarioResolved(ScenarioResolvedEvent ev) {
        orgRefreshScheduler.schedule();
    }

    @Subscribe
    public void personChanged(PersonChangedEvent ev) {
        orgTree.repaint();
        orgRefreshScheduler.schedule();
    }

    @Subscribe
    public void personRemoved(PersonRemovedEvent ev) {
        orgTree.repaint();
        orgRefreshScheduler.schedule();
    }

    @Subscribe
    public void unitChanged(UnitChangedEvent ev) {
        orgTree.repaint();
    }

    @Subscribe
    public void unitRemoved(UnitRemovedEvent ev) {
        orgRefreshScheduler.schedule();
    }
}
