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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.unit.Maintenance;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests {@link ContractExpenseProjection}: which contracts maintain at the combat pace, and how per-unit daily
 * maintenance is summed into a month of the contract or the journey out.
 */
class ContractExpenseProjectionTest {
    private static final double DELTA = 0.0001;

    private Campaign campaign;
    private Unit firstUnit;
    private Unit secondUnit;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        firstUnit = mock(Unit.class);
        secondUnit = mock(Unit.class);
        LocalHangar hangar = mock(LocalHangar.class);
        List<Unit> units = List.of(firstUnit, secondUnit);
        when(hangar.getUnits()).thenReturn(units);
        when(campaign.getPlayerForce().getHangar()).thenReturn(hangar);
    }

    private static AbstractContract contractWith(ContractObjectiveType objectiveType) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        return contract;
    }

    @Test
    void garrisonAndRetainerAreNotCombatContracts() {
        assertFalse(ContractExpenseProjection.isCombatContract(contractWith(ContractObjectiveType.GARRISON_DUTY)));
        assertFalse(ContractExpenseProjection.isCombatContract(contractWith(ContractObjectiveType.RETAINER)));
        assertFalse(ContractExpenseProjection.isCombatContract(contractWith(null)));
    }

    @Test
    void otherObjectivesAreCombatContracts() {
        assertTrue(ContractExpenseProjection.isCombatContract(contractWith(ContractObjectiveType.OBJECTIVE_RAID)));
    }

    @Test
    void contractMaintenanceIsAMonthOfEveryUnitsDailyCostAtTheContractsPace() {
        AbstractContract contract = contractWith(ContractObjectiveType.OBJECTIVE_RAID);

        try (MockedStatic<Maintenance> maintenance = mockStatic(Maintenance.class)) {
            maintenance.when(() -> Maintenance.getProjectedDailyMaintenanceCost(any(), eq(firstUnit), eq(true),
                  eq(true), eq(false))).thenReturn(Money.of(100));
            maintenance.when(() -> Maintenance.getProjectedDailyMaintenanceCost(any(), eq(secondUnit), eq(true),
                  eq(true), eq(false))).thenReturn(Money.of(50));

            double expected = 150 * ContractExpenseProjection.DAYS_PER_MONTH;
            assertEquals(expected,
                  ContractExpenseProjection.getMonthlyContractMaintenance(campaign, contract)
                        .getAmount().doubleValue(), DELTA);
        }
    }

    @Test
    void transitMaintenanceIsOffContractAndTravellingForEachDay() {
        try (MockedStatic<Maintenance> maintenance = mockStatic(Maintenance.class)) {
            maintenance.when(() -> Maintenance.getProjectedDailyMaintenanceCost(any(), any(), eq(false), eq(false),
                  eq(true))).thenReturn(Money.of(10));

            // 2 units x 10 a day x 12 days
            assertEquals(Money.of(240), ContractExpenseProjection.getTransitMaintenance(campaign, 12));
        }
    }

    @Test
    void noJourneyMeansNoTransitMaintenance() {
        try (MockedStatic<Maintenance> maintenance = mockStatic(Maintenance.class)) {
            maintenance.when(() -> Maintenance.getProjectedDailyMaintenanceCost(any(), any(), anyBoolean(),
                  anyBoolean(), anyBoolean())).thenReturn(Money.of(10));

            assertEquals(Money.zero(), ContractExpenseProjection.getTransitMaintenance(campaign, 0));
        }
    }
}
