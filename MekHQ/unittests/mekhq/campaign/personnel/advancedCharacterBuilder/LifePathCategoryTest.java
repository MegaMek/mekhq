/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.advancedCharacterBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.utilities.MHQInternationalization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class LifePathCategoryTest {
    @ParameterizedTest
    @EnumSource(LifePathCategory.class)
    public void testForUniqueLookupName(LifePathCategory lifePathCategory) {
        for (LifePathCategory lookup : LifePathCategory.values()) {
            if (lookup == lifePathCategory) {
                continue;
            }

            assertNotEquals(lifePathCategory.getLookupName(), lookup.getLookupName(), "Lookup names should be unique");
        }
    }


    @ParameterizedTest
    @EnumSource(LifePathCategory.class)
    public void testFromLookupName_ValidName(LifePathCategory lifePathCategory) {
        LifePathCategory result = LifePathCategory.fromLookupName(lifePathCategory.getLookupName());
        assertNotNull(result);
        assertEquals(lifePathCategory,
              result,
              "Failed to retrieve " + lifePathCategory.getLookupName() + " from lookup name.");
    }

    @ParameterizedTest
    @EnumSource(LifePathCategory.class)
    public void testFromLookupName_ValidName_CaseInsensitive(LifePathCategory lifePathCategory) {
        LifePathCategory result = LifePathCategory.fromLookupName(lifePathCategory.getLookupName().toLowerCase());
        assertNotNull(result);
        assertEquals(lifePathCategory,
              result,
              "Failed to retrieve " + lifePathCategory.getLookupName() + " from lookup name.");
    }

    @Test
    public void testFromLookupName_UnknownName() {
        LifePathCategory result = LifePathCategory.fromLookupName("UNKNOWN");
        assertNull(result, "Unknown lookup should return null.");
    }

    @Test
    public void testFromLookupName_NullLookup() {
        LifePathCategory result = LifePathCategory.fromLookupName(null);
        assertNull(result, "Null lookup should return null.");
    }

    @ParameterizedTest
    @EnumSource(LifePathCategory.class)
    void testGetDisplayName_isValidKey(LifePathCategory category) {
        assertTrue(MHQInternationalization.isResourceKeyValid(category.getDisplayName()),
              "Invalid key for " + category.name());
    }

    @ParameterizedTest
    @EnumSource(LifePathCategory.class)
    void testGetDescription_isValidKey(LifePathCategory category) {
        assertTrue(MHQInternationalization.isResourceKeyValid(category.getDescription()),
              "Invalid key for " + category.name());
    }
}
