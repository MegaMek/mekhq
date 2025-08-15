/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums.education;

import java.util.ResourceBundle;

import mekhq.MekHQ;

public enum EducationStage {
    //region Enum Declarations
    NONE("EducationStage.NONE.text", "EducationStage.NONE.toolTipText"),
    JOURNEY_TO_CAMPUS("EducationStage.JOURNEY_TO_CAMPUS.text", "EducationStage.JOURNEY_TO_CAMPUS.toolTipText"),
    EDUCATION("EducationStage.EDUCATION.text", "EducationStage.EDUCATION.toolTipText"),
    GRADUATING("EducationStage.GRADUATING.text", "EducationStage.GRADUATING.toolTipText"),
    DROPPING_OUT("EducationStage.DROPPING_OUT.text", "EducationStage.DROPPING_OUT.toolTipText"),
    JOURNEY_FROM_CAMPUS("EducationStage.JOURNEY_FROM_CAMPUS.text", "EducationStage.JOURNEY_FROM_CAMPUS.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    EducationStage(final String name, final String toolTipText) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
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
    public boolean isNone() {
        return this == NONE;
    }

    public boolean isJourneyToCampus() {
        return this == JOURNEY_TO_CAMPUS;
    }

    public boolean isEducation() {
        return this == EDUCATION;
    }

    public boolean isGraduating() {
        return this == GRADUATING;
    }

    public boolean isDroppingOut() {
        return this == DROPPING_OUT;
    }

    public boolean isJourneyFromCampus() {
        return this == JOURNEY_FROM_CAMPUS;
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    public static EducationStage parseFromString(final String educationLevel) {
        switch (educationLevel) {
            case "None":
                return NONE;
            case "Journeying to Campus":
                return JOURNEY_TO_CAMPUS;
            case "Undergoing Education":
                return EDUCATION;
            case "Graduating":
                return GRADUATING;
            case "Dropping Out":
                return DROPPING_OUT;
            case "Journeying from Campus":
                return JOURNEY_FROM_CAMPUS;
            default:
                throw new IllegalStateException(
                      "Unexpected value in mekhq/campaign/personnel/enums/education/EducationStage.java/parseFromString: "
                            + educationLevel);
        }
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
