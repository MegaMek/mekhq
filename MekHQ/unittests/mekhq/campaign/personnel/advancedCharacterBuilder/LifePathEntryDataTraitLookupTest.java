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
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LifePathEntryDataTraitLookupTest {
    @ParameterizedTest
    @EnumSource(LifePathEntryDataTraitLookup.class)
    public void testFromLookupName_ValidName(LifePathEntryDataTraitLookup lifePathEntryDataTraitLookup) {
        LifePathEntryDataTraitLookup result = LifePathEntryDataTraitLookup.fromLookupName(lifePathEntryDataTraitLookup.getLookupName());
        assertEquals(result,
              lifePathEntryDataTraitLookup,
              "Failed to retrieve " + lifePathEntryDataTraitLookup.getLookupName() + " from lookup name.");
    }

    @Test
    public void testFromLookupName_InvalidLookupName() {
        LifePathEntryDataTraitLookup result = LifePathEntryDataTraitLookup.fromLookupName("SOME_NONSENSE");
        assertNull(result, "Invalid lookup should return null.");
    }

    @Test
    public void testFromLookupName_NullLookupName() {
        LifePathEntryDataTraitLookup result = LifePathEntryDataTraitLookup.fromLookupName(null);
        assertNull(result, "Null lookup should return null.");
    }

    @Test
    public void testFromLookupName_EmptyLookupName() {
        LifePathEntryDataTraitLookup result = LifePathEntryDataTraitLookup.fromLookupName("");
        assertNull(result, "Empty lookup should return null.");
    }
}
