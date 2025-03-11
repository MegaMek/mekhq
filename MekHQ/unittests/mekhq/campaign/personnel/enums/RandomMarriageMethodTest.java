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
 */
package mekhq.campaign.personnel.enums;

import mekhq.MekHQ;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.marriage.DisabledRandomMarriage;
import mekhq.campaign.personnel.marriage.RandomMarriage;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RandomMarriageMethodTest {
    //region Variable Declarations
    private static final RandomMarriageMethod[] methods = RandomMarriageMethod.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(wordWrap(resources.getString("RandomMarriageMethod.NONE.toolTipText")),
                RandomMarriageMethod.NONE.getToolTipText());
        assertEquals(wordWrap(resources.getString("RandomMarriageMethod.DICE_ROLL.toolTipText")),
                RandomMarriageMethod.DICE_ROLL.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsNone() {
        for (final RandomMarriageMethod randomMarriageMethod : methods) {
            if (randomMarriageMethod == RandomMarriageMethod.NONE) {
                assertTrue(randomMarriageMethod.isNone());
            } else {
                assertFalse(randomMarriageMethod.isNone());
            }
        }
    }

    @Test
    public void testIsPercentage() {
        for (final RandomMarriageMethod randomMarriageMethod : methods) {
            if (randomMarriageMethod == RandomMarriageMethod.DICE_ROLL) {
                assertTrue(randomMarriageMethod.isDiceRoll());
            } else {
                assertFalse(randomMarriageMethod.isDiceRoll());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetMethod() {
        final CampaignOptions mockOptions = mock(CampaignOptions.class);
        when(mockOptions.isUseClanPersonnelMarriages()).thenReturn(false);
        when(mockOptions.isUsePrisonerMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomClanPersonnelMarriages()).thenReturn(false);
        when(mockOptions.isUseRandomPrisonerMarriages()).thenReturn(false);
        when(mockOptions.getRandomMarriageDiceSize()).thenReturn(5);

        assertInstanceOf(DisabledRandomMarriage.class, RandomMarriageMethod.NONE.getMethod(mockOptions));
        assertInstanceOf(RandomMarriage.class, RandomMarriageMethod.DICE_ROLL.getMethod(mockOptions));
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("RandomMarriageMethod.NONE.text").trim(),
                RandomMarriageMethod.NONE.toString().trim());
        assertEquals(resources.getString("RandomMarriageMethod.DICE_ROLL.text").trim(),
                RandomMarriageMethod.DICE_ROLL.toString().trim());
    }
}
