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
package mekhq.campaign.unit.crewAssignment;

/**
 * The kinds of crew seat a unit can have. Each type is filled through one {@link mekhq.campaign.unit.Unit} method, so
 * the type alone decides how a staged assignment is applied.
 *
 * <p>The names describe the mechanism, not the in-game title: a VTOL's or DropShip's pilot sits in a {@link #DRIVER}
 * seat, because those units take their pilot through {@code addDriver}, while a 'Mek's pilot sits in a {@link #PILOT}
 * seat, taken through {@code addPilotOrSoldier}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum SeatType {
    /** A 'Mek, ProtoMek or aerospace fighter pilot; filled with {@code addPilotOrSoldier}. */
    PILOT,
    /** A vehicle driver, or a VTOL, conventional fighter or vessel pilot; filled with {@code addDriver}. */
    DRIVER,
    /** A gunner on a unit whose pilot does not also fire its weapons; filled with {@code addGunner}. */
    GUNNER,
    /** General vessel or vehicle crew; filled with {@code addVesselCrew}. */
    CREW,
    /** A conventional infantry soldier or battle armor trooper; filled with {@code addPilotOrSoldier}. */
    SOLDIER,
    /** A JumpShip or WarShip navigator; filled with {@code setNavigator}. */
    NAVIGATOR,
    /** A cockpit tech officer, who must be able to both drive and gun the unit; filled with {@code setTechOfficer}. */
    TECH_OFFICER,
    /** A vehicle command console's secondary commander, who must drive or gun it; filled with {@code setTechOfficer}. */
    CONSOLE_COMMANDER;

    /**
     * @return {@code true} if this seat holds at most one person
     */
    public boolean isSingleSeat() {
        return (this == NAVIGATOR) || (this == TECH_OFFICER) || (this == CONSOLE_COMMANDER);
    }
}
