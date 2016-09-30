/*
 * WorkTime.java
 * 
 * Copyright (C) 2016 MegaMek team
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

public enum WorkTime {
    NORMAL(0, "Normal", 0, false, 0, 1.0),
    EXTRA_2(1, "Extra time (x2)", -1, false, 0, 2.0),
    EXTRA_3(2, "Extra time (x3)", -2, false, 0, 3.0),
    EXTRA_4(3, "Extra time (x4)", -3, false, 0, 4.0),
    RUSH_2(4, "Rush Job (1/2)", 1, true, 1, 0.5),
    RUSH_4(5, "Rush Job (1/4)", 2, true, 2, 0.25),
    RUSH_8(6, "Rush Job (1/8)", 3, true, 3, 0.125),
    // Some additional tiers, in case people want to use them in alternate rules
    EXTRA_6(-1, "Extra time (x6)", -4, false, 0, 6.0),
    EXTRA_8(-1, "Extra time (x8)", -5, false, 0, 8.0),
    RUSH_15(-1, "Rush Job (1/15)", 4, true, 4, 1.0/15.0),
    RUSH_30(-1, "Rush Job (1/30)", 5, true, 5, 1.0/30.0);
    
    public final int id;
    // User-displayable. TODO: Localize
    public final String name;
    // Base modificator to target number. Positive = more difficult. Use getMod(true) to get the value
    private final int mod;
    // Does this count as a rushed job?
    public final boolean isRushed;
    // Experience reduction for quick jobs
    public final int expReduction;
    public final double timeMultiplier;
    
    // Initialize by-id array lookup table
    private static WorkTime[] idMap;
    static {
        int maxId = 0;
        for(WorkTime workTime : values()) {
            maxId = Math.max(maxId, workTime.id);
        }
        idMap = new WorkTime[maxId + 1];
        Arrays.fill(idMap, NORMAL);
        for(WorkTime workTime : values()) {
            if(workTime.id > 0) {
                idMap[workTime.id] = workTime;
            }
        }
    }
    
    /** Default (Strategic Operations) work time modifiers */
    public static final WorkTime[] DEFAULT_TIMES = {
        NORMAL, EXTRA_2, EXTRA_3, EXTRA_4, RUSH_2, RUSH_4, RUSH_8
    };
    /** All possible work time modifiers (in a "logical" order, as opposed to <tt>values()</tt>) */
    public static final WorkTime[] ALL_TIMES = {
        NORMAL, EXTRA_2, EXTRA_3, EXTRA_4, EXTRA_6, EXTRA_8, RUSH_2, RUSH_4, RUSH_8, RUSH_15, RUSH_30
    };

    /** @return the work time order corresponding to the (old) ID */
    public static WorkTime of(int id) {
        return ((id > 0) && (id < idMap.length)) ? idMap[id] : NORMAL;
    }
    
    /** @return the work time order corresponding to the given string */
    public static WorkTime of(String str) {
        try {
            return of(Integer.valueOf(str));
        } catch(NumberFormatException nfex) {
            // Try something else
        }
        return valueOf(str.toUpperCase(Locale.ROOT));
    }
    
    private WorkTime(int id, String name, int mod, boolean isRushed, int expReduction, double timeMultiplier) {
        this.id = id;
        this.name = name;
        this.mod = mod;
        this.isRushed = isRushed;
        this.expReduction = expReduction;
        this.timeMultiplier = timeMultiplier;
    }
    
    /** @return the target number modificator */
    public int getMod(boolean includeRush) {
        return (!isRushed || includeRush) ? mod : 0;
    }
}