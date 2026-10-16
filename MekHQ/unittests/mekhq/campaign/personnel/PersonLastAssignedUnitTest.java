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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.UUID;

import mekhq.EventSpy;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.enums.CrewSeat;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that a character taken off active duty remembers the unit and seat they left, and returns to it when they come
 * back if it is still open.
 *
 * @author Illiani
 * @since 0.51.01
 */
class PersonLastAssignedUnitTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 1);

    private Campaign campaign;
    private Person person;
    private Unit unit;
    private UUID unitId;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        when(campaign.getCampaignOptions()).thenReturn(new CampaignOptions());
        when(campaign.getLocalDate()).thenReturn(TODAY);
        // Prerequisites for removeAllTechJobs, which every change to an inactive status runs
        when(campaign.getPlayerForce().getHangar()).thenReturn(new LocalHangar());
        when(campaign.getPlayerForce().getWarehouse()).thenReturn(new LocalWarehouse());

        Faction faction = mock(Faction.class);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
        when(faction.getShortName()).thenReturn("MERC");

        person = new Person(campaign);
        person.setFullNameDirect("Returning Crewman");

        unitId = UUID.randomUUID();
        unit = mock(Unit.class);
        when(unit.getId()).thenReturn(unitId);
        when(unit.getName()).thenReturn("Test Unit");
        when(unit.getCrewSeat(person)).thenReturn(CrewSeat.DRIVER);
        // The mock unit stands in for the real crew bookkeeping on the person's side
        doAnswer(invocation -> {
            person.setUnit(null);
            return null;
        }).when(unit).remove(eq(person), anyBoolean());
        doAnswer(invocation -> {
            person.setUnit(unit);
            return null;
        }).when(unit).addToCrewSeat(person, CrewSeat.DRIVER);

        person.setUnit(unit);
    }

    @Test
    void goingOnLeaveRemembersUnitAndSeat() {
        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
        }

        assertNull(person.getUnit());
        assertEquals(unitId, person.getLastAssignedUnitId());
        assertEquals(CrewSeat.DRIVER, person.getLastAssignedCrewSeat());
    }

    @Test
    void returningFromLeaveRetakesOpenSeat() {
        when(campaign.getUnit(unitId)).thenReturn(unit);
        when(unit.isAvailable(true)).thenReturn(true);
        when(unit.hasOpenCrewSeat(CrewSeat.DRIVER)).thenReturn(true);

        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            person.changeStatus(campaign, TODAY, PersonnelStatus.ACTIVE);
        }

        verify(unit).addToCrewSeat(person, CrewSeat.DRIVER);
        assertEquals(unit, person.getUnit());
        assertNull(person.getLastAssignedUnitId());
        assertNull(person.getLastAssignedCrewSeat());
    }

    @Test
    void returningFromLeaveToFilledSeatStaysUnassigned() {
        when(campaign.getUnit(unitId)).thenReturn(unit);
        when(unit.isAvailable(true)).thenReturn(true);
        when(unit.hasOpenCrewSeat(CrewSeat.DRIVER)).thenReturn(false);

        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            person.changeStatus(campaign, TODAY, PersonnelStatus.ACTIVE);
        }

        verify(unit, never()).addToCrewSeat(any(), any());
        assertNull(person.getUnit());
        assertNull(person.getLastAssignedUnitId());
    }

    @Test
    void returningFromLeaveToUnavailableUnitStaysUnassigned() {
        when(campaign.getUnit(unitId)).thenReturn(unit);
        when(unit.isAvailable(true)).thenReturn(false);
        when(unit.hasOpenCrewSeat(CrewSeat.DRIVER)).thenReturn(true);

        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            person.changeStatus(campaign, TODAY, PersonnelStatus.ACTIVE);
        }

        verify(unit, never()).addToCrewSeat(any(), any());
        assertNull(person.getUnit());
    }

    @Test
    void returningAfterUnitLeftCampaignStaysUnassigned() {
        when(campaign.getUnit(unitId)).thenReturn(null);

        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            person.changeStatus(campaign, TODAY, PersonnelStatus.ACTIVE);
        }

        verify(unit, never()).addToCrewSeat(any(), any());
        assertNull(person.getUnit());
        assertNull(person.getLastAssignedUnitId());
    }

    @Test
    void movingBetweenInactiveStatusesKeepsLastUnit() {
        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            person.changeStatus(campaign, TODAY, PersonnelStatus.MIA);
        }

        assertEquals(unitId, person.getLastAssignedUnitId());
        assertEquals(CrewSeat.DRIVER, person.getLastAssignedCrewSeat());
    }

    @Test
    void retrainingForgetsLastUnit() {
        try (EventSpy ignored = new EventSpy()) {
            person.changeStatus(campaign, TODAY, PersonnelStatus.ON_LEAVE);
            person.setPrimaryRole(TODAY, PersonnelRole.ADMINISTRATOR_COMMAND);
        }

        assertNull(person.getLastAssignedUnitId());
        assertNull(person.getLastAssignedCrewSeat());
    }
}
