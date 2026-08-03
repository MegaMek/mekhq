/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version, as published by
 * the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.gui.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AwardBonus;
import org.junit.jupiter.api.Test;

class AutoAwardsTableModelTest {
    @Test
    void combinesEdgeXpIntoSingleDescriptionWhenReplaceEdgeAwardsIsEnabled() {
        Campaign campaign = mockCampaign();
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseReplaceEdgeAwards()).thenReturn(true);
        when(options.getAwardBonusStyle()).thenReturn(AwardBonus.BOTH);
        when(campaign.getCampaignOptions()).thenReturn(options);

        UUID personId = UUID.randomUUID();
        Person person = mock(Person.class);
        when(campaign.getPerson(personId)).thenReturn(person);

        Award award = new Award("Test Award", "Test Set", "Award description", "Group", List.of(), List.of(),
              List.of(), 10, 1, false, 0, "", "", "", 1);

        Map<Integer, List<Object>> data = new HashMap<>();
        data.put(0, List.of(personId, award, Boolean.TRUE));

        AutoAwardsTableModel model = new AutoAwardsTableModel(campaign);
        model.setData(data);

        assertEquals("Award description (20 XP)", model.getValueAt(0, AutoAwardsTableModel.COL_DESCRIPTION));
    }
}
