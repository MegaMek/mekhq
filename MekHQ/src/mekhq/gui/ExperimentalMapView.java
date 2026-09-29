package mekhq.gui;

import java.awt.Shape;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.HPGLink;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.CapitalType;
import mekhq.campaign.universe.enums.HPGRating;

public interface ExperimentalMapView {
    interface RenderObserver {
        void frameStarted();

        void frameCompleted();
    }

    JComponent component();

    ViewState getViewState();

    void setViewState(ViewState state);

    void refresh();

    record ViewState(double centerX, double centerY, double scale, PlanetarySystem selectedSystem) {
    }

    record SystemPresentation(PlanetarySystem system, String name, List<Integer> factionColors, boolean empty,
            CapitalType capitalType, List<Integer> capitalColors, int rechargeStations, StarAppearance star,
            int analyticalColor) {
        public SystemPresentation {
            factionColors = List.copyOf(factionColors);
            capitalColors = List.copyOf(capitalColors);
        }
    }

    record Routes(List<PlanetarySystem> planned, List<PlanetarySystem> active,
                    PlanetarySystem currentSystem, boolean inTransit, double planetProximity,
                    Set<String> requestedWaypoints) {
        public Routes {
            planned = List.copyOf(planned);
            active = List.copyOf(active);
                        requestedWaypoints = Set.copyOf(requestedWaypoints);
            planetProximity = Double.isFinite(planetProximity) ? Math.clamp(planetProximity, 0, 1) : 0;
        }
    }

    record StarAppearance(int spectralColor, int coreColor, double luminosityScale) {
    }

    record SystemStyle(double minimumSize, double maximumSize) {
        public double sizeAt(double scale) {
            return Math.clamp(1 + 5 * Math.log(scale), minimumSize, maximumSize);
        }
    }

    record Territory(Shape shape, List<Integer> factionColors, boolean pocket, boolean enclave) {
        public Territory {
            factionColors = List.copyOf(factionColors);
        }
    }

    record Emblem(String factionCode, String imagePath, int color, int priority,
          double anchorX, double anchorY, int cellCount, double width, double height) {
    }

    record AdministrativeBorder(Shape shape, List<Integer> factionColors, boolean region) {
        public AdministrativeBorder {
            factionColors = List.copyOf(factionColors);
        }
    }

    record Territories(LocalDate date, List<Territory> contours, List<Emblem> emblems,
          List<AdministrativeBorder> administrativeBorders) {
        public Territories {
            contours = List.copyOf(contours);
            emblems = List.copyOf(emblems);
            administrativeBorders = List.copyOf(administrativeBorders);
        }
    }

    enum BoundaryDetail {
        OFF, REGIONS, DISTRICTS
    }

    record CartographyLayers(boolean territories, boolean emblems, BoundaryDetail administrative) {
    }

    enum CapitalDetail {
        OFF, NATIONAL, REGIONS, DISTRICTS
    }

    record LandmarkLayers(CapitalDetail capitals, boolean rechargeStations) {
    }

    record Measurement(boolean enabled, PlanetarySystem start, PlanetarySystem end, String label) {
    }

    record RouteConstraint(PlanetarySystem origin, PlanetarySystem destination, boolean blocked, String label) {
    }

    record Navigation(Measurement measurement, List<RouteConstraint> constraints,
                      double jumpRadius, double minimumRangeZoom, int rangeColor, Reachability reachability, HpgNetwork hpgNetwork) {
                public Navigation(Measurement measurement, List<RouteConstraint> constraints,
                            double jumpRadius, double minimumRangeZoom, int rangeColor) {
                        this(measurement, constraints, jumpRadius, minimumRangeZoom, rangeColor, null, null);
                }

                    public Navigation(Measurement measurement, List<RouteConstraint> constraints,
                          double jumpRadius, double minimumRangeZoom, int rangeColor, Reachability reachability) {
                        this(measurement, constraints, jumpRadius, minimumRangeZoom, rangeColor, reachability, null);
                    }

        public Navigation {
            constraints = List.copyOf(constraints);
        }
    }

        record ReachabilityEntry(PlanetarySystem system, int minimumHops,
                    boolean caution, boolean blocked) {
        }

        record Reachability(PlanetarySystem anchor, int maximumHops, String label,
                    List<ReachabilityEntry> entries) {
                public Reachability {
                        entries = List.copyOf(entries);
                }
        }

    record Presentation(List<SystemPresentation> systems, Set<String> routeSystemIds,
          boolean showEmptySystems, double labelZoomReference, Routes routes, Territories territories,
          CartographyLayers layers, Navigation navigation, LandmarkLayers landmarks, SystemStyle systemStyle,
          InterstellarMapPanel.MapMode mapMode) {
        public Presentation {
            systems = List.copyOf(systems);
            routeSystemIds = Set.copyOf(routeSystemIds);
        }
    }

    record HpgStation(PlanetarySystem system, HPGRating rating) {
    }

    record HpgNetwork(LocalDate date, InterstellarMapPanel.HpgNetworkDetail detail,
          List<HPGLink> links, List<HpgStation> stations) {
        public HpgNetwork {
            links = List.copyOf(links);
            stations = List.copyOf(stations);
        }
    }

    interface Factory {
          ExperimentalMapView create(Campaign campaign, Supplier<Presentation> presentation,
              Consumer<PlanetarySystem> selectionHandler,
              NavigationActions navigationActions,
              Consumer<Throwable> failureHandler);
    }

    interface NavigationActions {
        boolean click(PlanetarySystem system, int modifiers, int clickCount);

        JPopupMenu createMenu(PlanetarySystem system);

        String hover(PlanetarySystem system);

        void stopMeasuring();
    }
}
