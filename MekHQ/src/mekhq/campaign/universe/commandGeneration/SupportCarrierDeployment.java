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
package mekhq.campaign.universe.commandGeneration;

import megamek.common.annotations.Nullable;
import mekhq.campaign.mission.scenarios.Scenario;

/**
 * The single decision point for whether support carriers may deploy to a scenario.
 *
 * <p>Support carriers hold technicians, doctors and administrators for the TOE. They are unarmoured civilians carrying
 * knives, and every casualty is a real support character off the roster, so today they never deploy: not by the TOE
 * context menu, not when a formation containing them - HQ, typically - is assigned, and not by StratCon. Deploying a
 * formation that holds carriers deploys its fighting units and leaves the carriers, and their people, at home.</p>
 *
 * <p>That is a rule for now, not forever. A base attack is the obvious case where support staff get pulled into a
 * fight, and when that scenario type exists it flips this gate for itself. Every site that decides whether a carrier
 * deploys asks here, so flipping it means changing one method, not hunting for five call sites. The reconciler is
 * already deployment-aware and needs no change when that happens: it leaves a deployed carrier alone and catches its
 * profession up when the deployment ends.</p>
 *
 * @author Illiani
 * @since 0.51.0
 */
public final class SupportCarrierDeployment {

    private SupportCarrierDeployment() {
        // utility class
    }

    /**
     * Whether support carriers may deploy to this scenario.
     *
     * @param scenario the scenario being deployed to; {@code null} when the deployment has no scenario context, which
     *                 is treated like any other scenario
     *
     * @return {@code true} if carriers may deploy. Always {@code false} today; a future scenario type that pulls
     *       support staff into a fight returns {@code true} for itself here
     */
    public static boolean isAllowed(@Nullable Scenario scenario) {
        return false;
    }
}
