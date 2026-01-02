/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import static megamek.common.options.OptionsConstants.BASE_BLIND_DROP;
import static megamek.common.options.OptionsConstants.BASE_REAL_BLIND_DROP;
import static megamek.common.units.Entity.getEntityMajorTypeName;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import megamek.client.ui.dialogs.UnitEditorDialog;
import megamek.client.ui.dialogs.buttonDialogs.BotConfigDialog;
import megamek.common.annotations.Nullable;
import megamek.common.interfaces.IStartingPositions;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.PlanetaryConditions;
import megamek.common.units.Entity;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.ForceStub;
import mekhq.campaign.force.UnitStub;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.BotForceStub;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioObjective;
import mekhq.gui.baseComponents.JScrollablePanel;

/**
 * @author Neoancient
 */
public class AtBScenarioViewPanel extends JScrollablePanel {
    private final AtBScenario scenario;
    private final Campaign campaign;
    private final List<String> attachedAllyStub;
    private List<BotForceStub> botStubs;
    private final JFrame frame;

    private JPanel panStats;

    private final JLabel lblStatusDesc = new JLabel();
    private final JLabel lblType = new JLabel();
    private final JLabel lblTypeDesc = new JLabel();
    private final JLabel lblForce = new JLabel();
    private final JLabel lblForceDesc = new JLabel();
    private final JLabel lblTerrain = new JLabel();
    private final JLabel lblTerrainDesc = new JLabel();
    private final JLabel lblMap = new JLabel();
    private final JLabel lblMapDesc = new JLabel();
    private final JLabel lblMapSize = new JLabel();
    private final JLabel lblMapSizeDesc = new JLabel();
    private final JLabel lblLight = new JLabel();
    private final JLabel lblLightDesc = new JLabel();
    private final JLabel lblWeather = new JLabel();
    private final JLabel lblWeatherDesc = new JLabel();
    private final JLabel lblWind = new JLabel();
    private final JLabel lblWindDesc = new JLabel();
    private final JLabel lblFog = new JLabel();
    private final JLabel lblFogDesc = new JLabel();
    private final JLabel lblBlowingSand = new JLabel();
    private final JLabel lblBlowingSandDesc = new JLabel();
    private final JLabel lblEMI = new JLabel();
    private final JLabel lblEMIDesc = new JLabel();

    private final JLabel lblTemp = new JLabel();

    private final JLabel lblTempDesc = new JLabel();
    private final JLabel lblAtmosphere = new JLabel();
    private final JLabel lblAtmosphereDesc = new JLabel();
    private final JLabel lblGravity = new JLabel();
    private final JLabel lblGravityDesc = new JLabel();
    private final JLabel lblPlayerStart = new JLabel();
    private final JLabel lblPlayerStartPos = new JLabel();

    private final JTextArea txtDetails = new JTextArea();

    private final static int REROLL_TERRAIN = 0;
    private final static int REROLL_MAP = 1;
    private final static int REROLL_MAP_SIZE = 2;
    private final static int REROLL_LIGHT = 3;
    private final static int REROLL_WEATHER = 4;
    private final static int REROLL_NUM = 5;
    private final JCheckBox[] chkReroll = new JCheckBox[REROLL_NUM];
    private JButton btnReroll;

    private JTree playerForceTree;

    private JTextArea txtDesc;

    private final StubTreeModel playerForceModel;

    public AtBScenarioViewPanel(AtBScenario s, Campaign c, JFrame frame) {
        super();
        this.frame = frame;
        this.scenario = s;
        this.campaign = c;
        botStubs = new ArrayList<>();

        ForceStub playerForces;
        if (s.getStatus().isCurrent()) {
            s.refresh(c);
            playerForces = new ForceStub(s.getForces(campaign), campaign);
            attachedAllyStub = Utilities.generateEntityStub(s.getAlliesPlayer());
            for (int i = 0; i < s.getNumBots(); i++) {
                botStubs.add(s.getBotForce(i).generateStub(campaign));
            }
        } else {
            playerForces = s.getForceStub();
            attachedAllyStub = s.getAlliesPlayerStub();
            botStubs = s.getBotForcesStubs();
        }
        playerForceModel = new StubTreeModel(playerForces);
        initComponents();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        panStats = new JPanel();
        txtDesc = new JTextArea();
        JTextArea txtReport = new JTextArea();
        playerForceTree = new JTree();

        setLayout(new GridBagLayout());

        setTracksViewportWidth(false);

        int y = 0;

        panStats.setName("pnlStats");
        panStats.setBorder(BorderFactory.createTitledBorder(scenario.getName()));
        fillStats();
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y++;
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(panStats, gridBagConstraints);

        txtReport.setName("txtReport");
        txtReport.setText(scenario.getReport());
        txtReport.setEditable(false);
        txtReport.setLineWrap(true);
        txtReport.setWrapStyleWord(true);
        txtReport.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("After-Action Report"),
              BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(txtReport, gridBagConstraints);
    }

    private void fillStats() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.ScenarioViewPanel",
              MekHQ.getMHQOptions().getLocale());
        JLabel lblStatus = new JLabel();

        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        panStats.setLayout(new GridBagLayout());

        int y = 0;

        lblStatus.setName("lblStatus");
        lblStatus.setText(resourceMap.getString("lblStatus.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.fill = GridBagConstraints.NONE;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panStats.add(lblStatus, gridBagConstraints);

        lblStatusDesc.setName("lblOwner");
        lblStatusDesc.setText(scenario.getStatus().toString());
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
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panStats.add(playerForceTree, gridBagConstraints);

        if (!attachedAllyStub.isEmpty()) {
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
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panStats.add(tree, gridBagConstraints);
        }

        boolean isBlindDrop = campaign.getGameOptions().getOption(BASE_BLIND_DROP).booleanValue();
        boolean isTrueBlindDrop = campaign.getGameOptions().getOption(BASE_REAL_BLIND_DROP).booleanValue();
        boolean isCurrent = scenario.getStatus().isCurrent();
        for (int i = 0; i < botStubs.size(); i++) {
            BotForceStub botStub = botStubs.get(i);
            if (botStub == null) {
                continue;
            }

            int team = botStub.team();
            List<String> allEntries = botStub.entityList();
            DefaultMutableTreeNode top = new DefaultMutableTreeNode(botStubs.get(i).name());

            if (!(isTrueBlindDrop && (team != 1))) {
                boolean hideInformation = isCurrent && isBlindDrop && (team != 1);
                for (String entityString : allEntries) {
                    if (hideInformation) {
                        int unitIndex = allEntries.indexOf(entityString);
                        Entity entity = scenario.getBotForce(i).getFullEntityList(campaign).get(unitIndex);

                        if (entity == null) {
                            String label = "???";
                            top.add(new DefaultMutableTreeNode(label));
                            continue;
                        }

                        String weightClass = entity.getWeightClassName();
                        long entityType = entity.getEntityType();
                        String unitType = getEntityMajorTypeName(entityType);

                        String label = weightClass + ' ' + unitType;
                        top.add(new DefaultMutableTreeNode(label));
                    } else {
                        top.add(new DefaultMutableTreeNode(entityString));
                    }
                }
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
            gridBagConstraints.insets = new Insets(5, 5, 5, 5);
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panStats.add(tree, gridBagConstraints);
            if (scenario.getStatus().isCurrent()) {
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

        if (scenario.getStatus().isCurrent()) {
            lblForce.setText(resourceMap.getString("lblForce.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(lblForce, gridBagConstraints);

            if (null != scenario.getCombatTeamById(campaign)) {
                Formation formation = campaign.getForce(scenario.getCombatTeamId());

                if (formation != null) {
                    lblForceDesc.setText(campaign.getForce(scenario.getCombatTeamId()).getFullName());
                } else {
                    lblForceDesc.setText("Unknown Force ID: " + scenario.getCombatTeamId());
                }
            } else if (scenario instanceof AtBDynamicScenario) {
                StringBuilder forceBuilder = new StringBuilder();
                forceBuilder.append("<html>");
                boolean chop = false;
                for (int forceID : scenario.getForceIDs()) {
                    forceBuilder.append(campaign.getForce(forceID).getFullName());
                    forceBuilder.append("<br/>");
                    ScenarioForceTemplate template = ((AtBDynamicScenario) scenario).getPlayerForceTemplates()
                                                           .get(forceID);
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

        if (scenario.getBoardType() == Scenario.T_SPACE) {
            y = fillSpaceStats(gridBagConstraints, resourceMap, y);
        } else if (scenario.getBoardType() == Scenario.T_ATMOSPHERE) {
            y = fillLowAtmosphereStats(gridBagConstraints, resourceMap, y);
        } else {
            y = fillPlanetSideStats(gridBagConstraints, resourceMap, y);
        }

        if (!(scenario instanceof AtBDynamicScenario)) {
            lblPlayerStart.setText(resourceMap.getString("lblPlayerStart.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(lblPlayerStart, gridBagConstraints);

            lblPlayerStartPos.setText(IStartingPositions.START_LOCATION_NAMES[scenario.getStartingPos()]);
            gridBagConstraints.gridx = 2;
            gridBagConstraints.gridy = y++;
            panStats.add(lblPlayerStartPos, gridBagConstraints);
        }

        if (scenario.getStatus().isCurrent()) {
            btnReroll = new JButton(scenario.getRerollsRemaining() +
                                          " Reroll" +
                                          ((scenario.getRerollsRemaining() == 1) ? "" : "s") +
                                          " Remaining");
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y++;
            gridBagConstraints.gridwidth = 1;
            panStats.add(btnReroll, gridBagConstraints);
            btnReroll.setEnabled(scenario.getRerollsRemaining() > 0);
            btnReroll.addActionListener(evt -> rerollBattleConditions());
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
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panStats.add(txtDesc, gridBagConstraints);

        StringBuilder objectiveBuilder = new StringBuilder();
        objectiveBuilder.append(scenario.getDeploymentInstructions());

        for (ScenarioObjective objective : scenario.getScenarioObjectives()) {
            objectiveBuilder.append(objective.getDescription());
            objectiveBuilder.append('\n');

            for (String forceName : objective.getAssociatedForceNames()) {
                objectiveBuilder.append('\t');
                objectiveBuilder.append(forceName);
                objectiveBuilder.append('\n');
            }

            for (String associatedUnitID : objective.getAssociatedUnitIDs()) {
                String associatedUnitName = "";
                UUID uid = UUID.fromString(associatedUnitID);

                // "logic": try to get a hold of the unit with the given UUID,
                // either from the list of bot units or from the list of player units
                if (scenario.getExternalIDLookup().containsKey(associatedUnitID)) {
                    associatedUnitName = scenario.getExternalIDLookup().get(associatedUnitID).getShortName();
                } else if (scenario.getForces(campaign).getAllUnits(false).contains(uid)) {
                    associatedUnitName = campaign.getUnit(uid).getEntity().getShortName();
                }

                if (associatedUnitName.isBlank()) {
                    continue;
                }
                objectiveBuilder.append('\t');
                objectiveBuilder.append(associatedUnitName);
                objectiveBuilder.append('\n');
            }

            objectiveBuilder.append('\t');
            objectiveBuilder.append(objective.getTimeLimitString());
            objectiveBuilder.append('\n');

            for (String detail : objective.getDetails()) {
                objectiveBuilder.append('\t');
                objectiveBuilder.append(detail);
                objectiveBuilder.append('\n');
            }

            objectiveBuilder.append('\n');
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
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panStats.add(txtDetails, gridBagConstraints);

        if (!scenario.getLoot().isEmpty()) {
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.weightx = 1.0;
            gridBagConstraints.weighty = 0.0;
            gridBagConstraints.insets = new Insets(0, 0, 5, 0);
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
            panStats.add(new JLabel("<html><b>Scenario Costs or Loot:</b></html>"), gridBagConstraints);

            for (Loot loot : scenario.getLoot()) {
                gridBagConstraints.gridx = 0;
                gridBagConstraints.gridy++;
                gridBagConstraints.gridwidth = 2;
                gridBagConstraints.weightx = 1.0;
                gridBagConstraints.weighty = 0.0;
                gridBagConstraints.insets = new Insets(0, 10, 5, 0);
                gridBagConstraints.fill = GridBagConstraints.NONE;
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
                panStats.add(new JLabel(loot.getShortDescription()), gridBagConstraints);
            }
        }
    }

    /**
     * Worker function that generates UI elements appropriate for planet-side scenarios
     *
     * @param gridBagConstraints Current grid bag constraints in use
     * @param resourceMap        Text resource
     * @param y                  current row in the parent UI element
     *
     * @return the row at which we wind up after doing all this
     */
    private int fillPlanetSideStats(GridBagConstraints gridBagConstraints, ResourceBundle resourceMap, int y) {
        if (scenario.getScenarioType() != AtBScenario.DYNAMIC) {
            chkReroll[REROLL_TERRAIN] = new JCheckBox();
            lblTerrain.setText(resourceMap.getString("lblTerrain.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(lblTerrain, gridBagConstraints);

            if (scenario.getStatus().isCurrent()) {
                gridBagConstraints.gridx = 1;
                gridBagConstraints.gridy = y;
                gridBagConstraints.gridwidth = 1;
                panStats.add(chkReroll[REROLL_TERRAIN], gridBagConstraints);
                chkReroll[REROLL_TERRAIN].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollTerrain());
                chkReroll[REROLL_TERRAIN].addItemListener(checkBoxListener);
            }

            lblTerrainDesc.setText(scenario.getTerrainType());
        } else {
            lblTerrain.setText(resourceMap.getString("lblTerrain.text"));
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(lblTerrain, gridBagConstraints);

            String hasTrack = scenario.getHasTrack() ? " \u2606" : "";
            lblTerrainDesc.setText(scenario.getTerrainType() + hasTrack);
        }

        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblTerrainDesc, gridBagConstraints);

        lblMap.setText(resourceMap.getString("lblMap.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblMap, gridBagConstraints);

        chkReroll[REROLL_MAP] = new JCheckBox();
        if (scenario.getStatus().isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAP], gridBagConstraints);
            chkReroll[REROLL_MAP].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMap());
            chkReroll[REROLL_MAP].addItemListener(checkBoxListener);
        }

        lblMapDesc.setText(scenario.getMapForDisplay());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapDesc, gridBagConstraints);

        lblMapSize.setText(resourceMap.getString("lblMapSize.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblMapSize, gridBagConstraints);

        chkReroll[REROLL_MAP_SIZE] = new JCheckBox();
        if (scenario.getStatus().isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAP_SIZE], gridBagConstraints);
            chkReroll[REROLL_MAP_SIZE].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMapSize());
            chkReroll[REROLL_MAP_SIZE].addItemListener(checkBoxListener);
        }

        lblMapSizeDesc.setText(scenario.getMapX() + " W x " + scenario.getMapY() + " H");
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapSizeDesc, gridBagConstraints);

        lblLight.setText(resourceMap.getString("lblLight.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblLight, gridBagConstraints);

        chkReroll[REROLL_LIGHT] = new JCheckBox();
        if (scenario.getStatus().isCurrent() && campaign.getCampaignOptions().isUseLightConditions()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_LIGHT], gridBagConstraints);
            chkReroll[REROLL_LIGHT].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollLight());
            chkReroll[REROLL_LIGHT].addItemListener(checkBoxListener);
        }

        lblLightDesc.setText(scenario.getLight().toString());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblLightDesc, gridBagConstraints);
        lblLight.setVisible(campaign.getCampaignOptions().isUseLightConditions());
        lblLightDesc.setVisible(campaign.getCampaignOptions().isUseLightConditions());

        lblWeather.setText(resourceMap.getString("lblWeather.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblWeather, gridBagConstraints);

        chkReroll[REROLL_WEATHER] = new JCheckBox();
        if (scenario.getStatus().isCurrent() && campaign.getCampaignOptions().isUseWeatherConditions()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_WEATHER], gridBagConstraints);
            chkReroll[REROLL_WEATHER].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollWeather());
            chkReroll[REROLL_WEATHER].addItemListener(checkBoxListener);
        }

        lblWeatherDesc.setText(scenario.getWeather().toString());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblWeatherDesc, gridBagConstraints);
        lblWeather.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());
        lblWeatherDesc.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());

        lblWind.setText(resourceMap.getString("lblWind.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblWind, gridBagConstraints);

        lblWindDesc.setText(scenario.getWind().toString());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblWindDesc, gridBagConstraints);
        lblWind.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());
        lblWindDesc.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());

        lblFog.setText(resourceMap.getString("lblFog.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblFog, gridBagConstraints);

        lblFogDesc.setText(scenario.getFog().toString());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblFogDesc, gridBagConstraints);
        lblFog.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());
        lblFogDesc.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());

        lblBlowingSand.setText(resourceMap.getString("lblBlowingSand.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblBlowingSand, gridBagConstraints);

        String blowingSandDesc = scenario.getBlowingSand().toString();
        lblBlowingSandDesc.setText(blowingSandDesc);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblBlowingSandDesc, gridBagConstraints);
        lblBlowingSand.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());
        lblBlowingSandDesc.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());

        lblEMI.setText(resourceMap.getString("lblEMI.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblEMI, gridBagConstraints);

        String emiDesc = scenario.getEMI().toString();
        lblEMIDesc.setText(emiDesc);
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblEMIDesc, gridBagConstraints);
        lblEMI.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());
        lblEMIDesc.setVisible(campaign.getCampaignOptions().isUseWeatherConditions());

        lblTemp.setText(resourceMap.getString("lblTemperature.text"));
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = y;
        gridBagConstraints.gridwidth = 1;
        panStats.add(lblTemp, gridBagConstraints);

        lblTempDesc.setText(PlanetaryConditions.getTemperatureDisplayableName(scenario.getModifiedTemperature()));
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblTempDesc, gridBagConstraints);
        lblTemp.setVisible(campaign.getCampaignOptions().isUsePlanetaryConditions());
        lblTempDesc.setVisible(campaign.getCampaignOptions().isUsePlanetaryConditions());

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

        lblAtmosphereDesc.setText(scenario.getAtmosphere().toString());
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblAtmosphereDesc, gridBagConstraints);
        lblAtmosphere.setVisible(scenario.getAtmosphere() != Atmosphere.STANDARD);
        lblAtmosphereDesc.setVisible(scenario.getAtmosphere() != Atmosphere.STANDARD);

        return y;
    }

    /**
     * Worker function that generates UI elements appropriate for space scenarios
     *
     * @param gridBagConstraints Current grid bag constraints in use
     * @param resourceMap        Text resource
     * @param y                  current row in the parent UI element
     *
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

        chkReroll[REROLL_MAP_SIZE] = new JCheckBox();
        if (scenario.getStatus().isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAP_SIZE], gridBagConstraints);
            chkReroll[REROLL_MAP_SIZE].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMapSize());
            chkReroll[REROLL_MAP_SIZE].addItemListener(checkBoxListener);
        }

        lblMapSizeDesc.setText(scenario.getMapSizeX() + " W x " + scenario.getMapSizeY() + " H");
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapSizeDesc, gridBagConstraints);

        return y;
    }

    /**
     * Worker function that generates UI elements appropriate for low atmosphere scenarios
     *
     * @param gridBagConstraints Current grid bag constraints in use
     * @param resourceMap        Text resource
     * @param y                  current row in the parent UI element
     *
     * @return the row at which we wind up after doing all this
     */
    private int fillLowAtmosphereStats(GridBagConstraints gridBagConstraints, ResourceBundle resourceMap, int y) {
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

        chkReroll[REROLL_MAP_SIZE] = new JCheckBox();
        if (scenario.getStatus().isCurrent()) {
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.gridwidth = 1;
            panStats.add(chkReroll[REROLL_MAP_SIZE], gridBagConstraints);
            chkReroll[REROLL_MAP_SIZE].setVisible(scenario.getRerollsRemaining() > 0 && scenario.canRerollMapSize());
            chkReroll[REROLL_MAP_SIZE].addItemListener(checkBoxListener);
        }

        lblMapSizeDesc.setText(scenario.getMapSizeX() + " W x " + scenario.getMapSizeY() + " H");
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = y++;
        panStats.add(lblMapSizeDesc, gridBagConstraints);

        return y;
    }

    private final ItemListener checkBoxListener = e -> countRerollBoxes();

    private void countRerollBoxes() {
        int checkedBoxes = 0;
        for (int i = 0; i < REROLL_NUM; i++) {
            if ((chkReroll[i] != null) && chkReroll[i].isSelected()) {
                checkedBoxes++;
            }
        }

        /* Once the number of checked boxes hits the number of rerolls
         * remaining, any boxes that aren't checked need to be disabled.
         * If the number falls below that, all are re-enabled.
         */
        for (int i = 0; i < REROLL_NUM; i++) {
            if (chkReroll[i] != null) {
                chkReroll[i].setEnabled(checkedBoxes < scenario.getRerollsRemaining() || chkReroll[i].isSelected());
            }
        }
    }

    private void rerollBattleConditions() {
        if (chkReroll[REROLL_TERRAIN] != null && chkReroll[REROLL_TERRAIN].isSelected()) {
            scenario.setTerrain();
            scenario.setScenarioMap(campaign.getCampaignOptions().getFixedMapChance());
            scenario.useReroll();
            chkReroll[REROLL_TERRAIN].setSelected(false);
            lblTerrainDesc.setText(scenario.getTerrainType());
            lblMapDesc.setText(scenario.getMapForDisplay());
            lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        }
        if (chkReroll[REROLL_MAP] != null && chkReroll[REROLL_MAP].isSelected()) {
            scenario.setScenarioMap(campaign.getCampaignOptions().getFixedMapChance());
            scenario.useReroll();
            chkReroll[REROLL_MAP].setSelected(false);
            lblMapDesc.setText(scenario.getMapForDisplay());
            lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        }
        if (chkReroll[REROLL_MAP_SIZE] != null && chkReroll[REROLL_MAP_SIZE].isSelected()) {
            scenario.setMapSize(campaign);
            scenario.setScenarioMap(campaign.getCampaignOptions().getFixedMapChance());
            scenario.useReroll();
            chkReroll[REROLL_MAP_SIZE].setSelected(false);
            lblMapDesc.setText(scenario.getMapForDisplay());
            lblMapSizeDesc.setText(scenario.getMapSizeX() + "x" + scenario.getMapSizeY());
        }
        if (chkReroll[REROLL_LIGHT] != null && chkReroll[REROLL_LIGHT].isSelected()) {
            scenario.setLightConditions();
            scenario.useReroll();
            chkReroll[REROLL_LIGHT].setSelected(false);
            lblLightDesc.setText(scenario.getLight().toString());
        }
        if (chkReroll[REROLL_WEATHER] != null && chkReroll[REROLL_WEATHER].isSelected()) {
            scenario.setWeatherConditions(campaign.getCampaignOptions().isUseNoTornadoes());
            scenario.useReroll();
            chkReroll[REROLL_WEATHER].setSelected(false);
            lblWeatherDesc.setText(scenario.getWeather().toString());
            lblWindDesc.setText(scenario.getWind().toString());
            lblFogDesc.setText(scenario.getFog().toString());
            lblTempDesc.setText(PlanetaryConditions.getTemperatureDisplayableName(scenario.getModifiedTemperature()));
            String blowingSandDesc = scenario.getBlowingSand().toString();
            lblBlowingSandDesc.setText(blowingSandDesc);
            String emiDesc = scenario.getEMI().toString();
            lblEMIDesc.setText(emiDesc);
        }
        btnReroll.setText(scenario.getRerollsRemaining() +
                                " Reroll" +
                                ((scenario.getRerollsRemaining() == 1) ? "" : "s") +
                                " Remaining");
        btnReroll.setEnabled(scenario.getRerollsRemaining() > 0);
        countRerollBoxes();
    }

    protected static class StubTreeModel implements TreeModel {
        private final ForceStub rootForce;
        private final Vector<TreeModelListener> listeners = new Vector<>();

        public StubTreeModel(ForceStub root) {
            rootForce = root;
        }

        @Override
        public @Nullable Object getChild(final @Nullable Object parent, final int index) {
            return (parent instanceof ForceStub) ? ((ForceStub) parent).getAllChildren().get(index) : null;
        }

        @Override
        public int getChildCount(final @Nullable Object parent) {
            return (parent instanceof ForceStub) ? ((ForceStub) parent).getAllChildren().size() : 0;
        }

        @Override
        public int getIndexOfChild(final @Nullable Object parent, final @Nullable Object child) {
            return (parent instanceof ForceStub) ? ((ForceStub) parent).getAllChildren().indexOf(child) : 0;
        }

        @Override
        public Object getRoot() {
            return rootForce;
        }

        @Override
        public boolean isLeaf(final @Nullable Object node) {
            return (node instanceof UnitStub) ||
                         ((node instanceof ForceStub) && ((ForceStub) node).getAllChildren().isEmpty());
        }

        @Override
        public void valueForPathChanged(TreePath arg0, Object arg1) {

        }

        @Override
        public void addTreeModelListener(final @Nullable TreeModelListener listener) {
            if ((listener != null) && !listeners.contains(listener)) {
                listeners.addElement(listener);
            }
        }

        @Override
        public void removeTreeModelListener(final @Nullable TreeModelListener listener) {
            if (listener != null) {
                listeners.removeElement(listener);
            }
        }
    }

    protected static class ForceStubRenderer extends DefaultTreeCellRenderer {
        public ForceStubRenderer() {

        }

        @Override
        public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
              final boolean expanded, final boolean leaf, final int row,
              final boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            setIcon(getIcon(value));
            return this;
        }

        protected @Nullable Icon getIcon(final @Nullable Object node) {
            if (node instanceof UnitStub) {
                return ((UnitStub) node).getPortrait().getImageIcon(50);
            } else if (node instanceof ForceStub) {
                return ((ForceStub) node).getFormationIcon().getImageIcon(58);
            } else {
                return null;
            }
        }
    }

    private class TreeMouseAdapter extends MouseInputAdapter implements ActionListener {
        private final JTree tree;
        int forceIndex;

        public TreeMouseAdapter(JTree tree, int forceIndex) {
            this.tree = tree;
            this.forceIndex = forceIndex;
        }

        @Override
        public void actionPerformed(ActionEvent action) {
            String command = action.getActionCommand();

            if (command.equalsIgnoreCase("CONFIG_BOT")) {
                BotConfigDialog pbd = new BotConfigDialog(frame,
                      null,
                      scenario.getBotForce(forceIndex).getBehaviorSettings(),
                      null);
                pbd.setBotName(scenario.getBotForce(forceIndex).getName());
                pbd.setVisible(true);
                if (!pbd.getResult().isCancelled()) {
                    scenario.getBotForce(forceIndex).setBehaviorSettings(pbd.getBehaviorSettings());
                    scenario.getBotForce(forceIndex).setName(pbd.getBotName());
                }
            } else if (command.equalsIgnoreCase("EDIT_UNIT")) {
                if ((tree.getSelectionCount() > 0) && (tree.getSelectionRows() != null)) {
                    int unitIndex = tree.getSelectionRows()[0] - 1;
                    UnitEditorDialog editorDialog = new UnitEditorDialog(frame,
                          scenario.getBotForce(this.forceIndex).getFullEntityList(campaign).get(unitIndex));
                    editorDialog.setVisible(true);
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
            final boolean isBlindDrop = campaign.getGameOptions().getOption(BASE_BLIND_DROP).booleanValue();
            final JPopupMenu popup = new JPopupMenu();
            if (e.isPopupTrigger()) {
                final TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }

                JMenuItem menuItem;
                if ((path.getPathCount() > 1) &&
                          (tree.getSelectionRows() != null) &&
                          (tree.getSelectionRows()[0] != 0) &&
                          !isBlindDrop) {
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
