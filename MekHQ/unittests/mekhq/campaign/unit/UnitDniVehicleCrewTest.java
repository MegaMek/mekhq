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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.compute.Compute;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.exceptions.LocationFullException;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.SkillType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A vehicle whose driver is linked to it through a VDNI or Buffered VDNI implant needs no gunners, so the driver alone
 * is a full crew (issue #9543). The link only works where the campaign's neural interface setting allows it: Pilot
 * Abilities Only needs just the implant, Full Tracking also needs the DNI Cockpit Modification, and Off never links.
 *
 * @author Illiani
 * @since 0.51.01
 */
class UnitDniVehicleCrewTest {
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

    private void playUnder(NeuralInterfaceMode mode) {
        campaign.getGameOptions().getOption(OptionsConstants.ADVANCED_NEURAL_INTERFACE_MODE)
              .setValue(mode.optionValue());
    }

    private Unit vehicle(boolean hasDniCockpitMod) throws LocationFullException {
        Unit vehicle = scenario.withUnit(UnitFixture.VEDETTE_MEDIUM_TANK);
        assertTrue(Compute.getTotalGunnerNeeds(vehicle.getEntity()) > 0, "the fixture needs gunner seats");
        if (hasDniCockpitMod) {
            vehicle.getEntity().addEquipment(EquipmentType.get(EquipmentTypeLookup.DNI_COCKPIT_MOD), Tank.LOC_BODY);
        }
        return vehicle;
    }

    private Person crewMember(String implant) {
        Person person = new Person("Crew", "Member", campaign);
        person.setPrimaryRoleDirect(PersonnelRole.VEHICLE_CREW_GROUND);
        if (implant != null) {
            person.getOptions().acquireAbility(PersonnelOptions.MD_ADVANTAGES, implant, true);
        }
        campaign.importPerson(person);
        return person;
    }

    private static void assertGunneryCovered(Unit vehicle) {
        Entity entity = vehicle.getEntity();
        assertEquals(0, vehicle.getTotalGunnerNeeds());
        assertEquals(Compute.getTotalDriverNeeds(entity) + Compute.getAdditionalNonGunner(entity),
              vehicle.getFullCrewSize());
        assertTrue(vehicle.isFullyCrewed(), "the DNI-linked driver is a full crew");
        assertNull(vehicle.checkDeployment());
    }

    private static void assertGunneryNotCovered(Unit vehicle) {
        Entity entity = vehicle.getEntity();
        assertEquals(Compute.getTotalGunnerNeeds(entity), vehicle.getTotalGunnerNeeds());
        assertEquals(Compute.getFullCrewSize(entity), vehicle.getFullCrewSize());
        assertEquals("This vehicle requires a crew of " + Compute.getFullCrewSize(entity),
              vehicle.checkDeployment());
    }

    @Test
    void aVdniDriverInAModifiedVehicleIsAFullCrewUnderFullTracking() throws LocationFullException {
        playUnder(NeuralInterfaceMode.FULL_TRACKING);
        Unit vehicle = vehicle(true);

        vehicle.addDriver(crewMember(OptionsConstants.MD_VDNI));

        assertGunneryCovered(vehicle);
    }

    @Test
    void aBufferedVdniDriverInAModifiedVehicleIsAFullCrewUnderFullTracking() throws LocationFullException {
        playUnder(NeuralInterfaceMode.FULL_TRACKING);
        Unit vehicle = vehicle(true);

        vehicle.addDriver(crewMember(OptionsConstants.MD_BVDNI));

        assertGunneryCovered(vehicle);
    }

    @Test
    void aVdniDriverInAnUnmodifiedVehicleStillNeedsGunnersUnderFullTracking() throws LocationFullException {
        playUnder(NeuralInterfaceMode.FULL_TRACKING);
        Unit vehicle = vehicle(false);

        vehicle.addDriver(crewMember(OptionsConstants.MD_VDNI));

        assertGunneryNotCovered(vehicle);
    }

    @Test
    void aVdniDriverIsAFullCrewWithoutTheModificationUnderPilotAbilitiesOnly() throws LocationFullException {
        playUnder(NeuralInterfaceMode.PILOT_ABILITIES_ONLY);
        Unit vehicle = vehicle(false);

        vehicle.addDriver(crewMember(OptionsConstants.MD_VDNI));

        assertGunneryCovered(vehicle);
    }

    @Test
    void aVdniDriverStillNeedsGunnersWhenNeuralInterfacesAreOff() throws LocationFullException {
        playUnder(NeuralInterfaceMode.OFF);
        Unit vehicle = vehicle(true);

        vehicle.addDriver(crewMember(OptionsConstants.MD_VDNI));

        assertGunneryNotCovered(vehicle);
    }

    @Test
    void aDriverWithoutAnImplantStillNeedsGunners() throws LocationFullException {
        playUnder(NeuralInterfaceMode.FULL_TRACKING);
        Unit vehicle = vehicle(true);

        vehicle.addDriver(crewMember(null));

        assertGunneryNotCovered(vehicle);
    }

    @Test
    void aVdniGunnerDoesNotCoverTheOtherGunnerSeats() throws LocationFullException {
        playUnder(NeuralInterfaceMode.FULL_TRACKING);
        Unit vehicle = vehicle(true);

        vehicle.addDriver(crewMember(null));
        vehicle.addGunner(crewMember(OptionsConstants.MD_VDNI));

        assertGunneryNotCovered(vehicle);
    }
}
