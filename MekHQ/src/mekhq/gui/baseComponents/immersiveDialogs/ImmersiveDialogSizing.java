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

/** Calculates content-aware dialog and message viewport heights. */
final class ImmersiveDialogSizing {
    private static final double USABLE_SCREEN_FRACTION = 0.9;

    private ImmersiveDialogSizing() {
    }

    static SizingResult calculate(int naturalDialogHeight, int naturalViewportHeight, int minimumViewportHeight,
          int usableScreenHeight) {
        int maximumDialogHeight = Math.max(1, (int) Math.floor(usableScreenHeight * USABLE_SCREEN_FRACTION));
        int dialogHeight = naturalDialogHeight;
        int viewportHeight = naturalViewportHeight;
        if (naturalDialogHeight > maximumDialogHeight) {
            int overflow = naturalDialogHeight - maximumDialogHeight;
            viewportHeight = Math.max(minimumViewportHeight, naturalViewportHeight - overflow);
            dialogHeight = Math.min(maximumDialogHeight,
                  naturalDialogHeight - (naturalViewportHeight - viewportHeight));
        }

        int nonMessageHeight = Math.max(0, naturalDialogHeight - naturalViewportHeight);
        int minimumDialogHeight = Math.min(dialogHeight,
              nonMessageHeight + Math.min(naturalViewportHeight, minimumViewportHeight));
        return new SizingResult(dialogHeight, viewportHeight, minimumDialogHeight);
    }

    record SizingResult(int dialogHeight, int viewportHeight, int minimumDialogHeight) {
    }
}
