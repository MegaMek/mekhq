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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Infantry;
import megamek.common.units.Jumpship;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class SeatEligibilityTest {
    private final CrewFixture fixture = new CrewFixture();

    private static boolean canTake(Person person, Unit unit, SeatType type) {
        return SeatEligibility.canTake(person, unit, type, false, false);
    }

    private Unit tank(EntityMovementMode movementMode) {
        Tank tank = mock(Tank.class);
        when(tank.getMovementMode()).thenReturn(movementMode);
        return fixture.vehicle(tank, "Tank", 2).unit;
    }

    @Test
    void mekWarriorPilotsAMek() {
        Unit mek = fixture.soloPilotUnit(mock(Mek.class), "Mek").unit;

        assertTrue(canTake(fixture.person("Pilot", PersonnelRole.MEKWARRIOR), mek, SeatType.PILOT));
        assertFalse(canTake(fixture.person("Soldier", PersonnelRole.SOLDIER), mek, SeatType.PILOT));
    }

    @Test
    void secondaryRoleCountsAsMuchAsPrimary() {
        Unit mek = fixture.soloPilotUnit(mock(Mek.class), "Mek").unit;
        Person doctorWhoPilots = fixture.person("Doc", PersonnelRole.DOCTOR, PersonnelRole.MEKWARRIOR);

        assertTrue(canTake(doctorWhoPilots, mek, SeatType.PILOT));
    }

    @Test
    void groundVehicleCrewCannotCrewANavalVehicle() {
        Person groundCrew = fixture.person("Ground", PersonnelRole.VEHICLE_CREW_GROUND);
        Person navalCrew = fixture.person("Naval", PersonnelRole.VEHICLE_CREW_NAVAL);
        Unit tracked = tank(EntityMovementMode.TRACKED);
        Unit boat = tank(EntityMovementMode.NAVAL);

        assertTrue(canTake(groundCrew, tracked, SeatType.DRIVER));
        assertTrue(canTake(groundCrew, tracked, SeatType.GUNNER));
        assertFalse(canTake(groundCrew, boat, SeatType.DRIVER));
        assertTrue(canTake(navalCrew, boat, SeatType.DRIVER));
    }

    @Test
    void vtolTakesVtolCrewForBothSeats() {
        VTOL vtol = mock(VTOL.class);
        when(vtol.getMovementMode()).thenReturn(EntityMovementMode.VTOL);
        Unit unit = fixture.vehicle(vtol, "VTOL", 1).unit;

        assertTrue(canTake(fixture.person("VTOL", PersonnelRole.VEHICLE_CREW_VTOL), unit, SeatType.DRIVER));
        assertTrue(canTake(fixture.person("VTOL", PersonnelRole.VEHICLE_CREW_VTOL), unit, SeatType.GUNNER));
        assertFalse(canTake(fixture.person("Ground", PersonnelRole.VEHICLE_CREW_GROUND), unit, SeatType.DRIVER));
    }

    @Test
    void vesselSeatsEachTakeTheirOwnRole() {
        Unit dropShip = fixture.vessel(mock(Dropship.class), "DropShip", 2, 4, 10).unit;
        Person pilot = fixture.person("Pilot", PersonnelRole.VESSEL_PILOT);
        Person gunner = fixture.person("Gunner", PersonnelRole.VESSEL_GUNNER);
        Person crew = fixture.person("Crew", PersonnelRole.VESSEL_CREW);

        assertTrue(canTake(pilot, dropShip, SeatType.DRIVER));
        assertFalse(canTake(pilot, dropShip, SeatType.GUNNER));
        assertTrue(canTake(gunner, dropShip, SeatType.GUNNER));
        assertFalse(canTake(gunner, dropShip, SeatType.DRIVER));
        assertTrue(canTake(crew, dropShip, SeatType.CREW));
        assertFalse(canTake(pilot, dropShip, SeatType.CREW));
    }

    @Test
    void navigatorSeatTakesAVesselNavigator() {
        Unit jumpShip = fixture.vessel(mock(Jumpship.class), "JumpShip", 1, 2, 10).unit;

        assertTrue(canTake(fixture.person("Nav", PersonnelRole.VESSEL_NAVIGATOR), jumpShip, SeatType.NAVIGATOR));
        assertFalse(canTake(fixture.person("Pilot", PersonnelRole.VESSEL_PILOT), jumpShip, SeatType.NAVIGATOR));
    }

    @Test
    void infantryTakesSoldiersAndBattleArmorTakesTroopers() {
        Unit platoon = fixture.infantry(mock(Infantry.class), "Platoon", true, 28).unit;
        Unit squad = fixture.infantry(mock(BattleArmor.class), "Squad", false, 5).unit;
        Person soldier = fixture.person("Soldier", PersonnelRole.SOLDIER);
        Person trooper = fixture.person("Trooper", PersonnelRole.BATTLE_ARMOUR);

        assertTrue(canTake(soldier, platoon, SeatType.SOLDIER));
        assertFalse(canTake(trooper, platoon, SeatType.SOLDIER));
        assertTrue(canTake(trooper, squad, SeatType.SOLDIER));
        assertFalse(canTake(soldier, squad, SeatType.SOLDIER));
    }

    @Test
    void techOfficerMustDriveAndGunButConsoleCommanderNeedsOnlyOne() {
        Unit mek = fixture.soloPilotUnit(mock(Mek.class), "Mek").unit;
        Person drivesOnly = fixture.person("Driver", PersonnelRole.MEKWARRIOR);
        when(drivesOnly.canDrive(any(), anyBoolean(), anyBoolean())).thenReturn(true);
        when(drivesOnly.canGun(any())).thenReturn(false);
        Person both = fixture.person("Both", PersonnelRole.MEKWARRIOR);
        when(both.canDrive(any(), anyBoolean(), anyBoolean())).thenReturn(true);
        when(both.canGun(any())).thenReturn(true);

        assertFalse(canTake(drivesOnly, mek, SeatType.TECH_OFFICER));
        assertTrue(canTake(both, mek, SeatType.TECH_OFFICER));
        assertTrue(canTake(drivesOnly, tank(EntityMovementMode.TRACKED), SeatType.CONSOLE_COMMANDER));
    }

    @Test
    void prisonersInactivePeopleAndTheDeployedCannotBeAssigned() {
        Unit mek = fixture.soloPilotUnit(mock(Mek.class), "Mek").unit;
        Person prisoner = fixture.person("Prisoner", PersonnelRole.MEKWARRIOR);
        when(prisoner.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);
        Person retired = fixture.person("Retired", PersonnelRole.MEKWARRIOR);
        when(retired.getStatus()).thenReturn(PersonnelStatus.RETIRED);
        Person deployed = fixture.person("Deployed", PersonnelRole.MEKWARRIOR);
        when(deployed.isDeployed()).thenReturn(true);
        Person bondsman = fixture.person("Bondsman", PersonnelRole.MEKWARRIOR);
        when(bondsman.getPrisonerStatus()).thenReturn(PrisonerStatus.BONDSMAN);

        assertFalse(canTake(prisoner, mek, SeatType.PILOT));
        assertFalse(canTake(retired, mek, SeatType.PILOT));
        assertFalse(canTake(deployed, mek, SeatType.PILOT));
        assertTrue(canTake(bondsman, mek, SeatType.PILOT));
    }

    @Test
    void theQualifyingRoleDecidesWhichExperienceLevelCounts() {
        Unit mek = fixture.soloPilotUnit(mock(Mek.class), "Mek").unit;
        Person pilot = fixture.person("Pilot", PersonnelRole.MEKWARRIOR, PersonnelRole.DOCTOR);
        Person doctorWhoPilots = fixture.person("Doc", PersonnelRole.DOCTOR, PersonnelRole.MEKWARRIOR);

        assertTrue(SeatEligibility.qualifiesByPrimaryRole(pilot, mek, SeatType.PILOT));
        assertFalse(SeatEligibility.qualifiesByPrimaryRole(doctorWhoPilots, mek, SeatType.PILOT));
    }

    @Test
    void seatsFilledByAbilityAreNotJudgedByRole() {
        Unit mek = fixture.soloPilotUnit(mock(Mek.class), "Mek").unit;
        Person anyone = fixture.person("Anyone", PersonnelRole.DOCTOR);

        assertFalse(SeatEligibility.roleFits(PersonnelRole.MEKWARRIOR, mek, SeatType.TECH_OFFICER));
        assertTrue(SeatEligibility.qualifiesByPrimaryRole(anyone, mek, SeatType.TECH_OFFICER));
    }

    @Test
    void lamPilotsFitBothMeksAndAerospaceFighters() {
        Unit mek = fixture.soloPilotUnit(mock(Mek.class), "Mek").unit;
        Unit fighter = fixture.soloPilotUnit(mock(AeroSpaceFighter.class), "Fighter").unit;

        assertTrue(SeatEligibility.roleFits(PersonnelRole.LAM_PILOT, mek, SeatType.PILOT));
        assertTrue(SeatEligibility.roleFits(PersonnelRole.LAM_PILOT, fighter, SeatType.PILOT));
        assertFalse(SeatEligibility.roleFits(PersonnelRole.MEKWARRIOR, fighter, SeatType.PILOT));
    }
}
