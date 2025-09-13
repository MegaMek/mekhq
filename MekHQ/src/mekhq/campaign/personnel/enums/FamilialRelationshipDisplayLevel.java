/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

        MMLogger.create(FamilialRelationshipDisplayLevel.class)
              .error("Unable to parse {} into a FamilialRelationshipDisplayLevel. Returning PARENTS_CHILDREN_SIBLINGS.",
                    text);
        return PARENTS_CHILDREN_SIBLINGS;
    }
    // endregion File I/O

    @Override
    public String toString() {
        return name;
    }
}
