/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.dialog.nagDialogs;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.FinancialReport;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is a test class for the {@link UnableToAffordExpensesNagDialog} class.
 * It contains tests for various scenarios related to the {@code isUnableToAffordExpenses} method
 */
class UnableToAffordExpensesNagDialogTest {
    // Mock objects for the tests
    // I know some of these can be converted to a local variable, but it makes sense to keep all the
    // mock objects in one place
    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private UnableToAffordExpensesNagDialog unableToAffordExpensesNagDialog;
    private Finances finances;
    private Unit unit;
    private Hangar hangar;
    private Warehouse warehouse;
    private FinancialReport report;

    /**
     * Test setup for each test, runs before each test.
     * Initializes the mock objects and sets up the necessary mock behaviors.
     */
    @BeforeEach
    void init() {
        // Initialize the mock objects
        campaign = mock(Campaign.class);
        campaignOptions = mock(CampaignOptions.class);

        unableToAffordExpensesNagDialog = new UnableToAffordExpensesNagDialog(campaign);

        finances = mock(Finances.class);

        unit = mock(Unit.class);
        hangar = mock(Hangar.class);
        hangar.addUnit(unit);

        warehouse = mock(Warehouse.class);

        report = mock(FinancialReport.class);

        // Stubs
        when(campaign.getFinances()).thenReturn(finances);
        when(campaign.getHangar()).thenReturn(hangar);
        when(campaign.getWarehouse()).thenReturn(warehouse);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
    }

    @Test
    void canAffordExpenses() {
        when(campaign.getFunds()).thenReturn(Money.of(2));
        when(report.getMonthlyExpenses()).thenReturn(Money.of(1));

        assertFalse(unableToAffordExpensesNagDialog.unableToAffordExpenses());
    }

    @Test
    void cannotAffordExpenses() {
        when(campaign.getFunds()).thenReturn(Money.of(1));
        when(report.getMonthlyExpenses()).thenReturn(Money.of(2));

        assertFalse(unableToAffordExpensesNagDialog.unableToAffordExpenses());
    }
}
