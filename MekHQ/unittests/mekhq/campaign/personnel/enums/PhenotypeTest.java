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

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PhenotypeTest {
    //region Variable Declarations
    private static final Phenotype[] phenotypes = Phenotype.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    //endregion Variable Declarations

    //region Getters
    @Test
    public void testGetName() {
        assertEquals(resources.getString("Phenotype.AEROSPACE.text"), Phenotype.AEROSPACE.getName());
        assertEquals(resources.getString("Phenotype.NONE.text"), Phenotype.NONE.getName());
    }

    @Test
    public void testGetToolTipText() {
        assertEquals(resources.getString("Phenotype.NAVAL.toolTipText"),
                Phenotype.NAVAL.getToolTipText());
        assertEquals(resources.getString("Phenotype.NONE.toolTipText"),
                Phenotype.NONE.getToolTipText());
    }
    //endregion Getters

    //region Boolean Comparison Methods
    @Test
    public void testIsMekWarrior() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.MEKWARRIOR) {
                assertTrue(phenotype.isMekWarrior());
            } else {
                assertFalse(phenotype.isMekWarrior());
            }
        }
    }

    @Test
    public void testIsElemental() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.ELEMENTAL) {
                assertTrue(phenotype.isElemental());
            } else {
                assertFalse(phenotype.isElemental());
            }
        }
    }

    @Test
    public void testIsAerospace() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.AEROSPACE) {
                assertTrue(phenotype.isAerospace());
            } else {
                assertFalse(phenotype.isAerospace());
            }
        }
    }

    @Test
    public void testIsVehicle() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.VEHICLE) {
                assertTrue(phenotype.isVehicle());
            } else {
                assertFalse(phenotype.isVehicle());
            }
        }
    }

    @Test
    public void testIsProtoMek() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.PROTOMEK) {
                assertTrue(phenotype.isProtoMek());
            } else {
                assertFalse(phenotype.isProtoMek());
            }
        }
    }

    @Test
    public void testIsNaval() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.NAVAL) {
                assertTrue(phenotype.isNaval());
            } else {
                assertFalse(phenotype.isNaval());
            }
        }
    }

    @Test
    public void testIsNone() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.NONE) {
                assertTrue(phenotype.isNone());
            } else {
                assertFalse(phenotype.isNone());
            }
        }
    }

    @Test
    public void testIsGeneral() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.GENERAL) {
                assertTrue(phenotype.isGeneral());
            } else {
                assertFalse(phenotype.isGeneral());
            }
        }
    }
    //endregion Boolean Comparison Methods

    @Test
    public void testGetExternalPhenotypes() {
        final List<Phenotype> expected = Arrays.stream(phenotypes)
                .filter(phenotype -> (phenotype != Phenotype.NONE) && (phenotype != Phenotype.GENERAL))
                .collect(Collectors.toList());
        assertEquals(expected, Phenotype.getExternalPhenotypes());
    }

    //region File I/O
    @Test
    public void testParseFromString() {
        // Normal Parsing
        assertEquals(Phenotype.NONE, Phenotype.parseFromString("NONE"));
        assertEquals(Phenotype.NAVAL, Phenotype.parseFromString("NAVAL"));

        // Legacy Parsing
        assertEquals(Phenotype.NONE, Phenotype.parseFromString("0"));
        assertEquals(Phenotype.MEKWARRIOR, Phenotype.parseFromString("1"));
        assertEquals(Phenotype.ELEMENTAL, Phenotype.parseFromString("2"));
        assertEquals(Phenotype.AEROSPACE, Phenotype.parseFromString("3"));
        assertEquals(Phenotype.VEHICLE, Phenotype.parseFromString("4"));

        // Error Case
        assertEquals(Phenotype.NONE, Phenotype.parseFromString("5"));
        assertEquals(Phenotype.NONE, Phenotype.parseFromString("blah"));
    }
    //endregion File I/O

    @Test
    public void testToStringOverride() {
        assertEquals(resources.getString("Freeborn.text"), Phenotype.NONE.toString());
        assertEquals(resources.getString("Trueborn.text"), Phenotype.GENERAL.toString());
        assertEquals(resources.getString("Trueborn.text") + ' ' + resources.getString("Phenotype.MEKWARRIOR.text"),
                Phenotype.MEKWARRIOR.toString());
        assertEquals(resources.getString("Trueborn.text") + ' ' + resources.getString("Phenotype.AEROSPACE.groupingNameText"),
                Phenotype.AEROSPACE.toString());
    }
}
