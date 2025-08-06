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
package mekhq.campaign.mission.enums;

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum ContractCommandRights {
    // region Enum Declarations
    INTEGRATED("ContractCommandRights.INTEGRATED.text", "ContractCommandRights.INTEGRATED.toolTipText",
          "ContractCommandRights.INTEGRATED.stratConText"),
    HOUSE("ContractCommandRights.HOUSE.text", "ContractCommandRights.HOUSE.toolTipText",
          "ContractCommandRights.HOUSE.stratConText"),
    LIAISON("ContractCommandRights.LIAISON.text", "ContractCommandRights.LIAISON.toolTipText",
          "ContractCommandRights.LIAISON.stratConText"),
    INDEPENDENT("ContractCommandRights.INDEPENDENT.text", "ContractCommandRights.INDEPENDENT.toolTipText",
          "ContractCommandRights.INDEPENDENT.stratConText");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String stratConText;
    // endregion Variable Declarations

    // region Constructors
    ContractCommandRights(final String name, final String toolTipText, final String stratConText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission",
              MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.stratConText = resources.getString(stratConText);
    }
    // endregion Constructors

    // region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public String getStratConText() {
        return stratConText;
    }
    // endregion Getters

    // region Boolean Comparison Methods
    public boolean isIntegrated() {
        return this == INTEGRATED;
    }

    public boolean isHouse() {
        return this == HOUSE;
    }

    public boolean isLiaison() {
        return this == LIAISON;
    }

    public boolean isIndependent() {
        return this == INDEPENDENT;
    }
    // endregion Boolean Comparison Methods

    // region File I/O

    /**
     * @param text containing the ContractCommandRights
     *
     * @return the saved ContractCommandRights
     */
    public static ContractCommandRights parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return INTEGRATED;
                case 1:
                    return HOUSE;
                case 2:
                    return LIAISON;
                case 3:
                    return INDEPENDENT;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(ContractCommandRights.class)
              .error("Unable to parse " + text + " into a ContractCommandRights. Returning HOUSE.");
        return HOUSE;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
