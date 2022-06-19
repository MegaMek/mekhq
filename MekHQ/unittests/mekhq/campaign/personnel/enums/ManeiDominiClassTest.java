/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManeiDominiClassTest {
    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());

    //region File I/O
    /**
     * Testing to ensure the enum is properly parsed from a given String, dependent on whether it
     * is parsing from ManeiDominiClass.name(), the ordinal (formerly magic numbers), or a failure
     * condition
     */
    @Test
    public void testParseFromString() {
        // Enum.valueOf Testing
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("NONE"));
        assertEquals(ManeiDominiClass.GHOST, ManeiDominiClass.parseFromString("GHOST"));

        // Parsing from ordinal testing
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("0"));
        assertEquals(ManeiDominiClass.BANSHEE, ManeiDominiClass.parseFromString("3"));
        assertEquals(ManeiDominiClass.POLTERGEIST, ManeiDominiClass.parseFromString("7"));
        // This is an out of bounds check, as any future additions (albeit highly improbably)
        // must adjust for the fact that the old ordinal numbers only went up to 7
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("8"));

        // Default Failure Case
        assertEquals(ManeiDominiClass.NONE, ManeiDominiClass.parseFromString("failureFailsFake"));
    }
    //endregion File I/O

    /**
     * Testing to ensure the toString Override is working as intended
     */
    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("ManeiDominiClass.NONE.text"), ManeiDominiClass.NONE.toString());
        assertEquals(resources.getString("ManeiDominiClass.PHANTOM.text"), ManeiDominiClass.PHANTOM.toString());
        assertEquals(resources.getString("ManeiDominiClass.POLTERGEIST.text"), ManeiDominiClass.POLTERGEIST.toString());
    }
}
