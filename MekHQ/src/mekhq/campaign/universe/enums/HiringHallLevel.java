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
package mekhq.campaign.universe.enums;

import io.sentry.Sentry;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Atmosphere;

/**
 * The level of a Hiring Hall as defined in CamOps (4th printing). Used to determine various modifiers
 * related to contract generation.
 */
public enum HiringHallLevel {
    NONE,
    QUESTIONABLE,
    MINOR,
    STANDARD,
    GREAT;

    private static final MMLogger logger = MMLogger.create(HiringHallLevel.class);

    public boolean isNone() {
        return this == NONE;
    }
}


