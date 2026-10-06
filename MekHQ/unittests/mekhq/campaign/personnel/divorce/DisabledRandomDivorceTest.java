/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.divorce;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(value = MockitoExtension.class)
public class DisabledRandomDivorceTest {
    @Mock
    private CampaignOptions mockOptions;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.get(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_PRISONER_DIVORCE)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_PRISONER_DIVORCE)).thenReturn(false);
    }

    @Test
    public void testRandomDivorce() {
        assertFalse(new DisabledRandomDivorce(mockOptions).randomDivorce());
    }
}
