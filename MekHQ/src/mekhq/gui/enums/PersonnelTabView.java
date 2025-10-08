/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.enums;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum PersonnelTabView {
    //region Enum Declarations
    GRAPHIC("PersonnelTabView.GRAPHIC.text", "PersonnelTabView.GRAPHIC.toolTipText"),
    GENERAL("PersonnelTabView.GENERAL.text", "PersonnelTabView.GENERAL.toolTipText"),
    PILOT_GUNNERY_SKILLS("PersonnelTabView.PILOT_GUNNERY_SKILLS.text",
          "PersonnelTabView.PILOT_GUNNERY_SKILLS.toolTipText"),
    PILOT_GUNNERY_SKILLS_II("PersonnelTabView.PILOT_GUNNERY_SKILLS_II.text",
          "PersonnelTabView.PILOT_GUNNERY_SKILLS_II.toolTipText"),
    INFANTRY_SKILLS("PersonnelTabView.INFANTRY_SKILLS.text", "PersonnelTabView.INFANTRY_SKILLS.toolTipText"),
    TACTICAL_SKILLS("PersonnelTabView.TACTICAL_SKILLS.text", "PersonnelTabView.TACTICAL_SKILLS.toolTipText"),
    TECHNICAL_SKILLS("PersonnelTabView.TECHNICAL_SKILLS.text", "PersonnelTabView.TECHNICAL_SKILLS.toolTipText"),
    MEDICAL_SKILLS("PersonnelTabView.MEDICAL_SKILLS.text", "PersonnelTabView.MEDICAL_SKILLS.toolTipText"),
    ADMINISTRATIVE_SKILLS("PersonnelTabView.ADMINISTRATIVE_SKILLS.text",
          "PersonnelTabView.ADMINISTRATIVE_SKILLS.toolTipText"),
    TRAITS("PersonnelTabView.TRAITS.text", "PersonnelTabView.TRAITS.toolTipText"),
    ATTRIBUTES("PersonnelTabView.ATTRIBUTES.text", "PersonnelTabView.ATTRIBUTES.toolTipText"),
    PERSONALITY("PersonnelTabView.PERSONALITY.text", "PersonnelTabView.PERSONALITY.toolTipText"),
    BIOGRAPHICAL("PersonnelTabView.BIOGRAPHICAL.text", "PersonnelTabView.BIOGRAPHICAL.toolTipText"),
    FLUFF("PersonnelTabView.FLUFF.text", "PersonnelTabView.FLUFF.toolTipText"),
    DATES("PersonnelTabView.DATES.text", "PersonnelTabView.DATES.toolTipText"),
    FLAGS("PersonnelTabView.FLAGS.text", "PersonnelTabView.FLAGS.toolTipText"),
    TRANSPORT("PersonnelTabView.TRANSPORT.text", "PersonnelTabView.TRANSPORT.toolTipText"),
    OTHER("PersonnelTabView.OTHER.text", "PersonnelTabView.OTHER.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    PersonnelTabView(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI",
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
    public boolean isGraphic() {
        return this == GRAPHIC;
    }

    public boolean isGeneral() {
        return this == GENERAL;
    }

    public boolean isPilotGunnerySkillsII() {
        return this == PILOT_GUNNERY_SKILLS_II;
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

    public boolean isMedicalSkills() {
        return this == MEDICAL_SKILLS;
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

    public boolean isDates() {
        return this == DATES;
    }

    public boolean isFlags() {
        return this == FLAGS;
    }

    public boolean isTransport() {
        return this == TRANSPORT;
    }

    public boolean isPersonality() {
        return this == PERSONALITY;
    }

    public boolean isTraits() {
        return this == TRAITS;
    }

    public boolean isOther() {
        return this == OTHER;
    }
    //endregion Boolean Comparison Methods

    @Override
    public String toString() {
        return name;
    }
}
