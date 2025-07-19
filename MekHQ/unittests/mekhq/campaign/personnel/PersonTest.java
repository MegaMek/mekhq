/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.UUID;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Hangar;
import mekhq.campaign.Warehouse;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.PersonnelStatus;
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
        when(is1Entity.getTechLevel()).thenReturn(TechConstants.T_INTRO_BOXSET);
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
        person.processDiscontinuationSyndrome(mockCampaign, false, true, false, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigue());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_passedWillpowerCheck() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, false, true, true, false);
        assertEquals(0, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigue());
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
        person.processDiscontinuationSyndrome(mockCampaign, false, true, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(1, person.getHits());
        assertEquals(2, person.getFatigue());
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
        person.processDiscontinuationSyndrome(mockCampaign, true, true, true, true);
        assertEquals(1, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(2, person.getFatigue());
        assertEquals(PersonnelStatus.ACTIVE, person.getStatus());
    }

    @Test
    void testProcessDiscontinuationSyndrome_noFatigue_noAdvancedMedical() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processDiscontinuationSyndrome(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(1, person.getHits());
        assertEquals(0, person.getFatigue());
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
        person.processDiscontinuationSyndrome(mockCampaign, true, false, true, true);
        assertEquals(1, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigue());
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

        person.processDiscontinuationSyndrome(mockCampaign, false, false, true, true);
        assertEquals(0, person.getInjuries().size());
        assertEquals(6, person.getHits());
        assertEquals(0, person.getFatigue());
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

        person.processDiscontinuationSyndrome(mockCampaign, true, false, true, true);
        assertEquals(6, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(0, person.getFatigue());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessCripplingFlashbacks_noFlashbacks() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);
        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processCripplingFlashbacks(mockCampaign, false, false, false);
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
        person.processCripplingFlashbacks(mockCampaign, false, true, false);
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
        person.processCripplingFlashbacks(mockCampaign, false, true, true);
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
        person.processCripplingFlashbacks(mockCampaign, true, true, true);
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

        person.processCripplingFlashbacks(mockCampaign, false, true, true);
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

        person.processCripplingFlashbacks(mockCampaign, true, true, true);
        assertEquals(6, person.getInjuries().size());
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }

    @Test
    void testProcessConfusion_noConfusion() {
        Campaign mockCampaign = mock(Campaign.class);
        Faction mockFaction = mock(Faction.class);

        when(mockCampaign.getFaction()).thenReturn(mockFaction);
        when(mockFaction.getShortName()).thenReturn("MERC");

        Person person = new Person(mockCampaign);
        person.processConfusion(mockCampaign, false, false, false);
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
        person.processConfusion(mockCampaign, false, true, false);
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
        person.processConfusion(mockCampaign, false, true, true);
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
        person.processConfusion(mockCampaign, true, true, true);
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

        person.processConfusion(mockCampaign, false, true, true);
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

        person.processConfusion(mockCampaign, true, true, true);
        assertTrue(person.getInjuries().size() > 5);
        assertEquals(0, person.getHits());
        assertEquals(PersonnelStatus.MEDICAL_COMPLICATIONS, person.getStatus());
    }
}
