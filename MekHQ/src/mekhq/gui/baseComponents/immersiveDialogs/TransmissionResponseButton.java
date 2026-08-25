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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.Timer;

/** Response button with a signal frame that expands on hover. */
final class TransmissionResponseButton extends JButton {
    private static final int FRAME_DELAY = 16;
    static final long FRAME_TRANSITION_DURATION_NANOS = 320_000_000L;
    private static final int CORNER_LENGTH = 12;
    private static final int FRAME_THICKNESS = 2;
    private static final int CONTENT_VERTICAL_MARGIN = 8;
    private static final int CONTENT_HORIZONTAL_MARGIN = 16;

    private final Timer animationTimer;

    private long frameTransitionStartNanos;
    private long frameTransitionDurationNanos;
    private double frameTransitionStartProgress;
    private double frameTargetProgress;
    private double frameProgress;
    private boolean frameTransitionActive;
    private boolean pointerActive;
    private boolean focusActive;
    private boolean transmissionConfirmationVisible;
    private String transmissionConfirmationText;
    private String compactTransmissionConfirmationText;
    private String accessibleNameBeforeTransmission;

    TransmissionResponseButton(String text) {
        super(text);
        setMargin(new Insets(scaleForGUI(CONTENT_VERTICAL_MARGIN), scaleForGUI(CONTENT_HORIZONTAL_MARGIN),
              scaleForGUI(CONTENT_VERTICAL_MARGIN), scaleForGUI(CONTENT_HORIZONTAL_MARGIN)));
        setRolloverEnabled(true);

        animationTimer = new Timer(FRAME_DELAY, event -> advanceAnimation());
        animationTimer.setCoalesce(true);
        animationTimer.setRepeats(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                pointerActive = true;
                updateFrameTarget(System.nanoTime());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                pointerActive = false;
                updateFrameTarget(System.nanoTime());
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                focusActive = true;
                updateFrameForeground();
                repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                focusActive = false;
                updateFrameForeground();
                repaint();
            }
        });
        getModel().addChangeListener(event -> {
            updateFrameTarget(System.nanoTime());
            updateFrameForeground();
            repaint();
        });
        addPropertyChangeListener("enabled", event -> {
            if (!isEnabled()) {
                resetAnimation();
            } else {
                updateFrameForeground();
            }
        });
    }

    @Override
    public void removeNotify() {
        clearTransmissionConfirmation();
        resetAnimation();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (transmissionConfirmationVisible) {
            paintTransmissionConfirmation(graphics);
            return;
        }
        paintFrameButton(graphics);
    }

    void applyFrameStyle() {
        setFocusable(true);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        updateFrameForeground();
    }

    void setFrameActive(boolean active, long nowNanos) {
        if (!isEnabled() || transmissionConfirmationVisible) {
            return;
        }

        if (frameTransitionActive) {
            advanceFrameTransition(nowNanos);
        }

        double targetProgress = active ? 1.0 : 0.0;
        if (targetProgress == frameTargetProgress && frameTransitionActive) {
            return;
        }
        if (targetProgress == frameProgress) {
            frameTargetProgress = targetProgress;
            frameTransitionActive = false;
            animationTimer.stop();
            updateFrameForeground();
            return;
        }

        frameTransitionStartProgress = frameProgress;
        frameTargetProgress = targetProgress;
        frameTransitionStartNanos = nowNanos;
        frameTransitionDurationNanos = Math.max(1,
              Math.round(FRAME_TRANSITION_DURATION_NANOS * Math.abs(frameTargetProgress - frameProgress)));
        frameTransitionActive = true;
        updateFrameForeground();
        animationTimer.restart();
        repaint();
    }

    void advanceFrameTransition(long nowNanos) {
        if (!frameTransitionActive) {
            return;
        }

        long elapsedNanos = Math.max(0, nowNanos - frameTransitionStartNanos);
        double linearProgress = Math.min(1.0, (double) elapsedNanos / frameTransitionDurationNanos);
        double easedProgress = linearProgress * linearProgress * (3.0 - 2.0 * linearProgress);
        frameProgress = frameTransitionStartProgress +
                              (frameTargetProgress - frameTransitionStartProgress) * easedProgress;

        if (linearProgress >= 1.0) {
            frameProgress = frameTargetProgress;
            frameTransitionActive = false;
            animationTimer.stop();
        }
        repaint();
    }

    double getFrameProgress() {
        return frameProgress;
    }

    boolean isFrameTransitionRunning() {
        return frameTransitionActive;
    }

    boolean isAnimationTimerRunning() {
        return animationTimer.isRunning();
    }

    boolean isAnimationTimerRepeating() {
        return animationTimer.isRepeats();
    }

    boolean lockTransmissionConfirmation(String confirmationText, String compactConfirmationText,
          String accessibleFeedbackText) {
        if (transmissionConfirmationVisible) {
            return false;
        }
        if (confirmationText == null || compactConfirmationText == null || accessibleFeedbackText == null) {
            throw new IllegalArgumentException("transmission confirmation text cannot be null");
        }

        transmissionConfirmationVisible = true;
        transmissionConfirmationText = confirmationText;
        compactTransmissionConfirmationText = compactConfirmationText;
        accessibleNameBeforeTransmission = getAccessibleContext().getAccessibleName();
        animationTimer.stop();
        frameTransitionActive = false;
        frameTransitionStartProgress = 1.0;
        frameTargetProgress = 1.0;
        frameProgress = 1.0;
        getAccessibleContext().setAccessibleName(accessibleFeedbackText);
        updateFrameForeground();
        repaint();
        return true;
    }

    boolean isTransmissionConfirmationVisible() {
        return transmissionConfirmationVisible;
    }

    String getTransmissionConfirmationOverlayText(FontMetrics fontMetrics) {
        Insets insets = getInsets();
        int availableWidth = Math.max(0, getWidth() - insets.left - insets.right);
        return fontMetrics.stringWidth(transmissionConfirmationText) <= availableWidth
                     ? transmissionConfirmationText
                     : compactTransmissionConfirmationText;
    }

    private void advanceAnimation() {
        advanceFrameTransition(System.nanoTime());
        if (frameTransitionActive) {
            animationTimer.restart();
        }
    }

    private void updateFrameTarget(long nowNanos) {
        if (transmissionConfirmationVisible) {
            return;
        }
        setFrameActive(pointerActive, nowNanos);
    }

    private void resetAnimation() {
        animationTimer.stop();
        frameTransitionActive = false;
        frameTransitionStartProgress = 0.0;
        frameTargetProgress = 0.0;
        frameProgress = 0.0;
        pointerActive = false;
        focusActive = false;
        updateFrameForeground();
        repaint();
    }

    private void paintFrameButton(Graphics graphics) {
        ImmersiveDialogStyle.ResponseButtonColors colors = ImmersiveDialogStyle.getResponseButtonColors();
        ImmersiveDialogStyle.ResponseButtonStateColors stateColors;
        double paintProgress;
        if (!isEnabled()) {
            stateColors = colors.disabled();
            paintProgress = 0.0;
        } else if (getModel().isPressed()) {
            stateColors = colors.pressed();
            paintProgress = 1.0;
        } else {
            stateColors = blend(colors.idle(), colors.active(), frameProgress);
            if (focusActive || isDefaultButton()) {
                stateColors = new ImmersiveDialogStyle.ResponseButtonStateColors(
                      stateColors.background(), stateColors.foreground(), colors.active().frame());
            }
            paintProgress = frameProgress;
        }

        Graphics2D backgroundGraphics = (Graphics2D) graphics.create();
        backgroundGraphics.setColor(stateColors.background());
        backgroundGraphics.fillRect(0, 0, getWidth(), getHeight());
        backgroundGraphics.dispose();

        super.paintComponent(graphics);

        Graphics2D frameGraphics = (Graphics2D) graphics.create();
        frameGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        frameGraphics.setColor(stateColors.frame());
        paintCornerFrame(frameGraphics, paintProgress);
        frameGraphics.dispose();
    }

    private void paintTransmissionConfirmation(Graphics graphics) {
        ImmersiveDialogStyle.ResponseButtonStateColors pressedColors =
              ImmersiveDialogStyle.getTransmissionConfirmationColors();

        Graphics2D backgroundGraphics = (Graphics2D) graphics.create();
        backgroundGraphics.setColor(pressedColors.background());
        backgroundGraphics.fillRect(0, 0, getWidth(), getHeight());
        backgroundGraphics.dispose();

        Insets insets = getInsets();
        int innerWidth = Math.max(0, getWidth() - insets.left - insets.right);
        int innerHeight = Math.max(0, getHeight() - insets.top - insets.bottom);
        if (innerWidth > 0 && innerHeight > 0) {
            Graphics2D textGraphics = (Graphics2D) graphics.create();
            textGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            textGraphics.setColor(pressedColors.foreground());
            textGraphics.setFont(getFont());
            textGraphics.clipRect(insets.left, insets.top, innerWidth, innerHeight);
            FontMetrics fontMetrics = textGraphics.getFontMetrics();
            String overlayText = getTransmissionConfirmationOverlayText(fontMetrics);
            int textX = insets.left + Math.max(0, (innerWidth - fontMetrics.stringWidth(overlayText)) / 2);
            int textY = insets.top + Math.max(0, (innerHeight - fontMetrics.getHeight()) / 2)
                              + fontMetrics.getAscent();
            textGraphics.drawString(overlayText, textX, textY);
            textGraphics.dispose();
        }

        Graphics2D frameGraphics = (Graphics2D) graphics.create();
        frameGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        frameGraphics.setColor(pressedColors.frame());
        paintCornerFrame(frameGraphics, 1.0);
        frameGraphics.dispose();
    }

    private void paintCornerFrame(Graphics2D graphics, double progress) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        int thickness = Math.min(Math.max(1, scaleForGUI(FRAME_THICKNESS)), Math.min(width, height));
        int halfWidth = (width + 1) / 2;
        int halfHeight = (height + 1) / 2;
        int horizontalCorner = Math.min(scaleForGUI(CORNER_LENGTH), halfWidth);
        int verticalCorner = Math.min(scaleForGUI(CORNER_LENGTH), halfHeight);
        int horizontalLength = horizontalCorner +
                                     (int) Math.round((halfWidth - horizontalCorner) * progress);
        int verticalLength = verticalCorner +
                                   (int) Math.round((halfHeight - verticalCorner) * progress);

        graphics.fillRect(0, 0, horizontalLength, thickness);
        graphics.fillRect(width - horizontalLength, 0, horizontalLength, thickness);
        graphics.fillRect(0, height - thickness, horizontalLength, thickness);
        graphics.fillRect(width - horizontalLength, height - thickness, horizontalLength, thickness);
        graphics.fillRect(0, 0, thickness, verticalLength);
        graphics.fillRect(width - thickness, 0, thickness, verticalLength);
        graphics.fillRect(0, height - verticalLength, thickness, verticalLength);
        graphics.fillRect(width - thickness, height - verticalLength, thickness, verticalLength);
    }

    private void updateFrameForeground() {
        ImmersiveDialogStyle.ResponseButtonColors colors = ImmersiveDialogStyle.getResponseButtonColors();
        Color foreground;
        if (transmissionConfirmationVisible) {
            foreground = colors.pressed().foreground();
        } else if (getModel().isPressed()) {
            foreground = colors.pressed().foreground();
        } else {
            foreground = isEnabled() && frameTargetProgress > 0.0
                               ? colors.active().foreground()
                               : isEnabled() ? colors.idle().foreground() : colors.disabled().foreground();
        }
        if (!foreground.equals(super.getForeground())) {
            super.setForeground(foreground);
        }
    }

    void clearTransmissionConfirmation() {
        if (!transmissionConfirmationVisible) {
            return;
        }

        transmissionConfirmationVisible = false;
        transmissionConfirmationText = null;
        compactTransmissionConfirmationText = null;
        getAccessibleContext().setAccessibleName(accessibleNameBeforeTransmission);
        accessibleNameBeforeTransmission = null;
        frameTransitionActive = false;
        frameTransitionStartProgress = pointerActive ? 1.0 : 0.0;
        frameTargetProgress = frameTransitionStartProgress;
        frameProgress = frameTransitionStartProgress;
        animationTimer.stop();
        updateFrameForeground();
        repaint();
    }

    private static ImmersiveDialogStyle.ResponseButtonStateColors blend(
          ImmersiveDialogStyle.ResponseButtonStateColors idle,
          ImmersiveDialogStyle.ResponseButtonStateColors active, double progress) {
        return new ImmersiveDialogStyle.ResponseButtonStateColors(
              blend(idle.background(), active.background(), progress),
              blend(idle.foreground(), active.foreground(), progress),
              blend(idle.frame(), active.frame(), progress));
    }

    private static Color blend(Color firstColor, Color secondColor, double progress) {
        double firstWeight = 1.0 - progress;
        int red = (int) Math.round(firstColor.getRed() * firstWeight + secondColor.getRed() * progress);
        int green = (int) Math.round(firstColor.getGreen() * firstWeight + secondColor.getGreen() * progress);
        int blue = (int) Math.round(firstColor.getBlue() * firstWeight + secondColor.getBlue() * progress);
        int alpha = (int) Math.round(firstColor.getAlpha() * firstWeight + secondColor.getAlpha() * progress);
        return new Color(red, green, blue, alpha);
    }

}
