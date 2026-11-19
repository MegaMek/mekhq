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
package mekhq.gui.utilities;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;

import jakarta.annotation.Nullable;

/**
 * Helpers for sizing and placing windows.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class WindowUtilities {
    private WindowUtilities() {}

    /**
     * Centers a packed window on a reference component and shrinks it, and its minimum size, so it fits the usable area
     * of that component's screen, never slipping beneath the taskbar.
     *
     * <p>The screen bounds and insets come from the reference's {@link GraphicsConfiguration}, so the result honors the
     * display's resolution, the operating system's DPI scaling, and multi-monitor setups. With no reference, the
     * default screen is used and the window is centered on it.</p>
     *
     * @param window    the window, already packed
     * @param reference the component to center on, or {@code null} for the default screen
     */
    public static void fitToScreen(Window window, @Nullable Component reference) {
        GraphicsConfiguration graphicsConfiguration = (reference != null)
              ? reference.getGraphicsConfiguration()
              : null;
        if (graphicsConfiguration == null) {
            graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                                          .getDefaultScreenDevice()
                                          .getDefaultConfiguration();
        }

        final Rectangle screenBounds = graphicsConfiguration.getBounds();
        final Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);
        final int usableWidth = screenBounds.width - screenInsets.left - screenInsets.right;
        final int usableHeight = screenBounds.height - screenInsets.top - screenInsets.bottom;

        window.setMinimumSize(new Dimension(Math.min(window.getMinimumSize().width, usableWidth),
              Math.min(window.getMinimumSize().height, usableHeight)));
        window.setSize(Math.min(window.getWidth(), usableWidth), Math.min(window.getHeight(), usableHeight));

        // Center on the reference, then nudge the window so its edges stay out from under the taskbar
        window.setLocationRelativeTo(reference);
        final int minX = screenBounds.x + screenInsets.left;
        final int minY = screenBounds.y + screenInsets.top;
        final int clampedX = Math.max(minX, Math.min(window.getX(), minX + usableWidth - window.getWidth()));
        final int clampedY = Math.max(minY, Math.min(window.getY(), minY + usableHeight - window.getHeight()));
        window.setLocation(clampedX, clampedY);
    }
}
