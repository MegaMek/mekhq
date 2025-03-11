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
 */
package mekhq.gui;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.*;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.TOEMouseAdapter;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.handler.TOETransferHandler;
import mekhq.gui.model.CrewListModel;
import mekhq.gui.model.OrgTreeModel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.view.ForceViewPanel;
import mekhq.gui.view.PersonViewPanel;
import mekhq.gui.view.UnitViewPanel;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;

/**
 * Display organization tree (TO&amp;E) and force/unit summary
 */
public final class TOETab extends CampaignGuiTab {
    private JTree orgTree;
    private JSplitPane splitOrg;
    private JPanel panForceView;
    private JTabbedPane tabUnit;

    private OrgTreeModel orgModel;

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

        orgModel = new OrgTreeModel(getCampaign());
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

        panForceView = new JPanel();
        panForceView.getAccessibleContext().setAccessibleName("Selected Force Viewer");
        panForceView.setMinimumSize(new Dimension(550, 600));
        panForceView.setPreferredSize(new Dimension(550, 600));
        panForceView.setLayout(new BorderLayout());

        JScrollPane scrollOrg = new JScrollPaneWithSpeed(orgTree);
        splitOrg = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOrg, panForceView);
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
        if (node instanceof Unit) {
            Unit u = ((Unit) node);
            tabUnit = new JTabbedPane();
            int crewSize = u.getCrew().size();
            if (crewSize > 0) {
                JPanel crewPanel = new JPanel(new BorderLayout());
                crewPanel.getAccessibleContext().setAccessibleName("Crew for " + u.getName());
                final JScrollPane scrollPerson = new JScrollPaneWithSpeed();
                crewPanel.add(scrollPerson, BorderLayout.CENTER);
                CrewListModel model = new CrewListModel();
                model.setData(u);
                /* For units with multiple crew members, present a horizontal list above the PersonViewPanel.
                 * This custom version of JList was the only way I could figure out how to limit the JList
                 * to a single row with a horizontal scrollbar.
                 */
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
                if (crewSize > 1) {
                    crewPanel.add(new JScrollPaneWithSpeed(crewList), BorderLayout.NORTH);
                }
                String name = "Crew";
                if (u.usesSoloPilot()) {
                    name = "Pilot";
                }
                scrollPerson.setPreferredSize(crewList.getPreferredScrollableViewportSize());
                tabUnit.add(name, crewPanel);
                SwingUtilities.invokeLater(() -> scrollPerson.getVerticalScrollBar().setValue(0));
            }
            final JScrollPane scrollUnit = new JScrollPaneWithSpeed(new UnitViewPanel(u, getCampaign()));
            tabUnit.add("Unit", scrollUnit);
            panForceView.add(tabUnit, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> scrollUnit.getVerticalScrollBar().setValue(0));
            try {
                tabUnit.setSelectedIndex(tabUnitLastSelectedIndex);
                tabUnit.addChangeListener(evt -> forceViewTabChange()); // added late so it won't overwrite
            } catch (ArrayIndexOutOfBoundsException ignored) {}
            // We can ignore here because if the selected index is out of bounds, we're just going
            // to not select the unit in the TO&E.
        } else if (node instanceof Force) {
            final JScrollPane scrollForce = new JScrollPaneWithSpeed(new ForceViewPanel((Force) node, getCampaign()));
            panForceView.add(scrollForce, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> scrollForce.getVerticalScrollBar().setValue(0));
        }
        panForceView.updateUI();
    }

    private ActionScheduler orgRefreshScheduler = new ActionScheduler(this::refreshOrganization);

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
