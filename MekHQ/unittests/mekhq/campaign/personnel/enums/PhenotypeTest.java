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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel.enums;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class PhenotypeTest {
    private static final Phenotype[] phenotypes = Phenotype.values();

    @Test
    void testFromString() {
        // Valid inputs
        assertEquals(Phenotype.MEKWARRIOR, Phenotype.fromString("MEKWARRIOR"));
        assertEquals(Phenotype.ELEMENTAL, Phenotype.fromString("Elemental"));
        assertEquals(Phenotype.AEROSPACE, Phenotype.fromString("aerospace"));
        assertEquals(Phenotype.GENERAL, Phenotype.fromString("GENERAL"));

        // Index input
        assertEquals(Phenotype.VEHICLE, Phenotype.fromString("3"));

        // Invalid inputs
        assertEquals(Phenotype.NONE, Phenotype.fromString("test"));
        assertEquals(Phenotype.NONE, Phenotype.fromString(""));
        assertEquals(Phenotype.NONE, Phenotype.fromString(null));
    }

    @Test
    void testGetLabel() {
        for (final Phenotype phenotype : phenotypes) {
            String label = phenotype.getLabel();
            boolean isValid = isResourceKeyValid(label);
            assertTrue(isValid, "Invalid resource key: " + label);
        }
    }

    @Test
    void testGetTooltip() {
        for (final Phenotype phenotype : phenotypes) {
            String tooltip = phenotype.getTooltip();
            boolean isValid = isResourceKeyValid(tooltip);
            assertTrue(isValid, "Invalid resource key: " + tooltip);
        }
    }

    @Test
    void testGetShortName() {
        for (final Phenotype phenotype : phenotypes) {
            String shortName = phenotype.getShortName();
            boolean isValid = isResourceKeyValid(shortName);
            assertTrue(isValid, "Invalid resource key: " + shortName);
        }
    }

    @Test
    void testGetExternalPhenotypes() {
        final List<Phenotype> expected = Arrays.stream(phenotypes)
                                               .filter(phenotype -> (phenotype != Phenotype.NONE) &&
                                                                          (phenotype != Phenotype.GENERAL))
                                               .collect(Collectors.toList());
        assertEquals(expected, Phenotype.getExternalPhenotypes());
    }
}
