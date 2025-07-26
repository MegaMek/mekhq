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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.divorce.DisabledRandomDivorce;
import mekhq.campaign.personnel.divorce.RandomDivorce;
import org.junit.jupiter.api.Test;

public class RandomDivorceMethodTest {
    //region Variable Declarations
    private static final RandomDivorceMethod[] methods = RandomDivorceMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        String expected = resources.getString("RandomDivorceMethod.NONE.toolTipText").replaceAll("\\s", "");
        String actual = RandomDivorceMethod.NONE.getToolTipText().trim().replaceAll("\\s", "");

        assertEquals(expected, actual);
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomDivorceMethod randomDivorceMethod : methods) {
            if (randomDivorceMethod == RandomDivorceMethod.NONE) {
                assertTrue(randomDivorceMethod.isNone());
            } else {
                assertFalse(randomDivorceMethod.isNone());
            }
        }
    }

    @Test
    public void testIsDiceRoll() {
        for (final RandomDivorceMethod randomDivorceMethod : methods) {
            if (randomDivorceMethod == RandomDivorceMethod.DICE_ROLL) {
                assertTrue(randomDivorceMethod.isDiceRoll());
            } else {
                assertFalse(randomDivorceMethod.isDiceRoll());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.isUseClanPersonnelDivorce()).thenReturn(false);
        when(mockOptions.isUsePrisonerDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomOppositeSexDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomSameSexDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomClanPersonnelDivorce()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerDivorce()).thenReturn(false);
        when(mockOptions.getRandomDivorceDiceSize()).thenReturn(5);

        assertInstanceOf(DisabledRandomDivorce.class, RandomDivorceMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(RandomDivorce.class, RandomDivorceMethod.DICE_ROLL.getMethod(mockOptions));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomDivorceMethod.NONE.text"), RandomDivorceMethod.NONE.toString());
        assertEquals(resources.getString("RandomDivorceMethod.DICE_ROLL.text"),
              RandomDivorceMethod.DICE_ROLL.toString());
    }

    @Test
    public void testFromStringValidEnums() {
        assertEquals(RandomDivorceMethod.NONE, RandomDivorceMethod.fromString("NONE"));
        assertEquals(RandomDivorceMethod.DICE_ROLL, RandomDivorceMethod.fromString("DICE_ROLL"));
        assertEquals(RandomDivorceMethod.DICE_ROLL,
              RandomDivorceMethod.fromString("dice roll")); // Case and space insensitive
    }

    @Test
    public void testFromStringInvalidEnums() {
        assertEquals(RandomDivorceMethod.NONE, RandomDivorceMethod.fromString(null));
        assertEquals(RandomDivorceMethod.NONE, RandomDivorceMethod.fromString(""));
        assertEquals(RandomDivorceMethod.NONE, RandomDivorceMethod.fromString("InvalidMethod"));
    }

    @Test
    public void testFromStringOrdinalNumbers() {
        assertEquals(RandomDivorceMethod.NONE, RandomDivorceMethod.fromString("0")); // Ordinal mapping
        assertEquals(RandomDivorceMethod.DICE_ROLL, RandomDivorceMethod.fromString("1"));
        assertEquals(RandomDivorceMethod.NONE,
              RandomDivorceMethod.fromString("2")); // Non-existent ordinal returns default
    }
}
