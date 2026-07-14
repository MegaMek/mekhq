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
package mekhq.campaign.universe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import megamek.common.universe.Factions2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FactionsAliasTest {

    @BeforeEach
    void useTestFactions() {
        // Pin the shared Factions2 singleton to the test faction directory so the alias fixture
        // (MERGED_test.yml) is loaded deterministically regardless of test ordering.
        Factions2.setInstance(new Factions2("testresources/data/universe/factions"));
    }

    @Test
    void retiredCodeResolvesToSurvivingFactionViaAlias() {
        Factions factions = Factions.load(true);
        // OLDCODE has no faction file of its own; it resolves to MRG via the alias.
        assertEquals("MRG", factions.getFaction("OLDCODE").getShortName());
    }

    @Test
    void realFactionWinsOverCollidingAlias() {
        Factions factions = Factions.load(true);
        // MRG declares FS as an alias, but FS is a real faction and must win the lookup.
        assertEquals("FS", factions.getFaction("FS").getShortName());
    }

    @Test
    void layeredFormationIconArtResolvesByYear() {
        Factions factions = Factions.load(true);
        Faction merged = factions.getFaction("MRG");
        // Before the change year, the base art applies.
        assertEquals("BaseCategory", merged.getLayeredFormationIconLogoCategory(3050));
        assertEquals("base_logo.png", merged.getLayeredFormationIconLogoFilename(3050));
        // From the change year onward, the era art applies.
        assertEquals("EraCategory", merged.getLayeredFormationIconLogoCategory(3100));
        assertEquals("era_logo.png", merged.getLayeredFormationIconLogoFilename(3100));
        assertEquals("era_background.png", merged.getLayeredFormationIconBackgroundFilename(3100));
    }
}
