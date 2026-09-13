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
package mekhq.gui.baseComponents;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class FramedCommandButtonTest {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 44;

    @Test
    void framePaintsIdlePartialAndFullStates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FramedCommandButton button = createButton();
            int[] idlePixels = renderPixels(button);

            button.setFrameActiveForTest(true, 0);
            button.advanceFrameTransitionForTest(FramedCommandButton.FRAME_TRANSITION_DURATION_NANOS / 2);
            int[] partialPixels = renderPixels(button);

            button.advanceFrameTransitionForTest(FramedCommandButton.FRAME_TRANSITION_DURATION_NANOS);
            int[] fullPixels = renderPixels(button);

            assertEquals(pixelAt(idlePixels, 30, 0), pixelAt(idlePixels, 30, 3));
            assertNotEquals(pixelAt(partialPixels, 30, 0), pixelAt(partialPixels, 30, 3));
            assertEquals(pixelAt(partialPixels, WIDTH / 2, 0), pixelAt(partialPixels, WIDTH / 2, 3));
            assertNotEquals(pixelAt(fullPixels, WIDTH / 2, 0), pixelAt(fullPixels, WIDTH / 2, 3));
            assertFalse(java.util.Arrays.equals(idlePixels, partialPixels));
            assertFalse(java.util.Arrays.equals(partialPixels, fullPixels));
        });
    }

    @Test
    void frameDurationIsFixedAndReversesFromCurrentProgress() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FramedCommandButton button = createButton();
            long halfDuration = FramedCommandButton.FRAME_TRANSITION_DURATION_NANOS / 2;

            button.setFrameActiveForTest(true, 0);
            button.advanceFrameTransitionForTest(halfDuration);
            assertEquals(0.5, button.getFrameProgressForTest(), 0.001);

            button.setFrameActiveForTest(false, halfDuration);
            button.advanceFrameTransitionForTest(halfDuration + halfDuration / 2);
            assertEquals(0.25, button.getFrameProgressForTest(), 0.001);
            assertTrue(button.isFrameTransitionRunningForTest());

            button.advanceFrameTransitionForTest(FramedCommandButton.FRAME_TRANSITION_DURATION_NANOS);
            assertEquals(0.0, button.getFrameProgressForTest(), 0.001);
            assertFalse(button.isFrameTransitionRunningForTest());
            assertFalse(button.isAnimationTimerRepeatingForTest());
            assertEquals(320_000_000L, FramedCommandButton.FRAME_TRANSITION_DURATION_NANOS);
        });
    }

    @Test
    void focusDefaultAndPressedStatesRetainFiniteFrameGrammar() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FramedCommandButton button = createButton();
            int[] idlePixels = renderPixels(button);
            JRootPane rootPane = new JRootPane();
            rootPane.getContentPane().add(button);
            rootPane.setDefaultButton(button);

            fireFocusGained(button);
            int[] focusedPixels = renderPixels(button);
            assertFalse(java.util.Arrays.equals(idlePixels, focusedPixels));
            assertEquals(pixelAt(focusedPixels, WIDTH / 2, 0), pixelAt(focusedPixels, WIDTH / 2, 3));

            button.getModel().setArmed(true);
            button.getModel().setPressed(true);
            int[] pressedPixels = renderPixels(button);
            assertNotEquals(pixelAt(pressedPixels, WIDTH / 2, 0), pixelAt(pressedPixels, WIDTH / 2, 3));

            button.getModel().setPressed(false);
            button.getModel().setArmed(false);
            assertArrayEquals(focusedPixels, renderPixels(button));
        });
    }

    @Test
    void disabledAndRemovedButtonStopsCleanly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FramedCommandButton button = createButton();
            button.setFrameActiveForTest(true, 0);
            button.advanceFrameTransitionForTest(FramedCommandButton.FRAME_TRANSITION_DURATION_NANOS / 4);

            button.setEnabled(false);
            assertFalse(button.isFrameTransitionRunningForTest());
            assertFalse(button.isAnimationTimerRunningForTest());
            assertEquals(0.0, button.getFrameProgressForTest(), 0.001);

            button.setEnabled(true);
            button.setFrameActiveForTest(true, 0);
            button.removeNotify();
            assertFalse(button.isFrameTransitionRunningForTest());
            assertFalse(button.isAnimationTimerRunningForTest());
            assertEquals(0.0, button.getFrameProgressForTest(), 0.001);
        });
    }

    private static FramedCommandButton createButton() {
        FramedCommandButton button = new FramedCommandButton("Command");
        button.setSize(WIDTH, HEIGHT);
        return button;
    }

    private static int[] renderPixels(FramedCommandButton button) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        button.paint(graphics);
        graphics.dispose();
        return image.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
    }

    private static int pixelAt(int[] pixels, int x, int y) {
        return pixels[y * WIDTH + x];
    }

    private static void fireFocusGained(FramedCommandButton button) {
        FocusEvent event = new FocusEvent(button, FocusEvent.FOCUS_GAINED, false, null,
              FocusEvent.Cause.TRAVERSAL_FORWARD);
        for (FocusListener listener : button.getFocusListeners()) {
            listener.focusGained(event);
        }
    }
}
