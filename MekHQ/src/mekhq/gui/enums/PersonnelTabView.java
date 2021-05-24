/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

public enum PersonnelTabView {
    //region Enum Declarations
    GRAPHIC("PersonnelTabView.GRAPHIC.text", "PersonnelTabView.GRAPHIC.toolTipText"),
    GENERAL("PersonnelTabView.GENERAL.text", "PersonnelTabView.GENERAL.toolTipText"),
    PILOT_GUNNERY_SKILLS("PersonnelTabView.PILOT_GUNNERY_SKILLS.text", "PersonnelTabView.PILOT_GUNNERY_SKILLS.toolTipText"),
    INFANTRY_SKILLS("PersonnelTabView.INFANTRY_SKILLS.text", "PersonnelTabView.INFANTRY_SKILLS.toolTipText"),
    TACTICAL_SKILLS("PersonnelTabView.TACTICAL_SKILLS.text", "PersonnelTabView.TACTICAL_SKILLS.toolTipText"),
    TECHNICAL_SKILLS("PersonnelTabView.TECHNICAL_SKILLS.text", "PersonnelTabView.TECHNICAL_SKILLS.toolTipText"),
    ADMINISTRATIVE_SKILLS("PersonnelTabView.ADMINISTRATIVE_SKILLS.text", "PersonnelTabView.ADMINISTRATIVE_SKILLS.toolTipText"),
    BIOGRAPHICAL("PersonnelTabView.BIOGRAPHICAL.text", "PersonnelTabView.BIOGRAPHICAL.toolTipText"),
    FLUFF("PersonnelTabView.FLUFF.text", "PersonnelTabView.FLUFF.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    PersonnelTabView(final String name, final String toolTipText) {
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
    public boolean isGraphic() {
        return this == GRAPHIC;
    }

    public boolean isGeneral() {
        return this == GENERAL;
    }

    public boolean isPilotGunnerySkills() {
        return this == PILOT_GUNNERY_SKILLS;
    }

    public boolean isInfantrySkills() {
        return this == INFANTRY_SKILLS;
    }

    public boolean isTacticalSkills() {
        return this == TACTICAL_SKILLS;
    }

    public boolean isTechnicalSkills() {
        return this == TECHNICAL_SKILLS;
    }

    public boolean isAdministrativeSkills() {
        return this == ADMINISTRATIVE_SKILLS;
    }

    public boolean isBiographical() {
        return this == BIOGRAPHICAL;
    }

    public boolean isFluff() {
        return this == FLUFF;
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return name;
    }
}
