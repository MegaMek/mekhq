package mekhq.campaign.market.personnelMarket.enums;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

public class PersonnelMarketStyleTest {
    @ParameterizedTest
    @CsvSource(value = { "MEKHQ,MEKHQ", "CAMPAIGN_OPERATIONS,CAMPAIGN_OPERATIONS", "mekhq,MEKHQ",
                         "campaign_operations,CAMPAIGN_OPERATIONS", "'CAMPAIGN OPERATIONS',CAMPAIGN_OPERATIONS",
                         "2,CAMPAIGN_OPERATIONS", "'InvalidValue',NONE", "'-1',NONE" })
    void testFromString_Parameterized(String input, PersonnelMarketStyle expected) {
        assertEquals(expected, PersonnelMarketStyle.fromString(input));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void testFromString_NullOrEmpty(String input) {
        assertEquals(PersonnelMarketStyle.NONE, PersonnelMarketStyle.fromString(input));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelMarketStyle.class)
    void testToString_notInvalid(PersonnelMarketStyle status) {
        String label = status.toString();
        assertTrue(isResourceKeyValid(label));
    }
}
