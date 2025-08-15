/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts;

import megamek.common.ITechnology.AvailabilityValue;
import megamek.common.ITechnology.TechRating;
import megamek.logging.MMLogger;

/**
 * Helper functions for determining part availability and tech base and the associated modifiers. A lot of this code is
 * borrowed from the deprecated SSWLibHelper.java
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Availability {
    private static final MMLogger logger = MMLogger.create(Availability.class);

    public static int getAvailabilityModifier(AvailabilityValue availability) {
        if (availability == null) {
            // We don't know why we got a null availability, but it shouldn't raise.
            return 999;
        }

        return switch (availability) {
            case A -> -4;
            case B -> -3;
            case C -> -2;
            case D -> -1;
            case E -> 0;
            case F -> 2;
            case X ->
                // FIXME : Per IO, any IS equipment with a base SW availability of E-F that goes
                // FIXME : extinct during the SW has it increased by 1 with F+1 meaning that
                // there
                // FIXME : is a 50% chance of being unobtainable. This doesn't work so well with
                // the
                // FIXME : rules in StratOps, so for now I'm considering it equivalent to X,
                // which
                // FIXME : gives a +5.
                  5;
            default -> {
                logger.error("Attempting to get availability modifier for unknown rating of " + availability);
                yield 999;
            }
        };
    }

    public static int getTechModifier(TechRating tech) {
        switch (tech) {
            case A:
                return -4;
            case B:
                return -2;
            case C:
                return 0;
            case D:
                return 1;
            case E:
                return 2;
            case F:
                return 3;
            default:
                logger.error("Attempting to get tech modifier for unknown rating of " + tech);
                return 999;
        }
    }
}
