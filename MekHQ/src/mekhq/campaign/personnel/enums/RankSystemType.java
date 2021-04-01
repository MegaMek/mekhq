/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import mekhq.MekHQ;
import mekhq.MekHqConstants;

public enum RankSystemType {
    //region Enum Declarations
    DEFAULT, // Default MekHQ rank system
    USER_DATA, // User Data user custom rank system
    CAMPAIGN; // Campaign custom rank system
    //endregion Enum Declarations

    //region Boolean Comparison Methods
    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isUserData() {
        return this == USER_DATA;
    }

    public boolean isCampaign() {
        return this == CAMPAIGN;
    }
    //endregion Boolean Comparison Methods

    public String getFilePath() {
        switch (this) {
            case DEFAULT:
                return MekHqConstants.RANKS_FILE_PATH;
            case USER_DATA:
                return MekHqConstants.USER_RANKS_FILE_PATH;
            case CAMPAIGN:
            default:
                MekHQ.getLogger().error("Attempted to load an illegal file path. Returning a blank String, which will cause the load to fail.");
                return "";
        }
    }
}
