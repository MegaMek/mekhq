/*
 * Copyright (c) 2017 - The MegaMek Team. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.event.NetworkChangedEvent;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonRemovedEvent;
import mekhq.campaign.event.ScenarioResolvedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.event.UnitRemovedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.TOEMouseAdapter;
import mekhq.gui.handler.TOETransferHandler;
import mekhq.gui.model.CrewListModel;
import mekhq.gui.model.OrgTreeModel;
import mekhq.gui.view.ForceViewPanel;
import mekhq.gui.view.PersonViewPanel;
import mekhq.gui.view.UnitViewPanel;

/**
 * Display organization tree (TO&E) and force/unit summary
 */
public final class TOETab extends CampaignGuiTab {
    private static final long serialVersionUID = 5959426263276996830L;

    private JTree orgTree;
    private JSplitPane splitOrg;
    private JPanel panForceView;

    private OrgTreeModel orgModel;

    TOETab(CampaignGUI gui, String name) {
        super(gui, name);
        MekHQ.registerHandler(this);
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.TOE;
    }

    @Override
    public void initTab() {
        GridBagConstraints gridBagConstraints;
        setLayout(new GridBagLayout());

        orgModel = new OrgTreeModel(getCampaign());
        orgTree = new JTree(orgModel);
        TOEMouseAdapter.connect(getCampaignGui(), orgTree);
        orgTree.setCellRenderer(new ForceRenderer());
        orgTree.setRowHeight(60);
        orgTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        orgTree.addTreeSelectionListener(ev -> refreshForceView());
        orgTree.setDragEnabled(true);
        orgTree.setDropMode(DropMode.ON);
        orgTree.setTransferHandler(new TOETransferHandler(getCampaignGui()));

        panForceView = new JPanel();
        panForceView.setMinimumSize(new java.awt.Dimension(550, 600));
        panForceView.setPreferredSize(new java.awt.Dimension(550, 600));
        panForceView.setLayout(new BorderLayout());

        JScrollPane scrollOrg = new JScrollPane(orgTree);
        splitOrg = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollOrg, panForceView);
        splitOrg.setOneTouchExpandable(true);
        splitOrg.setResizeWeight(1.0);
        splitOrg.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> refreshForceView());
        gridBagConstraints = new java.awt.GridBagConstraints();

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(splitOrg, gridBagConstraints);
    }

    @Override
    public void refreshAll() {
        refreshOrganization();
    }

    public void refreshOrganization() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            orgTree.updateUI();
            refreshForceView();
        });
    }

    public void refreshForceView() {
    	panForceView.removeAll();
        Object node = orgTree.getLastSelectedPathComponent();
        if (null == node || -1 == orgTree.getRowForPath(orgTree.getSelectionPath())) {
            return;
        }
        if (node instanceof Unit) {
            Unit u = ((Unit) node);
            JTabbedPane tabUnit = new JTabbedPane();
            int crewSize = u.getCrew().size();
            if (crewSize > 0) {
                JPanel crewPanel = new JPanel(new BorderLayout());
                final JScrollPane scrollPerson = new JScrollPane();
                crewPanel.add(scrollPerson, BorderLayout.CENTER);
                CrewListModel model = new CrewListModel();
                model.setData(u);
                /* For units with multiple crew members, present a horizontal list above the PersonViewPanel.
                 * This custom version of JList was the only way I could figure out how to limit the JList
                 * to a single row with a horizontal scrollbar.
                 */
                final JList<Person> crewList = new JList<Person>(model) {
                    private static final long serialVersionUID = 2138771416032676227L;
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
                    crewPanel.add(new JScrollPane(crewList), BorderLayout.NORTH);
                }
                String name = "Crew";
                if (u.usesSoloPilot()) {
                    name = "Pilot";
                }
                scrollPerson.setPreferredSize(crewList.getPreferredScrollableViewportSize());
                tabUnit.add(name, crewPanel);
                SwingUtilities.invokeLater(() -> scrollPerson.getVerticalScrollBar().setValue(0));
            }
            final JScrollPane scrollUnit = new JScrollPane(new UnitViewPanel(u, getCampaign()));
            tabUnit.add("Unit", scrollUnit);
            panForceView.add(tabUnit, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> scrollUnit.getVerticalScrollBar().setValue(0));
        } else if (node instanceof Force) {
            final JScrollPane scrollForce = new JScrollPane(new ForceViewPanel((Force) node, getCampaign()));
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
    }

    @Subscribe
    public void personRemoved(PersonRemovedEvent ev) {
        orgTree.repaint();
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
