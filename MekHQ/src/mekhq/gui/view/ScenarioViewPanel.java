/*
 * AtBScenarioViewPanel.java
 *
 * Copyright (C) 2016 MegaMek team
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
package mekhq.gui.view;

import java.awt.Component;
import java.awt.Image;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import megamek.common.Crew;
import mekhq.IconPackage;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.force.UnitStub;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.utilities.MarkdownRenderer;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author  Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ScenarioViewPanel extends ScrollablePanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private Scenario scenario;
    private Campaign campaign;
    private ForceStub forces;
    private IconPackage icons;

    private javax.swing.JPanel pnlStats;
    private javax.swing.JTextPane txtDesc;
    private javax.swing.JTextPane txtReport;
    private javax.swing.JTree forceTree;
    private javax.swing.JLabel lblStatus;

    private StubTreeModel forceModel;

    public ScenarioViewPanel(Scenario s, Campaign c, IconPackage i) {
        this.scenario = s;
        this.campaign = c;
        this.icons = i;
        if(s.isCurrent()) {
            this.forces = new ForceStub(s.getForces(campaign), campaign);
        } else {
            this.forces = s.getForceStub();
        }
        forceModel = new StubTreeModel(forces);
        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pnlStats = new javax.swing.JPanel();
        txtDesc = new javax.swing.JTextPane();
        txtReport = new javax.swing.JTextPane();
        forceTree = new javax.swing.JTree();

        setLayout(new java.awt.GridBagLayout());

        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder(scenario.getName()));
        fillStats();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        forceTree.setModel(forceModel);
        forceTree.setCellRenderer(new ForceStubRenderer());
        forceTree.setRowHeight(50);
        forceTree.setRootVisible(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(forceTree, gridBagConstraints);

        if(null != scenario.getReport() && !scenario.getReport().isEmpty()) {
            txtReport.setName("txtReport");
            txtReport.setEditable(false);
            txtReport.setContentType("text/html");
            txtReport.setText(MarkdownRenderer.getRenderedHtml(scenario.getReport()));
            txtReport.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("After-Action Report"),
                    BorderFactory.createEmptyBorder(0,2,2,2)));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            add(txtReport, gridBagConstraints);
        }
    }

    private void fillStats() {

        lblStatus = new javax.swing.JLabel();

        java.awt.GridBagConstraints gridBagConstraints;
        pnlStats.setLayout(new java.awt.GridBagLayout());

        lblStatus.setName("lblOwner"); // NOI18N
        lblStatus.setText("<html><b>" + scenario.getStatusName() + "</b></html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        pnlStats.add(lblStatus, gridBagConstraints);

        if(null != scenario.getDescription() && !scenario.getDescription().isEmpty()) {
            txtDesc.setName("txtDesc");
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(scenario.getDescription()));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 2;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(txtDesc, gridBagConstraints);
        }

        if(scenario.getLoot().size() > 0) {
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = 3;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            pnlStats.add(new JLabel("<html><b>Potential Rewards:</b></html>"), gridBagConstraints);

            for(Loot loot : scenario.getLoot()) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 1.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
                gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                pnlStats.add(new JLabel(loot.getShortDescription()), gridBagConstraints);
            }
        }

    }

    protected class StubTreeModel implements TreeModel {

        private ForceStub rootForce;
        private Vector<TreeModelListener> listeners = new Vector<TreeModelListener>();

        public StubTreeModel(ForceStub root) {
            rootForce = root;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if(parent instanceof ForceStub) {
                return ((ForceStub)parent).getAllChildren().get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if(parent instanceof ForceStub) {
                return ((ForceStub)parent).getAllChildren().size();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if(parent instanceof ForceStub) {
                return ((ForceStub)parent).getAllChildren().indexOf(child);
            }
            return 0;
        }

        @Override
        public Object getRoot() {
            return rootForce;
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof UnitStub || (node instanceof ForceStub && ((ForceStub)node).getAllChildren().size() == 0);
        }

        @Override
        public void valueForPathChanged(TreePath arg0, Object arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addTreeModelListener( TreeModelListener listener ) {
              if ( listener != null && !listeners.contains( listener ) ) {
                 listeners.addElement( listener );
              }
           }

           @Override
        public void removeTreeModelListener( TreeModelListener listener ) {
              if ( listener != null ) {
                 listeners.removeElement( listener );
              }
           }
    }

    protected class ForceStubRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 4076620029822185784L;

        public ForceStubRenderer() {

        }

        @Override
        public Component getTreeCellRendererComponent(
                            JTree tree,
                            Object value,
                            boolean sel,
                            boolean expanded,
                            boolean leaf,
                            int row,
                            boolean hasFocus) {

            super.getTreeCellRendererComponent(
                            tree, value, sel,
                            expanded, leaf, row,
                            hasFocus);
            //setOpaque(true);
            setIcon(getIcon(value));

            return this;
        }

        protected Icon getIcon(Object node) {

            if(node instanceof UnitStub) {
                return getIconFrom((UnitStub)node);
            } else if(node instanceof ForceStub) {
                return getIconFrom((ForceStub)node);
            } else {
                return null;
            }
        }

        protected Icon getIconFrom(UnitStub unit) {
            String category = unit.getPortraitCategory();
            String filename = unit.getPortraitFileName();

            if(Crew.ROOT_PORTRAIT.equals(category)) {
                category = "";
            }

            // Return a null if the player has selected no portrait file.
            if ((null == category) || (null == filename) || Crew.PORTRAIT_NONE.equals(filename)) {
                filename = "default.gif";
            }
            // Try to get the player's portrait file.
            Image portrait = null;
            try {
                portrait = (Image) icons.getPortraits().getItem(category, filename);
                if(null != portrait) {
                    portrait = portrait.getScaledInstance(50, -1, Image.SCALE_DEFAULT);
                } else {
                    portrait = (Image) icons.getPortraits().getItem("", "default.gif");
                    if(null != portrait) {
                        portrait = portrait.getScaledInstance(50, -1, Image.SCALE_DEFAULT);
                    }
                }
                return new ImageIcon(portrait);
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "getIconFrom", e);
                return null;
            }
        }

        protected Icon getIconFrom(ForceStub force) {
            String category = force.getIconCategory();
            String filename = force.getIconFileName();
            LinkedHashMap<String, Vector<String>> iconMap = force.getIconMap();

            if(Crew.ROOT_PORTRAIT.equals(category)) {
                category = "";
            }

            // Return a null if the player has selected no portrait file.
            if ((null == category) || (null == filename) || (Crew.PORTRAIT_NONE.equals(filename) && !Force.ROOT_LAYERED.equals(category))) {
                filename = "empty.png";
            }

            // Try to get the player's portrait file.
            Image portrait = null;
            try {
                portrait = IconPackage.buildForceIcon(category, filename, icons.getForceIcons(), iconMap);
               if(null != portrait) {
                portrait = portrait.getScaledInstance(58, -1, Image.SCALE_SMOOTH);
            } else {
                portrait = (Image) icons.getForceIcons().getItem("", "empty.png");
                if(null != portrait) {
                    portrait = portrait.getScaledInstance(58, -1, Image.SCALE_SMOOTH);
                }
                }
                return new ImageIcon(portrait);
            } catch (Exception e) {
                MekHQ.getLogger().error(getClass(), "getIconFrom", e);
                return null;
            }
       }
    }
}
