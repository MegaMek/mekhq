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
    X(8, "Class X Experimental Refit", 7.5, 6), // For custom rule sets
    IMPOSSIBLE(9, "impossible", Double.POSITIVE_INFINITY, Integer.MAX_VALUE); // To mark some changes as forbidden

    // Initialize by-id array lookup table
    private static final RefitType[] idMap;

    static {
        int maxId = 0;
        for (RefitType refitType : values()) {
            maxId = Math.max(maxId, refitType.id);
        }
        idMap = new RefitType[maxId + 1];
        Arrays.fill(idMap, NO_CHANGE);
        for (RefitType refitType : values()) {
            if (refitType.id > 0) {
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
            return of(Integer.parseInt(str));
        } catch (NumberFormatException ignored) {
            // Try something else
        }

        return valueOf(str.toUpperCase(Locale.ROOT));
    }

    public final int id;
    // User-displayable. TODO: Localize
    public final String name;
    public final double timeMultiplier;
    public final int mod;

    RefitType(int id, String name, double timeMultiplier, int mod) {
        this.id = id;
        this.name = name;
        this.timeMultiplier = timeMultiplier;
        this.mod = mod;
    }
}
