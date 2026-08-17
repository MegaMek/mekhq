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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class TransmissionResponseButtonTest {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 44;
    private static final int HORIZONTAL_LAYOUT_ALLOWANCE = 4;
    private static final int VERTICAL_LAYOUT_ALLOWANCE = 2;
    private static final String CONFIRMATION_TEXT = "TRANSMITTING";
    private static final String COMPACT_CONFIRMATION_TEXT = "TX...";
    private static final String ACCESSIBLE_CONFIRMATION_TEXT = "Transmitting response";

    @Test
    void framePaintsIdlePartialAndFullStates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
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
            TransmissionResponseButton shortButton = createButton();
            TransmissionResponseButton longButton = createButton();
            longButton.setSize(WIDTH * 2, HEIGHT);

            shortButton.setFrameActive(true, 0);
            longButton.setFrameActive(true, 0);
            shortButton.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 2);
            longButton.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 2);

            assertEquals(shortButton.getFrameProgress(), longButton.getFrameProgress(), 0.001);
            assertEquals(0.5, shortButton.getFrameProgress(), 0.001);
            assertFalse(shortButton.isAnimationTimerRepeating());
            assertEquals(320_000_000L, TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS);
        });
    }

    @Test
    void frameReversesFromCurrentProgress() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
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
    void focusedDefaultFrameUsesBrightCornersWhileHoverStillAnimates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            int[] idlePixels = renderPixels(button);

            JRootPane rootPane = new JRootPane();
            rootPane.getContentPane().add(button);
            rootPane.setDefaultButton(button);

            fireFocusGained(button, FocusEvent.Cause.ACTIVATION);
            int[] focusedPixels = renderPixels(button);
            assertFalse(button.isFrameTransitionRunning());
            assertEquals(0.0, button.getFrameProgress(), 0.001);
            assertFalse(java.util.Arrays.equals(idlePixels, focusedPixels));
            assertNotEquals(pixelAt(idlePixels, 0, 0), pixelAt(focusedPixels, 0, 0));
            assertEquals(pixelAt(focusedPixels, WIDTH / 2, 0), pixelAt(focusedPixels, WIDTH / 2, 3));
            assertEquals(pixelAt(idlePixels, 5, HEIGHT / 2), pixelAt(focusedPixels, 5, HEIGHT / 2));

            fireMouseEntered(button);
            assertTrue(button.isFrameTransitionRunning());
            button.advanceFrameTransition(Long.MAX_VALUE);
            int[] hoverPixels = renderPixels(button);
            assertNotEquals(pixelAt(hoverPixels, WIDTH / 2, 0), pixelAt(hoverPixels, WIDTH / 2, 3));
            assertFalse(java.util.Arrays.equals(focusedPixels, hoverPixels));

            fireMouseExited(button);
            assertTrue(button.isFrameTransitionRunning());
            button.advanceFrameTransition(Long.MAX_VALUE);
            assertFalse(button.isFrameTransitionRunning());
            assertEquals(0.0, button.getFrameProgress(), 0.001);
            assertArrayEquals(focusedPixels, renderPixels(button));

            fireFocusLost(button);
            assertArrayEquals(focusedPixels, renderPixels(button));
            rootPane.setDefaultButton(null);
            assertArrayEquals(idlePixels, renderPixels(button));
        });
    }

    @Test
    void modelPressForcesFullFrameAndReleaseRestoresInteractionState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            fireFocusGained(button, FocusEvent.Cause.TRAVERSAL_FORWARD);
            int[] focusedPixels = renderPixels(button);

            button.getModel().setArmed(true);
            button.getModel().setPressed(true);
            int[] pressedPixels = renderPixels(button);
            assertEquals(0.0, button.getFrameProgress(), 0.001);
            assertNotEquals(pixelAt(pressedPixels, WIDTH / 2, 0), pixelAt(pressedPixels, WIDTH / 2, 3));
            assertNotEquals(pixelAt(focusedPixels, 5, HEIGHT / 2), pixelAt(pressedPixels, 5, HEIGHT / 2));

            button.getModel().setPressed(false);
            button.getModel().setArmed(false);
            assertArrayEquals(focusedPixels, renderPixels(button));

            fireMouseEntered(button);
            button.advanceFrameTransition(Long.MAX_VALUE);
            int[] hoverPixels = renderPixels(button);
            button.getModel().setArmed(true);
            button.getModel().setPressed(true);
            button.getModel().setPressed(false);
            button.getModel().setArmed(false);
            assertArrayEquals(hoverPixels, renderPixels(button));
        });
    }

    @Test
    void disabledAndRemovedFrameButtonStopsCleanly() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
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
    void defaultConstructionPreservesButtonMetadataAndActionsRemainImmediate() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ImmersiveDialogCore.ButtonLabelTooltipPair pair =
                  new ImmersiveDialogCore.ButtonLabelTooltipPair("Respond", null);
            TransmissionResponseButton button = new TransmissionResponseButton("Respond");
            AtomicInteger actionCount = new AtomicInteger();
            button.addActionListener(event -> actionCount.incrementAndGet());

            button.doClick(0);

            assertEquals("Respond", pair.btnLabel());
            assertEquals(null, pair.btnTooltip());
            assertEquals(1, actionCount.get());
        });
    }

    @Test
    void horizontalLabelsPreservePlainAndExplicitHtmlText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String plainText = ImmersiveDialogCore.resolveHorizontalButtonText("Request Support", "Tooltip");
            TransmissionResponseButton plainButton = createStyledFrameButton(plainText);
            TransmissionResponseButton comparisonButton = createStyledFrameButton("Respond");

            assertEquals("Request Support", plainText);
            assertEquals(plainText, plainButton.getText());
            assertFalse(plainButton.getText().startsWith("<html"));
            assertEquals(comparisonButton.getPreferredSize().height, plainButton.getPreferredSize().height);

            String htmlText = "<html><b>Request</b><br>Support</html>";
            String resolvedHtmlText = ImmersiveDialogCore.resolveHorizontalButtonText(htmlText, null);
            TransmissionResponseButton htmlButton = createStyledFrameButton(resolvedHtmlText);

            assertEquals(htmlText, resolvedHtmlText);
            assertEquals(htmlText, htmlButton.getText());
        });
    }

    @Test
    void uniformButtonSizingUsesWideGlyphContentAndIndependentDimensions() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton wideButton = createStyledFrameButton("WWW Request Support");
            TransmissionResponseButton tallButton = createStyledFrameButton("<html>Confirm<br>Assignment</html>");
            Dimension naturalWideSize = wideButton.getPreferredSize();
            Dimension naturalTallSize = tallButton.getPreferredSize();
            Dimension wideContentRequirement = calculatePlainTextRequirement(wideButton);
            Dimension expectedSize = new Dimension(
                  Math.max(Math.max(naturalWideSize.width, wideContentRequirement.width), naturalTallSize.width),
                  Math.max(Math.max(naturalWideSize.height, wideContentRequirement.height), naturalTallSize.height));

            assertTrue(wideContentRequirement.width > naturalTallSize.width);

            ImmersiveDialogCore.applyUniformButtonSizes(List.of(wideButton, tallButton));

            assertEquals(expectedSize, wideButton.getPreferredSize());
            assertEquals(expectedSize, wideButton.getMinimumSize());
            assertEquals(expectedSize, tallButton.getPreferredSize());
            assertEquals(expectedSize, tallButton.getMinimumSize());
            assertNotSame(wideButton.getPreferredSize(), wideButton.getMinimumSize());
            assertNotSame(wideButton.getPreferredSize(), tallButton.getPreferredSize());
            assertNotSame(wideButton.getMinimumSize(), tallButton.getMinimumSize());
            assertTrue(wideButton.getPreferredSize().width >= wideContentRequirement.width);
            assertTrue(wideButton.getMinimumSize().width >= wideContentRequirement.width);

            wideButton.getPreferredSize().width++;
            assertEquals(expectedSize, wideButton.getMinimumSize());
            assertEquals(expectedSize, tallButton.getPreferredSize());
        });
    }

    @Test
    void horizontalResponseButtonsStayUniformAndWrapOnlyWhenWidthRequiresIt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<String> labels = List.of(
                  "Advance", "Hold Position", "Withdraw", "Request Support", "Abort Operation");
            List<TransmissionResponseButton> buttons = labels.stream()
                .map(label -> createStyledFrameButton(
                    ImmersiveDialogCore.resolveHorizontalButtonText(label, null)))
                .toList();
            List<Dimension> contentRequirements = buttons.stream()
                .map(TransmissionResponseButtonTest::calculatePlainTextRequirement)
                .toList();

            ImmersiveDialogCore.applyUniformButtonSizes(buttons);

            Dimension uniformSize = new Dimension(buttons.get(0).getPreferredSize());
            for (TransmissionResponseButton button : buttons) {
                assertEquals(uniformSize, button.getPreferredSize());
                assertEquals(uniformSize, button.getMinimumSize());
                for (Dimension contentRequirement : contentRequirements) {
                    assertTrue(button.getPreferredSize().width >= contentRequirement.width);
                    assertTrue(button.getPreferredSize().height >= contentRequirement.height);
                    assertTrue(button.getMinimumSize().width >= contentRequirement.width);
                    assertTrue(button.getMinimumSize().height >= contentRequirement.height);
                }
            }

            TransmissionResponseButton requestSupportButton = buttons.get(3);
            assertEquals("Request Support", requestSupportButton.getText());
            assertFalse(requestSupportButton.getText().startsWith("<html"));
            FontMetrics fontMetrics = requestSupportButton.getFontMetrics(requestSupportButton.getFont());
            Insets insets = requestSupportButton.getInsets();
            int exactTextAndInsetsWidth = fontMetrics.stringWidth(requestSupportButton.getText())
                + insets.left
                + insets.right;
            assertTrue(requestSupportButton.getPreferredSize().width > exactTextAndInsetsWidth);
            assertTrue(requestSupportButton.getMinimumSize().width > exactTextAndInsetsWidth);

            JPanel buttonPanel = ImmersiveDialogCore.createResponseButtonPanel(false, scaleForGUI(5));
            buttons.forEach(buttonPanel::add);
            Dimension oneRowSize = buttonPanel.getPreferredSize();
            buttonPanel.setSize(oneRowSize);
            buttonPanel.doLayout();

            assertTrue(areInSingleRow(buttons));
            assertLaidOutAtUniformSize(buttons, uniformSize);

            int constrainedWidth = oneRowSize.width - uniformSize.width;
            buttonPanel.setSize(constrainedWidth, oneRowSize.height);
            Dimension wrappedSize = buttonPanel.getPreferredSize();
            buttonPanel.setSize(constrainedWidth, wrappedSize.height);
            buttonPanel.doLayout();

            assertFalse(areInSingleRow(buttons));
            assertTrue(wrappedSize.height > oneRowSize.height);
            assertLaidOutAtUniformSize(buttons, uniformSize);
        });
    }

    @Test
    void frameAnimationAndConfirmationPreserveDimensionsWithoutForegroundChurn() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            ImmersiveDialogCore.applyUniformButtonSizes(List.of(button));
            Dimension preferredSize = new Dimension(button.getPreferredSize());
            Dimension minimumSize = new Dimension(button.getMinimumSize());
            Dimension currentSize = new Dimension(button.getSize());
            AtomicInteger foregroundChanges = new AtomicInteger();
            button.addPropertyChangeListener("foreground", event -> foregroundChanges.incrementAndGet());

            button.setFrameActive(true, 0);
            int changesAfterHover = foregroundChanges.get();
            assertTrue(changesAfterHover <= 1);

            button.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS / 2);
            assertButtonDimensions(button, preferredSize, minimumSize, currentSize);
            assertEquals(changesAfterHover, foregroundChanges.get());

            button.advanceFrameTransition(TransmissionResponseButton.FRAME_TRANSITION_DURATION_NANOS);
            assertButtonDimensions(button, preferredSize, minimumSize, currentSize);
            assertEquals(changesAfterHover, foregroundChanges.get());

            button.lockTransmissionConfirmation(
                  CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT);
            assertButtonDimensions(button, preferredSize, minimumSize, currentSize);
        });
    }

    @Test
    void transmissionConfirmationPreservesTextAndSizeAndCleansUpOnRemoval() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            button.getAccessibleContext().setAccessibleName("Original response");
            String originalText = button.getText();
            Dimension originalPreferredSize = button.getPreferredSize();
            Dimension originalMinimumSize = button.getMinimumSize();
            Dimension originalMaximumSize = button.getMaximumSize();
            Dimension originalSize = button.getSize();
            int[] idlePixels = renderPixels(button);

            assertTrue(button.lockTransmissionConfirmation(
                CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT));
            assertFalse(button.lockTransmissionConfirmation(
                CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT));
            assertTrue(button.isTransmissionConfirmationVisible());
            assertEquals(originalText, button.getText());
            assertEquals(originalPreferredSize, button.getPreferredSize());
            assertEquals(originalMinimumSize, button.getMinimumSize());
            assertEquals(originalMaximumSize, button.getMaximumSize());
            assertEquals(originalSize, button.getSize());
            assertEquals(ACCESSIBLE_CONFIRMATION_TEXT, button.getAccessibleContext().getAccessibleName());
            assertEquals(1.0, button.getFrameProgress(), 0.001);
            assertFalse(button.isAnimationTimerRunning());

            int[] transmittingPixels = renderPixels(button);
            assertNotEquals(pixelAt(idlePixels, WIDTH / 2, 5), pixelAt(transmittingPixels, WIDTH / 2, 5));
            assertNotEquals(pixelAt(transmittingPixels, WIDTH / 2, 0),
                  pixelAt(transmittingPixels, WIDTH / 2, 3));
            assertNotEquals(pixelAt(transmittingPixels, 0, HEIGHT / 2),
                pixelAt(transmittingPixels, 3, HEIGHT / 2));

            fireFocusLost(button);
            assertEquals(1.0, button.getFrameProgress(), 0.001);

            button.removeNotify();
            assertFalse(button.isTransmissionConfirmationVisible());
            assertFalse(button.isAnimationTimerRunning());
            assertEquals(originalText, button.getText());
            assertEquals(originalPreferredSize, button.getPreferredSize());
            assertEquals(originalMinimumSize, button.getMinimumSize());
            assertEquals(originalMaximumSize, button.getMaximumSize());
            assertEquals(originalSize, button.getSize());
            assertEquals("Original response", button.getAccessibleContext().getAccessibleName());
        });
    }

    @Test
    void confirmationUsesCompactTextOnlyWhenFullTextDoesNotFit() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            FontMetrics fontMetrics = button.getFontMetrics(button.getFont());
            Insets insets = button.getInsets();
            int frameWidth = insets.left + insets.right;
            int fullTextWidth = fontMetrics.stringWidth(CONFIRMATION_TEXT);

            button.setSize(frameWidth + fullTextWidth, HEIGHT);
            button.lockTransmissionConfirmation(
                  CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT);
            assertEquals(CONFIRMATION_TEXT, button.getTransmissionConfirmationOverlayText(fontMetrics));

            button.clearTransmissionConfirmation();
            button.setSize(frameWidth + fullTextWidth - 1, HEIGHT);
            button.lockTransmissionConfirmation(
                  CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT);
            assertEquals(COMPACT_CONFIRMATION_TEXT, button.getTransmissionConfirmationOverlayText(fontMetrics));
        });
    }

    @Test
    void responseActivationCapturesImmediatelyAndRejectsDuplicates() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton selectedButton = createButton();
            TransmissionResponseButton alternateButton = createButton();
            AtomicInteger captureCount = new AtomicInteger();
            AtomicInteger disposeCount = new AtomicInteger();
            boolean[] capturedBeforeVisualChanges = new boolean[1];
            selectedButton.getAccessibleContext().setAccessibleName("Original response");
            ImmersiveDialogCore.ResponseActivationController controller =
                new ImmersiveDialogCore.ResponseActivationController(disposeCount::incrementAndGet);

            boolean activated = controller.activate(selectedButton,
                  List.of(selectedButton, alternateButton),
                  () -> {
                      captureCount.incrementAndGet();
                      capturedBeforeVisualChanges[0] = !selectedButton.isTransmissionConfirmationVisible()
                                                         && alternateButton.isEnabled();
                  },
                  CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT);

            assertTrue(activated);
            assertEquals(1, captureCount.get());
            assertTrue(capturedBeforeVisualChanges[0]);
            assertTrue(selectedButton.isEnabled());
            assertFalse(alternateButton.isEnabled());
            assertTrue(selectedButton.isTransmissionConfirmationVisible());
            assertEquals("Respond", selectedButton.getText());
            assertEquals(ACCESSIBLE_CONFIRMATION_TEXT, selectedButton.getAccessibleContext().getAccessibleName());
            int[] selectedPixels = renderPixels(selectedButton);
            assertNotEquals(pixelAt(selectedPixels, WIDTH / 2, 0), pixelAt(selectedPixels, WIDTH / 2, 3));
            assertNotEquals(pixelAt(selectedPixels, 0, HEIGHT / 2), pixelAt(selectedPixels, 3, HEIGHT / 2));
            assertTrue(controller.isConfirmationTimerRunning());
            assertFalse(controller.isConfirmationTimerRepeating());
            assertEquals(350,
                  ImmersiveDialogCore.ResponseActivationController.TRANSMISSION_CONFIRMATION_DELAY_MS);

            assertFalse(controller.activate(selectedButton,
                  List.of(selectedButton, alternateButton),
                  captureCount::incrementAndGet,
                  CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT));
            assertEquals(1, captureCount.get());

            controller.completeTransmission();
            controller.completeTransmission();
            assertEquals(1, disposeCount.get());
            assertFalse(controller.isConfirmationTimerRunning());
            assertFalse(selectedButton.isTransmissionConfirmationVisible());
            assertEquals("Original response", selectedButton.getAccessibleContext().getAccessibleName());
        });
    }

    @Test
    void responseActivationTimerCancelsWithoutDisposal() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TransmissionResponseButton button = createButton();
            AtomicInteger disposeCount = new AtomicInteger();
            button.getAccessibleContext().setAccessibleName("Original response");
            ImmersiveDialogCore.ResponseActivationController controller =
                new ImmersiveDialogCore.ResponseActivationController(disposeCount::incrementAndGet);
            controller.activate(button, List.of(button), () -> { },
                  CONFIRMATION_TEXT, COMPACT_CONFIRMATION_TEXT, ACCESSIBLE_CONFIRMATION_TEXT);

            controller.cancel();
            controller.completeTransmission();

            assertFalse(controller.isConfirmationTimerRunning());
            assertFalse(button.isTransmissionConfirmationVisible());
            assertFalse(button.isAnimationTimerRunning());
            assertEquals(0.0, button.getFrameProgress(), 0.001);
            assertEquals("Original response", button.getAccessibleContext().getAccessibleName());
            assertEquals(0, disposeCount.get());
        });
    }

    private static TransmissionResponseButton createButton() {
        TransmissionResponseButton button = new TransmissionResponseButton("Respond");
        ImmersiveDialogStyle.applyResponseButtonStyle(button);
        button.setSize(WIDTH, HEIGHT);
        return button;
    }

    private static TransmissionResponseButton createStyledFrameButton(String text) {
        TransmissionResponseButton button = new TransmissionResponseButton(text);
        ImmersiveDialogStyle.applyResponseButtonStyle(button);
        return button;
    }

    private static Dimension calculatePlainTextRequirement(TransmissionResponseButton button) {
        FontMetrics fontMetrics = button.getFontMetrics(button.getFont());
        Insets insets = button.getInsets();
        int horizontalAllowance = scaleForGUI(HORIZONTAL_LAYOUT_ALLOWANCE);
        int verticalAllowance = scaleForGUI(VERTICAL_LAYOUT_ALLOWANCE);
        return new Dimension(
              fontMetrics.stringWidth(button.getText()) + insets.left + insets.right + horizontalAllowance * 2,
              fontMetrics.getHeight() + insets.top + insets.bottom + verticalAllowance * 2);
    }

    private static void assertButtonDimensions(TransmissionResponseButton button, Dimension preferredSize,
          Dimension minimumSize, Dimension currentSize) {
        assertEquals(preferredSize, button.getPreferredSize());
        assertEquals(minimumSize, button.getMinimumSize());
        assertEquals(currentSize, button.getSize());
    }

    private static void assertLaidOutAtUniformSize(List<TransmissionResponseButton> buttons, Dimension uniformSize) {
        for (TransmissionResponseButton button : buttons) {
            assertEquals(uniformSize, button.getSize());
        }
    }

    private static boolean areInSingleRow(List<TransmissionResponseButton> buttons) {
        int firstY = buttons.get(0).getY();
        for (TransmissionResponseButton button : buttons) {
            if (button.getY() != firstY) {
                return false;
            }
        }
        return true;
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
