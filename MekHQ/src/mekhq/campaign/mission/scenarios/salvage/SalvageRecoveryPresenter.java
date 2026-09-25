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
package mekhq.campaign.mission.scenarios.salvage;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.scenarios.Scenario;

/**
 * Shows the player a scenario's salvage recovery and waits for them to confirm it.
 *
 * <p>The salvage rules build the {@link SalvageRecoverySession} and settle the salvage afterward; the presenter only
 * lets the player work through the session. This keeps the rules free of any user interface: the game passes in the
 * salvage recovery console, and tests pass in a stand-in that makes the player's choices directly.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
@FunctionalInterface
public interface SalvageRecoveryPresenter {
    /**
     * Shows the recovery and returns once the player has confirmed it. The player's choices (which units recover each
     * wreck, and what happens to it) are recorded in the session.
     *
     * @param campaign the current campaign
     * @param scenario the scenario whose salvage is being recovered
     * @param session  the recovery to show; it has at least one wreck
     */
    void presentRecovery(Campaign campaign, Scenario scenario, SalvageRecoverySession session);
}
