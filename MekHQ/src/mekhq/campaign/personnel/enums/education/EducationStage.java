/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums.education;

import mekhq.MekHQ;

import java.util.ResourceBundle;

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

    public boolean isJournalToCampus() {
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
                throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/education/EducationStage.java/parseFromString: "
                        + educationLevel);
        }
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
