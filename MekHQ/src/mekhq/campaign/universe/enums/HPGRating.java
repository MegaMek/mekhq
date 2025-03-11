/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.universe.enums;

import megamek.logging.MMLogger;

public enum HPGRating {
    X("None"),
    D("D-rated (Pony express)"),
    C("C-rated (Pony express)"),
    B("B-rated"),
    A("A-rated");

    private static final MMLogger logger = MMLogger.create(HPGRating.class);

    //region Variable Declarations
    private final String name;

    //region Constructors
    HPGRating(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static HPGRating parseHPGRating(String val) {
        try {
            return HPGRating.valueOf(val.toUpperCase());
        } catch (Exception ex) {
            logger.error(ex, "Couldn't find a HPG rating level matching " + val.toUpperCase());
            return X;
        }
    }
}
