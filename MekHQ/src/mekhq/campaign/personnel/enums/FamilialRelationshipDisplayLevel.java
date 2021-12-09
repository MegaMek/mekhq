/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;

import java.util.ResourceBundle;

public enum FamilialRelationshipDisplayLevel {
    //region Enum Declarations
    SPOUSE("FamilialRelationshipDisplayLevel.SPOUSE.text"),
    PARENTS_CHILDREN_SIBLINGS("FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS.text"),
    GRANDPARENTS_GRANDCHILDREN("FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN.text"),
    AUNTS_UNCLES_COUSINS("FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.text");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    FamilialRelationshipDisplayLevel(String name) {
        this.name = resources.getString(name);
    }
    //endregion Constructors

    //region Boolean Comparisons
    public boolean displayParentsChildrenSiblings() {
        return (this == PARENTS_CHILDREN_SIBLINGS) || displayGrandparentsGrandchildren();
    }

    public boolean displayGrandparentsGrandchildren() {
        return (this == GRANDPARENTS_GRANDCHILDREN) || displayAuntsUnclesCousins();
    }

    public boolean displayAuntsUnclesCousins() {
        return this == AUNTS_UNCLES_COUSINS;
    }

    public boolean displayExtendedFamily() {
        return displayParentsChildrenSiblings();
    }
    //endregion Boolean Comparisons

    //region File IO
    public static FamilialRelationshipDisplayLevel parseFromString(String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 1:
                    return GRANDPARENTS_GRANDCHILDREN;
                case 2:
                    return AUNTS_UNCLES_COUSINS;
                case 0:
                default:
                    return PARENTS_CHILDREN_SIBLINGS;
            }
        } catch (Exception ignored) {

        }

        MekHQ.getLogger().error("Failed to parse " + text + " into a FamilialRelationshipDisplayLevel");

        return PARENTS_CHILDREN_SIBLINGS;
    }
    //endregion File IO

    @Override
    public String toString() {
        return name;
    }
}
