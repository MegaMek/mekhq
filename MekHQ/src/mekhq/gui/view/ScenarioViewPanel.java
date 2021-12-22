/*
 * AtBScenarioViewPanel.java
 *
 * Copyright (C) 2016-2020 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.UnitEditorDialog;
import mekhq.MHQStaticDirectoryManager;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.force.UnitStub;
import mekhq.campaign.mission.BotForceStub;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.Scenario;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.MarkdownRenderer;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * A custom panel that gets filled in with goodies from a scenario object
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ScenarioViewPanel extends JScrollablePanel {
    private static final long serialVersionUID = 7004741688464105277L;

    private JFrame frame;

    private Scenario scenario;
    private Campaign campaign;
    private ForceStub forces;
    private List<BotForceStub> botStubs;

    private javax.swing.JPanel pnlStats;
    private javax.swing.JTextPane txtDesc;
    private javax.swing.JTextPane txtReport;
    private javax.swing.JTree forceTree;
    private javax.swing.JLabel lblStatus;

    private StubTreeModel forceModel;

    public ScenarioViewPanel(Scenario s, Campaign c, JFrame f) {
        super();
        this.frame = f;
        this.scenario = s;
        this.campaign = c;
        this.forces = s.getStatus().isCurrent() ? new ForceStub(s.getForces(c), c) : s.getForceStub();
        forceModel = new StubTreeModel(forces);

        botStubs = new ArrayList<>();
        if (s.getStatus().isCurrent()) {
            for (int i = 0; i < s.getNumBots(); i++) {
                botStubs.add(s.generateBotStub(s.getBotForce(i)));
            }
        } else {
            botStubs = s.getBotForceStubs();
        }

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
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
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

        int y = 2;

        for (int i = 0; i < botStubs.size(); i++) {
            if (null == botStubs.get(i)) {
                continue;
            }

            DefaultMutableTreeNode top = new DefaultMutableTreeNode(botStubs.get(i).getName());
            for (String en : botStubs.get(i).getEntityList()) {
                top.add(new DefaultMutableTreeNode(en));
            }
            JTree tree = new JTree(top);
            tree.collapsePath(new TreePath(top));
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridheight = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            add(tree, gridBagConstraints);
            if (scenario.getStatus().isCurrent()) {
                tree.addMouseListener(new ScenarioViewPanel.TreeMouseAdapter(tree, i));
            }
        }

        if (null != scenario.getReport() && !scenario.getReport().isEmpty()) {
            txtReport.setName("txtReport");
            txtReport.setEditable(false);
            txtReport.setContentType("text/html");
            txtReport.setText(MarkdownRenderer.getRenderedHtml(scenario.getReport()));
            txtReport.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("After-Action Report"),
                    BorderFactory.createEmptyBorder(0,2,2,2)));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
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

        lblStatus.setName("lblOwner");
        lblStatus.setText("<html><b>" + scenario.getStatus() + "</b></html>");
        lblStatus.setToolTipText(scenario.getStatus().getToolTipText());
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

        if (null != scenario.getDescription() && !scenario.getDescription().isEmpty()) {
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

        if (scenario.getLoot().size() > 0) {
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

            for (Loot loot : scenario.getLoot()) {
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

    protected static class StubTreeModel implements TreeModel {
        private ForceStub rootForce;
        private Vector<TreeModelListener> listeners = new Vector<>();

        public StubTreeModel(ForceStub root) {
            rootForce = root;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof ForceStub) {
                return ((ForceStub) parent).getAllChildren().get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof ForceStub) {
                return ((ForceStub) parent).getAllChildren().size();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof ForceStub) {
                return ((ForceStub) parent).getAllChildren().indexOf(child);
            }
            return 0;
        }

        @Override
        public Object getRoot() {
            return rootForce;
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof UnitStub || (node instanceof ForceStub && ((ForceStub) node).getAllChildren().size() == 0);
        }

        @Override
        public void valueForPathChanged(TreePath arg0, Object arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void addTreeModelListener( TreeModelListener listener ) {
              if ((listener != null) && !listeners.contains(listener)) {
                 listeners.addElement(listener);
              }
        }

        @Override
        public void removeTreeModelListener( TreeModelListener listener ) {
            if (listener != null) {
                listeners.removeElement(listener);
            }
        }
    }

    protected static class ForceStubRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 4076620029822185784L;

        public ForceStubRenderer() {

        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                      boolean expanded, boolean leaf, int row,
                                                      boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            //setOpaque(true);
            setIcon(getIcon(value));

            return this;
        }

        protected Icon getIcon(Object node) {
            if (node instanceof UnitStub) {
                return ((UnitStub) node).getPortrait().getImageIcon(50);
            } else if (node instanceof ForceStub) {
                return getIconFrom((ForceStub) node);
            } else {
                return null;
            }
        }

        protected Icon getIconFrom(ForceStub force) {
            try {
                return new ImageIcon(MHQStaticDirectoryManager.buildForceIcon(force.getIconCategory(),
                        force.getIconFileName(), force.getIconMap())
                        .getScaledInstance(58, -1, Image.SCALE_SMOOTH));
            } catch (Exception e) {
                LogManager.getLogger().error(e);
                return null;
            }
        }
    }

    private class TreeMouseAdapter extends MouseInputAdapter implements ActionListener {
        private JTree tree;
        int index;

        public TreeMouseAdapter(JTree tree, int index) {
            this.tree = tree;
            this.index = index;
        }

        @Override
        public void actionPerformed(ActionEvent action) {
            String command = action.getActionCommand();

            if (command.equalsIgnoreCase("CONFIG_BOT")) {
                BotConfigDialog pbd = new BotConfigDialog(frame,
                        null,
                        scenario.getBotForce(index).getBehaviorSettings(),
                        null);
                pbd.setBotName(scenario.getBotForce(index).getName());
                pbd.setVisible(true);
                if (pbd.getResult() != DialogResult.CANCELLED) {
                    scenario.getBotForce(index).setBehaviorSettings(pbd.getBehaviorSettings());
                    scenario.getBotForce(index).setName(pbd.getBotName());
                }
            } else if (command.equalsIgnoreCase("EDIT_UNIT")) {
                if ((tree.getSelectionCount() > 0) && (tree.getSelectionRows() != null)) {
                    // row 0 is root node
                    int i = tree.getSelectionRows()[0] - 1;
                    UnitEditorDialog med = new UnitEditorDialog(frame,
                            scenario.getBotForce(index).getEntityList().get(i));
                    med.setVisible(true);
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            final JPopupMenu popup = new JPopupMenu();
            if (e.isPopupTrigger()) {
                final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }

                JMenuItem menuItem;
                if (path.getPathCount() > 1) {
                    menuItem = new JMenuItem("Edit Unit...");
                    menuItem.setActionCommand("EDIT_UNIT");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }
                menuItem = new JMenuItem("Configure Bot...");
                menuItem.setActionCommand("CONFIG_BOT");
                menuItem.addActionListener(this);
                popup.add(menuItem);
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }
}
