/*
 * Copyright (c) 2024-2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.enums;

import io.sentry.Sentry;
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


