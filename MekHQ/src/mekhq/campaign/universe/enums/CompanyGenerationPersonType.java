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
package mekhq.campaign.universe.enums;

import mekhq.MekHQ;

import java.util.ResourceBundle;

/**
 * @author Justin "Windchild" Bowen
 */
public enum CompanyGenerationPersonType {
    //region Enum Declarations
    MEKWARRIOR_COMPANY_COMMANDER("CompanyGenerationPersonType.MEKWARRIOR_COMPANY_COMMANDER.text", "CompanyGenerationPersonType.MEKWARRIOR_COMPANY_COMMANDER.toolTipText"),
    MEKWARRIOR_CAPTAIN("CompanyGenerationPersonType.MEKWARRIOR_CAPTAIN.text", "CompanyGenerationPersonType.MEKWARRIOR_CAPTAIN.toolTipText"),
    MEKWARRIOR_LIEUTENANT("CompanyGenerationPersonType.MEKWARRIOR_LIEUTENANT.text", "CompanyGenerationPersonType.MEKWARRIOR_LIEUTENANT.toolTipText"),
    MEKWARRIOR("CompanyGenerationPersonType.MEKWARRIOR.text", "CompanyGenerationPersonType.MEKWARRIOR.toolTipText"),
    SUPPORT("CompanyGenerationPersonType.SUPPORT.text", "CompanyGenerationPersonType.SUPPORT.toolTipText"),
    ASSISTANT("CompanyGenerationPersonType.ASSISTANT.text", "CompanyGenerationPersonType.ASSISTANT.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    CompanyGenerationPersonType(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe",
                MekHQ.getMHQOptions().getLocale());
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
    public boolean isMekWarriorCompanyCommander() {
        return this == MEKWARRIOR_COMPANY_COMMANDER;
    }

    public boolean isMekWarriorCaptain() {
        return this == MEKWARRIOR_CAPTAIN;
    }

    public boolean isMekWarriorLieutenant() {
        return this == MEKWARRIOR_LIEUTENANT;
    }

    public boolean isMekWarrior() {
        return this == MEKWARRIOR;
    }

    public boolean isSupport() {
        return this == SUPPORT;
    }

    public boolean isAssistant() {
        return this == ASSISTANT;
    }

    public boolean isOfficer() {
        return isMekWarriorCaptain() || isMekWarriorLieutenant();
    }

    public boolean isCombat() {
        return isMekWarriorCompanyCommander() || isMekWarriorCaptain()
                || isMekWarriorLieutenant() || isMekWarrior();
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return name;
    }
}
