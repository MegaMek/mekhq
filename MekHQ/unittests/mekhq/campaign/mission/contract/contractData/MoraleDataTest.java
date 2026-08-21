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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link MoraleData}'s convenience constructor, which describes an enemy that is not routed: no rout window and
 * nothing owed. A non-zero payout leaking through that constructor would credit the player for a rout that never
 * happened.
 */
class MoraleDataTest {
    @ParameterizedTest
    @EnumSource(ContractMoraleLevel.class)
    void theConvenienceConstructorDescribesAnEnemyThatIsNotRouted(ContractMoraleLevel moraleLevel) {
        MoraleData data = new MoraleData(moraleLevel);

        assertEquals(moraleLevel, data.moraleLevel());
        assertNull(data.routEndDate(), "no rout is in progress");
        assertTrue(data.routedPayout().isZero(), "nothing is owed for a rout that has not happened");
    }

    @Test
    void routStateIsCarriedWhenSuppliedInFull() {
        MoraleData data = new MoraleData(ContractMoraleLevel.ROUTED,
              java.time.LocalDate.of(3051, 6, 1),
              Money.of(250_000));

        assertEquals(ContractMoraleLevel.ROUTED, data.moraleLevel());
        assertEquals(java.time.LocalDate.of(3051, 6, 1), data.routEndDate());
        assertEquals(Money.of(250_000), data.routedPayout());
    }
}
