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

/**
 * Personnel Testing Tracker:
 * Missing:
 * 1) FamilialRelationshipType
 * Partial:
 * 1) PersonnelRole
 * 2) PersonnelStatus
 * 3) Profession
 * 4) SplittingSurnameStyle
 * 5) MergingSurnameStyle
 * 6) PersonnelStatus
 */
public class FamilialRelationshipTypeTest {
/*
    //region Variable Declarations
    private static final FamilialRelationshipType[] types = FamilialRelationshipType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods

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
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(FamilialRelationshipType.NONE, FamilialRelationshipType.parseFromString("NONE"));
        assertEquals(FamilialRelationshipType.WIDOWED, FamilialRelationshipType.parseFromString("WIDOWED"));

        // Legacy Parsing
        assertEquals(FamilialRelationshipType.NONE, FamilialRelationshipType.parseFromString("0"));
        assertEquals(FamilialRelationshipType.WIDOWED, FamilialRelationshipType.parseFromString("1"));

        // Error Case
        assertEquals(FamilialRelationshipType.WIDOWED, FamilialRelationshipType.parseFromString("2"));
        assertEquals(FamilialRelationshipType.WIDOWED, FamilialRelationshipType.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("FamilialRelationshipType.NONE.text"), FamilialRelationshipType.NONE.toString());
        assertEquals(resources.getString("FamilialRelationshipType.WIDOWED.text"), FamilialRelationshipType.WIDOWED.toString());
    }
 */
}
