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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HPGRating;
import org.junit.jupiter.api.Test;

class InterstellarMapPanelOperationMarkerTest {
    private static final double DELTA = 0.000_001;
    private static final UUID FIRST_MISSION_ID = new UUID(0, 101);
    private static final UUID SECOND_MISSION_ID = new UUID(0, 102);

        @Test
        void hpgDetailTightensAtDistantZoomAndHonorsUserLimit() {
          assertEquals(InterstellarMapPanel.HpgNetworkDetail.CLASS_A,
              InterstellarMapPanel.effectiveHpgNetworkDetail(
                  InterstellarMapPanel.HpgNetworkDetail.ALL_STATIONS, 0.9, 3.0));
          assertEquals(InterstellarMapPanel.HpgNetworkDetail.CLASS_A_B,
              InterstellarMapPanel.effectiveHpgNetworkDetail(
                  InterstellarMapPanel.HpgNetworkDetail.ALL_STATIONS, 2.5, 3.0));
          assertEquals(InterstellarMapPanel.HpgNetworkDetail.ALL_STATIONS,
              InterstellarMapPanel.effectiveHpgNetworkDetail(
                  InterstellarMapPanel.HpgNetworkDetail.ALL_STATIONS, 3.2, 3.0));
          assertEquals(InterstellarMapPanel.HpgNetworkDetail.CLASS_A,
              InterstellarMapPanel.effectiveHpgNetworkDetail(
                  InterstellarMapPanel.HpgNetworkDetail.CLASS_A, 4.0, 3.0));
          assertTrue(InterstellarMapPanel.HpgNetworkDetail.CLASS_A.includes(HPGRating.A));
          assertFalse(InterstellarMapPanel.HpgNetworkDetail.CLASS_A.includes(HPGRating.B));
          assertTrue(InterstellarMapPanel.HpgNetworkDetail.CLASS_A_B.includes(HPGRating.B));
          assertTrue(InterstellarMapPanel.HpgNetworkDetail.ALL_STATIONS.includes(HPGRating.D));
          assertFalse(InterstellarMapPanel.HpgNetworkDetail.ALL_STATIONS.includes(HPGRating.X));
        }

    @Test
    void hpgBadgesReserveNavigationDetailForClassA() {
        assertEquals(0.6, InterstellarMapPanel.hpgStationMarkerAlpha(HPGRating.A, 0.6, 0.2));
        assertEquals(0.2, InterstellarMapPanel.hpgStationMarkerAlpha(HPGRating.B, 0.6, 0.2));
        assertTrue(InterstellarMapPanel.hpgStationMarkerRadius(10.0, HPGRating.A)
              > InterstellarMapPanel.hpgStationMarkerRadius(10.0, HPGRating.B));
    }

    @Test
    void hpgBadgeTextReusesFontAndPreservesGlyphCenter() {
        Font base = new Font(Font.DIALOG, Font.PLAIN, 12);
        FontRenderContext context = new FontRenderContext(new AffineTransform(), true, true);
        InterstellarMapPanel.HpgBadgeText text = InterstellarMapPanel.hpgBadgeText(base, context, 8.5, HPGRating.A);
        Font expectedFont = base.deriveFont(Font.BOLD, (float) (8.5 * 1.15));
        Rectangle2D expectedBounds = expectedFont.createGlyphVector(context, "A").getVisualBounds();

        assertSame(text, InterstellarMapPanel.hpgBadgeText(base, context, 8.5, HPGRating.A));
        assertEquals(expectedFont, text.font());
        assertEquals(expectedBounds.getCenterX(), text.centerX(), DELTA);
        assertEquals(expectedBounds.getCenterY(), text.centerY(), DELTA);
        assertNotSame(text, InterstellarMapPanel.hpgBadgeText(base, context, 8.5, HPGRating.B));
        assertNotSame(text, InterstellarMapPanel.hpgBadgeText(base, context, 6.0, HPGRating.A));
        assertNotSame(text, InterstellarMapPanel.hpgBadgeText(base.deriveFont(14.0f), context, 8.5, HPGRating.A));
        FontRenderContext scaledContext = new FontRenderContext(
              AffineTransform.getScaleInstance(2.0, 2.0), true, true);
        assertNotSame(text, InterstellarMapPanel.hpgBadgeText(base, scaledContext, 8.5, HPGRating.A));
    }

    @Test
    void semanticZoomKeepsOnlyUrgentOperationsVisibleAtAtlasZoom() {
        InterstellarMapPanel.SemanticZoomProfile atlas =
              InterstellarMapPanel.SemanticZoomProfile.create(0.9, 3.0);
        InterstellarMapPanel.SemanticZoomProfile navigation =
              InterstellarMapPanel.SemanticZoomProfile.create(2.5, 3.0);
        InterstellarMapPanel.StrategicMarker missionOnly = new InterstellarMapPanel.StrategicMarker(1, 0);
        InterstellarMapPanel.StrategicMarker urgent = new InterstellarMapPanel.StrategicMarker(1, 1);

        assertEquals(0.0, atlas.missionOperationAlpha(), DELTA);
        assertTrue(atlas.urgentOperationAlpha() > 0.0);
        assertTrue(navigation.missionOperationAlpha() > 0.0);
        assertTrue(navigation.urgentOperationAlpha() >= navigation.missionOperationAlpha());
        assertEquals(0.0, InterstellarMapPanel.visibleOperationAlpha(1.0, atlas, missionOnly), DELTA);
        assertEquals(atlas.urgentOperationAlpha(),
              InterstellarMapPanel.visibleOperationAlpha(1.0, atlas, urgent), DELTA);
        assertEquals(0.0, InterstellarMapPanel.visibleOperationAlpha(0.0, atlas, urgent), DELTA,
              "the operations layer toggle must still suppress urgent markers");
        assertEquals(atlas, InterstellarMapPanel.SemanticZoomProfile.create(0.9, 3.0));
    }

        @Test
        void semanticZoomKeepsMediumViewCompactUntilDetailZoom() {
          InterstellarMapPanel.SemanticZoomProfile atlas =
              InterstellarMapPanel.SemanticZoomProfile.create(0.9, 3.0);
          InterstellarMapPanel.SemanticZoomProfile navigation =
              InterstellarMapPanel.SemanticZoomProfile.create(3.0, 3.0);
          InterstellarMapPanel.SemanticZoomProfile transition =
              InterstellarMapPanel.SemanticZoomProfile.create(4.0, 3.0);
          InterstellarMapPanel.SemanticZoomProfile detail =
              InterstellarMapPanel.SemanticZoomProfile.create(5.0, 3.0);

        assertEquals(1.0, atlas.systemContactAlpha(), DELTA);
          assertEquals(0.0, atlas.systemDetailAlpha(), DELTA);
                    assertEquals(0.0, navigation.systemDetailAlpha(), DELTA,
                          "capital and operation symbols remain compact while strategic contacts are active");
                      assertEquals(navigation.systemDetailAlpha(), navigation.currentLocationAlpha(), DELTA,
                          "the ship appears with detailed system art");
                      assertEquals(1.0 - navigation.systemDetailAlpha(), navigation.strategicContactAlpha(), DELTA,
                          "the centered fleet beacon remains until the ship appears");
                      assertEquals(1.0, navigation.detailedOverlayAlpha(), DELTA,
                          "general overlays may expand before system-attached markers");
          assertEquals(0.0, navigation.ordinaryLabelAlpha(), DELTA);
          assertEquals(1.0, navigation.routeLabelAlpha(), DELTA,
              "route labels must remain independent from ordinary labels");
        assertTrue(transition.systemContactAlpha() > 0.0);
          assertTrue(transition.systemDetailAlpha() > 0.0);
          assertTrue(transition.systemDetailAlpha() < 1.0);
          assertTrue(transition.ordinaryLabelAlpha() > 0.0);
        assertEquals(0.0, detail.systemContactAlpha(), DELTA);
          assertEquals(1.0, detail.systemDetailAlpha(), DELTA);
          assertEquals(1.0, detail.ordinaryLabelAlpha(), DELTA);
        }

    @Test
    void urgentMarkerIsDeterministicAndHasMateriallyStrongerFilledArea() {
        BufferedImage missionOnly = render(new InterstellarMapPanel.StrategicMarker(1, 0));
        BufferedImage urgent = render(new InterstellarMapPanel.StrategicMarker(1, 1));

        int missionPixels = nonTransparentPixelCount(missionOnly);
        int urgentPixels = nonTransparentPixelCount(urgent);
        assertTrue(missionPixels > 0);
        assertTrue(urgentPixels > 0);
        assertFalse(Arrays.equals(pixels(missionOnly), pixels(urgent)));
        assertTrue(urgentPixels >= missionPixels * 1.2,
              "the filled urgent flag should occupy materially more pixels than the outline flag");
        assertArrayEquals(pixels(urgent), pixels(render(new InterstellarMapPanel.StrategicMarker(1, 1))));
    }

    @Test
    void aggregationCountsMissionsOnceAndNeverInflatesTheBadgeWithScenarios() {
        AbstractContract firstMission = missionAt("system-a");
        AbstractContract secondMission = missionAt("system-a");
        Scenario firstScenario = scenarioFor(FIRST_MISSION_ID);
        Scenario secondScenario = scenarioFor(FIRST_MISSION_ID);
        Scenario thirdScenario = scenarioFor(SECOND_MISSION_ID);

        Map<String, InterstellarMapPanel.StrategicMarker> markers =
              InterstellarMapPanel.buildStrategicMarkers(
                    List.of(firstMission, secondMission),
                    List.of(firstScenario, secondScenario, thirdScenario),
                    missionId -> FIRST_MISSION_ID.equals(missionId) ? firstMission : secondMission);

        assertEquals(1, markers.size(), "systems receive one flag regardless of operation count");
        assertEquals(new InterstellarMapPanel.StrategicMarker(2, 3), markers.get("system-a"));
    }

    @Test
    void presentationSnapshotOwnsAggregatedPaintInputs() {
        AbstractContract mission = missionAt("system-a");
        Faction employer = mock(Faction.class);
        Faction target = mock(Faction.class);
        when(mission.getEmployerFaction()).thenReturn(employer);
        when(mission.getEnemyFaction()).thenReturn(target);
        Scenario scenario = scenarioFor(FIRST_MISSION_ID);
        Map<String, Integer> baseCounts = new HashMap<>(Map.of("system-a", 2));

        InterstellarMapPanel.MapPresentationData presentationData =
              InterstellarMapPanel.createMapPresentationData(List.of(mission), List.of(scenario),
                    missionId -> mission, baseCounts);
        baseCounts.clear();

        assertEquals(new InterstellarMapPanel.StrategicMarker(1, 1),
              presentationData.strategicMarkers().get("system-a"));
        assertEquals(2, presentationData.playerBaseCounts().get("system-a"));
        assertEquals(Set.of(employer), presentationData.contractEmployers());
        assertEquals(Set.of(target), presentationData.contractTargets());
    }

    @Test
    void scenarioOnlyFallbackIsUrgentWithoutAZeroCountBadge() {
        AbstractContract resolvedMission = missionAt("system-a");
        Scenario scenario = scenarioFor(FIRST_MISSION_ID);

        InterstellarMapPanel.StrategicMarker marker = InterstellarMapPanel.buildStrategicMarkers(
              List.of(), List.of(scenario), missionId -> resolvedMission).get("system-a");

        assertEquals(new InterstellarMapPanel.StrategicMarker(0, 1), marker);
        assertTrue(marker.hasActiveScenario());
        assertArrayEquals(pixels(render(new InterstellarMapPanel.StrategicMarker(1, 1))),
              pixels(render(marker)), "zero and one mission should both omit a count badge");
        assertFalse(Arrays.equals(pixels(render(marker)),
              pixels(render(new InterstellarMapPanel.StrategicMarker(2, 1)))));
    }

    private static AbstractContract missionAt(String systemId) {
        AbstractContract mission = mock(AbstractContract.class, RETURNS_DEEP_STUBS);
        when(mission.getTargetSystem().getId()).thenReturn(systemId);
        return mission;
    }

    private static Scenario scenarioFor(UUID missionId) {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getMissionId()).thenReturn(missionId);
        return scenario;
    }

    private static BufferedImage render(InterstellarMapPanel.StrategicMarker marker) {
        BufferedImage canvas = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = canvas.createGraphics();
        InterstellarMapPanel.SystemMarkerLayout layout = InterstellarMapPanel.SystemMarkerLayout.create(
              70, 70, 12, InterstellarMapPanel.RouteMarkerState.NONE, false, false);
        InterstellarMapPanel.drawOperationMarker(graphics, layout, marker);
        graphics.dispose();
        return canvas;
    }

    private static int[] pixels(BufferedImage image) {
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    private static int nonTransparentPixelCount(BufferedImage image) {
        return (int) Arrays.stream(pixels(image)).filter(pixel -> (pixel >>> 24) > 0).count();
    }
}
