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

public class FamilialConnectionTypeTest {
    //region Variable Declarations
    private static final FamilialConnectionType[] types = FamilialConnectionType.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
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
        assertEquals(resources.getString("FamilialConnectionType.MARRIED.text"),
              FamilialConnectionType.MARRIED.toString());
        assertEquals(resources.getString("FamilialConnectionType.ADOPTED.text"),
              FamilialConnectionType.ADOPTED.toString());
    }
}
