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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import megamek.common.EquipmentType;
import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;

public class MovedEquipmentStepTest {
    @Test
    public void noMatchingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        when(mockProposal.getEquipment()).thenReturn(Collections.emptySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void noMatchingMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        when(mockProposal.getEquipment()).thenReturn(Collections.emptySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void doesNotMatchDestroyedEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.isDestroyed()).thenReturn(true);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void doesNotMatchDestroyedMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.isDestroyed()).thenReturn(true);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void doesNotMatchEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(EquipmentType.class));
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);
        when(mockPart.getType()).thenReturn(mock(EquipmentType.class));

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void doesNotMatchMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mock(EquipmentType.class));
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(0, mockMount).entrySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);
        when(mockMissingPart.getType()).thenReturn(mock(EquipmentType.class));

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void mountMatchesEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentType mockType = mock(EquipmentType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockType);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(1, mockMount).entrySet());
        EquipmentPart mockPart = mock(EquipmentPart.class);
        when(mockPart.getType()).thenReturn(mockType);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockPart);

        verify(mockProposal, times(1)).proposeMapping(eq(mockPart), eq(1));
    }

    @Test
    public void mountMatchesMissingEquipmentTest() {
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentType mockType = mock(EquipmentType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockType);
        when(mockProposal.getEquipment()).thenReturn(Collections.singletonMap(1, mockMount).entrySet());
        MissingEquipmentPart mockMissingPart = mock(MissingEquipmentPart.class);
        when(mockMissingPart.getType()).thenReturn(mockType);

        MovedEquipmentStep step = new MovedEquipmentStep();

        step.visit(mockProposal, mockMissingPart);

        verify(mockProposal, times(1)).proposeMapping(eq(mockMissingPart), eq(1));
    }
}
