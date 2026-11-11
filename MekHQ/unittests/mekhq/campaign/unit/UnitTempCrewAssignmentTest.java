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
package mekhq.campaign.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Assigning a real person to a vehicle or infantry unit that carries temporary ("blob") crew fills an empty seat
 * without releasing a temp crew member, and only replaces a temp crew member when the unit was already full (issue
 * #10397).
 *
 * <p>A vehicle's temp crew are one pool shared by its driver, gunner and crew seats, so the check is made against the
 * whole unit rather than against one position.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitTempCrewAssignmentTest {
    private Campaign campaign;
    private PartsScenario scenario;

    @BeforeEach
    void setUp() {
        if (SkillType.lookupHash == null) {
            SkillType.initializeTypes();
        }
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    private Person crewMember(PersonnelRole role) {
        Person person = new Person("Crew", "Member", campaign);
        person.setPrimaryRoleDirect(role);
        campaign.importPerson(person);
        return person;
    }

    private Unit vehicleWithTempCrew(int seatsLeftEmpty) {
        Unit vehicle = scenario.withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);
        int fullCrewSize = vehicle.getFullCrewSize();
        assertTrue(fullCrewSize > 1, "the fixture needs more than one crew seat");
        vehicle.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, fullCrewSize - seatsLeftEmpty);
        return vehicle;
    }

    private Unit platoonWithTempCrew(int seatsLeftEmpty) {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        int fullCrewSize = platoon.getFullCrewSize();
        assertTrue(fullCrewSize > 1, "the fixture needs more than one trooper");
        platoon.setTempCrew(PersonnelRole.SOLDIER, fullCrewSize - seatsLeftEmpty);
        return platoon;
    }

    // region vehicles

    @Test
    void aGunnerFillingAVehicleThatIsOneShortKeepsTheTempCrew() {
        Unit vehicle = vehicleWithTempCrew(1);
        int tempCrewBefore = vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND);

        vehicle.addGunner(crewMember(PersonnelRole.VEHICLE_CREW_GROUND));

        assertEquals(tempCrewBefore, vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
        assertTrue(vehicle.isFullyCrewed(), "the person filled the empty seat");
    }

    @Test
    void aDriverFillingAVehicleThatIsOneShortKeepsTheTempCrew() {
        Unit vehicle = vehicleWithTempCrew(1);
        int tempCrewBefore = vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND);

        // The temp crew already cover the driver's seat; the empty seat is elsewhere in the shared pool
        vehicle.addDriver(crewMember(PersonnelRole.VEHICLE_CREW_GROUND));

        assertEquals(tempCrewBefore, vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
        assertTrue(vehicle.isFullyCrewed(), "the person filled the empty seat");
    }

    /**
     * The MASH truck case: support staff join a vehicle through {@link Unit#addVesselCrew(Person, boolean)}.
     */
    @Test
    void vesselCrewFillingAVehicleThatIsOneShortKeepsTheTempCrew() {
        Unit vehicle = vehicleWithTempCrew(1);
        int tempCrewBefore = vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND);

        vehicle.addVesselCrew(crewMember(PersonnelRole.DOCTOR));

        assertEquals(tempCrewBefore, vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
        assertTrue(vehicle.isFullyCrewed(), "the person filled the empty seat");
    }

    @Test
    void aGunnerJoiningAFullVehicleReplacesOneTempCrewMember() {
        Unit vehicle = vehicleWithTempCrew(0);
        int tempCrewBefore = vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND);

        vehicle.addGunner(crewMember(PersonnelRole.VEHICLE_CREW_GROUND));

        assertEquals(tempCrewBefore - 1, vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
        assertTrue(vehicle.isFullyCrewed(), "the vehicle has exactly as many crew as seats");
    }

    // endregion vehicles

    // region infantry

    @Test
    void aSoldierFillingAPlatoonThatIsOneShortKeepsTheTempTroopers() {
        Unit platoon = platoonWithTempCrew(1);
        int tempCrewBefore = platoon.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER);

        platoon.addPilotOrSoldier(crewMember(PersonnelRole.SOLDIER));

        assertEquals(tempCrewBefore, platoon.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
        assertTrue(platoon.isFullyCrewed(), "the soldier filled the empty place");
    }

    @Test
    void aSoldierJoiningAFullPlatoonReplacesOneTempTrooper() {
        Unit platoon = platoonWithTempCrew(0);
        int tempCrewBefore = platoon.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER);

        platoon.addPilotOrSoldier(crewMember(PersonnelRole.SOLDIER));

        assertEquals(tempCrewBefore - 1, platoon.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
        assertTrue(platoon.isFullyCrewed(), "the platoon has exactly as many troopers as places");
    }

    /**
     * The temp crew released are the seat's, not the newcomer's primary role's: a doctor who also soldiers still
     * replaces one temp trooper, rather than leaving the platoon over strength.
     */
    @Test
    void someoneServingThroughTheirSecondaryRoleStillReplacesATempTrooper() {
        Unit platoon = platoonWithTempCrew(0);
        int tempCrewBefore = platoon.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER);
        Person doctorWhoSoldiers = crewMember(PersonnelRole.DOCTOR);
        doctorWhoSoldiers.setSecondaryRoleDirect(PersonnelRole.SOLDIER);

        platoon.addPilotOrSoldier(doctorWhoSoldiers);

        assertEquals(tempCrewBefore - 1, platoon.getTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
        assertTrue(platoon.isFullyCrewed(), "the platoon has exactly as many troopers as places");
    }

    // endregion infantry
}
