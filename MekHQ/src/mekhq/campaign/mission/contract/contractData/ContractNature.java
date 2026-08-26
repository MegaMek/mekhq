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

import megamek.logging.MMLogger;

/**
 * The special nature of a contract, if any. These designations are mutually exclusive: a contract is at most one of
 * them, so a contract cannot be both a Proving Ground and a covert operation. When generation could assign more than
 * one, the more deliberate designation wins (a Proving Ground is an intentional market top-up and is never overridden
 * by a random covert roll).
 */
public enum ContractNature {
    /** An ordinary contract with no special designation. */
    NORMAL,
    /**
     * A "pity" contract: an easy top-up offer (a veteran ally against a green enemy), surfaced in the market as a
     * Proving Ground for a struggling force.
     */
    PROVING_GROUND,
    /**
     * A covert operation: the enemy is drawn under covert rules, where even the employer's allies can become rare,
     * low-chance targets rather than being excluded outright (espionage and sabotage don't respect alliances the way
     * open warfare does).
     */
    COVERT;

    private static final MMLogger LOGGER = MMLogger.create(ContractNature.class);

    /** @return {@code true} if this is the {@link #PROVING_GROUND} designation */
    public boolean isProvingGround() {
        return this == PROVING_GROUND;
    }

    /** @return {@code true} if this is the {@link #COVERT} designation */
    public boolean isCovert() {
        return this == COVERT;
    }

    /**
     * Parses a saved nature name, falling back to {@link #NORMAL} for a blank or unrecognized value so that older or
     * hand-edited save data never fails to load.
     *
     * @param text the stored enum name
     *
     * @return the matching nature, or {@link #NORMAL} if none matches
     */
    public static ContractNature fromString(String text) {
        if ((text == null) || text.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(text.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Unknown ContractNature '{}'. Defaulting to NORMAL.", text);
            return NORMAL;
        }
    }
}
