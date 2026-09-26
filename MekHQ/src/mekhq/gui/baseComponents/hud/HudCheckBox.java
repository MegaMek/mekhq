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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER_CYAN;
import static mekhq.gui.baseComponents.hud.HudStyle.GROUND;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JCheckBox;

/**
 * A HUD checkbox: a hairline cyan box, filled with the accent colour and ticked when selected, beside a muted label
 * that brightens when the box is ticked.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudCheckBox extends JCheckBox {
    /**
     * @param text the checkbox's label
     */
    public HudCheckBox(String text) {
        super(text);
        setOpaque(false);
        setFocusPainted(false);
        setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        setBorder(BorderFactory.createEmptyBorder(scaleForGUI(2), 0, scaleForGUI(2), 0));
        setIconTextGap(scaleForGUI(6));
        setIcon(new BoxIcon());
        refreshForeground();
        addItemListener(event -> refreshForeground());
    }

    private void refreshForeground() {
        setForeground(isSelected() ? TEXT : TEXT_MUTED);
    }

    /** The checkbox's box: hollow when clear, accent-filled with a tick when selected. */
    private static final class BoxIcon implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = getIconWidth();
                boolean isSelected = (component instanceof AbstractButton button) && button.isSelected();
                g2.setColor(isSelected ? ACCENT : SURFACE_DEEP);
                g2.fillRect(x, y, size - 1, size - 1);
                g2.setColor(BORDER_CYAN);
                g2.drawRect(x, y, size - 1, size - 1);
                if (isSelected) {
                    g2.setColor(GROUND);
                    g2.setStroke(new BasicStroke(scaleForGUI(2)));
                    int inset = size / 4;
                    g2.drawLine(x + inset, y + size / 2, x + size / 2 - 1, y + size - inset - 1);
                    g2.drawLine(x + size / 2 - 1, y + size - inset - 1, x + size - inset, y + inset);
                }
            } finally {
                g2.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return scaleForGUI(13);
        }

        @Override
        public int getIconHeight() {
            return scaleForGUI(13);
        }
    }
}
