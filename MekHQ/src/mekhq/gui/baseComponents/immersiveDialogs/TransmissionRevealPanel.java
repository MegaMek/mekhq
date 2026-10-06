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
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Reveals a transmission screen from a horizontal center line, like a CRT powering on. */
final class TransmissionRevealPanel extends JPanel {
    private static final int FRAME_DELAY = 16;
    private static final int START_DELAY = 160;
    private static final long ANIMATION_DURATION_NANOS = 480_000_000L;

    private final Timer animationTimer;

    private long animationStartNanos;
    private double revealProgress;

    TransmissionRevealPanel(JComponent content) {
        super(new BorderLayout());
        setOpaque(false);
        add(content, BorderLayout.CENTER);

        animationTimer = new Timer(FRAME_DELAY, event -> advanceAnimation());
        animationTimer.setInitialDelay(START_DELAY);
        animationTimer.setCoalesce(true);
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) {
                    startAnimation();
                } else {
                    animationTimer.stop();
                }
            }
        });
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }

    @Override
    public boolean contains(int xPosition, int yPosition) {
        return (revealProgress >= 1.0) && super.contains(xPosition, yPosition);
    }

    @Override
    protected boolean isPaintingOrigin() {
        return revealProgress < 1.0;
    }

    @Override
    protected void paintChildren(Graphics graphics) {
        if (revealProgress >= 1.0) {
            super.paintChildren(graphics);
            return;
        }

        double easedProgress = revealProgress * revealProgress * (3.0 - 2.0 * revealProgress);
        int visibleHeight = Math.max(1, (int) Math.round(getHeight() * easedProgress));
        int revealTop = (getHeight() - visibleHeight) / 2;
        int revealBottom = revealTop + visibleHeight - 1;

        Graphics2D contentGraphics = (Graphics2D) graphics.create();
        contentGraphics.clipRect(0, revealTop, getWidth(), visibleHeight);
        super.paintChildren(contentGraphics);
        contentGraphics.dispose();

        paintApertureEdges(graphics, revealTop, revealBottom);
    }

    void completeReveal() {
        animationTimer.stop();
        revealProgress = 1.0;
        repaint();
    }

    private void startAnimation() {
        if (revealProgress < 1.0) {
            revealProgress = 0;
            animationStartNanos = 0;
            animationTimer.restart();
            repaint();
        }
    }

    private void advanceAnimation() {
        long now = System.nanoTime();
        if (animationStartNanos == 0) {
            animationStartNanos = now;
        }

        revealProgress = Math.min(1.0, (double) (now - animationStartNanos) / ANIMATION_DURATION_NANOS);
        if (revealProgress >= 1.0) {
            completeReveal();
        } else {
            repaint();
        }
    }

    private void paintApertureEdges(Graphics graphics, int revealTop, int revealBottom) {
        Graphics2D graphics2D = (Graphics2D) graphics.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float remaining = (float) (1.0 - revealProgress);
        int glowHeight = Math.max(1, scaleForGUI(5));
        graphics2D.setComposite(AlphaComposite.SrcOver.derive(0.24f * remaining));
        graphics2D.setColor(ImmersiveDialogStyle.getSignalColor());
        graphics2D.fillRect(0, revealTop - glowHeight / 2, getWidth(), glowHeight);
        if (revealBottom != revealTop) {
            graphics2D.fillRect(0, revealBottom - glowHeight / 2, getWidth(), glowHeight);
        }

        graphics2D.setComposite(AlphaComposite.SrcOver.derive(0.95f * remaining));
        graphics2D.setStroke(new BasicStroke(Math.max(1, scaleForGUI(1))));
        graphics2D.drawLine(0, revealTop, getWidth(), revealTop);
        if (revealBottom != revealTop) {
            graphics2D.drawLine(0, revealBottom, getWidth(), revealBottom);
        }
        graphics2D.dispose();
    }
}
