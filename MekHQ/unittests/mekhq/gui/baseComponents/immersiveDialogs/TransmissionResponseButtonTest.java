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
package mekhq.gui.baseComponents.immersiveDialogs;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class TransmissionResponseButtonTest {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 44;

    @Test
    void scanOverlayAppearsDuringAnimationAndClearsOnCompletion() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            int[] idlePixels = renderPixels(button);

            button.startScan();
            int[] scanningPixels = renderPixels(button);
            assertTrue(button.isScanRunning());
            assertFalse(java.util.Arrays.equals(idlePixels, scanningPixels));

            button.completeScan();
            assertFalse(button.isScanRunning());
            assertArrayEquals(idlePixels, renderPixels(button));
        });
    }

    @Test
    void disabledButtonDoesNotStartScan() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            button.setEnabled(false);

            button.startScan();

            assertFalse(button.isScanRunning());
        });
    }

    @Test
    void hoverAndKeyboardTraversalStartScanButWindowActivationDoesNot() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();

            MouseEvent mouseEntered = new MouseEvent(button,
                  MouseEvent.MOUSE_ENTERED,
                  System.currentTimeMillis(),
                  0,
                  2,
                  2,
                  0,
                  false);
            for (MouseListener listener : button.getMouseListeners()) {
                listener.mouseEntered(mouseEntered);
            }
            assertTrue(button.isScanRunning());

            button.completeScan();
            button.getModel().setRollover(true);
            button.startScan(true, 0);
            button.advanceScan(button.getScanPassDurationNanos() * 3 / 2);
            assertTrue(button.isScanRunning());
            assertFalse(button.isScanMovingForward());
            assertEquals(0.5, button.getScanProgress(), 0.001);

            MouseEvent mouseExited = new MouseEvent(button,
                  MouseEvent.MOUSE_EXITED,
                  System.currentTimeMillis(),
                  0,
                  WIDTH + 1,
                  HEIGHT + 1,
                  0,
                  false);
            for (MouseListener listener : button.getMouseListeners()) {
                listener.mouseExited(mouseExited);
            }
            assertFalse(button.isScanRunning());

            fireFocusGained(button, FocusEvent.Cause.ACTIVATION);
            assertFalse(button.isScanRunning());

            fireFocusGained(button, FocusEvent.Cause.TRAVERSAL_FORWARD);
            assertTrue(button.isScanRunning());
            button.advanceScan(Long.MAX_VALUE);
            assertFalse(button.isScanRunning());
        });
    }

    @Test
    void scanUsesTheSamePixelVelocityForDifferentButtonWidths() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton shortButton = createButton();
            TransmissionResponseButton longButton = createButton();
            longButton.setSize(WIDTH * 2, HEIGHT);

            assertEquals(TransmissionResponseButton.SCAN_NANOS_PER_PIXEL,
                  shortButton.getScanPassDurationNanos() / shortButton.getScanTravelDistance());
            assertEquals(TransmissionResponseButton.SCAN_NANOS_PER_PIXEL,
                  longButton.getScanPassDurationNanos() / longButton.getScanTravelDistance());
            assertTrue(longButton.getScanPassDurationNanos() > shortButton.getScanPassDurationNanos());
        });
    }

    @Test
    void framePaintsIdlePartialAndFullStates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createFrameButton(ResponseButtonMotion.MEKBAY_REFERENCE);
            int[] idlePixels = renderPixels(button);

            button.setFrameActive(true, 0);
            button.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 2);
            int[] partialPixels = renderPixels(button);

            button.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS);
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
    void frameDurationIsFixedIndependentOfButtonWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton shortButton = createFrameButton(ResponseButtonMotion.MEKBAY_REFERENCE);
            TransmissionResponseButton longButton = createFrameButton(ResponseButtonMotion.MEKBAY_REFERENCE);
            longButton.setSize(WIDTH * 2, HEIGHT);

            shortButton.setFrameActive(true, 0);
            longButton.setFrameActive(true, 0);
            shortButton.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 2);
            longButton.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 2);

            assertEquals(shortButton.getFrameProgress(), longButton.getFrameProgress(), 0.001);
            assertEquals(0.5, shortButton.getFrameProgress(), 0.001);
            assertFalse(shortButton.isAnimationTimerRepeating());
        });
    }

    @Test
    void frameReversesFromCurrentProgress() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createFrameButton(ResponseButtonMotion.MEKBAY_REFERENCE);
            long halfDuration = TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 2;

            button.setFrameActive(true, 0);
            button.advanceFrameTransition(halfDuration);
            assertEquals(0.5, button.getFrameProgress(), 0.001);

            button.setFrameActive(false, halfDuration);
            assertEquals(0.5, button.getFrameProgress(), 0.001);
            button.advanceFrameTransition(halfDuration + halfDuration / 2);
            assertEquals(0.25, button.getFrameProgress(), 0.001);
            assertTrue(button.isFrameTransitionRunning());

            button.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS);
            assertEquals(0.0, button.getFrameProgress(), 0.001);
            assertFalse(button.isFrameTransitionRunning());
        });
    }

    @Test
    void frameRespondsToWindowActivationAndHoldsForFocusOrHover() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createFrameButton(ResponseButtonMotion.MEKBAY_REFERENCE);

            fireFocusGained(button, FocusEvent.Cause.ACTIVATION);
            assertTrue(button.isFrameTransitionRunning());
            button.advanceFrameTransition(Long.MAX_VALUE);
            assertEquals(1.0, button.getFrameProgress(), 0.001);

            fireFocusLost(button);
            assertTrue(button.isFrameTransitionRunning());
            button.advanceFrameTransition(Long.MAX_VALUE);
            assertEquals(0.0, button.getFrameProgress(), 0.001);

            fireMouseEntered(button);
            button.advanceFrameTransition(Long.MAX_VALUE);
            fireFocusGained(button, FocusEvent.Cause.TRAVERSAL_FORWARD);
            fireMouseExited(button);
            assertFalse(button.isFrameTransitionRunning());
            assertEquals(1.0, button.getFrameProgress(), 0.001);

            fireFocusLost(button);
            assertTrue(button.isFrameTransitionRunning());
            button.advanceFrameTransition(Long.MAX_VALUE);
            assertEquals(0.0, button.getFrameProgress(), 0.001);
        });
    }

    @Test
    void disabledAndRemovedFrameButtonStopsCleanly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createFrameButton(ResponseButtonMotion.MEKBAY_REFERENCE);
            button.setFrameActive(true, 0);
            button.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 4);

            button.setEnabled(false);

            assertFalse(button.isFrameTransitionRunning());
            assertFalse(button.isAnimationTimerRunning());
            assertEquals(0.0, button.getFrameProgress(), 0.001);

            button.setEnabled(true);
            button.setFrameActive(true, 0);
            button.removeNotify();
            assertFalse(button.isFrameTransitionRunning());
            assertFalse(button.isAnimationTimerRunning());
            assertEquals(0.0, button.getFrameProgress(), 0.001);
        });
    }

    @Test
    void frameStylesUseDistinctNeutralAndSignalColors() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton mekBayButton = createFrameButton(ResponseButtonMotion.MEKBAY_REFERENCE);
            TransmissionResponseButton mekHQButton = createFrameButton(ResponseButtonMotion.MEKHQ_SIGNAL);

            int[] mekBayIdlePixels = renderPixels(mekBayButton);
            int[] mekHQIdlePixels = renderPixels(mekHQButton);
            assertNotEquals(pixelAt(mekBayIdlePixels, 0, 0), pixelAt(mekHQIdlePixels, 0, 0));

            mekBayButton.setFrameActive(true, 0);
            mekHQButton.setFrameActive(true, 0);
            mekBayButton.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS);
            mekHQButton.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS);

            assertNotEquals(pixelAt(renderPixels(mekBayButton), 0, 0),
                  pixelAt(renderPixels(mekHQButton), 0, 0));
        });
    }

    @Test
    void legacyConstructionDefaultsToTransmissionScanAndActionsRemainImmediate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ImmersiveDialogCore.ButtonLabelTooltipPair pair =
                  new ImmersiveDialogCore.ButtonLabelTooltipPair("Respond", null);
            TransmissionResponseButton button = createButton();
            AtomicInteger actionCount = new AtomicInteger();
            button.addActionListener(event -> actionCount.incrementAndGet());

            button.doClick(0);

            assertEquals(ResponseButtonMotion.TRANSMISSION_SCAN, pair.responseMotion());
            assertEquals(ResponseButtonMotion.TRANSMISSION_SCAN, button.getResponseMotion());
            assertEquals(1, actionCount.get());
        });
    }

    private static TransmissionResponseButton createButton() {
        TransmissionResponseButton button = new TransmissionResponseButton("Respond");
        ImmersiveDialogStyle.applyResponseButtonStyle(button);
        button.setSize(WIDTH, HEIGHT);
        return button;
    }

    private static TransmissionResponseButton createFrameButton(ResponseButtonMotion motion) {
        TransmissionResponseButton button = new TransmissionResponseButton("Respond", motion);
        ImmersiveDialogStyle.applyResponseButtonStyle(button);
        button.setSize(WIDTH, HEIGHT);
        return button;
    }

    private static int[] renderPixels(TransmissionResponseButton button) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        button.paint(graphics2D);
        graphics2D.dispose();
        return image.getRGB(0, 0, WIDTH, HEIGHT, null, 0, WIDTH);
    }

    private static int pixelAt(int[] pixels, int x, int y) {
        return pixels[y * WIDTH + x];
    }

    private static void fireMouseEntered(TransmissionResponseButton button) {
        MouseEvent event = new MouseEvent(button,
              MouseEvent.MOUSE_ENTERED,
              System.currentTimeMillis(),
              0,
              2,
              2,
              0,
              false);
        for (MouseListener listener : button.getMouseListeners()) {
            listener.mouseEntered(event);
        }
    }

    private static void fireMouseExited(TransmissionResponseButton button) {
        MouseEvent event = new MouseEvent(button,
              MouseEvent.MOUSE_EXITED,
              System.currentTimeMillis(),
              0,
              WIDTH + 1,
              HEIGHT + 1,
              0,
              false);
        for (MouseListener listener : button.getMouseListeners()) {
            listener.mouseExited(event);
        }
    }

    private static void fireFocusGained(TransmissionResponseButton button, FocusEvent.Cause cause) {
        FocusEvent event = new FocusEvent(button, FocusEvent.FOCUS_GAINED, false, null, cause);
        for (FocusListener listener : button.getFocusListeners()) {
            listener.focusGained(event);
        }
    }

    private static void fireFocusLost(TransmissionResponseButton button) {
        FocusEvent event = new FocusEvent(button, FocusEvent.FOCUS_LOST);
        for (FocusListener listener : button.getFocusListeners()) {
            listener.focusLost(event);
        }
    }
}
