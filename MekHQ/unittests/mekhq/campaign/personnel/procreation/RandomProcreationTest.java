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
package mekhq.campaign.personnel.procreation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.compute.Compute;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familyTree.Genealogy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(value = MockitoExtension.class)
public class RandomProcreationTest {
    @Mock
    private CampaignOptions mockOptions;

    @Mock
    private Person mockPerson;

    @BeforeEach
    public void beforeEach() {
        when(mockOptions.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_PRISONER_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE)).thenReturn(5);
        when(mockOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE)).thenReturn(5);
    }

    @Test
    public void testProcreation() {
        final RandomProcreation randomProcreation = new RandomProcreation(mockOptions);
        Genealogy mockGenealogy = mock(Genealogy.class);

        when(mockGenealogy.hasSpouse()).thenReturn(true);
        when(mockPerson.getGenealogy()).thenReturn(mockGenealogy);

        int diceSize = 5;

        try (MockedStatic<Compute> compute = Mockito.mockStatic(Compute.class)) {
            compute.when(() -> Compute.randomInt(diceSize)).thenReturn(0);
            assertTrue(randomProcreation.procreation(mockPerson));
            compute.when(() -> Compute.randomInt(diceSize)).thenReturn(1);
            assertFalse(randomProcreation.procreation(mockPerson));
        }
    }
}
