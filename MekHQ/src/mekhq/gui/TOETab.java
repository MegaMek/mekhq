/*
 * Copyright (c) 2017 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.DropMode;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.TreeSelectionModel;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.event.DeploymentChangedEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.adapter.OrgTreeMouseAdapter;
import mekhq.gui.handler.OrgTreeTransferHandler;
import mekhq.gui.model.OrgTreeModel;
import mekhq.gui.view.ForceViewPanel;
import mekhq.gui.view.PersonViewPanel;
import mekhq.gui.view.UnitViewPanel;

/**
 * Display organization tree (TO&E) and force/unit summary
 *
 */
public final class TOETab extends CampaignGuiTab {

    private static final long serialVersionUID = 5959426263276996830L;

    private JTree orgTree;
    private JSplitPane splitOrg;
    private JScrollPane scrollForceView;

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
        orgTree.addMouseListener(new OrgTreeMouseAdapter(getCampaignGui()));
        orgTree.setCellRenderer(new ForceRenderer(getIconPackage()));
        orgTree.setRowHeight(60);
        orgTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        orgTree.addTreeSelectionListener(ev -> refreshForceView());
        orgTree.setDragEnabled(true);
        orgTree.setDropMode(DropMode.ON);
        orgTree.setTransferHandler(new OrgTreeTransferHandler(getCampaignGui()));

        scrollForceView = new JScrollPane();
        scrollForceView.setMinimumSize(new java.awt.Dimension(550, 600));
        scrollForceView.setPreferredSize(new java.awt.Dimension(550, 600));
        scrollForceView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        splitOrg = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(orgTree), scrollForceView);
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
        refreshForceView();
    }

    public void refreshOrganization() {
        javax.swing.SwingUtilities.invokeLater(() -> {
            orgTree.updateUI();
            // This seems like bad juju since it makes it annoying as hell to
            // add multiple units to a force if it's de-selected every single
            // time
            // So, commenting it out - ralgith
            // orgTree.setSelectionPath(null);
            refreshForceView();
            if (getCampaign().getCampaignOptions().getUseAtB()) {
                getCampaignGui().refreshLanceAssignments();
            }
        });
        getCampaignGui().refreshRating();
    }

    public void refreshForceView() {
        Object node = orgTree.getLastSelectedPathComponent();
        if (null == node || -1 == orgTree.getRowForPath(orgTree.getSelectionPath())) {
            scrollForceView.setViewportView(null);
            return;
        }
        if (node instanceof Unit) {
            Unit u = ((Unit) node);
            JTabbedPane tabUnit = new JTabbedPane();
            Person p = u.getCommander();
            if (p != null) {
                String name = "Commander";
                if (u.usesSoloPilot()) {
                    name = "Pilot";
                }
                tabUnit.add(name, new PersonViewPanel(p, getCampaign(), getIconPackage()));
            }
            tabUnit.add("Unit",
                    new UnitViewPanel(u, getCampaign(), getIconPackage().getCamos(), getIconPackage().getMechTiles()));
            scrollForceView.setViewportView(tabUnit);
            // This odd code is to make sure that the scrollbar stays at the top
            // I can't just call it here, because it ends up getting reset
            // somewhere later
            javax.swing.SwingUtilities.invokeLater(() -> scrollForceView.getVerticalScrollBar().setValue(0));
        } else if (node instanceof Force) {
            scrollForceView.setViewportView(new ForceViewPanel((Force) node, getCampaign(), getIconPackage()));
            javax.swing.SwingUtilities.invokeLater(() -> scrollForceView.getVerticalScrollBar().setValue(0));
        }
    }
    
    @Subscribe
    public void deploymentChanged(DeploymentChangedEvent ev) {
        refreshOrganization();
    }
}
