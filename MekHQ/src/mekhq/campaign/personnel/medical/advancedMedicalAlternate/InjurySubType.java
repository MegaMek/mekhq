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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

/**
 * Enumeration representing the subcategories of injuries used in the {@code Advanced Medical Alternate} system. These
 * subtypes distinguish different sources or contexts of injury for medical treatment, healing, and replacement logic.
 *
 * <p>The supported subtypes include:</p>
 * <ul>
 *   <li>{@link #NORMAL} — Standard or conventional injuries.</li>
 *   <li>{@link #BURN} — Thermal or chemical burn injuries.</li>
 *   <li>{@link #DISEASE} — Injuries or conditions caused by illness or infection.</li>
 *   <li>{@link #PROSTHETIC} — Artificial or mechanical replacements for body parts.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.10
 */
public enum InjurySubType {
    /** Standard or conventional injuries. */
    NORMAL,

    /** Thermal or chemical burns. */
    BURN,

    /** Illness or infection-related conditions. */
    DISEASE,

    /** Mechanical or artificial body replacements. */
    PROSTHETIC;

    /**
     * Checks whether this subtype represents a normal injury.
     *
     * @return {@code true} if this subtype is {@link #NORMAL}, otherwise {@code false}.
     */
    public boolean isNormal() {
        return this == NORMAL;
    }

    /**
     * Checks whether this subtype represents a burn injury.
     *
     * @return {@code true} if this subtype is {@link #BURN}, otherwise {@code false}.
     */
    public boolean isBurn() {
        return this == BURN;
    }

    /**
     * Checks whether this subtype represents a disease-related injury.
     *
     * @return {@code true} if this subtype is {@link #DISEASE}, otherwise {@code false}.
     */
    public boolean isDisease() {
        return this == DISEASE;
    }

    /**
     * Checks whether this subtype represents a prosthetic or artificial body part.
     *
     * @return {@code true} if this subtype is {@link #PROSTHETIC}, otherwise {@code false}.
     */
    public boolean isProsthetic() {
        return this == PROSTHETIC;
    }
}
