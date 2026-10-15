/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit;

import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;

/**
 * How a unit's quirks change the monthly cost of its spare parts (Campaign Operations p.24). Each quirk multiplies the
 * cost in turn, so a unit that is Easy to Maintain (x0.8) and uses Non-Standard Parts (x2.0) pays 1.6 times the base
 * cost.
 */
public final class SparePartsQuirkMultiplier {
    private static final double EASY_TO_MAINTAIN_OR_RUGGED = 0.8;
    private static final double DIFFICULT_TO_MAINTAIN = 1.25;
    private static final double NON_STANDARD_PARTS = 2.0;
    private static final double UBIQUITOUS = 0.75;
    private static final double OBSOLETE = 1.1;
    private static final double OBSOLETE_PER_TWENTY_YEARS = 0.1;
    private static final int YEARS_PER_OBSOLETE_STEP = 20;

    private SparePartsQuirkMultiplier() {}

    /**
     * @param entity   the unit
     * @param gameYear the current year, which decides how long an obsolete unit has been out of production
     *
     * @return the factor to multiply the unit's spare parts cost by; {@code 1.0} for a unit with none of these quirks
     */
    public static double find(Entity entity, int gameYear) {
        double multiplier = 1.0;
        boolean isEasyToMaintain = entity.hasQuirk(OptionsConstants.QUIRK_POS_EASY_MAINTAIN);
        boolean isRugged = entity.hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_1)
                                 || entity.hasQuirk(OptionsConstants.QUIRK_POS_RUGGED_2);
        // The rule groups the two: either one gives x0.8, and both together still give x0.8
        if (isEasyToMaintain || isRugged) {
            multiplier *= EASY_TO_MAINTAIN_OR_RUGGED;
        }
        if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_DIFFICULT_MAINTAIN)) {
            multiplier *= DIFFICULT_TO_MAINTAIN;
        }
        if (entity.hasQuirk(OptionsConstants.QUIRK_NEG_NON_STANDARD)) {
            multiplier *= NON_STANDARD_PARTS;
        }
        multiplier *= findObsoleteMultiplier(entity, gameYear);
        boolean isUbiquitous = entity.hasQuirk(OptionsConstants.QUIRK_POS_UBIQUITOUS_IS)
                                     || entity.hasQuirk(OptionsConstants.QUIRK_POS_UBIQUITOUS_CLAN);
        if (isUbiquitous) {
            multiplier *= UBIQUITOUS;
        }
        return multiplier;
    }

    /**
     * x1.1 once a unit is obsolete, plus 0.1 for every full 20 years since; the obsolete year comes from MegaMek, which
     * also handles designs that went back into production.
     */
    private static double findObsoleteMultiplier(Entity entity, int gameYear) {
        int obsoleteYear = entity.getObsoleteYearForModifiers(gameYear);
        boolean isObsolete = (obsoleteYear > 0) && (gameYear >= obsoleteYear);
        if (!isObsolete) {
            return 1.0;
        }
        int twentyYearSteps = (gameYear - obsoleteYear) / YEARS_PER_OBSOLETE_STEP;
        return OBSOLETE + (OBSOLETE_PER_TWENTY_YEARS * twentyYearSteps);
    }
}
