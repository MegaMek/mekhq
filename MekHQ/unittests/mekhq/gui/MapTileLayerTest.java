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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MapTileLayerTest {
    private static final double PIXEL_SCALE = 1.75;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;

    @Test
    void tiledLayerMatchesDirectRenderingAcrossTileSeams() {
        double scale = 2.0;
        MapTileLayer.Camera camera = camera(37.3, -12.6, scale);
        MapTileLayer layer = new MapTileLayer(false, 8, () -> { });

        BufferedImage tiled = target();
        Graphics2D tiledGraphics = deviceTarget(tiled);
        assertTrue(layer.paint(tiledGraphics, "content", (levelScale, pixelScale) -> MapTileLayerTest::drawScene,
              camera, camera, false, null));
        tiledGraphics.dispose();

        BufferedImage direct = target();
        Graphics2D directGraphics = deviceTarget(direct);
        directGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawScene(directGraphics, new MapTileLayer.TileContext(InterstellarMapPanel.RenderViewKey.create(WIDTH,
              HEIGHT, snappedCenter(37.3, scale, WIDTH), snappedCenter(-12.6, scale, HEIGHT), scale, PIXEL_SCALE),
              8, 0, 0));
        directGraphics.dispose();

        assertTrue(layer.tileCount() > 4, "the scene must span several tiles to exercise seams");
        assertImagesClose(direct, tiled);
    }

    @Test
    void zoomedViewDrawsScaledFallbackAndRequestsExactTilesAsynchronously() throws Exception {
        ConcurrentLinkedQueue<Runnable> completions = new ConcurrentLinkedQueue<>();
        AtomicInteger repaints = new AtomicInteger();
        MapTileLayer layer = new MapTileLayer(true, 8, repaints::incrementAndGet, completions::add);
        AtomicInteger levelsCreated = new AtomicInteger();
        MapTileLayer.LevelSource source = (levelScale, pixelScale) -> {
            levelsCreated.incrementAndGet();
            return (graphics, context) -> {
                graphics.setColor(Color.ORANGE);
                graphics.fillRect(-10, -10, 1000, 1000);
                return true;
            };
        };
        MapTileLayer.Camera initial = camera(0.0, 0.0, 2.0);
        layer.paint(deviceTarget(target()), "content", source, initial, initial, false, Color.BLACK);
        int initialTiles = layer.tileCount();

        MapTileLayer.Camera zoomed = camera(0.0, 0.0, 2.35);
        BufferedImage fallbackFrame = target();
        boolean exact = layer.paint(deviceTarget(fallbackFrame), "content", source, zoomed, zoomed, true,
              Color.BLACK);

        assertFalse(exact, "the new zoom level must not block on rendering");
        assertEquals(Color.ORANGE.getRGB(), fallbackFrame.getRGB(fallbackFrame.getWidth() / 2,
              fallbackFrame.getHeight() / 2), "the cached level is drawn scaled in place of missing tiles");
        assertEquals(initialTiles, layer.tileCount());

        long deadline = System.nanoTime() + 10_000_000_000L;
        int expected = MapTileLayer.visibleKeys(MapTileLayer.Level.of("content", 2.35, PIXEL_SCALE), zoomed, 1).size();
        while ((completions.size() < expected) && (System.nanoTime() < deadline)) {
            Thread.sleep(5);
        }
        completions.forEach(Runnable::run);
        assertTrue(repaints.get() > 0);
        assertTrue(layer.paint(deviceTarget(target()), "content", source, zoomed, zoomed, false, Color.BLACK));
        assertTrue(layer.isComplete(MapTileLayer.Level.of("content", 2.35, PIXEL_SCALE)));
        assertEquals(2, levelsCreated.get());
    }

    @Test
    void preparingSeveralContentsOfOneLayerCompletesEachOfThem() throws Exception {
        ConcurrentLinkedQueue<Runnable> completions = new ConcurrentLinkedQueue<>();
        MapTileLayer layer = new MapTileLayer(true, 8, () -> { }, completions::add);
        MapTileLayer.Camera camera = camera(0.0, 0.0, 2.0);
        int perContent = MapTileLayer.visibleKeys(MapTileLayer.Level.of("with", 2.0, PIXEL_SCALE), camera, 1).size();

        assertFalse(layer.prepare("with", solid(Color.RED), camera));
        assertFalse(layer.prepare("without", solid(Color.BLUE), camera));
        long deadline = System.nanoTime() + 10_000_000_000L;
        while ((completions.size() < 2 * perContent) && (System.nanoTime() < deadline)) {
            Thread.sleep(5);
        }
        completions.forEach(Runnable::run);

        assertTrue(layer.prepare("with", solid(Color.RED), camera));
        assertTrue(layer.prepare("without", solid(Color.BLUE), camera));
        assertTrue(layer.hasContent("with") && layer.hasContent("without"));
    }

    @Test
    void changedContentRendersVisibleTilesInsteadOfShowingStaleFallback() {
        MapTileLayer layer = new MapTileLayer(true, 8, () -> { }, Runnable::run);
        MapTileLayer.Camera camera = camera(0.0, 0.0, 2.0);
        layer.paint(deviceTarget(target()), "old", solid(Color.RED), camera, camera, false, Color.BLACK);

        BufferedImage frame = target();
        assertTrue(layer.paint(deviceTarget(frame), "new", solid(Color.GREEN), camera, camera, false, Color.BLACK));
        assertEquals(Color.GREEN.getRGB(), frame.getRGB(10, 10));
    }

    @Test
    void cameraOffsetsAreWholePhysicalPixelsAfterSnapping() {
        for (double scale : new double[] { 0.37, 1.0, 2.0, 5.3 }) {
            MapTileLayer.Camera camera = camera(123.456, -78.9, scale);
            assertEquals(Math.rint(camera.offsetX()), camera.offsetX(), 0.000_001);
            assertEquals(Math.rint(camera.offsetY()), camera.offsetY(), 0.000_001);
        }
    }

    @Test
    void tileContextPlacesTileOriginAtGlobalPhysicalPosition() {
        MapTileLayer.TileKey key = new MapTileLayer.TileKey(MapTileLayer.Level.of("content", 2.0, PIXEL_SCALE), 3,
              -2);
        MapTileLayer.TileContext context = MapTileLayer.tileContext(key, 4);

        double mapX = (3 * MapTileLayer.TILE_SIZE) / (PIXEL_SCALE * 2.0);
        double mapY = (2 * MapTileLayer.TILE_SIZE) / (PIXEL_SCALE * 2.0);
        assertEquals(0.0, context.view().screenX(mapX), 0.000_001);
        assertEquals(0.0, context.view().screenY(mapY), 0.000_001);
        assertEquals(3 * MapTileLayer.TILE_SIZE, context.deviceOriginX());
        assertEquals(-2 * MapTileLayer.TILE_SIZE, context.deviceOriginY());
        assertEquals(4, context.margin());
    }

    @Test
    void zoomAnimationEasesGeometricallyToTheTarget() {
        assertEquals(2.0, InterstellarMapPanel.zoomAnimationFrameScale(2.0, 4.0, 0.0));
        assertEquals(4.0, InterstellarMapPanel.zoomAnimationFrameScale(2.0, 4.0, 1.0));
        double previous = 2.0;
        for (int step = 1; step <= 10; step++) {
            double scale = InterstellarMapPanel.zoomAnimationFrameScale(2.0, 4.0, step / 10.0);
            assertTrue(scale >= previous);
            previous = scale;
        }
        double halfway = InterstellarMapPanel.zoomAnimationFrameScale(2.0, 4.0, 0.5);
        assertTrue(halfway > Math.sqrt(8.0), "ease-out covers most of the zoom early");
        assertEquals(1.0, InterstellarMapPanel.zoomAnimationFrameScale(4.0, 1.0, 1.0));
    }

    @Test
    void disputedTerritoryStripesFollowTheMapOrigin() {
        Faction first = Mockito.mock(Faction.class);
        Faction second = Mockito.mock(Faction.class);
        Mockito.when(first.getColor()).thenReturn(Color.RED);
        Mockito.when(second.getColor()).thenReturn(Color.BLUE);
        LinearGradientPaint anchored = (LinearGradientPaint) InterstellarMapPanel.createDisputedTerritoryPaint(
              List.of(first, second), 1.0, 12.0, 40.0, -25.0);

        assertEquals(40.0, anchored.getStartPoint().getX());
        assertEquals(-25.0, anchored.getStartPoint().getY());
        assertNotEquals(anchored.getStartPoint(), anchored.getEndPoint());
    }

    @Test
    void cameraSnapIncludesTheViewHalfExtent() {
        double unit = 2.0 * PIXEL_SCALE;
        double halfExtent = PIXEL_SCALE * 301 / 2.0;
        double snapped = InterstellarMapPanel.snapToDevicePixel(10.123, unit, halfExtent);
        double offset = (snapped * unit) + halfExtent;

        assertEquals(Math.rint(offset), offset, 0.000_001);
        assertEquals(snapped, InterstellarMapPanel.snapToDevicePixel(snapped, unit, halfExtent));
    }

    private static MapTileLayer.LevelSource solid(Color color) {
        return (levelScale, pixelScale) -> (graphics, context) -> {
            graphics.setColor(color);
            graphics.fillRect(-10, -10, 1000, 1000);
            return true;
        };
    }

    private static boolean drawScene(Graphics2D graphics, MapTileLayer.TileContext context) {
        InterstellarMapPanel.RenderViewKey view = context.view();
        graphics.setColor(new Color(60, 180, 160, 200));
        graphics.setStroke(new BasicStroke(2.3f));
        graphics.draw(new Line2D.Double(view.screenX(-80.0), view.screenY(-40.0), view.screenX(70.0),
              view.screenY(55.0)));
        double radius = 25.0 * view.scale();
        graphics.setColor(new Color(255, 200, 140, 220));
        graphics.fill(new Ellipse2D.Double(view.screenX(30.0) - radius, view.screenY(-10.0) - radius, radius * 2,
              radius * 2));
        return true;
    }

    private static MapTileLayer.Camera camera(double centerX, double centerY, double scale) {
        return MapTileLayer.Camera.of(WIDTH, HEIGHT, snappedCenter(centerX, scale, WIDTH),
              snappedCenter(centerY, scale, HEIGHT), scale, PIXEL_SCALE);
    }

    private static double snappedCenter(double center, double scale, int extent) {
        return InterstellarMapPanel.snapToDevicePixel(center, scale * PIXEL_SCALE, PIXEL_SCALE * extent / 2.0);
    }

    private static BufferedImage target() {
        return new BufferedImage(InterstellarMapPanel.deviceExtent(WIDTH, PIXEL_SCALE),
              InterstellarMapPanel.deviceExtent(HEIGHT, PIXEL_SCALE), BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private static Graphics2D deviceTarget(BufferedImage image) {
        Graphics2D graphics = image.createGraphics();
        graphics.scale(PIXEL_SCALE, PIXEL_SCALE);
        return graphics;
    }

    /**
     * Java2D subdivides curves differently when a tile clips a shape, so antialiased curve edges may differ by a few
     * coverage levels; a misplaced tile or seam would instead produce near-full-intensity edge errors.
     */
    private static void assertImagesClose(BufferedImage expected, BufferedImage actual) {
        int worst = 0;
        int worstX = 0;
        int worstY = 0;
        int differing = 0;
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                int expectedPixel = expected.getRGB(x, y);
                int actualPixel = actual.getRGB(x, y);
                int pixelWorst = 0;
                for (int shift = 0; shift <= 24; shift += 8) {
                    pixelWorst = Math.max(pixelWorst,
                          Math.abs(premultiplied(expectedPixel, shift) - premultiplied(actualPixel, shift)));
                }
                if (pixelWorst > 4) {
                    differing++;
                }
                if (pixelWorst > worst) {
                    worst = pixelWorst;
                    worstX = x;
                    worstY = y;
                }
            }
        }
        assertTrue((worst <= 24) && (differing <= 200), String.format(
              "maximum channel difference %d at (%d, %d), %d differing pixels: expected %08x, actual %08x", worst,
              worstX, worstY, differing, expected.getRGB(worstX, worstY), actual.getRGB(worstX, worstY)));
    }

    /** Colour channels weighted by alpha, so nearly transparent edge pixels do not dominate the comparison. */
    private static int premultiplied(int argb, int shift) {
        int channel = (argb >>> shift) & 255;
        return shift == 24 ? channel : (channel * (argb >>> 24)) / 255;
    }
}
