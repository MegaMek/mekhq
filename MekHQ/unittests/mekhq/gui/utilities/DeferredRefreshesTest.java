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
package mekhq.gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Illiani
 * @since 0.51.01
 */
class DeferredRefreshesTest {
    private static final String RECOUNT = "recount";
    private static final String REBUILD = "rebuild";

    @Test
    void showingTabRefreshesStraightAway() {
        DeferredRefreshes deferredRefreshes = new DeferredRefreshes();
        List<String> runs = new ArrayList<>();

        deferredRefreshes.request(RECOUNT, () -> runs.add(RECOUNT), true);

        assertEquals(List.of(RECOUNT), runs);
        assertFalse(deferredRefreshes.isPending(RECOUNT));
    }

    @Test
    void hiddenTabRunsEachRefreshOnceWhenShown() {
        DeferredRefreshes deferredRefreshes = new DeferredRefreshes();
        List<String> runs = new ArrayList<>();

        for (int request = 0; request < 5; request++) {
            deferredRefreshes.request(RECOUNT, () -> runs.add(RECOUNT), false);
        }
        deferredRefreshes.request(REBUILD, () -> runs.add(REBUILD), false);

        assertTrue(runs.isEmpty(), "nothing should run while the tab is hidden");
        assertTrue(deferredRefreshes.isPending(RECOUNT));

        deferredRefreshes.runPending();

        assertEquals(List.of(RECOUNT, REBUILD), runs);
        assertFalse(deferredRefreshes.isPending(RECOUNT));

        deferredRefreshes.runPending();
        assertEquals(2, runs.size(), "showing the tab again with nothing held should do nothing");
    }

    @Test
    void refreshRunWhileShowingDropsTheHeldOne() {
        DeferredRefreshes deferredRefreshes = new DeferredRefreshes();
        List<String> runs = new ArrayList<>();

        deferredRefreshes.request(RECOUNT, () -> runs.add("held"), false);
        deferredRefreshes.request(RECOUNT, () -> runs.add("now"), true);
        deferredRefreshes.runPending();

        assertEquals(List.of("now"), runs);
    }

    @Test
    void callerCanRunOneHeldRefreshEarly() {
        DeferredRefreshes deferredRefreshes = new DeferredRefreshes();
        List<String> runs = new ArrayList<>();

        deferredRefreshes.request(RECOUNT, () -> runs.add(RECOUNT), false);
        deferredRefreshes.request(REBUILD, () -> runs.add(REBUILD), false);

        deferredRefreshes.runPending(REBUILD);
        deferredRefreshes.runPending(REBUILD);

        assertEquals(List.of(REBUILD), runs);
        assertTrue(deferredRefreshes.isPending(RECOUNT));
        assertFalse(deferredRefreshes.isPending(REBUILD));
    }

    @Test
    void refreshRequestedWhileRunningHeldRefreshesIsHeldForNextTime() {
        DeferredRefreshes deferredRefreshes = new DeferredRefreshes();
        List<String> runs = new ArrayList<>();

        deferredRefreshes.request(RECOUNT, () -> {
            runs.add(RECOUNT);
            deferredRefreshes.request(REBUILD, () -> runs.add(REBUILD), false);
        }, false);

        deferredRefreshes.runPending();

        assertEquals(List.of(RECOUNT), runs);
        assertTrue(deferredRefreshes.isPending(REBUILD));
    }
}
