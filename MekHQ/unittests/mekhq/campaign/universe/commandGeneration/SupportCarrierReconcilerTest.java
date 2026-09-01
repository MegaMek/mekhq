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
package mekhq.campaign.universe.commandGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.SupportPersonnelToTOE.SupportSection;
import org.junit.jupiter.api.Test;

/**
 * Covers the guards and the release path of {@link SupportCarrierReconciler}.
 *
 * <p>The seating path proper needs a loaded unit cache and real campaign state, so it is exercised in play rather than
 * here. What these tests pin down is the part that runs on every person event in every campaign: that the guards reject
 * the common case without touching campaign state, and that the one departure case the engine does not handle is
 * actually handled.</p>
 */
class SupportCarrierReconcilerTest {

    // --- The shared eligibility predicate ---

    @Test
    void sectionFor_techIsMaintenance() {
        assertEquals(SupportSection.MAINTENANCE, SupportPersonnelToTOE.sectionFor(PersonnelRole.MEK_TECH));
    }

    @Test
    void sectionFor_doctorIsMedical() {
        assertEquals(SupportSection.MEDICAL, SupportPersonnelToTOE.sectionFor(PersonnelRole.DOCTOR));
    }

    @Test
    void sectionFor_administratorIsCommand() {
        assertEquals(SupportSection.COMMAND, SupportPersonnelToTOE.sectionFor(PersonnelRole.ADMINISTRATOR));
    }

    @Test
    void sectionFor_combatRoleIsNotCarried() {
        assertNull(SupportPersonnelToTOE.sectionFor(PersonnelRole.MEKWARRIOR));
    }

    @Test
    void sectionFor_nullIsNotCarried() {
        assertNull(SupportPersonnelToTOE.sectionFor(null));
    }

    // --- Legacy carrier identification ---

    @Test
    void isCarrierChassis_recognisesEveryChassisTheGeneratorBuilds() {
        assertTrue(SupportPersonnelToTOE.isCarrierChassis("Support Platoon"));
        assertTrue(SupportPersonnelToTOE.isCarrierChassis("Support Squad"));
        assertTrue(SupportPersonnelToTOE.isCarrierChassis("Clan Support Point"));
        assertTrue(SupportPersonnelToTOE.isCarrierChassis("Clan Support Squad"));
    }

    @Test
    void isCarrierChassis_rejectsFightingUnitsAndNull() {
        assertFalse(SupportPersonnelToTOE.isCarrierChassis("Marauder"));
        assertFalse(SupportPersonnelToTOE.isCarrierChassis("Foot Platoon"));
        assertFalse(SupportPersonnelToTOE.isCarrierChassis(null));
    }

    // --- Arrival guards: each must exit before touching campaign state ---

    @Test
    void seatIfEligible_nullArgumentsAreIgnored() {
        Campaign campaign = mock(Campaign.class);
        SupportCarrierReconciler.seatIfEligible(campaign, null);
        SupportCarrierReconciler.seatIfEligible(null, mock(Person.class));
        verify(campaign, never()).getPlayerForce();
    }

    @Test
    void seatIfEligible_someoneAlreadyCrewingAUnitIsLeftAlone() {
        Campaign campaign = mock(Campaign.class);
        Person person = mock(Person.class);
        when(person.getUnit()).thenReturn(mock(Unit.class));

        SupportCarrierReconciler.seatIfEligible(campaign, person);

        // The cheapest guard must reject before anything else is read.
        verify(campaign, never()).getPlayerForce();
        verify(person, never()).getPrimaryRole();
    }

    @Test
    void seatIfEligible_combatRoleIsNotSeated() {
        Campaign campaign = mock(Campaign.class);
        Person person = activePerson(PersonnelRole.MEKWARRIOR);

        SupportCarrierReconciler.seatIfEligible(campaign, person);

        verify(campaign, never()).getPlayerForce();
    }

    @Test
    void seatIfEligible_inactiveSupportStaffAreNotSeated() {
        Campaign campaign = mock(Campaign.class);
        Person person = activePerson(PersonnelRole.MEK_TECH);
        when(person.getStatus()).thenReturn(PersonnelStatus.RETIRED);

        SupportCarrierReconciler.seatIfEligible(campaign, person);

        verify(campaign, never()).getPlayerForce();
    }

    @Test
    void seatIfEligible_prisonersAreNotSeated() {
        Campaign campaign = mock(Campaign.class);
        Person person = activePerson(PersonnelRole.DOCTOR);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.PRISONER);

        SupportCarrierReconciler.seatIfEligible(campaign, person);

        verify(campaign, never()).getPlayerForce();
    }

    @Test
    void seatIfEligible_campaignWithNoSupportStructureIsLeftAlone() {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getSupportCommandFormation()).thenReturn(null);
        Person person = activePerson(PersonnelRole.ADMINISTRATOR);

        SupportCarrierReconciler.seatIfEligible(campaign, person);

        // No support structure means the campaign never opted in. It must not be invented here.
        verify(playerForce, never()).isClanForce();
    }

    // --- Release: the one departure the campaign engine does not already handle ---

    @Test
    void releaseIfIneligible_technicianWhoRetrainedAsAWarriorLosesTheirSeat() {
        Campaign campaign = mock(Campaign.class);
        Unit carrier = mock(Unit.class);
        when(carrier.isCarrier()).thenReturn(true);

        Person person = activePerson(PersonnelRole.MEKWARRIOR);
        when(person.getUnit()).thenReturn(carrier);

        SupportCarrierReconciler.releaseIfIneligible(campaign, person);

        verify(carrier, times(1)).remove(person, true);
    }

    @Test
    void releaseIfIneligible_technicianInTheirOwnCarrierKeepsTheirSeat() {
        Campaign campaign = mock(Campaign.class);
        Person person = activePerson(PersonnelRole.MEK_TECH);
        Unit carrier = mock(Unit.class);
        when(carrier.isCarrier()).thenReturn(true);
        when(carrier.getCrew()).thenReturn(List.of(person));
        when(person.getUnit()).thenReturn(carrier);

        SupportCarrierReconciler.releaseIfIneligible(campaign, person);

        verify(carrier, never()).remove(any(Person.class), anyBoolean());
    }

    @Test
    void releaseIfIneligible_crewOfARealUnitIsNeverTouched() {
        Campaign campaign = mock(Campaign.class);
        Unit fightingUnit = mock(Unit.class);
        when(fightingUnit.isCarrier()).thenReturn(false);

        Person person = activePerson(PersonnelRole.MEKWARRIOR);
        when(person.getUnit()).thenReturn(fightingUnit);

        SupportCarrierReconciler.releaseIfIneligible(campaign, person);

        verify(fightingUnit, never()).remove(any(Person.class), anyBoolean());
    }

    // --- Emptied carriers ---

    @Test
    void onCarrierCrewChanged_emptyCarrierIsRemoved() {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);

        UUID unitId = UUID.randomUUID();
        Unit carrier = mock(Unit.class);
        when(carrier.isCarrier()).thenReturn(true);
        when(carrier.getCrew()).thenReturn(List.of());
        when(carrier.getId()).thenReturn(unitId);

        SupportCarrierReconciler.onCarrierCrewChanged(campaign, carrier);

        verify(campaign, times(1)).removeUnit(unitId);
    }

    @Test
    void onCarrierCrewChanged_carrierWithCrewIsKept() {
        Campaign campaign = mock(Campaign.class);
        // Built before the stubbing below: creating a mock inside when(...) leaves the outer stubbing unfinished.
        Person crew = activePerson(PersonnelRole.MEK_TECH);
        Unit carrier = mock(Unit.class);
        when(carrier.isCarrier()).thenReturn(true);
        when(carrier.getCrew()).thenReturn(List.of(crew));

        SupportCarrierReconciler.onCarrierCrewChanged(campaign, carrier);

        verify(campaign, never()).removeUnit(any(UUID.class));
    }

    @Test
    void onCarrierCrewChanged_emptyFightingUnitIsNotRemoved() {
        Campaign campaign = mock(Campaign.class);
        Unit fightingUnit = mock(Unit.class);
        when(fightingUnit.isCarrier()).thenReturn(false);

        SupportCarrierReconciler.onCarrierCrewChanged(campaign, fightingUnit);

        // A crewless Mek is a normal state of affairs and is emphatically not ours to delete.
        verify(campaign, never()).removeUnit(any(UUID.class));
    }

    /** A free, active character in the given role, holding no unit. */
    private static Person activePerson(PersonnelRole role) {
        Person person = mock(Person.class);
        when(person.getUnit()).thenReturn(null);
        when(person.getPrimaryRole()).thenReturn(role);
        when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.getPrisonerStatus()).thenReturn(PrisonerStatus.FREE);
        return person;
    }
}
