/*
 * Copyright (C) 2011-2026 The MegaMek Team. All Rights Reserved.
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

import static java.lang.Math.min;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllActiveBioweapons;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllActiveDiseases;

import java.awt.*;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicMenuItemUI;
import javax.swing.plaf.basic.BasicMenuUI;
import javax.vecmath.Vector2d;

import megamek.client.ui.util.UIUtil;
import megamek.client.ui.util.FontHandler;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.universe.FactionTag;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.NavigationRouteAnalysis;
import mekhq.campaign.NavigationRouteAnalysis.LegAssessment;
import mekhq.campaign.NavigationRouteAnalysis.PathAssessment;
import mekhq.campaign.NavigationRouteAnalysis.Severity;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.HPGLink;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.SocioIndustrialData;
import mekhq.campaign.universe.StarType;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.enums.CapitalType;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import mekhq.gui.baseComponents.ImmersiveComboBox;
import mekhq.gui.baseComponents.ImmersiveCheckBox;
import mekhq.gui.baseComponents.ImmersiveRadioButton;
import mekhq.gui.baseComponents.ImmersiveScrollBarStyle;
import mekhq.gui.baseComponents.ImmersiveSpinner;
import mekhq.gui.dialog.PlanetarySystemEditorDialog;

/**
 * This is not functional yet. Just testing things out. A lot of this code is borrowed from InterstellarMap.java in
 * MekWars
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class InterstellarMapPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(InterstellarMapPanel.class);
    private static final int MATERIAL_EDIT_SYMBOL = 0xE3C9;
    private static final int MATERIAL_STAR_SYMBOL = 0xE838;

    interface RoutePlanningHandler {
        void plotRoute(PlanetarySystem destination);

        void appendWaypoint(PlanetarySystem destination);

        void trimRouteAt(PlanetarySystem destination);

        void removeWaypoint(PlanetarySystem waypoint);

        void clearPlannedRoute();

        void cancelCurrentTrip();

        boolean hasPlannedRoute();

        boolean hasActiveTrip();

        boolean canTrimRouteAt(PlanetarySystem system);

        boolean isRequestedWaypoint(PlanetarySystem system);
    }

    private static final RoutePlanningHandler NO_ROUTE_PLANNING_HANDLER = new RoutePlanningHandler() {
        @Override
        public void plotRoute(PlanetarySystem destination) {
        }

        @Override
        public void appendWaypoint(PlanetarySystem destination) {
        }

        @Override
        public void trimRouteAt(PlanetarySystem destination) {
        }

        @Override
        public void removeWaypoint(PlanetarySystem waypoint) {
        }

        @Override
        public void clearPlannedRoute() {
        }

        @Override
        public void cancelCurrentTrip() {
        }

        @Override
        public boolean hasPlannedRoute() {
            return false;
        }

        @Override
        public boolean hasActiveTrip() {
            return false;
        }

        @Override
        public boolean canTrimRouteAt(PlanetarySystem system) {
            return false;
        }

        @Override
        public boolean isRequestedWaypoint(PlanetarySystem system) {
            return false;
        }
    };

    private static final String CURRENT_LOCATION_ICON_PATH =
          "data/images/universe/default_jumpship_fleet.png";
        private static final int CURRENT_LOCATION_ICON_SIZE = 34;
    private static final Color MAP_BACKGROUND_TOP = new Color(7, 16, 27);
    private static final Color MAP_BACKGROUND_BOTTOM = new Color(3, 8, 15);
    private static final Color MAP_GRID_MINOR = new Color(35, 66, 82, 45);
    private static final Color MAP_GRID_MAJOR = new Color(50, 91, 108, 75);
    private static final Color PLANNED_ROUTE_COLOR = new Color(65, 210, 224);
    private static final Color ACTIVE_ROUTE_COLOR = new Color(235, 166, 66);
    private static final Color ACTIVE_ROUTE_FLOW_COLOR = new Color(255, 226, 154);
    private static final Color MAP_POPUP_BACKGROUND = new Color(7, 16, 27);
    private static final Color MAP_POPUP_SELECTION_BACKGROUND = new Color(18, 45, 56);
    private static final Color MAP_POPUP_BORDER = new Color(35, 66, 82);
    private static final Color MAP_POPUP_TEXT = new Color(218, 231, 235);
    private static final Color MAP_POPUP_DISABLED_TEXT = new Color(132, 153, 161);
    private static final Color MAP_POPUP_SELECTION_TEXT = new Color(65, 210, 224);
    private static final Color CURRENT_LOCATION_COLOR = new Color(255, 190, 82);
    private static final Color SELECTED_SYSTEM_COLOR = ACTIVE_ROUTE_COLOR;
    private static final Color HOVERED_SYSTEM_COLOR = PLANNED_ROUTE_COLOR;
    private static final Color SYSTEM_LABEL_COLOR = new Color(218, 231, 235);
    private static final Color OPERATION_MARKER_COLOR = new Color(239, 170, 54);
    private static final Color URGENT_OPERATION_COLOR = new Color(255, 220, 122);
    private static final Color HPG_CLASS_A_COLOR = new Color(89, 226, 238);
    private static final Color HPG_CLASS_B_COLOR = new Color(105, 175, 255);
    private static final Color HPG_CLASS_C_COLOR = new Color(242, 184, 72);
    private static final Color HPG_CLASS_D_COLOR = new Color(234, 86, 86);
    private static final Color REACHABILITY_DEEP_COLOR = new Color(126, 169, 188);
    private static final Color NAVIGATION_CAUTION_COLOR = new Color(242, 184, 72);
    private static final Color NAVIGATION_BLOCKED_COLOR = new Color(234, 86, 86);
    private static final Color MEASUREMENT_COLOR = new Color(218, 231, 235);
    private static final int LAYER_ANIMATION_DELAY_MS = 16;
    private static final long LAYER_ANIMATION_DURATION_NS = 260_000_000L;
    private static final long MAP_MODE_ANIMATION_DURATION_NS = 300_000_000L;
    private static final int SELECTION_ANIMATION_DELAY_MS = 16;
    private static final long SELECTION_ANIMATION_DURATION_NS = 260_000_000L;
    private static final int PROPOSED_ROUTE_ANIMATION_DELAY_MS = 16;
    private static final long PROPOSED_ROUTE_BASE_LEG_DURATION_NS = 400_000_000L;
    private static final long PROPOSED_ROUTE_MIN_LEG_DURATION_NS = 90_000_000L;
    private static final int TRAVEL_ANIMATION_DELAY_MS = 16;
    private static final int SYSTEM_DIVE_ANIMATION_DELAY_MS = 16;
    private static final boolean RENDER_PROFILING_ENABLED = Boolean.getBoolean("mekhq.map.renderProfiling");
    private static final long RENDER_PROFILE_REPORT_INTERVAL_NS = 5_000_000_000L;
    private static final boolean RENDER_BENCHMARK_ENABLED = isRenderBenchmarkEnabled(RENDER_PROFILING_ENABLED,
          Boolean.getBoolean("mekhq.map.renderBenchmark"));
    private static final String RENDER_BENCHMARK_PATH_ID = "pan-v2";
    private static final int RENDER_BENCHMARK_DELAY_MS = 16;
    private static final long RENDER_BENCHMARK_WARMUP_NS = 5_000_000_000L;
    private static final long RENDER_BENCHMARK_DURATION_NS = 30_000_000_000L;
    private static final String TRANSITION_BENCHMARK_PATH_ID = "transition-cold-v1";
    private static final long TRANSITION_BENCHMARK_TIMEOUT_NS = 10_000_000_000L;
    private static final String ZOOM_BENCHMARK_PATH_ID = "zoom-v1";
    private static final long ZOOM_BENCHMARK_DURATION_NS = 8_000_000_000L;
    private static final long ZOOM_BENCHMARK_TIMEOUT_NS = 10_000_000_000L;
    private static final double ZOOM_WHEEL_FACTOR = 1.175;
    private static final int ZOOM_SETTLE_DELAY_MS = 180;
    private static final double MINIMUM_MAP_SCALE = 0.1;
    private static final double MAXIMUM_MAP_SCALE = 10.0;
    private static final double[] RENDER_BENCHMARK_X_OFFSETS = { 0.0, 640.0, 0.0, -640.0, 0.0, 0.0, 0.0,
        0.0, 0.0 };
    private static final double[] RENDER_BENCHMARK_Y_OFFSETS = { 0.0, 0.0, 0.0, 0.0, 0.0, 360.0, 0.0,
        -360.0, 0.0 };
    private static final double STATIC_AMBIENT_PHASE_SECONDS = 0.0;
    private static final long SYSTEM_DIVE_ANIMATION_DURATION_NS = 700_000_000L;
    private static final double SYSTEM_DIVE_MINIMUM_TARGET_SCALE = 18.0;
    private static final double SYSTEM_DIVE_MAXIMUM_TARGET_SCALE = 48.0;
    private static final long ROUTE_ACTIVATION_DURATION_NS = 550_000_000L;
    private static final long SYSTEM_HOP_DURATION_NS = 520_000_000L;
    private static final double SYSTEM_HOP_DEPARTURE_END_PROGRESS = 0.34;
    private static final double SYSTEM_HOP_ARRIVAL_START_PROGRESS = 0.64;
    private static final double FULL_CIRCLE_RADIANS = Math.PI * 2.0;
    private static final Color LAYER_CONTROL_BACKGROUND = new Color(5, 13, 23, 230);
    private static final Color LAYER_CONTROL_BORDER = new Color(65, 210, 224, 105);
    private static final Color LAYER_CONTROL_TEXT = new Color(198, 214, 220);
    private static final Color LAYER_CONTROL_BUTTON_HOVER = new Color(18, 45, 56, 245);
    private static final Color LAYER_CONTROL_BUTTON_PRESSED = new Color(33, 73, 82, 255);
    private static final Color LAYER_CONTROL_BUTTON_ICON = new Color(125, 230, 238);
    private static final Color NAVIGATION_UTILITY_BACKGROUND = new Color(15, 30, 43);
    private static final String LAYER_CONTROL_HEADING = "MAP LAYER";
    private static final String OVERLAY_CONTROL_HEADING = "OVERLAYS";
    private static final int MAP_LEGEND_CONTENT_WIDTH = 560;
    private static final int MAP_LEGEND_DIALOG_MARGIN = 16;
    private static final int MAP_LEGEND_BUTTON_SIZE = 24;
    private static final int MAP_LEGEND_SWATCH_WIDTH = 64;
    private static final int MAP_LEGEND_SWATCH_HEIGHT = 38;
    private static final int MAP_LEGEND_MAX_VIEWPORT_HEIGHT = 620;
    private static final Color MAP_LEGEND_BACKGROUND = new Color(5, 13, 23);
    private static final Color MAP_LEGEND_MUTED_TEXT = new Color(158, 179, 187);
    private static final Color MAP_LEGEND_DIVIDER = new Color(65, 210, 224, 45);
    private static final Color MAP_LEGEND_TITLE_BACKGROUND = new Color(17, 64, 78);
    private static final Color MAP_LEGEND_TITLE_FOREGROUND = new Color(222, 235, 239);
    private static final Color MAP_LEGEND_SCROLLBAR_THUMB = new Color(54, 101, 113);
    private static final int MAP_LEGEND_SCROLLBAR_WIDTH = 10;
    private static final Color PLAYER_BASE_COLOR = new Color(87, 214, 190);
    private static final Color PLAYER_BASE_DARK = new Color(4, 19, 25, 235);
    private static final Color NAVIGATION_INSTRUMENT_SHADOW = new Color(3, 8, 15, 215);
    private static final int NAVIGATION_INSTRUMENT_MARGIN = 14;
    private static final int NAVIGATION_INSTRUMENT_WIDTH = 280;
    private static final int NAVIGATION_INSTRUMENT_HEIGHT = 118;
    private static final int NAVIGATION_COMPASS_CENTER_X = 104;
    private static final int NAVIGATION_COMPASS_CENTER_Y = 36;
    private static final int NAVIGATION_COMPASS_RADIUS = 15;
    private static final int NAVIGATION_SCALE_BAR_INSET = 4;
    private static final int NAVIGATION_SCALE_BAR_WIDTH = 170;
    private static final int NAVIGATION_SCALE_BAR_Y = 96;
    private static final MathContext NAVIGATION_DISTANCE_FORMAT = new MathContext(3, RoundingMode.HALF_UP);
    private static final Stroke CONTRACT_SEARCH_RANGE_RING_STROKE = new BasicStroke(3.0f,
          BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 8, 6 }, 0);
    private static final Stroke PLANETARY_ACQUISITION_RANGE_RING_STROKE = new BasicStroke(3.0f,
          BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] { 9, 4, 2, 4 }, 0);
    private static final Stroke JUMP_RANGE_RING_STROKE = new BasicStroke(3.0f, BasicStroke.CAP_BUTT,
          BasicStroke.JOIN_BEVEL);
    private static final Stroke HPG_RANGE_RING_STROKE = new BasicStroke(1.5f, BasicStroke.CAP_BUTT,
          BasicStroke.JOIN_BEVEL, 0, new float[] { 2, 5 }, 0);
    private static final Color HPG_RANGE_RING_COLOR = new Color(0, 100, 50);
    private static final int GRID_TARGET_SPACING = 96;
    private static final long MAX_CACHED_RENDER_LAYER_PIXELS = 16_777_216L;
    private static final int RETAINED_CARTOGRAPHY_MAX_OVERSCAN = 1024;
    private static final ExecutorService RETAINED_TERRITORY_RENDERER =
          Executors.newSingleThreadExecutor(runnable -> {
              Thread thread = new Thread(runnable, "MekHQ retained territory renderer");
              thread.setDaemon(true);
              return thread;
          });
    private static final double TERRITORY_HEX_SIZE = 30.0;
    private static final double TERRITORY_HEX_SPACING_X = TERRITORY_HEX_SIZE * Math.sqrt(3) / 2.0;
    private static final double TERRITORY_HEX_RADIUS = TERRITORY_HEX_SIZE / Math.sqrt(3);
    private static final int TERRITORY_HEX_MARGIN = 2;
        private static final Stroke TERRITORY_CONTOUR_SOFTENING_STROKE = new BasicStroke(5.0f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Color TERRITORY_BORDER_DARK = new Color(2, 6, 10, 215);
    private static final Color TERRITORY_NEUTRAL_EDGE = new Color(198, 211, 214, 185);
    private static final Color TERRITORY_POCKET_FILL = new Color(1, 5, 9, 105);
    private static final double TERRITORY_LAYER_OPACITY = 0.72;
    private static final double FACTION_LOGO_OPACITY = 0.34;
    private static final int FACTION_LOGO_MIN_SIZE = 36;
    private static final int FACTION_LOGO_COMPACT_MIN_SIZE = 24;
    private static final int FACTION_LOGO_MAX_SIZE = 100;
    private static final int FACTION_LOGO_COLLISION_PADDING = 8;
    private static final BufferedImage CURRENT_LOCATION_ICON = loadCurrentLocationIcon();

    private enum MapMode {
        FACTION,
        TECHNOLOGY,
        INDUSTRY,
        RAW_MATERIALS,
        OUTPUT,
        AGRICULTURE,
        POPULATION,
        HPG,
        RECHARGE_STATIONS,
        ACADEMIES,
        HIRING_HALLS,
        DISEASE_OUTBREAKS
    }

    private enum TransitionBenchmarkPhase {
        WARMUP,
        MODE_TRANSITION,
        CACHE_REGENERATION,
        RESTORE
    }

    private enum ZoomBenchmarkPhase {
        WARMUP,
        ACTIVE_ZOOM,
        SETTLED_REGENERATION
    }

    private enum MapModeTransitionCacheStage {
        INACTIVE,
        CARTOGRAPHY,
        SYSTEMS,
        PREVIOUS,
        TARGET,
        READY
    }

    enum HpgNetworkDetail {
        CLASS_A("A only"),
        CLASS_A_B("A-B network"),
        ALL_STATIONS("A-D stations");

        private final String label;

        HpgNetworkDetail(String label) {
            this.label = label;
        }

        boolean includes(HPGRating rating) {
            return switch (rating) {
                case A -> true;
                case B -> this != CLASS_A;
                case C, D -> this == ALL_STATIONS;
                case X -> false;
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum CapitalDisplayDetail {
        NATIONAL("National only"),
        NATIONAL_REGION("National + Region"),
        ALL("All capitals");

        private final String label;

        CapitalDisplayDetail(String label) {
            this.label = label;
        }

        boolean includes(CapitalType capitalType) {
            return switch (capitalType) {
                case NATIONAL -> true;
                case REGION -> this != NATIONAL;
                case DISTRICT -> this == ALL;
                case NONE -> false;
            };
        }

        @Override
        public String toString() {
            return label;
        }
    }

        private enum MapLegendSymbol {
          FACTION_OWNERSHIP,
          LAYER_TECHNOLOGY,
          LAYER_INDUSTRY,
          LAYER_RAW_MATERIALS,
          LAYER_OUTPUT,
          LAYER_AGRICULTURE,
          LAYER_POPULATION,
          LAYER_HPG,
          HPG_STATIONS,
          LAYER_RECHARGE_STATIONS,
          LAYER_ACADEMIES,
          LAYER_HIRING_HALLS,
          LAYER_DISEASE_OUTBREAKS,
          FACTION_EMBLEM,
          SELECTED_SYSTEM,
          HOVERED_SYSTEM,
          CURRENT_FLEET,
          PLAYER_BASE,
          PLANNED_ROUTE,
          ACTIVE_ROUTE,
          WAYPOINT_BADGE,
          REACHABILITY,
          REACHABILITY_CAUTION,
          REACHABILITY_BLOCKED,
          ROUTE_CAUTION,
          ROUTE_BLOCKED,
          MEASUREMENT,
          CONTRACT_SEARCH_RADIUS,
          PLANETARY_ACQUISITION_RADIUS,
          JUMP_RADIUS,
          HPG_RANGE,
          CAPITAL_HIERARCHY,
          OPERATION,
          RESTRICTED_SYSTEM,
          GM_EDITED_SYSTEM,
          HPG_NETWORK,
          SOVEREIGN_TERRITORY,
          DISPUTED_TERRITORY,
          UNCLAIMED_POCKET,
          ENCLAVE
        }

        private record MapLegendEntry(MapLegendSymbol symbol, String title, String meaning) {
        }

        private record MapLegendSection(String heading, List<MapLegendEntry> entries) {
        }

        private static final List<MapLegendSection> MAP_LEGEND_SECTIONS = List.of(
            new MapLegendSection("NAVIGATION", List.of(
                new MapLegendEntry(MapLegendSymbol.SELECTED_SYSTEM, "Selected system",
                    "An amber ring identifies the selected system at distant zoom; corner brackets replace it as navigation detail appears."),
                new MapLegendEntry(MapLegendSymbol.HOVERED_SYSTEM, "Hovered system",
                    "A cyan ring identifies the system under the pointer at distant zoom; corner brackets replace it closer in. Selected systems suppress hover."),
                new MapLegendEntry(MapLegendSymbol.CURRENT_FLEET, "Current fleet",
                    "An amber JumpShip above-right of a system marks the fleet. At distant zoom, an amber ring surrounding the system replaces the ship."),
                new MapLegendEntry(MapLegendSymbol.PLAYER_BASE, "Player base",
                    "A teal marker identifies a system with player bases. Navigation detail adds the number of bases when more than one share the system."),
                new MapLegendEntry(MapLegendSymbol.MEASUREMENT,
                    getMapResource("map.legend.measurement.title"),
                    getMapResource("map.legend.measurement.description")))),
            new MapLegendSection("ROUTES", List.of(
                new MapLegendEntry(MapLegendSymbol.PLANNED_ROUTE, "Planned route",
                    "A cyan dashed path remains visible at distant zoom; complete thin stop rings appear with navigation detail."),
                new MapLegendEntry(MapLegendSymbol.ACTIVE_ROUTE, "Active route",
                    "Amber paths remain visible at distant zoom; complete stop rings appear with navigation detail."),
                new MapLegendEntry(MapLegendSymbol.WAYPOINT_BADGE, "Waypoint number",
                    "At navigation zoom, a numbered badge below-right of a system gives each requested route stop's order."),
                new MapLegendEntry(MapLegendSymbol.REACHABILITY,
                    getMapResource("map.legend.reachability.title"),
                    getMapResource("map.legend.reachability.description")),
                new MapLegendEntry(MapLegendSymbol.REACHABILITY_CAUTION,
                    getMapResource("map.legend.reachabilityCaution.title"),
                    getMapResource("map.legend.reachabilityCaution.description")),
                new MapLegendEntry(MapLegendSymbol.REACHABILITY_BLOCKED,
                    getMapResource("map.legend.reachabilityBlocked.title"),
                    getMapResource("map.legend.reachabilityBlocked.description")),
                new MapLegendEntry(MapLegendSymbol.ROUTE_CAUTION,
                    getMapResource("map.legend.routeCaution.title"),
                    getMapResource("map.legend.routeCaution.description")),
                new MapLegendEntry(MapLegendSymbol.ROUTE_BLOCKED,
                    getMapResource("map.legend.routeBlocked.title"),
                    getMapResource("map.legend.routeBlocked.description")))),
            new MapLegendSection("RANGE RINGS", List.of(
                new MapLegendEntry(MapLegendSymbol.CONTRACT_SEARCH_RADIUS, "Contract-search radius",
                    "A configurable-color thick dashed ring centered on the selected system bounds contract searches; campaign and MekHQ options control visibility."),
                new MapLegendEntry(MapLegendSymbol.PLANETARY_ACQUISITION_RADIUS, "Planetary-acquisition radius",
                    "A configurable-color thick dash-dot ring centered on the selected system bounds planetary acquisition; campaign, MekHQ, and zoom options control visibility."),
                new MapLegendEntry(MapLegendSymbol.JUMP_RADIUS, "Jump radius",
                    "A configurable-color thick solid ring centered on the selected system shows one-jump reach; MekHQ and zoom options control visibility."),
                new MapLegendEntry(MapLegendSymbol.HPG_RANGE, "50 ly HPG range",
                    "A dark-green dotted ring centered on the selected system marks 50 ly; HPG layer visibility controls when it appears."))),
            new MapLegendSection("SYSTEM STATUS", List.of(
                new MapLegendEntry(MapLegendSymbol.CAPITAL_HIERARCHY, "Capital hierarchy",
                    "National capitals use five-point stars, regional capitals use four-ray compass stars, and district capitals use simple pips. Layers controls the visible tiers; districts appear only at navigation detail."),
                new MapLegendEntry(MapLegendSymbol.OPERATION, "Operation flag",
                    "At distant zoom, a centered red diamond marks an urgent active scenario. Closer in, flags show missions, scenarios, and mission counts."),
                new MapLegendEntry(MapLegendSymbol.RESTRICTED_SYSTEM, "Restricted system",
                    "A red prohibition ring marks a system barred by outlaw or restricted-entry standing rules."),
                new MapLegendEntry(MapLegendSymbol.GM_EDITED_SYSTEM, "GM-edited system",
                    "At detail zoom, a cyan pencil below a system marks a non-canon override."))),
            new MapLegendSection("LAYERS", List.of(
                new MapLegendEntry(MapLegendSymbol.FACTION_OWNERSHIP, "Faction ownership",
                    "Compact contacts use faction color at navigation zoom; detail zoom shows intrinsic stars with crisp ownership rings. Shared systems divide both equally."),
                new MapLegendEntry(MapLegendSymbol.LAYER_TECHNOLOGY, "Technology",
                    "Regressed is dark gray; F purple; D blue; C teal; B green; A or Advanced yellow; no population is black."),
                new MapLegendEntry(MapLegendSymbol.LAYER_INDUSTRY, "Industry",
                    "F is near-black; D purple; C magenta; B coral; A pale yellow; no population is black."),
                new MapLegendEntry(MapLegendSymbol.LAYER_RAW_MATERIALS, "Raw Materials",
                    "F is blue; D purple; C magenta; B orange; A yellow; no population is black."),
                new MapLegendEntry(MapLegendSymbol.LAYER_OUTPUT, "Output",
                    "F is near-black; D purple; C magenta; B orange; A pale yellow; no population is black."),
                new MapLegendEntry(MapLegendSymbol.LAYER_AGRICULTURE, "Agriculture",
                    "F is dark blue; D blue-gray; C gray; B tan; A yellow; no population is black."),
                new MapLegendEntry(MapLegendSymbol.LAYER_POPULATION, "Population",
                    "Purple marks under 1M, then colors progress through violet, blue, teal, and green across 1M, 25M, 100M, 200M, 300M, 500M, 1B, and 1.5B; 3B+ is yellow; none is black."),
                new MapLegendEntry(MapLegendSymbol.LAYER_HPG, "HPG",
                    "No HPG is black; D dark gray; C light gray; B pink; A pale yellow."),
                new MapLegendEntry(MapLegendSymbol.LAYER_RECHARGE_STATIONS, "Recharge Stations",
                    "No station is gray; one station coral; two stations yellow; unavailable data black."),
                new MapLegendEntry(MapLegendSymbol.LAYER_ACADEMIES, "Academies",
                    "None is black; academy counts 1 through 6 progress from blue-teal through teal and green to yellow."),
                new MapLegendEntry(MapLegendSymbol.LAYER_HIRING_HALLS, "Hiring Halls",
                    "None is black; Questionable magenta; Minor orange; Standard yellow; Great green."),
                new MapLegendEntry(MapLegendSymbol.LAYER_DISEASE_OUTBREAKS, "Disease Outbreaks",
                    "None is black; one outbreak yellow; two orange; three magenta; four or more purple."))),
            new MapLegendSection("OVERLAYS", List.of(
                new MapLegendEntry(MapLegendSymbol.HPG_STATIONS, "HPG station classes",
                    "Hexagonal badges identify included HPG stations: A cyan, B blue, C amber, and D red. Network links are drawn only for A and B stations."),
                new MapLegendEntry(MapLegendSymbol.FACTION_EMBLEM, "Faction emblem",
                    "A faint emblem watermark identifies territory; its tint identifies the faction."),
                new MapLegendEntry(MapLegendSymbol.HPG_NETWORK, "HPG network",
                    "Layers controls maximum station detail. Distant zoom keeps only Class A links; navigation zoom adds Class B, and close zoom honors the selected station classes."),
                new MapLegendEntry(MapLegendSymbol.SOVEREIGN_TERRITORY, "Sovereign border",
                    "Translucent faction fill and a solid edge mark territory inferred from dated ownership."),
                new MapLegendEntry(MapLegendSymbol.DISPUTED_TERRITORY, "Disputed territory",
                    "Multiple faction colors, diagonal hatching, and a dashed border mark shared control."),
                new MapLegendEntry(MapLegendSymbol.UNCLAIMED_POCKET, "Unclaimed pocket",
                    "A dark enclosed void with a dotted boundary marks locally unclaimed space."),
                new MapLegendEntry(MapLegendSymbol.ENCLAVE, "Enclave",
                    "A closed double border marks one faction's territory enclosed by another."))));

        private static final class MapLegendSwatch extends JComponent {
          private final MapLegendSymbol symbol;

          private MapLegendSwatch(MapLegendEntry entry) {
            symbol = entry.symbol();
            Dimension swatchSize = UIUtil.scaleForGUI(MAP_LEGEND_SWATCH_WIDTH, MAP_LEGEND_SWATCH_HEIGHT);
            setPreferredSize(swatchSize);
            setMinimumSize(swatchSize);
            setMaximumSize(swatchSize);
            setFocusable(false);
            putClientProperty("mapLegendSwatch", Boolean.TRUE);
            putClientProperty("mapLegendTitle", entry.title());
          }

          @Override
          protected void paintComponent(Graphics graphics) {
            Graphics2D swatchGraphics = (Graphics2D) graphics.create();
            try {
                swatchGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                swatchGraphics.scale(getWidth() / (double) MAP_LEGEND_SWATCH_WIDTH,
                    getHeight() / (double) MAP_LEGEND_SWATCH_HEIGHT);
                swatchGraphics.setPaint(new GradientPaint(0, 0, MAP_BACKGROUND_TOP,
                    0, MAP_LEGEND_SWATCH_HEIGHT, MAP_BACKGROUND_BOTTOM));
                swatchGraphics.fillRect(0, 0, MAP_LEGEND_SWATCH_WIDTH, MAP_LEGEND_SWATCH_HEIGHT);
                paintMapLegendSymbol(swatchGraphics, symbol);
                swatchGraphics.setPaint(LAYER_CONTROL_BORDER);
                float borderWidth = 0.8f;
                double borderInset = borderWidth / 2.0;
                swatchGraphics.setStroke(new BasicStroke(borderWidth));
                swatchGraphics.draw(new Rectangle2D.Double(borderInset, borderInset,
                    MAP_LEGEND_SWATCH_WIDTH - borderWidth, MAP_LEGEND_SWATCH_HEIGHT - borderWidth));
            } finally {
                swatchGraphics.dispose();
            }
          }
        }

    enum RouteMarkerState {
        NONE,
        PLANNED,
        ACTIVE
    }

    enum NavigationMarkerShape {
        CIRCLE,
        SQUARE,
        HEXAGON,
        TRIANGLE,
        DIAMOND
    }

    enum NavigationMarkerTone {
        IMMEDIATE,
        DEEP,
        CAUTION,
        BLOCKED
    }

    record ReachabilityMarkerStyle(NavigationMarkerShape shape, NavigationMarkerTone tone) {
    }

    static ReachabilityMarkerStyle reachabilityMarkerStyle(int minimumHops, Severity severity,
          boolean blockedFrontier) {
        if (blockedFrontier || (severity == Severity.BLOCKED)) {
            return new ReachabilityMarkerStyle(NavigationMarkerShape.DIAMOND, NavigationMarkerTone.BLOCKED);
        }
        if (severity == Severity.CAUTION) {
            return new ReachabilityMarkerStyle(NavigationMarkerShape.TRIANGLE, NavigationMarkerTone.CAUTION);
        }
        return switch (minimumHops) {
            case 1 -> new ReachabilityMarkerStyle(NavigationMarkerShape.CIRCLE,
                  NavigationMarkerTone.IMMEDIATE);
            case 2 -> new ReachabilityMarkerStyle(NavigationMarkerShape.SQUARE, NavigationMarkerTone.DEEP);
            default -> new ReachabilityMarkerStyle(NavigationMarkerShape.HEXAGON, NavigationMarkerTone.DEEP);
        };
    }

    static BasicStroke reachabilityMarkerStroke(NavigationMarkerTone tone) {
        if (tone == NavigationMarkerTone.BLOCKED) {
            return new BasicStroke(2.2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER,
                  10.0f, new float[] { 4, 3 }, 0);
        }
        return new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    record RouteConstraintMarker(int legIndex, PlanetarySystem destination, LegAssessment assessment,
                                 boolean brokenSegment) {
    }

    static List<RouteConstraintMarker> routeConstraintMarkers(List<PlanetarySystem> routeSystems,
          PathAssessment assessment) {
        if ((routeSystems == null) || (assessment == null) || (routeSystems.size() < 2)) {
            return List.of();
        }
        int legCount = Math.min(routeSystems.size() - 1, assessment.legs().size());
        List<RouteConstraintMarker> markers = new ArrayList<>();
        for (int legIndex = 0; legIndex < legCount; legIndex++) {
            LegAssessment leg = assessment.legs().get(legIndex);
            if ((leg.severity() == Severity.CAUTION) || (leg.severity() == Severity.BLOCKED)) {
                markers.add(new RouteConstraintMarker(legIndex, routeSystems.get(legIndex + 1), leg,
                      leg.severity() == Severity.BLOCKED));
            }
        }
        return List.copyOf(markers);
    }

    record MeasurementState(boolean enabled, @Nullable PlanetarySystem start, @Nullable PlanetarySystem end) {
        static MeasurementState inactive() {
            return new MeasurementState(false, null, null);
        }

        static MeasurementState active() {
            return new MeasurementState(true, null, null);
        }

        MeasurementClick click(@Nullable PlanetarySystem system) {
            if (!enabled) {
                return new MeasurementClick(this, false);
            }
            if (system == null) {
                return new MeasurementClick(this, true);
            }
            if ((start == null) || (end != null)) {
                return new MeasurementClick(new MeasurementState(true, system, null), true);
            }
            return new MeasurementClick(new MeasurementState(true, start, system), true);
        }
    }

    record MeasurementClick(MeasurementState state, boolean consumed) {
    }

    static Rectangle clampMeasurementLabel(Rectangle viewport, Dimension labelSize, Point preferredCenter,
          List<Rectangle> exclusions) {
        int margin = Math.max(1, UIUtil.scaleForGUI(8));
        int gap = Math.max(1, UIUtil.scaleForGUI(6));
        int width = Math.min(labelSize.width, Math.max(1, viewport.width - (margin * 2)));
        int height = Math.min(labelSize.height, Math.max(1, viewport.height - (margin * 2)));
        int minimumX = viewport.x + margin;
        int maximumX = Math.max(minimumX, viewport.x + viewport.width - margin - width);
        int minimumY = viewport.y + margin;
        int maximumY = Math.max(minimumY, viewport.y + viewport.height - margin - height);
        int x = Math.clamp(preferredCenter.x - (width / 2), minimumX, maximumX);
        int y = Math.clamp(preferredCenter.y - height - gap, minimumY, maximumY);
        Rectangle label = new Rectangle(x, y, width, height);

        for (Rectangle exclusion : exclusions) {
            if ((exclusion == null) || !label.intersects(exclusion)) {
                continue;
            }
            int above = exclusion.y - gap - height;
            if (above >= minimumY) {
                label.y = above;
            } else {
                int right = exclusion.x + exclusion.width + gap;
                int left = exclusion.x - gap - width;
                label.x = right <= maximumX ? right : Math.max(minimumX, left);
            }
        }
        label.x = Math.clamp(label.x, minimumX, maximumX);
        label.y = Math.clamp(label.y, minimumY, maximumY);
        return label;
    }

    record TerritoryHex(int column, int row) {
    }

    enum TerritorySemantic {
        SOVEREIGN,
        DISPUTED,
        UNCLAIMED_EXTERIOR,
        UNCLAIMED_POCKET,
        ENCLAVE
    }

    record TerritoryCell(TerritoryHex hex, double centerX, double centerY, List<Faction> factions) {
    }

    record TerritoryComponent(Faction faction, TerritoryHex anchorHex, double anchorX, double anchorY,
          int cellCount, int interiorDepth, double minMapX, double maxMapX, double minMapY, double maxMapY) {
    }

    record TerritoryContour(List<Faction> factions, TerritorySemantic semantic, Shape shape, Paint paint,
          int cellCount, double minMapX, double maxMapX, double minMapY, double maxMapY) {
    }

    record TerritoryAtlas(LocalDate date, int minColumn, int maxColumn, int minRow, int maxRow,
            Map<TerritoryHex, TerritoryCell> cells, List<TerritoryContour> contours,
            List<TerritoryComponent> components) {
    }

    record RenderViewKey(int width, int height, long centerXBits, long centerYBits, long scaleBits) {
        static RenderViewKey create(int width, int height, double centerX, double centerY, double scale) {
            return new RenderViewKey(width, height, Double.doubleToLongBits(centerX),
                  Double.doubleToLongBits(centerY), Double.doubleToLongBits(scale));
        }
    }

    record MapQueryBounds(double minX, double minY, double maxX, double maxY) {
    }

    static MapQueryBounds retainedSystemQueryBounds(RenderViewKey viewKey, int overscan, double renderExtent) {
        double scale = Double.longBitsToDouble(viewKey.scaleBits());
        double centerX = Double.longBitsToDouble(viewKey.centerXBits());
        double centerY = Double.longBitsToDouble(viewKey.centerYBits());
        double screenMargin = overscan + renderExtent;
        double minX = (-screenMargin - (viewKey.width() / 2.0)) / scale - centerX;
        double maxX = (viewKey.width() + screenMargin - (viewKey.width() / 2.0)) / scale - centerX;
        double minY = ((viewKey.height() / 2.0) - viewKey.height() - screenMargin) / scale + centerY;
        double maxY = ((viewKey.height() / 2.0) + screenMargin) / scale + centerY;
        return new MapQueryBounds(minX, minY, maxX, maxY);
    }

        static MapQueryBounds viewportSystemQueryBounds(RenderViewKey viewKey, double markerExtent,
                    double rightVisualExtent) {
        double scale = Double.longBitsToDouble(viewKey.scaleBits());
        double centerX = Double.longBitsToDouble(viewKey.centerXBits());
        double centerY = Double.longBitsToDouble(viewKey.centerYBits());
                double rightExtent = Math.max(markerExtent, rightVisualExtent);
        double minX = (-rightExtent - (viewKey.width() / 2.0)) / scale - centerX;
        double maxX = (viewKey.width() + markerExtent - (viewKey.width() / 2.0)) / scale - centerX;
        double minY = ((viewKey.height() / 2.0) - viewKey.height() - markerExtent) / scale + centerY;
        double maxY = ((viewKey.height() / 2.0) + markerExtent) / scale + centerY;
        return new MapQueryBounds(minX, minY, maxX, maxY);
    }

    record MapCenter(double x, double y) {
    }

    record SystemDiveFrame(double centerX, double centerY, double scale) {
    }

        record RenderBenchmarkCamera(double centerX, double centerY, double scale) {
        }

        record RenderBenchmarkRun(RenderBenchmarkCamera origin, int width, int height, LocalDate date,
              String mapMode, boolean territory, boolean hpgNetwork, boolean operations, boolean reachability,
              boolean emptySystems) {
        }

          record TransitionBenchmarkRun(RenderBenchmarkCamera origin, int width, int height, LocalDate date,
              MapMode originMode, MapMode targetMode, boolean territory, boolean hpgNetwork, boolean operations,
              boolean reachability, boolean emptySystems) {
          }

                record ZoomBenchmarkRange(double atlasScale, double detailScale) {
                }

                record ZoomBenchmarkRun(RenderBenchmarkCamera origin, int width, int height, LocalDate date,
                            MapMode mapMode, boolean territory, boolean hpgNetwork, boolean operations, boolean reachability,
                            boolean emptySystems, int anchorX, int anchorY, double semanticReference,
                            ZoomBenchmarkRange range) {
                }

        static boolean isRenderBenchmarkEnabled(boolean profilingEnabled, boolean benchmarkRequested) {
            return profilingEnabled && benchmarkRequested;
        }

                static boolean isExactTransitionBenchmarkFrame(boolean mapModeAnimating,
                            boolean retainedCartographyProvisional, boolean retainedCartography,
                            boolean mergedNavigation) {
                        return !mapModeAnimating && !retainedCartographyProvisional
                                    && retainedCartography && mergedNavigation;
                }

                static boolean isExactZoomBenchmarkFrame(boolean retainedCartographyProvisional,
                            boolean retainedCartography, boolean mergedNavigation) {
                        return !retainedCartographyProvisional && retainedCartography && mergedNavigation;
                }

        static RenderBenchmarkCamera renderBenchmarkCameraAt(RenderBenchmarkCamera origin, double progress) {
          double clampedProgress = Math.clamp(progress, 0.0, 1.0);
          double pathPosition = clampedProgress * (RENDER_BENCHMARK_X_OFFSETS.length - 1);
          int startIndex = Math.min((int) pathPosition, RENDER_BENCHMARK_X_OFFSETS.length - 2);
          double segmentProgress = pathPosition - startIndex;
          double xOffset = Math.rint(interpolate(RENDER_BENCHMARK_X_OFFSETS[startIndex],
              RENDER_BENCHMARK_X_OFFSETS[startIndex + 1], segmentProgress));
          double yOffset = Math.rint(interpolate(RENDER_BENCHMARK_Y_OFFSETS[startIndex],
              RENDER_BENCHMARK_Y_OFFSETS[startIndex + 1], segmentProgress));
          return new RenderBenchmarkCamera(origin.centerX() + (xOffset / origin.scale()),
              origin.centerY() + (yOffset / origin.scale()), origin.scale());
        }

        static ZoomBenchmarkRange zoomBenchmarkRange(double semanticReference) {
            double boundedReference = Math.clamp(semanticReference, 2.4, 3.6);
            double atlasEnd = Math.clamp(boundedReference / 3.0, 0.8, 1.0);
            double fullSystemDetail = Math.clamp(boundedReference * 1.6, 4.2, 5.6);
            return new ZoomBenchmarkRange(atlasEnd * 0.95, fullSystemDetail * 1.05);
        }

        static boolean isValidZoomBenchmarkOrigin(RenderBenchmarkCamera origin) {
            return Double.isFinite(origin.centerX()) && Double.isFinite(origin.centerY())
                  && Double.isFinite(origin.scale())
                  && (origin.scale() >= MINIMUM_MAP_SCALE) && (origin.scale() <= MAXIMUM_MAP_SCALE);
        }

        static double boundedMapScale(double scale) {
            return Math.clamp(scale, MINIMUM_MAP_SCALE, MAXIMUM_MAP_SCALE);
        }

        static RenderBenchmarkCamera zoomBenchmarkCameraAt(RenderBenchmarkCamera origin, int width, int height,
              int anchorX, int anchorY, ZoomBenchmarkRange range, double progress) {
            double clampedProgress = Math.clamp(progress, 0.0, 1.0);
            if ((clampedProgress == 0.0) || (clampedProgress == 1.0)) {
                return origin;
            }

            double pathPosition = clampedProgress * 4.0;
            int segment = Math.min((int) pathPosition, 3);
            double segmentProgress = pathPosition - segment;
            double startScale = switch (segment) {
                case 0 -> origin.scale();
                case 1, 3 -> range.atlasScale();
                case 2 -> range.detailScale();
                default -> throw new IllegalStateException("Unexpected zoom benchmark segment: " + segment);
            };
            double endScale = switch (segment) {
                case 0, 2 -> range.atlasScale();
                case 1 -> range.detailScale();
                case 3 -> origin.scale();
                default -> throw new IllegalStateException("Unexpected zoom benchmark segment: " + segment);
            };
            double scale = zoomBenchmarkScaleAt(startScale, endScale, segmentProgress);
            return zoomBenchmarkCameraAtScale(origin, width, height, anchorX, anchorY, scale);
        }

        static double zoomBenchmarkScaleAt(double startScale, double endScale, double progress) {
            double scaleDistance = Math.abs(Math.log(endScale / startScale));
            int stepCount = Math.max(1, (int) Math.ceil(scaleDistance / Math.log(ZOOM_WHEEL_FACTOR)));
            int completedSteps = Math.min(stepCount,
                  (int) Math.floor(Math.clamp(progress, 0.0, 1.0) * stepCount));
            double stepProgress = (double) completedSteps / stepCount;
            return Math.exp(interpolate(Math.log(startScale), Math.log(endScale), stepProgress));
        }

        private static RenderBenchmarkCamera zoomBenchmarkCameraAtScale(RenderBenchmarkCamera origin,
              int width, int height, int anchorX, int anchorY, double scale) {
            double anchorMapX = (anchorX - width / 2.0) / origin.scale() - origin.centerX();
            double anchorMapY = (height / 2.0 - anchorY) / origin.scale() + origin.centerY();
            double centerX = (anchorX - width / 2.0) / scale - anchorMapX;
            double centerY = anchorMapY - (height / 2.0 - anchorY) / scale;
            return new RenderBenchmarkCamera(centerX, centerY, scale);
        }

    record TerritoryRenderKey(RenderViewKey viewKey, LocalDate date, long dataRevision) {
    }

    record PannableRenderLayer(BufferedImage image, int drawX, int drawY) {
    }

        record PannableRenderLayerSnapshot<K>(K key, RenderViewKey renderedView,
            BufferedImage image, int overscan) {
        }

    static final class RenderPerformanceTracker {
        private long[] frameSamplesNanos = new long[512];
        private int frameSampleCount;
        private long reportStartedNanos;
        private long frameCount;
        private long totalNanos;
        private long staticNanos;
        private long backgroundNanos;
        private long territoryNanos;
        private long factionLogoNanos;
        private long routeNanos;
        private long systemNanos;
        private long overlayNanos;
        private long maximumNanos;
        private long framesOver16Millis;
        private long framesOver33Millis;
        private long visibleSystemCount;
        private long territoryCacheHits;
        private long territoryStripRefreshes;
        private long territoryFullRenders;
        private long retainedCartographyFrames;
        private long mergedNavigationFrames;
        private long cacheHitFrameCount;
        private long cacheHitFrameNanos;
        private long cacheHitFrameMaximumNanos;
        private long stripRefreshFrameCount;
        private long stripRefreshFrameNanos;
        private long stripRefreshFrameMaximumNanos;
        private long fullRenderFrameCount;
        private long fullRenderFrameNanos;
        private long fullRenderFrameMaximumNanos;
        private long uncachedFrameCount;
        private long uncachedFrameNanos;
        private long uncachedFrameMaximumNanos;
        private long retainedRenderCount;
        private long retainedCartographyNanos;
        private long retainedRouteNanos;
        private long retainedHpgNanos;
        private long retainedActiveRouteNanos;
        private long retainedSystemNanos;
        private long zoomSnapshotFrameCount;
        private long zoomSnapshotFrameNanos;
        private long zoomSnapshotFrameMaximumNanos;
        private long zoomSnapshotTransformNanos;
        private long zoomSnapshotBackgroundNanos;
        private long zoomSnapshotSystemNanos;
        private long zoomSnapshotResidualNanos;
        private boolean zoomSnapshotPaintedInCurrentFrame;
        private long currentZoomSnapshotTransformNanos;

        RenderPerformanceTracker(long nowNanos) {
            reportStartedNanos = nowNanos;
        }

        void record(long frameNanos, long staticPhaseNanos, long backgroundLayerNanos, long territoryLayerNanos,
              long factionLogoLayerNanos, long routePhaseNanos, long systemPhaseNanos, long overlayPhaseNanos,
              int visibleSystems, int cacheHits, int stripRefreshes, int fullRenders,
              boolean retainedCartography, boolean mergedNavigation) {
            if (frameSampleCount == frameSamplesNanos.length) {
                frameSamplesNanos = Arrays.copyOf(frameSamplesNanos, frameSamplesNanos.length * 2);
            }
            frameSamplesNanos[frameSampleCount++] = frameNanos;
            frameCount++;
            totalNanos += frameNanos;
            staticNanos += staticPhaseNanos;
                        backgroundNanos += backgroundLayerNanos;
                        territoryNanos += territoryLayerNanos;
                        factionLogoNanos += factionLogoLayerNanos;
            routeNanos += routePhaseNanos;
            systemNanos += systemPhaseNanos;
            overlayNanos += overlayPhaseNanos;
            maximumNanos = Math.max(maximumNanos, frameNanos);
            framesOver16Millis += frameNanos > 16_000_000L ? 1 : 0;
            framesOver33Millis += frameNanos > 33_000_000L ? 1 : 0;
            visibleSystemCount += visibleSystems;
            territoryCacheHits += cacheHits;
            territoryStripRefreshes += stripRefreshes;
            territoryFullRenders += fullRenders;
            retainedCartographyFrames += retainedCartography ? 1 : 0;
            mergedNavigationFrames += mergedNavigation ? 1 : 0;
            recordCacheOutcome(frameNanos, cacheHits, stripRefreshes, fullRenders);
            recordZoomSnapshotFrame(frameNanos, backgroundLayerNanos, systemPhaseNanos);
        }

        boolean shouldReport(long nowNanos) {
            return (frameCount > 0) && ((nowNanos - reportStartedNanos) >= RENDER_PROFILE_REPORT_INTERVAL_NS);
        }

        void recordRetainedRender(long cartographyNanos, long routeNanos, long hpgNanos,
              long activeRouteNanos, long systemNanos) {
            retainedRenderCount++;
            retainedCartographyNanos += cartographyNanos;
            retainedRouteNanos += routeNanos;
            retainedHpgNanos += hpgNanos;
            retainedActiveRouteNanos += activeRouteNanos;
            retainedSystemNanos += systemNanos;
        }

        void recordZoomSnapshotTransform(long transformNanos) {
            zoomSnapshotPaintedInCurrentFrame = true;
            currentZoomSnapshotTransformNanos += transformNanos;
        }

        String reportAndReset(long nowNanos) {
            Arrays.sort(frameSamplesNanos, 0, frameSampleCount);
            String report = String.format(
                "Map render: frames=%d avg=%.1fms p50=%.1fms p95=%.1fms p99=%.1fms max=%.1fms "
                    + ">16ms=%d >33ms=%d "
                    + "static=%.1fms [background=%.1fms territory=%.1fms logos=%.1fms] "
                    + "routes/hpg=%.1fms systems=%.1fms overlays=%.1fms visibleSystems=%.0f "
                    + "territoryCache[hits=%d strips=%d full=%d] retainedFrames=%d/%d mergedFrames=%d/%d "
                    + "cacheFrames[hit=%d avg=%.1fms max=%.1fms; strip=%d avg=%.1fms max=%.1fms; "
                    + "full=%d avg=%.1fms max=%.1fms; none=%d avg=%.1fms max=%.1fms] "
                    + "zoomSnapshots[count=%d frame=%.1fms max=%.1fms transform=%.1fms "
                    + "background=%.1fms liveSystems=%.1fms residual=%.1fms] "
                    + "cachePaints[count=%d cartography=%.1fms routes=%.1fms hpg=%.1fms active=%.1fms systems=%.1fms]",
                  frameCount, millis(totalNanos) / frameCount, millis(percentile(0.50)), millis(percentile(0.95)),
                  millis(percentile(0.99)), millis(maximumNanos), framesOver16Millis, framesOver33Millis,
                  millis(staticNanos) / frameCount, millis(backgroundNanos) / frameCount,
                  millis(territoryNanos) / frameCount, millis(factionLogoNanos) / frameCount,
                  millis(routeNanos) / frameCount,
                  millis(systemNanos) / frameCount, millis(overlayNanos) / frameCount,
                  (double) visibleSystemCount / frameCount, territoryCacheHits, territoryStripRefreshes,
                  territoryFullRenders, retainedCartographyFrames, frameCount, mergedNavigationFrames, frameCount,
                  cacheHitFrameCount, averageMillis(cacheHitFrameNanos, cacheHitFrameCount),
                  millis(cacheHitFrameMaximumNanos), stripRefreshFrameCount,
                  averageMillis(stripRefreshFrameNanos, stripRefreshFrameCount),
                  millis(stripRefreshFrameMaximumNanos), fullRenderFrameCount,
                  averageMillis(fullRenderFrameNanos, fullRenderFrameCount),
                  millis(fullRenderFrameMaximumNanos), uncachedFrameCount,
                  averageMillis(uncachedFrameNanos, uncachedFrameCount), millis(uncachedFrameMaximumNanos),
                  zoomSnapshotFrameCount, averageMillis(zoomSnapshotFrameNanos, zoomSnapshotFrameCount),
                  millis(zoomSnapshotFrameMaximumNanos),
                  averageMillis(zoomSnapshotTransformNanos, zoomSnapshotFrameCount),
                  averageMillis(zoomSnapshotBackgroundNanos, zoomSnapshotFrameCount),
                  averageMillis(zoomSnapshotSystemNanos, zoomSnapshotFrameCount),
                  averageMillis(zoomSnapshotResidualNanos, zoomSnapshotFrameCount),
                  retainedRenderCount, averageMillis(retainedCartographyNanos, retainedRenderCount),
                  averageMillis(retainedRouteNanos, retainedRenderCount),
                  averageMillis(retainedHpgNanos, retainedRenderCount),
                  averageMillis(retainedActiveRouteNanos, retainedRenderCount),
                  averageMillis(retainedSystemNanos, retainedRenderCount));
            reset(nowNanos);
            return report;
        }

        boolean hasSamples() {
            return frameCount > 0;
        }

        void reset(long nowNanos) {
            reportStartedNanos = nowNanos;
            frameSampleCount = 0;
            frameCount = 0;
            totalNanos = 0;
            staticNanos = 0;
            backgroundNanos = 0;
            territoryNanos = 0;
            factionLogoNanos = 0;
            routeNanos = 0;
            systemNanos = 0;
            overlayNanos = 0;
            maximumNanos = 0;
            framesOver16Millis = 0;
            framesOver33Millis = 0;
            visibleSystemCount = 0;
            territoryCacheHits = 0;
            territoryStripRefreshes = 0;
            territoryFullRenders = 0;
            retainedCartographyFrames = 0;
            mergedNavigationFrames = 0;
            cacheHitFrameCount = 0;
            cacheHitFrameNanos = 0;
            cacheHitFrameMaximumNanos = 0;
            stripRefreshFrameCount = 0;
            stripRefreshFrameNanos = 0;
            stripRefreshFrameMaximumNanos = 0;
            fullRenderFrameCount = 0;
            fullRenderFrameNanos = 0;
            fullRenderFrameMaximumNanos = 0;
            uncachedFrameCount = 0;
            uncachedFrameNanos = 0;
            uncachedFrameMaximumNanos = 0;
            retainedRenderCount = 0;
            retainedCartographyNanos = 0;
            retainedRouteNanos = 0;
            retainedHpgNanos = 0;
            retainedActiveRouteNanos = 0;
            retainedSystemNanos = 0;
            zoomSnapshotFrameCount = 0;
            zoomSnapshotFrameNanos = 0;
            zoomSnapshotFrameMaximumNanos = 0;
            zoomSnapshotTransformNanos = 0;
            zoomSnapshotBackgroundNanos = 0;
            zoomSnapshotSystemNanos = 0;
            zoomSnapshotResidualNanos = 0;
            zoomSnapshotPaintedInCurrentFrame = false;
            currentZoomSnapshotTransformNanos = 0;
        }

        private void recordZoomSnapshotFrame(long frameNanos, long backgroundLayerNanos,
              long systemPhaseNanos) {
            if (!zoomSnapshotPaintedInCurrentFrame) {
                return;
            }
            zoomSnapshotFrameCount++;
            zoomSnapshotFrameNanos += frameNanos;
            zoomSnapshotFrameMaximumNanos = Math.max(zoomSnapshotFrameMaximumNanos, frameNanos);
            zoomSnapshotTransformNanos += currentZoomSnapshotTransformNanos;
            zoomSnapshotBackgroundNanos += backgroundLayerNanos;
            zoomSnapshotSystemNanos += systemPhaseNanos;
            zoomSnapshotResidualNanos += Math.max(0L,
                  frameNanos - currentZoomSnapshotTransformNanos - backgroundLayerNanos - systemPhaseNanos);
            zoomSnapshotPaintedInCurrentFrame = false;
            currentZoomSnapshotTransformNanos = 0;
        }

        private void recordCacheOutcome(long frameNanos, int cacheHits, int stripRefreshes, int fullRenders) {
            if (fullRenders > 0) {
                fullRenderFrameCount++;
                fullRenderFrameNanos += frameNanos;
                fullRenderFrameMaximumNanos = Math.max(fullRenderFrameMaximumNanos, frameNanos);
            } else if (stripRefreshes > 0) {
                stripRefreshFrameCount++;
                stripRefreshFrameNanos += frameNanos;
                stripRefreshFrameMaximumNanos = Math.max(stripRefreshFrameMaximumNanos, frameNanos);
            } else if (cacheHits > 0) {
                cacheHitFrameCount++;
                cacheHitFrameNanos += frameNanos;
                cacheHitFrameMaximumNanos = Math.max(cacheHitFrameMaximumNanos, frameNanos);
            } else {
                uncachedFrameCount++;
                uncachedFrameNanos += frameNanos;
                uncachedFrameMaximumNanos = Math.max(uncachedFrameMaximumNanos, frameNanos);
            }
        }

        private long percentile(double quantile) {
            int index = Math.clamp((int) Math.ceil(frameSampleCount * quantile) - 1, 0, frameSampleCount - 1);
            return frameSamplesNanos[index];
        }

        private static double millis(long nanos) {
            return nanos / 1_000_000.0;
        }

        private static double averageMillis(long nanos, long count) {
            return count == 0 ? 0.0 : millis(nanos) / count;
        }
    }

    record FactionLogoRenderKey(TerritoryRenderKey territoryKey, int majorMinimumSize,
          int compactMinimumSize, int maximumSize, int collisionPadding, int shadowOffset) {
    }

    record TerritoryDataKey(LocalDate date, long dataRevision) {
    }

            record RetainedCartographyRenderRequest(RetainedCartographyKey key, RenderViewKey viewKey,
                int overscan, TerritoryAtlas atlas, double territoryAlpha) {
        }

        record RetainedCartographyKey(TerritoryDataKey dataKey, MapMode mapMode,
            HpgNetworkDetail hpgNetworkDetail, boolean showEmptySystems, long territoryAlphaBits,
                long factionLogoAlphaBits, long hpgNetworkAlphaBits, long systemContactAlphaBits,
                long systemDetailAlphaBits, long serviceAlphaBits, long systemSizeBits,
                int logoMajorMinimumSize, int logoCompactMinimumSize, int logoMaximumSize,
                int logoCollisionPadding, int logoShadowOffset) {
        }

    record RetainedNavigationKey(RetainedCartographyKey cartographyKey,
          List<String> proposedRouteSystemIds, List<String> activeRouteSystemIds,
          long reachabilityRevision) {
        RetainedNavigationKey {
            proposedRouteSystemIds = List.copyOf(proposedRouteSystemIds);
            activeRouteSystemIds = List.copyOf(activeRouteSystemIds);
        }
    }

    record SystemRenderDataKey(LocalDate date, long dataRevision) {
    }

    record MapPresentationData(Map<String, StrategicMarker> strategicMarkers,
          Map<String, Integer> playerBaseCounts, Set<Faction> contractEmployers,
          Set<Faction> contractTargets) {
        MapPresentationData {
            strategicMarkers = Map.copyOf(strategicMarkers);
            playerBaseCounts = Map.copyOf(playerBaseCounts);
            contractEmployers = Set.copyOf(contractEmployers);
            contractTargets = Set.copyOf(contractTargets);
        }

        static MapPresentationData empty() {
            return new MapPresentationData(Map.of(), Map.of(), Set.of(), Set.of());
        }
    }

        record SystemRenderData(Set<Faction> factions, List<Color> factionColors, List<Faction> capitalFactions,
            CapitalType capitalType, boolean empty, long population, HPGRating hpgRating, String printableName,
            @Nullable String stellarDetail) {
          static SystemRenderData create(PlanetarySystem system, LocalDate date,
              Map<Faction, String> capitals, @Nullable Faction mercenaryFaction,
              @Nullable String mercenaryCapitalSystem) {
            Set<Faction> datedFactions = system.getFactionSet(date);
            Set<Faction> factions = datedFactions == null ? Set.of() : Set.copyOf(datedFactions);
            List<Faction> orderedFactions = factions.stream()
                  .sorted(Comparator.comparing(Faction::getShortName))
                  .toList();
            boolean empty = factions.isEmpty()
                  || factions.stream().allMatch(faction -> faction.is(FactionTag.ABANDONED));
            String stellarDetail = system.getStar() == null ? null : "  [" + system.getStar() + "]";
            return new SystemRenderData(factions, orderedFactions.stream().map(Faction::getColor).toList(),
                resolveDatedCapitalFactions(factions, capitals, system.getId(), mercenaryFaction,
                    mercenaryCapitalSystem), system.getCapitalType(date), empty, system.getPopulation(date),
                ObjectUtility.nonNull(system.getHPG(date), HPGRating.X), system.getPrintableName(date),
                stellarDetail);
        }
    }

    static final class SystemSpatialIndex {
        private record IndexedSystem(int ordinal, double x, double y) {
        }

        private final List<PlanetarySystem> systems;
        private final List<IndexedSystem> systemsByX;
        private final Map<String, Integer> ordinalsById;

        SystemSpatialIndex(List<PlanetarySystem> systems) {
            this.systems = List.copyOf(systems);
            systemsByX = new ArrayList<>(systems.size());
            ordinalsById = new HashMap<>(systems.size());
            for (int ordinal = 0; ordinal < systems.size(); ordinal++) {
                PlanetarySystem system = systems.get(ordinal);
                systemsByX.add(new IndexedSystem(ordinal, system.getX(), system.getY()));
                ordinalsById.put(system.getId(), ordinal);
            }
            systemsByX.sort(Comparator.comparingDouble(IndexedSystem::x)
                  .thenComparingInt(IndexedSystem::ordinal));
        }

        List<PlanetarySystem> query(double minX, double minY, double maxX, double maxY,
              @Nullable PlanetarySystem firstRequired, @Nullable PlanetarySystem secondRequired) {
            BitSet matches = new BitSet(systems.size());
            for (int index = lowerBound(minX); index < systemsByX.size(); index++) {
                IndexedSystem indexedSystem = systemsByX.get(index);
                if (indexedSystem.x() > maxX) {
                    break;
                }
                if ((indexedSystem.y() >= minY) && (indexedSystem.y() <= maxY)) {
                    matches.set(indexedSystem.ordinal());
                }
            }
            includeRequired(matches, firstRequired);
            includeRequired(matches, secondRequired);

            List<PlanetarySystem> result = new ArrayList<>(matches.cardinality());
            for (int ordinal = matches.nextSetBit(0); ordinal >= 0; ordinal = matches.nextSetBit(ordinal + 1)) {
                result.add(systems.get(ordinal));
            }
            return result;
        }

        private int lowerBound(double x) {
            int low = 0;
            int high = systemsByX.size();
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (systemsByX.get(middle).x() < x) {
                    low = middle + 1;
                } else {
                    high = middle;
                }
            }
            return low;
        }

        private void includeRequired(BitSet matches, @Nullable PlanetarySystem requiredSystem) {
            Integer ordinal = requiredSystem == null ? null : ordinalsById.get(requiredSystem.getId());
            if (ordinal != null) {
                matches.set(ordinal);
            }
        }
    }

    record RenderCacheDiagnostics(int backgroundRenderCount, int territoryRenderCount,
          int factionLogoRenderCount, int territoryPreparationCount, boolean backgroundImageRetained,
          boolean territoryImageRetained, boolean factionLogoImageRetained) {
    }

    static final class RenderLayerCache<K> {
        private K key;
        private BufferedImage image;
        private int renderCount;

        BufferedImage getOrRender(K requestedKey, int width, int height, Consumer<Graphics2D> renderer) {
            boolean hasMatchingDimensions = (image != null) && (image.getWidth() == width)
                  && (image.getHeight() == height);
            if (hasMatchingDimensions && requestedKey.equals(key)) {
                return image;
            }

            BufferedImage renderedImage;
            if (hasMatchingDimensions) {
                renderedImage = image;
            } else {
                clear();
                renderedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            }
            Graphics2D graphics = renderedImage.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, width, height);
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                renderer.accept(graphics);
            } catch (RuntimeException exception) {
                renderedImage.flush();
                key = null;
                image = null;
                throw exception;
            } finally {
                graphics.dispose();
            }
            key = requestedKey;
            image = renderedImage;
            renderCount++;
            return renderedImage;
        }

        void clear() {
            if (image != null) {
                image.flush();
            }
            key = null;
            image = null;
        }

        int getRenderCount() {
            return renderCount;
        }

        boolean hasImage() {
            return image != null;
        }
    }

    static final class PannableRenderLayerCache<K> {
        private K key;
        private RenderViewKey renderedView;
        private BufferedImage image;
        private int overscan;
        private int renderCount;
        private int reuseCount;
        private int stripRefreshCount;
        private int fullRenderCount;

        PannableRenderLayer getOrRender(K requestedKey, RenderViewKey requestedView, int requestedOverscan,
              Consumer<Graphics2D> renderer) {
                        return getOrRender(requestedKey, requestedView, requestedOverscan, 0, renderer);
                }

                PannableRenderLayer getOrRender(K requestedKey, RenderViewKey requestedView, int requestedOverscan,
                            int requiredMargin, Consumer<Graphics2D> renderer) {
                        if ((requiredMargin < 0) || (requiredMargin > requestedOverscan)) {
                                throw new IllegalArgumentException("Required margin must fit within overscan");
                        }
                    PannableRenderLayer availableLayer = getOrRefresh(
                          requestedKey, requestedView, requestedOverscan, requiredMargin, renderer);
                    if (availableLayer != null) {
                        return availableLayer;
            }

            int width = requestedView.width() + (requestedOverscan * 2);
            int height = requestedView.height() + (requestedOverscan * 2);
            boolean hasMatchingDimensions = (image != null) && (image.getWidth() == width)
                  && (image.getHeight() == height);
            BufferedImage renderedImage;
            if (hasMatchingDimensions) {
                renderedImage = image;
            } else {
                clear();
                renderedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            }
            Graphics2D graphics = renderedImage.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, width, height);
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.translate(requestedOverscan, requestedOverscan);
                renderer.accept(graphics);
            } catch (RuntimeException exception) {
                renderedImage.flush();
                key = null;
                renderedView = null;
                image = null;
                throw exception;
            } finally {
                graphics.dispose();
            }
            key = requestedKey;
            renderedView = requestedView;
            image = renderedImage;
            overscan = requestedOverscan;
            renderCount++;
            fullRenderCount++;
            return new PannableRenderLayer(image, -overscan, -overscan);
        }

        @Nullable PannableRenderLayer getOrRefresh(K requestedKey, RenderViewKey requestedView,
              int requestedOverscan, int requiredMargin, Consumer<Graphics2D> renderer) {
            if ((requiredMargin < 0) || (requiredMargin > requestedOverscan)) {
                throw new IllegalArgumentException("Required margin must fit within overscan");
            }
            PannableRenderLayer cachedLayer = getReusableLayer(
                  requestedKey, requestedView, requestedOverscan, requiredMargin);
            if (cachedLayer != null) {
                reuseCount++;
                return cachedLayer;
            }
            return refreshForPan(requestedKey, requestedView, requestedOverscan, renderer);
        }

        void install(K requestedKey, RenderViewKey requestedView, int requestedOverscan,
              BufferedImage renderedImage) {
            int expectedWidth = requestedView.width() + (requestedOverscan * 2);
            int expectedHeight = requestedView.height() + (requestedOverscan * 2);
            if ((renderedImage.getWidth() != expectedWidth) || (renderedImage.getHeight() != expectedHeight)) {
                throw new IllegalArgumentException("Prepared raster dimensions do not match the requested view");
            }
            if ((image != null) && (image != renderedImage)) {
                image.flush();
            }
            key = requestedKey;
            renderedView = requestedView;
            image = renderedImage;
            overscan = requestedOverscan;
            renderCount++;
            fullRenderCount++;
        }

        @Nullable PannableRenderLayerSnapshot<K> snapshot(K requestedKey) {
            PannableRenderLayerSnapshot<K> snapshot = snapshot();
            if ((snapshot == null) || !requestedKey.equals(snapshot.key())) {
                return null;
            }
            return snapshot;
        }

        @Nullable PannableRenderLayerSnapshot<K> snapshot() {
            if ((image == null) || (renderedView == null) || (key == null)) {
                return null;
            }
            return new PannableRenderLayerSnapshot<>(key, renderedView, image, overscan);
        }

          private @Nullable PannableRenderLayer refreshForPan(K requestedKey, RenderViewKey requestedView,
              int requestedOverscan, Consumer<Graphics2D> renderer) {
            Point pixelDelta = getPixelPanDelta(requestedKey, requestedView, requestedOverscan);
            if ((pixelDelta == null) || ((pixelDelta.x == 0) && (pixelDelta.y == 0))
                || (Math.abs(pixelDelta.x) >= image.getWidth())
                || (Math.abs(pixelDelta.y) >= image.getHeight())) {
                return null;
            }

            int retainedWidth = image.getWidth() - Math.abs(pixelDelta.x);
            int retainedHeight = image.getHeight() - Math.abs(pixelDelta.y);
            int sourceX = Math.max(0, -pixelDelta.x);
            int sourceY = Math.max(0, -pixelDelta.y);
            int retainedX = Math.max(0, pixelDelta.x);
            int retainedY = Math.max(0, pixelDelta.y);
            Area exposedArea = new Area(new Rectangle(0, 0, image.getWidth(), image.getHeight()));
            exposedArea.subtract(new Area(new Rectangle(retainedX, retainedY, retainedWidth, retainedHeight)));

            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setComposite(AlphaComposite.Src);
                graphics.copyArea(sourceX, sourceY, retainedWidth, retainedHeight,
                    pixelDelta.x, pixelDelta.y);
                graphics.setClip(exposedArea);
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                graphics.setComposite(AlphaComposite.SrcOver);
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.translate(requestedOverscan, requestedOverscan);
                exposedArea.transform(AffineTransform.getTranslateInstance(-requestedOverscan, -requestedOverscan));
                graphics.setClip(exposedArea);
                renderer.accept(graphics);
            } catch (RuntimeException exception) {
                clear();
                throw exception;
            } finally {
                graphics.dispose();
            }
            key = requestedKey;
            renderedView = requestedView;
            overscan = requestedOverscan;
            renderCount++;
            stripRefreshCount++;
            return new PannableRenderLayer(image, -overscan, -overscan);
          }

                private @Nullable PannableRenderLayer getReusableLayer(K requestedKey, RenderViewKey requestedView,
                            int requestedOverscan, int requiredMargin) {
            Point pixelDelta = getPixelPanDelta(requestedKey, requestedView, requestedOverscan);
                        int reusableMargin = overscan - requiredMargin;
                        if ((pixelDelta == null) || (Math.abs(pixelDelta.x) > reusableMargin)
                                || (Math.abs(pixelDelta.y) > reusableMargin)) {
                return null;
            }
            return new PannableRenderLayer(image, pixelDelta.x - overscan, pixelDelta.y - overscan);
          }

          private @Nullable Point getPixelPanDelta(K requestedKey, RenderViewKey requestedView,
              int requestedOverscan) {
            if ((image == null) || (renderedView == null) || !requestedKey.equals(key)
                || (requestedOverscan != overscan) || (requestedView.width() != renderedView.width())
                || (requestedView.height() != renderedView.height())
                || (requestedView.scaleBits() != renderedView.scaleBits())) {
                return null;
            }

            double scale = Double.longBitsToDouble(requestedView.scaleBits());
            double deltaX = (Double.longBitsToDouble(requestedView.centerXBits())
                - Double.longBitsToDouble(renderedView.centerXBits())) * scale;
            double deltaY = (Double.longBitsToDouble(requestedView.centerYBits())
                - Double.longBitsToDouble(renderedView.centerYBits())) * scale;
            int pixelDeltaX = (int) Math.rint(deltaX);
            int pixelDeltaY = (int) Math.rint(deltaY);
            if (!Double.isFinite(deltaX) || !Double.isFinite(deltaY)
                || (Math.abs(deltaX - pixelDeltaX) > 0.000_001)
                || (Math.abs(deltaY - pixelDeltaY) > 0.000_001)) {
                return null;
            }
            return new Point(pixelDeltaX, pixelDeltaY);
        }

        void clear() {
            if (image != null) {
                image.flush();
            }
            key = null;
            renderedView = null;
            image = null;
            overscan = 0;
        }

        int getRenderCount() {
            return renderCount;
        }

        int getReuseCount() {
            return reuseCount;
        }

        int getStripRefreshCount() {
            return stripRefreshCount;
        }

        int getFullRenderCount() {
            return fullRenderCount;
        }

        boolean hasImage() {
            return image != null;
        }
    }

    static final class PreparedRenderData<K, V> {
        private K key;
        private V value;
        private int preparationCount;

        V prepare(K requestedKey, Supplier<V> preparer) {
            if ((value != null) && requestedKey.equals(key)) {
                return value;
            }
            V preparedValue = preparer.get();
            key = requestedKey;
            value = preparedValue;
            preparationCount++;
            return preparedValue;
        }

        @Nullable V get(K requestedKey) {
            return ((value != null) && requestedKey.equals(key)) ? value : null;
        }

        void clear() {
            key = null;
            value = null;
        }

        int getPreparationCount() {
            return preparationCount;
        }

        @Nullable K getKey() {
            return key;
        }
    }

    static void drawRenderLayer(Graphics2D graphics, BufferedImage image, double alpha) {
        drawRenderLayer(graphics, image, 0, 0, alpha);
    }

    static void drawRenderLayer(Graphics2D graphics, BufferedImage image, int x, int y, double alpha) {
        paintLayerWithAlpha(graphics, alpha, layerGraphics -> layerGraphics.drawImage(image, x, y, null));
    }

    static void drawPannableRenderLayer(Graphics2D graphics, PannableRenderLayer layer,
          int width, int height, double alpha) {
        int sourceX = -layer.drawX();
        int sourceY = -layer.drawY();
        paintLayerWithAlpha(graphics, alpha, layerGraphics -> layerGraphics.drawImage(layer.image(),
              0, 0, width, height, sourceX, sourceY, sourceX + width, sourceY + height, null));
    }

        static AffineTransform transformPannableSnapshot(
            PannableRenderLayerSnapshot<?> snapshot, RenderViewKey requestedView) {
          RenderViewKey renderedView = snapshot.renderedView();
          double renderedScale = Double.longBitsToDouble(renderedView.scaleBits());
          double requestedScale = Double.longBitsToDouble(requestedView.scaleBits());
          double scaleRatio = requestedScale / renderedScale;
          double renderedCenterX = Double.longBitsToDouble(renderedView.centerXBits());
          double renderedCenterY = Double.longBitsToDouble(renderedView.centerYBits());
          double requestedCenterX = Double.longBitsToDouble(requestedView.centerXBits());
          double requestedCenterY = Double.longBitsToDouble(requestedView.centerYBits());
          double translateX = requestedView.width() / 2.0
              - (scaleRatio * renderedView.width() / 2.0)
              + ((requestedCenterX - renderedCenterX) * requestedScale)
              - (scaleRatio * snapshot.overscan());
          double translateY = requestedView.height() / 2.0
              - (scaleRatio * renderedView.height() / 2.0)
              + ((requestedCenterY - renderedCenterY) * requestedScale)
              - (scaleRatio * snapshot.overscan());
          AffineTransform transform = AffineTransform.getTranslateInstance(translateX, translateY);
          transform.scale(scaleRatio, scaleRatio);
          return transform;
        }

    static void drawPannableSnapshot(Graphics2D graphics, PannableRenderLayerSnapshot<?> snapshot,
          RenderViewKey requestedView, double alpha) {
        AffineTransform transform = transformPannableSnapshot(snapshot, requestedView);
        paintLayerWithAlpha(graphics, alpha, layerGraphics -> {
            layerGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                  RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            layerGraphics.drawImage(snapshot.image(), transform, null);
        });
    }

        private record FactionLogoKey(int gameYear, String factionCode) {
        }

        private record ScaledFactionLogoKey(FactionLogoKey logoKey, int width, int height) {
        }

        private record FactionLogoImage(BufferedImage tinted, BufferedImage shadow) {
        }

        private record FactionLogoCandidate(TerritoryComponent component, int priority, double projectedArea,
            Rectangle2D.Double bounds, FactionLogoImage image) {
        }

        static final class StaticCartographyPreparationQueue<K> {
            private final BooleanSupplier isShowing;
            private final Consumer<Runnable> eventLoopScheduler;
            private final Consumer<K> preparer;
            private K latestRequest;
            private boolean dispatchPending;

            StaticCartographyPreparationQueue(BooleanSupplier isShowing, Consumer<Runnable> eventLoopScheduler,
                  Consumer<K> preparer) {
                this.isShowing = isShowing;
                this.eventLoopScheduler = eventLoopScheduler;
                this.preparer = preparer;
            }

            void request(K requestedKey) {
                if (!isShowing.getAsBoolean()) {
                    return;
                }
                latestRequest = requestedKey;
                if (dispatchPending) {
                    return;
                }
                dispatchPending = true;
                eventLoopScheduler.accept(this::dispatch);
            }

            void cancel() {
                latestRequest = null;
            }

            private void dispatch() {
                dispatchPending = false;
                K requestedKey = latestRequest;
                latestRequest = null;
                if ((requestedKey != null) && isShowing.getAsBoolean()) {
                    preparer.accept(requestedKey);
                }
            }
        }

        static final class LatestBackgroundPreparationQueue<K, V> {
            private final BooleanSupplier isActive;
            private final Consumer<Runnable> backgroundScheduler;
            private final Consumer<Runnable> eventLoopScheduler;
            private final Function<K, V> preparer;
            private final BiConsumer<K, V> installer;
            private final BiConsumer<K, Throwable> failureHandler;
            private final Consumer<V> discarder;
            private K pendingRequest;
            private K inFlightRequest;
            private boolean workerRunning;
            private long generation;
            private long inFlightGeneration;

            LatestBackgroundPreparationQueue(BooleanSupplier isActive,
                  Consumer<Runnable> backgroundScheduler, Consumer<Runnable> eventLoopScheduler,
                  Function<K, V> preparer, BiConsumer<K, V> installer,
                  BiConsumer<K, Throwable> failureHandler, Consumer<V> discarder) {
                this.isActive = isActive;
                this.backgroundScheduler = backgroundScheduler;
                this.eventLoopScheduler = eventLoopScheduler;
                this.preparer = preparer;
                this.installer = installer;
                this.failureHandler = failureHandler;
                this.discarder = discarder;
            }

            void request(K requestedKey) {
                if (!isActive.getAsBoolean()) {
                    return;
                }
                if (requestedKey.equals(pendingRequest)) {
                    dispatchIfIdle();
                    return;
                }
                if (requestedKey.equals(inFlightRequest) && (inFlightGeneration == generation)) {
                    pendingRequest = null;
                    return;
                }
                pendingRequest = requestedKey;
                dispatchIfIdle();
            }

            void cancel() {
                pendingRequest = null;
                generation++;
            }

            private void dispatchIfIdle() {
                if (workerRunning || (pendingRequest == null) || !isActive.getAsBoolean()) {
                    return;
                }
                K request = pendingRequest;
                pendingRequest = null;
                inFlightRequest = request;
                workerRunning = true;
                long requestGeneration = generation;
                inFlightGeneration = requestGeneration;
                backgroundScheduler.accept(() -> prepare(request, requestGeneration));
            }

            private void prepare(K request, long requestGeneration) {
                V preparedValue = null;
                Throwable failure = null;
                try {
                    preparedValue = preparer.apply(request);
                } catch (RuntimeException | Error exception) {
                    failure = exception;
                }
                V completedValue = preparedValue;
                Throwable completedFailure = failure;
                eventLoopScheduler.accept(() -> complete(
                      request, requestGeneration, completedValue, completedFailure));
            }

            private void complete(K request, long requestGeneration, V preparedValue,
                  @Nullable Throwable failure) {
                workerRunning = false;
                inFlightRequest = null;
                boolean resultIsCurrent = (requestGeneration == generation)
                      && isActive.getAsBoolean() && (pendingRequest == null);
                if (resultIsCurrent) {
                    if (failure == null) {
                        installer.accept(request, preparedValue);
                    } else {
                        failureHandler.accept(request, failure);
                    }
                } else if (failure == null) {
                    discarder.accept(preparedValue);
                }
                dispatchIfIdle();
            }
        }

            record SemanticZoomProfile(double territoryAlpha, double factionLogoAlpha,
                double systemContactAlpha, double strategicContactAlpha, double systemDetailAlpha,
                double detailedOverlayAlpha,
                double ordinaryLabelAlpha,
                double routeLabelAlpha, double routeBadgeAlpha, double hpgNetworkAlpha,
                double capitalAlpha, double missionOperationAlpha,
                    double urgentOperationAlpha, double currentLocationAlpha, double serviceAlpha,
                    double stellarDetailAlpha) {
        static SemanticZoomProfile create(double scale, double semanticReference) {
            double boundedReference = Math.clamp(semanticReference, 2.4, 3.6);
            double atlasEnd = Math.clamp(boundedReference / 3.0, 0.8, 1.0);
            double navigationEnd = Math.clamp(boundedReference * 0.8, 2.4, 3.0);
            double navigationAlpha = fadeBetween(scale, atlasEnd, navigationEnd);
            double systemDetailStart = Math.clamp(boundedReference * 1.1, 3.0, 4.0);
            double fullSystemDetail = Math.clamp(boundedReference * 1.6, 4.2, 5.6);
            double systemDetailAlpha = fadeBetween(scale, systemDetailStart, fullSystemDetail);
            double ordinaryLabelStart = Math.clamp(boundedReference * 1.2, 3.4, 4.2);
            double ordinaryLabelAlpha = fadeBetween(scale, ordinaryLabelStart, fullSystemDetail);
            double stellarDetailAlpha = fadeBetween(scale,
                  Math.max(systemDetailStart, fullSystemDetail - 0.6), fullSystemDetail);
            double territoryAlpha = TERRITORY_LAYER_OPACITY;

            return new SemanticZoomProfile(
                  territoryAlpha,
                  1.0 - navigationAlpha,
                  1.0 - systemDetailAlpha,
                  1.0 - systemDetailAlpha,
                  systemDetailAlpha,
                  navigationAlpha,
                  ordinaryLabelAlpha,
                  navigationAlpha,
                  fadeBetween(navigationAlpha, 0.35, 0.85),
                  interpolate(0.65, 1.0, navigationAlpha),
                  interpolate(0.75, 1.0, navigationAlpha),
                  navigationAlpha,
                  interpolate(0.68, 1.0, navigationAlpha),
                  systemDetailAlpha,
                  systemDetailAlpha,
                  stellarDetailAlpha);
        }
    }

    record TerritoryVisualProfile(double secondaryDetailAlpha, double disputedBandWidth) {
        static TerritoryVisualProfile create(double scale) {
            double detailAlpha = fadeBetween(scale, 1.8, 3.2);
            double bandWidth = interpolate(12.0, 22.0, fadeBetween(scale, 3.2, 5.6));
            return new TerritoryVisualProfile(detailAlpha, bandWidth);
        }
    }

    record SystemMarkerLayout(double centerX, double centerY, double size,
          RouteMarkerState routeState, boolean selected, boolean hovered, double ownershipRadius,
          double navigationRadius, double selectedRadius, double hoveredRadius, double externalOrbitRadius) {
        static SystemMarkerLayout create(double centerX, double centerY, double size,
              RouteMarkerState routeState, boolean selected, boolean hovered) {
            double ownershipRadius = size + 2.8;
            double selectedRadius = ownershipRadius + 3.0;
            double hoveredRadius = selectedRadius + 3.0;
            double navigationOrbitRadius = navigationClearanceRadius(hoveredRadius) + 1.25;
            double navigationRadius = switch (routeState) {
                case NONE -> 0.0;
                case PLANNED, ACTIVE -> navigationOrbitRadius;
            };
            double externalOrbitRadius = Math.hypot(hoveredRadius, hoveredRadius) + 0.55;
            if (routeState != RouteMarkerState.NONE) {
                externalOrbitRadius = navigationOrbitRadius;
            }
            return new SystemMarkerLayout(centerX, centerY, size, routeState, selected, hovered,
                  ownershipRadius, navigationRadius, selectedRadius, hoveredRadius, externalOrbitRadius);
        }

        double navigationClearanceRadius() {
            return navigationClearanceRadius(hoveredRadius);
        }

        private static double navigationClearanceRadius(double hoveredRadius) {
            return Math.hypot(hoveredRadius, hoveredRadius) + 0.55 + 3.0;
        }

        Point2D.Double operationAnchor() {
            return operationAnchor(operationMarkerRadius(size, 1.0), 1.0);
        }

        Point2D.Double operationAnchor(double expansion) {
            return operationAnchor(operationMarkerRadius(size, 1.0), expansion);
        }

        Point2D.Double operationAnchor(double markerRadius, double expansion) {
            double radialOffset = externalOrbitRadius + markerRadius + externalOrbitGap(3.0, 4.0);
            return expandedAnchor(diagonalAnchor(-1.0, -1.0, radialOffset), expansion);
        }

        Point2D.Double playerBaseAnchor(double markerRadius, double expansion) {
            double compactOffset = ownershipRadius + markerRadius + 1.5;
            double detailedOffset = externalOrbitRadius + markerRadius + externalOrbitGap(3.0, 4.0);
            Point2D.Double compactAnchor = diagonalAnchor(-1.0, 1.0, compactOffset);
            Point2D.Double detailedAnchor = diagonalAnchor(-1.0, 1.0, detailedOffset);
            double clampedExpansion = Math.clamp(expansion, 0.0, 1.0);
            return new Point2D.Double(interpolate(compactAnchor.x, detailedAnchor.x, clampedExpansion),
                  interpolate(compactAnchor.y, detailedAnchor.y, clampedExpansion));
        }

        double overrideRadius() {
            return ownershipRadius + 2.5;
        }

        double orthogonalExternalOrbitRadius() {
            return routeState == RouteMarkerState.NONE
                  ? hoveredRadius + 0.55
                  : externalOrbitRadius;
        }

        Point2D.Double gmEditedAnchor() {
            return gmEditedAnchor(1.0);
        }

        Point2D.Double gmEditedAnchor(double expansion) {
            double verticalRadius = gmEditedMarkerSize(size) / 2.0 + 1.2;
            return expandedAnchor(new Point2D.Double(centerX,
                  centerY + orthogonalExternalOrbitRadius() + verticalRadius
                        + externalOrbitGap(3.0, 4.0)), expansion);
        }

        Point2D.Double capitalAnchor(int markerIndex, int markerCount) {
            return capitalAnchor(markerIndex, markerCount, 1.0);
        }

        Point2D.Double capitalAnchor(int markerIndex, int markerCount, double expansion) {
            Rectangle2D.Double markerBounds = capitalBandMarkerBounds(new Point2D.Double(), size);
            double markerSpacing = markerBounds.width + 2.0;
            double horizontalOffset = (markerIndex - ((markerCount - 1) / 2.0)) * markerSpacing;
            double bandShift = 0.0;
            if (markerCount > 1) {
                double rightmostX = centerX + (((markerCount - 1) / 2.0) * markerSpacing);
                double shipLeft = shipAnchor().x - (CURRENT_LOCATION_ICON_SIZE / 2.0);
                bandShift = Math.min(0.0,
                      shipLeft - 2.0 - (rightmostX + (markerBounds.width / 2.0)));
            }
            return expandedAnchor(new Point2D.Double(centerX + horizontalOffset + bandShift,
                centerY - orthogonalExternalOrbitRadius() - (markerBounds.height / 2.0)
                      - externalOrbitGap(3.0, 4.0)), expansion);
        }

        Point2D.Double routeBadgeAnchor(double badgeDiameter) {
            return routeBadgeAnchor(badgeDiameter, 1.0);
        }

        Point2D.Double routeBadgeAnchor(double badgeDiameter, double expansion) {
            double radialOffset = externalOrbitRadius + (badgeDiameter / 2.0)
                  + externalOrbitGap(3.0, 4.0);
            return expandedAnchor(diagonalAnchor(1.0, 1.0, radialOffset), expansion);
        }

        Point2D.Double routeStatusAnchor(double markerRadius) {
            return routeStatusAnchor(markerRadius, 1.0);
        }

        Point2D.Double routeStatusAnchor(double markerRadius, double expansion) {
            double slotSeparation = Math.max(18.0, size * 1.25);
            double radialOffset = externalOrbitRadius + markerRadius + slotSeparation;
            return expandedAnchor(new Point2D.Double(centerX + radialOffset, centerY), expansion);
        }

        Point2D.Double hpgStationAnchor(double markerRadius) {
            double radialOffset = externalOrbitRadius + markerRadius + externalOrbitGap(3.0, 4.0);
            return new Point2D.Double(centerX + radialOffset, centerY);
        }

        double routeStatusLabelX(double markerRadius) {
            return routeStatusAnchor(markerRadius).x + markerRadius + 3.0;
        }

        Point2D.Double shipAnchor() {
            return shipAnchor(1.0);
        }

        Point2D.Double shipAnchor(double expansion) {
            double iconClearance = CURRENT_LOCATION_ICON_SIZE / 2.0;
            double radialOffset = externalOrbitRadius + iconClearance + externalOrbitGap(3.0, 5.0);
            Point2D.Double detailedAnchor = diagonalAnchor(1.0, -1.0, radialOffset);
            if (CURRENT_LOCATION_ICON == null) {
                return expandedAnchor(detailedAnchor, expansion);
            }
            double halfIconSize = CURRENT_LOCATION_ICON_SIZE / 2.0;
            Point2D.Double alignedDetailedAnchor = new Point2D.Double(
                  Math.round(detailedAnchor.x - halfIconSize) + halfIconSize,
                  Math.round(detailedAnchor.y - halfIconSize) + halfIconSize);
            return expandedAnchor(alignedDetailedAnchor, expansion);
        }

        double labelX() {
            return centerX + Math.max(size * 1.8, hoveredRadius + 3.0);
        }

        private double externalOrbitGap(double compactGap, double expandedGap) {
            double expansion = Math.clamp((size - 3.0) / 8.0, 0.0, 1.0);
            return interpolate(compactGap, expandedGap, expansion);
        }

          private Point2D.Double diagonalAnchor(double horizontalDirection, double verticalDirection,
              double radialOffset) {
            double componentOffset = radialOffset / Math.sqrt(2.0);
            return new Point2D.Double(centerX + (horizontalDirection * componentOffset),
                  centerY + (verticalDirection * componentOffset));
        }

          private Point2D.Double expandedAnchor(Point2D.Double detailedAnchor, double expansion) {
            double clampedExpansion = Math.clamp(expansion, 0.0, 1.0);
            return new Point2D.Double(interpolate(centerX, detailedAnchor.x, clampedExpansion),
                interpolate(centerY, detailedAnchor.y, clampedExpansion));
          }
    }

        private SystemMarkerLayout createSystemMarkerLayout(PlanetarySystem system, double size,
                    RouteMarkerState routeState) {
                return createSystemMarkerLayout(system, size, routeState, null);
        }

        private SystemMarkerLayout createSystemMarkerLayout(PlanetarySystem system, double size,
                    RouteMarkerState routeState, @Nullable PlanetarySystem hoveredSystem) {
                boolean selected = isSameSystem(system, selectedSystem);
                boolean hovered = isSelectionAnimationTarget(system)
                            || (!selected && isSameSystem(system, hoveredSystem));
                return SystemMarkerLayout.create(map2scrX(system.getX()), map2scrY(system.getY()), size,
                            routeState, selected, hovered);
        }

    private static final Vector2d[] BASE_HEX_COORDS = {
          new Vector2d(1.0, 0.0),
          new Vector2d(Math.cos(Math.PI / 3.0), Math.sin(Math.PI / 3.0)),
          new Vector2d(Math.cos(2.0 * Math.PI / 3.0), Math.sin(2.0 * Math.PI / 3.0)),
          new Vector2d(-1.0, 0.0),
          new Vector2d(Math.cos(4.0 * Math.PI / 3.0), Math.sin(4.0 * Math.PI / 3.0)),
          new Vector2d(Math.cos(5.0 * Math.PI / 3.0), Math.sin(5.0 * Math.PI / 3.0))
    };

    private final JLayeredPane pane;
    private final JPanel mapPanel;
    private final JPanel optionControl;
    private final JViewport optionView;
    private final JPanel optionPanel;
    private final ResourceBundle resourceMap;

    // Map view options
    private final JRadioButton optFactions;
    private final JRadioButton optTech;
    private final JRadioButton optIndustry;
    private final JRadioButton optRawMaterials;
    private final JRadioButton optOutput;
    private final JRadioButton optAgriculture;
    private final JRadioButton optPopulation;
    private final JRadioButton optHPG;
    private final JRadioButton optRecharge;
    private final JRadioButton optAcademies;
    private final JRadioButton optHiringHalls;
    private final JRadioButton optDiseases;

    private final JCheckBox optEmptySystems;
    private final JCheckBox optHPGNetwork;
    private final ImmersiveComboBox<HpgNetworkDetail> optHpgNetworkDetail;
    private final JCheckBox optCapitals;
    private final ImmersiveComboBox<CapitalDisplayDetail> optCapitalDetail;
    private final JCheckBox optTerritory;
    private final JCheckBox optOperations;
    private final JCheckBox optReachability;
    private final ImmersiveSpinner reachabilityHops;
    private final JCheckBox optMeasureDistance;

    private final Timer layerAnimationTimer;
    private final Timer selectionAnimationTimer;
    private final Timer proposedRouteAnimationTimer;
    private final Timer travelAnimationTimer;
    private final Timer systemDiveAnimationTimer;
    private final Timer renderBenchmarkTimer;
    private final Timer transitionBenchmarkTimer;
    private final Timer zoomBenchmarkTimer;
    private final Timer zoomSettlingTimer;
    private final RenderPerformanceTracker renderPerformanceTracker =
          new RenderPerformanceTracker(System.nanoTime());
    private boolean optionPanelHidden;
    private Component layerControlsInvoker;
    private boolean layerDismissalListenerInstalled;
    private final AWTEventListener layerDismissalListener = this::dismissLayersOnOutsideClick;
    private JDialog mapLegendDialog;
    private boolean optionPanelAnimating;
    private long optionPanelAnimationStartTime;
    private long optionPanelAnimationDuration;
    private double optionPanelAnimationStartExpansion;
    private double optionPanelAnimationTargetExpansion;
    private double optionPanelExpansion;
    private boolean territoryLayerAnimating;
    private boolean territoryLayerSettling;
    private long territoryLayerAnimationStartTime;
    private long territoryLayerAnimationDuration;
    private double territoryLayerAnimationStartAlpha = 1.0;
    private double territoryLayerAnimationTargetAlpha = 1.0;
    private double territoryLayerAlpha = 1.0;
    private boolean hpgNetworkLayerAnimating;
    private long hpgNetworkLayerAnimationStartTime;
    private long hpgNetworkLayerAnimationDuration;
    private double hpgNetworkLayerAnimationStartAlpha;
    private double hpgNetworkLayerAnimationTargetAlpha;
    private double hpgNetworkLayerAlpha;
    private boolean operationsLayerAnimating;
    private long operationsLayerAnimationStartTime;
    private long operationsLayerAnimationDuration;
    private double operationsLayerAnimationStartAlpha = 1.0;
    private double operationsLayerAnimationTargetAlpha = 1.0;
    private double operationsLayerAlpha = 1.0;
    private boolean mapModeAnimating;
    private long mapModeAnimationStartTime;
    private long mapModeAnimationDuration;
    private double mapModeAnimationStartProgress = 1.0;
    private double mapModeAnimationProgress = 1.0;
    private MapMode previousMapMode = MapMode.FACTION;
    private MapMode targetMapMode = MapMode.FACTION;
        private MapModeTransitionCacheStage mapModeTransitionCacheStage =
            MapModeTransitionCacheStage.INACTIVE;
        private boolean mapModeTransitionSettling;

    private ArrayList<PlanetarySystem> systems;
    private SystemSpatialIndex systemSpatialIndex;

    private JumpPath jumpPath;
    private Campaign campaign;
    private final InnerStellarMapConfig conf = new InnerStellarMapConfig();
    private final CampaignGUI hqView;
    private PlanetarySystem selectedSystem = null;
    private String selectionAnimationSystemId = null;
    private long selectionAnimationStartTime;
    private double selectionAnimationProgress = 1.0;
    private long proposedRouteAnimationStartTime;
    private long proposedRouteAnimationDuration;
    private double proposedRouteAnimationProgress = 1.0;
    private boolean travelVisualStateInitialized;
    private String observedCurrentSystemId;
    private List<String> observedActiveRouteSystemIds = List.of();
    private boolean observedActiveRouteExists;
    private List<String> cachedProposedRouteSystemIds = List.of();
    private List<String> activatingRouteSystemIds = List.of();
    private long routeActivationStartTime;
    private double routeActivationProgress = 1.0;
    private String systemHopOriginId;
    private String systemHopDestinationId;
    private long systemHopStartTime;
    private double systemHopProgress = 1.0;
    private long systemDiveAnimationStartTime;
    private double systemDiveStartCenterX;
    private double systemDiveStartCenterY;
    private double systemDiveStartScale;
    private double systemDiveTargetCenterX;
    private double systemDiveTargetCenterY;
    private double systemDiveTargetScale;
    private double systemDiveAnimationProgress = 1.0;
    private PlanetarySystem systemDiveTarget;
    private SystemDiveFrame systemDiveReturnFrame;
    private boolean systemDiveReturning;
    private Runnable systemDiveCompletion;
    private RenderBenchmarkRun renderBenchmarkRun;
    private long renderBenchmarkPhaseStartedNanos;
    private boolean renderBenchmarkMeasuring;
    private TransitionBenchmarkRun transitionBenchmarkRun;
    private TransitionBenchmarkPhase transitionBenchmarkPhase;
    private long transitionBenchmarkPhaseStartedNanos;
    private boolean transitionBenchmarkExactFramePainted;
    private ZoomBenchmarkRun zoomBenchmarkRun;
    private ZoomBenchmarkPhase zoomBenchmarkPhase;
    private long zoomBenchmarkPhaseStartedNanos;
    private RenderBenchmarkCamera zoomBenchmarkExpectedCamera;
    private boolean zoomBenchmarkCameraPainted;
    private boolean zoomBenchmarkExactFramePainted;
    private boolean zoomBenchmarkPathComplete;
    private boolean zoomInteractionActive;
    private NavigationRouteAnalysis.Reachability cachedReachability;
    private long reachabilityRevision;
    private PathAssessment cachedProposedRouteAssessment = emptyPathAssessment();
    private PathAssessment cachedActiveRouteAssessment = emptyPathAssessment();
    private MeasurementState measurementState = MeasurementState.inactive();
    private PlanetarySystem measurementHoverSystem;
    private LegAssessment cachedMeasurementAssessment;
    private Point lastMousePos = null;
    private int mouseMod = 0;
    private final MapPointerGesture mapPointerGesture = new MapPointerGesture();
    private RoutePlanningHandler routePlanningHandler = NO_ROUTE_PLANNING_HANDLER;

    private transient double minX;
    private transient double minY;
    private transient double maxX;
    private transient double maxY;
    private transient LocalDate now;
    private final PreparedRenderData<TerritoryDataKey, TerritoryAtlas> preparedTerritoryAtlas =
          new PreparedRenderData<>();
    private final PreparedRenderData<SystemRenderDataKey, Map<String, SystemRenderData>> preparedSystemRenderData =
          new PreparedRenderData<>();
    private final RenderLayerCache<RenderViewKey> backgroundRenderCache = new RenderLayerCache<>();
        private final PannableRenderLayerCache<TerritoryDataKey> territoryRenderCache =
            new PannableRenderLayerCache<>();
    private final PannableRenderLayerCache<RetainedCartographyKey> retainedCartographyRenderCache =
          new PannableRenderLayerCache<>();
        private final PannableRenderLayerCache<RetainedCartographyKey> retainedSystemArtRenderCache =
            new PannableRenderLayerCache<>();
        private final PannableRenderLayerCache<RetainedNavigationKey> retainedNavigationRenderCache =
                    new PannableRenderLayerCache<>();
        private final PannableRenderLayerCache<RetainedNavigationKey> mapModeTransitionBaseRenderCache =
            new PannableRenderLayerCache<>();
        private final PannableRenderLayerCache<RetainedCartographyKey> mapModeTransitionSystemRenderCache =
            new PannableRenderLayerCache<>();
        private final PannableRenderLayerCache<RetainedCartographyKey> previousMapModeRenderCache =
            new PannableRenderLayerCache<>();
        private final PannableRenderLayerCache<RetainedCartographyKey> targetMapModeRenderCache =
            new PannableRenderLayerCache<>();
    private final RenderLayerCache<FactionLogoRenderKey> factionLogoRenderCache = new RenderLayerCache<>();
    private final StaticCartographyPreparationQueue<TerritoryDataKey> territoryPreparationQueue =
          new StaticCartographyPreparationQueue<>(this::isShowing, SwingUtilities::invokeLater,
                this::prepareRequestedStaticCartography);
            private final LatestBackgroundPreparationQueue<RetainedCartographyRenderRequest, BufferedImage>
                retainedCartographyPreparationQueue = new LatestBackgroundPreparationQueue<>(this::isShowing,
                RETAINED_TERRITORY_RENDERER::execute, SwingUtilities::invokeLater,
                    InterstellarMapPanel::renderRetainedCartographyTerritory,
                    this::installRetainedCartography, (request, failure) ->
                        LOGGER.error("Failed to prepare retained cartography raster", failure), BufferedImage::flush);
                private boolean retainedCartographyProvisional;
    private long cartographyDataRevision;
    private final Map<FactionLogoKey, FactionLogoImage> factionLogoImages = new HashMap<>();
    private final Map<ScaledFactionLogoKey, FactionLogoImage> scaledFactionLogoImages = new HashMap<>();
    private final Set<FactionLogoKey> missingFactionLogoImages = new HashSet<>();
    private MapPresentationData mapPresentationData = MapPresentationData.empty();
    private Map<String, SystemRenderData> systemLabelWidthRenderData = Map.of();
    private Font systemLabelWidthFont;
    private FontRenderContext systemLabelWidthFontRenderContext;
    private double maximumSystemLabelWidth;

    public InterstellarMapPanel(Campaign campaign, CampaignGUI view) {
        this.campaign = campaign;
        resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignGUI",
              MekHQ.getMHQOptions().getLocale());
        systems = this.campaign.getSystems();
        systemSpatialIndex = new SystemSpatialIndex(systems);
        refreshMapPresentationData();
        hqView = view;
        jumpPath = new JumpPath();
        optionPanelHidden = true;
        layerAnimationTimer = new Timer(LAYER_ANIMATION_DELAY_MS, e -> updateLayerAnimations());
        layerAnimationTimer.setCoalesce(true);
        selectionAnimationTimer = new Timer(SELECTION_ANIMATION_DELAY_MS, e -> updateSelectionAnimation());
        selectionAnimationTimer.setCoalesce(true);
                proposedRouteAnimationTimer = new Timer(PROPOSED_ROUTE_ANIMATION_DELAY_MS,
              e -> updateProposedRouteAnimation());
                proposedRouteAnimationTimer.setCoalesce(true);
                travelAnimationTimer = new Timer(TRAVEL_ANIMATION_DELAY_MS, e -> updateTravelAnimations());
                travelAnimationTimer.setCoalesce(true);
                systemDiveAnimationTimer = new Timer(SYSTEM_DIVE_ANIMATION_DELAY_MS,
              e -> updateSystemDiveAnimation());
                systemDiveAnimationTimer.setCoalesce(true);
            renderBenchmarkTimer = new Timer(RENDER_BENCHMARK_DELAY_MS, e -> updateRenderBenchmark());
            renderBenchmarkTimer.setCoalesce(true);
            transitionBenchmarkTimer = new Timer(RENDER_BENCHMARK_DELAY_MS,
                e -> updateTransitionBenchmark());
            transitionBenchmarkTimer.setCoalesce(true);
            zoomBenchmarkTimer = new Timer(RENDER_BENCHMARK_DELAY_MS, e -> updateZoomBenchmark());
            zoomBenchmarkTimer.setCoalesce(true);
            zoomSettlingTimer = new Timer(ZOOM_SETTLE_DELAY_MS, e -> finishZoomInteraction());
            zoomSettlingTimer.setRepeats(false);

        setBorder(BorderFactory.createLineBorder(Color.black));

        addKeyListener(new KeyAdapter() {
            /** Handle the key pressed event from the text field. */
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if ((keyCode == KeyEvent.VK_ESCAPE) && measurementState.enabled()) {
                    stopMeasuring();
                    return;
                }
                boolean moved = false;
                if (keyCode == KeyEvent.VK_LEFT) {
                    conf.centerY -= 1.0;
                    moved = true;
                }
                if (keyCode == KeyEvent.VK_RIGHT) {
                    conf.centerY += 1.0;
                    moved = true;
                }
                if (keyCode == KeyEvent.VK_DOWN) {
                    conf.centerX += 1.0;
                    moved = true;
                }
                if (keyCode == KeyEvent.VK_UP) {
                    conf.centerX -= 1.0;
                    moved = true;
                }
                if (moved) {
                    repaint();
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                lastMousePos = new Point(e.getX(), e.getY());
                Map<String, SystemRenderData> renderData =
                      getPreparedSystemRenderData(campaign.getLocalDate());
                if (findHoveredSystem(getSystemMarkerSize(), renderData) != null) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lastMousePos = null;
                if (measurementState.enabled() && (measurementState.end() == null)) {
                    measurementHoverSystem = null;
                    cachedMeasurementAssessment = null;
                }
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
                mouseMod = 0;
                if ((e.getButton() == MouseEvent.BUTTON1) && mapPointerGesture.release(e.getPoint())
                      && !e.isPopupTrigger()) {
                    handleMapClick(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
                mouseMod = e.getButton();
                if (mouseMod == MouseEvent.BUTTON1) {
                    mapPointerGesture.press(e.getPoint());
                }
            }

            public void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    final Point popupAnchor = new Point(e.getPoint());
                    final PlanetarySystem popupSystem = findSystemAt(popupAnchor);
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem item = new JMenuItem("Zoom In");
                    item.addActionListener(ae -> zoom(1.5, popupAnchor));
                    popup.add(item);
                    item = new JMenuItem("Zoom Out");
                    item.addActionListener(ae -> zoom(0.5, popupAnchor));
                    popup.add(item);
                    JMenu centerM = new JMenu("Center Map");
                    item = new JMenuItem("On Selected Planet");
                    item.setEnabled(selectedSystem != null);
                    if (selectedSystem != null) {
                        // only add if there is a planet to center on
                        item.addActionListener(ae -> center(selectedSystem));
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Current Location");
                    item.setEnabled(InterstellarMapPanel.this.campaign.getCurrentSystem() != null);
                    if (InterstellarMapPanel.this.campaign.getCurrentSystem() != null) {
                        // only add if there is a planet to center on
                        item.addActionListener(evt -> {
                            selectSystem(InterstellarMapPanel.this.campaign.getCurrentSystem(), true);
                            center(InterstellarMapPanel.this.campaign.getCurrentSystem());
                        });
                    }
                    centerM.add(item);
                    item = new JMenuItem("On Terra");
                    item.addActionListener(evt -> {
                        conf.centerX = 0.0;
                        conf.centerY = 0.0;
                        repaint();
                    });
                    centerM.add(item);
                    popup.add(centerM);
                      popup.addSeparator();
                      popup.add(createRoutePlanningMenuItem("map.route.plotHere",
                          popupSystem != null,
                          () -> routePlanningHandler.plotRoute(popupSystem)));
                      popup.add(createRoutePlanningMenuItem("map.route.appendWaypoint",
                          popupSystem != null,
                          () -> routePlanningHandler.appendWaypoint(popupSystem)));
                      popup.add(createRoutePlanningMenuItem("map.route.trimHere",
                          (popupSystem != null)
                              && routePlanningHandler.canTrimRouteAt(popupSystem),
                          () -> routePlanningHandler.trimRouteAt(popupSystem)));
                      popup.add(createRoutePlanningMenuItem("map.route.removeWaypoint",
                          (popupSystem != null)
                              && routePlanningHandler.isRequestedWaypoint(popupSystem),
                          () -> routePlanningHandler.removeWaypoint(popupSystem)));
                      popup.add(createRoutePlanningMenuItem("map.route.clear",
                          routePlanningHandler.hasPlannedRoute(),
                          routePlanningHandler::clearPlannedRoute));
                      popup.addSeparator();
                    item = new JMenuItem("Cancel Current Trip");
                    item.setEnabled(routePlanningHandler.hasActiveTrip());
                    item.addActionListener(evt -> routePlanningHandler.cancelCurrentTrip());
                    popup.add(item);
                    item = new JMenuItem("Save Map (64 Mpx at current zoom level) ...");
                    item.setEnabled(true);
                    item.addActionListener(evt -> {
                        final int imgSize = 8192;
                        BufferedImage img = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_RGB);
                        Graphics g = img.createGraphics();
                        int originalWidth = getWidth();
                        int originalHeight = getHeight();
                        double originalX = conf.centerX;
                        double originalY = conf.centerY;
                        try {
                            Optional<File> file = FileDialogs.saveStarMap(hqView.getFrame());
                            if (file.isPresent()) {
                                prepareStaticCartography();
                                clearRenderLayerCaches();
                                mapPanel.setSize(imgSize, imgSize);
                                conf.centerX += (imgSize - originalWidth) / conf.scale / 2.0;
                                conf.centerY += (imgSize - originalHeight) / conf.scale / 2.0;
                                mapPanel.print(g);
                                ImageIO.write(img, "png", file.get());
                            }
                        } catch (Exception ex) {
                            LOGGER.error("", ex);
                        }
                        conf.centerX = originalX;
                        conf.centerY = originalY;
                        clearRenderLayerCaches();
                        g.dispose();
                        mapPanel.repaint();
                    });
                    popup.add(item);
                    JMenu menuGM = new JMenu("GM Mode");
                    item = new JMenuItem("Move to selected planet");
                      item.setEnabled((selectedSystem != null)
                          && InterstellarMapPanel.this.campaign.isGM());
                    if (selectedSystem != null) {
                        // only add if there is a planet to center on
                        item.addActionListener(evt -> {
                            InterstellarMapPanel.this.campaign.getPlayerForce()
                                  .getDetachmentLocationManager()
                                  .moveToPlanetarySystem(InterstellarMapPanel.this.campaign, selectedSystem);
                                routePlanningHandler.clearPlannedRoute();
                            center(selectedSystem);
                        });
                    }
                    menuGM.add(item);
                    /*
                     * TODO: re-enable this later
                     * item = new JMenuItem("Edit planetary events");
                     * item.setEnabled(selectedSystem != null && campaign.isGM());
                     * if (selectedSystem != null) {
                     * item.setText("Edit planetary events for " +
                     * selectedSystem.getPrintableName(Utilities.getDateTimeDay(campaign.getCalendar
                     * ())));
                     * item.addActionListener(new ActionListener() {
                     *
                     * @Override
                     * public void actionPerformed(ActionEvent attackingEntity) {
                     * openPlanetEventEditor(selectedSystem);
                     * }
                     * });
                     * }
                     * menuGM.add(item);
                     */

                    item = new JMenuItem("Edit System (GM)...");
                                            item.setEnabled((selectedSystem != null)
                          && InterstellarMapPanel.this.campaign.isGM());
                    if (selectedSystem != null) {
                        final PlanetarySystem editTarget = selectedSystem;
                        item.addActionListener(evt -> openPlanetarySystemEditor(editTarget));
                    }
                    menuGM.add(item);

                    item = new JMenuItem("Recharge Jumpdrive");
                    item.setEnabled(InterstellarMapPanel.this.campaign.getPlayerForce()
                                          .getForceDetachment()
                                          .getCurrentLocation()
                                          .isRecharging(InterstellarMapPanel.this.campaign) &&
                                          InterstellarMapPanel.this.campaign.isGM());
                    item.addActionListener(evt -> {
                        InterstellarMapPanel.this.campaign.getPlayerForce().getForceDetachment().getCurrentLocation()
                              .chargeFully(InterstellarMapPanel.this.campaign);
                        InterstellarMapPanel.this.campaign.addReport(GENERAL, "GM: Jumpship drives fully charged");
                    });
                    menuGM.add(item);

                    popup.add(menuGM);
                    styleNavigationPopup(popup);
                    popup.show(e.getComponent(), e.getX() + 10, e.getY() + 10);
                }
            }

            private void handleMapClick(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    PlanetarySystem target = findSystemAt(e.getPoint());
                    MeasurementClick measurementClick = measurementState.click(target);
                    if (measurementClick.consumed()) {
                        measurementState = measurementClick.state();
                        measurementHoverSystem = null;
                        refreshMeasurementAssessment();
                        repaint();
                        return;
                    }
                    if (target == null) {
                        return;
                    }
                    if (e.getClickCount() >= 2) {
                        startSystemDive(target,
                              () -> hqView.getNavigationTab().getMapTab().switchPlanetaryMap(target));
                    } else {
                        if (e.isAltDown()) {
                            routePlanningHandler.plotRoute(target);
                            return;
                        } else if (e.isShiftDown()) {
                            routePlanningHandler.appendWaypoint(target);
                            return;
                        }
                        changeSelectedSystem(target);
                        repaint();
                    }
                }
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseMod != MouseEvent.BUTTON1) {
                    return;
                }
                Point delta = mapPointerGesture.drag(e.getPoint());
                if (delta != null) {
                    conf.centerX += delta.x / conf.scale;
                    conf.centerY += delta.y / conf.scale;
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                double systemSize = getSystemMarkerSize();
                Map<String, SystemRenderData> renderData =
                      getPreparedSystemRenderData(campaign.getLocalDate());
                PlanetarySystem previousHoveredSystem = findHoveredSystem(systemSize, renderData);
                if (lastMousePos == null) {
                    lastMousePos = new Point(e.getX(), e.getY());
                } else {
                    lastMousePos.x = e.getX();
                    lastMousePos.y = e.getY();
                }
                PlanetarySystem hoveredSystem = findHoveredSystem(systemSize, renderData);
                boolean hoverChanged = !isSameSystem(previousHoveredSystem, hoveredSystem);
                boolean measurementChanged = false;
                if (measurementState.enabled() && (measurementState.start() != null)
                      && (measurementState.end() == null)) {
                    if (!isSameSystem(measurementHoverSystem, hoveredSystem)) {
                        measurementHoverSystem = hoveredSystem;
                        refreshMeasurementAssessment();
                        measurementChanged = true;
                    }
                }
                if (hoverChanged || measurementChanged) {
                    repaint();
                }
            }
        });

        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "stopMeasuring");
        getActionMap().put("stopMeasuring", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (measurementState.enabled()) {
                    stopMeasuring();
                }
            }
        });
        if (RENDER_BENCHMARK_ENABLED) {
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                  KeyStroke.getKeyStroke(KeyEvent.VK_B,
                        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "toggleRenderBenchmark");
            getActionMap().put("toggleRenderBenchmark", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    toggleRenderBenchmark();
                }
            });
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                  KeyStroke.getKeyStroke(KeyEvent.VK_T,
                        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                  "toggleTransitionBenchmark");
            getActionMap().put("toggleTransitionBenchmark", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    toggleTransitionBenchmark();
                }
            });
            getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                  KeyStroke.getKeyStroke(KeyEvent.VK_Z,
                        InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
                  "toggleZoomBenchmark");
            getActionMap().put("toggleZoomBenchmark", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    toggleZoomBenchmark();
                }
            });
            LOGGER.info("Map render benchmarks enabled; Ctrl+Shift+B runs warm pan, "
                  + "Ctrl+Shift+T runs transition/cache regeneration, and Ctrl+Shift+Z runs zoom");
        }

        addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                zoom(Math.pow(1.175, -1 * e.getWheelRotation()), e.getPoint());
            }
        });

        pane = new JLayeredPane();
        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                long frameStartedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                now = InterstellarMapPanel.this.campaign.getLocalDate();
                refreshTravelVisualState();
                Graphics2D g2 = (Graphics2D) g;
                double ambientElapsedSeconds = STATIC_AMBIENT_PHASE_SECONDS;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    RenderViewKey renderViewKey = RenderViewKey.create(getWidth(), getHeight(), conf.centerX,
                        conf.centerY, conf.scale);
                long backgroundStartedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                paintStaticCartographyBackground(g2, renderViewKey);
                long backgroundFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                double size = getSystemMarkerSize();

                final Stroke thick = new BasicStroke(2.0f);
                final Stroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                      new float[] { 3 }, 0);

                    TerritoryAtlas atlas = getPreparedTerritoryAtlas(now);
                    TerritoryRenderKey territoryRenderKey = atlas == null ? null
                        : new TerritoryRenderKey(renderViewKey, now, cartographyDataRevision);
                Map<String, SystemRenderData> systemRenderData = getPreparedSystemRenderData(now);
                PlanetarySystem hoveredSystem = findHoveredSystem(size, systemRenderData);
                double semanticZoomReference = getSemanticZoomReference(conf.showPlanetNamesThreshold);
                    SemanticZoomProfile semanticZoom = SemanticZoomProfile.create(conf.scale, semanticZoomReference);
                                        CapitalDisplayDetail capitalDisplayDetail = ObjectUtility.nonNull(
                          (CapitalDisplayDetail) optCapitalDetail.getSelectedItem(), CapitalDisplayDetail.NATIONAL);
                    double markerQueryExtent = size * 2.0;
                      double rightVisualExtent = markerQueryExtent;
                      if (semanticZoom.ordinaryLabelAlpha() > 0.0) {
                        SystemMarkerLayout queryLayout = SystemMarkerLayout.create(
                            0.0, 0.0, size, RouteMarkerState.NONE, false, false);
                        rightVisualExtent = queryLayout.labelX()
                            + getMaximumSystemLabelWidth(g2, systemRenderData);
                      }
                    MapQueryBounds viewportQueryBounds = viewportSystemQueryBounds(
                          renderViewKey, markerQueryExtent, rightVisualExtent);
                    minX = viewportQueryBounds.minX();
                    minY = viewportQueryBounds.minY();
                    maxX = viewportQueryBounds.maxX();
                    maxY = viewportQueryBounds.maxY();
                    double visibleHpgNetworkAlpha = hpgNetworkLayerAlpha * semanticZoom.hpgNetworkAlpha();
                      HpgNetworkDetail hpgNetworkDetail = effectiveHpgNetworkDetail(
                          (HpgNetworkDetail) optHpgNetworkDetail.getSelectedItem(), conf.scale,
                          semanticZoomReference);

                Arc2D.Double arc = new Arc2D.Double();

                // Draw auras around a selected planet
                if (selectedSystem != null) {
                    final double x = map2scrX(selectedSystem.getX());
                    final double y = map2scrY(selectedSystem.getY());
                    // Contract Search Radius Aura
                    if (!InterstellarMapPanel.this.campaign.getCampaignOptions().get(CampaignOption.CONTRACT_MARKET_METHOD).isNone()
                              && MekHQ.getMHQOptions().getInterstellarMapShowContractSearchRadius()) {
                        final double z = map2scrX(selectedSystem.getX()
                                                        +
                                                        InterstellarMapPanel.this.campaign.getCampaignOptions()
                                                              .get(CampaignOption.CONTRACT_SEARCH_RADIUS));
                        final double contractSearchRadius = z - x;
                        g2.setPaint(MekHQ.getMHQOptions().getInterstellarMapContractSearchRadiusColour());
                        g2.setStroke(CONTRACT_SEARCH_RANGE_RING_STROKE);
                        arc.setArcByCenter(x, y, contractSearchRadius, 0, 360, Arc2D.OPEN);
                        g2.draw(arc);
                    }

                    // Acquisition Search Radius Aura
                    if (InterstellarMapPanel.this.campaign.getCampaignOptions().get(CampaignOption.USE_PLANETARY_ACQUISITION)
                              && MekHQ.getMHQOptions().getInterstellarMapShowPlanetaryAcquisitionRadius()
                              && (conf.scale > MekHQ.getMHQOptions()
                                                     .getInterstellarMapShowPlanetaryAcquisitionRadiusMinimumZoom())) {
                        final double z = map2scrX(selectedSystem.getX()
                                                        + (MHQConstants.MAX_JUMP_RADIUS
                                                                 *
                                                                 InterstellarMapPanel.this.campaign.getCampaignOptions()
                                                                       .get(CampaignOption.MAX_JUMPS_PLANETARY_ACQUISITION)));
                        final double acquisitionRadius = z - x;
                        g2.setPaint(MekHQ.getMHQOptions().getInterstellarMapPlanetaryAcquisitionRadiusColour());
                        g2.setStroke(PLANETARY_ACQUISITION_RANGE_RING_STROKE);
                        arc.setArcByCenter(x, y, acquisitionRadius, 0, 360, Arc2D.OPEN);
                        g2.draw(arc);
                    }

                    // Jump Radius Aura
                    if (MekHQ.getMHQOptions().getInterstellarMapShowJumpRadius()
                              && (conf.scale > MekHQ.getMHQOptions().getInterstellarMapShowJumpRadiusMinimumZoom())) {
                        final double z = map2scrX(selectedSystem.getX() + MHQConstants.MAX_JUMP_RADIUS);
                        final double jumpRadius = z - x;
                        g2.setPaint(MekHQ.getMHQOptions().getInterstellarMapJumpRadiusColour());
                        g2.setStroke(JUMP_RANGE_RING_STROKE);
                        arc.setArcByCenter(x, y, jumpRadius, 0, 360, Arc2D.OPEN);
                        g2.draw(arc);
                    }

                    if (visibleHpgNetworkAlpha > 0.0) {
                        paintLayerWithAlpha(g2, visibleHpgNetworkAlpha, hpgGraphics -> {
                            final double z = map2scrX(selectedSystem.getX() + 50);
                            final double jumpRadius = z - x;
                            hpgGraphics.setPaint(HPG_RANGE_RING_COLOR);
                            hpgGraphics.setStroke(HPG_RANGE_RING_STROKE);
                            arc.setArcByCenter(x, y, jumpRadius, 0, 360, Arc2D.OPEN);
                            hpgGraphics.draw(arc);
                        });
                    }
                }

                double visibleTerritoryAlpha = territoryLayerAlpha * semanticZoom.territoryAlpha();
                double visibleFactionLogoAlpha = semanticZoom.factionLogoAlpha() * getFactionMapModeAlpha();
                JumpPath activeJumpPath = getActiveJumpPath();
                boolean showRouteActivation = isRouteActivationAnimating();
                List<PlanetarySystem> activeRouteSystems = showRouteActivation
                      ? resolveRouteSystems(activatingRouteSystemIds)
                      : getPathSystems(activeJumpPath);
                if (showRouteActivation && (activeRouteSystems.size() != activatingRouteSystemIds.size())) {
                    showRouteActivation = false;
                    activeRouteSystems = getPathSystems(activeJumpPath);
                }
                int revealedProposedRouteSystemCount = showRouteActivation
                      ? 0
                      : getRevealedProposedRouteSystemCount();
                        boolean useRetainedMapModeTransition = canUseRetainedMapModeTransition(
                            atlas != null, mapModeAnimating || mapModeTransitionSettling,
                        territoryLayerAnimating || hpgNetworkLayerAnimating,
                        proposedRouteAnimationTimer.isRunning(), showRouteActivation)
                        && (territoryRenderKey != null)
                        && (renderLayerOverscan(territoryRenderKey.viewKey().width(),
                            territoryRenderKey.viewKey().height()) > 0);
                        if (mapModeAnimating && isPreparingMapModeTransition()
                            && !useRetainedMapModeTransition) {
                          mapModeTransitionCacheStage = MapModeTransitionCacheStage.READY;
                          mapModeAnimationStartTime = System.nanoTime();
                        }
                boolean useRetainedCartography = canUseRetainedCartography(
                      atlas != null, hasActiveRetainedCartographyAnimation());
                    boolean scaleChanging = isZoomInteractionActive();
                    boolean useMergedNavigation = canUseMergedNavigation(useRetainedCartography,
                        proposedRouteAnimationTimer.isRunning(), showRouteActivation, scaleChanging);
                    boolean useRetainedNavigation = useMergedNavigation || useRetainedMapModeTransition;
                    boolean supportingLayerAnimating = territoryLayerAnimating || territoryLayerSettling
                        || hpgNetworkLayerAnimating;
                    boolean useRetainedSystemArt = canUseRetainedSystemArt(
                        useRetainedCartography, useRetainedMapModeTransition,
                        supportingLayerAnimating, scaleChanging);
                long territoryStartedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                    int territoryCacheHitsBefore = RENDER_PROFILING_ENABLED
                        ? territoryRenderCache.getReuseCount()
                            + retainedCartographyRenderCache.getReuseCount()
                            + retainedSystemArtRenderCache.getReuseCount()
                            + retainedNavigationRenderCache.getReuseCount()
                            + mapModeTransitionBaseRenderCache.getReuseCount()
                            + mapModeTransitionSystemRenderCache.getReuseCount()
                            + previousMapModeRenderCache.getReuseCount()
                            + targetMapModeRenderCache.getReuseCount() : 0;
                    int territoryStripRefreshesBefore = RENDER_PROFILING_ENABLED
                        ? territoryRenderCache.getStripRefreshCount()
                            + retainedCartographyRenderCache.getStripRefreshCount()
                            + retainedSystemArtRenderCache.getStripRefreshCount()
                            + retainedNavigationRenderCache.getStripRefreshCount()
                            + mapModeTransitionBaseRenderCache.getStripRefreshCount()
                            + mapModeTransitionSystemRenderCache.getStripRefreshCount()
                            + previousMapModeRenderCache.getStripRefreshCount()
                            + targetMapModeRenderCache.getStripRefreshCount() : 0;
                    int territoryFullRendersBefore = RENDER_PROFILING_ENABLED
                        ? territoryRenderCache.getFullRenderCount()
                            + retainedCartographyRenderCache.getFullRenderCount()
                            + retainedSystemArtRenderCache.getFullRenderCount()
                            + retainedNavigationRenderCache.getFullRenderCount()
                            + mapModeTransitionBaseRenderCache.getFullRenderCount()
                            + mapModeTransitionSystemRenderCache.getFullRenderCount()
                            + previousMapModeRenderCache.getFullRenderCount()
                            + targetMapModeRenderCache.getFullRenderCount() : 0;
                if (!useRetainedNavigation) {
                    retainedNavigationRenderCache.clear();
                }
                if (useRetainedMapModeTransition) {
                    paintRetainedMapModeTransition(g2, atlas, territoryRenderKey, systemRenderData,
                          previousMapMode, targetMapMode, hpgNetworkDetail, size,
                          visibleTerritoryAlpha, semanticZoom.factionLogoAlpha() * FACTION_LOGO_OPACITY,
                          visibleHpgNetworkAlpha, semanticZoom, thick, dashed, activeRouteSystems,
                          revealedProposedRouteSystemCount, mapModeAnimationProgress);
                } else if (useMergedNavigation && (territoryRenderKey != null)) {
                    paintRetainedNavigationLayer(g2, atlas, territoryRenderKey, systemRenderData,
                          targetMapMode, hpgNetworkDetail, size, visibleTerritoryAlpha,
                          visibleFactionLogoAlpha * FACTION_LOGO_OPACITY, visibleHpgNetworkAlpha,
                          semanticZoom, thick, dashed, activeRouteSystems,
                          revealedProposedRouteSystemCount);
                } else if (useRetainedCartography && (territoryRenderKey != null)) {
                    paintRetainedCartographyLayer(g2, atlas, territoryRenderKey,
                          targetMapMode, hpgNetworkDetail, size, visibleTerritoryAlpha,
                          visibleFactionLogoAlpha * FACTION_LOGO_OPACITY,
                          visibleHpgNetworkAlpha, semanticZoom);
                } else if ((atlas != null) && (territoryRenderKey != null)
                      && (visibleTerritoryAlpha > 0.0)) {
                      paintStaticTerritoryLayer(g2, atlas, territoryRenderKey, visibleTerritoryAlpha);
                }
                    if (territoryLayerSettling && (atlas != null) && (territoryRenderKey != null)) {
                      continueTerritoryLayerSettling(atlas, territoryRenderKey, targetMapMode,
                          hpgNetworkDetail, size, visibleTerritoryAlpha,
                          visibleFactionLogoAlpha * FACTION_LOGO_OPACITY,
                          visibleHpgNetworkAlpha, semanticZoom);
                    }
                long territoryFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                long factionLogoStartedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                        if (!useRetainedCartography && !useRetainedMapModeTransition
                            && (atlas != null) && (territoryRenderKey != null)
                            && (visibleFactionLogoAlpha > 0.0)) {
                    FactionLogoRenderKey factionLogoRenderKey = createFactionLogoRenderKey(territoryRenderKey);
                    paintStaticFactionLogoLayer(g2, atlas, factionLogoRenderKey,
                          visibleFactionLogoAlpha * FACTION_LOGO_OPACITY);
                }
                long factionLogoFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                long staticPhaseFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;

                if (!useRetainedNavigation) {
                    drawReachability(g2, size, semanticZoom.detailedOverlayAlpha(), systemRenderData);
                }

                if (!useRetainedNavigation && !showRouteActivation) {
                      drawProposedRoute(g2, arc, size, revealedProposedRouteSystemCount,
                          semanticZoom.detailedOverlayAlpha());
                }

                                if (!useRetainedNavigation && (visibleHpgNetworkAlpha > 0.0)) {
                                        paintLayerWithAlpha(g2, visibleHpgNetworkAlpha,
                                                    hpgGraphics -> InterstellarMapPanel.this.drawHpgNetworkLayer(hpgGraphics,
                                                                thick, dashed, hpgNetworkDetail));
                }

                if (!useRetainedNavigation && !activeRouteSystems.isEmpty()) {
                    drawActiveRoute(g2, arc, activeRouteSystems, size,
                          showRouteActivation ? routeActivationProgress : 1.0,
                          semanticZoom.detailedOverlayAlpha());
                }
                long routePhaseFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                    if (useRetainedSystemArt && !useMergedNavigation
                        && (territoryRenderKey != null)) {
                    paintRetainedSystemArtLayer(g2, territoryRenderKey, systemRenderData,
                          targetMapMode, hpgNetworkDetail, size, visibleTerritoryAlpha,
                          visibleFactionLogoAlpha * FACTION_LOGO_OPACITY,
                          visibleHpgNetworkAlpha, semanticZoom);
                }
                int territoryCacheHits = RENDER_PROFILING_ENABLED
                      ? territoryRenderCache.getReuseCount()
                          + retainedCartographyRenderCache.getReuseCount()
                          + retainedSystemArtRenderCache.getReuseCount()
                          + retainedNavigationRenderCache.getReuseCount()
                          + mapModeTransitionBaseRenderCache.getReuseCount()
                          + mapModeTransitionSystemRenderCache.getReuseCount()
                          + previousMapModeRenderCache.getReuseCount()
                          + targetMapModeRenderCache.getReuseCount() - territoryCacheHitsBefore : 0;
                int territoryStripRefreshes = RENDER_PROFILING_ENABLED
                      ? territoryRenderCache.getStripRefreshCount()
                          + retainedCartographyRenderCache.getStripRefreshCount()
                          + retainedSystemArtRenderCache.getStripRefreshCount()
                          + retainedNavigationRenderCache.getStripRefreshCount()
                          + mapModeTransitionBaseRenderCache.getStripRefreshCount()
                          + mapModeTransitionSystemRenderCache.getStripRefreshCount()
                          + previousMapModeRenderCache.getStripRefreshCount()
                          + targetMapModeRenderCache.getStripRefreshCount()
                          - territoryStripRefreshesBefore : 0;
                int territoryFullRenders = RENDER_PROFILING_ENABLED
                      ? territoryRenderCache.getFullRenderCount()
                          + retainedCartographyRenderCache.getFullRenderCount()
                          + retainedSystemArtRenderCache.getFullRenderCount()
                          + retainedNavigationRenderCache.getFullRenderCount()
                          + mapModeTransitionBaseRenderCache.getFullRenderCount()
                          + mapModeTransitionSystemRenderCache.getFullRenderCount()
                          + previousMapModeRenderCache.getFullRenderCount()
                          + targetMapModeRenderCache.getFullRenderCount()
                          - territoryFullRendersBefore : 0;

                                Set<PlanetarySystem> activeRouteWaypoints = new HashSet<>(activeRouteSystems);
                                Set<PlanetarySystem> routeWaypoints = new HashSet<>();
                                for (int systemIndex = 0;
                                            systemIndex < revealedProposedRouteSystemCount;
                                            systemIndex++) {
                                        routeWaypoints.add(jumpPath.get(systemIndex));
                                }
                routeWaypoints.addAll(activeRouteSystems);

                PlanetarySystem currentSystem = InterstellarMapPanel.this.campaign.getCurrentSystem();
                Set<String> priorityLabelSystemIds = new HashSet<>();
                if (selectedSystem != null) {
                    priorityLabelSystemIds.add(selectedSystem.getId());
                }
                if (currentSystem != null) {
                    priorityLabelSystemIds.add(currentSystem.getId());
                }
                if (!activeRouteSystems.isEmpty()) {
                    priorityLabelSystemIds.add(activeRouteSystems.getLast().getId());
                }
                if (!showRouteActivation && !jumpPath.isEmpty()
                      && (revealedProposedRouteSystemCount >= jumpPath.size())) {
                                        priorityLabelSystemIds.add(jumpPath.get(jumpPath.size() - 1).getId());
                }

                    MapPresentationData presentationData = mapPresentationData;
                    Map<String, StrategicMarker> strategicMarkers = presentationData.strategicMarkers();
                    Map<String, Integer> playerBaseCounts = presentationData.playerBaseCounts();

                boolean isUseFactionStandingOutlawing =
                            campaign.getCampaignOptions().isUseFactionStandingOutlawedSafe();
                Faction campaignFaction = campaign.getPlayerForce().getFaction();
                FactionStandings factionStandings = campaign.getPlayerForce().getFactionStandings();
                LocalDate today = campaign.getLocalDate();
                Set<Faction> contractEmployers = presentationData.contractEmployers();
                Set<Faction> contractTargets = presentationData.contractTargets();

                FactionHints factionHints = FactionHints.getInstance();

                Graphics2D previousMapModeGraphics = null;
                Graphics2D targetMapModeGraphics = g2;
                if (mapModeAnimating) {
                    previousMapModeGraphics = createLayerGraphicsWithAlpha(g2, 1.0 - mapModeAnimationProgress);
                    targetMapModeGraphics = createLayerGraphicsWithAlpha(g2, mapModeAnimationProgress);
                }

                List<PlanetarySystem> viewportSystems = systemSpatialIndex.query(
                      minX, minY, maxX, maxY, currentSystem, selectedSystem);
                List<PlanetarySystem> renderedSystems = new ArrayList<>(viewportSystems.size());
                List<SystemMarkerLayout> renderedSystemLayouts = new ArrayList<>(viewportSystems.size());
                int visibleSystemCount = 0;
                for (PlanetarySystem system : viewportSystems) {
                    SystemRenderData renderData = systemRenderData.get(system.getId());
                    boolean routeWaypoint = routeWaypoints.contains(system);
                    boolean requiredNavigationSystem = routeWaypoint
                          || isSameSystem(system, currentSystem)
                          || isSameSystem(system, selectedSystem);
                    if (shouldRenderSystem(true, renderData.empty(), optEmptySystems.isSelected(),
                          requiredNavigationSystem)) {
                        renderedSystems.add(system);
                    visibleSystemCount++;
                        double x = map2scrX(system.getX());
                        double y = map2scrY(system.getY());
                        boolean systemEmpty = renderData.empty();
                        RouteMarkerState routeMarkerState = activeRouteWaypoints.contains(system)
                            ? RouteMarkerState.ACTIVE
                            : (routeWaypoint ? RouteMarkerState.PLANNED : RouteMarkerState.NONE);
                        SystemMarkerLayout markerLayout = createSystemMarkerLayout(system, size, routeMarkerState,
                            hoveredSystem);
                        renderedSystemLayouts.add(markerLayout);
                        boolean showRouteContact = systemEmpty && routeWaypoint && !optEmptySystems.isSelected();
                                                boolean showSystemArt = !systemEmpty || optEmptySystems.isSelected()
                            || isSameSystem(system, currentSystem)
                            || isSameSystem(system, selectedSystem)
                                                        || routeWaypoint;
                        boolean systemArtRetained = useRetainedSystemArt
                            && (!systemEmpty || optEmptySystems.isSelected());
                        if (showRouteContact) {
                            drawNavigationContact(g2, arc, x, y, size);
                                        } else if (!systemArtRetained && showSystemArt
                                            && (semanticZoom.systemDetailAlpha() > 0.0)) {
                                                    drawIntrinsicStar(g2, arc, system, x, y, markerLayout.size(),
                                                          semanticZoom.systemDetailAlpha());
                        }

                        if (previousMapModeGraphics != null) {
                            drawMapModeOverlay(previousMapModeGraphics, arc, system, renderData, markerLayout,
                                                          showSystemArt, showRouteContact, previousMapMode,
                                                          semanticZoom.systemContactAlpha(), semanticZoom.systemDetailAlpha(),
                                                          semanticZoom.serviceAlpha(), !systemArtRetained);
                        }
                        if (targetMapModeGraphics != null) {
                            drawMapModeOverlay(targetMapModeGraphics, arc, system, renderData, markerLayout,
                                                          showSystemArt, showRouteContact, targetMapMode,
                                                          semanticZoom.systemContactAlpha(), semanticZoom.systemDetailAlpha(),
                                                          semanticZoom.serviceAlpha(), !systemArtRetained);
                        }
                        if (optCapitals.isSelected()) {
                            drawCapitalHierarchyOverlay(g2, system, markerLayout, renderData,
                                  capitalDisplayDetail, semanticZoom.systemDetailAlpha(),
                                  semanticZoom.capitalAlpha(), semanticZoom.detailedOverlayAlpha());
                        }

                        StrategicMarker strategicMarker = strategicMarkers.get(system.getId());
                        double markerOperationsAlpha = visibleOperationAlpha(
                              operationsLayerAlpha, semanticZoom, strategicMarker);
                        if (markerOperationsAlpha > 0.0) {
                            double operationExpansion = semanticZoom.systemDetailAlpha();
                            if (strategicMarker.hasActiveScenario()) {
                                paintLayerWithAlpha(g2, markerOperationsAlpha * (1.0 - operationExpansion),
                                    markerGraphics -> drawStrategicOperationMarker(markerGraphics, markerLayout));
                            }
                            double detailedOperationAlpha = strategicMarker.hasActiveScenario()
                                  ? markerOperationsAlpha * operationExpansion
                                  : markerOperationsAlpha;
                            paintLayerWithAlpha(g2, detailedOperationAlpha,
                                markerGraphics -> drawOperationMarker(markerGraphics, markerLayout,
                                    strategicMarker, operationExpansion));
                        }

                        // Outlaw status image
                        if (isUseFactionStandingOutlawing) {
                            boolean isOutlawedInSystem = !FactionStandingUtilities.canEnterTargetSystem(campaignFaction,
                                  factionStandings, null, renderData.population(), renderData.factions(), today,
                                  contractEmployers, contractTargets, factionHints);
                            if (isOutlawedInSystem) {
                                  drawRestrictedSystemMarker(g2, markerLayout,
                                      semanticZoom.detailedOverlayAlpha());
                            }
                        }

                        if (campaign.hasPlanetarySystemOverride(system.getId())
                            && (semanticZoom.serviceAlpha() > 0.0)) {
                            paintLayerWithAlpha(g2, semanticZoom.serviceAlpha(),
                                  markerGraphics -> drawGmEditedSystemMarker(markerGraphics, markerLayout,
                                      semanticZoom.serviceAlpha()));
                        }

                        boolean isCurrentSystem = isSameSystem(system, currentSystem);
                        int playerBaseCount = playerBaseCounts.getOrDefault(system.getId(), 0);
                        if (playerBaseCount > 0) {
                            drawPlayerBaseMarker(g2, markerLayout, playerBaseCount,
                                  semanticZoom.systemDetailAlpha());
                        }
                        if (isCurrentSystem && (semanticZoom.strategicContactAlpha() > 0.0)) {
                            paintLayerWithAlpha(g2, semanticZoom.strategicContactAlpha(),
                                  markerGraphics -> drawStrategicCurrentLocationMarker(markerGraphics, markerLayout));
                        }
                        boolean isSelectionAnimating = isSelectionAnimationTarget(system);
                        if ((hoveredSystem != null) && hoveredSystem.equals(system)
                            && !isSameSystem(system, selectedSystem)) {
                            paintLayerWithAlpha(g2, 1.0 - semanticZoom.detailedOverlayAlpha(),
                                  markerGraphics -> drawStrategicFocusMarker(markerGraphics, markerLayout,
                                      HOVERED_SYSTEM_COLOR));
                            paintLayerWithAlpha(g2, semanticZoom.detailedOverlayAlpha(),
                                  markerGraphics -> drawHoveredSystemMarker(markerGraphics, markerLayout));
                        }
                        if ((selectedSystem != null) && selectedSystem.equals(system)) {
                            paintLayerWithAlpha(g2, 1.0 - semanticZoom.detailedOverlayAlpha(),
                                  markerGraphics -> drawStrategicFocusMarker(markerGraphics, markerLayout,
                                      SELECTED_SYSTEM_COLOR));
                            if (isSelectionAnimating) {
                                paintLayerWithAlpha(g2, semanticZoom.detailedOverlayAlpha(),
                                      markerGraphics -> drawSelectionAnimationMarker(markerGraphics, markerLayout,
                                          selectionAnimationProgress));
                            } else {
                                paintLayerWithAlpha(g2, semanticZoom.detailedOverlayAlpha(),
                                      markerGraphics -> drawSelectedSystemMarker(markerGraphics, markerLayout,
                                          system.getId(), ambientElapsedSeconds));
                            }
                        }
                        if ((semanticZoom.currentLocationAlpha() > 0.0) && !isSystemHopAnimating()
                            && isCurrentSystem) {
                            paintLayerWithAlpha(g2, semanticZoom.currentLocationAlpha(),
                                markerGraphics -> drawCurrentLocationMarker(markerGraphics, markerLayout,
                                    semanticZoom.systemDetailAlpha()));
                        }
                        HPGRating hpgRating = renderData.hpgRating();
                        if ((visibleHpgNetworkAlpha > 0.0) && hpgNetworkDetail.includes(hpgRating)
                              && (semanticZoom.detailedOverlayAlpha() > 0.0)) {
                            paintLayerWithAlpha(g2,
                                  visibleHpgNetworkAlpha * semanticZoom.detailedOverlayAlpha(),
                                  markerGraphics -> drawHpgStationMarker(markerGraphics, markerLayout, hpgRating));
                        }
                    }
                }

                if ((semanticZoom.currentLocationAlpha() > 0.0) && isSystemHopAnimating()) {
                      double hopMarkerSize = size;
                    paintLayerWithAlpha(g2, semanticZoom.currentLocationAlpha(),
                          markerGraphics -> drawSystemHopMarker(markerGraphics, hopMarkerSize, hoveredSystem));
                }

                if (previousMapModeGraphics != null) {
                    previousMapModeGraphics.dispose();
                }
                if (targetMapModeGraphics != g2) {
                    targetMapModeGraphics.dispose();
                }
                long systemPhaseFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;

                if (!activeRouteSystems.isEmpty()) {
                    drawActiveRouteWaypointBadges(g2, activeRouteSystems, size,
                          showRouteActivation ? routeActivationProgress : 1.0, hoveredSystem,
                          semanticZoom.routeBadgeAlpha());
                }
                if (!showRouteActivation) {
                    drawRouteWaypointBadges(g2, jumpPath, size, PLANNED_ROUTE_COLOR,
                          revealedProposedRouteSystemCount, hoveredSystem, semanticZoom.routeBadgeAlpha());
                }

                if (!activeRouteSystems.isEmpty()) {
                    drawRouteConstraints(g2, activeRouteSystems, cachedActiveRouteAssessment, size,
                          activeRouteSystems.size(), RouteMarkerState.ACTIVE, semanticZoom.routeBadgeAlpha());
                }
                if (!showRouteActivation) {
                    drawRouteConstraints(g2, getPathSystems(jumpPath), cachedProposedRouteAssessment, size,
                          revealedProposedRouteSystemCount, RouteMarkerState.PLANNED,
                          semanticZoom.routeBadgeAlpha());
                }

                    Set<PlanetarySystem> routeStatusDestinations = new HashSet<>();
                for (RouteConstraintMarker marker : routeConstraintMarkers(activeRouteSystems,
                      cachedActiveRouteAssessment)) {
                    if ((marker.legIndex() + 2) <= activeRouteSystems.size()) {
                        routeStatusDestinations.add(marker.destination());
                    }
                }
                    if (!showRouteActivation) {
                    for (RouteConstraintMarker marker : routeConstraintMarkers(getPathSystems(jumpPath),
                          cachedProposedRouteAssessment)) {
                        if ((marker.legIndex() + 2) <= revealedProposedRouteSystemCount) {
                            routeStatusDestinations.add(marker.destination());
                        }
                    }
                    }

                // cycle through planets again and assign names - to make sure names go on
                // outside
                for (int systemIndex = 0; systemIndex < renderedSystems.size(); systemIndex++) {
                    PlanetarySystem system = renderedSystems.get(systemIndex);
                    SystemRenderData renderData = systemRenderData.get(system.getId());
                    boolean routeWaypoint = routeWaypoints.contains(system);
                        double y = map2scrY(system.getY());
                        boolean isPriorityLabel = priorityLabelSystemIds.contains(system.getId())
                            || isSameSystem(system, hoveredSystem);
                        double baseLabelAlpha = isPriorityLabel
                            ? 1.0
                            : (routeWaypoint
                                ? semanticZoom.routeLabelAlpha()
                                : semanticZoom.ordinaryLabelAlpha());
                        if (baseLabelAlpha > 0.0) {
                            String planetName = renderData.printableName();
                            String stellarDetail = renderData.stellarDetail();
                                SystemMarkerLayout markerLayout = renderedSystemLayouts.get(systemIndex);
                                double markerRadius = Math.max(5.0, size * 0.9);
                                  double ordinaryLabelX = markerLayout.labelX();
                                  HPGRating hpgRating = renderData.hpgRating();
                                  if ((visibleHpgNetworkAlpha > 0.0) && hpgNetworkDetail.includes(hpgRating)
                                      && (semanticZoom.detailedOverlayAlpha() > 0.0)) {
                                    double hpgMarkerRadius = hpgStationMarkerRadius(markerLayout.size());
                                    ordinaryLabelX = Math.max(ordinaryLabelX,
                                        markerLayout.hpgStationAnchor(hpgMarkerRadius).x
                                            + hpgMarkerRadius + UIUtil.scaleForGUI(4));
                                  }
                                final float xPos = (float) (routeStatusDestinations.contains(system)
                                    ? interpolate(ordinaryLabelX, markerLayout.routeStatusLabelX(markerRadius),
                                        semanticZoom.routeBadgeAlpha())
                                    : ordinaryLabelX);
                            final float yPos = (float) y;
                            drawSystemLabel(g2, planetName, stellarDetail, xPos, yPos,
                                  isPriorityLabel ? Color.WHITE : SYSTEM_LABEL_COLOR, baseLabelAlpha,
                                  semanticZoom.stellarDetailAlpha() * baseLabelAlpha);
                        }
                }

                NavigationInstrumentLayout instrumentLayout = createNavigationInstrumentLayout(
                      getWidth(), getHeight(), conf.scale);
                drawReachabilityAnnotation(g2, size, semanticZoom.detailedOverlayAlpha());
                    drawMeasurement(g2, instrumentLayout);
                drawNavigationInstrument(g2, instrumentLayout);
                drawSystemDiveOverlay(g2, getWidth(), getHeight());
                if (RENDER_PROFILING_ENABLED) {
                    long frameFinishedNanos = System.nanoTime();
                    renderPerformanceTracker.record(frameFinishedNanos - frameStartedNanos,
                          staticPhaseFinishedNanos - frameStartedNanos,
                          backgroundFinishedNanos - backgroundStartedNanos,
                          territoryFinishedNanos - territoryStartedNanos,
                          factionLogoFinishedNanos - factionLogoStartedNanos,
                          routePhaseFinishedNanos - staticPhaseFinishedNanos,
                          systemPhaseFinishedNanos - routePhaseFinishedNanos,
                          frameFinishedNanos - systemPhaseFinishedNanos, visibleSystemCount, territoryCacheHits,
                          territoryStripRefreshes, territoryFullRenders,
                          useRetainedCartography || useRetainedMapModeTransition,
                          useMergedNavigation || useRetainedMapModeTransition);
                    if (transitionBenchmarkRun != null
                          && isExactTransitionBenchmarkFrame(mapModeAnimating,
                              retainedCartographyProvisional,
                              useRetainedCartography || useRetainedMapModeTransition,
                              useMergedNavigation || useRetainedMapModeTransition)) {
                        transitionBenchmarkExactFramePainted = true;
                    }
                    if (zoomBenchmarkRun != null) {
                        RenderBenchmarkCamera paintedCamera = new RenderBenchmarkCamera(
                              conf.centerX, conf.centerY, conf.scale);
                        zoomBenchmarkCameraPainted = paintedCamera.equals(zoomBenchmarkExpectedCamera);
                        if (isExactZoomBenchmarkFrame(retainedCartographyProvisional,
                              useRetainedCartography || useRetainedMapModeTransition,
                              useMergedNavigation || useRetainedMapModeTransition)) {
                            zoomBenchmarkExactFramePainted = true;
                        }
                    }
                      if (!renderBenchmarkTimer.isRunning() && !transitionBenchmarkTimer.isRunning()
                          && !zoomBenchmarkTimer.isRunning()
                          && renderPerformanceTracker.shouldReport(frameFinishedNanos)) {
                        LOGGER.info("{} timers[layer={}, selection={}, proposedRoute={}, travel={}, dive={}]",
                              renderPerformanceTracker.reportAndReset(frameFinishedNanos),
                              layerAnimationTimer.isRunning(), selectionAnimationTimer.isRunning(),
                                                        proposedRouteAnimationTimer.isRunning(), travelAnimationTimer.isRunning(),
                            systemDiveAnimationTimer.isRunning());
                    }
                }
                if (mapModeTransitionSettling && !isTransitionBenchmarkAwaitingSettledFrame()) {
                    finishSettledMapModeTransitionFrame();
                }
            }
        };
        pane.add(mapPanel, Integer.valueOf(1));

        optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
                optionPanel.setOpaque(false);
                optionPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(7), UIUtil.scaleForGUI(8),
                            UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(8)));

                JPanel optionHeader = new JPanel();
                optionHeader.setLayout(new BoxLayout(optionHeader, BoxLayout.X_AXIS));
                optionHeader.setOpaque(false);
                optionHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
                optionHeader.add(createLabel(LAYER_CONTROL_HEADING));
                optionPanel.add(optionHeader);

                                optFactions = createOptionRadioButton("Faction", MapMode.FACTION);
        optionPanel.add(optFactions);
                    optTech = createOptionRadioButton("Technology", MapMode.TECHNOLOGY);
        optionPanel.add(optTech);
                    optIndustry = createOptionRadioButton("Industry", MapMode.INDUSTRY);
        optionPanel.add(optIndustry);
                    optRawMaterials = createOptionRadioButton("Raw Materials", MapMode.RAW_MATERIALS);
        optionPanel.add(optRawMaterials);
                    optOutput = createOptionRadioButton("Output", MapMode.OUTPUT);
        optionPanel.add(optOutput);
                    optAgriculture = createOptionRadioButton("Agriculture", MapMode.AGRICULTURE);
        optionPanel.add(optAgriculture);
                    optPopulation = createOptionRadioButton("Population", MapMode.POPULATION);
        optionPanel.add(optPopulation);
                    optHPG = createOptionRadioButton("HPG", MapMode.HPG);
        optionPanel.add(optHPG);
                    optRecharge = createOptionRadioButton("Recharge Stations", MapMode.RECHARGE_STATIONS);
        optionPanel.add(optRecharge);
                    optAcademies = createOptionRadioButton("Academies", MapMode.ACADEMIES);
        optionPanel.add(optAcademies);
                    optHiringHalls = createOptionRadioButton("Hiring Halls", MapMode.HIRING_HALLS);
        optionPanel.add(optHiringHalls);
                    optDiseases = createOptionRadioButton("Disease Outbreaks", MapMode.DISEASE_OUTBREAKS);
        optionPanel.add(optDiseases);

        ButtonGroup colorChoice = new ButtonGroup();
        colorChoice.add(optFactions);
        colorChoice.add(optTech);
        colorChoice.add(optIndustry);
        colorChoice.add(optRawMaterials);
        colorChoice.add(optOutput);
        colorChoice.add(optAgriculture);
        colorChoice.add(optPopulation);
        colorChoice.add(optHPG);
        colorChoice.add(optRecharge);
        colorChoice.add(optAcademies);
        colorChoice.add(optHiringHalls);
        colorChoice.add(optDiseases);
        // factions by default
        optFactions.setSelected(true);

        optionPanel.add(Box.createRigidArea(new Dimension(0, 7)));
        optionPanel.add(createOptionDivider());
        optionPanel.add(Box.createRigidArea(new Dimension(0, 7)));
        optionPanel.add(createLabel(OVERLAY_CONTROL_HEADING));
        optEmptySystems = createOptionCheckBox("Empty systems");
        optEmptySystems.setSelected(false);
        optEmptySystems.addActionListener(e -> repaint());
        optionPanel.add(optEmptySystems);
        optTerritory = createOptionCheckBox("Territory");
        optTerritory.setSelected(true);
        optTerritory.addActionListener(e -> startTerritoryLayerAnimation());
        optionPanel.add(optTerritory);
                optCapitals = createOptionCheckBox("Capitals");
                optCapitals.setSelected(true);
                optCapitalDetail = new ImmersiveComboBox<>(CapitalDisplayDetail.values());
                optCapitalDetail.setSelectedItem(CapitalDisplayDetail.NATIONAL);
                Dimension capitalDetailSize = new Dimension(UIUtil.scaleForGUI(148),
              optCapitalDetail.getPreferredSize().height);
                optCapitalDetail.setPreferredSize(capitalDetailSize);
                optCapitalDetail.setMinimumSize(capitalDetailSize);
                optCapitalDetail.setMaximumSize(capitalDetailSize);
                optCapitalDetail.setToolTipText(
              "Maximum capital hierarchy shown. District capitals appear only at navigation detail.");
                optCapitalDetail.addActionListener(event -> repaint());
                optCapitals.addActionListener(event -> {
            optCapitalDetail.setEnabled(optCapitals.isSelected());
            repaint();
                });
                JPanel capitalControl = new JPanel();
                capitalControl.setLayout(new BoxLayout(capitalControl, BoxLayout.X_AXIS));
                capitalControl.setOpaque(false);
                capitalControl.setAlignmentX(Component.LEFT_ALIGNMENT);
                capitalControl.add(optCapitals);
                capitalControl.add(Box.createHorizontalGlue());
                capitalControl.add(optCapitalDetail);
                optionPanel.add(capitalControl);
        optHPGNetwork = createOptionCheckBox("HPG Network");
          optHpgNetworkDetail = new ImmersiveComboBox<>(HpgNetworkDetail.values());
        optHpgNetworkDetail.setSelectedItem(HpgNetworkDetail.CLASS_A_B);
        optHpgNetworkDetail.setEnabled(false);
          Dimension hpgDetailSize = new Dimension(UIUtil.scaleForGUI(148),
              optHpgNetworkDetail.getPreferredSize().height);
          optHpgNetworkDetail.setPreferredSize(hpgDetailSize);
          optHpgNetworkDetail.setMinimumSize(hpgDetailSize);
          optHpgNetworkDetail.setMaximumSize(hpgDetailSize);
        optHpgNetworkDetail.setToolTipText(
              "Maximum HPG detail. Wide zoom shows A only; medium zoom shows A-B; detailed zoom shows station badges.");
        optHpgNetworkDetail.addActionListener(e -> repaint());
        optHPGNetwork.addActionListener(e -> {
            optHpgNetworkDetail.setEnabled(optHPGNetwork.isSelected());
            startHpgNetworkLayerAnimation();
        });
          JPanel hpgControl = new JPanel();
          hpgControl.setLayout(new BoxLayout(hpgControl, BoxLayout.X_AXIS));
          hpgControl.setOpaque(false);
          hpgControl.setAlignmentX(Component.LEFT_ALIGNMENT);
          hpgControl.add(optHPGNetwork);
          hpgControl.add(Box.createHorizontalGlue());
          hpgControl.add(optHpgNetworkDetail);
          optionPanel.add(hpgControl);
        optOperations = createOptionCheckBox("Operations");
        optOperations.setSelected(true);
        optOperations.addActionListener(e -> startOperationsLayerAnimation());
        optionPanel.add(optOperations);
          optReachability = createOptionCheckBox(resourceMap.getString("map.overlay.reachability.text"));
          String reachabilityTooltip = resourceMap.getString("map.overlay.reachability.toolTipText");
          optReachability.setToolTipText(reachabilityTooltip);
          optReachability.getAccessibleContext().setAccessibleName(optReachability.getText());
          optReachability.getAccessibleContext().setAccessibleDescription(reachabilityTooltip);
          reachabilityHops = new ImmersiveSpinner(new SpinnerNumberModel(1, 1,
              NavigationRouteAnalysis.MAXIMUM_REACHABILITY_HOPS, 1));
          reachabilityHops.setEditor(new JSpinner.NumberEditor(reachabilityHops, "0"));
          Dimension hopSpinnerSize = new Dimension(UIUtil.scaleForGUI(64),
              reachabilityHops.getPreferredSize().height);
          reachabilityHops.setPreferredSize(hopSpinnerSize);
          reachabilityHops.setMinimumSize(hopSpinnerSize);
          reachabilityHops.setMaximumSize(hopSpinnerSize);
          String hopLabelText = resourceMap.getString("map.overlay.reachability.hops.text");
          String hopTooltip = resourceMap.getString("map.overlay.reachability.hops.toolTipText");
          reachabilityHops.setToolTipText(hopTooltip);
          reachabilityHops.getAccessibleContext().setAccessibleName(hopLabelText);
          reachabilityHops.getAccessibleContext().setAccessibleDescription(hopTooltip);
          reachabilityHops.setEnabled(false);
          reachabilityHops.addChangeListener(event -> {
            if (optReachability.isSelected()) {
                refreshReachability();
                repaint();
            }
          });
          JPanel reachabilityControl = new JPanel();
          reachabilityControl.setLayout(new BoxLayout(reachabilityControl, BoxLayout.X_AXIS));
          reachabilityControl.setOpaque(false);
          reachabilityControl.setAlignmentX(Component.LEFT_ALIGNMENT);
          reachabilityControl.setMaximumSize(new Dimension(Integer.MAX_VALUE,
              Math.max(optReachability.getPreferredSize().height, hopSpinnerSize.height)));
          reachabilityControl.add(optReachability);
          reachabilityControl.add(Box.createHorizontalGlue());
          JLabel hopLabel = new JLabel(hopLabelText);
          hopLabel.setForeground(LAYER_CONTROL_TEXT);
          hopLabel.setFont(hopLabel.getFont().deriveFont(Font.PLAIN, hopLabel.getFont().getSize2D() * 0.78f));
          hopLabel.setLabelFor(reachabilityHops);
          reachabilityControl.add(hopLabel);
          reachabilityControl.add(Box.createRigidArea(new Dimension(UIUtil.scaleForGUI(4), 0)));
          reachabilityControl.add(reachabilityHops);
          optionPanel.add(reachabilityControl);
          optReachability.addActionListener(event -> {
              reachabilityHops.setEnabled(optReachability.isSelected());
              refreshReachability();
              repaint();
          });
          optMeasureDistance = createOptionCheckBox(resourceMap.getString("map.overlay.measure.text"));
          String measurementTooltip = resourceMap.getString("map.overlay.measure.toolTipText");
          optMeasureDistance.setToolTipText(measurementTooltip);
          optMeasureDistance.getAccessibleContext().setAccessibleName(optMeasureDistance.getText());
          optMeasureDistance.getAccessibleContext().setAccessibleDescription(measurementTooltip);
          optMeasureDistance.addActionListener(event -> {
            measurementState = optMeasureDistance.isSelected()
                ? MeasurementState.active()
                : MeasurementState.inactive();
            measurementHoverSystem = null;
            cachedMeasurementAssessment = null;
            repaint();
          });
          optionPanel.add(optMeasureDistance);

        optionView = new JViewport();
        optionView.setOpaque(false);
          optionView.setView(optionPanel);

          optionControl = new JPanel(null);
          optionControl.setOpaque(true);
          optionControl.setBackground(LAYER_CONTROL_BACKGROUND);
          optionControl.setBorder(BorderFactory.createLineBorder(LAYER_CONTROL_BORDER,
              Math.max(1, UIUtil.scaleForGUI(1))));
          optionControl.add(optionView);
          optionControl.setVisible(!optionPanelHidden);

          pane.add(optionControl, Integer.valueOf(10));

        add(pane);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                updateAnimationVisibility();
                updateLayerDismissalListener();
                requestStaticCartographyPreparation();
            }
        });
    }

        static boolean shouldRenderSystem(boolean visibleInViewport, boolean systemEmpty,
                    boolean showEmptySystems, boolean requiredNavigationSystem) {
                return visibleInViewport && (!systemEmpty || showEmptySystems || requiredNavigationSystem);
        }

        static boolean canUseRetainedCartography(boolean territoryReady,
              boolean retainedCartographyAnimating) {
            return territoryReady && !retainedCartographyAnimating;
    }

          static boolean canUseRetainedMapModeTransition(boolean territoryReady,
              boolean mapModeAnimating, boolean otherRetainedLayerAnimating,
              boolean proposedRouteAnimating, boolean routeActivationAnimating) {
            return territoryReady && mapModeAnimating && !otherRetainedLayerAnimating
                && !proposedRouteAnimating && !routeActivationAnimating;
          }

          static boolean canUseMergedNavigation(boolean retainedCartography,
              boolean proposedRouteAnimating, boolean routeActivationAnimating, boolean scaleChanging) {
            return retainedCartography && !proposedRouteAnimating && !routeActivationAnimating
                && !scaleChanging;
          }

                    static boolean canUseRetainedSystemArt(boolean retainedCartography,
                            boolean retainedMapModeTransition, boolean supportingLayerAnimating,
                            boolean scaleChanging) {
                        return retainedMapModeTransition
                                    || ((retainedCartography || supportingLayerAnimating) && !scaleChanging);
        }

        private boolean hasActiveRetainedCartographyAnimation() {
            return territoryLayerAnimating || territoryLayerSettling
                  || hpgNetworkLayerAnimating || mapModeAnimating;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        updateAnimationVisibility();
        updateLayerDismissalListener();
        requestStaticCartographyPreparation();
    }

    @Override
    public void removeNotify() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(layerDismissalListener);
        layerDismissalListenerInstalled = false;
        cancelRenderBenchmark("map hidden");
        cancelTransitionBenchmark("map hidden");
        cancelZoomBenchmark("map hidden");
        zoomSettlingTimer.stop();
        zoomInteractionActive = false;
        travelAnimationTimer.stop();
        suspendSystemDiveAnimation();
        disposeMapLegendDialog();
        territoryPreparationQueue.cancel();
        clearRenderLayerCaches();
        super.removeNotify();
    }

    private void toggleRenderBenchmark() {
        if (transitionBenchmarkRun != null) {
            LOGGER.info("Map render benchmark not started: transition benchmark is running");
            return;
        }
        if (zoomBenchmarkRun != null) {
            LOGGER.info("Map render benchmark not started: zoom benchmark is running");
            return;
        }
        if (renderBenchmarkTimer.isRunning()) {
            cancelRenderBenchmark("cancelled by user");
            return;
        }
        if (!isShowing() || (getWidth() <= 0) || (getHeight() <= 0)) {
            LOGGER.info("Map render benchmark not started: map is not visible");
            return;
        }
        if (hasActiveRenderAnimation()) {
            LOGGER.info("Map render benchmark not started: wait for map animations to finish");
            return;
        }

        RenderBenchmarkCamera origin = new RenderBenchmarkCamera(conf.centerX, conf.centerY, conf.scale);
        renderBenchmarkRun = new RenderBenchmarkRun(origin, getWidth(), getHeight(), campaign.getLocalDate(),
              getSelectedMapMode().name(), optTerritory.isSelected(), optHPGNetwork.isSelected(),
              optOperations.isSelected(), optReachability.isSelected(), optEmptySystems.isSelected());
        renderBenchmarkMeasuring = false;
        renderBenchmarkPhaseStartedNanos = System.nanoTime();
        renderPerformanceTracker.reset(renderBenchmarkPhaseStartedNanos);
        renderBenchmarkTimer.start();
        LOGGER.info("Map render benchmark warmup started: path={} warmup={}s measurement={}s",
              RENDER_BENCHMARK_PATH_ID, RENDER_BENCHMARK_WARMUP_NS / 1_000_000_000L,
              RENDER_BENCHMARK_DURATION_NS / 1_000_000_000L);
    }

    private void updateRenderBenchmark() {
        RenderBenchmarkRun benchmarkRun = renderBenchmarkRun;
        if (benchmarkRun == null) {
            renderBenchmarkTimer.stop();
            return;
        }
        if ((getWidth() != benchmarkRun.width()) || (getHeight() != benchmarkRun.height())) {
            cancelRenderBenchmark("viewport changed");
            return;
        }
        if (hasActiveRenderAnimation()) {
            cancelRenderBenchmark("another map animation started");
            return;
        }

        long nowNanos = System.nanoTime();
        long phaseDuration = renderBenchmarkMeasuring
              ? RENDER_BENCHMARK_DURATION_NS
              : RENDER_BENCHMARK_WARMUP_NS;
        double progress = Math.min(1.0,
              (double) (nowNanos - renderBenchmarkPhaseStartedNanos) / phaseDuration);
        applyRenderBenchmarkCamera(renderBenchmarkCameraAt(benchmarkRun.origin(), progress));
        if (progress < 1.0) {
            return;
        }

        if (!renderBenchmarkMeasuring) {
            renderBenchmarkMeasuring = true;
            renderBenchmarkPhaseStartedNanos = nowNanos;
            renderPerformanceTracker.reset(nowNanos);
            LOGGER.info("Map render benchmark measurement started: path={}", RENDER_BENCHMARK_PATH_ID);
            return;
        }
        finishRenderBenchmark(nowNanos);
    }

    private boolean hasActiveRenderAnimation() {
        return layerAnimationTimer.isRunning() || selectionAnimationTimer.isRunning()
              || proposedRouteAnimationTimer.isRunning() || travelAnimationTimer.isRunning()
              || systemDiveAnimationTimer.isRunning();
    }

    private void applyRenderBenchmarkCamera(RenderBenchmarkCamera camera) {
        conf.centerX = camera.centerX();
        conf.centerY = camera.centerY();
        conf.scale = camera.scale();
        repaint();
    }

    private void finishRenderBenchmark(long nowNanos) {
        RenderBenchmarkRun benchmarkRun = renderBenchmarkRun;
        renderBenchmarkTimer.stop();
        if (benchmarkRun == null) {
            return;
        }

        long measuredMillis = (nowNanos - renderBenchmarkPhaseStartedNanos) / 1_000_000L;
        String report;
        if (renderPerformanceTracker.hasSamples()) {
            report = renderPerformanceTracker.reportAndReset(nowNanos);
        } else {
            renderPerformanceTracker.reset(nowNanos);
            report = "Map render: frames=0";
        }
        LOGGER.info("Map render benchmark result: path={} viewport={}x{} date={} mode={} "
                    + "layers[territory={} hpg={} operations={} reachability={} empty={}] "
                    + "center=({}, {}) scale={} measured={}ms {}",
              RENDER_BENCHMARK_PATH_ID, benchmarkRun.width(), benchmarkRun.height(), benchmarkRun.date(),
              benchmarkRun.mapMode(), benchmarkRun.territory(), benchmarkRun.hpgNetwork(),
              benchmarkRun.operations(), benchmarkRun.reachability(), benchmarkRun.emptySystems(),
              benchmarkRun.origin().centerX(), benchmarkRun.origin().centerY(), benchmarkRun.origin().scale(),
              measuredMillis, report);
        restoreRenderBenchmarkOrigin(benchmarkRun);
    }

    private void cancelRenderBenchmark(String reason) {
        RenderBenchmarkRun benchmarkRun = renderBenchmarkRun;
        if (benchmarkRun == null) {
            return;
        }
        renderBenchmarkTimer.stop();
        renderPerformanceTracker.reset(System.nanoTime());
        LOGGER.info("Map render benchmark cancelled: path={} reason={}", RENDER_BENCHMARK_PATH_ID, reason);
        restoreRenderBenchmarkOrigin(benchmarkRun);
    }

    private void restoreRenderBenchmarkOrigin(RenderBenchmarkRun benchmarkRun) {
        renderBenchmarkRun = null;
        renderBenchmarkMeasuring = false;
        applyRenderBenchmarkCamera(benchmarkRun.origin());
    }

    private void toggleTransitionBenchmark() {
        if (transitionBenchmarkRun != null) {
            cancelTransitionBenchmark("cancelled by user");
            return;
        }
        if (zoomBenchmarkRun != null) {
            LOGGER.info("Map transition benchmark not started: zoom benchmark is running");
            return;
        }
        if (renderBenchmarkTimer.isRunning()) {
            LOGGER.info("Map transition benchmark not started: warm-pan benchmark is running");
            return;
        }
        if (!isShowing() || (getWidth() <= 0) || (getHeight() <= 0)) {
            LOGGER.info("Map transition benchmark not started: map is not visible");
            return;
        }
        if (hasActiveRenderAnimation()) {
            LOGGER.info("Map transition benchmark not started: wait for map animations to finish");
            return;
        }

        MapMode originMode = getSelectedMapMode();
        MapMode targetMode = originMode == MapMode.FACTION ? MapMode.TECHNOLOGY : MapMode.FACTION;
        transitionBenchmarkRun = new TransitionBenchmarkRun(
              new RenderBenchmarkCamera(conf.centerX, conf.centerY, conf.scale),
              getWidth(), getHeight(), campaign.getLocalDate(), originMode, targetMode,
              optTerritory.isSelected(), optHPGNetwork.isSelected(), optOperations.isSelected(),
              optReachability.isSelected(), optEmptySystems.isSelected());
        transitionBenchmarkPhase = TransitionBenchmarkPhase.WARMUP;
        transitionBenchmarkPhaseStartedNanos = System.nanoTime();
        transitionBenchmarkExactFramePainted = false;
        renderPerformanceTracker.reset(transitionBenchmarkPhaseStartedNanos);
        transitionBenchmarkTimer.start();
        requestStaticCartographyPreparation();
        repaint();
        LOGGER.info("Map transition benchmark warmup started: path={}", TRANSITION_BENCHMARK_PATH_ID);
    }

    private void updateTransitionBenchmark() {
        TransitionBenchmarkRun benchmarkRun = transitionBenchmarkRun;
        if (benchmarkRun == null) {
            transitionBenchmarkTimer.stop();
            return;
        }
        if (!isTransitionBenchmarkConfigurationCurrent(benchmarkRun)) {
            cancelTransitionBenchmark("map configuration changed");
            return;
        }
        if (hasUnexpectedTransitionBenchmarkAnimation()) {
            cancelTransitionBenchmark("another map animation started");
            return;
        }
        long nowNanos = System.nanoTime();
        if ((nowNanos - transitionBenchmarkPhaseStartedNanos) > TRANSITION_BENCHMARK_TIMEOUT_NS) {
            cancelTransitionBenchmark(transitionBenchmarkPhase.name().toLowerCase(Locale.ROOT) + " timed out");
            return;
        }
        if (!transitionBenchmarkExactFramePainted) {
            return;
        }

        switch (transitionBenchmarkPhase) {
            case WARMUP -> startMeasuredMapModeTransition(benchmarkRun, nowNanos);
            case MODE_TRANSITION -> finishMapModeTransitionMeasurement(benchmarkRun, nowNanos);
            case CACHE_REGENERATION -> finishCacheRegenerationMeasurement(benchmarkRun, nowNanos);
            case RESTORE -> finishTransitionBenchmarkRestore(benchmarkRun, nowNanos);
        }
    }

    private boolean hasUnexpectedTransitionBenchmarkAnimation() {
        return optionPanelAnimating || territoryLayerAnimating || hpgNetworkLayerAnimating
              || operationsLayerAnimating || selectionAnimationTimer.isRunning()
              || proposedRouteAnimationTimer.isRunning() || travelAnimationTimer.isRunning()
              || systemDiveAnimationTimer.isRunning();
    }

    private boolean isTransitionBenchmarkConfigurationCurrent(TransitionBenchmarkRun benchmarkRun) {
        MapMode expectedMode = switch (transitionBenchmarkPhase) {
            case WARMUP, RESTORE -> benchmarkRun.originMode();
            case MODE_TRANSITION, CACHE_REGENERATION -> benchmarkRun.targetMode();
        };
        return (getWidth() == benchmarkRun.width()) && (getHeight() == benchmarkRun.height())
              && campaign.getLocalDate().equals(benchmarkRun.date())
              && benchmarkRun.origin().equals(
                  new RenderBenchmarkCamera(conf.centerX, conf.centerY, conf.scale))
              && (getSelectedMapMode() == expectedMode)
              && (optTerritory.isSelected() == benchmarkRun.territory())
              && (optHPGNetwork.isSelected() == benchmarkRun.hpgNetwork())
              && (optOperations.isSelected() == benchmarkRun.operations())
              && (optReachability.isSelected() == benchmarkRun.reachability())
              && (optEmptySystems.isSelected() == benchmarkRun.emptySystems());
    }

    private void startMeasuredMapModeTransition(TransitionBenchmarkRun benchmarkRun, long nowNanos) {
        transitionBenchmarkPhase = TransitionBenchmarkPhase.MODE_TRANSITION;
        transitionBenchmarkPhaseStartedNanos = nowNanos;
        transitionBenchmarkExactFramePainted = false;
        renderPerformanceTracker.reset(nowNanos);
        selectMapMode(benchmarkRun.targetMode());
        startMapModeAnimation(benchmarkRun.targetMode());
        LOGGER.info("Map transition benchmark measurement started: path={} phase=mode-transition from={} to={}",
              TRANSITION_BENCHMARK_PATH_ID, benchmarkRun.originMode(), benchmarkRun.targetMode());
    }

    private void finishMapModeTransitionMeasurement(TransitionBenchmarkRun benchmarkRun, long nowNanos) {
        logTransitionBenchmarkResult(benchmarkRun, "mode-transition", benchmarkRun.targetMode(), nowNanos);
        finishSettledMapModeTransitionFrame();
        transitionBenchmarkPhase = TransitionBenchmarkPhase.CACHE_REGENERATION;
        transitionBenchmarkPhaseStartedNanos = nowNanos;
        transitionBenchmarkExactFramePainted = false;
        renderPerformanceTracker.reset(nowNanos);
        clearRenderLayerCaches();
        repaint();
        LOGGER.info("Map transition benchmark measurement started: path={} phase=cache-regeneration mode={}",
              TRANSITION_BENCHMARK_PATH_ID, benchmarkRun.targetMode());
    }

    private void finishCacheRegenerationMeasurement(TransitionBenchmarkRun benchmarkRun, long nowNanos) {
        logTransitionBenchmarkResult(benchmarkRun, "cache-regeneration", benchmarkRun.targetMode(), nowNanos);
        transitionBenchmarkPhase = TransitionBenchmarkPhase.RESTORE;
        transitionBenchmarkPhaseStartedNanos = nowNanos;
        transitionBenchmarkExactFramePainted = false;
        renderPerformanceTracker.reset(nowNanos);
        selectMapMode(benchmarkRun.originMode());
        startMapModeAnimation(benchmarkRun.originMode());
    }

    private void finishTransitionBenchmarkRestore(TransitionBenchmarkRun benchmarkRun, long nowNanos) {
        transitionBenchmarkTimer.stop();
        finishSettledMapModeTransitionFrame();
        transitionBenchmarkRun = null;
        transitionBenchmarkPhase = null;
        transitionBenchmarkExactFramePainted = false;
        renderPerformanceTracker.reset(nowNanos);
        LOGGER.info("Map transition benchmark completed: path={} restoredMode={}",
              TRANSITION_BENCHMARK_PATH_ID, benchmarkRun.originMode());
    }

    private void logTransitionBenchmarkResult(TransitionBenchmarkRun benchmarkRun, String phase,
          MapMode mode, long nowNanos) {
        String report = renderPerformanceTracker.hasSamples()
              ? renderPerformanceTracker.reportAndReset(nowNanos)
              : "Map render: frames=0";
        long elapsedMillis = (nowNanos - transitionBenchmarkPhaseStartedNanos) / 1_000_000L;
        LOGGER.info("Map transition benchmark result: path={} phase={} viewport={}x{} date={} "
                    + "from={} to={} mode={} layers[territory={} hpg={} operations={} reachability={} empty={}] "
                    + "center=({}, {}) scale={} elapsed={}ms {}",
              TRANSITION_BENCHMARK_PATH_ID, phase, benchmarkRun.width(), benchmarkRun.height(),
              benchmarkRun.date(), benchmarkRun.originMode(), benchmarkRun.targetMode(), mode,
              benchmarkRun.territory(), benchmarkRun.hpgNetwork(), benchmarkRun.operations(),
              benchmarkRun.reachability(), benchmarkRun.emptySystems(), benchmarkRun.origin().centerX(),
              benchmarkRun.origin().centerY(), benchmarkRun.origin().scale(), elapsedMillis, report);
    }

    private void cancelTransitionBenchmark(String reason) {
        TransitionBenchmarkRun benchmarkRun = transitionBenchmarkRun;
        if (benchmarkRun == null) {
            return;
        }
        transitionBenchmarkTimer.stop();
        transitionBenchmarkRun = null;
        transitionBenchmarkPhase = null;
        transitionBenchmarkExactFramePainted = false;
        selectMapMode(benchmarkRun.originMode());
        previousMapMode = benchmarkRun.originMode();
        targetMapMode = benchmarkRun.originMode();
        mapModeAnimationProgress = 1.0;
        mapModeAnimating = false;
        mapModeTransitionSettling = false;
        mapModeTransitionCacheStage = MapModeTransitionCacheStage.INACTIVE;
        clearMapModeTransitionRenderCaches();
        if (!hasActiveLayerAnimation()) {
            layerAnimationTimer.stop();
        }
        renderPerformanceTracker.reset(System.nanoTime());
        repaint();
        LOGGER.info("Map transition benchmark cancelled: path={} reason={}",
              TRANSITION_BENCHMARK_PATH_ID, reason);
    }

    private void toggleZoomBenchmark() {
        if (zoomBenchmarkRun != null) {
            cancelZoomBenchmark("cancelled by user");
            return;
        }
        if (renderBenchmarkTimer.isRunning()) {
            LOGGER.info("Map zoom benchmark not started: warm-pan benchmark is running");
            return;
        }
        if (transitionBenchmarkRun != null) {
            LOGGER.info("Map zoom benchmark not started: transition benchmark is running");
            return;
        }
        if (!isShowing() || (getWidth() <= 0) || (getHeight() <= 0)) {
            LOGGER.info("Map zoom benchmark not started: map is not visible");
            return;
        }
        if (hasActiveRenderAnimation()) {
            LOGGER.info("Map zoom benchmark not started: wait for map animations to finish");
            return;
        }

        RenderBenchmarkCamera origin = new RenderBenchmarkCamera(conf.centerX, conf.centerY, conf.scale);
        if (!isValidZoomBenchmarkOrigin(origin)) {
            LOGGER.info("Map zoom benchmark not started: invalid camera center=({}, {}) scale={}",
                  origin.centerX(), origin.centerY(), origin.scale());
            return;
        }
        double semanticReference = getSemanticZoomReference(conf.showPlanetNamesThreshold);
        ZoomBenchmarkRange range = zoomBenchmarkRange(semanticReference);
        int anchorX = getWidth() * 2 / 3;
        int anchorY = getHeight() / 3;
        zoomBenchmarkRun = new ZoomBenchmarkRun(origin, getWidth(), getHeight(), campaign.getLocalDate(),
              getSelectedMapMode(), optTerritory.isSelected(), optHPGNetwork.isSelected(),
              optOperations.isSelected(), optReachability.isSelected(), optEmptySystems.isSelected(),
              anchorX, anchorY, semanticReference, range);
        zoomBenchmarkPhase = ZoomBenchmarkPhase.WARMUP;
        zoomBenchmarkPhaseStartedNanos = System.nanoTime();
        zoomBenchmarkExpectedCamera = origin;
        zoomBenchmarkCameraPainted = false;
        zoomBenchmarkExactFramePainted = false;
        zoomBenchmarkPathComplete = false;
        renderPerformanceTracker.reset(zoomBenchmarkPhaseStartedNanos);
        zoomBenchmarkTimer.start();
        requestStaticCartographyPreparation();
        repaint();
        LOGGER.info("Map zoom benchmark warmup started: path={} atlasScale={} detailScale={} duration={}s",
              ZOOM_BENCHMARK_PATH_ID, range.atlasScale(), range.detailScale(),
              ZOOM_BENCHMARK_DURATION_NS / 1_000_000_000L);
    }

    private void updateZoomBenchmark() {
        ZoomBenchmarkRun benchmarkRun = zoomBenchmarkRun;
        if (benchmarkRun == null) {
            zoomBenchmarkTimer.stop();
            return;
        }
        if (!isZoomBenchmarkConfigurationCurrent(benchmarkRun)) {
            cancelZoomBenchmark("map configuration changed");
            return;
        }
        if (hasActiveRenderAnimation()) {
            cancelZoomBenchmark("another map animation started");
            return;
        }

        long nowNanos = System.nanoTime();
        switch (zoomBenchmarkPhase) {
            case WARMUP -> updateZoomBenchmarkWarmup(nowNanos);
            case ACTIVE_ZOOM -> updateActiveZoomBenchmark(benchmarkRun, nowNanos);
            case SETTLED_REGENERATION -> updateSettledZoomBenchmark(benchmarkRun, nowNanos);
        }
    }

    private void updateZoomBenchmarkWarmup(long nowNanos) {
        if ((nowNanos - zoomBenchmarkPhaseStartedNanos) > ZOOM_BENCHMARK_TIMEOUT_NS) {
            cancelZoomBenchmark("warmup timed out");
            return;
        }
        if (!zoomBenchmarkExactFramePainted) {
            return;
        }

        zoomBenchmarkPhase = ZoomBenchmarkPhase.ACTIVE_ZOOM;
        zoomBenchmarkPhaseStartedNanos = nowNanos;
        zoomBenchmarkCameraPainted = false;
        zoomBenchmarkExactFramePainted = false;
        zoomBenchmarkPathComplete = false;
        renderPerformanceTracker.reset(nowNanos);
        LOGGER.info("Map zoom benchmark measurement started: path={} phase=active-zoom",
              ZOOM_BENCHMARK_PATH_ID);
    }

    private void updateActiveZoomBenchmark(ZoomBenchmarkRun benchmarkRun, long nowNanos) {
        if ((nowNanos - zoomBenchmarkPhaseStartedNanos)
              > (ZOOM_BENCHMARK_DURATION_NS + ZOOM_BENCHMARK_TIMEOUT_NS)) {
            cancelZoomBenchmark("active zoom timed out");
            return;
        }
        if (zoomBenchmarkPathComplete) {
            if (!zoomBenchmarkCameraPainted) {
                return;
            }
            finishActiveZoomMeasurement(benchmarkRun, nowNanos);
            return;
        }

        double progress = Math.min(1.0,
              (double) (nowNanos - zoomBenchmarkPhaseStartedNanos) / ZOOM_BENCHMARK_DURATION_NS);
          RenderBenchmarkCamera camera = zoomBenchmarkCameraAt(benchmarkRun.origin(), benchmarkRun.width(),
              benchmarkRun.height(), benchmarkRun.anchorX(), benchmarkRun.anchorY(), benchmarkRun.range(),
              progress);
          if (!camera.equals(zoomBenchmarkExpectedCamera)) {
            applyZoomBenchmarkCamera(camera);
          }
        zoomBenchmarkPathComplete = progress >= 1.0;
    }

    private void finishActiveZoomMeasurement(ZoomBenchmarkRun benchmarkRun, long nowNanos) {
        logZoomBenchmarkResult(benchmarkRun, "active-zoom", nowNanos);
        zoomBenchmarkPhase = ZoomBenchmarkPhase.SETTLED_REGENERATION;
        zoomBenchmarkPhaseStartedNanos = nowNanos;
        zoomBenchmarkExactFramePainted = false;
        zoomBenchmarkPathComplete = false;
        renderPerformanceTracker.reset(nowNanos);
        clearRenderLayerCaches();
        applyZoomBenchmarkCamera(benchmarkRun.origin());
        requestStaticCartographyPreparation();
        LOGGER.info("Map zoom benchmark measurement started: path={} phase=settled-regeneration",
              ZOOM_BENCHMARK_PATH_ID);
    }

    private void updateSettledZoomBenchmark(ZoomBenchmarkRun benchmarkRun, long nowNanos) {
        if ((nowNanos - zoomBenchmarkPhaseStartedNanos) > ZOOM_BENCHMARK_TIMEOUT_NS) {
            cancelZoomBenchmark("settled regeneration timed out");
            return;
        }
        if (!zoomBenchmarkExactFramePainted) {
            return;
        }

        logZoomBenchmarkResult(benchmarkRun, "settled-regeneration", nowNanos);
        zoomBenchmarkTimer.stop();
        zoomBenchmarkRun = null;
        zoomBenchmarkPhase = null;
        zoomBenchmarkExpectedCamera = null;
        zoomBenchmarkCameraPainted = false;
        zoomBenchmarkExactFramePainted = false;
        renderPerformanceTracker.reset(nowNanos);
        LOGGER.info("Map zoom benchmark completed: path={} restoredScale={}",
              ZOOM_BENCHMARK_PATH_ID, benchmarkRun.origin().scale());
    }

    private boolean isZoomBenchmarkConfigurationCurrent(ZoomBenchmarkRun benchmarkRun) {
        return (getWidth() == benchmarkRun.width()) && (getHeight() == benchmarkRun.height())
              && campaign.getLocalDate().equals(benchmarkRun.date())
              && (getSelectedMapMode() == benchmarkRun.mapMode())
              && (optTerritory.isSelected() == benchmarkRun.territory())
              && (optHPGNetwork.isSelected() == benchmarkRun.hpgNetwork())
              && (optOperations.isSelected() == benchmarkRun.operations())
              && (optReachability.isSelected() == benchmarkRun.reachability())
              && (optEmptySystems.isSelected() == benchmarkRun.emptySystems())
              && (Double.doubleToLongBits(getSemanticZoomReference(conf.showPlanetNamesThreshold))
                    == Double.doubleToLongBits(benchmarkRun.semanticReference()))
              && new RenderBenchmarkCamera(conf.centerX, conf.centerY, conf.scale)
                    .equals(zoomBenchmarkExpectedCamera);
    }

    private void applyZoomBenchmarkCamera(RenderBenchmarkCamera camera) {
        zoomBenchmarkExpectedCamera = camera;
        zoomBenchmarkCameraPainted = false;
        applyRenderBenchmarkCamera(camera);
    }

    private void logZoomBenchmarkResult(ZoomBenchmarkRun benchmarkRun, String phase, long nowNanos) {
        String report = renderPerformanceTracker.hasSamples()
              ? renderPerformanceTracker.reportAndReset(nowNanos)
              : "Map render: frames=0";
        long elapsedMillis = (nowNanos - zoomBenchmarkPhaseStartedNanos) / 1_000_000L;
        LOGGER.info("Map zoom benchmark result: path={} phase={} viewport={}x{} date={} mode={} "
                    + "layers[territory={} hpg={} operations={} reachability={} empty={}] "
                    + "center=({}, {}) scale={} anchor=({}, {}) range=({}, {}) elapsed={}ms {}",
              ZOOM_BENCHMARK_PATH_ID, phase, benchmarkRun.width(), benchmarkRun.height(), benchmarkRun.date(),
              benchmarkRun.mapMode(), benchmarkRun.territory(), benchmarkRun.hpgNetwork(),
              benchmarkRun.operations(), benchmarkRun.reachability(), benchmarkRun.emptySystems(),
              benchmarkRun.origin().centerX(), benchmarkRun.origin().centerY(), benchmarkRun.origin().scale(),
              benchmarkRun.anchorX(), benchmarkRun.anchorY(), benchmarkRun.range().atlasScale(),
              benchmarkRun.range().detailScale(), elapsedMillis, report);
    }

    private void cancelZoomBenchmark(String reason) {
        ZoomBenchmarkRun benchmarkRun = zoomBenchmarkRun;
        if (benchmarkRun == null) {
            return;
        }
        zoomBenchmarkTimer.stop();
        zoomBenchmarkRun = null;
        zoomBenchmarkPhase = null;
        zoomBenchmarkExpectedCamera = null;
        zoomBenchmarkCameraPainted = false;
        zoomBenchmarkExactFramePainted = false;
        zoomBenchmarkPathComplete = false;
        renderPerformanceTracker.reset(System.nanoTime());
        applyRenderBenchmarkCamera(benchmarkRun.origin());
        LOGGER.info("Map zoom benchmark cancelled: path={} reason={}", ZOOM_BENCHMARK_PATH_ID, reason);
    }

    private boolean isZoomInteractionActive() {
        return zoomInteractionActive || ((zoomBenchmarkRun != null)
              && (zoomBenchmarkPhase == ZoomBenchmarkPhase.ACTIVE_ZOOM));
    }

    private void finishZoomInteraction() {
        zoomSettlingTimer.stop();
        if (!zoomInteractionActive) {
            return;
        }
        zoomInteractionActive = false;
        retainedSystemArtRenderCache.clear();
        retainedNavigationRenderCache.clear();
        repaint();
    }

    private void updateAnimationVisibility() {
        if (isShowing()) {
            settleTravelVisualState();
        } else {
            travelAnimationTimer.stop();
        }
    }

    private void settleTravelVisualState() {
        travelAnimationTimer.stop();
        observedCurrentSystemId = getCurrentSystemId();
        observedActiveRouteSystemIds = getPathSystemIds(getActiveJumpPath());
        observedActiveRouteExists = !observedActiveRouteSystemIds.isEmpty();
        cachedProposedRouteSystemIds = List.of();
        activatingRouteSystemIds = List.of();
        routeActivationProgress = 1.0;
        systemHopOriginId = null;
        systemHopDestinationId = null;
        systemHopProgress = 1.0;
        travelVisualStateInitialized = true;
    }

    private void refreshTravelVisualState() {
        if (campaign == null) {
            return;
        }
        if (!travelVisualStateInitialized) {
            settleTravelVisualState();
            return;
        }

        String currentSystemId = getCurrentSystemId();
        List<String> activeRouteSystemIds = getPathSystemIds(getActiveJumpPath());
        boolean activeRouteExists = !activeRouteSystemIds.isEmpty();
        if (isRouteActivationAnimating() && !activatingRouteSystemIds.equals(activeRouteSystemIds)) {
            finishRouteActivation();
        }
        if (activeRouteExists && !observedActiveRouteExists) {
            if ((activeRouteSystemIds.size() > 1) && activeRouteSystemIds.equals(cachedProposedRouteSystemIds)) {
                startRouteActivation(activeRouteSystemIds);
            }
            cachedProposedRouteSystemIds = List.of();
        }
        if (!java.util.Objects.equals(currentSystemId, observedCurrentSystemId)
              && (activeRouteExists || observedActiveRouteExists)) {
            startSystemHop(observedCurrentSystemId, currentSystemId);
        }
        observedCurrentSystemId = currentSystemId;
        observedActiveRouteSystemIds = activeRouteSystemIds;
        observedActiveRouteExists = activeRouteExists;
    }

    private void startRouteActivation(List<String> routeSystemIds) {
        activatingRouteSystemIds = List.copyOf(routeSystemIds);
        routeActivationStartTime = System.nanoTime();
        routeActivationProgress = 0.0;
        startTravelAnimationTimerIfVisible();
    }

    private void finishRouteActivation() {
        activatingRouteSystemIds = List.of();
        routeActivationProgress = 1.0;
    }

    private void startSystemHop(@Nullable String originId, @Nullable String destinationId) {
        if ((originId == null) || (destinationId == null) || originId.equals(destinationId)
              || (campaign.getSystemById(originId) == null) || (campaign.getSystemById(destinationId) == null)) {
            return;
        }
        systemHopOriginId = originId;
        systemHopDestinationId = destinationId;
        systemHopStartTime = System.nanoTime();
        systemHopProgress = 0.0;
        startTravelAnimationTimerIfVisible();
    }

    private void startTravelAnimationTimerIfVisible() {
        if (isShowing() && !travelAnimationTimer.isRunning()) {
            travelAnimationTimer.start();
        }
    }

    private void updateTravelAnimations() {
        if (!isShowing()) {
            travelAnimationTimer.stop();
            return;
        }

        long nowNanos = System.nanoTime();
        if (isRouteActivationAnimating()) {
            routeActivationProgress = Math.min(1.0,
                  (double) (nowNanos - routeActivationStartTime) / ROUTE_ACTIVATION_DURATION_NS);
            if (routeActivationProgress >= 1.0) {
                finishRouteActivation();
            }
        }
        if (isSystemHopAnimating()) {
            systemHopProgress = Math.min(1.0,
                  (double) (nowNanos - systemHopStartTime) / SYSTEM_HOP_DURATION_NS);
            if (systemHopProgress >= 1.0) {
                systemHopOriginId = null;
                systemHopDestinationId = null;
            }
        }
        if (!isRouteActivationAnimating() && !isSystemHopAnimating()) {
            travelAnimationTimer.stop();
        }
        mapPanel.repaint();
    }

    private boolean isRouteActivationAnimating() {
        return (routeActivationProgress < 1.0) && (activatingRouteSystemIds.size() > 1);
    }

    private boolean isSystemHopAnimating() {
        return (systemHopProgress < 1.0) && (systemHopOriginId != null) && (systemHopDestinationId != null);
    }

    private @Nullable JumpPath getActiveJumpPath() {
        if (campaign == null) {
            return null;
        }
        return campaign.getPlayerForce().getForceDetachment().getCurrentLocation().getJumpPath();
    }

    private @Nullable String getCurrentSystemId() {
        PlanetarySystem currentSystem = campaign == null ? null : campaign.getCurrentSystem();
        return currentSystem == null ? null : currentSystem.getId();
    }

    static JTabbedPane createMapLegendTabbedPane() {
        int contentWidth = UIUtil.scaleForGUI(MAP_LEGEND_CONTENT_WIDTH);
        List<MapLegendSection> orderedSections = MAP_LEGEND_SECTIONS.stream()
              .sorted(Comparator.comparingInt(section -> switch (section.heading()) {
                  case "NAVIGATION" -> 0;
                  case "ROUTES" -> 1;
                  case "LAYERS" -> 2;
                  case "OVERLAYS" -> 3;
                  case "RANGE RINGS" -> 4;
                  case "SYSTEM STATUS" -> 5;
                  default -> Integer.MAX_VALUE;
              }))
              .toList();
        List<JPanel> sections = orderedSections.stream()
              .map(section -> createMapLegendSection(section, contentWidth))
              .toList();
        int viewportHeight = Math.min(sections.stream()
              .mapToInt(section -> section.getPreferredSize().height)
              .max()
              .orElse(1), UIUtil.scaleForGUI(MAP_LEGEND_MAX_VIEWPORT_HEIGHT));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setOpaque(true);
        tabbedPane.setBackground(MAP_LEGEND_BACKGROUND);
        tabbedPane.setForeground(LAYER_CONTROL_TEXT);
        tabbedPane.setFocusable(true);
        tabbedPane.getAccessibleContext().setAccessibleName("Map symbol legend");
        tabbedPane.getAccessibleContext().setAccessibleDescription(
              "Legend for symbols shown on the interstellar map");
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            MapLegendSection section = orderedSections.get(sectionIndex);
            JScrollPane scrollPane = createMapLegendSectionScrollPane(sections.get(sectionIndex), contentWidth,
                  viewportHeight);
            tabbedPane.addTab(section.heading(), scrollPane);
            tabbedPane.setBackgroundAt(sectionIndex, MAP_LEGEND_BACKGROUND);
            tabbedPane.setForegroundAt(sectionIndex, LAYER_CONTROL_TEXT);
        }
        return tabbedPane;
    }

    private static JScrollPane createMapLegendSectionScrollPane(JPanel section, int viewportWidth,
          int viewportHeight) {
        JScrollPane scrollPane = new JScrollPane(section, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(MAP_LEGEND_BACKGROUND);
        scrollPane.setFocusable(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(MAP_LEGEND_BACKGROUND);
        Dimension viewportSize = new Dimension(viewportWidth, viewportHeight);
        scrollPane.getViewport().setPreferredSize(viewportSize);
        scrollPane.setPreferredSize(viewportSize);
          ImmersiveScrollBarStyle.apply(scrollPane.getVerticalScrollBar(), MAP_LEGEND_BACKGROUND,
              MAP_LEGEND_DIVIDER, MAP_LEGEND_SCROLLBAR_THUMB, LAYER_CONTROL_BUTTON_ICON,
              UIUtil.scaleForGUI(MAP_LEGEND_SCROLLBAR_WIDTH));
        scrollPane.getAccessibleContext().setAccessibleName(section.getAccessibleContext().getAccessibleName());
        scrollPane.getAccessibleContext().setAccessibleDescription(
              "Map symbol legend section " + section.getAccessibleContext().getAccessibleName());
        return scrollPane;
    }

    private static JPanel createMapLegendSection(MapLegendSection section, int sectionWidth) {
        int horizontalPadding = UIUtil.scaleForGUI(8);
        int rowWidth = sectionWidth - (horizontalPadding * 2);
        JPanel sectionPanel = new JPanel();
        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
        sectionPanel.setOpaque(true);
        sectionPanel.setBackground(MAP_LEGEND_BACKGROUND);
        sectionPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8), horizontalPadding,
              UIUtil.scaleForGUI(8), horizontalPadding));
        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionPanel.getAccessibleContext().setAccessibleName(section.heading());
        sectionPanel.putClientProperty("mapLegendSection", section.heading());
        for (MapLegendEntry entry : section.entries()) {
            sectionPanel.add(createMapLegendRow(entry, rowWidth));
        }

        Dimension preferredSize = sectionPanel.getPreferredSize();
        sectionPanel.setPreferredSize(new Dimension(sectionWidth, preferredSize.height));
        sectionPanel.setMaximumSize(new Dimension(sectionWidth, preferredSize.height));
        return sectionPanel;
    }

    private static JPanel createMapLegendRow(MapLegendEntry entry, int rowWidth) {
        int verticalPadding = UIUtil.scaleForGUI(6);
        int swatchGap = UIUtil.scaleForGUI(10);
        int swatchWidth = UIUtil.scaleForGUI(MAP_LEGEND_SWATCH_WIDTH);
        int textWidth = rowWidth - swatchWidth - swatchGap;
        MapLegendSwatch swatch = new MapLegendSwatch(entry);

        JLabel title = new JLabel(entry.title());
        title.setForeground(LAYER_CONTROL_TEXT);
        Font baseFont = ObjectUtility.nonNull(UIManager.getFont("Label.font"), title.getFont());
        title.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D()));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea meaning = new JTextArea(entry.meaning());
        meaning.setEditable(false);
        meaning.setFocusable(false);
        meaning.setOpaque(false);
        meaning.setForeground(MAP_LEGEND_MUTED_TEXT);
        meaning.setFont(baseFont.deriveFont(Math.max(10.0f, baseFont.getSize2D() - 1.0f)));
        meaning.setLineWrap(true);
        meaning.setWrapStyleWord(true);
        meaning.setBorder(null);
        meaning.setAlignmentX(Component.LEFT_ALIGNMENT);
        meaning.setSize(textWidth, Short.MAX_VALUE);
        int meaningHeight = meaning.getPreferredSize().height;
        Dimension meaningSize = new Dimension(textWidth, meaningHeight);
        meaning.setPreferredSize(meaningSize);
        meaning.setMinimumSize(meaningSize);
        meaning.setMaximumSize(meaningSize);
        meaning.putClientProperty("mapLegendTitle", entry.title());

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(Box.createRigidArea(new Dimension(0, UIUtil.scaleForGUI(2))));
        textPanel.add(meaning);
        int textHeight = title.getPreferredSize().height + UIUtil.scaleForGUI(2) + meaningHeight;
        Dimension textSize = new Dimension(textWidth, textHeight);
        textPanel.setPreferredSize(textSize);
                textPanel.setMinimumSize(textSize);
        textPanel.setMaximumSize(textSize);
                textPanel.putClientProperty("mapLegendTextCell", Boolean.TRUE);
                textPanel.putClientProperty("mapLegendTitle", entry.title());

                JPanel swatchCell = new JPanel(new GridBagLayout());
                swatchCell.setOpaque(false);
                swatchCell.setPreferredSize(swatch.getPreferredSize());
                swatchCell.setMinimumSize(swatch.getPreferredSize());
                swatchCell.add(swatch);
                swatchCell.putClientProperty("mapLegendSwatchCell", Boolean.TRUE);
                swatchCell.putClientProperty("mapLegendTitle", entry.title());

                JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
                GridBagConstraints swatchConstraints = new GridBagConstraints();
                swatchConstraints.gridx = 0;
                swatchConstraints.gridy = 0;
                swatchConstraints.weighty = 1.0;
                swatchConstraints.fill = GridBagConstraints.VERTICAL;
                swatchConstraints.anchor = GridBagConstraints.CENTER;
                swatchConstraints.insets = new Insets(0, 0, 0, swatchGap);
                row.add(swatchCell, swatchConstraints);

                GridBagConstraints textConstraints = new GridBagConstraints();
                textConstraints.gridx = 1;
                textConstraints.gridy = 0;
                textConstraints.weightx = 1.0;
                textConstraints.weighty = 1.0;
                textConstraints.fill = GridBagConstraints.HORIZONTAL;
                textConstraints.anchor = GridBagConstraints.CENTER;
                row.add(textPanel, textConstraints);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, 1, 0, MAP_LEGEND_DIVIDER),
              BorderFactory.createEmptyBorder(verticalPadding, 0, verticalPadding, 0)));
        int rowHeight = Math.max(swatch.getPreferredSize().height, textHeight) + (verticalPadding * 2) + 1;
        Dimension rowSize = new Dimension(rowWidth, rowHeight);
        row.setPreferredSize(rowSize);
        row.setMinimumSize(rowSize);
        row.setMaximumSize(rowSize);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.putClientProperty("mapLegendRow", Boolean.TRUE);
        row.putClientProperty("mapLegendTitle", entry.title());
        row.getAccessibleContext().setAccessibleName(entry.title());
        row.getAccessibleContext().setAccessibleDescription(entry.meaning());
        return row;
    }

    private static void paintMapLegendSymbol(Graphics2D graphics, MapLegendSymbol symbol) {
        switch (symbol) {
            case FACTION_OWNERSHIP -> paintLegendFactionOwnership(graphics);
            case LAYER_TECHNOLOGY -> paintLegendCategoricalLayer(graphics, List.of(
                new Color(51, 51, 51), new Color(68, 1, 84), new Color(59, 82, 139),
                new Color(33, 144, 140), new Color(93, 200, 99), new Color(253, 231, 37)));
            case LAYER_INDUSTRY -> paintLegendCategoricalLayer(graphics, List.of(
                new Color(0, 0, 4), new Color(81, 18, 124), new Color(182, 54, 121),
                new Color(251, 136, 97), new Color(252, 253, 191)));
            case LAYER_RAW_MATERIALS -> paintLegendCategoricalLayer(graphics, List.of(
                new Color(13, 8, 135), new Color(126, 3, 168), new Color(204, 70, 120),
                new Color(248, 148, 65), new Color(240, 249, 33)));
            case LAYER_OUTPUT -> paintLegendCategoricalLayer(graphics, List.of(
                new Color(0, 0, 4), new Color(86, 15, 110), new Color(187, 55, 84),
                new Color(249, 140, 10), new Color(252, 255, 164)));
            case LAYER_AGRICULTURE -> paintLegendCategoricalLayer(graphics, List.of(
                new Color(0, 32, 77), new Color(66, 77, 107), new Color(124, 123, 120),
                new Color(188, 175, 111), new Color(255, 234, 70)));
            case LAYER_POPULATION -> paintLegendCategoricalLayer(graphics, List.of(
                new Color(68, 1, 84), new Color(72, 40, 120), new Color(62, 74, 137),
                new Color(49, 104, 142), new Color(38, 130, 142), new Color(31, 158, 137),
                new Color(53, 183, 121), new Color(109, 205, 89), new Color(180, 222, 44),
                new Color(253, 231, 37)));
            case LAYER_HPG -> paintLegendCategoricalLayer(graphics, List.of(Color.BLACK,
                new Color(84, 84, 84), new Color(168, 168, 168), new Color(222, 73, 104),
                new Color(252, 253, 191)));
            case HPG_STATIONS -> paintLegendHpgStations(graphics);
            case LAYER_RECHARGE_STATIONS -> paintLegendCategoricalLayer(graphics, List.of(
                new Color(128, 128, 128), new Color(225, 100, 98), new Color(240, 249, 33)));
            case LAYER_ACADEMIES -> paintLegendCategoricalLayer(graphics, List.of(Color.BLACK,
                new Color(38, 130, 142), new Color(31, 158, 137), new Color(53, 183, 121),
                new Color(109, 205, 89), new Color(180, 222, 44), new Color(253, 231, 37)));
            case LAYER_HIRING_HALLS -> paintLegendCategoricalLayer(graphics, List.of(Color.BLACK,
                new Color(187, 55, 84), new Color(249, 140, 10), new Color(253, 231, 37),
                new Color(93, 200, 99)));
            case LAYER_DISEASE_OUTBREAKS -> paintLegendCategoricalLayer(graphics, List.of(Color.BLACK,
                new Color(253, 231, 37), new Color(249, 140, 10), new Color(187, 55, 84),
                new Color(126, 3, 168)));
            case FACTION_EMBLEM -> paintLegendFactionEmblem(graphics);
            case SELECTED_SYSTEM -> paintLegendSelectedSystem(graphics);
            case HOVERED_SYSTEM -> paintLegendHoveredSystem(graphics);
            case CURRENT_FLEET -> paintLegendCurrentFleet(graphics);
            case PLAYER_BASE -> drawPlayerBaseGlyph(graphics, new Point2D.Double(32, 19), 8.0, 2, 1.0);
            case PLANNED_ROUTE -> paintLegendPlannedRoute(graphics);
            case ACTIVE_ROUTE -> paintLegendActiveRoute(graphics);
            case WAYPOINT_BADGE -> paintLegendWaypointBadge(graphics);
            case REACHABILITY -> paintLegendReachability(graphics);
            case REACHABILITY_CAUTION -> paintLegendReachabilityCaution(graphics);
            case REACHABILITY_BLOCKED -> paintLegendReachabilityBlocked(graphics);
            case ROUTE_CAUTION -> paintLegendRouteCaution(graphics);
            case ROUTE_BLOCKED -> paintLegendRouteBlocked(graphics);
            case MEASUREMENT -> paintLegendMeasurement(graphics);
            case CONTRACT_SEARCH_RADIUS -> paintLegendRangeRing(graphics,
                MekHQ.getMHQOptions().getInterstellarMapContractSearchRadiusColour(),
                CONTRACT_SEARCH_RANGE_RING_STROKE);
            case PLANETARY_ACQUISITION_RADIUS -> paintLegendRangeRing(graphics,
                MekHQ.getMHQOptions().getInterstellarMapPlanetaryAcquisitionRadiusColour(),
                PLANETARY_ACQUISITION_RANGE_RING_STROKE);
            case JUMP_RADIUS -> paintLegendSolidRangeRing(graphics,
                MekHQ.getMHQOptions().getInterstellarMapJumpRadiusColour());
            case HPG_RANGE -> paintLegendRangeRing(graphics, HPG_RANGE_RING_COLOR, HPG_RANGE_RING_STROKE);
            case CAPITAL_HIERARCHY -> paintLegendCapitalHierarchy(graphics);
            case OPERATION -> paintLegendOperation(graphics);
            case RESTRICTED_SYSTEM -> paintLegendRestrictedSystem(graphics);
            case GM_EDITED_SYSTEM -> paintLegendGmEditedSystem(graphics);
            case HPG_NETWORK -> paintLegendHpgNetwork(graphics);
            case SOVEREIGN_TERRITORY -> paintLegendTerritorySemantic(graphics,
                TerritorySemantic.SOVEREIGN);
            case DISPUTED_TERRITORY -> paintLegendTerritorySemantic(graphics,
                TerritorySemantic.DISPUTED);
            case UNCLAIMED_POCKET -> paintLegendTerritorySemantic(graphics,
                TerritorySemantic.UNCLAIMED_POCKET);
            case ENCLAVE -> paintLegendTerritorySemantic(graphics, TerritorySemantic.ENCLAVE);
        }
    }

    private static void paintLegendRangeRing(Graphics2D graphics, Color color, Stroke stroke) {
        graphics.setPaint(color);
        graphics.setStroke(stroke);
        graphics.draw(new Ellipse2D.Double(16, 3, 32, 32));
        paintLegendRangeRingCenter(graphics);
    }

    private static void paintLegendSolidRangeRing(Graphics2D graphics, Color color) {
        Area ring = new Area(new Ellipse2D.Double(16, 3, 32, 32));
        ring.subtract(new Area(new Ellipse2D.Double(19, 6, 26, 26)));
        graphics.setPaint(color);
        graphics.fill(ring);
        paintLegendRangeRingCenter(graphics);
    }

    private static void paintLegendRangeRingCenter(Graphics2D graphics) {
        graphics.setPaint(SELECTED_SYSTEM_COLOR);
        graphics.fill(new Ellipse2D.Double(30, 17, 4, 4));
    }

    private static void paintLegendFactionOwnership(Graphics2D graphics) {
        Arc2D.Double arc = new Arc2D.Double();
          drawNavigationContact(graphics, arc, 18, 19, 6);
          drawFactionOwnershipRing(graphics, arc,
              SystemMarkerLayout.create(18, 19, 7.2, RouteMarkerState.NONE, false, false),
              List.of(new Color(232, 112, 84)));
          drawNavigationContact(graphics, arc, 46, 19, 6);
          drawFactionOwnershipRing(graphics, arc,
              SystemMarkerLayout.create(46, 19, 7.2, RouteMarkerState.NONE, false, false),
              List.of(new Color(88, 170, 230), new Color(114, 196, 126), new Color(232, 112, 84)));
    }

    private static void paintLegendCategoricalLayer(Graphics2D graphics, List<Color> colors) {
        double swatchWidth = 48.0 / colors.size();
        for (int index = 0; index < colors.size(); index++) {
            graphics.setPaint(colors.get(index));
            graphics.fill(new Rectangle2D.Double(8 + (index * swatchWidth), 10, swatchWidth, 18));
        }
        graphics.setPaint(withAlpha(Color.WHITE, 130));
        graphics.setStroke(new BasicStroke(0.8f));
        graphics.draw(new Rectangle2D.Double(8, 10, 48, 18));
    }

    private static void paintLegendFactionEmblem(Graphics2D graphics) {
        GeneralPath emblem = new GeneralPath();
        emblem.moveTo(32, 5);
        emblem.lineTo(48, 12);
        emblem.lineTo(44, 29);
        emblem.lineTo(32, 35);
        emblem.lineTo(20, 29);
        emblem.lineTo(16, 12);
        emblem.closePath();
        graphics.setPaint(withAlpha(Color.BLACK, 150));
        graphics.translate(2, 2);
        graphics.fill(emblem);
        graphics.translate(-2, -2);
        graphics.setPaint(withAlpha(new Color(88, 170, 230), 105));
        graphics.fill(emblem);
        graphics.setPaint(withAlpha(new Color(205, 232, 240), 120));
        graphics.setStroke(new BasicStroke(1.2f));
        graphics.draw(emblem);
        graphics.draw(new Line2D.Double(23, 18, 41, 18));
        graphics.draw(new Line2D.Double(32, 9, 32, 30));
    }

    private static void paintLegendSelectedSystem(Graphics2D graphics) {
        Arc2D.Double arc = new Arc2D.Double();
        drawNavigationContact(graphics, arc, 32, 19, 7);
        SystemMarkerLayout layout = SystemMarkerLayout.create(32, 19, 7,
              RouteMarkerState.NONE, true, false);
        drawSelectedSystemMarker(graphics, layout, "legend", 0.0);
    }

    private static void paintLegendHoveredSystem(Graphics2D graphics) {
        Arc2D.Double arc = new Arc2D.Double();
        drawNavigationContact(graphics, arc, 32, 19, 7);
        SystemMarkerLayout layout = SystemMarkerLayout.create(32, 19, 7,
              RouteMarkerState.NONE, false, true);
        drawHoveredSystemMarker(graphics, layout);
    }

    private static void paintLegendCurrentFleet(Graphics2D graphics) {
        Arc2D.Double contact = new Arc2D.Double();
        drawNavigationContact(graphics, contact, 25, 23, 4);
        if (CURRENT_LOCATION_ICON == null) {
            drawJumpShipIcon(graphics, 45, 10, 0.0);
        } else {
            graphics.drawImage(CURRENT_LOCATION_ICON, 34, 0, 26, 26, null);
        }
    }

    private Map<String, Integer> playerBaseCountsBySystem() {
        Map<String, Integer> counts = new HashMap<>();
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            if ((base.getCurrentLocation() != null) && (base.getCurrentLocation().getCurrentSystem() != null)) {
                counts.merge(base.getCurrentLocation().getCurrentSystem().getId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private static void drawPlayerBaseMarker(Graphics2D graphics, SystemMarkerLayout layout, int count,
          double detailAlpha) {
        double detailedRadius = Math.max(5.0, Math.min(8.0, layout.size() * 0.72));
        double radius = interpolate(4.2, detailedRadius, detailAlpha);
        Point2D.Double anchor = layout.playerBaseAnchor(radius, detailAlpha);
        drawPlayerBaseGlyph(graphics, anchor, radius, count, detailAlpha);
    }

    static void drawPlayerBaseGlyph(Graphics2D graphics, Point2D.Double anchor, double radius, int count,
          double countAlpha) {
        Stroke oldStroke = graphics.getStroke();
        Font oldFont = graphics.getFont();
        Paint oldPaint = graphics.getPaint();
        try {
            double left = anchor.x - radius;
            double top = anchor.y - radius;
            double width = radius * 2.0;
            double roofY = top + (radius * 0.55);
            GeneralPath base = new GeneralPath();
            base.moveTo(left, roofY);
            base.lineTo(anchor.x, top);
            base.lineTo(left + width, roofY);
            base.lineTo(left + (width * 0.84), roofY);
            base.lineTo(left + (width * 0.84), top + (radius * 1.7));
            base.lineTo(left + (width * 0.16), top + (radius * 1.7));
            base.lineTo(left + (width * 0.16), roofY);
            base.closePath();
            graphics.setPaint(PLAYER_BASE_DARK);
            graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(base);
            graphics.setPaint(PLAYER_BASE_COLOR);
            graphics.fill(base);
            graphics.setStroke(new BasicStroke(1.2f));
            graphics.draw(base);

            if ((count > 1) && (countAlpha > 0.0)) {
                String countText = Integer.toString(count);
                graphics.setFont(oldFont.deriveFont(Font.BOLD, (float) Math.min(9.0, Math.max(7.0, radius * 1.1))));
                double badgeRadius = radius * 0.58;
                double badgeX = anchor.x + (radius * 0.4);
                double badgeY = anchor.y + (radius * 0.4);
                graphics.setPaint(withAlpha(PLAYER_BASE_DARK, (int) Math.round(255 * countAlpha)));
                graphics.fill(new Ellipse2D.Double(badgeX - badgeRadius, badgeY - badgeRadius,
                      badgeRadius * 2.0, badgeRadius * 2.0));
                graphics.setPaint(withAlpha(PLAYER_BASE_COLOR, (int) Math.round(255 * countAlpha)));
                graphics.draw(new Ellipse2D.Double(badgeX - badgeRadius, badgeY - badgeRadius,
                      badgeRadius * 2.0, badgeRadius * 2.0));
                Point2D.Double baseline = centeredGlyphBaseline(graphics, countText, badgeX, badgeY);
                graphics.drawString(countText, (float) baseline.x, (float) baseline.y);
            }
        } finally {
            graphics.setStroke(oldStroke);
            graphics.setFont(oldFont);
            graphics.setPaint(oldPaint);
        }
    }

    private static void paintLegendCapitalHierarchy(Graphics2D graphics) {
        Arc2D.Double contact = new Arc2D.Double();
        Color capitalColor = new Color(232, 112, 84);
        for (int index = 0; index < 3; index++) {
            double x = 17 + (index * 15);
            drawNavigationContact(graphics, contact, x, 28, 3);
        }
        drawCapitalHierarchyMarker(graphics, new Point2D.Double(17, 10), 7,
              CapitalType.NATIONAL, capitalColor);
        drawCapitalHierarchyMarker(graphics, new Point2D.Double(32, 10), 7,
              CapitalType.REGION, capitalColor);
        drawCapitalHierarchyMarker(graphics, new Point2D.Double(47, 10), 7,
              CapitalType.DISTRICT, capitalColor);
    }

    private static void paintLegendPlannedRoute(Graphics2D graphics) {
        graphics.setPaint(PLANNED_ROUTE_COLOR);
        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
              new float[] { 7, 5 }, 0));
          graphics.draw(new Line2D.Double(5, 19, 38, 19));
          graphics.draw(new Ellipse2D.Double(24, 5, 28, 28));
          drawNavigationContact(graphics, new Arc2D.Double(), 38, 19, 4);
    }

    private static void paintLegendActiveRoute(Graphics2D graphics) {
        graphics.setPaint(ACTIVE_ROUTE_COLOR);
        graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
          graphics.draw(new Line2D.Double(5, 19, 38, 19));
          graphics.draw(new Ellipse2D.Double(24, 5, 28, 28));
          drawNavigationContact(graphics, new Arc2D.Double(), 38, 19, 4);
        graphics.setPaint(withAlpha(ACTIVE_ROUTE_COLOR, 75));
          graphics.fill(new Ellipse2D.Double(12, 15, 8, 8));
        graphics.setPaint(ACTIVE_ROUTE_FLOW_COLOR);
          graphics.fill(new Ellipse2D.Double(14.3, 17.3, 3.4, 3.4));
    }

    private static void paintLegendWaypointBadge(Graphics2D graphics) {
          drawNavigationContact(graphics, new Arc2D.Double(), 27, 16, 4);
          graphics.setPaint(withAlpha(MAP_BACKGROUND_BOTTOM, 235));
          graphics.fill(new Ellipse2D.Double(40, 20, 16, 16));
          graphics.setPaint(PLANNED_ROUTE_COLOR);
          graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 10.0f));
          graphics.drawString("2", 45, 32);
    }

    private static void paintLegendReachability(Graphics2D graphics) {
        NavigationMarkerShape[] shapes = { NavigationMarkerShape.CIRCLE, NavigationMarkerShape.SQUARE,
                                          NavigationMarkerShape.HEXAGON };
        double[] centers = { 10.0, 32.0, 54.0 };
        Arc2D.Double contact = new Arc2D.Double();
        Font oldFont = graphics.getFont();
        graphics.setFont(oldFont.deriveFont(Font.BOLD, 7.0f));
        FontMetrics metrics = graphics.getFontMetrics();
        for (int index = 0; index < shapes.length; index++) {
            Color color = index == 0 ? PLANNED_ROUTE_COLOR : REACHABILITY_DEEP_COLOR;
            String shell = Integer.toString(index + 1);
            graphics.setPaint(color);
            graphics.setStroke(new BasicStroke(1.8f));
            graphics.draw(createNavigationMarkerShape(shapes[index], centers[index], 21, 6));
            graphics.drawString(shell, (float) (centers[index] - (metrics.stringWidth(shell) / 2.0)), 9);
            drawNavigationContact(graphics, contact, centers[index], 21, 2.2);
        }
        graphics.setFont(oldFont);
    }

    private static void paintLegendReachabilityCaution(Graphics2D graphics) {
        drawNavigationContact(graphics, new Arc2D.Double(), 32, 20, 4);
        graphics.setPaint(NAVIGATION_CAUTION_COLOR);
        graphics.setStroke(new BasicStroke(2.2f));
        graphics.draw(createNavigationMarkerShape(NavigationMarkerShape.TRIANGLE, 32, 20, 15));
    }

    private static void paintLegendReachabilityBlocked(Graphics2D graphics) {
        drawNavigationContact(graphics, new Arc2D.Double(), 32, 19, 4);
        graphics.setPaint(NAVIGATION_BLOCKED_COLOR);
        graphics.setStroke(reachabilityMarkerStroke(NavigationMarkerTone.BLOCKED));
        graphics.draw(createNavigationMarkerShape(NavigationMarkerShape.DIAMOND, 32, 19, 15));
    }

    private static void paintLegendRouteCaution(Graphics2D graphics) {
        graphics.setPaint(PLANNED_ROUTE_COLOR);
        graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
              new float[] { 7, 5 }, 0));
        graphics.draw(new Line2D.Double(5, 19, 29, 19));
        drawNavigationContact(graphics, new Arc2D.Double(), 29, 19, 4);
        graphics.setPaint(NAVIGATION_CAUTION_COLOR);
        graphics.setStroke(new BasicStroke(2.2f));
        graphics.draw(createNavigationMarkerShape(NavigationMarkerShape.TRIANGLE, 53, 19, 7));
    }

    private static void paintLegendRouteBlocked(Graphics2D graphics) {
        graphics.setPaint(NAVIGATION_BLOCKED_COLOR);
        graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
              0, new float[] { 5, 5 }, 0));
        graphics.draw(new Line2D.Double(5, 19, 29, 19));
        drawNavigationContact(graphics, new Arc2D.Double(), 29, 19, 4);
        graphics.setPaint(NAVIGATION_BLOCKED_COLOR);
        graphics.setStroke(new BasicStroke(2.2f));
        graphics.draw(createNavigationMarkerShape(NavigationMarkerShape.DIAMOND, 53, 19, 7));
    }

    private static void paintLegendMeasurement(Graphics2D graphics) {
        graphics.setPaint(MEASUREMENT_COLOR);
        graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
              0, new float[] { 9, 4, 2, 4 }, 0));
        graphics.draw(new Line2D.Double(12, 19, 52, 19));
        graphics.setStroke(new BasicStroke(2.0f));
        graphics.draw(createNavigationMarkerShape(NavigationMarkerShape.CIRCLE, 12, 19, 5));
        graphics.draw(createNavigationMarkerShape(NavigationMarkerShape.DIAMOND, 52, 19, 5));
        drawNavigationContact(graphics, new Arc2D.Double(), 12, 19, 2.5);
        drawNavigationContact(graphics, new Arc2D.Double(), 52, 19, 2.5);
    }

    private static String getMapResource(String key) {
        return ResourceBundle.getBundle("mekhq.resources.CampaignGUI", MekHQ.getMHQOptions().getLocale())
                     .getString(key);
    }

    private static void paintLegendOperation(Graphics2D graphics) {
          SystemMarkerLayout urgentLayout = SystemMarkerLayout.create(46, 32, 2,
              RouteMarkerState.NONE, false, false);
          drawNavigationContact(graphics, new Arc2D.Double(), 46, 32, 4);
          drawOperationMarker(graphics, urgentLayout, new StrategicMarker(3, 1));
    }

    private static void paintLegendRestrictedSystem(Graphics2D graphics) {
        SystemMarkerLayout layout = SystemMarkerLayout.create(32, 19, 9,
              RouteMarkerState.NONE, false, false);
        drawNavigationContact(graphics, new Arc2D.Double(), 32, 19, 9);
        drawRestrictedSystemMarker(graphics, layout);
    }

    private static void paintLegendGmEditedSystem(Graphics2D graphics) {
        drawNavigationContact(graphics, new Arc2D.Double(), 32, 8, 7);
        drawGmEditedSystemMarker(graphics, new Point2D.Double(32, 28), 7);
    }

    private static void paintLegendHpgNetwork(Graphics2D graphics) {
        graphics.setPaint(Color.CYAN);
        graphics.setStroke(new BasicStroke(2.8f));
        graphics.draw(new Line2D.Double(5, 12, 59, 12));
        graphics.draw(new Ellipse2D.Double(8, 7, 10, 10));
        graphics.setPaint(Color.BLUE);
        graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
              new float[] { 6, 4 }, 0));
        graphics.draw(new Line2D.Double(5, 27, 59, 27));
        graphics.draw(new Ellipse2D.Double(46, 22, 10, 10));
    }

    private static void paintLegendHpgStations(Graphics2D graphics) {
        HPGRating[] ratings = { HPGRating.A, HPGRating.B, HPGRating.C, HPGRating.D };
        for (int index = 0; index < ratings.length; index++) {
            drawHpgStationBadge(graphics, new Point2D.Double(9 + (index * 15), 19), 6.0, ratings[index]);
        }
    }

    private static void paintLegendTerritorySemantic(Graphics2D graphics, TerritorySemantic semantic) {
        Shape territory = createLegendHex(32, 19, 14);
        Color factionColor = new Color(88, 170, 230);
        if (semantic == TerritorySemantic.UNCLAIMED_POCKET) {
            graphics.setPaint(TERRITORY_POCKET_FILL);
        } else if (semantic == TerritorySemantic.DISPUTED) {
            graphics.setPaint(new GradientPaint(18, 19, new Color(232, 112, 84, 110),
                  46, 19, new Color(88, 170, 230, 110), true));
        } else {
            graphics.setPaint(withAlpha(factionColor, 100));
        }
        graphics.fill(territory);

        switch (semantic) {
            case SOVEREIGN -> drawSovereignBoundary(graphics, territory, factionColor);
            case DISPUTED -> {
                paintLegendDisputedHatch(graphics, territory);
                graphics.setPaint(TERRITORY_BORDER_DARK);
                graphics.setStroke(new BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.draw(territory);
                graphics.setPaint(TERRITORY_NEUTRAL_EDGE);
                graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT,
                      BasicStroke.JOIN_ROUND, 0, new float[] { 7.0f, 4.0f }, 0));
                graphics.draw(territory);
            }
            case UNCLAIMED_POCKET -> drawUnclaimedPocketBoundary(graphics, territory, 1.0);
            case ENCLAVE -> drawEnclaveBoundary(graphics, territory, factionColor, 1.0);
            case UNCLAIMED_EXTERIOR -> {
            }
        }
    }

    private static void paintLegendDisputedHatch(Graphics2D graphics, Shape territory) {
        Graphics2D hatchGraphics = (Graphics2D) graphics.create();
        try {
            hatchGraphics.clip(territory);
            hatchGraphics.setStroke(new BasicStroke(1.2f));
            Color[] colors = { new Color(232, 112, 84), new Color(88, 170, 230) };
            int colorIndex = 0;
            for (int hatchX = 8; hatchX < 56; hatchX += 8) {
                hatchGraphics.setPaint(colors[colorIndex++ % colors.length]);
                hatchGraphics.draw(new Line2D.Double(hatchX, 34, hatchX + 30, 4));
            }
        } finally {
            hatchGraphics.dispose();
        }
    }

    private static GeneralPath createLegendHex(double centerX, double centerY, double radius) {
        GeneralPath hex = new GeneralPath();
        for (int vertex = 0; vertex < 6; vertex++) {
            double angle = Math.toRadians(60 * vertex);
            double x = centerX + (Math.cos(angle) * radius);
            double y = centerY + (Math.sin(angle) * radius);
            if (vertex == 0) {
                hex.moveTo(x, y);
            } else {
                hex.lineTo(x, y);
            }
        }
        hex.closePath();
        return hex;
    }

    private static List<String> getPathSystemIds(@Nullable JumpPath path) {
        if ((path == null) || path.isEmpty()) {
            return List.of();
        }
        List<String> systemIds = new ArrayList<>(path.size());
        for (PlanetarySystem system : path.getSystems()) {
            systemIds.add(system.getId());
        }
        return List.copyOf(systemIds);
    }

    private static List<PlanetarySystem> getPathSystems(@Nullable JumpPath path) {
        return ((path == null) || path.isEmpty()) ? List.of() : List.copyOf(path.getSystems());
    }

    private List<PlanetarySystem> resolveRouteSystems(List<String> systemIds) {
        List<PlanetarySystem> routeSystems = new ArrayList<>(systemIds.size());
        for (String systemId : systemIds) {
            PlanetarySystem system = campaign.getSystemById(systemId);
            if (system == null) {
                return List.of();
            }
            routeSystems.add(system);
        }
        return List.copyOf(routeSystems);
    }

    static JButton createMapLegendButton() {
        JButton legendButton = new JButton() {
            @Override
            protected void paintComponent(Graphics graphics) {
                paintMapLegendButton(graphics, this);
            }
        };
        Dimension buttonSize = UIUtil.scaleForGUI(MAP_LEGEND_BUTTON_SIZE, MAP_LEGEND_BUTTON_SIZE);
        String accessibleText = "Show map symbol legend";
        configureNavigationUtilityButton(legendButton, buttonSize, accessibleText, accessibleText,
              "Open a legend describing symbols on the interstellar map");
        return legendButton;
    }

    private static void paintMapLegendButton(Graphics graphics, AbstractButton legendButton) {
        Graphics2D buttonGraphics = (Graphics2D) graphics.create();
        try {
            buttonGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintNavigationUtilityButton(buttonGraphics, legendButton);

            double centerX = legendButton.getWidth() / 2.0;
            double centerY = legendButton.getHeight() / 2.0;
            double radius = Math.max(5.0, Math.min(legendButton.getWidth(), legendButton.getHeight()) * 0.3);
            buttonGraphics.setPaint(legendButton.isEnabled()
                  ? LAYER_CONTROL_BUTTON_ICON : LAYER_CONTROL_TEXT.darker());
            buttonGraphics.setStroke(new BasicStroke(Math.max(1.2f, UIUtil.scaleForGUI(1))));
            buttonGraphics.draw(new Ellipse2D.Double(centerX - radius, centerY - radius,
                  radius * 2.0, radius * 2.0));
            double glyphWidth = Math.max(1.4, UIUtil.scaleForGUI(1));
            buttonGraphics.setStroke(new BasicStroke((float) glyphWidth, BasicStroke.CAP_ROUND,
                  BasicStroke.JOIN_ROUND));
            buttonGraphics.draw(new Line2D.Double(centerX, centerY - 0.5,
                  centerX, centerY + (radius * 0.52)));
            buttonGraphics.fill(new Ellipse2D.Double(centerX - (glyphWidth / 2.0),
                  centerY - (radius * 0.58), glyphWidth, glyphWidth));

        } finally {
            buttonGraphics.dispose();
        }
    }

    static void configureNavigationUtilityButton(JButton button, Dimension buttonSize, String toolTipText,
          String accessibleName, String accessibleDescription) {
                setNavigationUtilityButtonSize(button, buttonSize);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(null);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setFocusable(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(toolTipText);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        button.getAccessibleContext().setAccessibleDescription(accessibleDescription);
        button.putClientProperty("navigationUtilityButton", Boolean.TRUE);
    }

    static JButton createNavigationUtilityIconButton(int symbolCodePoint, int buttonSize, String toolTipText,
          String accessibleName, String accessibleDescription) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D buttonGraphics = (Graphics2D) graphics.create();
                paintNavigationUtilityButton(buttonGraphics, this);
                buttonGraphics.dispose();
                super.paintComponent(graphics);
            }
        };
        button.setIcon(FontHandler.symbolIcon(symbolCodePoint, UIUtil.scaleForGUI(18), LAYER_CONTROL_BUTTON_ICON));
        button.setIconTextGap(0);
        configureNavigationUtilityButton(button, new Dimension(buttonSize, buttonSize), toolTipText,
              accessibleName, accessibleDescription);
        return button;
    }

    static void setNavigationUtilityButtonSize(JButton button, Dimension buttonSize) {
        button.setPreferredSize(buttonSize);
        button.setMinimumSize(buttonSize);
        button.setMaximumSize(buttonSize);
    }

    static void paintNavigationUtilityButton(Graphics2D graphics, AbstractButton button) {
        ButtonModel model = button.getModel();
        Color background = model.isPressed() ? LAYER_CONTROL_BUTTON_PRESSED
              : model.isRollover() ? LAYER_CONTROL_BUTTON_HOVER : NAVIGATION_UTILITY_BACKGROUND;
        graphics.setPaint(background);
        graphics.fillRect(0, 0, button.getWidth(), button.getHeight());
        graphics.setPaint(model.isRollover() || button.isFocusOwner()
              ? LAYER_CONTROL_BUTTON_ICON : LAYER_CONTROL_BORDER);
          float strokeWidth = Math.max(1, UIUtil.scaleForGUI(1));
          double borderInset = strokeWidth / 2.0;
          graphics.setStroke(new BasicStroke(strokeWidth));
          graphics.draw(new Rectangle2D.Double(borderInset, borderInset,
              button.getWidth() - strokeWidth, button.getHeight() - strokeWidth));
    }

    public void toggleMapLegendDialog() {
        if ((mapLegendDialog != null) && mapLegendDialog.isDisplayable()) {
            disposeMapLegendDialog();
            return;
        }
        mapLegendDialog = null;

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Map Symbols", Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getRootPane().putClientProperty("JRootPane.titleBarBackground", MAP_LEGEND_TITLE_BACKGROUND);
        dialog.getRootPane().putClientProperty("JRootPane.titleBarForeground", MAP_LEGEND_TITLE_FOREGROUND);

        JTabbedPane tabbedPane = createMapLegendTabbedPane();
        JPanel dialogContent = new JPanel(new BorderLayout());
        dialogContent.setOpaque(true);
        dialogContent.setBackground(MAP_LEGEND_BACKGROUND);
        int contentPadding = UIUtil.scaleForGUI(8);
        dialogContent.setBorder(BorderFactory.createEmptyBorder(contentPadding, contentPadding,
              contentPadding, contentPadding));
        dialogContent.add(tabbedPane, BorderLayout.CENTER);
        dialog.setContentPane(dialogContent);
        installMapLegendDialogCloseBinding(dialog);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                if (mapLegendDialog == dialog) {
                    mapLegendDialog = null;
                }
            }
        });
        mapLegendDialog = dialog;

        dialog.pack();
        Rectangle availableBounds = getMapLegendDialogBounds(owner);
        Dimension packedSize = dialog.getSize();
        dialog.setSize(Math.min(packedSize.width, availableBounds.width),
              Math.min(packedSize.height, availableBounds.height));
        Component relativeTo = mapPanel.isShowing() ? mapPanel : owner;
        dialog.setLocationRelativeTo(relativeTo);
        constrainToBounds(dialog, availableBounds);
        dialog.setVisible(true);
        SwingUtilities.invokeLater(() -> {
            if (dialog.isVisible()) {
                tabbedPane.requestFocusInWindow();
            }
        });
    }

    private static void installMapLegendDialogCloseBinding(JDialog dialog) {
        String actionName = "closeMapLegendDialog";
        JRootPane rootPane = dialog.getRootPane();
        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), actionName);
        rootPane.getActionMap().put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dialog.dispose();
            }
        });
    }

    private Rectangle getMapLegendDialogBounds(@Nullable Window owner) {
        Rectangle bounds = getUsableScreenBounds(mapPanel);
        if ((owner != null) && owner.isShowing()) {
            Rectangle ownerBounds = bounds.intersection(owner.getBounds());
            if (!ownerBounds.isEmpty()) {
                bounds = ownerBounds;
            }
        }
        int margin = UIUtil.scaleForGUI(MAP_LEGEND_DIALOG_MARGIN);
        if ((bounds.width > (margin * 2)) && (bounds.height > (margin * 2))) {
            bounds.grow(-margin, -margin);
        }
        return bounds;
    }

    private static void constrainToBounds(Window window, Rectangle bounds) {
        int maximumX = bounds.x + bounds.width - window.getWidth();
        int maximumY = bounds.y + bounds.height - window.getHeight();
        int x = Math.max(bounds.x, Math.min(window.getX(), maximumX));
        int y = Math.max(bounds.y, Math.min(window.getY(), maximumY));
        window.setLocation(x, y);
    }

    private void disposeMapLegendDialog() {
        JDialog dialog = mapLegendDialog;
        mapLegendDialog = null;
        if (dialog != null) {
            dialog.dispose();
        }
    }

    private static Rectangle getUsableScreenBounds(Component component) {
        GraphicsConfiguration configuration = component.getGraphicsConfiguration();
        Rectangle usableScreen = new Rectangle(configuration.getBounds());
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(configuration);
        usableScreen.x += screenInsets.left;
        usableScreen.y += screenInsets.top;
        usableScreen.width -= screenInsets.left + screenInsets.right;
        usableScreen.height -= screenInsets.top + screenInsets.bottom;
        return usableScreen;
    }

    private void startOptionPanelAnimation() {
        optionControl.setVisible(true);
        long currentTime = System.nanoTime();
        advanceLayerAnimations(currentTime);
        optionPanelAnimationStartExpansion = optionPanelExpansion;
        optionPanelAnimationTargetExpansion = optionPanelHidden ? 0.0 : 1.0;
        double remainingDistance = Math.abs(optionPanelAnimationTargetExpansion - optionPanelExpansion);
        optionPanelAnimationDuration = getScaledLayerAnimationDuration(remainingDistance);
        optionPanelAnimationStartTime = currentTime;
        optionPanelAnimating = remainingDistance > 0.0;
        startLayerAnimationTimerIfNeeded();
        updateOptionViewBounds(getWidth(), getHeight());
        repaint();
    }

    /** Opens or closes the map layer drawer while preserving its current choices. */
    public void toggleLayerControls() {
        optionPanelHidden = !optionPanelHidden;
        startOptionPanelAnimation();
        updateLayerDismissalListener();
    }

    void toggleLayerControls(Component invoker) {
        layerControlsInvoker = invoker;
        toggleLayerControls();
    }

    private void updateLayerDismissalListener() {
        boolean shouldListen = !optionPanelHidden && isShowing();
        if (shouldListen == layerDismissalListenerInstalled) {
            return;
        }
        if (shouldListen) {
            Toolkit.getDefaultToolkit().addAWTEventListener(layerDismissalListener, AWTEvent.MOUSE_EVENT_MASK);
        } else {
            Toolkit.getDefaultToolkit().removeAWTEventListener(layerDismissalListener);
        }
        layerDismissalListenerInstalled = shouldListen;
    }

    private void dismissLayersOnOutsideClick(AWTEvent event) {
        if (!(event instanceof MouseEvent mouseEvent) || (mouseEvent.getID() != MouseEvent.MOUSE_PRESSED)
              || optionPanelHidden || !isShowing()) {
            return;
        }
        Component source = mouseEvent.getComponent();
        if ((SwingUtilities.getWindowAncestor(source) == SwingUtilities.getWindowAncestor(this))
              && !isLayerControlInteraction(source, optionControl, layerControlsInvoker)) {
            optionPanelHidden = true;
            startOptionPanelAnimation();
            updateLayerDismissalListener();
        }
    }

    static boolean isLayerControlInteraction(Component source, Component control, @Nullable Component invoker) {
        Component candidate = source;
        while (candidate != null) {
            if ((candidate == control) || (candidate == invoker)) {
                return true;
            }
            candidate = candidate instanceof JPopupMenu popup ? popup.getInvoker() : candidate.getParent();
        }
        return false;
    }

    private void startTerritoryLayerAnimation() {
        long currentTime = System.nanoTime();
        advanceLayerAnimations(currentTime);
        territoryLayerSettling = false;
        territoryLayerAnimationStartAlpha = territoryLayerAlpha;
        territoryLayerAnimationTargetAlpha = optTerritory.isSelected() ? 1.0 : 0.0;
        double remainingDistance = Math.abs(territoryLayerAnimationTargetAlpha - territoryLayerAlpha);
        territoryLayerAnimationDuration = getScaledLayerAnimationDuration(remainingDistance);
        territoryLayerAnimationStartTime = currentTime;
        territoryLayerAnimating = remainingDistance > 0.0;
        startLayerAnimationTimerIfNeeded();
        repaint();
    }

    private void startHpgNetworkLayerAnimation() {
        long currentTime = System.nanoTime();
        advanceLayerAnimations(currentTime);
        hpgNetworkLayerAnimationStartAlpha = hpgNetworkLayerAlpha;
        hpgNetworkLayerAnimationTargetAlpha = optHPGNetwork.isSelected() ? 1.0 : 0.0;
        double remainingDistance = Math.abs(hpgNetworkLayerAnimationTargetAlpha - hpgNetworkLayerAlpha);
        hpgNetworkLayerAnimationDuration = getScaledLayerAnimationDuration(remainingDistance);
        hpgNetworkLayerAnimationStartTime = currentTime;
        hpgNetworkLayerAnimating = remainingDistance > 0.0;
        startLayerAnimationTimerIfNeeded();
        repaint();
    }

    private void startOperationsLayerAnimation() {
        long currentTime = System.nanoTime();
        advanceLayerAnimations(currentTime);
        operationsLayerAnimationStartAlpha = operationsLayerAlpha;
        operationsLayerAnimationTargetAlpha = optOperations.isSelected() ? 1.0 : 0.0;
        double remainingDistance = Math.abs(operationsLayerAnimationTargetAlpha - operationsLayerAlpha);
        operationsLayerAnimationDuration = getScaledLayerAnimationDuration(remainingDistance);
        operationsLayerAnimationStartTime = currentTime;
        operationsLayerAnimating = remainingDistance > 0.0;
        startLayerAnimationTimerIfNeeded();
        repaint();
    }

    private void startMapModeAnimation(MapMode selectedMode) {
        long currentTime = System.nanoTime();
        advanceLayerAnimations(currentTime);
        if (selectedMode == targetMapMode) {
            return;
        }

        boolean reversingActiveTransition = mapModeAnimating && (selectedMode == previousMapMode);
        double startProgress = 0.0;
        if (reversingActiveTransition) {
            MapMode outgoingMode = targetMapMode;
            startProgress = 1.0 - mapModeAnimationProgress;
            previousMapMode = outgoingMode;
        } else {
            previousMapMode = targetMapMode;
        }
        targetMapMode = selectedMode;
        mapModeAnimationStartProgress = startProgress;
        mapModeAnimationProgress = startProgress;
        mapModeAnimationDuration = Math.max(1L,
              Math.round(MAP_MODE_ANIMATION_DURATION_NS * (1.0 - startProgress)));
        mapModeAnimationStartTime = currentTime;
        mapModeAnimating = previousMapMode != targetMapMode;
          mapModeTransitionSettling = false;
          clearMapModeTransitionRenderCaches();
          mapModeTransitionCacheStage = reversingActiveTransition
              ? MapModeTransitionCacheStage.READY
              : MapModeTransitionCacheStage.CARTOGRAPHY;
        startLayerAnimationTimerIfNeeded();
        repaint();
    }

    private void startLayerAnimationTimerIfNeeded() {
        if (hasActiveLayerAnimation() && !layerAnimationTimer.isRunning()) {
            layerAnimationTimer.start();
        }
    }

    private static long getScaledLayerAnimationDuration(double remainingDistance) {
        return Math.max(1L, Math.round(LAYER_ANIMATION_DURATION_NS * remainingDistance));
    }

    private void updateLayerAnimations() {
        boolean changed = advanceLayerAnimations(System.nanoTime());
        if (!hasActiveLayerAnimation()) {
            layerAnimationTimer.stop();
        }
        if (changed) {
            updateOptionViewBounds(getWidth(), getHeight());
            repaint();
        }
    }

    private boolean advanceLayerAnimations(long currentTime) {
        boolean changed = false;
        if (optionPanelAnimating) {
            double elapsedProgress = getAnimationProgress(currentTime, optionPanelAnimationStartTime,
                  optionPanelAnimationDuration);
            optionPanelExpansion = interpolate(optionPanelAnimationStartExpansion,
                  optionPanelAnimationTargetExpansion, easeInOutCubic(elapsedProgress));
            if (elapsedProgress >= 1.0) {
                optionPanelExpansion = optionPanelAnimationTargetExpansion;
                optionPanelAnimating = false;
                optionControl.setVisible(!optionPanelHidden);
            }
            changed = true;
        }
        if (territoryLayerAnimating) {
            double elapsedProgress = getAnimationProgress(currentTime, territoryLayerAnimationStartTime,
                  territoryLayerAnimationDuration);
            territoryLayerAlpha = interpolate(territoryLayerAnimationStartAlpha,
                  territoryLayerAnimationTargetAlpha, easeInOutCubic(elapsedProgress));
            if (elapsedProgress >= 1.0) {
                territoryLayerAlpha = territoryLayerAnimationTargetAlpha;
                territoryLayerAnimating = false;
                territoryLayerSettling = true;
            }
            changed = true;
        }
        if (hpgNetworkLayerAnimating) {
            double elapsedProgress = getAnimationProgress(currentTime, hpgNetworkLayerAnimationStartTime,
                  hpgNetworkLayerAnimationDuration);
            hpgNetworkLayerAlpha = interpolate(hpgNetworkLayerAnimationStartAlpha,
                  hpgNetworkLayerAnimationTargetAlpha, easeInOutCubic(elapsedProgress));
            if (elapsedProgress >= 1.0) {
                hpgNetworkLayerAlpha = hpgNetworkLayerAnimationTargetAlpha;
                hpgNetworkLayerAnimating = false;
            }
            changed = true;
        }
        if (operationsLayerAnimating) {
            double elapsedProgress = getAnimationProgress(currentTime, operationsLayerAnimationStartTime,
                  operationsLayerAnimationDuration);
            operationsLayerAlpha = interpolate(operationsLayerAnimationStartAlpha,
                  operationsLayerAnimationTargetAlpha, easeInOutCubic(elapsedProgress));
            if (elapsedProgress >= 1.0) {
                operationsLayerAlpha = operationsLayerAnimationTargetAlpha;
                operationsLayerAnimating = false;
            }
            changed = true;
        }
        if (mapModeAnimating) {
            if (!isPreparingMapModeTransition()) {
                double elapsedProgress = getAnimationProgress(currentTime, mapModeAnimationStartTime,
                    mapModeAnimationDuration);
                mapModeAnimationProgress = interpolate(mapModeAnimationStartProgress, 1.0,
                    easeInOutCubic(elapsedProgress));
                if (elapsedProgress >= 1.0) {
                  mapModeAnimationProgress = 1.0;
                  previousMapMode = targetMapMode;
                  mapModeAnimating = false;
                  mapModeTransitionSettling = true;
                }
            }
            changed = true;
        }
        return changed;
    }

    private boolean hasActiveLayerAnimation() {
        return optionPanelAnimating || territoryLayerAnimating || hpgNetworkLayerAnimating
              || operationsLayerAnimating || mapModeAnimating;
    }

    private static double getAnimationProgress(long currentTime, long startTime, long duration) {
        return Math.min(1.0, (double) (currentTime - startTime) / duration);
    }

    private boolean isPreparingMapModeTransition() {
          return (mapModeTransitionCacheStage == MapModeTransitionCacheStage.CARTOGRAPHY)
              || (mapModeTransitionCacheStage == MapModeTransitionCacheStage.SYSTEMS)
              || (mapModeTransitionCacheStage == MapModeTransitionCacheStage.PREVIOUS)
              || (mapModeTransitionCacheStage == MapModeTransitionCacheStage.TARGET);
    }

    private boolean isTransitionBenchmarkAwaitingSettledFrame() {
        return (transitionBenchmarkRun != null)
              && ((transitionBenchmarkPhase == TransitionBenchmarkPhase.MODE_TRANSITION)
                    || (transitionBenchmarkPhase == TransitionBenchmarkPhase.RESTORE));
    }

    private void finishSettledMapModeTransitionFrame() {
        mapModeTransitionSettling = false;
        mapModeTransitionCacheStage = MapModeTransitionCacheStage.INACTIVE;
        clearMapModeTransitionRenderCaches();
        repaint();
    }

    private static void paintLayerWithAlpha(Graphics2D graphics, double alpha,
          Consumer<Graphics2D> layerPainter) {
        Graphics2D layerGraphics = createLayerGraphicsWithAlpha(graphics, alpha);
        try {
            layerPainter.accept(layerGraphics);
        } finally {
            layerGraphics.dispose();
        }
    }

    private static Graphics2D createLayerGraphicsWithAlpha(Graphics2D graphics, double alpha) {
        Graphics2D layerGraphics = (Graphics2D) graphics.create();
        layerGraphics.setComposite(deriveCompositeWithAlpha(layerGraphics.getComposite(), alpha));
        return layerGraphics;
    }

    private static Composite deriveCompositeWithAlpha(Composite composite, double alpha) {
        float clampedAlpha = (float) Math.clamp(alpha, 0.0, 1.0);
        if (composite instanceof AlphaComposite alphaComposite) {
            return alphaComposite.derive(alphaComposite.getAlpha() * clampedAlpha);
        }
        return AlphaComposite.SrcOver.derive(clampedAlpha);
    }

    private void updateOptionViewBounds(int containerWidth, int containerHeight) {
        if ((containerWidth <= 0) || (containerHeight <= 0)) {
            return;
        }

        Dimension preferredSize = optionPanel.getPreferredSize();
        Insets controlInsets = optionControl.getInsets();
        int collapsedWidth = 0;
        int collapsedHeight = 0;
        int expandedWidth = Math.max(collapsedWidth,
              preferredSize.width + controlInsets.left + controlInsets.right);
        int expandedHeight = Math.max(collapsedHeight,
              preferredSize.height + controlInsets.top + controlInsets.bottom);
        int controlMargin = UIUtil.scaleForGUI(10);
        int availableWidth = Math.max(1, containerWidth - (controlMargin * 2));
        int availableHeight = Math.max(1, containerHeight - (controlMargin * 2));
        expandedWidth = Math.min(expandedWidth, availableWidth);
        expandedHeight = Math.min(expandedHeight, availableHeight);
        collapsedWidth = Math.min(collapsedWidth, expandedWidth);
        collapsedHeight = Math.min(collapsedHeight, expandedHeight);
        int viewportWidth = (int) Math.round(interpolate(collapsedWidth, expandedWidth, optionPanelExpansion));
        int viewportHeight = (int) Math.round(interpolate(collapsedHeight, expandedHeight, optionPanelExpansion));

        Rectangle controlBounds = new Rectangle(
              Math.max(0, containerWidth - controlMargin - viewportWidth),
              Math.max(0, containerHeight - controlMargin - viewportHeight), viewportWidth, viewportHeight);
        int contentWidth = Math.max(0, viewportWidth - controlInsets.left - controlInsets.right);
        int contentHeight = Math.max(0, viewportHeight - controlInsets.top - controlInsets.bottom);
        int expandedContentWidth = Math.max(0, expandedWidth - controlInsets.left - controlInsets.right);
        int expandedContentHeight = Math.max(0, expandedHeight - controlInsets.top - controlInsets.bottom);
        Dimension viewSize = new Dimension(expandedContentWidth, expandedContentHeight);
        Rectangle viewBounds = new Rectangle(controlInsets.left, controlInsets.top, contentWidth, contentHeight);
        Point viewPosition = new Point(0, 0);
        if (isOptionViewLayoutCurrent(optionControl, optionView, controlBounds, viewSize, viewBounds, viewPosition)) {
            return;
        }

        optionControl.setBounds(controlBounds);
        optionView.setViewSize(viewSize);
        optionView.setBounds(viewBounds);
        optionView.setViewPosition(viewPosition);
        optionControl.revalidate();
        optionView.revalidate();
    }

    static boolean isOptionViewLayoutCurrent(JPanel control, JViewport view, Rectangle controlBounds,
          Dimension viewSize, Rectangle viewBounds, Point viewPosition) {
        return controlBounds.equals(control.getBounds()) && viewSize.equals(view.getViewSize())
              && viewBounds.equals(view.getBounds()) && viewPosition.equals(view.getViewPosition());
    }

    public void setCampaign(Campaign c) {
        this.campaign = c;
        refreshSystemsFromCampaign();
        settleTravelVisualState();
        refreshNavigationAnalysis();
        repaint();
    }

    /** Refreshes immutable navigation snapshots after explicit campaign, route, date, or option changes. */
    public void refreshNavigationAnalysis() {
        refreshMapPresentationData();
        refreshRouteAssessments();
        refreshReachability();
        refreshMeasurementAssessment();
        repaint();
    }

    public void refreshPresentationData() {
        refreshMapPresentationData();
        repaint();
    }

    private void refreshRouteAssessments() {
        cachedProposedRouteAssessment = assessPath(jumpPath, true);
        cachedActiveRouteAssessment = assessPath(getActiveJumpPath(), false);
    }

    private PathAssessment assessPath(@Nullable JumpPath path, boolean includeRequestedStops) {
        if ((campaign == null) || (path == null) || (path.size() < 2)) {
            return emptyPathAssessment();
        }
        List<PlanetarySystem> requestedStops = includeRequestedStops
                                                     ? path.getSystems().stream()
                                                           .skip(1)
                                                           .filter(routePlanningHandler::isRequestedWaypoint)
                                                           .toList()
                                                     : List.of();
        PathAssessment assessment = requestedStops.isEmpty()
                                              ? campaign.assessNavigationPath(path.getSystems(),
                                                    campaign.isUseCommandCircuit())
                                              : campaign.assessNavigationPath(path.getSystems(), requestedStops,
                                                    destinationIndex -> campaign.isUseCommandCircuit());
        return assessment == null ? emptyPathAssessment() : assessment;
    }

    private void refreshReachability() {
        reachabilityRevision++;
        if ((campaign == null) || !optReachability.isSelected()) {
            cachedReachability = null;
            return;
        }
        PlanetarySystem anchor = selectedSystem == null ? campaign.getCurrentSystem() : selectedSystem;
        if (anchor == null) {
            cachedReachability = null;
            return;
        }
        cachedReachability = campaign.calculateNavigationReachability(anchor,
              ((Number) reachabilityHops.getValue()).intValue(), campaign.isUseCommandCircuit());
    }

    private void refreshMeasurementAssessment() {
        PlanetarySystem target = measurementState.end() == null
              ? measurementHoverSystem
              : measurementState.end();
        if ((campaign == null) || (measurementState.start() == null) || (target == null)) {
            cachedMeasurementAssessment = null;
            return;
        }
        cachedMeasurementAssessment = campaign.assessNavigationLeg(measurementState.start(), target,
              campaign.isUseCommandCircuit());
    }

    private void stopMeasuring() {
        measurementState = MeasurementState.inactive();
        measurementHoverSystem = null;
        cachedMeasurementAssessment = null;
        optMeasureDistance.setSelected(false);
        repaint();
    }

    private static PathAssessment emptyPathAssessment() {
        return new PathAssessment(List.of(), Severity.CLEAR);
    }

    private void refreshSystemsFromCampaign() {
        String selectedSystemId = selectedSystem == null ? null : selectedSystem.getId();
        this.systems = campaign.getSystems();
        systemSpatialIndex = new SystemSpatialIndex(systems);
        refreshMapPresentationData();
        cartographyDataRevision++;
        territoryPreparationQueue.cancel();
        preparedTerritoryAtlas.clear();
        preparedSystemRenderData.clear();
        clearRenderLayerCaches();
        clearFactionLogoImages();
        requestStaticCartographyPreparation();
        if (selectedSystemId != null) {
            selectSystem(campaign.getSystemById(selectedSystemId), false);
        }
    }

    public void setJumpPath(@Nullable JumpPath path) {
        setProposedJumpPath(path);
    }

    private void setProposedJumpPath(@Nullable JumpPath path) {
        List<String> previousRouteSystemIds = getPathSystemIds(jumpPath);
        jumpPath = path == null ? new JumpPath() : path;
        List<String> proposedRouteSystemIds = getPathSystemIds(jumpPath);
        if (!proposedRouteSystemIds.isEmpty()) {
            cachedProposedRouteSystemIds = proposedRouteSystemIds;
        } else if (!previousRouteSystemIds.equals(getPathSystemIds(getActiveJumpPath()))) {
            cachedProposedRouteSystemIds = List.of();
        }
        if (jumpPath.size() > 1) {
            startProposedRouteAnimation();
        } else {
            stopProposedRouteAnimation();
        }
        refreshTravelVisualState();
        if (proposedRouteSystemIds.isEmpty()) {
            cachedProposedRouteSystemIds = List.of();
        }
        refreshRouteAssessments();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        pane.setBounds(0, 0, width, height);
        mapPanel.setBounds(0, 0, width, height);
        updateOptionViewBounds(width, height);

        super.paintComponent(g);
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setOpaque(false);
        label.setForeground(PLANNED_ROUTE_COLOR);
        label.setFont(label.getFont().deriveFont(Font.BOLD, label.getFont().getSize2D() * 0.85f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 3, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return (label);
    }

    private JPanel createOptionDivider() {
        JPanel divider = new JPanel();
        divider.setOpaque(true);
        divider.setBackground(LAYER_CONTROL_BORDER);
        divider.setPreferredSize(new Dimension(150, 1));
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        return divider;
    }

    private JCheckBox createOptionCheckBox(String text) {
        JCheckBox checkBox = new ImmersiveCheckBox(text);
        checkBox.setForeground(LAYER_CONTROL_TEXT);
        checkBox.setPreferredSize(new Dimension(150, 20));
        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return checkBox;
    }

    private JRadioButton createOptionRadioButton(String text, MapMode mapMode) {
        JRadioButton radioButton = new ImmersiveRadioButton(text);
        radioButton.setForeground(LAYER_CONTROL_TEXT);
        radioButton.setPreferredSize(new Dimension(150, 20));
        radioButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        radioButton.addActionListener(e -> {
            if (radioButton.isSelected()) {
                startMapModeAnimation(mapMode);
            }
        });
        return radioButton;
    }

    private void selectMapMode(MapMode mapMode) {
        switch (mapMode) {
            case FACTION -> optFactions.setSelected(true);
            case TECHNOLOGY -> optTech.setSelected(true);
            case INDUSTRY -> optIndustry.setSelected(true);
            case RAW_MATERIALS -> optRawMaterials.setSelected(true);
            case OUTPUT -> optOutput.setSelected(true);
            case AGRICULTURE -> optAgriculture.setSelected(true);
            case POPULATION -> optPopulation.setSelected(true);
            case HPG -> optHPG.setSelected(true);
            case RECHARGE_STATIONS -> optRecharge.setSelected(true);
            case ACADEMIES -> optAcademies.setSelected(true);
            case HIRING_HALLS -> optHiringHalls.setSelected(true);
            case DISEASE_OUTBREAKS -> optDiseases.setSelected(true);
        }
    }

    private static void setupHexPath(@Nullable GeneralPath path, double centerX, double centerY, double radius) {
        if (null == path) {
            return;
        }
        radius *= Math.sqrt(4.0 / 3.0);
        path.reset();
        path.moveTo(centerX + radius * BASE_HEX_COORDS[0].x, centerY + radius * BASE_HEX_COORDS[0].y);
        for (int i = 1; i < 6; ++i) {
            path.lineTo(centerX + radius * BASE_HEX_COORDS[i].x, centerY + radius * BASE_HEX_COORDS[i].y);
        }
        path.closePath();
    }

    private void paintStaticCartographyBackground(Graphics2D graphics, RenderViewKey renderViewKey) {
        drawMapBackground(graphics, renderViewKey.width(), renderViewKey.height());
    }

    private void paintStaticTerritoryLayer(Graphics2D graphics, TerritoryAtlas atlas,
          TerritoryRenderKey renderKey, double alpha) {
        RenderViewKey viewKey = renderKey.viewKey();
        int overscan = renderLayerOverscan(viewKey.width(), viewKey.height());
        if (overscan == 0) {
            territoryRenderCache.clear();
            paintLayerWithAlpha(graphics, alpha, layerGraphics -> drawTerritoryLayer(layerGraphics, atlas));
            return;
        }
        TerritoryDataKey dataKey = new TerritoryDataKey(renderKey.date(), renderKey.dataRevision());
        PannableRenderLayer territory = territoryRenderCache.getOrRender(dataKey, viewKey, overscan,
              layerGraphics -> drawTerritoryLayer(layerGraphics, atlas, overscan));
          drawPannableRenderLayer(graphics, territory, viewKey.width(), viewKey.height(), alpha);
    }

        private void paintRetainedMapModeTransition(Graphics2D graphics, TerritoryAtlas atlas,
            TerritoryRenderKey renderKey, Map<String, SystemRenderData> systemRenderData,
            MapMode previousMode, MapMode targetMode, HpgNetworkDetail hpgNetworkDetail,
            double systemSize, double territoryAlpha, double factionLogoAlpha,
            double hpgNetworkAlpha, SemanticZoomProfile semanticZoom, Stroke thick,
            Stroke dashed, List<PlanetarySystem> activeRouteSystems,
            int revealedProposedRouteSystemCount, double transitionProgress) {
          territoryRenderCache.clear();
          factionLogoRenderCache.clear();
          retainedSystemArtRenderCache.clear();
          RenderViewKey viewKey = renderKey.viewKey();
          FactionLogoRenderKey factionLogoRenderKey = createFactionLogoRenderKey(renderKey);
          int overscan = renderLayerOverscan(viewKey.width(), viewKey.height());
          RetainedCartographyKey stableCartographyKey = createRetainedCartographyKey(
              renderKey, targetMode, hpgNetworkDetail, systemSize, territoryAlpha,
              0.0, hpgNetworkAlpha, semanticZoom, factionLogoRenderKey);
          RetainedNavigationKey stableKey = new RetainedNavigationKey(stableCartographyKey,
              getPathSystemIds(jumpPath), getSystemIds(activeRouteSystems), reachabilityRevision);
                    if (mapModeTransitionCacheStage == MapModeTransitionCacheStage.CARTOGRAPHY) {
            mapModeTransitionBaseRenderCache.getOrRender(
                stableKey, viewKey, overscan,
                layerGraphics -> drawRetainedMapModeTransitionBase(layerGraphics, atlas,
                                        factionLogoRenderKey, systemRenderData, hpgNetworkDetail, systemSize, territoryAlpha,
                                        hpgNetworkAlpha, semanticZoom, thick, dashed,
                    activeRouteSystems, revealedProposedRouteSystemCount, overscan));
                        mapModeTransitionCacheStage = MapModeTransitionCacheStage.SYSTEMS;
                        paintRetainedMapModeTransitionSource(graphics, atlas, renderKey, systemRenderData,
                                previousMode, hpgNetworkDetail, systemSize, territoryAlpha, factionLogoAlpha,
                                hpgNetworkAlpha, semanticZoom, thick, dashed, activeRouteSystems,
                                revealedProposedRouteSystemCount);
                        repaint();
                        return;
                    }
                    if (mapModeTransitionCacheStage == MapModeTransitionCacheStage.SYSTEMS) {
                        mapModeTransitionSystemRenderCache.getOrRender(
                                stableCartographyKey, viewKey, overscan,
                                layerGraphics -> drawRetainedSystemArt(layerGraphics, systemRenderData,
                                        targetMode, systemSize, semanticZoom, overscan, true, false));
            mapModeTransitionCacheStage = MapModeTransitionCacheStage.PREVIOUS;
            paintRetainedMapModeTransitionSource(graphics, atlas, renderKey, systemRenderData,
                previousMode, hpgNetworkDetail, systemSize, territoryAlpha, factionLogoAlpha,
                hpgNetworkAlpha, semanticZoom, thick, dashed, activeRouteSystems,
                revealedProposedRouteSystemCount);
            repaint();
            return;
          }
          if (mapModeTransitionCacheStage == MapModeTransitionCacheStage.PREVIOUS) {
            getRetainedMapModeEndpoint(previousMapModeRenderCache, atlas, renderKey,
                factionLogoRenderKey, systemRenderData, previousMode, hpgNetworkDetail,
                systemSize, factionLogoAlpha, semanticZoom, overscan);
            mapModeTransitionCacheStage = MapModeTransitionCacheStage.TARGET;
            paintRetainedMapModeTransitionSource(graphics, atlas, renderKey, systemRenderData,
                previousMode, hpgNetworkDetail, systemSize, territoryAlpha, factionLogoAlpha,
                hpgNetworkAlpha, semanticZoom, thick, dashed, activeRouteSystems,
                revealedProposedRouteSystemCount);
            repaint();
            return;
          }
          if (mapModeTransitionCacheStage == MapModeTransitionCacheStage.TARGET) {
            getRetainedMapModeEndpoint(targetMapModeRenderCache, atlas, renderKey,
                factionLogoRenderKey, systemRenderData, targetMode, hpgNetworkDetail,
                systemSize, factionLogoAlpha, semanticZoom, overscan);
            mapModeTransitionCacheStage = MapModeTransitionCacheStage.READY;
            mapModeAnimationStartTime = System.nanoTime();
            paintRetainedMapModeTransitionSource(graphics, atlas, renderKey, systemRenderData,
                previousMode, hpgNetworkDetail, systemSize, territoryAlpha, factionLogoAlpha,
                hpgNetworkAlpha, semanticZoom, thick, dashed, activeRouteSystems,
                revealedProposedRouteSystemCount);
            repaint();
            return;
          }
          PannableRenderLayer stableLayer = mapModeTransitionBaseRenderCache.getOrRender(
              stableKey, viewKey, overscan,
              layerGraphics -> drawRetainedMapModeTransitionBase(layerGraphics, atlas,
                  factionLogoRenderKey, systemRenderData, hpgNetworkDetail, systemSize, territoryAlpha,
                  hpgNetworkAlpha, semanticZoom, thick, dashed,
                  activeRouteSystems, revealedProposedRouteSystemCount, overscan));
          drawPannableRenderLayer(graphics, stableLayer, viewKey.width(), viewKey.height(), 1.0);
          PannableRenderLayer systemLayer = mapModeTransitionSystemRenderCache.getOrRender(
              stableCartographyKey, viewKey, overscan,
              layerGraphics -> drawRetainedSystemArt(layerGraphics, systemRenderData,
                  targetMode, systemSize, semanticZoom, overscan, true, false));
          drawPannableRenderLayer(graphics, systemLayer, viewKey.width(), viewKey.height(), 1.0);
          PannableRenderLayer previousLayer = getRetainedMapModeEndpoint(
              previousMapModeRenderCache, atlas, renderKey,
              factionLogoRenderKey, systemRenderData, previousMode, hpgNetworkDetail,
              systemSize, factionLogoAlpha, semanticZoom, overscan);
          drawPannableRenderLayer(graphics, previousLayer, viewKey.width(), viewKey.height(),
              1.0 - transitionProgress);
          PannableRenderLayer targetLayer = getRetainedMapModeEndpoint(
              targetMapModeRenderCache, atlas, renderKey,
              factionLogoRenderKey, systemRenderData, targetMode, hpgNetworkDetail,
              systemSize, factionLogoAlpha, semanticZoom, overscan);
          drawPannableRenderLayer(graphics, targetLayer, viewKey.width(), viewKey.height(),
              transitionProgress);
        }

        private void paintRetainedMapModeTransitionSource(Graphics2D graphics,
            TerritoryAtlas atlas, TerritoryRenderKey renderKey,
            Map<String, SystemRenderData> systemRenderData, MapMode mapMode,
            HpgNetworkDetail hpgNetworkDetail, double systemSize, double territoryAlpha,
            double factionLogoAlpha, double hpgNetworkAlpha, SemanticZoomProfile semanticZoom,
            Stroke thick, Stroke dashed, List<PlanetarySystem> activeRouteSystems,
            int revealedProposedRouteSystemCount) {
          double sourceFactionLogoAlpha = mapMode == MapMode.FACTION ? factionLogoAlpha : 0.0;
          paintRetainedNavigationLayer(graphics, atlas, renderKey, systemRenderData,
              mapMode, hpgNetworkDetail, systemSize, territoryAlpha, sourceFactionLogoAlpha,
              hpgNetworkAlpha, semanticZoom, thick, dashed, activeRouteSystems,
              revealedProposedRouteSystemCount);
        }

        private void drawRetainedMapModeTransitionBase(Graphics2D graphics,
            TerritoryAtlas atlas, FactionLogoRenderKey factionLogoRenderKey,
            Map<String, SystemRenderData> systemRenderData,
            HpgNetworkDetail hpgNetworkDetail, double systemSize, double territoryAlpha,
            double hpgNetworkAlpha, SemanticZoomProfile semanticZoom, Stroke thick,
            Stroke dashed, List<PlanetarySystem> activeRouteSystems,
            int revealedProposedRouteSystemCount, int overscan) {
          drawRetainedCartographyLayer(graphics, atlas, factionLogoRenderKey,
              territoryAlpha, 0.0, overscan);
          drawReachability(graphics, systemSize, semanticZoom.detailedOverlayAlpha(), systemRenderData);
          drawProposedRoute(graphics, new Arc2D.Double(), systemSize,
              revealedProposedRouteSystemCount, semanticZoom.detailedOverlayAlpha());
          if (hpgNetworkAlpha > 0.0) {
            paintLayerWithAlpha(graphics, hpgNetworkAlpha,
                layerGraphics -> drawHpgNetworkLayer(layerGraphics, thick, dashed,
                    hpgNetworkDetail, false));
          }
          if (!activeRouteSystems.isEmpty()) {
            drawActiveRoute(graphics, new Arc2D.Double(), activeRouteSystems, systemSize,
                1.0, semanticZoom.detailedOverlayAlpha());
          }
        }

        private PannableRenderLayer getRetainedMapModeEndpoint(
            PannableRenderLayerCache<RetainedCartographyKey> cache, TerritoryAtlas atlas,
            TerritoryRenderKey renderKey, FactionLogoRenderKey factionLogoRenderKey,
            Map<String, SystemRenderData> systemRenderData, MapMode mapMode,
            HpgNetworkDetail hpgNetworkDetail, double systemSize,
            double factionLogoAlpha, SemanticZoomProfile semanticZoom, int overscan) {
          RenderViewKey viewKey = renderKey.viewKey();
          double endpointFactionLogoAlpha = mapMode == MapMode.FACTION ? factionLogoAlpha : 0.0;
          RetainedCartographyKey key = createRetainedCartographyKey(
              renderKey, mapMode, hpgNetworkDetail, systemSize, 0.0,
              endpointFactionLogoAlpha, 0.0, semanticZoom, factionLogoRenderKey);
          return cache.getOrRender(
              key, viewKey, overscan, layerGraphics -> {
                if (endpointFactionLogoAlpha > 0.0) {
                  paintLayerWithAlpha(layerGraphics, endpointFactionLogoAlpha,
                      logoGraphics -> drawFactionLogoLayer(logoGraphics, atlas,
                          factionLogoRenderKey, false));
                }
                drawRetainedSystemArt(layerGraphics, systemRenderData, mapMode,
                    systemSize, semanticZoom, overscan, false, true);
              });
        }

        private void paintRetainedNavigationLayer(Graphics2D graphics, TerritoryAtlas atlas,
            TerritoryRenderKey renderKey, Map<String, SystemRenderData> systemRenderData,
            MapMode mapMode, HpgNetworkDetail hpgNetworkDetail, double systemSize,
            double territoryAlpha, double factionLogoAlpha, double hpgNetworkAlpha,
            SemanticZoomProfile semanticZoom, Stroke thick, Stroke dashed,
            List<PlanetarySystem> activeRouteSystems, int revealedProposedRouteSystemCount) {
          territoryRenderCache.clear();
          factionLogoRenderCache.clear();
          retainedSystemArtRenderCache.clear();
          RenderViewKey viewKey = renderKey.viewKey();
          FactionLogoRenderKey factionLogoRenderKey = createFactionLogoRenderKey(renderKey);
          int overscan = renderLayerOverscan(viewKey.width(), viewKey.height());
          int cartographyOverscan = retainedCartographyOverscan(viewKey.width(), viewKey.height());
          if ((overscan == 0) || (cartographyOverscan == 0)) {
                        clearRetainedCartographyRenderCache();
            retainedNavigationRenderCache.clear();
            drawRetainedNavigationLayer(graphics, null, atlas, factionLogoRenderKey, systemRenderData,
                mapMode, hpgNetworkDetail, systemSize, territoryAlpha, factionLogoAlpha,
                hpgNetworkAlpha, semanticZoom, thick, dashed, activeRouteSystems,
                revealedProposedRouteSystemCount, 0);
            return;
          }

          RetainedCartographyKey cartographyKey = createRetainedCartographyKey(
              renderKey, mapMode, hpgNetworkDetail, systemSize, territoryAlpha,
              factionLogoAlpha, hpgNetworkAlpha, semanticZoom, factionLogoRenderKey);
          RetainedCartographyKey baseCartographyKey = createRetainedCartographyKey(
              renderKey, mapMode, hpgNetworkDetail, systemSize, territoryAlpha,
              0.0, hpgNetworkAlpha, semanticZoom, factionLogoRenderKey);
          PannableRenderLayer cartographyLayer = getRetainedCartographyLayer(
              baseCartographyKey, viewKey, cartographyOverscan, overscan, atlas, territoryAlpha);
          RetainedNavigationKey key = new RetainedNavigationKey(cartographyKey,
              getPathSystemIds(jumpPath), getSystemIds(activeRouteSystems), reachabilityRevision);
          PannableRenderLayer retainedLayer = retainedNavigationRenderCache.getOrRender(
              key, viewKey, overscan,
              layerGraphics -> drawRetainedNavigationLayer(layerGraphics, cartographyLayer,
                  atlas, factionLogoRenderKey,
                  systemRenderData, mapMode, hpgNetworkDetail, systemSize, territoryAlpha,
                  factionLogoAlpha, hpgNetworkAlpha, semanticZoom, thick, dashed,
                  activeRouteSystems, revealedProposedRouteSystemCount, overscan));
          drawPannableRenderLayer(graphics, retainedLayer, viewKey.width(), viewKey.height(), 1.0);
        }

        private void drawRetainedNavigationLayer(Graphics2D graphics,
            @Nullable PannableRenderLayer cartographyLayer, TerritoryAtlas atlas,
            FactionLogoRenderKey factionLogoRenderKey,
            Map<String, SystemRenderData> systemRenderData, MapMode mapMode,
            HpgNetworkDetail hpgNetworkDetail, double systemSize, double territoryAlpha,
            double factionLogoAlpha, double hpgNetworkAlpha, SemanticZoomProfile semanticZoom,
            Stroke thick, Stroke dashed, List<PlanetarySystem> activeRouteSystems,
            int revealedProposedRouteSystemCount, int overscan) {
                    long phaseStartedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                    if (cartographyLayer == null) {
                        drawRetainedCartographyLayer(graphics, atlas, factionLogoRenderKey,
                                territoryAlpha, 0.0, overscan);
                    } else {
                        graphics.drawImage(cartographyLayer.image(), cartographyLayer.drawX(),
                                cartographyLayer.drawY(), null);
                    }
                    if (factionLogoAlpha > 0.0) {
                      paintLayerWithAlpha(graphics, factionLogoAlpha,
                          layerGraphics -> drawFactionLogoLayer(layerGraphics, atlas,
                              factionLogoRenderKey, false));
                    }
                    long cartographyFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
          drawReachability(graphics, systemSize, semanticZoom.detailedOverlayAlpha(), systemRenderData);
          drawProposedRoute(graphics, new Arc2D.Double(), systemSize,
              revealedProposedRouteSystemCount, semanticZoom.detailedOverlayAlpha());
                    long routeFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
          if (hpgNetworkAlpha > 0.0) {
            paintLayerWithAlpha(graphics, hpgNetworkAlpha,
                layerGraphics -> drawHpgNetworkLayer(layerGraphics, thick, dashed,
                    hpgNetworkDetail, false));
          }
                    long hpgFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
          if (!activeRouteSystems.isEmpty()) {
            drawActiveRoute(graphics, new Arc2D.Double(), activeRouteSystems, systemSize,
                1.0, semanticZoom.detailedOverlayAlpha());
          }
                    long activeRouteFinishedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
          drawRetainedSystemArt(graphics, systemRenderData, mapMode, systemSize,
              semanticZoom, overscan);
                    if (RENDER_PROFILING_ENABLED) {
                        long systemsFinishedNanos = System.nanoTime();
                        renderPerformanceTracker.recordRetainedRender(
                                cartographyFinishedNanos - phaseStartedNanos,
                                routeFinishedNanos - cartographyFinishedNanos,
                                hpgFinishedNanos - routeFinishedNanos,
                                activeRouteFinishedNanos - hpgFinishedNanos,
                                systemsFinishedNanos - activeRouteFinishedNanos);
                    }
        }

        private static List<String> getSystemIds(List<PlanetarySystem> routeSystems) {
          return routeSystems.stream().map(PlanetarySystem::getId).toList();
        }

        private RetainedCartographyKey createRetainedCartographyKey(TerritoryRenderKey renderKey,
            MapMode mapMode, HpgNetworkDetail hpgNetworkDetail, double systemSize,
            double territoryAlpha, double factionLogoAlpha, double hpgNetworkAlpha,
            SemanticZoomProfile semanticZoom, FactionLogoRenderKey factionLogoRenderKey) {
          return new RetainedCartographyKey(
              new TerritoryDataKey(renderKey.date(), renderKey.dataRevision()), mapMode,
              hpgNetworkDetail, optEmptySystems.isSelected(), Double.doubleToLongBits(territoryAlpha),
              Double.doubleToLongBits(factionLogoAlpha), Double.doubleToLongBits(hpgNetworkAlpha),
              Double.doubleToLongBits(semanticZoom.systemContactAlpha()),
              Double.doubleToLongBits(semanticZoom.systemDetailAlpha()),
              Double.doubleToLongBits(semanticZoom.serviceAlpha()), Double.doubleToLongBits(systemSize),
              factionLogoRenderKey.majorMinimumSize(), factionLogoRenderKey.compactMinimumSize(),
              factionLogoRenderKey.maximumSize(), factionLogoRenderKey.collisionPadding(),
              factionLogoRenderKey.shadowOffset());
        }

          private void continueTerritoryLayerSettling(TerritoryAtlas atlas,
              TerritoryRenderKey renderKey, MapMode mapMode, HpgNetworkDetail hpgNetworkDetail,
              double systemSize, double territoryAlpha, double factionLogoAlpha,
              double hpgNetworkAlpha, SemanticZoomProfile semanticZoom) {
            RenderViewKey viewKey = renderKey.viewKey();
            FactionLogoRenderKey factionLogoRenderKey = createFactionLogoRenderKey(renderKey);
            RetainedCartographyKey key = createRetainedCartographyKey(renderKey, mapMode,
                hpgNetworkDetail, systemSize, territoryAlpha, 0.0,
                hpgNetworkAlpha, semanticZoom, factionLogoRenderKey);
            PannableRenderLayerSnapshot<RetainedCartographyKey> exactLayer =
                retainedCartographyRenderCache.snapshot(key);
            if ((exactLayer != null) && exactLayer.renderedView().equals(viewKey)) {
                territoryLayerSettling = false;
                mapPanel.repaint();
                return;
            }

            int overscan = retainedCartographyOverscan(viewKey.width(), viewKey.height());
            if (overscan == 0) {
                territoryLayerSettling = false;
                return;
            }
            retainedCartographyPreparationQueue.request(new RetainedCartographyRenderRequest(
                key, viewKey, overscan, atlas, territoryAlpha));
          }

        private void paintRetainedCartographyLayer(Graphics2D graphics, TerritoryAtlas atlas,
            TerritoryRenderKey renderKey,
            MapMode mapMode, HpgNetworkDetail hpgNetworkDetail, double systemSize,
                double territoryAlpha, double factionLogoAlpha, double hpgNetworkAlpha,
                SemanticZoomProfile semanticZoom) {
                    territoryRenderCache.clear();
          RenderViewKey viewKey = renderKey.viewKey();
              FactionLogoRenderKey factionLogoRenderKey = createFactionLogoRenderKey(renderKey);
                    int overscan = retainedCartographyOverscan(viewKey.width(), viewKey.height());
          if (overscan == 0) {
                        clearRetainedCartographyRenderCache();
                drawRetainedCartographyLayer(graphics, atlas,
                    factionLogoRenderKey, territoryAlpha, 0.0, 0);
                paintStaticFactionLogoLayer(graphics, atlas, factionLogoRenderKey,
                    factionLogoAlpha);
            return;
          }

          RetainedCartographyKey key = createRetainedCartographyKey(
              renderKey, mapMode, hpgNetworkDetail, systemSize, territoryAlpha,
              0.0, hpgNetworkAlpha, semanticZoom, factionLogoRenderKey);
          if (isZoomInteractionActive()) {
              paintActiveZoomCartographyLayer(graphics, key, viewKey, overscan, atlas, territoryAlpha);
              if (factionLogoAlpha > 0.0) {
                paintLayerWithAlpha(graphics, factionLogoAlpha,
                    layerGraphics -> drawFactionLogoLayer(layerGraphics, atlas,
                        factionLogoRenderKey));
              }
              return;
          }
          PannableRenderLayer retainedLayer = getRetainedCartographyLayer(
              key, viewKey, overscan, 0, atlas, territoryAlpha);
          drawPannableRenderLayer(graphics, retainedLayer, viewKey.width(), viewKey.height(), 1.0);
          paintStaticFactionLogoLayer(graphics, atlas, factionLogoRenderKey, factionLogoAlpha);
        }

        private void paintActiveZoomCartographyLayer(Graphics2D graphics, RetainedCartographyKey key,
              RenderViewKey viewKey, int overscan, TerritoryAtlas atlas, double territoryAlpha) {
            RetainedCartographyRenderRequest request = new RetainedCartographyRenderRequest(
                key, viewKey, overscan, atlas, territoryAlpha);
            PannableRenderLayer availableLayer = retainedCartographyRenderCache.getOrRefresh(
                  key, viewKey, overscan, 0,
                layerGraphics -> drawRetainedCartographyTerritory(
                    layerGraphics, atlas, viewKey, territoryAlpha, overscan));
            if (availableLayer != null) {
                drawPannableRenderLayer(graphics, availableLayer, viewKey.width(), viewKey.height(), 1.0);
                return;
            }

            PannableRenderLayerSnapshot<RetainedCartographyKey> previousLayer =
                  retainedCartographyRenderCache.snapshot();
            retainedCartographyPreparationQueue.request(request);
            if ((previousLayer != null) && previousLayer.key().dataKey().equals(key.dataKey())) {
                retainedCartographyProvisional = true;
                long snapshotStartedNanos = RENDER_PROFILING_ENABLED ? System.nanoTime() : 0L;
                drawPannableSnapshot(graphics, previousLayer, viewKey, 1.0);
                if (RENDER_PROFILING_ENABLED) {
                    renderPerformanceTracker.recordZoomSnapshotTransform(
                          System.nanoTime() - snapshotStartedNanos);
                }
            } else {
                drawRetainedCartographyTerritory(graphics, atlas, viewKey, territoryAlpha, 0);
            }
        }

        private PannableRenderLayer getRetainedCartographyLayer(RetainedCartographyKey key,
                        RenderViewKey viewKey, int overscan, int requiredMargin,
            TerritoryAtlas atlas, double territoryAlpha) {
            RetainedCartographyRenderRequest request = new RetainedCartographyRenderRequest(
                  key, viewKey, overscan, atlas, territoryAlpha);
            PannableRenderLayer availableLayer = retainedCartographyRenderCache.getOrRefresh(
                key, viewKey, overscan, requiredMargin,
                layerGraphics -> drawRetainedCartographyTerritory(
                    layerGraphics, atlas, viewKey, territoryAlpha, overscan));
            if (availableLayer != null) {
                if (retainedCartographyProvisional) {
                    retainedCartographyPreparationQueue.request(request);
                }
                return availableLayer;
            }

            PannableRenderLayerSnapshot<RetainedCartographyKey> previousLayer =
                retainedCartographyRenderCache.snapshot();
            retainedCartographyPreparationQueue.request(request);
            BufferedImage fallbackImage = new BufferedImage(
                viewKey.width() + (overscan * 2), viewKey.height() + (overscan * 2),
                BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D fallbackGraphics = fallbackImage.createGraphics();
            try {
                fallbackGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                fallbackGraphics.translate(overscan, overscan);
                if ((previousLayer != null)
                    && previousLayer.key().dataKey().equals(key.dataKey())) {
                  drawPannableSnapshot(fallbackGraphics, previousLayer, viewKey, 1.0);
                }
            } finally {
                fallbackGraphics.dispose();
            }
            retainedCartographyRenderCache.install(key, viewKey, overscan, fallbackImage);
            retainedCartographyProvisional = true;
            return new PannableRenderLayer(fallbackImage, -overscan, -overscan);
        }

        private void drawRetainedCartographyLayer(Graphics2D graphics, TerritoryAtlas atlas,
            FactionLogoRenderKey factionLogoRenderKey, double territoryAlpha,
            double factionLogoAlpha, int overscan) {
                    drawRetainedCartographyTerritory(graphics, atlas,
                            factionLogoRenderKey.territoryKey().viewKey(), territoryAlpha, overscan);
              if (factionLogoAlpha > 0.0) {
                paintLayerWithAlpha(graphics, factionLogoAlpha,
                    layerGraphics -> drawFactionLogoLayer(layerGraphics, atlas,
                        factionLogoRenderKey, false));
              }
        }

                    private static void drawRetainedCartographyTerritory(Graphics2D graphics,
                            TerritoryAtlas atlas, RenderViewKey viewKey, double territoryAlpha, int overscan) {
                        if (territoryAlpha > 0.0) {
                            paintLayerWithAlpha(graphics, territoryAlpha,
                                    layerGraphics -> drawTerritoryLayer(layerGraphics, atlas, viewKey, overscan));
                        }
                    }

          static BufferedImage renderRetainedCartographyTerritory(
              RetainedCartographyRenderRequest request) {
            int width = request.viewKey().width() + (request.overscan() * 2);
            int height = request.viewKey().height() + (request.overscan() * 2);
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.translate(request.overscan(), request.overscan());
                drawRetainedCartographyTerritory(graphics, request.atlas(), request.viewKey(),
                    request.territoryAlpha(), request.overscan());
                return image;
            } catch (RuntimeException | Error exception) {
                image.flush();
                throw exception;
            } finally {
                graphics.dispose();
            }
          }

          private void installRetainedCartography(RetainedCartographyRenderRequest request,
              BufferedImage image) {
            RenderViewKey currentView = RenderViewKey.create(mapPanel.getWidth(), mapPanel.getHeight(),
                conf.centerX, conf.centerY, conf.scale);
            TerritoryDataKey currentDataKey = new TerritoryDataKey(
                campaign.getLocalDate(), cartographyDataRevision);
            if (!request.viewKey().equals(currentView)
                || !request.key().dataKey().equals(currentDataKey)) {
                image.flush();
                mapPanel.repaint();
                return;
            }
            retainedCartographyRenderCache.install(
                request.key(), request.viewKey(), request.overscan(), image);
            retainedCartographyProvisional = false;
            retainedNavigationRenderCache.clear();
            mapPanel.repaint();
          }

            private void paintRetainedSystemArtLayer(Graphics2D graphics, TerritoryRenderKey renderKey,
                Map<String, SystemRenderData> systemRenderData, MapMode mapMode,
                HpgNetworkDetail hpgNetworkDetail, double systemSize, double territoryAlpha,
                double factionLogoAlpha, double hpgNetworkAlpha, SemanticZoomProfile semanticZoom) {
              RenderViewKey viewKey = renderKey.viewKey();
              FactionLogoRenderKey factionLogoRenderKey = createFactionLogoRenderKey(renderKey);
              int overscan = renderLayerOverscan(viewKey.width(), viewKey.height());
              if (overscan == 0) {
                retainedSystemArtRenderCache.clear();
                drawRetainedSystemArt(graphics, systemRenderData, mapMode, systemSize, semanticZoom, 0);
                return;
              }

              RetainedCartographyKey key = new RetainedCartographyKey(
                  new TerritoryDataKey(renderKey.date(), renderKey.dataRevision()), mapMode,
                  hpgNetworkDetail, optEmptySystems.isSelected(), 0L, 0L, 0L,
                  Double.doubleToLongBits(semanticZoom.systemContactAlpha()),
                  Double.doubleToLongBits(semanticZoom.systemDetailAlpha()),
                  Double.doubleToLongBits(semanticZoom.serviceAlpha()), Double.doubleToLongBits(systemSize),
                  factionLogoRenderKey.majorMinimumSize(), factionLogoRenderKey.compactMinimumSize(),
                  factionLogoRenderKey.maximumSize(), factionLogoRenderKey.collisionPadding(),
                  factionLogoRenderKey.shadowOffset());
              PannableRenderLayer retainedLayer = retainedSystemArtRenderCache.getOrRender(
                  key, viewKey, overscan,
                  layerGraphics -> drawRetainedSystemArt(layerGraphics, systemRenderData, mapMode,
                      systemSize, semanticZoom, overscan));
              drawPannableRenderLayer(graphics, retainedLayer, viewKey.width(), viewKey.height(), 1.0);
            }

        private void drawRetainedSystemArt(Graphics2D graphics,
            Map<String, SystemRenderData> systemRenderData, MapMode mapMode,
            double systemSize, SemanticZoomProfile semanticZoom, int overscan) {
          drawRetainedSystemArt(graphics, systemRenderData, mapMode, systemSize,
              semanticZoom, overscan, true, true);
        }

        private void drawRetainedSystemArt(Graphics2D graphics,
            Map<String, SystemRenderData> systemRenderData, MapMode mapMode,
            double systemSize, SemanticZoomProfile semanticZoom, int overscan,
            boolean drawIntrinsicArt, boolean drawMapModeArt) {
                    Shape renderClip = graphics.getClip();
                    double renderExtent = Math.max(8.0, (systemSize * 2.4) + 3.0);
          RenderViewKey viewKey = RenderViewKey.create(
              mapPanel.getWidth(), mapPanel.getHeight(), conf.centerX, conf.centerY, conf.scale);
          MapQueryBounds queryBounds = retainedSystemQueryBounds(viewKey, overscan, renderExtent);
          Arc2D.Double arc = new Arc2D.Double();
          for (PlanetarySystem system : systemSpatialIndex.query(
              queryBounds.minX(), queryBounds.minY(), queryBounds.maxX(), queryBounds.maxY(), null, null)) {
            SystemRenderData renderData = systemRenderData.get(system.getId());
            if (!shouldRenderSystem(true, renderData.empty(), optEmptySystems.isSelected(), false)) {
                continue;
            }
            double x = map2scrX(system.getX());
            double y = map2scrY(system.getY());
                        if ((renderClip != null) && !renderClip.intersects(
                                    x - renderExtent, y - renderExtent, renderExtent * 2.0, renderExtent * 2.0)) {
                                continue;
                        }
            SystemMarkerLayout layout = SystemMarkerLayout.create(
                x, y, systemSize, RouteMarkerState.NONE, false, false);
            if (drawIntrinsicArt && (semanticZoom.systemDetailAlpha() > 0.0)) {
                drawIntrinsicStar(graphics, arc, system, x, y, systemSize,
                    semanticZoom.systemDetailAlpha());
            }
            if (drawMapModeArt) {
              drawSystemMapModeArt(graphics, arc, system, renderData, layout, mapMode,
                  semanticZoom.systemContactAlpha(), semanticZoom.systemDetailAlpha(),
                  semanticZoom.serviceAlpha(), optEmptySystems.isSelected());
            }
          }
        }

    private void paintStaticFactionLogoLayer(Graphics2D graphics, TerritoryAtlas atlas,
          FactionLogoRenderKey renderKey, double alpha) {
        RenderViewKey viewKey = renderKey.territoryKey().viewKey();
        if (!canCacheRenderLayer(viewKey.width(), viewKey.height())) {
            factionLogoRenderCache.clear();
            paintLayerWithAlpha(graphics, alpha,
                  layerGraphics -> drawFactionLogoLayer(layerGraphics, atlas, renderKey));
            return;
        }
        BufferedImage factionLogos = factionLogoRenderCache.getOrRender(renderKey,
              viewKey.width(), viewKey.height(),
              layerGraphics -> drawFactionLogoLayer(layerGraphics, atlas, renderKey));
        drawRenderLayer(graphics, factionLogos, alpha);
    }

    static boolean canCacheRenderLayer(int width, int height) {
        return (width > 0) && (height > 0) && ((long) width * height <= MAX_CACHED_RENDER_LAYER_PIXELS);
    }

    static int renderLayerOverscan(int width, int height) {
        if (!canCacheRenderLayer(width, height)) {
            return 0;
        }
        int overscan = Math.min(512, Math.max(64, Math.min(width, height) / 3));
        while ((overscan > 0) && !canCacheRenderLayer(width + (overscan * 2), height + (overscan * 2))) {
            overscan /= 2;
        }
        return overscan;
    }

    static int retainedCartographyOverscan(int width, int height) {
        if (!canCacheRenderLayer(width, height)) {
            return 0;
        }
        int overscan = RETAINED_CARTOGRAPHY_MAX_OVERSCAN;
        while ((overscan > 0) && !canCacheRenderLayer(width + (overscan * 2), height + (overscan * 2))) {
            overscan /= 2;
        }
        return overscan;
    }

    private FactionLogoRenderKey createFactionLogoRenderKey(TerritoryRenderKey territoryRenderKey) {
        return new FactionLogoRenderKey(territoryRenderKey,
              Math.max(1, UIUtil.scaleForGUI(FACTION_LOGO_MIN_SIZE)),
              Math.max(1, UIUtil.scaleForGUI(FACTION_LOGO_COMPACT_MIN_SIZE)),
              Math.max(1, UIUtil.scaleForGUI(FACTION_LOGO_MAX_SIZE)),
              Math.max(1, UIUtil.scaleForGUI(FACTION_LOGO_COLLISION_PADDING)),
              Math.max(1, UIUtil.scaleForGUI(2)));
    }

    private void drawTerritoryLayer(Graphics2D graphics, TerritoryAtlas atlas) {
        drawTerritoryLayer(graphics, atlas, 0);
    }

    private void drawTerritoryLayer(Graphics2D graphics, TerritoryAtlas atlas, int overscan) {
        RenderViewKey viewKey = RenderViewKey.create(
              mapPanel.getWidth(), mapPanel.getHeight(), conf.centerX, conf.centerY, conf.scale);
        drawTerritoryLayer(graphics, atlas, viewKey, overscan);
    }

    static void drawTerritoryLayer(Graphics2D graphics, TerritoryAtlas atlas,
          RenderViewKey viewKey, int overscan) {
        double scale = Double.longBitsToDouble(viewKey.scaleBits());
        double centerX = Double.longBitsToDouble(viewKey.centerXBits());
        double centerY = Double.longBitsToDouble(viewKey.centerYBits());
        double visibleMinX = (-overscan - (viewKey.width() / 2.0)) / scale - centerX;
        double visibleMaxX = (viewKey.width() + overscan - (viewKey.width() / 2.0)) / scale - centerX;
        double visibleMinY = ((viewKey.height() / 2.0) - viewKey.height() - overscan) / scale + centerY;
        double visibleMaxY = ((viewKey.height() / 2.0) + overscan) / scale + centerY;
        AffineTransform mapToScreen = new AffineTransform();
        mapToScreen.translate(viewKey.width() / 2.0, viewKey.height() / 2.0);
        mapToScreen.scale(scale, -scale);
        mapToScreen.translate(centerX, -centerY);
        TerritoryVisualProfile visualProfile = TerritoryVisualProfile.create(scale);
        for (TerritoryContour contour : atlas.contours()) {
            if ((contour.maxMapX() < visibleMinX) || (contour.minMapX() > visibleMaxX)
                  || (contour.maxMapY() < visibleMinY) || (contour.minMapY() > visibleMaxY)) {
                continue;
            }
            paintTerritoryContour(graphics, contour, mapToScreen, visualProfile);
        }
    }

    static void paintTerritoryContour(Graphics2D graphics, TerritoryContour contour,
          AffineTransform mapToScreen, TerritoryVisualProfile visualProfile) {
        if (contour.semantic() == TerritorySemantic.UNCLAIMED_EXTERIOR) {
            return;
        }

        Graphics2D territoryGraphics = (Graphics2D) graphics.create();
        try {
            territoryGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);
            Shape screenShape = mapToScreen.createTransformedShape(contour.shape());
                        Shape renderClip = graphics.getClip();
                        Rectangle2D screenBounds = screenShape.getBounds2D();
                        double paintMargin = 4.0;
                        if ((renderClip != null) && !renderClip.intersects(
                                    screenBounds.getX() - paintMargin, screenBounds.getY() - paintMargin,
                                    screenBounds.getWidth() + (paintMargin * 2.0),
                                    screenBounds.getHeight() + (paintMargin * 2.0))) {
                                return;
                        }
            if (contour.semantic() == TerritorySemantic.UNCLAIMED_POCKET) {
                territoryGraphics.setPaint(TERRITORY_POCKET_FILL);
            } else if (contour.semantic() == TerritorySemantic.DISPUTED) {
                territoryGraphics.setPaint(createDisputedTerritoryPaint(
                        contour.factions(), visualProfile.secondaryDetailAlpha(),
                        visualProfile.disputedBandWidth()));
            } else {
                territoryGraphics.setPaint(contour.paint());
            }
            territoryGraphics.fill(screenShape);

            switch (contour.semantic()) {
                case SOVEREIGN -> drawSovereignBoundary(territoryGraphics, screenShape,
                      contour.factions().getFirst().getColor());
                case DISPUTED -> drawDisputedTerritory(territoryGraphics, screenShape, contour.factions(),
                      visualProfile.secondaryDetailAlpha());
                case UNCLAIMED_POCKET -> drawUnclaimedPocketBoundary(territoryGraphics, screenShape,
                      visualProfile.secondaryDetailAlpha());
                case ENCLAVE -> drawEnclaveBoundary(territoryGraphics, screenShape,
                      contour.factions().getFirst().getColor(), visualProfile.secondaryDetailAlpha());
                case UNCLAIMED_EXTERIOR -> {
                }
            }
        } finally {
            territoryGraphics.dispose();
        }
    }

    private static void drawSovereignBoundary(Graphics2D graphics, Shape shape, Color factionColor) {
        graphics.setPaint(TERRITORY_BORDER_DARK);
        graphics.setStroke(new BasicStroke(3.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shape);
        graphics.setPaint(withAlpha(factionColor, 205));
        graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shape);
    }

    private static void drawDisputedTerritory(Graphics2D graphics, Shape shape, List<Faction> factions,
          double secondaryDetailAlpha) {
        graphics.setPaint(TERRITORY_BORDER_DARK);
        graphics.setStroke(new BasicStroke(3.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shape);
        graphics.setPaint(TERRITORY_NEUTRAL_EDGE);
        graphics.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0,
              new float[] { 7.0f, 4.0f }, 0));
        graphics.draw(shape);
    }

        static Paint createDisputedTerritoryPaint(List<Faction> factions, double detailAlpha,
            double bandWidth) {
        if (factions.isEmpty()) {
            return new Color(0.0f, 0.0f, 0.0f, 0.25f);
        }

        float[] average = new float[3];
        for (Faction faction : factions) {
            float[] components = faction.getColor().getRGBColorComponents(null);
            average[0] += components[0] / factions.size();
            average[1] += components[1] / factions.size();
            average[2] += components[2] / factions.size();
        }

        int factionCount = factions.size();
        float[] fractions = new float[factionCount * 2];
        Color[] colors = new Color[factionCount * 2];
        float transitionWidth = 0.001f;
        for (int factionIndex = 0; factionIndex < factionCount; factionIndex++) {
            float start = (float) factionIndex / factionCount;
            float end = (float) (factionIndex + 1) / factionCount;
            fractions[factionIndex * 2] = factionIndex == 0 ? 0.0f : start + transitionWidth;
            fractions[factionIndex * 2 + 1] = factionIndex == factionCount - 1
                  ? 1.0f : end - transitionWidth;
            float[] faction = factions.get(factionIndex).getColor().getRGBColorComponents(null);
            Color bandColor = new Color(
                  (float) interpolate(average[0], faction[0], detailAlpha),
                  (float) interpolate(average[1], faction[1], detailAlpha),
                  (float) interpolate(average[2], faction[2], detailAlpha), 0.25f);
            colors[factionIndex * 2] = bandColor;
            colors[factionIndex * 2 + 1] = bandColor;
        }

        double period = bandWidth * factionCount;
        return new LinearGradientPaint(new Point2D.Double(0.0, 0.0),
              new Point2D.Double(period * Math.cos(Math.PI / 6.0), period * Math.sin(Math.PI / 6.0)),
              fractions, colors, CycleMethod.REPEAT);
    }

    private static void drawUnclaimedPocketBoundary(Graphics2D graphics, Shape shape, double detailAlpha) {
        if (detailAlpha <= 0.0) {
            return;
        }
        graphics.setComposite(deriveCompositeWithAlpha(graphics.getComposite(), detailAlpha));
        graphics.setPaint(TERRITORY_NEUTRAL_EDGE);
        graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
              new float[] { 1.0f, 5.0f }, 0));
        graphics.draw(shape);
    }

    private static void drawEnclaveBoundary(Graphics2D graphics, Shape shape, Color factionColor,
          double detailAlpha) {
        drawSovereignBoundary(graphics, shape, factionColor);
        if (detailAlpha <= 0.0) {
            return;
        }
        graphics.setComposite(deriveCompositeWithAlpha(graphics.getComposite(), detailAlpha));
        graphics.setPaint(withAlpha(factionColor, 180));
        graphics.setStroke(new BasicStroke(5.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shape);
        graphics.setPaint(TERRITORY_BORDER_DARK);
        graphics.setStroke(new BasicStroke(2.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shape);
        graphics.setPaint(withAlpha(factionColor, 225));
        graphics.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(shape);
    }

    private void prepareStaticCartography() {
        if (campaign != null) {
            prepareStaticCartography(campaign.getLocalDate());
        }
    }

    private void prepareStaticCartography(LocalDate date) {
        TerritoryDataKey requestedKey = new TerritoryDataKey(date, cartographyDataRevision);
        if (preparedTerritoryAtlas.get(requestedKey) != null) {
            return;
        }

        TerritoryDataKey previousKey = preparedTerritoryAtlas.getKey();
        if ((previousKey != null) && (previousKey.date().getYear() != date.getYear())) {
            clearFactionLogoImages();
        }
        preparedTerritoryAtlas.prepare(requestedKey, () -> buildTerritoryAtlas(date));
        territoryRenderCache.clear();
        clearRetainedCartographyRenderCache();
        retainedSystemArtRenderCache.clear();
        retainedNavigationRenderCache.clear();
        clearMapModeTransitionRenderCaches();
        factionLogoRenderCache.clear();
    }

    private @Nullable TerritoryAtlas getPreparedTerritoryAtlas(LocalDate date) {
        TerritoryDataKey requestedKey = new TerritoryDataKey(date, cartographyDataRevision);
        TerritoryAtlas atlas = preparedTerritoryAtlas.get(requestedKey);
        if (atlas == null) {
            scheduleTerritoryPreparation(requestedKey);
        }
        return atlas;
    }

    private Map<String, SystemRenderData> getPreparedSystemRenderData(LocalDate date) {
        SystemRenderDataKey key = new SystemRenderDataKey(date, cartographyDataRevision);
        return preparedSystemRenderData.prepare(key, () -> {
            Map<Faction, String> capitals = new HashMap<>();
            Faction mercenaryFaction = null;
            String mercenaryCapitalSystem = null;
            for (Faction faction : Factions.getInstance().getFactions()) {
                String capitalSystem = faction.getStartingPlanet(date);
                capitals.put(faction, capitalSystem);
                if ((mercenaryFaction == null) && "MERC".equals(faction.getShortName())) {
                    mercenaryFaction = faction;
                    mercenaryCapitalSystem = capitalSystem;
                }
            }
            Map<String, SystemRenderData> renderData = new HashMap<>();
            for (PlanetarySystem system : systems) {
                renderData.put(system.getId(), SystemRenderData.create(system, date, capitals,
                      mercenaryFaction, mercenaryCapitalSystem));
            }
            return Map.copyOf(renderData);
        });
    }

    private double getMaximumSystemLabelWidth(Graphics2D graphics,
          Map<String, SystemRenderData> systemRenderData) {
        Font font = graphics.getFont();
        FontRenderContext fontRenderContext = graphics.getFontRenderContext();
        if ((systemRenderData == systemLabelWidthRenderData)
              && font.equals(systemLabelWidthFont)
              && fontRenderContext.equals(systemLabelWidthFontRenderContext)) {
            return maximumSystemLabelWidth;
        }

        FontMetrics metrics = graphics.getFontMetrics(font);
        double maximumWidth = 0.0;
        for (SystemRenderData renderData : systemRenderData.values()) {
            double width = metrics.stringWidth(renderData.printableName());
            if (renderData.stellarDetail() != null) {
                width += metrics.stringWidth(renderData.stellarDetail());
            }
            maximumWidth = Math.max(maximumWidth, width);
        }
        systemLabelWidthRenderData = systemRenderData;
        systemLabelWidthFont = font;
        systemLabelWidthFontRenderContext = fontRenderContext;
        maximumSystemLabelWidth = maximumWidth + 1.0;
        return maximumSystemLabelWidth;
    }

    private void scheduleTerritoryPreparation(TerritoryDataKey requestedKey) {
        territoryPreparationQueue.request(requestedKey);
    }

    void requestStaticCartographyPreparation() {
        if (campaign != null) {
            scheduleTerritoryPreparation(new TerritoryDataKey(campaign.getLocalDate(), cartographyDataRevision));
        }
    }

    private void prepareRequestedStaticCartography(TerritoryDataKey requestedKey) {
        if ((campaign == null) || (requestedKey.dataRevision() != cartographyDataRevision)
              || !requestedKey.date().equals(campaign.getLocalDate())) {
            return;
        }
        prepareStaticCartography(requestedKey.date());
        mapPanel.repaint();
    }

    void clearRenderLayerCaches() {
        backgroundRenderCache.clear();
        territoryRenderCache.clear();
        clearRetainedCartographyRenderCache();
        retainedSystemArtRenderCache.clear();
        retainedNavigationRenderCache.clear();
        clearMapModeTransitionRenderCaches();
        factionLogoRenderCache.clear();
    }

    private void clearRetainedCartographyRenderCache() {
        retainedCartographyPreparationQueue.cancel();
        retainedCartographyRenderCache.clear();
        retainedCartographyProvisional = false;
    }

    private void clearMapModeTransitionRenderCaches() {
        mapModeTransitionBaseRenderCache.clear();
        mapModeTransitionSystemRenderCache.clear();
        previousMapModeRenderCache.clear();
        targetMapModeRenderCache.clear();
    }

    RenderCacheDiagnostics getRenderCacheDiagnostics() {
        return new RenderCacheDiagnostics(backgroundRenderCache.getRenderCount(),
              territoryRenderCache.getRenderCount() + retainedCartographyRenderCache.getRenderCount()
                  + retainedSystemArtRenderCache.getRenderCount()
                  + retainedNavigationRenderCache.getRenderCount()
                  + mapModeTransitionBaseRenderCache.getRenderCount()
                  + mapModeTransitionSystemRenderCache.getRenderCount()
                  + previousMapModeRenderCache.getRenderCount()
                  + targetMapModeRenderCache.getRenderCount(),
              factionLogoRenderCache.getRenderCount(),
              preparedTerritoryAtlas.getPreparationCount(), backgroundRenderCache.hasImage(),
              territoryRenderCache.hasImage() || retainedCartographyRenderCache.hasImage()
                  || retainedSystemArtRenderCache.hasImage() || retainedNavigationRenderCache.hasImage()
                  || mapModeTransitionBaseRenderCache.hasImage()
                  || mapModeTransitionSystemRenderCache.hasImage()
                  || previousMapModeRenderCache.hasImage() || targetMapModeRenderCache.hasImage(),
              factionLogoRenderCache.hasImage());
    }

    private TerritoryAtlas buildTerritoryAtlas(LocalDate date) {
        if (systems.isEmpty()) {
            return new TerritoryAtlas(date, 0, -1, 0, -1, Map.of(), List.of(), List.of());
        }

        double systemMinX = systems.stream().mapToDouble(PlanetarySystem::getX).min().orElse(0.0);
        double systemMaxX = systems.stream().mapToDouble(PlanetarySystem::getX).max().orElse(0.0);
        double systemMinY = systems.stream().mapToDouble(PlanetarySystem::getY).min().orElse(0.0);
        double systemMaxY = systems.stream().mapToDouble(PlanetarySystem::getY).max().orElse(0.0);
        int minColumn = (int) Math.floor(systemMinX / TERRITORY_HEX_SPACING_X) - TERRITORY_HEX_MARGIN;
        int maxColumn = (int) Math.ceil(systemMaxX / TERRITORY_HEX_SPACING_X) + TERRITORY_HEX_MARGIN;
        int minRow = (int) Math.floor(systemMinY / TERRITORY_HEX_SIZE) - TERRITORY_HEX_MARGIN;
        int maxRow = (int) Math.ceil(systemMaxY / TERRITORY_HEX_SIZE) + TERRITORY_HEX_MARGIN;
        Faction independentFaction = Factions.getInstance().getFaction("IND");
        Map<TerritoryHex, TerritoryCell> cells = new HashMap<>();

        for (int column = minColumn; column <= maxColumn; column++) {
            for (int row = minRow; row <= maxRow; row++) {
                TerritoryHex hex = new TerritoryHex(column, row);
                double centerX = column * TERRITORY_HEX_SPACING_X;
                double centerY = row * TERRITORY_HEX_SIZE
                      + (column % 2) * TERRITORY_HEX_SIZE / 2.0;
                List<Faction> factions = classifyTerritoryHex(centerX, centerY, date, independentFaction);
                cells.put(hex, new TerritoryCell(hex, centerX, centerY, factions));
            }
        }
        List<TerritoryContour> contours = buildTerritoryContours(cells);
        List<TerritoryComponent> components = buildTerritoryComponents(cells);
        return new TerritoryAtlas(date, minColumn, maxColumn, minRow, maxRow, cells, contours, components);
    }

    static List<TerritoryContour> buildTerritoryContours(Map<TerritoryHex, TerritoryCell> cells) {
        List<TerritoryCell> orderedCells = cells.values().stream()
              .sorted(Comparator.comparingInt((TerritoryCell cell) -> cell.hex().column())
                    .thenComparingInt(cell -> cell.hex().row()))
              .toList();
        Set<TerritoryHex> visited = new HashSet<>();
        List<TerritoryContour> contours = new ArrayList<>();

        for (TerritoryCell seed : orderedCells) {
            if (!visited.add(seed.hex())) {
                continue;
            }
            List<TerritoryCell> regionCells = new ArrayList<>();
            List<TerritoryHex> pending = new ArrayList<>();
            pending.add(seed.hex());
            for (int pendingIndex = 0; pendingIndex < pending.size(); pendingIndex++) {
                TerritoryHex hex = pending.get(pendingIndex);
                TerritoryCell cell = cells.get(hex);
                regionCells.add(cell);
                for (TerritoryHex neighborHex : getTerritoryNeighbors(hex)) {
                    TerritoryCell neighbor = cells.get(neighborHex);
                    if ((neighbor != null) && seed.factions().equals(neighbor.factions())
                          && visited.add(neighborHex)) {
                        pending.add(neighborHex);
                    }
                }
            }

            GeneralPath mergedCells = new GeneralPath();
            GeneralPath cellPath = new GeneralPath();
            for (TerritoryCell cell : regionCells) {
                setupHexPath(cellPath, cell.centerX(), cell.centerY(), TERRITORY_HEX_SIZE / 2.0);
                mergedCells.append(cellPath, false);
            }
            Area contourArea = new Area(mergedCells);
            contourArea.add(new Area(TERRITORY_CONTOUR_SOFTENING_STROKE.createStrokedShape(contourArea)));
            Rectangle2D bounds = contourArea.getBounds2D();
            TerritorySemantic semantic = classifyTerritorySemantic(seed.factions(), regionCells, cells);
            contours.add(new TerritoryContour(List.copyOf(seed.factions()), semantic, contourArea,
                  createTerritoryPaint(seed.factions()), regionCells.size(), bounds.getMinX(), bounds.getMaxX(),
                  bounds.getMinY(), bounds.getMaxY()));
        }
        return List.copyOf(contours);
    }

    private static TerritorySemantic classifyTerritorySemantic(List<Faction> factions,
          List<TerritoryCell> regionCells, Map<TerritoryHex, TerritoryCell> cells) {
        if (factions.size() > 1) {
            return TerritorySemantic.DISPUTED;
        }

        Set<TerritoryHex> regionHexes = new HashSet<>();
        for (TerritoryCell cell : regionCells) {
            regionHexes.add(cell.hex());
        }
        boolean touchesAtlasEdge = regionCells.stream()
              .flatMap(cell -> getTerritoryNeighbors(cell.hex()).stream())
              .anyMatch(neighborHex -> !cells.containsKey(neighborHex));
        if (factions.isEmpty()) {
            return touchesAtlasEdge ? TerritorySemantic.UNCLAIMED_EXTERIOR
                  : TerritorySemantic.UNCLAIMED_POCKET;
        }
        if (touchesAtlasEdge) {
            return TerritorySemantic.SOVEREIGN;
        }

        Faction owner = factions.getFirst();
        Faction surroundingOwner = null;
        for (TerritoryCell cell : regionCells) {
            for (TerritoryHex neighborHex : getTerritoryNeighbors(cell.hex())) {
                if (regionHexes.contains(neighborHex)) {
                    continue;
                }
                TerritoryCell neighbor = cells.get(neighborHex);
                if ((neighbor == null) || (neighbor.factions().size() != 1)
                      || owner.equals(neighbor.factions().getFirst())) {
                    return TerritorySemantic.SOVEREIGN;
                }
                if (surroundingOwner == null) {
                    surroundingOwner = neighbor.factions().getFirst();
                } else if (!surroundingOwner.equals(neighbor.factions().getFirst())) {
                    return TerritorySemantic.SOVEREIGN;
                }
            }
        }
        return surroundingOwner == null ? TerritorySemantic.SOVEREIGN : TerritorySemantic.ENCLAVE;
    }

    private static Paint createTerritoryPaint(List<Faction> factions) {
        if (factions.isEmpty()) {
            return new Color(0.0f, 0.0f, 0.0f, 0.25f);
        }
        if (factions.size() == 1) {
            Color factionColor = factions.getFirst().getColor();
            float[] colorComponents = factionColor.getComponents(null);
            return new Color(colorComponents[0], colorComponents[1], colorComponents[2], 0.25f);
        }

        float red = 0.0f;
        float green = 0.0f;
        float blue = 0.0f;
        for (Faction faction : factions) {
            float[] colorComponents = faction.getColor().getRGBColorComponents(null);
            red += colorComponents[0];
            green += colorComponents[1];
            blue += colorComponents[2];
        }
        float factionCount = factions.size();
        return new Color(red / factionCount, green / factionCount, blue / factionCount, 0.25f);
    }

    private List<Faction> classifyTerritoryHex(double centerX, double centerY, LocalDate date,
          Faction independentFaction) {
        GeneralPath path = new GeneralPath();
        setupHexPath(path, centerX, centerY, TERRITORY_HEX_SIZE / 2.0);
        List<PlanetarySystem> nearbySystems = Systems.getInstance().getNearbySystems(centerX, centerY,
              (int) Math.round(TERRITORY_HEX_SIZE * 1.3));
        Set<Faction> hexFactions = new HashSet<>();

        for (PlanetarySystem system : nearbySystems) {
            if (!isSystemEmpty(system, date) && path.contains(system.getX(), system.getY())) {
                Set<Faction> factions = new HashSet<>(system.getFactionSet(date));
                factions.remove(independentFaction);
                hexFactions.addAll(factions);
            }
        }
        if (hexFactions.isEmpty()) {
            for (PlanetarySystem system : nearbySystems) {
                if (!isSystemEmpty(system, date)) {
                    hexFactions.addAll(new HashSet<>(system.getFactionSet(date)));
                }
            }
        }
        if (hexFactions.size() > 1) {
            hexFactions.remove(independentFaction);
        }

        return hexFactions.stream()
              .sorted(Comparator.comparing(Faction::getShortName))
              .toList();
    }

    private List<TerritoryComponent> buildTerritoryComponents(Map<TerritoryHex, TerritoryCell> cells) {
        List<TerritoryCell> singleOwnerCells = cells.values().stream()
              .filter(cell -> cell.factions().size() == 1)
              .sorted(Comparator.comparingInt((TerritoryCell cell) -> cell.hex().column())
                    .thenComparingInt(cell -> cell.hex().row()))
              .toList();
        Set<TerritoryHex> visited = new HashSet<>();
        List<TerritoryComponent> components = new ArrayList<>();

        for (TerritoryCell seed : singleOwnerCells) {
            if (!visited.add(seed.hex())) {
                continue;
            }
            Faction faction = seed.factions().getFirst();
            List<TerritoryCell> componentCells = new ArrayList<>();
            List<TerritoryHex> pending = new ArrayList<>();
            pending.add(seed.hex());
            for (int pendingIndex = 0; pendingIndex < pending.size(); pendingIndex++) {
                TerritoryHex hex = pending.get(pendingIndex);
                TerritoryCell cell = cells.get(hex);
                componentCells.add(cell);
                for (TerritoryHex neighborHex : getTerritoryNeighbors(hex)) {
                    TerritoryCell neighbor = cells.get(neighborHex);
                    if ((neighbor != null) && (neighbor.factions().size() == 1)
                          && faction.equals(neighbor.factions().getFirst()) && visited.add(neighborHex)) {
                        pending.add(neighborHex);
                    }
                }
            }
            components.add(createTerritoryComponent(faction, componentCells));
        }
        return List.copyOf(components);
    }

    private TerritoryComponent createTerritoryComponent(Faction faction, List<TerritoryCell> cells) {
        Set<TerritoryHex> componentHexes = new HashSet<>();
        double centroidX = 0.0;
        double centroidY = 0.0;
        double minMapX = Double.POSITIVE_INFINITY;
        double maxMapX = Double.NEGATIVE_INFINITY;
        double minMapY = Double.POSITIVE_INFINITY;
        double maxMapY = Double.NEGATIVE_INFINITY;
        for (TerritoryCell cell : cells) {
            componentHexes.add(cell.hex());
            centroidX += cell.centerX();
            centroidY += cell.centerY();
            minMapX = Math.min(minMapX, cell.centerX() - TERRITORY_HEX_RADIUS);
            maxMapX = Math.max(maxMapX, cell.centerX() + TERRITORY_HEX_RADIUS);
            minMapY = Math.min(minMapY, cell.centerY() - TERRITORY_HEX_SIZE / 2.0);
            maxMapY = Math.max(maxMapY, cell.centerY() + TERRITORY_HEX_SIZE / 2.0);
        }
        centroidX /= cells.size();
        centroidY /= cells.size();

        Map<TerritoryHex, Integer> boundaryDistance = new HashMap<>();
        List<TerritoryHex> pending = new ArrayList<>();
        for (TerritoryCell cell : cells) {
            boolean boundary = getTerritoryNeighbors(cell.hex()).stream()
                  .anyMatch(neighbor -> !componentHexes.contains(neighbor));
            if (boundary) {
                boundaryDistance.put(cell.hex(), 0);
                pending.add(cell.hex());
            }
        }
        for (int pendingIndex = 0; pendingIndex < pending.size(); pendingIndex++) {
            TerritoryHex hex = pending.get(pendingIndex);
            int nextDistance = boundaryDistance.get(hex) + 1;
            for (TerritoryHex neighbor : getTerritoryNeighbors(hex)) {
                if (componentHexes.contains(neighbor) && !boundaryDistance.containsKey(neighbor)) {
                    boundaryDistance.put(neighbor, nextDistance);
                    pending.add(neighbor);
                }
            }
        }

        TerritoryCell anchor = null;
        int anchorDepth = -1;
        double anchorCentroidDistance = Double.POSITIVE_INFINITY;
        for (TerritoryCell cell : cells) {
            int depth = boundaryDistance.getOrDefault(cell.hex(), 0);
            double centroidDistance = Point2D.distanceSq(cell.centerX(), cell.centerY(), centroidX, centroidY);
            if ((depth > anchorDepth)
                  || ((depth == anchorDepth) && (centroidDistance < anchorCentroidDistance))
                  || ((depth == anchorDepth) && (centroidDistance == anchorCentroidDistance)
                        && isHexBefore(cell.hex(), anchor.hex()))) {
                anchor = cell;
                anchorDepth = depth;
                anchorCentroidDistance = centroidDistance;
            }
        }
        return new TerritoryComponent(faction, anchor.hex(), anchor.centerX(), anchor.centerY(), cells.size(),
              anchorDepth, minMapX, maxMapX, minMapY, maxMapY);
    }

    private static boolean isHexBefore(TerritoryHex first, TerritoryHex second) {
        return (first.column() < second.column())
              || ((first.column() == second.column()) && (first.row() < second.row()));
    }

    private static List<TerritoryHex> getTerritoryNeighbors(TerritoryHex hex) {
        int column = hex.column();
        int row = hex.row();
        int leftColumn = column - 1;
        int rightColumn = column + 1;
        double currentOffset = (column % 2) / 2.0;
        double leftOffset = (leftColumn % 2) / 2.0;
        double rightOffset = (rightColumn % 2) / 2.0;
        int leftLowerRow = row + (int) Math.round(currentOffset - leftOffset - 0.5);
        int leftUpperRow = row + (int) Math.round(currentOffset - leftOffset + 0.5);
        int rightLowerRow = row + (int) Math.round(currentOffset - rightOffset - 0.5);
        int rightUpperRow = row + (int) Math.round(currentOffset - rightOffset + 0.5);
        return List.of(new TerritoryHex(leftColumn, leftLowerRow), new TerritoryHex(leftColumn, leftUpperRow),
              new TerritoryHex(column, row - 1), new TerritoryHex(column, row + 1),
              new TerritoryHex(rightColumn, rightLowerRow), new TerritoryHex(rightColumn, rightUpperRow));
    }

    private double getFactionMapModeAlpha() {
        if (!mapModeAnimating) {
            return targetMapMode == MapMode.FACTION ? 1.0 : 0.0;
        }
        double previousAlpha = previousMapMode == MapMode.FACTION ? 1.0 - mapModeAnimationProgress : 0.0;
        double targetAlpha = targetMapMode == MapMode.FACTION ? mapModeAnimationProgress : 0.0;
        return previousAlpha + targetAlpha;
    }

    private void drawFactionLogoLayer(Graphics2D graphics, TerritoryAtlas atlas,
          FactionLogoRenderKey renderKey) {
          drawFactionLogoLayer(graphics, atlas, renderKey, true);
        }

        private void drawFactionLogoLayer(Graphics2D graphics, TerritoryAtlas atlas,
            FactionLogoRenderKey renderKey, boolean cullToViewport) {
        int majorMinimumLogoSize = renderKey.majorMinimumSize();
        int compactMinimumLogoSize = renderKey.compactMinimumSize();
        int maximumLogoSize = Math.max(majorMinimumLogoSize, renderKey.maximumSize());
        int collisionPadding = renderKey.collisionPadding();
        double projectedCellArea = TERRITORY_HEX_SPACING_X * TERRITORY_HEX_SIZE
              * conf.scale * conf.scale;
        List<FactionLogoCandidate> candidates = new ArrayList<>();

        for (TerritoryComponent component : atlas.components()) {
            int priority = getFactionLogoPriority(component.faction());
            if (priority < 0) {
                continue;
            }
            double anchorX = map2scrX(component.anchorX());
            double anchorY = map2scrY(component.anchorY());

            double projectedArea = component.cellCount() * projectedCellArea;
            int minimumLogoSize = priority == 0 ? majorMinimumLogoSize : compactMinimumLogoSize;
            double minimumAreaFactor = switch (priority) {
                case 0 -> 4.0;
                case 1 -> 2.25;
                default -> 3.0;
            };
            double minimumExtentFactor = switch (priority) {
                case 0 -> 1.25;
                case 1 -> 0.9;
                default -> 1.0;
            };
            double minimumProjectedArea = minimumLogoSize * minimumLogoSize * minimumAreaFactor;
            double projectedWidth = (component.maxMapX() - component.minMapX()) * conf.scale;
            double projectedHeight = (component.maxMapY() - component.minMapY()) * conf.scale;
            if ((projectedArea < minimumProjectedArea) || (projectedWidth < minimumLogoSize * minimumExtentFactor)
                || (projectedHeight < minimumLogoSize * minimumExtentFactor)) {
                continue;
            }

            double desiredSize = Math.sqrt(projectedArea) * (priority == 0 ? 0.5 : 0.65);
            double containmentFactor = priority == 0 ? 0.68 : 0.85;
            double containedSize = Math.min(projectedWidth * containmentFactor,
                projectedHeight * containmentFactor);
            int logoSize = resolveLogoSize(Math.min(Math.min(desiredSize, containedSize), maximumLogoSize),
                  minimumLogoSize, maximumLogoSize);
            if (logoSize < minimumLogoSize) {
                continue;
            }

            FactionLogoKey logoKey = new FactionLogoKey(renderKey.territoryKey().date().getYear(),
                component.faction().getShortName());
            FactionLogoImage sourceImage = getFactionLogoImage(component.faction(), logoKey);
            if (sourceImage == null) {
                continue;
            }
            int sourceWidth = sourceImage.tinted().getWidth();
            int sourceHeight = sourceImage.tinted().getHeight();
            int targetWidth = sourceWidth >= sourceHeight
                  ? logoSize
                  : Math.max(1, (int) Math.round(logoSize * (double) sourceWidth / sourceHeight));
            int targetHeight = sourceHeight >= sourceWidth
                  ? logoSize
                  : Math.max(1, (int) Math.round(logoSize * (double) sourceHeight / sourceWidth));
            FactionLogoImage scaledImage = getScaledFactionLogoImage(logoKey, sourceImage, targetWidth, targetHeight);
            Rectangle2D.Double bounds = new Rectangle2D.Double(anchorX - targetWidth / 2.0,
                  anchorY - targetHeight / 2.0, targetWidth, targetHeight);
            if (cullToViewport && !isFactionLogoVisible(
                bounds, renderKey.shadowOffset(), mapPanel.getWidth(), mapPanel.getHeight())) {
                continue;
            }
            candidates.add(new FactionLogoCandidate(component, priority, projectedArea, bounds, scaledImage));
        }

        candidates.sort(Comparator.comparingInt(FactionLogoCandidate::priority)
              .thenComparing(Comparator.comparingDouble(FactionLogoCandidate::projectedArea).reversed())
              .thenComparing(candidate -> candidate.component().faction().getShortName())
              .thenComparingInt(candidate -> candidate.component().anchorHex().column())
              .thenComparingInt(candidate -> candidate.component().anchorHex().row()));
        List<Rectangle2D.Double> acceptedBounds = new ArrayList<>();
        int shadowOffset = renderKey.shadowOffset();
        Shape renderClip = graphics.getClip();
        for (FactionLogoCandidate candidate : candidates) {
            Rectangle2D.Double paddedBounds = new Rectangle2D.Double(
                  candidate.bounds().x - collisionPadding, candidate.bounds().y - collisionPadding,
                  candidate.bounds().width + collisionPadding * 2.0,
                  candidate.bounds().height + collisionPadding * 2.0);
            if (acceptedBounds.stream().anyMatch(accepted -> accepted.intersects(paddedBounds))) {
                continue;
            }
            acceptedBounds.add(paddedBounds);
                        if ((renderClip != null) && !renderClip.intersects(
                                    candidate.bounds().x, candidate.bounds().y,
                                    candidate.bounds().width + shadowOffset,
                                    candidate.bounds().height + shadowOffset)) {
                                continue;
                        }
            int imageX = (int) Math.round(candidate.bounds().x);
            int imageY = (int) Math.round(candidate.bounds().y);
            graphics.drawImage(candidate.image().shadow(), imageX + shadowOffset, imageY + shadowOffset, null);
            graphics.drawImage(candidate.image().tinted(), imageX, imageY, null);
        }
    }

    static boolean isFactionLogoVisible(Rectangle2D bounds, int shadowOffset,
          int viewportWidth, int viewportHeight) {
        return (bounds.getMaxX() + shadowOffset > 0.0)
              && (bounds.getMinX() < viewportWidth)
              && (bounds.getMaxY() + shadowOffset > 0.0)
              && (bounds.getMinY() < viewportHeight);
    }

    private static int getFactionLogoPriority(Faction faction) {
        if (faction.isIndependent() || faction.is(FactionTag.ABANDONED)) {
            return -1;
        }
        if (faction.isMajorOrSuperPower() || faction.isClan()) {
            return 0;
        }
        if (faction.isMinorPower() || faction.isPeriphery() || faction.isDeepPeriphery()) {
            return 1;
        }
        return faction.isPirate() ? 2 : -1;
    }

    static int resolveLogoSize(double size, int minimumSize, int maximumSize) {
        if (size < minimumSize) {
            return 0;
        }
        return Math.clamp((int) Math.round(size), minimumSize, maximumSize);
    }

    private @Nullable FactionLogoImage getFactionLogoImage(Faction faction, FactionLogoKey logoKey) {
        FactionLogoImage cachedImage = factionLogoImages.get(logoKey);
        if ((cachedImage != null) || missingFactionLogoImages.contains(logoKey)) {
            return cachedImage;
        }

        try {
            ImageIcon icon = Factions.getFactionLogo(logoKey.gameYear(), logoKey.factionCode());
            Image sourceImage = icon.getImage();
            int sourceWidth = icon.getIconWidth();
            int sourceHeight = icon.getIconHeight();
            if ((sourceImage == null) || (sourceWidth <= 0) || (sourceHeight <= 0)) {
                missingFactionLogoImages.add(logoKey);
                return null;
            }

            BufferedImage source = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D sourceGraphics = source.createGraphics();
            sourceGraphics.drawImage(sourceImage, 0, 0, null);
            sourceGraphics.dispose();
            Color factionColor = faction.getColor();
            int tintedRed = (factionColor.getRed() + 510) / 3;
            int tintedGreen = (factionColor.getGreen() + 510) / 3;
            int tintedBlue = (factionColor.getBlue() + 510) / 3;
            BufferedImage tinted = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB);
            BufferedImage shadow = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_ARGB);
            for (int imageY = 0; imageY < sourceHeight; imageY++) {
                for (int imageX = 0; imageX < sourceWidth; imageX++) {
                    int alpha = source.getRGB(imageX, imageY) >>> 24;
                    if (alpha == 0) {
                        continue;
                    }
                    tinted.setRGB(imageX, imageY,
                          (alpha << 24) | (tintedRed << 16) | (tintedGreen << 8) | tintedBlue);
                    int shadowAlpha = (int) Math.round(alpha * 0.72);
                    shadow.setRGB(imageX, imageY, shadowAlpha << 24);
                }
            }
            FactionLogoImage image = new FactionLogoImage(tinted, shadow);
            factionLogoImages.put(logoKey, image);
            return image;
        } catch (RuntimeException exception) {
            missingFactionLogoImages.add(logoKey);
            return null;
        }
    }

    private FactionLogoImage getScaledFactionLogoImage(FactionLogoKey logoKey, FactionLogoImage sourceImage,
          int width, int height) {
        ScaledFactionLogoKey scaledKey = new ScaledFactionLogoKey(logoKey, width, height);
        return scaledFactionLogoImages.computeIfAbsent(scaledKey, ignored -> new FactionLogoImage(
              scaleFactionLogoImage(sourceImage.tinted(), width, height),
              scaleFactionLogoImage(sourceImage.shadow(), width, height)));
    }

    private static BufferedImage scaleFactionLogoImage(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.drawImage(source, 0, 0, width, height, null);
        graphics.dispose();
        return scaled;
    }

    private void clearFactionLogoImages() {
        factionLogoImages.clear();
        scaledFactionLogoImages.clear();
        missingFactionLogoImages.clear();
        factionLogoRenderCache.clear();
    }

    static HpgNetworkDetail effectiveHpgNetworkDetail(HpgNetworkDetail selectedDetail, double scale,
          double semanticReference) {
        HpgNetworkDetail requestedDetail = ObjectUtility.nonNull(selectedDetail, HpgNetworkDetail.CLASS_A_B);
        double boundedReference = Math.clamp(semanticReference, 2.4, 3.6);
        double detailStart = Math.clamp(boundedReference * 0.8, 2.4, 3.0);
        double fullDetail = Math.clamp(boundedReference * 1.05, 2.8, 3.4);
        HpgNetworkDetail zoomLimit = scale < detailStart ? HpgNetworkDetail.CLASS_A
              : scale < fullDetail ? HpgNetworkDetail.CLASS_A_B : HpgNetworkDetail.ALL_STATIONS;
        return requestedDetail.ordinal() <= zoomLimit.ordinal() ? requestedDetail : zoomLimit;
    }

        private void drawHpgNetworkLayer(Graphics2D graphics, Stroke thick, Stroke dashed,
            HpgNetworkDetail detail) {
          drawHpgNetworkLayer(graphics, thick, dashed, detail, true);
        }

        private void drawHpgNetworkLayer(Graphics2D graphics, Stroke thick, Stroke dashed,
            HpgNetworkDetail detail, boolean cullToViewport) {
        Collection<HPGLink> hpgNetwork = Systems.getInstance().getHPGNetwork(now);
        for (HPGLink link : hpgNetwork) {
            if (!detail.includes(link.rating())) {
                continue;
            }
            PlanetarySystem primary = link.primary();
            PlanetarySystem secondary = link.secondary();
            if (!cullToViewport || isSystemVisible(primary, false) || isSystemVisible(secondary, false)) {
                if (link.rating() == HPGRating.A) {
                    graphics.setPaint(Color.CYAN);
                    graphics.setStroke(thick);
                    graphics.draw(new Line2D.Double(map2scrX(primary.getX()), map2scrY(primary.getY()),
                          map2scrX(secondary.getX()), map2scrY(secondary.getY())));
                }
                if (link.rating() == HPGRating.B) {
                    graphics.setPaint(Color.BLUE);
                    graphics.setStroke(dashed);
                    graphics.draw(new Line2D.Double(map2scrX(primary.getX()), map2scrY(primary.getY()),
                          map2scrX(secondary.getX()), map2scrY(secondary.getY())));
                }
            }
        }
    }

    private static void drawHpgStationMarker(Graphics2D graphics, SystemMarkerLayout layout,
          HPGRating rating) {
        double radius = hpgStationMarkerRadius(layout.size());
        Point2D.Double anchor = layout.hpgStationAnchor(radius);
          drawHpgStationBadge(graphics, anchor, radius, rating);
        }

        private static void drawHpgStationBadge(Graphics2D graphics, Point2D.Double anchor, double radius,
            HPGRating rating) {
        Shape badge = createRegularPolygon(anchor.x, anchor.y, radius, 6, 0.0);
        Paint oldPaint = graphics.getPaint();
        Stroke oldStroke = graphics.getStroke();
        Font oldFont = graphics.getFont();
        try {
            graphics.setPaint(withAlpha(Color.BLACK, 220));
            graphics.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(badge);
            graphics.setPaint(hpgStationColor(rating));
            graphics.fill(badge);
            graphics.setPaint(withAlpha(Color.WHITE, 135));
            graphics.setStroke(new BasicStroke(1.0f));
            graphics.draw(badge);
            String ratingText = rating.name();
            graphics.setFont(oldFont.deriveFont(Font.BOLD, (float) Math.max(8.0, radius * 1.15)));
            graphics.setPaint(Color.BLACK);
            Point2D.Double baseline = centeredGlyphBaseline(graphics, ratingText, anchor.x, anchor.y);
            graphics.drawString(ratingText, (float) baseline.x, (float) baseline.y);
        } finally {
            graphics.setFont(oldFont);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    private static double hpgStationMarkerRadius(double systemSize) {
        return Math.clamp(systemSize * 0.72, 7.0, 10.0);
    }

    private static Color hpgStationColor(HPGRating rating) {
        return switch (rating) {
            case A -> HPG_CLASS_A_COLOR;
            case B -> HPG_CLASS_B_COLOR;
            case C -> HPG_CLASS_C_COLOR;
            case D -> HPG_CLASS_D_COLOR;
            case X -> MAP_POPUP_DISABLED_TEXT;
        };
    }

        private PlanetarySystem findHoveredSystem(double systemSize,
            Map<String, SystemRenderData> systemRenderData) {
        if ((lastMousePos == null) || (mouseMod == MouseEvent.BUTTON1)) {
            return null;
        }

        PlanetarySystem nearestSystem = null;
        double nearestDistance = Math.max(10.0, systemSize * 2.5);
          double mapX = scr2mapX(lastMousePos.x);
          double mapY = scr2mapY(lastMousePos.y);
          double mapRadius = nearestDistance / Math.max(conf.scale, 0.0001);
          for (PlanetarySystem system : systemSpatialIndex.query(mapX - mapRadius, mapY - mapRadius,
              mapX + mapRadius, mapY + mapRadius, null, null)) {
            if (!isSystemVisible(system, !optEmptySystems.isSelected(), systemRenderData.get(system.getId()))) {
                continue;
            }
            double distance = Point2D.distance(lastMousePos.x, lastMousePos.y,
                  map2scrX(system.getX()), map2scrY(system.getY()));
            if (distance <= nearestDistance) {
                nearestSystem = system;
                nearestDistance = distance;
            }
        }
        return nearestSystem;
    }

    private PlanetarySystem findSystemAt(Point point) {
        double systemSize = getSystemMarkerSize();
                Map<String, SystemRenderData> systemRenderData =
                            getPreparedSystemRenderData(campaign.getLocalDate());
        PlanetarySystem nearestSystem = null;
        double nearestDistance = Math.max(10.0, systemSize * 2.5);
                  double mapX = scr2mapX(point.x);
                  double mapY = scr2mapY(point.y);
                  double mapRadius = nearestDistance / Math.max(conf.scale, 0.0001);
                  for (PlanetarySystem system : systemSpatialIndex.query(mapX - mapRadius, mapY - mapRadius,
                      mapX + mapRadius, mapY + mapRadius, null, null)) {
                        if (!isSystemVisible(system, !optEmptySystems.isSelected(), systemRenderData.get(system.getId()))) {
                continue;
            }
            double distance = Point2D.distance(point.x, point.y,
                  map2scrX(system.getX()), map2scrY(system.getY()));
            if (distance <= nearestDistance) {
                nearestSystem = system;
                nearestDistance = distance;
            }
        }
        return nearestSystem;
    }

    private double getSystemMarkerSize() {
        return Math.clamp(1 + (5 * Math.log(conf.scale)), conf.minDotSize, conf.maxDotSize);
    }

    private JMenuItem createRoutePlanningMenuItem(String resourceKey, boolean enabled, Runnable action) {
        JMenuItem item = new JMenuItem(resourceMap.getString(resourceKey + ".text"));
        item.setToolTipText(resourceMap.getString(resourceKey + ".toolTipText"));
        item.getAccessibleContext().setAccessibleName(item.getText());
        item.getAccessibleContext().setAccessibleDescription(item.getToolTipText());
        item.setEnabled(enabled);
        item.addActionListener(event -> action.run());
        return item;
    }

    static void styleNavigationPopup(JPopupMenu popup) {
        popup.setOpaque(true);
        popup.setBackground(MAP_POPUP_BACKGROUND);
        popup.setForeground(MAP_POPUP_TEXT);
        popup.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(MAP_POPUP_BORDER),
              BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(4), 0, UIUtil.scaleForGUI(4), 0)));
        for (Component component : popup.getComponents()) {
            if (component instanceof JMenu menu) {
                styleNavigationMenuItem(menu, new NavigationMenuUI());
                styleNavigationPopup(menu.getPopupMenu());
            } else if (component instanceof JMenuItem item) {
                styleNavigationMenuItem(item, new NavigationMenuItemUI());
            } else if (component instanceof JSeparator separator) {
                separator.setBackground(MAP_POPUP_BACKGROUND);
                separator.setForeground(MAP_POPUP_BORDER);
            }
        }
    }

    private static void styleNavigationMenuItem(JMenuItem item, BasicMenuItemUI menuItemUI) {
        item.setUI(menuItemUI);
        item.setOpaque(true);
        item.setBackground(MAP_POPUP_BACKGROUND);
        item.setForeground(MAP_POPUP_TEXT);
        item.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(12),
              UIUtil.scaleForGUI(6), UIUtil.scaleForGUI(12)));
    }

    private static class NavigationMenuItemUI extends BasicMenuItemUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            selectionBackground = MAP_POPUP_SELECTION_BACKGROUND;
            selectionForeground = MAP_POPUP_SELECTION_TEXT;
            disabledForeground = MAP_POPUP_DISABLED_TEXT;
        }
    }

    private static final class NavigationMenuUI extends BasicMenuUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            selectionBackground = MAP_POPUP_SELECTION_BACKGROUND;
            selectionForeground = MAP_POPUP_SELECTION_TEXT;
            disabledForeground = MAP_POPUP_DISABLED_TEXT;
        }
    }

    private static void drawIntrinsicStar(Graphics2D graphics, Arc2D.Double arc, PlanetarySystem system,
          double x, double y, double size, double alpha) {
        if (alpha <= 0.0) {
            return;
        }
        Paint oldPaint = graphics.getPaint();
        Composite oldComposite = graphics.getComposite();
        try {
            graphics.setComposite(deriveCompositeWithAlpha(oldComposite, alpha));
            Color spectralColor = getSpectralColor(system.getStar());
            double luminosityScale = getLuminosityClassVisualScale(system.getStar());
            double auraRadius = Math.max(2.0, size * 1.65) * luminosityScale;
            graphics.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) auraRadius,
                  new float[] { 0.0f, 0.16f, 0.38f, 0.68f, 1.0f },
                  new Color[] {
                      withAlpha(brighten(spectralColor), 245),
                      withAlpha(spectralColor, 225),
                      withAlpha(spectralColor, 130),
                      withAlpha(spectralColor, 42),
                      withAlpha(spectralColor, 0)
                  }, CycleMethod.NO_CYCLE));
            arc.setArcByCenter(x, y, auraRadius, 0, 360, Arc2D.OPEN);
            graphics.fill(arc);

            double coreRadius = Math.max(0.65, size * 0.3) * luminosityScale;
            arc.setArcByCenter(x, y, coreRadius, 0, 360, Arc2D.OPEN);
            graphics.setPaint(withAlpha(brighten(spectralColor), 250));
            graphics.fill(arc);
        } finally {
            graphics.setComposite(oldComposite);
            graphics.setPaint(oldPaint);
        }
    }

    private static void drawNavigationContact(Graphics2D graphics, Arc2D.Double arc,
          double x, double y, double size) {
        Color contactColor = new Color(165, 184, 190);
        arc.setArcByCenter(x, y, Math.max(1.5, size * 0.9), 0, 360, Arc2D.OPEN);
        graphics.setPaint(withAlpha(contactColor, 55));
        graphics.fill(arc);
        arc.setArcByCenter(x, y, Math.max(0.8, size * 0.46), 0, 360, Arc2D.OPEN);
        graphics.setPaint(contactColor);
        graphics.fill(arc);
        arc.setArcByCenter(x, y, Math.max(0.35, size * 0.16), 0, 360, Arc2D.OPEN);
        graphics.setPaint(brighten(contactColor));
        graphics.fill(arc);
    }

    private void drawStrategicContact(Graphics2D graphics, Arc2D.Double arc, PlanetarySystem system,
          SystemRenderData renderData, SystemMarkerLayout layout, MapMode mapMode) {
        if ((mapMode == MapMode.FACTION) && !renderData.empty()) {
            drawStrategicContactCore(graphics, arc, layout, renderData.factionColors());
        } else {
            Color contactColor = renderData.empty()
                  ? new Color(105, 120, 128)
                  : getSystemColor(system, mapMode);
            drawStrategicContactCore(graphics, arc, layout, contactColor);
        }
    }

    private static void drawStrategicContactCore(Graphics2D graphics, Arc2D.Double arc,
          SystemMarkerLayout layout, Color contactColor) {
        double contactRadius = Math.max(1.8, Math.min(3.2, layout.size() * 0.62));
        arc.setArcByCenter(layout.centerX(), layout.centerY(), contactRadius, 0, 360, Arc2D.OPEN);
        graphics.setPaint(contactColor);
        graphics.fill(arc);
    }

    static void drawStrategicContactCore(Graphics2D graphics, Arc2D.Double arc,
          SystemMarkerLayout layout, List<Color> contactColors) {
        if (contactColors.isEmpty()) {
            return;
        }
        Paint oldPaint = graphics.getPaint();
        try {
            double contactRadius = Math.max(1.8, Math.min(3.2, layout.size() * 0.62));
            double segmentExtent = 360.0 / contactColors.size();
            for (int colorIndex = 0; colorIndex < contactColors.size(); colorIndex++) {
                arc.setArcByCenter(layout.centerX(), layout.centerY(), contactRadius,
                      90.0 - ((colorIndex + 1) * segmentExtent), segmentExtent, Arc2D.PIE);
                graphics.setPaint(contactColors.get(colorIndex));
                graphics.fill(arc);
            }
        } finally {
            graphics.setPaint(oldPaint);
        }
    }

    private void refreshMapPresentationData() {
        List<AbstractContract> activeContracts = campaign.getActiveContracts();
          mapPresentationData = createMapPresentationData(activeContracts, campaign.getActiveScenarios(),
              campaign::getContract, playerBaseCountsBySystem());
        }

        static MapPresentationData createMapPresentationData(List<AbstractContract> activeContracts,
            List<Scenario> activeScenarios, Function<java.util.UUID, AbstractContract> missionLookup,
            Map<String, Integer> playerBaseCounts) {
        Set<Faction> contractEmployers = new HashSet<>();
        Set<Faction> contractTargets = new HashSet<>();
        for (AbstractContract contract : activeContracts) {
            contractEmployers.add(contract.getEmployerFaction());
            contractTargets.add(contract.getEnemyFaction());
        }
          return new MapPresentationData(
              buildStrategicMarkers(activeContracts, activeScenarios, missionLookup),
              playerBaseCounts, contractEmployers, contractTargets);
    }

        static Map<String, StrategicMarker> buildStrategicMarkers(List<AbstractContract> activeMissions,
            List<Scenario> activeScenarios, Function<java.util.UUID, AbstractContract> missionLookup) {
        Map<String, StrategicMarker> strategicMarkers = new HashMap<>();
          for (AbstractContract mission : activeMissions) {
            PlanetarySystem system = mission.getTargetSystem();
            if ((system == null) || (system.getId() == null)) {
                continue;
            }
            strategicMarkers.compute(system.getId(), (systemId, marker) -> marker == null
                  ? new StrategicMarker(1, 0)
                  : new StrategicMarker(marker.activeMissionCount() + 1, marker.activeScenarioCount()));
        }

        for (Scenario scenario : activeScenarios) {
            AbstractContract mission = missionLookup.apply(scenario.getMissionId());
            if (mission == null) {
                continue;
            }
            PlanetarySystem system = mission.getTargetSystem();
            if ((system == null) || (system.getId() == null)) {
                continue;
            }
            strategicMarkers.compute(system.getId(), (systemId, marker) -> marker == null
                  ? new StrategicMarker(0, 1)
                  : new StrategicMarker(marker.activeMissionCount(), marker.activeScenarioCount() + 1));
        }
        return Map.copyOf(strategicMarkers);
    }

    static double visibleOperationAlpha(double layerAlpha, SemanticZoomProfile semanticZoom,
          StrategicMarker marker) {
        if (marker == null) {
            return 0.0;
        }
        double semanticAlpha = marker.hasActiveScenario()
              ? semanticZoom.urgentOperationAlpha()
              : semanticZoom.missionOperationAlpha();
        return layerAlpha * semanticAlpha;
    }

    static void drawOperationMarker(Graphics2D graphics, SystemMarkerLayout layout,
          StrategicMarker marker) {
          drawOperationMarker(graphics, layout, marker, 1.0);
        }

        private static void drawOperationMarker(Graphics2D graphics, SystemMarkerLayout layout,
            StrategicMarker marker, double expansion) {
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        Font oldFont = graphics.getFont();
        try {
            boolean urgent = marker.hasActiveScenario();
            double hierarchyScale = urgent ? 1.2 : 1.0;
            double size = layout.size();
            double flagHeight = Math.max(6.0, size * 0.9) * hierarchyScale;
            double flagWidth = Math.max(8.0, size * 1.15) * hierarchyScale;
            Point2D.Double anchor = layout.operationAnchor(operationMarkerRadius(size, hierarchyScale), expansion);
            double mastX = anchor.x - (flagWidth / 2.0);
            double mastTopY = anchor.y - (flagHeight / 2.0);
            double mastBottomY = anchor.y + (flagHeight * 0.85);

            GeneralPath pennant = new GeneralPath();
            pennant.moveTo(mastX, mastTopY);
            pennant.lineTo(mastX + flagWidth, mastTopY + (flagHeight * 0.5));
            pennant.lineTo(mastX, mastTopY + flagHeight);
            pennant.closePath();
            Line2D.Double mast = new Line2D.Double(mastX, mastTopY, mastX, mastBottomY);

            if (urgent) {
                graphics.setPaint(URGENT_OPERATION_COLOR);
                graphics.fill(pennant);
            }
            graphics.setPaint(withAlpha(Color.BLACK, urgent ? 235 : 190));
            graphics.setStroke(new BasicStroke(urgent ? 4.6f : 3.8f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(mast);
            graphics.draw(pennant);
            graphics.setPaint(urgent ? URGENT_OPERATION_COLOR : withAlpha(OPERATION_MARKER_COLOR, 210));
            graphics.setStroke(new BasicStroke(urgent ? 2.5f : 1.8f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(mast);
            graphics.draw(pennant);

            if (marker.activeMissionCount() > 1) {
                String countText = Integer.toString(marker.activeMissionCount());
                Font countFont = oldFont.deriveFont(Font.BOLD,
                      Math.max(9.0f, Math.min(11.0f, oldFont.getSize2D())));
                graphics.setFont(countFont);
                double badgeHeight = Math.max(10.0, graphics.getFontMetrics().getAscent() + 2.0);
                double badgeWidth = Math.max(badgeHeight,
                    graphics.getFontMetrics().stringWidth(countText) + 6.0);
                double badgeX = mastX - badgeWidth - Math.max(4.0, size * 0.35);
                double badgeY = anchor.y - (badgeHeight / 2.0);
                java.awt.geom.RoundRectangle2D.Double badge = new java.awt.geom.RoundRectangle2D.Double(
                    badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);
                graphics.setPaint(withAlpha(Color.BLACK, 225));
                graphics.setStroke(new BasicStroke(3.0f));
                graphics.draw(badge);
                graphics.setPaint(urgent ? URGENT_OPERATION_COLOR : OPERATION_MARKER_COLOR);
                graphics.fill(badge);
                graphics.setPaint(Color.BLACK);
                Point2D.Double baseline = centeredGlyphBaseline(graphics, countText,
                    badgeX + (badgeWidth / 2.0), badgeY + (badgeHeight / 2.0));
                graphics.drawString(countText, (float) baseline.x, (float) baseline.y);
            }
        } finally {
            graphics.setFont(oldFont);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    private static double operationMarkerRadius(double systemSize, double hierarchyScale) {
        double halfWidth = Math.max(8.0, systemSize * 1.15) * hierarchyScale / 2.0;
        double lowerMastExtent = Math.max(6.0, systemSize * 0.9) * hierarchyScale * 0.85;
        return Math.hypot(halfWidth, lowerMastExtent);
    }

    private static void drawStrategicOperationMarker(Graphics2D graphics, SystemMarkerLayout layout) {
        double radius = Math.max(4.2, layout.size() * 0.72);
        Shape diamond = createRegularPolygon(layout.centerX(), layout.centerY(), radius, 4, 0.0);
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        try {
            graphics.setPaint(withAlpha(Color.BLACK, 225));
            graphics.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(diamond);
            graphics.setPaint(URGENT_OPERATION_COLOR);
            graphics.fill(diamond);
            graphics.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setPaint(withAlpha(Color.WHITE, 120));
            graphics.draw(diamond);
        } finally {
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    record StrategicMarker(int activeMissionCount, int activeScenarioCount) {
        boolean hasActiveScenario() {
            return activeScenarioCount > 0;
        }
    }

        private void drawCapitalHierarchyOverlay(Graphics2D graphics, PlanetarySystem system,
                    SystemMarkerLayout layout, SystemRenderData renderData, CapitalDisplayDetail displayDetail,
                    double detailAlpha, double capitalAlpha, double detailedOverlayAlpha) {
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        Composite oldComposite = graphics.getComposite();
        try {
            List<Faction> capitalFactions = renderData.capitalFactions();
            CapitalType capitalType = capitalFactions.isEmpty()
                  ? renderData.capitalType()
                  : CapitalType.NATIONAL;
            double visibleAlpha = capitalVisibilityAlpha(
                  capitalType, displayDetail, capitalAlpha, detailedOverlayAlpha);
            if (visibleAlpha > 0.0) {
                Composite capitalComposite = deriveCompositeWithAlpha(oldComposite, visibleAlpha);
                graphics.setComposite(capitalComposite);
                if (!capitalFactions.isEmpty()) {
                    for (int capitalIndex = 0; capitalIndex < capitalFactions.size(); capitalIndex++) {
                        Faction capitalFaction = capitalFactions.get(capitalIndex);
                        drawDatedCapitalMarker(graphics,
                              layout.capitalAnchor(capitalIndex, capitalFactions.size(), detailAlpha), layout.size(),
                              capitalFaction, system.getId(), system.getId());
                    }
                } else if (!capitalType.isNone()) {
                    Color markerColor = renderData.factionColors().isEmpty()
                          ? MAP_LEGEND_MUTED_TEXT
                          : renderData.factionColors().get(0);
                    drawCapitalHierarchyMarker(graphics, layout.capitalAnchor(0, 1, detailAlpha),
                          layout.size(), capitalType, markerColor);
                }
            }

        } finally {
            graphics.setComposite(oldComposite);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    static double capitalVisibilityAlpha(CapitalType capitalType, CapitalDisplayDetail displayDetail,
          double capitalAlpha, double detailedOverlayAlpha) {
        if (!displayDetail.includes(capitalType)) {
            return 0.0;
        }
        return capitalType == CapitalType.DISTRICT
              ? capitalAlpha * detailedOverlayAlpha
              : capitalAlpha;
    }

    static void drawFactionOwnershipRing(Graphics2D graphics, Arc2D.Double arc,
          SystemMarkerLayout layout, List<Color> factionColors) {
        drawFactionOwnershipRing(graphics, arc, layout, factionColors, 1.0);
    }

    private static void drawFactionOwnershipRing(Graphics2D graphics, Arc2D.Double arc,
          SystemMarkerLayout layout, List<Color> factionColors, double alpha) {
        if (factionColors.isEmpty() || (alpha <= 0.0)) {
            return;
        }
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        Composite oldComposite = graphics.getComposite();
        try {
            double sharpenedAlpha = Math.min(1.0, alpha * 1.75);
            graphics.setComposite(deriveCompositeWithAlpha(oldComposite, sharpenedAlpha));
            arc.setArcByCenter(layout.centerX(), layout.centerY(), layout.ownershipRadius(),
                  0.0, 360.0, Arc2D.OPEN);
            graphics.setPaint(withAlpha(Color.BLACK, 210));
            graphics.setStroke(new BasicStroke(4.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            graphics.draw(arc);
            graphics.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            double segmentExtent = 360.0 / factionColors.size();
            for (int colorIndex = 0; colorIndex < factionColors.size(); colorIndex++) {
                graphics.setPaint(factionColors.get(colorIndex));
                arc.setArcByCenter(layout.centerX(), layout.centerY(), layout.ownershipRadius(),
                      90.0 - ((colorIndex + 1) * segmentExtent), segmentExtent, Arc2D.OPEN);
                graphics.draw(arc);
            }
        } finally {
            graphics.setComposite(oldComposite);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    static List<Faction> resolveDatedCapitalFactions(@Nullable Set<Faction> owners,
          Map<Faction, String> capitals, String systemId) {
        Faction mercenaryFaction = null;
        String mercenaryCapitalSystem = null;
        for (Map.Entry<Faction, String> entry : capitals.entrySet()) {
            if ("MERC".equals(entry.getKey().getShortName())) {
                mercenaryFaction = entry.getKey();
                mercenaryCapitalSystem = entry.getValue();
                break;
            }
        }
        return resolveDatedCapitalFactions(owners, capitals, systemId,
              mercenaryFaction, mercenaryCapitalSystem);
    }

    private static List<Faction> resolveDatedCapitalFactions(@Nullable Set<Faction> owners,
          Map<Faction, String> capitals, String systemId, @Nullable Faction mercenaryFaction,
          @Nullable String mercenaryCapitalSystem) {
        List<Faction> capitalFactions = new ArrayList<>();
        if (owners != null) {
            for (Faction owner : owners) {
                if (systemId.equals(capitals.get(owner))) {
                    capitalFactions.add(owner);
                }
            }
        }

          if ((mercenaryFaction != null) && systemId.equals(mercenaryCapitalSystem)
              && !capitalFactions.contains(mercenaryFaction)) {
            capitalFactions.add(mercenaryFaction);
        }
        capitalFactions.sort(Comparator.comparing(Faction::getShortName));
        return List.copyOf(capitalFactions);
    }

    static void drawFactionCapitalMarker(Graphics2D graphics, Point2D.Double anchor, double size,
          Faction faction) {
        drawNationalCapitalMarker(graphics, anchor, size, faction.getColor());
    }

    static void drawDatedCapitalMarker(Graphics2D graphics, Point2D.Double anchor, double size,
          Faction faction, String systemId, @Nullable String datedCapitalSystemId) {
        drawFactionCapitalMarker(graphics, anchor, size, faction);
    }

        static void drawCapitalHierarchyMarker(Graphics2D graphics, Point2D.Double anchor, double size,
                    CapitalType capitalType, Color color) {
                switch (capitalType) {
            case NATIONAL -> drawNationalCapitalMarker(graphics, anchor, size, color);
            case REGION -> drawAdministrativeCapitalMarker(graphics, anchor, size, color, true);
            case DISTRICT -> drawAdministrativeCapitalMarker(graphics, anchor, size, color, false);
            case NONE -> {
            }
        }
        }

        private static void drawAdministrativeCapitalMarker(Graphics2D graphics, Point2D.Double anchor,
                    double size, Color color, boolean regional) {
                double radius = regional ? nationalCapitalHalfWidth(size) : Math.max(3.0, size * 0.42);
                Shape marker = regional
                            ? createRadialStar(anchor, radius, radius * 0.32, 4)
                            : new Ellipse2D.Double(anchor.x - radius, anchor.y - radius, radius * 2.0, radius * 2.0);
                graphics.setPaint(withAlpha(Color.BLACK, 205));
                graphics.setStroke(new BasicStroke(regional ? nationalCapitalOutlineWidth(size) : 3.2f,
              BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.draw(marker);
                graphics.setPaint(color);
                graphics.fill(marker);
                graphics.setPaint(withAlpha(Color.WHITE, regional ? 115 : 90));
                graphics.setStroke(new BasicStroke(1.0f,
              BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.draw(marker);
        }

        private static GeneralPath createRadialStar(Point2D.Double anchor, double outerRadius,
                    double innerRadius, int rayCount) {
                GeneralPath star = new GeneralPath();
                for (int pointIndex = 0; pointIndex < rayCount * 2; pointIndex++) {
                        double radius = (pointIndex % 2 == 0) ? outerRadius : innerRadius;
                        double angle = -Math.PI / 2.0 + (pointIndex * Math.PI / rayCount);
                        double x = anchor.x + (Math.cos(angle) * radius);
                        double y = anchor.y + (Math.sin(angle) * radius);
                        if (pointIndex == 0) {
                                star.moveTo(x, y);
                        } else {
                                star.lineTo(x, y);
            }
                }
                star.closePath();
                return star;
        }

    static Rectangle2D.Double capitalBandMarkerBounds(Point2D.Double anchor, double markerSize) {
        return nationalCapitalMarkerBounds(anchor, markerSize);
    }

    static Rectangle2D.Double nationalCapitalMarkerBounds(Point2D.Double anchor, double markerSize) {
        double strokePadding = nationalCapitalOutlineWidth(markerSize) / 2.0;
        double halfWidth = nationalCapitalHalfWidth(markerSize) + strokePadding;
        double halfHeight = nationalCapitalHalfHeight(markerSize) + strokePadding;
        return new Rectangle2D.Double(anchor.x - halfWidth, anchor.y - halfHeight,
              halfWidth * 2.0, halfHeight * 2.0);
    }

    private static double nationalCapitalHalfWidth(double markerSize) {
        return Math.max(4.5, markerSize * 0.62);
    }

    private static double nationalCapitalHalfHeight(double markerSize) {
        return Math.max(4.5, markerSize * 0.62);
    }

    private static float nationalCapitalOutlineWidth(double markerSize) {
        return (float) Math.clamp(markerSize * 0.42, 4.0, 5.0);
    }

    static void drawNationalCapitalMarker(Graphics2D graphics, Point2D.Double anchor, double size,
          Color factionColor) {
        double outerRadius = nationalCapitalHalfWidth(size);
        Shape star = createCenteredMaterialSymbol(graphics, MATERIAL_STAR_SYMBOL, anchor, outerRadius * 2.0);
        if (star == null) {
            star = createFallbackStar(anchor, outerRadius);
        }

        graphics.setPaint(withAlpha(Color.BLACK, 205));
        graphics.setStroke(new BasicStroke(nationalCapitalOutlineWidth(size), BasicStroke.CAP_ROUND,
              BasicStroke.JOIN_ROUND));
        graphics.draw(star);
        graphics.setPaint(factionColor);
        graphics.fill(star);
        graphics.setPaint(withAlpha(Color.WHITE, 115));
        graphics.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(star);
    }

    private static GeneralPath createFallbackStar(Point2D.Double anchor, double outerRadius) {
        double innerRadius = outerRadius * 0.43;
        GeneralPath star = new GeneralPath();
        for (int pointIndex = 0; pointIndex < 10; pointIndex++) {
            double radius = (pointIndex % 2 == 0) ? outerRadius : innerRadius;
            double angle = Math.toRadians(-90.0 + (pointIndex * 36.0));
            double pointX = anchor.x + (Math.cos(angle) * radius);
            double pointY = anchor.y + (Math.sin(angle) * radius);
            if (pointIndex == 0) {
                star.moveTo(pointX, pointY);
            } else {
                star.lineTo(pointX, pointY);
            }
        }
        star.closePath();
        return star;
    }

    static void drawRestrictedSystemMarker(Graphics2D graphics, SystemMarkerLayout layout) {
        drawRestrictedSystemMarker(graphics, layout, 1.0);
    }

    private static void drawRestrictedSystemMarker(Graphics2D graphics, SystemMarkerLayout layout,
          double expansion) {
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        try {
            double radius = restrictedMarkerRadius(layout, expansion);
            double inset = radius * 0.72;
            float backgroundStroke = (float) interpolate(3.0, 5.0, expansion);
            float foregroundStroke = (float) interpolate(1.7, 2.7, expansion);
            Arc2D.Double ring = new Arc2D.Double();
            ring.setArcByCenter(layout.centerX(), layout.centerY(), radius, 0, 360, Arc2D.OPEN);
            graphics.setPaint(withAlpha(Color.BLACK, 220));
            graphics.setStroke(new BasicStroke(backgroundStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(ring);
            graphics.draw(new Line2D.Double(layout.centerX() - inset, layout.centerY() + inset,
                  layout.centerX() + inset, layout.centerY() - inset));
            graphics.setPaint(NAVIGATION_BLOCKED_COLOR);
            graphics.setStroke(new BasicStroke(foregroundStroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(ring);
            graphics.draw(new Line2D.Double(layout.centerX() - inset, layout.centerY() + inset,
                  layout.centerX() + inset, layout.centerY() - inset));
        } finally {
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    static double restrictedMarkerRadius(SystemMarkerLayout layout, double expansion) {
        double strategicRadius = Math.max(3.8, Math.min(5.2, layout.size() * 0.75));
        return interpolate(strategicRadius, layout.overrideRadius(), Math.clamp(expansion, 0.0, 1.0));
    }

    static void drawGmEditedSystemMarker(Graphics2D graphics, SystemMarkerLayout layout) {
        drawGmEditedSystemMarker(graphics, layout, 1.0);
    }

    private static void drawGmEditedSystemMarker(Graphics2D graphics, SystemMarkerLayout layout,
          double expansion) {
        drawGmEditedSystemMarker(graphics, layout.gmEditedAnchor(expansion), layout.size());
    }

    private static void drawGmEditedSystemMarker(Graphics2D graphics, Point2D.Double anchor,
          double systemSize) {
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        try {
            Shape pencil = createCenteredMaterialSymbol(graphics, MATERIAL_EDIT_SYMBOL, anchor,
                  gmEditedMarkerSize(systemSize));
            if (pencil == null) {
                return;
            }
            graphics.setPaint(withAlpha(Color.BLACK, 225));
            graphics.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(pencil);
            graphics.setPaint(PLANNED_ROUTE_COLOR);
            graphics.fill(pencil);
            graphics.setPaint(withAlpha(Color.WHITE, 85));
            graphics.setStroke(new BasicStroke(0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.draw(pencil);
        } finally {
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    private static double gmEditedMarkerSize(double systemSize) {
        return Math.max(16.0, systemSize * 1.1);
    }

    private static @Nullable Shape createCenteredMaterialSymbol(Graphics2D graphics, int codePoint,
          Point2D.Double anchor, double targetSize) {
        Font symbolFont = FontHandler.symbolFont().deriveFont((float) (targetSize * 1.5));
        if (!symbolFont.canDisplay(codePoint)) {
            return null;
        }
        GlyphVector glyph = symbolFont.createGlyphVector(graphics.getFontRenderContext(),
              Character.toString(codePoint));
        Rectangle2D bounds = glyph.getVisualBounds();
        if (bounds.isEmpty()) {
            return null;
        }
        double scale = targetSize / Math.max(bounds.getWidth(), bounds.getHeight());
        AffineTransform transform = new AffineTransform();
        transform.translate(anchor.x, anchor.y);
        transform.scale(scale, scale);
        transform.translate(-bounds.getCenterX(), -bounds.getCenterY());
        return transform.createTransformedShape(glyph.getOutline());
    }

    private void drawMapModeOverlay(Graphics2D graphics, Arc2D.Double arc, PlanetarySystem system,
          SystemRenderData renderData, SystemMarkerLayout layout, boolean showSystemArt,
                    boolean showRouteContact, MapMode mapMode, double strategicContactAlpha,
                    double systemDetailAlpha, double serviceAlpha, boolean drawSystemArt) {
        if (drawSystemArt && showSystemArt && !showRouteContact) {
            drawSystemMapModeArt(graphics, arc, system, renderData, layout, mapMode,
                  strategicContactAlpha, systemDetailAlpha, serviceAlpha,
                  optEmptySystems.isSelected());
        }
    }

    private void drawSystemMapModeArt(Graphics2D graphics, Arc2D.Double arc, PlanetarySystem system,
          SystemRenderData renderData, SystemMarkerLayout layout, MapMode mapMode,
          double strategicContactAlpha, double systemDetailAlpha, double serviceAlpha,
          boolean showEmptySystems) {
        if (strategicContactAlpha > 0.0) {
            Composite oldComposite = graphics.getComposite();
            graphics.setComposite(deriveCompositeWithAlpha(oldComposite, strategicContactAlpha));
            drawStrategicContact(graphics, arc, system, renderData, layout, mapMode);
            graphics.setComposite(oldComposite);
        }
        if (mapMode == MapMode.FACTION) {
            if (!renderData.empty()) {
                drawFactionOwnershipRing(graphics, arc, layout, renderData.factionColors(),
                      systemDetailAlpha);
            } else if (showEmptySystems) {
                drawAnalyticalOverlay(graphics, arc, layout, Color.DARK_GRAY, systemDetailAlpha);
            }
        } else {
            double analyticalAlpha = isServiceMapMode(mapMode) ? serviceAlpha : systemDetailAlpha;
            drawAnalyticalOverlay(graphics, arc, layout, getSystemColor(system, mapMode), analyticalAlpha);
        }
    }

    private static boolean isServiceMapMode(MapMode mapMode) {
        return switch (mapMode) {
            case RECHARGE_STATIONS, ACADEMIES, HIRING_HALLS, DISEASE_OUTBREAKS -> true;
            default -> false;
        };
    }

    static void drawAnalyticalOverlay(Graphics2D graphics, Arc2D.Double arc,
          SystemMarkerLayout layout, Color color, double alpha) {
        if (alpha <= 0.0) {
            return;
        }
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        Composite oldComposite = graphics.getComposite();
        try {
            graphics.setComposite(deriveCompositeWithAlpha(oldComposite, alpha));
            graphics.setPaint(color);
            graphics.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            arc.setArcByCenter(layout.centerX(), layout.centerY(), layout.ownershipRadius(),
                  0, 360, Arc2D.OPEN);
            graphics.draw(arc);
        } finally {
            graphics.setComposite(oldComposite);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

        private static void drawRouteWaypoint(Graphics2D graphics, Arc2D.Double arc, SystemMarkerLayout layout,
                    Color color) {
        Stroke oldStroke = graphics.getStroke();
                graphics.setStroke(new BasicStroke(layout.routeState() == RouteMarkerState.ACTIVE ? 2.5f : 1.8f));
        graphics.setPaint(color);
                arc.setArcByCenter(layout.centerX(), layout.centerY(), layout.navigationRadius(), 0, 360, Arc2D.OPEN);
        graphics.draw(arc);
        graphics.setStroke(oldStroke);
    }

    private void drawReachability(Graphics2D graphics, double systemSize, double alpha,
          Map<String, SystemRenderData> systemRenderData) {
        if ((cachedReachability == null) || (alpha <= 0.0)) {
            return;
        }

        paintLayerWithAlpha(graphics, alpha, markerGraphics -> {
            for (NavigationRouteAnalysis.ReachabilityEntry entry : cachedReachability.reachableSystems()) {
                if (shouldRenderOptionalSystemOverlay(entry.system(), systemRenderData)) {
                    drawReachabilityEntry(markerGraphics, entry, false, systemSize);
                }
            }
            for (NavigationRouteAnalysis.ReachabilityEntry entry : cachedReachability.blockedFrontier()) {
                if (shouldRenderOptionalSystemOverlay(entry.system(), systemRenderData)) {
                    drawReachabilityEntry(markerGraphics, entry, true, systemSize);
                }
            }
        });
    }

    private boolean shouldRenderOptionalSystemOverlay(PlanetarySystem system,
          Map<String, SystemRenderData> systemRenderData) {
        SystemRenderData renderData = systemRenderData.get(system.getId());
        return shouldRenderOptionalSystemOverlay(renderData != null && renderData.empty(),
              optEmptySystems.isSelected(), isSameSystem(system, selectedSystem),
              isSameSystem(system, campaign.getCurrentSystem()));
    }

    static boolean shouldRenderOptionalSystemOverlay(boolean systemEmpty, boolean showEmptySystems,
          boolean selectedSystem, boolean currentSystem) {
        return !systemEmpty || showEmptySystems || selectedSystem || currentSystem;
    }

    private void drawReachabilityEntry(Graphics2D graphics, NavigationRouteAnalysis.ReachabilityEntry entry,
          boolean blockedFrontier, double systemSize) {
        PlanetarySystem system = entry.system();
          SystemMarkerLayout layout = SystemMarkerLayout.create(map2scrX(system.getX()), map2scrY(system.getY()),
              systemSize, RouteMarkerState.NONE, false, false);
        ReachabilityMarkerStyle style = reachabilityMarkerStyle(entry.minimumHops(),
              entry.arrivalAssessment().severity(), blockedFrontier);
        Color color = markerColor(style.tone());
          BasicStroke markerStroke = reachabilityMarkerStroke(style.tone());
          double markerRadius = Math.max(6.0, navigationMarkerPathRadius(style.shape(),
              layout.navigationClearanceRadius(), markerStroke.getLineWidth()));
          Shape marker = createNavigationMarkerShape(style.shape(), layout.centerX(), layout.centerY(), markerRadius);
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        Font oldFont = graphics.getFont();
        try {
            graphics.setPaint(withAlpha(color, blockedFrontier ? 225 : 185));
            graphics.setStroke(markerStroke);
            graphics.draw(marker);
            if (!blockedFrontier && (style.tone() != NavigationMarkerTone.CAUTION)) {
                String shell = Integer.toString(entry.minimumHops());
                Font shellFont = oldFont.deriveFont(Font.BOLD,
                      Math.max(8.0f, Math.min(10.0f, oldFont.getSize2D() * 0.8f)));
                graphics.setFont(shellFont);
                graphics.setPaint(color);
                    graphics.drawString(shell, (float) (layout.centerX() + markerRadius + 2.0),
                        (float) (layout.centerY() - markerRadius + graphics.getFontMetrics().getAscent()));
            }
        } finally {
            graphics.setFont(oldFont);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    private void drawReachabilityAnnotation(Graphics2D graphics, double systemSize, double alpha) {
        if ((cachedReachability == null) || (alpha <= 0.0)) {
            return;
        }
        PlanetarySystem anchor = cachedReachability.anchor();
        String annotation = MessageFormat.format(resourceMap.getString("map.reachability.anchor.format"),
              anchor.getPrintableName(campaign.getLocalDate()), cachedReachability.maximumHops());
        Font oldFont = graphics.getFont();
        Paint oldPaint = graphics.getPaint();
        Font annotationFont = oldFont.deriveFont(Font.BOLD,
              Math.max(8.0f, Math.min(10.0f, oldFont.getSize2D() * 0.78f)));
        graphics.setFont(annotationFont);
        float x = (float) (map2scrX(anchor.getX()) + Math.max(9.0, systemSize * 1.8));
        float y = (float) (map2scrY(anchor.getY()) - Math.max(8.0, systemSize * 1.4));
          paintLayerWithAlpha(graphics, alpha,
              annotationGraphics -> drawNavigationText(annotationGraphics, annotation, x, y, PLANNED_ROUTE_COLOR));
        graphics.setFont(oldFont);
        graphics.setPaint(oldPaint);
    }

    private void drawRouteConstraints(Graphics2D graphics, List<PlanetarySystem> routeSystems,
            PathAssessment assessment, double systemSize, int visibleSystemCount, RouteMarkerState routeState,
            double markerAlpha) {
        List<RouteConstraintMarker> markers = routeConstraintMarkers(routeSystems, assessment);
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        try {
            for (RouteConstraintMarker marker : markers) {
                if ((marker.legIndex() + 2) > visibleSystemCount) {
                    continue;
                }
                PlanetarySystem origin = routeSystems.get(marker.legIndex());
                PlanetarySystem destination = marker.destination();
                if (marker.brokenSegment()) {
                    graphics.setPaint(NAVIGATION_BLOCKED_COLOR);
                    graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                          0, new float[] { 5, 5 }, 0));
                    graphics.draw(new Line2D.Double(map2scrX(origin.getX()), map2scrY(origin.getY()),
                          map2scrX(destination.getX()), map2scrY(destination.getY())));
                }

                SystemMarkerLayout layout = createSystemMarkerLayout(destination, systemSize, routeState);
                double markerRadius = Math.max(5.0, systemSize * 0.9);
                    Point2D.Double anchor = layout.routeStatusAnchor(markerRadius, markerAlpha);
                NavigationMarkerShape shape = marker.brokenSegment()
                      ? NavigationMarkerShape.DIAMOND
                      : NavigationMarkerShape.TRIANGLE;
                if (markerAlpha > 0.0) {
                    paintLayerWithAlpha(graphics, markerAlpha, markerGraphics -> {
                        markerGraphics.setPaint(marker.brokenSegment()
                              ? NAVIGATION_BLOCKED_COLOR
                              : NAVIGATION_CAUTION_COLOR);
                        markerGraphics.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND,
                              BasicStroke.JOIN_ROUND));
                        markerGraphics.draw(createNavigationMarkerShape(shape, anchor.x, anchor.y, markerRadius));
                    });
                }
            }
        } finally {
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    private void drawMeasurement(Graphics2D graphics, NavigationInstrumentLayout instrumentLayout) {
        if (!measurementState.enabled() || (measurementState.start() == null)) {
            return;
        }
        PlanetarySystem start = measurementState.start();
        PlanetarySystem target = measurementState.end() == null
              ? measurementHoverSystem
              : measurementState.end();
        double startX = map2scrX(start.getX());
        double startY = map2scrY(start.getY());
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        Font oldFont = graphics.getFont();
        try {
            if (target != null) {
                double targetX = map2scrX(target.getX());
                double targetY = map2scrY(target.getY());
                graphics.setPaint(withAlpha(MEASUREMENT_COLOR, 210));
                graphics.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                      0, new float[] { 9, 4, 2, 4 }, 0));
                graphics.draw(new Line2D.Double(startX, startY, targetX, targetY));
                drawMeasurementEndpoint(graphics, startX, startY, "A", false);
                drawMeasurementEndpoint(graphics, targetX, targetY, "B", true);
                if (cachedMeasurementAssessment != null) {
                    drawMeasurementLabel(graphics, cachedMeasurementAssessment,
                          new Point((int) Math.round((startX + targetX) / 2.0),
                                (int) Math.round((startY + targetY) / 2.0)), instrumentLayout);
                }
            } else {
                drawMeasurementEndpoint(graphics, startX, startY, "A", false);
            }
        } finally {
            graphics.setFont(oldFont);
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    private static void drawMeasurementEndpoint(Graphics2D graphics, double x, double y, String label,
          boolean destination) {
        double radius = Math.max(5.0, UIUtil.scaleForGUI(5));
        graphics.setPaint(MEASUREMENT_COLOR);
        graphics.setStroke(new BasicStroke(2.0f));
        graphics.draw(createNavigationMarkerShape(destination ? NavigationMarkerShape.DIAMOND
              : NavigationMarkerShape.CIRCLE, x, y, radius));
        Font baseFont = graphics.getFont();
        graphics.setFont(baseFont.deriveFont(Font.BOLD, Math.max(9.0f, baseFont.getSize2D() * 0.78f)));
        graphics.drawString(label, (float) (x + radius + 3.0), (float) (y - radius - 1.0));
        graphics.setFont(baseFont);
    }

    private void drawMeasurementLabel(Graphics2D graphics, LegAssessment assessment, Point preferredCenter,
          NavigationInstrumentLayout instrumentLayout) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(MekHQ.getMHQOptions().getLocale());
        numberFormat.setMaximumFractionDigits(3);
        numberFormat.setMinimumFractionDigits(0);
        String distance = numberFormat.format(assessment.facts().distanceLy());
        String jumps = NumberFormat.getIntegerInstance(MekHQ.getMHQOptions().getLocale())
                             .format(assessment.facts().minimumStandardJumps());
        String circuitSuffix = assessment.facts().commandCircuitAssumed()
              ? resourceMap.getString("map.measurement.circuitSuffix.text")
              : "";
        String labelText = MessageFormat.format(resourceMap.getString("map.measurement.label.format"),
              distance, jumps, measurementStatusText(assessment, numberFormat), circuitSuffix);
        Font labelFont = graphics.getFont().deriveFont(Font.BOLD,
              Math.max(9.0f, Math.min(11.0f, graphics.getFont().getSize2D() * 0.82f)));
        graphics.setFont(labelFont);
        FontMetrics metrics = graphics.getFontMetrics();
        Dimension labelSize = new Dimension(metrics.stringWidth(labelText) + UIUtil.scaleForGUI(12),
              metrics.getHeight() + UIUtil.scaleForGUI(6));
        List<Rectangle> exclusions = new ArrayList<>();
        if (instrumentLayout.visible()) {
            exclusions.add(instrumentLayout.bounds().getBounds());
        }
        if (optionControl.isVisible()) {
            exclusions.add(optionControl.getBounds());
        }
        Rectangle bounds = clampMeasurementLabel(new Rectangle(0, 0, getWidth(), getHeight()), labelSize,
              preferredCenter, exclusions);
        graphics.setPaint(withAlpha(MAP_BACKGROUND_BOTTOM, 225));
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        graphics.setPaint(MEASUREMENT_COLOR);
        graphics.drawString(labelText, bounds.x + UIUtil.scaleForGUI(6),
              bounds.y + UIUtil.scaleForGUI(3) + metrics.getAscent());
    }

    private String measurementStatusText(LegAssessment assessment, NumberFormat numberFormat) {
        NavigationRouteAnalysis.FindingKind primaryFinding = assessment.findings().stream()
              .filter(finding -> finding.severity() == Severity.BLOCKED)
              .map(NavigationRouteAnalysis.Finding::kind)
              .findFirst()
              .orElseGet(() -> assessment.findings().stream()
                    .filter(finding -> finding.severity() == Severity.CAUTION)
                    .map(NavigationRouteAnalysis.Finding::kind)
                    .findFirst()
                    .orElse(null));
        if (primaryFinding != null) {
            String key = switch (primaryFinding) {
                case OUT_OF_STANDARD_JUMP_RANGE -> "map.measurement.status.rangeBlocked.text";
                case ACCESS_DENIED -> "map.measurement.status.accessBlocked.text";
                case ABANDONED_DESTINATION_AVOIDED -> "map.measurement.status.emptyBlocked.text";
                case ABANDONED_DESTINATION_ALLOWED -> "map.measurement.status.emptyCaution.text";
                case RECHARGE_IMPOSSIBLE -> "map.measurement.status.rechargeBlocked.text";
                default -> "map.measurement.status.blocked.text";
            };
            return resourceMap.getString(key);
        }
        return MessageFormat.format(resourceMap.getString("map.measurement.status.clear.format"),
              numberFormat.format(assessment.facts().rechargeHours()),
              assessment.facts().rechargeStationCount() > 0
                    ? resourceMap.getString("map.measurement.stationSuffix.text")
                    : "");
    }

    private static Shape createNavigationMarkerShape(NavigationMarkerShape markerShape, double centerX,
          double centerY, double radius) {
        return switch (markerShape) {
            case CIRCLE -> new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
            case SQUARE -> new Rectangle2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0);
            case TRIANGLE -> createRegularPolygon(centerX, centerY, radius, 3, -Math.PI / 2.0);
            case DIAMOND -> createRegularPolygon(centerX, centerY, radius, 4, 0.0);
            case HEXAGON -> createRegularPolygon(centerX, centerY, radius, 6, 0.0);
        };
    }

    static double navigationMarkerPathRadius(NavigationMarkerShape markerShape, double innerClearanceRadius,
          double strokeWidth) {
        double pathInradius = innerClearanceRadius + (strokeWidth / 2.0);
        return switch (markerShape) {
            case CIRCLE, SQUARE -> pathInradius;
            case TRIANGLE -> pathInradius / Math.cos(Math.PI / 3.0);
            case DIAMOND -> pathInradius / Math.cos(Math.PI / 4.0);
            case HEXAGON -> pathInradius / Math.cos(Math.PI / 6.0);
        };
    }

    private static Shape createRegularPolygon(double centerX, double centerY, double radius, int sides,
          double rotation) {
        GeneralPath polygon = new GeneralPath();
        for (int vertex = 0; vertex < sides; vertex++) {
            double angle = rotation + ((FULL_CIRCLE_RADIANS * vertex) / sides);
            double x = centerX + (Math.cos(angle) * radius);
            double y = centerY + (Math.sin(angle) * radius);
            if (vertex == 0) {
                polygon.moveTo(x, y);
            } else {
                polygon.lineTo(x, y);
            }
        }
        polygon.closePath();
        return polygon;
    }

    private static Color markerColor(NavigationMarkerTone tone) {
        return switch (tone) {
            case IMMEDIATE -> PLANNED_ROUTE_COLOR;
            case DEEP -> REACHABILITY_DEEP_COLOR;
            case CAUTION -> NAVIGATION_CAUTION_COLOR;
            case BLOCKED -> NAVIGATION_BLOCKED_COLOR;
        };
    }

    private void drawActiveRoute(Graphics2D graphics, Arc2D.Double arc, List<PlanetarySystem> routeSystems,
            double size, double activationProgress, double detailAlpha) {
        int legCount = routeSystems.size() - 1;
        double clampedProgress = Math.clamp(activationProgress, 0.0, 1.0);
        double routePosition = easeInOutCubic(clampedProgress) * Math.max(0, legCount);
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();

        for (int legIndex = 0; legIndex < legCount; legIndex++) {
            PlanetarySystem start = routeSystems.get(legIndex);
            PlanetarySystem end = routeSystems.get(legIndex + 1);
            double startX = map2scrX(start.getX());
            double startY = map2scrY(start.getY());
            double endX = map2scrX(end.getX());
            double endY = map2scrY(end.getY());
            double completedLegProgress = Math.clamp(routePosition - legIndex, 0.0, 1.0);
            double boundaryX = interpolate(startX, endX, completedLegProgress);
            double boundaryY = interpolate(startY, endY, completedLegProgress);

            if (completedLegProgress < 1.0) {
                graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
                      new float[] { 8, 6 }, 0));
                graphics.setPaint(PLANNED_ROUTE_COLOR);
                graphics.draw(new Line2D.Double(boundaryX, boundaryY, endX, endY));
            }
            if (completedLegProgress > 0.0) {
                graphics.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.setPaint(ACTIVE_ROUTE_COLOR);
                graphics.draw(new Line2D.Double(startX, startY, boundaryX, boundaryY));
            }
        }

        if ((clampedProgress > 0.0) && (clampedProgress < 1.0) && (legCount > 0) && (detailAlpha > 0.0)) {
            paintLayerWithAlpha(graphics, detailAlpha,
                  routeGraphics -> drawRouteActivationBoundary(routeGraphics, routeSystems, routePosition));
        }
        if (detailAlpha > 0.0) {
            paintLayerWithAlpha(graphics, detailAlpha, routeGraphics -> {
                for (int waypointIndex = 0; waypointIndex < routeSystems.size(); waypointIndex++) {
                    PlanetarySystem waypoint = routeSystems.get(waypointIndex);
                    double waypointActivation = getRouteWaypointActivation(routePosition, waypointIndex,
                          clampedProgress);
                    RouteMarkerState routeMarkerState = waypointActivation >= 0.5
                          ? RouteMarkerState.ACTIVE
                          : RouteMarkerState.PLANNED;
                    SystemMarkerLayout layout = createSystemMarkerLayout(waypoint, size, routeMarkerState);
                    drawRouteWaypoint(routeGraphics, arc, layout,
                          interpolateColor(PLANNED_ROUTE_COLOR, ACTIVE_ROUTE_COLOR, waypointActivation));
                }
            });
        }
        if (!routeSystems.isEmpty() && (detailAlpha < 1.0)) {
            SystemMarkerLayout destinationLayout = createSystemMarkerLayout(routeSystems.getLast(), size,
                  RouteMarkerState.ACTIVE);
            drawStrategicRouteEndpoint(graphics, destinationLayout, ACTIVE_ROUTE_COLOR, 1.0 - detailAlpha);
        }

        graphics.setStroke(oldStroke);
        graphics.setPaint(oldPaint);
    }

    private void drawRouteActivationBoundary(Graphics2D graphics, List<PlanetarySystem> routeSystems,
          double routePosition) {
        int legIndex = Math.min(routeSystems.size() - 2, (int) Math.floor(routePosition));
        double legProgress = routePosition - legIndex;
        PlanetarySystem start = routeSystems.get(legIndex);
        PlanetarySystem end = routeSystems.get(legIndex + 1);
        double boundaryX = interpolate(map2scrX(start.getX()), map2scrX(end.getX()), legProgress);
        double boundaryY = interpolate(map2scrY(start.getY()), map2scrY(end.getY()), legProgress);
        graphics.setPaint(withAlpha(ACTIVE_ROUTE_COLOR, 80));
        graphics.fill(new Ellipse2D.Double(boundaryX - 4.0, boundaryY - 4.0, 8.0, 8.0));
        graphics.setPaint(ACTIVE_ROUTE_FLOW_COLOR);
        graphics.fill(new Ellipse2D.Double(boundaryX - 1.7, boundaryY - 1.7, 3.4, 3.4));
    }

    private static double getRouteWaypointActivation(double routePosition, int waypointIndex,
          double activationProgress) {
        if (activationProgress >= 1.0) {
            return 1.0;
        }
        double transitionLength = 0.18;
        if (waypointIndex == 0) {
            return Math.clamp(routePosition / transitionLength, 0.0, 1.0);
        }
        return Math.clamp((routePosition - waypointIndex + transitionLength) / transitionLength, 0.0, 1.0);
    }

    private void drawActiveRouteWaypointBadges(Graphics2D graphics, List<PlanetarySystem> routeSystems,
            double size, double activationProgress, @Nullable PlanetarySystem hoveredSystem, double alpha) {
        if (alpha <= 0.0) {
            return;
        }
        int legCount = routeSystems.size() - 1;
        double clampedProgress = Math.clamp(activationProgress, 0.0, 1.0);
        double routePosition = easeInOutCubic(clampedProgress) * Math.max(0, legCount);
        paintLayerWithAlpha(graphics, alpha, badgeGraphics -> {
            for (int waypointIndex = 1; waypointIndex < routeSystems.size(); waypointIndex++) {
                PlanetarySystem waypoint = routeSystems.get(waypointIndex);
                double waypointActivation = getRouteWaypointActivation(routePosition, waypointIndex, clampedProgress);
                RouteMarkerState routeMarkerState = waypointActivation >= 0.5
                    ? RouteMarkerState.ACTIVE
                    : RouteMarkerState.PLANNED;
                SystemMarkerLayout layout = createSystemMarkerLayout(waypoint, size, routeMarkerState, hoveredSystem);
                drawRouteWaypointBadge(badgeGraphics, layout,
                    interpolateColor(PLANNED_ROUTE_COLOR, ACTIVE_ROUTE_COLOR, waypointActivation), waypointIndex,
                    alpha);
            }
        });
    }

        private void drawProposedRoute(Graphics2D graphics, Arc2D.Double arc, double size,
                int revealedSystemCount, double detailAlpha) {
                if (jumpPath.isEmpty()) {
            return;
                }

                Stroke oldStroke = graphics.getStroke();
                graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
                            new float[] { 8, 6 }, 0));
                PlanetarySystem origin = jumpPath.get(0);
                      SystemMarkerLayout originLayout = createSystemMarkerLayout(origin, size,
                          RouteMarkerState.PLANNED);
                      if (detailAlpha > 0.0) {
                        paintLayerWithAlpha(graphics, detailAlpha,
                            routeGraphics -> drawRouteWaypoint(routeGraphics, arc, originLayout,
                                PLANNED_ROUTE_COLOR));
                      }

                for (int systemIndex = 1; systemIndex < revealedSystemCount; systemIndex++) {
            PlanetarySystem systemA = jumpPath.get(systemIndex - 1);
            PlanetarySystem systemB = jumpPath.get(systemIndex);
            graphics.setPaint(PLANNED_ROUTE_COLOR);
            graphics.draw(new Line2D.Double(map2scrX(systemA.getX()), map2scrY(systemA.getY()),
                                    map2scrX(systemB.getX()), map2scrY(systemB.getY())));
            SystemMarkerLayout layout = createSystemMarkerLayout(systemB, size, RouteMarkerState.PLANNED);
            if (detailAlpha > 0.0) {
                paintLayerWithAlpha(graphics, detailAlpha,
                      routeGraphics -> drawRouteWaypoint(routeGraphics, arc, layout, PLANNED_ROUTE_COLOR));
            }
                }

                if (proposedRouteAnimationTimer.isRunning() && (revealedSystemCount < jumpPath.size())) {
            PlanetarySystem systemA = jumpPath.get(revealedSystemCount - 1);
            PlanetarySystem systemB = jumpPath.get(revealedSystemCount);
            double legProgress = getCurrentProposedRouteLegProgress();
            double startX = map2scrX(systemA.getX());
            double startY = map2scrY(systemA.getY());
            double endX = interpolate(startX, map2scrX(systemB.getX()), legProgress);
            double endY = interpolate(startY, map2scrY(systemB.getY()), legProgress);
            graphics.setPaint(PLANNED_ROUTE_COLOR);
            graphics.draw(new Line2D.Double(startX, startY, endX, endY));
        }
                if ((revealedSystemCount > 0) && (detailAlpha < 1.0)) {
                    int destinationIndex = Math.min(revealedSystemCount, jumpPath.size()) - 1;
                    SystemMarkerLayout destinationLayout = createSystemMarkerLayout(jumpPath.get(destinationIndex),
                          size, RouteMarkerState.PLANNED);
                    drawStrategicRouteEndpoint(graphics, destinationLayout, PLANNED_ROUTE_COLOR,
                          1.0 - detailAlpha);
                }
                graphics.setStroke(oldStroke);
        }

    private static void drawStrategicRouteEndpoint(Graphics2D graphics, SystemMarkerLayout layout, Color color,
          double alpha) {
        if (alpha <= 0.0) {
            return;
        }
        double radius = Math.max(4.2, layout.ownershipRadius() + 1.0);
        Ellipse2D.Double ring = new Ellipse2D.Double(layout.centerX() - radius, layout.centerY() - radius,
              radius * 2.0, radius * 2.0);
        paintLayerWithAlpha(graphics, alpha, endpointGraphics -> {
            endpointGraphics.setPaint(withAlpha(Color.BLACK, 220));
            endpointGraphics.setStroke(new BasicStroke(4.0f));
            endpointGraphics.draw(ring);
            endpointGraphics.setPaint(color);
            endpointGraphics.setStroke(new BasicStroke(1.8f));
            endpointGraphics.draw(ring);
        });
    }

        private void drawRouteWaypointBadges(Graphics2D graphics, JumpPath path, double size, Color color,
                  int visibleSystemCount, @Nullable PlanetarySystem hoveredSystem, double alpha) {
                if (alpha <= 0.0) {
            return;
        }
                int waypointLimit = Math.min(path.size(), visibleSystemCount);
                paintLayerWithAlpha(graphics, alpha, badgeGraphics -> {
                    int waypointNumber = 1;
                    for (int waypointIndex = 1; waypointIndex < waypointLimit; waypointIndex++) {
                        PlanetarySystem waypoint = path.get(waypointIndex);
                        if (!routePlanningHandler.isRequestedWaypoint(waypoint)) {
                            continue;
                        }
                        SystemMarkerLayout layout = createSystemMarkerLayout(waypoint, size, RouteMarkerState.PLANNED,
                              hoveredSystem);
                        drawRouteWaypointBadge(badgeGraphics, layout, color, waypointNumber++, alpha);
                    }
                });
    }

        private static void drawRouteWaypointBadge(Graphics2D graphics, SystemMarkerLayout layout, Color color,
            int waypointNumber, double expansion) {
        Font oldFont = graphics.getFont();
        Paint oldPaint = graphics.getPaint();
        Font badgeFont = oldFont.deriveFont(Font.BOLD, Math.max(9.0f, Math.min(11.0f, oldFont.getSize2D())));
        graphics.setFont(badgeFont);

        String badgeText = Integer.toString(waypointNumber);
        FontMetrics metrics = graphics.getFontMetrics();
          Rectangle2D textBounds = metrics.getStringBounds(badgeText, graphics);
          int badgeDiameter = Math.max(16,
              Math.max(metrics.getHeight() + 2, (int) Math.ceil(textBounds.getWidth()) + 8));
                Point2D.Double badgeAnchor = layout.routeBadgeAnchor(badgeDiameter, expansion);
                double badgeCenterX = badgeAnchor.x;
                double badgeCenterY = badgeAnchor.y;
        Ellipse2D.Double badge = new Ellipse2D.Double(badgeCenterX - (badgeDiameter / 2.0),
              badgeCenterY - (badgeDiameter / 2.0), badgeDiameter, badgeDiameter);

        graphics.setPaint(withAlpha(MAP_BACKGROUND_BOTTOM, 235));
        graphics.fill(badge);
        graphics.setPaint(color);
        Point2D.Double baseline = centeredGlyphBaseline(graphics, badgeText, badgeCenterX, badgeCenterY);
        graphics.drawString(badgeText, (float) baseline.x, (float) baseline.y);

        graphics.setFont(oldFont);
        graphics.setPaint(oldPaint);
    }

    static Point2D.Double centeredGlyphBaseline(Graphics2D graphics, String text,
          double centerX, double centerY) {
        GlyphVector glyphs = graphics.getFont().createGlyphVector(graphics.getFontRenderContext(), text);
        Rectangle2D visualBounds = glyphs.getVisualBounds();
        return new Point2D.Double(centerX - visualBounds.getCenterX(), centerY - visualBounds.getCenterY());
    }

    private static void drawSystemLabel(Graphics2D graphics, String baseText, @Nullable String suffix,
          float x, float y, Color color, double baseAlpha, double suffixAlpha) {
        drawOutlinedTextWithAlpha(graphics, baseText, x, y, color, baseAlpha);
        if ((suffix == null) || (suffixAlpha <= 0.0)) {
            return;
        }
        float suffixX = x + graphics.getFontMetrics().stringWidth(baseText);
        drawOutlinedTextWithAlpha(graphics, suffix, suffixX, y, color, suffixAlpha);
    }

    private static void drawOutlinedTextWithAlpha(Graphics2D graphics, String text, float x, float y,
          Color color, double alpha) {
        if (alpha <= 0.0) {
            return;
        }
        paintLayerWithAlpha(graphics, alpha, labelGraphics -> {
            labelGraphics.setPaint(Color.BLACK);
            labelGraphics.drawString(text, x - 1f, y - 1f);
            labelGraphics.drawString(text, x + 1f, y - 1f);
            labelGraphics.drawString(text, x + 1f, y + 1f);
            labelGraphics.drawString(text, x - 1f, y + 1f);
            labelGraphics.setPaint(color);
            labelGraphics.drawString(text, x, y);
        });
    }

    private static @Nullable BufferedImage loadCurrentLocationIcon() {
        try {
            BufferedImage source = ImageIO.read(new File(CURRENT_LOCATION_ICON_PATH));
            if (source == null) {
                LOGGER.error("Unable to read current-location JumpShip image: {}", CURRENT_LOCATION_ICON_PATH);
                return null;
            }

            BufferedImage tinted = new BufferedImage(source.getWidth(), source.getHeight(),
                  BufferedImage.TYPE_INT_ARGB);
            for (int imageY = 0; imageY < source.getHeight(); imageY++) {
                for (int imageX = 0; imageX < source.getWidth(); imageX++) {
                    int sourcePixel = source.getRGB(imageX, imageY);
                    int alpha = sourcePixel >>> 24;
                    if (alpha == 0) {
                        continue;
                    }
                    int red = (sourcePixel >>> 16) & 0xff;
                    int green = (sourcePixel >>> 8) & 0xff;
                    int blue = sourcePixel & 0xff;
                    int luminance = ((red * 54) + (green * 183) + (blue * 19)) >>> 8;
                    double shade = 0.35 + (luminance / 255.0 * 0.65);
                    int tintedRed = (int) Math.round(CURRENT_LOCATION_COLOR.getRed() * shade);
                    int tintedGreen = (int) Math.round(CURRENT_LOCATION_COLOR.getGreen() * shade);
                    int tintedBlue = (int) Math.round(CURRENT_LOCATION_COLOR.getBlue() * shade);
                    tinted.setRGB(imageX, imageY,
                          (alpha << 24) | (tintedRed << 16) | (tintedGreen << 8) | tintedBlue);
                }
            }

            BufferedImage scaled = new BufferedImage(CURRENT_LOCATION_ICON_SIZE, CURRENT_LOCATION_ICON_SIZE,
                  BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.translate(0, CURRENT_LOCATION_ICON_SIZE);
            graphics.rotate(-Math.PI / 2.0);
            graphics.drawImage(tinted, 0, 0, CURRENT_LOCATION_ICON_SIZE, CURRENT_LOCATION_ICON_SIZE, null);
            graphics.dispose();
            return scaled;
        } catch (IOException exception) {
            LOGGER.error("Unable to load current-location JumpShip image", exception);
            return null;
        }
    }

    private static void drawCurrentLocationMarker(Graphics2D graphics, SystemMarkerLayout layout,
          double expansion) {
        Point2D.Double shipAnchor = layout.shipAnchor(expansion);
        drawJumpShipIcon(graphics, shipAnchor.x, shipAnchor.y, 0.0);
    }

    private static void drawStrategicCurrentLocationMarker(Graphics2D graphics, SystemMarkerLayout layout) {
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        try {
            double radius = Math.max(3.8, layout.ownershipRadius() + 1.0);
            Ellipse2D.Double beacon = new Ellipse2D.Double(layout.centerX() - radius, layout.centerY() - radius,
                  radius * 2.0, radius * 2.0);
            graphics.setPaint(withAlpha(CURRENT_LOCATION_COLOR, 70));
            graphics.setStroke(new BasicStroke(4.0f));
            graphics.draw(beacon);
            graphics.setPaint(CURRENT_LOCATION_COLOR);
            graphics.setStroke(new BasicStroke(1.4f));
            graphics.draw(beacon);
        } finally {
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

    private static void drawStrategicFocusMarker(Graphics2D graphics, SystemMarkerLayout layout, Color color) {
        double radius = Math.max(4.0, layout.ownershipRadius() + 0.8);
        Ellipse2D.Double ring = new Ellipse2D.Double(layout.centerX() - radius, layout.centerY() - radius,
              radius * 2.0, radius * 2.0);
        Stroke oldStroke = graphics.getStroke();
        Paint oldPaint = graphics.getPaint();
        try {
            graphics.setPaint(withAlpha(Color.BLACK, 220));
            graphics.setStroke(new BasicStroke(4.0f));
            graphics.draw(ring);
            graphics.setPaint(color);
            graphics.setStroke(new BasicStroke(1.8f));
            graphics.draw(ring);
        } finally {
            graphics.setStroke(oldStroke);
            graphics.setPaint(oldPaint);
        }
    }

        private void drawSystemHopMarker(Graphics2D graphics, double size, @Nullable PlanetarySystem hoveredSystem) {
        if ((systemHopOriginId == null) || (systemHopDestinationId == null)) {
            return;
        }
        PlanetarySystem origin = campaign.getSystemById(systemHopOriginId);
        PlanetarySystem destination = campaign.getSystemById(systemHopDestinationId);
        if ((origin == null) || (destination == null)) {
            return;
        }

          double progress = Math.clamp(systemHopProgress, 0.0, 1.0);
          if (progress < SYSTEM_HOP_DEPARTURE_END_PROGRESS) {
            double phaseProgress = progress / SYSTEM_HOP_DEPARTURE_END_PROGRESS;
                        SystemMarkerLayout originLayout = createSystemMarkerLayout(origin, size, RouteMarkerState.ACTIVE,
                                    hoveredSystem);
            drawSystemHopEndpoint(graphics, originLayout, phaseProgress, true);
          } else if (progress >= SYSTEM_HOP_ARRIVAL_START_PROGRESS) {
            double phaseProgress = (progress - SYSTEM_HOP_ARRIVAL_START_PROGRESS)
                / (1.0 - SYSTEM_HOP_ARRIVAL_START_PROGRESS);
            SystemMarkerLayout destinationLayout = createSystemMarkerLayout(destination, size,
                  RouteMarkerState.ACTIVE, hoveredSystem);
            drawSystemHopEndpoint(graphics, destinationLayout, phaseProgress, false);
          }
    }

        private static void drawSystemHopEndpoint(Graphics2D graphics, SystemMarkerLayout layout,
            double phaseProgress, boolean departing) {
          double easedProgress = easeInOutCubic(phaseProgress);
          double shipAlpha = departing ? 1.0 - easedProgress : easedProgress;
          Point2D.Double shipAnchor = layout.shipAnchor();
          double shimmerAlpha = Math.sin(Math.PI * phaseProgress);

          drawSystemHopShimmer(graphics, shipAnchor.x, shipAnchor.y, shimmerAlpha);
          paintLayerWithAlpha(graphics, shipAlpha,
              shipGraphics -> drawJumpShipIcon(shipGraphics, shipAnchor.x, shipAnchor.y, 0.0));
        }

        private static void drawSystemHopShimmer(Graphics2D graphics, double centerX, double centerY, double alpha) {
          if (alpha <= 0.0) {
            return;
          }
        Paint oldPaint = graphics.getPaint();
          Stroke oldStroke = graphics.getStroke();
          double halfWidth = CURRENT_LOCATION_ICON_SIZE * 0.48;
          double spread = interpolate(2.0, 7.0, alpha);
          graphics.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
          graphics.setPaint(withAlpha(PLANNED_ROUTE_COLOR, scaleAlpha(150, alpha)));
          graphics.draw(new Line2D.Double(centerX - halfWidth - spread, centerY - 5.0,
              centerX + halfWidth - spread, centerY - 5.0));
          graphics.setPaint(withAlpha(ACTIVE_ROUTE_FLOW_COLOR, scaleAlpha(175, alpha)));
          graphics.draw(new Line2D.Double(centerX - halfWidth + spread, centerY,
              centerX + halfWidth + spread, centerY));
          graphics.setPaint(withAlpha(PLANNED_ROUTE_COLOR, scaleAlpha(120, alpha)));
          graphics.draw(new Line2D.Double(centerX - halfWidth - spread, centerY + 5.0,
              centerX + halfWidth - spread, centerY + 5.0));
          graphics.setStroke(oldStroke);
        graphics.setPaint(oldPaint);
    }

    private static void drawJumpShipIcon(Graphics2D graphics, double centerX, double centerY, double rotation) {
        Graphics2D shipGraphics = (Graphics2D) graphics.create();
        try {
            shipGraphics.translate(centerX, centerY);
            shipGraphics.rotate(rotation);

            if (CURRENT_LOCATION_ICON != null) {
                int iconOffset = -CURRENT_LOCATION_ICON_SIZE / 2;
                shipGraphics.drawImage(CURRENT_LOCATION_ICON, iconOffset, iconOffset, null);
            } else {
                drawJumpShipFallback(shipGraphics);
            }
        } finally {
            shipGraphics.dispose();
        }
    }

    private static void drawJumpShipFallback(Graphics2D graphics) {
        GeneralPath locationChevron = new GeneralPath();
        locationChevron.moveTo(-5.0, -2.0);
        locationChevron.lineTo(0.0, 3.0);
        locationChevron.lineTo(5.0, -2.0);
        graphics.setPaint(withAlpha(MAP_BACKGROUND_BOTTOM, 230));
        graphics.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(locationChevron);
        graphics.setPaint(CURRENT_LOCATION_COLOR);
        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(locationChevron);
    }

        private static void drawSelectedSystemMarker(Graphics2D graphics, SystemMarkerLayout layout,
                    String systemId, double ambientElapsedSeconds) {
        Stroke oldStroke = graphics.getStroke();
                double radius = layout.selectedRadius();
        double bracketLength = Math.min(5.0, radius * 0.47);
                GeneralPath brackets = createCornerBrackets(layout.centerX(), layout.centerY(), radius, bracketLength);

        double breath = getAmbientWave(ambientElapsedSeconds, getStableHash(systemId), 0x3c6ef372, 3.4, 4.0);
        graphics.setPaint(withAlpha(SELECTED_SYSTEM_COLOR, scaleAlpha(70, 1.0 + (breath * 0.09))));
        graphics.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        graphics.draw(brackets);
        graphics.setPaint(SELECTED_SYSTEM_COLOR);
        graphics.setStroke(new BasicStroke(2.3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        graphics.draw(brackets);
        graphics.setStroke(oldStroke);
    }

        private static void drawSelectionAnimationMarker(Graphics2D graphics, SystemMarkerLayout layout,
                    double progress) {
        double easedProgress = easeOutCubic(progress);
                double hoveredRadius = layout.hoveredRadius();
                double selectedRadius = layout.selectedRadius();
        double radius = interpolate(hoveredRadius, selectedRadius, easedProgress);
        double hoveredBracketLength = Math.min(5.0, hoveredRadius * 0.45);
        double selectedBracketLength = Math.min(5.0, selectedRadius * 0.47);
        double bracketLength = interpolate(hoveredBracketLength, selectedBracketLength, easedProgress);
        GeneralPath brackets = createCornerBrackets(layout.centerX(), layout.centerY(), radius, bracketLength);

        Stroke oldStroke = graphics.getStroke();
        int glowAlpha = (int) Math.round(interpolate(0.0, 70.0, easedProgress));
        float glowWidth = (float) interpolate(1.1, 4.0, easedProgress);
        graphics.setPaint(withAlpha(SELECTED_SYSTEM_COLOR, glowAlpha));
        graphics.setStroke(new BasicStroke(glowWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        graphics.draw(brackets);

        Color hoveredColor = withAlpha(HOVERED_SYSTEM_COLOR, 190);
        graphics.setPaint(interpolateColor(hoveredColor, SELECTED_SYSTEM_COLOR, easedProgress));
        float foregroundWidth = (float) interpolate(1.1, 2.3, easedProgress);
        graphics.setStroke(new BasicStroke(foregroundWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.draw(brackets);
        graphics.setStroke(oldStroke);
    }

    private static void drawHoveredSystemMarker(Graphics2D graphics, SystemMarkerLayout layout) {
        Stroke oldStroke = graphics.getStroke();
        graphics.setPaint(withAlpha(HOVERED_SYSTEM_COLOR, 190));
        graphics.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double radius = layout.hoveredRadius();
        double bracketLength = Math.min(5.0, radius * 0.45);
        graphics.draw(createCornerBrackets(layout.centerX(), layout.centerY(), radius, bracketLength));
        graphics.setStroke(oldStroke);
    }

    private static double easeOutCubic(double progress) {
        double clampedProgress = Math.clamp(progress, 0.0, 1.0);
        return 1.0 - Math.pow(1.0 - clampedProgress, 3.0);
    }

    private static double easeInOutCubic(double progress) {
        double clampedProgress = Math.clamp(progress, 0.0, 1.0);
        if (clampedProgress < 0.5) {
            return 4.0 * clampedProgress * clampedProgress * clampedProgress;
        }
        return 1.0 - Math.pow(-2.0 * clampedProgress + 2.0, 3.0) / 2.0;
    }

    private static double interpolate(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static double getSemanticZoomReference(double configuredNameThreshold) {
        return configuredNameThreshold > 0.0 ? configuredNameThreshold : 3.0;
    }

    private static double fadeBetween(double scale, double start, double end) {
        if (end <= start) {
            return scale >= end ? 1.0 : 0.0;
        }
        double progress = Math.clamp((scale - start) / (end - start), 0.0, 1.0);
        return progress * progress * (3.0 - (2.0 * progress));
    }

    private static Color interpolateColor(Color start, Color end, double progress) {
        double clampedProgress = Math.clamp(progress, 0.0, 1.0);
        int red = (int) Math.round(interpolate(start.getRed(), end.getRed(), clampedProgress));
        int green = (int) Math.round(interpolate(start.getGreen(), end.getGreen(), clampedProgress));
        int blue = (int) Math.round(interpolate(start.getBlue(), end.getBlue(), clampedProgress));
        int alpha = (int) Math.round(interpolate(start.getAlpha(), end.getAlpha(), clampedProgress));
        return new Color(red, green, blue, alpha);
    }

    private static GeneralPath createCornerBrackets(double x, double y, double radius, double bracketLength) {
        GeneralPath brackets = new GeneralPath();
        brackets.moveTo(x - radius + bracketLength, y - radius);
        brackets.lineTo(x - radius, y - radius);
        brackets.lineTo(x - radius, y - radius + bracketLength);
        brackets.moveTo(x + radius - bracketLength, y - radius);
        brackets.lineTo(x + radius, y - radius);
        brackets.lineTo(x + radius, y - radius + bracketLength);
        brackets.moveTo(x + radius, y + radius - bracketLength);
        brackets.lineTo(x + radius, y + radius);
        brackets.lineTo(x + radius - bracketLength, y + radius);
        brackets.moveTo(x - radius + bracketLength, y + radius);
        brackets.lineTo(x - radius, y + radius);
        brackets.lineTo(x - radius, y + radius - bracketLength);
        return brackets;
    }

    @SuppressWarnings("removal")
    private static Color getSpectralColor(@Nullable StarType star) {
        if (star == null) {
            return new Color(210, 220, 225);
        }
        return switch (star.getSpectralClass()) {
            case StarType.SPECTRAL_O -> new Color(145, 170, 255);
            case StarType.SPECTRAL_B -> new Color(170, 195, 255);
            case StarType.SPECTRAL_A -> new Color(210, 222, 255);
            case StarType.SPECTRAL_F -> new Color(242, 245, 255);
            case StarType.SPECTRAL_G -> new Color(255, 235, 175);
            case StarType.SPECTRAL_K -> new Color(255, 190, 120);
            case StarType.SPECTRAL_M -> new Color(255, 132, 92);
            case StarType.SPECTRAL_L -> new Color(205, 105, 72);
            case StarType.SPECTRAL_T -> new Color(135, 165, 190);
            case StarType.SPECTRAL_Y -> new Color(150, 145, 125);
            case StarType.SPECTRAL_D -> new Color(225, 235, 255);
            default -> new Color(205, 215, 220);
        };
    }

    @SuppressWarnings("removal")
    private static double getLuminosityClassVisualScale(@Nullable StarType star) {
        if ((star == null) || (star.getLuminosity() == null)) {
            return 1.0;
        }
        return switch (star.getLuminosity()) {
            case StarType.LUM_0 -> 1.42;
            case StarType.LUM_IA -> 1.4;
            case StarType.LUM_IAB, StarType.LUM_I -> 1.37;
            case StarType.LUM_IB -> 1.35;
            case StarType.LUM_II_EVOLVED, StarType.LUM_II -> 1.25;
            case StarType.LUM_III_EVOLVED, StarType.LUM_III -> 1.16;
            case StarType.LUM_IV_EVOLVED, StarType.LUM_IV -> 1.08;
            case StarType.LUM_V_EVOLVED, StarType.LUM_V -> 1.0;
            case StarType.LUM_VI -> 0.86;
            case StarType.LUM_VI_PLUS, StarType.LUM_VII -> 0.75;
            default -> 1.0;
        };
    }


    private static double getAmbientWave(double elapsedSeconds, int stableHash, int salt,
          double minimumPeriodSeconds, double maximumPeriodSeconds) {
        double periodSeconds = interpolate(minimumPeriodSeconds, maximumPeriodSeconds,
              getStableUnit(stableHash, salt));
        double phaseOffset = getStableUnit(stableHash, salt ^ 0x9e3779b9) * FULL_CIRCLE_RADIANS;
        return Math.sin((elapsedSeconds / periodSeconds * FULL_CIRCLE_RADIANS) + phaseOffset);
    }

    private static double getStableUnit(int stableHash, int salt) {
        int mixedHash = stableHash ^ salt;
        mixedHash ^= mixedHash >>> 16;
        mixedHash *= 0x7feb352d;
        mixedHash ^= mixedHash >>> 15;
        mixedHash *= 0x846ca68b;
        mixedHash ^= mixedHash >>> 16;
        return Integer.toUnsignedLong(mixedHash) / 4_294_967_296.0;
    }

    private static int getStableHash(@Nullable String stableId) {
        return stableId == null ? 0 : stableId.hashCode();
    }

    private static double fractionalPart(double value) {
        return value - Math.floor(value);
    }

    private static int scaleAlpha(int baseAlpha, double scale) {
        return Math.clamp((int) Math.round(baseAlpha * scale), 0, 255);
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static Color brighten(Color color) {
        return new Color((color.getRed() + 255) / 2, (color.getGreen() + 255) / 2,
              (color.getBlue() + 255) / 2);
    }

    private void drawMapBackground(Graphics2D graphics, int width, int height) {
        graphics.setPaint(new GradientPaint(0, 0, MAP_BACKGROUND_TOP, 0, height, MAP_BACKGROUND_BOTTOM));
        graphics.fillRect(0, 0, width, height);

        double spacing = getGridSpacing(conf.scale);
        double left = scr2mapX(0);
        double right = scr2mapX(width);
        double bottom = scr2mapY(height);
        double top = scr2mapY(0);
        long firstColumn = (long) Math.ceil(left / spacing);
        long lastColumn = (long) Math.floor(right / spacing);
        long firstRow = (long) Math.ceil(bottom / spacing);
        long lastRow = (long) Math.floor(top / spacing);

        Stroke oldStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(1.0f));
        for (long column = firstColumn; column <= lastColumn; column++) {
            graphics.setPaint((Math.floorMod(column, 5) == 0) ? MAP_GRID_MAJOR : MAP_GRID_MINOR);
            double x = map2scrX(column * spacing);
            graphics.draw(new Line2D.Double(x, 0, x, height));
        }
        for (long row = firstRow; row <= lastRow; row++) {
            graphics.setPaint((Math.floorMod(row, 5) == 0) ? MAP_GRID_MAJOR : MAP_GRID_MINOR);
            double y = map2scrY(row * spacing);
            graphics.draw(new Line2D.Double(0, y, width, y));
        }
        graphics.setStroke(oldStroke);
    }

    private static double getGridSpacing(double scale) {
        double targetSpacing = GRID_TARGET_SPACING / Math.max(scale, 0.0001);
        double magnitude = Math.pow(10, Math.floor(Math.log10(targetSpacing)));
        double normalizedSpacing = targetSpacing / magnitude;
        if (normalizedSpacing <= 2) {
            return 2 * magnitude;
        } else if (normalizedSpacing <= 5) {
            return 5 * magnitude;
        }
        return 10 * magnitude;
    }

    static NavigationInstrumentLayout createNavigationInstrumentLayout(
          int viewportWidth, int viewportHeight, double mapScale) {
        int margin = Math.max(1, UIUtil.scaleForGUI(NAVIGATION_INSTRUMENT_MARGIN));
        int instrumentWidth = Math.max(1, UIUtil.scaleForGUI(NAVIGATION_INSTRUMENT_WIDTH));
        int instrumentHeight = Math.max(1, UIUtil.scaleForGUI(NAVIGATION_INSTRUMENT_HEIGHT));
        if (!Double.isFinite(mapScale) || (mapScale <= 0.0)
              || (viewportWidth < instrumentWidth + (2 * margin))
              || (viewportHeight < instrumentHeight + (2 * margin))) {
            return NavigationInstrumentLayout.hidden();
        }

        double x = margin;
        double y = viewportHeight - margin - instrumentHeight;
        double barStartX = x + UIUtil.scaleForGUI(NAVIGATION_SCALE_BAR_INSET);
        double barPixelWidth = Math.max(1, UIUtil.scaleForGUI(NAVIGATION_SCALE_BAR_WIDTH));
        double distanceLy = barPixelWidth / mapScale;

        return new NavigationInstrumentLayout(true, x, y, instrumentWidth, instrumentHeight,
              x + UIUtil.scaleForGUI(NAVIGATION_COMPASS_CENTER_X),
              y + UIUtil.scaleForGUI(NAVIGATION_COMPASS_CENTER_Y),
              Math.max(1, UIUtil.scaleForGUI(NAVIGATION_COMPASS_RADIUS)),
              barStartX, barStartX + barPixelWidth,
              y + UIUtil.scaleForGUI(NAVIGATION_SCALE_BAR_Y), distanceLy);
    }

    static void drawNavigationInstrument(Graphics2D graphics, NavigationInstrumentLayout layout) {
        if (!layout.visible()) {
            return;
        }

        Graphics2D instrumentGraphics = (Graphics2D) graphics.create();
        try {
            instrumentGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);
            instrumentGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            drawNavigationCompass(instrumentGraphics, layout);
            drawNavigationScale(instrumentGraphics, layout);
        } finally {
            instrumentGraphics.dispose();
        }
    }

    private static void drawNavigationCompass(Graphics2D graphics, NavigationInstrumentLayout layout) {
        double centerX = layout.compassCenterX();
        double centerY = layout.compassCenterY();
        double radius = layout.compassRadius();
        float lineWidth = Math.max(1.0f, UIUtil.scaleForGUI(1));
        double shadowOffset = Math.max(1, UIUtil.scaleForGUI(1));

        graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));
        graphics.setPaint(NAVIGATION_INSTRUMENT_SHADOW);
        graphics.draw(new Line2D.Double(centerX - radius + shadowOffset, centerY + shadowOffset,
              centerX + radius + shadowOffset, centerY + shadowOffset));
        graphics.draw(new Line2D.Double(centerX + shadowOffset, centerY - radius + shadowOffset,
              centerX + shadowOffset, centerY + radius + shadowOffset));
        graphics.setPaint(LAYER_CONTROL_BUTTON_ICON);
        graphics.draw(new Line2D.Double(centerX - radius, centerY, centerX + radius, centerY));
        graphics.draw(new Line2D.Double(centerX, centerY - radius, centerX, centerY + radius));

        double pointerSize = Math.max(3, UIUtil.scaleForGUI(4));
        GeneralPath pointers = new GeneralPath();
        pointers.moveTo(centerX, centerY - radius);
        pointers.lineTo(centerX - pointerSize, centerY - radius + (pointerSize * 1.6));
        pointers.lineTo(centerX + pointerSize, centerY - radius + (pointerSize * 1.6));
        pointers.closePath();
        pointers.moveTo(centerX, centerY + radius);
        pointers.lineTo(centerX - pointerSize, centerY + radius - (pointerSize * 1.6));
        pointers.lineTo(centerX + pointerSize, centerY + radius - (pointerSize * 1.6));
        pointers.closePath();
        pointers.moveTo(centerX - radius, centerY);
        pointers.lineTo(centerX - radius + (pointerSize * 1.6), centerY - pointerSize);
        pointers.lineTo(centerX - radius + (pointerSize * 1.6), centerY + pointerSize);
        pointers.closePath();
        pointers.moveTo(centerX + radius, centerY);
        pointers.lineTo(centerX + radius - (pointerSize * 1.6), centerY - pointerSize);
        pointers.lineTo(centerX + radius - (pointerSize * 1.6), centerY + pointerSize);
        pointers.closePath();
        graphics.fill(pointers);

        double centerMarkerRadius = Math.max(1.5, UIUtil.scaleForGUI(2));
        graphics.setPaint(LAYER_CONTROL_TEXT);
        graphics.fill(new Ellipse2D.Double(centerX - centerMarkerRadius, centerY - centerMarkerRadius,
              centerMarkerRadius * 2.0, centerMarkerRadius * 2.0));

        Font baseFont = ObjectUtility.nonNull(UIManager.getFont("Label.font"), graphics.getFont());
        float fontSize = Math.max(UIUtil.scaleForGUI(9), baseFont.getSize2D() * 0.78f);
        graphics.setFont(baseFont.deriveFont(Font.BOLD, fontSize));
        FontMetrics metrics = graphics.getFontMetrics();
        double labelGap = Math.max(4, UIUtil.scaleForGUI(7));
        double horizontalBaseline = centerY + ((metrics.getAscent() - metrics.getDescent()) / 2.0);
        drawCenteredNavigationText(graphics, layout.corewardLabel(), centerX,
              centerY - radius - labelGap, LAYER_CONTROL_BUTTON_ICON);
        drawCenteredNavigationText(graphics, layout.rimwardLabel(), centerX,
              centerY + radius + labelGap + metrics.getAscent(), LAYER_CONTROL_TEXT);
        drawRightAlignedNavigationText(graphics, layout.antiSpinwardLabel(),
              centerX - radius - labelGap, horizontalBaseline, LAYER_CONTROL_TEXT);
        drawNavigationText(graphics, layout.spinwardLabel(), centerX + radius + labelGap,
              horizontalBaseline, LAYER_CONTROL_TEXT);
    }

    private static void drawNavigationScale(Graphics2D graphics, NavigationInstrumentLayout layout) {
        Font baseFont = ObjectUtility.nonNull(UIManager.getFont("Label.font"), graphics.getFont());
        float headingFontSize = Math.max(UIUtil.scaleForGUI(9), baseFont.getSize2D() * 0.78f);
        graphics.setFont(baseFont.deriveFont(Font.BOLD, headingFontSize));
        double headingBaseline = layout.scaleBarY() - Math.max(7, UIUtil.scaleForGUI(9));
        drawNavigationText(graphics, "DISTANCE", layout.scaleBarStartX(), headingBaseline,
              MAP_LEGEND_MUTED_TEXT);

        float lineWidth = Math.max(1.0f, UIUtil.scaleForGUI(1));
        double tickRadius = Math.max(3, UIUtil.scaleForGUI(4));
        graphics.setStroke(new BasicStroke(lineWidth + Math.max(1.0f, UIUtil.scaleForGUI(2)),
              BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        graphics.setPaint(NAVIGATION_INSTRUMENT_SHADOW);
        graphics.draw(new Line2D.Double(layout.scaleBarStartX(), layout.scaleBarY(),
              layout.scaleBarEndX(), layout.scaleBarY()));
        graphics.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        graphics.setPaint(LAYER_CONTROL_BUTTON_ICON);
        graphics.draw(new Line2D.Double(layout.scaleBarStartX(), layout.scaleBarY(),
              layout.scaleBarEndX(), layout.scaleBarY()));
        graphics.draw(new Line2D.Double(layout.scaleBarStartX(), layout.scaleBarY() - tickRadius,
              layout.scaleBarStartX(), layout.scaleBarY() + tickRadius));
        graphics.draw(new Line2D.Double(layout.scaleBarEndX(), layout.scaleBarY() - tickRadius,
              layout.scaleBarEndX(), layout.scaleBarY() + tickRadius));

        graphics.setFont(baseFont.deriveFont(Font.PLAIN, headingFontSize));
        FontMetrics metrics = graphics.getFontMetrics();
        double valueBaseline = layout.scaleBarY() + metrics.getAscent() + Math.max(2, UIUtil.scaleForGUI(3));
        drawCenteredNavigationText(graphics, "0", layout.scaleBarStartX(), valueBaseline,
              MAP_LEGEND_MUTED_TEXT);
        drawCenteredNavigationText(graphics, layout.distanceLabel(), layout.scaleBarEndX(), valueBaseline,
              LAYER_CONTROL_TEXT);
    }

    private static void drawCenteredNavigationText(
          Graphics2D graphics, String text, double centerX, double baseline, Color color) {
        double x = centerX - (graphics.getFontMetrics().stringWidth(text) / 2.0);
        drawNavigationText(graphics, text, x, baseline, color);
    }

    private static void drawRightAlignedNavigationText(
          Graphics2D graphics, String text, double rightX, double baseline, Color color) {
        double x = rightX - graphics.getFontMetrics().stringWidth(text);
        drawNavigationText(graphics, text, x, baseline, color);
    }

    private static void drawNavigationText(
          Graphics2D graphics, String text, double x, double baseline, Color color) {
        double shadowOffset = Math.max(1, UIUtil.scaleForGUI(1));
        graphics.setPaint(NAVIGATION_INSTRUMENT_SHADOW);
        graphics.drawString(text, (float) (x + shadowOffset), (float) (baseline + shadowOffset));
        graphics.setPaint(color);
        graphics.drawString(text, (float) x, (float) baseline);
    }

    record NavigationInstrumentLayout(
          boolean visible,
          double x,
          double y,
          double width,
          double height,
          double compassCenterX,
          double compassCenterY,
          double compassRadius,
          double scaleBarStartX,
          double scaleBarEndX,
          double scaleBarY,
                    double distanceLy) {

                static NavigationInstrumentLayout hidden() {
            return new NavigationInstrumentLayout(false, 0.0, 0.0, 0.0, 0.0,
                                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        Rectangle2D.Double bounds() {
            return new Rectangle2D.Double(x, y, width, height);
        }

        double scaleBarPixelWidth() {
            return scaleBarEndX - scaleBarStartX;
        }

        String corewardLabel() {
            return "COREWARD";
        }

        String rimwardLabel() {
            return "RIMWARD";
        }

        String antiSpinwardLabel() {
            return "ANTI-SPINWARD";
        }

        String spinwardLabel() {
            return "SPINWARD";
        }

        String distanceLabel() {
            BigDecimal exactDistance = BigDecimal.valueOf(distanceLy);
            BigDecimal displayedDistance = exactDistance.round(NAVIGATION_DISTANCE_FORMAT).stripTrailingZeros();
            String approximationMarker = displayedDistance.compareTo(exactDistance) == 0 ? "" : "~";
            return approximationMarker + displayedDistance.toPlainString() + " LY";
        }

    }

    /**
     * Computes the map-coordinate from the screen coordinate system
     */
    private double scr2mapX(double x) {
        return (x - getWidth() / 2.0) / conf.scale - conf.centerX;
    }

    private double map2scrX(double x) {
        return getWidth() / 2.0 + (x + conf.centerX) * conf.scale;
    }

    private double scr2mapY(double y) {
        return (getHeight() / 2.0 - y) / conf.scale + conf.centerY;
    }

    private double map2scrY(double y) {
        return getHeight() / 2.0 - (y - conf.centerY) * conf.scale;
    }

    private AffineTransform getMap2ScrTransform() {
        AffineTransform transform = new AffineTransform();
        transform.translate(getWidth() / 2.0, getHeight() / 2.0);
        transform.scale(conf.scale, -conf.scale);
        transform.translate(conf.centerX, -conf.centerY);
        return transform;
    }

    MapCenter getMapCenter() {
        return new MapCenter(-conf.centerX, conf.centerY);
    }

    void restoreMapCenter(MapCenter center) {
        conf.centerX = -center.x();
        conf.centerY = center.y();
        repaint();
    }

    public void setSelectedSystem(PlanetarySystem p) {
        selectSystem(p, true);
        if (conf.scale < 4.0) {
            conf.scale = 4.0;
        }
        center(selectedSystem);
        repaint();
    }

    /**
     * Selects and centers the campaign's current system without changing the proposed route.
     */
    public void centerOnCurrentSystem() {
        PlanetarySystem currentSystem = campaign == null ? null : campaign.getCurrentSystem();
        if (currentSystem == null) {
            return;
        }

        selectSystem(currentSystem, true);
        center(currentSystem);
        notifyListeners();
    }

    static final class MapPointerGesture {
        private Point pressPoint;
        private Point lastDragPoint;
        private boolean dragging;

        void press(Point point) {
            pressPoint = new Point(point);
            lastDragPoint = new Point(point);
            dragging = false;
        }

        Point drag(Point point) {
            if (pressPoint == null) {
                return null;
            }
            dragging |= exceedsDragThreshold(point);
            if (!dragging) {
                return null;
            }
            Point delta = new Point(point.x - lastDragPoint.x, point.y - lastDragPoint.y);
            lastDragPoint = new Point(point);
            return delta;
        }

        boolean release(Point point) {
            boolean click = (pressPoint != null) && !dragging && !exceedsDragThreshold(point);
            pressPoint = null;
            lastDragPoint = null;
            dragging = false;
            return click;
        }

        private boolean exceedsDragThreshold(Point point) {
            int threshold = UIUtil.scaleForGUI(5);
            return pressPoint.distanceSq(point) > (double) threshold * threshold;
        }
    }

    private static boolean isSystemEmpty(PlanetarySystem system, LocalDate date) {
        Set<Faction> factions = system.getFactionSet(date);
        if ((null == factions) || factions.isEmpty()) {
            return true;
        }

        return factions.stream()
                     .allMatch(faction -> faction.is(FactionTag.ABANDONED));
    }

    private boolean isSystemVisible(PlanetarySystem system, boolean hideEmpty) {
          SystemRenderData renderData = hideEmpty
              ? getPreparedSystemRenderData(campaign.getLocalDate()).get(system.getId())
              : null;
          return isSystemVisible(system, hideEmpty, renderData);
        }

        private boolean isSystemVisible(PlanetarySystem system, boolean hideEmpty,
            @Nullable SystemRenderData renderData) {
        if (null == system) {
            return false;
        }
        // The current planet and the selected one are always visible
        if (system.equals(campaign.getCurrentSystem()) || system.equals(selectedSystem)) {
            return true;
        }
        // viewport check
        double x = system.getX();
        double y = system.getY();
        if ((x < minX) || (x > maxX) || (y < minY) || (y > maxY)) {
            return false;
        }
        if (hideEmpty) {
            // Filter out "empty" systems
            return (renderData != null) && !renderData.empty();
        }
        return true;
    }

    /**
     * Activate and Center
     */
    private void center(PlanetarySystem p) {
        if (p == null) {
            return;
        }
        conf.centerX = -p.getX();
        conf.centerY = p.getY();
        repaint();
    }

    private void zoom(double percent, Point pos) {
        double requestedScale = conf.scale * percent;
        if (!Double.isFinite(percent) || (percent <= 0) || !Double.isFinite(requestedScale)
              || (requestedScale <= 0)) {
            return;
        }
        double newScale = boundedMapScale(requestedScale);
        if (Double.doubleToLongBits(newScale) == Double.doubleToLongBits(conf.scale)) {
            return;
        }
        zoomInteractionActive = true;
        zoomSettlingTimer.restart();

        int width = getWidth();
        int height = getHeight();
        int anchorX = width / 2;
        int anchorY = height / 2;
        if ((pos != null) && (pos.x >= 0) && (pos.x < width) && (pos.y >= 0) && (pos.y < height)) {
            anchorX = pos.x;
            anchorY = pos.y;
        }

        double anchorMapX = scr2mapX(anchorX);
        double anchorMapY = scr2mapY(anchorY);
        conf.scale = newScale;
        conf.centerX = (anchorX - width / 2.0) / newScale - anchorMapX;
        conf.centerY = anchorMapY - (height / 2.0 - anchorY) / newScale;
        repaint();
    }

    void startSystemDive(PlanetarySystem target, Runnable completion) {
        if ((target == null) || systemDiveAnimationTimer.isRunning()) {
            return;
        }

        changeSelectedSystem(target);
        if (!isShowing() || (getWidth() <= 0) || (getHeight() <= 0)) {
            systemDiveReturnFrame = null;
            conf.scale = Math.max(conf.scale, 4.0);
            center(target);
            completion.run();
            return;
        }

        systemDiveTarget = target;
        systemDiveReturnFrame = new SystemDiveFrame(conf.centerX, conf.centerY, conf.scale);
        systemDiveReturning = false;
        systemDiveCompletion = Objects.requireNonNull(completion);
        systemDiveStartCenterX = conf.centerX;
        systemDiveStartCenterY = conf.centerY;
        systemDiveStartScale = conf.scale;
        systemDiveTargetCenterX = -target.getX();
        systemDiveTargetCenterY = target.getY();
        systemDiveTargetScale = Math.max(systemDiveStartScale,
              Math.min(SYSTEM_DIVE_MAXIMUM_TARGET_SCALE,
                    Math.max(SYSTEM_DIVE_MINIMUM_TARGET_SCALE, systemDiveStartScale * 3.0)));
        systemDiveAnimationProgress = 0.0;
        systemDiveAnimationStartTime = System.nanoTime();
        systemDiveAnimationTimer.restart();
    }

    void startSystemReturn() {
        if ((systemDiveReturnFrame == null) || systemDiveAnimationTimer.isRunning()) {
            return;
        }
        if (!isShowing() || (getWidth() <= 0) || (getHeight() <= 0)) {
            applySystemDiveFrame(systemDiveReturnFrame);
            clearSystemDiveReturn();
            repaint();
            return;
        }

        systemDiveStartCenterX = conf.centerX;
        systemDiveStartCenterY = conf.centerY;
        systemDiveStartScale = conf.scale;
        systemDiveTargetCenterX = systemDiveReturnFrame.centerX();
        systemDiveTargetCenterY = systemDiveReturnFrame.centerY();
        systemDiveTargetScale = systemDiveReturnFrame.scale();
        systemDiveReturning = true;
        systemDiveCompletion = null;
        systemDiveAnimationProgress = 0.0;
        systemDiveAnimationStartTime = System.nanoTime();
        systemDiveAnimationTimer.restart();
    }

    private void updateSystemDiveAnimation() {
        systemDiveAnimationProgress = getAnimationProgress(System.nanoTime(), systemDiveAnimationStartTime,
              SYSTEM_DIVE_ANIMATION_DURATION_NS);
          SystemDiveFrame frame = calculateSystemDiveFrame(systemDiveStartCenterX, systemDiveStartCenterY,
              systemDiveStartScale, systemDiveTargetCenterX, systemDiveTargetCenterY, systemDiveTargetScale,
              systemDiveAnimationProgress);
                applySystemDiveFrame(frame);
        repaint();

        if (systemDiveAnimationProgress >= 1.0) {
            systemDiveAnimationTimer.stop();
            Runnable completion = systemDiveCompletion;
            systemDiveCompletion = null;
            if (systemDiveReturning) {
                clearSystemDiveReturn();
            } else if (completion != null) {
                completion.run();
            }
        }
    }

    private void applySystemDiveFrame(SystemDiveFrame frame) {
        conf.centerX = frame.centerX();
        conf.centerY = frame.centerY();
        conf.scale = frame.scale();
    }

    private void clearSystemDiveReturn() {
        systemDiveReturnFrame = null;
        systemDiveTarget = null;
        systemDiveReturning = false;
    }

    static SystemDiveFrame calculateSystemDiveFrame(double startCenterX, double startCenterY, double startScale,
          double targetCenterX, double targetCenterY, double targetScale, double progress) {
        double easedProgress = easeInOutCubic(progress);
        return new SystemDiveFrame(
              interpolate(startCenterX, targetCenterX, easedProgress),
              interpolate(startCenterY, targetCenterY, easedProgress),
              Math.exp(interpolate(Math.log(startScale), Math.log(targetScale), easedProgress)));
    }

    private void suspendSystemDiveAnimation() {
        systemDiveAnimationTimer.stop();
        systemDiveCompletion = null;
        systemDiveReturning = false;
        systemDiveAnimationProgress = 1.0;
    }

    private void drawSystemDiveOverlay(Graphics2D graphics, int width, int height) {
        if (!systemDiveAnimationTimer.isRunning() || (systemDiveTarget == null)) {
            return;
        }

          double overlayAnimationProgress = systemDiveReturning
              ? 1.0 - systemDiveAnimationProgress
              : systemDiveAnimationProgress;
          double apertureProgress = Math.clamp((overlayAnimationProgress - 0.42) / 0.58, 0.0, 1.0);
        if (apertureProgress <= 0.0) {
            return;
        }

        double easedProgress = easeOutCubic(apertureProgress);
        Color spectralColor = getSpectralColor(systemDiveTarget.getStar());
        Point2D.Double center = new Point2D.Double(width / 2.0, height / 2.0);
        float radius = (float) interpolate(8.0, Math.hypot(width, height) * 0.72, easedProgress);
        Graphics2D overlayGraphics = (Graphics2D) graphics.create();
        try {
            int veilAlpha = (int) Math.round(215.0 * apertureProgress * apertureProgress);
            overlayGraphics.setColor(new Color(spectralColor.getRed() / 5, spectralColor.getGreen() / 5,
                  spectralColor.getBlue() / 5, veilAlpha));
            overlayGraphics.fillRect(0, 0, width, height);
            overlayGraphics.setPaint(new RadialGradientPaint(center, radius,
                  new float[] { 0.0f, 0.14f, 0.48f, 1.0f },
                  new Color[] {
                      withAlpha(Color.WHITE, scaleAlpha(250, apertureProgress)),
                      withAlpha(brighten(spectralColor), scaleAlpha(245, apertureProgress)),
                      withAlpha(spectralColor, scaleAlpha(185, apertureProgress)),
                      withAlpha(spectralColor, 0)
                  }));
            overlayGraphics.fillRect(0, 0, width, height);
        } finally {
            overlayGraphics.dispose();
        }
    }

    public PlanetarySystem getSelectedSystem() {
        return selectedSystem;
    }

    public JumpPath getJumpPath() {
        return jumpPath;
    }

    void setRoutePlanningHandler(RoutePlanningHandler routePlanningHandler) {
        this.routePlanningHandler = Objects.requireNonNull(routePlanningHandler);
    }

    void selectRouteTarget(PlanetarySystem target) {
        selectSystem(target, true);
        repaint();
    }

    public void changeSelectedSystem(PlanetarySystem p) {
        selectSystem(p, true);
        notifyListeners();
    }

    private void startProposedRouteAnimation() {
          int legCount = Math.max(1, jumpPath.size() - 1);
          long adaptiveLegDuration = Math.max(PROPOSED_ROUTE_MIN_LEG_DURATION_NS,
              Math.round(PROPOSED_ROUTE_BASE_LEG_DURATION_NS / Math.sqrt(legCount)));
          proposedRouteAnimationDuration = legCount * adaptiveLegDuration;
        proposedRouteAnimationStartTime = System.nanoTime();
        proposedRouteAnimationProgress = 0.0;
        proposedRouteAnimationTimer.restart();
    }

    private void updateProposedRouteAnimation() {
        if (jumpPath.size() < 2) {
            stopProposedRouteAnimation();
            return;
        }

        long elapsedTime = System.nanoTime() - proposedRouteAnimationStartTime;
        proposedRouteAnimationProgress = Math.min(1.0,
              (double) elapsedTime / proposedRouteAnimationDuration);
        if (proposedRouteAnimationProgress >= 1.0) {
            stopProposedRouteAnimation();
        }
        repaint();
    }

    private void stopProposedRouteAnimation() {
        proposedRouteAnimationTimer.stop();
        proposedRouteAnimationProgress = 1.0;
    }

    private int getRevealedProposedRouteSystemCount() {
        if (jumpPath.isEmpty()) {
            return 0;
        }
        if (!proposedRouteAnimationTimer.isRunning()) {
            return jumpPath.size();
        }
        int legCount = jumpPath.size() - 1;
        int completedLegCount = Math.min(legCount,
              (int) Math.floor(proposedRouteAnimationProgress * legCount));
        return completedLegCount + 1;
    }

    private double getCurrentProposedRouteLegProgress() {
        int legCount = jumpPath.size() - 1;
        double routePosition = proposedRouteAnimationProgress * legCount;
        return easeOutCubic(routePosition - Math.floor(routePosition));
    }

    private void selectSystem(PlanetarySystem system, boolean animate) {
        boolean selectionChanged = !isSameSystem(selectedSystem, system);
        selectedSystem = system;
        if (!selectionChanged) {
            return;
        }

        if (optReachability.isSelected()) {
            refreshReachability();
        }

        if (animate && (system != null) && isDisplayable()) {
            selectionAnimationSystemId = system.getId();
            selectionAnimationStartTime = System.nanoTime();
            selectionAnimationProgress = 0.0;
            selectionAnimationTimer.restart();
            repaint();
        } else {
            stopSelectionAnimation();
        }
    }

    private void updateSelectionAnimation() {
        if (!isSelectionAnimationTarget(selectedSystem)) {
            stopSelectionAnimation();
            return;
        }

        long elapsedTime = System.nanoTime() - selectionAnimationStartTime;
        selectionAnimationProgress = Math.min(1.0, (double) elapsedTime / SELECTION_ANIMATION_DURATION_NS);
        if (selectionAnimationProgress >= 1.0) {
            stopSelectionAnimation();
        }
        repaint();
    }

    private void stopSelectionAnimation() {
        selectionAnimationTimer.stop();
        selectionAnimationSystemId = null;
        selectionAnimationProgress = 1.0;
    }

    private boolean isSelectionAnimationTarget(PlanetarySystem system) {
        return selectionAnimationTimer.isRunning() && (selectionAnimationSystemId != null) && (system != null)
              && selectionAnimationSystemId.equals(system.getId());
    }

    private static boolean isSameSystem(PlanetarySystem first, PlanetarySystem second) {
        if (first == second) {
            return true;
        }
        return (first != null) && (second != null) && first.getId().equals(second.getId());
    }

    /**
     * Return a planet color based on what the user has selected from the radio button options
     *
     * @param system PlanetarySystem object
     *
     * @return a Color
     */
    public Color getSystemColor(PlanetarySystem system) {
        return getSystemColor(system, getSelectedMapMode());
    }

    private Color getSystemColor(PlanetarySystem system, MapMode mapMode) {
        // color shading is from the Viridis color palettes
        long pop = system.getPopulation(campaign.getLocalDate());

        // if no population, then just return black no matter what we asked for
        if (pop == 0L) {
            return Color.BLACK;
        }

        SocioIndustrialData socio = system.getSocioIndustrial(campaign.getLocalDate());

        if (null != socio && (mapMode == MapMode.TECHNOLOGY)) {
            return switch (socio.tech) {
                case REGRESSED -> new Color(51, 51, 51);
                case F -> new Color(68, 1, 84);
                case D -> new Color(59, 82, 139);
                case C -> new Color(33, 144, 140);
                case B -> new Color(93, 200, 99);
                case A, ADVANCED -> new Color(253, 231, 37);
            };
        }
        if (null != socio && (mapMode == MapMode.INDUSTRY)) {
            return switch (socio.industry) {
                case F -> new Color(0, 0, 4);
                case D -> new Color(81, 18, 124);
                case C -> new Color(182, 54, 121);
                case B -> new Color(251, 136, 97);
                case A -> new Color(252, 253, 191);
            };
        }
        if (null != socio && (mapMode == MapMode.RAW_MATERIALS)) {
            return switch (socio.rawMaterials) {
                case F -> new Color(13, 8, 135);
                case D -> new Color(126, 3, 168);
                case C -> new Color(204, 70, 120);
                case B -> new Color(248, 148, 65);
                case A -> new Color(240, 249, 33);
            };
        }
        if (null != socio && (mapMode == MapMode.OUTPUT)) {
            return switch (socio.output) {
                case F -> new Color(0, 0, 4);
                case D -> new Color(86, 15, 110);
                case C -> new Color(187, 55, 84);
                case B -> new Color(249, 140, 10);
                case A -> new Color(252, 255, 164);
            };
        }
        if (null != socio && (mapMode == MapMode.AGRICULTURE)) {
            return switch (socio.agriculture) {
                case F -> new Color(0, 32, 77);
                case D -> new Color(66, 77, 107);
                case C -> new Color(124, 123, 120);
                case B -> new Color(188, 175, 111);
                case A -> new Color(255, 234, 70);
            };
        }

        if (mapMode == MapMode.POPULATION) {
            // numbers based roughly on deciles of population distribution in 2750
            if (pop >= 3000000000L) {
                return new Color(253, 231, 37);
            } else if (pop >= 1500000000L) {
                return new Color(180, 222, 44);
            } else if (pop >= 1000000000L) {
                return new Color(109, 205, 89);
            } else if (pop >= 500000000L) {
                return new Color(53, 183, 121);
            } else if (pop >= 300000000L) {
                return new Color(31, 158, 137);
            } else if (pop >= 200000000L) {
                return new Color(38, 130, 142);
            } else if (pop >= 100000000L) {
                return new Color(49, 104, 142);
            } else if (pop >= 25000000L) {
                return new Color(62, 74, 137);
            } else if (pop >= 1000000L) {
                return new Color(72, 40, 120);
            } else if (pop > 0L) {
                return new Color(68, 1, 84);
            } else {
                return Color.GRAY;
            }
        }

        if (mapMode == MapMode.HPG) {
            HPGRating hpg = system.getHPG(campaign.getLocalDate());
            if (null == hpg) {
                return Color.BLACK;
            }
            // use two shades of gray for C and D as this is pony express
            return switch (hpg) {
                case D -> new Color(84, 84, 84);
                case C -> new Color(168, 168, 168);
                case B -> new Color(222, 73, 104);
                case A -> new Color(252, 253, 191);
                default -> Color.BLACK;
            };
        }

        if (mapMode == MapMode.RECHARGE_STATIONS) {
            // use two shades of gray for C and D as this is pony express
            return switch (system.getNumberRechargeStations(campaign.getLocalDate())) {
                case 2 -> new Color(240, 249, 33);
                case 1 -> new Color(225, 100, 98);
                case 0 -> new Color(128, 128, 128);
                default -> Color.BLACK;
            };
        }

        if (mapMode == MapMode.ACADEMIES) {
            int academyCount = Math.clamp(system.getFilteredAcademies(campaign).size(), 0, 6);

            return switch (academyCount) {
                case 6 -> new Color(253, 231, 37);
                case 5 -> new Color(180, 222, 44);
                case 4 -> new Color(109, 205, 89);
                case 3 -> new Color(53, 183, 121);
                case 2 -> new Color(31, 158, 137);
                case 1 -> new Color(38, 130, 142);
                default -> Color.BLACK;
            };
        }

        if (mapMode == MapMode.HIRING_HALLS) {
            return switch (system.getHiringHallLevel(campaign.getLocalDate())) {
                case QUESTIONABLE -> new Color(187, 55, 84);
                case MINOR -> new Color(249, 140, 10);
                case STANDARD -> new Color(253, 231, 37);
                case GREAT -> new Color(93, 200, 99);
                default -> Color.BLACK;
            };
        }

        if (mapMode == MapMode.DISEASE_OUTBREAKS) {
            Set<InjuryType> diseases = getAllActiveDiseases(system.getId(), campaign.getLocalDate(), true);
            diseases.addAll(getAllActiveBioweapons(system.getId(), campaign.getLocalDate(), true));

            int diseaseCount = min(4, diseases.size());
            return switch (diseaseCount) {
                case 1 -> new Color(253, 231, 37);
                case 2 -> new Color(249, 140, 10);
                case 3 -> new Color(187, 55, 84);
                case 4 -> new Color(126, 3, 168);
                default -> Color.BLACK;
            };
        }

        return Color.GRAY;
    }

    private MapMode getSelectedMapMode() {
        if (optTech.isSelected()) {
            return MapMode.TECHNOLOGY;
        }
        if (optIndustry.isSelected()) {
            return MapMode.INDUSTRY;
        }
        if (optRawMaterials.isSelected()) {
            return MapMode.RAW_MATERIALS;
        }
        if (optOutput.isSelected()) {
            return MapMode.OUTPUT;
        }
        if (optAgriculture.isSelected()) {
            return MapMode.AGRICULTURE;
        }
        if (optPopulation.isSelected()) {
            return MapMode.POPULATION;
        }
        if (optHPG.isSelected()) {
            return MapMode.HPG;
        }
        if (optRecharge.isSelected()) {
            return MapMode.RECHARGE_STATIONS;
        }
        if (optAcademies.isSelected()) {
            return MapMode.ACADEMIES;
        }
        if (optHiringHalls.isSelected()) {
            return MapMode.HIRING_HALLS;
        }
        if (optDiseases.isSelected()) {
            return MapMode.DISEASE_OUTBREAKS;
        }
        return MapMode.FACTION;
    }

    /*
     * TODO: re-enable later
     * private void openPlanetEventEditor(Planet p) {
     * NewPlanetaryEventDialog editor = new NewPlanetaryEventDialog(null, campaign,
     * selectedSystem);
     * editor.setVisible(true);
     * List<Planet.PlanetaryEvent> result = editor.getChangedEvents();
     * if ((null != result) && !result.isEmpty()) {
     * Planets.getInstance().updatePlanetaryEvents(p.getId(), result, true);
     * Planets.getInstance().recalcHPGNetwork();
     * repaint();
     * notifyListeners();
     * }
     * }
     */

    /**
     * Opens the GM-only planetary system editor pre-selected on the given system. Refreshes and repaints the map after
     * the dialog closes so saved planetary overrides are drawn from the campaign overlay.
     */
    private void openPlanetarySystemEditor(PlanetarySystem system) {
        if ((system == null) || !campaign.isGM()) {
            return;
        }
        PlanetarySystemEditorDialog dialog = new PlanetarySystemEditorDialog(hqView.getFrame(), campaign);
        dialog.selectSystemById(system.getId());
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                refreshSystemsFromCampaign();
                repaint();
            }
        });
        dialog.setVisible(true);
    }

    private final transient List<ActionListener> listeners = new ArrayList<>();

    public void addActionListener(ActionListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void removeActionListener(ActionListener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_FIRST, "refresh");
        listeners.forEach(l -> l.actionPerformed(ev));
    }

    public boolean isFactionsSelected() {
        return optFactions.isSelected();
    }
}
