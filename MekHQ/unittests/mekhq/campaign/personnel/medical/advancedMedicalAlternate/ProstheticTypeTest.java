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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.personnel.InjuryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ProstheticTypeTest {
    @Test
    @DisplayName("Each ProstheticType has a unique, non-null InjuryType")
    public void injuryTypeIsUniqueAndNonNull() {
        Set<InjuryType> seen = new HashSet<>();

        for (ProstheticType prostheticType : ProstheticType.values()) {
            InjuryType injuryType = prostheticType.getInjuryType();
            assertNotNull(injuryType, () -> prostheticType.name() + " has null injuryType");
            boolean added = seen.add(injuryType);
            assertTrue(added, () -> "Duplicate injuryType detected for " + prostheticType.name());
        }

        assertEquals(ProstheticType.values().length, seen.size(),
              "InjuryType must be unique across all ProstheticType entries");
    }

    @Test
    @DisplayName("getProstheticFromInjury round-trips for all ProstheticType injury mappings")
    public void getProstheticFromInjury_roundTrip() {
        for (ProstheticType prostheticType : ProstheticType.values()) {
            InjuryType injuryType = prostheticType.getInjuryType();
            if (injuryType.getSubType().isPermanentModification()) {
                ProstheticType resolved = ProstheticType.getProstheticFromInjury(injuryType);
                assertSame(prostheticType, resolved, () -> "Expected round-trip to return " + prostheticType.name());
            }
        }
    }

    @Test
    @DisplayName("getProstheticFromInjury returns null for non-permanent modification injuries")
    public void getProstheticFromInjury_returnsNullForNonPermanent() {
        InjuryType nonPermanent = AlternateInjuries.FRACTURED_RIB;
        assertNotNull(nonPermanent);
        assertNull(ProstheticType.getProstheticFromInjury(nonPermanent),
              "Non-permanent injuries must not map to a ProstheticType");
    }
}
