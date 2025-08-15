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
package mekhq.campaign.universe.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

/**
 * @author Justin "Windchild" Bowen
 */
public enum CompanyGenerationPersonType {
    //region Enum Declarations
    MEKWARRIOR_COMPANY_COMMANDER("CompanyGenerationPersonType.MEKWARRIOR_COMPANY_COMMANDER.text",
          "CompanyGenerationPersonType.MEKWARRIOR_COMPANY_COMMANDER.toolTipText"),
    MEKWARRIOR_CAPTAIN("CompanyGenerationPersonType.MEKWARRIOR_CAPTAIN.text",
          "CompanyGenerationPersonType.MEKWARRIOR_CAPTAIN.toolTipText"),
    MEKWARRIOR_LIEUTENANT("CompanyGenerationPersonType.MEKWARRIOR_LIEUTENANT.text",
          "CompanyGenerationPersonType.MEKWARRIOR_LIEUTENANT.toolTipText"),
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
