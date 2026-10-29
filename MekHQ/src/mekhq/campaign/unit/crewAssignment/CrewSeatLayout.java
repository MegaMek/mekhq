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

import static mekhq.utilities.EntityUtilities.isUnsupportedEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SmallCraft;
import megamek.common.units.Tank;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

/**
 * Works out the crew seats a unit has, from the crew counts {@link Unit} already keeps. It reads the unit and changes
 * nothing.
 *
 * <p>The seats match what the Personnel and Hangar tab assignment menus offer, with one deliberate difference: a unit
 * whose pilot also fires its weapons (see {@link Unit#usesSoloPilot()}) has no separate gunner seat, so a solo
 * conventional fighter is never offered a second person as its gunner.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CrewSeatLayout {
    private CrewSeatLayout() {
    }

    /**
     * @param unit the unit
     *
     * @return the unit's seats in the order the player should see them (pilot or driver first, then gunners, crew,
     *       navigator and tech officer), or an empty list for a unit that takes no crew
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<CrewSeat> forUnit(final Unit unit) {
        if (!takesCrew(unit)) {
            return List.of();
        }

        final Entity entity = unit.getEntity();
        final List<CrewSeat> seats = new ArrayList<>();

        if (unit.usesSoldiers()) {
            final PersonnelRole soldierRole = unit.isConventionalInfantry() ?
                                                    PersonnelRole.SOLDIER :
                                                    PersonnelRole.BATTLE_ARMOUR;
            // Soldiers go into both the driver and gunner lists, and the gunner count is what limits them
            seats.add(new CrewSeat(SeatType.SOLDIER, unit.getTotalGunnerNeeds(), new ArrayList<>(unit.getGunners()),
                  unit.getTempCrewByPersonnelRole(soldierRole), soldierRole));
            return seats;
        }

        final boolean isVessel = (entity instanceof SmallCraft) || (entity instanceof Jumpship);

        if (unit.getTotalDriverNeeds() > 0) {
            final PersonnelRole tempRole = isVessel ? PersonnelRole.VESSEL_PILOT : null;
            seats.add(new CrewSeat(usesPilotSeat(entity) ? SeatType.PILOT : SeatType.DRIVER,
                  unit.getTotalDriverNeeds(), unit.getDrivers(), tempCrewOf(unit, tempRole), tempRole));
        }

        if (hasGunnerSeat(unit, entity) && (unit.getTotalGunnerNeeds() > 0)) {
            final PersonnelRole tempRole = isVessel ? PersonnelRole.VESSEL_GUNNER : null;
            seats.add(new CrewSeat(SeatType.GUNNER, unit.getTotalGunnerNeeds(), new ArrayList<>(unit.getGunners()),
                  tempCrewOf(unit, tempRole), tempRole));
        }

        if (unit.getTotalCrewNeeds() > 0) {
            final boolean isAerospaceVessel = (entity instanceof Aero) && !(entity instanceof ConvFighter);
            final PersonnelRole tempRole = isAerospaceVessel ? PersonnelRole.VESSEL_CREW : null;
            seats.add(new CrewSeat(SeatType.CREW, unit.getTotalCrewNeeds(), unit.getVesselCrew(),
                  tempCrewOf(unit, tempRole), tempRole));
        }

        if (unit.hasNavigatorSeat()) {
            seats.add(new CrewSeat(SeatType.NAVIGATOR, 1, listOf(unit.getNavigator()), 0, null));
        }

        if (unit.hasTechOfficerSeat()) {
            seats.add(new CrewSeat((entity instanceof Tank) ? SeatType.CONSOLE_COMMANDER : SeatType.TECH_OFFICER, 1,
                  listOf(unit.getTechOfficer()), 0, null));
        }

        return seats;
    }

    /**
     * @param unit the unit
     * @param type the kind of seat
     *
     * @return the unit's seat of that type, or {@code null} if it has none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable CrewSeat seatOf(final Unit unit, final SeatType type) {
        for (CrewSeat seat : forUnit(unit)) {
            if (seat.type() == type) {
                return seat;
            }
        }
        return null;
    }

    /**
     * @param unit the unit
     *
     * @return {@code true} if the unit can be crewed at all: it has a supported entity that is not an always-uncrewed
     *       type such as a handheld weapon or an unpowered trailer
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean takesCrew(final Unit unit) {
        final Entity entity = unit.getEntity();
        return (entity != null) && !isUnsupportedEntity(entity) && !unit.isNotCrewedEntityType();
    }

    /**
     * A 'Mek, ProtoMek or aerospace fighter takes its pilot through {@code addPilotOrSoldier}; everything else that has
     * a driver seat takes them through {@code addDriver}.
     */
    private static boolean usesPilotSeat(final Entity entity) {
        if ((entity instanceof Mek) || (entity instanceof ProtoMek)) {
            return true;
        }
        return (entity instanceof Aero)
                     && !(entity instanceof ConvFighter)
                     && !(entity instanceof SmallCraft)
                     && !(entity instanceof Jumpship);
    }

    private static boolean hasGunnerSeat(final Unit unit, final Entity entity) {
        if (unit.usesSoloPilot()) {
            return false;
        }
        return (entity instanceof Tank)
                     || (entity instanceof ConvFighter)
                     || (entity instanceof SmallCraft)
                     || (entity instanceof Jumpship)
                     || (entity instanceof Mek);
    }

    private static int tempCrewOf(final Unit unit, final @Nullable PersonnelRole role) {
        return (role == null) ? 0 : unit.getTempCrewByPersonnelRole(role);
    }

    private static List<Person> listOf(final @Nullable Person person) {
        return (person == null) ? List.of() : List.of(person);
    }

    /**
     * @param seats the seats
     *
     * @return how many more real people the seats can take in total
     */
    public static int openSeats(final Collection<CrewSeat> seats) {
        return seats.stream().mapToInt(CrewSeat::openSeats).sum();
    }
}
