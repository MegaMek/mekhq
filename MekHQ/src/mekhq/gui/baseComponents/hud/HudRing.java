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
package mekhq.gui.baseComponents.hud;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.translucent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * A small status ring drawn in a status colour, as the deployment wizard marks each row's readiness. A filled ring is
 * tinted inside with its colour.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudRing extends JComponent {
    private Color color = TEXT_MUTED;
    private boolean isFilled;

    public HudRing() {
        Dimension size = new Dimension(scaleForGUI(12), scaleForGUI(12));
        setPreferredSize(size);
        setMinimumSize(size);
    }

    /**
     * Sets the ring's colour.
     *
     * @param color    the status colour
     * @param isFilled {@code true} to tint the inside of the ring
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setColor(Color color, boolean isFilled) {
        this.color = color;
        this.isFilled = isFilled;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int diameter = Math.min(scaleForGUI(11), Math.min(getWidth(), getHeight()) - 2);
            int x = (getWidth() - diameter) / 2;
            int y = (getHeight() - diameter) / 2;
            if (isFilled) {
                g2.setColor(translucent(color, 64));
                g2.fillOval(x, y, diameter, diameter);
            }
            g2.setStroke(new BasicStroke(scaleForGUI(2)));
            g2.setColor(color);
            g2.drawOval(x, y, diameter, diameter);
        } finally {
            g2.dispose();
        }
    }
}
