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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * {@link Unit#getActiveCrew()} keeps every crew member whose status is Active, injured or not.
 *
 * <p>Injured vehicle and infantry crew used to be left out. The unit then looked one short, the daily temp crew fill
 * seated a temp on top of them, and once they healed the unit was over-crewed, which {@link Unit#isFullyCrewed()}
 * treats as not fully crewed (for example, a field kitchen stopped counting). Units already in that state are fixed by
 * {@link mekhq.campaign.ForceHumanResources#returnSurplusTempCrewToPool(mekhq.campaign.Campaign)} on each new day.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitActiveCrewTest {
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

    /**
     * @return a vehicle with every seat but one held by temp crew
     */
    private Unit vehicleOneShort() {
        Unit vehicle = scenario.withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);
        int fullCrewSize = vehicle.getFullCrewSize();
        assertTrue(fullCrewSize > 1, "the fixture needs more than one crew seat");
        vehicle.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, fullCrewSize - 1);
        return vehicle;
    }

    @Test
    void anInjuredVehicleDriverStillCountsAsCrew() {
        Unit vehicle = vehicleOneShort();
        Person driver = crewMember(PersonnelRole.VEHICLE_CREW_GROUND);
        vehicle.addDriver(driver);

        driver.setHits(2);

        assertTrue(vehicle.getActiveCrew().contains(driver));
        assertTrue(vehicle.isFullyCrewed(), "the injured driver still holds their seat");
    }

    @Test
    void anInjuredVehicleGunnerStillCountsAsCrew() {
        Unit vehicle = vehicleOneShort();
        Person gunner = crewMember(PersonnelRole.VEHICLE_CREW_GROUND);
        vehicle.addGunner(gunner);

        gunner.setHits(2);

        assertTrue(vehicle.getActiveCrew().contains(gunner));
        assertTrue(vehicle.isFullyCrewed(), "the injured gunner still holds their seat");
    }

    @Test
    void anInjuredSoldierStillCountsAsCrew() {
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_LASER);
        int fullCrewSize = platoon.getFullCrewSize();
        assertTrue(fullCrewSize > 1, "the fixture needs more than one trooper");
        platoon.setTempCrew(PersonnelRole.SOLDIER, fullCrewSize - 1);
        Person soldier = crewMember(PersonnelRole.SOLDIER);
        platoon.addPilotOrSoldier(soldier);

        soldier.setHits(2);

        assertTrue(platoon.getActiveCrew().contains(soldier));
        assertTrue(platoon.isFullyCrewed(), "the injured soldier still holds their place");
    }

    /**
     * The field kitchen report: a temp covered for a sick driver, the driver recovered, and the unit sat one over its
     * seats. The new day trim takes the extra temp off.
     */
    @Test
    void surplusTempCrewIsTakenOffAnOverCrewedUnit() {
        Unit vehicle = vehicleOneShort();
        vehicle.addDriver(crewMember(PersonnelRole.VEHICLE_CREW_GROUND));
        int fullCrewSize = vehicle.getFullCrewSize();
        vehicle.setTempCrew(PersonnelRole.VEHICLE_CREW_GROUND, fullCrewSize);
        assertFalse(vehicle.isFullyCrewed(), "the vehicle starts one over its seats");

        campaign.getPlayerForce().getHumanResources().returnSurplusTempCrewToPool(campaign);

        assertEquals(fullCrewSize - 1, vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
        assertTrue(vehicle.isFullyCrewed());
    }

    @Test
    void tempCrewOnAUnitWithNoSurplusAreLeftAlone() {
        Unit vehicle = vehicleOneShort();
        vehicle.addDriver(crewMember(PersonnelRole.VEHICLE_CREW_GROUND));
        int tempCrewBefore = vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND);

        campaign.getPlayerForce().getHumanResources().returnSurplusTempCrewToPool(campaign);

        assertEquals(tempCrewBefore, vehicle.getTempCrewByPersonnelRole(PersonnelRole.VEHICLE_CREW_GROUND));
        assertTrue(vehicle.isFullyCrewed());
    }

    @Test
    void crewWhoAreNotActiveAreLeftOut() {
        Unit vehicle = vehicleOneShort();
        Person driver = crewMember(PersonnelRole.VEHICLE_CREW_GROUND);
        vehicle.addDriver(driver);

        driver.setStatus(PersonnelStatus.MIA);

        assertFalse(vehicle.getActiveCrew().contains(driver));
        assertFalse(vehicle.isFullyCrewed(), "a missing driver does not hold their seat");
    }
}
