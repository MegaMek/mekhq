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

package mekhq.campaign.parts;

/**
 * The source of random rolls for part destruction checks.
 *
 * <p>Parts reach it through {@code Campaign.getDice()} rather than calling MegaMek's {@code Compute} directly, so
 * that a test can supply a fixed sequence of rolls and pin down which parts survive. The default implementation,
 * {@link CampaignDice}, delegates to {@code Compute} exactly as the parts did before.</p>
 */
public interface Dice {
    /**
     * Rolls a number of six-sided dice and returns their total.
     *
     * @param numberOfDice how many six-sided dice to roll
     *
     * @return the sum of the dice
     */
    int d6(int numberOfDice);

    /**
     * Returns a random integer from {@code 0} (inclusive) to {@code maximumValue} (exclusive).
     *
     * @param maximumValue the exclusive upper bound
     *
     * @return a random integer between {@code 0} and {@code maximumValue - 1}
     */
    int randomInt(int maximumValue);
}
