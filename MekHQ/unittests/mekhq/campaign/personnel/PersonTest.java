package mekhq.campaign.personnel;

import megamek.common.Entity;
import megamek.common.EntityWeightClass;
import megamek.common.TechConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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

        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1", LocalDate.parse("3000-01-01"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1", LocalDate.parse("3000-01-02"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 2", LocalDate.parse("3000-01-01"));

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-01"), LocalDate.parse("3000-01-02"));

        assertTrue(mockPerson.getAwardController().hasAwards());
        assertEquals(2, mockPerson.getAwardController().getAwards().size());

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 2", LocalDate.parse("3000-01-01"), LocalDate.parse("3000-01-02"));

        assertTrue(mockPerson.getAwardController().hasAwards());
        assertEquals(1, mockPerson.getAwardController().getAwards().size());

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-02"), LocalDate.parse("3000-01-02"));

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

        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1", LocalDate.parse("3000-01-01"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 1", LocalDate.parse("3000-01-02"));
        mockPerson.getAwardController().addAndLogAward(mockCampaign, "TestSet", "Test Award 2", LocalDate.parse("3000-01-01"));

        assertEquals( 2, mockPerson.getAwardController().getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-01"), LocalDate.parse("3000-01-02"));

        assertEquals(1, mockPerson.getAwardController().getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.getAwardController().removeAward("TestSet", "Test Award 1", LocalDate.parse("3000-01-02"), LocalDate.parse("3000-01-02"));

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
        AwardsFactory.getInstance().loadAwardsFromStream(PersonnelTestUtilities.getTestAwardSet(),"TestSet");
    }
}
