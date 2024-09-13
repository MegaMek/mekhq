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

import java.util.ResourceBundle;

import megamek.logging.MMLogger;
import mekhq.MekHQ;

public enum FamilialRelationshipDisplayLevel {
    // region Enum Declarations
    SPOUSE("FamilialRelationshipDisplayLevel.SPOUSE.text"),
    PARENTS_CHILDREN_SIBLINGS("FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS.text"),
    GRANDPARENTS_GRANDCHILDREN("FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN.text"),
    AUNTS_UNCLES_COUSINS("FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.text");
    // endregion Enum Declarations

    // region Variable Declarations
    private final String name;
    // endregion Variable Declarations

    // region Constructors
    FamilialRelationshipDisplayLevel(final String name) {
        final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
                MekHQ.getMHQOptions().getLocale());
        this.name = resources.getString(name);
    }
    // endregion Constructors

    // region Boolean Comparisons
    public boolean isSpouse() {
        return this == SPOUSE;
    }

    public boolean isParentsChildrenSiblings() {
        return this == PARENTS_CHILDREN_SIBLINGS;
    }

    public boolean isGrandparentsGrandchildren() {
        return this == GRANDPARENTS_GRANDCHILDREN;
    }

    public boolean isAuntsUnclesCousins() {
        return this == AUNTS_UNCLES_COUSINS;
    }

    public boolean displayParentsChildrenSiblings() {
        return isParentsChildrenSiblings() || displayGrandparentsGrandchildren();
    }

    public boolean displayGrandparentsGrandchildren() {
        return isGrandparentsGrandchildren() || isAuntsUnclesCousins();
    }
    // endregion Boolean Comparisons

    // region File I/O
    public static FamilialRelationshipDisplayLevel parseFromString(final String text) {
        try {
            return valueOf(text);
        } catch (Exception ignored) {

        }

        try {
            switch (Integer.parseInt(text)) {
                case 0:
                    return PARENTS_CHILDREN_SIBLINGS;
                case 1:
                    return GRANDPARENTS_GRANDCHILDREN;
                case 2:
                    return AUNTS_UNCLES_COUSINS;
                default:
                    break;
            }
        } catch (Exception ignored) {

        }

        MMLogger.create(FamilialRelationshipDisplayLevel.class).error("Unable to parse " + text
                + " into a FamilialRelationshipDisplayLevel. Returning PARENTS_CHILDREN_SIBLINGS.");
        return PARENTS_CHILDREN_SIBLINGS;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
