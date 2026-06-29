/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import org.jspecify.annotations.NonNull;

/**
 * The level of a Hiring Hall as defined in CamOps (4th printing). Used to determine various modifiers related to
 * contract generation.
 *
 * <p>All modifiers are taken from CamOps pg 39 Rev 5th printing.</p>
 */
public enum HiringHallLevel {
    NONE("NONE", -3, -2, -2),
    QUESTIONABLE("QUESTIONABLE", 0, -2, -2),
    MINOR("MINOR", 1, 0, 0),
    STANDARD("STANDARD", 2, 1, 1),
    GREAT("GREAT", 3, 2, 2);

    private final String lookupName;
    private final String label;
    private final String tooltip;
    private final int offersModifier;
    private final int employerModifier;
    private final int missionModifier;

    private final String RESOURCE_BUNDLE = "mekhq.resources.HiringHallLevel";

    // region Constructors
    HiringHallLevel(final String lookupName, final int offersModifier, final int employerModifier,
          final int missionModifier) {
        this.lookupName = lookupName;
        this.label = generateLabel(lookupName);
        this.tooltip = generateTooltip(lookupName);
        this.offersModifier = offersModifier;
        this.employerModifier = employerModifier;
        this.missionModifier = missionModifier;
    }

    private @NonNull String generateTooltip(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "HiringHallLevel." + lookupName + ".tooltip");
    }

    private @NonNull String generateLabel(String lookupName) {
        return getTextAt(RESOURCE_BUNDLE, "HiringHallLevel." + lookupName + ".name");
    }

    public String getLabel() {
        return label;
    }

    public String getTooltip() {
        return tooltip;
    }

    public int getMissionModifier() {
        return missionModifier;
    }

    public int getEmployerModifier() {
        return employerModifier;
    }

    public int getOffersModifier() {
        return offersModifier;
    }

    public boolean isNone() {
        return this == NONE;
    }
}
