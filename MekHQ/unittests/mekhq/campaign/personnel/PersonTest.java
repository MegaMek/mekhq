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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.enums.AwardBonus;
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
import org.mockito.Mockito;

public class PersonTest {
    private Person mockPerson;

    @Test
    public void testAddAndRemoveAward() {
        initPerson();
        initAwards();

        CampaignOptions mockCampaignOpts = Mockito.mock(CampaignOptions.class);
        Mockito.when(mockCampaignOpts.isTrackTotalXPEarnings()).thenReturn(false);
        Mockito.when(mockCampaignOpts.getAwardBonusStyle()).thenReturn(AwardBonus.BOTH);

        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

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

        CampaignOptions mockCampaignOpts = Mockito.mock(CampaignOptions.class);
        Mockito.when(mockCampaignOpts.isTrackTotalXPEarnings()).thenReturn(false);
        Mockito.when(mockCampaignOpts.getAwardBonusStyle()).thenReturn(AwardBonus.BOTH);

        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

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

        Unit is1 = Mockito.mock(Unit.class);
        Mockito.when(is1.getId()).thenReturn(is1Id);

        Entity is1Entity = Mockito.mock(Entity.class);
        Mockito.when(is1Entity.isClan()).thenReturn(false);
        Mockito.when(is1Entity.getTechLevel()).thenReturn(TechConstants.T_INTRO_BOXSET);
        Mockito.when(is1Entity.getWeightClass()).thenReturn(is1WeightClass);
        Mockito.when(is1.getEntity()).thenReturn(is1Entity);

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

            Unit is2 = Mockito.mock(Unit.class);
            Mockito.when(is2.getId()).thenReturn(is2Id);

            Entity is2Entity = Mockito.mock(Entity.class);
            Mockito.when(is2Entity.isClan()).thenReturn(false);
            Mockito.when(is2Entity.getTechLevel()).thenReturn(is2TechLevel);
            Mockito.when(is2Entity.getWeightClass()).thenReturn(is2WeightClass);
            Mockito.when(is2.getEntity()).thenReturn(is2Entity);

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

            Unit clan = Mockito.mock(Unit.class);
            Mockito.when(clan.getId()).thenReturn(clanId);

            Entity clanEntity = Mockito.mock(Entity.class);
            Mockito.when(clanEntity.isClan()).thenReturn(true);
            Mockito.when(clanEntity.getTechLevel()).thenReturn(clanTech);
            Mockito.when(clanEntity.getWeightClass()).thenReturn(clanWeightClass);
            Mockito.when(clan.getEntity()).thenReturn(clanEntity);

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
        Unit unit0 = Mockito.mock(Unit.class);
        Mockito.when(unit0.getId()).thenReturn(id0);

        UUID id1 = UUID.randomUUID();
        Unit unit1 = Mockito.mock(Unit.class);
        Mockito.when(unit1.getId()).thenReturn(id1);

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
        Unit unit0 = Mockito.mock(Unit.class);
        Mockito.when(unit0.getId()).thenReturn(id0);
        Mockito.when(unit0.getScenarioId()).thenReturn(-1);

        mockPerson.setUnit(unit0);
        assertEquals(unit0, mockPerson.getUnit());

        // If the unit is not deployed, the person is not delpoyed
        assertFalse(mockPerson.isDeployed());

        // Deploy the unit
        Mockito.when(unit0.getScenarioId()).thenReturn(1);

        // The person should now be deployed
        assertTrue(mockPerson.isDeployed());
    }

    @Test
    public void testAddInjuriesResetsUnitStatus() {
        initPerson();

        // Add an injury without a unit
        Injury injury0 = Mockito.mock(Injury.class);
        mockPerson.addInjury(injury0);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = Mockito.mock(Unit.class);
        Mockito.when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        // Add an injury with a unit
        Injury injury1 = Mockito.mock(Injury.class);
        mockPerson.addInjury(injury1);

        // Ensure the unit had its status reset to reflect crew damage
        verify(unit0, Mockito.times(1)).resetPilotAndEntity();
    }

    @Test
    public void testPrisonerRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = Mockito.mock(CampaignOptions.class);

        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Mockito.when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        Mockito.when(mockCampaign.getName()).thenReturn("Campaign");
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = Mockito.mock(Unit.class);
        Mockito.when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        mockPerson.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER, true);

        // Ensure the unit removes the person
        verify(unit0, Mockito.times(1)).remove(Mockito.eq(mockPerson), Mockito.anyBoolean());
    }

    @Test
    public void testPrisonerDefectorRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = Mockito.mock(CampaignOptions.class);

        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Mockito.when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        Mockito.when(mockCampaign.getName()).thenReturn("Campaign");
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = Mockito.mock(Unit.class);
        Mockito.when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        mockPerson.setPrisonerStatus(mockCampaign, PrisonerStatus.PRISONER_DEFECTOR, true);

        // Ensure the unit removes the person
        verify(unit0, Mockito.times(1)).remove(Mockito.eq(mockPerson), Mockito.anyBoolean());
    }

    @Test
    public void testBondsmanRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = Mockito.mock(CampaignOptions.class);

        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Mockito.when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        Mockito.when(mockCampaign.getName()).thenReturn("Campaign");
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = Mockito.mock(Unit.class);
        Mockito.when(unit0.getId()).thenReturn(id0);

        mockPerson.setUnit(unit0);

        mockPerson.setPrisonerStatus(mockCampaign, PrisonerStatus.BONDSMAN, true);

        // Ensure the unit removes the person
        verify(unit0, Mockito.times(1)).remove(Mockito.eq(mockPerson), Mockito.anyBoolean());
    }

    @Test
    public void testFreeNotRemovedFromUnit() {
        initPerson();

        CampaignOptions mockCampaignOpts = Mockito.mock(CampaignOptions.class);

        Campaign mockCampaign = Mockito.mock(Campaign.class);
        Mockito.when(mockCampaign.getLocalDate()).thenReturn(LocalDate.now());
        Mockito.when(mockCampaign.getName()).thenReturn("Campaign");
        Mockito.when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOpts);

        // Add a unit to the person
        UUID id0 = UUID.randomUUID();
        Unit unit0 = Mockito.mock(Unit.class);
        Mockito.when(unit0.getId()).thenReturn(id0);

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
    void testSwitchPersonality_singleSwitch_allFieldsChanged() {
        Person before = createPersonality();

        Person after = createPersonality();
        after.switchPersonality();

        assertEquals(before.getStoredGivenName(), after.getGivenName());
        assertEquals(before.getStoredSurname(), after.getSurname());
        assertEquals(before.getStoredLoyalty(), after.getLoyalty());
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
        assertEquals(before.getStoredReasoningDescriptionIndex(), after.getReasoningDescriptionIndex());
    }

    @Test
    void testSwitchPersonality_singleSwitch_allFieldsStored() {
        Person before = createPersonality();

        Person after = createPersonality();
        after.switchPersonality();

        assertEquals(after.getStoredGivenName(), before.getGivenName());
        assertEquals(after.getStoredSurname(), before.getSurname());
        assertEquals(after.getStoredLoyalty(), before.getLoyalty());
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
        assertEquals(after.getStoredReasoningDescriptionIndex(), before.getReasoningDescriptionIndex());
    }

    @Test
    void testSwitchPersonality_doubleSwitch_personalityFullyRestored() {
        Person before = createPersonality();

        Person after = createPersonality();
        after.switchPersonality();
        after.switchPersonality();

        assertEquals(before.getGivenName(), after.getGivenName());
        assertEquals(before.getSurname(), after.getSurname());
        assertEquals(before.getLoyalty(), after.getLoyalty());
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
        assertEquals(before.getReasoningDescriptionIndex(), after.getReasoningDescriptionIndex());

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
        assertEquals(before.getStoredReasoningDescriptionIndex(), after.getStoredReasoningDescriptionIndex());
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
        personality.setReasoningDescriptionIndex(1);

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
        personality.setStoredReasoningDescriptionIndex(0);

        return personality;
    }

}
