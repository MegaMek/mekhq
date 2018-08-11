package mekhq.campaign.personnel;

import static org.junit.Assert.*;

import org.junit.Test;

public class PersonTest {

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

}
