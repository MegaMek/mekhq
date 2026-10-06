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
package mekhq.gui.dialog.advancedCharacterBuilder.lifePathBuilder;

import megamek.logging.MMLogger;

/**
 * Shared helpers for the Life Path picker dialogs.
 *
 * <p>The pickers build their option rows from values that were loaded out of a Life Path file. Those values are not
 * guaranteed to fit the bounds the picker offers, because a file can be hand-edited or can predate a change to a
 * trait, skill or category range. This class holds the guards that every picker needs as a result.</p>
 *
 * @since 0.50.11
 */
final class LifePathPickerUtilities {
    private static final MMLogger LOGGER = MMLogger.create(LifePathPickerUtilities.class);

    private LifePathPickerUtilities() {
        // Utility class: not instantiable.
    }

    /**
     * Brings a stored value inside the bounds a spinner accepts, warning when it had to move.
     *
     * <p>{@link javax.swing.SpinnerNumberModel} throws {@link IllegalArgumentException} when its initial value sits
     * outside its own minimum and maximum. Building a model straight from a stored value therefore takes the whole
     * picker down, and the row the author wanted to see disappears with it. Clamping keeps the row visible and
     * records what happened in the log instead.</p>
     *
     * @param storedValue  the value read from the Life Path being edited
     * @param minimum      the lowest value the spinner will offer
     * @param maximum      the highest value the spinner will offer
     * @param rowLabel     the display name of the row, used only for the warning
     *
     * @return {@code storedValue} when it is already in range, otherwise the nearest bound
     *
     * @since 0.50.11
     */
    static int clampSpinnerValue(int storedValue, int minimum, int maximum, String rowLabel) {
        int clampedValue = Math.clamp(storedValue, minimum, maximum);

        if (clampedValue != storedValue) {
            LOGGER.warn("Stored value {} for [{}] is outside the permitted range {} to {}. Using {} instead.",
                  storedValue, rowLabel, minimum, maximum, clampedValue);
        }

        return clampedValue;
    }
}
