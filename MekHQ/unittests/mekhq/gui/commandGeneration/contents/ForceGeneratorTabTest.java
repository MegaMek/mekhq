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
package mekhq.gui.commandGeneration.contents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Covers resolving the faction whose ranks a generated command wears.
 *
 * <p>Sub-factions are a RAT Generator idea and mostly have no faction of their own, so reading their
 * key literally found nothing and silently left the campaign's own ranks in place - a Word of Blake
 * Shadow Division came out wearing the player's rank structure instead of the Militia's.</p>
 */
class ForceGeneratorTabTest {

    /**
     * The trimmed test faction set holds LA but not Word of Blake, so the sub-faction fallback is
     * exercised with a Lyran key. The logic under test is the walk up the key, which is the same
     * whichever faction sits at the top of it.
     */
    private static final String PARENT_CODE = "LA";

    @BeforeAll
    static void loadFactions() {
        Factions.setInstance(Factions.loadDefault(true));
    }

    @Test
    void aFactionOfItsOwnResolvesToItself() {
        Faction parent = ForceGeneratorTab.resolveRankAuthority(PARENT_CODE);
        assertNotNull(parent, PARENT_CODE + " must resolve to a faction");
        assertEquals(PARENT_CODE, parent.getShortName());
    }

    /**
     * The reported bug: a sub-faction key has no faction of its own, so a command generated for one
     * wore the campaign's ranks instead of the parent's.
     */
    @Test
    void aSubFactionTakesItsParentsRanks() {
        Faction resolved = ForceGeneratorTab.resolveRankAuthority(PARENT_CODE + ".SD");
        assertNotNull(resolved,
              "a sub-faction has no faction of its own and must fall back to its parent");
        assertEquals(PARENT_CODE, resolved.getShortName(),
              "the sub-command wears its parent's ranks, not the campaign's");
    }

    @Test
    void aDeeplyNestedSubFactionWalksUpUntilSomethingMatches() {
        Faction resolved = ForceGeneratorTab.resolveRankAuthority(PARENT_CODE + ".SD.NONSENSE");
        assertNotNull(resolved, "every parent must be tried, not just the immediate one");
        assertEquals(PARENT_CODE, resolved.getShortName());
    }

    /**
     * Guards the trap that caused the bug: {@code Factions.getFaction} hands back a blank "Unknown"
     * faction rather than {@code null}, so a null check silently accepted a placeholder.
     */
    @Test
    void anUnknownFactionResolvesToNothing() {
        assertNull(ForceGeneratorTab.resolveRankAuthority("NOT_A_FACTION"),
              "an unrecognised code must not resolve to the blank placeholder faction");
    }
}
