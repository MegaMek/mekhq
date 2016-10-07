/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mod.am;

import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

import mekhq.Utilities;
import mekhq.campaign.personnel.BodyLocation;

/**
 * Home to static methods returning a random hit location given a random integer value generator
 * and a function to check if a given {@link BodyLocation} is valid.
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
    private static NavigableMap<Integer, BodyLocation> MECH_RANDOM_HIT_TABLE = new TreeMap<>();
    static {
        MECH_RANDOM_HIT_TABLE.put(25, BodyLocation.HEAD);
        MECH_RANDOM_HIT_TABLE.put(41, BodyLocation.CHEST);
        MECH_RANDOM_HIT_TABLE.put(48, BodyLocation.ABDOMEN);
        MECH_RANDOM_HIT_TABLE.put(61, BodyLocation.LEFT_ARM);
        MECH_RANDOM_HIT_TABLE.put(74, BodyLocation.RIGHT_ARM);
        MECH_RANDOM_HIT_TABLE.put(79, BodyLocation.LEFT_FOOT);
        MECH_RANDOM_HIT_TABLE.put(100, BodyLocation.LEFT_LEG);
        MECH_RANDOM_HIT_TABLE.put(105, BodyLocation.RIGHT_FOOT);
        MECH_RANDOM_HIT_TABLE.put(126, BodyLocation.RIGHT_LEG);
        MECH_RANDOM_HIT_TABLE.put(131, BodyLocation.RIGHT_HAND);
        MECH_RANDOM_HIT_TABLE.put(139, BodyLocation.RIGHT_ARM);
        MECH_RANDOM_HIT_TABLE.put(144, BodyLocation.LEFT_HAND);
        MECH_RANDOM_HIT_TABLE.put(152, BodyLocation.LEFT_ARM);
        MECH_RANDOM_HIT_TABLE.put(159, BodyLocation.ABDOMEN);
        MECH_RANDOM_HIT_TABLE.put(176, BodyLocation.CHEST);
        MECH_RANDOM_HIT_TABLE.put(200, BodyLocation.HEAD);
    }

    private static BodyLocation queryRandomTable(NavigableMap<Integer, BodyLocation> table,
        IntUnaryOperator rnd, Function<BodyLocation, Boolean> validCheck) {
        validCheck = Utilities.nonNull(validCheck, (loc) -> true);
        Entry<Integer, BodyLocation> entry = null;
        do {
            entry = table.ceilingEntry(rnd.applyAsInt(table.lastKey().intValue()) + 1);
        } while((null == entry) || !validCheck.apply(entry.getValue()));
        return entry.getValue();
    }
    
    public static BodyLocation generic(IntUnaryOperator rnd, Function<BodyLocation, Boolean> validCheck) {
        return queryRandomTable(GENERIC_RANDOM_HIT_TABLE, rnd, validCheck);
    }
    
    public static BodyLocation mechAndAsf(IntUnaryOperator rnd, Function<BodyLocation, Boolean> validCheck) {
        return queryRandomTable(MECH_RANDOM_HIT_TABLE, rnd, validCheck);
    }
}
