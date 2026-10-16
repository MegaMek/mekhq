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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.parts.meks.MekGyro;
import org.junit.jupiter.api.Test;

/**
 * Spare parts repaired in the warehouse take the repair site penalty of the active contracts (issue #10335), so pulling
 * a component out of a unit no longer dodges the unit's Field Workshop or Improvised modifier.
 */
class WarehouseRepairSiteTest {

    private static Campaign campaignWithContracts(final ContractObjectiveType... objectiveTypes) {
        Campaign campaign = mockCampaign();
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());

        List<AbstractContract> contracts = new ArrayList<>();
        for (ContractObjectiveType objectiveType : objectiveTypes) {
            AbstractContract contract = mock(AbstractContract.class);
            when(contract.getObjectiveType()).thenReturn(objectiveType);
            contracts.add(contract);
        }
        when(campaign.getActiveContracts()).thenReturn(contracts);
        return campaign;
    }

    private static int spareGyroRepairTarget(final Campaign campaign) {
        MekGyro gyro = new MekGyro(50, Mek.GYRO_STANDARD, 5, false, campaign);
        gyro.setHits(1);
        return gyro.getAllMods(null).getValue();
    }

    @Test
    void withNoContractsTheWarehouseIsABasicFacility() {
        Campaign campaign = campaignWithContracts();
        MekGyro gyro = new MekGyro(50, Mek.GYRO_STANDARD, 5, false, campaign);
        gyro.setHits(1);

        assertTrue(gyro.getAllMods(null).getDesc().contains("Facility - Basic"));
    }

    @Test
    void raidContractsGiveWarehouseRepairsTheFieldWorkshopPenalty() {
        int basic = spareGyroRepairTarget(campaignWithContracts(ContractObjectiveType.GARRISON_DUTY));
        int raid = spareGyroRepairTarget(campaignWithContracts(ContractObjectiveType.OBJECTIVE_RAID));

        assertEquals(basic + 1, raid);
    }

    @Test
    void guerrillaContractsGiveWarehouseRepairsTheImprovisedPenalty() {
        int basic = spareGyroRepairTarget(campaignWithContracts());
        int guerrilla = spareGyroRepairTarget(campaignWithContracts(ContractObjectiveType.GUERRILLA_WARFARE));

        assertEquals(basic + 2, guerrilla);
    }

    @Test
    void theBestSiteAcrossActiveContractsIsUsed() {
        int raid = spareGyroRepairTarget(campaignWithContracts(ContractObjectiveType.OBJECTIVE_RAID));
        int mixed = spareGyroRepairTarget(campaignWithContracts(ContractObjectiveType.GUERRILLA_WARFARE,
              ContractObjectiveType.OBJECTIVE_RAID));

        assertEquals(raid, mixed);
    }
}
