/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.companyGeneration;

import megamek.common.EntityWeightClass;

/**
 * This class contains the parameters used to generate a random mek, and allows sorting and swapping the order of rolled
 * parameters while keeping them connected.
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
