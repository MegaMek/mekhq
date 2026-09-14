package mekhq.gui;

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

    record Presentation(List<SystemPresentation> systems, Set<String> routeSystemIds,
          boolean showEmptySystems, double labelZoomReference) {
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
