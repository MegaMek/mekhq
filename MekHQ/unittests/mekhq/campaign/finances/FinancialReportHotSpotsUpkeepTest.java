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
package mekhq.campaign.finances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;
import java.util.stream.Stream;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.parts.Part;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the Hot Spots upkeep line of {@link FinancialReport}: it is reported and counted in monthly expenses, and the
 * costs it replaces are left out whatever their own options say.
 */
class FinancialReportHotSpotsUpkeepTest {
    private static final Money UPKEEP = Money.of(15_000_000);
    private static final Money WEEKLY_MAINTENANCE = Money.of(1_000);
    private static final Money SALARIES = Money.of(40_000);
    private static final Money OVERHEAD = Money.of(2_000);
    private static final Money SPARE_PARTS = Money.of(300);
    private static final Money AMMO = Money.of(200);
    private static final Money FUEL = Money.of(100);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private Accountant accountant;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
        campaignOptions.set(CampaignOption.PAY_FOR_SALARIES, true);
        campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, true);
        campaignOptions.set(CampaignOption.USE_PEACETIME_COST, true);
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, true);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);

        Finances finances = mock(Finances.class);
        when(finances.getBalance()).thenReturn(Money.zero());
        when(finances.getLoanBalance()).thenReturn(Money.zero());
        when(finances.getTotalAssetValue()).thenReturn(Money.zero());
        when(campaign.getPlayerForce().getFinances()).thenReturn(finances);
        when(campaign.getTotalRentFeesExcludingBays()).thenReturn(Money.zero());

        LocalHangar hangar = mock(LocalHangar.class);
        when(campaign.getPlayerForce().getHangar()).thenReturn(hangar);
        LocalWarehouse warehouse = mock(LocalWarehouse.class);
        when(warehouse.streamSpareParts()).thenAnswer(invocation -> Stream.<Part>empty());
        when(campaign.getPlayerForce().getWarehouse()).thenReturn(warehouse);
        List<AbstractContract> noContracts = List.of();
        when(campaign.getActiveContracts()).thenReturn(noContracts);

        accountant = mock(Accountant.class);
        when(accountant.getHotSpotsUpkeepCosts()).thenReturn(UPKEEP);
        when(accountant.getWeeklyMaintenanceCosts()).thenReturn(WEEKLY_MAINTENANCE);
        when(accountant.getPayRoll()).thenReturn(SALARIES);
        when(accountant.getOverheadExpenses()).thenReturn(OVERHEAD);
        when(accountant.getMonthlySpareParts()).thenReturn(SPARE_PARTS);
        when(accountant.getMonthlyAmmo()).thenReturn(AMMO);
        when(accountant.getMonthlyFuel()).thenReturn(FUEL);
        when(campaign.getAccountant()).thenReturn(accountant);
    }

    @Test
    void upkeepIsReported() {
        assertEquals(UPKEEP, FinancialReport.calculate(campaign).getHotSpotsUpkeepCosts());
    }

    @Test
    void upkeepReplacesMaintenanceOverheadAndPeacetimeCosts() {
        FinancialReport report = FinancialReport.calculate(campaign);

        assertEquals(Money.zero(), report.getMaintenance());
        assertEquals(Money.zero(), report.getOverheadCosts());
        assertEquals(Money.zero(), report.getMonthlySparePartCosts());
        assertEquals(Money.zero(), report.getMonthlyAmmoCosts());
        assertEquals(Money.zero(), report.getMonthlyFuelCosts());
    }

    /** Salaries are not replaced by upkeep. */
    @Test
    void salariesAreStillReportedAlongsideUpkeep() {
        assertEquals(SALARIES, FinancialReport.calculate(campaign).getSalaries());
    }

    @Test
    void monthlyExpensesAreUpkeepPlusSalaries() {
        assertEquals(UPKEEP.plus(SALARIES), FinancialReport.calculate(campaign).getMonthlyExpenses());
    }

    @Test
    void withoutUpkeepTheOriginalCostsAreReported() {
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);

        FinancialReport report = FinancialReport.calculate(campaign);

        assertEquals(Money.zero(), report.getHotSpotsUpkeepCosts());
        assertEquals(WEEKLY_MAINTENANCE.multipliedBy(4), report.getMaintenance());
        assertEquals(OVERHEAD, report.getOverheadCosts());
        assertEquals(SPARE_PARTS, report.getMonthlySparePartCosts());
        assertEquals(WEEKLY_MAINTENANCE.multipliedBy(4).plus(SALARIES).plus(OVERHEAD).plus(SPARE_PARTS).plus(AMMO)
                           .plus(FUEL), report.getMonthlyExpenses());
    }

    @Test
    void upkeepIsNotWorkedOutWhenItIsOff() {
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);

        FinancialReport.calculate(campaign);

        verify(accountant, never()).getHotSpotsUpkeepCosts();
    }
}
