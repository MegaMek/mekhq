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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.MHQConstants;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A zero-minute maintenance job is helped by a full AsTech team when the campaign has the AsTechs, rather than being
 * checked as maximally shorthanded (issue #11047).
 */
class MaintenanceZeroMinuteAsTechTest {
    private ForceHumanResources humanResources;
    private CampaignOptions campaignOptions;

    @BeforeEach
    void setUp() {
        humanResources = mock(ForceHumanResources.class);
        campaignOptions = mock(CampaignOptions.class);
    }

    @Test
    void zeroMinuteJobGetsFullTeamWhenAsTechsAreAvailable() {
        when(humanResources.getNumberAsTechs(campaignOptions)).thenReturn(132);

        int asTechs = Maintenance.getAsTechsForMaintenance(humanResources, 0, false, campaignOptions);

        assertEquals(MHQConstants.AS_TECH_TEAM_SIZE, asTechs);
        verify(humanResources, never()).getAvailableAsTechs(anyInt(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void zeroMinuteJobGetsOnlyTheAsTechsTheCampaignHas() {
        when(humanResources.getNumberAsTechs(campaignOptions)).thenReturn(2);

        assertEquals(2, Maintenance.getAsTechsForMaintenance(humanResources, 0, false, campaignOptions));
    }

    @Test
    void zeroMinuteJobWithNoAsTechsGetsNoHelp() {
        when(humanResources.getNumberAsTechs(campaignOptions)).thenReturn(0);

        assertEquals(0, Maintenance.getAsTechsForMaintenance(humanResources, 0, false, campaignOptions));
    }

    @Test
    void timedJobDrawsHelpersFromThePool() {
        when(humanResources.getAvailableAsTechs(60, false, true, campaignOptions)).thenReturn(3);

        assertEquals(3, Maintenance.getAsTechsForMaintenance(humanResources, 60, true, campaignOptions));
    }
}
