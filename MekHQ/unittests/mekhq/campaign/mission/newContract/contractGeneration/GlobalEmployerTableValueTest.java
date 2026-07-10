/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.newContract.contractGeneration;

import static mekhq.campaign.mission.newContract.contractGeneration.GlobalEmployerTableValue.INDEPENDENT;
import static mekhq.campaign.mission.newContract.contractGeneration.GlobalEmployerTableValue.MAJOR_POWER;
import static mekhq.campaign.mission.newContract.contractGeneration.GlobalEmployerTableValue.MINOR_POWER;
import static mekhq.campaign.mission.newContract.contractGeneration.GlobalEmployerTableValue.SUPER_POWER;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class GlobalEmployerTableValueTest {
    // ---- Bands ------------------------------------------------------------------------------

    @Test
    public void testBands() {
        assertEquals(Integer.MIN_VALUE, INDEPENDENT.getLowerBand());
        assertEquals(5, INDEPENDENT.getUpperBand());
        assertEquals(6, MINOR_POWER.getLowerBand());
        assertEquals(7, MINOR_POWER.getUpperBand());
        assertEquals(8, MAJOR_POWER.getLowerBand());
        assertEquals(10, MAJOR_POWER.getUpperBand());
        assertEquals(11, SUPER_POWER.getLowerBand());
        assertEquals(Integer.MAX_VALUE, SUPER_POWER.getUpperBand());
    }

    @Test
    public void testIsWithinRange_Inclusive() {
        assertTrue(MINOR_POWER.isWithinRange(6));
        assertTrue(MINOR_POWER.isWithinRange(7));
    }

    @Test
    public void testIsWithinRange_OutsideBand() {
        assertFalse(MINOR_POWER.isWithinRange(5));
        assertFalse(MINOR_POWER.isWithinRange(8));
    }

    // ---- getEmployerForRoll -----------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
          "-2147483648, INDEPENDENT",
          "-100, INDEPENDENT",
          "0, INDEPENDENT",
          "5, INDEPENDENT",
          "6, MINOR_POWER",
          "7, MINOR_POWER",
          "8, MAJOR_POWER",
          "10, MAJOR_POWER",
          "11, SUPER_POWER",
          "2147483647, SUPER_POWER"
    })
    public void testGetEmployerForRoll(int roll, GlobalEmployerTableValue expected) {
        assertEquals(expected, GlobalEmployerTableValue.getEmployerForRoll(roll));
    }

    // ---- getNextLowestEmployerType ----------------------------------------------------------

    @Test
    public void testGetNextLowestEmployerType() {
        assertNull(INDEPENDENT.getNextLowestEmployerType());
        assertEquals(INDEPENDENT, MINOR_POWER.getNextLowestEmployerType());
        assertEquals(MINOR_POWER, MAJOR_POWER.getNextLowestEmployerType());
        assertEquals(MAJOR_POWER, SUPER_POWER.getNextLowestEmployerType());
    }

    // ---- getFactionTableType ----------------------------------------------------------------

    @Test
    public void testGetFactionTableType_MinorPower() {
        Faction faction = mock(Faction.class);
        when(faction.isMinorPower()).thenReturn(true);

        assertEquals(MINOR_POWER, GlobalEmployerTableValue.getFactionTableType(faction));
    }

    @Test
    public void testGetFactionTableType_MajorPower() {
        Faction faction = mock(Faction.class);
        when(faction.isMajorPower()).thenReturn(true);

        assertEquals(MAJOR_POWER, GlobalEmployerTableValue.getFactionTableType(faction));
    }

    @Test
    public void testGetFactionTableType_SuperPower() {
        Faction faction = mock(Faction.class);
        when(faction.isSuperPower()).thenReturn(true);

        assertEquals(SUPER_POWER, GlobalEmployerTableValue.getFactionTableType(faction));
    }

    @Test
    public void testGetFactionTableType_NoneMatches_DefaultsToIndependent() {
        Faction faction = mock(Faction.class);

        assertEquals(INDEPENDENT, GlobalEmployerTableValue.getFactionTableType(faction));
    }

    @Test
    public void testGetFactionTableType_MinorTakesPrecedenceOverMajor() {
        Faction faction = mock(Faction.class);
        when(faction.isMinorPower()).thenReturn(true);
        when(faction.isMajorPower()).thenReturn(true);

        assertEquals(MINOR_POWER, GlobalEmployerTableValue.getFactionTableType(faction));
    }

    // ---- fromString -------------------------------------------------------------------------

    @Test
    public void testFromString_ValidName() {
        assertEquals(SUPER_POWER, GlobalEmployerTableValue.fromString(SUPER_POWER.name()));
    }

    @Test
    public void testFromString_CaseInsensitive() {
        assertEquals(SUPER_POWER, GlobalEmployerTableValue.fromString("super_power"));
    }

    @Test
    public void testFromString_WithSpaces() {
        assertEquals(SUPER_POWER, GlobalEmployerTableValue.fromString("super power"));
    }

    @Test
    public void testFromString_FromOrdinal() {
        for (GlobalEmployerTableValue value : GlobalEmployerTableValue.values()) {
            assertEquals(value, GlobalEmployerTableValue.fromString(String.valueOf(value.ordinal())));
        }
    }

    @Test
    public void testFromString_OrdinalOutOfRange_ReturnsMajorPower() {
        assertEquals(MAJOR_POWER, GlobalEmployerTableValue.fromString("99"));
    }

    @Test
    public void testFromString_Invalid_ReturnsMajorPower() {
        assertEquals(MAJOR_POWER, GlobalEmployerTableValue.fromString("INVALID_value"));
    }

    @Test
    public void testFromString_Null_ReturnsMajorPower() {
        assertEquals(MAJOR_POWER, GlobalEmployerTableValue.fromString(null));
    }

    @Test
    public void testFromString_EmptyString_ReturnsMajorPower() {
        assertEquals(MAJOR_POWER, GlobalEmployerTableValue.fromString(""));
    }

    // ---- Resources & toString ---------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(GlobalEmployerTableValue.class)
    public void testGetLabel_notInvalid(GlobalEmployerTableValue value) {
        assertTrue(isResourceKeyValid(value.getLabel()), "Missing name resource for " + value.name());
    }

    @ParameterizedTest
    @EnumSource(GlobalEmployerTableValue.class)
    public void testGetTooltip_notInvalid(GlobalEmployerTableValue value) {
        assertTrue(isResourceKeyValid(value.getTooltip()), "Missing tooltip resource for " + value.name());
    }

    @ParameterizedTest
    @EnumSource(GlobalEmployerTableValue.class)
    public void testToString_MatchesLabel(GlobalEmployerTableValue value) {
        assertEquals(value.getLabel(), value.toString());
    }
}
