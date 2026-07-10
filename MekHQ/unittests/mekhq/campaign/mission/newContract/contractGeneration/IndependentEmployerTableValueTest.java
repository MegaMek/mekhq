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

import static mekhq.campaign.mission.newContract.contractGeneration.IndependentEmployerTableValue.CORPORATION;
import static mekhq.campaign.mission.newContract.contractGeneration.IndependentEmployerTableValue.MAJOR_PERIPHERY;
import static mekhq.campaign.mission.newContract.contractGeneration.IndependentEmployerTableValue.MERCENARY;
import static mekhq.campaign.mission.newContract.contractGeneration.IndependentEmployerTableValue.MINOR_PERIPHERY;
import static mekhq.campaign.mission.newContract.contractGeneration.IndependentEmployerTableValue.NOBLE;
import static mekhq.campaign.mission.newContract.contractGeneration.IndependentEmployerTableValue.PLANETARY_GOVERNMENT;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class IndependentEmployerTableValueTest {
    // ---- Bands ------------------------------------------------------------------------------

    @Test
    public void testBands() {
        assertEquals(Integer.MIN_VALUE, NOBLE.getLowerBand());
        assertEquals(3, NOBLE.getUpperBand());
        assertEquals(4, PLANETARY_GOVERNMENT.getLowerBand());
        assertEquals(5, PLANETARY_GOVERNMENT.getUpperBand());
        assertEquals(6, MERCENARY.getLowerBand());
        assertEquals(6, MERCENARY.getUpperBand());
        assertEquals(7, MAJOR_PERIPHERY.getLowerBand());
        assertEquals(8, MAJOR_PERIPHERY.getUpperBand());
        assertEquals(9, MINOR_PERIPHERY.getLowerBand());
        assertEquals(10, MINOR_PERIPHERY.getUpperBand());
        assertEquals(11, CORPORATION.getLowerBand());
        assertEquals(Integer.MAX_VALUE, CORPORATION.getUpperBand());
    }

    @Test
    public void testIsWithinRange_SingleValueBand() {
        assertTrue(MERCENARY.isWithinRange(6));
        assertFalse(MERCENARY.isWithinRange(5));
        assertFalse(MERCENARY.isWithinRange(7));
    }

    @Test
    public void testIsWithinRange_Inclusive() {
        assertTrue(MAJOR_PERIPHERY.isWithinRange(7));
        assertTrue(MAJOR_PERIPHERY.isWithinRange(8));
        assertFalse(MAJOR_PERIPHERY.isWithinRange(6));
        assertFalse(MAJOR_PERIPHERY.isWithinRange(9));
    }

    // ---- getEmployerForRoll -----------------------------------------------------------------

    @ParameterizedTest
    @CsvSource({
          "-2147483648, NOBLE",
          "0, NOBLE",
          "3, NOBLE",
          "4, PLANETARY_GOVERNMENT",
          "5, PLANETARY_GOVERNMENT",
          "6, MERCENARY",
          "7, MAJOR_PERIPHERY",
          "8, MAJOR_PERIPHERY",
          "9, MINOR_PERIPHERY",
          "10, MINOR_PERIPHERY",
          "11, CORPORATION",
          "2147483647, CORPORATION"
    })
    public void testGetEmployerForRoll(int roll, IndependentEmployerTableValue expected) {
        assertEquals(expected, IndependentEmployerTableValue.getEmployerForRoll(roll));
    }

    // ---- fromString -------------------------------------------------------------------------

    @Test
    public void testFromString_ValidName() {
        assertEquals(CORPORATION, IndependentEmployerTableValue.fromString(CORPORATION.name()));
    }

    @Test
    public void testFromString_CaseInsensitive() {
        assertEquals(MAJOR_PERIPHERY, IndependentEmployerTableValue.fromString("major_periphery"));
    }

    @Test
    public void testFromString_WithSpaces() {
        assertEquals(MAJOR_PERIPHERY, IndependentEmployerTableValue.fromString("major periphery"));
    }

    @Test
    public void testFromString_FromOrdinal() {
        for (IndependentEmployerTableValue value : IndependentEmployerTableValue.values()) {
            assertEquals(value, IndependentEmployerTableValue.fromString(String.valueOf(value.ordinal())));
        }
    }

    @Test
    public void testFromString_OrdinalOutOfRange_ReturnsPlanetaryGovernment() {
        assertEquals(PLANETARY_GOVERNMENT, IndependentEmployerTableValue.fromString("99"));
    }

    @Test
    public void testFromString_Invalid_ReturnsPlanetaryGovernment() {
        assertEquals(PLANETARY_GOVERNMENT, IndependentEmployerTableValue.fromString("INVALID_value"));
    }

    @Test
    public void testFromString_Null_ReturnsPlanetaryGovernment() {
        assertEquals(PLANETARY_GOVERNMENT, IndependentEmployerTableValue.fromString(null));
    }

    @Test
    public void testFromString_EmptyString_ReturnsPlanetaryGovernment() {
        assertEquals(PLANETARY_GOVERNMENT, IndependentEmployerTableValue.fromString(""));
    }

    // ---- Resources & toString ---------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(IndependentEmployerTableValue.class)
    public void testGetLabel_notInvalid(IndependentEmployerTableValue value) {
        assertTrue(isResourceKeyValid(value.getLabel()), "Missing name resource for " + value.name());
    }

    @ParameterizedTest
    @EnumSource(IndependentEmployerTableValue.class)
    public void testGetTooltip_notInvalid(IndependentEmployerTableValue value) {
        assertTrue(isResourceKeyValid(value.getTooltip()), "Missing tooltip resource for " + value.name());
    }

    @ParameterizedTest
    @EnumSource(IndependentEmployerTableValue.class)
    public void testToString_MatchesLabel(IndependentEmployerTableValue value) {
        assertEquals(value.getLabel(), value.toString());
    }
}
