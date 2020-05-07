package mekhq.campaign.personnel.enums;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ResourceBundle;

import megamek.common.util.EncodeControl;

public class ManeiDominiClassTest {
    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            new EncodeControl());

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ManeiDominiClass.NONE.text"), ManeiDominiClass.NONE.toString());
        assertEquals(resources.getString("ManeiDominiClass.PHANTOM.text"), ManeiDominiClass.PHANTOM.toString());
        assertEquals(resources.getString("ManeiDominiClass.POLTERGEIST.text"), ManeiDominiClass.POLTERGEIST.toString());
    }

    /**
     * Testing to ensure the enum is properly parsed from a given String, dependant on whether it is
     * is parsing from ManeiDominiClass.name(), the ordinal (formerly magic numbers), or a failure
     * condition
     */
    @Test
    public void testParseFromString() {
        // Enum.valueOf Testing
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("NONE"));
        assertEquals(ManeiDominiClass.GHOST, ManeiDominiClass.parseFromString("GHOST"));

        // Parsing from ordinal testing
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("0"));
        assertEquals(ManeiDominiClass.BANSHEE, ManeiDominiClass.parseFromString("3"));
        assertEquals(ManeiDominiClass.POLTERGEIST, ManeiDominiClass.parseFromString("7"));
        // This is an out of bounds check, as any future additions (albeit highly improbably)
        // must adjust for the fact that the old ordinal numbers only went up to 7
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("8"));

        // Default Failure Case
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("failureFailsFake"));
    }
}
