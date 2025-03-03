/*
 * Copyright (c) 2020-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.randomEvents.prisoners.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Defines the available styles for handling prisoner capture.
 *
 * <p>The {@code PrisonerCaptureStyle} enumeration specifies how prisoners are captured during a
 * campaign. The styles include:</p>
 * <ul>
 *     <li>{@code NONE} - No capture mechanics are applied.</li>
 *     <li>{@code CAMPAIGN_OPERATIONS} - Capture mechanics follow the Campaign Operations rules.</li>
 *     <li>{@code MEKHQ} - Capture mechanics follow MekHQ-specific rules.</li>
 * </ul>
 * <p>This enumeration provides utility methods for interaction, including fetching labels and
 * tooltips for display purposes and helper methods to identify the capture style.</p>
 */
public enum PrisonerCaptureStyle {
    //region Enum Declarations
    NONE, CAMPAIGN_OPERATIONS, MEKHQ;
    //endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources." + getClass().getSimpleName();

    //region Getters
    /**
     * Retrieves the localized label for the current capture style.
     *
     * <p>The label is fetched from the resource bundle and is used for user interface elements
     * such as drop-down menus or informational displays.</p>
     *
     * @return A {@link String} containing the localized label for the capture style.
     */
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    /**
     * Retrieves the localized tooltip for the current capture style.
     *
     * <p>The tooltip provides explanatory text for the capture style, enhancing its
     * description in the user interface.</p>
     *
     * @return A {@link String} containing the localized tooltip for the capture style.
     */
    public String getTooltip() {
        final String RESOURCE_KEY = name() + ".tooltip";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }
    //endregion Getters

    //region Boolean Comparison Methods
    /**
     * Determines if the current capture style is {@code NONE}.
     *
     * @return {@code true} if the current style is {@code NONE}, otherwise {@code false}.
     */
    public boolean isNone() {
        return this == NONE;
    }

    /**
     * Determines if the current capture style is {@code CAMPAIGN_OPERATIONS}.
     *
     * @return {@code true} if the current style is {@code CAMPAIGN_OPERATIONS}, otherwise {@code false}.
     */
    public boolean isCampaignOperations() {
        return this == CAMPAIGN_OPERATIONS;
    }

    /**
     * Determines if the current capture style is {@code MEKHQ}.
     *
     * @return {@code true} if the current style is {@code MEKHQ}, otherwise {@code false}.
     */
    public boolean isMekHQ() {
        return this == MEKHQ;
    }
    //endregion Boolean Comparison Methods

    /**
     * Converts the capture style to a localized label for display.
     *
     * <p>This method overrides the default {@code toString()} implementation to return the
     * label representation of the capture style instead of its name.</p>
     *
     * @return A {@link String} containing the localized label of the capture style.
     */
    @Override
    public String toString() {
        return getLabel();
    }
}
