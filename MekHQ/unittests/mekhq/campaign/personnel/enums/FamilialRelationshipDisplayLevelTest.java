/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

class FamilialRelationshipDisplayLevelTest {
    // region Variable Declarations
    private static final FamilialRelationshipDisplayLevel[] levels = FamilialRelationshipDisplayLevel.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Boolean Comparison Methods
    @Test
    void testIsSpouse() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.SPOUSE) {
                assertTrue(familialRelationshipDisplayLevel.isSpouse());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isSpouse());
            }
        }
    }

    @Test
    void testIsParentsChildrenSiblings() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS) {
                assertTrue(familialRelationshipDisplayLevel.isParentsChildrenSiblings());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isParentsChildrenSiblings());
            }
        }
    }

    @Test
    void testIsGrandparentsGrandchildren() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN) {
                assertTrue(familialRelationshipDisplayLevel.isGrandparentsGrandchildren());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isGrandparentsGrandchildren());
            }
        }
    }

    @Test
    void testIsAuntsUnclesCousins() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS) {
                assertTrue(familialRelationshipDisplayLevel.isAuntsUnclesCousins());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isAuntsUnclesCousins());
            }
        }
    }

    @Test
    void testDisplayParentsChildrenSiblings() {
        assertFalse(FamilialRelationshipDisplayLevel.SPOUSE.displayParentsChildrenSiblings());
        assertTrue(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS.displayParentsChildrenSiblings());
        assertTrue(FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN.displayParentsChildrenSiblings());
        assertTrue(FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.displayParentsChildrenSiblings());
    }

    @Test
    void testDisplayGrandparentsGrandchildren() {
        assertFalse(FamilialRelationshipDisplayLevel.SPOUSE.displayGrandparentsGrandchildren());
        assertFalse(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS.displayGrandparentsGrandchildren());
        assertTrue(FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN.displayGrandparentsGrandchildren());
        assertTrue(FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.displayGrandparentsGrandchildren());
    }
    // endregion Boolean Comparison Methods

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(FamilialRelationshipDisplayLevel.SPOUSE,
              FamilialRelationshipDisplayLevel.parseFromString("SPOUSE"));
        assertEquals(FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS,
              FamilialRelationshipDisplayLevel.parseFromString("AUNTS_UNCLES_COUSINS"));

        // Error Case
        assertEquals(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS,
              FamilialRelationshipDisplayLevel.parseFromString("3"));
        assertEquals(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS,
              FamilialRelationshipDisplayLevel.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("FamilialRelationshipDisplayLevel.SPOUSE.text"),
              FamilialRelationshipDisplayLevel.SPOUSE.toString());
        assertEquals(resources.getString("FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.text"),
              FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.toString());
    }
}
