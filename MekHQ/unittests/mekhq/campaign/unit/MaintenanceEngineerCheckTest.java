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

package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;

/**
 * The daily maintenance schedule check must cope with a person flagged as an engineer who serves on no unit. A save
 * from issue #10538 had a crew member who retired years earlier still on a ship's crew list; once they were flagged as
 * its engineer, the check crashed and the day could not advance.
 */
class MaintenanceEngineerCheckTest {
    @Test
    void engineerWithNoUnitDoesNotStopTheDay() {
        Campaign campaign = mockCampaign();
        CampaignOptions campaignOptions = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3051, 8, 2));
        when(campaign.getActiveContracts()).thenReturn(List.of());

        Person retiredEngineer = mock(Person.class);
        when(retiredEngineer.isEngineer()).thenReturn(true);
        when(retiredEngineer.getUnit()).thenReturn(null);
        when(retiredEngineer.getDailyAvailableTechTime(anyBoolean())).thenReturn(480);
        when(campaign.getPlayerForce().getHumanResources().getTechsExpanded(any(), any(), anyBoolean(), any()))
              .thenReturn(List.of(retiredEngineer));

        assertDoesNotThrow(() -> Maintenance.checkAndCorrectMaintenanceSchedule(campaign));
    }
}
