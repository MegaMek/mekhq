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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;

class InterstellarMapPanelTerritoryContourTest {
    private static final double HEX_SIZE = 30.0;
    private static final double HEX_SPACING_X = HEX_SIZE * Math.sqrt(3) / 2.0;
      private static final int IMAGE_SIZE = 72;

    @Test
    void adjacentCellsWithMatchingOwnershipBecomeOneSmoothCachedContour() {
        Faction faction = faction("FS");
        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
        addCell(cells, 0, 0, List.of(faction));
        addCell(cells, 1, 0, List.of(faction));

        List<InterstellarMapPanel.TerritoryContour> contours =
              InterstellarMapPanel.buildTerritoryContours(cells);

        assertEquals(1, contours.size());
        InterstellarMapPanel.TerritoryContour contour = contours.getFirst();
        assertEquals(List.of(faction), contour.factions());
      assertEquals(InterstellarMapPanel.TerritorySemantic.SOVEREIGN, contour.semantic());
        assertEquals(2, contour.cellCount());
        assertTrue(contour.shape().contains(HEX_SPACING_X / 2.0, HEX_SIZE / 4.0),
              "the shared edge must be inside one continuous region");
        assertTrue(hasCurvedClosedBoundary(contour.shape()),
              "the cached outer contour must contain a curved, closed boundary");

        InterstellarMapPanel.TerritoryAtlas atlas = new InterstellarMapPanel.TerritoryAtlas(
              LocalDate.of(3151, 1, 1), 0, 1, 0, 0, cells, contours, List.of());
        assertSame(contour.shape(), atlas.contours().getFirst().shape(),
              "the atlas must retain reusable map-space geometry");
    }

    @Test
    void ownershipCombinationsRemainDistinctAndDeterministic() {
        Faction factionA = faction("A");
        Faction factionB = faction("B");
        Faction independent = faction("IND");
        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
        addCell(cells, 0, 0, List.of(factionA));
        addCell(cells, 1, 0, List.of(factionA));
        addCell(cells, 2, 0, List.of(factionA, factionB));
        addCell(cells, 3, 0, List.of(factionA, factionB));
        addCell(cells, 4, 0, List.of(independent));
        addCell(cells, 5, 0, List.of());

        List<InterstellarMapPanel.TerritoryContour> contours =
              InterstellarMapPanel.buildTerritoryContours(cells);
        Map<List<Faction>, Integer> cellsByOwnership = contours.stream()
              .collect(Collectors.toMap(InterstellarMapPanel.TerritoryContour::factions,
                    InterstellarMapPanel.TerritoryContour::cellCount));

        assertEquals(Set.of(List.of(factionA), List.of(factionA, factionB), List.of(independent), List.of()),
              cellsByOwnership.keySet());
        assertEquals(2, cellsByOwnership.get(List.of(factionA)));
        assertEquals(2, cellsByOwnership.get(List.of(factionA, factionB)));
        assertEquals(1, cellsByOwnership.get(List.of(independent)));
        assertEquals(1, cellsByOwnership.get(List.of()));
    }

    @Test
    void contourSemanticsFollowOwnershipConnectivityAndAtlasEdge() {
        Faction owner = faction("A");
        Faction surroundingOwner = faction("B");

        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> exterior = new HashMap<>();
        addCell(exterior, 0, 0, List.of());
        assertEquals(InterstellarMapPanel.TerritorySemantic.UNCLAIMED_EXTERIOR,
              contourFor(exterior, List.of()).semantic());

        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> pocket =
              ringAround(List.of(), List.of(surroundingOwner));
        assertEquals(InterstellarMapPanel.TerritorySemantic.UNCLAIMED_POCKET,
              contourFor(pocket, List.of()).semantic());

        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> enclave =
              ringAround(List.of(owner), List.of(surroundingOwner));
        assertEquals(InterstellarMapPanel.TerritorySemantic.ENCLAVE,
              contourFor(enclave, List.of(owner)).semantic());

        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> disputed = new HashMap<>();
        addCell(disputed, 0, 0, List.of(owner, surroundingOwner));
        assertEquals(InterstellarMapPanel.TerritorySemantic.DISPUTED,
              contourFor(disputed, List.of(owner, surroundingOwner)).semantic());

        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> sovereign = new HashMap<>();
        addCell(sovereign, 0, 0, List.of(owner));
        assertEquals(InterstellarMapPanel.TerritorySemantic.SOVEREIGN,
              contourFor(sovereign, List.of(owner)).semantic());
    }

    @Test
    void islandsAndMixedSurroundingsAreNotEnclaves() {
        Faction owner = faction("A");
        Faction surroundingOwner = faction("B");
        Faction otherOwner = faction("C");

        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> island =
              ringAround(List.of(owner), List.of());
        assertEquals(InterstellarMapPanel.TerritorySemantic.SOVEREIGN,
              contourFor(island, List.of(owner)).semantic());

        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> mixed =
              ringAround(List.of(owner), List.of(surroundingOwner));
        addCell(mixed, 0, -1, List.of(otherOwner));
        assertEquals(InterstellarMapPanel.TerritorySemantic.SOVEREIGN,
              contourFor(mixed, List.of(owner)).semantic());
        assertFalse(InterstellarMapPanel.buildTerritoryContours(mixed).stream()
              .filter(contour -> contour.factions().equals(List.of(owner)))
              .anyMatch(contour -> contour.semantic() == InterstellarMapPanel.TerritorySemantic.ENCLAVE));
    }

    @Test
    void visualProfileSuppressesSecondaryDetailAtAtlasZoomAndRevealsItAtDetailZoom() {
        InterstellarMapPanel.TerritoryVisualProfile atlas =
              InterstellarMapPanel.TerritoryVisualProfile.create(1.0);
        InterstellarMapPanel.TerritoryVisualProfile detail =
              InterstellarMapPanel.TerritoryVisualProfile.create(4.0);

        assertEquals(0.0, atlas.secondaryDetailAlpha());
        assertEquals(1.0, detail.secondaryDetailAlpha());
    }

    @Test
    void exteriorIsOmittedAndVisibleSemanticStylesRemainDistinct() {
        Faction factionA = faction("A", new Color(218, 92, 72));
        Faction factionB = faction("B", new Color(70, 156, 220));
        InterstellarMapPanel.TerritoryVisualProfile detail =
              InterstellarMapPanel.TerritoryVisualProfile.create(4.0);

        BufferedImage exterior = render(semanticContour(
              InterstellarMapPanel.TerritorySemantic.UNCLAIMED_EXTERIOR,
              List.of(), Color.BLACK), detail);
        assertEquals(0, paintedPixelCount(exterior));

        List<BufferedImage> visibleStyles = List.of(
              render(semanticContour(InterstellarMapPanel.TerritorySemantic.SOVEREIGN,
                    List.of(factionA), new Color(218, 92, 72, 90)), detail),
              render(semanticContour(InterstellarMapPanel.TerritorySemantic.DISPUTED,
                    List.of(factionA, factionB), new GradientPaint(12, 12,
                          new Color(218, 92, 72, 90), 60, 60,
                          new Color(70, 156, 220, 90), true)), detail),
              render(semanticContour(InterstellarMapPanel.TerritorySemantic.UNCLAIMED_POCKET,
                    List.of(), Color.BLACK), detail),
              render(semanticContour(InterstellarMapPanel.TerritorySemantic.ENCLAVE,
                    List.of(factionA), new Color(218, 92, 72, 90)), detail));

        assertTrue(visibleStyles.stream().allMatch(image -> paintedPixelCount(image) > 0));
        assertEquals(visibleStyles.size(), visibleStyles.stream().mapToInt(
              InterstellarMapPanelTerritoryContourTest::imageSignature).distinct().count());
    }

    @Test
    void detailZoomAddsHatchingAndPocketDots() {
        Faction factionA = faction("A", new Color(218, 92, 72));
        Faction factionB = faction("B", new Color(70, 156, 220));
        InterstellarMapPanel.TerritoryContour disputed = semanticContour(
              InterstellarMapPanel.TerritorySemantic.DISPUTED, List.of(factionA, factionB),
              new Color(120, 120, 140, 90));
        InterstellarMapPanel.TerritoryContour pocket = semanticContour(
              InterstellarMapPanel.TerritorySemantic.UNCLAIMED_POCKET, List.of(), Color.BLACK);

        InterstellarMapPanel.TerritoryVisualProfile atlas =
              InterstellarMapPanel.TerritoryVisualProfile.create(1.0);
        InterstellarMapPanel.TerritoryVisualProfile detail =
              InterstellarMapPanel.TerritoryVisualProfile.create(4.0);
        assertNotEquals(imageSignature(render(disputed, atlas)), imageSignature(render(disputed, detail)));
        assertNotEquals(imageSignature(render(pocket, atlas)), imageSignature(render(pocket, detail)));
    }

    private static Faction faction(String shortName) {
        Faction faction = mock(Faction.class);
        when(faction.getShortName()).thenReturn(shortName);
            when(faction.getColor()).thenReturn(Color.GRAY);
        return faction;
    }

      private static Faction faction(String shortName, Color color) {
            Faction faction = mock(Faction.class);
            when(faction.getShortName()).thenReturn(shortName);
            when(faction.getColor()).thenReturn(color);
            return faction;
      }

    private static void addCell(
          Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells,
          int column, int row, List<Faction> factions) {
        InterstellarMapPanel.TerritoryHex hex = new InterstellarMapPanel.TerritoryHex(column, row);
        double centerX = column * HEX_SPACING_X;
        double centerY = row * HEX_SIZE + (column % 2) * HEX_SIZE / 2.0;
        cells.put(hex, new InterstellarMapPanel.TerritoryCell(hex, centerX, centerY, factions));
    }

            private static Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> ringAround(
                              List<Faction> centerFactions, List<Faction> ringFactions) {
                        Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells = new HashMap<>();
                        addCell(cells, 0, 0, centerFactions);
                        addCell(cells, -1, 0, ringFactions);
                        addCell(cells, -1, 1, ringFactions);
                        addCell(cells, 0, -1, ringFactions);
                        addCell(cells, 0, 1, ringFactions);
                        addCell(cells, 1, -1, ringFactions);
                        addCell(cells, 1, 0, ringFactions);
                        return cells;
            }

            private static InterstellarMapPanel.TerritoryContour contourFor(
                              Map<InterstellarMapPanel.TerritoryHex, InterstellarMapPanel.TerritoryCell> cells,
                              List<Faction> factions) {
                        return InterstellarMapPanel.buildTerritoryContours(cells).stream()
                                          .filter(contour -> contour.factions().equals(factions))
                                          .findFirst()
                                          .orElseThrow();
            }

                              private static InterstellarMapPanel.TerritoryContour semanticContour(
                                      InterstellarMapPanel.TerritorySemantic semantic, List<Faction> factions, Paint paint) {
                                    Area shape = new Area(new Ellipse2D.Double(12, 12, 48, 48));
                                    return new InterstellarMapPanel.TerritoryContour(factions, semantic, shape, paint, 1,
                                            12, 60, 12, 60);
                              }

                              private static BufferedImage render(InterstellarMapPanel.TerritoryContour contour,
                                      InterstellarMapPanel.TerritoryVisualProfile profile) {
                                    BufferedImage image = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
                                    Graphics2D graphics = image.createGraphics();
                                    InterstellarMapPanel.paintTerritoryContour(graphics, contour, new AffineTransform(), profile);
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

    private static boolean hasCurvedClosedBoundary(Shape shape) {
        PathIterator iterator = shape.getPathIterator(null);
        double[] coordinates = new double[6];
        boolean hasCurve = false;
        boolean hasClose = false;
        while (!iterator.isDone()) {
            int segment = iterator.currentSegment(coordinates);
            hasCurve |= (segment == PathIterator.SEG_QUADTO) || (segment == PathIterator.SEG_CUBICTO);
            hasClose |= segment == PathIterator.SEG_CLOSE;
            iterator.next();
        }
        return hasCurve && hasClose;
    }
}
