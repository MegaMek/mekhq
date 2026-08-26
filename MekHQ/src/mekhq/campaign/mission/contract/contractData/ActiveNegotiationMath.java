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
package mekhq.campaign.mission.contract.contractData;

import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;

/**
 * Pure scoring behind active negotiation: turning the two negotiators' opposed {@link MarginOfSuccess} results into a
 * single net margin. Kept free of dice and GUI so it can be tested in isolation.
 *
 * <p>Each margin is scored on a scale centered on {@link MarginOfSuccess#BARELY_MADE_IT} (zero), with better results
 * positive and worse results negative. The net margin is the player's score minus the employer's: a positive net means
 * the player's negotiator came out ahead by that many margin-of-success levels (improving that many terms), a negative
 * net means the employer did (lowering that many).</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ActiveNegotiationMath {
    private ActiveNegotiationMath() {
    }

    /**
     * The signed level score for a margin: {@code 0} at {@link MarginOfSuccess#BARELY_MADE_IT}, positive for better
     * results, negative for worse. Derived from enum position so it stays correct if labels change.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int marginScore(MarginOfSuccess margin) {
        // The enum is ordered best-first (SPECTACULAR at ordinal 0), so a lower ordinal is a better result.
        return MarginOfSuccess.BARELY_MADE_IT.ordinal() - margin.ordinal();
    }

    /**
     * The net margin of an opposed negotiation: the player's level score minus the employer's. Positive favors the
     * player (that many terms improve), negative favors the employer (that many lower), zero is a stalemate.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int netMargin(MarginOfSuccess playerMargin, MarginOfSuccess employerMargin) {
        return marginScore(playerMargin) - marginScore(employerMargin);
    }
}
