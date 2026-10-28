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

import java.util.function.Predicate;

import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * Decides who may take which crew seat. This is the one place these rules live, so the crew assignment dialog and the
 * right-click assignment menus cannot drift apart.
 *
 * <p>A role counts whether it is the person's primary or secondary role. The rules only ask whether the person is
 * qualified: whether the seat is free, and whether the person and unit are at the same location, are checked when an
 * assignment is staged and again when it is applied.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class SeatEligibility {
    private SeatEligibility() {
    }

    /**
     * Whether a person can be given any crew seat at all: they must be active, employed, not a current prisoner
     * (bondsmen may serve), and not deployed to a scenario.
     *
     * @param person the person
     *
     * @return {@code true} if the person can be assigned
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isAssignable(final Person person) {
        return person.getStatus().isActive()
                     && person.isEmployed()
                     && !person.getPrisonerStatus().isCurrentPrisoner()
                     && !person.isDeployed();
    }

    /**
     * Whether a person is qualified for a seat on a unit. This does not check whether the seat has room.
     *
     * @param person                the person
     * @param unit                  the unit
     * @param type                  the seat
     * @param useAltAdvancedMedical the campaign's alternative advanced medical option, which affects who can drive
     * @param useImplants           the campaign's implants option, which affects who can drive
     *
     * @return {@code true} if the person may take the seat
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean canTake(final Person person, final Unit unit, final SeatType type,
          final boolean useAltAdvancedMedical, final boolean useImplants) {
        final Entity entity = unit.getEntity();
        if ((entity == null) || !isAssignable(person)) {
            return false;
        }

        return switch (type) {
            case PILOT -> canPilot(person, entity);
            case DRIVER -> canDriveSeat(person, entity);
            case GUNNER -> canGunSeat(person, entity);
            case CREW -> ((entity instanceof Aero) && !(entity instanceof ConvFighter)) ?
                               person.hasRole(PersonnelRole.VESSEL_CREW) :
                               hasAnyRole(person, PersonnelRole::isVehicleCrewExtended);
            case SOLDIER -> person.hasRole(unit.isConventionalInfantry() ?
                                                 PersonnelRole.SOLDIER :
                                                 PersonnelRole.BATTLE_ARMOUR);
            case NAVIGATOR -> person.hasRole(PersonnelRole.VESSEL_NAVIGATOR);
            // A tech officer works the whole cockpit, so they must be able to both drive and gun
            case TECH_OFFICER -> person.canDrive(entity, useAltAdvancedMedical, useImplants)
                                       && person.canGun(entity);
            // A console commander needs only one of the two
            case CONSOLE_COMMANDER -> person.canDrive(entity, useAltAdvancedMedical, useImplants)
                                            || person.canGun(entity);
        };
    }

    private static boolean canPilot(final Person person, final Entity entity) {
        if (entity instanceof Mek) {
            return hasAnyRole(person, PersonnelRole::isMekWarriorGrouping);
        } else if (entity instanceof ProtoMek) {
            return person.hasRole(PersonnelRole.PROTOMEK_PILOT);
        } else if (entity instanceof Aero) {
            return hasAnyRole(person, PersonnelRole::isAerospaceGrouping);
        }
        return false;
    }

    private static boolean canDriveSeat(final Person person, final Entity entity) {
        if (entity instanceof Tank) {
            return person.hasRole(vehicleCrewRole(entity));
        } else if (entity instanceof ConvFighter) {
            return person.hasRole(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return person.hasRole(PersonnelRole.VESSEL_PILOT);
        }
        return false;
    }

    private static boolean canGunSeat(final Person person, final Entity entity) {
        if (entity instanceof Tank) {
            return person.hasRole(vehicleCrewRole(entity));
        } else if (entity instanceof ConvFighter) {
            return person.hasRole(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return person.hasRole(PersonnelRole.VESSEL_GUNNER);
        } else if (entity instanceof Mek) {
            return hasAnyRole(person, PersonnelRole::isMekWarriorGrouping);
        }
        return false;
    }

    /** A vehicle's drivers and gunners share one role: VTOL, naval or ground vehicle crew. */
    private static PersonnelRole vehicleCrewRole(final Entity entity) {
        if (entity instanceof VTOL) {
            return PersonnelRole.VEHICLE_CREW_VTOL;
        } else if (entity.getMovementMode().isMarine()) {
            return PersonnelRole.VEHICLE_CREW_NAVAL;
        }
        return PersonnelRole.VEHICLE_CREW_GROUND;
    }

    private static boolean hasAnyRole(final Person person, final Predicate<PersonnelRole> test) {
        return test.test(person.getPrimaryRole()) || test.test(person.getSecondaryRole());
    }
}
