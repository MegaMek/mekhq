/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.enums;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * The level of a Hiring Hall as defined in CamOps (4th printing). Used to determine various modifiers related to
 * contract generation.
 *
 * <p>Each level also carries its Hot Spots: Draconis Reach Hiring Hall Rating (A best, F worst). There are five
 * CamOps levels against six Hot Spots ratings, so rating E is unused.</p>
 */
public enum HiringHallLevel {
    NONE('F', 1.0),
    QUESTIONABLE('D', 1.0),
    MINOR('C', 0.75),
    STANDARD('B', 0.5),
    GREAT('A', 0.5);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Universe";

    private final char hotSpotsRating;
    private final double planetaryCostMultiplier;

    HiringHallLevel(char hotSpotsRating, double planetaryCostMultiplier) {
        this.hotSpotsRating = hotSpotsRating;
        this.planetaryCostMultiplier = planetaryCostMultiplier;
    }

    public boolean isNone() {
        return this == NONE;
    }

    /**
     * @return the Hot Spots: Draconis Reach Hiring Hall Rating, from {@code A} (best) to {@code F} (worst)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public char getHotSpotsRating() {
        return hotSpotsRating;
    }

    /**
     * @return the off-contract planetary repair and refit cost multiplier (Draconis Reach first printing pg 69)
     *
     * @author Illiani
     * @since 0.51.01
     */
    public double getPlanetaryCostMultiplier() {
        return planetaryCostMultiplier;
    }

    /**
     * @return the level's display label, giving both the CamOps name and the Hot Spots rating, e.g. "Standard (B)"
     *
     * @author Illiani
     * @since 0.51.01
     */
    public String getLabel() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "HiringHallLevel.label",
              getTextAt(RESOURCE_BUNDLE, "HiringHallLevel." + name() + ".text"),
              String.valueOf(hotSpotsRating));
    }
}
