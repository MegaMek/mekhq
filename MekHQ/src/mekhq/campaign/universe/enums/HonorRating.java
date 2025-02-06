/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe.enums;

/**
 * Represents the honor of a Clan
 */
public enum HonorRating {
    NONE(0.0, Integer.MAX_VALUE),
    LIBERAL(1.25, Integer.MAX_VALUE),
    OPPORTUNISTIC(1.0, 5),
    STRICT(0.75, 0);

    private final double bvMultiplier;
    private final int bondsmanTargetNumber;

    /**
     * Constructor for HonorRating enum to initialize its properties.
     *
     * @param bvMultiplier       Battle Value multiplier associated with the honor level - used by
     *                          Clan Bidding
     * @param bondsmanTargetNumber Target number for determining bondsmen with this style
     */
    HonorRating(double bvMultiplier, int bondsmanTargetNumber) {
        this.bvMultiplier = bvMultiplier;
        this.bondsmanTargetNumber = bondsmanTargetNumber;
    }

    /**
     * Gets the Battle Value multiplier associated with this capture style.
     *
     * @return the bvMultiplier
     */
    public double getBvMultiplier() {
        return bvMultiplier;
    }

    /**
     * Gets the target number for becoming a bondsman.
     *
     * @return the bondsmanTargetNumber
     */
    public int getBondsmanTargetNumber() {
        return bondsmanTargetNumber;
    }
}
