/*
 * Campaign.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doReturn;

/**
 * @author Deric Page (dericdotpageatgmaildotcom)
 * @version %Id%
 * @since 6/10/14 10:23 AM
 */
public class CampaignTest {
    @Test
    public void testGetTechs() {
        final UUID testId = UUID.fromString("c8682a91-346f-49b0-9f1f-28e669ee4e95");

        List<Person> testPersonList = new ArrayList<>(5);
        List<Person> testActivePersonList = new ArrayList<>(5);

        Person mockTechActive = Mockito.mock(Person.class);
        Mockito.when(mockTechActive.isTech()).thenReturn(true);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActive).getStatus();
        Mockito.when(mockTechActive.getMinutesLeft()).thenReturn(240);
        testPersonList.add(mockTechActive);
        testActivePersonList.add(mockTechActive);

        Person mockTechActiveTwo = Mockito.mock(Person.class);
        Mockito.when(mockTechActiveTwo.isTech()).thenReturn(true);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActiveTwo).getStatus();
        Mockito.when(mockTechActiveTwo.getMinutesLeft()).thenReturn(1);
        testPersonList.add(mockTechActiveTwo);
        testActivePersonList.add(mockTechActiveTwo);

        Person mockTechInactive = Mockito.mock(Person.class);
        Mockito.when(mockTechInactive.isTech()).thenReturn(true);
        doReturn(PersonnelStatus.RETIRED).when(mockTechActive).getStatus();
        Mockito.when(mockTechInactive.getMinutesLeft()).thenReturn(240);
        testPersonList.add(mockTechInactive);

        Person mockTechNoTime = Mockito.mock(Person.class);
        Mockito.when(mockTechNoTime.isTech()).thenReturn(true);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechNoTime).getStatus();
        Mockito.when(mockTechNoTime.getMinutesLeft()).thenReturn(0);
        testPersonList.add(mockTechNoTime);
        testActivePersonList.add(mockTechNoTime);

        Person mockNonTechOne = Mockito.mock(Person.class);
        Mockito.when(mockNonTechOne.isTech()).thenReturn(false);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechOne).getStatus();
        Mockito.when(mockNonTechOne.getMinutesLeft()).thenReturn(240);
        testPersonList.add(mockNonTechOne);
        testActivePersonList.add(mockNonTechOne);

        Person mockNonTechTwo = Mockito.mock(Person.class);
        Mockito.when(mockNonTechTwo.isTech()).thenReturn(false);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechTwo).getStatus();
        Mockito.when(mockNonTechTwo.getMinutesLeft()).thenReturn(240);
        testPersonList.add(mockNonTechTwo);
        testActivePersonList.add(mockNonTechTwo);

        Campaign testCampaign = Mockito.mock(Campaign.class);
        Mockito.when(testCampaign.getPersonnel()).thenReturn(testPersonList);
        Mockito.when(testCampaign.getActivePersonnel()).thenReturn(testActivePersonList);
        Mockito.when(testCampaign.getTechs()).thenCallRealMethod();
        Mockito.when(testCampaign.getTechs(Mockito.anyBoolean())).thenCallRealMethod();
        Mockito.when(testCampaign.getTechs(Mockito.anyBoolean(), Mockito.nullable(UUID.class), Mockito.anyBoolean(), Mockito.anyBoolean())).thenCallRealMethod();
        Mockito.when(testCampaign.getPerson(Mockito.eq(testId))).thenReturn(mockTechActiveTwo);

        // Test just getting the list of active techs.
        List<Person> expected = new ArrayList<>(3);
        expected.add(mockTechActive);
        expected.add(mockTechActiveTwo);
        expected.add(mockTechNoTime);
        Assert.assertEquals(expected, testCampaign.getTechs());

        // Test getting active techs with time remaining.
        expected = new ArrayList<>(2);
        expected.add(mockTechActive);
        expected.add(mockTechActiveTwo);
        Assert.assertEquals(expected, testCampaign.getTechs(true));

        // Test getting the active techs with a specific tech listed first.
        expected = new ArrayList<>(3);
        expected.add(mockTechActiveTwo);
        expected.add(mockTechActive);
        expected.add(mockTechNoTime);
        Assert.assertEquals(expected, testCampaign.getTechs(false, testId, false, false));
    }
}
