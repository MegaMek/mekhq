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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.mission.contract.contractData.ContractCharacteristic.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumSet;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.ContractCharacteristic;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import mekhq.campaign.mission.contract.contractData.ContractScheduleData;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ContractCharacteristics} - the roll, the generation-time bakes, and the lifecycle-hook values (bonus
 * payments, reputation/standing multipliers, negotiator override).
 *
 * <p>Where a value is a straight function of a characteristic's tuning ({@code magnitude}), the expectation is derived
 * from {@link ContractCharacteristic#getMagnitude()} rather than a hard-coded number, so a retune does not break the
 * test. The behaviors that must hold regardless of tuning - one-per-category rolling, cross-category isolation, the
 * lower bounds on length and victory points, and success-only gating - are asserted directly.</p>
 */
class ContractCharacteristicsTest {
    private static final LocalDate START = LocalDate.of(3051, 1, 1);
    private static final int LENGTH_MONTHS = 6;
    private static final int BASE_VICTORY_POINTS = 5;
    private static final Money MONTHLY_PAY = Money.of(100);
    private static final Money TRANSPORT_PAY = Money.of(1000);
    private static final Money COMBAT_PAY = Money.of(50);

    /** A contract with known finance, schedule and objective values, carrying the supplied characteristics. */
    private static AbstractContract contractWith(ContractCharacteristic... characteristics) {
        AbstractContract contract = new ChaosContract();
        contract.setContractName("Test Contract");
        contract.setScheduleData(new ContractScheduleData(START, START.plusMonths(LENGTH_MONTHS), LENGTH_MONTHS));
        contract.setContractFinanceData(new ContractFinanceData(TRANSPORT_PAY, MONTHLY_PAY, COMBAT_PAY));
        contract.setRequiredVictoryPoints(BASE_VICTORY_POINTS);

        EnumSet<ContractCharacteristic> set = EnumSet.noneOf(ContractCharacteristic.class);
        set.addAll(Arrays.asList(characteristics));
        contract.setCharacteristics(set);
        return contract;
    }

    // region roll()

    @Test
    void rollNeverExceedsTwoAndNeverRepeatsACategory() {
        for (int iteration = 0; iteration < 1000; iteration++) {
            EnumSet<ContractCharacteristic> rolled = ContractCharacteristics.roll();

            assertTrue(rolled.size() <= 2, "rolled more than two characteristics: " + rolled);

            EnumSet<Category> categories = EnumSet.noneOf(Category.class);
            for (ContractCharacteristic characteristic : rolled) {
                assertTrue(categories.add(characteristic.getCategory()),
                      "two characteristics from the same category were rolled: " + rolled);
            }
        }
    }

    @Test
    void rollProducesBothEmptyAndNonEmptyResults() {
        boolean sawEmpty = false;
        boolean sawNonEmpty = false;
        for (int iteration = 0; iteration < 1000 && !(sawEmpty && sawNonEmpty); iteration++) {
            EnumSet<ContractCharacteristic> rolled = ContractCharacteristics.roll();
            sawEmpty |= rolled.isEmpty();
            sawNonEmpty |= !rolled.isEmpty();
        }
        assertTrue(sawEmpty, "never rolled an empty set in 1000 tries");
        assertTrue(sawNonEmpty, "never rolled a non-empty set in 1000 tries");
    }

    @Test
    void rollAndApplyDoesNothingWhenTheOptionIsOff() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.get(CampaignOption.USE_RANDOM_CONTRACT_CHARACTERISTICS)).thenReturn(false);

        AbstractContract contract = contractWith();
        ContractCharacteristics.rollAndApply(contract, campaign);

        assertTrue(contract.getCharacteristics().isEmpty(),
              "no characteristics should be rolled when the option is off");
    }

    // endregion roll()

    // region generation-time bakes

    @Test
    void bakeMonthlyPayScalesByThePayCharacteristic() {
        Money base = Money.of(1000);
        assertEquals(base.multipliedBy(HIGH_PAY.getMagnitude()),
              ContractCharacteristics.bakeMonthlyPay(base, contractWith(HIGH_PAY)));
        assertEquals(base.multipliedBy(LOW_PAY.getMagnitude()),
              ContractCharacteristics.bakeMonthlyPay(base, contractWith(LOW_PAY)));
    }

    @Test
    void bakeMonthlyPayLeavesPayUnchangedWithoutAPayCharacteristic() {
        Money base = Money.of(1000);
        assertEquals(base, ContractCharacteristics.bakeMonthlyPay(base, contractWith()));
        // HAZARD_PAY is a COMBAT_PAY characteristic and must not touch monthly pay.
        assertEquals(base, ContractCharacteristics.bakeMonthlyPay(base, contractWith(HAZARD_PAY)));
    }

    @Test
    void bakeCombatPayScalesByTheCombatPayCharacteristic() {
        Money base = Money.of(1000);
        assertEquals(base.multipliedBy(HAZARD_PAY.getMagnitude()),
              ContractCharacteristics.bakeCombatPay(base, contractWith(HAZARD_PAY)));
        assertEquals(base.multipliedBy(CUT_RATE.getMagnitude()),
              ContractCharacteristics.bakeCombatPay(base, contractWith(CUT_RATE)));
    }

    @Test
    void bakeCombatPayLeavesPayUnchangedWithoutACombatPayCharacteristic() {
        Money base = Money.of(1000);
        assertEquals(base, ContractCharacteristics.bakeCombatPay(base, contractWith()));
        // HIGH_PAY is a PAY characteristic and must not touch combat pay.
        assertEquals(base, ContractCharacteristics.bakeCombatPay(base, contractWith(HIGH_PAY)));
    }

    @Test
    void bakeRequiredVictoryPointsStepsByTheObjectivesCharacteristic() {
        int base = 5;
        assertEquals(Math.max(1, base + (int) Math.round(DEMANDING.getMagnitude())),
              ContractCharacteristics.bakeRequiredVictoryPoints(base, contractWith(DEMANDING)));
        assertEquals(Math.max(1, base + (int) Math.round(LENIENT.getMagnitude())),
              ContractCharacteristics.bakeRequiredVictoryPoints(base, contractWith(LENIENT)));
    }

    @Test
    void bakeRequiredVictoryPointsNeverDropsBelowOne() {
        // LENIENT lowers the count; from a base of 1 it must clamp at 1 rather than going to zero or negative.
        assertEquals(1, ContractCharacteristics.bakeRequiredVictoryPoints(1, contractWith(LENIENT)));
    }

    @Test
    void bakeRequiredVictoryPointsLeavesCountUnchangedWithoutAnObjectivesCharacteristic() {
        assertEquals(5, ContractCharacteristics.bakeRequiredVictoryPoints(5, contractWith()));
        assertEquals(5, ContractCharacteristics.bakeRequiredVictoryPoints(5, contractWith(HIGH_PAY)));
    }

    @Test
    void bakeLengthScalesByTheLengthCharacteristic() {
        int base = 6;
        assertEquals(Math.max(1, (int) Math.round(base * LENGTHY.getMagnitude())),
              ContractCharacteristics.bakeLength(base, contractWith(LENGTHY)));
        assertEquals(Math.max(1, (int) Math.round(base * QUICK.getMagnitude())),
              ContractCharacteristics.bakeLength(base, contractWith(QUICK)));
    }

    @Test
    void bakeLengthNeverDropsBelowOneMonth() {
        // QUICK shortens the contract; from a single month it must clamp at 1 rather than rounding to zero.
        assertEquals(1, ContractCharacteristics.bakeLength(1, contractWith(QUICK)));
    }

    @Test
    void bakeLengthLeavesLengthUnchangedWithoutALengthCharacteristic() {
        assertEquals(6, ContractCharacteristics.bakeLength(6, contractWith()));
        assertEquals(6, ContractCharacteristics.bakeLength(6, contractWith(HIGH_PAY)));
    }

    // endregion generation-time bakes

    // region reputation / standing multipliers

    @Test
    void unitReputationMultiplierScalesTheSuccessGainForAllFour() {
        assertEquals(HIGH_PROFILE.getMagnitude(),
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(HIGH_PROFILE), MissionStatus.SUCCESS));
        assertEquals(CAREER_MAKER.getMagnitude(),
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(CAREER_MAKER), MissionStatus.SUCCESS));
        assertEquals(MEDIA_BLACKOUT.getMagnitude(),
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(MEDIA_BLACKOUT), MissionStatus.SUCCESS));
        assertEquals(THANKLESS.getMagnitude(),
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(THANKLESS), MissionStatus.SUCCESS));
    }

    @Test
    void unitReputationMultiplierNeverScalesABreach() {
        for (ContractCharacteristic characteristic : new ContractCharacteristic[] { HIGH_PROFILE, CAREER_MAKER,
                                                                                    MEDIA_BLACKOUT, THANKLESS }) {
            assertEquals(1.0,
                  ContractCharacteristics.getUnitReputationMultiplier(contractWith(characteristic),
                        MissionStatus.BREACH),
                  "a breach must never be scaled: " + characteristic);
        }
    }

    @Test
    void onlyHighProfileAndMediaBlackoutScaleANonBreachLoss() {
        // The "gains and losses" pair scales a failed (non-breach) contract...
        assertEquals(HIGH_PROFILE.getMagnitude(),
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(HIGH_PROFILE), MissionStatus.FAILED));
        assertEquals(MEDIA_BLACKOUT.getMagnitude(),
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(MEDIA_BLACKOUT), MissionStatus.FAILED));
        // ...while the "gains only" pair leaves it alone.
        assertEquals(1.0,
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(CAREER_MAKER), MissionStatus.FAILED));
        assertEquals(1.0,
              ContractCharacteristics.getUnitReputationMultiplier(contractWith(THANKLESS), MissionStatus.FAILED));
    }

    @Test
    void unitReputationMultiplierIsNeutralWithoutAReputationCharacteristic() {
        for (MissionStatus status : new MissionStatus[] { MissionStatus.SUCCESS, MissionStatus.FAILED,
                                                          MissionStatus.BREACH }) {
            assertEquals(1.0, ContractCharacteristics.getUnitReputationMultiplier(contractWith(), status));
            assertEquals(1.0, ContractCharacteristics.getUnitReputationMultiplier(contractWith(HIGH_PAY), status));
        }
    }

    @Test
    void employerRegardMultiplierReadsTheEmployerStandingCharacteristic() {
        assertEquals(MONITORED.getMagnitude(),
              ContractCharacteristics.getEmployerRegardMultiplier(contractWith(MONITORED)));
        assertEquals(UNMONITORED.getMagnitude(),
              ContractCharacteristics.getEmployerRegardMultiplier(contractWith(UNMONITORED)));
        assertEquals(1.0, ContractCharacteristics.getEmployerRegardMultiplier(contractWith()));
    }

    @Test
    void enemyRegardMultiplierReadsTheEnemyStandingCharacteristic() {
        assertEquals(ITS_PERSONAL.getMagnitude(),
              ContractCharacteristics.getEnemyRegardMultiplier(contractWith(ITS_PERSONAL)));
        assertEquals(JUST_BUSINESS.getMagnitude(),
              ContractCharacteristics.getEnemyRegardMultiplier(contractWith(JUST_BUSINESS)));
        assertEquals(1.0, ContractCharacteristics.getEnemyRegardMultiplier(contractWith()));
    }

    @Test
    void negotiatorSkillOverrideMapsToTheNegotiatorCharacteristic() {
        assertEquals(SkillLevel.ELITE,
              ContractCharacteristics.getNegotiatorSkillOverride(contractWith(ELITE_NEGOTIATOR)));
        assertEquals(SkillLevel.REGULAR,
              ContractCharacteristics.getNegotiatorSkillOverride(contractWith(NOVICE_NEGOTIATOR)));
        assertNull(ContractCharacteristics.getNegotiatorSkillOverride(contractWith()));
    }

    // endregion reputation / standing multipliers

    // region bonus payments

    @Test
    void paySigningBonusCreditsTheShiftedFivePercent() {
        Finances finances = mock(Finances.class);
        Campaign campaign = campaignWithFinances(finances);

        AbstractContract contract = contractWith(SIGNING_BONUS);
        // Monthly pay was reduced to 95% at generation; the up-front lump is 1/19 of the reduced monthly total.
        Money expected = contract.getTotalMonthlyPay().dividedBy(19);

        ContractCharacteristics.paySigningBonus(campaign, contract);

        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(START), eq(expected), anyString());
    }

    @Test
    void paySigningBonusDoesNothingWithoutTheCharacteristic() {
        Campaign campaign = mock(Campaign.class);
        ContractCharacteristics.paySigningBonus(campaign, contractWith());
        verifyNoInteractions(campaign);
    }

    @Test
    void payCompletionBonusCreditsAFractionOfTotalPayOnSuccess() {
        Finances finances = mock(Finances.class);
        Campaign campaign = campaignWithFinances(finances);

        AbstractContract contract = contractWith(COMPLETION_BONUS);
        Money expected = contract.getTotalPay().multipliedBy(COMPLETION_BONUS.getMagnitude());

        ContractCharacteristics.payCompletionBonus(campaign, contract, MissionStatus.SUCCESS);

        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(START), eq(expected), anyString());
    }

    @Test
    void payCompletionBonusDoesNothingWhenTheContractWasNotASuccess() {
        Campaign campaign = mock(Campaign.class);
        ContractCharacteristics.payCompletionBonus(campaign, contractWith(COMPLETION_BONUS), MissionStatus.FAILED);
        verifyNoInteractions(campaign);
    }

    @Test
    void payCompletionBonusDoesNothingWithoutTheCharacteristic() {
        Campaign campaign = mock(Campaign.class);
        ContractCharacteristics.payCompletionBonus(campaign, contractWith(), MissionStatus.SUCCESS);
        verifyNoInteractions(campaign);
    }

    // endregion bonus payments

    private static Campaign campaignWithFinances(Finances finances) {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getFinances()).thenReturn(finances);
        when(campaign.getLocalDate()).thenReturn(START);
        return campaign;
    }
}
