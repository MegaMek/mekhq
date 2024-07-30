/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.divorce;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PrisonerStatus;
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

    //region Getters/Setters
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testGettersAndSetters() {
/*
        when(mockCampaignOptions.isUseClanPersonnelDivorce()).thenReturn(false);
        when(mockCampaignOptions.isUsePrisonerDivorce()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomSameSexDivorce()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomClanPersonnelDivorce()).thenReturn(false);
        when(mockCampaignOptions.isUseRandomPrisonerDivorce()).thenReturn(false);

        final AbstractDivorce disabledDivorce = new DisabledRandomDivorce(mockCampaignOptions);

        assertEquals(RandomDivorceMethod.NONE, disabledDivorce.getMethod());
        assertFalse(disabledDivorce.isUseClanPersonnelDivorce());
        assertFalse(disabledDivorce.isUsePrisonerDivorce());
        assertFalse(disabledDivorce.isUseRandomSameSexDivorce());
        assertFalse(disabledDivorce.isUseRandomClanPersonnelDivorce());
        assertFalse(disabledDivorce.isUseRandomPrisonerDivorce());
*/
    }
    //endregion Getters/Setters

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
