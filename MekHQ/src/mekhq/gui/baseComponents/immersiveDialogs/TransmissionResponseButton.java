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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;
import javax.swing.Timer;

/** Response button with a restrained transmission scan on hover or keyboard focus. */
final class TransmissionResponseButton extends JButton {
    private static final int FRAME_DELAY = 16;
    static final long SCAN_NANOS_PER_PIXEL = 7_000_000L;
    private static final double FADE_START_PROGRESS = 0.80;
    private static final int TRAIL_ALPHA = 52;
    private static final int LINE_ALPHA = 112;
    private static final int TRAIL_WIDTH = 48;

    private final Timer scanTimer;

    private long scanStartNanos;
    private double scanProgress = 1.0;
    private boolean scanActive;
    private boolean scanForward = true;
    private boolean repeatScan;

    TransmissionResponseButton(String text) {
        super(text);
        setRolloverEnabled(true);

        scanTimer = new Timer(FRAME_DELAY, event -> advanceScan());
        scanTimer.setCoalesce(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                startScan(true, System.nanoTime());
            }

            @Override
            public void mouseExited(MouseEvent event) {
                completeScan();
            }
        });
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                if (isKeyboardTraversal(event)) {
                    startScan(false, System.nanoTime());
                }
            }

            @Override
            public void focusLost(FocusEvent event) {
                if (!getModel().isRollover()) {
                    completeScan();
                }
            }
        });
        addPropertyChangeListener("enabled", event -> {
            if (!isEnabled()) {
                completeScan();
            }
        });
    }

    @Override
    public void removeNotify() {
        scanTimer.stop();
        scanActive = false;
        scanProgress = 1.0;
        repeatScan = false;
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (!scanActive || !isEnabled()) {
            return;
        }

        int inset = Math.max(1, scaleForGUI(2));
        int innerWidth = getWidth() - inset * 2;
        int innerHeight = getHeight() - inset * 2;
        if ((innerWidth <= 0) || (innerHeight <= 0)) {
            return;
        }

        int scanX = inset + (int) Math.round((innerWidth - 1) * scanProgress);
        int trailSpace = scanForward
                       ? scanX - inset
                       : inset + innerWidth - 1 - scanX;
        int trailWidth = Math.min(scaleForGUI(TRAIL_WIDTH), Math.max(0, trailSpace));
        double opacity = scanOpacity();
        Color signalColor = ImmersiveDialogStyle.getSignalColor();

        Graphics2D graphics2D = (Graphics2D) graphics.create();
        int arc = scaleForGUI(6);
        graphics2D.clip(new RoundRectangle2D.Float(inset, inset, innerWidth, innerHeight, arc, arc));

        if (trailWidth > 0) {
            int trailStart = scanForward ? scanX - trailWidth : scanX;
            Color transparentSignal = withAlpha(signalColor, 0);
            Color trailSignal = withAlpha(signalColor, (int) Math.round(TRAIL_ALPHA * opacity));
            graphics2D.setPaint(scanForward
                                      ? new GradientPaint(trailStart, 0, transparentSignal, scanX, 0, trailSignal)
                                      : new GradientPaint(scanX, 0, trailSignal,
                                            scanX + trailWidth, 0, transparentSignal));
            graphics2D.fillRect(trailStart, inset, trailWidth, innerHeight);
        }

        graphics2D.setColor(withAlpha(signalColor, (int) Math.round(LINE_ALPHA * opacity)));
        graphics2D.fillRect(scanX, inset, Math.max(1, scaleForGUI(1)), innerHeight);
        graphics2D.dispose();
    }

    void startScan() {
        startScan(false, System.nanoTime());
    }

    void startScan(boolean repeat, long startNanos) {
        if (!isEnabled()) {
            return;
        }
        if (scanActive) {
            repeatScan |= repeat;
            return;
        }

        scanActive = true;
        scanForward = true;
        repeatScan = repeat;
        scanProgress = 0;
        scanStartNanos = startNanos;
        scanTimer.start();
        repaint();
    }

    void completeScan() {
        scanTimer.stop();
        scanActive = false;
        scanForward = true;
        repeatScan = false;
        scanProgress = 1.0;
        repaint();
    }

    boolean isScanRunning() {
        return scanActive;
    }

    boolean isScanMovingForward() {
        return scanForward;
    }

    double getScanProgress() {
        return scanProgress;
    }

    private void advanceScan() {
        advanceScan(System.nanoTime());
    }

    void advanceScan(long nowNanos) {
        long elapsedNanos = Math.max(0, nowNanos - scanStartNanos);
        long passDurationNanos = getScanPassDurationNanos();
        if (!repeatScan) {
            scanForward = true;
            scanProgress = Math.min(1.0, (double) elapsedNanos / passDurationNanos);
            if (scanProgress >= 1.0) {
                completeScan();
            } else {
                repaint();
            }
            return;
        }

        long passIndex = elapsedNanos / passDurationNanos;
        double passProgress = (double) (elapsedNanos % passDurationNanos) / passDurationNanos;
        scanForward = (passIndex % 2) == 0;
        scanProgress = scanForward ? passProgress : 1.0 - passProgress;
        if (!getModel().isRollover()) {
            completeScan();
        } else {
            repaint();
        }
    }

    long getScanPassDurationNanos() {
        return Math.max(1, getScanTravelDistance()) * SCAN_NANOS_PER_PIXEL;
    }

    int getScanTravelDistance() {
        int inset = Math.max(1, scaleForGUI(2));
        return Math.max(1, getWidth() - inset * 2 - 1);
    }

    private double scanOpacity() {
        if (repeatScan) {
            return 1.0;
        }
        if (scanProgress <= FADE_START_PROGRESS) {
            return 1.0;
        }
        return 1.0 - (scanProgress - FADE_START_PROGRESS) / (1.0 - FADE_START_PROGRESS);
    }

    private static boolean isKeyboardTraversal(FocusEvent event) {
        return switch (event.getCause()) {
            case TRAVERSAL_FORWARD, TRAVERSAL_BACKWARD, TRAVERSAL_UP, TRAVERSAL_DOWN -> true;
            default -> false;
        };
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
