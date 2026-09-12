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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import mekhq.gui.baseComponents.FramedCommandButton;
import mekhq.gui.baseComponents.FramedCommandButtonStyle.ButtonColors;
import mekhq.gui.baseComponents.FramedCommandButtonStyle.ButtonStateColors;

/** Dialog response button with a temporary transmission-confirmation overlay. */
final class TransmissionResponseButton extends FramedCommandButton {
    static final long FRAME_TRANSITION_DURATION_NANOS = 320_000_000L;
    private boolean transmissionConfirmationVisible;
    private String transmissionConfirmationText;
    private String compactTransmissionConfirmationText;
    private String accessibleNameBeforeTransmission;

    TransmissionResponseButton(String text) {
        super(text);
    }

    @Override
    public void removeNotify() {
        clearTransmissionConfirmation();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (transmissionConfirmationVisible) {
            paintTransmissionConfirmation(graphics);
            return;
        }
        super.paintComponent(graphics);
    }

    @Override
    protected boolean isFrameInteractionLocked() {
        return transmissionConfirmationVisible;
    }

    @Override
    protected ButtonStateColors getLockedButtonStateColors(ButtonColors colors) {
        return colors.pressed();
    }

    void setFrameActive(boolean active, long nowNanos) {
        transitionFrame(active, nowNanos);
    }

    void advanceFrameTransition(long nowNanos) {
        advanceFrame(nowNanos);
    }

    double getFrameProgress() {
        return frameProgress();
    }

    boolean isFrameTransitionRunning() {
        return isFrameTransitionActive();
    }

    boolean isAnimationTimerRunning() {
        return isFrameTimerRunning();
    }

    boolean isAnimationTimerRepeating() {
        return isFrameTimerRepeating();
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
        setFrameProgressImmediately(1.0);
        getAccessibleContext().setAccessibleName(accessibleFeedbackText);
        refreshFrameForeground();
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

    private void paintTransmissionConfirmation(Graphics graphics) {
        ButtonStateColors pressedColors = getButtonColors().pressed();

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

    void clearTransmissionConfirmation() {
        if (!transmissionConfirmationVisible) {
            return;
        }

        transmissionConfirmationVisible = false;
        transmissionConfirmationText = null;
        compactTransmissionConfirmationText = null;
        getAccessibleContext().setAccessibleName(accessibleNameBeforeTransmission);
        accessibleNameBeforeTransmission = null;
        setFrameProgressImmediately(isPointerActive() ? 1.0 : 0.0);
    }

}
