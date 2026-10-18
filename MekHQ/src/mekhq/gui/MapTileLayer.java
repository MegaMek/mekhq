/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;

/**
 * A world-anchored raster tile cache for one interstellar map layer. Tiles are rendered by pure renderers on a shared
 * worker pool, so camera changes never wait for a full-layer redraw; until exact tiles arrive, the closest cached zoom
 * level is drawn scaled in their place. All methods except tile rendering must run on the event dispatch thread.
 */
final class MapTileLayer {
    private static final MMLogger LOGGER = MMLogger.create(MapTileLayer.class);
    static final int TILE_SIZE = 256;
    /** Zoom levels' worth of visible tiles, including the prefetch ring, that a layer keeps images for. */
    private static final int TILE_BUDGET_LEVELS = 2;
    private static final int MIN_TILE_BUDGET = 64;
    /** Used until the first request reveals the viewport size. */
    private static final int DEFAULT_TILE_BUDGET = 320;
    /** Empty tiles are cheap; they are only bounded so the cache cannot grow without limit. */
    private static final int TILE_ENTRIES_PER_IMAGE = 4;
    private static final int PREFETCH_RING = 1;
    private static final int MAX_REQUESTED_CONTENTS = 8;
    private static final int MAX_POOLED_IMAGES = 64;
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final ThreadPoolExecutor RENDERERS = createRenderers();

    /**
     * Inputs for rendering one tile. The view's logical origin is the tile's top-left corner, which lies at the given
     * global physical-pixel origin.
     */
    record TileContext(InterstellarMapPanel.RenderViewKey view, int margin, int deviceOriginX, int deviceOriginY) {
    }

    /** Draws one tile from immutable data. Runs on worker threads. Returns false when nothing was drawn. */
    @FunctionalInterface
    interface TileRenderer {
        boolean render(Graphics2D graphics, TileContext context);
    }

    /** Captures an immutable renderer for one zoom level. Runs on the event dispatch thread. */
    @FunctionalInterface
    interface LevelSource {
        TileRenderer create(double scale, double pixelScale);
    }

    /** A view in physical pixels: a map point at global device position {@code g} appears at {@code g + offset}. */
    record Camera(double scale, double pixelScale, double offsetX, double offsetY, int deviceWidth,
          int deviceHeight) {
        static Camera of(int width, int height, double centerX, double centerY, double scale, double pixelScale) {
            return new Camera(scale, pixelScale, pixelScale * ((width / 2.0) + (centerX * scale)),
                  pixelScale * ((height / 2.0) + (centerY * scale)),
                  InterstellarMapPanel.deviceExtent(width, pixelScale),
                  InterstellarMapPanel.deviceExtent(height, pixelScale));
        }

        double unit() {
            return scale * pixelScale;
        }
    }

    record Level(Object content, long scaleBits, long pixelScaleBits) {
        static Level of(Object content, double scale, double pixelScale) {
            return new Level(content, Double.doubleToLongBits(scale), Double.doubleToLongBits(pixelScale));
        }

        double scale() {
            return Double.longBitsToDouble(scaleBits);
        }

        double pixelScale() {
            return Double.longBitsToDouble(pixelScaleBits);
        }

        double unit() {
            return scale() * pixelScale();
        }
    }

    record TileKey(Level level, int column, int row) {
    }

    /** Which tiles {@link #paint} requests for the target camera. */
    enum RequestMode {
        /** Request nothing. */
        NONE,
        /** Request the target's visible tiles and a prefetch ring around them. */
        WITH_PREFETCH,
        /**
         * Request only the target's visible tiles, for use while the zoom is still changing: a prefetch ring for a zoom
         * level the camera passes straight through would be rendered and discarded.
         */
        VISIBLE_ONLY
    }

    private record Tile(@Nullable BufferedImage image) {
    }

    private record Request(Level level, Set<TileKey> visible, Set<TileKey> wanted) {
    }

    private final boolean opaque;
    private final int margin;
    private final Runnable repaint;
    private final Consumer<Runnable> eventLoop;
    private final LinkedHashMap<TileKey, Tile> tiles = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Level, Integer> levelTileCounts = new HashMap<>();
    private final Set<TileKey> inFlight = new HashSet<>();
    private final Map<Level, TileRenderer> renderers = new LinkedHashMap<>();
    private final Set<Level> completeLevels = new HashSet<>();
    /** Latest request per content; read by workers to skip tiles the camera has left. */
    private final Map<Object, Request> requests = Collections.synchronizedMap(
          new LinkedHashMap<>(8, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<Object, Request> eldest) {
                  return size() > MAX_REQUESTED_CONTENTS;
              }
          });
    /** Evicted tile images, reused by workers instead of allocating a new raster per tile. */
    private final ArrayDeque<BufferedImage> imagePool = new ArrayDeque<>();
    private long generation;
    private int renderedTileCount;
    private int imageTileCount;
    private int tileBudget = DEFAULT_TILE_BUDGET;

    MapTileLayer(boolean opaque, int margin, Runnable repaint) {
        this(opaque, margin, repaint, SwingUtilities::invokeLater);
    }

    MapTileLayer(boolean opaque, int margin, Runnable repaint, Consumer<Runnable> eventLoop) {
        this.opaque = opaque;
        this.margin = margin;
        this.repaint = repaint;
        this.eventLoop = eventLoop;
    }

    /**
     * Paints the layer for {@code view}. Tiles are requested for {@code target} (the camera destination during an
     * animation) as selected by {@code requestMode}. When no cached level of the same content exists, visible tiles
     * are rendered in parallel before returning, because there is nothing to show in their place.
     *
     * @return {@code true} when every visible tile was drawn at the exact view scale
     */
    boolean paint(Graphics2D graphics, Object content, LevelSource source, Camera view, Camera target,
          RequestMode requestMode, @Nullable Color holeFill) {
        Level exactLevel = Level.of(content, view.scale(), view.pixelScale());
        tileBudget = tileBudget(view);
        Graphics2D deviceGraphics = (Graphics2D) graphics.create();
        try {
            InterstellarMapPanel.prepareLayerBlit(deviceGraphics, view.pixelScale());
            List<TileKey> missing = new ArrayList<>();
            int offsetX = (int) Math.rint(view.offsetX());
            int offsetY = (int) Math.rint(view.offsetY());
            for (TileKey key : visibleKeys(exactLevel, view, 0)) {
                Tile tile = tiles.get(key);
                if (tile == null) {
                    missing.add(key);
                }
            }
            Level fallback = missing.isEmpty() ? null : selectFallback(content, view.unit(), exactLevel);
            if (!missing.isEmpty() && (fallback == null) && !levelTileCounts.containsKey(exactLevel)) {
                renderSynchronously(missing, rendererFor(exactLevel, source));
                missing.clear();
            }
            if (!missing.isEmpty()) {
                List<Level> fallbackLevels = fallbackLevels(content, view.unit(), exactLevel);
                for (TileKey key : missing) {
                    Rectangle bounds = new Rectangle((key.column() * TILE_SIZE) + offsetX,
                          (key.row() * TILE_SIZE) + offsetY, TILE_SIZE, TILE_SIZE);
                    Graphics2D tileGraphics = (Graphics2D) deviceGraphics.create();
                    try {
                        tileGraphics.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);
                        if (holeFill != null) {
                            tileGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_OFF);
                            tileGraphics.setPaint(holeFill);
                            tileGraphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                        }
                        drawFallback(tileGraphics, fallbackLevels, view, bounds);
                    } finally {
                        tileGraphics.dispose();
                    }
                }
            } else {
                completeLevels.add(exactLevel);
            }
            for (TileKey key : visibleKeys(exactLevel, view, 0)) {
                Tile tile = tiles.get(key);
                if ((tile != null) && (tile.image() != null)) {
                    deviceGraphics.drawImage(tile.image(), (key.column() * TILE_SIZE) + offsetX,
                          (key.row() * TILE_SIZE) + offsetY, null);
                }
            }
            if (requestMode != RequestMode.NONE) {
                request(Level.of(content, target.scale(), target.pixelScale()), source, target,
                      requestMode == RequestMode.WITH_PREFETCH ? PREFETCH_RING : 0);
            }
            return missing.isEmpty();
        } finally {
            deviceGraphics.dispose();
        }
    }

    void clear() {
        generation++;
        for (Tile tile : tiles.values()) {
            recycle(tile.image());
        }
        tiles.clear();
        imageTileCount = 0;
        levelTileCounts.clear();
        completeLevels.clear();
        inFlight.clear();
        renderers.clear();
        requests.clear();
    }

    boolean isComplete(Level level) {
        return completeLevels.contains(level);
    }

    /** Requests the visible tiles of {@code content} at {@code camera} and reports whether all are already present. */
    boolean prepare(Object content, LevelSource source, Camera camera) {
        Level level = Level.of(content, camera.scale(), camera.pixelScale());
        request(level, source, camera, PREFETCH_RING);
        return tiles.keySet().containsAll(visibleKeys(level, camera, 0));
    }

    /** Whether any zoom level of {@code content} is cached, so painting it would not block. */
    boolean hasContent(Object content) {
        for (Level level : levelTileCounts.keySet()) {
            if (level.content().equals(content)) {
                return true;
            }
        }
        return false;
    }

    int tileCount() {
        return tiles.size();
    }

    int imageTileCount() {
        return imageTileCount;
    }

    int renderedTileCount() {
        return renderedTileCount;
    }

    static List<TileKey> visibleKeys(Level level, Camera camera, int ring) {
        int firstColumn = Math.floorDiv((int) Math.floor(-camera.offsetX()), TILE_SIZE) - ring;
        int lastColumn = Math.floorDiv((int) Math.ceil(camera.deviceWidth() - camera.offsetX()) - 1, TILE_SIZE)
              + ring;
        int firstRow = Math.floorDiv((int) Math.floor(-camera.offsetY()), TILE_SIZE) - ring;
        int lastRow = Math.floorDiv((int) Math.ceil(camera.deviceHeight() - camera.offsetY()) - 1, TILE_SIZE)
              + ring;
        List<TileKey> keys = new ArrayList<>(Math.max(0, (lastColumn - firstColumn + 1) * (lastRow - firstRow + 1)));
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                keys.add(new TileKey(level, column, row));
            }
        }
        return keys;
    }

    /** Builds the logical view whose origin is the tile's top-left corner in global device space. */
    static TileContext tileContext(TileKey key, int margin) {
        double scale = key.level().scale();
        double pixelScale = key.level().pixelScale();
        int extent = (int) Math.ceil(TILE_SIZE / pixelScale);
        double originX = key.column() * TILE_SIZE / pixelScale;
        double originY = key.row() * TILE_SIZE / pixelScale;
        double centerX = (-originX - (extent / 2.0)) / scale;
        double centerY = (-originY - (extent / 2.0)) / scale;
        return new TileContext(InterstellarMapPanel.RenderViewKey.create(extent, extent, centerX, centerY, scale,
              pixelScale), margin, key.column() * TILE_SIZE, key.row() * TILE_SIZE);
    }

    /**
     * Sizes the image cache to the viewport: the visible tiles and their prefetch ring for the current zoom level,
     * plus room for another level to stand in while the zoom changes.
     */
    static int tileBudget(Camera camera) {
        int columns = Math.ceilDiv(camera.deviceWidth(), TILE_SIZE) + 1 + (PREFETCH_RING * 2);
        int rows = Math.ceilDiv(camera.deviceHeight(), TILE_SIZE) + 1 + (PREFETCH_RING * 2);
        return Math.max(MIN_TILE_BUDGET, columns * rows * TILE_BUDGET_LEVELS);
    }

    private void request(Level level, LevelSource source, Camera camera, int prefetchRing) {
        tileBudget = tileBudget(camera);
        List<TileKey> visible = visibleKeys(level, camera, 0);
        List<TileKey> requested = prefetchRing == 0 ? visible : visibleKeys(level, camera, prefetchRing);
        Set<TileKey> visibleSet = new HashSet<>(visible);
        requests.put(level.content(), new Request(level, Set.copyOf(visibleSet), Set.copyOf(requested)));
        markCompleteWhenReady(level);
        double centerColumn = ((camera.deviceWidth() / 2.0) - camera.offsetX()) / TILE_SIZE;
        double centerRow = ((camera.deviceHeight() / 2.0) - camera.offsetY()) / TILE_SIZE;
        TileRenderer renderer = null;
        long requestGeneration = generation;
        for (TileKey key : requested) {
            if (tiles.containsKey(key) || !inFlight.add(key)) {
                continue;
            }
            if (renderer == null) {
                renderer = rendererFor(level, source);
            }
            double distance = Math.hypot(key.column() + 0.5 - centerColumn, key.row() + 0.5 - centerRow);
            TileRenderer tileRenderer = renderer;
            RENDERERS.execute(new TileJob(visibleSet.contains(key) ? distance : 1_000.0 + distance, () -> {
                Request current = requests.get(key.level().content());
                if ((requestGeneration != generation) || (current == null) || !current.wanted().contains(key)) {
                    eventLoop.accept(() -> inFlight.remove(key));
                    return;
                }
                BufferedImage image = renderTile(key, tileRenderer);
                eventLoop.accept(() -> {
                    inFlight.remove(key);
                    if (requestGeneration == generation) {
                        install(key, image);
                        repaint.run();
                    } else {
                        recycle(image);
                    }
                });
            }));
        }
    }

    private void renderSynchronously(List<TileKey> keys, TileRenderer renderer) {
        List<Future<BufferedImage>> results = new ArrayList<>(keys.size());
        for (TileKey key : keys) {
            Callable<BufferedImage> task = () -> renderTile(key, renderer);
            java.util.concurrent.FutureTask<BufferedImage> future = new java.util.concurrent.FutureTask<>(task);
            RENDERERS.execute(new TileJob(-1.0, future));
            results.add(future);
        }
        for (int index = 0; index < keys.size(); index++) {
            TileKey key = keys.get(index);
            try {
                install(key, results.get(index).get());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException exception) {
                LOGGER.error("Failed to render interstellar map tile", exception.getCause());
                install(key, null);
            }
        }
    }

    private @Nullable BufferedImage renderTile(TileKey key, TileRenderer renderer) {
        BufferedImage image = obtainImage();
        Graphics2D graphics = image.createGraphics();
        boolean drawn = false;
        try {
            if (!opaque) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
                graphics.setComposite(AlphaComposite.SrcOver);
            }
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.scale(key.level().pixelScale(), key.level().pixelScale());
            drawn = renderer.render(graphics, tileContext(key, margin)) || opaque;
        } catch (RuntimeException exception) {
            LOGGER.error("Failed to render interstellar map tile", exception);
        } finally {
            graphics.dispose();
        }
        if (drawn) {
            return image;
        }
        recycle(image);
        return null;
    }

    private BufferedImage obtainImage() {
        BufferedImage image;
        synchronized (imagePool) {
            image = imagePool.pollFirst();
        }
        return image != null ? image : new BufferedImage(TILE_SIZE, TILE_SIZE,
              opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB_PRE);
    }

    /** Returns an image that is no longer drawn to the pool. Called from the event dispatch thread and workers. */
    private void recycle(@Nullable BufferedImage image) {
        if (image != null) {
            synchronized (imagePool) {
                if (imagePool.size() < MAX_POOLED_IMAGES) {
                    imagePool.addFirst(image);
                }
            }
        }
    }

    private TileRenderer rendererFor(Level level, LevelSource source) {
        TileRenderer renderer = renderers.get(level);
        if (renderer == null) {
            renderer = source.create(level.scale(), level.pixelScale());
            renderers.put(level, renderer);
            if (renderers.size() > MAX_REQUESTED_CONTENTS) {
                Iterator<Level> iterator = renderers.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
        }
        return renderer;
    }

    private void install(TileKey key, @Nullable BufferedImage image) {
        Tile previous = tiles.put(key, new Tile(image));
        if (previous == null) {
            levelTileCounts.merge(key.level(), 1, Integer::sum);
        } else {
            recycle(previous.image());
            if (previous.image() != null) {
                imageTileCount--;
            }
        }
        if (image != null) {
            imageTileCount++;
        }
        renderedTileCount++;
        // Images are the only real cost, so only the image budget evicts images and the entry limit only evicts empty
        // tiles; that keeps image evictions in step with image installs, so every evicted image goes straight back to
        // a worker through the pool.
        int maxEntries = tileBudget * TILE_ENTRIES_PER_IMAGE;
        Iterator<Map.Entry<TileKey, Tile>> iterator = tiles.entrySet().iterator();
        while (((imageTileCount > tileBudget) || (tiles.size() > maxEntries)) && iterator.hasNext()) {
            Map.Entry<TileKey, Tile> eldest = iterator.next();
            BufferedImage evictedImage = eldest.getValue().image();
            if ((evictedImage == null) ? (tiles.size() <= maxEntries) : (imageTileCount <= tileBudget)) {
                continue;
            }
            iterator.remove();
            if (evictedImage != null) {
                imageTileCount--;
                recycle(evictedImage);
            }
            completeLevels.remove(eldest.getKey().level());
            levelTileCounts.computeIfPresent(eldest.getKey().level(),
                  (level, count) -> count > 1 ? count - 1 : null);
        }
        markCompleteWhenReady(key.level());
    }

    private void markCompleteWhenReady(Level level) {
        Request request = requests.get(level.content());
        if ((request != null) && request.level().equals(level) && !request.visible().isEmpty()
              && tiles.keySet().containsAll(request.visible())) {
            completeLevels.add(level);
        }
    }

    private @Nullable Level selectFallback(Object content, double unit, Level exactLevel) {
        List<Level> levels = fallbackLevels(content, unit, exactLevel);
        return levels.isEmpty() ? null : levels.getFirst();
    }

    /** Orders cached levels of the same content: complete levels first, each group nearest in zoom first. */
    private List<Level> fallbackLevels(Object content, double unit, Level exactLevel) {
        List<Level> levels = new ArrayList<>();
        for (Level level : levelTileCounts.keySet()) {
            if (!level.equals(exactLevel) && level.content().equals(content)) {
                levels.add(level);
            }
        }
        levels.sort((first, second) -> {
            int byCompleteness = Boolean.compare(!completeLevels.contains(first), !completeLevels.contains(second));
            return byCompleteness != 0 ? byCompleteness : Double.compare(Math.abs(Math.log(unit / first.unit())),
                  Math.abs(Math.log(unit / second.unit())));
        });
        return levels;
    }

    private void drawFallback(Graphics2D graphics, List<Level> levels, Camera view, Rectangle bounds) {
        Level partial = null;
        for (Level level : levels) {
            Coverage coverage = coverage(level, view, bounds);
            if (coverage.complete()) {
                drawScaledTiles(graphics, level, view, coverage);
                return;
            }
            if ((partial == null) && coverage.any()) {
                partial = level;
            }
        }
        if (partial != null) {
            drawScaledTiles(graphics, partial, view, coverage(partial, view, bounds));
        }
    }

    private record Coverage(int firstColumn, int lastColumn, int firstRow, int lastRow, boolean complete,
          boolean any) {
    }

    /** Finds the tiles of a cached level that lie under a screen rectangle and whether all of them are present. */
    private Coverage coverage(Level level, Camera view, Rectangle bounds) {
        double screenSize = TILE_SIZE * (view.unit() / level.unit());
        int firstColumn = (int) Math.floor((bounds.x - view.offsetX()) / screenSize);
        int lastColumn = (int) Math.floor((bounds.x + bounds.width - 1 - view.offsetX()) / screenSize);
        int firstRow = (int) Math.floor((bounds.y - view.offsetY()) / screenSize);
        int lastRow = (int) Math.floor((bounds.y + bounds.height - 1 - view.offsetY()) / screenSize);
        boolean complete = true;
        boolean any = false;
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                if (tiles.containsKey(new TileKey(level, column, row))) {
                    any = true;
                } else {
                    complete = false;
                }
            }
        }
        return new Coverage(firstColumn, lastColumn, firstRow, lastRow, complete, any);
    }

    /**
     * Draws stand-in tiles with Java2D's scaled-blit loop: interpolated affine transforms cost several times more per
     * pixel on the software pipeline, and these frames are replaced by exact tiles once rendering catches up.
     */
    private void drawScaledTiles(Graphics2D graphics, Level level, Camera view, Coverage coverage) {
        double screenSize = TILE_SIZE * (view.unit() / level.unit());
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
              RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (int row = coverage.firstRow(); row <= coverage.lastRow(); row++) {
            int top = (int) Math.round((row * screenSize) + view.offsetY());
            int bottom = (int) Math.round(((row + 1) * screenSize) + view.offsetY());
            for (int column = coverage.firstColumn(); column <= coverage.lastColumn(); column++) {
                Tile tile = tiles.get(new TileKey(level, column, row));
                if ((tile == null) || (tile.image() == null)) {
                    continue;
                }
                int left = (int) Math.round((column * screenSize) + view.offsetX());
                int right = (int) Math.round(((column + 1) * screenSize) + view.offsetX());
                graphics.drawImage(tile.image(), left, top, right, bottom, 0, 0, TILE_SIZE, TILE_SIZE, null);
            }
        }
    }

    private static ThreadPoolExecutor createRenderers() {
        int threads = Math.clamp(Runtime.getRuntime().availableProcessors() - 2, 2, 8);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 30, TimeUnit.SECONDS,
              new PriorityBlockingQueue<>(), runnable -> {
                  Thread thread = new Thread(runnable, "MekHQ map tile renderer");
                  thread.setDaemon(true);
                  thread.setPriority(Thread.NORM_PRIORITY - 1);
                  return thread;
              });
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static final class TileJob implements Runnable, Comparable<TileJob> {
        private final double priority;
        private final long sequence = SEQUENCE.incrementAndGet();
        private final Runnable work;

        private TileJob(double priority, Runnable work) {
            this.priority = priority;
            this.work = work;
        }

        @Override
        public void run() {
            work.run();
        }

        @Override
        public int compareTo(TileJob other) {
            int byPriority = Double.compare(priority, other.priority);
            return byPriority != 0 ? byPriority : Long.compare(sequence, other.sequence);
        }
    }
}
