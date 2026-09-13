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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;

import org.junit.jupiter.api.Test;

class InterstellarMapPanelPointerGestureTest {
    @Test
    void smallPointerMovementSelectsWithoutPanning() {
        var gesture = new InterstellarMapPanel.MapPointerGesture();
        gesture.press(new Point(100, 100));

        assertNull(gesture.drag(new Point(102, 101)));
        assertTrue(gesture.release(new Point(102, 101)));
        assertFalse(gesture.release(new Point(102, 101)));
    }

    @Test
    void dragUsesPressPositionAndDoesNotSelectEvenWhenReturningToStart() {
        var gesture = new InterstellarMapPanel.MapPointerGesture();
        gesture.press(new Point(100, 100));

        assertEquals(new Point(100, 50), gesture.drag(new Point(200, 150)));
        assertEquals(new Point(10, -5), gesture.drag(new Point(210, 145)));
        gesture.drag(new Point(100, 100));
        assertFalse(gesture.release(new Point(100, 100)));
    }

    @Test
    void newPressClearsPreviousDragAndDistantReleaseDoesNotSelect() {
        var gesture = new InterstellarMapPanel.MapPointerGesture();
        gesture.press(new Point(100, 100));
        gesture.drag(new Point(200, 200));
        gesture.press(new Point(20, 20));
        assertTrue(gesture.release(new Point(20, 20)));

        gesture.press(new Point(20, 20));
        assertFalse(gesture.release(new Point(200, 200)));
        assertNull(gesture.drag(new Point(20, 20)));
    }
}
