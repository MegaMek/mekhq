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
package mekhq.gui.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.client.ratgenerator.FactionRecord;
import org.junit.jupiter.api.Test;

/**
 * Covers which factions have their formation naming defaulted to the Greek alphabet.
 *
 * <p>ComStar and the Word of Blake fix their Level IV and Level III names in the ruleset itself
 * ("IV-alpha", "III-beta"), leaving the Level II beneath to follow the player's choice - so anything
 * but Greek there puts an "Able" or a "Bravo" inside a Greek-named Level III.</p>
 */
class CommandGenerationPaneTest {

    private static FactionRecord factionRecord(String key, boolean isClan) {
        FactionRecord factionRecord = new FactionRecord(key);
        factionRecord.setClan(isClan);
        return factionRecord;
    }

    @Test
    void comStarAndWordOfBlakeNameFormationsInGreek() {
        assertTrue(CommandGenerationPane.usesGreekFormationNames(factionRecord("CS", false)));
        assertTrue(CommandGenerationPane.usesGreekFormationNames(factionRecord("WOB", false)));
    }

    @Test
    void aSubCommandFollowsItsParent() {
        assertTrue(CommandGenerationPane.usesGreekFormationNames(factionRecord("WOB.SD", false)),
              "the Shadow Divisions name their formations the way the Word of Blake does");
        assertTrue(CommandGenerationPane.usesGreekFormationNames(factionRecord("WOB.PM", false)));
    }

    @Test
    void clansStillNameFormationsInGreek() {
        assertTrue(CommandGenerationPane.usesGreekFormationNames(factionRecord("CJF", true)),
              "the Clans name their galaxies in Greek and must keep doing so");
    }

    /**
     * Everything else leaves the player's choice alone rather than overwriting a deliberate one.
     */
    @Test
    void otherFactionsAreLeftToThePlayersChoice() {
        assertFalse(CommandGenerationPane.usesGreekFormationNames(factionRecord("LA", false)));
        assertFalse(CommandGenerationPane.usesGreekFormationNames(factionRecord("FS", false)));
        assertFalse(CommandGenerationPane.usesGreekFormationNames(factionRecord("MERC", false)));
    }

    /**
     * A key merely beginning with the same letters is a different faction: matching must be on the
     * whole code or a dotted sub-command of it, not a bare prefix.
     */
    @Test
    void aFactionWhoseKeyMerelyStartsTheSameIsNotMatched() {
        assertFalse(CommandGenerationPane.usesGreekFormationNames(factionRecord("CSA", false)),
              "Clan Snow Raven's CSA is not ComStar");
        assertFalse(CommandGenerationPane.usesGreekFormationNames(factionRecord("WOBBLE", false)));
    }
}
