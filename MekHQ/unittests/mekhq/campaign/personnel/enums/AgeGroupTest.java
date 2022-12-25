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

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AgeGroupTest {
    //region Variable Declarations
    private static final AgeGroup[] ageGroups = AgeGroup.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("AgeGroup.ELDER.toolTipText"), AgeGroup.ELDER.getToolTipText());
        assertEquals(resources.getString("AgeGroup.BABY.toolTipText"), AgeGroup.BABY.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsElder() {
        for (final AgeGroup ageGroup : ageGroups) {
            if (ageGroup == AgeGroup.ELDER) {
                assertTrue(ageGroup.isElder());
            } else {
                assertFalse(ageGroup.isElder());
            }
        }
    }

    @Test
    public void testIsAdult() {
        for (final AgeGroup ageGroup : ageGroups) {
            if (ageGroup == AgeGroup.ADULT) {
                assertTrue(ageGroup.isAdult());
            } else {
                assertFalse(ageGroup.isAdult());
            }
        }
    }

    @Test
    public void testIsTeenager() {
        for (final AgeGroup ageGroup : ageGroups) {
            if (ageGroup == AgeGroup.TEENAGER) {
                assertTrue(ageGroup.isTeenager());
            } else {
                assertFalse(ageGroup.isTeenager());
            }
        }
    }

    @Test
    public void testIsPreteen() {
        for (final AgeGroup ageGroup : ageGroups) {
            if (ageGroup == AgeGroup.PRETEEN) {
                assertTrue(ageGroup.isPreteen());
            } else {
                assertFalse(ageGroup.isPreteen());
            }
        }
    }

    @Test
    public void testIsChild() {
        for (final AgeGroup ageGroup : ageGroups) {
            if (ageGroup == AgeGroup.CHILD) {
                assertTrue(ageGroup.isChild());
            } else {
                assertFalse(ageGroup.isChild());
            }
        }
    }

    @Test
    public void testIsToddler() {
        for (final AgeGroup ageGroup : ageGroups) {
            if (ageGroup == AgeGroup.TODDLER) {
                assertTrue(ageGroup.isToddler());
            } else {
                assertFalse(ageGroup.isToddler());
            }
        }
    }

    @Test
    public void testIsBaby() {
        for (final AgeGroup ageGroup : ageGroups) {
            if (ageGroup == AgeGroup.BABY) {
                assertTrue(ageGroup.isBaby());
            } else {
                assertFalse(ageGroup.isBaby());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testDetermineAgeGroup() {
        assertEquals(AgeGroup.ADULT, AgeGroup.determineAgeGroup(-2));
        assertEquals(AgeGroup.BABY, AgeGroup.determineAgeGroup(-1));
        assertEquals(AgeGroup.BABY, AgeGroup.determineAgeGroup(0));
        assertEquals(AgeGroup.PRETEEN, AgeGroup.determineAgeGroup(12));
        assertEquals(AgeGroup.TEENAGER, AgeGroup.determineAgeGroup(13));
        assertEquals(AgeGroup.ELDER, AgeGroup.determineAgeGroup(1000));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("AgeGroup.ELDER.text"), AgeGroup.ELDER.toString());
        assertEquals(resources.getString("AgeGroup.BABY.text"), AgeGroup.BABY.toString());
    }
}
