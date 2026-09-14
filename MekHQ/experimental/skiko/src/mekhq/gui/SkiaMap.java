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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.jetbrains.skia.Font;
import org.jetbrains.skia.FontMgr;
import org.jetbrains.skia.FontStyle;
import org.jetbrains.skia.Paint;
import org.jetbrains.skia.PaintMode;
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
    private static long completedFrames;
    private static long completedStars;
    private static int activeSurfaces;

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
            if (starPaint != null) {
                starPaint.close();
                starPaint = null;
            }
            if (markerPaint != null) {
                markerPaint.close();
                markerPaint = null;
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
                    float markerRadius = Objects.equals(system, state.selectedSystem())
                        ? UIUtil.scaleForGUI(8) : UIUtil.scaleForGUI(4);
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
                float radius = UIUtil.scaleForGUI(8);
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
        float gap = UIUtil.scaleForGUI(10);
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
