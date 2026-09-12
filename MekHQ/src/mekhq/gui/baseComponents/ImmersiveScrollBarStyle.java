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
package mekhq.gui.baseComponents;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.plaf.basic.BasicScrollBarUI;

import megamek.client.ui.util.UIUtil;

public final class ImmersiveScrollBarStyle {
    private ImmersiveScrollBarStyle() {
    }

    public static void apply(JScrollBar scrollBar, Color background, Color border, Color thumb,
          Color rolloverThumb, int width) {
        scrollBar.setUI(new ImmersiveScrollBarUI(background, border, thumb, rolloverThumb, width));
        scrollBar.setPreferredSize(new Dimension(width, 0));
        scrollBar.setUnitIncrement(UIUtil.scaleForGUI(16));
        scrollBar.setOpaque(true);
        scrollBar.setBackground(background);
        scrollBar.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, border));
    }

    private static final class ImmersiveScrollBarUI extends BasicScrollBarUI {
        private final Color background;
        private final Color border;
        private final Color rolloverThumb;
        private final int width;

        private ImmersiveScrollBarUI(Color background, Color border, Color thumb, Color rolloverThumb, int width) {
            this.background = background;
            this.border = border;
            this.thumbColor = thumb;
            this.rolloverThumb = rolloverThumb;
            this.width = width;
        }

        @Override
        protected void configureScrollBarColors() {
            trackColor = background;
            thumbHighlightColor = rolloverThumb;
            thumbDarkShadowColor = border;
            thumbLightShadowColor = border;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createHiddenScrollButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createHiddenScrollButton();
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
            graphics.setColor(background);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            if (bounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D thumbGraphics = (Graphics2D) graphics.create();
            thumbGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            thumbGraphics.setColor(isThumbRollover() ? rolloverThumb : thumbColor);
            int inset = Math.max(1, UIUtil.scaleForGUI(2));
            int arc = Math.max(2, UIUtil.scaleForGUI(3));
            thumbGraphics.fillRoundRect(bounds.x + inset, bounds.y + inset,
                  Math.max(1, bounds.width - (inset * 2)), Math.max(1, bounds.height - (inset * 2)), arc, arc);
            thumbGraphics.dispose();
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(width, UIUtil.scaleForGUI(28));
        }

        private static JButton createHiddenScrollButton() {
            JButton button = new JButton();
            Dimension size = new Dimension(0, 0);
            button.setMinimumSize(size);
            button.setPreferredSize(size);
            button.setMaximumSize(size);
            return button;
        }
    }
}
