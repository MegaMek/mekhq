/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.medical.advancedMedical;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.personnel.BodyLocation;

/**
 * Home to static methods returning a random hit location given a random integer value generator and a function to check
 * if a given {@link BodyLocation} is valid.
 */
public class HitLocationGen {
    // Roll tables
    private static NavigableMap<Integer, BodyLocation> GENERIC_RANDOM_HIT_TABLE = new TreeMap<>();

    static {
        GENERIC_RANDOM_HIT_TABLE.put(10, BodyLocation.HEAD);
        GENERIC_RANDOM_HIT_TABLE.put(30, BodyLocation.CHEST);
        GENERIC_RANDOM_HIT_TABLE.put(40, BodyLocation.ABDOMEN);
        GENERIC_RANDOM_HIT_TABLE.put(43, BodyLocation.LEFT_HAND);
        GENERIC_RANDOM_HIT_TABLE.put(55, BodyLocation.LEFT_ARM);
        GENERIC_RANDOM_HIT_TABLE.put(58, BodyLocation.RIGHT_HAND);
        GENERIC_RANDOM_HIT_TABLE.put(70, BodyLocation.RIGHT_ARM);
        GENERIC_RANDOM_HIT_TABLE.put(76, BodyLocation.LEFT_FOOT);
        GENERIC_RANDOM_HIT_TABLE.put(100, BodyLocation.LEFT_LEG);
        GENERIC_RANDOM_HIT_TABLE.put(106, BodyLocation.RIGHT_FOOT);
        GENERIC_RANDOM_HIT_TABLE.put(130, BodyLocation.RIGHT_LEG);
        GENERIC_RANDOM_HIT_TABLE.put(133, BodyLocation.RIGHT_HAND);
        GENERIC_RANDOM_HIT_TABLE.put(145, BodyLocation.RIGHT_ARM);
        GENERIC_RANDOM_HIT_TABLE.put(148, BodyLocation.LEFT_HAND);
        GENERIC_RANDOM_HIT_TABLE.put(160, BodyLocation.LEFT_ARM);
        GENERIC_RANDOM_HIT_TABLE.put(170, BodyLocation.ABDOMEN);
        GENERIC_RANDOM_HIT_TABLE.put(190, BodyLocation.CHEST);
        GENERIC_RANDOM_HIT_TABLE.put(200, BodyLocation.HEAD);
    }

    private static NavigableMap<Integer, BodyLocation> MEK_RANDOM_HIT_TABLE = new TreeMap<>();

    static {
        MEK_RANDOM_HIT_TABLE.put(25, BodyLocation.HEAD);
        MEK_RANDOM_HIT_TABLE.put(41, BodyLocation.CHEST);
        MEK_RANDOM_HIT_TABLE.put(48, BodyLocation.ABDOMEN);
        MEK_RANDOM_HIT_TABLE.put(61, BodyLocation.LEFT_ARM);
        MEK_RANDOM_HIT_TABLE.put(74, BodyLocation.RIGHT_ARM);
        MEK_RANDOM_HIT_TABLE.put(79, BodyLocation.LEFT_FOOT);
        MEK_RANDOM_HIT_TABLE.put(100, BodyLocation.LEFT_LEG);
        MEK_RANDOM_HIT_TABLE.put(105, BodyLocation.RIGHT_FOOT);
        MEK_RANDOM_HIT_TABLE.put(126, BodyLocation.RIGHT_LEG);
        MEK_RANDOM_HIT_TABLE.put(131, BodyLocation.RIGHT_HAND);
        MEK_RANDOM_HIT_TABLE.put(139, BodyLocation.RIGHT_ARM);
        MEK_RANDOM_HIT_TABLE.put(144, BodyLocation.LEFT_HAND);
        MEK_RANDOM_HIT_TABLE.put(152, BodyLocation.LEFT_ARM);
        MEK_RANDOM_HIT_TABLE.put(159, BodyLocation.ABDOMEN);
        MEK_RANDOM_HIT_TABLE.put(176, BodyLocation.CHEST);
        MEK_RANDOM_HIT_TABLE.put(200, BodyLocation.HEAD);
    }

    private static BodyLocation queryRandomTable(NavigableMap<Integer, BodyLocation> table,
          IntUnaryOperator rnd, Function<BodyLocation, Boolean> validCheck) {
        validCheck = ObjectUtility.nonNull(validCheck, (loc) -> true);
        Entry<Integer, BodyLocation> entry = null;
        do {
            entry = table.ceilingEntry(rnd.applyAsInt(table.lastKey()) + 1);
        } while ((null == entry) || !validCheck.apply(entry.getValue()));
        return entry.getValue();
    }

    public static BodyLocation generic(IntUnaryOperator rnd, Function<BodyLocation, Boolean> validCheck) {
        return queryRandomTable(GENERIC_RANDOM_HIT_TABLE, rnd, validCheck);
    }

    public static BodyLocation mekAndAsf(IntUnaryOperator rnd, Function<BodyLocation, Boolean> validCheck) {
        return queryRandomTable(MEK_RANDOM_HIT_TABLE, rnd, validCheck);
    }
}
