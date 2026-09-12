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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.Test;

class InterstellarMapPanelAdministrativeBoundaryTest {
    private static final LocalDate DATE = LocalDate.of(3151, 1, 1);
    private static final double HEX_SIZE = 30.0;
    private static final double HEX_SPACING_X = HEX_SIZE * Math.sqrt(3) / 2.0;

    @Test
            void administrativeKeyUsesCommonPathForMatchingFactionEvidence() {
        Faction owner = faction("FS", Color.BLUE);
        Faction other = faction("DC", Color.RED);
        PlanetarySystem first = system(owner, List.of("Crucis March", "Coreward PDZ"));
        PlanetarySystem matching = system(owner, List.of("Crucis March", "Coreward PDZ"));
                        PlanetarySystem regionOnly = system(owner, List.of("Crucis March"));
                        PlanetarySystem anotherDistrict = system(owner, List.of("Crucis March", "Capellan PDZ"));
        PlanetarySystem missing = system(owner, List.of());
        PlanetarySystem conflicting = system(owner, List.of("Draconis March", "Robinson PDZ"));
        PlanetarySystem wrongOwner = system(other, List.of("Dieron District", "Algedi Prefecture"));

        InterstellarMapPanel.AdministrativeKey key = InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(first, matching), DATE);

        assertEquals(owner, key.faction());
        assertEquals(List.of("Crucis March", "Coreward PDZ"), key.path());
        assertEquals(List.of("Crucis March"), InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(first, regionOnly), DATE).path());
        assertEquals(List.of("Crucis March"), InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(first, anotherDistrict), DATE).path());
        assertNull(InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(first, conflicting), DATE));
        assertNull(InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(conflicting, first), DATE));
        assertNull(InterstellarMapPanel.classifyAdministrativeKey(List.of(owner), List.of(), DATE));
        assertNull(InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(missing, first), DATE));
        assertNull(InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(first, missing), DATE));
        assertNull(InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner), List.of(wrongOwner, first), DATE));
        assertNull(InterstellarMapPanel.classifyAdministrativeKey(
              List.of(owner, other), List.of(first), DATE));
    }

    @Test
      void sharedEdgesAndNationalPerimeterFormAdministrativeBoundariesWithoutUnknownInternalEdges() {
        Faction owner = faction("FS", new Color(70, 156, 220));
        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
      addCell(cells, 0, 0, owner, List.of("Crucis March", "Coreward PDZ"));
      addCell(cells, 1, 0, owner, List.of("Crucis March", "Capellan PDZ"));
      addCell(cells, 2, 0, owner, List.of("Draconis March", "Robinson PDZ"));
        addCell(cells, 3, 0, owner, null);

        List<InterstellarMapPanel.AdministrativeBoundary> boundaries =
              InterstellarMapPanel.buildAdministrativeBoundaries(cells);

        assertEquals(3, boundaries.size());
        assertEquals(2, boundaries.stream().filter(boundary ->
              boundary.level() == InterstellarMapPanel.AdministrativeBoundaryLevel.REGION).count());
        assertEquals(1, boundaries.stream().filter(boundary ->
              boundary.level() == InterstellarMapPanel.AdministrativeBoundaryLevel.DISTRICT).count());
    }

    @Test
      void neighboringNationsUseClosedOutlinesWithoutAnIntermediateAdministrativeEdge() {
            Faction owner = faction("FS", Color.YELLOW);
            Faction other = faction("CC", Color.GREEN);
            Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
            addCell(cells, 0, 0, owner, List.of("Crucis March", "Coreward PDZ"));
            addCell(cells, 1, 0, other, List.of("Sian Commonality", "Sian District"));

            List<InterstellarMapPanel.AdministrativeBoundary> boundaries =
                    InterstellarMapPanel.buildAdministrativeBoundaries(cells);
            List<InterstellarMapPanel.TerritoryContour> contours = InterstellarMapPanel.buildTerritoryContours(cells);

            assertEquals(2, boundaries.size());
            for (InterstellarMapPanel.AdministrativeBoundary boundary : boundaries) {
                  assertEquals(1, boundary.factions().size());
                  InterstellarMapPanel.TerritoryContour contour = contours.stream()
                          .filter(candidate -> candidate.factions().equals(boundary.factions()))
                          .findFirst().orElseThrow();
                  java.awt.geom.Area difference = new java.awt.geom.Area(boundary.shape());
                  difference.exclusiveOr(new java.awt.geom.Area(contour.shape()));
                  assertTrue(difference.isEmpty());
            }
      }

      @Test
    void bothBoundaryLevelsMeetExpandedNationalEdgesWithoutEnteringDisputedCells() {
        Faction owner = faction("FS", Color.YELLOW);
        Faction other = faction("CC", Color.GREEN);
        for (List<String> adjacent : List.of(List.of("Crucis March", "Capellan PDZ"),
              List.of("Draconis March", "Robinson PDZ"))) {
            Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
            addCell(cells, 0, 0, owner, List.of("Crucis March", "Coreward PDZ"));
            addCell(cells, 1, 0, owner, adjacent);
            addCell(cells, 0, 1, other, null);
            addCell(cells, 1, -1, other, null);
            InterstellarMapPanel.TerritoryHex disputedHex = new InterstellarMapPanel.TerritoryHex(1, -1);
            InterstellarMapPanel.TerritoryCell disputed = cells.get(disputedHex);
            cells.put(disputedHex, new InterstellarMapPanel.TerritoryCell(disputedHex,
                  disputed.centerX(), disputed.centerY(), List.of(owner, other), null));

            List<InterstellarMapPanel.AdministrativeBoundary> boundaries =
                  InterstellarMapPanel.buildAdministrativeBoundaries(cells);

            assertEquals(2, boundaries.size());
            assertEquals(-2.5, boundaries.getFirst().minMapY(), 0.0001);
            assertEquals(17.5, boundaries.getFirst().maxMapY(), 0.0001);
            InterstellarMapPanel.AdministrativeBoundary perimeter = boundaries.getLast();
            assertEquals(InterstellarMapPanel.AdministrativeBoundaryLevel.REGION, perimeter.level());
            assertTrue(perimeter.shape().contains(0.0, 0.0));
            assertTrue(perimeter.shape().contains(HEX_SPACING_X, HEX_SIZE / 2.0));
            assertFalse(perimeter.shape().contains(0.0, HEX_SIZE));
            assertFalse(perimeter.shape().contains(disputed.centerX(), disputed.centerY()));
            assertNull(cells.get(disputedHex).administration());
        }
    }

    @Test
    void unknownAdministrationWithinNationDoesNotExtendEitherBoundaryLevel() {
        Faction owner = faction("FS", Color.YELLOW);
        for (List<String> adjacent : List.of(List.of("Crucis March", "Capellan PDZ"),
              List.of("Draconis March", "Robinson PDZ"))) {
            Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
            addCell(cells, 0, 0, owner, List.of("Crucis March", "Coreward PDZ"));
            addCell(cells, 1, 0, owner, adjacent);
            addCell(cells, 0, 1, owner, null);
            addCell(cells, 1, -1, owner, null);

            List<InterstellarMapPanel.AdministrativeBoundary> boundaries =
                  InterstellarMapPanel.buildAdministrativeBoundaries(cells);

            assertEquals(2, boundaries.size());
            assertEquals(0.0, boundaries.getFirst().minMapY(), 0.0001);
            assertEquals(15.0, boundaries.getFirst().maxMapY(), 0.0001);
        }
    }

    @Test
    void singleAdministrationHasClosedNationalOutlineWithoutTerritoryFill() {
        Faction owner = faction("FS", Color.YELLOW);
        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
        addCell(cells, 0, 0, owner, List.of("Crucis March", "Coreward PDZ"));
        List<InterstellarMapPanel.AdministrativeBoundary> boundaries =
              InterstellarMapPanel.buildAdministrativeBoundaries(cells);
        InterstellarMapPanel.TerritoryContour national = InterstellarMapPanel.buildTerritoryContours(cells).getFirst();

        assertEquals(1, boundaries.size());
        java.awt.geom.Area difference = new java.awt.geom.Area(boundaries.getFirst().shape());
        difference.exclusiveOr(new java.awt.geom.Area(national.shape()));
        assertTrue(difference.isEmpty());
        assertTrue(boundaries.getFirst().shape().contains(0.0, 0.0));

        InterstellarMapPanel.TerritoryAtlas atlas = new InterstellarMapPanel.TerritoryAtlas(
              DATE, 0, 0, 0, 0, cells, List.of(), List.of(), boundaries);
        InterstellarMapPanel.RenderViewKey viewKey = InterstellarMapPanel.RenderViewKey.create(
              160, 100, 0.0, 0.0, 2.0);
        BufferedImage regions = render(atlas, viewKey,
              InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS);
        BufferedImage districts = render(atlas, viewKey,
              InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS_AND_DISTRICTS);
        assertTrue(paintedPixelCount(regions) > 0);
        assertEquals(imageSignature(regions), imageSignature(districts));
        assertEquals(0, regions.getRGB(80, 50));
    }

    @Test
    void nationWithoutKnownAdministrationHasNoAdministrativeOutline() {
        Faction owner = faction("FS", Color.YELLOW);
        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
        addCell(cells, 0, 0, owner, null);

        assertTrue(InterstellarMapPanel.buildAdministrativeBoundaries(cells).isEmpty());
    }

    @Test
    void detailSelectorIsCumulativeAndCachedOffscreenRenderingReusesPreparedGeometry() {
        assertTrue(InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS.includes(
              InterstellarMapPanel.AdministrativeBoundaryLevel.REGION));
        assertFalse(InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS.includes(
              InterstellarMapPanel.AdministrativeBoundaryLevel.DISTRICT));
        assertTrue(InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS_AND_DISTRICTS.includes(
              InterstellarMapPanel.AdministrativeBoundaryLevel.REGION));
        assertTrue(InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS_AND_DISTRICTS.includes(
              InterstellarMapPanel.AdministrativeBoundaryLevel.DISTRICT));

        Faction owner = faction("FS", new Color(70, 156, 220));
        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
      addCell(cells, 0, 0, owner, List.of("Crucis March", "Coreward PDZ"));
      addCell(cells, 1, 0, owner, List.of("Crucis March", "Capellan PDZ"));
      addCell(cells, 2, 0, owner, List.of("Draconis March", "Robinson PDZ"));
        List<InterstellarMapPanel.AdministrativeBoundary> boundaries =
              InterstellarMapPanel.buildAdministrativeBoundaries(cells);
        InterstellarMapPanel.TerritoryAtlas atlas = new InterstellarMapPanel.TerritoryAtlas(
              DATE, 0, 2, 0, 0, cells, List.of(), List.of(), boundaries);
        InterstellarMapPanel.RenderViewKey viewKey = InterstellarMapPanel.RenderViewKey.create(
              160, 100, 0.0, 0.0, 2.0);

        BufferedImage regions = render(atlas, viewKey,
              InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS);
        BufferedImage allBoundaries = render(atlas, viewKey,
              InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS_AND_DISTRICTS);
        assertNotEquals(imageSignature(regions), imageSignature(allBoundaries));
        assertTrue(paintedPixelCount(allBoundaries) > paintedPixelCount(regions));

        InterstellarMapPanel.PannableRenderLayerCache<InterstellarMapPanel.AdministrativeRenderKey> cache =
              new InterstellarMapPanel.PannableRenderLayerCache<>();
        InterstellarMapPanel.AdministrativeRenderKey key = new InterstellarMapPanel.AdministrativeRenderKey(
              new InterstellarMapPanel.TerritoryDataKey(DATE, 7L),
              InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS_AND_DISTRICTS);
        AtomicInteger rendererCalls = new AtomicInteger();
        InterstellarMapPanel.PannableRenderLayer first = cache.getOrRender(key, viewKey, 16, graphics -> {
            rendererCalls.incrementAndGet();
            InterstellarMapPanel.drawAdministrativeBoundaryLayer(graphics, atlas, viewKey, 16,
                  InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS_AND_DISTRICTS);
        });
        InterstellarMapPanel.PannableRenderLayer second = cache.getOrRender(key, viewKey, 16, graphics -> {
            rendererCalls.incrementAndGet();
            InterstellarMapPanel.drawAdministrativeBoundaryLayer(graphics, atlas, viewKey, 16,
                  InterstellarMapPanel.AdministrativeDisplayDetail.REGIONS_AND_DISTRICTS);
        });

        assertSame(first.image(), second.image());
        assertEquals(1, rendererCalls.get());
        assertEquals(1, cache.getReuseCount());
        assertTrue(paintedPixelCount(first.image()) > 0);
    }

    private static Faction faction(String shortName, Color color) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(shortName);
        when(faction.getColor()).thenReturn(color);
        return faction;
    }

    private static PlanetarySystem system(Faction faction, List<String> administration) {
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getFactionSet(DATE)).thenReturn(Set.of(faction));
        when(system.getAdministration(DATE)).thenReturn(administration);
        return system;
    }

    private static void addCell(
          Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells,
          int column, int row, Faction faction, List<String> administration) {
        InterstellarMapPanel.TerritoryHex hex = new InterstellarMapPanel.TerritoryHex(column, row);
        double centerX = column * HEX_SPACING_X;
        double centerY = row * HEX_SIZE + (column % 2) * HEX_SIZE / 2.0;
        InterstellarMapPanel.AdministrativeKey key = administration == null
              ? null
              : new InterstellarMapPanel.AdministrativeKey(faction, administration);
        cells.put(hex, new InterstellarMapPanel.TerritoryCell(
              hex, centerX, centerY, List.of(faction), key));
    }

    private static BufferedImage render(InterstellarMapPanel.TerritoryAtlas atlas,
          InterstellarMapPanel.RenderViewKey viewKey,
          InterstellarMapPanel.AdministrativeDisplayDetail detail) {
        BufferedImage image = new BufferedImage(viewKey.width(), viewKey.height(), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = image.createGraphics();
        InterstellarMapPanel.drawAdministrativeBoundaryLayer(graphics, atlas, viewKey, 0, detail);
        graphics.dispose();
        return image;
    }

    private static int paintedPixelCount(BufferedImage image) {
        int paintedPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    paintedPixels++;
                }
            }
        }
        return paintedPixels;
    }

    private static int imageSignature(BufferedImage image) {
        int signature = 1;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                signature = (31 * signature) + image.getRGB(x, y);
            }
        }
        return signature;
    }

    @Test
    void administrativeKeyRejectsAnEmptyPath() {
        assertThrows(IllegalArgumentException.class,
              () -> new InterstellarMapPanel.AdministrativeKey(faction("FS", Color.BLUE), List.of()));
    }

    @Test
    void prefixPathsDoNotCreateFalseDistrictBoundaries() {
        Faction owner = faction("LC", new Color(70, 156, 220));
        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
        addCell(cells, 0, 0, owner, List.of("Skye Province"));
        addCell(cells, 1, 0, owner, List.of("Skye Province", "Freedom Theater"));

        List<InterstellarMapPanel.AdministrativeBoundary> boundaries =
              InterstellarMapPanel.buildAdministrativeBoundaries(cells);

        assertEquals(1, boundaries.size());
        assertEquals(InterstellarMapPanel.AdministrativeBoundaryLevel.REGION, boundaries.getFirst().level());
    }
}
