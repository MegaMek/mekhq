/*
 * AtBScenarioViewPanel.java
 *
 * Copyright (C) 2014-2020 - The MegaMek Team. All Rights Reserved.
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import megamek.client.ui.swing.UnitEditorDialog;
import megamek.common.IStartingPositions;
import megamek.common.PlanetaryConditions;
import megamek.common.util.EncodeControl;
import mekhq.MHQStaticDirectoryManager;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.force.UnitStub;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForceStub;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.gui.dialog.PrincessBehaviorDialog;

/**
 * @author Neoancient
 */
public class AtBScenarioViewPanel extends ScrollablePanel {
    private static final long serialVersionUID = -3104784717190158181L;

    private AtBScenario scenario;
    private Campaign campaign;
    private ForceStub playerForces;
    private List<String> attachedAllyStub;
    private List<BotForceStub> botStubs;
    private JFrame frame;

    private JPanel panStats;

    private JLabel lblStatus = new JLabel();
    private JLabel lblStatusDesc = new JLabel();
    private JLabel lblType = new JLabel();
    private JLabel lblTypeDesc = new JLabel();
    private JLabel lblForce = new JLabel();
    private JLabel lblForceDesc = new JLabel();
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
    private JLabel lblGravity = new JLabel();
    private JLabel lblGravityDesc = new JLabel();
    private JLabel lblPlayerStart = new JLabel();
    private JLabel lblPlayerStartPos = new JLabel();

    private JTextArea txtDetails = new JTextArea();

    private final static int REROLL_TERRAIN = 0;
    private final static int REROLL_MAP = 1;
    private final static int REROLL_MAPSIZE = 2;
    private final static int REROLL_LIGHT = 3;
    private final static int REROLL_WEATHER = 4;
    private final static int REROLL_NUM = 5;
    private JCheckBox[] chkReroll = new JCheckBox[REROLL_NUM];
    private JButton btnReroll;

    private JTree playerForceTree;

    private JTextArea txtDesc;
    private JTextArea txtReport;

    private StubTreeModel playerForceModel;

    public AtBScenarioViewPanel(AtBScenario s, Campaign c, JFrame frame) {
        this.frame = frame;
        this.scenario = s;
        this.campaign = c;
        botStubs = new ArrayList<>();

        if (s.isCurrent()) {
            s.refresh(c);
            this.playerForces = new ForceStub(s.getForces(campaign), campaign);
            attachedAllyStub = s.generateEntityStub(s.getAlliesPlayer());
            for (int i = 0; i < s.getNumBots(); i++) {
                botStubs.add(s.generateBotStub(s.getBotForce(i)));
            }
        } else {
            this.playerForces = s.getForceStub();
            attachedAllyStub = s.getAlliesPlayerStub();
            botStubs = s.getBotForceStubs();
        }
        playerForceModel = new StubTreeModel(playerForces);
        initComponents();
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        panStats = new JPanel();
        txtDesc = new JTextArea();
        txtReport = new JTextArea();
        playerForceTree = new JTree();

        setLayout(new GridBagLayout());

        setScrollableTracksViewportWidth(false);

        int y = 0;

        panStats.setName("pnlStats");
        panStats.setBorder(BorderFactory.createTitledBorder(scenario.getName()));
        fillStats();
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(panStats, gridBagConstraints);

        txtReport.setName("txtReport");
        txtReport.setText(scenario.getReport());
        txtReport.setEditable(false);
        txtReport.setLineWrap(true);
        txtReport.setWrapStyleWord(true);
        txtReport.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("After-Action Report"),
                BorderFactory.createEmptyBorder(5,5,5,5)));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(txtReport, gridBagConstraints);
    }

    private void fillStats() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.AtBScenarioViewPanel", new EncodeControl()); //$NON-NLS-1$
        lblStatus = new javax.swing.JLabel();

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        panStats.setLayout(new java.awt.GridBagLayout());

        int y = 0;

        lblStatus.setName("lblStatus"); // NOI18N
        lblStatus.setText(resourceMap.getString("lblStatus.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panStats.add(lblStatus, gridBagConstraints);

        lblStatusDesc.setName("lblOwner"); // NOI18N
        lblStatusDesc.setText(scenario.getStatusName());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblStatusDesc, gridBagConstraints);

        playerForceTree.setModel(playerForceModel);
        playerForceTree.setCellRenderer(new ForceStubRenderer());
        playerForceTree.setRowHeight(50);
        playerForceTree.setRootVisible(false);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panStats.add(playerForceTree, gridBagConstraints);

        if (attachedAllyStub.size() > 0) {
            DefaultMutableTreeNode top = new DefaultMutableTreeNode("Attached Allies");
            for (String en : attachedAllyStub) {
                top.add(new DefaultMutableTreeNode(en));
            }
            JTree tree = new JTree(top);
            tree.collapsePath(new TreePath(top));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.gridheight = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            panStats.add(tree, gridBagConstraints);
        }



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
            gridBagConstraints.gridwidth = 3;
            gridBagConstraints.gridheight = 1;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 1.0;
            gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
            gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            panStats.add(tree, gridBagConstraints);
            if (scenario.isCurrent()) {
                tree.addMouseListener(new TreeMouseAdapter(tree, i));
            }
        }

        gridBagConstraints = new GridBagConstraints();
        lblType.setText(resourceMap.getString("lblType.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panStats.add(lblType, gridBagConstraints);

        lblTypeDesc.setText(scenario.getDesc());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblTypeDesc, gridBagConstraints);

        if (scenario.isCurrent()) {
            lblForce.setText(resourceMap.getString("lblForce.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(lblForce, gridBagConstraints);

            if (null != scenario.getLance(campaign)) {
                lblForceDesc.setText(campaign.getForce(scenario.getLanceForceId()).getFullName());
            } else if (scenario instanceof AtBDynamicScenario) {
                StringBuilder forceBuilder = new StringBuilder();
                forceBuilder.append("<html>");
                boolean chop = false;
                for (int forceID : scenario.getForceIDs()) {
                    forceBuilder.append(campaign.getForce(forceID).getFullName());
                    forceBuilder.append("<br/>");
                    ScenarioForceTemplate template = ((AtBDynamicScenario) scenario).getPlayerForceTemplates().get(forceID);
                    if (template != null && template.getActualDeploymentZone() >= 0) {
                        forceBuilder.append("Deploy: ");
                        forceBuilder.append(IStartingPositions.START_LOCATION_NAMES[template.getActualDeploymentZone()]);
                        forceBuilder.append("<br/>");
                    }
                    chop = true;
                }

                if (chop) {
                    forceBuilder.delete(forceBuilder.length() - 5, forceBuilder.length());
                }
                forceBuilder.append("</html>");
                lblForceDesc.setText(forceBuilder.toString());
            }

            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 1;
            panStats.add(lblForceDesc, gridBagConstraints);
        }

        if(scenario.getTerrainType() == AtBScenario.TER_SPACE) {
            y = fillSpaceStats(gridBagConstraints, resourceMap, y);
        } else if (scenario.getTerrainType() == AtBScenario.TER_LOW_ATMO) {
            y = fillLowAtmoStats(gridBagConstraints, resourceMap, y);
        } else {
            y = fillPlanetSideStats(gridBagConstraints, resourceMap, y);
        }

        if(!(scenario instanceof AtBDynamicScenario)) {
            lblPlayerStart.setText(resourceMap.getString("lblPlayerStart.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(lblPlayerStart, gridBagConstraints);

            lblPlayerStartPos.setText(IStartingPositions.START_LOCATION_NAMES[scenario.getStart()]);
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = y++;
            panStats.add(lblPlayerStartPos, gridBagConstraints);
        }

        if (scenario.isCurrent()) {
            btnReroll = new JButton(scenario.getRerollsRemaining() +
                    " Reroll" + ((scenario.getRerollsRemaining() == 1)?"":"s") +
                    " Remaining");
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 1;
            panStats.add(btnReroll, gridBagConstraints);
            btnReroll.setEnabled(scenario.getRerollsRemaining() > 0);
            btnReroll.addActionListener(arg0 -> rerollBattleConditions());
        }

        txtDesc.setName("txtDesc");
        txtDesc.setText(scenario.getDescription());
        txtDesc.setEditable(false);
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panStats.add(txtDesc, gridBagConstraints);

        StringBuilder objectiveBuilder = new StringBuilder();
        objectiveBuilder.append(scenario.getDeploymentInstructions());

        for(ScenarioObjective objective : scenario.getScenarioObjectives()) {
            objectiveBuilder.append(objective.getDescription());
            objectiveBuilder.append("\n");

            for(String forceName : objective.getAssociatedForceNames()) {
                objectiveBuilder.append("\t");
                objectiveBuilder.append(forceName);
                objectiveBuilder.append("\n");
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

                if(associatedUnitName.length() == 0) {
                    continue;
                }
                objectiveBuilder.append("\t");
                objectiveBuilder.append(associatedUnitName);
                objectiveBuilder.append("\n");
            }

            for(String detail : objective.getDetails()) {
                objectiveBuilder.append("\t");
                objectiveBuilder.append(detail);
                objectiveBuilder.append("\n");
            }

            objectiveBuilder.append("\t");
            objectiveBuilder.append(objective.getTimeLimitString());
            objectiveBuilder.append("\n");

            objectiveBuilder.append("\n");
        }

        objectiveBuilder.append(scenario.getBattlefieldControlDescription());

        txtDetails.setText(objectiveBuilder.toString());
        txtDetails.setLineWrap(true);
        txtDetails.setWrapStyleWord(true);
        txtDetails.setEditable(false);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        panStats.add(txtDetails, gridBagConstraints);

        if(scenario.getLoot().size() > 0) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
            gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
            gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
            panStats.add(new JLabel("<html><b>Potential Rewards:</b></html>"), gridBagConstraints);

            for(Loot loot : scenario.getLoot()) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 0.0;
                gridBagConstraints.insets = new java.awt.Insets(0, 10, 5, 0);
                gridBagConstraints.fill = java.awt.GridBagConstraints.NONE;
                gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
                panStats.add(new JLabel(loot.getShortDescription()), gridBagConstraints);
            }
        }
    }

    /**
     * Worker function that generates UI elements appropriate for planet-side scenarios
     * @param gridBagConstraints Current grid bag constraints in use
     * @param resourceMap Text resource
     * @param y current row in the parent UI element
     * @return the row at which we wind up after doing all this
     */
    private int fillPlanetSideStats(GridBagConstraints gridBagConstraints, ResourceBundle resourceMap, int y) {
        chkReroll[REROLL_TERRAIN] = new JCheckBox();
        lblTerrain.setText(resourceMap.getString("lblTerrain.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblTerrain, gridBagConstraints);

        if (scenario.isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_TERRAIN], gridBagConstraints);
            chkReroll[REROLL_TERRAIN].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollTerrain());
            chkReroll[REROLL_TERRAIN].addItemListener(checkBoxListener);
        }

        lblTerrainDesc.setText(AtBScenario.terrainTypes[scenario.getTerrainType()]);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblTerrainDesc, gridBagConstraints);

        lblMap.setText(resourceMap.getString("lblMap.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblMap, gridBagConstraints);

        chkReroll[REROLL_MAP] = new JCheckBox();
        if (scenario.isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAP], gridBagConstraints);
            chkReroll[REROLL_MAP].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMap());
            chkReroll[REROLL_MAP].addItemListener(checkBoxListener);
        }

        lblMapDesc.setText(scenario.getMap());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapDesc, gridBagConstraints);

        lblMapSize.setText(resourceMap.getString("lblMapSize.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblMapSize, gridBagConstraints);

        chkReroll[REROLL_MAPSIZE] = new JCheckBox();
        if (scenario.isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAPSIZE], gridBagConstraints);
            chkReroll[REROLL_MAPSIZE].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMapSize());
            chkReroll[REROLL_MAPSIZE].addItemListener(checkBoxListener);
        }

        lblMapSizeDesc.setText(scenario.getMapX() + "x" + scenario.getMapY());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapSizeDesc, gridBagConstraints);

        lblLight.setText(resourceMap.getString("lblLight.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblLight, gridBagConstraints);

        chkReroll[REROLL_LIGHT] = new JCheckBox();
        if (scenario.isCurrent() && campaign.getCampaignOptions().getUseLightConditions()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_LIGHT], gridBagConstraints);
            chkReroll[REROLL_LIGHT].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollLight());
            chkReroll[REROLL_LIGHT].addItemListener(checkBoxListener);
        }

        lblLightDesc.setText(PlanetaryConditions.getLightDisplayableName(scenario.getLight()));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblLightDesc, gridBagConstraints);
        lblLight.setVisible(campaign.getCampaignOptions().getUseLightConditions());
        lblLightDesc.setVisible(campaign.getCampaignOptions().getUseLightConditions());

        lblWeather.setText(resourceMap.getString("lblWeather.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblWeather, gridBagConstraints);

        chkReroll[REROLL_WEATHER] = new JCheckBox();
        if (scenario.isCurrent() && campaign.getCampaignOptions().getUseWeatherConditions()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_WEATHER], gridBagConstraints);
            chkReroll[REROLL_WEATHER].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollWeather());
            chkReroll[REROLL_WEATHER].addItemListener(checkBoxListener);
        }

        lblWeatherDesc.setText(PlanetaryConditions.getWeatherDisplayableName(scenario.getWeather()));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblWeatherDesc, gridBagConstraints);
        lblWeather.setVisible(campaign.getCampaignOptions().getUseWeatherConditions());
        lblWeatherDesc.setVisible(campaign.getCampaignOptions().getUseWeatherConditions());

        lblWind.setText(resourceMap.getString("lblWind.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblWind, gridBagConstraints);

        lblWindDesc.setText(PlanetaryConditions.getWindDisplayableName(scenario.getWind()));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblWindDesc, gridBagConstraints);
        lblWind.setVisible(campaign.getCampaignOptions().getUseWeatherConditions());
        lblWindDesc.setVisible(campaign.getCampaignOptions().getUseWeatherConditions());

        lblFog.setText(resourceMap.getString("lblFog.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblFog, gridBagConstraints);

        lblFogDesc.setText(PlanetaryConditions.getFogDisplayableName(scenario.getFog()));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblFogDesc, gridBagConstraints);
        lblFog.setVisible(campaign.getCampaignOptions().getUseWeatherConditions());
        lblFogDesc.setVisible(campaign.getCampaignOptions().getUseWeatherConditions());

        lblGravity.setText(resourceMap.getString("lblGravity.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblGravity, gridBagConstraints);

        lblGravityDesc.setText(DecimalFormat.getInstance().format(scenario.getGravity()));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblGravityDesc, gridBagConstraints);
        lblGravity.setVisible(scenario.getGravity() != 1.0);
        lblGravityDesc.setVisible(scenario.getGravity() != 1.0);

        lblAtmosphere.setText(resourceMap.getString("lblAtmosphere.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblAtmosphere, gridBagConstraints);

        lblAtmosphereDesc.setText(PlanetaryConditions.getAtmosphereDisplayableName(scenario.getAtmosphere()));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblAtmosphereDesc, gridBagConstraints);
        lblAtmosphere.setVisible(scenario.getAtmosphere() != PlanetaryConditions.ATMO_STANDARD);
        lblAtmosphereDesc.setVisible(scenario.getAtmosphere() != PlanetaryConditions.ATMO_STANDARD);

        return y;
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
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblTerrain, gridBagConstraints);

        lblTerrainDesc.setText("Space");
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblTerrainDesc, gridBagConstraints);

        lblMapSize.setText(resourceMap.getString("lblMapSize.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblMapSize, gridBagConstraints);

        chkReroll[REROLL_MAPSIZE] = new JCheckBox();
        if (scenario.isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAPSIZE], gridBagConstraints);
            chkReroll[REROLL_MAPSIZE].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMapSize());
            chkReroll[REROLL_MAPSIZE].addItemListener(checkBoxListener);
        }

        lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapSizeDesc, gridBagConstraints);

        return y;
    }

    /**
     * Worker function that generates UI elements appropriate for low atmosphere scenarios
     * @param gridBagConstraints Current grid bag constraints in use
     * @param resourceMap Text resource
     * @param y current row in the parent UI element
     * @return the row at which we wind up after doing all this
     */
    private int fillLowAtmoStats(GridBagConstraints gridBagConstraints, ResourceBundle resourceMap, int y) {
        lblTerrain.setText(resourceMap.getString("lblTerrain.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblTerrain, gridBagConstraints);

        lblTerrainDesc.setText("Low Atmosphere");
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblTerrainDesc, gridBagConstraints);

        lblMapSize.setText(resourceMap.getString("lblMapSize.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblMapSize, gridBagConstraints);

        chkReroll[REROLL_MAPSIZE] = new JCheckBox();
        if (scenario.isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAPSIZE], gridBagConstraints);
            chkReroll[REROLL_MAPSIZE].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMapSize());
            chkReroll[REROLL_MAPSIZE].addItemListener(checkBoxListener);
        }

        lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapSizeDesc, gridBagConstraints);

        return y;
    }

    private ItemListener checkBoxListener = e -> countRerollBoxes();

    private void countRerollBoxes() {
        int checkedBoxes = 0;
        for (int i = 0; i < REROLL_NUM; i++) {
            if (chkReroll[i] != null && chkReroll[i].isSelected()) checkedBoxes++;
        }

        /* Once the number of checked boxes hits the number of rerolls
         * remaining, any boxes that aren't checked need to be disabled.
         * If the number falls below that, all are re-enabled.
         */
        for (int i = 0; i < REROLL_NUM; i++) {
            if(chkReroll[i] != null) {
                chkReroll[i].setEnabled(checkedBoxes < scenario.getRerollsRemaining() ||
                        chkReroll[i].isSelected());
            }
        }
    }

    private void rerollBattleConditions() {
        if (chkReroll[REROLL_TERRAIN] != null && chkReroll[REROLL_TERRAIN].isSelected()) {
            scenario.setTerrain();
            scenario.setMapFile();
            scenario.useReroll();
            chkReroll[REROLL_TERRAIN].setSelected(false);
            lblTerrainDesc.setText(AtBScenario.terrainTypes[scenario.getTerrainType()]);
            lblMapDesc.setText(scenario.getMap());
        }
        if (chkReroll[REROLL_MAP] != null && chkReroll[REROLL_MAP].isSelected()) {
            scenario.setMapFile();
            scenario.useReroll();
            chkReroll[REROLL_MAP].setSelected(false);
            lblMapDesc.setText(scenario.getMap());
        }
        if (chkReroll[REROLL_MAPSIZE] != null && chkReroll[REROLL_MAPSIZE].isSelected()) {
            scenario.setMapSize();
            scenario.useReroll();
            chkReroll[REROLL_MAPSIZE].setSelected(false);
            lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        }
        if (chkReroll[REROLL_LIGHT] != null && chkReroll[REROLL_LIGHT].isSelected()) {
            scenario.setLightConditions();
            scenario.useReroll();
            chkReroll[REROLL_LIGHT].setSelected(false);
            lblLightDesc.setText(PlanetaryConditions.getLightDisplayableName(scenario.getLight()));
        }
        if (chkReroll[REROLL_WEATHER] != null && chkReroll[REROLL_WEATHER].isSelected()) {
            scenario.setWeather();
            scenario.useReroll();
            chkReroll[REROLL_WEATHER].setSelected(false);
            lblWeatherDesc.setText(PlanetaryConditions.getWeatherDisplayableName(scenario.getWeather()));
            lblWindDesc.setText(PlanetaryConditions.getWindDisplayableName(scenario.getWind()));
            lblFogDesc.setText(PlanetaryConditions.getFogDisplayableName(scenario.getFog()));
        }
        btnReroll.setText(scenario.getRerollsRemaining() +
                " Reroll" + ((scenario.getRerollsRemaining() == 1)?"":"s") +
                " Remaining");
        btnReroll.setEnabled(scenario.getRerollsRemaining() > 0);
        countRerollBoxes();
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
                return ((ForceStub)parent).getAllChildren().get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof ForceStub) {
                return ((ForceStub)parent).getAllChildren().size();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof ForceStub) {
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
            //  Auto-generated method stub

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

    protected static class ForceStubRenderer extends DefaultTreeCellRenderer {
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
                MekHQ.getLogger().error(e);
                return null;
            }
       }
    }

    protected static class EntityListModel implements TreeModel {
        private ArrayList<String> root;
        private Vector<TreeModelListener> listeners = new Vector<>();
        private String name;

        public EntityListModel(ArrayList<String> root, String name) {
            this.root = root;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof ArrayList<?>) {
                return ((ArrayList<?>) parent).get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof ArrayList<?>) {
                return ((ArrayList<?>) parent).size();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof ArrayList<?>) {
                return ((ArrayList<?>) parent).indexOf(child);
            }
            return 0;
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof String;
        }

        @Override
        public void valueForPathChanged(TreePath arg0, Object arg1) {
            // Auto-generated method stub

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
                PrincessBehaviorDialog pbd = new PrincessBehaviorDialog(frame,
                        scenario.getBotForce(index).getBehaviorSettings(),
                        scenario.getBotForce(index).getName());
                pbd.setVisible(true);
                if (!pbd.dialogAborted) {
                    scenario.getBotForce(index).setBehaviorSettings(pbd.getBehaviorSettings());
                    scenario.getBotForce(index).setName(pbd.getBotName());
                }
            } else if (command.equalsIgnoreCase("EDIT_UNIT")) {
                if (tree.getSelectionCount() > 0) {
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
            JPopupMenu popup = new JPopupMenu();
            if (e.isPopupTrigger()) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
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
    };

}
