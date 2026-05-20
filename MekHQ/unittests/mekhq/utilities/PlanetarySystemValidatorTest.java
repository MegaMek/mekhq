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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
package mekhq.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystemYamlIO;
import mekhq.utilities.ValidationMessage.Category;
import org.junit.jupiter.api.Test;

class PlanetarySystemValidatorTest {

    private static final String INVALID_SYSTEM = """
          id: Bad Validator Test
          xcood: 0.0
          ycood: 0.0
          primarySlot: 2
          planet:
            - name: Bad Validator Test Prime
              type: TERRESTRIAL
              sysPos: 1
              pressure: STANDARD
              atmosphere: BREATHABLE
          """;

    @Test
    void validateEditedSystemSurfacesExistingRules() throws Exception {
        PlanetarySystem system = PlanetarySystemYamlIO.read(new ByteArrayInputStream(
              INVALID_SYSTEM.getBytes(StandardCharsets.UTF_8)));

        ValidationResult result = new PlanetarySystemValidator().validate(system, "editor.yml");

        assertTrue(result.hasErrors());
        assertFalse(result.getByCategory(Category.MISSING_STAR).isEmpty());
        assertFalse(result.getByCategory(Category.INVALID_PRIMARY_SLOT).isEmpty());
    }
}

