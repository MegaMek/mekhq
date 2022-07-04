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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FamilialRelationshipTypeTest {
    //region Variable Declarations
    private static final FamilialRelationshipType[] types = FamilialRelationshipType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
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
    public void testIsChild() {
        for (final FamilialRelationshipType familialRelationshipType : types) {
            if (familialRelationshipType == FamilialRelationshipType.CHILD) {
                assertTrue(familialRelationshipType.isChild());
            } else {
                assertFalse(familialRelationshipType.isChild());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGetTypename() {

    }
}
