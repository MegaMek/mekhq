/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.enums;

import megamek.common.enums.Gender;
import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GenderDescriptorsTest {
    //region Variable Declarations
    private static final GenderDescriptors[] genderDescriptors = GenderDescriptors.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Boolean Comparison Methods
    @Test
    public void testIsMaleFemaleOther() {
        for (final GenderDescriptors descriptors : genderDescriptors) {
            if (descriptors == GenderDescriptors.MALE_FEMALE_OTHER) {
                assertTrue(descriptors.isMaleFemaleOther());
            } else {
                assertFalse(descriptors.isMaleFemaleOther());
            }
        }
    }

    @Test
    public void testIsHeSheThey() {
        for (final GenderDescriptors descriptors : genderDescriptors) {
            if (descriptors == GenderDescriptors.HE_SHE_THEY) {
                assertTrue(descriptors.isHeSheThey());
            } else {
                assertFalse(descriptors.isHeSheThey());
            }
        }
    }

    @Test
    public void testIsHeHerHim() {
        for (final GenderDescriptors descriptors : genderDescriptors) {
            if (descriptors == GenderDescriptors.HIM_HER_THEM) {
                assertTrue(descriptors.isHimHerThem());
            } else {
                assertFalse(descriptors.isHimHerThem());
            }
        }
    }

    @Test
    public void testIsHisHerTheir() {
        for (final GenderDescriptors descriptors : genderDescriptors) {
            if (descriptors == GenderDescriptors.HIS_HER_THEIR) {
                assertTrue(descriptors.isHisHerTheir());
            } else {
                assertFalse(descriptors.isHisHerTheir());
            }
        }
    }

    @Test
    public void testIsHisHersTheirs() {
        for (final GenderDescriptors descriptors : genderDescriptors) {
            if (descriptors == GenderDescriptors.HIS_HERS_THEIRS) {
                assertTrue(descriptors.isHisHersTheirs());
            } else {
                assertFalse(descriptors.isHisHersTheirs());
            }
        }
    }

    @Test
    public void testIsBoyGirl() {
        for (final GenderDescriptors descriptors : genderDescriptors) {
            if (descriptors == GenderDescriptors.BOY_GIRL) {
                assertTrue(descriptors.isBoyGirl());
            } else {
                assertFalse(descriptors.isBoyGirl());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetDescriptor() {
        assertEquals(resources.getString("GenderDescriptors.HIM.text"),
                GenderDescriptors.HIM_HER_THEM.getDescriptor(Gender.MALE));
        assertEquals(resources.getString("GenderDescriptors.SHE.text"),
                GenderDescriptors.HE_SHE_THEY.getDescriptor(Gender.FEMALE));
        assertEquals(resources.getString("GenderDescriptors.THEIR.text"),
                GenderDescriptors.HIS_HER_THEIR.getDescriptor(Gender.OTHER_MALE));
        assertEquals(resources.getString("GenderDescriptors.THEIRS.text"),
                GenderDescriptors.HIS_HERS_THEIRS.getDescriptor(Gender.OTHER_FEMALE));
        assertEquals(resources.getString("GenderDescriptors.BOY.text"),
                GenderDescriptors.BOY_GIRL.getDescriptor(Gender.OTHER_MALE));
        assertEquals(resources.getString("GenderDescriptors.GIRL.text"),
                GenderDescriptors.BOY_GIRL.getDescriptor(Gender.OTHER_FEMALE));
    }

    @Test
    public void testGetDescriptorCapitalized() {
        // Test Capitalization
        final String expected = resources.getString("GenderDescriptors.HIS.text").substring(0, 1).toUpperCase()
                + resources.getString("GenderDescriptors.HIS.text").substring(1);
        assertEquals(expected, GenderDescriptors.HIS_HERS_THEIRS.getDescriptorCapitalized(Gender.MALE));

        // Test Empty Return - Only possible with BOY_GIRL and Gender.RANDOMIZE
        assertEquals("", GenderDescriptors.BOY_GIRL.getDescriptorCapitalized(Gender.RANDOMIZE));
    }
}
