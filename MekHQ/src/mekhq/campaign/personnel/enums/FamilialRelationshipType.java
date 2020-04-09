/*
 * Copyright (c) 2020 - The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.Crew;
import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Person;

import java.util.ResourceBundle;

public enum FamilialRelationshipType {
    //region Enum Declarations
    // Direct Line
    GREAT_GRANDPARENT("FamilialRelationshipType.GRANDPARENT.MALE.text",
            "FamilialRelationshipType.GRANDPARENT.FEMALE.text",
            "FamilialRelationshipType.GRANDPARENT.OTHER.text"),
    GRANDPARENT("FamilialRelationshipType.GRANDPARENT.MALE.text",
            "FamilialRelationshipType.GRANDPARENT.FEMALE.text",
            "FamilialRelationshipType.GRANDPARENT.OTHER.text"),
    PARENT("FamilialRelationshipType.PARENT.MALE.text",
            "FamilialRelationshipType.PARENT.FEMALE.text",
            "FamilialRelationshipType.PARENT.OTHER.text"),
    SIBLING("FamilialRelationshipType.SIBLING.MALE.text",
            "FamilialRelationshipType.SIBLING.FEMALE.text",
            "FamilialRelationshipType.SIBLING.OTHER.text"),
    CHILD("FamilialRelationshipType.CHILD.MALE.text",
            "FamilialRelationshipType.CHILD.FEMALE.text",
            "FamilialRelationshipType.CHILD.OTHER.text"),
    GRANDCHILD("FamilialRelationshipType.GRANDCHILD.MALE.text",
            "FamilialRelationshipType.GRANDCHILD.FEMALE.text",
            "FamilialRelationshipType.GRANDCHILD.OTHER.text"),
    GREAT_GRANDCHILD("FamilialRelationshipType.GRANDCHILD.MALE.text",
            "FamilialRelationshipType.GRANDCHILD.FEMALE.text",
            "FamilialRelationshipType.GRANDCHILD.OTHER.text"),

    // Relatives
    GREAT_AUNT_GREAT_UNCLE("FamilialRelationshipType.AUNT_UNCLE.MALE.text",
            "FamilialRelationshipType.AUNT_UNCLE.FEMALE.text"),
    AUNT_UNCLE("FamilialRelationshipType.AUNT_UNCLE.MALE.text",
            "FamilialRelationshipType.AUNT_UNCLE.FEMALE.text"),
    COUSIN("FamilialRelationshipType.COUSIN.text"),
    NIBLING("FamilialRelationshipType.NIBLING.MALE.text",
            "FamilialRelationshipType.NIBLING.FEMALE.text",
            "FamilialRelationshipType.NIBLING.OTHER.text"),

    // Family-in-law Relationships
    SPOUSE("FamilialRelationshipType.SPOUSE.MALE.text",
            "FamilialRelationshipType.SPOUSE.FEMALE.text",
            "FamilialRelationshipType.SPOUSE.OTHER.text"),
    PARTNER("FamilialRelationshipType.PARTNER.text"),
    PARENT_IN_LAW("FamilialRelationshipType.PARENT_IN_LAW.MALE.text",
            "FamilialRelationshipType.PARENT_IN_LAW.FEMALE.text",
            "FamilialRelationshipType.PARENT_IN_LAW.OTHER.text"),
    SIBLING_IN_LAW("FamilialRelationshipType.SIBLING_IN_LAW.MALE.text",
            "FamilialRelationshipType.SIBLING_IN_LAW.FEMALE.text",
            "FamilialRelationshipType.SIBLING_IN_LAW.OTHER.text"),
    CHILD_IN_LAW("FamilialRelationshipType.CHILD_IN_LAW.MALE.text",
            "FamilialRelationshipType.CHILD_IN_LAW.FEMALE.text",
            "FamilialRelationshipType.CHILD_IN_LAW.OTHER.text"),

    // Stepfamily Relationships
    STEPPARENT("FamilialRelationshipType.STEPPARENT.MALE.text",
            "FamilialRelationshipType.STEPPARENT.FEMALE.text",
            "FamilialRelationshipType.STEPPARENT.OTHER.text"),
    STEPSIBLING("FamilialRelationshipType.STEPSIBLING.MALE.text",
            "FamilialRelationshipType.STEPSIBLING.FEMALE.text",
            "FamilialRelationshipType.STEPSIBLING.OTHER.text"),
    STEPCHILD("FamilialRelationshipType.STEPCHILD.MALE.text",
            "FamilialRelationshipType.STEPCHILD.FEMALE.text",
            "FamilialRelationshipType.STEPCHILD.OTHER.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String masculine;
    private final String feminine;
    private final String other;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FamilialRelationshipType(String neutral) {
        this(neutral, neutral, neutral);
    }

    FamilialRelationshipType(String masculine, String feminine) {
        this(masculine, feminine, null);
    }

    FamilialRelationshipType(String masculine, String feminine, String other) {
        this.masculine = resources.getString(masculine);
        this.feminine = resources.getString(feminine);
        this.other = (other != null) ? resources.getString(other): "";
    }
    //endregion Constructors

    public String getTypeName(int gender) {
        return getTypeName(gender, 0, false);
    }
    public String getTypeName(int gender, int numGreats) {
        return getTypeName(gender, numGreats, false);
    }
    public String getTypeName(int gender, boolean adopted) {
        return getTypeName(gender, 0, adopted);
    }
    public String getTypeName(int gender, int numGreats, boolean adopted) {
        StringBuilder name = new StringBuilder(adopted
                ? resources.getString("FamilialRelationshipType.adopted") + " " : "");

        for (int i = 0; i < numGreats; i++) {
            name.append(resources.getString("FamilialRelationshipType.great"));
        }

        switch (gender) {
            case Crew.G_MALE:
                name.append(masculine);
                break;
            case Crew.G_FEMALE:
                name.append(feminine);
                break;
            default:
                name.append(other);
                break;
        }

        return name.toString();
    }
}
