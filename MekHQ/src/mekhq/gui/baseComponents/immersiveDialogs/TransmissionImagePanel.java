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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.Timer;

import megamek.common.annotations.Nullable;

/**
 * Displays an image as a live video transmission with restrained analog interference.
 */
final class TransmissionImagePanel extends JComponent {
    private static final int ANIMATION_DELAY = 125;

    private final BufferedImage sourceImage;
    private final TransmissionSignalQuality signalQuality;
    private final Timer animationTimer;

    private int animationFrame;
    private int glitchFramesRemaining;
    private int glitchOffset;
    private int glitchPosition;
    private int glitchHeight;

    TransmissionImagePanel(@Nullable ImageIcon imageIcon) {
        this(imageIcon, TransmissionSignalQuality.REMOTE);
    }

    TransmissionImagePanel(@Nullable ImageIcon imageIcon, TransmissionSignalQuality signalQuality) {
        sourceImage = createSourceImage(imageIcon);
        this.signalQuality = Objects.requireNonNull(signalQuality);

        int width = (sourceImage == null) ? 0 : sourceImage.getWidth();
        int height = (sourceImage == null) ? 0 : sourceImage.getHeight();
        Dimension imageSize = new Dimension(width, height);
        setMinimumSize(imageSize);
        setPreferredSize(imageSize);
        setMaximumSize(imageSize);
        setOpaque(false);

        animationTimer = new Timer(ANIMATION_DELAY, event -> advanceAnimation());
        animationTimer.setCoalesce(true);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (sourceImage != null) {
            animationTimer.start();
        }
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (sourceImage == null) {
            return;
        }

        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
              RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int width = getWidth();
        int height = getHeight();
        graphics2D.drawImage(sourceImage, 0, 0, width, height, null);
        paintSignalTint(graphics2D, width, height);

        if (glitchFramesRemaining > 0) {
            paintGlitchBand(graphics2D, width, height);
        }

        paintScanlines(graphics2D, width, height);
        paintSignalNoise(graphics2D, width, height);
        paintFrame(graphics2D, width, height);
        graphics2D.dispose();
    }

    private void advanceAnimation() {
        animationFrame++;

        if (glitchFramesRemaining > 0) {
            glitchFramesRemaining--;
        } else if (ThreadLocalRandom.current().nextInt(signalQuality.glitchInterval) == 0) {
            int height = getHeight();
            glitchFramesRemaining = ThreadLocalRandom.current().nextInt(signalQuality.glitchFrameMinimum,
                signalQuality.glitchFrameMaximum);
            glitchOffset = scaleForGUI(ThreadLocalRandom.current().nextInt(-signalQuality.glitchOffset,
                signalQuality.glitchOffset + 1));
            glitchHeight = max(1, scaleForGUI(ThreadLocalRandom.current().nextInt(
                signalQuality.glitchHeightMinimum,
                signalQuality.glitchHeightMaximum)));
            glitchPosition = (height <= glitchHeight)
                                   ? 0
                                   : ThreadLocalRandom.current().nextInt(height - glitchHeight);
        }

        repaint();
    }

    private void paintSignalTint(Graphics2D graphics2D, int width, int height) {
        graphics2D.setComposite(AlphaComposite.SrcOver.derive(signalQuality.tintOpacity));
        graphics2D.setColor(ImmersiveDialogStyle.getSignalColor());
        graphics2D.fillRect(0, 0, width, height);
        graphics2D.setComposite(AlphaComposite.SrcOver);
    }

    private void paintGlitchBand(Graphics2D graphics2D, int width, int height) {
        Graphics2D glitchGraphics = (Graphics2D) graphics2D.create();
        glitchGraphics.setClip(0, glitchPosition, width, glitchHeight);
        glitchGraphics.drawImage(sourceImage, glitchOffset, 0, width, height, null);
        glitchGraphics.setComposite(AlphaComposite.SrcOver.derive(signalQuality.glitchLineOpacity));
        glitchGraphics.setColor(ImmersiveDialogStyle.getInformationColor());
        glitchGraphics.fillRect(0, glitchPosition, width, max(1, scaleForGUI(1)));
        glitchGraphics.dispose();
    }

    private void paintScanlines(Graphics2D graphics2D, int width, int height) {
        graphics2D.setComposite(AlphaComposite.SrcOver.derive(signalQuality.scanlineOpacity));
        graphics2D.setColor(Color.BLACK);
        int scanlineGap = scaleForGUI(signalQuality.scanlineGap);
        for (int y = scanlineGap - 1; y < height; y += scanlineGap) {
            graphics2D.drawLine(0, y, width, y);
        }
        graphics2D.setComposite(AlphaComposite.SrcOver);
    }

    private void paintSignalNoise(Graphics2D graphics2D, int width, int height) {
        if ((width == 0) || (height == 0)) {
            return;
        }

        int noiseSeed = animationFrame * 1_103_515_245;
        graphics2D.setComposite(AlphaComposite.SrcOver.derive(signalQuality.noiseOpacity));
        graphics2D.setColor(ImmersiveDialogStyle.getSignalColor().brighter());
        for (int mark = 0; mark < signalQuality.noiseMarks; mark++) {
            noiseSeed = noiseSeed * 1_103_515_245 + 12_345;
            int x = Math.floorMod(noiseSeed, width);
            noiseSeed = noiseSeed * 1_103_515_245 + 12_345;
            int y = Math.floorMod(noiseSeed, height);
            int markWidth = max(1, Math.floorMod(noiseSeed >>> 8, scaleForGUI(8)));
            graphics2D.fillRect(x, y, markWidth, max(1, scaleForGUI(1)));
        }
        graphics2D.setComposite(AlphaComposite.SrcOver);
    }

    private void paintFrame(Graphics2D graphics2D, int width, int height) {
        int inset = max(1, scaleForGUI(1));
        int cornerLength = scaleForGUI(14);

        graphics2D.setColor(ImmersiveDialogStyle.getSignalColor());
        graphics2D.drawRect(inset, inset, width - inset * 2 - 1, height - inset * 2 - 1);

        graphics2D.drawLine(inset, inset, inset + cornerLength, inset);
        graphics2D.drawLine(inset, inset, inset, inset + cornerLength);
        graphics2D.drawLine(width - inset - cornerLength, height - inset - 1, width - inset - 1, height - inset - 1);
        graphics2D.drawLine(width - inset - 1, height - inset - cornerLength, width - inset - 1, height - inset - 1);
    }

    private static @Nullable BufferedImage createSourceImage(@Nullable ImageIcon imageIcon) {
        if ((imageIcon == null) || (imageIcon.getIconWidth() <= 0) || (imageIcon.getIconHeight() <= 0)) {
            return null;
        }

        BufferedImage image = new BufferedImage(imageIcon.getIconWidth(), imageIcon.getIconHeight(),
              BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = image.createGraphics();
        imageIcon.paintIcon(null, graphics2D, 0, 0);
        graphics2D.dispose();
        return image;
    }

    private static int max(int firstValue, int secondValue) {
        return Math.max(firstValue, secondValue);
    }
}
