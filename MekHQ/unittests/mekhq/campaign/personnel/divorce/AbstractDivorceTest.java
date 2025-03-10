/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.personnel.divorce;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class AbstractDivorceTest {
    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @Mock
    private AbstractDivorce mockDivorce;

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testCanDivorce() {
        doCallRealMethod().when(mockDivorce).canDivorce(any(), anyBoolean());

        final Person mockPerson = mock(Person.class);

        // Can't be Clan Personnel with Random Clan Divorce Disabled
        when(mockDivorce.isUseRandomClanPersonnelDivorce()).thenReturn(false);
        when(mockDivorce.isUseRandomPrisonerDivorce()).thenReturn(true);
        assertNotNull(mockDivorce.canDivorce(mockPerson, true));

        // Can be Non-Clan Personnel with Random Clan Divorce Disabled
        when(mockPerson.isClanPersonnel()).thenReturn(false);
        assertNull(mockDivorce.canDivorce(mockPerson, true));

        // Can be a Non-Prisoner with Random Prisoner Divorce Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        when(mockDivorce.isUseRandomPrisonerDivorce()).thenReturn(false);
        assertNull(mockDivorce.canDivorce(mockPerson, true));

        // Can't be a Prisoner with Random Prisoner Divorce Disabled
        when(mockPerson.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        assertNotNull(mockDivorce.canDivorce(mockPerson, true));

        // Can be a Clan Prisoner with Random Clan and Random Prisoner Divorce Enabled
        lenient().when(mockPerson.isClanPersonnel()).thenReturn(true);
        when(mockDivorce.isUseRandomClanPersonnelDivorce()).thenReturn(true);
        when(mockDivorce.isUseRandomPrisonerDivorce()).thenReturn(true);
        assertNull(mockDivorce.canDivorce(mockPerson, true));
    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testWidowed() {

    }


    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testDivorce() {

    }

    //region New Day
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testProcessNewWeek() {

    }
    //endregion New Day
}
