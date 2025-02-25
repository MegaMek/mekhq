/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.unit.cleanup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import megamek.common.EquipmentType;
import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;

public class ExactMatchStepTest {
    @Test
    public void noMatchingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentPart mockPart = mock(EquipmentPart.class);

        ExactMatchStep step = new ExactMatchStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void noMatchingMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);

        ExactMatchStep step = new ExactMatchStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void mountDoesNotMatchEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(EquipmentType.class));
        doReturn(mockMount).when(mockProposal).getEquipment(eq(1));
        EquipmentPart mockPart = mock(EquipmentPart.class);
        when(mockPart.getEquipmentNum()).thenReturn(1);
        when(mockPart.getType()).thenReturn(mock(EquipmentType.class));

        ExactMatchStep step = new ExactMatchStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void mountDoesNotMatchMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(EquipmentType.class));
        doReturn(mockMount).when(mockProposal).getEquipment(eq(1));
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);
        when(mockMissingPart.getEquipmentNum()).thenReturn(1);
        when(mockMissingPart.getType()).thenReturn(mock(EquipmentType.class));

        ExactMatchStep step = new ExactMatchStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void mountMatchesEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentType mockType = mock(EquipmentType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockType);
        doReturn(mockMount).when(mockProposal).getEquipment(eq(1));
        EquipmentPart mockPart = mock(EquipmentPart.class);
        when(mockPart.getEquipmentNum()).thenReturn(1);
        when(mockPart.getType()).thenReturn(mockType);

        ExactMatchStep step = new ExactMatchStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(1)).proposeMapping(eq(mockPart), eq(1));
    }

    @Test
    public void mountMatchesMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentType mockType = mock(EquipmentType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockType);
        doReturn(mockMount).when(mockProposal).getEquipment(eq(1));
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);
        when(mockMissingPart.getEquipmentNum()).thenReturn(1);
        when(mockMissingPart.getType()).thenReturn(mockType);

        ExactMatchStep step = new ExactMatchStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(1)).proposeMapping(eq(mockMissingPart), eq(1));
    }
}
