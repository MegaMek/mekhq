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
package mekhq.campaign.icons.enums;

import mekhq.MekHqConstants;

public enum LayeredForceIconOperationalStatus {
    //region Enum Declarations
    FULLY_OPERATIONAL(MekHqConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_FULLY_OPERATIONAL_FILE_PATH),
    SUBSTANTIALLY_OPERATIONAL(MekHqConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_SUBSTANTIALLY_OPERATIONAL_FILE_PATH),
    MARGINALLY_OPERATIONAL(MekHqConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_MARGINALLY_OPERATIONAL_FILE_PATH),
    NOT_OPERATIONAL(MekHqConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_NOT_OPERATIONAL_FILE_PATH),
    FACTORY_FRESH(MekHqConstants.LAYERED_FORCE_ICON_OPERATIONAL_STATUS_FACTORY_FRESH_FILE_PATH);
    //endregion Enum Declarations

    //region Variable Declarations
    private final String filename;
    //endregion Variable Declarations

    //region Constructors
    LayeredForceIconOperationalStatus(final String filename) {
        this.filename = filename;
    }
    //endregion Constructors

    //region Getters
    public String getFilename() {
        return filename;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isFullyOperational() {
        return this == FULLY_OPERATIONAL;
    }

    public boolean isSubstantiallyOperational() {
        return this == SUBSTANTIALLY_OPERATIONAL;
    }

    public boolean isMarginallyOperational() {
        return this == MARGINALLY_OPERATIONAL;
    }

    public boolean isNotOperational() {
        return this == NOT_OPERATIONAL;
    }

    public boolean isFactoryFresh() {
        return this == FACTORY_FRESH;
    }
    //endregion Boolean Comparison Methods
}
