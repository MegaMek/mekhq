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
import java.util.Set;
import java.util.UUID;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.ContractExpenseProjection;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitTestUtilities;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Verifies {@link CommandGenerator#processStartingCash}: the generated command is granted free and the campaign is
 * credited working capital equal to the configured number of months of the command's running costs, with the
 * pay-for debits pricing only units created after the pre-build hangar snapshot.
 */
class CommandGeneratorStartingCashTest {

    /** A known month of running costs, so the cash arithmetic does not depend on the test campaign's expenses. */
    private static final Money MONTHLY_EXPENSES = Money.of(1_000_000);

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        // The starting-loan path names a random financial institution on the Loan it creates.
        FinancialInstitutions.initializeFinancialInstitutions();
    }

    private static CommandGenerationOptions optionsWithMonths(int months) {
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setProcessFinances(true);
        options.setStartingCashMonths(months);
        options.setRandomizeStartingCash(false);
        options.setPayForSetup(false);
        // These cases assert the months arithmetic itself, so the float must not floor it.
        options.setMinimumStartingFloat(0);
        return options;
    }

    /** Pay for Initial Setup with only the unit-purchase debit, so the debit is exactly the new units' cost. */
    private static void payForUnitsOnly(CommandGenerationOptions options) {
        options.setPayForSetup(true);
        options.setPayForPersonnel(false);
        options.setPayForUnits(true);
        options.setPayForParts(false);
        options.setPayForArmour(false);
        options.setPayForAmmunition(false);
    }

    private static void processWithKnownExpenses(Campaign campaign, CommandGenerationOptions options,
          Set<UUID> preExisting, Money monthlyExpenses) {
        CommandGenerator.processStartingCash(campaign, options, preExisting, List.of(),
              CommandGenerator.SpareCosts.zero(), monthlyExpenses);
    }

    private static Money balance(Campaign campaign) {
        return campaign.getPlayerForce().getFinances().getBalance();
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

    /** A dice-rolled starting cash must not be reported as months of running costs (#10475). */
    @Test
    void randomizedStartingCash_reportsTheDiceRolledNotMonths() {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);

        CommandGenerationOptions options = optionsWithMonths(6);
        options.setRandomizeStartingCash(true);
        options.setRandomStartingCashDiceCount(18);
        processWithKnownExpenses(campaign, options, preExisting, MONTHLY_EXPENSES);

        String report = lastFinancesReport(campaign);
        assertTrue(report.contains("18d6 million C-bills"), "the report names the dice rolled: " + report);
        assertFalse(report.contains("months"), "the report does not mention the unused months: " + report);
    }

    @Test
    void creditsConfiguredMonthsOfRunningCosts() {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        processWithKnownExpenses(campaign, optionsWithMonths(6), preExisting, MONTHLY_EXPENSES);

        assertEquals(MONTHLY_EXPENSES.multipliedBy(6), balance(campaign).minus(balanceBefore),
              "starting cash should be six months of running costs");
        String report = lastFinancesReport(campaign);
        assertTrue(report.contains("(6 months of running costs at " + MONTHLY_EXPENSES.toAmountAndSymbolString()
                                         + " a month)"), "the report names the months and the monthly cost: " + report);
        assertFalse(report.contains("setup costs"), "nothing was deducted, so none is reported: " + report);
    }

    /** The public entry point measures the running costs with the off-contract expense projection. */
    @Test
    void projectsTheCampaignsRunningCosts() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money expected = ContractExpenseProjection.getMonthlyOffContractExpenses(campaign).round()
                               .multipliedBy(6).round();
        Money balanceBefore = balance(campaign);

        CommandGenerator.processStartingCash(campaign, optionsWithMonths(6), preExisting, List.of(),
              CommandGenerator.SpareCosts.zero());

        assertEquals(expected, balance(campaign).minus(balanceBefore),
              "starting cash should be six months of the projected running costs");
    }

    @Test
    void payForSetupWithCoveringBase_reportsTheDeductedCosts() {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit generated = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);

        // A thousand months of a million C-bills covers any single unit.
        CommandGenerationOptions options = optionsWithMonths(1000);
        payForUnitsOnly(options);
        processWithKnownExpenses(campaign, options, preExisting, MONTHLY_EXPENSES);

        String report = lastFinancesReport(campaign);
        assertTrue(report.contains("1000 months of running costs") || report.contains("1,000 months of running costs"),
              "the report names the base: " + report);
        assertTrue(report.contains("less " + generated.getBuyCost().toAmountAndSymbolString() + " in setup costs"),
              "the report names the setup costs deducted from the base: " + report);
    }

    @Test
    void payForSetupWithCoveringBase_deductsCostsFromCash() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit generated = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        CommandGenerationOptions options = optionsWithMonths(1000);
        payForUnitsOnly(options);
        processWithKnownExpenses(campaign, options, preExisting, MONTHLY_EXPENSES);

        Money expected = MONTHLY_EXPENSES.multipliedBy(1000).minus(generated.getBuyCost()).round();
        assertEquals(expected, balance(campaign).minus(balanceBefore),
              "setup costs are deducted from the covering base");
    }

    @Test
    void payForUnits_excludesUnitsPresentBeforeTheSnapshot() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);

        // Snapshot AFTER the first unit: only the unit added below may be paid for.
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit generated = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        CommandGenerationOptions options = optionsWithMonths(1000);
        payForUnitsOnly(options);
        processWithKnownExpenses(campaign, options, preExisting, MONTHLY_EXPENSES);

        Money expected = MONTHLY_EXPENSES.multipliedBy(1000).minus(generated.getBuyCost()).round();
        assertEquals(expected, balance(campaign).minus(balanceBefore),
              "pre-existing units must not be paid for again");
    }

    @Test
    void baseBelowMinimumFloat_reportsTheMinimumFloat() {
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        // Zero months is below the float, so the float is what the command starts with.
        CommandGenerationOptions options = optionsWithMonths(0);
        options.setMinimumStartingFloat(1_000_000);
        processWithKnownExpenses(campaign, options, preExisting, MONTHLY_EXPENSES);

        assertEquals(Money.of(1_000_000), balance(campaign).minus(balanceBefore),
              "cash floors at the minimum float");
        String report = lastFinancesReport(campaign);
        assertTrue(report.contains("minimum starting float"), "the report says the float was used: " + report);
        assertFalse(report.contains("months"), "the report does not claim months were granted: " + report);
    }

    @Test
    void payForUnits_debitsUnitCostAndTakesLoanForTheShortfall() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit generated = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);
        int loansBefore = campaign.getPlayerForce().getFinances().getLoans().size();

        // One month of a thousand C-bills cannot cover the unit: cash floors at the minimum float (zero) and the
        // shortfall becomes a starting loan.
        Money smallMonthlyExpenses = Money.of(1_000);
        CommandGenerationOptions options = optionsWithMonths(1);
        payForUnitsOnly(options);
        options.setStartingLoan(true);
        processWithKnownExpenses(campaign, options, preExisting, smallMonthlyExpenses);

        assertEquals(Money.zero(), balance(campaign).minus(balanceBefore),
              "cash floors at the minimum float when costs exceed the base");
        assertEquals(loansBefore + 1, campaign.getPlayerForce().getFinances().getLoans().size(),
              "the shortfall is taken as a starting loan");
        Money expectedLoan = generated.getBuyCost().minus(smallMonthlyExpenses).round();
        assertEquals(expectedLoan,
              campaign.getPlayerForce().getFinances().getLoans().get(loansBefore).getPrincipal(),
              "loan principal covers costs minus the pre-loan cash");
    }

    @Test
    void processFinancesOff_creditsNothing() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        CommandGenerationOptions options = optionsWithMonths(6);
        options.setProcessFinances(false);
        processWithKnownExpenses(campaign, options, preExisting, MONTHLY_EXPENSES);

        assertEquals(Money.zero(), balance(campaign).minus(balanceBefore),
              "no starting cash when Process Finances is off");
    }
}
