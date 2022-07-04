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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

@Disabled // FIXME : Windchild : All Tests Missing
public class FamilialRelationshipTypeTest {
    //region Variable Declarations
    private static final FamilialRelationshipType[] types = FamilialRelationshipType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
/*
    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }

    @Test
    public void testIs() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.NONE) {
                assertTrue(familialRelationshipType.isNone());
            } else {
                assertFalse(familialRelationshipType.isNone());
            }
        }
    }
*/
    //endregion Boolean Comparison Methods

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetTypename() {

    }
}
