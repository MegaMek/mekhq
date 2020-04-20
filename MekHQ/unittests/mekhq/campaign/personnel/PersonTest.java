package mekhq.campaign.personnel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

import org.junit.Test;
import org.mockito.Mockito;

import megamek.common.Entity;
import mekhq.campaign.unit.Unit;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.UUID;

public class PersonTest {
    private Person mockPerson;

    @Test
    public void testIsCombatRoleReturnsTrueAtEdgesOfRange() {
        assertTrue(Person.isCombatRole(Person.T_MECHWARRIOR));
        assertTrue(Person.isCombatRole(Person.T_NAVIGATOR));
    }

    @Test
    public void testIsCombatRoleReturnsOutsideRange() {
        assertFalse(Person.isCombatRole(Person.T_NONE));
        assertFalse(Person.isCombatRole(Person.T_MECH_TECH));
        assertFalse(Person.isCombatRole(Person.T_NUM));
        assertFalse(Person.isCombatRole(Person.T_LAM_PILOT));
        assertFalse(Person.isCombatRole(-1));
    }

    @Test
    public void testIsSupportRoleReturnsTrueAtEdgesOfRange() {
        assertTrue(Person.isSupportRole(Person.T_MECH_TECH));
        assertTrue(Person.isSupportRole(Person.T_ADMIN_HR));
    }

    @Test
    public void testIsSupportRoleReturnsOutsideRange() {
        assertFalse(Person.isSupportRole(Person.T_NONE));
        assertFalse(Person.isSupportRole(Person.T_NAVIGATOR));
        assertFalse(Person.isSupportRole(Person.T_NUM));
        assertFalse(Person.isSupportRole(Person.T_LAM_PILOT));
        assertFalse(Person.isSupportRole(-1));
    }

    @Test
    public void testAddAndRemoveAward() throws ParseException {
        initPerson();
        initAwards();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", simpleDateFormat.parse("3000-01-01"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", simpleDateFormat.parse("3000-01-02"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 2", simpleDateFormat.parse("3000-01-01"));

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", simpleDateFormat.parse("3000-01-01"), simpleDateFormat.parse("3000-01-02"));

        assertTrue(mockPerson.awardController.hasAwards());
        assertEquals(2, mockPerson.awardController.getAwards().size());

        mockPerson.awardController.removeAward("TestSet", "Test Award 2", simpleDateFormat.parse("3000-01-01"), simpleDateFormat.parse("3000-01-02"));

        assertTrue(mockPerson.awardController.hasAwards());
        assertEquals(1, mockPerson.awardController.getAwards().size());

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", simpleDateFormat.parse("3000-01-02"), simpleDateFormat.parse("3000-01-02"));

        assertFalse(mockPerson.awardController.hasAwards());
        assertEquals(0, mockPerson.awardController.getAwards().size());
    }

    @Test
    public void testGetNumberOfAwards() throws ParseException {
        initPerson();
        initAwards();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", simpleDateFormat.parse("3000-01-01"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", simpleDateFormat.parse("3000-01-02"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 2", simpleDateFormat.parse("3000-01-01"));

        assertEquals( 2, mockPerson.awardController.getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", simpleDateFormat.parse("3000-01-01"), simpleDateFormat.parse("3000-01-02"));

        assertEquals(1, mockPerson.awardController.getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", simpleDateFormat.parse("3000-01-02"), simpleDateFormat.parse("3000-01-02"));

        assertEquals(0, mockPerson.awardController.getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));
    }

    @Test
    public void testSetOriginalUnit() {
        initPerson();

        UUID is1Id = UUID.randomUUID();
        int is1WeightClass = megamek.common.EntityWeightClass.WEIGHT_LIGHT;

        Unit is1 = Mockito.mock(Unit.class);
        Mockito.when(is1.getId()).thenReturn(is1Id);

        Entity is1Entity = Mockito.mock(Entity.class);
        Mockito.when(is1Entity.isClan()).thenReturn(false);
        Mockito.when(is1Entity.getTechLevel()).thenReturn(megamek.common.TechConstants.T_INTRO_BOXSET);
        Mockito.when(is1Entity.getWeightClass()).thenReturn(is1WeightClass);
        Mockito.when(is1.getEntity()).thenReturn(is1Entity);

        mockPerson.setOriginalUnit(is1);
        assertEquals(Person.TECH_IS1, mockPerson.getOriginalUnitTech());
        assertEquals(is1WeightClass, mockPerson.getOriginalUnitWeight());
        assertEquals(is1Id, mockPerson.getOriginalUnitId());

        int[] is2Techs = new int[] {
            megamek.common.TechConstants.T_IS_TW_NON_BOX,
            megamek.common.TechConstants.T_IS_TW_ALL,
            megamek.common.TechConstants.T_IS_ADVANCED,
            megamek.common.TechConstants.T_IS_EXPERIMENTAL,
            megamek.common.TechConstants.T_IS_UNOFFICIAL,
        };
        for (int is2TechLevel : is2Techs) {
            UUID is2Id = UUID.randomUUID();
            int is2WeightClass = megamek.common.EntityWeightClass.WEIGHT_HEAVY;

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
            megamek.common.TechConstants.T_CLAN_TW,
            megamek.common.TechConstants.T_CLAN_ADVANCED,
            megamek.common.TechConstants.T_CLAN_EXPERIMENTAL,
            megamek.common.TechConstants.T_CLAN_UNOFFICIAL,
        };
        for (int clanTech : clanTechs) {
            UUID clanId = UUID.randomUUID();
            int clanWeightClass = megamek.common.EntityWeightClass.WEIGHT_MEDIUM;

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

    private void initPerson(){
        mockPerson = spy(new Person("TestGivenName", "TestSurname", null, "MERC"));
    }

    private void initAwards(){
        AwardsFactory.getInstance().loadAwardsFromStream(PersonnelTestUtilities.getTestAwardSet(),"TestSet");
    }
}
