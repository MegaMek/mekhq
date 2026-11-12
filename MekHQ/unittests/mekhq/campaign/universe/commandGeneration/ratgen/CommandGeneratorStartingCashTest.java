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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static mekhq.campaign.enums.DailyReportType.FINANCES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.ContractExpenseProjection;
import mekhq.campaign.finances.Loan;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Verifies {@link CommandGenerator#processStartingCash}: the generated command is granted free and the campaign is
 * credited working capital equal to the configured number of months of the command's running costs, never less than
 * the minimum starting cash, and optionally doubled by a starting loan for the same amount.
 */
class CommandGeneratorStartingCashTest {

    /** A known month of running costs, so the cash arithmetic does not depend on the test campaign's expenses. */
    private static final Money MONTHLY_EXPENSES = Money.of(1_000_000);

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        // The starting loan names a random financial institution on the Loan it creates.
        FinancialInstitutions.initializeFinancialInstitutions();
    }

    private static CommandGenerationOptions optionsWithMonths(int months) {
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setStartingCashMonths(months);
        return options;
    }

    private static Money balance(Campaign campaign) {
        return campaign.getPlayerForce().getFinances().getBalance();
    }

    private static List<Loan> loans(Campaign campaign) {
        return campaign.getPlayerForce().getFinances().getLoans();
    }

    /**
     * @param campaign a spied campaign that {@link CommandGenerator#processStartingCash} has run against
     *
     * @return the last report the starting-cash stage posted to the Finances log
     */
    private static String lastFinancesReport(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign, atLeastOnce()).addReport(eq(FINANCES), reportCaptor.capture());
        return reportCaptor.getValue();
    }

    @Test
    void minimumStartingCash_defaultsToTenMillion() {
        assertEquals(10_000_000L, new CommandGenerationOptions().getMinimumStartingCash());
    }

    @Test
    void creditsConfiguredMonthsOfRunningCosts() {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        // Twelve months of a million C-bills is above the minimum, so the months are granted as they are.
        CommandGenerator.processStartingCash(campaign, optionsWithMonths(12), MONTHLY_EXPENSES);

        assertEquals(MONTHLY_EXPENSES.multipliedBy(12), balance(campaign).minus(balanceBefore),
              "starting cash should be twelve months of running costs");
        String report = lastFinancesReport(campaign);
        assertTrue(report.contains("(12 months of running costs at " + MONTHLY_EXPENSES.toAmountAndSymbolString()
                                         + " a month)"), "the report names the months and the monthly cost: " + report);
    }

    /** The public entry point measures the running costs with the off-contract expense projection. */
    @Test
    void projectsTheCampaignsRunningCosts() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        CommandGenerationOptions options = optionsWithMonths(12);
        Money months = ContractExpenseProjection.getMonthlyOffContractExpenses(campaign).round()
                             .multipliedBy(12).round();
        Money minimum = Money.of(options.getMinimumStartingCash());
        Money expected = months.isGreaterOrEqualThan(minimum) ? months : minimum;
        Money balanceBefore = balance(campaign);

        CommandGenerator.processStartingCash(campaign, options);

        assertEquals(expected, balance(campaign).minus(balanceBefore),
              "starting cash should be twelve months of the projected running costs, floored at the minimum");
    }

    @Test
    void baseBelowMinimum_creditsAndReportsTheMinimum() {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());
        Money balanceBefore = balance(campaign);

        // Three months of a million C-bills is below the default minimum, so the minimum is what the command gets.
        CommandGenerator.processStartingCash(campaign, optionsWithMonths(3), MONTHLY_EXPENSES);

        assertEquals(Money.of(10_000_000), balance(campaign).minus(balanceBefore),
              "cash floors at the minimum starting cash");
        String report = lastFinancesReport(campaign);
        assertTrue(report.contains("minimum starting cash"), "the report says the minimum was used: " + report);
        assertFalse(report.contains("months"), "the report does not claim months were granted: " + report);
    }

    @Test
    void customMinimum_isTheFloor() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Money balanceBefore = balance(campaign);

        CommandGenerationOptions options = optionsWithMonths(12);
        options.setMinimumStartingCash(25_000_000);
        CommandGenerator.processStartingCash(campaign, options, MONTHLY_EXPENSES);

        assertEquals(Money.of(25_000_000), balance(campaign).minus(balanceBefore),
              "twelve million is below the chosen minimum, so the minimum is credited");
    }

    @Test
    void zeroMinimumAndNoRunningCosts_creditsNothing() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Money balanceBefore = balance(campaign);

        CommandGenerationOptions options = optionsWithMonths(12);
        options.setMinimumStartingCash(0);
        CommandGenerator.processStartingCash(campaign, options, Money.zero());

        assertEquals(Money.zero(), balance(campaign).minus(balanceBefore), "nothing to grant");
    }

    @Test
    void noStartingLoan_byDefault() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        int loansBefore = loans(campaign).size();

        CommandGenerator.processStartingCash(campaign, optionsWithMonths(12), MONTHLY_EXPENSES);

        assertEquals(loansBefore, loans(campaign).size(), "the command is granted free unless a loan is asked for");
    }

    @Test
    void startingLoan_doublesTheStartingCash_withTheExtraHalfBorrowed() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Money balanceBefore = balance(campaign);
        int loansBefore = loans(campaign).size();

        CommandGenerationOptions options = optionsWithMonths(12);
        options.setStartingLoan(true);
        CommandGenerator.processStartingCash(campaign, options, MONTHLY_EXPENSES);

        Money startingCash = MONTHLY_EXPENSES.multipliedBy(12);
        assertEquals(startingCash.multipliedBy(2), balance(campaign).minus(balanceBefore),
              "the starting cash is doubled");
        assertEquals(loansBefore + 1, loans(campaign).size(), "the extra half is a loan");
        assertEquals(startingCash, loans(campaign).get(loansBefore).getPrincipal(),
              "the loan is for the same amount as the starting cash");
    }

    @Test
    void startingLoan_doublesTheMinimumWhenTheMinimumApplies() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Money balanceBefore = balance(campaign);
        int loansBefore = loans(campaign).size();

        CommandGenerationOptions options = optionsWithMonths(3);
        options.setStartingLoan(true);
        CommandGenerator.processStartingCash(campaign, options, MONTHLY_EXPENSES);

        assertEquals(Money.of(20_000_000), balance(campaign).minus(balanceBefore),
              "the ten million minimum is doubled to twenty");
        assertEquals(Money.of(10_000_000), loans(campaign).get(loansBefore).getPrincipal(),
              "ten million of it is borrowed");
    }
}
