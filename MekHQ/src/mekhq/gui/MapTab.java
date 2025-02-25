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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.JumpPath;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.event.OptionsChangedEvent;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.view.JumpPathViewPanel;
import mekhq.gui.view.PlanetViewPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Displays interstellar map and contains transit controls.
 */
public final class MapTab extends CampaignGuiTab implements ActionListener {
    private JViewport mapView;
    private JPanel panMapView;
    private InterstellarMapPanel panMap;
    private PlanetarySystemMapPanel panSystem;
    private JScrollPane scrollPlanetView;
    JSuggestField suggestPlanet;

    //region Constructors
    public MapTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
        MekHQ.registerHandler(this);
    }
    //endregion Constructors

    @Override
    public MHQTabType tabType() {
        return MHQTabType.INTERSTELLAR_MAP;
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        final ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
                MekHQ.getMHQOptions().getLocale());

        panMapView = new JPanel(new BorderLayout());

        JPanel panTopButtons = new JPanel(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 0.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panTopButtons.add(new JLabel(resourceMap.getString("lblFindPlanet.text")),
                gridBagConstraints);

        suggestPlanet = new JSuggestField(getFrame(), getCampaign().getSystemNames());
        suggestPlanet.addActionListener(ev -> {
            PlanetarySystem p = getCampaign().getSystemByName(suggestPlanet.getText());
            if (null != p) {
                panMap.setSelectedSystem(p);
                refreshPlanetView();
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        panTopButtons.add(suggestPlanet, gridBagConstraints);

        JButton btnCalculateJumpPath = new JButton(resourceMap.getString("btnCalculateJumpPath.text"));
        btnCalculateJumpPath.setToolTipText(resourceMap.getString("btnCalculateJumpPath.toolTipText"));
        btnCalculateJumpPath.addActionListener(ev -> calculateJumpPath());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panTopButtons.add(btnCalculateJumpPath, gridBagConstraints);

        JButton btnBeginTransit = new JButton(resourceMap.getString("btnBeginTransit.text"));
        btnBeginTransit.setToolTipText(resourceMap.getString("btnBeginTransit.toolTipText"));
        btnBeginTransit.addActionListener(ev -> beginTransit());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panTopButtons.add(btnBeginTransit, gridBagConstraints);

        panMapView.add(panTopButtons, BorderLayout.PAGE_START);

        //the actual map
        panMap = new InterstellarMapPanel(getCampaign(), getCampaignGui());
        // let's go ahead and zoom in on the current location
        panMap.setSelectedSystem(getCampaign().getLocation().getCurrentSystem());
        panMapView.add(panMap, BorderLayout.CENTER);

        mapView = new JViewport();
        mapView.setMinimumSize(new Dimension(600,600));
        mapView.setView(panMapView);

        scrollPlanetView = new JScrollPaneWithSpeed();
        scrollPlanetView.setMinimumSize(new Dimension(400, 600));
        scrollPlanetView.setPreferredSize(new Dimension(400, 600));
        scrollPlanetView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPlanetView.setViewportView(null);
        JSplitPane splitMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapView, scrollPlanetView);
        splitMap.setOneTouchExpandable(true);
        splitMap.setResizeWeight(1.0);
        splitMap.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, ev -> refreshPlanetView());

        panMap.setCampaign(getCampaign());
        panMap.addActionListener(this);

        panSystem = new PlanetarySystemMapPanel(getCampaign(), getCampaignGui());
        panSystem.addActionListener(this);

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
        refreshSystemView();
    }

    private void calculateJumpPath() {
        if (null != panMap.getSelectedSystem()) {
            panMap.setJumpPath(
                    getCampaign().calculateJumpPath(getCampaign().getCurrentSystem(), panMap.getSelectedSystem()));
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

        getCampaign().getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));
    }

    private void refreshSystemView() {
        JumpPath path = panMap.getJumpPath();
        if (null != path && !path.isEmpty()) {
            scrollPlanetView.setViewportView(new JumpPathViewPanel(path, getCampaign()));
            SwingUtilities.invokeLater(() -> scrollPlanetView.getVerticalScrollBar().setValue(0));
            return;
        }
        PlanetarySystem system = panMap.getSelectedSystem();
        if (null != system) {
            scrollPlanetView.setViewportView(new PlanetViewPanel(system, getCampaign()));
            SwingUtilities.invokeLater(() -> scrollPlanetView.getVerticalScrollBar().setValue(0));
        }
    }

    private void refreshPlanetView() {
        JumpPath path = panMap.getJumpPath();
        if (null != path && !path.isEmpty()) {
            scrollPlanetView.setViewportView(new JumpPathViewPanel(path, getCampaign()));
            SwingUtilities.invokeLater(() -> scrollPlanetView.getVerticalScrollBar().setValue(0));
            return;
        }
        int pos = panSystem.getSelectedPlanetPosition();
        PlanetarySystem system = panMap.getSelectedSystem();
        if (null != system) {
            scrollPlanetView.setViewportView(new PlanetViewPanel(system, getCampaign(), pos));
            SwingUtilities.invokeLater(() -> scrollPlanetView.getVerticalScrollBar().setValue(0));
        }
    }

    /**
     * Switch to the planetary system view, highlighting
     * a specific {@link Planet}
     * @param p The {@link Planet} to select.
     */
    public void switchPlanetaryMap(Planet p) {
        PlanetarySystem s = p.getParentSystem();
        panMap.setSelectedSystem(s);
        panSystem.updatePlanetarySystem(p);
        mapView.setView(panSystem);
        refreshPlanetView();
    }

    /**
     * Switches to the planetary system view, highlighting
     * a specific {@link PlanetarySystem}.
     * @param s The {@link PlanetarySystem} to select.
     */
    public void switchPlanetaryMap(PlanetarySystem s) {
        panMap.setSelectedSystem(s);
        panSystem.updatePlanetarySystem(s);
        mapView.setView(panSystem);
        refreshPlanetView();
    }

    /**
     * Switches to the interstellar map view, highlighting
     * a specific {@link PlanetarySystem}.
     * @param s The {@link PlanetarySystem} to select.
     */
    public void switchSystemsMap(PlanetarySystem s) {
        panMap.setSelectedSystem(s);
        panSystem.updatePlanetarySystem(s);
        switchSystemsMap();
    }

    public void switchSystemsMap() {
        mapView.setView(panMapView);
        refreshSystemView();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (Objects.equals(e.getSource(), panMap)) {
            refreshSystemView();
        } else if (Objects.equals(e.getSource(), panSystem)) {
            refreshPlanetView();
        }
    }

    @Subscribe
    public void handle(NewDayEvent ev) {
        panMap.repaint();
        suggestPlanet.setSuggestData(getCampaign().getSystemNames());
    }

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        panMap.repaint();
    }
}
