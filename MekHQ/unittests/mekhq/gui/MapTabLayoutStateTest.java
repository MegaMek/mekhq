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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class MapTabLayoutStateTest {
    @Test
    void dossierViewReturnsToTopAfterDeferredLayoutAdjustment() throws Exception {
        AtomicReference<JScrollPane> scrollPaneReference = new AtomicReference<>();
        AtomicReference<Point> viewPositionReference = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setSize(120, 100);
            JPanel dossier = new JPanel();
            dossier.setPreferredSize(new Dimension(100, 500));
            MapTab.setViewportViewAtTop(scrollPane, dossier);
            scrollPane.doLayout();
            scrollPane.getViewport().setViewPosition(new Point(0, 120));
            scrollPaneReference.set(scrollPane);
        });
        SwingUtilities.invokeAndWait(() -> viewPositionReference.set(
              scrollPaneReference.get().getViewport().getViewPosition()));

        assertEquals(new Point(0, 0), viewPositionReference.get());
    }

    @Test
    void dossierRevealRunsForFirstAndChangedSelectionsOnly() {
        MapTab.DossierIdentity first = new MapTab.DossierIdentity("first", 0);
        MapTab.DossierIdentity second = new MapTab.DossierIdentity("second", 0);

        assertTrue(MapTab.shouldAnimateDossierReveal(null, first));
        assertTrue(MapTab.shouldAnimateDossierReveal(first, second));
        assertFalse(MapTab.shouldAnimateDossierReveal(first, first));
    }

    @Test
    void initializesAbsentLayoutStateWithoutReplacingExistingState() {
        MapTabLayoutState initializedState = MapTab.initializeLayoutState(null);

        assertNotNull(initializedState);
        assertSame(initializedState, MapTab.initializeLayoutState(initializedState));
    }

    @Test
    void startsWithContextInspectorExpanded() {
        MapTabLayoutState state = new MapTabLayoutState();

        assertTrue(state.isContextInspectorExpanded());
    }

    @Test
    void contextInspectorCanBeCollapsedAndExpanded() {
        MapTabLayoutState state = new MapTabLayoutState();

        state.toggleContextInspector();
        assertFalse(state.isContextInspectorExpanded());

        state.toggleContextInspector();
        assertTrue(state.isContextInspectorExpanded());
    }

    @Test
    void explicitRevealExpandsCollapsedInspector() {
        MapTabLayoutState state = new MapTabLayoutState();
        state.toggleContextInspector();

        state.revealContextInspector();

        assertTrue(state.isContextInspectorExpanded());
    }

    @Test
    void repeatedLayoutChangesRestoreTheExactWorldCenter() {
        InterstellarMapPanel.MapCenter expectedCenter = new InterstellarMapPanel.MapCenter(42.5, -17.25);
        AtomicReference<InterstellarMapPanel.MapCenter> camera = new AtomicReference<>(expectedCenter);

        for (int toggle = 0; toggle < 20; toggle++) {
            MapTabLayoutState.preserveViewportCenter(camera::get, camera::set,
                  () -> camera.set(new InterstellarMapPanel.MapCenter(-100.0, 100.0)));
            assertEquals(expectedCenter, camera.get());
        }
    }
}
