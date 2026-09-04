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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.UUID;

import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the aero-vessel temp ("blob") crew rules on {@link Unit}:
 *
 * <ul>
 *     <li>{@link Unit#hasRealCrewInVesselRole(PersonnelRole)} and
 *         {@link Unit#getEffectiveTempCrewByPersonnelRole(PersonnelRole)} implement the rule that a vessel role must
 *         contain at least one real crew member before its temp crew counts.</li>
 *     <li>{@link Unit#addDriver(Person, boolean)}, {@link Unit#addGunner(Person, boolean)} and
 *         {@link Unit#addVesselCrew(Person, boolean)} release one matching temp crew member when a real crew member is
 *         assigned.</li>
 * </ul>
 *
 * <p>Each add method also fires a {@code PersonCrewAssignmentEvent}, whose {@code removeExcessTempCrew()} trims temp
 * crew down to {@link Unit#getFullCrewSize()}. To isolate the behaviour under test, the fixture stubs
 * {@code getFullCrewSize()} to a large value so that event is a no-op.</p>
 */
class VesselTempCrewTest {
    private static final LocalDate DATE = LocalDate.of(3067, 1, 1);

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        when(campaign.getLocalDate()).thenReturn(DATE);
    }

    /**
     * Builds a spied unit whose {@code resetPilotAndEntity()} is stubbed out, and whose {@code getFullCrewSize()}
     * returns a large value so the {@code PersonCrewAssignmentEvent}'s excess-trimming does not interfere with the
     * behaviour being tested.
     */
    private Unit unitFor(Entity entity) {
        Unit unit = spy(new Unit(entity, campaign));
        when(unit.getId()).thenReturn(UUID.randomUUID());
        doNothing().when(unit).resetPilotAndEntity();
        doReturn("Test Vessel").when(unit).getName();
        doReturn(1000).when(unit).getFullCrewSize();
        return unit;
    }

    /**
     * A dropship mock whose {@code isLargeCraft()} is true, so {@code getDriverRole()}/{@code getGunnerRole()} resolve
     * to the vessel roles and assignment releases the matching temp crew.
     */
    private Entity dropship() {
        Dropship entity = mock(Dropship.class);
        when(entity.isLargeCraft()).thenReturn(true);
        return entity;
    }

    private Person mockPerson() {
        Person person = mock(Person.class);
        UUID id = UUID.randomUUID();
        when(person.getId()).thenReturn(id);
        when(person.getFullName()).thenReturn("Crew Member");
        when(campaign.getPlayerForce().getHumanResources().getPerson(eq(id))).thenReturn(person);
        return person;
    }

    // region hasRealCrewInVesselRole

    @Test
    void vesselRoleHasNoRealCrewWhenTheRelevantListIsEmpty() {
        Unit unit = unitFor(mock(Entity.class));

        assertFalse(unit.hasRealCrewInVesselRole(PersonnelRole.VESSEL_PILOT));
        assertFalse(unit.hasRealCrewInVesselRole(PersonnelRole.VESSEL_GUNNER));
        assertFalse(unit.hasRealCrewInVesselRole(PersonnelRole.VESSEL_CREW));
    }

    @Test
    void rolesOutsideTheRuleAreNeverGated() {
        Unit unit = unitFor(mock(Entity.class));

        // Non-vessel roles must not be affected: they always report as satisfied.
        assertTrue(unit.hasRealCrewInVesselRole(PersonnelRole.MEKWARRIOR));
        assertTrue(unit.hasRealCrewInVesselRole(PersonnelRole.SOLDIER));
        assertTrue(unit.hasRealCrewInVesselRole(PersonnelRole.VESSEL_NAVIGATOR));
    }

    @Test
    void addingARealDriverSatisfiesThePilotRole() {
        // A plain entity has no known driver role, so no temp crew is released; we only care that the driver is added.
        Unit unit = unitFor(mock(Entity.class));

        unit.addDriver(mockPerson());

        assertTrue(unit.hasRealCrewInVesselRole(PersonnelRole.VESSEL_PILOT));
    }

    @Test
    void addingARealGunnerSatisfiesTheGunnerRole() {
        Unit unit = unitFor(mock(Entity.class));

        unit.addGunner(mockPerson());

        assertTrue(unit.hasRealCrewInVesselRole(PersonnelRole.VESSEL_GUNNER));
    }

    @Test
    void addingRealVesselCrewSatisfiesTheCrewRole() {
        Unit unit = unitFor(mock(Entity.class));

        unit.addVesselCrew(mockPerson());

        assertTrue(unit.hasRealCrewInVesselRole(PersonnelRole.VESSEL_CREW));
    }

    // endregion hasRealCrewInVesselRole

    // region getEffectiveTempCrewByPersonnelRole

    @Test
    void tempPilotsDoNotCountWithoutARealPilot() {
        Unit unit = unitFor(mock(Entity.class));
        unit.setTempCrew(PersonnelRole.VESSEL_PILOT, 3);

        assertEquals(0, unit.getEffectiveTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT),
              "temp pilots must not count while no real pilot is aboard");
        assertEquals(3, unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT),
              "the raw temp count is unchanged; only the effective count is gated");
    }

    @Test
    void tempPilotsCountOnceARealPilotIsPresent() {
        // A plain entity yields a null driver role, so addDriver does not release a temp pilot here; this lets us test
        // the effective-count gate in isolation from the release behaviour.
        Unit unit = unitFor(mock(Entity.class));
        unit.setTempCrew(PersonnelRole.VESSEL_PILOT, 3);

        unit.addDriver(mockPerson());

        assertEquals(3, unit.getEffectiveTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT));
    }

    @Test
    void tempGunnersCountOnceARealGunnerIsPresent() {
        Unit unit = unitFor(mock(Entity.class));
        unit.setTempCrew(PersonnelRole.VESSEL_GUNNER, 2);

        assertEquals(0, unit.getEffectiveTempCrewByPersonnelRole(PersonnelRole.VESSEL_GUNNER));

        unit.addGunner(mockPerson());

        assertEquals(2, unit.getEffectiveTempCrewByPersonnelRole(PersonnelRole.VESSEL_GUNNER));
    }

    @Test
    void tempVesselCrewCountsOnceRealCrewIsPresent() {
        Unit unit = unitFor(mock(Entity.class));
        unit.setTempCrew(PersonnelRole.VESSEL_CREW, 5);

        assertEquals(0, unit.getEffectiveTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW));

        unit.addVesselCrew(mockPerson());

        assertEquals(5, unit.getEffectiveTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW));
    }

    @Test
    void nonVesselRolesReportTheirRawTempCount() {
        Unit unit = unitFor(mock(Entity.class));
        unit.setTempCrew(PersonnelRole.SOLDIER, 5);

        // The rule does not apply to infantry temp crew, so the effective count equals the raw count.
        assertEquals(5, unit.getEffectiveTempCrewByPersonnelRole(PersonnelRole.SOLDIER));
    }

    // endregion getEffectiveTempCrewByPersonnelRole

    // region temp crew release on assignment

    @Test
    void assigningARealDriverReleasesOneTempPilot() {
        Unit unit = unitFor(dropship());
        unit.setTempCrew(PersonnelRole.VESSEL_PILOT, 3);

        unit.addDriver(mockPerson());

        assertEquals(2, unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT),
              "a real pilot must replace a temp pilot rather than stack on top of the full temp crew");
    }

    @Test
    void assigningARealGunnerReleasesOneTempGunner() {
        Unit unit = unitFor(dropship());
        unit.setTempCrew(PersonnelRole.VESSEL_GUNNER, 4);

        unit.addGunner(mockPerson());

        assertEquals(3, unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_GUNNER));
    }

    @Test
    void assigningRealVesselCrewReleasesOneTempCrew() {
        Unit unit = unitFor(dropship());
        unit.setTempCrew(PersonnelRole.VESSEL_CREW, 6);

        unit.addVesselCrew(mockPerson());

        assertEquals(5, unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW));
    }

    @Test
    void assigningARealDriverWithNoTempPilotsLeavesTheCountAtZero() {
        Unit unit = unitFor(dropship());

        unit.addDriver(mockPerson());

        assertEquals(0, unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT));
    }

    @Test
    void releasingTempPilotsStopsAtZeroAcrossSuccessiveAssignments() {
        Unit unit = unitFor(dropship());
        unit.setTempCrew(PersonnelRole.VESSEL_PILOT, 1);

        unit.addDriver(mockPerson());
        // The single temp pilot is now released.
        assertEquals(0, unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT));

        // Adding a second real pilot must not drive the temp count negative.
        unit.addDriver(mockPerson());
        assertEquals(0, unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT));
    }

    // endregion temp crew release on assignment
}
