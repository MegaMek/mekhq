/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import static java.lang.Math.ceil;
import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.randomEvents.prisoners.RecoverMIAPersonnel.abandonMissingPersonnel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Objects;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import megamek.client.ui.util.UIUtil;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.JumpPath;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.view.JumpPathViewPanel;
import mekhq.gui.view.PlanetViewPanel;

/**
 * Displays interstellar map and contains transit controls.
 */
public final class MapTab extends CampaignGuiTab implements ActionListener {
    private static final int PADDING = UIUtil.scaleForGUI(10);

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
        panTopButtons.add(new JLabel(resourceMap.getString("lblFindPlanet.text")), gridBagConstraints);

        suggestPlanet = new JSuggestField(getFrame(), getCampaign().getSystemNames());
        suggestPlanet.addActionListener(ev -> {
            PlanetarySystem p = getCampaign().getSystemByName(suggestPlanet.getText());
            if (null != p) {
                panMap.setSelectedSystem(p);
                panSystem.updatePlanetarySystem(p);
                refreshPlanetView();
            }
        });
        suggestPlanet.setBorder(BorderFactory.createLineBorder(suggestPlanet.getBackground(), 1));
        suggestPlanet.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                suggestPlanet.setBorder(BorderFactory.createLineBorder(suggestPlanet.getBackground(), 1));
            }

            @Override
            public void focusLost(FocusEvent e) {
                suggestPlanet.setBorder(BorderFactory.createLineBorder(suggestPlanet.getBackground(), 1));
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 0, PADDING);
        panTopButtons.add(suggestPlanet, gridBagConstraints);

        RoundedJButton btnCalculateJumpPath = new RoundedJButton(resourceMap.getString("btnCalculateJumpPath.text"));
        btnCalculateJumpPath.setToolTipText(resourceMap.getString("btnCalculateJumpPath.toolTipText"));
        btnCalculateJumpPath.addActionListener(ev -> calculateJumpPath());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 0, PADDING);
        panTopButtons.add(btnCalculateJumpPath, gridBagConstraints);

        RoundedJButton btnBeginTransit = new RoundedJButton(resourceMap.getString("btnBeginTransit.text"));
        btnBeginTransit.setToolTipText(resourceMap.getString("btnBeginTransit.toolTipText"));
        btnBeginTransit.addActionListener(ev -> beginTransit());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        gridBagConstraints.insets = new Insets(0, 0, 0, PADDING);
        panTopButtons.add(btnBeginTransit, gridBagConstraints);

        JCheckBox chkAvoidAbandonedSystems = new JCheckBox(resourceMap.getString("chkAvoidAbandonedSystems.text"));
        chkAvoidAbandonedSystems.setToolTipText(wordWrap(resourceMap.getString("chkAvoidAbandonedSystems.toolTipText")));
        chkAvoidAbandonedSystems.addActionListener(ev -> getCampaign().setIsAvoidingEmptySystems(
              chkAvoidAbandonedSystems.isSelected()));
        chkAvoidAbandonedSystems.setSelected(getCampaign().isAvoidingEmptySystems());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panTopButtons.add(chkAvoidAbandonedSystems, gridBagConstraints);

        JCheckBox chkUseCommandCircuits = new JCheckBox(resourceMap.getString("chkUseCommandCircuits.text"));
        chkUseCommandCircuits.setToolTipText(wordWrap(resourceMap.getString("chkUseCommandCircuits.toolTipText")));
        chkUseCommandCircuits.addActionListener(ev -> getCampaign().setIsOverridingCommandCircuitRequirements(
              chkUseCommandCircuits.isSelected()));
        chkUseCommandCircuits.setSelected(getCampaign().isOverridingCommandCircuitRequirements());
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 0.0;
        panTopButtons.add(chkUseCommandCircuits, gridBagConstraints);

        panMapView.add(panTopButtons, BorderLayout.PAGE_START);

        //the actual map
        panMap = new InterstellarMapPanel(getCampaign(), getCampaignGui());
        // let's go ahead and zoom in on the current location
        panMap.setSelectedSystem(getCampaign().getLocation().getCurrentSystem());
        panMapView.add(panMap, BorderLayout.CENTER);

        JPanel pnlTutorial = new TutorialHyperlinkPanel("mapTab");
        panMapView.add(pnlTutorial, BorderLayout.SOUTH);

        mapView = new JViewport();
        mapView.setMinimumSize(new Dimension(600, 600));
        mapView.setView(panMapView);

        scrollPlanetView = new JScrollPaneWithSpeed();
        scrollPlanetView.setBorder(null);
        scrollPlanetView.setMinimumSize(new Dimension(400, 600));
        scrollPlanetView.setPreferredSize(new Dimension(400, 600));
        scrollPlanetView.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPlanetView.setViewportView(null);
        scrollPlanetView.setBorder(null);
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
            panMap.setJumpPath(getCampaign().calculateJumpPath(getCampaign().getCurrentSystem(),
                  panMap.getSelectedSystem(), false, false));
            refreshPlanetView();
        }
    }

    private void beginTransit() {
        if (panMap.getJumpPath().isEmpty()) {
            return;
        }

        ImmersiveDialogConfirmation dialog = new ImmersiveDialogConfirmation(getCampaign());
        if (!dialog.wasConfirmed()) {
            return;
        }

        JumpPath jumpPath = panMap.getJumpPath();


        boolean isUseCommandCircuits = getCampaign().isUseCommandCircuit();
        int duration = (int) ceil(jumpPath.getTotalTime(getCampaign().getLocalDate(),
              getCampaign().getLocation().getTransitTime(), isUseCommandCircuits));

        TransportCostCalculations transportCostCalculations = getCampaign().getTransportCostCalculation(EXP_REGULAR);
        Money journeyCost = transportCostCalculations.calculateJumpCostForEntireJourney(duration, jumpPath.getJumps());

        String jumpReport = TransportCostCalculations.performJumpTransaction(getCampaign().getFinances(), jumpPath,
              getCampaign().getLocalDate(), journeyCost, getCampaign().getCurrentSystem());

        if (!jumpReport.isBlank()) {
            getCampaign().addReport(jumpReport);
        }

        getCampaign().getLocation().setJumpPath(panMap.getJumpPath());

        refreshPlanetView();
        getCampaignGui().refreshLocation();

        panMap.setJumpPath(new JumpPath());
        panMap.repaint();

        getCampaign().getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));

        abandonMissingPersonnel(getCampaign());

        NewPersonnelMarket personnelMarket = getCampaign().getNewPersonnelMarket();
        if (personnelMarket.getAssociatedPersonnelMarketStyle() == MEKHQ) {
            personnelMarket.clearCurrentApplicants();
        }
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
     * Switch to the planetary system view, highlighting a specific {@link Planet}
     *
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
     * Switches to the planetary system view, highlighting a specific {@link PlanetarySystem}.
     *
     * @param s The {@link PlanetarySystem} to select.
     */
    public void switchPlanetaryMap(PlanetarySystem s) {
        panMap.setSelectedSystem(s);
        panSystem.updatePlanetarySystem(s);
        mapView.setView(panSystem);
        refreshPlanetView();
    }

    /**
     * Switches to the interstellar map view, highlighting a specific {@link PlanetarySystem}.
     *
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
