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
package mekhq.campaign.utilities;

import java.util.Collection;

import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SpaceStation;
import mekhq.campaign.mission.utilities.TransportCostCalculations;
import mekhq.campaign.unit.Unit;

/**
 * Determines whether a traveling fleet can benefit from Lithium-Fusion (LF) batteries.
 *
 * <p>A jump only benefits from LF batteries when every ship performing it has one. So the fleet qualifies only
 * when:</p>
 * <ul>
 *     <li>It includes at least one active (not mothballed or salvage) JumpShip or WarShip.</li>
 *     <li>Every active JumpShip or WarShip has an undamaged LF battery and a working drive.</li>
 *     <li>The fleet's own docking collars carry everything, so no hired (NPC) JumpShips are needed.</li>
 * </ul>
 *
 * <p>Space stations fitted with a KF adapter, or modular stations, are carried rather than jumping under their own
 * power, so they are not treated as jumping ships.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class LithiumFusionBatteries {
    private LithiumFusionBatteries() {
    }

    /**
     * Checks whether the traveling fleet qualifies for the Lithium-Fusion battery benefit.
     *
     * @param travelingUnits         the units traveling with the fleet
     * @param transportCalculations the transport calculations for the same units, used to determine whether any hired
     *                               JumpShips are needed. If they haven't been run yet, this method runs them.
     *
     * @return {@code true} if every jumping ship is player-owned and has a working LF battery
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isFleetEligible(Collection<Unit> travelingUnits,
          TransportCostCalculations transportCalculations) {
        boolean hasEligibleShip = false;
        for (Unit unit : travelingUnits) {
            if (unit.isMothballed() || unit.isSalvage()) {
                continue;
            }

            Entity entity = unit.getEntity();
            if (!(entity instanceof Jumpship jumpship) || isCarriedStation(jumpship)) {
                continue;
            }

            if (!hasWorkingBattery(jumpship)) {
                return false;
            }
            hasEligibleShip = true;
        }

        if (!hasEligibleShip) {
            return false;
        }

        // The transport calculations aren't idempotent, so only run them if nobody has yet
        if (transportCalculations.getTotalCost() == null) {
            transportCalculations.calculateJumpCostForEachDay();
        }
        return transportCalculations.getJumpShipsRequired() <= 0;
    }

    /**
     * @return {@code true} if the ship can jump and carries an undamaged Lithium-Fusion battery
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean hasWorkingBattery(Jumpship jumpship) {
        return jumpship.hasLF() && !jumpship.getLFBatteryHit() && jumpship.canJump();
    }

    /**
     * @return {@code true} if the station is carried by other ships rather than jumping under its own power
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isCarriedStation(Jumpship jumpship) {
        return (jumpship instanceof SpaceStation spaceStation)
                     && (spaceStation.hasKFAdapter() || spaceStation.isModular());
    }
}
