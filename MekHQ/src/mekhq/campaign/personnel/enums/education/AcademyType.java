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

public enum AcademyType {
    //region Enum Declarations
    NONE("AcademyType.NONE.text", "AcademyType.NONE.toolTipText"),
    HIGH_SCHOOL("AcademyType.HIGH_SCHOOL.text", "AcademyType.HIGH_SCHOOL.toolTipText"),
    COLLEGE("AcademyType.COLLEGE.text", "AcademyType.COLLEGE.toolTipText"),
    UNIVERSITY("AcademyType.UNIVERSITY.text", "AcademyType.UNIVERSITY.toolTipText"),
    MILITARY_ACADEMY("AcademyType.MILITARY_ACADEMY.text", "AcademyType.MILITARY_ACADEMY.toolTipText"),
    BASIC_TRAINING("AcademyType.BASIC_TRAINING.text", "AcademyType.BASIC_TRAINING.toolTipText"),
    NCO_ACADEMY("AcademyType.NCO_ACADEMY.text", "AcademyType.NCO_ACADEMY.toolTipText"),
    WARRANT_OFFICER_ACADEMY("AcademyType.WARRANT_OFFICER_ACADEMY.text",
          "AcademyType.WARRANT_OFFICER_ACADEMY.toolTipText"),
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
     * Parses a given string and returns the corresponding AcademyType. Accepts either the ENUM ordinal value, or its
     * name
     *
     * @param academyType the string to be parsed
     *
     * @return the AcademyType object that corresponds to the given string
     *
     * @throws IllegalStateException if the given string does not match any valid AcademyType
     */
    //region File I/O
    public static AcademyType parseFromString(final String academyType) {
        return switch (academyType) {
            case "0", "None" -> NONE;
            case "1", "High School" -> HIGH_SCHOOL;
            case "2", "College" -> COLLEGE;
            case "3", "University" -> UNIVERSITY;
            case "4", "Military Academy" -> MILITARY_ACADEMY;
            case "5", "Basic Training" -> BASIC_TRAINING;
            case "6", "NCO Academy" -> NCO_ACADEMY;
            case "7", "Warrant Officer Academy" -> WARRANT_OFFICER_ACADEMY;
            case "8", "Officer Academy" -> OFFICER_ACADEMY;
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/enums/education/AcademyType.java/parseFromString: "
                        + academyType);
        };
    }
    //endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
