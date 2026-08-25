/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.enums;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.logging.MMLogger;

public enum DragoonRating {
    DRAGOON_F(ForceDescriptor.RATING_0, "F"),
    DRAGOON_D(ForceDescriptor.RATING_1, "D"),
    DRAGOON_C(ForceDescriptor.RATING_2, "C"),
    DRAGOON_B(ForceDescriptor.RATING_3, "B"),
    DRAGOON_A(ForceDescriptor.RATING_4, "A"),
    DRAGOON_ASTAR(ForceDescriptor.RATING_5, "A*");

    private static final MMLogger LOGGER = MMLogger.create(DragoonRating.class);

    private final int rating;
    private final String label;

    DragoonRating(int rating, String label) {
        this.rating = rating;
        this.label = label;
    }

    public int getRating() {
        return rating;
    }

    /**
     * @return the single-character rating as players know it: {@code F} through {@code A}, or {@code A*} for the top
     *       rating. Read the scale from here rather than indexing a parallel array - the array that used to back the
     *       contract view was one element short and threw on an A* force.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the DragoonRating that matches the given integer rating.
     *
     * @param rating the integer rating to look up
     *
     * @return the corresponding DragoonRating, or null if none matches
     */
    public static DragoonRating fromRating(int rating) {
        for (DragoonRating dragoonRating : values()) {
            if (dragoonRating.rating == rating) {
                return dragoonRating;
            }
        }

        LOGGER.warn("Invalid dragoon rating: {}. Returning DRAGOON_C", rating);
        return DRAGOON_C;
    }
}
