package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.CapitalType;
import mekhq.utilities.MHQInternationalization;
import org.jetbrains.skia.Canvas;
import org.jetbrains.skia.BlendMode;
import org.jetbrains.skia.ColorFilter;
import org.jetbrains.skia.Color4f;
import org.jetbrains.skia.FilterTileMode;
import org.jetbrains.skia.Gradient;
import org.jetbrains.skia.Shader;
import org.jetbrains.skia.Font;
import org.jetbrains.skia.FontMgr;
import org.jetbrains.skia.FontStyle;
import org.jetbrains.skia.Image;
import org.jetbrains.skia.Data;
import org.jetbrains.skia.EncodedImageFormat;
import org.jetbrains.skia.Surface;
import org.jetbrains.skia.Paint;
import org.jetbrains.skia.PaintMode;
import org.jetbrains.skia.PaintStrokeCap;
import org.jetbrains.skia.PaintStrokeJoin;
import org.jetbrains.skia.Path;
import org.jetbrains.skia.PathBuilder;
import org.jetbrains.skia.PathEffect;
import org.jetbrains.skia.PathDirection;
import org.jetbrains.skia.PathFillMode;
import org.jetbrains.skia.PixelGeometry;
import org.jetbrains.skia.Rect;
import org.jetbrains.skia.Typeface;
import org.jetbrains.skiko.SkiaLayer;
import org.jetbrains.skiko.SkiaLayerAnalytics;
import org.jetbrains.skiko.SkiaLayerProperties;

public final class SkiaMap extends JPanel implements ExperimentalMapView {
    private static final int BACKGROUND_COLOR = 0xFF060E14;
    private static final int STAR_COLOR = 0xFFDAE5E8;
    private static final int CURRENT_COLOR = 0xFFFFBE52;
    private static final int SELECTED_COLOR = 0xFFF4BB62;
    private static final int PLANNED_ROUTE_COLOR = 0xFF41D2E0;
    private static final int ACTIVE_ROUTE_COLOR = 0xFFEBA642;
    private static long completedFrames;
    private static long completedStars;
    private static int activeSurfaces;
    private static int activeTerritoryPaths;
    private static int activeEmblemImages;
    private static int activeAdministrativePaths;

    private final Campaign campaign;
    private final Supplier<Presentation> presentationSupplier;
    private final Consumer<PlanetarySystem> selectionHandler;
    private final NavigationActions navigationActions;
    private final Consumer<Throwable> failureHandler;
    private final InterstellarMapPanel.MapPointerGesture gesture = new InterstellarMapPanel.MapPointerGesture();
    private Presentation presentation;
    private RenderObserver performanceObserver;

    public void setPerformanceObserver(RenderObserver observer) {
        performanceObserver = observer;
    }
    private final Map<String, Rect> textMeasurements = new HashMap<>();
    private List<LabelPlacement> labels = List.of();
    private List<String> visibleSystemIds = List.of();
    private ViewState state;
    private SkiaLayer layer;
    private Paint starPaint;
    private final Map<Integer, Shader> starShaders = new HashMap<>();
    private final Map<String, SystemPresentation> systemPresentations = new HashMap<>();
    private Method radialGradientFactory;
    private int starShaderBuilds;
    private List<SystemPlacement> systemPlacements = List.of();

    public record SystemPlacement(String systemId, float radius, float contactAlpha, float detailAlpha,
          boolean navigationContact, int spectralColor, float auraRadius, float coreRadius) {
    }

    public List<SystemPlacement> getSystemPlacements() {
        return systemPlacements;
    }

    public int getStarShaderCount() {
        return starShaders.size();
    }

    public int getStarShaderBuilds() {
        return starShaderBuilds;
    }
    private Paint markerPaint;
    private Paint textPaint;
    private Paint plannedRoutePaint;
    private Paint activeRoutePaint;
    private PathEffect routeDashes;
    private Paint territoryFill;
    private Paint territoryEdge;
    private Territories territorySource;
    private final List<NativeTerritory> territories = new ArrayList<>();
    private final List<NativeBorder> administrativeBorders = new ArrayList<>();
    private int renderedAdministrativeBorders;
    private int territoryBuilds;
    private int renderedTerritories;
    private Paint emblemPaint;
    private final Map<String, Image> emblemImages = new HashMap<>();
    private final Set<String> missingEmblems = new HashSet<>();
    private List<EmblemPlacement> emblemPlacements = List.of();
    private int plannedLegs;
    private int activeLegs;
    private boolean transitMarkerDrawn;
    private Font labelFont;
    private Typeface labelTypeface;
    private boolean requested;
    private boolean failed;
    private long lastReport;
    private PlanetarySystem hoveredSystem;
    private Point hoverPoint;
    private String hoverText = "";
    private boolean hoverVisible;
    private final Timer hoverTimer = new Timer(450, event -> {
        hoverVisible = hoveredSystem != null && layer != null && isShowing();
        requestRender();
    });
    private JPopupMenu navigationPopup;
    private Rect hoverBounds;
    private Rect measurementBounds;
    private boolean jumpRadiusDrawn;
    private int constraintMarkersDrawn;
    private List<ReachabilityPlacement> reachabilityPlacements = List.of();
    private Rect reachabilityAnnotationBounds;
    private List<HpgLinkPlacement> hpgLinkPlacements = List.of();
    private List<HpgStationPlacement> hpgStationPlacements = List.of();

    public record HpgLinkPlacement(String primaryId, String secondaryId, String rating,
          float startX, float startY, float endX, float endY, int color, float width, boolean dashed) {
    }

    public record HpgStationPlacement(String systemId, String rating, Rect bounds, int color, float alpha) {
    }

    public List<HpgLinkPlacement> getHpgLinkPlacements() {
        return hpgLinkPlacements;
    }

    public List<HpgStationPlacement> getHpgStationPlacements() {
        return hpgStationPlacements;
    }

    public record ReachabilityPlacement(String systemId, int minimumHops, boolean caution, boolean blocked,
          String shape, int color, float alpha, Rect bounds, Rect shellBounds) {
    }

    public List<ReachabilityPlacement> getReachabilityPlacements() {
        return reachabilityPlacements;
    }

    public Rect getReachabilityAnnotationBounds() {
        return reachabilityAnnotationBounds;
    }
    private final Timer selectionTimer = new Timer(16, event -> updateSelectionAnimation());
    private long selectionStarted;
    private double selectionProgress = 1;
    private List<FocusPlacement> focusPlacements = List.of();
    private final Timer travelTimer = new Timer(16, event -> updateTravelAnimation());
    private long plannedStarted;
    private long activationStarted;
    private long hopStarted;
    private double plannedProgress = 1;
    private double activationProgress = 1;
    private double hopProgress = 1;
    private PlanetarySystem hopOrigin;
    private List<PlanetarySystem> lastPlannedRoute = List.of();
    private Image fleetImage;
    private ColorFilter fleetTint;
    private final List<Rect> navigationBounds = new ArrayList<>();
    private List<RouteBadge> routeBadges = List.of();
    private List<FleetPlacement> fleetPlacements = List.of();
    private final List<RouteLegPlacement> routeLegPlacements = new ArrayList<>();

    public record RouteLegPlacement(boolean active, String originId, String destinationId, double progress) {
    }

    public List<RouteLegPlacement> getRouteLegPlacements() {
        return List.copyOf(routeLegPlacements);
    }

    public record RouteBadge(String systemId, int number, boolean active, Rect bounds, float alpha) {
    }

    public record FleetPlacement(String systemId, Rect bounds, float ringAlpha, float shipAlpha,
          boolean image) {
    }

    public List<RouteBadge> getRouteBadges() {
        return routeBadges;
    }

    public List<FleetPlacement> getFleetPlacements() {
        return fleetPlacements;
    }

    public boolean hasFleetImage() {
        return fleetImage != null;
    }

    public record TravelAnimation(double planned, double activation, double hop, boolean running) {
    }

    public TravelAnimation getTravelAnimation() {
        return new TravelAnimation(plannedProgress, activationProgress, hopProgress, travelTimer.isRunning());
    }

    public record FocusPlacement(String systemId, boolean selected, float ringAlpha, float bracketAlpha,
          double progress, Rect bounds) {
    }

    public List<FocusPlacement> getFocusPlacements() {
        return focusPlacements;
    }

    public boolean isSelectionAnimating() {
        return selectionTimer.isRunning();
    }
    private List<LandmarkPlacement> landmarkPlacements = List.of();
    private final Set<String> capitalLabelIds = new HashSet<>();

    public record LandmarkPlacement(String systemId, CapitalType capitalType, int rechargeStations,
          Rect bounds, float alpha) {
    }

    public List<LandmarkPlacement> getLandmarkPlacements() {
        return landmarkPlacements;
    }

    private SkiaMap(Campaign campaign, Supplier<Presentation> presentationSupplier,
            Consumer<PlanetarySystem> selectionHandler, NavigationActions navigationActions,
            Consumer<Throwable> failureHandler) {
        super(new BorderLayout());
        this.campaign = campaign;
        this.presentationSupplier = presentationSupplier;
        this.selectionHandler = selectionHandler;
        this.navigationActions = navigationActions;
        this.failureHandler = failureHandler;
        hoverTimer.setRepeats(false);
        selectionTimer.setCoalesce(true);
        travelTimer.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
                clearHover();
                dismissPopup();
                stopSelectionAnimation();
                stopTravelAnimation();
            }
        });
        state = new ViewState(0, 0, 4, campaign.getCurrentSystem());
        setBackground(new Color(BACKGROUND_COLOR, true));
        setMinimumSize(new Dimension(0, 0));
        getAccessibleContext().setAccessibleName(MHQInternationalization.getTextAt(
              "mekhq.resources.MekHQMenuBar", "miExperimentalSkiaMap.text"));
        refresh();
    }

    public static long getCompletedFrames() {
        return completedFrames;
    }

    public static long getCompletedStars() {
        return completedStars;
    }

    public static int getActiveSurfaces() {
        return activeSurfaces;
    }

    public static int getActiveTerritoryPaths() {
        return activeTerritoryPaths;
    }

    public int getTerritoryBuilds() {
        return territoryBuilds;
    }

    public int getRenderedTerritories() {
        return renderedTerritories;
    }

    private record NativeTerritory(Territory data, Path path, Rectangle2D bounds) {
    }

    private record NativeBorder(AdministrativeBorder data, Path path, Rectangle2D bounds) {
    }

    public static int getActiveAdministrativePaths() {
        return activeAdministrativePaths;
    }

    public int getRenderedAdministrativeBorders() {
        return renderedAdministrativeBorders;
    }

    public record EmblemPlacement(Emblem emblem, Rect bounds, double projectedArea) {
    }

    public List<EmblemPlacement> getEmblemPlacements() {
        return emblemPlacements;
    }

    public static int getActiveEmblemImages() {
        return activeEmblemImages;
    }

    public byte[] captureNativePng() {
        requireEdt();
        if (layer == null || failed || getWidth() <= 0 || getHeight() <= 0) {
            throw new IllegalStateException("Native map surface is unavailable");
        }
        try (Surface surface = Surface.Companion.makeRasterN32Premul(getWidth(), getHeight())) {
            Canvas canvas = surface.getCanvas();
            canvas.scale(1 / layer.getContentScale(), 1 / layer.getContentScale());
            render(canvas, getWidth(), getHeight(), 0);
            if (failed) {
                throw new IllegalStateException("Native map snapshot failed");
            }
            try (Image image = surface.makeImageSnapshot();
                  Data data = Objects.requireNonNull(image.encodeToData(EncodedImageFormat.PNG, 100, 0))) {
                return data.getBytes();
            }
        }
    }

    public record LabelPlacement(String systemId, String text, Rect bounds, float baselineX, float baselineY,
          float alpha) {
    }

    public byte[] captureNativeStarPng(SystemPresentation data, int dimension) {
        requireEdt();
        if (layer == null || failed) {
            throw new IllegalStateException("Native map surface is unavailable");
        }
        try (Surface surface = Surface.Companion.makeRasterN32Premul(dimension, dimension)) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(BACKGROUND_COLOR);
            drawStar(canvas, data, dimension / 2f, dimension / 2f,
                  InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference()));
            try (Image image = surface.makeImageSnapshot();
                  Data encoded = Objects.requireNonNull(image.encodeToData(EncodedImageFormat.PNG, 100, 0))) {
                return encoded.getBytes();
            }
        }
    }

    public byte[] captureNativeFocusPng(PlanetarySystem system, boolean selected, double progress, int dimension) {
        requireEdt();
        if (layer == null || failed) {
            throw new IllegalStateException("Native map surface is unavailable");
        }
        try (Surface surface = Surface.Companion.makeRasterN32Premul(dimension, dimension); Paint paint = new Paint()) {
            Canvas canvas = surface.getCanvas();
            canvas.clear(BACKGROUND_COLOR);
            canvas.translate(dimension / 2f - screenX(system), dimension / 2f - screenY(system));
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            drawFocusMarker(canvas, system, selected,
                  InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference()),
                  paint, progress);
            try (Image image = surface.makeImageSnapshot();
                  Data encoded = Objects.requireNonNull(image.encodeToData(EncodedImageFormat.PNG, 100, 0))) {
                return encoded.getBytes();
            }
        }
    }

    public List<LabelPlacement> getLabelPlacements() {
        return labels;
    }

    public List<String> getVisibleSystemIds() {
        return visibleSystemIds;
    }

    public Presentation getPresentation() {
        return presentation;
    }

    public JPopupMenu getNavigationPopup() {
        return navigationPopup;
    }

    public Rect getHoverBounds() {
        return hoverBounds;
    }

    public Rect getMeasurementBounds() {
        return measurementBounds;
    }

    public boolean isJumpRadiusDrawn() {
        return jumpRadiusDrawn;
    }

    public int getConstraintMarkersDrawn() {
        return constraintMarkersDrawn;
    }

    public int getPlannedLegs() {
        return plannedLegs;
    }

    public int getActiveLegs() {
        return activeLegs;
    }

    public boolean isTransitMarkerDrawn() {
        return transitMarkerDrawn;
    }

    @Override
    public JComponent component() {
        return this;
    }

    @Override
    public ViewState getViewState() {
        return state;
    }

    @Override
    public void setViewState(ViewState state) {
        requireEdt();
        Objects.requireNonNull(state);
        if (!Double.isFinite(state.centerX()) || !Double.isFinite(state.centerY())
              || !Double.isFinite(state.scale()) || (state.scale() <= 0)) {
            throw new IllegalArgumentException("Invalid map camera");
        }
        clearHover();
        boolean selectionChanged = !Objects.equals(this.state.selectedSystem(), state.selectedSystem());
        this.state = new ViewState(state.centerX(), state.centerY(),
              InterstellarMapPanel.boundedMapScale(state.scale()), state.selectedSystem());
        if (selectionChanged) {
            stopSelectionAnimation();
            if (state.selectedSystem() != null && layer != null && isShowing()) {
                selectionStarted = System.nanoTime();
                selectionProgress = 0;
                selectionTimer.restart();
            }
        }
        requestRender();
    }

    private void updateSelectionAnimation() {
        if (layer == null || !isShowing() || state.selectedSystem() == null) {
            stopSelectionAnimation();
            return;
        }
        selectionProgress = Math.min(1, (System.nanoTime() - selectionStarted)
              / (double) InterstellarMapPanel.SELECTION_ANIMATION_DURATION_NS);
        if (selectionProgress >= 1) {
            stopSelectionAnimation();
        }
        requestRender();
    }

    private void stopSelectionAnimation() {
        selectionTimer.stop();
        selectionProgress = 1;
    }

    @Override
    public void refresh() {
        requireEdt();
        clearHover();
        updatePresentation();
        requestRender();
    }

    private void updatePresentation() {
        Presentation next = presentationSupplier.get();
        if (presentation != null) {
            updateTravelState(presentation.routes(), next.routes());
        }
        if ((presentation == null) || (presentation.systems() != next.systems())) {
            textMeasurements.clear();
            systemPresentations.clear();
            for (SystemPresentation data : next.systems()) {
                systemPresentations.put(data.system().getId(), data);
            }
        }
        presentation = next;
    }

    private void updateTravelState(Routes previous, Routes next) {
        boolean visible = layer != null && isShowing();
        long now = System.nanoTime();
        if (!previous.planned().equals(next.planned())) {
            plannedProgress = visible && next.planned().size() > 1 ? 0 : 1;
            plannedStarted = now;
        }
        if (!previous.active().equals(next.active())) {
            activationProgress = visible && previous.active().isEmpty() && next.active().size() > 1
                  && next.active().equals(lastPlannedRoute) ? 0 : 1;
            activationStarted = now;
        }
        if (!Objects.equals(previous.currentSystem(), next.currentSystem())) {
            hopOrigin = previous.currentSystem();
            hopProgress = visible && hopOrigin != null && next.currentSystem() != null
                  && (!previous.active().isEmpty() || !next.active().isEmpty()) ? 0 : 1;
            hopStarted = now;
        }
        if (!next.planned().isEmpty()) {
            lastPlannedRoute = next.planned();
        }
        if (!visible) {
            stopTravelAnimation();
        } else if (plannedProgress < 1 || activationProgress < 1 || hopProgress < 1) {
            travelTimer.start();
        } else {
            travelTimer.stop();
        }
    }

    private void updateTravelAnimation() {
        if (layer == null || !isShowing()) {
            stopTravelAnimation();
            return;
        }
        long now = System.nanoTime();
        if (plannedProgress < 1) {
            plannedProgress = Math.min(1, (now - plannedStarted)
                  / (double) InterstellarMapPanel.proposedRouteDuration(presentation.routes().planned().size()));
        }
        if (activationProgress < 1) {
            activationProgress = Math.min(1, (now - activationStarted)
                  / (double) InterstellarMapPanel.ROUTE_ACTIVATION_DURATION_NS);
        }
        if (hopProgress < 1) {
            hopProgress = Math.min(1, (now - hopStarted)
                  / (double) InterstellarMapPanel.SYSTEM_HOP_DURATION_NS);
        }
        if (plannedProgress >= 1 && activationProgress >= 1 && hopProgress >= 1) {
            stopTravelAnimation();
        }
        requestRender();
    }

    private void stopTravelAnimation() {
        travelTimer.stop();
        plannedProgress = 1;
        activationProgress = 1;
        hopProgress = 1;
        hopOrigin = null;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if ((layer != null) || failed) {
            return;
        }
        try {
            starPaint = new Paint();
            starPaint.setAntiAlias(true);
            radialGradientFactory = Shader.Companion.getClass().getMethod("makeRadialGradient-kyGds4A",
                float.class, float.class, float.class, Gradient.class, float[].class);
            markerPaint = new Paint();
            markerPaint.setAntiAlias(true);
            markerPaint.setMode(PaintMode.STROKE);
            markerPaint.setStrokeWidth(UIUtil.scaleForGUI(1));
            routeDashes = PathEffect.Companion.makeDash(new float[] {
                UIUtil.scaleForGUI(8), UIUtil.scaleForGUI(6) }, 0);
            plannedRoutePaint = createRoutePaint(PLANNED_ROUTE_COLOR, 2);
            plannedRoutePaint.setPathEffect(routeDashes);
            activeRoutePaint = createRoutePaint(ACTIVE_ROUTE_COLOR, 3);
            loadFleetImage();
            territoryFill = new Paint();
            territoryFill.setAntiAlias(true);
            territoryEdge = createRoutePaint(0, 1);
            territoryEdge.setStrokeJoin(PaintStrokeJoin.ROUND);
            emblemPaint = new Paint();
            emblemPaint.setAntiAlias(true);
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            java.awt.Font swingFont = UIManager.getFont("Label.font");
            labelTypeface = Objects.requireNonNull(FontMgr.Companion.getDefault().matchFamilyStyle(
                swingFont.getFamily(), FontStyle.Companion.getNORMAL()), "Map label typeface unavailable");
            labelFont = new Font(labelTypeface, swingFont.getSize2D());
            textMeasurements.clear();
            layer = new SkiaLayer(null, new SkiaLayerProperties(), SkiaLayerAnalytics.Companion.getEmpty(),
                  PixelGeometry.UNKNOWN);
            activeSurfaces++;
            layer.setFocusable(true);
            layer.setRenderDelegate(this::render);
            installInput();
            add(layer, BorderLayout.CENTER);
            revalidate();
            SwingUtilities.invokeLater(this::requestRender);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            fail(exception);
        }
    }

    @Override
    public void removeNotify() {
        requireEdt();
        stopSelectionAnimation();
        focusPlacements = List.of();
        stopTravelAnimation();
        clearHover();
        dismissPopup();
        try {
            SkiaLayer retired = layer;
            layer = null;
            requested = false;
            if (retired != null) {
                try {
                    retired.dispose();
                } finally {
                    activeSurfaces--;
                    remove(retired);
                }
            }
        } finally {
            if (fleetImage != null) {
                fleetImage.close();
                fleetImage = null;
            }
            if (fleetTint != null) {
                fleetTint.close();
                fleetTint = null;
            }
            fleetPlacements = List.of();
            routeBadges = List.of();
            navigationBounds.clear();
            routeLegPlacements.clear();
            clearTerritories();
            clearEmblems();
            if (emblemPaint != null) {
                emblemPaint.close();
                emblemPaint = null;
            }
            if (territoryFill != null) {
                territoryFill.close();
                territoryFill = null;
            }
            if (territoryEdge != null) {
                territoryEdge.close();
                territoryEdge = null;
            }
            if (starPaint != null) {
                starPaint.close();
                starPaint = null;
            }
            for (Shader shader : starShaders.values()) {
                shader.close();
            }
            starShaders.clear();
            systemPlacements = List.of();
            radialGradientFactory = null;
            if (markerPaint != null) {
                markerPaint.close();
                markerPaint = null;
            }
            if (plannedRoutePaint != null) {
                plannedRoutePaint.close();
                plannedRoutePaint = null;
            }
            if (activeRoutePaint != null) {
                activeRoutePaint.close();
                activeRoutePaint = null;
            }
            if (routeDashes != null) {
                routeDashes.close();
                routeDashes = null;
            }
            if (textPaint != null) {
                textPaint.close();
                textPaint = null;
            }
            if (labelFont != null) {
                labelFont.close();
                labelFont = null;
            }
            if (labelTypeface != null) {
                labelTypeface.close();
                labelTypeface = null;
            }
            textMeasurements.clear();
            super.removeNotify();
        }
    }

    private void requestRender() {
        if ((layer != null) && !failed && !requested && layer.isShowing()) {
            requested = true;
            try {
                layer.needRender(true);
            } catch (RuntimeException | LinkageError exception) {
                fail(exception);
            }
        }
    }

    private void render(Canvas canvas, int width, int height, long nanoTime) {
        var observer = performanceObserver;
        if (observer != null) {
            observer.frameStarted();
        }
        try {
            renderMap(canvas, width, height, nanoTime);
        } finally {
            if (observer != null) {
                observer.frameCompleted();
            }
        }
    }

    private void renderMap(Canvas canvas, int width, int height, long nanoTime) {
        requireEdt();
        requested = false;
        if ((layer == null) || failed || (width <= 0) || (height <= 0)) {
            return;
        }
        canvas.save();
        try {
            canvas.clear(BACKGROUND_COLOR);
            canvas.scale(layer.getContentScale(), layer.getContentScale());
            updatePresentation();
            drawTerritories(canvas);
            drawAdministrativeBorders(canvas);
            drawEmblems(canvas);
            drawHpgLinks(canvas);
            drawJumpRadius(canvas);
            routeLegPlacements.clear();
            activeLegs = drawRouteLines(canvas, presentation.routes().active(), true);
            plannedLegs = drawRouteLines(canvas, presentation.routes().planned(), false);
            int visibleStars = 0;
            var zoom = InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference());
            List<SystemPlacement> drawnSystems = new ArrayList<>();
            List<SystemPresentation> visible = new ArrayList<>();
            List<Rect> markerBounds = new ArrayList<>();
            List<String> renderedIds = new ArrayList<>();
            for (SystemPresentation data : presentation.systems()) {
                PlanetarySystem system = data.system();
                if (!isVisible(data)) {
                    continue;
                }
                float horizontal = screenX(system);
                float vertical = screenY(system);
                float radius = systemArtRadius(system);
                if ((horizontal < -radius) || (horizontal > getWidth() + radius)
                      || (vertical < -radius) || (vertical > getHeight() + radius)) {
                    continue;
                }
                    drawnSystems.add(drawStar(canvas, data, horizontal, vertical, zoom));
                    visible.add(data);
                    renderedIds.add(system.getId());
                    float markerRadius = markerRadius(system);
                    markerBounds.add(Rect.makeLTRB(horizontal - markerRadius, vertical - markerRadius,
                        horizontal + markerRadius, vertical + markerRadius));
                visibleStars++;
            }
                visibleSystemIds = List.copyOf(renderedIds);
            systemPlacements = List.copyOf(drawnSystems);
            navigationBounds.clear();
            drawRouteMarkers(canvas, presentation.routes().active(), ACTIVE_ROUTE_COLOR);
            drawRouteMarkers(canvas, presentation.routes().planned(), PLANNED_ROUTE_COLOR);
            drawTransitMarker(canvas);
            drawFleet(canvas);
            drawReachability(canvas, markerBounds);
            drawRouteBadges(canvas, markerBounds);
            markerBounds.addAll(navigationBounds);
            drawConstraints(canvas);
            drawFocusMarkers(canvas);
            drawHpgStations(canvas, markerBounds);
            drawLandmarks(canvas, visible, markerBounds);
            drawLabels(canvas, visible, markerBounds);
            drawMeasurement(canvas);
            hoverBounds = null;
            if (hoverVisible && hoverPoint != null && !hoverText.isBlank()) {
                hoverBounds = drawInfoBox(canvas, hoverText,
                        hoverPoint.x + UIUtil.scaleForGUI(14), hoverPoint.y + UIUtil.scaleForGUI(18), measurementBounds);
            }
            completedFrames++;
            completedStars += visibleStars;
            if (System.nanoTime() - lastReport > 5_000_000_000L) {
                lastReport = System.nanoTime();
                    System.out.printf("SKIA_MAP backend=%s scale=%.2f frames=%d visibleStars=%d labels=%d surfaces=%d%n",
                        layer.getRenderApi(), layer.getContentScale(), completedFrames, visibleStars, labels.size(), activeSurfaces);
            }
        } catch (RuntimeException | LinkageError exception) {
            fail(exception);
        } finally {
            canvas.restore();
        }
    }

    private void clearTerritories() {
        for (NativeTerritory territory : territories) {
            territory.path().close();
            activeTerritoryPaths--;
        }
        territories.clear();
        for (NativeBorder border : administrativeBorders) {
            border.path().close();
            activeAdministrativePaths--;
        }
        administrativeBorders.clear();
        territorySource = null;
    }

    private void prepareTerritories() {
        if (territorySource == presentation.territories()) {
            return;
        }
        clearTerritories();
        clearEmblems();
        for (Territory territory : presentation.territories().contours()) {
            territories.add(new NativeTerritory(territory, toNativePath(territory.shape()), territory.shape().getBounds2D()));
            activeTerritoryPaths++;
        }
        for (AdministrativeBorder border : presentation.territories().administrativeBorders()) {
            administrativeBorders.add(new NativeBorder(border, toNativePath(border.shape()), border.shape().getBounds2D()));
            activeAdministrativePaths++;
        }
        territorySource = presentation.territories();
        territoryBuilds++;
    }

    private static Path toNativePath(java.awt.Shape shape) {
        PathIterator iterator = shape.getPathIterator(null);
        PathFillMode fillMode = iterator.getWindingRule() == PathIterator.WIND_EVEN_ODD
                  ? PathFillMode.EVEN_ODD : PathFillMode.WINDING;
        try (PathBuilder builder = new PathBuilder(fillMode)) {
                float[] coordinates = new float[6];
                while (!iterator.isDone()) {
                    switch (iterator.currentSegment(coordinates)) {
                        case PathIterator.SEG_MOVETO -> builder.moveTo(coordinates[0], coordinates[1]);
                        case PathIterator.SEG_LINETO -> builder.lineTo(coordinates[0], coordinates[1]);
                        case PathIterator.SEG_QUADTO -> builder.quadTo(coordinates[0], coordinates[1],
                              coordinates[2], coordinates[3]);
                        case PathIterator.SEG_CUBICTO -> builder.cubicTo(coordinates[0], coordinates[1],
                              coordinates[2], coordinates[3], coordinates[4], coordinates[5]);
                        case PathIterator.SEG_CLOSE -> builder.closePath();
                        default -> throw new IllegalStateException("Unknown territory path segment");
                    }
                    iterator.next();
                }
            return builder.detach();
        }
    }

    private void drawAdministrativeBorders(Canvas canvas) {
        renderedAdministrativeBorders = 0;
        BoundaryDetail detail = presentation.layers().administrative();
        if (detail == BoundaryDetail.OFF) {
            return;
        }
        float scale = (float) state.scale();
        float unit = (float) UIUtil.scaleForGUI(1) / scale;
        Rectangle2D viewport = new Rectangle2D.Double(state.centerX() - getWidth() / (2.0 * scale),
              state.centerY() - getHeight() / (2.0 * scale), getWidth() / scale, getHeight() / scale);
        try (PathEffect dash = PathEffect.Companion.makeDash(new float[] { 4 * unit, 5 * unit }, 0)) {
            canvas.save();
            try {
                canvas.translate(getWidth() / 2.0f, getHeight() / 2.0f);
                canvas.scale(scale, -scale);
                canvas.translate((float) -state.centerX(), (float) -state.centerY());
                for (NativeBorder border : administrativeBorders) {
                    AdministrativeBorder data = border.data();
                    if ((!data.region() && detail == BoundaryDetail.REGIONS)
                          || !border.bounds().intersects(viewport) || data.factionColors().isEmpty()) {
                        continue;
                    }
                    territoryEdge.setPathEffect(data.region() ? null : dash);
                    territoryEdge.setColor(0xAF02060A);
                    territoryEdge.setStrokeWidth((data.region() ? 3.8f : 2.6f) * unit);
                    canvas.drawPath(border.path(), territoryEdge);
                    territoryEdge.setColor(((data.region() ? 215 : 165) << 24)
                          | (data.factionColors().getFirst() & 0xFFFFFF));
                    territoryEdge.setStrokeWidth((data.region() ? 1.8f : 1) * unit);
                    canvas.drawPath(border.path(), territoryEdge);
                    if (data.factionColors().size() > 1) {
                        territoryEdge.setPathEffect(null);
                        territoryEdge.setColor(0xF0000000 | (data.factionColors().get(1) & 0xFFFFFF));
                        territoryEdge.setStrokeWidth(1.2f * unit);
                        canvas.drawPath(border.path(), territoryEdge);
                    }
                    renderedAdministrativeBorders++;
                }
            } finally {
                territoryEdge.setPathEffect(null);
                canvas.restore();
            }
        }
    }

    private void clearEmblems() {
        for (Image image : emblemImages.values()) {
            image.close();
            activeEmblemImages--;
        }
        emblemImages.clear();
        missingEmblems.clear();
        emblemPlacements = List.of();
    }

    private Image getEmblemImage(String path) {
        if (emblemImages.containsKey(path)) {
            return emblemImages.get(path);
        }
        if (missingEmblems.contains(path)) {
            return null;
        }
        try {
            Image image = Image.Companion.makeFromEncoded(Files.readAllBytes(java.nio.file.Path.of(path)));
            emblemImages.put(path, image);
            activeEmblemImages++;
            return image;
        } catch (IOException | IllegalArgumentException exception) {
            missingEmblems.add(path);
            return null;
        }
    }

    private void drawEmblems(Canvas canvas) {
        float alpha = (float) (0.34 * InterstellarMapPanel.SemanticZoomProfile.create(state.scale(),
              presentation.labelZoomReference()).factionLogoAlpha());
        if (alpha <= 0 || !presentation.layers().emblems()) {
            emblemPlacements = List.of();
            return;
        }
        double cellArea = 30 * 30 * Math.sqrt(3) / 2 * state.scale() * state.scale();
        int maximum = UIUtil.scaleForGUI(100);
        int padding = UIUtil.scaleForGUI(8);
        List<EmblemPlacement> candidates = new ArrayList<>();
        for (Emblem emblem : presentation.territories().emblems()) {
            int minimum = UIUtil.scaleForGUI(emblem.priority() == 0 ? 36 : 24);
            double area = emblem.cellCount() * cellArea;
            double width = emblem.width() * state.scale();
            double height = emblem.height() * state.scale();
            double areaFactor = emblem.priority() == 0 ? 4 : emblem.priority() == 1 ? 2.25 : 3;
            double extentFactor = emblem.priority() == 0 ? 1.25 : emblem.priority() == 1 ? 0.9 : 1;
            if (area < minimum * minimum * areaFactor || width < minimum * extentFactor
                  || height < minimum * extentFactor) {
                continue;
            }
            double containment = emblem.priority() == 0 ? 0.68 : 0.85;
            double desired = Math.min(Math.sqrt(area) * (emblem.priority() == 0 ? 0.5 : 0.65),
                  Math.min(width, height) * containment);
            int size = InterstellarMapPanel.resolveLogoSize(desired, minimum, maximum);
            float horizontal = (float) (getWidth() / 2.0 + (emblem.anchorX() - state.centerX()) * state.scale());
            float vertical = (float) (getHeight() / 2.0 - (emblem.anchorY() - state.centerY()) * state.scale());
            if (size < minimum || horizontal + size / 2f < 0 || horizontal - size / 2f > getWidth()
                  || vertical + size / 2f < 0 || vertical - size / 2f > getHeight()) {
                continue;
            }
            Image image = getEmblemImage(emblem.imagePath());
            if (image == null) {
                continue;
            }
            float targetWidth = size * image.getWidth() / (float) Math.max(image.getWidth(), image.getHeight());
            float targetHeight = size * image.getHeight() / (float) Math.max(image.getWidth(), image.getHeight());
            candidates.add(new EmblemPlacement(emblem, Rect.makeXYWH(horizontal - targetWidth / 2,
                  vertical - targetHeight / 2, targetWidth, targetHeight), area));
        }
        candidates.sort(Comparator.comparingInt((EmblemPlacement candidate) -> candidate.emblem().priority())
              .thenComparing(Comparator.comparingDouble(EmblemPlacement::projectedArea).reversed())
              .thenComparing(candidate -> candidate.emblem().factionCode())
              .thenComparingDouble(candidate -> candidate.emblem().anchorX())
              .thenComparingDouble(candidate -> candidate.emblem().anchorY()));
        List<EmblemPlacement> accepted = new ArrayList<>();
        List<Rectangle2D> occupied = new ArrayList<>();
        for (EmblemPlacement candidate : candidates) {
            Rect bounds = candidate.bounds();
            Rectangle2D padded = new Rectangle2D.Double(bounds.getLeft() - padding, bounds.getTop() - padding,
                  bounds.getWidth() + padding * 2, bounds.getHeight() + padding * 2);
            boolean collision = false;
            for (Rectangle2D previous : occupied) {
                if (previous.intersects(padded)) {
                    collision = true;
                    break;
                }
            }
            if (collision) {
                continue;
            }
            occupied.add(padded);
            accepted.add(candidate);
            int color = candidate.emblem().color();
            int tint = 0xFF000000 | (((color >> 16 & 255) + 510) / 3 << 16)
                  | (((color >> 8 & 255) + 510) / 3 << 8) | ((color & 255) + 510) / 3;
            Image image = emblemImages.get(candidate.emblem().imagePath());
            float shadow = UIUtil.scaleForGUI(2);
            try (ColorFilter shadowFilter = ColorFilter.Companion.makeBlend(0xFF000000, BlendMode.SRC_IN);
                  ColorFilter tintFilter = ColorFilter.Companion.makeBlend(tint, BlendMode.SRC_IN)) {
                emblemPaint.setAlphaf(alpha * 0.72f);
                emblemPaint.setColorFilter(shadowFilter);
                canvas.drawImageRect(image, Rect.makeXYWH(bounds.getLeft() + shadow, bounds.getTop() + shadow,
                      bounds.getWidth(), bounds.getHeight()), emblemPaint);
                emblemPaint.setAlphaf(alpha);
                emblemPaint.setColorFilter(tintFilter);
                canvas.drawImageRect(image, bounds, emblemPaint);
            } finally {
                emblemPaint.setColorFilter(null);
            }
        }
        emblemPlacements = List.copyOf(accepted);
    }

    private void drawTerritories(Canvas canvas) {
        prepareTerritories();
        renderedTerritories = 0;
        if (!presentation.layers().territories()) {
            return;
        }
        float scale = (float) state.scale();
        double halfWidth = getWidth() / (2.0 * scale);
        double halfHeight = getHeight() / (2.0 * scale);
        Rectangle2D viewport = new Rectangle2D.Double(state.centerX() - halfWidth,
              state.centerY() - halfHeight, halfWidth * 2, halfHeight * 2);
        var profile = InterstellarMapPanel.TerritoryVisualProfile.create(scale);
        float unit = (float) UIUtil.scaleForGUI(1) / scale;
        try (PathEffect disputedDashes = PathEffect.Companion.makeDash(new float[] { 7 * unit, 4 * unit }, 0);
              PathEffect pocketDashes = PathEffect.Companion.makeDash(new float[] { unit, 5 * unit }, 0)) {
            canvas.save();
            try {
                canvas.translate(getWidth() / 2.0f, getHeight() / 2.0f);
                canvas.scale(scale, -scale);
                canvas.translate((float) -state.centerX(), (float) -state.centerY());
                for (NativeTerritory territory : territories) {
                    if (!territory.bounds().intersects(viewport)) {
                        continue;
                    }
                    Territory data = territory.data();
                    List<Integer> colors = data.factionColors();
                    int color = colors.isEmpty() ? 0xFFC6D3D6 : colors.getFirst();
                    if (colors.size() > 1) {
                        drawDisputedBands(canvas, territory, viewport, profile, scale);
                    } else {
                        territoryFill.setColor(territoryColor(data.pocket() ? 0xFF010509 : color,
                              data.pocket() ? 70 : 41));
                        canvas.drawPath(territory.path(), territoryFill);
                    }
                    territoryEdge.setPathEffect(null);
                    if (!data.pocket()) {
                        territoryEdge.setColor(territoryColor(0xFF02060A, 175));
                        territoryEdge.setStrokeWidth(2.8f * unit);
                        canvas.drawPath(territory.path(), territoryEdge);
                    }
                    if (data.pocket() || colors.size() > 1) {
                        territoryEdge.setPathEffect(data.pocket() ? pocketDashes : disputedDashes);
                        territoryEdge.setColor(territoryColor(0xFFC6D3D6,
                              data.pocket() ? (int) (145 * profile.secondaryDetailAlpha()) : 145));
                        territoryEdge.setStrokeWidth((data.pocket() ? 1.6f : 1) * unit);
                    } else {
                        territoryEdge.setColor(territoryColor(color, 170));
                        territoryEdge.setStrokeWidth(unit);
                    }
                    canvas.drawPath(territory.path(), territoryEdge);
                    if (data.enclave() && profile.secondaryDetailAlpha() > 0) {
                        territoryEdge.setStrokeWidth(4.4f * unit);
                        territoryEdge.setColor(territoryColor(color, (int) (75 * profile.secondaryDetailAlpha())));
                        canvas.drawPath(territory.path(), territoryEdge);
                    }
                    renderedTerritories++;
                }
            } finally {
                territoryEdge.setPathEffect(null);
                canvas.restore();
            }
        }
    }

    private void drawDisputedBands(Canvas canvas, NativeTerritory territory, Rectangle2D viewport,
          InterstellarMapPanel.TerritoryVisualProfile profile, float scale) {
        List<Integer> colors = territory.data().factionColors();
        int red = 0;
        int green = 0;
        int blue = 0;
        for (int color : colors) {
            red += color >> 16 & 255;
            green += color >> 8 & 255;
            blue += color & 255;
        }
        red /= colors.size();
        green /= colors.size();
        blue /= colors.size();
        double width = UIUtil.scaleForGUI(1) * profile.disputedBandWidth() / scale;
        Rectangle2D bounds = territory.bounds().createIntersection(viewport);
        double bottom = bounds.getMinY() - width;
        double top = bounds.getMaxY() + width;
        double height = top - bottom;
        long firstBand = (long) Math.floor((bounds.getMinX() - top) / width);
        long lastBand = (long) Math.ceil((bounds.getMaxX() - bottom) / width);
        canvas.save();
        try {
            canvas.clipPath(territory.path());
            territoryEdge.setPathEffect(null);
            territoryEdge.setStrokeWidth((float) (width / Math.sqrt(2) + 0.5 / scale));
            for (long band = firstBand; band <= lastBand; band++) {
                int color = colors.get(Math.floorMod(band, colors.size()));
                double detail = profile.secondaryDetailAlpha();
                int bandRed = (int) (red + ((color >> 16 & 255) - red) * detail);
                int bandGreen = (int) (green + ((color >> 8 & 255) - green) * detail);
                int bandBlue = (int) (blue + ((color & 255) - blue) * detail);
                territoryEdge.setColor(territoryColor(bandRed << 16 | bandGreen << 8 | bandBlue, 46));
                float start = (float) (band * width + bottom);
                canvas.drawLine(start, (float) bottom, (float) (start + height), (float) top, territoryEdge);
            }
        } finally {
            canvas.restore();
        }
    }

    private static int territoryColor(int color, int alpha) {
        return ((int) (alpha * 0.72) << 24) | (color & 0xFFFFFF);
    }

    private static Paint createRoutePaint(int color, int width) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setMode(PaintMode.STROKE);
        paint.setStrokeCap(PaintStrokeCap.ROUND);
        paint.setStrokeWidth(UIUtil.scaleForGUI(width));
        paint.setColor(color);
        return paint;
    }

    private int drawRouteLines(Canvas canvas, List<PlanetarySystem> route, boolean active) {
        double position = (active ? InterstellarMapPanel.easeInOutCubic(activationProgress) : plannedProgress)
              * Math.max(0, route.size() - 1);
        int drawn = 0;
        for (int index = 1; index < route.size(); index++) {
            PlanetarySystem origin = route.get(index - 1);
            PlanetarySystem destination = route.get(index);
            double progress = Math.clamp(position - index + 1, 0, 1);
            if (!active) {
                progress = InterstellarMapPanel.easeOutCubic(progress);
            }
            float boundaryX = screenX(origin) + (float) progress * (screenX(destination) - screenX(origin));
            float boundaryY = screenY(origin) + (float) progress * (screenY(destination) - screenY(origin));
            if (active && progress < 1) {
                canvas.drawLine(boundaryX, boundaryY, screenX(destination), screenY(destination), plannedRoutePaint);
            }
            if (progress > 0) {
                canvas.drawLine(screenX(origin), screenY(origin), boundaryX, boundaryY,
                      active ? activeRoutePaint : plannedRoutePaint);
            }
            if (active || progress > 0) {
                drawn++;
                routeLegPlacements.add(new RouteLegPlacement(active, origin.getId(), destination.getId(), progress));
            }
            if (active && progress > 0 && progress < 1) {
                float detail = navigationAlpha();
                starPaint.setColor(withOpacity(ACTIVE_ROUTE_COLOR, 80 / 255f * detail));
                canvas.drawCircle(boundaryX, boundaryY, UIUtil.scaleForGUI(4), starPaint);
                starPaint.setColor(withOpacity(0xFFFFD589, detail));
                canvas.drawCircle(boundaryX, boundaryY, UIUtil.scaleForGUI(1.7f), starPaint);
            }
        }
        return drawn;
    }

    private static int withOpacity(int color, float alpha) {
        return (color & 0xFFFFFF) | (Math.round(255 * Math.clamp(alpha, 0, 1)) << 24);
    }

    private static int routeColor(double progress) {
        int color = 0xFF000000;
        for (int shift = 0; shift <= 16; shift += 8) {
            int start = (PLANNED_ROUTE_COLOR >>> shift) & 255;
            int end = (ACTIVE_ROUTE_COLOR >>> shift) & 255;
            color |= (int) Math.round(start + (end - start) * progress) << shift;
        }
        return color;
    }

    private float navigationAlpha() {
        return (float) InterstellarMapPanel.SemanticZoomProfile.create(state.scale(),
              presentation.labelZoomReference()).detailedOverlayAlpha();
    }

    private void drawRouteMarkers(Canvas canvas, List<PlanetarySystem> route, int color) {
        float detail = (float) InterstellarMapPanel.SemanticZoomProfile.create(state.scale(),
              presentation.labelZoomReference()).detailedOverlayAlpha();
        float unit = UIUtil.scaleForGUI(1);
        var routeState = color == ACTIVE_ROUTE_COLOR ? InterstellarMapPanel.RouteMarkerState.ACTIVE
              : InterstellarMapPanel.RouteMarkerState.PLANNED;
          boolean active = color == ACTIVE_ROUTE_COLOR;
          int revealed = active || plannedProgress >= 1 ? route.size()
              : Math.min(route.size(), (int) Math.floor(plannedProgress * (route.size() - 1)) + 1);
          double position = InterstellarMapPanel.easeInOutCubic(activationProgress) * Math.max(0, route.size() - 1);
          for (int index = 0; index < revealed; index++) {
            PlanetarySystem system = route.get(index);
            var layout = InterstellarMapPanel.SystemMarkerLayout.create(0, 0,
                  presentation.systemStyle().sizeAt(state.scale()) / unit, routeState, false, false);
            float horizontal = screenX(system);
            float vertical = screenY(system);
                        int markerColor = active ? routeColor(InterstellarMapPanel.getRouteWaypointActivation(position,
                            index, activationProgress)) : color;
            if (detail > 0) {
                                markerPaint.setColor(withOpacity(markerColor, detail));
                markerPaint.setStrokeWidth((color == ACTIVE_ROUTE_COLOR ? 2.5f : 1.8f) * unit);
                canvas.drawCircle(horizontal, vertical, (float) layout.navigationRadius() * unit, markerPaint);
            }
            if ((index == revealed - 1) && detail < 1) {
                float radius = (float) Math.max(4.2, layout.ownershipRadius() + 1) * unit;
                markerPaint.setColor(Math.round(220 * (1 - detail)) << 24);
                markerPaint.setStrokeWidth(4 * unit);
                canvas.drawCircle(horizontal, vertical, radius, markerPaint);
                markerPaint.setColor((color & 0xFFFFFF) | (Math.round(255 * (1 - detail)) << 24));
                markerPaint.setStrokeWidth(1.8f * unit);
                canvas.drawCircle(horizontal, vertical, radius, markerPaint);
            }
        }
        markerPaint.setStrokeWidth(UIUtil.scaleForGUI(2));
    }

    private void drawTransitMarker(Canvas canvas) {
        Routes routes = presentation.routes();
        transitMarkerDrawn = routes.inTransit() && (routes.currentSystem() != null);
        if (!transitMarkerDrawn) {
            return;
        }
        float horizontal = screenX(routes.currentSystem());
        float vertical = screenY(routes.currentSystem());
        float radius = transitRadius(routes.currentSystem());
          float detail = navigationAlpha();
          markerPaint.setColor(withOpacity(0xFF45606C, detail));
        canvas.drawCircle(horizontal, vertical, radius, markerPaint);
        float sweep = (float) (routes.planetProximity() * 360);
          activeRoutePaint.setAlphaf(detail);
        canvas.drawArc(horizontal - radius, vertical - radius, horizontal + radius, vertical + radius,
              -90, sweep, false, activeRoutePaint);
          activeRoutePaint.setAlphaf(1);
        double angle = Math.toRadians(sweep - 90);
        starPaint.setColor(withOpacity(CURRENT_COLOR, detail));
        canvas.drawCircle(horizontal + radius * (float) Math.cos(angle),
              vertical + radius * (float) Math.sin(angle), UIUtil.scaleForGUI(3), starPaint);
    }

    private void loadFleetImage() {
        try {
            fleetImage = Image.Companion.makeFromEncoded(Files.readAllBytes(
                  java.nio.file.Path.of(InterstellarMapPanel.CURRENT_LOCATION_ICON_PATH)));
            try (ColorFilter gray = ColorFilter.Companion.makeHighContrast(true,
                  org.jetbrains.skia.InversionMode.NO, 0);
                  ColorFilter lighting = ColorFilter.Companion.makeLighting(0xFFA67B35, 0xFF59431D)) {
                fleetTint = ColorFilter.Companion.makeComposed(lighting, gray);
            }
        } catch (IOException | IllegalArgumentException exception) {
            if (fleetImage != null) {
                fleetImage.close();
                fleetImage = null;
            }
        }
    }

    private InterstellarMapPanel.SystemMarkerLayout routeLayout(PlanetarySystem system) {
        var routeState = presentation.routeSystemIds().contains(system.getId())
              ? InterstellarMapPanel.RouteMarkerState.ACTIVE : InterstellarMapPanel.RouteMarkerState.NONE;
        return InterstellarMapPanel.SystemMarkerLayout.create(0, 0,
              presentation.systemStyle().sizeAt(state.scale()) / UIUtil.scaleForGUI(1), routeState,
              Objects.equals(system, state.selectedSystem()), Objects.equals(system, hoveredSystem));
    }

    private void drawFleet(Canvas canvas) {
        List<FleetPlacement> placed = new ArrayList<>();
        PlanetarySystem current = presentation.routes().currentSystem();
        if (current != null) {
            float detail = navigationAlpha();
            var layout = routeLayout(current);
            float radius = (float) Math.max(3.8, layout.ownershipRadius() + 1) * UIUtil.scaleForGUI(1);
            markerPaint.setColor(withOpacity(CURRENT_COLOR, (1 - detail) * 70 / 255));
            markerPaint.setStrokeWidth(UIUtil.scaleForGUI(4));
            canvas.drawCircle(screenX(current), screenY(current), radius, markerPaint);
            markerPaint.setColor(withOpacity(CURRENT_COLOR, 1 - detail));
            markerPaint.setStrokeWidth(UIUtil.scaleForGUI(1.4f));
            canvas.drawCircle(screenX(current), screenY(current), radius, markerPaint);
            if (hopProgress < 1 && hopOrigin != null) {
                if (hopProgress < InterstellarMapPanel.SYSTEM_HOP_DEPARTURE_END_PROGRESS) {
                    double phase = hopProgress / InterstellarMapPanel.SYSTEM_HOP_DEPARTURE_END_PROGRESS;
                    placed.add(drawFleetShip(canvas, hopOrigin, detail,
                          (float) (1 - InterstellarMapPanel.easeInOutCubic(phase)), (float) Math.sin(Math.PI * phase)));
                } else if (hopProgress >= InterstellarMapPanel.SYSTEM_HOP_ARRIVAL_START_PROGRESS) {
                    double phase = (hopProgress - InterstellarMapPanel.SYSTEM_HOP_ARRIVAL_START_PROGRESS)
                          / (1 - InterstellarMapPanel.SYSTEM_HOP_ARRIVAL_START_PROGRESS);
                    placed.add(drawFleetShip(canvas, current, detail,
                          (float) InterstellarMapPanel.easeInOutCubic(phase), (float) Math.sin(Math.PI * phase)));
                }
            } else {
                placed.add(drawFleetShip(canvas, current, detail, 1, 0));
            }
        }
        markerPaint.setStrokeWidth(UIUtil.scaleForGUI(1));
        fleetPlacements = List.copyOf(placed);
    }

    private FleetPlacement drawFleetShip(Canvas canvas, PlanetarySystem system, float detail, float opacity,
          float shimmer) {
        float unit = UIUtil.scaleForGUI(1);
        var anchor = routeLayout(system).shipAnchor(detail);
        float centerX = screenX(system) + (float) anchor.x * unit;
        float centerY = screenY(system) + (float) anchor.y * unit;
        float size = InterstellarMapPanel.CURRENT_LOCATION_ICON_SIZE * unit;
        float halfWidth = Math.max(size / 2, size * 0.48f + 7 * unit);
        Rect bounds = Rect.makeXYWH(centerX - halfWidth, centerY - size / 2, halfWidth * 2, size);
        if (detail > 0) {
            navigationBounds.add(bounds);
            if (shimmer > 0) {
                float spread = (2 + 5 * shimmer) * unit;
                float shimmerHalfWidth = size * 0.48f;
                markerPaint.setStrokeWidth(1.2f * unit);
                markerPaint.setColor(withOpacity(PLANNED_ROUTE_COLOR, 150 / 255f * shimmer * detail));
                    canvas.drawLine(centerX - shimmerHalfWidth - spread, centerY - 5 * unit,
                        centerX + shimmerHalfWidth - spread, centerY - 5 * unit, markerPaint);
                    canvas.drawLine(centerX - shimmerHalfWidth - spread, centerY + 5 * unit,
                        centerX + shimmerHalfWidth - spread, centerY + 5 * unit, markerPaint);
                markerPaint.setColor(withOpacity(0xFFFFD589, 175 / 255f * shimmer * detail));
                    canvas.drawLine(centerX - shimmerHalfWidth + spread, centerY,
                        centerX + shimmerHalfWidth + spread, centerY, markerPaint);
            }
            if (fleetImage != null) {
                canvas.save();
                try {
                    canvas.translate(centerX, centerY);
                    canvas.rotate(-90);
                    starPaint.setColor(0xFFFFFFFF);
                    starPaint.setAlphaf(detail * opacity);
                    starPaint.setColorFilter(fleetTint);
                      canvas.drawImageRect(fleetImage, Rect.makeWH(fleetImage.getWidth(), fleetImage.getHeight()),
                          Rect.makeXYWH(-size / 2, -size / 2, size, size),
                          org.jetbrains.skia.SamplingMode.Companion.getCATMULL_ROM(), starPaint, true);
                } finally {
                    starPaint.setColorFilter(null);
                    starPaint.setAlphaf(1);
                    canvas.restore();
                }
            } else {
                markerPaint.setColor(withOpacity(CURRENT_COLOR, detail * opacity));
                markerPaint.setStrokeWidth(2 * unit);
                canvas.drawLine(centerX - 5 * unit, centerY - 2 * unit, centerX, centerY + 3 * unit, markerPaint);
                canvas.drawLine(centerX, centerY + 3 * unit, centerX + 5 * unit, centerY - 2 * unit, markerPaint);
            }
        }
        return new FleetPlacement(system.getId(), bounds, 1 - detail, detail * opacity, fleetImage != null);
    }

    private void drawRouteBadges(Canvas canvas, List<Rect> occupied) {
        List<RouteBadge> badges = new ArrayList<>();
        float detail = navigationAlpha();
        if (detail <= 0) {
            routeBadges = List.of();
            return;
        }
        float unit = UIUtil.scaleForGUI(1);
        try (Font badgeFont = new Font(labelTypeface, 11 * unit)) {
            for (int pass = 0; pass < 2; pass++) {
                boolean active = pass == 1;
                List<PlanetarySystem> route = active ? presentation.routes().active() : presentation.routes().planned();
                int limit = active || plannedProgress >= 1 ? route.size()
                      : Math.min(route.size(), (int) Math.floor(plannedProgress * (route.size() - 1)) + 1);
                int number = 0;
                for (int index = 1; index < limit; index++) {
                    PlanetarySystem system = route.get(index);
                    if (!active && !presentation.routes().requestedWaypoints().contains(system.getId())) {
                        continue;
                    }
                    number++;
                    String text = Integer.toString(number);
                    Rect ink = badgeFont.measureText(text, textPaint);
                    float diameter = Math.max(16 * unit, Math.max(ink.getHeight() + 2 * unit, ink.getWidth() + 8 * unit));
                    var anchor = routeLayout(system).routeBadgeAnchor(diameter / unit, detail);
                    float centerX = screenX(system) + (float) anchor.x * unit;
                    float centerY = screenY(system) + (float) anchor.y * unit;
                    Rect bounds = Rect.makeXYWH(centerX - diameter / 2, centerY - diameter / 2, diameter, diameter);
                    if (!landmarkFits(bounds, navigationBounds) || !badgeClearsSystems(bounds, occupied)) {
                        continue;
                    }
                    starPaint.setColor(withOpacity(BACKGROUND_COLOR, 235 / 255f * detail));
                    canvas.drawCircle(centerX, centerY, diameter / 2, starPaint);
                    double position = InterstellarMapPanel.easeInOutCubic(activationProgress) * (route.size() - 1);
                    int color = active ? routeColor(InterstellarMapPanel.getRouteWaypointActivation(position, index,
                          activationProgress)) : PLANNED_ROUTE_COLOR;
                    textPaint.setColor(withOpacity(color, detail));
                    canvas.drawString(text, centerX - (ink.getLeft() + ink.getRight()) / 2,
                          centerY - (ink.getTop() + ink.getBottom()) / 2, badgeFont, textPaint);
                    navigationBounds.add(bounds);
                    badges.add(new RouteBadge(system.getId(), number, active, bounds, detail));
                }
            }
        }
        routeBadges = List.copyOf(badges);
    }

    private static boolean badgeClearsSystems(Rect bounds, List<Rect> systems) {
        float badgeX = (bounds.getLeft() + bounds.getRight()) / 2;
        float badgeY = (bounds.getTop() + bounds.getBottom()) / 2;
        for (Rect system : systems) {
            float centerX = (system.getLeft() + system.getRight()) / 2;
            float centerY = (system.getTop() + system.getBottom()) / 2;
            if (Math.hypot(centerX - badgeX, centerY - badgeY) < (system.getWidth() + bounds.getWidth()) / 2) {
                return false;
            }
        }
        return true;
    }

    private void drawHpgLinks(Canvas canvas) {
        hpgLinkPlacements = List.of();
        HpgNetwork network = presentation.navigation().hpgNetwork();
        if (network == null) {
            return;
        }
        var zoom = InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference());
        var detail = InterstellarMapPanel.effectiveHpgNetworkDetail(network.detail(), state.scale(),
              presentation.labelZoomReference());
        float unit = UIUtil.scaleForGUI(1);
        List<HpgLinkPlacement> placements = new ArrayList<>();
        var viewport = new Rectangle2D.Float(-2 * unit, -2 * unit, getWidth() + 4 * unit, getHeight() + 4 * unit);
        var segment = new java.awt.geom.Line2D.Float();
          try (Paint paint = createRoutePaint(0, 1)) {
            for (var link : network.links()) {
                boolean classA = link.rating() == mekhq.campaign.universe.enums.HPGRating.A;
                if ((!classA && link.rating() != mekhq.campaign.universe.enums.HPGRating.B)
                      || !detail.includes(link.rating())) {
                    continue;
                }
                segment.setLine(screenX(link.primary()), screenY(link.primary()),
                      screenX(link.secondary()), screenY(link.secondary()));
                if (!InterstellarMapPanel.isHpgLinkVisible(segment, viewport)) {
                    continue;
                }
                var color = classA ? InterstellarMapPanel.HPG_CLASS_A_LINK_COLOR : InterstellarMapPanel.HPG_CLASS_B_LINK_COLOR;
                int nativeColor = withOpacity(color.getRGB(), (float) zoom.hpgNetworkAlpha() * color.getAlpha() / 255);
                float width = (classA ? 1.35f : 0.8f) * unit;
                paint.setColor(nativeColor);
                paint.setStrokeWidth(width);
                paint.setStrokeCap(classA ? PaintStrokeCap.ROUND : PaintStrokeCap.BUTT);
                if (classA) {
                    canvas.drawLine(segment.x1, segment.y1, segment.x2, segment.y2, paint);
                } else {
                    var clipped = clipHpgDash(segment, viewport, 16 * unit);
                    if (clipped != null) {
                        drawHpgDashes(canvas, clipped, 8 * unit, paint);
                    }
                }
                placements.add(new HpgLinkPlacement(link.primary().getId(), link.secondary().getId(), link.rating().name(),
                      segment.x1, segment.y1, segment.x2, segment.y2, nativeColor, width, !classA));
            }
        }
        hpgLinkPlacements = List.copyOf(placements);
    }

    public static void drawHpgDashes(Canvas canvas, java.awt.geom.Line2D.Float line, double dashLength, Paint paint) {
        double deltaX = (double) line.x2 - line.x1;
        double deltaY = (double) line.y2 - line.y1;
        double length = Math.hypot(deltaX, deltaY);
        int count = (int) Math.ceil(length / (dashLength * 2));
        float[] points = new float[count * 4];
        for (int index = 0; index < count; index++) {
            double start = index * dashLength * 2 / length;
            double end = Math.min(1, (index * dashLength * 2 + dashLength) / length);
            points[index * 4] = (float) (line.x1 + deltaX * start);
            points[index * 4 + 1] = (float) (line.y1 + deltaY * start);
            points[index * 4 + 2] = (float) (line.x1 + deltaX * end);
            points[index * 4 + 3] = (float) (line.y1 + deltaY * end);
        }
        if (count > 0) {
            canvas.drawLines(points, paint);
        }
    }

    public static java.awt.geom.Line2D.Float clipHpgDash(java.awt.geom.Line2D.Float line,
          Rectangle2D viewport, double period) {
        double deltaX = (double) line.x2 - line.x1;
        double deltaY = (double) line.y2 - line.y1;
        double length = Math.hypot(deltaX, deltaY);
        if (length == 0) {
            return null;
        }
        double start = 0;
        double end = 1;
        for (int axis = 0; axis < 2; axis++) {
            double origin = axis == 0 ? line.x1 : line.y1;
            double delta = axis == 0 ? deltaX : deltaY;
            double minimum = axis == 0 ? viewport.getMinX() : viewport.getMinY();
            double maximum = axis == 0 ? viewport.getMaxX() : viewport.getMaxY();
            if (delta == 0) {
                if (origin < minimum || origin > maximum) {
                    return null;
                }
            } else {
                double first = (minimum - origin) / delta;
                double last = (maximum - origin) / delta;
                start = Math.max(start, Math.min(first, last));
                end = Math.min(end, Math.max(first, last));
            }
        }
        if (start > end) {
            return null;
        }
        double startDistance = Math.floor(start * length / period) * period;
        double endDistance = Math.min(length, Math.ceil(end * length / period) * period);
        return new java.awt.geom.Line2D.Float((float) (line.x1 + deltaX * startDistance / length),
              (float) (line.y1 + deltaY * startDistance / length),
              (float) (line.x1 + deltaX * endDistance / length),
              (float) (line.y1 + deltaY * endDistance / length));
    }

    private void drawHpgStations(Canvas canvas, List<Rect> occupied) {
        hpgStationPlacements = List.of();
        HpgNetwork network = presentation.navigation().hpgNetwork();
        if (network == null) {
            return;
        }
        var zoom = InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference());
        var detail = InterstellarMapPanel.effectiveHpgNetworkDetail(network.detail(), state.scale(),
              presentation.labelZoomReference());
        float unit = UIUtil.scaleForGUI(1);
        List<HpgStationPlacement> placements = new ArrayList<>();
        try (Paint paint = new Paint(); Font font = new Font(labelTypeface, 10 * unit)) {
            paint.setAntiAlias(true);
            for (HpgStation station : network.stations()) {
                SystemPresentation data = systemPresentations.get(station.system().getId());
                float alpha = (float) (zoom.hpgNetworkAlpha() * InterstellarMapPanel.hpgStationMarkerAlpha(
                      station.rating(), zoom.detailedOverlayAlpha(), zoom.systemDetailAlpha()));
                if (data == null || !isVisible(data) || !detail.includes(station.rating()) || alpha <= 0) {
                    continue;
                }
                PlanetarySystem system = station.system();
                var routeState = presentation.routes().planned().contains(system) ? InterstellarMapPanel.RouteMarkerState.PLANNED
                      : presentation.routes().active().contains(system) ? InterstellarMapPanel.RouteMarkerState.ACTIVE
                      : InterstellarMapPanel.RouteMarkerState.NONE;
                var layout = InterstellarMapPanel.SystemMarkerLayout.create(0, 0,
                      presentation.systemStyle().sizeAt(state.scale()) / unit, routeState,
                      Objects.equals(system, state.selectedSystem()), Objects.equals(system, hoveredSystem));
                float radius = (float) InterstellarMapPanel.hpgStationMarkerRadius(layout.size(), station.rating());
                var anchor = layout.hpgStationAnchor(radius);
                float centerX = screenX(system) + (float) anchor.x * unit;
                float centerY = screenY(system) + (float) anchor.y * unit;
                radius *= unit;
                var shape = InterstellarMapPanel.createNavigationMarkerShape(
                      InterstellarMapPanel.NavigationMarkerShape.HEXAGON, centerX, centerY, radius);
                var extent = shape.getBounds2D();
                Rect bounds = Rect.makeLTRB((float) extent.getMinX() - 2 * unit, (float) extent.getMinY() - 2 * unit,
                      (float) extent.getMaxX() + 2 * unit, (float) extent.getMaxY() + 2 * unit);
                if (!landmarkFits(bounds, occupied)) {
                    continue;
                }
                int color = InterstellarMapPanel.hpgStationColor(station.rating()).getRGB();
                try (Path path = toNativePath(shape)) {
                    paint.setMode(PaintMode.STROKE);
                    paint.setStrokeWidth(4 * unit);
                    paint.setStrokeJoin(PaintStrokeJoin.ROUND);
                    paint.setColor(withOpacity(0xFF000000, alpha * 220 / 255));
                    canvas.drawPath(path, paint);
                    paint.setMode(PaintMode.FILL);
                    paint.setColor(withOpacity(color, alpha));
                    canvas.drawPath(path, paint);
                    paint.setMode(PaintMode.STROKE);
                    paint.setStrokeWidth(unit);
                    paint.setColor(withOpacity(0xFFFFFFFF, alpha * 135 / 255));
                    canvas.drawPath(path, paint);
                }
                font.setSize(Math.max(8 * unit, radius * 1.15f));
                String text = station.rating().name();
                Rect ink = font.measureText(text, textPaint);
                textPaint.setColor(withOpacity(0xFF000000, alpha));
                canvas.drawString(text, centerX - (ink.getLeft() + ink.getRight()) / 2,
                      centerY - (ink.getTop() + ink.getBottom()) / 2, font, textPaint);
                occupied.add(bounds);
                placements.add(new HpgStationPlacement(system.getId(), text, bounds, color, alpha));
            }
        }
        hpgStationPlacements = List.copyOf(placements);
    }

    private void drawReachability(Canvas canvas, List<Rect> occupied) {
        reachabilityPlacements = List.of();
        reachabilityAnnotationBounds = null;
        Reachability reachability = presentation.navigation().reachability();
        float alpha = navigationAlpha();
        if (reachability == null || alpha <= 0) {
            return;
        }
        float unit = UIUtil.scaleForGUI(1);
        List<ReachabilityPlacement> placements = new ArrayList<>();
        try (Paint paint = createRoutePaint(0, 2);
              PathEffect dash = PathEffect.Companion.makeDash(new float[] { 4 * unit, 3 * unit }, 0);
              Font shellFont = new Font(labelTypeface, 10 * unit)) {
            for (ReachabilityEntry entry : reachability.entries()) {
                SystemPresentation data = systemPresentations.get(entry.system().getId());
                if (data == null || !InterstellarMapPanel.shouldRenderOptionalSystemOverlay(data.empty(),
                      presentation.showEmptySystems(), Objects.equals(entry.system(), state.selectedSystem()),
                      Objects.equals(entry.system(), presentation.routes().currentSystem()))) {
                    continue;
                }
                var style = InterstellarMapPanel.reachabilityMarkerStyle(entry);
                var stroke = InterstellarMapPanel.reachabilityMarkerStroke(style.tone());
                var layout = InterstellarMapPanel.SystemMarkerLayout.create(0, 0,
                      presentation.systemStyle().sizeAt(state.scale()) / unit,
                      InterstellarMapPanel.RouteMarkerState.NONE, false, false);
                float radius = (float) Math.max(6, InterstellarMapPanel.navigationMarkerPathRadius(style.shape(),
                      layout.navigationClearanceRadius(), stroke.getLineWidth())) * unit;
                float horizontal = screenX(entry.system());
                float vertical = screenY(entry.system());
                float halfStroke = stroke.getLineWidth() * unit / 2;
                var shape = InterstellarMapPanel.createNavigationMarkerShape(style.shape(), horizontal, vertical, radius);
                var shapeBounds = shape.getBounds2D();
                Rect bounds = Rect.makeLTRB((float) shapeBounds.getMinX() - halfStroke,
                      (float) shapeBounds.getMinY() - halfStroke, (float) shapeBounds.getMaxX() + halfStroke,
                      (float) shapeBounds.getMaxY() + halfStroke);
                if (bounds.getRight() < 0 || bounds.getLeft() > getWidth()
                      || bounds.getBottom() < 0 || bounds.getTop() > getHeight()) {
                    continue;
                }
                int color = InterstellarMapPanel.markerColor(style.tone()).getRGB();
                paint.setColor(withOpacity(color, alpha * (entry.blocked() ? 225 : 185) / 255));
                paint.setStrokeWidth(stroke.getLineWidth() * unit);
                paint.setStrokeCap(entry.blocked() ? PaintStrokeCap.SQUARE : PaintStrokeCap.ROUND);
                paint.setStrokeJoin(entry.blocked() ? PaintStrokeJoin.MITER : PaintStrokeJoin.ROUND);
                paint.setPathEffect(entry.blocked() ? dash : null);
                try (Path path = toNativePath(shape)) {
                    canvas.drawPath(path, paint);
                }
                occupied.add(bounds);
                Rect shellBounds = null;
                if (!entry.blocked() && !entry.caution()) {
                    String shell = Integer.toString(entry.minimumHops());
                    Rect ink = shellFont.measureText(shell, textPaint);
                    float left = horizontal + radius + 2 * unit;
                    float top = vertical - radius;
                    Rect candidate = Rect.makeXYWH(left, top, ink.getWidth(), ink.getHeight());
                    if (landmarkFits(candidate, occupied) && landmarkFits(candidate, navigationBounds)) {
                        textPaint.setColor(withOpacity(color, alpha));
                        canvas.drawString(shell, left - ink.getLeft(), top - ink.getTop(), shellFont, textPaint);
                        shellBounds = candidate;
                        occupied.add(candidate);
                    }
                }
                placements.add(new ReachabilityPlacement(entry.system().getId(), entry.minimumHops(), entry.caution(),
                      entry.blocked(), style.shape().name(), color, alpha, bounds, shellBounds));
            }
            PlanetarySystem anchor = reachability.anchor();
            Rect ink = shellFont.measureText(reachability.label(), textPaint);
            float offset = Math.max(9 * unit, (float) presentation.systemStyle().sizeAt(state.scale()) * 1.8f);
            for (int direction : new int[] { -1, 1 }) {
                float left = screenX(anchor) + offset;
                float top = screenY(anchor) + direction * (offset + ink.getHeight());
                Rect bounds = Rect.makeXYWH(left - 2 * unit, top - 2 * unit,
                      ink.getWidth() + 4 * unit, ink.getHeight() + 4 * unit);
                if (landmarkFits(bounds, occupied) && landmarkFits(bounds, navigationBounds)) {
                    paint.setMode(PaintMode.FILL);
                    paint.setPathEffect(null);
                    paint.setColor(withOpacity(BACKGROUND_COLOR, alpha * 0.9f));
                    canvas.drawRect(bounds, paint);
                    textPaint.setColor(withOpacity(PLANNED_ROUTE_COLOR, alpha));
                    canvas.drawString(reachability.label(), left - ink.getLeft(), top - ink.getTop(), shellFont, textPaint);
                    reachabilityAnnotationBounds = bounds;
                    occupied.add(bounds);
                    break;
                }
            }
        }
        reachabilityPlacements = List.copyOf(placements);
    }

    private void drawJumpRadius(Canvas canvas) {
        Navigation navigation = presentation.navigation();
        jumpRadiusDrawn = state.selectedSystem() != null && navigation.jumpRadius() > 0
              && state.scale() > navigation.minimumRangeZoom();
        if (jumpRadiusDrawn) {
            markerPaint.setColor(navigation.rangeColor());
            canvas.drawCircle(screenX(state.selectedSystem()), screenY(state.selectedSystem()),
                  (float) (navigation.jumpRadius() * state.scale()), markerPaint);
        }
    }

    private void drawConstraints(Canvas canvas) {
        constraintMarkersDrawn = 0;
        float size = UIUtil.scaleForGUI(10);
        try (Paint paint = createRoutePaint(0, 2);
              PathEffect dashes = PathEffect.Companion.makeDash(
                    new float[] { UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(5) }, 0)) {
            for (RouteConstraint constraint : presentation.navigation().constraints()) {
                float horizontal = screenX(constraint.destination());
                float vertical = screenY(constraint.destination());
                paint.setColor(constraint.blocked() ? 0xFFF07373 : 0xFFF4BB62);
                if (constraint.blocked()) {
                    paint.setPathEffect(dashes);
                    canvas.drawLine(screenX(constraint.origin()), screenY(constraint.origin()), horizontal, vertical, paint);
                    paint.setPathEffect(null);
                }
                if (horizontal + size < 0 || horizontal - size > getWidth()
                      || vertical + size < 0 || vertical - size > getHeight()) {
                    continue;
                }
                if (constraint.blocked()) {
                    canvas.drawRect(Rect.makeXYWH(horizontal - size, vertical - size, size * 2, size * 2), paint);
                } else {
                    canvas.drawLine(horizontal, vertical - size, horizontal + size, vertical + size, paint);
                    canvas.drawLine(horizontal + size, vertical + size, horizontal - size, vertical + size, paint);
                    canvas.drawLine(horizontal - size, vertical + size, horizontal, vertical - size, paint);
                }
                constraintMarkersDrawn++;
            }
        }
    }

    private void drawMeasurement(Canvas canvas) {
        measurementBounds = null;
        Measurement measurement = presentation.navigation().measurement();
        if (!measurement.enabled() || measurement.start() == null) {
            return;
        }
        float startX = screenX(measurement.start());
        float startY = screenY(measurement.start());
        float radius = UIUtil.scaleForGUI(6);
        try (Paint paint = createRoutePaint(STAR_COLOR, 2);
              PathEffect dash = PathEffect.Companion.makeDash(new float[] {
                  UIUtil.scaleForGUI(9), UIUtil.scaleForGUI(4), UIUtil.scaleForGUI(2), UIUtil.scaleForGUI(4) }, 0)) {
            canvas.drawCircle(startX, startY, radius, paint);
            if (measurement.end() == null) {
                return;
            }
            float endX = screenX(measurement.end());
            float endY = screenY(measurement.end());
            paint.setPathEffect(dash);
            canvas.drawLine(startX, startY, endX, endY, paint);
            paint.setPathEffect(null);
            canvas.drawLine(endX, endY - radius, endX + radius, endY, paint);
            canvas.drawLine(endX + radius, endY, endX, endY + radius, paint);
            canvas.drawLine(endX, endY + radius, endX - radius, endY, paint);
            canvas.drawLine(endX - radius, endY, endX, endY - radius, paint);
            if (!measurement.label().isBlank()) {
                measurementBounds = drawInfoBox(canvas, measurement.label(),
                        (startX + endX) / 2, (startY + endY) / 2 + UIUtil.scaleForGUI(12), null);
            }
        }
    }

    private Rect drawInfoBox(Canvas canvas, String text, float preferredX, float preferredY, Rect exclusion) {
        float padding = UIUtil.scaleForGUI(6);
        float maximumWidth = Math.max(1, Math.min(UIUtil.scaleForGUI(420), getWidth() - padding * 4));
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\\n")) {
            String remaining = paragraph;
            while (!remaining.isEmpty()) {
                int end = remaining.length();
                while (end > 1 && labelFont.measureText(remaining.substring(0, end), textPaint).getWidth() > maximumWidth) {
                    end = remaining.offsetByCodePoints(end, -1);
                }
                if (end < remaining.length()) {
                    int space = remaining.lastIndexOf(' ', end);
                    if (space > 0) {
                        end = space;
                    }
                }
                lines.add(remaining.substring(0, end));
                remaining = remaining.substring(end).stripLeading();
            }
        }
        Rect fontBounds = labelFont.measureText("Mg", textPaint);
        float lineHeight = fontBounds.getHeight() + UIUtil.scaleForGUI(5);
        float textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, labelFont.measureText(line, textPaint).getWidth());
        }
        float width = Math.min(getWidth(), textWidth + padding * 2);
        float height = Math.min(getHeight(), lines.size() * lineHeight + padding * 2);
        float left = Math.clamp(preferredX, 0, getWidth() - width);
        float top = Math.clamp(preferredY, 0, getHeight() - height);
        Rect bounds = Rect.makeXYWH(left, top, width, height);
        if (exclusion != null && overlaps(bounds, exclusion)) {
            float gap = UIUtil.scaleForGUI(6);
            if (exclusion.getBottom() + gap + height <= getHeight()) {
                top = exclusion.getBottom() + gap;
            } else if (exclusion.getTop() - gap - height >= 0) {
                top = exclusion.getTop() - gap - height;
            } else if (exclusion.getRight() + gap + width <= getWidth()) {
                left = exclusion.getRight() + gap;
            } else if (exclusion.getLeft() - gap - width >= 0) {
                left = exclusion.getLeft() - gap - width;
            } else {
                return null;
            }
            bounds = Rect.makeXYWH(left, top, width, height);
        }
        textPaint.setColor(0xFF07101B);
        canvas.drawRect(bounds, textPaint);
        markerPaint.setColor(0xFF416A79);
        canvas.drawRect(bounds, markerPaint);
        textPaint.setColor(STAR_COLOR);
        canvas.save();
        try {
            canvas.clipRect(bounds);
            float baseline = top + padding - fontBounds.getTop();
            for (String line : lines) {
                Rect ink = labelFont.measureText(line, textPaint);
                canvas.drawString(line, left + padding - ink.getLeft(), baseline, labelFont, textPaint);
                baseline += lineHeight;
            }
        } finally {
            canvas.restore();
        }
        return bounds;
    }

    private float markerRadius(PlanetarySystem system) {
        float artRadius = focusRadius(system) + UIUtil.scaleForGUI(2);
        if (Objects.equals(system, state.selectedSystem()) || Objects.equals(system, hoveredSystem)) {
            var layout = focusLayout(system);
            artRadius = Math.max(artRadius, (float) layout.hoveredRadius() * UIUtil.scaleForGUI(1)
                  + UIUtil.scaleForGUI(3));
        }
        if (presentation.routes().inTransit() && Objects.equals(system, presentation.routes().currentSystem())) {
            return Math.max(artRadius, transitRadius(system) + UIUtil.scaleForGUI(4));
        }
        if (presentation.routeSystemIds().contains(system.getId())) {
            return Math.max(artRadius, routeMarkerRadius(system, 8) + UIUtil.scaleForGUI(3));
        }
        return Objects.equals(system, state.selectedSystem()) || Objects.equals(system, hoveredSystem)
              || Objects.equals(system, campaign.getCurrentSystem())
              ? artRadius : systemArtRadius(system);
    }

    private InterstellarMapPanel.SystemMarkerLayout focusLayout(PlanetarySystem system) {
        return InterstellarMapPanel.SystemMarkerLayout.create(0, 0,
              presentation.systemStyle().sizeAt(state.scale()) / UIUtil.scaleForGUI(1),
              InterstellarMapPanel.RouteMarkerState.NONE, Objects.equals(system, state.selectedSystem()),
              Objects.equals(system, hoveredSystem));
    }

    private void drawFocusMarkers(Canvas canvas) {
        List<FocusPlacement> placed = new ArrayList<>();
        var zoom = InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference());
        try (Paint paint = new Paint()) {
            paint.setAntiAlias(true);
            paint.setMode(PaintMode.STROKE);
            if (hoveredSystem != null && !Objects.equals(hoveredSystem, state.selectedSystem())) {
                placed.add(drawFocusMarker(canvas, hoveredSystem, false, zoom, paint, 0));
            }
            if (state.selectedSystem() != null) {
                placed.add(drawFocusMarker(canvas, state.selectedSystem(), true, zoom, paint, selectionProgress));
            }
        }
        focusPlacements = List.copyOf(placed);
    }

    private FocusPlacement drawFocusMarker(Canvas canvas, PlanetarySystem system, boolean selected,
            InterstellarMapPanel.SemanticZoomProfile zoom, Paint paint, double progress) {
        var layout = focusLayout(system);
        float unit = UIUtil.scaleForGUI(1);
        float ringAlpha = (float) (1 - zoom.detailedOverlayAlpha());
        float bracketAlpha = (float) zoom.detailedOverlayAlpha();
        double eased = InterstellarMapPanel.easeOutCubic(progress);
        double radius = selected ? layout.hoveredRadius() + (layout.selectedRadius() - layout.hoveredRadius()) * eased
              : layout.hoveredRadius();
        double hoveredLength = Math.min(5, layout.hoveredRadius() * 0.45);
        double selectedLength = Math.min(5, layout.selectedRadius() * 0.47);
        double length = selected ? hoveredLength + (selectedLength - hoveredLength) * eased : hoveredLength;
        double ringRadius = Math.max(4, layout.ownershipRadius() + 0.8);
        canvas.save();
        try {
            canvas.translate(screenX(system), screenY(system));
            canvas.scale(unit, unit);
            if (ringAlpha > 0) {
                paint.setColor(0xDC000000);
                paint.setAlphaf(ringAlpha * 220 / 255f);
                paint.setStrokeWidth(4);
                canvas.drawCircle(0, 0, (float) ringRadius, paint);
                paint.setColor(selected ? ACTIVE_ROUTE_COLOR : PLANNED_ROUTE_COLOR);
                paint.setAlphaf(ringAlpha);
                paint.setStrokeWidth(1.8f);
                canvas.drawCircle(0, 0, (float) ringRadius, paint);
            }
            if (bracketAlpha > 0) {
                try (Path brackets = toNativePath(InterstellarMapPanel.createCornerBrackets(0, 0, radius, length))) {
                    if (selected) {
                        paint.setStrokeCap(PaintStrokeCap.SQUARE);
                        paint.setStrokeJoin(PaintStrokeJoin.MITER);
                        paint.setStrokeWidth((float) (1.1 + 2.9 * eased));
                        paint.setColor(ACTIVE_ROUTE_COLOR);
                        paint.setAlphaf(bracketAlpha * (float) (70 * eased / 255));
                        canvas.drawPath(brackets, paint);
                    }
                    boolean settled = selected && progress >= 1;
                    paint.setStrokeCap(settled ? PaintStrokeCap.SQUARE : PaintStrokeCap.ROUND);
                    paint.setStrokeJoin(settled ? PaintStrokeJoin.MITER : PaintStrokeJoin.ROUND);
                    paint.setStrokeWidth((float) (selected ? 1.1 + 1.2 * eased : 1.1));
                    Color hovered = new Color(PLANNED_ROUTE_COLOR, true);
                    Color active = new Color(ACTIVE_ROUTE_COLOR, true);
                    int red = (int) Math.round(hovered.getRed() + (active.getRed() - hovered.getRed()) * eased);
                    int green = (int) Math.round(hovered.getGreen() + (active.getGreen() - hovered.getGreen()) * eased);
                    int blue = (int) Math.round(hovered.getBlue() + (active.getBlue() - hovered.getBlue()) * eased);
                    paint.setColor(0xFF000000 | red << 16 | green << 8 | blue);
                    paint.setAlphaf(bracketAlpha * (float) ((190 + 65 * eased) / 255));
                    canvas.drawPath(brackets, paint);
                }
            }
        } finally {
            canvas.restore();
        }
        float extent = (float) Math.max(radius + 3, ringRadius + 2) * unit;
        return new FocusPlacement(system.getId(), selected, ringAlpha, bracketAlpha, progress,
              Rect.makeXYWH(screenX(system) - extent, screenY(system) - extent, extent * 2, extent * 2));
    }

    private float focusRadius(PlanetarySystem system) {
        return Math.max(UIUtil.scaleForGUI(7), systemArtRadius(system) + UIUtil.scaleForGUI(3));
    }

    private float routeMarkerRadius(PlanetarySystem system, int size) {
        var layout = routeLayout(system);
        double radius = navigationAlpha() > 0 ? layout.navigationRadius()
              : Math.max(4.2, layout.ownershipRadius() + 1);
        return (float) radius * UIUtil.scaleForGUI(1);
    }

    private float transitRadius(PlanetarySystem system) {
        return Math.max(UIUtil.scaleForGUI(13), routeMarkerRadius(system, 8) + UIUtil.scaleForGUI(5));
    }

    private boolean isNavigationContact(SystemPresentation data) {
        return data.empty() && !presentation.showEmptySystems()
              && presentation.routeSystemIds().contains(data.system().getId());
    }

    private float systemArtRadius(PlanetarySystem system) {
        double size = presentation.systemStyle().sizeAt(state.scale());
        var data = systemPresentations.get(system.getId());
        if (data != null && isNavigationContact(data)) {
            return (float) Math.max(UIUtil.scaleForGUI(1.5f), size * 0.9);
        }
        var zoom = InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference());
        if (zoom.systemDetailAlpha() <= 0) {
            return (float) Math.max(UIUtil.scaleForGUI(1.8f), Math.min(UIUtil.scaleForGUI(3.2f), size * 0.62));
        }
        double luminosity = data == null ? 1 : data.star().luminosityScale();
        return (float) Math.max(size + UIUtil.scaleForGUI(5.1f),
              Math.max(UIUtil.scaleForGUI(2), size * 1.65) * luminosity);
    }

    private void drawLandmarks(Canvas canvas, List<SystemPresentation> visible, List<Rect> occupied) {
        List<LandmarkPlacement> placed = new ArrayList<>();
        capitalLabelIds.clear();
        var zoom = InterstellarMapPanel.SemanticZoomProfile.create(state.scale(), presentation.labelZoomReference());
        var detail = switch (presentation.landmarks().capitals()) {
            case REGIONS -> InterstellarMapPanel.CapitalDisplayDetail.NATIONAL_REGION;
            case DISTRICTS -> InterstellarMapPanel.CapitalDisplayDetail.ALL;
            default -> InterstellarMapPanel.CapitalDisplayDetail.NATIONAL;
        };
        float unit = UIUtil.scaleForGUI(1);
        try (Paint fill = new Paint(); Paint edge = new Paint()) {
            fill.setAntiAlias(true);
            edge.setAntiAlias(true);
            edge.setMode(PaintMode.STROKE);
            for (int pass = 0; pass < 2; pass++) {
                for (SystemPresentation data : visible) {
                    boolean capital = pass == 0;
                    float alpha = (float) (capital
                          ? presentation.landmarks().capitals() == CapitalDetail.OFF ? 0
                                : InterstellarMapPanel.capitalVisibilityAlpha(data.capitalType(), detail,
                                      zoom.capitalAlpha(), zoom.detailedOverlayAlpha())
                          : presentation.landmarks().rechargeStations() && data.rechargeStations() > 0
                                ? zoom.serviceAlpha() : 0);
                    if (alpha <= 0 || (capital && data.capitalType() == CapitalType.NONE)) {
                        continue;
                    }
                    int count = capital ? Math.max(1, data.capitalColors().size()) : 1;
                    float width = count * 12 * unit;
                    float height = (capital ? 12 : 10) * unit;
                    float left = screenX(data.system()) - width / 2;
                    float top = capital ? screenY(data.system()) - markerRadius(data.system()) - 3 * unit - height
                          : screenY(data.system()) + markerRadius(data.system()) + 3 * unit;
                    Rect bounds = Rect.makeXYWH(left, top, width, height);
                    if (!landmarkFits(bounds, occupied)) {
                        continue;
                    }
                    if (capital) {
                        for (int index = 0; index < count; index++) {
                            int color = !data.capitalColors().isEmpty() ? data.capitalColors().get(index)
                                  : data.factionColors().isEmpty() ? STAR_COLOR : data.factionColors().getFirst();
                            drawCapital(canvas, left + (6 + index * 12) * unit, top + 6 * unit,
                                  data.capitalType(), color, alpha, fill, edge);
                        }
                        capitalLabelIds.add(data.system().getId());
                    } else {
                        Rect battery = Rect.makeXYWH(left + unit, top + 2 * unit, 9 * unit, 6 * unit);
                        fill.setColor(BACKGROUND_COLOR);
                        fill.setAlphaf(alpha);
                        canvas.drawRect(battery, fill);
                        edge.setColor(0xFF80DED0);
                        edge.setAlphaf(alpha);
                        edge.setStrokeWidth(unit);
                        canvas.drawRect(battery, edge);
                        canvas.drawLine(left + 11 * unit, top + 4 * unit,
                              left + 11 * unit, top + 6 * unit, edge);
                        fill.setColor(0xFF80DED0);
                        fill.setAlphaf(alpha);
                        for (int station = 0; station < data.rechargeStations(); station++) {
                            canvas.drawRect(Rect.makeXYWH(left + (3 + station * 4) * unit,
                                  top + 4 * unit, 2 * unit, 2 * unit), fill);
                        }
                    }
                    occupied.add(bounds);
                    placed.add(new LandmarkPlacement(data.system().getId(), capital ? data.capitalType() : CapitalType.NONE,
                          capital ? 0 : data.rechargeStations(), bounds, alpha));
                }
            }
        }
        landmarkPlacements = List.copyOf(placed);
    }

    private boolean landmarkFits(Rect bounds, List<Rect> occupied) {
        if (bounds.getLeft() < 0 || bounds.getTop() < 0 || bounds.getRight() > getWidth()
              || bounds.getBottom() > getHeight()) {
            return false;
        }
        for (Rect other : occupied) {
            if (overlaps(bounds, other)) {
                return false;
            }
        }
        return true;
    }

    private void drawCapital(Canvas canvas, float horizontal, float vertical, CapitalType type, int color,
          float alpha, Paint fill, Paint edge) {
        float radius = UIUtil.scaleForGUI(type == CapitalType.DISTRICT ? 3 : 4);
        try (PathBuilder builder = new PathBuilder()) {
            if (type == CapitalType.DISTRICT) {
                builder.addCircle(horizontal, vertical, radius, PathDirection.CLOCKWISE);
            } else {
                int rays = type == CapitalType.NATIONAL ? 5 : 4;
                for (int point = 0; point < rays * 2; point++) {
                    double angle = -Math.PI / 2 + point * Math.PI / rays;
                    double length = point % 2 == 0 ? radius : radius * (rays == 5 ? 0.47 : 0.32);
                    float pointX = horizontal + (float) (Math.cos(angle) * length);
                    float pointY = vertical + (float) (Math.sin(angle) * length);
                    if (point == 0) {
                        builder.moveTo(pointX, pointY);
                    } else {
                        builder.lineTo(pointX, pointY);
                    }
                }
                builder.closePath();
            }
            try (Path path = builder.detach()) {
                edge.setColor(BACKGROUND_COLOR);
                edge.setAlphaf(alpha);
                edge.setStrokeWidth(UIUtil.scaleForGUI(3));
                canvas.drawPath(path, edge);
                fill.setColor(color);
                fill.setAlphaf(alpha);
                canvas.drawPath(path, fill);
                edge.setColor(STAR_COLOR);
                edge.setAlphaf(alpha * 0.65f);
                edge.setStrokeWidth(UIUtil.scaleForGUI(1));
                canvas.drawPath(path, edge);
            }
        }
    }

    private boolean isVisible(SystemPresentation data) {
        return InterstellarMapPanel.shouldRenderSystem(true, data.empty(), presentation.showEmptySystems(),
              Objects.equals(data.system(), state.selectedSystem())
                    || Objects.equals(data.system(), campaign.getCurrentSystem())
                    || presentation.routeSystemIds().contains(data.system().getId()));
    }

    private SystemPlacement drawStar(Canvas canvas, SystemPresentation data, float horizontal, float vertical,
          InterstellarMapPanel.SemanticZoomProfile zoom) {
        double size = presentation.systemStyle().sizeAt(state.scale());
        float detailAlpha = (float) zoom.systemDetailAlpha();
        float contactAlpha = (float) zoom.systemContactAlpha();
        float auraRadius = (float) (Math.max(UIUtil.scaleForGUI(2), size * 1.65) * data.star().luminosityScale());
        float coreRadius = (float) (Math.max(UIUtil.scaleForGUI(0.65f), size * 0.3) * data.star().luminosityScale());
        boolean navigationContact = isNavigationContact(data);
        starPaint.setShader(null);
        starPaint.setMode(PaintMode.FILL);
        if (navigationContact) {
            starPaint.setColor(0x37A5B8BE);
            canvas.drawCircle(horizontal, vertical, (float) Math.max(UIUtil.scaleForGUI(1.5f), size * 0.9), starPaint);
            starPaint.setColor(0xFFA5B8BE);
            canvas.drawCircle(horizontal, vertical, (float) Math.max(UIUtil.scaleForGUI(0.8f), size * 0.46), starPaint);
            starPaint.setColor(0xFFD2DBDE);
            canvas.drawCircle(horizontal, vertical, (float) Math.max(UIUtil.scaleForGUI(0.35f), size * 0.16), starPaint);
        } else {
            if (detailAlpha > 0) {
                Shader shader = starShaders.computeIfAbsent(data.star().spectralColor(),
                      color -> createStarShader(data.star()));
                canvas.save();
                try {
                    canvas.translate(horizontal, vertical);
                    canvas.scale(auraRadius, auraRadius);
                    starPaint.setShader(shader);
                    starPaint.setAlphaf(detailAlpha);
                    canvas.drawCircle(0, 0, 1, starPaint);
                } finally {
                    canvas.restore();
                    starPaint.setShader(null);
                }
                starPaint.setColor(data.star().coreColor());
                starPaint.setAlphaf(detailAlpha * 250 / 255f);
                canvas.drawCircle(horizontal, vertical, coreRadius, starPaint);
            }
            boolean factionMode = presentation.mapMode() == InterstellarMapPanel.MapMode.FACTION;
            List<Integer> colors = data.empty() ? List.of(0xFF697880)
                : factionMode ? data.factionColors() : List.of(data.analyticalColor());
            float radius = (float) Math.max(UIUtil.scaleForGUI(1.8f), Math.min(UIUtil.scaleForGUI(3.2f), size * 0.62));
            float sweep = colors.isEmpty() ? 360 : 360f / colors.size();
            if (contactAlpha > 0) {
                for (int index = 0; index < colors.size(); index++) {
                    starPaint.setColor(colors.get(index));
                    starPaint.setAlphaf(contactAlpha);
                    canvas.drawArc(horizontal - radius, vertical - radius, horizontal + radius, vertical + radius,
                          -90 + index * sweep, sweep, true, starPaint);
                }
            }
            float ringDetail = (float) (InterstellarMapPanel.isServiceMapMode(presentation.mapMode())
                ? zoom.serviceAlpha() : detailAlpha);
            if (ringDetail > 0 && (!data.empty() || presentation.showEmptySystems())) {
                radius = (float) size + UIUtil.scaleForGUI(2.8f);
                starPaint.setMode(PaintMode.STROKE);
                starPaint.setStrokeCap(PaintStrokeCap.BUTT);
                if (!factionMode || data.empty()) {
                    starPaint.setColor(factionMode ? 0xFF404040 : data.analyticalColor());
                    starPaint.setAlphaf(ringDetail);
                    starPaint.setStrokeWidth(UIUtil.scaleForGUI(2.3f));
                    canvas.drawCircle(horizontal, vertical, radius, starPaint);
                } else if (!colors.isEmpty()) {
                    float ringAlpha = Math.min(1, detailAlpha * 1.75f);
                    starPaint.setColor(0xFF000000);
                    starPaint.setAlphaf(ringAlpha * 210 / 255f);
                    starPaint.setStrokeWidth(UIUtil.scaleForGUI(4.6f));
                    canvas.drawCircle(horizontal, vertical, radius, starPaint);
                    starPaint.setStrokeWidth(UIUtil.scaleForGUI(2.4f));
                    for (int index = 0; index < colors.size(); index++) {
                        starPaint.setColor(colors.get(index));
                        starPaint.setAlphaf(ringAlpha);
                        canvas.drawArc(horizontal - radius, vertical - radius, horizontal + radius, vertical + radius,
                              -90 + index * sweep, sweep, false, starPaint);
                    }
                }
                starPaint.setMode(PaintMode.FILL);
            }
        }
        starPaint.setAlphaf(1);
        return new SystemPlacement(data.system().getId(), systemArtRadius(data.system()),
              navigationContact ? 1 : contactAlpha, navigationContact ? 0 : detailAlpha,
              navigationContact, data.star().spectralColor(), auraRadius, coreRadius);
    }

    private Shader createStarShader(StarAppearance appearance) {
        int[] colors = { appearance.coreColor(), appearance.spectralColor(), appearance.spectralColor(),
            appearance.spectralColor(), appearance.spectralColor() };
        int[] alphas = { 245, 225, 130, 42, 0 };
        Color4f[] stops = new Color4f[colors.length];
        for (int index = 0; index < colors.length; index++) {
            int color = colors[index];
            stops[index] = new Color4f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f,
                  (color & 255) / 255f, alphas[index] / 255f);
        }
        Gradient gradient = new Gradient(new Gradient.Colors(stops, new float[] { 0, 0.16f, 0.38f, 0.68f, 1 },
              FilterTileMode.CLAMP, null), new Gradient.Interpolation());
        try {
            Shader shader = (Shader) radialGradientFactory.invoke(Shader.Companion, 0f, 0f, 1f, gradient, null);
            starShaderBuilds++;
            return shader;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Skiko radial-gradient factory unavailable", exception);
        }
    }

    private void drawLabels(Canvas canvas, List<SystemPresentation> visible, List<Rect> markers) {
        List<LabelPlacement> placed = new ArrayList<>();
        List<Rect> priorityMarkers = new ArrayList<>();
        priorityMarkers.addAll(navigationBounds);
        for (HpgStationPlacement station : hpgStationPlacements) {
            priorityMarkers.add(station.bounds());
        }
        for (ReachabilityPlacement marker : reachabilityPlacements) {
            priorityMarkers.add(marker.bounds());
            if (marker.shellBounds() != null) {
                priorityMarkers.add(marker.shellBounds());
            }
        }
        if (reachabilityAnnotationBounds != null) {
            priorityMarkers.add(reachabilityAnnotationBounds);
        }
        for (LandmarkPlacement landmark : landmarkPlacements) {
            priorityMarkers.add(landmark.bounds());
        }
        for (SystemPresentation data : visible) {
            if (Objects.equals(data.system(), state.selectedSystem())
                || Objects.equals(data.system(), campaign.getCurrentSystem())
                || Objects.equals(data.system(), hoveredSystem)) {
                float radius = markerRadius(data.system());
                priorityMarkers.add(Rect.makeLTRB(screenX(data.system()) - radius, screenY(data.system()) - radius,
                      screenX(data.system()) + radius, screenY(data.system()) + radius));
            }
        }
        InterstellarMapPanel.SemanticZoomProfile zoom = InterstellarMapPanel.SemanticZoomProfile.create(
              state.scale(), presentation.labelZoomReference());
        for (int priority = 0; priority < 5; priority++) {
            for (SystemPresentation data : visible) {
                int systemPriority = Objects.equals(data.system(), state.selectedSystem()) ? 0
                      : Objects.equals(data.system(), campaign.getCurrentSystem()) ? 1
                      : presentation.routeSystemIds().contains(data.system().getId()) ? 2
                      : capitalLabelIds.contains(data.system().getId()) ? 3 : 4;
                if (systemPriority != priority) {
                    continue;
                }
                float alpha = (float) (priority < 2 ? 1.0 : priority == 2
                      ? zoom.routeLabelAlpha() : priority == 3 ? zoom.capitalAlpha() : zoom.ordinaryLabelAlpha());
                if (alpha <= 0) {
                    continue;
                }
                LabelPlacement label = placeLabel(data, alpha, priority < 2 ? priorityMarkers : markers, placed);
                if (label != null) {
                    placed.add(label);
                    if (priority < 2) {
                        textPaint.setColor(BACKGROUND_COLOR);
                        textPaint.setAlphaf(1);
                        canvas.drawRect(label.bounds(), textPaint);
                    }
                    textPaint.setColor(priority < 2 ? 0xFFFFFFFF : STAR_COLOR);
                    textPaint.setAlphaf(alpha);
                    canvas.drawString(label.text(), label.baselineX(), label.baselineY(), labelFont, textPaint);
                }
            }
        }
        labels = List.copyOf(placed);
    }

    private LabelPlacement placeLabel(SystemPresentation data, float alpha, List<Rect> markers,
          List<LabelPlacement> placed) {
        float padding = UIUtil.scaleForGUI(2);
        String name = data.name();
        Rect ink = textMeasurements.computeIfAbsent(name, text -> labelFont.measureText(text, textPaint));
        if ((ink.getWidth() <= 0) || (ink.getHeight() <= 0)) {
            return null;
        }
        float width = ink.getWidth() + 2 * padding;
        float height = ink.getHeight() + 2 * padding;
        float horizontal = screenX(data.system());
        float vertical = screenY(data.system());
        float gap = Math.max(UIUtil.scaleForGUI(10), markerRadius(data.system()) + UIUtil.scaleForGUI(2));
        for (int candidate = 0; candidate < 4; candidate++) {
            float left = switch (candidate) {
                case 0 -> horizontal + gap;
                case 1 -> horizontal - gap - width;
                default -> horizontal - width / 2;
            };
            float top = switch (candidate) {
                case 2 -> vertical - gap - height;
                case 3 -> vertical + gap;
                default -> vertical - height / 2;
            };
            Rect bounds = Rect.makeLTRB(left, top, left + width, top + height);
            if ((left < 0) || (top < 0) || (bounds.getRight() > getWidth()) || (bounds.getBottom() > getHeight())) {
                continue;
            }
            boolean collision = false;
            for (Rect marker : markers) {
                if (overlaps(bounds, marker)) {
                    collision = true;
                    break;
                }
            }
            for (LabelPlacement label : placed) {
                if (overlaps(bounds, label.bounds())) {
                    collision = true;
                    break;
                }
            }
            if (!collision) {
                return new LabelPlacement(data.system().getId(), name, bounds,
                      left + padding - ink.getLeft(), top + padding - ink.getTop(), alpha);
            }
        }
        return null;
    }

    private static boolean overlaps(Rect first, Rect second) {
        return (first.getLeft() < second.getRight()) && (first.getRight() > second.getLeft())
              && (first.getTop() < second.getBottom()) && (first.getBottom() > second.getTop());
    }

    private float screenX(PlanetarySystem system) {
        return (float) (getWidth() / 2.0 + (system.getX() - state.centerX()) * state.scale());
    }

    private float screenY(PlanetarySystem system) {
        return (float) (getHeight() / 2.0 - (system.getY() - state.centerY()) * state.scale());
    }

    private void installInput() {
        MouseAdapter mouse = new MouseAdapter() {
            private boolean popupTriggered;
            private Point position(MouseEvent event) {
                return SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), SkiaMap.this);
            }

            @Override
            public void mousePressed(MouseEvent event) {
                popupTriggered = false;
                clearHover();
                if (event.isPopupTrigger()) {
                    showNavigationPopup(position(event));
                    popupTriggered = true;
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(event) || SwingUtilities.isMiddleMouseButton(event)) {
                    layer.requestFocusInWindow();
                    gesture.press(position(event));
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                Point delta = gesture.drag(position(event));
                if (delta != null) {
                    pan(-delta.x / state.scale(), delta.y / state.scale());
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                if (event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event)) {
                    if (!popupTriggered) {
                        showNavigationPopup(position(event));
                    }
                    popupTriggered = false;
                    return;
                }
                if (gesture.release(position(event)) && SwingUtilities.isLeftMouseButton(event)) {
                    selectAt(position(event), event.getModifiersEx(), event.getClickCount());
                }
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                Point point = position(event);
                PlanetarySystem system = findSystemAt(point);
                hoverPoint = point;
                if (!Objects.equals(hoveredSystem, system)) {
                    hoverTimer.stop();
                    hoveredSystem = system;
                    hoverVisible = false;
                    hoverText = navigationActions.hover(system);
                    for (RouteConstraint constraint : presentation.navigation().constraints()) {
                        if (Objects.equals(system, constraint.destination())) {
                            hoverText += "\n" + constraint.label();
                        }
                    }
                    if (system != null) {
                        hoverTimer.restart();
                    }
                    requestRender();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                clearHover();
                requestRender();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent event) {
                zoom(Math.pow(1.15, -event.getPreciseWheelRotation()), position(event));
                event.consume();
            }
        };
        layer.addMouseListener(mouse);
        layer.addMouseMotionListener(mouse);
        layer.addMouseWheelListener(mouse);
        layer.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                double step = UIUtil.scaleForGUI(40) / state.scale();
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE -> {
                        dismissPopup();
                        navigationActions.stopMeasuring();
                        refresh();
                    }
                    case KeyEvent.VK_CONTEXT_MENU -> showNavigationPopup(selectedAnchor());
                    case KeyEvent.VK_F10 -> {
                        if (!event.isShiftDown()) {
                            return;
                        }
                        showNavigationPopup(selectedAnchor());
                    }
                    case KeyEvent.VK_LEFT -> pan(-step, 0);
                    case KeyEvent.VK_RIGHT -> pan(step, 0);
                    case KeyEvent.VK_UP -> pan(0, step);
                    case KeyEvent.VK_DOWN -> pan(0, -step);
                    case KeyEvent.VK_ADD, KeyEvent.VK_EQUALS -> zoom(1.15, new Point(getWidth() / 2, getHeight() / 2));
                    case KeyEvent.VK_SUBTRACT, KeyEvent.VK_MINUS -> zoom(1 / 1.15, new Point(getWidth() / 2, getHeight() / 2));
                    default -> {
                        return;
                    }
                }
                event.consume();
            }
        });
    }

    private void clearHover() {
        hoverTimer.stop();
        hoverVisible = false;
        hoverBounds = null;
        hoverPoint = null;
        hoverText = "";
        if (hoveredSystem != null) {
            hoveredSystem = null;
            navigationActions.hover(null);
            requestRender();
        }
    }

    private void dismissPopup() {
        if (navigationPopup != null) {
            navigationPopup.setVisible(false);
            navigationPopup = null;
        }
    }

    private Point selectedAnchor() {
        return state.selectedSystem() == null ? new Point(getWidth() / 2, getHeight() / 2)
              : new Point(Math.round(screenX(state.selectedSystem())), Math.round(screenY(state.selectedSystem())));
    }

    private JMenuItem navigationItem(String key, boolean enabled, Runnable action) {
        JMenuItem item = new JMenuItem(MHQInternationalization.getTextAt("mekhq.resources.CampaignGUI", key));
        item.setEnabled(enabled);
        item.addActionListener(event -> action.run());
        return item;
    }

    private void showNavigationPopup(Point anchor) {
        clearHover();
        dismissPopup();
        PlanetarySystem target = findSystemAt(anchor);
        JPopupMenu popup = navigationActions.createMenu(target);
        popup.setLightWeightPopupEnabled(false);
        popup.addSeparator();
        popup.add(navigationItem("map.context.zoomIn.text", true, () -> zoom(1.5, anchor)));
        popup.add(navigationItem("map.context.zoomOut.text", true, () -> zoom(0.5, anchor)));
        JMenu center = new JMenu(MHQInternationalization.getTextAt("mekhq.resources.CampaignGUI", "map.context.center.text"));
        PlanetarySystem selected = state.selectedSystem();
        PlanetarySystem current = campaign.getCurrentSystem();
        center.add(navigationItem("map.context.center.selected.text", selected != null,
              () -> setViewState(new ViewState(selected.getX(), selected.getY(), state.scale(), selected))));
        center.add(navigationItem("map.context.center.current.text", current != null, () -> {
            selectionHandler.accept(current);
            setViewState(new ViewState(current.getX(), current.getY(), state.scale(), current));
        }));
        center.add(navigationItem("map.context.center.terra.text", true,
              () -> setViewState(new ViewState(0, 0, state.scale(), state.selectedSystem()))));
        popup.add(center);
        for (java.awt.Component component : popup.getComponents()) {
            if (component instanceof JMenuItem item) {
                item.addActionListener(event -> SwingUtilities.invokeLater(() -> {
                    if (layer != null) {
                        refresh();
                        layer.requestFocusInWindow();
                    }
                }));
            }
        }
        InterstellarMapPanel.styleNavigationPopup(popup);
        navigationPopup = popup;
        popup.show(this, Math.clamp(anchor.x, 0, Math.max(0, getWidth() - 1)),
              Math.clamp(anchor.y, 0, Math.max(0, getHeight() - 1)));
    }

    private void pan(double horizontal, double vertical) {
        setViewState(new ViewState(state.centerX() + horizontal, state.centerY() + vertical,
              state.scale(), state.selectedSystem()));
    }

    private void zoom(double factor, Point anchor) {
        double nextScale = InterstellarMapPanel.boundedMapScale(state.scale() * factor);
        double offsetX = anchor.x - getWidth() / 2.0;
        double offsetY = getHeight() / 2.0 - anchor.y;
        setViewState(new ViewState(state.centerX() + offsetX / state.scale() - offsetX / nextScale,
              state.centerY() + offsetY / state.scale() - offsetY / nextScale, nextScale, state.selectedSystem()));
    }

    private PlanetarySystem findSystemAt(Point point) {
        updatePresentation();
        PlanetarySystem nearest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        for (SystemPresentation data : presentation.systems()) {
            if (!isVisible(data)) {
                continue;
            }
            PlanetarySystem system = data.system();
            double distance = point.distanceSq(screenX(system), screenY(system));
            double hitRadius = Math.max(UIUtil.scaleForGUI(9), systemArtRadius(system));
            if (distance <= closestDistance && distance <= hitRadius * hitRadius) {
                nearest = system;
                closestDistance = distance;
            }
        }
        return nearest;
    }

    private void selectAt(Point point, int modifiers, int clickCount) {
        PlanetarySystem nearest = findSystemAt(point);
        if (navigationActions.click(nearest, modifiers, clickCount)) {
            requestRender();
            return;
        }
        if (nearest != null) {
            setViewState(new ViewState(state.centerX(), state.centerY(), state.scale(), nearest));
            selectionHandler.accept(nearest);
            requestRender();
        }
    }

    private void fail(Throwable exception) {
        if (!failed) {
            failed = true;
            failureHandler.accept(exception);
        }
    }

    private static void requireEdt() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("SkiaMap must run on the EDT");
        }
    }

    public static final class Factory implements ExperimentalMapView.Factory {
        @Override
                public ExperimentalMapView create(Campaign campaign, Supplier<Presentation> presentation,
                            Consumer<PlanetarySystem> selectionHandler,
                  NavigationActions navigationActions,
              Consumer<Throwable> failureHandler) {
                    return new SkiaMap(campaign, presentation, selectionHandler, navigationActions, failureHandler);
        }
    }
}
