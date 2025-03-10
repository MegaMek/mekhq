/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import mekhq.MekHQ;

class PhenotypeTest {
    // region Variable Declarations
    private static final Phenotype[] phenotypes = Phenotype.values();

    private final transient ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());
    // endregion Variable Declarations

    // region Getters
    @Test
    void testGetName() {
        assertEquals(resources.getString("Phenotype.AEROSPACE.text"), Phenotype.AEROSPACE.getName());
        assertEquals(resources.getString("Phenotype.NONE.text"), Phenotype.NONE.getName());
    }

    @Test
    void testGetToolTipText() {
        assertEquals(resources.getString("Phenotype.NAVAL.toolTipText"),
                Phenotype.NAVAL.getToolTipText());
        assertEquals(resources.getString("Phenotype.NONE.toolTipText"),
                Phenotype.NONE.getToolTipText());
    }
    // endregion Getters

    // region Boolean Comparison Methods
    @Test
    void testIsMekWarrior() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.MEKWARRIOR) {
                assertTrue(phenotype.isMekWarrior());
            } else {
                assertFalse(phenotype.isMekWarrior());
            }
        }
    }

    @Test
    void testIsElemental() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.ELEMENTAL) {
                assertTrue(phenotype.isElemental());
            } else {
                assertFalse(phenotype.isElemental());
            }
        }
    }

    @Test
    void testIsAerospace() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.AEROSPACE) {
                assertTrue(phenotype.isAerospace());
            } else {
                assertFalse(phenotype.isAerospace());
            }
        }
    }

    @Test
    void testIsVehicle() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.VEHICLE) {
                assertTrue(phenotype.isVehicle());
            } else {
                assertFalse(phenotype.isVehicle());
            }
        }
    }

    @Test
    void testIsProtoMek() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.PROTOMEK) {
                assertTrue(phenotype.isProtoMek());
            } else {
                assertFalse(phenotype.isProtoMek());
            }
        }
    }

    @Test
    void testIsNaval() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.NAVAL) {
                assertTrue(phenotype.isNaval());
            } else {
                assertFalse(phenotype.isNaval());
            }
        }
    }

    @Test
    void testIsNone() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.NONE) {
                assertTrue(phenotype.isNone());
            } else {
                assertFalse(phenotype.isNone());
            }
        }
    }

    @Test
    void testIsGeneral() {
        for (final Phenotype phenotype : phenotypes) {
            if (phenotype == Phenotype.GENERAL) {
                assertTrue(phenotype.isGeneral());
            } else {
                assertFalse(phenotype.isGeneral());
            }
        }
    }
    // endregion Boolean Comparison Methods

    @Test
    void testGetExternalPhenotypes() {
        final List<Phenotype> expected = Arrays.stream(phenotypes)
                .filter(phenotype -> (phenotype != Phenotype.NONE) && (phenotype != Phenotype.GENERAL))
                .collect(Collectors.toList());
        assertEquals(expected, Phenotype.getExternalPhenotypes());
    }

    // region File I/O
    @Test
    void testParseFromString() {
        // Normal Parsing
        assertEquals(Phenotype.NONE, Phenotype.parseFromString("NONE"));
        assertEquals(Phenotype.NAVAL, Phenotype.parseFromString("NAVAL"));

        // Error Case
        assertEquals(Phenotype.NONE, Phenotype.parseFromString("5"));
        assertEquals(Phenotype.NONE, Phenotype.parseFromString("blah"));
    }
    // endregion File I/O

    @Test
    void testToStringOverride() {
        assertEquals(resources.getString("Freeborn.text"), Phenotype.NONE.toString());
        assertEquals(resources.getString("Trueborn.text"), Phenotype.GENERAL.toString());
        assertEquals(resources.getString("Trueborn.text") + ' ' + resources.getString("Phenotype.MEKWARRIOR.text"),
                Phenotype.MEKWARRIOR.toString());
        assertEquals(
                resources.getString("Trueborn.text") + ' '
                        + resources.getString("Phenotype.AEROSPACE.groupingNameText"),
                Phenotype.AEROSPACE.toString());
    }
}
