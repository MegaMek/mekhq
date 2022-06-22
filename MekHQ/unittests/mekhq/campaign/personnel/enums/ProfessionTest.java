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
import org.junit.jupiter.api.Test;

import java.util.ResourceBundle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProfessionTest {
    //region Variable Declarations
    private static final Profession[] professions = Profession.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("Profession.MECHWARRIOR.toolTipText"),
                Profession.MECHWARRIOR.getToolTipText());
        assertEquals(resources.getString("Profession.ADMINISTRATOR.toolTipText"),
                Profession.ADMINISTRATOR.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsMechWarrior() {
        for (final Profession profession : professions) {
            if (profession == Profession.MECHWARRIOR) {
                assertTrue(profession.isMechWarrior());
            } else {
                assertFalse(profession.isMechWarrior());
            }
        }
    }

    @Test
    public void testIsAerospace() {
        for (final Profession profession : professions) {
            if (profession == Profession.AEROSPACE) {
                assertTrue(profession.isAerospace());
            } else {
                assertFalse(profession.isAerospace());
            }
        }
    }

    @Test
    public void testIsVehicle() {
        for (final Profession profession : professions) {
            if (profession == Profession.VEHICLE) {
                assertTrue(profession.isVehicle());
            } else {
                assertFalse(profession.isVehicle());
            }
        }
    }

    @Test
    public void testIsNaval() {
        for (final Profession profession : professions) {
            if (profession == Profession.NAVAL) {
                assertTrue(profession.isNaval());
            } else {
                assertFalse(profession.isNaval());
            }
        }
    }

    @Test
    public void testIsInfantry() {
        for (final Profession profession : professions) {
            if (profession == Profession.INFANTRY) {
                assertTrue(profession.isInfantry());
            } else {
                assertFalse(profession.isInfantry());
            }
        }
    }

    @Test
    public void testIsTech() {
        for (final Profession profession : professions) {
            if (profession == Profession.TECH) {
                assertTrue(profession.isTech());
            } else {
                assertFalse(profession.isTech());
            }
        }
    }

    @Test
    public void testIsMedical() {
        for (final Profession profession : professions) {
            if (profession == Profession.MEDICAL) {
                assertTrue(profession.isMedical());
            } else {
                assertFalse(profession.isMedical());
            }
        }
    }

    @Test
    public void testIsAdministrator() {
        for (final Profession profession : professions) {
            if (profession == Profession.ADMINISTRATOR) {
                assertTrue(profession.isAdministrator());
            } else {
                assertFalse(profession.isAdministrator());
            }
        }
    }

    @Test
    public void testIsCivilian() {
        for (final Profession profession : professions) {
            if (profession == Profession.CIVILIAN) {
                assertTrue(profession.isCivilian());
            } else {
                assertFalse(profession.isCivilian());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetProfession() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testGetProfessionFromBase() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testGetBaseProfession() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testIsEmptyProfession() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testGetAlternateProfession() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testGetProfessionFromPersonnelRole() {
        // FIXME : Windchild : ADD
    }

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("Profession.AEROSPACE.text"), Profession.AEROSPACE.toString());
        assertEquals(resources.getString("Profession.CIVILIAN.text"), Profession.CIVILIAN.toString());
    }
}
