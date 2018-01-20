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

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;

import megamek.common.event.Subscribe;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.JumpPath;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.universe.Planet;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.view.JumpPathViewPanel;
import mekhq.gui.view.PlanetViewPanel;

/**
 * Displays interstellar map and contains transit controls.
 *
 */
public final class MapTab extends CampaignGuiTab implements ActionListener {

    private static final long serialVersionUID = 31953140144022679L;

    private JPanel panMapView;
    InterstellarMapPanel panMap;
    private JSplitPane splitMap;
    private JScrollPane scrollPlanetView;
    JSuggestField suggestPlanet;

    MapTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
        MekHQ.registerHandler(this);
    }

    @Override
    public GuiTabType tabType() {
        return GuiTabType.MAP;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI", //$NON-NLS-1$ ;
                new EncodeControl());
        GridBagConstraints gridBagConstraints;

        panMapView = new JPanel(new GridBagLayout());

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panMapView.add(new JLabel(resourceMap.getString("lblFindPlanet.text")), //$NON-NLS-1$ ;
                gridBagConstraints);

        suggestPlanet = new JSuggestField(getFrame(), getCampaign().getPlanetNames());
        suggestPlanet.addActionListener(ev -> {
            Planet p = getCampaign().getPlanet(suggestPlanet.getText());
            if (null != p) {
                panMap.setSelectedPlanet(p);
                refreshPlanetView();
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panMapView.add(suggestPlanet, gridBagConstraints);

        JButton btnCalculateJumpPath = new JButton(resourceMap.getString("btnCalculateJumpPath.text")); // NOI18N
        btnCalculateJumpPath.setToolTipText(resourceMap.getString("btnCalculateJumpPath.toolTipText")); // NOI18N
        btnCalculateJumpPath.addActionListener(ev -> calculateJumpPath());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panMapView.add(btnCalculateJumpPath, gridBagConstraints);

        JButton btnBeginTransit = new JButton(resourceMap.getString("btnBeginTransit.text")); // NOI18N
        btnBeginTransit.setToolTipText(resourceMap.getString("btnBeginTransit.toolTipText")); // NOI18N
        btnBeginTransit.addActionListener(ev -> beginTransit());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panMapView.add(btnBeginTransit, gridBagConstraints);

        panMap = new InterstellarMapPanel(getCampaign(), getCampaignGui());
        // lets go ahead and zoom in on the current location
        panMap.setSelectedPlanet(getCampaign().getLocation().getCurrentPlanet());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panMapView.add(panMap, gridBagConstraints);

        scrollPlanetView = new JScrollPane();
        scrollPlanetView.setMinimumSize(new java.awt.Dimension(400, 600));
        scrollPlanetView.setPreferredSize(new java.awt.Dimension(400, 600));
        scrollPlanetView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPlanetView.setViewportView(null);
        splitMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panMapView, scrollPlanetView);
        splitMap.setOneTouchExpandable(true);
        splitMap.setResizeWeight(1.0);
        splitMap.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> refreshPlanetView());

        panMap.setCampaign(getCampaign());
        panMap.addActionListener(this);

        setLayout(new BorderLayout());
        add(splitMap, BorderLayout.CENTER);
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#refreshAll()
     */
    @Override
    public void refreshAll() {
        refreshPlanetView();
    }

    private void calculateJumpPath() {
        if (null != panMap.getSelectedPlanet()) {
            panMap.setJumpPath(
                    getCampaign().calculateJumpPath(getCampaign().getCurrentPlanet(), panMap.getSelectedPlanet()));
            refreshPlanetView();
        }
    }

    private void beginTransit() {
        if (panMap.getJumpPath().isEmpty()) {
            return;
        }
        getCampaign().getLocation().setJumpPath(panMap.getJumpPath());
        refreshPlanetView();
        getCampaignGui().refreshLocation();
        panMap.setJumpPath(new JumpPath());
        panMap.repaint();
    }

    protected void refreshPlanetView() {
        JumpPath path = panMap.getJumpPath();
        if (null != path && !path.isEmpty()) {
            scrollPlanetView.setViewportView(new JumpPathViewPanel(path, getCampaign()));
            return;
        }
        Planet planet = panMap.getSelectedPlanet();
        if (null != planet) {
            scrollPlanetView.setViewportView(new PlanetViewPanel(planet, getCampaign()));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == panMap) {
            refreshPlanetView();
        }
    }

    @Subscribe
    public void handle(NewDayEvent ev) {
        panMap.repaint();
        suggestPlanet.setSuggestData(getCampaign().getPlanetNames());
    }

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        panMap.repaint();
    }
}
