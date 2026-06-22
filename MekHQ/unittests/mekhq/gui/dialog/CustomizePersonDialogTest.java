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

package mekhq.gui.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CustomizePersonDialog#chooseDisplayedOwner(List, List, Set, boolean)}, the rule that decides whether
 * a world's era-correct owner is replaced by its eventual owner when building the origin-system picker.
 *
 * <p>The pivotal regression is the renamed-faction case (Outworlds Alliance -> Raven Alliance): a faction that
 * dissolves into a brand-new successor must NOT be substituted, or pre-rename characters get credited to a state that
 * does not yet exist and the origin-faction world filter finds no worlds for the still-extant faction.</p>
 */
class CustomizePersonDialogTest {

    /**
     * The core bug: Outworlds Alliance (ends 3082) renames into the Raven Alliance in 3083. The Raven Alliance never
     * owned the world before the rename, so a pre-3083 snapshot must stay Outworlds Alliance even though OA dissolves
     * within the proximity window.
     */
    @Test
    void renamedFactionKeepsEraCorrectOwner() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("OA"), List.of("RA"),
              Set.of("OA"), true);

        assertEquals(List.of("OA"), displayed);
    }

    /**
     * Same class of bug for a Clan: Clan Wolf (ends 3142) renames into the Wolf Empire. The successor never held the
     * world before, so the era-correct Clan Wolf ownership is kept.
     */
    @Test
    void renamedClanKeepsEraCorrectOwner() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("CW"), List.of("CWE"),
              Set.of("CW"), true);

        assertEquals(List.of("CW"), displayed);
    }

    /**
     * FedCom overlay must still be substituted: the Federated Suns held New Avalon before the FedCom era, so the
     * eventual owner appears in the prior-owner set and the transient FC label is replaced by FS.
     */
    @Test
    void transientOverlaySubstitutesToRevertedOwner() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("FC"), List.of("FS"),
              Set.of("FS", "FC"), true);

        assertEquals(List.of("FS"), displayed);
    }

    /**
     * Tharkad: FedCom overlay over a world the Lyrans held before and end up holding again -> substitutes to LA.
     */
    @Test
    void transientOverlaySubstitutesToLyranSide() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("FC"), List.of("LA"),
              Set.of("LA", "FC"), true);

        assertEquals(List.of("LA"), displayed);
    }

    /**
     * A Disputed snapshot is always substituted regardless of dissolution proximity or prior ownership.
     */
    @Test
    void disputedSnapshotAlwaysSubstitutes() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("DIS"), List.of("FS"),
              Set.of("DIS"), false);

        assertEquals(List.of("FS"), displayed);
    }

    /**
     * When the snapshot faction does not dissolve near {@code when} and is not disputed, the era-correct owner is kept
     * even if a different future owner exists.
     */
    @Test
    void stableSnapshotIsKept() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("OA"), List.of("RA"),
              Set.of("OA"), false);

        assertEquals(List.of("OA"), displayed);
    }

    /**
     * Identical snapshot and eventual owner short-circuits to the snapshot (no spurious substitution).
     */
    @Test
    void sameOwnerIsKept() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("LA"), List.of("LA"),
              Set.of("LA"), true);

        assertEquals(List.of("LA"), displayed);
    }

    /**
     * No future faction event (data ends at the snapshot) -> the snapshot is used as-is.
     */
    @Test
    void noEventualOwnerKeepsSnapshot() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(List.of("OA"), null,
              Set.of("OA"), true);

        assertEquals(List.of("OA"), displayed);
    }

    /**
     * An uncolonized world (no snapshot) is never credited to a future colonization event.
     */
    @Test
    void uncolonizedWorldHasNoOwner() {
        List<String> displayed = CustomizePersonDialog.chooseDisplayedOwner(null, List.of("FS"),
              Set.of(), true);

        assertNull(displayed);
    }
}
