/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import megamek.client.ui.util.UIUtil;

/**
 * Specifies width presets for immersive dialog windows in the user interface.
 *
 * <p>Each enum constant is associated with a specific pixel width (scaled for GUI) that can be used to control the
 * layout and appearance of dialogs throughout the application.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum ImmersiveDialogWidth {
    /**
     * Small preset width, suitable for most immersive dialogs.
     */
    SMALL(400),

    /**
     * Medium preset width, intended for larger dialogs.
     */
    MEDIUM(600),

    /**
     * Large preset width, intended for dialogs that require significantly more space.
     */
    LARGE(800);

    /**
     * The scaled pixel width for this dialog size.
     */
    private final int width;

    /**
     * Constructs an {@code ImmersiveDialogWidth} with the given pixel width.
     *
     * @param width the base width in pixels before scaling
     */
    ImmersiveDialogWidth(int width) {
        this.width = width;
    }

    /**
     * Returns the pixel width (scaled for GUI) of this dialog size.
     *
     * @return the width in pixels after scaling
     */
    public int getWidth() {
        return UIUtil.scaleForGUI(width);
    }
}

