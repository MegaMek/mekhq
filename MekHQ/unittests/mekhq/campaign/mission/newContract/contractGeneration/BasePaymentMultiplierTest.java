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
package mekhq.campaign.mission.newContract.contractGeneration;

import static mekhq.campaign.mission.newContract.contractGeneration.BasePaymentMultiplier.GENEROUS;
import static mekhq.campaign.mission.newContract.contractGeneration.BasePaymentMultiplier.MISERLY;
import static mekhq.campaign.mission.newContract.contractGeneration.BasePaymentMultiplier.NORMAL;
import static mekhq.campaign.mission.newContract.contractGeneration.BasePaymentMultiplier.SPENDTHRIFT;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BasePaymentMultiplierTest {
    @Test
    public void testFromString_ValidName() {
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString(GENEROUS.name());

        assertEquals(GENEROUS, result);
    }

    @Test
    public void testFromString_CaseInsensitive() {
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString("generous");

        assertEquals(GENEROUS, result);
    }

    @Test
    public void testFromString_ReplacesSpacesWithUnderscores() {
        // valueOf() replaces spaces with underscores; verify the substitution path resolves.
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString("spendthrift");

        assertEquals(SPENDTHRIFT, result);
    }

    @Test
    public void testFromString_FromOrdinal() {
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString(MISERLY.ordinal() + "");

        assertEquals(MISERLY, result);
    }

    @Test
    public void testFromString_AllOrdinals() {
        for (BasePaymentMultiplier value : BasePaymentMultiplier.values()) {
            BasePaymentMultiplier result = BasePaymentMultiplier.fromString(String.valueOf(value.ordinal()));

            assertEquals(value, result);
        }
    }

    @Test
    public void testFromString_OrdinalOutOfRange_ReturnsNormal() {
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString("99");

        assertEquals(NORMAL, result);
    }

    @Test
    public void testFromString_Invalid_ReturnsNormal() {
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString("INVALID_multiplier");

        assertEquals(NORMAL, result);
    }

    @Test
    public void testFromString_Null_ReturnsNormal() {
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString(null);

        assertEquals(NORMAL, result);
    }

    @Test
    public void testFromString_EmptyString_ReturnsNormal() {
        BasePaymentMultiplier result = BasePaymentMultiplier.fromString("");

        assertEquals(NORMAL, result);
    }

    @Test
    public void testFromString_RoundTripsAllValues() {
        for (BasePaymentMultiplier value : BasePaymentMultiplier.values()) {
            assertEquals(value, BasePaymentMultiplier.fromString(value.name()));
        }
    }

    @Test
    public void testGetMultiplier_Values() {
        assertEquals(1.5, GENEROUS.getMultiplier());
        assertEquals(1.0, NORMAL.getMultiplier());
        assertEquals(0.5, SPENDTHRIFT.getMultiplier());
        assertEquals(0.2, MISERLY.getMultiplier());
    }

    @ParameterizedTest
    @EnumSource(BasePaymentMultiplier.class)
    public void testGetLabel_notInvalid(BasePaymentMultiplier value) {
        String label = value.getLabel();

        assertNotNull(label);
        assertTrue(isResourceKeyValid(label), "Missing resource for " + value.name());
    }

    @ParameterizedTest
    @EnumSource(BasePaymentMultiplier.class)
    public void testToString_MatchesLabel(BasePaymentMultiplier value) {
        assertEquals(value.getLabel(), value.toString());
    }
}
