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
package mekhq.campaign.universe.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum CompanyGenerationPersonType {
    //region Enum Declarations
    COMPANY_COMMANDER("CompanyGenerationPersonType.COMPANY_COMMANDER.text", "CompanyGenerationPersonType.COMPANY_COMMANDER.toolTipText"),
    CAPTAIN("CompanyGenerationPersonType.CAPTAIN.text", "CompanyGenerationPersonType.CAPTAIN.toolTipText"),
    LIEUTENANT("CompanyGenerationPersonType.LIEUTENANT.text", "CompanyGenerationPersonType.LIEUTENANT.toolTipText"),
    MECHWARRIOR("CompanyGenerationPersonType.MECHWARRIOR.text", "CompanyGenerationPersonType.MECHWARRIOR.toolTipText"),
    SUPPORT("CompanyGenerationPersonType.SUPPORT.text", "CompanyGenerationPersonType.SUPPORT.toolTipText"),
    ASSISTANT("CompanyGenerationPersonType.ASSISTANT.text", "CompanyGenerationPersonType.ASSISTANT.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    CompanyGenerationPersonType(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe", new EncodeControl());
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparison Methods
    public boolean isCompanyCommander() {
        return this == COMPANY_COMMANDER;
    }

    public boolean isCaptain() {
        return this == CAPTAIN;
    }

    public boolean isLieutenant() {
        return this == LIEUTENANT;
    }

    public boolean isMechWarrior() {
        return this == MECHWARRIOR;
    }

    public boolean isSupport() {
        return this == SUPPORT;
    }

    public boolean isAssistant() {
        return this == ASSISTANT;
    }

    public boolean isOfficer() {
        return isCaptain() || isLieutenant();
    }

    public boolean isCombat() {
        return isCompanyCommander() || isCaptain() || isLieutenant() || isMechWarrior();
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return name;
    }
}
