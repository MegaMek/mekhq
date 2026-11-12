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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import megamek.common.units.Dropship;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A large vessel's engineer is the highest-ranked member of its vessel crew. The chosen engineer is flagged as such,
 * so the daily maintenance check and the salvage, refit and TOE tech pickers recognise them, and the flag is cleared
 * when they stop being the engineer.
 */
class UnitVesselEngineerTest {
    private final Map<Person, Integer> ranks = new HashMap<>();

    private Campaign campaign;
    private Unit unit;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        unit = spy(new Unit(mock(Dropship.class), campaign));
        doNothing().when(unit).resetPilotAndEntity();
    }

    @Test
    void highestRankedVesselCrewIsEngineer() {
        vesselCrew(1);
        Person captain = vesselCrew(10);
        vesselCrew(5);

        unit.resetEngineer();

        assertSame(captain, unit.getEngineer());
    }

    @Test
    void engineerIsFlagged() {
        Person captain = vesselCrew(10);
        vesselCrew(1);

        unit.resetEngineer();

        verify(captain).setEngineer(true);
    }

    @Test
    void engineerWhoLeavesTheShipIsNoLongerFlagged() {
        // Otherwise, as an ordinary Tech elsewhere they would still be skipped for salvage and refits
        Person captain = vesselCrew(10);
        Person mate = vesselCrew(5);
        unit.resetEngineer();

        unit.remove(captain, false);
        verify(captain).setEngineer(false);

        unit.resetEngineer();
        verify(mate).setEngineer(true);
        assertSame(mate, unit.getEngineer());
    }

    @Test
    void engineerOutrankedByANewCrewMemberIsNoLongerFlagged() {
        Person captain = vesselCrew(5);
        unit.resetEngineer();

        Person commodore = vesselCrew(10);
        unit.resetEngineer();

        verify(captain).setEngineer(false);
        verify(commodore).setEngineer(true);
        assertSame(commodore, unit.getEngineer());
    }

    @Test
    void crewMemberWhoNoLongerServesAboardIsNotEngineer() {
        // Issue #10538: a crew member who retired years ago was still on the ship's crew list. As the highest rank they
        // became the engineer, and the daily maintenance check crashed looking up the unit they no longer serve on.
        Person retiredCaptain = vesselCrew(10);
        when(retiredCaptain.getUnit()).thenReturn(null);
        Person mate = vesselCrew(5);

        unit.resetEngineer();

        assertSame(mate, unit.getEngineer());
        verify(retiredCaptain, never()).setEngineer(true);
    }

    @Test
    void vesselWhoseOnlyCrewNoLongerServesAboardHasNoEngineer() {
        Person retiredCaptain = vesselCrew(10);
        when(retiredCaptain.getUnit()).thenReturn(null);

        unit.resetEngineer();

        assertNull(unit.getEngineer());
    }

    @Test
    void crewMemberWhoseUnitIsStillALoadPlaceholderIsEngineer() {
        // While a campaign loads, the engineer is chosen before each person's unit placeholder is swapped for the
        // real unit
        Person captain = vesselCrewStillLoading(10);

        unit.resetEngineer();

        assertSame(captain, unit.getEngineer());
    }

    @Test
    void activationSurvivesEngineerResetWhileLoading() {
        // A DropShip part-way through activation lost its remaining time, and went back to plain mothballed, on every
        // load. Mothballed first: mothballing a unit sends its crew ashore
        unit.setMothballed(true);
        vesselCrewStillLoading(10);
        unit.setMothballTime(10608);

        unit.resetEngineer();

        assertEquals(10608, unit.getMothballTime());
    }

    @Test
    void mothballingSurvivesEngineerResetWhileLoading() {
        // Not yet mothballed, so the running time is mothballing work; it was lost on load the same way
        vesselCrewStillLoading(10);
        unit.setMothballTime(10608);

        unit.resetEngineer();

        assertEquals(10608, unit.getMothballTime());
    }

    @Test
    void crewMemberOfAnotherUnitIsNotEngineer() {
        unit.setId(UUID.randomUUID());
        Person transferredCaptain = vesselCrew(10);
        Unit otherUnit = mock(Unit.class);
        when(otherUnit.getId()).thenReturn(UUID.randomUUID());
        when(transferredCaptain.getUnit()).thenReturn(otherUnit);

        unit.resetEngineer();

        assertNull(unit.getEngineer());
    }

    @Test
    void vesselWithNoCrewHasNoEngineer() {
        unit.resetEngineer();

        assertNull(unit.getEngineer());
    }

    /** Adds a vessel crew member of the given rank; a higher rank outranks a lower one. */
    private Person vesselCrew(int rank) {
        UUID id = UUID.randomUUID();
        Person person = mock(Person.class);
        lenient().when(person.getStatus()).thenReturn(PersonnelStatus.ACTIVE);
        when(person.getId()).thenReturn(id);
        when(person.getUnit()).thenReturn(unit);
        when(campaign.getPlayerForce().getHumanResources().getPerson(eq(id))).thenReturn(person);
        ranks.put(person, rank);
        when(person.outRanksUsingSkillTiebreaker(any(), anyBoolean(), any(), any())).thenAnswer(invocation -> {
            Person otherPerson = invocation.getArgument(3);
            return (otherPerson == null) || (rank > ranks.get(otherPerson));
        });

        unit.addVesselCrew(person);
        return person;
    }

    /**
     * Adds a vessel crew member whose unit is, as during campaign loading, a placeholder carrying this unit's id rather
     * than the unit itself.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private Person vesselCrewStillLoading(int rank) {
        UUID unitId = UUID.randomUUID();
        unit.setId(unitId);
        Unit placeholder = mock(Unit.class);
        when(placeholder.getId()).thenReturn(unitId);

        Person person = vesselCrew(rank);
        when(person.getUnit()).thenReturn(placeholder);
        return person;
    }
}
