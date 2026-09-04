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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.financialInstitutions.FinancialInstitutions;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.campaign.unit.UnitTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Verifies {@link CommandGenerator#processStartingCash}: the generated command is granted free and
 * the campaign is credited working capital equal to the configured percentage of the generated
 * units' total purchase cost, pricing only units created after the pre-build hangar snapshot.
 */
class CommandGeneratorStartingCashTest {

    @BeforeAll
    static void initializeTypes() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        // The starting-loan path names a random financial institution on the Loan it creates.
        FinancialInstitutions.initializeFinancialInstitutions();
    }

    private static CommandGenerationOptions optionsWithPercent(int percent) {
        CommandGenerationOptions options = new CommandGenerationOptions();
        options.setProcessFinances(true);
        options.setStartingCashPercent(percent);
        options.setRandomizeStartingCash(false);
        options.setPayForSetup(false);
        return options;
    }

    private static Money balance(Campaign campaign) {
        return campaign.getPlayerForce().getFinances().getBalance();
    }

    /** The units added since the snapshot, as the rolls' output: every unit in the hangar that is not in it. */
    private static Set<UUID> rolledSince(Campaign campaign, Set<UUID> preExisting) {
        Set<UUID> rolled = CommandGenerator.snapshotHangarUnitIds(campaign);
        rolled.removeAll(preExisting);
        return rolled;
    }

    @Test
    void unitsTheBuildAddsAfterTheRollsAreNotInThePercentageBase() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit rolled = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Set<UUID> rolledUnitIds = rolledSince(campaign, preExisting);
        // A ship the lift top-up or the cargo stage adds after the rolls.
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        CommandGenerator.processStartingCash(campaign, optionsWithPercent(10), preExisting, rolledUnitIds,
              List.of(), CommandGenerator.SpareCosts.zero());

        Money expected = rolled.getBuyCost().multipliedBy(10).dividedBy(100).round();
        assertEquals(expected, balance(campaign).minus(balanceBefore),
              "the build credits the percentage of the rolled units the tab previewed, nothing more");
    }

    @Test
    void creditsConfiguredPercentOfGeneratedUnitValue() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);

        Unit first = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Unit second = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money unitValue = first.getBuyCost().plus(second.getBuyCost());
        Money balanceBefore = campaign.getPlayerForce().getFinances().getBalance();

        CommandGenerator.processStartingCash(campaign, optionsWithPercent(10), preExisting, rolledSince(campaign, preExisting), List.of(),
              CommandGenerator.SpareCosts.zero());

        Money expected = unitValue.multipliedBy(10).dividedBy(100).round();
        assertEquals(expected,
              campaign.getPlayerForce().getFinances().getBalance().minus(balanceBefore),
              "starting cash should be 10% of the generated units' purchase cost");
    }

    @Test
    void excludesUnitsPresentBeforeTheSnapshot() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);

        // Snapshot AFTER the first unit: only the unit added below may be priced.
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit generated = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = campaign.getPlayerForce().getFinances().getBalance();

        CommandGenerator.processStartingCash(campaign, optionsWithPercent(10), preExisting, rolledSince(campaign, preExisting), List.of(),
              CommandGenerator.SpareCosts.zero());

        Money expected = generated.getBuyCost().multipliedBy(10).dividedBy(100).round();
        assertEquals(expected,
              campaign.getPlayerForce().getFinances().getBalance().minus(balanceBefore),
              "pre-existing units must not be priced into the starting cash");
    }

    @Test
    void payForUnits_debitsUnitCostAndTakesLoanForTheShortfall() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit generated = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);
        int loansBefore = campaign.getPlayerForce().getFinances().getLoans().size();

        // 10% base minus 100% unit cost cannot cover the debit: cash floors at the minimum float
        // (zero) and the shortfall becomes a starting loan.
        CommandGenerationOptions options = optionsWithPercent(10);
        options.setPayForSetup(true);
        options.setPayForPersonnel(false);
        options.setPayForUnits(true);
        options.setPayForParts(false);
        options.setPayForArmour(false);
        options.setPayForAmmunition(false);
        options.setStartingLoan(true);
        options.setMinimumStartingFloat(0);
        CommandGenerator.processStartingCash(campaign, options, preExisting, rolledSince(campaign, preExisting), List.of(),
              CommandGenerator.SpareCosts.zero());

        assertEquals(Money.zero(), balance(campaign).minus(balanceBefore),
              "cash floors at the minimum float when costs exceed the base");
        assertEquals(loansBefore + 1, campaign.getPlayerForce().getFinances().getLoans().size(),
              "the shortfall is taken as a starting loan");
        Money base = generated.getBuyCost().multipliedBy(10).dividedBy(100).round();
        Money expectedLoan = generated.getBuyCost().minus(base).round();
        assertEquals(expectedLoan,
              campaign.getPlayerForce().getFinances().getLoans().get(loansBefore).getPrincipal(),
              "loan principal covers costs minus the pre-loan cash");
    }

    @Test
    void payForSetupWithCoveringBase_deductsCostsFromCash() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        Unit generated = campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = balance(campaign);

        // 200% base covers the 100% unit debit, leaving 100% of unit value as cash.
        CommandGenerationOptions options = optionsWithPercent(200);
        options.setPayForSetup(true);
        options.setPayForPersonnel(false);
        options.setPayForUnits(true);
        options.setPayForParts(false);
        options.setPayForArmour(false);
        options.setPayForAmmunition(false);
        CommandGenerator.processStartingCash(campaign, options, preExisting, rolledSince(campaign, preExisting), List.of(),
              CommandGenerator.SpareCosts.zero());

        Money base = generated.getBuyCost().multipliedBy(200).dividedBy(100).round();
        Money expected = base.minus(generated.getBuyCost()).round();
        assertEquals(expected, balance(campaign).minus(balanceBefore),
              "setup costs are deducted from the covering base");
    }

    @Test
    void processFinancesOff_creditsNothing() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        Set<UUID> preExisting = CommandGenerator.snapshotHangarUnitIds(campaign);
        campaign.addNewUnit(UnitTestUtilities.getLocustLCT1V(), false, 0, PartQuality.QUALITY_D);
        Money balanceBefore = campaign.getPlayerForce().getFinances().getBalance();

        CommandGenerationOptions options = optionsWithPercent(10);
        options.setProcessFinances(false);
        CommandGenerator.processStartingCash(campaign, options, preExisting, rolledSince(campaign, preExisting), List.of(),
              CommandGenerator.SpareCosts.zero());

        assertEquals(Money.zero(),
              campaign.getPlayerForce().getFinances().getBalance().minus(balanceBefore),
              "no starting cash when Process Finances is off");
    }
}
