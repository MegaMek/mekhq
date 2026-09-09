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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.CapitalType;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;

class InterstellarMapPanelCapitalMarkerTest {
    private static final Color TEST_FACTION_COLOR = new Color(220, 90, 70);

    @Test
    void factionPowerClassificationDoesNotChangeVisibilityOrSymbolSelection() {
        Faction major = faction(TEST_FACTION_COLOR, true, false);
        Faction clan = faction(TEST_FACTION_COLOR, false, true);
        Faction other = faction(TEST_FACTION_COLOR, false, false);

        BufferedImage majorMarker = renderCapital(major, 3.0);
        BufferedImage clanMarker = renderCapital(clan, 3.0);
        BufferedImage otherMarker = renderCapital(other, 3.0);

        assertTrue(nonTransparentPixelCount(majorMarker) > 0);
        assertTrue(Arrays.equals(pixels(majorMarker), pixels(clanMarker)));
        assertTrue(Arrays.equals(pixels(majorMarker), pixels(otherMarker)));
        for (Faction faction : new Faction[] { major, clan, other }) {
            verify(faction, never()).isMajorOrSuperPower();
            verify(faction, never()).isClan();
        }
    }

    @Test
    void unifiedMarkerPaintsVisiblyInDifferentFactionColors() {
        BufferedImage redMarker = renderCapital(faction(TEST_FACTION_COLOR, false, false), 7.5);
        Color blue = new Color(70, 145, 225);
        BufferedImage blueMarker = renderCapital(faction(blue, false, false), 7.5);

        assertTrue(exactColorPixelCount(redMarker, TEST_FACTION_COLOR) > 0);
        assertTrue(exactColorPixelCount(blueMarker, blue) > 0);
        assertFalse(Arrays.equals(pixels(redMarker), pixels(blueMarker)));
        assertTrue(darkPixelCount(redMarker) > 0, "the star needs a dark contrast backing");
    }

    @Test
        void datedCapitalResolverKeepsOwnersAndAddsOnlyMercenaryExceptionInShortNameOrder() {
          Faction capitalOwner = faction(TEST_FACTION_COLOR, false, false);
          when(capitalOwner.getShortName()).thenReturn("ZZ");
          Faction nonCapitalOwner = faction(TEST_FACTION_COLOR, false, false);
          when(nonCapitalOwner.getShortName()).thenReturn("AA");
        Faction mercenaries = faction(TEST_FACTION_COLOR, false, false);
        when(mercenaries.getShortName()).thenReturn("MERC");
          Faction mercenaryReviewBoard = faction(TEST_FACTION_COLOR, false, false);
          when(mercenaryReviewBoard.getShortName()).thenReturn("MRB");
          Map<Faction, String> capitals = Map.of(
              capitalOwner, "Galatea",
              nonCapitalOwner, "New Avalon",
              mercenaries, "Galatea",
              mercenaryReviewBoard, "Galatea");

          List<Faction> galateaCapitals = InterstellarMapPanel.resolveDatedCapitalFactions(
              Set.of(capitalOwner, nonCapitalOwner), capitals, "Galatea");

          assertEquals(List.of(mercenaries, capitalOwner), galateaCapitals);
          assertFalse(galateaCapitals.contains(nonCapitalOwner));
          assertFalse(galateaCapitals.contains(mercenaryReviewBoard));

          List<Faction> mercenaryOwnedCapital = InterstellarMapPanel.resolveDatedCapitalFactions(
              Set.of(capitalOwner, mercenaries), capitals, "Galatea");
          assertEquals(List.of(mercenaries, capitalOwner), mercenaryOwnedCapital);
          assertEquals(1, mercenaryOwnedCapital.stream().filter(faction -> faction == mercenaries).count());
    }

    @Test
    void preparedSystemRenderDataRetainsDatedCapitalResolution() {
        LocalDate date = LocalDate.of(3151, 4, 12);
        Faction capitalOwner = faction(TEST_FACTION_COLOR, false, false);
        when(capitalOwner.getShortName()).thenReturn("ZZ");
        Faction mercenaries = faction(TEST_FACTION_COLOR, false, false);
        when(mercenaries.getShortName()).thenReturn("MERC");
        PlanetarySystem system = mock(PlanetarySystem.class);
        when(system.getId()).thenReturn("Galatea");
        when(system.getFactionSet(date)).thenReturn(Set.of(capitalOwner));
        when(system.getCapitalType(date)).thenReturn(CapitalType.REGION);
        when(system.getHPG(date)).thenReturn(HPGRating.X);
        when(system.getPrintableName(date)).thenReturn("Galatea");

        InterstellarMapPanel.SystemRenderData renderData = InterstellarMapPanel.SystemRenderData.create(
              system, date, Map.of(capitalOwner, "Galatea", mercenaries, "Galatea"),
              mercenaries, "Galatea");

        assertEquals(List.of(mercenaries, capitalOwner), renderData.capitalFactions());
        assertEquals(CapitalType.REGION, renderData.capitalType());
    }

    @Test
    void allDatedCapitalsUseTheFactionStar() {
        Faction ordinaryFaction = faction(TEST_FACTION_COLOR, false, false);
        when(ordinaryFaction.getShortName()).thenReturn("FS");
        Faction mercenaries = faction(TEST_FACTION_COLOR, false, false);
        when(mercenaries.getShortName()).thenReturn("MERC");

        BufferedImage expectedStar = renderCapital(ordinaryFaction, 7.5);
        BufferedImage ordinaryCapital = renderDatedCapital(ordinaryFaction, "New Avalon", "New Avalon");
        BufferedImage mercenaryCapital = renderDatedCapital(mercenaries, "Galatea", "Galatea");

        assertTrue(Arrays.equals(pixels(expectedStar), pixels(ordinaryCapital)));
        assertTrue(Arrays.equals(pixels(expectedStar), pixels(mercenaryCapital)));
    }

    @Test
    void hierarchyUsesDistinctFilledShapesWithDecreasingComplexity() {
        BufferedImage national = renderHierarchyCapital(CapitalType.NATIONAL);
        BufferedImage region = renderHierarchyCapital(CapitalType.REGION);
        BufferedImage district = renderHierarchyCapital(CapitalType.DISTRICT);

        assertTrue(Arrays.equals(pixels(renderCapital(faction(TEST_FACTION_COLOR, false, false), 7.5)),
              pixels(national)));
        assertFalse(Arrays.equals(pixels(national), pixels(region)));
        assertFalse(Arrays.equals(pixels(region), pixels(district)));
            assertTrue((national.getRGB(24, 24) >>> 24) > 0);
            assertTrue((region.getRGB(24, 24) >>> 24) > 0);
            assertTrue((district.getRGB(24, 24) >>> 24) > 0);
        }

        @Test
        void capitalDetailIsCumulativeAndDistrictsRequireNavigationDetail() {
          assertTrue(InterstellarMapPanel.CapitalDisplayDetail.NATIONAL.includes(CapitalType.NATIONAL));
          assertFalse(InterstellarMapPanel.CapitalDisplayDetail.NATIONAL.includes(CapitalType.REGION));
          assertTrue(InterstellarMapPanel.CapitalDisplayDetail.NATIONAL_REGION.includes(CapitalType.REGION));
          assertFalse(InterstellarMapPanel.CapitalDisplayDetail.NATIONAL_REGION.includes(CapitalType.DISTRICT));
          assertTrue(InterstellarMapPanel.CapitalDisplayDetail.ALL.includes(CapitalType.DISTRICT));

          assertEquals(0.8, InterstellarMapPanel.capitalVisibilityAlpha(CapitalType.REGION,
              InterstellarMapPanel.CapitalDisplayDetail.NATIONAL_REGION, 0.8, 0.0));
          assertEquals(0.0, InterstellarMapPanel.capitalVisibilityAlpha(CapitalType.DISTRICT,
              InterstellarMapPanel.CapitalDisplayDetail.ALL, 0.8, 0.0));
          assertEquals(0.4, InterstellarMapPanel.capitalVisibilityAlpha(CapitalType.DISTRICT,
              InterstellarMapPanel.CapitalDisplayDetail.ALL, 0.8, 0.5));
    }

    private static Faction faction(Color color, boolean major, boolean clan) {
        Faction faction = mock(Faction.class);
        when(faction.getColor()).thenReturn(color);
        when(faction.isMajorOrSuperPower()).thenReturn(major);
        when(faction.isClan()).thenReturn(clan);
        return faction;
    }

    private static BufferedImage renderCapital(Faction faction, double markerSize) {
        BufferedImage canvas = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        InterstellarMapPanel.drawFactionCapitalMarker(graphics, new Point2D.Double(24, 24), markerSize,
              faction);
        graphics.dispose();
        return canvas;
    }

    private static BufferedImage renderDatedCapital(Faction faction, String systemId, String capitalSystemId) {
        BufferedImage canvas = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        InterstellarMapPanel.drawDatedCapitalMarker(graphics, new Point2D.Double(24, 24), 7.5,
              faction, systemId, capitalSystemId);
        graphics.dispose();
        return canvas;
    }

    private static BufferedImage renderHierarchyCapital(CapitalType capitalType) {
        BufferedImage canvas = new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        InterstellarMapPanel.drawCapitalHierarchyMarker(graphics, new Point2D.Double(24, 24), 7.5,
              capitalType, TEST_FACTION_COLOR);
        graphics.dispose();
        return canvas;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static int exactColorPixelCount(BufferedImage image, Color color) {
        return (int) Arrays.stream(pixels(image)).filter(pixel -> pixel == color.getRGB()).count();
    }

    private static int darkPixelCount(BufferedImage image) {
        return (int) Arrays.stream(pixels(image))
              .filter(pixel -> ((pixel >>> 24) > 0)
                    && (((pixel >>> 16) & 0xff) < 40)
                    && (((pixel >>> 8) & 0xff) < 40)
                    && ((pixel & 0xff) < 40))
              .count();
    }

    private static int nonTransparentPixelCount(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
