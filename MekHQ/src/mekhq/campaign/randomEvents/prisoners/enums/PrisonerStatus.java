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

import megamek.logging.MMLogger;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

public enum PrisonerStatus {
    // region Enum Declarations
    FREE,
    PRISONER,
    PRISONER_DEFECTOR,
    BONDSMAN,
    BECOMING_BONDSMAN;
    // endregion Enum Declarations

    final private String RESOURCE_BUNDLE = "mekhq.resources.PrisonerCaptureStyle";

    // region Getters
    public String getLabel() {
        final String RESOURCE_KEY = name() + ".label";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }

    public String getTitleExtension() {
        final String RESOURCE_KEY = name() + ".titleExtension";

        return getFormattedTextAt(RESOURCE_BUNDLE, RESOURCE_KEY);
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isFree() {
        return this == FREE;
    }

    public boolean isFreeOrBondsman() {
        return isFree() || isBondsman();
    }

    public boolean isPrisoner() {
        return this == PRISONER;
    }

    public boolean isPrisonerDefector() {
        return this == PRISONER_DEFECTOR;
    }

    public boolean isBecomingBondsman() {
        return this == BECOMING_BONDSMAN;
    }

    public boolean isBondsman() {
        return this == BONDSMAN;
    }

    public boolean isCurrentPrisoner() {
        return isPrisoner() || isPrisonerDefector();
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    /**
     * @param text The saved value to parse, either the older magic number save
     *             format or the
     *             PrisonerStatus.name() value
     * @return the Prisoner Status in question
     */
    public static PrisonerStatus parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {
            MMLogger.create(PrisonerStatus.class)
                .error("Unable to parse {} into a PrisonerStatus. Returning {}.", text, FREE);
            return FREE;
        }
    }
    // endregion File I/O

    @Override
    public String toString() {
        return getLabel();
    }
}
