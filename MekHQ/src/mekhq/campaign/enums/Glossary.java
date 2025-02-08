/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public enum Glossary {
    // region Enum Declarations
    PRISONER_CAPACITY,
    REPUTATION;
    // endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    // region Getters
    /**
     * Retrieves the title associated with this enum constant.
     *
     * <p>
     * This method constructs a resource key by appending {@code ".title"} to the enum constant's
     * name and looks up the corresponding title from the resource bundle. The title is then
     * formatted and returned as a string.
     * </p>
     *
     * @return A formatted string representing the title for this enum constant,
     *         fetched from the resource bundle.
     */
    public String getTitle() {
        final String RESOURCE_KEY = name() + ".title";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the description associated with this enum constant.
     *
     * <p>
     * This method constructs a resource key by appending {@code ".description"} to the enum constant's
     * name and looks up the corresponding description from the resource bundle. The description
     * is then formatted and returned as a string.
     * </p>
     *
     * @return A formatted string representing the description for this enum constant,
     *         fetched from the resource bundle.
     */
    public String getDescription() {
        final String RESOURCE_KEY = name() + ".description";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    @Override
    public String toString() {
        return getTitle();
    }
}
