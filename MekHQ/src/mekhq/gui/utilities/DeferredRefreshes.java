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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import megamek.logging.MMLogger;

/**
 * Holds back costly refreshes of a tab while nobody can see it, and runs each of them once when the tab is shown.
 *
 * <p>Campaign events refresh every tab, shown or not. Some refreshes are expensive on a large campaign (recounting
 * the parts in use, rebuilding the parts acquisition list) and only matter once their tab is on screen, so a hidden
 * tab records that the refresh is due and does the work when it is next shown. However many times a refresh is
 * requested while hidden, it runs once.</p>
 *
 * <p>This class knows nothing about Swing: the tab says whether it is showing on each request, and tells this class
 * when it has been shown.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class DeferredRefreshes {
    private static final MMLogger LOGGER = MMLogger.create(DeferredRefreshes.class);

    private final Map<String, Runnable> pendingRefreshes = new LinkedHashMap<>();

    /**
     * Runs the refresh now if the tab is showing, and otherwise holds it until the tab is shown. A refresh run now
     * replaces any held under the same name.
     *
     * @param refreshName the name the refresh is held under, unique within the tab
     * @param refresh     the refresh to run
     * @param isShowing   whether the tab is on screen
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void request(String refreshName, Runnable refresh, boolean isShowing) {
        if (isShowing) {
            pendingRefreshes.remove(refreshName);
            refresh.run();
            return;
        }
        if (pendingRefreshes.put(refreshName, refresh) == null) {
            LOGGER.debug("[DeferredRefresh] {} held until its tab is shown", refreshName);
        }
    }

    /**
     * Runs every held refresh, in the order they were first held, and forgets them.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void runPending() {
        if (pendingRefreshes.isEmpty()) {
            return;
        }
        // Copy first: a refresh may request another one while it runs
        List<Map.Entry<String, Runnable>> refreshesToRun = new ArrayList<>(pendingRefreshes.entrySet());
        pendingRefreshes.clear();
        for (Map.Entry<String, Runnable> pendingRefresh : refreshesToRun) {
            LOGGER.debug("[DeferredRefresh] Running {}, held while its tab was hidden", pendingRefresh.getKey());
            pendingRefresh.getValue().run();
        }
    }

    /**
     * Runs one held refresh now, if it is held, for a caller that needs its result before the tab is shown.
     *
     * @param refreshName the name the refresh is held under
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void runPending(String refreshName) {
        Runnable refresh = pendingRefreshes.remove(refreshName);
        if (refresh != null) {
            LOGGER.debug("[DeferredRefresh] Running {} early, for a caller that needs it", refreshName);
            refresh.run();
        }
    }

    /**
     * @param refreshName the name the refresh is held under
     *
     * @return {@code true} if that refresh is waiting for its tab to be shown
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isPending(String refreshName) {
        return pendingRefreshes.containsKey(refreshName);
    }
}
