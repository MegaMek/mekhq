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
package mekhq.campaign.parts.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.Map;

import megamek.common.enums.AvailabilityValue;

/**
 * ATOWLegalityRating represents the A Time of War (AToW) legality classes (A–F) used in MekHQ to influence acquisition
 * difficulty and pricing.
 *
 * <p><b>Source:</b> ATOW pg 255-256.</p>
 */
public enum ATOWLegalityRating {
    A("A", Map.of(AvailabilityValue.A, 0.5,
          AvailabilityValue.B, 1.0,
          AvailabilityValue.C, 1.25,
          AvailabilityValue.D, 1.5,
          AvailabilityValue.E, 2.0,
          AvailabilityValue.F, 4.0)),
    B("B", Map.of(AvailabilityValue.A, 1.0,
          AvailabilityValue.B, 2.0,
          AvailabilityValue.C, 2.5,
          AvailabilityValue.D, 2.5, // ATOW states this is 2.0, but that's clearly a typo
          AvailabilityValue.E, 3.0,
          AvailabilityValue.F, 6.0)),
    C("C", Map.of(AvailabilityValue.A, 2.0,
          AvailabilityValue.B, 3.0,
          AvailabilityValue.C, 4.0,
          AvailabilityValue.D, 4.0, // ATOW states this is 3.0, but that's clearly a typo
          AvailabilityValue.E, 4.0,
          AvailabilityValue.F, 9.0)),
    D("D", Map.of(AvailabilityValue.A, 3.0,
          AvailabilityValue.B, 4.0,
          AvailabilityValue.C, 5.0,
          AvailabilityValue.D, 6.0,
          AvailabilityValue.E, 8.0,
          AvailabilityValue.F, 14.0)),
    E("E", Map.of(AvailabilityValue.A, 5.0,
          AvailabilityValue.B, 6.0,
          AvailabilityValue.C, 7.0,
          AvailabilityValue.D, 10.0,
          AvailabilityValue.E, 15.0,
          AvailabilityValue.F, 21.0)),
    F("F", Map.of(AvailabilityValue.A, 7.0,
          AvailabilityValue.B, 9.0,
          AvailabilityValue.C, 11.0,
          AvailabilityValue.D, 13.0,
          AvailabilityValue.E, 20.0,
          AvailabilityValue.F, 30.0));

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ATOWLegalityRating";

    /** Short lookup key ("A"–"F") used in resource bundle keys. */
    private final String lookupName;
    /**
     * Map of availability grades to black market price multipliers for this legality rating.
     */
    private final Map<AvailabilityValue, Double> blackMarketMultiplier;
    /** Localized description for UI/tooltips, resolved at construction. */
    private final String description;

    /**
     * Creates a new legality rating definition.
     *
     * @param lookupName            short name used for localization keys ("A"–"F")
     * @param blackMarketMultiplier mapping of {@link AvailabilityValue} to price multipliers when purchasing on the
     *                              black market
     */
    ATOWLegalityRating(String lookupName, Map<AvailabilityValue, Double> blackMarketMultiplier) {
        this.lookupName = lookupName;
        this.blackMarketMultiplier = blackMarketMultiplier;
        description = generateDescription();
    }

    /**
     * Resolves and returns the localized description for this rating from the resource bundle. The value is cached in
     * {@link #description} at construction time to avoid repeated lookups.
     *
     * @return localized description text for this legality rating
     */
    private String generateDescription() {
        return getTextAt(RESOURCE_BUNDLE, "ATOWLegalityRating." + lookupName + ".description");
    }

    /**
     * Returns the enum constant name. This is preferred over the display text
     * for logging/debugging, as UI layers should use {@link #getDescription()}.
     *
     * @return the enum constant identifier (e.g., "A", "B", ...)
     */
    @Override
    public String toString() {
        return this.name();
    }

    /**
     * Gets the localized description for this legality rating for display in UI components and tooltips.
     *
     * @return human-readable, localized description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Returns the black market price multiplier to apply for the given {@link AvailabilityValue} under this legality
     * rating.
     *
     * <p>Callers should ensure the provided availability value is present for
     * this rating. All ratings in this enum define multipliers for {@code AvailabilityValue.A} through
     * {@code AvailabilityValue.F}.</p>
     *
     * @param availabilityValue the item's availability rating
     *
     * @return the price multiplier associated with the availability rating
     *
     * @throws NullPointerException if {@code availabilityValue} is null
     */
    public double getBlackMarketMultiplier(AvailabilityValue availabilityValue) {
        return this.blackMarketMultiplier.get(availabilityValue);
    }
}
