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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.ArrayList;
import java.util.List;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.UnitCostModel;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Tests "Alternate Unit Cost": a unit is worth its base Battle Value (that of its undamaged design, without pilot, C3,
 * or TAG) in support points, scaled by the tech-base unit price multiplier and converted to C-bills when support point
 * conversion is on. Units sell for half that, adjusted for quality relative to quality D and reduced if obsolete.
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
        private static final String DESIGN_NAME = "Test Mek TST-1";
        /** The design's Battle Value; the mocked unit starts undamaged, so its own Battle Value matches. */
        private static final int BASE_BATTLE_VALUE = 500;
        /** A lower Battle Value, as the unit's own calculation would give once damaged. */
        private static final int DAMAGED_BATTLE_VALUE = 300;

        private Campaign campaign;
        private CampaignOptions campaignOptions;
        private Entity entity;
        private List<Mounted<?>> equipment;
        private Unit unit;
        private MockedStatic<MekSummaryCache> mockedSummaryCache;
        private MekSummaryCache summaryCache;
        private MekSummary designSummary;

        @BeforeEach
        void setUp() {
            designSummary = mock(MekSummary.class);
            when(designSummary.getBV()).thenReturn(BASE_BATTLE_VALUE);
            summaryCache = mock(MekSummaryCache.class);
            when(summaryCache.isInitialized()).thenReturn(true);
            when(summaryCache.getMek(DESIGN_NAME)).thenReturn(designSummary);
            mockedSummaryCache = mockStatic(MekSummaryCache.class);
            mockedSummaryCache.when(MekSummaryCache::getInstance).thenReturn(summaryCache);

            campaign = mockCampaign();
            campaignOptions = new CampaignOptions();
            campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.CHAOS_CAMPAIGN);
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
            when(entity.getShortNameRaw()).thenReturn(DESIGN_NAME);
            when(entity.calculateBattleValue(true, true, true)).thenReturn(BASE_BATTLE_VALUE);
            when(entity.getCost(false)).thenReturn(3_000_000.0);
            // Not obsolete unless a test says otherwise
            when(entity.getObsoleteResaleModifier(anyInt())).thenReturn(1.0);

            unit = new Unit(entity, campaign);
        }

        @AfterEach
        void tearDown() {
            mockedSummaryCache.close();
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
        void undamagedUnitSellsForHalfItsBuyCost() {
            assertEquals(Money.of(2_500_000), unit.getSellValue());
        }

        /**
         * Damage lowers neither value: a damaged unit costs and sells exactly as an undamaged one.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void damageLowersNeitherTheBuyCostNorTheSellValue() {
            when(entity.getTotalArmor()).thenReturn(40);
            when(entity.calculateBattleValue(true, true, true)).thenReturn(DAMAGED_BATTLE_VALUE);

            assertEquals(Money.of(BASE_BATTLE_VALUE * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            assertEquals(Money.of(BASE_BATTLE_VALUE * SUPPORT_POINTS_TO_C_BILLS * 0.5), unit.getSellValue());
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
            when(designSummary.getBV()).thenReturn(501);

            assertEquals(Money.of(250.5), unit.getSellValue());
        }

        @Test
        void zeroBattleValueIsWorthNothing() {
            when(designSummary.getBV()).thenReturn(0);

            assertEquals(Money.zero(), unit.getBuyCost());
            assertEquals(Money.zero(), unit.getSellValue());
        }

        /**
         * Buy and sell values come from the design's base Battle Value, never the unit's own calculation, which damage
         * lowers.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void valueUsesTheDesignBattleValue() {
            unit.getBuyCost();
            unit.getSellValue();

            verify(summaryCache).getMek(DESIGN_NAME);
            verify(entity, never()).calculateBattleValue(anyBoolean(), anyBoolean(), anyBoolean());
            verify(entity, never()).calculateBattleValue(anyBoolean(), anyBoolean());
        }

        /**
         * A design missing from the unit summaries falls back to the unit's own Battle Value, without pilot, C3, or
         * TAG.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void missingDesignFallsBackToTheCurrentBattleValue() {
            when(summaryCache.getMek(DESIGN_NAME)).thenReturn(null);
            when(entity.calculateBattleValue(true, true, true)).thenReturn(DAMAGED_BATTLE_VALUE);

            assertEquals(Money.of(DAMAGED_BATTLE_VALUE * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            verify(entity).calculateBattleValue(true, true, true);
        }

        /**
         * A fallback value is not kept, so the design's base Battle Value is used once its summary can be found.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void fallbackIsNotCachedOnceTheDesignIsFound() {
            when(summaryCache.getMek(DESIGN_NAME)).thenReturn(null);
            when(entity.calculateBattleValue(true, true, true)).thenReturn(DAMAGED_BATTLE_VALUE);
            unit.getBuyCost();

            when(summaryCache.getMek(DESIGN_NAME)).thenReturn(designSummary);

            assertEquals(Money.of(BASE_BATTLE_VALUE * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
        }

        /**
         * While the unit summaries are (re)loading, the unit is valued without waiting for them, so the caller (often
         * the UI) never freezes; the base Battle Value is used once loading finishes.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void loadingSummariesAreNotWaitedFor() {
            when(summaryCache.isInitialized()).thenReturn(false);
            when(entity.calculateBattleValue(true, true, true)).thenReturn(DAMAGED_BATTLE_VALUE);

            assertEquals(Money.of(DAMAGED_BATTLE_VALUE * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            verify(summaryCache, never()).getMek(anyString());

            when(summaryCache.isInitialized()).thenReturn(true);
            assertEquals(Money.of(BASE_BATTLE_VALUE * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
        }

        @Test
        void optionOffKeepsTheConstructionCost() {
            campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);

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
                campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);
                when(entity.isClan()).thenReturn(isClan);
                when(entity.isMixedTech()).thenReturn(isMixedTech);

                assertEquals(Money.of(expected), unit.getBuyCost());
            }

            /**
             * The Command Designer's starting-cash preview prices rolled entities before they become units, so it must
             * value them exactly as the units will be valued once built.
             *
             * @author Illiani
             * @since 0.51.01
             */
            @ParameterizedTest
            @CsvSource({ "false, false, 1.1", "true, false, 2.0", "false, true, 1.5", "true, true, 1.5" })
            void estimateBuyCostMatchesTheBattleValueBuyCost(boolean isClan, boolean isMixedTech,
                  double multiplier) {
                when(entity.isClan()).thenReturn(isClan);
                when(entity.isMixedTech()).thenReturn(isMixedTech);

                Money estimate = Unit.estimateBuyCost(entity, campaignOptions);
                assertEquals(Money.of(500 * multiplier * SUPPORT_POINTS_TO_C_BILLS), estimate);
                assertEquals(unit.getBuyCost(), estimate);
            }

            /**
             * @author Illiani
             * @since 0.51.01
             */
            @ParameterizedTest
            @CsvSource({ "false, false", "true, false", "false, true", "true, true" })
            void estimateBuyCostMatchesTheConstructionBuyCostWithTheOptionOff(boolean isClan, boolean isMixedTech) {
                campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);
                when(entity.isClan()).thenReturn(isClan);
                when(entity.isMixedTech()).thenReturn(isMixedTech);

                assertEquals(unit.getBuyCost(), Unit.estimateBuyCost(entity, campaignOptions));
                verify(entity, never()).calculateBattleValue(anyBoolean(), anyBoolean(), anyBoolean());
            }
        }

        /**
         * Chaos Campaign values large craft by Battle Value like any other unit.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void chaosCampaignValuesLargeCraftByBattleValue() {
            when(entity.isLargeCraft()).thenReturn(true);

            assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
        }

        /** A WarShip's BV times 10,000 is well past the range of an int. */
        @Test
        void largeVesselValuesDoNotOverflow() {
            when(designSummary.getBV()).thenReturn(1_500_000);

            assertEquals(Money.of(15_000_000_000.0), unit.getBuyCost());
            assertTrue(unit.getBuyCost().isPositive());
        }

        @Nested
        class BattleValueCache {
            @Test
            void unchangedDesignReusesItsBattleValue() {
                unit.getBuyCost();
                unit.getSellValue();
                unit.getBuyCost();

                verify(summaryCache, times(1)).getMek(DESIGN_NAME);
            }

            /**
             * Damage does not lower the value: lost armor, lost structure, and damaged equipment all leave the base
             * Battle Value unchanged.
             *
             * @author Illiani
             * @since 0.51.01
             */
            @Test
            void damageDoesNotLowerTheValue() {
                Mounted<?> destroyedWeapon = addEquipment();
                Mounted<?> hitWeapon = addEquipment();
                Mounted<?> missingWeapon = addEquipment();
                Money undamaged = unit.getBuyCost();

                when(entity.getTotalArmor()).thenReturn(0);
                when(entity.getTotalInternal()).thenReturn(10);
                when(destroyedWeapon.isDestroyed()).thenReturn(true);
                when(hitWeapon.isHit()).thenReturn(true);
                when(missingWeapon.isMissing()).thenReturn(true);

                assertEquals(undamaged, unit.getBuyCost());
                assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
                assertEquals(Money.of(250 * SUPPORT_POINTS_TO_C_BILLS), unit.getSellValue());
                verify(entity, never()).calculateBattleValue(anyBoolean(), anyBoolean(), anyBoolean());
            }

            /**
             * A refit changes the design, so its base Battle Value is looked up again.
             *
             * @author Illiani
             * @since 0.51.01
             */
            @Test
            void refitToANewDesignLooksUpItsBattleValue() {
                unit.getBuyCost();

                MekSummary refitSummary = mock(MekSummary.class);
                when(refitSummary.getBV()).thenReturn(650);
                when(summaryCache.getMek("Test Mek TST-1R")).thenReturn(refitSummary);
                when(entity.getShortNameRaw()).thenReturn("Test Mek TST-1R");

                assertEquals(Money.of(650 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            /** The multipliers and conversion are read live; only the Battle Value is cached. */
            @Test
            void optionChangesApplyImmediately() {
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

                assertTrue(breakdown.contains("Base Battle Value: 500"), breakdown);
                assertTrue(breakdown.contains("Sold at: x" + String.format("%.2f", 0.5)), breakdown);
                assertTrue(breakdown.contains(unit.getSellValue().toAmountString()), breakdown);
            }

            @Test
            void breakdownShowsTheObsoleteStepWhenTheUnitIsObsolete() {
                when(entity.getObsoleteResaleModifier(anyInt())).thenReturn(0.8);

                String breakdown = unit.getSellValueBreakdown();

                assertTrue(breakdown.contains("Obsolete ("), breakdown);
                assertTrue(breakdown.contains(Money.of(2_000_000).toAmountString()), breakdown);
            }

            @Test
            void breakdownShowsTheQualityStepButNoObsoleteStepForACurrentUnit() {
                String breakdown = unit.getSellValueBreakdown();

                assertTrue(breakdown.contains("Quality ("), breakdown);
                assertTrue(breakdown.contains("x" + String.format("%.2f", 1.0)), breakdown);
                assertFalse(breakdown.contains("Obsolete ("), breakdown);
            }
        }

        /** Quirks that change a unit's traditional price change its alternate value in the same way. */
        @Nested
        class PriceQuirks {
            @Test
            void goodReputationOneAddsTenPercent() {
                when(entity.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_1)).thenReturn(true);

                assertEquals(Money.of(500 * 1.1 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
                assertEquals(Money.of(500 * 1.1 * SUPPORT_POINTS_TO_C_BILLS).multipliedBy(0.5), unit.getSellValue());
            }

            @Test
            void goodReputationTwoAddsAQuarter() {
                when(entity.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_2)).thenReturn(true);

                assertEquals(Money.of(500 * 1.25 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            /** Matches MegaMek, where Good Reputation 1 is checked first. */
            @Test
            void bothGoodReputationsUseTheFirst() {
                when(entity.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_1)).thenReturn(true);
                when(entity.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_2)).thenReturn(true);

                assertEquals(Money.of(500 * 1.1 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void goodReputationStacksWithTheTechBaseMultiplier() {
                campaignOptions.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 2.0);
                when(entity.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_2)).thenReturn(true);

                assertEquals(Money.of(500 * 2.0 * 1.25 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            /** Only 'Meks' construction cost includes Good Reputation, so other units ignore it. */
            @Test
            void goodReputationDoesNotAffectNonMeks() {
                Tank tank = mock(Tank.class);
                when(tank.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_1)).thenReturn(true);
                when(tank.hasQuirk(OptionsConstants.QUIRK_POS_GOOD_REP_2)).thenReturn(true);

                assertEquals(1.0, Unit.getQuirkPriceMultiplier(tank));
            }

            @Test
            void noEntityHasNoQuirkMultiplier() {
                assertEquals(1.0, Unit.getQuirkPriceMultiplier(null));
            }

            @Test
            void obsoleteUnitsSellForLess() {
                when(entity.getObsoleteResaleModifier(anyInt())).thenReturn(0.8);

                // Half of 5,000,000, then x0.8
                assertEquals(Money.of(2_000_000), unit.getSellValue());
            }

            /** Obsolescence only affects resale, as it does for traditionally priced units. */
            @Test
            void obsoleteUnitsCostTheSameToBuy() {
                when(entity.getObsoleteResaleModifier(anyInt())).thenReturn(0.8);

                assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            }

            @Test
            void unaffectedByTheQuirksWithoutThem() {
                assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
                assertEquals(Money.of(2_500_000), unit.getSellValue());
            }
        }

        /**
         * Battle Value ignores quality, so sale prices apply the Used Part Price Multipliers relative to quality D:
         * a quality D unit sells for exactly half.
         */
        @Nested
        class Quality {
            private Unit unitOfQuality(PartQuality quality) {
                Unit qualityUnit = spy(unit);
                doReturn(quality).when(qualityUnit).getQuality();
                return qualityUnit;
            }

            /** Default multipliers are 0.1 / 0.2 / 0.3 / 0.5 / 0.7 / 0.9 for A to F. */
            @ParameterizedTest
            @CsvSource({ "QUALITY_A, 500000", "QUALITY_B, 1000000", "QUALITY_C, 1500000", "QUALITY_D, 2500000",
                         "QUALITY_E, 3500000", "QUALITY_F, 4500000" })
            void qualityScalesTheSaleFromHalfAtQualityD(PartQuality quality, int expected) {
                assertEquals(Money.of(expected), unitOfQuality(quality).getSellValue());
            }

            @Test
            void qualityDoesNotChangeTheBuyCost() {
                assertEquals(Money.of(500 * SUPPORT_POINTS_TO_C_BILLS), unitOfQuality(PartQuality.QUALITY_A).getBuyCost());
            }

            @Test
            void customMultipliersAreUsed() {
                campaignOptions.set(CampaignOption.USED_PART_PRICE_MULTIPLIERS,
                      new double[] { 0.25, 0.5, 0.75, 1.0, 1.5, 2.0 });

                // Half of 5,000,000, then 2.0 / 1.0
                assertEquals(Money.of(5_000_000), unitOfQuality(PartQuality.QUALITY_F).getSellValue());
            }

            /** A zero quality D multiplier can't be divided by, so quality is ignored rather than crashing. */
            @Test
            void zeroQualityDMultiplierIgnoresQuality() {
                campaignOptions.set(CampaignOption.USED_PART_PRICE_MULTIPLIERS,
                      new double[] { 0.1, 0.2, 0.3, 0.0, 0.7, 0.9 });

                assertEquals(Money.of(2_500_000), unitOfQuality(PartQuality.QUALITY_A).getSellValue());
            }

            @Test
            void qualityAndObsolescenceStack() {
                when(entity.getObsoleteResaleModifier(anyInt())).thenReturn(0.5);

                // Half of 5,000,000, x(0.9 / 0.5) for quality F, then x0.5 for obsolescence
                assertEquals(Money.of(2_250_000), unitOfQuality(PartQuality.QUALITY_F).getSellValue());
            }

            @Test
            void breakdownShowsTheQualityMultiplier() {
                String breakdown = unitOfQuality(PartQuality.QUALITY_B).getSellValueBreakdown();

                assertTrue(breakdown.contains("x" + String.format("%.2f", 0.4)), breakdown);
                assertTrue(breakdown.contains(Money.of(1_000_000).toAmountString()), breakdown);
            }
        }

        /** Maintenance is priced from the unit's value, so it follows the alternate value too. */
        @Test
        void weeklyMaintenanceFollowsTheAlternateValue() {
            campaignOptions.set(CampaignOption.USE_PERCENTAGE_MAINTENANCE, true);
            campaignOptions.set(CampaignOption.EQUIPMENT_CONTRACT_SALE_VALUE, false);
            Money alternateMaintenance = unit.getWeeklyMaintenanceCost();

            campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);
            Money constructionMaintenance = unit.getWeeklyMaintenanceCost();

            // Meks cost 2% of their value a year, spread over 52 weeks
            assertEquals(Money.of(5_000_000).multipliedBy(0.02).dividedBy(52.0), alternateMaintenance);
            assertEquals(Money.of(3_000_000).multipliedBy(0.02).dividedBy(52.0), constructionMaintenance);
        }
    }

    /**
     * Pricing from a unit summary alone, as the Purchase Unit dialog's price column does for every row.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Nested
    class FromSummary {
        private CampaignOptions campaignOptions;
        private MekSummary summary;

        @BeforeEach
        void setUp() {
            campaignOptions = new CampaignOptions();
            campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.CHAOS_CAMPAIGN);
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
            campaignOptions.set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 1.1);
            campaignOptions.set(CampaignOption.CLAN_UNIT_PRICE_MULTIPLIER, 2.0);
            campaignOptions.set(CampaignOption.MIXED_TECH_UNIT_PRICE_MULTIPLIER, 1.5);

            summary = mock(MekSummary.class);
            when(summary.getUnitType()).thenReturn(UnitType.getTypeName(UnitType.MEK));
            when(summary.getBV()).thenReturn(500);
            when(summary.getCost()).thenReturn(3_000_000L);
            when(summary.getAlternateCost()).thenReturn(400_000L);
        }

        /**
         * @author Illiani
         * @since 0.51.01
         */
        @ParameterizedTest
        @CsvSource({ "false, false, 1.1", "true, false, 2.0", "false, true, 1.5", "true, true, 1.5" })
        void alternateUnitCostPricesTheSummaryBattleValue(boolean isClan, boolean isMixedTech, double multiplier) {
            when(summary.isClan()).thenReturn(isClan);
            when(summary.isMixedTech()).thenReturn(isMixedTech);

            assertEquals(Money.of(500 * multiplier * SUPPORT_POINTS_TO_C_BILLS),
                  Unit.estimateBuyCost(summary, campaignOptions));
        }

        /**
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void withoutConversionTheSummaryIsPricedInRawSupportPoints() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);

            assertEquals(Money.of(500 * 1.1), Unit.estimateBuyCost(summary, campaignOptions));
        }

        /**
         * @author Illiani
         * @since 0.51.01
         */
        @ParameterizedTest
        @CsvSource({ "false, false, 3300000", "true, false, 6000000", "false, true, 4500000", "true, true, 4500000" })
        void optionOffPricesTheConstructionCost(boolean isClan, boolean isMixedTech, int expected) {
            campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);
            when(summary.isClan()).thenReturn(isClan);
            when(summary.isMixedTech()).thenReturn(isMixedTech);

            assertEquals(Money.of(expected), Unit.estimateBuyCost(summary, campaignOptions));
        }

        /**
         * @author Illiani
         * @since 0.51.01
         */
        @ParameterizedTest
        @ValueSource(ints = { UnitType.DROPSHIP, UnitType.JUMPSHIP, UnitType.WARSHIP, UnitType.SPACE_STATION })
        void chaosCampaignPricesLargeCraftSummariesByBattleValue(int unitType) {
            when(summary.getUnitType()).thenReturn(UnitType.getTypeName(unitType));

            assertEquals(Money.of(500 * 1.1 * SUPPORT_POINTS_TO_C_BILLS), Unit.estimateBuyCost(summary, campaignOptions));
        }

        /**
         * Infantry, battle armor included, is priced by its alternate cost, as a unit of it is.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @ParameterizedTest
        @ValueSource(ints = { UnitType.INFANTRY, UnitType.BATTLE_ARMOR })
        void optionOffPricesInfantryByItsAlternateCost(int unitType) {
            campaignOptions.set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);
            when(summary.getUnitType()).thenReturn(UnitType.getTypeName(unitType));

            assertEquals(Money.of(440_000), Unit.estimateBuyCost(summary, campaignOptions));
        }
    }

    /**
     * Against a real Locust, to show that damage which lowers MegaMek's Battle Value lowers neither Chaos Campaign
     * value, while a Total Warfare sell value still falls with damaged parts.
     */
    @Nested
    class WithRealEntity {
        private Campaign campaign;
        private Unit unit;
        private int baseBattleValue;
        private MockedStatic<MekSummaryCache> mockedSummaryCache;

        @BeforeEach
        void setUp() {
            campaign = MHQTestUtilities.getTestCampaign();
            campaign.getCampaignOptions().set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.CHAOS_CAMPAIGN);
            campaign.getCampaignOptions().set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
            campaign.getCampaignOptions().set(CampaignOption.INNER_SPHERE_UNIT_PRICE_MULTIPLIER, 1.0);

            // Tests cannot read the unit files, so the summary carries the BV of an undamaged Locust
            baseBattleValue = UnitTestUtilities.getLocustLCT1V().calculateBattleValue(true, true, true);
            MekSummary locustSummary = mock(MekSummary.class);
            when(locustSummary.getBV()).thenReturn(baseBattleValue);
            MekSummaryCache summaryCache = mock(MekSummaryCache.class);
            when(summaryCache.isInitialized()).thenReturn(true);
            when(summaryCache.getMek(anyString())).thenReturn(locustSummary);
            mockedSummaryCache = mockStatic(MekSummaryCache.class);
            mockedSummaryCache.when(MekSummaryCache::getInstance).thenReturn(summaryCache);

            unit = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        }

        @AfterEach
        void tearDown() {
            mockedSummaryCache.close();
        }

        @Test
        void anUndamagedUnitIsWorthItsBattleValue() {
            assertTrue(baseBattleValue > 0);
            assertEquals(baseBattleValue, unit.getEntity().calculateBattleValue(true, true, true));
            assertEquals(Money.of((double) baseBattleValue * SUPPORT_POINTS_TO_C_BILLS), unit.getBuyCost());
            assertEquals(unit.getBuyCost().multipliedBy(0.5), unit.getSellValue());
        }

        /**
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void armorDamageLowersNeitherValue() {
            Money buyCost = unit.getBuyCost();
            Money sellValue = unit.getSellValue();

            Entity entity = unit.getEntity();
            for (int location = 0; location < entity.locations(); location++) {
                entity.setArmor(0, location);
            }

            assertTrue(entity.calculateBattleValue(true, true, true) < baseBattleValue,
                  "stripping all armor must lower the unit's own Battle Value");
            assertEquals(buyCost, unit.getBuyCost());
            assertEquals(sellValue, unit.getSellValue());
        }

        /**
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void destroyedWeaponsLowerNeitherValue() {
            Money buyCost = unit.getBuyCost();
            Money sellValue = unit.getSellValue();

            for (Mounted<?> weapon : unit.getEntity().getWeaponList()) {
                weapon.setDestroyed(true);
            }

            assertTrue(unit.getEntity().calculateBattleValue(true, true, true) < baseBattleValue,
                  "destroying every weapon must lower the unit's own Battle Value");
            assertEquals(buyCost, unit.getBuyCost());
            assertEquals(sellValue, unit.getSellValue());
        }

        /**
         * Under Total Warfare, damaged parts lower the sell value but the buy cost stays the construction cost.
         *
         * @author Illiani
         * @since 0.51.01
         */
        @Test
        void totalWarfareDamageLowersOnlyTheSellValue() {
            campaign.getCampaignOptions().set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);
            Money constructionCost = Money.of(unit.getEntity().getCost(false));
            Money undamagedSellValue = unit.getSellValue();

            for (Part part : unit.getParts()) {
                if (part instanceof EquipmentPart equipmentPart) {
                    equipmentPart.setHits(1);
                }
            }

            assertEquals(constructionCost, unit.getBuyCost());
            assertTrue(unit.getSellValue().isLessThan(undamagedSellValue), "damaged parts must lower the sell value");
        }

        @Test
        void optionOffRestoresTheConstructionCost() {
            Money alternate = unit.getBuyCost();

            campaign.getCampaignOptions().set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);

            assertNotEquals(alternate, unit.getBuyCost());
            assertEquals(Money.of(unit.getEntity().getCost(false)), unit.getBuyCost());
        }

        /** Without the option, selling goes back to parts value, quality, and obsolescence. */
        @Test
        void optionOffRestoresTheNormalSellValue() {
            campaign.getCampaignOptions().set(CampaignOption.UNIT_COST_MODEL, UnitCostModel.TOTAL_WARFARE);

            assertNotEquals(unit.getBuyCost().multipliedBy(0.5), unit.getSellValue());
            assertTrue(unit.getSellValue().isPositive());
        }
    }
}
