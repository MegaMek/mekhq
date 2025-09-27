/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
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
package mekhq.campaign.work;

import java.util.Arrays;
import java.util.Locale;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

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
    RUSH_15(-1, "Rush Job (1/15)", 4, true, 4, 1.0 / 15.0),
    RUSH_30(-1, "Rush Job (1/30)", 5, true, 5, 1.0 / 30.0);

    // Initialize by-id array lookup table
    private static final WorkTime[] idMap;

    static {
        int maxId = 0;
        for (WorkTime workTime : values()) {
            maxId = Math.max(maxId, workTime.id);
        }
        idMap = new WorkTime[maxId + 1];
        Arrays.fill(idMap, NORMAL);
        for (WorkTime workTime : values()) {
            if (workTime.id > 0) {
                idMap[workTime.id] = workTime;
            }
        }
    }

    /** Default (Strategic Operations) work time modifiers */
    public static final WorkTime[] DEFAULT_TIMES = {
          NORMAL, EXTRA_2, EXTRA_3, EXTRA_4, RUSH_2, RUSH_4, RUSH_8
    };

    /** StratOps times in increasing order **/
    public static final WorkTime[] STRAT_OPTS_INCREASING_TIMES = {
          RUSH_8, RUSH_4, RUSH_2, NORMAL, EXTRA_2, EXTRA_3, EXTRA_4
    };

    /**
     * @return the work time order corresponding to the (old) ID
     */
    public static WorkTime of(int id) {
        return ((id > 0) && (id < idMap.length)) ? idMap[id] : NORMAL;
    }

    /**
     * @return the work time order corresponding to the given string
     */
    public static WorkTime of(String str) {
        try {
            return of(Integer.parseInt(str));
        } catch (NumberFormatException ignored) {
            // Try something else
        }
        return valueOf(str.toUpperCase(Locale.ROOT));
    }

    public final int id;
    // User-displayable. TODO: Localize
    public final String name;
    // Base modificator to target number. Positive = more difficult. Use
    // getMod(true) to get the value
    private final int mod;
    // Does this count as a rushed job?
    public final boolean isRushed;
    // Experience reduction for quick jobs
    public final int expReduction;
    public final double timeMultiplier;

    WorkTime(int id, String name, int mod, boolean isRushed, int expReduction, double timeMultiplier) {
        this.id = id;
        this.name = name;
        this.mod = mod;
        this.isRushed = isRushed;
        this.expReduction = expReduction;
        this.timeMultiplier = timeMultiplier;
    }

    /**
     * @return the target number modificator
     */
    public int getMod(boolean includeRush) {
        return (!isRushed || includeRush) ? mod : 0;
    }

    public @Nullable WorkTime moveTimeToNextLevel(boolean increase) {
        int currentIdx = -1;

        for (int i = 0; i < STRAT_OPTS_INCREASING_TIMES.length; i++) {
            if (id == STRAT_OPTS_INCREASING_TIMES[i].id) {
                currentIdx = i;
                break;
            }
        }

        if (currentIdx == -1) {
            return null;
        }

        if (increase) {
            if (currentIdx == STRAT_OPTS_INCREASING_TIMES.length - 1) {
                return null;
            }

            return WorkTime.of(STRAT_OPTS_INCREASING_TIMES[currentIdx + 1].id);
        } else {
            if (currentIdx == 0) {
                return null;
            }

            return WorkTime.of(STRAT_OPTS_INCREASING_TIMES[currentIdx - 1].id);
        }
    }

    // region File I/O
    public static WorkTime parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        final WorkTime workTime = of(text);

        if (workTime != null) {
            return workTime;
        }

        MMLogger.create(WorkTime.class).error("Unable to parse {} into a WorkTime. Returning NORMAL.", text);
        return NORMAL;
    }
    // endregion File I/O
}
