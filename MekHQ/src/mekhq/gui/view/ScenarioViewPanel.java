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
import megamek.common.PlanetaryConditions;
import megamek.common.util.EncodeControl;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
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

    private JPanel pnlStats;
    private JPanel pnlMap;
    private JTextPane txtDesc;
    private JTextPane txtReport;
    private JTree forceTree;
    private JLabel lblStatus;

    private JLabel lblTerrain = new JLabel();
    private JLabel lblTerrainDesc = new JLabel();
    private JLabel lblMap = new JLabel();
    private JLabel lblMapDesc = new JLabel();
    private JLabel lblMapSize = new JLabel();
    private JLabel lblMapSizeDesc = new JLabel();
    private JLabel lblLight = new JLabel();
    private JLabel lblLightDesc = new JLabel();
    private JLabel lblWeather = new JLabel();
    private JLabel lblWeatherDesc = new JLabel();
    private JLabel lblWind = new JLabel();
    private JLabel lblWindDesc = new JLabel();
    private JLabel lblFog = new JLabel();
    private JLabel lblFogDesc = new JLabel();
    private JLabel lblAtmosphere = new JLabel();
    private JLabel lblAtmosphereDesc = new JLabel();
    private JLabel lblTemperature = new JLabel();
    private JLabel lblTemperatureDesc = new JLabel();
    private JLabel lblGravity = new JLabel();
    private JLabel lblGravityDesc = new JLabel();
    private JLabel lblOtherConditions = new JLabel();
    private JLabel lblOtherConditionsDesc = new JLabel();

    private StubTreeModel forceModel;

    ResourceBundle resourceMap;

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
            botStubs = s.getBotForcesStubs();
        }

        initComponents();
    }

    private void initComponents() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.ScenarioViewPanel", new EncodeControl()); //$NON-NLS-1$

        java.awt.GridBagConstraints gridBagConstraints;

        pnlStats = new JPanel();
        pnlMap = new JPanel();
        txtDesc = new JTextPane();
        txtReport = new JTextPane();
        forceTree = new JTree();

        setLayout(new java.awt.GridBagLayout());

        int y = 0;

        pnlStats.setName("pnlStats");
        pnlStats.setBorder(BorderFactory.createTitledBorder(scenario.getName()));
        fillStats();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(pnlStats, gridBagConstraints);

        if(null != scenario.getMap()) {
            pnlMap.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlMap.title")));
            fillMapData();
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridheight = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            add(pnlMap, gridBagConstraints);
        }

        forceTree.setModel(forceModel);
        forceTree.setCellRenderer(new ForceStubRenderer());
        forceTree.setRowHeight(50);
        forceTree.setRootVisible(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(forceTree, gridBagConstraints);

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
            gridBagConstraints.gridwidth = 2;
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

    private void fillMapData() {

        pnlMap.setLayout(new java.awt.GridBagLayout());

        java.awt.GridBagConstraints gridBagConstraints;
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;

        int y = 0;

        if (scenario.getTerrainType() == Scenario.TER_SPACE) {
            y = fillSpaceStats(gridBagConstraints, resourceMap, y);
        } else {
            y = fillPlanetSideStats(gridBagConstraints, resourceMap, y);
        }
    }

    /**
     * Worker function that generates UI elements appropriate for space scenarios
     * @param gridBagConstraints Current grid bag constraints in use
     * @param resourceMap Text resource
     * @param y current row in the parent UI element
     * @return the row at which we wind up after doing all this
     */
    private int fillSpaceStats(GridBagConstraints gridBagConstraints, ResourceBundle resourceMap, int y) {

        lblTerrain.setText(resourceMap.getString("lblTerrain.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblTerrain, gridBagConstraints);

        lblTerrainDesc.setText("Space");
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblTerrainDesc, gridBagConstraints);

        lblMapSize.setText(resourceMap.getString("lblMapSize.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblMapSize, gridBagConstraints);

        lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblMapSizeDesc, gridBagConstraints);

        return y;
    }

    /**
     * Worker function that generates UI elements appropriate for planet-side scenarios
     * @param gridBagConstraints Current grid bag constraints in use
     * @param resourceMap Text resource
     * @param y current row in the parent UI element
     * @return the row at which we wind up after doing all this
     */
    private int fillPlanetSideStats(GridBagConstraints gridBagConstraints, ResourceBundle resourceMap, int y) {

        lblTerrain.setText(resourceMap.getString("lblTerrain.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblTerrain, gridBagConstraints);

        if (scenario.getTerrainType() == Scenario.TER_LOW_ATMO) {
            lblTerrainDesc.setText("Low Atmosphere");
        } else {
            lblTerrainDesc.setText("Ground");
        }
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblTerrainDesc, gridBagConstraints);

        lblMap.setText(resourceMap.getString("lblMap.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblMap, gridBagConstraints);

        lblMapDesc.setText(scenario.getMapForDisplay());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblMapDesc, gridBagConstraints);

        lblMapSize.setText(resourceMap.getString("lblMapSize.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblMapSize, gridBagConstraints);

        lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblMapSizeDesc, gridBagConstraints);

        lblLight.setText(resourceMap.getString("lblLight.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblLight, gridBagConstraints);

        lblLightDesc.setText(PlanetaryConditions.getLightDisplayableName(scenario.getLight()));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblLightDesc, gridBagConstraints);

        lblWeather.setText(resourceMap.getString("lblWeather.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblWeather, gridBagConstraints);

        lblWeatherDesc.setText(PlanetaryConditions.getWeatherDisplayableName(scenario.getWeather()));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblWeatherDesc, gridBagConstraints);

        lblWind.setText(resourceMap.getString("lblWind.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblWind, gridBagConstraints);

        lblWindDesc.setText(PlanetaryConditions.getWindDisplayableName(scenario.getWind()));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblWindDesc, gridBagConstraints);

        lblFog.setText(resourceMap.getString("lblFog.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblFog, gridBagConstraints);

        lblFogDesc.setText(PlanetaryConditions.getFogDisplayableName(scenario.getFog()));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblFogDesc, gridBagConstraints);

        lblTemperature.setText(resourceMap.getString("lblTemperature.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        pnlMap.add(lblTemperature, gridBagConstraints);
        lblTemperatureDesc.setText(PlanetaryConditions.getTemperatureDisplayableName(scenario.getTemperature()));
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = y++;
        pnlMap.add(lblTemperatureDesc, gridBagConstraints);

        if(scenario.getGravity() != 1.0) {
            lblGravity.setText(resourceMap.getString("lblGravity.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            pnlMap.add(lblGravity, gridBagConstraints);
            lblGravityDesc.setText(DecimalFormat.getInstance().format(scenario.getGravity()));
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            pnlMap.add(lblGravityDesc, gridBagConstraints);
        }


        if(scenario.getAtmosphere() != PlanetaryConditions.ATMO_STANDARD) {
            lblAtmosphere.setText(resourceMap.getString("lblAtmosphere.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            pnlMap.add(lblAtmosphere, gridBagConstraints);
            lblAtmosphereDesc.setText(PlanetaryConditions.getAtmosphereDisplayableName(scenario.getAtmosphere()));
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            pnlMap.add(lblAtmosphereDesc, gridBagConstraints);
        }

        String otherConditions = "";
        if(scenario.usesEMI()) {
            otherConditions = resourceMap.getString("emi.text");
        }
        if(scenario.usesBlowingSand()) {
            if(otherConditions.isEmpty()) {
                otherConditions = resourceMap.getString("sand.text");
            } else {
                otherConditions = otherConditions + ", " + resourceMap.getString("sand.text");
            }
        }
        if(!otherConditions.isEmpty()) {
            lblOtherConditions.setText(resourceMap.getString("lblOtherConditions.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            pnlMap.add(lblOtherConditions, gridBagConstraints);
            lblOtherConditionsDesc.setText(otherConditions);
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y++;
            pnlMap.add(lblOtherConditionsDesc, gridBagConstraints);
        }

        return y;
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
