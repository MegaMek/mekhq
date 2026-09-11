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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import org.junit.jupiter.api.Test;

class CampaignSummaryTest {
    @Test
    void getTransportCapacityCountsHeavyVehiclesPlacedInSuperHeavyBays() {
        Campaign campaign = campaignWithEmptySummaryInputs();
        HangarStatistics hangarStatistics = mock(HangarStatistics.class);
        when(campaign.getHangarStatistics()).thenReturn(hangarStatistics);
        when(hangarStatistics.getNumberOfUnitsByType(Entity.ETYPE_TANK)).thenReturn(2);
        when(hangarStatistics.getTotalSuperHeavyVehicleBays()).thenReturn(3);

        CampaignSummary summary = new CampaignSummary();
        summary.setCampaign(campaign);

        assertEquals("100% bay capacity", summary.getTransportCapacity());
    }

    @Test
    void getCargoCapacityReportClampsNonFiniteCargoTonnage() {
        // A single part reporting a non-finite tonnage (e.g. infantry ammo, see MekHQ issue #9616) makes
        // getCargoTonnage() return Infinity. BigDecimal cannot parse "Infinity"/"NaN", so an unclamped
        // report throws a NumberFormatException that repeats on every Command Center refresh and bricks
        // the save. getCargoCapacityReport() must clamp the non-finite figure to 0 instead of throwing.
        Campaign campaign = campaignWithEmptySummaryInputs();
        when(campaign.getHangarStatistics()).thenReturn(mock(HangarStatistics.class));
        CargoStatistics cargoStatistics = campaign.getCargoStatistics();
        when(cargoStatistics.getCargoTonnage(false)).thenReturn(Double.POSITIVE_INFINITY);

        CampaignSummary summary = new CampaignSummary();
        summary.setCampaign(campaign);

        String report = assertDoesNotThrow(summary::getCargoCapacityReport);
        assertTrue(report.contains("0.0 tons"), "Expected non-finite cargo to be clamped to 0.0, got: " + report);
    }

    @Test
    void getCargoCapacityReportClampsNaNCargoCapacity() {
        // The capacity side is equally vulnerable: a NaN maximum cargo capacity must be clamped rather
        // than handed to BigDecimal.
        Campaign campaign = campaignWithEmptySummaryInputs();
        when(campaign.getHangarStatistics()).thenReturn(mock(HangarStatistics.class));
        CargoStatistics cargoStatistics = campaign.getCargoStatistics();
        when(cargoStatistics.getTotalCargoCapacity()).thenReturn(Double.NaN);

        CampaignSummary summary = new CampaignSummary();
        summary.setCampaign(campaign);

        String report = assertDoesNotThrow(summary::getCargoCapacityReport);
        assertTrue(report.contains("0.0 tons capacity)"),
              "Expected non-finite capacity to be clamped to 0.0, got: " + report);
    }

    private static Campaign campaignWithEmptySummaryInputs() {
        Campaign campaign = mockCampaign();
        LocalHangar hangar = mock(LocalHangar.class);
        CargoStatistics cargoStatistics = mock(CargoStatistics.class);

        when(campaign.getPlayerForce().getHumanResources().getActivePersonnel(false, false)).thenReturn(List.of());
        when(campaign.getPlayerForce().getHangar()).thenReturn(hangar);
        when(hangar.getUnits()).thenReturn(List.of());
        when(campaign.getActiveContracts()).thenReturn(List.of());
        when(campaign.getCargoStatistics()).thenReturn(cargoStatistics);

        return campaign;
    }
}
