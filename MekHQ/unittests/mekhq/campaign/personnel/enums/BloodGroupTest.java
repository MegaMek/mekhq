package mekhq.campaign.personnel.enums;

import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.personnel.enums.BloodGroup.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class BloodGroupTest {
    @Test
    public void testFromString_ValidStatus() {
        BloodGroup status = fromString(AO_NEGATIVE.name());
        assertEquals(AO_NEGATIVE, status);
    }

    @Test
    public void testFromString_InvalidStatus() {
        BloodGroup status = fromString("INVALID_STATUS");

        assertEquals(OO_POSITIVE, status);
    }

    @Test
    public void testFromString_NullStatus() {
        BloodGroup status = fromString(null);

        assertEquals(OO_POSITIVE, status);
    }

    @Test
    public void testFromString_EmptyString() {
        BloodGroup status = fromString("");

        assertEquals(OO_POSITIVE, status);
    }

    @Test
    public void testFromString_FromOrdinal() {
        BloodGroup status = fromString(AO_POSITIVE.ordinal() + "");

        assertEquals(AO_POSITIVE, status);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (BloodGroup status : values()) {
            String label = status.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testGetInherentedBloodGroup_AAandBB_RhPositive() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(AA_POSITIVE.getAlleles())).thenReturn(Allele.A);
            mockedUtility.when(() -> getRandomItem(BB_POSITIVE.getAlleles())).thenReturn(Allele.B);

            BloodGroup result = getInherentedBloodGroup(AA_POSITIVE, BB_POSITIVE);

            assertEquals(AB_POSITIVE, result);
        }
    }

    @Test
    public void testGetInherentedBloodGroup_BOandAO_RhNegative() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(BO_NEGATIVE.getAlleles())).thenReturn(Allele.B);
            mockedUtility.when(() -> getRandomItem(AO_NEGATIVE.getAlleles())).thenReturn(Allele.O);

            BloodGroup result = getInherentedBloodGroup(BO_NEGATIVE, AO_NEGATIVE);

            assertEquals(BO_NEGATIVE, result);
        }
    }

    @Test
    public void testGetInherentedBloodGroup_OOandAA_MixedRhFactor() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(OO_NEGATIVE.getAlleles())).thenReturn(Allele.O);
            mockedUtility.when(() -> getRandomItem(AA_POSITIVE.getAlleles())).thenReturn(Allele.A);

            BloodGroup result = getInherentedBloodGroup(OO_NEGATIVE, AA_POSITIVE);

            assertEquals(AO_POSITIVE, result);
        }
    }

    @Test
    public void testGetInherentedBloodGroup_OOandOO_UniveralDonor() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(OO_NEGATIVE.getAlleles())).thenReturn(Allele.O);
            mockedUtility.when(() -> getRandomItem(OO_POSITIVE.getAlleles())).thenReturn(Allele.O);

            BloodGroup result = getInherentedBloodGroup(OO_NEGATIVE, OO_POSITIVE);

            assertEquals(OO_POSITIVE, result);
        }
    }
}
