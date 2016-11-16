/*
 * RefitType.java
 * 
 * Copyright (C) 2016 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.work;

import java.util.Arrays;
import java.util.Locale;

public enum RefitType {
    // The order is important, so that sorting and taking the highest works properly
    NO_CHANGE(0, "no change", 0.0, 0),
    TRIVIAL(1, "trivial change", 0.5, 0), // For custom rule sets
    A(2, "Class A Refit (Field)", 1.0, 1),
    B(3, "Class B Refit (Field)", 1.0, 1),
    C(4, "Class C Refit (Maintenance)", 2.0, 2),
    D(5, "Class D Refit (Maintenance)", 3.0, 2),
    E(6, "Class E Refit (Factory)", 4.0, 3),
    F(7, "Class F Refit (Factory)", 5.0, 4),
    X(8, "Class X Experimental Refit", 7.5, 6); // For custom rule sets

    // Initialize by-id array lookup table
    private static RefitType[] idMap;
    static {
        int maxId = 0;
        for(RefitType refitType : values()) {
            maxId = Math.max(maxId, refitType.id);
        }
        idMap = new RefitType[maxId + 1];
        Arrays.fill(idMap, NO_CHANGE);
        for(RefitType refitType : values()) {
            if(refitType.id > 0) {
                idMap[refitType.id] = refitType;
            }
        }
    }

    /** @return the refit type corresponding to the (old) ID */
    public static RefitType of(int id) {
        return ((id > 0) && (id < idMap.length)) ? idMap[id] : NO_CHANGE;
    }
    
    /** @return the refit type corresponding to the given string */
    public static RefitType of(String str) {
        try {
            return of(Integer.valueOf(str));
        } catch(NumberFormatException nfex) {
            // Try something else
        }
        return valueOf(str.toUpperCase(Locale.ROOT));
    }
    
    public final int id;
    // User-displayable. TODO: Localize
    public final String name;
    public final double timeMultiplier;
    public final int mod;

    private RefitType(int id, String name, double timeMultiplier, int mod) {
        this.id = id;
        this.name = name;
        this.timeMultiplier = timeMultiplier;
        this.mod = mod;
    }
}
