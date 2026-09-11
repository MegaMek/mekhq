/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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
import static mekhq.MHQConstants.CONFIRMATION_BEGIN_TRANSIT;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.campaign.mission.contract.utilities.ContractAutomation.outOfContractMothballAutomation;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.randomEvents.prisoners.RecoverMIAPersonnel.abandonMissingPersonnel;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import megamek.client.ui.util.UIUtil;
import megamek.common.event.Subscribe;
import megamek.common.ui.FastJScrollPane;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.JumpPath;
import mekhq.campaign.NavigationRouteAnalysis.PathAssessment;
import mekhq.campaign.NavigationRouteAnalysis.Severity;
import mekhq.campaign.RouteAlternativesPlanner.Course;
import mekhq.campaign.RouteAlternativesPlanner.PlanningResult;
import mekhq.campaign.RouteAlternativesPlanner.PlanningStatus;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.events.LocationAddedEvent;
import mekhq.campaign.events.LocationRemovedEvent;
import mekhq.campaign.events.NewDayEvent;
import mekhq.campaign.events.OptionsChangedEvent;
import mekhq.campaign.events.missions.MissionEvent;
import mekhq.campaign.events.scenarios.ScenarioEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.mission.utilities.TransportCostCalculations;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.utilities.JumpBlockers;
import mekhq.gui.RoutePlanningIntent.ChangeResult;
import mekhq.gui.baseComponents.FramedCommandButton;
import mekhq.gui.baseComponents.FramedCommandButtonStyle.ButtonColors;
import mekhq.gui.baseComponents.FramedCommandButtonStyle.ButtonStateColors;
import mekhq.gui.baseComponents.ImmersiveCheckBox;
import mekhq.gui.baseComponents.ImmersiveScrollBarStyle;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogConfirmation;
import mekhq.gui.dialog.JumpCostsSummary;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.panels.TutorialHyperlinkPanel;
import mekhq.gui.utilities.JSuggestField;
import mekhq.gui.view.JumpPathViewPanel;
import mekhq.gui.view.PlanetViewPanel;
import mekhq.utilities.MHQInternationalization;

/**
 * Displays interstellar map and contains transit controls.
 */
// FIXME: this class should not inherit from CampaignGuiTab because it is managed by NavigationTab now
public final class MapTab extends CampaignGuiTab implements ActionListener,
    InterstellarMapPanel.RoutePlanningHandler {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignGUI";
    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final Color ROUTE_STRIP_BACKGROUND = new Color(7, 16, 27);
    private static final Color ROUTE_STRIP_BORDER = new Color(35, 66, 82);
    private static final Color ROUTE_TEXT_COLOR = new Color(218, 231, 235);
    private static final Color ROUTE_MUTED_COLOR = new Color(132, 153, 161);
    private static final Color PLANNED_ROUTE_COLOR = new Color(65, 210, 224);
    private static final Color ACTIVE_ROUTE_COLOR = new Color(235, 166, 66);
    private static final Color HUD_CONTROL_BACKGROUND = new Color(15, 30, 43);
        private static final Color HUD_CONTROL_HOVER_BACKGROUND = new Color(18, 45, 56);
        private static final Color HUD_CONTROL_PRESSED_BACKGROUND = new Color(33, 73, 82);
        private static final ButtonColors NAVIGATION_BUTTON_COLORS = new ButtonColors(
            new ButtonStateColors(HUD_CONTROL_BACKGROUND, ROUTE_TEXT_COLOR, ROUTE_STRIP_BORDER),
            new ButtonStateColors(HUD_CONTROL_HOVER_BACKGROUND, PLANNED_ROUTE_COLOR, PLANNED_ROUTE_COLOR),
            new ButtonStateColors(HUD_CONTROL_PRESSED_BACKGROUND, ROUTE_TEXT_COLOR, PLANNED_ROUTE_COLOR),
            new ButtonStateColors(HUD_CONTROL_BACKGROUND.darker(), ROUTE_MUTED_COLOR, ROUTE_STRIP_BORDER.darker()));
    private static final int HUD_MINIMUM_HEIGHT = UIUtil.scaleForGUI(38);
        private static final int HUD_BUTTON_VERTICAL_MARGIN = UIUtil.scaleForGUI(4);
        private static final int HUD_BUTTON_HORIZONTAL_MARGIN = UIUtil.scaleForGUI(10);
    private static final int INSPECTOR_PREFERRED_WIDTH = UIUtil.scaleForGUI(426);
    private static final int INSPECTOR_TOGGLE_SIZE = UIUtil.scaleForGUI(28);
    private static final int INSPECTOR_RAIL_WIDTH = UIUtil.scaleForGUI(36);
    private static final int INSPECTOR_SCROLLBAR_WIDTH = UIUtil.scaleForGUI(10);
    private static final int ROUTE_TRANSITION_FRAME_DELAY_MS = 16;
    private static final long ROUTE_TRANSITION_DURATION_NS = 280_000_000L;
    private static final float ROUTE_TRANSITION_MIDPOINT = 0.5f;
    private static final float ROUTE_TRANSITION_MIN_ALPHA = 0.15f;
    private static final float ROUTE_REVEAL_SWEEP_END = 0.75f;
    private static final float ROUTE_REVEAL_MAX_ALPHA = 0.75f;

    private JViewport mapView;
    private JPanel panMapView;
    private InterstellarMapPanel panMap;
    private PlanetarySystemMapPanel panSystem;
    private JScrollPane systemView;
    private JScrollPane routeView;
    private JPanel contextWorkspace;
    private JPanel collapsedContextRail;
    private JSplitPane splitMap;
    private FramedCommandButton btnCalculateJumpPath;
    private FramedCommandButton btnQuickPlotCourse;
    private FramedCommandButton btnBeginTransit;
    private JLabel lblRouteStatus;
    private JLabel lblRouteDestination;
    private JLabel lblRouteJumps;
    private JLabel lblRouteDuration;
    private JLabel lblRouteNextJump;
    private JLabel lblRouteCost;
    private RouteStripPanel routeStrip;
    private JSuggestField suggestRouteOrigin;
    private JSuggestField suggestRouteDestination;
    private JPanel routeViewSelector;
    private JToggleButton activeRouteViewButton;
    private JToggleButton plannedRouteViewButton;
    private RouteViewMode routeViewMode = RouteViewMode.ACTIVE;
    private boolean advancedRoutePlanningExpanded;
    private Timer routeTransitionTimer;
    private RouteStripSnapshot presentedRouteSnapshot;
    private RouteStripSnapshot targetRouteSnapshot;
    private long routeTransitionStartTime;
    private float routeTransitionStartAlpha = 1.0f;
    private boolean routeTransitionMidpointApplied;
    private boolean routeRevealTransition;
    private DossierIdentity presentedDossierIdentity;
    private MapTabLayoutState layoutState;
    private transient RoutePlanningIntent routePlanningIntent;
    private int inspectorDividerSize;
    private int inspectorWidth = INSPECTOR_PREFERRED_WIDTH;
    private boolean applyingLayoutState;
    JSuggestField suggestPlanet;

    //region Constructors
    public MapTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }

    static MapTabLayoutState initializeLayoutState(MapTabLayoutState currentState) {
        return (currentState == null) ? new MapTabLayoutState() : currentState;
    }

    static RoutePlanningIntent initializeRoutePlanningIntent(RoutePlanningIntent currentIntent,
          PlanetarySystem defaultOrigin) {
        return (currentIntent == null) ? new RoutePlanningIntent(defaultOrigin) : currentIntent;
    }

        static boolean isWhatIfRoute(JumpPath proposedPath, PlanetarySystem campaignCurrentSystem) {
          return (proposedPath != null) && !proposedPath.isEmpty()
              && !Objects.equals(proposedPath.getFirstSystem(), campaignCurrentSystem);
        }

        static double getProposedRouteTransitProgress(JumpPath proposedPath, PlanetarySystem campaignCurrentSystem,
            double campaignTransitProgress) {
          return isWhatIfRoute(proposedPath, campaignCurrentSystem) ? 0.0 : campaignTransitProgress;
        }

                static boolean canBeginTransit(JumpPath proposedPath, PlanetarySystem campaignCurrentSystem,
                        JumpPath activePath, PathAssessment routeAssessment) {
          return (proposedPath != null) && !proposedPath.isEmpty() && (proposedPath.getJumps() > 0)
              && Objects.equals(proposedPath.getFirstSystem(), campaignCurrentSystem)
                            && ((activePath == null) || activePath.isEmpty()) && (routeAssessment != null)
                            && (routeAssessment.severity() != Severity.BLOCKED);
        }

                    static boolean canQuickPlotRoute(PlanetarySystem selectedSystem, PlanetarySystem campaignCurrentSystem) {
                  return (selectedSystem != null) && (campaignCurrentSystem != null)
                          && !Objects.equals(selectedSystem, campaignCurrentSystem);
                }

            static String quickPlotResourceKey(boolean hasProposedPath) {
                return hasProposedPath ? "routeStrip.replot.text" : "routeStrip.plot.text";
            }

    static void dispatchRoutePlanningFeedback(ChangeResult changeResult,
          Consumer<PlanningStatus> feedbackHandler) {
        Objects.requireNonNull(changeResult);
        Objects.requireNonNull(feedbackHandler);
        switch (changeResult) {
            case ACCESS_DENIED -> feedbackHandler.accept(PlanningStatus.ACCESS_DENIED);
            case NO_ROUTE -> feedbackHandler.accept(PlanningStatus.NO_ROUTE);
            default -> {
            }
        }
    }
    //endregion Constructors

    @Override
    public MHQTabType tabType() {
        return null;
    }

    @Override
    public void removeNotify() {
        finishRouteTransition();
        super.removeNotify();
    }

    /*
     * (non-Javadoc)
     *
     * @see mekhq.gui.CampaignGuiTab#initTab()
     */
    @Override
    public void initTab() {
        layoutState = initializeLayoutState(layoutState);
        routePlanningIntent = initializeRoutePlanningIntent(routePlanningIntent, getCampaign().getCurrentSystem());

        panMapView = new JPanel(new BorderLayout());
        panMapView.add(createNavigationHud(), BorderLayout.PAGE_START);

        //the actual map
        panMap = new InterstellarMapPanel(getCampaign(), getCampaignGui());
        panMap.setRoutePlanningHandler(this);
        // let's go ahead and zoom in on the current location
        panMap.setSelectedSystem(getCampaign().getPlayerForce()
                                       .getForceDetachment()
                                       .getCurrentLocation()
                                       .getCurrentSystem());
        panMapView.add(panMap, BorderLayout.CENTER);
        panMapView.add(createRouteStrip(), BorderLayout.SOUTH);

        mapView = new JViewport();
        mapView.setMinimumSize(new Dimension(UIUtil.scaleForGUI(600), UIUtil.scaleForGUI(600)));
        mapView.setView(panMapView);

        contextWorkspace = createContextWorkspace();
        collapsedContextRail = createCollapsedContextRail();
        splitMap = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapView, contextWorkspace);
        splitMap.setOneTouchExpandable(false);
        splitMap.setResizeWeight(1.0);
        inspectorDividerSize = splitMap.getDividerSize();
        splitMap.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> {
            if (!applyingLayoutState && layoutState.isContextInspectorExpanded()) {
                rememberInspectorWidth();
            }
        });

        panMap.setCampaign(getCampaign());
        panMap.addActionListener(this);

        panSystem = new PlanetarySystemMapPanel(getCampaign(), getCampaignGui());
        panSystem.addActionListener(this);

        setLayout(new BorderLayout());
        add(splitMap, BorderLayout.CENTER);
        add(new TutorialHyperlinkPanel("mapTab.keyText"), BorderLayout.SOUTH);
        refreshSystemView();
        applyLayoutState();
    }

    private JPanel createNavigationHud() {
        JPanel navigationHud = new JPanel(new GridBagLayout());
        navigationHud.setBackground(ROUTE_STRIP_BACKGROUND);
        navigationHud.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(1), 0, ROUTE_STRIP_BORDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4), PADDING,
                  UIUtil.scaleForGUI(4), PADDING)));

        JLabel searchLabel = new JLabel(text("mapHud.systemSearch.text"));
        searchLabel.setForeground(ROUTE_MUTED_COLOR);
        searchLabel.setFont(searchLabel.getFont().deriveFont(Font.BOLD,
              searchLabel.getFont().getSize2D() * 0.8f));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        navigationHud.add(searchLabel, constraints);

        suggestPlanet = new JSuggestField(getFrame(), getCampaign().getSystemNames());
        suggestPlanet.setToolTipText(text("mapHud.systemSearch.toolTipText"));
        suggestPlanet.setBackground(HUD_CONTROL_BACKGROUND);
        suggestPlanet.setForeground(ROUTE_TEXT_COLOR);
        suggestPlanet.setCaretColor(PLANNED_ROUTE_COLOR);
        suggestPlanet.setSelectionColor(ROUTE_STRIP_BORDER);
        suggestPlanet.setSelectedTextColor(ROUTE_TEXT_COLOR);
        suggestPlanet.setBorder(BorderFactory.createLineBorder(ROUTE_STRIP_BORDER, UIUtil.scaleForGUI(1)));
        suggestPlanet.addActionListener(ev -> {
            PlanetarySystem system = getCampaign().getSystemByName(suggestPlanet.getText());
            if (system != null) {
                panMap.setSelectedSystem(system);
                panSystem.updatePlanetarySystem(system);
                syncRouteDestinationToSelection();
                refreshPlanetView();
            }
        });
        suggestPlanet.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                suggestPlanet.setBorder(BorderFactory.createLineBorder(PLANNED_ROUTE_COLOR,
                      UIUtil.scaleForGUI(1)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                suggestPlanet.setBorder(BorderFactory.createLineBorder(ROUTE_STRIP_BORDER,
                      UIUtil.scaleForGUI(1)));
            }
        });
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        navigationHud.add(suggestPlanet, constraints);

                btnQuickPlotCourse = createHudButton("routeStrip.plot.text", "routeStrip.plot.toolTipText");
                btnQuickPlotCourse.addActionListener(event -> plotRouteToSelectedSystem());
                constraints = new GridBagConstraints();
                constraints.gridx = 2;
                constraints.gridy = 0;
                constraints.anchor = GridBagConstraints.EAST;
                constraints.insets = new Insets(0, 0, 0, PADDING);
                navigationHud.add(btnQuickPlotCourse, constraints);

                btnBeginTransit = createHudButton("btnBeginTransit.text", "btnBeginTransit.toolTipText");
                btnBeginTransit.addActionListener(event -> beginTransit());
                constraints = new GridBagConstraints();
                constraints.gridx = 3;
                constraints.gridy = 0;
                constraints.anchor = GridBagConstraints.EAST;
                constraints.insets = new Insets(0, 0, 0, PADDING);
                navigationHud.add(btnBeginTransit, constraints);

                FramedCommandButton centerOnFleet = createHudButton("mapHud.centerOnFleet.text",
              "mapHud.centerOnFleet.toolTipText");
        centerOnFleet.addActionListener(event -> panMap.centerOnCurrentSystem());
        constraints = new GridBagConstraints();
                constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        navigationHud.add(centerOnFleet, constraints);

                FramedCommandButton layers = createHudButton("mapHud.layers.text", "mapHud.layers.toolTipText");
                layers.addActionListener(event -> panMap.toggleLayerControls(layers));
        constraints = new GridBagConstraints();
                constraints.gridx = 5;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        navigationHud.add(layers, constraints);

        JButton information = InterstellarMapPanel.createMapLegendButton();
          int utilityButtonSize = layers.getPreferredSize().height;
          InterstellarMapPanel.setNavigationUtilityButtonSize(information,
              new Dimension(utilityButtonSize, utilityButtonSize));
        information.addActionListener(event -> panMap.toggleMapLegendDialog());
        constraints = new GridBagConstraints();
        constraints.gridx = 6;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        navigationHud.add(information, constraints);

        Dimension preferredSize = navigationHud.getPreferredSize();
        preferredSize.height = Math.max(preferredSize.height, HUD_MINIMUM_HEIGHT);
        navigationHud.setPreferredSize(preferredSize);
        return navigationHud;
    }

    private void applyLayoutState() {
        MapTabLayoutState.preserveViewportCenter(panMap::getMapCenter, panMap::restoreMapCenter, () -> {
            applyingLayoutState = true;
            try {
                setContextInspectorExpanded(layoutState.isContextInspectorExpanded());
                revalidate();
                repaint();
            } finally {
                applyingLayoutState = false;
            }
        });
    }

    private void setContextInspectorExpanded(boolean expanded) {
        if (expanded && (splitMap.getRightComponent() != contextWorkspace)) {
            splitMap.setDividerSize(inspectorDividerSize);
            splitMap.setRightComponent(contextWorkspace);
            int dividerLocation = Math.max(0,
                  splitMap.getWidth() - inspectorWidth - splitMap.getDividerSize());
            splitMap.setDividerLocation(dividerLocation);
        } else if (!expanded && (splitMap.getRightComponent() != collapsedContextRail)) {
            rememberInspectorWidth();
            splitMap.setDividerSize(0);
            splitMap.setRightComponent(collapsedContextRail);
            splitMap.setDividerLocation(Math.max(0, splitMap.getWidth() - INSPECTOR_RAIL_WIDTH));
        }
    }

    private void rememberInspectorWidth() {
        if ((splitMap == null) || (splitMap.getRightComponent() != contextWorkspace)) {
            return;
        }

        int currentInspectorWidth = splitMap.getWidth() - splitMap.getDividerLocation()
              - splitMap.getDividerSize();
        if (currentInspectorWidth > 0) {
            inspectorWidth = currentInspectorWidth;
        }
    }

    private JPanel createContextWorkspace() {
        systemView = createContextScrollPane();
        routeView = createContextScrollPane();
        JPanel routeWorkspace = new JPanel(new BorderLayout());
        routeWorkspace.setBackground(ROUTE_STRIP_BACKGROUND);
        routeWorkspace.add(createRouteWorkspaceControls(), BorderLayout.PAGE_START);
        routeWorkspace.add(routeView, BorderLayout.CENTER);

        JButton collapseButton = createInspectorToggleButton(true,
              text("mapContext.collapse.toolTipText"),
              text("mapContext.collapse.accessibleName"));
        collapseButton.addActionListener(event -> toggleContextInspector());

        CardLayout contextCardLayout = new CardLayout();
        JPanel contextCards = new JPanel(contextCardLayout);
        contextCards.add(systemView, "system");
        contextCards.add(routeWorkspace, "route");

        JToggleButton systemTab = createContextTabButton(text("mapContext.system.text"));
        JToggleButton routeTab = createContextTabButton(text("mapContext.route.text"));
        ButtonGroup contextTabGroup = new ButtonGroup();
        contextTabGroup.add(systemTab);
        contextTabGroup.add(routeTab);
        systemTab.setSelected(true);
        updateContextTabStyle(systemTab);
        updateContextTabStyle(routeTab);
        systemTab.addActionListener(event -> {
            contextCardLayout.show(contextCards, "system");
            updateContextTabStyle(systemTab);
            updateContextTabStyle(routeTab);
        });
        routeTab.addActionListener(event -> {
            contextCardLayout.show(contextCards, "route");
            updateContextTabStyle(systemTab);
            updateContextTabStyle(routeTab);
        });

        JPanel contextHeader = createContextHeader(systemTab, routeTab, collapseButton);

        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setMinimumSize(new Dimension(INSPECTOR_RAIL_WIDTH, UIUtil.scaleForGUI(600)));
        workspace.setPreferredSize(new Dimension(INSPECTOR_PREFERRED_WIDTH, UIUtil.scaleForGUI(600)));
        workspace.add(contextHeader, BorderLayout.PAGE_START);
        workspace.add(contextCards, BorderLayout.CENTER);
        return workspace;
    }

    static JPanel createContextHeader(JToggleButton systemTab, JToggleButton routeTab, JButton collapseButton) {
        int utilityInset = UIUtil.scaleForGUI(4);
        int headerHeight = collapseButton.getPreferredSize().height + (utilityInset * 2) + UIUtil.scaleForGUI(1);

        JPanel tabs = new JPanel(new GridLayout(1, 2));
        tabs.setOpaque(false);
        tabs.setPreferredSize(new Dimension(UIUtil.scaleForGUI(210), headerHeight));
        tabs.add(systemTab);
        tabs.add(routeTab);

        JPanel utilityCell = new JPanel(null) {
            @Override
            public void doLayout() {
                Dimension buttonSize = collapseButton.getPreferredSize();
                int buttonX = (getWidth() - buttonSize.width) / 2;
                int buttonY = (getHeight() - buttonSize.height) / 2;
                collapseButton.setBounds(buttonX, buttonY, buttonSize.width, buttonSize.height);
            }
        };
        utilityCell.setOpaque(false);
        utilityCell.setPreferredSize(new Dimension(collapseButton.getPreferredSize().width + (utilityInset * 2),
              headerHeight));
        utilityCell.add(collapseButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ROUTE_STRIP_BACKGROUND);
          header.setBorder(BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(1), 0,
              ROUTE_STRIP_BORDER));
        header.setPreferredSize(new Dimension(0, headerHeight));
        header.add(tabs, BorderLayout.LINE_START);
        header.add(utilityCell, BorderLayout.LINE_END);
        return header;
    }

    static JToggleButton createContextTabButton(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setBackground(ROUTE_STRIP_BACKGROUND);
        button.setForeground(ROUTE_TEXT_COLOR);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setMargin(new Insets(0, UIUtil.scaleForGUI(14), 0, UIUtil.scaleForGUI(14)));
        return button;
    }

    private static void updateContextTabStyle(JToggleButton button) {
        button.setBorder(BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(3), 0,
              button.isSelected() ? PLANNED_ROUTE_COLOR.darker() : ROUTE_STRIP_BACKGROUND));
    }

    private JPanel createCollapsedContextRail() {
        JPanel rail = new JPanel(new BorderLayout());
        rail.setBackground(ROUTE_STRIP_BACKGROUND);
          rail.setBorder(BorderFactory.createMatteBorder(0, UIUtil.scaleForGUI(1), 0, 0,
              ROUTE_STRIP_BORDER));
        Dimension railSize = new Dimension(INSPECTOR_RAIL_WIDTH, INSPECTOR_RAIL_WIDTH);
        rail.setMinimumSize(railSize);
        rail.setPreferredSize(railSize);
        rail.setMaximumSize(new Dimension(INSPECTOR_RAIL_WIDTH, Integer.MAX_VALUE));
        JButton expandButton = createInspectorToggleButton(false,
              text("mapContext.expand.toolTipText"),
              text("mapContext.expand.accessibleName"));
        expandButton.addActionListener(event -> toggleContextInspector());
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.setOpaque(false);
          int utilityInset = UIUtil.scaleForGUI(4);
          buttonRow.setBorder(BorderFactory.createEmptyBorder(utilityInset, utilityInset,
              utilityInset, utilityInset));
        buttonRow.add(expandButton, BorderLayout.PAGE_START);
        rail.add(buttonRow, BorderLayout.PAGE_START);
        return rail;
    }

    private void toggleContextInspector() {
        layoutState.toggleContextInspector();
        applyLayoutState();
    }

    static JButton createInspectorToggleButton(boolean expanded, String toolTipText, String accessibleName) {
        return InterstellarMapPanel.createNavigationUtilityIconButton(expanded ? 0xE5CC : 0xE5CB,
              INSPECTOR_TOGGLE_SIZE, toolTipText, accessibleName, toolTipText);
    }

    private JScrollPane createContextScrollPane() {
        JScrollPane scrollPane = new FastJScrollPane();
        scrollPane.setBackground(ROUTE_STRIP_BACKGROUND);
        scrollPane.getViewport().setBackground(ROUTE_STRIP_BACKGROUND);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        styleContextScrollBar(scrollPane.getVerticalScrollBar());
        return scrollPane;
    }

    static void styleContextScrollBar(javax.swing.JScrollBar scrollBar) {
        ImmersiveScrollBarStyle.apply(scrollBar, ROUTE_STRIP_BACKGROUND, ROUTE_STRIP_BORDER,
              new Color(54, 101, 113), PLANNED_ROUTE_COLOR, INSPECTOR_SCROLLBAR_WIDTH);
    }

    private JPanel createRouteWorkspaceControls() {
        JPanel controls = new JPanel(new GridBagLayout());
        controls.setBackground(ROUTE_STRIP_BACKGROUND);
        controls.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(1), 0, ROUTE_STRIP_BORDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(10), PADDING,
                  UIUtil.scaleForGUI(10), PADDING)));

        JLabel heading = createRouteWorkspaceHeading(text("routePlanner.heading.text"));
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, UIUtil.scaleForGUI(8), 0);
        controls.add(heading, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        controls.add(createRoutePlanningPanel(), constraints);

          JPanel routeOptions = createRouteOptionsPanel();
          constraints = new GridBagConstraints();
          constraints.gridx = 0;
          constraints.gridy = 2;
          constraints.weightx = 1.0;
          constraints.fill = GridBagConstraints.HORIZONTAL;
          constraints.anchor = GridBagConstraints.WEST;
          constraints.insets = new Insets(UIUtil.scaleForGUI(8), 0, 0, 0);
          controls.add(routeOptions, constraints);

        btnCalculateJumpPath = createHudButton("btnCalculateJumpPath.text", "btnCalculateJumpPath.toolTipText");
        btnCalculateJumpPath.addActionListener(event -> calculateJumpPath());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(UIUtil.scaleForGUI(8), 0, 0, 0);
        controls.add(btnCalculateJumpPath, constraints);

        routeViewSelector = createRouteViewSelector();
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(UIUtil.scaleForGUI(10), 0, 0, 0);
        controls.add(routeViewSelector, constraints);
        return controls;
    }

    private JLabel createRouteWorkspaceHeading(String text) {
        JLabel heading = new JLabel(text);
        heading.setForeground(PLANNED_ROUTE_COLOR);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, heading.getFont().getSize2D() * 0.85f));
        return heading;
    }

    private JPanel createRouteOptionsPanel() {
        JPanel options = new JPanel();
        options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
        options.setOpaque(false);

          JCheckBox avoidAbandonedSystems = new ImmersiveCheckBox(
              text("chkAvoidAbandonedSystems.text"));
        avoidAbandonedSystems.setToolTipText(wordWrap(
              text("chkAvoidAbandonedSystems.toolTipText")));
        avoidAbandonedSystems.setSelected(getCampaign().getPlayerForce().isAvoidingEmptySystems());
        avoidAbandonedSystems.setAlignmentX(Component.LEFT_ALIGNMENT);
        avoidAbandonedSystems.addActionListener(event ->
              getCampaign().getPlayerForce().setIsAvoidingEmptySystems(avoidAbandonedSystems.isSelected()));
        options.add(avoidAbandonedSystems);

          JCheckBox useCommandCircuits = new ImmersiveCheckBox(
              text("chkUseCommandCircuits.text"));
          useCommandCircuits.setToolTipText(wordWrap(text("chkUseCommandCircuits.toolTipText")));
        useCommandCircuits.setSelected(getCampaign().getPlayerForce().isOverridingCommandCircuitRequirements());
        useCommandCircuits.setAlignmentX(Component.LEFT_ALIGNMENT);
        useCommandCircuits.addActionListener(event -> {
            getCampaign().getPlayerForce()
                  .setIsOverridingCommandCircuitRequirements(useCommandCircuits.isSelected());
            updateRouteStrip();
        });
          options.add(useCommandCircuits);
        return options;
    }

    private JPanel createRouteViewSelector() {
        JPanel selector = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintChildren(Graphics graphics) {
                super.paintChildren(graphics);
                if ((activeRouteViewButton == null) || (plannedRouteViewButton == null)) {
                    return;
                }
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                JToggleButton selectedButton = activeRouteViewButton.isSelected()
                      ? activeRouteViewButton : plannedRouteViewButton;
                graphics2D.setColor(activeRouteViewButton.isSelected()
                      ? ACTIVE_ROUTE_COLOR : PLANNED_ROUTE_COLOR);
                graphics2D.fillRect(selectedButton.getX(), getHeight() - UIUtil.scaleForGUI(3),
                      selectedButton.getWidth(), UIUtil.scaleForGUI(2));
                graphics2D.dispose();
            }
        };
        selector.setOpaque(true);
        selector.setBackground(ROUTE_STRIP_BACKGROUND);
          selector.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(ROUTE_STRIP_BORDER, UIUtil.scaleForGUI(1)),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4), 0, UIUtil.scaleForGUI(4), 0)));
        ButtonGroup group = new ButtonGroup();
        activeRouteViewButton = createRouteViewButton("routePlanner.activeTrip.text", RouteViewMode.ACTIVE);
        plannedRouteViewButton = createRouteViewButton("routePlanner.plannedRoute.text", RouteViewMode.PLANNED);
        group.add(activeRouteViewButton);
        group.add(plannedRouteViewButton);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        selector.add(activeRouteViewButton, constraints);
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        selector.add(plannedRouteViewButton, constraints);
        return selector;
    }

    private JToggleButton createRouteViewButton(String textKey, RouteViewMode mode) {
        JToggleButton button = new JToggleButton(text(textKey));
        button.setForeground(ROUTE_TEXT_COLOR);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setMargin(new Insets(UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(3), UIUtil.scaleForGUI(8)));
        button.addActionListener(event -> {
            routeViewMode = mode;
            refreshRouteView();
        });
        return button;
    }

    private FramedCommandButton createHudButton(String textKey, String toolTipKey) {
        return createNavigationButton(text(textKey), text(toolTipKey));
    }

    static FramedCommandButton createNavigationButton(String text, String toolTipText) {
        FramedCommandButton button = new FramedCommandButton(text, NAVIGATION_BUTTON_COLORS);
        button.setToolTipText(toolTipText);
        button.getAccessibleContext().setAccessibleName(button.getText());
        button.getAccessibleContext().setAccessibleDescription(button.getToolTipText());
        button.setMargin(new Insets(HUD_BUTTON_VERTICAL_MARGIN, HUD_BUTTON_HORIZONTAL_MARGIN,
              HUD_BUTTON_VERTICAL_MARGIN, HUD_BUTTON_HORIZONTAL_MARGIN));
        return button;
    }

    private JPanel createRouteStrip() {
        routeStrip = new RouteStripPanel();
        routeStrip.setBackground(ROUTE_STRIP_BACKGROUND);
        routeStrip.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(UIUtil.scaleForGUI(1), 0, 0, 0, ROUTE_STRIP_BORDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4), PADDING,
                  UIUtil.scaleForGUI(4), PADDING)));

        lblRouteStatus = createRouteLabel(Font.BOLD);
        lblRouteDestination = createRouteLabel(Font.PLAIN);
        lblRouteJumps = createRouteLabel(Font.PLAIN);
        lblRouteDuration = createRouteLabel(Font.PLAIN);
        lblRouteNextJump = createRouteLabel(Font.PLAIN);
        lblRouteCost = createRouteLabel(Font.PLAIN);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        routeStrip.add(lblRouteStatus, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        routeStrip.add(lblRouteDestination, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        routeStrip.add(lblRouteJumps, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, PADDING);
        routeStrip.add(lblRouteDuration, constraints);

        routeTransitionTimer = new Timer(ROUTE_TRANSITION_FRAME_DELAY_MS, event -> updateRouteTransition());
        routeTransitionTimer.setCoalesce(true);
        return routeStrip;
    }

    private JPanel createRoutePlanningPanel() {
        JPanel planningPanel = new JPanel(new GridBagLayout());
        planningPanel.setOpaque(false);

        JLabel originLabel = createRouteFieldLabel("routePlanner.from.text");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, UIUtil.scaleForGUI(3), 0);
        planningPanel.add(originLabel, constraints);

        suggestRouteOrigin = createRouteField("routePlanner.from.text", "routePlanner.from.toolTipText");
        originLabel.setLabelFor(suggestRouteOrigin);
        setRouteFieldSystem(suggestRouteOrigin, routePlanningIntent.getOrigin());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        planningPanel.add(suggestRouteOrigin, constraints);

        JLabel destinationLabel = createRouteFieldLabel("routePlanner.to.text");
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(UIUtil.scaleForGUI(8), 0, UIUtil.scaleForGUI(3), 0);
        planningPanel.add(destinationLabel, constraints);

        suggestRouteDestination = createRouteField("routePlanner.to.text", "routePlanner.to.toolTipText");
        destinationLabel.setLabelFor(suggestRouteDestination);
        setRouteFieldSystem(suggestRouteDestination, panMap.getSelectedSystem());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        planningPanel.add(suggestRouteDestination, constraints);

        installRouteFieldListeners();
        return planningPanel;
    }

    private JLabel createRouteFieldLabel(String textKey) {
        JLabel label = new JLabel(text(textKey));
        label.setForeground(ROUTE_MUTED_COLOR);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() * 0.8f));
        return label;
    }

    private JSuggestField createRouteField(String accessibleNameKey, String toolTipKey) {
        JSuggestField field = new JSuggestField(getFrame(), getCampaign().getSystemNames());
        field.setToolTipText(text(toolTipKey));
        field.setBackground(HUD_CONTROL_BACKGROUND);
        field.setForeground(ROUTE_TEXT_COLOR);
        field.setCaretColor(PLANNED_ROUTE_COLOR);
        field.setSelectionColor(ROUTE_STRIP_BORDER);
        field.setSelectedTextColor(ROUTE_TEXT_COLOR);
        field.setBorder(BorderFactory.createLineBorder(ROUTE_STRIP_BORDER, UIUtil.scaleForGUI(1)));
        Dimension preferredSize = field.getPreferredSize();
        field.setPreferredSize(new Dimension(UIUtil.scaleForGUI(180), preferredSize.height));
        field.setMinimumSize(new Dimension(UIUtil.scaleForGUI(96), preferredSize.height));
        field.getAccessibleContext().setAccessibleName(text(accessibleNameKey));
        field.getAccessibleContext().setAccessibleDescription(field.getToolTipText());
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                field.setBorder(BorderFactory.createLineBorder(PLANNED_ROUTE_COLOR,
                      UIUtil.scaleForGUI(1)));
            }

            @Override
            public void focusLost(FocusEvent event) {
                field.setBorder(BorderFactory.createLineBorder(ROUTE_STRIP_BORDER,
                      UIUtil.scaleForGUI(1)));
            }
        });
        return field;
    }

    private void installRouteFieldListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                updateRouteEntryAction();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                updateRouteEntryAction();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                updateRouteEntryAction();
            }
        };
        suggestRouteOrigin.getDocument().addDocumentListener(listener);
        suggestRouteDestination.getDocument().addDocumentListener(listener);
        suggestRouteOrigin.addActionListener(event -> updateRouteEntryAction());
        suggestRouteDestination.addActionListener(event -> updateRouteEntryAction());
    }

    private void updateRouteEntryAction() {
        if ((btnCalculateJumpPath == null)
              || ((routeTransitionTimer != null) && routeTransitionTimer.isRunning())) {
            return;
        }
          btnCalculateJumpPath.setEnabled((resolveRouteField(suggestRouteOrigin) != null)
              && (resolveRouteField(suggestRouteDestination) != null));
    }

    private PlanetarySystem resolveRouteField(JSuggestField field) {
        return getCampaign().getSystemByName(field.getText());
    }

    private void setRouteFieldSystem(JSuggestField field, PlanetarySystem system) {
        field.setText((system == null) ? "" : system.getPrintableName(getCampaign().getLocalDate()));
    }

    private void showJumpFeeSummary() {
        TransportCostCalculations transportCostCalculations =
              getCampaign().getTransportCostCalculation(EXP_REGULAR);
        transportCostCalculations.calculateJumpCostForEachDay();
        new JumpCostsSummary(getCampaign(), transportCostCalculations);
    }

    private JLabel createRouteLabel(int fontStyle) {
        JLabel label = new JLabel();
        label.setForeground(ROUTE_TEXT_COLOR);
        label.setFont(label.getFont().deriveFont(fontStyle));
        return label;
    }

    private void updateRouteStrip() {
        if ((panMap == null) || (lblRouteStatus == null)) {
            return;
        }

        panMap.refreshNavigationAnalysis();
        transitionToRouteSnapshot(createRouteStripSnapshot());
    }

    private RouteStripSnapshot createRouteStripSnapshot() {
        AbstractLocation currentLocation = getCampaign().getPlayerForce()
                                                    .getForceDetachment()
                                                    .getCurrentLocation();
        JumpPath proposedPath = routePlanningIntent.getJumpPath();
        JumpPath activePath = (currentLocation == null) ? null : currentLocation.getJumpPath();
        boolean hasProposedPath = (proposedPath != null) && !proposedPath.isEmpty();
        boolean hasActivePath = (activePath != null) && !activePath.isEmpty();
        JumpPath displayedPath = hasProposedPath ? proposedPath : (hasActivePath ? activePath : null);
        String unavailable = text("routeStrip.unavailable.text");
        boolean isCostVisible = getCampaign().getCampaignOptions().get(CampaignOption.PAY_FOR_TRANSPORT);
        PlanetarySystem selectedSystem = panMap.getSelectedSystem();
        boolean isQuickPlotVisible = true;
        boolean isQuickPlotEnabled = canQuickPlotRoute(selectedSystem, getCampaign().getCurrentSystem());
          String quickPlotText = text(quickPlotResourceKey(hasProposedPath));

        if (displayedPath == null) {
            String selectedSystemName = (selectedSystem == null)
                ? unavailable
                : selectedSystem.getPrintableName(getCampaign().getLocalDate());
            return new RouteStripSnapshot(RouteStripState.NO_ROUTE,
                                    text("routeStrip.noRoute.text"),
                  ROUTE_MUTED_COLOR,
                                text("routeStrip.target.text") + "  " + selectedSystemName,
                                    text("routeStrip.jumps.text") + "  " + unavailable,
                                    text("routeStrip.duration.text") + "  " + unavailable,
                                    text("routeStrip.nextJump.text") + "  " + unavailable,
                                    text("routeStrip.estimatedCost.text") + "  " + unavailable,
                  ROUTE_MUTED_COLOR,
                  isCostVisible,
                  quickPlotText,
                  isQuickPlotVisible,
                  isQuickPlotEnabled,
                  hasProposedPath);
        }

          boolean isPlannedRoute = hasProposedPath;
          PathAssessment proposedRouteAssessment = isPlannedRoute ? assessPlannedRoute(proposedPath) : null;
          boolean whatIfRoute = isPlannedRoute
              && isWhatIfRoute(proposedPath, getCampaign().getCurrentSystem());
          RouteStripState routeState = whatIfRoute ? RouteStripState.WHAT_IF_ROUTE
              : (isPlannedRoute ? RouteStripState.PLANNED_ROUTE : RouteStripState.IN_TRANSIT);
          String statusText = text(switch (routeState) {
            case NO_ROUTE -> "routeStrip.noRoute.text";
            case PLANNED_ROUTE -> "routeStrip.plannedRoute.text";
            case WHAT_IF_ROUTE -> "routeStrip.whatIfRoute.text";
            case IN_TRANSIT -> "routeStrip.inTransit.text";
          });
        PlanetarySystem destination = displayedPath.getLastSystem();
        String destinationName = (destination == null)
              ? unavailable
              : destination.getPrintableName(getCampaign().getLocalDate());
        String destinationText = text("routeStrip.destination.text") + "  " + destinationName;
        String jumpsText = text(isPlannedRoute
              ? "routeStrip.jumps.text"
              : "routeStrip.jumpsRemaining.text") + "  " + displayedPath.getJumps();

        boolean useCampaignProgress = !whatIfRoute;
          double currentTransit = (currentLocation == null)
              ? 0.0
              : (isPlannedRoute
                  ? getProposedRouteTransitProgress(proposedPath, getCampaign().getCurrentSystem(),
                      currentLocation.getTransitTime())
                  : currentLocation.getTransitTime());
          int duration = (int) ceil(displayedPath.getTotalTime(getCampaign().getLocalDate(), currentTransit,
              getCampaign().isUseCommandCircuit()));
          String durationText = text("routeStrip.duration.text") + "  " + duration + ' '
              + text("routeStrip.days.text");
          String nextJumpText = createNextJumpText(displayedPath, currentLocation, unavailable, useCampaignProgress);

        String costText = text("routeStrip.estimatedCost.text") + "  " + unavailable;
        if (isCostVisible) {
            TransportCostCalculations calculations = getCampaign().getTransportCostCalculation(EXP_REGULAR);
            Money journeyCost = calculations.calculateJumpCostForEntireJourney(duration, displayedPath.getJumps());
            costText = text("routeStrip.estimatedCost.text") + "  "
                + journeyCost.toAmountAndSymbolString();
        }

          boolean beginTransitEnabled = isPlannedRoute
              && canBeginTransit(proposedPath, getCampaign().getCurrentSystem(), activePath,
                    proposedRouteAssessment);
          return new RouteStripSnapshot(routeState,
              statusText,
              isPlannedRoute ? PLANNED_ROUTE_COLOR : ACTIVE_ROUTE_COLOR,
              destinationText,
              jumpsText,
              durationText,
              nextJumpText,
              costText,
              ROUTE_TEXT_COLOR,
              isCostVisible,
              quickPlotText,
              isQuickPlotVisible,
              isQuickPlotEnabled,
              beginTransitEnabled);
    }

        private String createNextJumpText(JumpPath displayedPath, AbstractLocation currentLocation, String unavailable,
            boolean useCampaignProgress) {
        String label = text("routeStrip.nextJump.text") + "  ";
        if ((displayedPath == null) || displayedPath.isEmpty() || (displayedPath.getJumps() <= 0)
              || (displayedPath.getFirstSystem() == null)
              || (useCampaignProgress && ((currentLocation == null)
                  || (currentLocation.getCurrentSystem() == null)))) {
            return label + unavailable;
        }

          double elapsedTransit = useCampaignProgress ? currentLocation.getTransitTime() : 0.0;
        double remainingTransitDays = Math.max(0.0,
              displayedPath.getStartTime(elapsedTransit));
          PlanetarySystem currentSystem = useCampaignProgress
              ? currentLocation.getCurrentSystem()
              : displayedPath.getFirstSystem();
          double elapsedRecharge = useCampaignProgress ? currentLocation.getRechargeTime() : 0.0;
        double remainingRechargeHours = Math.max(0.0,
              currentSystem.getRechargeTime(getCampaign().getLocalDate(), getCampaign().isUseCommandCircuit())
                  - elapsedRecharge);
        double remainingDays = Math.max(remainingTransitDays, remainingRechargeHours / 24.0);
        if (remainingDays <= 0.0) {
            return label + text("routeStrip.nextJump.ready.text");
        }

        long totalHours = (long) Math.ceil(remainingDays * 24.0);
        long days = totalHours / 24;
        long hours = totalHours % 24;
        String timeText;
        if ((days > 0) && (hours > 0)) {
            timeText = days + text("routeStrip.nextJump.daySuffix.text") + ' '
                  + hours + text("routeStrip.nextJump.hourSuffix.text");
        } else if (days > 0) {
            timeText = days + text("routeStrip.nextJump.daySuffix.text");
        } else {
            timeText = hours + text("routeStrip.nextJump.hourSuffix.text");
        }
        return label + timeText;
    }

    private void transitionToRouteSnapshot(RouteStripSnapshot nextSnapshot) {
        if (nextSnapshot.equals(targetRouteSnapshot)) {
            return;
        }

        if ((presentedRouteSnapshot == null) || !routeStrip.isDisplayable()) {
            showRouteSnapshotImmediately(nextSnapshot);
            return;
        }

        if (nextSnapshot.equals(presentedRouteSnapshot)) {
            showRouteSnapshotImmediately(nextSnapshot);
            return;
        }

        routeTransitionTimer.stop();
        targetRouteSnapshot = nextSnapshot;
        routeTransitionStartTime = System.nanoTime();
        routeTransitionStartAlpha = routeStrip.getChildAlpha();
        routeTransitionMidpointApplied = false;
        routeRevealTransition = (nextSnapshot.state() == RouteStripState.PLANNED_ROUTE)
              && (presentedRouteSnapshot.state() != RouteStripState.PLANNED_ROUTE);
        btnCalculateJumpPath.setEnabled(false);
          btnQuickPlotCourse.setEnabled(false);
        btnBeginTransit.setEnabled(false);
        routeStrip.setRevealProgress(0.0f);
        routeTransitionTimer.restart();
    }

    private void updateRouteTransition() {
        float progress = Math.min(1.0f,
              (float) (System.nanoTime() - routeTransitionStartTime) / ROUTE_TRANSITION_DURATION_NS);

        if (!routeTransitionMidpointApplied && (progress >= ROUTE_TRANSITION_MIDPOINT)) {
            applyRouteSnapshot(targetRouteSnapshot, false);
            routeTransitionMidpointApplied = true;
        }

        float alpha;
        if (progress < ROUTE_TRANSITION_MIDPOINT) {
            float phaseProgress = easeRouteTransition(progress / ROUTE_TRANSITION_MIDPOINT);
            alpha = routeTransitionStartAlpha
                  + ((ROUTE_TRANSITION_MIN_ALPHA - routeTransitionStartAlpha) * phaseProgress);
        } else {
            float phaseProgress = easeRouteTransition((progress - ROUTE_TRANSITION_MIDPOINT)
                  / ROUTE_TRANSITION_MIDPOINT);
            alpha = ROUTE_TRANSITION_MIN_ALPHA + ((1.0f - ROUTE_TRANSITION_MIN_ALPHA) * phaseProgress);
        }
        routeStrip.setChildAlpha(alpha);
        routeStrip.setRevealProgress(routeRevealTransition ? progress : 0.0f);

        if (progress >= 1.0f) {
            finishRouteTransition();
        }
    }

    private static float easeRouteTransition(float progress) {
        return progress * progress * (3.0f - (2.0f * progress));
    }

    private void showRouteSnapshotImmediately(RouteStripSnapshot snapshot) {
        if (routeTransitionTimer != null) {
            routeTransitionTimer.stop();
        }
        targetRouteSnapshot = snapshot;
        routeTransitionMidpointApplied = true;
        routeRevealTransition = false;
        routeStrip.setChildAlpha(1.0f);
        routeStrip.setRevealProgress(0.0f);
        applyRouteSnapshot(snapshot, true);
    }

    private void finishRouteTransition() {
        if (routeTransitionTimer == null) {
            return;
        }
        routeTransitionTimer.stop();
        routeStrip.setChildAlpha(1.0f);
        routeStrip.setRevealProgress(0.0f);
        if (targetRouteSnapshot != null) {
            applyRouteSnapshot(targetRouteSnapshot, true);
        }
        routeTransitionMidpointApplied = true;
        routeRevealTransition = false;
    }

    private void applyRouteSnapshot(RouteStripSnapshot snapshot, boolean enableActions) {
        lblRouteStatus.setText(snapshot.statusText());
        lblRouteStatus.setForeground(snapshot.statusColor());
        String statusDescription = snapshot.state() == RouteStripState.WHAT_IF_ROUTE
              ? text("routeStrip.whatIfRoute.toolTipText")
              : snapshot.statusText();
        lblRouteStatus.setToolTipText(statusDescription);
        lblRouteStatus.getAccessibleContext().setAccessibleName(snapshot.statusText());
        lblRouteStatus.getAccessibleContext().setAccessibleDescription(statusDescription);
        lblRouteDestination.setText(snapshot.destinationText());
        lblRouteJumps.setText(snapshot.jumpsText());
        lblRouteDuration.setText(snapshot.durationText());
        lblRouteNextJump.setText(snapshot.nextJumpText());
        lblRouteCost.setText(snapshot.costText());
        setRouteMetricColor(snapshot.metricColor());
        lblRouteCost.setVisible(false);
        updateRouteEntryAction();
        btnQuickPlotCourse.setText(snapshot.quickPlotText());
        btnQuickPlotCourse.getAccessibleContext().setAccessibleName(snapshot.quickPlotText());
          String quickPlotDescription = text("routeStrip.plot.toolTipText");
          btnQuickPlotCourse.setToolTipText(quickPlotDescription);
          btnQuickPlotCourse.getAccessibleContext().setAccessibleDescription(quickPlotDescription);
        btnQuickPlotCourse.setVisible(snapshot.quickPlotVisible());
        btnQuickPlotCourse.setEnabled(enableActions && snapshot.quickPlotEnabled());
        btnBeginTransit.setVisible(true);
                    btnBeginTransit.setEnabled(enableActions && snapshot.beginTransitEnabled());
                        String beginTransitDescription = snapshot.state() == RouteStripState.WHAT_IF_ROUTE
              ? text("routePlanner.beginTransitWhatIf.toolTipText")
              : text("btnBeginTransit.toolTipText");
          btnBeginTransit.setToolTipText(beginTransitDescription);
          btnBeginTransit.getAccessibleContext().setAccessibleDescription(beginTransitDescription);
        presentedRouteSnapshot = snapshot;
        routeStrip.revalidate();
        routeStrip.repaint();
    }

    private void setRouteMetricColor(Color color) {
        lblRouteDestination.setForeground(color);
        lblRouteJumps.setForeground(color);
        lblRouteDuration.setForeground(color);
        lblRouteNextJump.setForeground(color);
        lblRouteCost.setForeground(color);
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
        PlanetarySystem origin = resolveRouteField(suggestRouteOrigin);
        PlanetarySystem destination = resolveRouteField(suggestRouteDestination);
        if ((origin != null) && (destination != null)) {
            applyRoutePlanningChange(routePlanningIntent.plot(origin, destination, this::calculateRouteSegment),
                  destination);
        }
    }

    private void plotRouteToSelectedSystem() {
        PlanetarySystem origin = getCampaign().getCurrentSystem();
        PlanetarySystem destination = panMap.getSelectedSystem();
        if (!canQuickPlotRoute(destination, origin)) {
            return;
        }
        applyRoutePlanningChange(routePlanningIntent.plot(origin, destination, this::calculateRouteSegment),
              destination);
    }

    @Override
    public void plotRoute(PlanetarySystem destination) {
        PlanetarySystem origin = resolveRouteField(suggestRouteOrigin);
        if (origin == null) {
            origin = routePlanningIntent.getOrigin();
        }
        if (origin == null) {
            origin = getCampaign().getCurrentSystem();
        }
        applyRoutePlanningChange(routePlanningIntent.plot(origin, destination, this::calculateRouteSegment),
              destination);
    }

    @Override
    public void appendWaypoint(PlanetarySystem destination) {
        ChangeResult routeChange;
        if (routePlanningIntent.getJumpPath().isEmpty()) {
            PlanetarySystem origin = resolveRouteField(suggestRouteOrigin);
            if (origin == null) {
                origin = routePlanningIntent.getOrigin();
            }
            routeChange = routePlanningIntent.plot(origin, destination, this::calculateRouteSegment);
        } else {
            routeChange = routePlanningIntent.append(destination, this::calculateRouteSegment);
        }
        applyRoutePlanningChange(routeChange, destination);
    }

    @Override
    public void trimRouteAt(PlanetarySystem destination) {
        applyRoutePlanningChange(routePlanningIntent.trimAt(destination, this::calculateRouteSegment), destination);
    }

    @Override
    public void removeWaypoint(PlanetarySystem waypoint) {
        applyRoutePlanningChange(routePlanningIntent.removeRequestedStop(waypoint, this::calculateRouteSegment),
              waypoint);
    }

    @Override
    public void clearPlannedRoute() {
        routePlanningIntent.clear(getCampaign().getCurrentSystem());
        panMap.setJumpPath(routePlanningIntent.getJumpPath());
        setRouteFieldSystem(suggestRouteOrigin, routePlanningIntent.getOrigin());
        setRouteFieldSystem(suggestRouteDestination, panMap.getSelectedSystem());
        refreshPlanetView();
    }

    @Override
    public boolean hasPlannedRoute() {
        return !routePlanningIntent.getJumpPath().isEmpty();
    }

    @Override
    public boolean canTrimRouteAt(PlanetarySystem system) {
        return routePlanningIntent.canTrimAt(system);
    }

    @Override
    public boolean isRequestedWaypoint(PlanetarySystem system) {
        return routePlanningIntent.isRequestedStop(system);
    }

    private PlanningResult calculateRouteSegment(PlanetarySystem origin, PlanetarySystem destination) {
        return getCampaign().calculateJumpPathForPlanning(origin, destination);
    }

    private PathAssessment assessPlannedRoute(JumpPath path) {
        return getCampaign().assessNavigationPath(path.getSystems(), routePlanningIntent.getRequestedStops(),
              destinationIndex -> getCampaign().isUseCommandCircuit());
    }

    private void applyRoutePlanningChange(ChangeResult routeChange, PlanetarySystem selection) {
        dispatchRoutePlanningFeedback(routeChange, getCampaign()::showRoutePlanningFailure);
        if (!routeChange.changed()) {
            return;
        }
        routeViewMode = RouteViewMode.PLANNED;
        panMap.setJumpPath(routePlanningIntent.getJumpPath());
        panMap.selectRouteTarget(selection);
        setRouteFieldSystem(suggestRouteOrigin, routePlanningIntent.getOrigin());
        List<PlanetarySystem> requestedStops = routePlanningIntent.getRequestedStops();
        setRouteFieldSystem(suggestRouteDestination,
              requestedStops.isEmpty() ? selection : requestedStops.getLast());
        refreshPlanetView();
    }

    private void adoptRouteAlternative(Course course) {
        JumpPath alternative = course.toJumpPath();
        if (routePlanningIntent.adopt(alternative)) {
            applyRoutePlanningChange(ChangeResult.CHANGED, alternative.getLastSystem());
        }
    }

    private void beginTransit() {
        JumpPath jumpPath = routePlanningIntent.getJumpPath();
        AbstractLocation currentLocation = getCampaign().getPlayerForce()
                                                   .getForceDetachment()
                                                   .getCurrentLocation();
        if (currentLocation == null) {
            return;
        }
        JumpPath activePath = currentLocation.getJumpPath();
        PathAssessment routeAssessment = assessPlannedRoute(jumpPath);
        if (!canBeginTransit(jumpPath, getCampaign().getCurrentSystem(), activePath, routeAssessment)) {
            return;
        }

        if (!JumpBlockers.areAllUnitsJumpCapable(getCampaign())) {
            return;
        }

        if (!MekHQ.getMHQOptions().getNagDialogIgnore(CONFIRMATION_BEGIN_TRANSIT)) {
            ImmersiveDialogConfirmation dialog = new ImmersiveDialogConfirmation(getCampaign(),
                  CONFIRMATION_BEGIN_TRANSIT);
            if (!dialog.wasConfirmed()) {
                return;
            }
        }

        // Mothballing
        outOfContractMothballAutomation(getCampaign());

        // Everything else
        boolean isUseCommandCircuits = getCampaign().isUseCommandCircuit();
        int duration = (int) ceil(jumpPath.getTotalTime(getCampaign().getLocalDate(),
              currentLocation.getTransitTime(),
              isUseCommandCircuits));

        TransportCostCalculations transportCostCalculations = getCampaign().getTransportCostCalculation(EXP_REGULAR);
        Money journeyCost = transportCostCalculations.calculateJumpCostForEntireJourney(duration, jumpPath.getJumps());

        String jumpReport = TransportCostCalculations.performJumpTransaction(getCampaign().getPlayerForce()
                                                                                   .getFinances(), jumpPath,
              getCampaign().getLocalDate(), journeyCost, getCampaign().getCurrentSystem());

        if (!jumpReport.isBlank()) {
            getCampaign().addReport(GENERAL, jumpReport);
        }

        currentLocation.setJumpPath(jumpPath);
        routeViewMode = RouteViewMode.ACTIVE;
        routePlanningIntent.clear(getCampaign().getCurrentSystem());
        panMap.setJumpPath(routePlanningIntent.getJumpPath());
        setRouteFieldSystem(suggestRouteOrigin, routePlanningIntent.getOrigin());
        setRouteFieldSystem(suggestRouteDestination, panMap.getSelectedSystem());
        refreshPlanetView();

        getCampaign().getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));

        abandonMissingPersonnel(getCampaign());

        NewPersonnelMarket personnelMarket = getCampaign().getPlayerForce().getHumanResources().getNewPersonnelMarket();
        if (personnelMarket.getAssociatedPersonnelMarketStyle() == MEKHQ) {
            personnelMarket.clearCurrentApplicants();
        }
    }

    private void refreshSystemView() {
        updateRouteStrip();
        refreshRouteView();
        PlanetarySystem system = panMap.getSelectedSystem();
        if (null != system) {
            showDossier(system, 0);
        }
    }

    private void refreshPlanetView() {
        updateRouteStrip();
        refreshRouteView();
        int pos = panSystem.getSelectedPlanetPosition();
        PlanetarySystem system = panMap.getSelectedSystem();
        if (null != system) {
            showDossier(system, pos);
        }
    }

    private JumpPathViewPanel createJumpPathViewPanel(JumpPath path, boolean plannedRoute) {
        List<Course> courses = List.of();
        List<PlanetarySystem> requestedStops = plannedRoute
              ? routePlanningIntent.getRequestedStops()
              : List.of();
        if (plannedRoute && !routePlanningIntent.getJumpPath().isEmpty() && !requestedStops.isEmpty()) {
            courses = getCampaign().calculateRouteAlternatives(routePlanningIntent.getOrigin(),
                  requestedStops);
        }
          return new JumpPathViewPanel(path, getCampaign(), courses, requestedStops, this::adoptRouteAlternative,
              this::showJumpFeeSummary, plannedRoute ? null : this::cancelCurrentTrip,
              advancedRoutePlanningExpanded,
              expanded -> advancedRoutePlanningExpanded = expanded);
    }

    private void refreshRouteView() {
        JumpPath plannedPath = routePlanningIntent.getJumpPath();
        JumpPath activePath = getActiveRoute();
        boolean hasPlannedRoute = (plannedPath != null) && !plannedPath.isEmpty();
        boolean hasActiveRoute = (activePath != null) && !activePath.isEmpty();
        if (routeViewMode == RouteViewMode.ACTIVE && !hasActiveRoute) {
            routeViewMode = hasPlannedRoute ? RouteViewMode.PLANNED : RouteViewMode.ACTIVE;
        } else if (routeViewMode == RouteViewMode.PLANNED && !hasPlannedRoute) {
            routeViewMode = RouteViewMode.ACTIVE;
        }

        if (routeViewSelector != null) {
            routeViewSelector.setVisible(true);
            activeRouteViewButton.setEnabled(hasActiveRoute);
            plannedRouteViewButton.setEnabled(hasPlannedRoute);
            activeRouteViewButton.setSelected(routeViewMode == RouteViewMode.ACTIVE);
            plannedRouteViewButton.setSelected(routeViewMode == RouteViewMode.PLANNED);
            updateRouteViewButtonStyle(activeRouteViewButton, ACTIVE_ROUTE_COLOR);
            updateRouteViewButtonStyle(plannedRouteViewButton, PLANNED_ROUTE_COLOR);
            routeViewSelector.repaint();
        }

          int scrollPosition = routeView.getVerticalScrollBar().getValue();
          JumpPath displayedPath = routeViewMode == RouteViewMode.ACTIVE ? activePath : plannedPath;
        routeView.setViewportView((displayedPath == null) || displayedPath.isEmpty()
              ? createEmptyRouteView()
              : createJumpPathViewPanel(displayedPath, routeViewMode == RouteViewMode.PLANNED));
          SwingUtilities.invokeLater(() -> routeView.getVerticalScrollBar().setValue(scrollPosition));
    }

        private static void updateRouteViewButtonStyle(JToggleButton button, Color selectionColor) {
          button.setBackground(button.isSelected() ? HUD_CONTROL_HOVER_BACKGROUND : ROUTE_STRIP_BACKGROUND);
          button.setForeground(button.isEnabled()
              ? button.isSelected() ? selectionColor : ROUTE_TEXT_COLOR
              : ROUTE_MUTED_COLOR);
          button.setBorder(BorderFactory.createEmptyBorder());
        }

    private JumpPath getActiveRoute() {
        AbstractLocation currentLocation = getCampaign().getPlayerForce()
                                                   .getForceDetachment()
                                                   .getCurrentLocation();
        return (currentLocation == null) ? null : currentLocation.getJumpPath();
    }

    @Override
    public void cancelCurrentTrip() {
        AbstractLocation currentLocation = getCampaign().getPlayerForce()
                                                   .getForceDetachment()
                                                   .getCurrentLocation();
        if ((currentLocation == null) || (currentLocation.getJumpPath() == null)) {
            return;
        }
        currentLocation.setJumpPath(null);
        refreshPlanetView();
        panMap.repaint();
    }

    @Override
    public boolean hasActiveTrip() {
        return getActiveRoute() != null;
    }

    private void syncRouteDestinationToSelection() {
        if ((suggestRouteDestination != null) && (panMap != null)) {
            setRouteFieldSystem(suggestRouteDestination, panMap.getSelectedSystem());
        }
    }

    private JPanel createEmptyRouteView() {
        JPanel emptyView = new JPanel(new GridBagLayout());
        emptyView.setBackground(ROUTE_STRIP_BACKGROUND);
          emptyView.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(24), PADDING,
              UIUtil.scaleForGUI(24), PADDING));

        JPanel message = new JPanel(new GridBagLayout());
        message.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
          message.add(createRouteWorkspaceHeading(text("routePlanner.empty.eyebrow.text")),
              constraints);

          JLabel status = new JLabel(text("routePlanner.empty.text"));
        status.setForeground(ROUTE_TEXT_COLOR);
        status.setFont(status.getFont().deriveFont(Font.BOLD, status.getFont().getSize2D() * 1.3f));
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(UIUtil.scaleForGUI(4), 0, 0, 0);
        message.add(status, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        emptyView.add(message, constraints);
        return emptyView;
    }

    private static String text(String key) {
        return MHQInternationalization.getTextAt(RESOURCE_BUNDLE, key);
    }

    private void showDossier(PlanetarySystem system, int planetPosition) {
                DossierIdentity dossierIdentity = new DossierIdentity(system.getId(), planetPosition);
                boolean animateReveal = shouldAnimateDossierReveal(presentedDossierIdentity, dossierIdentity);
                setViewportViewAtTop(systemView,
                    new PlanetViewPanel(system, getCampaign(), planetPosition, animateReveal));
        presentedDossierIdentity = dossierIdentity;
    }

    static void setViewportViewAtTop(JScrollPane scrollPane, Component view) {
        scrollPane.setViewportView(view);
        resetViewportToTop(scrollPane, view);
        SwingUtilities.invokeLater(() -> resetViewportToTop(scrollPane, view));
    }

    private static void resetViewportToTop(JScrollPane scrollPane, Component expectedView) {
        JViewport viewport = scrollPane.getViewport();
        if (viewport.getView() == expectedView) {
            viewport.setViewPosition(new Point(0, 0));
        }
    }

        static boolean shouldAnimateDossierReveal(DossierIdentity previous,
                    DossierIdentity next) {
                return !next.equals(previous);
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
        boolean returningFromSystem = mapView.getView() == panSystem;
        mapView.setView(panMapView);
        refreshSystemView();
        if (returningFromSystem) {
            panMap.startSystemReturn();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (Objects.equals(e.getSource(), panMap)) {
            syncRouteDestinationToSelection();
            refreshSystemView();
        } else if (Objects.equals(e.getSource(), panSystem)) {
            refreshPlanetView();
        }
    }

    private enum RouteViewMode {
        ACTIVE,
        PLANNED
    }

    @Subscribe
    public void handle(NewDayEvent ev) {
        panMap.requestStaticCartographyPreparation();
        panMap.repaint();
        if (!hasPlannedRoute()) {
            routePlanningIntent.clear(getCampaign().getCurrentSystem());
            setRouteFieldSystem(suggestRouteOrigin, routePlanningIntent.getOrigin());
        }
        updateRouteStrip();
        suggestPlanet.setSuggestData(getCampaign().getSystemNames());
        suggestRouteOrigin.setSuggestData(getCampaign().getSystemNames());
        suggestRouteDestination.setSuggestData(getCampaign().getSystemNames());
    }

    @Subscribe
    public void handle(OptionsChangedEvent ev) {
        panMap.repaint();
        updateRouteStrip();
    }

    @Subscribe
    public void handle(LocationAddedEvent event) {
        panMap.refreshPresentationData();
        panSystem.repaint();
    }

    @Subscribe
    public void handle(LocationRemovedEvent event) {
        panMap.refreshPresentationData();
        panSystem.repaint();
    }

    @Subscribe
    public void handle(MissionEvent event) {
        panMap.refreshPresentationData();
    }

    @Subscribe
    public void handle(ScenarioEvent event) {
        panMap.refreshPresentationData();
    }

    private enum RouteStripState {
        NO_ROUTE,
        PLANNED_ROUTE,
        WHAT_IF_ROUTE,
        IN_TRANSIT
    }

        private record RouteStripSnapshot(RouteStripState state, String statusText, Color statusColor,
            String destinationText, String jumpsText, String durationText, String nextJumpText, String costText,
            Color metricColor, boolean costVisible, String quickPlotText, boolean quickPlotVisible,
            boolean quickPlotEnabled, boolean beginTransitEnabled) {
    }

    record DossierIdentity(String systemId, int planetPosition) {
        }

    private static final class RouteStripPanel extends JPanel {
        private float childAlpha = 1.0f;
        private float revealProgress;

        private RouteStripPanel() {
            super(new GridBagLayout());
        }

        private float getChildAlpha() {
            return childAlpha;
        }

        private void setChildAlpha(float childAlpha) {
            this.childAlpha = Math.max(0.0f, Math.min(1.0f, childAlpha));
            repaint();
        }

        private void setRevealProgress(float revealProgress) {
            this.revealProgress = Math.max(0.0f, Math.min(1.0f, revealProgress));
            repaint();
        }

        @Override
        protected void paintChildren(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            Composite originalComposite = graphics2D.getComposite();
            try {
                graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, childAlpha));
                super.paintChildren(graphics2D);
            } finally {
                graphics2D.setComposite(originalComposite);
                graphics2D.dispose();
            }
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            super.paintBorder(graphics);
            if (revealProgress <= 0.0f) {
                return;
            }

            float sweepProgress = Math.min(1.0f, revealProgress / ROUTE_REVEAL_SWEEP_END);
            float revealAlpha = revealProgress < ROUTE_REVEAL_SWEEP_END
                  ? ROUTE_REVEAL_MAX_ALPHA
                  : ROUTE_REVEAL_MAX_ALPHA * (1.0f - revealProgress) / (1.0f - ROUTE_REVEAL_SWEEP_END);
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            Composite originalComposite = graphics2D.getComposite();
            try {
                graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, revealAlpha));
                graphics2D.setColor(PLANNED_ROUTE_COLOR);
                graphics2D.drawLine(0, 0, Math.round((getWidth() - 1) * sweepProgress), 0);
            } finally {
                graphics2D.setComposite(originalComposite);
                graphics2D.dispose();
            }
        }
    }
}
