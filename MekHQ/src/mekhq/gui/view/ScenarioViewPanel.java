/*
 * AtBScenarioViewPanel.java
 *
 * Copyright (C) 2016-2021 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.force.UnitStub;
import mekhq.campaign.mission.BotForceStub;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.gui.baseComponents.JScrollablePanel;
import mekhq.gui.utilities.MarkdownRenderer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

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

    private JPanel pnlInfo;
    private JPanel pnlLoot;
    private JPanel pnlMap;
    private JPanel pnlObjectives;
    private JPanel pnlDeployment;
    private JTextPane txtDesc;

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

        GridBagConstraints gridBagConstraints = new GridBagConstraints();

        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;

        setLayout(new GridBagLayout());

        fillBasicInfo();
        add(pnlInfo, gridBagConstraints);
        gridBagConstraints.gridy++;


        if (null != scenario.getReport() && !scenario.getReport().isEmpty()) {
            JTextPane txtReport = new JTextPane();
            txtReport.setName("txtReport");
            txtReport.setEditable(false);
            txtReport.setContentType("text/html");
            txtReport.setText(MarkdownRenderer.getRenderedHtml(scenario.getReport()));
            txtReport.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("After-Action Report"),
                    BorderFactory.createEmptyBorder(0,2,2,2)));
            add(txtReport, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        JTree forceTree = new JTree();
        forceTree.setModel(forceModel);
        forceTree.setCellRenderer(new ForceStubRenderer());
        forceTree.setRowHeight(50);
        forceTree.setRootVisible(false);
        gridBagConstraints.weighty = 1.0;
        add(forceTree, gridBagConstraints);
        gridBagConstraints.gridy++;

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
            gridBagConstraints.weighty = 1.0;
            add(tree, gridBagConstraints);
            gridBagConstraints.gridy++;
            if (scenario.getStatus().isCurrent()) {
                tree.addMouseListener(new ScenarioViewPanel.TreeMouseAdapter(tree, i));
            }
        }

        gridBagConstraints.weighty = 0.0;

        if(null != scenario.getDeploymentLimit()) {
            fillDeployment();
            add(pnlDeployment, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        if(!scenario.getScenarioObjectives().isEmpty()) {
            fillObjectives();
            add(pnlObjectives, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        if (scenario.getLoot().size() > 0) {
            fillLoot();
            add(pnlLoot, gridBagConstraints);
            gridBagConstraints.gridy++;
        }

        if(null != scenario.getMap()) {
            fillMapData();
            add(pnlMap, gridBagConstraints);
            gridBagConstraints.gridy++;
        }
    }

    private void fillBasicInfo() {

        pnlInfo = new JPanel(new BorderLayout());
        pnlInfo.setName("pnlStats");
        pnlInfo.setBorder(BorderFactory.createTitledBorder(scenario.getName()));

        JLabel lblStatus = new JLabel("<html><b>" + scenario.getStatus() + "</b></html>");
        lblStatus.setToolTipText(scenario.getStatus().getToolTipText());

        pnlInfo.add(lblStatus, BorderLayout.PAGE_START);

        if (null != scenario.getDescription() && !scenario.getDescription().isEmpty()) {
            txtDesc = new JTextPane();
            txtDesc.setName("txtDesc");
            txtDesc.setEditable(false);
            txtDesc.setContentType("text/html");
            txtDesc.setText(MarkdownRenderer.getRenderedHtml(scenario.getDescription()));
            pnlInfo.add(txtDesc, BorderLayout.CENTER);
        }
    }

    private void fillLoot() {
        pnlLoot = new JPanel();
        pnlLoot.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlLoot.title")));
        pnlLoot.setLayout(new BoxLayout(pnlLoot, BoxLayout.PAGE_AXIS));
        for (Loot loot : scenario.getLoot()) {
            pnlLoot.add(new JLabel(loot.getShortDescription()));
        }
    }

    private void fillDeployment() {

        pnlDeployment = new JPanel(new GridBagLayout());
        pnlDeployment.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlDeployment.title")));

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.gridy = 0;
        leftGbc.gridwidth = 1;
        leftGbc.weightx = 0.0;
        leftGbc.weighty = 0.0;
        leftGbc.insets = new java.awt.Insets(0, 0, 5, 10);
        leftGbc.fill = GridBagConstraints.NONE;
        leftGbc.anchor = GridBagConstraints.NORTHWEST;

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 1;
        rightGbc.gridy = 0;
        rightGbc.gridwidth = 1;
        rightGbc.weightx = 1.0;
        rightGbc.weighty = 0.0;
        rightGbc.insets = new java.awt.Insets(0, 10, 5, 0);
        rightGbc.fill = GridBagConstraints.NONE;
        rightGbc.anchor = GridBagConstraints.NORTHWEST;

        JLabel lblAllowedUnits = new JLabel(resourceMap.getString("lblAllowedUnits.text"));
        leftGbc.gridy++;
        pnlDeployment.add(lblAllowedUnits, leftGbc);

        JLabel lblAllowedUnitsDesc = new JLabel(scenario.getDeploymentLimit().getAllowedUnitTypeDesc());
        rightGbc.gridy++;
        pnlDeployment.add(lblAllowedUnitsDesc, rightGbc);

        JLabel lblQuantityLimit = new JLabel(resourceMap.getString("lblQuantityLimit.text"));
        leftGbc.gridy++;
        pnlDeployment.add(lblQuantityLimit, leftGbc);

        JLabel lblQuantityLimitDesc = new JLabel(scenario.getDeploymentLimit().getQuantityLimitDesc(scenario, campaign));
        rightGbc.gridy++;
        pnlDeployment.add(lblQuantityLimitDesc, rightGbc);

        String reqPersonnel = scenario.getDeploymentLimit().getRequiredPersonnelDesc(campaign);
        if(!reqPersonnel.isEmpty()) {
            JLabel lblRequiredPersonnel = new JLabel(resourceMap.getString("lblRequiredPersonnel.text"));
            leftGbc.gridy++;
            pnlDeployment.add(lblRequiredPersonnel, leftGbc);

            JLabel lblRequiredPersonnelDesc = new JLabel(reqPersonnel);
            rightGbc.gridy++;
            pnlDeployment.add(lblRequiredPersonnelDesc, rightGbc);
        }

        String reqUnits = scenario.getDeploymentLimit().getRequiredUnitDesc(campaign);
        if(!reqUnits.isEmpty()) {
            JLabel lblRequiredUnits = new JLabel(resourceMap.getString("lblRequiredUnits.text"));
            leftGbc.gridy++;
            pnlDeployment.add(lblRequiredUnits, leftGbc);

            JLabel lblRequiredUnitsDesc = new JLabel(reqUnits);
            rightGbc.gridy++;
            pnlDeployment.add(lblRequiredUnitsDesc, rightGbc);
        }

    }

    private void fillObjectives() {

        pnlObjectives = new JPanel(new BorderLayout());
        pnlObjectives.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlObjectives.title")));

        StringBuilder objectiveBuilder = new StringBuilder();

        for (ScenarioObjective objective : scenario.getScenarioObjectives()) {
            objectiveBuilder.append("### ");
            objectiveBuilder.append(objective.getDescription());
            objectiveBuilder.append("  \n");

            for (String forceName : objective.getAssociatedForceNames()) {
                objectiveBuilder.append("* ");
                objectiveBuilder.append(forceName);
                objectiveBuilder.append("  \n");
            }

            for (String associatedUnitID : objective.getAssociatedUnitIDs()) {
                String associatedUnitName = "";
                UUID uid = UUID.fromString(associatedUnitID);

                // "logic": try to get a hold of the unit with the given UUID,
                // either from the list of bot units or from the list of player units
                if (scenario.getExternalIDLookup().containsKey(associatedUnitID)) {
                    associatedUnitName = scenario.getExternalIDLookup().get(associatedUnitID).getShortName();
                } else if (scenario.getForces(campaign).getAllUnits(true).contains(uid)) {
                    associatedUnitName = campaign.getUnit(uid).getEntity().getShortName();
                }

                if (associatedUnitName.length() == 0) {
                    continue;
                }
                objectiveBuilder.append("* ");
                objectiveBuilder.append(associatedUnitName);
                objectiveBuilder.append("  \n");
            }

            if(!objective.getTimeLimitString().isEmpty()) {
                objectiveBuilder.append("> *Time Limits*: ");
                objectiveBuilder.append(objective.getTimeLimitString());
                objectiveBuilder.append("  \n");
            }

            if(!objective.getDetails().isEmpty()) {
                objectiveBuilder.append("> *Details*:");
                for (String detail : objective.getDetails()) {
                    objectiveBuilder.append(" ");
                    objectiveBuilder.append(detail);
                }
                objectiveBuilder.append("  \n");
            }

            objectiveBuilder.append("  \n");
        }

        JTextPane txtObjectives = new JTextPane();
        txtObjectives.setEditable(false);
        txtObjectives.setContentType("text/html");
        txtObjectives.setText(MarkdownRenderer.getRenderedHtml(objectiveBuilder.toString()));

        pnlObjectives.add(txtObjectives, BorderLayout.CENTER);
    }

    private void fillMapData() {

        pnlMap = new JPanel(new java.awt.GridBagLayout());
        pnlMap.setBorder(BorderFactory.createTitledBorder(resourceMap.getString("pnlMap.title")));

        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0;
        leftGbc.gridy = 0;
        leftGbc.gridwidth = 1;
        leftGbc.weightx = 0.0;
        leftGbc.weighty = 0.0;
        leftGbc.insets = new java.awt.Insets(0, 0, 5, 10);
        leftGbc.fill = GridBagConstraints.NONE;
        leftGbc.anchor = GridBagConstraints.NORTHWEST;

        GridBagConstraints rightGbc = new GridBagConstraints();
        rightGbc.gridx = 1;
        rightGbc.gridy = 0;
        rightGbc.gridwidth = 1;
        rightGbc.weightx = 1.0;
        rightGbc.weighty = 0.0;
        rightGbc.insets = new java.awt.Insets(0, 10, 5, 0);
        rightGbc.fill = GridBagConstraints.NONE;
        rightGbc.anchor = GridBagConstraints.NORTHWEST;

        JLabel lblTerrain = new JLabel(resourceMap.getString("lblTerrain.text"));
        leftGbc.gridy++;
        pnlMap.add(lblTerrain, leftGbc);

        JLabel lblTerrainDesc = new  JLabel();
        if (scenario.getTerrainType() == Scenario.TER_SPACE) {
            lblTerrainDesc.setText("Space");
        } else if (scenario.getTerrainType() == Scenario.TER_LOW_ATMO) {
            lblTerrainDesc.setText("Low Atmosphere");
        } else {
            lblTerrainDesc.setText("Ground");
        }
        rightGbc.gridy++;
        pnlMap.add(lblTerrainDesc, rightGbc);

        if (scenario.getTerrainType() != Scenario.TER_SPACE) {
            JLabel lblMap = new JLabel(resourceMap.getString("lblMap.text"));
            leftGbc.gridy++;
            pnlMap.add(lblMap, leftGbc);

            JLabel lblMapDesc = new JLabel(scenario.getMapForDisplay());
            rightGbc.gridy++;
            pnlMap.add(lblMapDesc, rightGbc);
        }

        JLabel lblMapSize = new JLabel(resourceMap.getString("lblMapSize.text"));
        leftGbc.gridy++;
        pnlMap.add(lblMapSize, leftGbc);

        JLabel lblMapSizeDesc = new JLabel(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        rightGbc.gridy++;
        pnlMap.add(lblMapSizeDesc, rightGbc);

        if (scenario.getTerrainType() == Scenario.TER_SPACE) {
            // if a space scenario return here as the rest is all planet based information
            return;
        }

        JLabel lblLight = new JLabel(resourceMap.getString("lblLight.text"));
        leftGbc.gridy++;
        pnlMap.add(lblLight, leftGbc);

        JLabel lblLightDesc = new JLabel(PlanetaryConditions.getLightDisplayableName(scenario.getLight()));
        rightGbc.gridy++;
        pnlMap.add(lblLightDesc, rightGbc);

        JLabel lblWeather = new JLabel(resourceMap.getString("lblWeather.text"));
        leftGbc.gridy++;
        pnlMap.add(lblWeather, leftGbc);

        JLabel lblWeatherDesc = new JLabel(PlanetaryConditions.getWeatherDisplayableName(scenario.getWeather()));
        rightGbc.gridy++;
        pnlMap.add(lblWeatherDesc, rightGbc);

        JLabel lblWind = new JLabel(resourceMap.getString("lblWind.text"));
        leftGbc.gridy++;
        pnlMap.add(lblWind, leftGbc);

        JLabel lblWindDesc = new JLabel(PlanetaryConditions.getWindDisplayableName(scenario.getWind()));
        rightGbc.gridy++;
        pnlMap.add(lblWindDesc, rightGbc);

        JLabel lblFog = new JLabel(resourceMap.getString("lblFog.text"));
        leftGbc.gridy++;
        pnlMap.add(lblFog, leftGbc);

        JLabel lblFogDesc = new JLabel(PlanetaryConditions.getFogDisplayableName(scenario.getFog()));
        rightGbc.gridy++;
        pnlMap.add(lblFogDesc, rightGbc);

        JLabel lblTemperature = new JLabel(resourceMap.getString("lblTemperature.text"));
        leftGbc.gridy++;
        pnlMap.add(lblTemperature, leftGbc);

        JLabel lblTemperatureDesc = new JLabel(PlanetaryConditions.getTemperatureDisplayableName(scenario.getTemperature()));
        rightGbc.gridy++;
        pnlMap.add(lblTemperatureDesc, rightGbc);

        if(scenario.getGravity() != 1.0) {
            JLabel lblGravity = new JLabel(resourceMap.getString("lblGravity.text"));
            leftGbc.gridy++;
            pnlMap.add(lblGravity, leftGbc);

            JLabel lblGravityDesc = new JLabel(DecimalFormat.getInstance().format(scenario.getGravity()));
            rightGbc.gridy++;
            pnlMap.add(lblGravityDesc, rightGbc);
        }


        if(scenario.getAtmosphere() != PlanetaryConditions.ATMO_STANDARD) {
            JLabel lblAtmosphere = new JLabel(resourceMap.getString("lblAtmosphere.text"));
            leftGbc.gridy++;
            pnlMap.add(lblAtmosphere, leftGbc);

             JLabel lblAtmosphereDesc = new JLabel(PlanetaryConditions.getAtmosphereDisplayableName(scenario.getAtmosphere()));
            rightGbc.gridy++;
            pnlMap.add(lblAtmosphereDesc, rightGbc);
        }

        ArrayList<String> otherConditions = new ArrayList<String>();
        if(scenario.usesEMI()) {
            otherConditions.add(resourceMap.getString("emi.text"));
        }
        if(scenario.usesBlowingSand()) {
            otherConditions.add(resourceMap.getString("sand.text"));
        }
        if(!otherConditions.isEmpty()) {
            JLabel lblOtherConditions = new JLabel(resourceMap.getString("lblOtherConditions.text"));
            leftGbc.gridy++;
            pnlMap.add(lblOtherConditions, leftGbc);

            JLabel lblOtherConditionsDesc = new JLabel(String.join(", ", otherConditions));
            rightGbc.gridy++;
            pnlMap.add(lblOtherConditionsDesc, rightGbc);
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
            setIcon(getIcon(value));
            return this;
        }

        protected @Nullable Icon getIcon(final Object node) {
            if (node instanceof UnitStub) {
                return ((UnitStub) node).getPortrait().getImageIcon(50);
            } else if (node instanceof ForceStub) {
                return ((ForceStub) node).getForceIcon().getImageIcon(50);
            } else {
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
