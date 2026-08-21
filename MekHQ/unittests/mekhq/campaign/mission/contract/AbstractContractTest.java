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
package mekhq.campaign.mission.contract;

import static mekhq.campaign.mission.contract.AbstractContract.DEFAULT_SHARES_PERCENT;
import static mekhq.campaign.mission.contract.AbstractContract.INITIAL_SUPPORT_POINTS_PER_COMBAT_TEAM;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.ADVANCING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.OVERWHELMING;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.ROUTED;
import static mekhq.campaign.mission.contract.contractData.ContractMoraleLevel.STALEMATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveData;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractData.ContractScheduleData;
import mekhq.campaign.mission.contract.contractData.ContractTermsData;
import mekhq.campaign.mission.contract.contractData.MoraleData;
import mekhq.campaign.mission.scenarios.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Behavioral tests for {@link AbstractContract} - the state every contract type shares.
 *
 * <p>{@code AbstractContract} is deliberately a thin data class, so the coverage here concentrates on the handful of
 * places where it is more than a getter: the morale ladder and its clamping, the escrow arithmetic that reads through
 * to the schedule and finance records, the salvage predicates derived from the negotiated terms, and the support-point
 * reserve. {@link ChaosContract} stands in as the concrete type since it adds nothing of its own.</p>
 */
class AbstractContractTest {
    private static final LocalDate START = LocalDate.of(3051, 1, 1);

    private static AbstractContract contract() {
        AbstractContract contract = new ChaosContract();
        contract.setScheduleData(new ContractScheduleData(START, START.plusMonths(6), 6));
        return contract;
    }

    private static ContractTermsData termsWithSalvage(ChaosContractStepsTable salvageRights) {
        return new ContractTermsData(ChaosContractStepsTable.STEP_ONE,
              ChaosContractStepsTable.STEP_ONE,
              ChaosContractStepsTable.STEP_ONE,
              salvageRights,
              ChaosContractStepsTable.STEP_ONE);
    }

    // region morale

    @Test
    void freshlyConstructedContractStartsAtStalemate() {
        assertEquals(STALEMATE, new ChaosContract().getMoraleLevel(),
              "generation performs a morale check before assigning morale data, so the seed must be well-formed");
    }

    @Test
    void changeMoraleToALevelClearsTheRoutState() {
        AbstractContract contract = contract();
        contract.setMoraleData(new MoraleData(ROUTED, START.plusDays(10), Money.of(500)));

        contract.changeMorale(ADVANCING);

        assertEquals(ADVANCING, contract.getMoraleLevel());
        assertNull(contract.getRoutEndDate(), "a morale level change ends any rout in progress");
        assertTrue(contract.getRoutPayout().isZero(), "a morale level change clears the pending rout payout");
    }

    @Test
    void changeMoraleToARoutDateKeepsTheCurrentLevel() {
        AbstractContract contract = contract();
        contract.changeMorale(ADVANCING);

        contract.changeMorale(START.plusDays(10), Money.of(500));

        assertEquals(ADVANCING, contract.getMoraleLevel(), "setting a rout window must not move the morale level");
        assertEquals(START.plusDays(10), contract.getRoutEndDate());
        assertEquals(Money.of(500), contract.getRoutPayout());
    }

    @ParameterizedTest
    @CsvSource({
          "STALEMATE, 1, ADVANCING",
          "STALEMATE, -1, WEAKENED",
          "STALEMATE, 3, OVERWHELMING",
          "STALEMATE, -3, ROUTED"
    })
    void changeMoraleByDeltaWalksTheLadder(ContractMoraleLevel start, int delta, ContractMoraleLevel expected) {
        AbstractContract contract = contract();
        contract.changeMorale(start);

        assertEquals(expected, contract.changeMorale(delta), "the returned level must be the level applied");
        assertEquals(expected, contract.getMoraleLevel());
    }

    @ParameterizedTest
    @CsvSource({ "OVERWHELMING, 5, OVERWHELMING", "ROUTED, -5, ROUTED" })
    void changeMoraleByDeltaClampsAtTheEndsOfTheLadder(ContractMoraleLevel start, int delta,
          ContractMoraleLevel expected) {
        AbstractContract contract = contract();
        contract.changeMorale(start);

        assertEquals(expected, contract.changeMorale(delta),
              "a delta past the end of the ladder must clamp rather than fall off it");
    }

    @Test
    void garrisonContractWithRoutedEnemyIsPeaceful() {
        AbstractContract contract = contract();
        contract.setObjectiveData(new ContractObjectiveData(ContractObjectiveType.GARRISON_DUTY,
              ContractObjectiveType.PLANETARY_ASSAULT));
        contract.changeMorale(ROUTED);

        assertTrue(contract.isPeaceful(), "a garrison whose opposition has been routed has nothing left to fight");
    }

    @Test
    void garrisonContractWithIntactEnemyIsNotPeaceful() {
        AbstractContract contract = contract();
        contract.setObjectiveData(new ContractObjectiveData(ContractObjectiveType.GARRISON_DUTY,
              ContractObjectiveType.PLANETARY_ASSAULT));
        contract.changeMorale(OVERWHELMING);

        assertFalse(contract.isPeaceful());
    }

    @Test
    void nonGarrisonContractIsNeverPeacefulEvenAgainstARoutedEnemy() {
        AbstractContract contract = contract();
        contract.setObjectiveData(new ContractObjectiveData(ContractObjectiveType.PLANETARY_ASSAULT,
              ContractObjectiveType.GARRISON_DUTY));
        contract.changeMorale(ROUTED);

        assertFalse(contract.isPeaceful(), "an offensive contract still has objectives to take");
    }

    // endregion morale

    // region schedule and escrow

    @Test
    void setStartAndEndDateDerivesTheEndFromTheAgreedLength() {
        AbstractContract contract = contract();

        contract.setStartAndEndDate(LocalDate.of(3055, 3, 4));

        assertEquals(LocalDate.of(3055, 3, 4), contract.getStartDate());
        assertEquals(LocalDate.of(3055, 9, 4), contract.getEndingDate(),
              "the end date must be the start date plus the contract length in months");
        assertEquals(6, contract.getLengthInMonths(), "shifting the whole contract must not change its length");
    }

    @Test
    void monthsLeftCountsWholeMonthsToTheEndDate() {
        AbstractContract contract = contract();

        assertEquals(6, contract.getMonthsLeft(START));
        assertEquals(3, contract.getMonthsLeft(START.plusMonths(3)));
    }

    @Test
    void monthsLeftIsZeroOnceTheContractHasEnded() {
        AbstractContract contract = contract();

        assertEquals(0, contract.getMonthsLeft(START.plusMonths(9)),
              "a contract past its end date owes no further escrow");
    }

    @Test
    void monthsLeftIsZeroForAnUnsettledSchedule() {
        AbstractContract contract = new ChaosContract();
        contract.setScheduleData(new ContractScheduleData(null, null, 0));

        assertEquals(0, contract.getMonthsLeft(START), "a contract with no end date owes no escrow");
    }

    @Test
    void totalPayMultipliesMonthlyPayByLengthAndAddsTransport() {
        AbstractContract contract = contract();
        contract.setContractFinanceData(new ContractFinanceData(Money.of(1_000), Money.of(100), Money.of(50)));

        assertEquals(Money.of(600), contract.getTotalMonthlyPay(), "100 per month over the six-month term");
        assertEquals(Money.of(1_600), contract.getTotalPay(), "transport is paid on top of the monthly total");
        assertEquals(Money.of(100), contract.getMonthlyPayOut());
        assertEquals(Money.of(1_000), contract.getTransportPayment());
    }

    // endregion schedule and escrow

    // region salvage

    @Test
    void salvageIsAvailableWhenTheNegotiatedStepGrantsAShare() {
        AbstractContract contract = contract();
        contract.setContractTerms(termsWithSalvage(ChaosContractStepsTable.STEP_TWO));

        assertTrue(contract.canSalvage(), "STEP_TWO grants a positive salvage share");
        assertTrue(contract.isSalvageExchange(), "STEP_TWO grants that share as an exchange");
    }

    @Test
    void salvageIsUnavailableWhenTheNegotiatedStepGrantsNoShare() {
        AbstractContract contract = contract();
        contract.setContractTerms(termsWithSalvage(ChaosContractStepsTable.STEP_ONE));

        assertFalse(contract.canSalvage(), "STEP_ONE leaves the employer with all salvage");
        assertFalse(contract.isSalvageExchange());
    }

    @Test
    void salvageTalliesAccumulateSeparatelyForTheUnitAndTheEmployer() {
        AbstractContract contract = contract();

        contract.changeSalvagedByUnitValue(Money.of(300));
        contract.changeSalvagedByUnitValue(Money.of(200));
        contract.changeSalvagedByEmployerValue(Money.of(100));

        assertEquals(Money.of(500), contract.getSalvagedByUnitValue());
        assertEquals(Money.of(100), contract.getSalvagedByEmployerValue());
    }

    @Test
    void salvageTalliesStartAtZeroSoTheyCanBeAccumulatedIntoWithoutInitialization() {
        AbstractContract contract = new ChaosContract();

        assertTrue(contract.getSalvagedByUnitValue().isZero());
        assertTrue(contract.getSalvagedByEmployerValue().isZero());
    }

    // endregion salvage

    // region attacker/defender

    @ParameterizedTest
    @EnumSource(ContractObjectiveType.class)
    void playerIsAttackerFollowsTheObjectivesChaosType(ContractObjectiveType objectiveType) {
        AbstractContract contract = contract();
        contract.setObjectiveData(new ContractObjectiveData(objectiveType, ContractObjectiveType.GARRISON_DUTY));

        assertEquals(objectiveType.getChaosObjectiveType().isAttacker(), contract.isPlayerAttacker(),
              objectiveType + " must report the attacker stance of its underlying Chaos objective");
    }

    // endregion attacker/defender

    // region support points

    @Test
    void supportPointReserveScalesWithTheContractScale() {
        AbstractContract contract = contract();
        contract.setScale(4);

        assertEquals(4 * INITIAL_SUPPORT_POINTS_PER_COMBAT_TEAM, contract.getMaximumSupportPoints());
    }

    @Test
    void currentSupportPointsReadThroughToStratConState() {
        AbstractContract contract = contract();
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        when(campaignState.getSupportPoints()).thenReturn(7);
        contract.setStratConCampaignState(campaignState);

        assertEquals(7, contract.getCurrentSupportPoints());
    }

    @Test
    void currentSupportPointsAreZeroWithoutStratConState() {
        assertEquals(0, contract().getCurrentSupportPoints(),
              "a contract the player opted out of StratCon on has no support points, rather than throwing");
    }

    // endregion support points

    // region identity and defaults

    @Test
    void everyContractGetsItsOwnIdentity() {
        AbstractContract first = new ChaosContract();
        AbstractContract second = new ChaosContract();

        assertNull(first.getId(), "a contract's id is assigned on acceptance, not construction");
        assertNull(second.getId());
        assertEquals(DEFAULT_SHARES_PERCENT, first.getSharesPercent());
    }

    @Test
    void scenariosStartEmptyAndAccumulate() {
        AbstractContract contract = contract();
        Scenario scenario = mock(Scenario.class);

        assertTrue(contract.getScenarios().isEmpty());
        contract.addScenario(scenario);
        assertEquals(1, contract.getScenarios().size());
        assertSame(scenario, contract.getScenarios().get(0));

        contract.clearScenarios();
        assertTrue(contract.getScenarios().isEmpty());
    }

    // endregion identity and defaults
}
