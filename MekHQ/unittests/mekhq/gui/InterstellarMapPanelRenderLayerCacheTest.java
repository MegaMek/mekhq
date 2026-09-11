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
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.JViewport;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class InterstellarMapPanelRenderLayerCacheTest {
    @Test
    void hiddenEmptySystemsRenderOnlyWhenRequiredForNavigation() {
        assertFalse(InterstellarMapPanel.shouldRenderSystem(true, true, false, false));
        assertTrue(InterstellarMapPanel.shouldRenderSystem(true, true, true, false));
        assertTrue(InterstellarMapPanel.shouldRenderSystem(true, true, false, true));
        assertTrue(InterstellarMapPanel.shouldRenderSystem(true, false, false, false));
        assertFalse(InterstellarMapPanel.shouldRenderSystem(false, false, true, true));
    }

    @Test
    void optionalOverlaysDoNotResurrectHiddenEmptySystems() {
        assertFalse(InterstellarMapPanel.shouldRenderOptionalSystemOverlay(true, false, false, false));
        assertTrue(InterstellarMapPanel.shouldRenderOptionalSystemOverlay(true, true, false, false));
        assertTrue(InterstellarMapPanel.shouldRenderOptionalSystemOverlay(true, false, true, false));
        assertTrue(InterstellarMapPanel.shouldRenderOptionalSystemOverlay(true, false, false, true));
        assertTrue(InterstellarMapPanel.shouldRenderOptionalSystemOverlay(false, false, false, false));
    }

        @Test
        void retainedCartographyRequiresStableStaticMapState() {
          assertTrue(InterstellarMapPanel.canUseRetainedCartography(
              true, false));
          assertFalse(InterstellarMapPanel.canUseRetainedCartography(
              false, false));
          assertFalse(InterstellarMapPanel.canUseRetainedCartography(
              true, true));
        }

    @Test
    void routesAndReachabilityDoNotInvalidateStaticCartography() {
        assertTrue(InterstellarMapPanel.canUseRetainedCartography(true, false));
    }

        @Test
        void retainedMapModeTransitionRequiresStableSupportingLayers() {
          assertTrue(InterstellarMapPanel.canUseRetainedMapModeTransition(
              true, true, false, false, false));
          assertFalse(InterstellarMapPanel.canUseRetainedMapModeTransition(
              false, true, false, false, false));
          assertFalse(InterstellarMapPanel.canUseRetainedMapModeTransition(
              true, false, false, false, false));
          assertFalse(InterstellarMapPanel.canUseRetainedMapModeTransition(
              true, true, true, false, false));
          assertFalse(InterstellarMapPanel.canUseRetainedMapModeTransition(
              true, true, false, true, false));
          assertFalse(InterstellarMapPanel.canUseRetainedMapModeTransition(
              true, true, false, false, true));
        }

    @Test
    void mergedNavigationRequiresStableRouteRendering() {
        assertTrue(InterstellarMapPanel.canUseMergedNavigation(true, false, false, false));
        assertFalse(InterstellarMapPanel.canUseMergedNavigation(false, false, false, false));
        assertFalse(InterstellarMapPanel.canUseMergedNavigation(true, true, false, false));
        assertFalse(InterstellarMapPanel.canUseMergedNavigation(true, false, true, false));
        assertFalse(InterstellarMapPanel.canUseMergedNavigation(true, false, false, true));
    }

    @Test
    void activeZoomKeepsSystemArtLiveUntilExactRenderingSettles() {
        assertTrue(InterstellarMapPanel.canUseRetainedSystemArt(true, false, false, false));
        assertFalse(InterstellarMapPanel.canUseRetainedSystemArt(true, false, false, true));
        assertTrue(InterstellarMapPanel.canUseRetainedSystemArt(true, true, false, true));
        assertFalse(InterstellarMapPanel.canUseRetainedSystemArt(false, false, false, false));
    }

    @Test
    void supportingLayerFadeKeepsSystemArtRetained() {
        assertTrue(InterstellarMapPanel.canUseRetainedSystemArt(false, false, true, false));
        assertFalse(InterstellarMapPanel.canUseRetainedSystemArt(false, false, true, true));
    }

        @Test
        void retainedTransitionDoesNotPaintTargetSystemArtAgain() {
          boolean retainedTransition = InterstellarMapPanel.canUseRetainedMapModeTransition(
              true, true, false, false, false);
          boolean retainedSystemArt = InterstellarMapPanel.canUseRetainedSystemArt(
              false, retainedTransition, false, false);

          assertFalse(InterstellarMapPanel.shouldPaintSeparateSystemArt(retainedSystemArt, retainedTransition));
          assertFalse(InterstellarMapPanel.shouldPaintSeparateSystemArt(true, true));
          assertTrue(InterstellarMapPanel.shouldPaintSeparateSystemArt(true, false));
          assertFalse(InterstellarMapPanel.shouldPaintSeparateSystemArt(false, false));
        }

        @Test
        void territoryRasterSurvivesModeChangesAfterPanning() {
          InterstellarMapPanel.PannableRenderLayerCache<InterstellarMapPanel.RetainedCartographyKey> cache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          InterstellarMapPanel.TerritoryDataKey dataKey =
              new InterstellarMapPanel.TerritoryDataKey(LocalDate.of(3050, 12, 23), 1L);
          AtomicInteger rendererCalls = new AtomicInteger();
          InterstellarMapPanel.RetainedCartographyKey faction = new InterstellarMapPanel.RetainedCartographyKey(
              dataKey, InterstellarMapPanel.MapMode.FACTION, InterstellarMapPanel.HpgNetworkDetail.CLASS_A_B,
              false, Double.doubleToLongBits(0.5), 1L, 2L, 3L, 4L, 5L, 6L, 24, 16, 100, 8, 2);
          InterstellarMapPanel.RetainedCartographyKey technology = new InterstellarMapPanel.RetainedCartographyKey(
              dataKey, InterstellarMapPanel.MapMode.TECHNOLOGY, InterstellarMapPanel.HpgNetworkDetail.CLASS_A_B,
              true, Double.doubleToLongBits(0.5), 0L, 0L, 0L, 0L, 0L, 0L, 0, 0, 0, 0, 0);
          InterstellarMapPanel.RenderViewKey pannedView = viewKey(100, 80, 5.0, -3.0, 2.0);
          InterstellarMapPanel.PannableRenderLayer initial = cache.getOrRender(
              faction.territoryOnly(), viewKey(100, 80, 0.0, 0.0, 2.0), 20,
              graphics -> rendererCalls.incrementAndGet());
          InterstellarMapPanel.PannableRenderLayer panned = cache.getOrRender(
              faction.territoryOnly(), pannedView, 20, graphics -> rendererCalls.incrementAndGet());
          InterstellarMapPanel.PannableRenderLayer switched = cache.getOrRefresh(
              technology.territoryOnly(), pannedView, 20, 0, graphics -> rendererCalls.incrementAndGet());

          assertEquals(faction.territoryOnly(), technology.territoryOnly());
          assertSame(initial.image(), switched.image());
          assertEquals(panned.drawX(), switched.drawX());
          assertEquals(panned.drawY(), switched.drawY());
          assertEquals(1, rendererCalls.get());
        }

        @Test
        void territoryCacheIdentityPreservesDataAndOpacity() {
          InterstellarMapPanel.TerritoryDataKey dataKey =
              new InterstellarMapPanel.TerritoryDataKey(LocalDate.of(3050, 12, 23), 7L);
          InterstellarMapPanel.RetainedCartographyKey key = new InterstellarMapPanel.RetainedCartographyKey(
              dataKey, InterstellarMapPanel.MapMode.TECHNOLOGY, InterstellarMapPanel.HpgNetworkDetail.CLASS_A_B,
              true, Double.doubleToLongBits(0.5), 1L, 2L, 3L, 4L, 5L, 6L, 24, 16, 100, 8, 2);

          assertEquals(dataKey, key.territoryOnly().dataKey());
          assertEquals(key.territoryAlphaBits(), key.territoryOnly().territoryAlphaBits());
          assertEquals(key.territoryOnly(), key.territoryOnly().territoryOnly());
        }

    @Test
    void territoryOpacityRemainsStableAcrossSemanticZoomBands() {
        double atlasAlpha = InterstellarMapPanel.SemanticZoomProfile.create(0.8, 3.0).territoryAlpha();
        double navigationAlpha = InterstellarMapPanel.SemanticZoomProfile.create(2.4, 3.0).territoryAlpha();
        double detailAlpha = InterstellarMapPanel.SemanticZoomProfile.create(5.6, 3.0).territoryAlpha();

        assertEquals(atlasAlpha, navigationAlpha);
        assertEquals(atlasAlpha, detailAlpha);
    }

    @Test
    void factionLogoSizeTracksProjectedSizeWithoutFourPixelPulses() {
        assertEquals(0, InterstellarMapPanel.resolveLogoSize(23.9, 24, 100));
        assertEquals(43, InterstellarMapPanel.resolveLogoSize(43.4, 24, 100));
        assertEquals(44, InterstellarMapPanel.resolveLogoSize(43.6, 24, 100));
        assertEquals(100, InterstellarMapPanel.resolveLogoSize(120.0, 24, 100));
    }

        @Test
        void factionLogoRemainsVisibleWhileItsBoundsCrossViewportEdge() {
          assertTrue(InterstellarMapPanel.isFactionLogoVisible(
              new Rectangle2D.Double(-40.0, 20.0, 50.0, 50.0), 2, 800, 600));
          assertTrue(InterstellarMapPanel.isFactionLogoVisible(
              new Rectangle2D.Double(795.0, 20.0, 50.0, 50.0), 2, 800, 600));
          assertTrue(InterstellarMapPanel.isFactionLogoVisible(
              new Rectangle2D.Double(-51.0, 20.0, 50.0, 50.0), 2, 800, 600));
          assertFalse(InterstellarMapPanel.isFactionLogoVisible(
              new Rectangle2D.Double(-53.0, 20.0, 50.0, 50.0), 2, 800, 600));
        }

    @Test
    void disputedTerritoryUsesEqualContiguousOwnerBands() {
        Faction firstFaction = mock(Faction.class);
        Faction secondFaction = mock(Faction.class);
        Faction thirdFaction = mock(Faction.class);
        when(firstFaction.getColor()).thenReturn(Color.RED);
        when(secondFaction.getColor()).thenReturn(Color.GREEN);
        when(thirdFaction.getColor()).thenReturn(Color.BLUE);

          Paint paint = InterstellarMapPanel.createDisputedTerritoryPaint(
              List.of(firstFaction, secondFaction, thirdFaction), 1.0, 12.0);

        LinearGradientPaint bands = (LinearGradientPaint) paint;
        assertEquals(6, bands.getFractions().length);
        assertEquals(bands.getColors()[0], bands.getColors()[1]);
        assertEquals(bands.getColors()[2], bands.getColors()[3]);
        assertEquals(bands.getColors()[4], bands.getColors()[5]);
        assertEquals(18.0, bands.getEndPoint().getY(), 0.001);

          double closeBandWidth = InterstellarMapPanel.TerritoryVisualProfile.create(5.6).disputedBandWidth();
          LinearGradientPaint closeBands = (LinearGradientPaint) InterstellarMapPanel
              .createDisputedTerritoryPaint(
                  List.of(firstFaction, secondFaction, thirdFaction), 1.0, closeBandWidth);
          assertEquals(22.0, closeBandWidth, 0.001);
          assertEquals(33.0, closeBands.getEndPoint().getY(), 0.001);
    }

    @Test
    void mapScaleStaysBounded() {
        assertEquals(0.1, InterstellarMapPanel.boundedMapScale(0.001), 0.000_001);
        assertEquals(4.7, InterstellarMapPanel.boundedMapScale(4.7), 0.000_001);
        assertEquals(10.0, InterstellarMapPanel.boundedMapScale(1_000.0), 0.000_001);
    }

    @Test
    void optionViewLayoutComparisonDetectsGeometryChanges() {
        JPanel control = new JPanel();
        JViewport view = new JViewport();
        Rectangle controlBounds = new Rectangle(10, 20, 200, 300);
        Dimension viewSize = new Dimension(180, 280);
        Rectangle viewBounds = new Rectangle(1, 1, 198, 298);
        Point viewPosition = new Point(0, 0);
        control.setBounds(controlBounds);
        view.setView(new JPanel());
        view.setBounds(viewBounds);
        view.setViewSize(viewSize);
        view.setViewPosition(viewPosition);

        assertTrue(InterstellarMapPanel.isOptionViewLayoutCurrent(
              control, view, controlBounds, viewSize, viewBounds, viewPosition));
        assertFalse(InterstellarMapPanel.isOptionViewLayoutCurrent(
              control, view, new Rectangle(10, 20, 201, 300), viewSize, viewBounds, viewPosition));
    }

    @Test
    void identicalViewReusesRasterAcrossAnimationStyleRepaints() {
        InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.RenderViewKey> cache =
              new InterstellarMapPanel.RenderLayerCache<>();
        InterstellarMapPanel.RenderViewKey viewKey =
              InterstellarMapPanel.RenderViewKey.create(80, 60, 12.5, -7.25, 2.0);
        AtomicInteger rendererCalls = new AtomicInteger();

        BufferedImage first = cache.getOrRender(viewKey, 80, 60, graphics -> {
            rendererCalls.incrementAndGet();
            graphics.setColor(Color.CYAN);
            graphics.fillRect(0, 0, 80, 60);
        });
        BufferedImage second = cache.getOrRender(viewKey, 80, 60, graphics -> rendererCalls.incrementAndGet());

        assertSame(first, second);
        assertEquals(1, rendererCalls.get());
        assertEquals(1, cache.getRenderCount());
        assertEquals(Color.CYAN.getRGB(), second.getRGB(40, 30));
    }

    @Test
    void changedSameSizeKeyReusesAndFullyClearsRasterBeforeOneRerender() {
        InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.RenderViewKey> cache =
              new InterstellarMapPanel.RenderLayerCache<>();
        InterstellarMapPanel.RenderViewKey firstKey = viewKey(4, 2, 0.0, 0.0, 1.0);
        InterstellarMapPanel.RenderViewKey changedKey = viewKey(4, 2, 1.0, 0.0, 1.0);
        AtomicInteger rendererCalls = new AtomicInteger();
        BufferedImage first = cache.getOrRender(firstKey, 4, 2, graphics -> {
            rendererCalls.incrementAndGet();
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, 4, 2);
        });

        BufferedImage changed = cache.getOrRender(changedKey, 4, 2, graphics -> {
            rendererCalls.incrementAndGet();
            graphics.setColor(Color.GREEN);
            graphics.fillRect(0, 0, 1, 1);
        });
        BufferedImage unchanged = cache.getOrRender(changedKey, 4, 2,
              graphics -> rendererCalls.incrementAndGet());

        assertSame(first, changed);
        assertSame(changed, unchanged);
        assertEquals(Color.GREEN.getRGB(), changed.getRGB(0, 0));
        assertEquals(0, changed.getRGB(3, 1), "transparent regions must not retain pixels from the old key");
        assertEquals(2, rendererCalls.get());
        assertEquals(2, cache.getRenderCount());
    }

    @Test
    void changedDimensionsAllocateNewRaster() {
        InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.RenderViewKey> cache =
              new InterstellarMapPanel.RenderLayerCache<>();
        BufferedImage first = cache.getOrRender(viewKey(4, 2, 0.0, 0.0, 1.0), 4, 2, graphics -> { });

        BufferedImage resized = cache.getOrRender(viewKey(5, 2, 0.0, 0.0, 1.0), 5, 2, graphics -> { });

        assertNotSame(first, resized);
        assertEquals(BufferedImage.TYPE_INT_ARGB_PRE, resized.getType());
        assertEquals(2, cache.getRenderCount());
    }

        @Test
        void pannableCacheReusesExactScaleRasterUntilViewportLeavesOverscan() {
          InterstellarMapPanel.PannableRenderLayerCache<String> cache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          AtomicInteger rendererCalls = new AtomicInteger();
          InterstellarMapPanel.RenderViewKey initialView = viewKey(100, 80, 0.0, 0.0, 2.0);

          InterstellarMapPanel.PannableRenderLayer initial = cache.getOrRender("territory", initialView, 20,
              graphics -> rendererCalls.incrementAndGet());
          InterstellarMapPanel.PannableRenderLayer panned = cache.getOrRender("territory",
              viewKey(100, 80, 5.0, -3.0, 2.0), 20, graphics -> rendererCalls.incrementAndGet());

          assertSame(initial.image(), panned.image());
          assertEquals(-10, panned.drawX());
          assertEquals(-26, panned.drawY());
          assertEquals(1, rendererCalls.get());

          InterstellarMapPanel.PannableRenderLayer outsideCoverage = cache.getOrRender("territory",
              viewKey(100, 80, 11.0, -3.0, 2.0), 20, graphics -> rendererCalls.incrementAndGet());
          assertSame(initial.image(), outsideCoverage.image());
          assertEquals(-20, outsideCoverage.drawX());
          assertEquals(-20, outsideCoverage.drawY());
          assertEquals(2, rendererCalls.get());
          assertEquals(2, cache.getRenderCount());
                    assertEquals(1, cache.getReuseCount());
                    assertEquals(1, cache.getStripRefreshCount());
                    assertEquals(1, cache.getFullRenderCount());
        }

        @Test
        void pannableCacheRerendersInsteadOfApplyingFractionalTranslation() {
          InterstellarMapPanel.PannableRenderLayerCache<String> cache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          AtomicInteger rendererCalls = new AtomicInteger();
          cache.getOrRender("territory", viewKey(100, 80, 0.0, 0.0, 2.0), 20,
              graphics -> rendererCalls.incrementAndGet());

          InterstellarMapPanel.PannableRenderLayer fractionalPan = cache.getOrRender("territory",
              viewKey(100, 80, 0.25, 0.0, 2.0), 20, graphics -> rendererCalls.incrementAndGet());

          assertEquals(-20, fractionalPan.drawX());
          assertEquals(2, rendererCalls.get());
                    assertEquals(0, cache.getReuseCount());
                    assertEquals(0, cache.getStripRefreshCount());
                    assertEquals(2, cache.getFullRenderCount());
        }

            @Test
            void pannableCacheDefersFullRenderAndInstallsPreparedRaster() {
              InterstellarMapPanel.PannableRenderLayerCache<String> cache =
                  new InterstellarMapPanel.PannableRenderLayerCache<>();
              InterstellarMapPanel.RenderViewKey initialView = viewKey(100, 80, 0.0, 0.0, 2.0);
              AtomicInteger rendererCalls = new AtomicInteger();

              assertNull(cache.getOrRefresh("territory", initialView, 20, 0,
                  graphics -> rendererCalls.incrementAndGet()));
              BufferedImage prepared = new BufferedImage(140, 120, BufferedImage.TYPE_INT_ARGB_PRE);
              Graphics2D preparedGraphics = prepared.createGraphics();
              preparedGraphics.setColor(Color.RED);
              preparedGraphics.fillRect(0, 0, prepared.getWidth(), prepared.getHeight());
              preparedGraphics.dispose();
              cache.install("territory", initialView, 20, prepared);

              InterstellarMapPanel.PannableRenderLayer available = cache.getOrRefresh(
                  "territory", initialView, 20, 0, graphics -> rendererCalls.incrementAndGet());
              assertSame(prepared, available.image());
              assertEquals(-20, available.drawX());
              assertEquals(0, rendererCalls.get());
              assertEquals(1, cache.getFullRenderCount());

              InterstellarMapPanel.RenderViewKey zoomedView = viewKey(100, 80, 0.0, 0.0, 3.0);
              assertNull(cache.getOrRefresh("territory", zoomedView, 20, 0,
                  graphics -> rendererCalls.incrementAndGet()));
              InterstellarMapPanel.PannableRenderLayerSnapshot<String> snapshot =
                  cache.snapshot("territory");
              BufferedImage target = new BufferedImage(100, 80, BufferedImage.TYPE_INT_ARGB_PRE);
              Graphics2D targetGraphics = target.createGraphics();
              InterstellarMapPanel.drawPannableSnapshot(targetGraphics, snapshot, zoomedView, 1.0);
              targetGraphics.dispose();

              assertEquals(Color.RED.getRGB(), target.getRGB(50, 40));
              assertSame(prepared, cache.snapshot("territory").image());
              assertEquals(1, cache.getFullRenderCount());
              assertNull(cache.snapshot("other"));
            }

    @Test
    void pannableCacheRendersOnlyExposedPixelsWhenRecenteringAfterPan() {
        InterstellarMapPanel.PannableRenderLayerCache<String> cache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
        AtomicInteger rendererCalls = new AtomicInteger();
        cache.getOrRender("territory", viewKey(4, 2, 0.0, 0.0, 1.0), 1, graphics -> {
            rendererCalls.incrementAndGet();
            graphics.setColor(Color.RED);
            graphics.fillRect(-1, -1, 6, 4);
        });

        InterstellarMapPanel.PannableRenderLayer recentered = cache.getOrRender("territory",
              viewKey(4, 2, 2.0, 0.0, 1.0), 1, graphics -> {
                  rendererCalls.incrementAndGet();
                  graphics.setColor(Color.BLUE);
                  graphics.fillRect(-1, -1, 6, 4);
              });

        assertEquals(-1, recentered.drawX());
        assertEquals(-1, recentered.drawY());
        assertEquals(Color.BLUE.getRGB(), recentered.image().getRGB(0, 1));
        assertEquals(Color.RED.getRGB(), recentered.image().getRGB(2, 1));
        assertEquals(2, rendererCalls.get());
    }

        @Test
        void pannableCacheRecenteringMatchesFreshRasterWithinPremultipliedRounding() {
          InterstellarMapPanel.PannableRenderLayerCache<String> incrementalCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          InterstellarMapPanel.RenderViewKey initialView = viewKey(24, 16, 0.0, 0.0, 1.0);
          InterstellarMapPanel.RenderViewKey shiftedView = viewKey(24, 16, 5.0, -2.0, 1.0);
          incrementalCache.getOrRender("territory", initialView, 4,
              graphics -> drawShiftedTestPattern(graphics, 0, 0));

          BufferedImage incremental = incrementalCache.getOrRender("territory", shiftedView, 4,
              graphics -> drawShiftedTestPattern(graphics, 5, -2)).image();
          InterstellarMapPanel.PannableRenderLayerCache<String> freshCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          BufferedImage fresh = freshCache.getOrRender("territory", shiftedView, 4,
              graphics -> drawShiftedTestPattern(graphics, 5, -2)).image();

          assertPremultipliedImagesEquivalent(fresh, incremental);
        }

        @Test
        void pannableCacheRecenteringMatchesFreshRasterForReverseOverlap() {
          InterstellarMapPanel.PannableRenderLayerCache<String> incrementalCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          InterstellarMapPanel.RenderViewKey initialView = viewKey(24, 16, 0.0, 0.0, 1.0);
          InterstellarMapPanel.RenderViewKey shiftedView = viewKey(24, 16, -5.0, 2.0, 1.0);
          incrementalCache.getOrRender("territory", initialView, 4,
              graphics -> drawShiftedTestPattern(graphics, 0, 0));

          BufferedImage incremental = incrementalCache.getOrRender("territory", shiftedView, 4,
              graphics -> drawShiftedTestPattern(graphics, -5, 2)).image();
          InterstellarMapPanel.PannableRenderLayerCache<String> freshCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          BufferedImage fresh = freshCache.getOrRender("territory", shiftedView, 4,
              graphics -> drawShiftedTestPattern(graphics, -5, 2)).image();

          assertPremultipliedImagesEquivalent(fresh, incremental);
        }

        private static void drawShiftedTestPattern(Graphics2D graphics, int deltaX, int deltaY) {
          graphics.setColor(new Color(40, 170, 220, 180));
          graphics.setStroke(new java.awt.BasicStroke(3.0f));
          graphics.draw(new Line2D.Double(-8 + deltaX, 2 + deltaY, 30 + deltaX, 14 + deltaY));
        }

    @Test
    void pannableLayerBlitsOnlyTheVisibleSourceRectangle() {
        BufferedImage source = new BufferedImage(6, 4, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D sourceGraphics = source.createGraphics();
        sourceGraphics.setColor(Color.RED);
        sourceGraphics.fillRect(0, 0, 6, 4);
        sourceGraphics.setColor(Color.GREEN);
        sourceGraphics.fillRect(2, 1, 3, 2);
        sourceGraphics.dispose();
        BufferedImage target = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D targetGraphics = target.createGraphics();

        InterstellarMapPanel.drawPannableRenderLayer(targetGraphics,
              new InterstellarMapPanel.PannableRenderLayer(source, -2, -1), 3, 2, 1.0);
        targetGraphics.dispose();

        assertEquals(Color.GREEN.getRGB(), target.getRGB(0, 0));
        assertEquals(Color.GREEN.getRGB(), target.getRGB(2, 1));
    }

    @Test
    void pannableSnapshotTransformPreservesWorldCoordinatesAcrossScaleAndCenterChanges() {
        InterstellarMapPanel.RenderViewKey renderedView = viewKey(100, 80, 10.0, -5.0, 2.0);
        InterstellarMapPanel.RenderViewKey requestedView = viewKey(100, 80, 20.0, 0.0, 4.0);
        BufferedImage image = new BufferedImage(140, 120, BufferedImage.TYPE_INT_ARGB_PRE);
        InterstellarMapPanel.PannableRenderLayerSnapshot<String> snapshot =
              new InterstellarMapPanel.PannableRenderLayerSnapshot<>("territory", renderedView, image, 20);

        Point2D transformed = InterstellarMapPanel.transformPannableSnapshot(snapshot, requestedView)
              .transform(new Point2D.Double(90.0, 50.0), null);

        assertEquals(130.0, transformed.getX());
        assertEquals(40.0, transformed.getY());
    }

    @Test
    void largerCartographyCacheFeedsNavigationRefreshWithoutRerendering() {
        InterstellarMapPanel.PannableRenderLayerCache<String> cartographyCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
        InterstellarMapPanel.PannableRenderLayerCache<String> navigationCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
        AtomicInteger cartographyRenders = new AtomicInteger();
        InterstellarMapPanel.RenderViewKey initialView = viewKey(24, 16, 0.0, 0.0, 1.0);
        InterstellarMapPanel.PannableRenderLayer initialCartography = cartographyCache.getOrRender(
              "cartography", initialView, 8, 4, graphics -> {
                  cartographyRenders.incrementAndGet();
                  drawOpaqueShiftedTestPattern(graphics, 0, 0);
              });
        navigationCache.getOrRender("navigation", initialView, 4,
              graphics -> graphics.drawImage(initialCartography.image(), initialCartography.drawX(),
                  initialCartography.drawY(), null));

        InterstellarMapPanel.RenderViewKey shiftedView = viewKey(24, 16, 5.0, 0.0, 1.0);
        InterstellarMapPanel.PannableRenderLayer shiftedCartography = cartographyCache.getOrRender(
              "cartography", shiftedView, 8, 4, graphics -> {
                  cartographyRenders.incrementAndGet();
                  drawOpaqueShiftedTestPattern(graphics, 5, 0);
              });
        navigationCache.getOrRender(
              "navigation", shiftedView, 4,
              graphics -> graphics.drawImage(shiftedCartography.image(), shiftedCartography.drawX(),
                  shiftedCartography.drawY(), null));

        InterstellarMapPanel.RenderViewKey exposedView = viewKey(24, 16, 9.0, 0.0, 1.0);
        InterstellarMapPanel.PannableRenderLayer refreshedCartography = cartographyCache.getOrRender(
              "cartography", exposedView, 8, 4, graphics -> {
                  cartographyRenders.incrementAndGet();
                  drawOpaqueShiftedTestPattern(graphics, 9, 0);
              });
        InterstellarMapPanel.PannableRenderLayer exposedNavigation = navigationCache.getOrRender(
              "navigation", exposedView, 4,
              graphics -> graphics.drawImage(refreshedCartography.image(), refreshedCartography.drawX(),
                  refreshedCartography.drawY(), null));
        BufferedImage actual = new BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D actualGraphics = actual.createGraphics();
        InterstellarMapPanel.drawPannableRenderLayer(actualGraphics, exposedNavigation, 24, 16, 1.0);
        actualGraphics.dispose();
          InterstellarMapPanel.PannableRenderLayerCache<String> freshCartographyCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          InterstellarMapPanel.PannableRenderLayer freshCartography = freshCartographyCache.getOrRender(
              "cartography", exposedView, 8, 4,
              graphics -> drawOpaqueShiftedTestPattern(graphics, 9, 0));
          InterstellarMapPanel.PannableRenderLayerCache<String> freshNavigationCache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
          InterstellarMapPanel.PannableRenderLayer freshNavigation = freshNavigationCache.getOrRender(
              "navigation", exposedView, 4,
              graphics -> graphics.drawImage(freshCartography.image(), freshCartography.drawX(),
                freshCartography.drawY(), null));
        BufferedImage expected = new BufferedImage(24, 16, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D expectedGraphics = expected.createGraphics();
          InterstellarMapPanel.drawPannableRenderLayer(expectedGraphics, freshNavigation, 24, 16, 1.0);
        expectedGraphics.dispose();

        assertPremultipliedImagesEquivalent(expected, actual);
        assertEquals(2, cartographyRenders.get());
        assertEquals(1, cartographyCache.getReuseCount());
        assertEquals(1, navigationCache.getStripRefreshCount());
        assertEquals(1, navigationCache.getReuseCount());
    }

    private static void drawOpaqueShiftedTestPattern(Graphics2D graphics, int deltaX, int deltaY) {
        graphics.setColor(Color.BLACK);
        graphics.fillRect(-20, -20, 80, 60);
        drawShiftedTestPattern(graphics, deltaX, deltaY);
    }

        @Test
        void renderLayerOverscanRespectsRasterPixelBudget() {
          assertEquals(512, InterstellarMapPanel.renderLayerOverscan(2560, 1600));
          int fiveKMargin = InterstellarMapPanel.renderLayerOverscan(5120, 2880);
          assertTrue(fiveKMargin > 0);
          assertTrue(InterstellarMapPanel.canCacheRenderLayer(
              5120 + (fiveKMargin * 2), 2880 + (fiveKMargin * 2)));
          assertEquals(0, InterstellarMapPanel.renderLayerOverscan(8192, 8192));
        }

    @Test
    void retainedCartographyUsesLargerOverscanWithinPixelBudget() {
        int width = 1280;
        int height = 720;
        int overscan = InterstellarMapPanel.retainedCartographyOverscan(width, height);

        assertEquals(1024, overscan);
        assertTrue(overscan > InterstellarMapPanel.renderLayerOverscan(width, height));
        assertTrue(InterstellarMapPanel.canCacheRenderLayer(
              width + (overscan * 2), height + (overscan * 2)));
        assertEquals(0, InterstellarMapPanel.retainedCartographyOverscan(8192, 8192));
    }

    @Test
    void spatialIndexReturnsBoundedAndRequiredSystemsInOriginalOrder() {
        PlanetarySystem right = systemAt("right", 10.0, 0.0);
        PlanetarySystem required = systemAt("required", 100.0, 100.0);
        PlanetarySystem equivalentRequired = systemAt("required", -100.0, -100.0);
        PlanetarySystem left = systemAt("left", 0.0, 0.0);
        PlanetarySystem above = systemAt("above", 5.0, 20.0);
        InterstellarMapPanel.SystemSpatialIndex index =
              new InterstellarMapPanel.SystemSpatialIndex(List.of(right, required, left, above));

        List<PlanetarySystem> result = index.query(-1.0, -1.0, 11.0, 1.0, equivalentRequired, null);

        assertEquals(List.of(right, required, left), result);
    }

    @Test
    void retainedSystemQueryBoundsIncludeMarkerExtentBeyondCacheEdge() {
        InterstellarMapPanel.MapQueryBounds bounds = InterstellarMapPanel.retainedSystemQueryBounds(
              viewKey(100, 80, 10.0, -5.0, 2.0), 20, 8.0);

        assertEquals(-49.0, bounds.minX());
        assertEquals(-39.0, bounds.minY());
        assertEquals(29.0, bounds.maxX());
        assertEquals(29.0, bounds.maxY());
    }

    @Test
    void viewportSystemQueryBoundsIncludeRightExtendingVisualBeyondLeftEdge() {
        InterstellarMapPanel.MapQueryBounds bounds = InterstellarMapPanel.viewportSystemQueryBounds(
              viewKey(100, 80, 10.0, -5.0, 2.0), 8.0, 48.0);

        assertEquals(-59.0, bounds.minX());
        assertEquals(-29.0, bounds.minY());
        assertEquals(19.0, bounds.maxX());
        assertEquals(19.0, bounds.maxY());
    }

    @Test
    void viewportQueryIncludesLeftHpgBadgeBeforeSystemEntersRightEdge() {
        for (double size : new double[] { 3.0, 7.5, 12.0, 18.0, 25.0 }) {
            double extent = InterstellarMapPanel.viewportMarkerQueryExtent(size, true);
            InterstellarMapPanel.MapQueryBounds bounds = InterstellarMapPanel.viewportSystemQueryBounds(
                  viewKey(100, 80, 0.0, 0.0, 2.0), extent, extent);
            for (InterstellarMapPanel.RouteMarkerState state : InterstellarMapPanel.RouteMarkerState.values()) {
                InterstellarMapPanel.SystemMarkerLayout layout = InterstellarMapPanel.SystemMarkerLayout.create(
                      0.0, 0.0, size, state, false, false);
                double radius = InterstellarMapPanel.hpgStationMarkerRadius(size,
                      mekhq.campaign.universe.enums.HPGRating.A);
                double badgeExtent = -layout.hpgStationAnchor(radius).x + radius + 2.0;
                double enteringSystemX = (50.0 + badgeExtent) / 2.0;
                assertTrue(enteringSystemX < bounds.maxX());
            }
            assertEquals(size * 2.0, InterstellarMapPanel.viewportMarkerQueryExtent(size, false));
        }
    }

    @Test
    void hpgClipRetainsCrossingLinksAndRejectsUnrelatedLinks() {
        Rectangle2D clip = new Rectangle2D.Double(40, 20, 10, 60);
        assertTrue(InterstellarMapPanel.isHpgLinkVisible(new Line2D.Double(-100, 50, 200, 50), clip));
        assertTrue(InterstellarMapPanel.isHpgLinkVisible(new Line2D.Double(45, -100, 45, 200), clip));
        assertFalse(InterstellarMapPanel.isHpgLinkVisible(new Line2D.Double(-100, 10, 200, 10), clip));
        assertFalse(InterstellarMapPanel.isHpgLinkVisible(new Line2D.Double(0, 0, 35, 90), clip));
        assertTrue(InterstellarMapPanel.isHpgLinkVisible(new Line2D.Double(0, 0, 35, 90), null));
    }

    @Test
    void hpgClipFilteringPreservesDashedStripPixels() {
        BufferedImage expected = new BufferedImage(100, 80, BufferedImage.TYPE_INT_ARGB_PRE);
        BufferedImage actual = new BufferedImage(100, 80, BufferedImage.TYPE_INT_ARGB_PRE);
        Rectangle strip = new Rectangle(40, 0, 10, 80);
        Rectangle2D paddedClip = new Rectangle2D.Double(36, -4, 18, 88);
        List<Line2D.Double> segments = List.of(
              new Line2D.Double(-100, 35, 200, 35),
              new Line2D.Double(39.5, -20, 39.5, 100),
              new Line2D.Double(-100, -100, 200, 200),
              new Line2D.Double(0, 10, 20, 70));
        for (BufferedImage image : List.of(expected, actual)) {
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setClip(strip);
                graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                      java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(Color.CYAN);
                graphics.setStroke(new java.awt.BasicStroke(0.8f, java.awt.BasicStroke.CAP_BUTT,
                      java.awt.BasicStroke.JOIN_ROUND, 10.0f, new float[] { 12.0f, 8.0f }, 0.0f));
                for (Line2D.Double segment : segments) {
                      if ((image == expected) || (InterstellarMapPanel.isHpgLinkVisible(segment, paddedClip)
                          && InterstellarMapPanel.hpgLinkIntersectsDamage(segment, strip, 4.0))) {
                        graphics.draw(segment);
                    }
                }
            } finally {
                graphics.dispose();
            }
        }
        assertTrue((expected.getRGB(40, 35) >>> 24) > 0);
        assertPremultipliedImagesEquivalent(expected, actual);
    }

        @Test
        void hpgDamageClipExcludesRetainedInteriorDuringDiagonalPan() {
          java.awt.geom.Area damage = new java.awt.geom.Area(new Rectangle(0, 0, 100, 80));
          damage.subtract(new java.awt.geom.Area(new Rectangle(10, 10, 90, 70)));
          Line2D interior = new Line2D.Double(30, 30, 80, 60);

          assertTrue(InterstellarMapPanel.isHpgLinkVisible(interior, damage.getBounds2D()));
          assertFalse(InterstellarMapPanel.hpgLinkIntersectsDamage(interior, damage, 4.0));
          assertTrue(InterstellarMapPanel.hpgLinkIntersectsDamage(
              new Line2D.Double(-20, 40, 120, 40), damage, 4.0));
          assertTrue(InterstellarMapPanel.hpgLinkIntersectsDamage(
              new Line2D.Double(12, 20, 12, 70), damage, 4.0));
          assertTrue(InterstellarMapPanel.hpgLinkIntersectsDamage(interior, null, 4.0));
        }

        private static void assertPremultipliedImagesEquivalent(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        int[] expectedPixels = ((DataBufferInt) expected.getRaster().getDataBuffer()).getData();
        int[] actualPixels = ((DataBufferInt) actual.getRaster().getDataBuffer()).getData();
        for (int index = 0; index < expectedPixels.length; index++) {
            for (int shift = 0; shift <= 24; shift += 8) {
                int expectedChannel = (expectedPixels[index] >>> shift) & 0xFF;
                int actualChannel = (actualPixels[index] >>> shift) & 0xFF;
                assertTrue(Math.abs(expectedChannel - actualChannel) <= 1,
                      "premultiplied pixel channel differs at index " + index + ", shift " + shift);
            }
        }
    }

    private static PlanetarySystem systemAt(String id, double x, double y) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn(id);
        when(system.getX()).thenReturn(x);
        when(system.getY()).thenReturn(y);
        return system;
    }

    @Test
    void renderCacheSizeGuardAccepts4kAndRejectsOversizedOrInvalidLayers() {
        assertTrue(InterstellarMapPanel.canCacheRenderLayer(3840, 2160));
        assertFalse(InterstellarMapPanel.canCacheRenderLayer(8192, 8192));
        assertFalse(InterstellarMapPanel.canCacheRenderLayer(0, 2160));
        assertFalse(InterstellarMapPanel.canCacheRenderLayer(3840, -1));
    }

    @Test
    void viewAndCartographyDependenciesInvalidateOnlyAffectedLayers() {
        InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.RenderViewKey> backgroundCache =
              new InterstellarMapPanel.RenderLayerCache<>();
        InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.TerritoryRenderKey> territoryCache =
              new InterstellarMapPanel.RenderLayerCache<>();
        LocalDate date = LocalDate.of(3151, 4, 12);
        InterstellarMapPanel.RenderViewKey view = viewKey(80, 60, 0.0, 0.0, 1.0);

        renderLayers(backgroundCache, territoryCache, view, date, 1);
        renderLayers(backgroundCache, territoryCache, view, date, 1);
        assertEquals(1, backgroundCache.getRenderCount());
        assertEquals(1, territoryCache.getRenderCount());

        view = viewKey(80, 60, 1.0, 0.0, 1.0);
        renderLayers(backgroundCache, territoryCache, view, date, 1);
        view = viewKey(80, 60, 1.0, 0.0, 2.0);
        renderLayers(backgroundCache, territoryCache, view, date, 1);
        view = viewKey(96, 72, 1.0, 0.0, 2.0);
        renderLayers(backgroundCache, territoryCache, view, date, 1);
        assertEquals(4, backgroundCache.getRenderCount());
        assertEquals(4, territoryCache.getRenderCount());

        renderLayers(backgroundCache, territoryCache, view, date.plusDays(1), 1);
        renderLayers(backgroundCache, territoryCache, view, date.plusDays(1), 2);
        assertEquals(4, backgroundCache.getRenderCount(), "date and data do not affect the grid");
        assertEquals(6, territoryCache.getRenderCount());
    }

    @Test
    void liveAlphaChangesReuseRasterAndCompositeIndependently() {
          InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.TerritoryRenderKey> cache =
              new InterstellarMapPanel.RenderLayerCache<>();
          InterstellarMapPanel.TerritoryRenderKey key = new InterstellarMapPanel.TerritoryRenderKey(
              viewKey(4, 1, 0.0, 0.0, 1.0), LocalDate.of(3151, 4, 12), 1);
        BufferedImage layer = cache.getOrRender(key, 4, 1, graphics -> {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, 4, 1);
        });
        BufferedImage lowAlpha = new BufferedImage(4, 1, BufferedImage.TYPE_INT_ARGB);
        BufferedImage highAlpha = new BufferedImage(4, 1, BufferedImage.TYPE_INT_ARGB);

        drawLayer(lowAlpha, layer, 0.25);
        BufferedImage reusedLayer = cache.getOrRender(key, 4, 1, graphics -> {
            throw new AssertionError("animation progress must not rerender the raster");
        });
        drawLayer(highAlpha, reusedLayer, 0.75);

        assertSame(layer, reusedLayer);
        assertEquals(1, cache.getRenderCount());
        assertEquals(64, alphaAt(lowAlpha, 0));
        assertEquals(191, alphaAt(highAlpha, 0));
    }

        @Test
        void guiScaleSensitiveMetricsInvalidateOnlyTheFactionLogoRaster() {
          InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.FactionLogoRenderKey> cache =
              new InterstellarMapPanel.RenderLayerCache<>();
          InterstellarMapPanel.TerritoryRenderKey territoryKey = new InterstellarMapPanel.TerritoryRenderKey(
              viewKey(8, 6, 0.0, 0.0, 1.0), LocalDate.of(3151, 4, 12), 1);
          InterstellarMapPanel.FactionLogoRenderKey original =
              new InterstellarMapPanel.FactionLogoRenderKey(territoryKey, 36, 24, 100, 8, 2);
          InterstellarMapPanel.FactionLogoRenderKey scaled =
              new InterstellarMapPanel.FactionLogoRenderKey(territoryKey, 54, 36, 150, 12, 3);

          cache.getOrRender(original, 8, 6, graphics -> { });
          cache.getOrRender(original, 8, 6, graphics -> { });
          cache.getOrRender(scaled, 8, 6, graphics -> { });

          assertEquals(2, cache.getRenderCount());
        }

    @Test
    void cachedStaticLayersRemainBehindDynamicNavigationPixels() {
        BufferedImage territory = solidLayer(3, 1, Color.RED);
        BufferedImage emblem = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        emblem.setRGB(1, 0, Color.GREEN.getRGB());
        BufferedImage output = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = output.createGraphics();
        InterstellarMapPanel.drawRenderLayer(graphics, territory, 1.0);
        InterstellarMapPanel.drawRenderLayer(graphics, emblem, 0.5);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(2, 0, 1, 1);
        graphics.dispose();

        assertEquals(Color.RED.getRGB(), output.getRGB(0, 0));
        assertTrue(((output.getRGB(1, 0) >>> 8) & 0xff) > 0, "emblem alpha remains independent");
        assertEquals(Color.BLUE.getRGB(), output.getRGB(2, 0), "navigation paints after static cartography");
    }

    @Test
    void explicitClearReleasesCurrentRasterAndForcesRegeneration() {
        InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.RenderViewKey> cache =
              new InterstellarMapPanel.RenderLayerCache<>();
        InterstellarMapPanel.RenderViewKey key = viewKey(8, 6, 0.0, 0.0, 1.0);
        BufferedImage first = cache.getOrRender(key, 8, 6, graphics -> { });

        cache.clear();

        assertFalse(cache.hasImage());
        BufferedImage second = cache.getOrRender(key, 8, 6, graphics -> { });
        assertNotSame(first, second);
        assertTrue(cache.hasImage());
        assertEquals(2, cache.getRenderCount());
    }

    @Test
    void territoryLookupNeverBuildsAndExplicitDatePreparationDoes() {
        InterstellarMapPanel.PreparedRenderData<InterstellarMapPanel.TerritoryDataKey, String> preparedData =
              new InterstellarMapPanel.PreparedRenderData<>();
        AtomicInteger builderCalls = new AtomicInteger();
        InterstellarMapPanel.TerritoryDataKey firstDate = new InterstellarMapPanel.TerritoryDataKey(
              LocalDate.of(3151, 4, 12), 3);
        InterstellarMapPanel.TerritoryDataKey nextDate = new InterstellarMapPanel.TerritoryDataKey(
              firstDate.date().plusDays(1), 3);

        assertNull(preparedData.get(firstDate));
        assertEquals("atlas-1", preparedData.prepare(firstDate,
              () -> "atlas-" + builderCalls.incrementAndGet()));
        assertEquals("atlas-1", preparedData.get(firstDate));
        assertNull(preparedData.get(nextDate), "a render lookup cannot lazily build or return stale data");
        assertEquals(1, builderCalls.get());

        assertEquals("atlas-2", preparedData.prepare(nextDate,
              () -> "atlas-" + builderCalls.incrementAndGet()));
        assertEquals(2, preparedData.getPreparationCount());
    }

        @Test
        void hiddenRequestsDoNothingAndVisibleRequestsCoalesceToLatestKey() {
          AtomicBoolean showing = new AtomicBoolean();
          ArrayDeque<Runnable> eventLoop = new ArrayDeque<>();
          AtomicInteger preparationCalls = new AtomicInteger();
          AtomicReference<InterstellarMapPanel.TerritoryDataKey> preparedKey = new AtomicReference<>();
          InterstellarMapPanel.StaticCartographyPreparationQueue<InterstellarMapPanel.TerritoryDataKey> queue =
              new InterstellarMapPanel.StaticCartographyPreparationQueue<>(showing::get, eventLoop::addLast, key -> {
                preparationCalls.incrementAndGet();
                preparedKey.set(key);
              });
          InterstellarMapPanel.TerritoryDataKey firstDate = new InterstellarMapPanel.TerritoryDataKey(
              LocalDate.of(3151, 4, 12), 3);
          InterstellarMapPanel.TerritoryDataKey latestDate = new InterstellarMapPanel.TerritoryDataKey(
              firstDate.date().plusDays(1), 4);

          queue.request(firstDate);
          queue.request(latestDate);

          assertTrue(eventLoop.isEmpty(), "hidden map requests must not enqueue preparation");
          assertEquals(0, preparationCalls.get());

          showing.set(true);
          queue.request(firstDate);
          queue.request(latestDate);

          assertEquals(1, eventLoop.size(), "one event-loop runnable must serve the visible request burst");
          assertEquals(0, preparationCalls.get());
          eventLoop.removeFirst().run();
          assertEquals(1, preparationCalls.get());
          assertEquals(latestDate, preparedKey.get());
        }

    @Test
    void backgroundPreparationPublishesOnlyLatestVisibleRequest() {
        AtomicBoolean showing = new AtomicBoolean(true);
        ArrayDeque<Runnable> background = new ArrayDeque<>();
        ArrayDeque<Runnable> eventLoop = new ArrayDeque<>();
        AtomicReference<String> installed = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        InterstellarMapPanel.LatestBackgroundPreparationQueue<String, String> queue =
              new InterstellarMapPanel.LatestBackgroundPreparationQueue<>(showing::get,
                    background::addLast, eventLoop::addLast, key -> "prepared-" + key,
                  (key, value) -> installed.set(value), (key, exception) -> failure.set(exception), value -> { });

        queue.request("first");
        queue.request("latest");
        assertEquals(1, background.size());

        background.removeFirst().run();
        eventLoop.removeFirst().run();
        assertNull(installed.get(), "superseded work must not be published");
        assertEquals(1, background.size(), "the latest request starts after the worker becomes idle");

        background.removeFirst().run();
        eventLoop.removeFirst().run();
        assertEquals("prepared-latest", installed.get());
        assertNull(failure.get());

        showing.set(false);
        queue.request("hidden");
        assertTrue(background.isEmpty());
    }

    @Test
    void backgroundPreparationReturningToInFlightRequestSupersedesPendingWork() {
        ArrayDeque<Runnable> background = new ArrayDeque<>();
        ArrayDeque<Runnable> eventLoop = new ArrayDeque<>();
        AtomicReference<String> installed = new AtomicReference<>();
        InterstellarMapPanel.LatestBackgroundPreparationQueue<String, String> queue =
              new InterstellarMapPanel.LatestBackgroundPreparationQueue<>(() -> true,
                    background::addLast, eventLoop::addLast, key -> "prepared-" + key,
                    (key, value) -> installed.set(value), (key, exception) -> { }, value -> { });

        queue.request("current");
        queue.request("superseded");
        queue.request("current");
        background.removeFirst().run();
        eventLoop.removeFirst().run();

        assertEquals("prepared-current", installed.get());
        assertTrue(background.isEmpty(), "superseded work must not start after returning to the current request");
    }

    @Test
    void pendingBackgroundPreparationResumesWhenMapBecomesVisible() {
        AtomicBoolean showing = new AtomicBoolean(true);
        ArrayDeque<Runnable> background = new ArrayDeque<>();
        ArrayDeque<Runnable> eventLoop = new ArrayDeque<>();
        AtomicReference<String> installed = new AtomicReference<>();
        InterstellarMapPanel.LatestBackgroundPreparationQueue<String, String> queue =
              new InterstellarMapPanel.LatestBackgroundPreparationQueue<>(showing::get,
                    background::addLast, eventLoop::addLast, key -> "prepared-" + key,
                    (key, value) -> installed.set(value), (key, exception) -> { }, value -> { });

        queue.request("obsolete");
        queue.request("current");
        showing.set(false);
        background.removeFirst().run();
        eventLoop.removeFirst().run();
        assertTrue(background.isEmpty());

        showing.set(true);
        queue.request("current");
        assertEquals(1, background.size());
        background.removeFirst().run();
        eventLoop.removeFirst().run();

        assertEquals("prepared-current", installed.get());
    }

        @Test
        void canceledBackgroundPreparationDiscardsCompletedValue() {
          ArrayDeque<Runnable> background = new ArrayDeque<>();
          ArrayDeque<Runnable> eventLoop = new ArrayDeque<>();
          AtomicReference<String> installed = new AtomicReference<>();
          AtomicReference<String> discarded = new AtomicReference<>();
          InterstellarMapPanel.LatestBackgroundPreparationQueue<String, String> queue =
              new InterstellarMapPanel.LatestBackgroundPreparationQueue<>(() -> true,
                  background::addLast, eventLoop::addLast, key -> "prepared-" + key,
                  (key, value) -> installed.set(value), (key, failure) -> { }, discarded::set);

          queue.request("obsolete");
          background.removeFirst().run();
          queue.cancel();
          eventLoop.removeFirst().run();

          assertNull(installed.get());
          assertEquals("prepared-obsolete", discarded.get());
        }

        @Test
        void retainedCartographyWorkerRendersPreparedContoursAtRequestedView() {
          LocalDate date = LocalDate.of(3151, 4, 12);
          mekhq.campaign.universe.Faction faction = mock(mekhq.campaign.universe.Faction.class);
          when(faction.getColor()).thenReturn(Color.RED);
          Rectangle2D contourShape = new Rectangle2D.Double(-5.0, -5.0, 10.0, 10.0);
          InterstellarMapPanel.TerritoryContour contour = new InterstellarMapPanel.TerritoryContour(
              List.of(faction), InterstellarMapPanel.TerritorySemantic.SOVEREIGN,
              contourShape, Color.RED, 1, -5.0, 5.0, -5.0, 5.0);
          InterstellarMapPanel.TerritoryAtlas atlas = new InterstellarMapPanel.TerritoryAtlas(
              date, 0, 0, 0, 0, java.util.Map.of(), List.of(contour), List.of());
          InterstellarMapPanel.RenderViewKey view = viewKey(20, 20, 0.0, 0.0, 1.0);
          InterstellarMapPanel.RetainedCartographyRenderRequest request =
              new InterstellarMapPanel.RetainedCartographyRenderRequest(
                  null, view, 2, atlas, 1.0);

          BufferedImage rendered = InterstellarMapPanel.renderRetainedCartographyTerritory(request);

          assertEquals(24, rendered.getWidth());
          assertEquals(24, rendered.getHeight());
          assertEquals(BufferedImage.TYPE_INT_ARGB_PRE, rendered.getType());
          assertEquals(Color.RED.getRGB(), rendered.getRGB(12, 12));
        }

    private static void renderLayers(
          InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.RenderViewKey> backgroundCache,
          InterstellarMapPanel.RenderLayerCache<InterstellarMapPanel.TerritoryRenderKey> territoryCache,
          InterstellarMapPanel.RenderViewKey view, LocalDate date, long revision) {
        backgroundCache.getOrRender(view, view.width(), view.height(), graphics -> { });
        InterstellarMapPanel.TerritoryRenderKey territoryKey =
              new InterstellarMapPanel.TerritoryRenderKey(view, date, revision);
        territoryCache.getOrRender(territoryKey, view.width(), view.height(), graphics -> { });
    }

    private static InterstellarMapPanel.RenderViewKey viewKey(
          int width, int height, double centerX, double centerY, double scale) {
        return InterstellarMapPanel.RenderViewKey.create(width, height, centerX, centerY, scale);
    }

    private static BufferedImage solidLayer(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    private static void drawLayer(BufferedImage destination, BufferedImage layer, double alpha) {
        Graphics2D graphics = destination.createGraphics();
        InterstellarMapPanel.drawRenderLayer(graphics, layer, alpha);
        graphics.dispose();
    }

    private static int alphaAt(BufferedImage image, int x) {
        return image.getRGB(x, 0) >>> 24;
    }
}
