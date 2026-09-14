package mekhq.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
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
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQInternationalization;
import org.jetbrains.skia.Canvas;
import org.jetbrains.skia.BlendMode;
import org.jetbrains.skia.ColorFilter;
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
    private static final int CURRENT_COLOR = 0xFF66E1ED;
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
    private final Consumer<Throwable> failureHandler;
    private final InterstellarMapPanel.MapPointerGesture gesture = new InterstellarMapPanel.MapPointerGesture();
    private Presentation presentation;
    private final Map<String, Rect> textMeasurements = new HashMap<>();
    private List<LabelPlacement> labels = List.of();
    private List<String> visibleSystemIds = List.of();
    private ViewState state;
    private SkiaLayer layer;
    private Paint starPaint;
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

    private SkiaMap(Campaign campaign, Supplier<Presentation> presentationSupplier,
          Consumer<PlanetarySystem> selectionHandler, Consumer<Throwable> failureHandler) {
        super(new BorderLayout());
        this.campaign = campaign;
        this.presentationSupplier = presentationSupplier;
        this.selectionHandler = selectionHandler;
        this.failureHandler = failureHandler;
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

    public List<LabelPlacement> getLabelPlacements() {
        return labels;
    }

    public List<String> getVisibleSystemIds() {
        return visibleSystemIds;
    }

    public Presentation getPresentation() {
        return presentation;
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
        this.state = new ViewState(state.centerX(), state.centerY(),
              InterstellarMapPanel.boundedMapScale(state.scale()), state.selectedSystem());
        requestRender();
    }

    @Override
    public void refresh() {
        requireEdt();
        updatePresentation();
        requestRender();
    }

    private void updatePresentation() {
        Presentation next = presentationSupplier.get();
        if ((presentation == null) || (presentation.systems() != next.systems())) {
            textMeasurements.clear();
        }
        presentation = next;
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
            markerPaint = new Paint();
            markerPaint.setAntiAlias(true);
            markerPaint.setMode(PaintMode.STROKE);
            markerPaint.setStrokeWidth(UIUtil.scaleForGUI(1));
            routeDashes = PathEffect.Companion.makeDash(new float[] {
                UIUtil.scaleForGUI(8), UIUtil.scaleForGUI(6) }, 0);
            plannedRoutePaint = createRoutePaint(PLANNED_ROUTE_COLOR, 2);
            plannedRoutePaint.setPathEffect(routeDashes);
            activeRoutePaint = createRoutePaint(ACTIVE_ROUTE_COLOR, 3);
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
        } catch (RuntimeException | LinkageError exception) {
            fail(exception);
        }
    }

    @Override
    public void removeNotify() {
        requireEdt();
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
            activeLegs = drawRouteLines(canvas, presentation.routes().active(), activeRoutePaint);
            plannedLegs = drawRouteLines(canvas, presentation.routes().planned(), plannedRoutePaint);
            float radius = UIUtil.scaleForGUI(2);
            int visibleStars = 0;
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
                if ((horizontal < -radius) || (horizontal > getWidth() + radius)
                      || (vertical < -radius) || (vertical > getHeight() + radius)) {
                    continue;
                }
                    drawStar(canvas, data, horizontal, vertical, radius);
                    visible.add(data);
                    renderedIds.add(system.getId());
                    float markerRadius = markerRadius(system);
                    markerBounds.add(Rect.makeLTRB(horizontal - markerRadius, vertical - markerRadius,
                        horizontal + markerRadius, vertical + markerRadius));
                visibleStars++;
            }
                visibleSystemIds = List.copyOf(renderedIds);
            PlanetarySystem selected = state.selectedSystem();
            if (selected != null) {
                markerPaint.setColor(SELECTED_COLOR);
                canvas.drawCircle(screenX(selected), screenY(selected), UIUtil.scaleForGUI(7), markerPaint);
            }
            PlanetarySystem current = campaign.getCurrentSystem();
            if ((current != null) && !Objects.equals(current, selected)) {
                markerPaint.setColor(CURRENT_COLOR);
                canvas.drawCircle(screenX(current), screenY(current), UIUtil.scaleForGUI(4), markerPaint);
            }
            drawRouteMarkers(canvas, presentation.routes().active(), ACTIVE_ROUTE_COLOR, 5);
            drawRouteMarkers(canvas, presentation.routes().planned(), PLANNED_ROUTE_COLOR, 8);
            drawTransitMarker(canvas);
            drawLabels(canvas, visible, markerBounds);
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

    private int drawRouteLines(Canvas canvas, List<PlanetarySystem> route, Paint paint) {
        for (int index = 1; index < route.size(); index++) {
            PlanetarySystem origin = route.get(index - 1);
            PlanetarySystem destination = route.get(index);
            canvas.drawLine(screenX(origin), screenY(origin), screenX(destination), screenY(destination), paint);
        }
        return Math.max(0, route.size() - 1);
    }

    private void drawRouteMarkers(Canvas canvas, List<PlanetarySystem> route, int color, int size) {
        markerPaint.setColor(color);
        float radius = UIUtil.scaleForGUI(size);
        for (int index = 0; index < route.size(); index++) {
            PlanetarySystem system = route.get(index);
            float horizontal = screenX(system);
            float vertical = screenY(system);
            canvas.drawCircle(horizontal, vertical, radius, markerPaint);
            if (index == route.size() - 1) {
                canvas.drawCircle(horizontal, vertical, radius + UIUtil.scaleForGUI(2), markerPaint);
            }
            if (index > 0) {
                PlanetarySystem previous = route.get(index - 1);
                float deltaX = horizontal - screenX(previous);
                float deltaY = vertical - screenY(previous);
                double length = Math.hypot(deltaX, deltaY);
                if (length > UIUtil.scaleForGUI(30)) {
                    float directionX = (float) (deltaX / length);
                    float directionY = (float) (deltaY / length);
                    float centerX = (horizontal + screenX(previous)) / 2;
                    float centerY = (vertical + screenY(previous)) / 2;
                    float arrow = UIUtil.scaleForGUI(5);
                    canvas.drawLine(centerX, centerY,
                          centerX - directionX * arrow - directionY * arrow,
                          centerY - directionY * arrow + directionX * arrow, markerPaint);
                    canvas.drawLine(centerX, centerY,
                          centerX - directionX * arrow + directionY * arrow,
                          centerY - directionY * arrow - directionX * arrow, markerPaint);
                }
            }
        }
    }

    private void drawTransitMarker(Canvas canvas) {
        Routes routes = presentation.routes();
        transitMarkerDrawn = routes.inTransit() && (routes.currentSystem() != null);
        if (!transitMarkerDrawn) {
            return;
        }
        float horizontal = screenX(routes.currentSystem());
        float vertical = screenY(routes.currentSystem());
        float radius = UIUtil.scaleForGUI(13);
        markerPaint.setColor(0xFF45606C);
        canvas.drawCircle(horizontal, vertical, radius, markerPaint);
        float sweep = (float) (routes.planetProximity() * 360);
        canvas.drawArc(horizontal - radius, vertical - radius, horizontal + radius, vertical + radius,
              -90, sweep, false, activeRoutePaint);
        double angle = Math.toRadians(sweep - 90);
        starPaint.setColor(CURRENT_COLOR);
        canvas.drawCircle(horizontal + radius * (float) Math.cos(angle),
              vertical + radius * (float) Math.sin(angle), UIUtil.scaleForGUI(3), starPaint);
    }

    private float markerRadius(PlanetarySystem system) {
        if (presentation.routes().inTransit() && Objects.equals(system, presentation.routes().currentSystem())) {
            return UIUtil.scaleForGUI(17);
        }
        if (presentation.routeSystemIds().contains(system.getId())) {
            return UIUtil.scaleForGUI(11);
        }
        return UIUtil.scaleForGUI(Objects.equals(system, state.selectedSystem()) ? 8 : 4);
    }

    private boolean isVisible(SystemPresentation data) {
        return InterstellarMapPanel.shouldRenderSystem(true, data.empty(), presentation.showEmptySystems(),
              Objects.equals(data.system(), state.selectedSystem())
                    || Objects.equals(data.system(), campaign.getCurrentSystem())
                    || presentation.routeSystemIds().contains(data.system().getId()));
    }

    private void drawStar(Canvas canvas, SystemPresentation data, float horizontal, float vertical, float radius) {
        List<Integer> colors = data.factionColors();
        if (colors.size() <= 1) {
            starPaint.setColor(colors.isEmpty() ? STAR_COLOR : colors.getFirst());
            canvas.drawCircle(horizontal, vertical, radius, starPaint);
        } else {
            float sweep = 360.0f / colors.size();
            for (int index = 0; index < colors.size(); index++) {
                starPaint.setColor(colors.get(index));
                canvas.drawArc(horizontal - radius, vertical - radius, horizontal + radius, vertical + radius,
                      index * sweep, sweep, true, starPaint);
            }
        }
    }

    private void drawLabels(Canvas canvas, List<SystemPresentation> visible, List<Rect> markers) {
        List<LabelPlacement> placed = new ArrayList<>();
        List<Rect> priorityMarkers = new ArrayList<>();
        for (SystemPresentation data : visible) {
            if (Objects.equals(data.system(), state.selectedSystem())
                  || Objects.equals(data.system(), campaign.getCurrentSystem())) {
                float radius = markerRadius(data.system());
                priorityMarkers.add(Rect.makeLTRB(screenX(data.system()) - radius, screenY(data.system()) - radius,
                      screenX(data.system()) + radius, screenY(data.system()) + radius));
            }
        }
        InterstellarMapPanel.SemanticZoomProfile zoom = InterstellarMapPanel.SemanticZoomProfile.create(
              state.scale(), presentation.labelZoomReference());
        for (int priority = 0; priority < 4; priority++) {
            for (SystemPresentation data : visible) {
                int systemPriority = Objects.equals(data.system(), state.selectedSystem()) ? 0
                      : Objects.equals(data.system(), campaign.getCurrentSystem()) ? 1
                      : presentation.routeSystemIds().contains(data.system().getId()) ? 2 : 3;
                if (systemPriority != priority) {
                    continue;
                }
                float alpha = (float) (priority < 2 ? 1.0 : priority == 2
                      ? zoom.routeLabelAlpha() : zoom.ordinaryLabelAlpha());
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
            private Point position(MouseEvent event) {
                return SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), SkiaMap.this);
            }

            @Override
            public void mousePressed(MouseEvent event) {
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
                if (gesture.release(position(event)) && SwingUtilities.isLeftMouseButton(event)) {
                    selectAt(position(event));
                }
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

    private void selectAt(Point point) {
        updatePresentation();
        PlanetarySystem nearest = null;
        double closestDistance = Math.pow(UIUtil.scaleForGUI(9), 2);
        for (SystemPresentation data : presentation.systems()) {
            if (!isVisible(data)) {
                continue;
            }
            PlanetarySystem system = data.system();
            double distance = point.distanceSq(screenX(system), screenY(system));
            if (distance <= closestDistance) {
                nearest = system;
                closestDistance = distance;
            }
        }
        if (nearest != null) {
            state = new ViewState(state.centerX(), state.centerY(), state.scale(), nearest);
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
              Consumer<Throwable> failureHandler) {
                        return new SkiaMap(campaign, presentation, selectionHandler, failureHandler);
        }
    }
}
