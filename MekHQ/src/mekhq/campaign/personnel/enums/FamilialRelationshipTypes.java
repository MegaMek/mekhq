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

public enum FamilialRelationshipTypes {
    //region Enum Declarations
    // Direct Line
    GREAT_GRANDPARENT("FamilialRelationshipTypes.GRANDPARENT.MALE.text",
            "FamilialRelationshipTypes.GRANDPARENT.FEMALE.text",
            "FamilialRelationshipTypes.GRANDPARENT.OTHER.text"),
    GRANDPARENT("FamilialRelationshipTypes.GRANDPARENT.MALE.text",
            "FamilialRelationshipTypes.GRANDPARENT.FEMALE.text",
            "FamilialRelationshipTypes.GRANDPARENT.OTHER.text"),
    PARENT("FamilialRelationshipTypes.PARENT.MALE.text",
            "FamilialRelationshipTypes.PARENT.FEMALE.text",
            "FamilialRelationshipTypes.PARENT.OTHER.text"),
    SIBLING("FamilialRelationshipTypes.SIBLING.MALE.text",
            "FamilialRelationshipTypes.SIBLING.FEMALE.text",
            "FamilialRelationshipTypes.SIBLING.OTHER.text"),
    CHILD("FamilialRelationshipTypes.CHILD.MALE.text",
            "FamilialRelationshipTypes.CHILD.FEMALE.text",
            "FamilialRelationshipTypes.CHILD.OTHER.text"),
    GRANDCHILD("FamilialRelationshipTypes.GRANDCHILD.MALE.text",
            "FamilialRelationshipTypes.GRANDCHILD.FEMALE.text",
            "FamilialRelationshipTypes.GRANDCHILD.OTHER.text"),
    GREAT_GRANDCHILD("FamilialRelationshipTypes.GRANDCHILD.MALE.text",
            "FamilialRelationshipTypes.GRANDCHILD.FEMALE.text",
            "FamilialRelationshipTypes.GRANDCHILD.OTHER.text"),

    // Relatives
    GREAT_AUNT_GREAT_UNCLE("FamilialRelationshipTypes.AUNT_UNCLE.MALE.text",
            "FamilialRelationshipTypes.AUNT_UNCLE.FEMALE.text"),
    AUNT_UNCLE("FamilialRelationshipTypes.AUNT_UNCLE.MALE.text",
            "FamilialRelationshipTypes.AUNT_UNCLE.FEMALE.text"),
    COUSIN("FamilialRelationshipTypes.COUSIN.text",
            "FamilialRelationshipTypes.COUSIN.text",
            "FamilialRelationshipTypes.COUSIN.text"),
    NIBLING("FamilialRelationshipTypes.NIBLING.MALE.text",
            "FamilialRelationshipTypes.NIBLING.FEMALE.text",
            "FamilialRelationshipTypes.NIBLING.OTHER.text"),

    // Family-in-law Relationships
    SPOUSE("FamilialRelationshipTypes.SPOUSE.MALE.text",
            "FamilialRelationshipTypes.SPOUSE.FEMALE.text",
            "FamilialRelationshipTypes.SPOUSE.OTHER.text"),
    PARENT_IN_LAW("FamilialRelationshipTypes.PARENT_IN_LAW.MALE.text",
            "FamilialRelationshipTypes.PARENT_IN_LAW.FEMALE.text",
            "FamilialRelationshipTypes.PARENT_IN_LAW.OTHER.text"),
    SIBLING_IN_LAW("FamilialRelationshipTypes.SIBLING_IN_LAW.MALE.text",
            "FamilialRelationshipTypes.SIBLING_IN_LAW.FEMALE.text",
            "FamilialRelationshipTypes.SIBLING_IN_LAW.OTHER.text"),
    CHILD_IN_LAW("FamilialRelationshipTypes.CHILD_IN_LAW.MALE.text",
            "FamilialRelationshipTypes.CHILD_IN_LAW.FEMALE.text",
            "FamilialRelationshipTypes.CHILD_IN_LAW.OTHER.text"),

    // Stepfamily Relationships
    STEPPARENT("FamilialRelationshipTypes.STEPPARENT.MALE.text",
            "FamilialRelationshipTypes.STEPPARENT.FEMALE.text",
            "FamilialRelationshipTypes.STEPPARENT.OTHER.text"),
    STEPSIBLING("FamilialRelationshipTypes.STEPSIBLING.MALE.text",
            "FamilialRelationshipTypes.STEPSIBLING.FEMALE.text",
            "FamilialRelationshipTypes.STEPSIBLING.OTHER.text"),
    STEPCHILD("FamilialRelationshipTypes.STEPCHILD.MALE.text",
            "FamilialRelationshipTypes.STEPCHILD.FEMALE.text",
            "FamilialRelationshipTypes.STEPCHILD.OTHER.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String masculine;
    private final String feminine;
    private final String other;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FamilialRelationshipTypes(String masculine, String feminine) {
        this(masculine, feminine, null);
    }

    FamilialRelationshipTypes(String masculine, String feminine, String other) {
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
                ? resources.getString("FamilialRelationshipTypes.adopted") + " " : "");

        for (int i = 0; i < numGreats; i++) {
            name.append(resources.getString("FamilialRelationshipTypes.great"));
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
