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
package mekhq.campaign.market.contractMarket;

import static megamek.common.units.EntityWeightClass.WEIGHT_ASSAULT;
import static megamek.common.units.EntityWeightClass.WEIGHT_HEAVY;
import static megamek.common.units.EntityWeightClass.WEIGHT_LIGHT;
import static megamek.common.units.EntityWeightClass.WEIGHT_MEDIUM;
import static megamek.common.units.EntityWeightClass.WEIGHT_SUPER_HEAVY;
import static megamek.common.units.EntityWeightClass.WEIGHT_ULTRA_LIGHT;
import static mekhq.campaign.market.contractMarket.AlternatePaymentModelValues.DIMINISHING_RETURNS_FLOOR;
import static mekhq.campaign.market.contractMarket.AlternatePaymentModelValues.DIMINISHING_RETURNS_SLOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Hangar;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AlternatePaymentModelValuesTest {
    @Test
    void getValue_returnsTheConfiguredMoneyValue() {
        assertEquals(Money.of(3_000_000), AlternatePaymentModelValues.AEROSPACE_FIGHTER_LIGHT.getValue());
        assertEquals(Money.of(750_000), AlternatePaymentModelValues.BATTLE_ARMOR_PER_SUIT.getValue());
        assertEquals(Money.of(50_000_000), AlternatePaymentModelValues.LARGE_CRAFT.getValue());
    }

    @Test
    void getForceValue_skipsNonStandardForces() {
        Force nonStandard = mockForce(false, true, List.of(mockUnitWithEntity(mock(Entity.class))));
        Money total = AlternatePaymentModelValues.getForceValue(
              dummyFaction(),
              List.of(nonStandard),
              mock(Hangar.class),
              false,
              false,
              100, 100, 100, 100
        );
        assertEquals(Money.zero(), total);
    }

    @Test
    void getForceValue_skipsNonCombatRoleForces() {
        Force nonCombat = mockForce(true, false, List.of(mockUnitWithEntity(mock(Entity.class))));
        Money total = AlternatePaymentModelValues.getForceValue(
              dummyFaction(),
              List.of(nonCombat),
              mock(Hangar.class),
              false,
              false,
              100, 100, 100, 100
        );
        assertEquals(Money.zero(), total);
    }

    @Test
    void getForceValue_skipsNullUnitsAndNullEntities() {
        Unit nullEntityUnit = mock(Unit.class);
        when(nullEntityUnit.getEntity()).thenReturn(null);

        List<Unit> units = new ArrayList<>();
        units.add(null);
        units.add(nullEntityUnit);

        Force force = mockForce(true, true, units);

        Money total = AlternatePaymentModelValues.getForceValue(
              dummyFaction(),
              List.of(force),
              mock(Hangar.class),
              false,
              false,
              100, 100, 100, 100
        );

        assertEquals(Money.zero(), total);
    }

    @Test
    void getForceValue_doesNotUseDiminishingReturnsWhenDisabled_evenIfUnitCountExceedsStart() {
        Faction faction = dummyFaction();
        Hangar hangar = mock(Hangar.class);

        // 3 ProtoMeks @ 100% combat multiplier => raw total = 3,000,000
        Force force = mockForce(true, true, List.of(
              mockUnitWithEntity(mockProtoMekEntity()),
              mockUnitWithEntity(mockProtoMekEntity()),
              mockUnitWithEntity(mockProtoMekEntity())
        ));

        // Make diminishingReturnsStart = 0 so the force is definitely "affected" (size > start).
        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(0);

            Money actual = AlternatePaymentModelValues.getForceValue(
                  faction,
                  List.of(force),
                  hangar,
                  false, // useDiminishingContractPay DISABLED
                  false,
                  100, 0, 0, 0
            );

            Money rawExpected = AlternatePaymentModelValues.PROTOMEK.getValue().multipliedBy(3);
            assertEquals(rawExpected, actual, "When diminishing pay is disabled, total must be the raw sum");
        }
    }

    @Test
    void getForceValue_usesDiminishingReturnsOnlyWhenEnabled_andUnitCountExceedsStart() {
        Faction faction = dummyFaction();
        Hangar hangar = mock(Hangar.class);

        // 3 ProtoMeks @ 100% combat multiplier => raw total = 3,000,000
        Force force = mockForce(true, true, List.of(
              mockUnitWithEntity(mockProtoMekEntity()),
              mockUnitWithEntity(mockProtoMekEntity()),
              mockUnitWithEntity(mockProtoMekEntity())
        ));

        // Make diminishingReturnsStart = 2 (standard force size=1 => cutoff=2),
        // so with 3 units we exceed the start and should see discounting.
        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(1);

            Money actual = AlternatePaymentModelValues.getForceValue(
                  faction,
                  List.of(force),
                  hangar,
                  true, // useDiminishingContractPay ENABLED
                  false,
                  100, 0, 0, 0
            );

            List<Money> unitValues = new ArrayList<>(List.of(
                  AlternatePaymentModelValues.PROTOMEK.getValue(),
                  AlternatePaymentModelValues.PROTOMEK.getValue(),
                  AlternatePaymentModelValues.PROTOMEK.getValue()
            ));
            Money diminishedExpected = AlternatePaymentModelValues.adjustValuesForDiminishingReturns(faction,
                  unitValues);

            assertEquals(diminishedExpected, actual,
                  "When enabled and unit count exceeds start, total must be the diminished-returns sum");
        }
    }

    @Test
    void getForceValue_appliesPercentMultipliersByDividingBy100_usingProtoMekBranch() {
        Entity protoMek = mock(Entity.class);
        when(protoMek.isProtoMek()).thenReturn(true);

        Unit unit = mockUnitWithEntity(protoMek);
        Force force = mockForce(true, true, List.of(unit));

        // 50% combat multiplier => PROTOMEK(1_000_000) * 0.5 = 500_000
        Money total = AlternatePaymentModelValues.getForceValue(
              dummyFaction(),
              List.of(force),
              mock(Hangar.class),
              false,
              false,
              50, 0, 0, 0
        );

        assertEquals(Money.of(500_000), total);
    }

    @Nested
    class GetUnitContractValueTests {

        @Test
        void largeCraft_dropShip_usesDropShipMultiplier() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isLargeCraft()).thenReturn(true);
            when(entity.isDropShip()).thenReturn(true);

            Money result = invokeGetUnitContractValue(entity, false, 0, 0.10, 0.20, 0.30);
            assertEquals(AlternatePaymentModelValues.LARGE_CRAFT.getValue().multipliedBy(0.10), result);
        }

        @Test
        void largeCraft_jumpShip_usesJumpShipMultiplier() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isLargeCraft()).thenReturn(true);
            when(entity.isDropShip()).thenReturn(false);
            when(entity.isJumpShip()).thenReturn(true);

            Money result = invokeGetUnitContractValue(entity, false, 0, 0.10, 0.20, 0.30);
            assertEquals(AlternatePaymentModelValues.LARGE_CRAFT.getValue().multipliedBy(0.30), result);
        }

        @Test
        void largeCraft_warShipElseBranch_usesWarShipMultiplier() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isLargeCraft()).thenReturn(true);
            when(entity.isDropShip()).thenReturn(false);
            when(entity.isJumpShip()).thenReturn(false);

            Money result = invokeGetUnitContractValue(entity, false, 0, 0.10, 0.20, 0.30);
            assertEquals(AlternatePaymentModelValues.LARGE_CRAFT.getValue().multipliedBy(0.20), result);
        }

        @Test
        void protoMek_usesCombatMultiplier() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isProtoMek()).thenReturn(true);

            Money result = invokeGetUnitContractValue(entity, false, 0.5, 0, 0, 0);
            assertEquals(AlternatePaymentModelValues.PROTOMEK.getValue().multipliedBy(0.5), result);
        }

        @Test
        void supportVehicle_weightBands_areCorrect() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isSupportVehicle()).thenReturn(true);

            when(entity.getWeight()).thenReturn(4.99);
            assertEquals(AlternatePaymentModelValues.SUPPORT_VEHICLE_LIGHT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeight()).thenReturn(5.0);
            assertEquals(AlternatePaymentModelValues.SUPPORT_VEHICLE_MEDIUM.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeight()).thenReturn(100.0);
            assertEquals(AlternatePaymentModelValues.SUPPORT_VEHICLE_MEDIUM.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeight()).thenReturn(100.01);
            assertEquals(AlternatePaymentModelValues.SUPPORT_VEHICLE_HEAVY.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeight()).thenReturn(1000.0);
            assertEquals(AlternatePaymentModelValues.SUPPORT_VEHICLE_HEAVY.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeight()).thenReturn(1000.01);
            assertEquals(AlternatePaymentModelValues.SUPPORT_VEHICLE_SUPER_HEAVY.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));
        }

        @Test
        void aerospaceFighter_weightClasses_mapCorrectly() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isAerospaceFighter()).thenReturn(true);

            when(entity.getWeightClass()).thenReturn(WEIGHT_LIGHT);
            assertEquals(AlternatePaymentModelValues.AEROSPACE_FIGHTER_LIGHT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_MEDIUM);
            assertEquals(AlternatePaymentModelValues.AEROSPACE_FIGHTER_MEDIUM.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_HEAVY);
            assertEquals(AlternatePaymentModelValues.AEROSPACE_FIGHTER_HEAVY.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));
        }

        @Test
        void battleMek_weightClasses_mapCorrectly() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isBattleMek()).thenReturn(true);

            when(entity.getWeightClass()).thenReturn(WEIGHT_ULTRA_LIGHT);
            assertEquals(AlternatePaymentModelValues.BATTLEMEK_LIGHT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_LIGHT);
            assertEquals(AlternatePaymentModelValues.BATTLEMEK_LIGHT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_MEDIUM);
            assertEquals(AlternatePaymentModelValues.BATTLEMEK_MEDIUM.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_HEAVY);
            assertEquals(AlternatePaymentModelValues.BATTLEMEK_HEAVY.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_ASSAULT);
            assertEquals(AlternatePaymentModelValues.BATTLEMEK_ASSAULT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_SUPER_HEAVY);
            assertEquals(AlternatePaymentModelValues.BATTLEMEK_SUPER_HEAVY.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));
        }

        @Test
        void combatVehicle_weightClasses_mapCorrectly() throws Exception {
            Entity entity = mock(Entity.class);
            when(entity.isCombatVehicle()).thenReturn(true);

            when(entity.getWeightClass()).thenReturn(WEIGHT_ULTRA_LIGHT);
            assertEquals(AlternatePaymentModelValues.COMBAT_VEHICLE_LIGHT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_LIGHT);
            assertEquals(AlternatePaymentModelValues.COMBAT_VEHICLE_LIGHT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_MEDIUM);
            assertEquals(AlternatePaymentModelValues.COMBAT_VEHICLE_MEDIUM.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_HEAVY);
            assertEquals(AlternatePaymentModelValues.COMBAT_VEHICLE_HEAVY.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_ASSAULT);
            assertEquals(AlternatePaymentModelValues.COMBAT_VEHICLE_ASSAULT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));

            when(entity.getWeightClass()).thenReturn(WEIGHT_SUPER_HEAVY);
            // Implementation maps SUPER_HEAVY to ASSAULT value
            assertEquals(AlternatePaymentModelValues.COMBAT_VEHICLE_ASSAULT.getValue(),
                  invokeGetUnitContractValue(entity, false, 1.0, 0, 0, 0));
        }

        @Test
        void unknownEntityType_returnsZero() throws Exception {
            Entity entity = mock(Entity.class);
            Money result = invokeGetUnitContractValue(entity, false, 1.0, 1.0, 1.0, 1.0);
            assertEquals(Money.zero(), result);
        }
    }

    private static Force mockForce(boolean isStandard, boolean isCombatRole, List<Unit> units) {
        Force force = mock(Force.class, RETURNS_DEEP_STUBS);

        when(force.getForceType().isStandard()).thenReturn(isStandard);
        when(force.getCombatRoleInMemory().isCombatRole()).thenReturn(isCombatRole);

        when(force.getUnitsAsUnits(any(Hangar.class))).thenReturn(units);

        return force;
    }

    private static Unit mockUnitWithEntity(Entity entity) {
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    private static Entity mockProtoMekEntity() {
        Entity entity = mock(Entity.class);
        when(entity.isProtoMek()).thenReturn(true);
        return entity;
    }

    private static Money invokeGetUnitContractValue(Entity entity, boolean excludeInfantry,
          double combatMultiplier, double dropShipMultiplier, double warShipMultiplier, double jumpShipMultiplier)
          throws Exception {

        Method getUnitContractValue = AlternatePaymentModelValues.class.getDeclaredMethod(
              "getUnitContractValue",
              Entity.class,
              boolean.class,
              double.class,
              double.class,
              double.class,
              double.class
        );
        getUnitContractValue.setAccessible(true);
        return (Money) getUnitContractValue.invoke(null, entity, excludeInfantry, combatMultiplier,
              dropShipMultiplier, warShipMultiplier, jumpShipMultiplier);
    }

    @Test
    void returnsZeroForEmptyList() throws Exception {
        Faction faction = dummyFaction();

        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            // Even if this is called, it shouldn't matter for an empty list
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(36);

            Money result = invokeAdjustValuesForDiminishingReturns(faction, new ArrayList<>());

            assertEquals(Money.zero(), result);
        }
    }

    @Test
    void sortsListDescendingInPlace() throws Exception {
        Faction faction = dummyFaction();
        List<Money> values = new ArrayList<>(List.of(
              Money.of(5),
              Money.of(1),
              Money.of(10),
              Money.of(3)
        ));

        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            // Start way after list size so no discount; we just verify sorting side-effect
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(999);

            Money result = invokeAdjustValuesForDiminishingReturns(faction, values);

            assertEquals(Money.of(19), result);
            assertEquals(List.of(Money.of(10), Money.of(5), Money.of(3), Money.of(1)), values,
                  "Method should sort unitValues descending in-place");
        }
    }

    @Test
    void appliesNoDiscountBeforeCutoff() throws Exception {
        Faction faction = dummyFaction();

        // If standard force size = 2, cutoff = 2 * 2 = 4
        // List size 4 => indices 0..3 => no i >= 4 => no discount
        List<Money> values = new ArrayList<>(List.of(
              Money.of(10),
              Money.of(10),
              Money.of(10),
              Money.of(10)
        ));

        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(2);

            Money result = invokeAdjustValuesForDiminishingReturns(faction, values);

            assertEquals(Money.of(40), result);
        }
    }

    @Test
    void discountsUnitsAfterCutoff_usingFormula() throws Exception {
        Faction faction = dummyFaction();

        // Mock cutoff small so we can test easily:
        // standard force size = 1 => cutoff = 2
        // indices:
        // 0,1 => full value
        // 2 => distance=1 => 1/(1+slope*1)
        // 3 => distance=2 => 1/(1+slope*2)
        List<Money> values = new ArrayList<>(List.of(
              Money.of(1),   // will be sorted
              Money.of(100),
              Money.of(10),
              Money.of(1)
        ));

        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(1);

            // Build expected using the exact same math & Money operations as the method (avoids rounding mismatches)
            List<Money> sorted = new ArrayList<>(values);
            sorted.sort(Money::compareTo);
            Collections.reverse(sorted);

            final int diminishingReturnsStart = 2;

            Money expected = Money.zero();
            for (int i = 0; i < sorted.size(); i++) {
                Money unitValue = sorted.get(i);
                double multiplier = 1.0;
                if (i >= diminishingReturnsStart) {
                    int distance = (i - diminishingReturnsStart) + 1;
                    multiplier = 1.0 / (1.0 + DIMINISHING_RETURNS_SLOPE * distance);
                    multiplier = Math.max(DIMINISHING_RETURNS_FLOOR, multiplier);
                }
                expected = expected.plus(unitValue.multipliedBy(multiplier));
            }

            Money actual = invokeAdjustValuesForDiminishingReturns(faction, values);

            assertEquals(expected, actual);
        }
    }

    @Test
    void hitsFloorAtExpectedDistance_whenCutoffIsSmallEnough() throws Exception {
        Faction faction = dummyFaction();

        // With slope=0.1233 and minMultiplier=0.10, floor begins when:
        // 1/(1 + slope*distance) <= 0.10  => distance >= 73 (since 9/0.1233 ≈ 72.98).
        //
        // Use standard force size = 1 => cutoff = 2, so index for first floored unit is:
        // i = cutoff + distance - 1 = 2 + 73 - 1 = 74 (0-based)
        // => list size must be >= 75.
        List<Money> values = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            values.add(Money.of(1));
        }

        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(1);

            Money result = invokeAdjustValuesForDiminishingReturns(faction, values);

            final int diminishingReturnsStart = 2;

            Money expected = Money.zero();
            for (int i = 0; i < 75; i++) {
                double multiplier = 1.0;
                if (i >= diminishingReturnsStart) {
                    int distance = (i - diminishingReturnsStart) + 1;
                    multiplier = 1.0 / (1.0 + DIMINISHING_RETURNS_SLOPE * distance);
                    multiplier = Math.max(DIMINISHING_RETURNS_FLOOR, multiplier);
                }
                expected = expected.plus(Money.of(1).multipliedBy(multiplier));
            }

            assertEquals(expected, result);
        }
    }

    @Test
    void discountsLeastValuableUnitsFirst_dueToDescendingSort() throws Exception {
        Faction faction = dummyFaction();

        // cutoff=2 (standard force size=1)
        // After sorting desc => [1000, 100, 10, 1]
        // The discounted units are indices 2 and 3 => values 10 and 1 get discounted (least valuable first).
        List<Money> values = new ArrayList<>(Arrays.asList(
              Money.of(1),
              Money.of(10),
              Money.of(100),
              Money.of(1000)
        ));

        try (MockedStatic<CombatTeam> combatTeam = mockStatic(CombatTeam.class)) {
            combatTeam.when(() -> CombatTeam.getStandardForceSize(any(), anyInt()))
                  .thenReturn(1);

            Money actual = invokeAdjustValuesForDiminishingReturns(faction, values);

            List<Money> sorted = new ArrayList<>(values);
            sorted.sort(Money::compareTo);
            Collections.reverse(sorted);

            final int diminishingReturnsStart = 2;

            Money expected = Money.zero();
            for (int i = 0; i < sorted.size(); i++) {
                Money unitValue = sorted.get(i);
                double multiplier = 1.0;
                if (i >= diminishingReturnsStart) {
                    int distance = (i - diminishingReturnsStart) + 1;
                    multiplier = 1.0 / (1.0 + DIMINISHING_RETURNS_SLOPE * distance);
                    multiplier = Math.max(DIMINISHING_RETURNS_FLOOR, multiplier);
                }
                expected = expected.plus(unitValue.multipliedBy(multiplier));
            }

            assertEquals(expected, actual);
        }
    }

    private static Money invokeAdjustValuesForDiminishingReturns(Faction campaignFaction, List<Money> unitValues)
          throws Exception {
        Method adjustValuesForDiminishingReturns = AlternatePaymentModelValues.class.getDeclaredMethod(
              "adjustValuesForDiminishingReturns",
              Faction.class,
              List.class
        );
        adjustValuesForDiminishingReturns.setAccessible(true);
        return (Money) adjustValuesForDiminishingReturns.invoke(null, campaignFaction, unitValues);
    }

    private static Faction dummyFaction() {
        // The method under test only passes this through to CombatTeam.getStandardForceSize,
        // which we mock, so this can be any non-null instance.
        return mock(Faction.class);
    }
}
