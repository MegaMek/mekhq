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
package mekhq.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import megamek.common.event.Subscribe;
import mekhq.campaign.events.OrganizationChangedEvent;
import org.junit.jupiter.api.Test;

/**
 * A force generation runs off the event dispatch thread and sets a flag that makes several tabs skip their refreshes,
 * because refreshing against a half-built campaign crashed them. The flag is the easy half. The hard half is refreshing
 * once generation finishes, and generation signs off by firing exactly one event: {@link OrganizationChangedEvent}.
 *
 * <p>A tab that skips during generation and does not subscribe to that event never refreshes at all. That is what left
 * the Warehouse tab empty after a build while the log showed the warehouse holding twenty million C-Bills of spares, and
 * the Finances tab is wired the same way for the starting simulation's ten years of transactions.</p>
 */
class BulkGenerationRefreshTest {

    /**
     * The tabs that skip refreshing while a bulk generation is running. Each one must therefore refresh when the
     * generation ends. Add a tab here when you add the skip guard to it.
     */
    private static final List<Class<?>> TABS_THAT_SKIP_DURING_GENERATION =
          List.of(WarehouseTab.class, FinancesTab.class, CommandCenterTab.class);

    @Test
    void everyTabThatSkipsDuringGenerationRefreshesWhenItEnds() {
        List<String> missing = new ArrayList<>();
        for (Class<?> tab : TABS_THAT_SKIP_DURING_GENERATION) {
            if (!subscribesToGenerationFinished(tab)) {
                missing.add(tab.getSimpleName());
            }
        }

        assertTrue(missing.isEmpty(),
              "these tabs skip their refresh during a bulk generation and never hear that it finished, so they keep "
                    + "showing pre-generation contents: " + missing);
    }

    /**
     * Whether a tab has a {@link Subscribe} handler that takes an {@link OrganizationChangedEvent}. Declared methods
     * only: the handler has to be on the tab itself for the event bus to register it.
     *
     * @param tab the tab class to inspect
     *
     * @return {@code true} if the tab is told when a generation finishes
     */
    private static boolean subscribesToGenerationFinished(Class<?> tab) {
        for (Method method : tab.getDeclaredMethods()) {
            if (!method.isAnnotationPresent(Subscribe.class)) {
                continue;
            }
            Class<?>[] parameters = method.getParameterTypes();
            if ((parameters.length == 1) && parameters[0].isAssignableFrom(OrganizationChangedEvent.class)) {
                return true;
            }
        }
        return false;
    }
}
