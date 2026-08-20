/*
 * Copyright (C) 2022-2026 The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.procreation.DisabledRandomProcreation;
import mekhq.campaign.personnel.procreation.RandomProcreation;
import org.junit.jupiter.api.Test;

public class RandomProcreationMethodTest {
    //region Variable Declarations
    private static final RandomProcreationMethod[] methods = RandomProcreationMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("RandomProcreationMethod.NONE.toolTipText"),
              RandomProcreationMethod.NONE.getToolTipText());
        assertEquals(resources.getString("RandomProcreationMethod.DICE_ROLL.toolTipText"),
              RandomProcreationMethod.DICE_ROLL.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomProcreationMethod randomProcreationMethod : methods) {
            if (randomProcreationMethod == RandomProcreationMethod.NONE) {
                assertTrue(randomProcreationMethod.isNone());
            } else {
                assertFalse(randomProcreationMethod.isNone());
            }
        }
    }

    @Test
    public void testIsPercentage() {
        for (final RandomProcreationMethod randomProcreationMethod : methods) {
            if (randomProcreationMethod == RandomProcreationMethod.DICE_ROLL) {
                assertTrue(randomProcreationMethod.isDiceRoll());
            } else {
                assertFalse(randomProcreationMethod.isDiceRoll());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_PRISONER_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION)).thenReturn(false);
        when(mockOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE)).thenReturn(5);
        when(mockOptions.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE)).thenReturn(5);

        assertInstanceOf(DisabledRandomProcreation.class, RandomProcreationMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(RandomProcreation.class, RandomProcreationMethod.DICE_ROLL.getMethod(mockOptions));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomProcreationMethod.NONE.text"), RandomProcreationMethod.NONE.toString());
        assertEquals(resources.getString("RandomProcreationMethod.DICE_ROLL.text"),
              RandomProcreationMethod.DICE_ROLL.toString());
    }

    @Test
    public void testFromString() {
        assertEquals(RandomProcreationMethod.NONE, RandomProcreationMethod.fromString("NONE"));
        assertEquals(RandomProcreationMethod.DICE_ROLL, RandomProcreationMethod.fromString("DICE_ROLL"));
        assertEquals(RandomProcreationMethod.NONE, RandomProcreationMethod.fromString("none"));
        assertEquals(RandomProcreationMethod.DICE_ROLL, RandomProcreationMethod.fromString("dice_roll"));
    }

    @Test
    public void testFromStringInvalidInput() {
        assertEquals(RandomProcreationMethod.NONE, RandomProcreationMethod.fromString(null));
        assertEquals(RandomProcreationMethod.NONE, RandomProcreationMethod.fromString(""));
        assertEquals(RandomProcreationMethod.NONE, RandomProcreationMethod.fromString("INVALID_VALUE"));
        assertEquals(RandomProcreationMethod.NONE, RandomProcreationMethod.fromString("123"));
    }
}
