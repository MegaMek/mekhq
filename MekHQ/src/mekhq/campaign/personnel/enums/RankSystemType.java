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
package mekhq.campaign.personnel.enums;

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;

public enum RankSystemType {
    // region Enum Declarations
    DEFAULT("RankSystemType.DEFAULT.text", "RankSystemType.DEFAULT.toolTipText"),
    USER_DATA("RankSystemType.USER_DATA.text", "RankSystemType.USER_DATA.toolTipText"),
    CAMPAIGN("RankSystemType.CAMPAIGN.text", "RankSystemType.CAMPAIGN.toolTipText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    // endregion Variable Declarations

    // region Constructors
    RankSystemType(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isDefault() {
        return this == DEFAULT;
    }

    public boolean isUserData() {
        return this == USER_DATA;
    }

    public boolean isCampaign() {
        return this == CAMPAIGN;
    }
    // endregion Boolean Comparison Methods

    public String getFilePath() {
        switch (this) {
            case DEFAULT:
                return MHQConstants.RANKS_FILE_PATH;
            case USER_DATA:
                return MHQConstants.USER_RANKS_FILE_PATH;
            case CAMPAIGN:
            default:
                MMLogger.create(RankSystemType.class).error(
                      "Attempted to load an illegal file path. Returning a blank String, which will cause the load to fail.");
                return "";
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
