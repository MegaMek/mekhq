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
package mekhq.campaign.roleplay;

import mekhq.campaign.personnel.skills.ActionCheckRoll.RollType;

/**
 * The number a check has to roll on 2d6, and which way it counts.
 *
 * @param value      the target number, including modifiers
 * @param countUp    {@code true} for a skill that succeeds at or under the target, rather than at or over it
 * @param impossible {@code true} if the check cannot succeed whatever is rolled
 * @param rollType   how the dice are rolled: two dice, or three keeping two for natural aptitude
 */
public record CheckTarget(int value, boolean countUp, boolean impossible, RollType rollType) {
    /** The largest margin of success or failure a check can have, as for skill checks elsewhere in MekHQ. */
    public static final int MAXIMUM_MARGIN = 10;

    /**
     * @param value the target number
     *
     * @return a target for an ordinary two-dice roll that succeeds at or over {@code value}
     */
    public static CheckTarget of(final int value) {
        return new CheckTarget(value, false, false, RollType.NORMAL);
    }

    /**
     * @param roll the total rolled
     *
     * @return how far the roll beat the target, or a negative number for a failure, between −10 and 10
     */
    public int margin(final int roll) {
        if (impossible) {
            return -MAXIMUM_MARGIN;
        }
        long difference = countUp ? (long) value - roll : (long) roll - value;
        return (int) Math.clamp(difference, -MAXIMUM_MARGIN, MAXIMUM_MARGIN);
    }

    /**
     * @param roll the total rolled
     *
     * @return {@code true} if the roll succeeds
     */
    public boolean succeeds(final int roll) {
        return margin(roll) >= 0;
    }

    /**
     * @return {@code true} if some roll of the dice succeeds, so spending Edge on a re-roll could help
     */
    public boolean canSucceed() {
        return !impossible && (countUp ? value >= 2 : value <= 12);
    }

    /**
     * @return the best margin any roll of the dice can reach
     */
    public int bestMargin() {
        return margin(countUp ? 2 : 12);
    }

    /**
     * @return the target as players read it, such as "7+", or "7-" for a skill that counts up
     */
    public String describe() {
        if (impossible) {
            return "X";
        }
        return value + (countUp ? "-" : "+");
    }
}
