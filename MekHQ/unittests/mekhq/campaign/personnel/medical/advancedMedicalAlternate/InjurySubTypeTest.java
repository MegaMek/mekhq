/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link InjurySubType}, covering the wing implant subtype and mutual exclusivity.
 */
class InjurySubTypeTest {
    private static final Set<InjurySubType> MUTUALLY_EXCLUSIVE =
          Set.of(InjurySubType.IMPLANT_VDNI, InjurySubType.IMPLANT_WINGS);

    @Test
    void wingsAreAPermanentImplant() {
        assertTrue(InjurySubType.IMPLANT_WINGS.isImplant());
        assertTrue(InjurySubType.IMPLANT_WINGS.isPermanentModification());
        assertFalse(InjurySubType.IMPLANT_WINGS.isProsthetic(),
              "wings are an implant, so fitting them leaves other prosthetics at the spine in place");
    }

    @ParameterizedTest
    @EnumSource(InjurySubType.class)
    void onlyVdniAndWingsAreMutuallyExclusive(InjurySubType subType) {
        assertEquals(MUTUALLY_EXCLUSIVE.contains(subType), subType.isMutuallyExclusiveImplant(),
              subType + " has the wrong exclusivity");
    }

    @ParameterizedTest
    @EnumSource(InjurySubType.class)
    void everyMutuallyExclusiveSubtypeIsAnImplant(InjurySubType subType) {
        if (subType.isMutuallyExclusiveImplant()) {
            assertTrue(subType.isImplant(), subType + " is exclusive but not an implant");
        }
    }

    @Test
    void genericImplantsCanBeCombined() {
        assertFalse(InjurySubType.IMPLANT_GENERIC.isMutuallyExclusiveImplant());
    }
}
