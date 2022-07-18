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
package mekhq.campaign.personnel.enums;

import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

/**
 * This is used to determine the relationship type between related personnel
 */
public enum FamilialRelationshipType {
    //region Enum Declarations
    // Direct Line
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

    // Relatives
    GRANDPIBLING("FamilialRelationshipType.GRANDPIBLING.MALE.text",
            "FamilialRelationshipType.GRANDPIBLING.FEMALE.text",
            "FamilialRelationshipType.GRANDPIBLING.OTHER.text"),
    PIBLING("FamilialRelationshipType.PIBLING.MALE.text",
            "FamilialRelationshipType.PIBLING.FEMALE.text",
            "FamilialRelationshipType.PIBLING.OTHER.text"),
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

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FamilialRelationshipType(final String neutral) {
        this(neutral, neutral, neutral);
    }

    /**
     * @param masculine the masculine form of the relationship type
     * @param feminine the feminine form of the relationship type
     * @param other the non-gendered form of the relationship type
     */
    FamilialRelationshipType(final String masculine, final String feminine, final String other) {
        this.masculine = resources.getString(masculine);
        this.feminine = resources.getString(feminine);
        this.other = resources.getString(other);
    }
    //endregion Constructors

    //region Boolean Comparison Methods
    // Direct Line
    public boolean isGrandparent() {
        return this == GRANDPARENT;
    }

    public boolean isParent() {
        return this == PARENT;
    }

    public boolean isSibling() {
        return this == SIBLING;
    }

    public boolean isHalfSibling() {
        return this == HALF_SIBLING;
    }

    public boolean isChild() {
        return this == CHILD;
    }

    public boolean isGrandchild() {
        return this == GRANDCHILD;
    }

    // Relatives
    public boolean isGrandpibling() {
        return this == GRANDPIBLING;
    }

    public boolean isPibling() {
        return this == PIBLING;
    }

    public boolean isCousin() {
        return this == COUSIN;
    }

    public boolean isNibling() {
        return this == NIBLING;
    }

    // Family-in-law Relationships
    public boolean isSpouse() {
        return this == SPOUSE;
    }

    public boolean isDivorce() {
        return this == DIVORCE;
    }

    public boolean isWidow() {
        return this == WIDOW;
    }

    public boolean isPartner() {
        return this == PARTNER;
    }

    public boolean isParentInLaw() {
        return this == PARENT_IN_LAW;
    }

    public boolean isSiblingInLaw() {
        return this == SIBLING_IN_LAW;
    }

    public boolean isChildInLaw() {
        return this == CHILD_IN_LAW;
    }

    // Stepfamily Relationships
    public boolean isStepparent() {
        return this == STEPPARENT;
    }

    public boolean isStepsibling() {
        return this == STEPSIBLING;
    }

    public boolean isStepchild() {
        return this == STEPCHILD;
    }
    //endregion Boolean Comparison Methods

    public String getTypeName(final Gender gender) {
        return getTypeName(gender, 0, false);
    }

    /**
     * This is used to get the specific type name for a relationship between two people, based on the
     * gender of the relative
     * @param gender the relative's gender
     * @param numGreats how many greats to add to the front of the relationship type
     * @param adopted whether the relative was adopted
     * @return the FamilialRelationshipType name
     */
    public String getTypeName(final Gender gender, final int numGreats, final boolean adopted) {
        final StringBuilder name = new StringBuilder(adopted
                ? resources.getString("FamilialRelationshipType.adopted") + ' ' : "");

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
