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
package mekhq.campaign.universe.companyGeneration.ratgen;

import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.UnitType;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Maps a MegaMek {@link UnitType} integer (and optionally the live {@link Entity}) to the
 * {@link PersonnelRole} a primary crew member should hold, plus the role used for any extra gunner /
 * vessel-crew / navigator seats on the same unit.
 *
 * <p>The table here is the single source of truth for "what role does the commander of a tank get?",
 * "what role do the gunners of a DropShip get?", etc. Phase 2 fills in every {@code UnitType} value;
 * the resolver falls back to {@link PersonnelRole#MEKWARRIOR} for unknown ints so a bad descriptor
 * doesn't crash the pipeline.</p>
 *
 * <p>MekHQ uses a unified {@code VEHICLE_CREW_*} role for both drivers and gunners on ground / VTOL /
 * naval vehicles, separate {@code VESSEL_*} roles for the seats on large craft, and {@code SOLDIER} /
 * {@code BATTLE_ARMOUR} for infantry squads (every member added via {@code addPilotOrSoldier}).</p>
 */
public final class PersonnelRoleResolver {

    private PersonnelRoleResolver() {
        // utility class
    }

    /**
     * Returns the primary crew role for a unit of the given type. The commander descriptor (from
     * {@code ForceDescriptor.getCo()}) is applied to this Person.
     *
     * @param unitType the {@link UnitType} integer of the unit
     * @return the primary {@link PersonnelRole}; never {@code null}
     */
    public static PersonnelRole primaryRole(int unitType) {
        return primaryRole(unitType, null);
    }

    /**
     * Returns the primary crew role for a unit, with an optional {@link Entity} to refine the lookup
     * (e.g. distinguishing LAMs from ordinary Meks).
     *
     * @param unitType the {@link UnitType} integer of the unit
     * @param entity the live entity, or {@code null} if not yet available
     * @return the primary {@link PersonnelRole}; never {@code null}
     */
    public static PersonnelRole primaryRole(int unitType, Entity entity) {
        if (entity instanceof LandAirMek) {
            return PersonnelRole.LAM_PILOT;
        }
        return switch (unitType) {
            case UnitType.MEK -> PersonnelRole.MEKWARRIOR;
            case UnitType.TANK, UnitType.GUN_EMPLACEMENT -> PersonnelRole.VEHICLE_CREW_GROUND;
            case UnitType.BATTLE_ARMOR -> PersonnelRole.BATTLE_ARMOUR;
            case UnitType.INFANTRY -> PersonnelRole.SOLDIER;
            case UnitType.PROTOMEK -> PersonnelRole.PROTOMEK_PILOT;
            case UnitType.VTOL -> PersonnelRole.VEHICLE_CREW_VTOL;
            case UnitType.NAVAL -> PersonnelRole.VEHICLE_CREW_NAVAL;
            case UnitType.CONV_FIGHTER -> PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT;
            case UnitType.AEROSPACE_FIGHTER, UnitType.AERO -> PersonnelRole.AEROSPACE_PILOT;
            case UnitType.SMALL_CRAFT, UnitType.DROPSHIP, UnitType.JUMPSHIP, UnitType.WARSHIP,
                  UnitType.SPACE_STATION -> PersonnelRole.VESSEL_PILOT;
            default -> PersonnelRole.MEKWARRIOR;
        };
    }

    /**
     * Returns the role assigned to additional gunner seats on a multi-crew unit (tanks with separate
     * gunner stations, large craft turrets, etc.). For unified-crew vehicles this is the same role as
     * the driver; for vessels it's {@link PersonnelRole#VESSEL_GUNNER}.
     */
    public static PersonnelRole gunnerRole(int unitType) {
        return switch (unitType) {
            case UnitType.SMALL_CRAFT, UnitType.DROPSHIP, UnitType.JUMPSHIP, UnitType.WARSHIP,
                  UnitType.SPACE_STATION -> PersonnelRole.VESSEL_GUNNER;
            default -> primaryRole(unitType, null);
        };
    }

    /**
     * Returns the role assigned to the generic engineering / utility crew slots on large craft
     * (the seats that aren't drivers, gunners, or the navigator).
     */
    public static PersonnelRole vesselCrewRole() {
        return PersonnelRole.VESSEL_CREW;
    }

    /**
     * Returns the role assigned to the navigator on a JumpShip / WarShip.
     */
    public static PersonnelRole navigatorRole() {
        return PersonnelRole.VESSEL_NAVIGATOR;
    }
}
