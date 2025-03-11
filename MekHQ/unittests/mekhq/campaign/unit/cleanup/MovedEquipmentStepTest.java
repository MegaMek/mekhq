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
