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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
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
    void vesselWithNoCrewHasNoEngineer() {
        unit.resetEngineer();

        assertNull(unit.getEngineer());
    }

    /** Adds a vessel crew member of the given rank; a higher rank outranks a lower one. */
    private Person vesselCrew(int rank) {
        UUID id = UUID.randomUUID();
        Person person = mock(Person.class);
        when(person.getId()).thenReturn(id);
        when(person.getUnit()).thenReturn(unit);
        when(campaign.getPlayerForce().getHumanResources().getPerson(eq(id))).thenReturn(person);
        ranks.put(person, rank);
        when(person.outRanksUsingSkillTiebreaker(any(Campaign.class), any())).thenAnswer(invocation -> {
            Person otherPerson = invocation.getArgument(1);
            return (otherPerson == null) || (rank > ranks.get(otherPerson));
        });

        unit.addVesselCrew(person);
        return person;
    }
}
