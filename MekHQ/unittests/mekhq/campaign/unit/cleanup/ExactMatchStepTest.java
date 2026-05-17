/*
 * Copyright (C) 2021-2026 The MegaMek Team. All Rights Reserved.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import org.junit.jupiter.api.Test;

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

    @Test
    public void missingAmmoBinWithNonAmmoTypeMountDoesNotThrowTest() {
        // mount.getType() is a plain EquipmentType (not AmmoType).
        // Before the instanceof AmmoType guard was added, the cast to AmmoType would
        // throw a ClassCastException.
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        EquipmentType mockNonAmmoType = mock(EquipmentType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockNonAmmoType);
        doReturn(mockMount).when(mockProposal).getEquipment(eq(1));
        MissingAmmoBin mockMissingAmmoBin = mock(MissingAmmoBin.class);
        when(mockMissingAmmoBin.getEquipmentNum()).thenReturn(1);
        when(mockMissingAmmoBin.getType()).thenReturn(mock(AmmoType.class));

        ExactMatchStep step = new ExactMatchStep();

        assertDoesNotThrow(() -> step.visit(mockProposal, mockMissingAmmoBin));
        verify(mockProposal, times(0)).proposeMapping(any(), anyInt());
    }

    @Test
    public void missingAmmoBinWithAmmoTypeMountProposesMapping() {
        // When mount.getType() is an AmmoType and canChangeMunitions returns true,
        // proposeMapping should be called for the MissingAmmoBin.
        EquipmentProposal mockProposal = mock(EquipmentProposal.class);
        AmmoType mockMountAmmoType = mock(AmmoType.class);
        Mounted mockMount = mock(Mounted.class);
        when(mockMount.getType()).thenReturn(mockMountAmmoType);
        doReturn(mockMount).when(mockProposal).getEquipment(eq(1));
        AmmoBin mockAmmoBin = mock(AmmoBin.class);
        when(mockAmmoBin.canChangeMunitions(mockMountAmmoType)).thenReturn(true);
        MissingAmmoBin mockMissingAmmoBin = mock(MissingAmmoBin.class);
        when(mockMissingAmmoBin.getEquipmentNum()).thenReturn(1);
        when(mockMissingAmmoBin.getType()).thenReturn(mock(AmmoType.class));
        doReturn(mockAmmoBin).when(mockMissingAmmoBin).getReplacementPart();

        ExactMatchStep step = new ExactMatchStep();

        step.visit(mockProposal, mockMissingAmmoBin);

        verify(mockProposal, times(1)).proposeMapping(eq(mockMissingAmmoBin), eq(1));
    }
}
