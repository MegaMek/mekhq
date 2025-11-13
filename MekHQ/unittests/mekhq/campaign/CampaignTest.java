/*
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;


import static mekhq.campaign.unit.enums.TransporterType.ASF_BAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.TEST_CANON_SYSTEMS_DIR;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Dropship;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.AbstractTransportedUnitsSummary;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.TestSystems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import testUtilities.MHQTestUtilities;

/**
 * @author Deric Page (dericdotpageatgmaildotcom)
 * @since 6/10/14 10:23 AM
 */
public class CampaignTest {

    private TestSystems systems;

    @BeforeAll
    public static void setup() {
        EquipmentType.initializeTypes();
        Ranks.initializeRankSystems();
    }

    @BeforeEach
    public void before() {
        // Reset TestSystems
        systems = TestSystems.getInstance();
    }

    @Test
    void testCampaignConstructorWithDependencyInjection() {
        // Example of using dependency injection to provide test data directly to a Campaign instance
        // without mocking or spying.

        // Create a test CampaignConfiguration with default values but using the above TestSystems instance
        CampaignConfiguration config = MHQTestUtilities.buildTestConfigWithSystems(systems);

        // Let's try switching the year up.
        config.setCurrentDay(LocalDate.ofYearDay(2875, 183));

        // Add a system to the systems instance; it must exist in the testresources dir
        config.getSystemsInstance().load(TEST_CANON_SYSTEMS_DIR + "Skye.yml");

        // Instantiate the campaign with the new info
        Campaign campaign = new Campaign(config);

        // Let's plot a trip from the starting location to Skye!  It should be about 6 days:
        int travelTime = campaign.getSimplifiedTravelTime(systems.getSystemByName("Skye", config.getDate()));
        assertEquals(6, travelTime);
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
        when(mockTechActive.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActive);
        testActivePersonList.add(mockTechActive);

        Person mockTechActiveTwo = mock(Person.class);
        when(mockTechActiveTwo.isTech()).thenReturn(true);
        when(mockTechActiveTwo.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechActiveTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechActiveTwo).getStatus();
        when(mockTechActiveTwo.getMinutesLeft()).thenReturn(1);
        when(mockTechActiveTwo.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechActiveTwo);
        testActivePersonList.add(mockTechActiveTwo);

        Person mockTechInactive = mock(Person.class);
        when(mockTechInactive.isTech()).thenReturn(true);
        when(mockTechInactive.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechInactive.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.RETIRED).when(mockTechInactive).getStatus();
        when(mockTechInactive.getMinutesLeft()).thenReturn(240);
        when(mockTechInactive.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechInactive);

        Person mockTechNoTime = mock(Person.class);
        when(mockTechNoTime.isTech()).thenReturn(true);
        when(mockTechNoTime.getPrimaryRole()).thenReturn(PersonnelRole.MEK_TECH);
        when(mockTechNoTime.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockTechNoTime).getStatus();
        when(mockTechNoTime.getMinutesLeft()).thenReturn(0);
        when(mockTechNoTime.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockTechNoTime);
        testActivePersonList.add(mockTechNoTime);

        Person mockNonTechOne = mock(Person.class);
        when(mockNonTechOne.isTech()).thenReturn(false);
        when(mockNonTechOne.getPrimaryRole()).thenReturn(PersonnelRole.MEKWARRIOR);
        when(mockNonTechOne.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechOne).getStatus();
        when(mockNonTechOne.getMinutesLeft()).thenReturn(240);
        when(mockNonTechOne.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechOne);
        testActivePersonList.add(mockNonTechOne);

        Person mockNonTechTwo = mock(Person.class);
        when(mockNonTechTwo.isTech()).thenReturn(false);
        when(mockNonTechTwo.getPrimaryRole()).thenReturn(PersonnelRole.ADMINISTRATOR_COMMAND);
        when(mockNonTechTwo.getSecondaryRole()).thenReturn(PersonnelRole.NONE);
        doReturn(PersonnelStatus.ACTIVE).when(mockNonTechTwo).getStatus();
        when(mockNonTechTwo.getMinutesLeft()).thenReturn(240);
        when(mockNonTechTwo.getSkillLevel(any(Campaign.class),
              anyBoolean(),
              anyBoolean())).thenReturn(SkillLevel.REGULAR);
        testPersonList.add(mockNonTechTwo);
        testActivePersonList.add(mockNonTechTwo);

        Campaign testCampaign = mock(Campaign.class);
        when(testCampaign.getPersonnel()).thenReturn(testPersonList);
        when(testCampaign.getActivePersonnel(false, false)).thenReturn(testActivePersonList);
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
        Campaign campaign = spy(MHQTestUtilities.getTestCampaign());

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
        when(mockTransportedUnitsSummary.getTransportCapabilities()).thenReturn(new HashSet<>(List.of(ASF_BAY)));

        when(mockUnit.getTransportedUnitsSummary(campaignTransportType)).thenReturn(mockTransportedUnitsSummary);

        // Add our mock transport
        campaign.importUnit(mockUnit);
        campaign.addCampaignTransport(campaignTransportType, mockUnit);

        // Ensure our mock transport exists
        assertEquals(1, campaign.getTransports(campaignTransportType).size());
        assertTrue(campaign.getTransportsByType(campaignTransportType, ASF_BAY).contains(mockUnit));

        // Add our mock transport a second time
        campaign.addCampaignTransport(campaignTransportType, mockUnit);

        // Ensure our mock transport exists only once
        assertEquals(1, campaign.getTransports(campaignTransportType).size());
        assertTrue(campaign.getTransportsByType(campaignTransportType, ASF_BAY).contains(mockUnit));

        // Remove the mock transport
        campaign.removeCampaignTransporter(campaignTransportType, mockUnit);

        // Ensure it was removed
        campaign.hasTransports(campaignTransportType);
        assertTrue(campaign.getTransports(campaignTransportType).isEmpty());
    }

    @Test
    void testInitiative() {
        Campaign campaign = MHQTestUtilities.getTestCampaign();

        campaign.applyInitiativeBonus(6);
        // should increase bonus to 6 and max to 6
        assertEquals(6, campaign.getInitiativeBonus());
        assertEquals(6, campaign.getInitiativeMaxBonus());
        // Should not be able to increment over max of 6
        campaign.initiativeBonusIncrement(true);
        assertNotEquals(7, campaign.getInitiativeBonus());
        campaign.applyInitiativeBonus(2);
        assertEquals(6, campaign.getInitiativeBonus());
        // But should be able to decrease below max
        campaign.initiativeBonusIncrement(false);
        assertEquals(5, campaign.getInitiativeBonus());
        // After setting lower Max Bonus any applied bonus that's less than max should set
        // bonus to max
        campaign.setInitiativeMaxBonus(3);
        campaign.applyInitiativeBonus(2);
        assertEquals(3, campaign.getInitiativeBonus());

    }
}
