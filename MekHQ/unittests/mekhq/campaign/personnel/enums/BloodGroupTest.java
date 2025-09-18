/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.enums;

import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static mekhq.campaign.personnel.enums.BloodGroup.*;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import megamek.codeUtilities.ObjectUtility;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class BloodGroupTest {
    @Test
    public void testFromString_ValidStatus() {
        BloodGroup bloodGroup = fromString(AO_NEGATIVE.name());
        assertEquals(AO_NEGATIVE, bloodGroup);
    }

    @Test
    public void testFromString_InvalidStatus() {
        BloodGroup bloodGroup = fromString("INVALID_STATUS");

        assertEquals(OO_POSITIVE, bloodGroup);
    }

    @Test
    public void testFromString_NullStatus() {
        BloodGroup bloodGroup = fromString(null);

        assertEquals(OO_POSITIVE, bloodGroup);
    }

    @Test
    public void testFromString_EmptyString() {
        BloodGroup bloodGroup = fromString("");

        assertEquals(OO_POSITIVE, bloodGroup);
    }

    @Test
    public void testFromString_FromOrdinal() {
        BloodGroup bloodGroup = fromString(AO_POSITIVE.ordinal() + "");

        assertEquals(AO_POSITIVE, bloodGroup);
    }

    @Test
    public void testGetLabel_notInvalid() {
        for (BloodGroup bloodGroup : values()) {
            String label = bloodGroup.getLabel();
            assertTrue(isResourceKeyValid(label));
        }
    }

    @Test
    public void testCumulativeChance() {
        int cumulativeChance = 0;

        for (BloodGroup bloodGroup : values()) {
            cumulativeChance += bloodGroup.getChance();
        }

        assertEquals(100, cumulativeChance);
    }

    @Test
    public void testGetInheritedBloodGroup_AAandBB_RhPositive() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(AA_POSITIVE.getAlleles())).thenReturn(Allele.A);
            mockedUtility.when(() -> getRandomItem(BB_POSITIVE.getAlleles())).thenReturn(Allele.B);

            BloodGroup result = getInheritedBloodGroup(AA_POSITIVE, BB_POSITIVE);

            assertEquals(AB_POSITIVE, result);
        }
    }

    @Test
    public void testGetInheritedBloodGroup_BOandAO_RhNegative() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(BO_NEGATIVE.getAlleles())).thenReturn(Allele.B);
            mockedUtility.when(() -> getRandomItem(AO_NEGATIVE.getAlleles())).thenReturn(Allele.O);

            BloodGroup result = getInheritedBloodGroup(BO_NEGATIVE, AO_NEGATIVE);

            assertEquals(BO_NEGATIVE, result);
        }
    }

    @Test
    public void testGetInheritedBloodGroup_OOandAA_MixedRhFactor() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(OO_NEGATIVE.getAlleles())).thenReturn(Allele.O);
            mockedUtility.when(() -> getRandomItem(AA_POSITIVE.getAlleles())).thenReturn(Allele.A);

            BloodGroup result = getInheritedBloodGroup(OO_NEGATIVE, AA_POSITIVE);

            assertEquals(AO_POSITIVE, result);
        }
    }

    @Test
    public void testGetInheritedBloodGroup_OOandOO_UniveralDonor() {
        try (MockedStatic<ObjectUtility> mockedUtility = mockStatic(ObjectUtility.class)) {
            mockedUtility.when(() -> getRandomItem(OO_NEGATIVE.getAlleles())).thenReturn(Allele.O);
            mockedUtility.when(() -> getRandomItem(OO_POSITIVE.getAlleles())).thenReturn(Allele.O);

            BloodGroup result = getInheritedBloodGroup(OO_NEGATIVE, OO_POSITIVE);

            assertEquals(OO_POSITIVE, result);
        }
    }
}
