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
package mekhq.campaign.force;

/**
 * Describes the range of altitudes a force is able to fight at, derived from the composition of its units.
 *
 * <p>This is a cheap, cached summary computed once per day (see
 * {@link AbstractForce#calculateFleetAltitudeCapability(mekhq.campaign.Campaign)}) so that consumers - most notably
 * StratCon scenario generation - can restrict the scenarios they hand a dedicated aerospace outfit without rescanning
 * the whole roster on every call.</p>
 *
 * @author Illiani
 */
public enum FleetAltitudeCapability {
    /**
     * The force fields at least one ground-capable unit ('Mek, vehicle, infantry, and so on), or has no units to
     * reason about. No altitude restriction applies.
     */
    UNRESTRICTED,

    /**
     * Every unit is airborne and at least one is space-capable (an aerospace fighter, small craft, DropShip, and so
     * on). Only space and low-atmosphere scenarios are appropriate.
     */
    SPACE_AND_ATMOSPHERE,

    /**
     * Every unit is airborne but none is space-capable - the force is made up solely of conventional fighters. Only
     * low-atmosphere scenarios are appropriate.
     */
    ATMOSPHERE_ONLY
}
