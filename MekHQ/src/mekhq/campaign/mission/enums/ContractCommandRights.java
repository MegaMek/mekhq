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
package mekhq.campaign.mission.enums;

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum ContractCommandRights {
    //region Enum Declarations
    INTEGRATED("ContractCommandRights.INTEGRATED.text", "ContractCommandRights.INTEGRATED.toolTipText", "ContractCommandRights.INTEGRATED.stratConText"),
    HOUSE("ContractCommandRights.HOUSE.text", "ContractCommandRights.HOUSE.toolTipText", "ContractCommandRights.HOUSE.stratConText"),
    LIAISON("ContractCommandRights.LIAISON.text", "ContractCommandRights.LIAISON.toolTipText", "ContractCommandRights.LIAISON.stratConText"),
    INDEPENDENT("ContractCommandRights.INDEPENDENT.text", "ContractCommandRights.INDEPENDENT.toolTipText", "ContractCommandRights.INDEPENDENT.stratConText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    private final String stratConText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Mission", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    ContractCommandRights(final String name, final String toolTipText, final String stratConText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
        this.stratConText = resources.getString(stratConText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }

    public String getStratConText() {
        return stratConText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
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
    //endregion Boolean Comparison Methods

    //region File I/O
    /**
     * @param text containing the ContractCommandRights
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

        MekHQ.getLogger().error("Failed to parse text " + text + " into a ContractCommandRights, returning HOUSE.");

        return HOUSE;
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
