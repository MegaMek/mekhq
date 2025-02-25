/*
 * Availability.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
package mekhq.campaign.parts;

import megamek.common.EquipmentType;
import megamek.common.ITechnology;
import megamek.logging.MMLogger;

/**
 * Helper functions for determining part availability and tech base
 * and the associated modifiers. A lot of this code is borrowed from
 * the deprecated SSWLibHelper.java
 * 
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Availability {
    private static final MMLogger logger = MMLogger.create(Availability.class);

    public static int getAvailabilityModifier(int availability) {
        switch (availability) {
            case ITechnology.RATING_A:
                return -4;
            case ITechnology.RATING_B:
                return -3;
            case ITechnology.RATING_C:
                return -2;
            case ITechnology.RATING_D:
                return -1;
            case ITechnology.RATING_E:
                return 0;
            case ITechnology.RATING_F:
                return 2;
            case ITechnology.RATING_FSTAR:
            case ITechnology.RATING_X:
                // FIXME : Per IO, any IS equipment with a base SW availability of E-F that goes
                // FIXME : extinct during the SW has it increased by 1 with F+1 meaning that
                // there
                // FIXME : is a 50% chance of being unobtainable. This doesn't work so well with
                // the
                // FIXME : rules in StratOps, so for now I'm considering it equivalent to X,
                // which
                // FIXME : gives a +5.
                return 5;
            default:
                logger.error("Attempting to get availability modifier for unknown rating of " + availability);
                return 999;
        }
    }

    public static int getTechModifier(int tech) {
        switch (tech) {
            case EquipmentType.RATING_A:
                return -4;
            case EquipmentType.RATING_B:
                return -2;
            case EquipmentType.RATING_C:
                return 0;
            case EquipmentType.RATING_D:
                return 1;
            case EquipmentType.RATING_E:
                return 2;
            case EquipmentType.RATING_F:
                return 3;
            default:
                logger.error("Attempting to get tech modifier for unknown rating of " + tech);
                return 999;
        }
    }
}
