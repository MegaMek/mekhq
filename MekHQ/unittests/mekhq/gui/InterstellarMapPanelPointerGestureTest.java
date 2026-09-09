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