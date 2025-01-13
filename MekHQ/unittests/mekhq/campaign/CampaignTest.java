/*
 * Copyright (c) 2009-2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.*;

import megamek.common.Bay;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import megamek.common.Dropship;
import megamek.common.EquipmentType;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Systems;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
    void testGetTechs() {
        List<Person> testPersonList = new ArrayList<>(5);
        List<Person> testActivePersonList = new ArrayList<>(5);

        Person mockTechActive = mock(Person.class);
        when(mockTechActive.isTech()).thenReturn(true);
        when(mockTechActive.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechActive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActive).getStatus();
        when(mockTechActive.getMinutesLeft()).thenReturn(240);
        when(mockTechActive.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActive);
        testActivePersonList.add(mockTechActive);

        Person mockTechActiveTwo = mock(Person.class);
        when(mockTechActiveTwo.isTech()).thenReturn(true);
        when(mockTechActiveTwo.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechActiveTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActiveTwo).getStatus();
        when(mockTechActiveTwo.getMinutesLeft()).thenReturn(1);
        when(mockTechActiveTwo.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActiveTwo);
        testActivePersonList.add(mockTechActiveTwo);

        Person mockTechInactive = mock(Person.class);
        when(mockTechInactive.isTech()).thenReturn(true);
        when(mockTechInactive.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechInactive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.RETIRED).when(mockTechInactive).getStatus();
        when(mockTechInactive.getMinutesLeft()).thenReturn(240);
        when(mockTechInactive.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechInactive);

        Person mockTechNoTime = mock(Person.class);
        when(mockTechNoTime.isTech()).thenReturn(true);
        when(mockTechNoTime.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechNoTime.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechNoTime).getStatus();
        when(mockTechNoTime.getMinutesLeft()).thenReturn(0);
        when(mockTechNoTime.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechNoTime);
        testActivePersonList.add(mockTechNoTime);

        Person mockNonTechOne = mock(Person.class);
        when(mockNonTechOne.isTech()).thenReturn(false);
        when(mockNonTechOne.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
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
        when(testCampaign.getTechsExpanded(anyBoolean(), anyBoolean(), anyBoolean())).thenCallRealMethod();

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

    @ParameterizedTest
    @EnumSource(value = CampaignTransportType.class)
    void testTransportShips(CampaignTransportType campaignTransportType) {
        Campaign campaign = spy(new Campaign());

        // New campaigns have no transports
        assertTrue(campaign.getTransports(campaignTransportType).isEmpty());
        campaign.hasTransports(campaignTransportType);

        // Create a mock transport
        Dropship mockTransport = mock(Dropship.class);

        UUID mockId = UUID.randomUUID();
        Unit mockUnit = mock(Unit.class);
        when(mockUnit.getId()).thenReturn(mockId);
        when(mockUnit.getEntity()).thenReturn(mockTransport);

        // Create mock transport capacity info for transport
        AbstractTransportedUnitsSummary mockTransportedUnitsSummary = mock(campaignTransportType.getTransportedUnitsSummaryType());
        when(mockTransportedUnitsSummary.getTransportCapabilities()).thenReturn(new HashSet<>(List.of(Bay.class)));

        when(mockUnit.getTransportedUnitsSummary(campaignTransportType)).thenReturn(mockTransportedUnitsSummary);

        // Add our mock transport
        campaign.importUnit(mockUnit);
        campaign.addCampaignTransport(campaignTransportType, mockUnit);

        // Ensure our mock transport exists
        assertEquals(1, campaign.getTransports(campaignTransportType).size());
        assertTrue(campaign.getTransportsByType(campaignTransportType, Bay.class).contains(mockUnit));

        // Add our mock transport a second time
        campaign.addCampaignTransport(campaignTransportType, mockUnit);

        // Ensure our mock transport exists only once
        assertEquals(1, campaign.getTransports(campaignTransportType).size());
        assertTrue(campaign.getTransportsByType(campaignTransportType, Bay.class).contains(mockUnit));

        // Remove the mock transport
        campaign.removeCampaignTransporter(campaignTransportType, mockUnit);

        // Ensure it was removed
        campaign.hasTransports(campaignTransportType);
        assertTrue(campaign.getTransports(campaignTransportType).isEmpty());
    }

    @Test
    void testIniative() {
        Campaign campaign = new Campaign();

        campaign.applyInitiativeBonus(6);
        // should increase bonus to 6 and max to 6
        assertTrue(campaign.getInitiativeBonus() == 6);
        assertTrue(campaign.getInitiativeMaxBonus() == 6);
        // Should not be able to increment over max of 6
        campaign.initiativeBonusIncrement(true);
        assertFalse(campaign.getInitiativeBonus() == 7);
        campaign.applyInitiativeBonus(2);
        assertEquals(campaign.getInitiativeBonus(), 6);
        // But should be able to decrease below max
        campaign.initiativeBonusIncrement(false);
        assertEquals(campaign.getInitiativeBonus(), 5);
        // After setting lower Max Bonus any appied bonus thats less then max should set
        // bonus to max
        campaign.setInitiativeMaxBonus(3);
        campaign.applyInitiativeBonus(2);
        assertEquals(campaign.getInitiativeBonus(), 3);

    }
}
