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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.universe.commandGeneration.ratgen.FormationNamer.NamedFormation;
import mekhq.campaign.universe.enums.ForceNamingMethod;
import org.junit.jupiter.api.Test;

/**
 * Verifies {@link FormationNamer}: company-tier formations allocate sequential alphabet designators
 * that continue across generator runs, lance-tier formations get callsigns under their company's
 * designator, and battalion-tier names de-duplicate by advancing their sequence token.
 */
class FormationNamerTest {

    private static FormationNamer ccbNamer(String... existingNames) {
        return new FormationNamer(ForceNamingMethod.CCB_1943, List.of(existingNames));
    }

    @Test
    void companiesAllocateSequentialDesignatorsAndLancesGetCallsigns() {
        FormationNamer namer = ccbNamer();

        NamedFormation firstCompany = namer.nameFormation("A Company", FormationLevel.COMPANY, null, 1);
        assertEquals("Able Company", firstCompany.name());
        assertEquals("Able", firstCompany.designator());

        assertEquals("Able-1 Battle Lance",
              namer.nameFormation("Battle Lance", FormationLevel.LANCE, firstCompany.designator(), 1).name());
        assertEquals("Able-2 Battle Lance",
              namer.nameFormation("Battle Lance", FormationLevel.LANCE, firstCompany.designator(), 2).name(),
              "identical flavour names in one company stay unique through the lance number");

        NamedFormation secondCompany = namer.nameFormation("A Company", FormationLevel.COMPANY, null, 2);
        assertEquals("Baker Company", secondCompany.name(),
              "the second company advances the alphabet even though the engine repeated 'A Company'");
        assertEquals("Baker-1 Battle Lance",
              namer.nameFormation("Battle Lance", FormationLevel.LANCE, secondCompany.designator(), 1).name());
    }

    @Test
    void designatorSequenceContinuesAcrossGeneratorRuns() {
        FormationNamer namer = ccbNamer("Able Company", "Able-1 Battle Lance", "Baker Company");
        assertEquals("Charlie Company",
              namer.nameFormation("A Company", FormationLevel.COMPANY, null, 1).name());
    }

    @Test
    void engineDesignatorTokensAreStrippedBeforePrefixing() {
        FormationNamer namer = ccbNamer();
        assertEquals("Able Company",
              namer.nameFormation("1/A Company", FormationLevel.COMPANY, null, 1).name(),
              "a parent-qualified engine name must not keep its old designator");
        assertEquals("Baker Trinary",
              namer.nameFormation("Trinary Bravo", FormationLevel.BINARY_OR_TRINARY, null, 1).name(),
              "a trailing designator word is stripped too");
    }

    @Test
    void battalionTierKeepsNameAndBumpsSequenceTokenOnClash() {
        FormationNamer namer = ccbNamer("First Battalion", "1st Battalion");
        NamedFormation spelled = namer.nameFormation("First Battalion", FormationLevel.BATTALION, null, 1);
        assertEquals("Second Battalion", spelled.name());
        assertNull(spelled.designator(), "battalion-tier formations do not allocate designators");
        assertEquals("2nd Battalion",
              namer.nameFormation("1st Battalion", FormationLevel.BATTALION, null, 2).name());
    }

    @Test
    void namesWithoutSequenceTokensFallBackToACounter() {
        FormationNamer namer = ccbNamer("Assault Echelon");
        assertEquals("Assault Echelon (2)",
              namer.nameFormation("Assault Echelon", FormationLevel.BATTALION, null, 1).name());
    }

    @Test
    void greekMethodUsesGreekWords() {
        FormationNamer namer = new FormationNamer(ForceNamingMethod.GREEK_ALPHABET,
              List.of("Alpha Trinary"));
        NamedFormation trinary = namer.nameFormation("Trinary Bravo", FormationLevel.BINARY_OR_TRINARY, null, 1);
        assertEquals("Beta Trinary", trinary.name(),
              "an existing 'Alpha Trinary' marks Alpha as used, so the next trinary is Beta");
        assertEquals("Beta-1 Star",
              namer.nameFormation("Alpha Star", FormationLevel.STAR_OR_NOVA, trinary.designator(), 1).name(),
              "the engine's per-trinary star word is replaced by the trinary callsign");
    }

    @Test
    void lanceWithoutACompanyAboveAllocatesItsOwnDesignator() {
        FormationNamer namer = ccbNamer();
        assertEquals("Able Battle Lance",
              namer.nameFormation("Battle Lance", FormationLevel.LANCE, null, 1).name());
        assertEquals("Baker Fire Lance",
              namer.nameFormation("Fire Lance", FormationLevel.LANCE, null, 2).name());
    }

    @Test
    void comStarLevelNamesSurviveIntact() {
        FormationNamer namer = ccbNamer();
        // Roman numerals are not designator tokens, so CS names like "III-alpha" keep their identity.
        assertEquals("Able III-alpha",
              namer.nameFormation("III-alpha", FormationLevel.LEVEL_III, null, 1).name());
    }
}
