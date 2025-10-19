/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.rating;

import java.util.ResourceBundle;

import mekhq.MekHQ;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @since 9/24/2013
 */
@Deprecated(since = "0.50.10", forRemoval = true)
public enum UnitRatingMethod {
    //region Enum Declarations
    @Deprecated(since = "0.50.10", forRemoval = true)
    NONE("UnitRatingMethod.NONE.text"),
    CAMPAIGN_OPS("UnitRatingMethod.CAMPAIGN_OPS.text"),
    @Deprecated(since = "0.50.10", forRemoval = true)
    FLD_MAN_MERCS_REV("UnitRatingMethod.FLD_MAN_MERCS_REV.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    //endregion Variable Declarations

    //region Constructors
    UnitRatingMethod(String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Rating",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
    public boolean isEnabled() {
        return this != NONE;
    }

    public boolean isCampaignOperations() {
        return this == CAMPAIGN_OPS;
    }

    public boolean isFMMR() {
        return this == FLD_MAN_MERCS_REV;
    }
    //endregion Boolean Comparison Methods

    //region File IO
    public static UnitRatingMethod parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        return switch (text) {
            case "Campaign Ops", "Taharqa", "Interstellar Ops" -> CAMPAIGN_OPS;
            default -> FLD_MAN_MERCS_REV;
        };
    }
    //endregion File IO

    @Override
    public String toString() {
        return name;
    }
}
