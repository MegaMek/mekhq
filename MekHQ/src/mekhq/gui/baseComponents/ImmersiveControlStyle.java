/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 */
package mekhq.gui.baseComponents;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.swing.BorderFactory;
import javax.swing.JButton;

import megamek.client.ui.util.UIUtil;

final class ImmersiveControlStyle {
    static final int CONTROL_HEIGHT = UIUtil.scaleForGUI(30);
    static final Color TEXT = new Color(218, 231, 235);
    static final Color MUTED_TEXT = new Color(132, 153, 161);
    static final Color ACCENT = new Color(65, 210, 224);
    static final Color SELECTED = new Color(235, 166, 66);
    static final Color BORDER = new Color(35, 66, 82);
    static final Color CONTROL_BACKGROUND = new Color(12, 29, 42);
    static final Color INPUT_BACKGROUND = new Color(17, 64, 78);
    static final Color DISABLED_BACKGROUND = new Color(19, 29, 34);
    static final Color ACTIVE_BACKGROUND = new Color(18, 45, 56);

    private ImmersiveControlStyle() {
    }

    static final class ArrowButton extends JButton {
        private final boolean pointsUp;
        private final boolean leadingDivider;

        ArrowButton(boolean pointsUp) {
            this(pointsUp, false);
        }

        ArrowButton(boolean pointsUp, boolean leadingDivider) {
            this.pointsUp = pointsUp;
            this.leadingDivider = leadingDivider;
            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setPreferredSize(new Dimension(UIUtil.scaleForGUI(28), 1));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            Color background = getModel().isPressed() ? BORDER
                  : getModel().isRollover() ? ACTIVE_BACKGROUND : CONTROL_BACKGROUND;
            graphics2D.setColor(isEnabled() ? background : DISABLED_BACKGROUND);
            int fillX = leadingDivider ? 1 : 0;
            graphics2D.fillRect(fillX, 1, Math.max(0, getWidth() - fillX - 1),
                Math.max(0, getHeight() - 2));
            if (leadingDivider) {
                graphics2D.setColor(BORDER);
                graphics2D.drawLine(0, 0, 0, getHeight() - 1);
            }

            int centerX = getWidth() / 2;
            int centerY = getHeight() / 2;
            int halfWidth = Math.max(2, UIUtil.scaleForGUI(4));
            int halfHeight = Math.max(1, UIUtil.scaleForGUI(2));
            Polygon arrow = pointsUp
                  ? new Polygon(new int[] { centerX - halfWidth, centerX + halfWidth, centerX },
                        new int[] { centerY + halfHeight, centerY + halfHeight, centerY - halfHeight }, 3)
                  : new Polygon(new int[] { centerX - halfWidth, centerX + halfWidth, centerX },
                        new int[] { centerY - halfHeight, centerY - halfHeight, centerY + halfHeight }, 3);
            graphics2D.setColor(isEnabled() ? TEXT : MUTED_TEXT);
            graphics2D.fillPolygon(arrow);
            graphics2D.dispose();
        }
    }
}
