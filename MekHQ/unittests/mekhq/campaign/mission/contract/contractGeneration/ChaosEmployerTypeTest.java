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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Locks the negotiation modifiers and the classification predicates of {@link ChaosEmployerType}. The modifier table is
 * transcribed from Hot Spots: Draconis Reach (pg 144, first printing), so any accidental edit to a value is a test
 * failure rather than a silent balance change.
 */
class ChaosEmployerTypeTest {

    private static final Set<ChaosEmployerType> CURRENT_SYSTEM_EMPLOYERS =
          EnumSet.of(ChaosEmployerType.LOCAL_SYSTEM_OWNER, ChaosEmployerType.LOCAL_PLANETARY_GOVERNMENT);

    private static final Set<ChaosEmployerType> SYSTEM_OWNERS =
          EnumSet.of(ChaosEmployerType.LOCAL_SYSTEM_OWNER, ChaosEmployerType.ANY_SYSTEM_OWNER);

    // employer, pay, support, transport, salvage, command
    @ParameterizedTest
    @CsvSource({ "ANY_PLANETARY_GOVERNMENT, 0, 1, 0, 1, 0",
                 "ANY_SYSTEM_OWNER, 1, 2, 1, -2, -3",
                 "CIVILIAN_ORGANIZATION_BUSINESS, -2, -2, -1, 4, 4",
                 "CIVILIAN_ORGANIZATION_MILITIA, -2, -2, -1, 4, 4",
                 "CIVILIAN_ORGANIZATION_REBELS, -2, -2, -1, 4, 4",
                 "CORPORATION, 2, -2, 1, 2, 0",
                 "LOCAL_PLANETARY_GOVERNMENT, 0, 1, 0, 1, 0",
                 "LOCAL_SYSTEM_OWNER, 0, 2, 1, -2, -3",
                 "MERCENARY_SUBCONTRACT, 0, 0, 0, 0, 3",
                 "NOBLE, 0, 0, 0, 0, 0" })
    void negotiationModifiersMatchTheSourcebook(final ChaosEmployerType employerType, final int pay,
          final int support, final int transport, final int salvage, final int command) {
        assertEquals(pay, employerType.getPayRateModifier(), "pay rate modifier for " + employerType);
        assertEquals(support, employerType.getSupportModifier(), "support modifier for " + employerType);
        assertEquals(transport, employerType.getTransportModifier(), "transport modifier for " + employerType);
        assertEquals(salvage, employerType.getSalvageRightsModifier(), "salvage rights modifier for " + employerType);
        assertEquals(command, employerType.getCommandRightsModifier(), "command rights modifier for " + employerType);
    }

    @ParameterizedTest
    @EnumSource(ChaosEmployerType.class)
    void isCurrentSystemEmployerOnlyForLocalTypes(final ChaosEmployerType employerType) {
        assertEquals(CURRENT_SYSTEM_EMPLOYERS.contains(employerType), employerType.isCurrentSystemEmployer(),
              "isCurrentSystemEmployer for " + employerType);
    }

    @ParameterizedTest
    @EnumSource(ChaosEmployerType.class)
    void isSystemOwnerOnlyForSystemOwnerTypes(final ChaosEmployerType employerType) {
        assertEquals(SYSTEM_OWNERS.contains(employerType), employerType.isSystemOwner(),
              "isSystemOwner for " + employerType);
    }
}
