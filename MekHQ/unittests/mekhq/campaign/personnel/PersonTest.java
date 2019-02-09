package mekhq.campaign.personnel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.spy;

import mekhq.campaign.finances.Money;
import org.joda.money.CurrencyUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*"})
@PrepareForTest(Money.class)
public class PersonTest {
    private Person mockPerson;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Money.class);
        Money zero = Money.zero(CurrencyUnit.USD);
        Money amount = Money.of(2500, CurrencyUnit.USD);
        Mockito.when(Money.zero()).thenReturn(zero);
        Mockito.when(Money.of(Mockito.anyDouble())).thenReturn(amount);
    }

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

        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-01"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-02"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 2", new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-01"));

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", "3000-01-01");

        assertTrue(mockPerson.awardController.hasAwards());
        assertEquals(2, mockPerson.awardController.getAwards().size());

        mockPerson.awardController.removeAward("TestSet", "Test Award 2", "3000-01-01");

        assertTrue(mockPerson.awardController.hasAwards());
        assertEquals(1, mockPerson.awardController.getAwards().size());

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", "3000-01-02");

        assertFalse(mockPerson.awardController.hasAwards());
        assertEquals(0, mockPerson.awardController.getAwards().size());
    }

    @Test
    public void testGetNumberOfAwards() throws ParseException {
        initPerson();
        initAwards();

        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-01"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 1", new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-02"));
        mockPerson.awardController.addAndLogAward("TestSet","Test Award 2", new SimpleDateFormat("yyyy-MM-dd").parse("3000-01-01"));

        assertEquals( 2, mockPerson.awardController.getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", "3000-01-01");

        assertEquals(1, mockPerson.awardController.getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));

        mockPerson.awardController.removeAward("TestSet", "Test Award 1", "3000-01-02");

        assertEquals(0, mockPerson.awardController.getNumberOfAwards(PersonnelTestUtilities.getTestAward1()));
    }

    private void initPerson(){
        mockPerson = spy(new Person("Test", null, "MERC"));
    }

    private void initAwards(){
        AwardsFactory.getInstance().loadAwardsFromStream(PersonnelTestUtilities.getTestAwardSet(),"TestSet");
    }
}
