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
package mekhq.campaign.digitalGM.stratCon;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import megamek.common.compute.Compute;
import org.mockito.MockedStatic;

/**
 * Loaded dice for StratCon tests: every roll of several d6 comes up {@link #PIPS_PER_DIE} on each die, so a test can
 * tell how many dice were rolled - 3d6 gives exactly 15, where 2d6 would give 10. Everything else in {@link Compute}
 * behaves as usual.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConTestDice {
    /** What each die comes up while the dice are loaded. */
    public static final int PIPS_PER_DIE = 5;

    private StratConTestDice() {
    }

    /**
     * Loads the dice until the returned mock is closed; use it in a try-with-resources block.
     *
     * @return the static mock of {@link Compute}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static MockedStatic<Compute> loadDice() {
        MockedStatic<Compute> compute = mockStatic(Compute.class, CALLS_REAL_METHODS);
        compute.when(() -> Compute.d6(anyInt())).thenAnswer(invocation -> {
            int dice = invocation.getArgument(0);
            return dice * PIPS_PER_DIE;
        });
        return compute;
    }
}
