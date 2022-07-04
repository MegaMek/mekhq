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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenealogyRelationshipTrackingTypeTest {
    //region Variable Declarations
    private static final GenealogyRelationshipTrackingType[] types = GenealogyRelationshipTrackingType.values();
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsParent() {
        for (final GenealogyRelationshipTrackingType genealogyRelationshipTrackingType : types) {
            if (genealogyRelationshipTrackingType == GenealogyRelationshipTrackingType.PARENT) {
                assertTrue(genealogyRelationshipTrackingType.isParent());
            } else {
                assertFalse(genealogyRelationshipTrackingType.isParent());
            }
        }
    }

    @Test
    public void testIsChild() {
        for (final GenealogyRelationshipTrackingType genealogyRelationshipTrackingType : types) {
            if (genealogyRelationshipTrackingType == GenealogyRelationshipTrackingType.CHILD) {
                assertTrue(genealogyRelationshipTrackingType.isChild());
            } else {
                assertFalse(genealogyRelationshipTrackingType.isChild());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetFamilialRelationshipType() {
        assertTrue(GenealogyRelationshipTrackingType.CHILD.getFamilialRelationshipType().isChild());
        assertFalse(GenealogyRelationshipTrackingType.CHILD.getFamilialRelationshipType().isParent());
        assertFalse(GenealogyRelationshipTrackingType.PARENT.getFamilialRelationshipType().isChild());
        assertTrue(GenealogyRelationshipTrackingType.PARENT.getFamilialRelationshipType().isParent());
    }
}
