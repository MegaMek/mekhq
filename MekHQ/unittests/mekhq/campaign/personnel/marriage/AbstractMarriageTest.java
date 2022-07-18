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
package mekhq.campaign.personnel.marriage;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled // FIXME : Windchild : All Tests Missing
@ExtendWith(value = MockitoExtension.class)
public class AbstractMarriageTest {
    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @Mock
    private AbstractMarriage mockMarriage;

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testCanMarry() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testSafeSpouse() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testMarry() {

    }

    //region New Day
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testProcessNewDay() {

    }

    //region Random Marriage
    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testMarryRandomSpouse() {

    }

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testIsPotentialRandomSpouse() {

    }
    //endregion Random Marriage
    //endregion New Day
}
