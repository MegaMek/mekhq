/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.Person.MAXIMUM_WEALTH;
import static mekhq.campaign.personnel.Person.MINIMUM_WEALTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.TechConstants;
import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk;
import mekhq.campaign.randomEvents.personalities.enums.Reasoning;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class PersonTest {
    private Person mockPerson;

    @Test
    public void testAddAndRemoveAward() {
        initPerson();
        initAwards();

        CampaignOptions mockCampaignOpts = mock(CampaignOptions.class);
        when(mockCampaignOpts.isTrackTotalXPEarnings()).thenReturn(false);
        when(mockCampaignOpts.getAwardBonusStyle()).thenReturn(AwardBonus.BOTH);

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1",
              LocalDate.parse("3000-01-01"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1",
              LocalDate.parse("3000-01-02"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 2",
              LocalDate.parse("3000-01-01"));

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-01"),
              LocalDate.parse("3000-01-02"));

        assertTrue(mockPerson.getAwardController().hasAwards());
        assertEquals(2, mockPerson.getAwardController().getAwards().size());

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 2", LocalDate.parse("3000-01-01"),
              LocalDate.parse("3000-01-02"));

        assertTrue(mockPerson.getAwardController().hasAwards());
        assertEquals(1, mockPerson.getAwardController().getAwards().size());

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-02"),
              LocalDate.parse("3000-01-02"));

        assertFalse(mockPerson.getAwardController().hasAwards());
        assertEquals(0, mockPerson.getAwardController().getAwards().size());
    }

    @Test
    public void testGetNumberOfAwards() {
        initPerson();
        initAwards();

        CampaignOptions mockCampaignOpts = mock(CampaignOptions.class);
        when(mockCampaignOpts.isTrackTotalXPEarnings()).thenReturn(false);
        when(mockCampaignOpts.getAwardBonusStyle()).thenReturn(AwardBonus.BOTH);

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1",
              LocalDate.parse("3000-01-01"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1",
              LocalDate.parse("3000-01-02"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 2",
              LocalDate.parse("3000-01-01"));

        assertEquals(2, mockPerson.getAwardController().getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-01"),
              LocalDate.parse("3000-01-02"));

        assertEquals(1, mockPerson.getAwardController().getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-02"),
              LocalDate.parse("3000-01-02"));

        assertEquals(0, mockPerson.getAwardController().getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));
    }

    @Test
    public void testSetOriginalUnit() {
        initPerson();

        UUID is1Id = UUID.randomUUID();
        int is1WeightClass = EntityWeightClass.WEIGHT_LIGHT;

        Unit is1 = mock(Unit.class);
        when(is1.getId()).thenReturn(is1Id);

        Entity is1Entity = mock(Entity.class);
        when(is1Entity.isClan()).thenReturn(false);
        when(is1Entity.getTechLevel()).thenReturn(TechConstants.T_INTRO_BOX_SET);
        when(is1Entity.getWeightClass()).thenReturn(is1WeightClass);
        when(is1.getEntity()).thenReturn(is1Entity);

        mockPerson.setOriginalUnit(is1);
        assertEquals(Person.TECH_IS1, mockPerson.getOriginalUnitTech());
        assertEquals(is1WeightClass, mockPerson.getOriginalUnitWeight());
        assertEquals(is1Id, mockPerson.getOriginalUnitId());

        int[] is2Techs = new int[] {
              TechConstants.T_IS_TW_NON_BOX,
              TechConstants.T_IS_TW_ALL,
              TechConstants.T_IS_ADVANCED,
              TechConstants.T_IS_EXPERIMENTAL,
              TechConstants.T_IS_UNOFFICIAL,
              };
        for (int is2TechLevel : is2Techs) {
            UUID is2Id = UUID.randomUUID();
            int is2WeightClass = EntityWeightClass.WEIGHT_HEAVY;

            Unit is2 = mock(Unit.class);
            when(is2.getId()).thenReturn(is2Id);

            Entity is2Entity = mock(Entity.class);
            when(is2Entity.isClan()).thenReturn(false);
            when(is2Entity.getTechLevel()).thenReturn(is2TechLevel);
            when(is2Entity.getWeightClass()).thenReturn(is2WeightClass);
            when(is2.getEntity()).thenReturn(is2Entity);

            mockPerson.setOriginalUnit(is2);
            assertEquals(Person.TECH_IS2, mockPerson.getOriginalUnitTech());
            assertEquals(is2WeightClass, mockPerson.getOriginalUnitWeight());
            assertEquals(is2Id, mockPerson.getOriginalUnitId());
        }

        int[] clanTechs = new int[] {
              TechConstants.T_CLAN_TW,
              TechConstants.T_CLAN_ADVANCED,
              TechConstants.T_CLAN_EXPERIMENTAL,
              TechConstants.T_CLAN_UNOFFICIAL,
              };
        for (int clanTech : clanTechs) {
            UUID clanId = UUID.randomUUID();
            int clanWeightClass = EntityWeightClass.WEIGHT_MEDIUM;

            Unit clan = mock(Unit.class);
            when(clan.getId()).thenReturn(clanId);

            Entity clanEntity = mock(Entity.class);
            when(clanEntity.isClan()).thenReturn(true);
            when(clanEntity.getTechLevel()).thenReturn(clanTech);
            when(clanEntity.getWeightClass()).thenReturn(clanWeightClass);
            when(clan.getEntity()).thenReturn(clanEntity);

            mockPerson.setOriginalUnit(clan);
            assertEquals(Person.TECH_CLAN, mockPerson.getOriginalUnitTech());
            assertEquals(clanWeightClass, mockPerson.getOriginalUnitWeight());
            assertEquals(clanId, mockPerson.getOriginalUnitId());
        }
    }

    @Test
    public void testTechUnits() {
        initPerson();

        UUID id0 = UUID.randomUUID();
        Unit unit0 = mock(Unit.class);
        when(unit0.getId()).thenReturn(id0);

        UUID id1 = UUID.randomUUID();
        Unit unit1 = mock(Unit.class);
        when(unit1.getId()).thenReturn(id1);

        // Add a tech unit
        mockPerson.addTechUnit(unit0);
        assertNotNull(mockPerson.getTechUnits());
        assertFalse(mockPerson.getTechUnits().isEmpty());
        assertTrue(mockPerson.getTechUnits().contains(unit0));

        // Add a second unit
        mockPerson.addTechUnit(unit1);
        assertEquals(2, mockPerson.getTechUnits().size());
        assertTrue(mockPerson.getTechUnits().contains(unit1));

        // Adding the same unit twice does not add it again!
        mockPerson.addTechUnit(unit1);
        assertEquals(2, mockPerson.getTechUnits().size());
        assertTrue(mockPerson.getTechUnits().contains(unit1));

        // Remove the first unit
        mockPerson.removeTechUnit(unit0);
        assertEquals(1, mockPerson.getTechUnits().size());
        assertFalse(mockPerson.getTechUnits().contains(unit0));
        assertTrue(mockPerson.getTechUnits().contains(unit1));

        // Ensure we can clear the units
        mockPerson.clearTechUnits();
        assertNotNull(mockPerson.getTechUnits());
        assertTrue(mockPerson.getTechUnits().isEmpty());
    }

    @Test
    public void testIsDeployed() {
        initPerson();

        // No unit? We're not deployed
        assertFalse(mockPerson.isDeployed());

        UUID id0 = UUID.randomUUID();
        Unit unit0 = mock(Unit.class);
        when(unit0.getId()).thenReturn(id0);
        when(unit0.getScenarioId()).thenReturn(-1);

        mockPerson.setUnit(unit0);
        assertEquals(unit0, mockPerson.getUnit());

        // If the unit is not deployed, the person is not delpoyed
        assertFalse(mockPerson.isDeployed());

        // Deploy the unit
        when(unit0.getScenarioId()).thenReturn(1);

        // The person should now be deployed
        assertTrue(mockPerson.isDeployed());
    }

    @Test
    public void testAddInjuriesResetsUnitStatus() {
        initPerson();

        // Add an injury without a unit
        Injury injury0 = mock(Injury.class);
        mockPerson.addInjury(injury0);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = mock(Unit.class);
        when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        // Add an injury with a unit
        Injury injury1 = mock(Injury.class);
        mockPerson.addInjury(injury1);

        // Ensure the unit had its status reset to reflect crew damage
        verify(unit0, Mockito.times(1)).resetPilotAndEntity();
    }

    @Test
    public void testPrisonerRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = mock(CampaignOptions.class);

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        when(mockCampaign.getName()).thenReturn("Campaign");
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = mock(Unit.class);
        when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        mockPerson.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, true);

        // Ensure the unit removes the person
        verify(unit0, Mockito.times(1)).remove(Mockito.eq(mockPerson), Mockito.anyBoolean());
    }

    @Test
    public void testPrisonerDefectorRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = mock(CampaignOptions.class);

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        when(mockCampaign.getName()).thenReturn("Campaign");
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = mock(Unit.class);
        when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        mockPerson.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER_DEFECTOR, true);

        // Ensure the unit removes the person
        verify(unit0, Mockito.times(1)).remove(Mockito.eq(mockPerson), Mockito.anyBoolean());
    }

    @Test
    public void testBondsmanRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = mock(CampaignOptions.class);

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        when(mockCampaign.getName()).thenReturn("Campaign");
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = mock(Unit.class);
        when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        mockPerson.setPrisonerStatus(mockCampaign, PrisonerStatus.BONDSMAN, true);

        // Ensure the unit removes the person
        verify(unit0, Mockito.times(1)).remove(Mockito.eq(mockPerson), Mockito.anyBoolean());
    }

    @Test
    public void testFreeNotRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = mock(CampaignOptions.class);

        Campaign mockCampaign = mock(Campaign.class);
        when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        when(mockCampaign.getName()).thenReturn("Campaign");
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = mock(Unit.class);
        when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        mockPerson.setPrisonerStatus(mockCampaign, PrisonerStatus.FREE, true);

        // Ensure the unit DOES NOT remove the person
        verify(unit0, Mockito.times(0)).remove(Mockito.eq(mockPerson), Mockito.anyBoolean());
    }

    private void initPerson() {
        mockPerson = spy(new Person("TestGivenName", "TestSurname", null, "MERC"));
    }

    private void initAwards() {
        AwardsFactory.getInstance().loadAwardsFromStream(PersonnelTestUtilities.getTestAwardSet(), "TestSet");
    }

    @Test
    void testGambleWealth_rollLosesWealth() {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person gambler = new Person(mockCampaign);
        gambler.setWealth(3);
        gambler.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, PersonnelOptions.COMPULSION_GAMBLING,
              true);

        try (MockedStatic<Compute> mockStatic = mockStatic(Compute.class)) {
            mockStatic.when(Compute::d6).thenReturn(2); // Simulate losing roll
            gambler.gambleWealth();
        }

        int expected = 2;
        int actual = gambler.getWealth();
        assertEquals(expected, actual, "Expected wealth to be " + expected + " but was " + actual);
    }

    @Test
    void testGambleWealth_rollGainsWealth() {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person gambler = new Person(mockCampaign);
        gambler.setWealth(3);
        gambler.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, PersonnelOptions.COMPULSION_GAMBLING,
              true);

        try (MockedStatic<Compute> mockStatic = mockStatic(Compute.class)) {
            mockStatic.when(Compute::d6).thenReturn(6); // Simulate winning roll
            gambler.gambleWealth();
        }

        int expected = 4;
        int actual = gambler.getWealth();
        assertEquals(expected, actual, "Expected wealth to be " + expected + " but was " + actual);
    }

    @Test
    void testGambleWealth_noWealthChange() {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person gambler = new Person(mockCampaign);
        gambler.setWealth(3);
        gambler.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, PersonnelOptions.COMPULSION_GAMBLING,
              true);

        try (MockedStatic<Compute> mockStatic = mockStatic(Compute.class)) {
            mockStatic.when(Compute::d6).thenReturn(4); // Simulate neutral roll
            gambler.gambleWealth();
        }

        int expected = 3;
        int actual = gambler.getWealth();
        assertEquals(expected, actual, "Expected wealth to be " + expected + " but was " + actual);
    }

    @Test
    void testGambleWealth_noWealthChange_wealthTooHighForGain() {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person gambler = new Person(mockCampaign);
        gambler.setWealth(MAXIMUM_WEALTH);
        gambler.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, PersonnelOptions.COMPULSION_GAMBLING,
              true);

        try (MockedStatic<Compute> mockStatic = mockStatic(Compute.class)) {
            mockStatic.when(Compute::d6).thenReturn(6); // Simulate winning roll
            gambler.gambleWealth();
        }

        int expected = MAXIMUM_WEALTH;
        int actual = gambler.getWealth();
        assertEquals(expected, actual, "Expected wealth to be " + expected + " but was " + actual);
    }

    @Test
    void testGambleWealth_noWealthChange_wealthTooLowForLoss() {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person gambler = new Person(mockCampaign);
        gambler.setWealth(MINIMUM_WEALTH);
        gambler.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, PersonnelOptions.COMPULSION_GAMBLING,
              true);

        try (MockedStatic<Compute> mockStatic = mockStatic(Compute.class)) {
            mockStatic.when(Compute::d6).thenReturn(1); // Simulate losing roll
            gambler.gambleWealth();
        }

        int expected = MINIMUM_WEALTH;
        int actual = gambler.getWealth();
        assertEquals(expected, actual, "Expected wealth to be " + expected + " but was " + actual);
    }


    @Test
    void testProcessDiscontinuationSyndrome_noAddiction() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, false, false, true, 1, false, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigueDirect());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_passedWillpowerCheck() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, false, false, true, 1, true, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigueDirect());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_useFatigue_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, false, false, true, 1, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(1, person.getHits());
        assertEquals(2, person.getFatigueDirect());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_useFatigue_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, true, false, true, 1, true, true);
        assertEquals(1, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(2, person.getFatigueDirect());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_noFatigue_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, false, false, false, 1, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(1, person.getHits());
        assertEquals(0, person.getFatigueDirect());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_noFatigue_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, true, false, false, 1, true, true);
        assertEquals(1, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigueDirect());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_characterKilled_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.setHits(5);

        person.processDiscontinuationSyndrome(mockCampaign, false, false, false, 1, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(6, person.getHits());
        assertEquals(0, person.getFatigueDirect());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_characterKilled_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        for (int i = 0; i < 5; i++) {
            person.addInjury(new Injury());
        }

        person.processDiscontinuationSyndrome(mockCampaign, true, false, false, 1, true, true);
        assertEquals(6, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigueDirect());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessCripplingFlashbacks_noFlashbacks() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processCripplingFlashbacks(mockCampaign, false, false, false, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCripplingFlashbacks_passedWillpowerCheck() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processCripplingFlashbacks(mockCampaign, false, false, true, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCripplingFlashbacks_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processCripplingFlashbacks(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(1, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCripplingFlashbacks_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processCripplingFlashbacks(mockCampaign, true, false, true, true);
        assertEquals(1, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCripplingFlashbacks_characterKilled_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.setHits(5);

        person.processCripplingFlashbacks(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(6, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessCripplingFlashbacks_characterKilled_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        for (int i = 0; i < 5; i++) {
            person.addInjury(new Injury());
        }

        person.processCripplingFlashbacks(mockCampaign, true, false, true, true);
        assertEquals(6, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testSwitchPersonality_singleSwitch_allFieldsChanged() {
        Person before = createPersonality();

        Person after = createPersonality();
        after.switchPersonality();

        assertEquals(before.getStoredGivenName(), after.getGivenName());
        assertEquals(before.getStoredSurname(), after.getSurname());
        assertEquals(before.getStoredLoyalty(), after.getBaseLoyalty());
        assertEquals(before.getStoredOriginFaction().getShortName(), after.getOriginFaction().getShortName());
        assertEquals(before.getStoredAggression(), after.getAggression());
        assertEquals(before.getStoredAggressionDescriptionIndex(), after.getAggressionDescriptionIndex());
        assertEquals(before.getStoredAmbition(), after.getAmbition());
        assertEquals(before.getStoredAmbitionDescriptionIndex(), after.getAmbitionDescriptionIndex());
        assertEquals(before.getStoredGreed(), after.getGreed());
        assertEquals(before.getStoredGreedDescriptionIndex(), after.getGreedDescriptionIndex());
        assertEquals(before.getStoredSocial(), after.getSocial());
        assertEquals(before.getStoredSocialDescriptionIndex(), after.getSocialDescriptionIndex());
        assertEquals(before.getStoredPersonalityQuirk(), after.getPersonalityQuirk());
        assertEquals(before.getStoredPersonalityQuirkDescriptionIndex(), after.getPersonalityQuirkDescriptionIndex());
        assertEquals(before.getStoredReasoning(), after.getReasoning());
    }

    @Test
    void testSwitchPersonality_singleSwitch_allFieldsStored() {
        Person before = createPersonality();

        Person after = createPersonality();
        after.switchPersonality();

        assertEquals(after.getStoredGivenName(), before.getGivenName());
        assertEquals(after.getStoredSurname(), before.getSurname());
        assertEquals(after.getStoredLoyalty(), before.getBaseLoyalty());
        assertEquals(after.getStoredOriginFaction().getShortName(), before.getOriginFaction().getShortName());
        assertEquals(after.getStoredAggression(), before.getAggression());
        assertEquals(after.getStoredAggressionDescriptionIndex(), before.getAggressionDescriptionIndex());
        assertEquals(after.getStoredAmbition(), before.getAmbition());
        assertEquals(after.getStoredAmbitionDescriptionIndex(), before.getAmbitionDescriptionIndex());
        assertEquals(after.getStoredGreed(), before.getGreed());
        assertEquals(after.getStoredGreedDescriptionIndex(), before.getGreedDescriptionIndex());
        assertEquals(after.getStoredSocial(), before.getSocial());
        assertEquals(after.getStoredSocialDescriptionIndex(), before.getSocialDescriptionIndex());
        assertEquals(after.getStoredPersonalityQuirk(), before.getPersonalityQuirk());
        assertEquals(after.getStoredPersonalityQuirkDescriptionIndex(), before.getPersonalityQuirkDescriptionIndex());
        assertEquals(after.getStoredReasoning(), before.getReasoning());
    }

    @Test
    void testSwitchPersonality_doubleSwitch_personalityFullyRestored() {
        Person before = createPersonality();

        Person after = createPersonality();
        after.switchPersonality();
        after.switchPersonality();

        assertEquals(before.getGivenName(), after.getGivenName());
        assertEquals(before.getSurname(), after.getSurname());
        assertEquals(before.getBaseLoyalty(), after.getBaseLoyalty());
        assertEquals(before.getOriginFaction().getShortName(), after.getOriginFaction().getShortName());
        assertEquals(before.getAggression(), after.getAggression());
        assertEquals(before.getAggressionDescriptionIndex(), after.getAggressionDescriptionIndex());
        assertEquals(before.getAmbition(), after.getAmbition());
        assertEquals(before.getAmbitionDescriptionIndex(), after.getAmbitionDescriptionIndex());
        assertEquals(before.getGreed(), after.getGreed());
        assertEquals(before.getGreedDescriptionIndex(), after.getGreedDescriptionIndex());
        assertEquals(before.getSocial(), after.getSocial());
        assertEquals(before.getSocialDescriptionIndex(), after.getSocialDescriptionIndex());
        assertEquals(before.getPersonalityQuirk(), after.getPersonalityQuirk());
        assertEquals(before.getPersonalityQuirkDescriptionIndex(), after.getPersonalityQuirkDescriptionIndex());
        assertEquals(before.getReasoning(), after.getReasoning());

        assertEquals(before.getStoredGivenName(), after.getStoredGivenName());
        assertEquals(before.getStoredSurname(), after.getStoredSurname());
        assertEquals(before.getStoredLoyalty(), after.getStoredLoyalty());
        assertEquals(before.getStoredOriginFaction().getShortName(), after.getStoredOriginFaction().getShortName());
        assertEquals(before.getStoredAggression(), after.getStoredAggression());
        assertEquals(before.getStoredAggressionDescriptionIndex(), after.getStoredAggressionDescriptionIndex());
        assertEquals(before.getStoredAmbition(), after.getStoredAmbition());
        assertEquals(before.getStoredAmbitionDescriptionIndex(), after.getStoredAmbitionDescriptionIndex());
        assertEquals(before.getStoredGreed(), after.getStoredGreed());
        assertEquals(before.getStoredGreedDescriptionIndex(), after.getStoredGreedDescriptionIndex());
        assertEquals(before.getStoredSocial(), after.getStoredSocial());
        assertEquals(before.getStoredSocialDescriptionIndex(), after.getStoredSocialDescriptionIndex());
        assertEquals(before.getStoredPersonalityQuirk(), after.getStoredPersonalityQuirk());
        assertEquals(before.getStoredPersonalityQuirkDescriptionIndex(),
              after.getStoredPersonalityQuirkDescriptionIndex());
        assertEquals(before.getStoredReasoning(), after.getStoredReasoning());
    }

    private Person createPersonality() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction originalMockFaction = mock(Faction.class);
        Faction storedMockFaction = mock(Faction.class);

        when(mockCampaign.getFaction()).thenReturn(originalMockFaction);
        when(originalMockFaction.getShortName()).thenReturn("MERC");
        when(storedMockFaction.getShortName()).thenReturn("DC");

        Person personality = new Person(mockCampaign);
        personality.setGivenName("Arnold");
        personality.setSurname("Rimmer");
        personality.setLoyalty(6);
        personality.setOriginFaction(originalMockFaction);
        personality.setAggression(Aggression.AGGRESSIVE);
        personality.setAggressionDescriptionIndex(1);
        personality.setAmbition(Ambition.AMBITIOUS);
        personality.setAmbitionDescriptionIndex(1);
        personality.setGreed(Greed.ADEPT);
        personality.setGreedDescriptionIndex(1);
        personality.setSocial(Social.ALTRUISTIC);
        personality.setSocialDescriptionIndex(1);
        personality.setPersonalityQuirk(PersonalityQuirk.ACROPHOBIA);
        personality.setPersonalityQuirkDescriptionIndex(1);
        personality.setReasoning(Reasoning.BRAIN_DEAD);

        personality.setStoredGivenName("Mr.");
        personality.setStoredSurname("Flibble");
        personality.setStoredLoyalty(9);
        personality.setStoredOriginFaction(storedMockFaction);
        personality.setStoredAggression(Aggression.BLOODTHIRSTY);
        personality.setStoredAggressionDescriptionIndex(0);
        personality.setStoredAmbition(Ambition.CUTTHROAT);
        personality.setStoredAmbitionDescriptionIndex(0);
        personality.setStoredGreed(Greed.ENTERPRISING);
        personality.setStoredGreedDescriptionIndex(0);
        personality.setStoredSocial(Social.COMPASSIONATE);
        personality.setStoredSocialDescriptionIndex(0);
        personality.setStoredPersonalityQuirk(PersonalityQuirk.AMBUSH_LOVER);
        personality.setStoredPersonalityQuirkDescriptionIndex(0);
        personality.setStoredReasoning(Reasoning.DIMWITTED);

        return personality;
    }


    @Test
    void testProcessConfusion_noConfusion() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);

        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processConfusion(mockCampaign, false, false, true, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessConfusion_passedWillpowerCheck() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);

        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processConfusion(mockCampaign, false, false, true, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessConfusion_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);

        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processConfusion(mockCampaign, false, false, true, true);
        assertTrue(person.getInjuries().isEmpty());
        assertTrue(person.getHits() > 0);
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessConfusion_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);

        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processConfusion(mockCampaign, true, false, true, true);
        assertFalse(person.getInjuries().isEmpty());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessConfusion_characterKilled_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);

        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.setHits(5);

        person.processConfusion(mockCampaign, false, false, true, true);
        assertTrue(person.getInjuries().isEmpty());
        assertTrue(person.getHits() > 5);
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessConfusion_characterKilled_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);

        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        for (int i = 0; i < 5; i++) {
            person.addInjury(new Injury());
        }

        person.processConfusion(mockCampaign, true, false, true, true);
        assertTrue(person.getInjuries().size() > 5);
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessChildlikeRegression_noRegression() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processChildlikeRegression(mockCampaign, false, false, false, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessChildlikeRegression_passedWillpowerCheck() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processChildlikeRegression(mockCampaign, false, false, true, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessChildlikeRegression_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processChildlikeRegression(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(1, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessChildlikeRegression_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processChildlikeRegression(mockCampaign, true, false, true, true);
        assertEquals(1, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessChildlikeRegression_characterKilled_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.setHits(5);

        person.processChildlikeRegression(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(6, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessChildlikeRegression_characterKilled_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        for (int i = 0; i < 5; i++) {
            person.addInjury(new Injury());
        }

        person.processChildlikeRegression(mockCampaign, true, false, true, true);
        assertEquals(6, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessCatatonia_noCatatonia() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processCatatonia(mockCampaign, false, false, false, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCatatonia_passedWillpowerCheck() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processCatatonia(mockCampaign, false, false, true, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCatatonia_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processCatatonia(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(1, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCatatonia_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.processCatatonia(mockCampaign, true, false, true, true);
        assertEquals(1, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessCatatonia_characterKilled_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        person.setHits(5);

        person.processCatatonia(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(6, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessCatatonia_characterKilled_useAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        LocalDate currentDate = LocalDate.of(3151, 1, 1);
        Hangar mockHangar = mock(Hangar.class);
        Warehouse mockWarehouse = mock(Warehouse.class);
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");
        when(mockCampaign.getLocalDate()).thenReturn(currentDate);
        when(mockCampaign.getHangar()).thenReturn(mockHangar);
        when(mockCampaign.getWarehouse()).thenReturn(mockWarehouse);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);

        Person person = new Person(mockCampaign);
        for (int i = 0; i < 5; i++) {
            person.addInjury(new Injury());
        }

        person.processCatatonia(mockCampaign, true, false, true, true);
        assertEquals(6, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void returnsHitsWhenNoInjuries() throws Exception {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        setField(person, "hits", 3);
        setField(person, "injuries", new ArrayList<Injury>());

        assertEquals(3, person.getNonPermanentInjurySeverity());
    }

    @Test
    void ignoresPermanentInjuries() throws Exception {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        setField(person, "hits", 2);

        Injury permanent1 = mock(Injury.class);
        when(permanent1.isPermanent()).thenReturn(true);
        when(permanent1.getHits()).thenReturn(10);

        Injury permanent2 = mock(Injury.class);
        when(permanent2.isPermanent()).thenReturn(true);
        when(permanent2.getHits()).thenReturn(1);

        setField(person, "injuries", new ArrayList<>(List.of(permanent1, permanent2)));

        // Only base hits should count
        assertEquals(2, person.getNonPermanentInjurySeverity());
    }

    @Test
    void sumsOnlyNonPermanentInjuryHitsPlusBaseHits() throws Exception {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        setField(person, "hits", 5);

        Injury nonPermanent1 = mock(Injury.class);
        when(nonPermanent1.isPermanent()).thenReturn(false);
        when(nonPermanent1.getHits()).thenReturn(2);

        Injury permanent = mock(Injury.class);
        when(permanent.isPermanent()).thenReturn(true);
        when(permanent.getHits()).thenReturn(100); // should be ignored

        Injury nonPermanent2 = mock(Injury.class);
        when(nonPermanent2.isPermanent()).thenReturn(false);
        when(nonPermanent2.getHits()).thenReturn(4);

        setField(person, "injuries", new ArrayList<>(List.of(nonPermanent1, permanent, nonPermanent2)));

        // 5 + 2 + 4 = 11
        assertEquals(11, person.getNonPermanentInjurySeverity());
    }

    @Test
    void countsNonPermanentInjuriesEvenWhenHitsIsZero() throws Exception {
        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        setField(person, "hits", 0);

        Injury injury = mock(Injury.class);
        when(injury.isPermanent()).thenReturn(false);
        when(injury.getHits()).thenReturn(3);

        setField(person, "injuries", new ArrayList<>(List.of(injury)));

        assertEquals(3, person.getNonPermanentInjurySeverity());
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field declaredField = target.getClass().getDeclaredField(fieldName);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }
}
