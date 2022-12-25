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

public class PrisonerStatusTest {
    //region Variable Declarations
    private static final PrisonerStatus[] statuses = PrisonerStatus.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetTitleExtension() {
        assertEquals(resources.getString("PrisonerStatus.FREE.titleExtension"),
                PrisonerStatus.FREE.getTitleExtension());
        assertEquals(resources.getString("PrisonerStatus.PRISONER_DEFECTOR.titleExtension"),
                PrisonerStatus.PRISONER_DEFECTOR.getTitleExtension());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsFree() {
        for (final PrisonerStatus prisonerStatus : statuses) {
            if (prisonerStatus == PrisonerStatus.FREE) {
                assertTrue(prisonerStatus.isFree());
            } else {
                assertFalse(prisonerStatus.isFree());
            }
        }
    }

    @Test
    public void testIsPrisoner() {
        for (final PrisonerStatus prisonerStatus : statuses) {
            if (prisonerStatus == PrisonerStatus.PRISONER) {
                assertTrue(prisonerStatus.isPrisoner());
            } else {
                assertFalse(prisonerStatus.isPrisoner());
            }
        }
    }

    @Test
    public void testIsPrisonerDefector() {
        for (final PrisonerStatus prisonerStatus : statuses) {
            if (prisonerStatus == PrisonerStatus.PRISONER_DEFECTOR) {
                assertTrue(prisonerStatus.isPrisonerDefector());
            } else {
                assertFalse(prisonerStatus.isPrisonerDefector());
            }
        }
    }

    @Test
    public void testIsBondsman() {
        for (final PrisonerStatus prisonerStatus : statuses) {
            if (prisonerStatus == PrisonerStatus.BONDSMAN) {
                assertTrue(prisonerStatus.isBondsman());
            } else {
                assertFalse(prisonerStatus.isBondsman());
            }
        }
    }

    @Test
    public void testIsCurrentPrisoner() {
        for (final PrisonerStatus prisonerStatus : statuses) {
            if ((prisonerStatus == PrisonerStatus.PRISONER)
                    || (prisonerStatus == PrisonerStatus.PRISONER_DEFECTOR)) {
                assertTrue(prisonerStatus.isCurrentPrisoner());
            } else {
                assertFalse(prisonerStatus.isCurrentPrisoner());
            }
        }
    }
    //endregion Boolean Comparison Methods

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(PrisonerStatus.FREE, PrisonerStatus.parseFromString("FREE"));
        assertEquals(PrisonerStatus.BONDSMAN, PrisonerStatus.parseFromString("BONDSMAN"));

        // Legacy Parsing
        assertEquals(PrisonerStatus.FREE, PrisonerStatus.parseFromString("0"));
        assertEquals(PrisonerStatus.PRISONER, PrisonerStatus.parseFromString("1"));
        assertEquals(PrisonerStatus.BONDSMAN, PrisonerStatus.parseFromString("2"));

        // Error Case
        assertEquals(PrisonerStatus.FREE, PrisonerStatus.parseFromString("3"));
        assertEquals(PrisonerStatus.FREE, PrisonerStatus.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("PrisonerStatus.FREE.text"), PrisonerStatus.FREE.toString());
        assertEquals(resources.getString("PrisonerStatus.BONDSMAN.text"), PrisonerStatus.BONDSMAN.toString());
    }
}
