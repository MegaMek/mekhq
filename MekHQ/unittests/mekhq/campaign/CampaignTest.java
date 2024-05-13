/*
 * Campaign.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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

import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Infantry;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Systems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Deric Page (dericdotpageatgmaildotcom)
 * @since 6/10/14 10:23 AM
 */
public class CampaignTest {
    @BeforeAll
    public static void setup() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @Test
    public void testGetTechs() {
        List<Person> testPersonList = new ArrayList<>(5);
        List<Person> testActivePersonList = new ArrayList<>(5);

        Person mockTechActive = mock(Person.class);
        when(mockTechActive.isTech()).thenReturn(true);
        when(mockTechActive.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockTechActive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActive).getStatus();
        when(mockTechActive.getMinutesLeft()).thenReturn(240);
        when(mockTechActive.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActive);
        testActivePersonList.add(mockTechActive);

        Person mockTechActiveTwo = mock(Person.class);
        when(mockTechActiveTwo.isTech()).thenReturn(true);
        when(mockTechActiveTwo.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockTechActiveTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActiveTwo).getStatus();
        when(mockTechActiveTwo.getMinutesLeft()).thenReturn(1);
        when(mockTechActiveTwo.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActiveTwo);
        testActivePersonList.add(mockTechActiveTwo);

        Person mockTechInactive = mock(Person.class);
        when(mockTechInactive.isTech()).thenReturn(true);
        when(mockTechInactive.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockTechInactive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.RETIRED).when(mockTechInactive).getStatus();
        when(mockTechInactive.getMinutesLeft()).thenReturn(240);
        when(mockTechInactive.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechInactive);

        Person mockTechNoTime = mock(Person.class);
        when(mockTechNoTime.isTech()).thenReturn(true);
        when(mockTechNoTime.getPrimaryRole()).thenReturn(PersonnelRole.MECH_TECH);
        when(mockTechNoTime.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechNoTime).getStatus();
        when(mockTechNoTime.getMinutesLeft()).thenReturn(0);
        when(mockTechNoTime.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechNoTime);
        testActivePersonList.add(mockTechNoTime);

        Person mockNonTechOne = mock(Person.class);
        when(mockNonTechOne.isTech()).thenReturn(false);
        when(mockNonTechOne.getPrimaryRole()).thenReturn(PersonnelRole.MECHWARRIOR);
        when(mockNonTechOne.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechOne).getStatus();
        when(mockNonTechOne.getMinutesLeft()).thenReturn(240);
        when(mockNonTechOne.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechOne);
        testActivePersonList.add(mockNonTechOne);

        Person mockNonTechTwo = mock(Person.class);
        when(mockNonTechTwo.isTech()).thenReturn(false);
        when(mockNonTechTwo.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);
        when(mockNonTechTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechTwo).getStatus();
        when(mockNonTechTwo.getMinutesLeft()).thenReturn(240);
        when(mockNonTechTwo.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechTwo);
        testActivePersonList.add(mockNonTechTwo);

        Campaign testCampaign = mock(Campaign.class);
        when(testCampaign.getPersonnel()).thenReturn(testPersonList);
        when(testCampaign.getActivePersonnel()).thenReturn(testActivePersonList);
        when(testCampaign.getTechs()).thenCallRealMethod();
        when(testCampaign.getTechs(anyBoolean())).thenCallRealMethod();
        when(testCampaign.getTechs(anyBoolean(), anyBoolean())).thenCallRealMethod();

        // Test just getting the list of active techs.
        List<Person> expected = new ArrayList<>(3);
        expected.add(mockTechActive);
        expected.add(mockTechActiveTwo);
        expected.add(mockTechNoTime);
        assertEquals(expected, testCampaign.getTechs());

        // Test getting active techs with time remaining.
        expected = new ArrayList<>(2);
        expected.add(mockTechActive);
        expected.add(mockTechActiveTwo);
        assertEquals(expected, testCampaign.getTechs(true));
    }

    @Test
    public void testCampaignResetInfantry() {
        // It is possible for Infantry to have BAP equal true, but empty Sensors vector.
        Campaign campaign = new Campaign();
        Entity infantry = spy(new Infantry());
        when(infantry.hasBAP()).thenReturn(true);
        campaign.clearGameData(infantry);
    }

    @Test
    public void testTransportShips() {
        Campaign campaign = spy(new Campaign());

        // New campaigns have no transports
        assertTrue(campaign.getTransportShips().isEmpty());

        // Create a mock transport
        Dropship mockTransport = mock(Dropship.class);
        UUID mockId = UUID.randomUUID();
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(mockId);
        when(mockUnit.getEntity()).thenReturn(mockTransport);

        // Add our mock transport
        campaign.addTransportShip(mockUnit);

        // Ensure our mock transport exists
        assertEquals(1, campaign.getTransportShips().size());
        assertTrue(campaign.getTransportShips().contains(mockUnit));

        // Add our mock transport a second time
        campaign.addTransportShip(mockUnit);

        // Ensure our mock transport exists only once
        assertEquals(1, campaign.getTransportShips().size());
        assertTrue(campaign.getTransportShips().contains(mockUnit));

        // Remove the mock transport
        campaign.removeTransportShip(mockUnit);

        // Ensure it was removed
        assertTrue(campaign.getTransportShips().isEmpty());
    }
}
