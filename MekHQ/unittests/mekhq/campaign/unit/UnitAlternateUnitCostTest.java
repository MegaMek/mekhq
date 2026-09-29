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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartQuality;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import testUtilities.MHQTestUtilities;

/**
 * Tests "Alternate Unit Cost": a unit is worth its current Battle Value (without pilot, C3, or TAG) in support points,
 * scaled by the tech-base unit price multiplier and converted to C-bills when support point conversion is on. Units
 * always sell for half that.
 */
class UnitAlternateUnitCostTest {
    private static final int SUPPORT_POINTS_TO_C_BILLS = 10_000;

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
    }

    /** Exact arithmetic, option handling, and caching, against a mocked entity. */
    @Nested
    class WithMockedEntity {
        private Campaign campaign;
        private CampaignOptions campaignOptions;
        private Entity entity;
        private List<Mounted<?>> equipment;
        private Unit unit;

        @BeforeEach
        void setUp() {
            campaign = mockCampaign();
            campaignOptions = new CampaignOptions();
            campaignOptions.set(CampaignOption.USE_ALTERNATE_UNIT_COST, true);
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
            campaignOptions.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 1.0);
            campaignOptions.set(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER, 1.0);
            campaignOptions.set(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER, 1.0);
            when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

            entity = mock(Mek.class);
            equipment = new ArrayList<>();
            when(entity.getEquipment()).thenAnswer(invocation -> equipment);
            when(entity.getTotalArmor()).thenReturn(100);
            when(entity.getTotalInternal()).thenReturn(50);
            when(entity.calculateBattleValue(true, true, true)).thenReturn(500);
            when(entity.getCost(false)).thenReturn(3_000_000.0);

            unit = new Unit(entity, campaign);
        }

        private Mounted<?> addEquipment() {
            Mounted<?> mounted = mock(Mounted.class);
            equipment.add(mounted);
            return mounted;
        }

        @Test
        void buyCostIsBattleValueInSupportPointsConvertedToCBills() {
            // BV 500 -> 500 SP -> 5,000,000 C-bills
            assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            assertEquals(unit.getBuyCost(), unit.getAlternateUnitCost());
        }

        @Test
        void sellValueIsAlwaysHalfTheBuyCost() {
            assertEquals(Money.of(2_500_000), unit.getSellValue());
        }

        @Test
        void withoutConversionTheValueIsInRawSupportPoints() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);

            assertEquals(Money.of(500), unit.getBuyCost());
            assertEquals(Money.of(250), unit.getSellValue());
        }

        @Test
        void oddBattleValueSellsForAnExactHalf() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);
            when(entity.calculateBattleValue(true, true, true)).thenReturn(501);
            when(entity.getTotalArmor()).thenReturn(99);

            assertEquals(Money.of(250.5), unit.getSellValue());
        }

        @Test
        void zeroBattleValueIsWorthNothing() {
            when(entity.calculateBattleValue(true, true, true)).thenReturn(0);

            assertEquals(Money.zero(), unit.getBuyCost());
            assertEquals(Money.zero(), unit.getSellValue());
        }

        /** Pilot skill, C3 and TAG are all excluded from the value. */
        @Test
        void battleValueExcludesPilotC3AndTag() {
            unit.getBuyCost();

            verify(entity).calculateBattleValue(true, true, true);
            verify(entity, never()).calculateBattleValue(true, true);
            verify(entity, never()).calculateBattleValue(false, false, false);
        }

        @Test
        void optionOffKeepsTheConstructionCost() {
            campaignOptions.set(CampaignOption.USE_ALTERNATE_UNIT_COST, false);

            assertEquals(Money.of(3_000_000), unit.getBuyCost());
            verify(entity, never()).calculateBattleValue(anyBoolean(), anyBoolean(), anyBoolean());
        }

        @Nested
        class TechBaseMultipliers {
            @BeforeEach
            void distinctMultipliers() {
                campaignOptions.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 1.1);
                campaignOptions.set(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER, 2.0);
                campaignOptions.set(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER, 1.5);
            }

            @Test
            void innerSphereMultiplierApplies() {
                assertEquals(Money.of(500 * 1.1 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void clanMultiplierApplies() {
                when(entity.isClan()).thenReturn(true);

                assertEquals(Money.of(500 * 2.0 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
                assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getSellValue());
            }

            /** Mixed tech takes priority over Clan, as it does for construction cost. */
            @Test
            void mixedTechMultiplierTakesPriority() {
                when(entity.isClan()).thenReturn(true);
                when(entity.isMixedTech()).thenReturn(true);

                assertEquals(Money.of(500 * 1.5 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            /** The refactor into a shared helper must leave the ordinary price unchanged. */
            @ParameterizedTest
            @CsvSource({ "false, false, 3300000", "true, false, 6000000", "false, true, 4500000",
                         "true, true, 4500000" })
            void optionOffStillAppliesTheSameMultipliers(boolean isClan, boolean isMixedTech, int expected) {
                campaignOptions.set(CampaignOption.USE_ALTERNATE_UNIT_COST, false);
                when(entity.isClan()).thenReturn(isClan);
                when(entity.isMixedTech()).thenReturn(isMixedTech);

                assertEquals(Money.of(expected), unit.getBuyCost());
            }
        }

        /** A WarShip's BV times 10,000 is well past the range of an int. */
        @Test
        void largeVesselValuesDoNotOverflow() {
            when(entity.calculateBattleValue(true, true, true)).thenReturn(1_500_000);

            assertEquals(Money.of(15_000_000_000.0), unit.getBuyCost());
            assertTrue(unit.getBuyCost().isPositive());
        }

        @Nested
        class BattleValueCache {
            @Test
            void unchangedUnitReusesItsBattleValue() {
                unit.getBuyCost();
                unit.getSellValue();
                unit.getBuyCost();

                verify(entity, times(1)).calculateBattleValue(true, true, true);
            }

            @Test
            void armorDamageRecalculates() {
                unit.getBuyCost();
                when(entity.getTotalArmor()).thenReturn(60);
                when(entity.calculateBattleValue(true, true, true)).thenReturn(400);

                assertEquals(Money.of(400 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void structureDamageRecalculates() {
                unit.getBuyCost();
                when(entity.getTotalInternal()).thenReturn(30);
                when(entity.calculateBattleValue(true, true, true)).thenReturn(350);

                assertEquals(Money.of(350 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void destroyedEquipmentRecalculates() {
                Mounted<?> weapon = addEquipment();
                unit.getBuyCost();
                when(weapon.isDestroyed()).thenReturn(true);
                when(entity.calculateBattleValue(true, true, true)).thenReturn(420);

                assertEquals(Money.of(420 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void hitEquipmentRecalculates() {
                Mounted<?> weapon = addEquipment();
                unit.getBuyCost();
                when(weapon.isHit()).thenReturn(true);
                when(entity.calculateBattleValue(true, true, true)).thenReturn(430);

                assertEquals(Money.of(430 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void missingEquipmentRecalculates() {
                Mounted<?> weapon = addEquipment();
                unit.getBuyCost();
                when(weapon.isMissing()).thenReturn(true);
                when(entity.calculateBattleValue(true, true, true)).thenReturn(440);

                assertEquals(Money.of(440 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            /** Refits add or strip equipment, which must be noticed even if nothing is damaged. */
            @Test
            void addedEquipmentRecalculates() {
                unit.getBuyCost();
                addEquipment();
                when(entity.calculateBattleValue(true, true, true)).thenReturn(650);

                assertEquals(Money.of(650 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void repairsRestoreTheValue() {
                Mounted<?> weapon = addEquipment();
                assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());

                when(weapon.isDestroyed()).thenReturn(true);
                when(entity.calculateBattleValue(true, true, true)).thenReturn(420);
                assertEquals(Money.of(420 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());

                when(weapon.isDestroyed()).thenReturn(false);
                when(entity.calculateBattleValue(true, true, true)).thenReturn(500);
                assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            /** The multipliers and conversion are read live; only the Battle Value is cached. */
            @Test
            void optionChangesApplyWithoutWaitingForDamage() {
                unit.getBuyCost();
                campaignOptions.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 2.0);

                assertEquals(Money.of(1_000 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }
        }

        @Nested
        class SellValueBreakdown {
            @Test
            void breakdownExplainsTheAlternateValue() {
                String breakdown = unit.getSellValueBreakdown();

                assertTrue(breakdown.contains("Battle Value (current condition): 500"), breakdown);
                assertTrue(breakdown.contains("Sold at: x" + String.format("%.2f", 0.5)), breakdown);
                assertTrue(breakdown.contains(unit.getSellValue().toAmountString()), breakdown);
            }

            @Test
            void breakdownHasNoQualityOrObsoleteStepsUnderTheAlternateValue() {
                String breakdown = unit.getSellValueBreakdown();

                assertFalse(breakdown.contains("Quality ("), breakdown);
                assertFalse(breakdown.contains("Obsolete ("), breakdown);
            }
        }

        /** Maintenance is priced from the unit's value, so it follows the alternate value too. */
        @Test
        void weeklyMaintenanceFollowsTheAlternateValue() {
            campaignOptions.set(CampaignOption.USE_PERCENTAGE_MAINTENANCE, true);
            campaignOptions.set(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE, false);
            Money alternateMaintenance = unit.getWeeklyMaintenanceCost();

            campaignOptions.set(CampaignOption.USE_ALTERNATE_UNIT_COST, false);
            Money constructionMaintenance = unit.getWeeklyMaintenanceCost();

            // Meks cost 2% of their value a year, spread over 52 weeks
            assertEquals(Money.of(5_000_000).multipliedBy(0.02).dividedBy(52.0), alternateMaintenance);
            assertEquals(Money.of(3_000_000).multipliedBy(0.02).dividedBy(52.0), constructionMaintenance);
        }
    }

    /** Against a real Locust, to show the MegaMek BV calculation really does track damage. */
    @Nested
    class WithRealEntity {
        private Campaign campaign;
        private Unit unit;

        @BeforeEach
        void setUp() {
            campaign = MHQTestUtilities.getTestCampaign();
            campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATE_UNIT_COST, true);
            campaign.getCampaignOptions().set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
            campaign.getCampaignOptions().set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 1.0);
            unit = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        }

        @Test
        void anUndamagedUnitIsWorthItsBattleValue() {
            int battleValue = unit.getEntity().calculateBattleValue(true, true, true);

            assertTrue(battleValue > 0);
            assertEquals(Money.of((double) battleValue * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            assertEquals(unit.getBuyCost().multipliedBy(0.5), unit.getSellValue());
        }

        @Test
        void armorDamageLowersTheValue() {
            Money undamaged = unit.getBuyCost();

            Entity entity = unit.getEntity();
            for (int location = 0; location < entity.locations(); location++) {
                entity.setArmor(0, location);
            }

            assertTrue(unit.getBuyCost().isLessThan(undamaged), "stripping all armor must lower the value");
            assertTrue(unit.getSellValue().isLessThan(undamaged.multipliedBy(0.5)));
        }

        @Test
        void destroyedWeaponsLowerTheValue() {
            Money undamaged = unit.getBuyCost();

            for (Mounted<?> weapon : unit.getEntity().getWeaponList()) {
                weapon.setDestroyed(true);
            }

            assertTrue(unit.getBuyCost().isLessThan(undamaged), "destroying every weapon must lower the value");
        }

        @Test
        void optionOffRestoresTheConstructionCost() {
            Money alternate = unit.getBuyCost();

            campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATE_UNIT_COST, false);

            assertNotEquals(alternate, unit.getBuyCost());
            assertEquals(Money.of(unit.getEntity().getCost(false)), unit.getBuyCost());
        }

        /** Without the option, selling goes back to parts value, quality, and obsolescence. */
        @Test
        void optionOffRestoresTheNormalSellValue() {
            campaign.getCampaignOptions().set(CampaignOption.USE_ALTERNATE_UNIT_COST, false);

            assertNotEquals(unit.getBuyCost().multipliedBy(0.5), unit.getSellValue());
            assertTrue(unit.getSellValue().isPositive());
        }
    }
}
