/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map.Entry;

import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class EquipmentProposalTest {

    @Test
    void getUnitTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);
        assertEquals(unit, proposal.getUnit());
    }

    @Test
    void considerTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        Part mockPart = mock(Part.class);
        proposal.consider(mockPart);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        proposal.consider(mockEquipmentPart);

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        proposal.consider(mockMissingEquipmentPart);

        assertFalse(proposal.getParts().contains(mockPart));
        assertTrue(proposal.getParts().contains(mockEquipmentPart));
        assertTrue(proposal.getParts().contains(mockMissingEquipmentPart));
    }

    @Test
    void includeEquipmentTest() {
        Unit unit = mock(Unit.class);

        int equipmentNum = 1;
        EquipmentProposal proposal = new EquipmentProposal(unit);

        assertNull(proposal.getEquipment(equipmentNum));
        assertTrue(proposal.getEquipment().isEmpty());

        Mounted mockMounted = mock(Mounted.class);
        proposal.includeEquipment(equipmentNum, mockMounted);

        assertEquals(mockMounted, proposal.getEquipment(equipmentNum));
        assertFalse(proposal.getEquipment().isEmpty());

        for (Entry<Integer, Mounted> entry : proposal.getEquipment()) {
            assertEquals(equipmentNum, (int) entry.getKey());
            assertEquals(mockMounted, entry.getValue());
        }
    }

    @Test
    void getOriginalMappingTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        assertTrue(proposal.getOriginalMapping(mock(EquipmentPart.class)) < 0);
        assertEquals(originalEquipmentNum, proposal.getOriginalMapping(mockEquipmentPart));
        assertEquals(originalMissingEquipmentNum, proposal.getOriginalMapping(mockMissingEquipmentPart));
    }

    @Test
    void proposeMappingTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        assertFalse(proposal.hasProposal(mockEquipmentPart));

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        assertFalse(proposal.hasProposal(mockMissingEquipmentPart));

        Mounted mockMount0 = mock(Mounted.class);
        proposal.includeEquipment(originalEquipmentNum, mockMount0);
        Mounted mockMount1 = mock(Mounted.class);
        proposal.includeEquipment(originalMissingEquipmentNum, mockMount1);

        proposal.proposeMapping(mockEquipmentPart, originalEquipmentNum);

        assertTrue(proposal.hasProposal(mockEquipmentPart));
        assertNull(proposal.getEquipment(originalEquipmentNum));
        assertEquals(mockMount1, proposal.getEquipment(originalMissingEquipmentNum));

        proposal.proposeMapping(mockMissingEquipmentPart, originalMissingEquipmentNum);

        assertTrue(proposal.hasProposal(mockMissingEquipmentPart));
        assertNull(proposal.getEquipment(originalEquipmentNum));
        assertNull(proposal.getEquipment(originalMissingEquipmentNum));
    }

    @Test
    void isReducedTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        assertFalse(proposal.isReduced());

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        assertFalse(proposal.isReduced());

        Mounted mockMount0 = mock(Mounted.class);
        proposal.includeEquipment(originalEquipmentNum, mockMount0);
        Mounted mockMount1 = mock(Mounted.class);
        proposal.includeEquipment(originalMissingEquipmentNum, mockMount1);

        proposal.proposeMapping(mockEquipmentPart, originalEquipmentNum);

        assertFalse(proposal.isReduced());

        proposal.proposeMapping(mockMissingEquipmentPart, originalMissingEquipmentNum);

        assertTrue(proposal.isReduced());
    }

    @Test
    void applyTest() {
        Unit unit = mock(Unit.class);

        EquipmentProposal proposal = new EquipmentProposal(unit);

        EquipmentPart mockEquipmentPart = mock(EquipmentPart.class);
        int originalEquipmentNum = 1;
        when(mockEquipmentPart.getEquipmentNum()).thenReturn(originalEquipmentNum);
        proposal.consider(mockEquipmentPart);

        MissingEquipmentPart mockMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int originalMissingEquipmentNum = 2;
        when(mockMissingEquipmentPart.getEquipmentNum()).thenReturn(originalMissingEquipmentNum);
        proposal.consider(mockMissingEquipmentPart);

        EquipmentPart mockIncorrectEquipmentPart = mock(EquipmentPart.class);
        int incorrectEquipmentNum = 3;
        when(mockIncorrectEquipmentPart.getEquipmentNum()).thenReturn(incorrectEquipmentNum);
        proposal.consider(mockIncorrectEquipmentPart);

        MissingEquipmentPart mockIncorrectMissingEquipmentPart = mock(MissingEquipmentPart.class);
        int incorrectMissingEquipmentNum = 4;
        when(mockIncorrectMissingEquipmentPart.getEquipmentNum()).thenReturn(incorrectMissingEquipmentNum);
        proposal.consider(mockIncorrectMissingEquipmentPart);

        Mounted mockMount0 = mock(Mounted.class);
        proposal.includeEquipment(originalEquipmentNum, mockMount0);
        Mounted mockMount1 = mock(Mounted.class);
        proposal.includeEquipment(originalMissingEquipmentNum, mockMount1);

        proposal.proposeMapping(mockEquipmentPart, originalEquipmentNum);
        proposal.proposeMapping(mockMissingEquipmentPart, originalMissingEquipmentNum);

        assertFalse(proposal.isReduced());

        proposal.apply();

        verify(mockEquipmentPart, times(1)).setEquipmentNum(originalEquipmentNum);
        verify(mockMissingEquipmentPart, times(1)).setEquipmentNum(originalMissingEquipmentNum);
        verify(mockIncorrectEquipmentPart, times(1)).setEquipmentNum(-1);
        verify(mockIncorrectMissingEquipmentPart, times(1)).setEquipmentNum(-1);
    }
}
