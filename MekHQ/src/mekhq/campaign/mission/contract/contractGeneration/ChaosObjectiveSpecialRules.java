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
package mekhq.campaign.mission.contract.contractGeneration;

public enum ChaosObjectiveSpecialRules {
    /* Player gets bonus loot after each track. */
    USE_PIRATE_LOOTING,

    /* Contract automatically ends once one side has achieved two consecutive victories */
    END_CONTRACT_AFTER_TWO_CONSECUTIVE_TRACKS,

    /* All costs are doubled */
    DOUBLE_ALL_COSTS,

    /* No support payments made until end of contract */
    NO_IN_CONTRACT_SUPPORT,

    /* All support payments doubled */
    DOUBLE_SUPPORT_PAYOUTS,

    /* All combat damaged removed at the end of each scenario */
    SIMULATED_DAMAGE,

    /* Combat pay is quartered */
    REDUCED_COMBAT_PAY,
}
