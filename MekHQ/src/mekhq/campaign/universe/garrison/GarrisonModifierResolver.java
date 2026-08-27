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
package mekhq.campaign.universe.garrison;

import java.time.LocalDate;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.SocioIndustrialData;

/**
 * Resolves the total modifier applied to a {@link RandomGarrisonTable} roll for a specific planetary system and date,
 * combining the world modifiers (capital status, industrialization, Clan control) with the era modifier.
 *
 * <p>The "within one interstellar hex of a pre-war border" world modifier from the source table is intentionally not
 * implemented.</p>
 */
public final class GarrisonModifierResolver {

    private GarrisonModifierResolver() {}

    /**
     * @param system the planetary system the garrison defends
     * @param when   the date to evaluate
     *
     * @return the total modifier to apply to the garrison roll (world modifiers plus the era modifier)
     */
    public static int resolveModifier(PlanetarySystem system, LocalDate when) {
        return capitalModifier(system, when)
                     + industrialModifier(system, when)
                     + clanControlModifier(system, when)
                     + RandomGarrisonTable.eraModifier(when.getYear());
    }

    /**
     * @return {@code +4} for a national capital, {@code +2} for a regional or district capital, otherwise {@code 0}
     */
    static int capitalModifier(PlanetarySystem system, LocalDate when) {
        return switch (system.getCapitalType(when)) {
            case NATIONAL -> 4;
            case REGION, DISTRICT -> 2;
            case NONE -> 0;
        };
    }

    /**
     * @return {@code +4} for a hyper-industrial world (industry A), {@code +2} for major industrial (B), {@code +1} for
     *       minor industrial (C), otherwise {@code 0}
     */
    static int industrialModifier(PlanetarySystem system, LocalDate when) {
        SocioIndustrialData socioIndustrial = system.getSocioIndustrial(when);
        if ((socioIndustrial == null) || (socioIndustrial.industry == null)) {
            return 0;
        }
        return switch (socioIndustrial.industry) {
            case A -> 4;
            case B -> 2;
            case C -> 1;
            case D, F -> 0;
        };
    }

    /**
     * @return {@code -1} if any faction controlling the system on the given date is a Clan, otherwise {@code 0}
     */
    static int clanControlModifier(PlanetarySystem system, LocalDate when) {
        for (Faction faction : system.getFactionSet(when)) {
            if ((faction != null) && faction.isClan()) {
                return -1;
            }
        }
        return 0;
    }
}
