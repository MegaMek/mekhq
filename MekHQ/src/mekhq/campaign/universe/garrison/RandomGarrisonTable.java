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

import megamek.common.compute.Compute;

/**
 * The Random Garrisons Table: a 2D6 lookup that sizes a planetary garrison as a number of infantry regiments, armor
 * battalions, and BattleMek battalions. The roll is modified by world modifiers (capital status, industrialization,
 * ownership) and an era modifier before the row is looked up; those world modifiers are resolved elsewhere and passed
 * in here as a combined total. This class is the pure table and roll, with no dependency on campaign or planet data.
 *
 * <p>The table caps at "2 or less" and "10 or more", so a modified roll outside that range is clamped to the nearest
 * row.</p>
 */
public final class RandomGarrisonTable {

    /** Lowest and highest rows on the table; a modified roll is clamped to this range before lookup. */
    private static final int MIN_MODIFIED_ROLL = 2;
    private static final int MAX_MODIFIED_ROLL = 10;

    /** Garrison composition per modified roll, indexed by {@code modifiedRoll - MIN_MODIFIED_ROLL} (rolls 2..10). */
    private static final GarrisonComposition[] ROWS = {
          new GarrisonComposition(2, 1, 0), // 2 or less
          new GarrisonComposition(2, 2, 0), // 3
          new GarrisonComposition(3, 2, 0), // 4
          new GarrisonComposition(3, 3, 0), // 5
          new GarrisonComposition(4, 3, 1), // 6
          new GarrisonComposition(4, 3, 1), // 7
          new GarrisonComposition(5, 4, 2), // 8
          new GarrisonComposition(6, 5, 2), // 9
          new GarrisonComposition(7, 6, 3), // 10 or more
    };

    // Era boundaries for the era modifier. The table uses the classic five-era division, which predates the
    // fine-grained era list in data/universe/eras.xml, so the boundaries are stated explicitly here rather than
    // derived from that list.
    private static final int STAR_LEAGUE_START_YEAR = 2571;                // Age of War ends 2570
    private static final int FIRST_SUCCESSION_WAR_START_YEAR = 2781;       // Star League ends 2780
    private static final int THIRD_SUCCESSION_WAR_START_YEAR = 2865;       // Second Succession War ends 2864
    private static final int POST_FOURTH_SUCCESSION_WAR_START_YEAR = 3031; // Fourth Succession War ends 3030

    private RandomGarrisonTable() {}

    /**
     * Returns the era modifier applied to the garrison roll for the given year, per the table's Era Modifiers.
     *
     * @param year the in-universe year
     *
     * @return {@code +2} in the Age of War, {@code -2} during the Star League, {@code +2} in the First or Second
     *       Succession Wars, {@code 0} in the Third or Fourth Succession Wars, and {@code +1} afterwards
     */
    public static int eraModifier(int year) {
        if (year < STAR_LEAGUE_START_YEAR) {
            return 2;
        }
        if (year < FIRST_SUCCESSION_WAR_START_YEAR) {
            return -2;
        }
        if (year < THIRD_SUCCESSION_WAR_START_YEAR) {
            return 2;
        }
        if (year < POST_FOURTH_SUCCESSION_WAR_START_YEAR) {
            return 0;
        }
        return 1;
    }

    /**
     * Looks up the garrison composition for an already-modified roll, clamping to the "2 or less" / "10 or more" rows.
     *
     * @param modifiedRoll the 2D6 roll plus all modifiers
     *
     * @return the garrison composition for that row (never {@code null})
     */
    public static GarrisonComposition forModifiedRoll(int modifiedRoll) {
        int clampedRoll = Math.clamp(modifiedRoll, MIN_MODIFIED_ROLL, MAX_MODIFIED_ROLL);
        return ROWS[clampedRoll - MIN_MODIFIED_ROLL];
    }

    /**
     * Rolls 2D6, applies the given total modifier (era plus world modifiers, summed by the caller), and returns the
     * resulting garrison composition.
     *
     * @param totalModifier the combined modifier to add to the 2D6 roll
     *
     * @return the rolled garrison composition (never {@code null})
     */
    public static GarrisonComposition roll(int totalModifier) {
        return forModifiedRoll(Compute.d6(2) + totalModifier);
    }
}
