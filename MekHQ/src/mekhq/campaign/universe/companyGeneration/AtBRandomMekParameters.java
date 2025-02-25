/*
 * Copyright (c) 2021-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.companyGeneration;

import megamek.common.EntityWeightClass;

/**
 * This class contains the parameters used to generate a random mek, and allows sorting and
 * swapping the order of rolled parameters while keeping them connected.
 *
 * @author Justin "Windchild" Bowen
 */
public class AtBRandomMekParameters {
    //region Variable Declarations
    private int weight;
    private int quality;
    private boolean starLeague;
    //endregion Variable Declarations

    //region Constructors
    public AtBRandomMekParameters(final int weight, final int quality) {
        setWeight(weight);
        setQuality(quality);
        setStarLeague(weight == EntityWeightClass.WEIGHT_SUPER_HEAVY);
    }
    //endregion Constructors

    //region Getters/Setters
    public int getWeight() {
        return weight;
    }

    public void setWeight(final int weight) {
        this.weight = weight;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(final int quality) {
        this.quality = quality;
    }

    public boolean isStarLeague() {
        return starLeague;
    }

    public void setStarLeague(final boolean starLeague) {
        this.starLeague = starLeague;
    }
    //endregion Getters/Setters
}
