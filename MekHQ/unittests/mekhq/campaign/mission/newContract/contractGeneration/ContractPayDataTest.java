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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import java.util.UUID;

import mekhq.campaign.finances.Money;
import org.junit.jupiter.api.Test;

class ContractPayDataTest {
    private static ContractBasePayData basePayData() {
        return new ContractBasePayData(Money.of(100), Money.of(200), Money.of(300));
    }

    private static TransitPayData transitPayData() {
        return new TransitPayData(Money.of(10), 3, 1.5, 0.75, Money.of(40));
    }

    private static Map<UUID, ObjectivePayData> objectivePayDataMap() {
        UUID id = UUID.randomUUID();
        return Map.of(id, new ObjectivePayData(Money.of(5), 2, 1.1, 1.2, 0.9, Money.of(25)));
    }

    /** Builds a ContractPayData via the convenience (four-argument) constructor. */
    private static ContractPayData baseline() {
        return new ContractPayData(basePayData(), transitPayData(), objectivePayDataMap(), Money.of(500));
    }

    @Test
    public void testConvenienceConstructor_DefaultsPayFieldsToZero() {
        ContractPayData data = baseline();

        assertEquals(0.0, data.straightSupportMultiplier());
        assertEquals(Money.zero(), data.straightSupportEstimate());
        assertEquals(0.0, data.transportPaymentMultiplier());
        assertEquals(Money.zero(), data.transportPayment());
    }

    @Test
    public void testConvenienceConstructor_PreservesProvidedFields() {
        ContractBasePayData basePay = basePayData();
        TransitPayData transit = transitPayData();
        Map<UUID, ObjectivePayData> objectives = objectivePayDataMap();
        Money totalObjectivePay = Money.of(500);

        ContractPayData data = new ContractPayData(basePay, transit, objectives, totalObjectivePay);

        assertSame(basePay, data.basePayData());
        assertSame(transit, data.transitPayData());
        assertSame(objectives, data.objectivePayDataMap());
        assertEquals(totalObjectivePay, data.totalObjectivePay());
    }

    @Test
    public void testCanonicalConstructor_AllAccessors() {
        ContractBasePayData basePay = basePayData();
        TransitPayData transit = transitPayData();
        Map<UUID, ObjectivePayData> objectives = objectivePayDataMap();
        Money totalObjectivePay = Money.of(500);
        Money straightSupportEstimate = Money.of(600);
        Money transportPayment = Money.of(700);

        ContractPayData data = new ContractPayData(basePay, transit, objectives, totalObjectivePay,
              2.5, straightSupportEstimate, 3.5, transportPayment);

        assertSame(basePay, data.basePayData());
        assertSame(transit, data.transitPayData());
        assertSame(objectives, data.objectivePayDataMap());
        assertEquals(totalObjectivePay, data.totalObjectivePay());
        assertEquals(2.5, data.straightSupportMultiplier());
        assertEquals(straightSupportEstimate, data.straightSupportEstimate());
        assertEquals(3.5, data.transportPaymentMultiplier());
        assertEquals(transportPayment, data.transportPayment());
    }

    @Test
    public void testRebuildIncludingStraightSupport_UpdatesStraightSupportFields() {
        ContractPayData data = baseline().rebuildIncludingStraightSupport(2.5, Money.of(600));

        assertEquals(2.5, data.straightSupportMultiplier());
        assertEquals(Money.of(600), data.straightSupportEstimate());
    }

    @Test
    public void testRebuildIncludingStraightSupport_LeavesTransportAndOthersUnchanged() {
        ContractPayData original = baseline();

        ContractPayData data = original.rebuildIncludingStraightSupport(2.5, Money.of(600));

        // Transport fields are carried over untouched.
        assertEquals(original.transportPaymentMultiplier(), data.transportPaymentMultiplier());
        assertEquals(original.transportPayment(), data.transportPayment());
        // Remaining fields are carried over untouched.
        assertSame(original.basePayData(), data.basePayData());
        assertSame(original.transitPayData(), data.transitPayData());
        assertSame(original.objectivePayDataMap(), data.objectivePayDataMap());
        assertEquals(original.totalObjectivePay(), data.totalObjectivePay());
    }

    @Test
    public void testRebuildIncludingTransportPay_UpdatesTransportFields() {
        ContractPayData data = baseline().rebuildIncludingTransportPay(3.5, Money.of(700));

        assertEquals(3.5, data.transportPaymentMultiplier());
        assertEquals(Money.of(700), data.transportPayment());
    }

    @Test
    public void testRebuildIncludingTransportPay_LeavesStraightSupportAndOthersUnchanged() {
        ContractPayData original = baseline().rebuildIncludingStraightSupport(2.5, Money.of(600));

        ContractPayData data = original.rebuildIncludingTransportPay(3.5, Money.of(700));

        // Straight-support fields set earlier are carried over untouched.
        assertEquals(2.5, data.straightSupportMultiplier());
        assertEquals(Money.of(600), data.straightSupportEstimate());
        // Remaining fields are carried over untouched.
        assertSame(original.basePayData(), data.basePayData());
        assertSame(original.transitPayData(), data.transitPayData());
        assertSame(original.objectivePayDataMap(), data.objectivePayDataMap());
        assertEquals(original.totalObjectivePay(), data.totalObjectivePay());
    }

    @Test
    public void testRebuild_ReturnsNewInstance() {
        ContractPayData original = baseline();

        assertNotSame(original, original.rebuildIncludingStraightSupport(2.5, Money.of(600)));
        assertNotSame(original, original.rebuildIncludingTransportPay(3.5, Money.of(700)));
    }

    @Test
    public void testRebuild_Chained_ProducesFullyPopulatedData() {
        ContractPayData data = baseline()
                                     .rebuildIncludingStraightSupport(2.5, Money.of(600))
                                     .rebuildIncludingTransportPay(3.5, Money.of(700));

        assertEquals(2.5, data.straightSupportMultiplier());
        assertEquals(Money.of(600), data.straightSupportEstimate());
        assertEquals(3.5, data.transportPaymentMultiplier());
        assertEquals(Money.of(700), data.transportPayment());
    }

    @Test
    public void testEqualsAndHashCode() {
        ContractBasePayData basePay = basePayData();
        TransitPayData transit = transitPayData();
        Map<UUID, ObjectivePayData> objectives = objectivePayDataMap();

        ContractPayData first = new ContractPayData(basePay, transit, objectives, Money.of(500),
              2.5, Money.of(600), 3.5, Money.of(700));
        ContractPayData second = new ContractPayData(basePay, transit, objectives, Money.of(500),
              2.5, Money.of(600), 3.5, Money.of(700));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }
}
