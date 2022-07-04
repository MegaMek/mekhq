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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SplittingSurnameStyleTest {
    //region Variable Declarations
    private static final SplittingSurnameStyle[] styles = SplittingSurnameStyle.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.toolTipText"),
                SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.getToolTipText());
        assertEquals(resources.getString("SplittingSurnameStyle.WEIGHTED.toolTipText"),
                SplittingSurnameStyle.WEIGHTED.getToolTipText());

    }

    @Test
    public void testGetDropDownText() {
        assertEquals(resources.getString("SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.dropDownText"),
                SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.getDropDownText());
        assertEquals(resources.getString("SplittingSurnameStyle.WEIGHTED.dropDownText"),
                SplittingSurnameStyle.WEIGHTED.getDropDownText());

    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsOriginChangesSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME) {
                assertTrue(style.isOriginChangesSurname());
            } else {
                assertFalse(style.isOriginChangesSurname());
            }
        }
    }

    @Test
    public void testIsSpouseChangesSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.SPOUSE_CHANGES_SURNAME) {
                assertTrue(style.isSpouseChangesSurname());
            } else {
                assertFalse(style.isSpouseChangesSurname());
            }
        }
    }

    @Test
    public void testIsBothChangeSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.BOTH_CHANGE_SURNAME) {
                assertTrue(style.isBothChangeSurname());
            } else {
                assertFalse(style.isBothChangeSurname());
            }
        }
    }

    @Test
    public void testIsBothKeepSurname() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.BOTH_KEEP_SURNAME) {
                assertTrue(style.isBothKeepSurname());
            } else {
                assertFalse(style.isBothKeepSurname());
            }
        }
    }

    @Test
    public void testIsWeighted() {
        for (final SplittingSurnameStyle style : styles) {
            if (style == SplittingSurnameStyle.WEIGHTED) {
                assertTrue(style.isWeighted());
            } else {
                assertFalse(style.isWeighted());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Disabled // FIXME : Windchild : Test Missing
    @Test
    public void testApply() {

    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.text"),
                SplittingSurnameStyle.ORIGIN_CHANGES_SURNAME.toString());
        assertEquals(resources.getString("SplittingSurnameStyle.WEIGHTED.text"),
                SplittingSurnameStyle.WEIGHTED.toString());
    }
}
