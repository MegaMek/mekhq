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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.unit.Unit;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/**
 * Tests the Chaos pay scheme, {@link ChaosContractDeterminationPay}.
 *
 * <ul>
 *     <li>Base Pay: by default the force's covered monthly running costs (Hot Spots: Draconis Reach pg 26); with
 *     "Base Pay Only Considers Scale", 500 support points per Scale. Either way scaled by the base pay multiplier.</li>
 *     <li>Combat pay: 500 support points per Scale, optionally tapered above Scale 3.</li>
 *     <li>Salvage: optionally tapered above Scale 3, locked in when pay is set.</li>
 *     <li>The Hiring Hall Scale cap reduces cost-based Base Pay to the share of the force the job needs.</li>
 * </ul>
 */
class ChaosContractDeterminationPayTest {
    private static final double DELTA = 1e-9;
    private static final int SUPPORT_POINTS_TO_C_BILLS = 10_000;

    private final ChaosContractDeterminationPay payScheme = new ChaosContractDeterminationPay();

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Accountant accountant;
    private LocalHangar hangar;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        // Every running cost starts off, so each test turns on only what it checks
        campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);
        campaignOptions.set(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE, false);
        campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, false);
        campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, false);
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, false);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        accountant = mock(Accountant.class);
        when(accountant.getPayRoll()).thenReturn(Money.zero());
        when(accountant.getHotSpotsUpkeepCosts()).thenReturn(Money.zero());
        when(accountant.getOverheadExpenses()).thenReturn(Money.zero());
        when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(Money.zero());
        when(campaign.getAccountant()).thenReturn(accountant);

        hangar = mock(LocalHangar.class);
        List<Unit> noUnits = List.of();
        when(hangar.getUnits()).thenReturn(noUnits);
        when(campaign.getPlayerForce().getHangar()).thenReturn(hangar);
    }

    private static AbstractContract contract(int scale, double basePayMultiplier) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getScale()).thenReturn(scale);
        when(contract.getBasePayMultiplier()).thenReturn(basePayMultiplier);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);
        return contract;
    }

    private static Money supportPoints(int supportPoints) {
        return Money.of(supportPoints * SUPPORT_POINTS_TO_C_BILLS);
    }

    private void unitsWithWeeklyMaintenance(int... weeklyCosts) {
        Unit[] units = new Unit[weeklyCosts.length];
        for (int index = 0; index < weeklyCosts.length; index++) {
            units[index] = mock(Unit.class);
            when(units[index].getWeeklyMaintenanceCost()).thenReturn(Money.of(weeklyCosts[index]));
        }
        List<Unit> unitList = List.of(units);
        when(hangar.getUnits()).thenReturn(unitList);
    }

    @Test
    void newOptionsDefaultToCostBasedPayWithoutTaperOrCap() {
        CampaignOptions defaults = new CampaignOptions();
        assertEquals(false, defaults.get(CampaignOption.BASE_PAY_ONLY_CONSIDERS_SCALE));
        assertEquals(false, defaults.get(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE));
        assertEquals(false, defaults.get(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL));
    }

    /** "Base Pay Only Considers Scale": the pre-Hot Spots behaviour, kept unchanged. */
    @Nested
    class ScaleBasedBasePay {
        @BeforeEach
        void useScaleBasedPay() {
            campaignOptions.set(CampaignOption.BASE_PAY_ONLY_CONSIDERS_SCALE, true);
        }

        @Test
        void monthlyPayIsScaleTimesTheMultiplierInSupportPoints() {
            // 500 * scale(2) * basePay(1.0) = 1000 support points, unconverted.
            assertEquals(Money.of(1_000), payScheme.getMonthlyPay(campaign, contract(2, 1.0)));
        }

        @Test
        void monthlyPayAppliesTheNegotiatedBasePayMultiplier() {
            // 500 * 2 = 1000, scaled by 1.5 -> 1500 support points.
            assertEquals(Money.of(1_500), payScheme.getMonthlyPay(campaign, contract(2, 1.5)));
        }

        @Test
        void monthlyPayConvertsSupportPointsToCBillsWhenEnabled() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);

            assertEquals(Money.of(10_000_000), payScheme.getMonthlyPay(campaign, contract(2, 1.0)));
        }

        @Test
        void runningCostsAreIgnored() {
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
            when(accountant.getPayRoll()).thenReturn(Money.of(9_999_999));

            assertEquals(Money.of(1_000), payScheme.getMonthlyPay(campaign, contract(2, 1.0)));
            verify(accountant, never()).getPayRoll();
        }

        /** The cap already shrinks Scale, so Scale-based pay must not be cut a second time by the job share. */
        @Test
        void scaleCapDoesNotReduceScaleBasedPayTwice() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);

            try (MockedStatic<AbstractContractGeneration> generation = mockStatic(AbstractContractGeneration.class)) {
                generation.when(() -> AbstractContractGeneration.determineUncappedScale(any(), any(), any(), any()))
                      .thenReturn(8);

                assertEquals(Money.of(1_000), payScheme.getMonthlyPay(campaign, contract(2, 1.0)));
            }
        }
    }

    /** Default: Base Pay covers the force's running costs (Hot Spots: Draconis Reach pg 26). */
    @Nested
    class CostBasedBasePay {
        @BeforeEach
        void convertToCBills() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
        }

        @Test
        void noRunningCostsMeansNoBasePay() {
            assertEquals(Money.zero(), payScheme.getMonthlyPay(campaign, contract(5, 1.0)));
        }

        @Test
        void scaleDoesNotSetCostBasedPay() {
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
            when(accountant.getPayRoll()).thenReturn(Money.of(100_000));

            assertEquals(payScheme.getMonthlyPay(campaign, contract(1, 1.0)),
                  payScheme.getMonthlyPay(campaign, contract(9, 1.0)));
        }

        @Test
        void salariesAreCoveredWhenPaid() {
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
            when(accountant.getPayRoll()).thenReturn(Money.of(250_000));

            assertEquals(Money.of(250_000), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        @Test
        void salariesAreNotCoveredWhenNotPaid() {
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, false);
            when(accountant.getPayRoll()).thenReturn(Money.of(250_000));

            assertEquals(Money.zero(), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        @Test
        void maintenanceIsCoveredAsFourWeeksOfTheWholeHangar() {
            campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
            unitsWithWeeklyMaintenance(1_000, 2_500);

            assertEquals(Money.of(14_000), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        @Test
        void maintenanceIsNotCoveredWhenNotCharged() {
            campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, false);
            unitsWithWeeklyMaintenance(1_000, 2_500);

            assertEquals(Money.zero(), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        /** Upkeep replaces maintenance, so the employer never covers both. */
        @Test
        void maintenanceIsNotCoveredAlongsideUpkeep() {
            campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
            campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, true);
            unitsWithWeeklyMaintenance(1_000);
            when(accountant.getHotSpotsUpkeepCosts()).thenReturn(Money.of(5_000_000));

            assertEquals(Money.of(5_000_000), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        @Test
        void upkeepOverheadAndFoodAndHousingAreAllCovered() {
            when(accountant.getHotSpotsUpkeepCosts()).thenReturn(Money.of(5_000_000));
            when(accountant.getOverheadExpenses()).thenReturn(Money.of(30_000));
            when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(Money.of(12_000));

            assertEquals(Money.of(5_042_000), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        @Test
        void everyCoveredCostIsSummed() {
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
            campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
            when(accountant.getPayRoll()).thenReturn(Money.of(100_000));
            unitsWithWeeklyMaintenance(500);
            when(accountant.getOverheadExpenses()).thenReturn(Money.of(5_000));
            when(accountant.getMonthlyFoodAndHousingExpenses()).thenReturn(Money.of(3_000));

            // 100,000 + 4 x 500 + 5,000 + 3,000
            assertEquals(Money.of(110_000), ChaosContractDeterminationPay.getCoveredMonthlyCosts(campaign));
            assertEquals(Money.of(110_000), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        @ParameterizedTest
        @CsvSource({ "0.5, 50000", "1.0, 100000", "1.75, 175000", "0.0, 0" })
        void basePayMultiplierScalesTheCoveredCosts(double basePayMultiplier, int expected) {
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
            when(accountant.getPayRoll()).thenReturn(Money.of(100_000));

            assertEquals(Money.of(expected), payScheme.getMonthlyPay(campaign, contract(3, basePayMultiplier)));
        }
    }

    /** Without the support point conversion, cost-based pay is converted to support points, rounding up. */
    @Nested
    class CostBasedBasePayInSupportPoints {
        @BeforeEach
        void keepSupportPoints() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
        }

        @ParameterizedTest
        @CsvSource({ "0, 0", "1, 1", "9999, 1", "10000, 1", "10001, 2", "1234567, 124", "5000000, 500" })
        void costsAreDividedByTenThousandAndRoundedUp(int costs, int expectedSupportPoints) {
            when(accountant.getPayRoll()).thenReturn(Money.of(costs));

            assertEquals(Money.of(expectedSupportPoints), payScheme.getMonthlyPay(campaign, contract(3, 1.0)));
        }

        @Test
        void roundingHappensAfterTheBasePayMultiplier() {
            when(accountant.getPayRoll()).thenReturn(Money.of(10_000));

            // 10,000 x 1.5 = 15,000 C-bills = 1.5 SP -> 2, not 1 x 1.5 rounded
            assertEquals(Money.of(2), payScheme.getMonthlyPay(campaign, contract(3, 1.5)));
        }
    }

    /** With the Hiring Hall cap, the employer only covers the share of the force the job needs. */
    @Nested
    class JobShare {
        private MockedStatic<AbstractContractGeneration> generation;

        @BeforeEach
        void setUpCosts() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
            campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
            when(accountant.getPayRoll()).thenReturn(Money.of(1_000_000));
        }

        private void uncappedScaleOf(int uncappedScale, Runnable test) {
            try (MockedStatic<AbstractContractGeneration> mockedGeneration =
                       mockStatic(AbstractContractGeneration.class)) {
                generation = mockedGeneration;
                generation.when(() -> AbstractContractGeneration.determineUncappedScale(any(), any(), any(), any()))
                      .thenReturn(uncappedScale);
                test.run();
            }
        }

        @Test
        void noShareWithoutTheCapOption() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, false);

            uncappedScaleOf(8, () -> {
                assertEquals(1.0, ChaosContractDeterminationPay.getJobShare(campaign, contract(2, 1.0)), DELTA);
                generation.verifyNoInteractions();
            });
        }

        @Test
        void cappedContractCoversOnlyItsShare() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);

            uncappedScaleOf(8, () -> {
                assertEquals(0.25, ChaosContractDeterminationPay.getJobShare(campaign, contract(2, 1.0)), DELTA);
                assertEquals(Money.of(250_000), payScheme.getMonthlyPay(campaign, contract(2, 1.0)));
            });
        }

        @Test
        void uncappedContractCoversEverything() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);

            uncappedScaleOf(4, () ->
                  assertEquals(1.0, ChaosContractDeterminationPay.getJobShare(campaign, contract(4, 1.0)), DELTA));
        }

        /** A GM-edited Scale above the force's own must never pay more than the full costs. */
        @Test
        void shareNeverExceedsOne() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);

            uncappedScaleOf(3, () ->
                  assertEquals(1.0, ChaosContractDeterminationPay.getJobShare(campaign, contract(9, 1.0)), DELTA));
        }

        /** No committed combat force: nothing to divide by, so the costs are covered in full. */
        @Test
        void emptyForceIsCoveredInFull() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);

            uncappedScaleOf(0, () ->
                  assertEquals(1.0, ChaosContractDeterminationPay.getJobShare(campaign, contract(0, 1.0)), DELTA));
        }

        @Test
        void negativeScaleIsTreatedAsNoShare() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);

            uncappedScaleOf(5, () ->
                  assertEquals(0.0, ChaosContractDeterminationPay.getJobShare(campaign, contract(-1, 1.0)), DELTA));
        }

        @Test
        void shareAndBasePayMultiplierCombine() {
            campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);

            // 1,000,000 x (4 / 8) x 1.5
            uncappedScaleOf(8, () ->
                  assertEquals(Money.of(750_000), payScheme.getMonthlyPay(campaign, contract(4, 1.5))));
        }
    }

    @Nested
    class CombatPay {
        @Test
        void combatPayIsScaleInSupportPointsAndIgnoresTheBasePayMultiplier() {
            // 500 * scale(3) = 1500 support points; the base-pay multiplier does not affect combat pay.
            assertEquals(Money.of(1_500), payScheme.getCombatPay(campaign, contract(3, 2.0)));
        }

        @Test
        void combatPayConvertsSupportPointsToCBillsWhenEnabled() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);

            assertEquals(supportPoints(1_500), payScheme.getCombatPay(campaign, contract(3, 1.0)));
        }

        @Test
        void combatPayIsDividedByScaleWhenTrackIntensityIsMultipliedByScale() {
            // 500 * scale(3) / scale(3) = 500 support points
            campaignOptions.set(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE, true);

            assertEquals(Money.of(500), payScheme.getCombatPay(campaign, contract(3, 1.0)));
        }

        @Test
        void combatPayIsNotTaperedWithoutTheOption() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, false);

            assertEquals(Money.of(3_500), payScheme.getCombatPay(campaign, contract(7, 1.0)));
        }

        @ParameterizedTest
        @CsvSource({ "0, 0", "1, 500", "3, 1500", "4, 1750", "7, 2500", "10, 3250" })
        void taperedCombatPayIsFullToScaleThreeThenHalf(int scale, int expectedSupportPoints) {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);

            assertEquals(Money.of(expectedSupportPoints), payScheme.getCombatPay(campaign, contract(scale, 1.0)));
        }

        @Test
        void taperedCombatPayConvertsToCBills() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);

            assertEquals(supportPoints(2_500), payScheme.getCombatPay(campaign, contract(7, 1.0)));
        }

        @Test
        void taperAndTrackIntensityCombine() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);
            campaignOptions.set(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE, true);

            // Scale 5 tapers to 2000, divided across 5 x the scenarios; Scale 7 tapers to 2500, / 7 (integer division)
            assertEquals(Money.of(400), payScheme.getCombatPay(campaign, contract(5, 1.0)));
            assertEquals(Money.of(357), payScheme.getCombatPay(campaign, contract(7, 1.0)));
        }

        @Test
        void scaleZeroWithTrackIntensityDoesNotDivideByZero() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);
            campaignOptions.set(CampaignOption.MULTIPLY_TRACK_INTENSITY_BY_SCALE, true);

            assertEquals(Money.zero(), payScheme.getCombatPay(campaign, contract(0, 1.0)));
        }
    }

    @Nested
    class SalvageTaper {
        @Test
        void noSalvageTaperWithoutTheOption() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, false);

            assertEquals(1.0, payScheme.getSalvageTaperMultiplier(campaign, contract(10, 1.0)), DELTA);
        }

        @ParameterizedTest
        @CsvSource({ "1, 1.0", "3, 1.0", "4, 0.875", "7, 0.7142857142857143", "10, 0.65" })
        void salvageTapersLikeCombatPay(int scale, double expected) {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);

            assertEquals(expected, payScheme.getSalvageTaperMultiplier(campaign, contract(scale, 1.0)), DELTA);
        }

        /** Tapered salvage and tapered combat pay must fall by the same proportion. */
        @Test
        void salvageAndCombatPayTaperByTheSameFraction() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);
            AbstractContract contract = contract(9, 1.0);

            double combatPayFraction = payScheme.getCombatPay(campaign, contract).getAmount().doubleValue()
                                             / (ChaosContractDeterminationPay.DEFAULT_COMBAT_PAY_MULTIPLIER * 9);

            assertEquals(combatPayFraction, payScheme.getSalvageTaperMultiplier(campaign, contract), DELTA);
        }

        @Test
        void determiningPayLocksTheSalvageTaperIntoTheContract() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);
            AbstractContract contract = contract(7, 1.0);

            new FixedChaosPay().determineContractPay(campaign, LocalDate.of(3052, 6, 1), contract,
                  mock(AbstractLocation.class));

            ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
            verify(contract).setSalvageTaperMultiplier(captor.capture());
            assertEquals(5.0 / 7.0, captor.getValue(), DELTA);
        }

        @Test
        void determiningPayWithoutTheTaperLocksInFullSalvage() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, false);
            AbstractContract contract = contract(7, 1.0);

            new FixedChaosPay().determineContractPay(campaign, LocalDate.of(3052, 6, 1), contract,
                  mock(AbstractLocation.class));

            verify(contract).setSalvageTaperMultiplier(1.0);
        }

        /** Legacy (CamOps) contract pay never tapers salvage, whatever the option. */
        @Test
        void legacyPayNeverTapersSalvage() {
            campaignOptions.set(CampaignOption.TAPER_COMBAT_PAY_AND_SALVAGE_BY_SCALE, true);

            assertEquals(1.0,
                  new CamOpsContractDeterminationPay().getSalvageTaperMultiplier(campaign, contract(10, 1.0)), DELTA);
        }
    }

    /** Chaos pay with fixed pay components, so only the salvage taper wiring is under test. */
    private static class FixedChaosPay extends ChaosContractDeterminationPay {
        @Override
        public @NonNull Money getMonthlyPay(Campaign campaign, AbstractContract contract) {
            return Money.of(1);
        }

        @Override
        public @NonNull Money getCombatPay(Campaign campaign, AbstractContract contract) {
            return Money.of(1);
        }

        @Override
        public @NonNull Money getTransportPay(Campaign campaign, LocalDate currentDate, AbstractContract contract,
              AbstractLocation currentLocation) {
            return Money.of(1);
        }
    }
}
