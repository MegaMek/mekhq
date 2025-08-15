/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import org.junit.jupiter.api.Test;

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
