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

import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;

import java.util.ResourceBundle;

/**
 * This is used to track the relationship type between related personnel
 */
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
    HALF_SIBLING("FamilialRelationshipType.HALF_SIBLING.MALE.text",
            "FamilialRelationshipType.HALF_SIBLING.FEMALE.text",
            "FamilialRelationshipType.HALF_SIBLING.OTHER.text"),
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
    GREAT_GRANDAUNT_GREAT_GRANDUNCLE("FamilialRelationshipType.GRANDAUNT_GRANDUNCLE.MALE.text",
            "FamilialRelationshipType.GRANDAUNT_GRANDUNCLE.FEMALE.text"),
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
    DIVORCE("FamilialRelationshipType.DIVORCE.text"),
    WIDOW("FamilialRelationshipType.WIDOW.text"),
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
    private final String masculine; // Masculine form of the relationship type, like Father for Parent
    private final String feminine;  // Feminine form of the relationship type, like Mother for Parent
    private final String other; // Genderless form of the relationship type, like Parent for Parent

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

    /**
     * @param masculine the masculine form of the relationship type
     * @param feminine the feminine form of the relationship type
     * @param other the non-gendered form of the relationship type
     */
    FamilialRelationshipType(String masculine, String feminine, String other) {
        this.masculine = resources.getString(masculine);
        this.feminine = resources.getString(feminine);
        this.other = (other != null) ? resources.getString(other): "";
    }
    //endregion Constructors

    public String getTypeName(Gender gender) {
        return getTypeName(gender, 0, false);
    }
    public String getTypeName(Gender gender, int numGreats) {
        return getTypeName(gender, numGreats, false);
    }
    public String getTypeName(Gender gender, boolean adopted) {
        return getTypeName(gender, 0, adopted);
    }

    /**
     * This is used to get the specific type name for a relationship between two people, based on the
     * gender of the relative
     * @param gender the relative's gender
     * @param numGreats how many greats to add to the front of the relationship type
     * @param adopted whether or not the relative was adopted
     * @return the FamilialRelationshipType name
     */
    public String getTypeName(Gender gender, int numGreats, boolean adopted) {
        StringBuilder name = new StringBuilder(adopted
                ? resources.getString("FamilialRelationshipType.adopted") + " " : "");

        for (int i = 0; i < numGreats; i++) {
            name.append(resources.getString("FamilialRelationshipType.great"));
        }

        switch (gender) {
            case MALE:
                name.append(masculine);
                break;
            case FEMALE:
                name.append(feminine);
                break;
            default:
                name.append(other);
                break;
        }

        return name.toString();
    }
}
