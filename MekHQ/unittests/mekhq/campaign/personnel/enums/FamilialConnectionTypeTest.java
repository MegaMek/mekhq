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

public class FamilialConnectionTypeTest {
    //region Variable Declarations
    private static final FamilialConnectionType[] types = FamilialConnectionType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsMarried() {
        for (final FamilialConnectionType familialConnectionType : types) {
            if (familialConnectionType == FamilialConnectionType.MARRIED) {
                assertTrue(familialConnectionType.isMarried());
            } else {
                assertFalse(familialConnectionType.isMarried());
            }
        }
    }

    @Test
    public void testIsDivorced() {
        for (final FamilialConnectionType familialConnectionType : types) {
            if (familialConnectionType == FamilialConnectionType.DIVORCED) {
                assertTrue(familialConnectionType.isDivorced());
            } else {
                assertFalse(familialConnectionType.isDivorced());
            }
        }
    }

    @Test
    public void testIsWidowed() {
        for (final FamilialConnectionType familialConnectionType : types) {
            if (familialConnectionType == FamilialConnectionType.WIDOWED) {
                assertTrue(familialConnectionType.isWidowed());
            } else {
                assertFalse(familialConnectionType.isWidowed());
            }
        }
    }

    @Test
    public void testIsPartner() {
        for (final FamilialConnectionType familialConnectionType : types) {
            if (familialConnectionType == FamilialConnectionType.PARTNER) {
                assertTrue(familialConnectionType.isPartner());
            } else {
                assertFalse(familialConnectionType.isPartner());
            }
        }
    }

    @Test
    public void testIsSingleParent() {
        for (final FamilialConnectionType familialConnectionType : types) {
            if (familialConnectionType == FamilialConnectionType.SINGLE_PARENT) {
                assertTrue(familialConnectionType.isSingleParent());
            } else {
                assertFalse(familialConnectionType.isSingleParent());
            }
        }
    }

    @Test
    public void testIsAdopted() {
        for (final FamilialConnectionType familialConnectionType : types) {
            if (familialConnectionType == FamilialConnectionType.ADOPTED) {
                assertTrue(familialConnectionType.isAdopted());
            } else {
                assertFalse(familialConnectionType.isAdopted());
            }
        }
    }

    @Test
    public void testIsUndefined() {
        for (final FamilialConnectionType familialConnectionType : types) {
            if (familialConnectionType == FamilialConnectionType.UNDEFINED) {
                assertTrue(familialConnectionType.isUndefined());
            } else {
                assertFalse(familialConnectionType.isUndefined());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("FamilialConnectionType.MARRIED.text"), FamilialConnectionType.MARRIED.toString());
        assertEquals(resources.getString("FamilialConnectionType.ADOPTED.text"), FamilialConnectionType.ADOPTED.toString());
    }
}
