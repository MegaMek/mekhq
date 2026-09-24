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
package mekhq.campaign.digitalGM.stratCon.deployment;

/**
 * The support-point cost of a paid reinforcement attempt, and the target-number modifier the spent points buy.
 *
 * <p>Reinforcement pricing works as follows: the player chooses how many support points to spend to improve the roll;
 * a base cost is added on top (one point for a normal arrival, two for an instant arrival); and the whole thing is
 * multiplied by the number of forces committed, since each force is a separate attempt. Every point the player chooses
 * to spend (as opposed to the base cost) lowers the reinforcement target number.</p>
 *
 * @param perForceSupportPoints the support points each committed force costs (the player's chosen spend plus the base
 *                              cost)
 * @param totalSupportPoints    the total support points the whole attempt costs across every committed force
 * @param targetNumberModifier  the (non-positive) modifier the chosen spend applies to the reinforcement target number
 *
 * @author Illiani
 * @since 0.51.01
 */
public record ReinforcementCost(int perForceSupportPoints, int totalSupportPoints, int targetNumberModifier) {}
