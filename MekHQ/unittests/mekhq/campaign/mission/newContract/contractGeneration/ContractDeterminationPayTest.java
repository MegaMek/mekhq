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
package mekhq.campaign.mission.newContract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import mekhq.campaign.JumpPath;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Accountant;
import mekhq.campaign.finances.Money;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.market.contractMarket.AlternatePaymentModelValues;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.mission.newContract.AbstractContractManager;
import mekhq.campaign.mission.newContract.AbstractContractObjective;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

class ContractDeterminationPayTest {
    private static final LocalDate DATE = LocalDate.of(3025, 1, 1);

    // ---- calculateTransportPeriod -----------------------------------------------------------

    @Test
    public void testCalculateTransportPeriod_SameSystem_IsZero() {
        PlanetarySystem system = mock(PlanetarySystem.class);
        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getFirstSystem()).thenReturn(system);
        when(jumpPath.getLastSystem()).thenReturn(system);

        assertEquals(0, ContractDeterminationPay.calculateTransportPeriod(jumpPath, false));
    }

    @Test
    public void testCalculateTransportPeriod_OneWay_NoJumps() {
        JumpPath jumpPath = differentSystemJumpPath(0);

        // round((2 + 1.1*0) * 1) = 2
        assertEquals(2, ContractDeterminationPay.calculateTransportPeriod(jumpPath, false));
    }

    @Test
    public void testCalculateTransportPeriod_OneWay_ThreeJumps() {
        JumpPath jumpPath = differentSystemJumpPath(3);

        // round((2 + 1.1*3) * 1) = round(5.3) = 5
        assertEquals(5, ContractDeterminationPay.calculateTransportPeriod(jumpPath, false));
    }

    @Test
    public void testCalculateTransportPeriod_TwoWay_ThreeJumps() {
        JumpPath jumpPath = differentSystemJumpPath(3);

        // round((2 + 1.1*3) * 2) = round(10.6) = 11
        assertEquals(11, ContractDeterminationPay.calculateTransportPeriod(jumpPath, true));
    }

    @Test
    public void testCalculateTransportPeriod_RoundsHalfUp() {
        JumpPath jumpPath = differentSystemJumpPath(5);

        // round((2 + 1.1*5) * 1) = round(7.5) = 8
        assertEquals(8, ContractDeterminationPay.calculateTransportPeriod(jumpPath, false));
    }

    private static JumpPath differentSystemJumpPath(int jumps) {
        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getFirstSystem()).thenReturn(mock(PlanetarySystem.class));
        when(jumpPath.getLastSystem()).thenReturn(mock(PlanetarySystem.class));
        when(jumpPath.getJumps()).thenReturn(jumps);
        return jumpPath;
    }

    // ---- calculateBasePay -------------------------------------------------------------------

    @Test
    public void testCalculateBasePay_NormalPaymentModel() {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseAlternatePaymentMode()).thenReturn(false);

        try (MockedStatic<Accountant> accountant = mockStatic(Accountant.class)) {
            accountant.when(() -> Accountant.getPeacetimeOperatingCosts(any(), any(), any(), anyBoolean(), any(),
                  anyInt(), anyInt(), any(), anyBoolean())).thenReturn(Money.of(1000));
            accountant.when(() -> Accountant.getForceValue(any(), any(), any(), any(), anyBoolean(), anyBoolean(),
                  anyDouble(), anyDouble(), anyDouble(), anyBoolean())).thenReturn(Money.of(500));

            ContractBasePayData data = ContractDeterminationPay.calculateBasePay(false, List.of(),
                  mock(LocalHangar.class), options, DATE, 0, 0, Map.of(), mock(Faction.class));

            // Peacetime is scaled by 0.75; base pay = (peacetime + combat) * NORMAL multiplier (1.0).
            assertEquals(Money.of(750), data.peacetimeOperatingCosts());
            assertEquals(Money.of(500), data.totalCostOfCombatUnits());
            assertEquals(Money.of(1250), data.calculatedBasePay());
        }
    }

    @Test
    public void testCalculateBasePay_AlternatePaymentModel() {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseAlternatePaymentMode()).thenReturn(true);

        try (MockedStatic<Accountant> accountant = mockStatic(Accountant.class);
              MockedStatic<AlternatePaymentModelValues> alternate = mockStatic(AlternatePaymentModelValues.class)) {
            accountant.when(() -> Accountant.getPeacetimeOperatingCosts(any(), any(), any(), anyBoolean(), any(),
                  anyInt(), anyInt(), any(), anyBoolean())).thenReturn(Money.of(1000));
            alternate.when(() -> AlternatePaymentModelValues.getForceValue(any(), any(), any(), anyBoolean(),
                  anyBoolean(), anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(Money.of(800));

            ContractBasePayData data = ContractDeterminationPay.calculateBasePay(false, List.of(),
                  mock(LocalHangar.class), options, DATE, 0, 0, Map.of(), mock(Faction.class));

            assertEquals(Money.of(750), data.peacetimeOperatingCosts());
            assertEquals(Money.of(800), data.totalCostOfCombatUnits());
            assertEquals(Money.of(1550), data.calculatedBasePay());
        }
    }

    // ---- determineStraightSupport -----------------------------------------------------------

    @Test
    public void testDetermineStraightSupport_AppliesMultiplier() {
        try (MockedStatic<Accountant> accountant = mockStatic(Accountant.class)) {
            accountant.when(() -> Accountant.getPeacetimeOperatingCosts(any(), any(), any(), anyBoolean(), any(),
                  anyInt(), anyInt(), any(), anyBoolean())).thenReturn(Money.of(2000));

            Money result = ContractDeterminationPay.determineStraightSupport(false, List.of(), mock(LocalHangar.class),
                  mock(CampaignOptions.class), DATE, 0, 0, Map.of(), 0.5);

            assertEquals(Money.of(1000), result);
        }
    }

    // ---- determineTransportPayment ----------------------------------------------------------

    @Test
    public void testDetermineTransportPayment_CeilsDurationAndDelegatesToCostCalculator() {
        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getTotalTime(any(), anyDouble(), anyBoolean())).thenReturn(5.2);
        when(jumpPath.getJumps()).thenReturn(4);

        ILocation location = mock(ILocation.class);
        when(location.getTransitTime()).thenReturn(1.0);

        try (MockedStatic<FactionStandingUtilities> standings = mockStatic(FactionStandingUtilities.class);
              MockedConstruction<TransportCostCalculations> construction = mockConstruction(
                    TransportCostCalculations.class,
                    (theMock, context) -> when(theMock.calculateJumpCostForEntireJourney(anyInt(), anyInt()))
                                                .thenReturn(Money.of(9999)))) {
            standings.when(() -> FactionStandingUtilities.isUseCommandCircuit(anyBoolean(), anyBoolean(),
                  any(FactionStandings.class), anyString())).thenReturn(true);

            Money result = ContractDeterminationPay.determineTransportPayment(false, false,
                  mock(FactionStandings.class), "EMPLOYER", jumpPath, DATE, location, List.of(), List.of(),
                  List.of(), 3);

            assertEquals(Money.of(9999), result);

            // Duration is ceil(5.2) = 6; jumps are passed straight through.
            TransportCostCalculations costCalculator = construction.constructed().get(0);
            verify(costCalculator).calculateJumpCostForEntireJourney(6, 4);
        }
    }

    // ---- generateContractPay (integration) --------------------------------------------------

    @Test
    public void testGenerateContractPay_AssemblesPayDataFromComponents() {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseAlternatePaymentMode()).thenReturn(false);
        when(options.isUseTwoWayPay()).thenReturn(false);

        Faction campaignFaction = mock(Faction.class);
        when(campaignFaction.isClan()).thenReturn(false);

        // Same-system jump path -> transport period 0 -> transit pay 0.
        PlanetarySystem system = mock(PlanetarySystem.class);
        JumpPath jumpPath = mock(JumpPath.class);
        when(jumpPath.getFirstSystem()).thenReturn(system);
        when(jumpPath.getLastSystem()).thenReturn(system);

        EmployerModifierData employerModifierData = new EmployerModifierData();

        AbstractContractManager contractManager = mock(AbstractContractManager.class);
        when(contractManager.getCachedJumpPathDirect()).thenReturn(jumpPath);
        when(contractManager.getEmployerModifierData()).thenReturn(employerModifierData);
        when(contractManager.getContractAllObjectivesCopy()).thenReturn(List.<AbstractContractObjective>of());

        try (MockedStatic<Accountant> accountant = mockStatic(Accountant.class)) {
            accountant.when(() -> Accountant.getPeacetimeOperatingCosts(any(), any(), any(), anyBoolean(), any(),
                  anyInt(), anyInt(), any(), anyBoolean())).thenReturn(Money.of(1000));
            accountant.when(() -> Accountant.getForceValue(any(), any(), any(), any(), anyBoolean(), anyBoolean(),
                  anyDouble(), anyDouble(), anyDouble(), anyBoolean())).thenReturn(Money.of(200));

            ContractPayData data = ContractDeterminationPay.generateContractPay(contractManager, List.of(),
                  mock(LocalHangar.class), options, DATE, 0, 0, Map.of(), campaignFaction, 1.0);

            // Base pay = (1000 * 0.75) + 200 = 950.
            assertEquals(Money.of(950), data.basePayData().calculatedBasePay());
            // Same-system journey -> zero transit pay.
            assertEquals(0, data.transitPayData().transportPeriod());
            assertEquals(Money.zero(), data.transitPayData().calculatedTransitPay());
            // No objectives -> empty map and zero total.
            assertTrue(data.objectivePayDataMap().isEmpty());
            assertEquals(Money.zero(), data.totalObjectivePay());
        }
    }
}
