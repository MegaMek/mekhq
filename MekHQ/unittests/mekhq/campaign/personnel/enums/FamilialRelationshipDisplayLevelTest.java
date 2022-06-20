/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FamilialRelationshipDisplayLevelTest {
    //region Variable Declarations
    private static final FamilialRelationshipDisplayLevel[] levels = FamilialRelationshipDisplayLevel.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsSpouse() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.SPOUSE) {
                assertTrue(familialRelationshipDisplayLevel.isSpouse());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isSpouse());
            }
        }
    }

    @Test
    public void testIsParentsChildrenSiblings() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS) {
                assertTrue(familialRelationshipDisplayLevel.isParentsChildrenSiblings());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isParentsChildrenSiblings());
            }
        }
    }

    @Test
    public void testIsGrandparentsGrandchildren() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN) {
                assertTrue(familialRelationshipDisplayLevel.isGrandparentsGrandchildren());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isGrandparentsGrandchildren());
            }
        }
    }

    @Test
    public void testIsAuntsUnclesCousins() {
        for (final FamilialRelationshipDisplayLevel familialRelationshipDisplayLevel : levels) {
            if (familialRelationshipDisplayLevel == FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS) {
                assertTrue(familialRelationshipDisplayLevel.isAuntsUnclesCousins());
            } else {
                assertFalse(familialRelationshipDisplayLevel.isAuntsUnclesCousins());
            }
        }
    }

    @Test
    public void testDisplayParentsChildrenSiblings() {
        assertFalse(FamilialRelationshipDisplayLevel.SPOUSE.displayParentsChildrenSiblings());
        assertTrue(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS.displayParentsChildrenSiblings());
        assertTrue(FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN.displayParentsChildrenSiblings());
        assertTrue(FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.displayParentsChildrenSiblings());
    }

    @Test
    public void testDisplayGrandparentsGrandchildren() {
        assertFalse(FamilialRelationshipDisplayLevel.SPOUSE.displayGrandparentsGrandchildren());
        assertFalse(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS.displayGrandparentsGrandchildren());
        assertTrue(FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN.displayGrandparentsGrandchildren());
        assertTrue(FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.displayGrandparentsGrandchildren());
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(FamilialRelationshipDisplayLevel.SPOUSE,
                FamilialRelationshipDisplayLevel.parseFromString("SPOUSE"));
        assertEquals(FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS,
                FamilialRelationshipDisplayLevel.parseFromString("AUNTS_UNCLES_COUSINS"));

        // Legacy Parsing
        assertEquals(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS,
                FamilialRelationshipDisplayLevel.parseFromString("0"));
        assertEquals(FamilialRelationshipDisplayLevel.GRANDPARENTS_GRANDCHILDREN,
                FamilialRelationshipDisplayLevel.parseFromString("1"));
        assertEquals(FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS,
                FamilialRelationshipDisplayLevel.parseFromString("2"));

        // Error Case
        assertEquals(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS,
                FamilialRelationshipDisplayLevel.parseFromString("3"));
        assertEquals(FamilialRelationshipDisplayLevel.PARENTS_CHILDREN_SIBLINGS,
                FamilialRelationshipDisplayLevel.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("FamilialRelationshipDisplayLevel.SPOUSE.text"),
                FamilialRelationshipDisplayLevel.SPOUSE.toString());
        assertEquals(resources.getString("FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.text"),
                FamilialRelationshipDisplayLevel.AUNTS_UNCLES_COUSINS.toString());
    }
}
