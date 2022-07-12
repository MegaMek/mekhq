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

import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FamilialRelationshipTypeTest {
    //region Variable Declarations
    private static final FamilialRelationshipType[] types = FamilialRelationshipType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    // Direct Line
    @Test
    public void testIsGrandparent() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.GRANDPARENT) {
                assertTrue(familialRelationshipType.isGrandparent());
            } else {
                assertFalse(familialRelationshipType.isGrandparent());
            }
        }
    }

    @Test
    public void testIsParent() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.PARENT) {
                assertTrue(familialRelationshipType.isParent());
            } else {
                assertFalse(familialRelationshipType.isParent());
            }
        }
    }

    @Test
    public void testIsSibling() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.SIBLING) {
                assertTrue(familialRelationshipType.isSibling());
            } else {
                assertFalse(familialRelationshipType.isSibling());
            }
        }
    }

    @Test
    public void testIsHalfSibling() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.HALF_SIBLING) {
                assertTrue(familialRelationshipType.isHalfSibling());
            } else {
                assertFalse(familialRelationshipType.isHalfSibling());
            }
        }
    }

    @Test
    public void testIsChild() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.CHILD) {
                assertTrue(familialRelationshipType.isChild());
            } else {
                assertFalse(familialRelationshipType.isChild());
            }
        }
    }

    @Test
    public void testIsGrandchild() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.GRANDCHILD) {
                assertTrue(familialRelationshipType.isGrandchild());
            } else {
                assertFalse(familialRelationshipType.isGrandchild());
            }
        }
    }

    // Relatives
    @Test
    public void testIsGrandpibling() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.GRANDPIBLING) {
                assertTrue(familialRelationshipType.isGrandpibling());
            } else {
                assertFalse(familialRelationshipType.isGrandpibling());
            }
        }
    }

    @Test
    public void testIsPibling() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.PIBLING) {
                assertTrue(familialRelationshipType.isPibling());
            } else {
                assertFalse(familialRelationshipType.isPibling());
            }
        }
    }

    @Test
    public void testIsCousin() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.COUSIN) {
                assertTrue(familialRelationshipType.isCousin());
            } else {
                assertFalse(familialRelationshipType.isCousin());
            }
        }
    }

    @Test
    public void testIsNibling() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NIBLING) {
                assertTrue(familialRelationshipType.isNibling());
            } else {
                assertFalse(familialRelationshipType.isNibling());
            }
        }
    }

    @Test
    public void testIsSpouse() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.SPOUSE) {
                assertTrue(familialRelationshipType.isSpouse());
            } else {
                assertFalse(familialRelationshipType.isSpouse());
            }
        }
    }

    @Test
    public void testIsDivorce() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.DIVORCE) {
                assertTrue(familialRelationshipType.isDivorce());
            } else {
                assertFalse(familialRelationshipType.isDivorce());
            }
        }
    }

    @Test
    public void testIsWidow() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.WIDOW) {
                assertTrue(familialRelationshipType.isWidow());
            } else {
                assertFalse(familialRelationshipType.isWidow());
            }
        }
    }

    @Test
    public void testIsPartner() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.PARTNER) {
                assertTrue(familialRelationshipType.isPartner());
            } else {
                assertFalse(familialRelationshipType.isPartner());
            }
        }
    }

    @Test
    public void testIsParentInLaw() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.PARENT_IN_LAW) {
                assertTrue(familialRelationshipType.isParentInLaw());
            } else {
                assertFalse(familialRelationshipType.isParentInLaw());
            }
        }
    }

    @Test
    public void testIsSiblingInLaw() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.SIBLING_IN_LAW) {
                assertTrue(familialRelationshipType.isSiblingInLaw());
            } else {
                assertFalse(familialRelationshipType.isSiblingInLaw());
            }
        }
    }

    @Test
    public void testIsChildInLaw() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.CHILD_IN_LAW) {
                assertTrue(familialRelationshipType.isChildInLaw());
            } else {
                assertFalse(familialRelationshipType.isChildInLaw());
            }
        }
    }

    // Stepfamily
    @Test
    public void testIsStepparent() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.STEPPARENT) {
                assertTrue(familialRelationshipType.isStepparent());
            } else {
                assertFalse(familialRelationshipType.isStepparent());
            }
        }
    }

    @Test
    public void testIsStepsibling() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.STEPSIBLING) {
                assertTrue(familialRelationshipType.isStepsibling());
            } else {
                assertFalse(familialRelationshipType.isStepsibling());
            }
        }
    }

    @Test
    public void testIsStepchild() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.STEPCHILD) {
                assertTrue(familialRelationshipType.isStepchild());
            } else {
                assertFalse(familialRelationshipType.isStepchild());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetTypename() {
        assertEquals(resources.getString("FamilialRelationshipType.PARENT.MALE.text"),
                FamilialRelationshipType.PARENT.getTypeName(Gender.MALE));
        assertEquals(resources.getString("FamilialRelationshipType.PARENT.FEMALE.text"),
                FamilialRelationshipType.PARENT.getTypeName(Gender.FEMALE));
        assertEquals(resources.getString("FamilialRelationshipType.PARENT.OTHER.text"),
                FamilialRelationshipType.PARENT.getTypeName(Gender.RANDOMIZE));

        assertEquals(resources.getString("FamilialRelationshipType.great")
                        + resources.getString("FamilialRelationshipType.GRANDPIBLING.MALE.text"),
                FamilialRelationshipType.GRANDPIBLING.getTypeName(Gender.MALE, 1, false));
        assertEquals(resources.getString("FamilialRelationshipType.great")
                        + resources.getString("FamilialRelationshipType.GRANDPIBLING.FEMALE.text"),
                FamilialRelationshipType.GRANDPIBLING.getTypeName(Gender.FEMALE, 1, false));
        assertEquals(resources.getString("FamilialRelationshipType.great")
                        + resources.getString("FamilialRelationshipType.GRANDPIBLING.OTHER.text"),
                FamilialRelationshipType.GRANDPIBLING.getTypeName(Gender.RANDOMIZE, 1, false));

        assertEquals(resources.getString("FamilialRelationshipType.adopted")
                        + ' ' + resources.getString("FamilialRelationshipType.CHILD.MALE.text"),
                FamilialRelationshipType.CHILD.getTypeName(Gender.MALE, 0, true));
        assertEquals(resources.getString("FamilialRelationshipType.adopted")
                        + ' ' + resources.getString("FamilialRelationshipType.CHILD.FEMALE.text"),
                FamilialRelationshipType.CHILD.getTypeName(Gender.FEMALE, 0, true));
        assertEquals(resources.getString("FamilialRelationshipType.adopted")
                        + ' ' + resources.getString("FamilialRelationshipType.CHILD.OTHER.text"),
                FamilialRelationshipType.CHILD.getTypeName(Gender.RANDOMIZE, 0, true));

        assertEquals(resources.getString("FamilialRelationshipType.adopted")
                        + ' ' + resources.getString("FamilialRelationshipType.great")
                        + resources.getString("FamilialRelationshipType.GRANDCHILD.MALE.text"),
                FamilialRelationshipType.GRANDCHILD.getTypeName(Gender.MALE, 1, true));
        assertEquals(resources.getString("FamilialRelationshipType.adopted")
                        + ' ' + resources.getString("FamilialRelationshipType.great")
                        + resources.getString("FamilialRelationshipType.GRANDCHILD.FEMALE.text"),
                FamilialRelationshipType.GRANDCHILD.getTypeName(Gender.FEMALE, 1, true));
        assertEquals(resources.getString("FamilialRelationshipType.adopted")
                        + ' ' + resources.getString("FamilialRelationshipType.great")
                        + resources.getString("FamilialRelationshipType.GRANDCHILD.OTHER.text"),
                FamilialRelationshipType.GRANDCHILD.getTypeName(Gender.RANDOMIZE, 1, true));
    }
}
