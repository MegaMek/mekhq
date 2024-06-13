/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

public enum AcademyType {
    //region Enum Declarations
    NONE("AcademyType.NONE.text", "AcademyType.NONE.toolTipText"),
    HIGH_SCHOOL("AcademyType.HIGH_SCHOOL.text", "AcademyType.HIGH_SCHOOL.toolTipText"),
    COLLEGE("AcademyType.COLLEGE.text", "AcademyType.COLLEGE.toolTipText"),
    UNIVERSITY("AcademyType.UNIVERSITY.text", "AcademyType.UNIVERSITY.toolTipText"),
    MILITARY_ACADEMY("AcademyType.MILITARY_ACADEMY.text", "AcademyType.MILITARY_ACADEMY.toolTipText"),
    BASIC_TRAINING("AcademyType.BASIC_TRAINING.text", "AcademyType.BASIC_TRAINING.toolTipText"),
    NCO_ACADEMY("AcademyType.NCO_ACADEMY.text", "AcademyType.NCO_ACADEMY.toolTipText"),
    WARRANT_OFFICER_ACADEMY("AcademyType.WARRANT_OFFICER_ACADEMY.text", "AcademyType.WARRANT_OFFICER_ACADEMY.toolTipText"),
    OFFICER_ACADEMY("AcademyType.OFFICER_ACADEMY.text", "AcademyType.OFFICER_ACADEMY.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;
    //endregion Variable Declarations

    //region Constructors
    AcademyType(final String name, final String toolTipText) {
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
    public boolean IsNone() {
        return this == NONE;
    }

    public boolean isHighSchool() {
        return this == HIGH_SCHOOL;
    }

    public boolean isCollege() {
        return this == COLLEGE;
    }

    public boolean isUniversity() {
        return this == UNIVERSITY;
    }

    public boolean isMilitaryAcademy() {
        return this == MILITARY_ACADEMY;
    }

    public boolean isBasicTraining() {
        return this == BASIC_TRAINING;
    }

    public boolean isNcoAcademy() {
        return this == NCO_ACADEMY;
    }

    public boolean isWarrantOfficerAcademy() {
        return this == WARRANT_OFFICER_ACADEMY;
    }

    public boolean isOfficerAcademy() {
        return this == OFFICER_ACADEMY;
    }
    //endregion Boolean Comparison Methods

/**
 * Parses a given string and returns the corresponding AcademyType.
 * Accepts either the ENUM ordinal value, or its name
 *
 * @param text the string to be parsed
 * @return the AcademyType object that corresponds to the given string
 * @throws IllegalStateException if the given string does not match any valid AcademyType
 */
//region File I/O
    public static AcademyType parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {}

        switch (Integer.parseInt(text)) {
            case 0:
                return NONE;
            case 1:
                return HIGH_SCHOOL;
            case 2:
                return COLLEGE;
            case 3:
                return UNIVERSITY;
            case 4:
                return MILITARY_ACADEMY;
            case 5:
                return BASIC_TRAINING;
            case 6:
                return NCO_ACADEMY;
            case 7:
                return WARRANT_OFFICER_ACADEMY;
            case 8:
                return OFFICER_ACADEMY;
            default:
                throw new IllegalStateException("Unexpected value in mekhq/campaign/personnel/enums/education/AcademyType.java/parseFromString: " + Integer.parseInt(text));
        }
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
