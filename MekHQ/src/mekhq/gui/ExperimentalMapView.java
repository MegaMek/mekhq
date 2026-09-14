package mekhq.gui;

import java.awt.Shape;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.PlanetarySystem;

public interface ExperimentalMapView {
    JComponent component();

    ViewState getViewState();

    void setViewState(ViewState state);

    void refresh();

    record ViewState(double centerX, double centerY, double scale, PlanetarySystem selectedSystem) {
    }

    record SystemPresentation(PlanetarySystem system, String name, List<Integer> factionColors, boolean empty) {
        public SystemPresentation {
            factionColors = List.copyOf(factionColors);
        }
    }

    record Routes(List<PlanetarySystem> planned, List<PlanetarySystem> active,
          PlanetarySystem currentSystem, boolean inTransit, double planetProximity) {
        public Routes {
            planned = List.copyOf(planned);
            active = List.copyOf(active);
            planetProximity = Double.isFinite(planetProximity) ? Math.clamp(planetProximity, 0, 1) : 0;
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

    record Presentation(List<SystemPresentation> systems, Set<String> routeSystemIds,
          boolean showEmptySystems, double labelZoomReference, Routes routes, Territories territories,
          CartographyLayers layers) {
        public Presentation {
            systems = List.copyOf(systems);
            routeSystemIds = Set.copyOf(routeSystemIds);
        }
    }

    interface Factory {
          ExperimentalMapView create(Campaign campaign, Supplier<Presentation> presentation,
              Consumer<PlanetarySystem> selectionHandler,
              Consumer<Throwable> failureHandler);
    }
}
